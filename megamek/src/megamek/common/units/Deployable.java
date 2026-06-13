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

package megamek.common.units;

import megamek.common.board.Coords;
import megamek.common.game.AbstractGame;
import megamek.common.hexArea.HexArea;

/**
 * This interface is implemented by those units (by InGameObjects) that can be deployed either offboard or on a board.
 * There are InGameObjects that are only targets (HexTarget) and may thus not actually be deployable. All Deployable
 * objects could theoretically be listed in the lobby's unit list.
 */
public interface Deployable {

    /**
     * Returns true when this unit/object is deployed, i.e. it has arrived in the game and may perform actions or be
     * targeted by actions. Usually that means it has a fixed position on a board. Offboard units also count as
     * un-deployed as long as they cannot perform actions and as deployed when they can.
     */
    boolean isDeployed();

    /**
     * Returns the round that this unit/object is to be deployed on the board or offboard.
     */
    int getDeployRound();

    /**
     * @return True if this unit has its own area it is allowed to flee the board(s) from; false if the unit's owner
     *       should be asked instead.
     */
    default boolean hasFleeZone() {
        return false;
    }

    /**
     * @return The area of the board(s) this unit is allowed to flee from; the return value is only valid when
     *       {@link #hasFleeZone()} returns true. Normally this method should not be called, use
     *       {@link AbstractGame#canFleeFrom(Deployable, Coords)} instead.
     *
     * @see AbstractGame#canFleeFrom(Deployable, Coords)
     * @see #hasFleeZone()
     */
    default HexArea getFleeZone() {
        return HexArea.EMPTY_AREA;
    }

    /**
     * @return The board ID of the board this deployable is on.
     */
    default int getBoardId() {
        // This defaults to 0 so that any code that doesn't  support multiple boards yet safely points to the first
        // (and only) board; should eventually default to -1
        return 0;
    }
}
