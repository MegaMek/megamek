/*
 * Copyright (c) 2007-2008 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.victory;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.Game;
import megamek.common.IGame;
import megamek.common.options.BasicGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.server.scriptedevent.TriggeredEvent;
import megamek.server.scriptedevent.VictoryTriggeredEvent;
import megamek.server.trigger.TriggerSituation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class manages the victory conditions of a game. As victory conditions could potentially have some state they need to save in
 * savegames, this class is actually part of the Game rather than just a GameManager helper. TODO: This should be resolved by storing
 * victory conditions in Game but manage them from GameManager.
 */
public class VictoryHelper implements Serializable {

    private final boolean checkForVictory;
    private int neededVictoryConditionCount;
    private final VictoryCondition playerAgreedVC = new PlayerAgreedVictory();
    private final VictoryCondition battlefieldControlVC = new BattlefieldControlVictory();
    private final List<VictoryCondition> victoryConditions = new ArrayList<>();

    /**
     * Constructs the VictoryHelper. Note that this should be called after the lobby phase so that the final victory game settings for this
     * game are used.
     *
     * @param game The game
     */
    public VictoryHelper(IGame game) {
        checkForVictory = game.getOptions().booleanOption(OptionsConstants.VICTORY_CHECK_VICTORY);

        if (checkForVictory) {
            buildVClist(game);
        }
    }

    /**
     * Checks the various victory conditions if any lead to a game-ending result. Player-agreed /victory is always checked, other victory
     * conditions only if victory checking is at all enabled. Scripted victory and game-ending events are also always tested.
     *
     * @param game    The Game
     * @param context The victory context - to my knowledge, this is currently not used at all
     * @return A combined victory result giving the current victory status
     * @see VictoryResult#noResult()
     * @see VictoryResult#drawResult()
     */
    public VictoryResult checkForVictory(IGame game, Map<String, Object> context) {
        // Always check for forced victory, so games without victory conditions can be completed
        VictoryResult playerAgreedVR = playerAgreedVC.checkVictory(game, context);
        if (playerAgreedVR.isVictory()) {
            return playerAgreedVR;
        }

        if (gameEndsByScriptedEvent(game)) {
            // The game does end now; therefore, test all victory events. If none are met, the game is a draw
            for (TriggeredEvent event : game.scriptedEvents()) {
                if (event instanceof VictoryTriggeredEvent victoryEvent) {
                    VictoryResult victoryResult = victoryEvent.checkVictory(game, context);
                    if (victoryResult.isVictory()) {
                        return victoryResult;
                    }
                }
            }
            return VictoryResult.drawResult();
        }

        if (checkForVictory) {
            VictoryResult result = checkOptionalVictoryConditions(game, context);
            if (result.isVictory()) {
                return result;
            }

            // Check for battlefield control; this is currently an automatic victory when VCs are checked at all
            // this could be made optional to allow the game to continue once alone if there's a use case
            VictoryResult battlefieldControlVR = battlefieldControlVC.checkVictory(game, context);
            if (battlefieldControlVR.isVictory()) {
                return battlefieldControlVR;
            }
        }

        return VictoryResult.noResult();
    }

    /**
     * @return True when the game ends right now (at the end of round victory check) through a scripted event, either a game-end
     * event or a victory event that is set to be game-ending.
     */
    private boolean gameEndsByScriptedEvent(IGame game) {
        return game.scriptedEvents().stream()
            .filter(TriggeredEvent::isGameEnding)
            .anyMatch(event -> event.trigger().isTriggered(game, TriggerSituation.ROUND_END));
    }

    private VictoryResult checkOptionalVictoryConditions(IGame game, Map<String, Object> context) {
        boolean isVictory = false;
        VictoryResult combinedResult = new VictoryResult(true);

        // combine scores; the current score system is used to check the VC count that is achieved
        for (VictoryCondition victoryCondition : victoryConditions) {
            VictoryResult victoryResult = victoryCondition.checkVictory(game, context);
            combinedResult.addReports(victoryResult.getReports());
            isVictory |= victoryResult.isVictory();
            combinedResult.addScores(victoryResult);
        }

        // find highscore
        double highScore = 0.0;
        for (int playerId : combinedResult.getScoringPlayers()) {
            double score = combinedResult.getPlayerScore(playerId);
            highScore = Math.max(highScore, score);
        }
        for (int teamId : combinedResult.getScoringTeams()) {
            double score = combinedResult.getTeamScore(teamId);
            highScore = Math.max(highScore, score);
        }
        if (highScore < neededVictoryConditionCount) {
            isVictory = false;
        }
        combinedResult.setVictory(isVictory);

        if (combinedResult.isVictory()) {
            return combinedResult;
        } else if (game.gameTimerIsExpired()) {
            return VictoryResult.drawResult();
        } else {
            return combinedResult;
        }
    }

    /**
     * Returns a list of victory conditions that are checked if victory conditions are checked at all as per this game's options. The
     * conditions include those set in the game options as well as those added by code (e.g. through a scenario).
     */
    private void buildVClist(IGame game) {
        var options = game.getOptions();
        neededVictoryConditionCount = options.intOption(OptionsConstants.VICTORY_ACHIEVE_CONDITIONS);
        if (options.booleanOption(OptionsConstants.VICTORY_USE_BV_DESTROYED)) {
            victoryConditions.add(new BVDestroyedVictoryCondition(options.intOption(OptionsConstants.VICTORY_BV_DESTROYED_PERCENT)));
        }
        if (options.booleanOption(OptionsConstants.VICTORY_USE_BV_RATIO)) {
            victoryConditions.add(new BVRatioVictoryCondition(options.intOption(OptionsConstants.VICTORY_BV_RATIO_PERCENT)));
        }
        if (options.booleanOption(OptionsConstants.VICTORY_USE_KILL_COUNT)) {
            victoryConditions.add(new KillCountVictory(options.intOption(OptionsConstants.VICTORY_GAME_KILL_COUNT)));
        }
        if (options.booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) {
            victoryConditions.add(new EnemyCmdrDestroyedVictory());
        }
    }
}
