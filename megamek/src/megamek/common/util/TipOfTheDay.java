/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.text.AttributedString;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.internationalization.Internationalization;

/**
 * @author Drake
 * 
 * Provides a daily/random tip functionality
 */
public class TipOfTheDay {
    private static final String TIP_BUNDLE_KEY = "TipOfTheDay.tip.";
    private static final String TIP_BUNDLE_TITLE_KEY = "TipOfTheDay.title.text";
    private static final int TIP_BORDER_MARGIN = 60;
    private static final int TIP_SIDE_PADDING = 20;
    private static final float TIP_TITLE_FONT_SIZE = 24f;
    private static final float TIP_FONT_SIZE = 32f;
    private static final float STROKE_WIDTH = 4.0f;
    private static final Color TIP_STROKE_COLOR = Color.BLACK;
    private static final Color TIP_TITLE_FONT_COLOR = Color.WHITE;
    private static final Color TIP_FONT_COLOR = Color.WHITE;
    private final String bundleName;
    private final int countTips;
    private final String tipOfTheDay;
    private final String tipLabel;
    private Font tipFont;
    private Font tipLabelFont;

    // Prevent instantiation
    public TipOfTheDay(String bundleName) {
        this.bundleName = bundleName;
        countTips = countTips();
        tipLabel = Internationalization.getTextAt(bundleName, TIP_BUNDLE_TITLE_KEY);
        tipOfTheDay = getRandomTip();

        SkinSpecification skinSpec = SkinXMLHandler.getSkin(UIComponents.MainMenuBorder.getComp(), true);
        Font baseFont = new Font(skinSpec.fontName, Font.PLAIN, skinSpec.fontSize);
        tipLabelFont = baseFont.deriveFont(Font.BOLD, TIP_TITLE_FONT_SIZE); // Tip title font
        tipFont = baseFont.deriveFont(Font.BOLD, TIP_FONT_SIZE); // Tip font
    }

    /**
     * Count the number of tips in the resource bundle
     */
    private int countTips() {
        int count = 0;
        try {
            while (true) {
                count++;
                String tip = Internationalization.getTextAt(bundleName, TIP_BUNDLE_KEY + count);
                if (tip.startsWith("!") && tip.endsWith("!")) {
                    return count - 1;
                }
            }
        } catch (Exception e) {
            // When we get an exception, we've found all tips
            return count - 1;
        }
    }

    /**
     * Gets the tip for today based on the current date
     * 
     * @return A tip string for today
     */
    public String getTodaysTip() {
        LocalDate today = LocalDate.now();
        int dayOfYear = today.getDayOfYear();
        int tipIndex = (dayOfYear % countTips) + 1;
        return Internationalization.getTextAt(bundleName, TIP_BUNDLE_KEY + tipIndex);
    }

    /**
     * Gets a random tip from the list
     * 
     * @return A random tip string
     */
    public String getRandomTip() {
        int randomIndex = (int) (Math.random() * countTips) + 1;
        return Internationalization.getTextAt(bundleName, TIP_BUNDLE_KEY + randomIndex);
    }

    /**
     * Draws the Tip of the Day text with word wrap and styling.
     */
    public void drawTipOfTheDay(Graphics2D g2d, Rectangle referenceBounds, boolean positionBottom) {
        if (tipOfTheDay == null || tipOfTheDay.isEmpty() || tipLabelFont == null || tipFont == null) {
            return;
        }
        if (referenceBounds == null || referenceBounds.width <= 0 || referenceBounds.height <= 0) {
            return; // Cannot draw if referenceBounds is invalid
        }
        float availableWidth = referenceBounds.width - (TIP_SIDE_PADDING * 2);
        if (availableWidth <= 0)
            return; // Not enough space to draw

        Graphics2D g = (Graphics2D) g2d.create();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            FontRenderContext frc = g.getFontRenderContext();

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
            while (measurer.getPosition() < tipAS.getIterator().getEndIndex()) {
                TextLayout layout = measurer.nextLayout(availableWidth);
                if (layout != null) {
                    tipLayouts.add(layout);
                    totalTipHeight += layout.getAscent() + layout.getDescent() + layout.getLeading();
                } else {
                    break; // Should not happen with LineBreakMeasurer unless width is tiny
                }
                if (measurer.getPosition() == layout.getCharacterCount() + measurer.getPosition()
                        && measurer.getPosition() < tipAS.getIterator().getEndIndex()) {
                    break;
                }
            }

            // Positioning// Positioning
            float totalBlockHeight = labelHeight + totalTipHeight;
            float startY;
            if (positionBottom) {
                startY = referenceBounds.y + referenceBounds.height - TIP_BORDER_MARGIN - totalBlockHeight;
            } else {
                startY = referenceBounds.y + TIP_BORDER_MARGIN;
            }
            float startX = referenceBounds.x + TIP_SIDE_PADDING;

            // Draw the text (outline then fill)
            BasicStroke outlineStroke = new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g.setStroke(outlineStroke);

            // Draw Label
            float labelDrawX = startX + (availableWidth - labelWidth) / 2; // Center label
            float labelDrawY = startY + labelLayout.getAscent();
            Shape labelShape = labelLayout.getOutline(null);

            g.translate(labelDrawX, labelDrawY);
            g.setColor(TIP_STROKE_COLOR);
            g.draw(labelShape); // Draw outline
            g.setColor(TIP_TITLE_FONT_COLOR); // Fill color
            g.fill(labelShape); // Draw fill
            g.translate(-labelDrawX, -labelDrawY); // Translate back

            // Draw Tip Lines
            float currentY = startY + labelHeight; // Start drawing tips below the label
            for (TextLayout tipLayout : tipLayouts) {
                float lineAscent = tipLayout.getAscent();
                float lineHeight = lineAscent + tipLayout.getDescent() + tipLayout.getLeading();
                float lineDrawY = currentY + lineAscent; // Baseline for this line
                float lineWidth = (float) tipLayout.getBounds().getWidth();

                float lineDrawX = startX + (availableWidth - lineWidth) / 2f; // Center line
                lineDrawX = Math.max(startX, lineDrawX); // Ensure it doesn't go out of bounds
                Shape tipShape = tipLayout.getOutline(AffineTransform.getTranslateInstance(lineDrawX, lineDrawY));
                g.setColor(TIP_STROKE_COLOR); // Outline color
                g.draw(tipShape); // Draw outline
                g.setColor(TIP_FONT_COLOR); // Fill color
                g.fill(tipShape); // Draw fill

                currentY += lineHeight;
            }

        } finally {
            g.dispose();
        }
    }

}