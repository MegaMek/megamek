/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * Marks one hex a Small Craft or DropShip's cranes can unload a unit into (TW p.91). Each hex is filled and outlined on
 * its own, so the hexes around a grounded DropShip read as a band of targets with the DropShip's own hexes left plain.
 * The movement envelope is not used here because it only outlines the edge of an area, which draws both the inner and
 * the outer edge of that band and looks like two rings.
 */
public class CraneUnloadTargetSprite extends HexSprite {

    /** Opacity (0-255) of the tint filling the hex. */
    private static final int FILL_OPACITY = 90;

    /** Opacity (0-255) of the line around the hex. */
    private static final int OUTLINE_OPACITY = 230;

    /** Width, in unscaled hex pixels, of the line around the hex. */
    private static final float OUTLINE_WIDTH = 2.5f;

    private final Color highlightColor;

    /**
     * Creates a crane unloading target marker.
     *
     * @param boardView      the parent board view
     * @param location       a hex the cranes can unload into
     * @param highlightColor the base colour of the marker; its opacity is replaced
     */
    public CraneUnloadTargetSprite(BoardView boardView, Coords location, Color highlightColor) {
        super(boardView, location);
        this.highlightColor = highlightColor;
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());

        graph.setColor(withOpacity(FILL_OPACITY));
        graph.fill(BoardView.getHexPoly());

        Stroke oldStroke = graph.getStroke();
        graph.setStroke(new BasicStroke(OUTLINE_WIDTH));
        graph.setColor(withOpacity(OUTLINE_OPACITY));
        graph.draw(BoardView.getHexPoly());
        graph.setStroke(oldStroke);
        graph.dispose();
    }

    private Color withOpacity(int opacity) {
        return new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), opacity);
    }

    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
