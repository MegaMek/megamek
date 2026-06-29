package megamek.common.rules.totalwarfare;
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

import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rules.core.CoreRulesPilot;
import megamek.common.units.Entity;

import java.util.Vector;

public class TwRulesPilot extends CoreRulesPilot {

    public Vector<Report> pilotHits(Entity e, int totalHits, int damage, int crewPos, boolean toughness) {
        Vector<Report> vDesc = new Vector<>();
        for (int hit = (totalHits - damage) + 1; hit <= totalHits; hit++) {
            int rollTarget = Game.rulesManager.getRulesCharts().escalatingFailure(hit);
            if (toughness) {
                rollTarget -= e.getCrew().getToughness(crewPos);
            }
            boolean edgeUsed = false;
            do {
                if (edgeUsed) {
                    e.getCrew().decreaseEdge();
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
                    if (e.shouldUseEdge(OptionsConstants.EDGE_WHEN_KO) ||
                          e.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_KO)) {
                        edgeUsed = true;
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
            } while (e.getCrew().isKoThisRound(crewPos) &&
                  (e.shouldUseEdge(OptionsConstants.EDGE_WHEN_KO) ||
                        e.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_KO)));
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
        }
        return vDesc;
    }

    // How many pilot hits for an explosion
    @Override
    public int getExplosionPilotHits() {
        return 2;
    }
}
