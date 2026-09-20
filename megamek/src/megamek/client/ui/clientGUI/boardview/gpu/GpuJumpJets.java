/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/** Visible jump exhaust only. Borrowed posed emitters feed a bounded world-space trail on the unit timeline. */
final class GpuJumpJets implements Disposable {
    static final int MAX_EMITTERS = 128;
    static final int PUFFS_PER_EMITTER = 64;
    private static final float EMISSION_STEP = .0125f;
    private static final float SMOKE_LIFE = .7f;
    private static final int MAX_QUADS = MAX_EMITTERS * (PUFFS_PER_EMITTER + 1);
    private final Map<String, Jet> jets = new LinkedHashMap<>();
    private final List<Puff> sortedSmoke = new ArrayList<>();
    private final Vector3 right = new Vector3(), across = new Vector3(), along = new Vector3();
    private final GpuEffectBatch batch = new GpuEffectBatch(MAX_QUADS);

    private static final class Puff {
        final Vector3 origin, velocity;
        final Vector3 center = new Vector3();
        final float birth, width, strength;
        float radius, alpha, depth;

        Puff(Vector3 origin, Vector3 velocity, float birth, float width, float strength) {
            this.origin = origin;
            this.velocity = velocity;
            this.birth = birth;
            this.width = width;
            this.strength = strength;
        }
    }

    private static final class Jet {
        final ArrayDeque<Puff> smoke = new ArrayDeque<>();
        final Vector3 position = new Vector3(), previous = new Vector3(), direction = new Vector3();
        final int seed;
        long sequence = -1;
        float time, previousTime = -1, width;
        int tick;
        boolean seen;
        UnitMotion.JumpJets power;

        Jet(int seed) {
            this.seed = seed;
        }

        void update(ModelInstance instance, Node node, UnitModelDescriptor.Emitter emitter,
              UnitMotion.Sample motion, float nozzleWidth) {
            power = motion.jets();
            time = power.seconds();
            UnitModelAttachment.emitter(instance, node, emitter, position, direction);
            width = nozzleWidth * node.globalTransform.getScaleX() * instance.transform.getScaleX();
            if (sequence != power.sequence() || time < previousTime) {
                smoke.clear();
                previousTime = -1;
                sequence = power.sequence();
            }
            if (previousTime < 0) {
                // Newly revealed units never invent a trail through an unobserved portion of the path.
                previousTime = time;
                previous.set(position);
                tick = (int) Math.ceil(time / EMISSION_STEP);
            }
            smoke.removeIf(puff -> time - puff.birth >= SMOKE_LIFE);
            while (tick * EMISSION_STEP <= time + .00001f) {
                float birth = tick * EMISSION_STEP;
                float strength = power.smoke(birth);
                int random = seed + tick++ * 7919;
                if (noise(random) < strength && time - birth < SMOKE_LIFE) {
                    float fraction = time == previousTime ? 1 : MathUtils.clamp((birth - previousTime) / (time - previousTime), 0, 1);
                    var origin = new Vector3(previous).lerp(position, fraction).mulAdd(direction, width * 3);
                    var velocity = new Vector3(noise(random + 1) - .5f, noise(random + 2) - .5f, .25f)
                          .scl(width * 3).mulAdd(direction, width * 5);
                    if (smoke.size() == PUFFS_PER_EMITTER) {
                        smoke.removeFirst();
                    }
                    smoke.addLast(new Puff(origin, velocity, birth, width * (.85f + .3f * noise(random + 3)), strength));
                }
            }
            previous.set(position);
            previousTime = time;
            seen = true;
        }
    }

    void beginFrame() {
        jets.values().forEach(jet -> jet.seen = false);
    }

    /** Call after bone animation, placement and hover. No sensor-contact or absent body is a source of effects. */
    void update(String key, GpuUnitModel model, ModelInstance instance, BoardScene.Unit unit, UnitMotion.Sample motion) {
        if (unit.sensorContact() || unit.model().state() == null || motion.jets() == null && motion.group() == null) {
            return;
        }
        for (var rig : model.rigs()) {
            var individual = motion.member(unit.id(), rig.container());
            Node container = rig.container() == null ? null : instance.getNode(rig.container());
            var nodes = container == null ? instance.nodes : container.getChildren();
            for (var emitter : rig.emitters()) {
                if ("exhaust".equals(emitter.role())) {
                    emit(key + "/body/" + rig.container() + "/" + emitter.id(), instance,
                          UnitAnimator.find(nodes, emitter.node()), emitter, individual, rig.trooper() ? .8f : 1.8f);
                }
            }
        }
        for (var binding : model.equipment()) {
            var appearance = unit.model().state().appearance();
            var effective = binding.memberId() < 0 ? appearance : appearance.fighters().get(binding.memberId());
            if (effective == null || effective.inoperableEquipment().contains(binding.index())
                  || unit.model().damage().removed().contains(binding.location())
                  || unit.model().damage().wrecked().contains(binding.location())) {
                continue;
            }
            for (var emitter : binding.emitters()) {
                if ("exhaust".equals(emitter.role())) {
                    emit(key + "/" + binding.node() + "/" + emitter.id(), instance,
                          instance.getNode(emitter.node()), emitter, motion, 1.8f);
                }
            }
        }
    }

    private void emit(String key, ModelInstance instance, Node node, UnitModelDescriptor.Emitter emitter,
          UnitMotion.Sample motion, float width) {
        if (node == null || motion.jets() == null || !visible(node)) {
            return;
        }
        Jet jet = jets.get(key);
        if (jet == null) {
            if (jets.size() == MAX_EMITTERS) {
                return;
            }
            jet = new Jet(key.hashCode());
            jets.put(key, jet);
        }
        jet.update(instance, node, emitter, motion, width);
    }

    private static boolean visible(Node node) {
        if (!node.parts.isEmpty()) {
            for (var part : node.parts) {
                if (part.enabled && !part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)) {
                    return true;
                }
            }
            return false;
        }
        for (Node child : node.getChildren()) {
            if (visible(child)) {
                return true;
            }
        }
        return false;
    }

    void endFrame() {
        // Landing, skip, casualties, board changes and loss of visibility leave no detached/ghost flames or smoke.
        jets.values().removeIf(jet -> !jet.seen);
        sortedSmoke.clear();
    }

    int emitterCount() {
        return jets.size();
    }

    int smokeCount() {
        return jets.values().stream().mapToInt(jet -> jet.smoke.size()).sum();
    }

    void clear() {
        jets.clear();
        sortedSmoke.clear();
    }

    void render(Camera camera) {
        if (jets.isEmpty()) {
            return;
        }
        right.set(camera.direction).crs(camera.up).nor();
        sortedSmoke.clear();
        for (Jet jet : jets.values()) {
            for (Puff puff : jet.smoke) {
                float age = jet.time - puff.birth, remaining = 1 - age / SMOKE_LIFE;
                puff.center.set(puff.origin).mulAdd(puff.velocity, age);
                puff.radius = puff.width * (2.3f + age * 8);
                puff.alpha = .5f * puff.strength * jet.power.smoke() * remaining * remaining;
                puff.depth = puff.center.dot(camera.direction);
                sortedSmoke.add(puff);
            }
        }
        sortedSmoke.sort(Comparator.comparingDouble((Puff puff) -> puff.depth).reversed());
        batch.begin();
        for (Puff puff : sortedSmoke) {
            across.set(right).scl(puff.radius);
            along.set(camera.up).scl(puff.radius);
            batch.quad(puff.center, across, along, -1, 0, puff.alpha);
        }
        int smokeCount = batch.size();
        for (Jet jet : jets.values()) {
            across.set(jet.direction).crs(camera.direction);
            if (across.isZero(.001f)) {
                across.set(right);
            }
            across.nor().scl(jet.width * 1.5f);
            float flicker = .94f + .06f * MathUtils.sin(jet.time * 89 + (jet.seed & 1023) * .017f);
            along.set(jet.direction).scl(jet.width * 10 * jet.power.flame() * flicker);
            batch.quad(jet.position, across, along, 0, 1, jet.power.flame());
        }
        batch.render(camera, smokeCount);
    }

    private static float noise(int value) {
        value ^= value >>> 16;
        value *= 0x45d9f3b;
        value ^= value >>> 16;
        return (value & 0xFFFFFF) / 16777216f;
    }

    @Override
    public void dispose() {
        clear();
        batch.dispose();
    }
}
