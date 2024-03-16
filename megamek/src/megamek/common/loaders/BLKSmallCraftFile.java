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
package megamek.common.loaders;

import megamek.common.*;
import megamek.common.util.BuildingBlock;
import megamek.common.verifier.TestEntity;

/**
 * @author taharqa
 * @since April 6, 2002, 2:06 AM
 */
public class BLKSmallCraftFile extends BLKFile implements IMechLoader {

    public BLKSmallCraftFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        SmallCraft a = new SmallCraft();
        setBasicEntityData(a);

        if (dataFile.exists("originalBuildYear")) {
            a.setOriginalBuildYear(dataFile.getDataAsInt("originalBuildYear")[0]);
        }

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find tonnage block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        if (!dataFile.exists("crew")) {
            throw new EntityLoadingException("Could not find crew block.");
        }
        a.setNCrew(dataFile.getDataAsInt("crew")[0]);

        if (dataFile.exists("officers")) {
            a.setNOfficers(dataFile.getDataAsInt("officers")[0]);
        }

        if (dataFile.exists("gunners")) {
            a.setNGunners(dataFile.getDataAsInt("gunners")[0]);
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

        if (dataFile.exists("life_boat")) {
            a.setLifeBoats(dataFile.getDataAsInt("life_boat")[0]);
        }

        if (dataFile.exists("escape_pod")) {
            a.setEscapePods(dataFile.getDataAsInt("escape_pod")[0]);
        }

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find motion_type block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        if (sMotion.equalsIgnoreCase("spheroid")) {
            nMotion = EntityMovementMode.SPHEROID;
            a.setSpheroid(true);
        }
        a.setMovementMode(nMotion);

        // All small craft are VSTOL and can hover
        a.setVSTOL(true);

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
        // not done for small craft and up
        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException("Could not find SafeThrust block.");
        }
        a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);

        a.setEngine(new Engine(400, 0, 0));

        // Switch older files with standard armor to aerospace
        int at = EquipmentType.T_ARMOR_AEROSPACE;
        if (dataFile.exists("armor_type")) {
            at = dataFile.getDataAsInt("armor_type")[0];
            if (at == EquipmentType.T_ARMOR_STANDARD) {
                at = EquipmentType.T_ARMOR_AEROSPACE;
            }
        }
        a.setArmorType(at);
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

        a.initializeArmor(armor[BLKAeroSpaceFighterFile.NOSE], SmallCraft.LOC_NOSE);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.RW], SmallCraft.LOC_RWING);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.LW], SmallCraft.LOC_LWING);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.AFT], SmallCraft.LOC_AFT);
        a.initializeArmor(IArmorState.ARMOR_NA, SmallCraft.LOC_HULL);

        a.autoSetInternal();
        // This is not working right for arrays for some reason
        a.autoSetThresh();
        a.recalculateTechAdvancement();

        for (int loc = 0; loc < a.locations(); loc++) {
            loadEquipment(a, a.getLocationName(loc), loc);
        }

        addTransports(a);

        // how many bombs can it carry; depends on transport bays
        a.autoSetMaxBombPoints();

        a.setArmorTonnage(a.getArmorWeight());
        loadQuirks(a);
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
                        int useLoc = TestEntity.eqRequiresLocation(t, etype) ? nLoc : SmallCraft.LOC_HULL;
                        Mounted mount = t.addEquipment(etype, useLoc, rearMount);
                        // Need to set facing for VGLs
                        if ((etype instanceof WeaponType)
                                && etype.hasFlag(WeaponType.F_VGL)) {
                            if (facing == -1) {
                                mount.setFacing(defaultAeroVGLFacing(useLoc, rearMount));
                            } else {
                                mount.setFacing(facing);
                            }
                        }
                        if (etype.isVariableSize()) {
                            if (size == 0.0) {
                                size = getLegacyVariableSize(equipName);
                            }
                            mount.setSize(size);
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (!equipName.isBlank()) {
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
