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
import java.awt.geom.Ellipse2D;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * A dot in a route's colour at the middle of a hex: a small one for each hex of the way between waypoints, so a route
 * reads as a line, and a larger one for a turning point the bot planned (HammerGS, 2026-10-03).
 */
public class RouteDotSprite extends HexSprite {

    /** The dot size of a hex on the way between waypoints, in unscaled hex pixels. */
    public static final double TRAIL_DIAMETER = 7;

    /** The dot size of a turning point the bot planned. */
    public static final double TURN_DIAMETER = 15;

    private static final Color OUTLINE_COLOR = new Color(40, 40, 40, 220);
    private static final BasicStroke OUTLINE_STROKE = new BasicStroke(1.5f);

    private final Color dotColor;
    private final double diameter;

    /**
     * @param boardView the board view to draw on
     * @param location  the hex
     * @param dotColor  the route's colour
     * @param diameter  {@link #TRAIL_DIAMETER} or {@link #TURN_DIAMETER}
     */
    public RouteDotSprite(BoardView boardView, Coords location, Color dotColor, double diameter) {
        super(boardView, location);
        this.dotColor = dotColor;
        this.diameter = diameter;
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());
        Ellipse2D.Double dot = new Ellipse2D.Double((HexTileset.HEX_W - diameter) / 2.0,
              (HexTileset.HEX_H - diameter) / 2.0, diameter, diameter);
        graph.setColor(dotColor);
        graph.fill(dot);
        graph.setColor(OUTLINE_COLOR);
        graph.setStroke(OUTLINE_STROKE);
        graph.draw(dot);
        graph.dispose();
    }

    /** A route is drawn on top of buildings and bridges in isometric view, as its flags are. */
    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
