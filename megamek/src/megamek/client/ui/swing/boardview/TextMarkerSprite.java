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
package megamek.client.ui.swing.boardview;

import megamek.MMConstants;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TextMarkerSprite extends HexSprite {

    private final String spriteText;
    private final Color spriteColor;
    private final StringDrawer textDrawer;

    public TextMarkerSprite(BoardView boardView1, Coords loc, String text, Color color) {
        super(boardView1, loc);
        spriteText = text;
        spriteColor = color;
        textDrawer = new StringDrawer(spriteText).color(spriteColor).center();
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();

        // create image for buffer
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // get a big font and test to see which font size will fit the hex shape
        Font textFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 1000);
        graph.setFont(textFont);
        FontMetrics fm = graph.getFontMetrics(graph.getFont());
        Rectangle2D rect = fm.getStringBounds(spriteText, graph);
        Point pos = new Point((int) (bounds.getWidth() / 2), (int) (bounds.getHeight() / 2));
        textDrawer.at(pos).outline(Color.BLACK, bv.scale).font(getFont(rect)).draw(graph);
    }

    private Font getFont(Rectangle2D rect) {
        float factor = 1;
        if (rect.getHeight() > bounds.getHeight()) {
            factor = (float) bounds.getHeight() / (float) rect.getHeight();
        }

        if ((rect.getWidth() * factor) > bounds.getWidth()) {
            factor = Math.min(factor, ((float) bounds.getWidth() / (float) rect.getWidth()));
        }
        // make smaller to actually fit the hex shape
        factor = factor * 0.7f;
        return new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, (int) (factor * 1000));
    }
}
