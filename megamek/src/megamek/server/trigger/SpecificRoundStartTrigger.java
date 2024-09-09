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
 * This Trigger reacts at the start of the specified game round.
 * Note that this Trigger can react multiple times!
 */
public class SpecificRoundStartTrigger implements Trigger {

    private final int gameRound;

    public SpecificRoundStartTrigger(int round) {
        gameRound = round;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        return game.getCurrentRound() == gameRound;
    }
}
