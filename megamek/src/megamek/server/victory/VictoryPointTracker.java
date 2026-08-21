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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.logging.MMLogger;

/**
 * Holds the running Victory Point (VP) tally of a game. Victory points are awarded during play (typically in the End
 * Phase, e.g. for controlling or scanning objectives) and resolved into a winner by {@link VictoryPointVictory} when
 * the game ends: the side with the highest VP total wins; ties are a draw.
 *
 * <P>The tracker is stored in the game's victory context (see {@link Game#getVictoryContext()}) under
 * {@link #VICTORY_CONTEXT_KEY}, so it is part of savegames and survives save/load. Use {@link #getTracker(Game)} to
 * obtain (and lazily create) the tracker for awarding points and {@link #findTracker(Map)} for read-only access from
 * victory condition checks.</P>
 *
 * <P>Points can be awarded to players or to teams. Within one game, awards should consistently target either players
 * or teams; when both are present, {@link VictoryResult} resolves them independently (matching the behavior of the
 * existing victory conditions).</P>
 */
public class VictoryPointTracker implements Serializable {

    /** The key under which the tracker is stored in the game's victory context map. */
    public static final String VICTORY_CONTEXT_KEY = "VictoryPointTracker";

    private static final MMLogger LOGGER = MMLogger.create(VictoryPointTracker.class);
    private static final long serialVersionUID = 1L;

    /** The type of recipient of a victory point award. */
    public enum Recipient {
        PLAYER,
        TEAM
    }

    /**
     * A single victory point award, kept for reporting and diagnosis.
     *
     * @param gameRound   The game round in which the points were awarded
     * @param recipient   Whether the points went to a player or a team
     * @param recipientId The ID of the receiving player or team
     * @param points      The number of victory points awarded (may be negative for penalties)
     * @param reason      A human-readable reason for the award, e.g. "controls Objective 2"
     */
    public record VictoryPointAward(int gameRound, Recipient recipient, int recipientId, int points, String reason)
          implements Serializable {}

    private final Map<Integer, Integer> playerVictoryPoints = new HashMap<>();
    private final Map<Integer, Integer> teamVictoryPoints = new HashMap<>();
    private final List<VictoryPointAward> awardLog = new ArrayList<>();
    private boolean endScoringDone = false;

    /**
     * @return {@code true} once the one-time end-of-mission scoring (Objective Raid) has been performed, so the
     *       repeated victory checks at game end do not award it twice
     */
    public boolean isEndScoringDone() {
        return endScoringDone;
    }

    public void setEndScoringDone(boolean endScoringDone) {
        this.endScoringDone = endScoringDone;
    }

    /**
     * Awards victory points to a player, adding to their running total.
     *
     * @param playerId  The ID of the receiving player
     * @param points    The number of victory points to award (may be negative for penalties)
     * @param gameRound The game round in which the points are awarded
     * @param reason    A human-readable reason for the award
     */
    public void awardToPlayer(int playerId, int points, int gameRound, String reason) {
        playerVictoryPoints.merge(playerId, points, Integer::sum);
        awardLog.add(new VictoryPointAward(gameRound, Recipient.PLAYER, playerId, points, reason));
        LOGGER.debug("[VP] Round {}: player {} awarded {} VP ({}); new total: {}",
              gameRound, playerId, points, reason, playerVictoryPoints.get(playerId));
    }

    /**
     * Awards victory points to a team, adding to its running total.
     *
     * @param teamId    The ID of the receiving team
     * @param points    The number of victory points to award (may be negative for penalties)
     * @param gameRound The game round in which the points are awarded
     * @param reason    A human-readable reason for the award
     */
    public void awardToTeam(int teamId, int points, int gameRound, String reason) {
        teamVictoryPoints.merge(teamId, points, Integer::sum);
        awardLog.add(new VictoryPointAward(gameRound, Recipient.TEAM, teamId, points, reason));
        LOGGER.debug("[VP] Round {}: team {} awarded {} VP ({}); new total: {}",
              gameRound, teamId, points, reason, teamVictoryPoints.get(teamId));
    }

    /**
     * @param playerId The ID of a player
     *
     * @return The player's current victory point total; 0 if the player has never been awarded points
     */
    public int getPlayerVictoryPoints(int playerId) {
        return playerVictoryPoints.getOrDefault(playerId, 0);
    }

    /**
     * @param teamId The ID of a team
     *
     * @return The team's current victory point total; 0 if the team has never been awarded points
     */
    public int getTeamVictoryPoints(int teamId) {
        return teamVictoryPoints.getOrDefault(teamId, 0);
    }

    /** @return The IDs of all players that have received at least one victory point award */
    public Set<Integer> getScoringPlayers() {
        return playerVictoryPoints.keySet();
    }

    /** @return The IDs of all teams that have received at least one victory point award */
    public Set<Integer> getScoringTeams() {
        return teamVictoryPoints.keySet();
    }

    /** @return {@code true} if any victory points have been awarded to any player or team in this game */
    public boolean hasAnyScore() {
        return !playerVictoryPoints.isEmpty() || !teamVictoryPoints.isEmpty();
    }

    /** @return An unmodifiable snapshot of all victory point awards made in this game, in award order */
    public List<VictoryPointAward> getAwardLog() {
        return List.copyOf(awardLog);
    }

    /**
     * Retrieves the victory point tracker of the given game, creating and storing it in the game's victory context if
     * it does not exist yet. If the victory context itself has not been initialized (it is set up at game start), it
     * is initialized here so points awarded early are not lost.
     *
     * @param game The game
     *
     * @return The game's victory point tracker, never {@code null}
     */
    public static VictoryPointTracker getTracker(Game game) {
        Map<String, Object> victoryContext = game.getVictoryContext();
        if (victoryContext == null) {
            LOGGER.debug("[VP] Victory context not yet initialized; creating it to hold the victory point tracker");
            game.setVictoryContext(new HashMap<>());
            victoryContext = game.getVictoryContext();
        }
        if (victoryContext.get(VICTORY_CONTEXT_KEY) instanceof VictoryPointTracker tracker) {
            return tracker;
        }
        VictoryPointTracker tracker = new VictoryPointTracker();
        victoryContext.put(VICTORY_CONTEXT_KEY, tracker);
        return tracker;
    }

    /**
     * Looks up the victory point tracker in the given victory context without creating one. Use this for read-only
     * access from victory condition checks; use {@link #getTracker(Game)} when awarding points.
     *
     * @param victoryContext The victory context to search, or {@code null} when no context exists
     *
     * @return The tracker stored in the context, or {@code null} if the context is {@code null} or holds no tracker
     */
    public static @Nullable VictoryPointTracker findTracker(@Nullable Map<String, Object> victoryContext) {
        if ((victoryContext != null)
              && (victoryContext.get(VICTORY_CONTEXT_KEY) instanceof VictoryPointTracker tracker)) {
            return tracker;
        }
        return null;
    }
}
