/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Authored rim artwork and composed normal maps must respond together to the real terrain shader's light. */
@Tag("on-demand")
class GpuRimMaterialSmokeTest {
    @Test
    void rimReliefRespondsToMovingLightAndTheNormalMapToggle() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkRendering();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Rim material rendering", failure.get()); }
    }

    private static void checkRendering() throws Exception {
        Coords raised = new Coords(3, 3);
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        BufferedImage flat = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) {
                image.setRGB(x, y, 0xffa0a0a0);
                flat.setRGB(x, y, 0xff8080ff);
            }
        }
        BoardScene.Pixels pixels = new BoardScene.Pixels(image), normals = new BoardScene.Pixels(flat);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                Coords coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, coords.equals(raised) ? 3 : 0, -1, false, 0,
                      BoardScene.Surface.GRASS, pixels, normals, null, null, List.of(), List.of()));
            }
        }
        BoardScene scene = new BoardScene(0, 7, 7, tiles, List.of(), List.of(), -1, "", List.of());
        GpuTerrain terrain = new GpuTerrain();
        try {
            terrain.update(scene);
            BoardCamera camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            camera.camera.zoom = 0.18f;
            camera.center(BoardGeometry.center(raised, 3));
            List<Vector3> probes = new ArrayList<>();
            for (int edge = 0; edge < 6; edge++) {
                Vector3 a = BoardGeometry.corner(raised, 3, edge), b = BoardGeometry.corner(raised, 3, edge + 1);
                Vector3 along = b.cpy().sub(a).nor(), inward = new Vector3(-along.y, along.x, 0);
                for (int u = 2; u <= 8; u++) {
                    for (int depth = 3; depth <= 21; depth += 2) {
                        probes.add(a.cpy().lerp(b, u / 10f).mulAdd(inward, depth));
                    }
                }
            }
            probes.add(BoardGeometry.center(raised, 3));
            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(output.isDirectory() || output.mkdirs());
            for (boolean isometric : new boolean[] { false, true }) {
                camera.setIsometric(isometric);
                int[][] changes = new int[2][];
                for (int direction = 0; direction < 2; direction++) {
                    Vector3 light = new Vector3(direction == 0 ? -1 : 1, 0, -0.5f).nor();
                    terrain.setAtmosphere(new BoardAtmosphere.Lighting(light, new Color(0.7f, 0.7f, 0.7f, 1),
                          new Color(0.25f, 0.25f, 0.25f, 1), Color.BLACK, Color.BLACK, Color.BLACK, Color.WHITE, 1, 1, true));
                    terrain.renderShadows(camera.camera, List.of());
                    terrain.setNormalMaps(false);
                    int[] without = samples(terrain, camera, probes);
                    GpuBoardTestUi.capture(new File(output, "rim-flat-" + isometric + "-" + direction + ".png"));
                    terrain.setNormalMaps(true);
                    int[] with = samples(terrain, camera, probes);
                    GpuBoardTestUi.capture(new File(output, "rim-normal-" + isometric + "-" + direction + ".png"));
                    changes[direction] = new int[probes.size()];
                    int changed = 0;
                    for (int index = 0; index < probes.size(); index++) {
                        changes[direction][index] = (with[index] >>> 24) - (without[index] >>> 24);
                        if (Math.abs(changes[direction][index]) > 1) { changed++; }
                    }
                    assertTrue(changed > 20, "The normal toggle must affect actual rim pixels: " + changed);
                    assertEquals(without[without.length - 1], with[with.length - 1], "The flat centre keeps its original normal");
                }
                int reversed = 0;
                for (int index = 0; index < probes.size(); index++) {
                    if (changes[0][index] * changes[1][index] < 0) { reversed++; }
                }
                assertTrue(reversed > 20, "Rim relief must change its light response when the light reverses: " + reversed);
            }
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        } finally {
            terrain.dispose();
        }
    }

    private static int[] samples(GpuTerrain terrain, BoardCamera camera, List<Vector3> probes) {
        ScreenUtils.clear(0.03f, 0.045f, 0.06f, 1, true);
        terrain.render(camera.camera, false);
        Pixmap image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            int[] result = new int[probes.size()];
            for (int index = 0; index < probes.size(); index++) {
                Vector3 screen = camera.camera.project(probes.get(index).cpy());
                int x = (int) (screen.x * image.getWidth() / Gdx.graphics.getWidth());
                int y = (int) (screen.y * image.getHeight() / Gdx.graphics.getHeight());
                result[index] = image.getPixel(x, y);
            }
            return result;
        } finally {
            image.dispose();
        }
    }
}
