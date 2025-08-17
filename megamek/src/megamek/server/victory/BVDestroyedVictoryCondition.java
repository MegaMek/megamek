/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import megamek.common.game.Game;
import megamek.common.Player;
import megamek.common.Report;

/**
 * This is a VictoryCondition that will match when a given percentage of all enemy BV is no longer alive.
 */
public class BVDestroyedVictoryCondition implements BvVictoryCondition {

    protected int destroyedPercent;

    public BVDestroyedVictoryCondition(int destroyedPercent) {
        this.destroyedPercent = destroyedPercent;
    }

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
        boolean isVictory = false;
        VictoryResult victoryResult = new VictoryResult(true);
        HashSet<Integer> doneTeams = new HashSet<>();
        List<Player> players = List.copyOf(game.getPlayersList());
        for (Player player : players) {
            if (player.isObserver()) {
                continue;
            }
            int team = player.getTeam();
            if (team != Player.TEAM_NONE) {
                if (doneTeams.contains(team)) {
                    continue;
                }
                doneTeams.add(team);
            }
            int enemyBV = getEnemyBV(game, player);
            int enemyInitialBV = getEnemyInitialBV(game, player);

            if ((enemyInitialBV != 0) && ((enemyBV * 100) / enemyInitialBV <= 100 - destroyedPercent)) {
                isVictory = true;
                Report report = new Report(7105, Report.PUBLIC);
                if (team == Player.TEAM_NONE) {
                    report.add(player.getName());
                    victoryResult.setPlayerScore(player.getId(), 1);
                } else {
                    report.add("Team " + team);
                    victoryResult.setTeamScore(team, 1);
                }
                report.add(100 - ((enemyBV * 100) / enemyInitialBV));
                victoryResult.addReport(report);
            }
        }
        return isVictory ? victoryResult : VictoryResult.noResult();
    }
}
