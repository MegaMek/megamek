/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;


/**
 * Interface for entity flags, used by {@link EquipmentBitSet}. Allows for easy flag manipulation with enums that
 * implement this interface. Also provides a default method to convert a single flag to a bitset so multiple flags can
 * be composed into an {@link EquipmentBitSet}.
 *
 * @author Luana Coppio
 */
public interface EquipmentFlag {
    /**
     * Returns the index of the flag in the bitset
     *
     * @return the index of the flag in the bitset
     */
    int ordinal();

    /**
     * Converts this flag to an {@link EquipmentBitSet}.
     *
     * @return an {@link EquipmentBitSet} with this flag set
     */
    default EquipmentBitSet asEquipmentBitSet() {
        return new EquipmentBitSet().or(this);
    }
}
