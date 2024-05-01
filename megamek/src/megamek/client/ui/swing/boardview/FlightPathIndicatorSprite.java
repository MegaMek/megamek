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
import java.awt.geom.AffineTransform;

import megamek.MMConstants;
import megamek.client.ui.swing.util.FontHandler;
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

    private static final int TEXT_SIZE = 50;
    private static final int ARROW_X_OFFSET = 14;
    private static final int ARROW_Y_OFFSET = 4;
    private static final Color COLOR_YELLOW = new Color(255, 255, 0, 200);
    private static final Color COLOR_GREEN = new Color(0, 255, 0, 200);
    private static final Color COLOR_RED = new Color(255, 0, 0, 200);
    private static final Color COLOR_OUTLINE = new Color(40, 40,40,200);

    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;
    private static final int TEXT_Y_OFFSET = 17;

    private final MoveStep step;
    private final boolean isLast;

    private static final String STRAIGHT_ARROW = "\uEB95";
    private static final String FLAG = "\uf06e";
    private static final String FLY_OFF = "\uf700";
    private static final String RIGHT_TURN = "\uEB9A";
    private static final String LEFT_TURN = "\uEBA4";

    private final StringDrawer.StringDrawerConfig symbolConfig =
            new StringDrawer.StringDrawerConfig().absoluteCenter().fontSize(TEXT_SIZE)
                    .outline(COLOR_OUTLINE, 1.5f);

    // Setup 'StringDrawers' to write special characters as icons.
    private final StringDrawer straight = new StringDrawer(STRAIGHT_ARROW)
            .at(HEX_CENTER_X, HEX_CENTER_Y - ARROW_Y_OFFSET).useConfig(symbolConfig);

    private final StringDrawer right = new StringDrawer(RIGHT_TURN)
            .at(HEX_CENTER_X + ARROW_X_OFFSET, HEX_CENTER_Y + ARROW_Y_OFFSET).useConfig(symbolConfig);

    private final StringDrawer left = new StringDrawer(LEFT_TURN)
            .at(HEX_CENTER_X - ARROW_X_OFFSET, HEX_CENTER_Y + ARROW_Y_OFFSET).useConfig(symbolConfig);

    private final StringDrawer flag = new StringDrawer(FLAG)
            .at(HEX_CENTER_X, HEX_CENTER_Y).useConfig(symbolConfig);

    private final StringDrawer flyOffIcon = new StringDrawer(FLY_OFF)
            .at(HEX_CENTER_X, HEX_CENTER_Y).useConfig(symbolConfig);


    /**
     * @param boardView - BoardView associated with the sprite.
     * @param loc - hex coordinate to place the sprite.
     * @param step - the MoveStep object that backs the flight indicator state at that hex.
     * @param last - true if the sprite represents the last indicator on the board.
     */
    public FlightPathIndicatorSprite(BoardView boardView, Coords loc, final MoveStep step, boolean last) {
        super(boardView, loc);
        this.step = step;
        isLast = last;
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
     * hex. Draw a flag for final hex and a special arrow to indicate flying
     * off the map.
     */
    private void drawSprite(Graphics2D graph) {
        AffineTransform oldTransForm = graph.getTransform();
        graph.rotate(angleForFacing(step.getFacing()), HEX_CENTER_X, HEX_CENTER_Y);
        if (isLastIndicator()) {
            if (step.getVelocityLeft() > 0) {
                flyOffIcon.draw(graph);
                drawRemainingDistance(graph, step);
            } else {
                if (canTurnForFree()) {
                    flag.color(COLOR_GREEN).draw(graph);
                } else if (canTurnWithThrustCost()) {
                    if (costsTooMuchToTurn()) {
                        flag.color(COLOR_RED).draw(graph);
                    } else {
                        flag.color(COLOR_YELLOW).draw(graph);
                    }
                } else {
                    flag.color(COLOR_RED).draw(graph);
                }
            }
        } else {
            if (canTurnForFree()) {
                left.color(COLOR_GREEN).draw(graph);
                right.color(COLOR_GREEN).draw(graph);
                straight.color(COLOR_GREEN).draw(graph);
            } else if (canTurnWithThrustCost()) {
                if (costsTooMuchToTurn()) {
                    straight.color(COLOR_RED).draw(graph);
                } else {
                    left.color(COLOR_YELLOW).draw(graph);
                    right.color(COLOR_YELLOW).draw(graph);
                    straight.color(COLOR_GREEN).draw(graph);
                }
            } else {
                straight.color(COLOR_RED).draw(graph);
            }
        }
        graph.setTransform(oldTransForm);
    }

    double angleForFacing(int facing) {
        return facing * Math.PI / 3;
    }

    /*
     * Return true if the fighter would be able to turn at this step but doesn't have enough
     * movement points left to actually make the turn.
     */
    private boolean costsTooMuchToTurn() {
        Entity entity = step.getEntity();
        int maxMP = Integer.MAX_VALUE;
        int turnCost = Integer.MIN_VALUE;


        if (null != entity) {
            maxMP = entity.getRunMP();
            turnCost = step.asfTurnCost(step.getGame(), MoveStepType.TURN_LEFT, entity);
        }

        return ((step.getMpUsed() + turnCost) > maxMP);
    }

    /*
     * Returns true if the fighter can make a turn at this step given it's current velocity
     * and turn restrictions.
     */
    private boolean canTurnWithThrustCost() {
        return (step.canAeroTurn(bv.game));
    }

    /*
     * Returns true if the fighter can make a free turn at this given step.
     */
    private boolean canTurnForFree() {
        return (step.dueFreeTurn());
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
        graph.setFont(FontHandler.symbolFont());
        return graph;
    }

    /*
     * Return true if this sprite/step is the last of the flight path indicators in the
     * flight path.
     */
    private boolean isLastIndicator() {
        return isLast;
    }

    /*
     * Render a number on the lower center of the hex that represents the distance remaining to
     * travel for the given step.
     */
    private void drawRemainingDistance(Graphics2D graph, MoveStep moveStep) {
        int velocity = moveStep.getVelocity();
        if (bv.game.getBoard().onGround()) {
            velocity *= 16;
        }

        // calculate the remaining number of hexes off the end of the map.
        int remainingDistance = velocity - moveStep.getDistance();

        // string and font setup for text.
        String remString = Integer.toString(remainingDistance);
        graph.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));

        // center the remaining distance and put it just above the fly-off icon indicator.
        int x_offset = HEX_CENTER_X - (graph.getFontMetrics(graph.getFont()).stringWidth(remString) / 2);
        int y_offset = HEX_CENTER_Y - TEXT_Y_OFFSET;

        // draw a dark gray shadow string and then a red string on top.
        graph.setColor(Color.darkGray);
        graph.drawString(remString, x_offset, y_offset);
        graph.setColor(Color.red);
        graph.drawString(remString, x_offset - 1, y_offset - 1);
    }
}