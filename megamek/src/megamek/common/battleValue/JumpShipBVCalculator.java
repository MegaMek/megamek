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

import java.util.function.Predicate;

import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SpaceStation;

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
    protected Predicate<Mounted<?>> frontWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_NOSE);
    }

    @Override
    protected Predicate<Mounted<?>> rearWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_AFT);
    }

    @Override
    protected Predicate<Mounted<?>> leftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_FLS);
    }

    @Override
    protected Predicate<Mounted<?>> leftAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_ALS);
    }

    @Override
    protected Predicate<Mounted<?>> rightWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_FRS);
    }

    @Override
    protected Predicate<Mounted<?>> rightAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Jumpship.LOC_ARS);
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
        } else {
            return BV_LOC_RIGHT;
        }
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
            default -> "Error: Unexpected location value.";
        };
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
