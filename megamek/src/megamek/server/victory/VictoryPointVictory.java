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

package megamek.server.victory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Resolves the winner of a game by cumulative Victory Points (VP), as used by objective-based missions: VP are awarded
 * during play into the game's {@link VictoryPointTracker}, and when the game ends (its duration expires through the
 * game turn limit or a game-ending scripted event), the side with the highest VP total wins. Tied VP totals are a
 * draw. This is a scoring model distinct from the boolean "first condition to fire" victory conditions.
 *
 * <P>While the game runs, this condition never ends the game; {@link #checkVictory(Game, Map)} returns
 * {@link VictoryResult#noResult()} until the game turn limit expires. When the game ends through a scripted event
 * instead, {@link VictoryHelper} calls {@link #checkAtGameEnd(Game, Map)} directly.</P>
 *
 * <P>When no VP have been scored at game end, the result depends on the
 * {@link OptionsConstants#VICTORY_USE_OBJECTIVES} game option: if VP scoring is enabled, a scoreless game ends in a
 * draw; otherwise this condition is not applicable and other rules decide the outcome. VP awarded by scenario events
 * are resolved even when the game option is off.</P>
 */
public class VictoryPointVictory implements VictoryCondition, Serializable {

    private static final MMLogger LOGGER = MMLogger.create(VictoryPointVictory.class);
    private static final long serialVersionUID = 1L;

    private static final int REPORT_VICTORY_POINT_TOTAL = 7115;
    private static final int REPORT_VICTORY_POINTS_TIED = 7116;
    private static final int REPORT_RAID_OBJECTIVE_SCORED = 7131;
    private static final int REPORT_FALSE_OBJECTIVE = 7132;

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> context) {
        if (!game.gameTimerIsExpired()) {
            return VictoryResult.noResult();
        }
        LOGGER.debug("[VP] Game turn limit reached; resolving victory points");
        return checkAtGameEnd(game, context);
    }

    /**
     * Resolves the victory point totals into a game result. This is called when the game is known to end right now,
     * either through the expired game turn limit or through a game-ending scripted event.
     *
     * @param game    The current {@link Game}
     * @param context The victory context holding the {@link VictoryPointTracker}, or {@code null} when no context
     *                exists (no points can have been scored then)
     *
     * @return The winner by highest VP; a draw on tied VP or when VP scoring is enabled but scoreless;
     *       {@link VictoryResult#noResult()} when VP play no role in this game
     */
    public VictoryResult checkAtGameEnd(Game game, @Nullable Map<String, Object> context) {
        boolean victoryPointScoringEnabled = game.getOptions()
              .booleanOption(OptionsConstants.VICTORY_USE_OBJECTIVES);
        boolean objectiveRaid = game.getOptions().booleanOption(OptionsConstants.VICTORY_OBJECTIVE_RAID);

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(context);
        List<Report> endScoringReports = new ArrayList<>();
        if (objectiveRaid) {
            tracker = VictoryPointTracker.getTracker(game);
            endScoringReports = awardObjectiveRaidPoints(game, tracker);
        }

        if ((tracker == null) || !tracker.hasAnyScore()) {
            if (victoryPointScoringEnabled || objectiveRaid) {
                LOGGER.info("[VP] Game ends with no victory points scored by any side; the game is a draw");
                VictoryResult drawResult = VictoryResult.drawResult();
                endScoringReports.forEach(drawResult::addReport);
                return drawResult;
            }
            LOGGER.debug("[VP] No victory points were scored and VP scoring is not enabled; not applicable");
            return VictoryResult.noResult();
        }
        return buildResult(game, tracker, endScoringReports);
    }

    /**
     * Objective Raid mission end-scoring: once, when the game ends, every controlled objective awards its victory
     * point value to its controlling side (per the last End Phase control resolution). Unconfirmed Potential
     * Objective candidates and destroyed objectives score nothing. With more than two objectives in play, a
     * controlled False Objective counts nothing on a 1D6 roll of 1 (RAW: the variant is not used with two or fewer
     * objectives).
     *
     * @return The reports of the end-scoring, to be shown with the victory result
     */
    private List<Report> awardObjectiveRaidPoints(Game game, VictoryPointTracker tracker) {
        List<Report> endScoringReports = new ArrayList<>();
        if (tracker.isEndScoringDone()) {
            return endScoringReports;
        }
        tracker.setEndScoringDone(true);

        List<ObjectiveMarker> scorableMarkers = findScorableMarkers(game);
        boolean falseRollsApply = scorableMarkers.size() > 2;
        LOGGER.info("[VP] Objective Raid end-scoring: {} scorable objective(s), False Objective rolls {}",
              scorableMarkers.size(), falseRollsApply ? "apply" : "do not apply (two or fewer objectives)");

        for (ObjectiveMarker marker : scorableMarkers) {
            boolean isControlled = (marker.getControllingTeam() != ObjectiveMarker.NO_CONTROLLER)
                  || (marker.getControllingPlayerId() != ObjectiveMarker.NO_CONTROLLER);
            if (!isControlled) {
                LOGGER.debug("[VP] Objective Raid: {} is uncontrolled at mission end - no points",
                      marker.generalName());
                continue;
            }
            if (falseRollsApply && marker.isFalseObjective()) {
                int falseRoll = rollFalseObjectiveCheck();
                LOGGER.info("[VP] Objective Raid: {} is a False Objective - rolled {} ({})",
                      marker.generalName(), falseRoll, (falseRoll == 1) ? "counts nothing" : "counts normally");
                if (falseRoll == 1) {
                    Report report = new Report(REPORT_FALSE_OBJECTIVE, Report.PUBLIC);
                    report.add(marker.generalName());
                    endScoringReports.add(report);
                    continue;
                }
            }
            int points = marker.getVictoryPointValue();
            String sideName;
            if (marker.getControllingTeam() != ObjectiveMarker.NO_CONTROLLER) {
                tracker.awardToTeam(marker.getControllingTeam(), points, game.getCurrentRound(),
                      "Objective Raid: controls " + marker.generalName());
                sideName = "Team " + marker.getControllingTeam();
            } else {
                tracker.awardToPlayer(marker.getControllingPlayerId(), points, game.getCurrentRound(),
                      "Objective Raid: controls " + marker.generalName());
                sideName = playerDisplayName(game, marker.getControllingPlayerId());
            }
            Report report = new Report(REPORT_RAID_OBJECTIVE_SCORED, Report.PUBLIC);
            report.add(sideName);
            report.add(marker.generalName());
            report.add(points);
            endScoringReports.add(report);
        }
        return endScoringReports;
    }

    /**
     * @return All objective markers that can score in the Objective Raid end-scoring: on the ground or carried, not
     *       destroyed, and not unconfirmed Potential Objective candidates
     */
    private List<ObjectiveMarker> findScorableMarkers(Game game) {
        List<ObjectiveMarker> scorableMarkers = new ArrayList<>();
        for (List<ICarryable> groundObjects : game.getGroundObjects().values()) {
            for (ICarryable groundObject : groundObjects) {
                addIfScorable(scorableMarkers, groundObject);
            }
        }
        for (Entity entity : game.getEntitiesVector()) {
            for (ICarryable carriedObject : entity.getDistinctCarriedObjects()) {
                addIfScorable(scorableMarkers, carriedObject);
            }
        }
        return scorableMarkers;
    }

    private void addIfScorable(List<ObjectiveMarker> scorableMarkers, ICarryable carryable) {
        if ((carryable instanceof ObjectiveMarker marker) && !marker.isDestroyed()
              && !(marker.isPotential() && !marker.isConfirmed())) {
            scorableMarkers.add(marker);
        }
    }

    /** Seam for tests: the 1D6 False Objective end roll (a 1 means the objective counts nothing). */
    int rollFalseObjectiveCheck() {
        return Compute.d6();
    }

    private VictoryResult buildResult(Game game, VictoryPointTracker tracker, List<Report> endScoringReports) {
        VictoryResult result = new VictoryResult(true);
        endScoringReports.forEach(result::addReport);
        for (int playerId : tracker.getScoringPlayers()) {
            int points = tracker.getPlayerVictoryPoints(playerId);
            result.setPlayerScore(playerId, points);
            result.addReport(victoryPointReport(playerDisplayName(game, playerId), points));
        }
        for (int teamId : tracker.getScoringTeams()) {
            int points = tracker.getTeamVictoryPoints(teamId);
            result.setTeamScore(teamId, points);
            result.addReport(victoryPointReport("Team " + teamId, points));
        }

        if (result.isDraw()) {
            result.addReport(new Report(REPORT_VICTORY_POINTS_TIED, Report.PUBLIC));
            LOGGER.info("[VP] Game ends with tied victory points; the game is a draw");
        } else {
            LOGGER.info("[VP] Game ends by victory points; winning player ID: {}, winning team: {}",
                  result.getWinningPlayer(), result.getWinningTeam());
        }
        return result;
    }

    private String playerDisplayName(Game game, int playerId) {
        Player player = game.getPlayer(playerId);
        return (player == null) ? "Player " + playerId : player.getName();
    }

    private Report victoryPointReport(String sideName, int points) {
        Report report = new Report(REPORT_VICTORY_POINT_TOTAL, Report.PUBLIC);
        report.add(sideName);
        report.add(points);
        return report;
    }
}
