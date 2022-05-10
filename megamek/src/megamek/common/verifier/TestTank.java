/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.verifier;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.util.StringUtil;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;
import megamek.common.weapons.lasers.CLChemicalLaserWeapon;

import java.util.*;

/**
 * @author Reinhard Vicinus
 */
public class TestTank extends TestEntity {

    /**
     * Defines the maximum amount of armor a VTOL can mount on its rotor.
     */
    public static int VTOL_MAX_ROTOR_ARMOR = 2;

    private final Tank tank;

    public TestTank(Tank tank, TestEntityOption options, String fileString) {
        super(options, tank.getEngine(), getArmor(tank), getStructure(tank));
        this.tank = tank;
        this.fileString = fileString;
    }

    protected static Structure getStructure(Tank tank) {
        if (tank.isSupportVehicle()) {
            return new SupportVeeStructure(tank);
        }
        int type = EquipmentType.T_STRUCTURE_STANDARD;
        if (tank.getStructureType() == 1) {
            type = EquipmentType.T_STRUCTURE_ENDO_STEEL;
        }
        return new Structure(type, tank.isSuperHeavy(), tank.getMovementMode());
    }

    private static Armor[] getArmor(Tank tank) {
        Armor[] armor;
        if (!tank.hasPatchworkArmor()) {
            armor = new Armor[1];
            int type = tank.getArmorType(1);
            int flag = 0;
            if (tank.isClanArmor(1)) {
                flag |= Armor.CLAN_ARMOR;
            }
            armor[0] = new Armor(type, flag);
            return armor;
        } else {
            armor = new Armor[tank.locations()];
            for (int i = 0; i < tank.locations(); i++) {
                int type = tank.getArmorType(1);
                int flag = 0;
                if (tank.isClanArmor(1)) {
                    flag |= Armor.CLAN_ARMOR;
                }
                armor[i] = new Armor(type, flag);
            }
        }
        return armor;
    }

    /**
     * Filters all vehicle armor according to given tech constraints
     *
     * @param movementMode The vehicle movement mode
     * @param techManager  The tech constraints
     * @return             The armors legal for the unit
     */
    public static List<EquipmentType> legalArmorsFor(EntityMovementMode movementMode, ITechManager techManager) {
        List<EquipmentType> retVal = new ArrayList<>();
        for (int at = 0; at < EquipmentType.armorNames.length; at++) {
            if ((at == EquipmentType.T_ARMOR_PATCHWORK)
                    || ((at == EquipmentType.T_ARMOR_HARDENED)
                            && ((movementMode == EntityMovementMode.VTOL)
                            || (movementMode == EntityMovementMode.HOVER)
                            || (movementMode == EntityMovementMode.WIGE)))) {
                continue;
            }
            String name = EquipmentType.getArmorTypeName(at, techManager.useClanTechBase());
            EquipmentType eq = EquipmentType.get(name);
            if ((null != eq)
                    && eq.hasFlag(MiscType.F_TANK_EQUIPMENT)
                    && techManager.isLegal(eq)) {
                retVal.add(eq);
            }
            if (techManager.useMixedTech()) {
                name = EquipmentType.getArmorTypeName(at, !techManager.useClanTechBase());
                EquipmentType eq2 = EquipmentType.get(name);
                if ((null != eq2) && (eq != eq2)
                        && eq2.hasFlag(MiscType.F_TANK_EQUIPMENT)
                        && techManager.isLegal(eq2)) {
                    retVal.add(eq2);
                }
            }
        }
        return retVal;
    }

    /**
     * Maximum construction weight by vehicle type
     *
     * @param mode        The vehicle's movement mode
     * @param superheavy  Whether the vehicle is superheavy
     * @return            The maximum construction tonnage
     */
    public static double maxTonnage(EntityMovementMode mode, boolean superheavy) {
        switch (mode) {
            case WHEELED:
            case WIGE:
                return superheavy ? 160.0 : 80.0;
            case HOVER:
                return superheavy ? 100.0 : 50.0;
            case VTOL:
                return superheavy ? 60.0 : 30.0;
            case NAVAL:
            case SUBMARINE:
                return superheavy ? 555.0 : 300.0;
            case HYDROFOIL:
                return 100.0; // not eligible for superheavy
            case TRACKED:
            default:
                return superheavy ? 200.0 : 100.0;
        }
    }

    @Override
    public Entity getEntity() {
        return tank;
    }

    @Override
    public boolean isTank() {
        return !(tank instanceof GunEmplacement);
    }

    @Override
    public boolean isMech() {
        return false;
    }

    @Override
    public boolean isAero() {
        return false;
    }

    @Override
    public boolean isSmallCraft() {
        return false;
    }
    
    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }

    @Override
    public boolean isProtomech() {
        return false;
    }

    public double getTankWeightTurret() {
        double weight = 0;

        // For omni vees, the base chassis sets a turret weight
        if (tank.isOmni() && tank.getBaseChassisTurretWeight() >= 0) {
            weight = tank.getBaseChassisTurretWeight();
        } else {
            // For non-omnis, count up the weight of eq in the turret
            for (Mounted m : tank.getEquipment()) {
                if ((m.getLocation() == tank.getLocTurret())
                        && !(m.getType() instanceof AmmoType)
                        // Skip any patchwork armor mounts
                        && (EquipmentType.getArmorType(m.getType()) == EquipmentType.T_ARMOR_UNKNOWN)) {
                    weight += m.getTonnage();
                }
            }
            // Turrets weight 10% of the weight of weapons in them
            weight = weight / 10.0f;
        }

        if (tank.isSupportVehicle()) {
            if (getEntity().getWeight() < 5) {
                return TestEntity.ceil(weight, Ceil.KILO);
            } else {
                return TestEntity.ceil(weight, Ceil.HALFTON);
            }
        } else {
            return TestEntity.ceilMaxHalf(weight, getWeightCeilingTurret());
        }
    }

    public double getTankWeightDualTurret() {
        double weight = 0;

        // For omni vees, the base chassis sets a turret weight
        if (tank.isOmni() && tank.getBaseChassisTurret2Weight() >= 0) {
            weight = tank.getBaseChassisTurret2Weight();
        } else {
            // For non-omnis, count up the weight of eq in the turret
            for (Mounted m : tank.getEquipment()) {
                if ((m.getLocation() == tank.getLocTurret2())
                        && !(m.getType() instanceof AmmoType)) {
                    weight += m.getTonnage();
                }
            }
            // Turrets weight 10% of the weight of weapons in them
            weight = weight / 10.0f;
        }
        return TestEntity.ceilMaxHalf(weight, getWeightCeilingTurret());
    }

    public double getTankWeightLifting() {
        switch (tank.getMovementMode()) {
            case HOVER:
            case VTOL:
            case HYDROFOIL:
            case SUBMARINE:
            case WIGE:
                return TestEntity.ceilMaxHalf(tank.getWeight() / 10.0f,
                        getWeightCeilingLifting());
            default:
                return 0f;
        }
    }

    @Override
    public double getWeightMisc() {
        return getTankWeightTurret() + getTankWeightDualTurret() + getTankWeightLifting();
    }

    @Override
    public double getWeightControls() {
        if (tank.hasNoControlSystems()) {
            return 0;
        } else {
            return TestEntity.ceilMaxHalf(tank.getWeight() / 20.0f,
                    getWeightCeilingControls());
        }
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        // tanks can't have DHS
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return heatNeutralHSRequirement();
    }

    @Override
    public double getWeightHeatSinks() {
        int heat = getCountHeatSinks();
        heat -= engine.getWeightFreeEngineHeatSinks();
        if (heat < 0) {
            heat = 0;
        }
        return heat;
    }

    @Override
    public String printWeightMisc() {
        String turretString = !tank.hasNoTurret() ? StringUtil.makeLength("Turret:",
                getPrintSize() - 5)
                + TestEntity.makeWeightString(getTankWeightTurret()) + "\n" : "";
        String dualTurretString = !tank.hasNoDualTurret() ? StringUtil.makeLength("Front Turret:",
                getPrintSize() - 5)
                + TestEntity.makeWeightString(getTankWeightDualTurret()) + "\n" : "";
        return turretString + dualTurretString
                + (getTankWeightLifting() != 0 ? StringUtil.makeLength(
                        "Lifting Equip:", getPrintSize() - 5)
                        + TestEntity.makeWeightString(getTankWeightLifting()) + "\n" : "")
                + (getWeightPowerAmp() != 0 ? StringUtil.makeLength(
                        "Power Amp:", getPrintSize() - 5)
                        + TestEntity.makeWeightString(getWeightPowerAmp()) + "\n" : "");
    }

    @Override
    public String printWeightControls() {
        return StringUtil.makeLength("Controls:", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightControls()) + "\n";
    }

    @Override
    public double getWeightCarryingSpace() {
        return super.getWeightCarryingSpace() + tank.getExtraCrewSeats() * 0.5;
    }

    @Override
    public String printWeightCarryingSpace() {
        if (tank.getExtraCrewSeats() > 0) {
            return super.printWeightCarryingSpace()
                    + StringUtil.makeLength("Combat Seats:", getPrintSize() - 5)
                    + TestEntity.makeWeightString(tank.getExtraCrewSeats() * 0.5) + "\n";
        } else {
            return super.printWeightCarryingSpace();
        }
    }

    public Tank getTank() {
        return tank;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, getEntity().getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;
        if (skip()) {
            return true;
        }
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation()).append("\n");
            correct = false;
        }
        if (!engine.engineValid) {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if (tank.getFreeSlots() < 0) {
            buff.append("Not enough item slots available! Using ");
            buff.append(Math.abs(tank.getFreeSlots()));
            buff.append(" slot(s) too many.\n");
            buff.append(printSlotCalculation()).append("\n");
            correct = false;
        }
        int armorLimit = (int) (((tank.getWeight() * 7) / 2) + 40);
        if (tank.getTotalOArmor() > armorLimit) {
            buff.append("Armor exceeds point limit for ");
            buff.append(tank.getWeight());
            buff.append("-ton vehicle: ");
            buff.append(tank.getTotalOArmor());
            buff.append(" points > ");
            buff.append(armorLimit);
            buff.append(".\n\n");
            correct = false;
        }
        if (tank instanceof VTOL) {
            if (!tank.hasWorkingMisc(MiscType.F_MAST_MOUNT)) {
                for (Mounted m : tank.getEquipment()) {
                    // Units with patchwork armor place any armor slots in the same location
                    // as the armor. This is for accounting convenience.
                    if ((m.getLocation() == VTOL.LOC_ROTOR)
                            && EquipmentType.getArmorType(m.getType()) == EquipmentType.T_ARMOR_UNKNOWN) {
                        buff.append("rotor equipment must be placed in mast mount");
                        correct = false;
                    }
                }
            }
            if (tank.getOArmor(VTOL.LOC_ROTOR) > VTOL_MAX_ROTOR_ARMOR) {
                buff.append(tank.getOArmor(VTOL.LOC_ROTOR));
                buff.append(" points of VTOL rotor armor exceed ")
                        .append(VTOL_MAX_ROTOR_ARMOR).append("-point limit.\n\n");
                correct = false;
            }
        }
        for (Mounted m : tank.getEquipment()) {
            if (!legalForMotiveType(m.getType(), tank.getMovementMode(), false)) {
                buff.append(m.getType().getName()).append(" is incompatible with ")
                        .append(tank.getMovementModeAsString());
                correct = false;
            }
        }
        for (Mounted m : tank.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_COMBAT_VEHICLE_ESCAPE_POD)) {
                if (m.getLocation() != (tank instanceof SuperHeavyTank?SuperHeavyTank.LOC_REAR:Tank.LOC_REAR)) {
                    buff.append("combat vehicle escape pod must be placed in rear");
                    correct = false;
                }
            } else if (m.getType().hasFlag(MiscType.F_MASC) && m.getType().hasSubType(MiscType.S_SUPERCHARGER)
                    && (tank instanceof VTOL)) {
                buff.append("VTOLS cannot mount superchargers.");
                correct = false;
            }
        }
        for (int loc = 0; loc < tank.locations(); loc++) {
            int count = 0;
            for (Mounted misc : tank.getMisc()) {
                if ((misc.getLocation() == loc) && misc.getType().hasFlag(MiscType.F_MANIPULATOR)) {
                    count++;
                }
            }
            if (count > 2) {
                buff.append("max of 2 manipulators per location");
                correct = false;
                break;
            }
        }

        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (hasIllegalTechLevels(buff, ammoTechLvl)) {
            correct = false;
        }
        if (showIncorrectIntroYear() && hasIncorrectIntroYear(buff)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }
        if (!correctCriticals(buff)) {
            correct = false;
        }
        return correct;
    }

    /**
     * Checks whether the equipment is compatible with the vehicle's motive type
     *
     * @param eq            The equipment to check
     * @param mode          The vehicle's motive type
     * @param supporVehicle Whether the vehicle is a support vehicle.
     * @return              Whether the equipment and motive type are compatible
     */
    public static boolean legalForMotiveType(EquipmentType eq, EntityMovementMode mode, boolean supporVehicle) {
        // A couple broad categories for convenience
        final boolean isNaval = mode.equals(EntityMovementMode.NAVAL)
                || mode.equals(EntityMovementMode.HYDROFOIL)
                || mode.equals(EntityMovementMode.SUBMARINE);
        final boolean isAero = mode.equals(EntityMovementMode.AERODYNE)
                || mode.equals(EntityMovementMode.AIRSHIP)
                || mode.equals(EntityMovementMode.STATION_KEEPING);
        if (eq instanceof MiscType) {
            if (eq.hasFlag(MiscType.F_FLOTATION_HULL)) {
                // Per errata, WiGE vehicles automatically include flotation hull
                return mode.equals(EntityMovementMode.HOVER) || mode.equals(EntityMovementMode.VTOL);
            }
            if (eq.hasFlag(MiscType.F_FULLY_AMPHIBIOUS)
                    || eq.hasFlag(MiscType.F_LIMITED_AMPHIBIOUS)
                    || eq.hasFlag(MiscType.F_BULLDOZER)
                    || (eq.hasFlag(MiscType.F_CLUB) && eq.hasSubType(MiscType.S_COMBINE))) {
                return mode.equals(EntityMovementMode.WHEELED) || mode.equals(EntityMovementMode.TRACKED);
            }
            if (eq.hasFlag(MiscType.F_DUNE_BUGGY)) {
                return mode.equals(EntityMovementMode.WHEELED);
            }
            // Submarines have environmental sealing as part of their base construction
            if (eq.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                return !mode.equals(EntityMovementMode.SUBMARINE);
            }
            if (eq.hasFlag(MiscType.F_JUMP_JET)
                    || eq.hasFlag(MiscType.F_VEEDC)
                    || (eq.hasFlag(MiscType.F_CLUB)
                    && eq.hasSubType(MiscType.S_CHAINSAW | MiscType.S_DUAL_SAW | MiscType.S_MINING_DRILL))) {
                return mode.equals(EntityMovementMode.WHEELED) || mode.equals(EntityMovementMode.TRACKED)
                        || mode.equals(EntityMovementMode.HOVER) || mode.equals(EntityMovementMode.WIGE);
            }
            if (eq.hasFlag(MiscType.F_MINESWEEPER) || eq.hasFlag(MiscType.F_CLUB)
                    && eq.hasSubType(MiscType.S_PILE_DRIVER)) {
                return mode.equals(EntityMovementMode.WHEELED) || mode.equals(EntityMovementMode.TRACKED)
                        || isNaval;
            }
            if (eq.hasFlag(MiscType.F_HITCH)) {
                return mode.equals(EntityMovementMode.WHEELED) || mode.equals(EntityMovementMode.TRACKED)
                        || mode.equals(EntityMovementMode.RAIL) || mode.equals(EntityMovementMode.MAGLEV);
            }
            if (eq.hasFlag(MiscType.F_LIFEBOAT)) {
                if (eq.hasSubType(MiscType.S_MARITIME_ESCAPE_POD | MiscType.S_MARITIME_LIFEBOAT)) {
                    // Allowed for all naval units and support vehicles with an amphibious chassis mod
                    return supporVehicle ? !mode.equals(EntityMovementMode.HOVER) : isNaval;
                } else {
                    return isAero;
                }
            }
            if (eq.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                    || eq.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || eq.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || (eq.hasFlag(MiscType.F_MASC) && eq.hasSubType(MiscType.S_SUPERCHARGER))
                    || (eq.hasFlag(MiscType.F_CLUB)
                    && eq.hasSubType(MiscType.S_BACKHOE | MiscType.S_ROCK_CUTTER
                    | MiscType.S_SPOT_WELDER | MiscType.S_WRECKING_BALL))) {
                return !mode.equals(EntityMovementMode.VTOL) && !isAero;
            }
            if (eq.hasFlag(MiscType.F_AP_POD)) {
                return !isNaval && !isAero;
            }
            if (eq.hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                return !isAero
                        && !mode.equals(EntityMovementMode.VTOL)
                        && !mode.equals(EntityMovementMode.RAIL)
                        && !mode.equals(EntityMovementMode.MAGLEV);
            }
            if (eq.hasFlag(MiscType.F_MASH)) {
                return !mode.equals(EntityMovementMode.VTOL);
            }
            if (eq.hasFlag(MiscType.F_SPONSON_TURRET)
                    || eq.hasFlag(MiscType.F_LADDER)) {
                return !isAero;
            }
            if (eq.hasFlag(MiscType.F_PINTLE_TURRET)) {
                return !isNaval && !mode.equals(EntityMovementMode.AERODYNE)
                        && !mode.equals(EntityMovementMode.STATION_KEEPING);
            }
            if (eq.hasFlag(MiscType.F_LOOKDOWN_RADAR)
                    || eq.hasFlag(MiscType.F_INFRARED_IMAGER)
                    || eq.hasFlag(MiscType.F_HIRES_IMAGER)) {
                return isAero || mode.equals(EntityMovementMode.VTOL);
            }
            if (eq.hasFlag(MiscType.F_REFUELING_DROGUE)) {
                return mode.equals(EntityMovementMode.VTOL)
                        || mode.equals(EntityMovementMode.AERODYNE)
                        || mode.equals(EntityMovementMode.AIRSHIP);
            }
            if (eq.hasFlag(MiscType.F_SASRCS)
                    || eq.hasFlag(MiscType.F_LIGHT_SAIL)
                    || eq.hasFlag(MiscType.F_SPACE_MINE_DISPENSER)
                    || eq.hasFlag(MiscType.F_SMALL_COMM_SCANNER_SUITE)) {
                return mode.equals(EntityMovementMode.STATION_KEEPING);
            }
            if (eq.hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                return !mode.equals(EntityMovementMode.STATION_KEEPING);
            }
            if (eq.hasFlag(MiscType.F_EXTERNAL_STORES_HARDPOINT)) {
                return mode.equals(EntityMovementMode.AERODYNE);
            }
        } else if (eq instanceof WeaponType) {
            if (((WeaponType) eq).getAmmoType() == AmmoType.T_BPOD) {
                return !isNaval;
            }
            if (((WeaponType) eq).getAmmoType() == AmmoType.T_NAIL_RIVET_GUN) {
                return !mode.equals(EntityMovementMode.VTOL);
            }
        }
        return true;
    }

    @Override
    public boolean correctWeight(StringBuffer buff, boolean showO, boolean showU) {
        boolean correct = super.correctWeight(buff, showO, showU);
        double max = maxTonnage(getEntity().getMovementMode(), getEntity().isSuperHeavy());
        if (getEntity().getWeight() > max) {
            correct = false;
            buff.append("Exceeds maximum tonnage of ").append(max).append(" for ")
                    .append(getEntity().getMovementModeAsString())
                    .append(" combat vehicle.\n");
        }
        return correct;
    }

    public boolean correctCriticals(StringBuffer buff) {
        List<Mounted> unallocated = new ArrayList<>();
        boolean correct = true;

        for (Mounted mount : tank.getMisc()) {
            if (mount.getLocation() == Entity.LOC_NONE && !(mount.getCriticals() == 0)) {
                unallocated.add(mount);
            }
        }
        for (Mounted mount : tank.getWeaponList()) {
            if (mount.getLocation() == Entity.LOC_NONE) {
                unallocated.add(mount);
            }
        }
        for (Mounted mount : tank.getAmmo()) {
            int ammoType = ((AmmoType) mount.getType()).getAmmoType();
            if ((mount.getLocation() == Entity.LOC_NONE) &&
                    (mount.getUsableShotsLeft() > 1
                            || ammoType == AmmoType.T_CRUISE_MISSILE )) {
                unallocated.add(mount);
            }
        }

        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");
            for (Mounted mount : unallocated) {
                buff.append(mount.getType().getInternalName()).append("\n");
            }
            correct = false;
        }

        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Tank: ").append(tank.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(tank.getYear()).append("\n");
        buff.append(printSource());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (").append(
                    calculateWeight()).append(")\n");
        }

        buff.append(printWeightCalculation()).append("\n");
        printFailedEquipment(buff);
        return buff;
    }
    
    public StringBuffer printSlotCalculation() {
        StringBuffer buff = new StringBuffer();
        buff.append("Available slots: ").append(tank.getTotalSlots()).append("\n");
        // different engines take different amounts of slots
        int engineSlots = 0;
        if (tank.hasEngine() && tank.getEngine().isFusion()) {
            if (tank.getEngine().getEngineType() == Engine.LIGHT_ENGINE) {
                engineSlots = 1;
            } else if (tank.getEngine().getEngineType() == Engine.XL_ENGINE) {
                engineSlots = tank.getEngine().hasFlag(Engine.CLAN_ENGINE) ? 1 : 2;
            } else if (tank.getEngine().getEngineType() == Engine.XXL_ENGINE) {
                engineSlots = tank.getEngine().hasFlag(Engine.CLAN_ENGINE) ? 2 : 4;
            } else if (tank.getEngine().getEngineType() == Engine.COMPACT_ENGINE) {
                engineSlots--;
            }
            
            if (tank.getEngine().getEngineType() == Engine.LARGE_ENGINE) {
                engineSlots++;
            }
        }
        if (engineSlots != 0) {
            buff.append(StringUtil.makeLength(tank.getEngine().getEngineName(), 30));
            buff.append(engineSlots).append("\n");
        }

        // JJs take just 1 slot
        if (tank.getJumpMP(false) > 0) {
            buff.append(StringUtil.makeLength("Jump Jets", 30)).append("1\n");
        }

        boolean addedCargo = false;
        for (Mounted mount : tank.getEquipment()) {
            if ((mount.getType() instanceof MiscType)
                    && mount.getType().hasFlag(MiscType.F_CARGO)) {
                if (!addedCargo) {
                    buff.append(StringUtil.makeLength(mount.getName(), 30));
                    buff.append(mount.getType().getTankSlots(tank)).append("\n");
                    addedCargo = true;
                    continue;
                } else {
                    continue;
                }
            }
            if (!((mount.getType() instanceof AmmoType) || Arrays.asList(
                    EquipmentType.armorNames).contains(
                    mount.getType().getName()))) {
                buff.append(StringUtil.makeLength(mount.getName(), 30));
                buff.append(mount.getType().getTankSlots(tank)).append("\n");
            }
        }
        if (tank.getExtraCrewSeats() > 0) {
            buff.append(StringUtil.makeLength("Combat Crew Seats:", 30));
            buff.append(tank.getExtraCrewSeats()).append("\n");
        }
        // different armor types take different amount of slots
        int armorSlots = 0;
        if (!tank.hasPatchworkArmor()) {
            int type = tank.getArmorType(1);
            switch (type) {
                case EquipmentType.T_ARMOR_FERRO_FIBROUS:
                    if (TechConstants.isClan(tank.getArmorTechLevel(1))) {
                        armorSlots++;
                    } else {
                        armorSlots += 2;
                    }
                    break;
                case EquipmentType.T_ARMOR_HEAVY_FERRO:
                    armorSlots += 3;
                    break;
                case EquipmentType.T_ARMOR_LIGHT_FERRO:
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_HARDENED:
                    armorSlots++;
                    break;
                case EquipmentType.T_ARMOR_STEALTH:
                    armorSlots += 2;
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                    if (TechConstants.isClan(tank.getArmorTechLevel(1))) {
                        armorSlots++;
                    } else {
                        armorSlots += 2;
                    }
                    break;
                default:
                    break;
            }
            if (armorSlots != 0) {
                buff.append(StringUtil.makeLength(EquipmentType.getArmorTypeName(type,
                        TechConstants.isClan(tank.getArmorTechLevel(1))), 30));
                buff.append(armorSlots).append("\n");
            }
        }

        // for ammo, each type of ammo takes one slots, regardless of
        // submunition type
        Map<String, Boolean> foundAmmo = new HashMap<>();
        for (Mounted ammo : tank.getAmmo()) {
            // don't count oneshot ammo
            if ((ammo.getLocation() == Entity.LOC_NONE)
                    && (ammo.getBaseShotsLeft() == 1)) {
                continue;
            }
            AmmoType at = (AmmoType) ammo.getType();
            if (foundAmmo.get(at.getAmmoType() + ":" + at.getRackSize()) == null) {
                buff.append(StringUtil.makeLength(at.getName(), 30));
                buff.append("1\n");
                foundAmmo.put(at.getAmmoType() + ":" + at.getRackSize(), true);
            }
        }
        // if a tank has an infantry bay, add 1 slots (multiple bays take 1 slot
        // total)
        boolean infantryBayCounted = false;
        for (Transporter transport : tank.getTransports()) {
            if (transport instanceof TroopSpace) {
                buff.append(StringUtil.makeLength("Troop Space", 30));
                buff.append("1\n");
                infantryBayCounted = true;
                break;
            }
        }
        // unit transport bays take 1 slot each
        for (Bay bay : tank.getTransportBays()) {
            if (((bay instanceof BattleArmorBay) || (bay instanceof InfantryBay))
                    && !infantryBayCounted) {
                buff.append(StringUtil.makeLength("Infantry Bay", 30));
                buff.append("1").append("\n");
                infantryBayCounted = true;
            } else {
                buff.append(StringUtil.makeLength("Transport Bay", 30));
                buff.append("1").append("\n");
            }
        }        
        return buff;
    }

    @Override
    public String getName() {
        return "Tank: " + tank.getDisplayName();
    }

    @Override
    public double getWeightPowerAmp() {
        if (getEntity().isSupportVehicle() && (getEntity().getWeight() < 5)) {
            return 0;
        }

        if (!engine.isFusion() && (engine.getEngineType() != Engine.FISSION)) {
            double weight = 0;
            for (Mounted m : tank.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_ENERGY) && !(wt instanceof CLChemicalLaserWeapon) && !(wt instanceof VehicleFlamerWeapon)) {
                    weight += m.getTonnage();
                }
                if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof
                        MiscType) && m.getLinkedBy().getType().
                        hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    weight += m.getLinkedBy().getTonnage();
                }
            }
            for (Mounted m : tank.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_CLUB) && m.getType().hasSubType(MiscType.S_SPOT_WELDER)) {
                    weight += m.getTonnage();
                }
            }
            return TestEntity.ceil(weight / 10, getWeightCeilingPowerAmp());
        }
        return 0;
    }
    
    /**
     * Check if the unit has combinations of equipment which are not allowed in
     * the construction rules.
     *
     * @param buff
     *            diagnostics are appended to this
     * @return true if the entity is illegal
     */
    @Override
    public boolean hasIllegalEquipmentCombinations(StringBuffer buff) {
        boolean illegal = super.hasIllegalEquipmentCombinations(buff);
        
        boolean hasSponsonTurret = false;

        for (Mounted m : getEntity().getMisc()) {
            if (m.getType().hasFlag(MiscType.F_SPONSON_TURRET)) {
                hasSponsonTurret = true;
            }
        }
        
        for (Mounted m : getEntity().getMisc()) {
            final MiscType misc = (MiscType) m.getType();
            
            if (misc.hasFlag(MiscType.F_JUMP_JET)) {
                if (hasSponsonTurret) {
                    buff.append("can't combine vehicular jump jets and sponson turret\n");
                    illegal = true;
                }
                if ((getEntity().getMovementMode() != EntityMovementMode.HOVER)
                        && (getEntity().getMovementMode() != EntityMovementMode.WHEELED)
                        && (getEntity().getMovementMode() != EntityMovementMode.TRACKED)
                        && (getEntity().getMovementMode() != EntityMovementMode.WIGE)) {
                    buff.append("jump jets only possible on vehicles with hover, wheeled, tracked, or Wing-in-Ground Effect movement mode\n");
                    illegal = true;
                }
            }
        }

        if ((tank.getMovementMode() == EntityMovementMode.VTOL)
                || (tank.getMovementMode() == EntityMovementMode.WIGE)
                || (tank.getMovementMode() == EntityMovementMode.HOVER)) {
            for (int i = 0; i < tank.locations(); i++) {
                if (tank.getArmorType(i) == EquipmentType.T_ARMOR_HARDENED) {
                    buff.append("Hardened armor can't be mounted on WiGE/Hover/Wheeled vehicles\n");
                    illegal = true;
                }
            }
        }
        
        // Ensure that omni tank turrets aren't overloaded
        if (tank.isOmni()) {
            // Check to see if the base chassis turret weight is set
            double turretWeight = 0;
            double turret2Weight = 0;
            for (Mounted m : tank.getEquipment()) {
                if ((m.getLocation() == tank.getLocTurret2())
                        && !(m.getType() instanceof AmmoType)) {
                    turret2Weight += m.getTonnage();
                }
                if ((m.getLocation() == tank.getLocTurret())
                        && !(m.getType() instanceof AmmoType)) {
                    turretWeight += m.getTonnage();
                }
            }
            turretWeight *= 0.1;
            turret2Weight *= 0.1;
            if (tank.isSupportVehicle()) {
                if (getEntity().getWeight() < 5) {
                    turretWeight = TestEntity.ceil(turretWeight, Ceil.KILO);
                    turret2Weight = TestEntity.ceil(turret2Weight, Ceil.KILO);
                } else {
                    turretWeight = TestEntity.ceil(turretWeight, Ceil.HALFTON);
                    turret2Weight = TestEntity.ceil(turret2Weight, Ceil.HALFTON);
                }
            } else {
                turretWeight = TestEntity.ceil(turretWeight,
                        getWeightCeilingTurret());
                turret2Weight = TestEntity.ceil(turret2Weight,
                        getWeightCeilingTurret());
            }
            if ((tank.getBaseChassisTurretWeight() >= 0)
                    && (turretWeight > tank.getBaseChassisTurretWeight())) {
                buff.append("Unit has more weight in the turret than allowed ")
                        .append("by base chassis!  Current weight: ")
                        .append(turretWeight)
                        .append(", base chassis turret weight: ")
                        .append(tank.getBaseChassisTurretWeight()).append("\n");
                illegal = true;
            }
            if ((tank.getBaseChassisTurret2Weight() >= 0)
                    && (turret2Weight > tank.getBaseChassisTurret2Weight())) {
                buff.append("Unit has more weight in the second turret than ")
                        .append("allowed by base chassis!  Current weight: ")
                        .append(turret2Weight).append(", base chassis turret weight: ")
                        .append(tank.getBaseChassisTurret2Weight()).append("\n");
                illegal = true;
            }
        }
        
        return illegal;
    }

    /**
     * @param tank      The Tank
     * @param eq        The equipment
     * @param location  A location index on the Entity
     * @param buffer    If non-null and the location is invalid, will be appended with an explanation
     * @return          Whether the equipment can be mounted in the location on the Tank
     */
    public static boolean isValidTankLocation(Tank tank, EquipmentType eq, int location,
                                              @Nullable StringBuffer buffer) {
        if (isBodyEquipment(eq) && (location != Tank.LOC_BODY)) {
            if (buffer != null) {
                buffer.append(eq.getName()).append(" must be mounted in the body.\n");
            }
            return false;
        }
        final boolean isRearLocation;
        final boolean isTurretLocation;
        // SuperHeavyTank and LargeSupportTank have the same location indices.
        if ((tank instanceof SuperHeavyTank) || (tank instanceof LargeSupportTank)) {
            isRearLocation = location == SuperHeavyTank.LOC_REAR;
            isTurretLocation = (location == SuperHeavyTank.LOC_TURRET) || (location == SuperHeavyTank.LOC_TURRET_2);
        } else {
            isRearLocation = location == Tank.LOC_REAR;
            isTurretLocation = (location == Tank.LOC_TURRET) || (location == Tank.LOC_TURRET_2);
        }
        if (eq instanceof MiscType) {
            // Equipment explicitly forbidden to a mast mount
            if ((eq.hasFlag(MiscType.F_MODULAR_ARMOR) || eq.hasFlag(MiscType.F_HARJEL)
                    || eq.hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM) || eq.hasFlag(MiscType.F_LIFTHOIST)
                    || eq.hasFlag(MiscType.F_MANIPULATOR) || eq.hasFlag(MiscType.F_FLUID_SUCTION_SYSTEM)
                    || eq.hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM) || eq.hasFlag(MiscType.F_SPRAYER))
                    && (tank instanceof VTOL) && (location == VTOL.LOC_ROTOR)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be mounted in the rotor.\n");
                }
                return false;
            }
            if ((eq.hasFlag(MiscType.F_HARJEL) || eq.hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM)
                    || eq.hasFlag(MiscType.F_SPRAYER)
                    || (eq.hasFlag(MiscType.F_LIFTHOIST) && !(tank instanceof VTOL)))
                    && (location == Tank.LOC_BODY)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be mounted in the body.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_BULLDOZER) && ((location != Tank.LOC_FRONT) && (location != Tank.LOC_REAR))) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted on the front or rear.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_CLUB) && eq.hasSubType(MiscType.S_PILE_DRIVER)
                    && (location != Tank.LOC_FRONT) && !isRearLocation) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted on the front or rear.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_CLUB) && eq.hasSubType(MiscType.S_WRECKING_BALL) && !isTurretLocation) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted on a turret.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_CLUB) && !eq.hasSubType(MiscType.S_PILE_DRIVER | MiscType.S_SPOT_WELDER
                    | MiscType.S_WRECKING_BALL)
                    && (location != Tank.LOC_FRONT) && !isRearLocation && !isTurretLocation) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted on the front, rear, or turret.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_LADDER) && ((location == Tank.LOC_FRONT) || isRearLocation
                    || ((tank instanceof VTOL) && (location == VTOL.LOC_ROTOR)))) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted on the side or turret.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_MAST_MOUNT) && (location != VTOL.LOC_ROTOR)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted on the rotor.\n");
                }
                return false;
            }
            // The minesweeper is also permitted in the front-side/rear-side location for "particularly
            // large vehicles" (TacOps, 326) but it is not clear how large this needs to be. Superheavy?
            // multi-hex large naval support?
            if (eq.hasFlag(MiscType.F_MINESWEEPER) && (location != Tank.LOC_FRONT) && !isRearLocation) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted on the front or rear.\n");
                }
                return false;
            }
        } else if (eq instanceof WeaponType) {
            if ((((WeaponType) eq).getAmmoType() == AmmoType.T_GAUSS_HEAVY)
                    && (location != Tank.LOC_FRONT) && !isRearLocation) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be mounted on the sides or turret.\n");
                }
                return false;
            }
            if ((((WeaponType) eq).getAmmoType() == AmmoType.T_IGAUSS_HEAVY)
                    && isTurretLocation) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be mounted on a turret.\n");
                }
                return false;
            }
            if (!eq.hasFlag(WeaponType.F_C3M) && !eq.hasFlag(WeaponType.F_C3MBS)
                    && !eq.hasFlag(WeaponType.F_TAG) && (location == Tank.LOC_BODY)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be mounted in the body.\n");
                }
                return false;
            }
            if ((tank instanceof VTOL) && (location == VTOL.LOC_ROTOR)
                    && !eq.hasFlag(WeaponType.F_TAG)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be mounted in the rotor.\n");
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a piece of equipment should be mounted in the body location.
     *
     * @param eq       The equipment
     * @return         Whether the equipment needs to be assigned to the body location.
     */
    public static boolean isBodyEquipment(EquipmentType eq) {
        if (eq instanceof MiscType) {
            return eq.hasFlag(MiscType.F_CHASSIS_MODIFICATION)
                    || (eq.hasFlag(MiscType.F_CASE) && !eq.isClan())
                    || eq.hasFlag(MiscType.F_CASEII)
                    || eq.hasFlag(MiscType.F_JUMP_JET)
                    || eq.hasFlag(MiscType.F_FUEL)
                    || eq.hasFlag(MiscType.F_BLUE_SHIELD);
        } else {
            return eq instanceof AmmoType;
        }
    }
}
