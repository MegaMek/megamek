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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.UIManager;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.analysis.DamageProfile;
import megamek.common.analysis.DamageProfile.ArcSummary;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * A panel that renders a unit's damage analysis from its {@link DamageProfile} as three charts:
 * a Damage vs Range curve chart (maximum, expected, and heat-sustained damage), a Damage per
 * Direction radar, and a Weapon Reach radar.
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

    private static final Color MAX_DAMAGE_COLOR = new Color(76, 175, 80);        // green
    private static final Color EXPECTED_DAMAGE_COLOR = new Color(211, 84, 66);   // red
    private static final Color SUSTAINED_DAMAGE_COLOR = new Color(66, 106, 211); // blue
    private static final Color MEDIUM_BAND_COLOR = new Color(150, 140, 40);      // olive
    private static final Color REACH_COLOR = new Color(230, 190, 40);            // yellow
    private static final int FILL_ALPHA = 70;

    /** Subtle facing-sector tints for the radars: front, front-right, rear-right, rear, rear-left, front-left. */
    private static final Color[] SECTOR_TINTS = {
          new Color(66, 133, 244, 26),   // front - blue
          new Color(76, 175, 80, 26),    // front-right - green
          new Color(230, 190, 40, 26),   // rear-right - yellow
          new Color(211, 84, 66, 26),    // rear - red
          new Color(230, 190, 40, 26),   // rear-left - yellow
          new Color(76, 175, 80, 26) };  // front-left - green

    private static final String[] DIRECTION_LABEL_KEYS = {
          "DamageAnalysisPanel.front", "DamageAnalysisPanel.frontRight", "DamageAnalysisPanel.rearRight",
          "DamageAnalysisPanel.rear", "DamageAnalysisPanel.rearLeft", "DamageAnalysisPanel.frontLeft" };

    private DamageProfile profile;
    private String unitName = "";

    public DamageAnalysisPanel() {
        setName("damageAnalysisPanel");
        setMinimumSize(UIUtil.scaleForGUI(480, 640));
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

            // Layout: range chart across the top, the two radars side by side below it
            int rangeChartHeight = (int) (getHeight() * 0.52);
            int radarTop = rangeChartHeight;
            int radarHeight = getHeight() - radarTop;
            int radarWidth = getWidth() / 2;

            paintRangeChart(graphics2D, 0, 0, getWidth(), rangeChartHeight);
            paintDirectionRadar(graphics2D, 0, radarTop, radarWidth, radarHeight);
            paintReachRadar(graphics2D, radarWidth, radarTop, radarWidth, radarHeight);
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

    // ========== Damage vs Range chart ==========

    private void paintRangeChart(Graphics2D graphics2D, int regionX, int regionY, int regionWidth,
          int regionHeight) {
        Color foreground = UIManager.getColor("Label.foreground");
        Color gridColor = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 60);

        FontMetrics metrics = graphics2D.getFontMetrics();
        int marginLeft = regionX + UIUtil.scaleForGUI(48);
        int marginTop = regionY + UIUtil.scaleForGUI(28);
        int plotWidth = regionWidth - UIUtil.scaleForGUI(48) - UIUtil.scaleForGUI(16);
        int plotHeight = regionHeight - UIUtil.scaleForGUI(28) - UIUtil.scaleForGUI(56);
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

        paintLegend(graphics2D,
              new String[] { Messages.getString("DamageAnalysisPanel.maximum"),
                             Messages.getString("DamageAnalysisPanel.expected"),
                             Messages.getString("DamageAnalysisPanel.sustained") },
              new Color[] { MAX_DAMAGE_COLOR, EXPECTED_DAMAGE_COLOR, SUSTAINED_DAMAGE_COLOR },
              marginLeft, marginTop + plotHeight + metrics.getHeight() + UIUtil.scaleForGUI(14), plotWidth);
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

    // ========== Radars ==========

    private void paintDirectionRadar(Graphics2D graphics2D, int regionX, int regionY, int regionWidth,
          int regionHeight) {
        double[][] series = new double[4][DamageProfile.DIRECTIONS];
        double ceiling = 0;
        for (int direction = 0; direction < DamageProfile.DIRECTIONS; direction++) {
            ArcSummary arc = profile.arcSummary(direction);
            series[0][direction] = arc.maximumAverage();
            series[1][direction] = arc.shortRangeAverage();
            series[2][direction] = arc.mediumRangeAverage();
            series[3][direction] = arc.longRangeAverage();
            ceiling = Math.max(ceiling, arc.maximumAverage());
        }
        paintRadar(graphics2D, regionX, regionY, regionWidth, regionHeight,
              Messages.getString("DamageAnalysisPanel.directionRadar"),
              series,
              new Color[] { MAX_DAMAGE_COLOR, EXPECTED_DAMAGE_COLOR, MEDIUM_BAND_COLOR,
                            SUSTAINED_DAMAGE_COLOR },
              new String[] { Messages.getString("DamageAnalysisPanel.maxAverage"),
                             Messages.getString("DamageAnalysisPanel.shortAverage"),
                             Messages.getString("DamageAnalysisPanel.mediumAverage"),
                             Messages.getString("DamageAnalysisPanel.longAverage") },
              niceCeiling(ceiling));
    }

    private void paintReachRadar(Graphics2D graphics2D, int regionX, int regionY, int regionWidth,
          int regionHeight) {
        double[][] series = new double[1][DamageProfile.DIRECTIONS];
        double ceiling = 0;
        for (int direction = 0; direction < DamageProfile.DIRECTIONS; direction++) {
            series[0][direction] = profile.arcSummary(direction).reach();
            ceiling = Math.max(ceiling, series[0][direction]);
        }
        paintRadar(graphics2D, regionX, regionY, regionWidth, regionHeight,
              Messages.getString("DamageAnalysisPanel.reachRadar"),
              series,
              new Color[] { REACH_COLOR },
              new String[] { Messages.getString("DamageAnalysisPanel.reach") },
              niceCeiling(ceiling));
    }

    private void paintRadar(Graphics2D graphics2D, int regionX, int regionY, int regionWidth,
          int regionHeight, String title, double[][] series, Color[] colors, String[] labels,
          double ceiling) {
        Color foreground = UIManager.getColor("Label.foreground");
        FontMetrics metrics = graphics2D.getFontMetrics();

        int titleHeight = metrics.getHeight() + UIUtil.scaleForGUI(4);
        int legendHeight = metrics.getHeight() + UIUtil.scaleForGUI(10);
        int labelClearance = metrics.getHeight() + UIUtil.scaleForGUI(8);
        int radius = Math.min((regionWidth / 2) - UIUtil.scaleForGUI(56),
              ((regionHeight - titleHeight - legendHeight) / 2) - labelClearance);
        if (radius < UIUtil.scaleForGUI(40)) {
            return;
        }
        int centerX = regionX + (regionWidth / 2);
        int centerY = regionY + titleHeight + labelClearance + radius;

        graphics2D.setColor(foreground);
        graphics2D.drawString(title, centerX - (metrics.stringWidth(title) / 2),
              regionY + metrics.getAscent() + UIUtil.scaleForGUI(2));

        // Facing-sector tints (60-degree wedges centered on each direction spoke)
        for (int direction = 0; direction < DamageProfile.DIRECTIONS; direction++) {
            graphics2D.setColor(SECTOR_TINTS[direction]);
            graphics2D.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2,
                  60 - (60 * direction), 60);
        }

        // Concentric hexagonal rings. One set of scale numbers on the north-south axis: each grid
        // ring gets its value where it crosses the vertical axis, above and below center, so both
        // the front and the rear halves of a polygon read against the same scale.
        Color gridColor = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 70);
        int rings = 4;
        for (int ring = 1; ring <= rings; ring++) {
            double fraction = ring / (double) rings;
            Polygon ringShape = new Polygon();
            for (int direction = 0; direction < DamageProfile.DIRECTIONS; direction++) {
                ringShape.addPoint(radarX(centerX, radius * fraction, direction),
                      radarY(centerY, radius * fraction, direction));
            }
            graphics2D.setColor(gridColor);
            graphics2D.drawPolygon(ringShape);
            graphics2D.setColor(foreground);
            String ringLabel = String.valueOf((int) Math.round(ceiling * fraction));
            int labelX = centerX + UIUtil.scaleForGUI(3);
            graphics2D.drawString(ringLabel, labelX,
                  (int) (centerY - (radius * fraction)) + metrics.getAscent());
            graphics2D.drawString(ringLabel, labelX,
                  (int) (centerY + (radius * fraction)) - UIUtil.scaleForGUI(2));
        }

        // Spokes and direction labels
        for (int direction = 0; direction < DamageProfile.DIRECTIONS; direction++) {
            int spokeX = radarX(centerX, radius, direction);
            int spokeY = radarY(centerY, radius, direction);
            graphics2D.setColor(gridColor);
            graphics2D.drawLine(centerX, centerY, spokeX, spokeY);
            graphics2D.setColor(foreground);
            String label = Messages.getString(DIRECTION_LABEL_KEYS[direction]);
            int labelX = radarX(centerX, radius + UIUtil.scaleForGUI(10), direction);
            int labelY = radarY(centerY, radius + UIUtil.scaleForGUI(10), direction);
            // Nudge the label so it sits outside the radar instead of overlapping it
            labelX -= (int) (metrics.stringWidth(label) * (1 - Math.cos(radarAngle(direction))) / 2);
            labelY += (int) (metrics.getAscent() * (1 + Math.sin(radarAngle(direction))) / 2);
            graphics2D.drawString(label, labelX, labelY);
        }

        // Series polygons, first series at the back
        for (int index = 0; index < series.length; index++) {
            Polygon shape = new Polygon();
            for (int direction = 0; direction < DamageProfile.DIRECTIONS; direction++) {
                double fraction = Math.min(1.0, series[index][direction] / ceiling);
                shape.addPoint(radarX(centerX, radius * fraction, direction),
                      radarY(centerY, radius * fraction, direction));
            }
            Color color = colors[index];
            graphics2D.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA));
            graphics2D.fillPolygon(shape);
            graphics2D.setColor(color);
            Stroke savedStroke = graphics2D.getStroke();
            graphics2D.setStroke(new BasicStroke(UIUtil.scaleForGUI(2)));
            graphics2D.drawPolygon(shape);
            graphics2D.setStroke(savedStroke);
        }

        paintLegend(graphics2D, labels, colors, regionX + UIUtil.scaleForGUI(8),
              regionY + regionHeight - UIUtil.scaleForGUI(8), regionWidth - UIUtil.scaleForGUI(16));
    }

    /** The screen angle of a direction spoke in radians: front is straight up, clockwise after. */
    private static double radarAngle(int direction) {
        return Math.toRadians(-90 + (60 * direction));
    }

    private static int radarX(int centerX, double distance, int direction) {
        return centerX + (int) Math.round(distance * Math.cos(radarAngle(direction)));
    }

    private static int radarY(int centerY, double distance, int direction) {
        return centerY + (int) Math.round(distance * Math.sin(radarAngle(direction)));
    }

    // ========== Shared helpers ==========

    private void paintLegend(Graphics2D graphics2D, String[] labels, Color[] colors, int x, int y,
          int availableWidth) {
        FontMetrics metrics = graphics2D.getFontMetrics();
        int dotSize = UIUtil.scaleForGUI(10);
        int gap = UIUtil.scaleForGUI(5);
        int spacing = UIUtil.scaleForGUI(14);
        int rowHeight = metrics.getHeight() + UIUtil.scaleForGUI(2);

        // Greedy row wrap so a legend never spills into a neighboring chart's region
        int[] itemWidths = new int[labels.length];
        for (int index = 0; index < labels.length; index++) {
            itemWidths[index] = dotSize + gap + metrics.stringWidth(labels[index]);
        }
        List<int[]> rows = new ArrayList<>(); // {firstIndex, lastIndex, rowWidth}
        int rowStart = 0;
        int rowWidth = 0;
        for (int index = 0; index < labels.length; index++) {
            int addedWidth = itemWidths[index] + ((rowWidth > 0) ? spacing : 0);
            if ((rowWidth > 0) && (rowWidth + addedWidth > availableWidth)) {
                rows.add(new int[] { rowStart, index - 1, rowWidth });
                rowStart = index;
                rowWidth = itemWidths[index];
            } else {
                rowWidth += addedWidth;
            }
        }
        rows.add(new int[] { rowStart, labels.length - 1, rowWidth });

        // y is the baseline of the LAST row; earlier rows stack upward so the legend stays in-region
        int rowY = y - ((rows.size() - 1) * rowHeight);
        for (int[] row : rows) {
            int currentX = x + Math.max(0, (availableWidth - row[2]) / 2);
            for (int index = row[0]; index <= row[1]; index++) {
                graphics2D.setColor(colors[index]);
                graphics2D.fillOval(currentX, rowY - dotSize + UIUtil.scaleForGUI(2), dotSize, dotSize);
                graphics2D.setColor(UIManager.getColor("Label.foreground"));
                int textX = currentX + dotSize + gap;
                graphics2D.drawString(labels[index], textX, rowY);
                currentX = textX + metrics.stringWidth(labels[index]) + spacing;
            }
            rowY += rowHeight;
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

    /** Rounds a value up to a pleasant axis ceiling (multiples of 5, minimum 5). */
    private static double niceCeiling(double value) {
        return Math.max(5, Math.ceil(value / 5.0) * 5);
    }
}
