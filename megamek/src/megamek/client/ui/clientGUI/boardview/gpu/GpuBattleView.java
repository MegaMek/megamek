/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.Messages;
import megamek.client.ui.util.KeyCommandBind;
import megamek.common.board.Coords;

/** GPU board and Scene2D controls. The source remains the sole bridge to the existing client. */
class GpuBattleView extends ApplicationAdapter {
    private static final double[] STEP_SECONDS = { 0.15, 0.30, 0.075, 0 };
    private static final String[] SPEED_LABELS = { "1x", "0.5x", "2x", Messages.getString("GpuBoard.instant") };
    private final GpuBoardSource source;
    private final GpuDisplayScale displayScale = new GpuDisplayScale();
    final BoardCamera boardCamera = new BoardCamera();
    private final Map<Integer, UnitMotion> motions = new HashMap<>();
    private final Map<String, Decal> sprites = new HashMap<>();
    private final Map<Integer, KeyCommandBind> cameraKeys = new HashMap<>();
    private final BoardInput boardInput = new BoardInput();
    private GpuTerrain terrain;
    private GpuTextures<BoardScene.Pixels> unitTextures;
    private CameraGroupStrategy decalStrategy;
    private DecalBatch decals;
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
        decalStrategy = new CameraGroupStrategy(boardCamera.camera,
              (a, b) -> Float.compare(boardCamera.camera.direction.dot(b.getPosition()),
                    boardCamera.camera.direction.dot(a.getPosition())));
        decals = new DecalBatch(decalStrategy);
        lines = new ShapeRenderer();
        ui = new GpuBoardUi(source, boardCamera, () -> speedIndex = (speedIndex + 1) % STEP_SECONDS.length);
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
            sprites.clear();
        }
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
                      .append(movement.path());
            }
        }
        for (UnitMotion motion : motions.values()) {
            motion.advance(Gdx.graphics.getDeltaTime(), STEP_SECONDS[speedIndex]);
        }
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        HdpiUtils.glViewport(0, ui.bottomPixels(),
              (int) boardCamera.camera.viewportWidth, (int) boardCamera.camera.viewportHeight);
        terrain.render(boardCamera.camera, false);
        renderUnits();
        terrain.render(boardCamera.camera, true);
        renderHints();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        ui.draw();
        frames++;
    }

    private void renderUnits() {
        for (BoardScene.Unit unit : scene.units()) {
            Vector3 position = BoardGeometry.center(unit.location().coords(), unit.location().elevation());
            float facing = unit.location().facing() * 60;
            UnitMotion motion = motions.get(unit.id());
            if (motion != null && motion.isMoving()) {
                position.sub(motion.destination()).add(motion.position());
                facing = motion.facing();
            }
            if (!boardCamera.camera.frustum.sphereInFrustum(position, BoardGeometry.WIDTH)) {
                continue;
            }
            TextureRegion region = unitTextures.region(unit.image());
            Decal sprite = sprites.computeIfAbsent(unit.id() + ":" + unit.part(), key ->
                  Decal.newDecal(unit.image().width(), unit.image().height(), region, true));
            sprite.setTextureRegion(region);
            sprite.setDimensions(unit.image().width(), unit.image().height());
            // The artwork lies in the hex plane in both views; the camera supplies the perspective.
            sprite.setRotation(new Quaternion(Vector3.Z, -facing));
            position.add(0, 0, 0.5f);
            sprite.setPosition(position);
            decals.add(sprite);
        }
        decals.flush();
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

    @Override
    public void dispose() {
        if (ui != null) {
            ui.dispose();
        }
        source.stopKeys();
        if (decals != null) {
            decals.dispose();
        }
        if (decalStrategy != null) {
            decalStrategy.dispose();
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
