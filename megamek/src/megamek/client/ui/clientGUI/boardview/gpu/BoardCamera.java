/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Rectangle;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/** One orbit camera with top/isometric presets and shared geometry for every orientation. */
final class BoardCamera {
    private static final float ISOMETRIC_TILT = 54.73561f;
    // Manual orthographic zoom limits: smaller values zoom in, larger values zoom out.
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 20f;
    static final float MAX_TILT = 80;
    final OrthographicCamera camera = new OrthographicCamera();
    final Vector3 focus = new Vector3();
    private float azimuth;
    private float tilt;
    private float overviewZoom;
    private Vector3 overviewFocus;
    private boolean overviewFit;
    private boolean fitToWindow;
    private float displayScale = 1;
    private long revision;

    BoardCamera() {
        camera.near = 1;
        camera.far = 100000;
        camera.zoom = 1;
    }

    void resize(int width, int height) {
        resize(width, height, null, 1);
    }

    void resize(int width, int height, BoardScene scene, float scale) {
        camera.viewportWidth = Math.max(1, width);
        camera.viewportHeight = Math.max(1, height);
        camera.zoom *= displayScale / scale;
        overviewZoom *= displayScale / scale;
        displayScale = scale;
        if (fitToWindow && scene != null) {
            fit(scene);
        }
        update();
    }

    void setIsometric(boolean value) {
        azimuth = value ? 45 : 0;
        tilt = value ? ISOMETRIC_TILT : 0;
        update();
    }

    boolean isIsometric() {
        return MathUtils.isEqual(azimuth, 45) && MathUtils.isEqual(tilt, ISOMETRIC_TILT);
    }

    boolean isTopDown() {
        return tilt == 0;
    }

    float tilt() {
        return tilt;
    }

    long revision() {
        return revision;
    }

    void orbit(float rotation, float inclination) {
        fitToWindow = false;
        azimuth = (azimuth + rotation) % 360;
        if (azimuth < 0) {
            azimuth += 360;
        }
        tilt = MathUtils.clamp(tilt + inclination, 0, MAX_TILT);
        update();
    }

    void reset(BoardScene scene) {
        overviewFocus = null;
        setIsometric(true);
        fit(scene);
    }

    void zoom(float factor) {
        fitToWindow = false;
        camera.zoom = MathUtils.clamp(camera.zoom * factor, MIN_ZOOM, MAX_ZOOM);
        update();
    }

    /** Zoom around the pointer's position on the focus plane. Coordinates are local to the board viewport. */
    void zoomAt(float factor, float x, float y) {
        float before = camera.zoom;
        zoom(factor);
        float difference = before - camera.zoom;
        moveOnBoard((x - camera.viewportWidth / 2) * difference, (y - camera.viewportHeight / 2) * difference);
        update();
    }

    void fit(BoardScene scene) {
        fitToWindow = true;
        focus.set(scene.width() * BoardGeometry.WIDTH * 0.375f,
              -(scene.height() + 0.5f) * BoardGeometry.HEIGHT / 2, 0);
        update();
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float floor = BoardGeometry.floor(scene) / BoardGeometry.LEVEL;
        for (BoardScene.Tile tile : scene.tiles()) {
            float top = tile.elevation();
            for (BoardScene.Feature feature : tile.features()) {
                top = Math.max(top, tile.elevation() + feature.elevation() + feature.height());
            }
            for (int corner = 0; corner < 6; corner++) {
                for (float elevation : new float[] { top, floor }) {
                    Vector3 point = BoardGeometry.corner(tile.coords(), elevation, corner).sub(focus);
                    float x = point.dot(right);
                    float y = point.dot(camera.up);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        focus.mulAdd(right, (minX + maxX) / 2).mulAdd(camera.up, (minY + maxY) / 2);
        camera.zoom = Math.max((maxX - minX) / camera.viewportWidth,
              (maxY - minY) / camera.viewportHeight) * 1.15f;
        update();
    }

    void pan(float dx, float dy) {
        fitToWindow = false;
        moveOnBoard(-dx * camera.zoom, dy * camera.zoom);
        update();
    }

    private void moveOnBoard(float dx, float dy) {
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor();
        Vector3 up = new Vector3(camera.up.x, camera.up.y, 0);
        // Invert the projected length so dragging follows the pointer without lifting the orbit pivot.
        up.scl(1 / up.len2());
        focus.mulAdd(right, dx).mulAdd(up, dy);
    }

    void center(Vector3 position) {
        fitToWindow = false;
        focus.set(position);
        update();
    }

    void toggleOverview(BoardScene scene) {
        if (overviewFocus == null) {
            overviewZoom = camera.zoom;
            overviewFocus = new Vector3(focus);
            overviewFit = fitToWindow;
            fit(scene);
        } else {
            camera.zoom = overviewZoom;
            focus.set(overviewFocus);
            overviewFocus = null;
            fitToWindow = overviewFit;
            if (fitToWindow) {
                fit(scene);
            } else {
                update();
            }
        }
    }

    /** Bounds of the camera's rays at both terrain height extremes, in board hex coordinates. */
    Rectangle visibleArea(BoardScene scene) {
        float low = BoardGeometry.floor(scene);
        float high = scene.tiles().stream().mapToInt(BoardScene.Tile::elevation).max().orElse(0) * BoardGeometry.LEVEL;
        Vector3 right = new Vector3(camera.direction).crs(camera.up).nor();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int x : new int[] { -1, 1 }) {
            for (int y : new int[] { -1, 1 }) {
                Vector3 origin = new Vector3(camera.position)
                      .mulAdd(right, x * camera.viewportWidth * camera.zoom / 2)
                      .mulAdd(camera.up, y * camera.viewportHeight * camera.zoom / 2);
                for (float z : new float[] { low, high }) {
                    Vector3 point = new Vector3(origin).mulAdd(camera.direction, (z - origin.z) / camera.direction.z);
                    minX = Math.min(minX, point.x);
                    maxX = Math.max(maxX, point.x);
                    minY = Math.min(minY, -point.y);
                    maxY = Math.max(maxY, -point.y);
                }
            }
        }
        int left = Math.max(0, (int) Math.floor(minX / (BoardGeometry.WIDTH * 0.75f)) - 2);
        int top = Math.max(0, (int) Math.floor(minY / BoardGeometry.HEIGHT) - 2);
        int rightHex = Math.min(scene.width(), (int) Math.ceil(maxX / (BoardGeometry.WIDTH * 0.75f)) + 2);
        int bottom = Math.min(scene.height(), (int) Math.ceil(maxY / BoardGeometry.HEIGHT) + 2);
        return new Rectangle(left, top, Math.max(0, rightHex - left), Math.max(0, bottom - top));
    }

    void update() {
        float sin = MathUtils.sinDeg(azimuth);
        float cos = MathUtils.cosDeg(azimuth);
        float horizontal = MathUtils.sinDeg(tilt);
        float vertical = MathUtils.cosDeg(tilt);
        camera.position.set(focus).add(10000 * sin * horizontal, -10000 * cos * horizontal, 10000 * vertical);
        // An explicit up vector also defines a stable bearing at the directly overhead pole.
        camera.up.set(-sin * vertical, cos * vertical, horizontal);
        camera.lookAt(focus);
        camera.update();
        revision++;
    }
}
