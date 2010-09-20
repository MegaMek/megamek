/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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

import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LargeSupportTank;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SpaceStation;
import megamek.common.SupportTank;
import megamek.common.SupportVTOL;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.Warship;
import megamek.common.util.BuildingBlock;

public class BLKFile {

    BuildingBlock dataFile;

    public static final int FUSION = 0;
    public static final int ICE = 1;
    public static final int XL = 2;
    public static final int XXL = 3; // don't ask
    public static final int LIGHT = 4; // don't ask
    public static final int COMPACT = 5; // don't ask
    public static final int FUELCELL = 6;
    public static final int FISSION = 7;

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

        if (saEquip[0] != null) {
            for (int x = 0; x < saEquip.length; x++) {
                String equipName = saEquip[x].trim();
                EquipmentType etype = EquipmentType.get(equipName);

                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }

                if (etype != null) {
                    try {
                        t.addEquipment(etype, nLoc);
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (equipName != "0") {
                    t.addFailedEquipment(equipName);
                }
            }
        }
    }

    public boolean isMine() {

        if (dataFile.exists("blockversion")) {
            return true;
        }

        return false;

    }

    static int translateEngineCode(int code) {
        if (code == BLKFile.FUSION) {
            return Engine.NORMAL_ENGINE;
        } else if (code == BLKFile.ICE) {
            return Engine.COMBUSTION_ENGINE;
        } else if (code == BLKFile.XL) {
            return Engine.XL_ENGINE;
        } else if (code == BLKFile.LIGHT) {
            return Engine.LIGHT_ENGINE;
        } else if (code == BLKFile.XXL) {
            return Engine.XXL_ENGINE;
        } else if (code == BLKFile.COMPACT) {
            return Engine.COMPACT_ENGINE;
        } else if (code == BLKFile.FUELCELL) {
            return Engine.FUEL_CELL;
        } else if (code == BLKFile.FISSION) {
            return Engine.FISSION;
        } else {
            return -1;
        }
    }

    public void setTechLevel(Entity e) throws EntityLoadingException {
        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        e.setYear(dataFile.getDataAsInt("year")[0]);

        if (!dataFile.exists("type")) {
            throw new EntityLoadingException("Could not find type block.");
        }

        if (dataFile.getDataAsString("type")[0].equals("IS")) {
            if (e.getYear() == 3025) {
                e.setTechLevel(TechConstants.T_INTRO_BOXSET);
            } else {
                e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
            }
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 1")) {
            e.setTechLevel(TechConstants.T_INTRO_BOXSET);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 2")) {
            e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 3")) {
            e.setTechLevel(TechConstants.T_IS_ADVANCED);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 4")) {
            e.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        } else if (dataFile.getDataAsString("type")[0].equals("IS Level 5")) {
            e.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan") || dataFile.getDataAsString("type")[0].equals("Clan Level 2")) {
            e.setTechLevel(TechConstants.T_CLAN_TW);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 3")) {
            e.setTechLevel(TechConstants.T_CLAN_ADVANCED);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 4")) {
            e.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 5")) {
            e.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (IS Chassis)")) {
            e.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (IS Chassis) Experimental")) {
            e.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (IS Chassis) Unofficial")) {
            e.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (Clan Chassis)")) {
            e.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (Clan Chassis) Experimental")) {
            e.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (Clan Chassis) Unofficial")) {
            e.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed")) {
            throw new EntityLoadingException("Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
        } else {
            throw new EntityLoadingException("Unsupported tech level: " + dataFile.getDataAsString("type")[0]);
        }
    }

    public static void encode(String fileName, Entity t) {
        BuildingBlock blk = new BuildingBlock();
        blk.createNewBlock();
        blk.writeBlockData("UnitType", "LargeSupportTank");

        if (t instanceof BattleArmor) {
            blk.writeBlockData("UnitType", "BattleArmor");
        } else if (t instanceof Protomech) {
            blk.writeBlockData("UnitType", "ProtoMech");
        } else if (t instanceof Mech) {
            blk.writeBlockData("UnitType", "Mech");
        } else if (t instanceof GunEmplacement) {
            blk.writeBlockData("UnitType", "GunEmplacement");
        } else if (t instanceof LargeSupportTank) {
            blk.writeBlockData("UnitType", "LargeSupportTank");
        } else if (t instanceof SupportTank) {
            blk.writeBlockData("UnitType", "SupportTank");
        } else if (t instanceof SupportVTOL) {
            blk.writeBlockData("UnitType", "SupportVTOL");
        } else if (t instanceof VTOL) {
            blk.writeBlockData("UnitType", "VTOL");
        } else if (t instanceof ConvFighter) {
            blk.writeBlockData("UnitType", "ConvFighter");
        } else if (t instanceof Dropship) {
            blk.writeBlockData("UnitType", "Dropship");
        } else if (t instanceof SmallCraft) {
            blk.writeBlockData("UnitType", "SmallCraft");
        } else if (t instanceof Warship) {
            blk.writeBlockData("UnitType", "Warship");
        } else if (t instanceof SpaceStation) {
            blk.writeBlockData("UnitType", "SpaceStation");
        } else if (t instanceof Jumpship) {
            blk.writeBlockData("UnitType", "Jumpship");
        } else if (t instanceof Tank) {
            blk.writeBlockData("UnitType", "Tank");
        } else if (t instanceof Infantry) {
            blk.writeBlockData("UnitType", "Infantry");
        } else if (t instanceof Aero) {
            blk.writeBlockData("UnitType", "Aero");
        }

        blk.writeBlockData("blockversion", 1);
        blk.writeBlockData("Name", t.getChassis());
        blk.writeBlockData("Model", t.getModel());
        blk.writeBlockData("year", t.getYear());
        String type;
        if (t.isMixedTech()) {
            if (!t.isClan()) {
                type = "Mixed (IS Chassis)";
            } else {
                type = "Mixed (Clan Chassis)";
            }
        } else {
            switch (t.getTechLevel()) {
                case TechConstants.T_INTRO_BOXSET:
                    type = "IS Level 1";
                    break;
                case TechConstants.T_IS_TW_NON_BOX:
                    type = "IS Level 2";
                    break;
                case TechConstants.T_IS_ADVANCED:
                default:
                    type = "IS Level 3";
                    break;
                case TechConstants.T_CLAN_TW:
                    type = "Clan Level 2";
                    break;
                case TechConstants.T_CLAN_ADVANCED:
                    type = "Clan Level 3";
                    break;
            }
        }
        blk.writeBlockData("type", type);
        blk.writeBlockData("tonnage", t.getWeight());
        blk.writeBlockData("motion_type", t.getMovementModeAsString());
        if (t.getTroopCarryingSpace() > 0) {
            blk.writeBlockData("transporters", "TroopSpace: " + t.getTroopCarryingSpace());
        }
        int engineCode = BLKFile.FUSION;
        switch (t.getEngine().getEngineType()) {
            case Engine.COMBUSTION_ENGINE:
                engineCode = BLKFile.ICE;
                break;
            case Engine.LIGHT_ENGINE:
                engineCode = BLKFile.LIGHT;
                break;
            case Engine.XL_ENGINE:
                engineCode = BLKFile.XL;
                break;
            case Engine.XXL_ENGINE:
                engineCode = BLKFile.XXL;
                break;
            case Engine.FUEL_CELL:
                engineCode = BLKFile.FUELCELL;
                break;
            case Engine.FISSION:
                engineCode = BLKFile.FISSION;
                break;
        }
        blk.writeBlockData("engine_type", engineCode);
        blk.writeBlockData("cruiseMP", t.getOriginalWalkMP());
        if (t.getArmorType() != 0) {
            blk.writeBlockData("armor_type", t.getArmorType());
            blk.writeBlockData("armor_tech", t.getArmorTechLevel());
        }
        if (t.getStructureType() != 0) {
            blk.writeBlockData("internal_type", t.getStructureType());
        }
        if (t.isOmni()) {
            blk.writeBlockData("omni", 1);
        }
        int armor_array[];
        armor_array = new int[t.locations() - 1];
        for (int i = 1; i < t.locations(); i++) {
            armor_array[i - 1] = t.getOArmor(i);
        }
        blk.writeBlockData("armor", armor_array);

        Vector<Vector<String>> eq = new Vector<Vector<String>>(t.locations());
        for (int i = 0; i < t.locations(); i++) {
            eq.add(new Vector<String>());
        }
        for (Mounted m : t.getEquipment()) {
            String name = m.getType().getInternalName();
            int loc = m.getLocation();
            if (loc != Entity.LOC_NONE) {
                eq.get(loc).add(name);
            }
        }
        for (int i = 0; i < t.locations(); i++) {
            blk.writeBlockData(t.getLocationName(i) + " Equipment", eq.get(i));
        }
        blk.writeBlockData("barrating", t.getBARRating());

        if (t.getFluff().getHistory().trim().length() > 0) {
            blk.writeBlockData("history", t.getFluff().getHistory());
        }

        if (t.getFluff().getMMLImagePath().trim().length() > 0) {
            blk.writeBlockData("imagepath", t.getFluff().getMMLImagePath());
        }

        if (t instanceof BattleArmor) {
            BattleArmor ba = (BattleArmor) t;
            if (ba.getChassisType() == BattleArmor.CHASSIS_TYPE_BIPED) {
                blk.writeBlockData("chassis", "biped");

            } else if (ba.getChassisType() == BattleArmor.CHASSIS_TYPE_QUAD) {
                blk.writeBlockData("chassis", "quad");
            }
        }
        blk.writeBlockFile(fileName);
    }
}
