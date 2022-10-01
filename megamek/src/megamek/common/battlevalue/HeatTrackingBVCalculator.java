/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of Megaentity.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Megaentity. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.battlevalue;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.ppc.PPCWeapon;
import megamek.common.weapons.prototypes.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class HeatTrackingBVCalculator extends BVCalculator {

    HeatTrackingBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected boolean usesWeaponHeat() {
        return true;
    }

    @Override
    protected void processExplosiveEquipment() {
        if (defensiveValue < 1) {
            defensiveValue = 1;
            bvReport.addLine("Minimum Defensive Value", "", "= 1");
        }
        defensiveValue = Math.max(1, defensiveValue);
    }

    @Override
    protected void processWeapons() {
        List<WeaponBvHeatRecord> weaponRecords = new ArrayList<>();
        for (Mounted mounted : entity.getWeaponList()) {
            if (!countAsOffensiveWeapon(mounted)) {
                continue;
            }

            double thisWeaponBV = processWeapon(mounted, false, false);
            weaponRecords.add(new WeaponBvHeatRecord(mounted, thisWeaponBV, weaponHeat(mounted)));
        }

        if (entity.hasVibroblades()) {
            Mounted vibroblade = getVibroblade(Mech.LOC_LARM);
            if (vibroblade != null) {
                double weaponHeat = entity.getActiveVibrobladeHeat(Mech.LOC_LARM, true);
                double thisWeaponBV = processWeapon(vibroblade, false, false);
                weaponRecords.add(new WeaponBvHeatRecord(vibroblade, thisWeaponBV, weaponHeat));
            }

            vibroblade = getVibroblade(Mech.LOC_RARM);
            if (vibroblade != null) {
                double weaponHeat = entity.getActiveVibrobladeHeat(Mech.LOC_LARM, true);
                double thisWeaponBV = processWeapon(vibroblade, false, false);
                weaponRecords.add(new WeaponBvHeatRecord(vibroblade, thisWeaponBV, weaponHeat));
            }
        }

        bvReport.addLine("Weapons:", "", "");
        if (weaponRecords.isEmpty()) {
            bvReport.addLine("- None", "", "");
            return;
        }

        weaponRecords.sort(heatSorter);
        boolean exceededEfficiency = false;
        int heatEfficiency = heatEfficiency();

        for (WeaponBvHeatRecord weaponRecord : weaponRecords) {
            heatSum += weaponRecord.heat;
            processWeapon(weaponRecord.weapon, true, true);
            if ((!exceededEfficiency) && (heatSum >= heatEfficiency)) {
                exceededEfficiency = true;
                heatEfficiencyExceeded = true;
            }
        }
        heatEfficiencyExceeded = false;
    }

    /** The weapon sorter. Will place weapons without heat first, then in descending order of BV > heat. */
    Comparator<WeaponBvHeatRecord> heatSorter = (obj1, obj2) -> {
        if ((obj1.heat == 0) && (obj2.heat > 0)) {
            return -1;
        } else if ((obj1.heat > 0) && (obj2.heat == 0)) {
            return 1;
        } else if ((obj1.heat == 0) && (obj2.heat == 0)) {
            return 0;
        }

        if (obj1.bv == obj2.bv) {
            return Double.compare(obj1.heat, obj2.heat);
        }
        return Double.compare(obj2.bv, obj1.bv);
    };

    static class WeaponBvHeatRecord {
        Mounted weapon;
        double bv;
        double heat;

        WeaponBvHeatRecord(Mounted weapon, double bv, double heat) {
            this.weapon = weapon;
            this.bv = bv;
            this.heat = heat;
        }
    }

    private @Nullable Mounted getVibroblade(int location) {
        for (int slot = 0; slot < entity.locations(); slot++) {
            CriticalSlot cs = entity.getCritical(location, slot);

            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                Mounted mounted = cs.getMount();
                if ((mounted.getType() instanceof MiscType)
                        && ((MiscType) mounted.getType()).isVibroblade()) {
                    return mounted;
                }
            }
        }
        return null;
    }

    protected double weaponHeat(Mounted weapon) {
        WeaponType wType = (WeaponType) weapon.getType();
        double weaponHeat = wType.getHeat();

        // one shot weapons count 1/4
        if (weapon.isOneShotWeapon()) {
            weaponHeat *= 0.25;
        }

        // double heat for ultras
        if ((wType.getAmmoType() == AmmoType.T_AC_ULTRA)
                || (wType.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
            weaponHeat *= 2;
        } else if (wType.getAmmoType() == AmmoType.T_AC_ROTARY) {
            weaponHeat *= 6;
        }

        // 1d6 extra heat; add half for heat calculations (1d3/+2 for small pulse)
        if ((wType instanceof ISERLaserLargePrototype)
                || (wType instanceof ISPulseLaserLargePrototype)
                || (wType instanceof ISPulseLaserMediumPrototype)
                || (wType instanceof ISPulseLaserMediumRecovered)) {
            weaponHeat += 3;
        } else if (wType instanceof ISPulseLaserSmallPrototype) {
            weaponHeat += 2;
        }

        // RISC laser pulse module adds 2 heat
        if ((wType.hasFlag(WeaponType.F_LASER)) && (weapon.getLinkedBy() != null)
                && (weapon.getLinkedBy().getType() instanceof MiscType)
                && (weapon.getLinkedBy().getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE))) {
            weaponHeat += 2;
        }

        // laser insulator reduce heat by 1, to a minimum of 1
        if (wType.hasFlag(WeaponType.F_LASER)
                && (weapon.getLinkedBy() != null)
                && !weapon.getLinkedBy().isInoperable()
                && (weapon.getLinkedBy().getType() instanceof MiscType)
                && weapon.getLinkedBy().getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
            weaponHeat = Math.max(1, weaponHeat - 1);
        }

        // half heat for streaks
        if ((wType.getAmmoType() == AmmoType.T_SRM_STREAK)
                || (wType.getAmmoType() == AmmoType.T_LRM_STREAK)
                || (wType.getAmmoType() == AmmoType.T_IATM)) {
            weaponHeat *= 0.5;
        }

        // PPC with Capacitor
        if (wType.hasFlag(WeaponType.F_PPC) && (weapon.getLinkedBy() != null)) {
            weaponHeat += 5;
        }
        return weaponHeat;
    }

    protected double totalWeaponHeat() {
        return entity.getWeaponList().stream()
                .filter(this::countAsOffensiveWeapon)
                .mapToDouble(this::weaponHeat)
                .sum();
    }

    /** @return True when the given mounted may count as explosive for BV purposes. */
    protected boolean countsAsExplosive(Mounted mounted) {
        EquipmentType eType = mounted.getType();
        if (mounted.isWeaponGroup() || (mounted.getLocation() == Entity.LOC_NONE)
                || !eType.isExplosive(mounted, true)) {
            return false;
        } else if ((eType instanceof AmmoType) && (mounted.getUsableShotsLeft() == 0)) {
            return false;
        } else if ((eType instanceof MiscType) && eType.hasFlag(MiscType.F_PPC_CAPACITOR)) {
            return false;
        } else if ((eType instanceof PPCWeapon) && (mounted.getLinkedBy() == null)) {
            return false;
        } else if ((eType instanceof WeaponType) && ((((WeaponType) eType).getAmmoType() == AmmoType.T_AC_ROTARY)
                || (((WeaponType) eType).getAmmoType() == AmmoType.T_AC)
                || (((WeaponType) eType).getAmmoType() == AmmoType.T_AC_IMP)
                || (((WeaponType) eType).getAmmoType() == AmmoType.T_AC_PRIMITIVE)
                || (((WeaponType) eType).getAmmoType() == AmmoType.T_PAC)
                || (((WeaponType) eType).getAmmoType() == AmmoType.T_LAC))) {
            return false;
        }

        return true;
    }
}