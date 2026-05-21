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
package megamek.common.universe;

import java.util.List;

import megamek.common.annotations.Nullable;

/**
 * A faction-specific rank table loaded from {@code data/universe/ranks.xml}. The index of each entry in {@link #ranks}
 * corresponds to the integer rank value stored on a person (E0 at index 0, E1 at 1, etc.) — the same scheme ratgen
 * {@code CommanderNode} uses when it picks {@code %CAPTAIN%} / {@code %COLONEL%} for a force.
 *
 * <p>Each {@code rankNames} string is a comma-separated row of profession-specific names (None, Naval, MW, MW, MW, MW,
 * Tech, Tech, Admin). A {@code -} placeholder means "use column 0 / the default name."</p>
 *
 * @param code  The faction rank-system code, e.g. "LAAF", "DCMS", "CLAN".
 * @param name  The display name of the rank system.
 * @param ranks The rank rows in ascending order. Element 0 is the rank int 0, element 1 is rank int 1, and so on.
 */
public record RankSystem(String code, String name, List<String[]> ranks) {

    private static final int DEFAULT_PROFESSION_COLUMN = 0;
    private static final String INHERIT_PLACEHOLDER = "-";

    /**
     * Resolves the rank int to its short display name, e.g. rank 34 in DCMS → "Tai-i". Returns {@code null} when the
     * rank index is out of range or the row only contains placeholders.
     *
     * @param rankIndex The zero-based rank index stored on the person.
     *
     * @return The rank's display name, or {@code null} if no name is defined at that index.
     */
    public @Nullable String nameAt(int rankIndex) {
        if (rankIndex < 0 || rankIndex >= ranks.size()) {
            return null;
        }
        String[] row = ranks.get(rankIndex);
        if (row.length == 0) {
            return null;
        }
        String preferred = column(row, DEFAULT_PROFESSION_COLUMN);
        if (preferred != null) {
            return preferred;
        }
        // Fall back to the first non-placeholder column so an XML row that only defines, e.g., a
        // "Spaceman Recruit" naval entry still produces a meaningful label instead of nothing.
        for (int i = 0; i < row.length; i++) {
            String value = column(row, i);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static @Nullable String column(String[] row, int index) {
        if (index < 0 || index >= row.length) {
            return null;
        }
        String value = row[index].trim();
        if (value.isEmpty() || INHERIT_PLACEHOLDER.equals(value)) {
            return null;
        }
        return value;
    }
}
