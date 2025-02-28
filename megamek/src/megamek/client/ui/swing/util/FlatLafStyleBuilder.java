/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.util;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * This builder class is used to assign fonts and font styles to JComponents in the Swing GUI using FlatLaf's typography system. When
 * FlatLaf is not used as look and feel, this simply has no effect.
 */
public final class FlatLafStyleBuilder {

    private String fontName = "";
    private double size = 1;
    private boolean bold = false;

    /**
     * Creates an empty style builder. Add styles by chaining calls to the other methods. Apply the style using the finalizer method
     * {@link #apply(JComponent)}.
     */
    public FlatLafStyleBuilder() {
    }

    /**
     * Creates a style builder, initializing it with the given font. Add styles by chaining calls to the other methods. Apply the style
     * using the finalizer method {@link #apply(JComponent)}.
     */
    public FlatLafStyleBuilder(Font font) {
        font(font);
    }

    /**
     * Adds the given font (only the font name, not its style nor size) to the style builder. Note that multiple calls to this method simply
     * overwrite previous values.
     *
     * @param font The font to use
     */
    public FlatLafStyleBuilder font(@Nullable Font font) {
        return font((font == null) ? "" : font.getFontName());
    }

    /**
     * Adds the given font name to the style builder. Note that multiple calls to this method simply overwrite previous values.
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
     * Adds the given relative size to the style builder. The default value is 1, meaning default font size. Values above 1 increase font
     * size with, for example, 1.2 resulting in a font size of 120% of the default font size. Size values of 0 or less are ignored. Not that
     * the resulting font size is affected by GUI scaling automatically, there is no need to scale using this method. Note that multiple
     * calls to this method simply overwrite previous values.
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
     * Sets the given JComponent use the given style. Note that when not using a FlatLaf look-and-feel, this method simply has no effect.
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
     * @param boldText flag indicating whether the text should be bold
     * @param size the relative font size to use
     */
    public static void setFontScaling(JComponent component, boolean boldText, double size) {
        setFontScaling(component, "MekHQ", boldText, size);
    }

    /**
     * Sets the font scaling for a given Swing component with a custom font.
     *
     * @param component the Swing component for which to set the font scaling
     * @param font the font to apply to the component
     * @param boldText flag indicating whether the text should be bold
     * @param size the relative font size to use
     */
    public static void setFontScaling(JComponent component, String font, boolean boldText, double size) {
        if (boldText) {
            new FlatLafStyleBuilder().font(font).bold().size(size).apply(component);
        } else {
            new FlatLafStyleBuilder().font(font).size(size).apply(component);
        }
    }
}
