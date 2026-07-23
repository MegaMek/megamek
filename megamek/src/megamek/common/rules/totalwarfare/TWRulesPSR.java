package megamek.common.rules.totalwarfare;


/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.core.CoreRulesPSR;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.MekWithArms;
import java.util.List;

public class TWRulesPSR extends CoreRulesPSR {

    public void checkRunningWithDamage(Entity entity, PilotingRollData roll, int gyroDamage,
          EntityMovementType overallMoveType, int distance) {
        if (entity.getGyroType() == Mek.GYRO_HEAVY_DUTY) {
            gyroDamage--; // HD gyro ignores 1st damage
        }
        if (((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType.MOVE_SPRINT)) &&
              entity.canFall() &&
              ((gyroDamage > 0) || entity.hasHipCrit())) {
            // append the reason modifier
            roll.append(new PilotingRollData(entity.getId(), 0, "running with damaged hip actuator or gyro"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to run with damage");
        }
    }

    // No modifier to stand
    @Override
    public void standing(PilotingRollData roll) { }

    @Override
    public void facingChangeAfterFall(Entity entity, int facing){
        entity.setFacing((entity.getFacing() + (facing)) % 6);
        entity.setSecondaryFacing(entity.getFacing());
    }

    @Override
    public void legDamageModifiers(MekWithArms unit, final PilotingRollData roll, final boolean toLegDamage) {
        for (int loc : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG)) {
            if (unit.isLocationBad(loc)) {
                roll.addModifier(5, unit.getLocationName(loc) + " destroyed");
            } else {
                if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, unit.getLocationName(loc) + " Hip Actuator destroyed");
                    if (toLegDamage) {
                        continue;
                    }
                }
                if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_LEG, loc) > 0) {
                    roll.addModifier(1, unit.getLocationName(loc) + " Upper Leg Actuator destroyed");
                }
                if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_LEG, loc) > 0) {
                    roll.addModifier(1, unit.getLocationName(loc) + " Lower Leg Actuator destroyed");
                }
                if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_FOOT, loc) > 0) {
                    roll.addModifier(1, unit.getLocationName(loc) + " Foot Actuator destroyed");
                }
            }
        }
    }

    // We do not reduce leg actuator rolls in TW
    @Override
    public void checkLegActuatorPsrRolls(Game game, Entity entity) {};

    @Override
    public boolean getFootActuatorPsr() {
        return true;
    }

    @Override
    public int getHipPenalty() {
        return 1;
    }

    // Gyro hit modifiers
    @Override
    public int getGyroModifier(int gyroHits, int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY && gyroHits == 1) {
            return gyroHits;
        }
        return 3;
    }

    // Leg destroyed in +5.
    @Override
    public int getLegDestroyedModifier() { return 5; }

    // HD Gyro hits cause issues
    @Override
    public void handleHDGyroHits(Game game, Entity en, int actualGyroHits) {
        switch (actualGyroHits) {
            case 4:
            case 3:
                // 3rd hit to HD gyro (gyro destroyed)
                game.addPSR(new PilotingRollData(en.getId(),
                      TargetRoll.AUTOMATIC_FAIL,
                      1,
                      "gyro destroyed"));
                en.setHullDown(false);
                break;
            case 2:
                // 2nd hit to HD gyro (PSR +3, same as standard gyro 1st hit)
                game.addPSR(new PilotingRollData(en.getId(), 3, "gyro hit"));
                break;
            case 1:
                // 1st hit to HD gyro: NO PSR per errata (just +1 modifier to future PSRs)
                // No action needed
                break;
            default:
                // Ignore if >4 hits (auto-fail already happened)
                break;
        }
    }

    // Entering water causes a PSR
    @Override
    public boolean psrForWaterEntry(EntityMovementType overallMoveType) {
        return true;
    }
    
    // Successful DFA PSR is +4
    @Override
    public int getSuccessfulDFAModifier() {
        return 4;
    }
}
