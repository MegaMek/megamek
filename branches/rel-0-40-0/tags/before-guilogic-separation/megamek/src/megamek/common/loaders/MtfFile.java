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
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;

/**
 *
 * @author  Ben
 * @version
 */
public class MtfFile implements MechLoader {

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

    String heatSinks;
    String walkMP;
    String jumpMP;

    String armorType;
    String larmArmor;
    String rarmArmor;
    String ltArmor;
    String rtArmor;
    String ctArmor;
    String headArmor;
    String llegArmor;
    String rlegArmor;
    String ltrArmor;
    String rtrArmor;
    String ctrArmor;

    String weaponCount;
    String[] weaponData;

    String[][] critData;

    Hashtable hSharedEquip = new Hashtable();
    Vector vSplitWeapons = new Vector();


    /** Creates new MtfFile */
    public MtfFile(InputStream is) throws EntityLoadingException {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));

            version = r.readLine();

            name = r.readLine();
            model = r.readLine();

            r.readLine();

            chassisConfig = r.readLine();
            techBase = r.readLine();
            techYear = r.readLine();
            rulesLevel = r.readLine();

            r.readLine();

            tonnage = r.readLine();
            engine = r.readLine();
            internalType = r.readLine();
            myomerType = r.readLine();

            r.readLine();

            heatSinks = r.readLine();
            walkMP = r.readLine();
            jumpMP = r.readLine();

            r.readLine();

            armorType = r.readLine();
            larmArmor = r.readLine();
            rarmArmor = r.readLine();
            ltArmor = r.readLine();
            rtArmor = r.readLine();
            ctArmor = r.readLine();
            headArmor = r.readLine();
            llegArmor = r.readLine();
            rlegArmor = r.readLine();
            ltrArmor = r.readLine();
            rtrArmor = r.readLine();
            ctrArmor = r.readLine();

            r.readLine();

            weaponCount = r.readLine();

            int a = 9;

            int weapons = Integer.parseInt(weaponCount.substring(8));
            weaponData = new String[weapons];
            for(int i = 0; i < weapons; i++) {
                weaponData[i] = r.readLine();
            }

            critData = new String[8][12];

            readCrits(r, Mech.LOC_LARM);
            readCrits(r, Mech.LOC_RARM);
            readCrits(r, Mech.LOC_LT);
            readCrits(r, Mech.LOC_RT);
            readCrits(r, Mech.LOC_CT);
            readCrits(r, Mech.LOC_HEAD);
            readCrits(r, Mech.LOC_LLEG);
            readCrits(r, Mech.LOC_RLEG);

            r.close();
        } catch (IOException ex) {
            throw new EntityLoadingException("I/O Error reading file");
        } catch (StringIndexOutOfBoundsException ex) {
            throw new EntityLoadingException("StringIndexOutOfBoundsException reading file (format error)");
        } catch (NumberFormatException ex) {
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

            if (chassisConfig.indexOf("Quad") != -1) {
                mech = new QuadMech();
            } else {
                mech = new BipedMech();
            }

            if (!version.trim().equalsIgnoreCase("Version:1.0")) {
                throw new EntityLoadingException("Wrong MTF file version.");
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

            /*
            //TODO: this ought to be a better test
            boolean innerSphere = "Inner Sphere".equals(this.techBase.substring(9).trim());
            boolean mixed = "Mixed".equals(this.techBase.substring(9).trim());
            switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 1 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_1);
                    break;
                case 2 :
                    if (innerSphere) {
                        mech.setTechLevel(TechConstants.T_IS_LEVEL_2);
                    } else if (mixed) {
                        mech.setTechLevel(TechConstants.T_MIXED_LEVEL_2);
                    } else {
                        mech.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
                    }
                    break;
                default :
                    throw new EntityLoadingException("Unsupported tech base and/or level: " + this.techBase.substring(9) + " (level " + this.rulesLevel.substring(12) + ")");
            }
            */

            if (techBase.substring(9).trim().equals("Inner Sphere")) {
                switch (Integer.parseInt(rulesLevel.substring(12).trim())) {
                case 1 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_1);
                    break;
                case 2 :
                    mech.setTechLevel(TechConstants.T_IS_LEVEL_2);
                    break;
                default :
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
                }
            } else if (techBase.substring(9).trim().equals("Clan")) {
                if (Integer.parseInt(rulesLevel.substring(12).trim()) == 2)
                    mech.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
                else
                    throw new EntityLoadingException("Unsupported tech level: " + rulesLevel.substring(12).trim());
            } else if (techBase.substring(9).trim().equals("Mixed (IS Chassis)")) {
                mech.setTechLevel(TechConstants.T_MIXED_BASE_IS_LEVEL_2);
            } else if (techBase.substring(9).trim().equals("Mixed (Clan Chassis)")) {
                mech.setTechLevel(TechConstants.T_MIXED_BASE_CLAN_LEVEL_2);
            } else if (techBase.substring(9).trim().equals("Mixed")) {
                throw new EntityLoadingException("Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
            } else {
                throw new EntityLoadingException("Unsupported tech base: " + techBase.substring(9).trim());
            }

            mech.setWeight((float)Integer.parseInt(tonnage.substring(5)));

            mech.setOriginalWalkMP(Integer.parseInt(walkMP.substring(8)));
            mech.setOriginalJumpMP(Integer.parseInt(jumpMP.substring(8)));

            boolean dblSinks = heatSinks.substring(14).equalsIgnoreCase("Double");
            int expectedSinks = Integer.parseInt(heatSinks.substring(11, 14).trim());

            mech.autoSetInternal();

            mech.initializeArmor(Integer.parseInt(larmArmor.substring(9)), Mech.LOC_LARM);
            mech.initializeArmor(Integer.parseInt(rarmArmor.substring(9)), Mech.LOC_RARM);
            mech.initializeArmor(Integer.parseInt(ltArmor.substring(9)), Mech.LOC_LT);
            mech.initializeArmor(Integer.parseInt(rtArmor.substring(9)), Mech.LOC_RT);
            mech.initializeArmor(Integer.parseInt(ctArmor.substring(9)), Mech.LOC_CT);
            mech.initializeArmor(Integer.parseInt(headArmor.substring(9)), Mech.LOC_HEAD);
            mech.initializeArmor(Integer.parseInt(llegArmor.substring(9)), Mech.LOC_LLEG);
            mech.initializeArmor(Integer.parseInt(rlegArmor.substring(9)), Mech.LOC_RLEG);
            mech.initializeRearArmor(Integer.parseInt(ltrArmor.substring(10)), Mech.LOC_LT);
            mech.initializeRearArmor(Integer.parseInt(rtrArmor.substring(10)), Mech.LOC_RT);
            mech.initializeRearArmor(Integer.parseInt(ctrArmor.substring(10)), Mech.LOC_CT);

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
            throw new EntityLoadingException("NumberFormatException parsing file");
        } catch (NullPointerException ex) {
            throw new EntityLoadingException("NullPointerException parsing file");
        } catch (StringIndexOutOfBoundsException ex) {
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
            boolean rearMounted = false;
            boolean split = false;

            if (critName.equalsIgnoreCase("Fusion Engine")) {
                mech.setCritical(loc,i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, 3));
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
                        else {
                            m = mech.addEquipment(etype, loc, rearMounted);
                            hSharedEquip.put(etype, m);
                        }
                    }
                    else if (split) {
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
                            // give the most restrictive location for arcs
                            m.setLocation(Mech.mostRestrictiveLoc(loc, m.getLocation()));
                        }
                        else {
                            // make a new one
                            m = new Mounted(mech, etype);
                            m.setSplit(true);
                            m.setFoundCrits(1);
                            vSplitWeapons.addElement(m);
                        }
                        mech.addEquipment(m, loc, rearMounted);
                        //mech.addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                        //        mech.getEquipmentNum(m), etype.isHittable()));
                    }
                    else {
                        mech.addEquipment(etype, loc, rearMounted);
                    }
                } else {
                    if (!critName.equals("-Empty-")) {
                        //Can't load this piece of equipment!
                        // Add it to the list so we can show the user.
                        mech.addFailedEquipment(critName);
                        // Make the failed equipment an empty slot
                        critData[loc][i] = "-Empty-";
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
            if (critData[loc][slot].equals("-Empty-")) {
                firstEmpty = slot;
            }
            if (firstEmpty != -1 && !critData[loc][slot].equals("-Empty-")) {
                //move this to the first empty slot
                critData[loc][firstEmpty] = critData[loc][slot];
                //mark the old slot empty
                critData[loc][slot] = "-Empty-";
                //restart just after the moved slot's new location
                slot = firstEmpty;
                firstEmpty = -1;
            }
        }
    }

}
