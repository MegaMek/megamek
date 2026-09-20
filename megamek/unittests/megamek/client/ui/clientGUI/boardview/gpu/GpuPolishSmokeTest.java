/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

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
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.FallSide;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.ProneCause;
import megamek.common.units.QuadMek;
import megamek.common.units.QuadVee;
import megamek.common.units.Targetable;
import megamek.common.units.TripodMek;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("on-demand")
class GpuPolishSmokeTest {
    @Test
    void renderDamageConversionsFallsAndFlamersWithProductionMaterials() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override public void create() {
                var library = new GpuUnitModels();
                var damage = new UnitDamageDisplay();
                var camo = new GpuUnitCamouflage();
                try (var renderer = new GpuPlaybackReview.ReviewRenderer()) {
                    var tileset = new MekTileset(Configuration.unitImagesDir());
                    tileset.loadFromFile("mekset.txt");
                    var atlas = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
                    atlas.setId(9100);
                    damage(renderer, library, damage, camo, tileset, atlas);
                    falls(renderer, library, tileset, atlas);
                    conversions(renderer, library, tileset);
                    fallbacks(renderer, library, tileset);
                    effects(renderer, library, tileset);
                } catch (Throwable error) { failure.set(error); }
                finally { camo.dispose(); damage.dispose(); library.dispose(); Gdx.app.exit(); }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Animation/damage native review", failure.get()); }
    }

    private static BoardScene.Unit unit(Entity entity, MekTileset tileset) {
        return new BoardScene.Unit(entity.getId(), -1, entity.getShortNameRaw(),
              new BoardScene.Waypoint(new Coords(2, 3), 0, 0, entity.getProneCause()).withFallSide(entity.getFallSide()),
              null, false, null, 2, false, UnitModelSelection.capture(entity, -1, false, tileset), 0);
    }

    private static void damage(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModels library, UnitDamageDisplay damage,
          GpuUnitCamouflage camo, MekTileset tileset, Entity atlas) {
        var unit = unit(atlas, tileset);
        for (int stage = -1; stage < UnitDamageDisplay.Stage.values().length; stage++) {
            var selection = stage < 3 ? unit.model() : GpuFamilyAssemblyReview.selection("tracked", List.of());
            var model = library.get(selection, unit.id());
            var instance = new ModelInstance(model.instance.model);
            camo.apply(instance, model.instance, selection.state().appearance());
            String name = stage < 0 ? "intact" : UnitDamageDisplay.Stage.values()[stage].file;
            if (stage >= 0) {
                var level = UnitDamageDisplay.Stage.values()[stage];
                var snapshot = new BoardScene.LocationDamage(Set.of(), Set.of(), Map.of(stage < 3 ? "LT" : "*", level));
                damage.applyTexture(instance, snapshot, unit.id());
                String location = stage < 3 ? "LT" : "hull";
                assertFalse(UnitDamageDisplay.locationParts(model.instance, location).getFirst().material.has(UnitDamageDisplay.Overlay.TYPE));
                assertTrue(UnitDamageDisplay.locationParts(instance, location).getFirst().material.has(UnitDamageDisplay.Overlay.TYPE));
                if (stage < 3) { assertFalse(UnitDamageDisplay.locationParts(instance, "RT").getFirst().material.has(UnitDamageDisplay.Overlay.TYPE)); }
                if (level == UnitDamageDisplay.Stage.BODY_100) {
                    var material = instance.getNode("hull").parts.first().material;
                    damage.wreck(instance, model, unit.id());
                    assertSame(material, instance.getNode("hull").parts.first().material, "A wreck must reuse its final damage material");
                }
            }
            var origin = BoardGeometry.center(unit.location().coords(), 0);
            model.place(instance, renderer.camera, origin, 180, unit);
            renderer.frame(List.of(instance), origin, null, "polish-damage-" + name, 0);
        }
    }

    private static void falls(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModels library, MekTileset tileset, Entity atlas) {
        var unit = unit(atlas, tileset);
        var model = library.get(unit.model(), unit.id());
        var origin = BoardGeometry.center(unit.location().coords(), 0);
        for (FallSide side : FallSide.values()) {
            var start = unit.location();
            var down = start.withProneCause(ProneCause.FORCED).withFallSide(side);
            var motion = new UnitMotion(start);
            motion.append(List.of(start, down, start), EntityMovementType.MOVE_WALK, 0);
            var animator = new UnitAnimator();
            var instance = new ModelInstance(model.instance.model);
            Vector3 center = null;
            for (int frame = 0; frame <= 64; frame++) {
                motion.advance(frame == 0 ? 0 : UnitMotion.POSTURE_SECONDS / 32, 1);
                animator.apply(model, instance, unit, motion.sample(), 0, (float) UnitMotion.POSTURE_SECONDS / 32, false, 0);
                model.place(instance, renderer.camera, motion.position(), motion.facing(), unit);
                var bounds = UnitBounds.world(instance);
                assertEquals(.5f, bounds.min.z, .02f);
                var current = bounds.getCenter(new Vector3());
                if (center == null) { center = current.cpy(); }
                assertTrue(Math.hypot(current.x - center.x, current.y - center.y) < 1, "Fall stays centered in its hex");
                if (frame % 2 == 0) { renderer.frame(List.of(instance), origin, null, "polish-fall-" + side, frame); }
            }
        }
    }

    private static void conversions(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModels library, MekTileset tileset) {
        for (Mek entity : List.of(new QuadVee(), new LandAirMek(0, 0, LandAirMek.LAM_STANDARD))) {
            entity.setId(9120);
            entity.setWeight(50);
            for (int loc = 0; loc < entity.locations(); loc++) { entity.initializeInternal(10, loc); }
            var before = unit(entity, tileset);
            for (int mode : entity instanceof QuadVee ? new int[] { 1, 0 } : new int[] { 1, 2, 1, 0 }) {
                entity.setConversionMode(mode);
                var after = unit(entity, tileset);
                var conversion = new UnitConversion(new BoardScene.Conversion(0, before, after));
                if (entity instanceof QuadVee) {
                    assertSame(library.get(before.model(), entity.getId()), library.get(after.model(), entity.getId()),
                          "QuadVee conversion must reuse the same assembly and buffers");
                }
                var animator = new UnitAnimator();
                for (int frame = 0; frame <= 32; frame++) {
                    conversion.seconds = UnitConversion.DURATION_SECONDS * frame / 32;
                    var shown = conversion.displayed();
                    var model = library.get(shown.model(), shown.id());
                    assertNotNull(model);
                    var instance = new ModelInstance(model.instance.model);
                    animator.apply(model, instance, shown, UnitMotion.Sample.STILL, 0, 0, true, 0);
                    animator.conversion(conversion, shown);
                    var origin = BoardGeometry.center(shown.location().coords(), 0);
                    model.place(instance, renderer.camera, origin, 180, shown);
                    renderer.frame(List.of(instance), origin, null,
                          "polish-conversion-" + entity.getClass().getSimpleName() + "-" + UnitConversion.form(before).mode() + "-" + mode, frame);
                }
                before = after;
            }
        }
    }

    private static void effects(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModels library, MekTileset tileset) throws Exception {
        float normalFlame = 0;
        for (String weapon : List.of("Flamer", "Heavy Flamer", "ISMediumLaser", "LRM 20")) {
            var atlas = new MekFileParser(new File("testresources/megamek/common/units/Atlas AS7-D.mtf")).getEntity();
            atlas.setId(9100);
            var mount = atlas.addEquipment(EquipmentType.get(weapon), Mek.LOC_LEFT_ARM);
            var unit = unit(atlas, tileset);
            var model = library.get(unit.model(), unit.id());
            var target = new BoardScene.Unit(9199, -1, "Target", new BoardScene.Waypoint(new Coords(2, 1), 1, 3),
                  null, false, null, 2, false, unit.model(), 0);
            var a = new ModelInstance(model.instance.model);
            var b = new ModelInstance(model.instance.model);
            var origin = BoardGeometry.center(unit.location().coords(), 0);
            model.place(b, renderer.camera, BoardGeometry.center(target.location().coords(), 1), 180, target);
            for (int hits : "LRM 20".equals(weapon) ? new int[] { 0, 20, 10 } : new int[] { 0, 1 }) {
                boolean hit = hits > 0;
                var shot = ResolvedAttack.Shot.capture(mount);
                if (shot.missiles() > 0) { shot = shot.withResolution(null, hits); }
                String outcome = hit && hits < shot.missiles() ? hits + "-hits" : Boolean.toString(hit);
                var event = new ResolvedAttack(new UUID(4, 301), ResolvedAttack.Kind.SHOT,
                      new UnitLocation(unit.id(), unit.location().coords(), 0, 0, 0),
                      new UnitLocation(target.id(), target.location().coords(), 3, 0, 0), Targetable.TYPE_ENTITY,
                      mount.getEquipmentNum(), weapon, Mek.LOC_LEFT_ARM, hit,
                      List.of(new ResolvedAttack.Mount(unit.id(), mount.getEquipmentNum())), shot);
                if (hit) {
                    event = event.withImpacts(shot.missiles() > 0
                          ? hits == 10 ? List.of(new ResolvedAttack.Impact("LA", false, 5), new ResolvedAttack.Impact("RT", false, 5))
                                : List.of(new ResolvedAttack.Impact("LA", false, 5), new ResolvedAttack.Impact("RT", false, 5),
                                      new ResolvedAttack.Impact("CT", false, 10))
                          : List.of(new ResolvedAttack.Impact("RT", false, 1)));
                }
                var attack = new UnitAttack(new BoardScene.Combat(event, unit, target, target.location()));
                var scene = GpuFamilyMotionReview.ramp();
                var terrain = GpuFamilyMotionReview.terrain(scene);
                attack.landscape = ray -> BoardGeometry.hit(scene, ray);
                var effects = new GpuAttackEffects();
                var animator = new UnitAnimator();
                try {
                    for (int frame = 0; frame <= 32; frame++) {
                        attack.seconds = attack.duration * frame / 32;
                        animator.apply(model, a, unit, UnitMotion.Sample.STILL, 0, 0, true, 0);
                        animator.attack(model, unit, attack);
                        model.place(a, renderer.camera, origin, 0, unit);
                        animator.aim(model, unit, attack, attack.endpoint(b, origin, new Vector3()), b);
                        effects.update(attack, library, Map.of(unit.id() + ":-1", a, target.id() + ":-1", b));
                        renderer.frame(List.of(terrain, a, b), origin.cpy().lerp(BoardGeometry.center(target.location().coords(), 1), .5f), effects,
                              "polish-shot-" + weapon.replace(' ', '-') + "-" + outcome, frame);
                        if (shot.missiles() > 0 && frame == 12) {
                            assertEquals(20, effects.missileCount(), "Cluster hits do not reduce the launched rack size");
                            assertEquals(hits, attack.missileHits(unit.id(), mount.getEquipmentNum(), 20));
                        }
                        if ("ISMediumLaser".equals(weapon) && frame == 12) {
                            var image = ImageIO.read(new File(System.getProperty("megamek.gpu.screenshots"),
                                  "playback-polish-shot-ISMediumLaser-" + outcome + "/012.png"));
                            int redPixels = 0;
                            for (int y = 0; y < image.getHeight(); y++) {
                                for (int x = 0; x < image.getWidth(); x++) {
                                    int color = image.getRGB(x, y);
                                    int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
                                    if (red > 180 && red > green * 1.5f && red > blue * 1.5f) { redPixels++; }
                                }
                            }
                            assertTrue(redPixels > 100, "The beam must be visible for both hits and long off-board misses: " + redPixels);
                        }
                        if (frame == 12 && hit) {
                            if ("Flamer".equals(weapon)) { normalFlame = effects.flameSize(); assertTrue(normalFlame > 0); }
                            if ("Heavy Flamer".equals(weapon)) {
                                assertTrue(effects.flameSize() > normalFlame * 1.05f,
                                      "The heavy flamer must have a larger plume: " + effects.flameSize() + " vs " + normalFlame);
                            }
                        }
                        if (weapon.contains("Flamer")) {
                            if (frame == 20) {
                                assertTrue(attack.seconds > attack.contactSeconds);
                                assertTrue(effects.flameParticleCount() > 0,
                                      "Traveling flame packets must finish burning after contact instead of switching off like a beam");
                            }
                            if (frame == 32) { assertEquals(0, effects.flameParticleCount(), "Completed flame effects leave no particles"); }
                        }
                    }
                } finally { effects.dispose(); terrain.model.dispose(); }
            }
        }
    }

    private static void fallbacks(GpuPlaybackReview.ReviewRenderer renderer, GpuUnitModels library, MekTileset tileset) {
        for (Mek entity : List.of(new BipedMek(), new TripodMek(), new QuadMek())) {
            entity.setId(9160);
            for (int loc = 0; loc < entity.locations(); loc++) { entity.initializeInternal(10, loc); }
            float previousHeight = 0, previousWidth = 0;
            var paths = new java.util.HashSet<String>();
            for (int weight : new int[] { 20, 50, 70, 100, 150 }) {
                entity.setWeight(weight);
                var unit = unit(entity, tileset);
                assertTrue(paths.add(unit.model().asset()), "Each weight class must select a distinct authored body");
                var model = library.get(unit.model(), entity.getId());
                assertNotNull(model, unit.model().asset());
                assertEquals(new Vector3(1, 1, 1), model.instance.getNode("root").scale,
                      "Authored fallbacks must not be resized by a second runtime weight multiplier");
                assertEquals(UnitFamilyScale.MEK, UnitFamilyScale.forFamily(model.rigs().getFirst().family()));
                var dimensions = UnitBounds.world(model.instance).getDimensions(new Vector3());
                assertTrue(dimensions.z > previousHeight && dimensions.x > previousWidth);
                previousHeight = dimensions.z;
                previousWidth = dimensions.x;
                var origin = BoardGeometry.center(unit.location().coords(), 0);
                var instance = new ModelInstance(model.instance.model);
                model.place(instance, renderer.camera, origin, 180, unit);
                renderer.frame(List.of(instance), origin, null,
                      "polish-fallback-" + model.rigs().getFirst().type() + "-" + weight, 0);
                entity.setProne(ProneCause.VOLUNTARY);
                var crouch = new ModelInstance(model.instance.model);
                new UnitAnimator().apply(model, crouch, unit(entity, tileset), UnitMotion.Sample.STILL, 0, 0, true, 0);
                assertTrue(UnitBounds.local(crouch).getDepth() < dimensions.z * .9f,
                      "Crouching must lower the body, including reverse-knee scouts: " + unit.model().asset());
                entity.setProne(ProneCause.NONE);
            }
        }
    }
}
