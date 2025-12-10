/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.displayWrappers;

import java.awt.Font;
import java.util.List;
import java.util.stream.Collectors;

import megamek.client.ui.util.FontHandler;
import megamek.common.annotations.Nullable;

/**
 * FontDisplay is a display wrapper around a Font, primarily to be used in ComboBoxes. This is largely for performance
 * reasons, as it doesn't require recalculating the font and/ or proper name each time.
 *
 * @param font region Variable Declarations
 */
public record FontDisplay(Font font, String displayName) {
    //endregion Variable Declarations

    //region Constructors
    public FontDisplay(final String familyName) {
        this(Font.decode(familyName).deriveFont(Font.PLAIN, 12f), familyName);
    }

    //endregion Constructors
    //endregion Getters/Setters

    public static List<FontDisplay> getSortedFontDisplays() {
        return FontHandler.getAvailableFonts().stream().map(FontDisplay::new).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof FontDisplay) {
            return font().equals(((FontDisplay) other).font());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return font.hashCode();
    }
}
