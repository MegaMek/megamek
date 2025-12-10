/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import javax.swing.JPanel;
import javax.swing.Scrollable;

import megamek.client.ui.tileset.HexTileset;
import megamek.common.board.Coords;

/**
 * This class represents the specialized JPanel that paints the contents of the BoardView. For its drawing, it refers
 * back to the BoardView.
 */
public class BoardViewPanel extends JPanel implements Scrollable {

    private final BoardView boardView;

    public BoardViewPanel(BoardView boardView) {
        super(true);
        this.boardView = boardView;
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        Coords hexCoords = boardView.getCoordsAt(event.getPoint());
        Point point = boardView.getCentreHexLocation(hexCoords);
        // add board padding
        point.translate(HexTileset.HEX_W, HexTileset.HEX_H);
        // move to the right of the current hex
        point.translate((int) (HexTileset.HEX_W * boardView.scale * 0.75),
              (int) ((-HexTileset.HEX_H / 4.0f) * boardView.scale));
        return new Point(point.x, point.y);
    }

    @Override
    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
        // If FRAMEBITS is set, then new frame from a multi-frame image is ready
        // This indicates an animated image, which shouldn't be cached
        if ((flags & ImageObserver.FRAMEBITS) != 0) {
            boardView.getAnimatedImages().add(img.hashCode());
        }
        return super.imageUpdate(img, flags, x, y, w, h);
    }

    @Override
    public synchronized void paintComponent(Graphics g) {
        boardView.draw(g);
    }

    @Override
    public Dimension getPreferredSize() {
        // If the board is small, we want the preferred size to fill the whole ScrollPane viewport,
        // for purposes of drawing the tiled background icon. However, we also need the scrollable
        // client to be as big as the board plus the pad size.
        return new Dimension(
              Math.max(boardView.getBoardSize().width + (2 * HexTileset.HEX_W), boardView.getComponent().getWidth()),
              Math.max(boardView.getBoardSize().height + (2 * HexTileset.HEX_W), boardView.getComponent().getHeight()));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
        return boardView.getScrollableBlockIncrement(arg0, arg1, arg2);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
        return boardView.getScrollableUnitIncrement(arg0, arg1, arg2);
    }
}
