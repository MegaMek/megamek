/*
 * Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.interfaces;

public interface IStartingPositions {
    String[] START_LOCATION_NAMES = { "Any", "NW", "N",
                                      "NE", "E", "SE", "S", "SW", "W", "EDG", "CTR", "NW (deep)",
                                      "N (deep)", "NE (deep)", "E (deep)", "SE (deep)", "S (deep)",
                                      "SW (deep)", "W (deep)", "EDG (deep)" };


    /**
     * Returns a display name for the given starting position index. "Custom" is returned for any value of index outside
     * the standard starting positions (the method is safe to call for all values of index).
     *
     * @param index the starting position index
     *
     * @return A name, e.g. "Any" for index 0
     */
    static String getDisplayName(int index) {
        if ((index >= 0) && (index < START_LOCATION_NAMES.length)) {
            return START_LOCATION_NAMES[index];
        } else {
            return "Custom";
        }
    }
}
