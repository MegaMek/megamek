/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.victory;

import java.io.Serializable;
import java.util.Map;

import megamek.common.Game;
import megamek.common.IGame;
import megamek.common.Player;

/**
 * This class represents a battlefield control (units of only one team left alive) victory
 * Note that this currently does not exclude gun emplacements or spawns (MekWarriors, Missiles) from
 * the test.
 */
public class BattlefieldControlVictory implements VictoryCondition, Serializable {

    @Override
    public VictoryResult checkVictory(IGame game, Map<String, Object> ctx) {
        // check all players/teams for aliveness
        int playersAlive = 0;
        Player lastPlayer = null;
        boolean oneTeamAlive = false;
        int lastTeam = Player.TEAM_NONE;
        boolean unteamedAlive = false;
        for (Player player : game.getPlayersList()) {
            int team = player.getTeam();
            if (game.getLiveDeployedEntitiesOwnedBy(player) <= 0) {
                continue;
            }
            // we found a live one!
            playersAlive++;
            lastPlayer = player;
            // check team
            if (team == Player.TEAM_NONE) {
                unteamedAlive = true;
            } else if (lastTeam == Player.TEAM_NONE) {
                // possibly only one team alive
                oneTeamAlive = true;
                lastTeam = team;
            } else if (team != lastTeam) {
                // more than one team alive
                oneTeamAlive = false;
                lastTeam = team;
            }
        }

        // check if there's one player alive
        if (playersAlive < 1) {
            return VictoryResult.drawResult();
        } else if (playersAlive == 1) {
            if (lastPlayer.getTeam() == Player.TEAM_NONE) {
                // individual victory
                return new VictoryResult(true, lastPlayer.getId(), Player.TEAM_NONE);
            }
        }

        // did we only find one live team?
        if (oneTeamAlive && !unteamedAlive) {
            // team victory
            return new VictoryResult(true, Player.PLAYER_NONE, lastTeam);
        }

        return VictoryResult.noResult();
    }
}
