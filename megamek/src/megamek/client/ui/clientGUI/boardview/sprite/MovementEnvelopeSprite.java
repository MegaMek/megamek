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

import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.CUT_LEFT_BORDER;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.CUT_LEFT_INSIDE;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.CUT_RIGHT_BORDER;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.CUT_RIGHT_INSIDE;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexBorderArea;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexBorderLine;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * Sprite for displaying information about where a unit can move to.
 */
public class MovementEnvelopeSprite extends HexSprite {

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
        graph.scale(bv.getScale(), bv.getScale());

        graph.setStroke(new BasicStroke(LINE_THICKNESS, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
              10f, new float[] { 5f, 3f }, 0f));

        // cycle through directions
        for (int i = 0; i < 6; i++) {
            if ((borders & (1 << i)) > 0) {
                // 1) thick transparent border
                int cut = ((borders & (1 << ((i + 1) % 6))) == 0) ? CUT_RIGHT_BORDER : CUT_RIGHT_INSIDE;
                cut |= ((borders & (1 << ((i + 5) % 6))) == 0) ? CUT_LEFT_BORDER : CUT_LEFT_INSIDE;

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
