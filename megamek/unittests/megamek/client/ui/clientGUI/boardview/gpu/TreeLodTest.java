/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TreeLodTest {
    @Test
    void retainsCloseDetailAndCanSkipLevelsWhenOverviewToggles() {
        assertEquals(0, TreeLod.level(360, 2));
        assertEquals(1, TreeLod.level(50, 0));
        assertEquals(2, TreeLod.level(10, 0));
        assertEquals(2, TreeLod.level(0, 0));
        assertEquals("birch-snow-lod2", TreeLod.asset("birch-snow", 2));
        assertEquals("birch-snow-lod0", TreeLod.asset("birch-snow", 0));
    }

    @Test
    void smallZoomOscillationsDoNotRebuildMeshesAtEitherBoundary() {
        for (float pixels : new float[] { 74, 80, 86, 80 }) {
            assertEquals(0, TreeLod.level(pixels, 0));
            assertEquals(1, TreeLod.level(pixels, 1));
        }
        for (float pixels : new float[] { 22, 24, 26, 24 }) {
            assertEquals(1, TreeLod.level(pixels, 1));
            assertEquals(2, TreeLod.level(pixels, 2));
        }
        assertEquals(1, TreeLod.level(71, 0));
        assertEquals(0, TreeLod.level(89, 1));
        assertEquals(2, TreeLod.level(21, 1));
        assertEquals(1, TreeLod.level(27, 2));
    }
}
