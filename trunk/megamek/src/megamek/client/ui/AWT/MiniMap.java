/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;


import megamek.common.*;

/**
 * Displays the info for a mech.  This is also a sort
 * of interface for special movement and firing actions.
 */
public class MiniMap extends Canvas
implements BoardListener, MouseListener, ComponentListener, GameListener {
    // these indices match those in Terrain.java, and are therefore sensitive to any changes there

    private final static Color[] m_terrainColors = new Color[Terrain.SIZE];

    private Image m_mapImage;
    private BoardView1 m_bview;
    private Game m_game;
    private Dialog m_dialog;
    private final double cos30 = 0.866;
    private int          hexSide;
    private final int    minHexSide = 3;
    private int          hexSideByCos30;
    private int          hexSideBySin30;
    private int          minMargin = 3;
    private int          topMargin;
    private int          leftMargin;
    private int          heightBufer;
    private boolean      minimized = false;
    private final int    unitSize = 6; //variable which define size of triangle for unit representation
    private int          halfRoadWidth;
    private int          halfRoadWidthByCos30;
    private int          halfRoadWidthBySin30;
    private Vector       roadHexIndexes = new Vector();
    /**
     * Creates and lays out a new mech display.
     */
    public MiniMap(Dialog d, Client c, BoardView1 bview) {
        m_game = c.game;
        m_bview = bview;
        m_dialog = d;
        initializeColors();
        m_game.board.addBoardListener(this);
        c.addGameListener(this);
        addMouseListener(this);
        addComponentListener(this);
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        if (m_mapImage != null) {
            g.drawImage(m_mapImage, 0, 0, this);
        }
    }


    private void initializeColors() {
        m_terrainColors[0] = new Color(218,215,170);
        m_terrainColors[Terrain.WOODS] = new Color(180,230,130);
        m_terrainColors[Terrain.ROUGH] = new Color(215,181,0);
        m_terrainColors[Terrain.RUBBLE] = new Color(200,200,200);
        m_terrainColors[Terrain.WATER] = new Color(200,247,253);
        m_terrainColors[Terrain.PAVEMENT] = new Color(204,204,204);
        m_terrainColors[Terrain.ROAD] = new Color(71,79,107);
        m_terrainColors[Terrain.FIRE] = Color.red;
        m_terrainColors[Terrain.SMOKE] = new Color(204,204,204);
        m_terrainColors[Terrain.SWAMP] = new Color(49,136,74);
        m_terrainColors[Terrain.BUILDING] = new Color(204,204,204);
        m_terrainColors[Terrain.BRIDGE] = new Color(109,55,25);

    }

    private void initializeMap() {
        double heightFactor = (2*m_game.board.height+1)*cos30;
        double widthFactor = (1.5 * m_game.board.width + 0.5);
        m_dialog.show();
        hexSide = (int) ( Math.min((getSize().height - 14 - minMargin*2)/heightFactor, (getSize().width - minMargin*2)/widthFactor));
        hexSide = Math.max(hexSide, minHexSide);
        hexSideByCos30 = (int) Math.round(hexSide* cos30);
        hexSideBySin30 = (int) Math.round(hexSide/2);
        halfRoadWidth =  (int) (hexSide/4);
        halfRoadWidthByCos30 = (int) (hexSide*cos30/4);
        halfRoadWidthBySin30 = (int) (hexSide/8);
        if ((halfRoadWidthByCos30 == 0) ||(halfRoadWidthBySin30 == 0)){
            halfRoadWidth = 0;
            halfRoadWidthByCos30 = 0;
            halfRoadWidthBySin30 = 0;
        }
        leftMargin = (int)((getSize().width - m_game.board.width*(hexSide + hexSideBySin30) - hexSideBySin30)/2);
        topMargin =(int)(((getSize().height - 14) - hexSideByCos30*(2*m_game.board.height + 1))/2);
        m_mapImage = createImage(Math.max(1, getSize().width), Math.max(1, getSize().height)); // area must be > 0
        drawMap();
    }

    // draw everything
    private void drawMap() {
        if (m_mapImage == null) {
            return;
        }
        Graphics g = m_mapImage.getGraphics();
        Color oldColor = g.getColor();
        g.setColor(Color.black);
        g.fillRect(0,0,getSize().width,getSize().height);
        g.setColor(oldColor);
        if (!minimized){
            roadHexIndexes.removeAllElements();
            for (int j = 0; j < m_game.board.width; j++) {
                for (int k = 0; k < m_game.board.height; k++) {
                    g.setColor(terrainColor(m_game.board.getHex(j, k), j, k));
                    paintCoord(g, j, k, true);
                }
            }
            if (! roadHexIndexes.isEmpty()) paintRoads(g);

            for (Enumeration enum = m_game.getEntities(); enum.hasMoreElements(); ) {
                Entity e = (Entity)enum.nextElement();
                if (e.getPosition() == null) continue;
                g.setColor(e.getOwner().getColor());
                paintUnit(g, e.getPosition().x, e.getPosition().y, true);
            }
        }
        drawBtn(g);
        repaint();
    }
    //Draws green Button in the bottom to close and open mini map. Height of button is fixed to 14pix.
    private void drawBtn(Graphics g){
        int [] xPoints = new int[3];
        int [] yPoints = new int[3];
        Color oldColor = g.getColor();
        if (minimized){
            xPoints[0] = (int)Math.round((getSize().width - 11) / 2);
            yPoints[0] = getSize().height - 10;
            xPoints[1] = xPoints[0] + 11;
            yPoints[1] = yPoints[0];
            xPoints[2] = xPoints[0] + 6;
            yPoints[2] = yPoints[0] + 5;
        } else {
            xPoints[0] = (int)Math.round((getSize().width - 11) / 2);
            yPoints[0] = getSize().height - 4;
            xPoints[1] = xPoints[0] + 11;
            yPoints[1] = yPoints[0];
            xPoints[2] = xPoints[0] + 5;
            yPoints[2] = yPoints[0] - 5;
        }
        g.setColor(Color.green.darker().darker());
        g.fillRect(0,getSize().height - 14,getSize().width,14);
        g.setColor(Color.green.darker());
        g.drawLine(0,getSize().height - 14,getSize().width,getSize().height -14);
        g.drawLine(0,getSize().height - 14,0,getSize().height);
        g.setColor(Color.black);
        g.drawLine(0,getSize().height-1,getSize().width,getSize().height-1);
        g.drawLine(getSize().width-1,getSize().height - 14,getSize().width-1,getSize().height);
        g.setColor(Color.yellow);
        g.fillPolygon(xPoints,yPoints,3);
        g.setColor(oldColor);
    }


    private void paintCoord(Graphics g, int x, int y, boolean border) {
        int baseX = x *(hexSide + hexSideBySin30) + leftMargin;
        int baseY = (2*y + 1 + x%2)* hexSideByCos30 + topMargin;
        int [] xPoints = new int[6];
        int [] yPoints = new int[6];
        xPoints[0] = baseX;
        yPoints[0] = baseY;
        xPoints[1] = baseX + hexSideBySin30;
        yPoints[1] = baseY + hexSideByCos30;
        xPoints[2] = baseX + hexSideBySin30 + hexSide;
        yPoints[2] = baseY + hexSideByCos30;
        xPoints[3] = baseX + hexSideBySin30 + hexSide + hexSideBySin30;
        yPoints[3] = baseY;
        xPoints[4] = baseX + hexSideBySin30 + hexSide;
        yPoints[4] = baseY - hexSideByCos30;
        xPoints[5] = baseX + hexSideBySin30;
        yPoints[5] = baseY - hexSideByCos30;
        g.fillPolygon(xPoints,yPoints,6);
        if (border) {
            Color oldColor = g.getColor();
            g.setColor(oldColor.darker());
            g.drawPolygon(xPoints,yPoints,6);
            g.setColor(oldColor);
        }
    }

    private void paintUnit (Graphics g, int x, int y, boolean border) {
        int baseX = x *(hexSide + hexSideBySin30) + leftMargin + hexSide;
        int baseY = (2*y + 1 + x%2)* hexSideByCos30 + topMargin;
        int [] xPoints = new int[3];
        int [] yPoints = new int[3];
        xPoints[0] = baseX;
        yPoints[0] = baseY - unitSize;
        xPoints[1] = baseX - unitSize;
        yPoints[1] = baseY + unitSize/2;
        xPoints[2] = baseX + unitSize;
        yPoints[2] = baseY + unitSize/2;
        g.fillPolygon(xPoints,yPoints,3);
        if (border) {
            Color oldColor = g.getColor();
            g.setColor(oldColor.darker().darker().darker());
            g.drawPolygon(xPoints,yPoints,3);
            g.setColor(oldColor);
        }
    }

    private void paintRoads (Graphics g){
        int exits = 0;
        int baseX, baseY, x, y;
        int [] xPoints = new int[4];
        int [] yPoints = new int[4];
        Color oldColor = g.getColor();
        g.setColor(m_terrainColors[Terrain.ROAD]);
        for (Enumeration enum = roadHexIndexes.elements(); enum.hasMoreElements(); ){
            int[] hex = (int[])enum.nextElement();
            x = hex[0];
            y = hex[1];
            baseX = x *(hexSide + hexSideBySin30) + leftMargin + hexSide;
            baseY = (2*y + 1 + x%2)* hexSideByCos30 + topMargin;
            exits = hex[2];
            // Is there a North exit?
            if ( 0 != (exits & 0x0001) ) {
                xPoints[0] = baseX - halfRoadWidth ;
                yPoints[0] = baseY;
                xPoints[1] = baseX - halfRoadWidth;
                yPoints[1] = baseY - hexSideByCos30;
                xPoints[2] = baseX + halfRoadWidth;
                yPoints[2] = baseY - hexSideByCos30;
                xPoints[3] = baseX + halfRoadWidth;
                yPoints[3] = baseY;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a North-East exit?
            if ( 0 != (exits & 0x0002) ) {
                xPoints[0] = baseX - halfRoadWidthBySin30;
                yPoints[0] = baseY - halfRoadWidthByCos30;
                xPoints[1] = (int) Math.round(baseX + 3*hexSide/4 - halfRoadWidthBySin30);
                yPoints[1] = (int) Math.round(baseY - hexSideByCos30/2 - halfRoadWidthByCos30);
                xPoints[2] = xPoints[1] + 2 * halfRoadWidthBySin30;
                yPoints[2] = yPoints[1] + 2 * halfRoadWidthByCos30;
                xPoints[3] = baseX + halfRoadWidthBySin30;
                yPoints[3] = baseY + halfRoadWidthByCos30;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
                  // Is there a South-East exit?
            if ( 0 != (exits & 0x0004) ) {
                xPoints[0] = baseX + halfRoadWidthBySin30;
                yPoints[0] = baseY - halfRoadWidthByCos30;
                xPoints[1] = (int) Math.round(baseX + 3*hexSide/4 + halfRoadWidthBySin30);
                yPoints[1] = (int) Math.round(baseY + hexSideByCos30/2 - halfRoadWidthByCos30);
                xPoints[2] = xPoints[1] - 2 * halfRoadWidthBySin30;
                yPoints[2] = yPoints[1] + 2 * halfRoadWidthByCos30;
                xPoints[3] = baseX - halfRoadWidthBySin30;
                yPoints[3] = baseY + halfRoadWidthByCos30;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a South exit?
            if ( 0 != (exits & 0x0008) ) {
                xPoints[0] = baseX + halfRoadWidth;
                yPoints[0] = baseY;
                xPoints[1] = baseX + halfRoadWidth;
                yPoints[1] = baseY + hexSideByCos30;
                xPoints[2] = baseX - halfRoadWidth;
                yPoints[2] = baseY + hexSideByCos30;
                xPoints[3] = baseX - halfRoadWidth;;
                yPoints[3] = baseY;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a South-West exit?
            if ( 0 != (exits & 0x0010) ) {
                xPoints[0] = baseX + halfRoadWidthBySin30;
                yPoints[0] = baseY + halfRoadWidthByCos30;
                xPoints[1] = (int) Math.round(baseX - 3*hexSide/4 + halfRoadWidthBySin30);
                yPoints[1] = (int) Math.round(baseY + hexSideByCos30/2 + halfRoadWidthByCos30);
                xPoints[2] = xPoints[1] - 2*halfRoadWidthBySin30;
                yPoints[2] = yPoints[1] - 2*halfRoadWidthByCos30;
                xPoints[3] = baseX - halfRoadWidthBySin30;
                yPoints[3] = baseY - halfRoadWidthByCos30;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a North-West exit?
            if ( 0 != (exits & 0x0020) ) {
                xPoints[0] = baseX - halfRoadWidthBySin30;
                yPoints[0] = baseY + halfRoadWidthByCos30;
                xPoints[1] = (int) Math.round(baseX - 3*hexSide/4 - halfRoadWidthBySin30);
                yPoints[1] = (int) Math.round(baseY - hexSideByCos30/2 + halfRoadWidthByCos30);
                xPoints[2] = xPoints[1] + 2*halfRoadWidthBySin30;;
                yPoints[2] = yPoints[1] - 2*halfRoadWidthByCos30;;
                xPoints[3] = baseX + halfRoadWidthBySin30;
                yPoints[3] = baseY - halfRoadWidthByCos30;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }

        }
        g.setColor(oldColor);
    }


    private Color terrainColor(Hex x, int boardX, int boardY) {
        Color terrColor = m_terrainColors[0];
        int level = 0;
        int terrain = 0;
        for (int j = m_terrainColors.length - 1; j >= 0; j--) {
            if (x.getTerrain(j) != null && m_terrainColors[j] != null) {
                if (j == Terrain.ROAD){
                    int [] roadHex = {boardX, boardY, x.getTerrain(j).getExits()};
                    roadHexIndexes.addElement(roadHex);
                    continue;
                }
                terrColor = m_terrainColors[j];
                terrain = j;
                break;
            }
        }
        if (terrain < 5){
             level = (int) Math.abs(x.floor());
             // By experiment it is possible to make only 6 distinctive color steps
             if (level > 5) level = 5;
             int r = terrColor.getRed()-level*30;
             int g = terrColor.getGreen()-level*30;
             int b = terrColor.getBlue()-level*30;
             if (r < 0) r = 0;
             if (g < 0) g = 0;
             if (b < 0) b = 0;
             return new Color(r, g, b);
        }
            return terrColor;
    }

    private Coords translateCoords(int x, int y) {
       int gridX = (int) (x / (hexSideBySin30 + hexSide));
       int restX = x % (hexSideBySin30 + hexSide);
       int gridY = (int) (y / (2*hexSideByCos30));
       int restY = y % (2*hexSideByCos30);
       restX = hexSideBySin30 + hexSide - restX;
       restY -= hexSideByCos30;
       if (hexSideBySin30*restX > hexSideByCos30*restY) gridX ++;
       if (-hexSideBySin30*restX > hexSideByCos30*restY) gridY --;
       if (gridY <0)gridY = 0;
       return new Coords(gridX, gridY);
    }

    private void processMouseClick(int x, int y){
        if (y > (getSize().height - 14)){

            if (minimized){
                m_dialog.setResizable(true);
                setSize(getSize().width, heightBufer);
            }else{
                heightBufer = getSize().height;
                setSize(getSize().width, 14);
                m_mapImage = createImage(Math.max(1, getSize().width), 14);
                m_dialog.setResizable(false);
            }
            minimized = ! minimized;
            m_dialog.pack();
            drawMap();
        }else{
            if ((x < leftMargin) || (x > (getSize().width -  leftMargin)) || (y < topMargin) || (y > (getSize().height - topMargin))){
                return;
            } else {
                m_bview.centerOnHex(translateCoords(x - leftMargin, y - topMargin));
            }
       }
    }

    // begin GameListener implementation
    public void gamePlayerChat(GameEvent e) {}
    public void gamePlayerStatusChange(GameEvent e) {}
    public void gameTurnChange(GameEvent e) {
        drawMap();
    }
    public void gamePhaseChange(GameEvent e) {
        drawMap();
    }
    public void gameNewEntities(GameEvent e) {
        //drawMap();
    }
    public void gameNewSettings(GameEvent e) {}
    // end GameListener implementation

    // begin BoardListener implementation
    public void boardHexMoused(BoardEvent b) {
    }
    public void boardHexCursor(BoardEvent b) {
    }
    public void boardHexHighlighted(BoardEvent b) {
    }
    public void boardHexSelected(BoardEvent b) {
    }
    public void boardNewBoard(BoardEvent b) {
        initializeMap();
    }

    public void boardChangedHex(BoardEvent b) {
        drawMap();
    }
    // end BoardListener implementation

    // begin MouseListener implementation
    public void mousePressed(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
        // center main map on clicked area
        processMouseClick(me.getX(), me.getY());
    }
    // end MouseListener implementation

    // begin ComponentListener implementation
    public void componentHidden(ComponentEvent ce) {
    }

    public void componentMoved(ComponentEvent ce) {
    }

    public void componentShown(ComponentEvent ce) {
    }

    public void componentResized(ComponentEvent ce) {
            if (!minimized) initializeMap();
    }


    // end ComponentListener implementation
}
