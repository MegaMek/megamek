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

import megamek.common.IGame;

/**
 * This is the original implementation of victory moved under the new
 * infrastructure
 */
public class SpaghettiVictory implements Victory, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -7901479121287053964L;
    protected Victory force = new ForceVictory();
    protected Victory lastMan = new CheckVictory(new LastManStandingVictory());
    protected Victory check = new CheckVictory(new NoodleVictory());

    public Victory.Result victory(IGame game, HashMap<String, Object> ctx) {
        // here we make the assumption that none of the later
        // victories need to update ctx all the time.. which is nasty
        Victory.Result ret = null;
        ret = force.victory(game, ctx);
        if (ret.victory())
            return ret;

        ret = lastMan.victory(game, ctx);
        if (ret.victory())
            return ret;

        return check.victory(game, ctx);
    }
}