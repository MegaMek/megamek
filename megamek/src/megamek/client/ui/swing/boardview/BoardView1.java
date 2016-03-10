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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.Kernel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.widget.MegamekBorder;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.ArtilleryTracker;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.ComputeECM;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.ECMInfo;
import megamek.common.Entity;
import megamek.common.Flare;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.QuadMech;
import megamek.common.TripodMech;
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
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;

/**
 * Displays the board; lets the user scroll around and select points on it.
 */
public class BoardView1 extends JPanel implements IBoardView, Scrollable,
        BoardListener, MouseListener, IPreferenceChangeListener {

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

    private static final float[] ZOOM_FACTORS = {0.30f, 0.41f, 0.50f, 0.60f,
                                                 0.68f, 0.79f, 0.90f, 1.00f, 1.09f, 1.17f, 1.3f};
    
    public static final int [] allDirections = {0,1,2,3,4,5};
    
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
    private Dimension preferredSize = new Dimension(0, 0);

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
    private List<EntitySprite> entitySprites = new ArrayList<EntitySprite>();
    private List<IsometricSprite> isometricSprites = new ArrayList<IsometricSprite>();

    private ArrayList<FlareSprite> flareSprites = new ArrayList<FlareSprite>();
    /**
     * A Map that maps an Entity ID and a secondary position to a Sprite. Note
     * that the key is a List where the first entry will be the Entity ID and
     * the second entry will be which secondary position the sprite belongs to;
     * if the Entity has no secondary positions, the first element will be the
     * ID and the second element will be -1.
     */
    private Map<List<Integer>, EntitySprite> entitySpriteIds = new HashMap<>();
    /**
     * A Map that maps an Entity ID and a secondary position to a Sprite. Note
     * that the key is a List where the first entry will be the Entity ID and
     * the second entry will be which secondary position the sprite belongs to;
     * if the Entity has no secondary positions, the first element will be the
     * ID and the second element will be -1.
     */
    private Map<List<Integer>, IsometricSprite> isometricSpriteIds = new HashMap<>();

    // sprites for the three selection cursors
    private CursorSprite cursorSprite;
    private CursorSprite highlightSprite;
    private CursorSprite selectedSprite;
    private CursorSprite firstLOSSprite;
    private CursorSprite secondLOSSprite;

    // sprite for current movement
    ArrayList<StepSprite> pathSprites = new ArrayList<StepSprite>();

    private ArrayList<Coords> strafingCoords = new ArrayList<Coords>(5);

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
    
    // List of sprites for the weapon field of fire
    private ArrayList<HexSprite> fieldofFireSprites = new ArrayList<>();
    public int[][] fieldofFireRanges = { new int[5], new int[5] };
    public int fieldofFireWpArc;
    public Entity fieldofFireUnit;
    public int fieldofFireWpLoc;
    // int because it acts as an array index
    public int fieldofFireWpUnderwater = 0;
    private static final String[] rangeTexts = { "min", "S", "M", "L", "E" };

    TilesetManager tileManager = null;

    // polygons for a few things
    static Polygon hexPoly;
    static {
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
    }
    
    Shape[] movementPolys;
    Shape[] facingPolys;
    Shape UpArrow;
    Shape DownArrow;
    
    // Image to hold the complete board shadow map
    BufferedImage ShadowMap;
    double[] LightDirection = { -19, 7 };

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
    private ImageCache<Integer, BufferedImage> shadowImageCache =
            new ImageCache<Integer, BufferedImage>();

    private Set<Integer> animatedImages = new HashSet<Integer>();

    // Displayables (Chat box, etc.)
    ArrayList<IDisplayable> displayables = new ArrayList<IDisplayable>();

    // Move units step by step
    private ArrayList<MovingUnit> movingUnits = new ArrayList<MovingUnit>();

    private long moveWait = 0;

    // moving entity sprites
    private List<MovingEntitySprite> movingEntitySprites = new ArrayList<>();
    private HashMap<Integer, MovingEntitySprite> movingEntitySpriteIds = new HashMap<>();
    private ArrayList<GhostEntitySprite> ghostEntitySprites = new ArrayList<>();

    protected transient ArrayList<BoardViewListener> boardListeners = new ArrayList<>();

    // wreck sprites
    private ArrayList<WreckSprite> wreckSprites = new ArrayList<WreckSprite>();
    private ArrayList<IsometricWreckSprite> isometricWreckSprites = new ArrayList<>();

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
    private Map<Coords, Color> ecmHexes = null;
    // hexes that are teh centers of ECM effects
    private Map<Coords, Color> ecmCenters = null;
    // hexes with ECM effect
    private Map<Coords, Color> eccmHexes = null;
    // hexes that are teh centers of ECCM effects
    private Map<Coords, Color> eccmCenters = null;

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

    /**
     * Keeps track of whether an outside source tells the BoardView that it
     * should ignore keyboard commands.
     */
    private boolean shouldIgnoreKeys = false;

    FovHighlightingAndDarkening fovHighlightingAndDarkening;

    private String FILENAME_FLARE_IMAGE = "flare.png";

    private String FILENAME_RADAR_BLIP_IMAGE = "radarBlip.png";

    private Image flareImage;

    private Image radarBlipImage;

    /**
    * Cache that stores hex images for different coords
    */
    ImageCache<Coords, HexImageCacheEntry> hexImageCache;
    
    
    /**
     * Keeps track of whether all deployment zones should
     * be shown in the Arty Auto Hit Designation phase
     */
    public boolean showAllDeployment = false;

    private long paintCompsStartTime;

    private Rectangle displayablesRect = new Rectangle();


    /**
     * Construct a new board view for the specified game
     */
    public BoardView1(final IGame game, final MegaMekController controller)
            throws java.io.IOException {
        this.game = game;

        hexImageCache = new ImageCache<Coords, HexImageCacheEntry>();

        tileManager = new TilesetManager(this);
        ToolTipManager.sharedInstance().registerComponent(this);

        game.addGameListener(gameListener);
        game.getBoard().addBoardListener(this);
        ourTask = scheduleRedrawTimer();// call only once
        clearSprites();
        addMouseListener(this);
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent we) {
                Point mousePoint = we.getPoint();
                Point dispPoint = new Point();
                dispPoint.setLocation(mousePoint.x + getBounds().x,
                        mousePoint.y + getBounds().y);
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
                // calculate a few things to reposition the map
                Coords zoomCenter = getCoordsAt(we.getPoint());
                Point hexL = getCentreHexLocation(zoomCenter);
                Point inhexDelta = new Point(we.getPoint());
                inhexDelta.translate(-HEX_W, -HEX_H);
                inhexDelta.translate(-hexL.x, -hexL.y);
                double ihdx = ((double)inhexDelta.x)/((double)HEX_W)/scale;
                double ihdy = ((double)inhexDelta.y)/((double)HEX_H)/scale;
                int oldzoomIndex = zoomIndex;
                
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
                    if (zoomIndex != oldzoomIndex)
                        adjustVisiblePosition(zoomCenter, dispPoint, ihdx, ihdy);
                    
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
                        if (zoomIndex != oldzoomIndex)
                            adjustVisiblePosition(zoomCenter, dispPoint, ihdx, ihdy);
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
                pingMinimap();
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
        radarBlipImage = getToolkit().getImage(
                new File(Configuration.miscImagesDir(),
                        FILENAME_RADAR_BLIP_IMAGE).toString());
    }

    private void registerKeyboardCommands(final BoardView1 bv,
            final MegaMekController controller) {
        // Register the action for TOGGLE_ISO
        controller.registerCommandAction(KeyCommandBind.TOGGLE_ISO.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        GUIPreferences guip = GUIPreferences.getInstance();
                        guip.setIsometricEnabled(toggleIsometric());
                    }

                });

        // Register the action for TOGGLE_CHAT
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (!getChatterBoxActive()) {
                            setChatterBoxActive(true);
                            for (IDisplayable disp : displayables) {
                                if (disp instanceof ChatterBox2) {
                                    ((ChatterBox2) disp).slideUp();
                                }
                            }
                            requestFocus();
                        }
                    }

                });

        // Register the action for TOGGLE_CHAT
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT_CMD.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        if (!getChatterBoxActive()) {
                            setChatterBoxActive(true);
                            for (IDisplayable disp : displayables) {
                                if (disp instanceof ChatterBox2) {
                                    ((ChatterBox2) disp).slideUp();
                                    ((ChatterBox2) disp).setMessage("/");
                                }
                            }
                            requestFocus();
                        }
                    }

                });

        // Register the action for CENTER_ON_SELECTED
        controller.registerCommandAction(KeyCommandBind.CENTER_ON_SELECTED.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands() || (selectedEntity == null)) {
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
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_SOUTH);
                        vbar.setValue((int) (vbar.getValue() - (HEX_H * scale)));
                        pingMinimap();
                    }

                });

        // Register the action for SCROLL_SOUTH
        controller.registerCommandAction(KeyCommandBind.SCROLL_SOUTH.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_NORTH);
                        vbar.setValue((int) (vbar.getValue() + (HEX_H * scale)));
                        pingMinimap();
                    }

                });

        // Register the action for SCROLL_EAST
        controller.registerCommandAction(KeyCommandBind.SCROLL_EAST.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_WEST);
                        hbar.setValue((int) (hbar.getValue() + (HEX_W * scale)));
                        pingMinimap();
                    }

                });

        // Register the action for SCROLL_WEST
        controller.registerCommandAction(KeyCommandBind.SCROLL_WEST.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        controller.stopRepeating(KeyCommandBind.SCROLL_EAST);
                        hbar.setValue((int) (hbar.getValue() - (HEX_W * scale)));
                        pingMinimap();
                    }

                });
        
        // Register the action for Showing the Field of Fire
        controller.registerCommandAction(KeyCommandBind.FIELD_FIRE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (shouldIgnoreKeyCommands()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        GUIPreferences guip = GUIPreferences.getInstance();
                        guip.setShowFieldOfFire(!guip.getShowFieldOfFire());
                        repaint();
                    }

                });
    }

    private boolean shouldIgnoreKeyCommands() {
        return getChatterBoxActive() || !isVisible()
               || (game.getPhase() == Phase.PHASE_LOUNGE)
               || (game.getPhase() == Phase.PHASE_END_REPORT)
               || (game.getPhase() == Phase.PHASE_MOVEMENT_REPORT)
               || (game.getPhase() == Phase.PHASE_TARGETING_REPORT)
               || (game.getPhase() == Phase.PHASE_FIRING_REPORT)
               || (game.getPhase() == Phase.PHASE_PHYSICAL_REPORT)
               || (game.getPhase() == Phase.PHASE_OFFBOARD_REPORT)
               || (game.getPhase() == Phase.PHASE_INITIATIVE_REPORT)
               || shouldIgnoreKeys;
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
     * @param listener the board listener.
     */
    public void addBoardViewListener(BoardViewListener listener) {
        if (!boardListeners.contains(listener)) {
            boardListeners.add(listener);
        }
    }

    /**
     * Removes the specified board listener.
     *
     * @param listener the board listener.
     */
    public void removeBoardViewListener(BoardViewListener listener) {
        boardListeners.remove(listener);
    }

    /**
     * Notifies attached board listeners of the event.
     *
     * @param event the board event.
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

        if (guip.getBoolean(GUIPreferences.ADVANCED_SHOW_FPS)) {
            paintCompsStartTime = System.nanoTime();
        }

        if (guip.getAntiAliasing()) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
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
        if ((bvBgBuffer == null) || (bvBgBuffer.getWidth() != viewRect.getWidth())
            || (bvBgBuffer.getHeight() != viewRect.getHeight())) {
            pingMinimap();
            bvBgBuffer = new BufferedImage((int) viewRect.getWidth(),
                                           (int) viewRect.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics bgGraph = bvBgBuffer.getGraphics();
            if (bvBgIcon != null) {
                int w = (int) viewRect.getWidth();
                int h = (int) viewRect.getHeight();
                int iW = bvBgIcon.getIconWidth();
                int iH = bvBgIcon.getIconHeight();

                for (int x = 0; x < w; x += iW) {
                    for (int y = 0; y < h; y += iH) {
                        bgGraph.drawImage(bvBgIcon.getImage(), x, y,
                                          bvBgIcon.getImageObserver());
                    }
                }
            }
        }
        g.drawImage(bvBgBuffer, g.getClipBounds().x, g.getClipBounds().y, null);

        // Used to pad the board edge
        g.translate(HEX_W, HEX_H);
        
        // Initialize the shadow map when its not yet present
        if (ShadowMap == null) {
            updateShadowMap();
        }

        drawHexes(g, g.getClipBounds());

        // draw wrecks
        if (guip.getShowWrecks() && !useIsometric()) {
            drawSprites(g, wreckSprites);
        }
        
        // Field of Fire
        if (!useIsometric()
                && GUIPreferences.getInstance().getShowFieldOfFire()) {
            drawSprites(g, fieldofFireSprites);
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
        
        if ((game.getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES)
                && (showAllDeployment)) {
            drawAllDeployment(g);
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

        if (game.getPhase() == Phase.PHASE_FIRING) {
            for (Coords c : strafingCoords) {
                drawHexBorder(g, getHexLocation(c), Color.yellow, 0, 3);
            }
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
        if (displayablesRect == null) {
            displayablesRect = new Rectangle();
        }
        displayablesRect.x = -getX();
        displayablesRect.y = -getY();
        displayablesRect.width = scrollpane.getViewport().getViewRect().width;
        displayablesRect.height = scrollpane.getViewport().getViewRect().height;
        for (int i = 0; i < displayables.size(); i++) {
            IDisplayable disp = displayables.get(i);
            disp.draw(g, displayablesRect);
        }

        if (guip.getBoolean(GUIPreferences.ADVANCED_SHOW_FPS)) {
            if (frameCount == FRAMES) {
                averageTime = totalTime / FRAMES;
                totalTime = 0;
                frameCount = 0;
            } else {
                totalTime += System.nanoTime() - paintCompsStartTime;
                frameCount++;
            }
            String s = String.format("%1$5.3f", averageTime / 1000000d);
            g.setFont(fpsFont);
            g.setColor(Color.YELLOW);
            g.drawString(s, g.getClipBounds().x + 5, g.getClipBounds().y + 20);
        }
    }
    
    /**
     *  Returns a list of Coords of all hexes on the board.
     *  Returns ONLY hexes where board.getHex != null.
     */
    private ArrayList<Coords> allBoardHexes() {
        IBoard board = game.getBoard();
        if (board == null) return null;
        
        ArrayList<Coords> CoordList = new ArrayList<Coords>();
        for (int i = 0; i < board.getWidth(); i++) {
            for (int j = 0; j < board.getHeight(); j++) {
                IHex hex = board.getHex(i, j);
                if (hex != null) {
                    CoordList.add(new Coords(i, j));
                }
            }
        }
        
        return CoordList;
    }

    /**
     *  Prepares a shadow map for the board, drawing shadows for hills/trees/buildings.
     *  The shadow map is an image the size of the whole board.
     */
    private void updateShadowMap() {
        // Issues: 
        // Bridge shadows show a gap towards connected hexes. I don't know why.
        // More than one super image on a hex (building+road) doesnt work. how do I get
        //   the super for a hex for a specific terrain? This would also help
        //   with building shadowing other buildings.
        // AO shadows might be handled by this too. But: 
        // this seems to need a lot of additional copying (paint shadow on a clean map for this level alone; soften up; copy to real shadow
        // map with clipping area active; get new clean shadow map for next shadowed level; 
        // too much hassle currently; it works so beautifully
        if (!GUIPreferences.getInstance().getShadowMap()) return;
        
        IBoard board = game.getBoard();
        if (board == null) return;
        if (board.inSpace()) return;
        if (boardSize == null) updateBoardSize();
        if (!isTileImagesLoaded()) return;
        // Map editor? No shadows
        if (game.getPhase() == IGame.Phase.PHASE_UNKNOWN) return;
        
        // the shadowmap needs to be painted as if scale == 1
        // therefore some of the methods of boardview1 cannot be used
        int width = game.getBoard().getWidth() * HEX_WC + (int) (HEX_W / 4);
        int height = game.getBoard().getHeight() * (int) (HEX_H) + (int) (HEX_H / 2);
        
        GraphicsConfiguration config = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
        
        ShadowMap = config.createCompatibleImage(width, height,
                Transparency.TRANSLUCENT);
        
        Graphics2D g = (Graphics2D)(ShadowMap.createGraphics());
        
        // Shadows for elevation
        // 1) Sort the board hexes by elevation
        HashMap<Integer,ArrayList<Coords>> sortedHexes = new HashMap<Integer,ArrayList<Coords>>();
        for (Coords c: allBoardHexes()) {
            IHex hex = board.getHex(c);
            int level = hex.getLevel();
            if (sortedHexes.get(level) == null) { // no hexes yet for this height
                sortedHexes.put(level, new ArrayList<Coords>());
            }
            sortedHexes.get(level).add(c);
        }

        // 2) Create clipping areas
        HashMap<Integer,Area> levelClips = new HashMap<Integer,Area>();
        for (Integer h: sortedHexes.keySet()) {
            for (Coords c: sortedHexes.get(h)) {
                if (levelClips.get(h) == null) { // no area yet for this level
                    levelClips.put(h, new Area());
                }
                Point p = getHexLocationLargeTile(c.getX(), c.getY(), 1);
                AffineTransform t = AffineTransform.getTranslateInstance(p.x+42, p.y+36);
                t.scale(1.02, 1.02);
                t.translate(-42, -36);
                Area addHex = new Area(t.createTransformedShape(hexPoly));
                Area fullArea = levelClips.get(h);
                fullArea.add(addHex);
                levelClips.put(h, fullArea);
            }
        }

        // Create a hex-shaped shadow mask
        BufferedImage hexMask = config.createCompatibleImage(HEX_W, HEX_H,
                Transparency.TRANSLUCENT);
        
        Graphics2D gHM = (Graphics2D)(hexMask.createGraphics());
        gHM.fillPolygon(hexPoly);
        gHM.dispose();
        Image hexShadow = createShadowMask(hexMask);

        // 3) Draw shadows
        for (int shadowcaster = board.getMinElevation(); 
                shadowcaster <= board.getMaxElevation(); 
                shadowcaster++) {
            if (levelClips.get(shadowcaster) == null) continue;

            for (int shadowed = board.getMinElevation(); 
                    shadowed <= board.getMaxElevation(); 
                    shadowed++) {
                if (levelClips.get(shadowed) == null) continue;

                Shape saveClip = g.getClip();
                g.setClip(levelClips.get(shadowed));

                for (Coords c: sortedHexes.get(shadowcaster)) {
                    Point2D p0 = getHexLocationLargeTile(c.getX(), c.getY(), 1);
                    double deltaX = LightDirection[0]/10;
                    double deltaY = LightDirection[1]/10;
                    Point2D p1 = new Point2D.Double();
                    
                    // Elevation Shadow
                    if (shadowcaster > shadowed) {
                        p1.setLocation(p0);
                        for (int i = 0; i<10*(shadowcaster-shadowed); i++) {
                            g.drawImage(hexShadow, (int)p1.getX(), (int)p1.getY(), null);
                            p1.setLocation(p1.getX()+deltaX, p1.getY()+deltaY);
                        }
                    }
                    
                    // Woods Shadow
                    IHex hex = board.getHex(c);
                    List<Image> supers = tileManager.supersFor(hex);

                    if (!supers.isEmpty()) {
                        Image lastSuper = supers.get(supers.size()-1);
                        if (lastSuper.getWidth(null) == -1) {
                            clearShadowMap();
                            return;
                        }
                        Image mask = createShadowMask(lastSuper);

                        if (hex.containsTerrain(Terrains.WOODS) ||
                                hex.containsTerrain(Terrains.JUNGLE)) {
                            // Woods are 2 levels high, but then shadows
                            // appear very extreme, therefore only 
                            // 1.5 levels: (shadowcaster+1.5-shadowed)
                            p1.setLocation(p0);
                            if ((shadowcaster+1.5-shadowed) > 0) {
                                for (int i = 0; i<10*(shadowcaster+1.5-shadowed); i++) {
                                    g.drawImage(mask, (int)p1.getX(), (int)p1.getY(), null);
                                    p1.setLocation(p1.getX()+deltaX, p1.getY()+deltaY);
                                }
                            }
                        }

                        // Buildings Shadow
                        if (hex.containsTerrain(Terrains.BUILDING))
                        {
                            int h = hex.terrainLevel(Terrains.BLDG_ELEV);
                            if ((shadowcaster+h-shadowed) > 0) {
                                p1.setLocation(p0);
                                for (int i = 0; i<10*(shadowcaster+h-shadowed); i++) {
                                    g.drawImage(mask, (int)p1.getX(), (int)p1.getY(), null);
                                    p1.setLocation(p1.getX()+deltaX, p1.getY()+deltaY);
                                }
                            }
                        }
                    }


                    // Bridge Shadow
                    if (hex.containsTerrain(Terrains.BRIDGE)) {
                        supers = tileManager.orthoFor(hex);
                        if (supers.isEmpty()) break; 
                        Image maskB = supers.get(supers.size()-1);
                        if (maskB.getWidth(null) == -1) {
                            clearShadowMap();
                            return;
                        }
                        Image mask = createShadowMask(maskB);
                        int h = hex.terrainLevel(Terrains.BRIDGE_ELEV);
                        p1.setLocation(p0.getX()+deltaX*10*(shadowcaster+h-shadowed), 
                                p0.getY()+deltaY*10*(shadowcaster+h-shadowed));
                        // the shadowmask is translucent, therefore draw 10 times
                        // stupid hack
                        for (int i=0;i<10;i++)
                            g.drawImage(mask, (int)p1.getX(), (int)p1.getY(), null);
                    }

                }
                g.setClip(saveClip);
            }
        }

        // 4) Soften up the shadows
        Kernel kernel = new Kernel(5, 5,
                new float[] {
                        1f/25f, 1f/25f, 1f/25f, 1f/25f, 1f/25f,
                        1f/25f, 1f/25f, 1f/25f, 1f/25f, 1f/25f,
                        1f/25f, 1f/25f, 1f/25f, 1f/25f, 1f/25f,
                        1f/25f, 1f/25f, 1f/25f, 1f/25f, 1f/25f,
                        1f/25f, 1f/25f, 1f/25f, 1f/25f, 1f/25f});
        BufferedImageOp op = new ConvolveOp(kernel);
        ShadowMap = op.filter(ShadowMap, null);
        ShadowMap = op.filter(ShadowMap, null); // soft, soft
    }
    
    public void clearShadowMap() {
        ShadowMap = null;
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
            List<? extends Sprite> spriteArrayList) {
        for (Sprite sprite : spriteArrayList) {
            drawSprite(g, sprite);
        }
    }

    private synchronized void drawHexSpritesForHex(Coords c, Graphics g,
            ArrayList<? extends HexSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        
        for (HexSprite sprite : spriteArrayList) {
            Coords cp = sprite.getPosition();
            // This can potentially be an expensive operation
            Rectangle spriteBounds = sprite.getBounds();
            if (cp.equals(c) && view.intersects(spriteBounds)
                    && !sprite.isHidden()) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, spriteBounds.x, spriteBounds.y,
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
     * @param c               The Coordinates of the hex that the sprites should be drawn
     *                        for.
     * @param g               The Graphics object for this board.
     * @param spriteArrayList The complete list of all IsometricSprite on the board.
     */
    private synchronized void drawIsometricSpritesForHex(Coords c, Graphics g,
            List<IsometricSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricSprite sprite : spriteArrayList) {
            Coords cp = sprite.getPosition();
            // This can potentially be an expensive operation
            Rectangle spriteBounds = sprite.getBounds();
            if (cp.equals(c) && view.intersects(spriteBounds)
                && !sprite.isHidden()) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, spriteBounds.x, spriteBounds.y, this, false);
            }
        }
    }

    /**
     * Draws the wrecksprites for the given hex. This function is used by the
     * isometric rendering process so that sprites are drawn in the order that
     * hills are rendered to create the appearance that the sprite is behind the
     * hill.
     *
     * @param c               The Coordinates of the hex that the sprites should be drawn
     *                        for.
     * @param g               The Graphics object for this board.
     * @param spriteArrayList The complete list of all IsometricSprite on the board.
     */
    private synchronized void drawIsometricWreckSpritesForHex(Coords c,
            Graphics g, ArrayList<IsometricWreckSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricWreckSprite sprite : spriteArrayList) {
            Coords cp = sprite.getEntity().getPosition();
            if (cp.equals(c) && view.intersects(sprite.getBounds())
                && !sprite.isHidden()) {
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
     * <p/>
     * TODO: Optimize this function so that it is only applied to sprites that
     * are actually hidden. This implementation performs the second rendering
     * for all sprites.
     */
    private final void drawIsometricSprites(Graphics g,
            List<IsometricSprite> spriteArrayList) {
        Rectangle view = g.getClipBounds();
        for (IsometricSprite sprite : spriteArrayList) {
            // This can potentially be an expensive operation
            Rectangle spriteBounds = sprite.getBounds();
            if (view.intersects(spriteBounds) && !sprite.isHidden()) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(g, spriteBounds.x, spriteBounds.y,
                                this, true);
            }
        }
    }

    /**
     * Draws a sprite, if it is in the current view
     */
    private final void drawSprite(Graphics g, Sprite sprite) {
        Rectangle view = g.getClipBounds();
        // This can potentially be an expensive operation
        Rectangle spriteBounds = sprite.getBounds();
        if (view.intersects(spriteBounds) && !sprite.isHidden()) {
            if (!sprite.isReady()) {
                sprite.prepare();
            }
            sprite.drawOnto(g, spriteBounds.x, spriteBounds.y, this);
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
                    drawHexBorder(g, getHexLocation(c), Color.yellow);
                }
            }
        }
    }
    
    /**
     * Draw indicators for the deployment zones of all players
     */
    private void drawAllDeployment(Graphics g) {
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
                Enumeration<IPlayer> allP = game.getPlayers();
                IPlayer cp;
                int pCount = 0;
                int bThickness = 1 + 10 / game.getNoOfPlayers();
                // loop through all players
                while (allP.hasMoreElements()) {
                    cp = allP.nextElement();
                    if (board.isLegalDeployment(c, cp.getStartingPos())) {
                        Color bC = new Color(PlayerColors.getColorRGB(cp.getColorIndex()));
                        drawHexBorder(g, getHexLocation(c), bC, (bThickness+2)
                                * pCount, bThickness);
                        pCount++;
                    }
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
    void drawHexLayer(Point p, Graphics g, Color col, boolean outOfFOV) {
        drawHexLayer(p, g, col, outOfFOV, 0);
    }

    /**
     * Draw a layer of a solid color (alpha possible) on the hex at Point p with
     * some padding around the border
     */
    private void drawHexLayer(Point p, Graphics g, Color col, boolean outOfFOV,
            double pad) {
        g.setColor(col);

        // create stripe effect for FOV darkening but not for colored weapon
        // ranges
        int fogStripes = GUIPreferences.getInstance().getFovStripes();
        if (outOfFOV && (fogStripes > 0) && (g instanceof Graphics2D)) {
            float lineSpacing = fogStripes;
            // totally transparent here hurts the eyes
            Color c2 = new Color(col.getRed() / 2, col.getGreen() / 2,
                    col.getBlue() / 2, col.getAlpha() / 2); 

            // the numbers make the lines align across hexes
            GradientPaint gp = new GradientPaint(42.0f / lineSpacing, 0.0f,
                    col, 104.0f / lineSpacing, 106.0f / lineSpacing, c2, true);
            ((Graphics2D)g).setPaint(gp);
        }
        
        ((Graphics2D)g).fill(
                AffineTransform.getTranslateInstance(p.x, p.y).createTransformedShape(
                AffineTransform.getScaleInstance(scale, scale).createTransformedShape(
                        HexDrawUtilities.getHexFullBorderLine(pad))));
    }
    
    private void drawHexBorder(Graphics g, Color col, double pad,
            double linewidth) {
        drawHexBorder(g, new Point(0,0), col, pad, linewidth);
    }
    
    public void drawHexBorder(Graphics g, Point p, Color col, double pad,
            double linewidth) {
        g.setColor(col);
        ((Graphics2D)g).fill(
                AffineTransform.getTranslateInstance(p.x, p.y).createTransformedShape(
                AffineTransform.getScaleInstance(scale, scale).createTransformedShape(
                HexDrawUtilities.getHexFullBorderArea(linewidth, pad))));
    }

    /**
     * Draw an outline around the hex at Point p no padding and a width of 1
     */
    private void drawHexBorder(Graphics g, Point p, Color col) {
        drawHexBorder(g, p, col, 0);
    }

    /**
     * Draw an outline around the hex at Point p padded around the border by pad
     * and a line-width of 1
     */
    private void drawHexBorder(Graphics g, Point p, Color col, double pad) {
        drawHexBorder(g, p, col, pad, 1);
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

        Image artyIconImage;

        // Draw incoming artillery sprites - requires server to update client's
        // view of game
        for (Enumeration<ArtilleryAttackAction> attacks = game
                .getArtilleryAttacks(); attacks.hasMoreElements(); ) {
            ArtilleryAttackAction a = attacks.nextElement();
            Coords c = a.getTarget(game).getPosition();
            // Is the Coord within the viewing area?
            if ((c.getX() >= drawX) && (c.getX() <= (drawX + drawWidth))
                && (c.getY() >= drawY) && (c.getY() <= (drawY + drawHeight))) {

                Point p = getHexLocation(c);
                artyIconImage = tileManager
                        .getArtilleryTarget(TilesetManager.ARTILLERY_INCOMING);
                g.drawImage(getScaledImage(artyIconImage, true), p.x, p.y, this);
            }
        }

        // Draw pre-designated auto-hit hexes
        if (localPlayer != null) // Could be null, like in map-editor
        {
            for (Coords c : localPlayer.getArtyAutoHitHexes()) {
                // Is the Coord within the viewing area?
                if ((c.getX() >= drawX) && (c.getX() <= (drawX + drawWidth))
                    && (c.getY() >= drawY) && (c.getY() <= (drawY + drawHeight))) {

                    Point p = getHexLocation(c);
                    artyIconImage = tileManager
                            .getArtilleryTarget(TilesetManager.ARTILLERY_AUTOHIT);
                    g.drawImage(getScaledImage(artyIconImage, true), p.x, p.y, this);
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
                if ((c.getX() >= drawX) && (c.getX() <= (drawX + drawWidth))
                    && (c.getY() >= drawY) && (c.getY() <= (drawY + drawHeight))) {

                    Point p = getHexLocation(c);
                    // draw the crosshairs
                    if (attackMod.getModifier() == TargetRoll.AUTOMATIC_SUCCESS) {
                        // predesignated or already hit
                        artyIconImage = tileManager
                                .getArtilleryTarget(TilesetManager.ARTILLERY_AUTOHIT);
                    } else {
                        artyIconImage = tileManager
                                .getArtilleryTarget(TilesetManager.ARTILLERY_ADJUSTED);
                    }
                    g.drawImage(getScaledImage(artyIconImage, true), p.x, p.y, this);
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
             minedCoords.hasMoreElements(); ) {
            Coords c = minedCoords.nextElement();
            // If the coords aren't visible, skip
            if ((c.getX() < drawX) || (c.getX() > maxX) || (c.getY() < drawY) || (c.getY() > maxY)
                || !board.contains(c)) {
                continue;
            }

            Point p = getHexLocation(c);
            Image mineImg = getScaledImage(tileManager.getMinefieldSign(), true);
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
        Graphics2D boardGraph = (Graphics2D) entireBoard.getGraphics();
        if (GUIPreferences.getInstance().getAntiAliasing()) {
            boardGraph.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        }
        drawHexes(boardGraph, new Rectangle(boardSize), true);
        boardGraph.dispose();
        return (BufferedImage) entireBoard;
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

        // draw some hexes.
        if (useIsometric()) {
            // When using isometric rendering, hexes within a given row
            // must be drawn from lowest to highest elevation.
            IBoard board = game.getBoard();
            final int minElev = board.getMinElevation();
            final int maxElev = board.getMaxElevation();
            for (int i = 0; i < drawHeight; i++) {
                for (int x = minElev; x <= maxElev; x++) {
                    for (int j = 0; j < drawWidth; j++) {
                        Coords c = new Coords(j + drawX, i + drawY);
                        IHex hex = board.getHex(c);
                        if ((hex != null) && (hex.getLevel() == x)) {
                            drawHex(c, g, saveBoardImage);
                            if (GUIPreferences.getInstance()
                                    .getShowFieldOfFire()) {
                                drawHexSpritesForHex(c, g, fieldofFireSprites);
                            }
                            drawHexSpritesForHex(c, g, moveEnvSprites);
                            drawHexSpritesForHex(c, g, moveModEnvSprites);
                            if ((en_Deployer != null)
                                    && board.isLegalDeployment(c,
                                            en_Deployer.getStartingPos())) {
                                drawHexBorder(g, getHexLocation(c),
                                        Color.yellow);
                            }
                        }
                    }
                }
                for (int k = 0; k < drawWidth; k++) {
                    Coords c = new Coords(k + drawX, i + drawY);
                    IHex hex = board.getHex(c);
                    if (hex != null) {
                        drawOrthograph(c, g);
                        if (!saveBoardImage) {
                            drawIsometricWreckSpritesForHex(c, g,
                                    isometricWreckSprites);
                            drawIsometricSpritesForHex(c, g, isometricSprites);
                        }
                    }
                }
            }
            if (!saveBoardImage) {
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

        final GUIPreferences guip = GUIPreferences.getInstance();
        final IHex hex = game.getBoard().getHex(c);
        final Point hexLoc = getHexLocation(c);

        // Check the cache to see if we already have the image
        HexImageCacheEntry cacheEntry = hexImageCache.get(c);
        if ((cacheEntry != null) && !cacheEntry.needsUpdating) {
            boardGraph.drawImage(cacheEntry.hexImage, hexLoc.x, hexLoc.y, this);
            return;
        }

        int level = hex.getLevel();
        int depth = hex.depth(false);

        ITerrain basement = hex.getTerrain(Terrains.BLDG_BASEMENT_TYPE);
        if (basement != null) {
            depth = 0;
        }

        int height = Math.max(hex.terrainLevel(Terrains.BLDG_ELEV),
                              hex.terrainLevel(Terrains.BRIDGE_ELEV));
        height = Math.max(height, hex.terrainLevel(Terrains.INDUSTRIAL));

        // get the base tile image
        Image baseImage = tileManager.baseFor(hex);
        
        // Some hex images shouldn't be cached, like if they are animated
        boolean dontCache = animatedImages.contains(baseImage.hashCode());
        
        // check if this is a standard tile image 84x72 or something different
        boolean standardTile = (baseImage.getHeight(null) == HEX_H)
                && (baseImage.getWidth(null) == HEX_W);
        
        Image scaledImage = getScaledImage(baseImage, true);

        int imgHeight, imgWidth;
        imgWidth = scaledImage.getWidth(null);
        imgHeight = scaledImage.getHeight(null);
        
        // do not make larger than hex images even when the input image is big
        int origImgWidth = imgWidth; // save for later, needed for large tiles
        int origImgHeight = imgHeight;
        
        imgWidth = Math.min(imgWidth,(int)(HEX_W*scale));
        imgHeight = Math.min(imgHeight,(int)(HEX_H*scale));
        
        if (useIsometric()) {
            int largestLevelDiff = 0;
            for (int dir: allDirections) {
                IHex adjHex = game.getBoard().getHexInDir(c, dir);
                if (adjHex == null) {
                    continue;
                }
                int levelDiff = Math.abs(level - adjHex.getLevel());
                if (levelDiff > largestLevelDiff) {
                    largestLevelDiff = levelDiff;
                }
            }
            imgHeight += HEX_ELEV * scale * largestLevelDiff;
        }
        // If the base image isn't ready, we should signal a repaint and stop
        if ((imgWidth < 0) || (imgHeight < 0)) {
            repaint();
            return;
        }
        
        Image hexImage = new BufferedImage(imgWidth, imgHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D)(hexImage.getGraphics());
        GUIPreferences.AntiAliasifSet(g);
        
        if (standardTile) { // is the image hex-sized, 84*72?
            g.drawImage(scaledImage, 0, 0, this);
        } else { // Draw image for a texture larger than a hex
            AffineTransform t = new AffineTransform();
            // without the 1.02 unwanted hex borders will remain
            t.scale(scale * 1.02, scale * 1.02); 
            Shape clipShape = t.createTransformedShape(hexPoly);

            Shape saveclip = g.getClip();
            g.setClip(clipShape);

            Point p1SRC = getHexLocationLargeTile(c.getX(), c.getY());
            p1SRC.x = p1SRC.x % origImgWidth;
            p1SRC.y = p1SRC.y % origImgHeight;
            Point p2SRC = new Point((int) (p1SRC.x + HEX_W * scale),
                    (int) (p1SRC.y + HEX_H * scale));
            Point p2DST = new Point((int) (HEX_W * scale),
                    (int) (HEX_H * scale));
            // paint the right slice from the big pic
            g.drawImage(scaledImage, 0, 0, p2DST.x, p2DST.y, p1SRC.x, p1SRC.y,
                    p2SRC.x, p2SRC.y, null); 

            // Handle wrapping of the image
            if (p2SRC.x > origImgWidth && p2SRC.y <= origImgHeight) {
                g.drawImage(scaledImage, origImgWidth - p1SRC.x, 0, p2DST.x,
                        p2DST.y, 0, p1SRC.y, p2SRC.x - origImgWidth, p2SRC.y,
                        null); // paint addtl slice on the left side
            } else if (p2SRC.x <= origImgWidth && p2SRC.y > origImgHeight) {
                g.drawImage(scaledImage, 0, origImgHeight - p1SRC.y, p2DST.x,
                        p2DST.y, p1SRC.x, 0, p2SRC.x, p2SRC.y - origImgHeight,
                        null); // paint addtl slice on the top
            } else if (p2SRC.x > origImgWidth && p2SRC.y > origImgHeight) {
                g.drawImage(scaledImage, origImgWidth - p1SRC.x, 0, p2DST.x,
                        p2DST.y, 0, p1SRC.y, p2SRC.x - origImgWidth, p2SRC.y,
                        null); // paint addtl slice on the top
                g.drawImage(scaledImage, 0, origImgHeight - p1SRC.y, p2DST.x,
                        p2DST.y, p1SRC.x, 0, p2SRC.x, p2SRC.y - origImgHeight,
                        null); // paint addtl slice on the left side
             // paint addtl slice on the top left side
                g.drawImage(scaledImage, origImgWidth - p1SRC.x,
                        origImgHeight - p1SRC.y, p2DST.x, p2DST.y, 0, 0,
                        p2SRC.x - origImgWidth, p2SRC.y - origImgHeight, null); 
            }

            g.setClip(saveclip);
        }
        
        // To place roads under the shadow map, the supers for hexes
        // with roads have to be drawn before the shadow map, otherwise
        // the supers are drawn after
        // Unfortunately I dont think the supers images themselves can be checked for
        // roads.
        List<Image> supers = tileManager.supersFor(hex);
        boolean supersUnderShadow = false;
        if (hex.containsTerrain(Terrains.ROAD) ||
                hex.containsTerrain(Terrains.WATER)) {
            supersUnderShadow = true;
            if (supers != null) {
                for (Image image : supers) {
                    if (animatedImages.contains(image.hashCode())) {
                        dontCache = true;
                    }
                    scaledImage = getScaledImage(image, true);
                    g.drawImage(scaledImage, 0, 0, this);
                }
            }
        }
        
        if (guip.getBoolean(GUIPreferences.SHADOWMAP) &&  
            (ShadowMap != null)) {
            Image scaledShadow = getScaledImage(ShadowMap, true);
            
            AffineTransform t = new AffineTransform();
            // without the 1.02 unwanted hex borders will remain
            t.scale(scale * 1.02, scale * 1.02); 
            Shape clipShape = t.createTransformedShape(hexPoly);

            Shape saveclip = g.getClip();
            g.setClip(clipShape);
            
            int shWidth = scaledShadow.getWidth(null);
            int shHeight = scaledShadow.getHeight(null);

            Point p1SRC = getHexLocationLargeTile(c.getX(), c.getY());
            p1SRC.x = p1SRC.x % shWidth; 
            p1SRC.y = p1SRC.y % shHeight;
            Point p2SRC = new Point((int) (p1SRC.x + HEX_W * scale),
                    (int) (p1SRC.y + HEX_H * scale));
            Point p2DST = new Point((int) (HEX_W * scale),
                    (int) (HEX_H * scale));
            
            Composite svComp = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    0.45f));

            // paint the right slice from the big pic
            g.drawImage(scaledShadow, 0, 0, p2DST.x, p2DST.y, p1SRC.x, p1SRC.y,
                    p2SRC.x, p2SRC.y, null); 
            g.setClip(saveclip);
            g.setComposite(svComp);
        }

        if (!supersUnderShadow) {
            if (supers != null) {
                for (Image image : supers) {
                    if (animatedImages.contains(image.hashCode())) {
                        dontCache = true;
                    }
                    scaledImage = getScaledImage(image, true);
                    g.drawImage(scaledImage, 0, 0, this);
                }
            }
        }
        
        // AO Hex Shadow in this hex when a higher one is adjacent
        if (guip.getBoolean(GUIPreferences.AOHEXSHADOWS) ||
                guip.getBoolean(GUIPreferences.SHADOWMAP))   
        {
            for (int dir: allDirections) {
                Shape ShadowShape = getElevationShadowArea(c, dir);
                GradientPaint gpl = getElevationShadowGP(c, dir);
                if (ShadowShape != null && gpl != null) {
                    g.setPaint(gpl);
                    g.fill(getElevationShadowArea(c, dir));
                }
            }
        }

        // Orthos (bridges) 
        List<Image> orthos = tileManager.orthoFor(hex);
        if (orthos != null) {
            for (Image image : orthos) {
                if (animatedImages.contains(image.hashCode())) {
                    dontCache = true;
                }
                scaledImage = getScaledImage(image, true);
                if (!useIsometric()) {
                    g.drawImage(scaledImage, 0, 0, this);
                }
                // draw a shadow for bridge hex.
                if (useIsometric()
                    && (hex.terrainLevel(Terrains.BRIDGE_ELEV) > 0)) {
                    Image shadow = createShadowMask(scaledImage);
                    g.drawImage(shadow, 0, 0, this);
                }
            }
        }
        // Shade and add static noise to hexes that are in an ECM field
        if (ecmHexes != null) {
            Color tint = ecmHexes.get(c);
            if (tint != null) {
                Color origColor = g.getColor();
                g.setColor(tint);
                AffineTransform sc = new AffineTransform();
                sc.scale(scale, scale);
                g.fill(sc.createTransformedShape(hexPoly));
                g.setColor(origColor);
                Image staticImage = getScaledImage(tileManager.getEcmStaticImage(tint), false);
                g.drawImage(staticImage, 0, 0,
                        staticImage.getWidth(null),
                        staticImage.getHeight(null), this);
            }
        }
        // Shade hexes that are in an ECCM field
        if (eccmHexes != null) {
            Color tint = eccmHexes.get(c);
            if (tint != null) {
                Color origColor = g.getColor();
                g.setColor(tint);
                AffineTransform sc = new AffineTransform();
                sc.scale(scale, scale);
                g.fill(sc.createTransformedShape(hexPoly));
                g.setColor(origColor);
            }
        }
        // Highlight hexes that contain the source of an ECM field
        if (ecmCenters != null) {
            Color tint = ecmCenters.get(c);
            if (tint != null) {
                drawHexBorder(g, tint.darker(), 5, 10);
            }
        }
        
        // Highlight hexes that contain the source of an ECCM field
        if (eccmCenters != null) {
            Color tint = eccmCenters.get(c);
            if (tint != null) {
                drawHexBorder(g, tint.darker(), 5, 10);
            }
        }
        
        // Darken the hex image if nighttime 
        if (guip.getBoolean(GUIPreferences.ADVANCED_DARKEN_MAP_AT_NIGHT)
                && (game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DAY)
                && (game.isPositionIlluminated(c) == IGame.ILLUMINATED_NONE)) {
            scaledImage = getScaledImage(tileManager.getNightFog(), true);
            g.drawImage(scaledImage, 0, 0, this);
        }
        
        // Set the text color according to Preferences or Light Gray in space
        g.setColor(guip.getMapTextColor());
        if (game.getBoard().inSpace()) 
            g.setColor(Color.LIGHT_GRAY);

        // draw special stuff for the hex
        final Collection<SpecialHexDisplay> shdList = game.getBoard()
                .getSpecialHexDisplay(c);
        try {
            if (shdList != null) {
                for (SpecialHexDisplay shd : shdList) {
                    if (shd.drawNow(game.getPhase(), game.getRoundCount(),
                            localPlayer)) {
                        scaledImage = getScaledImage(shd.getType()
                                .getDefaultImage(), true);
                        g.drawImage(scaledImage, 0, 0, this);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal argument exception, probably "
                    + "can't load file.");
            e.printStackTrace();
            drawCenteredString("Loading Error", 0, 0
                    + (int) (50 * scale), font_note, g);
            return;
        }

        // write hex coordinate unless deactivated or scale factor too small
        if (guip.getBoolean(GUIPreferences.ADVANCED_SHOW_COORDS)
                && (scale >= 0.5)) {
            drawCenteredString(c.getBoardNum(), 0, 0
                    + (int) (12 * scale), font_hexnum, g);
        }

        // write terrain level / water depth / building height
        if (scale > 0.5f) {
            int ypos = HEX_H-2;
            if (level != 0) {
                drawCenteredString(
                        Messages.getString("BoardView1.LEVEL") + level, //$NON-NLS-1$
                        0, (int) (ypos * scale), font_elev, g);
                ypos -= 10;
            }
            if (depth != 0) {
                drawCenteredString(
                        Messages.getString("BoardView1.DEPTH") + depth, //$NON-NLS-1$
                        0, (int) (ypos * scale), font_elev, g);
                ypos -= 10;
            }
            if (height > 0) {
                g.setColor(GUIPreferences.getInstance().getColor(
                        "AdvancedBuildingTextColor"));                 //$NON-NLS-1$
                drawCenteredString(
                        Messages.getString("BoardView1.HEIGHT") + height, //$NON-NLS-1$
                        0, (int) (ypos * scale), font_elev, g);
                ypos -= 10;
            }
        }

        // Used to make the following draw calls shorter
        int s21 = (int)(21*scale);
        int s71 = (int)(71*scale);
        int s35 = (int)(35*scale);
        int s36 = (int)(36*scale);
        int s62 = (int)(62*scale);
        int s83 = (int)(83*scale);
        
        Point p1 = new Point(s62, 0);
        Point p2 = new Point(s21, 0);
        Point p3 = new Point(s83, s35);
        Point p4 = new Point(s83, s36);
        Point p5 = new Point(s62, s71);
        Point p6 = new Point(s21, s71);
        Point p7 = new Point(0, s36);
        Point p8 = new Point(0, s35);

        g.setColor(Color.black);

        // draw elevation borders
        if (drawElevationLine(c, 0)) {
            drawIsometricElevation(c, Color.GRAY, p1, p2, 0, g);
            if (guip.getBoolean(GUIPreferences.LEVELHIGHLIGHT)) {
                g.drawLine(s21, 0, s62, 0);
            }
        }

        if (drawElevationLine(c, 1)) {
            drawIsometricElevation(c, Color.DARK_GRAY, p3, p1, 1, g);
            if (guip.getBoolean(GUIPreferences.LEVELHIGHLIGHT)) {
                g.drawLine(s62, 0, s83, s35);
            }
        }

        if (drawElevationLine(c, 2)) {
            drawIsometricElevation(c, Color.LIGHT_GRAY, p4, p5, 2, g);
            if (guip.getBoolean(GUIPreferences.LEVELHIGHLIGHT)) {
                g.drawLine(s83, s36, s62, s71);
            }
        }

        if (drawElevationLine(c, 3)) {
            drawIsometricElevation(c, Color.GRAY, p6, p5, 3, g);
            if (guip.getBoolean(GUIPreferences.LEVELHIGHLIGHT)) {
                g.drawLine(s62, s71, s21, s71);
            }
        }

        if (drawElevationLine(c, 4)) {
            drawIsometricElevation(c, Color.DARK_GRAY, p7, p6, 4, g);
            if (guip.getBoolean(GUIPreferences.LEVELHIGHLIGHT)) {
                g.drawLine(s21, s71, 0, s36);
            }
        }

        if (drawElevationLine(c, 5)) {
            drawIsometricElevation(c, Color.LIGHT_GRAY, p8, p2, 5, g);
            if (guip.getBoolean(GUIPreferences.LEVELHIGHLIGHT)) {
                g.drawLine(0, s35, s21, 0);
            }

        }

        boolean hasLoS = fovHighlightingAndDarkening.draw(g, c, 0, 0,
                saveBoardImage);

        // draw mapsheet borders
        if (GUIPreferences.getInstance().getShowMapsheets()) {
            g.setColor(GUIPreferences.getInstance().getColor(
                    GUIPreferences.ADVANCED_MAPSHEET_COLOR));
            if ((c.getX() % 16) == 0) {
                // left edge of sheet (edge 4 & 5)
                g.drawLine(s21, s71, 0, s36);
                g.drawLine(0, s35, s21, 0);
            } else if ((c.getX() % 16) == 15) {
                // right edge of sheet (edge 1 & 2)
                g.drawLine(s62, 0, s83, s35);
                g.drawLine(s83, s36, s62, s71);
            }
            if ((c.getY() % 17) == 0) {
                // top edge of sheet (edge 0 and possible 1 & 5)
                g.drawLine(s21, 0, s62, 0);
                if ((c.getX() % 2) == 0) {
                    g.drawLine(s62, 0, s83, s35);
                    g.drawLine(0, s35, s21, 0);
                }
            } else if ((c.getY() % 17) == 16) {
                // bottom edge of sheet (edge 3 and possible 2 & 4)
                g.drawLine(s62, s71, s21, s71);
                if ((c.getX() % 2) == 1) {
                    g.drawLine(s83, s36, s62, s71);
                    g.drawLine(s21, s71, 0, s36);
                }
            }
            g.setColor(Color.black);
        }
        
        if (!hasLoS && guip.getFovGrayscale()) {
            // write in grayscale (does not support alpha)
            BufferedImage GrayedOut = new BufferedImage(imgWidth, imgHeight,
                    BufferedImage.TYPE_BYTE_GRAY);
            Graphics gG = GrayedOut.getGraphics();
            gG.drawImage(hexImage, 0, 0, null);
            gG.dispose();

            // initialize clipping shape for Gray Visifog, this will leave
            // isometric shapes intact as only the hexagon is overwritten in
            // grayscale
            AffineTransform t = new AffineTransform();
            t.scale(scale, scale);
            g.setClip(t.createTransformedShape(hexPoly));

            // write back to hexImage
            g.drawImage(GrayedOut, 0, 0, null);
        }        

        cacheEntry = new HexImageCacheEntry(hexImage);
        if (!dontCache) {
            hexImageCache.put(c, cacheEntry);
        }
        boardGraph.drawImage(cacheEntry.hexImage, hexLoc.x, hexLoc.y, this);
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

        // We need to adjust the height based on several cases
        int elevOffset = oHex.terrainLevel(Terrains.BRIDGE_ELEV);

        int orthX = oHexLoc.x;
        int orthY = oHexLoc.y - (int) (HEX_ELEV * scale * elevOffset);
        if (!useIsometric()) {
            orthY = oHexLoc.y;
        }
        if (tileManager.orthoFor(oHex) != null) {
            for (Image image : tileManager.orthoFor(oHex)) {
                Image scaledImage = getScaledImage(image, true);

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
     * <p/>
     * By drawing the elevated hex as two separate triangles we avoid clipping
     * problems with other hexes because the lower elevation is rendered before
     * the higher elevation. Thus any hexes that have a higher elevation than
     * the lower hex will overwrite the lower hex.
     * <p/>
     * The Triangle for each hex side is formed by points p1, p2, and p3. Where
     * p1 and p2 are the original hex edges, and p3 has the same X value as p1,
     * but the y value has been increased (or decreased) based on the difference
     * in elevation between the given hex and the adjacent hex.
     *
     * @param c          Coordinates of the source hex.
     * @param color      Color to use for the elevation polygons.
     * @param p1         The First point on the edge of the hex.
     * @param p2         The second point on the edge of the hex.
     * @param dir        The side of the hex to have the elevation drawn on.
     * @param g
     */
    private final void drawIsometricElevation(Coords c, Color color, Point p1,
            Point p2, int dir, Graphics g) {
        final IHex dest = game.getBoard().getHexInDir(c, dir);
        final IHex src = game.getBoard().getHex(c);

        if (!useIsometric() || 
                GUIPreferences.getInstance().getBoolean(GUIPreferences.FLOATINGISO)) {
            return;
        }

        // Pad polygon size slightly to avoid rounding errors from scale float.
        int fudge = -1;
        if ((dir == 2) || (dir == 4) || (dir == 3)) {
            fudge = 1;
        }

        final int elev = src.getLevel();
        // If the Destination is null, draw the complete elevation side.
        if ((dest == null) && (elev > 0)
            && ((dir == 2) || (dir == 3) || (dir == 4))) {

            // Determine the depth of the edge that needs to be drawn.
            int height = elev;
            IHex southHex = game.getBoard().getHexInDir(c, 3);
            if ((dir != 3) && (southHex != null)
                && (elev > southHex.getLevel())) {
                height = elev - southHex.getLevel();
            }
            int scaledHeight = (int) (HEX_ELEV * scale * height);

            Polygon p = new Polygon(new int[] { p1.x, p2.x, p2.x, p1.x },
                    new int[] { p1.y + fudge, p2.y + fudge,
                            p2.y + scaledHeight, p1.y + scaledHeight }, 4);
            g.setColor(color);
            g.drawPolygon(p);
            g.fillPolygon(p);

            g.setColor(Color.BLACK);
            if ((dir == 2) || (dir == 4)) {
                g.drawLine(p1.x, p1.y, p1.x, p1.y + scaledHeight);
            }
            return;
        } else if (dest == null) {
            return;
        }

        int delta = elev - dest.getLevel();
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
            if ((p2.x - (int) (HEX_ELEV * scale)) < 0) {
                System.out.println("Negative X value!: " + (p2.x - (int) (HEX_ELEV * scale)));
            }
            g.setColor(new Color(0, 0, 0, 0.4f));
            g.fillPolygon(shadow1);
        }

        if ((dir == 2) || (dir == 3) || (dir == 4)) {
            int scaledDelta = (int) (HEX_ELEV * scale * delta);
            Point p3 = new Point(p1.x, p1.y + scaledDelta + fudge);

            Polygon p = new Polygon(new int[] { p1.x, p2.x, p2.x, p1.x },
                    new int[] { p1.y + fudge, p2.y + fudge,
                            p2.y + fudge + scaledDelta,
                            p1.y + fudge + scaledDelta }, 4);

            if ((p1.y + fudge) < 0) {
                System.out.println("Negative Y value (Fudge)!: " + (p1.y + fudge));
            }
            if ((p2.y + fudge) < 0) {
                System.out.println("Negative Y value (Fudge)!: " + (p2.y + fudge));
            }

            if ((p2.y + fudge + scaledDelta) < 0) {
                System.out.println("Negative Y value!: " + (p2.y + fudge + scaledDelta));
            }
            if (( p1.y + fudge + scaledDelta) < 0) {
                System.out.println("Negative Y value!: " + ( p1.y + fudge + scaledDelta));
            }
            g.setColor(color);
            g.drawPolygon(p);
            g.fillPolygon(p);

            g.setColor(Color.BLACK);
            if ((dir == 1) || (dir == 2) || (dir == 5) || (dir == 4)) {
                g.drawLine(p1.x, p1.y, p3.x, p3.y);
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
        if ((destHex == null) && (srcHex.getLevel() != 0)) {
            return true;
        } else if (destHex == null) {
            return false;
        } else if (srcHex.getLevel() != destHex.getLevel()) {
            return true;
        } else {
            return (srcHex.floor() != destHex.floor());
        }
    }
    
    /**
     * Generates a Shape drawing area for the hex shadow effect in a lower hex
     * when a higher hex is found in direction. 
     */
    private final Shape getElevationShadowArea(Coords src, int direction) {
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHexInDir(src, direction);
        
        // When at the board edge, create a shadow in hexes of level < 0
        if (destHex == null)
        {
            if (srcHex.getLevel() >= 0) return null; 
        }
        else
        {
            // no shadow area when the current hex is not lower than the next hex in direction
            if (srcHex.getLevel() >= destHex.getLevel()) return null;
        }

        return(AffineTransform.getScaleInstance(scale, scale).createTransformedShape(
                HexDrawUtilities.getHexBorderArea(direction, HexDrawUtilities.CUT_BORDER, 36)));
    }
    
    /**
     * Generates a fill gradient which is rotated and aligned properly for 
     * the drawing area for a hex shadow effect in a lower hex.
     */
    private final GradientPaint getElevationShadowGP(Coords src, int direction) {
        final IHex srcHex = game.getBoard().getHex(src);
        final IHex destHex = game.getBoard().getHexInDir(src, direction);
        
        if (destHex == null) return null;  
        
        int ldiff = destHex.getLevel()-srcHex.getLevel();
        // the shadow strength depends on the level difference,
        // but only to a maximum difference of 3 levels
        ldiff = Math.min(ldiff*5,15);
        
        Color c1 = new Color(30,30,50,255); // dark end of shadow
        Color c2 = new Color(50,50,70,0);   // light end of shadow

        Point2D p1 = new Point2D.Double(41.5,-25+ldiff);
        Point2D p2 = new Point2D.Double(41.5,8.0+ldiff);
        
        AffineTransform t = new AffineTransform();
        t.scale(scale,scale);
        t.rotate(Math.toRadians(direction*60),41.5,35.5);
        t.transform(p1,p1);
        t.transform(p2,p2);
        
        return(new GradientPaint(p1,c1,p2,c2));
    }

    /**
     * Returns the absolute position of the upper-left hand corner of the hex
     * graphic
     */
    private Point getHexLocation(int x, int y) {
        float elevationAdjust = 0.0f;

        IHex hex = game.getBoard().getHex(x, y);
        if ((hex != null) && useIsometric()) {
            int level = hex.getLevel();
            if (level != 0) {
                elevationAdjust = level * HEX_ELEV * scale * -1.0f;
            }
        }
        int ypos = (y * (int) (HEX_H * scale))
                   + ((x & 1) == 1 ? (int) ((HEX_H / 2) * scale) : 0);
        return new Point(x * (int) (HEX_WC * scale), ypos
                                                     + (int) elevationAdjust);
    }
    
    /**
     * For large tile texture: Returns the absolute position of the upper-left
     * hand corner of the hex graphic When using large tiles multiplying the
     * rounding errors from the (int) cast must be avoided however this cannot
     * be used for small tiles as it will make gaps appear between hexes This
     * will not factor in Isometric as this would be incorrect for large tiles
     */
    private Point getHexLocationLargeTile(int x, int y, float tscale) {
        int ypos = (int) (y * HEX_H * tscale)
                + ((x & 1) == 1 ? (int) ((HEX_H / 2) * tscale) : 0);
        return new Point((int) (x * HEX_WC * tscale), ypos);
    }
    
    private Point getHexLocationLargeTile(int x, int y) {
        return getHexLocationLargeTile(x, y, scale);
    }

    Point getHexLocation(Coords c) {
        return getHexLocation(c.getX(), c.getY());
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
        return getCentreHexLocation(c.getX(), c.getY());
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

        // base values
        int x = p.x / (int) (HEX_WC * scale);
        int y = p.y / (int) (HEX_H * scale);
        // correction for the displaced odd columns
        if ((float) p.y / (scale * HEX_H) - y < 0.5)
            y -= x % 2;

        // check the surrounding hexes if they contain p
        // checking at most 3 hexes would be sufficient
        // but which ones? This is failsafer.
        Coords cc = new Coords(x, y); 
        if (!HexDrawUtilities.getHexFull(getHexLocation(cc),scale).contains(p)) {
            boolean hasMatch = false;
            for (int dir = 0; dir < 6 && !hasMatch; dir++) {
                Coords cn = cc.translated(dir);
                if (HexDrawUtilities.getHexFull(getHexLocation(cn),scale).contains(p)) {
                    cc = cn;
                    hasMatch = true;
                }
            }
        }
        
        if (useIsometric()) {
            // When using isometric rendering, a lower hex can obscure the
            // normal hex. Iterate over all hexes from highest to lowest, 
            // looking for a hex that contains the selected mouse click point.
            final int minElev = Math.min(0, game.getBoard().getMinElevation());
            final int maxElev = Math.max(0, game.getBoard().getMaxElevation());
            final int delta = (int) Math
                    .ceil(((double) maxElev - minElev) / 3.0f);
            final int minHexSpan = Math.max(y - delta, 0);
            final int maxHexSpan = Math.min(y + delta, game.getBoard()
                                                           .getHeight());
            for (int elev = maxElev; elev >= minElev; elev--) {
                for (int i = minHexSpan; i <= maxHexSpan; i++) {
                    for (int dx = -1; dx < 2; dx++) {
                        Coords c1 = new Coords(x + dx, i);
                        IHex hexAlt = game.getBoard().getHex(c1);
                        if (HexDrawUtilities.getHexFull(getHexLocation(c1),scale).contains(p) 
                                && (hexAlt != null)
                                && (hexAlt.getLevel() == elev)) {
                            // Return immediately with highest hex found.
                            return c1;
                        }
                    }
                }
            }
            // nothing found
            return new Coords(-1,-1);
        }
        else {
            // not Isometric
            return cc;
        }
    }

    public void redrawMovingEntity(Entity entity, Coords position, int facing,
            int elevation) {
        Integer entityId = new Integer(entity.getId());
        List<Integer> spriteKey = getIdAndLoc(entityId, -1);
        EntitySprite sprite = entitySpriteIds.get(spriteKey);
        IsometricSprite isoSprite = isometricSpriteIds.get(spriteKey);
        // We can ignore secondary locations for now, as we don't have moving
        // multi-location entitys (will need to change for mobile structures)

        ArrayList<EntitySprite> newSprites;
        ArrayList<IsometricSprite> isoSprites;
        HashMap<List<Integer>, EntitySprite> newSpriteIds;
        HashMap<List<Integer>, IsometricSprite> newIsoSpriteIds;

        if (sprite != null) {
            newSprites = new ArrayList<EntitySprite>(entitySprites);
            newSpriteIds = new HashMap<>(entitySpriteIds);

            newSprites.remove(sprite);
            newSpriteIds.remove(spriteKey);

            entitySprites = newSprites;
            entitySpriteIds = newSpriteIds;
        }

        if (isoSprite != null) {
            isoSprites = new ArrayList<IsometricSprite>(isometricSprites);
            newIsoSpriteIds = new HashMap<>(isometricSpriteIds);

            isoSprites.remove(isoSprite);
            newIsoSpriteIds.remove(spriteKey);

            isometricSprites = isoSprites;
            isometricSpriteIds = newIsoSpriteIds;
        }

        MovingEntitySprite mSprite = movingEntitySpriteIds.get(entityId);
        List<MovingEntitySprite> newMovingSprites = new ArrayList<>(
                movingEntitySprites);
        HashMap<Integer, MovingEntitySprite> newMovingSpriteIds = new HashMap<>(
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
     * Convenience method for returning a Key value for the entitySpriteIds and
     * isometricSprite maps. The List contains as the first element the Entity
     * ID and as the second element it's location ID: either -1 if the Entity
     * has no secondary locations, or the index of its secondary location.
     * 
     * @param entityId
     *            The Entity ID
     * @param secondaryLoc
     *            the secondary loc index, or -1 for Entitys without secondary
     *            positions
     * @return
     */
    private List<Integer> getIdAndLoc(Integer entityId, int secondaryLoc) {
        List<Integer> idLoc = new ArrayList<Integer>(2);
        idLoc.add(entityId);
        idLoc.add(secondaryLoc);
        return idLoc;
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

        // If the entity we are updating doesn't have a position, ensure we
        // remove all of its old sprites
        if (entity.getPosition() == null) {
            Iterator<EntitySprite> spriteIter;
            
            // Remove Entity Sprites
            spriteIter = entitySprites.iterator();
            while (spriteIter.hasNext()) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            
            //  Update ID -> Sprite map
            spriteIter = entitySpriteIds.values().iterator();
            while (spriteIter.hasNext()) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.entity.equals(entity)) {
                    spriteIter.remove();
                }
            }
            
            Iterator<IsometricSprite> isoSpriteIter;
            
            // Remove IsometricSprites
            isoSpriteIter = isometricSprites.iterator();
            while (isoSpriteIter.hasNext()) {
                IsometricSprite sprite = isoSpriteIter.next();
                if (sprite.entity.equals(entity)) {
                    isoSpriteIter.remove();
                }
            }
            
            // Update ID -> Iso Sprite Map
            isoSpriteIter  = isometricSpriteIds.values().iterator();
            while (isoSpriteIter.hasNext()) {
                IsometricSprite sprite = isoSpriteIter.next();
                if (sprite.entity.equals(entity)) {
                    isoSpriteIter.remove();
                }
            }
        }

        // Create a copy of the sprite list
        ArrayList<EntitySprite> newSprites = new ArrayList<>(entitySprites);
        HashMap<List<Integer>, EntitySprite> newSpriteIds = 
                new HashMap<>(entitySpriteIds);
        ArrayList<IsometricSprite> isoSprites = 
                new ArrayList<>(isometricSprites);
        HashMap<List<Integer>, IsometricSprite> newIsoSpriteIds = 
                new HashMap<>(isometricSpriteIds);

        // Remove the sprites we are going to update
        EntitySprite sprite = entitySpriteIds.get(getIdAndLoc(entityId, -1));
        IsometricSprite isoSprite = isometricSpriteIds.get(getIdAndLoc(
                entityId, -1));
        if (sprite != null) {
            newSprites.remove(sprite);
        }
        if (isoSprite != null) {
            isoSprites.remove(isoSprite);
        }
        for (int secondaryPos : oldEntity.getSecondaryPositions().keySet()) {
            sprite = entitySpriteIds.get(getIdAndLoc(entityId, secondaryPos));
            if (sprite != null) {
                newSprites.remove(sprite);
            }
            isoSprite = isometricSpriteIds.get(getIdAndLoc(entityId,
                    secondaryPos));
            if (isoSprite != null) {
                isoSprites.remove(isoSprite);
            }
        }

        // Create the new sprites
        Coords position = entity.getPosition();
        boolean canSee = (localPlayer == null)
                || !game.getOptions().booleanOption("double_blind")
                || !entity.getOwner().isEnemyOf(localPlayer)
                || entity.hasSeenEntity(localPlayer)
                || entity.hasDetectedEntity(localPlayer);

        if ((position != null) && canSee) {
            // Add new EntitySprite
            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                sprite = new EntitySprite(this, entity, -1, radarBlipImage);
                newSprites.add(sprite);
                newSpriteIds.put(getIdAndLoc(entityId, -1), sprite);
            } else { // Add all secondary position sprites, which includes a
                // sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    sprite = new EntitySprite(this, entity, secondaryPos,
                            radarBlipImage);
                    newSprites.add(sprite);
                    newSpriteIds.put(getIdAndLoc(entityId, secondaryPos),
                            sprite);
                }
            }

            // Add new IsometricSprite
            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                isoSprite = new IsometricSprite(this, entity, -1,
                        radarBlipImage);
                isoSprites.add(isoSprite);
                newIsoSpriteIds.put(getIdAndLoc(entityId, -1), isoSprite);
            } else { // Add all secondary position sprites, which includes a
                // sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    isoSprite = new IsometricSprite(this, entity, secondaryPos,
                            radarBlipImage);
                    isoSprites.add(isoSprite);
                    newIsoSpriteIds.put(getIdAndLoc(entityId, secondaryPos),
                            isoSprite);
                }
            }
        }

        // Update Sprite state with new collections
        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;
        isometricSprites = isoSprites;
        isometricSpriteIds = newIsoSpriteIds;

        // Remove C3 sprites
        for (Iterator<C3Sprite> i = c3Sprites.iterator(); i.hasNext(); ) {
            final C3Sprite c3sprite = i.next();
            if ((c3sprite.entityId == entity.getId())
                || (c3sprite.masterId == entity.getId())) {
                i.remove();
            }
        }
        
        // Update C3 link, if necessary
        if (entity.hasC3() || entity.hasC3i() || entity.hasActiveNovaCEWS()) {
            addC3Link(entity);
        }

        // Remove Flyover Sprites
        Iterator<FlyOverSprite> flyOverIt = flyOverSprites.iterator();
        while (flyOverIt.hasNext()) {
            final FlyOverSprite flyOverSprite = flyOverIt.next();
            if (flyOverSprite.getEntityId() == entity.getId()) {
                flyOverIt.remove();
            }
        }
        
        // Add Flyover path, if necessary
        if (entity.isAirborne() && (entity.getPassedThrough().size() > 1)) {
            addFlyOverPath(entity);
        }

        updateEcmList();
        scheduleRedraw();
    }

    /**
     * Clears all old entity sprites out of memory and sets up new ones.
     */
    void redrawAllEntities() {
        int numEntities = game.getNoOfEntities();
        List<EntitySprite> newSprites = new ArrayList<EntitySprite>(numEntities);
        List<IsometricSprite> newIsometricSprites = new ArrayList<>(numEntities);
        Map<List<Integer>, EntitySprite> newSpriteIds = new HashMap<>(
                numEntities);
        Map<List<Integer>, IsometricSprite> newIsoSpriteIds = new HashMap<>(
                numEntities);

        ArrayList<WreckSprite> newWrecks = new ArrayList<>();
        ArrayList<IsometricWreckSprite> newIsometricWrecks = new ArrayList<>();

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
                        iws = new IsometricWreckSprite(this, entity,
                                secondaryPos);
                        newIsometricWrecks.add(iws);
                    }
                }
            }
        }

        clearC3Networks();
        clearFlyOverPaths();
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getPosition() == null) {
                continue;
            }
            if ((localPlayer != null)
                && game.getOptions().booleanOption("double_blind")
                && entity.getOwner().isEnemyOf(localPlayer)
                && !entity.hasSeenEntity(localPlayer)
                && !entity.hasDetectedEntity(localPlayer)) {
                continue;
            }
            if (entity.getSecondaryPositions().isEmpty()) {
                EntitySprite sprite = new EntitySprite(this, entity, -1,
                        radarBlipImage);
                newSprites.add(sprite);
                newSpriteIds.put(getIdAndLoc(entity.getId(), -1), sprite);
                IsometricSprite isosprite = new IsometricSprite(this, entity,
                        -1, radarBlipImage);
                newIsometricSprites.add(isosprite);
                newIsoSpriteIds.put(getIdAndLoc(entity.getId(), -1), isosprite);
            } else {
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    EntitySprite sprite = new EntitySprite(this, entity,
                            secondaryPos, radarBlipImage);
                    newSprites.add(sprite);
                    newSpriteIds.put(getIdAndLoc(entity.getId(), secondaryPos),
                            sprite);

                    IsometricSprite isosprite = new IsometricSprite(this,
                            entity, secondaryPos, radarBlipImage);
                    newIsometricSprites.add(isosprite);
                    newIsoSpriteIds.put(
                            getIdAndLoc(entity.getId(), secondaryPos),
                            isosprite);
                }
            }

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

        // Update ECM list, to ensure that Sprites are updated with ECM info
        updateEcmList();
        
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
        // correct for upper left board padding
        hexPoint.translate(HEX_W, HEX_H);
        JScrollBar vscroll = scrollpane.getVerticalScrollBar();
        vscroll.setValue(hexPoint.y - (vscroll.getVisibleAmount() / 2));
        JScrollBar hscroll = scrollpane.getHorizontalScrollBar();
        hscroll.setValue(hexPoint.x - (hscroll.getVisibleAmount() / 2));
        pingMinimap();
        repaint();
    }
    
    private void adjustVisiblePosition(Coords c, Point dispPoint, double ihdx, double ihdy) {
        if ((c == null) || (dispPoint == null)) return;
        
        Point hexPoint = getCentreHexLocation(c);
        // correct for upper left board padding
        hexPoint.translate(HEX_W, HEX_H);
        JScrollBar hscroll = scrollpane.getHorizontalScrollBar();
        hscroll.setValue(hexPoint.x-dispPoint.x+(int)(ihdx*scale*HEX_W));
        JScrollBar vscroll = scrollpane.getVerticalScrollBar();
        vscroll.setValue(hexPoint.y-dispPoint.y+(int)(ihdy*scale*HEX_H));
        pingMinimap();
        repaint();
    }
    
    /**
     * Centers the board to a point
     * @param xrel the x position relative to board width.
     * @param yrel the y position relative to board height.
     * Both xrel and yrel should be between 0 and 1. 
     * The method will clip both values to this range. 
     */
    public void centerOnPointRel(double xrel, double yrel) {
        // restrict both values to between 0 and 1
        xrel = Math.max(0,xrel);
        xrel = Math.min(1,xrel);
        yrel = Math.max(0,yrel);
        yrel = Math.min(1,yrel);
        Point p = new Point(
                (int)((double)boardSize.getWidth()*xrel)+HEX_W,
                (int)((double)boardSize.getHeight()*yrel)+HEX_H);
        JScrollBar vscroll = scrollpane.getVerticalScrollBar();
        vscroll.setValue(p.y - (vscroll.getVisibleAmount() / 2));
        JScrollBar hscroll = scrollpane.getHorizontalScrollBar();
        hscroll.setValue(p.x - (hscroll.getVisibleAmount() / 2));
        repaint();
    }
    
    /**
     * Returns the currently visible area of the board.
     * @return an array of 4 double values indicating the relative size,
     * where the first two values indicate the x and y position of the upper left
     * corner of the visible area and the second two values the x and y position of
     * the lower right corner.
     * So when the whole board is visible, the values should be 0,0,1,1. 
     * When the lower right corner of the board is visible 
     * and 90% of width and height: 0.1,0.1,1,1
     */
    public double[] getVisibleArea() {
        double[] values = new double[4];
        
        // adjust for padding
        double x = (double)(scrollpane.getViewport().getViewPosition().getX()-HEX_W);
        double y = (double)(scrollpane.getViewport().getViewPosition().getY()-HEX_H);
        
        values[0] = x/(double)boardSize.getWidth();
        values[1] = y/(double)boardSize.getHeight();
        
        values[2] = (x+(double)scrollpane.getViewport().getWidth())/(double)(boardSize.getWidth());
        values[3] = (y+(double)scrollpane.getViewport().getHeight())/(double)(boardSize.getHeight());
        
        // the viewport is bigger than the image, but we want only values for the image
        // therefore: restrict values to 0 ... 1 
        for (int i=0;i<4;i++) values[i] = Math.min(1, Math.max(0,values[i]));
        
        return values;
    }

    /**
     * Clears the old movement data and draws the new.
     */
    public void drawMovementData(Entity entity, MovePath md) {
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
            switch (md.getLastStep().getMovementType(true)) {
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

        for (Enumeration<MoveStep> i = md.getSteps(); i.hasMoreElements(); ) {
            final MoveStep step = i.nextElement();
            if ((null != previousStep)
                && ((step.getType() == MoveStepType.UP)
                    || (step.getType() == MoveStepType.DOWN)
                    || (step.getType() == MoveStepType.ACC)
                    || (step.getType() == MoveStepType.DEC)
                    || (step.getType() == MoveStepType.ACCN)
                    || (step.getType() == MoveStepType.DECN))) {
                // Mark the previous elevation change sprite hidden
                // so that we can draw a new one in it's place without
                // having overlap.
                pathSprites.get(pathSprites.size() - 1).setHidden(true);
            }

            // for advanced movement, we always need to hide prior
            // because costs will overlap and we only want the current
            // facing
            if ((previousStep != null) && game.useVectorMove()) {
                pathSprites.get(pathSprites.size() - 1).setHidden(true);
            }
            pathSprites.add(new StepSprite(this, step, md.isEndStep(step)));           
            previousStep = step;
        }
        repaint(100);
    }

    /**
     * Clears current movement data from the screen
     */
    public void clearMovementData() {
        pathSprites = new ArrayList<StepSprite>();
        checkFoVHexImageCacheClear();
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

    public void addStrafingCoords(Coords c) {
        strafingCoords.add(c);
    }

    public void clearStrafingCoords() {
        strafingCoords.clear();
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
            int mvType = -1;
            if (gear == MovementDisplay.GEAR_JUMP) {
                if (mvEnvData.get(loc) <= jump) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_JUMP_COLOR);
                    mvType = 1;
                }
            } else {
                if (mvEnvData.get(loc) <= walk) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_DEFAULT_COLOR);
                    mvType = 2;
                    
                } else if (mvEnvData.get(loc) <= run) {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_RUN_COLOR);
                    mvType = 3;
                } else {
                    spriteColor = guip
                            .getColor(GUIPreferences.ADVANCED_MOVE_SPRINT_COLOR);
                    mvType = 4;
                }
            }
            
            // Next: check the adjacent hexes and find
            // those with the same movement type,
            // send this to the Sprite so it paints only
            // the borders of the movement type areas
            int mvAdjType;
            int edgesToPaint = 0;
            // cycle through hexes
            for (int dir = 0; dir < 6; dir++) {
                mvAdjType = 0;
                Coords adjacentHex = loc.translated(dir);
                // get the movement type
                Integer Adjmv = mvEnvData.get(adjacentHex);
                if (Adjmv != null) {
                    if (gear == MovementDisplay.GEAR_JUMP) {
                        if (Adjmv <= jump) mvAdjType = 1;
                    } else {
                        if (Adjmv <= walk) mvAdjType = 2;
                        else if (Adjmv <= run) mvAdjType = 3;
                        else mvAdjType = 4;
                    }
                }
                // other movement type: paint a border in this direction
                if (mvAdjType != mvType) edgesToPaint += (1 << dir); 
            }

            if (spriteColor != null) {
                MovementEnvelopeSprite mvSprite = new MovementEnvelopeSprite(
                        this, spriteColor, loc, edgesToPaint);
                moveEnvSprites.add(mvSprite);
            }
        }

        repaint();

    }

    public void setMovementModifierEnvelope(Collection<MovePath> movePaths) {
        moveModEnvSprites.clear();
        for (MovePath mp : movePaths) {
            moveModEnvSprites.add(new MovementModifierEnvelopeSprite(this, mp));
        }
        repaint();
    }

    public void clearMovementEnvelope() {
        moveEnvSprites.clear();
        moveModEnvSprites.clear();
        repaint();
    }
    
    /**
     * Draws the given <code>text</code> in the currently active font of the Graphics <code>g2D</code>
     * at font size <code>fontSize</code>. The text is centered in both 
     * x and y directions around the position <code>pos</code>. The text is colored with 
     * the given <code>color</code>, made translucent if the flag is set. The outline of the text
     * will be dark gray.
     * @param g2D the graphics to draw to, as <code>Graphics2D</code>
     * @param text the string to write
     * @param pos the board pixel position
     * @param fontSize the font size. This will be scaled by the current board zoom
     * @param color the color to draw the text in
     * @param translucent (optional)  makes the text translucent if set to true. Defaults to false
     * @param cOutline (optional) the color of the outline. Defaults to Color.DARK_GRAY
     */
    public void drawOutlineText(Graphics2D g2D, String text, Point pos,
            float fontSize, Color color, boolean translucent, Color cOutline) {
        g2D.setFont(g2D.getFont().deriveFont(fontSize));
        FontMetrics fm = g2D.getFontMetrics(g2D.getFont());
        // Center the text around pos
        int cx = pos.x - (fm.stringWidth(text) / 2);
        int cy = pos.y + (fm.getAscent() - fm.getDescent()) / 2;
        
        // get text shape and position it
        GlyphVector gv = g2D.getFont().createGlyphVector(g2D.getFontRenderContext(), text);
        Shape shape = gv.getOutline();
        shape = AffineTransform.getTranslateInstance(cx,cy).
                createTransformedShape(shape);
        
        // text area fill
        if (translucent) 
            color = new Color(color.getRGB() & 0x00FFFFFF | 0xA0000000, true);
        g2D.setColor(color);
        g2D.fill(shape);
        
        // outline
        g2D.setStroke(new BasicStroke(0.5f));
        Color lineColor = cOutline;
        if (translucent) 
            lineColor = new Color(lineColor.getRGB() & 0x00FFFFFF | 0xA0000000, true);
        g2D.setColor(lineColor);
        g2D.draw(shape);
    }
    
    public void drawOutlineText(Graphics2D g2D, String text, Point pos,
            float fontSize, Color color, boolean translucent) {
        drawOutlineText(g2D, text, pos, fontSize, color, translucent, Color.DARK_GRAY);
    }
    
    public void drawOutlineText(Graphics2D g2D, String text, Point pos,
            float fontSize, Color color) {
        drawOutlineText(g2D, text, pos, fontSize, color, false, Color.DARK_GRAY);
    }
    
    public void drawTextShadow(Graphics2D g2D, String text, Point pos,
            Font font) {
        g2D.setFont(font);
        // to keep the shadow always 1 px wide,
        // counteract the current graph scaling
        double scX = g2D.getTransform().getScaleX();
        double scY = g2D.getTransform().getScaleY();
        
        drawCenteredText(g2D, text, (float)pos.x+(1.0f)/(float)scX,(float)pos.y, Color.BLACK, false);
        drawCenteredText(g2D, text, (float)pos.x-(1.0f)/(float)scX,(float)pos.y, Color.BLACK, false);
        drawCenteredText(g2D, text, (float)pos.x,(float)pos.y+(1.0f)/(float)scY, Color.BLACK, false);
        drawCenteredText(g2D, text, (float)pos.x,(float)pos.y-(1.0f)/(float)scY, Color.BLACK, false);
    }
    
    public void drawCenteredText(Graphics2D g2D, String text, Point pos,
            Color color, boolean translucent) {
        FontMetrics fm = g2D.getFontMetrics(g2D.getFont());
        // Center the text around pos
        int cx = pos.x - (fm.stringWidth(text) / 2);
        int cy = pos.y - fm.getAscent()/2-fm.getDescent() / 2+fm.getAscent();
        
        if (translucent) 
            color = new Color(color.getRGB() & 0x00FFFFFF | 0xA0000000, true);
        g2D.setColor(color);
        g2D.drawString(text, cx, cy);
    }
    
    // This method is used to draw text shadows even when the g2D is scaled
    public void drawCenteredText(Graphics2D g2D, String text, float posx, float posy,
            Color color, boolean translucent) {
        FontMetrics fm = g2D.getFontMetrics(g2D.getFont());
        // Center the text around pos
        float cx = posx - (fm.stringWidth(text) / 2);
        float cy = posy - fm.getAscent()/2-fm.getDescent() / 2+fm.getAscent();
        
        if (translucent) 
            color = new Color(color.getRGB() & 0x00FFFFFF | 0xA0000000, true);
        g2D.setColor(color);
        g2D.drawString(text, cx, cy);
    }
    
    public void drawCenteredText(Graphics2D g2D, String text, Point pos,
            Color color, boolean translucent, Font font) {
        g2D.setFont(font);
        drawCenteredText(g2D, text, pos, color, translucent);
    }
    
    public void drawCenteredText(Graphics2D g2D, String text, Point pos,
            Color color, boolean translucent, int fontSize) {
        g2D.setFont(g2D.getFont().deriveFont(fontSize));
        drawCenteredText(g2D, text, pos, color, translucent);
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
    
    public List<Entity> getEntitiesFlyingOver(Coords c) {
        List<Entity> entities = new ArrayList<Entity>();
        for (FlyOverSprite fsprite : flyOverSprites) {
            if (fsprite.getEntity().getPassedThrough().contains(c)) {
                entities.add(fsprite.getEntity());
            }
        }
        return entities;
    }

    /**
     * Adds a c3 line to the sprite list.
     */
    public void addC3Link(Entity e) {
        if (e.getPosition() == null) {
            return;
        }

        if (e.hasC3i()) {
            for (Entity fe : game.getEntitiesVector()) {
                if (fe.getPosition() == null) {
                    return;
                }
                if (e.onSameC3NetworkAs(fe)
                        && !fe.equals(e)
                        && !ComputeECM.isAffectedByECM(e, e.getPosition(),
                                fe.getPosition())) {
                    c3Sprites.add(new C3Sprite(this, e, fe));
                }
            }
        } else if (e.hasActiveNovaCEWS()) {
            // WOR Nova CEWS
            for (Entity fe : game.getEntitiesVector()) {
                if (fe.getPosition() == null) {
                    return;
                }
                ECMInfo ecmInfo = ComputeECM.getECMEffects(e, e.getPosition(),
                        fe.getPosition(), true, null);
                if (e.onSameC3NetworkAs(fe)
                    && !fe.equals(e)
                    && (ecmInfo != null)
                    && !ecmInfo.isNovaECM()) {
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
                blocked = ComputeECM.isAffectedByAngelECM(e, e.getPosition(),
                        eMaster.getPosition())
                        || ComputeECM.isAffectedByAngelECM(eMaster,
                                eMaster.getPosition(), eMaster.getPosition());
            } else {
                blocked = ComputeECM.isAffectedByECM(e, e.getPosition(),
                        eMaster.getPosition())
                        || ComputeECM.isAffectedByECM(eMaster,
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

    /**
     * Removes all attack sprites from a certain entity
     */
    public synchronized void removeAttacksFor(Entity e) {
        if (e == null) {
            return;
        }
        int entityId = e.getId();
        for (Iterator<AttackSprite> i = attackSprites.iterator(); i.hasNext(); ) {
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
                .hasMoreElements(); ) {
            EntityAction ea = i.nextElement();
            if (ea instanceof AttackAction) {
                addAttack((AttackAction) ea);
            }
        }
        for (Enumeration<AttackAction> i = game.getCharges(); i
                .hasMoreElements(); ) {
            EntityAction ea = i.nextElement();
            if (ea instanceof PhysicalAttackAction) {
                addAttack((AttackAction) ea);
            }
        }
        repaint(100);
    }

    public void refreshMoveVectors() {
        clearAllMoveVectors();
        for (Entity e : game.getEntitiesVector()) {
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
        for (Entity e : game.getEntitiesVector()) {
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
                                               "BoardView1.Attacker", new Object[]{ //$NON-NLS-1$
                                                                                    mechInFirst ? Messages
                                                                                            .getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech"), //$NON-NLS-1$ //$NON-NLS-2$
                                                                                    c1.getBoardNum()}));
                message.append(Messages
                                       .getString(
                                               "BoardView1.Target", new Object[]{ //$NON-NLS-1$
                                                                                  mechInSecond ? Messages
                                                                                          .getString("BoardView1.Mech") : Messages.getString("BoardView1.NonMech"), //$NON-NLS-1$ //$NON-NLS-2$
                                                                                  c2.getBoardNum()}));
            } else {
                le = LosEffects.calculateLos(game, ae.getId(), te);
                message.append(Messages.getString(
                        "BoardView1.Attacker", new Object[]{ //$NON-NLS-1$
                                                             ae.getDisplayName(), c1.getBoardNum()}));
                message.append(Messages.getString(
                        "BoardView1.Target", new Object[]{ //$NON-NLS-1$
                                                           te.getDisplayName(), c2.getBoardNum()}));
            }
            // Check to see if LoS is blocked
            if (!le.canSee()) {
                message.append(Messages.getString("BoardView1.LOSBlocked",
                                                  new Object[]{ //$NON-NLS-1$
                                                                new Integer(c1.distance(c2))}));
                ToHitData thd = le.losModifiers(game);
                message.append("\t" + thd.getDesc() + "\n");
            } else {
                message.append(Messages.getString("BoardView1.LOSNotBlocked",
                                                  new Object[]{ //$NON-NLS-1$
                                                                new Integer(c1.distance(c2))}));
                if (le.getHeavyWoods() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.HeavyWoods", new Object[]{ //$NON-NLS-1$
                                                                   new Integer(le.getHeavyWoods())}));
                }
                if (le.getLightWoods() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.LightWoods", new Object[]{ //$NON-NLS-1$
                                                                   new Integer(le.getLightWoods())}));
                }
                if (le.getLightSmoke() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.LightSmoke", new Object[]{ //$NON-NLS-1$
                                                                   new Integer(le.getLightSmoke())}));
                }
                if (le.getHeavySmoke() > 0) {
                    message.append(Messages.getString(
                            "BoardView1.HeavySmoke", new Object[]{ //$NON-NLS-1$
                                                                   new Integer(le.getHeavySmoke())}));
                }
                if (le.isTargetCover() && le.canSee()) {
                    message.append(Messages
                                           .getString(
                                                   "BoardView1.TargetPartialCover", new Object[]{ //$NON-NLS-1$
                                                                                                  LosEffects.getCoverName(
                                                                                                          le.getTargetCover(), true)}));
                }
                if (le.isAttackerCover() && le.canSee()) {
                    message.append(Messages.getString(
                            "BoardView1.AttackerPartialCover", new Object[]{ //$NON-NLS-1$
                                                                             LosEffects.getCoverName(le.getAttackerCover(),
                                                                                                     false)}));
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

        AffineTransform FacingRotate = new AffineTransform();
        
        // facing polygons
        Polygon facingPoly_tmp = new Polygon();
        facingPoly_tmp.addPoint(41, 3);
        facingPoly_tmp.addPoint(35, 9);
        facingPoly_tmp.addPoint(41, 7);
        facingPoly_tmp.addPoint(42, 7);
        facingPoly_tmp.addPoint(48, 9);
        facingPoly_tmp.addPoint(42, 3);
        
        // create the rotated shapes
        facingPolys = new Shape[8];
        for (int dir: allDirections)
        {
            facingPolys[dir] = FacingRotate.createTransformedShape(facingPoly_tmp);
            FacingRotate.rotate(Math.toRadians(60),HEX_W/2,HEX_H/2);
        }

        // movement polygons
        Polygon movementPoly_tmp = new Polygon();
        movementPoly_tmp.addPoint(47, 67);
        movementPoly_tmp.addPoint(48, 66);
        movementPoly_tmp.addPoint(42, 62);
        movementPoly_tmp.addPoint(41, 62);
        movementPoly_tmp.addPoint(35, 66);
        movementPoly_tmp.addPoint(36, 67);
        
        movementPoly_tmp.addPoint(47, 67);
        movementPoly_tmp.addPoint(45, 68);
        movementPoly_tmp.addPoint(38, 68);
        movementPoly_tmp.addPoint(38, 69);
        movementPoly_tmp.addPoint(45, 69);
        movementPoly_tmp.addPoint(45, 68);
        
        movementPoly_tmp.addPoint(45, 70);
        movementPoly_tmp.addPoint(38, 70);
        movementPoly_tmp.addPoint(38, 71);
        movementPoly_tmp.addPoint(45, 71);
        movementPoly_tmp.addPoint(45, 68);

        // create the rotated shapes
        FacingRotate.setToIdentity();  
        movementPolys = new Shape[8];
        for (int dir: allDirections)
        {
            movementPolys[dir] = FacingRotate.createTransformedShape(movementPoly_tmp);
            FacingRotate.rotate(Math.toRadians(60),HEX_W/2,HEX_H/2);
        }
        
        // Up and Down Arrows
        FacingRotate.setToIdentity();
        FacingRotate.translate(0, -31);
        UpArrow = FacingRotate.createTransformedShape(movementPoly_tmp);

        FacingRotate.setToIdentity();
        FacingRotate.rotate(Math.toRadians(180),HEX_W/2,HEX_H/2);
        FacingRotate.translate(0, -31);
        DownArrow = FacingRotate.createTransformedShape(movementPoly_tmp);
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
     * <code>false</code> if more need to be loaded.
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
     * @param lastCursor The lastCursor to set.
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
     * @param highlighted The highlighted to set.
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
     * @param selected The selected to set.
     */
    public void setSelected(Coords selected) {
        if (this.selected != selected) {
            this.selected = selected;
            checkFoVHexImageCacheClear();
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
     * @param firstLOS The firstLOS to set.
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
     * @param coords the Coords.
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
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public void select(int x, int y) {
        select(new Coords(x, y));
    }

    /**
     * Determines if this Board contains the Coords, and if so, highlights that
     * Coords.
     *
     * @param coords the Coords.
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

    public void setHighlightColor(Color c) {
        highlightSprite.setColor(c);
        highlightSprite.prepare();
        repaint();
    }

    /**
     * Highlights the specified Coords.
     *
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public void highlight(int x, int y) {
        highlight(new Coords(x, y));
    }

    /**
     * Determines if this Board contains the Coords, and if so, "cursors" that
     * Coords.
     *
     * @param coords the Coords.
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
     * @param x the x coordinate.
     * @param y the y coordinate.
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
     * @param coords the Coords.
     */
    public void mouseAction(Coords coords, int mtype, int modifiers) {
        mouseAction(coords.getX(), coords.getY(), mtype, modifiers);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.BoardListener#boardNewBoard(megamek.common.BoardEvent)
     */
    public void boardNewBoard(BoardEvent b) {
        updateBoard();
        clearHexImageCache();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.BoardListener#boardChangedHex(megamek.common.BoardEvent)
     */
    public synchronized void boardChangedHex(BoardEvent b) {
        hexImageCache.remove(b.getCoords());
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
        clearHexImageCache();
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
            final Vector<UnitLocation> mp = e.getMovePath();
            final Entity en = e.getEntity();
            final GameOptions gopts = game.getOptions();
            GUIPreferences guip = GUIPreferences.getInstance();

            updateEcmList();
            redrawAllEntities();
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                refreshMoveVectors();
            }
            if ((mp != null) && (mp.size() > 0) && guip.getShowMoveStep()
                    && !gopts.booleanOption("simultaneous_movement")) {
                if ((localPlayer == null)
                        || !game.getOptions().booleanOption("double_blind")
                        || !en.getOwner().isEnemyOf(localPlayer)
                        || en.hasSeenEntity(localPlayer)) {
                    addMovingUnit(en, mp);
                }
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
            clearHexImageCache();
            updateBoard();
            clearShadowMap();
        }

        @Override
        public void gameBoardChanged(GameBoardChangeEvent e) {
            clearHexImageCache();
            boardChanged();
            clearShadowMap();
        }

        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            refreshAttacks();

            // Clear some information regardless of what phase it is
            clearFiringSolutionData();
            clearMovementEnvelope();

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
                    clearHexImageCache();
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
        fieldofFireSprites.clear();
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
        checkFoVHexImageCacheClear();
        // If we don't do this, the selectedWeapon might not correspond to this
        // entity
        selectedWeapon = null;
        updateEcmList();
    }

    public synchronized void weaponSelected(MechDisplayEvent b) {
        selectedEntity = b.getEntity();
        selectedWeapon = b.getEquip();
        repaint();
    }

    /**
     *  Updates maps that determine how to shade hexes affected by E(C)CM. This
     *  is expensive, so precalculate only when entity changes occur
     **/
    public void updateEcmList() {
        Map<Coords, Color> newECMHexes = new HashMap<Coords, Color>();
        Map<Coords, Color> newECMCenters = new HashMap<Coords, Color>();
        Map<Coords, Color> newECCMHexes = new HashMap<Coords, Color>();
        Map<Coords, Color> newECCMCenters = new HashMap<Coords, Color>();

        // Compute info about all E(C)CM on the board
        final List<ECMInfo> allEcmInfo = ComputeECM
                .computeAllEntitiesECMInfo(game.getEntitiesVector());

        // First, mark the sources of E(C)CM
        // Used for highlighting hexes and tooltips
        for (Entity e : game.getEntitiesVector()) {
            if (e.getPosition() == null) {
                continue;
            }
            // If this unit isn't spotted somehow, it's ECM doesn't show up
            if ((localPlayer != null)
                    && game.getOptions().booleanOption("double_blind")
                    && e.getOwner().isEnemyOf(localPlayer)
                    && !e.hasSeenEntity(localPlayer)
                    && !e.hasDetectedEntity(localPlayer)) {
                continue;
            }
            
            final Color ecmColor = ECMEffects.getECMColor(e.getOwner());
            // Update ECM center information
            if (e.getECMInfo() != null) {
                newECMCenters.put(e.getPosition(), ecmColor);
            }
            // Update ECCM center information
            if (e.getECCMInfo() != null) {
                newECCMCenters.put(e.getPosition(), ecmColor);
            }
            // Update Entity sprite's ECM status
            int secondaryIdx = -1;
            if (e.getSecondaryPositions().size() > 0) {
                secondaryIdx = 0;
            }
            EntitySprite eSprite = entitySpriteIds.get(getIdAndLoc(e.getId(),
                    secondaryIdx));
            if (eSprite != null) {
                Coords pos = e.getPosition();
                eSprite.setAffectedByECM(ComputeECM.isAffectedByECM(e, pos,
                        pos, allEcmInfo));
            }
        }

        // Next, determine what E(C)CM effects each Coord
        Map<Coords, ECMEffects> ecmAffectedCoords =
                new HashMap<Coords, ECMEffects>();
        for (ECMInfo ecmInfo : allEcmInfo) {
            // Can't see ECM field of unspotted unit
            if ((ecmInfo.getEntity() != null) && (localPlayer != null)
                    && game.getOptions().booleanOption("double_blind")
                    && ecmInfo.getEntity().getOwner().isEnemyOf(localPlayer)
                    && !ecmInfo.getEntity().hasSeenEntity(localPlayer)
                    && !ecmInfo.getEntity().hasDetectedEntity(localPlayer)) {
                continue;
            }
            final Coords ecmPos = ecmInfo.getPos();
            final int range = ecmInfo.getRange();

            // Add each Coords within range to the list of ECM Coords
            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    Coords c = new Coords(x + ecmPos.getX(), y + ecmPos.getY());
                    int dist = ecmPos.distance(c);
                    int dir = ecmInfo.getDirection();
                    // Direction is the facing of the owning Entity
                    boolean inArc = (dir == -1)
                            || Compute
                                    .isInArc(ecmPos, dir, c, Compute.ARC_NOSE);
                    if ((dist > range) || !inArc) {
                        continue;
                    }
                    ECMEffects ecmEffects = ecmAffectedCoords.get(c);
                    if (ecmEffects == null) {
                        ecmEffects = new ECMEffects();
                        ecmAffectedCoords.put(c, ecmEffects);
                    }
                    ecmEffects.addECM(ecmInfo);
                }
            }
        }

        // Finally, determine the color for each affected hex
        for (Coords c : ecmAffectedCoords.keySet()) {
            ECMEffects ecmEffects = ecmAffectedCoords.get(c);
            Color hexColor = ecmEffects.getHexColor();
            // Hex color is null if all effects cancel out
            if (hexColor == null) {
                continue;
            }
            if (ecmEffects.isECCM()) {
                newECCMHexes.put(c, hexColor);
            } else {
                newECMHexes.put(c, hexColor);
            }
        }

        synchronized (this) {
            ecmHexes    = newECMHexes;
            ecmCenters  = newECMCenters;
            eccmHexes   = newECCMHexes;
            eccmCenters = newECCMCenters;
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
    public void setPreferredSize(Dimension d) {
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
                Math.max(boardSize.width + (2 * HEX_W), preferredSize.width),
                Math.max(boardSize.height + (2 * HEX_W), preferredSize.height));
    }

    /**
     * Have the player select an Entity from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Entity chooseEntity(Coords pos) {

        // Assume that we have *no* choice.
        Entity choice = null;

        // Get the available choices.
        List<Entity> entities = game.getEntitiesVector(pos);


        // Do we have a single choice?
        if (entities.size() == 1) {
            // Return that choice.
            choice = entities.get(0);
        }

        // If we have multiple choices, display a selection dialog.
        else if (entities.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            null,
                            Messages.getString(
                                    "BoardView1.ChooseEntityDialog.message", new Object[]{pos.getBoardNum()}), //$NON-NLS-1$
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

        if (game.getBoard().contains(mcoords)) 
            mhex = game.getBoard().getHex(mcoords);

        txt.append("<html>"); //$NON-NLS-1$

        // Hex Terrain
        if (GUIPreferences.getInstance().getShowMapHexPopup() && (mhex != null)) {
    
            txt.append("<TABLE BORDER=0 BGCOLOR=#DDFFDD width=100%><TR><TD>"); //$NON-NLS-1$
            
            txt.append(Messages.getString("BoardView1.Tooltip.Hex", //$NON-NLS-1$
                    new Object[] { mcoords.getBoardNum(), mhex.getLevel() }));
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
                        name = name + " (TF: " + tf + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    if (name != null) {
                        txt.append(name);
                        txt.append("<br>"); //$NON-NLS-1$
                    }
                }
            }
            txt.append("</TD></TR></TABLE>"); //$NON-NLS-1$
            
            // Fuel Tank
            if (mhex.containsTerrain(Terrains.FUEL_TANK)) {
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                txt.append("<TABLE BORDER=0 BGCOLOR=#999999 width=100%><TR><TD>"); //$NON-NLS-1$
                txt.append(Messages.getString("BoardView1.Tooltip.Bridge", new Object[] { //$NON-NLS-1$
                        mhex.terrainLevel(Terrains.FUEL_TANK_ELEV),
                        bldg.toString(),
                        bldg.getCurrentCF(mcoords),
                }));
                txt.append("</TD></TR></TABLE>"); //$NON-NLS-1$
            }
            
            // Building
            if (mhex.containsTerrain(Terrains.BUILDING)) {
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                // in the map editor, the building might not exist
                if (bldg != null) {
                    txt.append("<TABLE BORDER=0 BGCOLOR=#CCCC99 width=100%><TR><TD>"); //$NON-NLS-1$
                    txt.append(Messages.getString("BoardView1.Tooltip.Building", new Object[] { //$NON-NLS-1$
                            mhex.terrainLevel(Terrains.BLDG_ELEV),
                            bldg.toString(),
                            bldg.getCurrentCF(mcoords),
                            bldg.getArmor(mcoords),
                            bldg.getBasement(mcoords).getDesc()       
                    }));
                    
                    if (bldg.getBasementCollapsed(mcoords)) {
                        txt.append(Messages
                                .getString("BoardView1.Tooltip.BldgBasementCollapsed")); //$NON-NLS-1$
                    }
                    txt.append("</TD></TR></TABLE>"); //$NON-NLS-1$
                }
            }
            
            // Bridge
            if (mhex.containsTerrain(Terrains.BRIDGE)) {
                Building bldg = game.getBoard().getBuildingAt(mcoords);
                txt.append("<TABLE BORDER=0 BGCOLOR=#999999 width=100%><TR><TD>"); //$NON-NLS-1$
                txt.append(Messages.getString("BoardView1.Tooltip.Bridge", new Object[] { //$NON-NLS-1$
                        mhex.terrainLevel(Terrains.BRIDGE_ELEV),
                        bldg.toString(),
                        bldg.getCurrentCF(mcoords),
                }));
                txt.append("</TD></TR></TABLE>"); //$NON-NLS-1$
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
        
        // Show the player(s) that may deploy here 
        // in the artillery autohit designation phase
        if ((game.getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) && (mhex != null)) {
            txt.append("<TABLE BORDER=0 width=100%><TR><TD>"); //$NON-NLS-1$
            Enumeration<IPlayer> allP = game.getPlayers();
            boolean foundPlayer = false;
            // loop through all players
            while (allP.hasMoreElements())
            {
                IPlayer cp = allP.nextElement();
                if (game.getBoard().isLegalDeployment(mcoords, cp.getStartingPos())) {
                    if (!foundPlayer) {
                        foundPlayer = true;
                        txt.append(Messages.getString("BoardView1.Tooltip.ArtyAutoHeader")); //$NON-NLS-1$
                    }
                    txt.append("<B><FONT COLOR=#"); //$NON-NLS-1$
                    txt.append(Integer.toHexString(PlayerColors.getColorRGB(cp.getColorIndex())));
                    txt.append(">&nbsp;&nbsp;"); //$NON-NLS-1$
                    txt.append(cp.getName());
                    txt.append("</FONT></B><BR>"); //$NON-NLS-1$
                }
            }
            if (foundPlayer) txt.append("<BR>"); //$NON-NLS-1$

            // Add a hint with keybind that the zones can be shown graphically
            String keybindText = KeyEvent.getKeyModifiersText(KeyCommandBind.getBindByCmd("autoArtyDeployZone").modifiers); //$NON-NLS-1$
            if (!keybindText.isEmpty()) keybindText += "+";
            keybindText += KeyEvent.getKeyText(KeyCommandBind.getBindByCmd("autoArtyDeployZone").key); //$NON-NLS-1$
            txt.append(Messages.getString("BoardView1.Tooltip.ArtyAutoHint",  //$NON-NLS-1$
                    new Object[] { keybindText }));
            
            txt.append("</TD></TR></TABLE>"); //$NON-NLS-1$
        }
        

        // check if it's on any flares
        for (FlareSprite fSprite : flareSprites) {
            if (fSprite.isInside(point)) {
                txt.append(fSprite.getTooltip().toString());
            }
        }
        
        // check if it's on any attacks
        for (AttackSprite aSprite : attackSprites) {
            if (aSprite.isInside(point)) {
                txt.append("<TABLE BORDER=0 BGCOLOR=#FFDDDD width=100%><TR><TD>"); //$NON-NLS-1$
                txt.append(aSprite.getTooltip().toString());
                txt.append("</TD></TR></TABLE>"); //$NON-NLS-1$
            }
        }

        // Entity tooltips
        int entityCount = 0;
        // Maximum number of entities to show in the tooltip
        int maxShown = 4; 

        Set<Entity> coordEnts = new HashSet<>(game.getEntitiesVector(mcoords));
        Set<Entity> usedSet = new HashSet<Entity>(entitySprites.size());
        for (EntitySprite eSprite : entitySprites) {
            if ((eSprite.isInside(point) || coordEnts.contains(eSprite.entity))
                    && !usedSet.contains(eSprite.entity)) {
                usedSet.add(eSprite.entity);
                entityCount++;
                
                // List only the first four units
                if (entityCount <= maxShown) {
                    // Table to add a bar to the left of an entity in
                    // the player's color
                    txt.append("<hr style=width:90%>"); //$NON-NLS-1$
                    txt.append("<TABLE><TR><TD bgcolor=#"); //$NON-NLS-1$
                    txt.append(eSprite.getPlayerColor());
                    txt.append(" width=6></TD><TD>"); //$NON-NLS-1$

                    // TT generated by Sprite
                    txt.append(eSprite.getTooltip());

                    // ECM and ECCM source
                    if ((ecmCenters != null)
                            && ecmCenters.containsKey(eSprite.getPosition())) {
                        txt.append("<br><FONT SIZE=-2><img src=file:" //$NON-NLS-1$
                                + Configuration.widgetsDir()
                                + "/Tooltip/ECM.png>&nbsp;"); //$NON-NLS-1$
                        txt.append(Messages.getString("BoardView1.ecmSource")); //$NON-NLS-1$
                        txt.append("</FONT>"); //$NON-NLS-1$
                    }
                    if ((eccmCenters != null)
                            && eccmCenters.containsKey(eSprite.getPosition())) {
                        txt.append("<br><FONT SIZE=-2><img src=file:" //$NON-NLS-1$
                                + Configuration.widgetsDir()
                                + "/Tooltip/ECM.png>&nbsp;"); //$NON-NLS-1$
                        txt.append(Messages.getString("BoardView1.eccmSource")); //$NON-NLS-1$
                        txt.append("</FONT>");
                    }
                    txt.append("</TD></TR></TABLE>"); //$NON-NLS-1$
                } 
            }
        }
        // Info block if there are more than 4 units in that hex
        if (entityCount > maxShown)
        {
            txt.append("<TABLE BORDER=0 BGCOLOR=#000060 width=100%><TR><TD><FONT COLOR=WHITE>There ");
            if (entityCount-maxShown == 1) 
                txt.append("is 1 more<BR>unit");
            else 
                txt.append("are "+(entityCount-maxShown)+" more<BR>units");
            txt.append(" in this hex...</FONT>");
            txt.append("</TD></TR></TABLE>");
        }

        // Artillery attacks
        for (ArtilleryAttackAction aaa : artilleryAttacks) {
            // Default texts if no real names can be found
            String wpName = Messages.getString("BoardView1.Artillery");
            String ammoName = "Unknown";

            // Get real weapon and ammo name
            final Entity artyEnt = game.getEntity(aaa.getEntityId());
            if (artyEnt != null) {
                if (aaa.getWeaponId() > -1) {
                    wpName = artyEnt.getEquipment(aaa.getWeaponId()).getName();
                    if (aaa.getAmmoId() > -1) {
                        ammoName =  artyEnt.getEquipment(aaa.getAmmoId()).getName();
                    }
                }
            }
            
            txt.append("<TABLE BORDER=0 BGCOLOR=#FFDDDD width=100%><TR><TD>");
            if (aaa.turnsTilHit == 1)
                txt.append(Messages.getString("BoardView1.Tooltip.ArtilleryAttack1", 
                        new Object[] { wpName, ammoName }));
            else
                txt.append(Messages.getString("BoardView1.Tooltip.ArtilleryAttackN", 
                        new Object[] { wpName, ammoName, aaa.turnsTilHit }));
            txt.append("</TD></TR></TABLE>");
        }

        // Artillery fire adjustment
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
                        new Object[]{new Integer(amod)}));
                txt.append("<br>"); //$NON-NLS-1$
            }
        }

        final Collection<SpecialHexDisplay> shdList = game.getBoard()
                .getSpecialHexDisplay(mcoords);
        final Phase currPhase = game.getPhase();
        int round = game.getRoundCount();
        if (shdList != null) {
            boolean isHexAutoHit = localPlayer.getArtyAutoHitHexes().contains(
                    mcoords);
            for (SpecialHexDisplay shd : shdList) {
                boolean isTypeAutoHit = shd.getType() 
                        == SpecialHexDisplay.Type.ARTILLERY_AUTOHIT;
                // Don't draw if this SHD is obscured from this player
                // The SHD list may also contain stale SHDs, so don't show 
                // tooltips for SHDs that aren't drawn.
                // The exception is auto hits.  There will be an icon for auto
                // hits, so we need to draw a tooltip
                if (!shd.isObscured(localPlayer)
                        && (shd.drawNow(currPhase, round, localPlayer) 
                                || (isHexAutoHit && isTypeAutoHit))) {
                    if (shd.getType() == SpecialHexDisplay.Type.PLAYER_NOTE) {
                        if (localPlayer.equals(shd.getOwner())) {
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
        // Check to see if the tool tip is completely empty
        if (txt.toString().equals("<html></html>")) {
            // Returning null prevents the tooltip from being displayed
            // This prevents a small blue tooltip rectangle being drawn at the
            // edge of the board
            return null;
        }
        return txt.toString();
    }

    private ArrayList<ArtilleryAttackAction> getArtilleryAttacksAtLocation(
            Coords c) {
        ArrayList<ArtilleryAttackAction> v = new ArrayList<ArtilleryAttackAction>();
        for (Enumeration<ArtilleryAttackAction> attacks = game
                .getArtilleryAttacks(); attacks.hasMoreElements(); ) {
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
            if (bvSkinSpec.backgrounds.size() > 0) {
                file = new File(Configuration.widgetsDir(),
                                bvSkinSpec.backgrounds.get(0));
                imgURL = file.toURI();
                if (!file.exists()) {
                    System.err.println("BoardView1 Error: icon doesn't exist: "
                                       + file.getAbsolutePath());
                } else {
                    bvBgIcon = new ImageIcon(imgURL.toURL());
                }
            }
            if (bvSkinSpec.backgrounds.size() > 1) {
                file = new File(Configuration.widgetsDir(),
                                bvSkinSpec.backgrounds.get(1));
                imgURL = file.toURI();
                if (!file.exists()) {
                    System.err.println("BoardView1 Error: icon doesn't exist: "
                                       + file.getAbsolutePath());
                } else {
                    scrollPaneBgIcon = new ImageIcon(imgURL.toURL());
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading BoardView background images!");
            System.out.println(e.getMessage());
        }

        // Place the board viewer in a set of scrollbars.
        scrollpane = new JScrollPane(this) {

            /**
             *
             */
            private static final long serialVersionUID = 5973610449428194319L;

            @Override
            protected void paintComponent(Graphics g) {
                if (scrollPaneBgIcon == null) {
                    super.paintComponent(g);
                    return;
                }
                int w = getWidth();
                int h = getHeight();
                int iW = scrollPaneBgIcon.getIconWidth();
                int iH = scrollPaneBgIcon.getIconHeight();
                if ((scrollPaneBgBuffer == null)
                    || (scrollPaneBgBuffer.getWidth() != w)
                    || (scrollPaneBgBuffer.getHeight() != h)) {
                    scrollPaneBgBuffer = new BufferedImage(w, h,
                                                           BufferedImage.TYPE_INT_RGB);
                    Graphics bgGraph = scrollPaneBgBuffer.getGraphics();
                    for (int x = 0; x < w; x += iW) {
                        for (int y = 0; y < h; y += iH) {
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

        if (!scrollBars && !bvSkinSpec.showScrollBars) {
            vbar.setPreferredSize(new Dimension(0, vbar.getHeight()));
            hbar.setPreferredSize(new Dimension(hbar.getWidth(), 0));
        }

        return scrollpane;
    }

    /**
     * refresh the IDisplayables
     */
    public void refreshDisplayables() {
        repaint();
    }
    
    private void pingMinimap() {
        // send the minimap a hex moused event to make it 
        // update the visible area rectangle
        BoardViewEvent bve = new BoardViewEvent(this,BoardViewEvent.BOARD_HEX_DRAGGED); 
        if (boardListeners != null) {
            for (BoardViewListener l : boardListeners) l.hexMoused(bve);
        }
    }

    public void showPopup(Object popup, Coords c) {
        Point p = getHexLocation(c);
        p.x += ((int) (HEX_WC * scale) - scrollpane.getX()) + HEX_W;
        p.y += ((int) ((HEX_H * scale) / 2) - scrollpane.getY()) + HEX_H;
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
        for (Sprite spr : moveEnvSprites) {
            spr.prepare();
        }
        for (Sprite spr : moveModEnvSprites) {
            spr.prepare();
        }
        for (Sprite spr : fieldofFireSprites) {
            spr.prepare();
        }

        updateFontSizes();
        updateBoard();
        for (StepSprite sprite : pathSprites) {
            sprite.refreshZoomLevel();
        }
        for (FiringSolutionSprite sprite : firingSprites) {
            sprite.prepare();
        }
        this.setSize(boardSize);

        clearHexImageCache();
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
     * @param base     The image to get a scaled copy of.  The current zoom level
     *                 is used to determine the scale.
     * @param useCache This flag determines whether the scaled image should
     *                 be stored in a cache for later retrieval.
     */
    Image getScaledImage(Image base, boolean useCache) {
        if (base == null) {
            return null;
        }
        if (zoomIndex == BASE_ZOOM_INDEX) {
            return base;
        }


        Image scaled;
        if (useCache) {
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
                if (tracker.isErrorAny()) {
                    return null;
                }
                tracker.removeImage(base);
            }
            int width = (int) (base.getWidth(null) * scale);
            int height = (int) (base.getHeight(null) * scale);

            if ((width < 1) || (height < 1)) {
                return null;
            }

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
            if (useCache) {
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
        for (Sprite spr : moveEnvSprites) spr.prepare();
        for (Sprite spr : moveModEnvSprites) spr.prepare();
        for (Sprite spr : fieldofFireSprites) spr.prepare();
        clearHexImageCache();
        updateBoard();
        for (MovementEnvelopeSprite sprite: moveEnvSprites)
            sprite.updateBounds();
        for (MovementModifierEnvelopeSprite sprite: moveModEnvSprites)
            sprite.updateBounds();
        repaint();
        return drawIsometric;
    }
    
    public void updateEntityLabels() {
        for (Entity e: game.getEntitiesVector()) {
            e.generateShortName();
        }
        for (EntitySprite eS: entitySprites) {
            eS.prepare();
        }
        repaint();
    }

    BufferedImage createShadowMask(Image image) {
        int hashCode = image.hashCode();
        BufferedImage mask = shadowImageCache.get(hashCode);
        if (mask != null) {
            return mask;
        }
        mask = new BufferedImage(image.getWidth(null),
                image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        float opacity = 0.4f;
        Graphics2D g2d = mask.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN,
                                                    opacity));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        g2d.dispose();
        shadowImageCache.put(hashCode, mask);
        return mask;
    }

    public void die() {
        ourTask.cancel();
        fovHighlightingAndDarkening.die();
    }

    /**
     * Returns true if the BoardView has an active chatter box else false.
     *
     * @return
     */
    public boolean getChatterBoxActive() {
        return chatterBoxActive;
    }

    /**
     * Sets whether the BoardView has an active chatter box or not.
     *
     * @param cba
     */
    public void setChatterBoxActive(boolean cba) {
        chatterBoxActive = cba;
    }

    public void setShouldIgnoreKeys(boolean shouldIgnoreKeys) {
        this.shouldIgnoreKeys = shouldIgnoreKeys;
    }

    @Override
    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
        // If FRAMEBITS is set, then new frame from a multi-frame image is ready
        // This indicates an animated image, which shouldn't be cached
        if ((flags & ImageObserver.FRAMEBITS) != 0) {
            animatedImages.add(img.hashCode());
        }
        return super.imageUpdate(img, flags, x, y, w, h);
    }

    public void clearHexImageCache() {
        hexImageCache.clear();
    }

    /**
     * Check to see if the HexImageCache should be cleared because of
     * field-of-view changes.
     */
    public void checkFoVHexImageCacheClear() {
        boolean darken = GUIPreferences.getInstance().getBoolean(
                GUIPreferences.FOV_DARKEN);
        boolean highlight = GUIPreferences.getInstance().getBoolean(
                GUIPreferences.FOV_HIGHLIGHT);
        if ((game.getPhase() == Phase.PHASE_MOVEMENT)
                && (darken || highlight)) {
            clearHexImageCache();
        }
    }

    public Polygon getHexPoly() {
        return hexPoly;
    }

    public void clearFieldofF() {
        fieldofFireWpArc = -1;
        fieldofFireUnit = null;
        fieldofFireSprites.clear();
        repaint();
    }
    
    // this is called from MovementDisplay and checks if 
    // the unit ends up underwater
    public void setWeaponFieldofFire(Entity ce, MovePath cmd) {
        // if lack of data: clear and return
        if ((fieldofFireUnit == null) 
            || (ce == null) 
            || (cmd == null)) {
            clearFieldofF();
            return;
        }

        // If the field of fire is not dispalyed
        // for the active unit, then don't change anything
        if (fieldofFireUnit.equals(ce)) {
            
            fieldofFireWpUnderwater = 0;
            // check if the weapon ends up underwater
            IHex hex = game.getBoard().getHex(cmd.getFinalCoords());
            
            if ((hex.terrainLevel(Terrains.WATER) > 0) && !cmd.isJumping() 
                    && (cmd.getFinalElevation() < 0)) {
                if ((fieldofFireUnit instanceof Mech) && !fieldofFireUnit.isProne()
                        && (hex.terrainLevel(Terrains.WATER) == 1)) {
                    if ((fieldofFireWpLoc == Mech.LOC_RLEG)
                        || (fieldofFireWpLoc == Mech.LOC_LLEG))
                        fieldofFireWpUnderwater = 1;
                    
                    if (fieldofFireUnit instanceof QuadMech) {
                        if ((fieldofFireWpLoc == Mech.LOC_RARM)
                            || (fieldofFireWpLoc == Mech.LOC_LARM))
                            fieldofFireWpUnderwater = 1;
                    }
                    if (fieldofFireUnit instanceof TripodMech) {
                        if (fieldofFireWpLoc == Mech.LOC_CLEG)
                            fieldofFireWpUnderwater = 1;
                    }
                } else {
                    fieldofFireWpUnderwater = 1;
                }
            } 
            setWeaponFieldofFire(cmd.getFinalFacing(), cmd.getFinalCoords());
        }
    }
    
    // prepares the sprites for a field of fire
    public void setWeaponFieldofFire(int fac, Coords c) {
        if (fieldofFireUnit == null) {
            clearFieldofF();
            return;
        }

        // Do not display anything for offboard units
        if (fieldofFireUnit.isOffBoard()) {
            clearFieldofF();
            return;
        }
        
        // check if extreme range is used
        int maxrange = 4;
        if (game.getOptions().
                booleanOption(OptionsConstants.AC_TAC_OPS_RANGE)) maxrange = 5;
        
        // create the lists of hexes
        List<Set<Coords>> fieldFire = new ArrayList<Set<Coords>>(5);
        int range = 1;
        // for all available range brackets Min/S/M/L/E ...
        for (int bracket = 0; bracket < maxrange; bracket++) {
            fieldFire.add(new HashSet<Coords>());
            // Add all hexes up to the weapon range to separate lists
            while (range<=fieldofFireRanges[fieldofFireWpUnderwater][bracket]) {
                fieldFire.get(bracket).addAll(Compute.coordsAtRange(c, range));
                range++;
                if (range>100) break; // only to avoid hangs
            }

            // Remove hexes that are not on the board or not in the arc
            for (Iterator<Coords> iterator = fieldFire.get(bracket).iterator(); iterator.hasNext();) {
                Coords h = iterator.next();
                if (!game.getBoard().contains(h) 
                        || !Compute.isInArc(c, fac, h, fieldofFireWpArc)) {
                    iterator.remove();
                }
            }
        }
        
        // create the sprites
        //
        fieldofFireSprites.clear();

        // for all available range brackets Min/S/M/L/E ...
        for (int bracket = 0; bracket < fieldFire.size(); bracket++) {
            if (fieldFire.get(bracket) == null) continue;
            for (Coords loc : fieldFire.get(bracket)) {
                // check surrounding hexes
                int edgesToPaint = 0;
                for (int dir = 0; dir < 6; dir++) {
                    Coords adjacentHex = loc.translated(dir);
                    if (!fieldFire.get(bracket).contains(adjacentHex)) edgesToPaint += (1 << dir);
                }
                // create sprite if there's a border to paint
                if (edgesToPaint > 0) {
                    FieldofFireSprite ffSprite = new FieldofFireSprite(
                            this, bracket, loc, edgesToPaint);
                    fieldofFireSprites.add(ffSprite);
                }
            }
            // Add range markers (m, S, M, L, E)
            // this looks for a hex in the middle of the range bracket;
            // if outside the board, nearer hexes will be tried until
            // the inner edge of the range bracket is reached
            // the directions tested are those that fall between the
            // hex facings because this makes for a better placement
            // ... most of the time...
            
            // The directions[][] is used to make the marker placement
            // fairly symmetrical to the unit facing which a simple for 
            // loop over the hex facings doesn't do
            int[][] directions = { {0,1},{0,5},{3,2},{3,4},{1,2},{5,4} };
            // don't paint too many "min" markers
            int numMinMarkers = 0; 
            for (int[] dir: directions) {
                // find the middle of the range bracket
                int rangeend = Math.max(fieldofFireRanges[fieldofFireWpUnderwater][bracket],0);
                int rangebegin = 1; 
                if (bracket>0) 
                    rangebegin = Math.max(fieldofFireRanges[fieldofFireWpUnderwater][bracket-1]+1,1);
                int dist = (rangeend + rangebegin)/2;
                // translate to the middle of the range bracket
                Coords mark = c.translated((dir[0]+fac)%6,(dist+1)/2)
                        .translated((dir[1]+fac)%6,dist/2);
                // traverse back to the unit until a hex is onboard
                while (!game.getBoard().contains(mark)) 
                    mark = Coords.nextHex(mark, c);
                
                // add a text range marker if the found position is good
                if (game.getBoard().contains(mark) && fieldFire.get(bracket).contains(mark)
                        && ((bracket > 0) || (numMinMarkers < 2))) {
                    TextMarkerSprite tS = new TextMarkerSprite(this, mark, 
                            rangeTexts[bracket], FieldofFireSprite.fieldofFireColors[bracket]);
                    fieldofFireSprites.add(tS);
                    if (bracket == 0) numMinMarkers++;
                }
            }
        }
        
        repaint();
    }
    
}
