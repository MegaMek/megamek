/**
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

package megamek.common.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

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

// Known bug! Split-location weapons are not supported.

public class MepFile implements IMechLoader {
    String version;
    String name;

    String mechYear;
    String innerSphere;
    String techYear;
    String chassisType;
    String tonnage;

    String engineType;
    String heatSinkType;
    String armorType;
    String internalType;

    String walkMP;
    String jumpMP;
    String heatSinks;

    String armorPoints;
    String armorPoints1;
    String armorPoints2;

    String headArmor;
    String larmArmor;
    String ltArmor;
    String ltrArmor;
    String ctArmor;
    String ctrArmor;
    String rtArmor;
    String rtrArmor;
    String rarmArmor;
    String llegArmor;
    String rlegArmor;

    String eqCount;
    String[] equipData;

    String eqWeight;
    String eqSlots;

    String[] critData;

    Hashtable<EquipmentType, Mounted> hSharedEquip = new Hashtable<EquipmentType, Mounted>();

    public MepFile(InputStream is) throws EntityLoadingException {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));

            version = r.readLine();
            name = r.readLine();

            r.readLine(); // don't know what these are
            r.readLine();
            r.readLine();
            r.readLine();
            r.readLine();

            mechYear = r.readLine();
            innerSphere = r.readLine();
            techYear = r.readLine();
            chassisType = r.readLine();
            tonnage = r.readLine();

            engineType = r.readLine();
            heatSinkType = r.readLine();
            internalType = r.readLine();
            armorType = r.readLine();

            walkMP = r.readLine();
            jumpMP = r.readLine();
            heatSinks = r.readLine();

            r.readLine(); // weapons table descriptor -- useless?

            armorPoints = r.readLine();
            armorPoints1 = r.readLine(); // what are these two?
            armorPoints2 = r.readLine();

            headArmor = r.readLine();
            larmArmor = r.readLine();
            ltArmor = r.readLine();
            ltrArmor = r.readLine();
            ctArmor = r.readLine();
            ctrArmor = r.readLine();
            rtArmor = r.readLine();
            rtrArmor = r.readLine();
            rarmArmor = r.readLine();
            llegArmor = r.readLine();
            rlegArmor = r.readLine();

            eqCount = r.readLine();

            int eqs = Integer.parseInt(eqCount.substring(1));
            equipData = new String[eqs];
            for (int i = 0; i < eqs; i++) {
                equipData[i] = r.readLine();
            }

            eqWeight = r.readLine();
            eqSlots = r.readLine();

            r.readLine(); // mystery number

            int crits = 78;
            critData = new String[crits];
            for (int i = 0; i < crits; i++) {
                critData[i] = r.readLine();
            }

            r.close();
        } catch (IOException ex) {
            throw new EntityLoadingException(
                    "I/O error occured during file read");
        } catch (StringIndexOutOfBoundsException ex) {
            throw new EntityLoadingException(
                    "StringIndexOutOfBoundsException reading file (format error)");
        } catch (NumberFormatException ex) {
            throw new EntityLoadingException(
                    "NumberFormatException reading file (format error)");
        }
    }

    public Entity getEntity() throws EntityLoadingException {
        try {
            Mech mech;

            if ("Quad".equals(chassisType.trim())) {
                mech = new QuadMech();
            } else {
                mech = new BipedMech();
            }

            int firstSpace = name.indexOf(" ");
            if (firstSpace != -1) {
                mech.setChassis(name.substring(firstSpace).trim());
                mech.setModel(name.substring(5, firstSpace).trim());
            } else {
                mech.setChassis(name.substring(5).trim());
                mech.setModel(name.substring(5).trim());
            }

            mech.setWeight(Integer.decode(tonnage.trim()).intValue());
            mech.setYear(Integer.parseInt(techYear.trim()));
            mech.setOmni("OmniMech".equals(chassisType.trim()));

            // TODO: this ought to be a better test
            if ("InnerSphere".equals(innerSphere.trim())) {
                if (mech.getYear() == 3025) {
                    mech.setTechLevel(TechConstants.T_INTRO_BOXSET);
                } else {
                    mech.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                }
            } else {
                mech.setTechLevel(TechConstants.T_CLAN_TW);
            }

            int engineFlags = 0;
            if (mech.isClan()) {
                engineFlags = Engine.CLAN_ENGINE;
            }
            int engineRating = Integer.parseInt(walkMP.trim())
                    * (int) mech.getWeight();
            mech.setEngine(new Engine(engineRating, Engine
                    .getEngineTypeByString(engineType), engineFlags));
            // No support for moveable system crits due to goofy critical
            // format. Could be fixed, but I don't think anyone uses
            // MEP for level 3 designs.
            mech.addEngineCrits();
            mech.addCockpit();
            mech.addGyro();

            mech.setOriginalJumpMP(Integer.parseInt(jumpMP.trim()));

            boolean dblSinks = "Double".equals(heatSinkType.trim());
            mech.addEngineSinks(Integer.parseInt(heatSinks.trim()),
                    dblSinks);

            mech.setStructureType(internalType);

            mech.setArmorType(armorType);

            decodeArmorAndInternals(mech, Mech.LOC_HEAD, headArmor);
            decodeArmorAndInternals(mech, Mech.LOC_LARM, larmArmor);
            decodeArmorAndInternals(mech, Mech.LOC_LT, ltArmor);
            decodeRearArmor(mech, Mech.LOC_LT, ltrArmor);
            decodeArmorAndInternals(mech, Mech.LOC_CT, ctArmor);
            decodeRearArmor(mech, Mech.LOC_CT, ctrArmor);
            decodeArmorAndInternals(mech, Mech.LOC_RT, rtArmor);
            decodeRearArmor(mech, Mech.LOC_RT, rtrArmor);
            decodeArmorAndInternals(mech, Mech.LOC_RARM, rarmArmor);
            decodeArmorAndInternals(mech, Mech.LOC_RLEG, rlegArmor);
            decodeArmorAndInternals(mech, Mech.LOC_LLEG, llegArmor);

            // remove arm actuators
            for (int i = 0; i < equipData.length; i++) {
                String eqName = new String(equipData[i]);
                eqName = eqName.substring(5, 28).trim();

                if (eqName.equals("No Lower Right Arm")) {
                    mech.removeCriticals(Mech.LOC_RARM, new CriticalSlot(
                            CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM));
                    mech.removeCriticals(Mech.LOC_RARM, new CriticalSlot(
                            CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND));
                } else if (eqName.equals("No Lower Left Arm")) {
                    mech.removeCriticals(Mech.LOC_LARM, new CriticalSlot(
                            CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM));
                    mech.removeCriticals(Mech.LOC_LARM, new CriticalSlot(
                            CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND));
                } else if (eqName.equals("No Right Hand")) {
                    mech.removeCriticals(Mech.LOC_RARM, new CriticalSlot(
                            CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND));
                } else if (eqName.equals("No Left Hand")) {
                    mech.removeCriticals(Mech.LOC_LARM, new CriticalSlot(
                            CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND));
                }
            }

            // hmm, what to do with the rest of equipment list... I dunno.

            // prefix is "Clan " or "IS "
            String prefix;
            if (mech.getTechLevel() == TechConstants.T_CLAN_TW) {
                prefix = "Clan ";
            } else {
                prefix = "IS ";
            }

            // parse the critical hit slots
            for (int i = 0; i < critData.length; i++) {
                int loc = mech.getLocationFromAbbr(critData[i].substring(3, 5));
                int slot = Integer.parseInt(critData[i].substring(5, 7));
                boolean rearMounted = false;
                String critName = critData[i].substring(7).trim();

                // if the slot's full already, skip it.
                if (mech.getCritical(loc, slot) != null) {
                    continue;
                }

                if (critName.startsWith("(R)")) {
                    rearMounted = true;
                    critName = critName.substring(3).trim();
                }
                if (critName.equalsIgnoreCase("Armored Cowl")) {
                    mech.setCowl(5);
                }
                // this is a bit a kludge, but MEP is stupid
                if (critName.equals("Heat Sink") && dblSinks) {
                    critName = "Double Heat Sink";
                }

                EquipmentType etype = EquipmentType.get(prefix + critName);
                if (etype == null) {
                    // try w/o prefix
                    etype = EquipmentType.get(critName);
                }
                if (etype != null) {
                    try {
                        if (etype.isSpreadable()) {
                            // do we already have one of these? Key on Type
                            Mounted m = hSharedEquip.get(etype);
                            if (m != null) {
                                // use the existing one
                                mech.addCritical(loc, new CriticalSlot(
                                        CriticalSlot.TYPE_EQUIPMENT, mech
                                                .getEquipmentNum(m), etype
                                                .isHittable(), m));
                                continue;
                            }
                            m = mech.addEquipment(etype, loc, rearMounted);
                            hSharedEquip.put(etype, m);
                        } else {
                            mech.addEquipment(etype, loc, rearMounted);
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else {
                    if (!critName.equals("-----------------")
                            && !critName.equals("''")) {
                        // Can't load this piece of equipment!
                        mech.addFailedEquipment(critName);
                    }
                }
            }

            if (mech.isClan()) {
                mech.addClanCase();
            }

            return mech;
        } catch (NumberFormatException ex) {
            throw new EntityLoadingException(
                    "NumberFormatException parsing file");
        } catch (NullPointerException ex) {
            throw new EntityLoadingException(
                    "NullPointerException parsing file");
        }
    }

    /**
     * Decodes and sets the mech's armor and internal structure values
     */
    private void decodeArmorAndInternals(Mech mech, int loc, String s) {
        mech.initializeArmor(Integer.parseInt(s.substring(2, 4)), loc);
        mech.initializeInternal(Integer.parseInt(s.substring(12)), loc);
    }

    /**
     * Decodes and sets the mech's rear armor values
     */
    private void decodeRearArmor(Mech mech, int loc, String string) {
        mech.initializeRearArmor(Integer.parseInt(string.substring(2, 4)), loc);
    }

}
