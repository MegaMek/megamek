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
package megamek.common.strategicBattleSystems;

import megamek.common.AbstractPlayerTurn;

/**
 * This is a subclass of AbstractPlayerTurn that is (for now) only used to have a clear SBF designated
 * turn so that SBF classes don't use AbstractPlayerTurn directly. At the moment this class is not strictly
 * necessary, but maybe it gets additions later.
 */
public abstract class SBFTurn extends AbstractPlayerTurn {

    public SBFTurn(int playerId) {
        super(playerId);
    }

    /**
     * Returns true when this turn can be played given the current state of the given game. Should
     * return false when e.g. no valid formation can be found to move for the player or the player or
     * formation of the turn is null (e.g. because it has previously been destroyed).
     *
     * @param game The game object
     * @return True when the turn can be played, false when it should be skipped
     */
    public abstract boolean isValid(SBFGame game);
}
