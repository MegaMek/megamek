/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.ResolvedAttack;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import megamek.common.units.ProneCause;
import megamek.common.units.QuadMek;
import megamek.common.units.Targetable;
import megamek.common.units.TripodMek;
import megamek.common.units.UnitLocation;

/** Native integration proofs, with image sequences from the production pose/placement/effects code. */
final class GpuPlaybackReview {
    private GpuPlaybackReview() { }

    static void verify(GpuUnitModels library, MekTileset tileset, Entity atlas) {
        var selection = UnitModelSelection.capture(atlas, -1, false, tileset);
        var model = library.get(selection, atlas.getId());
        var unit = unit(atlas, selection);
        try (var renderer = new ReviewRenderer()) {
            movementFrames(renderer, model, unit, "atlas-walk", false);
            postureFrames(renderer, library, tileset, atlas);
            attacks(renderer, library, model, unit);
            var members = new ArrayList<Entity>();
            var armor = new BattleArmor();
            armor.setSquadSize(6);
            for (int i = 1; i <= 6; i++) { armor.initializeInternal(1, i); }
            members.add(armor);
            var infantry = new ConvInfantry();
            infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
            members.add(infantry);
            members.add(new QuadMek());
            members.add(new TripodMek());
            int id = 9100;
            for (var entity : members) {
                entity.setId(id++);
                if (entity instanceof Mek) {
                    for (int i = 0; i < entity.locations(); i++) { entity.initializeInternal(10, i); }
                    entity.setWeight(50);
                }
                var selected = UnitModelSelection.capture(entity, -1, false, tileset);
                var body = library.get(selected, entity.getId());
                measureFeet(body, unit(entity, selected));
                withFamilyScale(UnitFamilyScale.forFamily(body.rigs().getFirst().family()),
                      () -> measureFeet(body, unit(entity, selected)));
                if (entity instanceof BattleArmor) {
                    groupJumpFrames(renderer, body, unit(entity, selected));
                }
            }
            infantry.setId(9200);
            infantry.setMovementMode(EntityMovementMode.WHEELED);
            var selected = UnitModelSelection.capture(infantry, -1, false, tileset);
            var transport = library.get(selected, infantry.getId());
            measureVehicles(transport, unit(infantry, selected));
            movementFrames(renderer, transport, unit(infantry, selected), "infantry-transports", true);
            GpuInfantryPlaybackReview.verify(library, tileset);
            withFamilyScale(UnitFamilyScale.INFANTRY, () -> {
                measureVehicles(transport, unit(infantry, selected));
                GpuInfantryPlaybackReview.verify(library, tileset);
            });
        }
    }

    private static void withFamilyScale(UnitFamilyScale family, Runnable review) {
        float size = family.UNIT_SCALE, height = family.HEIGHT_SCALE;
        try {
            family.UNIT_SCALE = size * 1.25f;
            family.HEIGHT_SCALE = height * 1.4f;
            review.run();
        } finally {
            family.UNIT_SCALE = size;
            family.HEIGHT_SCALE = height;
        }
    }

    private static void postureFrames(ReviewRenderer renderer, GpuUnitModels library, MekTileset tileset, Entity atlas) {
        ProneCause original = atlas.getProneCause();
        try {
            for (ProneCause cause : List.of(ProneCause.VOLUNTARY, ProneCause.FORCED, ProneCause.NONE)) {
                atlas.setProne(cause);
                var selection = UnitModelSelection.capture(atlas, -1, false, tileset);
                var model = library.get(selection, atlas.getId());
                var unit = unit(atlas, selection);
                var from = unit.location().withProneCause(cause == ProneCause.NONE ? ProneCause.FORCED : ProneCause.NONE);
                var to = unit.location().withProneCause(cause);
                var motion = new UnitMotion(from);
                motion.append(List.of(from, to), EntityMovementType.MOVE_NONE, 0);
                var instance = new ModelInstance(model.instance.model);
                var animator = new UnitAnimator();
                for (int frame = 0; frame <= 8; frame++) {
                    motion.advance(frame == 0 ? 0 : UnitMotion.POSTURE_SECONDS / 8, 1);
                    animator.apply(model, instance, unit, motion.sample(), 0, 0, false, 0);
                    model.place(instance, renderer.camera, motion.position(), 0, unit);
                    renderer.frame(List.of(instance), motion.position(), null, "posture-" + cause.name().toLowerCase(java.util.Locale.ROOT), frame);
                }
                assertEquals(cause, motion.sample().proneCause());
                assertFalse(motion.isMoving());
            }
        } finally {
            atlas.setProne(original);
        }
    }

    private static void groupJumpFrames(ReviewRenderer renderer, GpuMeeple model, BoardScene.Unit unit) {
        var start = new BoardScene.Waypoint(new Coords(2, 3), 0, 0);
        var motion = new UnitMotion(start);
        motion.append(List.of(start, unit.location()), EntityMovementType.MOVE_JUMP, 2, false, 2, 6);
        var instance = new ModelInstance(model.instance.model);
        var animator = new UnitAnimator();
        var jets = new GpuJumpJets();
        int frames = (int) Math.ceil((motion.remainingSeconds() / UnitMotion.Speed.NORMAL.rate + 1) * 24);
        boolean mixedTakeoff = false, mixedLanding = false;
        try {
            for (int frame = 0; frame <= frames; frame++) {
                motion.advance(frame == 0 ? 0 : 1.0 / 24, UnitMotion.Speed.NORMAL.rate);
                var sample = motion.sample();
                animator.apply(model, instance, unit, sample, frame / 48f, 1f / 48, false, 0);
                model.place(instance, renderer.camera, motion.position(), 0, unit);
                jets.beginFrame();
                jets.update("stagger-review", model, instance, unit, sample);
                jets.endFrame();
                int flying = 0;
                for (var rig : model.rigs()) {
                    var member = sample.member(unit.id(), rig.container());
                    if (member.jets() != null) { flying++; }
                    if (!member.moving()) {
                        var bounds = UnitBounds.subtree(instance.getNode(rig.container()));
                        float floor = new Vector3(0, 0, bounds.min.z).mul(instance.transform).z;
                        assertEquals(.5f, floor, .02f, "Waiting/landed suits remain on the ground while other suits fly");
                    }
                }
                assertEquals(flying * 2, jets.emitterCount(), "Each suit's nozzles follow its own takeoff and landing");
                if (flying > 0 && flying < 6) {
                    mixedTakeoff |= sample.progress() < .5f;
                    mixedLanding |= sample.progress() > .5f;
                }
                renderer.frame(List.of(instance), new Vector3(motion.position().x, motion.position().y, 0),
                      null, jets, "battle-armor-staggered-jump", frame);
            }
            assertTrue(mixedTakeoff && mixedLanding, "The native reel must include staggered departures and arrivals");
            assertEquals(0, jets.emitterCount());
            var skipped = new ModelInstance(model.instance.model);
            new UnitAnimator().apply(model, skipped, unit, UnitMotion.Sample.STILL, 0, 0, true, 0);
            for (var rig : model.rigs()) {
                assertEquals(skipped.getNode(rig.container()).translation, instance.getNode(rig.container()).translation);
                assertEquals(skipped.getNode(rig.container()).rotation, instance.getNode(rig.container()).rotation);
            }
            motion.append(List.of(start, unit.location()), EntityMovementType.MOVE_JUMP, 2, false, 2, 6);
            motion.advance(.08, 1);
            animator.apply(model, instance, unit, motion.sample(), 1, .08f, false, 0);
            motion.finish();
            animator.apply(model, instance, unit, motion.sample(), 1, 0, true, 0);
            for (var rig : model.rigs()) {
                assertEquals(skipped.getNode(rig.container()).translation, instance.getNode(rig.container()).translation,
                      "Skipping during staggered takeoff clears every child's temporary offset");
                assertEquals(skipped.getNode(rig.container()).rotation, instance.getNode(rig.container()).rotation);
            }
        } finally {
            jets.dispose();
        }
    }

    private static void measureVehicles(GpuMeeple model, BoardScene.Unit unit) {
        var instance = new ModelInstance(model.instance.model);
        var animator = new UnitAnimator();
        var camera = new OrthographicCamera();
        var motion = new UnitMotion(new BoardScene.Waypoint(new Coords(2, 5), 0, 0));
        var previousPosition = new HashMap<String, Vector3>();
        var previousAngle = new HashMap<String, Float>();
        float driven = 0, rolled = 0;
        // Two consecutive routes exercise opposite approach directions, parking, and per-event wheel accumulation.
        for (int trip = 0; trip < 2; trip++) {
            var start = new BoardScene.Waypoint(new Coords(2, trip == 0 ? 5 : 1), 0, 0);
            var finish = new BoardScene.Waypoint(new Coords(2, trip == 0 ? 1 : 5), 0, 0);
            motion.append(List.of(start, finish), EntityMovementType.MOVE_WALK, 0, true, 4);
            float dt = (float) (motion.remainingSeconds() / 600);
            for (int frame = 0; frame < 600; frame++) {
                motion.advance(frame == 0 ? 0 : dt, 1);
                var sample = motion.sample();
                animator.apply(model, instance, unit, sample, frame * dt, dt, false, 0);
                model.place(instance, camera, motion.position(), 0, unit);
                for (var rig : model.rigs()) {
                    var container = instance.getNode(rig.container());
                    if (rig.trooper()) {
                        if (sample.boarding().stage() == UnitMotion.Stage.DRIVE) {
                            assertFalse(UnitBounds.subtree(container).isValid(), "Passengers remain inside until parking completes");
                        }
                        continue;
                    }
                    var wheelId = rig.joints().entrySet().stream().filter(entry -> entry.getKey().startsWith("wheel-"))
                          .findFirst().orElseThrow().getValue();
                    var wheel = UnitAnimator.find(container.getChildren(), wheelId);
                    var restWheel = UnitAnimator.find(model.instance.getNode(rig.container()).getChildren(), wheelId);
                    float radius = UnitBounds.subtree(restWheel).getDepth() * .5f * model.horizontalScale(unit);
                    float angle = wheel.rotation.getAngleAround(Vector3.X);
                    var position = container.globalTransform.getTranslation(new Vector3()).mul(instance.transform);
                    var previous = previousPosition.put(rig.container(), position);
                    Float rotation = previousAngle.put(rig.container(), angle);
                    if (frame > 0 && previous != null) {
                        float distance = position.dst(previous);
                        float rolling = Math.abs(UpperBodyTurn.shortestTurn(angle - rotation))
                              * com.badlogic.gdx.math.MathUtils.degreesToRadians * radius;
                        if (sample.boarding().stage() == UnitMotion.Stage.UNLOAD) {
                            assertEquals(0, distance, .02f, "Vehicles remain parked while troops unload");
                        } else {
                            driven += distance;
                            rolled += rolling;
                        }
                    }
                }
            }
            motion.advance(dt * 2, 1);
        }
        System.out.println("Transport wheel rolling / actual path distance = " + rolled / driven);
        assertTrue(driven > 100);
        assertEquals(1, rolled / driven, .015f, "Wheel rotation follows the vehicle's curved path, including reversing");
    }

    private static BoardScene.Unit unit(Entity entity, BoardScene.UnitModel model) {
        return new BoardScene.Unit(entity.getId(), -1, entity.getShortName(), new BoardScene.Waypoint(new Coords(2, 1), 0, 0),
              null, false, null, 3, false, model, 0);
    }

    private static void measureFeet(GpuMeeple model, BoardScene.Unit unit) {
        for (var rig : model.rigs()) {
            var feet = rig.joints().entrySet().stream().filter(joint -> joint.getKey().endsWith("Foot"))
                  .map(Map.Entry::getValue).toList();
            if (feet.size() < 2) { continue; }
            var instance = new ModelInstance(model.instance.model);
            var animator = new UnitAnimator();
            var motion = new UnitMotion(new BoardScene.Waypoint(new Coords(2, 5), 0, 0));
            motion.append(List.of(new BoardScene.Waypoint(new Coords(2, 5), 0, 0), unit.location()), EntityMovementType.MOVE_WALK, 0, false, 6,
                  rig.trooper() ? model.rigs().size() : 0);
            float dt = (float) (motion.remainingSeconds() / 480);
            var camera = new OrthographicCamera();
            var previous = new Vector3();
            var previousRoot = new Vector3();
            String previousFoot = "";
            float drift = 0, travel = 0;
            for (int frame = 0; frame < 480; frame++) {
                motion.advance(dt, 1);
                animator.apply(model, instance, unit, motion.sample(), frame * dt, dt, false, 0);
                model.place(instance, camera, motion.position(), 0, unit);
                var container = rig.container() == null ? instance.nodes : instance.getNode(rig.container()).getChildren();
                Vector3 planted = null;
                String supporting = "";
                for (String foot : feet) {
                    var position = UnitAnimator.find(container, foot).globalTransform.getTranslation(new Vector3()).mul(instance.transform);
                    if (planted == null || position.z < planted.z) { planted = position; supporting = foot; }
                }
                var individual = motion.sample().member(unit.id(), rig.container());
                var memberOrigin = rig.container() == null ? instance.transform.getTranslation(new Vector3())
                      : instance.getNode(rig.container()).globalTransform.getTranslation(new Vector3()).mul(instance.transform);
                if (frame > 0 && individual.progress() > .15f && individual.progress() < .85f
                      && supporting.equals(previousFoot) && Math.abs(planted.z - previous.z) < .02f) {
                    drift += (float) Math.hypot(planted.x - previous.x, planted.y - previous.y);
                    travel += (float) Math.hypot(memberOrigin.x - previousRoot.x, memberOrigin.y - previousRoot.y);
                }
                previous.set(planted);
                previousRoot.set(memberOrigin);
                previousFoot = supporting;
            }
            System.out.println(rig.type() + " " + rig.container() + " foot drift / travel = " + drift / travel);
            assertTrue(travel > 10 && drift / travel < .08f, rig.type() + " " + rig.container() + " drift ratio " + drift / travel);
        }
    }

    private static void movementFrames(ReviewRenderer renderer, GpuMeeple model, BoardScene.Unit unit, String name, boolean transports) {
        var start = new BoardScene.Waypoint(new Coords(2, 5), 0, 0);
        var motion = new UnitMotion(start);
        motion.append(List.of(start, unit.location()), EntityMovementType.MOVE_WALK, 0, transports, 4);
        var instance = new ModelInstance(model.instance.model);
        var animator = new UnitAnimator();
        double duration = motion.remainingSeconds() / UnitMotion.Speed.NORMAL.rate + UnitPlayback.COMPLETION_HOLD_SECONDS;
        int frames = (int) Math.ceil(duration * 24);
        for (int frame = 0; frame <= frames; frame++) {
            motion.advance(frame == 0 ? 0 : 1.0 / 24, UnitMotion.Speed.NORMAL.rate);
            animator.apply(model, instance, unit, motion.sample(), frame / 48f, 1f / 48, false, 0);
            model.place(instance, renderer.camera, motion.position(), transports ? 0 : motion.facing(), unit);
            renderer.frame(List.of(instance), motion.position(), null, name, frame);
        }
    }

    private static void attacks(ReviewRenderer renderer, GpuUnitModels library, GpuMeeple model, BoardScene.Unit unit) {
        var attacker = new ModelInstance(model.instance.model);
        var target = new ModelInstance(model.instance.model);
        var animator = new UnitAnimator();
        var victim = new BoardScene.Unit(unit.id() + 1, -1, "Target", new BoardScene.Waypoint(new Coords(2, 0), 0, 3),
              null, false, null, 3, false, unit.model(), 0);
        var origin = BoardGeometry.center(unit.location().coords(), 0);
        model.place(target, renderer.camera, BoardGeometry.center(victim.location().coords(), 0), 180, victim);
        var effects = new GpuAttackEffects();
        try {
            int index = model.equipment().stream().filter(binding -> binding.emitters().stream()
                  .anyMatch(emitter -> "laser".equals(emitter.effect()))).findFirst().orElseThrow().index();
            for (var kind : ResolvedAttack.Kind.values()) {
                int location = kind == ResolvedAttack.Kind.KICK ? Mek.LOC_LEFT_LEG : Mek.LOC_LEFT_ARM;
                var result = new ResolvedAttack(UUID.randomUUID(), kind,
                      new UnitLocation(unit.id(), unit.location().coords(), 0, 0, 0),
                      new UnitLocation(victim.id(), victim.location().coords(), 3, 0, 0), Targetable.TYPE_ENTITY,
                      index, "ISMediumLaser", location, true);
                var combat = new BoardScene.Combat(result, unit, victim, victim.location());
                var attack = new UnitAttack(combat);
                for (int frame = 0; frame < 50; frame++) {
                    attack.seconds = Math.min(attack.duration, frame / 48f);
                    animator.apply(model, attacker, unit, UnitMotion.Sample.STILL, frame / 48f, 1f / 48, false, 0);
                    animator.attack(model, unit, attack);
                    model.place(attacker, renderer.camera, origin, 0, unit);
                    effects.update(attack, library, Map.of(unit.id() + ":-1", attacker, victim.id() + ":-1", target));
                    renderer.frame(List.of(attacker, target), origin.cpy().add(0, BoardGeometry.HEIGHT * .4f, 0), effects,
                          "attack-" + kind.name().toLowerCase(java.util.Locale.ROOT), frame);
                }
                animator.apply(model, attacker, unit, UnitMotion.Sample.STILL, 2, .1f, false, 0);
                animator.attack(model, unit, null);
                for (var binding : model.equipment()) {
                    assertEquals(model.instance.getNode(binding.node()).translation, attacker.getNode(binding.node()).translation,
                          "Cancelled/completed attacks cannot leave a displaced mount");
                }
            }
        } finally {
            effects.dispose();
        }
    }

    private static final class ReviewRenderer implements AutoCloseable {
        final OrthographicCamera camera = new OrthographicCamera(180, 135);
        final ModelBatch batch = new ModelBatch();
        final ShapeRenderer lines = new ShapeRenderer();
        final Environment light = new Environment();
        final FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 640, 480, true);

        ReviewRenderer() {
            camera.near = 1;
            camera.far = 2000;
            light.set(ColorAttribute.createAmbientLight(.65f, .65f, .65f, 1));
            light.add(new DirectionalLight().set(.8f, .8f, .8f, -.3f, -.5f, -1));
        }

        void frame(List<ModelInstance> models, Vector3 origin, GpuAttackEffects effects, String name, int frame) {
            frame(models, origin, effects, null, name, frame);
        }

        void frame(List<ModelInstance> models, Vector3 origin, GpuAttackEffects effects, GpuJumpJets jets, String name, int frame) {
            buffer.begin();
            camera.position.set(origin).add(95, -150, 105);
            camera.up.set(Vector3.Z);
            camera.lookAt(origin.x, origin.y, origin.z + 22);
            camera.update();
            Gdx.gl.glClearColor(.15f, .19f, .23f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            lines.setProjectionMatrix(camera.combined);
            lines.begin(ShapeRenderer.ShapeType.Line);
            lines.setColor(.33f, .37f, .39f, 1);
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 8; y++) {
                    var center = BoardGeometry.center(new Coords(x, y), 0);
                    for (int edge = 0; edge < 6; edge++) {
                        float a = edge * 60 * com.badlogic.gdx.math.MathUtils.degreesToRadians;
                        float b = (edge + 1) * 60 * com.badlogic.gdx.math.MathUtils.degreesToRadians;
                        lines.line(center.x + 42 * (float) Math.cos(a), center.y + 42 * (float) Math.sin(a), 0,
                              center.x + 42 * (float) Math.cos(b), center.y + 42 * (float) Math.sin(b), 0);
                    }
                }
            }
            lines.end();
            batch.begin(camera);
            models.forEach(model -> batch.render(model, light));
            batch.end();
            if (effects != null) { effects.render(camera); }
            if (jets != null) { jets.render(camera); }
            File output = new File(System.getProperty("megamek.gpu.screenshots"), "playback-" + name);
            assertTrue(output.isDirectory() || output.mkdirs());
            var pixels = Pixmap.createFromFrameBuffer(0, 0, 640, 480);
            PixmapIO.writePNG(new FileHandle(new File(output, String.format("%03d.png", frame))), pixels, -1, true);
            pixels.dispose();
            assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
            buffer.end();
        }

        @Override
        public void close() {
            buffer.dispose();
            lines.dispose();
            batch.dispose();
        }
    }
}
