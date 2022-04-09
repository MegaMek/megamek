/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/
package megamek.client.ui.swing.boardview;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityMovementType;
import megamek.common.IAero;
import megamek.common.MiscType;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;

/**
 * Sprite for a step in a movement path. Only one sprite should exist for
 * any hex in a path. Contains a colored number, and arrows indicating
 * entering, exiting or turning.
 */
class StepSprite extends Sprite {
    
    private final static GUIPreferences GUIP = GUIPreferences.getInstance(); 
    private static AffineTransform shadowOffset = new AffineTransform();
    private static AffineTransform upDownOffset = new AffineTransform();
    private static AffineTransform stepOffset = new AffineTransform();
    static {
        shadowOffset.translate(-1, -1);
        upDownOffset.translate(-30, 0);
        stepOffset.translate(1, 1);
    }

    private MoveStep step;
    private boolean isLastStep;
    private Image baseScaleImage;

    public StepSprite(BoardView boardView1, final MoveStep step,
                      boolean isLastStep) {
        super(boardView1);
        this.step = step;
        this.isLastStep = isLastStep;

        // step is the size of the hex that this step is in
        bounds = new Rectangle(bv.getHexLocation(step.getPosition()), bv.hex_size);
        image = null;
        baseScaleImage = null;
    }

    /**
     * Refreshes this StepSprite's image to handle changes in the zoom
     * level.
     */
    public void refreshZoomLevel() {
        if (baseScaleImage == null) {
            return;
        }

        image = bv.getScaledImage(baseScaleImage, false);
    }

    @Override
    public void prepare() {
        // create image for buffer
        Image tempImage = new BufferedImage(BoardView.HEX_W, BoardView.HEX_H,
                BufferedImage.TYPE_INT_ARGB);
        Graphics graph = tempImage.getGraphics();
        Graphics2D g2D = (Graphics2D) graph;

        GUIPreferences.AntiAliasifSet(graph);

        // fill with key color
        graph.setColor(new Color(0, 0, 0, 0));
        graph.fillRect(0, 0, BoardView.HEX_W, BoardView.HEX_H);

        // setup some variables
        Shape moveArrow = bv.movementPolys[step.getFacing()];
        Shape facingArrow = bv.facingPolys[step.getFacing()];

        boolean isLastLegalStep = isLastStep &&
                (step.getMovementType(true) != EntityMovementType.MOVE_ILLEGAL);

        Color col;
        // Choose the color according to the type of movement
        switch (step.getMovementType(isLastStep)) {
            case MOVE_RUN:
            case MOVE_VTOL_RUN:
            case MOVE_OVER_THRUST:
                if (step.isUsingMASC() || step.isUsingSupercharger()) {
                    col = GUIP.getColor("AdvancedMoveMASCColor");
                } else {
                    col = GUIP.getColor("AdvancedMoveRunColor");
                }
                break;
            case MOVE_JUMP:
                col = GUIP.getColor("AdvancedMoveJumpColor");
                break;
            case MOVE_SPRINT:
            case MOVE_VTOL_SPRINT:
                col = GUIP.getColor("AdvancedMoveSprintColor");
                break;
            case MOVE_ILLEGAL:
                col = GUIP.getColor("AdvancedMoveIllegalColor");
                break;
            default:
                if (Stream.of(MoveStepType.BACKWARDS, MoveStepType.LATERAL_LEFT_BACKWARDS, MoveStepType.LATERAL_RIGHT_BACKWARDS).anyMatch(moveStepType -> (step.getType() == moveStepType))) {
                    col = GUIP.getColor("AdvancedMoveBackColor");
                } else {
                    col = GUIP.getColor("AdvancedMoveDefaultColor");
                }
                break;
        }

        if (bv.game.useVectorMove()) {
            drawActiveVectors(step, graph);
        }

        drawConditions(step, graph, col);

        Shape currentArrow;
        // draw arrows and cost for the step
        switch (step.getType()) {
            case FORWARDS:
            case SWIM:
            case BACKWARDS:
            case CHARGE:
            case DFA:
            case LATERAL_LEFT:
            case LATERAL_RIGHT:
            case LATERAL_LEFT_BACKWARDS:
            case LATERAL_RIGHT_BACKWARDS:
            case DEC:
            case DECN:
            case ACC:
            case ACCN:
            case LOOP:
                // forward movement arrow
                drawArrowShape(g2D, moveArrow, col);
                drawMovementCost(step, isLastStep, new Point(0, 0), graph, col, true);
                drawRemainingVelocity(step, graph);
                break;
            case GO_PRONE:
            case HULL_DOWN:
            case DOWN:
            case DIG_IN:
            case FORTIFY:
            case TAKE_COVER:
                // draw arrow indicating dropping prone
                currentArrow = upDownOffset.createTransformedShape(bv.downArrow);
                drawArrowShape(g2D, currentArrow, col);
                drawMovementCost(step, isLastStep, new Point(1, 15), graph, col, false);
                drawRemainingVelocity(step, graph);
                break;
            case GET_UP:
            case UP:
            case CAREFUL_STAND:
                // draw arrow indicating standing up
                currentArrow = upDownOffset.createTransformedShape(bv.upArrow);
                drawArrowShape(g2D, currentArrow, col);
                drawMovementCost(step, isLastStep, new Point(0, 15), graph, col, false);
                drawRemainingVelocity(step, graph);
                break;
            case CLIMB_MODE_ON:
                String climb;
                if (step.getEntity().getMovementMode() == EntityMovementMode.WIGE) {
                    climb = Messages.getString("BoardView1.WIGEClimb");
                } else {
                    climb = Messages.getString("BoardView1.Climb");
                }
                drawAnnouncement(g2D, climb, step, col);
                break;
            case CLIMB_MODE_OFF:
                String climbOff;
                if (step.getEntity().getMovementMode() == EntityMovementMode.WIGE) {
                    climbOff = Messages.getString("BoardView1.WIGEClimbOff");
                } else {
                    climbOff = Messages.getString("BoardView1.ClimbOff");
                }
                drawAnnouncement(g2D, climbOff, step, col);
                break;
            case TURN_LEFT:
            case TURN_RIGHT:
            case THRUST:
            case YAW:
            case EVADE:
            case ROLL:
                // if this is the last legal step then the facing arrow is drawn later
                if (!isLastLegalStep) {
                    // draw arrows showing the facing
                    drawArrowShape(g2D, facingArrow, col);
                }

                if (bv.game.useVectorMove()) {
                    drawMovementCost(step, isLastStep, new Point(0, 0), graph, col, false);
                }
                break;
            case BOOTLEGGER:
                // draw arrows showing them entering the next
                drawArrowShape(g2D, moveArrow, col);
                drawMovementCost(step, isLastStep, new Point(0, 0), graph, col, true);
                break;
            case LOAD:
                String load = Messages.getString("BoardView1.Load");
                drawAnnouncement(g2D, load, step, col);
                break;
            case TOW:
                String tow = Messages.getString("BoardView1.Tow");
                drawAnnouncement(g2D, tow, step, col);
                break;
            case DISCONNECT:
                String disconnect = Messages.getString("BoardView1.Disconnect");
                drawAnnouncement(g2D, disconnect, step, col);
                break;
            case LAUNCH:
            case UNDOCK:
                String launch = Messages.getString("BoardView1.Launch");
                drawAnnouncement(g2D, launch, step, col);
                break;
            case DROP:
                String drop = Messages.getString("BoardView1.Drop");
                drawAnnouncement(g2D, drop, step, col);
                break;
            case RECOVER:
                String recover = Messages.getString("BoardView1.Recover");
                if (step.isDocking()) {
                    recover = Messages.getString("BoardView1.Dock");
                }
                drawAnnouncement(g2D, recover, step, col);
                break;
            case JOIN:
                String join = Messages.getString("BoardView1.Join");
                drawAnnouncement(g2D, join, step, col);
                break;
            case UNLOAD:
                String unload = Messages.getString("BoardView1.Unload");
                drawAnnouncement(g2D, unload, step, col);
                break;
            case HOVER:
                String hover = Messages.getString("BoardView1.Hover");
                drawAnnouncement(g2D, hover, step, col);
                break;
            case LAND:
                String land = Messages.getString("BoardView1.Land");
                drawAnnouncement(g2D, land, step, col);
                break;
            case CONVERT_MODE:
                int modePos = 38;
                if (step.getMp() > 0) {
                    // draw movement cost
                    drawMovementCost(step, isLastStep, new Point(0, 0), graph, col, true);
                    modePos += 16;
                }
                // show new movement mode
                String mode = Messages.getString("BoardView1.ConversionMode."
                        + step.getMovementMode());
                graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
                int modeX = 42 - (graph.getFontMetrics(graph.getFont()).stringWidth(mode) / 2);
                graph.setColor(Color.darkGray);
                graph.drawString(mode, modeX, modePos - 1);
                graph.setColor(col);
                graph.drawString(mode, modeX - 1, modePos);

                break;
            default:
                break;
        }

        // draw arrows showing the facing for final step only
        if (isLastLegalStep) {
            Shape finalFacingArrow = bv.finalFacingPolys[step.getFacing()];
            drawArrowShape(g2D, finalFacingArrow, col);
        }

        if (step.isVTOLBombingStep() || step.isStrafingStep()) {
            graph.setColor(col);
            g2D.fill(HexDrawUtilities.getHexFullBorderArea(3, 0));
        }

        baseScaleImage = bv.createImage(tempImage.getSource());
        image = bv.getScaledImage(bv.createImage(tempImage.getSource()), false);

        graph.dispose();
        tempImage.flush();
    }

    /** Draws the given form in the given Color col with a shadow. */ 
    private void drawArrowShape(Graphics2D graph, Shape form, Color col) {
        graph.setColor(Color.darkGray);
        Shape currentArrow = stepOffset.createTransformedShape(form);
        graph.fill(currentArrow);

        graph.setColor(col);
        currentArrow = shadowOffset.createTransformedShape(currentArrow);
        graph.fill(currentArrow);
    }
    
    private void drawAnnouncement(Graphics2D graph, String text, MoveStep step, Color col) {
        if (step.isPastDanger()) {
            text = "(" + text + ")";
        }
        graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int posX = 42 - (graph.getFontMetrics(graph.getFont()).stringWidth(text) / 2);
        int posY = 38 + graph.getFontMetrics(graph.getFont()).getHeight();
        graph.setColor(Color.darkGray);
        graph.drawString(text, posX, posY + 1);
        graph.setColor(col);
        graph.drawString(text, posX - 1, posY);
    }

    /**
     * Draws conditions separate from the step. Allows keeping
     * conditions on Aeros even when that step is erased (advanced
     * movement), such as evading, rolling, loading and
     * unloading.
     */
    private void drawConditions(MoveStep step, Graphics graph, Color col) {
        if (step.isEvading()) {
            String evade = Messages.getString("BoardView1.Evade");
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int evadeX = 42 - (graph.getFontMetrics(graph.getFont()).stringWidth(evade) / 2);
            graph.setColor(Color.darkGray);
            graph.drawString(evade, evadeX, 64);
            graph.setColor(col);
            graph.drawString(evade, evadeX - 1, 63);
        }

        if (step.isRolled()) {
            // Announce roll
            String roll = Messages.getString("BoardView1.Roll");
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int rollX = 42 - (graph.getFontMetrics(graph.getFont()).stringWidth(roll) / 2);
            graph.setColor(Color.darkGray);
            graph.drawString(roll, rollX, 18);
            graph.setColor(col);
            graph.drawString(roll, rollX - 1, 17);
        }
    }

    private void drawActiveVectors(MoveStep step, Graphics graph) {

        /*
         * TODO: it might be better to move this to the MovementSprite so
         * that it is visible before first step and you can't see it for all
         * entities
         */

        int[] activeXpos = {39, 59, 59, 40, 19, 19};
        int[] activeYpos = {20, 28, 52, 59, 52, 28};

        int[] v = step.getVectors();
        for (int i = 0; i < 6; i++) {
            String active = Integer.toString(v[i]);
            graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
            graph.setColor(Color.darkGray);
            graph.drawString(active, activeXpos[i], activeYpos[i]);
            graph.setColor(Color.red);
            graph.drawString(active, activeXpos[i] - 1, activeYpos[i] - 1);
        }

    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(bv.getHexLocation(step.getPosition()), bv.hex_size);
        return bounds;
    }

    public MoveStep getStep() {
        return step;
    }

    private Font getMovementFont() {
        String fontName = GUIPreferences.getInstance().getString(
                GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
        int fontStyle = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
        int fontSize = GUIPreferences.getInstance().getInt(
                GUIPreferences.ADVANCED_MOVE_FONT_SIZE);

        return new Font(fontName, fontStyle, fontSize);
    }

    private void drawRemainingVelocity(MoveStep step, Graphics graph) {
        StringBuilder velStringBuf = new StringBuilder();

        if (bv.game.useVectorMove()) {
            return;
        }

        if (!step.getEntity().isAirborne()
                || !step.getEntity().isAero()) {
            return;
        }

        if (((IAero) step.getEntity()).isSpheroid()) {
            return;
        }

        int distTraveled = step.getDistance();
        int velocity = step.getVelocity();
        if (bv.game.getBoard().onGround()) {
            velocity *= 16;
        }

        velStringBuf.append("(").append(distTraveled).append("/")
                .append(velocity).append(")");

        Color col = (step.getVelocityLeft() > 0) ? Color.RED : Color.GREEN;

        // Convert the buffer to a String and draw it.
        String velString = velStringBuf.toString();
        graph.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int costX = 42;
        costX -= (graph.getFontMetrics(graph.getFont()).stringWidth(velString) / 2);
        graph.setColor(Color.darkGray);
        graph.drawString(velString, costX, 28);
        graph.setColor(col);
        graph.drawString(velString, costX - 1, 27);

        // if we are in atmosphere, then report the free turn status as well
        if (!bv.game.getBoard().inSpace()) {
            if (step.dueFreeTurn()) {
                col = Color.GREEN;
            } else if (step.canAeroTurn(bv.game)) {
                col = Color.YELLOW;
            } else {
                col = Color.RED;
            }

            String turnString = "<" + step.getNStraight() + ">";
            graph.setFont(new Font("SansSerif", Font.PLAIN, 10));
            costX = 50;
            graph.setColor(Color.darkGray);
            graph.drawString(turnString, costX, 15);
            graph.setColor(col);
            graph.drawString(turnString, costX - 1, 14);
        }
    }

    private void drawMovementCost(MoveStep step, boolean isLastStep,
                                  Point stepPos, Graphics graph, Color col, boolean shiftFlag) {
        StringBuilder costStringBuf = new StringBuilder();
        costStringBuf.append(step.getMpUsed());

        Entity e = step.getEntity();

        // If the step is using a road bonus, mark it.
        if (step.isOnlyPavement() && e.isEligibleForPavementBonus()) {
            costStringBuf.append("+");
        }

        // Show WiGE descent bonus
        costStringBuf.append("+".repeat(Math.max(0, step.getWiGEBonus())));

        // If the step is dangerous, mark it.
        if (step.isDanger()) {
            costStringBuf.append("*");
        }

        // If the step is past danger, mark that.
        if (step.isPastDanger()) {
            costStringBuf.insert(0, "(");
            costStringBuf.append(")");
        }

        EntityMovementType moveType = step.getMovementType(isLastStep);
        if ((moveType == EntityMovementType.MOVE_VTOL_WALK)
                || (moveType == EntityMovementType.MOVE_VTOL_RUN)
                || (moveType == EntityMovementType.MOVE_VTOL_SPRINT)
                || (moveType == EntityMovementType.MOVE_SUBMARINE_WALK)
                || (moveType == EntityMovementType.MOVE_SUBMARINE_RUN)) {
            costStringBuf.append("{").append(step.getElevation())
                    .append("}");
        }

        if (step.getAltitude() > 0) {
            costStringBuf.append("{").append(step.getAltitude())
                    .append("}");
        }

        // Convert the buffer to a String and draw it.
        String costString = costStringBuf.toString();
        graph.setFont(getMovementFont());
        int costX = stepPos.x + 42;
        if (shiftFlag) {
            costX -= (graph.getFontMetrics(graph.getFont()).stringWidth(costString) / 2);
        }
        graph.setColor(Color.darkGray);
        graph.drawString(costString, costX, stepPos.y + 39);
        graph.setColor(col);
        graph.drawString(costString, costX - 1, stepPos.y + 38);

        // draw target number hints smaller
        int rollY = (graph.getFontMetrics(graph.getFont()).getHeight() / 2);
        StringBuilder rollsStringBuf = new StringBuilder();
        if (step.isUsingSupercharger() && !step.getEntity().hasWorkingMisc(MiscType.F_JET_BOOSTER)) {
            rollsStringBuf.append('S');
            rollsStringBuf.append(step.getTargetNumberSupercharger());
            rollsStringBuf.append('+');
        }

        if (step.isUsingMASC() && !step.getEntity().hasWorkingMisc(MiscType.F_JET_BOOSTER)) {
            if (step.isUsingSupercharger()) {
                rollsStringBuf.append(' ');
            }
            rollsStringBuf.append('M');
            rollsStringBuf.append(step.getTargetNumberMASC());
            rollsStringBuf.append('+');
        }

        if (rollsStringBuf.length() != 0) {
            // draw it below the main string.
            String rollsString = rollsStringBuf.toString();
            Font smallFont = getMovementFont().deriveFont(getMovementFont().getSize() * 0.5f);
            graph.setFont(smallFont);
            int rollsX = stepPos.x + 42;
            if (shiftFlag) {
                rollsX -= (graph.getFontMetrics(graph.getFont()).stringWidth(rollsString) / 2);
            }
            graph.setColor(Color.darkGray);
            graph.drawString(rollsString, rollsX, stepPos.y + 39 + rollY);
            graph.setColor(col);
            graph.drawString(rollsString, rollsX - 1, stepPos.y + 38 + rollY);
        }
    }
}
