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
 * The Short/Medium/Long range of a Battlefield Support Asset, in hexes. The Asset card shows this as
 * {@code short/medium/long} (for example {@code 3/6/9}).
 * <p>
 * Assets whose single attack is not a standard ranged attack - for example Artillery or Arrow assets, or Assets with
 * no ranged attack at all - use {@link #KEYWORD} (all three values set to {@code -1}). In that case the display form is
 * a keyword label (such as {@code Artillery} or {@code Arrow}) derived from the Asset's Specials, or an em dash when
 * no keyword applies.
 *
 * @param shortRange  the short-range band in hexes, or {@code -1} for a keyword range
 * @param mediumRange the medium-range band in hexes, or {@code -1} for a keyword range
 * @param longRange   the long-range band in hexes, or {@code -1} for a keyword range
 */
public record BFSRange(int shortRange, int mediumRange, int longRange) implements Serializable {

    /** The em dash shown on the card for a keyword range with no applicable label. */
    private static final String NA_DISPLAY = "\u2014";

    /** A non-standard range represented by a keyword (Artillery, Arrow, or none). */
    public static final BFSRange KEYWORD = new BFSRange(-1, -1, -1);

    /** @return true if this is a keyword range (all three bands negative) rather than a numeric S/M/L range */
    public boolean isKeyword() {
        return shortRange < 0 && mediumRange < 0 && longRange < 0;
    }

    /**
     * Returns the card display form of this range. For a numeric range this is {@code short/medium/long}. For a keyword
     * range (see {@link #isKeyword()}) the given {@code keywordLabel} is returned, or an em dash when it is
     * {@code null}/blank.
     *
     * @param keywordLabel the label to show for a keyword range (for example {@code Artillery})
     *
     * @return the display form
     */
    public String displayString(@Nullable String keywordLabel) {
        if (isKeyword()) {
            return (keywordLabel == null || keywordLabel.isBlank()) ? NA_DISPLAY : keywordLabel;
        }
        return shortRange + "/" + mediumRange + "/" + longRange;
    }

    /** @return the numeric display form, or an em dash for a keyword range with no label */
    public String displayString() {
        return displayString(null);
    }
}
