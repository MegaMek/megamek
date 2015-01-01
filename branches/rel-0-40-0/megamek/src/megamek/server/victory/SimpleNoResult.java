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

import megamek.common.IPlayer;

/**
 * quick implementation of a Victory.Result to say "nobody won yet"
 */
public class SimpleNoResult extends SimpleResult {
    public SimpleNoResult() {
        super(false, IPlayer.PLAYER_NONE, IPlayer.TEAM_NONE);
    }
}