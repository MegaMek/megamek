/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
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
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.universe;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;

/**
 * Stores details about a leader of a faction, including name, gender, and years of tenure.
 *
 * <p>This record encapsulates all relevant information about an individual who has led a given faction, such as
 * their title, full name, gender, period in office, and any additional honorifics. It also provides utility methods for
 * constructing full display titles and for validating the leader's tenure for a given year.</p>
 *
 * @param title     the leader's title (e.g., "Duke", "Commander")
 * @param firstName the leader's given name
 * @param surname   the leader's surname; may be {@code null}
 * @param honorific additional information following the leader's name (e.g., "III"); may be {@code null}
 * @param gender    the gender of the leader
 * @param startYear the first year of the leader's tenure, or {@code null} if unknown
 * @param endYear   the last year of the leader's tenure, or {@code null} if unknown
 *
 * @author Illiani
 * @since 0.50.07
 */
public record FactionLeaderData(
      String title,
      String firstName,
      @Nullable String surname,
      @Nullable String honorific,
      Gender gender,
      @Nullable Integer startYear,
      @Nullable Integer endYear
) {
    /** Constant indicating that no year was specified. */
    final static int NO_YEAR = -1;

    /**
     * Compact constructor that replaces any {@code null} surname or honorific with empty strings, and sets missing
     * years to {@link #NO_YEAR}.
     *
     * @param title     the leader's title
     * @param firstName the leader's given name
     * @param surname   the leader's surname; may be {@code null}
     * @param honorific additional name information; may be {@code null}
     * @param gender    the leader's gender
     * @param startYear the first year of the leader's tenure; may be {@code null}
     * @param endYear   the last year of the leader's tenure; may be {@code null}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionLeaderData {
        if (surname == null) {
            surname = "";
        }
        if (honorific == null) {
            honorific = "";
        }
        if (startYear == null) {
            startYear = NO_YEAR;
        }
        if (endYear == null) {
            endYear = NO_YEAR;
        }
    }

    /**
     * Returns the full name of the leader, constructed from the first name and surname.
     *
     * @return the full name (first name and surname separated by a space)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getFullName() {
        return firstName + " " + surname;
    }

    /**
     * Constructs and returns the full formal title of the leader.
     *
     * @param excludeFirstOfTheirName if true, omits the honorific "I" (used for the first of their name)
     *
     * @return the complete title line for the leader, including honorifics
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getFullTitle(boolean excludeFirstOfTheirName) {
        String honorificToUse = honorific;
        if (honorific.equals("I") && excludeFirstOfTheirName) {
            honorificToUse = "";
        }

        if (!honorificToUse.isBlank()) {
            honorificToUse += ' ';
        }

        return title + ' ' + getFullName() + honorificToUse;
    }

    /**
     * Determines if this leader was in office during the specified year.
     *
     * @param year the year to check for validity
     *
     * @return {@code true} if the year is within the leader's tenure, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isValidInYear(int year) {
        return (startYear == NO_YEAR || year >= startYear)
                     && (endYear == NO_YEAR || year < endYear);
    }
}
