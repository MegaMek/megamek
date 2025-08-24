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

package megamek.common.interfaces;

import java.io.Serializable;

import megamek.common.game.IGame;
import megamek.common.game.InGameObject;

/**
 * This interface is implemented by player turns (e.g. GameTurn), where a specific player has to declare their
 * action(s). GameTurn is specific to Total Warfare/Entity turns.
 */
public interface PlayerTurn extends Serializable {

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
     * Returns true if this player turn requires the player to provide actions for more than one unit. This is true for
     * example when vehicles must be moved as a lance. This is not equivalent to providing multiple actions, such as in
     * firing multiple weapons. That is not considered a multi-turn. By default, this method returns false.
     *
     * @return True when this turn requires actions for at least two units
     */
    default boolean isMultiTurn() {
        return false;
    }
}
