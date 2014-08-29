/**
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.swing.widget;

import java.awt.Graphics;
import java.awt.Image;

/**
 * Background drawer is a class which keeps reference to a single Image and
 * draws it according specified rules. For example, we can order to draw image
 * by fully tiling it over all area, or tile it in a single row or column with
 * desired alignment, or draw it just once. Alignment of drawing can be
 * <li>logical (top, bottom, center for vertical alignment and left, right,
 * center for horizontal one)</li>
 * <li>or given by exact number of pixels from top or left borders of area</li>
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
    public static final int VALIGN_TOP = 16;
    /**
     * Alignment to the centre in case of Horizontal or single tiling.
     */
    public static final int VALIGN_CENTER = 32;
    /**
     * Alignment to the bottom in case of Horizontal or single tiling.
     */
    public static final int VALIGN_BOTTOM = 64;
    /**
     * Shift down from top border of area by exact number of pixels. (default 0
     * pixels)
     */
    public static final int VALIGN_EXACT = 128;
    /**
     * Alignment to the left in case of Vertical or single tiling.
     */
    public static final int HALIGN_LEFT = 256;
    /**
     * Alignment to the center in case of Vertical or single tiling.
     */
    public static final int HALIGN_CENTER = 512;
    /**
     * Alignment to the right in case of Vertical or single tiling.
     */
    public static final int HALIGN_RIGHT = 1024;
    /**
     * Shift right from left border of area by exact number of pixels. (default
     * 0 pixels)
     */
    public static final int HALIGN_EXACT = 2048;

    // Required bit masks to manipulate behavior variable.
    private static final int TILING_TYPE_MASK = 4080;
    private static final int VALIGN_MASK = 3855;
    private static final int HALIGN_MASK = 255;
    private static final int TILING_TYPE_SELECT_MASK = 15;
    private static final int VALIGN_SELECT_MASK = 240;
    private static final int HALIGN_SELECT_MASK = 3840;

    private Image mainImage;
    private int behavior = NO_TILING | VALIGN_CENTER | HALIGN_CENTER;
    private int fixedX = 0;
    private int fixedY = 0;

    /**
     * @param mainImage image to draw by BackGroundDrawer.
     * @param behavior Integer value specifying way of tiling and alignment. For
     *            exapmple:
     *            <code> BackGroundDrawer(myImage, BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT);
     *                  </code>
     */

    public BackGroundDrawer(Image mainImage, int behavior) {
        this.mainImage = mainImage;
        this.behavior = behavior;
    }

    /**
     * Tiling style is set to NO_TILING | VALIGN_CENTER | HALIGN_CENTER.
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
     * @param type Possible values: NO_TILING, TILING_BOTH, TILING_VERTICAL,
     *            TILING_HORIZONTAL.
     */
    public void setTilingType(int type) {
        type &= TILING_TYPE_SELECT_MASK;
        behavior &= TILING_TYPE_MASK;
        behavior |= type;
    }

    /**
     * Sets vertical align of tiling
     * 
     * @param type Must be VALIGN_TOP, VALIGN_CENTER, VALIGN_BOTTOM
     */

    public void setValign(int vAlign) {
        vAlign &= VALIGN_SELECT_MASK;
        behavior &= VALIGN_MASK;
        behavior |= vAlign;
    }

    /**
     * Sets horizontal align of tiling
     * 
     * @param type Must be HALIGN_LEFT, HALIGN_CENTER, HALIGN_RIGHT
     */
    public void setHalign(int hAlign) {
        hAlign &= HALIGN_SELECT_MASK;
        behavior &= HALIGN_MASK;
        behavior |= hAlign;
    }

    /**
     * Sets exact vertical alignment exactly at "y" pixels
     */

    public void setValignExactAt(int y) {
        setValign(VALIGN_EXACT);
        fixedY = y;
    }

    /**
     * Sets exact horizontal alignment exactly at "x" pixels
     */

    public void setHalignExactAt(int x) {
        setHalign(HALIGN_EXACT);
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
     * @param g Graphics to which draw to.
     * @param width Width of the drawing area.
     * @param height Height of the drawing area.
     */

    public void drawInto(Graphics g, int width, int height) {

        if (mainImage == null)
            return;

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
            return;
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
        if (width % tileWidth != 0)
            countX++;
        int countY = (height / tileHeight);
        if (height % tileHeight != 0)
            countY++;
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
        if (height % tileHeight != 0)
            countY++;
        for (int j = 0; j < countY; j++) {
            g.drawImage(mainImage, dx, j * tileHeight, null);
        }
    }

    private void drawTilingHorizontal(Graphics g, int width, int height) {
        int dy = getDY(height);
        int tileWidth = mainImage.getWidth(null);
        int countX = (width / tileWidth);
        if (width % tileWidth != 0)
            countX++;
        for (int i = 0; i < countX; i++) {
            g.drawImage(mainImage, i * tileWidth, dy, null);
        }
    }

    private int getDX(int width) {
        int dx = 0;
        int tw = mainImage.getWidth(null);
        if ((behavior & HALIGN_LEFT) != 0) {
            dx = 0;
        } else if ((behavior & HALIGN_CENTER) != 0) {
            dx = (width - tw) / 2;
            if (dx < 0)
                dx = 0;
        } else if ((behavior & HALIGN_RIGHT) != 0) {
            dx = width - tw;
        } else if ((behavior & HALIGN_EXACT) != 0) {
            dx = fixedX;
        }
        return dx;
    }

    private int getDY(int height) {
        int dy = 0;
        int th = mainImage.getHeight(null);

        if ((behavior & VALIGN_TOP) != 0) {
            dy = 0;
        } else if ((behavior & VALIGN_CENTER) != 0) {
            dy = (height - th) / 2;
            if (dy < 0)
                dy = 0;
        } else if ((behavior & VALIGN_BOTTOM) != 0) {
            dy = height - th;
        } else if ((behavior & VALIGN_EXACT) != 0) {
            dy = fixedY;
        }
        return dy;
    }
}
