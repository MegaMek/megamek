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

import static megamek.client.ui.swing.boardview.HexDrawUtilities.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;

/**
 * Sprite for displaying information about where a unit can move to.
 */
class MovementEnvelopeSprite extends HexSprite {

    // control values
    private static final int BORDER_THICKNESS = 10;
    private static final int BORDER_OPACITY = 60;
    private static final float LINE_THICKNESS = 2;
    
    // sprite settings
    protected final Color drawColor;
    protected final int borders;

    public MovementEnvelopeSprite(BoardView boardView1, Color c, Coords l, int borders) {
        super(boardView1, l);
        drawColor = c;
        this.borders = borders;
    }
    
    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();
        
        // create image for buffer
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        graph.setStroke(new BasicStroke(LINE_THICKNESS, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[] { 5f, 3f }, 0f));

        // cycle through directions
        for (int i = 0; i < 6; i++) {
            if ((borders & (1 << i)) > 0) {
                // 1) thick transparent border
                int cut = ((borders & (1 << ((i + 1) % 6))) == 0) ? CUT_RBORDER : CUT_RINSIDE;
                cut |= ((borders & (1 << ((i + 5) % 6))) == 0) ? CUT_LBORDER : CUT_LINSIDE;

                graph.setColor(new Color(drawColor.getRed(), drawColor.getGreen(),
                        drawColor.getBlue(), BORDER_OPACITY));
                graph.fill(getHexBorderArea(i, cut, BORDER_THICKNESS));

                // 2) thin dashed line border
                graph.setColor(drawColor);
                graph.draw(getHexBorderLine(i, cut, LINE_THICKNESS / 2));
            }
        }
        graph.dispose();
    }
    
}