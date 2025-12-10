/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.*;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.ImageObserver;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;
import megamek.common.RangeType;

/**
 * This sprite is used to paint the field of fire for weapons.
 *
 * <BR>
 * <BR>
 * Extends {@link MovementEnvelopeSprite}
 *
 * @author Simon
 */
public class FieldOfFireSprite extends MovementEnvelopeSprite {
    // ### Control values

    // thick border
    private static final int borderWidth = 10;
    private static final int borderOpacity = 120;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    // thin line
    private static final float lineThickness = 1.4f;
    private static final Color lineColor = Color.WHITE;
    private static final Stroke lineStroke = new BasicStroke(lineThickness, BasicStroke.CAP_BUTT,
          BasicStroke.JOIN_MITER, 10f, new float[] { 2f, 2f }, 0f);
    // ### -------------

    // the fields control when and how borders are drawn
    // across a hex instead of along its borders
    private static final int[] bDir = {
          0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 2, 2, 1, 0, 0, 0, 0, 0,
          0, 0, 1, 0, 3, 3, 3, 0, 2, 2, 1, 0, 2, 5, 4, 5, 6, 5, 1, 5,
          0, 5, 2, 5, 2, 2, 1, 5, 4, 4, 4, 4, 4, 4, 1, 4, 3, 3, 3, 3, 2, 2, 1, 0
    };
    private static final int[] bTypes = {
          0, 0, 0, 1, 0, 0, 1, 2, 0, 0, 0, 6, 1, 7, 2, 3, 0, 0, 0, 7,
          0, 1, 6, 5, 1, 6, 7, 4, 2, 5, 3, 8, 0, 1, 4, 2, 6, 6, 7, 3,
          0, 7, 2, 5, 6, 4, 5, 8, 1, 2, 6, 3, 7, 5, 4, 8, 2, 3, 5, 8, 3, 8, 8, 0
    };

    private static final int COLORS_MAX = 5;

    // in this sprite type, the images are very repetitive
    // therefore they get saved in a static array
    // they will be painted only once for each border
    // arrangement and color and repainted only when
    // the board is zoomed
    private static Image[][] images = new Image[64][COLORS_MAX];
    private float oldZoom;

    // individual sprite values
    private Color fillColor;
    private final int rangeBracket;

    public FieldOfFireSprite(BoardView boardView1, int rangeBracket, Coords l,
          int borders) {
        // the color of the super doesn't matter
        super(boardView1, Color.BLACK, l, borders);
        Color c = getFieldOfFireColor(rangeBracket);
        fillColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), borderOpacity);
        this.rangeBracket = rangeBracket;
    }

    public static Color getFieldOfFireColor(int rangeBracket) {
        // colors for Min,S,M,L,E ranges
        return switch (rangeBracket) {
            case RangeType.RANGE_MINIMUM -> GUIP.getFieldOfFireMinColor();
            case RangeType.RANGE_SHORT -> GUIP.getFieldOfFireShortColor();
            case RangeType.RANGE_MEDIUM -> GUIP.getFieldOfFireMediumColor();
            case RangeType.RANGE_LONG -> GUIP.getFieldOfFireLongColor();
            case RangeType.RANGE_EXTREME -> GUIP.getFieldOfFireExtremeColor();
            default -> new Color(0, 0, 0);
        };
    }

    protected void setFillColor(Color c) {
        fillColor = c;
    }

    protected int getBorderOpacity() {
        return borderOpacity;
    }

    protected float getOldZoom() {
        return oldZoom;
    }

    protected void setOldZoom(float f) {
        oldZoom = f;
    }

    protected int getRangeBracket() {
        return rangeBracket;
    }

    protected Stroke getLineStroke() {
        return lineStroke;
    }

    protected int[] getBTypes() {
        return bTypes;
    }

    protected int[] getBDir() {
        return bDir;
    }

    protected int getBorderW() {
        return borderWidth;
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();

        // when the zoom hasn't changed and there is already
        // a prepared image for these borders, then do nothing more
        if ((bv.getScale() == oldZoom) && isReady()) {
            return;
        }

        // when the board is re-zoomed, ditch all images
        if (bv.getScale() != oldZoom) {
            oldZoom = bv.getScale();
            images = new Image[64][COLORS_MAX];
        }

        // create image for buffer
        images[borders][rangeBracket] = createNewHexImage();
        Graphics2D graph = (Graphics2D) images[borders][rangeBracket].getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.getScale(), bv.getScale());

        graph.setStroke(lineStroke);

        // this will take the right way to paint the borders
        // from the static arrays; depends on the exact
        // borders that are present
        switch (bTypes[borders]) {
            case 1: // 2 adjacent borders
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderWidth),
                      getHexCrossLine01(bDir[borders], borderWidth));
                break;
            case 2: // 3 adjacent borders
                drawBorderXC(graph, getHexCrossArea012(bDir[borders], borderWidth),
                      getHexCrossLine012(bDir[borders], borderWidth));
                break;
            case 3: // 4 adjacent borders
                drawBorderXC(graph, getHexCrossArea0123(bDir[borders], borderWidth),
                      getHexCrossLine0123(bDir[borders], borderWidth));
                break;
            case 4: // twice two adjacent borders
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderWidth),
                      getHexCrossLine01(bDir[borders], borderWidth));
                drawBorderXC(graph, getHexCrossArea01(bDir[borders] + 3, borderWidth),
                      getHexCrossLine01(bDir[borders] + 3, borderWidth));
                break;
            case 5: // three adjacent borders and one lone
                drawBorderXC(graph, getHexCrossArea012(bDir[borders], borderWidth),
                      getHexCrossLine012(bDir[borders], borderWidth));
                drawLoneBorder(graph, bDir[borders] + 4);
                break;
            case 6: // two adjacent borders and one lone
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderWidth),
                      getHexCrossLine01(bDir[borders], borderWidth));
                drawLoneBorder(graph, bDir[borders] + 3);
                break;
            case 7: // two adjacent borders and one lone (other hex face)
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], borderWidth),
                      getHexCrossLine01(bDir[borders], borderWidth));
                drawLoneBorder(graph, bDir[borders] + 4);
                break;
            case 8:
                drawBorderXC(graph, getHexCrossArea01234(bDir[borders], borderWidth),
                      getHexCrossLine01234(bDir[borders], borderWidth));
                break;
            default:
                drawNormalBorders(graph);
        }

        graph.dispose();
    }

    protected void drawBorderXC(Graphics2D graph, Shape fillShape, Shape lineShape) {
        // 1) thick transparent border
        graph.setColor(fillColor);
        graph.fill(fillShape);

        // 2) thin dashed line border
        graph.setColor(lineColor);
        graph.draw(lineShape);
    }

    protected void drawLoneBorder(Graphics2D graph, int dir) {
        // 1) thick transparent border
        graph.setColor(fillColor);
        graph.fill(getHexBorderArea(dir, CUT_BORDER, borderWidth));

        // 2) thin dashed line border
        graph.setColor(lineColor);
        graph.draw(getHexBorderLine(dir));
    }

    protected void drawNormalBorders(Graphics2D graph) {
        // cycle through directions
        for (int i = 0; i < 6; i++) {
            if ((borders & (1 << i)) != 0) {
                // 1) thick transparent border
                int cut = ((borders & (1 << ((i + 1) % 6))) == 0) ? CUT_RIGHT_BORDER : CUT_RIGHT_INSIDE;
                cut |= ((borders & (1 << ((i + 5) % 6))) == 0) ? CUT_LEFT_BORDER : CUT_LEFT_INSIDE;

                graph.setColor(fillColor);
                graph.fill(getHexBorderArea(i, cut, borderWidth));

                // 2) thin dashed line border
                graph.setColor(lineColor);
                graph.draw(getHexBorderLine(i, cut, lineThickness / 2));
            }
        }
    }

    @Override
    public boolean isReady() {
        return (bv.getScale() == oldZoom) && (images[borders][rangeBracket] != null);
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer, boolean makeTranslucent) {
        if (isReady()) {
            if (makeTranslucent) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.drawImage(images[borders][rangeBracket], x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(images[borders][rangeBracket], x, y, observer);
            }
        } else {
            prepare();
        }
    }
}
