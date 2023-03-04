/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.common.TargetRoll;
import megamek.common.util.FiringSolution;

/**
 * Sprite for displaying generic firing information. This is used for
 * the firing phase and displays either range and target modifier or
 * a big red X if the target cannot be hit.
 */
class FiringSolutionSprite extends HexSprite {
    
    private static final int FONT_SIZE_SMALL = 25;
    private static final int FONT_SIZE_RANGE = 20;
    private static final int FONT_SIZE_LARGE = 40;
    private static final Color TEXT_COLOR = Color.CYAN;
    private static final Color X_COLOR = new Color(255, 40, 40, 140);

    private static final Color HEX_ICON_COLOR = new Color(80, 80, 80, 140);
    private static final Stroke HEX_ICON_STROKE = new BasicStroke(1.5f);

    private static final Color INDIRECT_DASH_COLOR_1 = new Color(255,  0, 0, 140);
    private static final Color INDIRECT_DASH_COLOR_2 = new Color(255, 255, 0, 140);
    private static final float[] DASH_PERIOD = { 10.0f };
    private static final BasicStroke INDIRECT_STROKE_1 = new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 10.0f, DASH_PERIOD, 0.0f);
    private static final BasicStroke INDIRECT_STROKE_2 = new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND, 10.0f, DASH_PERIOD, 10.0f);

    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;
    private static final Point HEX_CENTER = new Point(HEX_CENTER_X, HEX_CENTER_Y);
    private static final Point FIRST_LINE = new Point(HEX_CENTER_X - 2, BoardView.HEX_H / 4 + 2);
    private static final Point SECOND_LINE = new Point(HEX_CENTER_X + 9, BoardView.HEX_H * 3 / 4 - 2);

    // sprite object data
    private final FiringSolution firingSolution;
    private final String range;
    private final String toHitMod;
    private final boolean noHitPossible;
    private final Shape finalHex;
    private final StringDrawer bigXDrawer = new StringDrawer("X").at(HEX_CENTER).color(X_COLOR)
            .outline(Color.BLACK, 1.5f).center();

    public FiringSolutionSprite(BoardView boardView1, final FiringSolution firingSolution) {
        super(boardView1, firingSolution.getToHitData().getLocation());
        updateBounds();
        this.firingSolution = firingSolution;
        int toHitModifier = firingSolution.getToHitData().getValue();
        toHitMod = ((toHitModifier >= 0) ? "+" : "") + toHitModifier;
        noHitPossible = (toHitModifier == TargetRoll.IMPOSSIBLE) || (toHitModifier == TargetRoll.AUTOMATIC_FAIL);
        
        // range
        int range = firingSolution.getToHitData().getRange();
        this.range = Integer.toString(range);

        // create the small hex shape
        AffineTransform at = AffineTransform.getTranslateInstance((range > 9) ? 25 : 30, SECOND_LINE.y + 2);
        at.scale(0.17, 0.17);
        at.translate(-HEX_CENTER_X, -HEX_CENTER_Y);
        finalHex = at.createTransformedShape(BoardView.hexPoly);
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        GUIPreferences.AntiAliasifSet(graph);
        graph.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graph.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        graph.scale(bv.scale, bv.scale);
        
        String fontName = GUIPreferences.getInstance().getString(GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
        int fontStyle = GUIPreferences.getInstance().getInt(GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
        
        if (noHitPossible) {  
            graph.setFont(new Font(fontName, fontStyle, FONT_SIZE_LARGE));
            bigXDrawer.draw(graph);
        } else {
            // hittable: write modifier and range
            Font textFont = new Font(fontName, fontStyle, FONT_SIZE_SMALL);
            bv.drawTextShadow(graph, toHitMod, FIRST_LINE, textFont);
            BoardView.drawCenteredText(graph, toHitMod, FIRST_LINE, TEXT_COLOR, false, textFont);

            Font rangeFont = new Font(fontName, fontStyle, FONT_SIZE_RANGE);
            bv.drawTextShadow(graph, range, SECOND_LINE, rangeFont);
            BoardView.drawCenteredText(graph, range, SECOND_LINE, TEXT_COLOR, false, rangeFont);

            // Draw a small hex shape to indicate range
            graph.setStroke(HEX_ICON_STROKE);
            graph.setColor(HEX_ICON_COLOR);
            graph.fill(finalHex);
            graph.setColor(TEXT_COLOR);
            graph.draw(finalHex);
        }
        
        if (firingSolution.isTargetSpotted()) {
            graph.setColor(INDIRECT_DASH_COLOR_1);
            graph.setStroke(INDIRECT_STROKE_1);
            graph.draw(BoardView.hexPoly);
            graph.setColor(INDIRECT_DASH_COLOR_2);
            graph.setStroke(INDIRECT_STROKE_2);
            graph.draw(BoardView.hexPoly);
        }

        graph.dispose();
    }
}