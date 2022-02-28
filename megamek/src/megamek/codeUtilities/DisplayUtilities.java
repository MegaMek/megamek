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

import java.awt.*;

public class DisplayUtilities {
    private static final int DEFAULT_DISPLAY_DPI = 96;
    public static int getScaledScreenWidth(DisplayMode currentMonitor) {
        int monitorW = currentMonitor.getWidth();
        int pixelPerInch= Toolkit.getDefaultToolkit().getScreenResolution();
        return DEFAULT_DISPLAY_DPI * monitorW / pixelPerInch;
    }

    public static int getScaledScreenHeight(DisplayMode currentMonitor) {
        int monitorH = currentMonitor.getHeight();
        int pixelPerInch= Toolkit.getDefaultToolkit().getScreenResolution();
        return DEFAULT_DISPLAY_DPI * monitorH / pixelPerInch;
    }

    public static Image constrainImageSize(Image image, int maxWidth) {
        return image.getScaledInstance(maxWidth, -1, Image.SCALE_DEFAULT);

    }
}
