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
package megamek.common.battlefieldSupport;

import java.io.Serializable;
import java.util.Optional;

import megamek.common.annotations.Nullable;

/**
 * A single Special ability on a Battlefield Support Asset, stored as an abbreviated code plus an optional parameter
 * value. The value is a String so it can hold either a number (for Specials like {@code IF2}, {@code APC1},
 * {@code ECM6}) or a type token (for {@code Artillery (LT)}).
 * <p>
 * On the Asset card, a numeric value is appended directly to the code (for example {@code IF2}) while a non-numeric
 * value is shown in parentheses (for example {@code Artillery (LT)}); Specials without a value are shown as-is (for
 * example {@code TAG}, {@code No Turret}).
 * <p>
 * Any code may be stored here, including ones this build does not implement: unknown Specials are preserved verbatim
 * and printed on the card, but ignored during gameplay. Recognized codes can be resolved to a {@link BFSSpecialType}
 * via {@link #knownType()}.
 *
 * @param code  the Special's abbreviation as authored (never {@code null}; blank is tolerated but not expected)
 * @param value the Special's parameter, or {@code null} if it has none
 */
public record BFSSpecial(String code, @Nullable String value) implements Serializable {

    public BFSSpecial {
        code = (code == null) ? "" : code.strip();
        value = (value == null) ? null : value.strip();
    }

    /**
     * @param code the Special's abbreviation
     *
     * @return a Special with no value
     */
    public static BFSSpecial of(String code) {
        return new BFSSpecial(code, null);
    }

    /**
     * @param code  the Special's abbreviation
     * @param value the Special's parameter
     *
     * @return a Special with the given code and value
     */
    public static BFSSpecial of(String code, @Nullable String value) {
        return new BFSSpecial(code, value);
    }

    /**
     * @param code  the Special's abbreviation
     * @param value the Special's integer parameter
     *
     * @return a Special with the given code and value
     */
    public static BFSSpecial of(String code, int value) {
        return new BFSSpecial(code, Integer.toString(value));
    }

    /** @return true if this Special has a parameter value */
    public boolean hasValue() {
        return value != null && !value.isEmpty();
    }

    /** @return true if this Special's value is present and purely numeric */
    public boolean hasNumericValue() {
        return hasValue() && value.chars().allMatch(Character::isDigit);
    }

    /** @return the Special's value parsed as an integer, or empty if it is absent or non-numeric */
    public Optional<Integer> intValue() {
        return hasNumericValue() ? Optional.of(Integer.valueOf(value)) : Optional.empty();
    }

    /** @return the known-Special registry entry for this code, if it is recognized */
    public Optional<BFSSpecialType> knownType() {
        return BFSSpecialType.forCode(code);
    }

    /** @return true if this Special's code is recognized in the {@link BFSSpecialType} registry */
    public boolean isKnown() {
        return knownType().isPresent();
    }

    /**
     * @return the card display form: the code with a numeric value appended (for example {@code IF2}), a non-numeric
     *       value in parentheses (for example {@code Artillery (LT)}), or just the code when there is no value
     */
    public String displayString() {
        if (!hasValue()) {
            return code;
        }
        return hasNumericValue() ? code + value : code + " (" + value + ")";
    }

    /**
     * Parses a card display token into a {@link BFSSpecial}. Handles three forms: a parenthesized value (for example
     * {@code Artillery (LT)} yields code {@code Artillery}, value {@code LT}); a trailing run of digits (for example
     * {@code IF2} yields code {@code IF}, value {@code 2}); or no value (for example {@code No Turret}, {@code TAG}).
     *
     * @param token the token to parse
     *
     * @return the parsed Special, or {@code null} if the token is {@code null} or blank
     */
    public static @Nullable BFSSpecial parse(@Nullable String token) {
        if (token == null) {
            return null;
        }
        String stripped = token.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        int openParen = stripped.indexOf('(');
        if (openParen >= 0 && stripped.endsWith(")")) {
            String code = stripped.substring(0, openParen).strip();
            String value = stripped.substring(openParen + 1, stripped.length() - 1).strip();
            return new BFSSpecial(code, value.isEmpty() ? null : value);
        }
        int splitIndex = stripped.length();
        while (splitIndex > 0 && Character.isDigit(stripped.charAt(splitIndex - 1))) {
            splitIndex--;
        }
        if (splitIndex > 0 && splitIndex < stripped.length()) {
            return new BFSSpecial(stripped.substring(0, splitIndex).strip(), stripped.substring(splitIndex));
        }
        return new BFSSpecial(stripped, null);
    }
}
