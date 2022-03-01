/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.codeUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;

public class DisplayUtilities {
    private static final int DEFAULT_DISPLAY_DPI = 96;

    /**
     *
     * @param currentMonitor
     * @return the width of the screen taking into account display scaling
     */
    public static int getScaledScreenWidth(DisplayMode currentMonitor) {
        int monitorW = currentMonitor.getWidth();
        int pixelPerInch= Toolkit.getDefaultToolkit().getScreenResolution();
        return DEFAULT_DISPLAY_DPI * monitorW / pixelPerInch;
    }

    /**
     *
     * @param currentMonitor
     * @return The height of the screen taking into account display scaling
     */
    public static int getScaledScreenHeight(DisplayMode currentMonitor) {
        int monitorH = currentMonitor.getHeight();
        int pixelPerInch= Toolkit.getDefaultToolkit().getScreenResolution();
        return DEFAULT_DISPLAY_DPI * monitorH / pixelPerInch;
    }

    /**
     *
     * @param image
     * @param maxWidth
     * @param maxHeight
     * @return an image with the same aspect ratio that fits within the given bounds, or the existing image if it already does
     */
    public static Image constrainImageSize(Image image, ImageObserver observer, int maxWidth, int maxHeight) {
        Image newImage = image;
        if (newImage.getWidth(observer) > maxWidth) {
            newImage = newImage.getScaledInstance(maxWidth, -1, Image.SCALE_DEFAULT);
        }
        if (newImage.getHeight(observer) > maxHeight) {
            newImage = newImage.getScaledInstance(-1, maxHeight, Image.SCALE_DEFAULT);
        }
        return newImage;
    }

}
