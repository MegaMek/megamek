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

import megamek.common.Player;

/**
 * quick implementation of a Victory.Result
 */
public class SimpleResult extends VictoryResult implements Victory.Result {
    public SimpleResult(boolean win, int player, int team) {
        super(win);
        if (player != Player.PLAYER_NONE)
            addPlayerScore(player, 1.0);
        if (team != Player.TEAM_NONE)
            addTeamScore(team, 1.0);
    }
}