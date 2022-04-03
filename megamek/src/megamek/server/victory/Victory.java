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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import megamek.common.Game;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.server.LeaderBoard.LeaderBoard;
import megamek.server.LeaderBoard.LeaderBoardEntry;

public class Victory implements Serializable {
    private static final long serialVersionUID = -8633873540471130320L;

    private boolean checkForVictory;
    private int neededVictoryConditions;

    private IVictoryConditions force = new ForceVictory();
    private IVictoryConditions lastMan = new LastManStandingVictory();
    private IVictoryConditions[] VCs = null;

    public Victory(GameOptions options) {
        checkForVictory = options.booleanOption(OptionsConstants.VICTORY_CHECK_VICTORY);

        if (checkForVictory) {
            VCs = buildVClist(options);
        }
    }

    private IVictoryConditions[] buildVClist(GameOptions options) {
        neededVictoryConditions = options.intOption(OptionsConstants.VICTORY_ACHIEVE_CONDITIONS);
        List<IVictoryConditions> victories = new ArrayList<>();
        // BV related victory conditions
        if (options.booleanOption(OptionsConstants.VICTORY_USE_BV_DESTROYED)) {
            victories.add(new BVDestroyedVictory(options.intOption(OptionsConstants.VICTORY_BV_DESTROYED_PERCENT)));
        }
        if (options.booleanOption(OptionsConstants.VICTORY_USE_BV_RATIO)) {
            victories.add(new BVRatioVictory(options.intOption(OptionsConstants.VICTORY_BV_RATIO_PERCENT)));
        }

        // Kill count victory condition
        if (options.booleanOption(OptionsConstants.VICTORY_USE_KILL_COUNT)) {
            victories.add(new KillCountVictory(options.intOption(OptionsConstants.VICTORY_GAME_KILL_COUNT)));
        }

        // Commander killed victory condition
        if (options.booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) {
            victories.add(new EnemyCmdrDestroyedVictory());
        }
        return victories.toArray(new IVictoryConditions[0]);
    }

    public VictoryResult checkForVictory(Game game, Map<String, Object> context) {
        VictoryResult reVal;

        // Check for ForceVictory
        // Always check for forced victory, so games without victory conditions
        // can be completed
        reVal = force.victory(game, context);
        if (reVal.victory()) {
            return reVal;
        }

        // Check optional Victory conditions
        // These can have reports
        if (checkForVictory) {
            if (VCs == null) {
                VCs = buildVClist(game.getOptions());
            }
            reVal = checkOptionalVictory(game, context);
            if (reVal.victory()) {
                return reVal;
            }
        }

        // Check for LastManStandingVictory
        VictoryResult lastManResult = lastMan.victory(game, context);
        if (checkForVictory && !reVal.victory() && lastManResult.victory()) {
            return lastManResult;
        }
        return reVal;
    }

    private VictoryResult checkOptionalVictory(Game game, Map<String, Object> context) {
        boolean victory = false;
        VictoryResult vr = new VictoryResult(true);

        // combine scores
        for (IVictoryConditions v : VCs) {
            VictoryResult res = v.victory(game, context);
            for (Report r : res.getReports()) {
                vr.addReport(r);
            }
            if (res.victory()) {
                victory = true;
            }
            for (int pl : res.getPlayers()) {
                vr.addPlayerScore(pl, vr.getPlayerScore(pl) + res.getPlayerScore(pl));
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
            vr.addPlayerScore(pl, sc / VCs.length);
            if (sc > highScore) {
                highScore = sc;
            }
        }
        for (int pl : vr.getTeams()) {
            double sc = vr.getTeamScore(pl);
            vr.addTeamScore(pl, sc / VCs.length);
            if (sc > highScore) {
                highScore = sc;
            }
        }
        if (highScore < neededVictoryConditions) {
            victory = false;
        }
        vr.setVictory(victory);

        if (vr.victory()) {
            return vr;
        }

        if (!vr.victory() && game.gameTimerIsExpired()) {
            return VictoryResult.drawResult();
        }

        //
        Enumeration<Player> players = game.getPlayers();
        LeaderBoard lb = game.leaderBoard.get();

        LeaderBoardEntry entry = lb.get(players.nextElement());
        if (entry.getElo() == 0){

        }else{

        }

        return vr;
    }
}