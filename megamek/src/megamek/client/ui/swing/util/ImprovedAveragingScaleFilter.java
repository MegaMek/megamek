/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.util;

import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.ColorModel;

/**
 * Extension of java.awt.image.AreaAveragingScaleFilter. Uses the same algorithm but makes sure all images are scaled
 * using area averaging. Ensures there is no fallback to ReplicateScaleFilter.
 *
 * @author Ben Smith
 */
public class ImprovedAveragingScaleFilter extends AreaAveragingScaleFilter {
    private int savedWidth;
    private int savedHeight;
    private int[] savedPixels;
    private static final ColorModel defaultCM = ColorModel.getRGBdefault();

    public ImprovedAveragingScaleFilter(int savedWidth, int savedHeight, int destWidth, int destHeight) {
        super(destWidth, destHeight);
        this.savedWidth = savedWidth;
        this.savedHeight = savedHeight;
        this.destWidth = destWidth;
        this.destHeight = destHeight;
        savedPixels = new int[savedWidth * savedHeight];
    }

    @Override
    public ImprovedAveragingScaleFilter clone() {
        ImprovedAveragingScaleFilter improvedAveragingScaleFilter = (ImprovedAveragingScaleFilter) super.clone();
        improvedAveragingScaleFilter.savedWidth = savedWidth;
        improvedAveragingScaleFilter.savedHeight = savedHeight;
        improvedAveragingScaleFilter.savedPixels = savedPixels.clone();
        return improvedAveragingScaleFilter;
    }

    @Override
    public void setColorModel(ColorModel model) {
        // Change color model to model you are generating
        consumer.setColorModel(defaultCM);
    }

    @Override
    public void setHints(int hintFlags) {
        consumer.setHints(TOPDOWNLEFTRIGHT | COMPLETESCANLINES | SINGLEPASS | (hintFlags & SINGLEFRAME));
    }

    @Override
    public void setPixels(int x, int y, int width, int height, ColorModel cm, byte[] pixels, int offset, int scanSize) {
        setThePixels(x, y, width, height, cm, pixels, offset, scanSize);
    }

    @Override
    public void setPixels(int x, int y, int width, int height, ColorModel cm, int[] pixels, int offset, int scanSize) {
        setThePixels(x, y, width, height, cm, pixels, offset, scanSize);
    }

    private void setThePixels(int x, int y, int width, int height, ColorModel cm, Object pixels, int offset,
          int scanSize) {

        int sourceOffset = offset;
        int destinationOffset = y * savedWidth + x;
        boolean bytearray = (pixels instanceof byte[]);
        for (int yy = 0; yy < height; yy++) {
            for (int xx = 0; xx < width; xx++) {
                if (bytearray) {
                    savedPixels[destinationOffset++] = cm.getRGB(((byte[]) pixels)[sourceOffset++] & 0xff);
                } else {
                    savedPixels[destinationOffset++] = cm.getRGB(((int[]) pixels)[sourceOffset++]);
                }
            }
            sourceOffset += (scanSize - width);
            destinationOffset += (savedWidth - width);
        }
    }

    @Override
    public void imageComplete(int status) {
        if ((status == IMAGEABORTED) || (status == IMAGEERROR)) {
            consumer.imageComplete(status);
            return;
        }
        // get orig image width and height
        int[] pixels = new int[savedWidth];
        int position;
        for (int yy = 0; yy < savedHeight; yy++) {
            position = 0;
            int start = yy * savedWidth;
            for (int xx = 0; xx < savedWidth; xx++) {
                pixels[position++] = savedPixels[start + xx];
            }
            super.setPixels(0, yy, savedWidth, 1, defaultCM, pixels, 0, savedWidth);
        }
        consumer.imageComplete(status);
    }
}
