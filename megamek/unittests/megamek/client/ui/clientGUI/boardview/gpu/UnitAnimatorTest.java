/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxNativesLoader;
import megamek.common.ResolvedAttack;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;
import org.junit.jupiter.api.Test;

class UnitAnimatorTest {
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
            var model = new GpuMeeple(raw, null, true, List.of(), 1, new Vector3(1, 1, 1),
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
                assertEquals(45, placed.getNode("root").rotation.getAngleAround(Vector3.X), .001);
                assertEquals(0, playback.holdSeconds());
                playback.advance(motion.remainingSeconds() / speed.rate, speed);
                animator.apply(model, placed, fallen, motion.sample(), 0, 0, false, 0);
                assertEquals(90, placed.getNode("root").rotation.getAngleAround(Vector3.X), .001);
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
            var model = new GpuMeeple(raw, null, true, List.of(binding));
            try {
                var source = UnitPlaybackTest.unit(1, 1);
                var appearance = new BoardScene.UnitModel("", "", "", 1, 0, BoardScene.LocationDamage.NONE,
                      UnitModelState.capture(new BipedMek()));
                var unit = new BoardScene.Unit(1, -1, "Mek", source.location(), null, false, null, 2, false, appearance, 0);
                var placed = new ModelInstance(raw);
                var animator = new UnitAnimator();
                animator.apply(model, placed, unit, new UnitMotion(unit.location()).sample(), 0, 0, false, 0);
                var attack = new UnitAttack(UnitPlaybackTest.attack(unit, UnitPlaybackTest.unit(2, 2), ResolvedAttack.Kind.SHOT, true));
                attack.seconds = UnitAttack.ANTICIPATION_SECONDS + .12f;
                var original = placed.getNode("gun").globalTransform.getTranslation(new Vector3());
                var forward = new Vector3(0, 1, 0).rot(placed.getNode("muzzle").globalTransform).nor();
                animator.attack(model, unit, attack);
                var displacement = placed.getNode("gun").globalTransform.getTranslation(new Vector3()).sub(original);
                assertTrue(displacement.len() > .8f);
                assertEquals(-1, displacement.nor().dot(forward), .0001, "Wrong recoil axis at mount angle " + angle);
                attack.seconds = attack.duration;
                animator.attack(model, unit, attack);
                assertTrue(placed.getNode("gun").globalTransform.getTranslation(new Vector3()).epsilonEquals(original, .0001f));
            } finally {
                model.dispose();
            }
        }
    }
}
