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
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.TargetRoll;
import megamek.common.util.FiringSolution;

/**
 * Sprite for displaying generic firing information. This is used for
 * the firing phase and displays either range and target modifier or
 * a big red X if the target cannot be hit.
 */
class FiringSolutionSprite extends HexSprite {
    
    private static final int FONT_SIZE_TOHITMOD = 25;
    private static final int FONT_SIZE_RANGE = 20;
    private static final Color TEXT_COLOR = new Color(40,255,255,200);
    private static final Color OUTLINE_COLOR = new Color(40, 40,40,200);
    private static final int FONT_SIZE_X = 40;
    private static final Color COLOR_X = new Color(255, 40, 40, 140);
    private static final Color HEX_ICON_COLOR = new Color(80, 80, 80, 140);
    private static final Stroke HEX_ICON_STROKE = new BasicStroke(1.5f);

    private static final Color INDIRECT_DASH_COLOR_1 = new Color(255,  0, 0, 140);
    private static final Color INDIRECT_DASH_COLOR_2 = new Color(255, 255, 0, 140);
    private static final float[] DASH_PERIOD = { 10.0f };
    private static final BasicStroke INDIRECT_STROKE_1 = new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10.0f, DASH_PERIOD, 0.0f);
    private static final BasicStroke INDIRECT_STROKE_2 = new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10.0f, DASH_PERIOD, 10.0f);

    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;
    private static final Point HEX_CENTER = new Point(HEX_CENTER_X, HEX_CENTER_Y);
    private static final Point FIRST_LINE = new Point(HEX_CENTER_X, HEX_CENTER_Y / 2);
    private static final Point SECOND_LINE = new Point(HEX_CENTER_X + 9, BoardView.HEX_H * 3 / 4);

    // sprite object data
    private final FiringSolution firingSolution;
    private final String range;
    private final String toHitMod;
    private final boolean noHitPossible;
    private final Shape rangeHexPolygon;

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
        AffineTransform at = AffineTransform.getTranslateInstance((range > 9) ? 25 : 30, SECOND_LINE.y);
        at.scale(0.17, 0.17);
        at.translate(-HEX_CENTER_X, -HEX_CENTER_Y);
        rangeHexPolygon = at.createTransformedShape(BoardView.hexPoly);
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        
        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        String fontName = GUIPreferences.getInstance().getString(GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
        int fontStyle = GUIPreferences.getInstance().getInt(GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
        graph.setFont(new Font(fontName, fontStyle, FONT_SIZE_X));

        if (noHitPossible) {
            new StringDrawer("X").at(HEX_CENTER).color(COLOR_X).fontSize(scaledFontSize(FONT_SIZE_X))
                    .center().outline(OUTLINE_COLOR, 1.5f).draw(graph);
        } else {
            new StringDrawer(toHitMod).at(FIRST_LINE).color(TEXT_COLOR).fontSize(scaledFontSize(FONT_SIZE_TOHITMOD))
                    .center().outline(OUTLINE_COLOR, 1.5f).draw(graph);

            new StringDrawer(range).at(SECOND_LINE).color(TEXT_COLOR).fontSize(FONT_SIZE_RANGE)
                    .center().outline(OUTLINE_COLOR, 1.2f).draw(graph);

            // Draw a small hex shape to indicate range
            graph.setStroke(HEX_ICON_STROKE);
            graph.setColor(HEX_ICON_COLOR);
            graph.fill(rangeHexPolygon);
            graph.setColor(TEXT_COLOR);
            graph.draw(rangeHexPolygon);
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

    /** Divides the given font size by the boardview scale if that is > 1 to stop text growth when zooming in. */
    private float scaledFontSize(float originalSize) {
        return bv.scale > 1 ? originalSize / bv.scale : originalSize;
    }
}