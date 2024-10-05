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

import javax.swing.*;
import java.awt.*;

/**
 * This builder class is used to assign fonts and font styles to JComponents in the Swing GUI using FlatLaf's typography system. When
 * FlatLaf is not used as look and feel, this simply has no effect.
 */
public final class FlatLafStyleBuilder {

    private final JComponent component;
    private String fontName = "";
    private double size = 1;
    private boolean bold = false;

    /**
     * Creates a style builder for the given component. Add styles by chaining calls to the other methods. Apply the style using the
     * finalizer method {@link #set()}.
     */
    public FlatLafStyleBuilder(JComponent component) {
        this.component = component;
    }

    /**
     * Adds the given font (only the font name, not its style nor size) to the style builder.
     *
     * @param font The font to use
     */
    public FlatLafStyleBuilder font(Font font) {
        fontName = font.getFontName();
        return this;
    }

    /**
     * Adds the given font name to the style builder.
     *
     * @param fontName The font to use
     */
    public FlatLafStyleBuilder font(String fontName) {
        this.fontName = fontName;
        return this;
    }

    /**
     * Sets the style to use bold text.
     */
    public FlatLafStyleBuilder bold() {
        bold = true;
        return this;
    }

    /**
     * Adds the given relative size to the style builder. The default value is 1, meaning default font size. Values above 1 increase font
     * size with, for example, 1.2 resulting in a font size of 120% of the default font size. Size values of 0 or less are ignored.
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
     * Sets the JComponent of this style builder to use the given style. Note that when not using a FlatLaf look-and-feel, this method
     * simply has no effect.
     */
    public void set() {
        String styleText = "font:";
        if ((size != 1) && (size > 0)) {
            styleText += " " + (int) (100 * size) + "%";
        }
        if (bold) {
            styleText += " bold";
        }
        if (fontName != null && !fontName.isBlank()) {
            styleText += " \"" + fontName + "\"";
        }
        component.putClientProperty("FlatLaf.style", styleText);
    }
}
