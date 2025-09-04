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
import java.util.Map;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.game.Game;

/**
 * This is a VictoryCondition that will match when friendly BV over enemy BV is at least a given ratio in percent. For
 * example, for a ratio of 150%, friendly BV must be 1.5 times enemy BV. In games with more than two teams, all enemy BV
 * is considered together, so a starting point of 1:1:1 BV for three teams would place the initial ratio at 1 / (1 + 1)
 * = 0.5.
 */
public class BVRatioVictoryCondition implements BVVictoryCondition {

    protected int ratio;

    public BVRatioVictoryCondition(int ratio) {
        this.ratio = ratio;
    }

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> ctx) {
        boolean isVictory = false;
        VictoryResult victoryResult = new VictoryResult(true);
        HashSet<Integer> doneTeams = new HashSet<>();

        for (Player player : game.getPlayersList()) {
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
            int friendlyBV = getFriendlyBV(game, player);
            int enemyBV = getEnemyBV(game, player);

            if ((enemyBV == 0) || ((100 * friendlyBV) / enemyBV >= ratio)) {
                Report r = new Report(7100, Report.PUBLIC);
                isVictory = true;
                if (team == Player.TEAM_NONE) {
                    r.add(player.getName());
                    victoryResult.setPlayerScore(player.getId(), 1);
                } else {
                    r.add("Team " + team);
                    victoryResult.setTeamScore(team, 1);
                }
                r.add(enemyBV == 0 ? 9999 : (100 * friendlyBV) / enemyBV);
                victoryResult.addReport(r);
            }
        }
        return isVictory ? victoryResult : VictoryResult.noResult();
    }
}
