/*
 * MegaMek - Copyright (C) 2007-2008 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.victory;

import megamek.common.Game;
import megamek.common.Player;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * implementation of player-agreed victory
 */
public class ForceVictory implements IVictoryConditions, Serializable {
    private static final long serialVersionUID = 1782762191476942976L;

    public ForceVictory() {

    }

    @Override
    public VictoryResult victory(Game game, Map<String, Object> ctx) {
        if (!game.isForceVictory()) {
            return VictoryResult.noResult();
        }
        int victoryPlayerId = game.getVictoryPlayerId();
        int victoryTeam = game.getVictoryTeam();
        List<Player> players = game.getPlayersVector();
        boolean forceVictory = true;

        // Individual victory.
        if (victoryPlayerId != Player.PLAYER_NONE) {
            forceVictory = players.stream().filter(player -> player.getId() != victoryPlayerId && !player.isObserver()).allMatch(Player::admitsDefeat);
        }
        // Team victory.
        if (victoryTeam != Player.TEAM_NONE) {
            if (players.stream().filter(player -> player.getTeam() != victoryTeam && !player.isObserver()).anyMatch(player -> !player.admitsDefeat())) {
                forceVictory = false;
            }
        }

        if (forceVictory) {
            return new VictoryResult(true, victoryPlayerId, victoryTeam);
        }

        return VictoryResult.noResult();
    }
}
