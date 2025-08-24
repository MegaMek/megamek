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

import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossArea01;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossArea012;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossArea0123;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossArea01234;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossLine01;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossLine012;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossLine0123;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexCrossLine01234;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * This sprite is used to paint the visual and sensor range
 *
 * <BR><BR>Extends {@link FieldOfFireSprite}
 */
public class SensorRangeSprite extends FieldOfFireSprite {
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
        setFillColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), getBorderOpacity()));

    }

    public static Color getColor(int sensorType) {
        return switch (sensorType) {
            case SENSORS, SENSORS_AIR -> GUIP.getSensorRangeColor();
            case VISUAL, VISUAL_DARK -> GUIP.getVisualRangeColor();
            default -> new Color(0, 0, 0);
        };
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();

        // when the zoom hasn't changed and there is already
        // a prepared image for these borders, then do nothing more
        if ((bv.getScale() == getOldZoom()) && isReady()) {
            return;
        }

        // when the board is rezoomed, ditch all images
        if (bv.getScale() != getOldZoom()) {
            setOldZoom(bv.getScale());
            images = new Image[64][COLORS_MAX];
        }

        // create image for buffer
        images[borders][getRangeBracket()] = createNewHexImage();
        Graphics2D graph = (Graphics2D) images[borders][getRangeBracket()].getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.getScale(), bv.getScale());

        graph.setStroke(getLineStroke());

        int[] bTypes = getBTypes();
        int[] bDir = getBDir();

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
                drawBorderXC(graph, getHexCrossArea01(bDir[borders] + 3, getBorderW()),
                      getHexCrossLine01(bDir[borders] + 3, getBorderW()));
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
        return (bv.getScale() == getOldZoom()) && (images[borders][getRangeBracket()] != null);
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
