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
 * This Trigger is satisfied at any point during its given game round. It is intended to be used as a component
 * in a combined Trigger using {@link AndTrigger} or {@link OrTrigger}.
 */
public class RoundTrigger implements Trigger {

    private final int gameRound;

    public RoundTrigger(int round) {
        gameRound = round;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        return game.getCurrentRound() == gameRound;
    }
}
