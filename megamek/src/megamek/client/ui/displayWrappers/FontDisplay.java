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

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return Stream.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .map(familyName -> new FontDisplay(Font.decode(familyName).deriveFont(Font.PLAIN, 16f), familyName))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return displayName;
    }
}
