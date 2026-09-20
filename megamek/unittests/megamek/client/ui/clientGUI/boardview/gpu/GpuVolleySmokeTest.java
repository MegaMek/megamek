/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Targetable;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Exercises actual authored launchers, native batch rendering, and hit/miss curves against posed target bounds. */
@Tag("on-demand")
class GpuVolleySmokeTest {
    @Test
    void realLaunchersEmitRackSizeWithPartialHitsArcsAndBoundedSmoke() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var library = new GpuUnitModels();
                var effects = new GpuAttackEffects();
                var missiles = new GpuMissileEffects();
                try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
                    renderer.camera.viewportWidth = 460;
                    renderer.camera.viewportHeight = 345;
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    var atlas = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
                    atlas.setId(9300);
                    var selected = UnitModelSelection.capture(atlas, -1, false, tileset);
                    var body = library.get(selected, atlas.getId());
                    var attacker = new BoardScene.Unit(atlas.getId(), -1, "Atlas", new BoardScene.Waypoint(new Coords(2, 5), 0, 0),
                          null, false, null, 2, false, selected, 0);
                    var victim = new BoardScene.Unit(9301, -1, "Target", new BoardScene.Waypoint(new Coords(2, 1), 0, 3),
                          null, false, null, 2, false, selected, 0);
                    var source = new ModelInstance(body.instance.model);
                    var target = new ModelInstance(body.instance.model);
                    var position = BoardGeometry.center(attacker.location().coords(), 0);
                    body.place(target, renderer.camera, BoardGeometry.center(victim.location().coords(), 0), 180, victim);
                    var weapon = atlas.getWeaponList().stream().filter(mount -> mount.getType().getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
                          .findFirst().orElseThrow();
                    int index = weapon.getEquipmentNum();
                    var animator = new UnitAnimator();
                    for (boolean indirect : List.of(false, true)) {
                        var captured = ResolvedAttack.Shot.capture(weapon);
                        var shot = new ResolvedAttack.Shot(indirect ? "Indirect" : "", captured.munitions(), false, false, 1, 20, indirect, 12);
                        var result = new ResolvedAttack(new UUID(12, indirect ? 2 : 1), ResolvedAttack.Kind.SHOT,
                              new UnitLocation(attacker.id(), attacker.location().coords(), 0, 0, 0),
                              new UnitLocation(victim.id(), victim.location().coords(), 3, 0, 0), Targetable.TYPE_ENTITY, index,
                              weapon.getType().getInternalName(), weapon.getLocation(), true, ResolvedAttack.captureMounts(atlas, index), shot);
                        var attack = new UnitAttack(new BoardScene.Combat(result, attacker, victim, victim.location()));
                        for (int frame = 0; frame <= 54; frame++) {
                            attack.seconds = frame / 60f;
                            animator.apply(body, source, attacker, UnitMotion.Sample.STILL, frame / 60f, 1f / 60, false, 0);
                            animator.attacks(body, attacker, List.of(attack));
                            body.place(source, renderer.camera, position, 0, attacker);
                            animator.aim(body, attacker, attack, UnitAttack.center(target, victim.location(), new Vector3()), target);
                            effects.update(List.of(attack), library, Map.of("9300:-1", source, "9301:-1", target));
                            renderer.frame(List.of(source, target), position.cpy().lerp(BoardGeometry.center(victim.location().coords(), 0), .5f),
                                  effects, indirect ? "lrm20-indirect-12-hits" : "lrm20-direct-12-hits", frame);
                            if (frame == 18) {
                                assertEquals(20, effects.missileCount(), "Twenty missiles, independent of authored emitter count or twelve hits");
                                assertTrue(effects.smokeCount() > 20);
                            }
                        }
                        var binding = body.equipment().stream().filter(attack::fires).findFirst().orElseThrow();
                        var origin = new Vector3();
                        UnitModelAttachment.emitter(source, binding.emitters().getFirst(), origin, new Vector3());
                        verifyCurves(attack, origin, target, indirect);
                        effects.update(List.of(), library, Map.of());
                        effects.render(renderer.camera);
                        assertEquals(0, effects.missileCount());
                        assertEquals(0, effects.smokeCount(), "Cancel/Instant clears the cached flight and trail");

                        attack.seconds = .3f;
                        var launch = GpuMissileEffects.capture(attack, new Vector3[] { origin }, target, 20, 12, indirect, 42);
                        missiles.begin();
                        missiles.add(launch);
                        var profiler = new GLProfiler(Gdx.graphics);
                        profiler.enable();
                        try {
                            profiler.reset();
                            missiles.render(renderer.camera);
                            assertEquals(20, missiles.missileCount());
                            assertTrue(profiler.getDrawCalls() <= 3, "One missile-body draw plus smoke and flame batches");
                            int smoke = missiles.smokeCount();
                            missiles.render(renderer.camera);
                            assertEquals(smoke, missiles.smokeCount(), "A paused clock does not grow smoke");
                            missiles.begin();
                            for (int i = 0; i < 64; i++) { missiles.add(launch); }
                            profiler.reset();
                            missiles.render(renderer.camera);
                            assertEquals(1280, missiles.missileCount(), "Bodies are not dropped when the smoke budget is reached");
                            assertTrue(missiles.smokeCount() <= GpuMissileEffects.SMOKE_BUDGET);
                            assertTrue(profiler.getDrawCalls() <= 5);
                            attack.seconds = attack.duration;
                            missiles.render(renderer.camera);
                            assertEquals(0, missiles.missileCount());
                            assertEquals(0, missiles.smokeCount(), "Smoke dissipates before recovery completes");
                        } finally {
                            profiler.disable();
                        }
                    }
                    for (String ammo : List.of("M_SMOKE_WARHEAD", "M_INFERNO", "M_FLARE")) {
                        var profile = new ResolvedAttack.Shot("", java.util.Set.of(ammo), false, false, 1, 20, true, 12);
                        var result = new ResolvedAttack(new UUID(12, ammo.hashCode()), ResolvedAttack.Kind.SHOT,
                              new UnitLocation(attacker.id(), attacker.location().coords(), 0, 0, 0),
                              new UnitLocation(victim.id(), victim.location().coords(), 3, 0, 0), Targetable.TYPE_ENTITY, index,
                              weapon.getType().getInternalName(), weapon.getLocation(), true, ResolvedAttack.captureMounts(atlas, index), profile);
                        var attack = new UnitAttack(new BoardScene.Combat(result, attacker, victim, victim.location()));
                        for (int frame = 0; frame <= 12; frame++) {
                            attack.seconds = .25f + frame * (attack.duration - .25f) / 12;
                            effects.update(attack, library, Map.of("9300:-1", source, "9301:-1", target));
                            renderer.frame(List.of(source, target), position.cpy().lerp(BoardGeometry.center(victim.location().coords(), 0), .5f),
                                  effects, "ammo-" + ammo, frame);
                            assertTrue(effects.smokeCount() <= GpuMissileEffects.SMOKE_BUDGET);
                        }
                    }
                    var events = new java.util.ArrayList<BoardScene.Animation>();
                    for (var gun : atlas.getWeaponList()) {
                        var shot = ResolvedAttack.Shot.capture(gun);
                        var result = new ResolvedAttack(new UUID(0, 100 + gun.getEquipmentNum()), ResolvedAttack.Kind.SHOT,
                              new UnitLocation(attacker.id(), attacker.location().coords(), 0, 0, 0),
                              new UnitLocation(victim.id(), victim.location().coords(), 3, 0, 0), Targetable.TYPE_ENTITY,
                              gun.getEquipmentNum(), gun.getType().getInternalName(), gun.getLocation(), gun.getEquipmentNum() % 2 == 0,
                              ResolvedAttack.captureMounts(atlas, gun.getEquipmentNum()), shot.withResolution(null, shot.missiles() / 2));
                        events.add(new BoardScene.Combat(result, attacker, victim, victim.location()));
                    }
                    var playback = new UnitPlayback();
                    playback.accept(events, UnitPlaybackTest.scene(attacker, victim), ignored -> false);
                    for (int frame = 0; frame < 68; frame++) {
                        playback.advance(1.0 / 60, UnitMotion.Speed.DOUBLE);
                        animator.apply(body, source, attacker, UnitMotion.Sample.STILL, frame / 60f, 1f / 60, false, 0);
                        animator.attacks(body, attacker, playback.attacks());
                        body.place(source, renderer.camera, position, 0, attacker);
                        animator.aimShots(body, attacker, playback.attacks(), ignored -> target);
                        effects.update(playback.attacks(), library, Map.of("9300:-1", source, "9301:-1", target));
                        renderer.frame(List.of(source, target), position.cpy().lerp(BoardGeometry.center(victim.location().coords(), 0), .5f),
                              effects, "atlas-mixed-volley", frame);
                        if (frame == 19) { assertEquals(26, effects.missileCount(), "The Atlas fires LRM20 and SRM6 together"); }
                    }
                    // A new loadout arriving during old-shot recovery must not be replaced again by effects/aim lookups.
                    var recovering = playback.attacks().stream().filter(attack -> attack.event.result().equipmentIndex() == index)
                          .findFirst().orElseThrow();
                    recovering.seconds = recovering.contactSeconds + .05f;
                    atlas.addEquipment(megamek.common.equipment.EquipmentType.get("ISMediumLaser"), megamek.common.units.Mek.LOC_LEFT_ARM);
                    var refit = UnitModelSelection.capture(atlas, -1, false, tileset);
                    var replacement = library.get(refit, atlas.getId());
                    effects.update(List.of(recovering), library, Map.of("9300:-1", new ModelInstance(replacement.instance.model), "9301:-1", target));
                    effects.render(renderer.camera);
                    assertNull(library.loaded(selected, atlas.getId()));
                    assertSame(replacement, library.loaded(refit, atlas.getId()));
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    missiles.dispose();
                    effects.dispose();
                    library.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Missile volley review failed", failure.get()); }
    }

    private static void verifyCurves(UnitAttack attack, Vector3 origin, ModelInstance target, boolean indirect) {
        var original = target.transform.cpy();
        try {
            for (float size : List.of(1f, 3.5f)) {
                target.transform.set(original).scale(size, size, size);
                var bounds = UnitBounds.world(target);
                for (int hits : List.of(0, 1, 10, 12, 20)) {
                    var launch = GpuMissileEffects.capture(attack, new Vector3[] { origin }, target, 20, hits, indirect, 42);
                    int arrived = 0;
                    var hitPoints = new java.util.HashSet<Vector3>();
                    for (int missile = 0; missile < 20; missile++) {
                        if (bounds.contains(GpuMissileEffects.position(launch, missile, 1, new Vector3()))) { arrived++; }
                        if (launch.hit(missile)) { hitPoints.add(launch.targets()[missile]); }
                        if (!launch.hit(missile)) {
                            for (int sample = 1; sample <= 100; sample++) {
                                assertFalse(bounds.contains(GpuMissileEffects.position(launch, missile, sample / 100f, new Vector3())),
                                      "A miss must clear even a large target's bounds throughout its flight");
                            }
                        }
                        if (indirect) {
                            float midpoint = GpuMissileEffects.position(launch, missile, .5f, new Vector3()).z;
                            assertTrue(midpoint > (origin.z + launch.targets()[missile].z) * .5f + BoardGeometry.HEIGHT * .3f);
                        }
                    }
                    assertEquals(hits, arrived, "Only the resolved cluster hits may arrive inside the target");
                    assertEquals(hits, hitPoints.size(), "Successful missiles must land at distinct surface points");
                }
                var event = attack.event;
                var located = new UnitAttack(new BoardScene.Combat(event.result()
                      .withImpacts(List.of(new ResolvedAttack.Impact("LA", false, 5), new ResolvedAttack.Impact("RT", false, 5))),
                      event.attacker(), event.target(), event.destination()));
                var launch = GpuMissileEffects.capture(located, new Vector3[] { origin }, target, 20, 10, indirect, 42);
                int[] locatedHits = { 0, 0 };
                for (int missile = 0; missile < 20; missile++) {
                    if (!launch.hit(missile)) { continue; }
                    for (int loc = 0; loc < 2; loc++) {
                        var part = UnitBounds.subtree(target.getNode(loc == 0 ? "LA" : "RT")).mul(target.transform);
                        if (part.contains(launch.targets()[missile])) { locatedHits[loc]++; }
                    }
                }
                assertEquals(5, locatedHits[0], "Five observed cluster hits reach the left arm");
                assertEquals(5, locatedHits[1], "Five observed cluster hits reach the right torso");
            }
        } finally {
            target.transform.set(original);
        }
    }
}
