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
package megamek.server.nuclear;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves a nuclear weapon hitting an aerospace unit in space: the special critical hit that strikes the structural
 * integrity and the Control Roll that follows (IO:AE p.166).
 */
public class NuclearSpaceHit {

    /** What the special critical hit roll did to the unit. */
    public enum SpecialCriticalOutcome {
        /** The roll missed: no extra damage. */
        NO_EFFECT,
        /** The roll hit and the extra damage went to the structural integrity. */
        STRUCTURE_DAMAGED,
        /** The roll hit, but the crew used Edge to reroll and the reroll missed. */
        AVOIDED_WITH_EDGE
    }

    private final TWGameManager gameManager;

    /**
     * @param gameManager the game manager running the game the hit happens in
     */
    public NuclearSpaceHit(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Queues the Control Roll for a nuclear hit and rolls the special critical hit against the unit's structural
     * integrity.
     *
     * @param aero           the unit hit
     * @param hit            the hit; its capital missile critical modifier is the target number
     * @param damageOriginal the damage of the attack, ten times which goes to the structural integrity on a hit
     * @param vDesc          the report the results are added to
     *
     * @return what the roll did to the unit
     */
    public SpecialCriticalOutcome resolveSpecialCritical(Aero aero, HitData hit, int damageOriginal,
          Vector<Report> vDesc) {
        int criticalTarget = hit.getCapMisCritMod();

        // add a control roll
        PilotingRollData nukePSR = new PilotingRollData(aero.getId(), 4, "Nuclear attack", false);
        gameManager.getGame().addControlRoll(nukePSR);

        Report.addNewline(vDesc);
        Roll diceRoll = Compute.rollD6(2);
        Report report = new Report(9145);
        report.subject = aero.getId();
        report.indent(3);
        report.add(criticalTarget);
        report.add(diceRoll);
        vDesc.add(report);

        if (diceRoll.getIntValue() < criticalTarget) {
            report = new Report(9147);
            report.subject = aero.getId();
            report.indent(4);
            vDesc.addElement(report);
            return SpecialCriticalOutcome.NO_EFFECT;
        }

        // Allow a reroll with edge
        if (aero.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_NUKE_CRIT) && aero.getCrew().hasEdgeRemaining()) {
            aero.getCrew().decreaseEdge();
            report = new Report(9148);
            report.subject = aero.getId();
            report.indent(3);
            report.add(aero.getCrew().getOptions().intOption(OptionsConstants.EDGE));
            vDesc.add(report);
            Roll edgeReroll = Compute.rollD6(2);
            report = new Report(9149);
            report.subject = aero.getId();
            report.indent(3);
            report.add(criticalTarget);
            report.add(edgeReroll);
            report.choose(edgeReroll.getIntValue() >= criticalTarget);
            vDesc.add(report);

            if (edgeReroll.getIntValue() < criticalTarget) {
                // We might be vaporized by the damage itself, but no additional effect
                return SpecialCriticalOutcome.AVOIDED_WITH_EDGE;
            }
        }
        aero.setSI(aero.getSI() - (damageOriginal * 10));
        aero.damageThisPhase += (damageOriginal * 10);
        report = new Report(9146);
        report.subject = aero.getId();
        report.add((damageOriginal * 10));
        report.indent(4);
        report.add(Math.max(aero.getSI(), 0));
        vDesc.addElement(report);
        if (aero.getSI() <= 0) {
            // No auto-ejection chance here. Nuke would vaporize the pilot.
            vDesc.addAll(gameManager.destroyEntity(aero, "Structural Integrity Collapse"));
            aero.setSI(0);
            if (hit.getAttackerId() != Entity.NONE) {
                gameManager.creditKill(aero, gameManager.getGame().getEntity(hit.getAttackerId()));
            }
        }
        return SpecialCriticalOutcome.STRUCTURE_DAMAGED;
    }
}
