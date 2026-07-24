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
package megamek.common.units;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.logging.MMLogger;

/**
 * Force Generator availability declared inside a unit file (.mtf or .blk). This lets a custom unit tell the Force
 * Generator which factions field it and how often, without editing the canon era files in data/forcegenerator.
 * <p>
 * Note this is NOT the tech availability rating (see {@link megamek.common.enums.AvailabilityValue} and
 * {@link megamek.common.interfaces.ITechnology}), which rates how hard equipment is to obtain on an A-F/X scale.
 * </p>
 * <p>
 * A single line holds an optional year range followed by a comma-separated list of availability codes:
 * </p>
 *
 * <pre>
 * availability:FS:5,LA:3                 intro year to extinction
 * availability:3067-3085 FS:7,LA:6       explicit year range
 * availability:3067- FS:7                open-ended range
 * </pre>
 * <p>
 * The codes use the same FKEY:AV[+/-][:YEAR] format as the era XML files, so they are handed to
 * {@code AvailabilityRating} unchanged. AV runs 0 (none) to 10 (ubiquitous) on a base-2 log scale, where 6 is typical
 * and every +2 doubles how often the unit appears.
 * </p>
 *
 * @param startYear          first year this entry applies, or {@link #UNSPECIFIED_YEAR} to start at the unit's
 *                           introduction year
 * @param endYear            last year this entry applies, or {@link #UNSPECIFIED_YEAR} for no end
 * @param availabilityCodes  comma-separated availability codes, e.g. {@code "FS:5,LA:3"}
 */
public record ForceGeneratorAvailability(int startYear, int endYear, String availabilityCodes)
      implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final MMLogger LOGGER = MMLogger.create(ForceGeneratorAvailability.class);

    /** Marks a year that the unit file did not specify, so the unit's own introduction/extinction dates are used. */
    public static final int UNSPECIFIED_YEAR = 0;

    /**
     * Matches a leading year range, e.g. "3067-3085 FS:7,LA:6", "3067- FS:7", or "-3068 FS:2".
     * <p>
     * Either end of the range may be left off: "3067-" starts in 3067 with no end, and "-3068" runs from the unit's
     * own start through 3068. The range and the codes may be separated by a space or a colon; the documented form uses
     * a space, but the codes are full of colons ("ZN:8"), so a player naturally writes "3056-3061:ZN:8". Both are
     * accepted, and a range prefix can never be a faction code, so there is nothing to confuse it with.
     * </p>
     */
    private static final Pattern YEAR_RANGE = Pattern.compile("^(\\d{3,4})?\\s*-\\s*(\\d{3,4})?[\\s:]+(.+)$");

    public ForceGeneratorAvailability {
        if ((availabilityCodes == null) || availabilityCodes.isBlank()) {
            throw new IllegalArgumentException("Availability codes must not be blank");
        }
        if ((startYear != UNSPECIFIED_YEAR) && (endYear != UNSPECIFIED_YEAR) && (endYear < startYear)) {
            throw new IllegalArgumentException("Availability end year " + endYear
                  + " is before start year " + startYear);
        }
        availabilityCodes = availabilityCodes.trim();
    }

    /**
     * Parses a single availability line from a unit file. The line must not include the {@code availability:} prefix.
     *
     * @param line the line contents, e.g. {@code "FS:5,LA:3"} or {@code "3067-3085 FS:7"}
     *
     * @return the parsed entry
     *
     * @throws IllegalArgumentException if the line is blank or the year range is malformed
     */
    public static ForceGeneratorAvailability parse(String line) {
        if ((line == null) || line.isBlank()) {
            throw new IllegalArgumentException("Availability line must not be blank");
        }

        String trimmedLine = line.trim();
        Matcher yearRangeMatcher = YEAR_RANGE.matcher(trimmedLine);
        if (!yearRangeMatcher.matches()
              || ((yearRangeMatcher.group(1) == null) && (yearRangeMatcher.group(2) == null))) {
            // No leading range, or a bare "-" with neither year: treat the whole line as codes with no year limit
            return new ForceGeneratorAvailability(UNSPECIFIED_YEAR, UNSPECIFIED_YEAR, trimmedLine);
        }

        int parsedStartYear = (yearRangeMatcher.group(1) == null)
              ? UNSPECIFIED_YEAR
              : Integer.parseInt(yearRangeMatcher.group(1));
        int parsedEndYear = (yearRangeMatcher.group(2) == null)
              ? UNSPECIFIED_YEAR
              : Integer.parseInt(yearRangeMatcher.group(2));
        return new ForceGeneratorAvailability(parsedStartYear, parsedEndYear, yearRangeMatcher.group(3));
    }

    /**
     * Parses every availability line from a unit file, skipping any that are malformed. A bad line is logged and
     * dropped rather than failing the load, so a typo cannot make the unit unusable.
     *
     * @param lines    the line contents, without the {@code availability:} prefix
     * @param unitName the unit these lines came from, used for logging
     *
     * @return the parsed entries, in file order; empty if there are none
     */
    public static List<ForceGeneratorAvailability> parseAll(Collection<String> lines, String unitName) {
        List<ForceGeneratorAvailability> entries = new ArrayList<>();
        if (lines == null) {
            return entries;
        }

        for (String line : lines) {
            if ((line == null) || line.isBlank()) {
                continue;
            }
            try {
                entries.add(parse(line));
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("[UnitAvailability] Ignoring malformed availability line '{}' in unit {}: {}",
                      line,
                      unitName,
                      exception.getMessage());
            }
        }

        return entries;
    }

    /**
     * Resolves the first year this entry applies, falling back to the unit's introduction year when the file did not
     * specify one.
     *
     * @param introductionYear the unit's introduction year
     *
     * @return the effective start year
     */
    public int effectiveStartYear(int introductionYear) {
        return (startYear == UNSPECIFIED_YEAR) ? introductionYear : startYear;
    }

    /**
     * Resolves the last year this entry applies. An unspecified end year means the unit never drops out.
     *
     * @return the effective end year
     */
    public int effectiveEndYear() {
        return (endYear == UNSPECIFIED_YEAR) ? Integer.MAX_VALUE : endYear;
    }

    /**
     * Tests whether this entry overlaps an era bucket. Era buckets run from their own year up to the year before the
     * next era begins.
     *
     * @param eraStartYear     first year of the era bucket
     * @param eraEndYear       last year of the era bucket, or {@link Integer#MAX_VALUE} for the final era
     * @param introductionYear the unit's introduction year, used when the file gave no start year
     *
     * @return {@code true} if any part of this entry falls inside the era
     */
    public boolean appliesToEra(int eraStartYear, int eraEndYear, int introductionYear) {
        return (effectiveStartYear(introductionYear) <= eraEndYear) && (effectiveEndYear() >= eraStartYear);
    }

    /**
     * Renders this entry the way it appears in a unit file, without the {@code availability:} prefix.
     *
     * @return the file representation of this entry
     */
    public String toFileFormat() {
        // No range at all: just the codes.
        if ((startYear == UNSPECIFIED_YEAR) && (endYear == UNSPECIFIED_YEAR)) {
            return availabilityCodes;
        }
        // Either end may be open. "3050-" starts in 3050 with no end; "-3060" runs from the intro year through 3060.
        // Leaving the start off here is what previously dropped the end year, which read back as "never stops".
        String startYearText = (startYear == UNSPECIFIED_YEAR) ? "" : String.valueOf(startYear);
        String endYearText = (endYear == UNSPECIFIED_YEAR) ? "" : String.valueOf(endYear);
        return startYearText + "-" + endYearText + " " + availabilityCodes;
    }
}
