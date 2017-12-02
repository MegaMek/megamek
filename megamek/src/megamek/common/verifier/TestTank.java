/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Author: Reinhard Vicinus
 */

package megamek.common.verifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.GunEmplacement;
import megamek.common.ITechManager;
import megamek.common.InfantryBay;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SuperHeavyTank;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.Transporter;
import megamek.common.TroopSpace;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;
import megamek.common.weapons.lasers.CLChemicalLaserWeapon;

public class TestTank extends TestEntity {

    /**
     * Defines the maximum amount of armor a VTOL can mount on its rotor.
     */
    public static int VTOL_MAX_ROTOR_ARMOR = 2;

    private Tank tank = null;

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
     * @param noHardened
     * @param techManager
     * @return
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
    public boolean isJumpship() {
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
                        && !(m.getType() instanceof AmmoType)) {
                    weight += m.getType().getTonnage(tank);
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
                    weight += m.getType().getTonnage(tank);
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

    private int getTankCountHeatLaserWeapons() {
        int heat = 0;
        for (Mounted m : tank.getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if ((wt.hasFlag(WeaponType.F_LASER) && (wt.getAmmoType() == AmmoType.T_NA))
                    || wt.hasFlag(WeaponType.F_PPC)
                    || wt.hasFlag(WeaponType.F_PLASMA)
                    || wt.hasFlag(WeaponType.F_PLASMA_MFUK)
                    || (wt.hasFlag(WeaponType.F_FLAMER) && (wt.getAmmoType() == AmmoType.T_NA))) {
                heat += wt.getHeat();
            }
            // laser insulator reduce heat by 1, to a minimum of 1
            if (wt.hasFlag(WeaponType.F_LASER) && (m.getLinkedBy() != null)
                    && !m.getLinkedBy().isInoperable()
                    && m.getLinkedBy().getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
                heat -= 1;
                if (heat == 0) {
                    heat++;
                }
            }

            if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof
                    MiscType) && m.getLinkedBy().getType().
                    hasFlag(MiscType.F_PPC_CAPACITOR)) {
                heat += 5;
            }
        }
        for (Mounted m : tank.getMisc()) {
            MiscType mtype = (MiscType)m.getType();
            // mobile HPGs count as energy weapons for construction purposes
            if (mtype.hasFlag(MiscType.F_MOBILE_HPG)) {
                heat += 20;
            }
            if (mtype.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                heat += 2;
            }
            if (mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)||mtype.hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)) {
                heat += 12;
            }
        }
        return heat;
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        // tanks can't have DHS
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        float heat = getTankCountHeatLaserWeapons();
        return Math.round(heat);
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
        if (tank.hasWorkingMisc(MiscType.F_ARMORED_MOTIVE_SYSTEM) && !((tank.getMovementMode() == EntityMovementMode.WHEELED) || (tank.getMovementMode() == EntityMovementMode.TRACKED) || (tank.getMovementMode() == EntityMovementMode.HOVER) || (tank.getMovementMode() == EntityMovementMode.HYDROFOIL) || (tank.getMovementMode() == EntityMovementMode.NAVAL) || (tank.getMovementMode() == EntityMovementMode.SUBMARINE) || (tank.getMovementMode() == EntityMovementMode.WIGE))) {
            buff.append("Armored Motive system and incompatible movemement mode!\n\n");
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
                    if (m.getLocation() == VTOL.LOC_ROTOR) {
                        buff.append("rotor equipment must be placed in mast mount");
                        correct = false;
                    }
                }
            }
            if (tank.getOArmor(VTOL.LOC_ROTOR) > VTOL_MAX_ROTOR_ARMOR) {
                buff.append(tank.getOArmor(VTOL.LOC_ROTOR));
                buff.append(" points of VTOL rotor armor exceed "
                        + VTOL_MAX_ROTOR_ARMOR + "-point limit.\n\n");
                correct = false;
            }
        }
        for (Mounted m : tank.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_COMBAT_VEHICLE_ESCAPE_POD)) {
                if (m.getLocation() != (tank instanceof SuperHeavyTank?SuperHeavyTank.LOC_REAR:Tank.LOC_REAR)) {
                    buff.append("combat vehicle escape pod must be placed in rear");
                    correct = false;
                }
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
        // only tanks with fusion engine can be vacuum protected
        if(tank.hasEngine() && !(tank.getEngine().isFusion() 
                || (tank.getEngine().getEngineType() == Engine.FUEL_CELL)
                || (tank.getEngine().getEngineType() == Engine.SOLAR)
                || (tank.getEngine().getEngineType() == Engine.BATTERY)
                || (tank.getEngine().getEngineType() == Engine.FISSION)
                || (tank.getEngine().getEngineType() == Engine.NONE))
                && !tank.doomedInVacuum()) {
                buff.append("Vacuum protection requires fusion engine.\n");
                correct = false;
                }

        if (!correctCriticals(buff)) {
            correct = false;
        }
        return correct;
    }

    public boolean correctCriticals(StringBuffer buff) {
        Vector<Mounted> unallocated = new Vector<Mounted>();
        boolean correct = true;

        for (Mounted mount : tank.getMisc()) {
            if (mount.getLocation() == Entity.LOC_NONE && !(mount.getType().getCriticals(tank) == 0)) {
                unallocated.add(mount);
            }
        }
        for (Mounted mount : tank.getWeaponList()) {
            if (mount.getLocation() == Entity.LOC_NONE) {
                unallocated.add(mount);
            }
        }
        for (Mounted mount : tank.getAmmo()) {
            int ammoType = ((AmmoType)mount.getType()).getAmmoType();
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
        buff.append("Intro year: ").append(tank.getYear());
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
                engineSlots = tank.getEngine().hasFlag(Engine.CLAN_ENGINE)? 1 : 2;
            } else if (tank.getEngine().getEngineType() == Engine.XXL_ENGINE) {
                engineSlots = tank.getEngine().hasFlag(Engine.CLAN_ENGINE)? 2 : 4;
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
                    buff.append(StringUtil.makeLength(mount.getType().getName(), 30));
                    buff.append(mount.getType().getTankslots(tank)).append("\n");
                    addedCargo = true;
                    continue;
                } else {
                    continue;
                }
            }
            if (!((mount.getType() instanceof AmmoType) || Arrays.asList(
                    EquipmentType.armorNames).contains(
                    mount.getType().getName()))) {
                buff.append(StringUtil.makeLength(mount.getType().getName(), 30));
                buff.append(mount.getType().getTankslots(tank)).append("\n");
            }
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
        Map<String, Boolean> foundAmmo = new HashMap<String, Boolean>();
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
                    weight += wt.getTonnage(tank);
                }
                if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof
                        MiscType) && m.getLinkedBy().getType().
                        hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    weight += ((MiscType)m.getLinkedBy().getType()).getTonnage(tank);
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
            final MiscType misc = (MiscType)m.getType();
            
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

            if (misc.hasFlag(MiscType.F_HARJEL)
                    && ((m.getLocation() == Tank.LOC_BODY)
                            || ((getEntity() instanceof VTOL)
                                && (m.getLocation() == VTOL.LOC_ROTOR)))) {
                illegal = true;
                buff.append("Unable to load harjel in body or rotor.\n");
            }

            if (misc.hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM)
                    && m.getLocation() != Tank.LOC_BODY) {
                illegal = true;
                buff.append("Vehicle must not mount light fluid suction system in body\n");                
            }

            if (misc.hasFlag(MiscType.F_BULLDOZER)) {
                for (Mounted m2 : getEntity().getMisc()) {
                    if (m2.getLocation() == m.getLocation()) {
                        if (m2.getType().hasFlag(MiscType.F_CLUB)) {
                            if (m2.getType().hasSubType(MiscType.S_BACKHOE)
                                    || m2.getType().hasSubType(
                                            MiscType.S_CHAINSAW)
                                    || m2.getType().hasSubType(
                                            MiscType.S_COMBINE)
                                    || m2.getType().hasSubType(
                                            MiscType.S_DUAL_SAW)
                                    || m2.getType().hasSubType(
                                            MiscType.S_PILE_DRIVER)
                                    || m2.getType().hasSubType(
                                            MiscType.S_MINING_DRILL)
                                    || m2.getType().hasSubType(
                                            MiscType.S_ROCK_CUTTER)
                                    || m2.getType().hasSubType(
                                            MiscType.S_WRECKING_BALL)) {
                                illegal = true;
                                buff.append("bulldozer in same location as prohibited physical weapon\n");
                            }
                        }
                    }
                }
                if ((m.getLocation() != Tank.LOC_FRONT) && (m.getLocation() != Tank.LOC_REAR)) {
                    illegal = true;
                    buff.append("bulldozer must be mounted in front\n");
                }
                if ((getEntity().getMovementMode() != EntityMovementMode.TRACKED)
                        && (getEntity().getMovementMode() != EntityMovementMode.WHEELED)) {
                    illegal = true;
                    buff.append("bulldozer must be mounted in unit with tracked or wheeled movement mode\n");
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
                    turret2Weight += m.getType().getTonnage(tank);
                }
                if ((m.getLocation() == tank.getLocTurret())
                        && !(m.getType() instanceof AmmoType)) {
                    turretWeight += m.getType().getTonnage(tank);
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
                buff.append("Unit has more weight in the turret than allowed "
                        + "by base chassis!  Current weight: " + turretWeight
                        + ", base chassis turret weight: "
                        + tank.getBaseChassisTurretWeight() + "\n");
                illegal = true;
            }
            if ((tank.getBaseChassisTurret2Weight() >= 0)
                    && (turret2Weight > tank.getBaseChassisTurret2Weight())) {
                buff.append("Unit has more weight in the second turret than "
                        + "allowed by base chassis!  Current weight: "
                        + turret2Weight + ", base chassis turret weight: "
                        + tank.getBaseChassisTurret2Weight() + "\n");
                illegal = true;
            }
        }
        
        return illegal;
    }
}
