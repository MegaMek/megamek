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
import java.util.stream.Collectors;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.EntitySprite;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.board.Coords;

/** GPU board and Scene2D controls. The source remains the sole bridge to the existing client. */
class GpuBattleView extends ApplicationAdapter {
    private static final boolean SPREAD_UNIT_ANNOTATIONS = false;
    private static final float UNIT_ANNOTATION_MAX_OFFSCREEN_DISTANCE = -1;
    private static final double[] PLAYBACK_SPEEDS = { 1, 0.5, 2, 0 };
    private static final String[] SPEED_LABELS = { "1x", "0.5x", "2x", Messages.getString("GpuBoard.instant") };
    private final GpuBoardSource source;
    private final GpuDisplayScale displayScale = new GpuDisplayScale();
    final BoardCamera boardCamera = new BoardCamera();
    private final Map<Integer, UnitMotion> motions = new HashMap<>();
    private final Map<BoardScene.Pixels, GpuMeeple> meeples = new HashMap<>();
    private final Map<BoardScene.Unit, Vector3> unitAnchors = new HashMap<>();
    private final Map<String, ModelInstance> unitInstances = new HashMap<>();
    private final Map<Integer, KeyCommandBind> cameraKeys = new HashMap<>();
    private final BoardInput boardInput = new BoardInput();
    private GpuTerrain terrain;
    private GpuTextures<BoardScene.Pixels> unitTextures;
    private GpuTextures<String> annotationTextures;
    private ModelBatch unitBatch;
    private SpriteBatch annotationBatch;
    private ShapeRenderer lines;
    private GpuBoardUi ui;
    private BoardScene scene;
    private Coords hovered;
    private int speedIndex;
    private boolean fitted;
    private long frames;
    private long boardGeneration;
    private long centerSequence;
    private int layoutWidth;
    private int layoutHeight;
    private float layoutScale;
    private float layoutPreference;
    private long hoverCameraRevision;

    GpuBattleView(GpuBoardSource source) {
        this.source = source;
    }

    @Override
    public void create() {
        terrain = new GpuTerrain();
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
        if (width == layoutWidth && height == layoutHeight && scale == layoutScale && preference == layoutPreference) {
            return;
        }
        layoutWidth = width;
        layoutHeight = height;
        layoutScale = scale;
        layoutPreference = preference;
        ui.resize(width, height, scale);
        int boardHeight = Math.max(1, height - ui.topPixels() - ui.bottomPixels());
        boardCamera.resize(width, boardHeight, scene, scale);
        source.setViewport(Math.round(width / ui.hudScale()), Math.round(boardHeight / ui.hudScale()));
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
        scene = frame.scene();
        boardGeneration = frame.boardGeneration();
        ui.update(frame, Messages.getString("GpuBoard.speed", SPEED_LABELS[speedIndex]));
        terrain.update(scene);
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
            centerSequence = frame.centerRequest().sequence();
        }
        if (centerSequence != frame.centerRequest().sequence()) {
            centerSequence = frame.centerRequest().sequence();
            Coords center = frame.centerRequest().coords();
            if (center != null && scene.tile(center) != null) {
                boardCamera.center(BoardGeometry.center(center, scene.tile(center).elevation()));
            }
        }
        if (ui.acceptsCameraKeys()) {
            float distance = 500 * Gdx.graphics.getDeltaTime();
            for (KeyCommandBind command : cameraKeys.values()) {
                switch (command) {
                    case SCROLL_NORTH -> boardCamera.pan(0, distance);
                    case SCROLL_SOUTH -> boardCamera.pan(0, -distance);
                    case SCROLL_EAST -> boardCamera.pan(-distance, 0);
                    case SCROLL_WEST -> boardCamera.pan(distance, 0);
                    default -> { }
                }
            }
        }
        if (hoverCameraRevision != boardCamera.revision()) {
            hoverCameraRevision = boardCamera.revision();
            boardInput.mouseMoved(Gdx.input.getX(), Gdx.input.getY());
        }
        source.setVisibleArea(boardCamera.visibleArea(scene));
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
        prepareUnits();
        terrain.renderShadows(new ArrayList<>(unitInstances.values()));
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        HdpiUtils.glViewport(0, ui.bottomPixels(),
              (int) boardCamera.camera.viewportWidth, (int) boardCamera.camera.viewportHeight);
        terrain.render(boardCamera.camera, false);
        terrain.render(boardCamera.camera, true);
        renderHexText();
        renderUnits();
        renderHints();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        renderAnnotations();
        ui.draw();
        frames++;
    }

    private void prepareUnits() {
        unitAnchors.clear();
        unitInstances.keySet().retainAll(scene.units().stream().map(unit -> unit.id() + ":" + unit.part())
              .collect(Collectors.toSet()));
        for (BoardScene.Unit unit : scene.units()) {
            Vector3 position = BoardGeometry.center(unit.location().coords(), unit.location().elevation());
            float facing = unit.location().facing() * 60;
            UnitMotion motion = motions.get(unit.id());
            if (motion != null && motion.isMoving()) {
                position.sub(motion.destination()).add(motion.position());
                facing = motion.facing();
            }
            GpuMeeple meeple = meeples.computeIfAbsent(unit.image(), pixels ->
                  new GpuMeeple(pixels, unitTextures.region(pixels)));
            String key = unit.id() + ":" + unit.part();
            ModelInstance instance = unitInstances.get(key);
            if (instance == null || instance.model != meeple.instance.model) {
                instance = new ModelInstance(meeple.instance.model);
                unitInstances.put(key, instance);
            }
            Vector3 anchor = meeple.place(instance, boardCamera.camera, position, facing, unit.height());
            unitAnchors.put(unit, anchor);
        }
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
        BitmapFont font = ui.font();
        float scaleX = font.getData().scaleX;
        float scaleY = font.getData().scaleY;
        Color color = new Color(font.getColor());
        boolean integerPositions = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        annotationBatch.setProjectionMatrix(boardCamera.camera.combined);
        GlyphLayout layout = new GlyphLayout();
        annotationBatch.begin();
        for (var group : scene.tiles().stream().filter(tile -> !tile.text().isEmpty())
              .filter(tile -> boardCamera.camera.frustum.sphereInFrustum(
                    BoardGeometry.center(tile.coords(), tile.elevation()), BoardGeometry.WIDTH))
              .collect(Collectors.groupingBy(BoardScene.Tile::elevation)).entrySet()) {
            annotationBatch.setTransformMatrix(new Matrix4().setToTranslation(0, 0, group.getKey() * BoardGeometry.LEVEL + 0.6f));
            for (BoardScene.Tile tile : group.getValue()) {
                for (BoardView.HexText label : tile.text()) {
                    font.getData().setScale(label.font().getSize2D() / GpuBoardUi.FONT_RESOLUTION);
                    font.setColor(new Color((label.argb() << 8) | ((label.argb() >>> 24) & 0xff)));
                    layout.setText(font, label.text());
                    font.draw(annotationBatch, layout, BoardGeometry.centerX(tile.coords()) - layout.width / 2,
                          BoardGeometry.centerY(tile.coords()) + BoardGeometry.HEIGHT / 2 - label.baseline() + layout.height);
                }
            }
        }
        annotationBatch.end();
        annotationBatch.setTransformMatrix(new Matrix4());
        font.getData().setScale(scaleX, scaleY);
        font.setColor(color);
        font.setUseIntegerPositions(integerPositions);
    }

    static float annotationScale(float displayScale) {
        return 1.4f * Math.max(1, displayScale) / EntitySprite.ANNOTATION_RESOLUTION;
    }

    private void renderHints() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        lines.setProjectionMatrix(boardCamera.camera.combined);
        lines.begin(ShapeRenderer.ShapeType.Line);
        if (ui.allHints()) {
            lines.setColor(Color.valueOf("386B78"));
            for (BoardScene.Tile tile : scene.tiles()) {
                if (boardCamera.camera.frustum.sphereInFrustum(BoardGeometry.center(tile.coords(), tile.elevation()),
                      BoardGeometry.WIDTH)) {
                    ring(tile.coords(), tile.elevation());
                }
            }
        }
        lines.setColor(Color.valueOf("55E1CF"));
        for (BoardScene.Unit unit : scene.units()) {
            if (ui.hintsFor(unit, hovered)) {
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
        for (int edge = 0; edge < 6; edge++) {
            lines.line(BoardGeometry.corner(coords, elevation, edge).add(0, 0, 0.5f),
                  BoardGeometry.corner(coords, elevation, edge + 1).add(0, 0, 0.5f));
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
            return scene == null ? null : BoardGeometry.pick(scene,
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
            if (key == Input.Keys.ENTER && hovered != null) {
                Vector3 point = screenPosition(hovered);
                ui.inspect(hovered, (int) point.x, (int) point.y);
                return true;
            }
            int awt = awtKey(key);
            for (KeyCommandBind command : KeyCommandBind.getBindByKey(awt, modifiers())) {
                switch (command) {
                    case SCROLL_NORTH, SCROLL_SOUTH, SCROLL_EAST, SCROLL_WEST -> {
                        cameraKeys.put(key, command);
                        return true;
                    }
                    case ZOOM_IN -> {
                        boardCamera.zoom(1 / 1.2f);
                        return true;
                    }
                    case ZOOM_OUT -> {
                        boardCamera.zoom(1.2f);
                        return true;
                    }
                    case ZOOM_OVERVIEW_TOGGLE -> {
                        boardCamera.toggleOverview(scene);
                        return true;
                    }
                    default -> { }
                }
            }
            if (awt != KeyEvent.VK_UNDEFINED) {
                source.key(awt, true, modifiers());
            }
            return awt != KeyEvent.VK_UNDEFINED;
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
        if (ui != null) {
            ui.releaseInput();
        }
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
        if (ui != null) {
            ui.dispose();
        }
        source.stopKeys();
        meeples.values().forEach(GpuMeeple::dispose);
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
        if (lines != null) {
            lines.dispose();
        }
        source.close();
    }
}
