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
package megamek.client.ui.clientGUI.boardview.sprite.sbf;

import java.awt.*;
import java.awt.geom.AffineTransform;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.UIUtil;
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

    private final Image baseScaleImage;

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

        // fill with key color
        graph.setColor(new Color(0, 0, 0, 0));
        graph.fillRect(0, 0, HexTileset.HEX_W, HexTileset.HEX_H);

        // setup some variables
        Shape moveArrow = bv.getMovementPolys()[step.getMovementDirection()];

        Color col = isIllegal ? Color.GRAY : Color.GREEN;

        // forward movement arrow
        drawArrowShape(graph, moveArrow, col);
        drawMovementCost(isLastStep, new Point(0, 0), graph, col, true);

        graph.dispose();
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

    private Font getMovementFont() {
        String fontName = GUIP.getMoveFontType();
        int fontStyle = GUIP.getMoveFontStyle();
        int fontSize = GUIP.getMoveFontSize();
        return new Font(fontName, fontStyle, fontSize);
    }

    private void drawMovementCost(boolean isLastStep, Point stepPos, Graphics graph, Color col, boolean shiftFlag) {

        // Convert the buffer to a String and draw it.
        String costString = String.valueOf(totalMp);
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

    @Override
    protected int getSpritePriority() {
        return 20;
    }
}
