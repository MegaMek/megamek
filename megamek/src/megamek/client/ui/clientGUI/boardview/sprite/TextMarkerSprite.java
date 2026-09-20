/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

public class TextMarkerSprite extends HexSprite {

    private final String spriteText;
    private final Color textColor;
    private final int rangeBracket;

    public TextMarkerSprite(BoardView boardView1, Coords loc, String text, Color color) {
        this(boardView1, loc, text, color, -1);
    }

    public TextMarkerSprite(BoardView boardView, Coords loc, int rangeBracket) {
        this(boardView, loc, FieldOfFireSprite.getRangeText(rangeBracket),
              FieldOfFireSprite.getFieldOfFireColor(rangeBracket), rangeBracket);
    }

    private TextMarkerSprite(BoardView boardView1, Coords loc, String text, Color color, int rangeBracket) {
        super(boardView1, loc);
        spriteText = text;
        textColor = color;
        this.rangeBracket = rangeBracket;
    }

    /** Identifies range labels so the GPU board can choose flat markers or contour text. */
    public boolean isWeaponRange() {
        return rangeBracket >= 0;
    }

    public int getRangeBracket() {
        return rangeBracket;
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();

        // create image for buffer
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        try {
            drawMarker(graph, spriteText, textColor, bounds.width, bounds.height, bv.getScale());
        } finally {
            graph.dispose();
        }
    }

    /** Shared flat artwork for the classic sprite and the GPU's camera-facing range labels. */
    public static void drawMarker(Graphics2D graph, String text, Color color, int width, int height, float scale) {
        UIUtil.setHighQualityRendering(graph);
        Font textFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 1000);
        FontMetrics fm = graph.getFontMetrics(textFont);
        Rectangle2D rect = fm.getStringBounds(text, graph);
        new StringDrawer(text).color(color).center().at(new Point(width / 2, height / 2))
              .outline(Color.BLACK, scale).font(getFont(rect, width, height)).draw(graph);
    }

    private static Font getFont(Rectangle2D rect, int width, int height) {
        float factor = 1;
        if (rect.getHeight() > height) {
            factor = height / (float) rect.getHeight();
        }

        if ((rect.getWidth() * factor) > width) {
            factor = Math.min(factor, width / (float) rect.getWidth());
        }
        // make smaller to actually fit the hex shape
        factor = factor * 0.7f;
        return new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, (int) (factor * 1000));
    }
}
