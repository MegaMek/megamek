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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;
import static megamek.client.ui.swing.boardview.HexDrawUtilities.*;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;

/**
 * This sprite is used to paint the visual and sensor range
 * 
 * <BR><BR>Extends {@link FieldofFireSprite}
 */
public class SensorRangeSprite extends FieldofFireSprite {
    public final static int SENSORS = 0;
    public final static int SENSORS_AIR = 1;
    public final static int VISUAL = 2;
    public final static int VISUAL_DARK = 3;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private static final int COLORS_MAX = 4;

    // in this sprite type, the images are very repetitive
    // therefore they get saved in a static array
    // they will be painted only once for each border
    // arrangement and color and repainted only when
    // the board is zoomed
    private static Image[][] images = new Image[64][COLORS_MAX];
    
    public SensorRangeSprite(BoardView boardView1, int sensorType, Coords l,
                             int borders) {
        // the color of the super doesn't matter
        super(boardView1, sensorType, l, borders);
        Color c = getColor(sensorType);
        setFillColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), getBorderOpac()));

    }

    public static Color getColor(int sensorType) {
        switch (sensorType) {
            case SENSORS:
                return GUIP.getSensorRangeColor();
            case SENSORS_AIR:
                return GUIP.getSensorRangeColor();
            case VISUAL:
                return GUIP.getVisualRangeColor();
            case VISUAL_DARK:
                return GUIP.getVisualRangeColor();
            default:
                return new Color(0,0,0);
        }
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();

        // when the zoom hasn't changed and there is already
        // a prepared image for these borders, then do nothing more
        if ((bv.scale == getOldZoom()) && isReady()) {
            return;
        }

        // when the board is rezoomed, ditch all images
        if (bv.scale != getOldZoom()) {
            setOldZoom(bv.scale);
            images = new Image[64][COLORS_MAX];
        }

        // create image for buffer
        images[borders][getRangeBracket()] = createNewHexImage();
        Graphics2D graph = (Graphics2D) images[borders][getRangeBracket()].getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        graph.setStroke(getLineStroke());

        int [] bTypes = getBTypes();
        int [] bDir = getBDir();

        // this will take the right way to paint the borders
        // from the static arrays; depends on the exact
        // borders that are present
        switch (bTypes[borders]) {
            case 1: // 2 adjacent borders
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], getBorderW()),
                        getHexCrossLine01(bDir[borders], getBorderW()));
                break;
            case 2: // 3 adjacent borders
                drawBorderXC(graph, getHexCrossArea012(bDir[borders], getBorderW()),
                        getHexCrossLine012(bDir[borders], getBorderW()));
                break;
            case 3: // 4 adjacent borders
                drawBorderXC(graph, getHexCrossArea0123(bDir[borders], getBorderW()),
                        getHexCrossLine0123(bDir[borders], getBorderW()));
                break;
            case 4: // twice two adjacent borders
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], getBorderW()),
                        getHexCrossLine01(bDir[borders], getBorderW()));
                drawBorderXC(graph, getHexCrossArea01(bDir[borders]+3, getBorderW()),
                        getHexCrossLine01(bDir[borders]+3, getBorderW()));
                break;
            case 5: // three adjacent borders and one lone
                drawBorderXC(graph, getHexCrossArea012(bDir[borders], getBorderW()),
                        getHexCrossLine012(bDir[borders], getBorderW()));
                drawLoneBorder(graph, bDir[borders] + 4);
                break;
            case 6: // two adjacent borders and one lone
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], getBorderW()),
                        getHexCrossLine01(bDir[borders], getBorderW()));
                drawLoneBorder(graph, bDir[borders] + 3);
                break;
            case 7: // two adjacent borders and one lone (other hexface)
                drawBorderXC(graph, getHexCrossArea01(bDir[borders], getBorderW()),
                        getHexCrossLine01(bDir[borders], getBorderW()));
                drawLoneBorder(graph, bDir[borders] + 4);
                break;
            case 8:
                drawBorderXC(graph, getHexCrossArea01234(bDir[borders], getBorderW()),
                        getHexCrossLine01234(bDir[borders], getBorderW()));
                break;
            default:
                drawNormalBorders(graph);
        }

        graph.dispose();
    }

    @Override
    public boolean isReady() {
        return (bv.scale == getOldZoom()) && (images[borders][getRangeBracket()] != null);
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer, boolean makeTranslucent) {
        if (isReady()) {
            if (makeTranslucent) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.drawImage(images[borders][getRangeBracket()], x, y, observer);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            } else {
                g.drawImage(images[borders][getRangeBracket()], x, y, observer);
            }
        } else {
            prepare();
        }
    }
}
