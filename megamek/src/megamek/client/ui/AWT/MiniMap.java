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
    private int m_nPixWidth = 2;
    private int m_nPixHeight = 2;
    
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
        m_terrainColors[0] = Color.orange.darker();
        m_terrainColors[Terrain.WOODS] = Color.green;
        m_terrainColors[Terrain.ROUGH] = Color.yellow;
        m_terrainColors[Terrain.RUBBLE] = Color.orange.darker().darker();
        m_terrainColors[Terrain.WATER] = Color.blue;
        m_terrainColors[Terrain.PAVEMENT] = Color.gray;
        m_terrainColors[Terrain.ROAD] = Color.orange;
        m_terrainColors[Terrain.FIRE] = Color.red;
        m_terrainColors[Terrain.SMOKE] = Color.darkGray;
        m_terrainColors[Terrain.SWAMP] = Color.green.darker();
        m_terrainColors[Terrain.BUILDING] = Color.white;
        m_terrainColors[Terrain.BRIDGE] = Color.darkGray;
        
    }
    
    private void initializeMap() {
        m_dialog.show();
        m_nPixWidth = getSize().width / m_game.board.width;
        m_nPixHeight = getSize().height / m_game.board.height;
        m_mapImage = createImage(getSize().width, getSize().height);
        drawMap();
    }
    
    // draw everything
    private void drawMap() {
        if (m_mapImage == null) {
            return;
        }
        Graphics g = m_mapImage.getGraphics();
        for (int j = 0; j < m_game.board.width; j++) {
            for (int k = 0; k < m_game.board.height; k++) {
                // eventually this will color by terrain, otherwise we'd obviously do it as
                // one big rectangle
                g.setColor(terrainColor(m_game.board.getHex(j, k)));
                paintCoord(g, j, k, false);
            }
        }
        
        for (Enumeration enum = m_game.getEntities(); enum.hasMoreElements(); ) {
            Entity e = (Entity)enum.nextElement();
            g.setColor(e.getOwner().getColor());
            paintCoord(g, e.getPosition().x, e.getPosition().y, true);
        }
        repaint();
    }
    
    private void paintCoord(Graphics g, int x, int y, boolean border) {
        int pixX = x * m_nPixWidth;
        int pixY = y * m_nPixHeight;
        if (x % 2 == 1) {
            pixY = pixY + (m_nPixHeight / 2);
        }
        
        g.fillRect(pixX, pixY, m_nPixWidth, m_nPixHeight);
        if (border) {
            Color oldColor = g.getColor();
            g.setColor(Color.white);
            g.drawRect(pixX, pixY, m_nPixWidth, m_nPixHeight);
            g.setColor(oldColor);
        }
    }
    
    private Color terrainColor(Hex x) {
        for (int j = m_terrainColors.length - 1; j >= 0; j--) {
            if (x.getTerrain(j) != null && m_terrainColors[j] != null) {
                return m_terrainColors[j];
            }
        }
        return m_terrainColors[0];
    }
    
    private Coords translateCoords(int x, int y) {
        int newX = x / m_nPixWidth;
        int newY = y / m_nPixHeight;
        if (newX >= m_game.board.width) {
            newX = m_game.board.width - 1;
        }
        if (newY >= m_game.board.height) {
            newY = m_game.board.height - 1;
        }
        return new Coords(newX, newY);
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
        m_bview.centerOnHex(translateCoords(me.getX(), me.getY()));
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
        initializeMap();
    }
    // end ComponentListener implementation
}
