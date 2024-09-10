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
package megamek.server.trigger;

import megamek.common.IGame;

/**
 * This Trigger reacts at the end of the specified game round. This is more or less the same as a RoundStartTrigger
 * of the following round, but this can be used to trigger something without actually entering the following
 * round just yet (e.g. ending the game at the end of round 10 instead of at the beginning of round 11).
 * Note that this Trigger can react multiple times!
 */
public final class SpecificRoundEndTrigger implements Trigger {

    private final int gameRound;

    public SpecificRoundEndTrigger(int round) {
        gameRound = round;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        return (event == TriggerSituation.ROUND_END) && (game.getCurrentRound() == gameRound);
    }
}
