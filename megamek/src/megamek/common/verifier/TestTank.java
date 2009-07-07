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

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IEntityMovementMode;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SupportTank;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;

public class TestTank extends TestEntity {
    private Tank tank = null;

    public TestTank(Tank tank, TestEntityOption options, String fileString) {
        super(options, tank.getEngine(), getArmor(tank), getStructure(tank));
        this.tank = tank;
        this.fileString = fileString;
    }

    private static Structure getStructure(Tank tank) {
        int type = EquipmentType.T_STRUCTURE_STANDARD;
        int flag = 0;

        if (tank.getStructureType() == 1) {
            type = EquipmentType.T_STRUCTURE_ENDO_STEEL;
        }

        if (tank.isClan()) {
            flag |= Structure.CLAN_STRUCTURE;
        }
        return new Structure(type, flag);
    }

    private static Armor getArmor(Tank tank) {
        int type = EquipmentType.T_ARMOR_STANDARD;
        int flag = 0;

        type = tank.getArmorType();
        if (tank.isClanArmor()) {
            flag |= Armor.CLAN_ARMOR;
        }
        return new Armor(type, flag);
    }

    @Override
    public Entity getEntity() {
        return tank;
    }

    @Override
    public boolean isTank() {
        return true;
    }

    @Override
    public boolean isMech() {
        return false;
    }

    public float getTankWeightTurret() {
        float weight = 0f;
        for (Mounted m : tank.getWeaponList()) {
            if (m.getLocation() == Tank.LOC_TURRET) {
                weight += ((WeaponType) m.getType()).getTonnage(tank);
            }
        }
        return ceilMaxHalf(weight / 10.0f, getWeightCeilingTurret());
    }

    public float getTankWeightLifting() {
        if (tank.getMovementMode() == IEntityMovementMode.HOVER) {
            return tank.getWeight() / 10.0f;
        } else if (tank.getMovementMode() == IEntityMovementMode.VTOL) {
            return tank.getWeight() / 10.0f;
        } else if (tank.getMovementMode() == IEntityMovementMode.HYDROFOIL) {
            return tank.getWeight() / 10.0f;
        } else if (tank.getMovementMode() == IEntityMovementMode.SUBMARINE) {
            return tank.getWeight() / 10.0f;
        }
        return 0f;
    }

    public float getTankPowerAmplifier() {
        if (!engine.isFusion()) {
            int weight = 0;
            for (Mounted m : tank.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_LASER)
                        || wt.hasFlag(WeaponType.F_PPC)
                        || (wt.hasFlag(WeaponType.F_FLAMER) && (wt.getAmmoType() == AmmoType.T_NA))) {
                    weight += wt.getTonnage(tank);
                }
                if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof
                        MiscType) && m.getLinkedBy().getType().
                        hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    weight += ((MiscType)m.getLinkedBy().getType()).getTonnage(tank);
                }
            }
            return ceil(weight / 10f, getWeightCeilingPowerAmp());
        }
        return 0f;
    }

    @Override
    public float getWeightMisc() {
        return getTankWeightTurret() + getTankWeightLifting()
                + getTankPowerAmplifier();
    }

    @Override
    public float getWeightControls() {
        return TestEntity.ceilMaxHalf(tank.getWeight() / 20.0f,
                getWeightCeilingControls());
    }

    private int getTankCountHeatLaserWeapons() {
        int heat = 0;
        for (Mounted m : tank.getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)) {
                heat += wt.getHeat();
            }
            if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof
                    MiscType) && m.getLinkedBy().getType().
                    hasFlag(MiscType.F_PPC_CAPACITOR)) {
                heat += 5;
            }
        }
        return heat;
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        if (!engine.isFusion()) {
            return false;
        }
        if (getTankCountHeatLaserWeapons() <= 10) {
            return false;
        }
        if (tank.getTechLevel() == TechConstants.T_INTRO_BOXSET) {
            return false;
        }
        return false;
        // return true;
    }

    @Override
    public int getCountHeatSinks() {
        float heat = getTankCountHeatLaserWeapons();
        if (hasDoubleHeatSinks()) {
            heat = heat / 2.0f;
        }
        return Math.round(heat);
    }

    @Override
    public int getWeightHeatSinks() {
        int heat = getCountHeatSinks();
        heat -= engine.getWeightFreeEngineHeatSinks();
        if (heat < 0) {
            heat = 0;
        }
        return heat;
    }

    @Override
    public String printWeightMisc() {
        return (!tank.hasNoTurret() ? StringUtil.makeLength("Turret:",
                getPrintSize() - 5)
                + makeWeightString(getTankWeightTurret()) + "\n" : "")
                + (getTankWeightLifting() != 0 ? StringUtil.makeLength(
                        "Lifting Equip:", getPrintSize() - 5)
                        + makeWeightString(getTankWeightLifting()) + "\n" : "")
                + (getTankPowerAmplifier() != 0 ? StringUtil.makeLength(
                        "Power Amp:", getPrintSize() - 5)
                        + makeWeightString(getTankPowerAmplifier()) + "\n" : "");
    }

    @Override
    public String printWeightControls() {
        return StringUtil.makeLength("Controls:", getPrintSize() - 5)
                + makeWeightString(getWeightControls()) + "\n";
    }

    public Tank getTank() {
        return tank;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, true);
    }

    @Override
    public boolean correctEntity(StringBuffer buff, boolean ignoreAmmo) {
        if ((tank instanceof VTOL) || (tank instanceof SupportTank)) {
            return true;
        } // don't bother checking, won't work. Needs fixing (new class
            // needed.)
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
        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (hasIllegalTechLevels(buff, ignoreAmmo)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }
        // only tanks with fusion engine can be vacuum protected
        if (!tank.getEngine().isFusion() && !tank.doomedInVacuum()) {
            buff.append("Vacuum protection requires fusion engine.\n");
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
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (").append(
                    calculateWeight()).append(")\n");
        }

        buff.append(printWeightCalculation()).append("\n");
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String getName() {
        return "Tank: " + tank.getDisplayName();
    }
}
