/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.Mounted;
import megamek.common.SpaceStation;

import java.util.function.Predicate;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class JumpShipBVCalculator extends LargeAeroBVCalculator {

    JumpShipBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected void processStructure() {
        String calculation = "+ " + aero.getSI() + " x 20";
        defensiveValue += aero.getSI() * 20;
        bvReport.addLine("Structural Integrity:", calculation, "= " + formatForReport(defensiveValue));
    }

    @Override
    protected double armorFactor() {
        return 25;
    }

    @Override
    protected Predicate<Mounted> frontWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_NOSE);
    }

    @Override
    protected Predicate<Mounted> rearWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_AFT);
    }

    @Override
    protected Predicate<Mounted> leftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_FLS);
    }

    @Override
    protected Predicate<Mounted> leftAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_ALS);
    }

    @Override
    protected Predicate<Mounted> rightWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_FRS);
    }

    @Override
    protected Predicate<Mounted> rightAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_ARS);
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
        } else {
            return BVLOC_RIGHT;
        }
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
        }
        return "Error: Unexpected location value.";
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        if (entity instanceof SpaceStation) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    protected void processTypeModifier() {
        double typeModifier = (entity instanceof SpaceStation) ? 0.7 : 0.75;
        bvReport.addLine("Type Modifier:",
                formatForReport(defensiveValue) + " x " + formatForReport(typeModifier),
                "= " + formatForReport(defensiveValue * typeModifier));
        defensiveValue *= typeModifier;
    }
}