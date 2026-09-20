/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Dropship;
import megamek.common.units.EntityMovementType;

/** Native contact, visibility, scale, terrain-update and camera review using the real Union selection. */
final class GpuLandingSupportReview {
    private GpuLandingSupportReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) throws Exception {
        var original = BoardGeometry.tuning();
        try (var fixture = GpuBoardFixture.create(new Board(9, 9, Stream.generate(Hex::new).limit(81).toArray(Hex[]::new)))) {
            var ship = (Dropship) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
                  "dropships/TRO3057R/IS/Union (2708) (Cargo).blk").getEntity();
            SwingUtilities.invokeAndWait(() -> {
                ship.setId(8200);
                ship.setOwner(fixture.player);
                ship.setDeployed(true);
                fixture.game.addEntity(ship, false);
                ship.land();
                ship.setPosition(new Coords(4, 4));
            });
            BoardScene scene = terrain(fixture, ship, 0, null);
            var unit = scene.units().getFirst();
            assertEquals(BoardScene.AeroState.LANDED, unit.location().aeroState());
            var model = library.get(unit.model(), unit.id());
            assertNotNull(model);
            var placed = new ModelInstance(model.instance);
            var animator = new UnitAnimator();
            var camera = new BoardCamera();
            camera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            var terrain = new GpuTerrain();
            var picker = new UnitPicking();
            try {
                pose(model, placed, animator, scene, unit, camera, 0, UnitMotion.Sample.STILL);
                contacts(model, placed, scene);
                render(batch, terrain, camera, scene, placed, "flat");
                scene = terrain(fixture, ship, 0, ship.getPosition());
                unit = scene.units().getFirst();
                assertEquals(1, unit.location().elevation());
                pose(model, placed, animator, scene, unit, camera, 0, UnitMotion.Sample.STILL);
                contacts(model, placed, scene);
                // Every pad is outside the central level-1 hex in this authored Union fallback.
                for (var support : model.rigs().getFirst().landingSupports()) {
                    assertEquals(0, contact(placed, support).z, .002f);
                }
                render(batch, terrain, camera, scene, placed, "center-high");
                for (boolean top : List.of(false, true)) {
                    camera.setIsometric(!top);
                    camera.fit(scene);
                    camera.center(BoardGeometry.center(unit.location().coords(), 1));
                    var target = UnitBounds.world(placed).getCenter(new Vector3());
                    var ray = new Ray(new Vector3(target).mulAdd(camera.camera.direction, -1000), camera.camera.direction);
                    assertTrue(Float.isFinite(picker.distance(placed, ray)), "Both cameras pick the extended model");
                }
                var foot = contact(placed, model.rigs().getFirst().landingSupports().getFirst());
                Coords underFoot = scene.tiles().stream().filter(tile -> BoardGeometry.contains(tile.coords(), foot.x, foot.y))
                      .findFirst().orElseThrow().coords();
                scene = terrain(fixture, ship, 0, underFoot);
                unit = scene.units().getFirst();
                pose(model, placed, animator, scene, unit, camera, 0, UnitMotion.Sample.STILL);
                contacts(model, placed, scene);
                var heights = model.rigs().getFirst().landingSupports().stream().map(support -> Math.round(contact(placed, support).z))
                      .collect(java.util.stream.Collectors.toSet());
                assertEquals(Set.of(0, Math.round(BoardGeometry.LEVEL)), heights, "Each foot samples its own hex");
                render(batch, terrain, camera, scene, placed, "mixed-feet");
                // A downward ray at a pad outside the hull must hit its current mesh, not just the hull bounds.
                Vector3 pad = contact(placed, model.rigs().getFirst().landingSupports().getFirst());
                assertTrue(Float.isFinite(picker.distance(placed, new Ray(new Vector3(pad).add(0, 0, 5), new Vector3(0, 0, -1)))));
                for (float scale : new float[] { .7f, .85f, 1.05f }) {
                    BoardGeometry.tune(new BoardGeometry.Tuning(original.hexScale() * 1.35f, original.unitScale(),
                          original.unitHeightScale(), original.levelHeight(), original.gridShade(), scale));
                    for (float facing : new float[] { 0, 60, 137 }) {
                        pose(model, placed, animator, scene, unit, camera, facing, UnitMotion.Sample.STILL);
                        contacts(model, placed, scene);
                    }
                }
                BoardGeometry.tune(original);
                // A negative world height is still landed. Terrain edits must replace, not accumulate, reach.
                scene = terrain(fixture, ship, -2, underFoot);
                unit = scene.units().getFirst();
                pose(model, placed, animator, scene, unit, camera, 0, UnitMotion.Sample.STILL);
                contacts(model, placed, scene);
                assertEquals(-1, unit.location().elevation());
                scene = terrain(fixture, ship, 0, underFoot);
                unit = scene.units().getFirst();
                pose(model, placed, animator, scene, unit, camera, 0, UnitMotion.Sample.STILL);
                contacts(model, placed, scene);
                terrain.update(scene);
                terrain.renderShadows(camera.camera, List.of(placed));
                long deployedShadow = GpuFamilyAssemblyReview.shadowChecksum(terrain);
                lifecycle(model, placed, animator, scene, unit, camera);
                // Shadow invalidation must see disabled parts even with an unchanged hull/root transform.
                terrain.renderShadows(camera.camera, List.of(placed));
                assertTrue(deployedShadow != GpuFamilyAssemblyReview.shadowChecksum(terrain));
                render(batch, terrain, camera, scene, placed, "retracted");
                gearCycle(model, placed, animator, batch, terrain, camera, scene, unit);
                // Reveal/rebind starts from shared rest art and must immediately restore local contacts.
                var revealed = new ModelInstance(model.instance);
                pose(model, revealed, animator, scene, unit, camera, 0, UnitMotion.Sample.STILL);
                contacts(model, revealed, scene);
                revealed.transform.setTranslation(-BoardGeometry.WIDTH * 20, BoardGeometry.HEIGHT * 20, 0);
                assertTrue(animator.groundSupports(scene, unit, UnitMotion.Sample.STILL));
                hidden(model, revealed);
                assertTrue(UnitBounds.world(revealed).isValid(), "Off-board contacts cannot create invalid bounds");
                pose(model, revealed, animator, scene, unit, camera, 0, UnitMotion.Sample.STILL);
                contacts(model, revealed, scene);
                for (var support : model.rigs().getFirst().landingSupports()) {
                    assertEquals(1, model.instance.getNode(support.shaft()).scale.z, .0001f, "Shared geometry is immutable");
                }
                fallback(library, batch, terrain, camera, scene, unit, "aerodyne");
                fallback(library, batch, terrain, camera, scene, unit, "small-spheroid");
                assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            } finally {
                picker.clear();
                terrain.dispose();
            }
        } finally {
            BoardGeometry.tune(original);
        }
    }

    private static BoardScene terrain(GpuBoardFixture fixture, Dropship ship, int level, Coords raised) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ship.getOccupiedCoords().forEach(coords -> fixture.game.getBoard().setHex(coords, new Hex(level)));
            if (raised != null) {
                fixture.game.getBoard().setHex(raised, new Hex(level + 1));
            }
            fixture.source.refresh();
        });
        var captured = fixture.source.takeFrame().scene();
        var unit = captured.units().stream().filter(item -> item.id() == ship.getId()).findFirst().orElseThrow();
        return new BoardScene(captured.boardId(), captured.width(), captured.height(), captured.tiles(), List.of(unit),
              List.of(), unit.id(), "Landing support review", List.of(), new BoardScene.Light(1, -.6f));
    }

    private static void pose(GpuMeeple model, ModelInstance placed, UnitAnimator animator, BoardScene scene,
          BoardScene.Unit unit, BoardCamera camera, float facing, UnitMotion.Sample sample) {
        animator.apply(model, placed, unit, sample, 0, 0, true, 0);
        var placement = sample.placement(unit);
        model.place(placed, camera.camera, BoardGeometry.center(unit.location().coords(), unit.location().elevation()), facing, placement);
        var hull = new Matrix4(placed.getNode("hull").globalTransform);
        var root = new Matrix4(placed.transform);
        assertTrue(animator.groundSupports(scene, placement, sample));
        assertTrue(java.util.Arrays.equals(hull.val, placed.getNode("hull").globalTransform.val), "Supports never move the hull");
        assertTrue(java.util.Arrays.equals(root.val, placed.transform.val), "Extensions never feed back into fitting");
        var exact = placed.calculateBoundingBox(new BoundingBox()).mul(placed.transform);
        var approximate = UnitBounds.world(placed);
        approximate.set(new Vector3(approximate.min).sub(.002f, .002f, .002f), new Vector3(approximate.max).add(.002f, .002f, .002f));
        assertTrue(approximate.contains(exact), "Bounds contain the extended geometry");
    }

    private static Vector3 contact(ModelInstance placed, UnitModelDescriptor.LandingSupport support) {
        return UnitModelDescriptor.vector(support.contact()).mul(placed.getNode(support.foot()).globalTransform).mul(placed.transform);
    }

    private static void contacts(GpuMeeple model, ModelInstance placed, BoardScene scene) {
        for (var support : model.rigs().getFirst().landingSupports()) {
            Node foot = placed.getNode(support.foot()), shaft = placed.getNode(support.shaft());
            assertTrue(foot.parts.first().enabled);
            var point = contact(placed, support);
            assertEquals(UnitLandingSupports.ground(scene, point.x, point.y), point.z, .002f, support.id());
            assertTrue(foot.scale.epsilonEquals(1, 1, 1, .0001f), "A foot cannot be stretched");
            assertEquals(1, shaft.scale.x, .0001f, "Shaft thickness is constant");
            assertEquals(1, shaft.scale.y, .0001f);
            Vector3 end = new Vector3(0, 0, -support.length()).mul(shaft.globalTransform).mul(placed.transform);
            Vector3 pivot = new Vector3().mul(foot.globalTransform).mul(placed.transform);
            assertTrue(end.epsilonEquals(pivot, .002f), "The extended shaft stays connected to the pad");
        }
    }

    private static void lifecycle(GpuMeeple model, ModelInstance placed, UnitAnimator animator, BoardScene scene,
          BoardScene.Unit unit, BoardCamera camera) {
        var landing = unit.location();
        var flight = new BoardScene.Waypoint(landing.coords(), 5, 0).withAeroState(BoardScene.AeroState.AIRBORNE);
        var motion = new UnitMotion(flight);
        motion.append(List.of(flight, landing), EntityMovementType.MOVE_SAFE_THRUST, 0);
        motion.advance(motion.remainingSeconds() - UnitMotion.LANDING_GEAR_SECONDS - .01, 1);
        pose(model, placed, animator, scene, unit, camera, 0, motion.sample());
        hidden(model, placed);
        motion.finish();
        pose(model, placed, animator, scene, unit, camera, 0, motion.sample());
        contacts(model, placed, scene);
        // A building/non-Aero location cannot trigger terrain supports through an accidentally reused mesh.
        var building = withState(unit, null);
        assertFalse(animator.groundSupports(scene, building, UnitMotion.Sample.STILL));
        for (var state : List.of(BoardScene.AeroState.ELEVATED, BoardScene.AeroState.AIRBORNE)) {
            pose(model, placed, animator, scene, withState(unit, state), camera, 0, UnitMotion.Sample.STILL);
            hidden(model, placed);
        }
    }

    private static BoardScene.Unit withState(BoardScene.Unit unit, BoardScene.AeroState state) {
        return new BoardScene.Unit(unit.id(), unit.part(), unit.name(), unit.location().withAeroState(state), unit.image(), false,
              unit.annotations(), unit.height(), state == BoardScene.AeroState.AIRBORNE, unit.model(), unit.outlineRgb(), unit.footprint());
    }

    private static void hidden(GpuMeeple model, ModelInstance placed) {
        for (var support : model.rigs().getFirst().landingSupports()) {
            hidden(placed.getNode(support.node()));
            assertEquals(1, placed.getNode(support.shaft()).scale.z, .0001f, "Flight clears old reach");
        }
    }

    private static void hidden(Node node) {
        node.parts.forEach(part -> assertFalse(part.enabled, "No support geometry protrudes in flight"));
        node.getChildren().forEach(GpuLandingSupportReview::hidden);
    }

    /** A close view of the real grounded gear phases, with flight travel omitted between the two shots. */
    private static void gearCycle(GpuMeeple model, ModelInstance placed, UnitAnimator animator, ModelBatch batch,
          GpuTerrain terrain, BoardCamera camera, BoardScene scene, BoardScene.Unit unit) {
        var landing = unit.location().withFootprint(unit.footprint());
        var flight = new BoardScene.Waypoint(landing.coords(), 5, 0).withAeroState(BoardScene.AeroState.AIRBORNE);
        var first = model.rigs().getFirst().landingSupports().getFirst();
        camera.setIsometric(true);
        camera.fit(scene);
        camera.zoom(.62f);
        camera.center(BoardGeometry.center(landing.coords(), 2));
        for (boolean retract : List.of(true, false)) {
            var motion = new UnitMotion(retract ? landing : flight);
            motion.append(retract ? List.of(landing, flight) : List.of(flight, landing), EntityMovementType.MOVE_SAFE_THRUST, 0);
            if (!retract) {
                motion.advance(motion.remainingSeconds() - UnitMotion.LANDING_GEAR_SECONDS, 1);
            }
            float previousHeight = retract ? -Float.MAX_VALUE : Float.MAX_VALUE;
            for (int frame = 0; frame <= 20; frame++) {
                if (frame > 0) {
                    motion.advance(UnitMotion.LANDING_GEAR_SECONDS / 20, 1);
                }
                pose(model, placed, animator, scene, unit, camera, 0, motion.sample());
                float height = contact(placed, first).z;
                assertTrue(retract ? height >= previousHeight - .002f : height <= previousHeight + .002f,
                      "Feet move continuously between the ground and the hull");
                previousHeight = height;
                if (frame == 10) {
                    assertTrue(placed.getNode(first.foot()).parts.first().enabled, "Partial motion is visible");
                    assertTrue(placed.getNode(first.foot()).scale.epsilonEquals(1, 1, 1, .001f));
                }
                draw(batch, terrain, camera, placed);
                GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"),
                      "runtime-gear-" + (retract ? "retract-" : "extend-") + String.format("%02d", frame) + ".png"));
            }
            if (retract) {
                hidden(model, placed);
                stowedInsideHull(model, placed, batch, terrain, camera);
                camera.setIsometric(true);
            } else {
                contacts(model, placed, scene);
            }
        }
    }

    private static void stowedInsideHull(GpuMeeple model, ModelInstance placed, ModelBatch batch, GpuTerrain terrain,
          BoardCamera camera) {
        for (boolean top : List.of(false, true)) {
            camera.setIsometric(!top);
            draw(batch, terrain, camera, placed);
            long hidden = imageChecksum();
            for (var support : model.rigs().getFirst().landingSupports()) {
                visibility(placed.getNode(support.node()), true);
            }
            draw(batch, terrain, camera, placed);
            assertEquals(hidden, imageChecksum(), "Fully stowed supports are already occluded by the hull before hiding");
            for (var support : model.rigs().getFirst().landingSupports()) {
                visibility(placed.getNode(support.node()), false);
            }
        }
    }

    private static void visibility(Node node, boolean visible) {
        node.parts.forEach(part -> part.enabled = visible);
        node.getChildren().forEach(child -> visibility(child, visible));
    }

    private static long imageChecksum() {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            var checksum = new java.util.zip.CRC32();
            checksum.update(pixels.getPixels());
            return checksum.getValue();
        } finally {
            pixels.dispose();
        }
    }

    private static void fallback(GpuUnitModels library, ModelBatch batch, GpuTerrain terrain, BoardCamera camera,
          BoardScene scene, BoardScene.Unit unit, String name) {
        var selected = new BoardScene.UnitModel("units/modular/families/" + name + ".json", null, "", 1, 0,
              BoardScene.LocationDamage.NONE, unit.model().state());
        var model = library.get(selected, name.hashCode());
        assertNotNull(model);
        var placed = new ModelInstance(model.instance);
        pose(model, placed, new UnitAnimator(), scene, unit, camera, 0, UnitMotion.Sample.STILL);
        contacts(model, placed, scene);
        render(batch, terrain, camera, scene, placed, name);
        pose(model, placed, new UnitAnimator(), scene, withState(unit, BoardScene.AeroState.AIRBORNE), camera, 0,
              UnitMotion.Sample.STILL);
        stowedInsideHull(model, placed, batch, terrain, camera);
    }

    private static void render(ModelBatch batch, GpuTerrain terrain, BoardCamera camera, BoardScene scene,
          ModelInstance placed, String name) {
        terrain.update(scene);
        for (boolean top : List.of(false, true)) {
            camera.setIsometric(!top);
            camera.fit(scene);
            camera.zoom(.62f);
            camera.center(BoardGeometry.center(scene.units().getFirst().location().coords(), 2));
            draw(batch, terrain, camera, placed);
            GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"),
                  "runtime-landing-supports-" + name + (top ? "-top.png" : "-isometric.png")));
        }
    }

    private static void draw(ModelBatch batch, GpuTerrain terrain, BoardCamera camera, ModelInstance placed) {
        terrain.renderShadows(camera.camera, List.of(placed));
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(.12f, .16f, .2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        terrain.render(camera.camera, false);
        batch.begin(camera.camera);
        batch.render(placed, terrain.environment());
        batch.end();
    }

}
