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

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import megamek.common.MPCalculationSetting;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.Warship;

public class WarShipBVCalculator extends JumpShipBVCalculator {

    protected static final int BV_LOC_LEFT_BROADSIDE = 6;
    protected static final int BV_LOC_RIGHT_BROADSIDE = 7;

    protected static final int[] nextLocationCW = { 5, 0, 6, 2, 3, 7, 1, 4 };
    protected static final int[] nextLocationCCW = { 1, 6, 3, 4, 7, 0, 2, 5 };
    protected static final int[] oppositeLocation = { 3, 4, 5, 0, 1, 2, 7, 6 };

    protected int weakerAdjacentArc;

    WarShipBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected boolean validArmorLocation(int location) {
        return (location <= 5) || (location == 7) || (location == 8);
    }

    @Override
    protected int bvLocation(Mounted<?> equipment) {
        if (equipment.getLocation() == Jumpship.LOC_NOSE) {
            return BV_LOC_NOSE;
        } else if (equipment.getLocation() == Jumpship.LOC_FLS) {
            return BV_LOC_LEFT;
        } else if (equipment.getLocation() == Jumpship.LOC_ALS) {
            return BV_LOC_LEFT_AFT;
        } else if (equipment.getLocation() == Jumpship.LOC_AFT) {
            return BV_LOC_AFT;
        } else if (equipment.getLocation() == Jumpship.LOC_ARS) {
            return BV_LOC_RIGHT_AFT;
        } else if (equipment.getLocation() == Jumpship.LOC_FRS) {
            return BV_LOC_RIGHT;
        } else if (equipment.getLocation() == Warship.LOC_LBS) {
            return BV_LOC_LEFT_BROADSIDE;
        } else {
            return BV_LOC_RIGHT_BROADSIDE;
        }
    }

    @Override
    protected void determineFront() {
        Predicate<Mounted<?>> frontFilter = frontWeaponFilter();
        Predicate<Mounted<?>> rearFilter = rearWeaponFilter();
        Predicate<Mounted<?>> leftFilter = leftWeaponFilter();
        Predicate<Mounted<?>> rightFilter = rightWeaponFilter();
        Predicate<Mounted<?>> leftAftFilter = leftAftWeaponFilter();
        Predicate<Mounted<?>> rightAftFilter = rightAftWeaponFilter();
        Predicate<Mounted<?>> leftBroadsideFilter = weapon -> (weapon.getLocation() == Warship.LOC_LBS);
        Predicate<Mounted<?>> rightBroadsideFilter = weapon -> (weapon.getLocation() == Warship.LOC_RBS);
        Map<Integer, Double> bvPerArc = new HashMap<>();
        double weaponsBVFront = processWeaponSection(false, frontFilter, false);
        double weaponsBVRear = processWeaponSection(false, rearFilter, false);
        double weaponsBVLeft = processWeaponSection(false, leftFilter, false);
        double weaponsBVRight = processWeaponSection(false, rightFilter, false);
        double weaponsBVAftLeft = processWeaponSection(false, leftAftFilter, false);
        double weaponsBVAftRight = processWeaponSection(false, rightAftFilter, false);
        double weaponsBVRightBroadside = processWeaponSection(false, rightBroadsideFilter, false);
        double weaponsBVLeftBroadside = processWeaponSection(false, leftBroadsideFilter, false);
        bvPerArc.put(BV_LOC_NOSE, weaponsBVFront);
        bvPerArc.put(BV_LOC_AFT, weaponsBVRear);
        bvPerArc.put(BV_LOC_LEFT_BROADSIDE, weaponsBVLeftBroadside);
        bvPerArc.put(BV_LOC_RIGHT_BROADSIDE, weaponsBVRightBroadside);
        final double maxBV = bvPerArc.values().stream().mapToDouble(bv -> bv).max().orElse(0);
        for (Map.Entry<Integer, Double> entry : bvPerArc.entrySet()) {
            if (entry.getValue() == maxBV) {
                nominalNoseLocation = entry.getKey();
                break;
            }
        }
        bvPerArc.put(BV_LOC_LEFT, weaponsBVLeft);
        bvPerArc.put(BV_LOC_RIGHT, weaponsBVRight);
        bvPerArc.put(BV_LOC_LEFT_AFT, weaponsBVAftLeft);
        bvPerArc.put(BV_LOC_RIGHT_AFT, weaponsBVAftRight);
        int firstAdjacentArc = getAdjacentLocationCCW(nominalNoseLocation);
        int secondAdjacentArc = getAdjacentLocationCW(nominalNoseLocation);
        if (bvPerArc.get(firstAdjacentArc) > bvPerArc.get(secondAdjacentArc)) {
            nominalLeftLocation = firstAdjacentArc;
            nominalRightLocation = secondAdjacentArc;
        } else {
            nominalLeftLocation = secondAdjacentArc;
            nominalRightLocation = firstAdjacentArc;
        }

        weakerAdjacentArc = nominalRightLocation;
        nominalRightLocation = oppositeLocation[nominalNoseLocation];

        bvReport.addLine("Nominal Nose Location",
              arcName(nominalNoseLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalNoseLocation)),
              "");
        bvReport.addLine("Nominal Left Location",
              arcName(nominalLeftLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalLeftLocation)),
              "");
        bvReport.addLine("Nominal Right Location",
              arcName(nominalRightLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalRightLocation)),
              "");
        frontAndRearDecided = true;
    }

    @Override
    protected void processWeapons() {
        for (WeaponMounted weapon : entity.getTotalWeaponList()) {
            if (countAsOffensiveWeapon(weapon)) {
                // Create a copy of the keySet to avoid CME when modifying the map
                List<WeaponMounted> keys = new ArrayList<>(collectedWeapons.keySet());
                WeaponMounted key = keys.stream().filter(wp -> canBeSummed(weapon, wp)).findFirst().orElse(weapon);
                collectedWeapons.merge(key, 1, Integer::sum);
            }
        }
        int heatEfficiency = heatEfficiency();

        double totalHeatSum = processArc(nominalNoseLocation);
        bvReport.addLine("Total Heat:", formatForReport(totalHeatSum), "");
        heatEfficiencyExceeded = totalHeatSum > heatEfficiency;
        if (heatEfficiencyExceeded) {
            bvReport.addLine("Heat Efficiency Exceeded", "", "");
        }

        totalHeatSum += processArc(nominalLeftLocation);
        bvReport.addLine("Total Heat:", formatForReport(totalHeatSum), "");
        heatEfficiencyExceeded = totalHeatSum > heatEfficiency;
        if (heatEfficiencyExceeded) {
            bvReport.addLine("Heat Efficiency Exceeded", "", "");
        }

        processArc(nominalRightLocation);
        processArc(getAdjacentLocationCCW(nominalRightLocation));
        processArc(getAdjacentLocationCW(nominalRightLocation));
        processArc(getAdjacentLocationCCW(getAdjacentLocationCCW(nominalRightLocation)));
        processArc(getAdjacentLocationCW(getAdjacentLocationCW(nominalRightLocation)));
        processArc(weakerAdjacentArc);
        bvReport.addEmptyLine();
    }

    @Override
    protected String arcName(int bvLocation) {
        return switch (bvLocation) {
            case BV_LOC_NOSE -> entity.getLocationName(Jumpship.LOC_NOSE);
            case BV_LOC_LEFT -> entity.getLocationName(Jumpship.LOC_FLS);
            case BV_LOC_LEFT_AFT -> entity.getLocationName(Jumpship.LOC_ALS);
            case BV_LOC_AFT -> entity.getLocationName(Jumpship.LOC_AFT);
            case BV_LOC_RIGHT_AFT -> entity.getLocationName(Jumpship.LOC_ARS);
            case BV_LOC_RIGHT -> entity.getLocationName(Jumpship.LOC_FRS);
            case BV_LOC_LEFT_BROADSIDE -> entity.getLocationName(Warship.LOC_LBS);
            case BV_LOC_RIGHT_BROADSIDE -> entity.getLocationName(Warship.LOC_RBS);
            default -> "Error: Unexpected location value.";
        };
    }

    @Override
    protected int getAdjacentLocationCCW(int bvLocation) {
        return nextLocationCCW[bvLocation];
    }

    @Override
    protected int getAdjacentLocationCW(int bvLocation) {
        return nextLocationCW[bvLocation];
    }

    @Override
    protected int getOppositeLocation(int bvLocation) {
        return oppositeLocation[bvLocation];
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        return aero.getRunMP(MPCalculationSetting.BV_CALCULATION);
    }

    @Override
    protected void processTypeModifier() {
        double typeModifier = 0.8;
        bvReport.addLine("Type Modifier:",
              formatForReport(defensiveValue) + " x " + formatForReport(typeModifier),
              "= " + formatForReport(defensiveValue * typeModifier));
        defensiveValue *= typeModifier;
    }
}
