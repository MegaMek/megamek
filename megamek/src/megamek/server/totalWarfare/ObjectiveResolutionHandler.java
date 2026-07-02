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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryPointTracker;

/**
 * End-Phase resolution for objective markers (Standard Missions, Objectives): determines which side controls each
 * {@link ObjectiveMarker} on the board and awards Victory Points into the game's {@link VictoryPointTracker} per the
 * standard control mission scoring.
 *
 * <P>Control: a side controls an objective when it has strictly more eligible units within the objective's control
 * radius than any other side. Crippled, prone, immobile and transported units do not count, and flying units
 * (airborne aerospace units, VTOLs and WiGEs at altitude) cannot control - grounded units count normally. A tie, or
 * no units in range, leaves the objective uncontrolled.</P>
 *
 * <P>Scoring (standard control mission, played with four counters, two per side): each End Phase a side receives 1 VP
 * for controlling at least one friendly and at least one enemy objective, and 2 VP for controlling all objectives on
 * the board. Other mission scoring rules (Objective Raid, Sensor Check) resolve in later implementation phases.</P>
 *
 * <P>Destruction: an objective counter inside a building rides that building - when the building hex is destroyed,
 * a destructible objective is destroyed with it. Objectives cannot be destroyed unless the mission allows it
 * (scenario key {@code destructible: true}); destroyed objectives no longer score.</P>
 *
 * <P>Sides are teams; a player without a team forms its own side.</P>
 */
class ObjectiveResolutionHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveResolutionHandler.class);

    private static final int REPORT_OBJECTIVE_CONTROLLED = 7117;
    private static final int REPORT_OBJECTIVE_UNCONTROLLED = 7118;
    private static final int REPORT_OBJECTIVE_POINTS_AWARDED = 7119;
    private static final int REPORT_OBJECTIVE_DESTROYED = 7120;

    /**
     * A scoring side. Normally this is a team; a player that is not on any team forms its own side.
     *
     * @param isTeam {@code true} when {@code id} is a team ID, {@code false} when it is a player ID
     * @param id     The team or player ID
     */
    record Side(boolean isTeam, int id) {}

    /**
     * An objective marker together with the position it is placed at (its position is the key of the game's ground
     * object map).
     *
     * @param position The board position of the marker
     * @param marker   The objective marker
     */
    record PlacedObjective(Coords position, ObjectiveMarker marker) {}

    /**
     * The control resolution of one objective for one End Phase.
     *
     * @param placed     The objective and its position
     * @param owningSide The side that owns (placed) the objective, or {@code null} when the owner is unknown
     * @param controller The side controlling the objective this End Phase, or {@code null} when uncontrolled
     */
    record ResolvedObjective(PlacedObjective placed, @Nullable Side owningSide, @Nullable Side controller) {}

    ObjectiveResolutionHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Resolves objectives for the current End Phase: syncs objective destruction (an objective inside a destroyed
     * building is destroyed with it), then determines the controller of every surviving objective marker, reports the
     * results and awards Victory Points per the standard control scoring. Does nothing when the game has no objective
     * markers.
     */
    void resolveObjectives() {
        List<PlacedObjective> allObjectives = findAllObjectives();
        if (allObjectives.isEmpty()) {
            return;
        }

        syncObjectiveDestruction(allObjectives);

        List<PlacedObjective> activeObjectives = allObjectives.stream()
              .filter(objective -> !objective.marker().isDestroyed())
              .toList();
        if (activeObjectives.isEmpty()) {
            LOGGER.debug("[Objective] All objectives are destroyed - no control resolution");
            return;
        }

        List<Entity> entities = getGame().getEntitiesVector();
        List<ResolvedObjective> resolvedObjectives = new ArrayList<>();
        for (PlacedObjective objective : activeObjectives) {
            Side controller = determineControllingSide(objective, entities);
            Side owningSide = sideOfPlayerId(objective.marker().getOwnerId());
            resolvedObjectives.add(new ResolvedObjective(objective, owningSide, controller));
            reportObjectiveControl(objective, controller);
        }
        awardStandardControlVictoryPoints(resolvedObjectives, VictoryPointTracker.getTracker(getGame()));
    }

    /**
     * @return All objective markers placed on the ground, including destroyed ones, with their positions
     */
    private List<PlacedObjective> findAllObjectives() {
        List<PlacedObjective> objectives = new ArrayList<>();
        for (Map.Entry<Coords, List<ICarryable>> groundObjectEntry : getGame().getGroundObjects().entrySet()) {
            for (ICarryable groundObject : groundObjectEntry.getValue()) {
                if (groundObject instanceof ObjectiveMarker marker) {
                    objectives.add(new PlacedObjective(groundObjectEntry.getKey(), marker));
                }
            }
        }
        return objectives;
    }

    /**
     * Syncs objective destruction: detects on the first pass which objectives sit inside a building, destroys
     * destructible objectives whose building hex has since been destroyed (an objective counter rides its building),
     * and reports each destroyed objective exactly once, regardless of how it was destroyed.
     *
     * @param objectives All objective markers, including already destroyed ones
     */
    private void syncObjectiveDestruction(List<PlacedObjective> objectives) {
        // Ground objects live on the game's main board (ID 0); null in board-less setups
        Board board = getGame().getBoard();
        for (PlacedObjective objective : objectives) {
            if (board != null) {
                checkBuildingDestruction(board, objective);
            }
            reportDestructionOnce(objective);
        }
    }

    private void checkBuildingDestruction(Board board, PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        initializeBuildingLink(board, objective);
        boolean ridesDestroyedBuilding = !marker.isDestroyed() && marker.isInsideBuilding()
              && isBuildingHexGone(board, objective.position());
        if (!ridesDestroyedBuilding) {
            return;
        }
        if (marker.isInvulnerable()) {
            LOGGER.debug("[Objective] {} at {}: its building was destroyed, but objectives cannot be destroyed "
                  + "in this mission", marker.generalName(), objective.position());
            return;
        }
        marker.setDestroyed(true);
        LOGGER.info("[Objective] {} at {}: destroyed with its building", marker.generalName(), objective.position());
    }

    /**
     * Detects once, on the first End Phase, whether the objective sits inside a building. From then on the objective
     * rides that building: when the building hex is destroyed, so is a destructible objective.
     */
    private void initializeBuildingLink(Board board, PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        if (marker.isBuildingLinkInitialized()) {
            return;
        }
        boolean insideBuilding = board.getBuildingAt(objective.position()) != null;
        marker.setInsideBuilding(insideBuilding);
        marker.setBuildingLinkInitialized(true);
        if (insideBuilding) {
            LOGGER.debug("[Objective] {} at {} sits inside a building and is destroyed with it (if destructible)",
                  marker.generalName(), objective.position());
        }
    }

    private boolean isBuildingHexGone(Board board, Coords position) {
        IBuilding building = board.getBuildingAt(position);
        return (building == null) || (building.getCurrentCF(position) <= 0);
    }

    private void reportDestructionOnce(PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        if (!marker.isDestroyed() || marker.isDestructionProcessed()) {
            return;
        }
        marker.setDestructionProcessed(true);
        Report report = new Report(REPORT_OBJECTIVE_DESTROYED, Report.PUBLIC);
        report.add(marker.generalName());
        report.add(objective.position().toFriendlyString());
        addReport(report);
        LOGGER.info("[Objective] {} at {} has been destroyed", marker.generalName(), objective.position());
    }

    /**
     * Determines the side controlling the given objective: the side with strictly more eligible units within the
     * objective's control radius than any other side.
     *
     * @param objective The objective to evaluate
     * @param entities  The game's entities to consider
     *
     * @return The controlling side, or {@code null} when the objective is uncontrolled (tie or no units in range)
     */
    @Nullable
    Side determineControllingSide(PlacedObjective objective, List<Entity> entities) {
        Map<Side, Integer> unitCountsBySide = new HashMap<>();
        for (Entity entity : entities) {
            if (!isEligibleToControl(entity, objective)) {
                continue;
            }
            Side side = sideOfPlayer(entity.getOwner());
            if (side != null) {
                unitCountsBySide.merge(side, 1, Integer::sum);
            }
        }

        Side leadingSide = null;
        int leadingCount = 0;
        boolean tie = false;
        for (Map.Entry<Side, Integer> countEntry : unitCountsBySide.entrySet()) {
            if (countEntry.getValue() > leadingCount) {
                leadingSide = countEntry.getKey();
                leadingCount = countEntry.getValue();
                tie = false;
            } else if (countEntry.getValue() == leadingCount) {
                tie = true;
            }
        }

        if (leadingSide == null) {
            LOGGER.debug("[Objective] {} at {}: uncontrolled - no eligible units in control radius {}",
                  objective.marker().generalName(), objective.position(), objective.marker().getControlRadius());
            return null;
        }
        if (tie) {
            LOGGER.debug("[Objective] {} at {}: uncontrolled - tied unit counts {}",
                  objective.marker().generalName(), objective.position(), unitCountsBySide);
            return null;
        }
        LOGGER.debug("[Objective] {} at {}: controlled by {} with unit counts {}",
              objective.marker().generalName(), objective.position(), displayName(leadingSide), unitCountsBySide);
        return leadingSide;
    }

    /**
     * Checks whether a unit counts toward controlling the given objective. Only deployed, on-board units within the
     * control radius count; crippled, prone, immobile and transported units are excluded, and flying units cannot
     * control (grounded units count normally).
     *
     * @param entity    The unit to check
     * @param objective The objective being evaluated
     *
     * @return {@code true} if the unit counts toward control of the objective
     */
    boolean isEligibleToControl(Entity entity, PlacedObjective objective) {
        Coords entityPosition = entity.getPosition();
        boolean isOnBoard = (entityPosition != null) && entity.isDeployed() && !entity.isOffBoard()
              && !entity.isDestroyed();
        if (!isOnBoard) {
            return false;
        }
        if (entityPosition.distance(objective.position()) > objective.marker().getControlRadius()) {
            return false;
        }
        // The unit is in range; log the excluded ones so a playtest can tell why a unit does not count.
        // TRACE because this runs in a loop over all entities each End Phase.
        if (entity.getTransportId() != Entity.NONE) {
            LOGGER.trace("[Objective] {} does not count for {}: being transported",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isCrippled()) {
            LOGGER.trace("[Objective] {} does not count for {}: crippled",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isProne()) {
            LOGGER.trace("[Objective] {} does not count for {}: prone",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isImmobile()) {
            LOGGER.trace("[Objective] {} does not count for {}: immobile",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            LOGGER.trace("[Objective] {} does not count for {}: flying units cannot control objectives",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        return true;
    }

    /**
     * Awards Victory Points per the standard control mission scoring: each End Phase, a side receives 1 VP for
     * controlling at least one friendly and at least one enemy objective, and 2 VP for controlling all objectives on
     * the board (with more than one objective in play).
     *
     * @param resolvedObjectives The control resolution of all active objectives this End Phase
     * @param tracker            The victory point tracker to award into
     */
    void awardStandardControlVictoryPoints(List<ResolvedObjective> resolvedObjectives, VictoryPointTracker tracker) {
        Set<Side> controllers = new LinkedHashSet<>();
        for (ResolvedObjective resolvedObjective : resolvedObjectives) {
            if (resolvedObjective.controller() != null) {
                controllers.add(resolvedObjective.controller());
            }
        }
        if (controllers.isEmpty()) {
            LOGGER.debug("[Objective] No objective is controlled this round - no victory points awarded");
            return;
        }

        int totalObjectives = resolvedObjectives.size();
        for (Side side : controllers) {
            int controlledFriendly = 0;
            int controlledEnemy = 0;
            for (ResolvedObjective resolvedObjective : resolvedObjectives) {
                if (!side.equals(resolvedObjective.controller())) {
                    continue;
                }
                // An objective with an unknown owner is not friendly to anyone and counts as enemy
                if (side.equals(resolvedObjective.owningSide())) {
                    controlledFriendly++;
                } else {
                    controlledEnemy++;
                }
            }

            int points = 0;
            String reason = "";
            if ((controlledFriendly + controlledEnemy == totalObjectives) && (totalObjectives > 1)) {
                points = 2;
                reason = "controls all " + totalObjectives + " objectives";
            } else if ((controlledFriendly >= 1) && (controlledEnemy >= 1)) {
                points = 1;
                reason = "controls " + controlledFriendly + " friendly and " + controlledEnemy
                      + " enemy objective(s)";
            }

            if (points > 0) {
                awardVictoryPoints(side, points, reason, tracker);
            } else {
                LOGGER.debug("[Objective] {} controls friendly: {}, enemy: {} objective(s) - standard control "
                            + "scoring requires at least one of each, no victory points awarded",
                      displayName(side), controlledFriendly, controlledEnemy);
            }
        }
    }

    private void awardVictoryPoints(Side side, int points, String reason, VictoryPointTracker tracker) {
        int gameRound = getGame().getCurrentRound();
        if (side.isTeam()) {
            tracker.awardToTeam(side.id(), points, gameRound, reason);
        } else {
            tracker.awardToPlayer(side.id(), points, gameRound, reason);
        }
        Report report = new Report(REPORT_OBJECTIVE_POINTS_AWARDED, Report.PUBLIC);
        report.add(displayName(side));
        report.add(points);
        addReport(report);
    }

    private void reportObjectiveControl(PlacedObjective objective, @Nullable Side controller) {
        Report report;
        if (controller == null) {
            report = new Report(REPORT_OBJECTIVE_UNCONTROLLED, Report.PUBLIC);
            report.add(objective.marker().generalName());
            report.add(objective.position().toFriendlyString());
        } else {
            report = new Report(REPORT_OBJECTIVE_CONTROLLED, Report.PUBLIC);
            report.add(objective.marker().generalName());
            report.add(objective.position().toFriendlyString());
            report.add(displayName(controller));
        }
        addReport(report);
    }

    /**
     * @param player A player, or {@code null} when unknown
     *
     * @return The scoring side of the player: their team, or the player itself when not on a team; {@code null} when
     *       the player is {@code null}
     */
    @Nullable
    private Side sideOfPlayer(@Nullable Player player) {
        if (player == null) {
            return null;
        }
        if (player.getTeam() != Player.TEAM_NONE) {
            return new Side(true, player.getTeam());
        }
        return new Side(false, player.getId());
    }

    @Nullable
    private Side sideOfPlayerId(int playerId) {
        Player player = getGame().getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("[Objective] Objective owner player ID {} does not exist in the game - the objective "
                  + "counts as an enemy objective for every side", playerId);
        }
        return sideOfPlayer(player);
    }

    private String displayName(Side side) {
        if (side.isTeam()) {
            return "Team " + side.id();
        }
        Player player = getGame().getPlayer(side.id());
        return (player == null) ? "Player " + side.id() : player.getName();
    }
}
