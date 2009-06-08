/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
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

import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * @author Ben
 * @version
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

    String heatSinks;
    String walkMP;
    String jumpMP;
    String baseChassieHeatSinks = "base chassis heat sinks:-1";

    String armorType;
    String[] armorValues = new String[11];

    String weaponCount;
    String[] weaponData;

    String[][] critData;


    Hashtable<EquipmentType, Mounted> hSharedEquip = new Hashtable<EquipmentType, Mounted>();
    Vector<Mounted> vSplitWeapons = new Vector<Mounted>();

    public static final int locationOrder[] = { Mech.LOC_LARM, Mech.LOC_RARM,
            Mech.LOC_LT, Mech.LOC_RT, Mech.LOC_CT, Mech.LOC_HEAD,
            Mech.LOC_LLEG, Mech.LOC_RLEG };
    public static final int rearLocationOrder[] = { Mech.LOC_LT, Mech.LOC_RT,
            Mech.LOC_CT };

    public static final String EMPTY = "-Empty-";
    public static final String ARMORED = "(armored)";

    /** Creates new MtfFile */
    public MtfFile(InputStream is) throws EntityLoadingException {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));

            version = r.readLine();
            // Version 1.0: Initial version.
            // Version 1.1: Added level 3 cockpit and gyro options.
            if (!version.trim().equalsIgnoreCase("Version:1.0")
                    && !version.trim().equalsIgnoreCase("Version:1.1")) {
                throw new EntityLoadingException("Wrong MTF file version.");
            }

            name = r.readLine();
            model = r.readLine();

            critData = new String[8][12];

            readCrits(r);

            r.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("I/O Error reading file");
        } catch (StringIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException(
                    "StringIndexOutOfBoundsException reading file (format error)");
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException(
                    "NumberFormatException reading file (format error)");
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
                iCockpitType = Mech.getCockpitTypeForString(cockpitType
                        .substring(8));
                if (iCockpitType == Mech.COCKPIT_UNKNOWN) {
                    iCockpitType = Mech.COCKPIT_STANDARD;
                }
            } catch (Exception e) {
                iCockpitType = Mech.COCKPIT_STANDARD;
            }
            if (chassisConfig.indexOf("Quad") != -1) {
                mech = new QuadMech(iGyroType, iCockpitType);
            } else if (chassisConfig.indexOf("LAM") != -1) {
                mech = new LandAirMech(iGyroType, iCockpitType);
            } else {
                mech = new BipedMech(iGyroType, iCockpitType);
            }

            // aarg! those stupid sub-names in parenthesis screw everything up
            // we may do something different in the future, but for now, I'm
            // going to strip them out
            int pindex = name.indexOf("(");
            if (pindex == -1) {
                mech.setChassis(name.trim());
            } else {
                mech.setChassis(name.substring(0, pindex - 1).trim());
            }
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
                        throw new EntityLoadingException(
                                "Unsupported tech level: "
                                        + rulesLevel.substring(12).trim());
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
                        throw new EntityLoadingException(
                                "Unsupported tech level: "
                                        + rulesLevel.substring(12).trim());
                }
            } else if (techBase.substring(9).trim()
                    .equals("Mixed (IS Chassis)")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
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
                    throw new EntityLoadingException(
                            "Unsupported tech level: "
                                    + rulesLevel.substring(12).trim());
                }
                mech.setMixedTech(true);
            } else if (techBase.substring(9).trim().equals(
                    "Mixed (Clan Chassis)")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
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
                    throw new EntityLoadingException(
                            "Unsupported tech level: "
                                    + rulesLevel.substring(12).trim());
                }
                mech.setMixedTech(true);
            } else if (techBase.substring(9).trim().equals("Mixed")) {
                throw new EntityLoadingException(
                        "Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
            } else {
                throw new EntityLoadingException("Unsupported tech base: "
                        + techBase.substring(9).trim());
            }

            mech.setWeight(Integer.parseInt(tonnage.substring(5)));

            int engineFlags = 0;
            if ((mech.isClan() && !mech.isMixedTech())
                    || (mech.isMixedTech() && mech.isClan() && !mech
                            .itemOppositeTech(engine))) {
                engineFlags = Engine.CLAN_ENGINE;
            }

            int engineRating = Integer.parseInt(engine.substring(engine
                    .indexOf(":") + 1, engine.indexOf(" ")));
            mech.setEngine(new Engine(engineRating, Engine
                    .getEngineTypeByString(engine), engineFlags));

            mech.setOriginalJumpMP(Integer.parseInt(jumpMP.substring(8)));

            boolean dblSinks = (heatSinks.substring(14).equalsIgnoreCase("Double"));

            boolean laserSinks = heatSinks.substring(14).equalsIgnoreCase("Laser");

            int expectedSinks = Integer.parseInt(heatSinks.substring(11, 13).trim());

            int baseHeatSinks = Integer.parseInt(baseChassieHeatSinks.substring("base chassis heat sinks:".length()).trim());

            String thisStructureType = internalType.substring(internalType.indexOf(':') + 1);
            if (thisStructureType.length() > 0) {
                mech.setStructureType(thisStructureType);
            } else {
                mech.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
            }
            mech.autoSetInternal();

            String thisArmorType = armorType
                    .substring(armorType.indexOf(':') + 1);
            if (thisArmorType.indexOf('(') != -1) {
                if (thisArmorType.toLowerCase().indexOf("clan") != -1) {
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
                        throw new EntityLoadingException(
                                "Unsupported tech level: "
                                + rulesLevel.substring(12).trim());
                    }
                } else if (thisArmorType.toLowerCase().indexOf("inner sphere") != -1) {
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
                        throw new EntityLoadingException(
                                "Unsupported tech level: "
                                + rulesLevel.substring(12).trim());
                    }
                }
                thisArmorType = thisArmorType.substring(0, thisArmorType.indexOf('('));
            } else {
                mech.setArmorTechLevel(mech.getTechLevel());
            }
            if (thisArmorType.length() > 0) {
                mech.setArmorType(thisArmorType);
            } else {
                mech.setArmorType(EquipmentType.T_ARMOR_STANDARD);
            }
            for (int x = 0; x < locationOrder.length; x++) {
                mech.initializeArmor(Integer.parseInt(armorValues[x]
                        .substring(armorValues[x].indexOf(':')+1)), locationOrder[x]);
            }
            for (int x = 0; x < rearLocationOrder.length; x++) {
                mech.initializeRearArmor(Integer.parseInt(armorValues[x
                        + locationOrder.length].substring(10)),
                        rearLocationOrder[x]);
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
            if ( laserSinks ){
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), "CLLaser Heat Sink");
            } else {
                mech.addEngineSinks(expectedSinks - mech.heatSinks(), dblSinks);
            }

            if (mech.isOmni()) {
                if (baseHeatSinks >= 10) {
                    mech.getEngine().setBaseChassisHeatSinks(baseHeatSinks);
                } else {
                    mech.getEngine().setBaseChassisHeatSinks(expectedSinks);
                }
            }
            return mech;
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException(
                    "NumberFormatException parsing file");
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException(
                    "NullPointerException parsing file");
        } catch (StringIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException(
                    "StringIndexOutOfBoundsException parsing file");
        }
    }

    private void parseCrits(Mech mech, int loc) throws EntityLoadingException {
        // check for removed arm actuators
        if (!(mech instanceof QuadMech)) {
            if ((loc == Mech.LOC_LARM) || (loc == Mech.LOC_RARM)) {
                if (!critData[loc][3].equals("Hand Actuator")) {
                    mech.setCritical(loc, 3, null);
                }
                if (!critData[loc][2].equals("Lower Arm Actuator")) {
                    mech.setCritical(loc, 2, null);
                }
            }
        }

        // go thru file, add weapons
        for (int i = 0; i < mech.getNumberOfCriticals(loc); i++) {

            // parse out and add the critical
            String critName = critData[loc][i];

            critName.trim();
            boolean rearMounted = false;
            boolean isArmored = false;

            // Check for Armored Actuators
            if (critName.toLowerCase().trim().endsWith(ARMORED)) {
                critName = critName.substring(0, critName.length() - ARMORED.length()).trim();
                isArmored = true;
            }

            if (critName.equalsIgnoreCase("Fusion Engine") || critName.equalsIgnoreCase("Engine")) {
                mech.setCritical(loc, i, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, true, isArmored, null));
                continue;
            } else if (critName.equalsIgnoreCase("Life Support")) {
                mech.setCritical(loc, i, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, true, isArmored, null));
                continue;
            } else if (critName.equalsIgnoreCase("Sensors")) {
                mech.setCritical(loc, i, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, true, isArmored, null));
                continue;
            } else if (critName.equalsIgnoreCase("Cockpit")) {
                mech.setCritical(loc, i, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, true, isArmored, null));
                continue;
            } else if (critName.equalsIgnoreCase("Gyro")) {
                mech.setCritical(loc, i, new CriticalSlot(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, true, isArmored, null));
                continue;
            } else if ((critName.indexOf("Actuator") != -1) || critName.equalsIgnoreCase("Shoulder") || critName.equalsIgnoreCase("Hip")) {
                mech.getCritical(loc, i).setArmored(isArmored);
                continue;
            }
            // if the slot's full already, skip it.
            if (mech.getCritical(loc, i) != null) {
                continue;
            }

            if (critName.toUpperCase().endsWith("(R)")) {
                rearMounted = true;
                critName = critName.substring(0, critName.length() - 3).trim();
            }
            if (critName.toLowerCase().endsWith("(split)")) {
                critName = critName.substring(0, critName.length() - 7).trim();
            }
            if (critName.equalsIgnoreCase("Armored Cowl")) {
                mech.setCowl(5); // Cowl starts with 5 points of armor
            }

            try {
                EquipmentType etype = EquipmentType.get(critName);
                if (etype != null) {
                    if (etype.isSpreadable()) {
                        // do we already have one of these? Key on Type
                        Mounted m = hSharedEquip.get(etype);
                        if (m != null) {
                            // use the existing one
                            mech.addCritical(loc, new CriticalSlot(
                                    CriticalSlot.TYPE_EQUIPMENT, mech
                                            .getEquipmentNum(m), etype
                                            .isHittable(), isArmored, m));
                            continue;
                        }
                        m = mech.addEquipment(etype, loc, rearMounted);
                        m.setArmored(isArmored);
                        hSharedEquip.put(etype, m);
                    } else if ((etype instanceof WeaponType)
                            && etype.hasFlag(WeaponType.F_SPLITABLE)) {
                        // do we already have this one in this or an outer
                        // location?
                        Mounted m = null;
                        boolean bFound = false;
                        for (int x = 0, n = vSplitWeapons.size(); x < n; x++) {
                            m = vSplitWeapons.elementAt(x);
                            int nLoc = m.getLocation();
                            if (((nLoc == loc) || (loc == Mech
                                    .getInnerLocation(nLoc)))
                                    && (m.getType() == etype)) {
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
                                m.setSecondLocation(Mech.leastRestrictiveLoc(
                                        loc, help));
                            }
                        } else {
                            // make a new one
                            m = new Mounted(mech, etype);
                            m.setFoundCrits(1);
                            m.setArmored(isArmored);
                            vSplitWeapons.addElement(m);
                        }
                        m.setArmored(isArmored);
                        mech.addEquipment(m, loc, rearMounted);
                    } else {
                        mech.addEquipment(etype, loc, rearMounted, false, isArmored);
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
        }

        else if (location.trim().toLowerCase().startsWith("rtc armor:")) {
            loc = Mech.LOC_CT;
            rear = true;
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

        if (location.trim().equalsIgnoreCase("Left Arm:")
                || location.trim().equalsIgnoreCase("Right Arm:")
                || location.equalsIgnoreCase("Left Leg:")
                || location.trim().equalsIgnoreCase("Right Leg:")
                || location.trim().equalsIgnoreCase("Front Left Leg:")
                || location.trim().equalsIgnoreCase("Front Right Leg:")
                || location.trim().equalsIgnoreCase("Rear Left Leg:")
                || location.trim().equalsIgnoreCase("Rear Right Leg:")
                || location.trim().equalsIgnoreCase("Left Torso:")
                || location.trim().equalsIgnoreCase("Right Torso:")
                || location.trim().equalsIgnoreCase("Center Torso:")
                || location.trim().equalsIgnoreCase("Head:")) {
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

        if (line.trim().toLowerCase().startsWith("base chassie heat sinks:")) {
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

        return false;
    }

    private int weaponsList(String line) {
        if (line.trim().toLowerCase().startsWith("weapons:")) {
            return Integer.parseInt(line.substring(8));
        }

        return -1;
    }
}
