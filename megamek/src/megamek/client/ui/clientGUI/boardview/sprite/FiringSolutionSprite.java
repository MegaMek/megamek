/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.rolls.TargetRoll;
import megamek.common.util.FiringSolution;

/**
 * Sprite for displaying generic firing information. This is used for the firing phase and displays either range and
 * target modifier or a big red X if the target cannot be hit.
 */
public class FiringSolutionSprite extends HexSprite {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int HEX_CENTER_Y = HexTileset.HEX_H / 2;
    private static final Color TEXT_COLOR = new Color(40, 255, 255, 230);
    private static final Color OUTLINE_COLOR = new Color(40, 40, 40, 200);

    private static final int TO_HIT_MOD_SIZE = 25;
    private static final Point TO_HIT_MOD_AT = new Point(HEX_CENTER_X, 18);

    private static final int RANGE_SIZE = 20;
    private static final Point RANGE_AT = new Point(HEX_CENTER_X + 11, 40);

    private static final int X_SIZE = 40;
    private static final Color X_COLOR = new Color(255, 40, 40, 230);

    private static final Color HEX_ICON_COLOR = new Color(80, 80, 80, 140);
    private static final Stroke HEX_ICON_STROKE = new BasicStroke(1.5f);

    private static final Color INDIRECT_DASH_COLOR_1 = new Color(255, 0, 0, 140);
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
        toHitModWriter = new StringDrawer(toHitMod).at(TO_HIT_MOD_AT).color(TEXT_COLOR).fontSize(TO_HIT_MOD_SIZE)
              .center().outline(OUTLINE_COLOR, 1.5f);

        // range
        int range = firingSolution.getToHitData().getRange();
        rangeWriter = new StringDrawer(Integer.toString(range)).at(RANGE_AT).color(TEXT_COLOR).fontSize(RANGE_SIZE)
              .center().outline(OUTLINE_COLOR, 1.2f);

        // small hex shape
        AffineTransform at = AffineTransform.getTranslateInstance(30, RANGE_AT.y);
        at.scale(0.17, 0.17);
        at.translate(-HEX_CENTER_X, -HEX_CENTER_Y);
        rangeHexPolygon = at.createTransformedShape(BoardView.getHexPoly());
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());

        String fontName = GUIP.getMoveFontType();
        int fontStyle = GUIP.getMoveFontStyle();
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
            graph.draw(BoardView.getHexPoly());
            graph.setColor(INDIRECT_DASH_COLOR_2);
            graph.setStroke(INDIRECT_STROKE_2);
            graph.draw(BoardView.getHexPoly());
        }

        graph.dispose();
    }

    @Override
    public boolean isBehindTerrain() {
        return false;
    }

    @Override
    protected int getSpritePriority() {
        return 80;
    }
}
