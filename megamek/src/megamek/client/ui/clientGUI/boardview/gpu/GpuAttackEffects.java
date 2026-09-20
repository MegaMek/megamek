/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import megamek.common.ResolvedAttack;

/** Bounded procedural effects using the posed assembly's actual muzzle/launcher emitters. No rules or hit tests. */
final class GpuAttackEffects implements Disposable {
    private static final int MAX_EFFECTS = 512;
    private static final int MAX_MUZZLES = 512;
    private final Vector3[] origins = new Vector3[MAX_MUZZLES];
    private final Vector3[] endpoints = new Vector3[MAX_MUZZLES];
    private final String[] effects = new String[MAX_MUZZLES];
    private final boolean[] arcs = new boolean[MAX_MUZZLES];
    private final boolean[] impacts = new boolean[MAX_MUZZLES];
    private final float[] flameSizes = new float[MAX_MUZZLES];
    private final int[] rounds = new int[MAX_MUZZLES];
    private final ResolvedAttack.Shot[] profiles = new ResolvedAttack.Shot[MAX_MUZZLES];
    private final Vector3 target = new Vector3();
    private final Vector3 direction = new Vector3();
    private final Vector3 point = new Vector3();
    private final Vector3 end = new Vector3();
    private final ModelInstance[] particles = new ModelInstance[MAX_EFFECTS];
    private Model model;
    private ModelBatch batch;
    private UnitAttack attack;
    private List<UnitAttack> volley = List.of();
    private GpuUnitModels library;
    private Map<String, ModelInstance> instances = Map.of();
    private final GpuMissileEffects missiles = new GpuMissileEffects();
    private final GpuExhaustBatch flames = new GpuExhaustBatch(2048);
    private final Vector3 flameWidth = new Vector3(), flameLength = new Vector3();
    private Camera camera;
    private record Trace(Vector3 origin, Vector3 target, boolean impact) { }
    private final Map<UnitAttack, Map<String, Trace>> traces = new HashMap<>();
    private final Map<UnitAttack, Map<String, GpuMissileEffects.Launch>> launches = new HashMap<>();
    private int muzzleCount, count;

    void update(UnitAttack next, GpuUnitModels library, Map<String, ModelInstance> instances) {
        update(next == null ? List.of() : List.of(next), library, instances);
    }

    void update(List<UnitAttack> next, GpuUnitModels library, Map<String, ModelInstance> instances) {
        this.volley = next;
        this.library = library;
        this.instances = instances;
        launches.keySet().retainAll(next);
        traces.keySet().retainAll(next);
        missiles.begin();
    }

    private void prepare(UnitAttack next) {
        attack = next;
        muzzleCount = 0;
        if (attack.event.attacker().sensorContact()) { return; }
        var event = attack.event;
        var unit = event.attacker();
        var instance = instances.get(unit.id() + ":" + unit.part());
        var assembly = library == null ? null : library.loaded(unit.model(), unit.id());
        var receiving = event.target();
        var targetInstance = receiving == null ? null : instances.get(receiving.id() + ":" + receiving.part());
        UnitAttack.center(instance, unit.location(), point);
        attack.endpoint(targetInstance, point, target);
        var captured = launches.computeIfAbsent(attack, ignored -> new HashMap<>());
        var paths = traces.computeIfAbsent(attack, ignored -> new HashMap<>());
        captured.values().forEach(missiles::add);
        boolean missileWeapon = !captured.isEmpty();
        if (assembly != null && instance != null) {
            for (var binding : assembly.equipment()) {
                if (captured.containsKey(binding.node())) { continue; }
                var attachment = instance.getNode(binding.node());
                if (!attack.fires(binding) || attachment == null || !UnitBounds.subtree(attachment).isValid()) { continue; }
                var profile = attack.profile(binding);
                int count = profile == null ? 0 : profile.missiles();
                if (profile == null && binding.emitters().stream().anyMatch(emitter -> "missile".equals(emitter.effect())
                      || "cluster".equals(emitter.effect()))) { count = 1; }
                if (count > 0) {
                    missileWeapon = true;
                    if (attack.seconds < UnitAttack.ANTICIPATION_SECONDS) { continue; }
                    List<Vector3> ports = new ArrayList<>();
                    for (var emitter : binding.emitters()) {
                        if (!firingEmitter(emitter) || instance.getNode(emitter.node()) == null) { continue; }
                        var port = new Vector3();
                        UnitModelAttachment.emitter(instance, emitter, port, direction);
                        ports.add(port);
                    }
                    if (ports.isEmpty()) { continue; }
                    int owner = binding.memberId() < 0 ? event.entityId() : binding.memberId();
                    var launch = GpuMissileEffects.capture(attack, ports.toArray(Vector3[]::new), targetInstance, count,
                          attack.missileHits(owner, binding.index(), count), attack.arcing(binding),
                          binding.node().hashCode() ^ event.result().id().hashCode(), profile);
                    captured.put(binding.node(), launch);
                    missiles.add(launch);
                    continue;
                }
                int barrel = 0;
                int barrels = (int) binding.emitters().stream().filter(GpuAttackEffects::firingEmitter).count();
                for (var emitter : binding.emitters()) {
                    if (muzzleCount == MAX_MUZZLES || !firingEmitter(emitter)) { continue; }
                    int shots = profile == null ? 1 : MathUtils.clamp(profile.shots(), 1, 16);
                    int fired = "bullet".equals(emitter.effect()) ? shots * (barrel + 1) / barrels - shots * barrel / barrels : shots;
                    barrel++;
                    if (fired == 0) { continue; }
                    var node = instance.getNode(emitter.node());
                    if (node != null) {
                        String key = binding.node() + ":" + barrel;
                        Trace trace = paths.get(key);
                        if (trace == null) {
                            var start = new Vector3();
                            var finish = new Vector3();
                            UnitModelAttachment.emitter(instance, node, emitter, start, direction);
                            boolean contact;
                            if ("laser".equals(emitter.effect())) {
                                contact = attack.beamEndpoint(targetInstance, start, key.hashCode(), finish);
                            } else {
                                attack.endpoint(targetInstance, start, !event.result().hit(), key.hashCode(), finish);
                                contact = true;
                            }
                            trace = new Trace(start, finish, contact);
                            paths.put(key, trace);
                        }
                        origin(muzzleCount).set(trace.origin());
                        endpoint(muzzleCount).set(trace.target());
                        impacts[muzzleCount] = trace.impact();
                        flameSizes[muzzleCount] = MathUtils.clamp(UnitBounds.subtree(attachment).getDimensions(point).len()
                              * instance.transform.getScaleX() * .5f, 1.5f, 14);
                        arcs[muzzleCount] = attack.arcing(binding);
                        rounds[muzzleCount] = fired;
                        profiles[muzzleCount] = profile;
                        effects[muzzleCount++] = emitter.effect();
                    }
                }
            }
        }
        if (muzzleCount == 0 && !missileWeapon && (assembly == null || instance == null || assembly.equipment().isEmpty())
              && !event.result().mounts().isEmpty()) {
            UnitAttack.center(instance, unit.location(), origin(0));
            for (var mount : event.result().mounts()) {
                var profile = attack.profile(mount.entityId(), mount.equipmentIndex());
                if (profile != null && profile.missiles() > 0 && attack.seconds >= UnitAttack.ANTICIPATION_SECONDS) {
                    String key = mount.entityId() + ":" + mount.equipmentIndex();
                    var launch = captured.computeIfAbsent(key, ignored -> GpuMissileEffects.capture(attack,
                          new Vector3[] { origin(0).cpy() }, targetInstance, profile.missiles(),
                          attack.missileHits(mount.entityId(), mount.equipmentIndex(), profile.missiles()),
                          profile.indirect() || profile.artillery(), key.hashCode() ^ event.result().id().hashCode(), profile));
                    missiles.add(launch);
                    missileWeapon = true;
                }
            }
            if (!missileWeapon) {
                endpoint(0).set(target);
                var profile = event.result().shot();
                arcs[0] = profile != null && (profile.artillery() || profile.indirect());
                rounds[0] = profile == null ? 1 : MathUtils.clamp(profile.shots(), 1, 16);
                profiles[0] = profile;
                impacts[0] = true;
                effects[muzzleCount++] = "bullet";
            }
        }
    }

    private static boolean firingEmitter(UnitModelDescriptor.Emitter emitter) {
        return "muzzle".equals(emitter.role()) || "beam".equals(emitter.role()) || "launcher".equals(emitter.role())
              || "contact".equals(emitter.role());
    }

    private Vector3 endpoint(int index) {
        if (endpoints[index] == null) { endpoints[index] = new Vector3(); }
        return endpoints[index];
    }

    int missileCount() { return missiles.missileCount(); }
    int smokeCount() { return missiles.smokeCount(); }

    float flameSize() {
        float size = 0;
        for (int index = 0; index < muzzleCount; index++) {
            if ("flame".equals(effects[index])) { size = Math.max(size, flameSizes[index]); }
        }
        return size;
    }

    private Vector3 origin(int index) {
        if (origins[index] == null) {
            origins[index] = new Vector3();
        }
        return origins[index];
    }

    void render(Camera camera) {
        if (volley.isEmpty()) { return; }
        ensureResources();
        missiles.begin();
        flames.begin();
        this.camera = camera;
        count = 0;
        batch.begin(camera);
        for (var shot : volley) {
            if (shot.seconds < UnitAttack.ANTICIPATION_SECONDS || shot.seconds >= shot.duration) { continue; }
            prepare(shot);
            renderShot();
        }
        batch.end();
        missiles.render(camera);
        flames.render(camera, 0);
    }

    private void renderShot() {
        if (attack.death()) {
            float progress = attack.deathProgress();
            float burst = MathUtils.sin(MathUtils.clamp(progress * 1.4f, 0, 1) * MathUtils.PI);
            if (burst > 0) { ball(target, 6 + 14 * burst, Color.ORANGE, burst); }
            for (int puff = 0; puff < 16; puff++) {
                float age = progress - puff * .035f;
                if (age <= 0) { continue; }
                float angle = puff * 2.399963f;
                point.set(target).add(MathUtils.cos(angle) * age * 18, MathUtils.sin(angle) * age * 18, age * 22);
                ball(point, 2 + age * 9, Color.DARK_GRAY, .65f * (1 - progress), GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
            return;
        }
        if (attack.shot() && attack.seconds < attack.contactSeconds) {
            for (int index = 0; index < muzzleCount; index++) {
                String effect = effects[index];
                Vector3 start = origins[index];
                Vector3 target = endpoints[index];
                float t = attack.flight();
                switch (effect) {
                    case "laser" -> line(start, target, 1, Color.RED, Math.min(1, t * 8));
                    case "ppc", "energy" -> {
                        point.set(start).lerp(target, t);
                        ball(point, 2.5f, Color.CYAN, 1);
                        end.set(start).lerp(target, Math.max(0, t - .07f));
                        line(end, point, .8f, Color.CYAN, .65f);
                    }
                    case "flame" -> flame(start, target, flameSizes[index], t);
                    case "spray", "screen" -> {
                        Color color = "screen".equals(effect) ? Color.PURPLE : "spray".equals(effect) ? Color.SKY : Color.ORANGE;
                        for (int puff = 0; puff < 6; puff++) {
                            float progress = MathUtils.clamp(t * 1.3f - puff * .06f, 0, 1);
                            point.set(start).lerp(target, progress).add(0, 0, puff * .5f);
                            ball(point, 1.5f + progress * 3, color, .65f);
                        }
                    }
                    default -> {
                        for (int round = 0; round < rounds[index]; round++) {
                            float delay = round * Math.min(.03f, (attack.contactSeconds - UnitAttack.ANTICIPATION_SECONDS) / rounds[index] * .3f);
                            float travel = (attack.seconds - UnitAttack.ANTICIPATION_SECONDS - delay)
                                  / (attack.contactSeconds - UnitAttack.ANTICIPATION_SECONDS - delay);
                            if (travel < 0) { continue; }
                            UnitAttack.projectile(start, target, travel, arcs[index], point);
                            UnitAttack.projectile(start, target, Math.max(0, travel - .07f), arcs[index], end);
                            line(end, point, arcs[index] ? 1 : .55f, Color.YELLOW, 1);
                        }
                    }
                }
                float flash = Math.max(0, 1 - (attack.seconds - UnitAttack.ANTICIPATION_SECONDS) / .12f);
                if (flash > 0) {
                    ball(start, 2.5f * flash, Color.YELLOW, flash);
                }
            }
        }
        float impact = attack.contactFlash();
        if (impact > 0 && (attack.shot() ? !attack.event.result().mounts().isEmpty() : attack.event.result().hit())) {
            var launched = launches.get(attack);
            if (launched != null && !launched.isEmpty()) {
                for (var launch : launched.values()) {
                    // Nearby bursts merge visually; keep their cost bounded independently of rack size.
                    int step = Math.max(1, launch.missiles() / 12);
                    for (int index = 0; index < launch.missiles(); index += step) {
                        impact(launch.targets()[index], impact, launch.profile());
                    }
                }
            }
            if (!attack.shot()) { ball(target, 2 + (1 - impact) * 4, Color.LIGHT_GRAY, impact); }
            else {
                for (int index = 0; index < muzzleCount; index++) {
                    if (impacts[index] && !"flame".equals(effects[index])) { impact(endpoints[index], impact, profiles[index]); }
                }
            }
        }
    }

    /** Continuous turbulent ribbons, batched with the same bounded buffer used by exhaust effects. */
    private void flame(Vector3 start, Vector3 finish, float size, float progress) {
        float reach = Math.min(1, progress * 3);
        direction.set(finish).sub(start);
        flameWidth.set(direction).crs(camera.direction).nor();
        if (flameWidth.isZero()) { flameWidth.set(camera.up); }
        for (int strand = 0; strand < 3; strand++) {
            for (int segment = 0; segment < 8; segment++) {
                float t = segment / 8f * reach;
                float next = (segment + 1) / 8f * reach;
                float pulse = MathUtils.sin(attack.seconds * 35 + segment * 1.7f + strand * 2.1f);
                point.set(start).mulAdd(direction, t).mulAdd(flameWidth, pulse * size * t * .7f)
                      .add(0, 0, t * t * size * 1.2f);
                end.set(start).mulAdd(direction, next).add(0, 0, next * next * size * 1.2f);
                flameLength.set(end).sub(point);
                float width = size * (.3f + t * 3.5f) * (1 + strand * .3f);
                flames.quad(point, flameWidth.scl(width), flameLength, 0, 2 + strand * .2f,
                      .3f * (1 - progress * .45f));
                flameWidth.scl(1 / width);
            }
        }
    }

    /** Transient presentation of the observed munition. Persistent fire, smoke and minefields belong to the board. */
    private void impact(Vector3 center, float fade, ResolvedAttack.Shot profile) {
        var ammo = profile == null ? java.util.Set.<String>of() : profile.munitions();
        boolean smoke = ammo.contains("M_SMOKE") || ammo.contains("M_SMOKE_WARHEAD");
        boolean fire = ammo.contains("M_INFERNO") || ammo.contains("M_INFERNO_IV") || ammo.contains("M_INCENDIARY")
              || ammo.contains("M_INCENDIARY_LRM") || ammo.contains("M_THUNDER_INFERNO");
        if (!smoke) { ball(center, 2 + (1 - fade) * 4, ammo.contains("M_FLARE") ? Color.WHITE : Color.ORANGE, fade); }
        if (smoke || fire) {
            for (int puff = 0; puff < 4; puff++) {
                float angle = puff * 2.399963f, spread = (1 - fade) * 8;
                point.set(center).add(MathUtils.cos(angle) * spread, MathUtils.sin(angle) * spread, spread * (1 + puff * .2f));
                ball(point, 2 + spread * .6f, smoke ? Color.LIGHT_GRAY : Color.DARK_GRAY, fade * .6f, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    private void ensureResources() {
        if (model != null) {
            return;
        }
        model = new ModelBuilder().createSphere(1, 1, 1, 6, 4,
              new Material(ColorAttribute.createDiffuse(Color.WHITE), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 1)),
              VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new ModelInstance(model);
        }
        batch = new ModelBatch();
    }

    private ModelInstance particle(Color color, float alpha) {
        var particle = particles[count++];
        particle.materials.first().get(ColorAttribute.class, ColorAttribute.Diffuse).color.set(color);
        particle.materials.first().get(BlendingAttribute.class, BlendingAttribute.Type).opacity = alpha;
        particle.materials.first().get(BlendingAttribute.class, BlendingAttribute.Type).destFunction = GL20.GL_ONE;
        return particle;
    }

    private void ball(Vector3 position, float size, Color color, float alpha) {
        ball(position, size, color, alpha, GL20.GL_ONE);
    }

    private void ball(Vector3 position, float size, Color color, float alpha, int destinationBlend) {
        if (count == MAX_EFFECTS) {
            return;
        }
        var particle = particle(color, alpha);
        particle.materials.first().get(BlendingAttribute.class, BlendingAttribute.Type).destFunction = destinationBlend;
        particle.transform.setToTranslation(position).scale(size, size, size);
        batch.render(particle);
    }

    private void line(Vector3 start, Vector3 finish, float width, Color color, float alpha) {
        if (count == MAX_EFFECTS || start.epsilonEquals(finish, .001f)) {
            return;
        }
        var particle = particle(color, alpha);
        direction.set(finish).sub(start);
        float length = direction.len();
        particle.transform.setToTranslation((start.x + finish.x) * .5f, (start.y + finish.y) * .5f, (start.z + finish.z) * .5f)
              .rotate(Vector3.Y, direction.nor()).scale(width, length, width);
        batch.render(particle);
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            model.dispose();
            batch = null;
            model = null;
        }
        attack = null;
        volley = List.of();
        instances = Map.of();
        library = null;
        launches.clear();
        traces.clear();
        missiles.dispose();
        flames.dispose();
    }
}
