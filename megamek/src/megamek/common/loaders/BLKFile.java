/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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

import java.util.*;
import java.util.stream.Collectors;

import megamek.common.*;
import megamek.common.InfantryBay.PlatoonType;
import megamek.common.options.IOption;
import megamek.common.options.PilotOptions;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;

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
    public static final int EXTERNAL = 13;
    
    private static final String COMSTAR_BAY = "c*";

    static final String BLK_EXTRA_SEATS = "extra_seats";
    
    /**
     * If a vehicular grenade launcher does not have a facing provided, assign a default facing.
     * For vehicles this is determined by location. For protomechs the only legal location is
     * the torso, but it may be mounted rear-facing.
     * 
     * @param location The location where the VGL is mounted.
     * @param rear     Whether the VGL is rear-facing.
     * @return         The facing to assign to the VGL.
     */
    protected int defaultVGLFacing(int location, boolean rear) {
        return rear ? 3 : 0;
    }

    public int defaultAeroVGLFacing(int location, boolean rearFacing) {
        switch (location) {
            case Aero.LOC_LWING:
                return rearFacing ? 4 : 5;
            case Aero.LOC_RWING:
                return rearFacing ? 2 : 1;
            case Aero.LOC_AFT:
                return 4;
            case Aero.LOC_NOSE:
            default:
                return 0;
        }
    }

    /** Legacy support for Drone Carrier Control System capacity using additional equipment */
    int legacyDCCSCapacity = 0;
    /** Legacy support for MASH capacity using additional equipment */
    int mashOperatingTheaters = 0;

    /**
     * Legacy support for variable sized equipment that expands capacity by using an
     * additional MiscType.
     *
     * @param lookup The lookup name
     */
    boolean checkLegacyExtraEquipment(String lookup) {
        switch (lookup) {
            case "MASH Operation Theater":
                mashOperatingTheaters++;
                return true;
            case "ISDroneExtra":
            case "CLDroneExtra":
                legacyDCCSCapacity++;
                return true;
            default:
                return false;
        }
    }

    /**
     * Legacy support for variable equipment that had a separate EquipmentType entry for each possible
     * size
     *
     * @param eqName The equipment lookup name
     * @return       The size of the equipment
     */
    static double getLegacyVariableSize(String eqName) {
        if (eqName.startsWith("Cargo")
                || eqName.startsWith("Liquid Storage")
                || eqName.startsWith("Communications Equipment")) {
            return Double.parseDouble(eqName.substring(eqName.indexOf("(") + 1,
                    eqName.indexOf(" ton")));
        }
        if (eqName.startsWith("CommsGear")) {
            return Double.parseDouble(eqName.substring(eqName.indexOf(":") + 1));
        }
        if (eqName.startsWith("Mission Equipment Storage")) {
            int pos = eqName.indexOf("(");
            if (pos > 0) {
                return Double.parseDouble(eqName.substring(pos + 1,
                        eqName.indexOf("kg")).trim());
            } else {
                // If the internal name does not include a size, it's the original 20 kg version.
                return 0.02;
            }
        }
        if (eqName.startsWith("Ladder")) {
            return Double.parseDouble(eqName.substring(eqName.indexOf("(") + 1,
                    eqName.indexOf("m)")));
        }
        return 1.0;
    }

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
            for (String s : saEquip) {
                String equipName = s.trim();
                boolean isOmniMounted = false;
                boolean isTurreted = false;
                boolean isPintleTurreted = false;
                double size = 0.0;
                int sizeIndex = equipName.toUpperCase().indexOf(":SIZE:");
                if (sizeIndex > 0) {
                    size = Double.parseDouble(equipName.substring(sizeIndex + 6));
                    equipName = equipName.substring(0, sizeIndex);
                }
                if (equipName.toUpperCase().endsWith(":OMNI")) {
                    isOmniMounted = true;
                    equipName = equipName.substring(0, equipName.length() - 5).trim();
                }
                if (equipName.toUpperCase().endsWith("(PT)")) {
                    isPintleTurreted = true;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(ST)")) {
                    isTurreted = true;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }

                int facing = -1;
                if (equipName.toUpperCase().endsWith("(FL)")) {
                    facing = 5;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(FR)")) {
                    facing = 1;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(RL)")) {
                    facing = 4;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(RR)")) {
                    facing = 2;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                if (equipName.toUpperCase().endsWith("(R)")) {
                    facing = 3;
                    equipName = equipName.substring(0, equipName.length() - 4).trim();
                }
                EquipmentType etype = EquipmentType.get(equipName);

                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }
                if ((etype == null) && checkLegacyExtraEquipment(equipName)) {
                    continue;
                }

                // The stealth armor mount is added when the armor type is set
                if ((etype instanceof MiscType) && etype.hasFlag(MiscType.F_STEALTH)) {
                    continue;
                }

                if (etype != null) {
                    try {
                        Mounted mount = t.addEquipment(etype, nLoc, false,
                                BattleArmor.MOUNT_LOC_NONE, false, false,
                                isTurreted, isPintleTurreted, isOmniMounted);
                        // Need to set facing for VGLs
                        if ((etype instanceof WeaponType)
                                && etype.hasFlag(WeaponType.F_VGL)) {
                            if (facing == -1) {
                                mount.setFacing(defaultVGLFacing(nLoc, false));
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

    public boolean isMine() {
        return dataFile.exists("blockversion");
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
        } else if (code == BLKFile.EXTERNAL) {
            return Engine.EXTERNAL;
        } else {
            return -1;
        }
    }

    public void setFluff(Entity e) {

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
        
        if (dataFile.exists("manufacturer")) {
            e.getFluff().setManufacturer(dataFile.getDataAsString("manufacturer")[0]);
        }

        if (dataFile.exists("primaryFactory")) {
            e.getFluff().setPrimaryFactory(dataFile.getDataAsString("primaryFactory")[0]);
        }
        
        if (dataFile.exists("systemManufacturers")) {
            for (String line : dataFile.getDataAsString("systemManufacturers")) {
                String[] fields = line.split(":");
                EntityFluff.System comp = EntityFluff.System.parse(fields[0]);
                if ((null != comp) && (fields.length > 1)) {
                    e.getFluff().setSystemManufacturer(comp, fields[1]);
                }
            }
        }

        if (dataFile.exists("systemModels")) {
            for (String line : dataFile.getDataAsString("systemModels")) {
                String[] fields = line.split(":");
                EntityFluff.System comp = EntityFluff.System.parse(fields[0]);
                if ((null != comp) && (fields.length > 1)) {
                    e.getFluff().setSystemModel(comp, fields[1]);
                }
            }
        }

        if (dataFile.exists("imagepath")) {
            e.getFluff().setMMLImagePath(
                    dataFile.getDataAsString("imagepath")[0]);
        }

        if (dataFile.exists("notes")) {
            e.getFluff().setNotes(dataFile.getDataAsString("notes")[0]);
        }
        
        if (dataFile.exists("use")) {
            e.getFluff().setUse(dataFile.getDataAsString("use")[0]);
        }
        
        if (dataFile.exists("length")) {
            e.getFluff().setLength(dataFile.getDataAsString("length")[0]);
        }
        
        if (dataFile.exists("width")) {
            e.getFluff().setWidth(dataFile.getDataAsString("width")[0]);
        }
        
        if (dataFile.exists("height")) {
            e.getFluff().setHeight(dataFile.getDataAsString("height")[0]);
        }
        
        if (dataFile.exists("source")) {
            e.setSource(dataFile.getDataAsString("source")[0]);
        }
    }

    public void checkManualBV(Entity e) {
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

        switch (dataFile.getDataAsString("type")[0]) {
            case "IS":
                if (e.getYear() == 3025) {
                    e.setTechLevel(TechConstants.T_INTRO_BOXSET);
                } else {
                    e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                }
                break;
            case "IS Level 1":
                e.setTechLevel(TechConstants.T_INTRO_BOXSET);
                break;
            case "IS Level 2":
                e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                break;
            case "IS Level 3":
                e.setTechLevel(TechConstants.T_IS_ADVANCED);
                break;
            case "IS Level 4":
                e.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                break;
            case "IS Level 5":
                e.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                break;
            case "Clan":
            case "Clan Level 2":
                e.setTechLevel(TechConstants.T_CLAN_TW);
                break;
            case "Clan Level 3":
                e.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                break;
            case "Clan Level 4":
                e.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                break;
            case "Clan Level 5":
                e.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                break;
            case "Mixed (IS Chassis)":
                e.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                e.setMixedTech(true);
                break;
            case "Mixed (IS Chassis) Advanced":
                e.setTechLevel(TechConstants.T_IS_ADVANCED);
                e.setMixedTech(true);
                break;
            case "Mixed (IS Chassis) Experimental":
                e.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                e.setMixedTech(true);
                break;
            case "Mixed (IS Chassis) Unofficial":
                e.setTechLevel(TechConstants.T_IS_UNOFFICIAL);
                e.setMixedTech(true);
                break;
            case "Mixed (Clan Chassis)":
                e.setTechLevel(TechConstants.T_CLAN_TW);
                e.setMixedTech(true);
                break;
            case "Mixed (Clan Chassis) Advanced":
                e.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                e.setMixedTech(true);
                break;
            case "Mixed (Clan Chassis) Experimental":
                e.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                e.setMixedTech(true);
                break;
            case "Mixed (Clan Chassis) Unofficial":
                e.setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
                e.setMixedTech(true);
                break;
            case "Mixed":
                throw new EntityLoadingException("Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
            default:
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
        if (t.getOriginalBuildYear() >= 0) {
            blk.writeBlockData("originalBuildYear", t.getOriginalBuildYear());
        }
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

        String[] transporter_array = new String[t.getTransports().size()];
        int index = 0;
        for (Transporter transporter : t.getTransports()) {
            transporter_array[index] = transporter.toString();
            if (t.isPodMountedTransport(transporter)) {
                transporter_array[index] += ":omni";
            }
            index++;
        }
        blk.writeBlockData("transporters", transporter_array);

        if (!t.isConventionalInfantry()) {
            if (t instanceof Aero) {
                blk.writeBlockData("SafeThrust", t.getOriginalWalkMP());
            } else {
                blk.writeBlockData("cruiseMP", t.getOriginalWalkMP());
                if (t.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                    blk.writeBlockData("jumpingMP", t.getOriginalJumpMP());
                    blk.writeBlockData("interface_cockpit",
                            String.valueOf(((Protomech) t).hasInterfaceCockpit()));
                }
            }
        }

        int numLocs = t.locations();
        if (!(t instanceof Infantry)) {
            if (t instanceof Aero) {
                if (t.isFighter()) {
                    blk.writeBlockData("cockpit_type", ((Aero) t).getCockpitType());
                    if (t.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER) && ((Aero) t).isVSTOL()) {
                        blk.writeBlockData("vstol", 1);
                    }
                } else if ((t instanceof Dropship) && t.isPrimitive()) {
                    blk.writeBlockData("collartype", ((Dropship) t).getCollarType());
                }
                blk.writeBlockData("heatsinks", ((Aero) t).getHeatSinks());
                blk.writeBlockData("sink_type", ((Aero) t).getHeatType());
                if (((Aero) t).getPodHeatSinks() > 0) {
                    blk.writeBlockData("omnipodheatsinks", ((Aero) t).getPodHeatSinks());
                }
                blk.writeBlockData("fuel", ((Aero) t).getFuel());
            }
            if (t.hasEngine()) {
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
                    case Engine.MAGLEV:
                        engineCode = BLKFile.MAGLEV;
                        break;
                    case Engine.STEAM:
                        engineCode = BLKFile.STEAM;
                        break;
                    case Engine.BATTERY:
                        engineCode = BLKFile.BATTERY;
                        break;
                    case Engine.SOLAR:
                        engineCode = BLKFile.SOLAR;
                        break;
                    case Engine.EXTERNAL:
                        engineCode = BLKFile.EXTERNAL;
                        break;
                }
                blk.writeBlockData("engine_type", engineCode);
                if (t.getEngine().isClan() != t.isClan()) {
                    blk.writeBlockData("clan_engine", Boolean.toString(t.getEngine().isClan()));
                }
            }
            if (!t.hasPatchworkArmor() && (t.getArmorType(1) != 0)) {
                blk.writeBlockData("armor_type", t.getArmorType(1));
                blk.writeBlockData("armor_tech", t.getArmorTechLevel(1));
            } else if (t.hasPatchworkArmor()) {
                blk.writeBlockData("armor_type",
                        EquipmentType.T_ARMOR_PATCHWORK);
                for (int i = 1; i < t.locations(); i++) {
                    blk.writeBlockData(t.getLocationName(i) + "_armor_type", t.getArmorType(i));
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
            
            int[] armor_array;
            if (t.hasETypeFlag(Entity.ETYPE_AERO)) {
                if (t.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                    armor_array = new int[6];
                } else {
                    armor_array = new int[4];
                }
                for (int i = 0; i < armor_array.length; i++) {
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
        if (t instanceof BattleArmor) {
            blk.writeBlockData("armor_type", t.getArmorType(1));
            blk.writeBlockData("armor_tech", t.getArmorTechLevel(1));
        }

        Vector<Vector<String>> eq = new Vector<>(numLocs);

        for (int i = 0; i < numLocs; i++) {
            eq.add(new Vector<>());
        }
        for (Mounted m : t.getEquipment()) {
            // Ignore Mounteds that represent a WeaponGroup
            // BA anti-personnel weapons are written just after the mount
            if (m.isWeaponGroup() || m.isAPMMounted()) {
                continue;
            }

            // Ignore ammo for one-shot launchers
            if ((m.getLinkedBy() != null) && (m.getLinkedBy().isOneShot())) {
                continue;
            }
            
            if (m.getType() instanceof BayWeapon) {
                int loc = m.getLocation();
                if (loc == Entity.LOC_NONE) {
                    continue;
                }
                boolean rear = m.isRearMounted();
                for (int i = 0; i < m.getBayWeapons().size(); i++) {
                    Mounted w = t.getEquipment(m.getBayWeapons().get(i));
                    String name = w.getType().getInternalName();
                    if (i == 0) {
                        name = "(B) " + name;
                    }
                    if (rear) {
                        name = "(R) " + name;
                    }
                    eq.get(loc).add(name);
                }
                for (Integer aNum : m.getBayAmmo()) {
                    Mounted a = t.getEquipment(aNum);
                    String name = a.getType().getInternalName();
                    name += ":" + a.getBaseShotsLeft();
                    if (rear) {
                        name = "(R) " + name;
                    }
                    eq.get(loc).add(name);
                }
                continue;
            }

            if (t.usesWeaponBays() && ((m.getType() instanceof WeaponType)
                    || (m.getType() instanceof AmmoType))) {
                continue;
            }

            String name = encodeEquipmentLine(m);
            int loc = m.getLocation();
            if (loc != Entity.LOC_NONE) {
                eq.get(loc).add(name);
            }
            if ((m.getLinked() != null) && m.getLinked().isAPMMounted()) {
                eq.get(loc).add(encodeEquipmentLine(m.getLinked()));
            }
        }
        for (int i = 0; i < numLocs; i++) {
            if (!(t.isConventionalInfantry() && (i == Infantry.LOC_INFANTRY))) {
                blk.writeBlockData(t.getLocationName(i) + " Equipment", eq.get(i));
            }
        }
        if (!t.hasPatchworkArmor() && t.hasBARArmor(1)) {
            blk.writeBlockData("barrating", t.getBARRating(1));
        }
        
        if (t.isSupportVehicle()) {
            blk.writeBlockData("structural_tech_rating", t.getStructuralTechRating());
            blk.writeBlockData("engine_tech_rating", t.getEngineTechRating());
            blk.writeBlockData("armor_tech_rating", t.getArmorTechRating());
        }
        
        if (t.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT) || t.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            blk.writeBlockData("structural_integrity", ((Aero) t).get0SI());
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

        if (t.getFluff().getHistory().trim().length() > 0) {
            blk.writeBlockData("history", t.getFluff().getHistory());
        }

        if (t.getFluff().getManufacturer().trim().length() > 0) {
            blk.writeBlockData("manufacturer", t.getFluff().getManufacturer());
        }

        if (t.getFluff().getPrimaryFactory().trim().length() > 0) {
            blk.writeBlockData("primaryFactory", t.getFluff().getPrimaryFactory());
        }
        
        List<String> list = t.getFluff().createSystemManufacturersList();
        if (!list.isEmpty()) {
            blk.writeBlockData("systemManufacturers", list);
        }

        list = t.getFluff().createSystemModelsList();
        if (!list.isEmpty()) {
            blk.writeBlockData("systemModels", list);
        }

        if (t.getFluff().getMMLImagePath().trim().length() > 0) {
            blk.writeBlockData("imagepath", t.getFluff().getMMLImagePath());
        }

        if (t.getFluff().getNotes().trim().length() > 0) {
            blk.writeBlockData("notes", t.getFluff().getNotes());
        }

        if (t.getFluff().getUse().trim().length() > 0) {
            blk.writeBlockData("use", t.getFluff().getUse());
        }

        if (t.getFluff().getLength().trim().length() > 0) {
            blk.writeBlockData("length", t.getFluff().getLength());
        }

        if (t.getFluff().getWidth().trim().length() > 0) {
            blk.writeBlockData("width", t.getFluff().getWidth());
        }

        if (t.getFluff().getHeight().trim().length() > 0) {
            blk.writeBlockData("height", t.getFluff().getHeight());
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
                if (ba.getTurretCapacity() > 0) {
                    blk.writeBlockData("turret",
                            (ba.hasModularTurretMount() ? "Modular:" : "Standard:") + ba.getTurretCapacity());
                }
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
            
            EquipmentType et = infantry.getArmorKit();
            if (et != null) {
                blk.writeBlockData("armorKit", et.getInternalName());
            }
            if (infantry.getArmorDamageDivisor() != 1) {
                blk.writeBlockData("armordivisor",
                        Double.toString(infantry.getArmorDamageDivisor()));
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
            if (infantry.hasSpecialization()) {
                blk.writeBlockData("specialization", infantry.getSpecializations());
            }
            ArrayList<String> augmentations = new ArrayList<>();
            for (Enumeration<IOption> e = infantry.getCrew().getOptions(PilotOptions.MD_ADVANTAGES);
                    e.hasMoreElements();) {
                final IOption o = e.nextElement();
                if (o.booleanValue()) {
                    augmentations.add(o.getName());
                }
            }
            if (augmentations.size() > 0) {
                blk.writeBlockData("augmentation", augmentations.toArray(new String[augmentations.size()]));
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
            if (tank.getBaseChassisSponsonPintleWeight() >= 0) {
                blk.writeBlockData("baseChassisSponsonPintleWeight",
                        tank.getBaseChassisSponsonPintleWeight());
            }
        }

        if (t.isSupportVehicle() && t.isOmni()) {
            blk.writeBlockData("baseChassisFireConWeight",
                    t.getBaseChassisFireConWeight());
        }

        if (t instanceof Tank) {
            Tank tank = (Tank) t;
            if (tank.isSupportVehicle()) {
                blk.writeBlockData("fuel", tank.getFuelTonnage());
            }
            if (tank.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
                blk.writeBlockData("fuelType", tank.getICEFuelType().toString());
            }
            if (tank.hasNoControlSystems()) {
                blk.writeBlockData("hasNoControlSystems", 1);
            }
            if (!t.isSupportVehicle() && t.isTrailer()) {
                blk.writeBlockData("trailer", 1);
            }
            if (tank.getExtraCrewSeats() > 0) {
                blk.writeBlockData(BLK_EXTRA_SEATS, tank.getExtraCrewSeats());
            }
        }
        
        if (t instanceof SmallCraft) {
            SmallCraft sc = (SmallCraft) t;
            blk.writeBlockData("designtype", sc.getDesignType());
            blk.writeBlockData("crew", sc.getNCrew());
            blk.writeBlockData("officers", sc.getNOfficers());
            blk.writeBlockData("gunners", sc.getNGunners());
            blk.writeBlockData("passengers", sc.getNPassenger());
            blk.writeBlockData("marines", sc.getNMarines());
            blk.writeBlockData("battlearmor", sc.getNBattleArmor());
            blk.writeBlockData("otherpassenger", sc.getNOtherPassenger());
            blk.writeBlockData("life_boat", sc.getLifeBoats());
            blk.writeBlockData("escape_pod", sc.getEscapePods());
        }

        if (t instanceof Warship) {
            Warship ws = (Warship) t;
            blk.writeBlockData("kf_core", ws.getDriveCoreType());
            if (ws.getDriveCoreType() == Warship.DRIVE_CORE_PRIMITIVE) {
                blk.writeBlockData("jump_range", ws.getJumpRange());
            }
        } else if ((t instanceof SpaceStation)
                && ((SpaceStation) t).isModular()) {
            blk.writeBlockData("modular", 1);
        }
        
        if (t instanceof Jumpship) {
            Jumpship js = (Jumpship) t;
            if (js.hasHPG()) {
                blk.writeBlockData("hpg", 1);
            }
            if (js.hasLF()) {
                blk.writeBlockData("lithium-fusion", 1);
            }
            blk.writeBlockData("sail", js.hasSail() ? 1 : 0);
            if (js.getTotalGravDeck() > 0) {
                blk.writeBlockData("grav_decks", (Vector<String>) js.getGravDecks().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toCollection(Vector::new)));
            }
            blk.writeBlockData("crew", js.getNCrew());
            blk.writeBlockData("officers", js.getNOfficers());
            blk.writeBlockData("gunners", js.getNGunners());
            blk.writeBlockData("passengers", js.getNPassenger());
            blk.writeBlockData("marines", js.getNMarines());
            blk.writeBlockData("battlearmor", js.getNBattleArmor());
            blk.writeBlockData("life_boat", js.getLifeBoats());
            blk.writeBlockData("escape_pod", js.getEscapePods());
        }
        return blk;
    }

    private static String encodeEquipmentLine(Mounted m) {
        String name = m.getType().getInternalName();
        if (m.isRearMounted()) {
            name = "(R) " + name;
        }
        if (m.isSponsonTurretMounted()) {
            name = name + "(ST)";
        }
        if (m.isMechTurretMounted()) {
            name = name + "(T)";
        }
        if (m.isPintleTurretMounted()) {
            name = name + "(PT)";
        }
        if (m.isDWPMounted()) {
            name += ":DWP";
        }
        if (m.isSquadSupportWeapon()) {
            name += ":SSWM";
        }
        if (m.isAPMMounted()) {
            name += ":APM";
        }
        if (m.isOmniPodMounted()) {
            name += ":OMNI";
        }
        if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_BODY) {
            name += ":Body";
        }
        if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM) {
            name += ":LA";
        }
        if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM) {
            name += ":RA";
        }
        if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_TURRET) {
            name += ":TU";
        }
        // For BattleArmor and ProtoMechs, we need to save how many shots are in this
        //  location but they have different formats, yay!
        if ((m.getEntity() instanceof BattleArmor) && (m.getType() instanceof AmmoType)) {
            name += ":Shots" + m.getBaseShotsLeft() + "#";
        } else if (m.getEntity() instanceof Protomech && (m.getType() instanceof AmmoType)) {
            name += " (" + m.getBaseShotsLeft() + ")";
        } else if (m.getType().isVariableSize()
                || (m.getEntity().isSupportVehicle() && (m.getType() instanceof InfantryWeapon))) {
            name += ":SIZE:" + m.getSize();
        }
        return name;
    }

    public static void encode(String fileName, Entity t) {
        BuildingBlock blk = BLKFile.getBlock(t);
        blk.writeBlockFile(fileName);
    }

    protected void addTransports(Entity e) {
        if (dataFile.exists("transporters")) {
            String[] transporters = dataFile.getDataAsString("transporters");
            HashSet<Integer> usedBayNumbers = new HashSet<>();
            
            // Walk the array of transporters.
            for (String transporter : transporters) {
                transporter = transporter.toLowerCase();
                boolean isPod = transporter.endsWith(":omni");
                transporter = transporter.replace(":omni", "");
                boolean hasARTS = transporter.startsWith("arts");
                if (hasARTS) {
                    transporter = transporter.substring(4);
                }

                // TroopSpace:
                if (transporter.startsWith("troopspace:")) {
                    // Everything after the ':' should be the space's size.
                    double fsize = Double.parseDouble(transporter.substring(11));
                    e.addTransporter(new TroopSpace(fsize), isPod);
                } else if (transporter.startsWith("cargobay:")) {
                    String numbers = transporter.substring(9);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new CargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("liquidcargobay:")) {
                    String numbers = transporter.substring(15);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new LiquidCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("insulatedcargobay:")) {
                    String numbers = transporter.substring(18);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new InsulatedCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("refrigeratedcargobay:")) {
                    String numbers = transporter.substring(21);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new RefrigeratedCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("livestockcargobay:")) {
                    String numbers = transporter.substring(18);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new LivestockCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("asfbay:")) {
                    String numbers = transporter.substring(7);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new ASFBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(), hasARTS), isPod);
                } else if (transporter.startsWith("smallcraftbay:")) {
                    String numbers = transporter.substring(14);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new SmallCraftBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(), hasARTS), isPod);
                } else if (transporter.startsWith("mechbay:")) {
                    String numbers = transporter.substring(8);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new MechBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("lightvehiclebay:")) {
                    String numbers = transporter.substring(16);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new LightVehicleBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("heavyvehiclebay:")) {
                    String numbers = transporter.substring(16);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new HeavyVehicleBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("superheavyvehiclebay:")) {
                    String numbers = transporter.substring(21);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new SuperHeavyVehicleBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("infantrybay:")) {
                    String numbers = transporter.substring(12);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new InfantryBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(), pbi.getPlatoonType()), isPod);
                } else if (transporter.startsWith("battlearmorbay:")) {
                    String numbers = transporter.substring(15);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new BattleArmorBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(),
                            e.isClan(), pbi.isComstarBay()), isPod);
                } else if (transporter.startsWith("bay:")) {
                    String numbers = transporter.substring(4);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new Bay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("protomechbay:")) {
                    String numbers = transporter.substring(13);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new ProtomechBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                } else if (transporter.startsWith("dropshuttlebay:")) {
                    String numbers = transporter.substring("dropshuttlebay:".length());
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new DropshuttleBay(pbi.getDoors(), pbi.getBayNumber(), pbi.getFacing()), isPod);
                } else if (transporter.startsWith("navalrepairpressurized:")) {
                    String numbers = transporter.substring("navalrepairpressurized:".length());
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new NavalRepairFacility(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(),
                            pbi.getFacing(), true, hasARTS), isPod);
                } else if (transporter.startsWith("navalrepairunpressurized:")) {
                    String numbers = transporter.substring("navalrepairunpressurized:".length());
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new NavalRepairFacility(pbi.getSize(), pbi.getDoors(),
                            pbi.getBayNumber(), pbi.getFacing(), false, hasARTS), isPod);
                } else if (transporter.startsWith("reinforcedrepairfacility:")) {
                    String numbers = transporter.substring("reinforcedrepairfacility:".length());
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new ReinforcedRepairFacility(pbi.getSize(), pbi.getDoors(),
                            pbi.getBayNumber(), pbi.getFacing()), isPod);
                } else if (transporter.startsWith("crewquarters:")) {
                    String numbers = transporter.substring(13);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new CrewQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                } else if (transporter.startsWith("steeragequarters:")) {
                    String numbers = transporter.substring(17);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new SteerageQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                } else if (transporter.startsWith("2ndclassquarters:")) {
                    String numbers = transporter.substring(17);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new SecondClassQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                } else if (transporter.startsWith("1stclassquarters:")) {
                    String numbers = transporter.substring(17);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new FirstClassQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                } else if (transporter.startsWith("pillionseats:")) {
                    String numbers = transporter.substring(13);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new PillionSeatCargoBay(pbi.getSize()), isPod);
                } else if (transporter.startsWith("standardseats:")) {
                    String numbers = transporter.substring(14);
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new StandardSeatCargoBay(pbi.getSize()), isPod);
                } else if (transporter.startsWith("ejectionseats:")) {
                    String numbers = transporter.substring("ejectionseats:".length());
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new EjectionSeatCargoBay(pbi.getSize()), isPod);
                } else if (transporter.startsWith("dockingcollar")) {
                    //Add values for collars so they can be parsed and assigned a 'bay' number
                    String numbers = "1.0:0";
                    ParsedBayInfo pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                    e.addTransporter(new DockingCollar(1,pbi.getBayNumber()));
                }

            } // Handle the next transportation component.

        } // End has-transporters
    }
    
    /**
     * Class that holds data relating to transport bays
     * and functionality to parse .blk file transport bay entries
     * @author NickAragua
     *
     */
    public static class ParsedBayInfo {
        private double size;
        private int doors;
        private int bayNumber = -1;
        private PlatoonType platoonType = InfantryBay.PlatoonType.FOOT;
        private boolean isComstarBay;
        private int facing = Entity.LOC_NONE;
        
        public ParsedBayInfo(String numbers, HashSet<Integer> usedBayNumbers) {
            // expected format of "numbers" string:
            // a:b:c:d
            // a is the size of the bay, in tons or # of units and is required
            // b is the number of doors in the bay, and is required
            // c is the bay number OR an indicator that this bay is a comstar bay OR an indicator of the kind of infantry bay it is, and is optional
            // d is like c except that it's not going to be the bay number
            
            String[] temp = numbers.split(Bay.FIELD_SEPARATOR);
            size = Double.parseDouble(temp[0]);
            doors = Integer.parseInt(temp[1]);
            
            // the bay type indicator will be either the third or fourth item, but the bay number always comes before it
            // so we make sure to pick the last item in the array
            String potentialBayTypeIndicator = "";
            boolean bayNumberPresent = false;
            
            if (temp.length == 3) {
                potentialBayTypeIndicator = temp[2];
            } else if (temp.length == 4) {
                potentialBayTypeIndicator = temp[3];
                bayNumberPresent = true; // a 4-length array indicates that the bay number is in the third element
            }
                        
            if (!potentialBayTypeIndicator.isEmpty()) {
                // normally a great time for a switch statement, but we're using equalsignorecase for the comparator
                if (potentialBayTypeIndicator.equalsIgnoreCase(COMSTAR_BAY)) {
                    isComstarBay = true;
                } else if (potentialBayTypeIndicator.equalsIgnoreCase("jump")) {
                    platoonType = InfantryBay.PlatoonType.JUMP;
                } else if (potentialBayTypeIndicator.equalsIgnoreCase("foot")) {
                    platoonType = InfantryBay.PlatoonType.FOOT;
                } else if (potentialBayTypeIndicator.equalsIgnoreCase("motorized")) {
                    platoonType = InfantryBay.PlatoonType.MOTORIZED;
                } else if (potentialBayTypeIndicator.equalsIgnoreCase("mechanized")) {
                    platoonType = InfantryBay.PlatoonType.MECHANIZED;
                } else if (potentialBayTypeIndicator.startsWith(Bay.FACING_PREFIX)) {
                    facing = Integer.parseInt(potentialBayTypeIndicator.replace(Bay.FACING_PREFIX, ""));
                } else {
                    // if we looked at the 
                    bayNumberPresent = temp.length == 3; 
                }
            }
            
            // if we are looking for a bay number
            // and a bay number is present, parse it
            if (usedBayNumbers != null && bayNumberPresent) {
                bayNumber = Integer.parseInt(temp[2]);
            }

            // if a bay number was not specified, assign one
            // if a bay number was specified but is a duplicate, assign a different one
            int newBay = 1;
            if (bayNumber == -1 || usedBayNumbers.contains(bayNumber)) {
                while (usedBayNumbers.contains(newBay)) {
                    newBay++;
                }
                
                bayNumber = newBay;
            }
            
            usedBayNumbers.add(bayNumber);
        }
        
        public double getSize() {
            return size;
        }
        
        public int getDoors() {
            return doors;
        }
        
        public int getBayNumber() {
            return bayNumber;
        }
        
        public PlatoonType getPlatoonType() {
            return platoonType;
        }
        
        public boolean isComstarBay() {
            return isComstarBay;
        }
        
        public int getFacing() {
            return facing;
        }
    }
}
