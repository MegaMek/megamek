/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007,2008 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing.boardview;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneLayout;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import megamek.client.TimerSingleton;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.MechDisplayEvent;
import megamek.client.ui.IBoardView;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.ChatterBox2;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.MovementDisplay;
import megamek.client.ui.swing.TilesetManager;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.ImageCache;
import megamek.client.ui.swing.util.ImprovedAveragingScaleFilter;
import megamek.client.ui.swing.util.KeyAlphaFilter;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.widget.MegamekBorder;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Aero;
import megamek.common.ArtilleryTracker;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Flare;
import megamek.common.Game;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.ITerrain;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.PlanetaryConditions;
import megamek.common.SpecialHexDisplay;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.UnitLocation;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.BoardEvent;
import megamek.common.event.BoardListener;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;

/**
 * Displays the board; lets the user scroll around and select points on it.
 */
public class BoardView1 extends JPanel implements IBoardView, Scrollable,
        BoardListener, MouseListener, IPreferenceChangeListener, AutoCloseable {

    private static final long serialVersionUID = -5582195884759007416L;

    static final int TRANSPARENT = 0xFFFF00FF;

    private static final int BOARD_HEX_CLICK = 1;
    private static final int BOARD_HEX_DOUBLECLICK = 2;
    private static final int BOARD_HEX_DRAG = 3;
    private static final int BOARD_HEX_POPUP = 4;

    // the dimensions of megamek's hex images
    static final int HEX_W = 84;
    static final int HEX_H = 72;
    private static final int HEX_WC = HEX_W - (HEX_W / 4);
    static final int HEX_ELEV = 12;

    private static final float[] ZOOM_FACTORS = { 0.30f, 0.41f, 0.50f, 0.60f,
            0.68f, 0.79f, 0.90f, 1.00f, 1.09f, 1.17f };

    // Set to TRUE to draw hexes with isometric elevation.
    private boolean drawIsometric = GUIPreferences.getInstance()
            .getIsometricEnabled();

    int DROPSHDW_DIST = 20;

    // the index of zoom factor 1.00f
    static final int BASE_ZOOM_INDEX = 7;

    // Initial zoom index
    public int zoomIndex = BASE_ZOOM_INDEX;

    // line width of the c3 network lines
    static final int C3_LINE_WIDTH = 1;

    // line width of the fly over lines
    static final int FLY_OVER_LINE_WIDTH = 3;

    private static Font FONT_7 = new Font("SansSerif", Font.PLAIN, 7); //$NON-NLS-1$
    private static Font FONT_8 = new Font("SansSerif", Font.PLAIN, 8); //$NON-NLS-1$
    private static Font FONT_9 = new Font("SansSerif", Font.PLAIN, 9); //$NON-NLS-1$
    private static Font FONT_10 = new Font("SansSerif", Font.PLAIN, 10); //$NON-NLS-1$
    private static Font FONT_12 = new Font("SansSerif", Font.PLAIN, 12); //$NON-NLS-1$

    Dimension hex_size = null;

    private Font font_note = FONT_10;
    private Font font_hexnum = FONT_10;
    private Font font_elev = FONT_9;
    private Font font_minefield = FONT_12;

    IGame game;

    private Dimension boardSize;
    private Dimension preferredSize = new Dimension(0,0);

    // scrolly stuff:
    private JScrollPane scrollpane = null;
    private JScrollBar vbar;
    private JScrollBar hbar;
    private int scrollXDifference = 0;
    private int scrollYDifference = 0;
    // are we drag-scrolling?
    private boolean dragging = false;
    // should we scroll when the mouse is dragged?
    private boolean shouldScroll = false;

    // entity sprites
    private ArrayList<EntitySprite> entitySprites = new ArrayList<EntitySprite>();
    private ArrayList<IsometricSprite> isometricSprites = new ArrayList<IsometricSprite>();

    private ArrayList<FlareSprite> flareSprites = new ArrayList<FlareSprite>();
    private HashMap<ArrayList<Integer>, EntitySprite> entitySpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>();
    private HashMap<ArrayList<Integer>, IsometricSprite> isometricSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>();

    // sprites for the three selection cursors
    private CursorSprite cursorSprite;
    private CursorSprite highlightSprite;
    private CursorSprite selectedSprite;
    private CursorSprite firstLOSSprite;
    private CursorSprite secondLOSSprite;

    // sprite for current movement
    ArrayList<StepSprite> pathSprites = new ArrayList<StepSprite>();

    private ArrayList<FiringSolutionSprite> firingSprites = new ArrayList<FiringSolutionSprite>();

    private ArrayList<MovementEnvelopeSprite> moveEnvSprites = new ArrayList<>();
    private ArrayList<MovementModifierEnvelopeSprite> moveModEnvSprites = new ArrayList<>();

    // vector of sprites for all firing lines
    ArrayList<AttackSprite> attackSprites = new ArrayList<AttackSprite>();

    // vector of sprites for all movement paths (using vectored movement)
    private ArrayList<MovementSprite> movementSprites = new ArrayList<MovementSprite>();

    // vector of sprites for C3 network lines
    private ArrayList<C3Sprite> c3Sprites = new ArrayList<C3Sprite>();

    // vector of sprites for aero flyover lines
    private ArrayList<FlyOverSprite> flyOverSprites = new ArrayList<FlyOverSprite>();

    TilesetManager tileManager = null;

    // polygons for a few things
    Polygon hexPoly;
    Polygon[] facingPolys;
    Polygon[] movementPolys;

    // the player who owns this BoardView's client
    private IPlayer localPlayer = null;

    /**
     * Stores the currently deploying entity, used for highlighting deployment
     * hexes.
     */
    private Entity en_Deployer = null;

    // should be able to turn it off(board editor)
    private boolean useLOSTool = true;

    // Initial scale factor for sprites and map
    float scale = 1.00f;
    private ImageCache<Integer, Image> scaledImageCache =
            new ImageCache<Integer, Image>();

    // Displayables (Chat box, etc.)
    ArrayList<IDisplayable> displayables = new ArrayList<IDisplayable>();

    // Move units step by step
    private ArrayList<MovingUnit> movingUnits = new ArrayList<MovingUnit>();

    private long moveWait = 0;

    // moving entity sprites
    private ArrayList<MovingEntitySprite> movingEntitySprites = new ArrayList<MovingEntitySprite>();
    private HashMap<Integer, MovingEntitySprite> movingEntitySpriteIds = new HashMap<Integer, MovingEntitySprite>();
    private ArrayList<GhostEntitySprite> ghostEntitySprites = new ArrayList<GhostEntitySprite>();

    protected transient ArrayList<BoardViewListener> boardListeners = new ArrayList<BoardViewListener>();

    // wreck sprites
    private ArrayList<WreckSprite> wreckSprites = new ArrayList<WreckSprite>();
    private ArrayList<IsometricWreckSprite> isometricWreckSprites = new ArrayList<IsometricWreckSprite>();

    private Coords rulerStart;
    private Coords rulerEnd;
    private Color rulerStartColor;
    private Color rulerEndColor;

    private Coords lastCursor;
    private Coords highlighted;
    Coords selected;
    private Coords firstLOS;

    // selected entity and weapon for artillery display
    Entity selectedEntity = null;
    private Mounted selectedWeapon = null;

    // hexes with ECM effect
    private HashMap<Coords, Integer> ecmHexes = null;

    // reference to our timertask for redraw
    private TimerTask ourTask = null;

    BufferedImage bvBgBuffer = null;
    ImageIcon bvBgIcon = null;
    BufferedImage scrollPaneBgBuffer = null;
    ImageIcon scrollPaneBgIcon = null;
    
    private static final int FRAMES = 24;
    private long totalTime;
    private long averageTime;
    private int frameCount;
    private Font fpsFont = new Font("SansSerif", 0, 20); //$NON-NLS-1$


    /**
     * Keeps track of whether we have an active ChatterBox2
     */
    private boolean chatterBoxActive = false;


    FovHighlightingAndDarkening fovHighlightingAndDarkening;
    
    private String FILENAME_FLARE_IMAGE = "flare.png";
    
    private Image flareImage;

    /**
     * Construct a new board view for the specified game
     */
    public BoardView1(final IGame game, final MegaMekController controller)
            throws java.io.IOException {
        this.game = game;

        tileManager = new TilesetManager(this);
        ToolTipManager.sharedInstance().registerComponent(this);

        game.addGameListener(gameListener);
        game.getBoard().addBoardListener(this);
        ourTask = scheduleRedrawTimer();// call only once
        addMouseListener(this);
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent we) {
                Point mousePoint = we.getPoint();
                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    if (!(disp instanceof ChatterBox2)) {
                        break;
                    }
                    double width = scrollpane.getViewport().getSize()
                            .getWidth();
                    double height = scrollpane.getViewport().getSize()
                            .getHeight();
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    // we need to adjust the point, because it should be against
                    // the displayable dimension
                    Point dispPoint = new Point();
                    dispPoint.setLocation(mousePoint.x + getBounds().x,
                            mousePoint.y + getBounds().y);
                    if (disp.isMouseOver(dispPoint, drawDimension)) {
                        ChatterBox2 cb2 = (ChatterBox2) disp;
                        if (we.getWheelRotation() > 0) {
                            cb2.scrollDown();
                        } else {
                            cb2.scrollUp();
                        }
                        refreshDisplayables();
                        return;
                    }
                }
                if (GUIPreferences.getInstance().getMouseWheelZoom()) {
                    boolean zoomIn = ((we.getWheelRotation() > 0) && !GUIPreferences
                            .getInstance().getMouseWheelZoomFlip())
                            || ((we.getWheelRotation() <= 0) && GUIPreferences
                                    .getInstance().getMouseWheelZoomFlip());
                    if (zoomIn) {
                        zoomIn();
                    } else {
                        zoomOut();
                    }
                } else {
                    if (we.isControlDown()) {
                        boolean zoomIn = ((we.getWheelRotation() > 0) && !GUIPreferences
                                .getInstance().getMouseWheelZoomFlip())
                                || ((we.getWheelRotation() <= 0) && GUIPreferences
                                        .getInstance().getMouseWheelZoomFlip());
                        if (zoomIn) {
                            zoomOut();
                        } else {
                            zoomIn();
                        }
                    } else if (we.isShiftDown()) {
                        int notches = we.getWheelRotation();
                        if (notches < 0) {
                            hbar.setValue((int) (hbar.getValue() - (HEX_H
                                    * scale * (-1 * notches))));

                        } else {
                            hbar.setValue((int) (hbar.getValue() + (HEX_H
                                    * scale * (notches))));
                        }
                    } else {
                        int notches = we.getWheelRotation();
                        if (notches < 0) {
                            vbar.setValue((int) (vbar.getValue() - (HEX_H
                                    * scale * (-1 * notches))));

                        } else {
                            vbar.setValue((int) (vbar.getValue() + (HEX_H
                                    * scale * (notches))));
                        }
                    }
                }

            }
        });

        MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();
                if (null == point) {
                    return;
                }

                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    if (disp.isBeingDragged()) {
                        return;
                    }
                    double width = Math.min(boardSize.getWidth(), scrollpane
                            .getViewport().getSize().getWidth());
                    double height = Math.min(boardSize.getHeight(), scrollpane
                            .getViewport().getSize().getHeight());
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    disp.isMouseOver(point, drawDimension);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point point = e.getPoint();
                if (null == point) {
                    return;
                }
                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    Point adjustPoint = new Point((int) Math.min(
                            boardSize.getWidth(), -getBounds().getX()),
                            (int) Math.min(boardSize.getHeight(), -getBounds()
                                    .getY()));
                    Point dispPoint = new Point();
                    dispPoint.x = point.x - adjustPoint.x;
                    dispPoint.y = point.y - adjustPoint.y;
                    double width = Math.min(boardSize.getWidth(), scrollpane
                            .getViewport().getSize().getWidth());
                    double height = Math.min(boardSize.getHeight(), scrollpane
                            .getViewport().getSize().getHeight());
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    if (disp.isDragged(dispPoint, drawDimension)) {
                        repaint();
                        return;
                    }
                }
                // only scroll when we should
                if (!shouldScroll) {
                    mouseAction(getCoordsAt(point), BOARD_HEX_DRAG,
                            e.getModifiers());
                    return;
                }
                // if we have not yet been dragging, set the var so popups don't
                // appear when we stop scrolling
                if (!dragging) {
                    dragging = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                Point p = scrollpane.getViewport().getViewPosition();
                int newX = p.x - (e.getX() - scrollXDifference);
                int newY = p.y - (e.getY() - scrollYDifference);
                int maxX = getWidth() - scrollpane.getViewport().getWidth();
                int maxY = getHeight() - scrollpane.getViewport().getHeight();
                if (newX < 0) {
                    newX = 0;
                }
                if (newX > maxX) {
                    newX = maxX;
                }
                if (newY < 0) {
                    newY = 0;
                }
                if (newY > maxY) {
                    newY = maxY;
                }
                // don't scroll horizontally if the board fits into the window
                if (scrollpane.getViewport().getWidth() >= getWidth()) {
                    newX = scrollpane.getViewport().getViewPosition().x;
                }
                scrollpane.getViewport().setViewPosition(new Point(newX, newY));
            }
        };
        addMouseMotionListener(mouseMotionListener);

        registerKeyboardCommands(this, controller);
        
        // setAutoscrolls(true);

        updateBoardSize();

        hex_size = new Dimension((int) (HEX_W * scale), (int) (HEX_H * scale));

        initPolys();

        cursorSprite = new CursorSprite(this, Color.cyan);
        highlightSprite = new CursorSprite(this, Color.white);
        selectedSprite = new CursorSprite(this, Color.blue);
        firstLOSSprite = new CursorSprite(this, Color.red);
        secondLOSSprite = new CursorSprite(this, Color.red);

        PreferenceManager.getClientPreferences().addPreferenceChangeListener(
                this);

        SpecialHexDisplay.Type.ARTILLERY_HIT.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_INCOMING.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_TARGET.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_ADJUSTED.init(getToolkit());
        SpecialHexDisplay.Type.ARTILLERY_AUTOHIT.init(getToolkit());
        SpecialHexDisplay.Type.PLAYER_NOTE.init(getToolkit());

        fovHighlightingAndDarkening = new FovHighlightingAndDarkening(this);
        
        flareImage = getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_FLARE_IMAGE)
                        .toString());
    }
    
    private void registerKeyboardCommands(final BoardView1 bv, 
            final MegaMekController controller) {
        // Register the action for TOGGLE_CHAT
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT.cmd,
                new CommandAction(){

                    @Override
                    public boolean shouldPerformAction(){
                        if (getChatterBoxActive() || !bv.isVisible()
                                || game.getPhase() == Phase.PHASE_LOUNGE){
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (!getChatterBoxActive()){
                            setChatterBoxActive(true);
                            for (IDisplayable disp : displayables){
                                if (disp instanceof ChatterBox2){
                                    ((ChatterBox2)disp).slideUp();
                                }
                            }
                            requestFocus();
                        }
                    }

        });

        // Register the action for TOGGLE_CHAT
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT_CMD.cmd,
                new CommandAction(){

                    @Override
                    public boolean shouldPerformAction(){
                        if (getChatterBoxActive() || !bv.isVisible()
                                || game.getPhase() == Phase.PHASE_LOUNGE){
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (!getChatterBoxActive()){
                            setChatterBoxActive(true);
                            for (IDisplayable disp : displayables){
                                if (disp instanceof ChatterBox2){
                                    ((ChatterBox2)disp).slideUp();
                                    ((ChatterBox2)disp).setMessage("/");
                                }
                            }
                            requestFocus();
                        }
                    }

        });

        // Register the action for CENTER_ON_SELECTED
        controller.registerCommandAction(KeyCommandBind.CENTER_ON_SELECTED.cmd,
                new CommandAction(){

                    @Override
                    public boolean shouldPerformAction(){
                        if (getChatterBoxActive() || !bv.isVisible()
                                || game.getPhase() == Phase.PHASE_LOUNGE
                                || selectedEntity == null){
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (selectedEntity != null) {
                            centerOnHex(selectedEntity.getPosition());
                        }
                    }

        });

        // Register the action for SCROLL_NORTH
        controller.registerCommandAction(KeyCommandBind.SCROLL_NORTH.cmd,
                new CommandAction(){

                    @Override
                    public boolean shouldPerformAction(){
                        if (getChatterBoxActive() || !bv.isVisible()
                                || game.getPhase() == Phase.PHASE_LOUNGE){
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_SOUTH);
                        vbar.setValue((int)
                                (vbar.getValue() - (HEX_H * scale)));
                    }

        });

        // Register the action for SCROLL_SOUTH
        controller.registerCommandAction(KeyCommandBind.SCROLL_SOUTH.cmd,
                new CommandAction(){

                    @Override
                    public boolean shouldPerformAction(){
                        if (getChatterBoxActive() || !bv.isVisible()
                                || game.getPhase() == Phase.PHASE_LOUNGE){
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_NORTH);
                        vbar.setValue((int)
                                (vbar.getValue() + (HEX_H * scale)));
                    }

        });

        // Register the action for SCROLL_EAST
        controller.registerCommandAction(KeyCommandBind.SCROLL_EAST.cmd,
                new CommandAction(){

                    @Override
                    public boolean shouldPerformAction(){
                        if (getChatterBoxActive() || !bv.isVisible()
                                || game.getPhase() == Phase.PHASE_LOUNGE){
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_WEST);
                        hbar.setValue((int)
                                (hbar.getValue() + (HEX_W * scale)));
                    }

        });

        // Register the action for SCROLL_WEST
        controller.registerCommandAction(KeyCommandBind.SCROLL_WEST.cmd,
                new CommandAction(){

                    @Override
                    public boolean shouldPerformAction(){
                        if (getChatterBoxActive() || !bv.isVisible()
                                || game.getPhase() == Phase.PHASE_LOUNGE){
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_EAST);
                        hbar.setValue((int)
                                (hbar.getValue() - (HEX_W * scale)));
                    }

        });
    }

    @Override
    public void close(){
        //There are a lot of listeners that should be removed, fortunately there should
        //be no more than one instance of BoardView1 in lifetime of app so no memory leak is possible.
       fovHighlightingAndDarkening.close();
    }

    protected final RedrawWorker redrawWorker = new RedrawWorker();

    /**
     * this should only be called once!! this will cause a timer to schedule
     * constant screen updates every 20 milliseconds!
     */
    protected TimerTask scheduleRedrawTimer() {
        final TimerTask redraw = new TimerTask() {
            @Override
            public void run() {
                try {
                    SwingUtilities.invokeLater(redrawWorker);
                } catch (Exception ie) {
                    System.err.print("Error scheduleRedrawTimer "); //$NON-NLS-1$
                    System.err.print(ie.getMessage());
                    System.err.print(": "); //$NON-NLS-1$
                    System.err.println("ignoring");
                }
            }
        };
        TimerSingleton.getInstance().schedule(redraw, 20, 20);
        return redraw;
    }

    protected void scheduleRedraw() {
        try {
            SwingUtilities.invokeLater(redrawWorker);
        } catch (Exception ie) {
            System.err.print("Error scheduleRedraw "); //$NON-NLS-1$
            System.err.print(ie.getMessage());
            System.err.print(": "); //$NON-NLS-1$
            System.err.println("ignoring");
        }
    }

    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(IClientPreferences.MAP_TILESET)) {
            updateBoard();
        }
    }

    /**
     * Adds the specified board listener to receive board events from this
     * board.
     *
     * @param listener
     *            the board listener.
     */
    public void addBoardViewListener(BoardViewListener listener) {
        if (!boardListeners.contains(listener)) {
            boardListeners.add(listener);
        }
    }

    /**
     * Removes the specified board listener.
     *
     * @param listener
     *            the board listener.
     */
    public void removeBoardViewListener(BoardViewListener listener) {
        boardListeners.remove(listener);
    }

    /**
     * Notifies attached board listeners of the event.
     *
     * @param event
     *            the board event.
     */
    public void processBoardViewEvent(BoardViewEvent event) {
        if (boardListeners == null) {
            return;
        }
        for (BoardViewListener l : boardListeners) {
            switch (event.getType()) {
                case BoardViewEvent.BOARD_HEX_CLICKED:
                case BoardViewEvent.BOARD_HEX_DOUBLECLICKED:
                case BoardViewEvent.BOARD_HEX_DRAGGED:
                case BoardViewEvent.BOARD_HEX_POPUP:
                    l.hexMoused(event);
                    break;
                case BoardViewEvent.BOARD_HEX_CURSOR:
                    l.hexCursor(event);
                    break;
                case BoardViewEvent.BOARD_HEX_HIGHLIGHTED:
                    l.boardHexHighlighted(event);
                    break;
                case BoardViewEvent.BOARD_HEX_SELECTED:
                    l.hexSelected(event);
                    break;
                case BoardViewEvent.BOARD_FIRST_LOS_HEX:
                    l.firstLOSHex(event);
                    break;
                case BoardViewEvent.BOARD_SECOND_LOS_HEX:
                    l.secondLOSHex(event, getFirstLOS());
                    break;
                case BoardViewEvent.FINISHED_MOVING_UNITS:
                    l.finishedMovingUnits(event);
                    break;
                case BoardViewEvent.SELECT_UNIT:
                    l.unitSelected(event);
                    break;
            }
        }
    }

    void addMovingUnit(Entity entity, Vector<UnitLocation> movePath) {
        if (!movePath.isEmpty()) {
            MovingUnit m = new MovingUnit(entity, movePath);
            movingUnits.add(m);

            GhostEntitySprite ghostSprite = new GhostEntitySprite(this, entity);
            ghostEntitySprites.add(ghostSprite);

            // Center on the starting hex of the moving unit.
            UnitLocation loc = movePath.get(0);
            centerOnHex(loc.getCoords());
        }
    }

    public void addDisplayable(IDisplayable disp) {
        displayables.add(disp);
    }

    public void removeDisplayable(IDisplayable disp) {
        displayables.remove(disp);
    }

    /**
     * Draw the screen!
     */
    @Override
    public synchronized void paintComponent(Graphics g) {
        GUIPreferences guip = GUIPreferences.getInstance();
        
        long startTime = 0;
        if (guip.getBoolean(GUIPreferences.ADVANCED_SHOW_FPS)) {
            startTime = System.nanoTime();
        }        
        
        if (guip.getAntiAliasing()){
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        if (!isTileImagesLoaded()) {
            g.drawString(Messages.getString("BoardView1.loadingImages"), 20, 50); //$NON-NLS-1$
            if (!tileManager.isStarted()) {
                System.out.println("boardview1: loading images for board"); //$NON-NLS-1$
                tileManager.loadNeededImages(game);
            }
            // wait 1 second, then repaint
            repaint(1000);
            return;
        }

        Rectangle viewRect = scrollpane.getVisibleRect();
        if (bvBgBuffer == null || bvBgBuffer.getWidth() != viewRect.getWidth()
                || bvBgBuffer.getHeight() != viewRect.getHeight()) {
            bvBgBuffer = new BufferedImage((int)viewRect.getWidth(),
                    (int)viewRect.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics bgGraph = bvBgBuffer.getGraphics();
            if (bvBgIcon != null) {
                int w = (int)viewRect.getWidth();
                int h = (int)viewRect.getHeight();
                int iW = bvBgIcon.getIconWidth();
                int iH = bvBgIcon.getIconHeight();
                
                for (int x = 0; x < w; x+=iW){
                    for (int y = 0; y < h; y+=iH){
                        bgGraph.drawImage(bvBgIcon.getImage(), x, y,
                                bvBgIcon.getImageObserver());
                    }
                }
            }
        }
        g.drawImage(bvBgBuffer, g.getClipBounds().x, g.getClipBounds().y, null);

        // Used to pad the board edge
        g.translate(HEX_W, HEX_H);

        drawHexes(g, g.getClipBounds());

        // draw wrecks
        if (guip.getShowWrecks() && !useIsometric()) {
            drawSprites(g, wreckSprites);
        }

        if ((game.getPhase() == Phase.PHASE_MOVEMENT) && !useIsometric()) {
            drawSprites(g, moveEnvSprites);
            drawSprites(g, moveModEnvSprites);
        }

        // Minefield signs all over the place!
        drawMinefields(g);

        // Artillery targets
        drawArtilleryHexes(g);

        // draw highlight border
        drawSprite(g, highlightSprite);

        // draw cursors
        drawSprite(g, cursorSprite);
        drawSprite(g, selectedSprite);
        drawSprite(g, firstLOSSprite);
        drawSprite(g, secondLOSSprite);

        // draw deployment indicators.
        // For Isometric rendering, this is done during drawHexes
        if ((en_Deployer != null) && !useIsometric()) {
            drawDeployment(g);
        }

        // draw Flare Sprites
        drawSprites(g, flareSprites);
        
        // draw C3 links
        drawSprites(g, c3Sprites);

        // draw flyover routes
        if (game.getBoard().onGround()) {
            drawSprites(g, flyOverSprites);
        }

        // draw onscreen entities
        drawSprites(g, entitySprites);

        // draw moving onscreen entities
        drawSprites(g, movingEntitySprites);

        // draw ghost onscreen entities
        drawSprites(g, ghostEntitySprites);

        // draw onscreen attacks
        drawSprites(g, attackSprites);

        // draw movement vectors.
        if (game.useVectorMove()
                && (game.getPhase() == IGame.Phase.PHASE_MOVEMENT)) {
            drawSprites(g, movementSprites);
        }

        // draw movement, if valid
        drawSprites(g, pathSprites);

        // draw firing solution sprites, but only during the firing phase
        if ((game.getPhase() == Phase.PHASE_FIRING) ||
                (game.getPhase() == Phase.PHASE_OFFBOARD)) {
            drawSprites(g, firingSprites);
        }

        // draw the ruler line
        if (rulerStart != null) {
            Point start = getCentreHexLocation(rulerStart);
            if (rulerEnd != null) {
                Point end = getCentreHexLocation(rulerEnd);
                g.setColor(Color.yellow);
                g.drawLine(start.x, start.y, end.x, end.y);

                g.setColor(rulerEndColor);
                g.fillRect(end.x - 1, end.y - 1, 2, 2);
            }

            g.setColor(rulerStartColor);
            g.fillRect(start.x - 1, start.y - 1, 2, 2);
        }

        // Undo the previous translation
        g.translate(-HEX_W, -HEX_H);


        // draw all the "displayables"
        Rectangle rect = new Rectangle();
        rect.x = -getX();
        rect.y = -getY();
        rect.width = scrollpane.getViewport().getViewRect().width;
        rect.height = scrollpane.getViewport().getViewRect().height;
        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.get(i);
            disp.draw(g, rect);
        }
        
        if (guip.getBoolean(GUIPreferences.ADVANCED_SHOW_FPS)) {
            if (frameCount == FRAMES) {
                averageTime = totalTime / FRAMES;
                totalTime = 0; frameCount = 0;
            } else {
                totalTime += System.nanoTime() - startTime;
                frameCount++;
            }
            String s = String.format("%1$5.3f", averageTime / 1000000d);
            g.setFont(fpsFont);
            g.setColor(Color.YELLOW);
            g.drawString(s, g.getClipBounds().x + 5, g.getClipBounds().y + 20);
        }
    }

    /**
     * Updates the boardSize variable with the proper values for this board.
     */
    private void updateBoardSize() {
        int width = (game.getBoard().getWidth() * (int) (HEX_WC * scale))
                + (int) ((HEX_W / 4) * scale);
        int height = (game.getBoard().getHeight() * (int) (HEX_H * scale))
                + (int) ((HEX_H / 2) * scale);
        boardSize = new Dimension(width, height);
    }

    /**
     * Looks through a vector of buffered images and draws them if they're
     * onscreen.
     */
    private synchronized void drawSprites(Graphics g,
            ArrayList<? extends Sprite> spriteArrayList) {
        for (Sprite sprite : spriteArrayList) {
            drawSprite(g, sprite);
        }
    }

    private synchronized void drawHexSpritesForHex(Coords c,
            Graphics g, ArrayList<? extends HexSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (HexSprite sprite : spriteArrayList) {
            Coords cp = sprite.getPosition();
            if (cp.equals(c) && view.intersects(sprite.getBounds())
                    && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, false);
            }
        }
    }

    /**
     * Draws the Entity for the given hex. This function is used by the
     * isometric rendering process so that sprites are drawn in the order that
     * hills are rendered to create the appearance that the sprite is behind the
     * hill.
     *
     * @param c
     *            The Coordinates of the hex that the sprites should be drawn
     *            for.
     * @param g
     *            The Graphics object for this board.
     * @param spriteArrayList
     *            The complete list of all IsometricSprite on the board.
     */
    private synchronized void drawIsometricSpritesForHex(Coords c, Graphics g,
            ArrayList<IsometricSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricSprite sprite : spriteArrayList) {
            Coords cp = sprite.getPosition();
            if (cp.equals(c) && view.intersects(sprite.getBounds())
                    && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, false);
            }
        }
    }

    /**
     * Draws the wrecksprites for the given hex. This function is used by the
     * isometric rendering process so that sprites are drawn in the order that
     * hills are rendered to create the appearance that the sprite is behind the
     * hill.
     *
     * @param c
     *            The Coordinates of the hex that the sprites should be drawn
     *            for.
     * @param g
     *            The Graphics object for this board.
     * @param spriteArrayList
     *            The complete list of all IsometricSprite on the board.
     */
    private synchronized void drawIsometricWreckSpritesForHex(Coords c,
            Graphics g, ArrayList<IsometricWreckSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricWreckSprite sprite : spriteArrayList) {
            Coords cp = sprite.getEntity().getPosition();
            if (cp.equals(c) && view.intersects(sprite.getBounds())
                    && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, false);
            }
        }
    }

    /**
     * Draws a translucent sprite without any of the companion graphics, if it
     * is in the current view. This is used only when performing isometric
     * rending. This function is used to show units (with 50% transparency) that
     * are hidden behind a hill.
     *
     * TODO: Optimize this function so that it is only applied to sprites that
     * are actually hidden. This implementation performs the second rendering
     * for all sprites.
     */
    private final void drawIsometricSprites(Graphics g,
            ArrayList<IsometricSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricSprite sprite : spriteArrayList) {
            if (view.intersects(sprite.getBounds()) && !sprite.hidden) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y,
                        this, true);
            }
        }
    }

    /**
     * Draws a sprite, if it is in the current view
     */
    private final void drawSprite(Graphics g, Sprite sprite) {
        Rectangle view = g.getClipBounds();
        if (view.intersects(sprite.getBounds()) && !sprite.hidden) {
            if (!sprite.isReady()) {
                sprite.prepare();
            }
            sprite.drawOnto(g, sprite.getBounds().x, sprite.getBounds().y, this);
        }
    }

    /**
     * Draw an outline around legal deployment hexes
     */
    private void drawDeployment(Graphics g) {
        Rectangle view = g.getClipBounds();
        // only update visible hexes
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;

        IBoard board = game.getBoard();
        // loop through the hexes
        for (int i = 0; i < drawHeight; i++) {
            for (int j = 0; j < drawWidth; j++) {
                Coords c = new Coords(j + drawX, i + drawY);
                if (board.isLegalDeployment(c, en_Deployer.getStartingPos())) {
                    drawHexBorder(getHexLocation(c), g, Color.yellow);
                }
            }
        }
    }

    /**
     * Darkens a hexes in the viewing area if there is no line of sight between
     * them and the supplied source hex. Used in non-isometric view.
     *
     * @param src
     *            The source hex for which line of sight originates
     * @param g
     *            The graphics object to draw on. / private void drawLos(Coords
     *            src, Graphics g) { Rectangle view = g.getClipBounds(); // only
     *            update visible hexes int drawX = (view.x / (int) (HEX_WC *
     *            scale)) - 1; int drawY = (view.y / (int) (HEX_H * scale)) - 1;
     *
     *            int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3; int
     *            drawHeight = (view.height / (int) (HEX_H * scale)) + 3;
     *
     *            // loop through the hexes for (int i = 0; i < drawHeight; i++)
     *            { for (int j = 0; j < drawWidth; j++) { Coords c = new
     *            Coords(j + drawX, i + drawY); if
     *            (game.getBoard().contains(c)){ drawLos(src,c,g); } } } }
     *
     *
     *            /** Darkens a destination hex if there is no line of sight
     *            between it and a source hex.
     *
     * @param src
     *            The source hex for which line of sight originates
     * @param dest
     *            The destination hex for computing the line of sight
     * @param g
     *            The graphics object to draw on. / private void drawLos(Coords
     *            src, Coords dest, Graphics g){ int max_dist; if (src == null
     *            || dest == null){ return; } if (selectedEntity != null){
     *            max_dist = game.getPlanetaryConditions().
     *            getVisualRange(selectedEntity, false); } else { max_dist = 30;
     *            } int dist = src.distance(dest); Color transparent_gray = new
     *            Color(0,0,0,100); if (dist <= max_dist && !(getLosEffects(src,
     *            dest).canSee())){ drawHexLayer(new Point(dest.x,dest.y), g,
     *            transparent_gray); } }
     */

    /**
     * Draw a layer of a solid color (alpha possible) on the hex at Point p no
     * padding by default
     */
    void drawHexLayer(Point p, Graphics g, Color col) {
        drawHexLayer(p, g, col, 0);
    }

    /**
     * Draw a layer of a solid color (alpha possible) on the hex at Point p with
     * some padding around the border
     */
    private void drawHexLayer(Point p, Graphics g, Color col, double pad) {
        g.setColor(col);

        final double[] x = { 0, 21. * scale, 62. * scale, 83. * scale };
        final double[] y = { 0, 35. * scale, 36. * scale, 71. * scale };
        g.setColor(col);

        if (pad > 0.1) {
            final double cos60 = 0.5;
            final double cos30 = 0.8660254;

            final double pd = pad * scale;
            final double a = cos60 * pad * scale;
            final double b = cos30 * pad * scale;

            int[] xcoords = { p.x + (int) (x[1] + a), p.x + (int) (x[2] - a),
                    p.x + (int) (x[3] - pd), p.x + (int) (x[3] - pd),
                    p.x + (int) (x[2] - a), p.x + (int) (x[1] + a),
                    p.x + (int) (x[0] + pd), p.x + (int) (x[0] + pd) };
            int[] ycoords = { p.y + (int) (y[0] + b), p.y + (int) (y[0] + b),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - b), p.y + (int) (y[3] - b),
                    p.y + (int) (y[2]), p.y + (int) (y[1]) };

            g.fillPolygon(xcoords, ycoords, 8);

        } else {

            int[] xcoords = { p.x + (int) (x[1]), p.x + (int) (x[2]),
                    p.x + (int) (x[3]), p.x + (int) (x[3]), p.x + (int) (x[2]),
                    p.x + (int) (x[1]), p.x + (int) (x[0]), p.x + (int) (x[0]) };
            int[] ycoords = { p.y + (int) (y[0]), p.y + (int) (y[0]),
                    p.y + (int) (y[1]), p.y + (int) (y[2]), p.y + (int) (y[3]),
                    p.y + (int) (y[3]), p.y + (int) (y[2]), p.y + (int) (y[1]) };

            g.fillPolygon(xcoords, ycoords, 8);
        }
    }

    /**
     * Draw an outline around the hex at Point p no padding and a width of 1
     */
    private void drawHexBorder(Point p, Graphics g, Color col) {
        drawHexBorder(p, g, col, 0);
    }

    /**
     * Draw an outline around the hex at Point p padded around the border by pad
     * and a line-width of 1
     */
    private void drawHexBorder(Point p, Graphics g, Color col, double pad) {
        drawHexBorder(p, g, col, pad, 1);
    }

    /**
     * Draw a thick outline around the hex at Coords c padded around the border
     * by pad and a line-width of linewidth
     */
    void drawHexBorder(Point p, Graphics g, Color col, double pad,
            double linewidth) {

        g.setColor(col);

        final double[] x = { 0, 21. * scale, 62. * scale, 83. * scale };
        final double[] y = { 0, 35. * scale, 36. * scale, 71. * scale };

        final double cos60 = 0.5;
        final double cos30 = 0.8660254;

        final double pd = pad * scale;
        final double a = cos60 * pd;
        final double b = cos30 * pd;

        if (linewidth < 1.5) {

            int[] xcoords = { p.x + (int) (x[1] + a), p.x + (int) (x[2] - a),
                    p.x + (int) (x[3] - pd), p.x + (int) (x[3] - pd),
                    p.x + (int) (x[2] - a), p.x + (int) (x[1] + a),
                    p.x + (int) (x[0] + pd), p.x + (int) (x[0] + pd) };
            int[] ycoords = { p.y + (int) (y[0] + b), p.y + (int) (y[0] + b),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - b), p.y + (int) (y[3] - b),
                    p.y + (int) (y[2]), p.y + (int) (y[1]) };

            g.drawPolygon(xcoords, ycoords, 8);

        } else {

            final double pl = (pad * scale) + (linewidth * scale);
            final double c = cos60 * pl;
            final double d = cos30 * pl;

            int[] xcoords = { p.x + (int) (x[1] + a), p.x + (int) (x[2] - a),
                    p.x + (int) (x[3] - pd), p.x + (int) (x[3] - pd),
                    p.x + (int) (x[2] - a), p.x + (int) (x[1] + a),
                    p.x + (int) (x[0] + pd), p.x + (int) (x[0] + pd),
                    p.x + (int) (x[1] + a), p.x + (int) (x[1] + c),
                    p.x + (int) (x[0] + pl), p.x + (int) (x[0] + pl),
                    p.x + (int) (x[1] + c), p.x + (int) (x[2] - c),
                    p.x + (int) (x[3] - pl), p.x + (int) (x[3] - pl),
                    p.x + (int) (x[2] - c), p.x + (int) (x[1] + c) };
            int[] ycoords = { p.y + (int) (y[0] + b), p.y + (int) (y[0] + b),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - b), p.y + (int) (y[3] - b),
                    p.y + (int) (y[2]), p.y + (int) (y[1]),
                    p.y + (int) (y[0] + b), p.y + (int) (y[0] + d),
                    p.y + (int) (y[1]), p.y + (int) (y[2]),
                    p.y + (int) (y[3] - d), p.y + (int) (y[3] - d),
                    p.y + (int) (y[2]), p.y + (int) (y[1]),
                    p.y + (int) (y[0] + d), p.y + (int) (y[0] + d) };

            g.fillPolygon(xcoords, ycoords, 18);
        }
    }

    /**
     * returns the weapon selected in the mech display, or null if none selected
     * or it is not artillery or null if the selected entity is not owned
     */
    private Mounted getSelectedArtilleryWeapon() {
        // We don't want to display artillery auto-hit/adjusted fire hexes
        // during
        // the artyautohithexes phase. These could be displayed if the player
        // uses the /reset command in some situations
        if (game.getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) {
            return null;
        }

        if ((selectedEntity == null) || (selectedWeapon == null)) {
            return null;
        }

        if (!selectedEntity.getOwner().equals(localPlayer)) {
            return null; // Not my business to see this
        }

        if (selectedEntity.getEquipmentNum(selectedWeapon) == -1) {
            return null; // inconsistent state - weapon not on entity
        }

        if (!((selectedWeapon.getType() instanceof WeaponType) && selectedWeapon
                .getType().hasFlag(WeaponType.F_ARTILLERY))) {
            return null; // not artillery
        }

        // otherwise, a weapon is selected, and it is artillery
        return selectedWeapon;
    }

    /**
     * Display artillery modifier in pretargeted hexes
     */
    private void drawArtilleryHexes(Graphics g) {
        Mounted weapon = getSelectedArtilleryWeapon();
        Rectangle view = g.getClipBounds();

        // Compute the origin of the viewing area
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        // Compute size of viewing area
        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;

        Image scaledImage;

        // Draw incoming artillery sprites - requires server to update client's
        // view of game
        for (Enumeration<ArtilleryAttackAction> attacks = game
                .getArtilleryAttacks(); attacks.hasMoreElements();) {
            ArtilleryAttackAction a = attacks.nextElement();
            Coords c = a.getTarget(game).getPosition();
            // Is the Coord within the viewing area?
            if ((c.x >= drawX) && (c.x <= (drawX + drawWidth))
                    && (c.y >= drawY) && (c.y <= (drawY + drawHeight))) {

                Point p = getHexLocation(c);
                scaledImage = tileManager
                        .getArtilleryTarget(TilesetManager.ARTILLERY_INCOMING);
                g.drawImage(scaledImage, p.x, p.y, this);
            }
        }

        // Draw pre-designated auto-hit hexes
        if (localPlayer != null) // Could be null, like in map-editor
        {
            for (Coords c : localPlayer.getArtyAutoHitHexes()) {
                // Is the Coord within the viewing area?
                if ((c.x >= drawX) && (c.x <= (drawX + drawWidth))
                        && (c.y >= drawY) && (c.y <= (drawY + drawHeight))) {

                    Point p = getHexLocation(c);
                    scaledImage = tileManager
                            .getArtilleryTarget(TilesetManager.ARTILLERY_AUTOHIT);
                    g.drawImage(scaledImage, p.x, p.y, this);
                }
            }
        }

        // Draw modifiers for selected entity and weapon
        if (weapon != null) {
            // Loop through all of the attack modifiers for this weapon
            for (ArtilleryTracker.ArtilleryModifier attackMod : selectedEntity.aTracker
                    .getWeaponModifiers(weapon)) {
                Coords c = attackMod.getCoords();
                // Is the Coord within the viewing area?
                if ((c.x >= drawX) && (c.x <= (drawX + drawWidth))
                        && (c.y >= drawY) && (c.y <= (drawY + drawHeight))) {

                    Point p = getHexLocation(c);
                    // draw the crosshairs
                    if (attackMod.getModifier() == TargetRoll.AUTOMATIC_SUCCESS) {
                        // predesignated or already hit
                        scaledImage = tileManager
                                .getArtilleryTarget(TilesetManager.ARTILLERY_AUTOHIT);
                    } else {
                        scaledImage = tileManager
                                .getArtilleryTarget(TilesetManager.ARTILLERY_ADJUSTED);
                    }
                    g.drawImage(scaledImage, p.x, p.y, this);
                }
            }
        }
    }

    /*
     * NOTENOTENOTE: (itmo) wouldnt this be simpler with two arrays. One with
     * the strings {"BoardView1.thunderblaablaa","BoardView1.Conventi.."} one
     * with the offsets {51,51,42} etc Preferably indexed by an enum: enum{
     * Conventional, Thunder; } or something?
     */
    /**
     * Writes "MINEFIELD" in minefield hexes...
     */
    private void drawMinefields(Graphics g) {
        Rectangle view = g.getClipBounds();
        // only update visible hexes
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;
        
        int maxX = drawX + drawWidth;
        int maxY = drawY + drawHeight;

        IBoard board = game.getBoard();
        for (Enumeration<Coords> minedCoords = game.getMinedCoords();
                minedCoords.hasMoreElements();) {
            Coords c = minedCoords.nextElement();
            // If the coords aren't visible, skip
            if (c.x < drawX || c.x > maxX || c.y < drawY || c.y > maxY
                    || !board.contains(c)) {
                continue;
            }
                
            Point p = getHexLocation(c);
            Image mineImg = getScaledImage(tileManager.getMinefieldSign(),true);
            g.drawImage(mineImg, p.x + (int) (13 * scale), p.y
                    + (int) (13 * scale), this);

            g.setColor(Color.black);
            int nbrMfs = game.getNbrMinefields(c);
            if (nbrMfs > 1) {
                drawCenteredString(
                        Messages.getString("BoardView1.Multiple"), //$NON-NLS-1$
                        p.x, p.y + (int) (51 * scale), font_minefield, g);
            } else if (nbrMfs == 1) {
                Minefield mf = game.getMinefields(c).get(0);
                
                switch (mf.getType()) {
                    case (Minefield.TYPE_CONVENTIONAL):
                        drawCenteredString(
                                Messages.getString("BoardView1.Conventional") //$NON-NLS-1$
                                + mf.getDensity() + ")", //$NON-NLS-1$
                                p.x, p.y + (int) (51 * scale),
                                font_minefield, g);
                        break;
                    case (Minefield.TYPE_INFERNO):
                        drawCenteredString(
                                Messages.getString("BoardView1.Inferno") //$NON-NLS-1$
                                + mf.getDensity() + ")", //$NON-NLS-1$
                                p.x, p.y + (int) (51 * scale),
                                font_minefield, g);
                        break;
                    case (Minefield.TYPE_ACTIVE):
                        drawCenteredString(
                                Messages.getString("BoardView1.Active") //$NON-NLS-1$
                                + mf.getDensity() + ")",  //$NON-NLS-2$
                                p.x, p.y + (int) (51 * scale),
                                font_minefield, g);
                        break;
                    case (Minefield.TYPE_COMMAND_DETONATED):
                        drawCenteredString(
                                Messages.getString("BoardView1.Command-"), //$NON-NLS-1$
                                p.x, p.y + (int) (51 * scale),
                                font_minefield, g);
                        drawCenteredString(
                                Messages.getString("BoardView1.detonated" //$NON-NLS-1$
                                + mf.getDensity() + ")"), //$NON-NLS-1$
                                p.x, p.y + (int) (60 * scale),
                                font_minefield, g);
                        break;
                    case (Minefield.TYPE_VIBRABOMB):
                        drawCenteredString(
                                Messages.getString("BoardView1.Vibrabomb"), //$NON-NLS-1$
                                p.x, p.y + (int) (51 * scale),
                                font_minefield, g);
                        if (mf.getPlayerId() == localPlayer.getId()) {
                            drawCenteredString("(" //$NON-NLS-1$
                                    + mf.getSetting() + ")", //$NON-NLS-1$
                                    p.x, p.y + (int) (60 * scale),
                                    font_minefield, g);
                        }
                        break;
                }
            }
        }
    }

    private void drawCenteredString(String string, int x, int y, Font font,
            Graphics graph) {
        FontMetrics currentMetrics = getFontMetrics(font);
        int stringWidth = currentMetrics.stringWidth(string);

        x += ((hex_size.width - stringWidth) / 2);

        graph.setFont(font);
        graph.drawString(string, x, y);
    }

    /**
     * This method creates an image the size of the entire board (all
     * mapsheets), draws the hexes onto it, and returns that image.
     */
    public BufferedImage getEntireBoardImage() {
        Image entireBoard = createImage(boardSize.width, boardSize.height);
        Graphics2D boardGraph = (Graphics2D)entireBoard.getGraphics();
        if (GUIPreferences.getInstance().getAntiAliasing()){
            boardGraph.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        drawHexes(boardGraph, new Rectangle(boardSize), true);
        boardGraph.dispose();
        return (BufferedImage)entireBoard;
    }

    private void drawHexes(Graphics g, Rectangle view) {
        drawHexes(g, view, false);
    }

    /**
     * Redraws all hexes in the specified rectangle
     */
    private void drawHexes(Graphics g, Rectangle view, boolean saveBoardImage) {
        // only update visible hexes
        int drawX = (int) (view.x / (HEX_WC * scale)) - 1;
        int drawY = (int) (view.y / (HEX_H * scale)) - 1;

        int drawWidth = (int) (view.width / (HEX_WC * scale)) + 3;
        int drawHeight = (int) (view.height / (HEX_H * scale)) + 3;

        /*
        if (!useIsometric()) {
            // clear, if we need to
            // The first column has no hexes in it, so drawHex won't update it
            if (view.x < (HEX_W_4TH * scale)) {
                boardGraph.fillRect(view.x - drawRect.x, view.y - drawRect.y,
                        (int) (HEX_W_4TH * scale) - view.x, view.height);
            }
            if (view.y < (HEX_H_HALF * scale)) {
                boardGraph.fillRect(view.x - drawRect.x, view.y - drawRect.y,
                        view.width, (int) (HEX_H_HALF * scale) - view.y);
            }
            if (view.x > (boardSize.width - view.width - (HEX_W_4TH * scale))) {
                boardGraph.fillRect(Math.min(drawRect.width, boardSize.width)
                        - (int) (HEX_W_4TH * scale), view.y - drawRect.y,
                        (int) (HEX_W_4TH * scale), view.height);
            }
            if (view.y > (boardSize.height - view.height - (HEX_H_HALF * scale))) {
                boardGraph.fillRect(view.x - drawRect.x,
                        Math.min(drawRect.height, boardSize.height)
                                - (int) (HEX_H_HALF * scale), view.width,
                        (int) (HEX_H_HALF * scale));
            }
        }
        */
        // draw some hexes.
        if (useIsometric()) {
            //g.clearRect(view.x, view.y, view.width, view.height);
            // When using isometric rendering, hexes within a given row
            // must be drawn from lowest to highest elevation.
            IBoard board = game.getBoard();
            final int minElev = board.getMinElevation();
            for (int i = 0; i < drawHeight; i++) {
                for (int x = minElev; x <= game.getBoard().getMaxElevation(); x++) {
                    for (int j = 0; j < drawWidth; j++) {
                        Coords c = new Coords(j + drawX, i + drawY);
                        IHex hex = game.getBoard().getHex(c);
                        if ((hex != null) && (hex.getElevation() == x)) {
                            drawHex(c, g, saveBoardImage);
                            drawHexSpritesForHex(c, g,
                                    moveEnvSprites);
                            drawHexSpritesForHex(c, g,
                                    moveModEnvSprites);
                            if ((en_Deployer != null)
                                    && board.isLegalDeployment(c,
                                            en_Deployer.getStartingPos())) {
                                drawHexBorder(getHexLocation(c), g,
                                        Color.yellow);
                            }
                        }
                    }
                }
                for (int k = 0; k < drawWidth; k++) {
                    Coords c = new Coords(k + drawX, i + drawY);
                    IHex hex = game.getBoard().getHex(c);
                    if (hex != null) {
                        drawOrthograph(c, g);
                        if (!saveBoardImage){
                            drawIsometricWreckSpritesForHex(c, g,
                                    isometricWreckSprites);
                            drawIsometricSpritesForHex(c, g, isometricSprites);
                        }
                    }
                }
            }
            if (!saveBoardImage){
                // If we are using Isometric rendering, redraw the entity
                // sprites at 50% transparent so sprites hidden behind hills can
                // still be seen by the user.
                drawIsometricSprites(g, isometricSprites);
            }
        } else {
            // Draw hexes without regard to elevation when
            // not using Isometric, since it does not matter.
            for (int i = 0; i < drawHeight; i++) {
                for (int j = 0; j < drawWidth; j++) {
                    Coords c = new Coords(j + drawX, i + drawY);
                    drawHex(c, g, saveBoardImage);
                }
            }
        }
    }

    /**
     * Draws a hex onto the board buffer. This assumes that drawRect is current,
     * and does not check if the hex is visible.
     */
    private void drawHex(Coords c, Graphics boardGraph,
            boolean saveBoardImage) {
        if (!game.getBoard().contains(c)) {
            return;
        }

        final IHex hex = game.getBoard().getHex(c);
        final Point hexLoc = getHexLocation(c);

        int level = hex.getElevation();

        int depth = hex.depth(false);

        ITerrain basement = hex.getTerrain(Terrains.BLDG_BASEMENT_TYPE);
        if (basement != null) {
            depth = 0;

        }
        int height = Math.max(hex.terrainLevel(Terrains.BLDG_ELEV),
                hex.terrainLevel(Terrains.BRIDGE_ELEV));
        height = Math.max(height, hex.terrainLevel(Terrains.INDUSTRIAL));

        // offset drawing point
        int drawX = hexLoc.x;
        int drawY = hexLoc.y;
        //if (!useIsometric()) {
        //    drawX -= drawRect.x;
        //    drawY -= drawRect.y;
        //}
        // draw picture
        Image baseImage = tileManager.baseFor(hex);
        Image scaledImage = getScaledImage(baseImage,true);

        boardGraph.drawImage(scaledImage, drawX, drawY, this);

        if (tileManager.supersFor(hex) != null) {
            for (Image image : tileManager.supersFor(hex)) {
                scaledImage = getScaledImage(image,true);
                boardGraph.drawImage(scaledImage, drawX, drawY, this);
            }
        }

        if (tileManager.orthoFor(hex) != null) {
            for (Image image : tileManager.orthoFor(hex)) {
                scaledImage = getScaledImage(image,true);
                if (!useIsometric()) {
                    boardGraph.drawImage(scaledImage, drawX, drawY, this);
                }
                // draw a shadow for bridge hex.
                if (useIsometric()
                        && (hex.terrainLevel(Terrains.BRIDGE_ELEV) > 0)) {
                    Image shadow = createShadowMask(scaledImage);

                    shadow = createImage(new FilteredImageSource(
                            shadow.getSource(), new KeyAlphaFilter(TRANSPARENT)));
                    boardGraph.drawImage(shadow, hexLoc.x, hexLoc.y, this);
                }
            }
        }

        if (ecmHexes != null) {
            Integer tint = ecmHexes.get(c);
            if (tint != null) {
                scaledImage = getScaledImage(tileManager.getEcmShade(tint
                        .intValue()),true);
                boardGraph.drawImage(scaledImage, drawX, drawY, this);
            }
        }

        if (GUIPreferences.getInstance().getBoolean(
                GUIPreferences.ADVANCED_DARKEN_MAP_AT_NIGHT)
                && (game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DAY)
                && game.isPositionIlluminated(c) == Game.ILLUMINATED_NONE) {
            scaledImage = getScaledImage(tileManager.getNightFog(),true);
            boardGraph.drawImage(scaledImage, drawX, drawY, this);
        }
        boardGraph.setColor(GUIPreferences.getInstance().getMapTextColor());
        if (game.getBoard().inSpace()) {
            boardGraph.setColor(Color.LIGHT_GRAY);
        }

        // draw special stuff for the hex
        final Collection<SpecialHexDisplay> shdList = game.getBoard()
                .getSpecialHexDisplay(c);
        try {
            if (shdList != null) {
                for (SpecialHexDisplay shd : shdList) {
                    if (shd.drawNow(game.getPhase(), game.getRoundCount(),
                            localPlayer)) {
                        scaledImage = getScaledImage(shd.getType()
                                .getDefaultImage(),true);
                        boardGraph.drawImage(scaledImage, drawX, drawY, this);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal argument exception, probably " +
                    "can't load file.");
            e.printStackTrace();
            drawCenteredString("Loading Error", drawX, drawY
                    + (int) (50 * scale), font_note, boardGraph);
            return;
        }

        // draw hex number
        if (scale >= 0.5) {
            drawCenteredString(c.getBoardNum(), drawX, drawY
                    + (int) (12 * scale), font_hexnum, boardGraph);
        }

        // draw terrain level / water depth / building height
        if (scale > 0.5f) {
            int ypos = 70;
            if (level != 0) {
                drawCenteredString(
                        Messages.getString("BoardView1.LEVEL") + level, //$NON-NLS-1$
                        drawX, drawY + (int) (ypos * scale), font_elev,
                        boardGraph);
                ypos -= 10;
            }
            if (depth != 0) {
                drawCenteredString(
                        Messages.getString("BoardView1.DEPTH") + depth, //$NON-NLS-1$
                        drawX, drawY + (int) (ypos * scale), font_elev,
                        boardGraph);
                ypos -= 10;
            }
            if (height > 0) {
                boardGraph.setColor(GUIPreferences.getInstance().getColor(
                        "AdvancedBuildingTextColor"));
                drawCenteredString(
                        Messages.getString("BoardView1.HEIGHT") + height, //$NON-NLS-1$
                        drawX, drawY + (int) (ypos * scale), font_elev,
                        boardGraph);
                ypos -= 10;
            }
        }

        // draw elevation borders
        boardGraph.setColor(Color.black);
        if (drawElevationLine(c, 0)) {
            final int x1 = drawX + (int) (21 * scale);
            final int x2 = drawX + (int) (62 * scale);
            final int y1 = drawY;
            final int y2 = drawY;

            drawIsometricElevation(c, Color.GRAY, new Point(x2, y2), new Point(
                    x1, y1), 0, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 1)) {
            final int x1 = drawX + (int) (62 * scale);
            final int x2 = drawX + (int) (83 * scale);
            final int y1 = drawY;
            final int y2 = drawY + (int) (35 * scale);

            drawIsometricElevation(c, Color.DARK_GRAY, new Point(x2, y2),
                    new Point(x1, y1), 1, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 2)) {
            final int x1 = drawX + (int) (83 * scale);
            final int x2 = drawX + (int) (62 * scale);
            final int y1 = drawY + (int) (36 * scale);
            final int y2 = drawY + (int) (71 * scale);

            drawIsometricElevation(c, Color.LIGHT_GRAY, new Point(x1, y1),
                    new Point(x2, y2), 2, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 3)) {
            final int x1 = drawX + (int) (62 * scale);
            final int x2 = drawX + (int) (21 * scale);
            final int y1 = drawY + (int) (71 * scale);
            final int y2 = drawY + (int) (71 * scale);

            drawIsometricElevation(c, Color.GRAY, new Point(x2, y2), new Point(
                    x1, y1), 3, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 4)) {
            final int x1 = drawX + (int) (21 * scale);
            final int x2 = drawX;
            final int y1 = drawY + (int) (71 * scale);
            final int y2 = drawY + (int) (36 * scale);

            drawIsometricElevation(c, Color.DARK_GRAY, new Point(x2, y2),
                    new Point(x1, y1), 4, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }
        if (drawElevationLine(c, 5)) {
            final int x1 = drawX;
            final int x2 = drawX + (int) (21 * scale);
            final int y1 = drawY + (int) (35 * scale);
            final int y2 = drawY;

            drawIsometricElevation(c, Color.LIGHT_GRAY, new Point(x1, y1),
                    new Point(x2, y2), 5, boardGraph);
            boardGraph.drawLine(x1, y1, x2, y2);
        }

        fovHighlightingAndDarkening.draw( boardGraph, c, drawX, drawY,
                saveBoardImage);

        // draw mapsheet borders
        if (GUIPreferences.getInstance().getShowMapsheets()) {
            boardGraph.setColor(GUIPreferences.getInstance().getColor(
                    GUIPreferences.ADVANCED_MAPSHEET_COLOR));
            if ((c.x % 16) == 0) {
                // left edge of sheet (edge 4 & 5)
                boardGraph
                        .drawLine(drawX + (int) (21 * scale), drawY
                                + (int) (71 * scale), drawX, drawY
                                + (int) (36 * scale));
                boardGraph.drawLine(drawX, drawY + (int) (35 * scale), drawX
                        + (int) (21 * scale), drawY);
            } else if ((c.x % 16) == 15) {
                // right edge of sheet (edge 1 & 2)
                boardGraph.drawLine(drawX + (int) (62 * scale), drawY, drawX
                        + (int) (83 * scale), drawY + (int) (35 * scale));
                boardGraph.drawLine(drawX + (int) (83 * scale), drawY
                        + (int) (36 * scale), drawX + (int) (62 * scale), drawY
                        + (int) (71 * scale));
            }
            if ((c.y % 17) == 0) {
                // top edge of sheet (edge 0 and possible 1 & 5)
                boardGraph.drawLine(drawX + (int) (21 * scale), drawY, drawX
                        + (int) (62 * scale), drawY);
                if ((c.x % 2) == 0) {
                    boardGraph.drawLine(drawX + (int) (62 * scale), drawY,
                            drawX + (int) (83 * scale), drawY
                                    + (int) (35 * scale));
                    boardGraph.drawLine(drawX, drawY + (int) (35 * scale),
                            drawX + (int) (21 * scale), drawY);
                }
            } else if ((c.y % 17) == 16) {
                // bottom edge of sheet (edge 3 and possible 2 & 4)
                boardGraph.drawLine(drawX + (int) (62 * scale), drawY
                        + (int) (71 * scale), drawX + (int) (21 * scale), drawY
                        + (int) (71 * scale));
                if ((c.x % 2) == 1) {
                    boardGraph.drawLine(drawX + (int) (83 * scale), drawY
                            + (int) (36 * scale), drawX + (int) (62 * scale),
                            drawY + (int) (71 * scale));
                    boardGraph.drawLine(drawX + (int) (21 * scale), drawY
                            + (int) (71 * scale), drawX, drawY
                            + (int) (36 * scale));
                }
            }
            boardGraph.setColor(Color.black);
        }
    }

    /**
     * Draws a orthographic hex onto the board buffer. This assumes that
     * drawRect is current, and does not check if the hex is visible.
     */
    private void drawOrthograph(Coords c, Graphics boardGraph) {
        if (!game.getBoard().contains(c)) {
            return;
        }

        final IHex oHex = game.getBoard().getHex(c);
        final Point oHexLoc = getHexLocation(c);

        int orthX = oHexLoc.x;
        int orthY = (oHexLoc.y - (int) (HEX_ELEV * scale * oHex
                .terrainLevel(Terrains.BRIDGE_ELEV)));
        if (!useIsometric()) {
            orthY = oHexLoc.y;
        }
        if (tileManager.orthoFor(oHex) != null) {
            for (Image image : tileManager.orthoFor(oHex)) {
                Image scaledImage = getScaledImage(image,true);

                // draw orthogonal
                boardGraph.drawImage(scaledImage, orthX, orthY, this);
            }
        }
    }

    final boolean useIsometric() {
        return drawIsometric;
    }

    /**
     * Draws the Isometric elevation for the hex at the given coordinates (c) on
     * the side indicated by the direction (dir). This method only draws a
     * triangle for the elevation, the companion triangle representing the
     * adjacent hex is also needed. The two triangles when drawn together make a
     * complete rectangle representing the complete elevated hex side.
     *
     * By drawing the elevated hex as two separate triangles we avoid clipping
     * problems with other hexes because the lower elevation is rendered before
     * the higher elevation. Thus any hexes that have a higher elevation than
     * the lower hex will overwrite the lower hex.
     *
     * The Triangle for each hex side is formed by points p1, p2, and p3. Where
     * p1 and p2 are the original hex edges, and p3 has the same X value as p1,
     * but the y value has been increased (or decreased) based on the difference
     * in elevation between the given hex and the adjacent hex.
     *
     * @param c
     *            Coordinates of the source hex.
     * @param color
     *            Color to use for the elevation polygons.
     * @param p1
     *            The First point on the edge of the hex.
     * @param p2
     *            The second point on the edge of the hex.
     * @param dir
     *            The side of the hex to have the elevation drawn on.
     * @param boardGraph
     */
    private final void drawIsometricElevation(Coords c, Color color, Point p1,
            Point p2, int dir, Graphics boardGraph) {
        final IHex dest = game.getBoard().getHexInDir(c, dir);
        final IHex src = game.getBoard().getHex(c);

        if (!useIsometric()) {
            return;
        }

        // Pad the polygon size slightly to avoid rounding errors from the scale
        // float.
        int fudge = -1;
        if ((dir == 2) || (dir == 4) || (dir == 3)) {
            fudge = 1;
        }

        final int elev = src.getElevation();
        // If the Destination is null, draw the complete elevation side.
        if ((dest == null) && (elev > 0)
                && ((dir == 2) || (dir == 3) || (dir == 4))) {

            // Determine the depth of the edge that needs to be drawn.
            int height = elev;
            IHex southHex = game.getBoard().getHexInDir(c, 3);
            if ((dir != 3) && (southHex != null)
                    && (elev > southHex.getElevation())) {
                height = elev - southHex.getElevation();
            }

            Polygon p = new Polygon(new int[] { p1.x, p2.x, p2.x, p1.x },
                    new int[] { p1.y + fudge, p2.y + fudge,
                            p2.y + (int) (HEX_ELEV * scale * height),
                            p1.y + (int) (HEX_ELEV * scale * height) }, 4);
            boardGraph.setColor(color);
            boardGraph.drawPolygon(p);
            boardGraph.fillPolygon(p);

            boardGraph.setColor(Color.BLACK);
            if ((dir == 2) || (dir == 4)) {
                boardGraph.drawLine(p1.x, p1.y, p1.x, p1.y
                        + (int) (HEX_ELEV * scale * height));
            }
            return;
        } else if (dest == null) {
            return;
        }

        int delta = elev - dest.getElevation();
        // Don't draw the elevation if there is no exposed edge for the player
        // to see.
        if ((delta == 0)
                || (((dir == 0) || (dir == 1) || (dir == 5)) && (delta > 0))
                || (((dir == 2) || (dir == 3) || (dir == 4)) && (delta < 0))) {
            return;
        }

        if (dir == 1) {
            // Draw a little bit of shadow to improve the 3d isometric effect.
            Polygon shadow1 = new Polygon(new int[] { p1.x, p2.x,
                    p2.x - (int) (HEX_ELEV * scale) }, new int[] { p1.y, p2.y,
                    p2.y }, 3);
            boardGraph.setColor(new Color(0, 0, 0, 0.4f));
            boardGraph.fillPolygon(shadow1);
        }

        if ((dir == 2) || (dir == 3) || (dir == 4)) {

            Point p3 = new Point(p1.x, p1.y + (int) (HEX_ELEV * scale * delta)
                    + fudge);

            Polygon p = new Polygon(new int[] { p1.x, p2.x, p2.x, p1.x },
                    new int[] { p1.y + fudge, p2.y + fudge,
                            p2.y + fudge + (int) (HEX_ELEV * scale * delta),
                            p1.y + fudge + (int) (HEX_ELEV * scale * delta) },
                    4);

            boardGraph.setColor(color);
            boardGraph.drawPolygon(p);
            boardGraph.fillPolygon(p);

            boardGraph.setColor(Color.BLACK);
            if ((dir == 1) || (dir == 2) || (dir == 5) || (dir == 4)) {
                boardGraph.drawLine(p1.x, p1.y, p3.x, p3.y);
            }
        }
    }

    /**
     * Returns true if an elevation line should be drawn between the starting
     * hex and the hex in the direction specified. Results should be transitive,
     * that is, if a line is drawn in one direction, it should be drawn in the
     * opposite direction as well.
     */
    private final boolean drawElevationLine(Coords src, int direction) {
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHexInDir(src, direction);
        if ((destHex == null) && (srcHex.getElevation() != 0)) {
            return true;
        } else if (destHex == null) {
            return false;
        } else if (srcHex.getElevation() != destHex.getElevation()) {
            return true;
        } else {
            return (srcHex.floor() != destHex.floor());
        }
    }

    /**
     * Returns the absolute position of the upper-left hand corner of the hex
     * graphic
     */
    private Point getHexLocation(int x, int y) {
        float elevationAdjust = 0.0f;

        IHex hex = game.getBoard().getHex(x, y);
        if ((hex != null) && useIsometric()) {
            int level = hex.getElevation();
            if (level != 0) {
                elevationAdjust = level * HEX_ELEV * scale * -1.0f;
            }
        }
        int ypos = (y * (int) (HEX_H * scale))
                + ((x & 1) == 1 ? (int) ((HEX_H / 2) * scale) : 0);
        return new Point(x * (int) (HEX_WC * scale), ypos
                + (int) elevationAdjust);
    }

    Point getHexLocation(Coords c) {
        return getHexLocation(c.x, c.y);
    }

    /**
     * Returns the absolute position of the centre of the hex graphic
     */
    private Point getCentreHexLocation(int x, int y) {
        Point p = getHexLocation(x, y);
        p.x += ((HEX_W / 2) * scale);
        p.y += ((HEX_H / 2) * scale);
        return p;
    }

    private Point getCentreHexLocation(Coords c) {
        return getCentreHexLocation(c.x, c.y);
    }

    public void drawRuler(Coords s, Coords e, Color sc, Color ec) {
        rulerStart = s;
        rulerEnd = e;
        rulerStartColor = sc;
        rulerEndColor = ec;

        repaint();
    }

    /**
     * Returns the coords at the specified point
     */
    Coords getCoordsAt(Point p) {
        // We must account for the board translation to add padding
        p.x -= HEX_W;
        p.y -= HEX_H;
        final int x = (p.x) / (int) (HEX_WC * scale);
        final int y = ((p.y) - ((x & 1) == 1 ? (int) ((HEX_H / 2) * scale) : 0))
                / (int) (HEX_H * scale);
        Coords cOriginal = new Coords(x, y);
        if (useIsometric()) {
            // When using isometric rendering, a lower hex can obscure the
            // normal hex.
            // Iterate over all hexes from highest to lowest, looking for a hex
            // that
            // falls within the selected mouse click point.
            final int minElev = Math.min(0, game.getBoard().getMinElevation());
            final int maxElev = Math.max(0, game.getBoard().getMaxElevation());
            final int delta = (int) Math
                    .ceil(((double) maxElev - minElev) / 3.0f);
            final int minHexSpan = Math.max(y - delta, 0);
            final int maxHexSpan = Math.min(y + delta, game.getBoard()
                    .getHeight());
            for (int elev = maxElev; elev >= minElev; elev--) {
                for (int i = minHexSpan; i <= maxHexSpan; i++) {
                    Coords c1 = new Coords(x, i);
                    Point pAlt = getHexLocation(c1);
                    IHex hexAlt = game.getBoard().getHex(c1);
                    if ((p.y > pAlt.y) && (p.y < (pAlt.y + HEX_H))
                            && (hexAlt != null)
                            && (hexAlt.getElevation() == elev)) {
                        // This hex's location falls under the point the user
                        // selected.
                        return c1;
                    }
                }
            }
        }
        return cOriginal;

    }

    public void redrawMovingEntity(Entity entity, Coords position, int facing,
            int elevation) {
        Integer entityId = new Integer(entity.getId());
        EntitySprite sprite = entitySpriteIds.get(entityId);
        IsometricSprite isoSprite = isometricSpriteIds.get(entityId);

        ArrayList<EntitySprite> newSprites;
        ArrayList<IsometricSprite> isoSprites;
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds;
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds;

        if (sprite != null) {
            newSprites = new ArrayList<EntitySprite>(entitySprites);
            newSpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>(
                    entitySpriteIds);

            newSprites.remove(sprite);

            entitySprites = newSprites;
            entitySpriteIds = newSpriteIds;
        }

        if (isoSprite != null) {
            isoSprites = new ArrayList<IsometricSprite>(isometricSprites);
            newIsoSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>(
                    isometricSpriteIds);

            isoSprites.remove(isoSprite);

            isometricSprites = isoSprites;
            isometricSpriteIds = newIsoSpriteIds;
        }

        MovingEntitySprite mSprite = movingEntitySpriteIds.get(entityId);
        ArrayList<MovingEntitySprite> newMovingSprites = new ArrayList<MovingEntitySprite>(
                movingEntitySprites);
        HashMap<Integer, MovingEntitySprite> newMovingSpriteIds = new HashMap<Integer, MovingEntitySprite>(
                movingEntitySpriteIds);

        if (mSprite != null) {
            newMovingSprites.remove(mSprite);
        }

        if (entity.getPosition() != null) {
            mSprite = new MovingEntitySprite(this, entity, position, facing,
                    elevation);
            newMovingSprites.add(mSprite);
            newMovingSpriteIds.put(entityId, mSprite);
        }

        movingEntitySprites = newMovingSprites;
        movingEntitySpriteIds = newMovingSpriteIds;
    }

    public boolean isMovingUnits() {
        return movingUnits.size() > 0;
    }

    /**
     * Clears the sprite for an entity and prepares it to be re-drawn. Replaces
     * the old sprite with the new! Try to prevent annoying
     * ConcurrentModificationExceptions
     */
    public void redrawEntity(Entity entity) {
        redrawEntity(entity, null);
    }

    /**
     * Clears the sprite for an entity and prepares it to be re-drawn. Replaces
     * the old sprite with the new! Takes a reference to the Entity object
     * before changes, in case it contained important state information, like
     * Dropships taking off (airborne dropships lose their secondary hexes). Try
     * to prevent annoying ConcurrentModificationExceptions
     */
    public void redrawEntity(Entity entity, Entity oldEntity) {
        Integer entityId = new Integer(entity.getId());
        if (oldEntity == null) {
            oldEntity = entity;
        }

        if (entity.getPosition() == null) {
            for (Iterator<EntitySprite> spriteIter = entitySprites.iterator(); spriteIter
                    .hasNext();) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            for (Iterator<EntitySprite> spriteIter = entitySpriteIds.values()
                    .iterator(); spriteIter.hasNext();) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            for (Iterator<IsometricSprite> spriteIter = isometricSprites
                    .iterator(); spriteIter.hasNext();) {
                IsometricSprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            for (Iterator<IsometricSprite> spriteIter = isometricSpriteIds
                    .values().iterator(); spriteIter.hasNext();) {
                IsometricSprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
        }

        ArrayList<EntitySprite> newSprites = new ArrayList<EntitySprite>(
                entitySprites);
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>(
                entitySpriteIds);
        ArrayList<IsometricSprite> isoSprites = new ArrayList<IsometricSprite>(
                isometricSprites);
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>(
                isometricSpriteIds);

        ArrayList<Integer> temp = new ArrayList<Integer>();
        temp.add(entityId);
        temp.add(-1);
        EntitySprite sprite = entitySpriteIds.get(temp);
        IsometricSprite isoSprite = isometricSpriteIds.get(temp);
        if (sprite != null) {
            newSprites.remove(sprite);
        }
        if (isoSprite != null) {
            isoSprites.remove(isoSprite);
        }
        for (int secondaryPos : oldEntity.getSecondaryPositions().keySet()) {
            temp = new ArrayList<Integer>();
            temp.add(entityId);
            temp.add(secondaryPos);
            sprite = entitySpriteIds.get(temp);
            if (sprite != null) {
                newSprites.remove(sprite);
            }
            isoSprite = isometricSpriteIds.get(temp);
            if (isoSprite != null) {
                isoSprites.remove(isoSprite);
            }

        }

        Coords position = entity.getPosition();
        if (position != null) {
            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                sprite = new EntitySprite(this, entity, -1);
                newSprites.add(sprite);
                temp = new ArrayList<Integer>();
                temp.add(entityId);
                temp.add(-1);
                newSpriteIds.put(temp, sprite);
            } else { // Add all secondary position sprites, which includes a
                     // sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    sprite = new EntitySprite(this, entity, secondaryPos);
                    newSprites.add(sprite);
                    temp = new ArrayList<Integer>();
                    temp.add(entityId);
                    temp.add(secondaryPos);
                    newSpriteIds.put(temp, sprite);
                }
            }

            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                isoSprite = new IsometricSprite(this, entity, -1);
                isoSprites.add(isoSprite);
                temp = new ArrayList<Integer>();
                temp.add(entityId);
                temp.add(-1);
                newIsoSpriteIds.put(temp, isoSprite);
            } else { // Add all secondary position sprites, which includes a
                     // sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    isoSprite = new IsometricSprite(this, entity, secondaryPos);
                    isoSprites.add(isoSprite);
                    temp = new ArrayList<Integer>();
                    temp.add(entityId);
                    temp.add(secondaryPos);
                    newIsoSpriteIds.put(temp, isoSprite);
                }
            }
        }

        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;
        isometricSprites = isoSprites;
        isometricSpriteIds = newIsoSpriteIds;

        for (Iterator<C3Sprite> i = c3Sprites.iterator(); i.hasNext();) {
            final C3Sprite c3sprite = i.next();
            if ((c3sprite.entityId == entity.getId())
                    || (c3sprite.masterId == entity.getId())) {
                i.remove();
            }
        }
        // WOR
        if (entity.hasC3() || entity.hasC3i() || entity.hasActiveNovaCEWS()) {
            addC3Link(entity);
        }

        for (Iterator<FlyOverSprite> i = flyOverSprites.iterator(); i.hasNext();) {
            final FlyOverSprite flyOverSprite = i.next();
            if (flyOverSprite.getEntityId() == entity.getId()) {
                i.remove();
            }
        }
        if (entity.isAirborne() && (entity.getPassedThrough().size() > 1)) {
            addFlyOverPath(entity);
        }

        scheduleRedraw();
    }

    /**
     * Clears all old entity sprites out of memory and sets up new ones.
     */
    void redrawAllEntities() {
        ArrayList<EntitySprite> newSprites = new ArrayList<EntitySprite>(
                game.getNoOfEntities());
        ArrayList<IsometricSprite> newIsometricSprites = new ArrayList<IsometricSprite>(
                game.getNoOfEntities());
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds = new HashMap<ArrayList<Integer>, EntitySprite>(
                game.getNoOfEntities());
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds = new HashMap<ArrayList<Integer>, IsometricSprite>(
                game.getNoOfEntities());

        ArrayList<WreckSprite> newWrecks = new ArrayList<WreckSprite>();
        ArrayList<IsometricWreckSprite> newIsometricWrecks = new ArrayList<IsometricWreckSprite>();

        Enumeration<Entity> e = game.getWreckedEntities();
        while (e.hasMoreElements()) {
            Entity entity = e.nextElement();
            if (!(entity instanceof Infantry) && (entity.getPosition() != null)) {
                WreckSprite ws;
                IsometricWreckSprite iws;
                if (entity.getSecondaryPositions().isEmpty()) {
                    ws = new WreckSprite(this, entity, -1);
                    newWrecks.add(ws);
                    iws = new IsometricWreckSprite(this, entity, -1);
                    newIsometricWrecks.add(iws);
                } else {
                    for (int secondaryPos : entity.getSecondaryPositions()
                            .keySet()) {
                        ws = new WreckSprite(this, entity, secondaryPos);
                        newWrecks.add(ws);
                        iws = new IsometricWreckSprite(this, entity, secondaryPos);
                        newIsometricWrecks.add(iws);
                    }
                }
            }
        }

        clearC3Networks();
        clearFlyOverPaths();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = i.nextElement();
            if (entity.getPosition() == null) {
                continue;
            }
            if ((localPlayer != null)
                    && game.getOptions().booleanOption("double_blind")
                    && entity.getOwner().isEnemyOf(localPlayer)
                    && !entity.isVisibleToEnemy()
                    && !entity.isDetectedByEnemy()){
                continue;
            }
            if (entity.getSecondaryPositions().isEmpty()) {
                EntitySprite sprite = new EntitySprite(this, entity, -1);
                newSprites.add(sprite);
                ArrayList<Integer> temp = new ArrayList<Integer>();
                temp.add(entity.getId());
                temp.add(-1);
                newSpriteIds.put(temp, sprite);
                IsometricSprite isosprite = new IsometricSprite(this, entity, -1);
                newIsometricSprites.add(isosprite);
                temp = new ArrayList<Integer>();
                temp.add(entity.getId());
                temp.add(-1);
                newIsoSpriteIds.put(temp, isosprite);
            } else {
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    EntitySprite sprite = new EntitySprite(this, entity, secondaryPos);
                    newSprites.add(sprite);
                    ArrayList<Integer> temp = new ArrayList<Integer>();
                    temp.add(entity.getId());
                    temp.add(secondaryPos);
                    newSpriteIds.put(temp, sprite);

                    IsometricSprite isosprite = new IsometricSprite(this, entity,
                            secondaryPos);
                    newIsometricSprites.add(isosprite);
                    temp = new ArrayList<Integer>();
                    temp.add(entity.getId());
                    temp.add(secondaryPos);
                    newIsoSpriteIds.put(temp, isosprite);
                }
            }
            // WOR
            if (entity.hasC3() || entity.hasC3i() || entity.hasActiveNovaCEWS()) {
                addC3Link(entity);
            }

            if (entity.isAirborne() && (entity.getPassedThrough().size() > 1)) {
                addFlyOverPath(entity);
            }
        }

        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;

        isometricSprites = newIsometricSprites;
        isometricSpriteIds = newIsoSpriteIds;

        wreckSprites = newWrecks;
        isometricWreckSprites = newIsometricWrecks;

        scheduleRedraw();
    }
    
    private void redrawAllFlares() {
        flareSprites.clear();
        for (Flare f : game.getFlares()) {
            flareSprites.add(new FlareSprite(this, f));
        }
    }
    
    public Image getFlareImage() {
        return flareImage;
    }

    /**
     * Moves the cursor to the new position, or hides it, if newPos is null
     */
    private void moveCursor(CursorSprite cursor, Coords newPos) {
        final Rectangle oldBounds = new Rectangle(cursor.getBounds());
        if (newPos != null) {
            cursor.setHexLocation(newPos);
        } else {
            cursor.setOffScreen();
        }
        // repaint affected area
        repaint(oldBounds);
        repaint(cursor.getBounds());
    }

    public void centerOnHex(Coords c) {
        if (null == c) {
            return;
        }
        // the scrollbars auto-correct if we try to set a value that's out of
        // bounds
        Point hexPoint = getCentreHexLocation(c);
        JScrollBar vscroll = scrollpane.getVerticalScrollBar();
        vscroll.setValue(hexPoint.y - (vscroll.getVisibleAmount() / 2));
        JScrollBar hscroll = scrollpane.getHorizontalScrollBar();
        hscroll.setValue(hexPoint.x - (hscroll.getVisibleAmount() / 2));
        repaint();
    }

    /**
     * Clears the old movement data and draws the new. Since it's less expensive
     * to check for and reuse old step sprites than to make a whole new one, we
     * do that.
     */
    public void drawMovementData(Entity entity, MovePath md) {
        ArrayList<StepSprite> temp = pathSprites;
        MoveStep previousStep = null;

        clearMovementData();

        // Nothing to do if we don't have a MovePath
        if (md == null) {
            return;
        }
        // need to update the movement sprites based on the move path for this
        // entity
        // only way to do this is to clear and refresh (seems wasteful)

        // first get the color for the vector
        Color col = Color.blue;
        if (md.getLastStep() != null) {
            switch (md.getLastStep().getMovementType()) {
                case MOVE_RUN:
                case MOVE_VTOL_RUN:
                case MOVE_OVER_THRUST:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveRunColor");
                    break;
                case MOVE_SPRINT:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveSprintColor");
                    break;
                case MOVE_JUMP:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveJumpColor");
                    break;
                case MOVE_ILLEGAL:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveIllegalColor");
                    break;
                default:
                    col = GUIPreferences.getInstance().getColor(
                            "AdvancedMoveDefaultColor");
                    break;
            }
        }

        refreshMoveVectors(entity, md, col);

        for (Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            // check old movement path for reusable step sprites
            boolean found = false;
            for (StepSprite sprite : temp) {
                if (sprite.getStep().canReuseSprite(step)
                        && !(entity instanceof Aero)) {
                    pathSprites.add(sprite);
                    found = true;
                }
            }
            if (!found) {
                if ((null != previousStep)
                        && ((step.getType() == MoveStepType.UP)
                                || (step.getType() == MoveStepType.DOWN)
                                || (step.getType() == MoveStepType.ACC)
                                || (step.getType() == MoveStepType.DEC)
                                || (step.getType() == MoveStepType.ACCN) || (step
                                .getType() == MoveStepType.DECN))) {
                    // Mark the previous elevation change sprite hidden
                    // so that we can draw a new one in it's place without
                    // having overlap.
                    pathSprites.get(pathSprites.size() - 1).hidden = true;
                }

                // for advanced movement, we always need to hide prior
                // because costs will overlap and we only want the current
                // facing
                if ((previousStep != null) && game.useVectorMove()) {
                    pathSprites.get(pathSprites.size() - 1).hidden = true;
                }

                pathSprites.add(new StepSprite(this, step));
            }
            previousStep = step;
        }
        repaint(100);
    }

    /**
     * Clears current movement data from the screen
     */
    public void clearMovementData() {
        pathSprites = new ArrayList<StepSprite>();
        repaint();
        refreshMoveVectors();
    }

    public void setFiringSolutions(Entity attacker,
            Hashtable<Integer, ToHitData> firingSolutions) {

        clearFiringSolutionData();
        if (firingSolutions == null) {
            return;
        }
        for (ToHitData thd : firingSolutions.values()) {
            FiringSolutionSprite sprite = new FiringSolutionSprite(
                    this, thd.getValue(), thd.getRange(), thd.getLocation());
            firingSprites.add(sprite);
        }
    }

    public void clearFiringSolutionData() {
        firingSprites.clear();
        repaint();
    }

    public void setMovementEnvelope(Hashtable<Coords, Integer> mvEnvData,
            int walk, int run, int jump, int gear) {
        clearMovementEnvelope();

        if (mvEnvData == null) {
            return;
        }

        GUIPreferences guip = GUIPreferences.getInstance();
        for (Coords loc : mvEnvData.keySet()) {
            Color spriteColor = null;
            if (gear == MovementDisplay.GEAR_JUMP) {
                if (mvEnvData.get(loc) <= jump) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_JUMP_COLOR);
                }
            } else {
                if (mvEnvData.get(loc) <= walk) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
                } else if (mvEnvData.get(loc) <= run) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_RUN_COLOR);
                } else {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_SPRINT_COLOR);
                }
            }
            if (spriteColor != null) {
                MovementEnvelopeSprite mvSprite = new MovementEnvelopeSprite(
                        this, spriteColor, loc);
                moveEnvSprites.add(mvSprite);
            }
        }

    }

    public void setMovementModifierEnvelope(Collection<MovePath> movePaths){
        moveModEnvSprites.clear();
        for (MovePath mp : movePaths)
            moveModEnvSprites.add(new MovementModifierEnvelopeSprite(this, mp));
    }

    public void clearMovementEnvelope() {
        moveEnvSprites.clear();
        moveModEnvSprites.clear();
        repaint();
    }

    public void setLocalPlayer(IPlayer p) {
        localPlayer = p;
    }

    public IPlayer getLocalPlayer() {
        return localPlayer;
    }

    /**
     * Specifies that this should mark the deployment hexes for a player. If the
     * player is set to null, no hexes will be marked.
     */
    public void markDeploymentHexesFor(Entity ce) {
        en_Deployer = ce;
        repaint(100);
    }

    /**
     * Returns the entity that is currently being deployed
     */
    public Entity getDeployingEntity() {
        return en_Deployer;
    }

    /**
     * add a fly over path to the sprite list
     */
    public void addFlyOverPath(Entity e) {
        if (e.getPosition() == null) {
            return;
        }

        flyOverSprites.add(new FlyOverSprite(this, e));
    }

    /**
     * Adds a c3 line to the sprite list.
     */
    public void addC3Link(Entity e) {
        if (e.getPosition() == null) {
            return;
        }

        if (e.hasC3i()) {
            for (Enumeration<Entity> i = game.getEntities(); i
                    .hasMoreElements();) {
                final Entity fe = i.nextElement();
                if (fe.getPosition() == null) {
                    return;
                }
                if (e.onSameC3NetworkAs(fe)
                        && !fe.equals(e)
                        && !Compute.isAffectedByECM(e, e.getPosition(),
                                fe.getPosition())) {
                    c3Sprites.add(new C3Sprite(this, e, fe));
                }
            }
        } else if (e.hasActiveNovaCEWS()) {
            // WOR Nova CEWS
            for (Enumeration<Entity> i = game.getEntities(); i
                    .hasMoreElements();) {
                final Entity fe = i.nextElement();
                if (fe.getPosition() == null) {
                    return;
                }
                if (e.onSameC3NetworkAs(fe)
                        && !fe.equals(e)
                        && !Compute.isAffectedByNovaECM(e, e.getPosition(),
                                fe.getPosition())) {
                    c3Sprites.add(new C3Sprite(this, e, fe));
                }
            }
        } else if (e.getC3Master() != null) {
            Entity eMaster = e.getC3Master();
            if (eMaster.getPosition() == null) {
                return;
            }

            // ECM cuts off the network
            boolean blocked = false;
            if (e.hasBoostedC3() && eMaster.hasBoostedC3()) {
                blocked = Compute.isAffectedByAngelECM(e, e.getPosition(),
                        eMaster.getPosition())
                        || Compute.isAffectedByAngelECM(eMaster,
                                eMaster.getPosition(), eMaster.getPosition());
            } else {
                blocked = Compute.isAffectedByECM(e, e.getPosition(),
                        eMaster.getPosition())
                        || Compute.isAffectedByECM(eMaster,
                                eMaster.getPosition(), eMaster.getPosition());
            }

            if (!blocked) {
                c3Sprites.add(new C3Sprite(this, e, e.getC3Master()));
            }
        }
    }

    /**
     * Adds an attack to the sprite list.
     */
    public synchronized void addAttack(AttackAction aa) {
        // do not make a sprite unless we're aware of both entities
        // this is not a great solution but better than a crash
        Entity ae = game.getEntity(aa.getEntityId());
        Targetable t = game.getTarget(aa.getTargetType(), aa.getTargetId());
        if ((ae == null) || (t == null)
                || (t.getTargetType() == Targetable.TYPE_INARC_POD)
                || (t.getPosition() == null) || (ae.getPosition() == null)) {
            return;
        }
        repaint(100);
        for (AttackSprite sprite : attackSprites) {
            // can we just add this attack to an existing one?
            if ((sprite.getEntityId() == aa.getEntityId())
                    && (sprite.getTargetId() == aa.getTargetId())) {
                // use existing attack, but add this weapon
                if (aa instanceof WeaponAttackAction) {
                    WeaponAttackAction waa = (WeaponAttackAction) aa;
                    if (aa.getTargetType() != Targetable.TYPE_HEX_ARTILLERY) {
                        sprite.addWeapon(waa);
                    } else if (waa.getEntity(game).getOwner().getId() == localPlayer
                            .getId()) {
                        sprite.addWeapon(waa);
                    }
                }
                if (aa instanceof KickAttackAction) {
                    sprite.addWeapon((KickAttackAction) aa);
                }
                if (aa instanceof PunchAttackAction) {
                    sprite.addWeapon((PunchAttackAction) aa);
                }
                if (aa instanceof PushAttackAction) {
                    sprite.addWeapon((PushAttackAction) aa);
                }
                if (aa instanceof ClubAttackAction) {
                    sprite.addWeapon((ClubAttackAction) aa);
                }
                if (aa instanceof ChargeAttackAction) {
                    sprite.addWeapon((ChargeAttackAction) aa);
                }
                if (aa instanceof DfaAttackAction) {
                    sprite.addWeapon((DfaAttackAction) aa);
                }
                if (aa instanceof ProtomechPhysicalAttackAction) {
                    sprite.addWeapon((ProtomechPhysicalAttackAction) aa);
                }
                if (aa instanceof SearchlightAttackAction) {
                    sprite.addWeapon((SearchlightAttackAction) aa);
                }
                return;
            }
        }
        // no re-use possible, add a new one
        // don't add a sprite for an artillery attack made by the other player
        if (aa instanceof WeaponAttackAction) {
            WeaponAttackAction waa = (WeaponAttackAction) aa;
            if (aa.getTargetType() != Targetable.TYPE_HEX_ARTILLERY) {
                attackSprites.add(new AttackSprite(this, aa));
            } else if (waa.getEntity(game).getOwner().getId() == localPlayer
                    .getId()) {
                attackSprites.add(new AttackSprite(this, aa));
            }
        } else {
            attackSprites.add(new AttackSprite(this, aa));
        }
    }

    /** Removes all attack sprites from a certain entity */
    public synchronized void removeAttacksFor(Entity e) {
        if (e == null) {
            return;
        }
        int entityId = e.getId();
        for (Iterator<AttackSprite> i = attackSprites.iterator(); i.hasNext();) {
            AttackSprite sprite = i.next();
            if (sprite.getEntityId() == entityId) {
                i.remove();
            }
        }
        repaint(100);
    }

    /**
     * Clears out all attacks and re-adds the ones in the current game.
     */
    public void refreshAttacks() {
        clearAllAttacks();
        for (Enumeration<EntityAction> i = game.getActions(); i
                .hasMoreElements();) {
            EntityAction ea = i.nextElement();
            if (ea instanceof AttackAction) {
                addAttack((AttackAction) ea);
            }
        }
        for (Enumeration<AttackAction> i = game.getCharges(); i
                .hasMoreElements();) {
            EntityAction ea = i.nextElement();
            if (ea instanceof PhysicalAttackAction) {
                addAttack((AttackAction) ea);
            }
        }
        repaint(100);
    }

    public void refreshMoveVectors() {
        clearAllMoveVectors();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity e = i.nextElement();
            if (e.getPosition() != null) {
                movementSprites.add(new MovementSprite(this, e, e.getVectors(),
                        Color.gray, false));
            }
        }
    }

    public void refreshMoveVectors(Entity en, MovePath md, Color col) {
        clearAllMoveVectors();
        // same as normal but when I find the active entity I used the MovePath
        // to get vector
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity e = i.nextElement();
            if (e.getPosition() != null) {
                if ((en != null) && (e.getId() == en.getId())) {
                    movementSprites.add(new MovementSprite(this, e, md
                            .getFinalVectors(), col, true));
                } else {
                    movementSprites.add(new MovementSprite(this, e, e.getVectors(),
                            col, false));
                }
            }
        }
    }

    public void clearC3Networks() {
        c3Sprites.clear();
    }

    public void clearFlyOverPaths() {
        flyOverSprites.clear();
    }

    /**
     * Clears out all attacks that were being drawn
     */
    public void clearAllAttacks() {
        attackSprites.clear();
    }

    /**
     * Clears out all movement vectors that were being drawn
     */
    public void clearAllMoveVectors() {
        movementSprites.clear();
    }

    protected void firstLOSHex(Coords c) {
        if (useLOSTool) {
            moveCursor(secondLOSSprite, null);
            moveCursor(firstLOSSprite, c);
        }
    }

    protected void secondLOSHex(Coords c2, Coords c1) {
        if (useLOSTool) {

            Entity ae = chooseEntity(c1);
            Entity te = chooseEntity(c2);

            StringBuffer message = new StringBuffer();
            LosEffects le;
            if ((ae == null) || (te == null)) {
                boolean mechInFirst = GUIPreferences.getInstance()
                        .getMechInFirst();
                boolean mechInSecond = GUIPreferences.getInstance()
                        .getMechInSecond();
                LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
                ai.attackPos = c1;
                ai.targetPos = c2;
                ai.attackHeight = mechInFirst ? 1 : 0;
                ai.targetHeight = mechInSecond ? 1 : 0;
                ai.targetIsMech = mechInSecond;
                ai.attackerIsMech = mechInFirst;
                ai.attackAbsHeight = game.getBoard().getHex(c1).floor()
                        + ai.attackHeight;
                ai.targetAbsHeight = game.getBoard().getHex(c2).floor()
                        + ai.targetHeight;
                le = LosEffects.calculateLos(game, ai);
                message.append(Messages
                        .getString(
                                "BoardView1.Attacker", new Object[] { //$NON-NLS-1$
                                        mechInFirst ? Messages
                                                .getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech"), //$NON-NLS-1$ //$NON-NLS-2$
                                        c1.getBoardNum() }));
                message.append(Messages
                        .getString(
                                "BoardView1.Target", new Object[] { //$NON-NLS-1$
                                        mechInSecond ? Messages
                                                .getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech"), //$NON-NLS-1$ //$NON-NLS-2$
                                        c2.getBoardNum() }));
            } else {
                le = LosEffects.calculateLos(game, ae.getId(), te);
                message.append(Messages.getString(
                        "BoardView1.Attacker", new Object[] { //$NON-NLS-1$
                        ae.getDisplayName(), c1.getBoardNum() }));
                message.append(Messages.getString(
                        "BoardView1.Target", new Object[] { //$NON-NLS-1$
                        te.getDisplayName(), c2.getBoardNum() }));
            }
            // Check to see if LoS is blocked
            if (!le.canSee()) {
                message.append(Messages.getString("BoardView1.LOSBlocked",
                        new Object[] { //$NON-NLS-1$
                        new Integer(c1.distance(c2)) }));
                ToHitData thd = le.losModifiers(game);
                message.append("\t" + thd.getDesc() + "\n");
            } else {
                message.append(Messages.getString("BoardView1.LOSNotBlocked",
                        new Object[] { //$NON-NLS-1$
                        new Integer(c1.distance(c2)) }));
                if (le.getHeavyWoods() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.HeavyWoods", new Object[] { //$NON-NLS-1$
                            new Integer(le.getHeavyWoods()) }));
                }
                if (le.getLightWoods() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.LightWoods", new Object[] { //$NON-NLS-1$
                            new Integer(le.getLightWoods()) }));
                }
                if (le.getLightSmoke() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.LightSmoke", new Object[] { //$NON-NLS-1$
                            new Integer(le.getLightSmoke()) }));
                }
                if (le.getHeavySmoke() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.HeavySmoke", new Object[] { //$NON-NLS-1$
                            new Integer(le.getHeavySmoke()) }));
                }
                if (le.isTargetCover() && le.canSee()) {
                    message.append(Messages
                            .getString(
                                    "BoardView1.TargetPartialCover", new Object[] { //$NON-NLS-1$
                                    LosEffects.getCoverName(
                                            le.getTargetCover(), true) }));
                }
                if (le.isAttackerCover() && le.canSee()) {
                    message.append(Messages.getString(
                            "BoardView1.AttackerPartialCover", new Object[] { //$NON-NLS-1$
                            LosEffects.getCoverName(le.getAttackerCover(),
                                    false) }));
                }
            }
            JOptionPane.showMessageDialog(getRootPane(), message.toString(),
                    Messages.getString("BoardView1.LOSTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Initializes the various overlay polygons with their vertices.
     */
    public void initPolys() {
        // hex polygon
        hexPoly = new Polygon();
        hexPoly.addPoint(21, 0);
        hexPoly.addPoint(62, 0);
        hexPoly.addPoint(83, 35);
        hexPoly.addPoint(83, 36);
        hexPoly.addPoint(62, 71);
        hexPoly.addPoint(21, 71);
        hexPoly.addPoint(0, 36);
        hexPoly.addPoint(0, 35);

        // facing polygons
        facingPolys = new Polygon[6];
        facingPolys[0] = new Polygon();
        facingPolys[0].addPoint(41, 3);
        facingPolys[0].addPoint(38, 6);
        facingPolys[0].addPoint(45, 6);
        facingPolys[0].addPoint(42, 3);
        facingPolys[1] = new Polygon();
        facingPolys[1].addPoint(69, 17);
        facingPolys[1].addPoint(64, 17);
        facingPolys[1].addPoint(68, 23);
        facingPolys[1].addPoint(70, 19);
        facingPolys[2] = new Polygon();
        facingPolys[2].addPoint(69, 53);
        facingPolys[2].addPoint(68, 49);
        facingPolys[2].addPoint(64, 55);
        facingPolys[2].addPoint(68, 54);
        facingPolys[3] = new Polygon();
        facingPolys[3].addPoint(41, 68);
        facingPolys[3].addPoint(38, 65);
        facingPolys[3].addPoint(45, 65);
        facingPolys[3].addPoint(42, 68);
        facingPolys[4] = new Polygon();
        facingPolys[4].addPoint(15, 53);
        facingPolys[4].addPoint(18, 54);
        facingPolys[4].addPoint(15, 48);
        facingPolys[4].addPoint(14, 52);
        facingPolys[5] = new Polygon();
        facingPolys[5].addPoint(13, 19);
        facingPolys[5].addPoint(15, 23);
        facingPolys[5].addPoint(19, 17);
        facingPolys[5].addPoint(17, 17);

        // movement polygons
        movementPolys = new Polygon[8];
        movementPolys[0] = new Polygon();
        movementPolys[0].addPoint(41, 65);
        movementPolys[0].addPoint(38, 68);
        movementPolys[0].addPoint(45, 68);
        movementPolys[0].addPoint(42, 65);
        movementPolys[1] = new Polygon();
        movementPolys[1].addPoint(17, 48);
        movementPolys[1].addPoint(12, 48);
        movementPolys[1].addPoint(16, 54);
        movementPolys[1].addPoint(17, 49);
        movementPolys[2] = new Polygon();
        movementPolys[2].addPoint(18, 19);
        movementPolys[2].addPoint(17, 15);
        movementPolys[2].addPoint(13, 21);
        movementPolys[2].addPoint(17, 20);
        movementPolys[3] = new Polygon();
        movementPolys[3].addPoint(41, 6);
        movementPolys[3].addPoint(38, 3);
        movementPolys[3].addPoint(45, 3);
        movementPolys[3].addPoint(42, 6);
        movementPolys[4] = new Polygon();
        movementPolys[4].addPoint(67, 15);
        movementPolys[4].addPoint(66, 19);
        movementPolys[4].addPoint(67, 20);
        movementPolys[4].addPoint(71, 20);
        movementPolys[5] = new Polygon();
        movementPolys[5].addPoint(69, 55);
        movementPolys[5].addPoint(66, 50);
        movementPolys[5].addPoint(67, 49);
        movementPolys[5].addPoint(72, 48);

        movementPolys[6] = new Polygon(); // up arrow with tail
        movementPolys[6].addPoint(35, 44);
        movementPolys[6].addPoint(30, 49);
        movementPolys[6].addPoint(33, 49);
        movementPolys[6].addPoint(33, 53);
        movementPolys[6].addPoint(38, 53);
        movementPolys[6].addPoint(38, 49);
        movementPolys[6].addPoint(41, 49);
        movementPolys[6].addPoint(36, 44);
        movementPolys[7] = new Polygon(); // down arrow with tail
        movementPolys[7].addPoint(34, 53);
        movementPolys[7].addPoint(29, 48);
        movementPolys[7].addPoint(32, 48);
        movementPolys[7].addPoint(32, 44);
        movementPolys[7].addPoint(37, 44);
        movementPolys[7].addPoint(37, 48);
        movementPolys[7].addPoint(40, 48);
        movementPolys[7].addPoint(35, 53);
    }

    synchronized boolean doMoveUnits(long idleTime) {
        boolean movingSomething = false;

        if (movingUnits.size() > 0) {

            moveWait += idleTime;

            if (moveWait > GUIPreferences.getInstance().getInt(
                    "AdvancedMoveStepDelay")) {

                ArrayList<MovingUnit> spent = new ArrayList<MovingUnit>();

                for (MovingUnit move : movingUnits) {
                    movingSomething = true;
                    Entity ge = game.getEntity(move.entity.getId());
                    if (move.path.size() > 0) {

                        UnitLocation loc = move.path.get(0);

                        if (ge != null) {
                            redrawMovingEntity(move.entity, loc.getCoords(),
                                    loc.getFacing(), loc.getElevation());
                        }
                        move.path.remove(0);
                    } else {
                        if (ge != null) {
                            redrawEntity(ge);
                        }
                        spent.add(move);
                    }

                }

                for (MovingUnit move : spent) {
                    movingUnits.remove(move);
                }
                moveWait = 0;

                if (movingUnits.size() == 0) {
                    movingEntitySpriteIds.clear();
                    movingEntitySprites.clear();
                    ghostEntitySprites.clear();
                    processBoardViewEvent(new BoardViewEvent(this,
                            BoardViewEvent.FINISHED_MOVING_UNITS));
                }
            }
        }
        return movingSomething;
    }

    //
    // MouseListener
    //
    public void mousePressed(MouseEvent me) {
        requestFocusInWindow();
        Point point = me.getPoint();
        if (null == point) {
            return;
        }
        // we clicked the right mouse button,
        // remember the position if we start to scroll
        // if we drag, we should scroll
        if (SwingUtilities.isRightMouseButton(me)) {
            scrollXDifference = me.getX();
            scrollYDifference = me.getY();
            shouldScroll = true;
        }

        if (me.isPopupTrigger() && !dragging) {
            mouseAction(getCoordsAt(point), BOARD_HEX_POPUP, me.getModifiers());
            return;
        }
        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.get(i);
            double width = scrollpane.getViewport().getSize()
                    .getWidth();
            double height = scrollpane.getViewport().getSize()
                    .getHeight();
            Dimension dispDimension = new Dimension();
            dispDimension.setSize(width, height);
            // we need to adjust the point, because it should be against the
            // displayable dimension
            Point dispPoint = new Point();
            dispPoint.setLocation(point.x + getBounds().x, point.y
                    + getBounds().y);
            if (disp.isHit(dispPoint, dispDimension)) {
                return;
            }
        }
        mouseAction(getCoordsAt(point), BOARD_HEX_DRAG, me.getModifiers());
    }

    public void mouseReleased(MouseEvent me) {
        // don't show the popup if we are drag-scrolling
        if (me.isPopupTrigger() && !dragging) {
            mouseAction(getCoordsAt(me.getPoint()), BOARD_HEX_POPUP,
                    me.getModifiers());
            // stop scrolling
            shouldScroll = false;
            return;
        }

        // if we released the right mouse button, there's no more
        // scrolling
        if (SwingUtilities.isRightMouseButton(me)) {
            scrollXDifference = 0;
            scrollYDifference = 0;
            dragging = false;
            shouldScroll = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.get(i);
            if (disp.isReleased()) {
                return;
            }
        }

        if (me.getClickCount() == 1) {
            mouseAction(getCoordsAt(me.getPoint()), BOARD_HEX_CLICK,
                    me.getModifiers());
        } else {
            mouseAction(getCoordsAt(me.getPoint()), BOARD_HEX_DOUBLECLICK,
                    me.getModifiers());
        }
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
    }

    private class MovingUnit {
        public Entity entity;

        public ArrayList<UnitLocation> path;

        MovingUnit(Entity entity, Vector<UnitLocation> path) {
            this.entity = entity;
            this.path = new ArrayList<UnitLocation>(path);
        }
    }

    /**
     * Determine if the tile manager's images have been loaded.
     *
     * @return <code>true</code> if all images have been loaded.
     *         <code>false</code> if more need to be loaded.
     */
    public boolean isTileImagesLoaded() {
        return tileManager.isLoaded();
    }

    public void setUseLOSTool(boolean use) {
        useLOSTool = use;
    }

    public TilesetManager getTilesetManager() {
        return tileManager;
    }

    /**
     * @param lastCursor
     *            The lastCursor to set.
     */
    public void setLastCursor(Coords lastCursor) {
        this.lastCursor = lastCursor;
    }

    /**
     * @return Returns the lastCursor.
     */
    public Coords getLastCursor() {
        return lastCursor;
    }

    /**
     * @param highlighted
     *            The highlighted to set.
     */
    public void setHighlighted(Coords highlighted) {
        this.highlighted = highlighted;
    }

    /**
     * @return Returns the highlighted.
     */
    public Coords getHighlighted() {
        return highlighted;
    }

    /**
     * @param selected
     *            The selected to set.
     */
    public void setSelected(Coords selected) {
        if (this.selected != selected) {
            this.selected = selected;

            // force a repaint of the board
            updateBoard();
        }
    }

    /**
     * @return Returns the selected.
     */
    public Coords getSelected() {
        return selected;
    }

    /**
     * @param firstLOS
     *            The firstLOS to set.
     */
    public void setFirstLOS(Coords firstLOS) {
        this.firstLOS = firstLOS;
    }

    /**
     * @return Returns the firstLOS.
     */
    public Coords getFirstLOS() {
        return firstLOS;
    }

    /**
     * Determines if this Board contains the Coords, and if so, "selects" that
     * Coords.
     *
     * @param coords
     *            the Coords.
     */
    public void select(Coords coords) {
        if ((coords == null) || game.getBoard().contains(coords)) {
            setSelected(coords);
            moveCursor(selectedSprite, coords);
            moveCursor(firstLOSSprite, null);
            moveCursor(secondLOSSprite, null);
            processBoardViewEvent(new BoardViewEvent(this, coords, null,
                    BoardViewEvent.BOARD_HEX_SELECTED, 0));
        }
    }

    /**
     * "Selects" the specified Coords.
     *
     * @param x
     *            the x coordinate.
     * @param y
     *            the y coordinate.
     */
    public void select(int x, int y) {
        select(new Coords(x, y));
    }

    /**
     * Determines if this Board contains the Coords, and if so, highlights that
     * Coords.
     *
     * @param coords
     *            the Coords.
     */
    public void highlight(Coords coords) {
        if ((coords == null) || game.getBoard().contains(coords)) {
            setHighlighted(coords);
            moveCursor(highlightSprite, coords);
            moveCursor(firstLOSSprite, null);
            moveCursor(secondLOSSprite, null);
            processBoardViewEvent(new BoardViewEvent(this, coords, null,
                    BoardViewEvent.BOARD_HEX_HIGHLIGHTED, 0));
        }
    }

    /**
     * Highlights the specified Coords.
     *
     * @param x
     *            the x coordinate.
     * @param y
     *            the y coordinate.
     */
    public void highlight(int x, int y) {
        highlight(new Coords(x, y));
    }

    /**
     * Determines if this Board contains the Coords, and if so, "cursors" that
     * Coords.
     *
     * @param coords
     *            the Coords.
     */
    public void cursor(Coords coords) {
        if ((coords == null) || game.getBoard().contains(coords)) {
            if ((getLastCursor() == null) || (coords == null)
                    || !coords.equals(getLastCursor())) {
                setLastCursor(coords);
                moveCursor(cursorSprite, coords);
                moveCursor(firstLOSSprite, null);
                moveCursor(secondLOSSprite, null);
                processBoardViewEvent(new BoardViewEvent(this, coords, null,
                        BoardViewEvent.BOARD_HEX_CURSOR, 0));
            } else {
                setLastCursor(coords);
            }
        }
    }

    /**
     * "Cursors" the specified Coords.
     *
     * @param x
     *            the x coordinate.
     * @param y
     *            the y coordinate.
     */
    public void cursor(int x, int y) {
        cursor(new Coords(x, y));
    }

    public void checkLOS(Coords c) {
        if ((c == null) || game.getBoard().contains(c)) {
            if (getFirstLOS() == null) {
                setFirstLOS(c);
                firstLOSHex(c);
                processBoardViewEvent(new BoardViewEvent(this, c, null,
                        BoardViewEvent.BOARD_FIRST_LOS_HEX, 0));
            } else {
                secondLOSHex(c, getFirstLOS());
                processBoardViewEvent(new BoardViewEvent(this, c, null,
                        BoardViewEvent.BOARD_SECOND_LOS_HEX, 0));
                setFirstLOS(null);
            }
        }
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, notifies
     * listeners about the specified mouse action.
     */
    public void mouseAction(int x, int y, int mtype, int modifiers) {
        if (game.getBoard().contains(x, y)) {
            Coords c = new Coords(x, y);
            switch (mtype) {
                case BOARD_HEX_CLICK:
                    if ((modifiers & java.awt.event.InputEvent.CTRL_MASK) != 0) {
                        checkLOS(c);
                    } else {
                        processBoardViewEvent(new BoardViewEvent(this, c, null,
                                BoardViewEvent.BOARD_HEX_CLICKED, modifiers));
                    }
                    break;
                case BOARD_HEX_DOUBLECLICK:
                    processBoardViewEvent(new BoardViewEvent(this, c, null,
                            BoardViewEvent.BOARD_HEX_DOUBLECLICKED, modifiers));
                    break;
                case BOARD_HEX_DRAG:
                    processBoardViewEvent(new BoardViewEvent(this, c, null,
                            BoardViewEvent.BOARD_HEX_DRAGGED, modifiers));
                    break;
                case BOARD_HEX_POPUP:
                    processBoardViewEvent(new BoardViewEvent(this, c, null,
                            BoardViewEvent.BOARD_HEX_POPUP, modifiers));
                    break;
            }
        }
    }

    /**
     * Notifies listeners about the specified mouse action.
     *
     * @param coords
     *            the Coords.
     */
    public void mouseAction(Coords coords, int mtype, int modifiers) {
        mouseAction(coords.x, coords.y, mtype, modifiers);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.BoardListener#boardNewBoard(megamek.common.BoardEvent)
     */
    public void boardNewBoard(BoardEvent b) {
        updateBoard();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public synchronized void boardChangedHex(BoardEvent b) {
        IHex hex = game.getBoard().getHex(b.getCoords());
        tileManager.clearHex(hex);
        tileManager.waitForHex(hex);
        repaint();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public synchronized void boardChangedAllHexes(BoardEvent b) {
        tileManager.loadAllHexes();
        repaint();
    }

    private GameListener gameListener = new GameListenerAdapter() {

        @Override
        public void gameEntityNew(GameEntityNewEvent e) {
            updateEcmList();
            redrawAllEntities();
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                refreshMoveVectors();
            }
        }

        @Override
        public void gameEntityRemove(GameEntityRemoveEvent e) {
            updateEcmList();
            redrawAllEntities();
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                refreshMoveVectors();
            }
        }

        @Override
        public void gameEntityChange(GameEntityChangeEvent e) {
            Vector<UnitLocation> mp = e.getMovePath();
            updateEcmList();
            if (e.getEntity().hasActiveECM()) {
                // this might disrupt c3/c3i lines, so redraw all
                redrawAllEntities();
            }
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                refreshMoveVectors();
            }
            if ((mp != null) && (mp.size() > 0)
                    && GUIPreferences.getInstance().getShowMoveStep()) {
                addMovingUnit(e.getEntity(), mp);
            } else {
                redrawEntity(e.getEntity(), e.getOldEntity());
            }
        }

        @Override
        public void gameNewAction(GameNewActionEvent e) {
            EntityAction ea = e.getAction();
            if (ea instanceof AttackAction) {
                addAttack((AttackAction) ea);
            }
        }

        @Override
        public void gameBoardNew(GameBoardNewEvent e) {
            IBoard b = e.getOldBoard();
            if (b != null) {
                b.removeBoardListener(BoardView1.this);
            }
            b = e.getNewBoard();
            if (b != null) {
                b.addBoardListener(BoardView1.this);
            }
            updateBoard();
        }

        @Override
        public void gameBoardChanged(GameBoardChangeEvent e) {
            boardChanged();
        }

        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            refreshAttacks();
            switch (e.getNewPhase()) {
                case PHASE_MOVEMENT:
                    refreshMoveVectors();
                case PHASE_FIRING:
                    clearAllMoveVectors();
                case PHASE_PHYSICAL:
                    refreshAttacks();
                    break;
                case PHASE_INITIATIVE:
                    clearAllAttacks();
                    break;
                case PHASE_END:
                case PHASE_VICTORY:
                    clearSprites();
                case PHASE_LOUNGE:
                    clearAllMoveVectors();
                    clearAllAttacks();
                    clearSprites();
                    select(null);
                    cursor(null);
                    highlight(null);
                default:
            }
        }
    };

    synchronized void boardChanged() {
        redrawAllEntities();
        redrawAllFlares();
    }

    void clearSprites() {
        pathSprites.clear();
        firingSprites.clear();
        attackSprites.clear();
        c3Sprites.clear();
        flyOverSprites.clear();
        movementSprites.clear();

    }

    public synchronized void updateBoard() {
        updateBoardSize();
        redrawAllEntities();
    }

    /**
     * the old redrawworker converted to a runnable which is called now and then
     * from the event thread
     */
    protected class RedrawWorker implements Runnable {

        protected long lastTime = System.currentTimeMillis();

        protected long currentTime = System.currentTimeMillis();

        public void run() {
            currentTime = System.currentTimeMillis();
            if (isShowing()) {
                boolean redraw = false;
                for (int i = 0; i < displayables.size(); i++) {
                    IDisplayable disp = displayables.get(i);
                    if (!disp.isSliding()) {
                        disp.setIdleTime(currentTime - lastTime, true);
                    } else {
                        redraw = redraw || disp.slide();
                    }
                }
                redraw = redraw || doMoveUnits(currentTime - lastTime);
                if (redraw) {
                    repaint();
                }
            }
            lastTime = currentTime;
        }
    }

    public synchronized void selectEntity(Entity e) {
        selectedEntity = e;
        // If we don't do this, the selectedWeapon might not correspond to this
        // entity
        selectedWeapon = null;
    }

    public synchronized void weaponSelected(MechDisplayEvent b) {
        selectedEntity = b.getEntity();
        selectedWeapon = b.getEquip();
        repaint();
    }

    // This is expensive, so precalculate when entity changes
    public void updateEcmList() {
        ArrayList<EcmBubble> list = new ArrayList<EcmBubble>();
        for (Enumeration<Entity> e = game.getEntities(); e.hasMoreElements();) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            int range = ent.getECMRange();
            boolean deployed = ent.isDeployed();
            boolean offboard = ent.isOffBoard();
            if ((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = game.getEntity(ent.getTransportId());
                if ((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                    deployed = carrier.isDeployed();
                    offboard = carrier.isOffBoard();
                }
            }
            if ((entPos == null) || !deployed || offboard) {
                continue;
            }
            if (range != Entity.NONE) {
                int tint = PlayerColors.getColorRGB(ent.getOwner()
                        .getColorIndex());
                list.add(new EcmBubble(entPos, range, tint));
            }
            if (game.getBoard().inSpace()) {
                // then BAP is also ECCM so it needs a bubble
                range = ent.getBAPRange();
                int direction = -1;
                if (range != Entity.NONE) {
                    if (range > 6) {
                        direction = ent.getFacing();
                    }
                    int tint = PlayerColors.getColorRGB(ent.getOwner()
                            .getColorIndex());
                    list.add(new EcmBubble(entPos, range, tint, direction));
                }
            }
        }
        HashMap<Coords, Integer> table = new HashMap<Coords, Integer>();
        for (EcmBubble b : list) {
            Integer col = new Integer(b.tint);
            for (int x = -b.range; x <= b.range; x++) {
                for (int y = -b.range; y <= b.range; y++) {
                    Coords c = new Coords(x + b.x, y + b.y);
                    // clip rectangle to hexagon
                    if ((b.distance(c) <= b.range)
                            && ((b.direction == -1) || Compute.isInArc(b,
                                    b.direction, c, Compute.ARC_NOSE))) {
                        Integer tint = table.get(c);
                        if (tint == null) {
                            table.put(c, col);
                        } else if (tint.intValue() != b.tint) {
                            int red1 = (tint.intValue() >> 16) & 0xff;
                            int green1 = (tint.intValue() >> 8) & 0xff;
                            int blue1 = tint.intValue() & 0xff;
                            int red2 = (b.tint >> 16) & 0xff;
                            int green2 = (b.tint >> 8) & 0xff;
                            int blue2 = b.tint & 0xff;
                            red1 = (red1 + red2) / 2;
                            green1 = (green1 + green2) / 2;
                            blue1 = (blue1 + blue2) / 2;
                            table.put(c, new Integer((red1 << 16)
                                    | (green1 << 8) | blue1));
                        }
                    }
                }
            }
        }
        synchronized (this) {
            ecmHexes = table;
        }
        repaint();
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
        final Dimension size = scrollpane.getViewport().getSize();
        if (arg1 == SwingConstants.VERTICAL) {
            return size.height;
        }
        return size.width;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
        if (arg1 == SwingConstants.VERTICAL) {
            return (int) ((scale * HEX_H) / 2.0);
        }
        return (int) ((scale * HEX_W) / 2.0);
    }

    @Override
    public void setPreferredSize(Dimension d){
        super.setPreferredSize(d);
        preferredSize = new Dimension(d);
    }

    @Override
    public Dimension getPreferredSize() {
        // If the board is small, we want the preferred size to fill the whole
        //  ScrollPane viewport, for purposes of drawing the tiled background
        //  icon.
        // However, we also need the scrollable client to be as big as the
        //  board plus the pad size.
        return new Dimension(
                Math.max(boardSize.width + 2 * HEX_W,preferredSize.width),
                Math.max(boardSize.height + 2 * HEX_W,preferredSize.height));
    }

    /**
     * Have the player select an Entity from the entities at the given coords.
     *
     * @param pos
     *            - the <code>Coords</code> containing targets.
     */
    private Entity chooseEntity(Coords pos) {

        // Assume that we have *no* choice.
        Entity choice = null;

        // Get the available choices.
        Enumeration<Entity> choices = game.getEntities(pos);

        // Convert the choices into a List of targets.
        Vector<Entity> entities = new Vector<Entity>();
        while (choices.hasMoreElements()) {
            entities.addElement(choices.nextElement());
        }

        // Do we have a single choice?
        if (entities.size() == 1) {
            // Return that choice.
            choice = entities.elementAt(0);
        }

        // If we have multiple choices, display a selection dialog.
        else if (entities.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            null,
                            Messages.getString(
                                    "BoardView1.ChooseEntityDialog.message", new Object[] { pos.getBoardNum() }), //$NON-NLS-1$
                            Messages.getString("BoardView1.ChooseEntityDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(entities), null);
            choice = (Entity) SharedUtility.getTargetPicked(entities, input);
        } // End have-choices

        // Return the chosen unit.
        return choice;
    }


    /**
     * The text to be displayed when the mouse is at a certain point.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        // If new instance of mouse event, redraw obscured hexes and elevations.
        repaint();

        StringBuffer txt = new StringBuffer();
        IHex mhex = null;
        final Point point = e.getPoint();
        final Coords mcoords = getCoordsAt(point);
        final ArrayList<ArtilleryAttackAction> artilleryAttacks =
                getArtilleryAttacksAtLocation(mcoords);
        final Mounted curWeapon = getSelectedArtilleryWeapon();

        if (GUIPreferences.getInstance().getShowMapHexPopup()
                && game.getBoard().contains(mcoords)) {
            mhex = game.getBoard().getHex(mcoords);
        }

        txt.append("<html>"); //$NON-NLS-1$

        // are we on a hex?
        if (mhex != null) {
            txt.append(Messages.getString("BoardView1.Hex") + //$NON-NLS-1$
                    mcoords.getBoardNum());
            txt.append(Messages.getString("BoardView1.level") + //$NON-NLS-1$
                    mhex.getElevation());
            txt.append("<br>"); //$NON-NLS-1$

            // cycle through the terrains and report types found
            // this will skip buildings and other constructed units
            int terrainTypes[] = mhex.getTerrainTypes();
            for (int i = 0; i < terrainTypes.length; i++) {
                int terType = terrainTypes[i];
                if (mhex.containsTerrain(terType)) {
                    int tf = mhex.getTerrain(terType).getTerrainFactor();
                    int ttl = mhex.getTerrain(terType).getLevel();
                    String name = Terrains.getDisplayName(terType, ttl);
                    if (tf > 0) {
                        name = name + " (" + tf + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    if (null != name) {
                        txt.append(name);
                        txt.append("<br>"); //$NON-NLS-1$
                    }
                }
            }

            // Do we have a building?
            if (mhex.containsTerrain(Terrains.FUEL_TANK)) {
                // Get the building.
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                StringBuffer buf = new StringBuffer(
                        Messages.getString("BoardView1.Height")); //$NON-NLS-1$
                // Each hex of a building has its own elevation.
                buf.append(mhex.terrainLevel(Terrains.FUEL_TANK_ELEV));
                buf.append(" "); //$NON-NLS-1$
                buf.append(bldg.toString());
                buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
                buf.append(bldg.getCurrentCF(mcoords));
                txt.append(buf.toString());
                txt.append("<br>"); //$NON-NLS-1$
            }
            if (mhex.containsTerrain(Terrains.BUILDING)) {
                // Get the building.
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                // in the map editor, the building might not exist
                if (bldg != null) {
                    StringBuffer buf = new StringBuffer(
                            Messages.getString("BoardView1.Height")); //$NON-NLS-1$
                    // Each hex of a building has its own elevation.
                    buf.append(mhex.terrainLevel(Terrains.BLDG_ELEV));
                    buf.append(" "); //$NON-NLS-1$
                    buf.append(bldg.toString());
                    buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
                    buf.append(bldg.getCurrentCF(mcoords));
                    buf.append(Messages.getString("BoardView1.BldgArmor")); //$NON-NLS-1$
                    buf.append(bldg.getArmor(mcoords));
                    buf.append(Messages.getString("BoardView1.BldgBasement"));
                    buf.append(bldg.getBasement(mcoords).getDesc());
                    if (bldg.getBasementCollapsed(mcoords)) {
                        buf.append(Messages
                                .getString("BoardView1.BldgBasementCollapsed")); //$NON-NLS-1$
                    }
                    txt.append(buf.toString());
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }

            // Do we have a bridge?
            if (mhex.containsTerrain(Terrains.BRIDGE)) {
                // Get the building.
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                StringBuffer buf = new StringBuffer(
                        Messages.getString("BoardView1.Height")); //$NON-NLS-1$
                // Each hex of a building has its own elevation.
                buf.append(mhex.terrainLevel(Terrains.BRIDGE_ELEV));
                buf.append(" "); //$NON-NLS-1$
                buf.append(bldg.toString());
                buf.append(Messages.getString("BoardView1.CF")); //$NON-NLS-1$
                buf.append(bldg.getCurrentCF(mcoords));
                txt.append(buf.toString());
                txt.append("<br>"); //$NON-NLS-1$
            }

            if (game.containsMinefield(mcoords)) {
                Vector<Minefield> minefields = game.getMinefields(mcoords);
                for (int i = 0; i < minefields.size(); i++) {
                    Minefield mf = minefields.elementAt(i);
                    String owner = " (" //$NON-NLS-1$
                    + game.getPlayer(mf.getPlayerId()).getName()
                    + ")"; //$NON-NLS-1$

                    switch (mf.getType()) {
                    case (Minefield.TYPE_CONVENTIONAL):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    case (Minefield.TYPE_COMMAND_DETONATED):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    case (Minefield.TYPE_VIBRABOMB):
                        if (mf.getPlayerId() == localPlayer.getId()) {
                            txt.append(mf.getName()
                                    + Messages
                                            .getString("BoardView1.minefield") //$NON-NLS-1$
                                    + "(" + mf.getDensity() + ")" + "(" //$NON-NLS-1$
                                    + mf.getSetting() + ") " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        } else {
                            txt.append(mf.getName()
                                    + Messages
                                            .getString("BoardView1.minefield") //$NON-NLS-1$
                                    + "(" + mf.getDensity() + ")" + " " + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        break;
                    case (Minefield.TYPE_ACTIVE):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    case (Minefield.TYPE_INFERNO):
                        txt.append(mf.getName()
                                + Messages.getString("BoardView1.minefield") //$NON-NLS-1$
                                + "(" + mf.getDensity() + ")" + owner); //$NON-NLS-1$ //$NON-NLS-2$
                        break;
                    }
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }

        }
        
        // check if it's on any flares
        for (FlareSprite fSprite : flareSprites) {
            if (fSprite.isInside(point)) {
                final String[] flareStrings = fSprite.getTooltip();
                for (String entString : flareStrings) {
                    txt.append(entString);
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        }
        
        // check if it's on any entities
        for (EntitySprite eSprite : entitySprites) {
            if (eSprite.isInside(point)) {
                final String[] entityStrings = eSprite.getTooltip();
                for (String entString : entityStrings){
                    txt.append(entString);
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        }

        // check if it's on any attacks
        for (AttackSprite aSprite : attackSprites) {
            if (aSprite.isInside(point)) {
                final String[] attackStrings = aSprite.getTooltip();
                for (String attString : attackStrings){
                    txt.append(attString);
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        }

        // check artillery attacks
        for (ArtilleryAttackAction aaa : artilleryAttacks) {
            final Entity ae = game.getEntity(aaa.getEntityId());
            String s = null;
            if (ae != null) {
                if (aaa.getWeaponId() > -1) {
                    Mounted weap = ae.getEquipment(aaa.getWeaponId());
                    s = weap.getName();
                    if (aaa.getAmmoId() > -1) {
                        Mounted ammo = ae.getEquipment(aaa.getAmmoId());
                        s += " (" + ammo.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
            if (s == null) {
                s = Messages.getString("BoardView1.Artillery");
            }
            txt.append(Messages.getString(
                    "BoardView1.ArtilleryAttack", new Object[] { s, //$NON-NLS-1$
                            new Integer(aaa.turnsTilHit)}));
            txt.append("<br>"); //$NON-NLS-1$
        }

        // check artillery fire adjustment
        if ((curWeapon != null) && (selectedEntity != null)) {
            // process targetted hexes
            int amod = 0;
            // Check the predesignated hexes
            if (selectedEntity.getOwner().getArtyAutoHitHexes()
                    .contains(mcoords)) {
                amod = TargetRoll.AUTOMATIC_SUCCESS;
            } else {
                amod = selectedEntity.aTracker.getModifier(curWeapon, mcoords);
            }

            if (amod == TargetRoll.AUTOMATIC_SUCCESS) {
                txt.append(Messages
                        .getString("BoardView1.ArtilleryAutohit")); //$NON-NLS-1$
                txt.append("<br>"); //$NON-NLS-1$
            } else {
                txt.append(Messages.getString(
                        "BoardView1.ArtilleryAdjustment", //$NON-NLS-1$
                        new Object[] { new Integer(amod) }));
                txt.append("<br>"); //$NON-NLS-1$
            }
        }

        final Collection<SpecialHexDisplay> shdList = game.getBoard()
                .getSpecialHexDisplay(mcoords);
        if (shdList != null) {
            for (SpecialHexDisplay shd : shdList) {
                if (!shd.isObscured(localPlayer)) {
                    if (shd.getType() == SpecialHexDisplay.Type.PLAYER_NOTE){
                        if (localPlayer.equals(shd.getOwner())){
                            txt.append("Note: ");
                        } else {
                            txt.append("Note (" + shd.getOwner().getName()
                                    + "): ");
                        }
                    }
                    String buf = shd.getInfo();
                    buf = buf.replaceAll("\\n", "<br>");
                    txt.append(buf);
                    txt.append("<br>"); //$NON-NLS-1$
                }
            }
        }

        txt.append("</html>"); //$NON-NLS-1$
        return txt.toString();
    }

    private ArrayList<ArtilleryAttackAction> getArtilleryAttacksAtLocation(
            Coords c) {
        ArrayList<ArtilleryAttackAction> v = new ArrayList<ArtilleryAttackAction>();
        for (Enumeration<ArtilleryAttackAction> attacks = game
                .getArtilleryAttacks(); attacks.hasMoreElements();) {
            ArtilleryAttackAction a = attacks.nextElement();
            if (a.getTarget(game).getPosition().equals(c)) {
                v.add(a);
            }
        }
        return v;
    }

    public Component getComponent() {
        return getComponent(false);
    }

    public Component getComponent(boolean scrollBars) {
        // If we're already configured, return the scrollpane
        if (scrollpane != null) {
            return scrollpane;
        }

        SkinSpecification bvSkinSpec =
                SkinXMLHandler.getSkin(SkinXMLHandler.BOARDVIEW);

        // Setup background icons
        try {
            java.net.URI imgURL;
            File file;
            if (bvSkinSpec.backgrounds.size() > 0){
                file = new File(Configuration.widgetsDir(),
                        bvSkinSpec.backgrounds.get(0));
                imgURL = file.toURI();
                if (!file.exists()){
                    System.err.println("BoardView1 Error: icon doesn't exist: "
                            + file.getAbsolutePath());
                } else {
                    bvBgIcon = new ImageIcon(imgURL.toURL());
                }
            }
            if (bvSkinSpec.backgrounds.size() > 1){
                file = new File(Configuration.widgetsDir(),
                        bvSkinSpec.backgrounds.get(1));
                imgURL = file.toURI();
                if (!file.exists()){
                    System.err.println("BoardView1 Error: icon doesn't exist: "
                            + file.getAbsolutePath());
                } else {
                    scrollPaneBgIcon = new ImageIcon(imgURL.toURL());
                }
            }
        } catch (Exception e){
            System.out.println("Error loading BoardView background images!");
            System.out.println(e.getMessage());
        }

        // Place the board viewer in a set of scrollbars.
        scrollpane = new JScrollPane(this){

            /**
             *
             */
            private static final long serialVersionUID = 5973610449428194319L;

            protected void paintComponent(Graphics g) {
                if (scrollPaneBgIcon == null){
                    super.paintComponent(g);
                    return;
                }
                int w = getWidth();
                int h = getHeight();
                int iW = scrollPaneBgIcon.getIconWidth();
                int iH = scrollPaneBgIcon.getIconHeight();
                if (scrollPaneBgBuffer == null 
                        || scrollPaneBgBuffer.getWidth() != w
                        || scrollPaneBgBuffer.getHeight() != h) {
                    scrollPaneBgBuffer = new BufferedImage(w, h,
                            BufferedImage.TYPE_INT_RGB);
                    Graphics bgGraph = scrollPaneBgBuffer.getGraphics();                       
                    for (int x = 0; x < w; x+=iW){
                        for (int y = 0; y < h; y+=iH){
                            bgGraph.drawImage(scrollPaneBgIcon.getImage(), x, y,
                                    scrollPaneBgIcon.getImageObserver());
                        }
                    }
                }
                g.drawImage(scrollPaneBgBuffer, 0, 0, null);                
            }
        };
        scrollpane.setBorder(new MegamekBorder(bvSkinSpec));
        scrollpane.setLayout(new ScrollPaneLayout());
        // we need to use the simple scroll mode because otherwise the
        // IDisplayables that are drawn in fixed positions in the viewport
        // leave artifacts when scrolling
        scrollpane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        vbar = scrollpane.getVerticalScrollBar();
        hbar = scrollpane.getHorizontalScrollBar();

        if (!scrollBars){
            vbar.setPreferredSize(new Dimension(0, vbar.getHeight()));
            hbar.setPreferredSize(new Dimension(hbar.getWidth(),0));
        }

        return scrollpane;
    }

    /**
     * refresh the IDisplayables
     */
    public void refreshDisplayables() {
        repaint();
    }

    public void showPopup(Object popup, Coords c) {
        Point p = getHexLocation(c);
        p.x += (int) (HEX_WC * scale) - scrollpane.getX() + HEX_W;
        p.y += (int) ((HEX_H * scale) / 2) - scrollpane.getY() + HEX_H;
        if (((JPopupMenu) popup).getParent() == null) {
            add((JPopupMenu) popup);
        }
        ((JPopupMenu) popup).show(this, p.x, p.y);
    }

    public void refreshMinefields() {
        repaint();
    }

    /**
     * Increases zoomIndex and refreshes the map.
     */
    public void zoomIn() {
        if (zoomIndex == (ZOOM_FACTORS.length - 1)) {
            return;
        }
        zoomIndex++;
        zoom();
    }

    /**
     * Decreases zoomIndex and refreshes the map.
     */
    public void zoomOut() {
        if (zoomIndex == 0) {
            return;
        }
        zoomIndex--;
        zoom();
    }

    public void hideTooltip() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void checkZoomIndex() {
        if (zoomIndex > (ZOOM_FACTORS.length - 1)) {
            zoomIndex = ZOOM_FACTORS.length - 1;
        }
        if (zoomIndex < 0) {
            zoomIndex = 0;
        }
    }

    /**
     * Changes hex dimensions and refreshes the map with the new scale
     */
    private void zoom() {
        checkZoomIndex();
        scale = ZOOM_FACTORS[zoomIndex];
        GUIPreferences.getInstance().setMapZoomIndex(zoomIndex);

        hex_size = new Dimension((int) (HEX_W * scale), (int) (HEX_H * scale));

        scaledImageCache = new ImageCache<Integer, Image>();

        cursorSprite.prepare();
        highlightSprite.prepare();
        selectedSprite.prepare();
        firstLOSSprite.prepare();
        secondLOSSprite.prepare();

        updateFontSizes();
        updateBoard();
        for (StepSprite sprite : pathSprites) {
            sprite.refreshZoomLevel();
        }
        for (FiringSolutionSprite sprite : firingSprites) {
            sprite.refreshZoomLevel();
        }
        this.setSize(boardSize);

        repaint();
    }

    private void updateFontSizes() {
        if (zoomIndex <= 4) {
            font_elev = FONT_7;
            font_hexnum = FONT_7;
            font_minefield = FONT_7;
        }
        if ((zoomIndex <= 5) & (zoomIndex > 4)) {
            font_elev = FONT_8;
            font_hexnum = FONT_8;
            font_minefield = FONT_8;
        }
        if (zoomIndex > 5) {
            font_elev = FONT_9;
            font_hexnum = FONT_9;
            font_minefield = FONT_9;
        }
    }

    /**
     * Return a scaled version of the input.  If the useCache flag is set, the
     * scaled image will be stored in an image cache for later retrieval.
     *
     * @param base   The image to get a scaled copy of.  The current zoom level
     *                  is used to determine the scale.
     *
     * @param useCache  This flag determines whether the scaled image should
     *                      be stored in a cache for later retrieval.
     */
    Image getScaledImage(Image base, boolean useCache) {
        if (base == null) {
            return null;
        }
        if (zoomIndex == BASE_ZOOM_INDEX) {
            return base;
        }


        Image scaled;
        if (useCache){
            // Check the cache
            scaled = scaledImageCache.get(base.hashCode());
        } else {
            scaled = null;
        }
        // Compute the scaled image
        if (scaled == null) {
            MediaTracker tracker = new MediaTracker(this);
            if ((base.getWidth(null) == -1) || (base.getHeight(null) == -1)) {
                tracker.addImage(base, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tracker.removeImage(base);
            }
            int width = (int) (base.getWidth(null) * scale);
            int height = (int) (base.getHeight(null) * scale);

            // TODO: insert a check that width and height are > 0.

            scaled = scale(base, width, height);
            tracker.addImage(scaled, 1);
            // Wait for image to load
            try {
                tracker.waitForID(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tracker.removeImage(scaled);
            // Cache the image if the flag is set
            if (useCache){
                scaledImageCache.put(base.hashCode(), scaled);
            }
        }
        return scaled;
    }

    /**
     * The actual scaling code.
     */
    private Image scale(Image img, int width, int height) {
        ImageFilter filter;
        filter = new ImprovedAveragingScaleFilter(img.getWidth(null),
                img.getHeight(null), width, height);

        ImageProducer prod;
        prod = new FilteredImageSource(img.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(prod);
    }

    public boolean toggleIsometric() {
        drawIsometric = !drawIsometric;
        updateBoard();
        repaint();
        return drawIsometric;
    }

    BufferedImage createShadowMask(Image image) {
        BufferedImage mask = new BufferedImage(image.getWidth(null),
                image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        float opacity = 0.4f;
        Graphics2D g2d = mask.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN,
                opacity));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        g2d.dispose();
        return mask;
    }

    public void die() {
        ourTask.cancel();
    }

    /**
     * Returns true if the BoardView has an active chatter box else false.
     * @return
     */
    public boolean getChatterBoxActive(){
        return chatterBoxActive;
    }

    /**
     * Sets whether the BoardView has an active chatter box or not.
     * @param cba
     */
    public void setChatterBoxActive(boolean cba){
        chatterBoxActive = cba;
    }
}
