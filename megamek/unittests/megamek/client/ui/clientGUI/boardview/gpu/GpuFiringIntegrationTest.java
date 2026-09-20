/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.common.Configuration;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real rigs, weapon assets, clocks and GPU effects at the boundaries missed by single-weapon demos. */
@Tag("on-demand")
class GpuFiringIntegrationTest {
    @Test
    void sharedJointsTargetPassesRoundLaunchesAndMeshFreeEffectsStayConsistent() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override public void create() {
                var library = new GpuUnitModels();
                try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
                    renderer.camera.viewportWidth = 520;
                    renderer.camera.viewportHeight = 390;
                    renderer.viewOffset.set(20, -210, 240);
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    targetPasses(library, renderer, tileset, false);
                    targetPasses(library, renderer, tileset, true);
                    secondTurret(library, renderer);
                    roundsAndFallback(library, renderer, tileset);
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) { failure.set(error); }
                finally { library.dispose(); Gdx.app.exit(); }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Firing integration", failure.get()); }
    }

    private static void targetPasses(GpuUnitModels library, GpuPlaybackReview.ReviewRenderer renderer,
          MekTileset tileset, boolean turret) throws Exception {
        megamek.common.units.Entity atlas;
        if (turret) {
            var tank = new megamek.common.units.Tank();
            tank.setWeight(80);
            tank.setMovementMode(megamek.common.units.EntityMovementMode.TRACKED);
            tank.setHasNoTurret(false);
            atlas = tank;
        } else { atlas = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity(); }
        atlas.setId(turret ? 9550 : 9500);
        var guns = new ArrayList<Mounted<?>>();
        for (int i = 0; i < 5; i++) {
            guns.add(atlas.addEquipment(EquipmentType.get("ISAC2"), turret ? megamek.common.units.Tank.LOC_TURRET : Mek.LOC_LEFT_ARM));
        }
        var selection = UnitModelSelection.capture(atlas, -1, false, tileset);
        var model = library.get(selection, atlas.getId());
        var source = unit(atlas.getId(), new Coords(4, 5), selection);
        var placed = new ModelInstance(model.instance.model);
        var targets = new ArrayList<BoardScene.Unit>();
        var models = new ArrayList<ModelInstance>();
        var instances = new java.util.HashMap<String, ModelInstance>();
        instances.put(source.id() + ":-1", placed);
        models.add(placed);
        var events = new ArrayList<BoardScene.Animation>();
        for (int i = 0; i < 5; i++) {
            var target = unit(atlas.getId() + 1 + i, new Coords(2 + i, 1), selection);
            var victim = new ModelInstance(model.instance.model);
            model.place(victim, renderer.camera, BoardGeometry.center(target.location().coords(), 0), 180, target);
            instances.put(target.id() + ":-1", victim);
            models.add(victim);
            targets.add(target);
            events.add(combat(source, target, guns.get(i), true));
        }
        // A second gun at the first target must join that pass, despite arriving after the other targets.
        events.add(combat(source, targets.getFirst(), guns.get(1), true));
        var scene = UnitPlaybackTest.scene(source, targets.get(0), targets.get(1), targets.get(2), targets.get(3), targets.get(4));
        var playback = new UnitPlayback();
        playback.accept(events, scene, ignored -> false);
        playback.advance(0, UnitMotion.Speed.DOUBLE);
        var animator = new UnitAnimator();
        var effects = new GpuAttackEffects();
        Quaternion previous = null;
        int fired = 0;
        var checked = new java.util.HashSet<UnitAttack>();
        try {
            for (int frame = 0; frame < 170 && playback.busy(); frame++) {
                playback.advance(1.0 / 30, UnitMotion.Speed.DOUBLE);
                animator.apply(model, placed, source, UnitMotion.Sample.STILL, 0, 0, true, 0);
                animator.attacks(model, source, playback.attacks());
                model.place(placed, renderer.camera, BoardGeometry.center(source.location().coords(), 0), 0, source);
                animator.aimShots(model, source, playback.attacks(), shot -> instances.get(shot.event.target().id() + ":-1"));
                var rotation = placed.getNode(turret ? "turret" : "LA").rotation.cpy();
                if (previous != null) {
                    assertTrue(Math.abs(previous.dot(rotation)) > .96f, "Target switching must not snap the shared joint");
                }
                previous = rotation;
                effects.update(playback.attacks(), library, instances);
                renderer.frame(models, BoardGeometry.center(new Coords(4, 3), 0), effects,
                      turret ? "firing-turret-five-targets" : "firing-five-targets", frame);
                for (var shot : playback.attacks()) {
                    if (shot.seconds < UnitAttack.ANTICIPATION_SECONDS || shot.seconds >= shot.contactSeconds) { continue; }
                    var binding = model.equipment().stream().filter(shot::fires).findFirst().orElseThrow();
                    var origin = new Vector3();
                    var forward = new Vector3();
                    UnitModelAttachment.emitter(placed, binding.emitters().getFirst(), origin, forward);
                    var trace = effects.emissions(shot).getFirst();
                    assertTrue(forward.dot(trace.target().cpy().sub(origin).nor()) > .97f, "Every gun must face its own target when it fires");
                    if (checked.add(shot)) { fired++; }
                }
            }
            assertEquals(6, fired);
        } finally { effects.dispose(); }
    }

    private static void secondTurret(GpuUnitModels library, GpuPlaybackReview.ReviewRenderer renderer) {
        var selection = GpuFamilyAssemblyReview.selection("tracked", List.of(new UnitModelEquipment.Mount(0,
              "ISAC20", "FT", "", false, false, 0, EquipmentModelPolicy.WEAPON, "ballistic", List.of())));
        var model = library.get(selection, 9600);
        var source = unit(9600, new Coords(3, 3), selection);
        var target = unit(9601, new Coords(0, 3), selection);
        var placed = new ModelInstance(model.instance.model);
        var raw = UnitPlaybackTest.attack(source, target, ResolvedAttack.Kind.SHOT, true);
        var shot = new UnitAttack(raw);
        shot.seconds = .24f;
        var animator = new UnitAnimator();
        animator.apply(model, placed, source, UnitMotion.Sample.STILL, 0, 0, true, 0);
        model.place(placed, renderer.camera, BoardGeometry.center(source.location().coords(), 0), 0, source);
        animator.aim(model, source, shot, new Vector3(), null);
        assertTrue(placed.getNode("turret2").rotation.getAngle() > 30, "Second turret must rotate, rather than bending just the gun");
        var origin = new Vector3();
        var forward = new Vector3();
        UnitModelAttachment.emitter(placed, model.equipment().getFirst().emitters().getFirst(), origin, forward);
        var endpoint = shot.endpoint(null, origin, new Vector3());
        assertTrue(forward.dot(endpoint.sub(origin).nor()) > .98f);
    }

    private static void roundsAndFallback(GpuUnitModels library, GpuPlaybackReview.ReviewRenderer renderer, MekTileset tileset) throws Exception {
        var atlas = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
        atlas.setId(9700);
        var gun = atlas.addEquipment(EquipmentType.get("ISRotaryAC5"), Mek.LOC_LEFT_ARM);
        gun.setMode("6-shot");
        gun.newRound(1);
        var selection = UnitModelSelection.capture(atlas, -1, false, tileset);
        var model = library.get(selection, atlas.getId());
        var source = unit(atlas.getId(), new Coords(2, 3), selection);
        var target = unit(9701, new Coords(2, 1), selection);
        var placed = new ModelInstance(model.instance.model);
        var victim = new ModelInstance(model.instance.model);
        model.place(placed, renderer.camera, BoardGeometry.center(source.location().coords(), 0), 0, source);
        model.place(victim, renderer.camera, BoardGeometry.center(target.location().coords(), 0), 180, target);
        var shot = new UnitAttack(combat(source, target, gun, true));
        assertEquals(6, shot.event.result().shot().shots());
        var effects = new GpuAttackEffects();
        try {
            for (int round = 0; round < 6; round++) {
                shot.seconds = UnitAttack.ANTICIPATION_SECONDS + shot.roundDelay(round, 6) + .00001f;
                effects.update(shot, library, Map.of(source.id() + ":-1", placed, target.id() + ":-1", victim));
                effects.render(renderer.camera);
                assertEquals(round + 1, effects.emissions(shot).size(), "A barrel must wait for its actual round's firing time");
            }
            var emitted = effects.emissions(shot);
            var origins = emitted.stream().map(trace -> trace.origin().cpy()).toList();
            var forwards = emitted.stream().map(trace -> trace.forward().cpy()).toList();
            placed.transform.translate(40, 20, 0).rotate(Vector3.Z, 60);
            shot.seconds += .1f;
            effects.render(renderer.camera);
            assertEquals(origins, effects.emissions(shot).stream().map(GpuAttackEffects.Trace::origin).toList());
            assertEquals(forwards, effects.emissions(shot).stream().map(GpuAttackEffects.Trace::forward).toList(), "Released exhaust retains its launch frame");

            var fallback = new megamek.common.units.Tank();
            fallback.setId(source.id());
            fallback.setWeight(50);
            for (String name : List.of("ISMediumLaser", "ISPPC", "Flamer", "LRM 20")) {
                fallback.addEquipment(EquipmentType.get(name), megamek.common.units.Tank.LOC_TURRET);
            }
            selection = UnitModelSelection.capture(fallback, -1, false, tileset);
            source = unit(atlas.getId(), source.location().coords(), selection);
            var guns = fallback.getWeaponList();
            var laser = combat(source, target, guns.getFirst(), false);
            var result = laser.result();
            var mounts = guns.stream().map(mount -> new ResolvedAttack.Mount(atlas.getId(), mount.getEquipmentNum(), ResolvedAttack.Shot.capture(mount))).toList();
            var mixed = new UnitAttack(new BoardScene.Combat(new ResolvedAttack(result.id(), result.kind(), result.attacker(), result.target(),
                  result.targetType(), result.equipmentIndex(), result.equipmentName(), result.limb(), false, mounts, result.shot()), source, target, target.location()));
            mixed.seconds = .27f;
            mixed.landscape = ignored -> null;
            assertEquals("laser", mixed.effect(source.id(), guns.get(0).getEquipmentNum()));
            assertEquals("ppc", mixed.effect(source.id(), guns.get(1).getEquipmentNum()));
            assertEquals("flame", mixed.effect(source.id(), guns.get(2).getEquipmentNum()));
            effects.update(mixed, null, Map.of(source.id() + ":-1", placed, target.id() + ":-1", victim));
            renderer.frame(List.of(placed, victim), BoardGeometry.center(new Coords(2, 2), 0), effects, "firing-mesh-free-mixed", 0);
            assertEquals(20, effects.missileCount(), "A missile fallback must not suppress the other guns in its bay");
            assertEquals(3, effects.emissions(mixed).size());
            assertEquals(2, effects.emissions(mixed).stream().filter(trace -> !trace.impact()).count(), "Missed beams without landscape have no fake impact");
            assertTrue(effects.flameParticleCount() > 0, "A mesh-free flamer is still a flame");
            assertFalse(effects.emissions(mixed).stream().filter(trace -> !trace.impact()).anyMatch(trace -> trace.origin().dst(trace.target()) < BoardGeometry.HEIGHT * 100));
            effects.update(List.of(), null, Map.of());
            assertTrue(effects.emissions(mixed).isEmpty());
        } finally { effects.dispose(); }
    }

    private static BoardScene.Unit unit(int id, Coords position, BoardScene.UnitModel model) {
        return new BoardScene.Unit(id, -1, "Firing review", new BoardScene.Waypoint(position, 0, 0), null, false, null, 2, false, model, 0);
    }

    private static BoardScene.Combat combat(BoardScene.Unit source, BoardScene.Unit target, Mounted<?> gun, boolean hit) {
        var profile = ResolvedAttack.Shot.capture(gun);
        var result = new ResolvedAttack(new UUID(target.id(), gun.getEquipmentNum()), ResolvedAttack.Kind.SHOT,
              new UnitLocation(source.id(), source.location().coords(), 0, 0, 0), new UnitLocation(target.id(), target.location().coords(), 0, 0, 0),
              Targetable.TYPE_ENTITY, gun.getEquipmentNum(), gun.getType().getInternalName(), gun.getLocation(), hit,
              List.of(new ResolvedAttack.Mount(source.id(), gun.getEquipmentNum(), profile)), profile);
        return new BoardScene.Combat(result, source, target, target.location());
    }
}
