/*
 * MegaMek - Copyright (C) 2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

package megamek.client.ui.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.IBoardView;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Aero;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.GameTurn;
import megamek.common.GunEmplacement;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.VTOL;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.BoardEvent;
import megamek.common.event.BoardListener;
import megamek.common.event.BoardListenerAdapter;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;

/**
 * Displays all the mapsheets in a scaled-down size. TBD refactorings: -make a
 * real public API for this with interfaces -decouple rest of the application
 * -use JPanel instead of canvas -move the buttons from graphics to real Swing
 * buttons -clean up listenercode.. -initializecolors is fugly -uses exception
 * to return from method? -uses break-to-label -uses while-true
 */
public class MiniMap extends JPanel {

    // these indices match those in Terrains.java, and are therefore sensitive
    // to any changes there

    /**
     *
     */
    private static final long serialVersionUID = 6964529682842424060L;
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

    private final static int SCROLL_PANE_WIDTH = 160;
    private final static int SCROLL_PANE_HEIGHT = 200;

    private Image m_mapImage;
    private IBoardView m_bview;
    private IGame m_game;
    private IBoard m_board;
    private Container m_dialog;
    private static final int margin = 6;
    private int topMargin;
    private int leftMargin;
    //This value is variable.
    //if the container m_dialog is an instance of JDialog, it is 14. otherwise 0.
    private int buttonHeight = 0;
    private boolean minimized = false;
    private int heightBufer;
    private int unitSize = 6;// variable which define size of triangle for
    // unit representation
    private Vector<int[]> roadHexIndexes = new Vector<int[]>();
    private int zoom = GUIPreferences.getInstance().getMinimapZoom();
    private int[] fontSize = {6, 8, 10, 12, 14, 16};
    private int[] hexSide = {3, 5, 6, 8, 10, 12};
    private int[] hexSideByCos30 = {3, 4, 5, 7, 9, 10};
    private int[] hexSideBySin30 = {2, 2, 3, 4, 5, 6};
    private int[] halfRoadWidthByCos30 = {0, 0, 1, 2, 2, 3};
    private int[] halfRoadWidthBySin30 = {0, 0, 1, 1, 1, 2};
    private int[] halfRoadWidth = {0, 0, 1, 2, 3, 3};
    private int[] unitSizes = {5, 6, 7, 8, 9, 10};
    private int[] stratZoom = {8, 9, 11, 12, 14, 16};
    private int[] unitBorder = {1, 1, 1, 2, 2, 2};

    private int heightDisplayMode = SHOW_NO_HEIGHT;
    Coords firstLOS;
    Coords secondLOS;

    private Client m_client;

    private ClientGUI clientgui = null;

    boolean dirtyMap = true;
    boolean[][] dirty;
    private Image terrainBuffer;
    
    // Here come the Strat Ops / NATO unit symbols
    Map<Coords, Integer> multiUnits = new HashMap<Coords, Integer>();
    private final static Path2D STRAT_BASERECT;
    private final static Path2D STRAT_INFANTRY;
    private final static Path2D STRAT_MECH;
    private final static Path2D STRAT_VTOL;
    private final static Path2D STRAT_TANKTRACKED;
    private final static Path2D STRAT_AERO;
    private final static Path2D STRAT_SPHEROID;
    private final static Path2D STRAT_HOVER;
    private final static Path2D STRAT_WHEELED;
    private final static Path2D STRAT_NAVAL;
    private final static Dimension SYMBOLSIZE = new Dimension(167,103);
    private final static String[] STRAT_WEIGHTS = { "L", "L", "M", "H", "A", "A" };
    private final static double STRAT_CX = SYMBOLSIZE.getWidth()/5; // X center for two symbols
    
    static {
        // Base rectangle for all units
        STRAT_BASERECT = new Path2D.Double();
        STRAT_BASERECT.moveTo(-SYMBOLSIZE.getWidth()/2, -SYMBOLSIZE.getHeight()/2);
        STRAT_BASERECT.lineTo( SYMBOLSIZE.getWidth()/2, -SYMBOLSIZE.getHeight()/2);
        STRAT_BASERECT.lineTo( SYMBOLSIZE.getWidth()/2,  SYMBOLSIZE.getHeight()/2);
        STRAT_BASERECT.lineTo(-SYMBOLSIZE.getWidth()/2,  SYMBOLSIZE.getHeight()/2);
        STRAT_BASERECT.closePath();
        
        // Infantry Symbol
        STRAT_INFANTRY = new Path2D.Double();
        STRAT_INFANTRY.append(STRAT_BASERECT, false);
        STRAT_INFANTRY.moveTo(-SYMBOLSIZE.getWidth()/2, -SYMBOLSIZE.getHeight()/2);
        STRAT_INFANTRY.lineTo( SYMBOLSIZE.getWidth()/2,  SYMBOLSIZE.getHeight()/2);
        STRAT_INFANTRY.moveTo(-SYMBOLSIZE.getWidth()/2,  SYMBOLSIZE.getHeight()/2);
        STRAT_INFANTRY.lineTo( SYMBOLSIZE.getWidth()/2, -SYMBOLSIZE.getHeight()/2);
        
        STRAT_VTOL = new Path2D.Double();
        STRAT_VTOL.append(STRAT_BASERECT, false);
        STRAT_VTOL.moveTo(-SYMBOLSIZE.getWidth()/4, -SYMBOLSIZE.getHeight()/4);
        STRAT_VTOL.lineTo(-SYMBOLSIZE.getWidth()/4,  SYMBOLSIZE.getHeight()/4);
        STRAT_VTOL.lineTo( 0,  0);
        STRAT_VTOL.lineTo(-SYMBOLSIZE.getWidth()/4, -SYMBOLSIZE.getHeight()/4);
        
        STRAT_VTOL.moveTo( SYMBOLSIZE.getWidth()/4,  SYMBOLSIZE.getHeight()/4);
        STRAT_VTOL.lineTo( SYMBOLSIZE.getWidth()/4, -SYMBOLSIZE.getHeight()/4);
        STRAT_VTOL.lineTo( 0, 0);
        STRAT_VTOL.closePath();
        
        STRAT_TANKTRACKED = new Path2D.Double();
        STRAT_TANKTRACKED.append(STRAT_BASERECT, false);
        double small = SYMBOLSIZE.getWidth()/20; 
        STRAT_TANKTRACKED.moveTo(-SYMBOLSIZE.getWidth()/3+small, -SYMBOLSIZE.getHeight()/4);
        STRAT_TANKTRACKED.lineTo( SYMBOLSIZE.getWidth()/3-small, -SYMBOLSIZE.getHeight()/4);
        STRAT_TANKTRACKED.lineTo( SYMBOLSIZE.getWidth()/3,       -SYMBOLSIZE.getHeight()/4+small);
        STRAT_TANKTRACKED.lineTo( SYMBOLSIZE.getWidth()/3,        SYMBOLSIZE.getHeight()/4-small);
        STRAT_TANKTRACKED.lineTo( SYMBOLSIZE.getWidth()/3-small,  SYMBOLSIZE.getHeight()/4);
        STRAT_TANKTRACKED.lineTo(-SYMBOLSIZE.getWidth()/3+small,  SYMBOLSIZE.getHeight()/4);
        STRAT_TANKTRACKED.lineTo(-SYMBOLSIZE.getWidth()/3,        SYMBOLSIZE.getHeight()/4-small);
        STRAT_TANKTRACKED.lineTo(-SYMBOLSIZE.getWidth()/3,       -SYMBOLSIZE.getHeight()/4+small);
        STRAT_TANKTRACKED.closePath();
        
        STRAT_MECH = new Path2D.Double();
        STRAT_MECH.append(STRAT_BASERECT, false);
        
        STRAT_MECH.moveTo(-STRAT_CX-1.5*small, -SYMBOLSIZE.getHeight()/4);
        STRAT_MECH.lineTo(-STRAT_CX-3.0*small,  SYMBOLSIZE.getHeight()/4);
        STRAT_MECH.lineTo(-STRAT_CX+3.0*small,  SYMBOLSIZE.getHeight()/4);
        STRAT_MECH.lineTo(-STRAT_CX+1.5*small, -SYMBOLSIZE.getHeight()/4);
        STRAT_MECH.closePath();
        
        STRAT_AERO = new Path2D.Double();
        STRAT_AERO.append(STRAT_BASERECT, false);
        double rad = SYMBOLSIZE.getWidth()/5;
        double r72 = Math.toRadians(72);
        STRAT_AERO.moveTo(-STRAT_CX+rad/3*Math.cos(Math.PI/2+2*r72), rad/3*Math.sin(Math.PI/2+2*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad*Math.cos(Math.PI/2+1*r72), -rad*Math.sin(Math.PI/2+1*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad/3*Math.cos(Math.PI/2+1*r72), rad/3*Math.sin(Math.PI/2+1*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad*Math.cos(Math.PI/2+2*r72), -rad*Math.sin(Math.PI/2+2*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad/3*Math.cos(Math.PI/2), rad/3*Math.sin(Math.PI/2));
        STRAT_AERO.lineTo(-STRAT_CX+rad*Math.cos(Math.PI/2+3*r72), -rad*Math.sin(Math.PI/2+3*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad/3*Math.cos(Math.PI/2+4*r72), rad/3*Math.sin(Math.PI/2+4*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad*Math.cos(Math.PI/2+4*r72), -rad*Math.sin(Math.PI/2+4*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad/3*Math.cos(Math.PI/2+3*r72), rad/3*Math.sin(Math.PI/2+3*r72));
        STRAT_AERO.lineTo(-STRAT_CX+rad*Math.cos(Math.PI/2),       -rad*Math.sin(Math.PI/2));
        STRAT_AERO.closePath();
        
        STRAT_SPHEROID = new Path2D.Double();
        STRAT_SPHEROID.append(STRAT_BASERECT, false);
        STRAT_SPHEROID.moveTo(rad*Math.cos(Math.PI/2),       -rad*Math.sin(Math.PI/2));
        STRAT_SPHEROID.lineTo(rad*Math.cos(Math.PI/2+r72),   -rad*Math.sin(Math.PI/2+r72));
        STRAT_SPHEROID.lineTo(rad*Math.cos(Math.PI/2+2*r72), -rad*Math.sin(Math.PI/2+2*r72));
        STRAT_SPHEROID.lineTo(rad*Math.cos(Math.PI/2+3*r72), -rad*Math.sin(Math.PI/2+3*r72));
        STRAT_SPHEROID.lineTo(rad*Math.cos(Math.PI/2+4*r72), -rad*Math.sin(Math.PI/2+4*r72));
        STRAT_SPHEROID.closePath();
        
        STRAT_HOVER = new Path2D.Double();
        STRAT_HOVER.append(STRAT_BASERECT, false);
        STRAT_HOVER.moveTo(-SYMBOLSIZE.getWidth()/3,  small);
        STRAT_HOVER.lineTo(-SYMBOLSIZE.getWidth()/3, -small);
        STRAT_HOVER.lineTo( SYMBOLSIZE.getWidth()/3, -small);
        STRAT_HOVER.lineTo( SYMBOLSIZE.getWidth()/3,  small);

        STRAT_HOVER.moveTo(-SYMBOLSIZE.getWidth()/6, -small);
        STRAT_HOVER.lineTo(-SYMBOLSIZE.getWidth()/6, +small);
        STRAT_HOVER.moveTo(0, -small);
        STRAT_HOVER.lineTo(0, +small);
        STRAT_HOVER.moveTo( SYMBOLSIZE.getWidth()/6, -small);
        STRAT_HOVER.lineTo( SYMBOLSIZE.getWidth()/6, +small);
        
        STRAT_WHEELED = new Path2D.Double();
        STRAT_WHEELED.append(STRAT_BASERECT, false);
        double smallr = SYMBOLSIZE.getWidth()/17;
        STRAT_WHEELED.moveTo(-STRAT_CX-smallr*2, -smallr);
        STRAT_WHEELED.lineTo(+STRAT_CX+smallr*2, -smallr);
        STRAT_WHEELED.moveTo(-STRAT_CX, -smallr);
        STRAT_WHEELED.lineTo(-STRAT_CX-smallr, 0);
        STRAT_WHEELED.lineTo(-STRAT_CX, +smallr);
        STRAT_WHEELED.lineTo(-STRAT_CX+smallr, 0);
        STRAT_WHEELED.closePath();
        STRAT_WHEELED.moveTo( STRAT_CX, -smallr);
        STRAT_WHEELED.lineTo( STRAT_CX-smallr, 0);
        STRAT_WHEELED.lineTo( STRAT_CX, +smallr);
        STRAT_WHEELED.lineTo( STRAT_CX+smallr, 0);
        STRAT_WHEELED.closePath();
        STRAT_WHEELED.moveTo(0, -smallr);
        STRAT_WHEELED.lineTo(-smallr, 0);
        STRAT_WHEELED.lineTo(0, +smallr);
        STRAT_WHEELED.lineTo(smallr, 0);
        STRAT_WHEELED.closePath();
        
        STRAT_NAVAL = new Path2D.Double();
        STRAT_NAVAL.append(STRAT_BASERECT, false);
        STRAT_NAVAL.moveTo(0, -SYMBOLSIZE.getHeight()/3);
        STRAT_NAVAL.lineTo(0,  SYMBOLSIZE.getHeight()/3);
        STRAT_NAVAL.moveTo(-STRAT_CX/2, -SYMBOLSIZE.getHeight()/5);
        STRAT_NAVAL.lineTo( STRAT_CX/2, -SYMBOLSIZE.getHeight()/5);
        
        STRAT_NAVAL.moveTo(rad, 0);
        STRAT_NAVAL.curveTo(
                rad*0.8, SYMBOLSIZE.getHeight()/3*0.8,
                rad*0.8, SYMBOLSIZE.getHeight()/3*0.8,
                0, SYMBOLSIZE.getHeight()/3);
        STRAT_NAVAL.curveTo(
                -rad*0.8, SYMBOLSIZE.getHeight()/3*0.8,
                -rad*0.8, SYMBOLSIZE.getHeight()/3*0.8,
                -rad, 0);
    }
    
    // are we drag-scrolling?
    private boolean dragging = false;

    /**
     * Creates and lays out a new mech display.
     */
    public MiniMap(Container d, IGame g, IBoardView bview) throws IOException {
        m_game = g;
        m_bview = bview;
        m_dialog = d;
        m_board = m_game.getBoard();
        m_bview.addBoardViewListener(boardViewListener);
        m_game.addGameListener(gameListener);
        m_board.addBoardListener(boardListener);
        initialize();
    }

    public MiniMap(Container d, IBoard b) throws IOException {
        m_dialog = d;
        m_board = b;
        initialize();
    }

    public MiniMap(Container d, ClientGUI c, IBoardView bview) throws IOException {
        this(d, c.getClient().getGame(), bview);
        clientgui = c;

        // this may come in useful later...
        m_client = c.getClient();
        assert (m_client != null);
    }

    public void setBoard(IBoard board) {
        m_board = board;
        initializeMap();
    }

    private void initialize() throws IOException {
        initializeColors();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseMotionListener);
        addMouseWheelListener(mouseWheelListener);
        addComponentListener(componentListener);
        m_dialog.addComponentListener(componentListener);
        if (m_dialog instanceof JDialog) {
            buttonHeight = 14;
            ((JDialog) m_dialog).setResizable(false);

            // TODO: replace this quick-and-dirty with some real size
            // calculator.
            Dimension size = getSize();
            boolean updateSize = false;
            if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
                size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
                updateSize = true;
            }
            if (size.height < GUIPreferences.getInstance()
                                            .getMinimumSizeHeight()) {
                size.height = GUIPreferences.getInstance()
                                            .getMinimumSizeHeight();
                updateSize = true;
            }
            if (updateSize) {
                setSize(size);
            }
            setLocation(GUIPreferences.getInstance().getMinimapPosX(),
                        GUIPreferences.getInstance().getMinimapPosY());

            ((JDialog) m_dialog).pack();
        }
    }

    @Override
    public synchronized void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        if (m_mapImage != null) {
            g.drawImage(m_mapImage, 0, 0, this);
            // drawBox(g); this would be a nice place to draw a visible-area box
            paintBVSection(g); // Happy to oblige
        }
    }

    /**
     * Initialize default colours and override with config file if there is one.
     */
    private void initializeColors() throws IOException {

        // set up defaults -- this might go away later...
        BACKGROUND = Color.black;
        m_terrainColors[0] = new Color(218, 215, 170);
        SINKHOLE = new Color(218, 215, 170);
        m_terrainColors[Terrains.WOODS] = new Color(180, 230, 130);
        HEAVY_WOODS = new Color(160, 200, 100);
        ULTRA_HEAVY_WOODS = new Color(0, 100, 0);
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
        m_terrainColors[Terrains.FUEL_TANK] = new Color(255, 204, 204);
        m_terrainColors[Terrains.BRIDGE] = new Color(109, 55, 25);
        m_terrainColors[Terrains.ICE] = new Color(204, 204, 255);
        m_terrainColors[Terrains.MAGMA] = new Color(200, 0, 0);
        //m_terrainColors[Terrains.MUD] = new Color(218, 160, 100);
        m_terrainColors[Terrains.JUNGLE] = new Color(180, 230, 130);
        m_terrainColors[Terrains.FIELDS] = new Color(250, 255, 205);
        m_terrainColors[Terrains.INDUSTRIAL] = new Color(112, 138, 144);
        m_terrainColors[Terrains.SPACE] = Color.gray;

        // now try to read in the config file
        int red;
        int green;
        int blue;

        File coloursFile = new File(
                Configuration.hexesDir(), GUIPreferences.getInstance().getMinimapColours()
        );

        // only while the defaults are hard-coded!
        if (!coloursFile.exists()) {
            return;
        }

        Reader cr = new FileReader(coloursFile);
        StreamTokenizer st = new StreamTokenizer(cr);

        st.lowerCaseMode(true);
        st.quoteChar('"');
        st.commentChar('#');

        scan:
        while (true) {
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
                        unitSize = (int) st.nval;
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

    private void clean() {
        dirtyMap = false;
        for (int i = 0; i < dirty.length; i++) {
            for (int j = 0; j < dirty[i].length; j++) {
                dirty[i][j] = false;
            }
        }
    }

    void initializeMap() {

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
        requiredWidth = (m_board.getWidth()
                         * (currentHexSide + currentHexSideBySin30))
                        + currentHexSideBySin30 + (2 * margin);
        requiredHeight = (((2 * m_board.getHeight()) + 1)
                          * currentHexSideByCos30) + (2 * margin) + buttonHeight;

        dirty = new boolean[(m_board.getWidth() / 10) + 1][(m_board.getHeight() / 10) + 1];
        dirtyMap = true;
        
        unitSize = unitSizes[zoom];

        // ensure its on screen
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                virtualBounds = virtualBounds.union(gc[i].getBounds());
            }
        }
        // zoom out if its too big for the screen
        while ((zoom > 0)
               && ((requiredWidth > virtualBounds.width) || (requiredHeight > virtualBounds.height))) {
            zoom--;
            currentHexSide = hexSide[zoom];
            currentHexSideByCos30 = hexSideByCos30[zoom];
            currentHexSideBySin30 = hexSideBySin30[zoom];
            requiredWidth = (m_board.getWidth()
                             * (currentHexSide + currentHexSideBySin30))
                            + currentHexSideBySin30 + (2 * margin);
            requiredHeight = (((2 * m_board.getHeight()) + 1)
                              * currentHexSideByCos30) + (2 * margin) + buttonHeight;
        }
        int x = getParent().getLocation().x;
        int y = getParent().getLocation().y;
        if ((x + requiredWidth) > virtualBounds.getMaxX()) {
            x = (int) (virtualBounds.getMaxX() - requiredWidth);
        }
        if (x < virtualBounds.getMinX()) {
            x = (int) (virtualBounds.getMinX());
        }
        if ((y + requiredHeight) > virtualBounds.getMaxY()) {
            y = (int) (virtualBounds.getMaxY() - requiredHeight);
        }
        if (y < virtualBounds.getMinY()) {
            y = (int) (virtualBounds.getMinY());
        }
        getParent().setLocation(x, y);
        int xTemp = requiredWidth;
        int yTemp = requiredHeight;

        if (m_dialog instanceof JScrollPane) {
            //For the scrollPane, enforce the minimum size.
            if (requiredWidth < SCROLL_PANE_WIDTH) {
                xTemp = SCROLL_PANE_WIDTH;
            }
            if (requiredHeight < SCROLL_PANE_HEIGHT) {
                yTemp = SCROLL_PANE_HEIGHT;
            }
        }
        
        if (minimized) {
            yTemp = 14;
        }
        
        setSize(xTemp, yTemp);
        setPreferredSize(new Dimension(xTemp, yTemp));
        if (m_dialog instanceof JDialog) {
            ((JDialog) m_dialog).pack();
        }
        // m_dialog.setVisible(true);
        m_mapImage = createImage(getSize().width, getSize().height);

        terrainBuffer = createImage(getSize().width, getSize().height);
        Graphics gg = terrainBuffer.getGraphics();
        gg.setColor(BACKGROUND);
        gg.fillRect(0, 0, getSize().width, getSize().height);

        if (getSize().width > requiredWidth) {
            leftMargin = ((getSize().width - requiredWidth) / 2) + margin;
        }
        if (getSize().height > requiredHeight) {
            topMargin = ((getSize().height - requiredHeight) / 2) + margin;
        }
        drawMap();
        revalidate();
    }

    protected long lastDrawMapReq = 0;
    protected long lastDrawStarted = 0;
    protected Runnable drawMapable = new Runnable() {
        protected final int redrawDelay = 0;

        public void run() {
            try {
                if ((System.currentTimeMillis() - lastDrawMapReq) > redrawDelay) {
                    drawMapOrig();
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        // should never happen
                    }
                    SwingUtilities.invokeLater(drawMapable);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    };

    /**
     * this replaces the original drawmap to speed up updates this can be called
     * any time necessary
     */
    public synchronized void drawMap() {
        lastDrawMapReq = System.currentTimeMillis();
        SwingUtilities.invokeLater(drawMapable);
    }

    /**
     * update the backbuffer and repaint.. should not require a synchronized but
     * i left it there anyways
     */
    protected synchronized void drawMapOrig() {
        if (lastDrawStarted > lastDrawMapReq) {
            return;
        }
        lastDrawStarted = System.currentTimeMillis();
        if (m_mapImage == null) {
            return;
        }

        if (!m_dialog.isVisible()) {
            return;
        }

        Graphics g = m_mapImage.getGraphics();
        // Activate AA
        if (GUIPreferences.getInstance().getAntiAliasing()) {
            ((Graphics2D)g).setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        
        Color oldColor = g.getColor();
        // g.setColor(BACKGROUND);
        // g.fillRect(0, 0, getSize().width, getSize().height);
        g.setColor(oldColor);
        if (!minimized) {
            roadHexIndexes.removeAllElements();
            Graphics gg = terrainBuffer.getGraphics();
            // Activate AA
            if (GUIPreferences.getInstance().getAntiAliasing()) {
                ((Graphics2D)gg).setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            }
            for (int j = 0; j < m_board.getWidth(); j++) {
                for (int k = 0; k < m_board.getHeight(); k++) {
                    IHex h = m_board.getHex(j, k);
                    if (dirtyMap || dirty[j / 10][k / 10]) {
                        gg.setColor(terrainColor(h, j, k));
                        paintCoord(gg, j, k, true);
                    }
                    addRoadElements(h, j, k);
                }
            }
            // draw backbuffer
            g.drawImage(terrainBuffer, 0, 0, this);

            if (firstLOS != null) {
                paintSingleCoordBorder(g, firstLOS.getX(), firstLOS.getY(), Color.red);
            }
            if (secondLOS != null) {
                paintSingleCoordBorder(g, secondLOS.getX(), secondLOS.getY(), Color.red);
            }

            if (!roadHexIndexes.isEmpty()) {
                paintRoads(g);
            }

            if (SHOW_NO_HEIGHT != heightDisplayMode) {
                for (int j = 0; j < m_board.getWidth(); j++) {
                    for (int k = 0; k < m_board.getHeight(); k++) {
                        IHex h = m_board.getHex(j, k);
                        paintHeight(g, h, j, k);
                    }
                }
            }

            // draw Drop Zone
            if ((null != m_client) && (null != m_game)) { // sanity check!
                if (IGame.Phase.PHASE_DEPLOYMENT == m_game.getPhase()) {
                    GameTurn turn = m_game.getTurn();
                    if ((turn != null)
                        && (turn.getPlayerNum() == m_client.getLocalPlayer()
                                                           .getId())) {
                        Entity depEnt = m_bview.getDeployingEntity();
                        int dir;
                        // We need to draw the same deployment zone as boardview
                        if (depEnt != null &&
                            depEnt.getOwnerId() == turn.getPlayerNum()) {
                            dir = depEnt.getStartingPos();
                        } else { // if we can't get the deploy zone from the 
                            // board view, punt and use the players zone
                            dir = m_client.getLocalPlayer().getStartingPos();
                        }

                        for (int j = 0; j < m_board.getWidth(); j++) {
                            for (int k = 0; k < m_board.getHeight(); k++) {
                                if (m_board.isLegalDeployment(
                                        new Coords(j, k), dir)) {
                                    paintSingleCoordBorder(g, j, k,
                                                           Color.yellow);
                                }
                            }
                        }
                    }
                }
                
                // draw declared fire
                if ((IGame.Phase.PHASE_FIRING == m_game.getPhase())
                    || (IGame.Phase.PHASE_PHYSICAL == m_game.getPhase())) {
                    for (Enumeration<EntityAction> iter = m_game.getActions(); iter
                            .hasMoreElements(); ) {
                        EntityAction action = iter.nextElement();
                        if (action instanceof AttackAction) {
                            paintAttack(g, (AttackAction) action);
                        }
                    }
                }

                multiUnits.clear();
                for (Entity e : m_game.getEntitiesVector()) {
                    if (e.getPosition() == null) {
                        continue;
                    }
                    paintUnit(g, e);
                }
            }
            clean();
        }

        if ((m_client != null) && (m_client.getArtilleryAutoHit() != null)) {
            for (int i = 0; i < m_client.getArtilleryAutoHit().size(); i++) {
                drawAutoHit(g, m_client.getArtilleryAutoHit().get(i));
            }
        }

        if (m_dialog instanceof JDialog) {
            drawBtn(g);
        }

        repaint();
    }
    
    private void paintBVSection(Graphics g) {
        if (minimized || (m_bview == null)) {
            return;
        }
        double[] relSize = m_bview.getVisibleArea();
        for (int i=0;i<4;i++) relSize[i] = Math.min(1, Math.max(0,relSize[i]));
        
        Color sc = g.getColor();
        Stroke sbs = ((Graphics2D) g).getStroke();
        
        // thicker but translucent rect
        g.setColor(new Color(100,100,160,80));
        ((Graphics2D) g).setStroke(new BasicStroke(zoom+2));
        
        g.drawRect(
                (int)(relSize[0]*             (hexSide[zoom] + hexSideBySin30[zoom])*m_board.getWidth())+leftMargin,
                (int)(relSize[1]*2*hexSideByCos30[zoom]*m_board.getHeight())+topMargin,
                (int)((relSize[2]-relSize[0])*(hexSide[zoom] + hexSideBySin30[zoom])*m_board.getWidth()),
                (int)((relSize[3]-relSize[1])*2*hexSideByCos30[zoom]*m_board.getHeight()));
        
        // thin less translucent rect
        g.setColor(new Color(255,255,255,180));
        ((Graphics2D) g).setStroke(new BasicStroke(zoom/2));

        g.drawRect(
                (int)(relSize[0]*(hexSide[zoom] + hexSideBySin30[zoom])*m_board.getWidth())+leftMargin,
                (int)(relSize[1]*2*hexSideByCos30[zoom]*m_board.getHeight())+topMargin,
                (int)((relSize[2]-relSize[0])*(hexSide[zoom] + hexSideBySin30[zoom])*m_board.getWidth()),
                (int)((relSize[3]-relSize[1])*2*hexSideByCos30[zoom]*m_board.getHeight()));
        
        // restore values
        ((Graphics2D) g).setStroke(sbs);
        g.setColor(sc);
        
    }

    /**
     * Draws a red crosshair for artillery autohit hexes (predesignated only).
     */
    private void drawAutoHit(Graphics g, Coords hex) {
        int baseX = (hex.getX() * (hexSide[zoom] + hexSideBySin30[zoom])) + leftMargin
                    + hexSide[zoom];
        int baseY = (((2 * hex.getY()) + 1 + (hex.getX() % 2)) * hexSideByCos30[zoom])
                    + topMargin;
        Color alt = g.getColor();
        g.setColor(Color.RED);
        g.drawOval(baseX - (unitSize - 1), baseY - (unitSize - 1),
                   (2 * unitSize) - 2, (2 * unitSize) - 2);
        g.drawLine(baseX - unitSize - 1, baseY, (baseX - unitSize) + 3, baseY);
        g.drawLine(baseX + unitSize + 1, baseY, (baseX + unitSize) - 3, baseY);
        g.drawLine(baseX, baseY - unitSize - 1, baseX, (baseY - unitSize) + 3);
        g.drawLine(baseX, baseY + unitSize + 1, baseX, (baseY + unitSize) - 3);
        g.setColor(alt);
    }

    /**
     * Draws green JButton in the bottom to close and open mini map. Height of
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
            g.fillRect(3, (getSize().height - 14) + 6, 8, 2);
            if (zoom == (hexSide.length - 1)) {
                g.setColor(Color.gray.brighter());
            } else {
                g.setColor(Color.yellow);
            }
            g.fillRect((getSize().width - 14) + 3, (getSize().height - 14) + 6, 8,
                       2);
            g.fillRect((getSize().width - 14) + 6, (getSize().height - 14) + 3, 2,
                       8);

            if (zoom > 2) {
                // JButton for displying heights.
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
                        label = Messages.getString("MiniMap.BuildingHeightLabel"); //$NON-NLS-1$
                        break;
                    case SHOW_TOTAL_HEIGHT:
                        label = Messages.getString("MiniMap.TotalHeightLabel"); //$NON-NLS-1$
                        break;
                    default:
                        label = ""; //$NON-NLS-1$
                }
                g.drawString(label, 17, (getSize().height - 14) + 12);
            }
        }

        g.setColor(oldColor);

    }

    private void paintHeight(Graphics g, IHex h, int x, int y) {
        if (heightDisplayMode == SHOW_NO_HEIGHT) {
            return;
        }
        if (zoom > 2) {
            int baseX = (x * (hexSide[zoom] + hexSideBySin30[zoom])) + leftMargin;
            int baseY = (((2 * y) + 1 + (x % 2)) * hexSideByCos30[zoom]) + topMargin;
            g.setColor(Color.white);
            int height = 0;
            if ((h.getTerrain(Terrains.BUILDING) != null)
                && (heightDisplayMode == SHOW_BUILDING_HEIGHT)) {
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

    private void paintSingleCoordBorder(Graphics g, int x, int y, Color c) {
        int baseX = (x * (hexSide[zoom] + hexSideBySin30[zoom])) + leftMargin;
        int baseY = (((2 * y) + 1 + (x % 2)) * hexSideByCos30[zoom]) + topMargin;
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
        g.setColor(c);
        g.drawPolygon(xPoints, yPoints, 6);
    }

    private void paintCoord(Graphics g, int x, int y, boolean border) {
        int baseX = (x * (hexSide[zoom] + hexSideBySin30[zoom])) + leftMargin;
        int baseY = (((2 * y) + 1 + (x % 2)) * hexSideByCos30[zoom]) + topMargin;
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

    /**
     * Draw a line to represent an attack
     */
    private void paintAttack(Graphics g, AttackAction attack) {
        Entity source = m_game.getEntity(attack.getEntityId());
        Targetable target = m_game.getTarget(attack.getTargetType(),
                attack.getTargetId());
        // sanity check...
        if ((null == source) || (null == target)) {
            return;
        }

        if (attack.getTargetType() == Targetable.TYPE_INARC_POD) {
            // iNarc pods don't have a position, so lets scrap this idea, shall
            // we?
            return;
        }
        if (attack instanceof WeaponAttackAction) {
            WeaponAttackAction waa = (WeaponAttackAction) attack;
            if ((attack.getTargetType() == Targetable.TYPE_HEX_ARTILLERY)
                    && (waa.getEntity(m_game).getOwner().getId() != m_client
                            .getLocalPlayer().getId())) {
                return;
            }
        }
        Color oldColor = g.getColor();

        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        xPoints[0] = ((source.getPosition().getX() * (hexSide[zoom] + hexSideBySin30[zoom]))
                + leftMargin + ((int) 1.5 * hexSide[zoom])) - 2;
        yPoints[0] = (((2 * source.getPosition().getY()) + 1 + (source
                .getPosition().getX() % 2)) * hexSideByCos30[zoom]) + topMargin;
        xPoints[1] = ((target.getPosition().getX() * (hexSide[zoom] + hexSideBySin30[zoom]))
                + leftMargin + ((int) 1.5 * hexSide[zoom])) - 2;
        yPoints[1] = (((2 * target.getPosition().getY()) + 1 + (target
                .getPosition().getX() % 2)) * hexSideByCos30[zoom]) + topMargin;
        xPoints[2] = xPoints[1] + 2;
        xPoints[3] = xPoints[0] + 2;
        if (((source.getPosition().getX() > target.getPosition().getX()) && (source
                .getPosition().getY() < target.getPosition().getY()))
                || ((source.getPosition().getX() < target.getPosition().getX()) && (source
                        .getPosition().getY() > target.getPosition().getY()))) {
            yPoints[3] = yPoints[0] + 2;
            yPoints[2] = yPoints[1] + 2;
        } else {
            yPoints[3] = yPoints[0] - 2;
            yPoints[2] = yPoints[1] - 2;
        }
        g.setColor(PlayerColors.getColor(source.getOwner().getColorIndex()));
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(Color.black);
        g.drawPolygon(xPoints, yPoints, 4);

        // if this is mutual fire, draw a half-and-half line
        for (Enumeration<EntityAction> iter = m_game.getActions(); iter
                .hasMoreElements();) {
            EntityAction action = iter.nextElement();
            if (action instanceof AttackAction) {
                AttackAction otherAttack = (AttackAction) action;
                if ((attack.getEntityId() == otherAttack.getTargetId())
                        && (otherAttack.getEntityId() == attack.getTargetId())) {
                    // attackTarget _must_ be an entity since it's shooting back
                    // (?)
                    Entity attackTarget = m_game.getEntity(otherAttack
                            .getEntityId());
                    g.setColor(PlayerColors.getColor(attackTarget.getOwner()
                            .getColorIndex()));

                    xPoints[0] = xPoints[3];
                    yPoints[0] = yPoints[3];
                    xPoints[1] = xPoints[2];
                    yPoints[1] = yPoints[2];
                    xPoints[2] = xPoints[1] + 2;
                    xPoints[3] = xPoints[0] + 2;
                    if (((source.getPosition().getX() > target.getPosition()
                            .getX()) && (source.getPosition().getY() < target
                            .getPosition().getY()))
                            || ((source.getPosition().getX() < target
                                    .getPosition().getX()) && (source
                                    .getPosition().getY() > target
                                    .getPosition().getY()))) {
                        yPoints[3] = yPoints[0] + 2;
                        yPoints[2] = yPoints[1] + 2;
                    } else {
                        yPoints[3] = yPoints[0] - 2;
                        yPoints[2] = yPoints[1] - 2;
                    }
                    g.fillPolygon(xPoints, yPoints, 4);
                    g.setColor(Color.black);
                    g.drawPolygon(xPoints, yPoints, 4);
                    break;
                }
            }
        }

        g.setColor(oldColor);
    }

    private void paintUnit(Graphics g, Entity entity) {
        boolean sensors = m_game.getOptions().booleanOption(
                OptionsConstants.ADVANCED_TACOPS_SENSORS);
        boolean sensorsDetectAll = m_game.getOptions().booleanOption(
                OptionsConstants.ADVANCED_SENSORS_DETECT_ALL);
        boolean doubleBlind = m_game.getOptions().booleanOption(
                OptionsConstants.ADVANCED_DOUBLE_BLIND);
        boolean hasVisual = entity.hasSeenEntity(m_bview.getLocalPlayer());
        boolean hasDetected = entity.hasDetectedEntity(m_bview.getLocalPlayer());
        
        int baseX = (entity.getPosition().getX() * (hexSide[zoom] + hexSideBySin30[zoom]))
                + leftMargin + hexSide[zoom];
        int baseY = (((2 * entity.getPosition().getY()) + 1 + (entity
                .getPosition().getX() % 2)) * hexSideByCos30[zoom]) + topMargin;
        int[] xPoints;
        int[] yPoints;

        if (sensors && doubleBlind && !sensorsDetectAll
                && hasDetected && !hasVisual) { // Sensor Return
            String sensorReturn = "?";           
            Font font = new Font("SansSerif", Font.BOLD, fontSize[zoom]); //$NON-NLS-1$            
            int width = getFontMetrics(font).stringWidth(sensorReturn) / 2;
            int height = getFontMetrics(font).getHeight() / 2 - 2;
            g.setFont(font);
            g.setColor(Color.RED);
            g.drawString(sensorReturn, baseX - width, baseY + height);
            return;
        } else if (doubleBlind && !hasVisual) { // Unseen Unit
            // Do nothing
            return;
        } else if (entity instanceof Mech) {
            xPoints = new int[3];
            yPoints = new int[3];
            xPoints[0] = baseX;
            yPoints[0] = baseY - unitSize;
            xPoints[1] = baseX - unitSize;
            yPoints[1] = baseY + (unitSize / 2);
            xPoints[2] = baseX + unitSize;
            yPoints[2] = baseY + (unitSize / 2);
        } else if (entity instanceof VTOL) {
            xPoints = new int[8];
            yPoints = new int[8];
            xPoints[0] = baseX - unitSize;
            xPoints[1] = baseX - (unitSize / 3);
            xPoints[2] = baseX;
            xPoints[3] = baseX + (unitSize / 3);
            xPoints[4] = baseX + unitSize;
            xPoints[5] = xPoints[3];
            xPoints[6] = xPoints[2];
            xPoints[7] = xPoints[1];
            yPoints[0] = baseY;
            yPoints[1] = baseY - (unitSize / 3);
            yPoints[2] = baseY - unitSize;
            yPoints[3] = baseY - (unitSize / 3);
            yPoints[4] = baseY;
            yPoints[5] = baseY + (unitSize / 3);
            yPoints[6] = baseY + unitSize;
            yPoints[7] = baseY + (unitSize / 3);
        } else if (entity instanceof Tank) {
            xPoints = new int[4];
            yPoints = new int[4];
            xPoints[0] = baseX - ((unitSize * 2) / 3);
            yPoints[0] = baseY - ((unitSize * 2) / 3);
            xPoints[1] = baseX - ((unitSize * 2) / 3);
            yPoints[1] = baseY + ((unitSize * 2) / 3);
            xPoints[2] = baseX + ((unitSize * 2) / 3);
            yPoints[2] = baseY + ((unitSize * 2) / 3);
            xPoints[3] = baseX + ((unitSize * 2) / 3);
            yPoints[3] = baseY - ((unitSize * 2) / 3);
        } else if (entity instanceof Protomech) {
            xPoints = new int[3];
            yPoints = new int[3];
            xPoints[0] = baseX;
            yPoints[0] = baseY + unitSize;
            xPoints[1] = baseX + unitSize;
            yPoints[1] = baseY - (unitSize / 2);
            xPoints[2] = baseX - unitSize;
            yPoints[2] = baseY - (unitSize / 2);
        } else if (entity instanceof GunEmplacement) {
            int twip = (unitSize * 2) / 3;
            xPoints = new int[8];
            yPoints = new int[8];
            xPoints[0] = baseX - (twip / 2);
            yPoints[0] = baseY - ((twip * 3) / 2);
            xPoints[1] = xPoints[0] - twip;
            yPoints[1] = yPoints[0] + twip;
            xPoints[2] = xPoints[1];
            yPoints[2] = yPoints[1] + twip;
            xPoints[3] = xPoints[2] + twip;
            yPoints[3] = yPoints[2] + twip;
            xPoints[4] = xPoints[3] + twip;
            yPoints[4] = yPoints[3];
            xPoints[5] = xPoints[4] + twip;
            yPoints[5] = yPoints[4] - twip;
            xPoints[6] = xPoints[5];
            yPoints[6] = yPoints[5] - twip;
            xPoints[7] = xPoints[6] - twip;
            yPoints[7] = yPoints[6] - twip;
        } else {
            // entity instanceof Infantry
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

        Graphics2D g2 = (Graphics2D)g;
        Stroke svStroke = g2.getStroke();

        if (GUIPreferences.getInstance().getBoolean(GUIPreferences.MMSYMBOL)) {
            AffineTransform svTransform = g2.getTransform();

            // Transform for placement and scaling
            AffineTransform t = AffineTransform.getTranslateInstance(baseX, baseY);
            t.scale(stratZoom[zoom]/100.0d, fontSize[zoom]/100.0d);
            g2.transform(t);

            // Add a position shift if multiple units are present in this hex
            Coords p = entity.getPosition();
            Integer eStack = multiUnits.get(p);
            if (eStack == null) eStack = 0;
            eStack++;
            multiUnits.put(p, eStack);
            g2.translate(20*(eStack-1), -20*(eStack-1));

            // White border to set off the icon from the background
            g2.setStroke(new BasicStroke(30f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
            g2.setColor(new Color(255,255,255,150));
            g2.draw(STRAT_BASERECT);

            // Black background to fill forms like the Dropship
            g2.setColor(Color.BLACK);
            g2.fill(STRAT_BASERECT);

            // Rectangle border for all units
            g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.draw(STRAT_BASERECT);

            // Select the correct form for the entity
            Path2D form;
            if ((entity instanceof Mech) || (entity instanceof Protomech)) {
                form = STRAT_MECH;
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            } else if (entity instanceof VTOL) {
                form = STRAT_VTOL;
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            } else if (entity instanceof Tank) {
                if (entity.getMovementMode() == EntityMovementMode.HOVER) {
                    form = STRAT_HOVER;
                } else if (entity.getMovementMode() == EntityMovementMode.WHEELED) {
                    form = STRAT_WHEELED;
                } else if ((entity.getMovementMode() == EntityMovementMode.HYDROFOIL) ||
                        (entity.getMovementMode() == EntityMovementMode.NAVAL)) {
                    form = STRAT_NAVAL; 
                } else {
                    form = STRAT_TANKTRACKED;
                }
            } else if (entity instanceof Aero) {
                if (((Aero) entity).isFighter()) {
                    form = STRAT_AERO;
                } else {
                    form = STRAT_SPHEROID;
                }
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            } else {
                form = STRAT_INFANTRY;
            }

            // Fill the form in player color
            g2.setColor(new Color(PlayerColors.getColorRGB(
                    entity.getOwner().getColorIndex())));
            g2.fill(form);

            // Add the weight class or other lettering for certain units
            g.setColor(Color.BLACK);
            if ((entity instanceof Protomech) || (entity instanceof Mech) || (entity instanceof Aero)) {
                String s = "";
                if (entity instanceof Protomech) {
                    s = "P";
                } else if ((entity instanceof Mech) && ((Mech)entity).isIndustrial()) {
                    s = "I";
                } else if (entity.getWeightClass() < 6) {
                    s = STRAT_WEIGHTS[entity.getWeightClass()];
                }
                if (!s.equals("")) {
                    FontRenderContext fontContext = new FontRenderContext(null, true, true);
                    Font font = new Font("SansSerif", Font.BOLD, 100);
                    FontMetrics currentMetrics = getFontMetrics(font);
                    int stringWidth = currentMetrics.stringWidth(s);
                    GlyphVector gv = font.createGlyphVector(fontContext, s);
                    g2.fill(gv.getOutline((int)STRAT_CX-stringWidth/2,(float)SYMBOLSIZE.getHeight()/3.0f));
                }
            } else if (entity instanceof MechWarrior) {
                g2.setColor(Color.black);
                g2.fillOval(0 - 25, 0 - 25, 50, 50);
            }
            // Draw the unit icon in black
            g2.draw(form);

            g2.setTransform(svTransform);
            
        } else {
            // Drawn a circle for MechWarriors
            if (entity instanceof MechWarrior) {
                // Draw a slight dark border
                int radius = unitSize - 3;
                int dia = radius * 2;
                ((Graphics2D)g).setStroke(new BasicStroke(unitBorder[zoom]+2));
                g.setColor(new Color(100,100,100,200));
                g.drawOval(baseX - radius, baseY - radius, dia, dia);

                // Fill the icon according to player color
                Color pColor = new Color(
                        PlayerColors.getColorRGB(entity.getOwner().getColorIndex()));
                g.setColor(pColor);
                g.fillOval(baseX - radius, baseY - radius, dia, dia);

                // Draw a white border to better show the player color
                ((Graphics2D)g).setStroke(new BasicStroke(unitBorder[zoom]));
                g.setColor(Color.WHITE);
                g.drawOval(baseX - radius, baseY - radius, dia, dia);
            } else {
                // Draw a slight dark border to set off the icon from the background
                ((Graphics2D)g).setStroke(new BasicStroke(unitBorder[zoom]+2));
                g.setColor(new Color(100,100,100,200));
                g.drawPolygon(xPoints, yPoints, xPoints.length);

                // Fill the icon according to the player color
                Color pColor = new Color(
                        PlayerColors.getColorRGB(entity.getOwner().getColorIndex()));
                g.setColor(pColor);
                g.fillPolygon(xPoints, yPoints, xPoints.length);

                // Draw a white border to better show the player color
                // maybe useful later: if (!entity.isSelectableThisTurn()) {
                ((Graphics2D)g).setStroke(new BasicStroke(unitBorder[zoom]));
                g.setColor(Color.WHITE);
                g.drawPolygon(xPoints, yPoints, xPoints.length);
            }

        }
        
        // Create a colored circle if this is the selected unit
        Entity se = (clientgui == null) ? null : 
            m_game.getEntity(clientgui.getSelectedEntityNum());
        
        if (entity == se) {
            g2.setStroke(new BasicStroke(unitBorder[zoom]+1));
            g2.setColor(GUIPreferences.getInstance().getColor(
                    GUIPreferences.ADVANCED_UNITOVERVIEW_SELECTED_COLOR));
            int rad = unitSize*2-1;
            g2.drawOval(baseX-rad, baseY-rad, rad*2, rad*2);
        }

        g2.setStroke(svStroke);
    }

    private void paintRoads(Graphics g) {
        int exits = 0;
        int baseX, baseY, x, y;
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];
        Color oldColor = g.getColor();
        g.setColor(m_terrainColors[Terrains.ROAD]);
        for (Enumeration<int[]> iter = roadHexIndexes.elements(); iter
                .hasMoreElements(); ) {
            int[] hex = iter.nextElement();
            x = hex[0];
            y = hex[1];
            baseX = (x * (hexSide[zoom] + hexSideBySin30[zoom])) + leftMargin
                    + hexSide[zoom];
            baseY = (((2 * y) + 1 + (x % 2)) * hexSideByCos30[zoom]) + topMargin;
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
                xPoints[1] = Math.round((baseX + ((3 * hexSide[zoom]) / 4))
                                        - halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round(baseY - (hexSideByCos30[zoom] / 2)
                                        - halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] + (2 * halfRoadWidthBySin30[zoom]);
                yPoints[2] = yPoints[1] + (2 * halfRoadWidthByCos30[zoom]);
                xPoints[3] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY + halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a South-East exit?
            if (0 != (exits & 0x0004)) {
                xPoints[0] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY - halfRoadWidthByCos30[zoom];
                xPoints[1] = Math.round(baseX + ((3 * hexSide[zoom]) / 4)
                                        + halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round((baseY + (hexSideByCos30[zoom] / 2))
                                        - halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] - (2 * halfRoadWidthBySin30[zoom]);
                yPoints[2] = yPoints[1] + (2 * halfRoadWidthByCos30[zoom]);
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
                xPoints[1] = Math.round((baseX - ((3 * hexSide[zoom]) / 4))
                                        + halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round(baseY + (hexSideByCos30[zoom] / 2)
                                        + halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] - (2 * halfRoadWidthBySin30[zoom]);
                yPoints[2] = yPoints[1] - (2 * halfRoadWidthByCos30[zoom]);
                xPoints[3] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY - halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a North-West exit?
            if (0 != (exits & 0x0020)) {
                xPoints[0] = baseX - halfRoadWidthBySin30[zoom];
                yPoints[0] = baseY + halfRoadWidthByCos30[zoom];
                xPoints[1] = Math.round(baseX - ((3 * hexSide[zoom]) / 4)
                                        - halfRoadWidthBySin30[zoom]);
                yPoints[1] = Math.round((baseY - (hexSideByCos30[zoom] / 2))
                                        + halfRoadWidthByCos30[zoom]);
                xPoints[2] = xPoints[1] + (2 * halfRoadWidthBySin30[zoom]);
                yPoints[2] = yPoints[1] - (2 * halfRoadWidthByCos30[zoom]);
                xPoints[3] = baseX + halfRoadWidthBySin30[zoom];
                yPoints[3] = baseY - halfRoadWidthByCos30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }

        }
        g.setColor(oldColor);
    }

    /**
     * check if hex contains roadelements and if it does, add to roadHexIndexes
     */
    private void addRoadElements(IHex x, int boardX, int boardY) {
        final int[] roadTypes = new int[]{Terrains.ROAD, Terrains.BRIDGE};
        for (int j : roadTypes) {
            if ((x.getTerrain(j) != null) && (m_terrainColors[j] != null)) {
                int[] roadHex = {boardX, boardY, x.getTerrain(j).getExits()};
                roadHexIndexes.addElement(roadHex);
            }
        }
    }

    private Color terrainColor(IHex x, int boardX, int boardY) {
        Color terrColor = m_terrainColors[0];
        if (x.getLevel() < 0) {
            // sinkholes have their own colour
            terrColor = SINKHOLE;
        }

        int terrain = 0;
        // Check for Smoke and Fire - this overrides any other colors
        if (x.containsTerrain(Terrains.SMOKE) && x.containsTerrain(Terrains.FIRE)) {
            terrColor = SMOKE_AND_FIRE;
        // Check for Fire - this overrides any other colors
        } else if (x.containsTerrain(Terrains.FIRE)) {
            terrColor = m_terrainColors[Terrains.FIRE];
        } else { // Otherwise, color based on terrains - higher valued terrains take color precedence
            for (int j = m_terrainColors.length - 1; j >= 0; j--) {
                if ((x.getTerrain(j) != null) && (m_terrainColors[j] != null)) {
                    if ((j == Terrains.ROAD) || (j == Terrains.BRIDGE)) {
                        continue;
                    }
                    terrColor = m_terrainColors[j];
                    terrain = j;
                    // make heavy woods darker
                    if (((j == Terrains.WOODS) || (j == Terrains.JUNGLE)) && (x.getTerrain(j).getLevel() == 2)) {
                        terrColor = HEAVY_WOODS;
                    }
                    if (((j == Terrains.WOODS) || (j == Terrains.JUNGLE)) && (x.getTerrain(j).getLevel() > 2)) {
                        terrColor = ULTRA_HEAVY_WOODS;
                    }
                    break;
                }
            }
        }
        int level = 0;

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
                if (level > 10) {
                    level = 10;
                }
                r = terrColor.getRed() - (level * 15);
                g = terrColor.getGreen() - (level * 15);
                b = terrColor.getBlue() - (level * 15);
                if (r < 0) {
                    r = 0;
                }
                if (g < 0) {
                    g = 0;
                }
                if (b < 0) {
                    b = 0;
                }
                return new Color(r, g, b);
            case Terrains.FUEL_TANK:
            case Terrains.BUILDING:
                level = Math.abs(x.ceiling());
                // By experiment it is possible to make only 6 distinctive color
                // steps
                if (level > 10) {
                    level = 10;
                }
                r = terrColor.getRed() - (level * 15);
                g = terrColor.getGreen() - (level * 15);
                b = terrColor.getBlue() - (level * 15);
                if (r < 0) {
                    r = 0;
                }
                if (g < 0) {
                    g = 0;
                }
                if (b < 0) {
                    b = 0;
                }
                return new Color(r, g, b);

        }
        /*
         * if (terrain < 5){ level = (int) Math.abs(x.floor()); // By experiment
         * it is possible to make only 6 distinctive color steps if (level > 5)
         * level = 5; int r = terrColor.getRed()-level30; int g =
         * terrColor.getGreen()-level30; int b = terrColor.getBlue()-level30; if
         * (r < 0) r = 0; if (g < 0) g = 0; if (b < 0) b = 0; return new
         * Color(r, g, b); }
         */
        return terrColor;
    }

    private Coords translateCoords(int x, int y) {
        int gridX = (x / (hexSideBySin30[zoom] + hexSide[zoom]));
        int restX = x % (hexSideBySin30[zoom] + hexSide[zoom]);
        int gridY = (y / (2 * hexSideByCos30[zoom]));
        int restY = y % (2 * hexSideByCos30[zoom]);

        boolean evenColumn = (gridX & 1) == 0;

        if (restY < hexSideByCos30[zoom]) {
            if (evenColumn) {
                if (restX < ((((restY - hexSideByCos30[zoom])
                               * hexSideBySin30[zoom]) / hexSideByCos30[zoom]) * -1)) {
                    gridX--;
                    gridY--;
                }
            } else {
                if (restX < ((restY * hexSideBySin30[zoom]) / hexSideByCos30[zoom])) {
                    gridX--;
                } else {
                    gridY--;
                }
            }
        } else {
            if (evenColumn) {
                if (restX < (((restY - hexSideByCos30[zoom])
                              * hexSideBySin30[zoom]) / hexSideByCos30[zoom])) {
                    gridX--;
                }
            } else {
                if (restX < ((((restY - (2 * hexSideByCos30[zoom]))
                               * hexSideBySin30[zoom]) / hexSideByCos30[zoom]) * -1)) {
                    gridX--;
                }
            }
        }
        /*
         * restX = hexSideBySin30[zoom] + hexSide[zoom] - restX; restY -=
         * hexSideByCos30[zoom]; if (hexSideBySin30[zoom]restX >
         * hexSideByCos30[zoom]restY) gridX ++; if (-hexSideBySin30[zoom]restX >
         * hexSideByCos30[zoom]restY) gridY --;
         */
        if (gridX < 0) {
            gridX = 0;
        }
        if (gridY < 0) {
            gridY = 0;
        }

        return new Coords(gridX, gridY);
    }

    protected void zoomIn() {
        if (zoom == 0) {
            return;
        }
        zoom--;
        initializeMap();
    }

    protected void zoomOut() {
        if (zoom == (hexSide.length - 1)) {
            return;
        }
        zoom++;
        initializeMap();
    }

    void processMouseClick(int x, int y, MouseEvent me) {
        if (y > (getSize().height - 14) && !dragging) {
            if (minimized) {
                setSize(getSize().width, heightBufer);
                m_mapImage = createImage(getSize().width, heightBufer);
                minimized = false;
                initializeMap();
            } else {
                if (x < 14) {
                    zoomIn();
                } else if ((x < 28) && (zoom > 2)) {
                    heightDisplayMode = ((++heightDisplayMode) > NBR_MODES) ? 0
                            : heightDisplayMode;
                    initializeMap();
                } else if (x > (getSize().width - 14)) {
                    zoomOut();
                } else {
                    // Minimize button
                    heightBufer = getSize().height;
                    setSize(getSize().width, 14);
                    m_mapImage = createImage(Math.max(1, getSize().width), 14);

                    minimized = true;
                    initializeMap();
                }  
            }
        } else if (m_bview != null) {
            if ((x < margin) || (x > (getSize().width - leftMargin))
                || (y < topMargin)
                || (y > (getSize().height - topMargin - 14))) {
                return;
            }
            if ((me.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                m_bview
                        .checkLOS(translateCoords(x - leftMargin, y - topMargin));
            } else {
                m_bview.centerOnPointRel(
                        ((double)(x - leftMargin))/(double)((hexSideBySin30[zoom] + hexSide[zoom])*m_board.getWidth()),
                        ((double)(y - topMargin))/(double)(2 * hexSideByCos30[zoom]*m_board.getHeight()));
                m_bview.stopSoftCentering();
                repaint();
            }
        }
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int z) {
        zoom = z;
    }

    protected BoardListener boardListener = new BoardListenerAdapter() {
        @Override
        public void boardNewBoard(BoardEvent b) {
            initializeMap();
        }

        @Override
        public void boardChangedHex(BoardEvent b) {
            if (dirty == null) {
                dirtyMap = true;
            } else {
                /*
                 * this must be tolerant since it might be called without
                 * notifying us of the boardsize first
                 */
                int x = b.getCoords().getX();
                int y = b.getCoords().getY();
                if ((x >= dirty.length) || (y >= dirty[x].length)) {
                    dirtyMap = true;
                    return;
                }
                dirty[x / 10][y / 10] = true;
            }

        }
    };

    protected GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            drawMap();
        }

        @Override
        public void gameTurnChange(GameTurnChangeEvent e) {
            drawMap();
        }

        @Override
        public void gameBoardNew(GameBoardNewEvent e) {
            IBoard b = e.getOldBoard();
            if (b != null) {
                b.removeBoardListener(boardListener);
            }
            b = e.getNewBoard();
            if (b != null) {
                b.addBoardListener(boardListener);
            }
            m_board = b;
            initializeMap();
        }

        @Override
        public void gameBoardChanged(GameBoardChangeEvent e) {
            drawMap();
        }
    };

    BoardViewListener boardViewListener = new BoardViewListenerAdapter() {
        @Override
        public void hexCursor(BoardViewEvent b) {
            update();
        }
        
        @Override
        public void hexMoused(BoardViewEvent b) {
            update();
        }

        @Override
        public void boardHexHighlighted(BoardViewEvent b) {
            update();
        }

        @Override
        public void hexSelected(BoardViewEvent b) {
            update();
        }

        @Override
        public void firstLOSHex(BoardViewEvent b) {
            secondLOS = null;
            firstLOS = b.getCoords();
            drawMap();
        }

        @Override
        public void secondLOSHex(BoardViewEvent b, Coords c) {
            firstLOS = c;
            secondLOS = b.getCoords();
            drawMap();
        }

        private void update() {
            firstLOS = null;
            secondLOS = null;
            drawMap();
        }
    };

    MouseListener mouseListener = new MouseAdapter() {

        public void mouseReleased(MouseEvent me) {
            // Center main map on clicked area, if there was no dragging
            if (m_dialog instanceof JDialog && !dragging) {
                processMouseClick(me.getX(), me.getY(), me);
            }
            // Clear up variables related to dragging
            dragging = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

    };

    MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging) {
                dragging = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            processMouseClick(e.getX(), e.getY(), e);
        }
    };

    MouseWheelListener mouseWheelListener = new MouseWheelListener() {
        public void mouseWheelMoved(MouseWheelEvent we) {
            if (we.getWheelRotation() < 0) {
                //Zoom Out: The processMouseClick method uses absolute
                // positions within the minimap to determine what the click
                // does.  These parameters should activate the zoom-out features
                processMouseClick(getSize().width - 12, getSize().height - 12, we);
            } else {
                //Zoom In: The processMouseClick method uses absolute
                // positions within the minimap to determine what the click
                // does.  These parameters should activate the zoom-in features
                processMouseClick(0, getSize().height - 12, we);
            }
        }
    };

    ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent ce) {
            drawMap();
        }

        @Override
        public void componentResized(ComponentEvent ce) {
            // if (!minimized) initializeMap();
        }
    };

}
