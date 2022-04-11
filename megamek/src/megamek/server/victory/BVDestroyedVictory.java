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
import megamek.common.Report;

import java.util.HashSet;
import java.util.Map;

/**
 * implementation which will match when a certain percentage of all enemy BV is
 * destroyed. NOTE: this could be improved by giving more points for killing
 * more than required amount
 */
public class BVDestroyedVictory extends AbstractBVVictory {

    private static final long serialVersionUID = -1807333576570154144L;
    protected int destroyedPercent;

    public BVDestroyedVictory(int destroyedPercent) {
        this.destroyedPercent = destroyedPercent;
    }

    @Override
    public VictoryResult victory(Game game, Map<String, Object> ctx) {
        boolean victory = false;
        VictoryResult vr = new VictoryResult(true);
        // now check for detailed victory conditions...
        HashSet<Integer> doneTeams = new HashSet<>();
        victory = checkVictoryConditions(game, false, vr, doneTeams);
        if (victory)
            return vr;
        return VictoryResult.noResult();
    }

    private boolean checkVictoryConditions(Game game, boolean victory, VictoryResult vr, HashSet<Integer> doneTeams) {
        for (Player player : game.getPlayersVector()) {
            Integer team = getIntegerTeam(doneTeams, player);
            if (team == null)
                continue; // skip if already
            int ebv = getEnemyBV(game, player);
            int eibv = getEnemyInitialBV(game, player);

            victory = addVictoryToRepport(victory, vr, player, team, ebv, eibv);
        }// end for
        return victory;
    }

    private boolean addVictoryToRepport(boolean victory, VictoryResult vr, Player player, Integer team, int ebv, int eibv) {
        if (eibv != 0 && (ebv * 100) / eibv <= 100 - destroyedPercent) {
            Report r = new Report(7105, Report.PUBLIC);
            victory = true;
            if (team == Player.TEAM_NONE) {
                r.add(player.getName());
                vr.addPlayerScore(player.getId(), 1.0);
            } else {
                r.add("Team " + team);
                vr.addTeamScore(team, 1.0);
            }
            r.add(100 - ((ebv * 100) / eibv));
            vr.addReport(r);
        }
        return victory;
    }

    private Integer getIntegerTeam(HashSet<Integer> doneTeams, Player player) {
        if (player.isObserver())
            return null;
        int ebv = 0;
        int eibv = 0;
        int team = player.getTeam();
        if (team != Player.TEAM_NONE) {
            if (doneTeams.contains(team))
                return null;
            doneTeams.add(team);
        }
        return team;
    }

    private boolean isVictory(VictoryResult vr, Player player, int team, Report r) {
        return verifyVictory(vr, player, team, r);
    }

    static boolean verifyVictory(VictoryResult vr, Player player, int team, Report r) {
        boolean victory;
        victory = true;
        if (team == Player.TEAM_NONE) {
            r.add(player.getName());
            vr.addPlayerScore(player.getId(), 1.0);
        } else {
            r.add("Team " + team);
            vr.addTeamScore(team, 1.0);
        }
        return victory;
    }
}
