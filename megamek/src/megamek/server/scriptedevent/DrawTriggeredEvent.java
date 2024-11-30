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
import megamek.common.IGame;
import megamek.server.trigger.Trigger;
import megamek.server.trigger.TriggerSituation;
import megamek.server.victory.VictoryCondition;
import megamek.server.victory.VictoryResult;

import java.util.Map;

/**
 * This class represents "victory" events that are a draw that can be added programmatically or from MM scenarios to check for a draw
 * result. There are two ways these victory events can be used, depending on the endsGame parameter: When endsGame is true, this
 * victory condition is checked at the end of game rounds and if it is met, the game ends. When endsGame is false, this victory
 * condition is only checked after the game has already ended through another condition (e.g. round limit or other event).
 *
 * Note: Victory Triggers must *not* be one-time triggers. Victory is checked multiple times, even when victory is achieved and triggers
 * must be able to react multiple times.
 *
 * @param trigger  The trigger that decides if a draw has occurred
 * @param endsGame When true, ends the game when it happens, when false, is only checked when the game has ended
 * @see GameEndTriggeredEvent
 * @see VictoryTriggeredEvent
 */
public record DrawTriggeredEvent(Trigger trigger, boolean endsGame) implements TriggeredEvent, VictoryCondition {

    @Override
    public VictoryResult checkVictory(IGame game, Map<String, Object> context) {
        if (trigger.isTriggered(game, TriggerSituation.ROUND_END)) {
            return VictoryResult.drawResult();
        } else {
            return VictoryResult.noResult();
        }
    }

    @Override
    public Trigger trigger() {
        return trigger;
    }

    @Override
    public boolean isGameEnding() {
        return endsGame;
    }

    @Override
    public String toString() {
        return "Draw: " + trigger + (endsGame ? " [ends]" : "");
    }
}
