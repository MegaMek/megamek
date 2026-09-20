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
                    renderer.viewOffset.set(95, 150, 105);
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    var sharedBody = library.modular("units/modular/bodies/king-crab.json");
                    assertNotNull(sharedBody);
                    assertTrue(sharedBody.triangles() <= 1500);
                    assertEquals(Map.of("leftLeg", "reverse", "rightLeg", "reverse"), sharedBody.descriptor().legBends());
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
                        var paint = new UnitModelState.Camo("", "", 0, 100, 0x94A787, null, null);
                        camo.apply(instance, model.instance, new UnitModelState.Appearance(Set.of(), false, paint));
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
                            firing(renderer, library, model, entity, unit);
                            movement(renderer, model, unit, EntityMovementType.MOVE_WALK, 3);
                            movement(renderer, model, unit, EntityMovementType.MOVE_RUN, 5);
                        } else if (entity.getModel().equals("KGC-008")) {
                            movement(renderer, model, unit, EntityMovementType.MOVE_JUMP, 3);
                        }
                    }
                    assertNotNull(stock);
                    customRefit(library, tileset);
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

    private static void customRefit(GpuUnitModels library, MekTileset tileset) throws Exception {
        var entity = new megamek.common.units.BipedMek();
        entity.setId(9899);
        entity.setChassis("King Crab");
        entity.setModel("Custom refit");
        entity.setWeight(100);
        for (int location = 0; location < entity.locations(); location++) { entity.initializeInternal(10, location); }
        entity.addEquipment(EquipmentType.get("ISAC2"), Mek.LOC_RIGHT_ARM);
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
            renderer.viewOffset.set(view == 1 ? new Vector3(0, -200, 90)
                  : view == 2 ? new Vector3(210, 0, 70) : new Vector3(0, 200, 70));
            renderer.frame(List.of(instance), center(unit), null, name + "-views", view);
        }
        renderer.topView = false;
        renderer.viewOffset.set(original);
    }

    private static void details(GpuPlaybackReview.ReviewRenderer renderer, UnitDamageDisplay damage, GpuUnitCamouflage camo,
          GpuUnitModel model, BoardScene.Unit unit) throws Exception {
        var instance = new ModelInstance(model.instance.model);
        var pixels = new BoardScene.Pixels(javax.imageio.ImageIO.read(new File(Configuration.camoDir(),
              "Word of Blake/TerraSec (Camo).png")));
        var color = new UnitModelState.Camo("Word of Blake", "TerraSec (Camo).png", 0, 10, 0xFFFFFF, pixels, null);
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
            UnitDamageDisplay.show(instance, locations);
            damage.applyTexture(instance, locations, unit.id());
            renderer.frame(List.of(instance), center(unit), null, "king-crab-damage", i);
        }
    }

    private static void movement(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModel model, BoardScene.Unit unit,
          EntityMovementType mode, int speed) {
        var motion = new UnitMotion(unit.location());
        var to = new BoardScene.Waypoint(unit.location().coords().translated(0, 3), 0, 0);
        motion.append(List.of(unit.location(), to), mode, mode == EntityMovementType.MOVE_JUMP ? 3 : 0, false, speed);
        var animator = new UnitAnimator();
        var instance = new ModelInstance(model.instance.model);
        var jets = new GpuJumpJets();
        var originalView = renderer.viewOffset.cpy();
        float dt = (float) motion.remainingSeconds() / 48;
        try {
            for (int frame = 0; frame <= 48; frame++) {
                motion.advance(frame == 0 ? 0 : dt, 1);
                animator.apply(model, instance, unit, motion.sample(), frame * dt, dt, false, 0);
                model.place(instance, renderer.camera, motion.position(), motion.facing(), unit);
                jets.beginFrame();
                jets.update("king-crab", model, instance, unit, motion.sample());
                assertTrue(instance.calculateBoundingBox(new com.badlogic.gdx.math.collision.BoundingBox()).isValid());
                for (String leg : List.of("LL", "RL")) {
                    var hip = instance.getNode(leg).globalTransform.getTranslation(new Vector3());
                    var knee = instance.getNode(leg + "-shin").globalTransform.getTranslation(new Vector3());
                    var ankle = instance.getNode(leg + "-foot").globalTransform.getTranslation(new Vector3());
                    var axis = ankle.sub(hip);
                    var projected = hip.cpy().mulAdd(axis, knee.cpy().sub(hip).dot(axis) / axis.len2());
                    assertTrue(knee.y < projected.y, mode + " frame " + frame + ": reverse knee crossed its leg axis");
                }
                renderer.viewOffset.set(originalView);
                renderer.frame(List.of(instance), motion.position(), null, jets, "king-crab-" + mode.name(), frame);
                renderer.viewOffset.set(180, 0, 43);
                renderer.frame(List.of(instance), motion.position(), null, jets, "king-crab-" + mode.name() + "-side", frame);
            }
        } finally {
            renderer.viewOffset.set(originalView);
            jets.dispose();
        }
    }

    private static void firing(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModels library, GpuUnitModel model,
          Entity entity, BoardScene.Unit source) {
        var victim = new BoardScene.Unit(9898, -1, "Target", new BoardScene.Waypoint(new Coords(2, 0), 0, 3),
              null, false, null, 3, false, source.model(), 0);
        var attacker = new ModelInstance(model.instance.model);
        var target = new ModelInstance(model.instance.model);
        model.place(target, renderer.camera, center(victim), 180, victim);
        var volley = new UnitVolley();
        var shots = new ArrayList<UnitAttack>();
        for (var weapon : entity.getWeaponList()) {
            var profile = megamek.common.ResolvedAttack.Shot.capture(weapon);
            if (profile.missiles() > 0) { profile = profile.withResolution(null, 10); }
            var result = new megamek.common.ResolvedAttack(new java.util.UUID(8, weapon.getEquipmentNum()),
                  megamek.common.ResolvedAttack.Kind.SHOT,
                  new megamek.common.units.UnitLocation(source.id(), source.location().coords(), 0, 0, 0),
                  new megamek.common.units.UnitLocation(victim.id(), victim.location().coords(), 3, 0, 0),
                  megamek.common.units.Targetable.TYPE_ENTITY, weapon.getEquipmentNum(), weapon.getType().getInternalName(),
                  weapon.getLocation(), true, List.of(new megamek.common.ResolvedAttack.Mount(source.id(), weapon.getEquipmentNum())),
                  profile).withImpacts(List.of(new megamek.common.ResolvedAttack.Impact("CT", false, profile.missiles() > 0 ? 10 : 1)));
            var shot = new UnitAttack(new BoardScene.Combat(result, source, victim, victim.location()));
            shots.add(shot);
            volley.add(shot, 0);
        }
        var animator = new UnitAnimator();
        var effects = new GpuAttackEffects();
        float width = renderer.camera.viewportWidth, height = renderer.camera.viewportHeight;
        renderer.camera.viewportWidth = 285;
        renderer.camera.viewportHeight = 213.75f;
        int launched = 0;
        float duration = (float) shots.stream().mapToDouble(shot -> shot.delay + shot.duration).max().orElseThrow();
        try {
            for (int frame = 0; frame <= 48; frame++) {
                float time = duration * frame / 48;
                volley.advance(time);
                shots.forEach(shot -> shot.seconds = time - shot.delay);
                animator.apply(model, attacker, source, UnitMotion.Sample.STILL, time, duration / 48, true, 0);
                for (var shot : shots) { animator.attack(model, source, shot); }
                model.place(attacker, renderer.camera, center(source), 0, source);
                animator.aimShots(model, source, shots, shot -> target);
                effects.update(shots, library, Map.of(source.id() + ":-1", attacker, victim.id() + ":-1", target));
                renderer.frame(List.of(attacker, target), center(source).lerp(center(victim), .5f), effects, "king-crab-volley", frame);
                launched = Math.max(launched, effects.missileCount());
            }
            assertEquals(15, launched, "The dorsal stock LRM15 launches all missiles alongside the claw cannons");
        } finally {
            renderer.camera.viewportWidth = width;
            renderer.camera.viewportHeight = height;
            effects.dispose();
        }
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
