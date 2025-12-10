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

package megamek.common.strategicBattleSystems;

import megamek.common.game.AbstractPlayerTurn;

/**
 * This is a subclass of AbstractPlayerTurn that is (for now) only used to have a clear SBF designated turn so that SBF
 * classes don't use AbstractPlayerTurn directly. At the moment this class is not strictly necessary, but maybe it gets
 * additions later.
 */
public abstract class SBFTurn extends AbstractPlayerTurn {

    public SBFTurn(int playerId) {
        super(playerId);
    }

    /**
     * Returns true when this turn can be played given the current state of the given game. Should return false when
     * e.g. no valid formation can be found to move for the player or the player or formation of the turn is null (e.g.
     * because it has previously been destroyed).
     *
     * @param game The game object
     *
     * @return True when the turn can be played, false when it should be skipped
     */
    public abstract boolean isValid(SBFGame game);
}
