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
import megamek.common.Entity;
import megamek.common.MovePath.MoveStepType;
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

    private static final String EMPTY_CIRCLE = "\u26AA";
    private static final String SOLID_CIRCLE = "\u26AB";
    private static final String EMPTY_FLAG = "\u2690";
    private static final String SOLID_FLAG = "\u2691";
    //private static final String EMPTY_TWO_WAY = "\u26D7";
    private static final String SOLID_TWO_WAY = "\u26D6";

    // Setup 'StringDrawers' to write special characters as icons.
    private final StringDrawer mustFlyIcon = new StringDrawer(EMPTY_CIRCLE)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_RED)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer greenFlagIcon = new StringDrawer(SOLID_FLAG)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_GREEN)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer redFlagIcon = new StringDrawer(EMPTY_FLAG)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_RED)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer yellowFlagIcon = new StringDrawer(SOLID_FLAG)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_YELLOW)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer yellowEmptyFlagIcon = new StringDrawer(EMPTY_FLAG)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_YELLOW)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer flyOffIcon = new StringDrawer(SOLID_TWO_WAY)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_YELLOW)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 2.0f);

    private final StringDrawer freeTurnIcon = new StringDrawer(SOLID_CIRCLE)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_GREEN)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer costTurnIcon = new StringDrawer(SOLID_CIRCLE)
            .at(HEX_CENTER_X, HEX_CENTER_Y)
            .color(COLOR_YELLOW)
            .fontSize(TEXT_SIZE)
            .center().outline(COLOR_OUTLINE, 1.5f);

    private final StringDrawer noThrustIcon = new StringDrawer(EMPTY_CIRCLE)
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
        Entity entity = step.getEntity();
        int maxMP = Integer.MAX_VALUE;
        int turnCost = Integer.MIN_VALUE;

        if (null != entity) {
            maxMP = entity.getRunMP();
            turnCost = step.asfTurnCost(step.getGame(), MoveStepType.TURN_LEFT, entity);
        }

        if (isLastIndicator()) {
            if (step.getVelocityLeft() > 0) {
                flyOffIcon.draw(graph);
            } else {
                // Its the last hex the bird can fly on the map - draw a flag - but what kind?
                if (step.dueFreeTurn()) {
                    // use a green flag to indicate ability to free turn on last hex.
                    greenFlagIcon.draw(graph);
                } else if (step.canAeroTurn(bv.game)) {
                    // use a yellow flag to indicate ability to turn with a cost.
                    if ((step.getMpUsed() + turnCost) > maxMP) {
                        // use an empty yellow flag to indicate turn with cost, but no remaining thrust
                        yellowEmptyFlagIcon.draw(graph);
                    } else {
                        // use a solid yellow flag to indicate player can turn on the last hex for a cost.
                        yellowFlagIcon.draw(graph);
                    }
                } else {
                    // use an empty red flag to indicate no turn on the last hex.
                    redFlagIcon.draw(graph);
                }
            }
        } else {
            if (step.dueFreeTurn()) {
                freeTurnIcon.draw(graph);
            } else if (step.canAeroTurn(bv.game)) {
                // instead of blindly trusting theoretical canTurn(), see if bird can actually turn.
                if ((step.getMpUsed() + turnCost) > maxMP) {
                    noThrustIcon.draw(graph);
                } else {
                    costTurnIcon.draw(graph);
                }
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