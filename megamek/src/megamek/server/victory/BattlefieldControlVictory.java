/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.Map;

import megamek.common.game.Game;
import megamek.common.Player;

/**
 * This class represents a battlefield control (units of only one team left alive) victory Note that this currently does
 * not exclude gun emplacements or spawns (MekWarriors, Missiles) from the test.
 */
public class BattlefieldControlVictory implements VictoryCondition, Serializable {

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
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
