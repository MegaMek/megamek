/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.acar.manager;

import megamek.common.Player;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.server.victory.VictoryResult;

public class VictoryHelper {

    private final SimulationManager manager;
    private VictoryResult result;

    public VictoryHelper(SimulationManager manager) {
        this.manager = manager;
    }

    public VictoryResult getVictoryResult() {
        if (this.result == null || !this.result.isVictory()) {
            this.result = new BattlefieldControlVictory().checkVictory(manager.getGame());
        }
        return this.result;
    }

    public static class BattlefieldControlVictory {

        public VictoryResult checkVictory(SimulationContext context) {
            // check all players/teams for aliveness
            int playersAlive = 0;
            Player lastPlayer = null;
            boolean oneTeamAlive = false;
            int lastTeam = Player.TEAM_NONE;
            boolean unteamedAlive = false;
            for (Player player : context.getPlayersList()) {
                int team = player.getTeam();
                if (context.getLiveDeployedEntitiesOwnedBy(player) <= 0) {
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

            if (context.gameTimerIsExpired()) {
                // draw
                return VictoryResult.drawResult();
            }

            return VictoryResult.noResult();
        }
    }

}
