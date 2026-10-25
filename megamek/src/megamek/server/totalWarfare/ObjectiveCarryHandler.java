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
package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.ObjectiveDestructionHandler.FragileRisk;

/**
 * Carrying Mobile objectives (Standard Missions, Objectives). Picking one up rides the cargo pickup step and is limited
 * to Meks in {@link ObjectiveMarker}; this handler covers what happens after. A carrier drops its objective when it
 * becomes immobile or lies prone at the End Phase, and when it fails the Piloting Skill Roll that any damage in a phase
 * forces. A carrier that falls or is destroyed drops it through the cargo rules, and the objective lands in that hex
 * intact unless it is Fragile. Only one objective fits in a hex: dropping a second pushes the first one hex on.
 */
class ObjectiveCarryHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveCarryHandler.class);

    /** "X drops objective Y in hex Z." */
    static final int REPORT_OBJECTIVE_DROPPED = 7164;
    /** "X took damage while carrying objective Y: needs N, rolls M : keeps hold / drops it" */
    static final int REPORT_FORCED_DROP_CHECK = 7165;
    /** "Objective X is pushed from Y to Z: only one objective fits in a hex." */
    static final int REPORT_OBJECTIVE_DISPLACED = 7168;
    /** "X is forced off the battlefield; objective Y stays behind in hex Z." */
    static final int REPORT_OBJECTIVE_LEFT_BEHIND = 7177;

    /** RAW: two intact hand actuators make the forced-drop roll easier by this much (two claws do not). */
    static final int TWO_HANDS_MODIFIER = -2;

    // a safety bound for a push that keeps finding the next hex occupied
    private static final int MAXIMUM_DISPLACEMENT_CHAIN = 20;

    ObjectiveCarryHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * End Phase drops. A carrier that has become immobile drops its objective, and the drop puts a Fragile objective
     * at risk. A carrier still prone at the End Phase went prone by choice, because one that fell has already dropped
     * it, so it sets the objective down without risk.
     */
    void resolveEndPhaseDrops() {
        for (Entity carrier : getGame().getEntitiesVector()) {
            for (ObjectiveMarker marker : carriedObjectives(carrier)) {
                if (carrier.isImmobile()) {
                    LOGGER.info("[Objective] {} is immobile and drops {}", carrier.getShortName(),
                          marker.generalName());
                    dropObjective(carrier, marker, true);
                } else if (carrier.isProne()) {
                    LOGGER.info("[Objective] {} is prone at the End Phase and sets down {} without a Fragile roll",
                          carrier.getShortName(), marker.generalName());
                    dropObjective(carrier, marker, false);
                }
            }
        }
    }

    /**
     * The forced-drop check after a phase in which a carrier took any damage: a Piloting Skill Roll to keep hold,
     * with {@value #TWO_HANDS_MODIFIER} for a Mek with two intact hand actuators. Failing drops the objective in the
     * carrier's hex and puts a Fragile objective at risk, but never causes a fall. Runs at the end of each phase
     * that deals damage, beside the damage Piloting Skill Rolls.
     */
    void resolveForcedObjectiveDrops() {
        for (Entity carrier : getGame().getEntitiesVector()) {
            if (carrier.damageThisPhase <= 0) {
                continue;
            }
            List<ObjectiveMarker> carriedMarkers = carriedObjectives(carrier);
            if (!carriedMarkers.isEmpty()) {
                resolveForcedDropCheck(carrier, carriedMarkers);
            }
        }
    }

    private void resolveForcedDropCheck(Entity carrier, List<ObjectiveMarker> carriedMarkers) {
        PilotingRollData rollData = carrier.getBasePilotingRoll();
        if (hasTwoIntactHandActuators(carrier)) {
            rollData.addModifier(TWO_HANDS_MODIFIER, Messages.getString("VictoryHex.forcedDrop.twoHands"));
        }
        int targetNumber = rollData.getValue();
        int roll = rollForcedDropCheck();
        boolean keepsHold = roll >= targetNumber;
        LOGGER.info("[Objective] {} took {} damage while carrying {}: forced-drop roll needs {} ({}), rolled {} - {}",
              carrier.getShortName(), carrier.damageThisPhase, carriedMarkers.getFirst().generalName(), targetNumber,
              rollData.getDesc(), roll, keepsHold ? "keeps hold" : "drops it");
        Report report = new Report(REPORT_FORCED_DROP_CHECK, Report.PUBLIC);
        report.subject = carrier.getId();
        report.addDesc(carrier);
        report.add(carriedMarkers.getFirst().generalName());
        report.add(targetNumber);
        report.add(roll);
        report.choose(keepsHold);
        addReport(report);
        if (!keepsHold) {
            for (ObjectiveMarker marker : carriedMarkers) {
                dropObjective(carrier, marker, true);
            }
        }
    }

    /**
     * @return {@code true} for a Mek with a working hand actuator in both arms; claws carry like hands, but two claws
     *       do not give the bonus
     */
    private boolean hasTwoIntactHandActuators(Entity carrier) {
        return (carrier instanceof Mek)
              && carrier.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM)
              && carrier.hasWorkingSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM);
    }

    /**
     * Settles an objective that the cargo rules drop because its carrier fell or was destroyed, or dumped it during a
     * move. The cargo rules would roll to wreck it; an objective is never wrecked by a drop, and only a Fragile one
     * risks destruction. It always lands in the hex, destroyed or not, so the board shows what happened to it.
     *
     * @param carrier the unit that was carrying it
     * @param marker  the dropped objective
     *
     * @return {@code false}: the objective is never lost from the board by being dropped
     */
    boolean resolveDropDamage(Entity carrier, ObjectiveMarker marker) {
        if (carrier.getPosition() != null) {
            destructionHandler().rollFragileDestruction(marker, carrier.getPosition(), FragileRisk.DROPPED);
        }
        LOGGER.info("[Objective] {} drops {} as cargo{}", carrier.getShortName(), marker.generalName(),
              marker.isDestroyed() ? "; it is destroyed" : "; it lands intact");
        return false;
    }

    /**
     * A carrier pushed or skidding off the battlefield (Core Standard Missions, Mobile Objectives, Displaced Off the
     * Battlefield). Over an edge it is allowed to leave by, it counts as having left with the objective, which then
     * pays as carried off at mission end. Over any other edge the objective stays behind in the last hex on the
     * battlefield the carrier occupied. Being displaced is not a drop, so a Fragile objective is not at risk.
     *
     * @param carrier       the unit leaving the battlefield
     * @param lastInPlayHex the last hex on the battlefield it occupied
     *
     * @return the report lines, for the caller's report of the displacement
     */
    Vector<Report> resolveCarrierForcedOffBoard(Entity carrier, Coords lastInPlayHex) {
        Vector<Report> reports = new Vector<>();
        List<ObjectiveMarker> carriedMarkers = carriedObjectives(carrier);
        if (carriedMarkers.isEmpty()) {
            return reports;
        }
        if (getGame().canFleeFrom(carrier, lastInPlayHex)) {
            LOGGER.info("[Objective] {} is forced off the battlefield at {}, an edge it may leave by - it takes {} with "
                  + "it", carrier.getShortName(), lastInPlayHex.getBoardNum(), carriedMarkers.getFirst().generalName());
            return reports;
        }
        for (ObjectiveMarker marker : carriedMarkers) {
            LOGGER.info("[Objective] {} is forced off the battlefield at {}, not an edge it may leave by - {} stays in "
                  + "that hex", carrier.getShortName(), lastInPlayHex.getBoardNum(), marker.generalName());
            carrier.dropCarriedObject(marker, false);
            placeDroppedObjective(carrier, lastInPlayHex, marker);
            Report report = new Report(REPORT_OBJECTIVE_LEFT_BEHIND, Report.PUBLIC);
            report.subject = carrier.getId();
            report.addDesc(carrier);
            report.add(marker.generalName());
            report.add(lastInPlayHex.getBoardNum());
            reports.add(report);
        }
        gameManager.sendGroundObjectUpdate();
        return reports;
    }

    /**
     * Takes the objective from the carrier and sets it in the carrier's hex, telling everyone.
     *
     * @param carrier         the carrier
     * @param marker          the carried objective
     * @param isFragileAtRisk whether this drop puts a Fragile objective at risk (a forced drop does; setting it down
     *                        by going prone does not)
     */
    private void dropObjective(Entity carrier, ObjectiveMarker marker, boolean isFragileAtRisk) {
        Coords position = carrier.getPosition();
        carrier.dropCarriedObject(marker, false);
        if (position == null) {
            LOGGER.warn("[Objective] {} dropped {} but has no position - the objective is lost",
                  carrier.getShortName(), marker.generalName());
            return;
        }
        if (isFragileAtRisk) {
            destructionHandler().rollFragileDestruction(marker, position, FragileRisk.DROPPED);
        }
        placeDroppedObjective(carrier, position, marker);
        gameManager.entityUpdate(carrier.getId());
        gameManager.sendGroundObjectUpdate();
        Report report = new Report(REPORT_OBJECTIVE_DROPPED, Report.PUBLIC);
        report.subject = carrier.getId();
        report.addDesc(carrier);
        report.add(marker.generalName());
        report.add(position.getBoardNum());
        addReport(report);
    }

    /**
     * Sets a dropped objective in the hex, enforcing one objective per hex: an objective already there is pushed one
     * hex in the direction the dropping unit faces, and the push carries on while the next hex is taken. An objective
     * that would be pushed off the board stays in its last hex on it, and a push to a different level puts a Fragile
     * objective at risk. A destroyed objective is a wreck and is neither pushed nor in the way.
     *
     * @param droppingUnit  the unit dropping it; its facing decides where a displaced objective goes
     * @param position      the hex it is dropped in
     * @param droppedMarker the objective being dropped
     */
    void placeDroppedObjective(Entity droppingUnit, Coords position, ObjectiveMarker droppedMarker) {
        ObjectiveMarker displacedMarker = intactObjectiveAt(position, droppedMarker);
        getGame().placeGroundObject(position, droppedMarker);
        if ((displacedMarker != null) && !droppedMarker.isDestroyed()) {
            displaceObjective(displacedMarker, position, droppingUnit.getFacing(), 0);
        }
    }

    private @Nullable ObjectiveMarker intactObjectiveAt(Coords position, ObjectiveMarker excludedMarker) {
        for (ICarryable groundObject : getGame().getGroundObjects(position)) {
            if ((groundObject instanceof ObjectiveMarker marker) && (marker != excludedMarker)
                  && !marker.isDestroyed()) {
                return marker;
            }
        }
        return null;
    }

    private void displaceObjective(ObjectiveMarker marker, Coords from, int direction, int chainDepth) {
        if ((direction < 0) || (chainDepth > MAXIMUM_DISPLACEMENT_CHAIN)) {
            LOGGER.warn("[Objective] {} at {} cannot be pushed (direction {}, chain {}) - it stays and shares the hex",
                  marker.generalName(), from.getBoardNum(), direction, chainDepth);
            return;
        }
        Board board = getGame().getBoard();
        Coords destination = from.translated(direction);
        if ((board == null) || !board.contains(destination)) {
            // RAW: an objective displaced off the battlefield stays in its last hex on it
            LOGGER.info("[Objective] {} would be pushed off the board - it stays at {}", marker.generalName(),
                  from.getBoardNum());
            return;
        }
        ObjectiveMarker nextMarker = intactObjectiveAt(destination, marker);
        getGame().removeGroundObject(from, marker);
        getGame().placeGroundObject(destination, marker);
        LOGGER.info("[Objective] {} is pushed from {} to {}: one objective per hex", marker.generalName(),
              from.getBoardNum(), destination.getBoardNum());
        Report report = new Report(REPORT_OBJECTIVE_DISPLACED, Report.PUBLIC);
        report.add(marker.generalName());
        report.add(from.getBoardNum());
        report.add(destination.getBoardNum());
        addReport(report);

        boolean isLevelChange = (board.getHex(from) != null) && (board.getHex(destination) != null)
              && (board.getHex(from).getLevel() != board.getHex(destination).getLevel());
        if (isLevelChange) {
            destructionHandler().rollFragileDestruction(marker, destination, FragileRisk.LEVEL_CHANGE);
        }
        if (nextMarker != null) {
            displaceObjective(nextMarker, destination, direction, chainDepth + 1);
        }
    }

    /** @return the objectives the unit is carrying, copied so they can be dropped while walking the list */
    private static List<ObjectiveMarker> carriedObjectives(Entity carrier) {
        List<ObjectiveMarker> carriedMarkers = new ArrayList<>();
        for (ICarryable carriedObject : carrier.getDistinctCarriedObjects()) {
            if (carriedObject instanceof ObjectiveMarker marker) {
                carriedMarkers.add(marker);
            }
        }
        return carriedMarkers;
    }

    /** Test seam: the handler that rolls a Fragile objective's destruction. */
    ObjectiveDestructionHandler destructionHandler() {
        return new ObjectiveDestructionHandler(gameManager);
    }

    /** Test seam: the forced-drop Piloting Skill Roll, 2D6. */
    int rollForcedDropCheck() {
        return Compute.d6(2);
    }
}
