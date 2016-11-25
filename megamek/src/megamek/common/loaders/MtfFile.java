/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * MtfFile.java
 *
 * Created on April 7, 2002, 8:47 PM
 */

package megamek.common.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.TripodMech;
import megamek.common.WeaponType;

/**
 * @author Ben
 */
public class MtfFile implements IMechLoader {

    String version;

    String name;
    String model;

    String chassisConfig;
    String techBase;
    String techYear;
    String rulesLevel;
    String source = "Source:";

    String tonnage;
    String engine;
    String internalType;
    String myomerType;
    String gyroType;
    String cockpitType;
    String ejectionType;

    String heatSinks;
    String walkMP;
    String jumpMP;
    String baseChassieHeatSinks = "base chassis heat sinks:-1";

    String armorType;
    String[] armorValues = new String[12];

    String weaponCount;
    String[] weaponData;

    String[][] critData;

    String capabilities = "";
    String deployment = "";
    String overview = "";
    String history = "";
    String imagePath = "";

    int bv = 0;

    Hashtable<EquipmentType, Mounted> hSharedEquip = new Hashtable<EquipmentType, Mounted>();
    Vector<Mounted> vSplitWeapons = new Vector<Mounted>();

    public static final int locationOrder[] =
            {Mech.LOC_LARM, Mech.LOC_RARM, Mech.LOC_LT, Mech.LOC_RT, Mech.LOC_CT, Mech.LOC_HEAD, Mech.LOC_LLEG, Mech.LOC_RLEG, Mech.LOC_CLEG};
    public static final int rearLocationOrder[] =
            {Mech.LOC_LT, Mech.LOC_RT, Mech.LOC_CT};

    public static final String EMPTY = "-Empty-";
    public static final String ARMORED = "(armored)";

    /**
     * Creates new MtfFile
     */
    public MtfFile(InputStream is) throws EntityLoadingException {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));

            version = r.readLine();
            if (version == null) {
                throw new EntityLoadingException("MTF File empty!");
            }
            // Version 1.0: Initial version.
            // Version 1.1: Added level 3 cockpit and gyro options.
            // version 1.2: added full head ejection
            if (!version.trim().equalsIgnoreCase("Version:1.0") && !version.trim().equalsIgnoreCase("Version:1.1") && !version.trim().equalsIgnoreCase("Version:1.2")) {
                throw new EntityLoadingException("Wrong MTF file version.");
            }

            name = r.readLine();
            model = r.readLine();

            critData = new String[9][12];

            readCrits(r);

            r.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("I/O Error reading file");
        } catch (StringIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("StringIndexOutOfBoundsException reading file (format error)");
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("NumberFormatException reading file (format error)");
        }
    }

    private void readCrits(BufferedReader r) throws IOException {

        int slot = 0;
        int loc = 0;
        String crit = "";
        int weaponsCount = -1;
        int armorLocation = -1;
        while (r.ready()) {
            crit = r.readLine();
            if (crit.trim().length() < 1) {
                continue;
            }

            if (isValidLocation(crit)) {
                loc = getLocation(crit);
                slot = 0;
                continue;
            }

            if (isProcessedComponent(crit)) {
                continue;
            }

            weaponsCount = weaponsList(crit);

            if (weaponsCount > 0) {
                for (int count = 0; count < weaponsCount; count++) {
                    r.readLine();
                }
                continue;
            }

            armorLocation = getArmorLocation(crit);

            if (armorLocation >= 0) {
                armorValues[armorLocation] = crit;
                continue;
            }
            if (critData.length <= loc) {
                continue;
            }
            if (critData[loc].length <= slot) {
                continue;
            }
            critData[loc][slot++] = crit.trim();

        }

        /*
         * r.readLine(); // blank line r.readLine(); // location name....
         * verify? for (int i = 0; i < 12; i++) { critData[loc][i] =
         * r.readLine(); }
         */
    }

    public Entity getEntity() throws EntityLoadingException {
        try {
            Mech mech;

            int iGyroType = Mech.GYRO_STANDARD;
            try {
                iGyroType = Mech.getGyroTypeForString(gyroType.substring(5));
                if (iGyroType == Mech.GYRO_UNKNOWN) {
                    iGyroType = Mech.GYRO_STANDARD;
                }
            } catch (Exception e) {
                iGyroType = Mech.GYRO_STANDARD;
            }
            int iCockpitType = Mech.COCKPIT_STANDARD;
            try {
                iCockpitType = Mech.getCockpitTypeForString(cockpitType.substring(8));
                if (iCockpitType == Mech.COCKPIT_UNKNOWN) {
                    iCockpitType = Mech.COCKPIT_STANDARD;
                }
            } catch (Exception e) {
                iCockpitType = Mech.COCKPIT_STANDARD;
            }
            boolean fullHead = false;
            try {
                fullHead = ejectionType.substring(9).equals(Mech.FULL_HEAD_EJECT_STRING);
            } catch (Exception e) {
            }
            if (chassisConfig.indexOf("Quad") != -1) {
                mech = new QuadMech(iGyroType, iCockpitType);
            } else if (chassisConfig.indexOf("LAM") != -1) {
                mech = new LandAirMech(iGyroType, iCockpitType);
            } else if (chassisConfig.indexOf("Tripod") != -1) {
                mech = new TripodMech(iGyroType, iCockpitType);
            } else {
                mech = new BipedMech(iGyroType, iCockpitType);
            }
            mech.setFullHeadEject(fullHead);

            mech.setChassis(name.trim());
            mech.setModel(model.trim());
            mech.setYear(Integer.parseInt(techYear.substring(4).trim()));
            mech.setSource(source.substring("Source:".length()).trim());

            if (chassisConfig.indexOf("Omni") != -1) {
                mech.setOmni(true);
            }

            if (techBase.substring(9).trim().equals("Inner Sphere")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                    case 1:
                        mech.setTechLevel(TechConstants.T_INTRO_BOXSET);
                        break;
                    case 2:
                        mech.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                        break;
                    case 3:
                        mech.setTechLevel(TechConstants.T_IS_ADVANCED);
                        break;
                    case 4:
                        mech.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                        break;
                    case 5:
                        mech.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                }
            } else if (techBase.substring(9).trim().equals("Clan")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                    case 2:
                        mech.setTechLevel(TechConstants.T_CLAN_TW);
                        break;
                    case 3:
                        mech.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                        break;
                    case 4:
                        mech.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                        break;
                    case 5:
                        mech.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                }
            } else if (techBase.substring(9).trim().equals("Mixed (IS Chassis)")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                    case 2:
                        mech.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                        break;
                    case 3:
                        mech.setTechLevel(TechConstants.T_IS_ADVANCED);
                        break;
                    case 4:
                        mech.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                        break;
                    case 5:
                        mech.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                }
                mech.setMixedTech(true);
            } else if (techBase.substring(9).trim().equals("Mixed (Clan Chassis)")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                    case 2:
                        mech.setTechLevel(TechConstants.T_CLAN_TW);
                        break;
                    case 3:
                        mech.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                        break;
                    case 4:
                        mech.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                        break;
                    case 5:
                        mech.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                }
                mech.setMixedTech(true);
            } else if (techBase.substring(9).trim().equals("Mixed")) {
                throw new EntityLoadingException("Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
            } else {
                throw new EntityLoadingException("Unsupported tech base: " + techBase.substring(9).trim());
            }

            mech.setWeight(Integer.parseInt(tonnage.substring(5)));

            int engineFlags = 0;
            if ((mech.isClan() && !mech.isMixedTech()) || (mech.isMixedTech() && mech.isClan() && !mech.itemOppositeTech(engine)) || (mech.isMixedTech() && !mech.isClan() && mech.itemOppositeTech(engine))) {
                engineFlags = Engine.CLAN_ENGINE;
            }
            if (mech.isSuperHeavy()) {
                engineFlags |= Engine.SUPERHEAVY_ENGINE;
            }

            int engineRating = Integer.parseInt(engine.substring(engine.indexOf(":") + 1, engine.indexOf(" ")));
            mech.setEngine(new Engine(engineRating, Engine.getEngineTypeByString(engine), engineFlags));

            mech.setOriginalJumpMP(Integer.parseInt(jumpMP.substring(8)));

            boolean dblSinks = (heatSinks.substring(14).startsWith("Double"));

            boolean laserSinks = heatSinks.substring(14).startsWith("Laser");

            boolean compactSinks = heatSinks.substring(14).startsWith("Compact");

            int expectedSinks = Integer.parseInt(heatSinks.substring(11, 13).trim());

            int baseHeatSinks = Integer.parseInt(baseChassieHeatSinks.substring("base chassis heat sinks:".length()).trim());

            String thisStructureType = internalType.substring(internalType.indexOf(':') + 1);
            if (thisStructureType.length() > 0) {
                mech.setStructureType(thisStructureType);
            } else {
                mech.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
            }
            mech.autoSetInternal();

            String thisArmorType = armorType.substring(armorType.indexOf(':') + 1);
            if (thisArmorType.indexOf('(') != -1) {
                boolean clan = thisArmorType.toLowerCase().indexOf("clan") != -1;
                if (clan) {
                    switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                        case 2:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_TW);
                            break;
                        case 3:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED);
                            break;
                        case 4:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                            break;
                        case 5:
                            mech.setArmorTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                            break;
                        default:
                            throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                    }
                } else {
                    switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                        case 1:
                            mech.setArmorTechLevel(TechConstants.T_INTRO_BOXSET);
                            break;
                        case 2:
                            mech.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX);
                            break;
                        case 3:
                            mech.setArmorTechLevel(TechConstants.T_IS_ADVANCED);
                            break;
                        case 4:
                            mech.setArmorTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                            break;
                        case 5:
                            mech.setArmorTechLevel(TechConstants.T_IS_UNOFFICIAL);
                            break;
                        default:
                            throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                    }
                }
                thisArmorType = thisArmorType.substring(0, thisArmorType.indexOf('(')).trim();
                mech.setArmorType(thisArmorType);
            } else if (!thisArmorType.equals(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK))) {
                mech.setArmorTechLevel(mech.getTechLevel());
                mech.setArmorType(thisArmorType);
            }
            if (!(thisArmorType.length() > 0)) {
                mech.setArmorType(EquipmentType.T_ARMOR_STANDARD);
            }
            for (int x = 0; x < locationOrder.length; x++) {
                if ((locationOrder[x] == Mech.LOC_CLEG) && !(mech instanceof TripodMech)) {
                    continue;
                }
                mech.initializeArmor(Integer.parseInt(armorValues[x].substring(armorValues[x].lastIndexOf(':') + 1)), locationOrder[x]);
                if (thisArmorType.equals(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK))) {
                    boolean clan = false;
                    if (armorValues[x].contains("Clan")) {
                        clan = true;
                    }
                    String armorName = armorValues[x].substring(armorValues[x].indexOf(':') + 1, armorValues[x].indexOf('('));
                    if (!armorName.contains("Clan")
                        && !armorName.contains("IS")) {
                        if (clan) {
                            armorName = "Clan " + armorName;
                        } else {
                            armorName = "IS " + armorName;
                        }
                    }
                    mech.setArmorType(EquipmentType.getArmorType(EquipmentType.get(armorName)), locationOrder[x]);
                    if (armorValues[x].toLowerCase().indexOf("clan") != -1) {
                        switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                            case 2:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_TW, locationOrder[x]);
                                break;
                            case 3:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED, locationOrder[x]);
                                break;
                            case 4:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_EXPERIMENTAL, locationOrder[x]);
                                break;
                            case 5:
                                mech.setArmorTechLevel(TechConstants.T_CLAN_UNOFFICIAL, locationOrder[x]);
                                break;
                            default:
                                throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                        }
                    } else if (armorValues[x].toLowerCase().indexOf("inner sphere") != -1) {
                        switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                            case 1:
                                mech.setArmorTechLevel(TechConstants.T_INTRO_BOXSET, locationOrder[x]);
                                break;
                            case 2:
                                mech.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX, locationOrder[x]);
                                break;
                            case 3:
                                mech.setArmorTechLevel(TechConstants.T_IS_ADVANCED, locationOrder[x]);
                                break;
                            case 4:
                                mech.setArmorTechLevel(TechConstants.T_IS_EXPERIMENTAL, locationOrder[x]);
                                break;
                            case 5:
                                mech.setArmorTechLevel(TechConstants.T_IS_UNOFFICIAL, locationOrder[x]);
                                break;
                            default:
                                throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                        }
                    }
                }
            }
            for (int x = 0; x < rearLocationOrder.length; x++) {
                mech.initializeRearArmor(Integer.parseInt(armorValues[x + locationOrder.length].substring(10)), rearLocationOrder[x]);
            }

            // oog, crits.
            compactCriticals(mech);
            // we do these in reverse order to get the outermost
            // locations first, which is necessary for split crits to work
            for (int i = mech.locations() - 1; i >= 0; i--) {
                parseCrits(mech, i);
            }

            if (mech.isClan()) {
                mech.addClanCase();
            }

            // add any heat sinks not allocated
            if (laserSinks) {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_LASER_HEAT_SINK);
            } else if (dblSinks) {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_DOUBLE_HEAT_SINK);
            } else if (compactSinks) {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_COMPACT_HEAT_SINK);
            } else {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), MiscType.F_HEAT_SINK);
            }

            if (mech.isOmni() && mech.hasEngine()) {
                if (baseHeatSinks >= 10) {
                    mech.getEngine().setBaseChassisHeatSinks(baseHeatSinks);
                } else {
                    mech.getEngine().setBaseChassisHeatSinks(expectedSinks);
                }
            }

            mech.getFluff().setCapabilities(capabilities);
            mech.getFluff().setOverview(overview);
            mech.getFluff().setDeployment(deployment);
            mech.getFluff().setHistory(history);
            mech.getFluff().setMMLImagePath(imagePath);

            mech.setArmorTonnage(mech.getArmorWeight());

            if (bv != 0) {
                mech.setUseManualBV(true);
                mech.setManualBV(bv);
            }

            return mech;
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("NumberFormatException parsing file");
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("NullPointerException parsing file");
        } catch (StringIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("StringIndexOutOfBoundsException parsing file");
        }
    }

    private void parseCrits(Mech mech, int loc) throws EntityLoadingException {
        // check for removed arm actuators
        if (!(mech instanceof QuadMech)) {
            if ((loc == Mech.LOC_LARM) || (loc == Mech.LOC_RARM)) {
                String toCheck = critData[loc][3];
                if (toCheck.toLowerCase().trim().endsWith(ARMORED)) {
                    toCheck = toCheck.substring(0, toCheck.length() - ARMORED.length()).trim();
                }
                if (!toCheck.equals("Hand Actuator")) {
                    mech.setCritical(loc, 3, null);
                }
                toCheck = critData[loc][2];
                if (toCheck.toLowerCase().trim().endsWith(ARMORED)) {
                    toCheck = toCheck.substring(0, toCheck.length() - ARMORED.length()).trim();
                }
                if (!toCheck.equals("Lower Arm Actuator")) {
                    mech.setCritical(loc, 2, null);
                }
            }
        }

        // go thru file, add weapons
        for (int i = 0; i < mech.getNumberOfCriticals(loc); i++) {

            // parse out and add the critical
            String critName = critData[loc][i];

            critName = critName.trim();
            boolean rearMounted = false;
            boolean isArmored = false;
            boolean isTurreted = false;

            // Check for Armored Actuators
            if (critName.toLowerCase().trim().endsWith(ARMORED)) {
                critName = critName.substring(0, critName.length() - ARMORED.length()).trim();
                isArmored = true;
            }

            if (critName.equalsIgnoreCase("Fusion Engine") || critName.equalsIgnoreCase("Engine")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Life Support")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Sensors")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Cockpit")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, true, isArmored));
                continue;
            } else if (critName.equalsIgnoreCase("Gyro")) {
                mech.setCritical(loc, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, true, isArmored));
                continue;
            } else if ((critName.indexOf("Actuator") != -1) || critName.equalsIgnoreCase("Shoulder") || critName.equalsIgnoreCase("Hip")) {
                mech.getCritical(loc, i).setArmored(isArmored);
                continue;
            }
            // if the slot's full already, skip it.
            if (mech.getCritical(loc, i) != null) {
                continue;
            }

            if (critName.toUpperCase().endsWith("(T)")) {
                isTurreted = true;
                critName = critName.substring(0, critName.length() - 3).trim();
            }

            if (critName.toUpperCase().endsWith("(R)")) {
                rearMounted = true;
                critName = critName.substring(0, critName.length() - 3).trim();
            }
            if (critName.toLowerCase().endsWith("(split)")) {
                critName = critName.substring(0, critName.length() - 7).trim();
            }
            // keep track of facing for vehicular grenade launchers
            int facing = -1;
            if (critName.toUpperCase().endsWith("(FL)")) {
                facing = 5;
                critName = critName.substring(0, critName.length() - 4).trim();
            }
            if (critName.toUpperCase().endsWith("(FR)")) {
                facing = 1;
                critName = critName.substring(0, critName.length() - 4).trim();
            }
            if (critName.toUpperCase().endsWith("(RL)")) {
                facing = 4;
                critName = critName.substring(0, critName.length() - 4).trim();
            }
            if (critName.toUpperCase().endsWith("(RR)")) {
                facing = 2;
                critName = critName.substring(0, critName.length() - 4).trim();
            }
            EquipmentType etype2 = null;
            if (critName.contains("|")) {
                String critName2 = critName.substring(critName.indexOf("|") + 1);
                etype2 = EquipmentType.get(critName2);
                if (etype2 == null) {
                    etype2 = EquipmentType.get(mech.isClan() ? "Clan " + critName2 : "IS " + critName2);
                }
                critName = critName.substring(0, critName.indexOf("|"));
            }

            try {
                EquipmentType etype = EquipmentType.get(critName);
                if (etype == null) {
                    etype = EquipmentType.get(mech.isClan() ? "Clan " + critName : "IS " + critName);
                }
                if (etype != null) {
                    if (etype.isSpreadable()) {
                        // do we already have one of these? Key on Type
                        Mounted m = hSharedEquip.get(etype);
                        if (m != null) {
                            // use the existing one
                            mech.addCritical(loc, new CriticalSlot(m));
                            continue;
                        }
                        m = mech.addEquipment(etype, loc, rearMounted,
                                              BattleArmor.MOUNT_LOC_NONE, isArmored,
                                              isTurreted);
                        hSharedEquip.put(etype, m);
                    } else if (((etype instanceof WeaponType) && ((WeaponType) etype).isSplitable()) || ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_SPLITABLE))) {
                        // do we already have this one in this or an outer
                        // location?
                        Mounted m = null;
                        boolean bFound = false;
                        for (int x = 0, n = vSplitWeapons.size(); x < n; x++) {
                            m = vSplitWeapons.elementAt(x);
                            int nLoc = m.getLocation();
                            if (((nLoc == loc) || (loc == Mech.getInnerLocation(nLoc))) && (m.getType() == etype)) {
                                bFound = true;
                                break;
                            }
                        }
                        if (bFound && (m != null)) {
                            m.setFoundCrits(m.getFoundCrits() + 1);
                            if (m.getFoundCrits() >= etype.getCriticals(mech)) {
                                vSplitWeapons.removeElement(m);
                            }
                            // if we're in a new location, set the weapon as
                            // split
                            if (loc != m.getLocation()) {
                                m.setSplit(true);
                            }
                            // give the most restrictive location for arcs
                            int help = m.getLocation();
                            m.setLocation(Mech.mostRestrictiveLoc(loc, help));
                            if (loc != help) {
                                m.setSecondLocation(Mech.leastRestrictiveLoc(loc, help));
                            }
                        } else {
                            // make a new one
                            m = new Mounted(mech, etype);
                            m.setFoundCrits(1);
                            m.setArmored(isArmored);
                            m.setMechTurretMounted(isTurreted);
                            vSplitWeapons.addElement(m);
                        }
                        m.setArmored(isArmored);
                        m.setMechTurretMounted(isTurreted);
                        mech.addEquipment(m, loc, rearMounted);
                    } else {
                        Mounted mount = null;
                        if (etype2 == null) {
                            mount = mech.addEquipment(etype, loc, rearMounted,
                                                      BattleArmor.MOUNT_LOC_NONE, isArmored,
                                                      isTurreted);
                        } else {
                            if (etype instanceof AmmoType) {
                                if (!(etype2 instanceof AmmoType) || (((AmmoType) etype).getAmmoType() != ((AmmoType) etype2).getAmmoType())) {
                                    throw new EntityLoadingException("Can't combine ammo for different weapons in one slot");
                                }
                            } else {
                                if (!(etype.equals(etype2)) || ((etype instanceof MiscType) && (!etype.hasFlag(MiscType.F_HEAT_SINK) && !etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)))) {
                                    throw new EntityLoadingException("must combine ammo or heatsinks in one slot");
                                }
                            }
                            mount = mech.addEquipment(etype, etype2, loc);
                        }
                        // vehicular grenade launchers need to have their facing
                        // set
                        if ((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_VGL)) {
                            if (facing == -1) {
                                // if facing has not been set earlier, we are
                                // front or rear mounted
                                if (rearMounted) {
                                    mount.setFacing(3);
                                } else {
                                    mount.setFacing(0);
                                }
                            } else {
                                mount.setFacing(facing);
                            }
                        }
                    }
                } else {
                    if (!critName.equals(MtfFile.EMPTY)) {
                        // Can't load this piece of equipment!
                        // Add it to the list so we can show the user.
                        mech.addFailedEquipment(critName);
                        // Make the failed equipment an empty slot
                        critData[loc][i] = MtfFile.EMPTY;
                        // Compact criticals again
                        compactCriticals(mech, loc);
                        // Re-parse the same slot, since the compacting
                        // could have moved new equipment to this slot
                        i--;
                    }

                }
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }

    /**
     * This function moves all "empty" slots to the end of a location's critical
     * list. MegaMek adds equipment to the first empty slot available in a
     * location. This means that any "holes" (empty slots not at the end of a
     * location), will cause the file crits and MegaMek's crits to become out of
     * sync.
     */
    private void compactCriticals(Mech mech) {
        for (int loc = 0; loc < mech.locations(); loc++) {
            compactCriticals(mech, loc);
        }
    }

    private void compactCriticals(Mech mech, int loc) {
        if (loc == Mech.LOC_HEAD) {
            // This location has an empty slot inbetween systems crits
            // which will mess up parsing if compacted.
            return;
        }
        int firstEmpty = -1;
        for (int slot = 0; slot < mech.getNumberOfCriticals(loc); slot++) {
            if (critData[loc][slot] == null) {
                critData[loc][slot] = MtfFile.EMPTY;
            }

            if (critData[loc][slot].equals(MtfFile.EMPTY) && (firstEmpty == -1)) {
                firstEmpty = slot;
            }
            if ((firstEmpty != -1) && !critData[loc][slot].equals(MtfFile.EMPTY)) {
                // move this to the first empty slot
                critData[loc][firstEmpty] = critData[loc][slot];
                // mark the old slot empty
                critData[loc][slot] = MtfFile.EMPTY;
                // restart just after the moved slot's new location
                slot = firstEmpty;
                firstEmpty = -1;
            }
        }
    }

    private int getLocation(String location) {

        if (location.trim().equalsIgnoreCase("Left Arm:") || location.trim().equalsIgnoreCase("Front Left Leg:")) {
            return Mech.LOC_LARM;
        }

        if (location.trim().equalsIgnoreCase("Right Arm:") || location.trim().equalsIgnoreCase("Front Right Leg:")) {
            return Mech.LOC_RARM;
        }

        if (location.equalsIgnoreCase("Left Leg:") || location.equalsIgnoreCase("Rear Left Leg:")) {
            return Mech.LOC_LLEG;
        }

        if (location.trim().equalsIgnoreCase("Right Leg:") || location.trim().equalsIgnoreCase("Rear Right Leg:")) {
            return Mech.LOC_RLEG;
        }

        if (location.trim().equalsIgnoreCase("Center Leg:")) {
            return Mech.LOC_CLEG;
        }

        if (location.trim().equalsIgnoreCase("Left Torso:")) {
            return Mech.LOC_LT;
        }

        if (location.trim().equalsIgnoreCase("Right Torso:")) {
            return Mech.LOC_RT;
        }

        if (location.trim().equalsIgnoreCase("Center Torso:")) {
            return Mech.LOC_CT;
        }

        // else
        return Mech.LOC_HEAD;
    }

    private int getArmorLocation(String location) {

        int loc = -1;
        boolean rear = false;
        if (location.trim().toLowerCase().startsWith("la armor:") || location.trim().toLowerCase().startsWith("fll armor:")) {
            loc = Mech.LOC_LARM;
        } else if (location.trim().toLowerCase().startsWith("ra armor:") || location.trim().toLowerCase().startsWith("frl armor:")) {
            loc = Mech.LOC_RARM;
        } else if (location.trim().toLowerCase().startsWith("lt armor:")) {
            loc = Mech.LOC_LT;
        } else if (location.trim().toLowerCase().startsWith("rt armor:")) {
            loc = Mech.LOC_RT;
        } else if (location.trim().toLowerCase().startsWith("ct armor:")) {
            loc = Mech.LOC_CT;
        } else if (location.trim().toLowerCase().startsWith("hd armor:")) {
            loc = Mech.LOC_HEAD;
        } else if (location.trim().toLowerCase().startsWith("ll armor:") || location.trim().toLowerCase().startsWith("rll armor:")) {
            loc = Mech.LOC_LLEG;
        } else if (location.trim().toLowerCase().startsWith("rl armor:") || location.trim().toLowerCase().startsWith("rrl armor:")) {
            loc = Mech.LOC_RLEG;
        } else if (location.trim().toLowerCase().startsWith("rtl armor:")) {
            loc = Mech.LOC_LT;
            rear = true;
        } else if (location.trim().toLowerCase().startsWith("rtr armor:")) {
            loc = Mech.LOC_RT;
            rear = true;
        } else if (location.trim().toLowerCase().startsWith("rtc armor:")) {
            loc = Mech.LOC_CT;
            rear = true;
        } else if (location.trim().toLowerCase().startsWith("cl armor:")) {
            loc = Mech.LOC_CLEG;
        }

        if (!rear) {
            for (int pos = 0; pos < locationOrder.length; pos++) {
                if (locationOrder[pos] == loc) {
                    loc = pos;
                    break;
                }
            }
        } else {
            for (int pos = 0; pos < rearLocationOrder.length; pos++) {
                if (rearLocationOrder[pos] == loc) {
                    loc = pos + locationOrder.length;
                    break;
                }
            }
        }
        return loc;
    }

    private boolean isValidLocation(String location) {

        if (location.trim().equalsIgnoreCase("Left Arm:") || location.trim().equalsIgnoreCase("Right Arm:") || location.equalsIgnoreCase("Left Leg:") || location.trim().equalsIgnoreCase("Right Leg:") || location.trim().equalsIgnoreCase("Center Leg:") || location.trim().equalsIgnoreCase("Front Left Leg:") || location.trim().equalsIgnoreCase("Front Right Leg:") || location.trim().equalsIgnoreCase("Rear Left Leg:") || location.trim().equalsIgnoreCase("Rear Right Leg:") || location.trim().equalsIgnoreCase("Left Torso:") || location.trim().equalsIgnoreCase("Right Torso:") || location.trim().equalsIgnoreCase("Center Torso:") || location.trim().equalsIgnoreCase("Head:")) {
            return true;
        }

        // else
        return false;
    }

    private boolean isProcessedComponent(String line) {

        if (line.trim().toLowerCase().startsWith("cockpit:")) {
            cockpitType = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("gyro:")) {
            gyroType = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("ejection:")) {
            ejectionType = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("mass:")) {
            tonnage = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("engine:")) {
            engine = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("structure:")) {
            internalType = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("myomer:")) {
            myomerType = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("config:")) {
            chassisConfig = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("techbase:")) {
            techBase = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("era:")) {
            techYear = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("source:")) {
            source = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("rules level:")) {
            rulesLevel = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("heat sinks:")) {
            heatSinks = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("base chassis heat sinks:")) {
            baseChassieHeatSinks = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("walk mp:")) {
            walkMP = line;
            return true;
        }
        if (line.trim().toLowerCase().startsWith("jump mp:")) {
            jumpMP = line;
            return true;
        }

        if (line.trim().toLowerCase().startsWith("armor:")) {
            armorType = line;
            return true;
        }
        
        if (line.trim().toLowerCase().startsWith("overview:")) {
            overview = line.substring("overview:".length());
            return true;
        }

        if (line.trim().toLowerCase().startsWith("capabilities:")) {
            capabilities = line.substring("capabilities:".length());
            return true;
        }
                
        if (line.trim().toLowerCase().startsWith("deployment:")) {
            deployment = line.substring("deployment:".length());
            return true;
        }
        
        if (line.trim().toLowerCase().startsWith("history:")) {
            history = line.substring("history:".length());
            return true;
        }

        if (line.trim().toLowerCase().startsWith("imagefile:")) {
            imagePath = line.substring("imagefile:".length());
            return true;
        }

        if (line.trim().toLowerCase().startsWith("bv:")) {
            bv = Integer.parseInt(line.substring("bv:".length()));
            return true;
        }

        return false;
    }

    private int weaponsList(String line) {
        if (line.trim().toLowerCase().startsWith("weapons:")) {
            return Integer.parseInt(line.substring(8));
        }

        return -1;
    }
}
