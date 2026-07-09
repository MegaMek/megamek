/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.JPanel;
import javax.swing.UIManager;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.analysis.DamageProfile;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * A panel that renders a unit's damage-versus-range analysis chart from its {@link DamageProfile}:
 * the maximum, expected (to-hit weighted), and sustained (heat-balanced) damage curves.
 *
 * <p>Shared by the unit viewers of MegaMek and MegaMekLab: it is a tab in
 * {@link EntityViewPane} (both unit selectors), and MegaMekLab's editor preview can mount the
 * same panel for live refresh while editing. Call {@link #setEntity(Entity)} to update; null
 * clears the display.</p>
 *
 * <p>Curve colors are fixed hues readable on both light and dark themes; the axes and text use
 * the current look-and-feel colors.</p>
 */
public class DamageAnalysisPanel extends JPanel {

    private static final Color MAX_DAMAGE_COLOR = new Color(76, 175, 80);      // green
    private static final Color EXPECTED_DAMAGE_COLOR = new Color(211, 84, 66); // red
    private static final Color SUSTAINED_DAMAGE_COLOR = new Color(66, 106, 211); // blue
    private static final int FILL_ALPHA = 70;

    private DamageProfile profile;
    private String unitName = "";

    public DamageAnalysisPanel() {
        setName("damageAnalysisPanel");
        setMinimumSize(UIUtil.scaleForGUI(400, 300));
    }

    /**
     * Updates the displayed unit. The damage profile is computed once here, not per repaint.
     *
     * @param entity the unit to analyze, or null to clear the display
     */
    public void setEntity(@Nullable Entity entity) {
        if (entity == null) {
            profile = null;
            unitName = "";
        } else {
            profile = DamageProfile.of(entity, false);
            unitName = entity.getShortName();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if ((profile == null) || !profile.hasWeapons()) {
                paintEmptyMessage(graphics2D);
                return;
            }
            paintChart(graphics2D);
        } finally {
            graphics2D.dispose();
        }
    }

    private void paintEmptyMessage(Graphics2D graphics2D) {
        String message = (profile == null)
              ? Messages.getString("DamageAnalysisPanel.noUnit")
              : Messages.getString("DamageAnalysisPanel.noWeapons");
        graphics2D.setColor(UIManager.getColor("Label.foreground"));
        FontMetrics metrics = graphics2D.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(message)) / 2;
        int y = (getHeight() + metrics.getAscent()) / 2;
        graphics2D.drawString(message, x, y);
    }

    private void paintChart(Graphics2D graphics2D) {
        Color foreground = UIManager.getColor("Label.foreground");
        Color gridColor = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 60);

        FontMetrics metrics = graphics2D.getFontMetrics();
        int marginLeft = UIUtil.scaleForGUI(48);
        int marginRight = UIUtil.scaleForGUI(16);
        int marginTop = UIUtil.scaleForGUI(28);
        int marginBottom = UIUtil.scaleForGUI(64);

        int plotWidth = getWidth() - marginLeft - marginRight;
        int plotHeight = getHeight() - marginTop - marginBottom;
        if ((plotWidth < 50) || (plotHeight < 50)) {
            return;
        }

        int maxRange = profile.maxRange();
        double damageCeiling = niceCeiling(profile.maxDamage(peakMaxRange()));

        // Title: unit name and the gunnery the expected curves assume
        graphics2D.setColor(foreground);
        String title = unitName + "  -  "
              + Messages.getString("DamageAnalysisPanel.gunnery", profile.gunnery());
        graphics2D.drawString(title, marginLeft, marginTop - UIUtil.scaleForGUI(10));

        // Grid and axis labels
        int ySteps = 5;
        for (int step = 0; step <= ySteps; step++) {
            double damageValue = damageCeiling * step / ySteps;
            int y = marginTop + plotHeight - (int) Math.round(plotHeight * step / (double) ySteps);
            graphics2D.setColor(gridColor);
            graphics2D.drawLine(marginLeft, y, marginLeft + plotWidth, y);
            graphics2D.setColor(foreground);
            String label = String.valueOf((int) Math.round(damageValue));
            graphics2D.drawString(label, marginLeft - metrics.stringWidth(label) - UIUtil.scaleForGUI(6),
                  y + (metrics.getAscent() / 2));
        }
        int xStep = Math.max(1, (int) Math.ceil(maxRange / 8.0));
        for (int range = 1; range <= maxRange; range += xStep) {
            int x = xForRange(range, maxRange, marginLeft, plotWidth);
            graphics2D.setColor(gridColor);
            graphics2D.drawLine(x, marginTop, x, marginTop + plotHeight);
            graphics2D.setColor(foreground);
            String label = String.valueOf(range);
            graphics2D.drawString(label, x - (metrics.stringWidth(label) / 2),
                  marginTop + plotHeight + metrics.getAscent() + UIUtil.scaleForGUI(4));
        }

        // Curves back to front: maximum, expected, sustained
        paintCurve(graphics2D, MAX_DAMAGE_COLOR, range -> profile.maxDamage(range),
              maxRange, damageCeiling, marginLeft, marginTop, plotWidth, plotHeight);
        paintCurve(graphics2D, EXPECTED_DAMAGE_COLOR, range -> profile.expectedDamage(range),
              maxRange, damageCeiling, marginLeft, marginTop, plotWidth, plotHeight);
        paintCurve(graphics2D, SUSTAINED_DAMAGE_COLOR, range -> profile.sustainedDamage(range),
              maxRange, damageCeiling, marginLeft, marginTop, plotWidth, plotHeight);

        paintLegend(graphics2D, marginLeft, marginTop + plotHeight + metrics.getHeight()
              + UIUtil.scaleForGUI(14), plotWidth);
    }

    /** The range where the maximum-damage curve is highest, used to pick the chart's y ceiling. */
    private int peakMaxRange() {
        int bestRange = 1;
        double bestDamage = 0;
        for (int range = 1; range <= profile.maxRange(); range++) {
            if (profile.maxDamage(range) > bestDamage) {
                bestDamage = profile.maxDamage(range);
                bestRange = range;
            }
        }
        return bestRange;
    }

    @FunctionalInterface
    private interface CurveFunction {
        double valueAt(int range);
    }

    private void paintCurve(Graphics2D graphics2D, Color color, CurveFunction curve, int maxRange,
          double damageCeiling, int marginLeft, int marginTop, int plotWidth, int plotHeight) {
        // Step outline: damage is constant across each hex, so the curve moves in steps, matching
        // how bracket boundaries actually behave (the drop happens between 12 and 13, not across 12).
        Polygon area = new Polygon();
        int baselineY = marginTop + plotHeight;
        area.addPoint(xForRange(1, maxRange, marginLeft, plotWidth), baselineY);
        int previousY = baselineY;
        for (int range = 1; range <= maxRange; range++) {
            int x = xForRange(range, maxRange, marginLeft, plotWidth);
            int y = yForDamage(curve.valueAt(range), damageCeiling, marginTop, plotHeight);
            if (range > 1) {
                area.addPoint(x, previousY);
            }
            area.addPoint(x, y);
            previousY = y;
        }
        area.addPoint(xForRange(maxRange, maxRange, marginLeft, plotWidth), baselineY);

        graphics2D.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA));
        graphics2D.fillPolygon(area);
        graphics2D.setColor(color);
        Stroke savedStroke = graphics2D.getStroke();
        graphics2D.setStroke(new BasicStroke(UIUtil.scaleForGUI(2)));
        graphics2D.drawPolyline(area.xpoints, area.ypoints, area.npoints);
        graphics2D.setStroke(savedStroke);
    }

    private void paintLegend(Graphics2D graphics2D, int x, int y, int plotWidth) {
        FontMetrics metrics = graphics2D.getFontMetrics();
        String[] labels = {
              Messages.getString("DamageAnalysisPanel.maximum"),
              Messages.getString("DamageAnalysisPanel.expected"),
              Messages.getString("DamageAnalysisPanel.sustained") };
        Color[] colors = { MAX_DAMAGE_COLOR, EXPECTED_DAMAGE_COLOR, SUSTAINED_DAMAGE_COLOR };

        int dotSize = UIUtil.scaleForGUI(10);
        int spacing = UIUtil.scaleForGUI(18);
        int legendWidth = 0;
        for (String label : labels) {
            legendWidth += dotSize + UIUtil.scaleForGUI(6) + metrics.stringWidth(label) + spacing;
        }
        int currentX = x + Math.max(0, (plotWidth - legendWidth) / 2);
        for (int index = 0; index < labels.length; index++) {
            graphics2D.setColor(colors[index]);
            graphics2D.fillOval(currentX, y - dotSize + UIUtil.scaleForGUI(2), dotSize, dotSize);
            graphics2D.setColor(UIManager.getColor("Label.foreground"));
            int textX = currentX + dotSize + UIUtil.scaleForGUI(6);
            graphics2D.drawString(labels[index], textX, y);
            currentX = textX + metrics.stringWidth(labels[index]) + spacing;
        }
    }

    private static int xForRange(int range, int maxRange, int marginLeft, int plotWidth) {
        if (maxRange <= 1) {
            return marginLeft;
        }
        return marginLeft + (int) Math.round(plotWidth * (range - 1) / (double) (maxRange - 1));
    }

    private static int yForDamage(double damage, double damageCeiling, int marginTop, int plotHeight) {
        double clamped = Math.min(damage, damageCeiling);
        return marginTop + plotHeight - (int) Math.round(plotHeight * clamped / damageCeiling);
    }

    /** Rounds a damage value up to a pleasant axis ceiling (multiples of 5, minimum 5). */
    private static double niceCeiling(double value) {
        return Math.max(5, Math.ceil(value / 5.0) * 5);
    }
}
