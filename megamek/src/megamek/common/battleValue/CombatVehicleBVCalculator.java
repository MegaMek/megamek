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

import megamek.common.MPCalculationSetting;
import megamek.common.compute.Compute;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.SuperHeavyTank;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;

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
        if (runMP == 0) {
            return 0;
        }
        int tmmRan = Compute.getTargetMovementModifier(runMP, entity instanceof VTOL,
              entity instanceof VTOL || (entity.getMovementMode() == EntityMovementMode.WIGE),
              entity.getGame()).getValue();
        tmmRan += (entity.hasStealth()) ? 2 : 0;
        return tmmRan;
    }

    @Override
    protected int getJumpingTMM() {
        if (jumpMP == 0) {
            return 0;
        }
        int tmmJumped = Compute.getTargetMovementModifier(jumpMP, true, false, entity.getGame()).getValue();
        tmmJumped += (entity.hasStealth()) ? 2 : 0;
        return tmmJumped;
    }

    @Override
    protected void processTypeModifier() {
        double typeModifier = switch (entity.getMovementMode()) {
            case TRACKED -> 0.9;
            case WHEELED -> 0.8;
            case HOVER, VTOL, WIGE -> 0.7;
            default -> 0.6;
        };

        if (!entity.isSupportVehicle()) {
            for (Mounted<?> m : entity.getMisc()) {
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
    protected Predicate<Mounted<?>> rearWeaponFilter() {
        return weapon -> weapon.getLocation() == rearLocation();
    }

    @Override
    protected Predicate<Mounted<?>> frontWeaponFilter() {
        return weapon -> weapon.getLocation() == Tank.LOC_FRONT;
    }

    @Override
    protected boolean isNominalRear(Mounted<?> weapon) {
        return (switchRearAndFront ^ rearWeaponFilter().test(weapon)) && !(weapon.getLocation() == Tank.LOC_TURRET)
              && !(weapon.getLocation() == Tank.LOC_TURRET_2);
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
        int runMP = entity.getRunMP(MPCalculationSetting.BV_CALCULATION);
        int jumpMP = entity.getJumpMP(MPCalculationSetting.BV_CALCULATION);
        if (entity.getMovementMode().isTrain()) {
            runMP = entity.getWalkMP(MPCalculationSetting.BV_CALCULATION);
        }
        // trailers have original run MP of 0, but should count at 1 for speed factor
        // calculation
        if (entity.getOriginalRunMP() == 0) {
            runMP = 1;
        }
        return runMP + (int) Math.round(jumpMP / 2.0);
    }
}
