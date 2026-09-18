/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

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
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.EntitySprite;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.board.Coords;
import megamek.logging.MMLogger;

/** GPU board and Scene2D controls. The source remains the sole bridge to the existing client. */
class GpuBattleView extends ApplicationAdapter {
    private static final boolean SPREAD_UNIT_ANNOTATIONS = false;
    private static final float UNIT_ANNOTATION_MAX_OFFSCREEN_DISTANCE = -1;
    /** Airborne meeples hover: one full wave cycle lasts this long. */
    static final float HOVER_PERIOD_SECONDS = 2.6f;
    /** Height of that wave in terrain levels, so a floating token drifts off its flight height. */
    static final float HOVER_LEVELS = 0.2f;
    /** Fraction of a cycle between neighboring units, so floating units do not bob in lockstep. */
    private static final float HOVER_PHASE_STEP = 0.381966f;
    /** Floating meeples are tied to their hex with this faint solid stem; solid, so it needs no blending state. */
    private static final Color TETHER_COLOR = Color.valueOf("A9B8B8");
    private static final double[] PLAYBACK_SPEEDS = { 1, 0.5, 2, 0 };
    private static final String[] SPEED_LABELS = { "1x", "0.5x", "2x", Messages.getString("GpuBoard.instant") };
    private static final float TILT_DEGREES_PER_SECOND = 60;
    private static final MMLogger LOGGER = MMLogger.create(GpuBattleView.class);
    private final GpuBoardSource source;
    private final GpuDisplayScale displayScale = new GpuDisplayScale();
    final BoardCamera boardCamera = new BoardCamera();
    private final Map<Integer, UnitMotion> motions = new HashMap<>();
    private final Map<BoardScene.Pixels, GpuMeeple> meeples = new HashMap<>();
    private final GpuUnitModels unitModels = GpuUnitModels.ENABLED ? new GpuUnitModels() : null;
    private final Map<BoardScene.Unit, Vector3> unitAnchors = new HashMap<>();
    private final Map<String, ModelInstance> unitInstances = new HashMap<>();
    private final Map<String, BoardScene.Pixels> unitTints = new HashMap<>();
    private final Map<Integer, KeyCommandBind> cameraKeys = new HashMap<>();
    private final BoardInput boardInput = new BoardInput();
    private final List<Hover> hover = new ArrayList<>();
    private GpuTerrain terrain;
    private GpuFireControl fireControl;
    private GpuAtmosphere atmosphere;
    private GpuUnitVisibility unitVisibility;
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
    private int speedIndex;
    private boolean fitted;
    private long frames;
    private float hoverClock;
    private long boardGeneration;
    private long centerSequence;
    private int layoutWidth;
    private int layoutHeight;
    private int layoutPixelWidth;
    private int layoutPixelHeight;
    private float layoutScale;
    private float layoutPreference;
    private long hoverCameraRevision;

    GpuBattleView(GpuBoardSource source) {
        this.source = source;
    }

    @Override
    public void create() {
        terrain = new GpuTerrain();
        fireControl = new GpuFireControl();
        atmosphere = new GpuAtmosphere();
        unitVisibility = new GpuUnitVisibility();
        unitTextures = new GpuTextures<>();
        annotationTextures = new GpuTextures<>();
        unitBatch = new ModelBatch();
        annotationBatch = new SpriteBatch();
        lines = new ShapeRenderer();
        ui = new GpuBoardUi(source, boardCamera, () -> speedIndex = (speedIndex + 1) % PLAYBACK_SPEEDS.length);
        Gdx.input.setInputProcessor(new InputMultiplexer(ui.stage, boardInput));
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
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
        if (scene != null && (boardGeneration != frame.boardGeneration() || scene.boardId() != frame.scene().boardId() || scene.width() != frame.scene().width()
              || scene.height() != frame.scene().height())) {
            motions.clear();
            hovered = null;
            fitted = false;
        }
        boolean changedTiles = scene == null || scene.tiles() != frame.scene().tiles();
        scene = frame.scene();
        boardGeneration = frame.boardGeneration();
        ui.update(frame, Messages.getString("GpuBoard.speed", SPEED_LABELS[speedIndex]));
        terrain.update(scene);
        fireControl.update(scene);
        atmosphere.configure(ui.atmosphere());
        terrain.setAtmosphere(atmosphere.lighting());
        if (unitTextures.update(scene.units().stream().map(BoardScene.Unit::image).distinct()
              .collect(Collectors.toMap(pixels -> pixels, pixels -> pixels)))) {
            meeples.values().forEach(GpuMeeple::dispose);
            meeples.clear();
        }
        Set<BoardScene.Pixels> images = scene.units().stream().map(BoardScene.Unit::image).collect(Collectors.toSet());
        meeples.entrySet().removeIf(entry -> {
            if (images.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().dispose();
            return true;
        });
        annotationTextures.update(scene.units().stream().collect(Collectors.toMap(
              unit -> unit.id() + ":" + unit.part(), BoardScene.Unit::annotations)));
        if (!fitted) {
            boardCamera.fit(scene);
            fitted = true;
            // A unit-list click can switch boards. Do not consume its pending focus request while fitting.
            centerSequence = 0;
        }
        if (centerSequence != frame.centerRequest().sequence()) {
            centerSequence = frame.centerRequest().sequence();
            Coords center = frame.centerRequest().coords();
            if (center != null && scene.tile(center) != null) {
                boardCamera.center(BoardGeometry.center(center, scene.tile(center).elevation()));
            }
        }
        boardCamera.advance(Gdx.graphics.getDeltaTime());
        if (ui.acceptsCameraKeys()) {
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
        Set<Integer> visible = scene.units().stream().filter(unit -> !unit.sensorContact())
              .map(BoardScene.Unit::id).collect(Collectors.toSet());
        motions.keySet().retainAll(visible);
        for (BoardScene.Movement movement : frame.movements()) {
            if (movement.boardId() == scene.boardId() && visible.contains(movement.entityId())
                  && !movement.path().isEmpty()) {
                motions.computeIfAbsent(movement.entityId(), id -> new UnitMotion(movement.path().getFirst()))
                      .append(movement.path(), movement.type(), movement.jumpMP());
            }
        }
        for (UnitMotion motion : motions.values()) {
            motion.advance(Gdx.graphics.getDeltaTime(), PLAYBACK_SPEEDS[speedIndex]);
        }
        hoverClock += Gdx.graphics.getDeltaTime();
        prepareUnits();
        applyHover();
        List<ModelInstance> units = new ArrayList<>(unitInstances.values());
        float seeThrough = ui.seeThrough();
        terrain.animate(Gdx.graphics.getDeltaTime(), units, ui.buildingOpacity(), ui.treeOpacity());
        terrain.renderShadows(boardCamera.camera, units);
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        atmosphere.begin((int) boardCamera.camera.viewportWidth, (int) boardCamera.camera.viewportHeight,
              Gdx.graphics.getDeltaTime(), seeThrough > 0 && !units.isEmpty());
        terrain.render(boardCamera.camera, false);
        renderUnits();
        renderTethers();
        terrain.renderTransparent(boardCamera.camera);
        atmosphere.end(boardCamera.camera, terrain, units, scene, ui.bottomPixels());
        atmosphere.restoreDepth(boardCamera.camera, terrain, units);
        atmosphere.renderWeather(boardCamera.camera, scene);
        unitVisibility.render(boardCamera.camera, units, atmosphere.depthTexture(), ui.bottomPixels(), seeThrough, layoutScale);
        terrain.render(boardCamera.camera, true);
        fireControl.render(boardCamera.camera);
        renderHexText();
        renderSelectionOutlines();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        renderAnnotations();
        ui.draw();
        frames++;
    }

    private void prepareUnits() {
        hover.clear();
        unitAnchors.clear();
        unitInstances.keySet().retainAll(scene.units().stream().map(unit -> unit.id() + ":" + unit.part())
              .collect(Collectors.toSet()));
        unitTints.keySet().retainAll(unitInstances.keySet());
        for (BoardScene.Unit unit : scene.units()) {
            Vector3 position = BoardGeometry.center(unit.location().coords(), unit.location().elevation());
            float facing = unit.location().facing() * 60;
            UnitMotion motion = motions.get(unit.id());
            if (motion != null && motion.isMoving()) {
                position.sub(motion.destination()).add(unit.airborne() ? motion.position() : motion.surfacePosition(scene));
                facing = motion.facing();
            }
            BoardScene.Tile tile = scene.tile(unit.location().coords());
            if (tile != null && tile.waterDepth() == 0 && !tile.frozen() && !unit.airborne()
                  && MathUtils.isEqual(position.z, tile.elevation() * BoardGeometry.LEVEL)) {
                position.z = BoardGeometry.groundZ(tile);
            }
            GpuMeeple meeple = unitModels == null || unit.sensorContact() ? null : unitModels.get(unit.model());
            boolean authored = meeple != null;
            if (meeple == null) {
                meeple = meeples.computeIfAbsent(unit.image(), pixels ->
                      new GpuMeeple(pixels, unitTextures.region(pixels)));
            }
            String key = unit.id() + ":" + unit.part();
            ModelInstance instance = unitInstances.get(key);
            if (instance == null || instance.model != meeple.instance.model) {
                instance = new ModelInstance(meeple.instance.model);
                unitInstances.put(key, instance);
                unitTints.remove(key);
            }
            // Presentation-only color for the see-through pass; the normal model materials retain their artwork.
            if (!(instance.userData instanceof Color)) {
                instance.userData = new Color();
            }
            Color.rgb888ToColor((Color) instance.userData, unit.outlineRgb());
            if (authored && !unit.image().equals(unitTints.get(key))) {
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
            Vector3 anchor = meeple.place(instance, boardCamera.camera, position, facing, unit.height(), unit.part() >= 0);
            if (unit.airborne()) {
                float offset = hoverOffset(hoverClock, unit.id(), unit.part());
                hover.add(new Hover(instance, offset));
                anchor.add(0, 0, offset);
            }
            unitAnchors.put(unit, anchor);
        }
    }

    /** One floating meeple's drift for the current frame: a draw-time offset, never part of the game state. */
    private record Hover(ModelInstance instance, float offset) { }

    /** Apply the same hover transform before drawing, feature fading, and shadow capture. */
    private void applyHover() {
        for (Hover drifting : hover) {
            Vector3 center = drifting.instance().transform.getTranslation(new Vector3());
            drifting.instance().transform.setTranslation(center.add(0, 0, drifting.offset()));
        }
    }

    /**
     * Vertical hover offset in world units for an airborne meeple at the current animation time: a slow sine wave that
     * each unit starts at a different phase, so the floating tokens drift as a loose wave instead of rising and
     * falling together. A grounded unit (elevation or altitude 0) never gets an offset. This only moves the rendered
     * token; no game state changes.
     */
    static float hoverOffset(float seconds, int id, int part) {
        float phase = MathUtils.PI2 * HOVER_PHASE_STEP * (id + part);
        return HOVER_LEVELS * BoardGeometry.LEVEL
              * MathUtils.sin(MathUtils.PI2 * seconds / HOVER_PERIOD_SECONDS + phase);
    }

    /** Level a floating meeple's stem ends at, or NaN when no stem is drawn for this token at this time. */
    static float tetherGround(BoardScene.Unit unit, int tileElevation, Vector3 center, boolean moving) {
        float ground = tileElevation * BoardGeometry.LEVEL;
        return unit.airborne() && !moving && center.z > ground ? ground : Float.NaN;
    }

    /**
     * Faint stems from each floating meeple's center down to the center of the hex it occupies, so an airborne token
     * beside a hill or another raised tile still reads as belonging to the hex under it. A grounded token already
     * covers its hex, and a moving token is between hexes, so neither gets a stem.
     */
    private void renderTethers() {
        Vector3 center = new Vector3();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
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
    }

    private void renderUnits() {
        unitBatch.begin(boardCamera.camera);
        Vector3 position = new Vector3();
        for (BoardScene.Unit unit : unitAnchors.keySet()) {
            ModelInstance instance = unitInstances.get(unit.id() + ":" + unit.part());
            instance.transform.getTranslation(position);
            if (boardCamera.camera.frustum.sphereInFrustum(position,
                  BoardGeometry.WIDTH + unit.height() * BoardGeometry.LEVEL)) {
                unitBatch.render(instance, terrain.environment());
            }
        }
        unitBatch.end();
    }

    private void renderAnnotations() {
        annotationBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
              boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight));
        var ordered = unitAnchors.entrySet().stream().sorted(Comparator
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
            placed.add(SPREAD_UNIT_ANNOTATIONS
                  ? spreadAnnotation(bounds, placed, boardCamera.camera.viewportWidth,
                        boardCamera.camera.viewportHeight, 2 * layoutScale)
                  : bounds);
        }
        annotationBatch.begin();
        for (int index = ordered.size() - 1; index >= 0; index--) {
            BoardScene.Unit unit = ordered.get(index).getKey();
            Rectangle bounds = placed.get(index);
            annotationBatch.draw(annotationTextures.region(unit.id() + ":" + unit.part()),
                  bounds.x, bounds.y, bounds.width, bounds.height);
        }
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
        BitmapFont font = ui.boldFont();
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
        lines.begin(ShapeRenderer.ShapeType.Line);
        for (BoardScene.Unit unit : scene.units()) {
            if (unit.id() == scene.selectedId() || unit.location().coords().equals(hovered)) {
                lines.setColor(unit.sensorContact() ? Color.ORANGE : unit.id() == scene.selectedId()
                      ? Color.CYAN : Color.WHITE);
                ring(unit.location().coords(), unit.location().elevation());
            }
        }
        if (hovered != null && scene.tile(hovered) != null && !ui.hit(Gdx.input.getX(), Gdx.input.getY())) {
            BoardScene.Tile tile = scene.tile(hovered);
            lines.setColor(Color.WHITE);
            ring(hovered, tile.elevation());
        }
        lines.end();
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
        private int focusedUnit;
        private boolean skipHeld;

        private Coords pick(int x, int y) {
            return scene == null ? null : terrain.pick(scene,
                  boardCamera.camera.getPickRay(x, y, 0, ui.bottomPixels(),
                        boardCamera.camera.viewportWidth, boardCamera.camera.viewportHeight));
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
                boolean plotting = ui.plotting();
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
                boolean plotting = ui.plotting();
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
                          if (ui.plotting() || (mods & InputEvent.CTRL_DOWN_MASK) != 0) {
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
            if (key == Input.Keys.SPACE && (skipHeld || ui.acceptsCameraKeys() && isMoving())) {
                motions.values().forEach(UnitMotion::finish);
                skipHeld = true;
                return true;
            }
            if (ui.key(key, true)) {
                return true;
            }
            if (key == Input.Keys.TAB && scene != null && !scene.units().isEmpty()) {
                focusedUnit = Math.floorMod(focusedUnit + ((modifiers() & InputEvent.SHIFT_DOWN_MASK) != 0 ? -1 : 1),
                      scene.units().size());
                hovered = scene.units().get(focusedUnit).location().coords();
                source.setHover(hovered);
                return true;
            }
            // Enter with a modifier is a bound command such as Done, which must reach the phase display.
            boolean isPlainEnter = (key == Input.Keys.ENTER) && (modifiers() == 0);
            if (isPlainEnter && (hovered != null)) {
                Vector3 point = screenPosition(hovered);
                ui.inspect(hovered, (int) point.x, (int) point.y);
                return true;
            }
            int awt = awtKey(key);
            // Menu bar binds are included: this window has no menu bar to catch the camera shortcuts itself.
            for (KeyCommandBind command : KeyCommandBind.getAllBindsByKey(awt, modifiers())) {
                if (cameraCommand(key, command)) {
                    return true;
                }
            }
            if (awt != KeyEvent.VK_UNDEFINED) {
                source.key(awt, true, modifiers());
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
            if (key == Input.Keys.SPACE && skipHeld) {
                skipHeld = false;
                return true;
            }
            if (cameraKeys.remove(key) != null) {
                return true;
            }
            if (ui.key(key, false)) {
                return true;
            }
            int awt = awtKey(key);
            if (awt != KeyEvent.VK_UNDEFINED) {
                source.key(awt, false, modifiers());
            }
            return awt != KeyEvent.VK_UNDEFINED;
        }
    }

    private static int awtKey(int key) {
        if (key >= Input.Keys.A && key <= Input.Keys.Z) {
            return KeyEvent.VK_A + key - Input.Keys.A;
        }
        if (key >= Input.Keys.NUM_0 && key <= Input.Keys.NUM_9) {
            return KeyEvent.VK_0 + key - Input.Keys.NUM_0;
        }
        if (key >= Input.Keys.F1 && key <= Input.Keys.F12) {
            return KeyEvent.VK_F1 + key - Input.Keys.F1;
        }
        return switch (key) {
            case Input.Keys.UP -> KeyEvent.VK_UP;
            case Input.Keys.DOWN -> KeyEvent.VK_DOWN;
            case Input.Keys.LEFT -> KeyEvent.VK_LEFT;
            case Input.Keys.RIGHT -> KeyEvent.VK_RIGHT;
            case Input.Keys.ENTER -> KeyEvent.VK_ENTER;
            case Input.Keys.TAB -> KeyEvent.VK_TAB;
            case Input.Keys.ESCAPE -> KeyEvent.VK_ESCAPE;
            case Input.Keys.SPACE -> KeyEvent.VK_SPACE;
            case Input.Keys.BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case Input.Keys.FORWARD_DEL -> KeyEvent.VK_DELETE;
            case Input.Keys.HOME -> KeyEvent.VK_HOME;
            case Input.Keys.END -> KeyEvent.VK_END;
            case Input.Keys.PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case Input.Keys.PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            case Input.Keys.MINUS -> KeyEvent.VK_MINUS;
            case Input.Keys.EQUALS -> KeyEvent.VK_EQUALS;
            case Input.Keys.COMMA -> KeyEvent.VK_COMMA;
            case Input.Keys.PERIOD -> KeyEvent.VK_PERIOD;
            case Input.Keys.SLASH -> KeyEvent.VK_SLASH;
            case Input.Keys.NUMPAD_ADD -> KeyEvent.VK_ADD;
            case Input.Keys.NUMPAD_SUBTRACT -> KeyEvent.VK_SUBTRACT;
            default -> KeyEvent.VK_UNDEFINED;
        };
    }

    @Override
    public void pause() {
        cameraKeys.clear();
        boardInput.skipHeld = false;
        boardInput.reset();
        source.stopKeys();
    }

    private int modifiers() {
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
        return result;
    }

    long frames() {
        return frames;
    }

    boolean isMoving() {
        return motions.values().stream().anyMatch(UnitMotion::isMoving);
    }

    @Override
    public void dispose() {
        hexTextByHeight.clear();
        textTiles = null;
        if (ui != null) {
            ui.dispose();
        }
        source.stopKeys();
        meeples.values().forEach(GpuMeeple::dispose);
        if (unitModels != null) {
            unitModels.dispose();
        }
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
        if (atmosphere != null) {
            atmosphere.dispose();
        }
        if (unitVisibility != null) {
            unitVisibility.dispose();
        }
        if (lines != null) {
            lines.dispose();
        }
        source.close();
    }
}
