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

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tileset.HexTileset;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.MovePath.MoveStepType;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

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
        Image tempImage = new BufferedImage(HexTileset.HEX_W, HexTileset.HEX_H,
                BufferedImage.TYPE_INT_ARGB);
        Graphics graph = tempImage.getGraphics();
        Graphics2D g2D = (Graphics2D) graph;

        UIUtil.setHighQualityRendering(graph);

        // fill with key color
        graph.setColor(new Color(0, 0, 0, 0));
        graph.fillRect(0, 0, HexTileset.HEX_W, HexTileset.HEX_H);

        // setup some variables
        Shape moveArrow = bv.movementPolys[step.getFacing()];
        Shape facingArrow = bv.facingPolys[step.getFacing()];

        boolean isLastLegalStep = isLastStep &&
                (step.getMovementType(true) != EntityMovementType.MOVE_ILLEGAL);

        boolean jumped = false;
        boolean isMASCOrSuperCharger = false;
        boolean isBackwards = false;

        EntityMovementType movementType = step.getMovementType(isLastStep);
        switch (movementType) {
            case MOVE_RUN:
            case MOVE_VTOL_RUN:
            case MOVE_OVER_THRUST:
                isMASCOrSuperCharger = (step.isUsingMASC() || step.isUsingSupercharger());
                break;
            case MOVE_JUMP:
                jumped = true;
                break;
            default:
                if ((step.getType() == MoveStepType.BACKWARDS)
                        || (step.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS)
                        || (step.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS)) {
                    isBackwards = true;
                }
                break;
        }

        Color col = GUIP.getColorForMovement(movementType, isMASCOrSuperCharger, isBackwards);

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
                break;
            case GET_UP:
            case UP:
            case CAREFUL_STAND:
                // draw arrow indicating standing up
                currentArrow = upDownOffset.createTransformedShape(bv.upArrow);
                drawArrowShape(g2D, currentArrow, col);
                drawMovementCost(step, isLastStep, new Point(0, 15), graph, col, false);
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
            case PICKUP_CARGO:
            	String pickup = Messages.getString("MovementDisplay.movePickupCargo");
            	drawAnnouncement(g2D, pickup, step, col);
            	break;
            case DROP_CARGO:
            	String dropCargo = Messages.getString("MovementDisplay.moveDropCargo");
            	drawAnnouncement(g2D, dropCargo, step, col);
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
                graph.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));
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

        if (isLastLegalStep) {
            drawTMMAndRolls(step, jumped, bv.game, new Point(0, 0), graph, col, true);
        }

        baseScaleImage = bv.getPanel().createImage(tempImage.getSource());
        image = bv.getScaledImage(bv.getPanel().createImage(tempImage.getSource()), false);

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
        graph.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));
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
            graph.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));
            int evadeX = 42 - (graph.getFontMetrics(graph.getFont()).stringWidth(evade) / 2);
            graph.setColor(Color.darkGray);
            graph.drawString(evade, evadeX, 64);
            graph.setColor(col);
            graph.drawString(evade, evadeX - 1, 63);
        }

        if (step.isRolled()) {
            // Announce roll
            String roll = Messages.getString("BoardView1.Roll");
            graph.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));
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
            graph.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));
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
        String fontName = GUIP.getMoveFontType();
        int fontStyle = GUIP.getMoveFontStyle();
        int fontSize = GUIP.getMoveFontSize();
        return new Font(fontName, fontStyle, fontSize);
    }

    private void drawMovementCost(MoveStep step, boolean isLastStep,
                                  Point stepPos, Graphics graph, Color col, boolean shiftFlag) {
        StringBuilder costStringBuf = new StringBuilder();
        costStringBuf.append(step.getMpUsed());

        Entity e = step.getEntity();

        // If the step is using a road bonus, mark it.
        if (step.isOnlyPavementOrRoad() && e.isEligibleForPavementOrRoadBonus()) {
            costStringBuf.append('+');
        }

        // Show WiGE descent bonus
        for (int i = 0; i < step.getWiGEBonus(); i++) {
            costStringBuf.append('+');
        }

        // If the step is dangerous, mark it.
        if (step.isDanger()) {
            costStringBuf.append('*');
        }

        // If the step is past danger, mark that.
        if (step.isPastDanger()) {
            costStringBuf.insert(0, '(');
            costStringBuf.append(')');
        }

        EntityMovementType moveType = step.getMovementType(isLastStep);
        if ((moveType == EntityMovementType.MOVE_VTOL_WALK)
                || (moveType == EntityMovementType.MOVE_VTOL_RUN)
                || (moveType == EntityMovementType.MOVE_VTOL_SPRINT)
                || (moveType == EntityMovementType.MOVE_SUBMARINE_WALK)
                || (moveType == EntityMovementType.MOVE_SUBMARINE_RUN)) {
            costStringBuf.append('{').append(step.getElevation()).append('}');
        }

        if (step.getAltitude() > 0) {
            costStringBuf.append('{').append(step.getAltitude()).append('}');
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
    }

    private void drawTMMAndRolls(MoveStep step, boolean jumped, Game game,
                                 Point stepPos, Graphics graph, Color col, boolean shiftFlag) {

        StringBuilder subscriptStringBuf = new StringBuilder();

        int distance = step.getDistance();
        boolean airborneNonAerospace = (step.getMovementType(isLastStep) == EntityMovementType.MOVE_VTOL_RUN)
            || (step.getMovementType(isLastStep) == EntityMovementType.MOVE_VTOL_WALK)
            || ((step.getMovementMode() == EntityMovementMode.VTOL)
                && ( ((step.getMovementType(isLastStep) != EntityMovementType.MOVE_NONE)  ||  step.getEntity().isAirborneVTOLorWIGE()))
            || (step.getMovementType(isLastStep) == EntityMovementType.MOVE_VTOL_SPRINT));

        ToHitData toHitData = Compute.getTargetMovementModifier(distance, jumped, airborneNonAerospace, game);
        subscriptStringBuf.append((toHitData.getValue() < 0) ? '-' : '+');
        subscriptStringBuf.append(toHitData.getValue());

        if (step.isUsingSupercharger() && !step.getEntity().hasWorkingMisc(MiscType.F_JET_BOOSTER)) {
            subscriptStringBuf.append(" S");
            subscriptStringBuf.append(step.getTargetNumberSupercharger());
            subscriptStringBuf.append('+');
        }

        if (step.isUsingMASC() && !step.getEntity().hasWorkingMisc(MiscType.F_JET_BOOSTER)) {
            if (step.isUsingSupercharger()) {
                subscriptStringBuf.append(" M");
            } else {
                subscriptStringBuf.append(" M");
            }
            subscriptStringBuf.append(step.getTargetNumberMASC());
            subscriptStringBuf.append('+');
        }

        if (subscriptStringBuf.length() != 0) {
            // draw it below the main string.
            int subscriptY = stepPos.y + 39 + (graph.getFontMetrics(graph.getFont()).getHeight() / 2);
            String subscriptString = subscriptStringBuf.toString();
            Font subscriptFont = getMovementFont().deriveFont(getMovementFont().getSize() * 0.5f);
            graph.setFont(subscriptFont);
            int subscriptX = stepPos.x + 42;
            if (shiftFlag) {
                subscriptX -= (graph.getFontMetrics(graph.getFont()).stringWidth(subscriptString) / 2);
            }
            graph.setColor(Color.darkGray);
            graph.drawString(subscriptString, subscriptX, subscriptY);
            graph.setColor(col);
            graph.drawString(subscriptString, subscriptX - 1, subscriptY - 1);
        }

    }
}
