/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serializable;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.InGameObject;

public interface Targetable extends InGameObject, Serializable {
    int TYPE_ENTITY = 0;
    int TYPE_HEX_CLEAR = 1;
    int TYPE_HEX_IGNITE = 2;
    int TYPE_HEX_TAG = 19;
    int TYPE_BUILDING = 3;
    int TYPE_BLDG_IGNITE = 4;
    int TYPE_BLDG_TAG = 20;
    int TYPE_MINEFIELD_CLEAR = 5;
    int TYPE_MINEFIELD_DELIVER = 6;
    int TYPE_HEX_ARTILLERY = 7;
    int TYPE_HEX_EXTINGUISH = 8;
    int TYPE_I_NARC_POD = 11;
    int TYPE_SEARCHLIGHT = 12;
    int TYPE_FLARE_DELIVER = 13;
    int TYPE_HEX_BOMB = 14;
    int TYPE_FUEL_TANK = 15;
    int TYPE_FUEL_TANK_IGNITE = 16;
    int TYPE_HEX_SCREEN = 17;
    int TYPE_HEX_AERO_BOMB = 18;

    int getTargetType();

    /** @return the coordinates of the hex containing the target */
    Coords getPosition();

    /**
     * @return The board ID of the board this targetable is on. This defaults to 0 so that any code that doesn't support
     *       multiple boards yet safely points to the first (and only) board; should eventually default to -1
     */
    default int getBoardId() {
        return 0;
    }

    /**
     * @see BoardLocation#of(Coords, int)
     */
    default BoardLocation getBoardLocation() {
        return BoardLocation.of(getPosition(), getBoardId());
    }

    default boolean isOnBoard(int boardID) {
        return getBoardLocation().isOn(boardID);
    }

    Map<Integer, Coords> getSecondaryPositions();

    /**
     * @return elevation of the top (e.g. torso) of the target relative to surface
     */
    int relHeight();

    /**
     * Returns the height of the target, that is, how many levels above its elevation it is for LOS purposes.
     *
     * @return height of the target in elevation levels
     */
    int getHeight();

    /**
     * Returns the elevation of this target, relative to the position Hex's surface
     *
     * @return elevation of the bottom (e.g. legs) of the target relative to surface
     */
    int getElevation();

    /**
     * @return altitude of target
     */
    int getAltitude();

    /**
     * Returns true if the target is considered immobile (-4 to hit). If this is a game unit, implementations should
     * check the status of the unit (shutdown, damage) and also the status of the crew (unconscious) if any.
     *
     * @return True if the target is considered immobile
     */
    boolean isImmobile();

    /** @return name of the target for ui purposes */
    String getDisplayName();

    /** @return side hit from location */
    int sideTable(Coords src);

    /** @return side hit from location */
    int sideTable(Coords src, boolean usePrior);

    /** @return if this is off the board */
    boolean isOffBoard();

    /**
     * @return if this is an <code>Entity</code> capable of carrying and using bombs
     */
    default boolean isBomber() {
        return false;
    }

    /**
     * @return Is the entity airborne in the fashion of an aerospace unit? Does not include VTOL movement (see
     *       {@link Targetable#isAirborneVTOLorWIGE()}
     */
    boolean isAirborne();

    /**
     * @return is the entity airborne in the fashion of a VTOL Not used for aerospace units, see
     *       {@link Targetable#isAirborne()}
     */
    boolean isAirborneVTOLorWIGE();

    // Make sure Targetable implements both
    @Override
    boolean equals(Object obj);

    /**
     * Determines if this target should be considered the enemy of the supplied player.  Targets that aren't owned by
     * any player, such as buildings or terrain, are always considered enemies, since this will most often be used to
     * determine if something is valid to be shot at.
     *
     */
    boolean isEnemyOf(Entity other);

    default boolean isHexBeingBombed() {
        return getTargetType() == TYPE_HEX_AERO_BOMB || getTargetType() == TYPE_HEX_BOMB;
    }

    /**
     * Used to identify a target that tracks heat buildup (Meks, ASFs, and small craft).
     *
     * @return Whether the target tracks heat buildup.
     */
    default boolean tracksHeat() {
        return false;
    }

    default boolean isBracing() {
        return false;
    }

    @Override
    int hashCode();

    /**
     * Utility function used to safely tell whether two Targetable's are in the same hex. Does not throw exceptions in
     * case of nulls.
     */
    static boolean areAtSamePosition(@Nullable Targetable first, @Nullable Targetable second) {
        if ((first == null) || (second == null) ||
              (first.getPosition() == null) || (second.getPosition() == null)) {
            return false;
        }

        return first.getPosition().equals(second.getPosition());
    }


    /**
     * Replaced most instances of `instanceof GunEmplacement` to support {@link BuildingEntity}
     *
     * @return true if this unit is a {@link BuildingEntity} or {@link GunEmplacement}, false otherwise
     */
    default boolean isBuildingEntityOrGunEmplacement() {
        return false;
    }
}
