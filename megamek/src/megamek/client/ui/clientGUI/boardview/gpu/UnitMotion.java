/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayDeque;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/** A render-only timeline; advancing it never writes to Entity or MovePath. */
final class UnitMotion {
    private final ArrayDeque<BoardScene.Waypoint> remaining = new ArrayDeque<>();
    private final Vector3 from = new Vector3();
    private final Vector3 position = new Vector3();
    private float fromFacing;
    private float facing;
    private double elapsed;

    public UnitMotion(BoardScene.Waypoint initial) {
        snap(initial);
    }

    public void snap(BoardScene.Waypoint location) {
        remaining.clear();
        position.set(BoardGeometry.center(location.coords(), location.elevation()));
        from.set(position);
        facing = location.facing() * 60;
        fromFacing = facing;
        elapsed = 0;
    }

    public void append(List<BoardScene.Waypoint> path) {
        if (!isMoving() && !path.isEmpty()) {
            snap(path.getFirst());
            remaining.addAll(path.subList(1, path.size()));
        } else {
            int first = !path.isEmpty() && !remaining.isEmpty() && remaining.getLast().equals(path.getFirst()) ? 1 : 0;
            remaining.addAll(path.subList(first, path.size()));
        }
    }

    public void advance(double seconds, double secondsPerStep) {
        if (!isMoving()) {
            return;
        }
        if (secondsPerStep <= 0) {
            snap(remaining.getLast());
            return;
        }
        elapsed += Math.max(0, seconds);
        while (!remaining.isEmpty() && elapsed >= secondsPerStep) {
            BoardScene.Waypoint next = remaining.removeFirst();
            from.set(BoardGeometry.center(next.coords(), next.elevation()));
            fromFacing = next.facing() * 60;
            elapsed -= secondsPerStep;
        }
        position.set(from);
        facing = fromFacing;
        if (!remaining.isEmpty()) {
            BoardScene.Waypoint next = remaining.getFirst();
            float progress = (float) (elapsed / secondsPerStep);
            position.lerp(BoardGeometry.center(next.coords(), next.elevation()), progress);
            facing = MathUtils.lerpAngleDeg(fromFacing, next.facing() * 60, progress);
        }
    }

    public boolean isMoving() {
        return !remaining.isEmpty();
    }

    public Vector3 position() {
        return position;
    }

    public Vector3 destination() {
        BoardScene.Waypoint last = remaining.peekLast();
        return last == null ? new Vector3(position) : BoardGeometry.center(last.coords(), last.elevation());
    }

    public float facing() {
        return facing;
    }
}
