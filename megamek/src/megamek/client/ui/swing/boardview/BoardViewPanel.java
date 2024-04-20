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

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;

/**
 * This class represents the specialized JPanel that paints the contents of the BoardView. For its
 * drawing, it refers back to the BoardView.
 */
public class BoardViewPanel extends JPanel implements Scrollable {

    private final BoardView boardView;
    private Dimension preferredSize = new Dimension(0, 0);

    public BoardViewPanel(BoardView boardView) {
        super(true);
        this.boardView = boardView;
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
    public void setPreferredSize(Dimension d) {
        super.setPreferredSize(d);
        preferredSize = new Dimension(d);
    }

    @Override
    public Dimension getPreferredSize() {
        // If the board is small, we want the preferred size to fill the whole ScrollPane viewport,
        // for purposes of drawing the tiled background icon. However, we also need the scrollable
        // client to be as big as the board plus the pad size.
        return new Dimension(
                Math.max(boardView.getBoardSize().width + (2 * BoardView.HEX_W), preferredSize.width),
                Math.max(boardView.getBoardSize().height + (2 * BoardView.HEX_W), preferredSize.height));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
        return 500;
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
        if (arg1 == SwingConstants.VERTICAL) {
            return (int) ((boardView.scale * BoardView.HEX_H) / 2.0);
        }
        return (int) ((boardView.scale * BoardView.HEX_W) / 2.0);
    }
}