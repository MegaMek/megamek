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
import java.awt.geom.Path2D;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * Marks a hex with a flag symbol. Used for hexes designated as objectives / victory hexes, with the banner filled in
 * the owning player's color so each side can tell whose designation it is. The flag is drawn as a plain Java2D shape
 * (a dark pole with a solidly filled, outlined banner), so it stays clearly visible on any terrain, scales cleanly
 * with the board zoom and needs no image file. (The bundled Material Symbols font only renders its outlined
 * variants, which playtesting showed are too faint to spot on the board.)
 */
public class HexFlagSprite extends HexSprite {

    // flag geometry in unscaled hex coordinates (a hex image is 84 x 72)
    private static final int POLE_X = 32;
    private static final int POLE_TOP_Y = 16;
    private static final int POLE_BOTTOM_Y = 56;
    private static final int BANNER_RIGHT_X = 58;
    private static final int BANNER_BOTTOM_Y = 32;

    private static final Color POLE_COLOR = new Color(40, 40, 40, 230);
    private static final Color OUTLINE_COLOR = new Color(40, 40, 40, 220);
    private static final BasicStroke POLE_STROKE =
          new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke OUTLINE_STROKE =
          new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final Color flagColor;

    /**
     * @param boardView the board view this sprite is displayed on
     * @param location  the hex to mark with the flag
     * @param flagColor the fill color of the flag's banner, usually the owning player's color
     */
    public HexFlagSprite(BoardView boardView, Coords location, Color flagColor) {
        super(boardView, location);
        this.flagColor = flagColor;
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());

        // banner: solidly filled in the player color, dark outline for contrast on light terrain
        Path2D.Float banner = new Path2D.Float();
        banner.moveTo(POLE_X + 1, POLE_TOP_Y);
        banner.lineTo(BANNER_RIGHT_X, POLE_TOP_Y + 2);
        banner.lineTo(BANNER_RIGHT_X, BANNER_BOTTOM_Y - 2);
        banner.lineTo(POLE_X + 1, BANNER_BOTTOM_Y);
        banner.closePath();
        graph.setColor(flagColor);
        graph.fill(banner);
        graph.setColor(OUTLINE_COLOR);
        graph.setStroke(OUTLINE_STROKE);
        graph.draw(banner);

        // pole: a thick dark line the banner hangs from
        graph.setColor(POLE_COLOR);
        graph.setStroke(POLE_STROKE);
        graph.drawLine(POLE_X, POLE_TOP_Y, POLE_X, POLE_BOTTOM_Y);

        graph.dispose();
    }

    /** The flag should be displayed on top of buildings and bridges in isometric view. */
    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
