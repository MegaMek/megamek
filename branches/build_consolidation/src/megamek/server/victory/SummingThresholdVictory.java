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
import megamek.common.Report;

/**
 * a summing victory condition will first sum all given victory conditions, if
 * any of them are in victory state AND someone is over the threshold, then will
 * return victory , otherwise just the scores the scores will be an average of
 * the given victory scores.
 */
public class SummingThresholdVictory implements Victory, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 7211998189207932320L;
    protected Victory[] vs;
    protected int thr;

    public SummingThresholdVictory(int threshold, Victory[] victories) {
        this.vs = victories;
        this.thr = threshold;
    }

    public Victory.Result victory(IGame game, HashMap<String, Object> ctx) {
        boolean victory = false;
        VictoryResult vr = new VictoryResult(true);

        // combine scores
        for (Victory v : vs) {
            Victory.Result res = v.victory(game, ctx);
            for (Report r : res.getReports()) {
                vr.addReport(r);
            }
            if (res.victory())
                victory = true;
            for (int pl : res.getPlayers()) {
                vr.addPlayerScore(pl, vr.getPlayerScore(pl)
                        + res.getPlayerScore(pl));
            }
            for (int t : res.getTeams()) {
                vr.addTeamScore(t, vr.getTeamScore(t) + res.getTeamScore(t));
            }
        }
        // find highscore for thresholding, also divide the score
        // to an average
        double highScore = 0.0;
        for (int pl : vr.getPlayers()) {
            double sc = vr.getPlayerScore(pl);
            vr.addPlayerScore(pl, sc / vs.length);
            if (sc > highScore)
                highScore = sc;
        }
        for (int pl : vr.getTeams()) {
            double sc = vr.getTeamScore(pl);
            vr.addTeamScore(pl, sc / vs.length);
            if (sc > highScore)
                highScore = sc;
        }
        if (highScore < thr)
            victory = false;

        vr.setVictory(victory);

        return vr;
    }
}