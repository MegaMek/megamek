/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.codeUtilities.StringUtility;
import megamek.common.force.Force;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.utilities.xml.MMXMLUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class provides static methods to save a list of <code>Entity</code>s to,
 * and load a list of <code>Entity</code>s from a file.
 */
public class EntityListFile {

    /**
     * Produce a string describing this armor value. Valid output values are any
     * integer from 0 to 100, N/A, or Destroyed.
     *
     * @param points
     *            - the <code>int</code> value of the armor. This value may be
     *            any valid value of entity armor (including NA, DOOMED, and
     *            DESTROYED).
     * @return a <code>String</code> that matches the armor value.
     */
    private static String formatArmor(int points) {
        // Is the armor destroyed or doomed?
        if ((points == IArmorState.ARMOR_DOOMED) || (points == IArmorState.ARMOR_DESTROYED)) {
            return  MULParser.VALUE_DESTROYED;
        }

        // Was there armor to begin with?
        if (points == IArmorState.ARMOR_NA) {
            return  MULParser.VALUE_NA;
        }

        // Translate the int to a String.
        return String.valueOf(points);
    }

    /**
     * Produce a string describing the equipment in a critical slot.
     *
     * @param index
     *            - the <code>String</code> index of the slot. This value should
     *            be a positive integer or "N/A".
     * @param mount
     *            - the <code>Mounted</code> object of the equipment. This value
     *            should be <code>null</code> for a slot with system equipment.
     * @param isHit
     *            - a <code>boolean</code> that identifies this slot as having
     *            taken a hit.
     * @param isDestroyed
     *            - a <code>boolean</code> that identifies the equipment as
     *            having been destroyed. Note that a single slot in a multi-slot
     *            piece of equipment can be destroyed but not hit; it is still
     *            available to absorb additional critical hits.
     * @return a <code>String</code> describing the slot.
     */
    private static String formatSlot(String index, Mounted mount, boolean isHit, boolean isDestroyed,
                                     boolean isRepairable, boolean isMissing, int indentLvl) {
        StringBuilder output = new StringBuilder();

        output.append(indentStr(indentLvl))
                .append("<" + MULParser.ELE_SLOT + " " + MULParser.ATTR_INDEX + "=\"")
                .append(index)
                .append("\" " + MULParser.ATTR_TYPE + "=\"");

        if (mount == null) {
            output.append(MULParser.VALUE_SYSTEM);
        } else {
            output.append(mount.getType().getInternalName());
            if (mount.isRearMounted()) {
                output.append("\" " + MULParser.ATTR_IS_REAR + "=\"true");
            }

            if (mount.isMechTurretMounted()) {
                output.append("\" " + MULParser.ATTR_IS_TURRETED + "=\"true");
            }

            if (mount.getType() instanceof AmmoType) {
                output.append("\" " + MULParser.ATTR_SHOTS + "=\"")
                        .append(mount.getBaseShotsLeft());
                if (mount.getEntity().usesWeaponBays()
                        || (mount.getEntity() instanceof Dropship)) {
                    output.append("\" " + MULParser.ATTR_CAPACITY + "=\"")
                        .append(mount.getSize());
                }
            }

            if ((mount.getType() instanceof WeaponType)
                    && (mount.getType()).hasFlag(WeaponType.F_ONESHOT)
                    && (mount.getLinked() != null)) {
                output.append("\" " + MULParser.ATTR_MUNITION + "=\"");
                output.append(mount.getLinked().getType().getInternalName());
            }

            if (mount.getEntity().isSupportVehicle()
                    && (mount.getType() instanceof InfantryWeapon)) {
                for (Mounted ammo = mount.getLinked(); ammo != null; ammo = ammo.getLinked()) {
                    if (((AmmoType) ammo.getType()).getMunitionType().contains(AmmoType.Munitions.M_INFERNO)) {
                        output.append("\" " + MULParser.ATTR_INFERNO + "=\"").append(ammo.getBaseShotsLeft())
                            .append(":").append(ammo.getOriginalShots());
                    } else {
                        output.append("\" " + MULParser.ATTR_STANDARD + "=\"").append(ammo.getBaseShotsLeft())
                                .append(":").append(ammo.getOriginalShots());
                    }
                }
            }

            if (mount.isRapidfire()) {
                output.append("\" " + MULParser.ATTR_RFMG + "=\"true");
            }

            if (mount.countQuirks() > 0) {
                output.append("\" " + MULParser.ATTR_QUIRKS + "=\"")
                        .append(mount.getQuirkList("::"));
            }

            if (mount.isAnyMissingTroopers()) {
                output.append("\" " + MULParser.ATTR_TROOPER_MISS + "=\"")
                        .append(mount.getMissingTrooperString());
            }
        }

        if (isHit) {
            output.append("\" " + MULParser.ATTR_IS_HIT + "=\"")
                    .append(true);
        }

        if (!isRepairable && (isHit || isDestroyed)) {
            output.append("\" " + MULParser.ATTR_IS_REPAIRABLE + "=\"")
                    .append(false);
        }

        if (isMissing) {
            output.append("\" " + MULParser.ATTR_IS_MISSING + "=\"")
                    .append(true);
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
        // Just redirect to the XML Util for now, and this will make it easy to find for future replacement
        return MMXMLUtility.indentStr(level);
    }

    /**
     * Helper function that generates a string identifying the state of the
     * locations for an entity.
     *
     * @param entity
     *            - the <code>Entity</code> whose location state is needed
     */
    public static String getLocString(Entity entity, int indentLvl) {
        boolean isMech = entity instanceof Mech;
        boolean isNonSmallCraftAero = (entity instanceof Aero) && !(entity instanceof SmallCraft);
        boolean haveSlot = false;
        StringBuilder output = new StringBuilder();
        StringBuffer thisLoc = new StringBuffer();
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
            if (!(entity instanceof Aero)
                    && !entity.isConventionalInfantry()
                    && (entity.getOInternal(loc) != IArmorState.ARMOR_NA)
                    && (entity.getInternalForReal(loc) <= 0)) {
                isDestroyed = true;
            }

            // exact zeroes for BA should not be treated as destroyed as MHQ uses this to signify
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
                    thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ARMOR + " " + MULParser.ATTR_POINTS + "=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity
                            .getArmorForReal(loc)));
                    thisLoc.append("\"/>\n");
                }

                if (entity.getOInternal(loc) != entity.getInternalForReal(loc)) {
                    thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ARMOR + " " + MULParser.ATTR_POINTS + "=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity
                            .getInternalForReal(loc)));
                    thisLoc.append("\" " + MULParser.ATTR_TYPE + "=\"" + MULParser.VALUE_INTERNAL + "\"/>\n");
                }

                if (entity.hasRearArmor(loc)
                        && (entity.getOArmor(loc, true) != entity
                                .getArmorForReal(loc, true))) {
                    thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ARMOR + " " + MULParser.ATTR_POINTS + "=\"");
                    thisLoc.append(EntityListFile.formatArmor(entity
                            .getArmorForReal(loc, true)));
                    thisLoc.append("\" " + MULParser.ATTR_TYPE + "=\"" + MULParser.VALUE_REAR + "\"/>\n");
                }

                if (entity.getLocationStatus(loc) == ILocationExposureStatus.BREACHED) {
                    thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_BREACH + "/>\n");
                }

                if (blownOff) {
                    thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_BLOWN_OFF + "/>\n");
                }
            }

            //
            Map<Mounted, Integer> baySlotMap = new HashMap<>();

            // Walk through the slots in this location.
            for (int loop = 0; loop < entity.getNumberOfCriticals(loc); loop++) {

                // Get this slot.
                CriticalSlot slot = entity.getCritical(loc, loop);

                // Did we get a slot?
                if (null == slot) {

                    // Nope. Record missing actuators on Biped Mechs.
                    if (isMech
                            && !entity.entityIsQuad()
                            && ((loc == Mech.LOC_RARM) || (loc == Mech.LOC_LARM))
                            && ((loop == 2) || (loop == 3))) {
                        thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_SLOT + " " + MULParser.ATTR_INDEX + "=\"");
                        thisLoc.append(loop + 1);
                        thisLoc.append("\" " + MULParser.ATTR_TYPE + "=\"" + MULParser.VALUE_EMPTY + "\"/>\n");
                        haveSlot = true;
                    }

                } else {

                    // Yup. If the equipment isn't a system, get it.
                    Mounted mount = null;
                    if (CriticalSlot.TYPE_EQUIPMENT == slot.getType()) {
                        mount = slot.getMount();
                    }

                    // if the "equipment" is a weapons bay,
                    // then let's make a note of it
                    if (entity.usesWeaponBays() && (mount != null)
                            && !mount.getBayAmmo().isEmpty()) {
                        baySlotMap.put(slot.getMount(), loop + 1);
                    }

                    if ((mount != null) && (mount.getType() instanceof BombType)) {
                        continue;
                    }

                    // Destroyed locations on Mechs that contain slots
                    // that are missing but not hit or destroyed must
                    // have been blown off.
                    if (!isDestroyed && isMech && slot.isMissing()
                            && !slot.isHit() && !slot.isDestroyed()) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record damaged slots in undestroyed locations.
                    else if (!isDestroyed && slot.isDamaged()) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }

                    // record any quirks
                    else if ((null != mount) && (mount.countQuirks() > 0)) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record Rapid Fire Machine Guns
                    else if ((mount != null) && (mount.isRapidfire())) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record ammunition slots in undestroyed locations.
                    // N.B. the slot CAN\"T be damaged at this point.
                    else if (!isDestroyed && (mount != null)
                            && (mount.getType() instanceof AmmoType)) {

                        String bayIndex = "";

                        for (Mounted bay : baySlotMap.keySet()) {
                            if (bay.ammoInBay(entity.getEquipmentNum(mount))) {
                                bayIndex = String.valueOf(baySlotMap.get(bay));
                            }
                        }

                        thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_SLOT + " " + MULParser.ATTR_INDEX + "=\"");
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

                    // Record the munition type of oneshot launchers
                    // and the ammunition shots of small SV weapons
                    else if (!isDestroyed && (mount != null)
                            && (mount.getType() instanceof WeaponType)
                            && ((mount.getType()).hasFlag(WeaponType.F_ONESHOT)
                            || (entity.isSupportVehicle() && (mount.getType() instanceof InfantryWeapon)))) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }

                    // Record trooper missing equipment on BattleArmor
                    else if (null != mount && mount.isAnyMissingTroopers()) {
                        thisLoc.append(EntityListFile.formatSlot(
                                String.valueOf(loop + 1), mount, slot.isHit(),
                                slot.isDestroyed(), slot.isRepairable(),
                                slot.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }

                } // End have-slot

            } // Check the next slot in this location

            // Stabilizer hit
            if ((entity instanceof Tank)
                    && ((Tank) entity).isStabiliserHit(loc)) {
                thisLoc.append(indentStr(indentLvl + 1) + "<" + MULParser.ELE_STABILIZER + " " + MULParser.ATTR_IS_HIT + "=\"true\"/>\n");
            }

            // Protomechs only have system slots,
            // so we have to handle the ammo specially.
            if (entity instanceof Protomech) {
                for (Mounted mount : entity.getAmmo()) {
                    // Is this ammo in the current location?
                    if (mount.getLocation() == loc) {
                        thisLoc.append(EntityListFile.formatSlot(MULParser.VALUE_NA, mount,
                                mount.isHit(), mount.isDestroyed(), mount.isRepairable(), mount.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }
                } // Check the next ammo.
                // TODO: handle slotless equipment.
            } // End is-proto

            // GunEmplacements don't have system slots,
            // so we have to handle the ammo specially.
            if (entity instanceof GunEmplacement) {
                for (Mounted mount : entity.getEquipment()) {
                    // Is this ammo in the current location?
                    if (mount.getLocation() == loc) {
                        thisLoc.append(EntityListFile.formatSlot(MULParser.VALUE_NA, mount,
                                mount.isHit(), mount.isDestroyed(), mount.isRepairable(), mount.isMissing(), indentLvl + 1));
                        haveSlot = true;
                    }
                } // Check the next ammo.
                // TODO: handle slotless equipment.
            } // End is-ge

            // Did we record information for this location?
            if (thisLoc.length() > 0) {

                // Add this location to the output string.
                output.append(indentStr(indentLvl) + "<" + MULParser.ELE_LOCATION + " " + MULParser.ATTR_INDEX + "=\"");
                output.append(loc);
                if (isDestroyed) {
                    output.append("\" " + MULParser.ATTR_IS_DESTROYED + "=\"true");
                }
                output.append("\"> ");
                output.append(entity.getLocationName(loc));
                if (blownOff) {
                    output.append(" has been blown off.");
                }
                output.append("\n");
                output.append(thisLoc);
                output.append(indentStr(indentLvl) + "</" + MULParser.ELE_LOCATION + ">\n");

                // Reset the location buffer.
                thisLoc = new StringBuffer();
                blownOff = false;

            } // End output-location

            // If the location is completely destroyed, log it anyway.
            else if (isDestroyed) {

                // Add this location to the output string.
                output.append(indentStr(indentLvl) + "<" + MULParser.ELE_LOCATION + " " + MULParser.ATTR_INDEX + "=\"");
                output.append(loc);
                output.append("\" " + MULParser.ATTR_IS_DESTROYED + "=\"true\" /> ");
                output.append(entity.getLocationName(loc));
                output.append("\n");

            } // End location-completely-destroyed

            // Reset the "location is destroyed" flag.
            isDestroyed = false;

        } // Handle the next location

        // If there is no location string, return a null.
        if (output.length() == 0) {
            return null;
        }

        // If we recorded a slot, remind the player that slots start at 1.
        if (haveSlot) {
            output.insert(0, "\n");
            output.insert(0, "      The first slot in a location is at index=\"1\".");

            // Tanks do weird things with ammo.
            if (entity instanceof Tank) {
                output.insert(0, "\n");
                output.insert(0, "      Tanks have special needs, so don't delete any ammo slots.");
            }
        }

        // Convert the output into a String and return it.
        return output.toString();

    } // End private static String getLocString( Entity )

    /**
     * Save the <code>Entity</code>s in the list to the given file.
     * <p>
     * The <code>Entity</code>s\" pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to a
     * particular game is ignored.
     *
     * @param file
     *            - The current contents of the file will be discarded and all
     *            <code>Entity</code>s in the list will be written to the file.
     * @param list
     *            - a <code>Vector</code> containing <code>Entity</code>s to be
     *            stored in a file.
     * @throws IOException
     *             is thrown on any error.
     */
    public static void saveTo(File file, ArrayList<Entity> list) throws IOException {
        // Open up the file. Produce UTF-8 output.
        Writer output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8));

        // Output the doctype and header stuff.
        output.write("<?xml " + MULParser.VERSION + "=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        output.write("<" + MULParser.ELE_UNIT + " " + MULParser.VERSION + "=\"" + MMConstants.VERSION + "\" >\n\n");

        writeEntityList(output, list);

        // Finish writing.
        output.write("</" + MULParser.ELE_UNIT + ">\n");
        output.flush();
        output.close();
    }

    /**
     * Save the entities from the game of client to the given file. This will create
     * separate sections for salvage, devastated, and ejected crews in addition
     * to the surviving units
     * <p>
     * The <code>Entity</code>s pilots, damage, ammo loads, ammo usage, and
     * other campaign-related information are retained but data specific to a
     * particular game is ignored.
     *
     * @param file
     *            - The current contents of the file will be discarded and all
     *            <code>Entity</code>s in the list will be written to the file.
     * @param client
     *            - a <code>Client</code> containing the <code>Game</code>s to be used
     * @throws IOException
     *             is thrown on any error.
     */
    public static void saveTo(File file, Client client) throws IOException {
        if (null == client.getGame()) {
            return;
        }

        // Open up the file. Produce UTF-8 output.
        Writer output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8));

        // Output the doctype and header stuff.
        output.write("<?xml " + MULParser.VERSION + "=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        output.write("<" + MULParser.ELE_RECORD + " " + MULParser.VERSION + "=\"" + MMConstants.VERSION + "\" >");

        ArrayList<Entity> living = new ArrayList<>();
        ArrayList<Entity> allied = new ArrayList<>();
        ArrayList<Entity> salvage = new ArrayList<>();
        ArrayList<Entity> retreated = new ArrayList<>();
        ArrayList<Entity> devastated = new ArrayList<>();
        Hashtable<String, String> kills = new Hashtable<>();

        // Sort entities into player's, enemies, and allies and add to survivors, salvage, and allies.
        Iterator<Entity> entities = client.getGame().getEntities();
        while (entities.hasNext()) {
            Entity entity = entities.next();
            if (entity.getOwner().getId() == client.getLocalPlayer().getId()) {
                living.add(entity);
            } else if (entity.getOwner().isEnemyOf(client.getLocalPlayer())) {
                 if (!entity.canEscape()) {
                     kills.put(entity.getDisplayName(),  MULParser.VALUE_NONE);
                 }
                 salvage.add(entity);
            } else {
                allied.add(entity);
            }
        }

        // Be sure to include all units that have retreated in survivor and allied sections
        for (Enumeration<Entity> iter = client.getGame().getRetreatedEntities(); iter.hasMoreElements(); ) {
            Entity ent = iter.nextElement();
            if (ent.getOwner().getId() == client.getLocalPlayer().getId()) {
                living.add(ent);
            } else if (!ent.getOwner().isEnemyOf(client.getLocalPlayer())) {
                allied.add(ent);
            } else {
                retreated.add(ent);
            }
        }

        // salvageable stuff
        Enumeration<Entity> graveyard = client.getGame().getGraveyardEntities();
        while (graveyard.hasMoreElements()) {
            Entity entity = graveyard.nextElement();
            if (entity.getOwner().isEnemyOf(client.getLocalPlayer())) {
                Entity killer = client.getGame().getEntityFromAllSources(entity.getKillerId());
                if (null != killer
                        && !killer.getExternalIdAsString().equals("-1")) {
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
            if (entity.getOwner().isEnemyOf(client.getLocalPlayer())) {
                Entity killer = client.getGame().getEntityFromAllSources(entity.getKillerId());
                if (null != killer
                        && !killer.getExternalIdAsString().equals("-1")) {
                    kills.put(entity.getDisplayName(), killer.getExternalIdAsString());
                } else {
                    kills.put(entity.getDisplayName(), MULParser.VALUE_NONE);
                }
            }
            devastated.add(entity);
        }

        if (!living.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + "<" + MULParser.ELE_SURVIVORS + ">\n\n");
            writeEntityList(output, living);
            output.write(indentStr(1) + "</" + MULParser.ELE_SURVIVORS + ">\n");
        }

        if (!allied.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + "<" + MULParser.ELE_ALLIES + ">\n\n");
            writeEntityList(output, allied);
            output.write(indentStr(1) + "</" + MULParser.ELE_ALLIES + ">\n");
        }

        if (!salvage.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + "<" + MULParser.ELE_SALVAGE + ">\n\n");
            writeEntityList(output, salvage);
            output.write(indentStr(1) + "</" + MULParser.ELE_SALVAGE + ">\n");
        }

        if (!retreated.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + "<" + MULParser.ELE_RETREATED + ">\n\n");
            writeEntityList(output, retreated);
            output.write(indentStr(1) + "</" + MULParser.ELE_RETREATED + ">\n");
        }

        if (!devastated.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + "<" + MULParser.ELE_DEVASTATED + ">\n\n");
            writeEntityList(output, devastated);
            output.write(indentStr(1) + "</" + MULParser.ELE_DEVASTATED + ">\n");
        }

        if (!kills.isEmpty()) {
            output.write("\n");
            output.write(indentStr(1) + "<" + MULParser.ELE_KILLS + ">\n\n");
            writeKills(output, kills);
            output.write(indentStr(1) + "</" + MULParser.ELE_KILLS + ">\n");
        }

        // Finish writing.
        output.write("</" + MULParser.ELE_RECORD + ">\n");
        output.flush();
        output.close();
    }

    private static void writeKills(Writer output, Hashtable<String,String> kills) throws IOException {
        int indentLvl = 2;
        for (String killed : kills.keySet()) {
            output.write(indentStr(indentLvl) + "<" + MULParser.ELE_KILL + " " + MULParser.ATTR_KILLED + "=\"");
            output.write(killed.replaceAll("\"", "&quot;"));
            output.write("\" " + MULParser.ATTR_KILLER + "=\"");
            output.write(kills.get(killed));
            output.write("\"/>\n");
        }
    }

    private static void writeEntityList(Writer output, ArrayList<Entity> list) throws IOException {
        // Walk through the list of entities.
        Iterator<Entity> items = list.iterator();
        while (items.hasNext()) {
            final Entity entity = items.next();

            if (entity instanceof FighterSquadron) {
                continue;
            }
            int indentLvl = 2;

            // Start writing this entity to the file.
            output.write(indentStr(indentLvl) + "<" + MULParser.ELE_ENTITY + " " + MULParser.ATTR_CHASSIS + "=\"");
            output.write(entity.getFullChassis().replaceAll("\"", "&quot;"));
            output.write("\" " + MULParser.ATTR_MODEL + "=\"");
            output.write(entity.getModel().replaceAll("\"", "&quot;"));
            output.write("\" " + MULParser.ATTR_TYPE + "=\"");
            output.write(entity.getMovementModeAsString());
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
                output.write(((VTOL) entity).getElevation() + "");
            }
            if (!entity.getExternalIdAsString().equals("-1")) {
                output.write("\" " + MULParser.ATTR_EXT_ID + "=\"");
                output.write(entity.getExternalIdAsString());
            }
            if (entity.countQuirks() > 0) {
                output.write("\" " + MULParser.ATTR_QUIRKS + "=\"");
                output.write(String.valueOf(entity.getQuirkList("::")));
            }
            if (entity.getC3Master() != null) {
                output.write("\" " + MULParser.ATTR_C3MASTERIS + "=\"");
                output.write(entity.getGame()
                        .getEntity(entity.getC3Master().getId())
                        .getC3UUIDAsString());
            }
            if (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3()) {
                output.write("\" " + MULParser.ATTR_C3UUID + "=\"");
                output.write(entity.getC3UUIDAsString());
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

            if ((entity instanceof MechWarrior) && !((MechWarrior) entity).getPickedUpByExternalIdAsString().equals("-1")) {
                output.write("\" " + MULParser.ATTR_PICKUP_ID + "=\"");
                output.write(((MechWarrior) entity).getPickedUpByExternalIdAsString());
            }

            // Save some values for conventional infantry
            if (entity.isConventionalInfantry()) {
                Infantry inf = (Infantry) entity;
                if (inf.getArmorDamageDivisor() != 1) {
                    output.write("\" " + MULParser.ATTR_ARMOR_DIVISOR + "=\"");
                    output.write(inf.getArmorDamageDivisor() + "");
                }
                if (inf.isArmorEncumbering()) {
                    output.write("\" " + MULParser.ATTR_ARMOR_ENC + "=\"1");
                }
                if (inf.hasSpaceSuit()) {
                    output.write("\" " + MULParser.ATTR_SPACESUIT + "=\"1");
                }
                if (inf.hasDEST()) {
                    output.write("\" " + MULParser.ATTR_DEST_ARMOR + "=\"1");
                }
                if (inf.hasSneakCamo()) {
                    output.write("\" " + MULParser.ATTR_SNEAK_CAMO + "=\"1");
                }
                if (inf.hasSneakIR()) {
                    output.write("\" " + MULParser.ATTR_SNEAK_IR + "=\"1");
                }
                if (inf.hasSneakECM()) {
                    output.write("\" " + MULParser.ATTR_SNEAK_ECM + "=\"1");
                }
                if (inf.getSpecializations() > 0) {
                    output.write("\" " + MULParser.ATTR_INF_SPEC + "=\"");
                    output.write(inf.getSpecializations() + "");
                }
            }
            output.write("\">\n");

            // Add the crew this entity.
            final Crew crew = entity.getCrew();
            if (crew.getSlotCount() > 1) {
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_CREW + " " + MULParser.ATTR_CREWTYPE + "=\"");
                output.write(crew.getCrewType().toString().toLowerCase());
                writeCrewAttributes(output, entity, crew);
                output.write("\">\n");

                for (int pos = 0; pos < crew.getSlotCount(); pos++) {
                    if (crew.isMissing(pos)) {
                        continue;
                    }
                    output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_CREWMEMBER + " " + MULParser.ATTR_SLOT + "=\"" + pos);
                    writePilotAttributes(output, entity, crew, pos);
                    output.write("\"/>\n");
                }
                output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_CREW + ">");
            } else {
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_PILOT + " " + MULParser.ATTR_SIZE + "=\"");
                output.write(String.valueOf(crew.getSize()));
                writePilotAttributes(output, entity, crew, 0);
                writeCrewAttributes(output, entity, crew);
                output.write("\"/>");
            }
            output.write("\n");

            // If it's a tank, add a movement tag.
            if (entity instanceof Tank) {
                Tank tentity = (Tank) entity;
                output.write(EntityListFile.getMovementString(tentity));
                if (tentity.isTurretLocked(tentity.getLocTurret())) {
                    output.write(EntityListFile.getTurretLockedString(tentity));
                }
                // crits
                output.write(EntityListFile.getTankCritString(tentity));
            }

            // Aero stuff that also applies to LAMs
            if (entity instanceof IAero) {
                IAero a = (IAero) entity;
                // fuel
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_FUEL + " " + MULParser.ATTR_LEFT + "=\"");
                output.write(String.valueOf(a.getCurrentFuel()));
                output.write("\"/>\n");
            }

            // Write the Bomb Data if needed
            if (entity.isBomber()) {
                IBomber b = (IBomber) entity;
                int[] intBombChoices = b.getIntBombChoices();
                int[] extBombChoices = b.getExtBombChoices();
                if (intBombChoices.length > 0 || extBombChoices.length > 0) {
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_BOMBS + ">\n");
                    for (int type = 0; type < BombType.B_NUM; type++) {
                        String typeName = BombType.getBombInternalName(type);
                        if (intBombChoices[type] > 0) {
                            output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_BOMB + " " + MULParser.ATTR_TYPE + "=\"");
                            output.write(typeName);
                            output.write("\" " + MULParser.ATTR_LOAD + "=\"");
                            output.write(String.valueOf(intBombChoices[type]));
                            output.write("\" " + MULParser.ATTR_INTERNAL + "=\"true");
                            output.write("\"/>\n");
                        }
                        if (extBombChoices[type] > 0) {
                            output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_BOMB + " " + MULParser.ATTR_TYPE + "=\"");
                            output.write(typeName);
                            output.write("\" " + MULParser.ATTR_LOAD + "=\"");
                            output.write(String.valueOf(extBombChoices[type]));
                            output.write("\" " + MULParser.ATTR_INTERNAL + "=\"false");
                            output.write("\"/>\n");
                        }
                    }
                    for (Mounted m : b.getBombs()) {
                        if (!(m.getType() instanceof BombType)) {
                            continue;
                        }
                        output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_BOMB + " " + MULParser.ATTR_TYPE + "=\"");
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
            if (entity instanceof Aero) {
                Aero a = (Aero) entity;

                // SI
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_SI + " " + MULParser.ATTR_INTEGRITY + "=\"");
                output.write(String.valueOf(a.getSI()));
                output.write("\"/>\n");

                // heat sinks
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_HEAT + " " + MULParser.ATTR_SINK + "=\"");
                output.write(String.valueOf(a.getHeatSinks()));
                output.write("\"/>\n");

                // large craft bays and doors.
                if ((a instanceof Dropship) || (a instanceof Jumpship)) {
                    for (Bay nextbay : a.getTransportBays()) {
                        output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_BAY + " " + MULParser.ATTR_INDEX + "=\"" + nextbay.getBayNumber() + "\">\n");
                        output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_BAYDAMAGE + ">" + nextbay.getBayDamage() + "</" + MULParser.ELE_BAYDAMAGE + ">\n");
                        output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_BAYDOORS + ">" + nextbay.getCurrentDoors() + "</" + MULParser.ELE_BAYDOORS + ">\n");
                        for (Entity e : nextbay.getLoadedUnits()) {
                            output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_LOADED + ">" + e.getId() + "</" + MULParser.ELE_LOADED + ">\n");
                        }
                        output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_BAY + ">\n");
                    }
                }

                // jumpship, warship and space station stuff
                if (a instanceof Jumpship) {
                    Jumpship j = (Jumpship) a;

                    // kf integrity
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_KF + " " + MULParser.ATTR_INTEGRITY + "=\"");
                    output.write(String.valueOf(j.getKFIntegrity()));
                    output.write("\"/>\n");

                    // kf sail integrity
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_SAIL + " " + MULParser.ATTR_INTEGRITY + "=\"");
                    output.write(String.valueOf(j.getSailIntegrity()));
                    output.write("\"/>\n");
                }

                // general aero crits
                output.write(EntityListFile.getAeroCritString(a));

                // dropship only crits
                if (a instanceof Dropship) {
                    Dropship d = (Dropship) a;
                    output.write(EntityListFile.getDropshipCritString(d));
                }

            }

            if (entity instanceof BattleArmor) {
                BattleArmor ba = (BattleArmor) entity;
                for (Mounted m : entity.getEquipment()) {
                    if (m.getType().hasFlag(MiscType.F_BA_MEA)) {
                        Mounted manipulator = null;
                        if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM) {
                            manipulator = ba.getLeftManipulator();
                        } else if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM) {
                            manipulator = ba.getRightManipulator();
                        }
                        output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_BA_MEA + " ");
                        output.write(MULParser.ATTR_BA_MEA_MOUNT_LOC + "=\"" + m.getBaMountLoc() + "\" ");
                        if (manipulator != null) {
                            output.write( MULParser.ATTR_BA_MEA_TYPE_NAME + "=\""
                                    + manipulator.getType().getInternalName() + "\" ");
                        }
                        output.write("/>\n");
                    } else if (m.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                        int mountIdx = entity.getEquipmentNum(m);
                        EquipmentType apType = null;
                        if (m.getLinked() != null) {
                            apType = m.getLinked().getType();
                        }
                        output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_BA_APM + " ");
                        output.write(MULParser.ATTR_BA_APM_MOUNT_NUM + "=\"" + mountIdx + "\" ");
                        if (apType != null) {
                            output.write( MULParser.ATTR_BA_APM_TYPE_NAME + "=\"" + apType.getInternalName() + "\" ");
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
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_C3I + ">\n");
                Iterator<Entity> c3iList = list.iterator();
                while (c3iList.hasNext()) {
                    final Entity C3iEntity = c3iList.next();

                    if (C3iEntity.onSameC3NetworkAs(entity, true)) {
                        output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_C3ILINK + " " + MULParser.ATTR_LINK + "=\"");
                        output.write(C3iEntity.getC3UUIDAsString());
                        output.write("\"/>\n");
                    }
                }
                output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_C3I + ">\n");
            }

            // Write the NC3 Data if needed
            if (entity.hasNavalC3()) {
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_NC3 + ">\n");
                Iterator<Entity> NC3List = list.iterator();
                while (NC3List.hasNext()) {
                    final Entity NC3Entity = NC3List.next();

                    if (NC3Entity.onSameC3NetworkAs(entity, true)) {
                        output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_NC3LINK + " " + MULParser.ATTR_LINK + "=\"");
                        output.write(NC3Entity.getC3UUIDAsString());
                        output.write("\"/>\n");
                    }
                }
                output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_NC3 + ">\n");
            }

            // Record if this entity is transported by another
            if (entity.getTransportId() != Entity.NONE) {
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_CONVEYANCE + " " + MULParser.ATTR_ID + "=\"" + entity.getTransportId());
                output.write("\"/>\n");
            }
            // Record this unit's id number
            if (entity.getId() != Entity.NONE) {
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_GAME + " " + MULParser.ATTR_ID + "=\"" + entity.getId());
                output.write("\"/>\n");
            }

            // Write the force hierarchy
            if (!entity.getForceString().isBlank()) {
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_FORCE + " " + MULParser.ATTR_FORCE + "=\"");
                output.write(entity.getForceString());
                output.write("\"/>\n");
            } else if ((entity.getGame() != null) && (entity.getForceId() != Force.NO_FORCE)) {
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_FORCE + " " + MULParser.ATTR_FORCE + "=\"");
                output.write(entity.getGame().getForces().forceStringFor(entity));
                output.write("\"/>\n");
            }

            // Write the escape craft data, if needed
            if (entity instanceof Aero) {
                Aero aero = (Aero) entity;
                if (!aero.getEscapeCraft().isEmpty()) {
                    for (String id : aero.getEscapeCraft()) {
                        output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ESCCRAFT + " " + MULParser.ATTR_ID + "=\"" + id);
                        output.write("\"/>\n");
                    }
                }
            }

            if (entity instanceof SmallCraft) {
                SmallCraft craft = (SmallCraft) entity;
                if (!craft.getNOtherCrew().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ESCCREW + ">\n");
                    for (String id : craft.getNOtherCrew().keySet()) {
                        output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_SHIP + " " + MULParser.ATTR_ID + "=\"" + id + "\"" + " " + MULParser.ATTR_NUMBER + "=\"" + craft.getNOtherCrew().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCCREW + ">\n");
                }

                if (!craft.getPassengers().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ESCPASS + ">\n");
                    for (String id : craft.getPassengers().keySet()) {
                        output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_SHIP + " " + MULParser.ATTR_ID + "=\"" + id + "\"" + " " + MULParser.ATTR_NUMBER + "=\"" + craft.getPassengers().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCPASS + ">\n");
                }
                if (craft instanceof EscapePods) {
                    // Original number of pods, used to set the strength of a group of pods
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ORIG_PODS + " " + MULParser.ATTR_NUMBER + "=\"" + craft.get0SI());
                    output.write("\"/>\n");
                }

            } else if (entity instanceof EjectedCrew) {
                EjectedCrew eCrew = (EjectedCrew) entity;
                if (!eCrew.getNOtherCrew().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ESCCREW + ">\n");
                    for (String id : eCrew.getNOtherCrew().keySet()) {
                        output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_SHIP + " " + MULParser.ATTR_ID + "=\"" + id + "\"" + " " + MULParser.ATTR_NUMBER + "=\"" + eCrew.getNOtherCrew().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCCREW + ">\n");
                }

                if (!eCrew.getPassengers().isEmpty()) {
                    output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ESCPASS + ">\n");
                    for (String id : eCrew.getPassengers().keySet()) {
                        output.write(indentStr(indentLvl + 2) + "<" + MULParser.ELE_SHIP + " " + MULParser.ATTR_ID + "=\"" + id + "\"" + " " + MULParser.ATTR_NUMBER + "=\"" + eCrew.getPassengers().get(id));
                        output.write("\"/>\n");
                    }
                    output.write(indentStr(indentLvl + 1) + "</" + MULParser.ELE_ESCPASS + ">\n");
                }
                // Original number of men
                output.write(indentStr(indentLvl + 1) + "<" + MULParser.ELE_ORIG_MEN + " " + MULParser.ATTR_NUMBER + "=\"" + eCrew.getOInternal(Infantry.LOC_INFANTRY));
                output.write("\"/>\n");
            }

            // Finish writing this entity to the file.
            output.write(indentStr(indentLvl) + "</" + MULParser.ELE_ENTITY + ">\n\n");

        } // Handle the next entity
    }

    /**
     * Writes crew attributes that are tracked individually for multi-crew cockpits.
     *
     * @param output
     * @param entity
     * @param crew
     * @param pos
     * @throws IOException
     */
    private static void writePilotAttributes(Writer output, final Entity entity, final Crew crew, int pos)
            throws IOException {
        output.write("\" " + MULParser.ATTR_NAME + "=\"" + crew.getName(pos).replaceAll("\"", "&quot;"));
        output.write("\" " + MULParser.ATTR_NICK + "=\"");
        output.write(crew.getNickname(pos).replaceAll("\"", "&quot;"));
        output.write("\" " + MULParser.ATTR_GENDER + "=\"" + crew.getGender(pos).name());

        if ((null != entity.getGame())
                && entity.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            output.write("\" " + MULParser.ATTR_GUNNERYL + "=\"");
            output.write(String.valueOf(crew.getGunneryL(pos)));
            output.write("\" " + MULParser.ATTR_GUNNERYM + "=\"");
            output.write(String.valueOf(crew.getGunneryM(pos)));
            output.write("\" " + MULParser.ATTR_GUNNERYB + "=\"");
            output.write(String.valueOf(crew.getGunneryB(pos)));
        } else {
            output.write("\" " + MULParser.ATTR_GUNNERY + "=\"");
            output.write(String.valueOf(crew.getGunnery(pos)));
        }
        output.write("\" " + MULParser.ATTR_PILOTING + "=\"");
        output.write(String.valueOf(crew.getPiloting(pos)));
        if (crew instanceof LAMPilot) {
            writeLAMAeroAttributes(output, (LAMPilot) crew,
                    (null != entity.getGame())
                    && entity.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY));
        }
        if ((null != entity.getGame())
                && entity.getGame().getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            output.write("\" " + MULParser.ATTR_ARTILLERY + "=\"");
            output.write(String.valueOf(crew.getArtillery(pos)));
        }

        if (crew.getToughness(0) != 0) {
            output.write("\" " + MULParser.ATTR_TOUGH + "=\"");
            output.write(String.valueOf(crew.getToughness(pos)));
        }

        if (crew.isDead(pos) || (crew.getHits(pos) > 5)) {
            output.write("\" " + MULParser.ATTR_HITS + "=\"" + MULParser.VALUE_DEAD + "");
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

    private static void writeLAMAeroAttributes(Writer output, final LAMPilot crew,
            boolean rpgGunnery) throws IOException {
        output.write("\" " + MULParser.ATTR_GUNNERYAERO + "=\"");
        output.write(String.valueOf(crew.getGunneryAero()));
        if (rpgGunnery) {
            output.write("\" " + MULParser.ATTR_GUNNERYAEROL + "=\"");
            output.write(String.valueOf(crew.getGunneryAeroL()));
            output.write("\" " + MULParser.ATTR_GUNNERYAEROM + "=\"");
            output.write(String.valueOf(crew.getGunneryAeroM()));
            output.write("\" " + MULParser.ATTR_GUNNERYAEROB + "=\"");
            output.write(String.valueOf(crew.getGunneryAeroB()));
        }
        output.write("\" " + MULParser.ATTR_PILOTINGAERO + "=\"");
        output.write(String.valueOf(crew.getPilotingAero()));
    }

    /**
     * Writes attributes that pertain to entire crew.
     *
     * @param output
     * @param entity
     * @param crew
     * @throws IOException
     */
    private static void writeCrewAttributes(Writer output, final Entity entity, final Crew crew) throws IOException {
        if (crew.getInitBonus() != 0) {
            output.write("\" " + MULParser.ATTR_INITB + "=\"");
            output.write(String.valueOf(crew.getInitBonus()));
        }
        if (crew.getCommandBonus() != 0) {
            output.write("\" " + MULParser.ATTR_COMMANDB + "=\"");
            output.write(String.valueOf(crew.getCommandBonus()));
        }
        output.write("\" " + MULParser.ATTR_EJECTED + "=\"");
        output.write(String.valueOf(crew.isEjected()));
        if (crew.countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
            output.write("\" " + MULParser.ATTR_ADVS + "=\"");
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
        if (entity instanceof Mech) {
            if (((Mech) entity).isAutoEject()) {
                output.write("\" " + MULParser.ATTR_AUTOEJECT + "=\"true");
            } else {
                output.write("\" " + MULParser.ATTR_AUTOEJECT + "=\"false");
            }
            if (entity.game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)) {
                if (((Mech) entity).isCondEjectAmmo()) {
                    output.write("\" " + MULParser.ATTR_CONDEJECTAMMO + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_CONDEJECTAMMO + "=\"false");
                }
                if (((Mech) entity).isCondEjectEngine()) {
                    output.write("\" " + MULParser.ATTR_CONDEJECTENGINE + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_CONDEJECTENGINE + "=\"false");
                }
                if (((Mech) entity).isCondEjectCTDest()) {
                    output.write("\" " + MULParser.ATTR_CONDEJECTCTDEST + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_CONDEJECTCTDEST + "=\"false");
                }
                if (((Mech) entity).isCondEjectHeadshot()) {
                    output.write("\" " + MULParser.ATTR_CONDEJECTHEADSHOT + "=\"true");
                } else {
                    output.write("\" " + MULParser.ATTR_CONDEJECTHEADSHOT + "=\"false");
                }
            }
        }
    }

    private static String getTurretLockedString(Tank e) {
        String retval = "      <" + MULParser.ELE_TURRETLOCK + " " + MULParser.ATTR_DIRECTION + "=\"";
        retval = retval.concat(Integer.toString(e.getSecondaryFacing()));
        retval = retval.concat("\"/>\n");
        return retval;
    }

    private static String getMovementString(Tank e) {
        String retVal = "      <" + MULParser.ELE_MOTIVE + " " + MULParser.ATTR_MDAMAGE + "=\"";
        retVal = retVal.concat(Integer.toString(e.getMotiveDamage()));
        retVal = retVal.concat("\" " + MULParser.ATTR_MPENALTY + "=\"");
        retVal = retVal.concat(Integer.toString(e.getMotivePenalty()));
        retVal = retVal.concat("\"/>\n");
        return retVal;
    }

    // Aero crits
    private static String getAeroCritString(Aero a) {

        String retVal = "      <" + MULParser.ELE_AEROCRIT + "";
        String critVal = "";

        // crits
        if (a.getAvionicsHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_AVIONICS + "=\"");
            critVal = critVal.concat(Integer.toString(a.getAvionicsHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getSensorHits() > 0) {
            critVal = critVal.concat("  " + MULParser.ATTR_SENSORS + "=\"");
            critVal = critVal.concat(Integer.toString(a.getSensorHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getEngineHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_ENGINE + "=\"");
            critVal = critVal.concat(Integer.toString(a.getEngineHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getFCSHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_FCS + "=\"");
            critVal = critVal.concat(Integer.toString(a.getFCSHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getCICHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_CIC + "=\"");
            critVal = critVal.concat(Integer.toString(a.getCICHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getLeftThrustHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_LEFT_THRUST + "=\"");
            critVal = critVal.concat(Integer.toString(a.getLeftThrustHits()));
            critVal = critVal.concat("\"");
        }
        if (a.getRightThrustHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_RIGHT_THRUST + "=\"");
            critVal = critVal.concat(Integer.toString(a.getRightThrustHits()));
            critVal = critVal.concat("\"");
        }
        if (!a.hasLifeSupport()) {
            critVal = critVal.concat(" " + MULParser.ATTR_LIFE_SUPPORT + "=\"" + MULParser.VALUE_NONE + "\"");
        }
        if (a.isGearHit()) {
            critVal = critVal.concat(" " + MULParser.ATTR_GEAR + "=\"" + MULParser.VALUE_NONE + "\"");
        }

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal.concat(critVal);
            retVal = retVal.concat("/>\n");
        } else {
            return critVal;
        }

        return retVal;

    }
    // Dropship crits
    private static String getDropshipCritString(Dropship a) {
        String retVal = "      <" + MULParser.ELE_DROPCRIT;
        String critVal = "";

        // crits
        if (a.isDockCollarDamaged()) {
            critVal = critVal.concat(" " + MULParser.ATTR_DOCKING_COLLAR + "=\"" + MULParser.VALUE_NONE + "\"");
        }
        if (a.isKFBoomDamaged()) {
            critVal = critVal.concat(" " + MULParser.ATTR_KFBOOM + "=\"" + MULParser.VALUE_NONE + "\"");
        }

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal.concat(critVal);
            retVal = retVal.concat("/>\n");
        } else {
            return critVal;
        }

        return retVal;
    }

    // Tank crits
    private static String getTankCritString(Tank t) {

        String retVal = "      <" + MULParser.ELE_TANKCRIT;
        String critVal = "";

        // crits
        if (t.getSensorHits() > 0) {
            critVal = critVal.concat(" " + MULParser.ATTR_SENSORS + "=\"");
            critVal = critVal.concat(Integer.toString(t.getSensorHits()));
            critVal = critVal.concat("\"");
        }
        if (t.isEngineHit()) {
            critVal = critVal.concat(" " + MULParser.ATTR_ENGINE + "=\"");
            critVal = critVal.concat(MULParser.VALUE_HIT);
            critVal = critVal.concat("\"");
        }

        if (t.isDriverHit()) {
            critVal = critVal.concat(" " + MULParser.ATTR_DRIVER + "=\"");
            critVal = critVal.concat(MULParser.VALUE_HIT);
            critVal = critVal.concat("\"");
        }

        if (t.isCommanderHit()) {
            critVal = critVal.concat(" " + MULParser.ATTR_COMMANDER + "=\"");
            critVal = critVal.concat(MULParser.VALUE_HIT);
            critVal = critVal.concat("\"");
        } else if (t.isUsingConsoleCommander()) {
            critVal = critVal.concat(" " + MULParser.ATTR_COMMANDER + "=\"");
            critVal = critVal.concat(MULParser.VALUE_CONSOLE);
            critVal = critVal.concat("\"");
        }

        if (!critVal.isBlank()) {
            // then add beginning and end
            retVal = retVal.concat(critVal);
            retVal = retVal.concat("/>\n");
        } else {
            return critVal;
        }

        return retVal;

    }
}
