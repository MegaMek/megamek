/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT.util;

import java.awt.image.RGBImageFilter;

/**
 * Filters the pixels in the image by tinting a black and white image to a
 * certain color.
 */
public class TintFilter extends RGBImageFilter {
    private double cred;
    private double cgreen;
    private double cblue;

    public TintFilter(int tintColor) {
        cred = ((tintColor >> 16) & 0xff) / 255.0;
        cgreen = ((tintColor >> 8) & 0xff) / 255.0;
        cblue = ((tintColor) & 0xff) / 255.0;

        canFilterIndexColorModel = true;
    }

    public int filterRGB(int x, int y, int RGB) {
        final int alpha = RGB & 0xff000000;
        if (alpha != 0xff000000) {
            return 0;
        }
        final int black = (RGB) & 0xff; // assume black & white
        // alter pixel to tint
        int red = (int) Math.round(cred * black);
        int green = (int) Math.round(cgreen * black);
        int blue = (int) Math.round(cblue * black);

        return alpha | (red << 16) | (green << 8) | blue;
    }
}
