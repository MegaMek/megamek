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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.MMConstants;
import megamek.common.Entity;
import megamek.common.util.ImageUtil;

class GhostEntitySprite extends Sprite {

    private Entity entity;

    private Rectangle modelRect;

    public GhostEntitySprite(BoardView boardView1, final Entity entity) {
        super(boardView1);
        this.entity = entity;

        String shortName = entity.getShortName();
        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
        modelRect = new Rectangle(47, 55, bv.getPanel().getFontMetrics(font).stringWidth(
                shortName) + 1, bv.getPanel().getFontMetrics(font).getAscent());
        Rectangle tempBounds = new Rectangle(bv.hex_size).union(modelRect);
        tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));

        bounds = tempBounds;
        image = null;
    }

    /**
     * Creates the sprite for this entity.
     */
    @Override
    public void prepare() {
        image = ImageUtil.createAcceleratedImage(bounds.width, bounds.height);
        Graphics graph = image.getGraphics();
        graph.drawImage(bv.tileManager.imageFor(entity), 0, 0, this);
        image = bv.getScaledImage(image, false);
        graph.dispose();
    }

    @Override
    public Rectangle getBounds() {
        Rectangle tempBounds = new Rectangle(bv.hex_size).union(modelRect);
        tempBounds.setLocation(bv.getHexLocation(entity.getPosition()));
        bounds = tempBounds;

        return bounds;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        drawOnto(g, x, y, observer, true);
    }

}
