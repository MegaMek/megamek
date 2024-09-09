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

import megamek.common.Game;
import megamek.common.Player;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This class represents the player-agreed /victory through chat commands
 */
public class PlayerAgreedVictory implements VictoryCondition, Serializable {

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
        if (!game.isForceVictory()) {
            return VictoryResult.noResult();
        }
        final int victoryPlayerId = game.getVictoryPlayerId();
        final int victoryTeam = game.getVictoryTeam();
        List<Player> activePlayers = game.getPlayersList();
        activePlayers.removeIf(Player::isObserver);
        boolean forceVictory = true;

        // Individual victory
        if (victoryPlayerId != Player.PLAYER_NONE) {
            if (activePlayers.stream().filter(p -> p.getId() != victoryPlayerId).anyMatch(Player::doesNotAdmitDefeat)) {
                forceVictory = false;
            }
        }
        // Team victory
        if (victoryTeam != Player.TEAM_NONE) {
            if (activePlayers.stream().filter(p -> p.getTeam() != victoryTeam).anyMatch(Player::doesNotAdmitDefeat)) {
                forceVictory = false;
            }
        }

        if (forceVictory) {
            return new VictoryResult(true, victoryPlayerId, victoryTeam);
        } else {
            return VictoryResult.noResult();
        }
    }
}
