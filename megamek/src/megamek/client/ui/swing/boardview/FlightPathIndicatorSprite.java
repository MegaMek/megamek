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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.FontHandler;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
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
    private static final int TEXT_SIZE = 50;
    private static final int ARROW_X_OFFSET = 14;
    private static final int ARROW_Y_OFFSET = 4;
    private static final Color COLOR_OUTLINE = new Color(40, 40,40,200);

    private static final Color COLOR_CIRCLE = new Color(255, 255, 255, 128);
    private static final int CIRCLE_RADIUS = 60;

    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;
    private static final int TEXT_Y_OFFSET = 17;

    private final MoveStep currentStep;
    private final boolean isLast;
    private final boolean isRepeated;

    private static final String STRAIGHT_ARROW = "\uEB95";
    private static final String FLAG = "\uf06e";
    private static final String FLY_OFF = "\uf700";
    private static final String RIGHT_TURN = "\uEB9A";
    private static final String LEFT_TURN = "\uEBA4";

    private final StringDrawer.StringDrawerConfig symbolConfig =
            new StringDrawer.StringDrawerConfig().absoluteCenter().fontSize(TEXT_SIZE)
                    .outline(COLOR_OUTLINE, 2.5f);

    // Setup 'StringDrawers' to write special characters as icons.
    private final StringDrawer straight = new StringDrawer(STRAIGHT_ARROW)
            .at(HEX_CENTER_X, HEX_CENTER_Y).useConfig(symbolConfig);

    private final StringDrawer right = new StringDrawer(RIGHT_TURN)
            .at(HEX_CENTER_X + ARROW_X_OFFSET, HEX_CENTER_Y + ARROW_Y_OFFSET).useConfig(symbolConfig);

    private final StringDrawer left = new StringDrawer(LEFT_TURN)
            .at(HEX_CENTER_X - ARROW_X_OFFSET, HEX_CENTER_Y + ARROW_Y_OFFSET).useConfig(symbolConfig);

    private final StringDrawer flag = new StringDrawer(FLAG)
            .at(HEX_CENTER_X, HEX_CENTER_Y).useConfig(symbolConfig);

    private final StringDrawer flyOffIcon = new StringDrawer(FLY_OFF)
            .at(HEX_CENTER_X, HEX_CENTER_Y).useConfig(symbolConfig);

    public FlightPathIndicatorSprite(BoardView boardView, List<MoveStep> steps, int index, boolean last) {
        super(boardView, steps.get(index).getPosition());
        currentStep = steps.get(index);
        isLast = last;
        isRepeated = (index > 0) && equalType(steps.get(index - 1));
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
        Color green = GUIP.getOkColor();
        Color yellow = GUIP.getCautionColor();
        Color red = GUIP.getWarningColor();
        if (!isRepeated) {
            drawBackGroundCircle(graph);
        }
        if (isLastIndicator()) {
            if (currentStep.getVelocityLeft() > 0) {
                drawBackGroundCircle(graph);
                flyOffIcon.color(green).rotate(angleForFacing(currentStep.getFacing())).draw(graph);
                drawRemainingDistance(graph, currentStep);
            } else {
                if (canTurnForFree()) {
                    flag.color(green).draw(graph);
                } else if (canTurnWithThrustCost()) {
                    if (costsTooMuchToTurn()) {
                        flag.color(red).draw(graph);
                    } else {
                        flag.color(yellow).draw(graph);
                    }
                } else {
                    flag.color(red).draw(graph);
                }
            }
        } else {
            AffineTransform oldTransForm = graph.getTransform();
            graph.rotate(angleForFacing(currentStep.getFacing()), HEX_CENTER_X, HEX_CENTER_Y);
            if (canTurnForFree()) {
                if (!isRepeated) {
                    left.color(green).draw(graph);
                    right.color(green).draw(graph);
                }
                straight.color(green).draw(graph);
            } else if (canTurnWithThrustCost()) {
                if (costsTooMuchToTurn()) {
                    straight.color(red).draw(graph);
                } else {
                    if (!isRepeated) {
                        left.color(yellow).draw(graph);
                        right.color(yellow).draw(graph);
                    }
                    straight.color(green).draw(graph);
                }
            } else {
                straight.color(red).draw(graph);
            }
            graph.setTransform(oldTransForm);
        }
    }

    private double angleForFacing(int facing) {
        return facing * Math.PI / 3;
    }

    private void drawBackGroundCircle(Graphics2D graph) {
        graph.setColor(COLOR_CIRCLE);
        graph.fillOval(HEX_CENTER_X - CIRCLE_RADIUS / 2, HEX_CENTER_Y - CIRCLE_RADIUS / 2, CIRCLE_RADIUS, CIRCLE_RADIUS);
    }

    /*
     * Return true if the fighter would be able to turn at this step but doesn't have enough
     * movement points left to actually make the turn.
     */
    private boolean costsTooMuchToTurn(MoveStep step) {
        Entity entity = step.getEntity();
        int maxMP = Integer.MAX_VALUE;
        int turnCost = Integer.MIN_VALUE;


        if (null != entity) {
            maxMP = entity.getRunMP();
            turnCost = step.asfTurnCost(step.getGame(), MoveStepType.TURN_LEFT, entity);
        }

        return ((step.getMpUsed() + turnCost) > maxMP);
    }

    private boolean costsTooMuchToTurn() {
        return costsTooMuchToTurn(currentStep);
    }

    /*
     * Returns true if the fighter can make a turn at this step given it's current velocity
     * and turn restrictions.
     */
    private boolean canTurnWithThrustCost(MoveStep step) {
        return step.canAeroTurn(bv.game);
    }

    private boolean canTurnWithThrustCost() {
        return canTurnWithThrustCost(currentStep);
    }

    /*
     * Returns true if the fighter can make a free turn at this given step.
     */
    private boolean canTurnForFree() {
        return canTurnForFree(currentStep);
    }

    private boolean canTurnForFree(MoveStep step) {
        return step.dueFreeTurn();
    }

    private boolean equalType(MoveStep otherStep) {
        return bothFreeTurn(otherStep)
                || (!canTurnForFree() && bothTurnWithCost(otherStep))
                || (bothOnlyStraight(otherStep));
    }

    private boolean bothFreeTurn(MoveStep otherStep) {
        return canTurnForFree(otherStep) && canTurnForFree();
    }

    private boolean bothTurnWithCost(MoveStep otherStep) {
        return canTurnWithThrustCost(otherStep) && canTurnWithThrustCost();
    }

    private boolean onlyStraight(MoveStep step) {
        return !canTurnForFree(step) && !canTurnWithThrustCost(step);
    }

    private boolean bothOnlyStraight(MoveStep otherStep) {
        return onlyStraight(otherStep) && onlyStraight(currentStep);
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

        int remainingDistance = velocity - moveStep.getDistance();
        new StringDrawer(Integer.toString(remainingDistance)).at(HEX_CENTER_X, HEX_CENTER_Y - TEXT_Y_OFFSET)
                .font(new Font(GUIP.getMoveFontType(), GUIP.getMoveFontStyle(), 16)).outline(COLOR_OUTLINE, 2.5f)
                .color(GUIP.getOkColor()).centerX().draw(graph);
    }
}