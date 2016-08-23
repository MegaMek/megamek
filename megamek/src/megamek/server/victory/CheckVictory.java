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
import megamek.common.options.OptionsConstants;

/**
 * implementation of a filter which will wait until the
 * game.gameTimerIsExpired() is true or option OptionsConstants.VICTORY_CHECK_VICTORY is set before
 * returning whatever the given victory returns. otherwise returns
 * SimpleNoResult
 */
public class CheckVictory implements Victory, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -9146210812483189565L;
    protected Victory v;

    public CheckVictory(Victory v) {
        this.v = v;
        assert (v != null);
    }

    public Victory.Result victory(IGame game, HashMap<String, Object> ctx) {
        // lets call this now to make sure it gets to update its state
        Victory.Result ret = v.victory(game, ctx);

        if (!game.gameTimerIsExpired()
                && !game.getOptions().booleanOption(OptionsConstants.VICTORY_CHECK_VICTORY)) {
            return new SimpleNoResult();
        }
        return ret;
    }
}