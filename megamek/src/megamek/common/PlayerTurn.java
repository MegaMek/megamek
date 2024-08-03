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
package megamek.common;

import java.io.Serializable;

/**
 * This interface is implemented by player turns (e.g. GameTurn), where a specific player has to declare
 * their action(s). GameTurn is specific to Total Warfare/Entity turns.
 */
public interface PlayerTurn extends Serializable  {

    /**
     * @return this turn's player ID
     */
    int playerId();

    /**
     * @return true if the given player ID is this turn's player
     */
    default boolean isValid(int playerId, IGame game) {
        return playerId == playerId();
    }

    /**
     * @return true if the given unit may act in this turn
     */
    boolean isValidEntity(InGameObject unit, IGame game);

    /**
     * @return true if the given player ID is this turn's player and the given unit may act in this turn
     */
    default boolean isValid(int playerId, InGameObject unit, IGame game) {
        return isValid(playerId, game) && isValidEntity(unit, game);
    }

    /**
     * Returns true if this player turn requires the player to provide actions for more than one unit. This is
     * true for example when vehicles must be moved as a lance. This is not equivalent to providing multiple
     * actions, such as in firing multiple weapons. That is not considered a multi-turn.
     * By default, this method returns false.
     *
     * @return True when this turn requires actions for at least two units
     */
    default boolean isMultiTurn() {
        return false;
    }
}
