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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;

/**
 * Sprite for a cursor. Just a hexagon outline in a specified color.
 */
class CursorSprite extends Sprite {

    private Color color;

    private Coords hexLoc;

    public CursorSprite(BoardView boardView1, final Color color) {
        super(boardView1);
        this.color = color;
        bounds = new Rectangle(BoardView.getHexPoly().getBounds().width + 1,
                BoardView.getHexPoly().getBounds().height + 1);
        image = null;

        // start offscreen
        setOffScreen();
    }

    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage = new BufferedImage(bounds.width, bounds.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics graph = tempImage.getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // fill with key color
        graph.setColor(new Color(0, 0, 0, 0));
        graph.fillRect(0, 0, bounds.width, bounds.height);
        // draw attack poly
        graph.setColor(color);
        graph.drawPolygon(BoardView.getHexPoly());

        // create final image
        image = bv.getScaledImage(bv.getPanel().createImage(tempImage.getSource()), false);

        graph.dispose();
        tempImage.flush();
    }

    public void setOffScreen() {
        bounds.setLocation(-100, -100);
        hexLoc = new Coords(-2, -2);
    }

    public boolean isOffScreen() {
        return !bv.game.getBoard().contains(hexLoc);
    }

    public void setHexLocation(Coords hexLoc) {
        this.hexLoc = hexLoc;
        bounds.setLocation(bv.getHexLocation(hexLoc));
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(BoardView.getHexPoly().getBounds().width + 1,
                BoardView.getHexPoly().getBounds().height + 1);
        bounds.setLocation(bv.getHexLocation(hexLoc));

        return bounds;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public boolean isHidden() {
        return hidden || isOffScreen();
    }
}
