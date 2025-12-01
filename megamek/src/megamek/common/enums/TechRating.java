/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.enums;

import java.util.HashMap;
import java.util.Map;

// --- Tech Rating Enum ---
public enum TechRating {
    A(0, "A"),
    B(1, "B"),
    C(2, "C"),
    D(3, "D"),
    E(4, "E"),
    F(5, "F");

    private final int index;
    private final String name;
    private static final Map<Integer, TechRating> INDEX_LOOKUP = new HashMap<>();
    private static final Map<String, TechRating> NAME_LOOKUP = new HashMap<>();

    static {
        for (TechRating tr : values()) {
            INDEX_LOOKUP.put(tr.index, tr);
            NAME_LOOKUP.put(tr.name, tr);
        }
    }

    TechRating(int idx, String name) {
        this.index = idx;
        this.name = name;
    }

    public int getIndex() {return index;}

    public String getName() {return name;}

    /**
     * Determines whether this {@link TechRating} represents a higher technological level than the specified rating.
     *
     * <p>The comparison is based on the internal {@code index} value; a larger index indicates a more advanced
     * rating.</p>
     *
     * @param other the {@link TechRating} to compare against
     *
     * @return {@code true} if this rating is strictly higher than {@code other}
     */
    public boolean isBetterThan(TechRating other) {
        return this.index > other.index;
    }

    /**
     * Determines whether this {@link TechRating} represents a technological level that is greater than or equal to the
     * specified rating.
     *
     * <p>The comparison is based on the internal {@code index} value; a larger or equal index indicates an
     * equivalent or more advanced rating.</p>
     *
     * @param other the {@link TechRating} to compare against
     *
     * @return {@code true} if this rating is greater than or equal to {@code other}
     */
    public boolean isBetterOrEqualThan(TechRating other) {
        return this.index >= other.index;
    }

    public static TechRating fromIndex(int idx) {
        TechRating tr = INDEX_LOOKUP.get(idx);
        if (tr == null) {throw new IllegalArgumentException("Invalid TechRating index: " + idx);}
        return tr;
    }

    public static TechRating fromName(String name) {
        TechRating tr = NAME_LOOKUP.get(name);
        if (tr == null) {throw new IllegalArgumentException("Invalid TechRating name: " + name);}
        return tr;
    }
}
