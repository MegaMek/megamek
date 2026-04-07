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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * Displays a circular saw blade indicator on hexes that are being cleared by saws. Shows a steel gray blade with
 * triangular teeth and a turns-remaining number in the center, positioned in the lower portion of the hex.
 */
public class SawClearingSprite extends HexSprite {

    private static final Color SAW_TOOTH_COLOR = new Color(100, 100, 110);
    private static final Color SAW_BLADE_COLOR = new Color(170, 170, 180);
    private static final Color SAW_INNER_COLOR = new Color(130, 130, 140);
    private static final Color SAW_TEXT_OUTLINE_COLOR = new Color(40, 40, 50);
    private static final Color SAW_OUTLINE_COLOR = new Color(60, 60, 70);

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int BLADE_RADIUS = 10;
    private static final int TOOTH_HEIGHT = 4;
    private static final int NUM_TEETH = 12;
    private static final int INNER_RADIUS = 6;
    private static final int FONT_SIZE = 11;
    private static final int BOTTOM_OFFSET = 20;

    private final int turnsRemaining;

    /**
     * Creates a new saw clearing sprite for the given hex.
     *
     * @param boardView      the parent board view
     * @param loc            the hex coordinates
     * @param turnsRemaining the number of turns remaining to complete clearing
     */
    public SawClearingSprite(BoardView boardView, Coords loc, int turnsRemaining) {
        super(boardView, loc);
        this.turnsRemaining = turnsRemaining;
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        drawSawBlade(graph);
        graph.dispose();
    }

    private Graphics2D spriteSetup() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());
        return graph;
    }

    private void drawSawBlade(Graphics2D graph) {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Position in lower portion of hex, centered horizontally
        int centerX = HEX_CENTER_X;
        int centerY = HexTileset.HEX_H - BOTTOM_OFFSET;

        // Draw outer teeth (dark steel color)
        graph.setColor(SAW_TOOTH_COLOR);
        for (int i = 0; i < NUM_TEETH; i++) {
            double angle = (2 * Math.PI * i) / NUM_TEETH;
            double nextAngle = (2 * Math.PI * (i + 0.5)) / NUM_TEETH;
            int outerX = centerX + (int) ((BLADE_RADIUS + TOOTH_HEIGHT) * Math.cos(angle));
            int outerY = centerY + (int) ((BLADE_RADIUS + TOOTH_HEIGHT) * Math.sin(angle));
            int leftX = centerX + (int) (BLADE_RADIUS * Math.cos(angle - 0.15));
            int leftY = centerY + (int) (BLADE_RADIUS * Math.sin(angle - 0.15));
            int rightX = centerX + (int) (BLADE_RADIUS * Math.cos(nextAngle));
            int rightY = centerY + (int) (BLADE_RADIUS * Math.sin(nextAngle));
            int[] xPoints = { outerX, leftX, rightX };
            int[] yPoints = { outerY, leftY, rightY };
            graph.fillPolygon(xPoints, yPoints, 3);
        }

        // Draw blade body (steel gray)
        graph.setColor(SAW_BLADE_COLOR);
        graph.fillOval(centerX - BLADE_RADIUS, centerY - BLADE_RADIUS,
              BLADE_RADIUS * 2, BLADE_RADIUS * 2);

        // Draw inner ring (darker)
        graph.setColor(SAW_INNER_COLOR);
        graph.fillOval(centerX - INNER_RADIUS, centerY - INNER_RADIUS,
              INNER_RADIUS * 2, INNER_RADIUS * 2);

        // Draw turns remaining number in the center of the blade
        String turnsStr = String.valueOf(turnsRemaining);
        Font turnsFont = new Font("Sans Serif", Font.BOLD, FONT_SIZE);
        FontMetrics fm = graph.getFontMetrics(turnsFont);
        int textWidth = fm.stringWidth(turnsStr);
        int textX = centerX - (textWidth / 2);
        int textY = centerY + (fm.getAscent() / 2) - 1;

        // White text with dark outline for readability
        graph.setFont(turnsFont);
        graph.setColor(SAW_TEXT_OUTLINE_COLOR);
        graph.drawString(turnsStr, textX - 1, textY);
        graph.drawString(turnsStr, textX + 1, textY);
        graph.drawString(turnsStr, textX, textY - 1);
        graph.drawString(turnsStr, textX, textY + 1);
        graph.setColor(Color.WHITE);
        graph.drawString(turnsStr, textX, textY);

        // Draw blade outline
        graph.setColor(SAW_OUTLINE_COLOR);
        Stroke oldStroke = graph.getStroke();
        graph.setStroke(new BasicStroke(1));
        graph.drawOval(centerX - BLADE_RADIUS, centerY - BLADE_RADIUS,
              BLADE_RADIUS * 2, BLADE_RADIUS * 2);
        graph.setStroke(oldStroke);
    }

    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
