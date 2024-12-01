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
 * This interface is implemented by modifiers that affect systems of a unit that don't have a Mounted or other object to represent
 * them such as Gyro. Also, modifiers that aren't cumulative can be represented as a unit modifier.
 */
public interface SystemModifier extends EquipmentModifier {

    /**
     * The systems that a modifier can apply to and which are not Mounteds in Entity
     */
    enum EntitySystem {
        GYRO, CONTROLS, LIFE_SUPPORT, COCKPIT, JUMP_JETS
    }

    /**
     * @return The system (Gyro, Cockpit etc) that this modifier comes from.
     */
    EntitySystem system();
}
