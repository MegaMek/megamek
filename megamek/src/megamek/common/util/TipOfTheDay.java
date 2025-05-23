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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.AttributedString;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.internationalization.I18n;

/**
 * @author Drake
 * 
 * Provides a daily/random tip functionality
 */
public class TipOfTheDay {
    // Enum for positioning the tip text
    public enum Position {
        TOP_BORDER,
        BOTTOM_BORDER,
        BOTTOM_LEFT_CORNER,
        BOTTOM_RIGHT_CORNER,
    }

    private static final int TIP_BORDER_MARGIN = 40;
    private static final int TIP_SIDE_PADDING = 20;
    private static final float TIP_OPPOSITE_SIDE_PADDING = 0.4f; //% For corner positioning (example: BOTTOM_LEFT_CORNER)
    private static final float TIP_TITLE_FONT_SIZE = 14f;
    private static final float TIP_FONT_SIZE = 18f;
    private static final float STROKE_WIDTH = 3.0f;
    private static final Color TIP_STROKE_COLOR = Color.BLACK;
    private static final Color TIP_TITLE_FONT_COLOR = Color.WHITE;
    private static final Color TIP_FONT_COLOR = Color.WHITE;

    private static final float TIP_BACKGROUND_PADDING = TIP_BORDER_MARGIN;
    private static final float TIP_BACKGROUND_BASE_OPACITY = 0.8f;
    private static final float TIP_BACKGROUND_FADE_TO_OPACITY = 0.0f;
    private static final float TIP_BACKGROUND_FADE_AREA_HEIGHT_PERCENT = 1.0f;
    private static final boolean DEFAULT_USE_RADIAL_GRADIENT = false;

    private final String bundleName;
    private final String tipOfTheDay;
    private final String tipLabel;
    private Font tipFont;
    private Font tipLabelFont;
    private float scaleFactor;

    /**
     * Constructor for TipOfTheDay
     * 
     * @param bundleName The name of the resource bundle containing the tips
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
        int tipIndex = (int) (dayOfYear % keyList.size());
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
     * @param graphics2D The Graphics2D object to draw on
     * @param referenceBounds The bounds of the reference component
     * @param position The position of the tip (TOP_BORDER, BOTTOM_BORDER, etc.)

     */
    public void drawTipOfTheDay(Graphics2D graphics2D, Rectangle referenceBounds, Position position) {
        drawTipOfTheDay(graphics2D, referenceBounds, position, DEFAULT_USE_RADIAL_GRADIENT);
    }
    /**
     * Draws the Tip of the Day text with word wrap and styling.
     * 
     * @param graphics2D The Graphics2D object to draw on
     * @param referenceBounds The bounds of the reference component
     * @param position The position of the tip (TOP_BORDER, BOTTOM_BORDER, etc.)
     * @param useRadialGradient Whether to use a radial gradient for the background (only for corners)
     */
    public void drawTipOfTheDay(Graphics2D graphics2D, Rectangle referenceBounds, Position position, boolean useRadialGradient) {
        if (tipOfTheDay == null || tipOfTheDay.isEmpty() || tipLabelFont == null || tipFont == null) {
            return;
        }
        if (referenceBounds == null || referenceBounds.width <= 0 || referenceBounds.height <= 0) {
            return; // Cannot draw if referenceBounds is invalid
        }
        
        float scaledSidePadding = TIP_SIDE_PADDING * scaleFactor;
        float scaledBorderMargin = TIP_BORDER_MARGIN * scaleFactor;
        float scaledBgPadding = TIP_BACKGROUND_PADDING * scaleFactor;

        float currentAvailableTextWidth;
        float actualOppositeSidePadding = 0;
        
        switch (position) {
            case BOTTOM_LEFT_CORNER:
            case BOTTOM_RIGHT_CORNER:
            actualOppositeSidePadding = referenceBounds.width * TIP_OPPOSITE_SIDE_PADDING;
            currentAvailableTextWidth = referenceBounds.width - scaledSidePadding - actualOppositeSidePadding;
            break;
            case TOP_BORDER:
            case BOTTOM_BORDER:
            default:
                currentAvailableTextWidth = referenceBounds.width - (scaledSidePadding * 2);
                break;
        }
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

            // "Tip of the Day:" label
            AttributedString labelAS = new AttributedString(tipLabel);
            labelAS.addAttribute(TextAttribute.FONT, tipLabelFont);
            TextLayout labelLayout = new TextLayout(labelAS.getIterator(), frc);
            float labelHeight = labelLayout.getAscent() + labelLayout.getDescent() + labelLayout.getLeading();
            float labelWidth = (float) labelLayout.getBounds().getWidth();

            // Actual tip text with word wrapping
            AttributedString tipAS = new AttributedString(tipOfTheDay);
            tipAS.addAttribute(TextAttribute.FONT, tipFont);
            LineBreakMeasurer measurer = new LineBreakMeasurer(tipAS.getIterator(), frc);
            List<TextLayout> tipLayouts = new ArrayList<>();
            float totalTipHeight = 0;
            measurer.setPosition(0);
            int previousPosition = -1; 
            while (measurer.getPosition() < tipAS.getIterator().getEndIndex()) {
                // Check if the position hasn't advanced, indicating a potential infinite loop
                if (measurer.getPosition() == previousPosition) {
                    break;
                }
                previousPosition = measurer.getPosition();
                TextLayout layout = measurer.nextLayout(currentAvailableTextWidth);
                if (layout != null) {
                    tipLayouts.add(layout);
                    totalTipHeight += layout.getAscent() + layout.getDescent() + layout.getLeading();
                } else {
                    break; // Should not happen with LineBreakMeasurer unless width is tiny
                }
            }

            // Positioning
            float totalBlockHeight = labelHeight + totalTipHeight;
            float startX = referenceBounds.x + scaledSidePadding;
            float startY;

            switch (position) {
                case BOTTOM_BORDER:
                case BOTTOM_LEFT_CORNER:
                case BOTTOM_RIGHT_CORNER:
                    startY = referenceBounds.y + referenceBounds.height - scaledBorderMargin - totalBlockHeight;
                    break;
                default:
                    startY = referenceBounds.y + scaledBorderMargin;
            }

            // Background rectangle ---
            float bgRectX, bgRectWidth, bgRectTopY, bgRectBottomY, bgRectHeight;
            Paint bgPaint = null;

            Color baseRectColor = new Color(0f, 0f, 0f, TIP_BACKGROUND_BASE_OPACITY);
            Color fadeToRectColor = new Color(0f, 0f, 0f, TIP_BACKGROUND_FADE_TO_OPACITY);

            if (position == Position.BOTTOM_LEFT_CORNER) {
                bgRectX = referenceBounds.x;
                bgRectWidth = referenceBounds.width + scaledBgPadding - actualOppositeSidePadding;
                bgRectWidth = Math.max(0, bgRectWidth);

                bgRectTopY = startY - scaledBgPadding;
                bgRectTopY = Math.max(bgRectTopY, referenceBounds.y);
                bgRectBottomY = referenceBounds.y + referenceBounds.height;
                bgRectHeight = bgRectBottomY - bgRectTopY;

                if (bgRectHeight > 0 && bgRectWidth > 0) {
                    if (!useRadialGradient) {
                        float fadeRegionPhysicalWidth = bgRectWidth * TIP_BACKGROUND_FADE_AREA_HEIGHT_PERCENT;
                        fadeRegionPhysicalWidth = Math.max(0f, Math.min(fadeRegionPhysicalWidth, bgRectWidth));
                        // Opaque on the left, fades to transparent towards the right
                        bgPaint = new GradientPaint(
                            bgRectX + fadeRegionPhysicalWidth, bgRectTopY, fadeToRectColor,
                            bgRectX, bgRectTopY, baseRectColor,
                            false);
                    } else {
                        bgRectWidth = bgRectWidth + scaledBgPadding;
                        bgRectTopY = bgRectTopY - scaledBgPadding;
                        bgRectHeight = bgRectHeight + scaledBgPadding;
                        float[] dist = {0.0f, 0.7f, 1.0f};
                        Color[] colors = {baseRectColor, baseRectColor, fadeToRectColor};
                        AffineTransform gradientTx = AffineTransform.getTranslateInstance(bgRectX, bgRectBottomY);
                        gradientTx.scale(bgRectWidth, -bgRectHeight);
                        Point2D localCenter = new Point2D.Float(0f, 0f);
                        bgPaint = new RadialGradientPaint(localCenter, 1f, localCenter,
                                                                            dist, colors,
                                                                            RadialGradientPaint.CycleMethod.NO_CYCLE,
                                                                            MultipleGradientPaint.ColorSpaceType.SRGB,
                                                                            gradientTx);
                    }
                }
            } else if (position == Position.BOTTOM_RIGHT_CORNER) {
                bgRectWidth = referenceBounds.width + scaledBgPadding - actualOppositeSidePadding;
                bgRectWidth = Math.max(0, bgRectWidth);
                bgRectX = referenceBounds.width - bgRectWidth;

                bgRectTopY = startY - scaledBgPadding;
                bgRectTopY = Math.max(bgRectTopY, referenceBounds.y);
                bgRectBottomY = referenceBounds.y + referenceBounds.height;
                bgRectHeight = bgRectBottomY - bgRectTopY;

                if (bgRectHeight > 0 && bgRectWidth > 0) {
                    if (!useRadialGradient) {
                        float fadeRegionPhysicalWidth = bgRectWidth * TIP_BACKGROUND_FADE_AREA_HEIGHT_PERCENT;
                        fadeRegionPhysicalWidth = Math.max(0f, Math.min(fadeRegionPhysicalWidth, bgRectWidth));
                        // Opaque on the right, fades to transparent towards the left
                        bgPaint = new GradientPaint(
                            bgRectX, bgRectTopY, fadeToRectColor,
                            bgRectX + fadeRegionPhysicalWidth, bgRectTopY, baseRectColor,
                            false);
                    } else {
                        // Additional padding for radial gradient
                        bgRectWidth = bgRectWidth + scaledBgPadding;
                        bgRectTopY = bgRectTopY - scaledBgPadding;
                        bgRectHeight = bgRectHeight + scaledBgPadding;
                        float[] dist = {0.0f, 0.7f, 1.0f};
                        Color[] colors = {baseRectColor, baseRectColor, fadeToRectColor};
                        AffineTransform gradientTx = AffineTransform.getTranslateInstance(bgRectX, bgRectBottomY);
                        gradientTx.scale(bgRectWidth, -bgRectHeight);
                        Point2D localCenter = new Point2D.Float(1f, 0f);
                        bgPaint = new RadialGradientPaint(localCenter, 1f, localCenter,
                                                                            dist, colors,
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
                bgRectBottomY = Math.min(bgRectBottomY, referenceBounds.y + referenceBounds.height); // Don't go below reference bottom
                bgRectHeight = bgRectBottomY - bgRectTopY;

                if (bgRectHeight > 0 && bgRectWidth > 0) {
                    float fadeRegionPhysicalHeight = bgRectHeight * TIP_BACKGROUND_FADE_AREA_HEIGHT_PERCENT;
                    fadeRegionPhysicalHeight = Math.max(0f, Math.min(fadeRegionPhysicalHeight, bgRectHeight));
                    
                    // Opaque at its top, fades to transparent towards its bottom
                    bgPaint = new GradientPaint(
                            bgRectX, bgRectBottomY - fadeRegionPhysicalHeight, baseRectColor,
                            bgRectX, bgRectBottomY, fadeToRectColor,
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
                    float fadeRegionPhysicalHeight = bgRectHeight * TIP_BACKGROUND_FADE_AREA_HEIGHT_PERCENT;
                    fadeRegionPhysicalHeight = Math.max(0f, Math.min(fadeRegionPhysicalHeight, bgRectHeight));

                    // Transparent at its top, fades to opaque towards its bottom
                    bgPaint = new GradientPaint(
                            bgRectX, bgRectTopY, fadeToRectColor,
                            bgRectX, bgRectTopY + fadeRegionPhysicalHeight, baseRectColor,
                            false);
                }
            }

            if (bgPaint != null && bgRectHeight > 0 && bgRectWidth > 0) {
                java.awt.geom.Rectangle2D.Float fullBackgroundShape =
                        new java.awt.geom.Rectangle2D.Float(bgRectX, bgRectTopY, bgRectWidth, bgRectHeight);
                tipGraphics.setPaint(bgPaint);
                tipGraphics.fill(fullBackgroundShape);
            }
            // --- End background rectangle


            // Draw the text (outline then fill)
            BasicStroke outlineStroke = new BasicStroke(STROKE_WIDTH * scaleFactor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            tipGraphics.setStroke(outlineStroke);

            // Draw Label
            
            float labelDrawX;
            switch (position) {
                case BOTTOM_LEFT_CORNER:
                    labelDrawX = startX; // Left align
                    break;
                case BOTTOM_RIGHT_CORNER:
                    labelDrawX = referenceBounds.x + referenceBounds.width - labelWidth - scaledSidePadding; // Right align
                    break;
                default:
                    labelDrawX = startX + (currentAvailableTextWidth - labelWidth) / 2; // Center label
                    labelDrawX = Math.max(startX, labelDrawX);
            }
            float labelDrawY = startY + labelLayout.getAscent();

            AffineTransform oldTransform = tipGraphics.getTransform();
            tipGraphics.translate(labelDrawX, labelDrawY);
            Shape labelShape = labelLayout.getOutline(null);
            tipGraphics.setColor(TIP_STROKE_COLOR);
            tipGraphics.draw(labelShape); // Draw outline
            tipGraphics.setColor(TIP_TITLE_FONT_COLOR); // Fill color
            tipGraphics.fill(labelShape); // Draw fill
            tipGraphics.setTransform(oldTransform);

            // Draw Tip Lines
            float currentY = startY + labelHeight; // Start drawing tips below the label
            for (TextLayout tipLayout : tipLayouts) {
                float lineAscent = tipLayout.getAscent();
                float lineHeight = lineAscent + tipLayout.getDescent() + tipLayout.getLeading();
                float lineWidth = (float) tipLayout.getBounds().getWidth();

                float lineDrawX;
                switch (position) {
                    case BOTTOM_LEFT_CORNER:
                        lineDrawX = startX; // Left align
                        break;
                    case BOTTOM_RIGHT_CORNER:
                        lineDrawX = referenceBounds.x + referenceBounds.width - lineWidth - scaledSidePadding; // Right align
                        break;
                    default:
                        lineDrawX = startX + (currentAvailableTextWidth - lineWidth) / 2f; // Center line
                        lineDrawX = Math.max(startX, lineDrawX);
                }
                float lineDrawY = currentY + lineAscent; // Baseline for this line

                oldTransform = tipGraphics.getTransform();
                tipGraphics.translate(lineDrawX, lineDrawY);
                lineDrawX = Math.max(startX, lineDrawX); // Ensure it doesn't go out of bounds
                Shape tipShape = tipLayout.getOutline(null);
                tipGraphics.setColor(TIP_STROKE_COLOR); // Outline color
                tipGraphics.draw(tipShape); // Draw outline
                tipGraphics.setColor(TIP_FONT_COLOR); // Fill color
                tipGraphics.fill(tipShape); // Draw fill
                tipGraphics.setTransform(oldTransform);

                currentY += lineHeight;
            }

        } finally {
            tipGraphics.dispose();
        }
    }

}