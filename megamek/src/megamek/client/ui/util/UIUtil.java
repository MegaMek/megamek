/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.util;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.Serial;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.swing.*;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.widget.RawImagePanel;
import megamek.common.Player;
import megamek.logging.MMLogger;

public final class UIUtil {
    private static final MMLogger logger = MMLogger.create(UIUtil.class);

    // The standard pixels-per-inch to compare against for display scaling
    private static final int DEFAULT_DISPLAY_PPI = 96;
    private static final Dimension REFERENCE_RESOLUTION = new Dimension(1920, 1080);
    private static final double MINIMUM_RESOLUTION_SCALE_FACTOR = 0.5f;

    /**
     * The width for a tooltip displayed to the side of a dialog using one of TipXX classes.
     */
    private static final int TOOLTIP_WIDTH = 300;

    /** The style = font-size: xx value corresponding to a GUI scale of 1 */
    public static final int FONT_SCALE1 = 14;
    public static final int FONT_SCALE2 = 17;
    public final static String ECM_SIGN = " \u24BA ";
    public final static String LOADED_SIGN = " \u26DF ";
    public final static String UNCONNECTED_SIGN = " \u26AC";
    public final static String CONNECTED_SIGN = " \u26AF ";
    public final static String WARNING_SIGN = " \u26A0 ";
    public final static String QUIRKS_SIGN = " \u24E0 ";
    public static final String DOT_SPACER = " \u2B1D ";
    public static final String BOT_MARKER = " \u259A ";
    public static final String VRT_SIGN = " \u25CE ";  // Bullseye for Variable Range Targeting

    public static void showMUL(int mulId, Component parent) {
        browse(MMConstants.MUL_URL_PREFIX + mulId, parent);
    }

    public static void browse(String url, Component parent) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URL(url).toURI());
            }
        } catch (Exception ex) {
            logger.error("", ex);
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String repeat(String str, int count) {
        return String.valueOf(str).repeat(Math.max(0, count));
    }

    /**
     * Returns an HTML FONT tag setting the font face to Dialog and the font size according to GUIScale.
     */
    public static String fontHTML() {
        return "<FONT FACE=Dialog>";
    }

    /**
     * Returns an HTML FONT tag setting the color to the given col and the font face to Dialog.
     */
    public static String fontHTML(Color col) {
        return "<FONT FACE=Dialog " + colorString(col) + ">";
    }

    /**
     * Returns an HTML FONT tag setting the font face to Dialog and the font size according to the given scale delta,
     * where the font size target is standard font size * (1 + deltaScale)
     */
    public static String fontHTML(float deltaScale) {
        return "<FONT FACE=Dialog " + sizeString(deltaScale) + ">";
    }

    /**
     * Returns an HTML - tag attribute text end tag
     */
    public static String tag(String tag, String attributes, String text) {
        attributes = attributes.isEmpty() ? attributes : ' ' + attributes;
        String format = "<%s%s>%s</%s>";
        return String.format(format, tag, attributes, text, tag);
    }

    /** Returns the yellow and gui-scaled warning sign. */
    public static String warningSign() {
        return fontHTML(uiYellow()) + WARNING_SIGN + "</FONT>";
    }

    /** Returns the (usually) red and gui-scaled warning sign. */
    public static String criticalSign() {
        return fontHTML(GUIPreferences.getInstance().getWarningColor()) + WARNING_SIGN + "</FONT>";
    }

    /**
     * Returns a span of the given CSS class with opening and closing tag and the content in between. The class should
     * be defined in the header styles block, e.g. as
     * <code>.myspan { ... }</code>,
     * then it can be given as "myspan" here.
     *
     * @param cssClass the class as defined in CSS styles
     * @param content  the text to go in the span
     *
     * @return the complete span block
     */
    public static String spanCSS(String cssClass, String content) {
        return "<span class=\"" + cssClass + "\">" + content + "</span>";
    }

    /**
     * Returns an anchor with opening and closing tag and the content in between. Used for internal navigation in a
     * document
     *
     * @param anchorId the name of the anchor
     * @param content  the text to go in the span
     *
     * @return the complete anchor block
     */
    public static String anchor(String anchorId, String content) {
        return "<a name=\"" + anchorId + "\">" + content + "</a>";
    }

    /**
     * Wraps the content in a link for an internal anchor.
     *
     * @param anchorId the name of the anchor
     * @param content  the text to go in the link
     *
     * @return the complete link block
     */
    public static String link(String anchorId, String content) {
        return "<a href='#" + anchorId + "'>" + content + "</a>";
    }

    /**
     * Returns a hr element, which is usually represented as a line across the page.
     *
     * @return an hr element
     */
    public static String divider() {
        return "<hr/>";
    }

    /**
     * Returns a div of the given CSS class with opening and closing tag and the content in between. The class should be
     * defined in the header styles block, e.g. as
     * <code>.mydiv { ... }</code>,
     * then it can be given as "mydiv" here.
     *
     * @param cssClass the class as defined in CSS styles
     * @param content  the text to go in the div
     *
     * @return the complete div block
     */
    public static String divCSS(String cssClass, String content) {
        return "<div class=\"" + cssClass + "\">" + content + "</div>";
    }

    /**
     * Returns a TD table cell of the given CSS class with opening and closing tag and the content in between. The class
     * should be defined in the header styles block, e.g. as
     * <code>.mycell { ... }</code>, then it
     * can be given as "mycell" here.
     *
     * @param cssClass the class as defined in CSS styles
     * @param content  the text to go in the table cell
     *
     * @return the complete TD block
     */
    public static String tdCSS(String cssClass, String content) {
        return "<TD class=\"" + cssClass + "\">" + content + "</TD>";
    }

    /**
     * Returns a TD table cell of the given CSS class with opening and closing tag and the content in between (as text).
     * The class should be defined in the header styles block, e.g. as
     * <code>.mycell { ... }</code>, then it
     * can be given as "mycell" here.
     *
     * @param cssClass the class as defined in CSS styles
     * @param content  the text to go in the table cell
     *
     * @return the complete TD block
     */
    public static String tdCSS(String cssClass, int content) {
        return "<TD class=\"" + cssClass + "\">" + content + "</TD>";
    }

    /**
     * Helper method to place Strings in lines according to length. The Strings in origList will be added to one line
     * with separator sep between them as long as the total length does not exceed maxLength. If it exceeds maxLength, a
     * new line is begun. All lines but the last will end with sep if sepAtEnd is true.
     */
    public static ArrayList<String> arrangeInLines(List<String> origList, int maxLength, String sep, boolean sepAtEnd) {

        ArrayList<String> result = new ArrayList<>();
        if (origList == null || origList.isEmpty()) {
            return result;
        }
        StringBuilder currLine = new StringBuilder();
        for (String curr : origList) {
            // Skip empty strings to avoid double separators
            if (curr.isEmpty()) {
                continue;
            }

            if (currLine.isEmpty()) {
                // No entry in this line yet
                currLine = new StringBuilder(curr);
            } else if (currLine.length() + curr.length() + sep.length() <= maxLength) {
                // This line can hold another string
                currLine.append(sep).append(curr);
            } else {
                // This line cannot hold another string
                currLine.append(sepAtEnd ? sep : "");
                result.add(currLine.toString());
                currLine = new StringBuilder(curr);
            }
        }
        if (!currLine.isEmpty()) {
            // Add the last unfinished line
            result.add(currLine.toString());
        } else if (sepAtEnd) {
            // Remove the last unnecessary sep if there were no more Strings
            String lastLine = result.get(result.size() - 1);
            String newLine = lastLine.substring(0, lastLine.length() - sep.length());
            result.remove(result.size() - 1);
            result.add(newLine);
        }
        return result;
    }

    /**
     * Returns a UIManager Color that can be used as an alternate row color in a table to offset each other row.
     */
    public static Color alternateTableBGColor() {
        Color result = UIManager.getColor("Table.alternateRowColor");
        if (result != null) {
            return result;
        }
        result = UIManager.getColor("controlHighlight");
        if (result != null) {
            return result;
        }
        result = UIManager.getColor("Table.background");
        if (result != null) {
            return result;
        }
        // The really last fallback position
        return uiGray();
    }

    /**
     * Returns the Color associated with either enemies, allies or oneself from the GUIPreferences depending on the
     * relation of the given player1 and player2.
     */
    public static Color teamColor(Player player1, Player player2) {
        if (player1.getId() == player2.getId()) {
            return GUIPreferences.getInstance().getMyUnitColor();
        } else if (player1.isEnemyOf(player2)) {
            return GUIPreferences.getInstance().getEnemyUnitColor();
        } else {
            return GUIPreferences.getInstance().getAllyUnitColor();
        }
    }

    /**
     * Returns a green color suitable as a text color. The supplied color depends on the UI look and feel and will be
     * lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiGreen() {
        return uiBgBrightness() > 130 ? LIGHT_UI_GREEN : DARK_UI_GREEN;
    }

    /**
     * Returns a gray color suitable as a text color. The supplied color depends on the UI look and feel and will be
     * lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiGray() {
        return uiBgBrightness() > 130 ? LIGHT_UI_GRAY : DARK_UI_GRAY;
    }

    /**
     * Returns a gray color suitable as a text color.
     *
     * <p>The supplied color does is independent of the UI look and feel.</p>
     */
    public static Color uiIndependentGray() {
        return UI_GRAY;
    }

    /**
     * Returns a light blue color suitable as a text color. The supplied color depends on the UI look and feel and will
     * be lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightBlue() {
        return uiBgBrightness() > 130 ? LIGHT_UI_LIGHTBLUE : DARK_UI_LIGHTBLUE;
    }

    /**
     * Returns a light red color suitable as a text color. The supplied color depends on the UI look and feel and will
     * be lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightRed() {
        return uiBgBrightness() > 130 ? LIGHT_UI_LIGHT_RED : DARK_UI_LIGHT_RED;
    }

    /**
     * Returns a light violet color suitable as a text color. The supplied color depends on the UI look and feel and
     * will be lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightViolet() {
        return uiBgBrightness() > 130 ? LIGHT_UI_LIGHT_VIOLET : DARK_UI_LIGHT_VIOLET;
    }

    /**
     * Returns a light green color suitable as a text color. The supplied color depends on the UI look and feel and will
     * be lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiLightGreen() {
        return uiBgBrightness() > 130 ? LIGHT_UI_LIGHTGREEN : DARK_UI_LIGHTGREEN;
    }

    /**
     * Returns a yellow color suitable as a text color. The supplied color depends on the UI look and feel and will be
     * lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiYellow() {
        return uiBgBrightness() > 130 ? LIGHT_UI_YELLOW : DARK_UI_YELLOW;
    }

    /**
     * Returns a light red color suitable as a text color. The supplied color depends on the UI look and feel and will
     * be lighter for a dark UI LAF than for a light UI LAF.
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static Color uiBlack() {
        return uiBgBrightness() > 130 ? LIGHT_UI_BLACK : DARK_UI_BLACK;
    }

    /**
     * Returns a light red color suitable as a text color. The supplied color depends on the UI look and feel and will
     * be lighter for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiWhite() {
        return uiBgBrightness() > 130 ? LIGHT_UI_WHITE : DARK_UI_WHITE;
    }

    /**
     * Returns a color for the UI display of Quirks/Advantages. Different colors will be supplied for a dark and for a
     * light UI look-and-feel.
     */
    public static Color uiQuirksColor() {
        return uiBgBrightness() > 130 ? LIGHT_UI_LIGHTCYAN : DARK_UI_LIGHTCYAN;
    }

    /**
     * Returns a color for the UI display of Partial Repairs. Different colors will be supplied for a dark and for a
     * light UI look-and-feel.
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static Color uiPartialRepairColor() {
        return uiLightRed();
    }

    /**
     * Returns a color for the UI display of C3 Info. Different colors will be supplied for a dark and for a light UI
     * look-and-feel.
     */
    public static Color uiC3Color() {
        return uiLightViolet();
    }

    /**
     * Returns a color for the UI display of C3 Info. Different colors will be supplied for a dark and for a light UI
     * look-and-feel.
     */
    public static Color uiNickColor() {
        return uiLightGreen();
    }

    /**
     * Returns a color for the UI display of C3 Info. Different colors will be supplied for a dark and for a light UI
     * look-and-feel.
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static Color uiTTWeaponColor() {
        return uiLightBlue();
    }

    /**
     * Returns a dark blue color suitable as a background color. The supplied color depends on the UI look and feel and
     * will be darker for a dark UI LAF than for a light UI LAF.
     */
    public static Color uiDarkBlue() {
        return uiBgBrightness() > 130 ? LIGHT_UI_DARKBLUE : DARK_UI_DARKBLUE;
    }

    /**
     * Returns the given values multiplied by the current GUI scaling as a Dimension. Use this to adapt things that the
     * automatic scaling doesn't affect, e.g. images.
     *
     * @param width  the width of the Dimension
     * @param height the height of the Dimension
     */
    public static Dimension scaleForGUI(int width, int height) {
        return new Dimension(scaleForGUI(width), scaleForGUI(height));
    }

    /**
     * Returns the given value multiplied by the current GUI scaling. Use this to adapt things that the automatic
     * scaling doesn't affect, e.g. images. Note that the given int value is scaled as a float and then rounded.
     *
     * @param value The value to scale up or down according to the current GUI scale
     */
    public static int scaleForGUI(int value) {
        return Math.round(scaleForGUI((float) value));
    }

    /**
     * Returns the given value multiplied by the current GUI scaling. Use this to adapt things that the automatic
     * scaling doesn't affect, e.g. images.
     *
     * @param value The value to scale up or down according to the current GUI scale
     */
    public static float scaleForGUI(float value) {
        return GUIPreferences.getInstance().getGUIScale() * value;
    }

    /**
     * Returns the provided color with its alpha value set to the provided alpha. alpha should be from 0 to 255 with 0
     * meaning transparent.
     */
    public static Color addAlpha(Color color, int alpha) {
        Objects.requireNonNull(color);
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("Alpha value out of range: " + alpha);
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Returns a grayed-out version of the given color. gray should be from 0 to 255 with 255 meaning completely gray.
     * Does not change the brightness, nor alpha.
     */
    public static Color addGray(Color color, int gray) {
        Objects.requireNonNull(color);
        if (gray < 0 || gray > 255) {
            throw new IllegalArgumentException("Gray value out of range: " + gray);
        }
        int mid = (color.getRed() + color.getGreen() + color.getBlue()) * gray / 3;
        int red = (color.getRed() * (255 - gray) + mid) / 255;
        int green = (color.getGreen() * (255 - gray) + mid) / 255;
        int blue = (color.getBlue() * (255 - gray) + mid) / 255;
        return new Color(red, green, blue, color.getAlpha());
    }

    /**
     * @param currentMonitor The DisplayMode of the current monitor
     *
     * @return the width of the screen taking into account display scaling
     */
    public static int getScaledScreenWidth(DisplayMode currentMonitor) {
        int monitorW = currentMonitor.getWidth();
        int pixelPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
        return DEFAULT_DISPLAY_PPI * monitorW / pixelPerInch;
    }

    /**
     * @param currentMonitor The DisplayMode of the current monitor
     *
     * @return The height of the screen taking into account display scaling
     */
    public static int getScaledScreenHeight(DisplayMode currentMonitor) {
        int monitorH = currentMonitor.getHeight();
        int pixelPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
        return DEFAULT_DISPLAY_PPI * monitorH / pixelPerInch;
    }

    /**
     * Gets the DPI scale factor for the monitor containing the specified component
     *
     * @param component The component to check
     *
     * @return The DPI scale factor for the containing monitor
     */
    public static double getMonitorScaleFactor(Component component) {
        // Get the GraphicsConfiguration for the monitor containing this component
        GraphicsConfiguration gc = (component != null) ? component.getGraphicsConfiguration() : null;
        if (gc == null) {
            return getDpiScaleFactor(component); //fallback
        }
        // Calculate the DPI scale for this specific monitor
        AffineTransform transform = gc.getDefaultTransform();
        return transform.getScaleX();
    }

    /**
     * Gets the resolution scale factor for the monitor containing the specified component. Baseline is 1080p
     * (1920x1080).
     *
     * @param component The component to check
     *
     * @return The resolution scale factor for the containing monitor
     */
    public static double getResolutionScaleFactor(Component component) {
        return getResolutionScaleFactor(component, REFERENCE_RESOLUTION);
    }

    /**
     * Gets the resolution scale factor for the monitor containing the specified component.
     *
     * @param component           The component to check
     * @param referenceResolution The reference resolution width/height to use for scaling
     *
     * @return The resolution scale factor for the containing monitor
     */
    public static double getResolutionScaleFactor(Component component, Dimension referenceResolution) {
        final Dimension logicalScreenSize = UIUtil.getScaledScreenSize(component);
        final double scaleFactorX = logicalScreenSize.width / referenceResolution.getWidth();
        final double scaleFactorY = logicalScreenSize.height / referenceResolution.getHeight();
        return Math.max(MINIMUM_RESOLUTION_SCALE_FACTOR, Math.min(scaleFactorX, scaleFactorY));
    }

    /**
     * Calculate the DPI scale factor for a component
     *
     * @param component The component to get scaling information from
     *
     * @return The scaling factor based on DPI
     */
    public static float getDpiScaleFactor(Component component) {
        GraphicsConfiguration graphicsConfiguration = null;
        if (component != null) {
            graphicsConfiguration = component.getGraphicsConfiguration();
        }

        if (graphicsConfiguration == null) {
            // Fallback to default GraphicsEnvironment if component doesn't have a GraphicsConfiguration
            graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                  .getDefaultScreenDevice()
                  .getDefaultConfiguration();
        }
        // Get screen resolution
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        // Calculate scale factor (compared to reference 96 DPI)
        return (float) dpi / DEFAULT_DISPLAY_PPI;
    }

    /**
     * @return The height of the screen taking into account display scaling
     */
    public static Dimension getScaledScreenSize(Component component) {
        GraphicsConfiguration gc = null;
        if (component != null) {
            gc = component.getGraphicsConfiguration();
        }
        if (gc == null) {
            try {
                gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                      .getDefaultScreenDevice()
                      .getDefaultConfiguration();
            } catch (HeadlessException e) {
                logger.warn("No GraphicsConfiguration found, using default size");
                return new Dimension(800, 600);
            }
        }
        Rectangle bounds = gc.getBounds();
        return new Dimension(bounds.width, bounds.height);
    }

    /**
     * @param currentMonitor The DisplayMode of the current monitor
     *
     * @return The height of the screen taking into account display scaling
     */
    public static Dimension getScaledScreenSize(DisplayMode currentMonitor) {
        int monitorH = currentMonitor.getHeight();
        int monitorW = currentMonitor.getWidth();
        int pixelPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
        return new Dimension(DEFAULT_DISPLAY_PPI * monitorW / pixelPerInch,
              DEFAULT_DISPLAY_PPI * monitorH / pixelPerInch);
    }

    /**
     * @return an image with the same aspect ratio that fits within the given bounds, or the existing image if it
     *       already does
     */
    public static Image constrainImageSize(Image image, ImageObserver observer, int maxWidth, int maxHeight) {
        int w = image.getWidth(observer);
        int h = image.getHeight(observer);

        if (w <= 0 || h <= 0) {
            return image;
        }
        if ((w <= maxWidth) && (h <= maxHeight)) {
            return image;
        }
        int targetWidth;
        int targetHeight;

        // choose resize that fits in bounds
        double scaleW = maxWidth / (double) w;
        double scaleH = maxHeight / (double) h;

        if (scaleW < scaleH) {
            // Fit to width
            targetWidth = maxWidth;
            targetHeight = (int) Math.round(h * scaleW);
        } else {
            // Fit to height
            targetHeight = maxHeight;
            targetWidth = (int) Math.round(w * scaleH);
        }

        // Ensure dimensions are at least 1
        if (targetWidth <= 0) {
            targetWidth = 1;
        }
        if (targetHeight <= 0) {
            targetHeight = 1;
        }
        // Determine the type of the new BufferedImage.
        int imageType = BufferedImage.TYPE_INT_ARGB;
        if (image instanceof BufferedImage) {
            int currentType = ((BufferedImage) image).getType();
            if ((currentType != 0)) {
                imageType = currentType;
            }
        }
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D g2d = scaledImage.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2d.drawImage(image, 0, 0, targetWidth, targetHeight, observer);
        } finally {
            g2d.dispose();
        }

        return scaledImage;
    }

    /**
     * @param multiResImageMap a collection of widths matched with corresponding image file path
     * @param parent           component
     *
     * @return a RawImagePanel setup to the correct size to act as a splash screen
     */
    public static RawImagePanel createSplashComponent(TreeMap<Integer, String> multiResImageMap, Component parent) {
        // Use the current monitor so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        Dimension scaledMonitorSize = getScaledScreenSize(parent);
        Entry<Integer, String> entry = multiResImageMap.ceilingEntry(scaledMonitorSize.width);
        if (entry == null) {
            entry = multiResImageMap.lastEntry();
        }
        Image imgSplash = parent.getToolkit().getImage(entry.getValue());

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
     * @param imgSplashFile path to an image on disk
     * @param parent        component
     *
     * @return a RawImagePanel setup to the correct size to act as a splash screen
     */
    public static RawImagePanel createSplashComponent(String imgSplashFile, Component parent) {
        // Use the current monitor so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        Dimension scaledMonitorSize = getScaledScreenSize(parent);

        Image imgSplash = parent.getToolkit().getImage(imgSplashFile);

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(parent);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
            logger.warn("Splash image loading interrupted");
        }

        return createSplashComponent(imgSplash, parent, scaledMonitorSize);
    }

    /**
     * @param imgSplash         an image
     * @param observer          An imageObserver
     * @param scaledMonitorSize the dimensions of the monitor taking into account display scaling
     *
     * @return a RawImagePanel setup to the correct size to act as a splash screen
     */

    public static RawImagePanel createSplashComponent(Image imgSplash, ImageObserver observer,
          Dimension scaledMonitorSize) {
        if (imgSplash == null) {
            return new RawImagePanel(null);
        }
        Dimension maxSize = new Dimension((int) (scaledMonitorSize.width * 0.75),
              (int) (scaledMonitorSize.height * 0.75));

        Image constrainedSplashImage = UIUtil.constrainImageSize(imgSplash, observer, maxSize.width, maxSize.height);
        RawImagePanel splash = new RawImagePanel(constrainedSplashImage);

        Dimension splashDim = new Dimension(constrainedSplashImage.getWidth(observer),
              constrainedSplashImage.getHeight(observer));

        splash.setMaximumSize(splashDim);
        splash.setMinimumSize(splashDim);
        splash.setPreferredSize(splashDim);

        return splash;
    }

    public static void keepOnScreen(JFrame component) {

        DisplayMode currentMonitor = component.getGraphicsConfiguration().getDevice().getDisplayMode();
        Dimension scaledScreenSize = UIUtil.getScaledScreenSize(currentMonitor);

        Point pos = component.getLocationOnScreen();
        Dimension size = component.getSize();

        // center and size if out of bounds
        if ((pos.x < 0) ||
              (pos.y < 0) ||
              (pos.x + size.width > scaledScreenSize.width) ||
              (pos.y + size.height > scaledScreenSize.getHeight())) {
            component.setLocationRelativeTo(null);
        }
    }

    /**
     * Activates antialiasing and other high-quality settings for the given Graphics.
     *
     * @param graph Graphics context to use hq rendering for
     */
    public static void setHighQualityRendering(Graphics graph) {
        if (GUIPreferences.getInstance().getHighQualityGraphics()) {
            ((Graphics2D) graph).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D) graph).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            ((Graphics2D) graph).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                  RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        if (GUIPreferences.getInstance().getHighPerformanceGraphics()) {
            ((Graphics2D) graph).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            ((Graphics2D) graph).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                  RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else if (GUIPreferences.getInstance().getHighQualityGraphics()) {
            ((Graphics2D) graph).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            ((Graphics2D) graph).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
    }

    /**
     * Updates all existing windows and frames. Use after a gui scale change or look-and-feel change.
     */
    public static void updateAfterUiChange() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    /** A specialized panel for the header of a section. */
    public static class Header extends JPanel {
        @Serial
        private static final long serialVersionUID = -6235772150005269143L;

        public Header(String text) {
            super();
            setLayout(new GridLayout(1, 1, 0, 0));
            add(new JLabel("\u29C9  " + Messages.getString(text)));
            setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBackground(alternateTableBGColor());
        }
    }

    /** A panel for the content of a subsection of the dialog. */
    public static class Content extends JPanel {
        @Serial
        private static final long serialVersionUID = -6605053283642217306L;

        public Content(LayoutManager layout) {
            this();
            setLayout(layout);
        }

        public Content() {
            super();
            setBorder(BorderFactory.createEmptyBorder(8, 25, 5, 25));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    /** A panel for a subsection of the dialog, e.g. Minefields. */
    public static class OptionPanel extends FixedYPanel {
        @Serial
        private static final long serialVersionUID = -7168700339882132428L;

        public OptionPanel(String header) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            add(new Header(header));
        }
    }

    /** A JPanel that does not stretch vertically beyond its preferred height. */
    public static class FixedYPanel extends JPanel {
        @Serial
        private static final long serialVersionUID = -8805710112708937089L;

        public FixedYPanel(LayoutManager layout) {
            super(layout);
        }

        public FixedYPanel() {
            super();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
        }
    }

    /** A JPanel that does not stretch horizontally beyond its preferred width. */
    public static class FixedXPanel extends JPanel {
        @Serial
        private static final long serialVersionUID = -4634244641653743910L;

        public FixedXPanel(LayoutManager layout) {
            super(layout);
        }

        public FixedXPanel() {
            super();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(getPreferredSize().width, super.getMaximumSize().height);
        }
    }

    /**
     * A JLabel with a specialized tooltip display. Displays the tooltip to the right side of the parent dialog, not
     * following the mouse. Used in the player settings and planetary settings dialogs.
     */
    public static class TipLabel extends JLabel {

        public TipLabel(String text) {
            super(text);
        }

        public TipLabel(String text, int align) {
            super(text, align);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * A JButton with a specialized tooltip display. Displays the tooltip to the right side of the parent dialog, not
     * following the mouse. Used in the player settings and planetary settings dialogs.
     */
    public static class TipButton extends JButton {

        public TipButton(String text) {
            super(text);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * A MMComboBox with a specialized tooltip display. Displays the tooltip to the right side
     * <p>
     * of the parent dialog, not following the mouse. Used in the player settings dialog.
     */
    public static class TipCombo<E> extends MMComboBox<E> {

        /**
         * @deprecated no indicated uses.
         */
        @Deprecated(since = "0.50.06", forRemoval = true)
        public TipCombo(String name) {
            super(name);
        }

        public TipCombo(String name, E[] items) {
            super(name, items);
        }

        public TipCombo(String name, final ComboBoxModel<E> model) {
            super(name, model);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * A JList with a specialized tooltip display. Displays the tooltip to the right side of the parent dialog, not
     * following the mouse.
     */
    public static class TipList<E> extends JList<E> {

        /**
         * @deprecated no indicated uses.
         */
        @Deprecated(since = "0.50.06", forRemoval = true)
        public TipList() {
            super();
        }

        public TipList(ListModel<E> dataModel) {
            super(dataModel);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * A JTextField with a specialized tooltip display. Displays the tooltip to the right side of the parent dialog, not
     * following the mouse. Can also display a hint text such as "..., ..." when empty. Used in the player settings and
     * planetary settings dialogs.
     */
    public static class TipTextField extends JTextField {

        String hintText;

        public TipTextField(int n) {
            super(n);
        }

        public TipTextField(String text, int n) {
            super(text, n);
        }

        public TipTextField(int n, String hint) {
            this(n);
            prepareForHint(hint);

        }

        public TipTextField(String text, int n, String hint) {
            this(text, n);
            prepareForHint(hint);
        }

        private void prepareForHint(String hint) {
            hintText = hint;
            addFocusListener(l);
            updateHint();
        }

        private void updateHint() {
            if (getText().isEmpty()) {
                setText(hintText);
                setForeground(uiGray());
                setCaretPosition(0);
            }
        }

        @Override
        public void setText(String t) {
            if ((t != null) && !t.isBlank()) {
                setForeground(null);
            }
            super.setText(t);
        }

        FocusListener l = new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                updateHint();
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (getText().equals(hintText)) {
                    setText("");
                }
                setForeground(null);
            }
        };

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * A JPanel with a specialized tooltip display. Displays the tooltip to the right side of the parent dialog, not
     * following the mouse.
     */
    public static class TipPanel extends JPanel {

        public TipPanel() {
            super();
        }

        /**
         * @deprecated no indicated uses.
         */
        @Deprecated(since = "0.50.06", forRemoval = true)
        public TipPanel(LayoutManager lm) {
            super(lm);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * A JSlider with a specialized tooltip display. Displays the tooltip to the right side of the parent window
     * (dialog), not following the mouse. Implement the missing super constructors as necessary.
     */
    public static class TipSlider extends JSlider {

        public TipSlider(int orientation, int min, int max, int value) {
            super(orientation, min, max, value);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * A MMToggleButton with a specialized tooltip display. Displays the tooltip to the right side of the parent window
     * (dialog), not following the mouse.
     */
    public static class TipMMToggleButton extends MMToggleButton {

        public TipMMToggleButton(String text) {
            super(text);
        }

        @Override
        public Point getToolTipLocation(MouseEvent event) {
            Window win = SwingUtilities.getWindowAncestor(this);
            Point origin = SwingUtilities.convertPoint(this, 0, 0, win);
            return new Point(win.getWidth() - origin.x, 0);
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setBackground(alternateTableBGColor());
            tip.setBorder(BorderFactory.createLineBorder(uiGray(), 4));
            return tip;
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(formatSideTooltip(text));
        }
    }

    /**
     * Completes the tooltip for a dialog using one of the TipXXX classes, setting its width and adding HTML tags.
     */
    public static String formatSideTooltip(String text) {
        return "<html><p width=" + scaleForGUI(TOOLTIP_WIDTH) + " style='padding:5'>" + text + "</html>";
    }

    /**
     * This is a specialized JPanel for use with a button bar at the bottom of a dialog for when it's possible that the
     * button bar has to wrap (is wider than the dialog and needs to use two or more rows for the buttons). With a
     * normal JPanel the wrapped buttons just disappear. This Panel tries to detect when wrapping occurs and then
     * extends vertically. Note that it will only extend to two rows, not more. But if three rows of buttons are used,
     * this will be very obvious. The native FlowLayout should be kept for the buttons.
     */
    public static class WrappingButtonPanel extends JPanel {
        @Serial
        private static final long serialVersionUID = -6966176665047676553L;

        @Override
        public Dimension getPreferredSize() {
            int height = super.getPreferredSize().height;
            if (getSize().width < super.getPreferredSize().width) {
                height = height * 2;
            }
            return new Dimension(super.getPreferredSize().width, height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(super.getMinimumSize().width, getPreferredSize().height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
        }
    }

    /**
     * Returns a single menu item with the given text, the given command string cmd, the given enabled state, and
     * assigned the given listener.
     */
    public static JMenuItem menuItem(String text, String cmd, boolean enabled, ActionListener listener) {

        return menuItem(text, cmd, enabled, listener, Integer.MIN_VALUE);
    }

    /**
     * Returns a single menu item with the given text, the given command string cmd, the given enabled state, and
     * assigned the given listener. Also assigns the given key mnemonic.
     */
    public static JMenuItem menuItem(String text, String cmd, boolean enabled, ActionListener listener, int mnemonic) {

        JMenuItem result = new JMenuItem(text);
        result.setActionCommand(cmd);
        result.addActionListener(listener);
        result.setEnabled(enabled);
        if (mnemonic != Integer.MIN_VALUE) {
            result.setMnemonic(mnemonic);
        }
        return result;
    }

    /**
     * Returns a Font object using the "Dialog" logic font. The font size 14.
     */
    public static Font getDefaultFont() {
        return new Font(MMConstants.FONT_DIALOG, Font.PLAIN, FONT_SCALE1);
    }

    /**
     * Returns true when a modal dialog such as the Camo Chooser or a Load Force dialog is currently shown.
     */
    public static boolean isModalDialogDisplayed() {
        return Stream.of(Window.getWindows())
              .anyMatch(w -> w.isShowing() && (w instanceof JDialog dialogWindow) && (dialogWindow.isModal()));
    }

    // PRIVATE

    private static final Color LIGHT_UI_GREEN = new Color(20, 140, 20);
    private static final Color DARK_UI_GREEN = new Color(40, 180, 40);
    private static final Color UI_GRAY = new Color(100, 100, 100);
    private static final Color LIGHT_UI_GRAY = new Color(100, 100, 100);
    private static final Color DARK_UI_GRAY = new Color(150, 150, 150);
    private static final Color LIGHT_UI_LIGHTBLUE = new Color(100, 100, 150);
    private static final Color DARK_UI_LIGHTBLUE = new Color(150, 150, 210);
    private static final Color LIGHT_UI_LIGHT_RED = new Color(210, 100, 100);
    private static final Color DARK_UI_LIGHT_RED = new Color(210, 150, 150);
    private static final Color LIGHT_UI_LIGHT_VIOLET = new Color(180, 100, 220);
    private static final Color DARK_UI_LIGHT_VIOLET = new Color(180, 150, 220);
    private static final Color LIGHT_UI_YELLOW = new Color(250, 170, 40);
    private static final Color DARK_UI_YELLOW = new Color(200, 200, 60);
    private static final Color LIGHT_UI_LIGHTCYAN = new Color(40, 130, 130);
    private static final Color DARK_UI_LIGHTCYAN = new Color(100, 180, 180);
    private static final Color LIGHT_UI_LIGHTGREEN = new Color(80, 180, 80);
    private static final Color DARK_UI_LIGHTGREEN = new Color(150, 210, 150);
    private static final Color LIGHT_UI_DARKBLUE = new Color(225, 225, 245);
    private static final Color DARK_UI_DARKBLUE = new Color(50, 50, 80);
    private static final Color LIGHT_UI_BLACK = new Color(0, 0, 0);
    private static final Color DARK_UI_BLACK = new Color(0, 0, 0);
    private static final Color LIGHT_UI_WHITE = new Color(255, 255, 255);
    private static final Color DARK_UI_WHITE = new Color(255, 255, 255);

    /**
     * Returns an HTML FONT Size String, according to GUIScale and deltaScale (e.g. "style=font-size:22"). The given
     * deltaScale is added to the GUIScale value, so a positive deltaScale value will increase the font size. The
     * adjusted GUIScale value will be kept within the limits of GUIScale. Suitable deltaScale values are usually
     * between -0.4 and +0.4
     */
    private static String sizeString(float deltaScale) {
        int fontSize = (int) ((1 + deltaScale) * FONT_SCALE1);
        return " style=font-size:" + fontSize + " ";
    }

    /**
     * Returns an HTML FONT Color String, e.g. COLOR=#FFFFFF according to the given color.
     */
    public static String colorString(Color col) {
        return " COLOR=" + Integer.toHexString(col.getRGB() & 0xFFFFFF) + " ";
    }

    /**
     * Returns Color Hex String, e.g. #FFFFFF according to the given color.
     */
    public static String toColorHexString(Color col) {
        return Integer.toHexString(col.getRGB() & 0xFFFFFF);
    }

    private static int uiBgBrightness() {
        Color bgColor = UIManager.getColor("Table.background");
        if (bgColor == null) {
            // Try another
            bgColor = UIManager.getColor("Menu.background");
        }
        if (bgColor == null) {
            return 250;
        } else {
            return colorBrightness(bgColor);
        }
    }

    private static int colorBrightness(final Color color) {
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
    }

    /**
     * The only place this is used is in the Skin Builder. If the Skin Builder can be worked without this method, it can
     * be removed.
     *
     * @return the 'virtual bounds' of the screen. That is, the union of the displayable space on all available screen
     *       devices.
     *
     * @deprecated since 0.50.04 - Unknown replacement.
     */
    @Deprecated(since = "0.50.04")
    public static Rectangle getVirtualBounds() {
        final Rectangle bounds = new Rectangle();
        Stream.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
              .map(GraphicsDevice::getConfigurations)
              .flatMap(Stream::of)
              .map(GraphicsConfiguration::getBounds)
              .forEach(bounds::add);
        return bounds;
    }

    /**
     * Ensures an on-screen window fits within the bounds of a display.
     */
    public static void updateWindowBounds(Window window) {
        final Rectangle bounds = new Rectangle();
        Stream.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
              .map(GraphicsDevice::getConfigurations)
              .flatMap(Stream::of)
              .map(GraphicsConfiguration::getBounds)
              .forEach(bounds::add);

        final Dimension size = window.getSize();
        final Point location = window.getLocation();

        if ((location.x < bounds.getMinX()) || ((location.x + size.width) > bounds.getMaxX())) {
            location.x = 0;
        }

        if ((location.y < bounds.getMinY()) || ((location.y + size.height) > bounds.getMaxY())) {
            location.y = 0;
        }

        size.setSize(Math.min(size.width, bounds.width), Math.min(size.height, bounds.height));

        window.setLocation(location);
        window.setSize(size);
    }

    /*
     * Calculates center of view port for a given point
     */
    public static int calculateCenter(int vh, int h, int th, int y) {
        y = Math.max(0, y - ((vh - th) / 2));
        y = Math.min(y, h - vh);
        return y;
    }

    /**
     * Returns the HTML/CSS color string for the given color, e.g. #FF0000 for red. Includes the #. Does not include any
     * alpha components.
     *
     * @param color The color to convert
     *
     * @return The color as a string value for use in tags or styles
     */
    public static String hexColor(Color color) {
        return String.format("#%06x", color.getRGB() & 0x00FFFFFF);
    }

    private UIUtil() {
    }
}
