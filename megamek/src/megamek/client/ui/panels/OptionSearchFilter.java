/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.panels;

import java.util.Locale;

/**
 * The text matching behind the search boxes on the option lists (special pilot abilities, quirks).
 *
 * <p>Matching is a case-insensitive substring test. Callers normalize a row's searchable text once, when the row is
 * built, and normalize the filter text once per filter pass, so a keystroke costs one {@link String#contains} per
 * row rather than a fresh lower-casing of everything.</p>
 */
public final class OptionSearchFilter {

    private OptionSearchFilter() {
    }

    /**
     * @param text any display text
     *
     * @return the text folded to a form suitable for matching. Always apply this to both sides before calling
     *       {@link #matches(String, String)}.
     */
    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    /**
     * @param normalizedSearchText a row's searchable text, already {@link #normalize(String) normalized}
     * @param normalizedFilter     the filter text, already normalized
     *
     * @return {@code true} if the row should stay visible: an empty filter matches everything, otherwise the search
     *       text must contain the filter
     */
    public static boolean matches(String normalizedSearchText, String normalizedFilter) {
        return normalizedFilter.isEmpty() || normalizedSearchText.contains(normalizedFilter);
    }
}
