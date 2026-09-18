/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Exercises actual atlas packing/upload boundaries and shadow visibility in a native OpenGL context. */
@Tag("on-demand")
class GpuResourcesSmokeTest {
    @Test
    void packsResizedHudAndCastsHeightDependentShadows() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkPaddingTransitions();
                    checkMeepleShadows();
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    /**
     * Renders a board of one flat color, where only the hex frame may shade a pixel. Every other shaded pixel
     * is a hex edge drawing the surface's filtering against the transparent pixels beyond the tile art, and
     * the frame itself may not shade darker than the shade it repeats the artwork with.
     */
    private void checkHexEdges() {
        BoardScene.Pixels art = hexPixels(java.awt.Color.WHITE);
        GpuTerrain terrain = new GpuTerrain();
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(false);
        try {
            List<BoardScene.Tile> tiles = new ArrayList<>();
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    tiles.add(new BoardScene.Tile(new Coords(x, y), 0, art));
                }
            }
            BoardScene scene = new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of());
            camera.fit(scene);
            camera.zoom(0.5f);
            draw(terrain, camera, scene);
            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(output.isDirectory() || output.mkdirs());
            GpuBoardTestUi.capture(new File(output, "hex-frame.png"));
            Pixmap frame = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(),
                  Gdx.graphics.getBackBufferHeight());
            int shaded = 0;
            int darkest = 255;
            try {
                for (int y = 0; y < frame.getHeight(); y++) {
                    for (int x = 0; x < frame.getWidth(); x++) {
                        int red = frame.getPixel(x, y) >>> 24;
                        if (red > 15 && red < 240) {
                            shaded++;
                            darkest = Math.min(darkest, red);
                        }
                    }
                }
            } finally {
                frame.dispose();
            }
            assertTrue(shaded > 1000, "The hex frame must outline the hexes: " + shaded + " shaded pixels");
            assertTrue(darkest > 160,
                  "Hex edges must stay on the tile artwork and only carry the hex frame: darkest " + darkest);
        } finally {
            terrain.dispose();
        }
    }

    private void checkAtlas() {
        GpuTextures<String> atlas = new GpuTextures<>();
        try {
            for (int[] size : new int[][] { { 2042, 8 }, { 2043, 8 }, { 2048, 8 }, { 8, 2043 },
                  { 3840, 2160 }, { 1280, 800 } }) {
                BoardScene.Pixels pixels = new BoardScene.Pixels(new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB));
                assertTrue(atlas.update(Map.of("hud", pixels)));
                assertEquals(size[0], atlas.region("hud").getRegionWidth());
                assertEquals(size[1], atlas.region("hud").getRegionHeight());
                assertFalse(atlas.update(Map.of("hud", pixels)));
                BoardScene.Pixels changed = new BoardScene.Pixels(new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB));
                assertFalse(atlas.update(Map.of("hud", changed)), "A pixel update must retain the atlas layout");
                assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            }
        } finally {
            atlas.dispose();
        }
    }

    private void checkShadows() throws Exception {
        BufferedImage white = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = white.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels pixels = new BoardScene.Pixels(white);
        GpuTerrain terrain = new GpuTerrain();
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            for (float direction : new float[] { 38, -38 }) {
                BoardScene scene = shadowScene(pixels, 4, new BoardScene.Light(direction, 0));
                camera.fit(scene);
                draw(terrain, camera, scene);
                int left = lowGround(camera, false);
                int right = lowGround(camera, true);
                assertTrue(direction > 0 ? right < left - 20 : left < right - 20,
                      "The raised hex must cast onto the down-light neighbor: left=" + left + ", right=" + right);
                if (direction > 0) {
                    File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                    assertTrue(output.isDirectory() || output.mkdirs());
                    GpuBoardTestUi.capture(new File(output, "hex-shadow-top.png"));
                    camera.setIsometric(true);
                    camera.fit(scene);
                    draw(terrain, camera, scene);
                    GpuBoardTestUi.capture(new File(output, "hex-shadow-isometric.png"));
                    camera.setIsometric(false);
                }
            }
            BoardScene flat = shadowScene(pixels, 0, new BoardScene.Light(38, 0));
            camera.fit(flat);
            draw(terrain, camera, flat);
            assertEquals(lowGround(camera, false), lowGround(camera, true), 3,
                  "Lowering the caster must invalidate its old shadow");
            BoardScene disabled = shadowScene(pixels, 4, null);
            camera.fit(disabled);
            draw(terrain, camera, disabled);
            assertEquals(lowGround(camera, false), lowGround(camera, true), 3,
                  "Disabling terrain shadows must remove the shadow map");
            checkPaddingShadow(terrain, camera);
            // The light offset is an offset in tile artwork pixels, so it has to scale with the hexes. At a
            // doubled scale the sun keeps its angle and the shadow must still land on the same neighbor.
            BoardGeometry.Tuning original = new BoardGeometry.Tuning(BoardGeometry.PADDING, BoardGeometry.HEX_SCALE,
                  BoardGeometry.INTERPOLATION_INSET, BoardGeometry.CORNER_BEVEL, BoardGeometry.BAND_NORMAL_F,
                  BoardGeometry.HEX_FRAME_SHADE);
            try {
                BoardGeometry.tune(new BoardGeometry.Tuning(original.padding(), original.hexScale() * 2,
                      original.interpolationInset(), original.cornerBevel(), original.bandNormalFlatness(),
                      original.unitScale(), original.unitHeightScale(), original.baseLevelHeight(),
                      original.hexFrameShadeFlatness(),
                      original.unitScale(), original.unitHeightScale(), original.baseLevelHeight()));
                for (float direction : new float[] { 38, -38 }) {
                    BoardScene scene = shadowScene(pixels, 4, new BoardScene.Light(direction, 0));
                    camera.fit(scene);
                    draw(terrain, camera, scene);
                    int left = lowGround(camera, false);
                    int right = lowGround(camera, true);
                    assertTrue(direction > 0 ? right < left - 20 : left < right - 20,
                          "A doubled HEX_SCALE must keep the shadow on the down-light neighbor: left=" + left
                                + ", right=" + right);
                }
            } finally {
                BoardGeometry.tune(original);
            }
        } finally {
            terrain.dispose();
        }
    }

    /**
     * A plateau must shade the padding beside it exactly like the ground it continues: the band of the first
     * plain hex sits in the plateau's shadow, the band further out stays lit.
     */
    private void checkPaddingShadow(GpuTerrain terrain, BoardCamera camera) {
        if (!BoardGeometry.HAS_PADDING) {
            return;
        }
        BoardScene.Pixels art = hexPixels(java.awt.Color.WHITE);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x <= 1 ? 3 : 0, art));
            }
        }
        BoardScene scene = new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of(),
              new BoardScene.Light(38, 0));
        camera.fit(scene);
        camera.zoom(0.5f);
        draw(terrain, camera, scene);
        Coords shadowedTile = new Coords(2, 2);
        Coords litTile = new Coords(4, 2);
        int shadowedEdge = edgeToward(shadowedTile, new Coords(1, 2));
        int litEdge = edgeToward(litTile, new Coords(3, 2));
        assertTrue(shadowedEdge >= 0 && litEdge >= 0, "The plain hexes must touch the plateau side");
        int shaded = brightness(camera, BoardGeometry.padPoint(new Vector3(), scene, shadowedTile, 0,
              shadowedEdge, 0.5f, 0.5f));
        int open = brightness(camera, BoardGeometry.padPoint(new Vector3(), scene, litTile, 0, litEdge, 0.5f,
              0.5f));
        assertTrue(shaded < open - 40,
              "The plateau must shade the padding beside it: shaded=" + shaded + ", open=" + open);
    }

    /**
     * Renders two tone tiles side by side so the padding band must blend their ground artwork whatever the
     * level step is. The raised tile's feature artwork is blue while the ground the padding continues is
     * red, so a band that sampled the feature artwork instead of the ground artwork shows a missing red.
     */
    private void checkPaddingTransitions() {
        if (!BoardGeometry.HAS_PADDING) {
            // The bands only exist while the configuration keeps the hexes apart or pulls their interpolation
            // inside their edges, so there is nothing to check in a tight tiling.
            return;
        }
        BoardScene.Pixels black = hexPixels(java.awt.Color.BLACK);
        BoardScene.Pixels ground = hexPixels(java.awt.Color.RED);
        BoardScene.Pixels feature = hexPixels(java.awt.Color.BLUE);
        GpuTerrain terrain = new GpuTerrain();
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(false);
        Coords low = new Coords(2, 2);
        Coords high = new Coords(3, 2);
        try {
            for (int raise : new int[] { 0, 2, 4 }) {
                BoardScene scene = twoToneScene(black, ground, feature, raise);
                camera.fit(scene);
                camera.zoom(0.5f);
                draw(terrain, camera, scene);
                if (raise == 0) {
                    File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                    assertTrue(output.isDirectory() || output.mkdirs());
                    GpuBoardTestUi.capture(new File(output, "padding-flat.png"));
                }
                Vector3 raisedCenter = BoardGeometry.center(high, raise);
                Vector3 raisedPixel = camera.camera.project(new Vector3(raisedCenter), 0, 0,
                      Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                assertEquals(0, brightness(camera, raisedCenter), 25,
                      "The raised tile must render its own feature artwork: rgba=0x"
                            + Integer.toHexString(rgba(camera, raisedCenter)) + " at " + raisedPixel
                            + " buffer " + Gdx.graphics.getBackBufferWidth() + "x"
                            + Gdx.graphics.getBackBufferHeight() + " raise " + raise);
                assertEquals(0, brightness(camera, BoardGeometry.center(low, 0)), 25,
                      "The low tile must still render its own artwork");
                // Sample the band a quarter of the way from the midline back toward the raised hex.
                int edge = -1;
                for (int candidate = 0; candidate < 6; candidate++) {
                    if (high.translated(BoardGeometry.edgeDirection(candidate)).equals(low)) {
                        edge = candidate;
                    }
                }
                assertTrue(edge >= 0, "The raised hex must touch the low one");
                // A slope rises from the midline; a flat protrusion stays at the raised level.
                Vector3 surface = BoardGeometry.padPoint(new Vector3(), scene, high, raise, edge, 0.5f, 0.75f);
                int padding = brightness(camera, surface);
                assertTrue(padding > 70 && padding < 200,
                      "Levels 0 and " + raise + " must blend through the padding: " + padding);
                if (raise == 0) {
                    // A cap tip mirrors past the hexagon, where the artwork has no pixels for it.
                    Vector3 cornerTip = BoardGeometry.cellCorner(high, raise, 0);
                    cornerTip.z = BoardGeometry.cornerHeight(scene, high, raise, 0) * BoardGeometry.LEVEL;
                    Vector3 firstCut = BoardGeometry.padPoint(new Vector3(), scene, high, raise, -1, 1, 1);
                    Vector3 secondCut = BoardGeometry.padPoint(new Vector3(), scene, high, raise, 0, 0, 1);
                    Vector3 corner = new Vector3(firstCut).add(secondCut).scl(0.5f).lerp(cornerTip, 0.5f);
                    int cap = brightness(camera, corner);
                    assertTrue(cap > 180, "The corner cap must sample the painted hexagon: " + cap);
                }
                camera.setIsometric(true);
                camera.fit(scene);
                camera.zoom(0.6f);
                draw(terrain, camera, scene);
                if (raise > 0) {
                    File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                    assertTrue(output.isDirectory() || output.mkdirs());
                    GpuBoardTestUi.capture(new File(output, raise < BoardGeometry.CLIFF_LEVELS
                          ? "padding-slope.png" : "padding-cliff.png"));
                }
                camera.setIsometric(false);
            }
        } finally {
            terrain.dispose();
        }
    }

    /**
     * A water tile pads its water sides with the water artwork and its land sides with the waterless bank
     * artwork, so a lake joins with water and still keeps its banks toward the shore.
     */
    private void checkWaterPadding() {
        if (!BoardGeometry.HAS_PADDING) {
            return;
        }
        BoardScene.Pixels water = hexPixels(java.awt.Color.GREEN);
        BoardScene.Pixels bank = hexPixels(java.awt.Color.RED);
        BoardScene.Pixels land = hexPixels(java.awt.Color.BLUE);
        GpuTerrain terrain = new GpuTerrain();
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(false);
        Coords wet = new Coords(2, 2);
        Coords wetNeighbor = new Coords(3, 2);
        Coords dryNeighbor = new Coords(1, 1);
        try {
            List<BoardScene.Tile> tiles = new ArrayList<>();
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    boolean isWater = (x == 2 || x == 3) && y == 2;
                    tiles.add(new BoardScene.Tile(new Coords(x, y), 0, isWater ? water : land,
                          isWater ? bank : land, isWater));
                }
            }
            BoardScene scene = new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of());
            camera.fit(scene);
            camera.zoom(0.5f);
            draw(terrain, camera, scene);
            int waterEdge = edgeToward(wet, wetNeighbor);
            int bankEdge = edgeToward(wet, dryNeighbor);
            assertTrue(waterEdge >= 0 && bankEdge >= 0, "The water tile must touch water and land");
            Vector3 joint = BoardGeometry.padPoint(new Vector3(), scene, wet, 0, waterEdge, 0.5f, 0.75f);
            int joined = brightness(camera, joint);
            assertTrue(joined < 60, "A water side must continue the water artwork: " + joined);
            Vector3 shore = BoardGeometry.padPoint(new Vector3(), scene, wet, 0, bankEdge, 0.5f, 0.75f);
            int banked = brightness(camera, shore);
            assertTrue(banked > 100, "A land side must continue the bank artwork: " + banked);
        } finally {
            terrain.dispose();
        }
    }

    private static int edgeToward(Coords from, Coords to) {
        for (int edge = 0; edge < 6; edge++) {
            if (from.translated(BoardGeometry.edgeDirection(edge)).equals(to)) {
                return edge;
            }
        }
        return -1;
    }

    /** Artwork shaped like the captured hexes, transparent outside the hexagon. */
    private BoardScene.Pixels hexPixels(java.awt.Color color) {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillPolygon(new int[] { 84, 63, 21, 0, 21, 63 }, new int[] { 36, 0, 0, 36, 72, 72 }, 6);
        graphics.dispose();
        return new BoardScene.Pixels(image);
    }

    /** The raised tile renders blue feature artwork while the ground artwork its padding continues is red. */
    private BoardScene twoToneScene(BoardScene.Pixels black, BoardScene.Pixels ground, BoardScene.Pixels feature,
          int raise) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                boolean raised = x == 3 && y == 2;
                tiles.add(new BoardScene.Tile(new Coords(x, y), raised ? raise : 0, raised ? feature : black,
                      raised ? ground : black, null, List.of()));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of());
    }

    private void checkMeepleShadows() {
        BufferedImage artwork = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = artwork.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(30, 24, 24, 24);
        graphics.dispose();
        BoardScene.Pixels tokenPixels = new BoardScene.Pixels(artwork);
        BufferedImage ground = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        graphics = ground.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        GpuTextures<String> atlas = new GpuTextures<>();
        GpuTerrain terrain = new GpuTerrain();
        ModelBatch batch = new ModelBatch();
        atlas.update(Map.of("token", tokenPixels));
        GpuMeeple meeple = new GpuMeeple(tokenPixels, atlas.region("token"));
        ModelInstance unit = new ModelInstance(meeple.instance.model);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        BoardScene.Pixels floor = new BoardScene.Pixels(ground);
        Vector3 position = BoardGeometry.center(new Coords(2, 2), 0);
        try {
            for (float direction : new float[] { 38, -38 }) {
                BoardScene scene = shadowScene(floor, 0, new BoardScene.Light(direction, 0));
                terrain.update(scene);
                camera.setIsometric(false);
                camera.fit(scene);
                meeple.place(unit, camera.camera, position, 0, 2);
                BoundingBox bounds = unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform);
                assertEquals(2 * BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE, bounds.getDepth(), 0.001f);
                assertEquals(0.5f, bounds.min.z, 0.001f);
                assertEquals(24 * BoardGeometry.UNIT_SCALE, bounds.getWidth(), 0.01f);
                assertEquals(24 * BoardGeometry.UNIT_SCALE, bounds.getHeight(), 0.01f,
                      "Token size must follow its artwork, not the hex scale");
                terrain.renderShadows(List.of(unit));
                drawMeeple(terrain, camera, batch, unit);
                int left = brightness(camera, -35);
                int right = brightness(camera, 35);
                assertTrue(direction > 0 ? right < left - 20 : left < right - 20,
                      "Meeple shadow must follow light: left=" + left + ", right=" + right);
            }
            BoardGeometry.Tuning original = unitScaleTuning(BoardGeometry.UNIT_SCALE);
            try {
                BoardGeometry.tune(unitScaleTuning(2f));
                meeple.place(unit, camera.camera, position, 0, 2);
                assertEquals(48, unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform).getWidth(), 0.01f,
                      "The unit scale must size the token footprint");
            } finally {
                BoardGeometry.tune(original);
            }
            meeple.place(unit, camera.camera, position, 0, 1);
            assertEquals(BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE,
                  unit.calculateBoundingBox(new BoundingBox()).mul(unit.transform).getDepth(), 0.001f);
            meeple.place(unit, camera.camera, new Vector3(position).add(0, -100, 0), 0, 2);
            terrain.renderShadows(List.of(unit));
            drawMeeple(terrain, camera, batch, unit);
            assertEquals(brightness(camera, -35), brightness(camera, 35), 3,
                  "Moving a meeple must clear its old shadow");

            camera.setIsometric(true);
            camera.center(position);
            camera.zoom(0.4f);
            meeple.place(unit, camera.camera, position, 0, 2);
            int[] sideBrightness = new int[2];
            int index = 0;
            for (float direction : new float[] { 38, -38 }) {
                terrain.update(shadowScene(floor, 0, new BoardScene.Light(direction, 0)));
                terrain.renderShadows(List.of(unit));
                drawMeeple(terrain, camera, batch, unit);
                sideBrightness[index++] = brightness(camera, new Vector3(position).add(12, 0, BoardGeometry.LEVEL));
            }
            assertTrue(Math.abs(sideBrightness[0] - sideBrightness[1]) > 20,
                  "Meeple side normals must respond to light direction");
            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
            assertTrue(output.isDirectory() || output.mkdirs());
            GpuBoardTestUi.capture(new File(output, "meeple-shadow.png"));
        } finally {
            meeple.dispose();
            batch.dispose();
            terrain.dispose();
            atlas.dispose();
        }
    }

    /** The current tuning with the unit footprint scale replaced, for the meeple size checks. */
    private static BoardGeometry.Tuning unitScaleTuning(float scale) {
        return new BoardGeometry.Tuning(BoardGeometry.PADDING, BoardGeometry.HEX_SCALE,
              BoardGeometry.INTERPOLATION_INSET, BoardGeometry.CORNER_BEVEL, BoardGeo,
              BoardGeometry.HEX_FRAME_SHADEmetry.BAND_NORMAL_FLATNESS,
              scale, BoardGeometry.UNIT_HEIGHT_SCALE, BoardGeometry.BASE_LEVEL_HEIGHT);
    }

    private void drawMeeple(GpuTerrain terrain, BoardCamera camera, ModelBatch batch, ModelInstance unit) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        batch.render(unit, terrain.environment());
        batch.end();
    }

    private BoardScene shadowScene(BoardScene.Pixels pixels, int elevation, BoardScene.Light light) {
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), x == 2 && y == 2 ? elevation : 0, pixels));
            }
        }
        return new BoardScene(0, 5, 5, tiles, List.of(), List.of(), -1, "", List.of(), light);
    }

    private void draw(GpuTerrain terrain, BoardCamera camera, BoardScene scene) {
        terrain.update(scene);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        terrain.render(camera.camera, false);
    }

    /** Samples the ground of the neighbor the light direction casts onto, away from the frame and the padding. */
    private int lowGround(BoardCamera camera, boolean east) {
        return brightness(camera, BoardGeometry.center(east ? new Coords(3, 2) : new Coords(1, 2), 0));
    }

    private int brightness(BoardCamera camera, float dx) {
        return brightness(camera, BoardGeometry.center(new Coords(2, 2), 0).add(dx, 0, 0));
    }

    private int brightness(BoardCamera camera, Vector3 position) {
        return rgba(camera, position) >>> 24;
    }

    private int rgba(BoardCamera camera, Vector3 position) {
        // Camera.project projects in place, so the caller's vector must not be handed to it.
        Vector3 point = camera.camera.project(new Vector3(position),
              0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Pixmap sample = Pixmap.createFromFrameBuffer((int) point.x, (int) point.y, 1, 1);
        try {
            return sample.getPixel(0, 0);
        } finally {
            sample.dispose();
        }
    }
}
