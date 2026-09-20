/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.Targetable;
import megamek.common.units.TripodMek;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The selected real hand, foot or weapon must reach the posed opponent, then return to its rest assembly. */
@Tag("on-demand")
class GpuPhysicalContactSmokeTest {
    @Test
    void physicalContactUsesTheSelectedLimbAndRecoversAcrossMekRigs() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var library = new GpuUnitModels();
                var picking = new UnitPicking();
                var tuning = BoardGeometry.tuning();
                try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    var atlas = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
                    int id = 9700;
                    for (Entity entity : List.of(atlas, new TripodMek(), new QuadMek())) {
                        entity.setId(id++);
                        if (entity != atlas) {
                            entity.setWeight(50);
                            for (int loc = 0; loc < entity.locations(); loc++) { entity.initializeInternal(10, loc); }
                        }
                        var weapon = entity instanceof QuadMek ? null : entity.addEquipment(EquipmentType.get("Tree Club"), Mek.LOC_LEFT_ARM);
                        var selection = UnitModelSelection.capture(entity, -1, false, tileset);
                        var model = library.get(selection, entity.getId());
                        assertNotNull(model);
                        var source = new ModelInstance(model.instance.model);
                        var target = new ModelInstance(model.instance.model);
                        var animator = new UnitAnimator();
                        for (float scale : new float[] { .7f, 1f }) {
                            BoardGeometry.tune(new BoardGeometry.Tuning(tuning.hexScale(), scale, tuning.unitHeightScale(),
                                  tuning.levelHeight(), tuning.gridShade(), tuning.multiHexUnitScale()));
                            for (int facing = 0; facing < 6; facing++) {
                                var start = new Coords(2, 3);
                                var end = start.translated(facing);
                                var attacker = unit(entity.getId(), start, facing, selection);
                                var victim = unit(9799, end, (facing + 3) % 6, selection);
                                var origin = BoardGeometry.center(start, 0);
                                model.place(target, renderer.camera, BoardGeometry.center(end, 0), (facing + 3) * 60, victim);
                                var kinds = entity instanceof QuadMek ? List.of(ResolvedAttack.Kind.KICK)
                                      : List.of(ResolvedAttack.Kind.PUNCH, ResolvedAttack.Kind.KICK, ResolvedAttack.Kind.PUSH, ResolvedAttack.Kind.CLUB);
                                for (var kind : kinds) {
                                    int limb = kind == ResolvedAttack.Kind.KICK
                                          ? entity instanceof QuadMek ? Mek.LOC_LEFT_ARM : entity instanceof TripodMek ? Mek.LOC_CENTER_LEG : Mek.LOC_LEFT_LEG
                                          : Mek.LOC_LEFT_ARM;
                                    int index = kind == ResolvedAttack.Kind.CLUB ? weapon.getEquipmentNum() : -1;
                                    for (boolean hit : List.of(true, false)) {
                                        var result = new ResolvedAttack(new UUID(5, 17), kind,
                                              new UnitLocation(attacker.id(), start, facing, 0, 0),
                                              new UnitLocation(victim.id(), end, (facing + 3) % 6, 0, 0),
                                              Targetable.TYPE_ENTITY, index, index < 0 ? "" : "Tree Club", limb, hit);
                                        var attack = new UnitAttack(new BoardScene.Combat(result, attacker, victim, victim.location()));
                                        if (facing == 0 && scale == .7f && hit) {
                                            verifyTravel(model, animator, source, target, attacker, attack, picking, renderer);
                                        }
                                        Vector3 contact = null;
                                        for (int frame = 0; frame <= 40; frame++) {
                                            attack.seconds = frame <= 26 ? attack.contactSeconds * frame / 26
                                                  : attack.contactSeconds + (attack.duration - attack.contactSeconds) * (frame - 26) / 14;
                                            animator.apply(model, source, attacker, UnitMotion.Sample.STILL, 0, 0, true, 0);
                                            animator.attack(model, attacker, attack);
                                            model.place(source, renderer.camera, origin, facing * 60, attacker);
                                            contact = attack.contact(target, UnitAttack.center(source, attacker.location(), new Vector3()), picking, new Vector3());
                                            animator.aim(model, attacker, attack, contact, target);
                                            if (frame == 26) {
                                                var tip = animator.physicalTip(model, attack);
                                                assertNotNull(tip);
                                                String label = model.rigs().getFirst().type() + " " + kind + " facing " + facing + " scale " + scale;
                                                if (hit) { assertTrue(tip.dst(contact) < 1.5f, label + " contact gap " + tip.dst(contact)
                                                      + " tip " + tip + " target " + contact + " root " + source.transform.getTranslation(new Vector3())); }
                                                else { assertTrue(!UnitBounds.world(target).contains(tip), label + " a miss must clear the opponent"); }
                                            }
                                            if (scale == .7f && facing == 0 && hit && frame % 2 == 0) {
                                                for (boolean top : List.of(false, true)) {
                                                renderer.topView = top;
                                                renderer.frame(List.of(source, target), origin.cpy().lerp(BoardGeometry.center(end, 0), .5f), null,
                                                      "contact-" + model.rigs().getFirst().type() + "-" + kind.name().toLowerCase(java.util.Locale.ROOT)
                                                            + (top ? "-top" : "-iso"), frame);
                                                }
                                            }
                                        }
                                        assertTrue(source.transform.getTranslation(new Vector3()).dst(origin.cpy().add(0, 0, .5f)) < .001f,
                                              "Recovery cannot retain a contact lunge");
                                        int visibleLimb = limb == Mek.LOC_CENTER_LEG ? Mek.LOC_LEFT_LEG : limb;
                                        UnitDamageDisplay.show(source, new BoardScene.LocationDamage(Set.of(entity.getLocationAbbr(visibleLimb)), Set.of()));
                                        assertNull(animator.physicalTip(model, attack), "A blown-off selected limb cannot animate a substitute");
                                        source = new ModelInstance(model.instance.model);
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    BoardGeometry.tune(tuning);
                    picking.clear();
                    library.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Physical-contact review failed", failure.get()); }
    }

    private static BoardScene.Unit unit(int id, Coords coords, int facing, BoardScene.UnitModel model) {
        return new BoardScene.Unit(id, -1, "Contact review", new BoardScene.Waypoint(coords, 0, facing),
              null, false, null, 2, false, model, 0);
    }

    private static void verifyTravel(GpuUnitModel model, UnitAnimator animator, ModelInstance source, ModelInstance target,
          BoardScene.Unit unit, UnitAttack attack, UnitPicking picking, GpuPlaybackReview.ReviewRenderer renderer) {
        var feet = model.rigs().getFirst().joints().entrySet().stream().filter(j -> j.getKey().endsWith("Foot"))
              .map(java.util.Map.Entry::getValue).toList();
        var origin = BoardGeometry.center(unit.location().coords(), 0);
        for (boolean returning : List.of(false, true)) {
            Vector3 previous = null, previousRoot = null;
            String previousFoot = null;
            float drift = 0, distance = 0;
            for (int frame = 0; frame <= 160; frame++) {
                float t = frame / 160f;
                attack.seconds = returning ? attack.contactSeconds + UnitAttack.RECOVERY_SECONDS + t * UnitAttack.MELEE_RUN_SECONDS
                      : t * attack.approachSeconds;
                animator.apply(model, source, unit, UnitMotion.Sample.STILL, 0, 0, true, 0);
                model.place(source, renderer.camera, origin, 0, unit);
                var contact = attack.contact(target, UnitAttack.center(source, unit.location(), new Vector3()), picking, new Vector3());
                animator.aim(model, unit, attack, contact, target);
                Vector3 planted = null;
                String support = null;
                for (String foot : feet) {
                    var point = source.getNode(foot).globalTransform.getTranslation(new Vector3()).mul(source.transform);
                    if (planted == null || point.z < planted.z) { planted = point; support = foot; }
                }
                var root = source.transform.getTranslation(new Vector3());
                if (previous != null && t > .15f && t < .85f && support.equals(previousFoot)
                      && Math.abs(planted.z - previous.z) < .02f) {
                    drift += (float) Math.hypot(planted.x - previous.x, planted.y - previous.y);
                    distance += root.dst(previousRoot);
                }
                previous = planted;
                previousRoot = root;
                previousFoot = support;
            }
            String label = model.rigs().getFirst().type() + " " + attack.event.result().kind() + " return=" + returning;
            System.out.println(label + " planted foot drift/travel=" + drift / distance);
            assertTrue(distance > 1 && drift / distance < .12f, label + " feet slide " + drift + " / " + distance);
        }
    }
}
