/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;

/** Per-unit rigid pose state, evaluated from authored rest transforms and the shared movement/playback clock. */
final class UnitAnimator {
    static final float HOVER_PERIOD_SECONDS = 2.6f;
    static final float HOVER_LEVELS = .2f;
    private static final float HOVER_PHASE_STEP = .381966f;
    private static final String[][] LEGS = { { "leftLeg", "leftShin", "leftFoot" }, { "rightLeg", "rightShin", "rightFoot" },
          { "CL", "CLShin", "CLFoot" }, { "FLL", "FLLShin", "FLLFoot" }, { "FRL", "FRLShin", "FRLFoot" },
          { "RLL", "RLLShin", "RLLFoot" }, { "RRL", "RRLShin", "RRLFoot" },
          { "leg0", "leg0Shin", "" }, { "leg1", "leg1Shin", "" },
          { "leg2", "leg2Shin", "" }, { "leg3", "leg3Shin", "" } };
    private final List<Body> bodies = new ArrayList<>();
    private final List<Joint> mounts = new ArrayList<>();
    private final InfantryMotion formation = new InfantryMotion();
    private final Vector3 recoilDirection = new Vector3();
    private ModelInstance instance;
    private UnitLandingSupports landingSupports;
    private boolean initialized;
    private ProneCause posture = ProneCause.NONE;
    private float crouch, fallen, airborne;

    private record Joint(Node node, Vector3 translation, Quaternion rotation, Vector3 scale) {
        Joint(Node node, Node rest) {
            this(node, new Vector3(rest.translation), new Quaternion(rest.rotation), new Vector3(rest.scale));
        }

        void reset() {
            node.translation.set(translation);
            node.rotation.set(rotation);
            node.scale.set(scale);
        }

        void rotate(Vector3 axis, float degrees) {
            float half = degrees * MathUtils.degreesToRadians * .5f;
            float sine = (float) Math.sin(half);
            node.rotation.mul(axis.x * sine, axis.y * sine, axis.z * sine, (float) Math.cos(half));
        }
    }

    private static final class Body {
        final UnitRig rig;
        final Map<String, Joint> joints = new HashMap<>();
        final Map<String, Float> travelPitch = new HashMap<>();
        final Map<Joint, Float> wheels = new HashMap<>();
        final float restFloor;
        final float cycleDistance;
        final float groundReach;
        final float memberScale;
        float wheelSteps, lastSteps;
        long movementSequence = -1;

        Body(UnitRig rig, ModelInstance instance, ModelInstance rest) {
            this.rig = rig;
            Node container = rig.container() == null ? null : instance.getNode(rig.container());
            Node original = rig.container() == null ? null : rest.getNode(rig.container());
            for (var role : rig.joints().entrySet()) {
                Node node = find(container == null ? instance.nodes : container.getChildren(), role.getValue());
                Node source = find(original == null ? rest.nodes : original.getChildren(), role.getValue());
                if (node != null && source != null) {
                    joints.put(role.getKey(), new Joint(node, source));
                    if (role.getKey().startsWith("wheel-")) {
                        wheels.put(joints.get(role.getKey()), UnitBounds.subtree(source).getDepth() * .5f);
                    }
                }
            }
            Node sourceRoot = find(original == null ? rest.nodes : original.getChildren(), rig.joints().get("root"));
            restFloor = UnitBounds.subtree(sourceRoot).min.z;
            memberScale = sourceRoot.globalTransform.getScaleY();
            float shortestLeg = Float.POSITIVE_INFINITY;
            for (String[] leg : LEGS) {
                Joint shin = joints.get(leg[1]), foot = joints.get(leg[2]);
                if (shin != null && foot != null) {
                    shortestLeg = Math.min(shortestLeg, pitchLength(shin.translation) + pitchLength(foot.translation));
                    float hipAngle = pitch(shin.translation()), shinAngle = pitch(foot.translation());
                    travelPitch.put(leg[0], -hipAngle);
                    travelPitch.put(leg[1], hipAngle - shinAngle);
                    travelPitch.put(leg[2], shinAngle);
                }
            }
            cycleDistance = Float.isFinite(shortestLeg) ? shortestLeg * 1.2f : 0;
            groundReach = Float.isFinite(shortestLeg) ? shortestLeg * .94f : 0;
        }

        private static float pitch(Vector3 bone) {
            return MathUtils.atan2(bone.y, -bone.z) * MathUtils.radiansToDegrees;
        }

        private static float pitchLength(Vector3 bone) {
            // The fixed lateral offset does not participate in the leg's forward/backward rotation.
            return (float) Math.hypot(bone.y, bone.z);
        }

        void rotate(String role, Vector3 axis, float degrees) {
            Joint joint = joints.get(role);
            if (joint != null) {
                joint.rotate(axis, degrees);
            }
        }

        void reset() {
            joints.values().forEach(Joint::reset);
        }
    }

    static Node find(Iterable<Node> nodes, String id) {
        for (Node node : nodes) {
            if (id.equals(node.id)) {
                return node;
            }
            Node found = find(node.getChildren(), id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** A material replacement rebinds nodes but keeps playback; a new/revealed unit starts directly in its pose. */
    void apply(GpuMeeple model, ModelInstance placed, BoardScene.Unit unit, UnitMotion.Sample motion,
          float clock, float seconds, boolean instant, float twist) {
        if (instance != placed) {
            instance = placed;
            List<Body> previous = new ArrayList<>(bodies);
            bodies.clear();
            model.rigs().forEach(rig -> bodies.add(new Body(rig, placed, model.instance)));
            mounts.clear();
            for (var binding : model.equipment()) {
                Node node = placed.getNode(binding.node()), rest = model.instance.getNode(binding.node());
                if (node != null && rest != null) {
                    mounts.add(new Joint(node, rest));
                }
            }
            for (Body body : bodies) {
                previous.stream().filter(old -> old.rig.equals(body.rig)).findFirst().ifPresent(old -> {
                    body.wheelSteps = old.wheelSteps;
                    body.lastSteps = old.lastSteps;
                    body.movementSequence = old.movementSequence;
                });
            }
            formation.bind(model, placed);
            landingSupports = new UnitLandingSupports(placed, model.instance, model.rigs());
        }
        ProneCause observed = motion.moving() ? motion.proneCause() : unit.location().proneCause();
        if (!motion.moving() && observed == null) {
            observed = unit.model().state().pose().proneCause();
        }
        if (observed != null) {
            posture = observed;
        }
        boolean snap = instant || !initialized;
        initialized = true;
        var sampledPosture = motion.posture();
        if (sampledPosture == null && !motion.moving()
              && (unit.location().proneCause() != null || motion.proneCause() == posture)) {
            sampledPosture = UnitMotion.Posture.of(posture);
        }
        crouch = sampledPosture == null ? approach(crouch, posture == ProneCause.VOLUNTARY ? 1 : 0, seconds, snap) : sampledPosture.crouch();
        fallen = sampledPosture == null ? approach(fallen, posture == ProneCause.FORCED || posture == ProneCause.UNKNOWN ? 1 : 0, seconds, snap) : sampledPosture.fallen();
        airborne = approach(airborne, motion.airborne(unit) ? 1 : 0, seconds, snap);
        boolean mek = unit.model().state().structure().anatomy() != null;
        bodies.forEach(Body::reset);
        mounts.forEach(Joint::reset);
        formation.apply(model, unit, motion);
        for (Body body : bodies) {
            var bodyMotion = motion.member(unit.id(), body.rig.container());
            boolean flying = bodyMotion.airborne(unit);
            boolean jumping = bodyMotion.type() == EntityMovementType.MOVE_JUMP;
            float moving = bodyMotion.moving() ? 1 : Math.max(0, 1 - bodyMotion.settledSeconds() / .25f);
            float envelope = bodyMotion.moving() ? Math.min(1, Math.min(bodyMotion.progress(), 1 - bodyMotion.progress()) * 12) : 0;
            float phase = (bodyMotion.steps() * 2 + bodyMotion.progress() * Math.abs(bodyMotion.turn()) / 60)
                  * MathUtils.PI2 * Math.signum(bodyMotion.forward());
            int identity = 31 * unit.id() + (body.rig.container() == null ? 0 : body.rig.container().hashCode());
            // Keep the phase small: adding a large hash to a float loses sub-step movement precision.
            float seed = Math.floorMod(identity, 4096) * (MathUtils.PI2 / 4096);
            var memberStep = formation.step(body.rig.container());
            float distance = (memberStep == null ? bodyMotion.steps() * BoardGeometry.HEIGHT / model.horizontalScale(unit)
                  : memberStep.distance()) / body.memberScale;
            float localPhase = body.cycleDistance == 0 ? phase : distance / body.cycleDistance * MathUtils.PI2
                  * (body.rig.trooper() ? 1 : Math.signum(bodyMotion.forward()))
                  + bodyMotion.progress() * Math.abs(bodyMotion.turn()) / 60 * MathUtils.PI2;
            if (body.rig.trooper()) {
                localPhase += seed;
            }
            if (mek || body.rig.trooper() || "proto-v1".equals(body.rig.type())) {
                float stance = mek ? Math.max(crouch, fallen) : 0;
                boolean quad = "quad-v1".equals(body.rig.type());
                float gait = jumping || flying ? 0 : (memberStep == null ? envelope : memberStep.gait()) * (1 - stance);
                // Authored kneeling/aiming members rise into a walking stance, then return to that same rest pose.
                float standing = memberStep == null ? envelope : memberStep.standing();
                body.travelPitch.forEach((role, angle) -> body.rotate(role, Vector3.X, angle * standing));
                legs(body, localPhase, gait, mek ? crouch + (quad ? fallen * .3f : 0) : 0, jumping ? envelope : 0);
                body.rotate("leftArm", Vector3.X, -MathUtils.sin(localPhase) * 9 * gait);
                body.rotate("rightArm", Vector3.X, MathUtils.sin(localPhase) * 9 * gait);
                if (mek) {
                    body.rotate("torso", Vector3.X, -crouch * (quad ? 8 : 24));
                    body.rotate("leftArm", Vector3.X, crouch * 35);
                    body.rotate("rightArm", Vector3.X, crouch * 35);
                    body.rotate("leftForearm", Vector3.X, crouch * 55);
                    body.rotate("rightForearm", Vector3.X, crouch * 55);
                    body.rotate("torso", Vector3.Z, -twist);
                    body.rotate("root", Vector3.X, fallen * 90);
                } else {
                    body.rotate("torso", Vector3.X, MathUtils.sin(clock * 2 + seed) * .7f * (1 - moving));
                }
            }
            if ("aircraft-v1".equals(body.rig.type())) {
                body.rotate("rotor", Vector3.Z, clock * 900 * airborne);
                body.rotate("hull", Vector3.Y, -MathUtils.clamp(motion.turn(), -12, 12) * envelope * (flying ? airborne : 0));
                if (body.rig.landingSupports().isEmpty()) {
                    switch (unit.model().state().structure().movement()) {
                        case AERODYNE, SPHEROID, AEROSPACE -> body.joints.keySet().stream().filter(role -> role.startsWith("gear"))
                              .forEach(role -> body.rotate(role, Vector3.X, airborne * 85));
                        default -> { } // VTOL skids and airship supports are fixed, not retractable landing gear.
                    }
                }
            }
            if ("vehicle-v1".equals(body.rig.type()) || "transport-v1".equals(body.rig.type())) {
                body.rotate("hull", Vector3.X, MathUtils.sin(localPhase * 2) * .65f * envelope);
                if (motion.moving()) {
                    float traveled = body.rig.transport() ? formation.drivenDistance(body.rig.container())
                          : motion.steps() * BoardGeometry.HEIGHT / model.horizontalScale(unit);
                    if (body.movementSequence != motion.sequence()) {
                        body.lastSteps = 0;
                        body.movementSequence = motion.sequence();
                    }
                    body.wheelSteps += (traveled - body.lastSteps) * (body.rig.transport()
                          ? formation.forward(body.rig.container()) : Math.signum(motion.forward()));
                    body.lastSteps = traveled;
                }
                body.wheels.forEach((wheel, radius) -> wheel.rotate(Vector3.X,
                      -body.wheelSteps / Math.max(.1f, radius) * MathUtils.radiansToDegrees));
            }
            if (!mek) {
                var pose = unit.model().state().pose();
                body.rotate("turret", Vector3.Z, -(pose.secondaryFacing() - pose.facing()) * 60);
            }
            if ("naval-v1".equals(body.rig.type())) {
                body.rotate("hull", Vector3.Y, MathUtils.sin(clock * .8f + seed) * .8f);
            }
        }
        placed.calculateTransforms();
        // Preserve each body's authored contact level (including naval waterlines); only articulated land bodies settle.
        for (Body body : bodies) {
            if (body.rig.trooper() || "proto-v1".equals(body.rig.type())
                  || body.joints.containsKey("leftLeg") || body.joints.containsKey("FLL")) {
                Node root = body.joints.get("root").node();
                var bounds = UnitBounds.subtree(root);
                if (bounds.isValid()) {
                    float scale = root.getParent() == null ? 1 : root.getParent().globalTransform.getScaleZ();
                    root.translation.z += (body.restFloor + formation.verticalOffset(body.rig.container()) - bounds.min.z) / scale;
                }
            }
        }
        placed.calculateTransforms();
    }

    /** Called after footprint fitting and world placement, before final bounds, picking, shadows and drawing. */
    boolean groundSupports(BoardScene scene, BoardScene.Unit unit, UnitMotion.Sample motion) {
        return landingSupports != null && landingSupports.apply(scene, unit, motion);
    }

    /** Add a resolved clip to this frame's rest-based pose, before placement, emitters, bounds and shadows. */
    void attack(GpuMeeple model, BoardScene.Unit unit, UnitAttack attack) {
        if (attack == null || instance == null) {
            return;
        }
        var event = attack.event;
        if (unit.id() == event.entityId()) {
            int location = event.result().limb();
            boolean left = location == megamek.common.units.Mek.LOC_LEFT_ARM || location == megamek.common.units.Mek.LOC_LEFT_LEG;
            String arm = left ? "leftArm" : "rightArm";
            for (Body body : bodies) {
                if (attack.shot()) {
                    if (location == megamek.common.units.Mek.LOC_LEFT_ARM || location == megamek.common.units.Mek.LOC_RIGHT_ARM) {
                        body.rotate(arm, Vector3.X, -5 * attack.recoil());
                    } else {
                        body.rotate("torso", Vector3.X, -2 * attack.recoil());
                    }
                    body.rotate("hull", Vector3.X, -2 * attack.recoil());
                } else if (unit.model().state().structure().anatomy() != null) {
                    float strike = attack.strike();
                    switch (event.result().kind()) {
                        case PUNCH -> {
                            body.rotate(arm, Vector3.X, 80 * strike);
                            body.rotate(left ? "leftForearm" : "rightForearm", Vector3.X, -25 * strike);
                        }
                        case KICK -> {
                            String leg = "quad-v1".equals(body.rig.type())
                                  ? (location == megamek.common.units.Mek.LOC_LEFT_ARM ? "FLL"
                                        : location == megamek.common.units.Mek.LOC_RIGHT_ARM ? "FRL" : left ? "RLL" : "RRL")
                                  : left ? "leftLeg" : "rightLeg";
                            body.rotate(leg, Vector3.X, 65 * strike);
                            body.rotate("leftLeg".equals(leg) ? "leftShin" : "rightLeg".equals(leg) ? "rightShin" : leg + "Shin",
                                  Vector3.X, -15 * strike);
                        }
                        case PUSH -> {
                            body.rotate("leftArm", Vector3.X, 70 * strike);
                            body.rotate("rightArm", Vector3.X, 70 * strike);
                            body.rotate("torso", Vector3.X, -8 * strike);
                        }
                        case CLUB -> {
                            body.rotate(arm, Vector3.X, 120 * strike);
                            body.rotate(arm, Vector3.Y, (left ? -1 : 1) * 25 * strike);
                        }
                        default -> { }
                    }
                }
            }
            if (attack.shot()) {
                for (var binding : model.equipment()) {
                    if (attack.fires(binding)) {
                        Node node = instance.getNode(binding.node());
                        Node rest = model.instance.getNode(binding.node());
                        if (node != null && rest != null) {
                            // Independent mount recoil. Body.apply resets joints, so always start at the mount's rest transform.
                            var emitter = binding.emitters().isEmpty() ? null : binding.emitters().getFirst();
                            UnitModelAttachment.barrelDirection(model.instance, rest, emitter, recoilDirection);
                            node.translation.set(rest.translation).mulAdd(recoilDirection, -.9f * attack.recoil());
                        }
                    }
                }
            }
        }
        if (event.target() != null && unit.id() == event.target().id()) {
            for (Body body : bodies) {
                body.rotate("torso", Vector3.X, -2 * attack.impact());
                body.rotate("hull", Vector3.X, -1 * attack.impact());
            }
        }
        instance.calculateTransforms();
    }

    private static void legs(Body body, float phase, float gait, float crouch, float jump) {
        for (int index = 0; index < LEGS.length; index++) {
            String[] pair = LEGS[index];
            // Tripods use three phases; quads alternate diagonal pairs. Other bipeds alternate their two legs.
            float offset = "tripod-v1".equals(body.rig.type()) && index < 3 ? index * MathUtils.PI2 / 3
                  : switch (index) { case 1, 4, 5, 8, 9 -> MathUtils.PI; default -> 0; };
            if (gait > 0 && crouch == 0 && jump == 0 && body.cycleDistance > 0
                  && plantedStep(body, pair, phase + offset, gait)) {
                continue;
            }
            float swing = MathUtils.sin(phase + offset);
            float hip = 22 * swing * gait + 62 * crouch + 22 * jump;
            float knee = -34 * Math.max(0, swing) * gait - 112 * crouch - 48 * jump;
            body.rotate(pair[0], Vector3.X, hip);
            body.rotate(pair[1], Vector3.X, knee);
            body.rotate(pair[2], Vector3.X, -hip - knee);
        }
    }

    /** Two rigid leg segments reach a ground-relative foot path; cadence comes from distance, not a timer. */
    private static boolean plantedStep(Body body, String[] roles, float phase, float weight) {
        Joint shin = body.joints.get(roles[1]), foot = body.joints.get(roles[2]);
        if (shin == null || foot == null) {
            return false;
        }
        float upper = Body.pitchLength(shin.translation), lower = Body.pitchLength(foot.translation);
        if (upper < .001f || lower < .001f) {
            return false;
        }
        float cycle = phase / MathUtils.PI2 + .25f;
        cycle -= (float) Math.floor(cycle);
        float width = body.cycleDistance * .5f;
        float y, lift = 0;
        if (cycle < .5f) {
            // During support, the foot travels backwards exactly as far as the body advances.
            y = width * (.5f - cycle * 2);
        } else {
            float t = (cycle - .5f) * 2;
            // Match the support velocity at liftoff/touchdown, lifting only during the returning step.
            y = width * (-.5f - t + 6 * t * t - 4 * t * t * t);
            float sine = MathUtils.sin(t * MathUtils.PI);
            lift = (upper + lower) * .12f * sine * sine;
        }
        // Asymmetric authored poses must still put both supporting feet on the same ground plane.
        float z = body.groundReach - lift;
        float reach = (float) Math.hypot(y, z);
        float kneeCos = MathUtils.clamp((upper * upper + lower * lower - reach * reach) / (2 * upper * lower), -1, 1);
        float hipCos = MathUtils.clamp((upper * upper + reach * reach - lower * lower) / (2 * upper * reach), -1, 1);
        float hip = (MathUtils.atan2(y, z) + (float) Math.acos(hipCos)) * MathUtils.radiansToDegrees;
        float knee = ((float) Math.acos(kneeCos) - MathUtils.PI) * MathUtils.radiansToDegrees;
        body.rotate(roles[0], Vector3.X, hip * weight);
        body.rotate(roles[1], Vector3.X, knee * weight);
        body.rotate(roles[2], Vector3.X, -(hip + knee) * weight);
        return true;
    }

    private static float approach(float from, float to, float seconds, boolean instant) {
        if (instant) {
            return to;
        }
        return from + Math.signum(to - from) * Math.min(Math.abs(to - from), (float) (Math.max(0, seconds) / UnitMotion.POSTURE_SECONDS));
    }

    static float hoverOffset(float seconds, int id, int part) {
        return HOVER_LEVELS * BoardGeometry.LEVEL * MathUtils.sin(MathUtils.PI2 * seconds / HOVER_PERIOD_SECONDS
              + MathUtils.PI2 * HOVER_PHASE_STEP * (id + part));
    }
}
