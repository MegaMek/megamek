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
import java.util.HashMap;
import java.util.Vector;

import megamek.common.IGame;
import megamek.common.Player;

/**
 * implementation of player-agreed victory
 */
public class ForceVictory implements Victory, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1782762191476942976L;

    public ForceVictory() {
    }

    public Victory.Result victory(IGame game, HashMap<String, Object> ctx) {
        if (!game.isForceVictory())
            return new SimpleNoResult();
        int victoryPlayerId = game.getVictoryPlayerId();
        int victoryTeam = game.getVictoryTeam();
        Vector<Player> players = game.getPlayersVector();
        boolean forceVictory = true;

        // Individual victory.
        if (victoryPlayerId != Player.PLAYER_NONE) {
            for (int i = 0; i < players.size(); i++) {
                Player player = players.elementAt(i);

                if (player.getId() != victoryPlayerId && !player.isObserver()) {
                    if (!player.admitsDefeat()) {
                        forceVictory = false;
                        break;
                    }
                }
            }
        }
        // Team victory.
        if (victoryTeam != Player.TEAM_NONE) {
            for (int i = 0; i < players.size(); i++) {
                Player player = players.elementAt(i);

                if (player.getTeam() != victoryTeam && !player.isObserver()) {
                    if (!player.admitsDefeat()) {
                        forceVictory = false;
                        break;
                    }
                }
            }
        }

        if (forceVictory) {
            return new SimpleResult(true, victoryPlayerId, victoryTeam);
        }
        /*
         * modifying state is naaastyy for (int i = 0; i < players.size(); i++) {
         * Player player = players.elementAt(i); player.setAdmitsDefeat(false); }
         */

        // cancelVictory(); //modifying state is nasty
        return new SimpleNoResult();
    }
}