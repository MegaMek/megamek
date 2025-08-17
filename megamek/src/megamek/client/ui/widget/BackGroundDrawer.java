/*
 * Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.widget;

import java.awt.Graphics;
import java.awt.Image;

/**
 * Background drawer is a class which keeps reference to a single Image and draws it according specified rules. For
 * example, we can order to draw image by fully tiling it over all area, or tile it in a single row or column with
 * desired alignment, or draw it just once. Alignment of drawing can be
 * <ol>
 * <li>logical (top, bottom, center for vertical alignment and left, right,
 * center for horizontal one)</li>
 * <li>or given by exact number of pixels from top or left borders of area</li>
 * </ol>
 */
public class BackGroundDrawer {
    /**
     * Single copy of image will be drawn.
     */
    public static final int NO_TILING = 1;
    /**
     * Image will be tiled into single column over drawing area.
     */
    public static final int TILING_VERTICAL = 2;
    /**
     * Image will be tiled into single row over drawing area.
     */
    public static final int TILING_HORIZONTAL = 4;
    /**
     * Image will be tiled over all drawing area.
     */
    public static final int TILING_BOTH = 8;
    /**
     * Alignment to the top in case of Horizontal or single tiling.
     */
    public static final int V_ALIGN_TOP = 16;
    /**
     * Alignment to the centre in case of Horizontal or single tiling.
     */
    public static final int V_ALIGN_CENTER = 32;
    /**
     * Alignment to the bottom in case of Horizontal or single tiling.
     */
    public static final int V_ALIGN_BOTTOM = 64;
    /**
     * Shift down from top border of area by exact number of pixels. (default 0 pixels)
     */
    public static final int V_ALIGN_EXACT = 128;
    /**
     * Alignment to the left in case of Vertical or single tiling.
     */
    public static final int H_ALIGN_LEFT = 256;
    /**
     * Alignment to the center in case of Vertical or single tiling.
     */
    public static final int H_ALIGN_CENTER = 512;
    /**
     * Alignment to the right in case of Vertical or single tiling.
     */
    public static final int H_ALIGN_RIGHT = 1024;
    /**
     * Shift right from left border of area by exact number of pixels. (default 0 pixels)
     */
    public static final int H_ALIGN_EXACT = 2048;

    // Required bit masks to manipulate behavior variable.
    private static final int TILING_TYPE_MASK = 4080;
    private static final int V_ALIGN_MASK = 3855;
    private static final int H_ALIGN_MASK = 255;
    private static final int TILING_TYPE_SELECT_MASK = 15;
    private static final int V_ALIGN_SELECT_MASK = 240;
    private static final int H_ALIGN_SELECT_MASK = 3840;

    private Image mainImage;
    private int behavior = NO_TILING | V_ALIGN_CENTER | H_ALIGN_CENTER;
    private int fixedX = 0;
    private int fixedY = 0;

    /**
     * @param mainImage image to draw by BackGroundDrawer.
     * @param behavior  Integer value specifying way of tiling and alignment. For example:
     *                  <code> BackGroundDrawer(myImage, BackGroundDrawer.TILING_VERTICAL |
     *                  BackGroundDrawer.H_ALIGN_RIGHT);
     *                  </code>
     */

    public BackGroundDrawer(Image mainImage, int behavior) {
        this.mainImage = mainImage;
        this.behavior = behavior;
    }

    /**
     * Tiling style is set to NO_TILING | V_ALIGN_CENTER | H_ALIGN_CENTER.
     *
     * @param mainImage image to draw by BackGroundDrawer.
     */

    public BackGroundDrawer(Image mainImage) {
        this.mainImage = mainImage;
    }

    /**
     * Sets image to draw by BackGroundDrawer
     */

    public void setImage(Image mainImage) {
        this.mainImage = mainImage;
    }

    /**
     * Gets image to draw by BackGroundDrawer
     */
    public Image getImage() {
        return this.mainImage;
    }

    /**
     * Sets type of tiling.
     *
     * @param type Possible values: NO_TILING, TILING_BOTH, TILING_VERTICAL, TILING_HORIZONTAL.
     */
    public void setTilingType(int type) {
        type &= TILING_TYPE_SELECT_MASK;
        behavior &= TILING_TYPE_MASK;
        behavior |= type;
    }

    /**
     * Sets vertical align of tiling
     *
     * @param vAlign Must be V_ALIGN_TOP, V_ALIGN_CENTER, V_ALIGN_BOTTOM
     */

    public void setValign(int vAlign) {
        vAlign &= V_ALIGN_SELECT_MASK;
        behavior &= V_ALIGN_MASK;
        behavior |= vAlign;
    }

    /**
     * Sets horizontal align of tiling
     *
     * @param hAlign Must be H_ALIGN_LEFT, H_ALIGN_CENTER, H_ALIGN_RIGHT
     */
    public void setHAlign(int hAlign) {
        hAlign &= H_ALIGN_SELECT_MASK;
        behavior &= H_ALIGN_MASK;
        behavior |= hAlign;
    }

    /**
     * Sets exact vertical alignment exactly at "y" pixels
     */

    public void setValignExactAt(int y) {
        setValign(V_ALIGN_EXACT);
        fixedY = y;
    }

    /**
     * Sets exact horizontal alignment exactly at "x" pixels
     */

    public void setHAlignExactAt(int x) {
        setHAlign(H_ALIGN_EXACT);
        fixedX = x;
    }

    /**
     * Returns integer describing behavior of BackgroundDrawer
     */

    public int getBehavior() {
        return this.behavior;
    }

    /**
     * Draws image into Graphics with custom tiling type and alignment.
     *
     * @param g      Graphics to which draw to.
     * @param width  Width of the drawing area.
     * @param height Height of the drawing area.
     */

    public void drawInto(Graphics g, int width, int height) {
        if (mainImage == null) {
            return;
        }

        // Checking behavior of painter
        if ((behavior & NO_TILING) != 0) {
            drawNoTiling(g, width, height);
            return;
        }

        if ((behavior & TILING_BOTH) != 0) {
            drawTilingBoth(g, width, height);
            return;
        }

        if ((behavior & TILING_VERTICAL) != 0) {
            drawTilingVertical(g, width, height);
            return;
        }

        if ((behavior & TILING_HORIZONTAL) != 0) {
            drawTilingHorizontal(g, width, height);
        }
    }

    private void drawNoTiling(Graphics g, int width, int height) {
        int dx = getDX(width);
        int dy = getDY(height);
        g.drawImage(mainImage, dx, dy, null);
    }

    private void drawTilingBoth(Graphics g, int width, int height) {
        int tileWidth = mainImage.getWidth(null);
        int tileHeight = mainImage.getHeight(null);
        int countX = (width / tileWidth);
        if (width % tileWidth != 0) {
            countX++;
        }
        int countY = (height / tileHeight);
        if (height % tileHeight != 0) {
            countY++;
        }
        for (int i = 0; i < countX; i++) {
            for (int j = 0; j < countY; j++) {
                g.drawImage(mainImage, i * tileWidth, j * tileHeight, null);
            }
        }
    }

    private void drawTilingVertical(Graphics g, int width, int height) {
        int dx = getDX(width);
        int tileHeight = mainImage.getHeight(null);
        int countY = (height / tileHeight);
        if (height % tileHeight != 0) {
            countY++;
        }
        for (int j = 0; j < countY; j++) {
            g.drawImage(mainImage, dx, j * tileHeight, null);
        }
    }

    private void drawTilingHorizontal(Graphics g, int width, int height) {
        int dy = getDY(height);
        int tileWidth = mainImage.getWidth(null);
        int countX = (width / tileWidth);
        if (width % tileWidth != 0) {
            countX++;
        }
        for (int i = 0; i < countX; i++) {
            g.drawImage(mainImage, i * tileWidth, dy, null);
        }
    }

    private int getDX(int width) {
        int tw = mainImage.getWidth(null);
        if ((behavior & H_ALIGN_LEFT) != 0) {
            return 0;
        } else if ((behavior & H_ALIGN_CENTER) != 0) {
            return Math.max((width - tw) / 2, 0);
        } else if ((behavior & H_ALIGN_RIGHT) != 0) {
            return width - tw;
        } else if ((behavior & H_ALIGN_EXACT) != 0) {
            return fixedX;
        } else {
            return 0;
        }
    }

    private int getDY(int height) {
        int th = mainImage.getHeight(null);
        if ((behavior & V_ALIGN_TOP) != 0) {
            return 0;
        } else if ((behavior & V_ALIGN_CENTER) != 0) {
            return Math.max((height - th) / 2, 0);
        } else if ((behavior & V_ALIGN_BOTTOM) != 0) {
            return height - th;
        } else if ((behavior & V_ALIGN_EXACT) != 0) {
            return fixedY;
        } else {
            return 0;
        }
    }
}
