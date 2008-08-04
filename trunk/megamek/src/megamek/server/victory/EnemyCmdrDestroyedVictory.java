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
import java.util.HashSet;

import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Report;

/**
 * implements "enemy commander destroyed"
 */
public class EnemyCmdrDestroyedVictory implements Victory, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 2525190210964235691L;

    public EnemyCmdrDestroyedVictory() {
    }

    public Victory.Result victory(IGame game, HashMap<String, Object> ctx) {
        VictoryResult vr = new VictoryResult(true);
        // check all players/teams for killing enemy commanders
        // score is 1.0 when enemy commanders are dead
        boolean victory = false;
        HashSet<Integer> doneTeams = new HashSet<Integer>();
        for (Player player : game.getPlayersVector()) {
            boolean killedAll = true;
            int team = player.getTeam();
            if (team != Player.TEAM_NONE) {
                if (doneTeams.contains(team))
                    continue; 
                // skip if already dealt with this team
                doneTeams.add(team);
            }
            for (Player enemyPlayer : game.getPlayersVector()) {
                if (enemyPlayer.equals(player) ||
                        (team != Player.TEAM_NONE && team == enemyPlayer.getTeam()))
                    continue;
                if (game.getLiveCommandersOwnedBy(enemyPlayer) > 0) {
                    killedAll = false;
                }
            }
            // all enemy commanders are dead
            if (killedAll) {
                if (team == Player.TEAM_NONE) {
                    Report r = new Report(7110, Report.PUBLIC);
                    r.add(player.getName());
                    vr.addPlayerScore(player.getId(), 1);
                    vr.addReport(r);
                } else {
                    Report r = new Report(7110, Report.PUBLIC);
                    r.add("Team " + team);
                    vr.addTeamScore(team, 1);
                    vr.addReport(r);
                }
                victory = true;
            }
        }
        if (victory) {
            return vr;
        }
        return new SimpleNoResult();
    }
}