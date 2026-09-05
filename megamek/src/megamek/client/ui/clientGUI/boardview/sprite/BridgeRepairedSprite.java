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
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * Marks a bridge hex whose section was rebuilt in-game by Bridge-Building Engineers (the unofficial bridge-repair
 * option) with a small amber hazard-stripe badge, so players can tell a field-repaired section (which carries the kit's
 * lower CF) from an original bridge at a glance. The badge is driven by the persistent
 * {@link megamek.common.units.Terrains#BRIDGE_REPAIRED} marker terrain, so it survives save games and shows on every
 * client.
 */
public class BridgeRepairedSprite extends HexSprite {

    private static final Color BADGE_FILL = new Color(225, 150, 35);
    private static final Color BADGE_BORDER = new Color(35, 25, 10);
    private static final int BADGE_WIDTH = 18;
    private static final int BADGE_HEIGHT = 11;
    private static final int BADGE_CENTER_X = HexTileset.HEX_W / 2;
    // Sit the badge in the upper third of the hex, clear of the unit and any in-hex labels below.
    private static final int BADGE_CENTER_Y = (HexTileset.HEX_H * 2) / 5;

    /**
     * @param boardView the parent board view
     * @param loc       the repaired bridge hex
     */
    public BridgeRepairedSprite(BoardView boardView, Coords loc) {
        super(boardView, loc);
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());
        drawRepairBadge(graph);
        graph.dispose();
    }

    /**
     * Draws a small amber badge with black hazard stripes - a "work zone" cue that reads as a field repair without
     * needing tileset art.
     *
     * @param graph the sprite graphics
     */
    private void drawRepairBadge(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int left = BADGE_CENTER_X - (BADGE_WIDTH / 2);
        int top = BADGE_CENTER_Y - (BADGE_HEIGHT / 2);
        RoundRectangle2D badge = new RoundRectangle2D.Float(left, top, BADGE_WIDTH, BADGE_HEIGHT, 5, 5);

        graph.setColor(BADGE_FILL);
        graph.fill(badge);

        // Black hazard stripes clipped to the badge.
        Shape oldClip = graph.getClip();
        graph.clip(badge);
        graph.setColor(BADGE_BORDER);
        graph.setStroke(new BasicStroke(2.5f));
        for (int stripeX = left - BADGE_HEIGHT; stripeX < left + BADGE_WIDTH + BADGE_HEIGHT; stripeX += 7) {
            graph.drawLine(stripeX, top + BADGE_HEIGHT, stripeX + BADGE_HEIGHT, top);
        }
        graph.setClip(oldClip);

        graph.setStroke(new BasicStroke(1.2f));
        graph.setColor(BADGE_BORDER);
        graph.draw(badge);
    }

    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
