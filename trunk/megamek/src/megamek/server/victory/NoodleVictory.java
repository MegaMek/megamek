/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
import java.util.ArrayList;
import java.util.HashMap;

import megamek.common.IGame;

/**
 * detailed spaghetti from the old victory code this assumes game options do not
 * change during game (atleast regarding victory conditions)
 */
public class NoodleVictory implements Victory, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1458500624064481866L;
    protected Victory v;

    public NoodleVictory() {
    }

    protected void construct(IGame game) {
        ArrayList<Victory> victories = new ArrayList<Victory>();
        // BV related victory conditions
        if (game.getOptions().booleanOption("use_bv_destroyed")) {
            victories.add(new BVDestroyedVictory(game.getOptions().intOption(
            "bv_destroyed_percent")));
        }
        if (game.getOptions().booleanOption("use_bv_ratio")) {
            victories.add(new BVRatioVictory(game.getOptions().intOption(
            "bv_ratio_percent")));
        }

        // Commander killed victory condition
        if (game.getOptions().booleanOption("commander_killed")) {
            victories.add(new EnemyCmdrDestroyedVictory());
        }
        // use a summing victory target to check if someone is winning
        v = new SummingThresholdVictory(game.getOptions().intOption(
        "achieve_conditions"), victories.toArray(new Victory[0]));
    }

    public Victory.Result victory(IGame game, HashMap<String, Object> ctx) {
        if (v == null)
            construct(game);
        Victory.Result res = v.victory(game, ctx);

        if (res.victory()) {
            return res;
        }

        if (!res.victory() && game.gameTimerIsExpired()) {
            return new SimpleDrawResult();
        }

        return res;
    }
}