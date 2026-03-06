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
package megamek.client.ui.panels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.Serial;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.losDiagram.LOSDiagramData;
import megamek.common.losDiagram.LOSDiagramData.HexRow;

/**
 * A panel that renders a 2D elevation cross-section diagram for a LOS path. Shows ground elevation, terrain features,
 * unit positions, and the LOS line between attacker and target.
 *
 * <p>This panel is designed to be embedded in the {@code RulerDialog} as a
 * collapsible section. It takes a {@link LOSDiagramData} record as input and renders the diagram independently of game
 * state.</p>
 */
public class LOSElevationDiagramPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MIN_HEX_WIDTH = 30;
    private static final int LEFT_MARGIN = 40;
    private static final int RIGHT_MARGIN = 15;
    private static final int TOP_MARGIN = 20;
    private static final int BOTTOM_MARGIN = 30;
    private static final int LEVEL_PADDING = 2;
    private static final int UNIT_BAR_WIDTH = 8;

    private static final Color COLOR_GROUND = new Color(139, 119, 101);
    private static final Color COLOR_GROUND_OUTLINE = new Color(100, 80, 60);
    private static final Color COLOR_WATER = new Color(64, 128, 200, 160);
    private static final Color COLOR_WOODS_LIGHT = new Color(34, 139, 34, 160);
    private static final Color COLOR_WOODS_HEAVY = new Color(0, 100, 0, 180);
    private static final Color COLOR_WOODS_ULTRA = new Color(0, 70, 0, 200);
    private static final Color COLOR_JUNGLE_LIGHT = new Color(50, 160, 50, 160);
    private static final Color COLOR_JUNGLE_HEAVY = new Color(20, 120, 20, 180);
    private static final Color COLOR_JUNGLE_ULTRA = new Color(10, 80, 10, 200);
    private static final Color COLOR_BUILDING = new Color(160, 160, 160);
    private static final Color COLOR_BUILDING_OUTLINE = new Color(120, 120, 120);
    private static final Color COLOR_INDUSTRIAL = new Color(180, 160, 120);
    private static final Color COLOR_INDUSTRIAL_OUTLINE = new Color(140, 120, 80);
    private static final Color COLOR_SMOKE_LIGHT = new Color(160, 160, 160, 140);
    private static final Color COLOR_SMOKE_HEAVY = new Color(100, 100, 100, 180);
    private static final Color COLOR_SMOKE_HATCH = new Color(120, 120, 120, 100);
    private static final Color COLOR_FIRE = new Color(255, 80, 0, 150);
    private static final Color COLOR_SCREEN = new Color(180, 180, 255, 120);
    private static final Color COLOR_FIELDS = new Color(200, 180, 50, 100);
    private static final Color COLOR_GRID = new Color(210, 210, 210);
    private static final Color COLOR_LOS_CLEAR = new Color(0, 180, 0);
    private static final Color COLOR_LOS_BLOCKED = new Color(220, 0, 0);
    private static final Color COLOR_BACKGROUND = new Color(245, 245, 240);
    private static final Color COLOR_SPLIT_MARKER = new Color(255, 165, 0, 120);
    private static final Color COLOR_LABEL = new Color(60, 60, 60);

    private static final Stroke STROKE_LOS = new BasicStroke(2.0f);
    private static final Stroke STROKE_GRID = new BasicStroke(0.5f);
    private static final Stroke STROKE_DEFAULT = new BasicStroke(1.0f);
    private static final float[] DASH_PATTERN = { 6.0f, 4.0f };
    private static final Stroke STROKE_SPLIT = new BasicStroke(
          1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0.0f);

    private LOSDiagramData diagramData;
    private boolean showAllTerrain;

    public LOSElevationDiagramPanel() {
        setBackground(COLOR_BACKGROUND);
        int preferredHeight = UIUtil.scaleForGUI(200);
        setPreferredSize(new Dimension(0, preferredHeight));
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setToolTipText(getHexTooltip(e.getX(), e.getY()));
            }
        });
    }

    /**
     * Sets the diagram data and triggers a repaint.
     *
     * @param data the LOS diagram data, or null to clear the diagram
     */
    public void setData(LOSDiagramData data) {
        this.diagramData = data;
        updatePreferredWidth();
        revalidate();
        repaint();
    }

    /**
     * Sets whether all terrain types should be rendered, or only LOS-affecting terrain.
     *
     * @param showAll true to show all terrain, false to show only LOS-affecting terrain
     */
    public void setShowAllTerrain(boolean showAll) {
        this.showAllTerrain = showAll;
        repaint();
    }

    private void updatePreferredWidth() {
        if (diagramData == null || diagramData.hexPath().isEmpty()) {
            return;
        }

        int scaledMinHexWidth = UIUtil.scaleForGUI(MIN_HEX_WIDTH);
        int scaledLeftMargin = UIUtil.scaleForGUI(LEFT_MARGIN);
        int scaledRightMargin = UIUtil.scaleForGUI(RIGHT_MARGIN);
        int hexCount = diagramData.hexPath().size();
        int neededWidth = scaledLeftMargin + (hexCount * scaledMinHexWidth) + scaledRightMargin;

        // Only set preferred width if content is wider than the parent would give us
        Dimension current = getPreferredSize();
        setPreferredSize(new Dimension(Math.max(neededWidth, 0), current.height));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (diagramData == null || diagramData.hexPath().isEmpty()) {
            drawEmptyMessage(graphics);
            return;
        }

        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            List<HexRow> hexPath = diagramData.hexPath();
            DiagramMetrics metrics = calculateMetrics(hexPath);

            drawGrid(g2d, metrics);
            drawTerrain(g2d, metrics, hexPath);
            drawTerrainLabels(g2d, metrics, hexPath);
            drawUnitBars(g2d, metrics, hexPath);
            drawLosLine(g2d, metrics, hexPath);
            drawLabels(g2d, metrics, hexPath);
        } finally {
            g2d.dispose();
        }
    }

    private void drawEmptyMessage(Graphics graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(COLOR_LABEL);
            g2d.setFont(g2d.getFont().deriveFont(Font.ITALIC));
            String message = Messages.getString("Ruler.diagramEmpty");
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int textX = (getWidth() - fontMetrics.stringWidth(message)) / 2;
            int textY = getHeight() / 2;
            g2d.drawString(message, textX, textY);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Calculates rendering metrics (scale, offsets) for the current panel size and data.
     */
    private DiagramMetrics calculateMetrics(List<HexRow> hexPath) {
        int scaledLeftMargin = UIUtil.scaleForGUI(LEFT_MARGIN);
        int scaledRightMargin = UIUtil.scaleForGUI(RIGHT_MARGIN);
        int scaledTopMargin = UIUtil.scaleForGUI(TOP_MARGIN);
        int scaledBottomMargin = UIUtil.scaleForGUI(BOTTOM_MARGIN);

        int drawAreaWidth = getWidth() - scaledLeftMargin - scaledRightMargin;
        int drawAreaHeight = getHeight() - scaledTopMargin - scaledBottomMargin;

        int hexCount = hexPath.size();
        int scaledMinHexWidth = UIUtil.scaleForGUI(MIN_HEX_WIDTH);
        int hexColumnWidth = Math.max(scaledMinHexWidth, drawAreaWidth / Math.max(hexCount, 1));

        // Find elevation range across all hexes, units, and LOS line
        int minLevel = Integer.MAX_VALUE;
        int maxLevel = Integer.MIN_VALUE;

        for (HexRow hex : hexPath) {
            int waterFloor = hex.groundElevation() - hex.waterDepth();
            minLevel = Math.min(minLevel, waterFloor);
            maxLevel = Math.max(maxLevel, hex.topElevation());
        }

        // Include unit heights in the range (absHeight already includes +1 TW correction)
        minLevel = Math.min(minLevel, Math.min(diagramData.attackerAbsHeight(),
              diagramData.targetAbsHeight()));
        maxLevel = Math.max(maxLevel, Math.max(diagramData.attackerAbsHeight(),
              diagramData.targetAbsHeight()));

        // Add padding so units and terrain don't touch the edges
        minLevel -= LEVEL_PADDING;
        maxLevel += LEVEL_PADDING;

        int levelRange = Math.max(maxLevel - minLevel, 1);
        double levelHeight = (double) drawAreaHeight / levelRange;

        return new DiagramMetrics(
              scaledLeftMargin, scaledTopMargin,
              drawAreaWidth, drawAreaHeight,
              hexColumnWidth, levelHeight,
              minLevel, maxLevel, hexCount
        );
    }

    /**
     * Draws the background grid lines and axis labels.
     */
    private void drawGrid(Graphics2D g2d, DiagramMetrics metrics) {
        g2d.setStroke(STROKE_GRID);
        g2d.setColor(COLOR_GRID);
        Font labelFont = g2d.getFont().deriveFont(UIUtil.scaleForGUI(10.0f));
        g2d.setFont(labelFont);
        FontMetrics fontMetrics = g2d.getFontMetrics();

        // Horizontal grid lines (elevation levels)
        for (int level = metrics.minLevel + LEVEL_PADDING;
              level <= metrics.maxLevel - LEVEL_PADDING; level++) {
            int yPos = metrics.levelToY(level);
            g2d.setColor(COLOR_GRID);
            g2d.drawLine(metrics.leftMargin, yPos,
                  metrics.leftMargin + metrics.hexCount * metrics.hexColumnWidth, yPos);

            // Level labels on left axis
            g2d.setColor(COLOR_LABEL);
            String levelStr = String.valueOf(level);
            int labelWidth = fontMetrics.stringWidth(levelStr);
            g2d.drawString(levelStr, metrics.leftMargin - labelWidth - 4, yPos + 4);
        }

        // Vertical grid lines (hex boundaries)
        g2d.setColor(COLOR_GRID);
        for (int i = 0; i <= metrics.hexCount; i++) {
            int xPos = metrics.leftMargin + (i * metrics.hexColumnWidth);
            g2d.drawLine(xPos, metrics.topMargin, xPos,
                  metrics.topMargin + metrics.drawAreaHeight);
        }
    }

    /**
     * Draws the terrain profile (ground elevation, water, buildings, woods, and other features).
     */
    private void drawTerrain(Graphics2D g2d, DiagramMetrics metrics, List<HexRow> hexPath) {
        for (int i = 0; i < hexPath.size(); i++) {
            HexRow hex = hexPath.get(i);
            int xLeft = metrics.leftMargin + (i * metrics.hexColumnWidth);
            int columnWidth = metrics.hexColumnWidth;
            int yGround = metrics.levelToY(hex.groundElevation());
            int yBottom = metrics.levelToY(metrics.minLevel);

            // Draw water depth below ground level
            if (hex.waterDepth() > 0) {
                int waterFloor = hex.groundElevation() - hex.waterDepth();
                int yWaterFloor = metrics.levelToY(waterFloor);
                g2d.setColor(COLOR_WATER);
                g2d.fillRect(xLeft, yGround, columnWidth, yWaterFloor - yGround);
            }

            // Draw ground elevation
            g2d.setColor(COLOR_GROUND);
            g2d.fillRect(xLeft, yGround, columnWidth, yBottom - yGround);
            g2d.setColor(COLOR_GROUND_OUTLINE);
            g2d.setStroke(STROKE_DEFAULT);
            g2d.drawRect(xLeft, yGround, columnWidth, yBottom - yGround);

            // Draw building height
            if (hex.buildingHeight() > 0) {
                int yBuildingTop = metrics.levelToY(hex.groundElevation() + hex.buildingHeight());
                g2d.setColor(COLOR_BUILDING);
                g2d.fillRect(xLeft + 2, yBuildingTop, columnWidth - 4, yGround - yBuildingTop);
                g2d.setColor(COLOR_BUILDING_OUTLINE);
                g2d.drawRect(xLeft + 2, yBuildingTop, columnWidth - 4, yGround - yBuildingTop);
            }

            // Draw industrial zone height
            if (hex.industrialHeight() > 0) {
                int yIndustrialTop = metrics.levelToY(
                      hex.groundElevation() + hex.industrialHeight());
                g2d.setColor(COLOR_INDUSTRIAL);
                g2d.fillRect(xLeft + 2, yIndustrialTop, columnWidth - 4,
                      yGround - yIndustrialTop);
                g2d.setColor(COLOR_INDUSTRIAL_OUTLINE);
                g2d.drawRect(xLeft + 2, yIndustrialTop, columnWidth - 4,
                      yGround - yIndustrialTop);
            }

            // Draw woods/jungle canopy with density-based coloring
            if (hex.woodsHeight() > 0) {
                int yCanopyTop = metrics.levelToY(
                      hex.groundElevation() + hex.woodsHeight());
                g2d.setColor(getFoliageColor(hex));
                g2d.fillRect(xLeft + 2, yCanopyTop, columnWidth - 4, yGround - yCanopyTop);
            }

            // Draw LOS-modifier overlays (only when showAllTerrain is true, or always for smoke)
            drawTerrainOverlays(g2d, metrics, hex, xLeft, columnWidth, yGround);

            // Draw split hex marker
            if (hex.splitHex()) {
                g2d.setColor(COLOR_SPLIT_MARKER);
                g2d.setStroke(STROKE_SPLIT);
                g2d.drawRect(xLeft + 1, metrics.topMargin + 1,
                      columnWidth - 2, metrics.drawAreaHeight - 2);
                g2d.setStroke(STROKE_DEFAULT);
            }
        }
    }

    /**
     * Returns the appropriate foliage color based on woods/jungle type and density.
     */
    private Color getFoliageColor(HexRow hex) {
        if (hex.jungleLevel() > 0) {
            return switch (hex.jungleLevel()) {
                case 2 -> COLOR_JUNGLE_HEAVY;
                case 3 -> COLOR_JUNGLE_ULTRA;
                default -> COLOR_JUNGLE_LIGHT;
            };
        }
        return switch (hex.woodsLevel()) {
            case 2 -> COLOR_WOODS_HEAVY;
            case 3 -> COLOR_WOODS_ULTRA;
            default -> COLOR_WOODS_LIGHT;
        };
    }

    /**
     * Draws overlay indicators for terrain that modifies LOS (smoke, fire, screen, fields). Smoke is always shown since
     * it directly affects LOS. Other overlays are shown when showAllTerrain is enabled.
     */
    private void drawTerrainOverlays(Graphics2D g2d, DiagramMetrics metrics, HexRow hex,
          int xLeft, int columnWidth, int yGround) {
        // Smoke is always shown (directly affects LOS)
        if (hex.smokeLevel() > 0) {
            int smokeTop = hex.groundElevation() + 2;
            int ySmokeTop = metrics.levelToY(smokeTop);
            int smokeHeight = yGround - ySmokeTop;

            // Fill with smoke color
            g2d.setColor(hex.smokeLevel() >= 2 ? COLOR_SMOKE_HEAVY : COLOR_SMOKE_LIGHT);
            g2d.fillRect(xLeft, ySmokeTop, columnWidth, smokeHeight);

            // Add diagonal hatching for visual distinction
            g2d.setColor(COLOR_SMOKE_HATCH);
            g2d.setStroke(STROKE_DEFAULT);
            java.awt.Shape oldClip = g2d.getClip();
            g2d.clipRect(xLeft, ySmokeTop, columnWidth, smokeHeight);
            int hatchSpacing = UIUtil.scaleForGUI(6);
            for (int offset = -smokeHeight; offset < columnWidth + smokeHeight; offset += hatchSpacing) {
                g2d.drawLine(xLeft + offset, ySmokeTop, xLeft + offset + smokeHeight,
                      ySmokeTop + smokeHeight);
            }
            g2d.setClip(oldClip);

            // Draw border
            g2d.setColor(hex.smokeLevel() >= 2 ? COLOR_SMOKE_HEAVY.darker() : COLOR_SMOKE_LIGHT.darker());
            g2d.drawRect(xLeft, ySmokeTop, columnWidth, smokeHeight);
        }

        if (!showAllTerrain) {
            return;
        }

        // Fire indicator
        if (hex.hasFire()) {
            int yFireTop = metrics.levelToY(hex.groundElevation() + 1);
            g2d.setColor(COLOR_FIRE);
            g2d.fillRect(xLeft + 1, yFireTop, columnWidth - 2, yGround - yFireTop);
            g2d.setColor(COLOR_FIRE.darker());
            g2d.drawRect(xLeft + 1, yFireTop, columnWidth - 2, yGround - yFireTop);
        }

        // Screen indicator
        if (hex.hasScreen()) {
            int yScreenTop = metrics.levelToY(hex.groundElevation() + 2);
            g2d.setColor(COLOR_SCREEN);
            g2d.fillRect(xLeft, yScreenTop, columnWidth, yGround - yScreenTop);
            g2d.setColor(COLOR_SCREEN.darker());
            g2d.drawRect(xLeft, yScreenTop, columnWidth, yGround - yScreenTop);
        }

        // Fields indicator
        if (hex.hasFields()) {
            int yFieldsTop = metrics.levelToY(hex.groundElevation() + 1);
            g2d.setColor(COLOR_FIELDS);
            g2d.fillRect(xLeft, yFieldsTop, columnWidth, yGround - yFieldsTop);
            g2d.setColor(COLOR_FIELDS.darker());
            g2d.drawRect(xLeft, yFieldsTop, columnWidth, yGround - yFieldsTop);
        }
    }

    /**
     * Draws compact text labels inside terrain bars when columns are wide enough.
     */
    private void drawTerrainLabels(Graphics2D g2d, DiagramMetrics metrics, List<HexRow> hexPath) {
        Font labelFont = g2d.getFont().deriveFont(Font.PLAIN, UIUtil.scaleForGUI(8.0f));
        g2d.setFont(labelFont);
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int fontHeight = fontMetrics.getAscent();

        for (int i = 0; i < hexPath.size(); i++) {
            HexRow hex = hexPath.get(i);
            int xLeft = metrics.leftMargin + (i * metrics.hexColumnWidth);
            int columnWidth = metrics.hexColumnWidth;
            int xCenter = xLeft + columnWidth / 2;
            int yGround = metrics.levelToY(hex.groundElevation());

            // Only draw labels if column is wide enough
            if (columnWidth < UIUtil.scaleForGUI(20)) {
                continue;
            }

            // Woods/jungle label
            if (hex.woodsHeight() > 0) {
                int yCanopyTop = metrics.levelToY(hex.groundElevation() + hex.woodsHeight());
                int barMidY = (yCanopyTop + yGround) / 2 + fontHeight / 2;
                String label = hex.jungleLevel() > 0 ? "J" : "W";
                int density = Math.max(hex.woodsLevel(), hex.jungleLevel());
                label += density;
                drawCenteredLabel(g2d, fontMetrics, label, xCenter, barMidY, Color.WHITE);
            }

            // Building label
            if (hex.buildingHeight() > 0) {
                int yTop = metrics.levelToY(hex.groundElevation() + hex.buildingHeight());
                int barMidY = (yTop + yGround) / 2 + fontHeight / 2;
                drawCenteredLabel(g2d, fontMetrics, "B" + hex.buildingHeight(),
                      xCenter, barMidY, Color.WHITE);
            }

            // Smoke label
            if (hex.smokeLevel() > 0) {
                int ySmokeTop = metrics.levelToY(hex.groundElevation() + 2);
                int barMidY = (ySmokeTop + yGround) / 2 + fontHeight / 2;
                String label = hex.smokeLevel() >= 2 ? "S2" : "S1";
                drawCenteredLabel(g2d, fontMetrics, label, xCenter, barMidY, COLOR_LABEL);
            }

            // Fire label (when showing all terrain)
            if (showAllTerrain && hex.hasFire()) {
                int yFireTop = metrics.levelToY(hex.groundElevation() + 1);
                int barMidY = (yFireTop + yGround) / 2 + fontHeight / 2;
                drawCenteredLabel(g2d, fontMetrics, "F", xCenter, barMidY, Color.WHITE);
            }
        }
    }

    private void drawCenteredLabel(Graphics2D g2d, FontMetrics fontMetrics, String label,
          int xCenter, int y, Color color) {
        int labelWidth = fontMetrics.stringWidth(label);
        g2d.setColor(color);
        g2d.drawString(label, xCenter - labelWidth / 2, y);
    }

    /**
     * Draws the unit position bars at attacker and target hexes. The absHeight values from LOSDiagramData already
     * include the +1 TW correction, so a Mek at code height 1 on level 4 has absHeight=6, giving a 2-level bar (4 to
     * 6).
     */
    private void drawUnitBars(Graphics2D g2d, DiagramMetrics metrics, List<HexRow> hexPath) {
        if (hexPath.isEmpty()) {
            return;
        }

        int scaledBarWidth = UIUtil.scaleForGUI(UNIT_BAR_WIDTH);

        // Attacker bar (first hex)
        HexRow attackerHex = hexPath.get(0);
        drawUnitBar(g2d, metrics, 0, attackerHex.groundElevation(),
              diagramData.attackerAbsHeight(), diagramData.attackerAbsHeight(), scaledBarWidth,
              megamek.client.ui.dialogs.RulerDialog.color1);

        // Target bar (last hex)
        HexRow targetHex = hexPath.get(hexPath.size() - 1);
        drawUnitBar(g2d, metrics, hexPath.size() - 1, targetHex.groundElevation(),
              diagramData.targetAbsHeight(), diagramData.targetAbsHeight(), scaledBarWidth,
              megamek.client.ui.dialogs.RulerDialog.color2);
    }

    private void drawUnitBar(Graphics2D g2d, DiagramMetrics metrics,
          int hexIndex, int groundLevel, int visualTop, int losHeight, int barWidth,
          Color barColor) {
        int xCenter = metrics.leftMargin + (hexIndex * metrics.hexColumnWidth)
              + (metrics.hexColumnWidth / 2);
        int yTop = metrics.levelToY(visualTop);
        int yBottom = metrics.levelToY(groundLevel);
        int barHeight = Math.max(yBottom - yTop, 2);

        // Draw the unit bar
        g2d.setColor(barColor);
        g2d.fillRect(xCenter - barWidth / 2, yTop, barWidth, barHeight);
        g2d.setColor(barColor.darker());
        g2d.drawRect(xCenter - barWidth / 2, yTop, barWidth, barHeight);

        // Draw LOS eye-level marker if different from visual top
        if (losHeight != visualTop) {
            int yLos = metrics.levelToY(losHeight);
            g2d.setColor(barColor.brighter());
            g2d.fillRect(xCenter - barWidth, yLos - 1, barWidth * 2, 3);
        }

        // Draw height label above the bar
        Font labelFont = g2d.getFont().deriveFont(UIUtil.scaleForGUI(9.0f));
        g2d.setFont(labelFont);
        g2d.setColor(COLOR_LABEL);
        String heightStr = String.valueOf(losHeight);
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int labelWidth = fontMetrics.stringWidth(heightStr);
        g2d.drawString(heightStr, xCenter - labelWidth / 2, yTop - 3);
    }

    /**
     * Draws the LOS line from attacker to target.
     */
    private void drawLosLine(Graphics2D g2d, DiagramMetrics metrics, List<HexRow> hexPath) {
        if (hexPath.size() < 2) {
            return;
        }

        int xStart = metrics.leftMargin + (metrics.hexColumnWidth / 2);
        int yStart = metrics.levelToY(diagramData.attackerAbsHeight());
        int xEnd = metrics.leftMargin + ((hexPath.size() - 1) * metrics.hexColumnWidth)
              + (metrics.hexColumnWidth / 2);
        int yEnd = metrics.levelToY(diagramData.targetAbsHeight());

        g2d.setStroke(STROKE_LOS);
        g2d.setColor(diagramData.losBlocked() ? COLOR_LOS_BLOCKED : COLOR_LOS_CLEAR);
        g2d.drawLine(xStart, yStart, xEnd, yEnd);
        g2d.setStroke(STROKE_DEFAULT);
    }

    /**
     * Draws hex coordinate labels along the bottom axis.
     */
    private void drawLabels(Graphics2D g2d, DiagramMetrics metrics, List<HexRow> hexPath) {
        Font labelFont = g2d.getFont().deriveFont(UIUtil.scaleForGUI(9.0f));
        g2d.setFont(labelFont);
        g2d.setColor(COLOR_LABEL);
        FontMetrics fontMetrics = g2d.getFontMetrics();

        int yLabel = metrics.topMargin + metrics.drawAreaHeight + fontMetrics.getHeight() + 2;

        for (int i = 0; i < hexPath.size(); i++) {
            HexRow hex = hexPath.get(i);
            String coordStr = hex.coords().toFriendlyString();
            int xCenter = metrics.leftMargin + (i * metrics.hexColumnWidth)
                  + (metrics.hexColumnWidth / 2);
            int labelWidth = fontMetrics.stringWidth(coordStr);

            // Only draw if there's room (skip labels for very narrow columns)
            if (metrics.hexColumnWidth >= labelWidth + 4) {
                g2d.drawString(coordStr, xCenter - labelWidth / 2, yLabel);
            } else if (i == 0 || i == hexPath.size() - 1) {
                // Always draw first and last labels
                g2d.drawString(coordStr, xCenter - labelWidth / 2, yLabel);
            }
        }

        // Draw LOS status label
        String statusLabel = diagramData.losBlocked()
              ? Messages.getString("Ruler.diagramBlocked")
              : Messages.getString("Ruler.diagramClear");
        g2d.setFont(labelFont.deriveFont(Font.BOLD));
        g2d.setColor(diagramData.losBlocked() ? COLOR_LOS_BLOCKED : COLOR_LOS_CLEAR);
        FontMetrics boldMetrics = g2d.getFontMetrics();
        int statusWidth = boldMetrics.stringWidth(statusLabel);
        g2d.drawString(statusLabel,
              metrics.leftMargin + (metrics.hexCount * metrics.hexColumnWidth) / 2
                    - statusWidth / 2,
              metrics.topMargin - 5);
    }

    /**
     * Generates a tooltip string for the hex column at the given pixel coordinates.
     *
     * @param mouseX the mouse X coordinate
     * @param mouseY the mouse Y coordinate
     *
     * @return the tooltip text, or null if not over a hex column
     */
    private String getHexTooltip(int mouseX, int mouseY) {
        if (diagramData == null || diagramData.hexPath().isEmpty()) {
            return null;
        }

        List<HexRow> hexPath = diagramData.hexPath();
        DiagramMetrics metrics = calculateMetrics(hexPath);

        int hexIndex = (mouseX - metrics.leftMargin) / metrics.hexColumnWidth;
        if (hexIndex < 0 || hexIndex >= hexPath.size()) {
            return null;
        }

        HexRow hex = hexPath.get(hexIndex);
        StringBuilder tooltip = new StringBuilder("<html>");
        tooltip.append("<b>").append(hex.coords().toFriendlyString()).append("</b>");
        tooltip.append(" (Level ").append(hex.groundElevation()).append(")");

        if (hex.buildingHeight() > 0) {
            tooltip.append("<br>Building: ").append(hex.buildingHeight()).append(" levels");
        }
        if (hex.industrialHeight() > 0) {
            tooltip.append("<br>Industrial: ").append(hex.industrialHeight()).append(" levels");
        }
        if (hex.hasFoliage()) {
            String foliageType = hex.jungleLevel() > 0 ? "Jungle" : "Woods";
            int density = Math.max(hex.woodsLevel(), hex.jungleLevel());
            String densityStr = switch (density) {
                case 2 -> "Heavy";
                case 3 -> "Ultra";
                default -> "Light";
            };
            tooltip.append("<br>").append(densityStr).append(" ").append(foliageType);
            tooltip.append(" (height ").append(hex.woodsHeight()).append(")");
        }
        if (hex.waterDepth() > 0) {
            tooltip.append("<br>Water: depth ").append(hex.waterDepth());
        }
        if (hex.smokeLevel() > 0) {
            tooltip.append("<br>Smoke: ").append(hex.smokeLevel() >= 2 ? "Heavy" : "Light");
        }
        if (hex.hasFire()) {
            tooltip.append("<br>Fire");
        }
        if (hex.hasScreen()) {
            tooltip.append("<br>Screen");
        }
        if (hex.hasFields()) {
            tooltip.append("<br>Planted Fields");
        }
        if (hex.splitHex() && hex.splitAlternate() != null) {
            tooltip.append("<br><i>Split hex (alternate: ")
                  .append(hex.splitAlternate().toFriendlyString())
                  .append(")</i>");
        }
        if (hex.blocksLOS()) {
            tooltip.append("<br><font color='red'>Blocks LOS</font>");
        }

        tooltip.append("<br>LOS line: ").append(String.format("%.1f", hex.losLineElevation()));
        tooltip.append("</html>");
        return tooltip.toString();
    }

    /**
     * Holds precomputed rendering metrics for the current panel size and data.
     */
    private record DiagramMetrics(
          int leftMargin,
          int topMargin,
          int drawAreaWidth,
          int drawAreaHeight,
          int hexColumnWidth,
          double levelHeight,
          int minLevel,
          int maxLevel,
          int hexCount
    ) {
        /**
         * Converts an elevation level to a Y pixel coordinate. Higher levels have lower Y values (screen coordinates
         * are top-down).
         */
        int levelToY(double level) {
            return topMargin + (int) ((maxLevel - level) * levelHeight);
        }
    }
}
