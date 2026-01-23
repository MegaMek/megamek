/*
 * Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.units;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.codeUtilities.StringUtility;
import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.bays.Bay;
import megamek.common.equipment.*;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.force.Force;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntitySavingException;
import megamek.common.loaders.MULParser;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;

/**
 * This class provides static methods to save a list of <code>Entity</code>s to, and load a list of <code>Entity</code>s
 * from a file.
 */
public class EntityListFile {
    private static final MMLogger logger = MMLogger.create(EntityListFile.class);

    /**
     * Produce a string describing this armor value. Valid output values are any integer from 0 to 100, N/A, or
     * Destroyed.
     *
     * @param points - the <code>int</code> value of the armor. This value may be any valid value of entity armor
     *               (including NA, DOOMED, and DESTROYED).
     *
     * @return a <code>String</code> that matches the armor value.
     */
    private static String formatArmor(int points) {
        // Is the armor destroyed or doomed?
        if ((points == IArmorState.ARMOR_DOOMED) || (points == IArmorState.ARMOR_DESTROYED)) {
            return MULParser.VALUE_DESTROYED;
        }

        // Was there armor to begin with?
        if (points == IArmorState.ARMOR_NA) {
            return MULParser.VALUE_NA;
        }

        // Translate the int to a String.
        return String.valueOf(points);
    }

    /**
     * Produce a string describing the equipment in a critical slot.
     *
     * @param index       - the <code>String</code> index of the slot. This value should be a positive integer or
     *                    "N/A".
     * @param mount       - the <code>Mounted</code> object of the equipment. This value should be <code>null</code> for
     *                    a slot with system equipment.
     * @param isHit       - a <code>boolean</code> that identifies this slot as having taken a hit.
     * @param isDestroyed - a <code>boolean</code> that identifies the equipment as having been destroyed. Note that a
     *                    single slot in a multi-slot piece of equipment can be destroyed but not hit; it is still
     *                    available to absorb additional critical hits.
     *
     * @return a <code>String</code> describing the slot.
     */
    private static String formatSlot(String index, Mounted<?> mount, boolean isHit, boolean isDestroyed,
          boolean isRepairable, boolean isMissing, int indentLvl) {
        StringBuilder output = new StringBuilder();

        output.append(indentStr(indentLvl))
              .append('<' + MULParser.ELE_SLOT + ' ' + MULParser.ATTR_INDEX + "=\"")
              .append(index)
              .append("\" " + MULParser.ATTR_TYPE + "=\"");

        if (mount == null) {
            output.append(MULParser.VALUE_SYSTEM);
        } else {
            output.append(mount.getType().getInternalName());
            if (mount.isRearMounted()) {
                output.append("\" " + MULParser.ATTR_IS_REAR + "=\"true");
            }

            if (mount.isMekTurretMounted()) {
                output.append("\" " + MULParser.ATTR_IS_TURRETED + "=\"true");
            }

            if (mount.getType() instanceof AmmoType) {
                output.append("\" " + MULParser.ATTR_SHOTS + "=\"").append(mount.getBaseShotsLeft());
                if (mount.getEntity().usesWeaponBays() || (mount.getEntity() instanceof Dropship)) {
                    output.append("\" " + MULParser.ATTR_CAPACITY + "=\"").append(mount.getSize());
                }
            }

            if ((mount.getType() instanceof WeaponType) &&
                  (mount.getType()).hasFlag(WeaponType.F_ONE_SHOT) &&
                  (mount.getLinked() != null)) {
                output.append("\" " + MULParser.ATTR_MUNITION + "=\"");
                output.append(mount.getLinked().getType().getInternalName());
            }

            if (mount.getEntity().isSupportVehicle() && (mount.getType() instanceof InfantryWeapon)) {
                for (Mounted<?> ammo = mount.getLinked(); ammo != null; ammo = ammo.getLinked()) {
                    if (((AmmoType) ammo.getType()).getMunitionType().contains(Munitions.M_INFERNO)) {
                        output.append("\" " + MULParser.ATTR_INFERNO + "=\"")
                              .append(ammo.getBaseShotsLeft())
                              .append(':')
                              .append(ammo.getOriginalShots());
                    } else {
                        output.append("\" " + MULParser.ATTR_STANDARD + "=\"")
                              .append(ammo.getBaseShotsLeft())
                              .append(':')
                              .append(ammo.getOriginalShots());
                    }
                }
            }

            if (mount.isRapidFire()) {
                output.append("\" " + MULParser.ATTR_RFMG + "=\"true");
            }

            if (mount.countQuirks() > 0) {
                output.append("\" " + MULParser.ATTR_QUIRKS + "=\"").append(mount.getQuirkList("::"));
            }

            if (mount.isAnyMissingTroopers()) {
                output.append("\" " + MULParser.ATTR_TROOPER_MISS + "=\"").append(mount.getMissingTrooperString());
            }
        }

        if (isHit) {
            output.append("\" " + MULParser.ATTR_IS_HIT + "=\"").append(true);
        }

        if (!isRepairable && (isHit || isDestroyed)) {
            output.append("\" " + MULParser.ATTR_IS_REPAIRABLE + "=\"").append(false);
        }

        if (isMissing) {
            output.append("\" " + MULParser.ATTR_IS_MISSING + "=\"").append(true);
        }

        return output.append("\" " + MULParser.ATTR_IS_DESTROYED + "=\"")
              .append(isDestroyed)
              .append("\"/>\n")
              .toString();
    }

    /**
     * Helper function to indent based on an indent level
     */
    public static String indentStr(int level) {
        // Just redirect to the XML Util for now, and this will make it easy to find for
        // future replacement
        return MMXMLUtility.indentStr(level);
    }

    /**
     * Helper function that generates a string identifying the state of the locations for an entity.
     *
     * @param entity - the <code>Entity</code> whose location state is needed
     */
    public static String getLocString(Entity entity, int indentLvl) {
        boolean isMek = entity instanceof Mek;
        boolean isNonSmallCraftAero = (entity instanceof Aero) && !(entity instanceof SmallCraft);
        boolean haveSlot = false;
        StringBuilder output = new StringBuilder();
        StringBuilder thisLoc = new StringBuilder();
        boolean isDestroyed = false;

        // Walk through the locations for the entity,
        // and only record damage and ammo.
        for (int loc = 0; loc < entity.locations(); loc++) {
            // Aero.LOC_WINGS and Aero.LOC_FUSELAGE are not "real"
            // locations for the purpose of being destroyed or blown off,
            // but they may contain equipment which should be used below.
            boolean isPseudoLocation = isNonSmallCraftAero && (loc >= Aero.LOC_WINGS);

            // if the location is blown off, remove it so we can get the real
            // values
            boolean blownOff = entity.isLocationBlownOff(loc);

            // Record destroyed locations.
            if (!(entity instanceof Aero) &&
                  !entity.isConventionalInfantry() &&
                  (entity.getOInternal(loc) != IArmorState.ARMOR_NA) &&
                  (entity.getInternalForReal(loc) <= 0)) {
                isDestroyed = true;
            }

            // exact zeroes for BA should not be treated as destroyed as MHQ uses this to
            // signify
            // suits without pilots
            if (entity instanceof BattleArmor && entity.getInternalForReal(loc) >= 0) {
                isDestroyed = false;
            }

            if (isPseudoLocation) {
                isDestroyed = false;
                blownOff = false;
            }

            // Record damage to armor and internal structure.
            // Destroyed locations have lost all their armor and IS.
            if (!isDestroyed && !isPseudoLocation) {
                int currentArmor;
                if (entity instanceof BattleArmor) {
                    currentArmor = entity.getArmor(loc);
                } else {
                    currentArmor = entity.getArmorForReal(loc);
                }
                if (entity.getOArmor(loc) != currentArmor) {
                    thisLoc.append(indentStr(indentLvl + 1))
                          .append('<')
                          .append(MULParser.ELE_ARMOR)
                          .append(' ')
                          .append(MULParser.ATTR_POINTS)
                          .append("=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity.getArmorForReal(loc)));
                    thisLoc.append("\"/>\n");
                }

                if (entity.getOInternal(loc) != entity.getInternalForReal(loc)) {
                    thisLoc.append(indentStr(indentLvl + 1))
                          .append('<')
                          .append(MULParser.ELE_ARMOR)
                          .append(' ')
                          .append(MULParser.ATTR_POINTS)
                          .append("=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity.getInternalForReal(loc)));
                    thisLoc.append("\" " + MULParser.ATTR_TYPE + "=\"" + MULParser.VALUE_INTERNAL + "\"/>\n");
                }

                if (entity.hasRearArmor(loc) && (entity.getOArmor(loc, true) != entity.getArmorForReal(loc, true))) {
                    thisLoc.append(indentStr(indentLvl + 1))
                          .append('<')
                          .append(MULParser.ELE_ARMOR)
                          .append(' ')
                          .append(MULParser.ATTR_POINTS)
                          .append("=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity.getArmorForReal(loc, true)));
                    thisLoc.append("\" " + MULParser.ATTR_TYPE + "=\"" + MULParser.VALUE_REAR + "\"/>\n");
                }

                if (entity.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    thisLoc.append(indentStr(indentLvl + 1)).append('<').append(MULParser.ELE_BREACH).append("/>\n");
                }

                if (blownOff) {
                    thisLoc.append(indentStr(indentLvl + 1)).append('<').append(MULParser.ELE_BLOWN_OFF).append("/>\n");
                }
            }

            //
            Map<WeaponMounted, Integer> baySlotMap = new HashMap<>();

            // Walk through the slots in this location.
            for (int loop = 0; loop < entity.getNumberOfCriticalSlots(loc); loop++) {

                // Get this slot.
                CriticalSlot slot = entity.getCritical(loc, loop);

                // Did we get a slot?
                if (null == slot) {

                    // Nope. Record missing actuators on Biped Meks.
                    if (isMek &&
                          !entity.entityIsQuad() &&
                          ((loc == Mek.LOC_RIGHT_ARM) || (loc == Mek.LOC_LEFT_ARM)) &&
                          ((loop == 2) || (loop == 3))) {
                        thisLoc.append(indentStr(indentLvl + 1))
                              .append('<')
                              .append(MULParser.ELE_SLOT)
                              .append(' ')
                              .append(MULParser.ATTR_INDEX)
                              .append("=\"");
                        thisLoc.append(loop + 1);
                        thisLoc.append("\" " + MULParser.ATTR_TYPE + "=\"" + MULParser.VALUE_EMPTY + "\"/>\n");
                        haveSlot = true;
                    }

                } else {

                    // Yup. If the equipment isn't a system, get it.
                    Mounted<?> mount = null;
                    if (CriticalSlot.TYPE_EQUIPMENT == slot.getType()) {
                        mount = slot.getMount();
                    }

                    // if the "equipment" is a weapons bay,
                    // then let's make a note of it
                    if (entity.usesWeaponBays() &&
                          (mount instanceof WeaponMounted) &&
                          !((WeaponMounted) mount).getBayAmmo().isEmpty()) {
                        baySlotMap.put((WeaponMounted) slot.getMount(), loop + 1);
                    }

                    if ((mount != null) && (mount.getType() instanceof BombType)) {
                        continue;
                    }

                    // Destroyed locations on Meks that contain slots
                    // that are missing but not hit or destroyed must
                    // have been blown off.
                    if (!isDestroyed && isMek && slot.isMissing() && !slot.isHit() && !slot.isDestroyed()) {
                        thisLoc.append(EntityListFile.formatSlot(String.valueOf(loop + 1),
                              mount,
                              slot.isHit(),
                              slot.isDestroyed(),
                              slot.isRepairable(),
                              slot.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record damaged slots in undestroyed locations.
                    else if (!isDestroyed && slot.isDamaged()) {
                        thisLoc.append(EntityListFile.formatSlot(String.valueOf(loop + 1),
                              mount,
                              slot.isHit(),
                              slot.isDestroyed(),
                              slot.isRepairable(),
                              slot.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }

                    // record any quirks
                    else if ((null != mount) && (mount.countQuirks() > 0)) {
                        thisLoc.append(EntityListFile.formatSlot(String.valueOf(loop + 1),
                              mount,
                              slot.isHit(),
                              slot.isDestroyed(),
                              slot.isRepairable(),
                              slot.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record Rapid Fire Machine Guns
                    else if ((mount != null) && (mount.isRapidFire())) {
                        thisLoc.append(EntityListFile.formatSlot(String.valueOf(loop + 1),
                              mount,
                              slot.isHit(),
                              slot.isDestroyed(),
                              slot.isRepairable(),
                              slot.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record ammunition slots in undestroyed locations.
                    // N.B. the slot CAN'T be damaged at this point.
                    else if (!isDestroyed && (mount != null) && (mount.getType() instanceof AmmoType)) {

                        String bayIndex = "";

                        for (WeaponMounted bay : baySlotMap.keySet()) {
                            if (bay.ammoInBay(entity.getEquipmentNum(mount))) {
                                bayIndex = String.valueOf(baySlotMap.get(bay));
                            }
                        }

                        thisLoc.append(indentStr(indentLvl + 1))
                              .append('<')
                              .append(MULParser.ELE_SLOT)
                              .append(' ')
                              .append(MULParser.ATTR_INDEX)
                              .append("=\"");
                        thisLoc.append(loop + 1);
                        thisLoc.append("\" " + MULParser.ATTR_TYPE + "=\"");
                        thisLoc.append(mount.getType().getInternalName());
                        thisLoc.append("\" " + MULParser.ATTR_SHOTS + "=\"");
                        thisLoc.append(mount.getBaseShotsLeft());

                        if (!bayIndex.isEmpty()) {
                            thisLoc.append("\" " + MULParser.ATTR_WEAPONS_BAY_INDEX + "=\"");
                            thisLoc.append(bayIndex);
                        }

                        thisLoc.append("\"/>\n");
                        haveSlot = true;
                    }

                    // Record the munition type of one shot launchers
                    // and the ammunition shots of small SV weapons
                    else if (!isDestroyed &&
                          (mount != null) &&
                          (mount.getType() instanceof WeaponType) &&
                          ((mount.getType()).hasFlag(WeaponType.F_ONE_SHOT) ||
                                (entity.isSupportVehicle() && (mount.getType() instanceof InfantryWeapon)))) {
                        thisLoc.append(EntityListFile.formatSlot(String.valueOf(loop + 1),
                              mount,
                              slot.isHit(),
                              slot.isDestroyed(),
                              slot.isRepairable(),
                              slot.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record trooper missing equipment on BattleArmor
                    else if (null != mount && mount.isAnyMissingTroopers()) {
                        thisLoc.append(EntityListFile.formatSlot(String.valueOf(loop + 1),
                              mount,
                              slot.isHit(),
                              slot.isDestroyed(),
                              slot.isRepairable(),
                              slot.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }

                } // End have-slot

            } // Check the next slot in this location

            // Stabilizer hit
            if ((entity instanceof Tank) && ((Tank) entity).isStabiliserHit(loc)) {
                thisLoc.append(indentStr(indentLvl + 1))
                      .append('<')
                      .append(MULParser.ELE_STABILIZER)
                      .append(' ')
                      .append(MULParser.ATTR_IS_HIT)
                      .append("=\"true\"/>\n");
            }

            // ProtoMeks only have system slots,
            // so we have to handle the ammo specially.
            if (entity instanceof ProtoMek) {
                for (Mounted<?> mount : entity.getAmmo()) {
                    // Is this ammo in the current location?
                    if (mount.getLocation() == loc) {
                        thisLoc.append(EntityListFile.formatSlot(MULParser.VALUE_NA,
                              mount,
                              mount.isHit(),
                              mount.isDestroyed(),
                              mount.isRepairable(),
                              mount.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }
                } // Check the next ammo.
                // TODO: handle slotless equipment.
            } // End is-proto

            // GunEmplacements don't have system slots,
            // so we have to handle the ammo specially.
            if (entity instanceof GunEmplacement) {
                for (Mounted<?> mount : entity.getEquipment()) {
                    // Is this ammo in the current location?
                    if (mount.getLocation() == loc) {
                        thisLoc.append(EntityListFile.formatSlot(MULParser.VALUE_NA,
                              mount,
                              mount.isHit(),
                              mount.isDestroyed(),
                              mount.isRepairable(),
                              mount.isMissing(),
                              indentLvl + 1));
                        haveSlot = true;
                    }
                } // Check the next ammo.
                // TODO: handle slotless equipment.
            } // End is-ge

            // Did we record information for this location?
            if (!thisLoc.isEmpty()) {

                // Add this location to the output string.
                output.append(indentStr(indentLvl))
                      .append('<')
                      .append(MULParser.ELE_LOCATION)
                      .append(' ')
                      .append(MULParser.ATTR_INDEX)
                      .append("=\"");
                output.append(loc);
                if (isDestroyed) {
                    output.append("\" " + MULParser.ATTR_IS_DESTROYED + "=\"true");
                }
                output.append("\"> ");
                output.append(entity.getLocationName(loc));
                if (blownOff) {
                    output.append(" has been blown off.");
                }
                output.append('\n');
                output.append(thisLoc);
                output.append(indentStr(indentLvl)).append("</").append(MULParser.ELE_LOCATION).append(">\n");

                // Reset the location buffer.
                thisLoc = new StringBuilder();
                blownOff = false;

            } // End output-location

            // If the location is completely destroyed, log it anyway.
            else if (isDestroyed) {

                // Add this location to the output string.
                output.append(indentStr(indentLvl))
                      .append('<')
                      .append(MULParser.ELE_LOCATION)
                      .append(' ')
                      .append(MULParser.ATTR_INDEX)
                      .append("=\"");
                output.append(loc);
                output.append("\" " + MULParser.ATTR_IS_DESTROYED + "=\"true\" /> ");
                output.append(entity.getLocationName(loc));
                output.append('\n');

            } // End location-completely-destroyed

            // Reset the "location is destroyed" flag.
            isDestroyed = false;

        } // Handle the next location

        // If there is no location string, return a null.
        if (output.isEmpty()) {
            return null;
        }

        // If we recorded a slot, remind the player that slots start at 1.
        if (haveSlot) {
            output.insert(0, '\n');
            output.insert(0, "      The first slot in a location is at index=\"1\".");

            // Tanks do weird things with ammo.
            if (entity instanceof Tank) {
                output.insert(0, '\n');
                output.insert(0, "      Tanks have special needs, so don't delete any ammo slots.");
            }
        }

        // Convert the output into a String and return it.
        return output.toString();

    } // End private static String getLocString( Entity )

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <br><br>
     * The <code>Entity</code>'s pilots, damage, ammo loads, ammo usage, and other campaign-related information are
     * retained, but data specific to a particular game is ignored. This method is a simpler version of the overloaded
     * method {@code saveTo}, with a default generic battle value of 0 (this causes GBV to be ignored), and with unit
     * embedding off.
     *
     * @param file - The current contents of the file will be discarded and all
     *             <code>Entity</code>s in the list will be written to the file.
     * @param list - An <code>ArrayList</code> containing <code>Entity</code>s to be stored in a file.
     *
     * @throws IOException - Is thrown on any error.
     */
    public static void saveTo(File file, ArrayList<Entity> list) throws IOException {
        saveTo(file, list, 0, false);
    }

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <br><br>
     * The <code>Entity</code>'s pilots, damage, ammo loads, ammo usage, and other campaign-related information are
     * retained, but data specific to a particular game is ignored. This method is a simpler version of the overloaded
     * method {@code saveTo}, with a default generic battle value of 0 (this causes GBV to be ignored).
     *
     * @param file       - The current contents of the file will be discarded and all
     *                   <code>Entity</code>s in the list will be written to the file.
     * @param list       - An <code>ArrayList</code> containing <code>Entity</code>s to be stored in a file.
     * @param embedUnits - Set to <code>true</code> to embed the unit file of custom units (blk/mtf data) into the file.
     *                   This allows the resulting file to be loaded by someone who doesn't have those custom units
     *                   available.
     *
     * @throws IOException - Is thrown on any error.
     */
    public static void saveTo(File file, ArrayList<Entity> list, boolean embedUnits) throws IOException {
        saveTo(file, list, 0, embedUnits);
    }

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <br><br>
     * The <code>Entity</code>'s pilots, damage, ammo loads, ammo usage, and other campaign-related information are
     * retained, but data specific to a particular game is ignored. Unit embedding is off, see
     * {@link #saveTo(File, ArrayList, int, boolean) the overloaded version of this function}
     *
     * @param file               - The current contents of the file will be discarded and all
     *                           <code>Entity</code>s in the list will be written to the file.
     * @param list               - A <code>ArrayList</code> containing <code>Entity</code>s to be stored in a file.
     * @param genericBattleValue - An <code>Integer</code> representing the generic battle value. If it is greater than
     *                           0, it will be written into the XML.
     *
     * @throws IOException - Is thrown on any error.
     */
    public static void saveTo(File file, ArrayList<Entity> list, int genericBattleValue) throws IOException {
        saveTo(file, list, genericBattleValue, false);
    }

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <br><br>
     * The <code>Entity</code>'s pilots, damage, ammo loads, ammo usage, and other campaign-related information are
     * retained, but data specific to a particular game is ignored.
     *
     * @param file               - The current contents of the file will be discarded and all
     *                           <code>Entity</code>s in the list will be written to the file.
     * @param list               - A <code>ArrayList</code> containing <code>Entity</code>s to be stored in a file.
     * @param genericBattleValue - An <code>Integer</code> representing the generic battle value. If it is greater than
     *                           0, it will be written into the XML.
     * @param embedUnits         - Set to <code>true</code> to embed the unit file of custom units (blk/mtf data) into
     *                           the file. This allows the resulting file to be loaded by someone who doesn't have those
     *                           custom units available.
     *
     * @throws IOException - Is thrown on any error.
     */
    public static void saveTo(File file, ArrayList<Entity> list, int genericBattleValue, boolean embedUnits)
          throws IOException {
        // Open up the file. Produce UTF-8 output.
        Writer output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

        // Output the doctype and header stuff.
        output.write("<?xml " + MULParser.VERSION + "=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        output.write('<' + MULParser.ELE_UNIT + ' ' + MULParser.VERSION + "=\"" + MMConstants.VERSION + "\" >\n\n");

        if (genericBattleValue > 0) {
            output.write("<TotalGenericBattleValue>" + genericBattleValue + "</TotalGenericBattleValue>\n\n");
        }

        writeEntityList(output, list, embedUnits);

        // Finish writing.
        output.write("</" + MULParser.ELE_UNIT + ">\n");
        output.flush();
        output.close();
    }

    /**
     * Save the entities from the game of client to the given file. This will create separate sections for salvage,
     * devastated, and ejected crews in addition to the surviving units
     * <br><br>
     * The <code>Entity</code>s pilots, damage, ammo loads, ammo usage, and other campaign-related information are
     * retained but data specific to a particular game is ignored.
     *
     * @param file   - The current contents of the file will be discarded and all
     *               <code>Entity</code>s in the list will be written to the file.
     * @param client - a <code>Client</code> containing the <code>Game</code>s to be used
     *
     * @throws IOException is thrown on any error.
     */
    public static void saveTo(File file, Client client) throws IOException {
        saveTo(file, client, client.getLocalPlayer());
    }

    /**
     * Save the entities from the game of client to the given file. This will create separate sections for salvage,
     * devastated, and ejected crews in addition to the surviving units
     * <br><br>
     * The <code>Entity</code>s pilots, damage, ammo loads, ammo usage, and other campaign-related information are
     * retained but data specific to a particular game is ignored.
     *
     * @param file        - The current contents of the file will be discarded and all
     *                    <code>Entity</code>s in the list will be written to the file.
     * @param client      - a <code>Client</code> containing the <code>Game</code>s to be used
     * @param localPlayer - What player should we treat as "the" player?
     *
     * @throws IOException is thrown on any error.
     */
    public static void saveTo(File file, Client client, Player localPlayer) throws IOException {
        if (null == client.getGame() || !client.playerExists(localPlayer.getId())) {
            return;
        }

        // Open up the file. Produce UTF-8 output.
        Writer output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

        // Output the doctype and header stuff.
        output.write("<?xml " + MULParser.VERSION + "=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        output.write('<' + MULParser.ELE_RECORD + ' ' + MULParser.VERSION + "=\"" + MMConstants.VERSION + "\" >");

        ArrayList<Entity> living = new ArrayList<>();
        ArrayList<Entity> allied = new ArrayList<>();
        ArrayList<Entity> salvage = new ArrayList<>();
        ArrayList<Entity> retreated = new ArrayList<>();
        ArrayList<Entity> devastated = new ArrayList<>();
        Hashtable<String, String> kills = new Hashtable<>();

        // Sort entities into player's, enemies, and allies and add to survivors,
        // salvage, and allies.
        for (Entity entity : client.getGame().inGameTWEntities()) {
            if (entity.getOwner().getId() == localPlayer.getId()) {
                living.add(entity);
            } else if (entity.getOwner().isEnemyOf(localPlayer)) {
                if (!entity.canEscape()) {
                    kills.put(entity.getDisplayName(), MULParser.VALUE_NONE);
                }
                salvage.add(entity);
            } else {
                allied.add(entity);
            }
        }

        // Be sure to include all units that have retreated in survivor and allied
        // sections
        for (Enumeration<Entity> iter = client.getGame().getRetreatedEntities(); iter.hasMoreElements(); ) {
            Entity ent = iter.nextElement();
            if (ent.getOwner().getId() == localPlayer.getId()) {
                living.add(ent);
            } else if (!ent.getOwner().isEnemyOf(localPlayer)) {
                allied.add(ent);
            } else {
                retreated.add(ent);
            }
        }

        // salvageable stuff
        Enumeration<Entity> graveyard = client.getGame().getGraveyardEntities();
        while (graveyard.hasMoreElements()) {
            Entity entity = graveyard.nextElement();
            if (entity.getOwner().isEnemyOf(localPlayer)) {
                Entity killer = client.getGame().getEntityFromAllSources(entity.getKillerId());
                if (null != killer && !killer.getExternalIdAsString().equals("-1")) {
                    kills.put(entity.getDisplayName(), killer.getExternalIdAsString());
                } else {
                    kills.put(entity.getDisplayName(), MULParser.VALUE_NONE);
                }
            }
            salvage.add(entity);
        }

        // devastated units
        Enumeration<Entity> devastation = client.getGame().getDevastatedEntities();
        while (devastation.hasMoreElements()) {
            Entity entity = devastation.nextElement();
            if (entity.getOwner().isEnemyOf(localPlayer)) {
                Entity killer = client.getGame().getEntityFromAllSources(entity.getKillerId());
                if (null != killer && !killer.getExternalIdAsString().equals("-1")) {
                    kills.put(entity.getDisplayName(), killer.getExternalIdAsString());
                } else {
                    kills.put(entity.getDisplayName(), MULParser.VALUE_NONE);
                }
            }
            devastated.add(entity);
        }

        if (!living.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + '<' + MULParser.ELE_SURVIVORS + ">\n\n");
            writeEntityList(output, living);
            output.write(indentStr(1) + "</" + MULParser.ELE_SURVIVORS + ">\n");
        }

        if (!allied.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + '<' + MULParser.ELE_ALLIES + ">\n\n");
            writeEntityList(output, allied);
            output.write(indentStr(1) + "</" + MULParser.ELE_ALLIES + ">\n");
        }

        if (!salvage.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + '<' + MULParser.ELE_SALVAGE + ">\n\n");
            writeEntityList(output, salvage);
            output.write(indentStr(1) + "</" + MULParser.ELE_SALVAGE + ">\n");
        }

        if (!retreated.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + '<' + MULParser.ELE_RETREATED + ">\n\n");
            writeEntityList(output, retreated);
            output.write(indentStr(1) + "</" + MULParser.ELE_RETREATED + ">\n");
        }

        if (!devastated.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + '<' + MULParser.ELE_DEVASTATED + ">\n\n");
            writeEntityList(output, devastated);
            output.write(indentStr(1) + "</" + MULParser.ELE_DEVASTATED + ">\n");
        }

        if (!kills.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + '<' + MULParser.ELE_KILLS + ">\n\n");
            writeKills(output, kills);
            output.write(indentStr(1) + "</" + MULParser.ELE_KILLS + ">\n");
        }

        // Finish writing.
        output.write("</" + MULParser.ELE_RECORD + ">\n");
        output.flush();
        output.close();
    }

    private static void writeKills(Writer output, Hashtable<String, String> kills) throws IOException {
        int indentLvl = 2;
        for (String killed : kills.keySet()) {
            output.write(indentStr(indentLvl) + '<' + MULParser.ELE_KILL + ' ' + MULParser.ATTR_KILLED + "=\"");
            output.write(killed.replaceAll("\"", "&quot;"));
            output.write("\" " + MULParser.ATTR_KILLER + "=\"");
            output.write(kills.get(killed));
            output.write("\"/>\n");
        }
    }

    public static void writeEntityList(Writer output, ArrayList<Entity> list) throws IOException {
        writeEntityList(output, list, false);
    }

    public static void writeEntityList(Writer output, ArrayList<Entity> list, boolean embedUnits) throws IOException {
        // Walk through the list of entities.
        for (Entity entity : list) {
            int indentLvl = 2;

            // Start writing this entity to the file.
            output.write(indentStr(indentLvl) + '<' + MULParser.ELE_ENTITY + ' ' + MULParser.ATTR_CHASSIS + "=\"");
            output.write(entity.getFullChassis().replaceAll("\"", "&quot;"));
            output.write("\" " + MULParser.ATTR_MODEL + "=\"");
            output.write(entity.getModel().replaceAll("\"", "&quot;"));
            output.write("\" " + MULParser.ATTR_TYPE + "=\"");
            output.write((entity instanceof FighterSquadron) ? MULParser.VALUE_SQUADRON : entity.getMovementModeAsString());
            output.write("\" " + MULParser.ATTR_COMMANDER + "=\"");
            output.write(String.valueOf(entity.isCommander()));
            output.write("\" " + MULParser.ATTR_OFFBOARD + "=\"");
            output.write(String.valueOf(entity.isOffBoard()));
            if (entity.isOffBoard()) {
                output.write("\" " + MULParser.ATTR_OFFBOARD_DISTANCE + "=\"");
                output.write(String.valueOf(entity.getOffBoardDistance()));
                output.write("\" " + MULParser.ATTR_OFFBOARD_DIRECTION + "=\"");
                output.write(String.valueOf(entity.getOffBoardDirection().getValue()));
            }
            output.write("\" " + MULParser.ATTR_HIDDEN + "=\"");
            output.write(String.valueOf(entity.isHidden()));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT + "=\"");
            output.write(String.valueOf(entity.getDeployRound()));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT_ZONE + "=\"");
            output.write(String.valueOf(entity.getStartingPos(false)));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT_ZONE_WIDTH + "=\"");
            output.write(String.valueOf(entity.getStartingWidth(false)));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT_ZONE_OFFSET + "=\"");
            output.write(String.valueOf(entity.getStartingOffset(false)));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT_ZONE_ANY_NWX + "=\"");
            output.write(String.valueOf(entity.getStartingAnyNWx(false)));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT_ZONE_ANY_NWY + "=\"");
            output.write(String.valueOf(entity.getStartingAnyNWy(false)));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT_ZONE_ANY_SEX + "=\"");
            output.write(String.valueOf(entity.getStartingAnySEx(false)));
            output.write("\" " + MULParser.ATTR_DEPLOYMENT_ZONE_ANY_SEY + "=\"");
            output.write(String.valueOf(entity.getStartingAnySEy(false)));
            output.write("\" " + MULParser.ATTR_NEVER_DEPLOYED + "=\"");
            output.write(String.valueOf(entity.wasNeverDeployed()));
            if (entity.isAero()) {
                output.write("\" " + MULParser.ATTR_VELOCITY + "=\"");
                output.write(((IAero) entity).getCurrentVelocity() + "");
                output.write("\" " + MULParser.ATTR_ALTITUDE + "=\"");
                output.write(entity.getAltitude() + "");
            }
            if (entity instanceof VTOL) {
                output.write("\" " + MULParser.ATTR_ELEVATION + "=\"");
                output.write(entity.getElevation() + "");
            }
            if (!entity.getExternalIdAsString().equals("-1")) {
                output.write("\" " + MULParser.ATTR_EXT_ID + "=\"");
                output.write(entity.getExternalIdAsString());
            }
            if (entity.countQuirks() > 0) {
                output.write("\" " + MULParser.ATTR_QUIRKS + "=\"");
                output.write(String.valueOf(entity.getQuirkList("::")));
            }
            if ((entity.getGame() != null) && (entity.getC3Master() != null)) {
                Entity entityC3Master = entity.getGame().getEntity(entity.getC3Master().getId());
                if (entityC3Master != null && entityC3Master.getC3UUIDAsString() != null) {
                    output.write("\" " + MULParser.ATTR_C3_MASTER_IS + "=\"");
                    output.write(entityC3Master.getC3UUIDAsString());
                }
            }
            if (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3() || entity.hasNovaCEWS()) {
                if (entity.getC3UUIDAsString() != null) {
                    output.write("\" " + MULParser.ATTR_C3UUID + "=\"");
                    output.write(entity.getC3UUIDAsString());
                }
            }
            if (!entity.getCamouflage().hasDefaultCategory()) {
                output.write("\" " + MULParser.ATTR_CAMO_CATEGORY + "=\"");
                output.write(entity.getCamouflage().getCategory());
            }
            if (!entity.getCamouflage().hasDefaultFilename()) {
                output.write("\" " + MULParser.ATTR_CAMO_FILENAME + "=\"");
                output.write(entity.getCamouflage().getFilename());
            }
            if (!entity.getCamouflage().hasDefaultCategory()) {
                output.write("\" " + MULParser.ATTR_CAMO_ROTATION + "=\"");
                output.write(Integer.toString(entity.getCamouflage().getRotationAngle()));
                output.write("\" " + MULParser.ATTR_CAMO_SCALE + "=\"");
                output.write(Integer.toString(entity.getCamouflage().getScale()));
            }

            if ((entity instanceof MekWarrior) &&
                  !((MekWarrior) entity).getPickedUpByExternalIdAsString().equals("-1")) {
                output.write("\" " + MULParser.ATTR_PICKUP_ID + "=\"");
                output.write(((MekWarrior) entity).getPickedUpByExternalIdAsString());
            }

            // Save some values for conventional infantry
            if (entity.isConventionalInfantry() && entity instanceof Infantry infantry) {
                if (infantry.getCustomArmorDamageDivisor() != 1) {
                    output.write("\" " + MULParser.ATTR_ARMOR_DIVISOR + "=\"");
                    output.write(infantry.getCustomArmorDamageDivisor() + "");
                }
                if (infantry.isArmorEncumbering()) {
                    output.write("\" " + MULParser.ATTR_ARMOR_ENC + "=\"1");
                }
                if (infantry.hasSpaceSuit()) {
                    output.write("\" " + MULParser.ATTR_SPACESUIT + "=\"1");
                }
                if (infantry.hasDEST()) {
                    output.write("\" " + MULParser.ATTR_DEST_ARMOR + "=\"1");
                }
                if (infantry.hasSneakCamo()) {
                    output.write("\" " + MULParser.ATTR_SNEAK_CAMO + "=\"1");
                }
                if (infantry.hasSneakIR()) {
                    output.write("\" " + MULParser.ATTR_SNEAK_IR + "=\"1");
                }
                if (infantry.hasSneakECM()) {
                    output.write("\" " + MULParser.ATTR_SNEAK_ECM + "=\"1");
                }
                if (infantry.getSpecializations() > 0) {
                    output.write("\" " + MULParser.ATTR_INF_SPEC + "=\"");
                    output.write(infantry.getSpecializations() + "");
                }
            }
            output.write("\">\n");

            // Add the crew this entity.
            final Crew crew = entity.getCrew();
            if (crew.getSlotCount() > 1) {
                output.write(indentStr(indentLvl + 1) +
                      '<' +
                      MULParser.ELE_CREW +
                      ' ' +
                      MULParser.ATTR_CREW_TYPE +
                      "=\"");
                output.write(crew.getCrewType().toString().toLowerCase());
                writeCrewAttributes(output, entity, crew);
                output.write("\">\n");

                for (int pos = 0; pos < crew.getSlotCount(); pos++) {
                    if (crew.isMissing(pos)) {
                        continue;
                    }
                    output.write(indentStr(indentLvl + 2) +
                          '<' +
                          MULParser.ELE_CREWMEMBER +
                          ' ' +
                          MULParser.ATTR_SLOT +
                          "=\"" +
                          pos);
                    writePilotAttributes(output, entity, crew, pos);
                    output.write("\"/>\n");
                }
                output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_CREW + '>');
            } else {
                output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_PILOT + ' ' + MULParser.ATTR_SIZE + "=\"");
                output.write(String.valueOf(crew.getSize()));
                writePilotAttributes(output, entity, crew, 0);
                writeCrewAttributes(output, entity, crew);
                output.write("\"/>");
            }
            output.write("\n");

            // If it's a tank, add a movement tag.
            if (entity instanceof Tank tankEntity) {
                output.write(EntityListFile.getMovementString(tankEntity));
                if (tankEntity.isTurretLocked(tankEntity.getLocTurret())) {
                    output.write(EntityListFile.getTurretLockedString(tankEntity));
                }
                // crits
                output.write(EntityListFile.getTankCritString(tankEntity));
            }

            // Aero stuff that also applies to LAMs
            if (entity instanceof IAero a) {
                // fuel
                output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_FUEL + ' ' + MULParser.ATTR_LEFT + "=\"");
                output.write(String.valueOf(a.getCurrentFuel()));
                output.write("\"/>\n");
            }

            // Write the Bomb Data if needed
            if (entity.isBomber()) {
                IBomber b = (IBomber) entity;
                BombLoadout intBombChoices = b.getIntBombChoices();
                BombLoadout extBombChoices = b.getExtBombChoices();
                if (intBombChoices.getTotalBombs() > 0 || extBombChoices.getTotalBombs() > 0) {
                    output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_BOMBS + ">\n");
                    for (Map.Entry<BombTypeEnum, Integer> entry : intBombChoices.entrySet()) {
                        BombTypeEnum bombType = entry.getKey();
                        int count = entry.getValue();
                        if (count > 0) {
                            output.write(indentStr(indentLvl + 2) +
                                  '<' +
                                  MULParser.ELE_BOMB +
                                  ' ' +
                                  MULParser.ATTR_TYPE +
                                  "=\"");
                            output.write(bombType.getInternalName());
                            output.write("\" " + MULParser.ATTR_LOAD + "=\"");
                            output.write(String.valueOf(count));
                            output.write("\" " + MULParser.ATTR_INTERNAL + "=\"true");
                            output.write("\"/>\n");
                        }
                    }
                    for (Map.Entry<BombTypeEnum, Integer> entry : extBombChoices.entrySet()) {
                        BombTypeEnum bombType = entry.getKey();
                        int count = entry.getValue();
                        if (count > 0) {
                            output.write(indentStr(indentLvl + 2) +
                                  '<' +
                                  MULParser.ELE_BOMB +
                                  ' ' +
                                  MULParser.ATTR_TYPE +
                                  "=\"");
                            output.write(bombType.getInternalName());
                            output.write("\" " + MULParser.ATTR_LOAD + "=\"");
                            output.write(String.valueOf(count));
                            output.write("\" " + MULParser.ATTR_INTERNAL + "=\"false");
                            output.write("\"/>\n");
                        }
                    }
                    for (Mounted<?> m : b.getBombs()) {
                        if (!(m.getType() instanceof BombType)) {
                            continue;
                        }
                        output.write(indentStr(indentLvl + 2) +
                              '<' +
                              MULParser.ELE_BOMB +
                              ' ' +
                              MULParser.ATTR_TYPE +
                              "=\"");
                        output.write(m.getType().getShortName());
                        output.write("\" " + MULParser.ATTR_LOAD + "=\"");
                        output.write(String.valueOf(m.getBaseShotsLeft()));
                        output.write("\" " + MULParser.ATTR_INTERNAL + "=\"");
                        output.write(String.valueOf(m.isInternalBomb()));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_BOMBS + ">\n");
                }
            }

            // aero stuff that does not apply to LAMs
            if (entity instanceof Aero a) {

                // SI
                output.write(indentStr(indentLvl + 1) +
                      '<' +
                      MULParser.ELE_SI +
                      ' ' +
                      MULParser.ATTR_INTEGRITY +
                      "=\"");
                output.write(String.valueOf(a.getSI()));
                output.write("\"/>\n");

                // heat sinks
                output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_HEAT + ' ' + MULParser.ATTR_SINK + "=\"");
                output.write(String.valueOf(a.getHeatSinks()));
                output.write("\"/>\n");

                // large craft bays and doors.
                if ((a instanceof Dropship) || (a instanceof Jumpship)) {
                    for (Bay nextbay : a.getTransportBays()) {
                        output.write(indentStr(indentLvl + 1) +
                              '<' +
                              MULParser.ELE_BAY +
                              ' ' +
                              MULParser.ATTR_INDEX +
                              "=\"" +
                              nextbay.getBayNumber() +
                              "\">\n");
                        output.write(indentStr(indentLvl + 2) +
                              '<' +
                              MULParser.ELE_BAY_DAMAGE +
                              '>' +
                              nextbay.getBayDamage() +
                              "</" +
                              MULParser.ELE_BAY_DAMAGE +
                              ">\n");
                        output.write(indentStr(indentLvl + 2) +
                              '<' +
                              MULParser.ELE_BAY_DOORS +
                              '>' +
                              nextbay.getCurrentDoors() +
                              "</" +
                              MULParser.ELE_BAY_DOORS +
                              ">\n");
                        for (Entity e : nextbay.getLoadedUnits()) {
                            output.write(indentStr(indentLvl + 2) +
                                  '<' +
                                  MULParser.ELE_LOADED +
                                  '>' +
                                  e.getId() +
                                  "</" +
                                  MULParser.ELE_LOADED +
                                  ">\n");
                        }
                        output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_BAY + ">\n");
                    }
                }

                // jumpship, warship and space station stuff
                if (a instanceof Jumpship j) {

                    // kf integrity
                    output.write(indentStr(indentLvl + 1) +
                          '<' +
                          MULParser.ELE_KF +
                          ' ' +
                          MULParser.ATTR_INTEGRITY +
                          "=\"");
                    output.write(String.valueOf(j.getKFIntegrity()));
                    output.write("\"/>\n");

                    // kf sail integrity
                    output.write(indentStr(indentLvl + 1) +
                          '<' +
                          MULParser.ELE_SAIL +
                          ' ' +
                          MULParser.ATTR_INTEGRITY +
                          "=\"");
                    output.write(String.valueOf(j.getSailIntegrity()));
                    output.write("\"/>\n");
                }

                // general aero crits
                output.write(EntityListFile.getAeroCritString(a));

                // dropship only crits
                if (a instanceof Dropship d) {
                    output.write(EntityListFile.getDropshipCritString(d));
                }

            }

            if (entity instanceof BattleArmor ba) {
                for (Mounted<?> m : entity.getEquipment()) {
                    if (m.getType().hasFlag(MiscType.F_BA_MEA)) {
                        Mounted<?> manipulator = null;
                        if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LEFT_ARM) {
                            manipulator = ba.getLeftManipulator();
                        } else if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RIGHT_ARM) {
                            manipulator = ba.getRightManipulator();
                        }
                        output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_BA_MEA + ' ');
                        output.write(MULParser.ATTR_BA_MEA_MOUNT_LOC + "=\"" + m.getBaMountLoc() + "\" ");
                        if (manipulator != null) {
                            output.write(MULParser.ATTR_BA_MEA_TYPE_NAME +
                                  "=\"" +
                                  manipulator.getType().getInternalName() +
                                  "\" ");
                        }
                        output.write("/>\n");
                    } else if (m.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                        int mountIdx = entity.getEquipmentNum(m);
                        EquipmentType apType = null;
                        if (m.getLinked() != null) {
                            apType = m.getLinked().getType();
                        }
                        output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_BA_APM + ' ');
                        output.write(MULParser.ATTR_BA_APM_MOUNT_NUM + "=\"" + mountIdx + "\" ");
                        if (apType != null) {
                            output.write(MULParser.ATTR_BA_APM_TYPE_NAME + "=\"" + apType.getInternalName() + "\" ");
                        }
                        output.write("/>\n");
                    }
                }
            }

            // Add the locations of this entity (if any are needed).
            String loc = EntityListFile.getLocString(entity, indentLvl + 1);
            if (null != loc) {
                output.write(loc);
            }

            // Write the C3i Data if needed
            if (entity.hasC3i()) {
                output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_C3I + ">\n");
                for (Entity C3iEntity : list) {
                    if ((C3iEntity.getC3UUIDAsString() != null) && C3iEntity.onSameC3NetworkAs(entity, true)) {
                        output.write(indentStr(indentLvl + 1) +
                              '<' +
                              MULParser.ELE_C3I_LINK +
                              ' ' +
                              MULParser.ATTR_LINK +
                              "=\"");
                        output.write(C3iEntity.getC3UUIDAsString());
                        output.write("\"/>\n");
                    }
                }
                output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_C3I + ">\n");
            }

            // Write the NC3 Data if needed
            if (entity.hasNavalC3() || entity.hasNovaCEWS()) {
                logger.debug("[EntityListFile] Saving NC3 for entity {} ({}), hasNavalC3={}, hasNovaCEWS={}",
                    entity.getId(), entity.getShortName(), entity.hasNavalC3(), entity.hasNovaCEWS());
                output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_NC3 + ">\n");
                int linkCount = 0;
                for (Entity NC3Entity : list) {
                    if ((NC3Entity.getC3UUIDAsString() != null) && NC3Entity.onSameC3NetworkAs(entity, true)) {
                        logger.debug("[EntityListFile]   Writing NC3LINK for entity {} UUID: {}",
                            NC3Entity.getId(), NC3Entity.getC3UUIDAsString());
                        output.write(indentStr(indentLvl + 1) +
                              '<' +
                              MULParser.ELE_NC3LINK +
                              ' ' +
                              MULParser.ATTR_LINK +
                              "=\"");
                        output.write(NC3Entity.getC3UUIDAsString());
                        output.write("\"/>\n");
                        linkCount++;
                    }
                }
                logger.debug("[EntityListFile] Saved {} NC3 links for entity {}", linkCount, entity.getId());
                output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_NC3 + ">\n");
            }

            // Record if this entity is transported by another
            if (entity.getTransportId() != Entity.NONE) {
                output.write(indentStr(indentLvl + 1) +
                      '<' +
                      MULParser.ELE_CONVEYANCE +
                      ' ' +
                      MULParser.ATTR_ID +
                      "=\"" +
                      entity.getTransportId());
                output.write("\"/>\n");
            }
            // Record this unit's id number
            if (entity.getId() != Entity.NONE) {
                output.write(indentStr(indentLvl + 1) +
                      '<' +
                      MULParser.ELE_GAME +
                      ' ' +
                      MULParser.ATTR_ID +
                      "=\"" +
                      entity.getId());
                output.write("\"/>\n");
            }

            // Write the force hierarchy
            if (!entity.getForceString().isBlank()) {
                output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_FORCE + ' ' + MULParser.ATTR_FORCE + "=\"");
                output.write(entity.getForceString());
                output.write("\"/>\n");
            } else if ((entity.getGame() != null) && (entity.getForceId() != Force.NO_FORCE)) {
                output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_FORCE + ' ' + MULParser.ATTR_FORCE + "=\"");
                output.write(entity.getGame().getForces().forceStringFor(entity));
                output.write("\"/>\n");
            }

            // Write the escape craft data, if needed
            if (entity instanceof Aero aero) {
                if (!aero.getEscapeCraft().isEmpty()) {
                    for (String id : aero.getEscapeCraft()) {
                        output.write(indentStr(indentLvl + 1) +
                              '<' +
                              MULParser.ELE_ESCAPE_CRAFT +
                              ' ' +
                              MULParser.ATTR_ID +
                              "=\"" +
                              id);
                        output.write("\"/>\n");
                    }
                }
            }

            if (entity instanceof SmallCraft craft) {
                if (!craft.getNOtherCrew().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_ESCAPE_CREW + ">\n");
                    for (String id : craft.getNOtherCrew().keySet()) {
                        output.write(indentStr(indentLvl + 2) +
                              '<' +
                              MULParser.ELE_SHIP +
                              ' ' +
                              MULParser.ATTR_ID +
                              "=\"" +
                              id +
                              '"' +
                              ' ' +
                              MULParser.ATTR_NUMBER +
                              "=\"" +
                              craft.getNOtherCrew().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCAPE_CREW + ">\n");
                }

                if (!craft.getPassengers().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_ESCAPE_PASSENGERS + ">\n");
                    for (String id : craft.getPassengers().keySet()) {
                        output.write(indentStr(indentLvl + 2) +
                              '<' +
                              MULParser.ELE_SHIP +
                              ' ' +
                              MULParser.ATTR_ID +
                              "=\"" +
                              id +
                              '"' +
                              ' ' +
                              MULParser.ATTR_NUMBER +
                              "=\"" +
                              craft.getPassengers().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCAPE_PASSENGERS + ">\n");
                }
                if (craft instanceof EscapePods) {
                    // Original number of pods, used to set the strength of a group of pods
                    output.write(indentStr(indentLvl + 1) +
                          '<' +
                          MULParser.ELE_ORIG_PODS +
                          ' ' +
                          MULParser.ATTR_NUMBER +
                          "=\"" +
                          craft.getOSI());
                    output.write("\"/>\n");
                }

            } else if (entity instanceof EjectedCrew eCrew) {
                if (!eCrew.getNOtherCrew().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_ESCAPE_CREW + ">\n");
                    for (String id : eCrew.getNOtherCrew().keySet()) {
                        output.write(indentStr(indentLvl + 2) +
                              '<' +
                              MULParser.ELE_SHIP +
                              ' ' +
                              MULParser.ATTR_ID +
                              "=\"" +
                              id +
                              '"' +
                              ' ' +
                              MULParser.ATTR_NUMBER +
                              "=\"" +
                              eCrew.getNOtherCrew().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCAPE_CREW + ">\n");
                }

                if (!eCrew.getPassengers().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + '<' + MULParser.ELE_ESCAPE_PASSENGERS + ">\n");
                    for (String id : eCrew.getPassengers().keySet()) {
                        output.write(indentStr(indentLvl + 2) +
                              '<' +
                              MULParser.ELE_SHIP +
                              ' ' +
                              MULParser.ATTR_ID +
                              "=\"" +
                              id +
                              '"' +
                              ' ' +
                              MULParser.ATTR_NUMBER +
                              "=\"" +
                              eCrew.getPassengers().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCAPE_PASSENGERS + ">\n");
                }
                // Original number of men
                output.write(indentStr(indentLvl + 1) +
                      '<' +
                      MULParser.ELE_ORIG_MEN +
                      ' ' +
                      MULParser.ATTR_NUMBER +
                      "=\"" +
                      eCrew.getOInternal(Infantry.LOC_INFANTRY));
                output.write("\"/>\n");
            }

            // Write out the mtf/blk file for the unit
            String data = null;
            if (embedUnits && !entity.isCanon()) {
                if (entity instanceof Mek mek) {
                    data = mek.getMtf();
                } else {
                    try {
                        data = String.join("\n", BLKFile.getBlock(entity).getAllDataAsString());
                    } catch (EntitySavingException e) {
                        logger.error("Error writing unit: {}", entity);
                        logger.error(e);
                    }
                }
            }

            if (data != null) {
                String fileName = (entity.getChassis() + ' ' + entity.getModel()).trim();
                fileName = fileName.replaceAll("[/\\\\<>:\"|?*]", "_");
                fileName = fileName + ((entity instanceof Mek) ? ".mtf" : ".blk");


                output.write(indentStr(indentLvl + 1) +
                      '<' +
                      MULParser.ELE_CONSTRUCTION_DATA +
                      ' ' +
                      MULParser.ATTR_FILENAME +
                      "=\"" +
                      fileName +
                      "\">\n");

                output.write(indentStr(indentLvl + 2));

                var dataStream = new ByteArrayOutputStream();
                var ps = new PrintStream(new GZIPOutputStream(Base64.getEncoder().wrap(dataStream), true));
                ps.print(data);
                ps.close();

                output.write(dataStream.toString());
                output.write('\n' + indentStr(indentLvl + 1) + "</" + MULParser.ELE_CONSTRUCTION_DATA + ">\n");
            }


            // Finish writing this entity to the file.
            output.write(indentStr(indentLvl) + "</" + MULParser.ELE_ENTITY + ">\n\n");

        } // Handle the next entity
    }

    /**
     * Writes crew attributes that are tracked individually for multi-crew cockpits.
     *
     */
    private static void writePilotAttributes(Writer output, final Entity entity, final Crew crew, int pos)
          throws IOException {
        output.write("\" " + MULParser.ATTR_NAME + "=\"" + crew.getName(pos).replaceAll("\"", "&quot;"));
        output.write("\" " + MULParser.ATTR_NICK + "=\"");
        output.write(crew.getNickname(pos).replaceAll("\"", "&quot;"));
        output.write("\" " + MULParser.ATTR_GENDER + "=\"" + crew.getGender(pos).name());
        output.write("\" " + MULParser.ATTR_CLAN_PILOT + "=\"" + crew.isClanPilot(pos));

        if ((null != entity.getGame()) &&
              entity.gameOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            output.write("\" " + MULParser.ATTR_GUNNERY_L + "=\"");
            output.write(String.valueOf(crew.getGunneryL(pos)));
            output.write("\" " + MULParser.ATTR_GUNNERY_M + "=\"");
            output.write(String.valueOf(crew.getGunneryM(pos)));
            output.write("\" " + MULParser.ATTR_GUNNERY_B + "=\"");
            output.write(String.valueOf(crew.getGunneryB(pos)));
        } else {
            output.write("\" " + MULParser.ATTR_GUNNERY + "=\"");
            output.write(String.valueOf(crew.getGunnery(pos)));
        }
        output.write("\" " + MULParser.ATTR_PILOTING + "=\"");
        output.write(String.valueOf(crew.getPiloting(pos)));
        if (crew instanceof LAMPilot) {
            writeLAMAeroAttributes(output,
                  (LAMPilot) crew,
                  (null != entity.getGame()) &&
                        entity.gameOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY));
        }
        if ((null != entity.getGame()) &&
              entity.gameOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            output.write("\" " + MULParser.ATTR_ARTILLERY + "=\"");
            output.write(String.valueOf(crew.getArtillery(pos)));
        }

        if (crew.getToughness(0) != 0) {
            output.write("\" " + MULParser.ATTR_TOUGH + "=\"");
            output.write(String.valueOf(crew.getToughness(pos)));
        }

        if (crew.getCrewFatigue(0) != 0) {
            output.write("\" " + MULParser.ATTR_FATIGUE + "=\"");
            output.write(String.valueOf(crew.getCrewFatigue(pos)));
        }

        if (crew.isDead(pos) || (crew.getHits(pos) > 5)) {
            output.write("\" " + MULParser.ATTR_HITS + "=\"" + MULParser.VALUE_DEAD);
        } else if (crew.getHits(pos) > 0) {
            output.write("\" " + MULParser.ATTR_HITS + "=\"");
            output.write(String.valueOf(crew.getHits(pos)));
        }

        if (!crew.getPortrait(pos).hasDefaultCategory()) {
            output.write("\" " + MULParser.ATTR_CAT_PORTRAIT + "=\"");
            output.write(crew.getPortrait(pos).getCategory());
        }

        if (!crew.getPortrait(pos).hasDefaultFilename()) {
            output.write("\" " + MULParser.ATTR_FILE_PORTRAIT + "=\"");
            output.write(crew.getPortrait(pos).getFilename());
        }

        if (!crew.getExternalIdAsString(pos).equals("-1")) {
            output.write("\" " + MULParser.ATTR_EXT_ID + "=\"");
            output.write(crew.getExternalIdAsString(pos));
        }

        String extraData = crew.writeExtraDataToXMLLine(pos);
        if (!StringUtility.isNullOrBlank(extraData)) {
            output.write(extraData);
        }
    }

    private static void writeLAMAeroAttributes(Writer output, final LAMPilot crew, boolean rpgGunnery)
          throws IOException {
        output.write("\" " + MULParser.ATTR_GUNNERY_AERO + "=\"");
        output.write(String.valueOf(crew.getGunneryAero()));
        if (rpgGunnery) {
            output.write("\" " + MULParser.ATTR_GUNNERY_AERO_L + "=\"");
            output.write(String.valueOf(crew.getGunneryAeroL()));
            output.write("\" " + MULParser.ATTR_GUNNERY_AERO_M + "=\"");
            output.write(String.valueOf(crew.getGunneryAeroM()));
            output.write("\" " + MULParser.ATTR_GUNNERY_AERO_B + "=\"");
            output.write(String.valueOf(crew.getGunneryAeroB()));
        }
        output.write("\" " + MULParser.ATTR_PILOTING_AERO + "=\"");
        output.write(String.valueOf(crew.getPilotingAero()));
    }

    /**
     * Writes attributes that pertain to entire crew.
     *
     */
    private static void writeCrewAttributes(Writer output, final Entity entity, final Crew crew) throws IOException {
        if (crew.getInitBonus() != 0) {
            output.write("\" " + MULParser.ATTR_INIT_B + "=\"");
            output.write(String.valueOf(crew.getInitBonus()));
        }
        if (crew.getCommandBonus() != 0) {
            output.write("\" " + MULParser.ATTR_COMMAND_B + "=\"");
            output.write(String.valueOf(crew.getCommandBonus()));
        }
        output.write("\" " + MULParser.ATTR_EJECTED + "=\"");
        output.write(String.valueOf(crew.isEjected()));
        if (crew.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
            output.write("\" " + MULParser.ATTR_ADVANTAGES + "=\"");
            output.write(String.valueOf(crew.getOptionList("::", PilotOptions.LVL3_ADVANTAGES)));
        }
        if (crew.countOptions(PilotOptions.EDGE_ADVANTAGES) > 0) {
            output.write("\" " + MULParser.ATTR_EDGE + "=\"");
            output.write(String.valueOf(crew.getOptionList("::", PilotOptions.EDGE_ADVANTAGES)));
        }
        if (crew.countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
            output.write("\" " + MULParser.ATTR_IMPLANTS + "=\"");
            output.write(String.valueOf(crew.getOptionList("::", PilotOptions.MD_ADVANTAGES)));
        }
        if (crew.countOptions(PilotOptions.EI_ADVANTAGES) > 0) {
            output.write("\" " + MULParser.ATTR_EI_IMPLANTS + "=\"");
            output.write(String.valueOf(crew.getOptionList("::", PilotOptions.EI_ADVANTAGES)));
        }
        // Save EI Interface equipment mode (Off, Initiate enhanced imaging)
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_EI_INTERFACE)) {
                output.write("\" " + MULParser.ATTR_EI_MODE + "=\"");
                output.write(m.curMode().getName());
                break;
            }
        }
        // Write prosthetic enhancement data for infantry (IO p.84)
        if (entity instanceof Infantry infantry) {
            if (infantry.getProstheticEnhancement1() != null) {
                output.write("\" " + MULParser.ATTR_PROSTHETIC_ENHANCEMENT_1 + "=\"");
                output.write(infantry.getProstheticEnhancement1().name());
                output.write("\" " + MULParser.ATTR_PROSTHETIC_ENHANCEMENT_1_COUNT + "=\"");
                output.write(String.valueOf(infantry.getProstheticEnhancement1Count()));
            }
            if (infantry.getProstheticEnhancement2() != null) {
                output.write("\" " + MULParser.ATTR_PROSTHETIC_ENHANCEMENT_2 + "=\"");
                output.write(infantry.getProstheticEnhancement2().name());
                output.write("\" " + MULParser.ATTR_PROSTHETIC_ENHANCEMENT_2_COUNT + "=\"");
                output.write(String.valueOf(infantry.getProstheticEnhancement2Count()));
            }
        }
        if (entity instanceof Mek) {
            if (((Mek) entity).isAutoEject()) {
                output.write("\" " + MULParser.ATTR_AUTO_EJECT + "=\"true");
            } else {
                output.write("\" " + MULParser.ATTR_AUTO_EJECT + "=\"false");
            }
            if ((null != entity.getGame()) &&
                  (entity.gameOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION))) {
                if (((Mek) entity).isCondEjectAmmo()) {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_AMMO + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_AMMO + "=\"false");
                }
                if (((Mek) entity).isCondEjectEngine()) {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_ENGINE + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_ENGINE + "=\"false");
                }
                if (((Mek) entity).isCondEjectCTDest()) {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_CT_DEST + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_CT_DEST + "=\"false");
                }
                if (((Mek) entity).isCondEjectHeadshot()) {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_HEAD_SHOT + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_COND_EJECT_HEAD_SHOT + "=\"false");
                }
            }
        }
    }

    private static String getTurretLockedString(Tank e) {
        String retVal = "      <" + MULParser.ELE_TURRET_LOCK + ' ' + MULParser.ATTR_DIRECTION + "=\"";
        retVal = retVal + e.getSecondaryFacing();
        retVal = retVal + "\"/>\n";
        return retVal;
    }

    private static String getMovementString(Tank e) {
        String retVal = "      <" + MULParser.ELE_MOTIVE + ' ' + MULParser.ATTR_DAMAGE + "=\"";
        retVal = retVal + e.getMotiveDamage();
        retVal = retVal + "\" " + MULParser.ATTR_PENALTY + "=\"";
        retVal = retVal + e.getMotivePenalty();
        retVal = retVal + "\"/>\n";
        return retVal;
    }

    // Aero crits
    private static String getAeroCritString(Aero a) {

        String retVal = "      <" + MULParser.ELE_AERO_CRIT;
        String critVal = "";

        // crits
        if (a.getAvionicsHits() > 0) {
            critVal = critVal + ' ' + MULParser.ATTR_AVIONICS + "=\"";
            critVal = critVal + a.getAvionicsHits();
            critVal = critVal + '"';
        }
        if (a.getSensorHits() > 0) {
            critVal = critVal + "  " + MULParser.ATTR_SENSORS + "=\"";
            critVal = critVal + a.getSensorHits();
            critVal = critVal + '"';
        }
        if (a.getEngineHits() > 0) {
            critVal = critVal + ' ' + MULParser.ATTR_ENGINE + "=\"";
            critVal = critVal + a.getEngineHits();
            critVal = critVal + '"';
        }
        if (a.getFCSHits() > 0) {
            critVal = critVal + ' ' + MULParser.ATTR_FCS + "=\"";
            critVal = critVal + a.getFCSHits();
            critVal = critVal + '"';
        }
        if (a.getCICHits() > 0) {
            critVal = critVal + ' ' + MULParser.ATTR_CIC + "=\"";
            critVal = critVal + a.getCICHits();
            critVal = critVal + '"';
        }
        if (a.getLeftThrustHits() > 0) {
            critVal = critVal + ' ' + MULParser.ATTR_LEFT_THRUST + "=\"";
            critVal = critVal + a.getLeftThrustHits();
            critVal = critVal + '"';
        }
        if (a.getRightThrustHits() > 0) {
            critVal = critVal + ' ' + MULParser.ATTR_RIGHT_THRUST + "=\"";
            critVal = critVal + a.getRightThrustHits();
            critVal = critVal + '"';
        }
        if (!a.hasLifeSupport()) {
            critVal = critVal + ' ' + MULParser.ATTR_LIFE_SUPPORT + "=\"" + MULParser.VALUE_NONE + '"';
        }
        if (a.isGearHit()) {
            critVal = critVal + ' ' + MULParser.ATTR_GEAR + "=\"" + MULParser.VALUE_NONE + '"';
        }

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal + critVal;
            retVal = retVal + "/>\n";
        } else {
            return critVal;
        }

        return retVal;

    }

    // Dropship crits
    private static String getDropshipCritString(Dropship a) {
        String retVal = "      <" + MULParser.ELE_DROP_CRIT;
        String critVal = "";

        // crits
        if (a.isDockCollarDamaged()) {
            critVal = critVal + ' ' + MULParser.ATTR_DOCKING_COLLAR + "=\"" + MULParser.VALUE_NONE + '"';
        }
        if (a.isKFBoomDamaged()) {
            critVal = critVal + ' ' + MULParser.ATTR_KF_BOOM + "=\"" + MULParser.VALUE_NONE + '"';
        }

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal + critVal;
            retVal = retVal + "/>\n";
        } else {
            return critVal;
        }

        return retVal;
    }

    // Tank crits
    private static String getTankCritString(Tank t) {

        String retVal = "      <" + MULParser.ELE_TANK_CRIT;
        String critVal = "";

        // crits
        if (t.getSensorHits() > 0) {
            critVal = critVal + ' ' + MULParser.ATTR_SENSORS + "=\"";
            critVal = critVal + t.getSensorHits();
            critVal = critVal + '"';
        }
        if (t.isEngineHit()) {
            critVal = critVal + ' ' + MULParser.ATTR_ENGINE + "=\"";
            critVal = critVal + MULParser.VALUE_HIT;
            critVal = critVal + '"';
        }

        if (t.isDriverHit()) {
            critVal = critVal + ' ' + MULParser.ATTR_DRIVER + "=\"";
            critVal = critVal + MULParser.VALUE_HIT;
            critVal = critVal + '"';
        }

        if (t.isCommanderHit()) {
            critVal = critVal + ' ' + MULParser.ATTR_COMMANDER + "=\"";
            critVal = critVal + MULParser.VALUE_HIT;
            critVal = critVal + '"';
        } else if (t.isUsingConsoleCommander()) {
            critVal = critVal + ' ' + MULParser.ATTR_COMMANDER + "=\"";
            critVal = critVal + MULParser.VALUE_CONSOLE;
            critVal = critVal + '"';
        }

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal + critVal;
            retVal = retVal + "/>\n";
        } else {
            return critVal;
        }

        return retVal;

    }
}
