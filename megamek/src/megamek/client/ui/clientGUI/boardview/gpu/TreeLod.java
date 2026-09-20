/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

/** Screen-pixel budgets for the shared orthographic board, independent of orbit and camera distance. */
final class TreeLod {
    private static final float[] PIXELS = { 80, 24 };
    private static final float HYSTERESIS = 0.1f;

    private TreeLod() { }

    /** A conservative projected bounding diameter, measured in framebuffer pixels. */
    static int level(float pixels, int previous) {
        int level = previous;
        while (level < PIXELS.length && pixels < PIXELS[level] * (1 - HYSTERESIS)) {
            level++;
        }
        while (level > 0 && pixels > PIXELS[level - 1] * (1 + HYSTERESIS)) {
            level--;
        }
        return level;
    }

    static String asset(String name, int level) {
        return level < 0 ? name : name + "-lod" + level;
    }
}
