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
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesPSR;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.MekWithArms;

import java.util.ArrayList;
import java.util.List;

public class TWRulesPSR extends RulesPSR {

    /**
     * Entity calls this when trying to run with damage.
     *
     * @param entity the entity attempting to run with damage
     * @param roll the piloting roll data
     * @param gyroDamage the amount of gyro damage
     * @param overallMoveType the type of overall movement
     * @param distance the distance traveled
     */
    @Override
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

    /**
     * No modifier for standing
     *
     * @param roll the piloting roll data 
     */
    @Override
    public void standing(PilotingRollData roll) {}

    /**
     * Do we need to change facing when we fall? Yes
     *
     * @param entity the entity that fell
     * @param facing the new facing direction
     */
    @Override
    public void facingChangeAfterFall(Entity entity, int facing) {
        entity.setFacing((entity.getFacing() + (facing)) % 6);
        entity.setSecondaryFacing(entity.getFacing());
    }

    /**
     * Apply leg damage modifiers to PSR.
     *
     * @param unit the unit with arms to check
     * @param roll the piloting roll data to modify
     * @param toLegDamage true if applying leg damage
     */
    @Override
    public void legDamageModifiers(MekWithArms unit, final PilotingRollData roll, final boolean toLegDamage) {
        for (int loc : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG)) {
            if (unit.isLocationBad(loc)) {
                roll.addModifier(5, unit.getLocationName(loc) + " destroyed");
            } else {
                if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(2, unit.getLocationName(loc) + " Hip Actuator destroyed");
                    if (!toLegDamage) {
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

    /**
     * Do we need to reduce potential PSR rolls?
     * No, TW does not do this
     *
     * @param game the game instance
     * @param entity the entity to check
     */
    @Override
    public void checkLegActuatorPsrRolls(Game game, Entity entity) {}

    /**
     * Foot Actuators cause PSR when hit
     *
     * @return true if foot actuator damage causes PSR
     */
    @Override
    public boolean getFootActuatorPsr() {
        return true;
    }

    /**
     * Hip Penalty is 2.
     *
     * @return the hip penalty modifier
     */
    @Override
    public int getHipPenalty() {
        return 2;
    }

    /**
     * What is the penalty for the gyro.
     * three if it is normal, 1 if heavy duty and only a single hit
     *
     * @param gyroHits the number of gyro hits
     * @param gyroType the type of gyro
     * @return the gyro modifier
     */
    @Override
    public int getGyroModifier(int gyroHits, int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY && gyroHits == 1) {
            return gyroHits;
        }
        return 3;
    }

    /**
     * What is the penalty for leg destroyed? +5
     *
     * @return the leg destroyed modifier
     */
    @Override
    public int getLegDestroyedModifier() {return 5;}

    /**
     * Handle HD Gyro Hits.
     *
     * @param game the game instance
     * @param en the entity with HD gyro
     * @param actualGyroHits the actual number of gyro hits
     */
    @Override
    public void handleHDGyroHits(Game game, Entity en, int actualGyroHits) {
        switch (actualGyroHits) {
            case 4:
                // gyro already destroyed. Technically this should never occur
                // But if it can fall (which is a gate before this call), then yeah
                // it should fall
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
    /**
     * When entering water, we trigger a PSR.
     *
     * @param overallMoveType the type of overall movement
     * @return always is true
     */
    @Override
    public boolean psrForWaterEntry(EntityMovementType overallMoveType) {
        return true;
    }

    /**
     * What is the PSR mod for a successful DFA.
     * It is +4
     *
     * @return the modifier for successful Death from Above
     */
    @Override
    public int getSuccessfulDFAModifier() {
        return 4;
    }

    /**
     * Does a club impact cause a PSR? No. nothing
     *
     * @param game the game instance
     * @param entity the entity being impacted by a club
     */
    @Override
    public void clubImpact(Game game, Entity entity) {}

    /**
     * No Special gyro jump modifier.
     * 
     * @param gyroHits the number of gyro hits
     * @param gyroType the type of gyro
     * @return the gyro jump modifier
     */
    @Override
    public int getGyroJumpModifier(final int gyroHits, final int gyroType) {
        return 0;
    }
    
    /**
     * We don't care about walking meks with legs destroyed
     *
     * @param entity the entity attempting to walk
     * @param overallMoveType the type of overall movement
     * @param hexesMoved the number of hexes moved
     * @return the piloting roll data for the PSR, or null if no PSR required
     */
    @Override
    public PilotingRollData checkWalkWithLegDestroyed(Entity entity, EntityMovementType overallMoveType,
          int hexesMoved) {
        PilotingRollData roll = entity.getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: does not apply");
        entity.addPilotingModifierForTerrain(roll);
        return roll;
    }

    /**
     * Calculate the damage a falling unit in water receives
     *
     * @param damage how much damage is it supposed to take
     * @param waterDepth the depth of the water
     * @param fallHeight how big was the fall
     * @param weight how much does the unit weigh
     * @return the modified damage
     */
    @Override
    public FallDamageInWater reduceFallDamageIntoWater(int damage, int waterDepth, int fallHeight, double weight) {
        damage /= 2;
        int waterDamage = ((int) Math.round(weight / 10.0) * (waterDepth + 1)) / 2;

        if ((waterDepth >= fallHeight) && ((waterDepth != 0) || (fallHeight != 0))) {
            damage = 0;
            waterDamage = ((int) Math.round(weight / 10.0) * (fallHeight + 1)) / 2;
        }
        return new FallDamageInWater(damage, waterDamage);
    }
}
