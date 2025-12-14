/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.battleValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.ppc.PPCWeapon;
import megamek.common.weapons.prototypes.innerSphere.ISERLaserLargePrototype;
import megamek.common.weapons.prototypes.innerSphere.ISPulseLaserLargePrototype;
import megamek.common.weapons.prototypes.innerSphere.ISPulseLaserMediumPrototype;
import megamek.common.weapons.prototypes.innerSphere.ISPulseLaserMediumRecovered;
import megamek.common.weapons.prototypes.innerSphere.ISPulseLaserSmallPrototype;

public abstract class HeatTrackingBVCalculator extends BVCalculator {

    HeatTrackingBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected boolean usesWeaponHeat() {
        return !(entity instanceof ConvFighter);
    }

    @Override
    protected void processExplosiveEquipment() {
        if (defensiveValue < 1) {
            defensiveValue = 1;
            bvReport.addLine("Minimum Defensive Value", "", "= 1");
        }
    }

    @Override
    protected void processWeapons() {
        if (entity instanceof ConvFighter) {
            super.processWeapons();
            return;
        }

        List<WeaponBvHeatRecord> weaponRecords = new ArrayList<>();
        for (WeaponMounted mounted : entity.getTotalWeaponList()) {
            if (!countAsOffensiveWeapon(mounted)) {
                continue;
            }

            double thisWeaponBV = processWeapon(mounted, false, false);
            weaponRecords.add(new WeaponBvHeatRecord(mounted, thisWeaponBV, weaponHeat(mounted)));
        }

        if (entity.hasVibroblades()) {
            Mounted<?> vibroblade = getVibroblade(Mek.LOC_LEFT_ARM);
            if (vibroblade != null) {
                double weaponHeat = entity.getActiveVibrobladeHeat(Mek.LOC_LEFT_ARM, true);
                double thisWeaponBV = processWeapon(vibroblade, false, false);
                weaponRecords.add(new WeaponBvHeatRecord(vibroblade, thisWeaponBV, weaponHeat));
            }

            vibroblade = getVibroblade(Mek.LOC_RIGHT_ARM);
            if (vibroblade != null) {
                double weaponHeat = entity.getActiveVibrobladeHeat(Mek.LOC_LEFT_ARM, true);
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
        int heatEfficiency = heatEfficiency();
        heatEfficiencyExceeded = heatEfficiency <= 0;

        for (WeaponBvHeatRecord weaponRecord : weaponRecords) {
            heatSum += weaponRecord.heat;
            processWeapon(weaponRecord.weapon, true, true);
            if (heatSum >= heatEfficiency) {
                heatEfficiencyExceeded = true;
            }
        }
        heatEfficiencyExceeded = false;
    }

    /**
     * The weapon sorter. Will place weapons without heat first, then in descending order of BV > heat.
     */
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
        Mounted<?> weapon;
        double bv;
        double heat;

        WeaponBvHeatRecord(Mounted<?> weapon, double bv, double heat) {
            this.weapon = weapon;
            this.bv = bv;
            this.heat = heat;
        }
    }

    private @Nullable Mounted<?> getVibroblade(int location) {
        for (int slot = 0; slot < entity.locations(); slot++) {
            CriticalSlot cs = entity.getCritical(location, slot);

            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                Mounted<?> mounted = cs.getMount();
                if ((mounted.getType() instanceof MiscType)
                      && ((MiscType) mounted.getType()).isVibroblade()) {
                    return mounted;
                }
            }
        }
        return null;
    }

    protected double weaponHeat(WeaponMounted weapon) {
        WeaponType wType = weapon.getType();
        double weaponHeat = wType.getHeat();

        if (weapon.isOneShot()) {
            weaponHeat /= 4;
        }

        if ((wType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ULTRA) || (wType.getAmmoType()
              == AmmoType.AmmoTypeEnum.AC_ULTRA_THB)) {
            weaponHeat *= 2;
        } else if (wType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_ROTARY) {
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
        if ((wType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_STREAK)
              || (wType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_STREAK)
              || (wType.getAmmoType() == AmmoType.AmmoTypeEnum.IATM)) {
            weaponHeat *= 0.5;
        }

        // PPC with Capacitor
        if (wType.hasFlag(WeaponType.F_PPC) && (weapon.getLinkedBy() != null)) {
            weaponHeat += 5;
        }
        return weaponHeat;
    }

    protected double totalWeaponHeat() {
        return entity.getTotalWeaponList().stream()
              .filter(this::countAsOffensiveWeapon)
              .mapToDouble(this::weaponHeat)
              .sum();
    }

    /**
     * @return True when the given mounted may count as explosive for BV purposes.
     */
    protected boolean countsAsExplosive(Mounted<?> mounted) {
        EquipmentType eType = mounted.getType();
        if (mounted.isWeaponGroup() || (mounted.getLocation() == Entity.LOC_NONE)
              || !eType.isExplosive(mounted, true)) {
            return false;
        } else if ((eType instanceof AmmoType) && (mounted.getUsableShotsLeft() == 0)) {
            return false;
        } else if ((eType instanceof PPCWeapon) && (mounted.getLinkedBy() == null)) {
            return false;
        } else {
            return (!(eType instanceof WeaponType)) || ((((WeaponType) eType).getAmmoType()
                  != AmmoType.AmmoTypeEnum.AC_ROTARY)
                  && (((WeaponType) eType).getAmmoType() != AmmoType.AmmoTypeEnum.AC)
                  && (((WeaponType) eType).getAmmoType() != AmmoType.AmmoTypeEnum.AC_IMP)
                  && (((WeaponType) eType).getAmmoType() != AmmoType.AmmoTypeEnum.AC_PRIMITIVE)
                  && (((WeaponType) eType).getAmmoType() != AmmoType.AmmoTypeEnum.PAC)
                  && (((WeaponType) eType).getAmmoType() != AmmoType.AmmoTypeEnum.LAC));
        }
    }
}
