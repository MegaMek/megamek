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
 * Filters the pixels in the image by making all pixels that are the designated
 * "key" color transparent.
 */
public class KeyAlphaFilter extends RGBImageFilter {
    private int keyColor;

    public KeyAlphaFilter(int keyColor) {
        this.keyColor = keyColor;
    }

    public int filterRGB(int x, int y, int RGB) {
        if (RGB == keyColor) {
            return 0;
        }
        return RGB;
    }
}
