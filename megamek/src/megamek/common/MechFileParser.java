/*
 * MechFileParser.java - Copyright (C) 2002,2003,2004 Josh Yockey
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Vector;
import java.util.zip.ZipFile;

import megamek.common.loaders.BLKAeroFile;
import megamek.common.loaders.BLKBattleArmorFile;
import megamek.common.loaders.BLKConvFighterFile;
import megamek.common.loaders.BLKDropshipFile;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKFixedWingSupportFile;
import megamek.common.loaders.BLKGunEmplacementFile;
import megamek.common.loaders.BLKInfantryFile;
import megamek.common.loaders.BLKJumpshipFile;
import megamek.common.loaders.BLKLargeSupportTankFile;
import megamek.common.loaders.BLKMechFile;
import megamek.common.loaders.BLKProtoFile;
import megamek.common.loaders.BLKSmallCraftFile;
import megamek.common.loaders.BLKSpaceStationFile;
import megamek.common.loaders.BLKSupportTankFile;
import megamek.common.loaders.BLKSupportVTOLFile;
import megamek.common.loaders.BLKTankFile;
import megamek.common.loaders.BLKVTOLFile;
import megamek.common.loaders.BLKWarshipFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.HmpFile;
import megamek.common.loaders.HmvFile;
import megamek.common.loaders.IMechLoader;
import megamek.common.loaders.MepFile;
import megamek.common.loaders.MtfFile;
import megamek.common.loaders.TdbFile;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.CLERPPC;
import megamek.common.weapons.ISERPPC;
import megamek.common.weapons.ISHeavyPPC;
import megamek.common.weapons.ISLightPPC;
import megamek.common.weapons.ISPPC;
import megamek.common.weapons.ISSnubNosePPC;

/*
 * Switches between the various type-specific parsers depending on suffix
 */

public class MechFileParser {
    private Entity m_entity = null;
    private static Vector<String> canonUnitNames = null;
    public static final String FILENAME_OFFICIAL_UNITS = "OfficialUnitList.txt"; //$NON-NLS-1$

    public MechFileParser(File f) throws EntityLoadingException {
        this(f, null);
    }

    public MechFileParser(File f, String entryName)
            throws EntityLoadingException {
        if (entryName == null) {
            // try normal file
            try(InputStream is = new FileInputStream(f.getAbsolutePath())) {
                parse(is, f.getName());
            } catch (Exception ex) {
                System.out.println("Error parsing " + entryName + "!");
                ex.printStackTrace();
                if (ex instanceof EntityLoadingException) {
                    throw new EntityLoadingException("While parsing file "
                            + f.getName() + ", " + ex.getMessage());
                }
                throw new EntityLoadingException("Exception from "
                        + ex.getClass() + ": " + ex.getMessage());
            }
        } else {

            // try zip file
            try {
                ZipFile zFile = new ZipFile(f.getAbsolutePath());
                parse(zFile.getInputStream(zFile.getEntry(entryName)),
                        entryName);
                zFile.close();
            } catch (EntityLoadingException ele) {
                throw new EntityLoadingException(ele.getMessage());
            } catch (NullPointerException npe) {
                throw new NullPointerException();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new EntityLoadingException("Exception from "
                        + ex.getClass() + ": " + ex.getMessage());
            }
        }
    }

    public MechFileParser(InputStream is, String fileName)
            throws EntityLoadingException {
        try {
            parse(is, fileName);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof EntityLoadingException) {
                throw new EntityLoadingException(ex.getMessage());
            }
            throw new EntityLoadingException("Exception from " + ex.getClass()
                    + ": " + ex.getMessage());
        }
    }

    public Entity getEntity() {
        return m_entity;
    }

    public void parse(InputStream is, String fileName)
            throws EntityLoadingException {
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
                    loader = new BLKAeroFile(bb);
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
                    throw new EntityLoadingException("Unknown UnitType: "
                            + sType);
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
            ent.loadDefaultQuirks();
            ent.loadDefaultCustomWeaponOrder();
        } catch (Exception e) {
            System.out.println("Error in postLoadInit for "
                    + ent.getDisplayName() + "!");
            e.printStackTrace();
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
        } else if (ent instanceof BattleArmor) {
            if (ent.hasWorkingMisc(MiscType.F_HEAT_SENSOR)) {
                ent.getSensors().add(new Sensor(Sensor.TYPE_BA_HEAT));
                ent.setNextSensor(ent.getSensors().lastElement());
            }
        }

        // Walk through the list of equipment.
        for (Mounted m : ent.getMisc()) {

            // link laser insulators
            if ((m.getType().hasFlag(MiscType.F_LASER_INSULATOR))) {
                // get the mount directly before the insulator, this is the
                // weapon
                Mounted weapon = ent.getEquipment().get(
                        ent.getEquipment().indexOf(m) - 1);
                // already linked?
                if (weapon.getLinkedBy() != null) {
                    continue;
                }
                if (!(weapon.getType() instanceof WeaponType)
                        && !(weapon.getType().hasFlag(WeaponType.F_LASER))) {
                    continue;
                }
                // check location
                if (weapon.getLocation() == m.getLocation()) {
                    m.setLinked(weapon);
                    continue;
                }
                if (m.getLinked() == null) {
                    // huh. this shouldn't happen
                    throw new EntityLoadingException(
                            "Unable to match laser insulator to laser for "+ent.getShortName());
                }
            }

            // link DWPs to their weapons
            if ((m.getType().hasFlag(MiscType.F_DETACHABLE_WEAPON_PACK))) {
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
                    throw new EntityLoadingException(
                            "Unable to match DWP to weapon for "
                                    + ent.getShortName());
                }
            }

            // Link AP weapons to their AP Mount, when applicable
            if ((m.getType().hasFlag(MiscType.F_AP_MOUNT))) {
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
            }

            // Link Artemis IV fire-control systems to their missle racks.
            if ((m.getType().hasFlag(MiscType.F_ARTEMIS) || (m.getType()
                    .hasFlag(MiscType.F_ARTEMIS_V))) && (m.getLinked() == null)) {

                // link up to a weapon in the same location
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();

                    // only srm, lrm and mml are valid for artemis
                    if ((wtype.getAmmoType() != AmmoType.T_LRM)
                            && (wtype.getAmmoType() != AmmoType.T_MML)
                            && (wtype.getAmmoType() != AmmoType.T_SRM)
                            && (wtype.getAmmoType() != AmmoType.T_NLRM)
                            && (wtype.getAmmoType() != AmmoType.T_LRM_TORPEDO)
                            && (wtype.getAmmoType() != AmmoType.T_SRM_TORPEDO)
                            && (wtype.getAmmoType() != AmmoType.T_LRM_TORPEDO_COMBO)) {
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
                            "Unable to match Artemis to launcher for "+ent.getShortName());
                }
            } // End link-Artemis
            // Link RISC Laser Pulse Module to their lasers
            else if ((m.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE) && (m.getLinked() == null))) {

                // link up to a weapon in the same location
                for (Mounted mWeapon : ent.getTotalWeaponList()) {
                    WeaponType wtype = (WeaponType) mWeapon.getType();

                    // only IS lasers that are not pulse are allows
                    if (wtype.hasFlag(WeaponType.F_PULSE) || TechConstants.isClan(wtype.getTechLevel(ent.getYear()))) {
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
                            "Unable to match RISC Laser Pulse Model to laser for "+ent.getShortName());
                }
            } // End link-RISC laser pulse module
            else if ((m.getType().hasFlag(MiscType.F_STEALTH) || m.getType()
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
                for (Mounted mWeapon : ent.getWeaponList()) {
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
                    ent.getSensors().add(new Sensor(Sensor.TYPE_CLAN_BAP));
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

            if ((ent instanceof Mech)
                    && (m.getType().hasFlag(MiscType.F_CASE) || m.getType()
                            .hasFlag(MiscType.F_CASEII))) {
                ((Mech) ent).setAutoEject(false);
            }

            if ((ent instanceof Mech)
                    && m.getType().hasFlag(
                            MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {

                if (ent.hasTargComp()
                        || ((Mech) ent).hasTSM()
                        || (((Mech) ent).hasMASC() && !ent.hasWorkingMisc(
                                MiscType.F_MASC, MiscType.S_SUPERCHARGER))) {
                    throw new EntityLoadingException(
                            "Unable to load AES due to incompatible systems for "+ent.getShortName());
                }

                if ((m.getLocation() != Mech.LOC_LARM)
                        && (m.getLocation() != Mech.LOC_LLEG)
                        && (m.getLocation() != Mech.LOC_RARM)
                        && (m.getLocation() != Mech.LOC_RLEG)) {
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
                if (ent instanceof BipedMech) {
                    if ((m.getLocation() != Mech.LOC_LLEG)
                            && (m.getLocation() != Mech.LOC_RLEG)) {
                        throw new EntityLoadingException(
                                "Talons are only legal in the Legs for "+ent.getShortName());
                    }

                    if (!ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_RLEG)
                            || !ent.hasWorkingMisc(MiscType.F_TALON, -1,
                                    Mech.LOC_LLEG)) {
                        throw new EntityLoadingException(
                                "Talons must be in all legs for "+ent.getShortName());
                    }
                } else if (ent instanceof QuadMech) {
                    if ((m.getLocation() != Mech.LOC_LLEG)
                            && (m.getLocation() != Mech.LOC_RLEG)
                            && (m.getLocation() != Mech.LOC_LARM)
                            && (m.getLocation() != Mech.LOC_RARM)) {
                        throw new EntityLoadingException(
                                "Talons are only legal in the Legs for "+ent.getShortName());
                    }

                    if (!ent.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_RLEG)
                            || !ent.hasWorkingMisc(MiscType.F_TALON, -1,
                                    Mech.LOC_LLEG)
                            || !ent.hasWorkingMisc(MiscType.F_TALON, -1,
                                    Mech.LOC_LARM)
                            || !ent.hasWorkingMisc(MiscType.F_TALON, -1,
                                    Mech.LOC_LARM)) {
                        throw new EntityLoadingException(
                                "Talons must be in all legs for "+ent.getShortName());
                    }

                } else {
                    throw new EntityLoadingException(
                            "Unable to load talons in non-Mek entity for "+ent.getShortName());
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
                    if (wtype.getBayType().equals(EquipmentType.get("PPC Bay"))){
                        for (int wId : mWeapon.getBayWeapons())
                        {
                            Mounted bayMountedWeapon = ent.getEquipment(wId);
                            WeaponType bayWeapType =
                                    (WeaponType)bayMountedWeapon.getType();

                            // Check for PPC that isn't crosslinked
                            if (!bayWeapType.hasFlag(WeaponType.F_PPC) ||
                                    (bayMountedWeapon.getCrossLinkedBy() != null)){
                                continue;
                            }

                            // check location
                            if (bayMountedWeapon.getLocation() ==
                                    m.getLocation()) {

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
                                || (mWeapon.getType() instanceof CLERPPC && ent.getYear() >= 3101)) {

                            m.setCrossLinked(mWeapon);
                            break;
                        }
                    }
                }

                if (m.getLinked() == null) {
                    // huh. this shouldn't happen
                    throw new EntityLoadingException(
                            "No available PPC to match Capacitor for "+ent.getShortName()+"!");
                }

            } // End crossLink-PPC Capacitor

        } // Check the next piece of equipment.

        // For BattleArmor, we have to ensure that all ammo that is DWP mounted
        //  is linked to it's DWP mounted weapon, so that TestBattleArmor
        //  can properly account for DWP mounted ammo
        if (ent instanceof BattleArmor){
            for (Mounted ammo : ent.getAmmo()){
                if (ammo.isDWPMounted()){
                    // First, make sure every valid DWP weapon has ammo
                    for (Mounted weapon : ent.getWeaponList()){
                        if (weapon.isDWPMounted() && (weapon.getLinked() == null)
                                && AmmoType.isAmmoValid(ammo,
                                        (WeaponType)weapon.getType())){
                            weapon.setLinked(ammo);
                            break;
                        }
                    }
                    // If we didn't find a match, we can link to a weapon with
                    //  already linked ammo.
                    if (ammo.getLinkedBy() == null) {
                        for (Mounted weapon : ent.getWeaponList()){
                            if (weapon.isDWPMounted()
                                    && AmmoType.isAmmoValid(ammo,
                                            (WeaponType)weapon.getType())){
                                weapon.setLinked(ammo);
                                break;
                            }
                        }
                    }
                }
            }
        }

        Vector<Integer> usedMG = new Vector<Integer>();
        for (Mounted m : ent.getWeaponList()) {
            // link MGs to their MGA
            // we are going to use the bayWeapon vector because we can't
            // directly link them
            if (m.getType().hasFlag(WeaponType.F_MGA)) {
                for (Mounted other : ent.getWeaponList()) {
                    int eqn = ent.getEquipmentNum(other);
                    if (!usedMG.contains(eqn)
                            && (m.getLocation() == other.getLocation())
                            && other.getType().hasFlag(WeaponType.F_MG)
                            && (((WeaponType) m.getType()).getRackSize() == ((WeaponType) other
                                    .getType()).getRackSize())
                            && !m.getBayWeapons().contains(eqn)
                            && (m.getBayWeapons().size() <= 4)) {
                        m.addWeaponToBay(eqn);
                        usedMG.add(eqn);
                        if (m.getBayWeapons().size() >= 4) {
                            break;
                        }
                    }
                }
            }
        }

        // Don't forget to actually load any applicable weapons.
        ent.loadAllWeapons();

        if (ent instanceof Aero) {
            // set RACs and UACs at maximum firing rate if aero
            ent.setRapidFire();
        }
        if (ent instanceof BattleArmor) {
            // now, depending on equipment and chassis, BA might be able to do
            // leg
            // and swarm attacks
            if (((BattleArmor) ent).getChassisType() != BattleArmor.CHASSIS_TYPE_QUAD) {
                int tBasicManipulatorCount = ent
                        .countWorkingMisc(MiscType.F_BASIC_MANIPULATOR);
                int tArmoredGloveCount = ent
                        .countWorkingMisc(MiscType.F_ARMORED_GLOVE);
                int tBattleClawCount = ent
                        .countWorkingMisc(MiscType.F_BATTLE_CLAW);
                boolean hasSwarm, hasSwarmStart, hasSwarmStop, hasLegAttack;
                hasSwarm = hasSwarmStart = hasSwarmStop = hasLegAttack = false;
                for (Mounted m : ent.getWeaponList()){
                    if (m.getType().getInternalName()
                            .equals(Infantry.SWARM_WEAPON_MEK)) {
                        hasSwarm = true;
                    } else if (m.getType().getInternalName()
                            .equals(Infantry.SWARM_MEK)) {
                        hasSwarmStart = true;
                    } else if (m.getType().getInternalName()
                            .equals(Infantry.STOP_SWARM)) {
                        hasSwarmStop = true;
                    } else if (m.getType().getInternalName()
                            .equals(Infantry.LEG_ATTACK)) {
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
        else if ((ent instanceof Infantry) && ((Infantry) ent).canMakeAntiMekAttacks()) {
            try {
                ent.addEquipment(EquipmentType.get(Infantry.SWARM_MEK),
                        Infantry.LOC_INFANTRY, false,
                        BattleArmor.MOUNT_LOC_NONE, false);
                ent.addEquipment(EquipmentType.get(Infantry.STOP_SWARM),
                        Infantry.LOC_INFANTRY, false,
                        BattleArmor.MOUNT_LOC_NONE, false);
                ent.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK),
                        Infantry.LOC_INFANTRY, false,
                        BattleArmor.MOUNT_LOC_NONE, false);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }

        // Check if it's canon; if it is, mark it as such.
        ent.setCanon(false);// Guilty until proven innocent
        try {
            if (canonUnitNames == null) {
                canonUnitNames = new Vector<String>();
                // init the list.
                try(BufferedReader br = new BufferedReader(new FileReader(new File(
                            Configuration.docsDir(), FILENAME_OFFICIAL_UNITS)))) {
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
                } catch (FileNotFoundException e) {
                }
            }
        } catch (IOException e) {
        }
        int index = Collections.binarySearch(canonUnitNames,
                ent.getShortNameRaw()); 
        if (index >= 0) {
            ent.setCanon(true);
        }        
        ent.initMilitary();

    } // End private void postLoadInit(Entity) throws EntityLoadingException

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out
                    .println("Files in a supported MegaMek file format can be specified on");
            System.out
                    .println("the command line.  Multiple files may be processed at once.");
            System.out.println("The supported formats are:");
            System.out
                    .println("\t.mtf    The native MegaMek format that your file will be converted into");
            System.out.println("\t.blk    Another native MegaMek format");
            System.out.println("\t.hmp    Heavy Metal Pro (c)RCW Enterprises");
            System.out
                    .println("\t.mep    MechEngineer Pro (c)Howling Moon SoftWorks");
            System.out
                    .println("\t.xml    The Drawing Board (c)Blackstone Interactive");
            System.out
                    .println("Note: If you are using the MtfConvert utility, you may also drag and drop files onto it for conversion.");
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
                        if (!MechFileParser
                                .getResponse("File already exists, overwrite? ")) {
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
        System.out.print(prompt);
        try {
            response = in.readLine();
        } catch (IOException ioe) {
        }
        if ((response != null) && (response.toLowerCase().indexOf("y") == 0)) {
            return true;
        }
        return false;
    }

    public static Entity loadEntity(File f, String entityName) {
        Entity entity = null;
        try {
            entity = new MechFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            System.out.println("Exception: " + e.toString());
            e.printStackTrace();
        }
        return entity;
    }

    public static void dispose() {
        canonUnitNames = null;
    }
}
