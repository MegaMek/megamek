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
import java.util.TreeMap;

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
     * @param currentMonitor
     * @return The height of the screen taking into account display scaling
     */
    public static Dimension getScaledScreenSize(Component component) {
        return getScaledScreenSize(component.getGraphicsConfiguration().getDevice().getDisplayMode());
    }

    /**
     *
     * @param currentMonitor
     * @return The height of the screen taking into account display scaling
     */
    public static Dimension getScaledScreenSize(DisplayMode currentMonitor) {
        int monitorH = currentMonitor.getHeight();
        int monitorW = currentMonitor.getWidth();
        int pixelPerInch= Toolkit.getDefaultToolkit().getScreenResolution();
        return new Dimension(
                DEFAULT_DISPLAY_DPI * monitorW / pixelPerInch,
                DEFAULT_DISPLAY_DPI * monitorH / pixelPerInch);
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

    /**
     *
     * @param multiResImageMap a collection of widths matched with corresponding image file path
     * @param parent component
     * @return a JLabel setup to the correct size to act as a splash screen
     */
    public static JLabel createSplashComponent(TreeMap<Integer, String> multiResImageMap, Component parent) {
        // Use the current monitor so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        Dimension scaledMonitorSize = getScaledScreenSize( parent.getGraphicsConfiguration().getDevice().getDisplayMode());
        Image imgSplash = parent.getToolkit().getImage(multiResImageMap.floorEntry(scaledMonitorSize.width).getValue());

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(parent);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
        }

        return createSplashComponent(imgSplash, parent, scaledMonitorSize);
    }

    /**
     *
     * @param imgSplashFile path to an image on disk
     * @param parent component
     * @return a JLabel setup to the correct size to act as a splash screen
     */
    public static JLabel createSplashComponent(String imgSplashFile, Component parent) {
        // Use the current monitor so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        Dimension scaledMonitorSize = getScaledScreenSize( parent.getGraphicsConfiguration().getDevice().getDisplayMode());

        Image imgSplash = parent.getToolkit().getImage(imgSplashFile);

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(parent);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
        }

        return createSplashComponent(imgSplash, parent, scaledMonitorSize);
    }

    /**
     *
     * @param imgSplash an image
     * @param observer
     * @param scaledMonitorSize the dimensions of the monitor taking into account display scaling
     * @return a JLabel setup to the correct size to act as a splash screen
     */
    public static JLabel createSplashComponent(Image imgSplash, ImageObserver observer, Dimension scaledMonitorSize) {
        JLabel splash;
        Dimension maxSize = new Dimension(
                (int) (scaledMonitorSize.width*0.75),
                (int) (scaledMonitorSize.height * 0.75));

        if (imgSplash != null) {
            imgSplash = DisplayUtilities.constrainImageSize(imgSplash, null, maxSize.width, maxSize.height );
            Icon icon = new ImageIcon(imgSplash);
            splash = new JLabel(icon);
        } else {
            splash = new JLabel();
        }

        Dimension splashDim = new Dimension(
                imgSplash == null ? maxSize.width : imgSplash.getWidth(observer),
                imgSplash == null ? maxSize.height : imgSplash.getHeight(observer));

        splash.setMaximumSize(splashDim);
        splash.setMinimumSize(splashDim);
        splash.setPreferredSize(splashDim);

        return splash;
    }


    public static void keepOnScreen(JFrame component) {

        DisplayMode currentMonitor = component.getGraphicsConfiguration().getDevice().getDisplayMode();
        Dimension scaledScreenSize = DisplayUtilities.getScaledScreenSize(currentMonitor);

        Point pos = component.getLocationOnScreen();
        Dimension size = component.getSize();
        Rectangle r = new Rectangle(scaledScreenSize);

        // center and size if out of bounds
        if ( (pos.x < 0) || (pos.y < 0) ||
                (pos.x + size.width > scaledScreenSize.width) ||
                (pos.y + size.height > scaledScreenSize.getHeight())) {
            component.setLocationRelativeTo(null);
        }
    }
}
