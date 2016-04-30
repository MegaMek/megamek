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
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 *
 * @author taharqa
 * @version
 */
package megamek.common.loaders;

import megamek.common.Aero;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.util.BuildingBlock;

public class BLKAeroFile extends BLKFile implements IMechLoader {

    // armor locatioms
    public static final int NOSE = 0;
    public static final int RW = 1;
    public static final int LW = 2;
    public static final int AFT = 3;

    public BLKAeroFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        Aero a = new Aero();

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        a.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model") && (dataFile.getDataAsString("Model")[0] != null)) {
            a.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            a.setModel("");
        }

        setTechLevel(a);
        setFluff(a);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        // how many bombs can it carry
        a.autoSetMaxBombPoints();

        // get a movement mode - lets try Aerodyne
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        a.setMovementMode(nMotion);

        a.setVSTOL(true);

        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heatsinks block.");
        }
        a.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        a.setOHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        if (!dataFile.exists("sink_type")) {
            throw new EntityLoadingException("Could not find sink_type block.");
        }
        a.setHeatType(dataFile.getDataAsInt("sink_type")[0]);

        // figure out fuel
        if (!dataFile.exists("fuel")) {
            throw new EntityLoadingException("Could not find fuel block.");
        }
        a.setFuel(dataFile.getDataAsInt("fuel")[0]);

        // figure out engine stuff
        int engineCode = BLKFile.FUSION;
        if (dataFile.exists("engine_type")) {
            engineCode = dataFile.getDataAsInt("engine_type")[0];
        }
        int engineFlags = 0;
        if (a.isClan()) {
            engineFlags |= Engine.CLAN_ENGINE;
        }
        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException("Could not find SafeThrust block.");
        }

        // set cockpit type if not default
        if (dataFile.exists("cockpit_type")) {
            a.setCockpitType(dataFile.getDataAsInt("cockpit_type")[0]);
        }

        int engineRating = (dataFile.getDataAsInt("SafeThrust")[0] - 2) * (int) a.getWeight();
        if (a.isPrimitive()) {
            engineRating *= 1.2;
            // Ensure the rating is divisible by 5
            if ((engineRating % 5) != 0) {
                engineRating = engineRating - (engineRating % 5) + 5;
            }
        }
        a.setEngine(new Engine(engineRating, BLKFile.translateEngineCode(engineCode), engineFlags));

        boolean patchworkArmor = false;
        if (dataFile.exists("armor_type")) {
            if (dataFile.getDataAsInt("armor_type")[0] == EquipmentType.T_ARMOR_PATCHWORK) {
                patchworkArmor = true;
            } else {
                a.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
            }
        } else {
            a.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        }
        if (!patchworkArmor && dataFile.exists("armor_tech")) {
            a.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }
        if (patchworkArmor) {
            for (int i = 0; i < (a.locations() - 1); i++) {
                a.setArmorType(dataFile.getDataAsInt(a.getLocationName(i) + "_armor_type")[0], i);
                a.setArmorTechLevel(dataFile.getDataAsInt(a.getLocationName(i) + "_armor_type")[0], i);
            }
        }

        if (dataFile.exists("internal_type")) {
            a.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            a.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 4) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        if (dataFile.exists("source")) {
            a.setSource(dataFile.getDataAsString("source")[0]);
        }

        checkManualBV(a);

        a.initializeArmor(armor[BLKAeroFile.NOSE], Aero.LOC_NOSE);
        a.initializeArmor(armor[BLKAeroFile.RW], Aero.LOC_RWING);
        a.initializeArmor(armor[BLKAeroFile.LW], Aero.LOC_LWING);
        a.initializeArmor(armor[BLKAeroFile.AFT], Aero.LOC_AFT);
        a.initializeArmor(0, Aero.LOC_WINGS);

        a.autoSetCapArmor();
        a.autoSetFatalThresh();

        a.autoSetInternal();
        a.autoSetSI();
        // This is not working right for arrays for some reason
        a.autoSetThresh();

        loadEquipment(a, "Nose", Aero.LOC_NOSE);
        loadEquipment(a, "Right Wing", Aero.LOC_RWING);
        loadEquipment(a, "Left Wing", Aero.LOC_LWING);
        loadEquipment(a, "Aft", Aero.LOC_AFT);

        // now organize the weapons into groups for capital fighters
        a.updateWeaponGroups();

        if (dataFile.exists("omni")) {
            a.setOmni(true);
        }

        if (a.isClan()) {
            a.addClanCase();
        }

        addTransports(a);
        a.setArmorTonnage(a.getArmorWeight());
        return a;
    }

    @Override
    protected void loadEquipment(Entity t, String sName, int nLoc) throws EntityLoadingException {
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) {
            return;
        }

        // prefix is "Clan " or "IS "
        String prefix;
        if (t.getTechLevel() == TechConstants.T_CLAN_TW) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        boolean rearMount = false;

        if (saEquip[0] != null) {
            for (String element : saEquip) {
                rearMount = false;
                String equipName = element.trim();

                if (equipName.startsWith("(R) ")) {
                    rearMount = true;
                    equipName = equipName.substring(4);
                }
                int facing = -1;
                if (equipName.toUpperCase().endsWith("(FL)")) {
                    facing = 5;
                    equipName = equipName.substring(0, equipName.length() - 4)
                            .trim();
                }
                if (equipName.toUpperCase().endsWith("(FR)")) {
                    facing = 1;
                    equipName = equipName.substring(0, equipName.length() - 4)
                            .trim();
                }
                if (equipName.toUpperCase().endsWith("(RL)")) {
                    facing = 4;
                    equipName = equipName.substring(0, equipName.length() - 4)
                            .trim();
                }
                if (equipName.toUpperCase().endsWith("(RR)")) {
                    facing = 2;
                    equipName = equipName.substring(0, equipName.length() - 4)
                            .trim();
                }                 

                EquipmentType etype = EquipmentType.get(equipName);

                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }

                if (etype != null) {
                    try {
                        Mounted mount = t.addEquipment(etype, nLoc, rearMount);
                        // Need to set facing for VGLs
                        if ((etype instanceof WeaponType) 
                                && etype.hasFlag(WeaponType.F_VGL)) {
                            // If no facing specified, assume front
                            if (facing == -1) {
                                if (rearMount) {
                                    mount.setFacing(3);
                                } else {
                                    mount.setFacing(0);
                                }
                            } else {
                                mount.setFacing(facing);
                            }
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (!equipName.equals("")) {
                    t.addFailedEquipment(equipName);
                }
            }
        }
    }

    /*
     * protected void organizeIntoGroups(Aero a) throws EntityLoadingException {
     * //collect a hash of all the same weapons in each location by id
     * Map<String, Integer> groups = new HashMap<String, Integer>(); for
     * (Mounted mounted : a.getTotalWeaponList()) { int loc =
     * mounted.getLocation(); if(loc == Aero.LOC_RWING || loc == Aero.LOC_LWING)
     * { loc = Aero.LOC_WINGS; } if(mounted.isRearMounted()) { loc =
     * Aero.LOC_AFT; } String key = mounted.getType().getInternalName() + ":" +
     * loc; if(null == groups.get(key)) { groups.put(key, 1); } else {
     * groups.put(key, groups.get(key) + 1); } } //now we just need to traverse
     * the hash and add this new equipment Set<String> set= groups.keySet();
     * Iterator<String> iter = set.iterator(); while(iter.hasNext()) { String
     * key = iter.next(); String name = key.split(":")[0]; int loc =
     * Integer.parseInt(key.split(":")[1]); EquipmentType etype =
     * EquipmentType.get(name); Mounted newmount; if (etype != null) { try {
     * newmount = a.addWeaponGroup(etype, loc);
     * newmount.setNWeapons(groups.get(key)); } catch (LocationFullException ex)
     * { throw new EntityLoadingException(ex.getMessage()); } } else if(name !=
     * "0"){ a.addFailedEquipment(name); } } }
     */
}
