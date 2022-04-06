/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.codeUtilities;

import megamek.common.annotations.Nullable;

public class StringUtility {
    /**
     * @param text The string to be evaluated.
     * @return true if the passed in value is either null or a blank string
     */
    public static boolean isNullOrEmpty(final @Nullable String text) {
        return (text == null) || text.isBlank();
    }

    /**
     * @param text The string to be evaluated.
     * @return true if the passed in <code>StringBuilder</code> is null, or its contents are null
     * or empty
     */
    public static boolean isNullOrEmpty(final @Nullable StringBuilder text) {
        return (text == null) || isNullOrEmpty(text.toString());
    }
}
