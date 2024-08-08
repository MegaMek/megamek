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

/**
 * An interface defining all the required properties of a carryable object.
 */
public interface ICarryable extends InGameObject {

	/**
	 * @return The weight of the carryable object in units of tons.
	 */
	double getTonnage();

	/**
	 * Damages this carryable object by the given amount of damage. Returns true if the cargo is considered
	 * destroyed by applying the damage.
	 * Note: This method does *not* check if the object is invulnerable; it is up to the caller to do that.
	 * Calling this method on an invulnerable carryable object behaves exactly like calling it on a vulnerable
	 * one.
	 *
	 * @param amount The damage
	 * @return True if the cargo is destroyed by the damage, false otherwise
	 * @see #getTonnage()
	 */
	boolean damage(double amount);

	/**
	 * Returns true if this carryable object should never take damage nor be destroyed, false otherwise.
	 * Note that the carryable object does *not* itself enforce this; it is up to the caller of
	 * {@link #damage(double)} to test it.
	 *
	 * @return True if this carryable object cannot be damaged
	 */
	boolean isInvulnerable();

	@Override
	default boolean isCarryableObject() {
		return true;
	}
}