/**
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

package megamek.client.util;

import java.awt.*;
import java.awt.image.*;

/**
 * Filters the pixels in the image by tinting a black and white image to a 
 * certain color.
 */
public class TintFilter extends RGBImageFilter
{
    private int cred;
    private int cgreen;
    private int cblue;
    
    public TintFilter(int tintColor) {
        this.cred   = (tintColor >> 16) & 0xff;
        this.cgreen = (tintColor >>  8) & 0xff;
        this.cblue  = (tintColor      ) & 0xff;
    }
    
    public int filterRGB(int x, int y, int RGB) {
        final int alpha = (RGB >> 24) & 0xff;
        final int black = (RGB) & 0xff;  // assume black & white
        if (alpha != 0xff) {
            return RGB;
        }
        // alter pixel to tint
        int red   = (cred   * black) / 255;
        int green = (cgreen * black) / 255;
        int blue  = (cblue  * black) / 255;
                    
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
