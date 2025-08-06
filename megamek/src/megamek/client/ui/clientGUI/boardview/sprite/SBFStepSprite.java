/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.*;
import java.awt.geom.AffineTransform;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.MiscType;
import megamek.common.ToHitData;
import megamek.common.moves.MoveStep;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFMoveStep;

public class SBFStepSprite extends Sprite {

    private final static GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final AffineTransform shadowOffset = new AffineTransform();
    private static final AffineTransform upDownOffset = new AffineTransform();
    private static final AffineTransform stepOffset = new AffineTransform();

    static {
        shadowOffset.translate(-1, -1);
        upDownOffset.translate(-30, 0);
        stepOffset.translate(1, 1);
    }

    private final SBFMoveStep step;
    private final boolean isLastStep;
    private final int totalMp;
    private final boolean isIllegal;

    private Image baseScaleImage;

    public SBFStepSprite(BoardView bv, final SBFMoveStep step, SBFMovePath movePath) {
        super(bv);
        this.step = step;
        isLastStep = movePath.isEndStep(step);
        totalMp = movePath.getMpUpTo(step);
        isIllegal = step.isIllegal() || movePath.isIllegal();

        // step is the size of the hex that this step is in
        bounds = new Rectangle(this.bv.getHexLocation(step.getDestination().coords()), this.bv.getHexSize());
        image = null;
        baseScaleImage = null;
    }

    /**
     * Refreshes this StepSprite's image to handle changes in the zoom level.
     */
    public void refreshZoomLevel() {
        if (baseScaleImage == null) {
            return;
        }

        image = bv.getScaledImage(baseScaleImage, false);
    }

    @Override
    public void prepare() {
        getBounds();

        // create image for buffer
        GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
              .getDefaultConfiguration();
        image = config.createCompatibleImage(bounds.width, bounds.height, Transparency.TRANSLUCENT);
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);

        graph.scale(bv.getScale(), bv.getScale());
        //        // create image for buffer
        //        Image tempImage = new BufferedImage(HexTileset.HEX_W, HexTileset.HEX_H,
        //                BufferedImage.TYPE_INT_ARGB);
        //        Graphics graph = tempImage.getGraphics();
        //        Graphics2D g2D = (Graphics2D) graph;
        //
        //        UIUtil.setHighQualityRendering(graph);

        // fill with key color
        graph.setColor(new Color(0, 0, 0, 0));
        graph.fillRect(0, 0, HexTileset.HEX_W, HexTileset.HEX_H);

        // setup some variables
        Shape moveArrow = bv.getMovementPolys()[step.getMovementDirection()];

        boolean isLastLegalStep = isLastStep && !step.isIllegal();

        Color col = isIllegal ? Color.GRAY : Color.GREEN;

        //        drawConditions(step, graph, col);
        //
        Shape currentArrow;
        // forward movement arrow
        drawArrowShape(graph, moveArrow, col);
        drawMovementCost(isLastStep, new Point(0, 0), graph, col, true);

        if (isLastLegalStep) {
            //            drawTMMAndRolls(step, jumped, bv.game, new Point(0, 0), graph, col, true);
        }

        //        baseScaleImage = bv.getPanel().createImage(tempImage.getSource());
        //        image = bv.getScaledImage(bv.getPanel().createImage(tempImage.getSource()), false);

        graph.dispose();
        //        tempImage.flush();
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(0, 0, bv.getHexSize().width, bv.getHexSize().height);
        Point ePos = bv.getHexLocation(step.getDestination().coords());
        bounds.setLocation(ePos.x, ePos.y);
        return bounds;
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
     * Draws conditions separate from the step. Allows keeping conditions on Aeros even when that step is erased
     * (advanced movement), such as evading, rolling, loading and unloading.
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

    private Font getMovementFont() {
        String fontName = GUIP.getMoveFontType();
        int fontStyle = GUIP.getMoveFontStyle();
        int fontSize = GUIP.getMoveFontSize();
        return new Font(fontName, fontStyle, fontSize);
    }

    private void drawMovementCost(boolean isLastStep,
          Point stepPos, Graphics graph, Color col, boolean shiftFlag) {
        StringBuilder costStringBuf = new StringBuilder();
        costStringBuf.append(totalMp);

        // If the step is using a road bonus, mark it.
        //        if (step.isOnlyPavement() && e.isEligibleForPavementBonus()) {
        //            costStringBuf.append('+');
        //        }

        // Show WiGE descent bonus
        //        for (int i = 0; i < step.getWiGEBonus(); i++) {
        //            costStringBuf.append('+');
        //        }

        // If the step is dangerous, mark it.
        //        if (step.isDanger()) {
        //            costStringBuf.append('*');
        //        }

        // If the step is past danger, mark that.
        //        if (step.isPastDanger()) {
        //            costStringBuf.insert(0, '(');
        //            costStringBuf.append(')');
        //        }

        //        EntityMovementType moveType = step.getMovementType(isLastStep);
        //        if ((moveType == EntityMovementType.MOVE_VTOL_WALK)
        //                || (moveType == EntityMovementType.MOVE_VTOL_RUN)
        //                || (moveType == EntityMovementType.MOVE_VTOL_SPRINT)
        //                || (moveType == EntityMovementType.MOVE_SUBMARINE_WALK)
        //                || (moveType == EntityMovementType.MOVE_SUBMARINE_RUN)) {
        //            costStringBuf.append('{').append(step.getElevation()).append('}');
        //        }
        //
        //        if (step.getAltitude() > 0) {
        //            costStringBuf.append('{').append(step.getAltitude()).append('}');
        //        }

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
        boolean airborneNonAerospace = false; //step.getEntity().;

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

    @Override
    protected int getSpritePriority() {
        return 20;
    }
}
