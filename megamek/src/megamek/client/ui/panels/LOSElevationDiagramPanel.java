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
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.losDiagram.DiagramUnitType;
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
    private static final Color COLOR_FIRE = new Color(255, 50, 0, 210);
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

    private static final String LOS_SILHOUETTE_DIR = "units" + File.separator + "LOS" + File.separator;

    /** Cache of loaded silhouette images keyed by filename. Null values mean load was attempted but failed. */
    private static final Map<String, BufferedImage> silhouetteCache = new HashMap<>();

    private LOSDiagramData diagramData;

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

        // Include unit positions in the range. absHeight is the unit top (TW height).
        // Unit bottom = absHeight - TW height (accounts for elevation, e.g., VTOLs).
        int attackerTwHeight = diagramData.attackerUnitType().twHeight()
              - (diagramData.attackerIsHullDown() ? 1 : 0);
        int targetTwHeight = diagramData.targetUnitType().twHeight()
              - (diagramData.targetIsHullDown() ? 1 : 0);
        int attackerBottom = diagramData.attackerAbsHeight() - attackerTwHeight;
        int targetBottom = diagramData.targetAbsHeight() - targetTwHeight;

        minLevel = Math.min(minLevel, Math.min(attackerBottom, targetBottom));
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

            // For water hexes, ground (seabed) starts at the water floor
            if (hex.waterDepth() > 0) {
                int waterFloor = hex.groundElevation() - hex.waterDepth();
                int yWaterFloor = metrics.levelToY(waterFloor);

                // Draw seabed (ground below water)
                g2d.setColor(COLOR_GROUND);
                g2d.fillRect(xLeft, yWaterFloor, columnWidth, yBottom - yWaterFloor);
                g2d.setColor(COLOR_GROUND_OUTLINE);
                g2d.setStroke(STROKE_DEFAULT);
                g2d.drawRect(xLeft, yWaterFloor, columnWidth, yBottom - yWaterFloor);

                // Draw water from surface down to seabed with wave texture
                g2d.setColor(COLOR_WATER);
                g2d.fillRect(xLeft, yGround, columnWidth, yWaterFloor - yGround);
                drawWaterTexture(g2d, xLeft, yGround, columnWidth, yWaterFloor - yGround);
                g2d.setColor(COLOR_WATER.darker());
                g2d.drawRect(xLeft, yGround, columnWidth, yWaterFloor - yGround);
            } else {
                // Draw ground elevation (no water)
                g2d.setColor(COLOR_GROUND);
                g2d.fillRect(xLeft, yGround, columnWidth, yBottom - yGround);
                g2d.setColor(COLOR_GROUND_OUTLINE);
                g2d.setStroke(STROKE_DEFAULT);
                g2d.drawRect(xLeft, yGround, columnWidth, yBottom - yGround);
            }

            // Draw building height with silhouette
            if (hex.buildingHeight() > 0) {
                int yBuildingTop = metrics.levelToY(hex.groundElevation() + hex.buildingHeight());
                int bldgWidth = columnWidth - 4;
                int bldgHeight = yGround - yBuildingTop;
                drawBuildingSilhouette(g2d, xLeft + 2, yBuildingTop, bldgWidth, bldgHeight,
                      COLOR_BUILDING, COLOR_BUILDING_OUTLINE);
            }

            // Draw industrial zone with smokestack silhouette
            if (hex.industrialHeight() > 0) {
                int yIndustrialTop = metrics.levelToY(
                      hex.groundElevation() + hex.industrialHeight());
                int indWidth = columnWidth - 4;
                int indHeight = yGround - yIndustrialTop;
                drawIndustrialSilhouette(g2d, xLeft + 2, yIndustrialTop, indWidth, indHeight,
                      COLOR_INDUSTRIAL, COLOR_INDUSTRIAL_OUTLINE);
            }

            // Draw woods/jungle as tree silhouettes with density-based coloring
            if (hex.woodsHeight() > 0) {
                int yCanopyTop = metrics.levelToY(
                      hex.groundElevation() + hex.woodsHeight());
                Color foliageColor = getFoliageColor(hex);
                int density = Math.max(hex.woodsLevel(), hex.jungleLevel());
                boolean isJungle = hex.jungleLevel() > 0;
                drawTreeSilhouette(g2d, foliageColor, xLeft + 2, yCanopyTop,
                      columnWidth - 4, yGround - yCanopyTop, density, isJungle);
            }

            // Draw LOS-modifier terrain overlays
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
     * Draws a tree silhouette profile. Woods get conical (evergreen) shapes, jungle gets
     * rounded (broadleaf) shapes. Higher density draws more trees side by side.
     *
     * @param g2d       the graphics context
     * @param color     the foliage color
     * @param x         left edge of the drawing area
     * @param y         top edge of the drawing area
     * @param width     width of the drawing area
     * @param height    height of the drawing area
     * @param density   foliage density (1=light, 2=heavy, 3=ultra)
     * @param isJungle  true for jungle (rounded canopy), false for woods (conical canopy)
     */
    private void drawTreeSilhouette(Graphics2D g2d, Color color, int x, int y,
          int width, int height, int density, boolean isJungle) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int trunkHeight = Math.max(height / 5, 2);
        int canopyHeight = height - trunkHeight;
        int canopyTop = y;
        int canopyBottom = y + canopyHeight;
        int trunkBottom = y + height;

        // Draw more trees for higher density
        int treeCount = Math.min(density, 3);
        int treeWidth = width / treeCount;

        for (int i = 0; i < treeCount; i++) {
            int treeX = x + (i * treeWidth);
            int treeCenterX = treeX + (treeWidth / 2);
            int trunkWidth = Math.max(treeWidth / 6, 2);

            // Draw trunk
            Color trunkColor = new Color(101, 67, 33, color.getAlpha());
            g2d.setColor(trunkColor);
            g2d.fillRect(treeCenterX - trunkWidth / 2, canopyBottom,
                  trunkWidth, trunkHeight);

            // Draw canopy
            g2d.setColor(color);
            if (isJungle) {
                // Rounded canopy for jungle
                g2d.fillOval(treeX + 1, canopyTop, treeWidth - 2, canopyHeight);
            } else {
                // Conical (triangular) canopy for woods/evergreen
                int[] xPoints = { treeCenterX, treeX + 1, treeX + treeWidth - 1 };
                int[] yPoints = { canopyTop, canopyBottom, canopyBottom };
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        }
    }

    /**
     * Draws a building silhouette with windows and a flat roofline.
     */
    private void drawBuildingSilhouette(Graphics2D g2d, int x, int y, int width, int height,
          Color fillColor, Color outlineColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        // Main building body
        g2d.setColor(fillColor);
        g2d.fillRect(x, y, width, height);
        g2d.setColor(outlineColor);
        g2d.drawRect(x, y, width, height);

        // Windows - grid of small darker rectangles
        Color windowColor = new Color(80, 100, 120, 180);
        g2d.setColor(windowColor);
        int windowSize = Math.max(UIUtil.scaleForGUI(3), 2);
        int windowGap = windowSize + Math.max(UIUtil.scaleForGUI(3), 2);
        int margin = Math.max(UIUtil.scaleForGUI(2), 1);

        for (int wy = y + margin + windowSize; wy + windowSize < y + height - margin; wy += windowGap) {
            for (int wx = x + margin; wx + windowSize < x + width - margin; wx += windowGap) {
                g2d.fillRect(wx, wy, windowSize, windowSize);
            }
        }
    }

    /**
     * Draws an industrial silhouette with a sawtooth roof and smokestack.
     */
    private void drawIndustrialSilhouette(Graphics2D g2d, int x, int y, int width, int height,
          Color fillColor, Color outlineColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        // Main body
        g2d.setColor(fillColor);
        g2d.fillRect(x, y, width, height);

        // Sawtooth roof along the top
        g2d.setColor(outlineColor);
        int toothSize = Math.max(UIUtil.scaleForGUI(4), 3);
        int toothHeight = Math.min(toothSize, height / 3);
        for (int tx = x; tx < x + width - toothSize; tx += toothSize) {
            g2d.drawLine(tx, y, tx + toothSize / 2, y - toothHeight);
            g2d.drawLine(tx + toothSize / 2, y - toothHeight, tx + toothSize, y);
        }

        // Smokestack on the right side
        int stackWidth = Math.max(width / 6, 2);
        int stackHeight = Math.max(height / 3, 3);
        g2d.setColor(fillColor.darker());
        g2d.fillRect(x + width - stackWidth - 2, y - stackHeight, stackWidth, stackHeight);
        g2d.setColor(outlineColor);
        g2d.drawRect(x + width - stackWidth - 2, y - stackHeight, stackWidth, stackHeight);
    }

    /**
     * Draws horizontal wavy lines over a water area to add wave texture.
     */
    private void drawWaterTexture(Graphics2D g2d, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Color waveColor = new Color(40, 100, 180, 80);
        g2d.setColor(waveColor);
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(STROKE_DEFAULT);

        java.awt.Shape oldClip = g2d.getClip();
        g2d.clipRect(x, y, width, height);

        int waveSpacing = Math.max(UIUtil.scaleForGUI(6), 4);
        int waveAmplitude = Math.max(UIUtil.scaleForGUI(2), 1);

        for (int wy = y + waveSpacing; wy < y + height; wy += waveSpacing) {
            int prevX = x;
            int prevY = wy;
            int segWidth = Math.max(UIUtil.scaleForGUI(4), 3);
            for (int wx = x + segWidth; wx <= x + width; wx += segWidth) {
                int offset = ((wx / segWidth) % 2 == 0) ? -waveAmplitude : waveAmplitude;
                g2d.drawLine(prevX, prevY, wx, wy + offset);
                prevX = wx;
                prevY = wy + offset;
            }
        }

        g2d.setClip(oldClip);
        g2d.setStroke(oldStroke);
    }

    /**
     * Draws flame shapes rising from the ground.
     */
    private void drawFlames(Graphics2D g2d, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int flameCount = Math.max(width / Math.max(UIUtil.scaleForGUI(6), 4), 2);
        int flameWidth = width / flameCount;
        int flameBottom = y + height;

        for (int i = 0; i < flameCount; i++) {
            int fx = x + (i * flameWidth);
            int fCenterX = fx + flameWidth / 2;

            // Outer flame (orange/red)
            int outerHeight = height - (i % 2 == 0 ? 0 : height / 4);
            int outerTop = flameBottom - outerHeight;
            g2d.setColor(COLOR_FIRE);
            int[] oxPoints = { fCenterX, fx + 1, fx + flameWidth - 1 };
            int[] oyPoints = { outerTop, flameBottom, flameBottom };
            g2d.fillPolygon(oxPoints, oyPoints, 3);

            // Inner flame (yellow core)
            Color innerColor = new Color(255, 220, 0, 220);
            g2d.setColor(innerColor);
            int innerHeight = outerHeight * 2 / 3;
            int innerTop = flameBottom - innerHeight;
            int innerMargin = flameWidth / 4;
            int[] ixPoints = { fCenterX, fx + innerMargin + 1, fx + flameWidth - innerMargin - 1 };
            int[] iyPoints = { innerTop, flameBottom, flameBottom };
            g2d.fillPolygon(ixPoints, iyPoints, 3);
        }
    }

    /**
     * Draws crop/wheat stalks for planted fields.
     */
    private void drawFieldStalks(Graphics2D g2d, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        // Background fill
        g2d.setColor(COLOR_FIELDS);
        g2d.fillRect(x, y, width, height);

        // Draw individual stalks
        Color stalkColor = new Color(160, 140, 30, 180);
        Color headColor = new Color(180, 160, 40, 200);
        g2d.setColor(stalkColor);
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(STROKE_DEFAULT);

        int stalkSpacing = Math.max(UIUtil.scaleForGUI(4), 3);
        int stalkBottom = y + height;

        for (int sx = x + stalkSpacing / 2; sx < x + width; sx += stalkSpacing) {
            // Stalk leans slightly alternating left/right
            int lean = (sx / stalkSpacing % 2 == 0) ? -1 : 1;
            g2d.setColor(stalkColor);
            g2d.drawLine(sx, stalkBottom, sx + lean, y + height / 4);

            // Wheat head at top
            g2d.setColor(headColor);
            int headHeight = Math.max(height / 4, 2);
            g2d.fillOval(sx + lean - 1, y, 3, headHeight);
        }

        // Border
        g2d.setColor(COLOR_FIELDS.darker());
        g2d.drawRect(x, y, width, height);
        g2d.setStroke(oldStroke);
    }

    /**
     * Draws a rising smoke plume - narrow at the base, billowing wide at the top with overlapping cloud puffs. Inspired
     * by classic smoke column silhouettes. Heavy smoke is darker and more opaque.
     */
    private void drawSmokeBillows(Graphics2D g2d, int x, int y, int width, int height,
          boolean isHeavy) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int baseAlpha = isHeavy ? 150 : 90;
        int baseGray = isHeavy ? 80 : 165;
        int puffSize = Math.max(UIUtil.scaleForGUI(12), 7);
        int centerX = x + width / 2;

        // Draw from bottom (row=0) to top (row=layers-1)
        // Each layer gets wider as we go up, creating the plume shape
        int layerStep = Math.max(puffSize / 3, 3);
        int layers = Math.max(height / layerStep, 3);

        for (int layer = 0; layer < layers; layer++) {
            // 0.0 at bottom, 1.0 at top
            double progress = (double) layer / (layers - 1);

            // Plume width: narrow at base (~30%), expands to full + overflow at top
            // Use a curve so it expands gradually then billows out
            double widthFactor = 0.3 + 0.7 * Math.pow(progress, 0.6);
            int layerWidth = (int) (width * widthFactor);

            // Allow overflow beyond column at top
            int overflow = (int) (width * 0.3 * progress);
            layerWidth += overflow;

            int layerX = centerX - layerWidth / 2;
            int layerY = y + height - (layer * layerStep);

            // 2-3 puffs per layer, staggered
            int puffsInLayer = Math.max(2, layerWidth / (puffSize * 2 / 3));
            for (int puff = 0; puff < puffsInLayer; puff++) {
                // Spread puffs across the layer width
                double puffProgress = (puffsInLayer == 1) ? 0.5 : (double) puff / (puffsInLayer - 1);
                int px = layerX + (int) (puffProgress * (layerWidth - puffSize));

                // Pseudo-random variation using deterministic hash
                int hash = (layer * 7 + puff * 13) % 11;
                int jitterX = (hash - 5) * puffSize / 8;
                int jitterY = ((hash * 3) % 7 - 3) * puffSize / 6;
                px += jitterX;
                int py = layerY + jitterY;

                // Puff size grows toward the top
                int sizeBoost = (int) (puffSize * 0.6 * progress);
                int puffW = puffSize + sizeBoost + (hash % 3) * puffSize / 5;
                int puffH = (int) (puffW * 0.7);

                // Opacity: dense in the middle column, fading at edges and very top
                double edgeFade = 1.0 - 0.4 * Math.abs(puffProgress - 0.5) * 2;
                double topFade = progress > 0.85 ? 1.0 - (progress - 0.85) / 0.15 : 1.0;
                int alpha = (int) (baseAlpha * edgeFade * topFade);
                alpha = Math.max(15, Math.min(200, alpha));

                // Color variation: lighter at top (rising smoke), darker at base
                int grayShift = (int) (40 * progress);
                int grayVar = ((hash + puff) % 3 - 1) * 12;
                int gray = Math.max(0, Math.min(235, baseGray + grayShift + grayVar));

                g2d.setColor(new Color(gray, gray, gray, alpha));
                g2d.fillOval(px, py, puffW, puffH);
            }
        }

        // Crown puffs at the very top - larger, lighter billowing clouds
        int crownAlpha = Math.max(baseAlpha / 3, 20);
        int crownGray = Math.min(baseGray + 55, 230);
        int crownPuffSize = puffSize * 3 / 2;
        int crownWidth = width + width / 2;
        int crownX = centerX - crownWidth / 2;
        for (int puff = 0; puff < 4; puff++) {
            int px = crownX + (puff * crownWidth / 4);
            int hash = (puff * 11 + 5) % 7;
            int jitterX = (hash - 3) * crownPuffSize / 4;
            int jitterY = (hash % 3 - 1) * crownPuffSize / 3;
            int puffW = crownPuffSize + (hash % 3) * crownPuffSize / 3;
            int puffH = (int) (puffW * 0.65);
            g2d.setColor(new Color(crownGray, crownGray, crownGray, crownAlpha));
            g2d.fillOval(px + jitterX, y - crownPuffSize / 3 + jitterY, puffW, puffH);
        }
    }

    /**
     * Draws overlay indicators for terrain that modifies LOS (smoke, fire, screen, fields).
     */
    private void drawTerrainOverlays(Graphics2D g2d, DiagramMetrics metrics, HexRow hex,
          int xLeft, int columnWidth, int yGround) {
        // Smoke
        if (hex.smokeLevel() > 0) {
            int smokeTop = hex.groundElevation() + 2;
            int ySmokeTop = metrics.levelToY(smokeTop);
            int smokeHeight = yGround - ySmokeTop;
            boolean isHeavy = hex.smokeLevel() >= 2;
            drawSmokeBillows(g2d, xLeft, ySmokeTop, columnWidth, smokeHeight, isHeavy);
        }

        // Fire indicator with flame shapes
        if (hex.hasFire()) {
            int yFireTop = metrics.levelToY(hex.groundElevation() + 1);
            drawFlames(g2d, xLeft + 1, yFireTop, columnWidth - 2, yGround - yFireTop);
        }

        // Screen indicator
        if (hex.hasScreen()) {
            int yScreenTop = metrics.levelToY(hex.groundElevation() + 2);
            g2d.setColor(COLOR_SCREEN);
            g2d.fillRect(xLeft, yScreenTop, columnWidth, yGround - yScreenTop);
            g2d.setColor(COLOR_SCREEN.darker());
            g2d.drawRect(xLeft, yScreenTop, columnWidth, yGround - yScreenTop);
        }

        // Fields indicator with crop stalks
        if (hex.hasFields()) {
            int yFieldsTop = metrics.levelToY(hex.groundElevation() + 1);
            drawFieldStalks(g2d, xLeft, yFieldsTop, columnWidth, yGround - yFieldsTop);
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

            // Water depth label
            if (hex.waterDepth() > 0) {
                int yWaterFloor = metrics.levelToY(
                      hex.groundElevation() - hex.waterDepth());
                int barMidY = (yGround + yWaterFloor) / 2 + fontHeight / 2;
                drawCenteredLabel(g2d, fontMetrics, "D" + hex.waterDepth(),
                      xCenter, barMidY, Color.WHITE);
            }

            // Fire label
            if (hex.hasFire()) {
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
     * Draws unit silhouettes at attacker and target hexes. The absHeight values from LOSDiagramData already include the
     * +1 TW correction, so a Mek at code height 1 on level 4 has absHeight=6, giving a 2-level silhouette (4 to 6).
     */
    private void drawUnitBars(Graphics2D g2d, DiagramMetrics metrics, List<HexRow> hexPath) {
        if (hexPath.isEmpty()) {
            return;
        }

        // Attacker silhouette
        int attackerTwHeight = diagramData.attackerUnitType().twHeight();
        if (diagramData.attackerIsHullDown()) {
            attackerTwHeight = 1;
        }
        int attackerTop = diagramData.attackerAbsHeight();
        int attackerBottom = attackerTop - attackerTwHeight;
        drawUnitSilhouette(g2d, metrics, 0, attackerBottom, attackerTop,
              megamek.client.ui.dialogs.RulerDialog.color1,
              diagramData.attackerUnitType(), true);

        // Target silhouette
        int targetTwHeight = diagramData.targetUnitType().twHeight();
        if (diagramData.targetIsHullDown()) {
            targetTwHeight = 1;
        }
        int targetTop = diagramData.targetAbsHeight();
        int targetBottom = targetTop - targetTwHeight;
        drawUnitSilhouette(g2d, metrics, hexPath.size() - 1, targetBottom, targetTop,
              megamek.client.ui.dialogs.RulerDialog.color2,
              diagramData.targetUnitType(), false);
    }

    private void drawUnitSilhouette(Graphics2D g2d, DiagramMetrics metrics,
          int hexIndex, int bottomLevel, int topLevel,
          Color barColor, DiagramUnitType unitType, boolean facingRight) {
        int xCenter = metrics.leftMargin + (hexIndex * metrics.hexColumnWidth)
              + (metrics.hexColumnWidth / 2);
        int yBottom = metrics.levelToY(bottomLevel);

        // Use column width as the silhouette width basis instead of the tiny barWidth
        int silhouetteWidth = (int) (metrics.hexColumnWidth * 0.7f);

        // The silhouette must fit within its TW level height (e.g. 2 levels for Meks, 1 for vehicles).
        // It anchors at the bottom (feet on ground) and extends upward.
        int levelBasedHeight = Math.max(yBottom - metrics.levelToY(topLevel), 2);
        int silhouetteHeight = levelBasedHeight;
        int yTop = yBottom - silhouetteHeight;

        switch (unitType) {
            case BATTLE_MEK ->
                  drawMekSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor, facingRight);
            case QUAD_MEK ->
                  drawQuadMekSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor, facingRight);
            case SUPERHEAVY_MEK -> drawSuperHeavyMekSilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case INDUSTRIAL_MEK -> drawIndustrialMekSilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case TRACKED_VEHICLE -> drawTrackedVehicleSilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case HOVER_VEHICLE -> drawHoverVehicleSilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case WHEELED_VEHICLE -> drawWheeledVehicleSilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case WIGE_VEHICLE ->
                  drawWigeSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor, facingRight);
            case SUPPORT_VEHICLE ->
                  drawSupportVehicleSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            case VTOL_TYPE ->
                  drawVtolSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor, facingRight);
            case NAVAL -> drawNavalSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            case SUBMARINE ->
                  drawSubmarineSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor, facingRight);
            case INFANTRY ->
                  drawInfantrySilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor, facingRight);
            case JUMP_INFANTRY -> drawJumpInfantrySilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case MOTORIZED_INFANTRY -> drawMotorizedInfantrySilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case MECHANIZED_INFANTRY -> drawMechanizedInfantrySilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case BATTLE_ARMOR -> drawBattleArmorSilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case PROTO_MEK ->
                  drawProtoMekSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor, facingRight);
            case AEROSPACE_FIGHTER -> drawAeroFighterSilhouette(g2d,
                  xCenter,
                  yTop,
                  silhouetteWidth,
                  silhouetteHeight,
                  barColor,
                  facingRight);
            case CONVENTIONAL_FIGHTER ->
                  drawConvFighterSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            case DROPSHIP -> drawDropShipSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            case SMALL_CRAFT ->
                  drawSmallCraftSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            case JUMPSHIP -> drawJumpShipSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            case WARSHIP -> drawWarShipSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            case SPACE_STATION ->
                  drawSpaceStationSilhouette(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
            default -> drawDefaultBar(g2d, xCenter, yTop, silhouetteWidth, silhouetteHeight, barColor);
        }

        // Draw height label above the silhouette
        Font labelFont = g2d.getFont().deriveFont(UIUtil.scaleForGUI(9.0f));
        g2d.setFont(labelFont);
        g2d.setColor(COLOR_LABEL);
        String heightStr = String.valueOf(topLevel);
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int labelWidth = fontMetrics.stringWidth(heightStr);
        g2d.drawString(heightStr, xCenter - labelWidth / 2, yTop - 3);
    }

    /**
     * Draws a BattleMek silhouette using a PNG image, with GeneralPath fallback.
     */
    private void drawMekSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("BattleMech_Silhouette.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: simple humanoid shape
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillRect((int) (left + w * 0.3f), (int) yTop, (int) (w * 0.4f), (int) (h * 0.5f));
        g2d.fillRect((int) left, (int) (yTop + h * 0.5f), (int) (w * 0.35f), (int) (h * 0.5f));
        g2d.fillRect((int) (left + w * 0.65f), (int) (yTop + h * 0.5f), (int) (w * 0.35f), (int) (h * 0.5f));
    }

    /**
     * Draws a QuadMek silhouette: four-legged mek shape.
     */
    private void drawQuadMekSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("Quad_Mek.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: low wide shape
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillRect((int) (left + w * 0.1f), (int) (yTop + h * 0.2f), (int) (w * 0.8f), (int) (h * 0.4f));
        g2d.fillRect((int) left, (int) (yTop + h * 0.6f), (int) (w * 0.25f), (int) (h * 0.4f));
        g2d.fillRect((int) (left + w * 0.75f), (int) (yTop + h * 0.6f), (int) (w * 0.25f), (int) (h * 0.4f));
    }

    /**
     * Draws a SuperHeavy Mek silhouette.
     */
    private void drawSuperHeavyMekSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("SuperHeavy_Mek.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: use regular mek
        drawMekSilhouette(g2d, xCenter, yTop, width, height, color, facingRight);
    }

    /**
     * Draws a tracked vehicle silhouette: low hull with a turret bump on top.
     */
    private void drawTrackedVehicleSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("Tracked.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: simple box with turret bump
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillRect((int) left, (int) (yTop + h * 0.4f), (int) w, (int) (h * 0.6f));
        g2d.fillRect((int) (left + w * 0.3f), (int) (yTop + h * 0.1f), (int) (w * 0.4f), (int) (h * 0.3f));
    }

    /**
     * Draws a hover vehicle silhouette.
     */
    private void drawHoverVehicleSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("Hovercraft.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: use tracked vehicle fallback
        drawTrackedVehicleSilhouette(g2d, xCenter, yTop, width, height, color, facingRight);
    }

    /**
     * Draws a wheeled vehicle silhouette.
     */
    private void drawWheeledVehicleSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("Wheeled.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: use tracked vehicle fallback
        drawTrackedVehicleSilhouette(g2d, xCenter, yTop, width, height, color, facingRight);
    }

    /**
     * Draws a WiGE (Wing-in-Ground-Effect) vehicle silhouette.
     */
    private void drawWigeSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("WIGE.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: use tracked vehicle fallback
        drawTrackedVehicleSilhouette(g2d, xCenter, yTop, width, height, color, facingRight);
    }

    /**
     * Draws a VTOL silhouette: small body with rotor disc above.
     */
    private void drawVtolSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("VTOL.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: simple helicopter shape
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillOval(xCenter - (int) (w * 0.45f), (int) (yTop + h * 0.05f), (int) (w * 0.9f), 4);
        g2d.fillOval((int) (left + w * 0.15f), (int) (yTop + h * 0.25f), (int) (w * 0.7f), (int) (h * 0.5f));
    }

    /**
     * Draws a naval vessel silhouette: hull with superstructure and bridge, wider than column.
     */
    private void drawNavalSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        BufferedImage image = loadSilhouetteImage("Surface_Naval.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, true);
            return;
        }

        // Fallback: simple hull shape
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillRect((int) (left + w * 0.05f), (int) (yTop + h * 0.3f), (int) (w * 0.9f), (int) (h * 0.7f));
        g2d.fillRect((int) (left + w * 0.3f), (int) yTop, (int) (w * 0.4f), (int) (h * 0.3f));
    }

    /**
     * Draws a submarine silhouette using a PNG image from data/images/units/LOS/. Falls back to a simple shape if the
     * image cannot be loaded.
     */
    private void drawSubmarineSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage submarineImage = loadSilhouetteImage("Submarine.png");
        if (submarineImage != null) {
            drawSilhouetteImage(g2d, submarineImage, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: simple oval hull with conning tower
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        g2d.setColor(color);
        g2d.fillOval((int) left, (int) (yTop + h * 0.3f), (int) w, (int) (h * 0.6f));
        g2d.setColor(color.darker());
        g2d.drawOval((int) left, (int) (yTop + h * 0.3f), (int) w, (int) (h * 0.6f));
        g2d.setColor(color);
        g2d.fillRect((int) (left + w * 0.4f), (int) yTop, (int) (w * 0.15f), (int) (h * 0.35f));
        g2d.setColor(color.darker());
        g2d.drawRect((int) (left + w * 0.4f), (int) yTop, (int) (w * 0.15f), (int) (h * 0.35f));
    }

    /**
     * Draws infantry silhouette: 3 small humanoid mek-like figures side by side.
     */
    private void drawInfantrySilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        drawMultiFigureSilhouette(g2d, "Infantry.png", xCenter, yTop, width, height, color, facingRight, 3);
    }

    /**
     * Draws a jump infantry silhouette: soldiers with jump packs.
     */
    private void drawJumpInfantrySilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        drawMultiFigureSilhouette(g2d, "Jump_Infantry.png", xCenter, yTop, width, height, color, facingRight, 3);
    }

    /**
     * Draws a motorized infantry silhouette: soldiers on motorcycles/ATVs.
     */
    private void drawMotorizedInfantrySilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        drawMultiFigureSilhouette(g2d, "Motorized_Infantry.png", xCenter, yTop, width, height, color, facingRight, 2);
    }

    /**
     * Draws a mechanized infantry silhouette: APC/IFV transported soldiers.
     */
    private void drawMechanizedInfantrySilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        drawMultiFigureSilhouette(g2d, "Mech_Infantry.png", xCenter, yTop, width, height, color, facingRight, 2);
    }

    /**
     * Helper to draw multiple copies of a silhouette image side by side, used for infantry types.
     */
    private void drawMultiFigureSilhouette(Graphics2D g2d, String filename, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight, int figureCount) {
        BufferedImage image = loadSilhouetteImage(filename);
        if (image != null) {
            float gap = width * 0.04f;
            float figureWidth = (width - gap * (figureCount - 1)) / figureCount;
            float totalWidth = figureWidth * figureCount + gap * (figureCount - 1);
            float startX = xCenter - totalWidth / 2;

            for (int i = 0; i < figureCount; i++) {
                int figCenterX = (int) (startX + i * (figureWidth + gap) + figureWidth / 2);
                drawSilhouetteImage(g2d, image, figCenterX, yTop, (int) figureWidth, height, color, facingRight);
            }
            return;
        }

        // Fallback: simple humanoid figures
        float gap = width * 0.04f;
        float figureWidth = (width - gap * (figureCount - 1)) / figureCount;
        float totalWidth = figureWidth * figureCount + gap * (figureCount - 1);
        float startX = xCenter - totalWidth / 2;

        for (int i = 0; i < figureCount; i++) {
            float fx = startX + i * (figureWidth + gap);
            float figureCenterX = fx + figureWidth / 2;
            drawSmallHumanoid(g2d, figureCenterX, yTop, figureWidth, height, color, false);
        }
    }

    /**
     * Draws a Battle Armor silhouette: 2 stocky armored humanoid figures side by side.
     */
    private void drawBattleArmorSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        drawMultiFigureSilhouette(g2d, "Battle_Armor.png", xCenter, yTop, width, height, color, facingRight, 2);
    }

    /**
     * Draws a single small humanoid mek-like figure. Used by infantry and battle armor silhouettes.
     *
     * @param armored if true, draws a stockier figure with wider shoulders (battle armor style)
     */
    private void drawSmallHumanoid(Graphics2D g2d, float xCenter, float yTop,
          float width, float height, Color color, boolean armored) {
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        // Armored figures are stockier with wider shoulders
        float shoulderWidth = armored ? 0.95f : 0.8f;
        float armWidth = armored ? 0.0f : 0.05f;
        float legWidth = armored ? 0.15f : 0.1f;

        GeneralPath path = new GeneralPath();
        // Left foot
        path.moveTo(left + w * legWidth, yTop + h);
        path.lineTo(left + w * (legWidth + 0.05f), yTop + h * 0.55f);
        // Left side up to shoulder
        path.lineTo(left + w * armWidth, yTop + h * 0.45f);
        path.lineTo(left + w * armWidth, yTop + h * 0.2f);
        // Head
        float headInset = (1.0f - shoulderWidth) / 2 + 0.1f;
        path.lineTo(left + w * headInset, yTop + h * 0.12f);
        path.lineTo(left + w * (headInset + 0.05f), yTop);
        path.lineTo(left + w * (1.0f - headInset - 0.05f), yTop);
        path.lineTo(left + w * (1.0f - headInset), yTop + h * 0.12f);
        // Right side down
        path.lineTo(left + w * (1.0f - armWidth), yTop + h * 0.2f);
        path.lineTo(left + w * (1.0f - armWidth), yTop + h * 0.45f);
        path.lineTo(left + w * (1.0f - legWidth - 0.05f), yTop + h * 0.55f);
        // Right foot
        path.lineTo(left + w * (1.0f - legWidth), yTop + h);
        // Crotch gap
        path.lineTo(left + w * 0.6f, yTop + h);
        path.lineTo(left + w * 0.5f, yTop + h * 0.68f);
        path.lineTo(left + w * 0.4f, yTop + h);
        path.closePath();

        g2d.setColor(color);
        g2d.fill(path);
        g2d.setColor(color.darker());
        g2d.draw(path);

        // Visor slit for armored figures
        if (armored) {
            g2d.setColor(color.darker().darker());
            int visorY = (int) (yTop + h * 0.07f);
            g2d.drawLine((int) (left + w * 0.4f), visorY, (int) (left + w * 0.6f), visorY);
        }
    }

    /**
     * Draws a ProtoMek silhouette: smaller, hunched mek-like shape.
     */
    private void drawProtoMekSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("ProtoMek.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: small hunched humanoid
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillRect((int) (left + w * 0.2f), (int) yTop, (int) (w * 0.6f), (int) (h * 0.5f));
        g2d.fillRect((int) (left + w * 0.1f), (int) (yTop + h * 0.5f), (int) (w * 0.35f), (int) (h * 0.5f));
        g2d.fillRect((int) (left + w * 0.55f), (int) (yTop + h * 0.5f), (int) (w * 0.35f), (int) (h * 0.5f));
    }

    /**
     * Draws an aerospace fighter silhouette: side-view delta wing shape.
     */
    private void drawAeroFighterSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("Aerospace_Fighter_Silhouette.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: simple swept-wing shape
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        GeneralPath path = new GeneralPath();
        path.moveTo(left, yTop + h * 0.45f);
        path.lineTo(left + w * 0.5f, yTop);
        path.lineTo(left + w, yTop + h * 0.3f);
        path.lineTo(left + w, yTop + h * 0.7f);
        path.lineTo(left + w * 0.5f, yTop + h);
        path.closePath();
        g2d.setColor(color);
        g2d.fill(path);
        g2d.setColor(color.darker());
        g2d.draw(path);
    }

    /**
     * Draws an IndustrialMek silhouette: blockier, boxier humanoid than a BattleMek. Side view.
     */
    private void drawIndustrialMekSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color, boolean facingRight) {
        BufferedImage image = loadSilhouetteImage("Industrial_Mek.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, facingRight);
            return;
        }

        // Fallback: boxy humanoid shape
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillRect((int) (left + w * 0.25f), (int) yTop, (int) (w * 0.5f), (int) (h * 0.55f));
        g2d.fillRect((int) left, (int) (yTop + h * 0.55f), (int) (w * 0.4f), (int) (h * 0.45f));
        g2d.fillRect((int) (left + w * 0.6f), (int) (yTop + h * 0.55f), (int) (w * 0.4f), (int) (h * 0.45f));
    }

    /**
     * Draws a support vehicle silhouette: truck/utility profile without turret. Side view.
     */
    private void drawSupportVehicleSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        BufferedImage image = loadSilhouetteImage("Support_Vehicle.png");
        if (image != null) {
            drawSilhouetteImage(g2d, image, xCenter, yTop, width, height, color, true);
            return;
        }

        // Fallback: simple truck shape
        float w = width;
        float h = height;
        float left = xCenter - w / 2;
        g2d.setColor(color);
        g2d.fillRect((int) left, (int) (yTop + h * 0.3f), (int) w, (int) (h * 0.7f));
        g2d.fillRect((int) left, (int) yTop, (int) (w * 0.35f), (int) (h * 0.3f));
    }

    /**
     * Draws a conventional fighter silhouette: straight-wing propeller/jet aircraft. Side view.
     */
    private void drawConvFighterSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        GeneralPath path = new GeneralPath();
        // Nose pointing left, rounder than ASF
        path.moveTo(left, yTop + h * 0.45f);
        // Top fuselage - gentler curve
        path.lineTo(left + w * 0.15f, yTop + h * 0.3f);
        path.lineTo(left + w * 0.3f, yTop + h * 0.25f);
        // Straight wing top
        path.lineTo(left + w * 0.4f, yTop);
        path.lineTo(left + w * 0.6f, yTop + h * 0.2f);
        // Fuselage to tail
        path.lineTo(left + w * 0.75f, yTop + h * 0.25f);
        // Tail fin
        path.lineTo(left + w * 0.9f, yTop + h * 0.05f);
        path.lineTo(left + w, yTop + h * 0.2f);
        path.lineTo(left + w, yTop + h * 0.5f);
        // Bottom fuselage
        path.lineTo(left + w * 0.75f, yTop + h * 0.65f);
        // Straight wing bottom
        path.lineTo(left + w * 0.6f, yTop + h * 0.7f);
        path.lineTo(left + w * 0.4f, yTop + h);
        path.lineTo(left + w * 0.3f, yTop + h * 0.65f);
        // Belly to nose
        path.lineTo(left + w * 0.15f, yTop + h * 0.6f);
        path.closePath();

        g2d.setColor(color);
        g2d.fill(path);
        g2d.setColor(color.darker());
        g2d.draw(path);
    }

    /**
     * Draws a DropShip silhouette: large egg/teardrop shape, pointed nose up. Side view.
     */
    private void drawDropShipSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        GeneralPath path = new GeneralPath();
        // Nose at top center
        path.moveTo(left + w * 0.5f, yTop);
        // Right side - curves outward
        path.curveTo(left + w * 0.75f, yTop + h * 0.15f,
              left + w, yTop + h * 0.35f,
              left + w, yTop + h * 0.55f);
        // Right engine area - widens at base
        path.lineTo(left + w * 0.95f, yTop + h * 0.75f);
        path.lineTo(left + w * 0.95f, yTop + h * 0.95f);
        // Engine nozzles at bottom
        path.lineTo(left + w * 0.85f, yTop + h);
        path.lineTo(left + w * 0.15f, yTop + h);
        // Left engine area
        path.lineTo(left + w * 0.05f, yTop + h * 0.95f);
        path.lineTo(left + w * 0.05f, yTop + h * 0.75f);
        // Left side - curves outward
        path.lineTo(left, yTop + h * 0.55f);
        path.curveTo(left, yTop + h * 0.35f,
              left + w * 0.25f, yTop + h * 0.15f,
              left + w * 0.5f, yTop);
        path.closePath();

        g2d.setColor(color);
        g2d.fill(path);
        g2d.setColor(color.darker());
        g2d.draw(path);

        // Engine glow line at bottom
        g2d.setColor(new Color(255, 200, 50, 150));
        g2d.drawLine((int) (left + w * 0.25f), (int) (yTop + h * 0.98f),
              (int) (left + w * 0.75f), (int) (yTop + h * 0.98f));
    }

    /**
     * Draws a SmallCraft silhouette: compact shuttle, smaller and rounder than a DropShip. Side view.
     */
    private void drawSmallCraftSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        GeneralPath path = new GeneralPath();
        // Nose pointing left
        path.moveTo(left, yTop + h * 0.45f);
        // Top fuselage - rounded
        path.curveTo(left + w * 0.15f, yTop + h * 0.1f,
              left + w * 0.4f, yTop,
              left + w * 0.7f, yTop + h * 0.15f);
        // Tail
        path.lineTo(left + w, yTop + h * 0.2f);
        path.lineTo(left + w, yTop + h * 0.7f);
        // Bottom fuselage - rounded
        path.curveTo(left + w * 0.7f, yTop + h * 0.85f,
              left + w * 0.4f, yTop + h,
              left + w * 0.15f, yTop + h * 0.8f);
        path.closePath();

        g2d.setColor(color);
        g2d.fill(path);
        g2d.setColor(color.darker());
        g2d.draw(path);
    }

    /**
     * Draws a JumpShip silhouette: long thin vessel with a large solar sail. Side view.
     */
    private void drawJumpShipSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        // Solar sail - large translucent rectangle at front
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        int sailWidth = (int) (w * 0.15f);
        g2d.fillRect((int) (left + w * 0.08f), (int) yTop, sailWidth, (int) h);
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2d.drawRect((int) (left + w * 0.08f), (int) yTop, sailWidth, (int) h);

        // Main hull - long thin cylinder
        GeneralPath hull = new GeneralPath();
        float hullTop = yTop + h * 0.35f;
        float hullBottom = yTop + h * 0.65f;
        hull.moveTo(left + w * 0.2f, yTop + h * 0.45f);
        hull.lineTo(left + w * 0.25f, hullTop);
        hull.lineTo(left + w * 0.9f, hullTop);
        hull.lineTo(left + w, yTop + h * 0.45f);
        hull.lineTo(left + w, yTop + h * 0.55f);
        hull.lineTo(left + w * 0.9f, hullBottom);
        hull.lineTo(left + w * 0.25f, hullBottom);
        hull.lineTo(left + w * 0.2f, yTop + h * 0.55f);
        hull.closePath();

        g2d.setColor(color);
        g2d.fill(hull);
        g2d.setColor(color.darker());
        g2d.draw(hull);

        // Sail mast connecting hull to sail
        g2d.setColor(color.darker());
        g2d.drawLine((int) (left + w * 0.22f), (int) (yTop + h * 0.5f),
              (int) (left + w * 0.15f), (int) (yTop + h * 0.5f));
    }

    /**
     * Draws a WarShip silhouette: large armed capital ship, broader than a JumpShip. Side view.
     */
    private void drawWarShipSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        GeneralPath hull = new GeneralPath();
        // Pointed bow left
        hull.moveTo(left, yTop + h * 0.5f);
        hull.lineTo(left + w * 0.15f, yTop + h * 0.3f);
        // Upper hull with weapon blisters
        hull.lineTo(left + w * 0.3f, yTop + h * 0.2f);
        hull.lineTo(left + w * 0.35f, yTop + h * 0.1f);
        hull.lineTo(left + w * 0.45f, yTop);
        hull.lineTo(left + w * 0.55f, yTop);
        hull.lineTo(left + w * 0.65f, yTop + h * 0.1f);
        hull.lineTo(left + w * 0.7f, yTop + h * 0.2f);
        // Stern upper
        hull.lineTo(left + w * 0.85f, yTop + h * 0.25f);
        hull.lineTo(left + w, yTop + h * 0.35f);
        // Engine block
        hull.lineTo(left + w * 0.98f, yTop + h * 0.45f);
        hull.lineTo(left + w * 0.98f, yTop + h * 0.55f);
        hull.lineTo(left + w, yTop + h * 0.65f);
        // Stern lower
        hull.lineTo(left + w * 0.85f, yTop + h * 0.75f);
        hull.lineTo(left + w * 0.7f, yTop + h * 0.8f);
        hull.lineTo(left + w * 0.65f, yTop + h * 0.9f);
        hull.lineTo(left + w * 0.55f, yTop + h);
        hull.lineTo(left + w * 0.45f, yTop + h);
        hull.lineTo(left + w * 0.35f, yTop + h * 0.9f);
        hull.lineTo(left + w * 0.3f, yTop + h * 0.8f);
        hull.lineTo(left + w * 0.15f, yTop + h * 0.7f);
        hull.closePath();

        g2d.setColor(color);
        g2d.fill(hull);
        g2d.setColor(color.darker());
        g2d.draw(hull);

        // Engine glow
        g2d.setColor(new Color(255, 200, 50, 150));
        g2d.drawLine((int) (left + w * 0.98f), (int) (yTop + h * 0.47f),
              (int) (left + w * 0.98f), (int) (yTop + h * 0.53f));
    }

    /**
     * Draws a SpaceStation silhouette: cylindrical/modular structure with docking rings. Side view.
     */
    private void drawSpaceStationSilhouette(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        float w = width;
        float h = height;
        float left = xCenter - w / 2;

        // Central cylinder
        float cylTop = yTop + h * 0.25f;
        float cylBottom = yTop + h * 0.75f;
        g2d.setColor(color);
        g2d.fillRect((int) (left + w * 0.2f), (int) cylTop,
              (int) (w * 0.6f), (int) (cylBottom - cylTop));
        g2d.setColor(color.darker());
        g2d.drawRect((int) (left + w * 0.2f), (int) cylTop,
              (int) (w * 0.6f), (int) (cylBottom - cylTop));

        // Habitat ring - left
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 140));
        g2d.fillOval((int) left, (int) yTop, (int) (w * 0.25f), (int) h);
        g2d.setColor(color.darker());
        g2d.drawOval((int) left, (int) yTop, (int) (w * 0.25f), (int) h);

        // Habitat ring - right
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 140));
        g2d.fillOval((int) (left + w * 0.75f), (int) yTop, (int) (w * 0.25f), (int) h);
        g2d.setColor(color.darker());
        g2d.drawOval((int) (left + w * 0.75f), (int) yTop, (int) (w * 0.25f), (int) h);

        // Docking spars connecting rings to cylinder
        g2d.setColor(color.darker());
        float midY = yTop + h * 0.5f;
        g2d.drawLine((int) (left + w * 0.12f), (int) midY, (int) (left + w * 0.2f), (int) midY);
        g2d.drawLine((int) (left + w * 0.8f), (int) midY, (int) (left + w * 0.88f), (int) midY);
    }

    /**
     * Loads a silhouette PNG from the LOS silhouette directory, caching the result.
     *
     * @param filename the image filename (e.g., "Submarine.png")
     *
     * @return the loaded image, or null if the file could not be loaded
     */
    private static BufferedImage loadSilhouetteImage(String filename) {
        if (silhouetteCache.containsKey(filename)) {
            return silhouetteCache.get(filename);
        }

        BufferedImage image = null;
        File imageFile = new File(Configuration.imagesDir(), LOS_SILHOUETTE_DIR + filename);
        System.err.println("[LOS] Loading silhouette: "
              + imageFile.getAbsolutePath()
              + " exists="
              + imageFile.exists());
        if (imageFile.exists()) {
            try {
                image = ImageIO.read(imageFile);
            } catch (Exception ignored) {
                // Failed to load - will cache null
            }
        }
        silhouetteCache.put(filename, image);
        return image;
    }

    /**
     * Creates a tinted copy of a silhouette image. Dark pixels (near black) are recolored to the target color, and
     * light pixels (near white) become transparent. This allows black-on-white silhouette PNGs to be drawn in any unit
     * color.
     *
     * @param source the source silhouette image (black on white)
     * @param color  the target tint color
     *
     * @return a new BufferedImage with tinted pixels
     */
    private static BufferedImage tintSilhouette(BufferedImage source, Color color) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage tinted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int targetRgb = color.getRGB() & 0x00FFFFFF;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getRGB(x, y);
                int sourceAlpha = (pixel >> 24) & 0xFF;

                // Skip fully transparent pixels
                if (sourceAlpha == 0) {
                    tinted.setRGB(x, y, 0x00000000);
                    continue;
                }

                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                // Luminance: how bright is this pixel (0 = black, 255 = white)
                int luminance = (red + green + blue) / 3;

                if (luminance < 180) {
                    // Dark pixel - tint to unit color, alpha based on darkness and source alpha
                    int alpha = Math.min(sourceAlpha, 255 - luminance);
                    tinted.setRGB(x, y, (alpha << 24) | targetRgb);
                } else {
                    // Light pixel - transparent
                    tinted.setRGB(x, y, 0x00000000);
                }
            }
        }
        return tinted;
    }

    /**
     * Draws a silhouette image scaled to fit the given bounds, maintaining aspect ratio and centered horizontally. The
     * image is anchored at the bottom (feet/keel on ground level).
     *
     * <p>The source image is assumed to face right (bow/nose pointing right). When {@code facingRight} is false,
     * the image is flipped horizontally so the unit faces left (toward the attacker).</p>
     *
     * @param facingRight true if the unit faces right (attacker), false to flip horizontally (target)
     */
    private void drawSilhouetteImage(Graphics2D g2d, BufferedImage image, int xCenter, int yTop,
          int maxWidth, int maxHeight, Color color, boolean facingRight) {
        BufferedImage tinted = tintSilhouette(image, color);

        // Scale to fill the full height (TW levels), but cap width to avoid spilling
        // far beyond the column. Uses the larger of maxWidth or 1.5x maxWidth as the cap.
        int imgWidth = tinted.getWidth();
        int imgHeight = tinted.getHeight();
        double scaleByHeight = (double) maxHeight / imgHeight;
        int widthIfFullHeight = (int) (imgWidth * scaleByHeight);

        // If width would exceed 1.5x the column width, constrain by width instead
        int maxAllowedWidth = (int) (maxWidth * 1.5);
        double scale;
        if (widthIfFullHeight > maxAllowedWidth) {
            scale = (double) maxAllowedWidth / imgWidth;
        } else {
            scale = scaleByHeight;
        }

        int drawWidth = (int) (imgWidth * scale);
        int drawHeight = (int) (imgHeight * scale);
        int drawX = xCenter - drawWidth / 2;
        // Anchor at bottom (feet on ground level)
        int drawY = yTop + maxHeight - drawHeight;

        if (facingRight) {
            g2d.drawImage(tinted, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            // Flip horizontally: draw from right edge to left edge
            g2d.drawImage(tinted, drawX + drawWidth, drawY, -drawWidth, drawHeight, null);
        }
    }

    /**
     * Draws a crosshair marker for empty hex selections (no unit present).
     */
    private void drawDefaultBar(Graphics2D g2d, int xCenter, int yTop,
          int width, int height, Color color) {
        BufferedImage image = loadCrosshairImage();
        if (image != null) {
            int size = Math.min(width, height);
            int drawX = xCenter - size / 2;
            int drawY = yTop + (height - size) / 2;
            g2d.drawImage(image, drawX, drawY, size, size, null);
            return;
        }

        // Fallback: simple colored rectangle
        g2d.setColor(color);
        g2d.fillRect(xCenter - width / 2, yTop, width, height);
        g2d.setColor(color.darker());
        g2d.drawRect(xCenter - width / 2, yTop, width, height);
    }

    /** Cached crosshair image for empty hex targets. */
    private static BufferedImage crosshairImage;
    private static boolean crosshairLoaded = false;

    private static BufferedImage loadCrosshairImage() {
        if (crosshairLoaded) {
            return crosshairImage;
        }
        crosshairLoaded = true;
        File imageFile = new File(Configuration.imagesDir(), "hexes" + File.separator + "nukeinc.gif");
        if (imageFile.exists()) {
            try {
                crosshairImage = ImageIO.read(imageFile);
            } catch (Exception ignored) {
                // Failed to load
            }
        }
        return crosshairImage;
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
        int targetLosHeight = diagramData.targetAbsHeight();
        int yEnd = metrics.levelToY(targetLosHeight);

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

        // Show unit info for attacker/target hexes
        if (hexIndex == 0 && !diagramData.attackerName().isEmpty()) {
            tooltip.append("<br><b>Attacker: ").append(diagramData.attackerName()).append("</b>");
            tooltip.append(" (height ").append(diagramData.attackerAbsHeight()).append(")");
            if (diagramData.attackerIsHullDown()) {
                tooltip.append(" [Hull Down]");
            }
        }
        if (hexIndex == hexPath.size() - 1 && !diagramData.targetName().isEmpty()) {
            tooltip.append("<br><b>Target: ").append(diagramData.targetName()).append("</b>");
            tooltip.append(" (height ").append(diagramData.targetAbsHeight()).append(")");
            if (diagramData.targetIsHullDown()) {
                tooltip.append(" [Hull Down]");
            }
        }

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
            int foliageTopElevation = hex.groundElevation() + hex.woodsHeight();
            boolean foliageAffectsLos = foliageTopElevation >= hex.losLineElevation();
            tooltip.append("<br>").append(densityStr).append(" ").append(foliageType);
            tooltip.append(" (top elev ").append(foliageTopElevation).append(")");
            if (!foliageAffectsLos) {
                tooltip.append(" - <i>LOS line above foliage</i>");
            }
        }
        if (hex.waterDepth() > 0) {
            tooltip.append("<br>Water: depth ").append(hex.waterDepth());
        }
        if (hex.smokeLevel() > 0) {
            int smokeTopElevation = hex.groundElevation() + 2;
            boolean smokeAffectsLos = smokeTopElevation >= hex.losLineElevation();
            tooltip.append("<br>Smoke: ").append(hex.smokeLevel() >= 2 ? "Heavy" : "Light");
            tooltip.append(" (top elev ").append(smokeTopElevation).append(")");
            if (!smokeAffectsLos) {
                tooltip.append(" - <i>LOS line above smoke</i>");
            }
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
            tooltip.append("<br><font color='red'>Blocks LOS (terrain)</font>");
        }
        if (hex.hasLosModifiers() && !hex.blocksLOS()) {
            boolean anyTerrainReachesLos = false;
            if (hex.smokeLevel() > 0) {
                anyTerrainReachesLos |= (hex.groundElevation() + 2) >= hex.losLineElevation();
            }
            if (hex.hasFoliage()) {
                anyTerrainReachesLos |= (hex.groundElevation() + hex.woodsHeight()) >= hex.losLineElevation();
            }
            if (hex.industrialHeight() > 0) {
                anyTerrainReachesLos |= (hex.groundElevation() + hex.industrialHeight()) >= hex.losLineElevation();
            }
            if (hex.hasScreen() || hex.hasFields() || hex.hasFire()) {
                anyTerrainReachesLos = true;
            }
            if (anyTerrainReachesLos) {
                tooltip.append("<br><font color='orange'>Affects LOS (modifier)</font>");
            } else {
                tooltip.append("<br><font color='green'>LOS line clears terrain</font>");
            }
        }

        tooltip.append("<br>LOS line elevation: ").append(String.format("%.1f", hex.losLineElevation()));
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
