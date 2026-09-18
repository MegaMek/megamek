/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.Configuration;
import org.junit.jupiter.api.Test;

class GpuWaterTest {
    @Test
    void optimizedGifPatchesBecomeCompleteWaterFramesWithoutTransparentHoles() {
        for (int depth = 0; depth <= 4; depth++) {
            var water = GpuAssets.readWater(new File(Configuration.dataDir(), "models/board/tileset/saxarba/anim_water_" + depth + ".gif"));
            assertTrue(water.frames().size() > 1);
            assertEquals(water.frames().size(), water.ends().length);
            for (var frame : water.frames()) {
                assertEquals(84, frame.width(), "Offset GIF patches must retain the logical canvas width");
                assertEquals(72, frame.height());
                for (int y = 0; y < frame.height(); y++) {
                    for (int x = 0; x < frame.width(); x++) {
                        assertEquals(255, frame.rgba(y * frame.width() + x) & 255,
                              "Water opacity must stay uniform through scrolling, including the original GIF's hex corners");
                    }
                }
            }
            assertEquals(water.duration(), water.ends()[water.ends().length - 1]);
        }
    }
}
