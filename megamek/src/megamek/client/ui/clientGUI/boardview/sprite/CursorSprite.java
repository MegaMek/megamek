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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import megamek.client.ui.clientGUI.boardview.BoardTactical;
import megamek.client.ui.clientGUI.boardview.BoardTacticalGraphics;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * Sprite for a cursor. Just a hexagon outline in a specified color.
 */
public class CursorSprite extends Sprite implements TacticalSprite {

    @Override
    public BoardTactical.Playback playback() {
        return BoardTactical.Playback.LIVE;
    }

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
        Rectangle size = getBounds();
        BufferedImage rendered = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graph = rendered.createGraphics();
        try {
            UIUtil.setHighQualityRendering(graph);
            graph.scale(bv.getScale(), bv.getScale());
            graph.setColor(color);
            graph.drawPolygon(BoardView.getHexPoly());
        } finally {
            graph.dispose();
        }
        image = rendered;
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
        bounds = new Rectangle(bv.getHexSize());
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
    @Override
    public void drawTactical(Graphics2D graphics) {
        Graphics2D local = BoardTacticalGraphics.at(graphics, bv.getHexLocation(hexLoc));
        try {
            local.setColor(color);
            local.drawPolygon(BoardView.getHexPoly());
        } finally {
            local.dispose();
        }
    }

}
