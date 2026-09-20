/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;

/** Runs actual family rigs through the production clock, placement and contact evaluator in the native review. */
final class GpuFamilyMotionReview {
    private GpuFamilyMotionReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) {
        int id = 9800;
        var scene = ramp();
        var camera = new OrthographicCamera();
        var gallery = new ArrayList<ModelInstance>();
        var terrain = terrain(scene);
        try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
        for (String family : List.of("tracked", "wheeled", "hover", "wige", "rail", "vtol", "airship", "fighter",
              "aerodyne", "spheroid", "small-spheroid", "jumpship", "warship", "station", "naval", "hydrofoil",
              "submarine", "proto", "quad-proto", "glider-proto", "emplacement", "structure", "escape-pod", "missile")) {
            var selection = GpuFamilyAssemblyReview.selection(family, List.of());
            var model = library.get(selection, id++);
            assertNotNull(model, family);
            var instance = new ModelInstance(model.instance.model);
            var animator = new UnitAnimator();
            boolean airborne = List.of("wige", "vtol", "airship", "fighter", "aerodyne", "spheroid", "small-spheroid",
                  "jumpship", "warship", "station", "glider-proto", "missile").contains(family);
            var start = new BoardScene.Waypoint(new Coords(2, 4), 0, 0);
            var end = new BoardScene.Waypoint(new Coords(2, 0), 1, 0);
            var unit = new BoardScene.Unit(id, -1, family, end, null, false, null, 1, airborne, selection, 0);
            var motion = new UnitMotion(start);
            motion.append(List.of(start, new BoardScene.Waypoint(new Coords(2, 2), 1, 0), end),
                  EntityMovementType.MOVE_WALK, 0, false, 4);
            float seconds = (float) (motion.remainingSeconds() / 120);
            boolean contact = false;
            for (int frame = 0; frame <= 120; frame++) {
                motion.advance(frame == 0 ? 0 : seconds, 1);
                animator.apply(model, instance, unit, motion.sample(), frame * seconds, seconds, false, 0);
                model.place(instance, camera, airborne ? motion.position() : motion.surfacePosition(scene), motion.facing(), unit);
                contact |= animator.groundSupports(scene, unit, motion.sample());
                var bounds = UnitBounds.world(instance);
                assertTrue(bounds.isValid() && Float.isFinite(bounds.min.len2() + bounds.max.len2()), family + " finite posed bounds");
                if (frame == 52 && List.of("tracked", "wheeled", "hover", "proto").contains(family)) {
                    var pose = new ModelInstance(instance);
                    pose.transform.setToTranslation(gallery.size() * 80 - 120, 0, 0);
                    gallery.add(pose);
                }
                if (frame % 12 == 0) {
                    for (boolean top : List.of(false, true)) {
                        renderer.topView = top;
                        renderer.frame(List.of(terrain, instance), instance.transform.getTranslation(new Vector3()), null,
                              "family-" + family + (top ? "-top" : "-iso"), frame);
                    }
                }
            }
            if (List.of("tracked", "wheeled", "hover", "rail", "proto", "quad-proto").contains(family)) {
                assertTrue(contact, family + " uses the rendered ramp contact plane");
            } else if (airborne || List.of("naval", "hydrofoil", "submarine", "emplacement", "structure").contains(family)) {
                assertFalse(contact, family + " must not acquire walking ground contact");
            }
            // A stray Mek-only observation on a non-Mek fixture cannot make an aircraft, tank or ship fall over.
            var forced = new UnitMotion.Sample(false, EntityMovementType.MOVE_NONE, 1, 0, 0, 1, 0,
                  Float.POSITIVE_INFINITY, ProneCause.FORCED);
            animator.apply(model, instance, unit, forced, 0, 0, true, 60);
            for (var rig : model.rigs()) {
                assertFalse(rig.mek(), family);
                var container = rig.container() == null ? instance.nodes : instance.getNode(rig.container()).getChildren();
                assertTrue(UnitAnimator.find(container, rig.joints().get("root")).rotation.isIdentity(), family + " ignores Mek-only poses");
            }
        }
        conversions(library, renderer);
        } finally { terrain.model.dispose(); }
        GpuModularUnitModelsSmokeTest.renderReview(batch, gallery, "runtime-family-ramp-contact", 350, 12);
    }

    static ModelInstance terrain(BoardScene scene) {
        var builder = new com.badlogic.gdx.graphics.g3d.utils.ModelBuilder();
        builder.begin();
        var part = builder.part("ramp", com.badlogic.gdx.graphics.GL20.GL_TRIANGLES,
              com.badlogic.gdx.graphics.VertexAttributes.Usage.Position | com.badlogic.gdx.graphics.VertexAttributes.Usage.Normal,
              new com.badlogic.gdx.graphics.g3d.Material(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(.32f, .4f, .3f, 1)));
        for (var tile : scene.tiles()) {
            for (var face : new BoardSurface(scene, tile).faces) {
                var normal = face.b().cpy().sub(face.a()).crs(face.c().cpy().sub(face.a())).nor();
                part.triangle(new com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo().setPos(face.a()).setNor(normal),
                      new com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo().setPos(face.b()).setNor(normal),
                      new com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo().setPos(face.c()).setNor(normal));
            }
        }
        return new ModelInstance(builder.end());
    }

    private static void conversions(GpuUnitModels library, GpuPlaybackReview.ReviewRenderer renderer) {
        try {
            var tileset = new megamek.client.ui.tileset.MekTileset(megamek.common.Configuration.unitImagesDir());
            tileset.loadFromFile("mekset.txt");
            int id = 9850;
            for (megamek.common.units.Mek entity : List.of(
                  new megamek.common.units.LandAirMek(0, 0, megamek.common.units.LandAirMek.LAM_STANDARD),
                  new megamek.common.units.QuadVee())) {
                entity.setId(id++);
                entity.setWeight(50);
                for (int loc = 0; loc < entity.locations(); loc++) { entity.initializeInternal(10, loc); }
                var before = convertedUnit(entity, tileset);
                entity.setConversionMode(entity instanceof megamek.common.units.LandAirMek
                      ? megamek.common.units.LandAirMek.CONV_MODE_FIGHTER : megamek.common.units.QuadVee.CONV_MODE_VEHICLE);
                var after = convertedUnit(entity, tileset);
                for (boolean reverse : List.of(false, true)) {
                    var transition = new UnitConversion(new BoardScene.Conversion(0, reverse ? after : before, reverse ? before : after));
                    var animator = new UnitAnimator();
                    for (int frame = 0; frame <= 24; frame++) {
                        transition.seconds = UnitConversion.DURATION_SECONDS * frame / 24;
                        var unit = transition.displayed();
                        var model = library.get(unit.model(), id);
                        var instance = new ModelInstance(model.instance.model);
                        animator.apply(model, instance, unit, UnitMotion.Sample.STILL, transition.seconds, 1f / 30, true, 0);
                        animator.conversion(transition, unit);
                        var origin = BoardGeometry.center(unit.location().coords(), 0);
                        model.place(instance, renderer.camera, origin, 0, unit);
                        assertTrue(UnitBounds.world(instance).isValid(), "Conversion bounds");
                        for (boolean top : List.of(false, true)) {
                            renderer.topView = top;
                            renderer.frame(List.of(instance), origin, null,
                                  "conversion-" + entity.getClass().getSimpleName() + (reverse ? "-reverse" : "-forward")
                                        + (top ? "-top" : "-iso"), frame);
                        }
                    }
                }
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    private static BoardScene.Unit convertedUnit(megamek.common.units.Entity entity, megamek.client.ui.tileset.MekTileset tileset) {
        return new BoardScene.Unit(entity.getId(), -1, entity.getClass().getSimpleName(), new BoardScene.Waypoint(new Coords(2, 3), 0, 0),
              null, false, null, 2, false, UnitModelSelection.capture(entity, -1, false, tileset), 0);
    }

    static BoardScene ramp() {
        var tiles = new ArrayList<BoardScene.Tile>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 6; y++) {
                tiles.add(new BoardScene.Tile(new Coords(x, y), y <= 2 ? 1 : 0, -1, false, 9,
                      BoardScene.Surface.GRASS, null, null, null, List.of(), List.of()));
            }
        }
        return new BoardScene(0, 5, 6, tiles, List.of(), List.of(), -1, "", List.of());
    }
}
