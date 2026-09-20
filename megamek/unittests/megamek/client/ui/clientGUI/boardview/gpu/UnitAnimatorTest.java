/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;
import org.junit.jupiter.api.Test;

class UnitAnimatorTest {
    @Test
    void jumpingTiltsTheBodyAndItsExhaustTowardTravelThenReturnsUpright() {
        GdxNativesLoader.load();
        for (float heightScale : new float[] { .6f, 1.8f }) {
            var raw = new Model();
            var root = new Node();
            root.id = "root";
            var pack = new Node();
            pack.id = "pack";
            root.addChild(pack);
            raw.nodes.add(root);
            raw.calculateTransforms();
            var emitter = new UnitModelDescriptor.Emitter("jet", "pack", List.of(0f, 0f, 0f), List.of(0f, 0f, -1f), "exhaust", "exhaust");
            var model = new GpuUnitModel(raw, null, true, List.of(), heightScale, new Vector3(1, 1, 1),
                  List.of(new UnitRig("mek", "biped-v1", null, Map.of("root", "root"), List.of(), List.of())));
            try {
                for (int facing : new int[] { 0, 1, 3, 5 }) {
                    var start = new BoardScene.Waypoint(new Coords(2, 5), 0, facing);
                    var end = new BoardScene.Waypoint(new Coords(4, 3), 0, (facing + 1) % 6);
                    var appearance = new BoardScene.UnitModel("", "", "", 1, 0, BoardScene.LocationDamage.NONE,
                          UnitModelState.capture(new BipedMek()));
                    var unit = new BoardScene.Unit(1, -1, "Mek", end, null, false, null, 2, false, appearance, 0);
                    var motion = new UnitMotion(start);
                    motion.append(List.of(start, end), EntityMovementType.MOVE_JUMP, 6);
                    double duration = motion.remainingSeconds();
                    motion.advance(duration * .25, 1);
                    var instance = new ModelInstance(raw);
                    var animator = new UnitAnimator();
                    var camera = new OrthographicCamera();
                    animator.apply(model, instance, unit, motion.sample(), 0, 0, false, 0);
                    model.place(instance, camera, motion.position(), motion.facing(), unit);
                    var exhaust = new Vector3();
                    UnitModelAttachment.emitter(instance, emitter, new Vector3(), exhaust);
                    var travel = BoardGeometry.center(end.coords(), 0).sub(BoardGeometry.center(start.coords(), 0)).nor();
                    assertTrue(exhaust.dot(travel) < -.1f, "Thrust points along travel even during a sideways or backward jump");
                    assertEquals(motion.sample().jets().tilt(), Math.toDegrees(Math.acos(-exhaust.z)), .1,
                          "Display height scaling must preserve the intended world-space angle");
                    var rotation = instance.getNode("root").rotation.cpy();
                    animator.apply(model, instance, unit, motion.sample(), 0, 0, false, 0);
                    assertTrue(rotation.equals(instance.getNode("root").rotation), "Paused frames cannot accumulate lean");
                    motion.advance(duration * .6, 1);
                    animator.apply(model, instance, unit, motion.sample(), 0, 0, false, 0);
                    model.place(instance, camera, motion.position(), motion.facing(), unit);
                    UnitModelAttachment.emitter(instance, emitter, new Vector3(), exhaust);
                    assertTrue(exhaust.epsilonEquals(new Vector3(0, 0, -1), .001f), "The pack is upright for landing");
                    motion.finish();
                    animator.apply(model, instance, unit, motion.sample(), 0, 0, true, 0);
                    assertTrue(instance.getNode("root").rotation.isIdentity(.001f), "Skip removes the flight pose");
                }
            } finally {
                model.dispose();
            }
        }
    }

    @Test
    void terminalFallFinishesBeforeTheFullCompletionBufferAtEveryPlaybackSpeed() {
        GdxNativesLoader.load();
        for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE, UnitMotion.Speed.QUADRUPLE)) {
            var entity = new BipedMek();
            entity.setProne(ProneCause.FORCED);
            var appearance = new BoardScene.UnitModel("", "", "", 1, 0, BoardScene.LocationDamage.NONE, UnitModelState.capture(entity));
            var from = UnitPlaybackTest.unit(1, 0).location().withProneCause(ProneCause.NONE);
            var to = UnitPlaybackTest.unit(1, 1).location().withProneCause(ProneCause.FORCED);
            var fallen = new BoardScene.Unit(1, -1, "Mek", to, null, false, null, 2, false, appearance, 0);
            var target = UnitPlaybackTest.unit(2, 2);
            var playback = new UnitPlayback();
            playback.accept(List.of(new BoardScene.Movement(1, 0, List.of(from, to), EntityMovementType.MOVE_WALK, 0, 4, fallen),
                  UnitPlaybackTest.attack(fallen, target, ResolvedAttack.Kind.SHOT, true)), UnitPlaybackTest.scene(fallen, target), ignored -> false);
            playback.togglePaused();
            playback.advance(100, speed);
            var raw = new Model();
            var root = new Node();
            root.id = "root";
            raw.nodes.add(root);
            raw.calculateTransforms();
            var model = new GpuUnitModel(raw, null, true, List.of(), 1, new Vector3(1, 1, 1),
                  List.of(new UnitRig("mek", "biped-v1", null, Map.of("root", "root"), List.of(), List.of())));
            try {
                var placed = new ModelInstance(raw);
                var animator = new UnitAnimator();
                var waiting = playback.present(UnitPlaybackTest.scene(fallen, target)).units().getFirst();
                animator.apply(model, placed, waiting, UnitMotion.Sample.STILL, 0, 0, false, 0);
                assertEquals(0, placed.getNode("root").rotation.getAngleAround(Vector3.X), .001,
                      "A paused/queued fall must keep the captured starting pose instead of showing its future result");
                playback.togglePaused();
                playback.advance(0, speed);
                var motion = playback.motions.get(1);
                animator.apply(model, placed, fallen, motion.sample(), 0, 0, false, 0);
                assertEquals(0, placed.getNode("root").rotation.getAngleAround(Vector3.X), .001);
                playback.advance((motion.remainingSeconds() - UnitMotion.POSTURE_SECONDS / 2) / speed.rate, speed);
                animator.apply(model, placed, fallen, motion.sample(), 0, 0, false, 0);
                var halfway = new Vector3(Vector3.Z).mul(placed.getNode("root").rotation);
                assertTrue(halfway.y > 0 && halfway.z > .8f, "The forward fall accelerates from its standing pose");
                assertEquals(0, playback.holdSeconds());
                playback.advance(motion.remainingSeconds() / speed.rate, speed);
                animator.apply(model, placed, fallen, motion.sample(), 0, 0, false, 0);
                assertTrue(new Vector3(Vector3.Z).mul(placed.getNode("root").rotation).epsilonEquals(Vector3.Y, .001f),
                      "The full forward fall must finish before the completion hold");
                assertEquals(1, playback.holdSeconds(), 1e-6);
                playback.advance(.999, speed);
                assertNull(playback.attack());
                playback.advance(.002, speed);
                assertNotNull(playback.attack());
            } finally {
                model.dispose();
            }
        }
    }

    @Test
    void squadronRecoilAndEffectsSelectOnlyTheResolvedOwnersGuns() {
        var original = UnitPlaybackTest.attack(UnitPlaybackTest.unit(10, 1), UnitPlaybackTest.unit(2, 2), ResolvedAttack.Kind.SHOT, true);
        var result = original.result();
        var resolved = new ResolvedAttack(result.id(), result.kind(), result.attacker(), result.target(), result.targetType(),
              result.equipmentIndex(), result.equipmentName(), result.limb(), result.hit(),
              List.of(new ResolvedAttack.Mount(11, 0), new ResolvedAttack.Mount(12, 1)));
        var attack = new UnitAttack(new BoardScene.Combat(resolved, original.attacker(), original.target(), original.destination()));
        for (int member : List.of(11, 12, 13)) {
            for (int index : List.of(0, 1)) {
                var binding = new UnitEquipmentAssembly.Binding(index, "NOS", "gun", "module", false, List.of(), member);
                assertEquals(member == 11 && index == 0 || member == 12 && index == 1, attack.fires(binding));
            }
        }
        assertFalse(attack.fires(new UnitEquipmentAssembly.Binding(0, "NOS", "group", "module", false, List.of())));
    }

    @Test
    void recoilMovesOppositeTheAuthoredBarrelThroughRotatedScaledParents() {
        GdxNativesLoader.load();
        for (float angle : new float[] { 0, 90, 180, 247 }) {
            var raw = new Model();
            var torso = new Node();
            torso.id = "torso";
            torso.rotation.set(Vector3.Z, 60);
            torso.scale.set(2, 3, 1);
            var mount = new Node();
            mount.id = "gun";
            mount.rotation.set(Vector3.Z, angle);
            torso.addChild(mount);
            var muzzle = new Node();
            muzzle.id = "muzzle";
            muzzle.rotation.set(Vector3.X, 15);
            mount.addChild(muzzle);
            raw.nodes.add(torso);
            raw.calculateTransforms();
            var emitter = new UnitModelDescriptor.Emitter("muzzle", muzzle.id, List.of(0f, 2f, 0f),
                  List.of(0f, 1f, 0f), "muzzle", "laser");
            var binding = new UnitEquipmentAssembly.Binding(0, "RT", mount.id, "test-gun", false, List.of(emitter));
            var model = new GpuUnitModel(raw, null, true, List.of(binding));
            try {
                var source = UnitPlaybackTest.unit(1, 1);
                var appearance = new BoardScene.UnitModel("", "", "", 1, 0, BoardScene.LocationDamage.NONE,
                      UnitModelState.capture(new BipedMek()));
                var unit = new BoardScene.Unit(1, -1, "Mek", source.location(), null, false, null, 2, false, appearance, 0);
                var placed = new ModelInstance(raw);
                var animator = new UnitAnimator();
                animator.apply(model, placed, unit, new UnitMotion(unit.location()).sample(), 0, 0, false, 0);
                var attack = new UnitAttack(UnitPlaybackTest.attack(unit, UnitPlaybackTest.unit(2, 2), ResolvedAttack.Kind.SHOT, true));
                attack.seconds = UnitAttack.ANTICIPATION_SECONDS + UnitAttack.RECOIL_KICK_SECONDS;
                var original = placed.getNode("gun").globalTransform.getTranslation(new Vector3());
                var forward = new Vector3(0, 1, 0).rot(placed.getNode("muzzle").globalTransform).nor();
                animator.attack(model, unit, attack);
                var displacement = placed.getNode("gun").globalTransform.getTranslation(new Vector3()).sub(original);
                assertTrue(displacement.len() > .8f);
                assertEquals(-1, displacement.nor().dot(forward), .0001, "Wrong recoil axis at mount angle " + angle);
                attack.seconds = attack.duration;
                animator.apply(model, placed, unit, new UnitMotion(unit.location()).sample(), 0, 0, false, 0);
                animator.attack(model, unit, attack);
                assertTrue(placed.getNode("gun").globalTransform.getTranslation(new Vector3()).epsilonEquals(original, .0001f));
            } finally {
                model.dispose();
            }
        }
    }

    @Test
    void mixedGunsUseTheirOwnCalibreAndPpcKicksOnlyAfterCharging() {
        var original = UnitPlaybackTest.attack(UnitPlaybackTest.unit(1, 1), UnitPlaybackTest.unit(2, 2), ResolvedAttack.Kind.SHOT, true);
        var small = ResolvedAttack.Shot.capture(Mounted.createMounted(new BipedMek(), EquipmentType.get("ISAC2")));
        var large = ResolvedAttack.Shot.capture(Mounted.createMounted(new BipedMek(), EquipmentType.get("ISAC20")));
        var cannon = ResolvedAttack.Shot.capture(Mounted.createMounted(new BipedMek(), EquipmentType.get("ISLongTomCannon")));
        var ppc = ResolvedAttack.Shot.capture(Mounted.createMounted(new BipedMek(), EquipmentType.get("ISPPC")));
        var laser = ResolvedAttack.Shot.capture(Mounted.createMounted(new BipedMek(), EquipmentType.get("ISMediumLaser")));
        var result = original.result();
        var resolved = new ResolvedAttack(result.id(), result.kind(), result.attacker(), result.target(), result.targetType(),
              99, "Logical group", result.limb(), true,
              List.of(new ResolvedAttack.Mount(1, 0, small), new ResolvedAttack.Mount(1, 1, large),
                    new ResolvedAttack.Mount(2, 0, cannon), new ResolvedAttack.Mount(2, 1, ppc), new ResolvedAttack.Mount(3, 0, laser)), large);
        var attack = new UnitAttack(new BoardScene.Combat(resolved, original.attacker(), original.target(), original.destination()));
        var ac2 = new UnitEquipmentAssembly.Binding(0, "RT", "ac2", "", false, List.of(), 1);
        var ac20 = new UnitEquipmentAssembly.Binding(1, "RT", "ac20", "", false, List.of(), 1);
        var longTom = new UnitEquipmentAssembly.Binding(0, "RT", "cannon", "", false, List.of(), 2);
        var energy = new UnitEquipmentAssembly.Binding(1, "RT", "ppc", "", false, List.of(), 2);
        var beam = new UnitEquipmentAssembly.Binding(0, "RT", "laser", "", false, List.of(), 3);
        attack.seconds = UnitAttack.ANTICIPATION_SECONDS * .5f;
        assertTrue(attack.chargingPpc());
        assertEquals(0, attack.recoil(energy), "Charging must not recoil before the discharge");
        attack.seconds = UnitAttack.ANTICIPATION_SECONDS;
        assertFalse(attack.chargingPpc());
        assertEquals(0, attack.recoil(ac20));
        attack.seconds += UnitAttack.RECOIL_KICK_SECONDS;
        assertTrue(attack.recoil(ac2) > 0);
        assertTrue(attack.recoil(ac20) > attack.recoil(ac2) * 2);
        assertEquals(attack.recoil(ac20), attack.recoil(longTom), .0001f, "Both have rack size 20");
        assertTrue(attack.recoil(energy) > 0 && attack.recoil(energy) < attack.recoil(ac20));
        assertEquals(0, UnitAttack.cannonScale(ppc), "PPC recoil must not turn it into a ballistic muzzle explosion");
        assertEquals(0, attack.recoil(beam));
        float peak = attack.recoil(ac20);
        attack.seconds += UnitAttack.RECOIL_RECOVERY_SECONDS * .5f;
        assertTrue(attack.recoil(ac20) > 0 && attack.recoil(ac20) < peak);
        attack.seconds = attack.duration;
        assertEquals(0, attack.recoil(ac20));
        assertEquals(0, attack.recoil(energy));
        var extreme = new ResolvedAttack.Shot("", Set.of(), false, false, 1, 0, false, null, null, null, true, 10000);
        assertTrue(UnitAttack.cannonScale(extreme) < 3, "Large values cannot yank the gun out of its mounting");
    }
}
