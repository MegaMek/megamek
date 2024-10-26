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

import megamek.common.hexarea.HexArea;

/**
 * This interface is implemented by those units (by InGameObjects) that can be deployed either offboard or on a board. There are
 * InGameObjects that are only targets (HexTarget) and may thus not actually be deployable. All Deployable objects could theoretically be
 * listed in the lobby's unit list.
 */
public interface Deployable {

    /**
     * Returns true when this unit/object is deployed, i.e. it has arrived in the game and may perform actions or be targeted by actions.
     * Usually that means it has a fixed position on a board. Offboard units also count as undeployed as long as they cannot perform actions
     * and as deployed when they can.
     */
    boolean isDeployed();

    /**
     * Returns the round that this unit/object is to be deployed on the board or offboard.
     */
    int getDeployRound();

    /**
     * @return True if this unit has its own area it is allowed to flee the board(s) from; false if the unit's owner should be asked
     * instead.
     */
    default boolean hasFleeZone() {
        return false;
    }

    /**
     * @return The area of the board(s) this unit is allowed to flee from; the return value is only valid when {@link #hasFleeZone()}
     * returns true. Normally this method should not be called, use {@link AbstractGame#canFleeFrom(Deployable, Coords)} instead.
     * @see AbstractGame#canFleeFrom(Deployable, Coords)
     * @see #hasFleeZone()
     */
    default HexArea getFleeZone() {
        return HexArea.EMPTY_AREA;
    }
}
