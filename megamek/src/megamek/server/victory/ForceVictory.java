/*
 * MegaMek - Copyright (C) 2007-2008 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.server.victory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import megamek.common.Game;
import megamek.common.IPlayer;

/**
 * implementation of player-agreed victory
 */
public class ForceVictory implements IVictoryConditions, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1782762191476942976L;

    public ForceVictory() {
    }

    public VictoryResult victory(Game game, Map<String, Object> ctx) {
        if (!game.isForceVictory()) {
            return VictoryResult.noResult();
        }
        int victoryPlayerId = game.getVictoryPlayerId();
        int victoryTeam = game.getVictoryTeam();
        List<IPlayer> players = game.getPlayersVector();
        boolean forceVictory = true;

        // Individual victory.
        if (victoryPlayerId != IPlayer.PLAYER_NONE) {
            for (int i = 0; i < players.size(); i++) {
                IPlayer player = players.get(i);

                if (player.getId() != victoryPlayerId && !player.isObserver()) {
                    if (!player.admitsDefeat()) {
                        forceVictory = false;
                        break;
                    }
                }
            }
        }
        // Team victory.
        if (victoryTeam != IPlayer.TEAM_NONE) {
            for (int i = 0; i < players.size(); i++) {
                IPlayer player = players.get(i);

                if (player.getTeam() != victoryTeam && !player.isObserver()) {
                    if (!player.admitsDefeat()) {
                        forceVictory = false;
                        break;
                    }
                }
            }
        }

        if (forceVictory) {
            return new VictoryResult(true, victoryPlayerId, victoryTeam);
        }

        return VictoryResult.noResult();
    }
}