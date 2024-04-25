/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
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
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.battlevalue;

import megamek.common.*;
import megamek.common.equipment.WeaponMounted;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class WarShipBVCalculator extends JumpShipBVCalculator {

    protected static final int BVLOC_LEFT_BROADSIDE = 6;
    protected static final int BVLOC_RIGHT_BROADSIDE = 7;

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
    protected int bvLocation(Mounted equipment) {
        if (equipment.getLocation() == Jumpship.LOC_NOSE) {
            return BVLOC_NOSE;
        } else if (equipment.getLocation() == Jumpship.LOC_FLS) {
            return BVLOC_LEFT;
        } else if (equipment.getLocation() == Jumpship.LOC_ALS) {
            return BVLOC_LEFT_AFT;
        } else if (equipment.getLocation() == Jumpship.LOC_AFT) {
            return BVLOC_AFT;
        } else if (equipment.getLocation() == Jumpship.LOC_ARS) {
            return BVLOC_RIGHT_AFT;
        } else if (equipment.getLocation() == Jumpship.LOC_FRS) {
            return BVLOC_RIGHT;
        } else if (equipment.getLocation() == Warship.LOC_LBS) {
            return BVLOC_LEFT_BROADSIDE;
        } else {
            return BVLOC_RIGHT_BROADSIDE;
        }
    }

    @Override
    protected void determineFront() {
        Predicate<Mounted> frontFilter = frontWeaponFilter();
        Predicate<Mounted> rearFilter = rearWeaponFilter();
        Predicate<Mounted> leftFilter = leftWeaponFilter();
        Predicate<Mounted> rightFilter = rightWeaponFilter();
        Predicate<Mounted> leftAftFilter = leftAftWeaponFilter();
        Predicate<Mounted> rightAftFilter = rightAftWeaponFilter();
        Predicate<Mounted> leftBroadsideFilter = weapon -> (weapon.getLocation() == Warship.LOC_LBS);
        Predicate<Mounted> rightBroadsideFilter = weapon -> (weapon.getLocation() == Warship.LOC_RBS);
        Map<Integer, Double> bvPerArc = new HashMap<>();
        double weaponsBVFront = processWeaponSection(false, frontFilter, false);
        double weaponsBVRear = processWeaponSection(false, rearFilter, false);
        double weaponsBVLeft = processWeaponSection(false, leftFilter, false);
        double weaponsBVRight = processWeaponSection(false, rightFilter, false);
        double weaponsBVAftLeft = processWeaponSection(false, leftAftFilter, false);
        double weaponsBVAftRight = processWeaponSection(false, rightAftFilter, false);
        double weaponsBVRightBroadside = processWeaponSection(false, rightBroadsideFilter, false);
        double weaponsBVLeftBroadside = processWeaponSection(false, leftBroadsideFilter, false);
        bvPerArc.put(BVLOC_NOSE, weaponsBVFront);
        bvPerArc.put(BVLOC_AFT, weaponsBVRear);
        bvPerArc.put(BVLOC_LEFT_BROADSIDE, weaponsBVLeftBroadside);
        bvPerArc.put(BVLOC_RIGHT_BROADSIDE, weaponsBVRightBroadside);
        final double maxBV = bvPerArc.values().stream().mapToDouble(bv -> bv).max().orElse(0);
        for (Map.Entry<Integer, Double> entry : bvPerArc.entrySet()) {
            if (entry.getValue() == maxBV) {
                nominalNoseLocation = entry.getKey();
                break;
            }
        }
        bvPerArc.put(BVLOC_LEFT, weaponsBVLeft);
        bvPerArc.put(BVLOC_RIGHT, weaponsBVRight);
        bvPerArc.put(BVLOC_LEFT_AFT, weaponsBVAftLeft);
        bvPerArc.put(BVLOC_RIGHT_AFT, weaponsBVAftRight);
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
                WeaponMounted key = collectedWeapons.keySet().stream()
                        .filter(wp -> canBeSummed(weapon, wp)).findFirst().orElse(weapon);
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
        switch (bvLocation) {
            case BVLOC_NOSE:
                return entity.getLocationName(Jumpship.LOC_NOSE);
            case BVLOC_LEFT:
                return entity.getLocationName(Jumpship.LOC_FLS);
            case BVLOC_LEFT_AFT:
                return entity.getLocationName(Jumpship.LOC_ALS);
            case BVLOC_AFT:
                return entity.getLocationName(Jumpship.LOC_AFT);
            case BVLOC_RIGHT_AFT:
                return entity.getLocationName(Jumpship.LOC_ARS);
            case BVLOC_RIGHT:
                return entity.getLocationName(Jumpship.LOC_FRS);
            case BVLOC_LEFT_BROADSIDE:
                return entity.getLocationName(Warship.LOC_LBS);
            case BVLOC_RIGHT_BROADSIDE:
                return entity.getLocationName(Warship.LOC_RBS);
        }
        return "Error: Unexpected location value.";
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