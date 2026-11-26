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

import java.util.ArrayList;

import megamek.common.CriticalSlot;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesPSR;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;

/* This class is for Core Rules that involve PSR checks and modifiers
 */
public class CoreRulesPSR extends RulesPSR {
    /**
     * {@inheritDoc}
     * Running at least one hex forces one PSR for each gyro hit, hip actuator hit or destroyed leg, at most one per leg
     * (Core pp.45, 110-111), with a quad's leg damage in biped terms (Core p.238).
     */
    @Override
    public int checkRunningWithDamage(Entity entity, PilotingRollData roll, int gyroDamage,
          EntityMovementType overallMoveType, int distance) {
        if (entity.getGyroType() == Mek.GYRO_HEAVY_DUTY) {
            gyroDamage = 0;
        }
        boolean bRunningAndCanFall =
              (((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType.MOVE_SPRINT)) &&
              entity.canFall());
        if (!bRunningAndCanFall || (distance <= 0) || !(entity instanceof Mek mek)) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity cannot fall or did not move at least 1 hex");
            return 0;
        }
        int checks = ((gyroDamage > 0) ? 1 : 0) + CoreRulesUnits.legsForcingRunningChecks(mek);
        if (checks == 0) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to run with damage");
            return 0;
        }
        roll.append(new PilotingRollData(entity.getId(), 0, "running with destroyed leg, damaged hip actuator or gyro"));
        return checks;
    }

    /**
     * {@inheritDoc}
     * One PSR for gyro damage and one per leg that is destroyed or has an actuator hit (Core pp.90, 99, 110), with a
     * quad's leg damage in biped terms (Core p.238).
     */
    @Override
    public int landingWithDamageChecks(Entity entity, int gyroHits) {
        if (!(entity instanceof Mek mek)) {
            return singleLandingCheckForDamage(entity, gyroHits);
        }
        return ((gyroHits > 0) ? 1 : 0) + CoreRulesUnits.legsForcingLandingChecks(mek);
    }

    /**
     * {@inheritDoc}
     * A quad's first hip hit has none of the standard effects, so it forces no PSR (Core p.238).
     */
    @Override
    public void hitActuator(final Game game, Entity entity, int loc, int hitPart) {
        if ((hitPart == Mek.ACTUATOR_HIP) && (entity instanceof QuadMek quad) && !hasOtherHitHip(quad, loc)) {
            return;
        }
        super.hitActuator(game, entity, loc, hitPart);
    }

    private boolean hasOtherHitHip(QuadMek quad, int hitLocation) {
        for (int loc = 0; loc < quad.locations(); loc++) {
            // Include hits from this phase, before their critical slots are marked destroyed.
            if ((loc != hitLocation) && quad.locationIsLeg(loc)
                  && !quad.isLocationBad(loc) && !quad.isLocationDoomed(loc)
                  && (quad.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0)) {
                return true;
            }
        }
        return false;
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
     * +4 for a destroyed leg (Core p.90) and +1 for each destroyed hip, upper and lower leg actuator, with no foot
     * modifier (Core pp.110-111). The Tactical Operations leg damage option does not change this.
     */
    @Override
    public void legDamageModifiers(Mek unit, final PilotingRollData roll, final boolean toLegDamage) {
        // A tripod follows the biped rules (Core p.237); a quad ignores its first hip hit (Core p.238)
        boolean quad = unit instanceof QuadMek;
        boolean quadHipIgnored = false;
        for (int loc = 0; loc < unit.locations(); loc++) {
            if (!unit.locationIsLeg(loc)) {
                continue;
            }
            if (unit.isLocationBad(loc)) {
                if (!quad) {
                    roll.addModifier(4, unit.getLocationName(loc) + " destroyed");
                }
            } else {
                // "The modifiers from each destroyed actuator are all applied" (Core p.110)
                if (unit.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP, loc) > 0) {
                    if (quad && !quadHipIgnored) {
                        quadHipIgnored = true;
                    } else {
                        roll.addModifier(1, unit.getLocationName(loc) + " Hip Actuator destroyed");
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
     * Leg actuator damage forces one PSR per leg at a time, at the highest modifier (Core p.110)
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
     * Heavy-duty gyro jump modifier (Core p.98, p.204). A superheavy gyro is not in Core and damages like a standard
     * gyro (IO:AE p.154).
     */
    @Override
    public int getGyroJumpModifier(final int gyroHits, final int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY) {
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
     *
     * Core p.115: a unit falling in or into water falls the levels to the surface plus the water's depth (the given
     * fall height already reaches the bottom), taking 1 damage per 10 tons (rounding up) times the levels plus 1,
     * "divided by 2 (rounding down)". A unit standing in water falls 0 levels.
     */
    @Override
    public FallDamageInWater reduceFallDamageIntoWater(int damage, int waterDepth, int fallHeight, double weight) {
        int waterDamage = ((int) Math.ceil(weight / 10.0) * (Math.max(0, fallHeight) + 1)) / 2;
        return new FallDamageInWater(0, waterDamage);
    }

    /**
     * {@inheritDoc}
     * Core does not apply any modifiers for water level. Core p.111, p.122
     *
     * @param waterLevel
     * @return
     */
    @Override
    public int getWaterLevelModifier(final int waterLevel) {
        return 0;
    }
}
