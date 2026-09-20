/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;

/** One orbit camera with top/isometric presets and shared geometry for every orientation. */
final class BoardCamera {
    private static final float ISOMETRIC_TILT = 54.73561f;
    // Manual orthographic zoom limits: smaller values zoom in, larger values zoom out.
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 20f;
    static final float MAX_TILT = 80;
    /** One keyboard turn. Hex rows line up again every sixth of a circle, so each turn lands on a matching view. */
    static final float ROTATION_STEP = 60;
    static final float ROTATION_SECONDS = 0.25f;
    /** Animate these automatic camera changes; false applies the same framing immediately. */
    static final boolean ANIMATE_CAMERA_ON_SELECTION_CHANGE = true;
    static final boolean ANIMATE_CAMERA_COMBAT_PLAYBACK = true;
    static final boolean ANIMATE_CAMERA_ON_MOVE = true;
    /** Maximum wall-clock seconds for automatic framing, independent of playback speed. Zero snaps. */
    static final float CAMERA_FRAMING_SECONDS = .4f;
    /** Degrees from overhead: at or below this tilt, keep visible actions still; otherwise only pan or zoom out. */
    static final float ATTACK_TOP_VIEW_TILT_DEGREES = 30;
    private static final float FRAMING_MARGIN_PIXELS = 64;
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
    private float rotationStart;
    private float rotationSweep;
    private float rotationTarget;
    private float rotationElapsed = ROTATION_SECONDS;
    /** Render-owned endpoints of one camera move; later volley targets keep its original deadline. */
    private record Pose(Vector3 focus, float zoom, float azimuth, float tilt) { }
    private Pose framingStart;
    private Pose framingTarget;
    private float framingElapsed;
    private float framingStartTime;
    private Object framedAction;
    private int framedCount;
    private float framedWidth, framedViewportWidth, framedHeight;
    private int framedGeometry;

    BoardCamera() {
        camera.near = 1;
        camera.far = 100000;
        camera.zoom = 1;
    }

    /** Shared projected size for the orthographic board, including framebuffer/display scaling. */
    static float pixelsPerUnit(OrthographicCamera camera) {
        return Gdx.graphics.getBackBufferHeight() / (float) Math.max(1, Gdx.graphics.getHeight()) / camera.zoom;
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
        stopFraming();
        stopRotation();
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

    float azimuth() {
        return azimuth;
    }

    long revision() {
        return revision;
    }

    void orbit(float rotation, float inclination) {
        stopRotation();
        azimuth = wrapDegrees(azimuth + rotation);
        tilt(inclination);
    }

    /** Changes only the viewing angle, so holding a tilt key does not interrupt a keyboard turn in progress. */
    void tilt(float inclination) {
        stopFraming();
        fitToWindow = false;
        tilt = MathUtils.clamp(tilt + inclination, 0, MAX_TILT);
        update();
    }

    /**
     * Starts an eased keyboard turn of one {@link #ROTATION_STEP}. A turn requested while another is still playing
     * is added to it, so quick taps queue up and an opposite tap turns back.
     *
     * @param direction {@code -1} to turn left, {@code 1} to turn right
     */
    void rotateStep(int direction) {
        stopFraming();
        fitToWindow = false;
        float remaining = isRotating() ? rotationSweep * (1 - rotationProgress()) : 0;
        // The target is tracked apart from the eased path so that whole steps from a preset land exactly on it again.
        rotationTarget = wrapDegrees((isRotating() ? rotationTarget : azimuth) + direction * ROTATION_STEP);
        rotationStart = azimuth;
        rotationSweep = remaining + direction * ROTATION_STEP;
        rotationElapsed = 0;
    }

    boolean isRotating() {
        return rotationElapsed < ROTATION_SECONDS;
    }

    /** Advances a camera transition in wall-clock time, independently of the combat playback speed. */
    void advance(float seconds) {
        if (framingTarget != null) {
            framingElapsed = Math.min(framingElapsed + Math.max(0, seconds), CAMERA_FRAMING_SECONDS);
            float remaining = CAMERA_FRAMING_SECONDS - framingStartTime;
            float progress = remaining <= 0 ? 1 : Interpolation.smooth.apply((framingElapsed - framingStartTime) / remaining);
            focus.set(framingStart.focus()).lerp(framingTarget.focus(), progress);
            camera.zoom = MathUtils.lerp(framingStart.zoom(), framingTarget.zoom(), progress);
            azimuth = MathUtils.lerpAngleDeg(framingStart.azimuth(), framingTarget.azimuth(), progress);
            tilt = MathUtils.lerp(framingStart.tilt(), framingTarget.tilt(), progress);
            if (framingElapsed >= CAMERA_FRAMING_SECONDS) {
                framingStart = null;
                framingTarget = null;
            }
            update();
            return;
        }
        if (!isRotating()) {
            return;
        }
        rotationElapsed = Math.min(rotationElapsed + seconds, ROTATION_SECONDS);
        azimuth = isRotating() ? wrapDegrees(rotationStart + rotationSweep * rotationProgress()) : rotationTarget;
        update();
    }

    private float rotationProgress() {
        return Interpolation.smooth.apply(rotationElapsed / ROTATION_SECONDS);
    }

    private void stopRotation() {
        rotationElapsed = ROTATION_SECONDS;
    }

    boolean isFraming() {
        return framingTarget != null;
    }

    private void stopFraming() {
        framingStart = null;
        framingTarget = null;
        framingElapsed = CAMERA_FRAMING_SECONDS;
    }

    void clearPlaybackFrame() {
        if (framedAction != null) {
            stopFraming();
            framedAction = null;
        }
    }

    /** A render-owned action identity prevents repeated frames and late packets from restarting the deadline. */
    private boolean beginFrame(Object action, int count, float width) {
        boolean changedAction = framedAction != action;
        if (!changedAction && framedCount == count && framedWidth == width
              && framedViewportWidth == camera.viewportWidth && framedHeight == camera.viewportHeight
              && framedGeometry == BoardGeometry.revision()) {
            return false;
        }
        if (changedAction) {
            stopFraming();
            framingElapsed = 0;
        }
        framedAction = action;
        framedCount = count;
        framedWidth = width;
        framedViewportWidth = camera.viewportWidth;
        framedHeight = camera.viewportHeight;
        framedGeometry = BoardGeometry.revision();
        return true;
    }

    /** Fit authorized volley participants into the board area to the left of any open side panel. */
    void frameAttacks(List<UnitAttack> attacks, float availableWidth) {
        if (attacks.isEmpty()) {
            clearPlaybackFrame();
            return;
        }
        float width = MathUtils.clamp(availableWidth, 1, camera.viewportWidth);
        if (!beginFrame(attacks.getFirst(), attacks.size(), width)) { return; }
        List<Vector3> points = new ArrayList<>();
        Vector3 axis = new Vector3();
        for (var attack : attacks) {
            var event = attack.event;
            addUnit(points, event.attacker());
            if (event.target() != null) {
                addUnit(points, event.target());
            } else {
                addHex(points, event.destination().coords(), event.destination().elevation() - .5f,
                      event.destination().elevation() + .5f);
            }
            Vector3 direction = BoardGeometry.center(event.destination().coords(), 0)
                  .sub(BoardGeometry.center(event.attacker().location().coords(), 0));
            if (direction.len2() > axis.len2()) { axis.set(direction); }
        }
        // Show the longest shot across the usable area's long axis, choosing the nearer of the two sides.
        float bearing = azimuth;
        boolean topView = tilt <= ATTACK_TOP_VIEW_TILT_DEGREES;
        if (!topView && !axis.isZero(.001f)) {
            bearing = MathUtils.atan2(axis.y, axis.x) * MathUtils.radiansToDegrees;
            if (width < camera.viewportHeight) { bearing -= 90; }
            float turn = (wrapDegrees(bearing - azimuth) + 90) % 180 - 90;
            bearing = wrapDegrees(azimuth + turn);
        }
        float inclination = topView ? tilt : Math.min(tilt, ISOMETRIC_TILT);
        animateTo(fittedPose(points, width, bearing, inclination, topView ? camera.zoom : .5f / displayScale, !topView),
              ANIMATE_CAMERA_COMBAT_PLAYBACK);
    }

    /** Keep the chosen viewing angle and zoom, widening only when the selected unit cannot fit. */
    void frameSelection(BoardScene.Unit unit, float availableWidth) {
        clearPlaybackFrame();
        framingElapsed = 0;
        List<Vector3> points = new ArrayList<>();
        addUnit(points, unit);
        animateTo(fittedPose(points, availableWidth, azimuth, tilt, camera.zoom, tilt > ATTACK_TOP_VIEW_TILT_DEGREES),
              ANIMATE_CAMERA_ON_SELECTION_CHANGE);
    }

    /** Fit the complete rendered route once, with the smallest pan and no unnecessary zoom or rotation. */
    void frameMovement(BoardScene.Movement move, UnitMotion motion, BoardScene scene, float availableWidth) {
        float width = MathUtils.clamp(availableWidth, 1, camera.viewportWidth);
        if (!beginFrame(move, move.path().size(), width)) { return; }
        var unit = move.unit() != null ? move.unit() : scene.units().stream()
              .filter(candidate -> candidate.id() == move.entityId()).findFirst().orElse(null);
        List<Vector3> points = new ArrayList<>();
        if (unit != null && motion != null && motion.isMoving()) {
            for (var pose : motion.framingPath(scene, unit)) {
                for (var occupied : unit.footprint()) {
                    for (int corner = 0; corner < 6; corner++) {
                        var base = pose.outlinePoint(occupied, corner, 0);
                        points.add(base.cpy().add(0, 0, -.25f * BoardGeometry.LEVEL));
                        points.add(base.add(0, 0, Math.max(1, unit.height() + 1) * BoardGeometry.LEVEL));
                    }
                }
            }
        }
        if (points.isEmpty()) {
            for (var point : move.path()) {
                addHex(points, point.coords(), point.elevation() - .25f, point.elevation() + 3);
            }
        }
        if (!points.isEmpty()) {
            animateTo(fittedPose(points, width, azimuth, tilt, camera.zoom, false), ANIMATE_CAMERA_ON_MOVE);
        }
    }

    private Pose fittedPose(List<Vector3> points, float availableWidth, float bearing, float inclination,
          float minimumZoom, boolean centered) {
        float width = MathUtils.clamp(availableWidth, 1, camera.viewportWidth);
        Vector3 outward = new Vector3(), up = new Vector3();
        orientation(bearing, inclination, outward, up);
        Vector3 right = new Vector3(up).crs(outward).nor();
        Vector3 origin = focus;
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (Vector3 point : points) {
            Vector3 relative = new Vector3(point).sub(origin);
            float x = relative.dot(right), y = relative.dot(up);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if (!centered && minX >= -camera.viewportWidth * camera.zoom / 2
              && maxX <= (width - camera.viewportWidth / 2) * camera.zoom
              && minY >= -camera.viewportHeight * camera.zoom / 2 && maxY <= camera.viewportHeight * camera.zoom / 2) {
            return new Pose(focus.cpy(), camera.zoom, azimuth, tilt);
        }
        float margin = Math.min(FRAMING_MARGIN_PIXELS * displayScale, Math.min(width, camera.viewportHeight) * .2f);
        float zoom = Math.max(minimumZoom, Math.max((maxX - minX) / (width - 2 * margin),
              (maxY - minY) / (camera.viewportHeight - 2 * margin)));
        // The projection still fills the full viewport; place the action at the unobstructed area's center.
        float x = (minX + maxX) / 2 + (camera.viewportWidth - width) * zoom / 2;
        float y = (minY + maxY) / 2;
        if (!centered) {
            // Clamp the current pivot to the interval that fits the route, rather than centering it.
            x = MathUtils.clamp(0, maxX - (width - margin - camera.viewportWidth / 2) * zoom,
                  minX + (camera.viewportWidth / 2 - margin) * zoom);
            y = MathUtils.clamp(0, maxY - (camera.viewportHeight / 2 - margin) * zoom,
                  minY + (camera.viewportHeight / 2 - margin) * zoom);
        }
        return new Pose(new Vector3(origin).mulAdd(right, x).mulAdd(up, y), zoom, bearing, inclination);
    }

    private void animateTo(Pose target, boolean animate) {
        if (focus.epsilonEquals(target.focus(), .001f) && MathUtils.isEqual(camera.zoom, target.zoom())
              && MathUtils.isEqual(azimuth, target.azimuth()) && MathUtils.isEqual(tilt, target.tilt())) {
            stopFraming();
            return;
        }
        stopRotation();
        fitToWindow = false;
        framingStart = new Pose(new Vector3(focus), camera.zoom, azimuth, tilt);
        framingTarget = target;
        framingStartTime = framingElapsed;
        if (!animate) { framingElapsed = CAMERA_FRAMING_SECONDS; }
        if (framingElapsed >= CAMERA_FRAMING_SECONDS) { advance(0); }
    }

    private static void addUnit(List<Vector3> points, BoardScene.Unit unit) {
        var footprint = unit.footprint().isEmpty() ? List.of(unit.location().coords()) : unit.footprint();
        for (var coords : footprint) {
            addHex(points, coords, unit.location().elevation() - .25f,
                  unit.location().elevation() + Math.max(1, unit.height() + 1));
        }
    }

    private static void addHex(List<Vector3> points, Coords coords, float bottom, float top) {
        for (int corner = 0; corner < 6; corner++) {
            points.add(BoardGeometry.corner(coords, bottom, corner));
            points.add(BoardGeometry.corner(coords, top, corner));
        }
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360;
        return wrapped < 0 ? wrapped + 360 : wrapped;
    }

    void reset(BoardScene scene) {
        overviewFocus = null;
        setIsometric(true);
        fit(scene);
    }

    void zoom(float factor) {
        stopFraming();
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
        stopFraming();
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
        stopFraming();
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
        stopFraming();
        fitToWindow = false;
        focus.set(position);
        update();
    }

    void toggleOverview(BoardScene scene) {
        stopFraming();
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
        orientation(azimuth, tilt, camera.position, camera.up);
        camera.position.scl(10000).add(focus);
        camera.lookAt(focus);
        camera.update();
        revision++;
    }

    private static void orientation(float azimuth, float tilt, Vector3 outward, Vector3 up) {
        float sin = MathUtils.sinDeg(azimuth);
        float cos = MathUtils.cosDeg(azimuth);
        float horizontal = MathUtils.sinDeg(tilt);
        float vertical = MathUtils.cosDeg(tilt);
        outward.set(sin * horizontal, -cos * horizontal, vertical);
        // An explicit up vector also defines a stable bearing at the directly overhead pole.
        up.set(-sin * vertical, cos * vertical, horizontal);
    }
}
