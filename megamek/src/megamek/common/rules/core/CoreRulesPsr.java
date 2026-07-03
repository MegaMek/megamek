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
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesPsr;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.Entity;
import megamek.common.units.MekWithArms;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/* This class is for Core Rules that involve PSR checks and modifiers
 */
public class CoreRulesPsr extends RulesPsr {
    // Called from Entity
    public void checkRunningWithDamage(Entity e, PilotingRollData r, int gyroDamage, EntityMovementType overallMoveType) {
        if (e.getGyroType() == Mek.GYRO_HEAVY_DUTY) {
            gyroDamage = 0;
        }
        if (((overallMoveType == EntityMovementType.MOVE_RUN) || (overallMoveType == EntityMovementType.MOVE_SPRINT)) &&
              e.canFall() &&
              ((gyroDamage > 0) || e.hasHipCrit())) {
            // append the reason modifier
            r.append(new PilotingRollData(e.getId(), 0, "running with damaged hip actuator or gyro"));
        } else {
            r.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to run with damage");
        }
    }

    // Trying to stand is a -1 modifier Core p.111
    public void standing(PilotingRollData r) {
        r.addModifier(-1, "trying to stand");
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
    public void checkLegActuatorPsrRolls(Vector<PilotingRollData> pilotRolls, Entity entity) {
        PilotingRollData roll;
        Vector<Integer> rollsToRemove = new Vector<>();
        Vector<Integer> rollTarget = new Vector<>();
        Vector<Integer> rollLocation = new Vector<>();
        Vector<Integer> saveRolls = new Vector<>();

        // first, find all the rolls belonging to the target entity
        // Locations are: 1 = left leg, 2 = right leg, 3 = front left leg, 4 = front right leg, 5 = center leg
        for (int i = 0; i < pilotRolls.size(); i++) {
            roll = pilotRolls.elementAt(i);
            if (roll.getEntityId() == entity.getId()) {
                // This is the critical part.
                if (roll.getDesc().equals("left leg actuator hit") || roll.getDesc().equals("left hip actuator hit")) {
                    rollTarget.addElement(roll.getValue());
                    rollLocation.addElement(1);
                    rollsToRemove.addElement(i);
                } else if (roll.getDesc().equals("right leg actuator hit") || roll.getDesc()
                      .equals("right hip actuator hit")) {
                    rollTarget.addElement(roll.getValue());
                    rollLocation.addElement(2);
                    rollsToRemove.addElement(i);
                } else if (roll.getDesc().equals("front left leg actuator hit") || roll.getDesc().equals("front left "
                      + "hip actuator hit")) {
                    rollTarget.addElement(roll.getValue());
                    rollLocation.addElement(3);
                    rollsToRemove.addElement(i);
                } else if (roll.getDesc().equals("front right leg actuator hit") || roll.getDesc().equals("front "
                      + "right hip actuator hit")) {
                    rollTarget.addElement(roll.getValue());
                    rollLocation.addElement(4);
                    rollsToRemove.addElement(i);
                } else if (roll.getDesc().equals("center leg actuator hit") || roll.getDesc().equals("center hip "
                      + "actuator hit")) {
                    rollTarget.addElement(roll.getValue());
                    rollLocation.addElement(5);
                    rollsToRemove.addElement(i);
                }
            }
        }

        if (rollsToRemove.size() > 1) {
            int saveEntry = 0;
            int highTarget = 0;
            boolean entrySaved = false;
            // check which roll target is highest
            for (int location = 1; location < 6; location++) {
                highTarget = 0;
                saveEntry = 0;
                entrySaved = false;
                for (int i = 0; i < rollTarget.size(); i++) {
                    if ((rollTarget.elementAt(i) > highTarget) && (rollLocation.elementAt(i) == location)) {
                        saveEntry = i;
                        entrySaved = true;
                        highTarget = rollTarget.elementAt(i);
                    }
                }
                if (entrySaved) {
                    saveRolls.addElement(rollsToRemove.elementAt(saveEntry));
                }
            }
            // Remove the saved element from our removal list
            for (int i = saveRolls.size() - 1; i > -1; i--) {
                rollsToRemove.removeElementAt(saveRolls.elementAt(i));
            }

            // now, clear out remaining rolls from the PSRs
            for (int i = rollsToRemove.size() - 1; i > -1; i--) {
                pilotRolls.removeElementAt(rollsToRemove.elementAt(i));
            }
        }
    }
}
