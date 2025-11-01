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
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.server.totalWarfare.TWGameManager;

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

    /**
     * Returns true if the carryable object is able to be picked up.
     * @param isCarrierHullDown is the unit that's picking this up hull down, or otherwise able to pick up
     *                          ground-level objects
     * @return true if the object can be picked up, false if it cannot
     */
    boolean canBePickedUp(boolean isCarrierHullDown);

    default CarriedObjectDamageAllocation getCarriedObjectDamageAllocation() {
        return CarriedObjectDamageAllocation.NEVER;
    }

    void processPickupStep(MoveStep step, Integer cargoPickupLocation,
          TWGameManager gameManager, Entity entityPickingUpTarget, EntityMovementType overallMoveType);

    /**
     * Despite being carried by a unit in the same manner, the type of object impacts when it should be damaged if a
     * carried object's carrier is attacked.
     *  - Cargo (TW 261) always has a chance to get damaged when the carrier is hit
     *  - Handheld Weapons (TO:AUE 128) and Battle Armor (TO:AR 96) have a chance to be hit when the carrier is hit in
     *  the arms
     *  - Vehicles (TO:AR 95) follow the Grappling rules (TO:AR 88): If the attack misses, make another attack
     *  against the other unit (NOT IMPLEMENTED)
     */
    enum CarriedObjectDamageAllocation {
        ANY_HIT,
        ARM_HIT,
        ON_MISS,
        NEVER;

        public boolean isCarryableAlwaysDamaged() {
            return this == ANY_HIT;
        }

        public boolean isCarryableDamageOnArmHit() {
            return this == ARM_HIT;
        }

        public boolean isCarryableAttackedOnMiss() {
            return this == ON_MISS;
        }

    }

    default int targetForArmHitToHitCarriedObject() {
        return 0;
    }
}
