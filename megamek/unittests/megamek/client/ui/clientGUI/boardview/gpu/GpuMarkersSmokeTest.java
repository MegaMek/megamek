/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import megamek.client.ui.clientGUI.boardview.BoardMarker;
import megamek.client.ui.clientGUI.boardview.sprite.CollapseWarningSprite;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.SpecialHexDisplay;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Real OpenGL checks for marker geometry, roof clearance, and the per-type occlusion policies. */
@Tag("on-demand")
class GpuMarkersSmokeTest {
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void rendersOccupiedHexWithCollapseArtilleryAndNote(boolean sensorContact) throws Exception {
        Board board = new Board(7, 6);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 6; y++) {
                board.setHex(new Coords(x, y), new Hex(0));
            }
        }
        Coords stack = new Coords(3, 3);
        board.setHex(stack, new Hex(0, "building:1;bldg_elev:2;bldg_cf:10", ""));
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board)) {
            SwingUtilities.invokeAndWait(() -> {
                Player enemy = new Player(1, "Unknown contact");
                enemy.setTeam(2);
                fixture.game.addPlayer(enemy.getId(), enemy);
                fixture.entity.setPosition(stack);
                if (sensorContact) {
                    fixture.entity.setOwner(enemy);
                    fixture.entity.addBeenDetectedBy(fixture.player);
                } else {
                    fixture.entity.setElevation(2);
                }
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                fixture.view.addSprites(List.of(new CollapseWarningSprite(fixture.view, stack)));
                board.addSpecialHexDisplay(stack, new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_INCOMING,
                      SpecialHexDisplay.NO_ROUND, enemy, "Incoming artillery"));
                board.addSpecialHexDisplay(stack, new SpecialHexDisplay(SpecialHexDisplay.Type.PLAYER_NOTE,
                      SpecialHexDisplay.NO_ROUND, fixture.player, "Hold this building"));
                fixture.source.refresh();
            });
            BoardScene scene = fixture.source.takeFrame().scene();
            assertEquals(3, scene.markers().size());
            assertEquals(sensorContact, scene.units().getFirst().sensorContact());
            AtomicReference<Throwable> failure = new AtomicReference<>();
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;

                @Override
                public void create() {
                    try {
                        super.create();
                        if (sensorContact) {
                            checkContactPlacement(scene);
                        }
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void render() {
                    try {
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        checkOccupiedHex(this, scene);
                        tick++;
                        if (tick == 1) {
                            boardCamera.setIsometric(true);
                            boardCamera.center(BoardGeometry.center(stack, 2));
                            boardCamera.zoom(0.35f);
                        } else if (tick == 5) {
                            capture(sensorContact ? "markers-stack-isometric.png" : "markers-unit-stack-isometric.png");
                            boardCamera.setIsometric(false);
                            boardCamera.orbit(60, 0);
                            boardCamera.center(BoardGeometry.center(stack, 2));
                        } else if (tick == 9) {
                            capture(sensorContact ? "markers-stack-top.png" : "markers-unit-stack-top.png");
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.compareAndSet(null, error);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
            assertNull(failure.get(), () -> String.valueOf(failure.get()));
        }
    }

    private static void checkContactPlacement(BoardScene scene) {
        GpuMarkers markers = new GpuMarkers();
        try {
            BoardCamera camera = new BoardCamera();
            camera.resize(1280, 800);
            camera.fit(scene);
            BoardScene.Unit contact = scene.units().getFirst();
            ModelInstance sensor = new ModelInstance(markers.model(BoardMarker.Kind.SENSOR_CONTACT).instance.model);
            List<BoardMarker> dense = new ArrayList<>(scene.markers());
            for (var kind : List.of(BoardMarker.Kind.MINEFIELD, BoardMarker.Kind.CARGO, BoardMarker.Kind.OBJECTIVE)) {
                dense.add(new BoardMarker(kind, contact.location().coords(), 2));
            }
            for (boolean top : new boolean[] { false, true }) {
                camera.setIsometric(!top);
                for (float seconds : new float[] { 0, 1, 2, 4 }) {
                    markers.beginFrame(List.of(), seconds);
                    Vector3 position = BoardGeometry.center(contact.location().coords(), contact.location().elevation());
                    markers.placeSensor(contact.location().coords(), sensor, camera.camera, position);
                    Matrix4 alone = new Matrix4(sensor.transform);
                    for (int count = 0; count <= dense.size(); count++) {
                        List<BoardMarker> group = dense.subList(0, count);
                        markers.beginFrame(group, 0);
                        markers.placeSensor(contact.location().coords(), sensor, camera.camera, position);
                        markers.update(List.of(sensor), camera.camera);
                        if (!top || group.isEmpty()) {
                            assertArrayEquals(alone.val, sensor.transform.val, 0.0001f,
                                  "Angled contacts keep their unit size when sharing a hex");
                        } else {
                            assertTrue(new Vector3(Vector3.Y).rot(sensor.transform).len()
                                        < new Vector3(Vector3.Y).rot(alone).len(),
                                  "A top-view contact makes room for other markers inside its hex");
                        }
                        Vector3 center = new Vector3(0, 0, 0.5f).mul(sensor.transform);
                        assertEquals(position.x, center.x, 0.0001f);
                        assertEquals(position.y, center.y, 0.0001f);
                        assertTrue(UnitBounds.world(sensor).min.z < 2 * BoardGeometry.LEVEL,
                              "A sensor inside a building keeps its unit elevation, not the roof's");
                        for (var marker : markers.instances()) {
                            assertTrue(new Vector3(Vector3.Y).rot(sensor.transform).len()
                                  > new Vector3(Vector3.Y).rot(marker.transform).len(),
                                  "A contact must stay larger than both single and grouped location markers");
                            assertInsideHex(contact.location().coords(), marker);
                        }
                        assertInsideHex(contact.location().coords(), sensor);
                    }
                }
            }
        } finally {
            markers.dispose();
        }
    }

    /** Check the complete renderer, so placement must use the current unit pose rather than the previous frame. */
    private static void checkOccupiedHex(GpuBattleView view, BoardScene scene) throws Exception {
        var unitsField = GpuBattleView.class.getDeclaredField("unitInstances");
        unitsField.setAccessible(true);
        Map<?, ?> units = (Map<?, ?>) unitsField.get(view);
        ModelInstance unit = (ModelInstance) units.values().iterator().next();
        var markersField = GpuBattleView.class.getDeclaredField("markers");
        markersField.setAccessible(true);
        GpuMarkers markers = (GpuMarkers) markersField.get(view);
        assertEquals(scene.markers().size(), markers.instances().size());
        var unitBounds = UnitBounds.world(unit);
        for (var marker : markers.locatedInstances().entrySet()) {
            ModelInstance instance = marker.getValue();
            assertTrue(UnitBounds.world(instance).min.z
                        > Math.max(unitBounds.max.z, marker.getKey().elevation() * BoardGeometry.LEVEL),
                  "Warnings must clear the currently rendered sensor or Atlas, including its roof elevation");
            assertInsideHex(marker.getKey().coords(), instance);
        }
        if (scene.units().getFirst().sensorContact()) {
            Vector3 expected = BoardGeometry.center(scene.units().getFirst().location().coords(), 0);
            Vector3 actual = new Vector3(0, 0, 0.5f).mul(unit.transform);
            assertEquals(expected.x, actual.x, 0.0001f);
            assertEquals(expected.y, actual.y, 0.0001f);
        }
    }

    private static void assertInsideHex(Coords coords, ModelInstance instance) {
        var bounds = UnitBounds.local(instance);
        for (int corner = 0; corner < 8; corner++) {
            Vector3 point = new Vector3((corner & 1) == 0 ? bounds.min.x : bounds.max.x,
                  (corner & 2) == 0 ? bounds.min.y : bounds.max.y, (corner & 4) == 0 ? bounds.min.z : bounds.max.z)
                  .mul(instance.transform);
            assertTrue(BoardGeometry.contains(coords, point.x, point.y),
                  "Rendered markers must stay inside the hex they describe");
        }
    }

    @Test
    void rendersMarkersAndHonorsTheirOutlineSwitches() throws Exception {
        Board board = new Board(7, 6);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 6; y++) {
                board.setHex(new Coords(x, y), new Hex(0));
            }
        }
        Coords building = new Coords(2, 2), bridge = new Coords(4, 3);
        board.setHex(building, new Hex(1, "building:1;bldg_elev:4;bldg_cf:10", ""));
        board.setHex(bridge, new Hex(0, "water:2;bridge:1:9;bridge_elev:3;bridge_cf:10", ""));
        try (GpuBoardFixture fixture = GpuBoardFixture.create(board)) {
            SwingUtilities.invokeAndWait(() -> {
                Player enemy = new Player(1, "Unknown contact");
                enemy.setTeam(2);
                fixture.game.addPlayer(enemy.getId(), enemy);
                fixture.entity.setOwner(enemy);
                fixture.entity.setPosition(new Coords(3, 3));
                fixture.entity.addBeenDetectedBy(fixture.player);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                fixture.view.addSprites(List.of(new CollapseWarningSprite(fixture.view, building),
                      new CollapseWarningSprite(fixture.view, bridge)));
                fixture.source.refresh();
            });
            BoardScene scene = fixture.source.takeFrame().scene();
            assertTrue(scene.units().getFirst().sensorContact());
            assertTrue(scene.tile(building).features().stream().anyMatch(feature -> feature.kind() == BoardScene.FeatureKind.BUILDING),
                  "The fixture must contain an actual building model to exercise roof clearance");
            AtomicReference<Throwable> failure = new AtomicReference<>();
            new Lwjgl3Application(new GpuBattleView(fixture.source) {
                private int tick;

                @Override
                public void create() {
                    try {
                        super.create();
                        checkMarkers(scene);
                        boardCamera.setIsometric(true);
                    } catch (Throwable error) {
                        failure.set(error);
                        Gdx.app.exit();
                    }
                }

                @Override
                public void render() {
                    try {
                        super.render();
                        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                        tick++;
                        if (tick == 4) {
                            capture("markers-board-isometric.png");
                            boardCamera.setIsometric(false);
                            boardCamera.orbit(60, 0);
                            boardCamera.fit(scene);
                        } else if (tick == 8) {
                            capture("markers-board-top.png");
                            Gdx.app.exit();
                        }
                    } catch (Throwable error) {
                        failure.compareAndSet(null, error);
                        Gdx.app.exit();
                    }
                }
            }, GpuBoardWindow.configuration(false));
            assertNull(failure.get(), () -> String.valueOf(failure.get()));
        }
    }

    private void checkMarkers(BoardScene scene) {
        GpuMarkers markers = new GpuMarkers();
        GpuTerrain terrain = new GpuTerrain();
        GpuAssets assets = new GpuAssets();
        GpuAtmosphere atmosphere = new GpuAtmosphere();
        GpuUnitVisibility visibility = new GpuUnitVisibility();
        ModelBatch batch = new ModelBatch();
        List<Pixmap> captures = new ArrayList<>();
        try {
            BoardCamera camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            camera.setIsometric(true);
            camera.fit(scene);
            ModelInstance sensor = new ModelInstance(markers.model(BoardMarker.Kind.SENSOR_CONTACT).instance.model);
            sensor.userData = new Color();
            Color.rgb888ToColor((Color) sensor.userData, GpuMarkers.SENSOR_RGB);
            terrain.update(scene);
            markers.beginFrame(scene.markers(), 0.5f);
            markers.update(List.of(), camera.camera);
            for (var marker : scene.markers()) {
                ModelInstance coin = new ModelInstance(markers.model(marker.kind()).instance.model);
                markers.place(coin, camera.camera, BoardGeometry.center(marker.coords(), marker.elevation()));
                for (var feature : scene.tile(marker.coords()).features()) {
                    var bounds = assets.model(feature.asset()).calculateBoundingBox(new com.badlogic.gdx.math.collision.BoundingBox());
                    float roof = (scene.tile(marker.coords()).elevation() + feature.elevation()
                          + bounds.max.z * feature.height()) * BoardGeometry.LEVEL;
                    assertTrue(UnitBounds.world(coin).min.z > roof, "The whole coin must clear the actual roof/deck mesh");
                }
            }
            for (boolean top : new boolean[] { false, true }) {
                camera.setIsometric(!top);
                camera.orbit(60, 0);
                camera.fit(scene);
                markers.placeSensor(scene.units().getFirst().location().coords(), sensor, camera.camera,
                      BoardGeometry.center(scene.units().getFirst().location().coords(), 0));
                markers.update(List.of(sensor), camera.camera);
                draw(scene, terrain, markers, sensor, atmosphere, visibility, batch, camera, 0.75f, captures);
                capture(top ? "markers-top.png" : "markers-isometric.png");
            }

            // An opaque ridge hides both symbols. Each type's switch controls its see-through silhouette.
            BoardScene ridge = ridge();
            terrain.update(ridge);
            camera.setIsometric(true);
            camera.orbit(-45, 20);
            camera.fit(ridge);
            markers.beginFrame(ridge.markers(), 0);
            markers.placeSensor(new Coords(1, 2), sensor, camera.camera, BoardGeometry.center(new Coords(1, 2), 0));
            markers.update(List.of(sensor), camera.camera);
            Pixmap off = draw(ridge, terrain, markers, sensor, atmosphere, visibility, batch, camera, 0, captures);
            Pixmap on = draw(ridge, terrain, markers, sensor, atmosphere, visibility, batch, camera, 0.8f, captures);
            capture("markers-occluded.png");
            Vector3 sensorScreen = camera.camera.project(UnitBounds.world(sensor).getCenter(new Vector3()));
            Vector3 warningScreen = camera.camera.project(UnitBounds.world(markers.instances().iterator().next())
                  .getCenter(new Vector3()));
            long sensorDifference = difference(off, on, sensorScreen);
            long warningDifference = difference(off, on, warningScreen);
            assertTrue(GpuMarkers.outlineEnabled(BoardMarker.Kind.SENSOR_CONTACT)
                  ? sensorDifference > 10000 : sensorDifference == 0, "The sensor outline must follow its own switch");
            assertTrue(GpuMarkers.outlineEnabled(BoardMarker.Kind.COLLAPSE_WARNING)
                  ? warningDifference > 10000 : warningDifference == 0, "The collapse outline must follow its own switch");
            checkAllKinds(markers, terrain, atmosphere, visibility, batch, captures);
            markers.beginFrame(List.of(), 0);
            markers.update(List.of(), camera.camera);
            assertTrue(markers.instances().isEmpty(), "Removed warnings must leave no cached instances");
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
        } finally {
            captures.forEach(Pixmap::dispose);
            batch.dispose();
            visibility.dispose();
            atmosphere.dispose();
            assets.dispose();
            terrain.dispose();
            markers.dispose();
        }
    }

    private static Pixmap draw(BoardScene scene, GpuTerrain terrain, GpuMarkers markers, ModelInstance sensor,
          GpuAtmosphere atmosphere, GpuUnitVisibility visibility, ModelBatch batch, BoardCamera camera, float strength,
          List<Pixmap> captures) {
        List<ModelInstance> units = sensor == null ? List.of() : List.of(sensor);
        List<ModelInstance> objects = new ArrayList<>(units);
        objects.addAll(markers.instances());
        List<ModelInstance> outlined = new ArrayList<>(markers.outlinedInstances());
        if (sensor != null && GpuMarkers.outlineEnabled(BoardMarker.Kind.SENSOR_CONTACT)) {
            outlined.add(sensor);
        }
        terrain.setAtmosphere(atmosphere.lighting());
        terrain.animate(0, units, 1, 1);
        terrain.renderShadows(camera.camera, units);
        ScreenUtils.clear(0.03f, 0.05f, 0.07f, 1, true);
        atmosphere.begin((int) camera.camera.viewportWidth, (int) camera.camera.viewportHeight, 0, true);
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        objects.forEach(batch::render);
        batch.end();
        terrain.renderTransparent(camera.camera);
        atmosphere.end(camera.camera, terrain, objects, scene, 0);
        atmosphere.restoreDepth(camera.camera, terrain, objects);
        visibility.render(camera.camera, outlined, atmosphere.depthTexture(), 0, strength, 1);
        Pixmap result = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        captures.add(result);
        return result;
    }

    private static void checkAllKinds(GpuMarkers markers, GpuTerrain terrain, GpuAtmosphere atmosphere,
          GpuUnitVisibility visibility, ModelBatch batch, List<Pixmap> captures) {
        BoardScene.Pixels ground = ridge().tiles().getFirst().ground();
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 12; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), 0, -1, false, 0,
                      BoardScene.Surface.GRASS, ground, null, null, List.of(), List.of()));
            }
        }
        String[] labels = { "Sensor", "Collapse", "Minefield", "Charges", "Incoming", "Target", "Adjusted", "Auto hit",
              "Orbital", "Nuclear", "Objective", "Note", "Cargo", "Flare", "Saw clearing", "Bridge repair",
              "Bridge build", "Fortify", "Clear rubble", "Dig in" };
        List<BoardMarker> symbols = new ArrayList<>();
        for (var kind : BoardMarker.Kind.values()) {
            int index = kind.ordinal();
            symbols.add(new BoardMarker(kind, new Coords(1 + (index % 5) * 3, 1 + (index / 5) * 3),
                  0, kind.rgb(), labels[index]));
        }
        BoardScene gallery = new BoardScene(0, 15, 12, tiles, List.of(), List.of(), -1, "", List.of(), null,
              List.of(), List.of(), symbols);
        BoardCamera camera = new BoardCamera();
        camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        terrain.update(gallery);
        SpriteBatch text = new SpriteBatch();
        try {
            for (boolean top : new boolean[] { false, true }) {
                camera.setIsometric(!top);
                camera.fit(gallery);
                markers.beginFrame(symbols, 0);
                markers.update(List.of(), camera.camera);
                assertEquals(symbols.size(), markers.instances().size());
                assertEquals(symbols.stream().filter(marker -> GpuMarkers.outlineEnabled(marker.kind())).count(),
                      markers.outlinedInstances().size());
                for (var entry : markers.locatedInstances().entrySet()) {
                    assertEquals(entry.getKey().rgb(), Color.rgb888((Color) entry.getValue().userData));
                }
                draw(gallery, terrain, markers, null, atmosphere, visibility, batch, camera, 0.75f, captures);
                text.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, camera.camera.viewportWidth,
                      camera.camera.viewportHeight));
                text.begin();
                markers.renderLabels(text, camera.camera, 1, markers.labelObstacles(camera.camera));
                text.end();
                capture(top ? "markers-gallery-top.png" : "markers-gallery-isometric.png");
            }
        } finally {
            text.dispose();
        }
    }

    private static BoardScene ridge() {
        BufferedImage image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(111, 130, 76));
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        var pixels = new BoardScene.Pixels(image);
        List<BoardScene.Tile> tiles = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 6; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), y >= 3 ? 5 : 0, -1, false, 0,
                      BoardScene.Surface.GRASS, pixels, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 7, 6, tiles, List.of(), List.of(), -1, "", List.of(), null, List.of(), List.of(),
              List.of(new BoardMarker(BoardMarker.Kind.COLLAPSE_WARNING, new Coords(5, 2), 0)));
    }

    private static long difference(Pixmap before, Pixmap after, Vector3 center) {
        long difference = 0;
        for (int x = Math.max(0, (int) center.x - 70); x < Math.min(before.getWidth(), center.x + 70); x++) {
            for (int y = Math.max(0, (int) center.y - 70); y < Math.min(before.getHeight(), center.y + 70); y++) {
                int a = before.getPixel(x, y), b = after.getPixel(x, y);
                for (int shift = 8; shift <= 24; shift += 8) {
                    difference += Math.abs(((a >>> shift) & 255) - ((b >>> shift) & 255));
                }
            }
        }
        return difference;
    }

    private static void capture(String name) {
        File output = new File(System.getProperty("megamek.gpu.screenshots", "build/gpu-board-review"));
        assertTrue(output.isDirectory() || output.mkdirs());
        GpuBoardTestUi.capture(new File(output, name));
    }
}
