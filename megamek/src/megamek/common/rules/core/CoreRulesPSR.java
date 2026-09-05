package megamek.common.rules.core;


/*
 * Copyright (C) 2004-2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.Entity;
import megamek.common.units.MekWithArms;
import megamek.common.units.QuadMek;

import java.util.ArrayList;
import java.util.List;

/* This class is for Core Rules that involve PSR checks and modifiers
 */
public class CoreRulesPSR extends RulesPSR {
    /**
     * {@inheritDoc}
     * Called from Entity
     */
    @Override
    public void checkRunningWithDamage(Entity entity, PilotingRollData roll, int gyroDamage,
          EntityMovementType overallMoveType, int distance) {
        if (entity.getGyroType() == Mek.GYRO_HEAVY_DUTY) {
            gyroDamage = 0;
        }
        boolean bRunningAndCanFall =
              (((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType.MOVE_SPRINT)) &&
              entity.canFall());
        if (bRunningAndCanFall && distance > 0) {
            if (((entity instanceof MekWithArms && ((Mek) entity).countBadLegs() > 0) ||
                  (entity instanceof QuadMek && ((QuadMek) entity).countBadLegs() > 2))) {
                // Running with a leg destroyed
                roll.append(new PilotingRollData(entity.getId(), 0, "running with destroyed leg"));
            } else if ((gyroDamage > 0) || entity.hasHipCrit()) {
                // append the reason modifier
                roll.append(new PilotingRollData(entity.getId(), 0, "running with damaged hip actuator or gyro"));
            } else {
                roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to run with damage");
            }
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity cannot fall or did not move at least 1 hex");
        }
    }

    /**
     * {@inheritDoc}
     * Trying to stand is a -1 modifier Core p.111
     */
    @Override
    public void standing(PilotingRollData roll) {
        roll.addModifier(-1, "trying to stand");
    }

    /**
     * {@inheritDoc}
     * No change of facing after a fall in Core p.115
     */
    @Override
    public void facingChangeAfterFall(Entity entity, int facing) {}
    
    /**
     * {@inheritDoc}
     * Leg PSR numbers changed. Core p.90, 93
     */
    @Override
    public void legDamageModifiers(MekWithArms unit, final PilotingRollData roll, final boolean toLegDamage) {
        for (int loc : List.of(Mek.LOC_RIGHT_LEG, Mek.LOC_LEFT_LEG)) {
            if (unit.isLocationBad(loc)) {
                roll.addModifier(4, unit.getLocationName(loc) + " destroyed");
                
            } else {
                if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    roll.addModifier(1, unit.getLocationName(loc) + " Hip Actuator destroyed");
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
            }
        }
    }

    /**
     * {@inheritDoc}
     * Reduce PSR rolls for actuator hits to the highest per leg in a turn. Core p.93
     */
    @Override
    public void checkLegActuatorPsrRolls(Game game, Entity entity) {
        ArrayList<PilotingRollData> pilotRolls = game.getPSRsForEntity(entity);
        ArrayList<PilotingRollData> rollsToRemove = new ArrayList<>();
        ArrayList<PilotingRollData> rollLL = new ArrayList<>();
        ArrayList<PilotingRollData> rollRL = new ArrayList<>();
        ArrayList<PilotingRollData> rollCL = new ArrayList<>();
        ArrayList<PilotingRollData> rollFLL = new ArrayList<>();
        ArrayList<PilotingRollData> rollFRL = new ArrayList<>();
        
        // Organize all rolls by location
        for (PilotingRollData roll : pilotRolls) {
                // Assign rolls to locations
                switch (roll.getLocation()) {
                    case Mek.LOC_RIGHT_LEG:
                        rollRL.add(roll);
                        break;
                    case Mek.LOC_LEFT_LEG:
                        rollLL.add(roll);
                        break;
                    case Mek.LOC_LEFT_ARM:
                        // Quads!
                        rollFLL.add(roll);
                        break;
                    case Mek.LOC_RIGHT_ARM:
                        // Quads!
                        rollFRL.add(roll);
                        break;
                    case Mek.LOC_CENTER_LEG:
                        rollCL.add(roll);
                }
        }
        
        if (!rollLL.isEmpty()) {
            rollRemoveHighest(rollLL);
            if (!rollLL.isEmpty()) { rollsToRemove.addAll(rollLL); }
        }
        if (!rollRL.isEmpty()) {
            rollRemoveHighest(rollRL);
            if (!rollRL.isEmpty()) { rollsToRemove.addAll(rollRL); }
        }
        if (!rollFLL.isEmpty()) {
            rollRemoveHighest(rollFLL);
            if (!rollFLL.isEmpty()) { rollsToRemove.addAll(rollFLL); }
        }
        if (!rollFRL.isEmpty()) {
            rollRemoveHighest(rollFRL);
            if (!rollFRL.isEmpty()) { rollsToRemove.addAll(rollFRL); }
        }
        if (!rollCL.isEmpty()){
            rollRemoveHighest(rollCL);
            if (!rollCL.isEmpty()) { rollsToRemove.addAll(rollCL); }
        }
        
        if (!rollsToRemove.isEmpty()) {
            game.removePSRsByArray(rollsToRemove);
        }
    }
    
    /**
     * {@inheritDoc}
     * Remove the highest roll from the list
     */
    @Override
    public void rollRemoveHighest(ArrayList<PilotingRollData> rollList) {
        if (rollList.isEmpty()) {
            return;
        }
        // If there is only one roll, remove it and early exit
        if (rollList.size() == 1) {
            rollList.remove(rollList.getFirst());
            return;
        }
        
        // Find the highest value by iterating the list, and remove it. If nothing is higher than 0, it removes the 
        // first entry
        int highest = 0;
        int highestValue = 0;
        for (int index = 0; index < rollList.size(); index++) {
            if (rollList.get(index).getValue() > highestValue) {
                highest = index;
                highestValue = rollList.get(index).getValue();
            }
        }
        rollList.remove(highest);
    }
    
    /**
     * {@inheritDoc}
     * Do we do Foot Actuator PSRs? No, not in core.
     */
    @Override
    public boolean getFootActuatorPsr() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     * What is the hip hit penalty? Core p.99
     */
    @Override
    public int getHipPenalty() {
        return 1;
    }
    
    /**
     * {@inheritDoc}
     * Gyro hit modifiers
     */
    @Override
    public int getGyroModifier(int gyroHits, int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY && gyroHits <4) {
            return gyroHits;
        }
        return 2;
    }

    /**
     * {@inheritDoc}
     * Leg destroyed is +4. Core p.90
     */
    @Override
    public int getLegDestroyedModifier() { return 4; }

    /**
     * {@inheritDoc}
     * HD Gyro hits. Core p.98
     */
    @Override
    public void handleHDGyroHits(Game game, Entity en, int actualGyroHits) {
        if (actualGyroHits == 4) {
            game.addPSR(new PilotingRollData(en.getId(),TargetRoll.AUTOMATIC_FAIL, 1, "Gyro Destroyed"));
            en.setHullDown(false);
        }
    }

    /**
     * {@inheritDoc}
     * Walking into water does not cause a PSR. Core p.45, 51
     */
    @Override
    public boolean psrForWaterEntry(EntityMovementType overallMoveType) {
        return (overallMoveType == EntityMovementType.MOVE_WALK) ? false : true;
    }
    
    /**
     * {@inheritDoc}
     * Successful DFA is PSR+2 Core p.80
     */
    @Override
    public int getSuccessfulDFAModifier() {
        return 2;
    }
    
    /**
     * {@inheritDoc}
     * Successful club causes PSR . Core p.79
     */
    @Override
    public void clubImpact(final Game game,
          final Entity entity) {
        game.addPSR(new PilotingRollData(entity.getId(), 0, "was clubbed"));
    }

    /**
     * {@inheritDoc}
     * Special gyro jump modifier. Core p.98
     */
    @Override
    public int getGyroJumpModifier(final int gyroHits, final int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY || gyroType == Mek.GYRO_SUPERHEAVY) {
            return switch (gyroHits) {
                case 1 -> 1;
                case 3 -> -1;
                default -> 0;
            };
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * do you need a PSR for walking with a leg destroyed? Yes. Core p.90
     */
    @Override
    public PilotingRollData checkWalkWithLegDestroyed(Entity entity, EntityMovementType overallMoveType,
          int hexesMoved) {
        PilotingRollData roll = entity.getBasePilotingRoll(overallMoveType);
        if (entity.hasBadLegs() && overallMoveType == EntityMovementType.MOVE_WALK && hexesMoved > 0) {
            roll.append(new PilotingRollData(entity.getId(), 0, "walking with destroyed leg and moved at least 1 hex"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: does not apply");
        }
        entity.addPilotingModifierForTerrain(roll);
        return roll;
    }
    
    /**
     * {@inheritDoc}
     *
     * Falls into and in water get half damage. Core p.115
     * */
    @Override
    public FallDamageInWater reduceFallDamageIntoWater(int damage, int waterDepth, int fallHeight, double weight) {
        int waterDamage = (int) (((int) Math.round(weight / 10.0) * (fallHeight + waterDepth + 1)) / 2.0);
        int newDamage = 0;        
        return new FallDamageInWater(newDamage, waterDamage);
    }
}
