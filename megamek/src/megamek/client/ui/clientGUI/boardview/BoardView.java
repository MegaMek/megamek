/*
 * Copyright (C) 2000-2008 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview;

import static megamek.client.ui.tileset.HexTileset.HEX_H;
import static megamek.client.ui.tileset.HexTileset.HEX_W;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalTheme;

import megamek.MMConstants;
import megamek.client.TimerSingleton;
import megamek.client.bot.princess.MinefieldDeploymentPlanner;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.IDisplayable;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.gpu.GpuBoardWindow;
import megamek.client.ui.clientGUI.boardview.overlay.ChatterBoxOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.OverlayImage;
import megamek.client.ui.clientGUI.boardview.overlay.TurnDetailsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.UnitOverviewOverlay;
import megamek.client.ui.clientGUI.boardview.sprite.*;
import megamek.client.ui.clientGUI.boardview.sprite.TacticalSprite;
import megamek.client.ui.clientGUI.boardview.sprite.isometric.IsometricSprite;
import megamek.client.ui.clientGUI.boardview.sprite.isometric.IsometricWreckSprite;
import megamek.client.ui.clientGUI.boardview.toolTip.BoardViewTooltipProvider;
import megamek.client.ui.dialogs.phaseDisplay.EntityChoiceDialog;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.tileset.TilesetManager;
import megamek.client.ui.util.EntityWreckHelper;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.ImageCache;
import megamek.client.ui.util.KeyBindReceiver;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.MegaMekBorder;
import megamek.client.ui.widget.SkinSpecification;
import megamek.client.ui.widget.SkinSpecification.UIComponents;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.common.ArtilleryModifier;
import megamek.common.Configuration;
import megamek.common.ECMInfo;
import megamek.common.Hex;
import megamek.common.KeyBindParser;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.AllowedDeploymentHelper;
import megamek.common.board.Board;
import megamek.common.board.BoardHelper;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.FacingOption;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.compute.ComputeECM;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentActivation;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.board.BoardEvent;
import megamek.common.event.board.BoardListener;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.board.GameBoardNewEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.pathfinder.BoardClusterTracker.BoardCluster;
import megamek.common.planetaryConditions.IlluminationLevel;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.server.props.OrbitalBombardment;

/**
 * Displays the board; lets the user scroll around and select points on it.
 */
public final class BoardView extends AbstractBoardView
      implements BoardListener, MouseListener, IPreferenceChangeListener, KeyBindReceiver {
    private static final MMLogger LOGGER = MMLogger.create(BoardView.class);

    public static final int BOARD_HEX_CLICK = 1;
    public static final int BOARD_HEX_DOUBLE_CLICK = 2;
    public static final int BOARD_HEX_DRAG = 3;
    private static final int BOARD_HEX_POPUP = 4;

    // the dimensions of MegaMek's hex images
    public static final int HEX_DIAG = (int) Math.round(Math.sqrt(HEX_W * HEX_W + HEX_H * HEX_H));

    static final int HEX_WC = HEX_W - (HEX_W / 4);

    /**
     * This value is the vertical pixel offset for each hex elevation that is used to draw isometric mode
     */
    public static final int ISOMETRIC_OFFSET = 12;

    /**
     * This value is the vertical offset used currently for hex drawing. Its value is set to 0 for non-iso mode and
     * ISOMETRIC_OFFSET for iso mode. The drawing code itself uses the same logic to draw both modes.
     */
    private int verticalOffset = 0;

    private static final float[] ZOOM_FACTORS = { 0.30f, 0.41f, 0.50f, 0.60f, 0.68f, 0.79f, 0.90f, 1.00f, 1.09f, 1.17f,
                                                  1.3f, 1.6f, 2.0f, 3.0f };

    private static final int[] ZOOM_SCALE_TYPES = { ImageUtil.IMAGE_SCALE_AVG_FILTER, ImageUtil.IMAGE_SCALE_AVG_FILTER,
                                                    ImageUtil.IMAGE_SCALE_BICUBIC, ImageUtil.IMAGE_SCALE_BICUBIC,
                                                    ImageUtil.IMAGE_SCALE_BICUBIC, ImageUtil.IMAGE_SCALE_BICUBIC,
                                                    ImageUtil.IMAGE_SCALE_BICUBIC, ImageUtil.IMAGE_SCALE_BICUBIC,
                                                    ImageUtil.IMAGE_SCALE_BICUBIC, ImageUtil.IMAGE_SCALE_BICUBIC,
                                                    ImageUtil.IMAGE_SCALE_BICUBIC, ImageUtil.IMAGE_SCALE_BICUBIC,
                                                    ImageUtil.IMAGE_SCALE_BICUBIC, ImageUtil.IMAGE_SCALE_BICUBIC };

    public static final int[] allDirections = { 0, 1, 2, 3, 4, 5 };

    public int DROP_SHADOW_DISTANCE = 20;

    // the index of zoom factor 1.00f
    static final int BASE_ZOOM_INDEX = 7;

    // Initial zoom index
    public int zoomIndex = BASE_ZOOM_INDEX;

    // Set Zoom Out Overview Toggle inactive.
    public boolean zoomOverview = false;

    // line width of the c3 network lines
    public static final int C3_LINE_WIDTH = 1;

    // line width of the fly over lines
    public static final int FLY_OVER_LINE_WIDTH = 3;
    private static final Font FONT_7 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 7);
    private static final Font FONT_9 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 9);
    private static final Font FONT_10 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 10);
    private static final Font FONT_12 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12);
    private static final Font FONT_14 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 14);
    private static final Font FONT_16 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 16);
    private static final Font FONT_18 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 18);
    private static final Font FONT_24 = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 24);

    /**
     * Distance hex text labels keep from the tile edge they hang from, in tile artwork pixels: the coordinate label
     * keeps it from the tile's top edge, the level/depth/height/foliage stack from its bottom edge, so the text keeps
     * a visible margin instead of touching the hex border. The GPU board draws the same labels from these offsets.
     */
    private static final int HEX_TEXT_MARGIN = 6;

    Dimension hex_size;

    private final Font font_note = FONT_10;
    private Font font_hexNumber = FONT_10;
    private Font font_elev = FONT_9;
    private Font font_minefield = FONT_12;

    private final JPanel boardPanel = new BoardViewPanel(this);

    public final Game game;
    ClientGUI clientgui;

    private Dimension boardSize;

    // scroll stuff:
    private JScrollPane scrollPane = null;
    private JScrollBar verticalBar;
    private JScrollBar horizontalBar;
    private int scrollXDifference = 0;
    private int scrollYDifference = 0;
    private int preZoomOverviewIndex = 0;
    private int preZoomOverviewViewX = 0;
    private int preZoomOverviewViewY = 0;
    private int bestZoomFactor = 0;
    // are we drag-scrolling?
    private boolean dragging = false;
    private boolean wantsPopup = false;

    /** True when the right mouse button was pressed to start a drag */
    private boolean shouldScroll = false;

    // entity sprites
    private Queue<EntitySprite> entitySprites = new PriorityQueue<>();
    private Queue<IsometricSprite> isometricSprites = new PriorityQueue<>();
    /**
     * A Map that maps an Entity ID and a secondary position to a Sprite. Note that the key is a List where the first
     * entry will be the Entity ID and the second entry will be which secondary position the sprite belongs to; if the
     * Entity has no secondary positions, the first element will be the ID and the second element will be -1.
     */
    private Map<ArrayList<Integer>, EntitySprite> entitySpriteIds = new HashMap<>();
    /**
     * A Map that maps an Entity ID and a secondary position to a Sprite. Note that the key is a List where the first
     * entry will be the Entity ID and the second entry will be which secondary position the sprite belongs to; if the
     * Entity has no secondary positions, the first element will be the ID and the second element will be -1.
     */
    private Map<ArrayList<Integer>, IsometricSprite> isometricSpriteIds = new HashMap<>();

    // sprites for the three selection cursors
    private final CursorSprite cursorSprite;
    private final CursorSprite highlightSprite;
    private final CursorSprite selectedSprite;
    private final CursorSprite firstLOSSprite;
    private final CursorSprite secondLOSSprite;

    // sprite for current movement
    ArrayList<StepSprite> pathSprites = new ArrayList<>();
    ArrayList<FlightPathIndicatorSprite> fpiSprites = new ArrayList<>();

    private final ArrayList<Coords> strafingCoords = new ArrayList<>(5);

    // vector of sprites for all firing lines
    private final ArrayList<AttackSprite> attackSprites = new ArrayList<>();

    // vector of sprites for all movement paths (using vectored movement)
    private final ArrayList<MovementSprite> movementSprites = new ArrayList<>();

    // vector of sprites for C3 network lines
    private final ArrayList<C3Sprite> c3Sprites = new ArrayList<>();

    // list of sprites for declared VTOL/AirMek bombing/strafing targets
    private final ArrayList<VTOLAttackSprite> vtolAttackSprites = new ArrayList<>();

    // vector of sprites for aero flyover lines
    private final ArrayList<FlyOverSprite> flyOverSprites = new ArrayList<>();

    TilesetManager tileManager;
    private HexTileset gpuTileset;

    // polygons for a few things
    private static final Polygon HEX_POLY;

    static {
        // hex polygon
        HEX_POLY = new Polygon();
        HEX_POLY.addPoint(21, 0);
        HEX_POLY.addPoint(62, 0);
        HEX_POLY.addPoint(83, 35);
        HEX_POLY.addPoint(83, 36);
        HEX_POLY.addPoint(62, 71);
        HEX_POLY.addPoint(21, 71);
        HEX_POLY.addPoint(0, 36);
        HEX_POLY.addPoint(0, 35);
    }

    Shape[] movementPolys;
    Shape[] facingPolys;
    Shape[] finalFacingPolys;
    Shape upArrow;
    Shape downArrow;

    // Image to hold the complete board shadow map
    BufferedImage shadowMap;
    private boolean gpuCapture;

    /**
     * Stores the currently deploying entity, used for highlighting deployment hexes.
     */
    private Entity en_Deployer = null;

    // should be able to turn it off(board editor)
    private boolean useLOSTool = true;

    // Initial scale factor for sprites and map
    float scale = 1.00f;
    private ImageCache<Integer, Image> scaledImageCache = new ImageCache<>();
    private final ImageCache<Integer, BufferedImage> shadowImageCache = new ImageCache<>();

    private final Set<Integer> animatedImages = new HashSet<>();

    // Move units step by step
    private final ArrayList<MovingUnit> movingUnits = new ArrayList<>();

    private long moveWait = 0;

    // moving entity sprites
    private ArrayList<MovingEntitySprite> movingEntitySprites = new ArrayList<>();
    private HashMap<Integer, MovingEntitySprite> movingEntitySpriteIds = new HashMap<>();
    private final ArrayList<GhostEntitySprite> ghostEntitySprites = new ArrayList<>();

    // wreck sprites
    private ArrayList<WreckSprite> wreckSprites = new ArrayList<>();
    private ArrayList<IsometricWreckSprite> isometricWreckSprites = new ArrayList<>();

    // highlighted entity hexes (for Nova CEWS network dialog)
    private List<Coords> highlightedEntityHexes = new ArrayList<>();

    // highlighted demolition charge hexes (selected in the Detonate Charges dialog) - drawn with a bold yellow/black
    // hazard outline, separate from the plain entity highlight above
    private List<Coords> demolitionChargeHighlightHexes = new ArrayList<>();

    private Coords rulerStart;
    private Coords rulerEnd;
    private Color rulerStartColor;
    private Color rulerEndColor;

    private Coords lastCursor;
    Coords selected;
    private Coords firstLOS;

    /** stores the theme last selected to override all hex themes */
    private String selectedTheme = null;

    // hexes with ECM effect
    private Map<Coords, Color> ecmHexes = null;
    // hexes that are teh centers of ECM effects
    private Map<Coords, Color> ecmCenters = null;
    // hexes with ECM effect
    private Map<Coords, Color> eccmHexes = null;
    // hexes that are teh centers of ECCM effects
    private Map<Coords, Color> eccmCenters = null;

    // reference to our timer task for redraw
    private final TimerTask redrawTimerTask;

    BufferedImage bvBgImage = null;
    boolean bvBgShouldTile = false;
    BufferedImage scrollPaneBgBuffer = null;
    Image scrollPaneBgImg = null;

    private static final int FRAMES = 24;
    private long totalTime;
    private long averageTime;
    private int frameCount;
    private final Font fpsFont = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 20);

    /**
     * Keeps track of whether we have an active ChatterBox2
     */
    private boolean chatterBoxActive = false;

    /**
     * Keeps track of whether an outside source tells the BoardView that it should ignore keyboard commands.
     */
    private boolean shouldIgnoreKeys = false;

    private final FovHighlightingAndDarkening fovHighlightingAndDarkening;

    private static final String FILENAME_RADAR_BLIP_IMAGE = "radarBlip.png";
    private final Image radarBlipImage;

    /**
     * Cache that stores hex images for different coords
     */
    ImageCache<Coords, HexImageCacheEntry> hexImageCache;
    private final ImageCache<Coords, HexImageCacheEntry> planarHexImageCache = new ImageCache<>();

    /**
     * GPU hex layers, kept per hex so captures do not re-match the tileset every frame: the ground artwork
     * the hex paints, the same ground without its water, and the features drawn over both.
     */
    private record GroundArtwork(BufferedImage color, BufferedImage normal) { }
    private final Map<Coords, GroundArtwork> groundArtwork = new HashMap<>();
    private final Map<String, Image> groundNormals = new HashMap<>();
    private final Map<Coords, DecalArtwork> featureArtwork = new HashMap<>();

    private boolean showLobbyPlayerDeployment = false;

    private long paintCompsStartTime;

    private Rectangle displayablesRect = new Rectangle();

    // Soft Centering ---

    /** True when the board is in the process of centering to a spot. */
    private boolean isSoftCentering = false;
    /**
     * The final position of a soft centering relative to board size (x, y = 0...1).
     */
    private final Point2D softCenterTarget = new Point2D.Double();
    private Point2D oldCenter = new Point2D.Double();
    private long waitTimer;
    /** Speed of soft centering of the board, less is faster */
    private static final int SOFT_CENTER_SPEED = 8;

    /**
     * Holds the final Coords for a planned movement. Set by MovementDisplay, used to display the distance in the board
     * tooltip.
     */
    private Coords movementTarget;

    // Used to track the previous x/y for tooltip display
    int prevTipX = -1, prevTipY = -1;

    /**
     * Flag to indicate if we should display information about illegal terrain in hexes.
     */
    boolean displayInvalidHexInfo = false;

    /**
     * Stores the correct tooltip dismiss delay so it can be restored when exiting the BoardView
     */
    private final int dismissDelay = ToolTipManager.sharedInstance().getDismissDelay();

    /** The coords where the mouse was last. */
    Coords lastCoords;

    private final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final TerrainShadowHelper shadowHelper = new TerrainShadowHelper(this);

    /**
     * Keeps track of whether all deployment zones should be shown in the Arty Auto Hit Designation phase
     */
    public boolean showAllDeployment = false;

    private final StringDrawer invalidString =
          new StringDrawer(Messages.getString("BoardEditor.INVALID")).color(GUIP.getWarningColor())
                .font(FontHandler.notoFont().deriveFont(Font.BOLD))
                .center();

    BoardViewTooltipProvider boardViewToolTip = (point, movementTarget) -> null;
    private boolean tooltipSuspended = false;

    // Part of the sprites need specialized treatment; as there can be many sprites, filtering them on the spot is a
    // noticeable performance hit (in iso mode), therefore the sprites are copied to specialized lists when created
    private final TreeSet<Sprite> overTerrainSprites = new TreeSet<>();
    private final TreeSet<HexSprite> behindTerrainHexSprites = new TreeSet<>();

    private final List<HexDrawPlugin> hexDrawPlugins = new ArrayList<>();

    /**
     * Construct a new board view for the specified game
     */
    public BoardView(final Game game, final MegaMekController controller, @Nullable ClientGUI clientgui, int boardId)
          throws IOException {
        super(boardId);
        this.game = game;
        this.clientgui = clientgui;

        hexImageCache = new ImageCache<>();
        tileManager = new TilesetManager(game);
        ToolTipManager.sharedInstance().registerComponent(boardPanel);
        setVerticalOffset();

        // For Entities that have converted to another mode, check for a different sprite for units that have been
        // blown up, damaged or ejected, force a reload Clear some information regardless of what phase it is
        GameListener gameListener = new GameListenerAdapter() {

            @Override
            public void gameEntityNew(GameEntityNewEvent gameEntityNewEvent) {
                updateEcmList();
                redrawAllEntities();
                if (game.getPhase().isMovement()) {
                    refreshMoveVectors();
                }
            }

            @Override
            public void gameEntityRemove(GameEntityRemoveEvent gameEntityRemoveEvent) {
                updateEcmList();
                redrawAllEntities();
                if (game.getPhase().isMovement()) {
                    refreshMoveVectors();
                }
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent gameEntityChangeEvent) {
                final Vector<UnitLocation> movePath = gameEntityChangeEvent.getMovePath();
                final Entity entity = gameEntityChangeEvent.getEntity();
                final var gameOptions = game.getOptions();

                updateEcmList();

                // For Entities that have converted to another mode, check for a different sprite
                if (game.getPhase().isMovement() && entity.isConvertingNow()) {
                    tileManager.reloadImage(entity);
                }

                // For units that have been blown up, damaged, ejected or handed to another player (a traitor
                // switch means the new owner's camouflage), force a reload. Without the old state we cannot tell
                // whether the damage changed, so reload to be safe; the reload reuses the cached image when
                // nothing shown in fact changed, so it costs nothing in the common case.
                final Entity oldEntity = gameEntityChangeEvent.getOldEntity();
                boolean shownStateChanged = true;
                if (oldEntity != null) {
                    boolean damageChanged = entity.getDamageLevel() != oldEntity.getDamageLevel();
                    boolean destructionChanged = entity.isDestroyed() != oldEntity.isDestroyed();
                    boolean ownerChanged = entity.getOwnerId() != oldEntity.getOwnerId();
                    boolean ejectionChanged = entity.getCrew().isEjected() != oldEntity.getCrew().isEjected();
                    shownStateChanged = damageChanged || destructionChanged || ownerChanged || ejectionChanged;
                }
                if (shownStateChanged) {
                    tileManager.reloadImage(entity);
                }

                redrawAllEntities();

                if (game.getPhase().isMovement()) {
                    refreshMoveVectors();
                }

                if ((movePath != null) && !movePath.isEmpty() && GUIP.getShowMoveStep() && !gameOptions.booleanOption(
                      OptionsConstants.INIT_SIMULTANEOUS_MOVEMENT)) {
                    if ((localPlayer == null) || !game.getOptions()
                          .booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) || !entity.getOwner()
                          .isEnemyOf(localPlayer) || entity.hasSeenEntity(localPlayer)) {
                        addMovingUnit(entity, movePath);
                    }
                }
            }

            @Override
            public void gameNewAction(GameNewActionEvent gameNewActionEvent) {
                EntityAction entityAction = gameNewActionEvent.getAction();
                if (entityAction instanceof AttackAction attackAction) {
                    addAttack(attackAction);
                }
            }

            @Override
            public void gameBoardNew(GameBoardNewEvent gameBoardNewEvent) {
                Board oldBoard = gameBoardNewEvent.getOldBoard();

                if (oldBoard != null) {
                    oldBoard.removeBoardListener(BoardView.this);
                }

                oldBoard = gameBoardNewEvent.getNewBoard();

                if (oldBoard != null) {
                    oldBoard.addBoardListener(BoardView.this);
                }

                game.getBoard(boardId).initializeAllAutomaticTerrain();
                clearHexImageCache();
                updateBoard();
                clearShadowMap();
            }

            @Override
            public void gameBoardChanged(GameBoardChangeEvent gameBoardChangeEvent) {
                clearHexImageCache();
                boardChanged();
                // Update ECM list for temporary ECM fields (from EMP mines, etc.)
                updateEcmList();
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent gamePhaseChangeEvent) {
                if (GUIP.getGameSummaryBoardView() && (gamePhaseChangeEvent.getOldPhase().isDeployment()
                      || gamePhaseChangeEvent.getOldPhase().isMovement()
                      || gamePhaseChangeEvent.getOldPhase().isTargeting()
                      || gamePhaseChangeEvent.getOldPhase().isFiring()
                      || gamePhaseChangeEvent.getOldPhase().isPhysical())) {
                    File dir = new File(Configuration.gameSummaryImagesBVDir(), game.getUUIDString());

                    if (dir.exists() || dir.mkdirs()) {
                        String fileName = String.format("round_%03d_%03d_%s.png",
                              game.getRoundCount(),
                              gamePhaseChangeEvent.getOldPhase().ordinal(),
                              gamePhaseChangeEvent.getOldPhase());

                        File imgFile = new File(dir, fileName);

                        try {
                            ImageIO.write(getEntireBoardImage(false, true), "png", imgFile);
                        } catch (Exception ex) {
                            LOGGER.error(ex, "Unable to write board image {}", imgFile);
                        }
                    }
                }

                refreshAttacks();

                // Clear some information regardless of what phase it is
                if (clientgui != null) {
                    clientgui.clearTemporarySprites();
                }

                switch (gamePhaseChangeEvent.getNewPhase()) {
                    case MOVEMENT:
                        refreshMoveVectors();
                    case FIRING:
                        clearAllMoveVectors();
                    case PHYSICAL:
                        refreshAttacks();
                        break;
                    case INITIATIVE:
                        clearAllAttacks();
                        break;
                    case INITIATIVE_REPORT:
                    case MOVEMENT_REPORT:
                    case FIRING_REPORT:
                    case PHYSICAL_REPORT:
                    case END_REPORT:
                        // Rebuild entity sprites (including C3 connection lines) for report phases
                        redrawAllEntities();
                        break;
                    case END:
                    case VICTORY:
                        clearSprites();
                    case LOUNGE:
                        clearHexImageCache();
                        clearAllMoveVectors();
                        clearAllAttacks();
                        clearSprites();
                        select(null);
                        cursor(null);
                        highlight(null);
                    default:
                }
                for (Entity entity : game.getEntitiesVector()) {
                    if ((entity.getDamageLevel() != Entity.DMG_NONE) && ((entity.damageThisRound != 0)
                          || (entity.isBuildingEntityOrGunEmplacement()))) {
                        tileManager.reloadImage(entity);
                    }
                }

            }
        };

        game.addGameListener(gameListener);
        game.getBoard(boardId).addBoardListener(this);

        redrawTimerTask = scheduleRedrawTimer(); // call only once
        clearSprites();
        boardPanel.addMouseListener(this);
        boardPanel.addMouseWheelListener(mouseWheelEvent -> {
            Point mousePoint = mouseWheelEvent.getPoint();
            Point dispPoint = new Point(mousePoint.x + boardPanel.getBounds().x,
                  mousePoint.y + boardPanel.getBounds().y);

            // If the mouse is over an IDisplayable, have it react instead of the board. Currently only implemented
            // for the ChatterBox
            for (IDisplayable displayable : overlays) {
                if (displayable instanceof ChatterBoxOverlay chatterBox2) {
                    double width = scrollPane.getViewport().getSize().getWidth();
                    double height = scrollPane.getViewport().getSize().getHeight();
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);

                    // mouseWheelEvent need to adjust the point, because it should be against the displayable dimension
                    if (displayable.isMouseOver(dispPoint, drawDimension)) {
                        if (mouseWheelEvent.getWheelRotation() > 0) {
                            chatterBox2.scrollDown();
                        } else {
                            chatterBox2.scrollUp();
                        }

                        refreshDisplayables();
                        return;
                    }
                }
            }

            // calculate a few things to reposition the map
            Coords zoomCenter = getCoordsAt(mouseWheelEvent.getPoint());
            Point hexL = getCentreHexLocation(zoomCenter);
            Point inHexDelta = new Point(mouseWheelEvent.getPoint());
            inHexDelta.translate(-HEX_W, -HEX_H);
            inHexDelta.translate(-hexL.x, -hexL.y);
            double inHexDeltaX = ((double) inHexDelta.x) / ((double) HEX_W) / scale;
            double inHexDeltaY = ((double) inHexDelta.y) / ((double) HEX_H) / scale;
            int oldZoomIndex = zoomIndex;

            boolean ZoomNoCtrl = GUIP.getMouseWheelZoom();
            boolean wheelFlip = GUIP.getMouseWheelZoomFlip();
            boolean zoomIn = (mouseWheelEvent.getWheelRotation() > 0) ^ wheelFlip; // = XOR
            boolean doZoom = ZoomNoCtrl ^ mouseWheelEvent.isControlDown(); // = XOR
            boolean horizontalScroll = !doZoom && mouseWheelEvent.isShiftDown();

            if (doZoom) {
                if (zoomIn) {
                    zoomIn();
                } else {
                    zoomOut();
                }

                if (zoomIndex != oldZoomIndex) {
                    adjustVisiblePosition(zoomCenter, dispPoint, inHexDeltaX, inHexDeltaY);
                }
            } else {
                // SCROLL
                if (horizontalScroll) {
                    horizontalBar.setValue((int) (horizontalBar.getValue() + (HEX_H
                          * scale
                          * (mouseWheelEvent.getWheelRotation()))));
                } else {
                    verticalBar.setValue((int) (verticalBar.getValue() + (HEX_H
                          * scale
                          * (mouseWheelEvent.getWheelRotation()))));
                }
                stopSoftCentering();
            }

            pingMinimap();
        });

        MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                Point point = mouseEvent.getPoint();
                for (IDisplayable displayable : overlays) {

                    if (displayable.isBeingDragged()) {
                        return;
                    }

                    double width = Math.min(boardSize.getWidth(), scrollPane.getViewport().getSize().getWidth());
                    double height = Math.min(boardSize.getHeight(), scrollPane.getViewport().getSize().getHeight());
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    displayable.isMouseOver(point, drawDimension);
                }

                // Reset popup flag if the user moves their mouse away
                wantsPopup = false;

                final Coords mcoords = getCoordsAt(point);
                if (!mcoords.equals(lastCoords) && game.getBoard(boardId).contains(mcoords)) {
                    if (tooltipSuspended) {
                        boardPanel.setToolTipText(null);
                    } else {
                        lastCoords = mcoords;
                        boardPanel.setToolTipText(boardViewToolTip.getTooltip(mouseEvent, movementTarget));
                    }
                } else if (!game.getBoard(boardId).contains(mcoords)) {
                    boardPanel.setToolTipText(null);
                } else {
                    if (prevTipX > 0 && prevTipY > 0) {
                        int deltaX = point.x - prevTipX;
                        int deltaY = point.y - prevTipY;
                        double deltaMagnitude = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        if (deltaMagnitude > GUIP.getTooltipDistSuppression()) {
                            prevTipX = -1;
                            prevTipY = -1;
                            // Set the dismissal delay to 0 so that the tooltip goes away and does not reappear until
                            // the mouse has moved more than the suppression distance
                            ToolTipManager.sharedInstance().setDismissDelay(0);
                            // and then, when the tooltip has gone away, reset the dismiss delay
                            SwingUtilities.invokeLater(() -> {
                                if (GUIP.getTooltipDismissDelay() >= 0) {
                                    ToolTipManager.sharedInstance().setDismissDelay(GUIP.getTooltipDismissDelay());
                                } else {
                                    ToolTipManager.sharedInstance().setDismissDelay(dismissDelay);
                                }
                            });
                        }
                    }
                    prevTipX = point.x;
                    prevTipY = point.y;
                }
            }

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                Point point = mouseEvent.getPoint();
                for (IDisplayable displayable : overlays) {
                    Point adjustPoint = new Point((int) Math.min(boardSize.getWidth(), -boardPanel.getBounds().getX()),
                          (int) Math.min(boardSize.getHeight(), -boardPanel.getBounds().getY()));
                    Point dispPoint = new Point();
                    dispPoint.x = point.x - adjustPoint.x;
                    dispPoint.y = point.y - adjustPoint.y;
                    double width = Math.min(boardSize.getWidth(), scrollPane.getViewport().getSize().getWidth());
                    double height = Math.min(boardSize.getHeight(), scrollPane.getViewport().getSize().getHeight());
                    Dimension drawDimension = new Dimension();
                    drawDimension.setSize(width, height);
                    if (displayable.isDragged(dispPoint, drawDimension)) {
                        boardPanel.repaint();
                        return;
                    }
                }
                // only scroll when we should
                if (!shouldScroll) {
                    mouseAction(getCoordsAt(point),
                          BOARD_HEX_DRAG,
                          mouseEvent.getModifiersEx(),
                          mouseEvent.getButton());
                    return;
                }
                // if we have not yet been dragging, set the var so popups don't appear when we stop scrolling
                if (!dragging) {
                    dragging = true;
                    boardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                Point p = scrollPane.getViewport().getViewPosition();
                int newX = p.x - (mouseEvent.getX() - scrollXDifference);
                int newY = p.y - (mouseEvent.getY() - scrollYDifference);
                int maxX = boardPanel.getWidth() - scrollPane.getViewport().getWidth();
                int maxY = boardPanel.getHeight() - scrollPane.getViewport().getHeight();

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
                if (scrollPane.getViewport().getWidth() >= boardPanel.getWidth()) {
                    newX = scrollPane.getViewport().getViewPosition().x;
                }

                scrollPane.getViewport().setViewPosition(new Point(newX, newY));
                pingMinimap();
            }
        };
        boardPanel.addMouseMotionListener(mouseMotionListener);

        if (controller != null) {
            registerKeyboardCommands(controller);
        }

        updateBoardSize();

        hex_size = new Dimension((int) (HEX_W * scale), (int) (HEX_H * scale));

        initPolys();

        cursorSprite = new CursorSprite(this, Color.cyan);
        highlightSprite = new CursorSprite(this, Color.white);
        selectedSprite = new CursorSprite(this, Color.blue);
        firstLOSSprite = new CursorSprite(this, Color.red);
        secondLOSSprite = new CursorSprite(this, Color.red);

        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
        GUIP.addPreferenceChangeListener(this);
        KeyBindParser.addPreferenceChangeListener(this);

        SpecialHexDisplay.Type.ARTILLERY_MISS.init();
        SpecialHexDisplay.Type.ARTILLERY_HIT.init();
        SpecialHexDisplay.Type.ARTILLERY_DRIFT.init();
        SpecialHexDisplay.Type.ARTILLERY_INCOMING.init();
        SpecialHexDisplay.Type.ARTILLERY_TARGET.init();
        SpecialHexDisplay.Type.ARTILLERY_ADJUSTED.init();
        SpecialHexDisplay.Type.ARTILLERY_AUTO_HIT.init();
        SpecialHexDisplay.Type.BOMB_MISS.init();
        SpecialHexDisplay.Type.BOMB_HIT.init();
        SpecialHexDisplay.Type.BOMB_DRIFT.init();
        SpecialHexDisplay.Type.PLAYER_NOTE.init();
        SpecialHexDisplay.Type.ORBITAL_BOMBARDMENT.init();
        SpecialHexDisplay.Type.ORBITAL_BOMBARDMENT_INCOMING.init();
        SpecialHexDisplay.Type.NUKE_HIT.init();
        SpecialHexDisplay.Type.NUKE_INCOMING.init();

        fovHighlightingAndDarkening = new FovHighlightingAndDarkening(this);

        radarBlipImage = ImageUtil.loadImageFromFile(new MegaMekFile(Configuration.miscImagesDir(),
              FILENAME_RADAR_BLIP_IMAGE).toString());
    }

    private void registerKeyboardCommands(final MegaMekController controller) {
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT, this, this::performChat);
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CHAT_CMD, this, this::performChatCmd);
        controller.registerCommandAction(KeyCommandBind.CENTER_ON_SELECTED, this, this::centerOnSelected);

        controller.registerCommandAction(KeyCommandBind.SCROLL_NORTH,
              this::canScrollClassicView,
              this::scrollNorth,
              this::pingMinimap);
        controller.registerCommandAction(KeyCommandBind.SCROLL_SOUTH,
              this::canScrollClassicView,
              this::scrollSouth,
              this::pingMinimap);
        controller.registerCommandAction(KeyCommandBind.SCROLL_EAST,
              this::canScrollClassicView,
              this::scrollEast,
              this::pingMinimap);
        controller.registerCommandAction(KeyCommandBind.SCROLL_WEST,
              this::canScrollClassicView,
              this::scrollWest,
              this::pingMinimap);
    }

    private boolean canScrollClassicView() {
        return scrollPane != null && shouldReceiveKeyCommands()
              && (clientgui == null || !GpuBoardWindow.isActiveFor(clientgui));
    }

    private void scrollNorth() {
        verticalBar.setValue((int) (verticalBar.getValue() - (HEX_H * scale)));
        stopSoftCentering();
    }

    private void scrollSouth() {
        verticalBar.setValue((int) (verticalBar.getValue() + (HEX_H * scale)));
        stopSoftCentering();
    }

    private void scrollEast() {
        horizontalBar.setValue((int) (horizontalBar.getValue() + (HEX_W * scale)));
        stopSoftCentering();
    }

    private void scrollWest() {
        horizontalBar.setValue((int) (horizontalBar.getValue() - (HEX_W * scale)));
        stopSoftCentering();
    }

    private void performChatCmd() {
        if (!getChatterBoxActive()) {
            setChatterBoxActive(true);
            for (IDisplayable displayable : overlays) {
                if (displayable instanceof ChatterBoxOverlay chatterBox2) {
                    chatterBox2.slideUp();
                    chatterBox2.setMessage("/");
                }
            }
            boardPanel.requestFocus();
        }
    }

    private void performChat() {
        if (!getChatterBoxActive()) {
            setChatterBoxActive(true);
            for (IDisplayable displayable : overlays) {
                if (displayable instanceof ChatterBoxOverlay chatterBox2) {
                    chatterBox2.slideUp();
                }
            }
            boardPanel.requestFocus();
        }
    }

    @Override
    public boolean shouldReceiveKeyCommands() {
        return !getChatterBoxActive() && boardPanel.isVisible() && !game.getPhase().isLounge() && !shouldIgnoreKeys;
    }

    private final RedrawWorker redrawWorker = new RedrawWorker();
    private final AtomicBoolean redrawPending = new AtomicBoolean();

    /**
     * this should only be called once!! this will cause a timer to schedule constant screen updates every 20
     * milliseconds!
     */
    private TimerTask scheduleRedrawTimer() {
        final TimerTask redraw = new TimerTask() {
            @Override
            public void run() {
                scheduleRedraw();
            }
        };
        TimerSingleton.getInstance().schedule(redraw, 20, 20);
        return redraw;
    }

    private void scheduleRedraw() {
        if (!redrawPending.compareAndSet(false, true)) {
            return;
        }
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    redrawWorker.run();
                } finally {
                    redrawPending.set(false);
                }
            });
        } catch (Exception ignored) {
            redrawPending.set(false);
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        invalidatePlanarCapture();
        switch (e.getName()) {
            case GUIPreferences.SHOW_DEPLOY_ZONES_ARTY_AUTO:
                showAllDeployment = (boolean) e.getNewValue();
                repaint();
                break;

            case ClientPreferences.MAP_TILESET:
                clearHexImageCache();
                updateBoard();
                break;

            case GUIPreferences.UNIT_LABEL_STYLE:
                if (clientgui != null) {
                    clientgui.systemMessage("Label style changed to " + GUIP.getUnitLabelStyle().description);
                }
            case GUIPreferences.UNIT_LABEL_BORDER:
            case GUIPreferences.TEAM_COLORING:
            case GUIPreferences.SHOW_DAMAGE_DECAL:
            case GUIPreferences.SHOW_DAMAGE_LEVEL:
                updateEntityLabels();

                for (Sprite s : wreckSprites) {
                    s.prepare();
                }

                for (Sprite s : isometricWreckSprites) {
                    s.prepare();
                }

                break;
            case GUIPreferences.USE_CAMO_OVERLAY:
                tileManager.reloadUnitIcons();
                break;

            case GUIPreferences.USE_ISOMETRIC:
                toggleIsometric();
                break;

            case GUIPreferences.SHOW_MAP_SHEETS:
            case GUIPreferences.BOARD_MAP_SHEET_COLOR:
                hexImageCache.clear();
                boardPanel.repaint();
                break;
            case GUIPreferences.BOARD_ECM_TRANSPARENCY:
                updateEcmList();
                break;

            case GUIPreferences.AO_HEX_SHADOWS:
            case GUIPreferences.FLOATING_ISO:
            case GUIPreferences.LEVEL_HIGHLIGHT:
            case GUIPreferences.SHOW_COORDS:
            case GUIPreferences.FOV_DARKEN:
            case GUIPreferences.FOV_DARKEN_ALPHA:
            case GUIPreferences.FOV_GRAYSCALE:
            case GUIPreferences.FOV_SPOTTING_MODE:
            case GUIPreferences.LOS_MEK_IN_FIRST:
            case GUIPreferences.LOS_MEK_IN_SECOND:
            case GUIPreferences.FOV_HIGHLIGHT:
            case GUIPreferences.FOV_HIGHLIGHT_ALPHA:
            case GUIPreferences.FOV_STRIPES:
            case GUIPreferences.FOV_HIGHLIGHT_RINGS_COLORS_HSB:
            case GUIPreferences.FOV_HIGHLIGHT_RINGS_RADII:
            case GUIPreferences.SHADOW_MAP:
                clearHexImageCache();
                tileManager.reloadUnitIcons();
                boardPanel.repaint();
                break;
            case GUIPreferences.INCLINES:
                game.getBoard(boardId).initializeAllAutomaticTerrain();
                clearHexImageCache();
                boardPanel.repaint();
                break;
        }
    }

    void addMovingUnit(Entity entity, Vector<UnitLocation> movePath) {
        if (!movePath.isEmpty() && isOnThisBord(entity)) {
            MovingUnit m = new MovingUnit(entity, movePath);
            movingUnits.add(m);

            GhostEntitySprite ghostSprite = new GhostEntitySprite(this, entity);
            ghostEntitySprites.add(ghostSprite);

            // Center on the starting hex of the moving unit.
            UnitLocation loc = movePath.getFirst();

            if (GUIP.getAutoCenter()) {
                centerOnHex(loc.coords());
            }
        }
    }

    @Override
    public void draw(Graphics graphics) {
        if (!(graphics instanceof Graphics2D graphics2D)) {
            return;
        }
        if (GUIP.getShowFPS()) {
            paintCompsStartTime = java.lang.System.nanoTime();
        }

        UIUtil.setHighQualityRendering(graphics2D);
        Rectangle viewRect = scrollPane.getVisibleRect();

        if (!isTileImagesLoaded()) {
            graphics2D.setColor(Color.DARK_GRAY);
            graphics2D.fillRect(-boardPanel.getX(), -boardPanel.getY(), viewRect.width, viewRect.height);
            graphics2D.setColor(Color.LIGHT_GRAY);
            graphics2D.drawString(Messages.getString("BoardView1.loadingImages"), 20, 50);

            if (!tileManager.isStarted()) {
                LOGGER.info("Loading images for board");
                tileManager.loadNeededImages(game);
            }

            // wait 1 second, then repaint
            boardPanel.repaint(1000);
            return;
        }

        if (bvBgShouldTile && (bvBgImage != null)) {
            Rectangle clipping = graphics2D.getClipBounds();
            int x;
            int y = 0;
            int w = bvBgImage.getWidth();
            int h = bvBgImage.getHeight();

            while (y < (int) Math.floor(clipping.getHeight())) {
                int yRem = 0;

                if (y == 0) {
                    yRem = clipping.y % h;
                }

                x = 0;

                while (x < (int) Math.floor(clipping.getWidth())) {
                    int xRem = 0;
                    if (x == 0) {
                        xRem = clipping.x % w;
                    }
                    if ((xRem > 0) || (yRem > 0)) {
                        try {
                            graphics2D.drawImage(bvBgImage.getSubimage(xRem, yRem, w - xRem, h - yRem),
                                  clipping.x + x,
                                  clipping.y + y,
                                  boardPanel);
                        } catch (Exception e) {
                            // if we somehow messed up the math, log the error and simply act as if we have no
                            // background image.
                            Rectangle rasterBounds = bvBgImage.getRaster().getBounds();

                            String errorData = String.format(
                                  "Error drawing background image. Raster Bounds: %.2f, %.2f, width:%.2f, height:%.2f, Attempted Draw Coordinates: %d, %d, width:%d, height:%d",
                                  rasterBounds.getMinX(),
                                  rasterBounds.getMinY(),
                                  rasterBounds.getWidth(),
                                  rasterBounds.getHeight(),
                                  xRem,
                                  yRem,
                                  w - xRem,
                                  h - yRem);
                            LOGGER.error(errorData);
                        }
                    } else {
                        graphics2D.drawImage(bvBgImage, clipping.x + x, clipping.y + y, boardPanel);
                    }
                    x += w - xRem;
                }
                y += h - yRem;
            }
        } else if (bvBgImage != null) {
            graphics2D.drawImage(bvBgImage,
                  -boardPanel.getX(),
                  -boardPanel.getY(),
                  (int) viewRect.getWidth(),
                  (int) viewRect.getHeight(),
                  boardPanel);
        } else {
            MetalTheme theme = new DefaultMetalTheme();
            graphics2D.setColor(theme.getControl());
            graphics2D.fillRect(-boardPanel.getX(),
                  -boardPanel.getY(),
                  (int) viewRect.getWidth(),
                  (int) viewRect.getHeight());
        }

        // Used to pad the board edge
        graphics2D.translate(HEX_W, HEX_H);

        // Initialize the shadow map when it's not yet present
        if (shadowMap == null) {
            shadowMap = shadowHelper.updateShadowMap();
        }

        drawHexes(graphics2D, graphics2D.getClipBounds());

        drawTacticalLayers(graphics2D, true);

        // Undo the previous translation
        graphics2D.translate(-HEX_W, -HEX_H);

        // draw all the "displayable"
        if (displayablesRect == null) {
            displayablesRect = new Rectangle();
        }

        displayablesRect.x = -boardPanel.getX();
        displayablesRect.y = -boardPanel.getY();
        displayablesRect.width = scrollPane.getViewport().getViewRect().width;
        displayablesRect.height = scrollPane.getViewport().getViewRect().height;

        for (IDisplayable displayable : overlays) {
            displayable.draw(graphics2D, displayablesRect);
        }

        if (GUIP.getShowFPS()) {
            if (frameCount == FRAMES) {
                averageTime = totalTime / FRAMES;
                totalTime = 0;
                frameCount = 0;
            } else {
                totalTime += java.lang.System.nanoTime() - paintCompsStartTime;
                frameCount++;
            }

            String s = String.format("%1$5.3f", averageTime / 1000000d);
            graphics2D.setFont(fpsFont);
            graphics2D.setColor(Color.YELLOW);
            graphics2D.drawString(s, -boardPanel.getX() + 5, -boardPanel.getY() + 20);
        }

        // debugging method that renders the bounding box of a unit's movement envelope.
        // renderClusters((Graphics2D) graphics2D);
        // renderDonut(graphics2D, new Coords(10, 10), 2);
        // renderApproxHexDirection((Graphics2D) graphics2D);
        // renderMinefieldScores((Graphics2D) graphics2D);
    }

    /** Shared tactical presentation for the classic board and GPU surface layers. */
    private void drawTacticalLayers(Graphics2D graphics2D, boolean includeUnits) {
        // Minefield signs all over the place!
        drawMinefields(graphics2D);

        // Demolition charges set by the local player
        drawDemolitionCharges(graphics2D);

        // Artillery targets
        drawArtilleryHexes(graphics2D);
        drawOrbitalBombardmentHexes(graphics2D);

        // The GPU capture draws selection, hover and acting-unit outlines natively. Capturing the classic flat
        // hex cursors as well would render them on each hex's raised marking plane, split across neighbouring
        // levels wherever the hexes differ in elevation.
        if (includeUnits) {
            // draw highlight border
            drawSprite(graphics2D, highlightSprite);
        }

        // draw entity hex highlights (Nova CEWS network dialog)
        BoardTacticalGraphics.draw(graphics2D, BoardTactical.Playback.HIDE_DURING_MOVEMENT, this::drawEntityHexHighlights);

        // draw demolition charge selection highlights (Detonate Charges dialog)
        drawDemolitionChargeHighlights(graphics2D);

        // draw cursors
        if (includeUnits) {
            drawSprite(graphics2D, cursorSprite);
            drawSprite(graphics2D, selectedSprite);
        }
        drawSprite(graphics2D, firstLOSSprite);
        drawSprite(graphics2D, secondLOSSprite);

        // draw deployment indicators.
        if ((game.getPhase().isSetArtilleryAutoHitHexes() && showAllDeployment) || ((game.getPhase().isLounge())
              && showLobbyPlayerDeployment)) {
            BoardTacticalGraphics.draw(graphics2D, BoardTactical.Playback.HIDE_DURING_MOVEMENT, this::drawAllDeployment);
        }

        // A capture does not run drawHexes, which is where the deploying entity's legal deployment borders are
        // painted for the interactive board, so the captured layer carries them instead.
        if (!includeUnits && (en_Deployer != null)) {
            BoardTacticalGraphics.draw(graphics2D, BoardTactical.Playback.HIDE_DURING_MOVEMENT, this::drawDeploymentBorders);
        }

        // draw C3 links
        drawSprites(graphics2D, c3Sprites);

        // draw flyover routes
        if (game.getBoard(boardId).isGround()) {
            drawSprites(graphics2D, vtolAttackSprites);
            drawSprites(graphics2D, flyOverSprites);
        }

        // draw moving onscreen entities; a GPU capture leaves unit artwork out because it draws the moving token itself
        if (includeUnits) {
            drawSprites(graphics2D, movingEntitySprites);
            drawSprites(graphics2D, ghostEntitySprites);
        }

        // draw onscreen attacks
        drawSprites(graphics2D, attackSprites);

        // draw artillery drift lines (from the targeted hex to where the round actually landed)
        BoardTacticalGraphics.draw(graphics2D, BoardTactical.Playback.HOLD_DURING_PLAYBACK, this::drawArtilleryDriftLines);

        // draw movement vectors.
        if (game.useVectorMove() && game.getPhase().isMovement()) {
            drawSprites(graphics2D, movementSprites);
        }

        if (game.getPhase().isFiring() && (!gpuCapture || graphics2D instanceof BoardTacticalGraphics)) {
            BoardTacticalGraphics.draw(graphics2D, BoardTactical.Playback.HIDE_DURING_MOVEMENT, graphics -> {
                for (Coords c : strafingCoords) {
                    drawHexBorder(graphics, getHexLocation(c), Color.yellow, 0, 3);
                }
            });
        }

        // In iso mode, some sprites are drawn in drawHexes so they can go behind terrain; draw only the others here
          drawSprites(graphics2D, includeUnits ? overTerrainSprites : overTerrainSprites.stream()
              .filter(sprite -> !(sprite instanceof EntitySprite) && !(sprite instanceof IsometricSprite)).toList());

        // draw movement, if valid
        drawSprites(graphics2D, pathSprites);

        // draw flight path indicators
        drawSprites(graphics2D, fpiSprites);

        // draw the ruler line
        if (rulerStart != null && (!gpuCapture || graphics2D instanceof BoardTacticalGraphics)) {
            Point start = getCentreHexLocation(rulerStart);
            if (rulerEnd != null) {
                Point end = getCentreHexLocation(rulerEnd);
                graphics2D.setColor(Color.yellow);
                graphics2D.drawLine(start.x, start.y, end.x, end.y);

                drawRulerCrosshair(graphics2D, end, rulerEndColor);
            }

            drawRulerCrosshair(graphics2D, start, rulerStartColor);
        }

    }

    /**
     * Debugging method that renders a hex in the approximate direction from the selected entity to the selected hex, of
     * both exist.
     *
     * @param g Graphics object on which to draw.
     */
    @SuppressWarnings("unused")
    private void renderApproxHexDirection(Graphics2D g) {
        if (getSelectedEntity() == null || selected == null) {
            return;
        }

        int direction = getSelectedEntity().getPosition().approximateDirection(selected, 0, 0);

        Coords donutCoords = getSelectedEntity().getPosition().translated(direction);

        Point p = getCentreHexLocation(donutCoords.getX(), donutCoords.getY(), true);
        p.translate(HEX_W / 2, HEX_H / 2);
        drawHexBorder(g, p, Color.BLUE, 0, 6);
    }

    /**
     * Debugging method that renders a hex donut around the given coordinates, with the given radius.
     *
     * @param graphics2D Graphics object on which to draw.
     */
    @SuppressWarnings("unused")
    private void renderDonut(Graphics2D graphics2D, Coords coords, int radius) {
        ArrayList<Coords> donut = coords.allAtDistance(radius);

        for (Coords donutCoords : donut) {
            Point centreHexLocation = getCentreHexLocation(donutCoords.getX(), donutCoords.getY(), true);
            centreHexLocation.translate(HEX_W / 2, HEX_H / 2);
            drawHexBorder(graphics2D, centreHexLocation, Color.PINK, 0, 6);
        }
    }

    /**
     * Debugging method that renders an obnoxious pink lines around hexes in "Board Clusters"
     *
     * @param graphics2D Graphics object on which to draw.
     */
    @SuppressWarnings("unused")
    private void renderClusters(Graphics2D graphics2D) {
        BoardClusterTracker boardClusterTracker = new BoardClusterTracker();
        Map<Coords, BoardCluster> clusterMap = boardClusterTracker.generateClusters(getSelectedEntity(), false, true);

        for (BoardCluster cluster : clusterMap.values().stream().distinct().toList()) {
            for (Coords coords : cluster.contents.keySet()) {
                Point centreHexLocation = getCentreHexLocation(coords.getX(), coords.getY(), true);
                centreHexLocation.translate(HEX_W / 2, HEX_H / 2);
                drawHexBorder(graphics2D, centreHexLocation, new Color(0, 0, (20 * cluster.id) % 255), 0, 6);
            }
        }
    }

    /**
     * Debugging method used to render minefield effectiveness ratings
     */
    @SuppressWarnings("unused")
    private void renderMinefieldScores(Graphics2D graphics2D) {
    	/*Map<Coords, Integer> minefieldScores = mdp.getMinefieldScores(Minefield.TYPE_CONVENTIONAL, UnitType.TANK,
    			EntityMovementMode.WHEELED, getBoard());*/

    	MinefieldDeploymentPlanner mdp = new MinefieldDeploymentPlanner(getLocalPlayer(), game);
    	Map<Coords, Double> minefieldScores = mdp.buildCoalescedMinefieldScores(Minefield.TYPE_CONVENTIONAL, getBoard());

    	for (Coords coords : minefieldScores.keySet()) {
    		Point centreHexLocation = getCentreHexLocation(coords.getX(), coords.getY(), true);
            centreHexLocation.translate(HEX_W / 2, HEX_H);
            graphics2D.setColor(Color.pink);
            drawCenteredString(String.format("%3.1f", minefieldScores.get(coords)),
            		centreHexLocation.x, centreHexLocation.y, FONT_14, graphics2D);
    	}
    }

    public void clearShadowMap() {
        shadowMap = null;
        planarHexImageCache.clear();
        groundArtwork.clear();
        featureArtwork.clear();
    }

    public @Nullable Point getTerrainLightDirection() {
        return shadowHelper.lightDirection();
    }

    /**
     * Updates the boardSize variable with the proper values for this board.
     */
    void updateBoardSize() {
        int width = (game.getBoard(boardId).getWidth() * (int) (HEX_WC * scale)) + (int) ((HEX_W / 4.0f) * scale);
        int height = (game.getBoard(boardId).getHeight() * (int) (HEX_H * scale)) + (int) ((HEX_H / 2.0f) * scale);
        boardSize = new Dimension(width, height);
    }

    /**
     * Looks through a vector of buffered images and draws them if they're onscreen.
     */
    private synchronized void drawSprites(Graphics2D graphics2D, Collection<? extends Sprite> spriteArrayList) {
        for (Sprite sprite : spriteArrayList) {
            drawSprite(graphics2D, sprite);
        }
    }

    private synchronized void drawHexSpritesForHex(Coords coords, Graphics2D graphics2D,
          Collection<? extends HexSprite> spriteArrayList, boolean includeUnits) {
        Rectangle view = graphics2D.getClipBounds();

        for (HexSprite sprite : spriteArrayList) {
            if ((!includeUnits && sprite instanceof IsometricSprite)
                  || (gpuCapture && (hasNativeVolume(sprite) || sprite instanceof TacticalSprite))) {
                continue;
            }
            Coords spritePosition = sprite.getPosition();
            if (spritePosition == null) {
                continue;
            }
            // This can potentially be an expensive operation
            Rectangle spriteBounds = sprite.getBounds();
            if (spritePosition.equals(coords) && view.intersects(spriteBounds) && !sprite.isHidden()) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }

                sprite.drawOnto(graphics2D, spriteBounds.x, spriteBounds.y, boardPanel, false);
            }
        }
    }

    /**
     * Draws the wreck sprites for the given hex, optionally filtered by whether the wrecked entity was on a bridge.
     * Splitting the pass by bridge state lets callers draw under-bridge wrecks beneath the bridge orthograph and
     * on-bridge wrecks above it.
     *
     * @param coords          The Coordinates of the hex that the sprites should be drawn for.
     * @param graphics2D      The Graphics object for this board.
     * @param spriteArrayList The complete list of all IsometricWreckSprite on the board.
     * @param onBridge        When true, only draw wrecks of entities on a bridge; when false, only draw the rest.
     */
    private synchronized void drawIsometricWreckSpritesForHex(Coords coords, Graphics2D graphics2D,
          ArrayList<IsometricWreckSprite> spriteArrayList, boolean onBridge) {
        Rectangle view = graphics2D.getClipBounds();
        for (IsometricWreckSprite sprite : spriteArrayList) {
            Coords spritePosition = sprite.getPosition();
            if (spritePosition.equals(coords) && view.intersects(sprite.getBounds()) && !sprite.isHidden()
                  && EntityWreckHelper.entityOnBridge(sprite.getEntity()) == onBridge) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(graphics2D, sprite.getBounds().x, sprite.getBounds().y, boardPanel, false);
            }
        }
    }

    /**
     * Draws a translucent sprite without any of the companion graphics, if it is in the current view. This is used only
     * when performing isometric rending. This function is used to show units (with 50% transparency) that are hidden
     * behind a hill.
     * <p>
     * TODO: Optimize this function so that it is only applied to sprites that are actually hidden. This
     *  implementation performs the second rendering for all sprites.
     */
    private void drawIsometricSprites(Graphics2D graphics2D, Collection<IsometricSprite> spriteArrayList) {
        Rectangle view = graphics2D.getClipBounds();
        for (IsometricSprite sprite : spriteArrayList) {
            // This can potentially be an expensive operation
            Rectangle spriteBounds = sprite.getBounds();
            if (view.intersects(spriteBounds) && !sprite.isHidden()) {
                if (!sprite.isReady()) {
                    sprite.prepare();
                }
                sprite.drawOnto(graphics2D, spriteBounds.x, spriteBounds.y, boardPanel, true);
            }
        }
    }

    /**
     * Draws a sprite, if it is in the current view
     */
    private void drawSprite(Graphics2D graphics2D, Sprite sprite) {
        if (gpuCapture && hasNativeVolume(sprite)) {
            return;
        }
        if (graphics2D instanceof BoardTacticalGraphics) {
            if (!sprite.isHidden() && sprite instanceof TacticalSprite tactical) {
                BoardTacticalGraphics.draw(graphics2D, tactical.playback(), tactical::drawTactical);
            }
            return;
        }
        if (gpuCapture && sprite instanceof TacticalSprite) {
            return;
        }
        Rectangle view = graphics2D.getClipBounds();

        // This can potentially be an expensive operation
        Rectangle spriteBounds = sprite.getBounds();
        if (view.intersects(spriteBounds) && !sprite.isHidden()) {
            if (!sprite.isReady()) {
                sprite.prepare();
            }
            sprite.drawOnto(graphics2D, spriteBounds.x, spriteBounds.y, boardPanel);
        }
    }

    /**
     * Checks if a deployment indicator (yellow or cyan hex border) should be drawn for the given hex and draws it.
     *
     * @param graphics2D The graphics to draw to
     * @param coords     The hex coords of the hex to check
     */
    private void drawDeployment(Graphics2D graphics2D, Coords coords) {
        if (gpuCapture && !(graphics2D instanceof BoardTacticalGraphics)) {
            return;
        }
        Board board = game.getBoard(boardId);
        if (en_Deployer == null || !board.isLegalDeployment(coords, en_Deployer)) {
            return;
        }
        boolean isAirDeployGround = en_Deployer.getMovementMode().isHover() || en_Deployer.getMovementMode().isVTOL();
        boolean isWiGE = en_Deployer.getMovementMode().isWiGE();
        boolean boardProhibited = en_Deployer.isBoardProhibited(board);

        if (en_Deployer.isAero()) {
            if (en_Deployer.getAltitude() > 0) {
                // Flying Aeros are always above it all
                if (!en_Deployer.isLocationProhibited(coords, boardId, board.getMaxElevation()) && !boardProhibited) {
                    drawHexBorder(graphics2D, getHexLocation(coords), Color.yellow);
                }
            } else if (en_Deployer.getAltitude() == 0) {
                // Show prospective Altitude 1+ hexes
                if (!en_Deployer.isLocationProhibited(coords, boardId, 1) && !boardProhibited) {
                    drawHexBorder(graphics2D, getHexLocation(coords), Color.cyan);
                }
            }
        } else if (isAirDeployGround || isWiGE) {
            // Draw hexes that are legal at a higher deployment elevation
            Hex hex = board.getHex(coords);
            // Default to Elevation 1 if ceiling + 1 <= 0.
            int maxHeight = (isWiGE) ? 1 : (hex != null) ? Math.max(hex.ceiling() + 1, 1) : 1;
            if (!en_Deployer.isLocationProhibited(coords, boardId, maxHeight) && !boardProhibited) {
                drawHexBorder(graphics2D, getHexLocation(coords), Color.cyan);
            }
        } else if (en_Deployer instanceof AbstractBuildingEntity) {
            var deploymentHelper = new AllowedDeploymentHelper(en_Deployer, coords, board, board.getHex(coords), game);
            FacingOption facingOption = deploymentHelper.findAllowedFacings(0);
            if (facingOption != null && facingOption.hasValidFacings()) {
                // Draw hexes that're legal if we rotate
                if (!boardProhibited) {
                    drawHexBorder(graphics2D, getHexLocation(coords), Color.yellow);
                }
            }
        }

        if (!en_Deployer.isLocationProhibited(BoardLocation.of(coords, boardId)) && !boardProhibited) {
            // Draw hexes that are legal at lowest deployment elevation
            drawHexBorder(graphics2D, getHexLocation(coords), Color.yellow);
        }

        if (!en_Deployer.isLocationProhibited(BoardLocation.of(coords, boardId))
              && en_Deployer.isLocationDeadly(coords)) {
            drawHexBorder(graphics2D, getHexLocation(coords), GUIP.getWarningColor());
        }
    }

    /** Draws the deploying entity's legal deployment borders for every hex overlapping the clip. */
    private void drawDeploymentBorders(Graphics2D graphics2D) {
        Rectangle view = graphics2D.getClipBounds();
        int firstX = (view.x / (int) (HEX_WC * scale)) - 1;
        int firstY = (view.y / (int) (HEX_H * scale)) - 1;
        int lastX = firstX + (view.width / (int) (HEX_WC * scale)) + 3;
        int lastY = firstY + (view.height / (int) (HEX_H * scale)) + 3;
        for (int x = firstX; x <= lastX; x++) {
            for (int y = firstY; y <= lastY; y++) {
                Coords coords = new Coords(x, y);
                if (getBoard().getHex(coords) != null) {
                    drawDeployment(graphics2D, coords);
                }
            }
        }
    }

    /**
     * Draw indicators for the deployment zones of all players
     */
    private void drawAllDeployment(Graphics2D graphics2D) {
        if (gpuCapture && !(graphics2D instanceof BoardTacticalGraphics)) {
            return;
        }
        Rectangle view = graphics2D.getClipBounds();
        // only update visible hexes
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;

        List<Player> players = game.getPlayersList();
        final var gameOptions = game.getOptions();

        if (gameOptions.booleanOption(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0)) {
            players = players.stream()
                  .filter(player -> player.isBot() || player.getId() == 0)
                  .collect(Collectors.toList());
        }

        if (game.getPhase().isLounge()
              && !localPlayer.isGameMaster()
              && (gameOptions.booleanOption(OptionsConstants.BASE_BLIND_DROP) || gameOptions.booleanOption(
              OptionsConstants.BASE_REAL_BLIND_DROP))) {
            players = players.stream().filter(player -> !player.isEnemyOf(localPlayer)).collect(Collectors.toList());
        }

        Board board = game.getBoard(boardId);
        // loop through the hexes
        for (int i = 0;
              i < drawHeight;
              i++) {
            for (int j = 0;
                  j < drawWidth;
                  j++) {
                Coords coords = new Coords(j + drawX, i + drawY);
                int pCount = 0;
                int bThickness = 1 + 10 / game.getNoOfPlayers();
                // loop through all players
                for (Player player : players) {
                    if (board.isLegalDeployment(coords, player)) {
                        Color playerColor = player.getColour().getColour();
                        drawHexBorder(graphics2D,
                              getHexLocation(coords),
                              playerColor,
                              (bThickness + 2) * pCount,
                              bThickness);
                        pCount++;
                    }
                }
            }
        }
    }

    /**
     * Draw a layer of a solid color (alpha possible) on the hex at {@link Point} no padding by default
     */
    void drawHexLayer(Graphics2D graphics2D, Color color) {
        drawHexLayer(graphics2D, color, false, false);
    }

    /**
     * Draw a layer of a solid color (alpha possible) on the hex at {@link Point} with some padding around the border
     */
    void drawHexLayer(Graphics2D graphics2D, Color color, boolean outOfFOV, boolean reverseStripes) {
        graphics2D.setColor(color);

        // create stripe effect for FOV darkening but not for colored weapon ranges
        int fogStripes = GUIP.getFovStripes();

        if (outOfFOV && fogStripes > 0) {
            // totally transparent here hurts the eyes
            GradientPaint gradientPaint = getGradientPaint(color, (float) fogStripes, reverseStripes);
            graphics2D.setPaint(gradientPaint);
        }

        Composite svComposite = graphics2D.getComposite();
        graphics2D.setComposite(AlphaComposite.SrcAtop);
        graphics2D.fillRect(0, 0, hex_size.width, hex_size.height);
        graphics2D.setComposite(svComposite);
    }

    private static GradientPaint getGradientPaint(Color startingColor, float fogStripes, boolean reversed) {
        Color endingColor = new Color(startingColor.getRed() / 2,
              startingColor.getGreen() / 2,
              startingColor.getBlue() / 2,
              startingColor.getAlpha() / 2);

        // the numbers make the lines align across hexes
        // reversed changes stripe direction from bottom-left/top-right to top-left/bottom-right
        if (reversed) {
            return new GradientPaint(104.0f / fogStripes,
                  0.0f,
                  startingColor,
                  42.0f / fogStripes,
                  106.0f / fogStripes,
                  endingColor,
                  true);
        } else {
            return new GradientPaint(42.0f / fogStripes,
                  0.0f,
                  startingColor,
                  104.0f / fogStripes,
                  106.0f / fogStripes,
                  endingColor,
                  true);
        }
    }

    public void drawHexBorder(Graphics2D graphics2D, Color color, double padding, double lineWidth) {
        drawHexBorder(graphics2D, new Point(0, 0), color, padding, lineWidth);
    }

    public void drawHexBorder(Graphics2D graphics2D, Point point, Color col, double pad, double lineWidth) {
        graphics2D.setColor(col);
        graphics2D.fill(AffineTransform.getTranslateInstance(point.x, point.y)
              .createTransformedShape(AffineTransform.getScaleInstance(scale, scale)
                    .createTransformedShape(HexDrawUtilities.getHexFullBorderArea(lineWidth, pad))));
    }

    /**
     * Draw an outline around the hex at {@link Point} no padding and a width of 1
     */
    private void drawHexBorder(Graphics2D graphics2D, Point point, Color color) {
        drawHexBorder(graphics2D, point, color, 0, 1);
    }

    /**
     * returns the weapon selected in the mek display, or null if none selected, or it is not artillery or null if the
     * selected entity is not owned
     */
    public Mounted<?> getSelectedArtilleryWeapon() {
        // We don't want to display artillery auto-hit/adjusted fire hexes during the ArtyAutoHitHexes phase. These
        // could be displayed if the player uses the /reset command in some situations
        if (game.getPhase().isSetArtilleryAutoHitHexes()) {
            return null;
        }

        Mounted<?> selectedWeapon = selectedWeapon();

        if ((getSelectedEntity() == null) || (selectedWeapon == null)) {
            return null;
        }

        if (!getSelectedEntity().getOwner().equals(getLocalPlayer())) {
            return null; // Not my business to see this
        }

        if (getSelectedEntity().getEquipmentNum(selectedWeapon) == -1) {
            return null; // inconsistent state - weapon not on entity
        }

        if (!((selectedWeapon.getType() instanceof WeaponType) && selectedWeapon.getType()
              .hasFlag(WeaponType.F_ARTILLERY))) {
            return null; // not artillery
        }

        // otherwise, a weapon is selected, and it is artillery
        return selectedWeapon;
    }

    @Nullable
    private Mounted<?> selectedWeapon() {
        return (clientgui != null) ? clientgui.getDisplayedWeapon().orElse(null) : null;
    }

    /**
     * Draws hex borders for highlighted entity hexes (Nova CEWS network dialog).
     *
     * @param graphics The graphics object to draw on
     */
    private void drawEntityHexHighlights(Graphics2D graphics) {
        if (gpuCapture && !(graphics instanceof BoardTacticalGraphics)) {
            return;
        }
        graphics.setColor(UIUtil.uiGreen());
        graphics.setStroke(new BasicStroke((float) (2.0 * scale)));

        for (Coords hex : highlightedEntityHexes) {
            Point hexPos = getHexLocation(hex);
            Shape hexBorder = HexDrawUtilities.getHexFullBorderLine(0);
            Shape scaled = AffineTransform
                  .getScaleInstance(scale, scale)
                  .createTransformedShape(hexBorder);
            Shape translated = AffineTransform
                  .getTranslateInstance(hexPos.x, hexPos.y)
                  .createTransformedShape(scaled);
            graphics.draw(translated);
        }
    }

    /** Hazard-stripe yellow for the bold demolition charge selection outline. */
    private static final Color DEMO_CHARGE_HAZARD_COLOR = new Color(255, 213, 0);

    /**
     * Draws the outline around demolition charge hexes selected in the Detonate Charges dialog. With the hazard-outline
     * client setting on (default), each hex gets a bold yellow/black hazard-stripe border (a thick black base with a
     * yellow dashed line over it). With the setting off, it falls back to the same plain border the generic entity
     * highlight uses.
     *
     * @param graphics The graphics object to draw on
     */
    private void drawDemolitionChargeHighlights(Graphics2D graphics) {
        if (gpuCapture && !(graphics instanceof BoardTacticalGraphics)) {
            return;
        }
        if (demolitionChargeHighlightHexes.isEmpty()) {
            return;
        }
        boolean hazard = GUIP.getDemolitionChargeHazardOutline();
        Stroke oldStroke = graphics.getStroke();
        for (Coords hex : demolitionChargeHighlightHexes) {
            Point hexPos = getHexLocation(hex);
            Shape hexBorder = HexDrawUtilities.getHexFullBorderLine(0);
            Shape scaled = AffineTransform
                  .getScaleInstance(scale, scale)
                  .createTransformedShape(hexBorder);
            Shape border = AffineTransform
                  .getTranslateInstance(hexPos.x, hexPos.y)
                  .createTransformedShape(scaled);
            if (hazard) {
                float boldWidth = (float) Math.max(3.0, 4.0 * scale);
                // Black base pass, then a yellow dashed pass on top so the gaps show black underneath - a hazard stripe.
                graphics.setColor(Color.BLACK);
                graphics.setStroke(new BasicStroke(boldWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                graphics.draw(border);
                float dash = (float) Math.max(6.0, 10.0 * scale);
                graphics.setColor(DEMO_CHARGE_HAZARD_COLOR);
                graphics.setStroke(new BasicStroke(boldWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                      10.0f, new float[] { dash, dash }, 0.0f));
                graphics.draw(border);
            } else {
                graphics.setColor(UIUtil.uiGreen());
                graphics.setStroke(new BasicStroke((float) (2.0 * scale)));
                graphics.draw(border);
            }
        }
        graphics.setStroke(oldStroke);
    }

    /**
     * Draw the orbital bombardment attacks on the board view
     *
     * @param boardGraphics The graphics object to draw on
     */
    private void drawOrbitalBombardmentHexes(Graphics2D boardGraphics) {
        Image orbitalBombardmentImage = tileManager.getOrbitalBombardmentImage();
        Rectangle view = boardGraphics.getClipBounds();

        // Compute the origin of the viewing area
        int drawX = (view.x / (int) (HEX_WC * scale)) - 1;
        int drawY = (view.y / (int) (HEX_H * scale)) - 1;

        // Compute size of viewing area
        int drawWidth = (view.width / (int) (HEX_WC * scale)) + 3;
        int drawHeight = (view.height / (int) (HEX_H * scale)) + 3;

        // Draw incoming artillery sprites - requires server to update client's view of game
        for (Enumeration<OrbitalBombardment> attacks = game.getOrbitalBombardmentAttacks();
              attacks.hasMoreElements(); ) {
            final OrbitalBombardment orbitalBombardment = attacks.nextElement();
            final Coords coords = new Coords(orbitalBombardment.getX(), orbitalBombardment.getY());
            // Is the Coord within the viewing area?
            boolean insideViewArea = ((coords.getX() >= drawX)
                  && (coords.getX() <= (drawX + drawWidth))
                  && (coords.getY() >= drawY)
                  && (coords.getY() <= (drawY + drawHeight)));
            if (insideViewArea) {
                if (gpuCapture) {
                    if (!(boardGraphics instanceof BoardTacticalGraphics)) {
                        continue;
                    }
                    // Keep the blast footprint on the ground; only its centre symbol floats.
                    for (Coords affected : coords.allAtDistanceOrLess(orbitalBombardment.getRadius())) {
                        drawHexBorder(boardGraphics, getHexLocation(affected),
                              new Color(BoardMarker.Kind.ORBITAL_INCOMING.rgb()));
                    }
                    continue;
                }
                Point hexLocation = getHexLocation(coords);
                boardGraphics.drawImage(getScaledImage(orbitalBombardmentImage, true),
                      hexLocation.x,
                      hexLocation.y,
                      boardPanel);
                for (Coords atDistanceCoords : coords.allAtDistanceOrLess(orbitalBombardment.getRadius())) {
                    Point location = getHexLocation(atDistanceCoords);
                    boardGraphics.drawImage(getScaledImage(orbitalBombardmentImage, true),
                          location.x,
                          location.y,
                          boardPanel);
                }
            }
        }
    }

    /**
     * Display artillery modifier in retargeted hexes
     */
    private void drawArtilleryHexes(Graphics2D graphics2D) {
        if (gpuCapture) {
            return;
        }
        Rectangle area = markerHexArea(graphics2D);
        for (BoardMarker marker : artilleryMarkers()) {
            if (!area.contains(marker.coords().getX(), marker.coords().getY())) {
                continue;
            }
            int icon = switch (marker.kind()) {
                case ARTILLERY_AUTO_HIT -> TilesetManager.ARTILLERY_AUTO_HIT;
                case ARTILLERY_ADJUSTED -> TilesetManager.ARTILLERY_ADJUSTED;
                default -> TilesetManager.ARTILLERY_INCOMING;
            };
            Point location = getHexLocation(marker.coords());
            graphics2D.drawImage(getScaledImage(tileManager.getArtilleryTarget(icon), true),
                  location.x, location.y, boardPanel);
        }
    }

    /** The same owner-visible attacks and selected-weapon modifiers feed both views. */
    private List<BoardMarker> artilleryMarkers() {
        List<BoardMarker> result = new ArrayList<>();
        for (Enumeration<ArtilleryAttackAction> attacks = game.getArtilleryAttacks(); attacks.hasMoreElements();) {
            Targetable target = attacks.nextElement().getTarget(game);
            if (isOnThisBord(target)) {
                result.add(boardMarker(BoardMarker.Kind.ARTILLERY_INCOMING, target.getPosition(), ""));
            }
        }
        Mounted<?> weapon = getSelectedArtilleryWeapon();
        if (weapon != null) {
            for (ArtilleryModifier modifier : Objects.requireNonNull(getSelectedEntity()).aTracker.getWeaponModifiers(weapon)) {
                boolean automatic = modifier.getModifier() == TargetRoll.AUTOMATIC_SUCCESS;
                result.add(boardMarker(automatic ? BoardMarker.Kind.ARTILLERY_AUTO_HIT : BoardMarker.Kind.ARTILLERY_ADJUSTED,
                      modifier.getCoords(), automatic ? "" : Integer.toString(modifier.getModifier())));
            }
        }
        return result;
    }

    private Rectangle markerHexArea(Graphics2D graphics) {
        Rectangle clip = graphics.getClipBounds();
        return new Rectangle(clip.x / (int) (HEX_WC * scale) - 1, clip.y / (int) (HEX_H * scale) - 1,
              clip.width / (int) (HEX_WC * scale) + 4, clip.height / (int) (HEX_H * scale) + 4);
    }

    /**
     * Writes "MINEFIELD" in minefield hexes...
     */
    private void drawMinefields(Graphics2D graphics2D) {
        if (gpuCapture) {
            return;
        }
        Rectangle area = markerHexArea(graphics2D);
        for (BoardMarker marker : minefieldMarkers()) {
            Coords coords = marker.coords();
            if (!area.contains(coords.getX(), coords.getY())) {
                continue;
            }
            Point hexLocation = getHexLocation(coords);
            graphics2D.drawImage(getScaledImage(tileManager.getMinefieldSign(), true),
                  hexLocation.x, hexLocation.y + (int) (10 * scale), boardPanel);
            graphics2D.setColor(Color.black);
            boolean vibrabomb = game.getNbrMinefields(coords) == 1
                  && game.getMinefields(coords).getFirst().getType() == Minefield.TYPE_VIBRABOMB;
            int lineY = vibrabomb ? 22 : 31;
            for (String line : marker.label().split("\\n")) {
                drawCenteredString(line, hexLocation.x, hexLocation.y + (int) (lineY * scale), font_minefield, graphics2D);
                lineY += 9;
            }
        }
    }

    private List<BoardMarker> minefieldMarkers() {
        List<BoardMarker> result = new ArrayList<>();
        for (Enumeration<Coords> mined = game.getMinedCoords(); mined.hasMoreElements();) {
            Coords coords = mined.nextElement();
            if (!getBoard().contains(coords)) {
                continue;
            }
            String label = "";
            if (game.getNbrMinefields(coords) > 1) {
                label = Messages.getString("BoardView1.Multiple");
            } else if (game.getNbrMinefields(coords) == 1) {
                Minefield minefield = game.getMinefields(coords).getFirst();
                label = switch (minefield.getType()) {
                    case Minefield.TYPE_CONVENTIONAL -> Messages.getString("BoardView1.Conventional") + minefield.getDensity() + ")";
                    case Minefield.TYPE_INFERNO -> Messages.getString("BoardView1.Inferno") + minefield.getDensity() + ")";
                    case Minefield.TYPE_ACTIVE -> Messages.getString("BoardView1.Active") + minefield.getDensity() + ")";
                    case Minefield.TYPE_COMMAND_DETONATED -> Messages.getString("BoardView1.Command-") + "\n"
                          + Messages.getString("BoardView1.detonated") + minefield.getDensity() + ")";
                    case Minefield.TYPE_VIBRABOMB -> Messages.getString("BoardView1.Vibrabomb")
                          + (localPlayer != null && minefield.getPlayerId() == localPlayer.getId()
                                ? "\n(" + minefield.getSetting() + ")" : "");
                    case Minefield.TYPE_TRIPWIRE -> Messages.getString("BoardView1.Tripwire");
                    case Minefield.TYPE_PITFALL -> Messages.getString("BoardView1.Pitfall");
                    default -> "";
                };
            }
            result.add(boardMarker(BoardMarker.Kind.MINEFIELD, coords, label));
        }
        return result;
    }

    /**
     * Draws an indicator on every hex holding a demolition charge set by the local player, so the player can keep track
     * of armed charges until they are touched off (TO:AUE p.152). Charges are only visible to their owner.
     *
     * @param graphics2D the graphics context to draw on
     */
    private void drawDemolitionCharges(Graphics2D graphics2D) {
        if (gpuCapture) {
            return;
        }
        Rectangle area = markerHexArea(graphics2D);
        for (BoardMarker marker : demolitionMarkers()) {
            if (area.contains(marker.coords().getX(), marker.coords().getY())) {
                drawDemolitionChargeLabel(graphics2D, getHexLocation(marker.coords()), marker.label());
            }
        }
    }

    private List<BoardMarker> demolitionMarkers() {
        List<BoardMarker> result = new ArrayList<>();
        if (localPlayer != null) {
            for (IBuilding building : getBoard().getBuildingsVector()) {
                for (DemolitionCharge charge : building.getDemolitionCharges()) {
                    if (charge.playerId == localPlayer.getId() && getBoard().contains(charge.pos)) {
                        result.add(boardMarker(BoardMarker.Kind.DEMOLITION_CHARGE, charge.pos,
                              GUIP.getDemolitionChargeColor().getRGB(),
                              Messages.getString("BoardView1.demoChargeSet", charge.damage)));
                    }
                }
            }
        }
        return result;
    }

    private static final Color DEMO_CHARGE_OUTLINE_COLOR = new Color(0, 0, 0, 200);

    /**
     * Draws the armed-charge marker: a crosshair centered in the hex to clearly mark the rigged hex, with the damage
     * label on a dark backing pill below it so it stays readable over any terrain and is clearly distinct from unit
     * status tags.
     *
     * @param graphics2D  the graphics context to draw on
     * @param hexLocation the pixel location of the hex
     * @param label       the label text
     */
    private void drawDemolitionChargeLabel(Graphics2D graphics2D, Point hexLocation, String label) {
        // The marker color is a client setting so players can adjust it for color vision deficiencies
        // and for visibility against the terrain colors of the current map
        Color demolitionChargeColor = GUIP.getDemolitionChargeColor();
        int centerX = hexLocation.x + (hex_size.width / 2);
        int centerY = hexLocation.y + (hex_size.height / 2);
        int radius = Math.max(3, (int) (7 * scale));
        int tickLength = Math.max(2, (int) (4 * scale));

        Stroke oldStroke = graphics2D.getStroke();
        // Outline pass (thicker, dark) below the colored pass keeps the crosshair visible on any terrain
        graphics2D.setStroke(new BasicStroke(Math.max(2.5f, 3f * scale)));
        graphics2D.setColor(DEMO_CHARGE_OUTLINE_COLOR);
        drawCrosshair(graphics2D, centerX, centerY, radius, tickLength);
        graphics2D.setStroke(new BasicStroke(Math.max(1f, 1.5f * scale)));
        graphics2D.setColor(demolitionChargeColor);
        drawCrosshair(graphics2D, centerX, centerY, radius, tickLength);
        graphics2D.setStroke(oldStroke);

        // Damage label on a dark backing pill below the crosshair
        FontMetrics metrics = boardPanel.getFontMetrics(font_minefield);
        int stringWidth = metrics.stringWidth(label);
        int labelX = centerX - (stringWidth / 2);
        int labelY = centerY + radius + tickLength + metrics.getAscent() + 2;

        graphics2D.setColor(new Color(0, 0, 0, 160));
        graphics2D.fillRoundRect(labelX - 4, labelY - metrics.getAscent() - 1,
              stringWidth + 8, metrics.getAscent() + metrics.getDescent() + 2, 8, 8);

        graphics2D.setFont(font_minefield);
        graphics2D.setColor(demolitionChargeColor);
        graphics2D.drawString(label, labelX, labelY);
    }

    /**
     * Draws a crosshair: a circle with four tick lines extending outward at the cardinal points and a center dot.
     *
     * @param graphics2D the graphics context to draw on
     * @param centerX    the x pixel coordinate of the crosshair center
     * @param centerY    the y pixel coordinate of the crosshair center
     * @param radius     the circle radius in pixels
     * @param tickLength the length of the tick lines in pixels
     */
    private void drawCrosshair(Graphics2D graphics2D, int centerX, int centerY, int radius, int tickLength) {
        graphics2D.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        graphics2D.drawLine(centerX, centerY - radius - tickLength, centerX, centerY - radius + tickLength);
        graphics2D.drawLine(centerX, centerY + radius - tickLength, centerX, centerY + radius + tickLength);
        graphics2D.drawLine(centerX - radius - tickLength, centerY, centerX - radius + tickLength, centerY);
        graphics2D.drawLine(centerX + radius - tickLength, centerY, centerX + radius + tickLength, centerY);
        graphics2D.fillOval(centerX - 1, centerY - 1, 3, 3);
    }

    private void drawCenteredString(String string, int x, int y, Font font, Graphics2D graphics2D) {
        FontMetrics currentMetrics = boardPanel.getFontMetrics(font);
        int stringWidth = currentMetrics.stringWidth(string);
        x += ((hex_size.width - stringWidth) / 2);
        graphics2D.setFont(font);
        graphics2D.drawString(string, x, y);
    }

    /**
     * Draws a single combined turn label for all the bot artillery heat-map markers stacked on one hex, so the text is
     * visible in screenshots and not just in the hover text. Several tubes can target the same hex with different flight
     * times, so the distinct values are merged into one label rather than drawn over each other: firing markers count
     * down the turns until impact ({@code T-2}, joined as {@code T-1/2} when they differ, {@code SPLASH} when the only
     * one is this turn), and otherwise a predicted-position hex shows the prediction's turn ({@code T<turn>}). No-op when
     * no
     * heat-map markers are given.
     *
     * @param heatMapMarkers The heat-map markers drawn on this hex
     * @param graphics2D     The hex graphics context
     * @param scale          The current board scale
     */
    private void drawHeatMapTurnLabel(Collection<SpecialHexDisplay> heatMapMarkers, Graphics2D graphics2D,
          float scale) {
        SortedSet<Integer> firingCountdowns = new TreeSet<>();
        SortedSet<Integer> predictedTurns = new TreeSet<>();
        for (SpecialHexDisplay marker : heatMapMarkers) {
            String info = marker.getInfo();
            if ((info == null) || !info.startsWith(SpecialHexDisplay.HEAT_MAP_PREFIX)) {
                continue;
            }
            // Control token is "<turn-or-countdown>:<heat>:<kind>".
            String[] fields = heatMapToken(info).split(":");
            if (fields.length == 0) {
                continue;
            }
            try {
                int value = Integer.parseInt(fields[0]);
                if ((fields.length >= 3) && SpecialHexDisplay.HEAT_MAP_KIND_FIRING.equals(fields[2])) {
                    firingCountdowns.add(value);
                } else {
                    predictedTurns.add(value);
                }
            } catch (NumberFormatException ignored) {
                // skip a marker whose turn value is not numeric
            }
        }

        String label;
        if (!firingCountdowns.isEmpty()) {
            // A firing marker's countdown wins over a predicted label; merge distinct countdowns with '/'.
            if ((firingCountdowns.size() == 1) && (firingCountdowns.first() == 0)) {
                label = Messages.getString("BoardView.artillery.splash");
            } else {
                label = Messages.getString("BoardView.artillery.firingCountdown", joinValues(firingCountdowns));
            }
        } else if (!predictedTurns.isEmpty()) {
            label = Messages.getString("BoardView.artillery.predictedTurns", joinValues(predictedTurns));
        } else {
            return;
        }

        Color previousColor = graphics2D.getColor();
        graphics2D.setColor(Color.WHITE);
        drawCenteredString(label, 0, (int) (45 * scale), font_note, graphics2D);
        graphics2D.setColor(previousColor);
    }

    /**
     * @param values The values to render
     *
     * @return The values joined with a slash, e.g. {@code 1/2}
     */
    private String joinValues(Collection<Integer> values) {
        StringBuilder joined = new StringBuilder();
        for (Integer value : values) {
            if (joined.length() > 0) {
                joined.append('/');
            }
            joined.append(value);
        }
        return joined.toString();
    }

    /**
     * The cold-to-hot diverging color ramp for predicted-position heat-map markers: navy blue (coldest, a single
     * enemy converging on the hex) through light blue, light gray, and light orange to crimson red (hottest, many
     * enemies converging).
     */
    private static final Color[] HEAT_MAP_COLOR_RAMP = {
          new Color(0, 0, 128),      // navy blue - coldest
          new Color(102, 178, 255),  // light blue
          new Color(220, 220, 220),  // light gray (neutral middle)
          new Color(255, 178, 102),  // light orange
          new Color(220, 20, 60)     // crimson red - hottest
    };

    /** The enemy count at which a predicted-position heat-map hex is drawn fully hot (crimson). */
    private static final int HEAT_MAP_MAX_HEAT_UNITS = 5;

    /** Opacity of the predicted-position heat-map color fill, so the underlying terrain still shows through. */
    private static final float HEAT_MAP_FILL_ALPHA = 0.55f;

    /**
     * Extracts the colon-separated heat-map control token (the {@code <turn>:<heat>:<kind>} part up to the first
     * space) from a heat-map marker's info text. See {@link SpecialHexDisplay#HEAT_MAP_PREFIX}.
     *
     * @param info The marker's info text
     *
     * @return The control token (without the prefix), or an empty string if there is none
     */
    private String heatMapToken(String info) {
        int prefixLength = SpecialHexDisplay.HEAT_MAP_PREFIX.length();
        int space = info.indexOf(' ', prefixLength);
        return (space > prefixLength) ? info.substring(prefixLength, space) : info.substring(prefixLength);
    }

    /**
     * @param specialHexDisplay A special hex display being drawn
     *
     * @return {@code true} if this is any bot artillery heat-map marker (predicted-position or firing)
     */
    private boolean isHeatMapMarker(SpecialHexDisplay specialHexDisplay) {
        String info = specialHexDisplay.getInfo();
        return (info != null) && info.startsWith(SpecialHexDisplay.HEAT_MAP_PREFIX);
    }

    /**
     * @param specialHexDisplay A special hex display being drawn
     *
     * @return {@code true} if this is a predicted-position heat-map marker (painted as a cold-to-hot color fill),
     *       {@code false} for a firing marker or any non-heat-map display
     */
    private boolean isPredictedHeatMapMarker(SpecialHexDisplay specialHexDisplay) {
        String info = specialHexDisplay.getInfo();
        if ((info == null) || !info.startsWith(SpecialHexDisplay.HEAT_MAP_PREFIX)) {
            return false;
        }
        String[] fields = heatMapToken(info).split(":");
        return (fields.length >= 3) && SpecialHexDisplay.HEAT_MAP_KIND_PREDICTED.equals(fields[2]);
    }

    /**
     * @param info A heat-map marker's info text
     *
     * @return The number of enemies predicted to converge on the hex (the marker's heat), or 1 if it cannot be parsed
     */
    private int heatMapHeatUnits(String info) {
        String[] fields = heatMapToken(info).split(":");
        if (fields.length >= 2) {
            try {
                return Integer.parseInt(fields[1]);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * Maps a predicted hex's heat (number of enemies converging on it) to a color on the cold-to-hot diverging ramp:
     * one enemy is navy blue (coldest) and {@link #HEAT_MAP_MAX_HEAT_UNITS} or more is crimson red (hottest), with the
     * intermediate counts interpolated through the ramp.
     *
     * @param heatUnits The number of enemies converging on the hex
     *
     * @return The color to paint the hex
     */
    private Color heatMapDivergingColor(int heatUnits) {
        float normalized = (float) (heatUnits - 1) / (HEAT_MAP_MAX_HEAT_UNITS - 1);
        normalized = Math.max(0.0f, Math.min(1.0f, normalized));
        float scaledPosition = normalized * (HEAT_MAP_COLOR_RAMP.length - 1);
        int lowerStop = (int) Math.floor(scaledPosition);
        int upperStop = Math.min(lowerStop + 1, HEAT_MAP_COLOR_RAMP.length - 1);
        float fraction = scaledPosition - lowerStop;
        Color from = HEAT_MAP_COLOR_RAMP[lowerStop];
        Color to = HEAT_MAP_COLOR_RAMP[upperStop];
        int red = Math.round(from.getRed() + (fraction * (to.getRed() - from.getRed())));
        int green = Math.round(from.getGreen() + (fraction * (to.getGreen() - from.getGreen())));
        int blue = Math.round(from.getBlue() + (fraction * (to.getBlue() - from.getBlue())));
        return new Color(red, green, blue);
    }

    /**
     * Paints a predicted-position heat-map marker as a cold-to-hot translucent fill over the whole hex (navy = one
     * enemy converging, crimson = many), so the predicted enemy concentration reads at a glance and cools as units
     * are destroyed. No-op for any other special hex display.
     *
     * @param specialHexDisplay The special hex display being drawn
     * @param graphics2D        The hex graphics context
     * @param scale             The current board scale
     */
    private void drawHeatMapPredictedHex(SpecialHexDisplay specialHexDisplay, Graphics2D graphics2D, float scale) {
        Color heatColor = heatMapDivergingColor(heatMapHeatUnits(specialHexDisplay.getInfo()));
        Color previousColor = graphics2D.getColor();
        Composite previousComposite = graphics2D.getComposite();
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, HEAT_MAP_FILL_ALPHA));
        graphics2D.setColor(heatColor);
        AffineTransform hexScale = new AffineTransform();
        hexScale.scale(scale, scale);
        graphics2D.fill(hexScale.createTransformedShape(HEX_POLY));
        graphics2D.setComposite(previousComposite);
        graphics2D.setColor(previousColor);
    }

    /** Color of the artillery drift line drawn from a targeted hex to where the round actually landed. */
    private static final Color ARTILLERY_DRIFT_LINE_COLOR = new Color(255, 191, 0);

    /**
     * Draws a thin dashed line from each visible artillery miss marker's targeted hex to the hex the round actually
     * drifted to, with an arrowhead at the landing hex, so the drift reads at a glance (in addition to the combat
     * report). Only drawn for drift markers currently shown to the local player.
     *
     * @param graphics2D The board graphics context, in board pixel space at the current scale
     */
    private void drawArtilleryDriftLines(Graphics2D graphics2D) {
        if (gpuCapture && !(graphics2D instanceof BoardTacticalGraphics)) {
            return;
        }
        if (!GUIP.getShowArtilleryDriftArrows()) {
            return;
        }
        Board board = game.getBoard(boardId);
        if (board == null) {
            return;
        }
        Map<Coords, Collection<SpecialHexDisplay>> specialHexDisplays = board.getSpecialHexDisplayTable();
        if ((specialHexDisplays == null) || specialHexDisplays.isEmpty()) {
            return;
        }
        Stroke previousStroke = graphics2D.getStroke();
        Color previousColor = graphics2D.getColor();
        float dashLength = Math.max(4.0f, hex_size.width / 14.0f);
        graphics2D.setColor(ARTILLERY_DRIFT_LINE_COLOR);
        graphics2D.setStroke(new BasicStroke(Math.max(1.0f, hex_size.width / 60.0f), BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND, 1.0f, new float[] { dashLength, dashLength }, 0.0f));
        for (Map.Entry<Coords, Collection<SpecialHexDisplay>> entry : specialHexDisplays.entrySet()) {
            for (SpecialHexDisplay specialHexDisplay : entry.getValue()) {
                Coords landingHex = specialHexDisplay.getDriftHex();
                if ((landingHex == null)
                      || !specialHexDisplay.drawNow(game.getPhase(), game.getRoundCount(), getLocalPlayer(), GUIP)) {
                    continue;
                }
                drawDriftLine(graphics2D, entry.getKey(), landingHex);
            }
        }
        graphics2D.setStroke(previousStroke);
        graphics2D.setColor(previousColor);
    }

    /**
     * Draws a single drift line, with an arrowhead at the landing hex, between the centers of two hexes.
     *
     * @param graphics2D  The board graphics context
     * @param targetedHex The hex that was targeted (line origin)
     * @param landingHex  The hex the round drifted to (arrowhead end)
     */
    private void drawDriftLine(Graphics2D graphics2D, Coords targetedHex, Coords landingHex) {
        Point targetedPoint = getHexLocation(targetedHex);
        Point landingPoint = getHexLocation(landingHex);
        if ((targetedPoint == null) || (landingPoint == null)) {
            return;
        }
        int fromX = targetedPoint.x + (hex_size.width / 2);
        int fromY = targetedPoint.y + (hex_size.height / 2);
        int toX = landingPoint.x + (hex_size.width / 2);
        int toY = landingPoint.y + (hex_size.height / 2);
        graphics2D.drawLine(fromX, fromY, toX, toY);
        double angle = Math.atan2((double) toY - fromY, (double) toX - fromX);
        int headLength = Math.max(6, hex_size.width / 6);
        double spread = Math.toRadians(28);
        int leftX = (int) Math.round(toX - (headLength * Math.cos(angle - spread)));
        int leftY = (int) Math.round(toY - (headLength * Math.sin(angle - spread)));
        int rightX = (int) Math.round(toX - (headLength * Math.cos(angle + spread)));
        int rightY = (int) Math.round(toY - (headLength * Math.sin(angle + spread)));
        graphics2D.drawLine(toX, toY, leftX, leftY);
        graphics2D.drawLine(toX, toY, rightX, rightY);
    }

    @Override
    public BufferedImage getEntireBoardImage(boolean ignoreUnits, boolean useBaseZoom) {
        // Set zoom to base, so we get a consistent board image
        int oldZoom = zoomIndex;
        if (useBaseZoom) {
            zoomIndex = BASE_ZOOM_INDEX;
            zoom();
        }

        BufferedImage entireBoard = new BufferedImage(boardSize.width, boardSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D boardGraph = (Graphics2D) entireBoard.getGraphics();
        boardGraph.setClip(0, 0, boardSize.width, boardSize.height);
        UIUtil.setHighQualityRendering(boardGraph);

        if (shadowMap == null) {
            shadowMap = shadowHelper.updateShadowMap();
        }

        // Draw hexes
        drawHexes(boardGraph, new Rectangle(boardSize), ignoreUnits);

        // If we aren't ignoring units, draw everything else
        if (!ignoreUnits) {
            drawTacticalLayers(boardGraph, true);
        }
        boardGraph.dispose();

        // Restore the zoom setting
        zoomIndex = oldZoom;
        zoom();

        return entireBoard;
    }

    private void drawHexes(Graphics2D graphics2D, Rectangle view) {
        drawHexes(graphics2D, view, false);
    }

    /**
     * Redraws all hexes in the specified rectangle
     */
    private void drawHexes(Graphics2D graphics2D, Rectangle view, boolean saveBoardImage) {
        drawHexes(graphics2D, view, saveBoardImage, true);
    }

    private void drawHexes(Graphics2D graphics2D, Rectangle view, boolean saveBoardImage,
          boolean includeUnits) {
        // only update visible hexes
        double scaledX = (int) (HEX_WC * scale);
        double scaledY = (int) (HEX_H * scale);

        int drawX = (int) (view.x / scaledX) - 1;
        int drawY = (int) (view.y / scaledY) - 1;

        int drawWidth = (int) (view.width / scaledX) + 3;
        int drawHeight = (int) (view.height / scaledY) + 3;

        Board board = game.getBoard(boardId);
        for (int y = 0; y < drawHeight; y++) {
            // Half of each row is one-half hex farther back (above) the other; draw those first
            for (int s = 0; s <= 1; s++) {
                for (int x = s; x < drawWidth + s + 1; x = x + 2) {
                    // For s == 0 the x coordinate MUST be an even number to get correct occlusion; drawX may be
                    // any int though
                    Coords coords = new Coords(x + drawX / 2 * 2, y + drawY);
                    Hex hex = board.getHex(coords);
                    if (hex != null) {
                        drawHex(coords, graphics2D, saveBoardImage);
                        drawOrthograph(coords, graphics2D);
                        // Under-bridge / no-bridge wrecks: drawn before the iso entity sprite (so a unit on the
                        // wreck paints on top) and before the second drawOrthograph (so a bridge deck paints over).
                        if (!saveBoardImage && GUIP.getShowWrecks()) {
                            drawIsometricWreckSpritesForHex(coords, graphics2D, isometricWreckSprites, false);
                        }
                        drawHexSpritesForHex(coords, graphics2D, behindTerrainHexSprites, includeUnits);
                        drawDeployment(graphics2D, coords);
                        drawOrthograph(coords, graphics2D);
                        // On-bridge wrecks: drawn after the bridge orthograph so they sit on the deck.
                        if (!saveBoardImage && GUIP.getShowWrecks()) {
                            drawIsometricWreckSpritesForHex(coords, graphics2D, isometricWreckSprites, true);
                        }
                        drawHexText(coords, hex, board, graphics2D);
                    }
                }
            }
        }

        if (!saveBoardImage && includeUnits) {
            // If we are using Isometric rendering, redraw the entity sprites at 50% transparent so sprites
            // hidden behind hills can still be seen by the user.
            drawIsometricSprites(graphics2D, isometricSprites);
        }
    }

    /** Draws base terrain independently of superposed and orthographic feature artwork. */
    private void drawBaseTerrain(Hex hex, Graphics2D graphics2D) {
        if (hex == null) {
            return;
        }
        Image baseImage = gpuCapture ? gpuTileset.getBase(hex) : tileManager.baseFor(hex);
        drawBaseTerrain(hex, graphics2D, baseImage);
    }

    private void drawBaseTerrain(Hex hex, Graphics2D graphics2D, Image baseImage) {
        Image scaledImage = getScaledImage(baseImage, true);

        // check if this is a standard tile image 84x72 or something different
        boolean standardTile = (baseImage.getHeight(null) == HEX_H) && (baseImage.getWidth(null) == HEX_W);
        // do not make larger than hex images even when the input image is big
        int origImgWidth = scaledImage.getWidth(null); // save for later, needed for large tiles
        int origImgHeight = scaledImage.getHeight(null);

        if (standardTile) { // is the image hex-sized, 84*72?
            graphics2D.drawImage(scaledImage, 0, 0, boardPanel);
            return;
        }

        // Draw image for a texture larger than a hex
        Point p1SRC = getHexLocationLargeTile(hex.getCoords().getX(), hex.getCoords().getY());
        p1SRC.x = p1SRC.x % origImgWidth;
        p1SRC.y = p1SRC.y % origImgHeight;
        Point p2SRC = new Point((int) (p1SRC.x + HEX_W * scale), (int) (p1SRC.y + HEX_H * scale));
        Point p2DST = new Point((int) (HEX_W * scale), (int) (HEX_H * scale));

        // hex mask to limit drawing to the hex shape
        // TODO : this is not ideal yet but at least it draws without leaving gaps at any zoom
        Image hexMask = getScaledImage(tileManager.getHexMask(), true);
        graphics2D.drawImage(hexMask, 0, 0, boardPanel);
        Composite svComp = graphics2D.getComposite();
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1f));

        // paint the right slice from the big pic
        graphics2D.drawImage(scaledImage, 0, 0, p2DST.x, p2DST.y, p1SRC.x, p1SRC.y, p2SRC.x, p2SRC.y, null);

        // Handle wrapping of the image
        if (p2SRC.x > origImgWidth && p2SRC.y <= origImgHeight) {
            graphics2D.drawImage(scaledImage,
                  origImgWidth - p1SRC.x,
                  0,
                  p2DST.x,
                  p2DST.y,
                  0,
                  p1SRC.y,
                  p2SRC.x - origImgWidth,
                  p2SRC.y,
                  null); // paint additional slice on the left side
        } else if (p2SRC.x <= origImgWidth && p2SRC.y > origImgHeight) {
            graphics2D.drawImage(scaledImage,
                  0,
                  origImgHeight - p1SRC.y,
                  p2DST.x,
                  p2DST.y,
                  p1SRC.x,
                  0,
                  p2SRC.x,
                  p2SRC.y - origImgHeight,
                  null); // paint additional slice on the top
        } else if (p2SRC.x > origImgWidth) {
            graphics2D.drawImage(scaledImage,
                  origImgWidth - p1SRC.x,
                  0,
                  p2DST.x,
                  p2DST.y,
                  0,
                  p1SRC.y,
                  p2SRC.x - origImgWidth,
                  p2SRC.y,
                  null); // paint additional slice on the top
            graphics2D.drawImage(scaledImage,
                  0,
                  origImgHeight - p1SRC.y,
                  p2DST.x,
                  p2DST.y,
                  p1SRC.x,
                  0,
                  p2SRC.x,
                  p2SRC.y - origImgHeight,
                  null); // paint additional slice on the left side
            // paint additional slice on the top left side
            graphics2D.drawImage(scaledImage,
                  origImgWidth - p1SRC.x,
                  origImgHeight - p1SRC.y,
                  p2DST.x,
                  p2DST.y,
                  0,
                  0,
                  p2SRC.x - origImgWidth,
                  p2SRC.y - origImgHeight,
                  null);
        }

        graphics2D.setComposite(svComp);
    }

    /**
     * Draws a hex onto the board buffer. This assumes that drawRect is current, and does not check if the hex is
     * visible.
     */
    private void drawHex(Coords coords, Graphics boardGraph, boolean saveBoardImage) {
        if (!game.getBoard(boardId).contains(coords)) {
            return;
        }

        final Hex hex = game.getBoard(boardId).getHex(coords);
        if (hex == null) {
            return;
        }

        final Point hexLocation = getHexLocation(coords);
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        // Check the cache to see if we already have the image
        HexImageCacheEntry cacheEntry = hexImageCache.get(coords);
        if ((cacheEntry != null) && !cacheEntry.needsUpdating) {
            boardGraph.drawImage(cacheEntry.hexImage, hexLocation.x, hexLocation.y, boardPanel);
            return;
        }

        int level = hex.getLevel();

        Image scaledImage;
        boolean dontCache = false;
        int imgWidth = (int) (HEX_W * scale);
        int imgHeight = (int) (HEX_H * scale);
        if (!gpuCapture) {
            // Tactical capture does not draw the base tile or raised classic sides.
            Image baseImage = tileManager.baseFor(hex);
            scaledImage = getScaledImage(baseImage, true);
            dontCache = animatedImages.contains(baseImage.hashCode());
            imgWidth = Math.min(imgWidth, scaledImage.getWidth(null));
            imgHeight = Math.min(imgHeight, scaledImage.getHeight(null));
            int largestLevelDiff = 0;
            for (int dir : allDirections) {
                Hex adjHex = game.getBoard(boardId).getHexInDir(coords, dir);
                if (adjHex != null) {
                    largestLevelDiff = Math.max(largestLevelDiff, Math.abs(level - adjHex.getLevel()));
                }
            }
            imgHeight += (int) (verticalOffset * scale * largestLevelDiff);
        }
        // If the base image isn't ready, we should signal a repaint and stop
        if ((imgWidth < 0) || (imgHeight < 0)) {
            boardPanel.repaint();
            return;
        }

        BufferedImage hexImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics2D = (Graphics2D) (hexImage.getGraphics());
        UIUtil.setHighQualityRendering(graphics2D);

        if (!gpuCapture) {
            drawBaseTerrain(hex, graphics2D);

            // To place roads under the shadow map, some supers have to be drawn before the shadow map, otherwise the
            // supers are drawn after. Unfortunately the supers images themselves can't be checked for roads.
            List<Image> supers = tileManager.supersFor(hex);
            boolean supersUnderShadow = false;
            if (hex.containsTerrain(Terrains.ROAD)
                  || hex.containsTerrain(Terrains.WATER)
                  || hex.containsTerrain(Terrains.PAVEMENT)
                  || hex.containsTerrain(Terrains.GROUND_FLUFF)
                  || hex.containsTerrain(Terrains.ROUGH)
                  || hex.containsTerrain(Terrains.RUBBLE)
                  || hex.containsTerrain(Terrains.SNOW)) {
                supersUnderShadow = true;
                if (supers != null) {
                    for (Image image : supers) {
                        if (animatedImages.contains(image.hashCode())) {
                            dontCache = true;
                        }
                        scaledImage = getScaledImage(image, true);
                        graphics2D.drawImage(scaledImage, 0, 0, boardPanel);
                    }
                }
            }

            // Add the terrain & building shadows
            if (!gpuCapture && GUIP.getShadowMap() && (shadowMap != null)) {
                Point p1SRC = getHexLocationLargeTile(coords.getX(), coords.getY(), 1);
                Point p2SRC = new Point(p1SRC.x + HEX_W, p1SRC.y + HEX_H);
                Point p2DST = new Point(hex_size.width, hex_size.height);

                Composite svComp = graphics2D.getComposite();
                if (conditions.getLight().isDay()) {
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.55f));
                } else {
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.45f));
                }

                // paint the right slice from the big pic
                graphics2D.drawImage(shadowMap, 0, 0, p2DST.x, p2DST.y, p1SRC.x, p1SRC.y, p2SRC.x, p2SRC.y, null);
                graphics2D.setComposite(svComp);
            }

            if (!supersUnderShadow) {
                if (supers != null) {
                    for (Image image : supers) {
                        if (null != image) {
                            if (animatedImages.contains(image.hashCode())) {
                                dontCache = true;
                            }
                            scaledImage = getScaledImage(image, true);
                            graphics2D.drawImage(scaledImage, 0, 0, boardPanel);
                        }
                    }
                }
            }

            // Check for buildings and woods buried under their own shadows.
            if ((supers != null) && supersUnderShadow && (hex.containsTerrain(Terrains.BUILDING) || hex.containsTerrain(
                  Terrains.WOODS))) {
                Image lastSuper = supers.getLast();
                scaledImage = getScaledImage(lastSuper, true);
                graphics2D.drawImage(scaledImage, 0, 0, boardPanel);
            }

            // AO Hex Shadow in this hex when a higher one is adjacent
            if (!gpuCapture && GUIP.getAOHexShadows()) {
                for (int dir : allDirections) {
                    Shape ShadowShape = getElevationShadowArea(coords, dir);
                    GradientPaint gpl = getElevationShadowGP(coords, dir);
                    if ((ShadowShape != null) && (gpl != null)) {
                        graphics2D.setPaint(gpl);
                        graphics2D.fill(getElevationShadowArea(coords, dir));
                    }
                }
            }

            // Orthographic = bridges
            List<Image> orthogonalImages = tileManager.orthographicFor(hex);
            if (orthogonalImages != null) {
                for (Image image : orthogonalImages) {
                    if (animatedImages.contains(image.hashCode())) {
                        dontCache = true;
                    }
                }
            }
        }

        AffineTransform scaleTransform = new AffineTransform();
        scaleTransform.scale(scale, scale);

        int spaceInterfacePosition = BoardHelper.spaceAtmosphereInterfacePosition(game);
        // Draw in atmosphere in a high-altitude map (unless planetary conditions say its vacuum)
        if (BoardHelper.isAtmosphericRow(game, getBoard(), coords)) {
            int atmosphericRow = BoardHelper.effectiveAtmosphericRowNumber(game, getBoard(), coords);
            // First, fade out the stars
            int alphaStepStars = 120 / (spaceInterfacePosition - 1);
            graphics2D.setColor(new Color(0, 0, 0, 250 - atmosphericRow * alphaStepStars));
            graphics2D.fill(scaleTransform.createTransformedShape(HEX_POLY));
            // Add atmosphere
            int alphaStep = 160 / (spaceInterfacePosition - 1);
            graphics2D.setColor(new Color(0, 250, 250, 190 - atmosphericRow * alphaStep));
            graphics2D.fill(scaleTransform.createTransformedShape(HEX_POLY));
        }

        // Draw in the space/atmosphere interface in a high-altitude map
        if (BoardHelper.isSpaceAtmosphereInterface(game, getBoard(), coords)) {
            Polygon halfHex = new Polygon();
            halfHex.addPoint(21, 0);
            halfHex.addPoint(42, 0);
            halfHex.addPoint(42, 71);
            halfHex.addPoint(21, 71);
            halfHex.addPoint(0, 36);
            halfHex.addPoint(0, 35);
            graphics2D.setColor(new Color(0, 250, 250, 15));
            graphics2D.fill(scaleTransform.createTransformedShape(halfHex));
            Polygon line = new Polygon();
            line.addPoint(42, 0);
            line.addPoint(42, 71);
            graphics2D.setColor(new Color(130, 130, 130, 100));
            BasicStroke bs1 = new BasicStroke(2,
                  BasicStroke.CAP_BUTT,
                  BasicStroke.JOIN_ROUND,
                  1.0f,
                  new float[] { 3f, 5f },
                  0f);
            graphics2D.setStroke(bs1);
            AffineTransform oldTransform = graphics2D.getTransform();
            graphics2D.transform(scaleTransform);
            graphics2D.draw(line);
            graphics2D.setTransform(oldTransform);
        }

        // Draw in ground in a high-altitude map
        if (BoardHelper.isGroundRowHex(getBoard(), coords)) {
            // Atmosphere
            if (!game.getPlanetaryConditions().getAtmosphere().isVacuum()) {
                int atmosphericRow = BoardHelper.effectiveAtmosphericRowNumber(game, getBoard(), 1) - 1;
                // First, fade out the stars
                int alphaStepStars = 120 / (spaceInterfacePosition - 1);
                graphics2D.setColor(new Color(0, 0, 0, 250 - atmosphericRow * alphaStepStars));
                graphics2D.fill(scaleTransform.createTransformedShape(HEX_POLY));
                // Add atmosphere
                int alphaStep = 160 / (spaceInterfacePosition - 1);
                graphics2D.setColor(new Color(0, 250, 250, 190 - atmosphericRow * alphaStep));
                graphics2D.fill(scaleTransform.createTransformedShape(HEX_POLY));
            }

            Polygon leftTriangle = new Polygon();
            leftTriangle.addPoint(21, 0);
            leftTriangle.addPoint(21, 71);
            leftTriangle.addPoint(0, 36);
            leftTriangle.addPoint(0, 35);
            graphics2D.setColor(new Color(40, 80, 40));
            graphics2D.fill(scaleTransform.createTransformedShape(leftTriangle));
            graphics2D.setColor(new Color(40, 140, 40));
            graphics2D.draw(scaleTransform.createTransformedShape(HexDrawUtilities.getHexCrossLine01(4, 2)));
        }

        if (!gpuCapture && getBoard().embeddedBoardCoords().contains(coords)) {
            drawEmbeddedBoard(graphics2D);
        }
        drawElectronicWarfare(graphics2D, coords);

        // Darken the hex for nighttime, if applicable
        if (GUIP.getDarkenMapAtNight()
              && IlluminationLevel.determineIlluminationLevel(game, boardId, coords).isNone()
              && conditions.getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
            for (int x = 0;
                  x < hexImage.getWidth();
                  ++x) {
                for (int y = 0;
                      y < hexImage.getHeight();
                      ++y) {
                    hexImage.setRGB(x, y, getNightDarkenedColor(hexImage.getRGB(x, y)));
                }
            }
        }

        // Set the text color according to Preferences or Light Gray in space
        graphics2D.setColor(GUIP.getBoardTextColor());
        if (game.getBoard(boardId).isSpace()) {
            graphics2D.setColor(GUIP.getBoardSpaceTextColor());
        }

        // draw special stuff for the hex
        final Collection<SpecialHexDisplay> shdList = game.getBoard(boardId).getSpecialHexDisplay(coords);
        try {
            if (shdList != null) {
                // Several heat-map markers can stack on one hex (multiple tubes firing it, or a prediction plus a
                // shot). Draw each marker's icon/fill, but collect them so a single combined turn label is drawn (their
                // distinct values merged), rather than each marker drawing its label over the others.
                List<SpecialHexDisplay> heatMapMarkers = new ArrayList<>();
                for (SpecialHexDisplay shd : shdList) {
                    if (gpuCapture && isPredictedHeatMapMarker(shd)) {
                        continue;
                    }
                    if (gpuCapture && pointMarkerKind(shd) != null) {
                        continue;
                    }
                    if (shd.drawNow(game.getPhase(), game.getRoundCount(), getLocalPlayer(), GUIP)) {
                        // A predicted-position heat-map marker paints the hex with a cold-to-hot color (navy = one
                        // enemy converging, crimson = many) instead of an icon; the firing marker and every other
                        // display draw their icon.
                        if (isPredictedHeatMapMarker(shd)) {
                            drawHeatMapPredictedHex(shd, graphics2D, scale);
                        } else {
                            scaledImage = getScaledImage(shd.getDefaultImage(), true);
                            graphics2D.drawImage(scaledImage, 0, 0, boardPanel);
                        }
                        if (isHeatMapMarker(shd) && !gpuCapture) {
                            heatMapMarkers.add(shd);
                        }
                    }
                }
                if (!heatMapMarkers.isEmpty()) {
                    drawHeatMapTurnLabel(heatMapMarkers, graphics2D, scale);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e, "Exception, probably can't load file.");
            drawCenteredString("Loading Error", 0, (int) (50 * scale), font_note, graphics2D);
            graphics2D.dispose();
            return;
        }

        // Hex text (coordinates, level/depth/height) is drawn separately in drawHexText()
        // so that it renders on top of bridge orthographs

        // Used to make the following draw calls shorter
        int s21 = (int) (21 * scale);
        int s71 = (int) (71 * scale);
        int s35 = (int) (35 * scale);
        int s36 = (int) (36 * scale);
        int s62 = (int) (62 * scale);
        int s83 = (int) (83 * scale);

        Point p1 = new Point(s62, 0);
        Point p2 = new Point(s21, 0);
        Point p3 = new Point(s83, s35);
        Point p4 = new Point(s83, s36);
        Point p5 = new Point(s62, s71);
        Point p6 = new Point(s21, s71);
        Point p7 = new Point(0, s36);
        Point p8 = new Point(0, s35);

        if (!gpuCapture) {
            graphics2D.setColor(Color.black);
            graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // draw elevation borders
            if (drawElevationLine(coords, 0)) {
                drawIsometricElevation(coords, Color.GRAY, p1, p2, 0, graphics2D);
                if (GUIP.getLevelHighlight()) {
                    graphics2D.drawLine(s21, 0, s62, 0);
                }
            }

            if (drawElevationLine(coords, 1)) {
                drawIsometricElevation(coords, Color.DARK_GRAY, p3, p1, 1, graphics2D);
                if (GUIP.getLevelHighlight()) {
                    graphics2D.drawLine(s62, 0, s83, s35);
                }
            }

            if (drawElevationLine(coords, 2)) {
                drawIsometricElevation(coords, Color.LIGHT_GRAY, p4, p5, 2, graphics2D);
                if (GUIP.getLevelHighlight()) {
                    graphics2D.drawLine(s83, s36, s62, s71);
                }
            }

            if (drawElevationLine(coords, 3)) {
                drawIsometricElevation(coords, Color.GRAY, p6, p5, 3, graphics2D);
                if (GUIP.getLevelHighlight()) {
                    graphics2D.drawLine(s62, s71, s21, s71);
                }
            }

            if (drawElevationLine(coords, 4)) {
                drawIsometricElevation(coords, Color.DARK_GRAY, p7, p6, 4, graphics2D);
                if (GUIP.getLevelHighlight()) {
                    graphics2D.drawLine(s21, s71, 0, s36);
                }
            }

            if (drawElevationLine(coords, 5)) {
                drawIsometricElevation(coords, Color.LIGHT_GRAY, p8, p2, 5, graphics2D);
                if (GUIP.getLevelHighlight()) {
                    graphics2D.drawLine(0, s35, s21, 0);
                }

            }
        }

        // When the board image is saved, it shouldn't be spoiled by drawing LOS effects
        boolean hasLoS = gpuCapture || saveBoardImage || fovHighlightingAndDarkening.draw(graphics2D, coords);

        if (!gpuCapture) {
            drawMapSheetBorders(graphics2D, coords);
        }

        if (!hasLoS && GUIP.getFovGrayscale()) {
            // rework the pixels to grayscale
            for (int x = 0;
                  x < hexImage.getWidth();
                  x++) {
                for (int y = 0;
                      y < hexImage.getHeight();
                      y++) {
                    int rgb = hexImage.getRGB(x, y);
                    int rd = (rgb >> 16) & 0xFF;
                    int gr = (rgb >> 8) & 0xFF;
                    int bl = (rgb & 0xFF);
                    int al = (rgb >> 24);

                    int grayLevel = (rd + gr + bl) / 3;
                    int gray = (al << 24) + (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                    hexImage.setRGB(x, y, gray);
                }
            }
        }

        for (var plugin : hexDrawPlugins) {
            plugin.draw(graphics2D, hex, game, coords, this);
        }
        graphics2D.dispose();

        cacheEntry = new HexImageCacheEntry(hexImage);
        if (!dontCache) {
            hexImageCache.put(coords, cacheEntry);
        }
        boardGraph.drawImage(cacheEntry.hexImage, hexLocation.x, hexLocation.y, boardPanel);
    }

    /**
     * Draws an orthographic hex onto the board buffer. This assumes that drawRect is current, and does not check if the
     * hex is visible.
     */
    private void drawOrthograph(Coords coords, Graphics boardGraph) {
        if (!game.getBoard(boardId).contains(coords)) {
            return;
        }

        final Hex oHex = game.getBoard(boardId).getHex(coords);
        final Point oHexLoc = getHexLocation(coords);
        // Adjust the draw height for bridges according to their elevation
        int elevOffset = oHex.terrainLevel(Terrains.BRIDGE_ELEV);

        int orthogonalX = oHexLoc.x;
        int orthogonalY = oHexLoc.y - (int) (verticalOffset * scale * elevOffset);
        if (tileManager.orthographicFor(oHex) != null) {
            for (Image image : tileManager.orthographicFor(oHex)) {
                BufferedImage scaledImage = ImageUtil.createAcceleratedImage(getScaledImage(image, true));

                // Darken the hex for nighttime, if applicable
                PlanetaryConditions conditions = game.getPlanetaryConditions();
                if (GUIP.getDarkenMapAtNight() && IlluminationLevel.determineIlluminationLevel(game, boardId, coords)
                      .isNone() && conditions.getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
                    for (int x = 0;
                          x < Objects.requireNonNull(scaledImage).getWidth(null);
                          ++x) {
                        for (int y = 0;
                              y < scaledImage.getHeight();
                              ++y) {
                            scaledImage.setRGB(x, y, getNightDarkenedColor(scaledImage.getRGB(x, y)));
                        }
                    }
                }

                // draw orthogonal
                boardGraph.drawImage(scaledImage, orthogonalX, orthogonalY, boardPanel);
            }
        }
    }

    /**
     * Draws hex text overlays (coordinates, level, depth, height, foliage, invalid hex info) directly to the board
     * graphics. This is called after drawOrthograph so that text renders on top of bridge images.
     */
    private void drawHexText(Coords coords, Hex hex, Board board, Graphics2D boardGraph) {
        final Point hexLocation = getHexLocation(coords);
        int hexX = hexLocation.x;
        int hexY = hexLocation.y;
        if (!gpuCapture) {
            for (HexText label : hexText(coords, hex, board)) {
                boardGraph.setColor(new Color(label.argb(), true));
                // A label hanging from the tile's top edge is anchored by the top of its glyphs, so its baseline
                // sits one ascent below the published offset.
                int offset = label.baseline()
                      + (label.fromTop() ? boardPanel.getFontMetrics(label.font()).getAscent() : 0);
                drawCenteredString(label.text(), hexX, hexY + offset, label.font(), boardGraph);
            }
        }

        if (displayInvalidHexInfo && !hex.isValid(null)) {
            Point hexCenter = new Point(hexX + (int) (HEX_W / 2.0f * scale), hexY + (int) (HEX_H / 2.0f * scale));
            invalidString.at(hexCenter).fontSize(14.0f * scale).outline(Color.WHITE, scale / 2).draw(boardGraph);
        }
    }

    /**
     * One hex text label: its text, color, and font, plus the offset it keeps from the tile's top edge in tile
     * artwork pixels. The offset carries the label's baseline, except for a label hanging from the tile's top edge
     * ({@code fromTop}), where the glyphs rise from the offset instead: those are anchored by their top, so their
     * baseline cannot be put at the offset without pushing them into the tile's top border.
     */
    public record HexText(String text, int baseline, Font font, int argb, boolean fromTop, int elevation) { }

    private List<HexText> hexText(Coords coords, Hex hex, Board board) {
        List<HexText> labels = new ArrayList<>();
        Color color = board.isSpace() ? GUIP.getBoardSpaceTextColor() : GUIP.getBoardTextColor();
        if (GUIP.getCoordsEnabled() && scale >= 0.5) {
            labels.add(new HexText(coords.getBoardNum(), (int) (HEX_TEXT_MARGIN * scale), font_hexNumber,
                  color.getRGB(), true, 0));
        }
        if (scale > 0.5f) {
            int level = hex.getLevel();
            int depth = hex.depth(false);
            Terrain basement = hex.getTerrain(Terrains.BLDG_BASEMENT_TYPE);
            if (basement != null) {
                depth = 0;
            }
            int height = Math.max(hex.terrainLevel(Terrains.BLDG_ELEV), hex.terrainLevel(Terrains.BRIDGE_ELEV));
            height = Math.max(height, hex.terrainLevel(Terrains.INDUSTRIAL));
            int yPosition = HEX_H - HEX_TEXT_MARGIN;
            if (level != 0) {
                labels.add(new HexText(Messages.getString("BoardView1.LEVEL") + level,
                      (int) (yPosition * scale), font_elev, color.getRGB(), false, 0));
                yPosition -= 10;
            }
            if (depth != 0) {
                labels.add(new HexText(Messages.getString("BoardView1.DEPTH") + depth,
                      (int) (yPosition * scale), font_elev, color.getRGB(), false, 0));
                yPosition -= 10;
            }
            if (height > 0) {
                labels.add(new HexText(Messages.getString("BoardView1.HEIGHT") + height,
                      (int) (yPosition * scale), font_elev, GUIP.getBuildingTextColor().getRGB(), false, height));
                yPosition -= 10;
            }
            if (hex.terrainLevel(Terrains.FOLIAGE_ELEV) == 1) {
                labels.add(new HexText(Messages.getString("BoardView1.LowFoliage"),
                      (int) (yPosition * scale), font_elev, GUIP.getLowFoliageColor().getRGB(), false, 0));
            }
        }
        return List.copyOf(labels);
    }

    /**
     * Draws the Isometric elevation for the hex at the given coordinates (coords) on the side indicated by the
     * direction (direction). This method only draws a triangle for the elevation, the companion triangle representing
     * the adjacent hex is also needed. The two triangles when drawn together make a complete rectangle representing the
     * complete elevated hex side.
     * <p>
     * By drawing the elevated hex as two separate triangles we avoid clipping problems with other hexes because the
     * lower elevation is rendered before the higher elevation. Thus, any hexes that have a higher elevation than the
     * lower hex will overwrite the lower hex.
     * <p>
     * The Triangle for each hex side is formed by points point1, point2, and p3. Where point1 and point2 are the
     * original hex edges, and p3 has the same X value as point1, but the y value has been increased (or decreased)
     * based on the difference in elevation between the given hex and the adjacent hex.
     *
     * @param coords     Coordinates of the source hex.
     * @param color      Color to use for the elevation polygons.
     * @param point1     The First point on the edge of the hex.
     * @param point2     The second point on the edge of the hex.
     * @param direction  The side of the hex to have the elevation drawn on.
     * @param graphics2D {@link Graphics2D} 2D Graphics Context
     */
    private void drawIsometricElevation(Coords coords, Color color, Point point1, Point point2, int direction,
          Graphics graphics2D) {
        final Hex dest = game.getBoard(boardId).getHexInDir(coords, direction);
        final Hex src = game.getBoard(boardId).getHex(coords);

        if (GUIP.getFloatingIso() || verticalOffset == 0) {
            return;
        }

        // Pad polygon size slightly to avoid rounding errors from scale float.
        int fudge = -1;
        if ((direction == 2) || (direction == 4) || (direction == 3)) {
            fudge = 1;
        }

        final int elev = src.getLevel();
        // If the Destination is null, draw the complete elevation side.
        if ((dest == null) && (elev > 0) && ((direction == 2) || (direction == 3) || (direction == 4))) {

            // Determine the depth of the edge that needs to be drawn.
            int height = elev;
            Hex southHex = game.getBoard(boardId).getHexInDir(coords, 3);
            if ((direction != 3) && (southHex != null) && (elev > southHex.getLevel())) {
                height = elev - southHex.getLevel();
            }
            int scaledHeight = (int) (verticalOffset * scale * height);

            Polygon polygon = new Polygon(new int[] { point1.x, point2.x, point2.x, point1.x },
                  new int[] { point1.y + fudge, point2.y + fudge, point2.y + scaledHeight, point1.y + scaledHeight },
                  4);
            graphics2D.setColor(color);
            graphics2D.drawPolygon(polygon);
            graphics2D.fillPolygon(polygon);

            graphics2D.setColor(Color.BLACK);
            if ((direction == 2) || (direction == 4)) {
                graphics2D.drawLine(point1.x, point1.y, point1.x, point1.y + scaledHeight);
            }
            return;
        } else if (dest == null) {
            return;
        }

        int delta = elev - dest.getLevel();
        // Don't draw the elevation if there is no exposed edge for the player to see.
        if ((delta == 0) || (((direction == 0) || (direction == 1) || (direction == 5)) && (delta > 0)) || (((direction
              == 2)
              || (
              direction == 3) || (direction == 4)) && (delta < 0))) {
            return;
        }

        if ((direction == 2) || (direction == 3) || (direction == 4)) {
            int scaledDelta = (int) (verticalOffset * scale * delta);
            Point p3 = new Point(point1.x, point1.y + scaledDelta + fudge);

            Polygon polygon = new Polygon(new int[] { point1.x, point2.x, point2.x, point1.x },
                  new int[] { point1.y + fudge, point2.y + fudge, point2.y + fudge + scaledDelta,
                              point1.y + fudge + scaledDelta },
                  4);

            if ((point1.y + fudge) < 0) {
                LOGGER.info("Negative (P1) Y value (Fudge)!: {}", (point1.y + fudge));
            }

            if ((point2.y + fudge) < 0) {
                LOGGER.info("Negative (P2) Y value (Fudge)!: {}", (point2.y + fudge));
            }

            if ((point2.y + fudge + scaledDelta) < 0) {
                LOGGER.info("Negative (P2) Y value!: {}", (point2.y + fudge + scaledDelta));
            }

            if ((point1.y + fudge + scaledDelta) < 0) {
                LOGGER.info("Negative (P1) Y value!: {}", (point1.y + fudge + scaledDelta));
            }
            graphics2D.setColor(color);
            graphics2D.drawPolygon(polygon);
            graphics2D.fillPolygon(polygon);

            graphics2D.setColor(Color.BLACK);
            if (direction == 2 || direction == 4) {
                graphics2D.drawLine(point1.x, point1.y, p3.x, p3.y);
            }
        }
    }

    /**
     * Returns true if an elevation line should be drawn between the starting hex and the hex in the direction
     * specified. Results should be transitive, that is, if a line is drawn in one direction, it should be drawn in the
     * opposite direction as well.
     */
    private boolean drawElevationLine(Coords src, int direction) {
        final Hex srcHex = game.getBoard(boardId).getHex(src);
        final Hex destHex = game.getBoard(boardId).getHexInDir(src, direction);
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
     * Given an int-packed RGB value, apply a modifier for the light level and return the result.
     *
     * @param rgb int-packed ARGB value.
     *
     * @return An int-packed ARGB value, which is an adjusted value of the input, based on the light level
     */
    public int getNightDarkenedColor(int rgb) {
        int rd = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int bl = rgb & 0xFF;
        int al = (rgb >> 24);

        switch (game.getPlanetaryConditions().getLight()) {
            case FULL_MOON:
            case MOONLESS:
                rd = rd / 4; // 1/4 red
                gr = gr / 4; // 1/4 green
                bl = bl / 2; // half blue
                break;
            case PITCH_BLACK:
                int gy = (rd + gr + bl) / 16;
                if (Math.random() < 0.3) {
                    gy = gy * 4 / 5;
                }
                if (Math.random() < 0.3) {
                    gy = gy * 5 / 4;
                }
                rd = gy + rd / 5;
                gr = gy + gr / 5;
                bl = gy + bl / 5;
                break;
            case DUSK_DAWN:
                bl = bl * 3 / 4;
                break;
            default:
                break;
        }

        return (al << 24) + (rd << 16) + (gr << 8) + bl;
    }

    /**
     * Generates a Shape drawing area for the hex shadow effect in a lower hex when a higher hex is found in direction.
     */
    private @Nullable Shape getElevationShadowArea(Coords src, int direction) {
        final Hex srcHex = game.getBoard(boardId).getHex(src);
        final Hex destHex = game.getBoard(boardId).getHexInDir(src, direction);

        // When at the board edge, create a shadow in hexes of level < 0
        if (destHex == null) {
            if (srcHex.getLevel() >= 0) {
                return null;
            }
        } else {
            // no shadow area when the current hex is not lower than the next hex in direction
            if (srcHex.getLevel() >= destHex.getLevel()) {
                return null;
            } else if (GUIP.getHexInclines()
                  && (destHex.getLevel() - srcHex.getLevel() < 2)
                  && !destHex.hasCliffTopTowards(srcHex)) {
                return null;
            }
        }

        return AffineTransform.getScaleInstance(scale, scale)
              .createTransformedShape(HexDrawUtilities.getHexBorderArea(direction, HexDrawUtilities.CUT_BORDER, 36));
    }

    /**
     * Generates a fill gradient which is rotated and aligned properly for the drawing area for a hex shadow effect in a
     * lower hex.
     */
    private GradientPaint getElevationShadowGP(Coords src, int direction) {
        final Hex srcHex = game.getBoard(boardId).getHex(src);
        final Hex destHex = game.getBoard(boardId).getHexInDir(src, direction);

        if (destHex == null) {
            return null;
        }

        int levelDifference = destHex.getLevel() - srcHex.getLevel();
        // the shadow strength depends on the level difference, but only to a maximum difference of 3 levels
        levelDifference = Math.min(levelDifference * 5, 15);

        Color c1 = new Color(30, 30, 50, 255); // dark end of shadow
        Color c2 = new Color(50, 50, 70, 0); // light end of shadow

        Point2D p1 = new Point2D.Double(41.5, -25 + levelDifference);
        Point2D p2 = new Point2D.Double(41.5, 8.0 + levelDifference);

        AffineTransform t = new AffineTransform();
        t.scale(scale, scale);
        t.rotate(Math.toRadians(direction * 60), 41.5, 35.5);
        t.transform(p1, p1);
        t.transform(p2, p2);

        return new GradientPaint(p1, c1, p2, c2);
    }

    /**
     * @return The absolute position of the upper-left hand corner of the hex graphic
     */
    private Point getHexLocation(int x, int y, boolean ignoreElevation) {
        float elevationAdjust = 0.0f;

        Hex hex = game.getBoard(boardId).getHex(x, y);
        if ((hex != null) && !ignoreElevation) {
            elevationAdjust = hex.getLevel() * verticalOffset * scale * -1.0f;
        }
        int yPosition = (y * (int) (HEX_H * scale)) + ((x & 1) == 1 ? (int) ((HEX_H / 2.0f) * scale) : 0);
        return new Point(x * (int) (HEX_WC * scale), yPosition + (int) elevationAdjust);
    }

    /**
     * For large tile texture: Returns the absolute position of the upper-left hand corner of the hex graphic When using
     * large tiles multiplying the rounding errors from the (int) cast must be avoided however this cannot be used for
     * small tiles as it will make gaps appear between hexes This will not factor in Isometric as this would be
     * incorrect for large tiles
     */
    static Point getHexLocationLargeTile(int x, int y, float tileScale) {
        int yPosition = (int) (y * HEX_H * tileScale) + ((x & 1) == 1 ? (int) ((HEX_H / 2.0f) * tileScale) : 0);
        return new Point((int) (x * HEX_WC * tileScale), yPosition);
    }

    private Point getHexLocationLargeTile(int x, int y) {
        return getHexLocationLargeTile(x, y, scale);
    }

    public Point getHexLocation(Coords coords) {
        return coords == null ? null : getHexLocation(coords.getX(), coords.getY(), false);
    }

    /**
     * Returns the absolute position of the centre of the hex graphic
     */
    private Point getCentreHexLocation(int x, int y, boolean ignoreElevation) {
        Point p = getHexLocation(x, y, ignoreElevation);
        p.x += (int) Math.floor((HEX_W / 2.0f) * scale);
        p.y += (int) Math.floor((HEX_H / 2.0f) * scale);
        return p;
    }

    public Point getCentreHexLocation(Coords coords) {
        return getCentreHexLocation(coords.getX(), coords.getY(), false);
    }

    public Point getCentreHexLocation(Coords coords, boolean ignoreElevation) {
        return getCentreHexLocation(coords.getX(), coords.getY(), ignoreElevation);
    }

    /**
     * Draws a crosshair-with-circle (bullseye) marker at the given point for the ruler tool. The marker scales with the
     * current board zoom level so it remains visible at all zoom levels.
     */
    private void drawRulerCrosshair(Graphics2D g2d, Point center, Color color) {
        // Scale crosshair size to ~20% of hex width, with a minimum of 4px
        int radius = Math.max(4, (int) (HEX_W * scale * 0.10f));
        int crossLen = Math.max(6, (int) (HEX_W * scale * 0.15f));
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(Math.max(1.5f, scale * 1.5f)));

        // Outer circle
        g2d.setColor(color);
        g2d.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);

        // Crosshair lines extending beyond the circle
        g2d.drawLine(center.x - crossLen, center.y, center.x + crossLen, center.y);
        g2d.drawLine(center.x, center.y - crossLen, center.x, center.y + crossLen);

        // Center dot
        int dotRadius = Math.max(1, (int) (scale * 1.5f));
        g2d.fillOval(center.x - dotRadius, center.y - dotRadius, dotRadius * 2, dotRadius * 2);

        g2d.setStroke(oldStroke);
    }

    public void drawRuler(Coords startCoords, Coords endCoords, Color startColor, Color endColor) {
        rulerStart = startCoords;
        rulerEnd = endCoords;
        rulerStartColor = startColor;
        rulerEndColor = endColor;

        boardPanel.repaint();
    }

    public Coords getRulerStart() {
        return rulerStart;
    }

    public Coords getRulerEnd() {
        return rulerEnd;
    }

    @Override
    public Coords getCoordsAt(Point point) {
        // We must account for the board translation to add padding
        point.x -= HEX_W;
        point.y -= HEX_H;

        // base values
        int x = point.x / (int) (HEX_WC * scale);
        int y = point.y / (int) (HEX_H * scale);
        // correction for the displaced odd columns
        if ((float) point.y / (scale * HEX_H) - y < 0.5) {
            y -= x % 2;
        }

        // check the surrounding hexes if they contain point checking at most 3 hexes would be sufficient but which
        // ones? This is failsafe.
        Coords coords = new Coords(x, y);
        if (!HexDrawUtilities.getHexFull(getHexLocation(coords), scale).contains(point)) {
            boolean hasMatch = false;
            for (int direction = 0;
                  direction < 6 && !hasMatch;
                  direction++) {
                Coords translated = coords.translated(direction);
                if (HexDrawUtilities.getHexFull(getHexLocation(translated), scale).contains(point)) {
                    coords = translated;
                    hasMatch = true;
                }
            }
        }

        // When using isometric rendering, a lower hex can obscure the
        // normal hex. Iterate over all hexes from highest to lowest,
        // looking for a hex that contains the selected mouse click point.
        final int minElev = Math.min(0, game.getBoard(boardId).getMinElevation());
        final int maxElev = Math.max(0, game.getBoard(boardId).getMaxElevation());
        final int delta = (int) Math.ceil(((double) maxElev - minElev) / 3.0f);
        final int minHexSpan = Math.max(y - delta, 0);
        final int maxHexSpan = Math.min(y + delta, game.getBoard(boardId).getHeight());
        for (int elev = maxElev;
              elev >= minElev;
              elev--) {
            for (int i = minHexSpan;
                  i <= maxHexSpan;
                  i++) {
                for (int dx = -1;
                      dx < 2;
                      dx++) {
                    Coords c1 = new Coords(x + dx, i);
                    Hex hexAlt = game.getBoard(boardId).getHex(c1);
                    if (HexDrawUtilities.getHexFull(getHexLocation(c1), scale).contains(point)
                          && (hexAlt != null)
                          && (hexAlt.getLevel() == elev)) {
                        // Return immediately with the highest hex found.
                        return c1;
                    }
                }
            }
        }
        // nothing found
        return new Coords(-1, -1);
    }

    @Override
    public void setTooltipProvider(BoardViewTooltipProvider provider) {
        boardViewToolTip = provider;
    }

    public void redrawMovingEntity(Entity entity, Coords position, int facing, int elevation) {
        Integer entityId = entity.getId();
        ArrayList<Integer> spriteKey = getIdAndLoc(entityId, -1);
        EntitySprite sprite = entitySpriteIds.get(spriteKey);
        IsometricSprite isoSprite = isometricSpriteIds.get(spriteKey);
        // We can ignore secondary locations for now, as we don't have moving multi-location entities (will need to
        // change for mobile structures)

        PriorityQueue<EntitySprite> newSprites;
        PriorityQueue<IsometricSprite> isoSprites;
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds;
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds;

        // Remove sprite for Entity, so it's not displayed while moving
        if (sprite != null) {
            removeSprite(sprite);
            newSprites = new PriorityQueue<>(entitySprites);
            newSpriteIds = new HashMap<>(entitySpriteIds);

            newSprites.remove(sprite);
            newSpriteIds.remove(spriteKey);

            entitySprites = newSprites;
            entitySpriteIds = newSpriteIds;
        }
        // Remove iso sprite for Entity, so it's not displayed while moving
        if (isoSprite != null) {
            removeSprite(isoSprite);
            isoSprites = new PriorityQueue<>(isometricSprites);
            newIsoSpriteIds = new HashMap<>(isometricSpriteIds);

            isoSprites.remove(isoSprite);
            newIsoSpriteIds.remove(spriteKey);

            isometricSprites = isoSprites;
            isometricSpriteIds = newIsoSpriteIds;
        }

        MovingEntitySprite mSprite = movingEntitySpriteIds.get(entityId);
        ArrayList<MovingEntitySprite> newMovingSprites = new ArrayList<>(movingEntitySprites);
        HashMap<Integer, MovingEntitySprite> newMovingSpriteIds = new HashMap<>(movingEntitySpriteIds);
        // Remove any old movement sprite
        if (mSprite != null) {
            newMovingSprites.remove(mSprite);
        }
        // Create new movement sprite
        if (isOnThisBord(entity)) {
            mSprite = new MovingEntitySprite(this, entity, position, facing, elevation);
            newMovingSprites.add(mSprite);
            newMovingSpriteIds.put(entityId, mSprite);
        }

        movingEntitySprites = newMovingSprites;
        movingEntitySpriteIds = newMovingSpriteIds;
    }

    public boolean isMovingUnits() {
        return !movingUnits.isEmpty();
    }

    /**
     * @param entityId     The Entity ID
     * @param secondaryLoc the secondary loc index, or -1 for Entities without secondary positions
     *
     * @return a Key value for the entitySpriteIds and isometricSprite maps. The List contains as the first element the
     *       Entity ID and as the second element its location ID: either -1 if the Entity has no secondary locations, or
     *       the index of its secondary location.
     */
    private ArrayList<Integer> getIdAndLoc(Integer entityId, int secondaryLoc) {
        ArrayList<Integer> idLoc = new ArrayList<>(2);
        idLoc.add(entityId);
        idLoc.add(secondaryLoc);
        return idLoc;
    }

    /**
     * Clears the sprite for an entity and prepares it to be re-drawn. Replaces the old sprite with the new! Takes a
     * reference to the Entity object before changes, in case it contained important state information, like DropShips
     * taking off (airborne DropShips lose their secondary hexes). Try to prevent annoying
     * ConcurrentModificationExceptions
     */
    public void redrawEntity(Entity entity) {
        Integer entityId = entity.getId();

        // Remove sprites from backing sprite collections before modifying the entitySprites and isometricSprites.
        // Otherwise, orphaned overTerrainSprites or behindTerrainHexSprites can result.
        removeSprites(entitySprites);
        removeSprites(isometricSprites);

        // If the entity we are updating doesn't have a position, ensure we
        // remove all of its old sprites
        if (entity.getPosition() == null || !isOnThisBord(entity)) {
            Iterator<EntitySprite> spriteIter;

            // Remove Entity Sprites
            spriteIter = entitySprites.iterator();
            while (spriteIter.hasNext()) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.getEntity().equals(entity)) {
                    spriteIter.remove();
                }
            }

            // Update ID -> Sprite map
            spriteIter = entitySpriteIds.values().iterator();
            while (spriteIter.hasNext()) {
                EntitySprite sprite = spriteIter.next();
                if (sprite.getEntity().equals(entity)) {
                    spriteIter.remove();
                }
            }

            Iterator<IsometricSprite> isoSpriteIter;

            // Remove IsometricSprites
            isoSpriteIter = isometricSprites.iterator();
            while (isoSpriteIter.hasNext()) {
                IsometricSprite sprite = isoSpriteIter.next();
                if (sprite.getEntity().equals(entity)) {
                    isoSpriteIter.remove();
                }
            }

            // Update ID -> Iso Sprite Map
            isoSpriteIter = isometricSpriteIds.values().iterator();
            while (isoSpriteIter.hasNext()) {
                IsometricSprite sprite = isoSpriteIter.next();
                if (sprite.getEntity().equals(entity)) {
                    isoSpriteIter.remove();
                }
            }
        }

        // Create a copy of the sprite list
        Queue<EntitySprite> newSprites = new PriorityQueue<>(entitySprites);
        HashMap<ArrayList<Integer>, EntitySprite> newSpriteIds = new HashMap<>(entitySpriteIds);
        Queue<IsometricSprite> isoSprites = new PriorityQueue<>(isometricSprites);
        HashMap<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds = new HashMap<>(isometricSpriteIds);

        // Remove the sprites we are going to update
        EntitySprite sprite = entitySpriteIds.get(getIdAndLoc(entityId, -1));
        IsometricSprite isoSprite = isometricSpriteIds.get(getIdAndLoc(entityId, -1));
        if (sprite != null) {
            newSprites.remove(sprite);
        }

        if (isoSprite != null) {
            isoSprites.remove(isoSprite);
        }

        for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
            sprite = entitySpriteIds.get(getIdAndLoc(entityId, secondaryPos));
            if (sprite != null) {
                newSprites.remove(sprite);
            }
            isoSprite = isometricSpriteIds.get(getIdAndLoc(entityId, secondaryPos));
            if (isoSprite != null) {
                isoSprites.remove(isoSprite);
            }
        }

        // Create the new sprites
        Coords position = entity.getPosition();
        boolean canSee = EntityVisibilityUtils.detectedOrHasVisual(getLocalPlayer(), game, entity);

        if ((position != null) && canSee && isOnThisBord(entity)) {
            // Add new EntitySprite
            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                sprite = new EntitySprite(this, entity, -1, radarBlipImage);
                newSprites.add(sprite);
                newSpriteIds.put(getIdAndLoc(entityId, -1), sprite);
            } else {
                // Add all secondary position sprites, which includes a sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    sprite = new EntitySprite(this, entity, secondaryPos, radarBlipImage);
                    newSprites.add(sprite);
                    newSpriteIds.put(getIdAndLoc(entityId, secondaryPos), sprite);
                }
            }

            // Add new IsometricSprite
            // If no secondary positions, add a sprite for the central position
            if (entity.getSecondaryPositions().isEmpty()) {
                isoSprite = new IsometricSprite(this, entity, -1, radarBlipImage);
                isoSprites.add(isoSprite);
                newIsoSpriteIds.put(getIdAndLoc(entityId, -1), isoSprite);
            } else {
                // Add all secondary position sprites, which includes a sprite for the central hex
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    isoSprite = new IsometricSprite(this, entity, secondaryPos, radarBlipImage);
                    isoSprites.add(isoSprite);
                    newIsoSpriteIds.put(getIdAndLoc(entityId, secondaryPos), isoSprite);
                }
            }
        }

        // Update Sprite state with new collections
        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;
        isometricSprites = isoSprites;
        isometricSpriteIds = newIsoSpriteIds;
        addSprites(entitySprites);
        addSprites(isometricSprites);

        // Remove C3 sprites
        c3Sprites.removeIf(c3sprite -> (c3sprite.getEntityId() == entity.getId()) || (c3sprite.getMasterId()
              == entity.getId()));

        // Update C3 link, if necessary
        if (entity.hasC3() || entity.hasC3i() || entity.hasNovaCEWS() || entity.hasNavalC3()) {
            addC3Link(entity);
        }

        // The removal above also dropped the lines that this entity's hierarchic subordinates draw TO it (each
        // slave owns its own line to its master), and addC3Link(entity) only redraws the entity's own line to its
        // master. Re-add the subordinates' lines, otherwise selecting a master in the firing phase erases its
        // network on the board until the next full redraw.
        for (Entity subordinate : game.getEntitiesVector()) {
            if ((subordinate.getC3MasterId() == entity.getId())
                  && !subordinate.equals(entity)
                  && subordinate.hasC3()
                  && isOnThisBord(subordinate)) {
                addC3Link(subordinate);
            }
        }

        vtolAttackSprites.removeIf(s -> s.getEntity().getId() == entity.getId());

        // Remove Flyover Sprites
        flyOverSprites.removeIf(flyOverSprite -> flyOverSprite.getEntityId() == entity.getId());

        // Add Flyover path, if necessary
        if ((boardId == entity.getPassedThroughBoardId())
              && (entity.isAirborne() || entity.isMakingVTOLGroundAttack())
              && (entity.getPassedThrough().size() > 1)) {
            addFlyOverPath(entity);
        }

        updateEcmList();
        highlightSelectedEntity(getSelectedEntity());
        scheduleRedraw();
    }

    /**
     * Clears all old entity sprites out of memory and sets up new ones.
     */
    public void redrawAllEntities() {
        int numEntities = game.getNoOfEntities();
        // Prevent IllegalArgumentException
        numEntities = Math.max(1, numEntities);
        Queue<EntitySprite> newSprites = new PriorityQueue<>(numEntities);
        Queue<IsometricSprite> newIsometricSprites = new PriorityQueue<>(numEntities);
        Map<ArrayList<Integer>, EntitySprite> newSpriteIds = new HashMap<>(numEntities);
        Map<ArrayList<Integer>, IsometricSprite> newIsoSpriteIds = new HashMap<>(numEntities);

        ArrayList<WreckSprite> newWrecks = new ArrayList<>();
        ArrayList<IsometricWreckSprite> newIsometricWrecks = new ArrayList<>();

        Board board = game.getBoard(boardId);
        Enumeration<Entity> e = game.getWreckedEntities();
        while (e.hasMoreElements()) {
            Entity entity = e.nextElement();
            Coords position = entity.getPosition();
            // Infantry don't leave wrecks, but CVEP (which extends Infantry) should show crashed pod wreckage
            boolean isInfantryButNotCVEP = (entity instanceof Infantry) && !(entity instanceof CombatVehicleEscapePod);
            if (isOnThisBord(entity)
                  && !isInfantryButNotCVEP
                  && (position != null)
                  && board.contains(position)) {
                WreckSprite wreckSprite;
                IsometricWreckSprite isometricWreckSprite;
                if (entity.getSecondaryPositions().isEmpty()) {
                    wreckSprite = new WreckSprite(this, entity, -1);
                    newWrecks.add(wreckSprite);
                    isometricWreckSprite = new IsometricWreckSprite(this, entity, -1);
                    newIsometricWrecks.add(isometricWreckSprite);
                } else {
                    for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                        wreckSprite = new WreckSprite(this, entity, secondaryPos);
                        newWrecks.add(wreckSprite);
                        isometricWreckSprite = new IsometricWreckSprite(this, entity, secondaryPos);
                        newIsometricWrecks.add(isometricWreckSprite);
                    }
                }
            }
        }

        clearC3Networks();
        clearFlyOverPaths();
        for (Entity entity : game.getEntitiesVector()) {
            if ((boardId == entity.getPassedThroughBoardId()) && (entity.isAirborne()
                  || entity.isMakingVTOLGroundAttack()) && (
                  entity.getPassedThrough().size() > 1)) {
                addFlyOverPath(entity);
            }
            if (entity.getPosition() == null || !isOnThisBord(entity)) {
                continue;
            }
            if ((localPlayer != null)
                  && game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                  && entity.getOwner().isEnemyOf(localPlayer)
                  && !entity.hasSeenEntity(localPlayer)
                  && !entity.hasDetectedEntity(localPlayer)) {
                continue;
            }
            if ((localPlayer != null)
                  && game.getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)
                  && entity.getOwner().isEnemyOf(localPlayer)
                  && entity.isHidden()) {
                continue;
            }
            if (entity.getSecondaryPositions().isEmpty()) {
                EntitySprite sprite = new EntitySprite(this, entity, -1, radarBlipImage);
                newSprites.add(sprite);
                newSpriteIds.put(getIdAndLoc(entity.getId(), -1), sprite);
                IsometricSprite isometricSprite = new IsometricSprite(this, entity, -1, radarBlipImage);
                newIsometricSprites.add(isometricSprite);
                newIsoSpriteIds.put(getIdAndLoc(entity.getId(), -1), isometricSprite);
            } else {
                for (int secondaryPos : entity.getSecondaryPositions().keySet()) {
                    EntitySprite sprite = new EntitySprite(this, entity, secondaryPos, radarBlipImage);
                    newSprites.add(sprite);
                    newSpriteIds.put(getIdAndLoc(entity.getId(), secondaryPos), sprite);

                    IsometricSprite isometricSprite = new IsometricSprite(this, entity, secondaryPos, radarBlipImage);
                    newIsometricSprites.add(isometricSprite);
                    newIsoSpriteIds.put(getIdAndLoc(entity.getId(), secondaryPos), isometricSprite);
                }
            }

            if (entity.hasC3() || entity.hasC3i() || entity.hasNovaCEWS() || entity.hasNavalC3()) {
                addC3Link(entity);
            }
        }

        removeSprites(entitySprites);
        removeSprites(isometricSprites);

        entitySprites = newSprites;
        entitySpriteIds = newSpriteIds;

        isometricSprites = newIsometricSprites;
        isometricSpriteIds = newIsoSpriteIds;

        addSprites(entitySprites);
        addSprites(isometricSprites);

        wreckSprites = newWrecks;
        isometricWreckSprites = newIsometricWrecks;

        // Update ECM list, to ensure that Sprites are updated with ECM info
        updateEcmList();
        // Re-highlight a selected entity, if present
        highlightSelectedEntity(getSelectedEntity());

        scheduleRedraw();
    }

    /**
     * Moves the cursor to the new position, or hides it, if newPosition is null
     */
    private void moveCursor(CursorSprite cursor, Coords newPosition) {
        final Rectangle oldBounds = new Rectangle(cursor.getBounds());
        if (newPosition != null) {
            cursor.setHexLocation(newPosition);
        } else {
            cursor.setOffScreen();
        }
        // repaint affected area
        boardPanel.repaint(oldBounds);
        boardPanel.repaint(cursor.getBounds());
    }

    /**
     * Centers the board on the position of the selected unit, if any. Uses smooth centering if activated in the client
     * settings.
     */
    public void centerOnSelected() {
        if (isOnThisBord(getSelectedEntity())) {
            clientgui.showBoardView(boardId);
            centerOn(getSelectedEntity());
        }
    }

    /**
     * Centers the board on the position of the given unit. Uses smooth centering if activated in the client settings.
     * The given entity may be null, in which case nothing happens.
     *
     * @param entity The unit to center on.
     */
    public void centerOn(@Nullable Entity entity) {
        if (entity != null) {
            centerOnHex(entity.getPosition(), entity.getId());
        }
    }

    @Override
    public void centerOnHex(@Nullable Coords coords) {
        centerOnHex(coords, Entity.NONE);
    }

    private void centerOnHex(@Nullable Coords coords, int entityId) {
        if (coords == null) {
            return;
        }
        centerRequest = new CenterRequest(centerRequest.sequence() + 1, coords, entityId);

        // A native camera request must not construct or move the legacy viewport.
        if (scrollPane == null || (clientgui != null && GpuBoardWindow.isActiveFor(clientgui))) {
            stopSoftCentering();
            return;
        }

        if (GUIP.getSoftCenter()) {
            // Soft Centering:
            // set the target point
            Point p = getCentreHexLocation(coords);
            softCenterTarget.setLocation(p.x / boardSize.getWidth(), p.y / boardSize.getHeight());

            // adjust the target point because the board can't center on points too close to an edge
            double width = scrollPane.getViewport().getWidth();
            double height = scrollPane.getViewport().getHeight();
            double boardSizeWidth = boardSize.getWidth();
            double boardSizeHeight = boardSize.getHeight();

            double minX = (width / 2 - HEX_W) / boardSizeWidth;
            double minY = (height / 2 - HEX_H) / boardSizeHeight;
            double maxX = (boardSizeWidth + HEX_W - width / 2) / boardSizeWidth;
            double maxY = (boardSizeHeight + HEX_H - height / 2) / boardSizeHeight;

            // here the order is important because the top/left edges always stop the board, the bottom/right only
            // when the board is big enough
            softCenterTarget.setLocation(Math.min(softCenterTarget.getX(), maxX),
                  Math.min(softCenterTarget.getY(), maxY));

            softCenterTarget.setLocation(Math.max(softCenterTarget.getX(), minX),
                  Math.max(softCenterTarget.getY(), minY));

            // get the current board center point
            double[] visibleArea = getVisibleArea();
            oldCenter.setLocation((visibleArea[0] + visibleArea[2]) / 2, (visibleArea[1] + visibleArea[3]) / 2);

            waitTimer = 0;
            isSoftCentering = true;

        } else {
            // no soft centering:
            // center on coords directly
            Point centreHexLocation = getCentreHexLocation(coords);
            centerOnPointRel(centreHexLocation.x / boardSize.getWidth(), centreHexLocation.y / boardSize.getHeight());
        }
    }

    /**
     * Moves the board one step towards the final position in during soft centering.
     */
    private synchronized void centerOnHexSoftStep(long deltaTime) {
        if (isSoftCentering) {
            // don't move the board if 20ms haven't passed since the last move
            waitTimer += deltaTime;
            if (waitTimer < 20) {
                return;
            }
            waitTimer = 0;

            // move the board by a fraction of the distance to the target
            Point2D newCenter = new Point2D.Double(oldCenter.getX()
                  + (softCenterTarget.getX() - oldCenter.getX()) / SOFT_CENTER_SPEED,
                  oldCenter.getY() + (softCenterTarget.getY() - oldCenter.getY()) / SOFT_CENTER_SPEED);
            centerOnPointRel(newCenter.getX(), newCenter.getY());

            oldCenter = newCenter;

            // stop the motion when close enough to the final position
            if (softCenterTarget.distance(newCenter) < 0.0005) {
                stopSoftCentering();
                pingMinimap();
            }
        }
    }

    public void stopSoftCentering() {
        isSoftCentering = false;
    }

    private void adjustVisiblePosition(@Nullable Coords coords, @Nullable Point dispPoint, double inHexDeltaX,
          double inHexDeltaY) {
        if (scrollPane == null || (coords == null) || (dispPoint == null)) {
            return;
        }

        Point hexPoint = getCentreHexLocation(coords);
        // correct for upper left board padding
        hexPoint.translate(HEX_W, HEX_H);
        JScrollBar horizontalScroll = scrollPane.getHorizontalScrollBar();
        horizontalScroll.setValue(hexPoint.x - dispPoint.x + (int) (inHexDeltaX * scale * HEX_W));
        JScrollBar verticalScroll = scrollPane.getVerticalScrollBar();
        verticalScroll.setValue(hexPoint.y - dispPoint.y + (int) (inHexDeltaY * scale * HEX_H));
        pingMinimap();
        boardPanel.repaint();
    }

    /**
     * Centers the board to a point
     *
     * @param xRelative the x position relative to board width.
     * @param yRelative the y position relative to board height. Both xRelative and yRelative should be between 0 and 1.
     *                  The method will clip both values to this range.
     */
    public void centerOnPointRel(double xRelative, double yRelative) {
        updateBoardSize();
        // restrict both values to between 0 and 1
        xRelative = Math.max(0, xRelative);
        xRelative = Math.min(1, xRelative);
        yRelative = Math.max(0, yRelative);
        yRelative = Math.min(1, yRelative);
        Point point = new Point((int) (boardSize.getWidth() * xRelative) + HEX_W,
              (int) (boardSize.getHeight() * yRelative) + HEX_H);
        if (scrollPane == null || (clientgui != null && GpuBoardWindow.isActiveFor(clientgui))) {
            Coords coords = getCoordsAt(point);
            if (!getBoard().contains(coords)) {
                coords = new Coords(Math.clamp((int) (xRelative * getBoard().getWidth()), 0, getBoard().getWidth() - 1),
                      Math.clamp((int) (yRelative * getBoard().getHeight()), 0, getBoard().getHeight() - 1));
            }
            centerOnHex(coords);
            return;
        }
        JScrollBar verticalScroll = scrollPane.getVerticalScrollBar();
        verticalScroll.setValue(point.y - (verticalScroll.getVisibleAmount() / 2));
        JScrollBar horizontalScroll = scrollPane.getHorizontalScrollBar();
        horizontalScroll.setValue(point.x - (horizontalScroll.getVisibleAmount() / 2));
        boardPanel.repaint();
    }

    /**
     * Returns the currently visible area of the board.
     *
     * @return an array of 4 double values indicating the relative size, where the first two values indicate the x and y
     *       position of the upper left corner of the visible area and the second two values the x and y position of the
     *       lower right corner. So when the whole board is visible, the values should be 0, 0, 1, 1. When the lower
     *       right corner of the board is visible and 90% of width and height: 0.1, 0.1, 1, 1 Due to board padding the
     *       values can be outside [0;1]
     */
    public double[] getVisibleArea() {
        if (scrollPane == null) {
            return new double[] { 0, 0, 1, 1 };
        }
        double[] values = new double[4];
        double x = scrollPane.getViewport().getViewPosition().getX();
        double y = scrollPane.getViewport().getViewPosition().getY();
        double width = scrollPane.getViewport().getWidth();
        double height = scrollPane.getViewport().getHeight();
        double boardSizeWidth = boardSize.getWidth();
        double boardSizeHeight = boardSize.getHeight();

        values[0] = (x - HEX_W) / boardSizeWidth;
        values[1] = (y - HEX_H) / boardSizeHeight;
        values[2] = (x - HEX_W + width) / boardSizeWidth;
        values[3] = (y - HEX_H + height) / boardSizeHeight;

        return values;
    }

    /**
     * Clears the old movement data and draws the new.
     */
    public void drawMovementData(Entity entity, MovePath movePath) {
        MoveStep previousStep = null;

        clearMovementData();

        // Nothing to do if we don't have a MovePath
        if (movePath == null) {
            movementTarget = null;
            return;
        }
        // need to update the movement sprites based on the move path for this entity only way to do this is to clear
        // and refresh (seems wasteful)

        // first get the color for the vector
        Color color = Color.blue;
        if (movePath.getLastStep() != null) {
            color = switch (movePath.getLastStep().getMovementType(true)) {
                case MOVE_RUN, MOVE_VTOL_RUN, MOVE_OVER_THRUST -> GUIP.getMoveRunColor();
                case MOVE_SPRINT, MOVE_VTOL_SPRINT -> GUIP.getMoveSprintColor();
                case MOVE_JUMP -> GUIP.getMoveJumpColor();
                case MOVE_ILLEGAL -> GUIP.getMoveIllegalColor();
                default -> GUIP.getMoveDefaultColor();
            };
            movementTarget = movePath.getLastStep().getPosition();
        } else {
            movementTarget = null;
        }

        refreshMoveVectors(entity, movePath, color);

        for (ListIterator<MoveStep> i = movePath.getSteps();
              i.hasNext(); ) {
            final MoveStep step = i.next();
            if ((null != previousStep) && ((step.getType() == MoveStepType.UP)
                  || (step.getType() == MoveStepType.DOWN)
                  || (step.getType() == MoveStepType.ACC)
                  || (step.getType() == MoveStepType.DEC)
                  || (step.getType() == MoveStepType.ACCELERATION)
                  || (step.getType() == MoveStepType.DECELERATION))) {
                // Mark the previous elevation change sprite hidden so that we can draw a new one in its place
                // without having overlap.
                pathSprites.getLast().setHidden(true);
            }

            if (previousStep != null
                  // for advanced movement, we always need to hide prior because costs will overlap, and we only
                  // want the current facing
                  && (game.useVectorMove()
                  // A LAM converting from AirMek to Biped uses two convert steps, and we only want to
                  // show the last.
                  || (step.getType() == MoveStepType.CONVERT_MODE
                  && previousStep.getType() == MoveStepType.CONVERT_MODE)
                  || step.getType() == MoveStepType.BOOTLEGGER)) {
                pathSprites.getLast().setHidden(true);
            }

            pathSprites.add(new StepSprite(this, step, movePath.isEndStep(step)));
            previousStep = step;
        }

        displayFlightPathIndicator(movePath);
        boardPanel.repaint(100);
    }

    /**
     * Add Aerospace ground map flight path indicators on the last step based on how much aerodyne movement is left.
     * This will add sprites along the forward path for the remaining velocity and indicate what point along it's
     * forward path the unit can turn.
     *
     * @param movePath - Current MovePath that represents the current units movement state
     */
    private void displayFlightPathIndicator(MovePath movePath) {
        // Don't attempt displaying Flight Path Indicators if using advanced aero movement.
        if (game.useVectorMove()) {
            return;
        }

        // Don't calculate any kind of flight path indicators if the move is not legal.
        if (movePath.getLastStepMovementType() == EntityMovementType.MOVE_ILLEGAL) {
            return;
        }

        // If the unit has remaining aerodyne velocity display the flight path indicators for remaining velocity.
        if ((movePath.getFinalVelocityLeft() > 0) && !movePath.nextForwardStepOffBoard()) {
            List<MoveStep> fpiSteps = new ArrayList<>();

            // Cloning the current movement path because we don't want to change its state.
            MovePath fpiPath = movePath.clone();

            // While velocity remains, add a forward step to the cloned movement path.
            while (fpiPath.getFinalVelocityLeft() > 0) {
                fpiPath.addStep(MoveStepType.FORWARDS);
                fpiSteps.add(fpiPath.getLastStep());

                // short circuit the flight path indicator if we are off the board.
                if (fpiPath.nextForwardStepOffBoard()) {
                    break;
                }
            }

            // For each hex in the entities forward trajectory, add a flight turn indicator sprite.
            for (MoveStep moveStep : fpiSteps) {
                fpiSprites.add(new FlightPathIndicatorSprite(this,
                      fpiSteps,
                      fpiSteps.indexOf(moveStep),
                      fpiPath.isEndStep(moveStep)));
            }
        }
    }

    /**
     * Clears current movement data from the screen
     */
    public void clearMovementData() {
        pathSprites = new ArrayList<>();
        fpiSprites = new ArrayList<>();
        movementTarget = null;
        checkFoVHexImageCacheClear();
        boardPanel.repaint();
        refreshMoveVectors();
    }

    public void addStrafingCoords(Coords coords) {
        strafingCoords.add(coords);
        repaint();
    }

    public void setStrafingCoords(Collection<Coords> coords) {
        strafingCoords.clear();
        strafingCoords.addAll(coords);
        repaint();
    }

    public void clearStrafingCoords() {
        strafingCoords.clear();
        repaint();
    }

    public ClientGUI getClientgui() {
        return clientgui;
    }

    public float getScale() {
        return scale;
    }

    public Dimension getHexSize() {
        return hex_size;
    }

    public TilesetManager getTileManager() {
        return tileManager;
    }

    public Shape[] getFacingPolys() {
        return facingPolys;
    }

    public Shape[] getMovementPolys() {
        return movementPolys;
    }

    public Shape getUpArrow() {
        return upArrow;
    }

    public Shape getDownArrow() {
        return downArrow;
    }

    /**
     * Specifies that this should mark the deployment hexes for a player. If the player is set to null, no hexes will be
     * marked.
     */
    public void markDeploymentHexesFor(Entity ce) {
        en_Deployer = ce;
        repaint();
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
    public void addFlyOverPath(Entity entity) {
        if (entity.getPosition() == null) {
            return;
        }

        if (entity.isMakingVTOLGroundAttack()) {
            vtolAttackSprites.add(new VTOLAttackSprite(this, entity));
        }
        flyOverSprites.add(new FlyOverSprite(this, entity));
    }

    /**
     * @param coords the given coords
     *
     * @return any entities flying over the given coords
     */
    public ArrayList<Entity> getEntitiesFlyingOver(Coords coords) {
        ArrayList<Entity> entities = new ArrayList<>();
        for (FlyOverSprite flyOverSprite : flyOverSprites) {
            // Space borne units shouldn't count here. They show up incorrectly in the firing display when sensors
            // are in use.
            if (flyOverSprite.getEntity().getPassedThrough().contains(coords) && !flyOverSprite.getEntity()
                  .isSpaceborne()) {
                entities.add(flyOverSprite.getEntity());
            }
        }
        return entities;
    }

    /**
     * Adds a c3 line to the sprite list.
     */
    public void addC3Link(Entity entity) {
        if (entity.getPosition() == null) {
            return;
        }

        if (entity.hasC3i()) {
            for (Entity entity1 : game.getEntitiesVector()) {
                if (entity1.getPosition() == null) {
                    return;
                }

                if (entity.onSameC3NetworkAs(entity1) && !entity1.equals(entity) && !ComputeECM.isAffectedByECM(entity,
                      entity.getPosition(),
                      entity1.getPosition())) {
                    c3Sprites.add(new C3Sprite(this, entity, entity1));
                }
            }
        } else if (entity.hasNavalC3()) {
            for (Entity entity1 : game.getEntitiesVector()) {
                if (entity1.getPosition() == null) {
                    return;
                }

                if (entity.onSameC3NetworkAs(entity1) && !entity1.equals(entity)) {
                    c3Sprites.add(new C3Sprite(this, entity, entity1));
                }
            }
        } else if (entity.hasNovaCEWS()) {
            // WOR Nova CEWS
            for (Entity entity1 : game.getEntitiesVector()) {
                if (entity1.getPosition() == null) {
                    return;
                }
                ECMInfo ecmInfo = ComputeECM.getECMEffects(entity,
                      entity.getPosition(),
                      entity1.getPosition(),
                      true,
                      null);
                if (entity.onSameC3NetworkAs(entity1)
                      && !entity1.equals(entity)
                      && (ecmInfo != null)
                      && !ecmInfo.isNovaECM()) {
                    c3Sprites.add(new C3Sprite(this, entity, entity1));
                }
            }
        } else if (entity.getC3Master() != null) {
            Entity eMaster = entity.getC3Master();
            if (eMaster.getPosition() == null) {
                return;
            }

            // A unit whose C3 gear is switched off is not on the network, so neither end draws a link. The
            // non-hierarchic branches above get this from onSameC3NetworkAs(); the hierarchic branch does not
            // consult it, so the same check is applied here. Network wiring is left intact, so the links come
            // back when the gear is switched on again.
            if (EquipmentActivation.isC3SwitchedOff(entity) || EquipmentActivation.isC3SwitchedOff(eMaster)) {
                return;
            }

            // ECM cuts off the network
            boolean blocked;

            if (entity.hasBoostedC3() && eMaster.hasBoostedC3()) {
                blocked = ComputeECM.isAffectedByAngelECM(entity, entity.getPosition(), eMaster.getPosition())
                      || ComputeECM.isAffectedByAngelECM(eMaster, eMaster.getPosition(), eMaster.getPosition());
            } else {
                blocked = ComputeECM.isAffectedByECM(entity, entity.getPosition(), eMaster.getPosition())
                      || ComputeECM.isAffectedByECM(eMaster, eMaster.getPosition(), eMaster.getPosition());
            }

            if (!blocked) {
                c3Sprites.add(new C3Sprite(this, entity, entity.getC3Master()));
            }
        }
    }

    /**
     * Adds an attack to the sprite list.
     */
    public void addAttack(AttackAction attackAction) {
        // Don't make sprites for unknown entities and sensor returns
        // cross-board attacks don't get attack arrows (for now, must possibly allow some A2G, O2G, A2A attacks later
        // when target/attacker hexes are not really but effectively on the same board)
        Entity weaponEntity = game.getEntity(attackAction.getEntityId());
        if (weaponEntity == null) {
            return;
        }
        Entity attacker = weaponEntity.getAttackingEntity();
        Targetable target = game.getTarget(attackAction.getTargetType(), attackAction.getTargetId());
        if ((attacker == null)
              || (target == null)
              || (target.getTargetType() == Targetable.TYPE_I_NARC_POD)
              || (target.getPosition() == null)
              || (attacker.getPosition() == null)
              || !game.onTheSameBoard(attacker, target)
              || !isOnThisBord(target)) {
            return;
        }
        EntitySprite entitySprite = entitySpriteIds.get(getIdAndLoc(attacker.getId(),
              (attacker.getSecondaryPositions().isEmpty() ? -1 : 0)));
        if (entitySprite != null && entitySprite.onlyDetectedBySensors()) {
            return;
        }

        boardPanel.repaint(100);
        int attackerId = attackAction.getEntityId();
        for (AttackSprite sprite : attackSprites) {
            // can we just add this attack to an existing one?
            if ((sprite.getEntityId() == attackerId) && (sprite.getTargetId() == attackAction.getTargetId())) {
                // use existing attack, but add this weapon
                sprite.addEntityAction(attackAction);
                rebuildAllSpriteDescriptions(attackerId);
                return;
            }
        }
        // no re-use possible, add a new one don't add a sprite for an artillery attack made by the other player
        if (attackAction instanceof WeaponAttackAction weaponAttackAction) {
            int ownerId = weaponAttackAction.getEntity(game).getOwner().getId();
            int teamId = weaponAttackAction.getEntity(game).getOwner().getTeam();

            if (attackAction.getTargetType() != Targetable.TYPE_HEX_ARTILLERY) {
                attackSprites.add(new AttackSprite(this, attackAction));
            } else if (ownerId == getLocalPlayer().getId() || teamId == getLocalPlayer().getTeam()) {
                attackSprites.add(new AttackSprite(this, attackAction));
            }
        } else {
            attackSprites.add(new AttackSprite(this, attackAction));
        }
        rebuildAllSpriteDescriptions(attackerId);
    }

    /**
     * adding a new EntityAction may affect the ToHits of other attacks so rebuild. The underlying data is cached when
     * possible, so the should o the minimum amount of work needed
     */
    void rebuildAllSpriteDescriptions(int attackerId) {
        for (AttackSprite sprite : attackSprites) {
            if (sprite.getEntityId() == attackerId) {
                sprite.rebuildDescriptions();
            }
        }

    }

    /**
     * Removes all attack sprites from a certain entity
     */
    public synchronized void removeAttacksFor(@Nullable Entity entity) {
        if (entity == null) {
            return;
        }

        int entityId = entity.getId();
        attackSprites.removeIf(sprite -> sprite.getEntityId() == entityId);
        boardPanel.repaint(100);
    }

    /**
     * Clears out all attacks and re-adds the ones in the current game.
     */
    public void refreshAttacks() {
        clearAllAttacks();
        for (Enumeration<EntityAction> i = game.getActions();
              i.hasMoreElements(); ) {
            EntityAction entityAction = i.nextElement();
            if (entityAction instanceof AttackAction attackAction) {
                addAttack(attackAction);
            }
        }

        for (Enumeration<AttackAction> i = game.getDisplacementAttacks();
             i.hasMoreElements(); ) {
            AttackAction attackAction = i.nextElement();
            if (attackAction instanceof PhysicalAttackAction physicalAttackAction) {
                addAttack(physicalAttackAction);
            }
        }
        boardPanel.repaint(100);
    }

    public void refreshMoveVectors() {
        clearAllMoveVectors();
        if (game.useVectorMove()) {
            for (Entity entity : game.getEntitiesVector()) {
                if (entity.getPosition() != null) {
                    movementSprites.add(new MovementSprite(this, entity, entity.getVectors(), Color.GRAY, false));
                }
            }
        }
    }

    public void refreshMoveVectors(Entity entity, MovePath movePath, Color color) {
        clearAllMoveVectors();
        if (game.useVectorMove()) {
            // same as normal but when I find the active entity I used the MovePath to get vector
            for (Entity entity1 : game.getEntitiesVector()) {
                if (entity1.getPosition() != null) {
                    if ((entity != null) && (entity1.getId() == entity.getId())) {
                        movementSprites.add(new MovementSprite(this, entity1, movePath.getFinalVectors(), color, true));
                    } else {
                        movementSprites.add(new MovementSprite(this, entity1, entity1.getVectors(), color, false));
                    }
                }
            }
        }
    }

    public void clearC3Networks() {
        c3Sprites.clear();
    }

    public void clearFlyOverPaths() {
        vtolAttackSprites.clear();
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

    private void firstLOSHex(Coords coords) {
        if (useLOSTool) {
            moveCursor(secondLOSSprite, null);
            moveCursor(firstLOSSprite, coords);
        }
    }

    private void secondLOSHex(Coords targetCoords, Coords attackerCoords) {
        if (useLOSTool) {
            moveCursor(secondLOSSprite, targetCoords);
            // LOS calculation and display is handled by RulerDialog via the
            // BOARD_SECOND_LOS_HEX event fired by checkLOS()
        }
    }

    /**
     * Initializes the various overlay polygons with their vertices.
     */
    public void initPolys() {
        AffineTransform facingRotate = new AffineTransform();

        // facing polygons
        Polygon facingPolyTmp = new Polygon();
        facingPolyTmp.addPoint(41, 3);
        facingPolyTmp.addPoint(35, 9);
        facingPolyTmp.addPoint(41, 7);
        facingPolyTmp.addPoint(42, 7);
        facingPolyTmp.addPoint(48, 9);
        facingPolyTmp.addPoint(42, 3);

        // create the rotated shapes
        facingPolys = new Shape[8];
        for (int direction : allDirections) {
            facingPolys[direction] = facingRotate.createTransformedShape(facingPolyTmp);
            facingRotate.rotate(Math.toRadians(60), HEX_W / 2.0f, HEX_H / 2.0f);
        }

        // final facing polygons
        Polygon finalFacingPolyTmp = new Polygon();
        finalFacingPolyTmp.addPoint(41, 3);
        finalFacingPolyTmp.addPoint(21, 18);
        finalFacingPolyTmp.addPoint(41, 14);
        finalFacingPolyTmp.addPoint(42, 14);
        finalFacingPolyTmp.addPoint(61, 18);
        finalFacingPolyTmp.addPoint(42, 3);

        // create the rotated shapes
        facingRotate.setToIdentity();
        finalFacingPolys = new Shape[8];
        for (int direction : allDirections) {
            finalFacingPolys[direction] = facingRotate.createTransformedShape(finalFacingPolyTmp);
            facingRotate.rotate(Math.toRadians(60), HEX_W / 2.0f, HEX_H / 2.0f);
        }

        // movement polygons
        Polygon movementPolyTmp = getMovementPolyTmp();

        // create the rotated shapes
        facingRotate.setToIdentity();
        movementPolys = new Shape[8];
        for (int direction : allDirections) {
            movementPolys[direction] = facingRotate.createTransformedShape(movementPolyTmp);
            facingRotate.rotate(Math.toRadians(60), HEX_W / 2.0f, HEX_H / 2.0f);
        }

        // Up and Down Arrows
        facingRotate.setToIdentity();
        facingRotate.translate(0, -31);
        upArrow = facingRotate.createTransformedShape(movementPolyTmp);

        facingRotate.setToIdentity();
        facingRotate.rotate(Math.toRadians(180), HEX_W / 2.0f, HEX_H / 2.0f);
        facingRotate.translate(0, -31);
        downArrow = facingRotate.createTransformedShape(movementPolyTmp);
    }

    private static Polygon getMovementPolyTmp() {
        Polygon movementPolyTmp = new Polygon();
        movementPolyTmp.addPoint(47, 67);
        movementPolyTmp.addPoint(48, 66);
        movementPolyTmp.addPoint(42, 62);
        movementPolyTmp.addPoint(41, 62);
        movementPolyTmp.addPoint(35, 66);
        movementPolyTmp.addPoint(36, 67);

        movementPolyTmp.addPoint(47, 67);
        movementPolyTmp.addPoint(45, 68);
        movementPolyTmp.addPoint(38, 68);
        movementPolyTmp.addPoint(38, 69);
        movementPolyTmp.addPoint(45, 69);
        movementPolyTmp.addPoint(45, 68);

        movementPolyTmp.addPoint(45, 70);
        movementPolyTmp.addPoint(38, 70);
        movementPolyTmp.addPoint(38, 71);
        movementPolyTmp.addPoint(45, 71);
        movementPolyTmp.addPoint(45, 68);
        return movementPolyTmp;
    }

    synchronized boolean doMoveUnits(long idleTime) {
        boolean movingSomething = false;

        if (!movingUnits.isEmpty()) {
            moveWait += idleTime;

            if (moveWait > GUIP.getMoveStepDelay()) {
                ArrayList<MovingUnit> spent = new ArrayList<>();

                for (MovingUnit move : movingUnits) {
                    movingSomething = true;
                    Entity entity = game.getEntity(move.entity.getId());
                    if (!move.path.isEmpty()) {
                        UnitLocation loc = move.path.getFirst();

                        if (entity != null) {
                            redrawMovingEntity(move.entity, loc.coords(), loc.facing(), loc.elevation());
                        }
                        move.path.removeFirst();
                    } else {
                        if (entity != null) {
                            redrawEntity(entity);
                        }
                        spent.add(move);
                    }

                }

                for (MovingUnit move : spent) {
                    movingUnits.remove(move);
                }
                moveWait = 0;

                if (movingUnits.isEmpty()) {
                    movingEntitySpriteIds.clear();
                    movingEntitySprites.clear();
                    ghostEntitySprites.clear();
                    processBoardViewEvent(new BoardViewEvent(this, BoardViewEvent.FINISHED_MOVING_UNITS));
                }
            }
        }
        return movingSomething;
    }

    //
    // MouseListener
    //
    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        boardPanel.requestFocusInWindow();
        stopSoftCentering();
        Point point = mouseEvent.getPoint();

        // Button 4: Hide/Show the minimap and unitDisplay
        if (mouseEvent.getButton() == 4) {
            if (clientgui != null) {
                clientgui.toggleMMUDDisplays();
            }
        }

        // we clicked the right mouse button, remember the position if we start to scroll if we drag, we should scroll
        if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            scrollXDifference = mouseEvent.getX();
            scrollYDifference = mouseEvent.getY();
            shouldScroll = true;
        }

        if (mouseEvent.isPopupTrigger() && !dragging) {
            wantsPopup = true;
            return;
        }

        for (IDisplayable displayable : overlays) {
            double width = scrollPane.getViewport().getSize().getWidth();
            double height = scrollPane.getViewport().getSize().getHeight();
            Dimension dispDimension = new Dimension();
            dispDimension.setSize(width, height);
            // we need to adjust the point, because it should be against the displayable dimension
            Point dispPoint = new Point();
            dispPoint.setLocation(point.x + boardPanel.getBounds().x, point.y + boardPanel.getBounds().y);
            if (displayable.isHit(dispPoint, dispDimension)) {
                return;
            }
        }
        mouseAction(getCoordsAt(point), BOARD_HEX_DRAG, mouseEvent.getModifiersEx(), mouseEvent.getButton());
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        // don't show the popup if we are drag-scrolling
        if ((mouseEvent.isPopupTrigger() || wantsPopup) && !dragging) {
            mouseAction(getCoordsAt(mouseEvent.getPoint()),
                  BOARD_HEX_POPUP,
                  mouseEvent.getModifiersEx(),
                  mouseEvent.getButton());
            // stop scrolling
            shouldScroll = false;
            wantsPopup = false;
            return;
        }

        // if we released the right mouse button, there's no more scrolling
        if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            scrollXDifference = 0;
            scrollYDifference = 0;
            dragging = false;
            shouldScroll = false;
            wantsPopup = false;
            boardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        for (IDisplayable displayable : overlays) {
            if (displayable.isReleased()) {
                return;
            }
        }

        if (mouseEvent.getClickCount() == 1) {
            mouseAction(getCoordsAt(mouseEvent.getPoint()),
                  BOARD_HEX_CLICK,
                  mouseEvent.getModifiersEx(),
                  mouseEvent.getButton());
        } else {
            mouseAction(getCoordsAt(mouseEvent.getPoint()),
                  BOARD_HEX_DOUBLE_CLICK,
                  mouseEvent.getModifiersEx(),
                  mouseEvent.getButton());
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        // Reset the tooltip dismissal delay to the preference value so that elements outside the BoardView can use
        // tooltips
        if (GUIP.getTooltipDismissDelay() >= 0) {
            ToolTipManager.sharedInstance().setDismissDelay(GUIP.getTooltipDismissDelay());
        } else {
            ToolTipManager.sharedInstance().setDismissDelay(dismissDelay);
        }
    }

    @Override
    public void mouseClicked(MouseEvent me) {
    }

    private record MovingUnit(Entity entity, Vector<UnitLocation> path) {
    }

    /**
     * @return True if all hex tile images have been loaded and the board can be successfully drawn
     */
    public boolean isTileImagesLoaded() {
        return tileManager.isLoaded();
    }

    @Override
    public void setUseLosTool(boolean use) {
        useLOSTool = use;
    }

    public TilesetManager getTilesetManager() {
        return tileManager;
    }

    /** The same anonymous contact artwork used by every board renderer. */
    public Image getRadarBlipImage() {
        return radarBlipImage;
    }

    /** Flat terrain and decals; solid feature models are captured separately from the Hex. */
    public record PlanarHex(Coords coords, BufferedImage terrain, BufferedImage normals, BufferedImage decals,
          BufferedImage decalsWithoutLimbs, BufferedImage tactical, List<HexText> text,
          Map<Integer, String> structureModels) { }

    private record DecalArtwork(BufferedImage full, BufferedImage withoutLimbs) { }

    /** Use scrolling text on GPU range contours instead of flat, camera-facing range markers. */
    public static final boolean GPU_SCROLLING_RANGE_LABELS = false;
    private static final int GPU_MARKING_SCALE = 3;
    private BufferedImage planarChunkImage;
    private final AtomicLong planarRevision = new AtomicLong();

    void invalidatePlanarCapture() {
        if (!gpuCapture || !SwingUtilities.isEventDispatchThread()) {
            planarRevision.incrementAndGet();
        }
    }

    public long getPlanarRevision() {
        return planarRevision.get();
    }

    /** Releases capture-only working memory when the native view closes or switches boards. */
    public void releasePlanarCapture() {
        planarChunkImage = null;
        planarHexImageCache.clear();
        groundArtwork.clear();
        featureArtwork.clear();
    }

    /** Immutable navigation intent from the client; a unit request retains its identity even in a stacked hex. */
    public record CenterRequest(long sequence, Coords coords, int entityId) {
        public CenterRequest(long sequence, Coords coords) {
            this(sequence, coords, Entity.NONE);
        }
    }
    private CenterRequest centerRequest = new CenterRequest(0, null);

    public CenterRequest getCenterRequest() {
        return centerRequest;
    }

    /** Screen-anchored board widgets use their existing painters and input handlers in either window. */
    public BufferedImage captureOverlayImage(Dimension size, Dimension pixels) {
        BufferedImage image = new BufferedImage(Math.max(1, pixels.width), Math.max(1, pixels.height),
              BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = overlayGraphics(image, size, pixels);
        try {
            for (IDisplayable overlay : overlays) {
                overlay.draw(graphics, new Rectangle(size));
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public boolean overlayInput(int event, Point point, Dimension size, Dimension pixels) {
        // Drawing establishes widget bounds for this viewport before hit testing.
        if (event != MouseEvent.MOUSE_MOVED) {
            captureOverlayImage(size, pixels);
        }
        for (IDisplayable overlay : overlays) {
            boolean handled = switch (event) {
                case MouseEvent.MOUSE_PRESSED -> overlay.isHit(point, size);
                case MouseEvent.MOUSE_RELEASED -> overlay.isReleased();
                case MouseEvent.MOUSE_DRAGGED -> overlay.isDragged(point, size);
                default -> overlay.isMouseOver(point, size);
            };
            if (handled) {
                return true;
            }
        }
        return false;
    }

    /** Uses the existing tooltip provider with the GPU's picked hex, regardless of classic camera occlusion. */
    public String getHexTooltip(Coords coords) {
        if (coords == null || !getBoard().contains(coords)) {
            return "";
        }
        int originalOffset = verticalOffset;
        try {
            verticalOffset = 0;
            Point point = getCentreHexLocation(coords);
            point.translate(HEX_W, HEX_H);
            return boardViewToolTip.getTooltip(point, movementTarget);
        } finally {
            verticalOffset = originalOffset;
        }
    }

    public BufferedImage captureUnitAnnotations(Entity entity, int part) {
        return captureUnitAnnotations(entity, part, null).image();
    }

    public EntitySprite.Annotations captureUnitAnnotations(Entity entity, int part, EntitySprite.Annotations previous) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Unit annotations must be captured on the Swing event thread");
        }
        float originalScale = scale;
        Dimension originalSize = hex_size;
        try {
            scale = 1;
            hex_size = new Dimension(HEX_W, HEX_H);
            EntitySprite existing = entitySpriteIds.get(getIdAndLoc(entity.getId(), part));
            EntitySprite sprite = new EntitySprite(this, entity, part, radarBlipImage);
            return sprite.captureAnnotations(previous, existing == null ? entity.equals(getSelectedEntity()) : existing.getSelected(),
                  existing != null && existing.isAffectedByECM());
        } finally {
            scale = originalScale;
            hex_size = originalSize;
        }
    }

    public List<PlanarHex> capturePlanarHexes(Rectangle hexArea) {
        List<PlanarHex> result = new ArrayList<>();
        capturePlanarHexes(hexArea, true, hex -> {
            BufferedImage marking = hex.tactical();
            if (marking != null) {
                BufferedImage copy = new BufferedImage(marking.getWidth(), marking.getHeight(), BufferedImage.TYPE_INT_ARGB);
                copy.setData(marking.getData());
                marking = copy;
            }
            result.add(new PlanarHex(hex.coords(), hex.terrain(), hex.normals(), hex.decals(), hex.decalsWithoutLimbs(),
                  marking, hex.text(), hex.structureModels()));
        });
        result.sort(Comparator.comparingInt((PlanarHex hex) -> hex.coords().getX())
              .thenComparingInt(hex -> hex.coords().getY()));
        return result;
    }

    /** The consumer must copy pixels before returning: tactical images borrow the reusable capture buffer. */
    public void capturePlanarHexes(Rectangle hexArea, boolean includeTactical, Consumer<PlanarHex> consumer) {
        capturePlanarHexes(hexArea, true, includeTactical, consumer);
    }

    /** Refreshes markings and text without regenerating unchanged terrain artwork or feature models. */
    public void capturePlanarTactical(Rectangle hexArea, Consumer<PlanarHex> consumer) {
        capturePlanarHexes(hexArea, false, true, consumer);
    }

    private void capturePlanarHexes(Rectangle hexArea, boolean includeArtwork, boolean includeTactical,
          Consumer<PlanarHex> consumer) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Board layers must be captured on the Swing event thread");
        }
        if (gpuTileset == null) {
            gpuTileset = new HexTileset(game, new File(Configuration.dataDir(), "models/board/tileset"));
            try {
                gpuTileset.loadFromFile("saxarba.tileset");
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot load the 3D board's Saxarba tileset", exception);
            }
        }
        if (!tileManager.isStarted()) {
            tileManager.loadNeededImages(game);
        }
        Rectangle area = hexArea.intersection(new Rectangle(0, 0, getBoard().getWidth(), getBoard().getHeight()));
        if (area.isEmpty()) {
            return;
        }
        float originalScale = scale;
        int originalZoom = zoomIndex;
        Dimension originalSize = hex_size;
        ImageCache<Integer, Image> originalScaledCache = scaledImageCache;
        int originalOffset = verticalOffset;
        ImageCache<Coords, HexImageCacheEntry> originalCache = hexImageCache;
        BufferedImage originalShadows = shadowMap;
        boolean originalCapture = gpuCapture;
        Set<Sprite> prepared = new LinkedHashSet<>(allSprites);
        prepared.addAll(pathSprites);
        prepared.addAll(fpiSprites);
        prepared.addAll(attackSprites);
        prepared.addAll(c3Sprites);
        prepared.addAll(movementSprites);
        prepared.addAll(vtolAttackSprites);
        prepared.addAll(flyOverSprites);
        prepared.addAll(ghostEntitySprites);
        prepared.addAll(wreckSprites);
        prepared.addAll(isometricWreckSprites);
        prepared.addAll(List.of(cursorSprite, highlightSprite, selectedSprite, firstLOSSprite, secondLOSSprite));
        try {
            scale = 1;
            zoomIndex = BASE_ZOOM_INDEX;
            updateFontSizes();
            hex_size = new Dimension(HEX_W, HEX_H);
            verticalOffset = 0;
            if (originalScale != 1) {
                scaledImageCache = new ImageCache<>();
            }
            gpuCapture = true;
            shadowMap = null;
            hexImageCache = planarHexImageCache;
            ImageCache<Integer, Image> artworkScaledCache = scaledImageCache;
            // Repaint the shared paths, borders and symbols at their destination
            // resolution. Enlarging an already-captured 84px sprite blurs its edges.
            scale = GPU_MARKING_SCALE;
            hex_size = new Dimension(HEX_W * GPU_MARKING_SCALE, HEX_H * GPU_MARKING_SCALE);
            scaledImageCache = new ImageCache<>();
            font_minefield = font_minefield.deriveFont(font_minefield.getSize2D() * GPU_MARKING_SCALE);
            if (includeTactical) {
                prepared.stream().filter(sprite -> !sprite.isHidden() && !(sprite instanceof IsometricSprite)
                            && !(sprite instanceof TacticalSprite))
                      .filter(sprite -> originalOffset != 0 || originalScale != GPU_MARKING_SCALE || hasMarkerTerrain(sprite))
                      .forEach(Sprite::prepare);
            }
            ImageCache<Integer, Image> markingScaledCache = scaledImageCache;
            for (int column = area.x; column < area.x + area.width; column += 16) {
                for (int row = area.y; row < area.y + area.height; row += 16) {
                    // ImageCache's MAX_SIZE sets its initial capacity, not an eviction limit.
                    // Keep at most two chunks even when the camera shows the entire board.
                    if (planarHexImageCache.size() > 256) {
                        planarHexImageCache.clear();
                    }
                    if (groundArtwork.size() > 256) {
                        groundArtwork.clear();
                        featureArtwork.clear();
                    }
                    Rectangle chunk = new Rectangle(column, row, Math.min(16, area.x + area.width - column),
                          Math.min(16, area.y + area.height - row));
                    scale = 1;
                    hex_size = new Dimension(HEX_W, HEX_H);
                    scaledImageCache = artworkScaledCache;
                    Map<Coords, PlanarHex> artwork = new HashMap<>();
                    for (int x = chunk.x; x < chunk.x + chunk.width; x++) {
                        for (int y = chunk.y; y < chunk.y + chunk.height; y++) {
                            Coords coords = new Coords(x, y);
                            Hex hex = getBoard().getHex(coords);
                            GroundArtwork ground = includeArtwork ? captureGroundArtwork(coords) : null;
                            DecalArtwork decals = includeArtwork ? captureDecals(coords) : null;
                            PlanarHex art = new PlanarHex(coords, ground == null ? null : ground.color(),
                                  ground == null ? null : ground.normal(),
                                  decals == null ? null : decals.full(), decals == null ? null : decals.withoutLimbs(),
                                  null, hexText(coords, hex, getBoard()),
                                  includeArtwork ? structureModels(hex) : Map.of());
                            if (includeTactical) {
                                artwork.put(coords, art);
                            } else {
                                consumer.accept(art);
                            }
                        }
                    }
                    if (includeTactical) {
                        scale = GPU_MARKING_SCALE;
                        hex_size = new Dimension(HEX_W * GPU_MARKING_SCALE, HEX_H * GPU_MARKING_SCALE);
                        scaledImageCache = markingScaledCache;
                        capturePlanarChunk(chunk, artwork, consumer);
                    }
                }
            }
        } finally {
            scale = originalScale;
            zoomIndex = originalZoom;
            updateFontSizes();
            hex_size = originalSize;
            scaledImageCache = originalScaledCache;
            verticalOffset = originalOffset;
            hexImageCache = originalCache;
            shadowMap = originalShadows;
            gpuCapture = originalCapture;
            if (includeTactical) {
                prepared.stream().filter(sprite -> !sprite.isHidden() && !(sprite instanceof IsometricSprite)
                            && !(sprite instanceof TacticalSprite))
                      .filter(sprite -> originalOffset != 0 || originalScale != GPU_MARKING_SCALE || hasMarkerTerrain(sprite))
                      .forEach(Sprite::prepare);
            }
        }
    }

    /** Captures rule results only. The GPU owns their presentation, independently of per-hex raster artwork. */
    public BoardFieldOfView captureFieldOfView(Rectangle requestedArea) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Field of view must be captured on the Swing event thread");
        }
        if (!shouldFovDarken() && !shouldFovHighlight()) {
            return BoardFieldOfView.EMPTY;
        }
        int width = getBoard().getWidth(), height = getBoard().getHeight();
        Rectangle area = new Rectangle(requestedArea);
        area.grow(1, 1);
        area = area.intersection(new Rectangle(0, 0, width, height));
        List<BoardFieldOfView.Hex> hexes = new ArrayList<>(Collections.nCopies(width * height, BoardFieldOfView.Hex.NONE));
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                hexes.set(x * height + y, fovHighlightingAndDarkening.evaluate(new Coords(x, y)));
            }
        }
        BoardFieldOfView result = new BoardFieldOfView(width, height, hexes, GUIP.getFovDarkenAlpha(),
              GUIP.getFovHighlightAlpha(), shouldFovDarken(), GUIP.getFovGrayscale(), GUIP.getFovSpottingMode());
        return result.active() ? result : BoardFieldOfView.EMPTY;
    }

    /** Captures shared tactical shapes without preparing sprites or touching a board-sized bitmap. */
    public BoardTactical captureTacticalGeometry() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Tactical state must be captured on the Swing event thread");
        }
        float originalScale = scale;
        int originalZoom = zoomIndex;
        Dimension originalSize = hex_size;
        int originalOffset = verticalOffset;
        boolean originalCapture = gpuCapture;
        BoardTacticalGraphics graphics = new BoardTacticalGraphics();
        try {
            scale = 1;
            zoomIndex = BASE_ZOOM_INDEX;
            hex_size = new Dimension(HEX_W, HEX_H);
            verticalOffset = 0;
            gpuCapture = true;
            updateFontSizes();
            graphics.setClip(0, 0, getBoard().getWidth() * HEX_WC + HEX_W,
                  getBoard().getHeight() * HEX_H + HEX_H);
            UIUtil.setHighQualityRendering(graphics);
            captureHexOverlays(graphics);
            for (var entry : getBoard().getSpecialHexDisplayTable().entrySet()) {
                List<SpecialHexDisplay> heat = entry.getValue().stream().filter(this::isHeatMapMarker)
                      .filter(marker -> marker.drawNow(game.getPhase(), game.getRoundCount(), getLocalPlayer(), GUIP))
                      .toList();
                if (!heat.isEmpty()) {
                    Graphics2D local = BoardTacticalGraphics.at(graphics, getHexLocation(entry.getKey()));
                    try {
                        BoardTacticalGraphics.draw(local, BoardTactical.Playback.HOLD_DURING_PLAYBACK, layer -> {
                            heat.stream().filter(this::isPredictedHeatMapMarker)
                                  .forEach(marker -> drawHeatMapPredictedHex(marker, layer, 1));
                            drawHeatMapTurnLabel(heat, layer, 1);
                        });
                    } finally {
                        local.dispose();
                    }
                }
            }
            drawSprites(graphics, behindTerrainHexSprites);
            drawTacticalLayers(graphics, false);
            return graphics.snapshot();
        } finally {
            graphics.dispose();
            scale = originalScale;
            zoomIndex = originalZoom;
            hex_size = originalSize;
            verticalOffset = originalOffset;
            gpuCapture = originalCapture;
            updateFontSizes();
        }
    }

    private void capturePlanarChunk(Rectangle area, Map<Coords, PlanarHex> artwork, Consumer<PlanarHex> consumer) {
        Rectangle pixels = new Rectangle(area.x * HEX_WC * GPU_MARKING_SCALE, area.y * HEX_H * GPU_MARKING_SCALE,
              ((area.width - 1) * HEX_WC + HEX_W) * GPU_MARKING_SCALE,
              (area.height * HEX_H + HEX_H / 2) * GPU_MARKING_SCALE);
        if (planarChunkImage == null || planarChunkImage.getWidth() < pixels.width || planarChunkImage.getHeight() < pixels.height) {
            planarChunkImage = new BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage tactical = planarChunkImage;
        Graphics2D graphics = tactical.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, tactical.getWidth(), tactical.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.translate(-pixels.x, -pixels.y);
            graphics.setClip(pixels);
            UIUtil.setHighQualityRendering(graphics);
            // Physical terrain and native geometry are omitted; ECM static, legacy artwork and
            // plugin painters remain in the compatibility layer. No rules are copied here.
            for (int column = area.x; column < area.x + area.width; column++) {
                for (int row = area.y; row < area.y + area.height; row++) {
                    Coords coords = new Coords(column, row);
                    drawHex(coords, graphics, false);
                    if (GUIP.getShowWrecks() && !GpuBoardWindow.modelsEnabled()) {
                        drawIsometricWreckSpritesForHex(coords, graphics, isometricWreckSprites, false);
                        drawIsometricWreckSpritesForHex(coords, graphics, isometricWreckSprites, true);
                    }
                    drawHexSpritesForHex(coords, graphics, behindTerrainHexSprites, false);
                }
            }
            drawTacticalLayers(graphics, false);
        } finally {
            graphics.dispose();
        }
        for (int column = area.x; column < area.x + area.width; column++) {
            for (int row = area.y; row < area.y + area.height; row++) {
                Coords coords = new Coords(column, row);
                Point point = getHexLocation(coords);
                int left = point.x - pixels.x;
                int top = point.y - pixels.y;
                PlanarHex art = artwork.get(coords);
                consumer.accept(new PlanarHex(coords, art.terrain(), art.normals(), art.decals(),
                      art.decalsWithoutLimbs(), markingImage(tactical, left, top), art.text(), art.structureModels()));
            }
        }
    }

    private static BufferedImage markingImage(BufferedImage layer, int x, int y) {
        int width = HEX_W * GPU_MARKING_SCALE, height = HEX_H * GPU_MARKING_SCALE;
        int[] pixels = ((DataBufferInt) layer.getRaster().getDataBuffer()).getData();
        for (int row = y; row < y + height; row++) {
            int start = row * layer.getWidth() + x;
            for (int index = start; index < start + width; index++) {
                if ((pixels[index] >>> 24) != 0) {
                    return layer.getSubimage(x, y, width, height);
                }
            }
        }
        return null;
    }

    private Map<Integer, String> structureModels(Hex hex) {
        Map<Integer, String> models = new HashMap<>();
        if (hex.containsAnyTerrainOf(Terrains.BUILDING, Terrains.FUEL_TANK, Terrains.INDUSTRIAL)) {
            List<Image> images = new ArrayList<>(gpuTileset.getSupers(hex));
            images.add(gpuTileset.getBase(hex));
            for (Image image : images) {
                String source = gpuTileset.imageSource(image).replace('\\', '/');
                int extension = source.lastIndexOf('.');
                if (extension > 0) {
                    String model = "buildings/" + source.substring(0, extension);
                    if (new File(Configuration.dataDir(), "models/board/" + model + ".g3dj").isFile()) {
                        for (int terrain : new int[] { Terrains.BUILDING, Terrains.FUEL_TANK, Terrains.INDUSTRIAL }) {
                            if (hex.containsTerrain(terrain) && gpuTileset.imageHasTerrain(image, terrain)) {
                                models.putIfAbsent(terrain, model);
                            }
                        }
                    }
                }
            }
        }
        return Map.copyOf(models);
    }

    private GroundArtwork captureGroundArtwork(Coords coords) {
        return groundArtwork.computeIfAbsent(coords, key -> {
            Hex ground = game.getBoard(boardId).getHex(key).duplicate();
            ground.removeAllTerrains();
            Hex source = game.getBoard(boardId).getHex(key);
            for (int terrain : GROUND_TERRAINS) {
                if (source.containsTerrain(terrain)) {
                    ground.addTerrain(source.getTerrain(terrain));
                }
            }
            BufferedImage image = new BufferedImage(HEX_W, HEX_H, BufferedImage.TYPE_INT_ARGB);
            BufferedImage normal = new BufferedImage(HEX_W, HEX_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            Graphics2D normalGraphics = normal.createGraphics();
            try {
                UIUtil.setHighQualityRendering(graphics);
                UIUtil.setHighQualityRendering(normalGraphics);
                Image base = gpuTileset.getBase(ground);
                drawBaseTerrain(ground, graphics, base);
                drawBaseTerrain(ground, normalGraphics, groundNormal(base));
                // Select variants once, and use the same layering and large-image crop for their normal maps.
                for (Image overlay : gpuTileset.getSupers(ground)) {
                    if (overlay != null) {
                        graphics.drawImage(getScaledImage(overlay, true), 0, 0, boardPanel);
                        normalGraphics.drawImage(getScaledImage(groundNormal(overlay), true), 0, 0, boardPanel);
                    }
                }
            } finally {
                graphics.dispose();
                normalGraphics.dispose();
                // Filtered hexes are temporary, not board-owned tileset cache keys.
                gpuTileset.clearHex(ground);
            }
            return new GroundArtwork(image, normal);
        });
    }

    /** Normal assets are prepared offline from this exact source image; custom art without a map stays flat. */
    private Image groundNormal(Image artwork) {
        String source = gpuTileset.imageSource(artwork).replace('\\', '/');
        return groundNormals.computeIfAbsent(source, key -> {
            File file = new File(Configuration.dataDir(), "models/board/normals/" + key + ".png");
            if (!key.isEmpty() && file.isFile()) {
                Image normal = ImageUtil.loadImageFromFile(file.toString());
                if (normal != null && normal.getWidth(null) == artwork.getWidth(null)
                      && normal.getHeight(null) == artwork.getHeight(null)) {
                    return normal;
                }
            }
            BufferedImage flat = new BufferedImage(artwork.getWidth(null), artwork.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = flat.createGraphics();
            try {
                graphics.drawImage(artwork, 0, 0, null);
                graphics.setComposite(AlphaComposite.SrcIn);
                graphics.setColor(new Color(128, 128, 255));
                graphics.fillRect(0, 0, flat.getWidth(), flat.getHeight());
            } finally {
                graphics.dispose();
            }
            return flat;
        });
    }

    private DecalArtwork captureDecals(Coords coords) {
        return featureArtwork.computeIfAbsent(coords, key -> {
            Hex flat = game.getBoard(boardId).getHex(key).duplicate();
            for (int terrain : GROUND_TERRAINS) {
                flat.removeTerrain(terrain);
            }
            for (int terrain : MODEL_TERRAINS) {
                flat.removeTerrain(terrain);
            }
            BufferedImage full = drawDecals(flat);
            BufferedImage withoutLimbs = null;
            if (flat.containsAnyTerrainOf(Terrains.ARMS, Terrains.LEGS)) {
                flat.removeTerrain(Terrains.ARMS);
                flat.removeTerrain(Terrains.LEGS);
                withoutLimbs = drawDecals(flat);
            }
            // The GPU chooses the filtered image only after successfully loading the replacement mesh.
            return new DecalArtwork(full, withoutLimbs);
        });
    }

    private BufferedImage drawDecals(Hex flat) {
        BufferedImage image = new BufferedImage(HEX_W, HEX_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            UIUtil.setHighQualityRendering(graphics);
            drawSupers(flat, graphics);
        } finally {
            graphics.dispose();
            gpuTileset.clearHex(flat);
        }
        return image;
    }

    /** Read after overlay capture, on Swing's thread, so hidden or empty unit strips reserve no space. */
    public int sidePanelInset() {
        return unitStripInset(false);
    }

    public int leftPanelInset() {
        return unitStripInset(true);
    }

    private int unitStripInset(boolean left) {
        return overlays.stream().filter(UnitOverviewOverlay.class::isInstance)
              .map(UnitOverviewOverlay.class::cast).filter(strip -> strip.isOnLeft() == left)
              .mapToInt(UnitOverviewOverlay::sidePanelInset).max().orElse(0);
    }

    /** Preserves painter order while keeping cached text and its fade separate for native compositing. */
    public List<OverlayImage> captureOverlayLayers(Dimension size, Dimension pixels) {
        List<OverlayImage> layers = new ArrayList<>();
        Rectangle bounds = new Rectangle(size);
        Graphics2D metrics = overlayGraphics(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), size, pixels);
        BufferedImage pending = null;
        Graphics2D painter = null;
        try {
            for (IDisplayable overlay : overlays) {
                List<OverlayImage> captured = overlay.captureLayers(metrics, bounds);
                if (captured != null) {
                    if (pending != null) {
                        painter.dispose();
                        painter = null;
                        layers.add(new OverlayImage(pending, 0, 0, OverlayImage.Fade.OPAQUE));
                        pending = null;
                    }
                    layers.addAll(captured);
                } else {
                    // Consecutive widgets can share a raster, but must retain their order around faded panels.
                    if (pending == null) {
                        pending = new BufferedImage(Math.max(1, pixels.width), Math.max(1, pixels.height),
                              BufferedImage.TYPE_INT_ARGB);
                        painter = overlayGraphics(pending, size, pixels);
                    }
                    overlay.draw(painter, bounds);
                }
            }
            if (pending != null) {
                layers.add(new OverlayImage(pending, 0, 0, OverlayImage.Fade.OPAQUE));
            }
        } finally {
            metrics.dispose();
            if (painter != null) {
                painter.dispose();
            }
        }
        return List.copyOf(layers);
    }

    private static Graphics2D overlayGraphics(BufferedImage image, Dimension size, Dimension pixels) {
        Graphics2D graphics = image.createGraphics();
        // Layout and hit coordinates stay logical; artwork is rasterized at the display's native density.
        graphics.scale(Math.max(1, pixels.width) / (double) Math.max(1, size.width),
              Math.max(1, pixels.height) / (double) Math.max(1, size.height));
        UIUtil.setHighQualityRendering(graphics);
        return graphics;
    }

    /** Draws the tileset's super images for a hex, which it layers over the base terrain. */
    private void drawSupers(Hex hex, Graphics2D graphics) {
        if (hex == null) {
            return;
        }
        List<Image> supers = gpuTileset.getSupers(hex);
        if (supers == null) {
            return;
        }
        for (Image image : supers) {
            if (image != null) {
                graphics.drawImage(getScaledImage(image, true), 0, 0, boardPanel);
            }
        }
    }

    private static final int[] GROUND_TERRAINS = { Terrains.ROAD, Terrains.ROAD_FLUFF, Terrains.PAVEMENT, Terrains.SAND,
          Terrains.SNOW, Terrains.TUNDRA, Terrains.MUD, Terrains.SWAMP, Terrains.ICE, Terrains.MAGMA, Terrains.FIELDS,
          Terrains.ROUGH, Terrains.RUBBLE };

    /** These have geometry in 3D; painted cliffs and slopes would duplicate the actual faces. */
    private static final int[] MODEL_TERRAINS = { Terrains.WATER, Terrains.WATER_FLUFF, Terrains.RAPIDS, Terrains.HAZARDOUS_LIQUID,
          Terrains.BUILDING, Terrains.BLDG_CF, Terrains.BLDG_ELEV, Terrains.BLDG_FLUFF, Terrains.BLDG_ARMOR,
          Terrains.FUEL_TANK, Terrains.FUEL_TANK_CF, Terrains.FUEL_TANK_ELEV, Terrains.FUEL_TANK_MAGN,
          Terrains.BRIDGE, Terrains.BRIDGE_CF, Terrains.BRIDGE_ELEV, Terrains.BRIDGE_REPAIRED,
          Terrains.WOODS, Terrains.JUNGLE, Terrains.FOLIAGE_ELEV, Terrains.INDUSTRIAL,
          Terrains.CLIFF_TOP, Terrains.CLIFF_BOTTOM, Terrains.INCLINE_TOP, Terrains.INCLINE_BOTTOM,
          Terrains.INCLINE_HIGH_TOP, Terrains.INCLINE_HIGH_BOTTOM };

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
     * @param selected The selected to set.
     */
    public void setSelected(Coords selected) {
        if (this.selected != selected) {
            this.selected = selected;
            checkFoVHexImageCacheClear();
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
     * Determines if this Board contains the Coords, and if so, "selects" that Coords.
     *
     * @param coords the Coords.
     */
    @Override
    public void select(Coords coords) {
        if ((coords == null) || game.getBoard(boardId).contains(coords)) {
            setSelected(coords);
            moveCursor(selectedSprite, coords);
            moveCursor(firstLOSSprite, null);
            moveCursor(secondLOSSprite, null);
            processBoardViewEvent(new BoardViewEvent(this, coords, BoardViewEvent.BOARD_HEX_SELECTED, 0));
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
     * Determines if this Board contains the Coords, and if so, highlights that Coords.
     *
     * @param coords the Coords.
     */
    @Override
    public void highlight(Coords coords) {
        if ((coords == null) || game.getBoard(boardId).contains(coords)) {
            moveCursor(highlightSprite, coords);
            moveCursor(firstLOSSprite, null);
            moveCursor(secondLOSSprite, null);
            processBoardViewEvent(new BoardViewEvent(this, coords, BoardViewEvent.BOARD_HEX_HIGHLIGHTED, 0));
        }
    }

    /**
     * @param color The new colour of the highlight cursor.
     */
    public void setHighlightColor(Color color) {
        highlightSprite.setColor(color);
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

    public synchronized void highlightSelectedEntity(Entity entity) {
        for (EntitySprite sprite : entitySprites) {
            sprite.setSelected(sprite.getEntity().equals(entity));
        }
    }

    /**
     * Highlights multiple entities on the board view. All entities in the provided list will be highlighted. All other
     * entities will be unhighlighted.
     *
     * @param entities List of entities to highlight (can be empty to clear all highlights)
     */
    public synchronized void highlightSelectedEntities(List<Entity> entities) {
        for (EntitySprite sprite : entitySprites) {
            sprite.setSelected(entities.contains(sprite.getEntity()));
        }
    }

    /**
     * Sets the hexes to highlight with white borders (for Nova CEWS network dialog). Draws white hexagon borders around
     * the specified hex coordinates.
     *
     * @param hexes List of hex coordinates to highlight (can be empty to clear all highlights)
     */
    public void setHighlightedEntityHexes(List<Coords> hexes) {
        highlightedEntityHexes = new ArrayList<>(hexes);
        repaint();
    }

    /**
     * Sets the demolition charge hexes to highlight (selected in the Detonate Charges dialog). These are drawn with a
     * bold yellow/black hazard outline when the matching client setting is on, otherwise with the plain highlight.
     *
     * @param hexes List of hex coordinates to highlight (can be empty to clear all highlights)
     */
    public void setDemolitionChargeHighlightHexes(List<Coords> hexes) {
        demolitionChargeHighlightHexes = new ArrayList<>(hexes);
        repaint();
    }

    /**
     * Determines if this Board contains the Coords, and if so, "cursors" that Coords.
     *
     * @param coords the Coords.
     */
    @Override
    public void cursor(Coords coords) {
        if ((coords == null) || game.getBoard(boardId).contains(coords)) {
            if ((getLastCursor() == null) || (coords == null) || !coords.equals(getLastCursor())) {
                setLastCursor(coords);
                moveCursor(cursorSprite, coords);
                moveCursor(firstLOSSprite, null);
                moveCursor(secondLOSSprite, null);
                processBoardViewEvent(new BoardViewEvent(this, coords, BoardViewEvent.BOARD_HEX_CURSOR, 0));
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
        if ((c == null) || game.getBoard(boardId).contains(c)) {
            if (getFirstLOS() == null) {
                setFirstLOS(c);
                firstLOSHex(c);
                processBoardViewEvent(new BoardViewEvent(this, c, BoardViewEvent.BOARD_FIRST_LOS_HEX, 0));
            } else {
                secondLOSHex(c, getFirstLOS());
                processBoardViewEvent(new BoardViewEvent(this, c, BoardViewEvent.BOARD_SECOND_LOS_HEX, 0));
                setFirstLOS(null);
            }
        }
    }

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, notifies listeners about the specified mouse
     * action.
     */
    public void mouseAction(int x, int y, int mouseActionType, int modifiers, int mouseButton) {
        if (game.getBoard(boardId).contains(x, y)) {
            Coords coords = new Coords(x, y);
            switch (mouseActionType) {
                case BOARD_HEX_CLICK:
                    if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
                        checkLOS(coords);
                    } else {
                        processBoardViewEvent(new BoardViewEvent(this,
                              coords,
                              BoardViewEvent.BOARD_HEX_CLICKED,
                              modifiers,
                              mouseButton));
                    }
                    break;
                case BOARD_HEX_DOUBLE_CLICK:
                    processBoardViewEvent(new BoardViewEvent(this,
                          coords,
                          BoardViewEvent.BOARD_HEX_DOUBLE_CLICKED,
                          modifiers,
                          mouseButton));
                    break;
                case BOARD_HEX_DRAG:
                    processBoardViewEvent(new BoardViewEvent(this,
                          coords,
                          BoardViewEvent.BOARD_HEX_DRAGGED,
                          modifiers,
                          mouseButton));
                    break;
                case BOARD_HEX_POPUP:
                    processBoardViewEvent(new BoardViewEvent(this,
                          coords,
                          BoardViewEvent.BOARD_HEX_POPUP,
                          modifiers,
                          mouseButton));
                    break;
            }
        }
    }

    /**
     * Notifies listeners about the specified mouse action.
     *
     * @param coords      - coords the Coords.
     * @param eventType   - Board view event type
     * @param modifiers   - mouse event modifiers mask such as SHIFT_DOWN_MASK etc.
     * @param mouseButton - mouse button associated with this event 0 = no button 1 = Button 1 2 = Button 2
     */
    public void mouseAction(Coords coords, int eventType, int modifiers, int mouseButton) {
        mouseAction(coords.getX(), coords.getY(), eventType, modifiers, mouseButton);
    }

    @Override
    public void boardNewBoard(BoardEvent boardEvent) {
        updateBoard();
        game.getBoard(boardId).initializeAllAutomaticTerrain();
        clearHexImageCache();
        clearShadowMap();
        boardPanel.repaint();
    }

    @Override
    public void boardChangedHex(BoardEvent boardEvent) {
        // A changed blocker can affect LOS far beyond this hex and its immediate neighbors.
        checkFoVHexImageCacheClear();
        Coords coords = boardEvent.getCoords();
        Hex hex = game.getBoard(boardId).getHex(coords);
        // An elevator changes its terrain overlay level, which the isometric view can draw beyond the immediate
        // neighbors. A per-hex cache clear leaves the isometric view stale (it only recovers on a full reload), so
        // for elevator hexes clear the whole hex image cache - the same thing a board reload does.
        boolean hasIndustrialElevator = (hex != null) && hex.containsTerrain(Terrains.INDUSTRIAL_ELEVATOR);
        boolean hasSolarisElevator = (hex != null) && hex.containsTerrain(Terrains.SOLARIS_ELEVATOR);
        if (hasIndustrialElevator || hasSolarisElevator) {
            clearHexImageCache();
        } else {
            hexImageCache.remove(coords);
            // Also repaint the surrounding hexes because of shadows, border etc.
            for (int direction : allDirections) {
                hexImageCache.remove(coords.translated(direction));
            }
        }
        clearShadowMap();
        boardPanel.repaint();
    }

    @Override
    public synchronized void boardChangedAllHexes(BoardEvent boardEvent) {
        clearHexImageCache();
        clearShadowMap();
        boardPanel.repaint();
    }

    synchronized void boardChanged() {
        redrawAllEntities();
    }

    @Override
    public void clearSprites() {
        pathSprites.clear();
        fpiSprites.clear();
        attackSprites.clear();
        c3Sprites.clear();
        vtolAttackSprites.clear();
        flyOverSprites.clear();
        movementSprites.clear();

        overTerrainSprites.clear();
        behindTerrainHexSprites.clear();

        super.clearSprites();
    }

    public synchronized void updateBoard() {
        updateBoardSize();
        redrawAllEntities();
    }

    /**
     * the old redraw worker converted to a runnable which is called now and then from the event thread
     */
    private class RedrawWorker implements Runnable {

        private long lastTime = java.lang.System.currentTimeMillis();

        @Override
        public void run() {
            long currentTime = java.lang.System.currentTimeMillis();

            // The GPU window also presents these overlays while the classic panel is hidden.
            boolean redraw = false;
            for (IDisplayable displayable : overlays) {
                if (!displayable.isSliding()) {
                    displayable.setIdleTime(currentTime - lastTime, true);
                } else {
                    redraw |= displayable.slide();
                }
            }
            if (boardPanel.isShowing()) {
                redraw = redraw || doMoveUnits(currentTime - lastTime);

                if (redraw) {
                    boardPanel.repaint();
                }
                centerOnHexSoftStep(currentTime - lastTime);
            }

            lastTime = currentTime;
        }
    }

    /**
     * @param entity the BoardView's currently selected entity
     */
    public synchronized void selectEntity(Entity entity) {
        checkFoVHexImageCacheClear();
        updateEcmList();
        highlightSelectedEntity(entity);
    }

    /**
     * Updates maps that determine how to shade hexes affected by E(C)CM. This is expensive, so precalculate only when
     * entity changes occur
     **/
    public void updateEcmList() {
        Map<Coords, Color> newECMHexes = new HashMap<>();
        Map<Coords, Color> newECMCenters = new HashMap<>();
        Map<Coords, Color> newECCMHexes = new HashMap<>();
        Map<Coords, Color> newECCMCenters = new HashMap<>();

        // Compute info about all E(C)CM on the board
        final ArrayList<ECMInfo> allEcmInfo = ComputeECM.computeAllEntitiesECMInfo(game.getEntitiesVector());

        // First, mark the sources of E(C)CM Used for highlighting hexes and tooltips
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getPosition() == null || !isOnThisBord(entity)) {
                continue;
            }

            Player localPlayer = getLocalPlayer();
            boolean entityIsEnemy = entity.getOwner().isEnemyOf(localPlayer);

            // If this unit isn't spotted somehow, it's ECM doesn't show up
            if ((localPlayer != null)
                  && game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                  && entityIsEnemy
                  && !entity.hasSeenEntity(localPlayer)
                  && !entity.hasDetectedEntity(localPlayer)) {
                continue;
            }

            // hidden enemy entities don't show their ECM bubble
            if (entityIsEnemy && entity.isHidden()) {
                continue;
            }

            final Color ecmColor = ECMEffects.getECMColor(entity.getOwner());
            // Update ECM center information
            if (entity.getECMInfo() != null) {
                newECMCenters.put(entity.getPosition(), ecmColor);
            }
            // Update ECCM center information
            if (entity.getECCMInfo() != null) {
                newECCMCenters.put(entity.getPosition(), ecmColor);
            }
            // Update Entity sprite's ECM status
            int secondaryIdx = -1;
            if (!entity.getSecondaryPositions().isEmpty()) {
                secondaryIdx = 0;
            }
            EntitySprite entitySprite = entitySpriteIds.get(getIdAndLoc(entity.getId(), secondaryIdx));
            if (entitySprite != null) {
                Coords position = entity.getPosition();
                entitySprite.setAffectedByECM(ComputeECM.isAffectedByECM(entity, position, position, allEcmInfo));
            }
        }

        // Keep track of allied ECM and enemy ECCM
        Map<Coords, ECMEffects> ecmAffectedCoords = new HashMap<>();
        // Keep track of allied ECCM and enemy ECM
        Map<Coords, ECMEffects> eccmAffectedCoords = new HashMap<>();
        for (ECMInfo ecmInfo : allEcmInfo) {
            // Check if ECM source is on this board
            // Entity-based ECM: check if entity is on this board
            // Entity-less ECM (e.g., EMP mines): check if position is valid on this board
            if (ecmInfo.getEntity() != null) {
                if (!isOnThisBord(ecmInfo.getEntity())) {
                    continue;
                }
            } else {
                // Entity-less ECM field (from EMP mines, etc.) - check position is on board
                if (ecmInfo.getPos() == null || !game.getBoard(boardId).contains(ecmInfo.getPos())) {
                    continue;
                }
            }

            // Can't see ECM field of unspotted unit
            Player localPlayer = getLocalPlayer();
            if ((ecmInfo.getEntity() != null) && (localPlayer != null) && game.getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) && ecmInfo.getEntity()
                  .getOwner()
                  .isEnemyOf(localPlayer) && !ecmInfo.getEntity().hasSeenEntity(localPlayer) && !ecmInfo.getEntity()
                  .hasDetectedEntity(localPlayer)) {
                continue;
            }

            // hidden enemy entities don't show their ECM bubble
            if (ecmInfo.getEntity() != null
                  && ecmInfo.getEntity().getOwner().isEnemyOf(localPlayer)
                  && ecmInfo.getEntity().isHidden()) {
                continue;
            }

            final Coords ecmPos = ecmInfo.getPos();
            final int range = ecmInfo.getRange();

            // Add each Coords within range to the list of ECM Coords
            for (int x = -range;
                  x <= range;
                  x++) {
                for (int y = -range;
                      y <= range;
                      y++) {
                    Coords coords = new Coords(x + ecmPos.getX(), y + ecmPos.getY());
                    int distance = ecmPos.distance(coords);
                    int direction = ecmInfo.getDirection();
                    // Direction is the facing of the owning Entity
                    boolean inArc = (direction == -1) || ComputeArc.isInArc(ecmPos,
                          direction,
                          coords,
                          Compute.ARC_NOSE);
                    if ((distance > range) || !inArc) {
                        continue;
                    }

                    // Check for allied ECCM or enemy ECM
                    if ((!ecmInfo.isOpposed(localPlayer) && ecmInfo.isECCM()) || (ecmInfo.isOpposed(localPlayer)
                          && ecmInfo.isECCM())) {
                        ECMEffects ecmEffects = eccmAffectedCoords.computeIfAbsent(coords, k -> new ECMEffects());
                        ecmEffects.addECM(ecmInfo);
                    } else {
                        ECMEffects ecmEffects = ecmAffectedCoords.computeIfAbsent(coords, k -> new ECMEffects());
                        ecmEffects.addECM(ecmInfo);
                    }
                }
            }
        }

        // Finally, determine the color for each affected hex
        for (Coords coords : ecmAffectedCoords.keySet()) {
            ECMEffects ecm = ecmAffectedCoords.get(coords);
            ECMEffects eccm = eccmAffectedCoords.get(coords);
            processAffectedCoords(coords, ecm, eccm, newECMHexes, newECCMHexes);
        }

        for (Coords coords : eccmAffectedCoords.keySet()) {
            ECMEffects ecm = ecmAffectedCoords.get(coords);
            ECMEffects eccm = eccmAffectedCoords.get(coords);
            // Already processed all ECM affected coords

            if (ecm != null) {
                continue;
            }

            processAffectedCoords(coords, null, eccm, newECMHexes, newECCMHexes);
        }

        Set<Coords> updatedHexes = new HashSet<>();
        for (Map<Coords, Color> colors : Arrays.asList(ecmHexes, eccmHexes, ecmCenters, eccmCenters,
              newECMHexes, newECCMHexes, newECMCenters, newECCMCenters)) {
            if (colors != null) {
                updatedHexes.addAll(colors.keySet());
            }
        }
        for (Coords coords : updatedHexes) {
            hexImageCache.remove(coords);
            // Only ECM noise still belongs to the GPU raster cache. Terrain artwork is unaffected.
            if (!Objects.equals(colorAt(ecmHexes, coords), newECMHexes.get(coords))) {
                planarHexImageCache.remove(coords);
            }
        }
        if (!newECMHexes.equals(ecmHexes) || !newECCMHexes.equals(eccmHexes)
              || !newECMCenters.equals(ecmCenters) || !newECCMCenters.equals(eccmCenters)) {
            checkFoVHexImageCacheClear();
            invalidatePlanarCapture();
        }

        synchronized (this) {
            ecmHexes = newECMHexes;
            ecmCenters = newECMCenters;
            eccmHexes = newECCMHexes;
            eccmCenters = newECCMCenters;
        }

        boardPanel.repaint();
    }

    private void processAffectedCoords(Coords coords, ECMEffects ecm, ECMEffects eccm, Map<Coords, Color> newECMHexes,
          Map<Coords, Color> newECCMHexes) {
        Color hexColorECM = null;

        if (ecm != null) {
            hexColorECM = ecm.getHexColor();
        }

        Color hexColorECCM = null;

        if (eccm != null) {
            hexColorECCM = eccm.getHexColor();
        }

        // Hex color is null if all effects cancel out
        if ((hexColorECM == null) && (hexColorECCM == null)) {
            return;
        }

        if ((hexColorECM != null) && (hexColorECCM == null)) {
            if (ecm.isECCM()) {
                newECCMHexes.put(coords, hexColorECM);
            } else {
                newECMHexes.put(coords, hexColorECM);
            }
        } else if (hexColorECM == null) {
            if (eccm.isECCM()) {
                newECCMHexes.put(coords, hexColorECCM);
            } else {
                newECMHexes.put(coords, hexColorECCM);
            }
        } else { // Both are non-null
            newECMHexes.put(coords, hexColorECM);
            newECCMHexes.put(coords, hexColorECCM);
        }
    }

    /**
     * Have the player select an Entity from the entities at the given coords.
     *
     * @param position - the <code>Coords</code> containing targets.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    private Entity chooseEntity(Coords position) {
        // Assume that we have *no* choice.
        Entity choice = null;

        // Get the available choices.
        List<Entity> entities = game.getEntitiesVector(position);

        // Do we have a single choice?
        if (entities.size() == 1) {
            // Return that choice.
            choice = entities.getFirst();
        } else if (entities.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            choice = EntityChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                  "BoardView1.ChooseEntityDialog.title",
                  Messages.getString("BoardView1.ChooseEntityDialog.message", position.getBoardNum()),
                  entities);
        }

        // Return the chosen unit.
        return choice;
    }

    @Override
    public Component getComponent() {
        return getComponent(false);
    }

    @Override
    public void setDisplayInvalidFields(boolean displayInvalidFields) {
        displayInvalidHexInfo = displayInvalidFields;
    }

    @Override
    public void setLocalPlayer(int playerId) {
        setLocalPlayer(game.getPlayer(playerId));
    }

    public Component getComponent(boolean scrollBars) {
        // If we're already configured, return the ScrollPane
        if (scrollPane != null) {
            return scrollPane;
        }

        SkinSpecification bvSkinSpec = SkinXMLHandler.getSkin(UIComponents.BoardView.getComp());

        // Setup background icons
        try {
            File file;

            if (!bvSkinSpec.backgrounds.isEmpty()) {
                file = new MegaMekFile(Configuration.widgetsDir(), bvSkinSpec.backgrounds.getFirst()).getFile();
                if (!file.exists()) {
                    LOGGER.error("BoardView1 Error: Background 0 icon doesn't exist: {}", file.getAbsolutePath());
                } else {
                    bvBgImage = (BufferedImage) ImageUtil.loadImageFromFile(file.getAbsolutePath());
                    bvBgShouldTile = bvSkinSpec.tileBackground;
                }
            }

            if (bvSkinSpec.backgrounds.size() > 1) {
                file = new MegaMekFile(Configuration.widgetsDir(), bvSkinSpec.backgrounds.get(1)).getFile();
                if (!file.exists()) {
                    LOGGER.error("BoardView1 Error: Background 1 icon doesn't exist: {}", file.getAbsolutePath());
                } else {
                    scrollPaneBgImg = ImageUtil.loadImageFromFile(file.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Error loading BoardView background images!");
        }

        // Place the board viewer in a set of scrollbars.
        scrollPane = new JScrollPane(boardPanel) {
            @Override
            protected void paintComponent(Graphics graphics) {
                if (scrollPaneBgImg == null) {
                    super.paintComponent(graphics);
                    return;
                }

                int w = getWidth();
                int h = getHeight();
                int iW = scrollPaneBgImg.getWidth(null);
                int iH = scrollPaneBgImg.getHeight(null);

                if ((scrollPaneBgBuffer == null)
                      || (scrollPaneBgBuffer.getWidth() != w)
                      || (scrollPaneBgBuffer.getHeight() != h)) {
                    scrollPaneBgBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    Graphics bgGraph = scrollPaneBgBuffer.getGraphics();
                    // If the unit icon not loaded, prevent infinite loop

                    if ((iW < 1) || (iH < 1)) {
                        return;
                    }

                    for (int x = 0;
                          x < w;
                          x += iW) {
                        for (int y = 0;
                              y < h;
                              y += iH) {
                            bgGraph.drawImage(scrollPaneBgImg, x, y, null);
                        }
                    }

                    bgGraph.dispose();
                }
                graphics.drawImage(scrollPaneBgBuffer, 0, 0, null);
            }
        };
        scrollPane.setBorder(new MegaMekBorder(bvSkinSpec));
        scrollPane.setLayout(new ScrollPaneLayout());
        // we need to use the simple scroll mode because otherwise the IDisplayables that are drawn in fixed
        // positions in the viewport leave artifacts when scrolling
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        // Prevent the default arrow key scrolling
        scrollPane.getActionMap().put("unitScrollRight", DoNothing);
        scrollPane.getActionMap().put("unitScrollDown", DoNothing);
        scrollPane.getActionMap().put("unitScrollLeft", DoNothing);
        scrollPane.getActionMap().put("unitScrollUp", DoNothing);

        verticalBar = scrollPane.getVerticalScrollBar();
        horizontalBar = scrollPane.getHorizontalScrollBar();

        if (!scrollBars && !bvSkinSpec.showScrollBars) {
            verticalBar.setPreferredSize(new Dimension(0, verticalBar.getHeight()));
            horizontalBar.setPreferredSize(new Dimension(horizontalBar.getWidth(), 0));
        }

        return scrollPane;
    }

    /** Drop the inactive Swing viewport and its artwork; shared game interaction remains owned by this board adapter. */
    public void releaseClassicView() {
        stopSoftCentering();
        if (scrollPane != null) {
            scrollPane.setViewportView(null);
            scrollPane = null;
        }
        verticalBar = null;
        horizontalBar = null;
        scrollPaneBgBuffer = null;
        scrollPaneBgImg = null;
        bvBgImage = null;
        shadowMap = null;
        hexImageCache.clear();
    }

    AbstractAction DoNothing = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {

        }
    };

    private void pingMinimap() {
        // send the minimap a hex moused event to make it update the visible area rectangle
        BoardViewEvent bve = new BoardViewEvent(this, BoardViewEvent.BOARD_HEX_DRAGGED);
        for (BoardViewListener l : boardViewListeners) {
            l.hexMoused(bve);
        }
    }

    public void showPopup(JPopupMenu popUp, Coords coords) {
        Point p = getHexLocation(coords);
        p.x += ((int) (HEX_WC * scale) - scrollPane.getX()) + HEX_W;
        p.y += ((int) ((HEX_H * scale) / 2) - scrollPane.getY()) + HEX_H;

        if (popUp.getParent() == null) {
            boardPanel.add(popUp);
        }

        popUp.show(boardPanel, p.x, p.y);
    }

    @Override
    public void zoomIn() {
        if (zoomIndex == (ZOOM_FACTORS.length - 1)) {
            return;
        }

        zoomIndex++;
        zoom();
    }

    @Override
    public void zoomOut() {
        if (zoomIndex == 0) {
            return;
        }

        zoomIndex--;
        zoom();
    }

    @Override
    public void zoomOverviewToggle() {
        if (!zoomOverview) {
            preZoomOverviewIndex = zoomIndex;
            preZoomOverviewViewX = scrollPane.getHorizontalScrollBar().getValue();
            preZoomOverviewViewY = scrollPane.getVerticalScrollBar().getValue();

            for (int i = ZOOM_FACTORS.length - 1;
                  i > 0;
                  i--) {
                if (getComponent().getWidth() / getComponent().getHeight() < 1) {
                    if (((boardSize.width / ZOOM_FACTORS[zoomIndex]) + HEX_W * 2) * ZOOM_FACTORS[i]
                          < getComponent().getWidth()) {
                        bestZoomFactor = i;
                        break;
                    }
                } else {
                    if (((boardSize.height / ZOOM_FACTORS[zoomIndex]) + HEX_H * 2) * ZOOM_FACTORS[i]
                          < getComponent().getHeight()) {
                        bestZoomFactor = i;
                        break;
                    }
                }
                bestZoomFactor = 0;
            }
            zoomIndex = bestZoomFactor;
            zoomOverview = true;
            zoom();
        } else {
            zoomIndex = preZoomOverviewIndex;
            zoomOverview = false;
            zoom();
            scrollPane.getHorizontalScrollBar().setValue(preZoomOverviewViewX);
            scrollPane.getVerticalScrollBar().setValue(preZoomOverviewViewY);
        }
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
        Point dispPoint;
        double inHexDeltaX = 0;
        double inHexDeltaY = 0;
        Coords zoomCenter;

        try { // try to get mouse position and zoom centered on the cursor
            Point mouseP = new Point(boardPanel.getMousePosition());
            dispPoint = new Point(mouseP.x + boardPanel.getBounds().x, mouseP.y + boardPanel.getBounds().y);
            zoomCenter = new Coords(getCoordsAt(boardPanel.getMousePosition()));
            Point hexL = getCentreHexLocation(zoomCenter);
            Point inHexDelta = new Point(boardPanel.getMousePosition());
            inHexDelta.translate(-HEX_W, -HEX_H);
            inHexDelta.translate(-hexL.x, -hexL.y);
            inHexDeltaX = ((double) inHexDelta.x) / ((double) HEX_W) / scale;
            inHexDeltaY = ((double) inHexDelta.y) / ((double) HEX_H) / scale;

        } catch (Exception e) { // zoom on view center, if mouse position is outside the map
            Point viewCenter = new Point(boardPanel.getVisibleRect().getLocation().x
                  + boardPanel.getVisibleRect().width / 2,
                  boardPanel.getVisibleRect().getLocation().y + boardPanel.getVisibleRect().height / 2);
            dispPoint = new Point(viewCenter.x + boardPanel.getBounds().x, viewCenter.y + boardPanel.getBounds().y);
            zoomCenter = new Coords(getCoordsAt(viewCenter));
        }

        checkZoomIndex();
        stopSoftCentering();
        scale = ZOOM_FACTORS[zoomIndex];
        GUIP.setMapZoomIndex(zoomIndex);

        hex_size = new Dimension((int) (HEX_W * scale), (int) (HEX_H * scale));

        scaledImageCache = new ImageCache<>();

        cursorSprite.prepare();
        highlightSprite.prepare();
        selectedSprite.prepare();
        firstLOSSprite.prepare();
        secondLOSSprite.prepare();

        allSprites.forEach(Sprite::prepare);

        updateFontSizes();
        updateBoard();

        for (StepSprite sprite : pathSprites) {
            sprite.refreshZoomLevel();
        }

        for (FlightPathIndicatorSprite sprite : fpiSprites) {
            sprite.prepare();
        }

        boardPanel.setSize(boardSize);
        clearHexImageCache();

        adjustVisiblePosition(zoomCenter, dispPoint, inHexDeltaX, inHexDeltaY);

        if (zoomIndex != bestZoomFactor) {
            zoomOverview = false;
        }
    }

    private void updateFontSizes() {
        if (zoomIndex < 7) {
            font_elev = FONT_7;
            font_hexNumber = FONT_7;
            font_minefield = FONT_7;
        } else if ((zoomIndex < 8)) {
            font_elev = FONT_10;
            font_hexNumber = FONT_10;
            font_minefield = FONT_10;
        } else if ((zoomIndex < 10)) {
            font_elev = FONT_12;
            font_hexNumber = FONT_12;
            font_minefield = FONT_12;
        } else if ((zoomIndex < 11)) {
            font_elev = FONT_14;
            font_hexNumber = FONT_14;
            font_minefield = FONT_14;
        } else if (zoomIndex < 12) {
            font_elev = FONT_16;
            font_hexNumber = FONT_16;
            font_minefield = FONT_16;
        } else if (zoomIndex < 13) {
            font_elev = FONT_18;
            font_hexNumber = FONT_18;
            font_minefield = FONT_18;
        } else {
            font_elev = FONT_24;
            font_hexNumber = FONT_24;
            font_minefield = FONT_24;
        }
    }

    /**
     * Return a scaled version of the input. If the useCache flag is set, the scaled image will be stored in an image
     * cache for later retrieval.
     *
     * @param base     The image to get a scaled copy of. The current zoom level is used to determine the scale.
     * @param useCache This flag determines whether the scaled image should be stored in a cache for later retrieval.
     */
    @Nullable
    public Image getScaledImage(Image base, boolean useCache) {
        if (base == null) {
            return null;
        }

        // Captures can use a different raster scale from the classic view's zoom setting.
        if (scale == 1) {
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
            MediaTracker tracker = new MediaTracker(boardPanel);
            if ((base.getWidth(null) == -1) || (base.getHeight(null) == -1)) {
                tracker.addImage(base, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException e) {
                    LOGGER.error(e, "");
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
                LOGGER.error(e, "");
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
    private Image scale(Image image, int width, int height) {
        return ImageUtil.getScaledImage(image, width, height, ZOOM_SCALE_TYPES[zoomIndex]);
    }

    public void toggleIsometric() {
        setVerticalOffset();
        allSprites.forEach(Sprite::prepare);
        clearHexImageCache();
        updateBoard();
        repaint();
    }

    public void updateEntityLabels() {
        for (Entity entity : game.getEntitiesVector()) {
            entity.generateShortName();
        }

        for (EntitySprite entitySprite : entitySprites) {
            entitySprite.prepare();
        }

        for (IsometricSprite isometricSprite : isometricSprites) {
            isometricSprite.prepare();
        }
        boardPanel.repaint();
    }

    public BufferedImage createShadowMask(Image image) {
        int hashCode = image.hashCode();
        BufferedImage mask = shadowImageCache.get(hashCode);
        if (mask != null) {
            return mask;
        }
        mask = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        float opacity = 0.4f;
        Graphics2D graphics2D = mask.createGraphics();
        graphics2D.drawImage(image, 0, 0, null);
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, opacity));
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        graphics2D.dispose();
        shadowImageCache.put(hashCode, mask);
        return mask;
    }

    /**
     * @return Returns true if the BoardView has an active chatter box else false.
     */
    public boolean getChatterBoxActive() {
        return chatterBoxActive;
    }

    /**
     * @param chatterBoxActive whether the BoardView has an active chatter box or not.
     */
    public void setChatterBoxActive(boolean chatterBoxActive) {
        this.chatterBoxActive = chatterBoxActive;
    }

    public void setShouldIgnoreKeys(boolean shouldIgnoreKeys) {
        this.shouldIgnoreKeys = shouldIgnoreKeys;
    }

    public void clearHexImageCache() {
        if (fovHighlightingAndDarkening != null) {
            fovHighlightingAndDarkening.invalidate();
        }
        invalidatePlanarCapture();
        hexImageCache.clear();
        planarHexImageCache.clear();
        groundArtwork.clear();
        featureArtwork.clear();
    }

    /**
     * Clear a specific list of Coords from the hex image cache.
     *
     * @param setCoords Set of {@link Coords} to remove
     */
    public void clearHexImageCache(Set<Coords> setCoords) {
        if (fovHighlightingAndDarkening != null) {
            fovHighlightingAndDarkening.invalidate();
        }
        invalidatePlanarCapture();
        for (Coords coords : setCoords) {
            hexImageCache.remove(coords);
            planarHexImageCache.remove(coords);
            groundArtwork.remove(coords);
            featureArtwork.remove(coords);
        }
    }

    /**
     * Check to see if the HexImageCache should be cleared because of field-of-view changes.
     */
    public void checkFoVHexImageCacheClear() {
        boolean darken = shouldFovDarken();
        boolean highlight = shouldFovHighlight();
        if (darken || highlight) {
            if (fovHighlightingAndDarkening != null) {
                fovHighlightingAndDarkening.invalidate();
            }
            // GPU raster tiles no longer contain FoV. Only classic pixels and the presentation snapshot change.
            hexImageCache.clear();
            invalidatePlanarCapture();
        }
    }

    public static Polygon getHexPoly() {
        return HEX_POLY;
    }

    /**
     * Displays a dialog and changes the theme of all board hexes to the user-chosen theme.
     */
    public @Nullable String changeTheme() {
        if (game == null) {
            return null;
        }
        Board board = game.getBoard(boardId);
        if (board.isSpace()) {
            return null;
        }

        Set<String> themes = tileManager.getThemes();

        if (themes.remove("")) {
            themes.add("(No Theme)");
        }

        themes.add("(Original Theme)");

        setShouldIgnoreKeys(true);
        selectedTheme = (String) JOptionPane.showInputDialog(null,
              "Choose the desired theme:",
              "Theme Selection",
              JOptionPane.PLAIN_MESSAGE,
              null,
              themes.toArray(),
              selectedTheme);
        setShouldIgnoreKeys(false);

        if (selectedTheme == null) {
            return null;
        } else if (selectedTheme.equals("(Original Theme)")) {
            selectedTheme = null;
        } else if (selectedTheme.equals("(No Theme)")) {
            selectedTheme = "";
        }

        board.setTheme(selectedTheme);
        return selectedTheme;
    }

    public Rectangle getDisplayablesRect() {
        return displayablesRect;
    }

    public boolean shouldFovHighlight() {
        return GUIP.getFovHighlight() && !(game.getPhase().isReport());
    }

    public boolean shouldFovDarken() {
        return GUIP.getFovDarken() && !(game.getPhase().isReport());
    }

    public void setShowLobbyPlayerDeployment(boolean showLobbyPlayerDeployment) {
        this.showLobbyPlayerDeployment = showLobbyPlayerDeployment;
    }

    @Override
    public JPanel getPanel() {
        return boardPanel;
    }

    @Override
    public Dimension getBoardSize() {
        return boardSize;
    }

    @Override
    public Set<Integer> getAnimatedImages() {
        return animatedImages;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle rectangle, int arg1, int arg2) {
        return (int) (scale / 2.0) * ((arg1 == SwingConstants.VERTICAL) ? HEX_H : HEX_W);
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle rectangle, int arg1, int arg2) {
        Dimension size = scrollPane.getViewport().getSize();
        return (arg1 == SwingConstants.VERTICAL) ? size.height : size.width;
    }

    @Override
    public void dispose() {
        // The native window belongs to the client, so replacing a map must not close it.
        if (getClientgui() == null) {
            GpuBoardWindow.closeFor(this);
        }
        overlays.stream().filter(UnitOverviewOverlay.class::isInstance).map(UnitOverviewOverlay.class::cast)
              .forEach(GUIP::removePreferenceChangeListener);
        super.dispose();
        redrawTimerTask.cancel();
        fovHighlightingAndDarkening.die();
        KeyBindParser.removePreferenceChangeListener(this);
        GUIP.removePreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().removePreferenceChangeListener(this);
    }

    /** @return The TurnDetailsOverlay if this BoardView has one. */
    @Nullable
    public TurnDetailsOverlay getTurnDetailsOverlay() {
        return (TurnDetailsOverlay) overlays.stream()
              .filter(o -> o instanceof TurnDetailsOverlay)
              .findFirst()
              .orElse(null);
    }

    /**
     * @return The unit currently shown in the Unit Display. Note: This can be a unit than the one that is selected to
     *       move or fire.
     */
    @Nullable
    public Entity getSelectedEntity() {
        return clientgui != null ? clientgui.getDisplayedUnit() : null;
    }

    public FovHighlightingAndDarkening getFovHighlighting() {
        return fovHighlightingAndDarkening;
    }

    public ArrayList<IsometricWreckSprite> getIsoWreckSprites() {
        return isometricWreckSprites;
    }

    public ArrayList<AttackSprite> getAttackSprites() {
        return attackSprites;
    }

    private static boolean hasNativeVolume(Sprite sprite) {
        return sprite instanceof AttackSprite || sprite instanceof CollapseWarningSprite
              || sprite instanceof GroundObjectSprite || sprite instanceof FlareSprite
              || sprite instanceof HexFlagSprite || sprite instanceof SawClearingSprite
              || sprite instanceof BridgeRepairedSprite
              || sprite instanceof FieldOfFireSprite field && field.isWeaponRange()
              || sprite instanceof TextMarkerSprite text && text.isWeaponRange();
    }

    private static boolean hasMarkerTerrain(Sprite sprite) {
        return sprite instanceof BridgeBuildSprite || sprite instanceof FortifyBuildSprite
              || sprite instanceof RubbleClearSprite || sprite instanceof DugInSprite;
    }

    /** Used by mixed terrain/status sprites to leave only their terrain artwork in the planar capture. */
    public boolean isGpuCapture() {
        return gpuCapture;
    }

    public BoardMarker boardMarker(BoardMarker.Kind kind, Coords coords, String label) {
        return boardMarker(kind, coords, kind.rgb(), label);
    }

    public BoardMarker boardMarker(BoardMarker.Kind kind, Coords coords, int rgb, String label) {
        Hex hex = coords == null ? null : getBoard().getHex(coords);
        return new BoardMarker(kind, coords, hex == null ? 0 : hex.ceiling(), rgb, label);
    }

    /** Swing-only snapshot of the existing visible handlers, local attacks, and special-hex display rules. */
    public List<BoardMarker> getBoardMarkers() {
        List<BoardMarker> result = new ArrayList<>();
        allSprites.stream().filter(sprite -> !sprite.isHidden()).map(Sprite::boardMarker)
              .filter(Objects::nonNull).forEach(result::add);
        result.addAll(minefieldMarkers());
        result.addAll(demolitionMarkers());
        result.addAll(artilleryMarkers());
        for (Enumeration<OrbitalBombardment> attacks = game.getOrbitalBombardmentAttacks(); attacks.hasMoreElements();) {
            OrbitalBombardment attack = attacks.nextElement();
            result.add(boardMarker(BoardMarker.Kind.ORBITAL_INCOMING, new Coords(attack.getX(), attack.getY()), ""));
        }
        getBoard().getSpecialHexDisplayTable().forEach((coords, displays) -> {
            for (SpecialHexDisplay display : displays) {
                BoardMarker.Kind kind = pointMarkerKind(display);
                if (kind != null && display.drawNow(game.getPhase(), game.getRoundCount(), getLocalPlayer(), GUIP)) {
                    result.add(boardMarker(kind, coords, ""));
                }
            }
        });
        return result.stream().filter(marker -> marker.coords() != null && getBoard().contains(marker.coords()))
              .distinct().sorted(Comparator.comparingInt((BoardMarker marker) -> marker.coords().getX())
                    .thenComparingInt(marker -> marker.coords().getY()).thenComparing(BoardMarker::kind)
                    .thenComparing(BoardMarker::label).thenComparingInt(BoardMarker::rgb)).toList();
    }

    private BoardMarker.Kind pointMarkerKind(SpecialHexDisplay display) {
        if (isHeatMapMarker(display)) {
            return null;
        }
        return switch (display.getType()) {
            case ARTILLERY_AUTO_HIT -> BoardMarker.Kind.ARTILLERY_AUTO_HIT;
            case ARTILLERY_ADJUSTED -> BoardMarker.Kind.ARTILLERY_ADJUSTED;
            case ARTILLERY_INCOMING -> BoardMarker.Kind.ARTILLERY_INCOMING;
            case ARTILLERY_TARGET -> BoardMarker.Kind.ARTILLERY_TARGET;
            case NUKE_INCOMING -> BoardMarker.Kind.NUKE_INCOMING;
            case ORBITAL_BOMBARDMENT_INCOMING -> BoardMarker.Kind.ORBITAL_INCOMING;
            case PLAYER_NOTE -> BoardMarker.Kind.PLAYER_NOTE;
            default -> null;
        };
    }

    /** Existing handler output, including its arc, range and preference filtering. Swing thread only. */
    public List<FieldOfFireSprite> getWeaponRangeSprites() {
        return allSprites.stream().filter(FieldOfFireSprite.class::isInstance).map(FieldOfFireSprite.class::cast)
              .filter(sprite -> sprite.isWeaponRange() && !sprite.isHidden()).toList();
    }

    /** Preserve the handler's visible label positions; the GPU only changes their orientation. */
    public List<TextMarkerSprite> getWeaponRangeTextSprites() {
        return GPU_SCROLLING_RANGE_LABELS ? List.of() : allSprites.stream()
              .filter(TextMarkerSprite.class::isInstance).map(TextMarkerSprite.class::cast)
              .filter(sprite -> sprite.isWeaponRange() && !sprite.isHidden()).toList();
    }

    @Override
    public void repaint() {
        boardPanel.repaint();
    }

    @Override
    public void addSprites(Collection<? extends Sprite> sprites) {
        super.addSprites(sprites);
        sprites.stream()
              .filter(s -> !(s instanceof HexSprite hexSprite) || !hexSprite.isBehindTerrain())
              .forEach(overTerrainSprites::add);
        sprites.stream()
              .filter(s -> s instanceof HexSprite)
              .map(s -> (HexSprite) s)
              .filter(HexSprite::isBehindTerrain)
              .forEach(behindTerrainHexSprites::add);
    }

    @Override
    public void removeSprites(Collection<? extends Sprite> sprites) {
        super.removeSprites(sprites);
        overTerrainSprites.removeAll(sprites);

        for (Sprite sprite : sprites) {
            if (sprite instanceof HexSprite hexSprite) {
                behindTerrainHexSprites.remove(hexSprite);
            }
        }
    }

    /**
     * @return This BoardView's displayed board.
     */
    public Board getBoard() {
        return game.getBoard(boardId);
    }

    /**
     * @return True when the given Targetable is not null and is on this board as given by its boardId. Does *not*
     *       require the targetable to have a non-null position or a position contained within the board, nor is the
     *       targetable's deployment status checked.
     */
    public boolean isOnThisBord(@Nullable Targetable targetable) {
        return (targetable != null) && targetable.getBoardId() == boardId;
    }

    /**
     * @return True when the given boardLocation is on this board according to its boardId only. Does *not* test the
     *       position of the boardLocation.
     *
     * @see BoardLocation#isOn(int)
     */
    @SuppressWarnings("unused")
    public boolean isOnThisBord(BoardLocation boardLocation) {
        return boardLocation.isOn(boardId);
    }

    /** Capture only affected hexes, in stable order, using the same painters as the classic board. */
    private void captureHexOverlays(BoardTacticalGraphics graphics) {
        Board board = getBoard();
        Set<Coords> marked = new TreeSet<>(Comparator.comparingInt(Coords::getX).thenComparingInt(Coords::getY));
        marked.addAll(board.embeddedBoardCoords());
        for (Map<Coords, Color> colors : Arrays.asList(ecmHexes, eccmHexes, ecmCenters, eccmCenters)) {
            if (colors != null) {
                marked.addAll(colors.keySet());
            }
        }
        if (GUIP.getShowMapSheets()) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    if (x % 16 == 0 || x % 16 == 15 || y % 17 == 0 || y % 17 == 16) {
                        marked.add(new Coords(x, y));
                    }
                }
            }
        }
        marked.removeIf(coords -> !board.contains(coords));
        for (Coords coords : marked) {
            Graphics2D local = BoardTacticalGraphics.at(graphics, getHexLocation(coords));
            try {
                BoardTacticalGraphics.draw(local, BoardTactical.Playback.HOLD_DURING_PLAYBACK,
                      layer -> drawElectronicWarfare(layer, coords));
                if (board.embeddedBoardCoords().contains(coords)) {
                    drawEmbeddedBoard(local);
                }
            } finally {
                local.dispose();
            }
        }
        if (GUIP.getShowMapSheets()) {
            // Shared edges cross into both hexes; all coverage fills must be below the borders.
            for (Coords coords : marked) {
                Graphics2D local = BoardTacticalGraphics.at(graphics, getHexLocation(coords));
                try {
                    drawMapSheetBorders(local, coords);
                } finally {
                    local.dispose();
                }
            }
        }
    }

    private static Color colorAt(Map<Coords, Color> colors, Coords coords) {
        return colors == null ? null : colors.get(coords);
    }

    /** Shared authoritative colors; only the static texture remains in native raster capture. */
    private void drawElectronicWarfare(Graphics2D graphics, Coords coords) {
        Graphics2D local = (Graphics2D) graphics.create();
        try {
            boolean vectors = local instanceof BoardTacticalGraphics;
            boolean solids = !gpuCapture || vectors;
            Color ecm = colorAt(ecmHexes, coords);
            if (ecm != null) {
                if (solids) {
                    drawFieldTint(local, ecm);
                }
                if (!vectors) {
                    Image noise = getScaledImage(tileManager.getEcmStaticImage(ecm), false);
                    local.drawImage(noise, 0, 0, noise.getWidth(null), noise.getHeight(null), boardPanel);
                }
            }
            if (solids) {
                drawFieldTint(local, colorAt(eccmHexes, coords));
                drawFieldSource(local, colorAt(ecmCenters, coords));
                drawFieldSource(local, colorAt(eccmCenters, coords));
            }
        } finally {
            local.dispose();
        }
    }

    private void drawFieldTint(Graphics2D graphics, Color tint) {
        if (tint != null) {
            graphics.setColor(tint);
            // Use the full shared hex in native geometry, avoiding the classic bitmap's one-pixel gutters.
            Shape hex = graphics instanceof BoardTacticalGraphics ? HexDrawUtilities.getHexFullBorderLine(0) : HEX_POLY;
            graphics.fill(AffineTransform.getScaleInstance(scale, scale).createTransformedShape(hex));
        }
    }

    private void drawFieldSource(Graphics2D graphics, Color tint) {
        if (tint == null) {
            return;
        }
        if (graphics instanceof BoardTacticalGraphics) {
            int alpha = Math.min(255, tint.getAlpha() * 2);
            drawHexBorder(graphics, new Color(0, 0, 0, Math.min(160, alpha)), 4, 6);
            drawHexBorder(graphics, new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), alpha), 5, 4);
        } else {
            drawHexBorder(graphics, tint.darker(), 5, 10);
        }
    }

    private void drawMapSheetBorders(Graphics2D graphics, Coords coords) {
        if (!GUIP.getShowMapSheets()) {
            return;
        }
        int borders = 0;
        if (coords.getX() % 16 == 0) {
            borders |= (1 << 4) | (1 << 5);
        } else if (coords.getX() % 16 == 15) {
            borders |= (1 << 1) | (1 << 2);
        }
        if (coords.getY() % 17 == 0) {
            borders |= 1;
            if (coords.getX() % 2 == 0) {
                borders |= (1 << 1) | (1 << 5);
            }
        } else if (coords.getY() % 17 == 16) {
            borders |= 1 << 3;
            if (coords.getX() % 2 == 1) {
                borders |= (1 << 2) | (1 << 4);
            }
        }
        if (borders == 0) {
            return;
        }
        Path2D path = new Path2D.Double();
        for (int direction = 0; direction < 6; direction++) {
            if ((borders & (1 << direction)) != 0) {
                path.append(HexDrawUtilities.getHexBorderLine(direction), false);
            }
        }
        Graphics2D local = (Graphics2D) graphics.create();
        try {
            local.scale(scale, scale);
            Color color = GUIP.getMapsheetColor();
            boolean vectors = local instanceof BoardTacticalGraphics;
            if (vectors) {
                local.setColor(new Color(0, 0, 0, color.getAlpha() / 2));
                local.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                local.draw(path);
            }
            local.setColor(color);
            local.setStroke(new BasicStroke(vectors ? 2.5f : 1 / scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            local.draw(path);
        } finally {
            local.dispose();
        }
    }

    /** Draws an embedded-board indicator using the same rectangle in both renderers. */
    private void drawEmbeddedBoard(Graphics2D g) {
        AffineTransform oldTransform = g.getTransform();
        Stroke oldStroke = g.getStroke();
        g.transform(AffineTransform.getScaleInstance(scale, scale));
        g.setColor(new Color(0, 140, 0, 120));
        g.fillRect(HEX_W / 4 + 1, 2, HEX_W / 2 - 2, HEX_H - 4);
        if (g instanceof BoardTacticalGraphics) {
            g.setColor(new Color(0, 0, 0, 150));
            g.setStroke(new BasicStroke(3.5f));
            g.drawRect(HEX_W / 4 + 1, 2, HEX_W / 2 - 2, HEX_H - 4);
        }
        g.setColor(new Color(0, 140, 0));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(HEX_W / 4 + 1, 2, HEX_W / 2 - 2, HEX_H - 4);
        g.setTransform(oldTransform);
        g.setStroke(oldStroke);
    }

    @Override
    public boolean isShowingAnimation() {
        return isMovingUnits();
    }

    public void suspendTooltip() {
        tooltipSuspended = true;
        boardPanel.setToolTipText(null);
    }

    public void activateTooltip() {
        tooltipSuspended = false;
    }

    public void toggleShowDeployment() {
        showAllDeployment = !showAllDeployment;
        repaint();
    }

    public void addHexDrawPlugin(HexDrawPlugin plugin) {
        hexDrawPlugins.add(plugin);
        invalidatePlanarCapture();
    }

    /**
     * Sets the vertical offset to the correct value depending on the current isometric on/off status; the vertical
     * offset is the distance by which hexes are moved up/down in screen space to represent their elevation.
     */
    private void setVerticalOffset() {
        verticalOffset = GUIP.getIsometricEnabled() ? ISOMETRIC_OFFSET : 0;
    }

    /**
     * @return The currently used hex elevation vertical offset; when isometric mode is off, this is 0.
     */
    public int getVerticalOffset() {
        return verticalOffset;
    }
}
