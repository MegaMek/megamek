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
package megamek.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;

/**
 * Reads a typed list of unit IDs such as {@code 3-7, 10}: single IDs and inclusive ranges, separated by commas.
 *
 * <p>Spaces around the commas and the dash are allowed, and a range may be written either way round, so
 * {@code 7-3} means the same units as {@code 3-7}. Anything that cannot be read is handed back rather than dropped,
 * so the dialog can show the player what was ignored instead of silently adding fewer units than they typed.</p>
 */
public final class UnitIdListParser {

    /**
     * The most IDs one range may cover. A typo such as {@code 1-99999} would otherwise build a set of a hundred
     * thousand IDs, none of which exist; no game has anywhere near this many units.
     */
    public static final int MAXIMUM_RANGE_SIZE = 500;

    private static final Pattern SINGLE_ID = Pattern.compile("\\d+");
    private static final Pattern ID_RANGE = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)");

    /**
     * The outcome of reading a unit ID list.
     *
     * @param unitIds           every ID the text named, in ascending order, without duplicates
     * @param unreadableEntries the comma-separated entries that could not be read, trimmed, in the order typed
     */
    public record Result(SortedSet<Integer> unitIds, List<String> unreadableEntries) {

        /**
         * @return {@code true} when every entry in the text could be read
         */
        public boolean isFullyRead() {
            return unreadableEntries.isEmpty();
        }
    }

    private UnitIdListParser() {}

    /**
     * Reads a comma-separated list of unit IDs and ID ranges.
     *
     * @param text the typed text, or {@code null}, which reads as an empty list
     *
     * @return the IDs named and the entries that could not be read; never {@code null}
     */
    public static Result parse(@Nullable String text) {
        SortedSet<Integer> unitIds = new TreeSet<>();
        List<String> unreadableEntries = new ArrayList<>();
        if (text == null) {
            return new Result(unitIds, unreadableEntries);
        }
        for (String rawEntry : text.split(",")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (!readEntry(entry, unitIds)) {
                unreadableEntries.add(entry);
            }
        }
        return new Result(unitIds, unreadableEntries);
    }

    /**
     * Adds the IDs one entry names.
     *
     * @param entry   a single trimmed entry, either one ID or a range
     * @param unitIds where the IDs are added
     *
     * @return {@code false} when the entry could not be read, in which case nothing was added
     */
    private static boolean readEntry(String entry, SortedSet<Integer> unitIds) {
        try {
            if (SINGLE_ID.matcher(entry).matches()) {
                unitIds.add(Integer.parseInt(entry));
                return true;
            }
            Matcher range = ID_RANGE.matcher(entry);
            if (!range.matches()) {
                return false;
            }
            int first = Integer.parseInt(range.group(1));
            int second = Integer.parseInt(range.group(2));
            int start = Math.min(first, second);
            int end = Math.max(first, second);
            if ((end - start) >= MAXIMUM_RANGE_SIZE) {
                return false;
            }
            for (int unitId = start; unitId <= end; unitId++) {
                unitIds.add(unitId);
            }
            return true;
        } catch (NumberFormatException exception) {
            // only reachable for a number too large for an int, which cannot be a unit ID
            return false;
        }
    }
}
