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

import java.util.List;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves nuclear weapons hitting an aerospace unit in space (IO:AE p.166). The ordinary capital damage has already
 * been applied, armor first and halved into the structural integrity as usual. Each nuclear missile that hit then
 * rolls its own special critical hit: an unmodified 2d6 roll at or above the weapon's critical hit target puts ten
 * times the weapon's capital damage straight into the structural integrity, never halved. A unit that survives makes
 * a Control Roll at +4.
 */
public class NuclearSpaceHit {
    private static final MMLogger LOGGER = MMLogger.create(NuclearSpaceHit.class);

    /** The Control Roll modifier for a unit that survives a nuclear hit in space (IO:AE p.166). */
    static final int CONTROL_ROLL_MODIFIER = 4;

    /** The special critical hit deals this many times the weapon's capital damage (IO:AE p.166). */
    static final int CRITICAL_DAMAGE_MULTIPLIER = 10;

    /**
     * What the special critical hit rolls did to the unit.
     *
     * @param structureDamaged {@code true} if at least one roll put damage into the structural integrity
     * @param avoidedWithEdge  {@code true} if the crew used Edge to reroll a hit and the reroll missed
     */
    public record SpecialCriticalResult(boolean structureDamaged, boolean avoidedWithEdge) {}

    private final TWGameManager gameManager;

    /**
     * @param gameManager the game manager running the game the hit happens in
     */
    public NuclearSpaceHit(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Rolls the special critical hit for every nuclear warhead in the hit, then queues the +4 Control Roll if the unit
     * survived.
     *
     * @param aero  the unit hit
     * @param hit   the hit, which lists its nuclear warheads
     * @param vDesc the report the results are added to
     *
     * @return what the rolls did to the unit
     */
    public SpecialCriticalResult resolve(Aero aero, HitData hit, Vector<Report> vDesc) {
        List<NuclearWeaponType> warheads = hit.getNuclearWarheads();
        if (warheads.isEmpty()) {
            LOGGER.warn("[Nukes] Nuclear hit on {} names no warhead: no special critical hit rolled",
                  aero.getDisplayName());
            return new SpecialCriticalResult(false, false);
        }

        boolean structureDamaged = false;
        boolean avoidedWithEdge = false;
        for (NuclearWeaponType warhead : warheads) {
            if (isGone(aero)) {
                LOGGER.info("[Nukes] {} is already destroyed: the {} warhead rolls no special critical hit",
                      aero.getDisplayName(), warhead.getDisplayName());
                continue;
            }
            switch (rollSpecialCritical(aero, hit, warhead, vDesc)) {
                case STRUCTURE_DAMAGED -> structureDamaged = true;
                case AVOIDED_WITH_EDGE -> avoidedWithEdge = true;
                case NO_EFFECT -> {
                    // nothing more to record
                }
            }
        }

        if (isGone(aero)) {
            LOGGER.info("[Nukes] {} did not survive the nuclear hit: no Control Roll", aero.getDisplayName());
        } else {
            gameManager.getGame()
                  .addControlRoll(new PilotingRollData(aero.getId(), CONTROL_ROLL_MODIFIER, "Nuclear attack", false));
            LOGGER.info("[Nukes] {} survived the nuclear hit: Control Roll at +{} queued", aero.getDisplayName(),
                  CONTROL_ROLL_MODIFIER);
        }
        return new SpecialCriticalResult(structureDamaged, avoidedWithEdge);
    }

    /**
     * @param warhead the nuclear weapon
     *
     * @return the damage the weapon's special critical hit puts into the structural integrity
     */
    static int specialCriticalDamage(NuclearWeaponType warhead) {
        return CRITICAL_DAMAGE_MULTIPLIER * warhead.getCapitalDamage();
    }

    private enum RollOutcome {
        NO_EFFECT,
        STRUCTURE_DAMAGED,
        AVOIDED_WITH_EDGE
    }

    private RollOutcome rollSpecialCritical(Aero aero, HitData hit, NuclearWeaponType warhead, Vector<Report> vDesc) {
        int criticalTarget = warhead.getCriticalHitTarget();
        int criticalDamage = specialCriticalDamage(warhead);

        Report.addNewline(vDesc);
        Report report = new Report(6564);
        report.subject = aero.getId();
        report.indent(3);
        report.addDesc(aero);
        report.add(warhead.getDisplayName());
        vDesc.add(report);

        Roll diceRoll = Compute.rollD6(2);
        report = new Report(9145);
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
            LOGGER.info("[Nukes] {} hit by {}: special critical hit needs {}, rolled {}: no effect",
                  aero.getDisplayName(), warhead.getDisplayName(), criticalTarget, diceRoll.getIntValue());
            return RollOutcome.NO_EFFECT;
        }

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
                LOGGER.info("[Nukes] {} hit by {}: special critical hit rolled {}, Edge reroll {} misses {}",
                      aero.getDisplayName(), warhead.getDisplayName(), diceRoll.getIntValue(),
                      edgeReroll.getIntValue(), criticalTarget);
                return RollOutcome.AVOIDED_WITH_EDGE;
            }
        }

        aero.setSI(aero.getSI() - criticalDamage);
        aero.damageThisPhase += criticalDamage;
        report = new Report(9146);
        report.subject = aero.getId();
        report.add(criticalDamage);
        report.indent(4);
        report.add(Math.max(aero.getSI(), 0));
        vDesc.addElement(report);
        LOGGER.info("[Nukes] {} hit by {}: special critical hit needs {}, rolled {}: {} to the structural integrity,"
                    + " {} left", aero.getDisplayName(), warhead.getDisplayName(), criticalTarget,
              diceRoll.getIntValue(), criticalDamage, Math.max(aero.getSI(), 0));
        if (aero.getSI() <= 0) {
            // No auto-ejection chance here. Nuke would vaporize the pilot.
            vDesc.addAll(gameManager.destroyEntity(aero, "Structural Integrity Collapse"));
            aero.setSI(0);
            if (hit.getAttackerId() != Entity.NONE) {
                gameManager.creditKill(aero, gameManager.getGame().getEntity(hit.getAttackerId()));
            }
        }
        return RollOutcome.STRUCTURE_DAMAGED;
    }

    private static boolean isGone(Entity entity) {
        return entity.isDoomed() || entity.isDestroyed();
    }
}
