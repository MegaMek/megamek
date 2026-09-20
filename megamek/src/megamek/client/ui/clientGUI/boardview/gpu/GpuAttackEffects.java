/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

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
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;

/** Bounded procedural effects using the posed assembly's actual muzzle/launcher emitters. No rules or hit tests. */
final class GpuAttackEffects implements Disposable {
    private static final int MAX_EFFECTS = 48;
    private static final int MAX_MUZZLES = 8;
    private final Vector3[] origins = new Vector3[MAX_MUZZLES];
    private final String[] effects = new String[MAX_MUZZLES];
    private final Vector3 target = new Vector3();
    private final Vector3 direction = new Vector3();
    private final Vector3 point = new Vector3();
    private final Vector3 end = new Vector3();
    private final BoundingBox bounds = new BoundingBox();
    private final ModelInstance[] particles = new ModelInstance[MAX_EFFECTS];
    private Model model;
    private ModelBatch batch;
    private UnitAttack attack;
    private int muzzleCount, count;

    void update(UnitAttack next, GpuUnitModels library, Map<String, ModelInstance> instances) {
        attack = next;
        muzzleCount = 0;
        if (attack == null) {
            return;
        }
        var event = attack.event;
        var unit = event.attacker();
        var instance = instances.get(unit.id() + ":" + unit.part());
        var assembly = library == null || unit.model() == null ? null : library.get(unit.model(), unit.id());
        if (assembly != null && instance != null) {
            for (var binding : assembly.equipment()) {
                if (!attack.fires(binding)) {
                    continue;
                }
                for (var emitter : binding.emitters()) {
                    if (muzzleCount == MAX_MUZZLES || !("muzzle".equals(emitter.role()) || "beam".equals(emitter.role())
                          || "launcher".equals(emitter.role()) || "contact".equals(emitter.role()))) {
                        continue;
                    }
                    var node = instance.getNode(emitter.node());
                    if (node != null) {
                        UnitModelAttachment.emitter(instance, node, emitter, origin(muzzleCount), direction);
                        effects[muzzleCount++] = emitter.effect();
                    }
                }
            }
        }
        if (muzzleCount == 0) {
            center(instance, unit.location(), origin(0));
            effects[muzzleCount++] = "bullet";
        }
        var receiving = event.target();
        var targetInstance = receiving == null ? null : instances.get(receiving.id() + ":" + receiving.part());
        center(targetInstance, event.destination(), target);
        if (!event.result().hit()) {
            // A cosmetic miss endpoint, never a replacement for the server's damage/scatter rules.
            long seed = event.result().id().getLeastSignificantBits();
            direction.set(target).sub(origins[0]);
            point.set(direction.y, -direction.x, 0).nor().scl(BoardGeometry.HEIGHT * ((seed & 1) == 0 ? .35f : -.35f));
            target.add(point);
        }
    }

    private Vector3 origin(int index) {
        if (origins[index] == null) {
            origins[index] = new Vector3();
        }
        return origins[index];
    }

    private void center(ModelInstance instance, BoardScene.Waypoint location, Vector3 result) {
        if (instance == null) {
            result.set(BoardGeometry.center(location.coords(), location.elevation())).add(0, 0, BoardGeometry.LEVEL * .5f);
        } else {
            instance.calculateBoundingBox(bounds).getCenter(result).mul(instance.transform);
        }
    }

    void render(Camera camera) {
        if (attack == null || attack.seconds < UnitAttack.ANTICIPATION_SECONDS || attack.seconds >= attack.duration) {
            return;
        }
        ensureResources();
        count = 0;
        batch.begin(camera);
        if (attack.shot() && attack.seconds < attack.contactSeconds) {
            for (int index = 0; index < muzzleCount; index++) {
                String effect = effects[index];
                Vector3 start = origins[index];
                float t = attack.flight();
                switch (effect) {
                    case "laser" -> line(start, target, 1, Color.RED, Math.min(1, t * 8));
                    case "ppc", "energy" -> {
                        point.set(start).lerp(target, t);
                        ball(point, 2.5f, Color.CYAN, 1);
                        end.set(start).lerp(target, Math.max(0, t - .07f));
                        line(end, point, .8f, Color.CYAN, .65f);
                    }
                    case "missile", "cluster" -> {
                        int amount = "cluster".equals(effect) ? 6 : 3;
                        for (int missile = 0; missile < amount; missile++) {
                            float progress = MathUtils.clamp(t * 1.2f - missile * .035f, 0, 1);
                            point.set(start).lerp(target, progress).add((missile % 3 - 1) * 1.8f, 0,
                                  MathUtils.sin(progress * MathUtils.PI) * (4 + missile));
                            end.set(point).mulAdd(direction.set(target).sub(start).nor(), -4);
                            line(end, point, .85f, Color.LIGHT_GRAY, 1);
                            ball(end, 1.2f, Color.ORANGE, .9f);
                        }
                    }
                    case "flame", "spray", "screen" -> {
                        Color color = "screen".equals(effect) ? Color.PURPLE : "spray".equals(effect) ? Color.SKY : Color.ORANGE;
                        for (int puff = 0; puff < 6; puff++) {
                            float progress = MathUtils.clamp(t * 1.3f - puff * .06f, 0, 1);
                            point.set(start).lerp(target, progress).add(0, 0, puff * .5f);
                            ball(point, 1.5f + progress * 3, color, .65f);
                        }
                    }
                    default -> {
                        point.set(start).lerp(target, t);
                        end.set(start).lerp(target, Math.max(0, t - .07f));
                        line(end, point, .55f, Color.YELLOW, 1);
                    }
                }
                float flash = Math.max(0, 1 - (attack.seconds - UnitAttack.ANTICIPATION_SECONDS) / .12f);
                if (flash > 0) {
                    ball(start, 2.5f * flash, Color.YELLOW, flash);
                }
            }
        }
        float impact = attack.impact();
        if (impact > 0) {
            ball(target, 2 + (1 - impact) * 4, attack.shot() ? Color.ORANGE : Color.LIGHT_GRAY, impact);
        }
        batch.end();
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
        return particle;
    }

    private void ball(Vector3 position, float size, Color color, float alpha) {
        if (count == MAX_EFFECTS) {
            return;
        }
        var particle = particle(color, alpha);
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
    }
}
