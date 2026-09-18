/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.ArrayList;
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
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Riverbanks must continue the actual adjacent artwork, including when an atlas slot changes. */
@Tag("on-demand")
class GpuRiverbankSmokeTest {
    @Test
    void banksMatchSandGrassAndSnowAndFollowArtworkUpdates() throws Exception {
        Hex[] hexes = new Hex[49];
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                Hex hex = new Hex(0);
                if (x < 3) {
                    hex.addTerrain(new Terrain(Terrains.SAND, 1));
                } else if (x > 3 && y >= 4) {
                    hex.setTheme("snow");
                    hex.addTerrain(new Terrain(Terrains.SNOW, 1));
                }
                if (x == 3 && y > 0 && y < 6) {
                    hex.addTerrain(new Terrain(Terrains.WATER, 1));
                }
                hexes[y * 7 + x] = hex;
            }
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create(new Board(7, 7, hexes))) {
            SwingUtilities.invokeAndWait(fixture.source::refresh);
            BoardScene scene = fixture.source.takeFrame().scene();
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    GpuTerrain terrain = new GpuTerrain();
                    try {
                        Coords river = new Coords(3, 3);
                        BoardCamera camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        camera.center(BoardGeometry.center(river, 0));
                        camera.zoom(0.25f);
                        terrain.update(scene);
                        draw(terrain, camera);
                        File output = new File(System.getProperty("megamek.gpu.screenshots"));
                        GpuBoardTestUi.capture(new File(output, "riverbank-terrain-top.png"));
                        checkBanks(camera, river);
                        camera.setIsometric(true);
                        draw(terrain, camera);
                        GpuBoardTestUi.capture(new File(output, "riverbank-terrain-isometric.png"));

                        // Change pixels without changing terrain metadata or the water hex, as an atlas update does.
                        Coords land = river.translated(5);
                        BoardScene.Tile old = scene.tile(land);
                        var tiles = new ArrayList<>(scene.tiles());
                        tiles.set(land.getX() * scene.height() + land.getY(), new BoardScene.Tile(land,
                              old.elevation(), old.waterDepth(), old.frozen(), old.roadExits(), old.surface(),
                              scene.tile(river.translated(2)).ground(), old.decals(), old.tactical(), old.features(), old.text()));
                        terrain.update(new BoardScene(scene.boardId(), scene.width(), scene.height(), tiles, scene.units(),
                              scene.plannedPath(), scene.selectedId(), scene.phase(), scene.commands(), scene.light()));
                        camera.setIsometric(false);
                        draw(terrain, camera);
                        checkBanks(camera, river);
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        terrain.dispose();
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static void draw(GpuTerrain terrain, BoardCamera camera) {
        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
        terrain.render(camera.camera, false);
        terrain.renderTransparent(camera.camera);
    }

    private static void checkBanks(BoardCamera camera, Coords river) {
        Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0, 0,
              Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            for (int edge : new int[] { 0, 2, 3, 5 }) {
                Vector3 a = BoardGeometry.corner(river, 0, edge);
                Vector3 b = BoardGeometry.corner(river, 0, edge + 1);
                Vector3 outward = new Vector3(b).sub(a).crs(Vector3.Z).nor();
                Vector3 middle = new Vector3(a).lerp(b, 0.5f);
                Vector3 bank = camera.camera.project(new Vector3(middle).mulAdd(outward, -2));
                Vector3 land = camera.camera.project(new Vector3(middle).mulAdd(outward, 2));
                for (int shift : new int[] { 8, 16, 24 }) {
                    assertEquals(channel(pixels, land, shift), channel(pixels, bank, shift), 12,
                          "Riverbank artwork must continue its dry neighbor at edge " + edge + ", channel " + shift);
                }
            }
        } finally {
            pixels.dispose();
        }
    }

    private static int channel(Pixmap pixels, Vector3 point, int shift) {
        int sum = 0;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                sum += (pixels.getPixel(Math.round(point.x) + x, Math.round(point.y) + y) >>> shift) & 255;
            }
        }
        return sum / 9;
    }
}
