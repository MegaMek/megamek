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

/** Exercises the actual shader and GIF paths, including emissive opaque lava and transparent toxic water. */
@Tag("on-demand")
class GpuLiquidSmokeTest {
    private static final List<BoardLiquid> LIQUIDS = List.of(BoardLiquid.WATER,
          new BoardLiquid(BoardLiquid.Kind.HAZARDOUS, "", 0), new BoardLiquid(BoardLiquid.Kind.MAGMA, "", 0),
          new BoardLiquid(BoardLiquid.Kind.WATER, "", 1), new BoardLiquid(BoardLiquid.Kind.WATER, "", 2),
          new BoardLiquid(BoardLiquid.Kind.HAZARDOUS, "", 2), new BoardLiquid(BoardLiquid.Kind.WATER, "mars", 0),
          new BoardLiquid(BoardLiquid.Kind.WATER, "volcano", 0), new BoardLiquid(BoardLiquid.Kind.HAZARDOUS, "", 0));

    @Test
    void bothAnimationPathsRenderEveryLiquidAndRespectLightingAndLiveEdits() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkInterpolation();
                    for (boolean shader : new boolean[] { true, false }) { checkRendering(shader, false); }
                    checkRendering(true, true);
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Liquid rendering", failure.get()); }
    }

    /** Pixel changes alone also accept texture panning. Stationary pools must match authored frames spatially. */
    private static void checkInterpolation() {
        BoardScene original = scene();
        List<BoardScene.Tile> flat = original.tiles().stream()
              .map(tile -> tile(tile.coords(), 0, tile.liquid(), tile.ground(), tile.frozen())).toList();
        BoardScene scene = new BoardScene(0, original.width(), original.height(), flat, List.of(), List.of(), -1, "", List.of());
        GpuTerrain gif = new GpuTerrain(false), shader = new GpuTerrain(true);
        try {
            gif.setWaterEffects(false);
            shader.setWaterEffects(false);
            gif.update(scene);
            shader.update(scene);
            BoardCamera camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            camera.setIsometric(false);
            camera.fit(scene);
            List<int[]> first = samples(gif, camera, scene, true);
            gif.animate(0.1f, List.of());
            List<int[]> next = samples(gif, camera, scene, true);
            shader.animate(0.05f, List.of());
            List<int[]> middle = samples(shader, camera, scene, true);
            for (int family = 0; family < LIQUIDS.size(); family++) {
                for (int pixel = 0; pixel < first.get(family).length; pixel++) {
                    for (int shift : new int[] { 8, 16, 24 }) {
                        float expected = (((first.get(family)[pixel] >>> shift) & 255)
                              + ((next.get(family)[pixel] >>> shift) & 255)) / 2f;
                        assertEquals(expected, (middle.get(family)[pixel] >>> shift) & 255, 2,
                              "Stationary material must interpolate the changing artwork without shifting it");
                    }
                }
            }
            shader.animate(0.05f, List.of());
            List<int[]> boundary = samples(shader, camera, scene, true);
            for (int family = 0; family < LIQUIDS.size(); family++) {
                assertTrue(java.util.Arrays.equals(next.get(family), boundary.get(family)),
                      "Both modes must match at an authored frame boundary, family " + family);
            }
        } finally {
            gif.dispose();
            shader.dispose();
        }
    }

    private static void checkRendering(boolean shader, boolean procedural) throws Exception {
        BoardScene scene = scene();
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        GpuTerrain terrain = new GpuTerrain(shader, procedural);
        try {
            terrain.update(scene);
            BoardCamera camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            for (boolean isometric : new boolean[] { false, true }) {
                camera.setIsometric(isometric);
                camera.fit(scene);
                terrain.renderShadows(camera.camera, List.of());
                List<int[]> before = samples(terrain, camera, scene, true);
                GpuBoardTestUi.capture(new File(output, "liquids-" + (procedural ? "procedural" : shader ? "shader" : "gif")
                      + "-" + (isometric ? "isometric" : "top") + ".png"));
                terrain.animate(0.45f, List.of());
                List<int[]> after = samples(terrain, camera, scene, true);
                for (int family = 0; family < LIQUIDS.size() - 1; family++) {
                    int changed = 0;
                    for (int pixel = 0; pixel < before.get(family).length; pixel++) {
                        if (before.get(family)[pixel] != after.get(family)[pixel]) { changed++; }
                    }
                    assertTrue(changed > 15, "Animated liquid " + family + ", shader=" + shader + ": " + changed);
                }
                assertTrue(java.util.Arrays.equals(before.get(8), after.get(8)), "Frozen hazardous water stays solid");
                assertGreen(after.get(1));
                assertGreen(after.get(5));
                if (isometric) { assertWaterfallPalette(camera, scene, after); }
                int[] opaqueLava = samples(terrain, camera, scene, false).get(2);
                assertTrue(java.util.Arrays.equals(opaqueLava, after.get(2)), "Magma must render in the opaque depth pass");
            }

            // Equal game depth/elevation but a different liquid must rebuild the chunk's material and its tint.
            Coords changed = pool(1);
            var tiles = new ArrayList<>(scene.tiles());
            BoardScene.Tile old = scene.tile(changed);
            tiles.set(changed.getX() * scene.height() + changed.getY(), tile(changed, old.elevation(), BoardLiquid.WATER,
                  old.ground(), false));
            BoardScene edited = new BoardScene(0, scene.width(), scene.height(), tiles, List.of(), List.of(), -1, "", List.of());
            camera.setIsometric(false);
            camera.fit(scene);
            int[] toxic = samples(terrain, camera, scene, true).get(1);
            terrain.update(edited);
            int[] fresh = samples(terrain, camera, edited, true).get(1);
            assertTrue(channel(toxic, 16) - channel(toxic, 8) > channel(fresh, 16) - channel(fresh, 8) + 15,
                  "Live removal of hazardous liquid must remove the green tint");

            terrain.setAtmosphere(new BoardAtmosphere.Lighting(new Vector3(0, 0, -1), Color.BLACK, Color.BLACK,
                  Color.BLACK, Color.BLACK, Color.BLACK, Color.WHITE, 1, 0, false));
            List<int[]> dark = samples(terrain, camera, edited, true);
            assertTrue(channel(dark.get(2), 24) > 40, "Lava's emission remains visible without ambient or direct light");
            assertTrue(channel(dark.get(0), 24) < 5, "Water must not emit light");
        } finally {
            terrain.dispose();
        }
    }

    private static void assertGreen(int[] pixels) {
        assertTrue(channel(pixels, 16) > channel(pixels, 24) + 10 && channel(pixels, 16) > channel(pixels, 8) + 10,
              "Hazardous liquid should be green: " + channel(pixels, 24) + "," + channel(pixels, 16) + "," + channel(pixels, 8));
    }

    /** The vertical sheet must retain the upstream pool's hue, including the color seen through its transparent surface. */
    private static void assertWaterfallPalette(BoardCamera camera, BoardScene scene, List<int[]> pools) {
        Pixmap image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            for (int family = 0; family < LIQUIDS.size() - 1; family++) {
                if (LIQUIDS.get(family).molten()) { continue; }
                BoardSurface.Side drop = new BoardSurface(scene, scene.tile(pool(family))).waterfalls.getFirst();
                int[] pixels = new int[81];
                for (int y = 0; y < 9; y++) {
                    for (int x = 0; x < 9; x++) {
                        Vector3 point = new Vector3(drop.a()).lerp(drop.b(), 0.2f + x * 0.075f);
                        point.z = drop.lowA() + (drop.a().z - drop.lowA()) * (0.2f + y * 0.075f);
                        Vector3 screen = camera.camera.project(point);
                        pixels[y * 9 + x] = image.getPixel(Math.round(screen.x), Math.round(screen.y));
                    }
                }
                float poolSum = channel(pools.get(family), 24) + channel(pools.get(family), 16) + channel(pools.get(family), 8);
                float fallSum = channel(pixels, 24) + channel(pixels, 16) + channel(pixels, 8);
                for (int shift : new int[] { 8, 16, 24 }) {
                    assertEquals(channel(pools.get(family), shift) / poolSum, channel(pixels, shift) / fallSum, 0.05,
                          "Waterfall hue must follow the upstream pool, family " + family + ", channel " + shift);
                }
            }
        } finally { image.dispose(); }
    }

    private static int channel(int[] pixels, int shift) {
        int sum = 0;
        for (int pixel : pixels) { sum += (pixel >>> shift) & 255; }
        return sum / pixels.length;
    }

    private static List<int[]> samples(GpuTerrain terrain, BoardCamera camera, BoardScene scene, boolean transparent) {
        ScreenUtils.clear(0.03f, 0.045f, 0.06f, 1, true);
        terrain.render(camera.camera, false);
        if (transparent) { terrain.renderTransparent(camera.camera); }
        Pixmap image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            List<int[]> result = new ArrayList<>();
            for (int family = 0; family < LIQUIDS.size(); family++) {
                Coords coords = pool(family);
                Vector3 center = BoardGeometry.center(coords, scene.tile(coords).elevation());
                center.z = BoardGeometry.surfaceZ(scene.tile(coords));
                int[] pixels = new int[49];
                for (int y = -3; y <= 3; y++) {
                    for (int x = -3; x <= 3; x++) {
                        Vector3 screen = camera.camera.project(center.cpy().add(x * 2, y * 2, 0));
                        pixels[(y + 3) * 7 + x + 3] = image.getPixel(
                              Math.round(screen.x * image.getWidth() / Gdx.graphics.getWidth()),
                              Math.round(screen.y * image.getHeight() / Gdx.graphics.getHeight()));
                    }
                }
                result.add(pixels);
            }
            return result;
        } finally {
            image.dispose();
        }
    }

    private static Coords pool(int family) { return new Coords(1 + (family % 3) * 3, 1 + (family / 3) * 3); }

    private static BoardScene scene() {
        BufferedImage gray = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) { gray.setRGB(x, y, 0xff888888); }
        }
        BoardScene.Pixels ground = new BoardScene.Pixels(gray);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 10; y++) {
                Coords coords = new Coords(x, y);
                BoardLiquid liquid = BoardLiquid.NONE;
                int elevation = 0;
                boolean frozen = false;
                for (int family = 0; family < LIQUIDS.size(); family++) {
                    Coords top = pool(family);
                    if (coords.equals(top) || coords.equals(top.translated(3))) {
                        liquid = LIQUIDS.get(family);
                        elevation = coords.equals(top) ? 2 : 0;
                        frozen = family == 8;
                    }
                }
                tiles.add(tile(coords, elevation, liquid, ground, frozen));
            }
        }
        return new BoardScene(0, 9, 10, tiles, List.of(), List.of(), -1, "", List.of());
    }

    private static BoardScene.Tile tile(Coords coords, int elevation, BoardLiquid liquid, BoardScene.Pixels ground, boolean frozen) {
        return new BoardScene.Tile(coords, elevation, liquid.present() && !liquid.molten() ? 2 : -1, frozen, 0,
              BoardScene.Surface.ROCK, ground, null, null, null, null, List.of(), List.of(), liquid);
    }
}
