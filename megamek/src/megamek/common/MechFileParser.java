/*
 * MechFileParser.java - Copyright (C) 2002-2004 Josh Yockey
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import megamek.common.loaders.*;
import megamek.common.util.BuildingBlock;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.TestInfantry;
import megamek.common.weapons.ppc.*;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * Switches between the various type-specific parsers depending on suffix
 */
public class MechFileParser {
    private Entity m_entity = null;
    private static Vector<String> canonUnitNames = null;
    public static final String FILENAME_OFFICIAL_UNITS = "OfficialUnitList.txt"; // TODO : Remove inline filename

    public MechFileParser(File f) throws EntityLoadingException {
        this(f, null);
    }

    public MechFileParser(File f, String entryName) throws EntityLoadingException {
        if (entryName == null) {
            // try normal file
            try (InputStream is = new FileInputStream(f.getAbsolutePath())) {
                parse(is, f.getName());
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
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
                LogManager.getLogger().error("", ex);
                throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
            }
        }
    }

    public MechFileParser(InputStream is, String fileName) throws EntityLoadingException {
        try {
            parse(is, fileName);
        } catch (EntityLoadingException ex) {
            LogManager.getLogger().error("", ex);
            throw new EntityLoadingException(ex.getMessage());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            throw new EntityLoadingException("Exception from " + ex.getClass() + ": " + ex.getMessage());
        }
    }

    public Entity getEntity() {
        return m_entity;
    }

    public void parse(InputStream is, String fileName) throws Exception {
        String lowerName = fileName.toLowerCase();
        IMechLoader loader;

        if (lowerName.endsWith(".mep")) {
            loader = new MepFile(is);
        } else if (lowerName.endsWith(".mtf")) {
            loader = new MtfFile(is);
        } else if (lowerName.endsWith(".hmp")) {
            loader = new HmpFile(is);
        } else if (lowerName.endsWith(".hmv")) {
            loader = new HmvFile(is);
        } else if (lowerName.endsWith(".xml")) {
            loader = TdbFile.getInstance(is);
        } else if (lowerName.endsWith(".blk")) {
            BuildingBlock bb = new BuildingBlock(is);
            if (bb.exists("UnitType")) {
                String sType = bb.getDataAsString("UnitType")[0];
                if (sType.equals("Tank") || sType.equals("Naval")
                        || sType.equals("Surface") || sType.equals("Hydrofoil")) {
                    loader = new BLKTankFile(bb);
                } else if (sType.equals("Infantry")) {
                    loader = new BLKInfantryFile(bb);
                } else if (sType.equals("BattleArmor")) {
                    loader = new BLKBattleArmorFile(bb);
                } else if (sType.equals("ProtoMech")) {
                    loader = new BLKProtoFile(bb);
                } else if (sType.equals("Mech")) {
                    loader = new BLKMechFile(bb);
                } else if (sType.equals("VTOL")) {
                    loader = new BLKVTOLFile(bb);
                } else if (sType.equals("GunEmplacement")) {
                    loader = new BLKGunEmplacementFile(bb);
                } else if (sType.equals("SupportTank")) {
                    loader = new BLKSupportTankFile(bb);
                } else if (sType.equals("LargeSupportTank")) {
                    loader = new BLKLargeSupportTankFile(bb);
                } else if (sType.equals("SupportVTOL")) {
                    loader = new BLKSupportVTOLFile(bb);
                } else if (sType.equals("Aero")) {
                    loader = new BLKAeroSpaceFighterFile(bb);
                } else if (sType.equals("AeroSpaceFighter")) {
                    loader = new BLKAeroSpaceFighterFile(bb);
                } else if (sType.equals("FixedWingSupport")) {
                    loader = new BLKFixedWingSupportFile(bb);
                } else if (sType.equals("ConvFighter")) {
                    loader = new BLKConvFighterFile(bb);
                } else if (sType.equals("SmallCraft")) {
                    loader = new BLKSmallCraftFile(bb);
                } else if (sType.equals("Dropship")) {
                    loader = new BLKDropshipFile(bb);
                } else if (sType.equals("Jumpship")) {
                    loader = new BLKJumpshipFile(bb);
                } else if (sType.equals("Warship")) {
                    loader = new BLKWarshipFile(bb);
                } else if (sType.equals("SpaceStation")) {
                    loader = new BLKSpaceStationFile(bb);
                } else {
                    throw new EntityLoadingException("Unknown UnitType: " + sType);
                }
            } else {
                loader = new BLKMechFile(bb);
            }
        } else if (lowerName.endsWith(".dbm")) {
            throw new EntityLoadingException(
                    "In order to use mechs from The Drawing Board with MegaMek, you must save your mech as an XML file (look in the 'File' menu of TDB.)  Then use the resulting '.xml' file instead of the '.dbm' file.  Note that only version 2.0.23 or later of TDB is compatible with MegaMek.");
        } else {
            throw new EntityLoadingException("Unsupported file suffix");
        }

        m_entity = loader.getEntity();

        MechFileParser.postLoadInit(m_entity);
    }

    /**
     * File-format agnostic location to do post-load initialization on a unit.
     * Automatically add BattleArmorHandles to all OmniMechs.
     */
    public static void postLoadInit(Entity ent) throws EntityLoadingException {
        try {
            ent.loadDefaultCustomWeaponOrder();
        } catch (Exception ex) {
            LogManager.getLogger().error("Error in postLoadInit for " + ent.getDisplayName(), ex);
        }

        // add any sensors to the entity's vector of sensors
        if (ent instanceof Mech) {
            // all meks get the four basic sensors
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_MAGSCAN));
            ent.getSensors().add(new Sensor(Sensor.TYPE_MEK_SEISMIC));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof VTOL) {
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_MAGSCAN));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent instanceof Tank) {
            // all tanks get the four basic sensors
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_RADAR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_IR));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_MAGSCAN));
            ent.getSensors().add(new Sensor(Sensor.TYPE_VEE_SEISMIC));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            // Conventional Fighters get a combined sensor suite
            ent.getSensors().add(new Sensor(Sensor.TYPE_AERO_SENSOR));
            ent.setNextSensor(ent.getSensors().firstElement());
        } else if (ent.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                || ent.hasETypeFlag(Entity.ETYPE_SPACE_STATION)
                || ent.hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                || ent.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            // Large craft get active radar
            // And both a passive sensor suite and thermal/optical sensors, which only work in space
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
        for (Mounted m : ent.getMisc()) {
            // link laser insulators
            if ((m.getType().hasFlag(MiscType.F_LASER_INSULATOR)
                    || m.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE))) {
                // We can link to a laser in the same location that isn't already linked.
                Predicate<Mounted> linkable = mount ->
                        (mount.getLinkedBy() == null) && (mount.getLocation() == m.getLocation())
                            && (mount.getType() instanceof WeaponType)
                            && mount.getType().hasFlag(WeaponType.F_LASER);
                // The laser pulse module is also restricted to non-pulse lasers, IS only
                if (m.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    linkable = linkable.and(mount -> !mount.getType().hasFlag(WeaponType.F_PULSE)
                            && !mount.getType().isClan());
                }

                /*
                 * First check the immediate predecessor in the equipment list, which allows
                 * pairing the insulator or pulse module with a specific weapon by placing them
                 * in a specific order. If that doesn't work, fall back to finding the first eligible
                 * weapon in the location.
                 */
                int eqNum = ent.getEquipment().indexOf(m);
                if ((eqNum > 0) && linkable.test(ent.getEquipment().get(eqNum - 1))) {
                    m.setLinked(ent.getEquipment().get(eqNum - 1));
                } else {
                    for (Mounted weapon : ent.totalWeaponList) {
                        if (linkable.test(weapon)) {
                            m.setLinked(weapon);
                            break;
                        }
                    }
                }
                if (m.getLinked() == null) {
                    throw new EntityLoadingException("Unable to match " + m.getName() + " to laser for "
                            + ent.getShortName());
                }
            } else if ((m.getType().hasFlag(MiscType.F_DETACHABLE_WEAPON_PACK))) {
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
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
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    // Can only link APM mounted weapons that aren't linked
                    if (!mWeapon.isAPMMounted()
                            || (mWeapon.getLinkedBy() != null)) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {
                        m.setLinked(mWeapon);
                        break;
                    }
                }
            } else if ((m.getType().hasFlag(MiscType.F_ARTEMIS)
                    || (m.getType().hasFlag(MiscType.F_ARTEMIS_V))
                    || (m.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)))
                    && (m.getLinked() == null)) {

                // link up to a weapon in the same location
                for (Mounted mWeapon : ent.getTotalWeaponList()) {

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
                    throw new EntityLoadingException("Unable to match Artemis to launcher for " + ent.getShortName());
                }
            } else if ((m.getType().hasFlag(MiscType.F_STEALTH) || m.getType()
                    .hasFlag(MiscType.F_VOIDSIG))
                    && (m.getLinked() == null)
                    && (ent instanceof Mech)) {
                // Find an ECM suite to link to the stealth system.
                // Stop looking after we find the first ECM suite.
                for (Mounted mEquip : ent.getMisc()) {
                    MiscType mtype = (MiscType) mEquip.getType();
                    if (mtype.hasFlag(MiscType.F_ECM)) {
                        m.setLinked(mEquip);
                        break;
                    }
                }

                if (m.getLinked() == null) {
                    // This mech has stealth armor but no ECM. Probably
                    // an improperly created custom.
                    throw new EntityLoadingException(
                            "Unable to find an ECM Suite for "+ent.getShortName()+".  Mechs with Stealth Armor or Void-Signature-System must also be equipped with an ECM Suite.");
                }
            } // End link-Stealth
              // Link PPC Capacitor to PPC it its location.
            else if (m.getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                    && (m.getLinked() == null)) {

                // link up to a weapon in the same location
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();

                    // Only PPCS are Valid
                    if (!wtype.hasFlag(WeaponType.F_PPC)) {
                        continue;
                    }

                    // already linked?
                    if (mWeapon.getLinkedBy() != null) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {

                        // Only Legal IS PPC's are allowed.
                        if ((mWeapon.getType() instanceof ISPPC)
                                || (mWeapon.getType() instanceof ISLightPPC)
                                || (mWeapon.getType() instanceof ISHeavyPPC)
                                || (mWeapon.getType() instanceof ISERPPC)
                                || (mWeapon.getType() instanceof ISSnubNosePPC)
                                || (mWeapon.getType() instanceof CLERPPC && ent.getYear() >= 3101)) {
                            m.setLinked(mWeapon);
                            break;
                        }
                    }
                }
            } // End link-PPC Capacitor

            // Link MRM Apollo fire-control systems to their missle racks.
            else if (m.getType().hasFlag(MiscType.F_APOLLO)
                    && (m.getLinked() == null)) {

                // link up to a weapon in the same location
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();

                    // only srm and lrm are valid for artemis
                    if (wtype.getAmmoType() != AmmoType.T_MRM) {
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
                    throw new EntityLoadingException(
                            "Unable to match Apollo to launcher for "+ent.getShortName());
                }
            } // End link-Apollo
              // now find any active probes and add them to the sensor list
              // choose this sensor if added
              //WOR CEWS
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
                } else if (m.getType().getInternalName()
                        .equals(Sensor.LIGHT_AP)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_LIGHT_AP));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName()
                        .equals(Sensor.CLIMPROVED)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BA_IMPROVED));
                    ent.setNextSensor(ent.getSensors().lastElement());
                } else if (m.getType().getInternalName()
                        .equals(Sensor.ISIMPROVED)) {
                    ent.getSensors().add(new Sensor(Sensor.TYPE_BA_IMPROVED));
                    ent.setNextSensor(ent.getSensors().lastElement());
                }
            }

            if ((ent instanceof Mech) && (m.getType().hasFlag(MiscType.F_CASE) || m.getType().hasFlag(MiscType.F_CASEII)
                    || m.getType().hasFlag(MiscType.F_CASEP)

            )) {
                ((Mech) ent).setAutoEject(false);
            }

            if ((ent instanceof Mech)
                    && m.getType().hasFlag(
                            MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {

                if (ent.hasTargComp()
                        || ((Mech) ent).hasTSM(true)
                        || (!ent.getMPBoosters().isNone() && !ent.hasWorkingMisc(
                                MiscType.F_MASC, MiscType.S_SUPERCHARGER))) {
                    throw new EntityLoadingException(
                            "Unable to load AES due to incompatible systems for "+ent.getShortName());
                }

                if ((m.getLocation() != Mech.LOC_LARM)
                        && (m.getLocation() != Mech.LOC_LLEG)
                        && (m.getLocation() != Mech.LOC_RARM)
                        && (m.getLocation() != Mech.LOC_RLEG)
                        && (m.getLocation() != Mech.LOC_CLEG)) {
                    throw new EntityLoadingException(
                            "Unable to load AES due to incompatible location for "+ent.getShortName());
                }

            }

            if (m.getType().hasFlag(MiscType.F_HARJEL)
                    && (m.getLocation() == Mech.LOC_HEAD)) {
                throw new EntityLoadingException(
                        "Unable to load harjel in head for "+ent.getShortName());
            }

            if (m.getType().hasFlag(MiscType.F_MASS)
                    && ((m.getLocation() != Mech.LOC_HEAD) || ((((Mech) ent)
                            .getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) && (m
                            .getLocation() != Mech.LOC_CT)))) {
                throw new EntityLoadingException(
                        "Unable to load MASS for "+ent.getShortName()+"!  Must be located in the same location as the cockpit.");
            }

            if (m.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                    && (((ent instanceof Mech) && (m.getLocation() == Mech.LOC_HEAD)) || ((ent instanceof VTOL) && (m
                            .getLocation() == VTOL.LOC_ROTOR)))) {
                throw new EntityLoadingException(
                        "Unable to load Modular Armor in Rotor/Head location for "+ent.getShortName());
            }

            if (m.getType().hasFlag(MiscType.F_TALON)) {
                if (ent instanceof Mech) {
                    if (!ent.locationIsLeg(m.getLocation())) {
                        throw new EntityLoadingException("Talons are only legal in the Legs for " + ent.getShortName());
                    }
                    for (int loc = 0; loc < ent.locations(); loc++) {
                        if (ent.locationIsLeg(loc) && !ent.hasWorkingMisc(MiscType.F_TALON, -1, loc)) {
                            throw new EntityLoadingException("Talons must be in all legs for " + ent.getShortName());
                        }
                    }
                } else {
                    throw new EntityLoadingException("Unable to load talons in non-Mek entity for "
                            + ent.getShortName());
                }
            }

        } // Check the next piece of equipment.

        // Walk through the list of equipment.
        for (Mounted m : ent.getMisc()) {

            // Link PPC Capacitor to PPC it its location.
            if (m.getType().hasFlag(MiscType.F_PPC_CAPACITOR)
                    && (m.getLinked() == null)) {

                // link up to a weapon in the same location
                for (Mounted mWeapon : ent.getWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();

                    //Handle weapon bays
                    if (wtype.getBayType().equals(EquipmentType.get(EquipmentTypeLookup.PPC_BAY))) {
                        for (int wId : mWeapon.getBayWeapons()) {
                            Mounted bayMountedWeapon = ent.getEquipment(wId);
                            WeaponType bayWeapType = (WeaponType) bayMountedWeapon.getType();

                            // Check for PPC that isn't crosslinked
                            if (!bayWeapType.hasFlag(WeaponType.F_PPC) ||
                                    (bayMountedWeapon.getCrossLinkedBy() != null)) {
                                continue;
                            }

                            // check location
                            if (bayMountedWeapon.getLocation() == m.getLocation()) {
                                // Only Legal IS PPC's are allowed.
                                if ((bayWeapType instanceof ISPPC)
                                        || (bayWeapType instanceof ISLightPPC)
                                        || (bayWeapType instanceof ISHeavyPPC)
                                        || (bayWeapType instanceof ISERPPC)
                                        || (bayWeapType instanceof ISSnubNosePPC)
                                        || (bayWeapType instanceof CLERPPC && ent.getYear() >= 3101)) {

                                    m.setCrossLinked(bayMountedWeapon);
                                    break;
                                }
                            }
                        }
                    }

                    // Check for PPC that isn't crosslinked
                    if (!wtype.hasFlag(WeaponType.F_PPC) ||
                            (mWeapon.getCrossLinkedBy() != null)) {
                        continue;
                    }

                    // check location
                    if (mWeapon.getLocation() == m.getLocation()) {

                        // Only Legal IS PPC's are allowed.
                        if ((mWeapon.getType() instanceof ISPPC)
                                || (mWeapon.getType() instanceof ISLightPPC)
                                || (mWeapon.getType() instanceof ISHeavyPPC)
                                || (mWeapon.getType() instanceof ISERPPC)
                                || (mWeapon.getType() instanceof ISSnubNosePPC)
                                || (mWeapon.getType() instanceof CLEnhancedPPC)
                                || (mWeapon.getType() instanceof CLImprovedPPC)
                                || (mWeapon.getType() instanceof ISKinsSlaughterPPC)
                                || (mWeapon.getType() instanceof CLERPPC && ent.getYear() >= 3101)) {

                            m.setCrossLinked(mWeapon);
                            break;
                        }
                    }
                }

                // huh. this shouldn't happen
                Objects.requireNonNull(m.getLinked(), "No available PPC to match Capacitor for " + ent.getShortName() + "!");

            }
        } // Check the next piece of equipment.

        // For BattleArmor, we have to ensure that all ammo that is DWP mounted
        //  is linked to it's DWP mounted weapon, so that TestBattleArmor
        //  can properly account for DWP mounted ammo
        if (ent instanceof BattleArmor) {
            for (Mounted ammo : ent.getAmmo()) {
                if (ammo.isDWPMounted()) {
                    // First, make sure every valid DWP weapon has ammo
                    for (Mounted weapon : ent.getWeaponList()) {
                        if (weapon.isDWPMounted() && (weapon.getLinked() == null)
                                && AmmoType.isAmmoValid(ammo, (WeaponType) weapon.getType())) {
                            weapon.setLinked(ammo);
                            break;
                        }
                    }
                    // If we didn't find a match, we can link to a weapon with
                    //  already linked ammo.
                    if (ammo.getLinkedBy() == null) {
                        for (Mounted weapon : ent.getWeaponList()) {
                            if (weapon.isDWPMounted()
                                    && AmmoType.isAmmoValid(ammo, (WeaponType) weapon.getType())) {
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
            // now, depending on equipment and chassis, BA might be able to do leg and swarm attacks
            if (((BattleArmor) ent).getChassisType() != BattleArmor.CHASSIS_TYPE_QUAD) {
                int tBasicManipulatorCount = ent.countWorkingMisc(MiscType.F_BASIC_MANIPULATOR);
                int tArmoredGloveCount = ent.countWorkingMisc(MiscType.F_ARMORED_GLOVE);
                int tBattleClawCount = ent.countWorkingMisc(MiscType.F_BATTLE_CLAW);
                boolean hasSwarm, hasSwarmStart, hasSwarmStop, hasLegAttack;
                hasSwarm = hasSwarmStart = hasSwarmStop = hasLegAttack = false;
                for (Mounted m : ent.getWeaponList()) {
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
                        if ((tArmoredGloveCount > 1)
                                || (tBasicManipulatorCount > 1)
                                || (tBattleClawCount > 0)) {
                            try {
                                if (!hasSwarmStart) {
                                    ent.addEquipment(
                                            EquipmentType.get(Infantry.SWARM_MEK),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                                if (!hasSwarm) {
                                    ent.addEquipment(EquipmentType
                                            .get(Infantry.SWARM_WEAPON_MEK),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                                if (!hasSwarmStop) {
                                    ent.addEquipment(
                                            EquipmentType.get(Infantry.STOP_SWARM),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                                if (!hasLegAttack) {
                                    ent.addEquipment(
                                            EquipmentType.get(Infantry.LEG_ATTACK),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(
                                        ex.getMessage());
                            }
                        }
                        break;
                    case EntityWeightClass.WEIGHT_MEDIUM:
                        if ((tBasicManipulatorCount > 1)
                                || (tBattleClawCount > 0)) {
                            try {
                                if (!hasSwarmStart) {
                                    ent.addEquipment(
                                            EquipmentType.get(Infantry.SWARM_MEK),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                                if (!hasSwarm) {
                                    ent.addEquipment(EquipmentType
                                            .get(Infantry.SWARM_WEAPON_MEK),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                                if (!hasSwarmStop) {
                                    ent.addEquipment(
                                            EquipmentType.get(Infantry.STOP_SWARM),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                                if (!hasLegAttack) {
                                    ent.addEquipment(
                                            EquipmentType.get(Infantry.LEG_ATTACK),
                                            BattleArmor.LOC_SQUAD, false,
                                            BattleArmor.MOUNT_LOC_NONE, false);
                                }
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(
                                        ex.getMessage());
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
        try {
            if (canonUnitNames == null) {
                canonUnitNames = new Vector<>();
                // init the list.
                try (FileReader fr = new FileReader(new MegaMekFile(
                        Configuration.docsDir(), FILENAME_OFFICIAL_UNITS).getFile());
                     BufferedReader br = new BufferedReader(fr)) {
                    String s;
                    String name;
                    while ((s = br.readLine()) != null) {
                        int nIndex1 = s.indexOf('|');
                        if (nIndex1 > -1) {
                            name = s.substring(0, nIndex1);
                            canonUnitNames.addElement(name);
                        }
                    }
                    Collections.sort(canonUnitNames);
                } catch (Exception ignored) {

                }
            }
        } catch (Exception ignored) {

        }

        int index = Collections.binarySearch(canonUnitNames, ent.getShortNameRaw());
        if (index >= 0) {
            ent.setCanon(true);
        }
        ent.initMilitary();
        linkDumpers(ent);
    }

    /**
     * Links each Dumper to the first (unlinked) Cargo equipment if there is one in the same location.
     * Works only for variable size Cargo, {@link MiscType#createCargo()}, but not Liquid Storage,
     * Cargo containers or bays.
     *
     * @param entity The entity to add links to
     */
    static void linkDumpers(Entity entity) {
        List<Mounted> dumpers = entity.getMisc().stream()
                .filter(mounted -> mounted.getType().hasFlag(MiscType.F_DUMPER)).collect(Collectors.toList());

        List<Mounted> cargos = entity.getMisc().stream()
                .filter(mounted -> mounted.is(EquipmentTypeLookup.CARGO)).collect(Collectors.toList());
        cargos.forEach(cargo -> cargo.setLinkedBy(null));

        for (Mounted dumper : dumpers) {
            dumper.setLinked(null);
            for (Mounted cargo : cargos) {
                if ((cargo.getLinkedBy() == null) && (cargo.getLocation() == dumper.getLocation())) {
                    dumper.setLinked(cargo);
                    cargo.setLinkedBy(dumper);
                    break;
                }
            }
        }
    }

    /**
     * Links machine gun arrays to their machine guns using the bayWeapon list.
     * We take the first qualifying machine gun in the location (correct size and not already
     * linked to this or another MGA and continue linking successive slots until full or we get to
     * a slot that does not contain a qualifying machine gun. This allows specifying which guns go
     * with which array in the event of multiple MGAs in a location or some MGs that are not linked.
     *
     * @param entity
     */
    static void linkMGAs(Entity entity) {
        List<Integer> usedMG = new ArrayList<>();
        for (Mounted mga : entity.getWeaponList()) {
            if (mga.getType().hasFlag(WeaponType.F_MGA)) {
                // This may be called from MML after changing equipment location, so there
                // may be old data that needs to be cleared
                mga.getBayWeapons().clear();
                for (int i = 0; i < entity.getNumberOfCriticals(mga.getLocation()); i++) {
                    CriticalSlot slot = entity.getCritical(mga.getLocation(), i);
                    if ((slot != null) && (slot.getType() == CriticalSlot.TYPE_EQUIPMENT)
                            && (slot.getMount().getType() instanceof WeaponType)) {
                        WeaponType wtype = (WeaponType) slot.getMount().getType();
                        int eqNum = entity.getEquipmentNum(slot.getMount());
                        if (!usedMG.contains(eqNum)
                                && wtype.hasFlag(WeaponType.F_MG)
                                && (((WeaponType) mga.getType()).getRackSize() == wtype.getRackSize())) {
                            mga.addWeaponToBay(eqNum);
                            usedMG.add(eqNum);
                            if (mga.getBayWeapons().size() >= 4) {
                                break;
                            }
                        } else {
                            if (!mga.getBayWeapons().isEmpty()) {
                                break;
                            }
                        }
                    } else {
                        if (!mga.getBayWeapons().isEmpty()) {
                            break;
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
            System.out.println("\t.hmp    Heavy Metal Pro (c) RCW Enterprises");
            System.out.println("\t.mep    MechEngineer Pro (c) Howling Moon SoftWorks");
            System.out.println("\t.xml    The Drawing Board (c) Blackstone Interactive");
            System.out.println("Note: If you are using the MtfConvert utility, you may also drag and drop files onto it for conversion.");
            MechFileParser.getResponse("Press <enter> to exit...");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String filename = args[i];
            File file = new File(filename);
            String outFilename = filename.substring(0,
                    filename.lastIndexOf("."));
            BufferedWriter out = null;
            try {
                MechFileParser mfp = new MechFileParser(file);
                Entity e = mfp.getEntity();
                if (e instanceof Mech) {
                    outFilename += ".mtf";
                    File outFile = new File(outFilename);
                    if (outFile.exists()) {
                        if (!MechFileParser.getResponse("File already exists, overwrite? ")) {
                            return;
                        }
                    }
                    out = new BufferedWriter(new FileWriter(outFile));
                    out.write(((Mech) e).getMtf());
                } else if (e instanceof Tank) {
                    outFilename += ".blk";
                    BLKFile.encode(outFilename, e);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                MechFileParser.getResponse("Press <enter> to exit...");
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
        LogManager.getLogger().info(prompt);
        try {
            response = in.readLine();
        } catch (IOException ignored) {

        }

        return (response != null) && (response.toLowerCase().indexOf("y") == 0);
    }

    public static Entity loadEntity(File f, String entityName) {
        Entity entity = null;
        try {
            entity = new MechFileParser(f, entityName).getEntity();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
        return entity;
    }

    public static void dispose() {
        canonUnitNames = null;
    }
}