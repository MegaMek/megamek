/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.util;

import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.common.annotations.Nullable;

/**
 * This builder class is used to assign fonts and font styles to JComponents in the Swing GUI using FlatLaf's typography
 * system. When FlatLaf is not used as look and feel, this simply has no effect.
 */
public final class FlatLafStyleBuilder {

    private String fontName = "";
    private double size = 1;
    private boolean bold = false;

    /**
     * Creates an empty style builder. Add styles by chaining calls to the other methods. Apply the style using the
     * finalizer method {@link #apply(JComponent)}.
     */
    public FlatLafStyleBuilder() {
    }

    /**
     * Creates a style builder, initializing it with the given font. Add styles by chaining calls to the other methods.
     * Apply the style using the finalizer method {@link #apply(JComponent)}.
     */
    public FlatLafStyleBuilder(Font font) {
        font(font);
    }

    /**
     * Adds the given font (only the font name, not its style nor size) to the style builder. Note that multiple calls
     * to this method simply overwrite previous values.
     *
     * @param font The font to use
     */
    public FlatLafStyleBuilder font(@Nullable Font font) {
        return font((font == null) ? "" : font.getFontName());
    }

    /**
     * Adds the given font name to the style builder. Note that multiple calls to this method simply overwrite previous
     * values.
     *
     * @param fontName The font to use
     */
    public FlatLafStyleBuilder font(@Nullable String fontName) {
        this.fontName = Objects.requireNonNullElse(fontName, "");
        return this;
    }

    /**
     * Sets the style to use bold text. Note that multiple calls to this method have no further effect.
     */
    public FlatLafStyleBuilder bold() {
        bold = true;
        return this;
    }

    /**
     * Adds the given relative size to the style builder. The default value is 1, meaning default font size. Values
     * above 1 increase font size with, for example, 1.2 resulting in a font size of 120% of the default font size. Size
     * values of 0 or less are ignored. Not that the resulting font size is affected by GUI scaling automatically, there
     * is no need to scale using this method. Note that multiple calls to this method simply overwrite previous values.
     *
     * @param size The relative font size to use
     */
    public FlatLafStyleBuilder size(double size) {
        if (size > 0) {
            this.size = size;
        }
        return this;
    }

    /**
     * Sets the given JComponent use the given style. Note that when not using a FlatLaf look-and-feel, this method
     * simply has no effect.
     */
    public void apply(JComponent component) {
        String styleText = "font:";
        if ((size != 1) && (size > 0)) {
            styleText += " " + (int) (100 * size) + '%';
        }
        if (bold) {
            styleText += " bold";
        }
        if ((fontName != null) && !fontName.isBlank()) {
            styleText += " \"" + fontName + '"';
        }
        component.putClientProperty(FlatClientProperties.STYLE, styleText);
    }

    /**
     * Sets the font scaling for a given Swing component with a default font.
     *
     * @param component the Swing component for which to set the font scaling
     * @param boldText  flag indicating whether the text should be bold
     * @param size      the relative font size to use
     */
    public static void setFontScaling(JComponent component, boolean boldText, double size) {
        setFontScaling(component, "MekHQ", boldText, size);
    }

    /**
     * Sets the font scaling for a given Swing component with a custom font.
     *
     * @param component the Swing component for which to set the font scaling
     * @param font      the font to apply to the component
     * @param boldText  flag indicating whether the text should be bold
     * @param size      the relative font size to use
     */
    public static void setFontScaling(JComponent component, String font, boolean boldText, double size) {
        if (boldText) {
            new FlatLafStyleBuilder().font(font).bold().size(size).apply(component);
        } else {
            new FlatLafStyleBuilder().font(font).size(size).apply(component);
        }
    }
}
