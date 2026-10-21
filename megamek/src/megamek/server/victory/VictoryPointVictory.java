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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme.SchemePreset;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.scriptedEvents.TriggeredEvent;

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

    private static final MMLogger LOGGER = MMLogger.create("megamek.feature.VictoryHex");
    private static final long serialVersionUID = 1L;

    private static final int REPORT_VICTORY_POINT_TOTAL = 7115;
    private static final int REPORT_VICTORY_POINTS_TIED = 7116;
    private static final int REPORT_RAID_OBJECTIVE_SCORED = 7131;
    private static final int REPORT_RAID_CARRIED_OFF = 7174;
    private static final int REPORT_DECOY_CARRIED_OFF = 7175;
    private static final int REPORT_RAID_CARRIED = 7176;
    private static final int REPORT_SUDDEN_DEATH = 7143;
    private static final int REPORT_WIN_THRESHOLD_REACHED = 7144;
    private static final int REPORT_VICTORY_LEVEL = 7146;
    /** A decoy (a False objective) reached the moment it would have paid; shared with the control resolution. */
    public static final int REPORT_DECOY_AT_PAYOUT = 7145;

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> context) {
        if (game.gameTimerIsExpired()) {
            LOGGER.debug("[VP] Game turn limit reached; resolving victory points");
            return checkAtGameEnd(game, context);
        }
        return checkEarlyEnd(game, context);
    }

    /**
     * Checks the mid-game enders: sudden death (the game ends the moment any control point is decided) and the
     * victory point win threshold. Both resolve the running tally into a winner immediately instead of waiting
     * for the game clock.
     *
     * @param game    The current {@link Game}
     * @param context The victory context holding the {@link VictoryPointTracker}
     *
     * @return The resolved result when an ender fired; {@link VictoryResult#noResult()} otherwise
     */
    private VictoryResult checkEarlyEnd(Game game, Map<String, Object> context) {
        if (!game.getOptions().booleanOption(OptionsConstants.VICTORY_USE_OBJECTIVES)) {
            return VictoryResult.noResult();
        }
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(context);
        if (tracker == null) {
            return VictoryResult.noResult();
        }

        boolean isSuddenDeath = game.getOptions().booleanOption(OptionsConstants.VICTORY_VP_SUDDEN_DEATH);
        if (isSuddenDeath && tracker.isPointDecided()) {
            LOGGER.info("[VP] Sudden death: a control point was decided - resolving victory points now");
            VictoryResult result = checkAtGameEnd(game, context);
            result.addReport(new Report(REPORT_SUDDEN_DEATH, Report.PUBLIC));
            return result;
        }

        int winThreshold = game.getOptions().intOption(OptionsConstants.VICTORY_VP_WIN_THRESHOLD);
        boolean isWinThresholdReached = (winThreshold > 0) && soleLeaderIsAtOrAbove(tracker, winThreshold);
        if (isWinThresholdReached) {
            LOGGER.info("[VP] The victory point win threshold of {} was reached - resolving now", winThreshold);
            VictoryResult result = checkAtGameEnd(game, context);
            Report report = new Report(REPORT_WIN_THRESHOLD_REACHED, Report.PUBLIC);
            report.add(winThreshold);
            result.addReport(report);
            return result;
        }

        return VictoryResult.noResult();
    }

    /**
     * @param tracker   the tally
     * @param threshold the win threshold, above zero
     *
     * @return {@code true} when exactly one side holds the highest total and that total is at or above the
     *       threshold - a shared high score keeps the game going, first to pull ahead at the score wins
     */
    private boolean soleLeaderIsAtOrAbove(VictoryPointTracker tracker, int threshold) {
        List<Integer> totals = allSideTotals(tracker);
        int best = totals.stream().mapToInt(Integer::intValue).max().orElse(Integer.MIN_VALUE);
        long sidesAtBest = totals.stream().filter(total -> total == best).count();
        return (best >= threshold) && (sidesAtBest == 1);
    }

    /** @return every scoring side's current total, players and teams alike */
    private List<Integer> allSideTotals(VictoryPointTracker tracker) {
        List<Integer> totals = new ArrayList<>();
        for (int playerId : tracker.getScoringPlayers()) {
            totals.add(tracker.getPlayerVictoryPoints(playerId));
        }
        for (int teamId : tracker.getScoringTeams()) {
            totals.add(tracker.getTeamVictoryPoints(teamId));
        }
        return totals;
    }

    /**
     * @param game the game, using its final (post-lobby) options and scripted events
     *
     * @return {@code true} when the game has any way to resolve scored victory points into a result: the game
     *       turn limit, the victory point win threshold, sudden death, or a game-ending scripted event
     */
    public static boolean gameHasVictoryPointResolution(Game game) {
        boolean hasTurnLimit = game.getOptions().booleanOption(OptionsConstants.VICTORY_USE_GAME_TURN_LIMIT);
        boolean hasWinThreshold = game.getOptions().intOption(OptionsConstants.VICTORY_VP_WIN_THRESHOLD) > 0;
        boolean hasSuddenDeath = game.getOptions().booleanOption(OptionsConstants.VICTORY_VP_SUDDEN_DEATH);
        boolean hasGameEndEvent = game.scriptedEvents().stream().anyMatch(TriggeredEvent::isGameEnding);
        return hasTurnLimit || hasWinThreshold || hasSuddenDeath || hasGameEndEvent;
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

        VictoryPointTracker tracker = VictoryPointTracker.findTracker(context);
        List<Report> endScoringReports = new ArrayList<>();
        if (victoryPointScoringEnabled) {
            tracker = VictoryPointTracker.getTracker(game);
            endScoringReports = awardRaidPoints(game, tracker);
        }
        if ((tracker == null) || !tracker.hasAnyScore()) {
            if (victoryPointScoringEnabled) {
                LOGGER.info("[VP] Game ends with no victory points scored by any side; the game is a draw");
                // keep the end-of-game lines, such as a Raid decoy exposed, even though nobody scored
                VictoryResult draw = VictoryResult.drawResult();
                draw.addReports(endScoringReports);
                return draw;
            }
            LOGGER.debug("[VP] No victory points were scored and VP scoring is not enabled; not applicable");
            return VictoryResult.noResult();
        }
        return buildResult(game, tracker, endScoringReports);
    }

    /**
     * Objective Raid end-scoring, once when the game ends: every point using the {@code RAID} scheme awards its
     * victory point value to its controller per the last End Phase control resolution, and a carried one to its
     * carrier's side (see {@link #awardCarriedRaidPoints}). Uncontrolled or destroyed points award nothing.
     *
     * @return the end-scoring reports, shown with the victory result
     */
    private List<Report> awardRaidPoints(Game game, VictoryPointTracker tracker) {
        List<Report> endScoringReports = new ArrayList<>();
        if (tracker.isEndScoringDone()) {
            return endScoringReports;
        }
        tracker.setEndScoringDone(true);
        for (Map.Entry<Coords, List<ICarryable>> hexObjects : game.getGroundObjects().entrySet()) {
            for (ICarryable groundObject : hexObjects.getValue()) {
                if (!(groundObject instanceof ObjectiveMarker marker) || marker.isDestroyed()
                      || (marker.getScoringScheme().getPreset() != SchemePreset.RAID)) {
                    continue;
                }
                if (marker.isFalseObjective()) {
                    // a decoy is exposed at the moment it would pay, which for a Raid point is now
                    LOGGER.info("[VP] Raid point {} is a decoy - nobody scores it", marker.generalName());
                    Report decoyReport = new Report(REPORT_DECOY_AT_PAYOUT, Report.PUBLIC);
                    decoyReport.add(marker.generalName());
                    decoyReport.add(hexObjects.getKey().getBoardNum());
                    endScoringReports.add(decoyReport);
                    continue;
                }
                boolean isControlled = (marker.getControllingTeam() != ObjectiveMarker.NO_CONTROLLER)
                      || (marker.getControllingPlayerId() != ObjectiveMarker.NO_CONTROLLER);
                if (!isControlled) {
                    LOGGER.debug("[VP] Raid point {} is uncontrolled at mission end - no points",
                          marker.generalName());
                    continue;
                }
                int points = marker.getVictoryPointValue();
                String sideName;
                if (marker.getControllingTeam() != ObjectiveMarker.NO_CONTROLLER) {
                    tracker.awardToTeam(marker.getControllingTeam(), points, game.getCurrentRound(),
                          "Raid: controls " + marker.generalName());
                    sideName = teamName(marker.getControllingTeam());
                } else {
                    tracker.awardToPlayer(marker.getControllingPlayerId(), points, game.getCurrentRound(),
                          "Raid: controls " + marker.generalName());
                    sideName = playerDisplayName(game, marker.getControllingPlayerId());
                }
                Report report = new Report(REPORT_RAID_OBJECTIVE_SCORED, Report.PUBLIC);
                report.add(sideName);
                report.add(marker.generalName());
                report.add(points);
                endScoringReports.add(report);
                LOGGER.info("[VP] Raid: {} controls {} at mission end (+{} VP)", sideName,
                      marker.generalName(), points);
            }
        }
        awardCarriedRaidPoints(game, tracker, endScoringReports);
        return endScoringReports;
    }

    /**
     * A carried Raid point pays its carrier's side. A Mek still on the board carrying one holds it when the game ends
     * (RAW: carrying an objective controls it), and one that fled the battlefield with it took it off the field
     * (RAW: an objective carried out through a legal departure edge counts as taken off the field). Fleeing is only
     * allowed through a legal edge, so every unit that fled qualifies. So does one pushed off the battlefield still
     * carrying it: a push over any other edge has already left the objective behind on the board.
     */
    private void awardCarriedRaidPoints(Game game, VictoryPointTracker tracker, List<Report> endScoringReports) {
        for (Entity carrier : game.getEntitiesVector()) {
            if (carrier.getPosition() != null) {
                for (ObjectiveMarker marker : carriedRaidPoints(carrier)) {
                    awardCarriedRaidPoint(game, tracker, carrier, marker, false, endScoringReports);
                }
            }
        }
        for (Enumeration<Entity> retreated = game.getRetreatedEntities(); retreated.hasMoreElements(); ) {
            Entity carrier = retreated.nextElement();
            boolean isFled = carrier.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT;
            boolean isPushedOff = carrier.getRemovalCondition() == IEntityRemovalConditions.REMOVE_PUSHED;
            if (isFled || isPushedOff) {
                for (ObjectiveMarker marker : carriedRaidPoints(carrier)) {
                    awardCarriedRaidPoint(game, tracker, carrier, marker, true, endScoringReports);
                }
            }
        }
    }

    /** @return the intact Raid points the unit is carrying */
    private static List<ObjectiveMarker> carriedRaidPoints(Entity carrier) {
        List<ObjectiveMarker> raidPoints = new ArrayList<>();
        for (ICarryable carriedObject : carrier.getDistinctCarriedObjects()) {
            if ((carriedObject instanceof ObjectiveMarker marker) && !marker.isDestroyed()
                  && (marker.getScoringScheme().getPreset() == SchemePreset.RAID)) {
                raidPoints.add(marker);
            }
        }
        return raidPoints;
    }

    private void awardCarriedRaidPoint(Game game, VictoryPointTracker tracker, Entity carrier, ObjectiveMarker marker,
          boolean isCarriedOff, List<Report> endScoringReports) {
        if (marker.isFalseObjective()) {
            // a decoy is exposed at the moment it would pay, which for a Raid point is now
            LOGGER.info("[VP] Raid point {} carried by {} is a decoy - nobody scores it", marker.generalName(),
                  carrier.getShortName());
            Report decoyReport = new Report(isCarriedOff ? REPORT_DECOY_CARRIED_OFF : REPORT_DECOY_AT_PAYOUT,
                  Report.PUBLIC);
            decoyReport.add(marker.generalName());
            if (!isCarriedOff) {
                decoyReport.add(carrier.getPosition().getBoardNum());
            }
            endScoringReports.add(decoyReport);
            return;
        }
        Player owner = carrier.getOwner();
        if (owner == null) {
            LOGGER.warn("[VP] Raid point {} is carried by {}, which has no owner - nobody scores it",
                  marker.generalName(), carrier.getShortName());
            return;
        }
        int points = marker.getVictoryPointValue();
        String reason = "Raid: carries " + marker.generalName();
        String sideName;
        if (owner.getTeam() != Player.TEAM_NONE) {
            tracker.awardToTeam(owner.getTeam(), points, game.getCurrentRound(), reason);
            sideName = teamName(owner.getTeam());
        } else {
            tracker.awardToPlayer(owner.getId(), points, game.getCurrentRound(), reason);
            sideName = owner.getName();
        }
        Report report = new Report(isCarriedOff ? REPORT_RAID_CARRIED_OFF : REPORT_RAID_CARRIED, Report.PUBLIC);
        report.add(sideName);
        report.add(marker.generalName());
        report.add(points);
        endScoringReports.add(report);
        LOGGER.info("[VP] Raid: {} {} {} at mission end (+{} VP)", sideName,
              isCarriedOff ? "carried off the field" : "carries", marker.generalName(), points);
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
            result.addReport(victoryPointReport(teamName(teamId), points));
        }

        if (result.isDraw()) {
            result.addReport(new Report(REPORT_VICTORY_POINTS_TIED, Report.PUBLIC));
            LOGGER.info("[VP] Game ends with tied victory points; the game is a draw");
        } else {
            LOGGER.info("[VP] Game ends by victory points; winning player ID: {}, winning team: {}",
                  result.getWinningPlayer(), result.getWinningTeam());
            declareVictoryLevel(game, tracker, result);
        }
        return result;
    }

    /**
     * Declares the winner's victory level from the scenario's graded scale, when one is defined: the first band
     * whose bound covers the winner's final total names the victory (e.g. "Pyrrhic victory" up to 10, "Major
     * victory" above 20). Games without a scale keep the plain result.
     */
    private void declareVictoryLevel(Game game, VictoryPointTracker tracker, VictoryResult result) {
        List<VictoryPointLevel> levels = game.getVictoryPointLevels();
        if (levels.isEmpty()) {
            return;
        }
        int winnerPoints = (result.getWinningTeam() != Player.TEAM_NONE)
              ? tracker.getTeamVictoryPoints(result.getWinningTeam())
              : tracker.getPlayerVictoryPoints(result.getWinningPlayer());
        String winnerName = (result.getWinningTeam() != Player.TEAM_NONE)
              ? teamName(result.getWinningTeam())
              : playerDisplayName(game, result.getWinningPlayer());
        for (VictoryPointLevel level : levels) {
            if (winnerPoints <= level.getUpTo()) {
                Report report = new Report(REPORT_VICTORY_LEVEL, Report.PUBLIC);
                report.add(winnerName);
                report.add(level.getName());
                report.add(winnerPoints);
                result.addReport(report);
                LOGGER.info("[VP] {} achieves a {} ({} victory points)", winnerName, level.getName(),
                      winnerPoints);
                return;
            }
        }
        LOGGER.warn("[VP] The winner's total of {} is above every victory level bound - no level declared; "
              + "give the last level no upTo bound to catch every total", winnerPoints);
    }

    /** @return the team as a player reads it in the report */
    private static String teamName(int teamId) {
        return Messages.getString("ObjectiveScan.teamName", teamId);
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
