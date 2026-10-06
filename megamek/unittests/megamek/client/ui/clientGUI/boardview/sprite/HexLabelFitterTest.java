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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import megamek.MMConstants;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.tileset.HexTileset;
import megamek.common.moves.MoveStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that movement step labels are shrunk to fit inside the hex instead of being cut off at its edge (issues
 * #9094 and #9095).
 */
class HexLabelFitterTest {

    /** Columns at each side of the hex image that a fitted label must leave empty. */
    private static final int EDGE_COLUMNS = 2;

    /** The default movement font: SansSerif bold 26. */
    private static final Font MOVEMENT_FONT = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 26);

    private BufferedImage hexImage;
    private Graphics2D graph;
    private int originalMoveFontSize;

    @BeforeEach
    void setUp() {
        GUIPreferences preferences = GUIPreferences.getInstance();
        originalMoveFontSize = preferences.getMoveFontSize();
        preferences.setMoveFontSize(26);
        hexImage = new BufferedImage(HexTileset.HEX_W, HexTileset.HEX_H, BufferedImage.TYPE_INT_ARGB);
        graph = hexImage.createGraphics();
    }

    @AfterEach
    void tearDown() {
        graph.dispose();
        GUIPreferences.getInstance().setMoveFontSize(originalMoveFontSize);
    }

    /**
     * A WiGE that has descended ten hexes in a row shows 12 MP plus ten "+" marks. The label must stay inside the
     * hex image rather than run off both sides.
     */
    @Test
    void longWiGEDescentLabelStaysInsideTheHex() {
        MoveStep step = mock(MoveStep.class);
        when(step.getMpUsed()).thenReturn(12);
        when(step.getWiGEBonus()).thenReturn(10);
        when(step.isDanger()).thenReturn(true);

        StepSprite.drawMovementCost(step, false, new Point(0, 0), graph, Color.GREEN, true);

        assertTrue(countDrawnPixels(0, HexTileset.HEX_W) > 0, "the label was not drawn at all");
        assertEquals(0, countDrawnPixels(0, EDGE_COLUMNS), "the label runs off the left side of the hex");
        assertEquals(0, countDrawnPixels(HexTileset.HEX_W - EDGE_COLUMNS, HexTileset.HEX_W),
              "the label runs off the right side of the hex");
    }

    /**
     * Going down or up draws the cost starting at the hex centre instead of centred on it, so it has only the right
     * half of the hex to fit in.
     */
    @Test
    void leftAlignedLabelStaysInsideTheRightSideOfTheHex() {
        MoveStep step = mock(MoveStep.class);
        when(step.getMpUsed()).thenReturn(14);
        when(step.getWiGEBonus()).thenReturn(4);
        when(step.isPastDanger()).thenReturn(true);

        StepSprite.drawMovementCost(step, false, new Point(1, 15), graph, Color.GREEN, false);

        assertTrue(countDrawnPixels(0, HexTileset.HEX_W) > 0, "the label was not drawn at all");
        assertEquals(0, countDrawnPixels(HexTileset.HEX_W - EDGE_COLUMNS, HexTileset.HEX_W),
              "the label runs off the right side of the hex");
    }

    /** The TMM line under the cost is placed by the font height; a shrunk cost label must not move it. */
    @Test
    void drawingTheCostLeavesTheNormalMovementFontSet() {
        MoveStep step = mock(MoveStep.class);
        when(step.getMpUsed()).thenReturn(12);
        when(step.getWiGEBonus()).thenReturn(10);

        StepSprite.drawMovementCost(step, false, new Point(0, 0), graph, Color.GREEN, true);

        assertEquals(26, graph.getFont().getSize());
    }

    @Test
    void shortLabelKeepsItsFont() {
        Font fitted = HexLabelFitter.fitToWidth(graph, MOVEMENT_FONT, "5", HexLabelFitter.centredLabelWidth());

        assertSame(MOVEMENT_FONT, fitted);
    }

    @Test
    void longLabelIsShrunkUntilItFits() {
        String label = "(12++++++++++*)";
        int maxWidth = HexLabelFitter.centredLabelWidth();

        Font fitted = HexLabelFitter.fitToWidth(graph, MOVEMENT_FONT, label, maxWidth);

        assertTrue(fitted.getSize2D() < MOVEMENT_FONT.getSize2D());
        assertTrue(graph.getFontMetrics(fitted).stringWidth(label) <= maxWidth);
        assertEquals(MOVEMENT_FONT.getStyle(), fitted.getStyle());
        assertEquals(MOVEMENT_FONT.getName(), fitted.getName());
    }

    @Test
    void labelThatCannotFitStopsAtTheMinimumSize() {
        String label = "+".repeat(200);

        Font fitted = HexLabelFitter.fitToWidth(graph, MOVEMENT_FONT, label, HexLabelFitter.centredLabelWidth());

        assertEquals(HexLabelFitter.MINIMUM_FONT_SIZE, fitted.getSize2D());
    }

    @Test
    void availableWidthsLeaveTheEdgeMarginClear() {
        assertEquals(HexTileset.HEX_W - (2 * HexLabelFitter.EDGE_MARGIN), HexLabelFitter.centredLabelWidth());
        assertEquals(HexTileset.HEX_W - HexLabelFitter.EDGE_MARGIN - 43, HexLabelFitter.labelWidthFrom(43));
    }

    /** Counts pixels with any opacity in the columns from startColumn (inclusive) to endColumn (exclusive). */
    private int countDrawnPixels(int startColumn, int endColumn) {
        int drawnPixels = 0;
        for (int column = startColumn; column < endColumn; column++) {
            for (int row = 0; row < HexTileset.HEX_H; row++) {
                if ((hexImage.getRGB(column, row) >>> 24) != 0) {
                    drawnPixels++;
                }
            }
        }
        return drawnPixels;
    }
}
