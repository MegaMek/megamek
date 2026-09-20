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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.clientGUI.boardview.BoardFieldOfView;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Cliff faces lie exactly on visibility boundaries; their shading must not alternate while orbiting. */
@Tag("on-demand")
class GpuFieldOfViewCliffSmokeTest {
    @ParameterizedTest
    @EnumSource(value = GpuFieldOfView.Style.class, names = { "DIMMED", "GRAYSCALE" })
    void cliffSidesKeepTheirOwningHexVisibilityThroughoutAnOrbit(GpuFieldOfView.Style style) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkCliffs(style);
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError("FoV cliff orbit regression", failure.get());
        }
    }

    private void checkCliffs(GpuFieldOfView.Style style) throws Exception {
        BufferedImage ground = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = ground.createGraphics();
        graphics.setColor(new java.awt.Color(130, 150, 110));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels pixels = new BoardScene.Pixels(ground);
        Coords raised = new Coords(3, 3);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                Coords coords = new Coords(x, y);
                tiles.add(new BoardScene.Tile(coords, coords.equals(raised) ? 4 : 0, -1, false, 0,
                      BoardScene.Surface.GRASS, pixels, null, null, List.of(), List.of()));
            }
        }
        BoardScene scene = new BoardScene(0, 7, 7, tiles, List.of(), List.of(), -1, "", List.of());
        GpuTerrain terrain = new GpuTerrain();
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        GpuFieldOfView field = new GpuFieldOfView();
        field.configure(style, GpuFieldOfView.FOV_DARKNESS, style, GpuFieldOfView.SENSOR_DARKNESS);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setIsometric(true);
        camera.fit(scene);
        camera.center(BoardGeometry.center(raised, 2));
        camera.zoom(0.32f);
        terrain.update(scene);
        atmosphere.configure(new BoardAtmosphere.Settings(12, 0, 0, 1.5f, 0, 0));
        terrain.setAtmosphere(atmosphere.lighting());
        terrain.renderShadows(List.of());
        var sides = new BoardSurface(scene, scene.tile(raised)).sides(scene, BoardGeometry.floor(scene));
        int compared = 0, mismatched = 0;
        try {
            for (boolean blocked : List.of(false, true)) {
                for (int angle = 0; angle < 24; angle++) {
                    camera.orbit(angle % 2 == 0 ? 29.7f : 0.3f, 0);
                    List<Vector3> samples = new ArrayList<>();
                    Vector3 center = BoardGeometry.center(raised, 0);
                    for (BoardSurface.Side side : sides) {
                        Vector3 normal = new Vector3(side.a()).lerp(side.b(), 0.5f).sub(center);
                        normal.z = 0;
                        if (normal.nor().dot(camera.camera.direction) > -0.4f) {
                            continue;
                        }
                        for (int along = 2; along <= 8; along++) {
                            for (int height = 2; height <= 8; height++) {
                                Vector3 point = new Vector3(side.a()).lerp(side.b(), along / 10f);
                                point.z = BoardGeometry.LEVEL * 4 * height / 10f;
                                samples.add(camera.camera.project(point));
                            }
                        }
                    }
                    Pixmap reference = draw(scene, terrain, atmosphere, field, camera, mask(scene, raised, blocked, true));
                    Pixmap actual = draw(scene, terrain, atmosphere, field, camera, mask(scene, raised, blocked, false));
                    try {
                        for (Vector3 point : samples) {
                            int x = Math.round(point.x * actual.getWidth() / Gdx.graphics.getWidth());
                            int y = Math.round(point.y * actual.getHeight() / Gdx.graphics.getHeight());
                            if (x >= 0 && y >= 0 && x < actual.getWidth() && y < actual.getHeight()) {
                                compared++;
                                if (difference(reference.getPixel(x, y), actual.getPixel(x, y)) > 3) {
                                    mismatched++;
                                }
                                if (blocked && style == GpuFieldOfView.Style.GRAYSCALE) {
                                    int rgb = actual.getPixel(x, y);
                                    assertEquals(rgb >>> 24, rgb >>> 16 & 255, 1, "Blocked cliffs must be grayscale");
                                    assertEquals(rgb >>> 24, rgb >>> 8 & 255, 1, "Blocked cliffs must be grayscale");
                                }
                            }
                        }
                        if (angle == 0) {
                            File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
                            GpuBoardTestUi.capture(new File(output, "fov-cliff-" + style.name().toLowerCase(java.util.Locale.ROOT)
                                  + "-" + (blocked ? "blocked" : "visible") + ".png"));
                        }
                    } finally {
                        reference.dispose();
                        actual.dispose();
                    }
                }
            }
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            assertTrue(compared > 2000, "Sample wall interiors across all six sides, both visibility states and a full orbit");
            assertEquals(0, mismatched, "Cliff pixels must match their owning hex at every angle; sampled " + compared);
        } finally {
            field.dispose();
            atmosphere.dispose();
            terrain.dispose();
        }
    }

    private static BoardFieldOfView mask(BoardScene scene, Coords raised, boolean blocked, boolean uniform) {
        List<BoardFieldOfView.Hex> hexes = scene.tiles().stream().map(tile -> {
            boolean dark = uniform || tile.coords().equals(raised) ? blocked : !blocked;
            return new BoardFieldOfView.Hex(dark ? BoardFieldOfView.Visibility.BLOCKED : BoardFieldOfView.Visibility.VISIBLE, 0);
        }).toList();
        return new BoardFieldOfView(scene.width(), scene.height(), hexes, 120, 0, true, false, false);
    }

    private static Pixmap draw(BoardScene scene, GpuTerrain terrain, GpuAtmosphere atmosphere, GpuFieldOfView field,
          BoardCamera camera, BoardFieldOfView mask) {
        field.update(mask);
        ScreenUtils.clear(0, 0, 0, 1, true);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0, true);
        terrain.render(camera.camera, false);
        atmosphere.end(camera.camera, terrain, List.of(), scene, 0, field);
        return Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    private static int difference(int a, int b) {
        return Math.max(Math.abs((a >>> 24) - (b >>> 24)), Math.max(Math.abs((a >>> 16 & 255) - (b >>> 16 & 255)),
              Math.abs((a >>> 8 & 255) - (b >>> 8 & 255))));
    }
}
