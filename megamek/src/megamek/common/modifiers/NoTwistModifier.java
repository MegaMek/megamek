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
package megamek.common.modifiers;

/**
 * This modifier prevents the unit from torso/turret twisting. The modifier is used e.g. for salvage quality Gyro/Controls. It would ideally
 * be applied to such equipment but MM doesn't represent these as Mounted so it must be applied to the unit instead.
 */
public class NoTwistModifier extends AbstractSystemModifier {

    /**
     * Creates a modifier that prevents torso/turret twists when applied to a unit.
     *
     * @param reason The origin of the modifier
     * @param entitySystem The system that the modifier applies to
     */
    public NoTwistModifier(Reason reason, EntitySystem entitySystem) {
        super(reason, entitySystem);
    }
}
