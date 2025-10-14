/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.units.Entity;

/**
 * Implements a kill count victory condition.  Victory is achieved if a team (or a player with no team) achieves more
 * kills than the set amount.  If multiple teams/players achieve the kill condition in a turn, victory is awarded to the
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

    private void updateKillTables(IGame game,
          Map<Integer, Integer> teamKills,
          Map<Integer, Integer> playerKills,
          Enumeration<Entity> victims) {
        while (victims.hasMoreElements()) {
            Entity wreck = victims.nextElement();
            Entity killer = (Entity) game.getEntityFromAllSources(wreck.getKillerId());

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
