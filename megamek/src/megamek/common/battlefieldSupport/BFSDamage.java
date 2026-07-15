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

import megamek.common.annotations.Nullable;

/**
 * The damage of a Battlefield Support Asset, stored as a tuple of the damage applied per grouping ({@code perHit}) and
 * the number of groupings ({@code hits}). The Asset card shows this as {@code perHit x hits} (for example {@code 5x4});
 * a single value such as {@code 5} is {@code 5x1}, and a blank ({@code -}) is {@code 0x0}.
 *
 * @param perHit the damage value applied to each rolled location
 * @param hits   the number of separate damage groupings
 */
public record BFSDamage(int perHit, int hits) implements Serializable {

    /** The em dash shown on the card for an Asset with no attack. */
    private static final String NA_DISPLAY = "\u2014";

    /** An Asset with no attack damage (displayed as an em dash). */
    public static final BFSDamage NONE = new BFSDamage(0, 0);

    /** @return true if this Asset deals any damage (both {@code perHit} and {@code hits} are positive) */
    public boolean hasDamage() {
        return perHit > 0 && hits > 0;
    }

    /** @return the total damage dealt across all groupings ({@code perHit * hits}) */
    public int total() {
        return hasDamage() ? perHit * hits : 0;
    }

    /**
     * @return the card display form: an em dash for no damage, {@code perHit} for a single grouping, or
     *       {@code perHit x hits} otherwise
     */
    public String displayString() {
        if (!hasDamage()) {
            return NA_DISPLAY;
        }
        if (hits == 1) {
            return Integer.toString(perHit);
        }
        return perHit + "x" + hits;
    }

    /**
     * Parses a card display form such as {@code 5x4}, {@code 5}, {@code -} or an em dash into a {@link BFSDamage}.
     * Whitespace is ignored and the {@code x} separator is case-insensitive. A {@code null}, blank, {@code -} or em
     * dash value yields {@link #NONE}.
     *
     * @param text the card display form to parse
     *
     * @return the parsed damage, never {@code null}
     *
     * @throws NumberFormatException if a non-blank value cannot be parsed
     */
    public static BFSDamage parse(@Nullable String text) {
        if (text == null) {
            return NONE;
        }
        String stripped = text.strip();
        if (stripped.isEmpty() || stripped.equals("-") || stripped.equals(NA_DISPLAY)) {
            return NONE;
        }
        String[] parts = stripped.toLowerCase().split("x");
        if (parts.length == 1) {
            return new BFSDamage(Integer.parseInt(parts[0].strip()), 1);
        }
        return new BFSDamage(Integer.parseInt(parts[0].strip()), Integer.parseInt(parts[1].strip()));
    }
}
