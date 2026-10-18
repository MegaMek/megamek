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
package megamek.client.ui.clientGUI.boardview.sprite;

import java.awt.Font;
import java.awt.Graphics;

import megamek.client.ui.tileset.HexTileset;

/**
 * Shrinks the font of a text label drawn inside a hex so the whole label fits in the hex width. Movement step labels
 * (MP cost, TMM and rolls, announcements) can grow long, for example a WiGE descending ten hexes adds ten "+" marks,
 * and were cut off at the edge of the hex image.
 *
 * <p>The labels are drawn in board space, on an image the size of an unzoomed hex ({@link HexTileset#HEX_W}), which
 * is then scaled with the board zoom. The widths here are therefore board pixels, not screen pixels, and are not
 * GUI-scaled.</p>
 */
public final class HexLabelFitter {

    /** Board pixels kept clear between a label and the left or right edge of the hex. */
    public static final int EDGE_MARGIN = 4;

    /** The smallest font size a label is shrunk to; a label that still does not fit is drawn at this size. */
    public static final float MINIMUM_FONT_SIZE = 6f;

    private HexLabelFitter() {
    }

    /**
     * @return the width available to a label centred in the hex
     */
    public static int centredLabelWidth() {
        return HexTileset.HEX_W - (2 * EDGE_MARGIN);
    }

    /**
     * @param startX the x position, in board pixels within the hex, where a left-aligned label starts
     *
     * @return the width available to a left-aligned label that starts at the given x position
     */
    public static int labelWidthFrom(int startX) {
        return HexTileset.HEX_W - EDGE_MARGIN - startX;
    }

    /**
     * Returns the given font, or a smaller copy of it, so that the text is no wider than the maximum width. The font is
     * never made larger and never smaller than {@link #MINIMUM_FONT_SIZE}.
     *
     * @param graph    the graphics the label will be drawn with, used to measure the text
     * @param font     the font the label would normally use
     * @param text     the label text
     * @param maxWidth the widest the label may be, in board pixels
     *
     * @return the font to draw the label with
     */
    public static Font fitToWidth(Graphics graph, Font font, String text, int maxWidth) {
        Font fittedFont = font;
        int textWidth = graph.getFontMetrics(fittedFont).stringWidth(text);
        float fontSize = fittedFont.getSize2D();
        while ((textWidth > maxWidth) && (fontSize > MINIMUM_FONT_SIZE)) {
            // Text width grows about in line with font size; always shrink by at least one point so the loop ends
            float proportionalSize = fontSize * maxWidth / textWidth;
            fontSize = Math.max(MINIMUM_FONT_SIZE, Math.min(fontSize - 1, proportionalSize));
            fittedFont = font.deriveFont(fontSize);
            textWidth = graph.getFontMetrics(fittedFont).stringWidth(text);
        }
        return fittedFont;
    }
}
