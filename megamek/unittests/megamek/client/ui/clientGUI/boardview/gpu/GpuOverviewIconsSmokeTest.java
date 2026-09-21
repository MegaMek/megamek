/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static megamek.client.ui.clientGUI.boardview.gpu.GpuCamouflageReview.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual artwork, flat placement, input, shadow invalidation and reversible camera switching. */
@Tag("on-demand")
class GpuOverviewIconsSmokeTest {
    @Test
    void cameraToggleReplacesUnitsAndFoliageWithoutResettingPoses() throws Exception {
        Hex[] hexes = new Hex[12 * 12];
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                Hex hex = new Hex(x >= 8 ? 1 : 0);
                hex.setTheme(x >= 8 ? "snow" : "grass");
                if (y >= 3 && y <= 7 && x % 3 != 0) {
                    hex.addTerrain(new Terrain(x < 6 ? Terrains.WOODS : Terrains.JUNGLE, 1 + x % 3));
                    hex.addTerrain(new Terrain(Terrains.FOLIAGE_ELEV, 2));
                }
                hexes[y * 12 + x] = hex;
            }
        }
        var failure = new AtomicReference<Throwable>();
        try (var fixture = GpuBoardFixture.create(new Board(12, 12, hexes))) {
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setFacing(2);
                fixture.entity.setSecondaryFacing(2);
                fixture.source.refresh();
            });
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override public void create() {
                    var view = new GpuBattleView(fixture.source);
                    try {
                        view.create();
                        view.render();
                        verify(view, fixture);
                    } catch (Throwable error) { failure.set(error); }
                    finally { view.dispose(); Gdx.app.exit(); }
                }
            }, GpuBoardWindow.configuration(false));
            if (failure.get() != null) { throw new AssertionError("Overview icons failed", failure.get()); }
            assertEquals(0, fixture.clicks.get());
        }
    }

    @SuppressWarnings("unchecked")
    private static void verify(GpuBattleView view, GpuBoardFixture fixture) throws Exception {
        var icons = (GpuUnitIcons) field(view, "unitIcons");
        var terrain = (GpuTerrain) field(view, "terrain");
        var ui = (GpuBoardUi) field(view, "ui");
        var scene = (BoardScene) field(view, "scene");
        var unit = scene.units().getFirst();
        var models = (Map<String, ModelInstance>) field(view, "unitInstances");
        var original = models.get("1:-1");
        for (var tile : scene.tiles()) {
            if (tile.features().stream().anyMatch(feature -> feature.kind() == BoardScene.FeatureKind.TREE)) {
                assertNotNull(tile.foliage(), "Forested hexes retain their matching flat tileset art");
            }
        }
        verifyFoliageSilhouette(scene);
        view.boardCamera.setIsometric(false);
        view.boardCamera.zoom(2 / view.boardCamera.camera.zoom);
        view.render();
        assertFalse(icons.active(), "The option is opt-in");
        GpuBoardTestUi.click("camera");
        GpuBoardTestUi.click("camera-overview-icons");
        assertTrue(ui.overviewIcons());
        assertTrue(GpuBoardTestUi.stage().getRoot().<CheckBox>findActor("tuning-overview-icons").isChecked());
        GpuBoardTestUi.click("camera");
        view.render();
        assertTrue(icons.active());
        assertTrue((boolean) field(terrain, "flatTrees"));
        assertTrue(((List<?>) field(terrain, "shadowModels")).isEmpty(), "Hidden models must not cast shadows");
        assertSame(original, models.get("1:-1"), "Camera switches preserve the animated 3D instance");
        var icon = icons.instance(unit);
        assertNotNull(icon);
        assertEquals(0, UnitBounds.local(icon).getDepth(), .0001f);
        assertEquals(1, new Vector3(Vector3.Z).rot(icon.transform).nor().z, .0001f);
        assertEquals(0.25f * BoardGeometry.HEX_SCALE, icon.transform.getTranslation(new Vector3()).z, .0001f);
        capture("overview-icons-top.png");

        view.boardCamera.orbit(75, 29);
        view.render();
        assertTrue(icons.active());
        assertEquals(1, new Vector3(Vector3.Z).rot(icon.transform).nor().z, .0001f,
              "The icon lies on the board instead of following the camera tilt");
        Vector3 point = view.boardCamera.camera.project(icon.transform.getTranslation(new Vector3()), 0, ui.bottomPixels(),
              view.boardCamera.camera.viewportWidth, view.boardCamera.camera.viewportHeight);
        Object input = field(view, "boardInput");
        var pick = input.getClass().getDeclaredMethod("pick", int.class, int.class);
        pick.setAccessible(true);
        assertEquals(unit.location().coords(), pick.invoke(input, Math.round(point.x), Gdx.graphics.getHeight() - Math.round(point.y)));
        capture("overview-icons-tilted.png");

        var poses = (Map<BoardScene.Unit, UnitFootprint.Pose>) field(view, "unitFootprints");
        var moved = new HashMap<>(poses);
        var position = poses.get(unit).position().cpy().add(12, -9, 200);
        moved.put(unit, new UnitFootprint.Pose(unit, position, 240));
        var anchors = new HashMap<BoardScene.Unit, Vector3>();
        icons.update(true, 56, view.boardCamera.camera, scene, moved, anchors, new BoardSurface.Cache());
        assertEquals(position.x, icon.transform.getTranslation(new Vector3()).x, .0001f);
        assertEquals(position.y, icon.transform.getTranslation(new Vector3()).y, .0001f);
        assertTrue(icon.transform.getTranslation(new Vector3()).z < BoardGeometry.LEVEL,
              "Even airborne animation poses project onto the hex surface");

        view.boardCamera.orbit(0, 2);
        view.render();
        assertFalse(icons.active());
        assertFalse((boolean) field(terrain, "flatTrees"));
        assertSame(original, models.get("1:-1"));
        assertFalse(((List<?>) field(terrain, "shadowModels")).isEmpty());
        capture("overview-models-oblique.png");

        view.boardCamera.setIsometric(false);
        view.boardCamera.zoom(.6f / view.boardCamera.camera.zoom);
        view.render();
        assertFalse(icons.active(), "Near top view keeps the full models");
        view.boardCamera.zoom(2 / view.boardCamera.camera.zoom);
        view.render();
        assertTrue(icons.active());
        GpuBoardTestUi.click("tuning");
        Slider threshold = GpuBoardTestUi.stage().getRoot().findActor("Icon switch hex px");
        threshold.setValue(24);
        view.render();
        assertFalse(icons.active());
        threshold.setValue(90);
        view.render();
        assertTrue(icons.active());
        GpuBoardTestUi.click("tuning-defaults");
        view.render();
        assertFalse(icons.active());
        assertEquals(GpuUnitIcons.DEFAULT_HEX_PIXELS, ui.overviewHexPixels());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(new Coords(5, 5), fixture.entity.getPosition());
            assertEquals(2, fixture.entity.getFacing());
        });
        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
    }

    private static void capture(String name) {
        File directory = new File(System.getProperty("megamek.gpu.screenshots"));
        assertTrue(directory.isDirectory() || directory.mkdirs());
        GpuBoardTestUi.capture(new File(directory, name));
    }

    /** Compare real tileset canopy pixels outside the hex with the actual tactical draw. */
    private static void verifyFoliageSilhouette(BoardScene scene) {
        var coords = new Coords(0, 0);
        var tile = scene.tiles().stream().filter(item -> item.foliage() != null && !item.liquid().present()).findFirst().orElseThrow();
        var art = tile.foliage();
        var flat = new BoardScene.Tile(coords, 0, -1, false, 0, tile.surface(), tile.ground(),
              null, null, null, null, List.of(), List.of(), BoardLiquid.NONE, art);
        var isolated = new BoardScene(0, 1, 1, List.of(flat), List.of(), List.of(), -1, "", List.of());
        var terrain = new GpuTerrain();
        var target = new FrameBuffer(Pixmap.Format.RGBA8888, art.width(), art.height(), true);
        var camera = new OrthographicCamera(BoardGeometry.WIDTH, BoardGeometry.HEIGHT);
        camera.position.set(BoardGeometry.center(coords, 0)).add(0, 0, 50);
        camera.direction.set(0, 0, -1);
        camera.up.set(0, 1, 0);
        camera.update();
        Pixmap rendered = null;
        try {
            terrain.update(isolated);
            terrain.setFlatTrees(true);
            target.begin();
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            terrain.render(camera, true);
            rendered = Pixmap.createFromFrameBuffer(0, 0, art.width(), art.height());
            target.end();
            int overhang = 0;
            int missing = 0;
            for (int y = 0; y < art.height(); y++) {
                for (int x = 0; x < art.width(); x++) {
                    float worldX = (x + .5f) / art.width() * BoardGeometry.WIDTH;
                    float worldY = -(y + .5f) / art.height() * BoardGeometry.HEIGHT;
                    if ((art.rgba(y * art.width() + x) & 255) >= 240 && !BoardGeometry.contains(coords, worldX, worldY)) {
                        overhang++;
                        if ((rendered.getPixel(x, art.height() - y - 1) & 255) < 200) { missing++; }
                    }
                }
            }
            PixmapIO.writePNG(Gdx.files.absolute(new File(System.getProperty("megamek.gpu.screenshots"),
                  "overview-foliage-full.png").getAbsolutePath()), rendered, -1, true);
            assertTrue(overhang > 0, "The real artwork must exercise canopies extending outside the hex");
            assertEquals(0, missing, "Canopy pixels outside the hex must survive the tactical render");
        } finally {
            if (rendered != null) { rendered.dispose(); }
            target.dispose();
            terrain.dispose();
        }
    }
}
