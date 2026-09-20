/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native depth/mask integration: a level-one unit behind a level-four ridge, with real articulated unit geometry. */
@Tag("on-demand")
class GpuUnitVisibilitySmokeTest {
    @Test
    void highlightsOnlyOccludedPartsWithoutChangingOpacityOrExposingAbsentUnits() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    checkVisibility();
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        assertNull(failure.get(), () -> String.valueOf(failure.get()));
    }

    private void checkVisibility() {
        GpuTerrain terrain = new GpuTerrain();
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        GpuUnitVisibility visibility = new GpuUnitVisibility();
        GpuUnitModels models = new GpuUnitModels();
        GpuBoardSkin skin = new GpuBoardSkin();
        ModelBatch batch = new ModelBatch(GpuUnitCamouflage.shaders());
        GpuUnitCamouflage camouflage = new GpuUnitCamouflage();
        UnitCamouflage camoSource = new UnitCamouflage();
        List<Pixmap> captures = new ArrayList<>();
        try {
            GpuBoardTuning tuning = new GpuBoardTuning(skin.skin);
            Slider strength = tuning.panel().findActor("See-through");
            assertNotNull(strength);
            assertEquals(GpuUnitVisibility.DEFAULT_OUTLINE_INTENSITY, tuning.seeThrough());
            strength.setValue(0);
            assertEquals(0, tuning.seeThrough());
            assertEquals(GpuTerrain.DEFAULT_BUILDING_OPACITY, tuning.buildingOpacity());
            assertNull(tuning.panel().findActor("Tree opacity"));
            tuning.panel().findActor("tuning-defaults").fire(new ChangeListener.ChangeEvent());
            assertEquals(GpuUnitVisibility.DEFAULT_OUTLINE_INTENSITY, tuning.seeThrough());

            BoardScene scene = ridge();
            BoardCamera camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() - 120);
            camera.setIsometric(true);
            camera.orbit(-45, 20);
            camera.fit(scene);
            terrain.update(scene);
            GpuUnitModel atlas = models.get(new BoardScene.UnitModel("units/modular/meks/atlas.json",
                  "units/modular/meks/fallback-biped-heavy.json", "Atlas AS7-D", 1, 0, BoardScene.LocationDamage.NONE,
                  UnitModelState.capture(new megamek.common.units.BipedMek())));
            assertNotNull(atlas);
            ModelInstance unit = new ModelInstance(atlas.instance.model);
            var entity = new megamek.common.units.BipedMek();
            entity.setCamouflage(new megamek.common.icons.Camouflage("Word of Blake/", "TerraSec (Camo).png"));
            var paint = camoSource.resolve(UnitCamouflageTest.selection(entity)).state().appearance();
            camouflage.apply(unit, atlas.instance, paint);
            unit.userData = new Color(0.1f, 0.45f, 1, 1);
            atlas.place(unit, camera.camera, BoardGeometry.center(new Coords(3, 2), 1), 0, 2, false);
            List<ModelInstance> units = List.of(unit);

            Pixmap off = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0, captures);
            capture("see-through-off.png");
            Pixmap low = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0.25f, captures);
            Pixmap high = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0.75f, captures);
            capture("see-through-ridge.png");
            assertTrue(difference(off, high) > 10000, "A unit behind higher terrain must have a visible silhouette");
            assertTrue(difference(off, high) > difference(off, low) * 2,
                  "Intensity must control both the fill and outline without altering the scene");
            assertEquals(off.getPixel(20, 20), high.getPixel(20, 20), "The highlight must stay inside the board viewport");
            assertEquals(0, difference(off, draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0, captures)),
                  "Disabling see-through must restore the original frame immediately");

            ModelInstance opponent = new ModelInstance(atlas.instance.model);
            camouflage.apply(opponent, atlas.instance, paint);
            opponent.userData = new Color(0.9f, 0.15f, 0.1f, 1);
            atlas.place(opponent, camera.camera, BoardGeometry.center(new Coords(1, 2), 1), 0, 2, false);
            List<ModelInstance> teams = List.of(unit, opponent);
            Pixmap teamsOff = draw(terrain, atmosphere, visibility, batch, camera, scene, teams, 0, captures);
            Pixmap teamsOn = draw(terrain, atmosphere, visibility, batch, camera, scene, teams, 0.75f, captures);
            assertTrue(coloredPixels(teamsOff, teamsOn, 24) > 100, "The opponent must have its red outline");
            assertTrue(coloredPixels(teamsOff, teamsOn, 8) > 100, "Our unit must retain its blue outline in the same frame");
            capture("see-through-team-colors.png");

            // A partly hidden unit keeps its exposed head in the original material, with only its hidden body tinted.
            camera.setIsometric(true);
            camera.orbit(-45, 0);
            camera.fit(scene);
            Pixmap partialOff = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0, captures);
            Pixmap partialOn = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0.75f, captures);
            assertTrue(difference(partialOff, partialOn) > 1000, "Partial occlusion must also reveal the hidden section");
            capture("see-through-partial.png");

            // Nearest-surface depth must suppress self-occlusion between the Atlas's arms, torso and legs.
            atlas.place(unit, camera.camera, BoardGeometry.center(new Coords(3, 4), 4), 0, 2, false);
            for (boolean top : new boolean[] { false, true }) {
                camera.setIsometric(!top);
                camera.fit(scene);
                Pixmap exposed = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0, captures);
                Pixmap highlighted = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 1, captures);
                assertEquals(0, difference(exposed, highlighted), "An exposed unit must not highlight itself: top=" + top);
            }

            Pixmap empty = draw(terrain, atmosphere, visibility, batch, camera, scene, List.of(), 0, captures);
            assertEquals(0, difference(empty, draw(terrain, atmosphere, visibility, batch, camera, scene, List.of(), 1, captures)),
                  "Removed or game-hidden units must leave no stale silhouette");

            // Resize both buffers and exercise depth sharing with fog enabled, then disabled again.
            camera.resize(900, 480);
            camera.setIsometric(true);
            camera.orbit(-45, 20);
            camera.fit(scene);
            atlas.place(unit, camera.camera, BoardGeometry.center(new Coords(3, 2), 1), 0, 2, false);
            for (float fog : new float[] { 0.3f, 0 }) {
                atmosphere.configure(new BoardAtmosphere.Settings(13, 0, fog, 2.5f, 0, 0));
                Pixmap before = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0, captures);
                Pixmap after = draw(terrain, atmosphere, visibility, batch, camera, scene, units, 0.75f, captures);
                assertTrue(difference(before, after) > 1000, "See-through must survive viewport resizing and fog changes");
            }
            Coords wooded = new Coords(3, 2);
            List<BoardScene.Tile> groveTiles = scene.tiles().stream().map(tile -> new BoardScene.Tile(tile.coords(),
                  0, -1, false, 0, tile.surface(), tile.ground(), null, null,
                  tile.coords().equals(wooded) ? List.of(new BoardScene.Feature("tree-broad", 0, 0, 0, 1.5f, 4, 0,
                        BoardScene.FeatureKind.TREE)) : List.of(), List.of())).toList();
            BoardScene grove = new BoardScene(0, scene.width(), scene.height(), groveTiles,
                  List.of(), List.of(), -1, "", List.of());
            terrain.update(grove);
            for (boolean top : new boolean[] { false, true }) {
                camera.setIsometric(!top);
                camera.fit(grove);
                atlas.place(unit, camera.camera, BoardGeometry.center(wooded, 0), 0, 2, false);
                Pixmap opaque = draw(terrain, atmosphere, visibility, batch, camera, grove, units, 0, captures);
                Pixmap outlined = draw(terrain, atmosphere, visibility, batch, camera, grove, units, .75f, captures);
                assertTrue(difference(opaque, outlined) > 1000, "Opaque canopies must allow unit outlines in both views");
                capture("see-through-trees-" + (top ? "top" : "iso") + ".png");
            }
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        } finally {
            captures.forEach(Pixmap::dispose);
            batch.dispose();
            camouflage.dispose();
            camoSource.clear();
            skin.dispose();
            models.dispose();
            visibility.dispose();
            atmosphere.dispose();
            terrain.dispose();
        }
    }

    private Pixmap draw(GpuTerrain terrain, GpuAtmosphere atmosphere, GpuUnitVisibility visibility, ModelBatch batch,
          BoardCamera camera, BoardScene scene, List<ModelInstance> units, float intensity, List<Pixmap> captures) {
        terrain.setAtmosphere(atmosphere.lighting());
        // The effect must work with opaque terrain and the building cutaway disabled.
        terrain.animate(0, units, 1);
        terrain.renderShadows(camera.camera, units);
        ScreenUtils.clear(0.02f, 0.03f, 0.04f, 1, true);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0,
              intensity > 0 && !units.isEmpty());
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        units.forEach(unit -> batch.render(unit, terrain.environment()));
        batch.end();
        terrain.renderTransparent(camera.camera);
        atmosphere.end(camera.camera, terrain, units, scene, 60);
        atmosphere.restoreDepth(camera.camera, terrain, units);
        visibility.render(camera.camera, units, atmosphere.depthTexture(), 60, intensity, 1);
        Pixmap result = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        captures.add(result);
        return result;
    }

    private static BoardScene ridge() {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(111, 130, 76));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        BoardScene.Pixels art = new BoardScene.Pixels(image);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 6; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), y >= 3 ? 4 : 1, -1, false, 0,
                      BoardScene.Surface.GRASS, art, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 7, 6, tiles, List.of(), List.of(), -1, "", List.of());
    }

    private static long difference(Pixmap a, Pixmap b) {
        long result = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int before = a.getPixel(x, y), after = b.getPixel(x, y);
                for (int shift = 8; shift <= 24; shift += 8) {
                    result += Math.abs(((before >>> shift) & 255) - ((after >>> shift) & 255));
                }
            }
        }
        return result;
    }

    private static int coloredPixels(Pixmap before, Pixmap after, int channel) {
        int count = 0;
        for (int y = 0; y < after.getHeight(); y++) {
            for (int x = 0; x < after.getWidth(); x++) {
                int pixel = after.getPixel(x, y);
                int primary = (pixel >>> channel) & 255;
                boolean colored = pixel != before.getPixel(x, y) && primary > 160;
                for (int shift = 8; shift <= 24; shift += 8) {
                    if (shift != channel && primary < ((pixel >>> shift) & 255) + 60) {
                        colored = false;
                    }
                }
                if (colored) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void capture(String name) {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        GpuBoardTestUi.capture(new File(output, name));
    }
}
