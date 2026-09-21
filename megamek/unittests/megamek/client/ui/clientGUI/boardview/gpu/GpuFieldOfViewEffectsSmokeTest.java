/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The two effects must select their own settings from the same immutable visibility mask. */
@Tag("on-demand")
class GpuFieldOfViewEffectsSmokeTest {
    private record Samples(int visible, int sensor, int blocked) { }

    @Test
    void fovAndSensorEffectsChangeIndependentlyInBothCameraViews() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkEffects();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError("Independent FoV and sensor effects", failure.get());
        }
    }

    private void checkEffects() {
        BufferedImage ground = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = ground.createGraphics();
        graphics.setColor(new java.awt.Color(160, 200, 110));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels pixels = new BoardScene.Pixels(ground);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        List<BoardFieldOfView.Hex> hexes = new ArrayList<>();
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 7; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), 0, -1, false, 0,
                      BoardScene.Surface.GRASS, pixels, null, null, List.of(), List.of()));
                var visibility = x < 3 ? BoardFieldOfView.Visibility.VISIBLE
                      : x < 6 ? BoardFieldOfView.Visibility.SENSOR : BoardFieldOfView.Visibility.BLOCKED;
                hexes.add(new BoardFieldOfView.Hex(visibility, 0, x >= 6));
            }
        }
        BoardScene scene = new BoardScene(0, 9, 7, tiles, List.of(), List.of(), -1, "", List.of());
        // The classic grayscale preference must not override the explicit GPU styles.
        BoardFieldOfView mask = new BoardFieldOfView(9, 7, hexes, 255, 0, true, true, false);
        GpuTerrain terrain = new GpuTerrain();
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        GpuFieldOfView field = new GpuFieldOfView();
        BoardCamera camera = new BoardCamera();
        try {
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            terrain.update(scene);
            atmosphere.configure(new BoardAtmosphere.Settings(12, 0, 0, 1.5f, 0, 0));
            terrain.setAtmosphere(atmosphere.lighting());
            terrain.renderShadows(List.of());
            for (boolean isometric : List.of(false, true)) {
                camera.setIsometric(isometric);
                camera.fit(scene);
                field.configure(GpuFieldOfView.FOV_STYLE, 0, GpuFieldOfView.SENSOR_STYLE, 0);
                Samples clear = draw(scene, terrain, atmosphere, field, camera, mask);

                field.configure(GpuFieldOfView.FOV_STYLE, GpuFieldOfView.FOV_DARKNESS, GpuFieldOfView.SENSOR_STYLE, 0);
                Samples fov = draw(scene, terrain, atmosphere, field, camera, mask);
                assertEquals(clear.visible(), fov.visible());
                assertEquals(clear.blocked(), fov.blocked(), "FoV darkness must leave out-of-sensor hexes alone");
                assertTrue(brightness(fov.sensor()) < brightness(clear.sensor()) * 0.8f);

                field.configure(GpuFieldOfView.FOV_STYLE, 0, GpuFieldOfView.SENSOR_STYLE, GpuFieldOfView.SENSOR_DARKNESS);
                Samples sensor = draw(scene, terrain, atmosphere, field, camera, mask);
                assertEquals(clear.visible(), sensor.visible());
                assertEquals(clear.sensor(), sensor.sensor(), "Sensor darkness must leave sensor-only hexes alone");
                assertTrue(brightness(sensor.blocked()) < brightness(clear.blocked()) * 0.8f);

                field.configure(GpuFieldOfView.FOV_STYLE, GpuFieldOfView.FOV_DARKNESS,
                      GpuFieldOfView.SENSOR_STYLE, GpuFieldOfView.SENSOR_DARKNESS);
                Samples both = draw(scene, terrain, atmosphere, field, camera, mask);
                assertEquals(new Samples(clear.visible(), fov.sensor(), sensor.blocked()), both,
                      "Each hidden hex must use one effect, without stacking them");
                assertTrue(chroma(both.visible()) > 10 && chroma(both.sensor()) > 10,
                      "Visible and dimmed terrain must retain color");
                assertTrue(chroma(both.blocked()) < chroma(both.visible()) / 3,
                      "Fog of war must desaturate out-of-sensor terrain");

                field.configure(GpuFieldOfView.Style.GRAYSCALE, GpuFieldOfView.FOV_DARKNESS,
                      GpuFieldOfView.SENSOR_STYLE, GpuFieldOfView.SENSOR_DARKNESS);
                Samples grayFov = draw(scene, terrain, atmosphere, field, camera, mask);
                assertEquals(both.visible(), grayFov.visible());
                assertEquals(both.blocked(), grayFov.blocked());
                assertTrue(chroma(grayFov.sensor()) <= 1, "FoV grayscale must only desaturate sensor-only hexes");

                field.configure(GpuFieldOfView.FOV_STYLE, GpuFieldOfView.FOV_DARKNESS,
                      GpuFieldOfView.Style.GRAYSCALE, GpuFieldOfView.SENSOR_DARKNESS);
                Samples graySensor = draw(scene, terrain, atmosphere, field, camera, mask);
                assertEquals(both.visible(), graySensor.visible());
                assertEquals(both.sensor(), graySensor.sensor());
                assertTrue(chroma(graySensor.blocked()) <= 1, "Sensor grayscale must only desaturate blocked hexes");
            }
            assertEquals(1, field.uploads(), "Both effects and both cameras must reuse the visibility mask");
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        } finally {
            field.dispose();
            atmosphere.dispose();
            terrain.dispose();
        }
    }

    private static Samples draw(BoardScene scene, GpuTerrain terrain, GpuAtmosphere atmosphere, GpuFieldOfView field,
          BoardCamera camera, BoardFieldOfView mask) {
        field.update(mask);
        ScreenUtils.clear(0, 0, 0, 1, true);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0);
        terrain.render(camera.camera, false);
        atmosphere.end(camera.camera, terrain, scene, 0, field);
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            return new Samples(sample(pixels, camera, 1), sample(pixels, camera, 4), sample(pixels, camera, 7));
        } finally {
            pixels.dispose();
        }
    }

    private static int sample(Pixmap pixels, BoardCamera camera, int column) {
        Vector3 point = BoardGeometry.center(new Coords(column, 3), 0)
              .add(BoardGeometry.WIDTH * 0.1f, BoardGeometry.HEIGHT * 0.08f, 0);
        camera.camera.project(point);
        int x = Math.round(point.x * pixels.getWidth() / Gdx.graphics.getWidth());
        int y = Math.round(point.y * pixels.getHeight() / Gdx.graphics.getHeight());
        assertTrue(x >= 0 && y >= 0 && x < pixels.getWidth() && y < pixels.getHeight());
        return pixels.getPixel(x, y);
    }

    private static float brightness(int rgb) {
        return 0.2126f * (rgb >>> 24) + 0.7152f * (rgb >>> 16 & 255) + 0.0722f * (rgb >>> 8 & 255);
    }

    private static int chroma(int rgb) {
        int red = rgb >>> 24, green = rgb >>> 16 & 255, blue = rgb >>> 8 & 255;
        return Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
    }
}
