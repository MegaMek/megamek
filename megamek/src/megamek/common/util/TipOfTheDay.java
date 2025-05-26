/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.common.util;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.AttributedString;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.SkinSpecification;
import megamek.client.ui.widget.SkinSpecification.UIComponents;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.common.internationalization.I18n;

/**
 * @author Drake
 *       <p>
 *       Provides a daily/random tip functionality
 */
public class TipOfTheDay {
    // Enum for positioning the tip text
    public enum Position {
        TOP_BORDER, BOTTOM_BORDER, BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER,
    }

    private static final int TIP_BORDER_MARGIN = 40;
    private static final int TIP_SIDE_PADDING = 20;
    private static final float TIP_OPPOSITE_SIDE_PADDING_PERCENT = 0.4f; //% For corner positioning (example: BOTTOM_LEFT_CORNER)
    private static final float TIP_TITLE_FONT_SIZE = 14f;
    private static final float TIP_FONT_SIZE = 18f;
    private static final float STROKE_WIDTH = 3.0f;
    private static final Color TIP_STROKE_COLOR = Color.BLACK;
    private static final Color TIP_TITLE_FONT_COLOR = Color.WHITE;
    private static final Color TIP_FONT_COLOR = Color.WHITE;

    private static final float TIP_BACKGROUND_PADDING = TIP_BORDER_MARGIN;
    private static final float TIP_BACKGROUND_BASE_OPACITY = 0.8f;
    private static final float TIP_BACKGROUND_FADE_TO_OPACITY = 0.0f;
    private static final float TIP_BACKGROUND_FADE_AREA_PERCENT = 1.0f;
    private static final boolean DEFAULT_USE_RADIAL_GRADIENT = false;

    private static final String VARIABLE_PREFIX = "{";
    private static final String VARIABLE_SUFFIX = "}";
    private static final String KEYBIND_VARIABLE_PREFIX = VARIABLE_PREFIX+"keybind:";

    private final String bundleName;
    private final String tipOfTheDay;
    private final String tipLabel;
    private Font tipFont;
    private Font tipLabelFont;
    private float scaleFactor;

    /**
     * Constructor for TipOfTheDay
     *
     * @param bundleName         The name of the resource bundle containing the tips
     * @param referenceComponent A component to determine scaling
     */
    public TipOfTheDay(String title, String bundleName, Component referenceComponent) {
        this.bundleName = bundleName;
        tipLabel = title;
        tipOfTheDay = getRandomTip();
        updateScaleFactor(referenceComponent);
    }

    /**
     * Updates the scale factor and adjusts the font sizes accordingly.
     *
     * @param referenceComponent A component to determine scaling
     */
    public void updateScaleFactor(Component referenceComponent) {
        scaleFactor = (float) UIUtil.getResolutionScaleFactor(referenceComponent);
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(UIComponents.MainMenuBorder.getComp(), true);
        Font baseFont = new Font(skinSpec.fontName, Font.PLAIN, skinSpec.fontSize);
        tipLabelFont = baseFont.deriveFont(Font.BOLD, TIP_TITLE_FONT_SIZE * scaleFactor); // Tip title font
        tipFont = baseFont.deriveFont(Font.BOLD, TIP_FONT_SIZE * scaleFactor); // Tip font
    }

    /**
     * Gets the tip for today based on the current date
     *
     * @return A tip string for today
     */
    public String getTodaysTip() {
        LocalDate today = LocalDate.now();
        int dayOfYear = today.getDayOfYear();
        List<String> keyList = new ArrayList<>(I18n.getKeys(bundleName));
        int tipIndex = dayOfYear % keyList.size();
        return I18n.getTextAt(bundleName, keyList.get(tipIndex));
    }

    /**
     * Gets a random tip from the list
     *
     * @return A random tip string
     */
    public String getRandomTip() {
        List<String> keyList = new ArrayList<>(I18n.getKeys(bundleName));
        int randomIndex = (int) (Math.random() * keyList.size());
        return I18n.getTextAt(bundleName, keyList.get(randomIndex));
    }

    /**
     * Draws the Tip of the Day text with word wrap and styling.
     *
     * @param graphics2D      The Graphics2D object to draw on
     * @param referenceBounds The bounds of the reference component
     * @param position        The position of the tip (TOP_BORDER, BOTTOM_BORDER, etc.)
     */
    public void drawTipOfTheDay(Graphics2D graphics2D, Rectangle referenceBounds, Position position) {
        drawTipOfTheDay(graphics2D, referenceBounds, position, DEFAULT_USE_RADIAL_GRADIENT);
    }

    /**
     * Draws the Tip of the Day text with word wrap and styling.
     *
     * @param graphics2D        The Graphics2D object to draw on
     * @param referenceBounds   The bounds of the reference component
     * @param position          The position of the tip (TOP_BORDER, BOTTOM_BORDER, etc.)
     * @param useRadialGradient Whether to use a radial gradient for the background (only for corners)
     */
    public void drawTipOfTheDay(Graphics2D graphics2D, Rectangle referenceBounds, Position position,
          boolean useRadialGradient) {
        if (tipOfTheDay == null || tipOfTheDay.isEmpty() || tipLabelFont == null || tipFont == null) {
            return;
        }
        if (referenceBounds == null || referenceBounds.width <= 0 || referenceBounds.height <= 0) {
            return; // Cannot draw if referenceBounds is invalid
        }

        float scaledSidePadding = TIP_SIDE_PADDING * scaleFactor;
        float scaledBorderMargin = TIP_BORDER_MARGIN * scaleFactor;
        float scaledBgPadding = TIP_BACKGROUND_PADDING * scaleFactor;

        int currentAvailableTextWidth;
        float actualOppositeSidePadding = 0;

        currentAvailableTextWidth = (int) switch (position) {
            case BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER -> {
                actualOppositeSidePadding = referenceBounds.width * TIP_OPPOSITE_SIDE_PADDING_PERCENT;
                yield Math.floor(referenceBounds.width - scaledSidePadding - actualOppositeSidePadding);
            }
            default -> Math.floor(referenceBounds.width - (scaledSidePadding * 2));
        };

        if (currentAvailableTextWidth <= 0) {
            return; // Not enough space to draw text
        }

        Graphics2D tipGraphics = (Graphics2D) graphics2D.create();

        try {
            tipGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            tipGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            tipGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            tipGraphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            FontRenderContext frc = tipGraphics.getFontRenderContext();

            // "Tip of the Day" label
            AttributedString labelAS = new AttributedString(tipLabel);
            labelAS.addAttribute(TextAttribute.FONT, tipLabelFont);
            TextLayout labelLayout = new TextLayout(labelAS.getIterator(), frc);
            float labelHeight = labelLayout.getAscent() + labelLayout.getDescent() + labelLayout.getLeading();
            float labelWidth = (float) labelLayout.getBounds().getWidth();
            String actualTipContentToRender = mapVariables(tipOfTheDay);
            // We unwrap and wrap the tip content with HTML to ensure it is displayed correctly
            actualTipContentToRender = wrapTextWithHtml(unwrapHtml(tipOfTheDay));
            JTextPane htmlPane = createHtmlPane(actualTipContentToRender, tipFont, currentAvailableTextWidth, position);
            float totalTipHeight = htmlPane.getPreferredSize().height;

            // Positioning
            float totalBlockHeight = labelHeight + totalTipHeight;
            float startX = referenceBounds.x + scaledSidePadding;
            float startY = switch (position) {
                case BOTTOM_BORDER, BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER ->
                      referenceBounds.y + referenceBounds.height - scaledBorderMargin - totalBlockHeight;
                default -> referenceBounds.y + scaledBorderMargin;
            };

            // Background rectangle ---
            float bgRectX, bgRectWidth, bgRectTopY, bgRectBottomY, bgRectHeight;
            Paint bgPaint = null;

            Color baseRectColor = new Color(0f, 0f, 0f, TIP_BACKGROUND_BASE_OPACITY);
            Color fadeToRectColor = new Color(0f, 0f, 0f, TIP_BACKGROUND_FADE_TO_OPACITY);

            if (position == Position.BOTTOM_LEFT_CORNER) {
                bgRectX = referenceBounds.x;
                bgRectWidth = referenceBounds.width + scaledBgPadding - (actualOppositeSidePadding / 2.0f);
                bgRectWidth = Math.max(0, bgRectWidth);

                bgRectTopY = startY - scaledBgPadding;
                bgRectTopY = Math.max(bgRectTopY, referenceBounds.y);
                bgRectBottomY = referenceBounds.y + referenceBounds.height;
                bgRectHeight = bgRectBottomY - bgRectTopY;

                if (bgRectHeight > 0 && bgRectWidth > 0) {
                    if (!useRadialGradient) {
                        float fadeRegionPhysicalWidth = bgRectWidth * TIP_BACKGROUND_FADE_AREA_PERCENT;
                        fadeRegionPhysicalWidth = Math.max(0f, Math.min(fadeRegionPhysicalWidth, bgRectWidth));
                        // Opaque on the left, fades to transparent towards the right
                        bgPaint = new GradientPaint(bgRectX + fadeRegionPhysicalWidth,
                              bgRectTopY,
                              fadeToRectColor,
                              bgRectX,
                              bgRectTopY,
                              baseRectColor,
                              false);
                    } else {
                        bgRectWidth = bgRectWidth + scaledBgPadding;
                        bgRectTopY = bgRectTopY - scaledBgPadding;
                        bgRectHeight = bgRectHeight + scaledBgPadding;
                        float[] dist = { 0.0f, 0.7f, 1.0f };
                        Color[] colors = { baseRectColor, baseRectColor, fadeToRectColor };
                        AffineTransform gradientTx = AffineTransform.getTranslateInstance(bgRectX, bgRectBottomY);
                        gradientTx.scale(bgRectWidth, -bgRectHeight);
                        Point2D localCenter = new Point2D.Float(0f, 0f);
                        bgPaint = new RadialGradientPaint(localCenter,
                              1f,
                              localCenter,
                              dist,
                              colors,
                              RadialGradientPaint.CycleMethod.NO_CYCLE,
                              MultipleGradientPaint.ColorSpaceType.SRGB,
                              gradientTx);
                    }
                }
            } else if (position == Position.BOTTOM_RIGHT_CORNER) {
                bgRectWidth = referenceBounds.width + scaledBgPadding - (actualOppositeSidePadding / 2.0f);
                bgRectWidth = Math.max(0, bgRectWidth);
                bgRectX = referenceBounds.x + referenceBounds.width - bgRectWidth;

                bgRectTopY = startY - scaledBgPadding;
                bgRectTopY = Math.max(bgRectTopY, referenceBounds.y);
                bgRectBottomY = referenceBounds.y + referenceBounds.height;
                bgRectHeight = bgRectBottomY - bgRectTopY;

                if (bgRectHeight > 0 && bgRectWidth > 0) {
                    if (!useRadialGradient) {
                        float fadeRegionPhysicalWidth = bgRectWidth * TIP_BACKGROUND_FADE_AREA_PERCENT;
                        fadeRegionPhysicalWidth = Math.max(0f, Math.min(fadeRegionPhysicalWidth, bgRectWidth));
                        // Opaque on the right, fades to transparent towards the left
                        bgPaint = new GradientPaint(bgRectX,
                              bgRectTopY,
                              fadeToRectColor,
                              bgRectX + fadeRegionPhysicalWidth,
                              bgRectTopY,
                              baseRectColor,
                              false);
                    } else {
                        // Additional padding for radial gradient
                        bgRectWidth = bgRectWidth + scaledBgPadding;
                        bgRectTopY = bgRectTopY - scaledBgPadding;
                        bgRectHeight = bgRectHeight + scaledBgPadding;
                        float[] dist = { 0.0f, 0.7f, 1.0f };
                        Color[] colors = { baseRectColor, baseRectColor, fadeToRectColor };
                        AffineTransform gradientTx = AffineTransform.getTranslateInstance(bgRectX, bgRectBottomY);
                        gradientTx.scale(bgRectWidth, -bgRectHeight);
                        Point2D localCenter = new Point2D.Float(1f, 0f);
                        bgPaint = new RadialGradientPaint(localCenter,
                              1f,
                              localCenter,
                              dist,
                              colors,
                              RadialGradientPaint.CycleMethod.NO_CYCLE,
                              MultipleGradientPaint.ColorSpaceType.SRGB,
                              gradientTx);
                    }
                }
            } else if (position == Position.TOP_BORDER) {
                bgRectX = referenceBounds.x;
                bgRectWidth = referenceBounds.width;

                bgRectTopY = referenceBounds.y;
                bgRectBottomY = startY + totalBlockHeight + scaledBgPadding;
                bgRectBottomY = Math.min(bgRectBottomY,
                      referenceBounds.y + referenceBounds.height); // Don't go below the reference bottom
                bgRectHeight = bgRectBottomY - bgRectTopY;

                if (bgRectHeight > 0 && bgRectWidth > 0) {
                    float fadeRegionPhysicalHeight = bgRectHeight * TIP_BACKGROUND_FADE_AREA_PERCENT;
                    fadeRegionPhysicalHeight = Math.max(0f, Math.min(fadeRegionPhysicalHeight, bgRectHeight));

                    // Opaque at its top, fades to transparent towards its bottom
                    bgPaint = new GradientPaint(bgRectX,
                          bgRectBottomY - fadeRegionPhysicalHeight,
                          baseRectColor,
                          bgRectX,
                          bgRectBottomY,
                          fadeToRectColor,
                          false);
                }
            } else { // BOTTOM_BORDER
                bgRectX = referenceBounds.x;
                bgRectWidth = referenceBounds.width;

                bgRectTopY = startY - scaledBgPadding;
                bgRectTopY = Math.max(bgRectTopY, referenceBounds.y);
                bgRectBottomY = referenceBounds.y + referenceBounds.height;
                bgRectHeight = bgRectBottomY - bgRectTopY;

                if (bgRectHeight > 0 && bgRectWidth > 0) {
                    float fadeRegionPhysicalHeight = bgRectHeight * TIP_BACKGROUND_FADE_AREA_PERCENT;
                    fadeRegionPhysicalHeight = Math.max(0f, Math.min(fadeRegionPhysicalHeight, bgRectHeight));

                    // Transparent at its top, fades to opaque towards its bottom
                    bgPaint = new GradientPaint(bgRectX,
                          bgRectTopY,
                          fadeToRectColor,
                          bgRectX,
                          bgRectTopY + fadeRegionPhysicalHeight,
                          baseRectColor,
                          false);
                }
            }

            if (bgPaint != null && bgRectHeight > 0 && bgRectWidth > 0) {
                java.awt.geom.Rectangle2D.Float fullBackgroundShape = new java.awt.geom.Rectangle2D.Float(bgRectX,
                      bgRectTopY,
                      bgRectWidth,
                      bgRectHeight);
                tipGraphics.setPaint(bgPaint);
                tipGraphics.fill(fullBackgroundShape);
            }
            // --- End background rectangle

            // Draw the text (outline then fill)
            BasicStroke outlineStroke = new BasicStroke(STROKE_WIDTH * scaleFactor,
                  BasicStroke.CAP_ROUND,
                  BasicStroke.JOIN_ROUND);
            tipGraphics.setStroke(outlineStroke);

            // Draw Label
            float labelDrawX = switch (position) {
                case BOTTOM_LEFT_CORNER -> startX; // Left align
                case BOTTOM_RIGHT_CORNER ->
                      referenceBounds.x + referenceBounds.width - labelWidth - scaledSidePadding; // Right align
                default -> {
                    labelDrawX = startX + (currentAvailableTextWidth - labelWidth) / 2; // Center label
                    yield Math.max(startX, labelDrawX);
                }
            };
            float labelDrawY = startY + labelLayout.getAscent();

            drawTipTitle(tipGraphics, labelLayout, labelDrawX, labelDrawY, TIP_TITLE_FONT_COLOR);

            float tipStartY = startY + labelHeight;
            // Render HTML content
            drawHtmlTip(tipGraphics, htmlPane, startX, tipStartY,
                    currentAvailableTextWidth, position, referenceBounds, scaledSidePadding);

        } finally {
            tipGraphics.dispose();
        }
    }

    private String unwrapHtml(String originalHtml) {
        if (originalHtml == null) {
            return "";
        }
        String bodyContent = originalHtml.trim();
        if (originalHtml.toLowerCase().startsWith("<html>") && originalHtml.toLowerCase().endsWith("</html>")) {
            bodyContent = originalHtml.substring(6, originalHtml.length() - 7).trim();
            int bodyTagStartIndex = bodyContent.toLowerCase().indexOf("<body>");
            if (bodyTagStartIndex != -1) {
                int contentStartIndex = bodyContent.indexOf('>', bodyTagStartIndex) + 1;
                int bodyTagEndIndex = bodyContent.toLowerCase().lastIndexOf("</body>");
                if (bodyTagEndIndex > contentStartIndex) {
                    bodyContent = bodyContent.substring(contentStartIndex, bodyTagEndIndex).trim();
                } else {
                    // Use content within <html> but outside/malformed <body>
                    bodyContent = bodyContent.substring(bodyTagStartIndex + "<body>".length()).trim();
                    if(bodyContent.toLowerCase().endsWith("</body>")){
                        bodyContent = bodyContent.substring(0, bodyContent.length() - "</body>".length()).trim();
                    }
                }
            }
        }
        return bodyContent;
    }

    private String wrapTextWithHtml(String bodyContent) {
        if (bodyContent == null) {
            bodyContent = "";
        }
        return "<html>" + bodyContent + "</html>";
    }

    /**
     * Creates a JLabel configured for HTML rendering
     */
    private JTextPane createHtmlPane(String htmlText, Font font, int width, Position position) {
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setFont(font);
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        String fontWeight = font.isBold() ? "bold" : "normal";
        String textAlign = switch (position) {
            case BOTTOM_LEFT_CORNER -> "left";
            case BOTTOM_RIGHT_CORNER -> "right";
            default -> "center"; // TOP_BORDER, BOTTOM_BORDER
        };
        styleSheet.addRule("html { " +
            "font-family: '" + font.getFamily() + "'; " +
            "font-size: " + Math.ceil(font.getSize()*0.75) + "pt; " +
            "font-weight: " + fontWeight + "; " +
            "margin: 0; " +
            "padding: 0; " +
            "width: " + width + "px; " +
            "max-width: " + width + "px; " +
            "text-align: " + textAlign + "; " +
            "}");
        textPane.setEditorKit(kit);
        textPane.setMargin(new Insets(0, 0, 0, 0));
        textPane.setBorder(null);
        textPane.setText(htmlText);
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setFocusable(false);
        textPane.setSize(width, Short.MAX_VALUE);
        return textPane;
    }

    /**
     * Draws HTML content with outline and fill
     */
    private void drawHtmlTip(Graphics2D graphics, JTextPane htmlPane, float startX, float startY,
                            int availableWidth, Position position, Rectangle referenceBounds, float scaledSidePadding) {
        if (htmlPane == null || htmlPane.getText() == null || htmlPane.getText().trim().isEmpty()) {
            return;
        }
        Dimension preferredSize = htmlPane.getPreferredSize();

        if (preferredSize.width <= 0 || preferredSize.height <= 0) {
            // No valid size to draw
            return;
        }

        int contentWidthToDraw = Math.min(preferredSize.width, availableWidth);
        int actualHeight = preferredSize.height;

        float drawX;
        switch (position) {
            case BOTTOM_LEFT_CORNER:
                drawX = startX;
                break;
            case BOTTOM_RIGHT_CORNER:
                drawX = referenceBounds.x + referenceBounds.width - contentWidthToDraw - scaledSidePadding;
                break;
            default: // TOP_BORDER, BOTTOM_BORDER
                drawX = startX + (availableWidth - contentWidthToDraw) / 2f;
                drawX = Math.max(startX, drawX);
                break;
        }
        htmlPane.setBounds(0, 0, contentWidthToDraw, actualHeight);

        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2d.translate(drawX, startY);

            float strokeThickness = STROKE_WIDTH * scaleFactor;
            int stroke = Math.max(1, (int) Math.ceil(strokeThickness));

            // Outline Pass:
            htmlPane.setForeground(TIP_STROKE_COLOR);
            for (int dy_offset = -stroke; dy_offset <= stroke; dy_offset++) {
                for (int dx_offset = -stroke; dx_offset <= stroke; dx_offset++) {
                    if (dx_offset != 0 || dy_offset != 0) {
                        AffineTransform originalTransform = g2d.getTransform();
                        g2d.translate(dx_offset, dy_offset);
                        htmlPane.paint(g2d);
                        g2d.setTransform(originalTransform);
                    }
                }
            }
            htmlPane.setForeground(TIP_FONT_COLOR);
            htmlPane.paint(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawTipTitle(Graphics2D tipGraphics, TextLayout tipLayout,
          float lineDrawX, float lineDrawY, Color tipFontColor) {
        AffineTransform oldTransform = tipGraphics.getTransform();
        tipGraphics.translate(lineDrawX, lineDrawY);
        Shape tipShape = tipLayout.getOutline(null);
        tipGraphics.setColor(TIP_STROKE_COLOR); // Outline color
        tipGraphics.draw(tipShape); // Draw outline
        tipGraphics.setColor(tipFontColor); // Fill color
        tipGraphics.fill(tipShape); // Draw fill
        tipGraphics.setTransform(oldTransform);
    }

    private String mapVariables(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        text = mapKeybinds(text);
        return text;
    }

    private String mapKeybinds(String text) {
        if (!text.contains(KEYBIND_VARIABLE_PREFIX)) {
            return text; // No keybind variables to replace
        }
        Set<String> keybindCommands = new java.util.HashSet<>();
        Pattern pattern = Pattern.compile("\\"+KEYBIND_VARIABLE_PREFIX+"([^}]+)\\"+VARIABLE_SUFFIX);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            keybindCommands.add(matcher.group(1));
        }
        for (String cmd : keybindCommands) {
            String keybindVariable = KEYBIND_VARIABLE_PREFIX + cmd + VARIABLE_SUFFIX;
            KeyCommandBind kcb = null;
            try {
                // We try to find it by enum first (faster)
                kcb = KeyCommandBind.valueOf(cmd.toUpperCase());
            } catch (IllegalArgumentException e) {
                // If not found, we look it up by command name
                kcb = KeyCommandBind.getBindByCmd(cmd);
            }
            if (kcb != null) {
                String modifier = KeyEvent.getModifiersExText(kcb.modifiers);
                String key = KeyEvent.getKeyText(kcb.key);
                String keybind = modifier.isEmpty() ? key : modifier + "+" + key;
                text = text.replace(keybindVariable, keybind);
            } else {
                text = text.replace(keybindVariable, "UNASSIGNED");
            }
        }
        return text;
    }
}
