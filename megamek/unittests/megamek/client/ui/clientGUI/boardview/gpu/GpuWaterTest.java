/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Configuration;
import org.junit.jupiter.api.Test;

class GpuWaterTest {
    @Test
    void interpolationRespectsUnequalFrameDelaysAndWrapsToTheFirstFrame() {
        var animation = new GpuAssets.Animation<>(List.of("a", "b", "c"), new float[] { 0.1f, 0.35f, 0.4f }, 0.4f);
        assertEquals("b", animation.at(0.225f));
        assertEquals(0.5f, animation.blend(0.225f, animation.index(0.225f)), 0.0001f);
        assertEquals("c", animation.at(0.375f));
        assertEquals(0.5f, animation.blend(0.375f, animation.index(0.375f)), 0.0001f);
        assertEquals("a", animation.at(0.425f));
        assertEquals(0.25f, animation.blend(0.425f, animation.index(0.425f)), 0.0001f);
    }

    @Test
    void portedLiquidAnimationsHaveCompleteFramesAndFoamRetainsItsMaskAndTiming() {
        List<String> paths = new ArrayList<>();
        for (int level = -3; level <= 10; level++) { paths.add("saxarba/base/base_magma_anim_" + level + ".gif"); }
        for (String theme : List.of("mars", "volcano")) {
            for (int depth = 0; depth <= 4; depth++) {
                paths.add("saxarba/theme_" + theme + "/water_anim_" + theme + "_" + depth + ".gif");
            }
        }
        for (String path : paths) {
            File file = new File(Configuration.dataDir(), "models/board/tileset/" + path);
            var animation = GpuAssets.readWater(file);
            assertEquals(32, animation.frames().size(), path);
            assertEquals(3.2f, animation.duration(), 0.001f, path);
            assertTrue(animation.frames().stream().distinct().count() > 1, path);
            assertEquals(animation.frames().getFirst(), animation.at(animation.duration()), "Animation loops at its duration");
            for (var frame : animation.frames()) {
                for (int pixel = 0; pixel < frame.width() * frame.height(); pixel++) {
                    assertEquals(255, frame.rgba(pixel) & 255, "A liquid's mesh defines its boundary: " + path);
                }
            }
        }
        for (String flow : List.of("rapids", "torrent")) {
            var animation = GpuAssets.readAnimation(new File(Configuration.dataDir(),
                  "models/board/tileset/saxarba/water/" + flow + "_anim.gif"), false);
            assertEquals(32, animation.frames().size());
            assertEquals(3.2f, animation.duration(), 0.001f);
            var first = animation.frames().getFirst();
            int clear = 0, painted = 0;
            for (int pixel = 0; pixel < first.width() * first.height(); pixel++) {
                if ((first.rgba(pixel) & 255) == 0) { clear++; } else { painted++; }
            }
            assertTrue(clear > 0 && painted > 0, "Foam must show animated water through its gaps");
        }
    }

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
