/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;

/** Deterministic packing on one location face. Called only when a live loadout changes, also used by review fixtures. */
final class UnitModelMountArea {
    private static final float GAP = .4f;
    private static final float STEP = .5f;
    private static final float[] FITS = { 1, .85f, .72f, .61f, .52f, .4f };
    private final List<Rectangle> occupied = new ArrayList<>();

    record Fit(float x, float z, float scale) { }

    private record Rectangle(float x, float z, float width, float height) { }

    /** The bounds describe the permitted center movement around an authored socket, in the location's rest plane. */
    Fit place(float x, float z, float width, float height, float areaWidth, float areaHeight, float minimum) {
        return place(x, z, width, height, areaWidth, areaHeight, minimum, -1);
    }

    /**
     * As {@link #place(float, float, float, float, float, float, float)}, with the sideways direction that wins
     * when two free spots are equally near the socket.
     * <p>
     * Without a preference the search takes the lower x on a tie. A face's x runs the same world direction on both
     * sides of a Mek, so that sends the second weapon toward the centre line on one torso and away from it on the
     * other, and the two torsos come out as copies of each other instead of mirrors.
     * </p>
     *
     * @param lateral {@code 1} to prefer higher x on a tie, {@code -1} to prefer lower x
     */
    Fit place(float x, float z, float width, float height, float areaWidth, float areaHeight, float minimum,
          float lateral) {
        for (float fit : FITS) {
            if (fit < minimum || width * fit > areaWidth || height * fit > areaHeight) {
                continue;
            }
            float w = width * fit;
            float h = height * fit;
            if (free(x, z, w, h)) {
                occupied.add(new Rectangle(x, z, w, h));
                return new Fit(x, z, fit);
            }
            int reachX = (int) ((areaWidth - w) / (2 * STEP));
            int reachZ = (int) ((areaHeight - h) / (2 * STEP));
            float distance = Float.POSITIVE_INFINITY;
            int bestDx = 0;
            Fit best = null;
            for (int dx = -reachX; dx <= reachX; dx++) {
                for (int dz = -reachZ; dz <= reachZ; dz++) {
                    float squared = dx * dx + dz * dz;
                    float px = x + dx * STEP;
                    float pz = z + dz * STEP;
                    boolean nearer = squared < distance;
                    boolean preferredTie = squared == distance && dx * lateral > bestDx * lateral;
                    if ((nearer || preferredTie) && free(px, pz, w, h)) {
                        distance = squared;
                        bestDx = dx;
                        best = new Fit(px, pz, fit);
                    }
                }
            }
            if (best != null) {
                occupied.add(new Rectangle(best.x(), best.z(), w, h));
                return best;
            }
        }
        return null;
    }

    private boolean free(float x, float z, float width, float height) {
        return occupied.stream().allMatch(other -> Math.abs(x - other.x()) * 2 >= width + other.width() + GAP * 2
              || Math.abs(z - other.z()) * 2 >= height + other.height() + GAP * 2);
    }
}
