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
package megamek.client.ui.settings;

import java.awt.Color;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import jakarta.annotation.Nullable;
import org.apache.commons.text.StringEscapeUtils;

/**
 * A resolved marker shown beside a setting and explained in its legend. A marker is either a Material Symbols glyph or,
 * when {@code text} is set, a short run of colored text such as a version number.
 *
 * @param codePoint   the Material Symbols code point, or {@link #NO_SYMBOL} for a text marker
 * @param color       the marker color, or {@code null} to inherit the component foreground
 * @param description localized text explaining the marker
 * @param text        the text shown instead of a glyph, or {@code null} for a glyph marker
 */
public record SettingsBadge(int codePoint, @Nullable Color color, String description, @Nullable String text) {
    /** The code point carried by a text marker, which has no glyph. */
    public static final int NO_SYMBOL = -1;

    public SettingsBadge {
        Objects.requireNonNull(description);
        if (text == null) {
            if (!Character.isValidCodePoint(codePoint)) {
                throw new IllegalArgumentException("Invalid settings badge code point: " + codePoint);
            }
        } else if (text.isBlank()) {
            throw new IllegalArgumentException("Settings badge text must not be blank");
        } else if (codePoint != NO_SYMBOL) {
            throw new IllegalArgumentException("A text settings badge cannot also carry a code point: " + codePoint);
        }
    }

    /**
     * Creates a glyph marker.
     *
     * @param codePoint   the Material Symbols code point
     * @param color       the marker color, or {@code null} to inherit the component foreground
     * @param description localized text explaining the marker
     */
    public SettingsBadge(int codePoint, @Nullable Color color, String description) {
        this(codePoint, color, description, null);
    }

    /**
     * Creates a text marker, such as a version number, shown in place of a glyph.
     *
     * @param text        the text to show
     * @param color       the text color, or {@code null} to inherit the component foreground
     * @param description localized text explaining the marker
     *
     * @return the text marker
     */
    public static SettingsBadge ofText(String text, @Nullable Color color, String description) {
        return new SettingsBadge(NO_SYMBOL, color, description, Objects.requireNonNull(text));
    }

    /** @return {@code true} if this marker shows text rather than a glyph */
    public boolean isText() {
        return text != null;
    }

    /** @return this badge as an HTML fragment suitable for Swing label text */
    public String toHtml() {
        String colorAttribute = color == null
              ? ""
              : String.format(Locale.ROOT, " color=\"#%06x\"", color.getRGB() & 0xFFFFFF);
        if (isText()) {
            return " <font" + colorAttribute + '>' + StringEscapeUtils.escapeHtml4(text) + "</font>";
        }
        return " <font face=\"Material Symbols Rounded\"" + colorAttribute + '>'
              + Character.toString(codePoint) + "</font>";
    }

    /**
     * @param badges badges to render in iteration order
     *
     * @return concatenated HTML fragments, or an empty string when no badges are supplied
     */
    public static String formatHtml(@Nullable Collection<SettingsBadge> badges) {
        if (badges == null || badges.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        for (SettingsBadge badge : badges) {
            html.append(Objects.requireNonNull(badge).toHtml());
        }
        return html.toString();
    }
}
