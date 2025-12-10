/*
 * Copyright (c) 2002-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.minimap;

import static megamek.client.ui.dialogs.minimap.MinimapUnitSymbols.FACING_ARROW;
import static megamek.client.ui.dialogs.minimap.MinimapUnitSymbols.STD_DESTROYED;
import static megamek.client.ui.dialogs.minimap.MinimapUnitSymbols.STRAT_BASE_RECT;
import static megamek.client.ui.dialogs.minimap.MinimapUnitSymbols.STRAT_CX;
import static megamek.client.ui.dialogs.minimap.MinimapUnitSymbols.STRAT_DESTROYED;
import static megamek.client.ui.dialogs.minimap.MinimapUnitSymbols.STRAT_SYMBOL_SIZE;
import static megamek.common.units.Terrains.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.IClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.ScalingPopup;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardHelper;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.event.board.BoardEvent;
import megamek.common.event.board.BoardListener;
import megamek.common.event.board.BoardListenerAdapter;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.board.GameBoardNewEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.*;
import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;
import megamek.utilities.GifWriter;
import megamek.utilities.GifWriterThread;

/**
 * Obviously, displays the map in scaled-down size. TBD: -move the buttons from graphics to real Swing buttons -clean up
 * listenercode.. -initializecolors is fugly -uses break-to-label -uses while-true
 */
public final class MinimapPanel extends JPanel implements IPreferenceChangeListener {
    private static final MMLogger logger = MMLogger.create(MinimapPanel.class);

    private static final Color[] terrainColors = new Color[Terrains.SIZE];
    public static final int DESTROYED_UNIT_ALPHA = 64;
    private static Color HEAVY_WOODS;
    private static Color ULTRA_HEAVY_WOODS;
    private static Color BACKGROUND;
    private static Color SINKHOLE;
    private static Color SMOKE_AND_FIRE;

    private static final int[] FONT_SIZE = { 6, 6, 8, 10, 12, 14, 16 };
    private static final int[] HEX_SIDE = { 2, 3, 5, 6, 8, 10, 12 };
    private static final int[] HEX_SIDE_BY_COS30 = { 2, 3, 4, 5, 7, 9, 10 };
    private static final int[] HEX_SIDE_BY_SIN30 = { 1, 2, 2, 3, 4, 5, 6 };
    private static final int[] HALF_ROAD_WIDTH_BY_COS30 = { 0, 0, 0, 1, 2, 2, 3 };
    private static final int[] HALF_ROAD_WIDTH_BY_SIN30 = { 0, 0, 0, 1, 1, 1, 2 };
    private static final int[] HALF_ROAD_WIDTH = { 0, 0, 0, 1, 2, 3, 3 };
    private static final int[] UNIT_SIZES = { 4, 5, 6, 7, 8, 9, 10 };
    private static final int[] UNIT_SCALE = { 7, 8, 9, 11, 12, 14, 16 };
    private static final int MIM_ZOOM = 0;
    private static final int MAX_ZOOM = HEX_SIDE.length - 1;
    private static final int MIM_ZOOM_FOR_HEIGHT = 4;

    private static final int SHOW_NO_HEIGHT = 0;
    private static final int SHOW_GROUND_HEIGHT = 1;
    private static final int SHOW_BUILDING_HEIGHT = 2;
    private static final int SHOW_TOTAL_HEIGHT = 3;
    private static final int NBR_HEIGHT_MODES = 3;

    private static final int SHOW_SYMBOLS = 0;
    private static final int SHOW_NO_SYMBOLS = 1;
    private static final int NBR_SYMBOLS_MODES = 1;

    private static final int DIALOG_MARGIN = 6;
    private static final int MARGIN = 3;
    private static final int BUTTON_HEIGHT = 14;


    /**
     * The minimap zoom at which game summary images are saved regardless of the in game minimap setting.
     */
    private static final int GAME_SUMMARY_ZOOM = 4;

    private static final String ACTION_ZOOM_IN = "ZOOM_IN";
    private static final String ACTION_ZOOM_OUT = "ZOOM_OUT";
    private static final String ACTION_HEIGHT_NONE = "HEIGHT_NONE";
    private static final String ACTION_HEIGHT_GROUND = "HEIGHT_GROUND";
    private static final String ACTION_HEIGHT_BUILDING = "HEIGHT_BUILDING";
    private static final String ACTION_HEIGHT_TOTAL = "HEIGHT_TOTAL";
    private static final String ACTION_SYMBOLS_NO = "SYMBOLS_NO";
    private static final String ACTION_SYMBOLS_SHOW = "SYMBOLS_SHOW";
    private static final String ACTION_FACING_ARROWS_SHOW = "FACING_ARROWS_SHOW";
    private static final String ACTION_SENSORS_SHOW = "SENSORS_SHOW";

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();
    private BufferedImage mapImage;
    private final BoardView bv;
    private final Game game;
    private Board board;
    private final int boardId;
    private final JDialog dialog;
    private Client client;
    private final IClientGUI clientGui;
    private GifWriterThread gifWriterThread;
    private int margin = MARGIN;
    private int topMargin;
    private int leftMargin;
    // This value is variable.
    // if the container m_dialog is an instance of JDialog, it is 14. otherwise 0.
    private int buttonHeight = 0;
    /**
     * Indicates if the minimap has been rolled up using the wide green button. Can only be true when in a dialog.
     */
    private boolean minimized = false;
    /** Stores the (non-minimized) height of the minimap when it is minimized. */
    private int heightBuffer;
    private int unitSize = 6;
    /** A list of information on hexes with roads or bridges. */
    private final List<int[]> roadHexes = new ArrayList<>();
    private int zoom = GUIP.getMinimapZoom();
    private int heightDisplayMode = GUIP.getMinimapHeightDisplayMode();
    private int symbolsDisplayMode = GUIP.getMinimapSymbolsDisplayMode();
    private boolean drawSensorRangeOnMiniMap = GUIP.getDrawSensorRangeOnMiniMap();
    private boolean drawFacingArrowsOnMiniMap = GUIP.getDrawFacingArrowsOnMiniMap();
    private boolean paintBorders = GUIP.paintBorders();
    private Coords firstLOS;
    private Coords secondLOS;
    private static final Set<Integer> removalReasons = Set.of(IEntityRemovalConditions.REMOVE_CAPTURED,
          IEntityRemovalConditions.REMOVE_SALVAGEABLE,
          IEntityRemovalConditions.REMOVE_DEVASTATED,
          IEntityRemovalConditions.REMOVE_EJECTED);
    /** Signifies that the whole minimap must be repainted. */
    private boolean dirtyMap = true;
    /** Keeps track of portions of the minimap that must be repainted. */
    private boolean[][] dirty = new boolean[1][1];
    private Image terrainBuffer;

    private final Map<Coords, Integer> multiUnits = new HashMap<>();
    private static final String[] STRAT_WEIGHTS = { "L", "L", "M", "H", "A", "A" };

    private boolean dragging = false;

    /** Returns a minimap image of the given board at the maximum zoom index. */
    public static BufferedImage getMinimapImageMaxZoom(Board board) {
        return getMinimapImage(board, MAX_ZOOM, null);
    }

    /** Returns a minimap image of the given board at the maximum zoom index. */
    public static BufferedImage getMinimapImageMaxZoom(Board board, @Nullable File minimapTheme) {
        return getMinimapImage(board, MAX_ZOOM, minimapTheme);
    }

    /** Returns a minimap image of the given board at the given zoom index. */
    public static BufferedImage getMinimapImage(Board board, int zoom) {
        Game game = new Game();
        game.setBoard(board);
        return getMinimapImage(game, null, zoom, null, 0);
    }

    /** Returns a minimap image of the given board at the given zoom index. */
    public static BufferedImage getMinimapImage(Board board, int zoom, @Nullable File minimapTheme) {
        Game game = new Game();
        game.setBoard(board);
        return getMinimapImage(game, null, zoom, minimapTheme, 0);
    }

    /**
     * Returns a minimap image of the given board at the given zoom index. The game and {@link BoardView} object will be
     * used to display additional information.
     */
    public static BufferedImage getMinimapImage(Game game, BoardView boardView, int zoom, @Nullable File minimapTheme,
          int boardId) {
        return getMinimapImage(game, boardView, zoom, null, minimapTheme, Collections.emptyList(), boardId);
    }

    /**
     * Returns a minimap image of the given board at the given zoom index. The game and {@link BoardView} object will be
     * used to display additional information.
     */
    public static BufferedImage getMinimapImage(Game game, BoardView boardView, int zoom, IClientGUI clientGui,
          @Nullable File minimapTheme, List<Line> movePathLines, int boardId) {
        try {
            // Send the fail image when the zoom index is wrong to make this noticeable
            if ((zoom < MIM_ZOOM) || (zoom > MAX_ZOOM)) {
                throw new Exception("The given zoom index is out of bounds.");
            }

            MinimapPanel tempMM = new MinimapPanel(null, game, boardView, clientGui, minimapTheme, boardId);
            tempMM.zoom = zoom;
            tempMM.movePathLines.clear();
            tempMM.movePathLines.addAll(movePathLines);
            tempMM.initializeMap();
            tempMM.drawMap(true);
            return ImageUtil.createAcceleratedImage(tempMM.mapImage);
        } catch (Exception e) {
            logger.error(e, "");
            return ImageUtil.failStandardImage();
        }
    }

    /**
     * Creates a minimap panel. The only required parameter is a game that contains the board to display. When the
     * dialog is not null, it is assumed that this minimap will be visible for a while, and it will register itself to
     * various objects as a listener to changes. When the dialog is null, it is assumed that the minimap is only used to
     * create a snapshot image. When a {@link BoardView} is given, the visible area is shown.
     */

    public MinimapPanel(@Nullable MinimapDialog minimapDialog, Game game, @Nullable BoardView boardView,
          @Nullable IClientGUI clientGUI,
          @Nullable File minimapTheme, int boardId) {
        this.game = Objects.requireNonNull(game);
        board = Objects.requireNonNull(this.game.getBoard(boardId));
        this.boardId = boardId;
        bv = boardView;
        dialog = minimapDialog;
        clientGui = clientGUI;

        if (clientGui != null && clientGui.getClient() instanceof Client castClient) {
            client = castClient;
        }
        initializeColors(minimapTheme);
        if (dialog != null) {
            initializeDialog();
            initializeListeners();
            buttonHeight = BUTTON_HEIGHT;
            margin = DIALOG_MARGIN;
        }
    }

    /**
     * Registers the minimap as listener to the given game, board, {@link BoardView} (that are not null).
     */
    private void initializeListeners() {
        game.addGameListener(new GameListenerAdapter() {

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if ((GUIP.getGameSummaryMinimap() || GUIP.getGifGameSummaryMinimap())
                      && (e.getOldPhase().isDeployment() || e.getOldPhase().isMovement()
                      || e.getOldPhase().isTargeting() || e.getOldPhase().isPremovement()
                      || e.getOldPhase().isPreFiring() || e.getOldPhase().isFiring()
                      || e.getOldPhase().isPhysical())) {

                    File dir = new File(Configuration.gameSummaryImagesMMDir(), game.getUUIDString());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String filenameTemplate = "round_%d_%d_%s.png";
                    File imgFile = new File(dir,
                          filenameTemplate.formatted(game.getRoundCount(), e.getOldPhase().ordinal(), e.getOldPhase()));

                    try {
                        BufferedImage image = getMinimapImage(game, bv, GAME_SUMMARY_ZOOM, clientGui, null,
                              movePathLines, boardId);
                        if (GUIP.getGameSummaryMinimap()) {
                            ImageIO.write(image, "png", imgFile);
                        }
                        if (GUIP.getGifGameSummaryMinimap()) {
                            if (gifWriterThread == null || !gifWriterThread.isAlive()) {
                                gifWriterThread = new GifWriterThread(new GifWriter(game.getUUIDString()),
                                      "GifWriterThread");
                                gifWriterThread.start();
                            }
                            gifWriterThread.addFrame(image, 400);
                        }
                    } catch (Exception ex) {
                        logger.error(ex, "Error saving game summary image.");
                    }
                }

                if (e.getNewPhase().isVictory() && (gifWriterThread != null) && gifWriterThread.isAlive()) {
                    try {
                        gifWriterThread.stopThread();
                    } catch (Exception ex) {
                        logger.error(ex, "Error closing gif writer.");
                    }
                }

                // We clear the move path lines locally, since the game does not currently hold this information until the end of the turn
                if (e.getNewPhase().isDeployment() || e.getNewPhase().isLounge() || e.getNewPhase().isVictory()) {
                    movePathLines.clear();
                } else {
                    movePathLines.removeIf(line -> (line.round() + GUIP.getMovePathPersistenceOnMiniMap())
                          <= game.getCurrentRound());
                }
                refreshMap();
            }

            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                refreshMap();
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                // We store the move path lines locally because the game does not currently hold this information until the end of the turn
                var movePath = e.getMovePath();
                if (movePath != null && !movePath.isEmpty()) {
                    addMovePath(new ArrayList<>(movePath), e.getOldEntity());
                    refreshMap();
                }
            }

            @Override
            public void gameBoardNew(GameBoardNewEvent e) {
                if (e.getBoardId() == boardId) {
                    Board b = e.getOldBoard();
                    if (b != null) {
                        b.removeBoardListener(boardListener);
                    }
                    b = e.getNewBoard();
                    if (b != null) {
                        b.addBoardListener(boardListener);
                    }
                    board = b;
                    initializeMap();
                }
            }

            @Override
            public void gameBoardChanged(GameBoardChangeEvent e) {
                refreshMap();
            }

            @Override
            public void gameNewAction(GameNewActionEvent e) {
                refreshMap();
            }
        });

        board.addBoardListener(boardListener);
        if (bv != null) {
            bv.addBoardViewListener(boardViewListener);
        }
        if (client != null) {
            client.addCloseClientListener(() -> {
                if (gifWriterThread != null && gifWriterThread.isAlive()) {
                    gifWriterThread.stopThread(true);
                }
            });
        }
        GUIP.addPreferenceChangeListener(this);
    }

    /**
     * Adds listeners to the dialog to manipulate the minimap if it has an associated dialog.
     */
    private void initializeDialog() {
        if (dialog != null) {
            if (dialog.getMouseListeners().length == 0) {
                dialog.addMouseListener(mouseListener);
                dialog.addMouseMotionListener(mouseMotionListener);
                dialog.addMouseWheelListener(mouseWheelListener);
                dialog.addComponentListener(componentListener);
                dialog.addComponentListener(componentListener);
            }
        }
    }

    private @Nullable Player getLocalPlayer() {
        if (client != null && client.getLocalPlayer() != null) {
            return client.getLocalPlayer();
        }
        if (clientGui != null && clientGui.getClient() != null && clientGui.getClient().getLocalPlayer() != null) {
            return clientGui.getClient().getLocalPlayer();
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (mapImage != null) {
            g.drawImage(mapImage, 0, 0, this);
            paintVisibleSection(g);
        }
    }

    /** Initialize default colors and override with config file if there is one. */
    private void initializeColors(File minimapTheme) {

        BACKGROUND = Color.black;
        terrainColors[0] = new Color(218, 215, 170);
        SINKHOLE = new Color(218, 215, 170);
        terrainColors[WOODS] = new Color(180, 230, 130);
        HEAVY_WOODS = new Color(160, 200, 100);
        ULTRA_HEAVY_WOODS = new Color(0, 100, 0);
        terrainColors[ROUGH] = new Color(215, 181, 0);
        terrainColors[RUBBLE] = new Color(200, 200, 200);
        terrainColors[WATER] = new Color(200, 247, 253);
        terrainColors[PAVEMENT] = new Color(204, 204, 204);
        terrainColors[ROAD] = new Color(71, 79, 107);
        terrainColors[FIRE] = Color.red;
        terrainColors[SMOKE] = new Color(204, 204, 204);
        SMOKE_AND_FIRE = new Color(153, 0, 0);
        terrainColors[SWAMP] = new Color(49, 136, 74);
        terrainColors[BUILDING] = new Color(204, 204, 204);
        terrainColors[FUEL_TANK] = new Color(255, 204, 204);
        terrainColors[BRIDGE] = new Color(109, 55, 25);
        terrainColors[ICE] = new Color(204, 204, 255);
        terrainColors[MAGMA] = new Color(200, 0, 0);
        // m_terrainColors[MUD] = new Color(218, 160, 100);
        terrainColors[JUNGLE] = new Color(180, 230, 130);
        terrainColors[FIELDS] = new Color(250, 255, 205);
        terrainColors[INDUSTRIAL] = new Color(112, 138, 144);
        terrainColors[SPACE] = Color.BLACK;

        // now try to read in the config file
        int red;
        int green;
        int blue;

        if (minimapTheme == null || !minimapTheme.exists()) {
            minimapTheme = CLIENT_PREFERENCES.getMinimapTheme();
        }

        // only while the defaults are hard-coded!
        if (!minimapTheme.exists()) {
            return;
        }

        try (Reader cr = new FileReader(minimapTheme)) {
            StreamTokenizer st = new StreamTokenizer(cr);

            st.lowerCaseMode(true);
            st.quoteChar('"');
            st.commentChar('#');

            scan:
            while (true) {
                switch (st.nextToken()) {
                    case StreamTokenizer.TT_EOF:
                    case StreamTokenizer.TT_EOL:
                        break scan;
                    case StreamTokenizer.TT_WORD:
                        // read in
                        String key = st.sval;
                        switch (key) {
                            case "unitsize" -> {
                                st.nextToken();
                                unitSize = (int) st.nval;
                            }
                            case "background" -> {
                                st.nextToken();
                                red = (int) st.nval;
                                st.nextToken();
                                green = (int) st.nval;
                                st.nextToken();
                                blue = (int) st.nval;

                                BACKGROUND = new Color(red, green, blue);
                            }
                            case "heavywoods" -> {
                                st.nextToken();
                                red = (int) st.nval;
                                st.nextToken();
                                green = (int) st.nval;
                                st.nextToken();
                                blue = (int) st.nval;

                                HEAVY_WOODS = new Color(red, green, blue);
                            }
                            case "ultraheavywoods" -> {
                                st.nextToken();
                                red = (int) st.nval;
                                st.nextToken();
                                green = (int) st.nval;
                                st.nextToken();
                                blue = (int) st.nval;

                                ULTRA_HEAVY_WOODS = new Color(red, green, blue);
                            }
                            case "sinkhole" -> {
                                st.nextToken();
                                red = (int) st.nval;
                                st.nextToken();
                                green = (int) st.nval;
                                st.nextToken();
                                blue = (int) st.nval;

                                SINKHOLE = new Color(red, green, blue);
                            }
                            case "smokeandfire" -> {
                                st.nextToken();
                                red = (int) st.nval;
                                st.nextToken();
                                green = (int) st.nval;
                                st.nextToken();
                                blue = (int) st.nval;

                                SMOKE_AND_FIRE = new Color(red, green, blue);
                            }
                            default -> {
                                st.nextToken();
                                red = (int) st.nval;
                                st.nextToken();
                                green = (int) st.nval;
                                st.nextToken();
                                blue = (int) st.nval;

                                terrainColors[getType(key)] = new Color(red, green, blue);
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            // Fall back to the default colors
            logger.error(e, "");
        }
    }

    /** Marks all the minimap as clean (not requiring an update). */
    private void markAsClean() {
        dirtyMap = false;
        for (boolean[] booleans : dirty) {
            Arrays.fill(booleans, false);
        }
    }

    void initializeMap() {
        dirty = new boolean[(board.getWidth() / 10) + 1][(board.getHeight() / 10) + 1];
        dirtyMap = true;
        unitSize = UNIT_SIZES[zoom];

        topMargin = margin;
        leftMargin = margin;
        int requiredWidth = (board.getWidth() * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]))
              + HEX_SIDE_BY_SIN30[zoom] + (2 * margin);
        int requiredHeight = minimized ? BUTTON_HEIGHT
              : (((2 * board.getHeight()) + 1)
              * HEX_SIDE_BY_COS30[zoom]) + (2 * margin) + buttonHeight;

        if (dialog != null) {
            setSize(new Dimension(requiredWidth, requiredHeight));
            setPreferredSize(new Dimension(requiredWidth, requiredHeight));
            dialog.pack();
            mapImage = ImageUtil.createAcceleratedImage(getSize().width, getSize().height);
            terrainBuffer = createImage(getSize().width, getSize().height);

            // Center the minimap in the dialog
            if (getSize().width > requiredWidth) {
                leftMargin = ((getSize().width - requiredWidth) / 2) + DIALOG_MARGIN;
            }
            if (getSize().height > requiredHeight) {
                topMargin = ((getSize().height - requiredHeight) / 2) + DIALOG_MARGIN;
            }
            // Start the refresh timer only for a "live" minimap in a dialog
            refreshMap();
            revalidate();
        } else {
            mapImage = ImageUtil.createAcceleratedImage(requiredWidth, requiredHeight);
            terrainBuffer = ImageUtil.createAcceleratedImage(requiredWidth, requiredHeight);
        }
        Graphics gg = terrainBuffer.getGraphics();
        gg.setColor(BACKGROUND);
        gg.fillRect(0, 0, getSize().width, getSize().height);
    }

    private long lastDrawMapReq = 0;
    private long lastDrawStarted = 0;
    private final Runnable drawMapable = new Runnable() {

        @Override
        public void run() {
            try {
                int redrawDelay = 0;
                if ((java.lang.System.currentTimeMillis() - lastDrawMapReq) > redrawDelay) {
                    drawMap();
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        // should never happen
                    }
                    SwingUtilities.invokeLater(drawMapable);
                }
            } catch (Throwable t) {
                logger.error(t, "");
            }
        }
    };

    private final List<Line> movePathLines = new Vector<>();

    public record Line(int x1, int y1, int x2, int y2, Color color, int round) {}

    private final Color MOVE_PATH_COLOR = new Color(0, 0, 0, 128);

    private void addMovePath(List<UnitLocation> unitLocations, Entity entity) {
        if ((GUIP.getMovePathPersistenceOnMiniMap() <= 0)
              || !EntityVisibilityUtils.detectedOrHasVisual(getLocalPlayer(), game, entity)) {
            return;
        }
        Coords previousCoords = entity.getPosition();
        for (var unitLocation : unitLocations) {
            var coords = unitLocation.coords();
            if (previousCoords != null && unitLocation.boardId() == boardId) {
                movePathLines.add(new Line(previousCoords.getX(),
                      previousCoords.getY(),
                      coords.getX(),
                      coords.getY(),
                      MOVE_PATH_COLOR,
                      game.getCurrentRound()));
            }
            previousCoords = coords;
        }
    }

    /** Call this to schedule a minimap redraw. */
    public void refreshMap() {
        lastDrawMapReq = java.lang.System.currentTimeMillis();
        SwingUtilities.invokeLater(drawMapable);
    }

    /**
     * Draws the minimap to the back-buffer. If the dialog is not visible or the minimap is minimized, drawing will be
     * kept to a minimum.
     */
    private void drawMap() {
        drawMap(false);
    }

    /**
     * Draws the minimap to the back-buffer. When forceDraw is true, the map will be drawn even if it is minimized or
     * not visible. This can be used to draw the minimap for saving it as an image regardless of its visual status
     * onscreen.
     */
    private void drawMap(boolean forceDraw) {
        if ((lastDrawStarted > lastDrawMapReq) && !forceDraw) {
            return;
        }
        lastDrawStarted = java.lang.System.currentTimeMillis();

        if (!forceDraw && (dialog != null) && !dialog.isVisible()) {
            return;
        }

        Graphics g = mapImage.getGraphics();
        UIUtil.setHighQualityRendering(g);

        if (!minimized || forceDraw) {
            roadHexes.clear();
            Graphics gg = terrainBuffer.getGraphics();
            UIUtil.setHighQualityRendering(gg);
            for (int j = 0; j < board.getWidth(); j++) {
                for (int k = 0; k < board.getHeight(); k++) {
                    Hex h = board.getHex(j, k);
                    if (h == null) {
                        continue;
                    }
                    if (dirtyMap || dirty[j / 10][k / 10]) {
                        gg.setColor(terrainColor(h));
                        if (board.isHighAltitude()) {
                            paintHighAltCoord(gg, j, k);
                        } else if (board.isSpace()) {
                            paintSpaceCoord(gg, j, k);
                        } else if (h.containsTerrain(SKY)) {
                            paintLowAtmosphereSkyCoord(gg, j, k, paintBorders && zoom > 1);
                        } else {
                            paintCoord(gg, j, k, paintBorders && zoom > 1);
                        }
                    }
                    addRoadElements(h, j, k);
                    // Color invalid hexes red when in the Map Editor
                    if ((game != null) && game.getPhase().isUnknown() && !h.isValid(null)) {
                        gg.setColor(GUIP.getWarningColor());
                        paintCoord(gg, j, k, true);
                    }
                }
            }
            g.drawImage(terrainBuffer, 0, 0, this);
            markAsClean();

            if (firstLOS != null) {
                paintSingleCoordBorder(g, firstLOS.getX(), firstLOS.getY(), Color.red);
            }
            if (secondLOS != null) {
                paintSingleCoordBorder(g, secondLOS.getX(), secondLOS.getY(), Color.red);
            }

            paintRoads(g);

            if (SHOW_NO_HEIGHT != heightDisplayMode) {
                for (int j = 0; j < board.getWidth(); j++) {
                    for (int k = 0; k < board.getHeight(); k++) {
                        Hex h = board.getHex(j, k);
                        paintHeight(g, h, j, k);
                    }
                }
            }

            if (board.isHighAltitude()
                  && (HEX_SIDE[zoom] >= 10)) {
                int baseX = HEX_SIDE[zoom] + leftMargin;
                int baseY = (board.getHeight() + 1) * HEX_SIDE_BY_COS30[zoom] + topMargin;
                new StringDrawer("Ground Hexes").at(baseX, baseY).fontSize(HEX_SIDE[zoom])
                      .rotate(Math.toRadians(90)).center().color(Color.BLACK).draw(g);

                if ((game != null) && !game.getPlanetaryConditions().getAtmosphere().isVacuum()) {
                    baseX = HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom] + HEX_SIDE[zoom] + leftMargin;
                    new StringDrawer("Atmospheric Row 1").at(baseX, baseY).fontSize(HEX_SIDE[zoom])
                          .rotate(Math.toRadians(90)).center().color(Color.LIGHT_GRAY).draw(g);

                    baseX = (4 * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + HEX_SIDE[zoom]) + leftMargin;
                    new StringDrawer("Atmospheric Row 4").at(baseX, baseY).fontSize(HEX_SIDE[zoom])
                          .rotate(Math.toRadians(90)).center().color(Color.GRAY).draw(g);

                    if (game.getPlanetaryConditions().getAtmosphere().isVeryHigh()) {
                        baseX = (7 * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + HEX_SIDE[zoom]) + leftMargin;
                        new StringDrawer("Atmospheric Row 7").at(baseX, baseY).fontSize(HEX_SIDE[zoom])
                              .rotate(Math.toRadians(90)).center().color(Color.GRAY).draw(g);
                    }

                    baseX = (BoardHelper.spaceAtmosphereInterfacePosition(game)
                          * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + HEX_SIDE[zoom]) + leftMargin;
                    new StringDrawer("Space/Atmosphere Interface").at(baseX, baseY).fontSize(HEX_SIDE[zoom])
                          .rotate(Math.toRadians(90)).center().color(Color.GRAY).draw(g);
                }

                if (board.getWidth() > 15) {
                    baseX = (15 * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + HEX_SIDE[zoom]) + leftMargin;
                    new StringDrawer("End of Gravity").at(baseX, baseY).fontSize(HEX_SIDE[zoom])
                          .rotate(Math.toRadians(90)).center().color(Color.GRAY).draw(g);
                }
            }

            drawDeploymentZone(g);
            drawEmbeddedBoards(g);

            // In case the flag SHOW SYMBOLS is set, it will draw the units and other stuff
            if (symbolsDisplayMode == SHOW_SYMBOLS) {
                if (null != game) {
                    // draw dead units
                    multiUnits.clear();
                    for (Entity e : game.getOutOfGameEntitiesVector()) {
                        if (e.getBoardLocation().isOn(boardId) &&
                              removalReasons.contains(e.getRemovalCondition())) {
                            paintUnit(g, e, true);
                        }
                    }

                    if (!movePathLines.isEmpty()) {
                        paintMoveTracks(g);
                    }

                    // draw declared fire
                    for (EntityAction action : game.getActionsVector()) {
                        if (action instanceof AttackAction) {
                            paintAttack(g, (AttackAction) action);
                        }
                    }

                    // draw living units
                    for (Entity e : game.getEntitiesVector()) {
                        if (e.getBoardLocation().isOn(boardId)) {
                            paintUnit(g, e, false);
                        }
                    }

                    if (drawSensorRangeOnMiniMap) {
                        for (Entity e : game.getEntitiesVector()) {
                            if (e.getBoardLocation().isOn(boardId)) {
                                paintSensor(g, e);
                            }
                        }
                    }
                }

                Player localPlayer = getLocalPlayer();
                if (localPlayer != null) {
                    for (BoardLocation autoHitHex : localPlayer.getArtyAutoHitHexes()) {
                        if (autoHitHex.isOn(boardId)) {
                            drawAutoHit(g, autoHitHex.coords());
                        }
                    }
                }

            }
        }

        if (dialog != null) {
            drawButtons(g);
            repaint();
        }
    }

    private void paintMoveTracks(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
              0, new float[] { 6 }, 3);
        g2d.setStroke(dashed);
        for (Line line : movePathLines) {
            paintMoveTrack(g2d, line);
        }
        g2d.dispose();
    }

    /** Indicates the deployment hexes. */
    private void drawDeploymentZone(Graphics g) {
        if ((null != client) && (null != game) && game.getPhase().isDeployment() && (bv != null)
              && (bv.getDeployingEntity() != null) && (dialog != null) && (getLocalPlayer() != null)) {
            GameTurn turn = game.getTurn();
            if ((turn != null) && (turn.playerId() == getLocalPlayer().getId())) {
                Entity deployingUnit = bv.getDeployingEntity();

                for (int j = 0; j < board.getWidth(); j++) {
                    for (int k = 0; k < board.getHeight(); k++) {
                        Coords coords = new Coords(j, k);

                        if (board.isLegalDeployment(coords, deployingUnit) &&
                              !deployingUnit.isLocationProhibited(BoardLocation.of(coords, boardId)) &&
                              !deployingUnit.isBoardProhibited(board)) {
                            paintSingleCoordBorder(g, j, k, Color.yellow);
                        }
                    }
                }
            }
        }
    }

    /**
     * Draw an outline around legal deployment hexes
     */
    private void drawEmbeddedBoards(Graphics g) {
        for (Coords coords : board.embeddedBoardCoords()) {
            paintEmbeddedBoard(g, coords.getX(), coords.getY(), Color.GREEN);
        }
    }

    /**
     * Draws a box showing the portion of the board that is currently visible in the {@link BoardView}.
     */
    private void paintVisibleSection(Graphics g) {
        if (minimized || (bv == null)) {
            return;
        }
        double[] relSize = bv.getVisibleArea();
        for (int i = 0; i < 4; i++) {
            // keep between 0 and 1 to not fall outside the minimap
            relSize[i] = Math.min(1, Math.max(0, relSize[i]));
        }

        int x1 = (int) (relSize[0] * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) * board.getWidth()) + leftMargin;
        int y1 = (int) (relSize[1] * 2 * HEX_SIDE_BY_COS30[zoom] * board.getHeight()) + topMargin;
        int x2 = (int) ((relSize[2] - relSize[0]) * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) * board.getWidth());
        int y2 = (int) ((relSize[3] - relSize[1]) * 2 * HEX_SIDE_BY_COS30[zoom] * board.getHeight());

        // thicker but translucent rectangle
        g.setColor(new Color(100, 100, 160, 80));
        ((Graphics2D) g).setStroke(new BasicStroke(zoom + 2));
        g.drawRect(x1, y1, x2, y2);

        // thin less translucent rectangle
        g.setColor(new Color(255, 255, 255, 180));
        ((Graphics2D) g).setStroke(new BasicStroke(zoom / 2f));
        g.drawRect(x1, y1, x2, y2);
    }

    /** Draws a red crosshair for artillery auto hit hexes (predesignated only). */
    private void drawAutoHit(Graphics g, Coords hex) {
        int baseX = (hex.getX() * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin + HEX_SIDE[zoom];
        int baseY = (((2 * hex.getY()) + 1 + (hex.getX() % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        g.setColor(Color.RED);
        g.drawOval(baseX - (unitSize - 1), baseY - (unitSize - 1), (2 * unitSize) - 2, (2 * unitSize) - 2);
        g.drawLine(baseX - unitSize - 1, baseY, (baseX - unitSize) + 3, baseY);
        g.drawLine(baseX + unitSize + 1, baseY, (baseX + unitSize) - 3, baseY);
        g.drawLine(baseX, baseY - unitSize - 1, baseX, (baseY - unitSize) + 3);
        g.drawLine(baseX, baseY + unitSize + 1, baseX, (baseY + unitSize) - 3);
    }

    /** Draws the green control buttons at the bottom of the Minimap. */
    private void drawButtons(Graphics g) {
        int w = getSize().width;
        int h = getSize().height;
        int w0 = w - BUTTON_HEIGHT;
        int y0 = h - BUTTON_HEIGHT;

        // the center bar for rolling up/down the Minimap
        g.setColor(Color.green.darker().darker());
        g.fillRect(0, y0, w, BUTTON_HEIGHT);
        g.setColor(Color.green.darker());
        g.drawLine(0, y0, w, y0);
        g.drawLine(0, y0, 0, h);
        g.setColor(Color.black);
        g.drawLine(0, h - 1, w, h - 1);
        g.drawLine(w - 1, y0, w - 1, h);

        int[] xTriangle = new int[3];
        int[] yTriangle = new int[3];
        xTriangle[0] = Math.round((w - 11) / 2f);
        xTriangle[1] = xTriangle[0] + 11;
        if (minimized) {
            yTriangle[0] = h - 10;
            yTriangle[1] = yTriangle[0];
            xTriangle[2] = xTriangle[0] + 6;
            yTriangle[2] = yTriangle[0] + 5;
        } else {
            yTriangle[0] = h - 4;
            yTriangle[1] = yTriangle[0];
            xTriangle[2] = xTriangle[0] + 5;
            yTriangle[2] = yTriangle[0] - 5;
        }
        g.setColor(Color.yellow);
        g.fillPolygon(xTriangle, yTriangle, 3);

        // the zoom control "+" and "-" buttons
        if (!minimized) {
            g.setColor(Color.black);
            g.drawLine(BUTTON_HEIGHT - 1, y0, BUTTON_HEIGHT - 1, h);
            g.drawLine(w0 - 1, y0, w0 - 1, h);
            g.setColor(Color.green.darker());
            g.drawLine(BUTTON_HEIGHT, y0, BUTTON_HEIGHT, getSize().height);
            g.drawLine(w0, y0, w0, h);
            if (zoom == 0) {
                g.setColor(Color.gray.brighter());
            } else {
                g.setColor(Color.yellow);
            }
            g.fillRect(3, y0 + 6, 8, 2);
            if (zoom == (HEX_SIDE.length - 1)) {
                g.setColor(Color.gray.brighter());
            } else {
                g.setColor(Color.yellow);
            }
            g.fillRect(w0 + 3, y0 + 6, 8, 2);
            g.fillRect(w0 + 6, y0 + 3, 2, 8);

            if (zoom >= MIM_ZOOM_FOR_HEIGHT) {
                // the button for displaying heights
                int x = BUTTON_HEIGHT;
                g.setColor(Color.yellow);
                String label = switch (heightDisplayMode) {
                    case SHOW_NO_HEIGHT -> Messages.getString("Minimap.NoHeightLabel");
                    case SHOW_GROUND_HEIGHT -> Messages.getString("Minimap.GroundHeightLabel");
                    case SHOW_BUILDING_HEIGHT -> Messages.getString("Minimap.BuildingHeightLabel");
                    case SHOW_TOTAL_HEIGHT -> Messages.getString("Minimap.TotalHeightLabel");
                    default -> "";
                };
                g.drawString(label, x + 2, y0 + 11);

                x += BUTTON_HEIGHT;
                g.setColor(Color.black);
                g.drawLine(x - 1, y0, x - 1, h);
                g.setColor(Color.green.darker());
                g.drawLine(x, y0, x, h);

                // the button for displaying symbols
                g.setColor(Color.yellow);
                label = switch (symbolsDisplayMode) {
                    case SHOW_SYMBOLS -> Messages.getString("Minimap.SymbolsLabel");
                    case SHOW_NO_SYMBOLS -> Messages.getString("Minimap.NoSymbolsLabel");
                    default -> "";
                };
                g.drawString(label, x + 2, y0 + 11);

                x += BUTTON_HEIGHT;
                g.setColor(Color.black);
                g.drawLine(x - 1, y0, x - 1, h);
                g.setColor(Color.green.darker());
                g.drawLine(x, y0, x, h);

                // map size
                String mapSize = board.getWidth() + " " + Messages.getString("Minimap.X") + " " + board.getHeight();
                g.setColor(Color.yellow);
                g.drawString(mapSize, x, y0 + 11);

                int width = getFontMetrics(g.getFont()).stringWidth(mapSize);
                x += width + 3;
                g.setColor(Color.black);
                g.drawLine(x - 1, y0, x, h);
                g.setColor(Color.green.darker());
                g.drawLine(x, y0, x, h);
            }
        }
    }

    /** Writes the height value (hex/building/none) in the minimap hexes. */
    private void paintHeight(Graphics g, Hex h, int x, int y) {
        if ((heightDisplayMode == SHOW_NO_HEIGHT) || (zoom < MIM_ZOOM_FOR_HEIGHT)) {
            return;
        }

        int height = 0;
        if ((heightDisplayMode == SHOW_BUILDING_HEIGHT) && h.containsTerrain(BUILDING)) {
            height = h.ceiling();
        } else if (heightDisplayMode == SHOW_GROUND_HEIGHT) {
            height = h.floor();
        } else if (heightDisplayMode == SHOW_TOTAL_HEIGHT) {
            height = (h.containsAnyTerrainOf(BUILDING, FUEL_TANK)) ? h.ceiling() : h.floor();
        }
        if (height != 0) {
            String sHeight = height + "";
            int baseX = coordsXToPixel(x);
            int baseY = coordsYtoPixel(y, x);
            Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, FONT_SIZE[zoom]);
            int fontWidth = getFontMetrics(font).stringWidth(sHeight) / 2;
            int fontHeight = getFontMetrics(font).getHeight() / 3;
            g.setFont(font);
            g.setColor(Color.white);
            g.drawString(sHeight, baseX - fontWidth, baseY + fontHeight);
        }
    }

    private int[] xPoints(int x) {
        int baseX = (x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin;
        int[] xPoints = new int[6];
        xPoints[0] = baseX;
        xPoints[1] = baseX + HEX_SIDE_BY_SIN30[zoom];
        xPoints[2] = xPoints[1] + HEX_SIDE[zoom];
        xPoints[3] = xPoints[2] + HEX_SIDE_BY_SIN30[zoom];
        xPoints[4] = xPoints[2];
        xPoints[5] = xPoints[1];
        return xPoints;
    }

    private int[] yPoints(int x, int y) {
        int baseY = (((2 * y) + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        int[] yPoints = new int[6];
        yPoints[0] = baseY;
        yPoints[1] = baseY + HEX_SIDE_BY_COS30[zoom];
        yPoints[2] = yPoints[1];
        yPoints[3] = baseY;
        yPoints[4] = baseY - HEX_SIDE_BY_COS30[zoom];
        yPoints[5] = yPoints[4];
        return yPoints;
    }

    private void paintSingleCoordBorder(Graphics g, int x, int y, Color c) {
        g.setColor(c);
        ((Graphics2D) g).setStroke(new BasicStroke(Math.max(1, zoom / 2f)));
        g.drawPolygon(xPoints(x), yPoints(x, y), 6);
    }


    private void paintEmbeddedBoard(Graphics graphics, int x, int y, Color color) {
        graphics.setColor(color);
        ((Graphics2D) graphics).setStroke(new BasicStroke(Math.max(0.8f, zoom / 3f)));

        int baseX = (x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin;
        int[] xPoints = new int[6];
        xPoints[0] = baseX + HEX_SIDE_BY_SIN30[zoom];
        xPoints[1] = xPoints[0] + HEX_SIDE[zoom];
        xPoints[2] = xPoints[1];
        xPoints[3] = xPoints[0];

        int baseY = (((2 * y) + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        int[] yPoints = new int[6];
        yPoints[0] = baseY + HEX_SIDE_BY_COS30[zoom];
        yPoints[1] = yPoints[0];
        yPoints[2] = baseY - HEX_SIDE_BY_COS30[zoom];
        yPoints[3] = yPoints[2];
        graphics.drawPolygon(xPoints, yPoints, 4);
    }

    private void paintCoord(Graphics g, int x, int y, boolean border) {
        int[] xPoints = xPoints(x);
        int[] yPoints = yPoints(x, y);
        g.fillPolygon(xPoints, yPoints, 6);
        if (border) {
            g.setColor(g.getColor().darker());
        }
        g.drawPolygon(xPoints, yPoints, 6);
    }

    private void paintLowAtmosphereSkyCoord(Graphics g, int x, int y, boolean paintBorder) {
        int[] xPoints = xPoints(x);
        int[] yPoints = yPoints(x, y);
        int c = 190;
        g.setColor(new Color(c / 2, c, c));
        g.fillPolygon(xPoints, yPoints, 6);
        if (paintBorder) {
            c = 170;
            g.setColor(new Color(c / 2, c, c));
        }
        g.drawPolygon(xPoints, yPoints, 6);
    }

    private void paintSpaceCoord(Graphics g, int x, int y) {
        int baseX = (x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin;
        int baseY = (((2 * y) + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        int[] xPoints = xPoints(x);
        int[] yPoints = yPoints(x, y);
        g.fillPolygon(xPoints, yPoints, 6);
        // alpha to tone down borders and stars at low zoom so they don't overwhelm the
        // image
        int alpha = Math.min(250, 20 * HEX_SIDE[zoom]);
        g.setColor(new Color(20, 20, 60, alpha));
        g.drawPolygon(xPoints, yPoints, 6);
        // Drop in a star
        int dx = (int) (Math.random() * HEX_SIDE[zoom]);
        int dy = (int) ((Math.random() - 0.5) * HEX_SIDE_BY_COS30[zoom]);
        int c = (int) (Math.random() * 180);
        g.setColor(new Color(c, c, c, alpha));
        if (Math.random() < 0.1) {
            g.setColor(new Color(c, c / 10, c / 10, alpha)); // red star
        } else if (Math.random() < 0.1) {
            int factor = (int) (Math.random() * 10) + 1;
            g.setColor(new Color(c / factor, c / factor, c, alpha)); // blue star
        }
        g.fillRect(baseX + dx, baseY + dy, 1, 1);
    }

    private void paintHighAltCoord(Graphics g, int x, int y) {
        int baseX = (x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin;
        int baseY = (((2 * y) + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        int[] xPoints = xPoints(x);
        int[] yPoints = yPoints(x, y);

        int spaceInterfacePosition = BoardHelper.spaceAtmosphereInterfacePosition(game);
        // Draw in atmosphere in a high-altitude map (unless planetary conditions say its vacuum)
        if (BoardHelper.isAtmosphericRow(game, board, x)) {
            int atmosphericRow = BoardHelper.effectiveAtmosphericRowNumber(game, board, x);
            // Add atmosphere
            int step = 120 / (spaceInterfacePosition - 1);
            g.setColor(new Color(75 - step / 2 * atmosphericRow,
                  150 - step * atmosphericRow, 150 - step * atmosphericRow));
        } else if (BoardHelper.isGroundRowHex(board, x)) {
            g.setColor(Color.GREEN);
        }
        g.fillPolygon(xPoints, yPoints, 6);
        if (paintBorders && zoom > 1) {
            g.setColor(new Color(20, 20, 60));
        }
        g.drawPolygon(xPoints, yPoints, 6);

        if (x > spaceInterfacePosition) {
            // Drop in a star
            int dx = (int) (Math.random() * HEX_SIDE[zoom]);
            int dy = (int) ((Math.random() - 0.5) * HEX_SIDE_BY_COS30[zoom]);
            int c = (int) (Math.random() * 180);
            g.setColor(new Color(c, c, c));
            if (Math.random() < 0.1) {
                g.setColor(new Color(c, c / 10, c / 10)); // red star
            } else if (Math.random() < 0.1) {
                int factor = (int) (Math.random() * 10) + 1;
                g.setColor(new Color(c / factor, c / factor, c)); // blue star
            }
            g.fillRect(baseX + dx, baseY + dy, 1, 1);
        }
    }

    private void paintMoveTrack(Graphics2D g, Line line) {
        int baseX1 = (line.x1 * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin + HEX_SIDE[zoom];
        int baseY1 = (((2 * line.y1) + 1 + (line.x1 % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        int baseX2 = (line.x2 * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin + HEX_SIDE[zoom];
        int baseY2 = (((2 * line.y2) + 1 + (line.x2 % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        g.setColor(line.color);
        g.drawLine(baseX1, baseY1, baseX2, baseY2);
    }

    /**
     * Draw a line to represent an attack
     */
    private void paintAttack(Graphics g, AttackAction attack) {
        Entity weaponEntity = game.getEntity(attack.getEntityId()); // ex. HHW
        Entity source = weaponEntity != null ? weaponEntity.getAttackingEntity() : null;

        Targetable target = game.getTarget(attack.getTargetType(), attack.getTargetId());
        // sanity check...
        // cross-board attacks don't get attack arrows (for now, must possibly allow some A2G, O2G, A2A attacks later
        // when target/attacker hexes are not really but effectively on the same board)
        if ((null == source) || (null == target) || !game.onTheSameBoard(source, target)
              || (target.getBoardId() != boardId)) {
            return;
        }

        if (attack.getTargetType() == Targetable.TYPE_I_NARC_POD) {
            // iNarc pods don't have a position
            return;
        }
        if (attack instanceof WeaponAttackAction waa) {
            if ((getLocalPlayer() == null) || ((attack.getTargetType() == Targetable.TYPE_HEX_ARTILLERY)
                  && (waa.getEntity(game).getOwner().getId() != getLocalPlayer().getId()))) {
                return;
            }
        }

        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        xPoints[0] = ((source.getPosition().getX() * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]))
              + leftMargin + (HEX_SIDE[zoom])) - 2;
        yPoints[0] = (((2 * source.getPosition().getY()) + 1 + (source
              .getPosition().getX() % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
        xPoints[1] = ((target.getPosition().getX() * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]))
              + leftMargin + (HEX_SIDE[zoom])) - 2;
        yPoints[1] = (((2 * target.getPosition().getY()) + 1 + (target
              .getPosition().getX() % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
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
        g.setColor(source.getOwner().getColour().getColour());
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(Color.black);
        g.drawPolygon(xPoints, yPoints, 4);

        // if this is mutual fire, draw a half-and-half line
        for (EntityAction action : game.getActionsVector()) {
            if (action instanceof AttackAction otherAttack) {
                if ((attack.getEntityId() == otherAttack.getTargetId())
                      && (otherAttack.getEntityId() == attack.getTargetId())) {
                    // attackTarget _must_ be an entity since it's shooting back
                    // (?)
                    Entity attackTarget = game.getEntity(otherAttack.getEntityId());
                    if (attackTarget != null) {
                        Player attackOwner = attackTarget.getOwner();

                        if (attackOwner != null) {
                            g.setColor(attackOwner.getColour().getColour());
                        }
                    }


                    xPoints[0] = xPoints[3];
                    yPoints[0] = yPoints[3];
                    xPoints[1] = xPoints[2];
                    yPoints[1] = yPoints[2];
                    xPoints[2] = xPoints[1] + 2;
                    xPoints[3] = xPoints[0] + 2;
                    if (((source.getPosition().getX() > target.getPosition().getX())
                          && (source.getPosition().getY() < target.getPosition().getY()))
                          || ((source.getPosition().getX() < target.getPosition().getX())
                          && (source.getPosition().getY() > target.getPosition().getY()))) {
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
    }

    public static Color changeColorForDestroyedUnit(Color color, int alpha) {
        color = color.brighter();
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** Draws the symbol for a single entity. Checks visibility in double-blind. */
    private void paintUnit(Graphics g, Entity entity, boolean removedFromGame) {
        int x = entity.getPosition().getX();
        int y = entity.getPosition().getY();
        int baseX = coordsXToPixel(x);
        int baseY = coordsYtoPixel(y, x);

        if (EntityVisibilityUtils.onlyDetectedBySensors(getLocalPlayer(), entity) && !removedFromGame) {
            // This unit is visible only as a sensor Return
            String sensorReturn = "?";
            Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, FONT_SIZE[zoom]);
            int width = getFontMetrics(font).stringWidth(sensorReturn) / 2;
            int height = getFontMetrics(font).getHeight() / 2 - 2;
            g.setFont(font);
            g.setColor(Color.RED);
            g.drawString(sensorReturn, baseX - width, baseY + height);
            return;
        } else if (!EntityVisibilityUtils.detectedOrHasVisual(getLocalPlayer(), game, entity)) {
            // This unit is not visible, don't draw it
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        Stroke saveStroke = g2.getStroke();
        AffineTransform saveTransform = g2.getTransform();
        boolean stratOpsSymbols = GUIP.getMmSymbol();

        // Choose player or team color depending on preferences
        Color iconColor = entity.getOwner().getColour().getColour(false);
        if (GUIP.getTeamColoring() && (client != null)) {
            boolean isLocalTeam = (getLocalPlayer() != null) && (entity.getOwner().getTeam()
                  == getLocalPlayer().getTeam());
            boolean isLocalPlayer = entity.getOwner().equals(getLocalPlayer());
            if (isLocalPlayer) {
                iconColor = GUIP.getMyUnitColor();
            } else if (isLocalTeam) {
                iconColor = GUIP.getAllyUnitColor();
            } else {
                iconColor = GUIP.getEnemyUnitColor();
            }
        }

        if (removedFromGame) {
            iconColor = changeColorForDestroyedUnit(iconColor.brighter(), DESTROYED_UNIT_ALPHA);
        }

        // Transform for placement and scaling
        var placement = AffineTransform.getTranslateInstance(baseX, baseY);
        placement.scale(UNIT_SCALE[zoom] / 100.0d, UNIT_SCALE[zoom] / 100.0d);
        g2.transform(placement);

        // Add a position shift if multiple units are present in this hex
        Coords p = entity.getPosition();
        int eStack = multiUnits.getOrDefault(p, 0) + 1;
        multiUnits.put(p, eStack);
        g2.translate(20 * (eStack - 1), -20 * (eStack - 1));

        Path2D form = MinimapUnitSymbols.getForm(entity);

        Color borderColor = entity.moved != EntityMovementType.MOVE_NONE && !removedFromGame ?
              Color.BLACK :
              Color.WHITE;
        if (removedFromGame) {
            borderColor = changeColorForDestroyedUnit(borderColor.brighter(), DESTROYED_UNIT_ALPHA);
        }
        Color fontColor = Color.BLACK;
        if (removedFromGame) {
            fontColor = changeColorForDestroyedUnit(fontColor.brighter(), DESTROYED_UNIT_ALPHA);
        }
        float outerBorderWidth = 30f;
        float innerBorderWidth = 10f;
        float formStrokeWidth = 20f;

        if (stratOpsSymbols) {
            // White border to set off the icon from the background
            g2.setStroke(new BasicStroke(outerBorderWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
            g2.setColor(fontColor);
            g2.draw(STRAT_BASE_RECT);
            g2.fill(STRAT_BASE_RECT);

            g.setColor(fontColor);


            // Set a thin brush for filled areas (leave a thick brush for line symbols
            if ((entity instanceof Mek) || (entity instanceof ProtoMek)
                  || (entity instanceof VTOL) || (entity.isAero())) {
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            } else {
                g2.setStroke(new BasicStroke(formStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            }
            // Fill the form in player color / team color
            g.setColor(iconColor);
            g2.fill(form);

            g2.setColor(fontColor);
            if ((entity instanceof ProtoMek) || (entity instanceof Mek) || (entity instanceof Aero)) {
                String s = "";
                if (entity instanceof ProtoMek) {
                    s = "P";
                } else if ((entity instanceof Mek) && ((Mek) entity).isIndustrial()) {
                    s = "I";
                } else if (entity.getWeightClass() < 6) {
                    s = STRAT_WEIGHTS[entity.getWeightClass()];
                }
                if (!s.isBlank()) {
                    int fontType = Font.BOLD;
                    if (removedFromGame) {
                        fontType = Font.PLAIN;
                    }
                    var fontContext = new FontRenderContext(null, true, true);
                    var font = new Font(MMConstants.FONT_SANS_SERIF, fontType, 100);
                    FontMetrics currentMetrics = getFontMetrics(font);
                    int stringWidth = currentMetrics.stringWidth(s);
                    GlyphVector gv = font.createGlyphVector(fontContext, s);
                    g2.fill(gv.getOutline((int) STRAT_CX - (float) stringWidth / 2,
                          (float) STRAT_SYMBOL_SIZE.getHeight() / 3.0f));
                }
            } else if (entity instanceof MekWarrior) {
                g2.setColor(fontColor);
                g2.fillOval(-25, -25, 50, 50);
            }


            // Draw the unit icon in black
            g2.draw(form);

            // Rectangle border for all units
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(innerBorderWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g2.draw(STRAT_BASE_RECT);
        } else {
            // Standard symbols
            // White border to set off the icon from the background
            g2.setStroke(new BasicStroke(outerBorderWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
            g2.setColor(fontColor);
            g2.draw(form);

            // Fill the form in player color / team color
            g2.setColor(iconColor);
            g2.fill(form);

            // Black border
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(innerBorderWidth / 2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.draw(form);
            if (removedFromGame) {
                g2.draw(STRAT_DESTROYED);
                g2.fill(STRAT_DESTROYED);
            }
        }

        if (GUIP.showUnitDisplayNamesOnMinimap()) {
            // write unit ID and name to the minimap:
            g2.setColor(fontColor);
            int fontType = Font.BOLD;
            if (removedFromGame) {
                fontType = Font.PLAIN;
            }
            g2.setStroke(new BasicStroke(innerBorderWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            String s = entity.getShortName();
            var fontContext = new FontRenderContext(null, true, true);
            var font = new Font(MMConstants.FONT_SANS_SERIF, fontType, 75);
            GlyphVector gv = font.createGlyphVector(fontContext, s);
            g2.fill(gv.getOutline((float) -STRAT_SYMBOL_SIZE.getWidth() / 3f,
                  (float) -STRAT_SYMBOL_SIZE.getHeight() / 5 * 4));

        }
        // If the unit is destroyed, it gets a strike on it.
        if (removedFromGame) {
            g2.setColor(fontColor);
            g2.setStroke(new BasicStroke(formStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            if (stratOpsSymbols) {
                g2.draw(STRAT_DESTROYED);
            } else {
                g2.draw(STD_DESTROYED);
            }
        }

        g2.setStroke(new BasicStroke(innerBorderWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

        if (GUIP.getDrawFacingArrowsOnMiniMap() && !removedFromGame && !entity.isInfantry()) {
            // draw facing arrow
            var facing = entity.getFacing();
            if (facing > -1) {
                g2.setColor(fontColor);
                g2.rotate(Math.toRadians(facing * 60));
                g2.draw(FACING_ARROW);
                g.setColor(iconColor);
                g2.fill(FACING_ARROW);
            }
        }

        g2.setTransform(saveTransform);

        // Create a colored circle if this is the selected unit
        Entity se = (clientGui == null) ?
              null :
              clientGui instanceof ClientGUI ? ((ClientGUI) clientGui).getDisplayedUnit() : null;

        if (entity == se) {
            int rad = stratOpsSymbols ? 2 * unitSize - 1 : unitSize + unitSize / 2;
            Color color = GUIP.getUnitSelectedColor();
            g2.setColor(color.darker());
            g2.setStroke(new BasicStroke((float) unitSize / 5 + 1));
            g2.drawOval(baseX - rad, baseY - rad, rad * 2, rad * 2);
        }

        g2.setStroke(saveStroke);
    }


    /** Draws the symbol for a single entity. Checks visibility in double blind. */
    private void paintSensor(Graphics g, Entity entity) {
        if (EntityVisibilityUtils.onlyDetectedBySensors(getLocalPlayer(), entity)) {
            // This unit is visible only as a sensor Return, we dont know the range of its sensor yet
            return;
        } else if (!EntityVisibilityUtils.detectedOrHasVisual(getLocalPlayer(), game, entity)) {
            // This unit is not visible, don't draw it
            return;
        }

        int maxSensorRange = 0;
        int minSensorRange = 0;
        int ecmRange = entity.getECMRange();
        boolean ecmActive = entity.hasActiveECM();

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS)) {
            int bracket = Compute.getSensorRangeBracket(entity, null, null);
            int range = Compute.getSensorRangeByBracket(game, entity, null, null);

            maxSensorRange = bracket * range;
            minSensorRange = Math.max((bracket - 1) * range, 0);
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
                minSensorRange = 0;
            }
        }


        // Choose player or team color depending on preferences
        Color iconColor = entity.getOwner().getColour().getColour(false);
        if (GUIP.getTeamColoring() && (client != null)) {
            boolean isLocalTeam = (getLocalPlayer() != null) && (entity.getOwner().getTeam()
                  == getLocalPlayer().getTeam());
            boolean isLocalPlayer = entity.getOwner().equals(getLocalPlayer());
            if (isLocalPlayer) {
                iconColor = GUIP.getMyUnitColor();
            } else if (isLocalTeam) {
                iconColor = GUIP.getAllyUnitColor();
            } else {
                iconColor = GUIP.getEnemyUnitColor();
            }
        }
        Graphics2D g2 = (Graphics2D) g;
        Stroke saveStroke = g2.getStroke();

        g2.setStroke(new BasicStroke(2));
        Color iconColorSemiTransparent = new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 200);
        g2.setColor(iconColorSemiTransparent);

        var origin = entity.getPosition();
        if (maxSensorRange > 0) {
            paintSensorRange(maxSensorRange, true, origin, g2);
        }
        if (minSensorRange > 0) {
            paintSensorRange(minSensorRange, false, origin, g2);
        }
        if (ecmActive && ecmRange > 0) {
            Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                  0, new float[] { 6, 3 }, 3);
            g2.setStroke(dashed);
            paintSensorRange(ecmRange, true, origin, g2);
        }

        g2.setStroke(saveStroke);
    }

    private void paintSensorRange(int sensorRange, boolean offsetOut, Coords origin, Graphics2D g2) {
        int xo;
        int yo;
        var sensor = new Path2D.Double();

        var internalOrExternal = offsetOut ? 1 : -1;
        for (int i = 0; i < 6; i++) {
            var movingCoord = origin.translated(i, sensorRange + internalOrExternal);
            xo = coordsXToPixel(movingCoord.getX());
            yo = coordsYtoPixel(movingCoord.getY(), movingCoord.getX());
            if (i == 0) {
                sensor.moveTo(xo, yo);
            } else {
                sensor.lineTo(xo, yo);
            }
        }
        sensor.closePath();
        g2.draw(sensor);
    }

    private int coordsYtoPixel(int y, int x) {
        return (2 * y + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom] + topMargin;
    }

    private int coordsXToPixel(int x) {
        return x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + leftMargin + HEX_SIDE[zoom];
    }

    /** Draws the road elements previously assembles into the roadHexes list. */
    private void paintRoads(Graphics g) {
        int exits;
        int baseX;
        int baseY;
        int x;
        int y;
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];
        g.setColor(terrainColors[ROAD]);
        for (int[] roadEntry : roadHexes) {
            x = roadEntry[0];
            y = roadEntry[1];
            baseX = (x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin + HEX_SIDE[zoom];
            baseY = (((2 * y) + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
            exits = roadEntry[2];
            // Is there a North exit?
            if (0 != (exits & 1)) {
                xPoints[0] = baseX - HALF_ROAD_WIDTH[zoom];
                yPoints[0] = baseY;
                xPoints[1] = baseX - HALF_ROAD_WIDTH[zoom];
                yPoints[1] = baseY - HEX_SIDE_BY_COS30[zoom];
                xPoints[2] = baseX + HALF_ROAD_WIDTH[zoom];
                yPoints[2] = baseY - HEX_SIDE_BY_COS30[zoom];
                xPoints[3] = baseX + HALF_ROAD_WIDTH[zoom];
                yPoints[3] = baseY;
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a North-East exit?
            if (0 != (exits & 2)) {
                xPoints[0] = baseX - HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[0] = baseY - HALF_ROAD_WIDTH_BY_COS30[zoom];
                xPoints[1] = Math.round((baseX + ((3 * HEX_SIDE[zoom]) / 4.0f)) - HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[1] = Math.round(baseY - (HEX_SIDE_BY_COS30[zoom] / 2.0f) - HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[2] = xPoints[1] + (2 * HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[2] = yPoints[1] + (2 * HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[3] = baseX + HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[3] = baseY + HALF_ROAD_WIDTH_BY_COS30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a South-East exit?
            if (0 != (exits & 4)) {
                xPoints[0] = baseX + HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[0] = baseY - HALF_ROAD_WIDTH_BY_COS30[zoom];
                xPoints[1] = Math.round(baseX + ((3 * HEX_SIDE[zoom]) / 4.0f) + HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[1] = Math.round((baseY + (HEX_SIDE_BY_COS30[zoom] / 2.0f)) - HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[2] = xPoints[1] - (2 * HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[2] = yPoints[1] + (2 * HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[3] = baseX - HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[3] = baseY + HALF_ROAD_WIDTH_BY_COS30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a South exit?
            if (0 != (exits & 8)) {
                xPoints[0] = baseX + HALF_ROAD_WIDTH[zoom];
                yPoints[0] = baseY;
                xPoints[1] = baseX + HALF_ROAD_WIDTH[zoom];
                yPoints[1] = baseY + HEX_SIDE_BY_COS30[zoom];
                xPoints[2] = baseX - HALF_ROAD_WIDTH[zoom];
                yPoints[2] = baseY + HEX_SIDE_BY_COS30[zoom];
                xPoints[3] = baseX - HALF_ROAD_WIDTH[zoom];
                yPoints[3] = baseY;
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a South-West exit?
            if (0 != (exits & 16)) {
                xPoints[0] = baseX + HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[0] = baseY + HALF_ROAD_WIDTH_BY_COS30[zoom];
                xPoints[1] = Math.round((baseX - ((3 * HEX_SIDE[zoom]) / 4.0f)) + HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[1] = Math.round(baseY + (HEX_SIDE_BY_COS30[zoom] / 2.0f) + HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[2] = xPoints[1] - (2 * HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[2] = yPoints[1] - (2 * HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[3] = baseX - HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[3] = baseY - HALF_ROAD_WIDTH_BY_COS30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }
            // Is there a North-West exit?
            if (0 != (exits & 32)) {
                xPoints[0] = baseX - HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[0] = baseY + HALF_ROAD_WIDTH_BY_COS30[zoom];
                xPoints[1] = Math.round(baseX - ((3 * HEX_SIDE[zoom]) / 4.0f) - HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[1] = Math.round((baseY - (HEX_SIDE_BY_COS30[zoom] / 2.0f)) + HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[2] = xPoints[1] + (2 * HALF_ROAD_WIDTH_BY_SIN30[zoom]);
                yPoints[2] = yPoints[1] - (2 * HALF_ROAD_WIDTH_BY_COS30[zoom]);
                xPoints[3] = baseX + HALF_ROAD_WIDTH_BY_SIN30[zoom];
                yPoints[3] = baseY - HALF_ROAD_WIDTH_BY_COS30[zoom];
                g.drawPolygon(xPoints, yPoints, 4);
                g.fillPolygon(xPoints, yPoints, 4);
            }

        }
    }

    /** If the given hex contains ROAD or BRIDGE, adds an entry to roadHexes. */
    private void addRoadElements(Hex hex, int boardX, int boardY) {
        if (hex.containsAnyTerrainOf(ROAD, BRIDGE)) {
            var terrain = hex.getAnyTerrainOf(ROAD, BRIDGE);
            roadHexes.add(new int[] { boardX, boardY, terrain.getExits() });
        }
    }

    private Color terrainColor(Hex hex) {
        Color terrColor = terrainColors[0];
        if (hex.getLevel() < 0) {
            terrColor = SINKHOLE;
        }

        int terrain = 0;
        // Check for Smoke and Fire - this overrides any other colors
        if (hex.containsTerrain(SMOKE) && hex.containsTerrain(FIRE)) {
            terrColor = SMOKE_AND_FIRE;
            // Check for Fire - this overrides any other colors
        } else if (hex.containsTerrain(FIRE)) {
            terrColor = terrainColors[FIRE];
        } else { // Otherwise, color based on terrains - higher valued terrains take color
            // precedence
            for (int j = terrainColors.length - 1; j >= 0; j--) {
                if ((hex.getTerrain(j) != null) && (terrainColors[j] != null)) {
                    if ((j == ROAD) || (j == BRIDGE)) {
                        continue;
                    }
                    terrColor = terrainColors[j];
                    terrain = j;
                    // make heavy woods darker
                    if (((j == WOODS) || (j == JUNGLE)) && (hex.getTerrain(j).getLevel() == 2)) {
                        terrColor = HEAVY_WOODS;
                    }
                    if (((j == WOODS) || (j == JUNGLE)) && (hex.getTerrain(j).getLevel() > 2)) {
                        terrColor = ULTRA_HEAVY_WOODS;
                    }
                    break;
                }
            }
        }
        return switch (terrain) {
            case 0, WOODS, JUNGLE, ROUGH, RUBBLE, WATER, PAVEMENT, ICE, FIELDS ->
                  adjustByLevel(terrColor, Math.abs(hex.floor()));
            case FUEL_TANK, BUILDING -> adjustByLevel(terrColor, Math.abs(hex.ceiling()));
            default -> terrColor;
        };
    }

    private Color adjustByLevel(Color color, int level) {
        if (level > 10) {
            level = 10;
        }
        int r = color.getRed() - (level * 15);
        int g = color.getGreen() - (level * 15);
        int b = color.getBlue() - (level * 15);
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

    /** Returns a Board Coord for a given x and y pixel position. */
    private Coords translateCoords(int x, int y) {
        int gridX = (x / (HEX_SIDE_BY_SIN30[zoom] + HEX_SIDE[zoom]));
        int restX = x % (HEX_SIDE_BY_SIN30[zoom] + HEX_SIDE[zoom]);
        int gridY = (y / (2 * HEX_SIDE_BY_COS30[zoom]));
        int restY = y % (2 * HEX_SIDE_BY_COS30[zoom]);

        boolean evenColumn = (gridX & 1) == 0;

        if (restY < HEX_SIDE_BY_COS30[zoom]) {
            if (evenColumn) {
                if (restX < ((((restY - HEX_SIDE_BY_COS30[zoom])
                      * HEX_SIDE_BY_SIN30[zoom]) / HEX_SIDE_BY_COS30[zoom]) * -1)) {
                    gridX--;
                    gridY--;
                }
            } else {
                if (restX < ((restY * HEX_SIDE_BY_SIN30[zoom]) / HEX_SIDE_BY_COS30[zoom])) {
                    gridX--;
                } else {
                    gridY--;
                }
            }
        } else {
            if (evenColumn) {
                if (restX < (((restY - HEX_SIDE_BY_COS30[zoom])
                      * HEX_SIDE_BY_SIN30[zoom]) / HEX_SIDE_BY_COS30[zoom])) {
                    gridX--;
                }
            } else {
                if (restX < ((((restY - (2 * HEX_SIDE_BY_COS30[zoom]))
                      * HEX_SIDE_BY_SIN30[zoom]) / HEX_SIDE_BY_COS30[zoom]) * -1)) {
                    gridX--;
                }
            }
        }
        if (gridX < 0) {
            gridX = 0;
        }
        if (gridY < 0) {
            gridY = 0;
        }

        return new Coords(gridX, gridY);
    }

    /** Zooms out (smaller hexes), if possible. */
    private void zoomOut() {
        if (zoom > MIM_ZOOM) {
            zoom--;
            initializeMap();
            GUIP.setMinimapZoom(zoom);
        }
    }

    /** Zooms in (larger hexes), if possible. */
    private void zoomIn() {
        if (zoom < MAX_ZOOM) {
            zoom++;
            initializeMap();
            GUIP.setMinimapZoom(zoom);
        }
    }

    public void resetZoom() {
        zoom = MIM_ZOOM;
        initializeMap();
        GUIP.setMinimapZoom(zoom);
    }

    private void processMouseRelease(int x, int y, int modifiers) {
        if (!new Rectangle(getSize()).contains(x, y)) {
            return;
        }
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            bv.checkLOS(translateCoords(x - leftMargin, y - topMargin));
        }
        if (dragging) {
            return;
        }
        if (y <= getSize().height - BUTTON_HEIGHT) {
            // This is a click on the map itself
            centerOnPos(x, y);
        } else {
            // This is a click on the green control button bar
            if (minimized) {
                setSize(getSize().width, heightBuffer);
                mapImage = ImageUtil.createAcceleratedImage(getSize().width, heightBuffer);
                minimized = false;
                initializeMap();
            } else {
                if (x < BUTTON_HEIGHT) {
                    zoomOut();
                } else if ((x < 2 * BUTTON_HEIGHT) && (zoom >= MIM_ZOOM_FOR_HEIGHT)) {
                    setHeightDisplay(((++heightDisplayMode) > NBR_HEIGHT_MODES) ? 0 : heightDisplayMode);
                } else if ((x < 3 * BUTTON_HEIGHT) && (zoom >= MIM_ZOOM_FOR_HEIGHT)) {
                    setSymbolsDisplay(((++symbolsDisplayMode) > NBR_SYMBOLS_MODES) ? 0 : symbolsDisplayMode);
                } else if (x > (getSize().width - BUTTON_HEIGHT)) {
                    zoomIn();
                } else {
                    heightBuffer = getSize().height;
                    setSize(getSize().width, BUTTON_HEIGHT);
                    mapImage = ImageUtil.createAcceleratedImage(Math.max(1, getSize().width), BUTTON_HEIGHT);
                    minimized = true;
                    initializeMap();
                }
            }
        }
    }

    private void setSensorRangeDisplay(boolean state) {
        drawSensorRangeOnMiniMap = state;
        GUIP.setDrawSensorRangeOnMiniMap(state);
        initializeMap();
    }

    private void setFacingArrowsDisplay(boolean state) {
        drawFacingArrowsOnMiniMap = state;
        GUIP.setDrawFacingArrowsOnMiniMap(state);
        initializeMap();
    }

    private void setPaintBordersDisplay(boolean state) {
        paintBorders = state;
        GUIP.setPaintBorders(state);
        initializeMap();
    }

    private void setSymbolsDisplay(int i) {
        symbolsDisplayMode = i;
        GUIP.setMiniMapSymbolsDisplayMode(i);
        initializeMap();
    }

    private void setHeightDisplay(int i) {
        heightDisplayMode = i;
        GUIP.setMinimapHeightDisplayMode(i);
        initializeMap();
    }

    /**
     * Changes the currently shown {@link BoardView} to this minimap's own board.
     */
    private void ShowThisBoardView() {
        if (clientGui instanceof AbstractClientGUI abstractClientGUI) {
            abstractClientGUI.showBoardView(boardId);
        }
    }

    /**
     * Centers the BoardView connected to the Minimap on x, y in the Minimap's pixel coordinates.
     */
    private void centerOnPos(double x, double y) {
        ShowThisBoardView();
        bv.centerOnPointRel(
              ((x - leftMargin)) / ((HEX_SIDE_BY_SIN30[zoom] + HEX_SIDE[zoom]) * board.getWidth()),
              ((y - topMargin)) / (2 * HEX_SIDE_BY_COS30[zoom] * board.getHeight()));
        bv.stopSoftCentering();
        repaint();
    }

    private final BoardListener boardListener = new BoardListenerAdapter() {
        @Override
        public void boardNewBoard(BoardEvent b) {
            initializeMap();
        }

        @Override
        public void boardChangedHex(BoardEvent b) {
            // This must be tolerant since it might be called without notifying us of the board size first
            int x = b.getCoords().getX();
            int y = b.getCoords().getY();
            if ((x >= dirty.length) || (y >= dirty[x].length)) {
                dirtyMap = true;
            } else {
                dirty[x / 10][y / 10] = true;
            }
            refreshMap();
        }
    };

    BoardViewListener boardViewListener = new BoardViewListenerAdapter() {
        @Override
        public void hexCursor(BoardViewEvent b) {
            update();
        }

        @Override
        public void hexMoused(BoardViewEvent b) {
            repaint();
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
            refreshMap();
        }

        @Override
        public void secondLOSHex(BoardViewEvent b) {
            secondLOS = b.getCoords();
            refreshMap();
        }

        private void update() {
            firstLOS = null;
            secondLOS = null;
            refreshMap();
        }
    };

    MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.getButton() == MouseEvent.BUTTON1) {
                Point mapPoint = SwingUtilities.convertPoint(dialog, me.getX(), me.getY(), MinimapPanel.this);
                processMouseRelease(mapPoint.x, mapPoint.y, me.getModifiersEx());
                dragging = false;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger()) {
                showPopup(me);
            } else {
                ShowThisBoardView();
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger()) {
                showPopup(me);
            } else {
                ShowThisBoardView();
            }
        }

    };

    MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {

        @Override
        public void mouseDragged(MouseEvent me) {
            Point mapPoint = SwingUtilities.convertPoint(dialog, me.getX(), me.getY(), MinimapPanel.this);
            if (new Rectangle(getSize()).contains(mapPoint.x, mapPoint.y)) {
                if (!dragging) {
                    dragging = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                centerOnPos(mapPoint.x, mapPoint.y);
            }
        }
    };

    MouseWheelListener mouseWheelListener = new MouseWheelListener() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent we) {
            Point mapPoint = SwingUtilities.convertPoint(dialog, we.getX(), we.getY(), MinimapPanel.this);
            if (new Rectangle(getSize()).contains(mapPoint.x, mapPoint.y)) {
                if (we.getWheelRotation() > 0 ^ GUIP.getMouseWheelZoomFlip()) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            }
        }
    };

    ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent ce) {
            refreshMap();
        }
    };


    private void showPopup(MouseEvent me) {
        ScalingPopup popup = new ScalingPopup();

        JMenu zoomMenu = new JMenu(Messages.getString("Minimap.menu.Zoom", zoom));

        zoomMenu.add(menuItem(Messages.getString("Minimap.menu.ZoomIn"),
              ACTION_ZOOM_IN,
              zoom != MAX_ZOOM,
              l -> this.zoomIn(),
              false));
        zoomMenu.add(menuItem(Messages.getString("Minimap.menu.ZoomOut"),
              ACTION_ZOOM_OUT,
              zoom != MIM_ZOOM,
              l -> this.zoomOut(),
              false));
        popup.add(zoomMenu);

        JMenu heightMenu = new JMenu(Messages.getString("Minimap.menu.ShowHeight"));
        heightMenu.add(menuItem(Messages.getString("Minimap.menu.ShowHeightNone"),
              ACTION_HEIGHT_NONE,
              zoom >= MIM_ZOOM_FOR_HEIGHT,
              l -> this.setHeightDisplay(SHOW_NO_HEIGHT),
              heightDisplayMode == SHOW_NO_HEIGHT));
        heightMenu.add(menuItem(Messages.getString("Minimap.menu.ShowHeightGround"),
              ACTION_HEIGHT_GROUND,
              zoom >= MIM_ZOOM_FOR_HEIGHT,
              l -> this.setHeightDisplay(SHOW_GROUND_HEIGHT),
              heightDisplayMode == SHOW_GROUND_HEIGHT));
        heightMenu.add(menuItem(Messages.getString("Minimap.menu.ShowHeightBuilding"),
              ACTION_HEIGHT_BUILDING,
              zoom >= MIM_ZOOM_FOR_HEIGHT,
              l -> this.setHeightDisplay(SHOW_BUILDING_HEIGHT),
              heightDisplayMode == SHOW_BUILDING_HEIGHT));
        heightMenu.add(menuItem(Messages.getString("Minimap.menu.ShowHeightTotal"),
              ACTION_HEIGHT_TOTAL,
              zoom >= MIM_ZOOM_FOR_HEIGHT,
              l -> this.setHeightDisplay(SHOW_TOTAL_HEIGHT),
              heightDisplayMode == SHOW_TOTAL_HEIGHT));
        popup.add(heightMenu);

        JMenu symbolsMenu = new JMenu(Messages.getString("Minimap.menu.ShowSymbols"));
        symbolsMenu.add(menuItem(Messages.getString("Minimap.menu.ShowSymbolsNoSymbols"),
              ACTION_SYMBOLS_NO,
              true,
              l -> this.setSymbolsDisplay(SHOW_NO_SYMBOLS),
              symbolsDisplayMode == SHOW_NO_SYMBOLS));
        symbolsMenu.add(menuItem(Messages.getString("Minimap.menu.ShowSymbolsSymbols"),
              ACTION_SYMBOLS_SHOW,
              true,
              l -> this.setSymbolsDisplay(SHOW_SYMBOLS),
              symbolsDisplayMode == SHOW_SYMBOLS));

        JCheckBoxMenuItem toggleDrawSensor = new JCheckBoxMenuItem(Messages.getString(
              "Minimap.menu.ToggleShowSensorRange"));
        toggleDrawSensor.addActionListener(l -> setSensorRangeDisplay(!drawSensorRangeOnMiniMap));
        toggleDrawSensor.setSelected(drawSensorRangeOnMiniMap);
        symbolsMenu.add(toggleDrawSensor);


        JCheckBoxMenuItem toggleDrawFacing = new JCheckBoxMenuItem(Messages.getString(
              "Minimap.menu.ToggleDrawFacingArrows"));
        toggleDrawFacing.addActionListener(l -> setFacingArrowsDisplay(!drawFacingArrowsOnMiniMap));
        toggleDrawFacing.setSelected(drawFacingArrowsOnMiniMap);
        symbolsMenu.add(toggleDrawFacing);

        JCheckBoxMenuItem togglePaintBorders = new JCheckBoxMenuItem(Messages.getString(
              "Minimap.menu.ToggleDrawHexBorder"));
        togglePaintBorders.addActionListener(l -> setPaintBordersDisplay(!paintBorders));
        togglePaintBorders.setSelected(paintBorders);
        symbolsMenu.add(togglePaintBorders);

        popup.add(symbolsMenu);
        popup.show(this, me.getX(), me.getY());
    }

    public JCheckBoxMenuItem menuItem(String text, String cmd, boolean enabled,
          ActionListener listener, boolean checked) {
        JCheckBoxMenuItem result = new JCheckBoxMenuItem(text);
        result.setActionCommand(cmd);
        result.addActionListener(listener);
        result.setEnabled(enabled);
        result.setSelected(checked);
        return result;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.MM_SYMBOL)
              || e.getName().equals(GUIPreferences.TEAM_COLORING)) {
            refreshMap();
        }
    }

}
