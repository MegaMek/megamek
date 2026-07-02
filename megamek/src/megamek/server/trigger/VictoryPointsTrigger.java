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

package megamek.server.trigger;

import java.util.Objects;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.server.victory.VictoryPointTracker;

/**
 * This Trigger reacts while a side's Victory Point total (Standard Missions, Objectives) has reached the given
 * minimum. When the player name is blank, any side reaching the total triggers; otherwise only the given player's
 * side (their team, or the player itself when unteamed). This bridges the running VP score into the boolean victory
 * condition tree, enabling conditions like "first side to 5 VP wins". Note that this trigger reacts every time it is
 * checked while the total holds; use {@link OnceTrigger} to limit it.
 *
 * @param playerName    The player whose side must have the points, or blank/{@code null} for any side
 * @param minimumPoints The Victory Point total that triggers
 */
public record VictoryPointsTrigger(String playerName, int minimumPoints) implements Trigger {

    public VictoryPointsTrigger(@Nullable String playerName, int minimumPoints) {
        this.playerName = Objects.requireNonNullElse(playerName, "");
        this.minimumPoints = minimumPoints;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        if (!(game instanceof Game twGame)) {
            return false;
        }
        VictoryPointTracker tracker = VictoryPointTracker.findTracker(twGame.getVictoryContext());
        if (tracker == null) {
            return false;
        }
        if (playerName.isBlank()) {
            return anySideHasMinimumPoints(tracker);
        }
        Player player = ObjectiveTriggerHelper.findPlayerByName(game, playerName);
        if (player == null) {
            return false;
        }
        int sidePoints = (player.getTeam() != Player.TEAM_NONE)
              ? tracker.getTeamVictoryPoints(player.getTeam())
              : tracker.getPlayerVictoryPoints(player.getId());
        return sidePoints >= minimumPoints;
    }

    private boolean anySideHasMinimumPoints(VictoryPointTracker tracker) {
        for (int teamId : tracker.getScoringTeams()) {
            if (tracker.getTeamVictoryPoints(teamId) >= minimumPoints) {
                return true;
            }
        }
        for (int playerId : tracker.getScoringPlayers()) {
            if (tracker.getPlayerVictoryPoints(playerId) >= minimumPoints) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "VictoryPoints: " + minimumPoints + "+" + (playerName.isBlank() ? "" : " for " + playerName);
    }
}
