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
package megamek.common.orders;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import megamek.common.annotations.Nullable;

/**
 * The names of phase lines. A phase line is a waypoint marked as one: every lance with a waypoint on the same phase
 * line holds there until all of them have reached it, then they all move on together, so lances of different speeds
 * arrive in step (HammerGS, 2026-10-02).
 *
 * <p>A new phase line takes the next free name of the ICAO spelling alphabet - Alfa, Bravo, Charlie - or a name the
 * player types. It is always shown and called with "PL" in front ("PL Bravo"), so it never reads like a lance's
 * callsign (Bravo Lance) or a nav point (Nav Point Beta).</p>
 */
public final class PhaseLine {

    /** The ICAO spelling alphabet, the order new phase lines are named in. */
    public static final List<String> ICAO_NAMES = List.of("Alfa", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot",
          "Golf", "Hotel", "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo",
          "Sierra", "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu");

    /** The longest name a phase line may have. */
    public static final int MAXIMUM_NAME_LENGTH = 24;

    /** What a phase line's name stands after in route text, e.g. {@code PL:Alfa}. */
    static final String CODE_PREFIX = "PL:";

    private PhaseLine() {
    }

    /**
     * @param namesInUse the phase lines already named
     *
     * @return the first ICAO name not in use, ignoring case; after Zulu, Zulu and a number
     */
    public static String nextName(Collection<String> namesInUse) {
        for (String name : ICAO_NAMES) {
            if (!containsIgnoringCase(namesInUse, name)) {
                return name;
            }
        }
        int number = 2;
        while (containsIgnoringCase(namesInUse, "Zulu " + number)) {
            number++;
        }
        return "Zulu " + number;
    }

    private static boolean containsIgnoringCase(Collection<String> names, String name) {
        for (String candidate : names) {
            if (candidate.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cleans a typed name: letters, digits, spaces and hyphens only, single spaces, at most
     * {@link #MAXIMUM_NAME_LENGTH} characters.
     *
     * @param typed what the player typed
     *
     * @return the name, or {@code null} when nothing usable is left
     */
    public static @Nullable String cleanName(@Nullable String typed) {
        if (typed == null) {
            return null;
        }
        StringBuilder cleaned = new StringBuilder();
        for (char character : typed.trim().toCharArray()) {
            if (Character.isLetterOrDigit(character) || (character == '-')) {
                cleaned.append(character);
            } else if (Character.isWhitespace(character) || (character == '_')) {
                if ((cleaned.length() > 0) && (cleaned.charAt(cleaned.length() - 1) != ' ')) {
                    cleaned.append(' ');
                }
            }
        }
        String name = cleaned.toString().trim();
        if (name.length() > MAXIMUM_NAME_LENGTH) {
            name = name.substring(0, MAXIMUM_NAME_LENGTH).trim();
        }
        return name.isEmpty() ? null : name;
    }

    /**
     * @return the name as it is shown and called, e.g. {@code PL Bravo}
     */
    public static String display(String name) {
        return "PL " + name;
    }

    /**
     * @return the name as route text writes it, e.g. {@code PL:Nine_Mile_Ridge} or {@code PL:X.ray}: route text splits
     *       waypoints on hyphens and arguments on spaces, so a hyphen is written as a dot and a space as an underscore
     */
    static String toCode(String name) {
        return CODE_PREFIX + name.replace(' ', '_').replace('-', '.');
    }

    /**
     * @return {@code true} if a part of route text names a phase line
     */
    static boolean isCode(String segment) {
        return segment.trim().toUpperCase(Locale.ROOT).startsWith(CODE_PREFIX) && (segment.trim().length()
              > CODE_PREFIX.length());
    }

    /**
     * @return the phase line's name from its route text, cleaned
     *
     * @throws IllegalArgumentException when nothing usable follows {@code PL:}
     */
    static String fromCode(String segment) {
        String name = cleanName(segment.trim().substring(CODE_PREFIX.length()).replace('.', '-'));
        if (name == null) {
            throw new IllegalArgumentException("Not a phase line name: " + segment);
        }
        return name;
    }

    /**
     * @return {@code true} if the two names are the same phase line, ignoring case
     */
    public static boolean isSame(@Nullable String first, @Nullable String second) {
        return (first != null) && first.equalsIgnoreCase(second);
    }
}
