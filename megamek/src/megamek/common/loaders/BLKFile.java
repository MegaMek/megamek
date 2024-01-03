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

import com.sun.mail.util.DecodingException;
import megamek.common.*;
import megamek.common.InfantryBay.PlatoonType;
import megamek.common.options.IBasicOption;
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
    private static final int TRANSPORTER_FIELDS = 6;

    /** Bitmap fields; 2 of 32 used currently
     *  COMSTAR:                            bit 0
     *  IS/Clan tech (for mixed tech):      bit 1
     *
     *  Note: mutual exclusivity is _not_ enforced here.
     */

    private static final int COMSTAR_BIT = 1;
    // IS = 0; CLAN = 1, mutually exclusive
    private static final int TECH_CLAN_BASE = 1 << 1;

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

    protected void setBasicEntityData(Entity entity) throws EntityLoadingException {
        if (!dataFile.exists("Name")) {
            throw new EntityLoadingException("Could not find name block.");
        }

        entity.setChassis(dataFile.getDataAsString("Name")[0]);

        // Model is not strictly necessary.
        if (dataFile.exists("Model") && (dataFile.getDataAsString("Model")[0] != null)) {
            entity.setModel(dataFile.getDataAsString("Model")[0]);
        } else {
            entity.setModel("");
        }

        if (dataFile.exists(MtfFile.MUL_ID)) {
            entity.setMulId(dataFile.getDataAsInt(MtfFile.MUL_ID)[0]);
        }

        if (dataFile.exists("role")) {
            entity.setUnitRole(UnitRole.parseRole(dataFile.getDataAsString("role")[0]));
        }

        if (dataFile.exists("source")) {
            entity.setSource(dataFile.getDataAsString("source")[0]);
        }

        setTechLevel(entity);
        setFluff(entity);
        checkManualBV(entity);
    }

    protected void loadQuirks(Entity entity) throws EntityLoadingException {
        try {
            List<QuirkEntry> quirks = new ArrayList<>();
            if (dataFile.exists("quirks")) {
                for (String unitQuirk : dataFile.getDataAsVector("quirks")) {
                    QuirkEntry quirkEntry = new QuirkEntry(unitQuirk);
                    quirks.add(quirkEntry);
                }
            }
            if (dataFile.exists("weaponQuirks")) {
                for (String weaponQuirk : dataFile.getDataAsVector("weaponQuirks")) {
                    String[] fields = weaponQuirk.split(":");
                    int slot = Integer.parseInt(fields[2]);
                    QuirkEntry quirkEntry = new QuirkEntry(fields[0], fields[1], slot, fields[3]);
                    quirks.add(quirkEntry);
                }
            }
            entity.loadQuirks(quirks);
        } catch (Exception e) {
            throw new EntityLoadingException("Error loading unit quirks!", e);
        }
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
                            Objects.requireNonNull(mount.getLinked());
                            mount.getLinked().setOriginalShots((int) size
                                * ((InfantryWeapon) mount.getType()).getShots());
                            mount.getLinked().setShotsLeft(mount.getLinked().getOriginalShots());
                        }
                        if (etype.hasFlag(MiscType.F_CARGO)) {
                            // Treat F_CARGO equipment as cargo bays with 1 door, e.g. for ASF with IBB.
                            int idx = t.getTransportBays().size();
                            t.addTransporter(new CargoBay(mount.getSize(), 1, idx), isOmniMounted);
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
        } else if (t instanceof AeroSpaceFighter) {
            blk.writeBlockData("UnitType", "AeroSpaceFighter");
        } else if (t instanceof Aero) {
            blk.writeBlockData("UnitType", "Aero");
        }

        blk.writeBlockData("Name", t.getChassis());
        blk.writeBlockData("Model", t.getModel());
        if (t.hasMulId()) {
            blk.writeBlockData(MtfFile.MUL_ID, t.getMulId());
        }
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

        if (t.hasRole()) {
            blk.writeBlockData("role", t.getRole().toString());
        }

        List<String> quirkList = t.getQuirks().getOptionsList().stream()
                .filter(IOption::booleanValue)
                .map(IBasicOption::getName)
                .collect(Collectors.toList());

        if (!quirkList.isEmpty()) {
            blk.writeBlockData("quirks", String.join("\n", quirkList));
        }

        List<String> weaponQuirkList = new ArrayList<>();
        for (Mounted equipment : t.getEquipment()) {
            for (IOption weaponQuirk : equipment.getQuirks().activeQuirks()) {
                weaponQuirkList.add(weaponQuirk.getName() + ":" + t.getLocationAbbr(equipment.getLocation()) + ":"
                        + t.slotNumber(equipment) + ":" + equipment.getType().getInternalName());
            }
        }
        if (!weaponQuirkList.isEmpty()) {
            blk.writeBlockData("weaponQuirks", String.join("\n", weaponQuirkList));
        }

        if ((t instanceof Infantry) && ((Infantry) t).getMount() != null) {
            blk.writeBlockData("motion_type", ((Infantry) t).getMount().toString());
        } else {
            blk.writeBlockData("motion_type", t.getMovementModeAsString());
        }

        if(t.getTransports().size() > 0) {
            // We should only write the transporters block for units that can and do
            // have transporter bays.  Empty Transporters blocks cause issues.
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
        }

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

        if (!t.getFluff().getCapabilities().isBlank()) {
            blk.writeBlockData("capabilities", t.getFluff().getCapabilities());
        }

        if (!t.getFluff().getOverview().isBlank()) {
            blk.writeBlockData("overview", t.getFluff().getOverview());
        }

        if (!t.getFluff().getDeployment().isBlank()) {
            blk.writeBlockData("deployment", t.getFluff().getDeployment());
        }

        if (!t.getFluff().getHistory().isBlank()) {
            blk.writeBlockData("history", t.getFluff().getHistory());
        }

        if (!t.getFluff().getManufacturer().isBlank()) {
            blk.writeBlockData("manufacturer", t.getFluff().getManufacturer());
        }

        if (!t.getFluff().getPrimaryFactory().isBlank()) {
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

        if (!t.getFluff().getMMLImagePath().isBlank()) {
            blk.writeBlockData("imagepath", t.getFluff().getMMLImagePath());
        }

        if (!t.getFluff().getNotes().isBlank()) {
            blk.writeBlockData("notes", t.getFluff().getNotes());
        }

        if (!t.getFluff().getUse().isBlank()) {
            blk.writeBlockData("use", t.getFluff().getUse());
        }

        if (!t.getFluff().getLength().isBlank()) {
            blk.writeBlockData("length", t.getFluff().getLength());
        }

        if (!t.getFluff().getWidth().isBlank()) {
            blk.writeBlockData("width", t.getFluff().getWidth());
        }

        if (!t.getFluff().getHeight().isBlank()) {
            blk.writeBlockData("height", t.getFluff().getHeight());
        }

        if (!t.getSource().isBlank()) {
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
            blk.writeBlockData("squadn", infantry.getSquadCount());
            if (infantry.getSecondaryWeaponsPerSquad() > 0) {
                blk.writeBlockData("secondn", infantry.getSecondaryWeaponsPerSquad());
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
            if (!augmentations.isEmpty()) {
                blk.writeBlockData("augmentation", augmentations.toArray(new String[0]));
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
                && ((SpaceStation) t).isModularOrKFAdapter()) {
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
            blk.writeBlockData("designtype", js.getDesignType());
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

    protected void addTransports(Entity e) throws EntityLoadingException {
        if (dataFile.containsData("transporters")) {
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

                String[] transporterParts = transporter.split(Bay.FIELD_SEPARATOR, 2);
                String startsWith = transporterParts[0];
                String numbers = transporterParts.length > 1 ? transporterParts[1] : "";
                ParsedBayInfo pbi = null;

                // TroopSpace:
                try {
                    switch (startsWith) {
                        case "troopspace":
                            // Everything after the ':' should be the space's size.
                            double fsize = Double.parseDouble(numbers);
                            e.addTransporter(new TroopSpace(fsize), isPod);
                            break;
                        case "cargobay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new CargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "liquidcargobay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new LiquidCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "insulatedcargobay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new InsulatedCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "refrigeratedcargobay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new RefrigeratedCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "livestockcargobay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new LivestockCargoBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "asfbay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new ASFBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(), hasARTS), isPod);
                            break;
                        case "smallcraftbay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new SmallCraftBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(), hasARTS), isPod);
                            break;
                        case "mechbay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new MechBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "lightvehiclebay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new LightVehicleBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "heavyvehiclebay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new HeavyVehicleBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "superheavyvehiclebay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new SuperHeavyVehicleBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "infantrybay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new InfantryBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(), pbi.getPlatoonType()), isPod);
                            break;
                        case "battlearmorbay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers, e.isClan());
                            e.addTransporter(new BattleArmorBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(),
                                    pbi.isClan(), pbi.isComstarBay()), isPod);
                            break;
                        case "bay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new Bay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "protomekbay":
                            // Newer custom BLK handling
                        case "protomechbay":
                            // Backward compatibility
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new ProtomechBay(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber()), isPod);
                            break;
                        case "dropshuttlebay":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new DropshuttleBay(pbi.getDoors(), pbi.getBayNumber(), pbi.getFacing()), isPod);
                            break;
                        case "navalrepairpressurized":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new NavalRepairFacility(pbi.getSize(), pbi.getDoors(), pbi.getBayNumber(),
                                    pbi.getFacing(), true, hasARTS), isPod);
                            break;
                        case "navalrepairunpressurized":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new NavalRepairFacility(pbi.getSize(), pbi.getDoors(),
                                    pbi.getBayNumber(), pbi.getFacing(), false, hasARTS), isPod);
                            break;
                        case "reinforcedrepairfacility":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new ReinforcedRepairFacility(pbi.getSize(), pbi.getDoors(),
                                    pbi.getBayNumber(), pbi.getFacing()), isPod);
                            break;
                        case "crewquarters":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new CrewQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                            break;
                        case "steeragequarters":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new SteerageQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                            break;
                        case "2ndclassquarters":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new SecondClassQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                            break;
                        case "1stclassquarters":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new FirstClassQuartersCargoBay(pbi.getSize(), pbi.getDoors()), isPod);
                            break;
                        case "pillionseats":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new PillionSeatCargoBay(pbi.getSize()), isPod);
                            break;
                        case "standardseats":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new StandardSeatCargoBay(pbi.getSize()), isPod);
                            break;
                        case "ejectionseats":
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new EjectionSeatCargoBay(pbi.getSize()), isPod);
                            break;
                        case "dockingcollar":
                            //Add values for collars so they can be parsed and assigned a 'bay' number
                            numbers = "1.0:0";
                            pbi = new ParsedBayInfo(numbers, usedBayNumbers);
                            e.addTransporter(new DockingCollar(pbi.getBayNumber()));
                            break;
                        default:
                            // Some Transport types (e.g. BattleArmorHandles) are not added here.
                            // Do nothing for now.
                            break;
                    } // End switch-case
                }
                catch(DecodingException|NumberFormatException x){
                    throw new EntityLoadingException(
                        String.format(
                                "Error decoding transporter '%s' (was '%s')", transporter, x
                        )
                    );
                }

            } // Handle the next transportation component.

        } // End has-transporters.
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
        private boolean isClanBay;
        private int facing = Entity.LOC_NONE;
        private int tech_base = 0;

        public ParsedBayInfo(String numbers, HashSet<Integer> usedBayNumbers) throws DecodingException {
            // Overloaded constructor that assumes IS tech base
            this(numbers, usedBayNumbers, false);
        }

        public ParsedBayInfo(String numbers, HashSet<Integer> usedBayNumbers, boolean clanTechBase) throws DecodingException {
            // expected format of "numbers" string:
            // 0:1:2:3:4:5
            // Field 0 is the size of the bay, in tons or # of units and is required
            // Field 1 is the number of doors in the bay, and is required
            // Field 2 is the bay number and is required; default value of "-1" is "unset"
            // Field 3 is used to record infantry platoon type; default is an empty string
            // Field 4 is an int recording facing; default is string representation of entity.LOC_NONE
            // Field 5 is a bitmap recording status like tech type, ComStar bay; default is "0"
            // To facilitate loading older .blk files, we first convert them to current format

            String[] fields = {};
            try {
                // Turn 2-, 3-, or 4-field number lines into standardized 6-field line.
                fields = normalizeTransporterNumbers(numbers, clanTechBase);

                size = Double.parseDouble(fields[0]);
                doors = Integer.parseInt(fields[1]);
                bayNumber = Integer.parseInt(fields[2]);
                platoonType = decodePlatoonType(fields[3]);
                facing = Integer.parseInt(fields[4]);

                // Convert and unpack bitmap
                int bitmap = Integer.parseInt(fields[5]);

                isComstarBay = (COMSTAR_BIT & bitmap) > 0;
                isClanBay = (TECH_CLAN_BASE & bitmap) > 0;

            }
            catch(DecodingException|NumberFormatException e){
                throw new DecodingException(
                        String.format(
                                "Failure to load '%s' (was '%s'", numbers, e.toString()
                        )
                );
            }

            // if a positive bay number was not specified, assign one
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

        public boolean isClan() {
            return isClanBay;
        }

        public int getFacing() {
            return facing;
        }

        public static String[] normalizeTransporterNumbers(String numbers) throws DecodingException {
            // If we don't care about the tech base (e.g., non BA bays) use default value
            return normalizeTransporterNumbers(numbers, false);
        }
        public static String[] normalizeTransporterNumbers(String numbers, boolean clanTechBase) throws DecodingException {
            /** In order to make all transporter bays use the same number of data fields,
             *  but maintain compatibility with older blk files, we will do some
             *  pre-processing to check what format of field we are looking at, and convert it
             *  to the new format.
             */
            String[] numbersArray = numbers.split(Bay.FIELD_SEPARATOR);

            if (numbersArray.length == TRANSPORTER_FIELDS){
                // Already in expected format
                return numbersArray;
            }
            else if (numbersArray.length > TRANSPORTER_FIELDS){
                throw new DecodingException(String.format("Cannot decode numbers string '%s'", numbers));
            }

            // Expand old-format to new-format size; initialize new field.
            String[] temp = new String[TRANSPORTER_FIELDS];
            // Copy initial two fields; subsequent fields get defaults or are set later
            System.arraycopy(numbersArray,0,temp,0,2);
            // Fill in other fields with default/unset values
            temp[2] = String.valueOf(-1);
            temp[3] = "";
            temp[4] = String.valueOf(Entity.LOC_NONE);
            temp[5] = String.valueOf(0);

            // If 2-field format, return with default values set.
            if (numbersArray.length == 2){
                return temp;
            }
            else if (numbersArray.length > 2){
                // Attempt to parse index 2 as an integer bay number, otherwise leave it as default
                try{
                    temp[2] = String.valueOf(Integer.parseInt(numbersArray[2]));
                } catch (NumberFormatException e){
                    // pass
                }
            }

            // Add bitmap field
            int bitmap = 0;
            if (clanTechBase){
               bitmap |= TECH_CLAN_BASE;
            }

            // the bay type indicator will be either the third or fourth item, but the bay number always comes before it,
            // so we make sure to pick the last item in the array
            String potentialBayTypeIndicator = "";
            if (numbersArray.length == 3) {
                potentialBayTypeIndicator = numbersArray[2];
            } else if (numbersArray.length == 4) {
                potentialBayTypeIndicator = numbersArray[3];
                temp[2] = numbersArray[2];
            }

            if (!potentialBayTypeIndicator.isEmpty()) {
                if (potentialBayTypeIndicator.equalsIgnoreCase(COMSTAR_BAY)) {
                    bitmap |= COMSTAR_BIT;
                }
                else if (
                        Set.of(
                            new String [] {"jump", "foot", "motorized", "mechanized"}
                        ).contains(potentialBayTypeIndicator.toLowerCase())){
                    // Found an infantry type in the last field (2 or 3)
                    // Assign to field 3
                    temp[3] = potentialBayTypeIndicator;
                    if (temp[2].equals(temp[3])){
                        // We found the infantry type in the bay number field; unset bay number
                        temp[2] = String.valueOf(-1);
                    }
                } else if (potentialBayTypeIndicator.startsWith(Bay.FACING_PREFIX)) {
                    // Strip old facing prefix, set field to remaining value.
                    // It's difficult to standardize this
                    temp[4] = potentialBayTypeIndicator.replace(Bay.FACING_PREFIX, "");
                }
            }

            // save bitmap to normalize numbers
            temp[5] = String.format("%s",bitmap);
            return temp;
        }

        public static PlatoonType decodePlatoonType(String typeString) throws DecodingException {
            // Handle platoon type decoding from strings of various casing

            if (typeString.equalsIgnoreCase("jump")) {
                return PlatoonType.JUMP;
            } else if (typeString.equalsIgnoreCase("foot")) {
                return PlatoonType.FOOT;
            } else if (typeString.equalsIgnoreCase("motorized")) {
                return PlatoonType.MOTORIZED;
            } else if (typeString.equalsIgnoreCase("mechanized")) {
                return PlatoonType.MECHANIZED;
            } else if (typeString.isEmpty()) {
                return PlatoonType.FOOT;
            } else {
                throw new DecodingException(String.format("Cannot determine platoon type from '%s'", typeString));
            }
        }
    }
}
