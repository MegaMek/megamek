/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.Coords;

/**
 * Sprite for a cursor. Just a hexagon outline in a specified color.
 */
public class CursorSprite extends Sprite {

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
        return !bv.getBoard().contains(hexLoc);
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
