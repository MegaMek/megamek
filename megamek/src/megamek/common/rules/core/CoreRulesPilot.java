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
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesPilot;
import megamek.common.units.Entity;

import java.util.Vector;

public class CoreRulesPilot extends RulesPilot {

    /**
     * {@inheritDoc}
     * Handle pilot hits. Core p.117. Only the highest roll is performed.
     */
    @Override
    public Vector<Report> pilotHits(Entity e, int totalHits, int damage, int crewPos, boolean toughness) {
        Vector<Report> vDesc = new Vector<>();

        int rollTarget = Game.rulesManager.getRulesCharts().escalatingFailure(totalHits);

        if (toughness) {
            rollTarget -= e.getCrew().getToughness(crewPos);
        }

        boolean rerollWithEdge = false;
        boolean edgeAlreadyUsed = false;

        do {
            if (rerollWithEdge) {
                e.getCrew().decreaseEdge();
                edgeAlreadyUsed = true;
                rerollWithEdge = false;
            }
            Roll diceRoll = Compute.rollD6(2);
            int rollValue = diceRoll.getIntValue();
            String rollCalc = String.valueOf(rollValue);

            if (e.hasAbility(OptionsConstants.MISC_PAIN_RESISTANCE)) {
                rollValue = Math.min(12, rollValue + 1);
                rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 1] max 12";
            }

            Report r = new Report(6030);
            r.indent(2);
            r.subject = e.getId();
            r.add(e.getCrew().getCrewType().getRoleName(crewPos));
            r.addDesc(e);
            r.add(e.getCrew().getName(crewPos));
            r.add(rollTarget);
            r.addDataWithTooltip(rollCalc, diceRoll.getReport());

            if (rollValue >= rollTarget) {
                e.getCrew().setKoThisRound(false, crewPos);
                r.choose(true);
            } else {
                e.getCrew().setKoThisRound(true, crewPos);
                r.choose(false);
                if (!edgeAlreadyUsed && (e.shouldUseEdge(OptionsConstants.EDGE_WHEN_KO) ||
                      e.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_KO))) {
                    rerollWithEdge = true;
                    vDesc.add(r);
                    r = new Report(6520);
                    r.subject = e.getId();
                    r.addDesc(e);
                    r.add(e.getCrew().getName(crewPos));
                    r.add(e.getCrew().getOptions().intOption(OptionsConstants.EDGE));
                } // if
                // return true;
            } // else
            vDesc.add(r);
        } while (rerollWithEdge);
        // end of do-while
        if (e.getCrew().isKoThisRound(crewPos)) {
            boolean wasPilot = e.getCrew().getCurrentPilotIndex() == crewPos;
            boolean wasGunner = e.getCrew().getCurrentGunnerIndex() == crewPos;
            e.getCrew().setUnconscious(true, crewPos);
            Report r = createCrewTakeoverReport(e, crewPos, wasPilot, wasGunner);
            if (null != r) {
                vDesc.add(r);
            }
            return vDesc;
        }


        return vDesc;
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
}
