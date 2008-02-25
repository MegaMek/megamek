/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

/*
 * RotateFilter.java
 *
 * Created on April 17, 2002, 5:13 PM
 */

package megamek.client.ui.AWT.util;

import java.awt.image.ColorModel;
import java.awt.image.RGBImageFilter;

/**
 * Filters an image by rotating it. The image is rotated around its center.
 * TODO: This could be optimized... oh, um... everywhere. It was pretty late at
 * night when I programmed most of this.
 * 
 * @author Ben
 * @version
 */
public class RotateFilter extends RGBImageFilter {

    private static final int ALPHA_CLIP = 144;

    private double sin;
    private double cos;

    private int width;
    private int height;
    private double cx;
    private double cy;
    private int[] raster;

    /** Creates new RotateFilter1 */
    public RotateFilter(double angle) {
        this.sin = Math.sin(angle);
        this.cos = Math.cos(angle);
    }

    /**
     * Store the dimensions, when set.
     */
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        cx = width / 2.0;
        cy = height / 2.0;
        raster = new int[width * height];
        consumer.setDimensions(width, height);
    }

    /**
     * Don't filter, just store.
     */
    public int filterRGB(int x, int y, int rgb) {
        raster[y * width + x] = rgb;
        return rgb;
    }

    /**
     * Here's where we do the work.
     */
    public void imageComplete(int status) {
        if (status == IMAGEERROR || status == IMAGEABORTED) {
            consumer.imageComplete(status);
            return;
        }
        // filter everything
        rotate();
        // done!
        consumer.setPixels(0, 0, width, height, ColorModel.getRGBdefault(),
                raster, 0, width);
        consumer.imageComplete(status);
    }

    /**
     * Rotate all pixels.
     */
    private void rotate() {
        int[] newpixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                newpixels[y * width + x] = rotatedPixel(x, y);
            }
        }
        raster = newpixels;
    }

    /**
     * Returns the "destination image" pixel
     */
    private final int rotatedPixel(int x, int y) {
        double tx = -(cx - x);
        double ty = -(cy - y);

        double rx = cos * tx - sin * ty;
        double ry = cos * ty + sin * tx;

        return pixelBilinear(rx + cx, ry + cy);
    }

    /**
     * Returns a pixel from the source image
     */
    private final int pixel(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return raster[y * width + x];
    }

    private final int alpha(int pix) {
        return (pix >> 24) & 0xff;
    }

    private final int blue(int pix) {
        return pix & 0xff;
    }

    private final int red(int pix) {
        return (pix >> 16) & 0xff;
    }

    private final int green(int pix) {
        return (pix >> 8) & 0xff;
    }

    private final int combine(int alpha, int red, int green, int blue) {
        return (alpha > ALPHA_CLIP ? 0xFF000000 : 0) | (red << 16)
                | (green << 8) | (blue);
    }

    /**
     * Get the bilinearly calculated pixel at the coordinates. Lazy black &
     * white mode.
     */
    private int pixelBilinear(double x, double y) {
        int fx = (int) Math.floor(x);
        int fy = (int) Math.floor(y);

        int alpha0 = alpha(pixel(fx, fy));
        int alpha1 = alpha(pixel(fx + 1, fy));
        int alpha2 = alpha(pixel(fx, fy + 1));
        int alpha3 = alpha(pixel(fx + 1, fy + 1));

        // don't bother calculating transparent pixels
        if (alpha0 == 0 && alpha1 == 0 && alpha2 == 0 && alpha3 == 0) {
            return 0;
        }

        int red0 = red(pixel(fx, fy));
        int red1 = red(pixel(fx + 1, fy));
        int red2 = red(pixel(fx, fy + 1));
        int red3 = red(pixel(fx + 1, fy + 1));

        int green0 = green(pixel(fx, fy));
        int green1 = green(pixel(fx + 1, fy));
        int green2 = green(pixel(fx, fy + 1));
        int green3 = green(pixel(fx + 1, fy + 1));

        int blue0 = blue(pixel(fx, fy));
        int blue1 = blue(pixel(fx + 1, fy));
        int blue2 = blue(pixel(fx, fy + 1));
        int blue3 = blue(pixel(fx + 1, fy + 1));

        double xv = x - fx;
        double yv = y - fy;

        double mul0 = (1.0 - xv) * (1.0 - yv);
        double mul1 = xv * (1.0 - yv);
        double mul2 = (1.0 - xv) * yv;
        double mul3 = xv * yv;

        int alpha = (int) Math.round(mul0 * alpha0 + mul1 * alpha1 + mul2
                * alpha2 + mul3 * alpha3);
        int blue = (int) Math.round(mul0 * blue0 + mul1 * blue1 + mul2 * blue2
                + mul3 * blue3);
        int red = (int) Math.round(mul0 * red0 + mul1 * red1 + mul2 * red2
                + mul3 * red3);
        int green = (int) Math.round(mul0 * green0 + mul1 * green1 + mul2
                * green2 + mul3 * green3);

        return combine(alpha, red, green, blue);
    }

}
