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
import java.util.Map;

import megamek.common.Game;
import megamek.common.IPlayer;

/**
 * implementation of "last player/team standing"
 */
public class LastManStandingVictory implements IVictoryConditions, Serializable {

    private static final long serialVersionUID = 3372431109525075853L;

    public LastManStandingVictory() {
    }

    public VictoryResult victory(Game game, Map<String, Object> ctx) {
        // check all players/teams for aliveness
        int playersAlive = 0;
        IPlayer lastPlayer = null;
        boolean oneTeamAlive = false;
        int lastTeam = IPlayer.TEAM_NONE;
        boolean unteamedAlive = false;
        for (IPlayer player : game.getPlayersVector()) {
            int team = player.getTeam();
            if (game.getLiveDeployedEntitiesOwnedBy(player) <= 0) {
                continue;
            }
            // we found a live one!
            playersAlive++;
            lastPlayer = player;
            // check team
            if (team == IPlayer.TEAM_NONE) {
                unteamedAlive = true;
            } else if (lastTeam == IPlayer.TEAM_NONE) {
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
            if (lastPlayer != null && lastPlayer.getTeam() == IPlayer.TEAM_NONE) {
                // individual victory
                return new VictoryResult(true, lastPlayer.getId(), IPlayer.TEAM_NONE);
            }
        }

        // did we only find one live team?
        if (oneTeamAlive && !unteamedAlive) {
            // team victory
            return new VictoryResult(true, IPlayer.PLAYER_NONE, lastTeam);
        }
        return VictoryResult.noResult();
    }
}