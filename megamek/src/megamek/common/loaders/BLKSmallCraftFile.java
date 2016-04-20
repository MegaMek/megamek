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
import megamek.common.SmallCraft;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.util.BuildingBlock;

public class BLKSmallCraftFile extends BLKFile implements IMechLoader {

    // armor locatioms
    public static final int NOSE = 0;
    public static final int RW = 1;
    public static final int LW = 2;
    public static final int AFT = 3;

    public BLKSmallCraftFile(BuildingBlock bb) {
        dataFile = bb;
    }

    public Entity getEntity() throws EntityLoadingException {

        SmallCraft a = new SmallCraft();

        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }
        a.setChassis(dataFile.getDataAsString("Name")[0]);
        if (dataFile.exists("Model") && (dataFile.getDataAsString("Model")[0] != null)) {
            a.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            a.setModel("");
        }

        if (dataFile.exists("source")) {
            a.setSource(dataFile.getDataAsString("source")[0]);
        }

        setTechLevel(a);
        setFluff(a);
        checkManualBV(a);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find tonnage block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        if (!dataFile.exists("crew")) {
            throw new EntityLoadingException("Could not find crew block.");
        }
        a.setNCrew(dataFile.getDataAsInt("crew")[0]);

        if (dataFile.exists("passengers")) {
            a.setNPassenger(dataFile.getDataAsInt("passengers")[0]);
        }

        if (dataFile.exists("battlearmor")) {
            a.setNBattleArmor(dataFile.getDataAsInt("battlearmor")[0]);
        }

        if (dataFile.exists("marines")) {
            a.setNMarines(dataFile.getDataAsInt("marines")[0]);
        }

        if (dataFile.exists("otherpassenger")) {
            a.setNOtherPassenger(dataFile.getDataAsInt("otherpassenger")[0]);
        }

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find motion_type block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        if (sMotion.equals("spheroid")) {
            nMotion = EntityMovementMode.SPHEROID;
            a.setSpheroid(true);
        }
        a.setMovementMode(nMotion);
        if (a.isSpheroid()) {
            a.setVSTOL(true);
        }

        // figure out structural integrity
        if (!dataFile.exists("structural_integrity")) {
            throw new EntityLoadingException("Could not find structual_integrity block.");
        }
        a.set0SI(dataFile.getDataAsInt("structural_integrity")[0]);

        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heatsinks block.");
        }
        a.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
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
        // not done for small craft and up
        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException("Could not find SafeThrust block.");
        }
        a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);

        a.setEngine(new Engine(400, 0, 0));

        if (dataFile.exists("armor_type")) {
            a.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
        } else {
            a.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        }
        if (dataFile.exists("armor_tech")) {
            a.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
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

        a.autoSetInternal();
        // This is not working right for arrays for some reason
        a.autoSetThresh();

        loadEquipment(a, "Nose", Aero.LOC_NOSE);
        loadEquipment(a, "Right Side", Aero.LOC_RWING);
        loadEquipment(a, "Left Side", Aero.LOC_LWING);
        loadEquipment(a, "Aft", Aero.LOC_AFT);

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

}
