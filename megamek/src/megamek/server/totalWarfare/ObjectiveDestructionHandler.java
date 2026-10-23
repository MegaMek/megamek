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

import java.util.List;
import java.util.Map;

import megamek.client.ui.Messages;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Destroys objectives (Standard Missions, Objectives). An objective inside a building is destroyed when its building
 * hex collapses, and a Fragile objective rolls 1D6 each time something risks it, destroyed on 1-4: its hex burning in
 * the End Phase, a heat-causing weapon or area-effect damage hitting its hex, being dropped by force or with a carrier
 * that falls or dies, and being pushed to a different level. A mission that says objectives cannot be destroyed makes
 * them invulnerable, and they survive all of it. A destroyed objective stays on the board, marked destroyed, so the
 * End Phase can still settle who it counts for.
 */
class ObjectiveDestructionHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveDestructionHandler.class);

    /** A Fragile objective is destroyed on a 1D6 roll of this or lower. */
    static final int FRAGILE_DESTRUCTION_MAXIMUM_ROLL = 4;

    /** "Fragile objective X at Y (event): rolls N, destroyed on 1-4 : it survives / it is destroyed!" */
    static final int REPORT_FRAGILE_ROLL = 7169;
    /** "Objective X at Y is destroyed." */
    static final int REPORT_OBJECTIVE_DESTROYED = 7172;

    /** The events that put a Fragile objective at risk; each names its own report wording. */
    enum FragileRisk {
        FIRE,
        HEAT_WEAPON,
        AREA_EFFECT,
        DROPPED,
        LEVEL_CHANGE;

        /** @return the event as a player reads it in the report */
        String describe() {
            return Messages.getString("VictoryHex.fragileRisk." + name());
        }
    }

    ObjectiveDestructionHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * End Phase: every Fragile objective in a burning hex rolls, whether it lies on the ground or is carried through
     * the fire. Runs before control is resolved, so an objective destroyed here is gone before it pays this turn.
     */
    void checkFragileObjectivesOnFire() {
        Board board = getGame().getBoard();
        if (board == null) {
            return;
        }
        for (Map.Entry<Coords, List<ICarryable>> hexObjects : getGame().getGroundObjects().entrySet()) {
            if (!isBurning(board, hexObjects.getKey())) {
                continue;
            }
            for (ICarryable groundObject : List.copyOf(hexObjects.getValue())) {
                if (groundObject instanceof ObjectiveMarker marker) {
                    rollFragileDestruction(marker, hexObjects.getKey(), FragileRisk.FIRE);
                }
            }
        }
        for (Entity carrier : getGame().getEntitiesVector()) {
            Coords carrierPosition = carrier.getPosition();
            if ((carrierPosition == null) || !isBurning(board, carrierPosition)) {
                continue;
            }
            for (ICarryable carriedObject : carrier.getDistinctCarriedObjects()) {
                if (carriedObject instanceof ObjectiveMarker marker) {
                    rollFragileDestruction(marker, carrierPosition, FragileRisk.FIRE);
                }
            }
        }
    }

    private boolean isBurning(Board board, Coords position) {
        return (board.getHex(position) != null) && board.getHex(position).containsTerrain(Terrains.FIRE);
    }

    /**
     * Rolls for every Fragile objective lying in a hex that a heat-causing weapon or area-effect damage just hit, at
     * the time of the hit.
     *
     * @param position the hex that was hit
     * @param boardId  the board the hex is on; objectives lie only on the game's main board
     * @param risk     what hit the hex
     */
    void checkFragileObjectivesInHex(Coords position, int boardId, FragileRisk risk) {
        if (!isObjectiveBoard(boardId)) {
            return;
        }
        for (ICarryable groundObject : List.copyOf(getGame().getGroundObjects(position))) {
            if (groundObject instanceof ObjectiveMarker marker) {
                rollFragileDestruction(marker, position, risk);
            }
        }
    }

    /**
     * Destroys every objective in a building hex that has just collapsed (RAW: objectives inside a building count
     * as being at its ground level and are destroyed with it), unless the mission says objectives cannot be
     * destroyed.
     *
     * @param position the collapsed building hex
     * @param boardId  the board the building stands on
     */
    void destroyObjectivesInCollapsedHex(Coords position, int boardId) {
        if (!isObjectiveBoard(boardId)) {
            return;
        }
        for (ICarryable groundObject : getGame().getGroundObjects(position)) {
            if (!(groundObject instanceof ObjectiveMarker marker) || marker.isDestroyed()) {
                continue;
            }
            if (marker.isInvulnerable()) {
                LOGGER.info("[Objective] {} at {}: its building collapsed, but objectives cannot be destroyed in "
                      + "this mission", marker.generalName(), position.getBoardNum());
                continue;
            }
            LOGGER.info("[Objective] {} at {}: destroyed with its building", marker.generalName(),
                  position.getBoardNum());
            destroy(marker, position);
        }
    }

    /**
     * Rolls the Fragile destruction check for one event. An objective that is not Fragile, is already destroyed, or
     * cannot be destroyed in this mission makes no roll.
     *
     * @param marker   the objective at risk
     * @param position where it is: its hex, or its carrier's
     * @param risk     what put it at risk
     *
     * @return {@code true} if the objective is destroyed after the event
     */
    boolean rollFragileDestruction(ObjectiveMarker marker, Coords position, FragileRisk risk) {
        if (!marker.isFragile() || marker.isDestroyed()) {
            return marker.isDestroyed();
        }
        if (marker.isInvulnerable()) {
            LOGGER.debug("[Objective] Fragile objective {} at {} is at risk ({}), but objectives cannot be destroyed "
                  + "in this mission", marker.generalName(), position.getBoardNum(), risk);
            return false;
        }
        int roll = rollFragileCheck();
        boolean isDestroyed = roll <= FRAGILE_DESTRUCTION_MAXIMUM_ROLL;
        LOGGER.info("[Objective] Fragile objective {} at {} is at risk ({}): rolled {} - {}", marker.generalName(),
              position.getBoardNum(), risk, roll, isDestroyed ? "destroyed" : "survives");
        Report report = new Report(REPORT_FRAGILE_ROLL, Report.PUBLIC);
        report.add(marker.generalName());
        report.add(position.getBoardNum());
        report.add(risk.describe());
        report.add(roll);
        report.add(FRAGILE_DESTRUCTION_MAXIMUM_ROLL);
        report.choose(!isDestroyed);
        addReport(report);
        if (isDestroyed) {
            destroy(marker, position);
        }
        return isDestroyed;
    }

    /** Marks the objective destroyed, reports it once, and sends the clients the board. */
    private void destroy(ObjectiveMarker marker, Coords position) {
        marker.setDestroyed(true);
        if (marker.isDestructionProcessed()) {
            return;
        }
        marker.setDestructionProcessed(true);
        Report report = new Report(REPORT_OBJECTIVE_DESTROYED, Report.PUBLIC);
        report.add(marker.generalName());
        report.add(position.getBoardNum());
        addReport(report);
        gameManager.sendGroundObjectUpdate();
    }

    /** @return {@code true} for the board objectives lie on: the game's main board */
    private boolean isObjectiveBoard(int boardId) {
        Board board = getGame().getBoard();
        return (board != null) && (board.getBoardId() == boardId);
    }

    /** Test seam: the Fragile destruction roll, 1D6. */
    int rollFragileCheck() {
        return Compute.d6();
    }
}
