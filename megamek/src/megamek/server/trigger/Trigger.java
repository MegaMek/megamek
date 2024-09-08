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
 * This interface is implemented by Triggers that enable events like messages, game end, defeat or victory.
 */
public interface Trigger {

    /**
     * Returns true when this Trigger is triggered, i.e. if the prerequisites for its associated
     * event are deemed to be satisfied. Note that the event is itself responsible for making
     * sure that it only returns true once, if it should only trigger once. If it returns true
     * on multiple occasions, the triggered event may happen multiple times.
     * The given TriggerEvent specifies when at what moment of the game this method is called.
     *
     * @param game The game
     * @param event The type of event that caused the trigger to be called
     * @return True when this Trigger considers itself triggered, false otherwise
     */
    boolean isTriggered(IGame game, TriggerSituation event);
}
