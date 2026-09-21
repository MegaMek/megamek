/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.EntitySprite;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/** GPU board and Scene2D controls. The source remains the sole bridge to the existing client. */
class GpuBattleView extends ApplicationAdapter {
    private static final boolean SPREAD_UNIT_ANNOTATIONS = false;
    private static final float UNIT_ANNOTATION_MAX_OFFSCREEN_DISTANCE = -1;
    /** Airborne units hover: one full wave cycle lasts this long. */
    static final float HOVER_PERIOD_SECONDS = UnitAnimator.HOVER_PERIOD_SECONDS;
    /** Height of that wave in terrain levels, so a floating token drifts off its flight height. */
    static final float HOVER_LEVELS = UnitAnimator.HOVER_LEVELS;
    /** Width of the flat selection band as a fraction of the hex radius, extending inward from MARKER_INSET. */
    static final float SELECTION_BAND_WIDTH = .08f;
    /** Seconds for a complete rise and fall, on the shared animation clock. */
    static final float SELECTION_BOB_PERIOD_SECONDS = 4;
    /** Maximum lift above the unit's plane, in terrain levels. Zero disables bobbing. */
    static final float SELECTION_BOB_HEIGHT_OFFSET = .15f;
    static final float SELECTION_BOB_HEIGHT_LEVELS = .15f;
    /** Width of the target's hex-corner band as a fraction of the hex radius. */
    static final float TARGET_BAND_WIDTH = .08f;
    /** Seconds for a complete rise and fall, on the shared animation clock. */
    static final float TARGET_BOB_PERIOD_SECONDS = 4;
    /** Base clearance and maximum additional lift, matching the selection band's tuning. */
    static final float TARGET_BOB_HEIGHT_OFFSET = .15f;
    static final float TARGET_BOB_HEIGHT_LEVELS = .15f;
    /** Enable dashed hex-corner bands around units with assigned attacks. */
    static final boolean SHOW_TARGET_MARKERS = true;
    /** Hide all declared firing arrows while combat is playing, regardless of selection. */
    static final boolean HIDE_TARGET_ARROWS_DURING_ATTACKS = true;
    /** Hide the selected unit's target bands while combat is playing. */
    static final boolean HIDE_TARGET_MARKERS_DURING_ATTACKS = false;
    /** Floating units are tied to their hex with this faint solid stem; solid, so it needs no blending state. */
    private static final Color TETHER_COLOR = Color.valueOf("A9B8B8");
    private static final float TILT_DEGREES_PER_SECOND = 60;
    private static final MMLogger LOGGER = MMLogger.create(GpuBattleView.class);
    private GpuBoardSource source;
    private Stage loadingStage;
    private GpuBoardSkin loadingTheme;
    private Label loadingLabel;
    private volatile String loadingMessage = Messages.getString("ClientGUI.waitingOnTheServer");
    private final GpuDisplayScale displayScale = new GpuDisplayScale();
    final BoardCamera boardCamera = new BoardCamera();
    private final UnitPlayback playback = new UnitPlayback(this::completeMovement);
    private final BoardSurface.Cache groundSurfaces = new BoardSurface.Cache();
    private final Map<Integer, UnitMotion> motions = playback.motions;
    private final GpuAttackEffects attackEffects = new GpuAttackEffects();
    private final Map<BoardScene.Pixels, GpuUnitModel> spriteModels = new HashMap<>();
    private final GpuUnitModels unitModels = GpuUnitModels.ENABLED ? new GpuUnitModels() : null;
    private final UnitDamageDisplay damageDisplay = new UnitDamageDisplay();
    private final GpuJumpJets jumpJets = new GpuJumpJets();
    private final GpuUnitCamouflage camouflage = new GpuUnitCamouflage();
    private final GpuUnitIcons unitIcons = new GpuUnitIcons();
    private final Map<BoardScene.Unit, Vector3> unitAnchors = new HashMap<>();
    private final Map<BoardScene.Unit, UnitFootprint.Pose> unitFootprints = new HashMap<>();
    private final Matrix4 selectionTransform = new Matrix4();
    private final Map<String, ModelInstance> unitInstances = new HashMap<>();
    private final UnitBounds.Frame unitBounds = new UnitBounds.Frame();
    private final UnitPicking unitPicking = new UnitPicking();
    private final Map<String, UnitModelState.Appearance> equipmentAppearance = new HashMap<>();
    private final Map<String, BoardScene.Pixels> unitTints = new HashMap<>();
    private final Map<String, UpperBodyTurn> upperBodyTurns = new HashMap<>();
    private final Map<String, ArmFlip> armFlips = new HashMap<>();
    private final Map<String, UnitAnimator> animators = new HashMap<>();
    private final Map<String, BoardScene.LocationDamage> unitDamage = new HashMap<>();
    private final Map<Integer, KeyCommandBind> cameraKeys = new HashMap<>();
    private final BoardInput boardInput = new BoardInput();
    private final List<Hover> hover = new ArrayList<>();
    private GpuTerrain terrain;
    private GpuFireControl fireControl;
    private GpuTactical tactical;
    private final GpuFieldOfView fieldOfView;
    private GpuAtmosphere atmosphere;
    private GpuUnitVisibility unitVisibility;
    private GpuMarkers markers;
    private GpuTextures<BoardScene.Pixels> unitTextures;
    private GpuTextures<String> annotationTextures;
    private ModelBatch unitBatch;
    private SpriteBatch annotationBatch;
    private ShapeRenderer lines;
    private GpuBoardUi ui;
    private BoardScene scene;
    private record HexTextChunk(BitmapFontCache glyphs, BoundingBox bounds) { }
    private final Map<Integer, List<HexTextChunk>> hexTextByHeight = new TreeMap<>();
    private List<BoardScene.Tile> textTiles;
    private int textTuning = -1;
    private Coords hovered;
    private UnitMotion.Speed playbackSpeed = UnitMotion.Speed.NORMAL;
    private boolean fitted;
    private long frames;
    private float hoverClock;
    private long boardGeneration;
    private long centerSequence = -1;
    private boolean entrancePending;
    private boolean entranceStarting;
    private int cameraSelection = -1;
    private boolean cameraFollowingPlayback;
    private int layoutWidth;
    private int layoutHeight;
    private int layoutPixelWidth;
    private int layoutPixelHeight;
    private float layoutScale;
    private float layoutPreference;
    private long hoverCameraRevision;

    GpuBattleView(GpuBoardSource source) {
        this.source = source;
        fieldOfView = new GpuFieldOfView();
    }

    void setLoadingMessage(String message) {
        loadingMessage = message;
    }

    /** Attach the first map after the native loading window is already visible. Runs on the render thread. */
    void attachSource(GpuBoardSource next) {
        source = next;
        createBoard();
        loadingStage.dispose();
        loadingStage = null;
        loadingTheme.dispose();
        loadingTheme = null;
    }

    void prepareEntrance() {
        entrancePending = true;
    }

    void startEntrance() {
        entrancePending = false;
        entranceStarting = true;
        if (scene != null) {
            boardCamera.enter(scene);
        }
    }

    @Override
    public void create() {
        if (source == null) {
            loadingTheme = new GpuBoardSkin();
            loadingStage = new Stage(new ScreenViewport());
            Table content = new Table();
            content.setFillParent(true);
            loadingLabel = new Label(loadingMessage, loadingTheme.skin);
            loadingLabel.setName("board-loading-message");
            loadingLabel.setWrap(true);
            loadingLabel.setAlignment(Align.center);
            content.add(new Label("MEGAMEK", loadingTheme.skin, "kicker")).padBottom(18).row();
            content.add(loadingLabel).width(600);
            loadingStage.addActor(content);
            Gdx.input.setInputProcessor(new InputMultiplexer(loadingStage));
            return;
        }
        createBoard();
    }

    private void createBoard() {
        boardCamera.setIsometric(true);
        terrain = new GpuTerrain(unitModels, unitBounds);
        fireControl = new GpuFireControl();
        tactical = new GpuTactical();
        atmosphere = new GpuAtmosphere();
        unitVisibility = new GpuUnitVisibility();
        markers = new GpuMarkers();
        unitTextures = new GpuTextures<>();
        annotationTextures = new GpuTextures<>();
        unitBatch = new ModelBatch(GpuUnitShader.provider(), new GpuOpaqueSorter());
        annotationBatch = new SpriteBatch();
        lines = new ShapeRenderer();
        ui = new GpuBoardUi(source, boardCamera, () -> playbackSpeed = playbackSpeed.next(), playback::togglePaused);
        Gdx.input.setInputProcessor(new InputMultiplexer(ui.stage, boardInput) {
            @Override
            public boolean keyDown(int key) {
                // Window commands precede Scene2D focus; ordinary typing and navigation stay with the focused control.
                return isWindowShortcut(key) ? boardInput.keyDown(key) : super.keyDown(key);
            }

            @Override
            public boolean keyUp(int key) {
                // A menu may have taken focus after keyDown; always release the original board command.
                boolean handled = boardInput.keyUp(key);
                return ui.stage.keyUp(key) || handled;
            }
        });
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
        if (loadingStage != null && width > 0 && height > 0) {
            loadingStage.getViewport().update(width, height, true);
        }
        if (ui == null || width <= 0 || height <= 0) {
            return;
        }
        float preference = source.uiPreferences.scale();
        float scale = displayScale.read(preference);
        int pixelWidth = Gdx.graphics.getBackBufferWidth();
        int pixelHeight = Gdx.graphics.getBackBufferHeight();
        if (width == layoutWidth && height == layoutHeight && scale == layoutScale && preference == layoutPreference
              && pixelWidth == layoutPixelWidth && pixelHeight == layoutPixelHeight) {
            return;
        }
        layoutWidth = width;
        layoutHeight = height;
        layoutPixelWidth = pixelWidth;
        layoutPixelHeight = pixelHeight;
        layoutScale = scale;
        layoutPreference = preference;
        ui.resize(width, height, scale);
        int boardHeight = Math.max(1, height - ui.topPixels() - ui.bottomPixels());
        boardCamera.resize(width, boardHeight, scene, scale);
        source.setViewport(Math.round(width / ui.hudScale()), Math.round(boardHeight / ui.hudScale()),
              pixelWidth, Math.round(boardHeight * (pixelHeight / (float) height)));
    }

    @Override
    public void render() {
        if (source == null) {
            ScreenUtils.clear(.045f, .065f, .075f, 1, true);
            loadingLabel.setText(loadingMessage);
            loadingStage.act(Math.min(Gdx.graphics.getDeltaTime(), .1f));
            loadingStage.draw();
            return;
        }
        if (source.isClosed()) {
            Gdx.app.exit();
            return;
        }
        if (Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0) {
            return;
        }
        // Moving to another monitor can change DPI without changing the window's dimensions.
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        GpuBoardSource.Frame frame = source.takeFrame();
        if (frame.scene() == null) {
            return;
        }
        renderStage("scene update");
        if (scene != null && (boardGeneration != frame.boardGeneration() || scene.boardId() != frame.scene().boardId() || scene.width() != frame.scene().width()
              || scene.height() != frame.scene().height())) {
            playback.clear();
            animators.clear();
            groundSurfaces.clear();
            jumpJets.clear();
            unitPicking.clear();
            hovered = null;
            fitted = false;
        }
        List<BoardScene.Tile> previousTiles = scene == null ? null : scene.tiles();
        scene = frame.scene();
        playback.accept(frame.timeline(), scene, this::hasInfantryTransports);
        ui.update(frame, Messages.getString("GpuBoard.speed",
              playbackSpeed == UnitMotion.Speed.INSTANT ? Messages.getString("GpuBoard.instant") : playbackSpeed.label));
        playback.gravityOverride = ui.gravityOverride();
        boardCamera.viewableArea(ui.cameraLeft(), ui.cameraWidth());
        if (!fitted) { updateCameraFocus(scene, frame.centerRequest()); }
        var instantAction = playbackSpeed == UnitMotion.Speed.INSTANT ? playback.lastAction() : null;
        playback.advance(Gdx.graphics.getDeltaTime(), playbackSpeed, state -> preparePlaybackCamera(state, scene));
        scene = playback.present(scene);
        boolean changedTiles = previousTiles != scene.tiles();
        camouflage.retain(scene.units());
        if (unitModels != null) {
            unitModels.retainAssemblies(scene.units().stream().filter(unit -> !unit.sensorContact())
                  .map(BoardScene.Unit::id).collect(Collectors.toSet()));
        }
        boardGeneration = frame.boardGeneration();
        ui.setPlaybackPaused(playback.paused());
        terrain.update(scene);
        fireControl.update(scene, HIDE_TARGET_ARROWS_DURING_ATTACKS && !playback.attacks().isEmpty());
        tactical.update(scene);
        fieldOfView.update(scene.fieldOfView());
        fieldOfView.configure(ui.fovStyle(), ui.fovDarkness(), ui.sensorStyle(), ui.sensorDarkness());
        atmosphere.configure(ui.atmosphere());
        atmosphere.setOptions(ui.atmosphereOptions());
        terrain.setNormalMaps(ui.normalMaps());
        if (unitTextures.update(scene.units().stream().filter(unit -> !unit.sensorContact()
              && (unitModels == null || unitModels.get(unit.model(), unit.id()) == null)).map(BoardScene.Unit::image).distinct()
              .collect(Collectors.toMap(pixels -> pixels, pixels -> pixels)))) {
            spriteModels.values().forEach(GpuUnitModel::dispose);
            spriteModels.clear();
            unitPicking.clear();
        }
        Set<BoardScene.Pixels> images = scene.units().stream().map(BoardScene.Unit::image).collect(Collectors.toSet());
        boolean removedSprites = spriteModels.entrySet().removeIf(entry -> {
            if (images.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().dispose();
            return true;
        });
        if (removedSprites) {
            unitPicking.clear();
        }
        annotationTextures.update(scene.units().stream().filter(unit -> unit.annotations() != null).collect(Collectors.toMap(
              unit -> unit.id() + ":" + unit.part(), BoardScene.Unit::annotations)));
        updateCameraFocus(scene, frame.centerRequest(), instantAction);
        // The first visible frame's delta may still include the hidden window's loading time.
        boardCamera.advance(entranceStarting ? 0 : Gdx.graphics.getDeltaTime());
        entranceStarting = false;
        if (source.chatActive()) {
            cameraKeys.clear();
        }
        if (ui.acceptsCameraKeys() && !source.chatActive()) {
            float distance = 500 * Gdx.graphics.getDeltaTime();
            float inclination = TILT_DEGREES_PER_SECOND * Gdx.graphics.getDeltaTime();
            for (KeyCommandBind command : cameraKeys.values()) {
                switch (command) {
                    case SCROLL_NORTH -> boardCamera.pan(0, distance);
                    case SCROLL_SOUTH -> boardCamera.pan(0, -distance);
                    case SCROLL_EAST -> boardCamera.pan(-distance, 0);
                    case SCROLL_WEST -> boardCamera.pan(distance, 0);
                    case CAMERA_TILT_UP -> boardCamera.tilt(-inclination);
                    case CAMERA_TILT_DOWN -> boardCamera.tilt(inclination);
                    case CAMERA_ROTATE_LEFT -> continueRotation(-1);
                    case CAMERA_ROTATE_RIGHT -> continueRotation(1);
                    default -> { }
                }
            }
        }
        if (changedTiles || hoverCameraRevision != boardCamera.revision()) {
            source.setVisibleArea(boardCamera.visibleArea(scene));
        }
        if (hoverCameraRevision != boardCamera.revision()) {
            hoverCameraRevision = boardCamera.revision();
            boardInput.mouseMoved(Gdx.input.getX(), Gdx.input.getY());
        }
        hoverClock += animationSeconds();
        renderStage("unit poses");
        markers.beginFrame(scene.markers(), Gdx.graphics.getDeltaTime());
        prepareUnits();
        applyHover();
        for (var attack : playback.attacks()) { attack.landscape = ray -> terrain.hit(scene, ray); }
        aimAttack();
        updateEquipmentDetail();
        if (unitIcons.update(ui.overviewIcons(), ui.overviewHexPixels(), boardCamera.camera,
              scene, unitFootprints, unitAnchors, groundSurfaces)) { unitPicking.clear(); }
        terrain.setFlatTrees(unitIcons.active());
        markers.update(unitIcons.active() ? unitIcons.instances() : unitInstances.values(), boardCamera.camera);
        updateJumpJets();
        attackEffects.update(playback.attacks(), unitModels, unitInstances);
        unitBounds.begin();
        List<ModelInstance> units = new ArrayList<>(unitIcons.active() ? unitIcons.instances() : unitInstances.values());
        List<ModelInstance> outlined = scene.units().stream()
              .filter(unit -> !unitIcons.active())
              .filter(unit -> !unit.sensorContact() || GpuMarkers.outlineEnabled(BoardMarker.Kind.SENSOR_CONTACT))
              .map(unit -> unitInstances.get(unit.id() + ":" + unit.part()))
              .collect(Collectors.toCollection(ArrayList::new));
        outlined.addAll(markers.outlinedInstances());
        outlined.removeIf(instance -> instance == null || !boardCamera.camera.frustum.boundsInFrustum(unitBounds.get(instance)));
        float seeThrough = ui.seeThrough();
        renderStage("cutaways and light");
        atmosphere.updateLight(boardCamera.camera);
        terrain.setAtmosphere(atmosphere.lighting());
        terrain.animate(Gdx.graphics.getDeltaTime(), units, ui.buildingOpacity());
        renderStage("geometry shadows");
        terrain.renderShadows(boardCamera.camera, unitIcons.active() ? List.of() : units);
        renderStage("cloud transmission");
        atmosphere.prepareClouds(terrain, scene, Gdx.graphics.getDeltaTime());
        renderStage("opaque terrain");
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        atmosphere.begin((int) boardCamera.camera.viewportWidth, (int) boardCamera.camera.viewportHeight,
              Gdx.graphics.getDeltaTime());
        terrain.render(boardCamera.camera, false);
        renderStage("units");
        renderUnits();
        renderStage("transparent effects");
        if (!unitIcons.active()) { renderTethers(); }
        terrain.renderTransparent(boardCamera.camera);
        if (!unitIcons.active()) { jumpJets.render(boardCamera.camera); }
        attackEffects.render(boardCamera.camera);
        renderStage("atmosphere composite");
        atmosphere.end(boardCamera.camera, terrain, scene, ui.bottomPixels(), fieldOfView);
        renderStage("weather particles");
        atmosphere.renderWeather(boardCamera.camera, scene);
        renderStage("unit outlines");
        unitVisibility.render(boardCamera.camera, outlined, atmosphere.depthTexture(), ui.bottomPixels(), seeThrough, layoutScale, unitBounds);
        renderStage("tactical overlays");
        terrain.render(boardCamera.camera, true);
        fireControl.render(boardCamera.camera, Gdx.graphics.getDeltaTime());
        tactical.render(boardCamera.camera, Gdx.graphics.getDeltaTime(), unitIcons.active() ? unitIcons.instances() : List.of());
        renderHexText();
        fireControl.renderLabels(boardCamera.camera);
        renderSelectionOutlines();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        tactical.renderLabels(boardCamera.camera);
        renderStage("annotations and UI");
        renderAnnotations();
        renderEntrance();
        ui.draw();
        renderStage(null);
        frames++;
    }

    /** Benchmark boundary only. Normal gameplay does not allocate queries, read clocks or wait for the GPU. */
    void renderStage(String stage) { }

    private void renderEntrance() {
        float opacity = entrancePending ? 0 : boardCamera.entranceOpacity();
        if (opacity >= 1) { return; }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        lines.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
              boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight));
        lines.begin(ShapeRenderer.ShapeType.Filled);
        lines.setColor(0.035f, 0.055f, 0.075f, 1 - opacity);
        lines.rect(0, 0, boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight);
        lines.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void updateJumpJets() {
        jumpJets.beginFrame();
        if (unitModels != null) {
            for (var unit : scene.units()) {
                var motion = motions.get(unit.id());
                if (unit.sensorContact() || motion == null) {
                    continue;
                }
                var sample = motion.sample();
                if (sample.jets() == null && sample.group() == null) {
                    continue;
                }
                String key = unit.id() + ":" + unit.part();
                var instance = unitInstances.get(key);
                var model = unitModels.get(unit.model(), unit.id());
                if (instance != null && model != null) {
                    jumpJets.update(key, model, instance, unit, sample);
                }
            }
        }
        jumpJets.endFrame();
    }

    private void prepareUnits() {
        hover.clear();
        unitAnchors.clear();
        unitFootprints.clear();
        unitInstances.keySet().retainAll(scene.units().stream().map(unit -> unit.id() + ":" + unit.part())
              .collect(Collectors.toSet()));
        unitTints.keySet().retainAll(unitInstances.keySet());
        upperBodyTurns.keySet().retainAll(unitInstances.keySet());
        armFlips.keySet().retainAll(unitInstances.keySet());
        animators.keySet().retainAll(unitInstances.keySet());
        unitDamage.keySet().retainAll(unitInstances.keySet());
        equipmentAppearance.keySet().retainAll(unitInstances.keySet());
        for (BoardScene.Unit unit : scene.units()) {
            Vector3 position = BoardGeometry.center(unit.location().coords(), unit.location().elevation());
            playback.placeDisplacement(unit, position);
            float facing = unit.location().facing() * 60;
            UnitMotion motion = unit.sensorContact() ? null : motions.get(unit.id());
            UnitMotion.Sample sample = motion == null ? UnitMotion.Sample.STILL : motion.sample();
            BoardScene.Unit placement = sample.placement(unit);
            boolean airborne = sample.airborne(unit);
            if (motion != null && motion.isMoving()) {
                position.sub(motion.destination()).add(airborne ? motion.position() : motion.surfacePosition(scene));
                if (placement.footprint().size() > 1) {
                    // The path already carries its own absolute elevation; destination support is not a constant lift.
                    position.set(airborne ? motion.position() : motion.surfacePosition(scene));
                }
                facing = motion.facing();
                UnitFootprint.clearTerrain(scene, placement, position, facing, airborne);
            }
            BoardScene.Tile tile = scene.tile(unit.location().coords());
            if (tile != null && tile.waterDepth() == 0 && !tile.frozen() && !airborne
                  && MathUtils.isEqual(position.z, tile.elevation() * BoardGeometry.LEVEL)) {
                position.z = BoardGeometry.groundZ(tile);
            }
            float footprintFacing = facing;
            GpuUnitModel visual = unit.sensorContact() ? markers.model(BoardMarker.Kind.SENSOR_CONTACT)
                  : unitModels == null ? null : unitModels.get(unit.model(), unit.id());
            boolean authored = !unit.sensorContact() && visual != null;
            boolean dead = authored && unit.model().state() != null && unit.model().state().pose().dead();
            var death = playback.attacks().stream().filter(attack -> attack.death() && attack.event.entityId() == unit.id())
                  .findFirst().orElse(null);
            float collapse = death == null ? dead ? 1 : 0 : death.deathProgress();
            if (airborne && collapse > 0) {
                float ground = UnitLandingSupports.ground(scene, position.x, position.y, groundSurfaces);
                if (!Float.isFinite(ground) && tile != null) { ground = BoardGeometry.surfaceZ(tile); }
                if (Float.isFinite(ground)) { position.z = MathUtils.lerp(position.z, ground, collapse); }
            }
            if (visual == null) {
                visual = spriteModels.computeIfAbsent(unit.image(), pixels ->
                      GpuUnitModel.sprite(pixels, unitTextures.region(pixels)));
            }
            String key = unit.id() + ":" + unit.part();
            ModelInstance instance = unitInstances.get(key);
            if (instance == null || instance.model != visual.instance.model) {
                instance = newUnitInstance(key, visual);
            }
            BoardScene.LocationDamage shownDamage = unitDamage.getOrDefault(key, BoardScene.LocationDamage.NONE);
            boolean mek = authored && (unit.model().state() == null ? visual.turnsUpperBody()
                  : unit.model().state().structure().anatomy() != null);
            BoardScene.LocationDamage damage = authored ? visual.infantry() ? unit.model().damage()
                  : UnitDamageDisplay.preview(unit.model().damage(), mek, ui.damageOverride(), visual.damageLocation(ui.damageLocation()))
                  : BoardScene.LocationDamage.NONE;
            UnitModelState.Appearance appearance = authored && unit.model().state() != null
                  ? unit.model().state().appearance() : null;
            if (authored && (!damage.equals(shownDamage)
                  || !java.util.Objects.equals(appearance, equipmentAppearance.get(key)))) {
                UpperBodyTurn previousTurn = upperBodyTurns.get(key);
                instance = showDamage(key, visual, unit, damage);
                if (appearance != null) {
                    visual.showEquipment(instance, appearance);
                    camouflage.apply(instance, visual.instance, appearance);
                    equipmentAppearance.put(key, appearance);
                }
                damageDisplay.applyTexture(instance, damage, unit.id());
                if (previousTurn != null) {
                    upperBodyTurns.put(key, previousTurn);
                    visual.turnUpperBody(instance, previousTurn.degrees());
                }
            }
            if (dead) { damageDisplay.wreck(instance, visual, unit.id()); }
            // Presentation-only color for the see-through pass; the normal model materials retain their artwork.
            if (!(instance.userData instanceof Color)) {
                instance.userData = new Color();
            }
            Color.rgb888ToColor((Color) instance.userData, unit.outlineRgb());
            if (authored && appearance == null && !unit.image().equals(unitTints.get(key))) {
                Color tint = GpuCutout.averageColor(unit.image());
                float brightest = Math.max(tint.r, Math.max(tint.g, tint.b));
                if (brightest > 0.01f) {
                    tint.mul(1 / brightest);
                    tint.a = 1;
                }
                for (var material : instance.materials) {
                    if ("paint".equals(material.id)) {
                        material.set(ColorAttribute.createDiffuse(tint));
                    }
                }
                unitTints.put(key, unit.image());
            }
            if (authored && visual.turnsUpperBody()) {
                // A movement replay already follows the legs, so only a unit standing still shows its twist.
                boolean isMoving = (motion != null) && motion.isMoving();
                facing -= turnUpperBody(visual, instance, key, unit, isMoving ? 0 : unit.model().twist());
            }
            if (authored && unit.model().state() != null && !visual.rigs().isEmpty()) {
                if (visual.rigs().stream().allMatch(rig -> rig.trooper() || "infantry-transport".equals(rig.family()))) {
                    facing = 0; // Troops and their transports have cosmetic member headings, no gameplay facing.
                }
                var turn = upperBodyTurns.get(key);
                animators.computeIfAbsent(key, ignored -> new UnitAnimator(groundSurfaces)).apply(visual, instance, unit,
                      sample, hoverClock, animationSeconds(),
                      playbackSpeed == UnitMotion.Speed.INSTANT, turn == null ? 0 : turn.degrees());
                animators.get(key).attacks(visual, unit, playback.attacks());
                animators.get(key).conversion(playback.conversion(), unit);
                if (visual.infantry()) { animators.get(key).previewCasualties(ui.damageOverride()); }
            }
            // After the animator, never before it: the animator resets every joint to its rest pose
            // each frame and then poses leftArm and rightArm itself, which are the same nodes a flip
            // turns. Setting the flip first meant it was overwritten before anything was drawn.
            if (authored && visual.flipsArms()) {
                flipArms(visual, instance, key, unit);
            }
            Vector3 anchor = unit.sensorContact()
                  ? markers.placeSensor(unit.location().coords(), instance, boardCamera.camera, position)
                  : visual.place(instance, boardCamera.camera, position, facing, placement);
            UnitAnimator animator = animators.get(key);
            if (authored && animator != null && animator.groundSupports(scene, placement, sample)) {
                anchor = visual.anchor(instance, boardCamera.camera);
            }
            if (airborne && collapse == 0) {
                float offset = hoverOffset(hoverClock, unit.id(), unit.part());
                hover.add(new Hover(instance, offset));
                anchor.add(0, 0, offset);
                position.z += offset;
            }
            unitFootprints.put(unit, new UnitFootprint.Pose(placement, position, footprintFacing));
            unitAnchors.put(unit, anchor);
        }
    }

    /** Frame an action before its clock advances, including when one large frame reaches several queued actions. */
    boolean preparePlaybackCamera(UnitPlayback state, BoardScene scene) {
        if (state.movement() != null) {
            boardCamera.frameMovement(state.movement(), motions.get(state.activeEntityId()), state.present(scene), cameraWidth());
        } else if (state.attack() != null && state.attack().shot()) {
            boardCamera.frameAttacks(state.attacks(), cameraWidth());
        } else {
            return true;
        }
        return !boardCamera.isFraming();
    }

    private float cameraWidth() { return ui == null ? boardCamera.camera.viewportWidth : ui.cameraWidth(); }

    /** Frame the presented action; selection changes during playback take effect after its final hold. */
    void updateCameraFocus(BoardScene scene, BoardView.CenterRequest request) {
        updateCameraFocus(scene, request, null);
    }

    void updateCameraFocus(BoardScene scene, BoardView.CenterRequest request, BoardScene.Animation instantAction) {
        if (!fitted) {
            boardCamera.fit(scene);
            fitted = true;
            cameraSelection = scene.selectedId();
            cameraFollowingPlayback = false;
            // Start with the whole map, ignoring any earlier classic-board centering. Later board switches
            // can follow a unit-list click, whose pending focus request must still be applied.
            centerSequence = centerSequence < 0 ? request.sequence() : 0;
        }
        boolean firing = playback.attack() != null && playback.attack().shot();
        boolean instant = playbackSpeed == UnitMotion.Speed.INSTANT;
        if (!instant && !firing && playback.movement() == null) { boardCamera.clearPlaybackFrame(); }
        // Keep the last idle selection until the entire playback, including its completion hold, ends.
        if (playback.busy()) {
            if (playback.movement() != null) {
                // The complete route is already framed. Do not chase the unit or recenter it on arrival.
                cameraFollowingPlayback = false;
                // The classic board auto-centers each move's start; do not replay that request after arrival.
                if (request.entityId() == Entity.NONE
                      && playback.movement().path().getFirst().coords().equals(request.coords())) {
                    centerSequence = request.sequence();
                }
                boardCamera.frameMovement(playback.movement(), motions.get(playback.activeEntityId()), scene, cameraWidth());
                return;
            }
            cameraFollowingPlayback = true;
            if (firing) {
                boardCamera.frameAttacks(playback.attacks(), cameraWidth());
                return;
            }
            var active = scene.units().stream().filter(unit -> unit.id() == playback.activeEntityId())
                  .findFirst().orElse(null);
            if (active != null) {
                var motion = motions.get(active.id());
                Vector3 position = motion != null && motion.isMoving() ? motion.surfacePosition(scene).cpy()
                      : BoardGeometry.center(active.location().coords(), active.location().elevation());
                playback.placeDisplacement(active, position);
                if (!boardCamera.focus.epsilonEquals(position, .001f)) {
                    boardCamera.center(position);
                }
            }
            return;
        }
        boolean requested = centerSequence != request.sequence();
        boolean changedSelection = cameraSelection != scene.selectedId() || cameraFollowingPlayback && instantAction == null;
        var selection = changedSelection && scene.selectedId() != Entity.NONE
              ? scene.units().stream().filter(unit -> unit.id() == scene.selectedId()).findFirst().orElse(null) : null;
        if (selection == null && requested && request.entityId() != Entity.NONE) {
            selection = scene.units().stream().filter(unit -> unit.id() == request.entityId()).findFirst().orElse(null);
        }
        Coords center = selection == null && requested && request.entityId() == Entity.NONE ? request.coords() : null;
        centerSequence = request.sequence();
        cameraSelection = scene.selectedId();
        cameraFollowingPlayback = false;
        if (selection != null) {
            boardCamera.frameSelection(selection, cameraWidth());
        } else if (instantAction instanceof BoardScene.Combat combat && combat.result().kind() == ResolvedAttack.Kind.SHOT) {
            boardCamera.frameAttacks(List.of(new UnitAttack(combat)), cameraWidth());
        } else if (instantAction != null) {
            scene.units().stream().filter(unit -> unit.id() == instantAction.entityId()).findFirst()
                  .ifPresent(unit -> boardCamera.frameSelection(unit, cameraWidth()));
        } else if (center != null && scene.tile(center) != null) {
            boardCamera.frameLocation(BoardGeometry.center(center, scene.tile(center).elevation()), cameraWidth());
        }
        if (instant) {
            // Replace any old transition with the final requested view before snapping it, never visiting each event.
            if (boardCamera.isFraming()) { boardCamera.advance(BoardCamera.CAMERA_FRAMING_SECONDS); }
            boardCamera.clearPlaybackFrame();
        }
    }

    private void aimAttack() {
        if (unitModels == null) { return; }
        Set<Integer> aimed = new java.util.HashSet<>();
        for (var attack : playback.attacks()) {
            if (!attack.shot() && attack.aimWeight() <= 0) { continue; }
            var unit = attack.event.attacker();
            if (!aimed.add(unit.id())) { continue; }
            var key = unit.id() + ":" + unit.part();
            var animator = animators.get(key);
            if (animator == null || !unitInstances.containsKey(key) || unit.model() == null) { continue; }
            var model = unitModels.loaded(unit.model(), unit.id());
            if (model == null) { continue; }
            var target = attack.event.target();
            var targetInstance = target == null ? null : unitInstances.get(target.id() + ":" + target.part());
            var origin = UnitAttack.center(unitInstances.get(key), unit.location(), new Vector3());
            var endpoint = attack.shot() ? attack.endpoint(targetInstance, origin, new Vector3())
                  : attack.contact(targetInstance, origin, unitPicking, new Vector3());
            if (attack.shot()) {
                animator.aimShots(model, unit, playback.attacks(), shot -> shot.event.target() == null ? null
                      : unitInstances.get(shot.event.target().id() + ":" + shot.event.target().part()));
            } else { animator.aim(model, unit, attack, endpoint, targetInstance); }
            for (var displayed : scene.units()) {
                if (displayed.id() == unit.id()) {
                    unitAnchors.put(displayed, model.anchor(unitInstances.get(key), boardCamera.camera));
                    break;
                }
            }
        }
    }

    /** One floating visual's drift for the current frame: a draw-time offset, never part of the game state. */
    private record Hover(ModelInstance instance, float offset) { }

    /** Apply the same hover transform before drawing, feature fading, and shadow capture. */
    private void applyHover() {
        for (Hover drifting : hover) {
            Vector3 center = drifting.instance().transform.getTranslation(new Vector3());
            drifting.instance().transform.setTranslation(center.add(0, 0, drifting.offset()));
        }
    }

    /**
     * Vertical hover offset in world units for an airborne visual at the current animation time: a slow sine wave that
     * each unit starts at a different phase, so the floating tokens drift as a loose wave instead of rising and
     * falling together. A grounded unit (elevation or altitude 0) never gets an offset. This only moves the rendered
     * token; no game state changes.
     */
    static float hoverOffset(float seconds, int id, int part) {
        return UnitAnimator.hoverOffset(seconds, id, part);
    }

    private float animationSeconds() {
        return playback.paused() ? 0 : (float) (Gdx.graphics.getDeltaTime() * playbackSpeed.rate);
    }

    private boolean hasInfantryTransports(BoardScene.Unit unit) {
        if (unitModels == null || unit == null || unit.sensorContact()) {
            return false;
        }
        var model = unitModels.get(unit.model(), unit.id());
        return model != null && model.rigs().stream().anyMatch(UnitRig::transport);
    }

    /** Evaluate the final captured formation even when several queued moves are skipped in one frame. */
    private void completeMovement(BoardScene.Movement movement) {
        var unit = movement.unit();
        if (unitModels == null || unit == null || unit.sensorContact() || unit.model() == null || unit.model().state() == null) {
            return;
        }
        var model = unitModels.get(unit.model(), unit.id());
        if (model == null || model.rigs().isEmpty()) {
            return;
        }
        String key = unit.id() + ":" + unit.part();
        var instance = unitInstances.get(key);
        if (instance == null || instance.model != model.instance.model) {
            instance = newUnitInstance(key, model);
        }
        animators.computeIfAbsent(key, ignored -> new UnitAnimator()).apply(model, instance, unit,
              motions.get(unit.id()).sample(), hoverClock, 0, true, unit.model().twist());
    }

    /** Level a floating visual's stem ends at, or NaN when no stem is drawn for this token at this time. */
    static float tetherGround(BoardScene.Unit unit, int tileElevation, Vector3 center, boolean moving) {
        float ground = tileElevation * BoardGeometry.LEVEL;
        return unit.airborne() && !moving && center.z > ground ? ground : Float.NaN;
    }

    /**
     * Faint stems from each floating visual's center down to the center of the hex it occupies, so an airborne token
     * beside a hill or another raised tile still reads as belonging to the hex under it. A grounded token already
     * covers its hex, and a moving token is between hexes, so neither gets a stem.
     */
    private void renderTethers() {
        Vector3 center = new Vector3();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
        lines.setProjectionMatrix(boardCamera.camera.combined);
        lines.begin(ShapeRenderer.ShapeType.Line);
        lines.setColor(TETHER_COLOR);
        for (BoardScene.Unit unit : scene.units()) {
            BoardScene.Tile tile = scene.tile(unit.location().coords());
            ModelInstance instance = unitInstances.get(unit.id() + ":" + unit.part());
            UnitMotion motion = motions.get(unit.id());
            if (tile == null || instance == null) {
                continue;
            }
            instance.transform.getTranslation(center);
            float ground = tetherGround(unit, tile.elevation(), center, motion != null && motion.isMoving());
            if (!Float.isNaN(ground)) {
                lines.line(center.x, center.y, center.z, center.x, center.y, ground);
            }
        }
        lines.end();
        Gdx.gl.glDepthMask(true);
    }

    /** One render-only selection shared by color, depth, outlines and shadows. */
    void updateEquipmentDetail() {
        Set<Integer> detailedUnits = new java.util.HashSet<>();
        detailedUnits.add(scene.selectedId());
        for (var attack : playback.attacks()) {
            detailedUnits.add(attack.event.entityId());
            if (attack.event.target() != null) { detailedUnits.add(attack.event.target().id()); }
        }
        for (var unit : scene.units()) {
            if (unitInstances.get(unit.id() + ":" + unit.part()) instanceof GpuUnitInstance instance) {
                instance.equipmentDetail(boardCamera.camera, detailedUnits.contains(unit.id()));
            }
        }
    }

    /** A fresh instance shows no tint, twist or damage yet, so everything remembered about the old one is dropped. */
    private ModelInstance newUnitInstance(String key, GpuUnitModel visual) {
        ModelInstance instance = new GpuUnitInstance(visual);
        unitInstances.put(key, instance);
        unitTints.remove(key);
        upperBodyTurns.remove(key);
        armFlips.remove(key);
        unitDamage.remove(key);
        equipmentAppearance.remove(key);
        return instance;
    }

    /**
     * Takes lost arms off the unit's model and burns out its other lost locations. Starts from a fresh instance
     * instead of undoing the old damage, which also covers a location that a game master has repaired.
     */
    private ModelInstance showDamage(String key, GpuUnitModel visual, BoardScene.Unit unit, BoardScene.LocationDamage damage) {
        ModelInstance instance = newUnitInstance(key, visual);
        List<String> missing = UnitDamageDisplay.show(instance, damage);
        unitDamage.put(key, damage);
        LOGGER.debug("[GpuDamage] {}: taken off {}, burnt out {}, no part in the model for {}",
              unit.name(), damage.removed(), damage.wrecked(), missing);
        return instance;
    }

    /**
     * Shows a torso twist on a model whose upper body turns on its own. The scene gives the unit's torso facing, as
     * the classic sprite does, so the legs are that facing less the twist.
     *
     * @param twist hexsides the torso is turned clockwise from the legs
     *
     * @return the degrees to take off the displayed facing to get the facing of the legs
     */
    /**
     * Swings one unit's arms toward the pose its firing arc calls for, a step at a time, and poses the model again
     * when the shown angle moves. The game state holds the arc; this only shows it.
     */
    private void flipArms(GpuUnitModel visual, ModelInstance instance, String key, BoardScene.Unit unit) {
        var state = unit.model().state();
        boolean wanted = (state != null) && (state.pose() != null) && state.pose().armsFlipped();
        ArmFlip flip = armFlips.computeIfAbsent(key, ignored -> new ArmFlip());
        boolean previous = flip.flipped();
        if (flip.advance(wanted, playbackSpeed == UnitMotion.Speed.INSTANT ? Float.MAX_VALUE : animationSeconds())) {
            visual.flipArms(instance, flip.degrees());
        }
        if (previous != wanted) {
            LOGGER.debug("[GpuArmFlip] {}: arms now {}", unit.name(), wanted ? "flipped to the rear" : "forward");
        }
    }

    private float turnUpperBody(GpuUnitModel visual, ModelInstance instance, String key, BoardScene.Unit unit, int twist) {
        UpperBodyTurn turn = upperBodyTurns.get(key);
        if (turn == null) {
            turn = new UpperBodyTurn();
            upperBodyTurns.put(key, turn);
        }
        int previousTwist = turn.hexsides();
        if (turn.advance(twist, playbackSpeed == UnitMotion.Speed.INSTANT ? Float.MAX_VALUE : animationSeconds())) {
            visual.turnUpperBody(instance, turn.degrees());
        }
        if (previousTwist != twist) {
            LOGGER.debug("[GpuTwist] {}: upper body now {} hexside(s) clockwise of the legs, was {}",
                  unit.name(), twist, previousTwist);
        }
        return turn.targetDegrees();
    }

    private void renderUnits() {
        unitBatch.begin(boardCamera.camera);
        if (!unitIcons.active()) {
            for (BoardScene.Unit unit : unitAnchors.keySet()) {
                ModelInstance instance = unitInstances.get(unit.id() + ":" + unit.part());
                if (boardCamera.camera.frustum.boundsInFrustum(unitBounds.get(instance))) {
                    if (unit.sensorContact()) {
                        unitBatch.render(instance);
                    } else {
                        unitBatch.render(instance, terrain.environment());
                    }
                }
            }
        }
        // Every marker writes normal scene depth, independently of its optional see-through outline.
        markers.instances().forEach(unitBatch::render);
        unitBatch.end();
    }

    private void renderAnnotations() {
        annotationBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
              boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight));
        var ordered = unitAnchors.entrySet().stream().filter(entry -> entry.getKey().annotations() != null).sorted(Comparator
              .<Map.Entry<BoardScene.Unit, Vector3>>comparingInt(entry ->
                    entry.getKey().id() == scene.selectedId() ? 0
                          : entry.getKey().location().coords().equals(hovered) ? 1 : 2)
              .thenComparingDouble(entry -> boardCamera.camera.position.dst2(entry.getValue()))
              .thenComparingInt(entry -> entry.getKey().id())
              .thenComparingInt(entry -> entry.getKey().part()))
              .map(entry -> Map.entry(entry.getKey(), boardCamera.camera.project(new Vector3(entry.getValue()), 0, 0,
                  boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight)))
              .filter(entry -> withinAnnotationDistance(entry.getValue().x, entry.getValue().y,
                  boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight,
                  UNIT_ANNOTATION_MAX_OFFSCREEN_DISTANCE * layoutScale)).toList();
        List<Rectangle> placed = new ArrayList<>();
        List<Rectangle> markerBounds = markers.labelObstacles(boardCamera.camera);
        List<Rectangle> occupied = new ArrayList<>(markerBounds);
        for (Map.Entry<BoardScene.Unit, Vector3> entry : ordered) {
            BoardScene.Unit unit = entry.getKey();
            Vector3 point = entry.getValue();
            TextureRegion region = annotationTextures.region(unit.id() + ":" + unit.part());
            float scale = Math.min(annotationScale(layoutScale), boardCamera.camera.viewportWidth / region.getRegionWidth());
            scale = Math.min(scale, boardCamera.camera.viewportHeight / region.getRegionHeight());
            float width = region.getRegionWidth() * scale;
            float height = region.getRegionHeight() * scale;
            Rectangle bounds = new Rectangle(MathUtils.clamp(point.x - width / 2, 0,
                  boardCamera.camera.viewportWidth - width), MathUtils.clamp(point.y + 6 * layoutScale,
                        0, boardCamera.camera.viewportHeight - height), width, height);
            List<Rectangle> obstacles = SPREAD_UNIT_ANNOTATIONS || unit.sensorContact() ? occupied : markerBounds;
            placed.add(!obstacles.isEmpty()
                  ? spreadAnnotation(bounds, obstacles, boardCamera.camera.viewportWidth,
                        boardCamera.camera.viewportHeight, 2 * layoutScale)
                  : bounds);
            occupied.add(placed.getLast());
        }
        annotationBatch.begin();
        for (int index = ordered.size() - 1; index >= 0; index--) {
            BoardScene.Unit unit = ordered.get(index).getKey();
            Rectangle bounds = placed.get(index);
            annotationBatch.draw(annotationTextures.region(unit.id() + ":" + unit.part()),
                  bounds.x, bounds.y, bounds.width, bounds.height);
        }
        markers.renderLabels(annotationBatch, boardCamera.camera, layoutScale, occupied);
        annotationBatch.end();
    }

    static boolean withinAnnotationDistance(float screenX, float screenY, float viewportWidth,
          float viewportHeight, float maxDistance) {
        if (maxDistance < 0) {
            return true;
        }
        float distanceX = screenX - MathUtils.clamp(screenX, 0, viewportWidth);
        float distanceY = screenY - MathUtils.clamp(screenY, 0, viewportHeight);
        return distanceX * distanceX + distanceY * distanceY <= maxDistance * maxDistance;
    }

    static Rectangle spreadAnnotation(Rectangle preferred, List<Rectangle> occupied,
          float viewportWidth, float viewportHeight, float gap) {
        float maxX = Math.max(0, viewportWidth - preferred.width);
        float maxY = Math.max(0, viewportHeight - preferred.height);
        Rectangle origin = new Rectangle(MathUtils.clamp(preferred.x, 0, maxX),
              MathUtils.clamp(preferred.y, 0, maxY), preferred.width, preferred.height);
        Rectangle result = new Rectangle(origin);
        float nearest = Float.POSITIVE_INFINITY;
        List<Float> columns = new ArrayList<>(List.of(origin.x, 0f, maxX));
        for (Rectangle bounds : occupied) {
            columns.add(MathUtils.clamp(bounds.x - gap - origin.width, 0, maxX));
            columns.add(MathUtils.clamp(bounds.x + bounds.width + gap, 0, maxX));
        }
        var sorted = occupied.stream().sorted(Comparator.comparingDouble(bounds -> bounds.y)).toList();
        for (float left : columns) {
            float bottom = 0;
            for (int index = 0; index <= sorted.size(); index++) {
                Rectangle obstacle = index < sorted.size() ? sorted.get(index) : null;
                if (obstacle != null && (left + origin.width + gap <= obstacle.x
                      || left >= obstacle.x + obstacle.width + gap)) {
                    continue;
                }
                float top = obstacle == null ? maxY : Math.min(maxY, obstacle.y - gap - origin.height);
                if (bottom <= top) {
                    float candidateY = MathUtils.clamp(origin.y, bottom, top);
                    float distance = (left - origin.x) * (left - origin.x)
                          + (candidateY - origin.y) * (candidateY - origin.y);
                    if (distance < nearest) {
                        nearest = distance;
                        result.setPosition(left, candidateY);
                    }
                }
                if (obstacle != null) {
                    bottom = Math.max(bottom, obstacle.y + obstacle.height + gap);
                }
            }
        }
        return result;
    }

    private void renderHexText() {
        if (textTiles != scene.tiles() || textTuning != BoardGeometry.revision()) {
            cacheHexText();
        }
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        annotationBatch.setProjectionMatrix(boardCamera.camera.combined);
        annotationBatch.begin();
        for (var group : hexTextByHeight.entrySet()) {
            annotationBatch.setTransformMatrix(new Matrix4().setToTranslation(0, 0,
                  group.getKey() * BoardGeometry.LEVEL + 0.6f));
            for (HexTextChunk chunk : group.getValue()) {
                if (boardCamera.camera.frustum.boundsInFrustum(chunk.bounds())) {
                    chunk.glyphs().draw(annotationBatch);
                }
            }
        }
        annotationBatch.end();
        annotationBatch.setTransformMatrix(new Matrix4());
    }

    /** Immutable label vertices share the font atlas and are reused in both views until their source changes. */
    private void cacheHexText() {
        BitmapFont font = ui.font();
        float scaleX = font.getData().scaleX;
        float scaleY = font.getData().scaleY;
        Color color = new Color(font.getColor());
        Map<Integer, Map<Coords, HexTextChunk>> groups = new TreeMap<>();
        try {
            for (BoardScene.Tile tile : scene.tiles()) {
                for (BoardView.HexText label : tile.text()) {
                    int height = tile.elevation() + label.elevation();
                    Coords cell = new Coords(tile.coords().getX() / GpuTerrain.CHUNK_SIZE,
                          tile.coords().getY() / GpuTerrain.CHUNK_SIZE);
                    HexTextChunk chunk = groups.computeIfAbsent(height, key -> new HashMap<>())
                          .computeIfAbsent(cell, key -> new HexTextChunk(new BitmapFontCache(font, false), new BoundingBox().inf()));
                    float centerX = BoardGeometry.centerX(tile.coords());
                    float centerY = BoardGeometry.centerY(tile.coords());
                    float z = height * BoardGeometry.LEVEL;
                    chunk.bounds().ext(centerX - BoardGeometry.WIDTH, centerY - BoardGeometry.WIDTH, z - 1)
                          .ext(centerX + BoardGeometry.WIDTH, centerY + BoardGeometry.WIDTH, z + 1);
                    font.getData().setScale(label.font().getSize2D() / GpuBoardUi.FONT_RESOLUTION);
                    int argb = label.argb();
                    font.setColor(((argb >>> 16) & 255) / 255f, ((argb >>> 8) & 255) / 255f,
                          (argb & 255) / 255f, ((argb >>> 24) & 255) / 255f);
                    GlyphLayout layout = new GlyphLayout(font, label.text());
                    float x = centerX;
                    float baseline = centerY + BoardGeometry.HEIGHT / 2 - label.baseline() * BoardGeometry.HEX_SCALE;
                    var roof = label.elevation() > 0 ? terrain.roofBounds(tile.coords()) : null;
                    if (roof != null) {
                        float fit = Math.min(1, Math.max(8, roof.getWidth() - 4 * BoardGeometry.HEX_SCALE) / layout.width);
                        font.getData().setScale(font.getData().scaleX * fit);
                        layout.setText(font, label.text());
                        x = roof.getCenterX();
                        baseline = roof.getCenterY() - layout.height / 2;
                    }
                    chunk.glyphs().addText(layout, x - layout.width / 2,
                          label.fromTop() ? baseline : baseline + layout.height);
                }
            }
        } finally {
            font.getData().setScale(scaleX, scaleY);
            font.setColor(color);
        }
        hexTextByHeight.clear();
        groups.forEach((height, chunks) -> hexTextByHeight.put(height, List.copyOf(chunks.values())));
        textTiles = scene.tiles();
        textTuning = BoardGeometry.revision();
    }

    static float annotationScale(float displayScale) {
        return 1.4f * Math.max(1, displayScale) / EntitySprite.ANNOTATION_RESOLUTION;
    }

    private void renderSelectionOutlines() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        lines.setProjectionMatrix(boardCamera.camera.combined);
        lines.begin(ShapeRenderer.ShapeType.Filled);
        boolean showTargets = SHOW_TARGET_MARKERS && (!HIDE_TARGET_MARKERS_DURING_ATTACKS || playback.attacks().isEmpty());
        for (BoardScene.Unit unit : scene.units()) {
            if (unit.id() == scene.selectedId() || (hovered != null && unit.footprint().contains(hovered))) {
                lines.setColor(unit.sensorContact() ? Color.ORANGE : unit.id() == scene.selectedId()
                      ? Color.CYAN : Color.WHITE);
                float lift = unit.id() == scene.selectedId() ? selectionBob(hoverClock) : 0;
                unitBand(unit, SELECTION_BAND_WIDTH, lift, false);
            }
            if (showTargets && fireControl.targets(unit.id())) {
                lines.setColor(Color.RED);
                unitBand(unit, TARGET_BAND_WIDTH, targetBob(hoverClock), true);
            }
        }
        lines.end();
        lines.setTransformMatrix(selectionTransform.idt());
        lines.begin(ShapeRenderer.ShapeType.Line);
        if (hovered != null && scene.tile(hovered) != null && !ui.hit(Gdx.input.getX(), Gdx.input.getY())) {
            BoardScene.Tile tile = scene.tile(hovered);
            lines.setColor(Color.WHITE);
            ring(hovered, tile.elevation());
        }
        lines.end();
    }

    /** Stay above the support plane throughout the wave, including when the unit is standing on terrain. */
    static float selectionBob(float seconds) {
        return markerBob(seconds, SELECTION_BOB_HEIGHT_OFFSET, SELECTION_BOB_HEIGHT_LEVELS, SELECTION_BOB_PERIOD_SECONDS);
    }

    static float targetBob(float seconds) {
        return markerBob(seconds, TARGET_BOB_HEIGHT_OFFSET, TARGET_BOB_HEIGHT_LEVELS, TARGET_BOB_PERIOD_SECONDS);
    }

    private static float markerBob(float seconds, float offset, float height, float period) {
        return offset + height * BoardGeometry.LEVEL * (1 - MathUtils.cos(MathUtils.PI2 * seconds / period)) / 2;
    }

    private void unitBand(BoardScene.Unit unit, float width, float lift, boolean dashed) {
        UnitFootprint.Pose pose = unitFootprints.get(unit);
        float base = unitIcons.active() ? unitIcons.instance(unit).transform.getTranslation(new Vector3()).z : pose.position().z;
        lines.setTransformMatrix(selectionTransform.setToTranslation(0, 0, base + .5f + lift));
        for (Coords occupied : pose.unit().footprint()) {
            hexBand(pose, occupied, width, dashed);
        }
    }

    private void hexBand(UnitFootprint.Pose pose, Coords occupied, float width, boolean dashed) {
        for (int edge = 0; edge < 6; edge++) {
            Vector3 outer = pose.outlinePoint(occupied, edge);
            Vector3 nextOuter = pose.outlinePoint(occupied, edge + 1);
            Vector3 inner = pose.outlinePoint(occupied, edge, BoardGeometry.MARKER_INSET + width);
            Vector3 nextInner = pose.outlinePoint(occupied, edge + 1, BoardGeometry.MARKER_INSET + width);
            // Leave the middle half of every edge open, joining the remaining quarters at the corners.
            bandSegment(outer, nextOuter, inner, nextInner, 0, dashed ? .25f : 1);
            if (dashed) {
                bandSegment(outer, nextOuter, inner, nextInner, .75f, 1);
            }
        }
    }

    private void bandSegment(Vector3 outer, Vector3 nextOuter, Vector3 inner, Vector3 nextInner, float from, float to) {
        float ax = MathUtils.lerp(outer.x, nextOuter.x, from), ay = MathUtils.lerp(outer.y, nextOuter.y, from);
        float bx = MathUtils.lerp(outer.x, nextOuter.x, to), by = MathUtils.lerp(outer.y, nextOuter.y, to);
        float cx = MathUtils.lerp(inner.x, nextInner.x, from), cy = MathUtils.lerp(inner.y, nextInner.y, from);
        float dx = MathUtils.lerp(inner.x, nextInner.x, to), dy = MathUtils.lerp(inner.y, nextInner.y, to);
        lines.triangle(ax, ay, bx, by, cx, cy);
        lines.triangle(cx, cy, bx, by, dx, dy);
    }

    private void ring(Coords coords, float elevation) {
        // The outline stays inside the hex: on the shared edge it lies in the terrain's plane there, and the part
        // that spills onto a neighbour reads at that neighbour's level.
        Vector3 center = BoardGeometry.center(coords, elevation);
        Vector3 first = new Vector3();
        Vector3 second = new Vector3();
        for (int edge = 0; edge < 6; edge++) {
            lines.line(BoardGeometry.markerPoint(BoardGeometry.corner(first, coords, elevation, edge), center)
                        .add(0, 0, 0.5f),
                  BoardGeometry.markerPoint(BoardGeometry.corner(second, coords, elevation, edge + 1), center)
                        .add(0, 0, 0.5f));
        }
    }

    /** A held rotate key starts the next turn as soon as the previous one has finished playing. */
    private void continueRotation(int direction) {
        if (!boardCamera.isRotating()) {
            boardCamera.rotateStep(direction);
        }
    }

    Vector3 screenPosition(Coords coords) {
        Vector3 point = boardCamera.camera.project(BoardGeometry.center(coords, scene.tile(coords).elevation()),
              0, ui.bottomPixels(), boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight);
        point.y = Gdx.graphics.getHeight() - point.y;
        return point;
    }

    private final class BoardInput extends InputAdapter {
        private int dragX;
        private int dragY;
        private boolean panning;
        private boolean orbiting;
        private boolean dragged;
        private boolean boardGesture;
        private int gestureButton;
        private int startX;
        private int startY;
        private record KeyPress(int code, int modifiers) { }
        private final Map<Integer, KeyPress> pressedKeys = new HashMap<>();

        private Coords pick(int x, int y) {
            if (scene == null) {
                return null;
            }
            var ray = boardCamera.camera.getPickRay(x, y, 0, ui.bottomPixels(),
                  boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight);
            var ground = terrain.hit(scene, ray);
            float nearest = ground == null ? Float.POSITIVE_INFINITY : ground.distance();
            Coords coords = ground == null ? null : ground.coords();
            float nearestUnit = unitIcons.active() ? Float.POSITIVE_INFINITY : nearest;
            for (BoardScene.Unit unit : scene.units()) {
                var instance = unitIcons.active() ? unitIcons.instance(unit) : unitInstances.get(unit.id() + ":" + unit.part());
                if (instance != null) {
                    float distance = unitPicking.distance(instance, ray);
                    if (distance < nearestUnit) {
                        nearestUnit = distance;
                        nearest = distance;
                        coords = unit.location().coords();
                    }
                }
            }
            for (var entry : markers.locatedInstances().entrySet()) {
                float distance = unitPicking.distance(entry.getValue(), ray);
                if (distance < nearest) {
                    nearest = distance;
                    coords = entry.getKey().coords();
                }
            }
            return coords;
        }

        private void overlayInput(int event, int x, int y, Runnable fallback) {
            source.overlayInput(event, Math.round(x / ui.hudScale()),
                  Math.round((y - ui.topPixels()) / ui.hudScale()), fallback);
        }

        @Override
        public boolean touchDown(int x, int y, int pointer, int button) {
            if (boardGesture || ui.hit(x, y)) {
                return false;
            }
            dragX = x;
            dragY = y;
            startX = x;
            startY = y;
            boardGesture = true;
            gestureButton = button;
            dragged = false;
            panning = button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE;
            orbiting = panning && (modifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
            if (panning) {
                ui.closeMenu();
            }
            if (button == Input.Buttons.LEFT) {
                Coords coords = pick(x, y);
                int mods = modifiers();
                boolean plotting = ui.plotting() && !GpuBoardSource.isMeasurement(mods);
                overlayInput(MouseEvent.MOUSE_PRESSED, x, y, () -> {
                    if (plotting) {
                        source.hover(coords, mods);
                    }
                });
            }
            return true;
        }

        @Override
        public boolean touchDragged(int x, int y, int pointer) {
            if (!boardGesture) {
                return false;
            }
            if (panning) {
                if (!dragged && Math.abs(x - startX) + Math.abs(y - startY) < 5 * layoutScale) {
                    return true;
                }
                dragged = true;
                if (orbiting) {
                    boardCamera.orbit((x - dragX) * 0.3f / layoutScale, (y - dragY) * 0.3f / layoutScale);
                } else {
                    boardCamera.pan(x - dragX, y - dragY);
                }
            } else if (!ui.hit(x, y)) {
                Coords coords = pick(x, y);
                int mods = modifiers();
                boolean plotting = ui.plotting() && !GpuBoardSource.isMeasurement(mods);
                overlayInput(MouseEvent.MOUSE_DRAGGED, x, y, () -> {
                    if (plotting) {
                        source.hover(coords, mods);
                    }
                });
            }
            dragX = x;
            dragY = y;
            return true;
        }

        @Override
        public boolean touchUp(int x, int y, int pointer, int button) {
            if (!boardGesture || button != gestureButton) {
                return false;
            }
            if (button == Input.Buttons.RIGHT && !orbiting && !dragged && !ui.hit(x, y)) {
                ui.inspect(pick(x, y), x, y);
            } else if (!panning && button == Input.Buttons.LEFT && !ui.hit(x, y)) {
                Coords coords = pick(x, y);
                int mods = modifiers();
                overlayInput(MouseEvent.MOUSE_RELEASED, x, y,
                      () -> Gdx.app.postRunnable(() -> {
                          if (ui.plotting() || GpuBoardSource.isMeasurement(mods)) {
                              source.click(coords, false, mods);
                          } else {
                              ui.inspect(coords, x, y);
                          }
                      }));
            }
            reset();
            return true;
        }

        void reset() {
            panning = false;
            orbiting = false;
            dragged = false;
            boardGesture = false;
        }

        @Override
        public boolean mouseMoved(int x, int y) {
            hovered = ui.hit(x, y) ? null : pick(x, y);
            source.setHover(hovered);
            source.setPointer(ui.hit(x, y) ? -1 : Math.round(x / ui.hudScale()),
                  ui.hit(x, y) ? -1 : Math.round((y - ui.topPixels()) / ui.hudScale()));
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            if (!ui.hit(Gdx.input.getX(), Gdx.input.getY())) {
                boardCamera.zoomAt((float) Math.pow(1.12, amountY), Gdx.input.getX(),
                      Gdx.graphics.getHeight() - Gdx.input.getY() - ui.bottomPixels());
                return true;
            }
            return false;
        }

        @Override
        public boolean keyDown(int key) {
            if (pressedKeys.containsKey(key) || cameraKeys.containsKey(key)) {
                return true;
            }
            int awt = awtKey(key);
            int modifiers = modifiers();
            if (ui.isTextEditing() && !isWindowShortcut(key)) {
                return true;
            }
            if (!source.chatActive() && ui.key(key, true)) {
                return true;
            }
            if (!ui.acceptsCameraKeys() && !isWindowShortcut(key)) {
                return true;
            }
            if (!source.chatActive()) {
                for (KeyCommandBind command : KeyCommandBind.getAllBindsByKey(awt, modifiers)) {
                    if (command == KeyCommandBind.CENTER_ON_SELECTED && isMoving()) {
                        playback.finish();
                    }
                    if (cameraCommand(key, command)) {
                        return true;
                    }
                }
            }
            if (awt != KeyEvent.VK_UNDEFINED) {
                pressedKeys.put(key, new KeyPress(awt, modifiers));
                source.key(awt, true, modifiers);
            }
            return awt != KeyEvent.VK_UNDEFINED;
        }

        /**
         * Applies a bind that moves the camera. Held binds are only noted here and applied every frame while the key
         * stays down.
         *
         * @return {@code false} for a bind that is not a camera command, which is then left to the Swing key dispatcher
         */
        private boolean cameraCommand(int key, KeyCommandBind command) {
            switch (command) {
                case SCROLL_NORTH, SCROLL_SOUTH, SCROLL_EAST, SCROLL_WEST, CAMERA_TILT_UP, CAMERA_TILT_DOWN ->
                      cameraKeys.put(key, command);
                case CAMERA_ROTATE_LEFT, CAMERA_ROTATE_RIGHT -> {
                    if (ui.acceptsCameraKeys()) {
                        boardCamera.rotateStep(command == KeyCommandBind.CAMERA_ROTATE_LEFT ? -1 : 1);
                    } else {
                        LOGGER.debug("[GpuCamera] {} ignored: a menu is open", command);
                    }
                    cameraKeys.put(key, command);
                }
                case TOGGLE_ISO -> boardCamera.setIsometric(!boardCamera.isIsometric());
                case ZOOM_IN -> boardCamera.zoom(1 / 1.2f);
                case ZOOM_OUT -> boardCamera.zoom(1.2f);
                case CAMERA_RESET, CAMERA_FIT_BOARD, ZOOM_OVERVIEW_TOGGLE -> frameBoard(command);
                default -> {
                    return false;
                }
            }
            LOGGER.debug("[GpuCamera] {} handled by the GPU camera", command);
            return true;
        }

        /** These camera moves measure the board, so they wait until the first frame has delivered one. */
        private void frameBoard(KeyCommandBind command) {
            if (scene == null) {
                LOGGER.debug("[GpuCamera] {} ignored: no board has been drawn yet", command);
                return;
            }
            switch (command) {
                case CAMERA_RESET -> boardCamera.reset(scene);
                case CAMERA_FIT_BOARD -> boardCamera.fit(scene);
                case ZOOM_OVERVIEW_TOGGLE -> boardCamera.toggleOverview(scene);
                default -> { }
            }
        }

        @Override
        public boolean keyUp(int key) {
            if (cameraKeys.remove(key) != null) {
                return true;
            }
            KeyPress pressed = pressedKeys.remove(key);
            if (pressed != null) {
                source.key(pressed.code(), false, pressed.modifiers());
            }
            return pressed != null;
        }

        @Override
        public boolean keyTyped(char character) {
            source.keyTyped(character);
            return true;
        }
    }

    private boolean isWindowShortcut(int key) {
        if (source.chatActive()) {
            return false;
        }
        int modifiers = modifiers();
        int awt = awtKey(key);
        if (ui.isTextEditing() && (modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK
              | InputEvent.META_DOWN_MASK)) == 0 && !(awt >= KeyEvent.VK_F1 && awt <= KeyEvent.VK_F12)
              && !(awt >= KeyEvent.VK_F13 && awt <= KeyEvent.VK_F24)) {
            return false;
        }
        return KeyCommandBind.getAllBindsByKey(awt, modifiers).stream().anyMatch(command -> switch (command) {
            case ZOOM_IN, ZOOM_OUT, ZOOM_OVERVIEW_TOGGLE, TOGGLE_ISO -> false;
            case UD_GENERAL, UD_PILOT, UD_ARMOR, UD_WEAPONS, UD_SYSTEMS, UD_EXTRAS,
                 BOT_COMMANDS, PAUSE, UNPAUSE, EXTEND_TURN_TIMER,
                 REPORT_KEY_NEXT, REPORT_KEY_PREV, REPORT_KEY_SELECT_NEXT, REPORT_KEY_SELECT_PREVIOUS,
                 REPORT_FILTER_KEY_SELECT_NEXT, REPORT_KEY_FILTER -> true;
            default -> command.isMenuBar;
        });
    }

    static int awtKey(int key) {
        boolean numLock = true;
        if (key >= Input.Keys.NUMPAD_0 && key <= Input.Keys.NUMPAD_DOT) {
            try {
                numLock = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK);
            } catch (UnsupportedOperationException ignored) {
                // Some window systems do not expose lock state; keep the numeric keypad usable there.
            }
        }
        return awtKey(key, numLock);
    }

    static int awtKey(int key, boolean numLock) {
        if (!numLock) {
            int navigation = switch (key) {
                case Input.Keys.NUMPAD_0 -> KeyEvent.VK_INSERT;
                case Input.Keys.NUMPAD_1 -> KeyEvent.VK_END;
                case Input.Keys.NUMPAD_2 -> KeyEvent.VK_KP_DOWN;
                case Input.Keys.NUMPAD_3 -> KeyEvent.VK_PAGE_DOWN;
                case Input.Keys.NUMPAD_4 -> KeyEvent.VK_KP_LEFT;
                case Input.Keys.NUMPAD_5 -> KeyEvent.VK_CLEAR;
                case Input.Keys.NUMPAD_6 -> KeyEvent.VK_KP_RIGHT;
                case Input.Keys.NUMPAD_7 -> KeyEvent.VK_HOME;
                case Input.Keys.NUMPAD_8 -> KeyEvent.VK_KP_UP;
                case Input.Keys.NUMPAD_9 -> KeyEvent.VK_PAGE_UP;
                case Input.Keys.NUMPAD_DOT -> KeyEvent.VK_DELETE;
                default -> KeyEvent.VK_UNDEFINED;
            };
            if (navigation != KeyEvent.VK_UNDEFINED) {
                return navigation;
            }
        }
        if (key >= Input.Keys.A && key <= Input.Keys.Z) {
            return KeyEvent.VK_A + key - Input.Keys.A;
        }
        if (key >= Input.Keys.NUM_0 && key <= Input.Keys.NUM_9) {
            return KeyEvent.VK_0 + key - Input.Keys.NUM_0;
        }
        if (key >= Input.Keys.F1 && key <= Input.Keys.F12) {
            return KeyEvent.VK_F1 + key - Input.Keys.F1;
        }
        if (key >= Input.Keys.F13 && key <= Input.Keys.F24) {
            return KeyEvent.VK_F13 + key - Input.Keys.F13;
        }
        if (key >= Input.Keys.NUMPAD_0 && key <= Input.Keys.NUMPAD_9) {
            return KeyEvent.VK_NUMPAD0 + key - Input.Keys.NUMPAD_0;
        }
        return switch (key) {
            case Input.Keys.UP -> KeyEvent.VK_UP;
            case Input.Keys.DOWN -> KeyEvent.VK_DOWN;
            case Input.Keys.LEFT -> KeyEvent.VK_LEFT;
            case Input.Keys.RIGHT -> KeyEvent.VK_RIGHT;
            case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> KeyEvent.VK_ENTER;
            case Input.Keys.TAB -> KeyEvent.VK_TAB;
            case Input.Keys.ESCAPE -> KeyEvent.VK_ESCAPE;
            case Input.Keys.SPACE -> KeyEvent.VK_SPACE;
            case Input.Keys.BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case Input.Keys.FORWARD_DEL -> KeyEvent.VK_DELETE;
            case Input.Keys.INSERT -> KeyEvent.VK_INSERT;
            case Input.Keys.HOME -> KeyEvent.VK_HOME;
            case Input.Keys.END -> KeyEvent.VK_END;
            case Input.Keys.PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case Input.Keys.PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            case Input.Keys.MINUS -> KeyEvent.VK_MINUS;
            case Input.Keys.EQUALS, Input.Keys.NUMPAD_EQUALS -> KeyEvent.VK_EQUALS;
            case Input.Keys.COMMA -> KeyEvent.VK_COMMA;
            case Input.Keys.PERIOD -> KeyEvent.VK_PERIOD;
            case Input.Keys.SLASH -> KeyEvent.VK_SLASH;
            case Input.Keys.BACKSLASH -> KeyEvent.VK_BACK_SLASH;
            case Input.Keys.SEMICOLON -> KeyEvent.VK_SEMICOLON;
            case Input.Keys.APOSTROPHE -> KeyEvent.VK_QUOTE;
            case Input.Keys.GRAVE -> KeyEvent.VK_BACK_QUOTE;
            case Input.Keys.LEFT_BRACKET -> KeyEvent.VK_OPEN_BRACKET;
            case Input.Keys.RIGHT_BRACKET -> KeyEvent.VK_CLOSE_BRACKET;
            case Input.Keys.PLUS -> KeyEvent.VK_PLUS;
            case Input.Keys.NUMPAD_ADD -> KeyEvent.VK_ADD;
            case Input.Keys.NUMPAD_SUBTRACT -> KeyEvent.VK_SUBTRACT;
            case Input.Keys.NUMPAD_MULTIPLY -> KeyEvent.VK_MULTIPLY;
            case Input.Keys.NUMPAD_DIVIDE -> KeyEvent.VK_DIVIDE;
            case Input.Keys.NUMPAD_DOT -> KeyEvent.VK_DECIMAL;
            case Input.Keys.NUMPAD_COMMA -> KeyEvent.VK_SEPARATOR;
            case Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> KeyEvent.VK_SHIFT;
            case Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> KeyEvent.VK_CONTROL;
            case Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT -> KeyEvent.VK_ALT;
            case Input.Keys.SYM -> KeyEvent.VK_META;
            case Input.Keys.CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK;
            case Input.Keys.NUM_LOCK -> KeyEvent.VK_NUM_LOCK;
            case Input.Keys.SCROLL_LOCK -> KeyEvent.VK_SCROLL_LOCK;
            case Input.Keys.PAUSE -> KeyEvent.VK_PAUSE;
            case Input.Keys.PRINT_SCREEN -> KeyEvent.VK_PRINTSCREEN;
            case Input.Keys.MENU -> KeyEvent.VK_CONTEXT_MENU;
            default -> KeyEvent.VK_UNDEFINED;
        };
    }

    @Override
    public void pause() {
        cameraKeys.clear();
        boardInput.pressedKeys.clear();
        boardInput.reset();
        if (source != null) {
            source.stopKeys();
        }
    }

    static int modifiers() {
        int result = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
            result |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            result |= InputEvent.CTRL_DOWN_MASK;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)) {
            result |= InputEvent.ALT_DOWN_MASK;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SYM)) {
            result |= InputEvent.META_DOWN_MASK;
        }
        return result;
    }

    long frames() {
        return frames;
    }

    boolean isMoving() {
        return playback.busy();
    }

    @Override
    public void dispose() {
        if (loadingStage != null) {
            loadingStage.dispose();
            loadingTheme.dispose();
        }
        playback.clear();
        groundSurfaces.clear();
        animators.clear();
        unitInstances.clear();
        unitBounds.begin();
        unitAnchors.clear();
        unitTints.clear();
        unitDamage.clear();
        equipmentAppearance.clear();
        upperBodyTurns.clear();
        armFlips.clear();
        unitFootprints.clear();
        hover.clear();
        unitPicking.clear();
        hexTextByHeight.clear();
        textTiles = null;
        if (ui != null) {
            ui.dispose();
        }
        if (source != null) {
            source.stopKeys();
        }
        spriteModels.values().forEach(GpuUnitModel::dispose);
        spriteModels.clear();
        if (unitModels != null) {
            unitModels.dispose();
        }
        camouflage.dispose();
        damageDisplay.dispose();
        jumpJets.dispose();
        unitIcons.dispose();
        attackEffects.dispose();
        if (unitBatch != null) {
            unitBatch.dispose();
        }
        if (annotationBatch != null) {
            annotationBatch.dispose();
        }
        if (annotationTextures != null) {
            annotationTextures.dispose();
        }
        if (unitTextures != null) {
            unitTextures.dispose();
        }
        if (terrain != null) {
            terrain.dispose();
        }
        if (fireControl != null) {
            fireControl.dispose();
        }
        if (tactical != null) {
            tactical.dispose();
        }
        if (atmosphere != null) {
            atmosphere.dispose();
        }
        if (unitVisibility != null) {
            unitVisibility.dispose();
        }
        if (markers != null) {
            markers.dispose();
        }
        if (lines != null) {
            lines.dispose();
        }
        fieldOfView.dispose();
        if (source != null) {
            source.close();
        }
    }
}
