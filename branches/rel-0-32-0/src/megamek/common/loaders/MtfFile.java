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

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 *
 * @author  Ben
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

    String tonnage;
    String engine;
    String internalType;
    String myomerType;
    String gyroType;
    String cockpitType;

    String heatSinks;
    String walkMP;
    String jumpMP;

    String armorType;
    String[] armorValues = new String[11];

    String weaponCount;
    String[] weaponData;

    String[][] critData;

    Hashtable hSharedEquip = new Hashtable();
    Vector vSplitWeapons = new Vector();

    public static final int locationOrder[] = { Mech.LOC_LARM, Mech.LOC_RARM,
                                                Mech.LOC_LT, Mech.LOC_RT,
                                                Mech.LOC_CT, Mech.LOC_HEAD,
                                                Mech.LOC_LLEG, Mech.LOC_RLEG };
    public static final int rearLocationOrder[] = { Mech.LOC_LT, Mech.LOC_RT,
                                                    Mech.LOC_CT };

    public static final String EMPTY = "-Empty-";

    /** Creates new MtfFile */
    public MtfFile(InputStream is) throws EntityLoadingException {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));

            version = r.readLine();
            //Version 1.0: Initial version.
            //Version 1.1: Added level 3 cockpit and gyro options.
            if (!version.trim().equalsIgnoreCase("Version:1.0")
                && !version.trim().equalsIgnoreCase("Version:1.1")) {
                throw new EntityLoadingException("Wrong MTF file version.");
            }

            name = r.readLine();
            model = r.readLine();

            r.readLine();

            chassisConfig = r.readLine();
            techBase = r.readLine();
            techYear = r.readLine();
            rulesLevel = r.readLine();

            r.readLine();

            // The next line might either be blank or a system type.
            String tmp = r.readLine();
            while ((tmp != null) && (tmp.length() > 0)) {
                if (tmp.startsWith("Cockpit:")) {
                    cockpitType = tmp;
                } else if (tmp.startsWith("Gyro:")) {
                    gyroType = tmp;
                } else if (tmp.startsWith("Mass:")) {
                    tonnage = tmp;
                } else if (tmp.startsWith("Engine:")) {
                    engine = tmp;
                } else if (tmp.startsWith("Structure:")) {
                    internalType = tmp;
                } else if (tmp.startsWith("Myomer:")) {
                    myomerType = tmp;
                }
                tmp = r.readLine();
            }


            heatSinks = r.readLine();
            walkMP = r.readLine();
            jumpMP = r.readLine();

            r.readLine();

            armorType = r.readLine();
            for (int x = 0; x < armorValues.length; x++) {
                armorValues[x] = r.readLine();
            }

            r.readLine();

            weaponCount = r.readLine();

            int weapons = Integer.parseInt(weaponCount.substring(8));
            weaponData = new String[weapons];
            for(int i = 0; i < weapons; i++) {
                weaponData[i] = r.readLine();
            }

            critData = new String[8][12];

            for (int x = 0; x < locationOrder.length; x++) {
                readCrits(r, locationOrder[x]);
            }

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

    private void readCrits(BufferedReader r, int loc) throws IOException {
        r.readLine(); // blank line
        r.readLine(); // location name.... verify?

        for (int i = 0; i < 12; i++) {
            critData[loc][i] = r.readLine();
        }
    }

    public Entity getEntity() throws EntityLoadingException {
        try {
            Mech mech;

            int iGyroType = Mech.GYRO_STANDARD;
            try {
                iGyroType = Mech.getGyroTypeForString(gyroType.substring(5));
                if (iGyroType == Mech.GYRO_UNKNOWN)
                    iGyroType = Mech.GYRO_STANDARD;
            } catch (Exception e) {
                iGyroType = Mech.GYRO_STANDARD;
            }
            int iCockpitType = Mech.COCKPIT_STANDARD;
            try {
                iCockpitType = Mech.getCockpitTypeForString(cockpitType.substring(8));
                if (iCockpitType == Mech.COCKPIT_UNKNOWN)
                    iCockpitType = Mech.COCKPIT_STANDARD;
            } catch (Exception e) {
                iCockpitType = Mech.COCKPIT_STANDARD;
            }
            if (chassisConfig.indexOf("Quad") != -1) {
                mech = new QuadMech(iGyroType, iCockpitType);
            } else {
                mech = new BipedMech(iGyroType, iCockpitType);
            }

            // aarg!  those stupid sub-names in parenthesis screw everything up
            // we may do something different in the future, but for now, I'm
            // going to strip them out
            int pindex = name.indexOf("(");
            if (pindex == -1) {
                mech.setChassis(name.trim());
            } else {
                mech.setChassis(name.substring(0, pindex - 1).trim());
            }
            mech.setModel(model.trim());
            mech.setYear(Integer.parseInt(this.techYear.substring(4).trim()));
            if (chassisConfig.indexOf("Omni") != -1) {
                mech.setOmni(true);
            }

            if (techBase.substring(9).trim().equals("Inner Sphere")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 1 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_1);
                    break;
                case 2 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_2);
                    break;
                case 3 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_3);
                    break;
                default :
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                }
            } else if (techBase.substring(9).trim().equals("Clan")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 2 :
                    mech.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
                    break;
                case 3 :
                    mech.setTechLevel(TechConstants.T_CLAN_LEVEL_3);
                    break;
                default :
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                }
            } else if (techBase.substring(9).trim().equals("Mixed (IS Chassis)")) {
                mech.setTechLevel(TechConstants.T_IS_LEVEL_3);
                mech.setMixedTech(true);
            } else if (techBase.substring(9).trim().equals("Mixed (Clan Chassis)")) {
                mech.setTechLevel(TechConstants.T_CLAN_LEVEL_3);
                mech.setMixedTech(true);
            } else if (techBase.substring(9).trim().equals("Mixed")) {
                throw new EntityLoadingException("Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
            } else {
                throw new EntityLoadingException("Unsupported tech base: " + techBase.substring(9).trim());
            }

            mech.setWeight(Integer.parseInt(tonnage.substring(5)));

            int engineFlags = 0;
            if ((mech.isClan() && !mech.isMixedTech())
                || (mech.isMixedTech() && mech.isClan() && !mech.itemOppositeTech(engine))) {
                engineFlags = Engine.CLAN_ENGINE;
            }
            int engineRating = Integer.parseInt(engine.substring(engine.indexOf(":")+1,engine.indexOf(" ")));
            mech.setEngine(new Engine(engineRating,
                                      Engine.getEngineTypeByString(engine),
                                      engineFlags));

            mech.setOriginalJumpMP(Integer.parseInt(jumpMP.substring(8)));

            boolean dblSinks = (heatSinks.substring(14).equalsIgnoreCase("Double") ||
                                heatSinks.substring(14).equalsIgnoreCase("Laser"));
            int expectedSinks = Integer.parseInt(heatSinks.substring(11, 14).trim());

            String thisStructureType = internalType.substring(internalType.indexOf(':')+1);
            if (thisStructureType.length() > 0) {
                mech.setStructureType(thisStructureType);
            } else {
                mech.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
            }
            mech.autoSetInternal();

            String thisArmorType = armorType.substring(armorType.indexOf(':')+1);
            if (thisArmorType.length() > 0) {
                mech.setArmorType(thisArmorType);
            } else {
                mech.setArmorType(EquipmentType.T_ARMOR_STANDARD);
            }
            for (int x = 0; x < locationOrder.length; x++) {
                mech.initializeArmor(Integer.parseInt(armorValues[x].substring(9)), locationOrder[x]);
            }
            for (int x = 0; x < rearLocationOrder.length; x++) {
                mech.initializeRearArmor(Integer.parseInt(armorValues[x + locationOrder.length].substring(10)), rearLocationOrder[x]);
            }

            // oog, crits.
            compactCriticals(mech);
            // we do these in reverse order to get the outermost
            //  locations first, which is necessary for split crits to work
            for (int i = mech.locations() - 1; i >= 0; i--) {
                parseCrits(mech, i);
            }

            if (mech.isClan()) {
                mech.addClanCase();
            }

            // add any heat sinks not allocated
            mech.addEngineSinks(expectedSinks - mech.heatSinks(), dblSinks);

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
            if (loc == Mech.LOC_LARM || loc == Mech.LOC_RARM) {
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
            // if the slot's full already, skip it.
            if (mech.getCritical(loc, i) != null) {
                continue;
            }

            // parse out and add the critical
            String critName = critData[loc][i];
            critName.trim();
            boolean rearMounted = false;
            boolean split = false;

            if (critName.equalsIgnoreCase("Fusion Engine")
                    || critName.equalsIgnoreCase("Engine")) {
                mech.setCritical(loc,i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE));
                continue;
            } else if (critName.equalsIgnoreCase("Life Support")) {
                mech.setCritical(loc,i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT));
                continue;
            } else if (critName.equalsIgnoreCase("Sensors")) {
                mech.setCritical(loc,i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS));
                continue;
            } else if (critName.equalsIgnoreCase("Cockpit")) {
                mech.setCritical(loc,i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT));
                continue;
            } else if (critName.equalsIgnoreCase("Gyro")) {
                mech.setCritical(loc,i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO));
                continue;
            }

            if (critName.endsWith("(R)")) {
                rearMounted = true;
                critName = critName.substring(0, critName.length() - 3).trim();
            }
            if (critName.endsWith("(Split)")) {
                split = true;
                critName = critName.substring(0, critName.length() - 7).trim();
            }
            if (critName.equalsIgnoreCase("Armored Cowl")) {
                mech.setCowl(5); // Cowl starts with 5 points of armor
            }
            
            try {
                EquipmentType etype = EquipmentType.get(critName);
                if (etype != null) {
                    if (etype.isSpreadable()) {
                        // do we already have one of these?  Key on Type
                        Mounted m = (Mounted)hSharedEquip.get(etype);
                        if (m != null) {
                            // use the existing one
                            mech.addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                                    mech.getEquipmentNum(m), etype.isHittable()));
                            continue;
                        }
						m = mech.addEquipment(etype, loc, rearMounted);
						hSharedEquip.put(etype, m);
                    }
                    else if (etype instanceof WeaponType &&
                             etype.hasFlag(WeaponType.F_SPLITABLE)) {
                        // do we already have this one in this or an outer location?
                        Mounted m = null;
                        boolean bFound = false;
                        for (int x = 0, n = vSplitWeapons.size(); x < n; x++) {
                            m = (Mounted)vSplitWeapons.elementAt(x);
                            int nLoc = m.getLocation();
                            if ((nLoc == loc || loc == Mech.getInnerLocation(nLoc))
                                        && m.getType() == etype) {
                                bFound = true;
                                break;
                            }
                        }
                        if (bFound) {
                            m.setFoundCrits(m.getFoundCrits() + 1);
                            if (m.getFoundCrits() >= etype.getCriticals(mech)) {
                                vSplitWeapons.removeElement(m);
                            }
                            // if we're in a new location, set the weapon as split
                            if (loc != m.getLocation()) {
                                m.setSplit(true);
                            }
                            // give the most restrictive location for arcs
                            int help=m.getLocation();
                            m.setLocation(Mech.mostRestrictiveLoc(
                                    loc, help));
                            if (loc!=help) {
                                m.setSecondLocation(Mech.leastRestrictiveLoc(
                                    loc, help));
                            }
                        }
                        else {
                            // make a new one
                            m = new Mounted(mech, etype);
                            m.setFoundCrits(1);
                            vSplitWeapons.addElement(m);
                        }
                        mech.addEquipment(m, loc, rearMounted);
                    }
                    else {
                        mech.addEquipment(etype, loc, rearMounted);
                    }
                } else {
                    if (!critName.equals(MtfFile.EMPTY)) {
                        //Can't load this piece of equipment!
                        // Add it to the list so we can show the user.
                        mech.addFailedEquipment(critName);
                        // Make the failed equipment an empty slot
                        critData[loc][i] = MtfFile.EMPTY;
                        // Compact criticals again
                        compactCriticals(mech, loc);
                        // Re-parse the same slot, since the compacting
                        //  could have moved new equipment to this slot
                        i--;
                    }

                }
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }
    }

    /**
     * This function moves all "empty" slots to the end of a location's
     * critical list.
     *
     * MegaMek adds equipment to the first empty slot available in a
     * location.  This means that any "holes" (empty slots not at the
     * end of a location), will cause the file crits and MegaMek's crits
     * to become out of sync.
     */
    private void compactCriticals(Mech mech) {
        for (int loc = 0; loc < mech.locations(); loc++) {
            compactCriticals(mech, loc);
        }
    }

    private void compactCriticals(Mech mech, int loc) {
        if (loc == Mech.LOC_HEAD) {
            //This location has an empty slot inbetween systems crits
            // which will mess up parsing if compacted.
            return;
        }
        int firstEmpty = -1;
        for (int slot = 0; slot < mech.getNumberOfCriticals(loc); slot++) {
            if (critData[loc][slot].equals(MtfFile.EMPTY)) {
                firstEmpty = slot;
            }
            if (firstEmpty != -1 && !critData[loc][slot].equals(MtfFile.EMPTY)) {
                //move this to the first empty slot
                critData[loc][firstEmpty] = critData[loc][slot];
                //mark the old slot empty
                critData[loc][slot] = MtfFile.EMPTY;
                //restart just after the moved slot's new location
                slot = firstEmpty;
                firstEmpty = -1;
            }
        }
    }

}
