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

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.Report;

import java.io.Serializable;
import java.util.*;

/**
 * Implements a kill count victory condition.  Victory is achieved if a team (or
 * a player with no team) achieves more kills than the set amount.  If multiple
 * teams/players achieve the kill condition in a turn, victory is awarded to the
 * player/team with the highest kill count.
 */
public class KillCountVictory implements VictoryCondition, Serializable {

    protected int requiredKillCount;

    public KillCountVictory(int requiredKillCount) {
        this.requiredKillCount = requiredKillCount;
    }

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
        boolean isVictory = false;
        VictoryResult victoryResult = new VictoryResult(true);
        Map<Integer, Integer> killsPerTeam = new HashMap<>();
        Map<Integer, Integer> killsPerNoTeamPlayer = new HashMap<>();
        
        updateKillTables(game, killsPerTeam, killsPerNoTeamPlayer, game.getWreckedEntities());
        updateKillTables(game, killsPerTeam, killsPerNoTeamPlayer, game.getCarcassEntities());
        
        boolean teamHasHighestKills = true;
        int highestKillsId = -1;
        int killCount = 0;
        for (Integer killer : killsPerTeam.keySet()) {
            if (killsPerTeam.get(killer) > killCount) {
                highestKillsId = killer;
                killCount = killsPerTeam.get(killer);
            }
        }
        
        for (Integer killer : killsPerNoTeamPlayer.keySet()) {
            if (killsPerTeam.get(killer) > killCount) {
                highestKillsId = killer;
                killCount = killsPerNoTeamPlayer.get(killer);
                teamHasHighestKills = false;
            }
        }
        
        if (killCount >= requiredKillCount) {
            Report r = new Report(7106, Report.PUBLIC);
            isVictory = true;
            if (teamHasHighestKills) {
                r.add("Team " + highestKillsId);
                victoryResult.setTeamScore(highestKillsId, 1);
            } else {
                Player winner = game.getPlayer(highestKillsId);
                r.add(winner.getName());
                victoryResult.setPlayerScore(winner.getId(), 1);
            }
            r.add(killCount);
            victoryResult.addReport(r);
        }

        return isVictory ? victoryResult : VictoryResult.noResult();
    }

    private void updateKillTables(Game game,
                                  Map<Integer, Integer> teamKills,
                                  Map<Integer, Integer> playerKills,
                                  Enumeration<Entity> victims) {
        while (victims.hasMoreElements()) {
            Entity wreck = victims.nextElement();
            Entity killer = game.getEntityFromAllSources(wreck.getKillerId());
            
            if (killer == null) {
                continue;
            }            
            
            int team = killer.getOwner().getTeam();
            // Friendly fire doesn't count
            if (team == wreck.getOwner().getTeam()) {
                continue;
            }
            if (team != Player.TEAM_NONE) {
                teamKills.merge(team, 1, Integer::sum);
            } else {
                int player = killer.getOwner().getId();
                // Friendly fire doesn't count
                if (wreck.getOwner().getId() != player) {
                    playerKills.merge(team, 1, Integer::sum);
                }
            }
        }
    }
}
