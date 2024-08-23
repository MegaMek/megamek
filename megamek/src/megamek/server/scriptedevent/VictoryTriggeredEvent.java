/*
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
package megamek.server.scriptedevent;

import megamek.common.Game;
import megamek.common.Player;
import megamek.logging.MMLogger;
import megamek.server.trigger.Trigger;
import megamek.server.trigger.TriggerSituation;
import megamek.server.victory.VictoryCondition;
import megamek.server.victory.VictoryResult;

import java.util.Map;

/**
 * This class represents victory events that can be added programmatically or from MM scenarios to check
 * for victory of a team. There are two ways these victory events can be used, depending on the
 * isGameEnding parameter: When isGameEnding is true, this victory condition is checked at the end of
 * game rounds and if it is met, the game ends. When isGameEnding is false, this victory condition is
 * only checked after the game has already ended through another condition (round limit or other event).
 *
 * Note: Victory Triggers must *not* be one-time triggers. Victory is checked multiple times, even when
 * victory is achieved and triggers must be able to react multiple times.
 *
 * @param trigger The trigger that decides if victory has occurred
 * @param isGameEnding When true, ends the game when it happens, when false, is only checked when the game has ended
 * @see GameEndTriggeredEvent
 */
public record VictoryTriggeredEvent(Trigger trigger, boolean isGameEnding, String playerName)
        implements TriggeredEvent, VictoryCondition {

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> context) {
        if (isGameEnding && trigger.isTriggered(game, TriggerSituation.ROUND_END)) {
            VictoryResult victoryResult = new VictoryResult(true);
            int winningTeam = game.playerForPlayername(playerName).map(Player::getTeam).orElse(Player.TEAM_NONE);
            if (winningTeam == Player.TEAM_NONE) {
                int winningPlayer = game.idForPlayername(playerName).orElse(Player.PLAYER_NONE);
                if (winningPlayer == Player.PLAYER_NONE) {
                    MMLogger.create().error("Could not find winning player or team");
                    return VictoryResult.noResult();
                }
                victoryResult.setPlayerScore(winningPlayer, 1);
            } else {
                victoryResult.setTeamScore(winningTeam, 1);
            }
            return victoryResult;
        }
        return VictoryResult.noResult();
    }
}
