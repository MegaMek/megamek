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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;

import megamek.client.util.PlayerColors;
import megamek.common.*;
import megamek.common.actions.*;

/**
 * Displays all the mapsheets in a scaled-down size.
 */
public class MiniMap extends Canvas
    implements BoardListener, MouseListener, ComponentListener, GameListener {
    // these indices match those in Terrain.java, and are therefore sensitive to any changes there

    private final static Color[] m_terrainColors = new Color[Terrain.SIZE];
    private static Color HEAVY_WOODS;
    private static Color BACKGROUND;
    private static Color SINKHOLE;
    private static Color SMOKE_AND_FIRE;

    private final static int SHOW_NO_HEIGHT = 0;
    private final static int SHOW_GROUND_HEIGHT = 1;
    private final static int SHOW_BUILDING_HEIGHT = 2;
    private final static int SHOW_TOTAL_HEIGHT = 3;
    private final static int NBR_MODES = 3;

    private Image        m_mapImage;
    private BoardView1   m_bview;
    private Game         m_game;
    private Dialog       m_dialog;
    private static final int    margin = 6;
    private int          topMargin;
    private int          leftMargin;
    private static final int    buttonHeight = 14;
    private boolean      minimized = false;
    private int          heightBufer;
    private int          unitSize = 6;//variable which define size of triangle for unit representation
    private Vector       roadHexIndexes = new Vector();
    private int          zoom = GUIPreferences.getInstance().getMinimapZoom();
    private int[]        hexSide = {3,5,6,8,10,12};
    private int[]        hexSideByCos30 = {3,4,5,7,9,10};
    private int[]        hexSideBySin30 = {2,2,3,4,5,6};
    private int[]        halfRoadWidthByCos30 = {0,0,1,2,2,3};
    private int[]        halfRoadWidthBySin30 = {0,0,1,1,1,2};
    private int[]        halfRoadWidth        = {0,0,1,2,3,3};

    private int          heightDisplayMode = SHOW_NO_HEIGHT;
    Coords               firstLOS;
    Coords               secondLOS;

    private Client       m_client;

    /**
     * Creates and lays out a new mech display.
     */
    public MiniMap(Dialog d, Game g, BoardView1 bview) throws IOException {
        m_game = g;
        m_bview = bview;
        m_dialog = d;
        initializeColors();
        m_game.board.addBoardListener(this);
        addMouseListener(this);
        addComponentListener(this);
        m_dialog.addComponentListener(this);
        m_dialog.setResizable(false);

        // TODO: replace this quick-and-dirty with some real size calculator.
        Dimension size = getSize();
        boolean updateSize = false;
        if ( size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
            updateSize = true;
        }
        if ( size.height < GUIPreferences.getInstance().getMinimumSizeHeight() ) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
            updateSize = true;
        }
        if ( updateSize ) {
            setSize( size );
        }
        setLocation( GUIPreferences.getInstance().getMinimapPosX(), GUIPreferences.getInstance().getMinimapPosY() );

    }

    public MiniMap(Dialog d, ClientGUI c, BoardView1 bview) throws IOException {
        this (d, c.getClient().game, bview);

        c.getClient().addGameListener(this);
        c.minimapW.addKeyListener(c.menuBar);
        addKeyListener(c.menuBar);

        // this may come in useful later...
        m_client=c.getClient();
    }

    public synchronized void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        if (m_mapImage != null) {
            g.drawImage(m_mapImage, 0, 0, this);
        }
    }

    /*
     * Initialize default colours and override with config file if there is one.
     */
    private void initializeColors() throws IOException {

        // set up defaults -- this might go away later...
        BACKGROUND                        = Color.black;
        m_terrainColors[0]                = new Color(218,215,170);
        SINKHOLE                          = new Color(218,215,170);
        m_terrainColors[Terrain.WOODS]    = new Color(180,230,130);
        HEAVY_WOODS                       = new Color(160,200,100);
        m_terrainColors[Terrain.ROUGH]    = new Color(215,181,0);
        m_terrainColors[Terrain.RUBBLE]   = new Color(200,200,200);
        m_terrainColors[Terrain.WATER]    = new Color(200,247,253);
        m_terrainColors[Terrain.PAVEMENT] = new Color(204,204,204);
        m_terrainColors[Terrain.ROAD]     = new Color(71,79,107);
        m_terrainColors[Terrain.FIRE]     = Color.red;
        m_terrainColors[Terrain.SMOKE]    = new Color(204,204,204);
        SMOKE_AND_FIRE                    = new Color(153,0,0);
        m_terrainColors[Terrain.SWAMP]    = new Color(49,136,74);
        m_terrainColors[Terrain.BUILDING] = new Color(204,204,204);
        m_terrainColors[Terrain.BRIDGE]   = new Color(109,55,25);

        // now try to read in the config file
        int red;
        int green;
        int blue;

        File coloursFile = new File("data/hexes/" + GUIPreferences.getInstance().getMinimapColours()); //$NON-NLS-1$

        // only while the defaults are hard-coded!
        if(!coloursFile.exists()) { return; }

        Reader cr = new FileReader(coloursFile);
        StreamTokenizer st = new StreamTokenizer(cr);

        st.lowerCaseMode(true);
        st.quoteChar('"');
        st.commentChar('#');

        scan:
        while (true) {
            red=0;
            green=0;
            blue=0;

            switch(st.nextToken()) {
            case StreamTokenizer.TT_EOF:
                break scan;
            case StreamTokenizer.TT_EOL:
                break scan;
            case StreamTokenizer.TT_WORD:
                // read in
                String key = st.sval;
                if (key.equals("unitsize")) { //$NON-NLS-1$
                    st.nextToken();
                    unitSize = (int)st.nval;
                } else if (key.equals("background")) { //$NON-NLS-1$
                    st.nextToken();
                    red = (int)st.nval;
                    st.nextToken();
                    green = (int)st.nval;
                    st.nextToken();
                    blue = (int)st.nval;

                    BACKGROUND = new Color(red,green,blue);
                } else if (key.equals("heavywoods")) { //$NON-NLS-1$
                    st.nextToken();
                    red = (int)st.nval;
                    st.nextToken();
                    green = (int)st.nval;
                    st.nextToken();
                    blue = (int)st.nval;

                    HEAVY_WOODS = new Color(red,green,blue);
                } else if (key.equals("sinkhole")) { //$NON-NLS-1$
                    st.nextToken();
                    red = (int)st.nval;
                    st.nextToken();
                    green = (int)st.nval;
                    st.nextToken();
                    blue = (int)st.nval;

                    SINKHOLE = new Color(red,green,blue);
                } else if (key.equals("smokeandfire")) { //$NON-NLS-1$
                    st.nextToken();
                    red = (int)st.nval;
                    st.nextToken();
                    green = (int)st.nval;
                    st.nextToken();
                    blue = (int)st.nval;

                    SMOKE_AND_FIRE = new Color(red,green,blue);
                } else {
                    st.nextToken();
                    red = (int)st.nval;
                    st.nextToken();
                    green = (int)st.nval;
                    st.nextToken();
                    blue = (int)st.nval;

                    m_terrainColors[Terrain.parse(key)]=new Color(red,green,blue);
                }
            }
        }

        cr.close();
    }

    private void initializeMap() {

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
        requiredWidth = m_game.board.width*(currentHexSide + currentHexSideBySin30) + currentHexSideBySin30 + 2*margin;
        requiredHeight = (2*m_game.board.height + 1)*currentHexSideByCos30 + 2*margin + buttonHeight;
        setSize(requiredWidth, requiredHeight);
        m_dialog.pack();
        //m_dialog.show();
        m_mapImage = createImage(getSize().width,getSize().height);
        if (getSize().width > requiredWidth) leftMargin = (int) ((getSize().width - requiredWidth)/2) + margin;
        if (getSize().height > requiredHeight) topMargin = (int) ((getSize().height - requiredHeight)/2) + margin;
        drawMap();
    }

    // draw everything
    public synchronized void drawMap() {
        if (m_mapImage == null) {
            return;
        }

        if ( !m_dialog.isVisible() )
            return;

        Graphics g = m_mapImage.getGraphics();
        Color oldColor = g.getColor();
        g.setColor(BACKGROUND);
        g.fillRect(0,0,getSize().width,getSize().height);
        g.setColor(oldColor);
        if (!minimized) {
            roadHexIndexes.removeAllElements();
            for (int j = 0; j < m_game.board.width; j++) {
                for (int k = 0; k < m_game.board.height; k++) {
                    Hex h = m_game.board.getHex(j, k);
                    g.setColor(terrainColor(h, j, k));
                    paintCoord(g, j, k, true);
                }
            }

            if (firstLOS != null) paintSingleCoordBorder(g, firstLOS.x, firstLOS.y, Color.red);
            if (secondLOS != null) paintSingleCoordBorder(g, secondLOS.x, secondLOS.y, Color.red);

            if (! roadHexIndexes.isEmpty()) paintRoads(g);

            if (SHOW_NO_HEIGHT!=heightDisplayMode) {
                for (int j = 0; j < m_game.board.width; j++) {
                    for (int k = 0; k < m_game.board.height; k++) {
                        Hex h = m_game.board.getHex(j, k);
                        paintHeight(g, h, j , k);
                    }
                }
            }

            // draw Drop Zone
            if (null!=m_client && null!=m_game) {   // sanity check!
                if (Game.PHASE_DEPLOYMENT == m_game.getPhase()
                    && m_game.getTurn().getPlayerNum() == m_client.getLocalPlayer().getId()) {
                    for (int j = 0; j < m_game.board.width; j++) {
                        for (int k = 0; k < m_game.board.height; k++) {
                            if (m_game.board.isLegalDeployment(new Coords(j,k), m_client.getLocalPlayer())) {
                                paintSingleCoordBorder(g,j,k,Color.yellow);
                            }
                        }
                    }
                }
            }

            // draw declared fire
            if (Game.PHASE_FIRING==m_game.getPhase() || Game.PHASE_PHYSICAL==m_game.getPhase()) {
                for (Enumeration iter = m_game.getActions(); iter.hasMoreElements(); ) {
                    Object action = iter.nextElement();
                    if (action instanceof AttackAction) {
                        paintAttack(g,(AttackAction) action);
                    };
                };
            };

            for (Enumeration iter = m_game.getEntities(); iter.hasMoreElements(); ) {
                Entity e = (Entity)iter.nextElement();
                if (e.getPosition() == null) continue;
                paintUnit(g, e, true);
            }
        }

        drawBtn(g);

        repaint();
    }

    /**
     * Draws green Button in the bottom to close and open mini map. Height of button is fixed to 14pix.
     */
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


        //drawing "+" and "-" buttons
        if (! minimized){
            g.setColor(Color.black);
            g.drawLine(14 - 1,getSize().height - 14, 14 - 1,getSize().height);
            g.drawLine(getSize().width - 14 - 1,getSize().height - 14, getSize().width - 14 - 1,getSize().height);
            g.setColor(Color.green.darker());
            g.drawLine(14,getSize().height - 14, 14,getSize().height);
            g.drawLine(getSize().width - 14 ,getSize().height - 14, getSize().width - 14,getSize().height);
            if (zoom == 0){
                g.setColor(Color.gray.brighter());
            } else {
                g.setColor(Color.yellow);
            }
            g.fillRect(3,getSize().height - 14 + 6, 8, 2);
            if (zoom == (hexSide.length - 1)){
                g.setColor(Color.gray.brighter());
            } else {
                g.setColor(Color.yellow);
            }
            g.fillRect(getSize().width - 14 + 3, getSize().height - 14 + 6, 8, 2);
            g.fillRect(getSize().width - 14 + 6, getSize().height - 14 + 3, 2, 8);

            if (zoom > 2) {
                // Button for displying heights.
                g.setColor(Color.black);
                g.drawLine(28 - 1,getSize().height - 14, 28 - 1,getSize().height);
                g.setColor(Color.green.darker());
                g.drawLine(28, getSize().height - 14, 28, getSize().height);
                g.setColor(Color.yellow);
                String label;
                switch (heightDisplayMode) {
                case SHOW_NO_HEIGHT :
                    label = Messages.getString("MiniMap.NoHeightLabel"); //$NON-NLS-1$
                    break;
                case SHOW_GROUND_HEIGHT :
                    label = Messages.getString("MiniMap.GroundHeightLabel"); //$NON-NLS-1$
                    break;
                case SHOW_BUILDING_HEIGHT :
                    label = Messages.getString("MiniMap.BuildingHeightLabel"); //$NON-NLS-1$
                    break;
                case SHOW_TOTAL_HEIGHT :
                    label = Messages.getString("MiniMap.TotalHeightLabel"); //$NON-NLS-1$
                    break;
                default :
                    label = ""; //$NON-NLS-1$
                }
                g.drawString(label, 17, getSize().height - 14 + 12);
            }
        }

        g.setColor(oldColor);

    }

    private void paintHeight(Graphics g, Hex h, int x, int y) {
        if (heightDisplayMode == SHOW_NO_HEIGHT) return;
        if(zoom > 2){
            int baseX = x *(hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin;
            int baseY = (2*y + 1 + x%2)* hexSideByCos30[zoom] + topMargin;
            g.setColor(Color.white);
            int height = 0;
            if (h.getTerrain(Terrain.BUILDING) != null && heightDisplayMode == SHOW_BUILDING_HEIGHT) {
                height = h.ceiling();
            } else if (heightDisplayMode == SHOW_GROUND_HEIGHT) {
                height = h.floor();
            } else if (heightDisplayMode == SHOW_TOTAL_HEIGHT) {
                height = (h.getTerrain(Terrain.BUILDING) != null) ? h.ceiling() : h.floor();
            }
            if (height != 0) {
                g.drawString(height + "", baseX + 5, baseY + 5); //$NON-NLS-1$
            }
        }
    }

    private void paintSingleCoordBorder(Graphics g, int x, int y, Color c) {
        int baseX = x *(hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin;
        int baseY = (2*y + 1 + x%2)* hexSideByCos30[zoom] + topMargin;
        int [] xPoints = new int[6];
        int [] yPoints = new int[6];
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
        g.setColor(c);
        g.drawPolygon(xPoints,yPoints,6);
    }

    private void paintCoord(Graphics g, int x, int y, boolean border) {
        int baseX = x *(hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin;
        int baseY = (2*y + 1 + x%2)* hexSideByCos30[zoom] + topMargin;
        int [] xPoints = new int[6];
        int [] yPoints = new int[6];
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
        g.fillPolygon(xPoints,yPoints,6);
        if (border) {
            Color oldColor = g.getColor();
            g.setColor(oldColor.darker());
            g.drawPolygon(xPoints,yPoints,6);
            g.setColor(oldColor);
        }
    }

    /**
     * Draw a line to represent an attack
     */
    private void paintAttack(Graphics g, AttackAction attack) {
        Entity source = m_game.getEntity(attack.getEntityId());
        Targetable target = m_game.getTarget(attack.getTargetType(), attack.getTargetId());
        // sanity check...
        if (null==source || null==target) { return; };

        if (attack instanceof WeaponAttackAction) {
            WeaponAttackAction waa = (WeaponAttackAction)attack;
            if ( ((attack.getTargetType() == Targetable.TYPE_HEX_ARTILLERY 
                || attack.getTargetType() == Targetable.TYPE_HEX_FASCAM
                || attack.getTargetType() == Targetable.TYPE_HEX_INFERNO_IV
                || attack.getTargetType() == Targetable.TYPE_HEX_VIBRABOMB_IV)
                && waa.getEntity(m_game).getOwner().getId() != m_client.getLocalPlayer().getId())
                || attack.getTargetType() == Targetable.TYPE_INARC_POD) {
                return;
            }
        }
        Color oldColor = g.getColor();

        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        xPoints[0] = source.getPosition().x *(hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin + (int)1.5*hexSide[zoom] -2;
        yPoints[0] = (2*source.getPosition().y + 1 + source.getPosition().x%2)* hexSideByCos30[zoom] + topMargin;
        xPoints[1] = target.getPosition().x *(hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin + (int)1.5*hexSide[zoom] -2;
        yPoints[1] = (2*target.getPosition().y + 1 + target.getPosition().x%2)* hexSideByCos30[zoom] + topMargin;
        xPoints[2] = xPoints[1]+2;
        xPoints[3] = xPoints[0]+2;
        if ((source.getPosition().x > target.getPosition().x
             && source.getPosition().y < target.getPosition().y)
            || (source.getPosition().x < target.getPosition().x
                && source.getPosition().y > target.getPosition().y)) {
            yPoints[3] = yPoints[0]+2;
            yPoints[2] = yPoints[1]+2;
        } else {
            yPoints[3] = yPoints[0]-2;
            yPoints[2] = yPoints[1]-2;
        };
        g.setColor(PlayerColors.getColor(source.getOwner().getColorIndex()));
        g.fillPolygon(xPoints,yPoints,4);
        g.setColor(Color.black);
        g.drawPolygon(xPoints,yPoints,4);


        // if this is mutual fire, draw a half-and-half line
        for (Enumeration iter = m_game.getActions(); iter.hasMoreElements(); ) {
            Object action = iter.nextElement();
            if (action instanceof AttackAction) {
                AttackAction otherAttack = (AttackAction) action;
                if (attack.getEntityId() == otherAttack.getTargetId()
                    && otherAttack.getEntityId() == attack.getTargetId() ) {
                    // attackTarget _must_ be an entity since it's shooting back (?)
                    Entity attackTarget = m_game.getEntity(otherAttack.getEntityId());
                    g.setColor(PlayerColors.getColor(attackTarget.getOwner().getColorIndex()));

                    xPoints[0] = xPoints[3];
                    yPoints[0] = yPoints[3];
                    xPoints[1] = xPoints[2];
                    yPoints[1] = yPoints[2];
                    xPoints[2] = xPoints[1]+2;
                    xPoints[3] = xPoints[0]+2;
                    if ((source.getPosition().x > target.getPosition().x
                         && source.getPosition().y < target.getPosition().y)
                        || (source.getPosition().x < target.getPosition().x
                            && source.getPosition().y > target.getPosition().y)) {
                        yPoints[3] = yPoints[0]+2;
                        yPoints[2] = yPoints[1]+2;
                    } else {
                        yPoints[3] = yPoints[0]-2;
                        yPoints[2] = yPoints[1]-2;
                    };
                    g.fillPolygon(xPoints,yPoints,4);
                    g.setColor(Color.black);
                    g.drawPolygon(xPoints,yPoints,4);
                    break;
                };
            };
        };

        g.setColor(oldColor);
    };

    private void paintUnit (Graphics g, Entity entity, boolean border) {
        int baseX = entity.getPosition().x *(hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin + hexSide[zoom];
        int baseY = (2*entity.getPosition().y + 1 + entity.getPosition().x%2)* hexSideByCos30[zoom] + topMargin;
        int [] xPoints;
        int [] yPoints;

        if (entity instanceof Mech) {
            xPoints = new int[3];
            yPoints = new int[3];
            xPoints[0] = baseX;
            yPoints[0] = baseY - unitSize;
            xPoints[1] = baseX - unitSize;
            yPoints[1] = baseY + unitSize / 2;
            xPoints[2] = baseX + unitSize;
            yPoints[2] = baseY + unitSize / 2;
        }
        else if (entity instanceof Tank) {
            xPoints = new int[4];
            yPoints = new int[4];
            xPoints[0] = baseX - unitSize * 2 / 3;
            yPoints[0] = baseY - unitSize * 2 / 3;
            xPoints[1] = baseX - unitSize * 2 / 3;
            yPoints[1] = baseY + unitSize * 2 / 3;
            xPoints[2] = baseX + unitSize * 2 / 3;
            yPoints[2] = baseY + unitSize * 2 / 3;
            xPoints[3] = baseX + unitSize * 2 / 3;
            yPoints[3] = baseY - unitSize * 2 / 3;
        }
        else if (entity instanceof Protomech) {
            xPoints = new int[3];
            yPoints = new int[3];
            xPoints[0] = baseX;
            yPoints[0] = baseY + unitSize;
            xPoints[1] = baseX + unitSize;
            yPoints[1] = baseY - unitSize / 2;
            xPoints[2] = baseX - unitSize;
            yPoints[2] = baseY - unitSize / 2;
        }
        // entity instanceof Infantry
        else {
            xPoints = new int[4];
            yPoints = new int[4];
            xPoints[0] = baseX;
            yPoints[0] = baseY - unitSize;
            xPoints[1] = baseX - unitSize;
            yPoints[1] = baseY;
            xPoints[2] = baseX;
            yPoints[2] = baseY + unitSize;
            xPoints[3] = baseX + unitSize;
            yPoints[3] = baseY;
        }

        g.setColor (PlayerColors.getColor(entity.getOwner().getColorIndex()));
        if (! entity.isSelectableThisTurn()) {
            // entity has moved (or whatever) already
            g.setColor (g.getColor().darker());
        }
        g.fillPolygon (xPoints, yPoints, xPoints.length);

        if (entity.isSelected()) {
            Color w = new Color (255,255,255);
            Color b = new Color (0,0,0);
            g.setColor (b);
            g.drawRect (baseX-1, baseY-1, 3, 3);
            g.setColor (w);
            g.drawRect (baseX, baseY, 1, 1);
        }
        if (border) {
            Color oldColor = g.getColor();
            g.setColor (oldColor.darker().darker().darker());
            g.drawPolygon (xPoints, yPoints, xPoints.length);
            g.setColor (oldColor);
        }
    }

    private void paintRoads (Graphics g){
        int exits = 0;
        int baseX, baseY, x, y;
        int [] xPoints = new int[4];
        int [] yPoints = new int[4];
        Color oldColor = g.getColor();
        g.setColor(m_terrainColors[Terrain.ROAD]);
        for (Enumeration iter = roadHexIndexes.elements(); iter.hasMoreElements(); ){
            int[] hex = (int[])iter.nextElement();
            x = hex[0];
            y = hex[1];
            baseX = x *(hexSide[zoom] + hexSideBySin30[zoom]) + leftMargin + hexSide[zoom];
            baseY = (2*y + 1 + x%2)* hexSideByCos30[zoom] + topMargin;
            exits = hex[2];
            // Is there a North exit?
            if ( 0 != (exits & 0x0001) ) {
                xPoints[0] = baseX - halfRoadWidth[zoom] ;
                yPoints[0] = baseY;
                xPoints[1] = baseX - halfRoadWidth[zoom];
                yPoints[1] = baseY - hexSideByCos30[zoom];
                xPoints[2] = baseX + halfRoadWidth[zoom];
                yPoints[2] = baseY - hexSideByCos30[zoom];
                xPoints[3] = baseX + halfRoadWidth[zoom];
                yPoints[3] = baseY;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a North-East exit?
            if ( 0 != (exits & 0x0002) ) {
                xPoints[0] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY - halfRoadWidthByCos30[zoom];
                xPoints[1] = (int) Math.round(baseX + 3*hexSide[zoom]/4 - halfRoadWidthBySin30[zoom]);
                yPoints[1] = (int) Math.round(baseY - hexSideByCos30[zoom]/2 - halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] + 2 * halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] + 2 * halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY + halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a South-East exit?
            if ( 0 != (exits & 0x0004) ) {
                xPoints[0] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY - halfRoadWidthByCos30[zoom];
                xPoints[1] = (int) Math.round(baseX + 3*hexSide[zoom]/4 + halfRoadWidthBySin30[zoom]);
                yPoints[1] = (int) Math.round(baseY + hexSideByCos30[zoom]/2 - halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] - 2 * halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] + 2 * halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY + halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a South exit?
            if ( 0 != (exits & 0x0008) ) {
                xPoints[0] = baseX + halfRoadWidth[zoom];
                yPoints[0] = baseY;
                xPoints[1] = baseX + halfRoadWidth[zoom];
                yPoints[1] = baseY + hexSideByCos30[zoom];
                xPoints[2] = baseX - halfRoadWidth[zoom];
                yPoints[2] = baseY + hexSideByCos30[zoom];
                xPoints[3] = baseX - halfRoadWidth[zoom];
                yPoints[3] = baseY;
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a South-West exit?
            if ( 0 != (exits & 0x0010) ) {
                xPoints[0] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY + halfRoadWidthByCos30[zoom];
                xPoints[1] = (int) Math.round(baseX - 3*hexSide[zoom]/4 + halfRoadWidthBySin30[zoom]);
                yPoints[1] = (int) Math.round(baseY + hexSideByCos30[zoom]/2 + halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] - 2*halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] - 2*halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY - halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }
            // Is there a North-West exit?
            if ( 0 != (exits & 0x0020) ) {
                xPoints[0] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY + halfRoadWidthByCos30[zoom];
                xPoints[1] = (int) Math.round(baseX - 3*hexSide[zoom]/4 - halfRoadWidthBySin30[zoom]);
                yPoints[1] = (int) Math.round(baseY - hexSideByCos30[zoom]/2 + halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] + 2*halfRoadWidthBySin30[zoom];
                yPoints[2] = yPoints[1] - 2*halfRoadWidthByCos30[zoom];
                xPoints[3] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY - halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints,yPoints,4);
                g.fillPolygon(xPoints,yPoints,4);
            }

        }
        g.setColor(oldColor);
    }


    private Color terrainColor(Hex x, int boardX, int boardY) {
        Color terrColor = m_terrainColors[0];
        if (x.getElevation() < 0) {
            // sinkholes have their own colour
            terrColor = SINKHOLE;
        };

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
                // make heavy woods darker
                if (j == Terrain.WOODS && x.getTerrain(j).getLevel() > 1) {
                    terrColor = HEAVY_WOODS;
                };
                // contains both smoke and fire
                if (j == Terrain.SMOKE && x.getTerrain(Terrain.FIRE) != null) {
                    terrColor = SMOKE_AND_FIRE;
                }
                break;
            }
        }

        int r, g, b;
        switch (terrain) {
        case 0 :
        case Terrain.WOODS :
        case Terrain.ROUGH :
        case Terrain.RUBBLE :
        case Terrain.WATER :
        case Terrain.PAVEMENT :
            level = (int) Math.abs(x.floor());
            // By experiment it is possible to make only 6 distinctive color steps
            if (level > 10) level = 10;
            r = terrColor.getRed()-level*15;
            g = terrColor.getGreen()-level*15;
            b = terrColor.getBlue()-level*15;
            if (r < 0) r = 0;
            if (g < 0) g = 0;
            if (b < 0) b = 0;
            return new Color(r, g, b);

        case Terrain.BUILDING :
            level = (int) Math.abs(x.ceiling());
            // By experiment it is possible to make only 6 distinctive color steps
            if (level > 10) level = 10;
            r = terrColor.getRed()-level*15;
            g = terrColor.getGreen()-level*15;
            b = terrColor.getBlue()-level*15;
            if (r < 0) r = 0;
            if (g < 0) g = 0;
            if (b < 0) b = 0;
            return new Color(r, g, b);

        }
        /*
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
        */
        return terrColor;
    }

    private Coords translateCoords(int x, int y) {
        int gridX = (int) (x / (hexSideBySin30[zoom] + hexSide[zoom]));
        int restX = x % (hexSideBySin30[zoom] + hexSide[zoom]);
        int gridY = (int) (y / (2 * hexSideByCos30[zoom]));
        int restY = y % (2 * hexSideByCos30[zoom]);

        boolean evenColumn = (gridX & 1) == 0;

        if (restY < hexSideByCos30[zoom]) {
            if (evenColumn) {
                if (restX < ((restY - hexSideByCos30[zoom]) * hexSideBySin30[zoom] / hexSideByCos30[zoom] * -1)) {
                    gridX--;
                    gridY--;
                }
            } else {
                if (restX < (restY * hexSideBySin30[zoom] / hexSideByCos30[zoom])) {
                    gridX--;
                } else {
                    gridY--;
                }
            }
        } else {
            if (evenColumn) {
                if (restX < ((restY- hexSideByCos30[zoom]) * hexSideBySin30[zoom] / hexSideByCos30[zoom])) {
                    gridX--;
                }
            } else {
                if (restX < ((restY - 2 * hexSideByCos30[zoom]) * hexSideBySin30[zoom] / hexSideByCos30[zoom] * -1)) {
                    gridX--;
                }
            }
        }
        /*       restX = hexSideBySin30[zoom] + hexSide[zoom] - restX;
                 restY -= hexSideByCos30[zoom];
                 if (hexSideBySin30[zoom]*restX > hexSideByCos30[zoom]*restY) gridX ++;
                 if (-hexSideBySin30[zoom]*restX > hexSideByCos30[zoom]*restY) gridY --;
        */
        if (gridX < 0) gridX = 0;
        if (gridY < 0) gridY = 0;

        return new Coords(gridX, gridY);
    }

    private void processMouseClick(int x, int y, MouseEvent me){
        if (y > (getSize().height - 14)){

            if(x < 14){
                if (zoom == 0) return;
                zoom --;
                initializeMap();
            }else if (x < 28 && zoom > 2) {
                heightDisplayMode = ((++heightDisplayMode) > NBR_MODES) ? 0 : heightDisplayMode;
                initializeMap();
            }else if ( x> (getSize().width - 14)){
                if (zoom == (hexSide.length - 1)) return;
                zoom ++;
                initializeMap();
            } else{
                if (minimized){
                    //m_dialog.setResizable(true);
                    setSize(getSize().width, heightBufer);
                    m_mapImage = createImage(getSize().width, heightBufer);
                }else{
                    heightBufer = getSize().height;
                    setSize(getSize().width, 14);
                    m_mapImage = createImage(Math.max(1, getSize().width), 14);
                    //m_dialog.setResizable(false);
                }
                minimized = ! minimized;
                m_dialog.pack();
                drawMap();
            }
        }else{
            if ((x < margin) || (x > (getSize().width -  leftMargin)) || (y < topMargin) || (y > (getSize().height - topMargin - 14))){
                return;
            } else {
                if ((me.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                    //              m_bview.checkLOS(translateCoords(x - leftMargin, y - topMargin));
                    m_game.board.mouseAction(translateCoords(x - leftMargin, y - topMargin), Board.BOARD_HEX_CLICK, me.getModifiers());
                } else {
                    m_bview.centerOnHex(translateCoords(x - leftMargin, y - topMargin));
                }
            }
        }
    }

    public int getZoom() {
        return zoom;
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
        firstLOS = null;
        secondLOS = null;
        drawMap();
    }
    public void boardHexHighlighted(BoardEvent b) {
        firstLOS = null;
        secondLOS = null;
        drawMap();
    }
    public void boardHexSelected(BoardEvent b) {
        firstLOS = null;
        secondLOS = null;
        drawMap();
    }
    public void boardNewBoard(BoardEvent b) {
        initializeMap();
    }
    public void boardFirstLOSHex(BoardEvent b) {
        secondLOS = null;
        firstLOS = b.getCoords();
        drawMap();
    }
    public void boardSecondLOSHex(BoardEvent b, Coords c) {
        firstLOS = c;
        secondLOS = b.getCoords();
        drawMap();
    }

    public void boardChangedHex(BoardEvent b) {
        // Calling drawMap here turns out to be a bad idea since this
        //  method gets called about 7 times for each hex that is changed.
        //  Resolving fire/smoke updates, for example, was taking four times
        //  as long with this call enabled.  We'll just leave the drawing
        //  to the change turn/phase methods above.
        //        drawMap();
    }
    // end BoardListener implementation

    // begin MouseListener implementation
    public void mousePressed(MouseEvent me) {
        // center main map on clicked area
        processMouseClick(me.getX(), me.getY(), me);
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
    }
    // end MouseListener implementation

    // begin ComponentListener implementation
    public void componentHidden(ComponentEvent ce) {
    }

    public void componentMoved(ComponentEvent ce) {
    }

    public void componentShown(ComponentEvent ce) {
        drawMap();
    }

    public void componentResized(ComponentEvent ce) {
        // if (!minimized) initializeMap();
    }


    // end ComponentListener implementation
    public void gameBoardChanged(GameEvent e) {
        ;
    }

    public void gameDisconnected(GameEvent e) {
        ;
    }

    public void boardChangedEntity(BoardEvent b) {
        ;
    }

    public void boardNewAttack(BoardEvent a) {
        ;
    }

    public void gameEnd(GameEvent e) {
        ;
    }

    public void gameReport(GameEvent e) {
        ;
    }

    public void gameMapQuery(GameEvent e) {
        ;
    }

}
