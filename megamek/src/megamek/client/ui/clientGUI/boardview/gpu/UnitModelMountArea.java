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
            Fit best = null;
            for (int dx = -reachX; dx <= reachX; dx++) {
                for (int dz = -reachZ; dz <= reachZ; dz++) {
                    float squared = dx * dx + dz * dz;
                    float px = x + dx * STEP;
                    float pz = z + dz * STEP;
                    if (squared < distance && free(px, pz, w, h)) {
                        distance = squared;
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
