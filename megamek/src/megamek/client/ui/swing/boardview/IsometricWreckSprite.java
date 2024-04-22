/*
 * MegaMek - Copyright (C) 2020 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.boardview;

import megamek.MMConstants;
import megamek.common.Entity;

import java.awt.*;
import java.awt.image.ImageObserver;

/**
 * Sprite for an wreck. Consists of an image, drawn from the Tile Manager
 * and an identification label.
 */
class IsometricWreckSprite extends AbstractWreckSprite {

    /**
     * Isometric wreck sprite constructor, calculates boundaries
     */
    public IsometricWreckSprite(BoardView boardView1, final Entity entity, int secondaryPos) {
        super(boardView1);
        this.entity = entity;
        this.secondaryPos = secondaryPos;

        String shortName = entity.getShortName();

        Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
        modelRect = new Rectangle(47, 55, bv.getPanel().getFontMetrics(font).stringWidth(
                shortName) + 1, bv.getPanel().getFontMetrics(font).getAscent());

        image = null;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer, boolean makeTranslucent) {
        if (isReady()) {
            Graphics2D g2 = (Graphics2D) g;
            if (makeTranslucent) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.drawImage(image, x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(image, x, y, observer);
            }
        } else {
            prepare();
        }
    }

    public Entity getEntity() {
        return entity;
    }
}
