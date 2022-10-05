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

import java.util.function.Predicate;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class CombatVehicleBVCalculator extends BVCalculator {

    CombatVehicleBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected int getUmuTMM() {
        return 0;
    }

    @Override
    protected int getRunningTMM() {
        int tmmRan = Compute.getTargetMovementModifier(
                entity.getRunMP(false, true, true), entity instanceof VTOL,
                entity instanceof VTOL, entity.getGame()).getValue();
        tmmRan += (entity.hasStealth()) ? 2 : 0;
        tmmRan += (entity.getMovementMode() == EntityMovementMode.WIGE) ? 1 : 0;
        return tmmRan;
    }

    @Override
    protected int getJumpingTMM() {
        if (jumpMP == 0) {
            return 0;
        }
        int tmmJumped = Compute.getTargetMovementModifier(entity.getJumpMP(), true, false, entity.getGame()).getValue();
        tmmJumped += (entity.hasStealth()) ? 2 : 0;
        tmmJumped += (entity.getMovementMode() == EntityMovementMode.WIGE) ? 1 : 0;
        return tmmJumped;
    }

    @Override
    protected void processTypeModifier() {
        double typeModifier;
        switch (entity.getMovementMode()) {
            case TRACKED:
                typeModifier = 0.9;
                break;
            case WHEELED:
                typeModifier = 0.8;
                break;
            case HOVER:
            case VTOL:
            case WIGE:
                typeModifier = 0.7;
                break;
            default:
                typeModifier = 0.6;
        }

        if (!entity.isSupportVehicle()) {
            for (Mounted m : entity.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_FULLY_AMPHIBIOUS)) {
                    typeModifier += 0.2;
                } else if (m.getType().hasFlag(MiscType.F_LIMITED_AMPHIBIOUS)
                        || m.getType().hasFlag(MiscType.F_DUNE_BUGGY)
                        || m.getType().hasFlag(MiscType.F_FLOTATION_HULL)
                        || m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)
                        || m.getType().hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                    typeModifier += 0.1;
                }
            }
        }
        bvReport.addLine("Type Modifier:",
                formatForReport(defensiveValue) + " x " + formatForReport(typeModifier),
                "= " + formatForReport(defensiveValue * typeModifier));
        defensiveValue *= typeModifier;
    }

    @Override
    protected Predicate<Mounted> rearWeaponFilter() {
        return weapon -> weapon.getLocation() == rearLocation();
    }

    @Override
    protected Predicate<Mounted> frontWeaponFilter() {
        return weapon -> weapon.getLocation() == Tank.LOC_FRONT;
    }

    private int rearLocation() {
        if (entity instanceof SuperHeavyTank) {
            return SuperHeavyTank.LOC_REAR;
        } else if (entity instanceof LargeSupportTank) {
            return LargeSupportTank.LOC_REAR;
        } else {
            return Tank.LOC_REAR;
        }
    }

    @Override
    protected void processWeight() {
        offensiveValue += entity.getWeight() / 2;
        bvReport.addLine("Weight:",
                "+ " + formatForReport(entity.getWeight()) + " x 0.5 ",
                "= " + formatForReport(offensiveValue));
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        int runMP = entity.getRunMP(false, true, true);
        int jumpMP = entity.getJumpMP(false);
        if (entity.getMovementMode().isTrain()) {
            runMP = entity.getWalkMP(false, true, true);
        }
        // trailers have original run MP of 0, but should count at 1 for speed factor calculation
        if (entity.getOriginalRunMP() == 0) {
            runMP = 1;
        }
        return runMP + (int) Math.round(jumpMP / 2.0);
    }
}