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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.client.ui.Messages;
import megamek.common.event.GameToastEvent;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.equipment.ObjectiveScoringScheme.HoldCounting;
import megamek.common.equipment.ObjectiveScoringScheme.SchemePreset;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.victory.VictoryPointTracker;

/**
 * End-Phase resolution for objective markers (Standard Missions, Objectives): determines which side controls each
 * {@link ObjectiveMarker} on the board and awards Victory Points into the game's {@link VictoryPointTracker} per the
 * standard control mission scoring.
 *
 * <P>Control: a side controls an objective when it has strictly more eligible units within the objective's control
 * radius than any other side. Crippled, prone, immobile and transported units do not count, and flying units
 * (airborne aerospace units, VTOLs and WiGEs at altitude) cannot control - grounded units count normally, except
 * VTOLs, which can never control. A tie, or no units in range, leaves the objective uncontrolled.</P>
 *
 * <P>Scoring (standard control mission, played with four counters, two per side): each End Phase a side receives 1 VP
 * for controlling at least one friendly and at least one enemy objective, and 2 VP for controlling all objectives on
 * the board. Other mission scoring rules (Objective Raid, Sensor Check) resolve in later parts of the Objectives
 * series, as do the objective variants and objective destruction.</P>
 *
 * <P>Sides are teams; a player without a team forms its own side.</P>
 */
class ObjectiveResolutionHandler extends AbstractTWRuleHandler {

    /** Feature logger for the victory hex/objective diagnostics; enabled via the log4j2.xml VictoryHex block. */
    private static final MMLogger LOGGER = MMLogger.create("megamek.feature.VictoryHex");

    private static final int REPORT_OBJECTIVE_CONTROLLED = 7117;
    private static final int REPORT_OBJECTIVE_UNCONTROLLED = 7118;
    private static final int REPORT_OBJECTIVE_POINTS_AWARDED = 7119;
    private static final int REPORT_POINT_SECURED = 7133;
    private static final int REPORT_POINT_FELL = 7134;
    private static final int REPORT_POINT_CAPTURED = 7135;
    private static final int REPORT_HOLD_PROGRESS = 7136;
    private static final int REPORT_GRIP_DRAINED = 7137;
    private static final int REPORT_CAPTURE_PROGRESS = 7138;
    private static final int REPORT_OBJECTIVE_CONTESTED = 7139;
    private static final int REPORT_HOLD_STILL_REQUIRED = 7140;
    private static final int REPORT_HOLD_BROKEN = 7141;
    private static final int REPORT_CAPTURE_PUSHED_BACK = 7142;

    /**
     * A scoring side. Normally this is a team; a player that is not on any team forms its own side.
     *
     * @param isTeam {@code true} when {@code id} is a team ID, {@code false} when it is a player ID
     * @param id     The team or player ID
     */
    record Side(boolean isTeam, int id) {}

    /**
     * An objective marker together with its board position: the key of the game's ground object map.
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
     * Resolves objectives for the current End Phase: determines the controller of every scorable objective marker,
     * reports the results and awards Victory Points per the standard control scoring. Does nothing when the game
     * has no objective markers or the {@link OptionsConstants#VICTORY_USE_OBJECTIVES} victory option is off -
     * markers can be placed without opting into objective scoring.
     */
    void resolveObjectives() {
        List<PlacedObjective> allObjectives = findAllObjectives();
        if (allObjectives.isEmpty()) {
            return;
        }
        if (!getGame().getOptions().booleanOption(OptionsConstants.VICTORY_USE_OBJECTIVES)) {
            // markers can be placed without opting into objective scoring - without this gate, the
            // victory points awarded here would decide the winner of a game that never enabled them
            LOGGER.info("[Objective] {} objective marker(s) on the board, but the use_objectives victory "
                  + "option is off - no control resolution or scoring", allObjectives.size());
            return;
        }

        List<PlacedObjective> activeObjectives = allObjectives.stream()
              .filter(this::isScorableObjective)
              .toList();
        if (activeObjectives.isEmpty()) {
            LOGGER.debug("[Objective] No scorable objectives - no control resolution");
            return;
        }

        List<Entity> entities = getGame().getEntitiesVector();
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(getGame());
        List<ResolvedObjective> standardObjectives = new ArrayList<>();
        for (PlacedObjective objective : activeObjectives) {
            Map<Side, Integer> presenceBySide = countEligiblePresence(objective, entities);
            Side controller = leadingSide(objective, presenceBySide);
            boolean contested = (controller == null) && !presenceBySide.isEmpty();
            Side owningSide = sideOfPlayerId(objective.marker().getOwnerId());
            storeControllerOnMarker(objective.marker(), controller);
            reportObjectiveControl(objective, controller, contested);
            switch (objective.marker().getScoringScheme().getPreset()) {
                case STANDARD -> standardObjectives.add(new ResolvedObjective(objective, owningSide, controller));
                case RAID -> { /* end-scored when the game ends; control is stored above for that */ }
                case HOLD -> resolveHoldCounter(objective, controller, tracker);
                case DEFEND -> resolveDefendCounter(objective, owningSide, entities, tracker);
                case CAPTURE -> resolveCaptureCounter(objective, controller, owningSide, tracker);
            }
        }
        // the printed-rules pairing scoring runs over the STANDARD points only; the other presets carry
        // their own counters and award their point value when they are decided
        awardStandardControlVictoryPoints(standardObjectives, tracker);
    }

    /**
     * Advances a {@code HOLD} point's held-turn counter: the controlling side's count grows each End Phase, and
     * with consecutive counting a turn without control resets every count. Reaching the threshold secures the
     * point for that side and awards the point's value once.
     */
    private void resolveHoldCounter(PlacedObjective objective, @Nullable Side controller,
          VictoryPointTracker tracker) {
        ObjectiveScoringScheme scheme = objective.marker().getScoringScheme();
        if (scheme.isDecided()) {
            return;
        }
        if (controller == null) {
            if (scheme.getHoldCounting() == HoldCounting.CONSECUTIVE) {
                int lostProgress = scheme.bestHeldTurns();
                scheme.resetAllHeldTurns();
                if (lostProgress > 0) {
                    // a player needs to know the streak died and what it costs: the full run is owed again
                    Report report = new Report(REPORT_HOLD_BROKEN, Report.PUBLIC);
                    report.add(objective.marker().generalName());
                    report.add(scheme.getThreshold());
                    addReport(report);
                    LOGGER.debug("[Objective] {} at {}: the hold was broken - {} held turn(s) lost",
                          objective.marker().generalName(), objective.position(), lostProgress);
                }
            } else if (scheme.bestHeldTurns() > 0) {
                Report report = new Report(REPORT_HOLD_STILL_REQUIRED, Report.PUBLIC);
                report.add(objective.marker().generalName());
                report.add(scheme.bestHeldTurns());
                report.add(scheme.getThreshold());
                addReport(report);
            }
            return;
        }
        int sideTeam = controller.isTeam() ? controller.id() : ObjectiveScoringScheme.NO_SIDE;
        int sidePlayer = controller.isTeam() ? ObjectiveScoringScheme.NO_SIDE : controller.id();
        int heldTurns = scheme.getHeldTurns(sideTeam, sidePlayer) + 1;
        if (scheme.getHoldCounting() == HoldCounting.CONSECUTIVE) {
            scheme.resetAllHeldTurns();
        }
        scheme.setHeldTurns(sideTeam, sidePlayer, heldTurns);
        LOGGER.debug("[Objective] {} at {}: {} has held for {} of {} turn(s) ({})",
              objective.marker().generalName(), objective.position(), displayName(controller), heldTurns,
              scheme.getThreshold(), scheme.getHoldCounting());
        if (heldTurns < scheme.getThreshold()) {
            Report report = new Report(REPORT_HOLD_PROGRESS, Report.PUBLIC);
            report.add(displayName(controller));
            report.add(objective.marker().generalName());
            report.add(heldTurns);
            report.add(scheme.getThreshold());
            report.add(scheme.getThreshold() - heldTurns);
            addReport(report);
        }
        if (heldTurns >= scheme.getThreshold()) {
            decidePoint(objective, controller, tracker, REPORT_POINT_SECURED);
        }
    }

    /**
     * Advances a {@code DEFEND} point's grip: any End Phase with an enemy of the owner present in the zone drains
     * it. At zero or below, the point falls to the enemy side with the most units present; a tie defers the fall
     * to a later End Phase. The taker is awarded the point's value once.
     */
    private void resolveDefendCounter(PlacedObjective objective, @Nullable Side owningSide, List<Entity> entities,
          VictoryPointTracker tracker) {
        ObjectiveScoringScheme scheme = objective.marker().getScoringScheme();
        if (scheme.isDecided()) {
            return;
        }
        Map<Side, Integer> presenceBySide = countEligiblePresence(objective, entities);
        if (owningSide != null) {
            presenceBySide.remove(owningSide);
        }
        if (presenceBySide.isEmpty()) {
            return;
        }
        scheme.setDefendGrip(scheme.getDefendGrip() - scheme.getRatePerTurn());
        LOGGER.debug("[Objective] {} at {}: enemy presence drains the grip to {} of {}",
              objective.marker().generalName(), objective.position(), scheme.getDefendGrip(),
              scheme.getThreshold());
        if (scheme.getDefendGrip() > 0) {
            Report report = new Report(REPORT_GRIP_DRAINED, Report.PUBLIC);
            report.add(objective.marker().generalName());
            report.add(scheme.getDefendGrip());
            report.add(scheme.getThreshold());
            addReport(report);
            return;
        }
        Side taker = null;
        int highestPresence = 0;
        boolean tie = false;
        for (Map.Entry<Side, Integer> presenceEntry : presenceBySide.entrySet()) {
            if (presenceEntry.getValue() > highestPresence) {
                taker = presenceEntry.getKey();
                highestPresence = presenceEntry.getValue();
                tie = false;
            } else if (presenceEntry.getValue() == highestPresence) {
                tie = true;
            }
        }
        if (tie || (taker == null)) {
            LOGGER.debug("[Objective] {} at {}: the grip is gone but the enemy presence is tied - the fall is "
                  + "deferred until one side leads", objective.marker().generalName(), objective.position());
            return;
        }
        decidePoint(objective, taker, tracker, REPORT_POINT_FELL);
    }

    /**
     * Advances a {@code CAPTURE} point's progress meter: an enemy of the owner controlling the zone adds progress,
     * the owner controlling it pushes every side's progress back (to a minimum of zero), and an uncontrolled turn
     * changes nothing. Reaching the threshold captures the point for that side and awards its value once.
     */
    private void resolveCaptureCounter(PlacedObjective objective, @Nullable Side controller,
          @Nullable Side owningSide, VictoryPointTracker tracker) {
        ObjectiveScoringScheme scheme = objective.marker().getScoringScheme();
        if (scheme.isDecided() || (controller == null)) {
            return;
        }
        if (controller.equals(owningSide)) {
            int progressBeforePushback = scheme.bestCaptureProgress();
            scheme.pushBackAllCaptureProgress(scheme.getRatePerTurn());
            LOGGER.debug("[Objective] {} at {}: the owner holds the point - capture progress pushed back",
                  objective.marker().generalName(), objective.position());
            if (progressBeforePushback > 0) {
                Report report = new Report(REPORT_CAPTURE_PUSHED_BACK, Report.PUBLIC);
                report.add(displayName(controller));
                report.add(objective.marker().generalName());
                report.add(scheme.bestCaptureProgress());
                report.add(scheme.getThreshold());
                addReport(report);
            }
            return;
        }
        int sideTeam = controller.isTeam() ? controller.id() : ObjectiveScoringScheme.NO_SIDE;
        int sidePlayer = controller.isTeam() ? ObjectiveScoringScheme.NO_SIDE : controller.id();
        int progress = Math.min(scheme.getThreshold(),
              scheme.getCaptureProgress(sideTeam, sidePlayer) + scheme.getRatePerTurn());
        scheme.setCaptureProgress(sideTeam, sidePlayer, progress);
        LOGGER.debug("[Objective] {} at {}: {} pushes the capture progress to {} of {}",
              objective.marker().generalName(), objective.position(), displayName(controller), progress,
              scheme.getThreshold());
        if (progress < scheme.getThreshold()) {
            Report report = new Report(REPORT_CAPTURE_PROGRESS, Report.PUBLIC);
            report.add(displayName(controller));
            report.add(objective.marker().generalName());
            report.add(progress);
            report.add(scheme.getThreshold());
            addReport(report);
        }
        if (progress >= scheme.getThreshold()) {
            decidePoint(objective, controller, tracker, REPORT_POINT_CAPTURED);
        }
    }

    /**
     * Marks a counter-driven point decided by the given side, awards the point's victory point value to it once,
     * and reports the state change.
     */
    private void decidePoint(PlacedObjective objective, Side decidingSide, VictoryPointTracker tracker,
          int reportId) {
        ObjectiveScoringScheme scheme = objective.marker().getScoringScheme();
        scheme.setSecuredBy(decidingSide.isTeam() ? decidingSide.id() : ObjectiveScoringScheme.NO_SIDE,
              decidingSide.isTeam() ? ObjectiveScoringScheme.NO_SIDE : decidingSide.id());
        int points = objective.marker().getVictoryPointValue();
        if (!scheme.isVictoryPointsAwarded()) {
            scheme.setVictoryPointsAwarded(true);
            if (decidingSide.isTeam()) {
                tracker.awardToTeam(decidingSide.id(), points, getGame().getCurrentRound(),
                      "decided " + objective.marker().generalName());
            } else {
                tracker.awardToPlayer(decidingSide.id(), points, getGame().getCurrentRound(),
                      "decided " + objective.marker().generalName());
            }
        }
        Report report = new Report(reportId, Report.PUBLIC);
        report.add(objective.marker().generalName());
        report.add(objective.position().toFriendlyString());
        report.add(displayName(decidingSide));
        report.add(points);
        addReport(report);
        LOGGER.info("[Objective] {} at {} is decided by {} (+{} VP)", objective.marker().generalName(),
              objective.position(), displayName(decidingSide), points);
    }

    /**
     * @return how many eligible units each side has within the objective's control radius, using the same
     *       eligibility rules as control (see {@link #isEligibleToControl(Entity, PlacedObjective)})
     */
    private Map<Side, Integer> countEligiblePresence(PlacedObjective objective, List<Entity> entities) {
        Map<Side, Integer> presenceBySide = new HashMap<>();
        for (Entity entity : entities) {
            if (!isEligibleToControl(entity, objective)) {
                continue;
            }
            Side side = sideOfPlayer(entity.getOwner());
            if (side != null) {
                presenceBySide.merge(side, 1, Integer::sum);
            }
        }
        return presenceBySide;
    }

    /**
     * Checks whether an objective participates in control resolution and scoring: destroyed objectives and
     * unconfirmed Potential Objective candidates do not (RAW: only confirmed objectives are worth Victory Points;
     * confirmation by scanning arrives with the Sensor Check part of the Objectives series). A False Objective flag
     * has no effect with a running VP score (RAW: the variant is not used in such missions).
     */
    private boolean isScorableObjective(PlacedObjective objective) {
        ObjectiveMarker marker = objective.marker();
        if (marker.isDestroyed()) {
            return false;
        }
        if (marker.isPotential() && !marker.isConfirmed()) {
            LOGGER.debug("[Objective] {} at {} is an unconfirmed objective candidate - it cannot score until "
                  + "confirmed by a scan", marker.generalName(), objective.position());
            return false;
        }
        if (marker.isFalseObjective()) {
            LOGGER.debug("[Objective] {} at {} is flagged as a False Objective, but the variant is not used with "
                        + "a running VP score - the flag has no effect",
                  marker.generalName(), objective.position());
        }
        return true;
    }

    /**
     * Sends a toast when a unit's move ends inside a control zone it did not start in, so entering a zone is
     * announced the moment it happens instead of only surfacing in the End Phase report. Fires once per completed
     * move per zone; shuffling within a zone or ending outside one says nothing.
     *
     * @param movedEntity      the unit that finished moving
     * @param startingPosition where the unit stood before the move, or {@code null} (e.g. it was just deployed)
     */
    void toastZoneEntry(Entity movedEntity, @Nullable Coords startingPosition) {
        Coords endingPosition = movedEntity.getPosition();
        if ((endingPosition == null) || endingPosition.equals(startingPosition)) {
            return;
        }
        for (PlacedObjective objective : findAllObjectives()) {
            if (objective.marker().isDestroyed()) {
                continue;
            }
            int radius = objective.marker().getControlRadius();
            boolean wasInside = (startingPosition != null)
                  && (startingPosition.distance(objective.position()) <= radius);
            boolean isInside = endingPosition.distance(objective.position()) <= radius;
            if (isInside && !wasInside) {
                gameManager.sendToast(GameToastEvent.Level.INFO,
                      Messages.getString("Objective.toast.zoneEntered", movedEntity.getShortName(),
                            objective.marker().generalName()), movedEntity);
                LOGGER.debug("[Objective] {} entered the control zone of {} at {}",
                      movedEntity.getShortName(), objective.marker().generalName(), objective.position());
            }
        }
    }

    /** @return All objective markers placed on the ground, including destroyed ones, with their map positions */
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
        return leadingSide(objective, countEligiblePresence(objective, entities));
    }

    /**
     * @param objective       the objective being evaluated
     * @param unitCountsBySide how many eligible units each side has in the objective's zone
     *
     * @return the side with strictly more eligible units than any other, or {@code null} when the zone is empty
     *       or the leading counts are tied (a contested zone)
     */
    @Nullable
    private Side leadingSide(PlacedObjective objective, Map<Side, Integer> unitCountsBySide) {
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
        // DEBUG is safe here: only in-range units reach these lines, so the volume stays bounded.
        if (entity.getTransportId() != Entity.NONE) {
            LOGGER.debug("[Objective] {} does not count for {}: being transported",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isCrippled()) {
            LOGGER.debug("[Objective] {} does not count for {}: crippled",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isProne()) {
            LOGGER.debug("[Objective] {} does not count for {}: prone",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isImmobile()) {
            LOGGER.debug("[Objective] {} does not count for {}: immobile",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
            LOGGER.debug("[Objective] {} does not count for {}: flying units cannot control objectives",
                  entity.getShortName(), objective.marker().generalName());
            return false;
        }
        if (entity.getMovementMode().isVTOL()) {
            // RAW (Control Radius - Assets): air and VTOL vehicle Assets can never control objectives,
            // even when landed
            LOGGER.debug("[Objective] {} does not count for {}: VTOL units cannot control objectives",
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

    /** Awards victory points to the side's running tally and reports the award. */
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


    /**
     * Records the resolved controller on the marker itself, so state-based victory triggers
     * (objective control conditions) can read it without re-running the control algorithm.
     */
    private void storeControllerOnMarker(ObjectiveMarker marker, @Nullable Side controller) {
        if (controller == null) {
            if (marker.getScoringScheme().retainsControlWhenEmpty()) {
                // the point keeps whoever last held it: an empty zone is not the same as a lost one, which
                // is what lets a mission use more points than a side has units to garrison
                return;
            }
            marker.setController(ObjectiveMarker.NO_CONTROLLER, ObjectiveMarker.NO_CONTROLLER);
        } else if (controller.isTeam()) {
            marker.setController(controller.id(), ObjectiveMarker.NO_CONTROLLER);
        } else {
            marker.setController(ObjectiveMarker.NO_CONTROLLER, controller.id());
        }
    }

    private void reportObjectiveControl(PlacedObjective objective, @Nullable Side controller,
          boolean contested) {
        // the control line names the kind of point, so a Defend point reads as one in the round report
        String schemeWord = Messages.getString("VictoryHex.word."
              + objective.marker().getScoringScheme().getPreset().name().toLowerCase(Locale.ROOT));
        Report report;
        if (contested) {
            // equal forces deadlock the zone: nothing gains, nothing is lost
            report = new Report(REPORT_OBJECTIVE_CONTESTED, Report.PUBLIC);
            report.add(schemeWord);
            report.add(objective.marker().generalName());
            report.add(objective.position().toFriendlyString());
        } else if (controller == null) {
            report = new Report(REPORT_OBJECTIVE_UNCONTROLLED, Report.PUBLIC);
            report.add(schemeWord);
            report.add(objective.marker().generalName());
            report.add(objective.position().toFriendlyString());
        } else {
            report = new Report(REPORT_OBJECTIVE_CONTROLLED, Report.PUBLIC);
            report.add(schemeWord);
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
