/*
 * Copyright (C) 2002-2004 Josh Yockey
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.loaders;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import megamek.common.Configuration;
import megamek.common.CriticalSlot;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.Sensor;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import megamek.common.util.BuildingBlock;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.TestInfantry;
import megamek.common.weapons.lasers.clan.CLChemicalLaserWeapon;
import megamek.common.weapons.ppc.clan.CLERPPC;
import megamek.common.weapons.ppc.clan.CLEnhancedPPC;
import megamek.common.weapons.ppc.clan.CLImprovedPPC;
import megamek.common.weapons.ppc.innerSphere.ISERPPC;
import megamek.common.weapons.ppc.innerSphere.ISHeavyPPC;
import megamek.common.weapons.ppc.innerSphere.ISKinsSlaughterPPC;
import megamek.common.weapons.ppc.innerSphere.ISLightPPC;
import megamek.common.weapons.ppc.innerSphere.ISPPC;
import megamek.common.weapons.ppc.innerSphere.ISSnubNosePPC;
import megamek.logging.MMLogger;

/**
 * Switches between the various type-specific parsers depending on suffix
 */
public class MekFileParser {
    private static final MMLogger LOGGER = MMLogger.create(MekFileParser.class);

    private Entity m_entity = null;
    private static Vector<String> canonUnitNames = null;
    public static final String FILENAME_OFFICIAL_UNITS = "OfficialUnitList.txt"; // TODO : Remove inline filename

    public MekFileParser(File f) throws EntityLoadingException {
        this(f, null);
    }

    public MekFileParser(File f, String entryName) throws EntityLoadingException {
        if (entryName == null) {
            // try normal file
            try (InputStream is = new FileInputStream(f.getAbsolutePath())) {
                parse(is, f.getName());
            } catch (FileNotFoundException fileNotFoundException) {
                String homeFolder = System.getProperty("user.home");
                LOGGER.error("File not found: {}",
                      fileNotFoundException.getMessage().replace(homeFolder, "<redacted" + ">"));
            } catch (Exception ex) {
                LOGGER.error(ex, "Entity Loading Exception");
                if (ex instanceof EntityLoadingException) {
                    throw new EntityLoadingException("While parsing file " + f.getName() + ", " + ex.getMessage());
                } else {
                    throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
                }
            }
        } else {
            // try zip file
            try (ZipFile zipFile = new ZipFile(f.getAbsolutePath());
                  InputStream is = zipFile.getInputStream(zipFile.getEntry(entryName))) {
                parse(is, entryName);
            } catch (EntityLoadingException ele) {
                throw new EntityLoadingException(ele.getMessage());
            } catch (NullPointerException npe) {
                throw new NullPointerException();
            } catch (Exception ex) {
                LOGGER.error("", ex);
                throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
            }
        }
    }

    public MekFileParser(InputStream is, String fileName) throws EntityLoadingException {
        try {
            parse(is, fileName);
        } catch (EntityLoadingException ex) {
            LOGGER.error("", ex);
            throw new EntityLoadingException(ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error("", ex);
            throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
        }
    }

    public MekFileParser(String content) throws EntityLoadingException {
        try {
            parse(content);
        } catch (EntityLoadingException ex) {
            LOGGER.error("", ex);
            throw new EntityLoadingException(ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error("", ex);
            throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
        }
    }

    public static void initCanonUnitNames() {
        initCanonUnitNames(Configuration.docsDir(), FILENAME_OFFICIAL_UNITS);
    }

    /**
     * Initialize the list of canon unit names; should only be called if canonUnitNames is null or needs to be
     * reinitialized.
     *
     * @param dir      location where the canonUnitNames file should be
     * @param fileName String name of the file containing canon unit names
     */
    public static void initCanonUnitNames(File dir, String fileName) {
        Vector<String> unitNames = new Vector<>();
        try (FileReader fr = new FileReader(new MegaMekFile(dir, fileName).getFile());
              BufferedReader br = new BufferedReader(fr)) {
            String s;
            String name;
            while ((s = br.readLine()) != null) {
                int nIndex1 = s.indexOf('|');
                if (nIndex1 > -1) {
                    name = s.substring(0, nIndex1);
                    unitNames.addElement(name);
                }
            }
            Collections.sort(unitNames);
        } catch (Exception ignored) {

        }

        // Update names
        setCanonUnitNames(unitNames);
    }

    /**
     * Directly assign a Vector of unit names; protected for unit testing purposes
     *
     * @param unitNames List of unit names
     */
    public static void setCanonUnitNames(Vector<String> unitNames) {
        canonUnitNames = unitNames;
    }

    public Entity getEntity() {
        return m_entity;
    }

    public void parse(InputStream is, String fileName) throws Exception {
        String lowerName = fileName.toLowerCase();
        IMekLoader loader;

        if (lowerName.endsWith(".mtf")) {
            loader = new MtfFile(is);
        } else if (lowerName.endsWith(".blk")) {
            BuildingBlock bb = new BuildingBlock(is);
            if (bb.exists("UnitType")) {
                String sType = bb.getDataAsString("UnitType")[0];
                loader = switch (sType) {
                    case "Tank", "Naval", "Surface", "Hydrofoil" -> new BLKTankFile(bb);
                    case "Infantry" -> new BLKInfantryFile(bb);
                    case "BattleArmor" -> new BLKBattleArmorFile(bb);
                    // CHECKSTYLE IGNORE ForbiddenWords FOR 6 LINES
                    // Do not remove <50.0 compatibility handler 'ProtoMech' as this will break our cross-version
                    // compatibility promise
                    case "ProtoMech", "ProtoMek" -> new BLKProtoMekFile(bb);
                    // Do not remove <50.0 compatibility handler 'Mech' as this will break our cross-version
                    // compatibility promise
                    case "Mek", "Mech" -> new BLKMekFile(bb);
                    case "VTOL" -> new BLKVTOLFile(bb);
                    case "GunEmplacement" -> new BLKGunEmplacementFile(bb);
                    case "SupportTank" -> new BLKSupportTankFile(bb);
                    case "LargeSupportTank" -> new BLKLargeSupportTankFile(bb);
                    case "SupportVTOL" -> new BLKSupportVTOLFile(bb);
                    case "Aero", "AeroSpaceFighter" -> new BLKAeroSpaceFighterFile(bb);
                    case "FixedWingSupport" -> new BLKFixedWingSupportFile(bb);
                    case "ConvFighter" -> new BLKConvFighterFile(bb);
                    case "SmallCraft" -> new BLKSmallCraftFile(bb);
                    case "Dropship" -> new BLKDropshipFile(bb);
                    case "Jumpship" -> new BLKJumpshipFile(bb);
                    case "Warship" -> new BLKWarshipFile(bb);
                    case "SpaceStation" -> new BLKSpaceStationFile(bb);
                    case "HandheldWeapon" -> new BLKHandheldWeaponFile(bb);
                    case "BuildingEntity" -> new BLKStructureFile(bb);
                    default -> throw new EntityLoadingException("Unknown UnitType: " + sType);
                };
            } else {
                loader = new BLKMekFile(bb);
            }
        } else {
            throw new EntityLoadingException("Unsupported file suffix");
        }

        m_entity = loader.getEntity();

        MekFileParser.postLoadInit(m_entity);
    }

    /**
     * Parse a string containing the representation of a unit.
     *
     * @param content String containing the unit representation
     */
    public void parse(String content) throws Exception {
        final boolean isBlk = content.contains("<BlockVersion>") || content.contains("<UnitType>");
        parse(new ByteArrayInputStream(content.getBytes()), isBlk ? ".blk" : ".mtf");
    }

    /**
     * File-format agnostic location to do post-load initialization on a unit. Automatically add BattleArmorHandles to
     * all OmniMek's.
     */
    public static void postLoadInit(Entity ent) throws EntityLoadingException {
        try {
            ent.loadDefaultCustomWeaponOrder();
        } catch (Exception ex) {
            LOGGER.error(ex, "Error in postLoadInit for {}", ent.getDisplayName());
        }

        // add any sensors to the entity's vector of sensors
        if (ent instanceof Mek) {
            // all `Mek's get the four basic sensors
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_MAG_SCAN));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_SEISMIC));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof VTOL) {
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_MAG_SCAN));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof Tank) {
            // all tanks get the four basic sensors
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_MAG_SCAN));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_SEISMIC));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            // Conventional Fighters get a combined sensor suite
            ent.getSensors().add(new Sensor(Sensor.TYPE_AERO_SENSOR));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent.hasETypeFlag(Entity.ETYPE_DROPSHIP) ||
              ent.hasETypeFlag(Entity.ETYPE_SPACE_STATION) ||
              ent.hasETypeFlag(Entity.ETYPE_JUMPSHIP) ||
              ent.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            // Large craft get active radar
            // And both a passive sensor suite and thermal/optical sensors, which only work
            // in space
            ent.getSensors().add(new Sensor(Sensor.TYPE_SPACECRAFT_THERMAL));
            ent.getSensors().add(new Sensor(Sensor.TYPE_SPACECRAFT_RADAR));
            // Only military craft get ESM, which detects active radar
            Aero lc = (Aero) ent;
            if (lc.getDesignType() == Aero.MILITARY) {
                ent.getSensors().add(new Sensor(Sensor.TYPE_SPACECRAFT_ESM));
            }
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent.isAero()) {
            // ASFs and small craft get a combined sensor suite
            // And thermal/optical sensors, which only work in space
            ent.getSensors().add(new Sensor(Sensor.TYPE_AERO_THERMAL));
            ent.getSensors().add(new Sensor(Sensor.TYPE_AERO_SENSOR));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof BattleArmor) {
            if (ent.hasWorkingMisc(MiscType.F_HEAT_SENSOR)) {
                ent.getSensors().add(new Sensor(Sensor.TYPE_BA_HEAT));
                ent.setNextSensor(ent.getSensors().lastElement());
            }
        }

        // Walk through the list of equipment.
        for (Mounted<?> m : ent.getMisc()) {
            if (m.getLinked() != null) {
                continue;
            }
            // link laser insulators
            if ((m.getType().hasFlag(MiscType.F_LASER_INSULATOR) ||
                  m.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE))) {
                // We can link to a laser in the same location that isn't already linked.
                Predicate<Mounted<?>> linkable = mount -> (mount.getLinkedBy() == null) &&
                      (mount.getLocation() == m.getLocation()) &&
                      (mount.getType() instanceof WeaponType) &&
                      (mount.getType().hasFlag(WeaponType.F_LASER) ||
                            mount.getType() instanceof CLChemicalLaserWeapon);
                // The laser pulse module is also restricted to non-pulse lasers, IS only
                if (m.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    linkable = linkable.and(mount -> !mount.getType().hasFlag(WeaponType.F_PULSE) &&
                          !mount.getType().isClan());
                }

                /*
                 * First check the immediate predecessor in the equipment list, which allows
                 * pairing the insulator or pulse module with a specific weapon by placing them
                 * in a specific order. If that doesn't work, fall back to finding the first
                 * eligible
                 * weapon in the location.
                 */
                int eqNum = ent.getEquipment().indexOf(m);
                if ((eqNum > 0) && linkable.test(ent.getEquipment().get(eqNum - 1))) {
                    m.setLinked(ent.getEquipment().get(eqNum - 1));
                } else {
                    for (Mounted<?> weapon : ent.getTotalWeaponList()) {
                        if (linkable.test(weapon)) {
                            m.setLinked(weapon);
                            break;
                        }
                    }
                }
                if (m.getLinked() == null) {
                    LOGGER.error("Unable to match {} to laser for {}", m.getName(), ent.getShortName());
                }
            } else if ((m.getType().hasFlag(MiscType.F_DETACHABLE_WEAPON_PACK))) {
                for (Mounted<?> mWeapon : ent.getTotalWeaponList()) {
                    if (!mWeapon.isDWPMounted()) {
                        continue;
                    }
                    // already linked?
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }
                if (m.getLinked() == null) {
                    // huh. this shouldn't happen
                    throw new EntityLoadingException("Unable to match DWP to weapon for " + ent.getShortName());
                }
            } else if ((m.getType().hasFlag(MiscType.F_AP_MOUNT))) {
                for (Mounted<?> mWeapon : ent.getTotalWeaponList()) {
                    // Can only link APM mounted weapons that aren't linked
                    if (!mWeapon.isAPMMounted() || (mWeapon.getLinkedBy() != null)) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }
            } else if (m.getType().hasFlag(MiscType.F_ARTEMIS) ||
                  m.getType().hasFlag(MiscType.F_ARTEMIS_V) ||
                  m.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {

                // link up to a weapon in the same location
                for (Mounted<?> mWeapon : ent.getTotalWeaponList()) {

                    if (!mWeapon.getType().hasFlag(WeaponType.F_ARTEMIS_COMPATIBLE)) {
                        continue;
                    }

                    // already linked?
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }

                if (m.getLinked() == null) {
                    // huh. this shouldn't happen
                    LOGGER.error("Unable to match Artemis to launcher for {}", ent.getShortName());
                }
            } else if ((m.getType().hasFlag(MiscType.F_STEALTH) || m.getType().hasFlag(MiscType.F_VOID_SIG)) &&
                  (ent instanceof Mek)) {
                // Find an ECM suite to link to the stealth system. Stop looking after we find the first ECM suite.
                for (Mounted<?> mEquip : ent.getMisc()) {
                    MiscType miscType = (MiscType) mEquip.getType();
                    if (miscType.hasFlag(MiscType.F_ECM)) {
                        m.setLinked(mEquip);
                        break;
                    }
                }

                if (m.getLinked() == null) {
                    // This mek has stealth armor but no ECM. Probably an improperly created custom.
                    LOGGER.error(
                          "Unable to find an ECM Suite for {}. `Mek's with Stealth Armor or Void-Signature-System "
                                +
                                "must also be equipped with an ECM Suite.",
                          ent.getShortName());
                }
            } // End link-Stealth
            // Link PPC Capacitor to PPC it its location.
            else if (m.getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {

                // link up to a weapon in the same location
                for (Mounted<?> mWeapon : ent.getTotalWeaponList()) {
                    WeaponType weaponType = (WeaponType) mWeapon.getType();

                    // Only PPC'S are Valid
                    if (!weaponType.hasFlag(WeaponType.F_PPC)) {
                        continue;
                    }

                    // already linked?
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {

                        // Only Legal IS PPC's are allowed.
                        if ((mWeapon.getType() instanceof ISPPC) ||
                              (mWeapon.getType() instanceof ISLightPPC) ||
                              (mWeapon.getType() instanceof ISHeavyPPC) ||
                              (mWeapon.getType() instanceof ISERPPC) ||
                              (mWeapon.getType() instanceof ISSnubNosePPC) ||
                              (mWeapon.getType() instanceof CLERPPC && ent.getYear() >= 3101)) {
                            m.setLinked(mWeapon);
                            break;
                        }
                    }
                }
            } // End link-PPC Capacitor

            // Link MRM Apollo fire-control systems to their missile racks.
            else if (m.getType().hasFlag(MiscType.F_APOLLO)) {

                // link up to a weapon in the same location
                for (Mounted<?> mWeapon : ent.getTotalWeaponList()) {
                    WeaponType weaponType = (WeaponType) mWeapon.getType();

                    // only srm and lrm are valid for artemis
                    if (weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.MRM) {
                        continue;
                    }

                    // already linked?
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }

                }

                if (m.getLinked() == null) {
                    // huh. this shouldn't happen
                    LOGGER.error("Unable to match Apollo to launcher for {}", ent.getShortName());
                }
            } // End link-Apollo
        }
        // now find any active probes and add them to the sensor list choose this sensor if added
        // WOR CEWS
        for (Mounted<?> m : ent.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_BAP)) {
                if (m.getType().getInternalName().equals(Sensor.BAP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BAP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.BAPP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BAPP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.BLOODHOUND)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BLOODHOUND));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.WATCHDOG)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_WATCHDOG));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.NOVA)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_NOVA));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.CLAN_AP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_CLAN_AP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.LIGHT_AP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_LIGHT_AP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.CL_IMPROVED)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BA_IMPROVED));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName().equals(Sensor.IS_IMPROVED)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BA_IMPROVED));
                    ent.setNextSensor(ent.getSensors().lastElement());
                }
            }

            if ((ent instanceof Mek) && m.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {

                if (ent.hasTargComp() ||
                      ((Mek) ent).hasTSM(true) ||
                      (!ent.getMPBoosters().isNone() &&
                            !ent.hasWorkingMisc(MiscType.F_MASC, MiscTypeFlag.S_SUPERCHARGER))) {
                    LOGGER.error("Loading AES with incompatible systems for {}", ent.getShortName());
                }

                if ((m.getLocation() != Mek.LOC_LEFT_ARM) &&
                      (m.getLocation() != Mek.LOC_LEFT_LEG) &&
                      (m.getLocation() != Mek.LOC_RIGHT_ARM) &&
                      (m.getLocation() != Mek.LOC_RIGHT_LEG) &&
                      (m.getLocation() != Mek.LOC_CENTER_LEG)) {
                    throw new EntityLoadingException("Unable to load AES due to incompatible location for " +
                          ent.getShortName());
                }

            }

            if (m.getType().hasFlag(MiscType.F_HARJEL) && (m.getLocation() == Mek.LOC_HEAD)) {
                throw new EntityLoadingException("Unable to load harjel in head for " + ent.getShortName());
            }

            if (m.getType().hasFlag(MiscType.F_MODULAR_ARMOR) &&
                  (((ent instanceof Mek) && (m.getLocation() == Mek.LOC_HEAD)) ||
                        ((ent instanceof VTOL) && (m.getLocation() == VTOL.LOC_ROTOR)))) {
                throw new EntityLoadingException("Unable to load Modular Armor in Rotor/Head location for " +
                      ent.getShortName());
            }

            if (m.getType().hasFlag(MiscType.F_TALON)) {
                if (ent instanceof Mek) {
                    if (!ent.locationIsLeg(m.getLocation())) {
                        throw new EntityLoadingException("Talons are only legal in the Legs for " + ent.getShortName());
                    }
                    for (int loc = 0; loc < ent.locations(); loc++) {
                        if (ent.locationIsLeg(loc) && !ent.hasWorkingMisc(MiscType.F_TALON, null, loc)) {
                            throw new EntityLoadingException("Talons must be in all legs for " + ent.getShortName());
                        }
                    }
                } else {
                    throw new EntityLoadingException("Unable to load talons in non-Mek entity for " +
                          ent.getShortName());
                }
            }

        } // Check the next piece of equipment.

        // Walk through the list of equipment.
        for (Mounted<?> m : ent.getMisc()) {

            // Link PPC Capacitor to PPC it its location.
            if (m.getType().hasFlag(MiscType.F_PPC_CAPACITOR) && (m.getLinked() == null)) {

                // link up to a weapon in the same location
                for (WeaponMounted mWeapon : ent.getWeaponList()) {
                    WeaponType weaponType = mWeapon.getType();

                    // Handle weapon bays
                    if (weaponType.getBayType().equals(EquipmentType.get(EquipmentTypeLookup.PPC_BAY))) {
                        for (WeaponMounted bayMountedWeapon : mWeapon.getBayWeapons()) {
                            WeaponType bayWeaponType = bayMountedWeapon.getType();

                            // Check for PPC that isn't cross-linked
                            if (!bayWeaponType.hasFlag(WeaponType.F_PPC) ||
                                  (bayMountedWeapon.getCrossLinkedBy() != null)) {
                                continue;
                            }

                            // check location
                            if (bayMountedWeapon.getLocation() == m.getLocation()) {
                                // Only Legal IS PPC's are allowed.
                                if ((bayWeaponType instanceof ISPPC) ||
                                      (bayWeaponType instanceof ISLightPPC) ||
                                      (bayWeaponType instanceof ISHeavyPPC) ||
                                      (bayWeaponType instanceof ISERPPC) ||
                                      (bayWeaponType instanceof ISSnubNosePPC) ||
                                      (bayWeaponType instanceof CLERPPC && ent.getYear() >= 3101)) {

                                    m.setCrossLinked(bayMountedWeapon);
                                    break;
                                }
                            }
                        }
                    }

                    // Check for PPC that isn't cross-linked
                    if (!weaponType.hasFlag(WeaponType.F_PPC) || (mWeapon.getCrossLinkedBy() != null)) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {

                        // Only Legal IS PPC's are allowed.
                        if ((mWeapon.getType() instanceof ISPPC) ||
                              (mWeapon.getType() instanceof ISLightPPC) ||
                              (mWeapon.getType() instanceof ISHeavyPPC) ||
                              (mWeapon.getType() instanceof ISERPPC) ||
                              (mWeapon.getType() instanceof ISSnubNosePPC) ||
                              (mWeapon.getType() instanceof CLEnhancedPPC) ||
                              (mWeapon.getType() instanceof CLImprovedPPC) ||
                              (mWeapon.getType() instanceof ISKinsSlaughterPPC) ||
                              (mWeapon.getType() instanceof CLERPPC && ent.getYear() >= 3101)) {

                            m.setCrossLinked(mWeapon);
                            break;
                        }
                    }
                }
                if (m.getLinked() == null) {
                    // huh. this shouldn't happen
                    LOGGER.error("No available PPC to match Capacitor for {}!", ent.getShortName());
                }
            }
        } // Check the next piece of equipment.

        // For BattleArmor, we have to ensure that all ammo that is DWP mounted
        // is linked to it's DWP mounted weapon, so that TestBattleArmor
        // can properly account for DWP mounted ammo
        if (ent instanceof BattleArmor) {
            for (Mounted<?> ammo : ent.getAmmo()) {
                if (ammo.isDWPMounted()) {
                    // First, make sure every valid DWP weapon has ammo
                    for (Mounted<?> weapon : ent.getWeaponList()) {
                        if (weapon.isDWPMounted() &&
                              (weapon.getLinked() == null) &&
                              AmmoType.isAmmoValid(ammo, (WeaponType) weapon.getType())) {
                            weapon.setLinked(ammo);
                            break;
                        }
                    }
                    // If we didn't find a match, we can link to a weapon with
                    // already linked ammo.
                    if (ammo.getLinkedBy() == null) {
                        for (Mounted<?> weapon : ent.getWeaponList()) {
                            if (weapon.isDWPMounted() && AmmoType.isAmmoValid(ammo, (WeaponType) weapon.getType())) {
                                weapon.setLinked(ammo);
                                break;
                            }
                        }
                    }
                }
            }
        }

        linkMGAs(ent);

        // Don't forget to actually load any applicable weapons.
        ent.loadAllWeapons();

        if (ent instanceof Aero) {
            // set RACs and UACs at maximum firing rate if aero
            ent.setRapidFire();
        }

        ent.addClanCase();

        if (ent instanceof BattleArmor) {
            // now, depending on equipment and chassis, BA might be able to do leg and swarm
            // attacks
            if (((BattleArmor) ent).getChassisType() != BattleArmor.CHASSIS_TYPE_QUAD) {
                int tBasicManipulatorCount = ent.countWorkingMisc(MiscType.F_BASIC_MANIPULATOR);
                int tArmoredGloveCount = ent.countWorkingMisc(MiscType.F_ARMORED_GLOVE);
                int tBattleClawCount = ent.countWorkingMisc(MiscType.F_BATTLE_CLAW);
                boolean hasSwarm, hasSwarmStart, hasSwarmStop, hasLegAttack;
                hasSwarm = hasSwarmStart = hasSwarmStop = hasLegAttack = false;
                for (Mounted<?> m : ent.getWeaponList()) {
                    if (m.getType().getInternalName().equals(Infantry.SWARM_WEAPON_MEK)) {
                        hasSwarm = true;
                    } else if (m.getType().getInternalName().equals(Infantry.SWARM_MEK)) {
                        hasSwarmStart = true;
                    } else if (m.getType().getInternalName().equals(Infantry.STOP_SWARM)) {
                        hasSwarmStop = true;
                    } else if (m.getType().getInternalName().equals(Infantry.LEG_ATTACK)) {
                        hasLegAttack = true;
                    }
                }
                switch (ent.getWeightClass()) {
                    case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                    case EntityWeightClass.WEIGHT_LIGHT:
                        if ((tArmoredGloveCount > 1) || (tBasicManipulatorCount > 1) || (tBattleClawCount > 0)) {
                            try {
                                if (!hasSwarmStart) {
                                    ent.addEquipment(EquipmentType.get(Infantry.SWARM_MEK),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                                if (!hasSwarm) {
                                    ent.addEquipment(EquipmentType.get(Infantry.SWARM_WEAPON_MEK),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                                if (!hasSwarmStop) {
                                    ent.addEquipment(EquipmentType.get(Infantry.STOP_SWARM),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                                if (!hasLegAttack) {
                                    ent.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                        }
                        break;
                    case EntityWeightClass.WEIGHT_MEDIUM:
                        if ((tBasicManipulatorCount > 1) || (tBattleClawCount > 0)) {
                            try {
                                if (!hasSwarmStart) {
                                    ent.addEquipment(EquipmentType.get(Infantry.SWARM_MEK),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                                if (!hasSwarm) {
                                    ent.addEquipment(EquipmentType.get(Infantry.SWARM_WEAPON_MEK),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                                if (!hasSwarmStop) {
                                    ent.addEquipment(EquipmentType.get(Infantry.STOP_SWARM),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                                if (!hasLegAttack) {
                                    ent.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK),
                                          BattleArmor.LOC_SQUAD,
                                          false,
                                          BattleArmor.MOUNT_LOC_NONE,
                                          false);
                                }
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(ex.getMessage());
                            }
                        }
                        break;
                    case EntityWeightClass.WEIGHT_HEAVY:
                    case EntityWeightClass.WEIGHT_ASSAULT:
                    default:
                        break;
                }
            }
        }
        // physical attacks for conventional infantry
        else if (ent instanceof Infantry) {
            TestInfantry.adaptAntiMekAttacks((Infantry) ent);
        }

        // Check if it's canon; if it is, mark it as such.
        ent.setCanon(false);// Guilty until proven innocent
        if (canonUnitNames == null) {
            initCanonUnitNames();
        }

        int index = Collections.binarySearch(canonUnitNames, ent.getShortNameRaw());
        if (index >= 0) {
            ent.setCanon(true);
        }
        ent.initMilitary();
        linkDumpers(ent);
    }

    /**
     * Links each Dumper to the first (unlinked) Cargo equipment if there is one and the same location. Works only for
     * variable size Cargo, {@link MiscType#createCargo()}, but not Liquid Storage, Cargo containers or bays.
     *
     * @param entity The entity to add links to
     */
    static void linkDumpers(Entity entity) {
        List<Mounted<?>> dumpers = entity.getMisc()
              .stream()
              .filter(mounted -> mounted.getType().hasFlag(MiscType.F_DUMPER))
              .collect(Collectors.toList());

        List<Mounted<?>> cargos = entity.getMisc()
              .stream()
              .filter(mounted -> mounted.is(EquipmentTypeLookup.CARGO))
              .collect(Collectors.toList());
        cargos.forEach(cargo -> cargo.setLinkedBy(null));

        for (Mounted<?> dumper : dumpers) {
            dumper.setLinked(null);
            for (Mounted<?> cargo : cargos) {
                if ((cargo.getLinkedBy() == null) && (cargo.getLocation() == dumper.getLocation())) {
                    dumper.setLinked(cargo);
                    cargo.setLinkedBy(dumper);
                    break;
                }
            }
        }
    }

    /**
     * Links machine gun arrays to their machine guns using the bayWeapon list. We take the first qualifying machine gun
     * in the location correct size and not already linked to this or another MGA and continue linking successive slots
     * until full, or we get to a slot that does not contain a qualifying machine gun. This allows specifying which guns
     * go with which array in the event of multiple MGAs in a location or some MGs that are not linked.
     *
     * @param entity {@link Entity} with a machine gun array.
     */
    public static void linkMGAs(Entity entity) {
        List<Integer> usedMG = new ArrayList<>();
        for (WeaponMounted machineGunArray : entity.getWeaponList()) {
            if (machineGunArray.getType().hasFlag(WeaponType.F_MGA)) {
                // This may be called from MML after changing equipment location, so there may be old data that needs
                // to be cleared
                machineGunArray.clearBayWeapons();
                for (int i = 0; i < entity.getNumberOfCriticalSlots(machineGunArray.getLocation()); i++) {
                    CriticalSlot slot = entity.getCritical(machineGunArray.getLocation(), i);
                    if ((slot != null) &&
                          (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) &&
                          (slot.getMount().getType() instanceof WeaponType weaponType)) {
                        int eqNum = entity.getEquipmentNum(slot.getMount());
                        if (!usedMG.contains(eqNum) &&
                              weaponType.hasFlag(WeaponType.F_MG) &&
                              (machineGunArray.getType().getRackSize() == weaponType.getRackSize())) {
                            machineGunArray.addWeaponToBay(eqNum);
                            usedMG.add(eqNum);
                            if (machineGunArray.getBayWeapons().size() >= 4) {
                                break;
                            }
                        } else {
                            if (!machineGunArray.getBayWeapons().isEmpty()) {
                                break;
                            }
                        }
                    } else {
                        if (!machineGunArray.getBayWeapons().isEmpty()) {
                            break;
                        }
                    }
                }

                // Fallback for entities that don't use critical slots (e.g., ProtoMeks)
                // If no MGs were linked via critical slots, search the weapon list directly
                if (machineGunArray.getBayWeapons().isEmpty()) {
                    for (WeaponMounted weapon : entity.getWeaponList()) {
                        if (weapon.getLocation() == machineGunArray.getLocation() &&
                              weapon.getType().hasFlag(WeaponType.F_MG) &&
                              machineGunArray.getType().getRackSize() == weapon.getType().getRackSize()) {
                            int eqNum = entity.getEquipmentNum(weapon);
                            if (!usedMG.contains(eqNum)) {
                                machineGunArray.addWeaponToBay(eqNum);
                                usedMG.add(eqNum);
                                if (machineGunArray.getBayWeapons().size() >= 4) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Files in a supported MegaMek file format can be specified on");
            System.out.println("the command line. Multiple files may be processed at once.");
            System.out.println("The supported formats are:");
            System.out.println("\t.mtf    The native MegaMek format that your file will be converted into");
            System.out.println("\t.blk    Another native MegaMek format");
            MekFileParser.getResponse("Press <enter> to exit...");
            return;
        }
        for (String filename : args) {
            File file = new File(filename);
            String outFilename = filename.substring(0, filename.lastIndexOf("."));
            BufferedWriter out = null;
            try {
                MekFileParser mfp = new MekFileParser(file);
                Entity e = mfp.getEntity();
                if (e instanceof Mek) {
                    outFilename += ".mtf";
                    File outFile = new File(outFilename);
                    if (outFile.exists()) {
                        if (!MekFileParser.getResponse("File already exists, overwrite? ")) {
                            return;
                        }
                    }
                    out = new BufferedWriter(new FileWriter(outFile));
                    out.write(((Mek) e).getMtf());
                } else if (e instanceof Tank) {
                    outFilename += ".blk";
                    BLKFile.encode(outFilename, e);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                MekFileParser.getResponse("Press <enter> to exit...");
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        }
    }

    private static boolean getResponse(String prompt) {
        String response = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        LOGGER.info(prompt);
        try {
            response = in.readLine();
        } catch (IOException ignored) {

        }

        return (response != null) && (response.toLowerCase().indexOf("y") == 0);
    }

    public static Entity loadEntity(File f, String entityName) {
        Entity entity = null;
        try {
            entity = new MekFileParser(f, entityName).getEntity();
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }
        return entity;
    }

    public static void dispose() {
        canonUnitNames = null;
    }
}
