/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.board.BoardEvent;
import megamek.common.event.board.BoardListenerAdapter;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.moves.MovePath;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.EntityVisibilityUtils;
import megamek.common.units.UnitLocation;

/** Thin Swing adapter. Reuses MegaMek's tileset, visibility checks, movement path, and actual phase buttons. */
final class GpuBoardSource implements AutoCloseable {
    public record UiPreferences(int hintMode, int hintKey, float scale) { }
    private static final String HINT_MODE = "GpuBoardHintMode";
    private static final String HINT_KEY = "GpuBoardHintKey";
    public record Frame(BoardScene scene, List<BoardScene.Movement> movements, BoardScene.Context context,
          List<BoardScene.Command> globalCommands, BoardScene.Pixels hud, String tooltip,
          BoardView.CenterRequest centerRequest, long boardGeneration, String actorName) { }

    private volatile BoardView view;
    private final Supplier<JComponent> phasePanel;
    private GpuBoardActions actions;
    volatile UiPreferences uiPreferences;
    private final Map<Image, BoardScene.Pixels> unitImages = new IdentityHashMap<>();
    private final List<BoardScene.Movement> pendingMoves = new ArrayList<>();
    private final Timer timer;
    private final GameListenerAdapter gameListener;
    private final BoardListenerAdapter boardListener;
    private final IPreferenceChangeListener preferenceListener = event -> {
        if (ClientPreferences.MAP_TILESET.equals(event.getName())) {
            dirtyTerrain();
        } else if (GUIPreferences.GUI_SCALE.equals(event.getName())) {
            uiPreferences = new UiPreferences(uiPreferences.hintMode(), uiPreferences.hintKey(),
                  GUIPreferences.getInstance().getGUIScale());
        }
    };
    private Board board;
    private List<BoardScene.Tile> tiles = List.of();
    private boolean terrainDirty = true;
    private volatile boolean closed;
    private Frame frame;
    private Coords contextCoords;
    private volatile Dimension viewport = new Dimension(1, 1);
    private volatile Coords hoverCoords;
    private boolean overlayGesture;
    private volatile Point pointer = new Point(-1, -1);
    private long boardGeneration;
    private volatile Rectangle visibleArea = new Rectangle(0, 0, 16, 16);

    public void setVisibleArea(Rectangle area) {
        visibleArea = new Rectangle(area);
    }

    public void setViewport(int width, int height) {
        viewport = new Dimension(Math.max(1, width), Math.max(1, height));
    }

    public void setHover(Coords coords) {
        hoverCoords = coords;
    }

    public void setPointer(int x, int y) {
        pointer = new Point(x, y);
    }

    BoardView currentView() {
        return view;
    }

    public GpuBoardSource(BoardView view, Supplier<JComponent> phasePanel) {
        requireSwingThread();
        this.view = view;
        this.phasePanel = phasePanel;
        GUIPreferences preferences = GUIPreferences.getInstance();
        uiPreferences = new UiPreferences(preferences.getInt(HINT_MODE), preferences.getInt(HINT_KEY),
              preferences.getGUIScale());
        actions = new GpuBoardActions(view, phasePanel, () -> closed || this.view != view, this::refresh);
        boardListener = new BoardListenerAdapter() {
            @Override
            public void boardNewBoard(BoardEvent event) {
                dirtyTerrain();
            }

            @Override
            public void boardChangedHex(BoardEvent event) {
                dirtyTerrain();
            }

            @Override
            public void boardChangedAllHexes(BoardEvent event) {
                dirtyTerrain();
            }
        };
        gameListener = new GameListenerAdapter() {
            @Override
            public void gameEntityChange(GameEntityChangeEvent event) {
                BoardView eventView = GpuBoardSource.this.view;
                // BoardView consumes the event's Vector during playback. Copy it before leaving the event callback.
                List<UnitLocation> path = event.getMovePath() == null ? List.of() : List.copyOf(event.getMovePath());
                int entityId = event.getEntity().getId();
                EntityMovementType type = event.getEntity().moved;
                Entity old = event.getOldEntity();
                UnitLocation start = old == null || old.getPosition() == null ? null
                      : new UnitLocation(old.getId(), old.getPosition(), old.getFacing(), old.getElevation(),
                            old.getBoardId());
                SwingUtilities.invokeLater(() -> {
                    if (GpuBoardSource.this.view == eventView) {
                        Entity takeoff = old == null ? event.getEntity() : old;
                        captureMovement(entityId, start, path, type,
                            type == EntityMovementType.MOVE_JUMP ? takeoff.getAnyTypeMaxJumpMP() : 0);
                    }
                });
            }
        };
        view.game.addGameListener(gameListener);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(preferenceListener);
        preferences.addPreferenceChangeListener(preferenceListener);
        timer = new Timer(100, event -> refresh());
        timer.setCoalesce(true);
        try {
            refresh();
            timer.start();
        } catch (RuntimeException | Error failure) {
            close();
            throw failure;
        }
    }

    private static void requireSwingThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("GPU board game access must run on the Swing event thread");
        }
    }

    private void dirtyTerrain() {
        if (SwingUtilities.isEventDispatchThread()) {
            terrainDirty = true;
        } else {
            SwingUtilities.invokeLater(() -> terrainDirty = true);
        }
    }

        private void captureMovement(int entityId, UnitLocation start, List<UnitLocation> path, EntityMovementType type,
            int jumpMP) {
        if (closed || path.isEmpty()) {
            return;
        }
        Entity entity = view.game.getEntity(entityId);
        BoardView movingView = view;
        if (entity == null || !visible(entity) || sensorContact(entity)
              || path.stream().anyMatch(p -> p.boardId() != view.getBoardId() || p.coords() == null)) {
            return;
        }
        List<BoardScene.Waypoint> points = new ArrayList<>();
        if (start != null && start.boardId() == view.getBoardId()) {
            points.add(waypoint(start.coords(), start.elevation(), start.facing()));
        }
        for (UnitLocation location : path) {
            BoardScene.Waypoint point = waypoint(location.coords(), location.elevation(), location.facing());
            if (points.isEmpty() || !points.getLast().equals(point)) {
                points.add(point);
            }
        }
        // Publish the final visible state and its movement together, so a frame cannot jump to the end first.
        Frame next = capture();
        synchronized (this) {
            if (view == movingView) {
                pendingMoves.add(new BoardScene.Movement(entityId, view.getBoardId(), points, type, jumpMP));
            }
            frame = next;
        }
    }

    public void refresh() {
        requireSwingThread();
        if (!closed) {
            Frame next = capture();
            synchronized (this) {
                frame = next;
            }
        }
    }

    public synchronized Frame takeFrame() {
        Frame result = new Frame(frame.scene(), List.copyOf(pendingMoves), frame.context(), frame.globalCommands(),
              frame.hud(), frame.tooltip(), frame.centerRequest(), frame.boardGeneration(), frame.actorName());
        pendingMoves.clear();
        return result;
    }

    private Frame capture() {
        if (view.getClientgui() != null) {
            BoardView selectedView = view.getClientgui().getCurrentBoardView()
                  .filter(BoardView.class::isInstance).map(BoardView.class::cast).orElse(view);
            if (selectedView != view) {
                view = selectedView;
                actions = new GpuBoardActions(selectedView, phasePanel,
                      () -> closed || view != selectedView, this::refresh);
                contextCoords = null;
                unitImages.clear();
                terrainDirty = true;
                synchronized (this) {
                    pendingMoves.clear();
                }
            }
        }
        Board current = view.game.getBoard(view.getBoardId());
        if (current != board) {
            if (board != null) {
                board.removeBoardListener(boardListener);
            }
            board = current;
            boardGeneration++;
            board.addBoardListener(boardListener);
            terrainDirty = true;
        }
        if (terrainDirty) {
            List<BoardScene.Tile> nextTiles = new ArrayList<>();
            for (BoardView.PlanarHex hex : view.capturePlanarHexes(new Rectangle(0, 0, board.getWidth(), board.getHeight()))) {
                nextTiles.add(new BoardScene.Tile(hex.coords(), board.getHex(hex.coords()).getLevel(),
                        new BoardScene.Pixels(hex.ground()), new BoardScene.Pixels(hex.tactical()), hex.text()));
            }
            tiles = List.copyOf(nextTiles);
            terrainDirty = false;
        }
        List<BoardScene.Tile> painted = new ArrayList<>(tiles);
        boolean changed = false;
        for (BoardView.PlanarHex hex : view.capturePlanarHexes(visibleArea)) {
            int index = hex.coords().getX() * board.getHeight() + hex.coords().getY();
            BoardScene.Tile old = tiles.get(index);
            BoardScene.Pixels ground = BoardScene.Pixels.capture(hex.ground(), old.image());
            BoardScene.Pixels tactical = BoardScene.Pixels.capture(hex.tactical(), old.tactical());
            if (ground != old.image() || tactical != old.tactical() || !hex.text().equals(old.text())) {
                painted.set(index, new BoardScene.Tile(hex.coords(), old.elevation(), ground, tactical, hex.text()));
                changed = true;
            }
        }
        if (changed) {
            tiles = List.copyOf(painted);
        }
        List<BoardScene.Unit> units = new ArrayList<>();
        Map<Image, Boolean> usedImages = new IdentityHashMap<>();
        for (Entity entity : view.game.getEntitiesVector()) {
            if (!visible(entity)) {
                continue;
            }
            boolean sensor = sensorContact(entity);
            if (entity.getSecondaryPositions().isEmpty() || sensor) {
                units.add(unit(entity, -1, entity.getPosition(), sensor, usedImages));
            } else {
                entity.getSecondaryPositions().forEach((part, coords) ->
                      units.add(unit(entity, part, coords, false, usedImages)));
            }
        }
        unitImages.keySet().retainAll(usedImages.keySet());
        JComponent panel = phasePanel.get();
        List<BoardScene.Command> commands = actions.phaseCommands();
        BoardScene.Context nextContext = contextCoords == null ? null : new BoardScene.Context(contextCoords,
              actions.contextCommands(contextCoords));
        List<BoardScene.Command> nextGlobal = new ArrayList<>(actions.globalCommands());
        if (view.getClientgui() != null) {
            var gui = view.getClientgui();
            List<BoardScene.Command> boards = gui.boardViews().stream().map(boardView -> new BoardScene.Command(
                  "Map " + boardView.getBoardId(), true, () -> SwingUtilities.invokeLater(() -> {
                      if (!closed) {
                          gui.showBoardView(boardView.getBoardId());
                          refresh();
                      }
                  }))).toList();
            nextGlobal.add(new BoardScene.Command("boards", "Maps", "", true, false, boards, () -> { }));
        }
        view.overlayInput(MouseEvent.MOUSE_MOVED, pointer, viewport);
        BoardScene.Pixels nextHud = BoardScene.Pixels.capture(view.captureOverlayImage(viewport), frame == null ? null : frame.hud());
        String nextTooltip = GpuBoardActions.plainText(view.getHexTooltip(contextCoords == null ? hoverCoords : contextCoords));
        List<BoardScene.Waypoint> planned = new ArrayList<>();
        if (panel instanceof MovementDisplay movement) {
            MovePath path = movement.getPlannedMovement();
            if (path != null && path.getEntity().getBoardId() == view.getBoardId()) {
                Entity entity = path.getEntity();
                if (entity.getPosition() != null) {
                    planned.add(waypoint(entity.getPosition(), entity.getElevation(), entity.getFacing()));
                }
                path.getStepVector().stream().filter(step -> step.getPosition() != null).forEach(step ->
                      planned.add(waypoint(step.getPosition(), step.getElevation(), step.getFacing())));
            }
        }
        Point light = view.getTerrainLightDirection();
        BoardScene scene = new BoardScene(view.getBoardId(), board.getWidth(), board.getHeight(), tiles, units, planned,
              actions.actorId(), view.game.getPhase().localizedName(), commands,
              light == null || light.x == 0 && light.y == 0 ? null : new BoardScene.Light(light.x, -light.y));
        Entity actor = view.game.getEntity(actions.actorId());
        boolean knownActor = actor != null && (actor.getOwner().equals(view.getLocalPlayer())
              || visible(actor) && !sensorContact(actor));
        return new Frame(scene, List.of(), nextContext, List.copyOf(nextGlobal), nextHud, nextTooltip,
              view.getCenterRequest(), boardGeneration, knownActor ? actor.getShortName() : "");
    }

    private boolean visible(Entity entity) {
        return entity.getPosition() != null && entity.getBoardId() == view.getBoardId()
              && EntityVisibilityUtils.detectedOrHasVisual(view.getLocalPlayer(), view.game, entity);
    }

    private boolean sensorContact(Entity entity) {
        return EntityVisibilityUtils.onlyDetectedBySensors(view.getLocalPlayer(), entity);
    }

    private BoardScene.Unit unit(Entity entity, int part, Coords coords, boolean sensor,
          Map<Image, Boolean> usedImages) {
        Image image = sensor ? view.getRadarBlipImage() : view.getTileManager().textureFor(entity, part);
        usedImages.put(image, true);
        BoardScene.Pixels pixels = unitImages.computeIfAbsent(image, this::copyImage);
        int facing = sensor ? 0 : view.getTileManager().facingFor(entity);
        float elevation = sensor ? 0 : entity.getElevation();
        if (!sensor && (entity.isAirborne() || entity.isAirborneVTOLorWIGE())) {
            elevation = Math.max(2, elevation);
        }
        return new BoardScene.Unit(entity.getId(), part, sensor ? Messages.getString("BoardView1.sensorReturn")
              : entity.getShortName(),
              waypoint(coords, elevation, facing), pixels, sensor,
              BoardScene.Pixels.capture(view.captureUnitAnnotations(entity, part),
                  frame == null ? null : frame.scene().units().stream()
                      .filter(unit -> unit.id() == entity.getId() && unit.part() == part)
                          .map(BoardScene.Unit::annotations).findFirst().orElse(null)),
              sensor ? 1 : entity.height() + 1);
    }

    private BoardScene.Waypoint waypoint(Coords coords, float relativeElevation, int facing) {
        Hex hex = board == null ? null : board.getHex(coords);
        return new BoardScene.Waypoint(coords, relativeElevation + (hex == null ? 0 : hex.getLevel()), facing);
    }

    private BoardScene.Pixels copyImage(Image source) {
        ImageIcon loaded = new ImageIcon(source);
        BufferedImage copy = new BufferedImage(Math.max(1, loaded.getIconWidth()), Math.max(1, loaded.getIconHeight()),
              BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(loaded.getImage(), 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return new BoardScene.Pixels(copy);
    }

    public void inspect(Coords coords) {
        SwingUtilities.invokeLater(() -> {
            if (!closed) {
                contextCoords = coords;
                refresh();
            }
        });
    }

    void saveHints(int mode, int key) {
        SwingUtilities.invokeLater(() -> {
            GUIPreferences.getInstance().setValue(HINT_MODE, mode);
            GUIPreferences.getInstance().setValue(HINT_KEY, key);
        });
    }

    public void overlayInput(int event, int x, int y, Runnable unhandled) {
        SwingUtilities.invokeLater(() -> {
            if (closed) {
                return;
            }
            boolean handled = view.overlayInput(event, new Point(x, y), viewport);
            if (event == MouseEvent.MOUSE_PRESSED) {
                overlayGesture = handled;
            } else {
                handled |= overlayGesture;
                if (event == MouseEvent.MOUSE_RELEASED) {
                    overlayGesture = false;
                }
            }
            if (!handled) {
                unhandled.run();
            }
        });
    }

    public void key(int keyCode, boolean down, int modifiers) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && view.getClientgui() != null) {
                MegaMekGUI.getKeyDispatcher().dispatchKeyEvent(new KeyEvent(view.getPanel(),
                      down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, System.currentTimeMillis(), modifiers,
                      keyCode, KeyEvent.CHAR_UNDEFINED));
                refresh();
            }
        });
    }

    public void stopKeys() {
        SwingUtilities.invokeLater(() -> {
            if (MegaMekGUI.getKeyDispatcher() != null) {
                MegaMekGUI.getKeyDispatcher().stopAllRepeating();
            }
        });
    }

    public void click(Coords coords, boolean doubleClick, int modifiers) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && coords != null && (view.game.getPhase().isOnMap()
                  || (modifiers & InputEvent.CTRL_DOWN_MASK) != 0)) {
                view.mouseAction(coords, doubleClick ? BoardView.BOARD_HEX_DOUBLE_CLICK : BoardView.BOARD_HEX_CLICK,
                      modifiers, 1);
                refresh();
            }
        });
    }

    public void hover(Coords coords, int modifiers) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && coords != null && view.game.getPhase().isOnMap()) {
                view.mouseAction(coords, BoardView.BOARD_HEX_DRAG, modifiers | InputEvent.BUTTON1_DOWN_MASK, 1);
            }
        });
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::close);
            return;
        }
        closed = true;
        timer.stop();
        view.game.removeGameListener(gameListener);
        PreferenceManager.getClientPreferences().removePreferenceChangeListener(preferenceListener);
        GUIPreferences.getInstance().removePreferenceChangeListener(preferenceListener);
        if (board != null) {
            board.removeBoardListener(boardListener);
        }
        unitImages.clear();
    }
}
