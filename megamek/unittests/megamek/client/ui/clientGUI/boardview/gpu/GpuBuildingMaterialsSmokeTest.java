/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real tileset selection and native rendering of construction families, doors, and circular tank roofs. */
@Tag("on-demand")
class GpuBuildingMaterialsSmokeTest {
    @Test
    void selectedRoofFamiliesUseSmallContextualWallTextures() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (GpuBoardFixture fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Hex[] hexes = new Hex[9 * 9];
                for (int y = 0; y < 9; y++) {
                    for (int x = 0; x < 9; x++) {
                        Hex hex = new Hex(0);
                        if ((x & 1) == 1 && y <= 4 && (y & 1) == 0) {
                            hex.addTerrain(new Terrain(Terrains.BUILDING, (x + 1) / 2, true, 0));
                            hex.addTerrain(new Terrain(Terrains.BLDG_CF, 100));
                            hex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 3));
                            hex.addTerrain(new Terrain(Terrains.BLDG_CLASS, y / 2));
                        } else if ((x == 1 || x == 3) && y == 6) {
                            hex.addTerrain(new Terrain(Terrains.FUEL_TANK, x == 1 ? 2 : 4, true, 0));
                            hex.addTerrain(new Terrain(Terrains.FUEL_TANK_ELEV, 2));
                            hex.addTerrain(new Terrain(Terrains.FUEL_TANK_CF, 40));
                            hex.addTerrain(new Terrain(Terrains.FUEL_TANK_MAGN, 100));
                        } else if (x == 5 && y == 6) {
                            hex.addTerrain(new Terrain(Terrains.INDUSTRIAL, 2));
                        }
                        hexes[y * 9 + x] = hex;
                    }
                }
                fixture.game.setBoard(new Board(9, 9, hexes));
                fixture.source.refresh();
            });
            BoardScene captured = fixture.source.takeFrame().scene();
            BoardScene scene = new BoardScene(0, 9, 9, captured.tiles(), List.of(), List.of(), -1, "", List.of(),
                  new BoardScene.Light(-24, -30));
            assertEquals(15, scene.tiles().stream().mapToInt(tile -> tile.features().size()).sum());
            new Lwjgl3Application(new ApplicationAdapter() {
                GpuTerrain terrain;
                BoardCamera camera;
                int frames;

                @Override
                public void create() {
                    try {
                        checkMaterials(scene);
                        terrain = new GpuTerrain();
                        terrain.update(scene);
                        camera = new BoardCamera();
                        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        camera.setIsometric(true);
                        camera.fit(scene);
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void render() {
                    if (failure.get() != null) {
                        return;
                    }
                    try {
                        terrain.animate(0, List.of());
                        terrain.renderShadows(List.of());
                        ScreenUtils.clear(0.035f, 0.055f, 0.075f, 1, true);
                        terrain.render(camera.camera, false);
                        File output = new File(System.getProperty("megamek.gpu.screenshots"));
                        if (++frames == 4) {
                            GpuBoardTestUi.capture(new File(output, "building-material-families.png"));
                            camera.zoom(0.5f);
                            camera.center(BoardGeometry.center(new Coords(4, 0), 1));
                        } else if (frames == 7) {
                            GpuBoardTestUi.capture(new File(output, "building-material-construction.png"));
                            camera.center(BoardGeometry.center(new Coords(4, 2), 1));
                        } else if (frames == 10) {
                            GpuBoardTestUi.capture(new File(output, "building-material-hangars.png"));
                            camera.center(BoardGeometry.center(new Coords(4, 4), 1));
                        } else if (frames == 13) {
                            GpuBoardTestUi.capture(new File(output, "building-material-fortresses.png"));
                            camera.center(BoardGeometry.center(new Coords(3, 6), 1));
                        } else if (frames == 16) {
                            GpuBoardTestUi.capture(new File(output, "building-material-tanks-industrial.png"));
                            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void dispose() {
                    if (terrain != null) {
                        terrain.dispose();
                    }
                }
            }, GpuBoardWindow.configuration(false));
        }
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private static void checkMaterials(BoardScene scene) {
        GpuAssets assets = new GpuAssets();
        try {
            for (var tile : scene.tiles()) {
                for (var feature : tile.features()) {
                    String family = tile.coords().getY() == 6
                          ? tile.coords().getX() == 5 ? "industrial" : "tank"
                          : tile.coords().getY() == 4 ? "fortress" : tile.coords().getY() == 2 ? "hangar"
                          : List.of("light", "medium", "heavy", "hard").get((tile.coords().getX() - 1) / 2);
                    String role = family.equals("fortress") || family.equals("hangar") ? "shell" : "wall";
                    var wall = assets.model(feature.asset()).getMaterial(role);
                    assertNotNull(wall, feature.asset());
                    Texture texture = wall.get(TextureAttribute.class, TextureAttribute.Diffuse).textureDescription.texture;
                    assertSame(assets.material("buildings/" + family), texture, feature.asset());
                    assertEquals(128, texture.getWidth());
                    assertEquals(128, texture.getHeight());
                    assertEquals(Texture.TextureWrap.Repeat, texture.getUWrap());
                }
            }
            for (var surface : BoardScene.Surface.values()) {
                assertEquals(128, assets.material(surface.wall).getWidth());
                assertEquals(128, assets.material(surface.wall).getHeight());
            }
        } finally {
            assets.dispose();
        }
    }
}
