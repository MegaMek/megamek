/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2019 The MegaMek Team
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

package megamek.common.loaders;

import megamek.common.*;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 *
 * @author taharqa
 */
public class BLKFixedWingSupportFile extends BLKFile implements IMechLoader {

    // armor locatioms
    public static final int NOSE = 0;
    public static final int RW = 1;
    public static final int LW = 2;
    public static final int AFT = 3;

    public BLKFixedWingSupportFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        FixedWingSupport a = new FixedWingSupport();

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
        checkManualBV(a);

        if (dataFile.exists("source")) {
            a.setSource(dataFile.getDataAsString("source")[0]);
        }

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        // get a movement mode - lets try Aerodyne
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        a.setMovementMode(nMotion);

        addTransports(a);

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
        int engineFlags = Engine.SUPPORT_VEE_ENGINE;
        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException("Could not find SafeThrust block.");
        }
        a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);
        //support vees don't use engine ratings, so just use a value of 1
        a.setEngine(new Engine(1, BLKFile.translateEngineCode(engineCode), engineFlags));

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
        if (!patchworkArmor) {
            if (!dataFile.exists("barrating")) {
                throw new EntityLoadingException("Could not find barrating block.");
            }
            a.setBARRating(dataFile.getDataAsInt("barrating")[0]);
        } else {
            for (int i = 0; i < (a.locations() - 1); i++) {
                a.setArmorType(dataFile.getDataAsInt(a.getLocationName(i) + "_armor_type")[0], i);
                a.setArmorTechLevel(dataFile.getDataAsInt(a.getLocationName(i) + "_armor_type")[0], i);
                a.setBARRating(dataFile.getDataAsInt(a.getLocationName(i) + "_barrating")[0], i);
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

        a.initializeArmor(armor[BLKAeroFile.NOSE], Aero.LOC_NOSE);
        a.initializeArmor(armor[BLKAeroFile.RW], Aero.LOC_RWING);
        a.initializeArmor(armor[BLKAeroFile.LW], Aero.LOC_LWING);
        a.initializeArmor(armor[BLKAeroFile.AFT], Aero.LOC_AFT);
        
        // Set the structural tech rating
        if (!dataFile.exists("structural_tech_rating")) {
            throw new EntityLoadingException("Could not find " +
                    "structural_tech_rating block!");
        }
        a.setStructuralTechRating(dataFile
                .getDataAsInt("structural_tech_rating")[0]);
        // Set armor tech rating, if it exists (defaults to structural tr)
        if (dataFile.exists("armor_tech_rating")) {
            a.setArmorTechRating(dataFile
                    .getDataAsInt("armor_tech_rating")[0]);            
        }
        // Set engine tech rating, if it exists (defaults to structural tr)        
        if (dataFile.exists("engine_tech_rating")) {
            a.setEngineTechRating(dataFile
                    .getDataAsInt("engine_tech_rating")[0]);            
        }

        a.autoSetInternal();
        a.recalculateTechAdvancement();
        a.autoSetSI();
        // This is not working right for arrays for some reason
        a.autoSetThresh();

        loadEquipment(a, "Nose", Aero.LOC_NOSE);
        loadEquipment(a, "Right Wing", Aero.LOC_RWING);
        loadEquipment(a, "Left Wing", Aero.LOC_LWING);
        loadEquipment(a, "Aft", Aero.LOC_AFT);
        loadEquipment(a, "Body", FixedWingSupport.LOC_BODY);

        if (dataFile.exists("omni")) {
            a.setOmni(true);
        }

        if (a.isClan()) {
            a.addClanCase();
        }

        // how many bombs can it carry
        // do this here, after equipment has been loaded, because fixed wing
        // support vees need equipment for this
        a.autoSetMaxBombPoints();
        a.setArmorTonnage(a.getArmorWeight());

        if (dataFile.exists("baseChassisFireConWeight")) {
            a.setBaseChassisFireConWeight((dataFile.getDataAsDouble("baseChassisFireConWeight")[0]));
        }

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

                boolean omniMounted = equipName.contains(":OMNI");
                equipName = equipName.replace(":OMNI", "");

                double size = 0.0;
                int sizeIndex = equipName.toUpperCase().indexOf(":SIZE:");
                if (sizeIndex > 0) {
                    size = Double.parseDouble(equipName.substring(sizeIndex + 6));
                    equipName = equipName.substring(0, sizeIndex);
                }
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
                if ((etype == null) && checkLegacyExtraEquipment(equipName)) {
                    continue;
                }

                if (etype != null) {
                    try {
                        Mounted mount = t.addEquipment(etype, nLoc, rearMount);
                        mount.setOmniPodMounted(omniMounted);
                        // Need to set facing for VGLs
                        if ((etype instanceof WeaponType) 
                                && etype.hasFlag(WeaponType.F_VGL)) {
                            if (facing == -1) {
                                mount.setFacing(defaultAeroVGLFacing(nLoc, rearMount));
                            } else {
                                mount.setFacing(facing);
                            }
                        }
                        if (etype.isVariableSize()) {
                            if (size == 0.0) {
                                size = getLegacyVariableSize(equipName);
                            }
                            mount.setSize(size);
                        } else if (t.isSupportVehicle() && (mount.getType() instanceof InfantryWeapon)
                                && size > 1) {
                            // The ammo bin is created by Entity#addEquipment but the size has not
                            // been set yet, so if the unit carries multiple clips the number of
                            // shots needs to be adjusted.
                            mount.setSize(size);
                            assert(mount.getLinked() != null);
                            mount.getLinked().setOriginalShots((int) size
                                    * ((InfantryWeapon) mount.getType()).getShots());
                            mount.getLinked().setShotsLeft(mount.getLinked().getOriginalShots());
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (!equipName.equals("")) {
                    t.addFailedEquipment(equipName);
                }
            }
        }
        if (mashOperatingTheaters > 0) {
            for (Mounted m : t.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_MASH)) {
                    // includes one as part of the core component
                    m.setSize(m.getSize() + mashOperatingTheaters);
                    break;
                }
            }
        }
        if (legacyDCCSCapacity > 0) {
            for (Mounted m : t.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                    // core system does not include drone capacity
                    m.setSize(legacyDCCSCapacity);
                    break;
                }
            }
        }
    }
}
