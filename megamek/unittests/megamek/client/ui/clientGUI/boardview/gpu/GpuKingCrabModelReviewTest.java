/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Every actual refit uses one bare body, with native loadout, pose, camouflage and damage rendering. */
@Tag("on-demand")
class GpuKingCrabModelReviewTest {
    private static final String ASSET = "units/modular/meks/king-crab.json";

    @Test
    void allKingCrabRefitsShareBodyAndKeepTheirWeaponsAndAnatomy() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override public void create() {
                var library = new GpuUnitModels();
                var damage = new UnitDamageDisplay();
                var camo = new GpuUnitCamouflage();
                try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
                    renderer.camera.viewportWidth = 120;
                    renderer.camera.viewportHeight = 90;
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    var sharedBody = library.modular("units/modular/bodies/king-crab.json");
                    assertNotNull(sharedBody);
                    assertTrue(sharedBody.triangles() <= 1500);
                    for (String location : List.of("HD", "CT", "LT", "RT", "LA", "RA", "LL", "RL")) {
                        assertTrue(sharedBody.model().getNode(location).parts.size > 0, location);
                    }
                    List<Path> files;
                    try (var paths = Files.walk(Path.of("../../mm-data/data/mekfiles/meks"))) {
                        files = paths.filter(path -> path.getFileName().toString().startsWith("King Crab ")
                              && path.toString().endsWith(".mtf")).sorted().toList();
                    }
                    assertFalse(files.isEmpty());
                    int id = 9800;
                    Entity stock = null;
                    List<String> counts = new ArrayList<>();
                    for (var file : files) {
                        var entity = new MekFileParser(file.toFile()).getEntity();
                        entity.setId(id++);
                        var selection = UnitModelSelection.capture(entity, -1, false, tileset);
                        assertEquals(ASSET, selection.asset(), entity.getModel());
                        var model = library.get(selection, entity.getId());
                        assertNotNull(model, entity.getModel());
                        assertSame(sharedBody, library.modular("units/modular/bodies/king-crab.json"));
                        var instance = new ModelInstance(model.instance.model);
                        var unit = unit(entity, selection);
                        model.place(instance, renderer.camera, center(unit), 0, unit);
                        for (var mount : selection.state().structure().equipment()) {
                            if (mount.policy() != EquipmentModelPolicy.WEAPON) { continue; }
                            var bindings = model.equipment().stream().filter(binding -> binding.index() == mount.index()).toList();
                            assertEquals(1, bindings.size(), entity.getModel() + " " + mount.internalName());
                            var binding = bindings.getFirst();
                            assertFalse(binding.embedded(), entity.getModel() + " " + mount.internalName());
                            assertFalse(binding.emitters().isEmpty());
                            if (mount.secondLocation().equals("LA") || mount.secondLocation().equals("RA")) {
                                assertTrue(binding.node().startsWith(mount.secondLocation() + "-equipment-"));
                            }
                            for (var emitter : binding.emitters()) {
                                var muzzle = new Vector3();
                                var direction = new Vector3();
                                UnitModelAttachment.emitter(instance, emitter, muzzle, direction);
                                assertTrue(Float.isFinite(muzzle.x) && Float.isFinite(muzzle.y) && muzzle.z > 0);
                                assertEquals(1, direction.len(), .0001f);
                            }
                        }
                        String name = "king-crab-" + entity.getModel();
                        renderer.frame(List.of(instance), center(unit), null, name, 0);
                        var bare = bare(library, selection, id++);
                        var bareInstance = new ModelInstance(bare.instance.model);
                        bare.place(bareInstance, renderer.camera, center(unit), 0, unit);
                        assertTrue(triangles(bareInstance) <= 1500);
                        counts.add(entity.getModel() + ": bare=" + triangles(bareInstance)
                              + ", equipment=" + (triangles(instance) - triangles(bareInstance))
                              + ", assembled=" + triangles(instance));
                        if (entity.getModel().equals("KGC-000")) {
                            stock = entity;
                            views(renderer, instance, unit, "king-crab-stock");
                            views(renderer, bareInstance, unit, "king-crab-bare");
                            details(renderer, damage, camo, model, unit);
                            movement(renderer, model, unit, EntityMovementType.MOVE_WALK, 3);
                            movement(renderer, model, unit, EntityMovementType.MOVE_RUN, 5);
                        } else if (entity.getModel().equals("KGC-008")) {
                            movement(renderer, model, unit, EntityMovementType.MOVE_JUMP, 3);
                        }
                    }
                    assertNotNull(stock);
                    customRefit(library, tileset, stock);
                    var atlas = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
                    atlas.setId(id++);
                    var atlasSelection = UnitModelSelection.capture(atlas, -1, false, tileset);
                    var atlasModel = bare(library, atlasSelection, id++);
                    var crabSelection = UnitModelSelection.capture(stock, -1, false, tileset);
                    var crabModel = bare(library, crabSelection, id);
                    var atlasInstance = new ModelInstance(atlasModel.instance.model);
                    var crabInstance = new ModelInstance(crabModel.instance.model);
                    atlasModel.place(atlasInstance, renderer.camera, new Vector3(-40, 0, 0), 0, unit(atlas, atlasSelection));
                    crabModel.place(crabInstance, renderer.camera, new Vector3(40, 0, 0), 0, unit(stock, crabSelection));
                    renderer.camera.viewportWidth = 210;
                    renderer.camera.viewportHeight = 157.5f;
                    renderer.frame(List.of(atlasInstance, crabInstance), Vector3.Zero, null, "king-crab-assault-lineup", 0);
                    Files.writeString(Path.of(System.getProperty("megamek.gpu.screenshots"), "king-crab-counts.txt"),
                          String.join(System.lineSeparator(), counts));
                    System.out.println("King Crab native review: " + files.size() + " refits\n" + String.join("\n", counts));
                } catch (Throwable error) { failure.set(error); }
                finally { camo.dispose(); damage.dispose(); library.dispose(); Gdx.app.exit(); }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("King Crab model review", failure.get()); }
    }

    private static GpuUnitModel bare(GpuUnitModels library, BoardScene.UnitModel selection, int id) {
        var structure = selection.state().structure();
        var state = new UnitModelState(new UnitModelState.Structure(structure.movement(), List.of(), List.of(), 0,
              false, structure.anatomy(), structure.bodyForm()), selection.state().appearance(), selection.state().pose());
        return library.get(new BoardScene.UnitModel(selection.asset(), selection.fallback(), selection.variant(),
              1, 0, BoardScene.LocationDamage.NONE, state), id);
    }

    private static void customRefit(GpuUnitModels library, MekTileset tileset, Entity entity) throws Exception {
        var added = entity.addEquipment(EquipmentType.get("ISMediumLaser"), Mek.LOC_RIGHT_ARM, true);
        var selection = UnitModelSelection.capture(entity, -1, false, tileset);
        var model = library.get(selection, entity.getId());
        var binding = model.equipment().stream().filter(item -> item.index() == added.getEquipmentNum()).findFirst().orElseThrow();
        var direction = new Vector3();
        UnitModelAttachment.emitter(model.instance, binding.emitters().getFirst(), new Vector3(), direction);
        assertTrue(direction.y < -.9f, "A custom rear gun clears the back of the claw, without a baked variant mesh");
        assertFalse(binding.embedded());
    }

    private static void views(GpuPlaybackReview.ReviewRenderer renderer, ModelInstance instance, BoardScene.Unit unit, String name) {
        var original = new Vector3(renderer.viewOffset);
        for (int view = 0; view < 4; view++) {
            renderer.topView = view == 3;
            renderer.viewOffset.set(view == 1 ? new Vector3(0, 200, 90)
                  : view == 2 ? new Vector3(210, 0, 70) : new Vector3(0, -200, 70));
            renderer.frame(List.of(instance), center(unit), null, name + "-views", view);
        }
        renderer.topView = false;
        renderer.viewOffset.set(original);
    }

    private static void details(GpuPlaybackReview.ReviewRenderer renderer, UnitDamageDisplay damage, GpuUnitCamouflage camo,
          GpuUnitModel model, BoardScene.Unit unit) {
        var instance = new ModelInstance(model.instance.model);
        var color = new UnitModelState.Camo("", "", 0, 100, 0x73895D, null, null);
        camo.apply(instance, model.instance, new UnitModelState.Appearance(Set.of(), false, color));
        model.place(instance, renderer.camera, center(unit), 0, unit);
        instance.getNode("CT").rotation.set(Vector3.Z, 60);
        instance.calculateTransforms();
        renderer.frame(List.of(instance), center(unit), null, "king-crab-twist", 0);
        for (int i = 0; i < 4; i++) {
            var stage = List.of(UnitDamageDisplay.Stage.ARMOR_WORN, UnitDamageDisplay.Stage.ARMOR_STRIPPED,
                  UnitDamageDisplay.Stage.STRUCTURE_BATTERED).get(Math.min(i, 2));
            var locations = new BoardScene.LocationDamage(i == 3 ? Set.of("LA", "HD") : Set.of(),
                  i == 3 ? Set.of("RT") : Set.of(), i == 3 ? Map.of() : Map.of("LT", stage));
            damage.apply(instance, model.instance, locations);
            renderer.frame(List.of(instance), center(unit), null, "king-crab-damage", i);
        }
    }

    private static void movement(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModel model, BoardScene.Unit unit,
          EntityMovementType mode, int speed) {
        var motion = new UnitMotion(unit.location());
        var to = new BoardScene.Waypoint(unit.location().coords().translated(0, 3), 0, 0);
        motion.append(List.of(unit.location(), to), mode, 0, false, speed);
        var animator = new UnitAnimator();
        var instance = new ModelInstance(model.instance.model);
        var jets = new GpuJumpJets();
        float dt = (float) motion.remainingSeconds() / 48;
        try {
            for (int frame = 0; frame <= 48; frame++) {
                motion.advance(frame == 0 ? 0 : dt, 1);
                animator.apply(model, instance, unit, motion.sample(), frame * dt, dt, false, 0);
                model.place(instance, renderer.camera, motion.position(), motion.facing(), unit);
                jets.beginFrame();
                jets.update("king-crab", model, instance, unit, motion.sample());
                assertTrue(instance.transform.isValid());
                renderer.frame(List.of(instance), motion.position(), null, jets, "king-crab-" + mode.name(), frame);
            }
        } finally { jets.dispose(); }
    }

    private static BoardScene.Unit unit(Entity entity, BoardScene.UnitModel selection) {
        return new BoardScene.Unit(entity.getId(), -1, entity.getShortName(), new BoardScene.Waypoint(new Coords(2, 2), 0, 0),
              null, false, null, 3, false, selection, 0);
    }

    private static Vector3 center(BoardScene.Unit unit) { return BoardGeometry.center(unit.location().coords(), 0); }

    private static int triangles(ModelInstance instance) {
        int total = 0;
        for (Node node : instance.nodes) { total += triangles(node); }
        return total;
    }

    private static int triangles(Node node) {
        int total = 0;
        for (var part : node.parts) { if (part.enabled) { total += part.meshPart.size / 3; } }
        for (var child : node.getChildren()) { total += triangles(child); }
        return total;
    }
}
