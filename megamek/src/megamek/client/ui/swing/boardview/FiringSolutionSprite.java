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
    
    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;
    private static final Color TEXT_COLOR = new Color(40,255,255,230);
    private static final Color OUTLINE_COLOR = new Color(40, 40,40,200);

    private static final int TOHITMOD_SIZE = 25;
    private static final Point TOHITMOD_AT = new Point(HEX_CENTER_X, 18);

    private static final int RANGE_SIZE = 20;
    private static final Point RANGE_AT = new Point(HEX_CENTER_X + 11, 40);

    private static final int X_SIZE = 40;
    private static final Color X_COLOR = new Color(255, 40, 40, 230);

    private static final Color HEX_ICON_COLOR = new Color(80, 80, 80, 140);
    private static final Stroke HEX_ICON_STROKE = new BasicStroke(1.5f);

    private static final Color INDIRECT_DASH_COLOR_1 = new Color(255,  0, 0, 140);
    private static final Color INDIRECT_DASH_COLOR_2 = new Color(255, 255, 0, 140);
    private static final float[] DASH_PERIOD = { 10.0f };
    private static final BasicStroke INDIRECT_STROKE_1 = new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10.0f, DASH_PERIOD, 0.0f);
    private static final BasicStroke INDIRECT_STROKE_2 = new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10.0f, DASH_PERIOD, 10.0f);

    private final FiringSolution firingSolution;
    private final boolean noHitPossible;
    private final Shape rangeHexPolygon;
    private final StringDrawer xWriter = new StringDrawer("X").at(HEX_CENTER_X, HEX_CENTER_Y).color(X_COLOR)
            .fontSize(X_SIZE).center().outline(OUTLINE_COLOR, 1.5f);
    private final StringDrawer toHitModWriter;
    private final StringDrawer rangeWriter;

    public FiringSolutionSprite(BoardView boardView1, final FiringSolution firingSolution) {
        super(boardView1, firingSolution.getToHitData().getLocation());
        this.firingSolution = firingSolution;

        // to-hit modifier
        int toHitModifier = firingSolution.getToHitData().getValue();
        String toHitMod = ((toHitModifier >= 0) ? "+" : "") + toHitModifier;
        noHitPossible = (toHitModifier == TargetRoll.IMPOSSIBLE) || (toHitModifier == TargetRoll.AUTOMATIC_FAIL);
        toHitModWriter = new StringDrawer(toHitMod).at(TOHITMOD_AT).color(TEXT_COLOR).fontSize(TOHITMOD_SIZE)
                .center().outline(OUTLINE_COLOR, 1.5f);

        // range
        int range = firingSolution.getToHitData().getRange();
        rangeWriter = new StringDrawer(Integer.toString(range)).at(RANGE_AT).color(TEXT_COLOR).fontSize(RANGE_SIZE)
                .center().outline(OUTLINE_COLOR, 1.2f);

        // small hex shape
        AffineTransform at = AffineTransform.getTranslateInstance(30, RANGE_AT.y);
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
        graph.scale(bv.scale, bv.scale);

        String fontName = GUIPreferences.getInstance().getString(GUIPreferences.ADVANCED_MOVE_FONT_TYPE);
        int fontStyle = GUIPreferences.getInstance().getInt(GUIPreferences.ADVANCED_MOVE_FONT_STYLE);
        graph.setFont(new Font(fontName, fontStyle, X_SIZE));

        if (noHitPossible) {
            xWriter.draw(graph);
        } else {
            toHitModWriter.draw(graph);
            rangeWriter.draw(graph);

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
}