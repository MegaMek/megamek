package megamek.common.rules.core;


/*
 * Copyright (C) 2026 James Magnan (bmazur@sev.org)
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
import megamek.common.Messages;
import megamek.common.annotations.Nullable;
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
    // Called from Entity
    public void checkRunningWithDamage(Entity entity, PilotingRollData roll, int gyroDamage,
          EntityMovementType overallMoveType, int distance) {
        if (entity.getGyroType() == Mek.GYRO_HEAVY_DUTY) {
            gyroDamage = 0;
        }
        boolean bRunningAndCanFall =
              (((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType.MOVE_SPRINT)) &&
              entity.canFall());
        if (bRunningAndCanFall && ((gyroDamage > 0) || entity.hasHipCrit())) {
            // append the reason modifier
            roll.append(new PilotingRollData(entity.getId(), 0, "running with damaged hip actuator or gyro"));
        } else if (((entity instanceof MekWithArms && ((Mek) entity).countBadLegs() > 0) ||
              (entity instanceof QuadMek && ((QuadMek) entity).countBadLegs() > 2)) &&
        distance > 0 && bRunningAndCanFall) {
            // Running with a leg destroyed
            roll.append(new PilotingRollData(entity.getId(), 0, "running with destroyed leg"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to run with damage");
        }
    }

    // Trying to stand is a -1 modifier Core p.111
    public void standing(PilotingRollData roll) {
        roll.addModifier(-1, "trying to stand");
    }

    // No change of facing after a fall in Core p.115
    public void facingChangeAfterFall(Entity entity, int facing) {};
    
    // Leg PSR numbers changed. Core p.90, 93
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

    // Reduce PSR rolls for actuator hits to the highest per leg in a turn. Core p.93
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
        
        if (rollLL.size() > 0) {
            rollRemoveHighest(rollLL);
            if (rollLL.size() > 0) { rollsToRemove.addAll(rollLL); }
        }
        if (rollRL.size()>0) {
            rollRemoveHighest(rollRL);
            if (rollLL.size() > 0) { rollsToRemove.addAll(rollRL); }
        }
        if (rollFLL.size()>0) {
            rollRemoveHighest(rollFLL);
            if (rollLL.size() > 0) { rollsToRemove.addAll(rollFLL); }
        }
        if (rollFRL.size()>0) {
            rollRemoveHighest(rollFRL);
            if (rollLL.size() > 0) { rollsToRemove.addAll(rollFRL); }
        }
        if (rollCL.size()>0){
            rollRemoveHighest(rollCL);
            if (rollLL.size() > 0) { rollsToRemove.addAll(rollCL); }
        }
        
        if (rollsToRemove.size() > 0) {
            game.removePSRsByArray(rollsToRemove);
        }
    }
    
    // Remove the highest roll from the list
    public void rollRemoveHighest(ArrayList<PilotingRollData> rollList) {
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
    
    public void hitActuator(final Game game, Entity entity, int loc, int hitPart) {
        String psrText = Game.rulesManager.getRulesCharts().getLocationName(loc,(entity instanceof QuadMek));
        if (hitPart == Mek.ACTUATOR_FOOT) {
            psrText += Messages.getString("ActuatorHits.Foot");
        }
        if (hitPart == Mek.ACTUATOR_HIP) {
            psrText += Messages.getString("ActuatorHits.Hip");
        } 
        psrText += Messages.getString("ActuatorHits.Actuator");
        
        int psrPenalty = 1;
        
        if (hitPart == Mek.ACTUATOR_HIP) {
            psrPenalty = getHipPenalty();
        }
        
        if (getFootActuatorPsr() && hitPart == Mek.ACTUATOR_FOOT) {
            game.addPSR(new PilotingRollData(entity.getId(), psrPenalty, psrText, loc));
        } else if (hitPart != Mek.ACTUATOR_FOOT) {
            game.addPSR(new PilotingRollData(entity.getId(), psrPenalty, psrText, loc));
        }
    }
    
    // Do we do Foot Actuator PSRs? No, not in core.
    public boolean getFootActuatorPsr() {
        return false;
    }
    
    // What is the hip hit penalty? Core p.99
    public int getHipPenalty() {
        return 1;
    }
    
    // Gyro hit modifiers
    public int getGyroModifier(int gyroHits, int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY && gyroHits <4) {
            return gyroHits;
        }
        return 2;
    }

    // Leg destroyed is +4. Core p.90
    public int getLegDestroyedModifier() { return 4; }

    // HD Gyro hits. Core p.98
    public void handleHDGyroHits(Game game, Entity en, int actualGyroHits) {
        if (actualGyroHits == 4) {
            game.addPSR(new PilotingRollData(en.getId(),TargetRoll.AUTOMATIC_FAIL, 1, "Gyro Destroyed"));
            en.setHullDown(false);
        }
    }

    // Walking into water does not cause a PSR. Core p.45, 51
    public boolean psrForWaterEntry(EntityMovementType overallMoveType) {
        return (overallMoveType == EntityMovementType.MOVE_WALK) ? false : true;
    }
}
