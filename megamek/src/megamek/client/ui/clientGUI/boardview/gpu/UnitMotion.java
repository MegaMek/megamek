/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import megamek.common.units.EntityMovementType;

/** A render-only timeline; advancing it never writes to Entity or MovePath. */
final class UnitMotion {
    static final double WALK_SECONDS = 1.4;
    static final double RUN_SECONDS = 1.1;
    static final double JUMP_SECONDS = 1.2;
    static final float JUMP_DESTINATION_CLEARANCE = 3;
    private record JumpArc(float start, float end, float apex, float ascentEnd, float descentStart) {
        float elevation(float progress) {
            if (progress < ascentEnd) {
                float remaining = 1 - progress / ascentEnd;
                return apex - (apex - start) * remaining * remaining;
            }
            if (progress > descentStart) {
                float remaining = (progress - descentStart) / (1 - descentStart);
                return apex - (apex - end) * remaining * remaining;
            }
            return apex;
        }
    }
    private record Playback(List<BoardScene.Waypoint> path, double duration, JumpArc jump) { }
    private final ArrayDeque<Playback> remaining = new ArrayDeque<>();
    private final Vector3 position = new Vector3();
    private float facing;
    private double elapsed;

    public UnitMotion(BoardScene.Waypoint initial) {
        snap(initial);
    }

    public void snap(BoardScene.Waypoint location) {
        remaining.clear();
        position.set(BoardGeometry.center(location.coords(), location.elevation()));
        facing = location.facing() * 60;
        elapsed = 0;
    }

    public void append(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP) {
        if (path.isEmpty()) {
            return;
        }
        List<BoardScene.Waypoint> points = new ArrayList<>();
        if (isMoving()) {
            points.add(remaining.getLast().path().getLast());
        }
        for (BoardScene.Waypoint point : path) {
            if (points.isEmpty() || !points.getLast().equals(point)) {
                points.add(point);
            }
        }
        if (!isMoving()) {
            snap(points.getFirst());
        }
        if (points.size() < 2) {
            return;
        }
        double duration = switch (type) {
            case MOVE_JUMP -> JUMP_SECONDS;
            case MOVE_RUN, MOVE_SPRINT, MOVE_VTOL_RUN, MOVE_VTOL_SPRINT, MOVE_SUBMARINE_RUN,
                  MOVE_OVER_THRUST -> RUN_SECONDS;
            default -> WALK_SECONDS;
        };
        remaining.add(new Playback(List.copyOf(points), duration,
              type == EntityMovementType.MOVE_JUMP ? jumpArc(points, jumpMP) : null));
    }

    private static JumpArc jumpArc(List<BoardScene.Waypoint> points, int jumpMP) {
        BoardScene.Waypoint start = points.getFirst();
        BoardScene.Waypoint end = points.getLast();
        float ceiling = start.elevation() + Math.max(0, jumpMP);
        float apex = Math.max(Math.max(start.elevation(), end.elevation()),
              Math.min(ceiling, end.elevation() + JUMP_DESTINATION_CLEARANCE));
        for (BoardScene.Waypoint point : points) {
            apex = Math.max(apex, Math.min(ceiling, point.elevation()));
        }
        float rise = (float) Math.sqrt(apex - start.elevation());
        float fall = (float) Math.sqrt(apex - end.elevation());
        float ascentEnd = rise + fall == 0 ? 0 : rise / (rise + fall);
        float descentStart = ascentEnd;
        for (int index = 1; index < points.size() - 1; index++) {
            float progress = index / (float) (points.size() - 1);
            float elevation = Math.min(apex, points.get(index).elevation());
            if (elevation > start.elevation()) {
                float fraction = 1 - (float) Math.sqrt((apex - elevation) / (apex - start.elevation()));
                ascentEnd = Math.min(ascentEnd, progress / fraction);
            }
            if (elevation > end.elevation()) {
                float fraction = (float) Math.sqrt((apex - elevation) / (apex - end.elevation()));
                descentStart = Math.max(descentStart, (progress - fraction) / (1 - fraction));
            }
        }
        return new JumpArc(start.elevation(), end.elevation(), apex, ascentEnd, descentStart);
    }

    public void advance(double seconds, double speed) {
        if (!isMoving()) {
            return;
        }
        if (speed <= 0) {
            finish();
            return;
        }
        elapsed += Math.max(0, seconds) * speed;
        while (isMoving() && elapsed + 1e-9 >= remaining.getFirst().duration()) {
            Playback completed = remaining.removeFirst();
            BoardScene.Waypoint end = completed.path().getLast();
            position.set(BoardGeometry.center(end.coords(), end.elevation()));
            facing = end.facing() * 60;
            elapsed = Math.max(0, elapsed - completed.duration());
        }
        if (!isMoving()) {
            elapsed = 0;
            return;
        }
        Playback playback = remaining.getFirst();
        float progress = (float) (elapsed / playback.duration());
        float step = progress * (playback.path().size() - 1);
        int index = Math.min((int) step, playback.path().size() - 2);
        BoardScene.Waypoint start = playback.jump() != null ? playback.path().getFirst() : playback.path().get(index);
        BoardScene.Waypoint end = playback.jump() != null ? playback.path().getLast() : playback.path().get(index + 1);
        float fraction = playback.jump() != null ? progress : step - index;
        position.set(BoardGeometry.center(start.coords(), start.elevation()))
              .lerp(BoardGeometry.center(end.coords(), end.elevation()), fraction);
        if (playback.jump() != null) {
            position.z = playback.jump().elevation(progress) * BoardGeometry.LEVEL;
        }
        facing = MathUtils.lerpAngleDeg(start.facing() * 60, end.facing() * 60, fraction);
    }

    public void finish() {
        if (isMoving()) {
            snap(remaining.getLast().path().getLast());
        }
    }

    public boolean isMoving() {
        return !remaining.isEmpty();
    }

    public Vector3 position() {
        return position;
    }

    /** Grounded road travel follows the rendered cut/ramp without changing the game path. */
    public Vector3 surfacePosition(BoardScene scene) {
        if (!isMoving() || remaining.getFirst().jump() != null) {
            return position;
        }
        Playback playback = remaining.getFirst();
        int index = Math.min((int) (elapsed / playback.duration() * (playback.path().size() - 1)), playback.path().size() - 2);
        BoardScene.Waypoint from = playback.path().get(index), to = playback.path().get(index + 1);
        BoardScene.Tile start = scene.tile(from.coords()), end = scene.tile(to.coords());
        if (start == null || end == null || start.water() || end.water()
              || from.elevation() != start.elevation() || to.elevation() != end.elevation()) {
            return position;
        }
        for (int direction = 0; direction < 6; direction++) {
            if (from.coords().translated(direction).equals(to.coords())
                  && BoardSurface.hasRoadApproach(start, end, direction)) {
                BoardScene.Tile tile = BoardGeometry.contains(from.coords(), position.x, position.y) ? start : end;
                return new Vector3(position.x, position.y, new BoardSurface(scene, tile).height(position.x, position.y));
            }
        }
        return position;
    }

    public Vector3 destination() {
        BoardScene.Waypoint last = isMoving() ? remaining.getLast().path().getLast() : null;
        return last == null ? new Vector3(position) : BoardGeometry.center(last.coords(), last.elevation());
    }

    public float facing() {
        return facing;
    }
}
