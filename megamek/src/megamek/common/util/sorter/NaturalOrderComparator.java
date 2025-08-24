/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util.sorter;

import java.io.Serial;
import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;

/**
 * A comparator that compares the inputs based on natural sort order.
 * <p>
 * Natural sort order is an easier to parse format that counts multi-digit numbers atomically (as a single number)
 * <p>
 * Windows File Explorer uses this format for files as it is more human-friendly, but ASCII sort order is more common in
 * computer programs because of the ease of programming in that order.
 * <p>
 * To showcase how this works, below is an example: The list of Strings { "Atlas 0", "Atlas 15", "Atlas 2", "Atlas 1",
 * "Atlas 5" } would be sorted into { "Atlas 0", "Atlas 1", "Atlas 2", "Atlas 5", "Atlas 15" } instead of ASCII's {
 * "Atlas 0", "Atlas 1", "Atlas 15", "Atlas 2", "Atlas 5" }
 */
public class NaturalOrderComparator implements Comparator<String>, Serializable {
    @Serial
    private static final long serialVersionUID = -5116813198443091269L;
    private final Collator collator;

    public NaturalOrderComparator() {
        this(Collator.PRIMARY);
    }

    public NaturalOrderComparator(int collatorStrength) {
        collator = Collator.getInstance();
        collator.setStrength(collatorStrength);
    }

    @Override
    public int compare(String a, String b) {
        for (int ii = 0; ii < a.length() && ii < b.length(); ) {
            char ca = a.charAt(ii), cb = b.charAt(ii);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                // compare two runs of numbers by finding the end
                // of each and comparing magnitudes then values.

                // find the ends of the numbers
                // ii: first digit
                int da = 0, db = 0;
                while ((ii + da) < a.length() && Character.isDigit(a.charAt(ii + da))) {
                    da++;
                }
                while ((ii + db) < b.length() && Character.isDigit(b.charAt(ii + db))) {
                    db++;
                }

                // ii + da, ii + db: one past the end of each number
                int diff = Integer.compare(da, db);
                if (diff != 0) {
                    // MISMATCH: magnitudes differ
                    return diff;
                }

                // da == db: magnitudes are equal, we can compare
                //           base-10 numbers left to right.
                for (; da > 0; --da, ++ii) {
                    ca = a.charAt(ii);
                    cb = b.charAt(ii);
                    if (ca != cb) {
                        diff = Integer.compare(
                              Character.getNumericValue(ca),
                              Character.getNumericValue(cb));
                        if (diff != 0) {
                            return diff;
                        }
                    }
                }

                // if we've reached here ii points to the next
                // non-digit character or the end of the strings
                continue;
            } else if (ca != cb) {
                // compare any two other characters
                int diff = collator.compare(String.valueOf(ca), String.valueOf(cb));
                if (diff != 0) {
                    return diff;
                }
            }

            ii++;
        }

        // one string is shorter than the other,
        // or they are both the same length.
        return Integer.compare(a.length(), b.length());
    }
}
