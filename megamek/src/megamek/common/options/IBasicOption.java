/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.options;

import java.util.Arrays;

/**
 * Basic option. It's just <code>String</code> name - <code>Object</code> value pair
 */
public interface IBasicOption {

    /**
     * Returns the option name
     *
     * @return name of the option
     */
    String getName();

    /**
     * Returns the option value
     *
     * @return option value
     */
    Object getValue();

    /** @return True when this Option's name is equal to the given Option name. */
    default boolean is(String otherName) {
        return isAnyOf(otherName);
    }

    /** @return True when this Option's name is equal to at least one of the given Option names. */
    default boolean isAnyOf(String otherName, String... otherNames) {
        return getName().equals(otherName) || Arrays.stream(otherNames).anyMatch(name -> getName().equals(name));
    }

    /** @return True when this Option's name is not equal to any of the given names. */
    default boolean isNoneOf(String otherName, String... otherNames) {
        return !isAnyOf(otherName, otherNames);
    }
}
