/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.ProneCause;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;

/** Native rigid-pose review; captures the same evaluator used by both board cameras. */
final class GpuUnitAnimationReview {
    private GpuUnitAnimationReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) throws Exception {
        var tileset = new MekTileset(Configuration.unitImagesDir());
        tileset.loadFromFile("mekset.txt");
        int id = 6400;
        for (Mek mek : List.of(new BipedMek(), new QuadMek(), new TripodMek())) {
            mek.setId(id++);
            for (int location = 0; location < mek.locations(); location++) {
                mek.initializeInternal(10, location);
            }
            var selection = UnitModelSelection.capture(mek, -1, false, tileset);
            var model = library.get(selection, mek.getId());
            assertNotNull(model);
            assertEquals(1, model.rigs().size());
            var stand = new ModelInstance(model.instance.model);
            var walk = new ModelInstance(model.instance.model);
            var gait = new UnitMotion.Sample(true, EntityMovementType.MOVE_WALK, .4f, .125f, 0, 1, 0, 0, ProneCause.NONE);
            var animation = new UnitAnimator();
            animation.apply(model, walk, unit(mek, selection), gait, .4f, .1f, false, 30);
            String leftLeg = model.rigs().getFirst().joints().getOrDefault("leftLeg", "FLL");
            assertFalse(walk.getNode(leftLeg).rotation.isIdentity());
            assertTrue(stand.getNode(leftLeg).rotation.isIdentity());
            assertFalse(walk.getNode("CT").rotation.isIdentity(), "Twist is layered on the same rig");
            var crouch = new ModelInstance(model.instance.model);
            mek.setProne(ProneCause.VOLUNTARY);
            selection = UnitModelSelection.capture(mek, -1, false, tileset);
            animation.apply(model, crouch, unit(mek, selection), UnitMotion.Sample.STILL, 1, 0, true, 0);
            var fallen = new ModelInstance(model.instance.model);
            mek.setProne(ProneCause.FORCED);
            selection = UnitModelSelection.capture(mek, -1, false, tileset);
            new UnitAnimator().apply(model, fallen, unit(mek, selection), UnitMotion.Sample.STILL, 1, 0, false, 0);
            if (!(mek instanceof QuadMek)) {
                assertEquals(270, fallen.getNode("root").rotation.getAngleAround(Vector3.X), .01f,
                      "A newly revealed prone unit starts on the ground, without replaying its fall");
            }
            var poses = new ArrayList<>(List.of(stand, walk, crouch, fallen));
            for (int index = 0; index < poses.size(); index++) {
                var pose = poses.get(index);
                pose.transform.setToTranslation((1.5f - index) * 75, 0, -UnitBounds.local(pose).min.z);
            }
            GpuModularUnitModelsSmokeTest.renderReview(batch, poses, "runtime-animation-" + model.rigs().getFirst().type(), 335, 20);
            assertTrue(UnitBounds.local(crouch).getDepth() < UnitBounds.local(stand).getDepth() * .9f,
                  "Crouch Z extent " + UnitBounds.local(crouch) + "; standing " + UnitBounds.local(stand)
                        + "; rig " + model.rigs().getFirst().type());
            assertTrue(UnitBounds.local(fallen).getDepth() < UnitBounds.local(stand).getDepth() * .85f,
                  "Fall Z extent " + UnitBounds.local(fallen) + "; standing " + UnitBounds.local(stand)
                        + "; rig " + model.rigs().getFirst().type());
            var before = new ModelInstance(model.instance.model);
            var sameTime = new UnitAnimator();
            sameTime.apply(model, before, unit(mek, selection), UnitMotion.Sample.STILL, 1, 0, true, 0);
            var matrix = before.getNode("root").globalTransform.cpy();
            for (int frame = 0; frame < 100; frame++) {
                sameTime.apply(model, before, unit(mek, selection), UnitMotion.Sample.STILL, 1, .01f, false, 0);
            }
            assertTrue(java.util.Arrays.equals(matrix.val, before.getNode("root").globalTransform.val), "No cumulative pose drift");
            UnitDamageDisplay.show(before, new BoardScene.LocationDamage(Set.of(mek.getLocationAbbr(Mek.LOC_LEFT_ARM)), Set.of()));
            sameTime.apply(model, before, unit(mek, selection), gait, 2, .1f, false, 0);
            assertTrue(UnitDamageDisplay.locationParts(before, mek.getLocationAbbr(Mek.LOC_LEFT_ARM)).stream()
                  .noneMatch(part -> part.enabled), "Animation cannot restore detached geometry");
        }
        var armor = new BattleArmor();
        armor.setId(6450);
        armor.setSquadSize(6);
        for (int member = 1; member <= 6; member++) {
            armor.initializeInternal(1, member);
        }
        var selection = UnitModelSelection.capture(armor, -1, false, tileset);
        var model = library.get(selection, armor.getId());
        assertEquals(6, model.rigs().size());
        var instance = new ModelInstance(model.instance.model);
        var animator = new UnitAnimator();
        animator.apply(model, instance, unit(armor, selection), UnitMotion.Sample.STILL, 0, 0, false, 0);
        var heading = instance.getNode("trooper-3").rotation.cpy();
        assertEquals(6, instance.nodes.size);
        assertNotEquals(instance.getNode("trooper-1").rotation, instance.getNode("trooper-2").rotation);
        assertTroopsOutward(instance);
        armor.setFacing(4);
        selection = UnitModelSelection.capture(armor, -1, false, tileset);
        animator.apply(model, instance, unit(armor, selection), UnitMotion.Sample.STILL, 0, 0, false, 0);
        assertEquals(heading, instance.getNode("trooper-3").rotation, "Gameplay facing must not rotate the formation members");
        GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(instance), "runtime-animation-ba-idle", 110, 8);
        var survivorHeading = instance.getNode("trooper-6").rotation.cpy();
        for (int member = 1; member < 6; member++) {
            armor.setInternal(0, member);
        }
        selection = UnitModelSelection.capture(armor, -1, false, tileset);
        model = library.get(selection, armor.getId());
        instance = new ModelInstance(model.instance.model);
        animator.apply(model, instance, unit(armor, selection), UnitMotion.Sample.STILL, 0, 0, false, 0);
        assertEquals(survivorHeading, instance.getNode("trooper-6").rotation, "Casualties preserve surviving headings");

        int direction = 0;
        for (var mode : List.of(EntityMovementMode.INF_LEG, EntityMovementMode.INF_JUMP,
              EntityMovementMode.INF_MOTORIZED, EntityMovementMode.WHEELED, EntityMovementMode.TRACKED, EntityMovementMode.HOVER)) {
            var infantry = new ConvInfantry();
            infantry.setId(6460 + direction);
            infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
            infantry.setMovementMode(mode);
            selection = UnitModelSelection.capture(infantry, -1, false, tileset);
            model = library.get(selection, infantry.getId());
            instance = new ModelInstance(model.instance.model);
            animator = new UnitAnimator();
            var unit = unit(infantry, selection);
            animator.apply(model, instance, unit, UnitMotion.Sample.STILL, 0, 0, false, 0);
            assertTroopsOutward(instance);
            var start = new BoardScene.Waypoint(unit.location().coords().translated(direction++), 0, 0);
            var motion = new UnitMotion(start);
            boolean transport = model.rigs().stream().anyMatch(UnitRig::transport);
            motion.append(List.of(start, unit.location()), EntityMovementType.MOVE_WALK, 0, transport);
            animator.apply(model, instance, unit, motion.sample(), 0, 0, false, 0);
            motion.advance(motion.remainingSeconds() - (transport ? UnitMotion.UNLOAD_SECONDS : 0), 1);
            animator.apply(model, instance, unit, motion.sample(), 2, .1f, false, 0);
            if (transport) {
                var parking = instance.getNode("vehicle-0").globalTransform.cpy();
                for (var member : instance.nodes) {
                    if (member.id.startsWith("trooper-")) {
                        assertFalse(UnitBounds.subtree(member).isValid(), "Passengers wait until parking has finished");
                    }
                }
                motion.advance(UnitMotion.UNLOAD_SECONDS * .5, 1);
                animator.apply(model, instance, unit, motion.sample(), 2.3f, .3f, false, 0);
                assertArrayEquals(parking.val, instance.getNode("vehicle-0").globalTransform.val,
                      "The vehicle stays stopped while troops unload and turn outward");
            }
            motion.advance(1, 1);
            animator.apply(model, instance, unit, motion.sample(), 3, .1f, false, 0);
            assertTroopsOutward(instance);
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(instance),
                  "runtime-animation-infantry-" + mode.name().toLowerCase(java.util.Locale.ROOT), transport ? 220 : 110, 8);
        }
    }

    private static void assertTroopsOutward(ModelInstance instance) {
        int outward = 0, troops = 0;
        for (var member : instance.nodes) {
            if (member.id.startsWith("trooper-")) {
                troops++;
                Vector3 sight = new Vector3(Vector3.Y).mul(member.rotation);
                if (sight.dot(member.translation) > 0) {
                    outward++;
                }
            }
        }
        assertTrue(troops > 0 && outward >= troops - 1,
              "Troops watch outward around the hex, with at most one inward exception: " + outward + "/" + troops);
    }

    private static BoardScene.Unit unit(Entity entity, BoardScene.UnitModel selection) {
        return new BoardScene.Unit(entity.getId(), -1, "Animation review",
              new BoardScene.Waypoint(new Coords(2, 2), 0, entity.getFacing()),
              new BoardScene.Pixels(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)), false, null,
              entity.height() + 1, false, selection, 0xFFFFFF);
    }
}
