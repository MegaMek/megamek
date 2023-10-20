/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.displayWrappers;

import megamek.client.ui.swing.util.FontHandler;
import megamek.common.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FontDisplay is a display wrapper around a Font, primarily to be used in ComboBoxes. This is
 * largely for performance reasons, as it doesn't require recalculating the font and/ or proper
 * name each time.
 */
public class FontDisplay {
    //region Variable Declarations
    private final Font font;
    private final String displayName;
    //endregion Variable Declarations

    //region Constructors
    public FontDisplay(final String familyName) {
        this(Font.decode(familyName).deriveFont(Font.PLAIN, 12f), familyName);
    }

    public FontDisplay(final Font font, final String displayName) {
        this.font = font;
        this.displayName = displayName;
    }
    //endregion Constructors

    //region Getters/Setters
    public Font getFont() {
        return font;
    }
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
            return getFont().equals(((FontDisplay) other).getFont());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return font.hashCode();
    }
}
