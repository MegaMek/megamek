/*
 * MegaMek - Copyright (C) 2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.AWT;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Enumeration;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.Terrains;

/**
 * Displays a Preview of a Board
 */
public class MapPreview extends Canvas {

    // these indices match those in Terrains.java, and are therefore sensitive
    // to any changes there

    /**
     * 
     */
    private static final long serialVersionUID = -1116202683552362104L;
    private final static Color[] m_terrainColors = new Color[Terrains.SIZE];
    private static Color HEAVY_WOODS;
    private static Color ULTRA_HEAVY_WOODS;
    private static Color BACKGROUND;
    private static Color SINKHOLE;
    private static Color SMOKE_AND_FIRE;

    private final static int SHOW_NO_HEIGHT = 0;
    private final static int SHOW_GROUND_HEIGHT = 1;
    private final static int SHOW_BUILDING_HEIGHT = 2;
    private final static int SHOW_TOTAL_HEIGHT = 3;
    private final static int NBR_MODES = 3;

    private Image m_mapImage;
    private Dialog m_dialog;
    private IBoard m_board;
    private static final int margin = 6;
    private int topMargin;
    private int leftMargin;
    private static final int buttonHeight = 14;
    private boolean minimized = false;
    private int heightBufer;
    private Vector<int[]> roadHexIndexes = new Vector<int[]>();
    private int zoom = GUIPreferences.getInstance().getMinimapZoom();
    private int[] hexSide = { 3, 5, 6, 8, 10, 12 };
    private int[] hexSideByCos30 = { 3, 4, 5, 7, 9, 10 };
    private int[] hexSideBySin30 = { 2, 2, 3, 4, 5, 6 };
    private int[] halfRoadWidthByCos30 = { 0, 0, 1, 2, 2, 3 };
    private int[] halfRoadWidthBySin30 = { 0, 0, 1, 1, 1, 2 };
    private int[] halfRoadWidth = { 0, 0, 1, 2, 3, 3 };

    private int heightDisplayMode = SHOW_NO_HEIGHT;

    /**
     * Creates and lays out a new map preview.
     */
    public MapPreview(Dialog d, IBoard board) throws IOException {
        m_board = board;
        m_dialog = d;
        initializeColors();
        m_dialog.setResizable(false);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                processMouseClick(me.getX(), me.getY(), me);
            }
        });

        // TODO: replace this quick-and-dirty with some real size calculator.
        Dimension size = getSize();
        boolean updateSize = false;
        if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
            updateSize = true;
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
            updateSize = true;
        }
        if (updateSize) {
            setSize(size);
        }
        setLocation(GUIPreferences.getInstance().getMinimapPosX(),
                GUIPreferences.getInstance().getMinimapPosY());

    }

    /*
     * Initialize default colours and override with config file if there is one.
     */
    private void initializeColors() throws IOException {

        // set up defaults -- this might go away later...
        BACKGROUND = Color.black;
        m_terrainColors[0] = new Color(218, 215, 170);
        SINKHOLE = new Color(218, 215, 170);
        m_terrainColors[Terrains.WOODS] = new Color(180, 230, 130);
        HEAVY_WOODS = new Color(160, 200, 100);
        m_terrainColors[Terrains.ROUGH] = new Color(215, 181, 0);
        m_terrainColors[Terrains.RUBBLE] = new Color(200, 200, 200);
        m_terrainColors[Terrains.WATER] = new Color(200, 247, 253);
        m_terrainColors[Terrains.PAVEMENT] = new Color(204, 204, 204);
        m_terrainColors[Terrains.ROAD] = new Color(71, 79, 107);
        m_terrainColors[Terrains.FIRE] = Color.red;
        m_terrainColors[Terrains.SMOKE] = new Color(204, 204, 204);
        SMOKE_AND_FIRE = new Color(153, 0, 0);
        m_terrainColors[Terrains.SWAMP] = new Color(49, 136, 74);
        m_terrainColors[Terrains.BUILDING] = new Color(204, 204, 204);
        m_terrainColors[Terrains.BRIDGE] = new Color(109, 55, 25);
        m_terrainColors[Terrains.ICE] = new Color(204, 204, 255);
        m_terrainColors[Terrains.MAGMA] = new Color(200, 0, 0);
        m_terrainColors[Terrains.MUD] = new Color(218, 160, 100);
        m_terrainColors[Terrains.JUNGLE] = new Color(180, 230, 130);
        m_terrainColors[Terrains.FIELDS] = new Color(250, 255, 205);
        m_terrainColors[Terrains.INDUSTRIAL] = new Color(112, 138, 144);
        m_terrainColors[Terrains.SPACE] = Color.gray;

        // now try to read in the config file
        int red;
        int green;
        int blue;

        File coloursFile = new File(
                "data/images/hexes/" + GUIPreferences.getInstance().getMinimapColours()); //$NON-NLS-1$

        // only while the defaults are hard-coded!
        if (!coloursFile.exists()) {
            return;
        }

        Reader cr = new FileReader(coloursFile);
        StreamTokenizer st = new StreamTokenizer(cr);

        st.lowerCaseMode(true);
        st.quoteChar('"');
        st.commentChar('#');

        scan: while (true) {
            red = 0;
            green = 0;
            blue = 0;

            switch (st.nextToken()) {
                case StreamTokenizer.TT_EOF:
                    break scan;
                case StreamTokenizer.TT_EOL:
                    break scan;
                case StreamTokenizer.TT_WORD:
                    // read in
                    String key = st.sval;
                    if (key.equals("unitsize")) { //$NON-NLS-1$
                        st.nextToken();
                    } else if (key.equals("background")) { //$NON-NLS-1$
                        st.nextToken();
                        red = (int) st.nval;
                        st.nextToken();
                        green = (int) st.nval;
                        st.nextToken();
                        blue = (int) st.nval;

                        BACKGROUND = new Color(red, green, blue);
                    } else if (key.equals("heavywoods")) { //$NON-NLS-1$
                        st.nextToken();
                        red = (int) st.nval;
                        st.nextToken();
                        green = (int) st.nval;
                        st.nextToken();
                        blue = (int) st.nval;

                        HEAVY_WOODS = new Color(red, green, blue);
                    } else if (key.equals("ultraheavywoods")) { //$NON-NLS-1$
                        st.nextToken();
                        red = (int) st.nval;
                        st.nextToken();
                        green = (int) st.nval;
                        st.nextToken();
                        blue = (int) st.nval;

                        ULTRA_HEAVY_WOODS = new Color(red, green, blue);
                    } else if (key.equals("sinkhole")) { //$NON-NLS-1$
                        st.nextToken();
                        red = (int) st.nval;
                        st.nextToken();
                        green = (int) st.nval;
                        st.nextToken();
                        blue = (int) st.nval;

                        SINKHOLE = new Color(red, green, blue);
                    } else if (key.equals("smokeandfire")) { //$NON-NLS-1$
                        st.nextToken();
                        red = (int) st.nval;
                        st.nextToken();
                        green = (int) st.nval;
                        st.nextToken();
                        blue = (int) st.nval;

                        SMOKE_AND_FIRE = new Color(red, green, blue);
                    } else {
                        st.nextToken();
                        red = (int) st.nval;
                        st.nextToken();
                        green = (int) st.nval;
                        st.nextToken();
                        blue = (int) st.nval;

                        m_terrainColors[Terrains.getType(key)] = new Color(red,
                                green, blue);
                    }
            }
        }

        cr.close();
    }

    public void initializeMap() {
        // sanity check (cfg file could be hosed)
        if (zoom < 0) {
            zoom = 0;
        } else if (zoom > (hexSide.length - 1)) {
            zoom = (hexSide.length - 1);
        }
        int requiredWidth, requiredHeight;
        int currentHexSide = hexSide[zoom];
        int currentHexSideByCos30 = hexSideByCos30[zoom];
        int currentHexSideBySin30 = hexSideBySin30[zoom];
        topMargin = margin;
        leftMargin = margin;
        requiredWidth = m_board.getWidth()
                * (currentHexSide + currentHexSideBySin30)
                + currentHexSideBySin30 + 2 * margin;
        requiredHeight = (2 * m_board.getHeight() + 1) * currentHexSideByCos30
                + 2 * margin + buttonHeight;
        setSize(requiredWidth, requiredHeight);
        m_dialog.pack();
        // m_dialog.show();
        m_mapImage = createImage(getSize().width, getSize().height);
        if (getSize().width > requiredWidth)
            leftMargin = ((getSize().width - requiredWidth) / 2) + margin;
        if (getSize().height > requiredHeight)
            topMargin = ((getSize().height - requiredHeight) / 2) + margin;
        drawMap();
    }

    // draw everything
    public synchronized void drawMap() {
        if (m_mapImage == null) {
            return;
        }

        if (!m_dialog.isVisible())
            return;

        Graphics g = m_mapImage.getGraphics();
        Color oldColor = g.getColor();
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, getSize().width, getSize().height);
        g.setColor(oldColor);
        roadHexIndexes.removeAllElements();
        for (int j = 0; j < m_board.getWidth(); j++) {
            for (int k = 0; k < m_board.getHeight(); k++) {
                IHex h = m_board.getHex(j, k);
                g.setColor(terrainColor(h, j, k));
                paintCoord(g, j, k, true);
            }
        }

        if (!roadHexIndexes.isEmpty())
            paintRoads(g);

        if (SHOW_NO_HEIGHT != heightDisplayMode) {
            for (int j = 0; j < m_board.getWidth(); j++) {
                for (int k = 0; k < m_board.getHeight(); k++) {
                    IHex h = m_board.getHex(j, k);
                    paintHeight(g, h, j, k);
                }
            }
        }
        drawBtn(g);
        repaint();
    }

    public void paint(Graphics g) {
        if (m_mapImage != null) {
            g.drawImage(m_mapImage, 0, 0, this);
        }
    }

    private void paintHeight(Graphics g, IHex h, int x, int y) {
        if (heightDisplayMode == SHOW_NO_HEIGHT)
            return;
        if (zoom > 2) {
            int baseX = x * (hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin;
            int baseY = (2 * y + 1 + x % 2) * hexSideByCos30[zoom] + topMargin;
            g.setColor(Color.white);
            int height = 0;
            if (h.getTerrain(Terrains.BUILDING) != null
                    && heightDisplayMode == SHOW_BUILDING_HEIGHT) {
                height = h.ceiling();
            } else if (heightDisplayMode == SHOW_GROUND_HEIGHT) {
                height = h.floor();
            } else if (heightDisplayMode == SHOW_TOTAL_HEIGHT) {
                height = ((h.getTerrain(Terrains.BUILDING) != null) || (h
                        .getTerrain(Terrains.FUEL_TANK) != null)) ? h.ceiling()
                        : h.floor();
            }
            if (height != 0) {
                g.drawString(height + "", baseX + 5, baseY + 5); //$NON-NLS-1$
            }
        }
    }

    private void paintCoord(Graphics g, int x, int y, boolean border) {
        int baseX = x * (hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin;
        int baseY = (2 * y + 1 + x % 2) * hexSideByCos30[zoom] + topMargin;
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        xPoints[0] = baseX;
        yPoints[0] = baseY;
        xPoints[1] = baseX + hexSideBySin30[zoom];
        yPoints[1] = baseY + hexSideByCos30[zoom];
        xPoints[2] = xPoints[1] + hexSide[zoom];
        yPoints[2] = yPoints[1];
        xPoints[3] = xPoints[2] + hexSideBySin30[zoom];
        yPoints[3] = baseY;
        xPoints[4] = xPoints[2];
        yPoints[4] = baseY - hexSideByCos30[zoom];
        xPoints[5] = xPoints[1];
        yPoints[5] = yPoints[4];
        g.fillPolygon(xPoints, yPoints, 6);
        if (border) {
            Color oldColor = g.getColor();
            g.setColor(oldColor.darker());
            g.drawPolygon(xPoints, yPoints, 6);
            g.setColor(oldColor);
        }
    }

    private void paintRoads(Graphics g) {
        int exits = 0;
        int baseX, baseY, x, y;
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];
        Color oldColor = g.getColor();
        g.setColor(m_terrainColors[Terrains.ROAD]);
        for (Enumeration<int[]> iter = roadHexIndexes.elements(); iter
                .hasMoreElements();) {
            int[] hex = iter.nextElement();
            x = hex[0];
            y = hex[1];
            baseX = x * (hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin
                    + hexSide[zoom];
            baseY = (2 * y + 1 + x % 2) * hexSideByCos30[zoom] + topMargin;
            exits = hex[2];
            // Is there a North exit?
            if (0 != (exits & 0x0001)) {
                xPoints[0] = baseX - halfRoadWidth[zoom];
                yPoints[0] = baseY;
                xPoints[1] = baseX - halfRoadWidth[zoom];
                yPoints[1] = baseY - hexSideByCos30[zoom];
                xPoints[2] = baseX + halfRoadWidth[zoom];
                yPoints[2] = baseY - hexSideByCos30[zoom];
                xPoints[3] = baseX + halfRoadWidth[zoom];
                yPoints[3] = baseY;
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a North-East exit?
            if (0 != (exits & 0x0002)) {
                xPoints[0] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY - halfRoadWidthByCos30[zoom];
                xPoints[1] = Math.round(baseX + 3 * hexSide[zoom] / 4
                        - halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round(baseY - hexSideByCos30[zoom] / 2
                        - halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] + 2 * halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] + 2 * halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY + halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a South-East exit?
            if (0 != (exits & 0x0004)) {
                xPoints[0] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY - halfRoadWidthByCos30[zoom];
                xPoints[1] = Math.round(baseX + 3 * hexSide[zoom] / 4
                        + halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round(baseY + hexSideByCos30[zoom] / 2
                        - halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] - 2 * halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] + 2 * halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY + halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a South exit?
            if (0 != (exits & 0x0008)) {
                xPoints[0] = baseX + halfRoadWidth[zoom];
                yPoints[0] = baseY;
                xPoints[1] = baseX + halfRoadWidth[zoom];
                yPoints[1] = baseY + hexSideByCos30[zoom];
                xPoints[2] = baseX - halfRoadWidth[zoom];
                yPoints[2] = baseY + hexSideByCos30[zoom];
                xPoints[3] = baseX - halfRoadWidth[zoom];
                yPoints[3] = baseY;
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a South-West exit?
            if (0 != (exits & 0x0010)) {
                xPoints[0] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY + halfRoadWidthByCos30[zoom];
                xPoints[1] = Math.round(baseX - 3 * hexSide[zoom] / 4
                        + halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round(baseY + hexSideByCos30[zoom] / 2
                        + halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] - 2 * halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] - 2 * halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY - halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a North-West exit?
            if (0 != (exits & 0x0020)) {
                xPoints[0] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY + halfRoadWidthByCos30[zoom];
                xPoints[1] = Math.round(baseX - 3 * hexSide[zoom] / 4
                        - halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round(baseY - hexSideByCos30[zoom] / 2
                        + halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] + 2 * halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] - 2 * halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY - halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }

        }
        g.setColor(oldColor);
    }

    private Color terrainColor(IHex x, int boardX, int boardY) {
        Color terrColor = m_terrainColors[0];
        if (x.getElevation() < 0) {
            // sinkholes have their own colour
            terrColor = SINKHOLE;
        }

        int level = 0;
        int terrain = 0;
        for (int j = m_terrainColors.length - 1; j >= 0; j--) {
            if (x.getTerrain(j) != null && m_terrainColors[j] != null) {
                if (j == Terrains.ROAD || j == Terrains.BRIDGE) {
                    int[] roadHex = { boardX, boardY,
                            x.getTerrain(j).getExits() };
                    roadHexIndexes.addElement(roadHex);
                    continue;
                }
                terrColor = m_terrainColors[j];
                terrain = j;
                // make heavy woods darker
                if ((j == Terrains.WOODS || j == Terrains.JUNGLE)
                        && x.getTerrain(j).getLevel() == 2) {
                    terrColor = HEAVY_WOODS;
                }
                if ((j == Terrains.WOODS || j == Terrains.JUNGLE)
                        && x.getTerrain(j).getLevel() > 2) {
                    terrColor = ULTRA_HEAVY_WOODS;
                }
                // contains both smoke and fire
                if (j == Terrains.SMOKE && x.getTerrain(Terrains.FIRE) != null) {
                    terrColor = SMOKE_AND_FIRE;
                }
                break;
            }
        }

        int r, g, b;
        switch (terrain) {
            case 0:
            case Terrains.WOODS:
            case Terrains.JUNGLE:
            case Terrains.ROUGH:
            case Terrains.RUBBLE:
            case Terrains.WATER:
            case Terrains.PAVEMENT:
            case Terrains.ICE:
            case Terrains.FIELDS:
                level = Math.abs(x.floor());
                // By experiment it is possible to make only 6 distinctive color
                // steps
                if (level > 10)
                    level = 10;
                r = terrColor.getRed() - level * 15;
                g = terrColor.getGreen() - level * 15;
                b = terrColor.getBlue() - level * 15;
                if (r < 0)
                    r = 0;
                if (g < 0)
                    g = 0;
                if (b < 0)
                    b = 0;
                return new Color(r, g, b);
            case Terrains.FUEL_TANK:
            case Terrains.BUILDING:
                level = Math.abs(x.ceiling());
                // By experiment it is possible to make only 6 distinctive color
                // steps
                if (level > 10)
                    level = 10;
                r = terrColor.getRed() - level * 15;
                g = terrColor.getGreen() - level * 15;
                b = terrColor.getBlue() - level * 15;
                if (r < 0)
                    r = 0;
                if (g < 0)
                    g = 0;
                if (b < 0)
                    b = 0;
                return new Color(r, g, b);
        }
        return terrColor;
    }

    /**
     * Draws green Button in the bottom to close and open mini map. Height of
     * button is fixed to 14pix.
     */
    private void drawBtn(Graphics g) {
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        Color oldColor = g.getColor();
        if (minimized) {
            xPoints[0] = Math.round((getSize().width - 11) / 2);
            yPoints[0] = getSize().height - 10;
            xPoints[1] = xPoints[0] + 11;
            yPoints[1] = yPoints[0];
            xPoints[2] = xPoints[0] + 6;
            yPoints[2] = yPoints[0] + 5;
        } else {
            xPoints[0] = Math.round((getSize().width - 11) / 2);
            yPoints[0] = getSize().height - 4;
            xPoints[1] = xPoints[0] + 11;
            yPoints[1] = yPoints[0];
            xPoints[2] = xPoints[0] + 5;
            yPoints[2] = yPoints[0] - 5;
        }
        g.setColor(Color.green.darker().darker());
        g.fillRect(0, getSize().height - 14, getSize().width, 14);
        g.setColor(Color.green.darker());
        g.drawLine(0, getSize().height - 14, getSize().width,
                getSize().height - 14);
        g.drawLine(0, getSize().height - 14, 0, getSize().height);
        g.setColor(Color.black);
        g.drawLine(0, getSize().height - 1, getSize().width,
                getSize().height - 1);
        g.drawLine(getSize().width - 1, getSize().height - 14,
                getSize().width - 1, getSize().height);
        g.setColor(Color.yellow);
        g.fillPolygon(xPoints, yPoints, 3);

        // drawing "+" and "-" buttons
        if (!minimized) {
            g.setColor(Color.black);
            g.drawLine(14 - 1, getSize().height - 14, 14 - 1, getSize().height);
            g.drawLine(getSize().width - 14 - 1, getSize().height - 14,
                    getSize().width - 14 - 1, getSize().height);
            g.setColor(Color.green.darker());
            g.drawLine(14, getSize().height - 14, 14, getSize().height);
            g.drawLine(getSize().width - 14, getSize().height - 14,
                    getSize().width - 14, getSize().height);
            if (zoom == 0) {
                g.setColor(Color.gray.brighter());
            } else {
                g.setColor(Color.yellow);
            }
            g.fillRect(3, getSize().height - 14 + 6, 8, 2);
            if (zoom == (hexSide.length - 1)) {
                g.setColor(Color.gray.brighter());
            } else {
                g.setColor(Color.yellow);
            }
            g.fillRect(getSize().width - 14 + 3, getSize().height - 14 + 6, 8,
                    2);
            g.fillRect(getSize().width - 14 + 6, getSize().height - 14 + 3, 2,
                    8);

            if (zoom > 2) {
                // Button for displying heights.
                g.setColor(Color.black);
                g.drawLine(28 - 1, getSize().height - 14, 28 - 1,
                        getSize().height);
                g.setColor(Color.green.darker());
                g.drawLine(28, getSize().height - 14, 28, getSize().height);
                g.setColor(Color.yellow);
                String label;
                switch (heightDisplayMode) {
                    case SHOW_NO_HEIGHT:
                        label = Messages.getString("MiniMap.NoHeightLabel"); //$NON-NLS-1$
                        break;
                    case SHOW_GROUND_HEIGHT:
                        label = Messages.getString("MiniMap.GroundHeightLabel"); //$NON-NLS-1$
                        break;
                    case SHOW_BUILDING_HEIGHT:
                        label = Messages
                                .getString("MiniMap.BuildingHeightLabel"); //$NON-NLS-1$
                        break;
                    case SHOW_TOTAL_HEIGHT:
                        label = Messages.getString("MiniMap.TotalHeightLabel"); //$NON-NLS-1$
                        break;
                    default:
                        label = ""; //$NON-NLS-1$
                }
                g.drawString(label, 17, getSize().height - 14 + 12);
            }
        }

        g.setColor(oldColor);

    }

    private void processMouseClick(int x, int y, MouseEvent me) {
        if (y > (getSize().height - 14)) {

            if (x < 14) {
                if (zoom == 0)
                    return;
                zoom--;
                initializeMap();
            } else if (x < 28 && zoom > 2) {
                heightDisplayMode = ((++heightDisplayMode) > NBR_MODES) ? 0
                        : heightDisplayMode;
                initializeMap();
            } else if (x > (getSize().width - 14)) {
                if (zoom == (hexSide.length - 1))
                    return;
                zoom++;
                initializeMap();
            } else {
                if (minimized) {
                    // m_dialog.setResizable(true);
                    setSize(getSize().width, heightBufer);
                    m_mapImage = createImage(getSize().width, heightBufer);
                } else {
                    heightBufer = getSize().height;
                    setSize(getSize().width, 14);
                    m_mapImage = createImage(Math.max(1, getSize().width), 14);
                    // m_dialog.setResizable(false);
                }
                minimized = !minimized;
                m_dialog.pack();
                drawMap();
            }
        }
    }
}
