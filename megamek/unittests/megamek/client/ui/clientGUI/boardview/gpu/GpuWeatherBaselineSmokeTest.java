/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Verify actual weather placement on raised, negative-level and deep-water boards. */
@Tag("on-demand")
class GpuWeatherBaselineSmokeTest {
    @Test
    void waterDepthCannotMoveRainSnowHailOrSandBelowTheBoardSurface() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                GpuWeatherParticles particles = new GpuWeatherParticles();
                try {
                    BoardCamera camera = new BoardCamera();
                    camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    camera.setIsometric(true);
                    for (int level : new int[] { 0, 4, -3 }) {
                        BoardScene dry = scene(level, -1);
                        BoardScene deepWater = scene(level, 20);
                        camera.fit(dry);
                        camera.center(BoardGeometry.center(new Coords(2, 2), level));
                        for (int kind = 0; kind < 4; kind++) {
                            var effects = new BoardAtmosphere.Effects(kind == 0 ? 1 : 0, kind == 1 ? 1 : 0,
                                  kind == 2 ? 1 : 0, kind == 3 ? 1 : 0, 0, 0.6f, 60);
                            Pixmap reference = particleFrame(particles, camera, dry, effects);
                            Pixmap actual = particleFrame(particles, camera, deepWater, effects);
                            try {
                                int visible = 0;
                                for (int y = 0; y < reference.getHeight(); y++) {
                                    for (int x = 0; x < reference.getWidth(); x++) {
                                        if ((reference.getPixel(x, y) & 0xFFFFFF00) != 0) {
                                            visible++;
                                        }
                                    }
                                }
                                assertTrue(visible > 100, "Weather must render above level " + level + ", kind " + kind);
                                assertEquals(reference.getPixels(), actual.getPixels(),
                                      "Changing only water depth must not move weather: level " + level + ", kind " + kind);
                            } finally {
                                reference.dispose();
                                actual.dispose();
                            }
                        }
                    }
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    particles.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private static Pixmap particleFrame(GpuWeatherParticles particles, BoardCamera camera, BoardScene scene,
          BoardAtmosphere.Effects effects) {
        ScreenUtils.clear(0, 0, 0, 1, true);
        particles.render(camera.camera, scene, effects, Color.WHITE, 3);
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private static BoardScene scene(int level, int depth) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), level, depth, false, 0, BoardScene.Surface.GRASS,
                      null, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
