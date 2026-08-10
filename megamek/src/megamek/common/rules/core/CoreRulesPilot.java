package megamek.common.rules.core;
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

import megamek.common.Report;
import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesPilot;
import megamek.common.units.Entity;

import java.util.List;
import java.util.Vector;

public class CoreRulesPilot extends RulesPilot {

    /**
     * Helper tool to roll the pilot con checks
     * @param entity Unit in question
     * @param totalHits how many hits has that crewmember taken
     * @param crewPos crew position number
     * @param toughness is toughness on
     * @return return the report
     */
    private Vector<Report> rollPilotHits(Entity entity, int totalHits, int crewPos, boolean toughness) {
        Vector<Report> vDesc = new Vector<>();
        if (totalHits == 0 || entity.getCrew().isDead(crewPos)) {
            return vDesc;
        }
        int rollTarget = Game.rulesManager.getRulesCharts().escalatingFailure(totalHits);

        if (toughness) {
            rollTarget -= entity.getCrew().getToughness(crewPos);
        }

        boolean rerollWithEdge = false;
        boolean edgeAlreadyUsed = false;
        do {
            if (rerollWithEdge) {
                entity.getCrew().decreaseEdge();
                edgeAlreadyUsed = true;
                rerollWithEdge = false;
            }
            Roll diceRoll = Compute.rollD6(2);
            int rollValue = diceRoll.getIntValue();
            String rollCalc = String.valueOf(rollValue);

            if (entity.hasAbility(OptionsConstants.MISC_PAIN_RESISTANCE)) {
                rollValue = Math.min(12, rollValue + 1);
                rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 1] max 12";
            }

            Report r = new Report(6030);
            r.indent(2);
            r.subject = entity.getId();
            r.add(entity.getCrew().getCrewType().getRoleName(crewPos));
            r.addDesc(entity);
            r.add(entity.getCrew().getName(crewPos));
            r.add(rollTarget);
            r.addDataWithTooltip(rollCalc, diceRoll.getReport());

            if (rollValue >= rollTarget) {
                entity.getCrew().setKoThisRound(false, crewPos);
                r.choose(true);
            } else {
                entity.getCrew().setKoThisRound(true, crewPos);
                r.choose(false);
                if (!edgeAlreadyUsed && (entity.shouldUseEdge(OptionsConstants.EDGE_WHEN_KO) ||
                      entity.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_KO))) {
                    rerollWithEdge = true;
                    vDesc.add(r);
                    r = new Report(6520);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(entity.getCrew().getName(crewPos));
                    r.add(entity.getCrew().getOptions().intOption(OptionsConstants.EDGE));
                } // if
                // return true;
            } // else
            vDesc.add(r);
        } while (rerollWithEdge);
        // end of do-while
        if (entity.getCrew().isKoThisRound(crewPos)) {
            boolean wasPilot = entity.getCrew().getCurrentPilotIndex() == crewPos;
            boolean wasGunner = entity.getCrew().getCurrentGunnerIndex() == crewPos;
            entity.getCrew().setUnconscious(true, crewPos);
            Report r = createCrewTakeoverReport(entity, crewPos, wasPilot, wasGunner);
            if (null != r) {
                vDesc.add(r);
            }
            return vDesc;
        }
        return vDesc;
    }
    
    /**
     * {@inheritDoc}
     * Handle pilot hits. Core p.117. Only the highest roll is performed.
     */
    @Nullable
    @Override
    public Vector<Report> pilotHits(Entity e, int totalHits, int damage, int crewPos, boolean toughness,
          GamePhase phase) {
        Vector<Report> vDesc = new Vector<>();

        if (!phase.isMovement()) {
            Report r = new Report(3902);
            r.indent(2);
            r.subject = e.getId();
            r.add(e.getCrew().getCrewType().getRoleName(crewPos));
            r.addDesc(e);
            r.add(e.getCrew().getName(crewPos));
            vDesc.add(r);
            
            e.getCrew().setPendingConRolls(true, crewPos);
            return vDesc;
        }
        
        e.getCrew().setPendingConRolls(false, crewPos);
        
        vDesc = (rollPilotHits(e, totalHits, crewPos, toughness));
        
        return vDesc;
    }

    /**
    * {@inheritDoc}
     */
    @Override
    public Vector<Report> rollConRolls(Entity entity, boolean toughness) {
        Vector<Report> vDesc = new Vector<>();
        if (!entity.getCrew().isDead() && !entity.getCrew().isEjected() && !entity.getCrew().isDoomed()) {
            for (int pos = 0; pos < entity.getCrew().getSlotCount(); pos++) {
                if (entity.getCrew().hasPendingConRoll(pos)) {
                    vDesc.addAll(rollPilotHits(entity, entity.getCrew().getHits(pos), pos, toughness));
                    entity.getCrew().setPendingConRolls(false, pos);
                }
                
            }
        }
        if (!vDesc.isEmpty()) {
            return vDesc;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * How many pilot hits for an explosion Core p.117
     */
    @Override
    public int getExplosionPilotHits() {
        return 1;
    }

    /**
     * {@inheritDoc}
     * Seatbelt check for gyro has no modifier Core p.117
     */
    @Override
    public int getSeatbeltGyroModifier(int piloting) {
        return piloting;
    }

    /**
     * {@inheritDoc}
     * Seatbelt check for legs destroyed has no modifier Core p.117
     */
    @Override
    public int getSeatbeltLegModifier(int piloting, int legsDestroyed) {
        return piloting;
    }

    /**
     * {@inheritDoc}
     * Seatbelt check for shutdown has no modifier Core p.117
     */
    @Override
    public int getSeatbeltShutdown(int piloting) {
        return piloting;
    }

    /**
     * {@inheritDoc}
     * Always create a new roll with just piloting and height of fall
     */
    @Override
    public PilotingRollData getSeatbeltRoll(Entity entity,
          int fallHeight,
          int piloting,
          List<TargetRollModifier> modifiers,
          PilotingRollData roll) {
        if (roll.getValue() == TargetRoll.IMPOSSIBLE) {
            return roll;
        }
        PilotingRollData prd;
        if (entity.isImmobile()) {
            prd = new PilotingRollData(entity.getId(), TargetRoll.AUTOMATIC_FAIL, "unit is immobile");
        } else {
            prd = new PilotingRollData(entity.getId(), piloting, "Base piloting skill");
            if (fallHeight >= 1) {
                prd.addModifier(getSeatbeltHeightModifier(fallHeight), "height of "
                      + "fall");
            }
        }
        return prd;
    }
}
