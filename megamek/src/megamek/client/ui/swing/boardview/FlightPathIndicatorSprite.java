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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;
import megamek.common.MoveStep;

/**
 * The Flight Path Indicator Sprite paints a path in front of an aerospace movement path
 * and indicates the turn status and remaining velocity by showing the length of path
 * the flight must follow and when the unit can turn.
 *
 * A green solid circle indicates the craft can make a free turn on that hex.
 * A yellow solid circle indicates the craft can make a turn with cost to thrust.
 * A red empty circle indicates the craft cannot turn on that hex.
 * A green flag indicates the craft has used all its remaining velocity on that hex.
 * A yellow two-way diamond indicates the craft will fly off the map with remaining velocity.
 */
public class FlightPathIndicatorSprite extends HexSprite {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int TEXT_SIZE = 30;
    private static final Color COLOR_YELLOW = new Color(255, 255, 0, 128);
    private static final Color COLOR_GREEN = new Color(0, 255, 0, 128);
    private static final Color COLOR_RED = new Color(255, 0, 0, 128);
    private static final Color COLOR_OUTLINE = new Color(40, 40,40,200);

    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;

    private MoveStep step = null;
    private boolean isLast = false;

    //U+26AA  MEDIUM WHITE CIRCLE
    //U+26AB  MEDIUM BLACK CIRCLE
    //U+2690  WHITE FLAG
    //U+2691  BLACK FLAG
    //U+26E2  ASTRONOMICAL SYMBOL FOR URANUS
    //U+26D6  BLACK TWO-WAY LEFT WAY TRAFFIC
    //U+26D7  WHITE TWO-WAY LEFT WAY TRAFFIC

    //Draw a special character 'circle'.
    private final StringDrawer mustFlyIcon = new StringDrawer("\u26AA")
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_RED)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer finalIcon = new StringDrawer("\u2691")
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_GREEN)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer flyOffIcon = new StringDrawer("\u26D6")
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_YELLOW)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 2.0f);

    private final StringDrawer freeTurnIcon = new StringDrawer("\u26AB")
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_GREEN)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer costTurnIcon = new StringDrawer("\u26AB")
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_YELLOW)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    /**
     * @param boardView - BoardView associated with the sprite.
     * @param loc - hex coordinate to place the sprite.
     * @param step - the MoveStep object that backs the flight indicator state at that hex.
     * @param last - true if the sprite represents the last indicator on the board.
     */
    public FlightPathIndicatorSprite(BoardView boardView, Coords loc, final MoveStep step, boolean last) {
        super(boardView, loc);
        this.step = step;
        this.isLast = last;
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        drawSprite(graph);
        graph.dispose();
    }

    /*
     * Based on the condition of the MoveStep associated with this sprite, draw a
     * green, yellow, or red flight path indicator to indicate turn status on that
     * hex.  Draw a flag for final hex and a yellow diamond to indicate flying
     * off the map.
     */
    private void drawSprite(Graphics2D graph) {
        if (this.isLastIndicator()) {
            if (this.step.getVelocityLeft() > 0) {
                flyOffIcon.draw(graph);
            } else {
                finalIcon.draw(graph);
            }
        } else {
            if (this.step.dueFreeTurn()) {
                freeTurnIcon.draw(graph);
            } else if (this.step.canAeroTurn(bv.game)) {
                costTurnIcon.draw(graph);
            } else {
                mustFlyIcon.draw(graph);
            }
        }

        return;
    }

    /*
     * Standard Hex Sprite 2D Graphics setup.  Creates the context, base hex image
     * settings, scale, and fonts.
     */
    private Graphics2D spriteSetup() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.scale, bv.scale);

        fontSetup(graph);

        return graph;
    }

    /*
     * Sets the font name, style, and size from configured default parameters.
     */
    private void fontSetup(Graphics2D graph) {
        String fontName = GUIP.getMoveFontType();
        int fontStyle = GUIP.getMoveFontStyle();
        graph.setFont(new Font(fontName, fontStyle, TEXT_SIZE));
    }

    /*
     * Return true if this sprite/step is the last of the flight path indicators in the
     * flight path.
     */
    private boolean isLastIndicator() {
        return isLast;
    }
}