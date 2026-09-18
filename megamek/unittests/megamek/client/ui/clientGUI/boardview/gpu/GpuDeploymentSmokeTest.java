/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Ground markings retain their native shape and height while opaque structures hide them. */
@Tag("on-demand")
class GpuDeploymentSmokeTest {
    @Test
    void deploymentAndOtherGroundMarkersStayFlatAndRespectOpaqueDepth() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board())) {
            SwingUtilities.invokeAndWait(() -> {
                fixture.player.setStartingPos(Board.START_ANY);
                fixture.entity.setDeployed(false);
                fixture.game.setPhase(GamePhase.DEPLOYMENT);
                fixture.view.markDeploymentHexesFor(fixture.entity);
                fixture.source.refresh();
            });
            BoardScene scene = fixture.source.takeFrame().scene();
            assertTrue(scene.tiles().stream().anyMatch(tile -> tile.tactical() != null));
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    GpuTerrain terrain = new GpuTerrain();
                    GpuAtmosphere atmosphere = new GpuAtmosphere();
                    BoardGeometry.Tuning original = BoardGeometry.tuning();
                    try {
                        BoardCamera camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        File output = new File(System.getProperty("megamek.gpu.screenshots"));
                        assertTrue(output.isDirectory() || output.mkdirs());
                        terrain.update(scene);
                        terrain.setAtmosphere(atmosphere.lighting());
                        for (boolean isometric : new boolean[] { true, false }) {
                            camera.setIsometric(isometric);
                            camera.fit(scene);
                            drawWorld(terrain, atmosphere, camera, scene);
                            terrain.render(camera.camera, true);
                            GpuBoardTestUi.capture(new File(output, "deployment-depth-" + (isometric ? "isometric" : "top") + ".png"));
                        }
                        // Exercise the normal dimensions and a changed level/hex scale, using the same scene.
                        for (BoardGeometry.Tuning tuning : List.of(original,
                              new BoardGeometry.Tuning(1.4f, 0.6f, 0.87f, 27, 0.8f))) {
                            BoardGeometry.tune(tuning);
                            for (boolean isometric : new boolean[] { false, true }) {
                                camera.setIsometric(isometric);
                                checkProbe(terrain, atmosphere, camera, scene, new Coords(3, 3), 0, 0, false);
                                checkProbe(terrain, atmosphere, camera, scene, new Coords(1, 0), 0, 0, true);
                            }
                            camera.setIsometric(true);
                            checkProbe(terrain, atmosphere, camera, scene, new Coords(1, 1), 28, -16, false);
                            // A flat marker stays above a descending road cut and the foot of an approach,
                            // while the higher part of a rising approach occludes it.
                            checkProbe(terrain, atmosphere, camera, scene, new Coords(3, 1), 0, -30, true);
                            checkProbe(terrain, atmosphere, camera, scene, new Coords(3, 2), 0, 20, true);
                            checkProbe(terrain, atmosphere, camera, scene, new Coords(3, 2), 0, 30, false);
                            checkProbe(terrain, atmosphere, camera, scene, new Coords(6, 5), 0, 0, true);
                            checkProbe(terrain, atmosphere, camera, scene, new Coords(6, 6), 0, 0, true);
                        }
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        BoardGeometry.tune(original);
                        terrain.dispose();
                        atmosphere.dispose();
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static Board board() {
        Hex[] hexes = new Hex[7 * 7];
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                Hex hex = new Hex(x == 2 && y == 2 ? 4 : y < 2 ? 2 : 0);
                if (x == 3) {
                    hex.addTerrain(new Terrain(Terrains.ROAD, 1, true, (1 << 0) | (1 << 3)));
                }
                if ((x == 3 && y == 3) || (x == 1 && y == 4)) {
                    hex.addTerrain(new Terrain(Terrains.BUILDING, 3, true, 0));
                    hex.addTerrain(new Terrain(Terrains.BLDG_CF, 100));
                    hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 3));
                } else if (x == 4 && y == 5) {
                    hex.addTerrain(new Terrain(Terrains.FUEL_TANK, 2, true, 0));
                    hex.addTerrain(new Terrain(Terrains.FUEL_TANK_ELEV, 2));
                    hex.addTerrain(new Terrain(Terrains.FUEL_TANK_CF, 40));
                    hex.addTerrain(new Terrain(Terrains.FUEL_TANK_MAGN, 100));
                } else if (x == 6 && y >= 4) {
                    hex.addTerrain(new Terrain(Terrains.WATER, 2));
                    if (y == 6) {
                        hex.addTerrain(new Terrain(Terrains.ICE, 1));
                    }
                }
                hexes[y * 7 + x] = hex;
            }
        }
        return new Board(7, 7, hexes);
    }

    private static void drawWorld(GpuTerrain terrain, GpuAtmosphere atmosphere, BoardCamera camera, BoardScene scene) {
        terrain.animate(0, List.of());
        terrain.renderShadows(camera.camera, List.of());
        ScreenUtils.clear(0, 0, 0, 1, true);
        atmosphere.begin(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0);
        terrain.render(camera.camera, false);
        terrain.renderTransparent(camera.camera);
        atmosphere.end(camera.camera, terrain, List.of(), scene, 0);
        atmosphere.restoreDepth(camera.camera, terrain, List.of());
    }

    private static void checkProbe(GpuTerrain terrain, GpuAtmosphere atmosphere, BoardCamera camera, BoardScene source,
          Coords coords, int dx, int dy, boolean visible) {
        BufferedImage image = new BufferedImage(252, 216, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(255, 0, 255, 128));
            graphics.fillRect(126 + dx * 3 - 3, 108 - dy * 3 - 3, 6, 6);
        } finally {
            graphics.dispose();
        }
        BoardScene.Pixels probe = new BoardScene.Pixels(image);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (BoardScene.Tile tile : source.tiles()) {
            List<BoardScene.Feature> features = tile.features();
            if (tile.coords().equals(new Coords(3, 3))) {
                // A shipped solid roof gives a known enclosed test point independent of random tileset variants.
                features = List.of(new BoardScene.Feature("buildings/saxarba/building_hard/building_hard_00",
                      0, 0, 0, 1, 3, 0));
            }
            tiles.add(new BoardScene.Tile(tile.coords(), tile.elevation(), tile.waterDepth(), tile.frozen(), tile.roadExits(),
                  tile.surface(), tile.ground(), tile.decals(), tile.coords().equals(coords) ? probe : null, features, tile.text()));
        }
        BoardScene scene = new BoardScene(source.boardId(), source.width(), source.height(), tiles, List.of(), List.of(),
              -1, source.phase(), List.of(), source.light());
        terrain.update(scene);
        BoardScene.Tile tile = scene.tile(coords);
        Vector3 point = BoardGeometry.center(coords, tile.elevation()).add(dx * BoardGeometry.HEX_SCALE,
              dy * BoardGeometry.HEX_SCALE, 0);
        point.z = BoardGeometry.surfaceZ(tile) + BoardGeometry.LEVEL / 3;
        camera.camera.zoom = 0.25f * BoardGeometry.HEX_SCALE;
        camera.center(point);
        drawWorld(terrain, atmosphere, camera, scene);
        Vector3 screen = camera.camera.project(new Vector3(point));
        int x = Math.round(screen.x * Gdx.graphics.getBackBufferWidth() / Gdx.graphics.getWidth());
        int y = Math.round(screen.y * Gdx.graphics.getBackBufferHeight() / Gdx.graphics.getHeight());
        Pixmap before = ScreenUtils.getFrameBufferPixmap(x - 3, y - 3, 7, 7);
        terrain.render(camera.camera, true);
        Pixmap after = ScreenUtils.getFrameBufferPixmap(x - 3, y - 3, 7, 7);
        try {
            for (int row = 0; row < 7; row++) {
                for (int column = 0; column < 7; column++) {
                    int rgba = after.getPixel(column, row);
                    if (!visible) {
                        assertEquals(before.getPixel(column, row), rgba, "Opaque geometry must hide the marker at " + coords);
                    }
                }
            }
            if (visible) {
                int base = before.getPixel(3, 3);
                int marked = after.getPixel(3, 3);
                for (int shift : new int[] { 8, 16, 24 }) {
                    int color = shift == 16 ? 0 : 255;
                    int expected = Math.round((color * 128 + ((base >>> shift) & 255) * 127) / 255f);
                    assertEquals(expected, (marked >>> shift) & 255, 2,
                          "Marker must stay on the flat hex plane at one-third of a level and blend only once: " + coords);
                }
            }
        } finally {
            before.dispose();
            after.dispose();
        }
    }
}
