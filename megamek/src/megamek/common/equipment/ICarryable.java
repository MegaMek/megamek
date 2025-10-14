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

package megamek.common.equipment;

import megamek.common.game.InGameObject;

/**
 * An interface defining all the required properties of a carryable object.
 */
public interface ICarryable extends InGameObject {

    /**
     * @return The weight of the carryable object in units of tons.
     */
    double getTonnage();

    /**
     * Damages this carryable object by the given amount of damage. Returns true if the cargo is considered destroyed by
     * applying the damage. Note: This method does *not* check if the object is invulnerable; it is up to the caller to
     * do that. Calling this method on an invulnerable carryable object behaves exactly like calling it on a vulnerable
     * one.
     *
     * @param amount The damage
     *
     * @return True if the cargo is destroyed by the damage, false otherwise
     *
     * @see #getTonnage()
     */
    boolean damage(double amount);

    /**
     * Returns true if this carryable object should never take damage nor be destroyed, false otherwise. Note that the
     * carryable object does *not* itself enforce this; it is up to the caller of {@link #damage(double)} to test it.
     *
     * @return True if this carryable object cannot be damaged
     */
    boolean isInvulnerable();

    @Override
    default boolean isCarryableObject() {
        return true;
    }
}
