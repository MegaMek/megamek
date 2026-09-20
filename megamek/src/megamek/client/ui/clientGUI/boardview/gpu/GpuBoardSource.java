/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.ChatterBoxOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.OverlayImage;
import megamek.client.ui.clientGUI.boardview.sprite.EntitySprite;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.dialogs.clientDialogs.PlanetaryConditionsDialog;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.board.BoardEvent;
import megamek.common.event.board.BoardListenerAdapter;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.moves.MovePath;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.EntityVisibilityUtils;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.common.units.UnitLocation;

/** Thin Swing adapter. Reuses MegaMek's tileset, visibility checks, movement path, and actual phase buttons. */
final class GpuBoardSource implements AutoCloseable {
    public record UiPreferences(float scale) { }
    record HudLayer(BoardScene.Pixels pixels, int x, int y, OverlayImage.Fade fade, OverlayImage.Transition shiftY) {
        HudLayer(BoardScene.Pixels pixels, int x, int y, OverlayImage.Fade fade) {
            this(pixels, x, y, fade, OverlayImage.Transition.ZERO);
        }
    }
    record Hud(int width, int height, List<HudLayer> layers) { }
    public record Frame(BoardScene scene, List<BoardScene.Animation> timeline, BoardScene.Context context,
          List<BoardScene.Command> globalCommands, Hud hud, String tooltip,
          BoardView.CenterRequest centerRequest, long boardGeneration, String actorName,
          BoardAtmosphere.Settings scenarioAtmosphere, BoardScene.Attack attack) {
        Frame(BoardScene scene, List<BoardScene.Animation> animations, BoardScene.Context context,
              List<BoardScene.Command> globalCommands, Hud hud, String tooltip,
              BoardView.CenterRequest centerRequest, long boardGeneration, String actorName,
              BoardAtmosphere.Settings scenarioAtmosphere) {
            this(scene, animations, context, globalCommands, hud, tooltip, centerRequest, boardGeneration, actorName,
                  scenarioAtmosphere, null);
        }

        Frame(BoardScene scene, List<BoardScene.Animation> animations, BoardScene.Context context,
              List<BoardScene.Command> globalCommands, Hud hud, String tooltip,
              BoardView.CenterRequest centerRequest, long boardGeneration, String actorName) {
            this(scene, animations, context, globalCommands, hud, tooltip, centerRequest, boardGeneration, actorName,
                  BoardAtmosphere.DEFAULTS);
        }
        List<BoardScene.Movement> movements() {
            return timeline.stream().filter(BoardScene.Movement.class::isInstance).map(BoardScene.Movement.class::cast).toList();
        }

        List<BoardScene.Animation> animations() {
            return timeline.stream().filter(event -> !(event instanceof BoardScene.SceneUpdate)
                  && !(event instanceof BoardScene.Concealed)).toList();
        }
    }

    private final java.util.LinkedHashSet<java.util.UUID> receivedAttacks = new java.util.LinkedHashSet<>();

    private volatile BoardView view;
    private final Supplier<JComponent> phasePanel;
    private GpuBoardActions actions;
    volatile UiPreferences uiPreferences;
    private final Map<Image, BoardScene.Pixels> unitImages = new IdentityHashMap<>();
    private record AnnotationKey(int entityId, int part) { }
    private final Map<AnnotationKey, EntitySprite.Annotations> unitAnnotations = new HashMap<>();
    private final Map<Image, BoardScene.Pixels> overlayImages = new IdentityHashMap<>();
    private final UnitCamouflage camouflage = new UnitCamouflage();
    private final BoardScene.PixelPool terrainImages = new BoardScene.PixelPool();
    private final List<BoardScene.Animation> pendingEvents = new ArrayList<>();
    private final Timer timer;
    private final GameListenerAdapter gameListener;
    private final BoardListenerAdapter boardListener;
    private final IPreferenceChangeListener preferenceListener = event -> {
        if (ClientPreferences.MAP_TILESET.equals(event.getName())) {
            dirtyTerrain();
        } else if (GUIPreferences.GUI_SCALE.equals(event.getName())) {
            uiPreferences = new UiPreferences(GUIPreferences.getInstance().getGUIScale());
        }
    };
    private Board board;
    private List<BoardScene.Tile> tiles = List.of();
    private BoardFieldOfView fieldOfView = BoardFieldOfView.EMPTY;
    private boolean terrainDirty = true;
    private volatile boolean closed;
    private PlanetaryConditionsDialog conditionsDialog;
    /** Swing publishes chat focus for native camera/menu input; the BoardView owns the actual state. */
    private volatile boolean chatActive;
    private boolean suppressChatCharacter;
    private Frame frame;
    private Coords contextCoords;
    /** The GL thread publishes layout and raster sizes together; Swing owns overlay painting and hit testing. */
    private record OverlayViewport(Dimension size, Dimension pixels) { }
    private volatile OverlayViewport viewport = new OverlayViewport(new Dimension(1, 1), new Dimension(1, 1));
    private volatile Coords hoverCoords;
    private boolean overlayGesture;
    private volatile Point pointer = new Point(-1, -1);
    private long boardGeneration;
    private volatile Rectangle visibleArea = new Rectangle(0, 0, 16, 16);
    private Rectangle capturedArea;
    private long capturedRevision = -1;
    private GamePhase capturedPhase;

    public void setVisibleArea(Rectangle area) {
        visibleArea = new Rectangle(area);
    }

    public void setViewport(int width, int height, int pixelWidth, int pixelHeight) {
        viewport = new OverlayViewport(new Dimension(Math.max(1, width), Math.max(1, height)),
              new Dimension(Math.max(1, pixelWidth), Math.max(1, pixelHeight)));
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
        uiPreferences = new UiPreferences(preferences.getGUIScale());
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
            public void gameAttackResolved(megamek.common.event.GameAttackResolvedEvent event) {
                BoardView eventView = GpuBoardSource.this.view;
                onSwing(() -> {
                    if (GpuBoardSource.this.view == eventView) {
                        captureCombat(event);
                    }
                });
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent event) {
                BoardView eventView = GpuBoardSource.this.view;
                // BoardView consumes the event's Vector during playback. Copy it before leaving the event callback.
                List<UnitLocation> path = event.getMovePath() == null ? List.of() : List.copyOf(event.getMovePath());
                int entityId = event.getEntity().getId();
                EntityMovementType type = event.getEntity().moved;
                int moveMP = movementMP(event.getEntity(), type);
                Entity old = event.getOldEntity();
                UnitLocation start = old == null || old.getPosition() == null ? null
                      : new UnitLocation(old.getId(), old.getPosition(), old.getFacing(), old.getElevation(),
                            old.getBoardId(), old.getProneCause(), UnitLocation.Form.capture(old), old.getFallSide());
                int startAltitude = old == null ? 0 : old.getAltitude();
                Entity takeoff = old == null ? event.getEntity() : old;
                int jumpMP = type == EntityMovementType.MOVE_JUMP ? takeoff.getAnyTypeMaxJumpMP() : 0;
                onSwing(() -> {
                    if (GpuBoardSource.this.view == eventView) {
                        if (path.isEmpty()) {
                            refresh();
                        } else {
                            captureMovement(entityId, start, path, type, jumpMP, startAltitude, moveMP);
                        }
                    }
                });
            }

            @Override
            public void gameEntityNew(megamek.common.event.entity.GameEntityNewEvent event) {
                onSwing(GpuBoardSource.this::refresh);
            }

            @Override
            public void gameEntityRemove(megamek.common.event.entity.GameEntityRemoveEvent event) {
                BoardView eventView = GpuBoardSource.this.view;
                int condition = event.getEntity().getRemovalCondition();
                int id = event.getEntity().getId(), boardId = event.getEntity().getBoardId();
                onSwing(() -> {
                    if (closed || GpuBoardSource.this.view != eventView) { return; }
                    if (condition == megamek.common.interfaces.IEntityRemovalConditions.REMOVE_UNKNOWN) {
                        queueAnimation(new BoardScene.Concealed(id, boardId));
                    }
                    refresh();
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

    /** Packet handlers already run on Swing. Capturing here preserves each event inside a batched packet. */
    private static void onSwing(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
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
            int jumpMP, int startAltitude, int movementMP) {
        if (closed || path.isEmpty()) {
            return;
        }
        Entity entity = view.game.getEntity(entityId);
        BoardView movingView = view;
        if (entity == null || !visible(entity) || sensorContact(entity)
              || path.stream().anyMatch(p -> p.boardId() != view.getBoardId() || p.coords() == null)) {
            refresh();
            return;
        }
        List<BoardScene.Waypoint> points = new ArrayList<>();
        // An airborne visual plays at the altitude the unit has now, and at the altitude it started the move at for
        // its first point and when it has just landed, so flying stays on the board through climbs and landings.
        int flightAltitude = entity.getAltitude() > 0 ? entity.getAltitude() : startAltitude;
        if (start != null && start.boardId() == view.getBoardId()) {
            points.add(pathWaypoint(entity, start.coords(), start.elevation(), start.facing(),
                  startAltitude > 0 ? startAltitude : flightAltitude, start.form()).withProneCause(start.proneCause())
                  .withFallSide(start.fallSide()));
        }
        for (UnitLocation location : path) {
            var observedForm = location.form() != null ? location.form() : points.isEmpty() ? null : points.getLast().form();
            BoardScene.Waypoint point = pathWaypoint(entity, location.coords(), location.elevation(),
                  location.facing(), flightAltitude, observedForm).withProneCause(location.proneCause() != null ? location.proneCause()
                        : points.isEmpty() ? null : points.getLast().proneCause())
                  .withFallSide(location.proneCause() != null ? location.fallSide()
                        : points.isEmpty() ? null : points.getLast().fallSide());
            if (points.isEmpty() || !points.getLast().equals(point)) {
                points.add(point);
            }
        }
        // Older paths carry no conversion observations. Show their known final conversion at arrival.
        var finalForm = UnitLocation.Form.capture(entity);
        if (!points.isEmpty() && finalForm != null && path.stream().allMatch(location -> location.form() == null)) {
            var last = path.getLast();
            points.add(pathWaypoint(entity, last.coords(), last.elevation(), last.facing(), flightAltitude, finalForm)
                  .withProneCause(points.getLast().proneCause()).withFallSide(points.getLast().fallSide()));
        }
        // Publish the final visible state and its movement together, so a frame cannot jump to the end first.
        Frame next = capture();
        if (!points.isEmpty()) {
            points.set(0, supportedEndpoint(points.getFirst(), frame, entityId));
            points.set(points.size() - 1, supportedEndpoint(points.getLast(), next, entityId));
        }
        synchronized (this) {
            if (view == movingView) {
                var captured = next.scene().units().stream()
                      .filter(unit -> unit.id() == entityId && !unit.sensorContact()).findFirst().orElse(null);
                queueMovement(entity, captured, points, type, jumpMP, movementMP);
            }
            publishScene(next, false);
        }
    }

    /** Split only at observed conversion steps; normal travel keeps its one continuous acceleration interval. */
    private void queueMovement(Entity entity, BoardScene.Unit captured, List<BoardScene.Waypoint> points,
          EntityMovementType type, int jumpMP, int movementMP) {
        float gravity = view.game.getPlanetaryConditions().getGravity();
        if (captured == null || captured.model() == null || points.stream().noneMatch(point -> point.form() != null)) {
            queueAnimation(new BoardScene.Movement(entity.getId(), view.getBoardId(), points, type, jumpMP, movementMP, captured, gravity));
            return;
        }
        List<BoardScene.Waypoint> leg = new ArrayList<>();
        var form = points.getFirst().form();
        for (var point : points) {
            if (form != null && point.form() != null && form.mode() != point.form().mode()) {
                var before = inForm(captured, entity, leg.getLast(), form);
                var legStart = leg.getFirst();
                if (leg.stream().anyMatch(step -> !step.samePose(legStart))) {
                    queueAnimation(new BoardScene.Movement(entity.getId(), view.getBoardId(), leg, type, jumpMP, movementMP, before, gravity));
                }
                var after = inForm(captured, entity, point, point.form());
                queueAnimation(new BoardScene.Conversion(view.getBoardId(), before, after));
                leg = new ArrayList<>();
                form = point.form();
            }
            leg.add(point);
            if (form == null) { form = point.form(); }
        }
        if (leg.size() > 1) {
            queueAnimation(new BoardScene.Movement(entity.getId(), view.getBoardId(), leg, type, jumpMP, movementMP,
                  inForm(captured, entity, leg.getLast(), form), gravity));
        }
    }

    private BoardScene.Unit inForm(BoardScene.Unit unit, Entity entity, BoardScene.Waypoint location, UnitLocation.Form form) {
        var model = UnitModelSelection.inForm(unit.model(), entity, unit.part(), MMStaticDirectoryManager.getMekTileset(), form);
        return new BoardScene.Unit(unit.id(), unit.part(), unit.name(), location, unit.image(), false, unit.annotations(),
              unit.height(), form == null ? unit.airborne() : form.airborne(), model, unit.outlineRgb(), unit.footprint());
    }

    /** Copy authorized, then-visible appearance once; no Entity reaches the GL thread. */
    private void captureCombat(megamek.common.event.GameAttackResolvedEvent event) {
        requireSwingThread();
        var result = event.result();
        Entity attacker = event.attacker();
        Entity target = event.target() instanceof Entity entity ? entity : null;
        if (closed || result.attacker().boardId() != view.getBoardId() || attacker == null
              || !visible(attacker) || sensorContact(attacker)
              || (result.targetType() == Targetable.TYPE_ENTITY && target == null)
              || (target != null && (!visible(target) || sensorContact(target)))
              || !receivedAttacks.add(result.id())) {
            return;
        }
        if (receivedAttacks.size() > 2048) {
            receivedAttacks.removeFirst();
        }
        var usedImages = new IdentityHashMap<Image, Boolean>();
        var firing = unit(attacker, -1, result.attacker().coords(), false, usedImages);
        if (result.shot() != null && result.shot().launch() != null) {
            firing = inForm(firing, attacker, waypoint(result.attacker().coords(), result.attacker().elevation(), result.attacker().facing()),
                  result.attacker().form());
        }
        var receiving = target == null ? null : unit(target, -1, result.target().coords(), false, usedImages);
        var destination = receiving == null
              ? waypoint(result.target().coords(), result.target().elevation(), result.target().facing())
              : receiving.location();
        // A resolved attack packet precedes its damage packets. This checkpoint closes the preceding action.
        Frame before = capture();
        synchronized (this) {
            publishScene(before, true);
            queueAnimation(new BoardScene.Combat(result, firing, receiving, destination));
        }
    }

    private synchronized void queueAnimation(BoardScene.Animation animation) {
        if (animation instanceof BoardScene.SceneUpdate
              && !pendingEvents.isEmpty() && pendingEvents.getLast() instanceof BoardScene.SceneUpdate) {
            pendingEvents.removeLast();
        }
        if (pendingEvents.size() >= UnitPlayback.MAX_PENDING_EVENTS) {
            pendingEvents.clear();
        }
        pendingEvents.add(animation);
    }

    /** Consecutive captures share one checkpoint; animation boundaries are never coalesced. Swing owns capture. */
    private synchronized void publishScene(Frame next, boolean detectGear) {
        if (detectGear && frame != null && frame.boardGeneration() == next.boardGeneration()) {
            Map<Integer, BoardScene.Unit> previous = new HashMap<>();
            frame.scene().units().stream().filter(unit -> !unit.sensorContact())
                  .forEach(unit -> previous.put(unit.id(), unit));
            for (var old : previous.values()) {
                Entity entity = view.game.getEntity(old.id());
                if (entity != null && (!visible(entity) || sensorContact(entity))) {
                    queueAnimation(new BoardScene.Concealed(old.id(), next.scene().boardId()));
                }
            }
            for (var unit : next.scene().units()) {
                var old = previous.get(unit.id());
                if (UnitConversion.changes(old, unit)) {
                    queueAnimation(new BoardScene.Conversion(next.scene().boardId(), old, unit));
                }
                if (old != null && !unit.sensorContact() && UnitMotion.changesGear(old.location(), unit.location())) {
                    Entity entity = view.game.getEntity(unit.id());
                    queueAnimation(new BoardScene.Movement(unit.id(), next.scene().boardId(),
                          List.of(old.location(), unit.location()), EntityMovementType.MOVE_SAFE_THRUST, 0,
                          entity == null ? 0 : movementMP(entity, EntityMovementType.MOVE_SAFE_THRUST), unit));
                }
            }
        }
        if (frame == null || frame.boardGeneration() != next.boardGeneration() || !next.scene().samePlaybackState(frame.scene())
              || (!pendingEvents.isEmpty() && !(pendingEvents.getLast() instanceof BoardScene.SceneUpdate))) {
            queueAnimation(new BoardScene.SceneUpdate(next.scene()));
        }
        frame = next;
    }

    /** Copy the game's current capability once at the event boundary; the renderer never recalculates MP rules. */
    static int movementMP(Entity entity, EntityMovementType type) {
        if (entity instanceof Aero aero && aero.isAirborne()) {
            return Math.max(1, aero.getCurrentVelocity());
        }
        return switch (type) {
            case MOVE_JUMP -> entity.getJumpMP();
            case MOVE_SPRINT, MOVE_VTOL_SPRINT -> entity.getSprintMP();
            case MOVE_RUN, MOVE_VTOL_RUN, MOVE_SUBMARINE_RUN, MOVE_OVER_THRUST, MOVE_SKID -> entity.getRunMP();
            default -> entity.getWalkMP();
        };
    }

    /** A landed path endpoint uses the same whole-footprint support as its captured hull, not just its centre hex. */
    private static BoardScene.Waypoint supportedEndpoint(BoardScene.Waypoint point, Frame frame, int id) {
        if (frame == null || point.aeroState() != BoardScene.AeroState.LANDED) {
            return point;
        }
        return frame.scene().units().stream().filter(unit -> unit.id() == id && !unit.sensorContact()
                    && unit.footprint().size() > 1 && unit.location().coords().equals(point.coords())
                    && unit.location().aeroState() == BoardScene.AeroState.LANDED)
              .map(unit -> new BoardScene.Waypoint(point.coords(), unit.location().elevation(), point.facing(),
                    point.proneCause(), point.aeroState(), unit.footprint(), point.form(), point.fallSide())).findFirst().orElse(point);
    }

    public void refresh() {
        requireSwingThread();
        if (!closed) {
            Frame next = capture();
            synchronized (this) {
                publishScene(next, true);
            }
        }
    }

    public synchronized Frame takeFrame() {
        Frame result = new Frame(frame.scene(), List.copyOf(pendingEvents), frame.context(), frame.globalCommands(),
              frame.hud(), frame.tooltip(), frame.centerRequest(), frame.boardGeneration(), frame.actorName(),
              frame.scenarioAtmosphere(), frame.attack());
        pendingEvents.clear();
        return result;
    }

    private Frame capture() {
        if (view.getClientgui() != null) {
            BoardView selectedView = view.getClientgui().getCurrentBoardView()
                  .filter(BoardView.class::isInstance).map(BoardView.class::cast).orElse(view);
            if (selectedView != view) {
                view.releasePlanarCapture();
                view = selectedView;
                actions = new GpuBoardActions(selectedView, phasePanel,
                      () -> closed || view != selectedView, this::refresh);
                contextCoords = null;
                unitImages.clear();
                unitAnnotations.clear();
                terrainDirty = true;
                synchronized (this) {
                    pendingEvents.clear();
                }
            }
        }
        chatActive = view.getChatterBoxActive();
        OverlayViewport overlayViewport = viewport;
        view.overlayInput(MouseEvent.MOUSE_MOVED, pointer, overlayViewport.size(), overlayViewport.pixels());
        Hud nextHud = captureHud(overlayViewport);
        // A toggle starts on Swing. Publish its timeline before potentially expensive board/command capture,
        // so native rendering can already animate it while the rest of this scene snapshot is being prepared.
        synchronized (this) {
            if (frame != null && frame.scene().boardId() == view.getBoardId()) {
                frame = new Frame(frame.scene(), frame.timeline(), frame.context(), frame.globalCommands(), nextHud,
                      frame.tooltip(), frame.centerRequest(), frame.boardGeneration(), frame.actorName(),
                      frame.scenarioAtmosphere(), frame.attack());
            }
        }
        Board current = view.game.getBoard(view.getBoardId());
        if (current != board) {
            if (board != null) {
                board.removeBoardListener(boardListener);
            }
            board = current;
            boardGeneration++;
            synchronized (this) {
                pendingEvents.clear();
                receivedAttacks.clear();
            }
            board.addBoardListener(boardListener);
            terrainDirty = true;
        }
        boolean changedTerrain = terrainDirty;
        if (terrainDirty) {
            List<BoardScene.Tile> nextTiles = new ArrayList<>(Collections.nCopies(board.getWidth() * board.getHeight(), null));
            view.capturePlanarHexes(new Rectangle(0, 0, board.getWidth(), board.getHeight()), false,
                  hex -> nextTiles.set(hex.coords().getX() * board.getHeight() + hex.coords().getY(), tile(hex, null)));
            tiles = List.copyOf(nextTiles);
            terrainDirty = false;
        }
        Rectangle area = visibleArea;
        long revision = view.getPlanarRevision();
        if (changedTerrain || revision != capturedRevision || view.game.getPhase() != capturedPhase || !area.equals(capturedArea)) {
            List<BoardScene.Tile> painted = new ArrayList<>(tiles);
            for (int index = 0; index < tiles.size(); index++) {
                BoardScene.Tile old = tiles.get(index);
                if (old.tactical() != null && !area.contains(old.coords().getX(), old.coords().getY())) {
                    painted.set(index, new BoardScene.Tile(old.coords(), old.elevation(), old.waterDepth(), old.frozen(),
                          old.roadExits(), old.surface(), old.ground(), old.normals(), old.decals(), old.decalsWithoutLimbs(),
                          null, old.features(), old.text()));
                }
            }
            view.capturePlanarTactical(area, hex -> {
                int index = hex.coords().getX() * board.getHeight() + hex.coords().getY();
                BoardScene.Tile old = tiles.get(index);
                BoardScene.Tile next = new BoardScene.Tile(old.coords(), old.elevation(), old.waterDepth(), old.frozen(),
                      old.roadExits(), old.surface(), old.ground(), old.normals(), old.decals(), old.decalsWithoutLimbs(),
                      terrainImages.capture(hex.tactical(), old.tactical()), old.features(), hex.text());
                if (!next.equals(old)) {
                    painted.set(index, next);
                }
            });
            if (!painted.equals(tiles)) {
                tiles = List.copyOf(painted);
            }
            terrainImages.retain(tiles);
            fieldOfView = view.captureFieldOfView(area);
            capturedArea = area;
            capturedRevision = revision;
            capturedPhase = view.game.getPhase();
        }
        List<BoardScene.Unit> units = new ArrayList<>();
        camouflage.begin();
        Map<Image, Boolean> usedImages = new IdentityHashMap<>();
        for (Entity entity : view.game.getEntitiesVector()) {
            if (!visible(entity)) {
                continue;
            }
            boolean sensor = sensorContact(entity);
            boolean wholeModel = !sensor && GpuUnitModels.ENABLED
                  && MMStaticDirectoryManager.getMekTileset().modelFor(entity, -1) != null;
            if (entity.getSecondaryPositions().isEmpty() || sensor || wholeModel) {
                units.add(unit(entity, -1, entity.getPosition(), sensor, usedImages));
            } else {
                entity.getSecondaryPositions().forEach((part, coords) ->
                      units.add(unit(entity, part, coords, false, usedImages)));
            }
        }
        if (GpuUnitModels.ENABLED && GUIPreferences.getInstance().getShowWrecks()) {
            // BoardView already owns which removed units leave wrecks, including infantry/CVEP exceptions.
            var live = units.stream().map(BoardScene.Unit::id).collect(Collectors.toSet());
            view.getIsoWreckSprites().stream().map(sprite -> sprite.getEntity()).distinct()
                  .filter(entity -> !live.contains(entity.getId()) && visible(entity) && !sensorContact(entity))
                  .forEach(entity -> units.add(wreck(entity, usedImages)));
        }
        unitImages.keySet().retainAll(usedImages.keySet());
        unitAnnotations.values().removeIf(annotations -> !usedImages.containsKey(annotations.image()));
        camouflage.retain();
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
              light == null || light.x == 0 && light.y == 0 ? null : new BoardScene.Light(light.x, -light.y),
              firingLines(), view.getWeaponRangeSprites().stream().map(sprite -> new BoardScene.RangeBorder(
                    sprite.getPosition(), sprite.getBorders(),
                    FieldOfFireSprite.getFieldOfFireColor(sprite.getRangeBracket()).getRGB(),
                    FieldOfFireSprite.getRangeText(sprite.getRangeBracket()))).toList(),
              view.getBoardMarkers(), view.captureTacticalGeometry(),
              view.getWeaponRangeTextSprites().stream().map(sprite -> new BoardScene.RangeLabel(sprite.getPosition(),
                    FieldOfFireSprite.getFieldOfFireColor(sprite.getRangeBracket()).getRGB(),
                    FieldOfFireSprite.getRangeText(sprite.getRangeBracket()))).toList(), fieldOfView);
        Entity actor = view.game.getEntity(actions.actorId());
        boolean knownActor = actor != null && (actor.getOwner().equals(view.getLocalPlayer())
              || visible(actor) && !sensorContact(actor));
        return new Frame(scene, List.of(), nextContext, List.copyOf(nextGlobal), nextHud, nextTooltip,
              view.getCenterRequest(), boardGeneration, knownActor ? actor.getShortName() : "",
              BoardAtmosphere.fromScenario(view.game.getPlanetaryConditions(), board.isSpace()), actions.attackState());
    }

    private Hud captureHud(OverlayViewport layout) {
        List<OverlayImage> artwork = view.captureOverlayLayers(layout.size(), layout.pixels());
        List<HudLayer> layers = new ArrayList<>();
        Map<Image, BoardScene.Pixels> retained = new IdentityHashMap<>();
        for (int index = 0; index < artwork.size(); index++) {
            OverlayImage layer = artwork.get(index);
            BoardScene.Pixels pixels = overlayImages.get(layer.image());
            if (pixels == null) {
                BoardScene.Pixels previous = frame == null || index >= frame.hud().layers().size() ? null
                      : frame.hud().layers().get(index).pixels();
                pixels = BoardScene.Pixels.capture(layer.image(), previous);
            }
            retained.put(layer.image(), pixels);
            layers.add(new HudLayer(pixels, layer.x(), layer.y(), layer.fade(), layer.shiftY()));
        }
        overlayImages.clear();
        overlayImages.putAll(retained);
        return new Hud(layout.pixels().width, layout.pixels().height, List.copyOf(layers));
    }

    private boolean visible(Entity entity) {
        return entity.getPosition() != null && entity.getBoardId() == view.getBoardId()
              && EntityVisibilityUtils.detectedOrHasVisual(view.getLocalPlayer(), view.game, entity);
    }

    private List<BoardScene.FiringLine> firingLines() {
        List<BoardScene.FiringLine> result = new ArrayList<>();
        for (var sprite : view.getAttackSprites()) {
            Entity attacker = sprite.getAttackingEntity();
            Targetable target = sprite.getTargetedEntity();
            if (sprite.isHidden() || attacker.getPosition() == null || target.getPosition() == null
                  || !board.contains(attacker.getPosition()) || !board.contains(target.getPosition())) {
                continue;
            }
            for (EntityAction action : sprite.getActions()) {
                // ArtilleryAttackAction also represents direct artillery declared in the firing phase.
                boolean indirect = action instanceof ArtilleryAttackAction && switch (view.game.getPhase()) {
                    case TARGETING, TARGETING_REPORT, OFFBOARD, OFFBOARD_REPORT -> true;
                    default -> false;
                };
                if (action instanceof WeaponAttackAction weaponAttack) {
                    Entity weaponEntity = view.game.getEntity(action.getEntityId());
                    var weapon = weaponEntity == null ? null : weaponEntity.getEquipment(weaponAttack.getWeaponId());
                    indirect |= weapon != null && weapon.curMode().isIndirect();
                }
                result.add(new BoardScene.FiringLine(firingEndpoint(attacker), firingEndpoint(target),
                      attacker.getOwner().getColour().getColour().getRGB(), indirect));
            }
        }
        // Multiple weapons on one target share a trace, but direct and indirect fire remain distinct.
        return result.stream().distinct().toList();
    }

    private BoardScene.Waypoint firingEndpoint(Targetable target) {
        Coords coords = target.getPosition();
        if (target instanceof Entity entity) {
            if (sensorContact(entity)) {
                return waypoint(coords, 0.5f, 0);
            }
            return new BoardScene.Waypoint(coords, flightLevel(entity, coords) + (entity.height() + 1) * 0.5f, 0);
        }
        return waypoint(coords, target.getElevation() + Math.max(0.15f, target.getHeight() * 0.5f), 0);
    }

    private BoardScene.Tile tile(BoardView.PlanarHex pixels, BoardScene.Tile previous) {
        Hex hex = board.getHex(pixels.coords());
        return new BoardScene.Tile(pixels.coords(), hex.getLevel(),
              hex.containsTerrain(Terrains.WATER) ? Math.max(0, hex.terrainLevel(Terrains.WATER)) : -1,
              hex.containsTerrain(Terrains.ICE),
              hex.containsTerrain(Terrains.ROAD) ? hex.getTerrain(Terrains.ROAD).getExits() & 63 : 0,
              BoardFeatures.surface(hex),
              terrainImages.capture(pixels.terrain(), previous == null ? null : previous.ground()),
              terrainImages.capture(pixels.normals(), previous == null ? null : previous.normals()),
              terrainImages.captureOverlay(pixels.decals(), previous == null ? null : previous.decals()),
              terrainImages.capture(pixels.decalsWithoutLimbs(), previous == null ? null : previous.decalsWithoutLimbs()),
              terrainImages.capture(pixels.tactical(), previous == null ? null : previous.tactical()),
              BoardFeatures.capture(hex, pixels.coords(), pixels.structureModels()), pixels.text());
    }

    private boolean sensorContact(Entity entity) {
        return EntityVisibilityUtils.onlyDetectedBySensors(view.getLocalPlayer(), entity);
    }

    private BoardScene.Unit wreck(Entity entity, Map<Image, Boolean> usedImages) {
        var captured = unit(entity, -1, entity.getPosition(), false, usedImages);
        var model = captured.model();
        var state = model == null ? UnitModelState.capture(entity) : model.state();
        var pose = state.pose();
        var dead = new UnitModelState(state.structure(), state.appearance(),
              new UnitModelState.Pose(pose.proneCause(), pose.facing(), pose.secondaryFacing(), pose.form(), true));
        model = model == null ? new BoardScene.UnitModel("", "", "", 1, 0, BoardScene.LocationDamage.NONE, dead)
              : new BoardScene.UnitModel(model.asset(), model.fallback(), model.variant(), model.figures(), model.twist(), model.damage(), dead);
        Image image = view.getTileManager().wreckMarkerFor(entity, -1);
        usedImages.put(image, true);
        var pixels = unitImages.computeIfAbsent(image, BoardScene.Pixels::copy);
        return new BoardScene.Unit(captured.id(), -1, captured.name(), captured.location(), pixels, false, null,
              captured.height(), captured.airborne(), model, captured.outlineRgb(), captured.footprint());
    }

    private BoardScene.Unit unit(Entity entity, int part, Coords coords, boolean sensor,
          Map<Image, Boolean> usedImages) {
        Image image = sensor ? view.getRadarBlipImage() : view.getTileManager().textureFor(entity, part);
        usedImages.put(image, true);
        BoardScene.Pixels pixels = unitImages.computeIfAbsent(image, BoardScene.Pixels::copy);
        AnnotationKey annotationKey = new AnnotationKey(entity.getId(), part);
        EntitySprite.Annotations annotations = view.captureUnitAnnotations(entity, part, unitAnnotations.get(annotationKey));
        unitAnnotations.put(annotationKey, annotations);
        usedImages.put(annotations.image(), true);
        BoardScene.Pixels annotationPixels = unitImages.computeIfAbsent(annotations.image(), BoardScene.Pixels::copy);
        int facing = sensor ? 0 : view.getTileManager().facingFor(entity);
        boolean airborne = !sensor && airborne(entity);
        List<Coords> footprint = !sensor && part < 0 ? entity.getOccupiedCoords().stream()
              .sorted(java.util.Comparator.comparingInt(Coords::getX).thenComparingInt(Coords::getY)).toList() : List.of(coords);
        if (footprint.isEmpty()) {
            footprint = List.of(coords);
        }
        BoardScene.Waypoint location = airborne
              ? new BoardScene.Waypoint(coords, flightLevel(entity, coords), facing)
              : footprint.size() > 1 && UnitFootprint.terrainSupported(entity.getMovementMode())
                    ? new BoardScene.Waypoint(coords, UnitFootprint.support(board, coords, footprint, entity.getElevation()), facing)
                    : waypoint(coords, sensor ? 0 : entity.getElevation(), facing);
        if (!sensor) {
            location = location.withAeroState(aeroState(entity, entity.getElevation(), airborne))
                  .withFallSide(entity instanceof Mek ? entity.getFallSide() : null);
            if (location.aeroState() != null) {
                location = location.withFootprint(footprint);
            }
        }
        Color outline = sensor ? new Color(GpuMarkers.SENSOR_RGB)
              : GUIPreferences.getInstance().getTeamColoring() && view.getLocalPlayer() != null
                    ? UIUtil.teamColor(entity.getOwner(), view.getLocalPlayer())
                    : entity.getOwner().getColour().getColour(false);
        return new BoardScene.Unit(entity.getId(), part, sensor ? Messages.getString("BoardView1.sensorReturn")
              : entity.getShortName(),
              location, pixels, sensor,
              annotationPixels,
              sensor ? 1 : entity.height() + 1, airborne,
              GpuUnitModels.ENABLED
                    ? camouflage.resolve(UnitModelSelection.capture(entity, part, sensor, MMStaticDirectoryManager.getMekTileset(),
                          sensor ? 0 : UnitModelSelection.twist(entity.getFacing(), facing))) : null,
              outline.getRGB(), footprint);
    }

    private BoardScene.Waypoint waypoint(Coords coords, float relativeElevation, int facing) {
        Hex hex = board == null ? null : board.getHex(coords);
        return new BoardScene.Waypoint(coords, relativeElevation + (hex == null ? 0 : hex.getLevel()), facing);
    }

    /**
     * True while the unit is flying. Aerospace units carry an altitude; a parked aerodyne keeps its AERODYNE movement
     * mode, so {@code isAirborne()} alone would call it airborne, but the altitude decides. VTOLs and WiGEs hover on
     * their hex-relative elevation and are landed at elevation 0.
     */
    private static boolean airborne(Entity entity) {
        return entity.getAltitude() > 0 || entity.isAirborneVTOLorWIGE();
    }

    /**
     * Absolute level a flying visual floats at, with its token staying its own height above it. Aerospace altitude is
     * already absolute above the board, exactly as the LOS height conversion treats it, and is the only height
     * available for it because {@code Aero.getElevation()} reports the airborne sentinel while flying. VTOL and WiGE
     * elevation is relative to the hex below them.
     */
    private float flightLevel(Entity entity, Coords coords) {
        if (entity.getAltitude() > 0) {
            return entity.getAltitude();
        }
        Hex hex = board == null ? null : board.getHex(coords);
        return entity.getElevation() + (hex == null ? 0 : hex.getLevel());
    }

    /**
     * Playback position of one movement path point. An aerospace reports the airborne elevation sentinel (999) in
     * every step it takes while flying instead of a real level; those steps play at the given flight altitude, while a
     * step carrying a real elevation (an aerospace taxiing, or set down on its landing hex) keeps it. Every other unit
     * keeps its own per-step elevation, so VTOL and WiGE climbs and descents still animate.
     */
    private BoardScene.Waypoint pathWaypoint(Entity entity, Coords coords, float elevation, int facing,
          int flightAltitude, UnitLocation.Form form) {
        boolean aero = form == null ? entity.isAero() : form.aero();
        if (form != null && form.altitude() > 0) { flightAltitude = form.altitude(); }
        BoardScene.Waypoint point = aero && elevation >= Aero.AERO_EFFECTIVE_ELEVATION && flightAltitude > 0
              ? new BoardScene.Waypoint(coords, flightAltitude, facing)
              : waypoint(coords, elevation, facing);
        // A real path elevation describes the displayed step; the final Entity may already have landed/taken off.
        return point.withAeroState(aeroState(aero, elevation, false)).withForm(form);
    }

    private BoardScene.AeroState aeroState(Entity entity, float relativeElevation, boolean flying) {
        return aeroState(entity.isAero(), relativeElevation, flying);
    }

    private BoardScene.AeroState aeroState(boolean aero, float relativeElevation, boolean flying) {
        if (!aero) {
            return null;
        }
        if (board.isSpace() || flying || relativeElevation >= Aero.AERO_EFFECTIVE_ELEVATION) {
            return BoardScene.AeroState.AIRBORNE;
        }
        return relativeElevation == 0 ? BoardScene.AeroState.LANDED : BoardScene.AeroState.ELEVATED;
    }

    /** Swing owns the dialog and its conditions copy; completion receives immutable settings, or null on cancel. */
    void editPlanetaryConditions(Consumer<BoardAtmosphere.Settings> completed) {
        SwingUtilities.invokeLater(() -> {
            if (closed || conditionsDialog != null) {
                completed.accept(null);
                return;
            }
            BoardView initialView = view;
            Board initialBoard = board;
            BoardAtmosphere.Settings settings = null;
            try {
                var owner = view.getClientgui() == null ? SwingUtilities.getWindowAncestor(view.getPanel())
                      : view.getClientgui().getFrame();
                conditionsDialog = new PlanetaryConditionsDialog(owner instanceof JFrame frame ? frame : null,
                      view.game.getPlanetaryConditions());
                conditionsDialog.setAlwaysOnTop(true);
                if (conditionsDialog.showDialog() && !closed && view == initialView && board == initialBoard) {
                    settings = BoardAtmosphere.fromScenario(conditionsDialog.getConditions(), initialBoard.isSpace());
                }
            } finally {
                if (conditionsDialog != null) { conditionsDialog.dispose(); }
                conditionsDialog = null;
                completed.accept(settings);
            }
        });
    }

    public void inspect(Coords coords) {
        SwingUtilities.invokeLater(() -> {
            if (!closed) {
                contextCoords = coords;
                refresh();
            }
        });
    }

    public void overlayInput(int event, int x, int y, Runnable unhandled) {
        SwingUtilities.invokeLater(() -> {
            if (closed) {
                return;
            }
            OverlayViewport overlayViewport = viewport;
            boolean handled = view.overlayInput(event, new Point(x, y), overlayViewport.size(), overlayViewport.pixels());
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
            } else {
                // Publish overlay selection and camera requests together, without waiting for the HUD timer.
                refresh();
            }
        });
    }

    public void key(int keyCode, boolean down, int modifiers) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && view.getClientgui() != null && !view.getClientgui().shouldIgnoreHotKeys()) {
                KeyEvent event = new KeyEvent(view.getPanel(),
                      down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, System.currentTimeMillis(), modifiers,
                      keyCode, KeyEvent.CHAR_UNDEFINED);
                var controller = view.getClientgui().controller;
                boolean handled = controller != null && controller.dispatchKeyEvent(event);
                if (down) {
                    // Opening chat (especially with '/') must not also type the shortcut into its message.
                    suppressChatCharacter = handled;
                    if (!handled) {
                        if (view.getChatterBoxActive()) {
                            chatKey(event);
                        } else {
                            actions.menuShortcut(KeyStroke.getKeyStrokeForEvent(event));
                        }
                    }
                }
                refresh();
            }
        });
    }

    boolean chatActive() {
        return chatActive;
    }

    public void keyTyped(char character) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && !suppressChatCharacter && !Character.isISOControl(character)
                  && view.getChatterBoxActive() && view.getClientgui() != null
                  && !view.getClientgui().shouldIgnoreHotKeys()) {
                // ChatterBoxOverlay edits text in keyPressed; GLFW supplies Unicode separately from physical keys.
                chatKey(new KeyEvent(view.getPanel(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                      KeyEvent.VK_UNDEFINED, character));
                refresh();
            }
        });
    }

    private void chatKey(KeyEvent event) {
        for (var listener : view.getPanel().getKeyListeners()) {
            if (listener instanceof ChatterBoxOverlay chat) {
                chat.keyPressed(event);
            }
        }
    }

    public void stopKeys() {
        SwingUtilities.invokeLater(() -> {
            var gui = view.getClientgui();
            if (gui != null && gui.controller != null) {
                gui.controller.stopAllRepeating();
            }
        });
    }

    /** Both measurement gestures belong to the shared ruler, independently of the active phase tool. */
    static boolean isMeasurement(int modifiers) {
        return (modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0;
    }

    public void click(Coords coords, boolean doubleClick, int modifiers) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && coords != null && (view.game.getPhase().isOnMap()
                  || isMeasurement(modifiers))) {
                view.mouseAction(coords, doubleClick ? BoardView.BOARD_HEX_DOUBLE_CLICK : BoardView.BOARD_HEX_CLICK,
                      modifiers, 1);
                refresh();
            }
        });
    }

    public void hover(Coords coords, int modifiers) {
        SwingUtilities.invokeLater(() -> {
            if (!closed && coords != null && !isMeasurement(modifiers) && view.game.getPhase().isOnMap()) {
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
        if (conditionsDialog != null) { conditionsDialog.dispose(); }
        timer.stop();
        view.game.removeGameListener(gameListener);
        PreferenceManager.getClientPreferences().removePreferenceChangeListener(preferenceListener);
        GUIPreferences.getInstance().removePreferenceChangeListener(preferenceListener);
        if (board != null) {
            board.removeBoardListener(boardListener);
        }
        unitImages.clear();
        unitAnnotations.clear();
        overlayImages.clear();
        terrainImages.clear();
        camouflage.clear();
        tiles = List.of();
        view.releasePlanarCapture();
        synchronized (this) {
            pendingEvents.clear();
        }
    }
}
