/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server.scriptedEvents;

import java.util.Map;

import jakarta.annotation.Nonnull;
import megamek.common.game.Game;
import megamek.server.trigger.Trigger;
import megamek.server.trigger.TriggerSituation;
import megamek.server.victory.VictoryCondition;
import megamek.server.victory.VictoryResult;

/**
 * This class represents "victory" events that are a draw that can be added programmatically or from MM scenarios to
 * check for a draw result. There are two ways these victory events can be used, depending on the endsGame parameter:
 * When endsGame is true, this victory condition is checked at the end of game rounds and if it is met, the game ends.
 * When endsGame is false, this victory condition is only checked after the game has already ended through another
 * condition (e.g. round limit or other event).
 * <p>
 * Note: Victory Triggers must *not* be one-time triggers. Victory is checked multiple times, even when victory is
 * achieved and triggers must be able to react multiple times.
 *
 * @param trigger  The trigger that decides if a draw has occurred
 * @param endsGame When true, ends the game when it happens, when false, is only checked when the game has ended
 *
 * @see GameEndTriggeredEvent
 * @see VictoryTriggeredEvent
 */
public record DrawTriggeredEvent(Trigger trigger, boolean endsGame) implements TriggeredEvent, VictoryCondition {

    @Override
    public VictoryResult checkVictory(Game game, Map<String, Object> context) {
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
    @Nonnull
    public String toString() {
        return "Draw: " + trigger + (endsGame ? " [ends]" : "");
    }
}
