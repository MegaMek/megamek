/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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

import java.util.Vector;

import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.CargoBay;
import megamek.common.ConvFighter;
import megamek.common.CrewQuartersCargoBay;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.FirstClassQuartersCargoBay;
import megamek.common.FixedWingSupport;
import megamek.common.GunEmplacement;
import megamek.common.HeavyVehicleBay;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.InsulatedCargoBay;
import megamek.common.Jumpship;
import megamek.common.LargeSupportTank;
import megamek.common.LightVehicleBay;
import megamek.common.LiquidCargoBay;
import megamek.common.LivestockCargoBay;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.MechBay;
import megamek.common.Mounted;
import megamek.common.PillionSeatCargoBay;
import megamek.common.Protomech;
import megamek.common.ProtomechBay;
import megamek.common.RefrigeratedCargoBay;
import megamek.common.SecondClassQuartersCargoBay;
import megamek.common.SmallCraft;
import megamek.common.SmallCraftBay;
import megamek.common.SpaceStation;
import megamek.common.StandardSeatCargoBay;
import megamek.common.SteerageQuartersCargoBay;
import megamek.common.SupportTank;
import megamek.common.SupportVTOL;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.Transporter;
import megamek.common.TroopSpace;
import megamek.common.VTOL;
import megamek.common.Warship;
import megamek.common.WeaponType;
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
    public static final int NONE = 8;
    public static final int MAGLEV = 9;
    public static final int STEAM = 10;
    public static final int BATTERY = 11;
    public static final int SOLAR = 12;

    protected void loadEquipment(Entity t, String sName, int nLoc)
            throws EntityLoadingException {
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) {
            return;
        }

        // prefix is "Clan " or "IS "
        String prefix;
        if (t.isClan()) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        if (saEquip[0] != null) {
            for (int x = 0; x < saEquip.length; x++) {
                String equipName = saEquip[x].trim();
                boolean isTurreted = false;
                boolean isPintleTurreted = false;
                if (equipName.toUpperCase().endsWith("(ST)")) {
                    isTurreted = true;
                    equipName = equipName.substring(0, equipName.length() - 4)
                            .trim();
                }
                if (equipName.toUpperCase().endsWith("(PT)")) {
                    isPintleTurreted = true;
                    equipName = equipName.substring(0, equipName.length() - 4)
                            .trim();
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
                if (equipName.toUpperCase().endsWith("(R)")) {
                    facing = 3;
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
                        Mounted mount = t.addEquipment(etype, nLoc, false,
                                BattleArmor.MOUNT_LOC_NONE, false, false,
                                isTurreted, isPintleTurreted);
                        // Need to set facing for VGLs
                        if ((etype instanceof WeaponType)
                                && etype.hasFlag(WeaponType.F_VGL)) {
                            // If no facing specified, assume front
                            if (facing == -1) {
                                mount.setFacing(0);
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
        } else if (code == BLKFile.NONE) {
            return Engine.NONE;
        } else if (code == BLKFile.MAGLEV) {
            return Engine.MAGLEV;
        } else if (code == BLKFile.STEAM) {
            return Engine.STEAM;
        } else if (code == BLKFile.BATTERY) {
            return Engine.BATTERY;
        } else if (code == BLKFile.SOLAR) {
            return Engine.SOLAR;
          
        } else {
            return -1;
        }
    }

    public void setFluff(Entity e) throws EntityLoadingException {

        if (dataFile.exists("capabilities")) {
            e.getFluff().setCapabilities(dataFile.getDataAsString("capabilities")[0]);
        }

        if (dataFile.exists("overview")) {
            e.getFluff().setOverview(dataFile.getDataAsString("overview")[0]);
        }

        if (dataFile.exists("deployment")) {
            e.getFluff().setDeployment(dataFile.getDataAsString("deployment")[0]);
        }

        if (dataFile.exists("history")) {
            e.getFluff().setHistory(dataFile.getDataAsString("history")[0]);
        }


        if (dataFile.exists("imagepath")) {
            e.getFluff().setMMLImagePath(
                    dataFile.getDataAsString("imagepath")[0]);
        }

        if (dataFile.exists("source")) {
            e.setSource(dataFile.getDataAsString("source")[0]);
        }

    }

    public void checkManualBV(Entity e) throws EntityLoadingException {
        if (dataFile.exists("bv")) {
            int bv = dataFile.getDataAsInt("bv")[0];

            if (bv != 0) {
                e.setUseManualBV(true);
                e.setManualBV(bv);
            }
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
        } else if (dataFile.getDataAsString("type")[0].equals("Clan")
                || dataFile.getDataAsString("type")[0].equals("Clan Level 2")) {
            e.setTechLevel(TechConstants.T_CLAN_TW);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 3")) {
            e.setTechLevel(TechConstants.T_CLAN_ADVANCED);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 4")) {
            e.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
        } else if (dataFile.getDataAsString("type")[0].equals("Clan Level 5")) {
            e.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (IS Chassis)")) {
            e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (IS Chassis) Advanced")) {
            e.setTechLevel(TechConstants.T_IS_ADVANCED);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (IS Chassis) Experimental")) {
            e.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (IS Chassis) Unofficial")) {
            e.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (Clan Chassis)")) {
            e.setTechLevel(TechConstants.T_CLAN_TW);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (Clan Chassis) Advanced")) {
            e.setTechLevel(TechConstants.T_CLAN_ADVANCED);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (Clan Chassis) Experimental")) {
            e.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0]
                .equals("Mixed (Clan Chassis) Unofficial")) {
            e.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
            e.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed")) {
            throw new EntityLoadingException(
                    "Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
        } else {
            throw new EntityLoadingException("Unsupported tech level: "
                    + dataFile.getDataAsString("type")[0]);
        }
    }

    public static BuildingBlock getBlock(Entity t) {
        BuildingBlock blk = new BuildingBlock();
        blk.createNewBlock();

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
        } else if (t instanceof FixedWingSupport) {
            blk.writeBlockData("UnitType", "FixedWingSupport");
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
            if ((t.getTechLevel() == TechConstants.T_IS_ADVANCED)
                    || (t.getTechLevel() == TechConstants.T_CLAN_ADVANCED)) {
                type += " Advanced";
            } else if ((t.getTechLevel() == TechConstants.T_IS_EXPERIMENTAL)
                    || (t.getTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL)) {
                type += " Experimental";
            }
            if ((t.getTechLevel() == TechConstants.T_IS_UNOFFICIAL)
                    || (t.getTechLevel() == TechConstants.T_CLAN_UNOFFICIAL)) {
                type += " Unofficial";
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
                    type = "IS Level 3";
                    break;
                case TechConstants.T_IS_EXPERIMENTAL:
                    type = "IS Level 4";
                    break;
                case TechConstants.T_IS_UNOFFICIAL:
                default:
                    type = "IS Level 5";
                    break;
                case TechConstants.T_CLAN_TW:
                    type = "Clan Level 2";
                    break;
                case TechConstants.T_CLAN_ADVANCED:
                    type = "Clan Level 3";
                    break;
                case TechConstants.T_CLAN_EXPERIMENTAL:
                    type = "Clan Level 4";
                    break;
                case TechConstants.T_CLAN_UNOFFICIAL:
                    type = "Clan Level 5";
                    break;
            }
        }
        blk.writeBlockData("type", type);

        blk.writeBlockData("motion_type", t.getMovementModeAsString());

        for (Transporter tran : t.getTransports()) {
            blk.writeBlockData("transporters", tran.toString());
        }

        if (!((t instanceof Infantry) && !(t instanceof BattleArmor))) {
            if (t instanceof Aero){
                blk.writeBlockData("SafeThrust", t.getOriginalWalkMP());
            } else {
                blk.writeBlockData("cruiseMP", t.getOriginalWalkMP());
            }
        }

        int numLocs = t.locations();
        // Aeros have an extra special location called "wings" that we
        //  don't want to consider
        if (t instanceof Aero){
            numLocs--;
        }

        if (!(t instanceof Infantry)) {
            if (t instanceof Aero){
                blk.writeBlockData("cockpit_type", ((Aero)t).getCockpitType());
                blk.writeBlockData("heatsinks", ((Aero)t).getHeatSinks());
                blk.writeBlockData("sink_type", ((Aero)t).getHeatType());
                blk.writeBlockData("fuel", ((Aero)t).getFuel());
            }
            if(t.hasEngine()) {
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
                    case Engine.NONE:
                        engineCode = BLKFile.NONE;
                        break;
                }
                blk.writeBlockData("engine_type", engineCode);
            }
            if (!t.hasPatchworkArmor() && (t.getArmorType(1) != 0)) {
                blk.writeBlockData("armor_type", t.getArmorType(1));
                blk.writeBlockData("armor_tech", t.getArmorTechLevel(1));
            } else if (t.hasPatchworkArmor()) {
                blk.writeBlockData("armor_type",
                        EquipmentType.T_ARMOR_PATCHWORK);
                for (int i = 1; i < numLocs; i++) {
                    blk.writeBlockData(t.getLocationName(i) + "_armor_type",
                            t.getArmorType(i));
                    blk.writeBlockData(t.getLocationName(i) + "_armor_tech",
                            TechConstants.getTechName(t.getArmorTechLevel(i)));
                }
            }
            if (t.getStructureType() != 0) {
                blk.writeBlockData("internal_type", t.getStructureType());
            }
            if (t.isOmni()) {
                blk.writeBlockData("omni", 1);
            }
            int armor_array[];
            if (t instanceof Aero){
                armor_array = new int[numLocs];
                for (int i = 0; i < numLocs; i++) {
                    armor_array[i] = t.getOArmor(i);
                }
            } else {
                armor_array = new int[numLocs - 1];
                for (int i = 1; i < numLocs; i++) {
                    armor_array[i - 1] = t.getOArmor(i);
                }
            }
            blk.writeBlockData("armor", armor_array);
        }

        // Write out armor_type and armor_tech entries for BA
        if (t instanceof BattleArmor){
            blk.writeBlockData("armor_type", t.getArmorType(1));
            blk.writeBlockData("armor_tech", t.getArmorTechLevel(1));
        }

        Vector<Vector<String>> eq = new Vector<Vector<String>>(numLocs);

        for (int i = 0; i < numLocs; i++) {
            eq.add(new Vector<String>());
        }
        for (Mounted m : t.getEquipment()) {
            // Ignore Mounteds that represent a WeaponGroup
            if (m.isWeaponGroup()){
                continue;
            }

            // Ignore ammo for one-shot launchers
            if (m.getLinkedBy() != null
                    && m.getLinkedBy().isOneShot()){
                continue;
            }

            String name = m.getType().getInternalName();
            if (m.isSponsonTurretMounted()) {
                name = name + "(ST)";
            }
            if (m.isMechTurretMounted()) {
                name = name + "(T)";
            }
            if (m.isPintleTurretMounted()) {
                name = name + "(PT)";
            }
            if (m.isDWPMounted()){
                name += ":DWP";
            }
            if (m.isAPMMounted()){
                name += ":APM";
            }
            if (m.isSquadSupportWeapon()){
                name += ":SSWM";
            }
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_BODY){
                name += ":Body";
            }
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM){
                name += ":LA";
            }
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM){
                name += ":RA";
            }
            // For BattleArmor, we need to save how many shots are in this
            //  location
            if ((t instanceof BattleArmor)
                    && (m.getType() instanceof AmmoType)){
                name += ":Shots" + m.getBaseShotsLeft() + "#";
            }
            int loc = m.getLocation();
            if (loc != Entity.LOC_NONE) {
                eq.get(loc).add(name);
            }
        }
        for (int i = 0; i < numLocs; i++) {
            if (!(((t instanceof Infantry) && !(t instanceof BattleArmor)) && (i == Infantry.LOC_INFANTRY))) {
                blk.writeBlockData(t.getLocationName(i) + " Equipment",
                        eq.get(i));
            }
        }
        if (!t.hasPatchworkArmor() && t.hasBARArmor(1)) {
            blk.writeBlockData("barrating", t.getBARRating(1));
        }

        if (t.getFluff().getCapabilities().trim().length() > 0) {
            blk.writeBlockData("capabilities", t.getFluff().getCapabilities());
        }

        if (t.getFluff().getOverview().trim().length() > 0) {
            blk.writeBlockData("overview", t.getFluff().getOverview());
        }

        if (t.getFluff().getDeployment().trim().length() > 0) {
            blk.writeBlockData("deployment", t.getFluff().getDeployment());
        }

        if (t.getFluff().getDeployment().trim().length() > 0) {
            blk.writeBlockData("history", t.getFluff().getHistory());
        }

        if (t.getFluff().getMMLImagePath().trim().length() > 0) {
            blk.writeBlockData("imagepath", t.getFluff().getMMLImagePath());
        }

        if (t.getSource().trim().length() > 0) {
            blk.writeBlockData("source", t.getSource());
        }

        if (t instanceof BattleArmor) {
            BattleArmor ba = (BattleArmor) t;
            if (ba.getChassisType() == BattleArmor.CHASSIS_TYPE_BIPED) {
                blk.writeBlockData("chassis", "biped");

            } else if (ba.getChassisType() == BattleArmor.CHASSIS_TYPE_QUAD) {
                blk.writeBlockData("chassis", "quad");
            }
            if (ba.isExoskeleton()) {
                blk.writeBlockData("exoskeleton", "true");
            }
            blk.writeBlockData("jumpingMP", ba.getOriginalJumpMP());
            blk.writeBlockData("armor", new int[] { ba.getArmor(1) });
            blk.writeBlockData("Trooper Count", (int) t.getWeight());
            blk.writeBlockData("weightclass", ba.getWeightClass());
        } else if (t instanceof Infantry) {
            Infantry infantry = (Infantry) t;
            blk.writeBlockData("squad_size", infantry.getSquadSize());
            blk.writeBlockData("squadn", infantry.getSquadN());
            if (infantry.getSecondaryN() > 0) {
                blk.writeBlockData("secondn", infantry.getSecondaryN());
            }
            if (null != infantry.getPrimaryWeapon()) {
                blk.writeBlockData("Primary", infantry.getPrimaryWeapon()
                        .getInternalName());
            }
            if (null != infantry.getSecondaryWeapon()) {
                blk.writeBlockData("Secondary", infantry.getSecondaryWeapon()
                        .getInternalName());
            }
            
            if (infantry.canMakeAntiMekAttacks()) {
                blk.writeBlockData("antimek", (infantry.getAntiMekSkill() + ""));
            }
            
            if (infantry.getDamageDivisor() != 1) {
                blk.writeBlockData("armordivisor",
                        Double.toString(infantry.getDamageDivisor()));
            }
            if (infantry.isArmorEncumbering()) {
                blk.writeBlockData("encumberingarmor", "true");
            }
            if (infantry.hasSpaceSuit()) {
                blk.writeBlockData("spacesuit", "true");
            }
            if (infantry.hasDEST()) {
                blk.writeBlockData("dest", "true");
            }
            if (infantry.hasSneakCamo()) {
                blk.writeBlockData("sneakcamo", "true");
            }
            if (infantry.hasSneakIR()) {
                blk.writeBlockData("sneakir", "true");
            }
            if (infantry.hasSneakECM()) {
                blk.writeBlockData("sneakecm", "true");
            }
        } else {
            blk.writeBlockData("tonnage", t.getWeight());
        }

        if (t.getUseManualBV()) {
            blk.writeBlockData("bv", t.getManualBV());
        }

        if ((t instanceof Tank) && t.isOmni()) {
            Tank tank = (Tank) t;
            if (tank.getBaseChassisTurretWeight() >= 0) {
                blk.writeBlockData("baseChassisTurretWeight",
                        tank.getBaseChassisTurretWeight());
            }
            if (tank.getBaseChassisTurret2Weight() >= 0) {
                blk.writeBlockData("baseChassisTurret2Weight",
                        tank.getBaseChassisTurret2Weight());
            }
        }

        if (t instanceof Tank) {
            Tank tank = (Tank) t;
            if (tank.hasNoControlSystems()) {
                blk.writeBlockData("hasNoControlSystems", 1);
            }
        }

        return blk;
    }

    public static void encode(String fileName, Entity t) {
        BuildingBlock blk = BLKFile.getBlock(t);
        blk.writeBlockFile(fileName);
    }

    protected void addTransports(Entity e) {
        if (dataFile.exists("transporters")) {
            String[] transporters = dataFile.getDataAsString("transporters");
            // Walk the array of transporters.
            for (String transporter : transporters) {
                transporter = transporter.toLowerCase();
                // for bays, we have to save the baynumber in each bay, because
                // one conceputal bay can contain several different ones
                // we default to bay 1
                int bayNumber = 1;
                // TroopSpace:
                if (transporter.startsWith("troopspace:", 0)) {
                    // Everything after the ':' should be the space's size.
                    Double fsize = new Double(transporter.substring(11));
                    e.addTransporter(new TroopSpace(fsize));
                } else if (transporter.startsWith("cargobay:", 0)) {
                    String numbers = transporter.substring(9);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new CargoBay(size, doors, bayNumber));
                } else if (transporter.startsWith("liquidcargobay:", 0)) {
                    String numbers = transporter.substring(15);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new LiquidCargoBay(size, doors, bayNumber));
                } else if (transporter.startsWith("insulatedcargobay:", 0)) {
                    String numbers = transporter.substring(18);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new InsulatedCargoBay(size, doors,
                            bayNumber));
                } else if (transporter.startsWith("refrigeratedcargobay:", 0)) {
                    String numbers = transporter.substring(21);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new RefrigeratedCargoBay(size, doors,
                            bayNumber));
                } else if (transporter.startsWith("livestockcargobay:", 0)) {
                    String numbers = transporter.substring(18);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new LivestockCargoBay(size, doors,
                            bayNumber));
                } else if (transporter.startsWith("asfbay:", 0)) {
                    String numbers = transporter.substring(7);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new ASFBay(size, doors, bayNumber));
                } else if (transporter.startsWith("smallcraftbay:", 0)) {
                    String numbers = transporter.substring(14);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new SmallCraftBay(size, doors, bayNumber));
                } else if (transporter.startsWith("mechbay:", 0)) {
                    String numbers = transporter.substring(8);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new MechBay(size, doors, bayNumber));
                } else if (transporter.startsWith("lightvehiclebay:", 0)) {
                    String numbers = transporter.substring(16);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new LightVehicleBay(size, doors, bayNumber));
                } else if (transporter.startsWith("heavyvehiclebay:", 0)) {
                    String numbers = transporter.substring(16);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new HeavyVehicleBay(size, doors, bayNumber));
                } else if (transporter.startsWith("infantrybay:", 0)) {
                    String numbers = transporter.substring(12);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new InfantryBay(size, doors, bayNumber));
                } else if (transporter.startsWith("battlearmorbay:", 0)) {
                    String numbers = transporter.substring(15);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    boolean comstar = false;
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    } catch (NumberFormatException ex) {
                        // if not a number, check for c* bay
                        if (temp[2].equalsIgnoreCase("c*")) {
                            comstar = true;
                        }
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    if (temp.length == 4) {
                        if (temp[3].equalsIgnoreCase("c*")) {
                            comstar = true;
                        }
                    }
                    e.addTransporter(new BattleArmorBay(size, doors, bayNumber,
                            e.isClan(), comstar));
                } else if (transporter.startsWith("bay:", 0)) {
                    String numbers = transporter.substring(4);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new Bay(size, doors, bayNumber));
                } else if (transporter.startsWith("protomechbay:", 0)) {
                    String numbers = transporter.substring(13);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    try {
                        bayNumber = Integer.parseInt(temp[2]);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        // Find an unused bay number and use it.
                        int i = 1;
                        while (e.getBayById(i) != null) {
                            i++;
                        }
                        bayNumber = i;
                    }
                    e.addTransporter(new ProtomechBay(size, doors, bayNumber));
                } else if (transporter.startsWith("crewquarters:", 0)) {
                    String numbers = transporter.substring(13);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    e.addTransporter(new CrewQuartersCargoBay(size, doors));
                } else if (transporter.startsWith("steeragequarters:", 0)) {
                    String numbers = transporter.substring(17);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    e.addTransporter(new SteerageQuartersCargoBay(size, doors));
                } else if (transporter.startsWith("2ndclassquarters:", 0)) {
                    String numbers = transporter.substring(17);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    e.addTransporter(new SecondClassQuartersCargoBay(size,
                            doors));
                } else if (transporter.startsWith("1stclassquarters:", 0)) {
                    String numbers = transporter.substring(17);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    e.addTransporter(new FirstClassQuartersCargoBay(size, doors));
                } else if (transporter.startsWith("pillionseats:", 0)) {
                    String numbers = transporter.substring(13);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    e.addTransporter(new PillionSeatCargoBay(size, doors));
                } else if (transporter.startsWith("standardseats:", 0)) {
                    String numbers = transporter.substring(14);
                    String temp[] = numbers.split(":");
                    double size = Double.parseDouble(temp[0]);
                    int doors = Integer.parseInt(temp[1]);
                    e.addTransporter(new StandardSeatCargoBay(size, doors));
                }

            } // Handle the next transportation component.

        } // End has-transporters
    }
}
