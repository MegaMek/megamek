/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;

/** Per-unit rigid pose state, evaluated from authored rest transforms and the shared movement/playback clock. */
final class UnitAnimator {
    static final float HOVER_PERIOD_SECONDS = 2.6f;
    static final float HOVER_LEVELS = .2f;
    private static final float HOVER_PHASE_STEP = .381966f;
    static final float PHYSICAL_APPROACH_HEXES = .9f;
    /** A longer distance per cycle also lengthens airtime, without changing planted-foot speed. */
    static final float MEK_STRIDE_LENGTH = 1.6f;
    static final float MEK_STEP_LIFT = .16f;
    private static final float ARM_AIM_LIMIT_DEGREES = 120;
    private static final String[][] LEGS = { { "leftLeg", "leftShin", "leftFoot" }, { "rightLeg", "rightShin", "rightFoot" },
          { "CL", "CLShin", "CLFoot" }, { "FLL", "FLLShin", "FLLFoot" }, { "FRL", "FRLShin", "FRLFoot" },
          { "RLL", "RLLShin", "RLLFoot" }, { "RRL", "RRLShin", "RRLFoot" },
          { "leg0", "leg0Shin", "leg0Foot" }, { "leg1", "leg1Shin", "leg1Foot" },
          { "leg2", "leg2Shin", "leg2Foot" }, { "leg3", "leg3Shin", "leg3Foot" } };
    private final List<Body> bodies = new ArrayList<>();
    private final List<Joint> mounts = new ArrayList<>();
    private final InfantryMotion formation = new InfantryMotion();
    private final Vector3 recoilDirection = new Vector3();
    private final Map<String, Float> mountRecoil = new HashMap<>();
    private ModelInstance instance;
    private UnitLandingSupports landingSupports;
    private UnitGroundContact groundContact;
    private final BoardSurface.Cache surfaces;
    private boolean dying;
    private boolean initialized;
    private ProneCause posture = ProneCause.NONE;
    private float crouch, fallen, airborne;
    private megamek.common.units.FallSide fallSide;

    UnitAnimator() { this(new BoardSurface.Cache()); }

    UnitAnimator(BoardSurface.Cache surfaces) { this.surfaces = surfaces; }

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

        void turn(float degrees) {
            float half = degrees * MathUtils.degreesToRadians * .5f;
            // Turn in the parent's ground plane, before the authored leg's rest-pose pitch.
            node.rotation.mulLeft(0, 0, (float) Math.sin(half), (float) Math.cos(half));
        }
    }

    private static final class Body {
        final UnitRig rig;
        final Map<String, Joint> joints = new HashMap<>();
        final Map<String, Float> travelPitch = new HashMap<>();
        final Map<Joint, Float> wheels = new HashMap<>();
        final float restFloor;
        final float restHeight;
        final Vector3 restCenter;
        final float cycleDistance;
        final float groundReach;
        final float stepLift;
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
            var restBounds = UnitBounds.subtree(sourceRoot);
            restFloor = restBounds.min.z;
            restHeight = restBounds.getDepth();
            restCenter = restBounds.getCenter(new Vector3());
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
            boolean longStride = rig.mek() || "proto-v1".equals(rig.type());
            cycleDistance = Float.isFinite(shortestLeg) ? shortestLeg * (longStride ? MEK_STRIDE_LENGTH : 1.2f) : 0;
            // Slightly flex the support leg so the longer step remains inside its physical reach.
            groundReach = Float.isFinite(shortestLeg) ? shortestLeg * (longStride ? .9f : .94f) : 0;
            stepLift = longStride ? MEK_STEP_LIFT : .12f;
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
    void apply(GpuUnitModel model, ModelInstance placed, BoardScene.Unit unit, UnitMotion.Sample motion,
          float clock, float seconds, boolean instant, float twist) {
        mountRecoil.clear();
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
            landingSupports = new UnitLandingSupports(placed, model.instance, model.rigs(), surfaces);
            groundContact = new UnitGroundContact(placed, model.instance, model.rigs(), surfaces);
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
            sampledPosture = UnitMotion.Posture.of(posture, unit.location().fallSide());
        }
        if (sampledPosture != null && sampledPosture.side() != null) { fallSide = sampledPosture.side(); }
        else if (unit.location().fallSide() != null) { fallSide = unit.location().fallSide(); }
        float previousFall = fallen;
        crouch = sampledPosture == null ? approach(crouch, posture == ProneCause.VOLUNTARY ? 1 : 0, seconds, snap) : sampledPosture.crouch();
        fallen = sampledPosture == null ? approach(fallen, posture == ProneCause.FORCED || posture == ProneCause.UNKNOWN ? 1 : 0, seconds, snap) : sampledPosture.fallen();
        if (fallen == 0 && posture == ProneCause.NONE) { fallSide = null; }
        airborne = approach(airborne, motion.airborne(unit) ? 1 : 0, seconds, snap);
        bodies.forEach(Body::reset);
        mounts.forEach(Joint::reset);
        formation.apply(model, unit, motion);
        for (Body body : bodies) {
            boolean mek = body.rig.mek() && unit.model().state().structure().anatomy() != null;
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
            float travelSign = body.rig.trooper() || bodyMotion.forward() >= -.001f ? 1 : -1;
            float strideYaw = body.rig.trooper() ? 0 : MathUtils.atan2(bodyMotion.lateral() * travelSign,
                  Math.abs(bodyMotion.forward())) * MathUtils.radiansToDegrees;
            float localPhase = body.cycleDistance == 0 ? phase : distance / body.cycleDistance * MathUtils.PI2
                  * travelSign
                  + bodyMotion.progress() * Math.abs(bodyMotion.turn()) / 60 * MathUtils.PI2;
            if (body.rig.trooper()) {
                localPhase += seed;
            }
            if (mek || body.rig.trooper() || "proto-v1".equals(body.rig.type())) {
                float stance = mek ? Math.max(crouch, fallen) : 0;
                boolean quad = "quad-v1".equals(body.rig.type());
                float gait = jumping || flying || UnitConversion.vehiclePose(unit) > 0 ? 0
                      : (memberStep == null ? envelope : memberStep.gait()) * (1 - stance);
                // Authored kneeling/aiming members rise into a walking stance, then return to that same rest pose.
                float legCrouch = mek ? crouch + (quad ? fallen * .3f : 0) : 0;
                // Absolute crouch angles must also account for the authored reverse-knee rest pose.
                float legPose = Math.max(legCrouch, UnitConversion.vehiclePose(unit) > 0 ? 0
                      : memberStep == null ? envelope : memberStep.standing());
                body.travelPitch.forEach((role, angle) -> body.rotate(role, Vector3.X, angle * legPose));
                legs(body, localPhase, gait, legCrouch, jumping ? envelope : 0, strideYaw);
                body.rotate("leftArm", Vector3.X, -MathUtils.sin(localPhase) * 9 * gait);
                body.rotate("rightArm", Vector3.X, MathUtils.sin(localPhase) * 9 * gait);
                if (mek) {
                    body.rotate("torso", Vector3.X, -crouch * (quad ? 8 : 24));
                    body.rotate("leftArm", Vector3.X, crouch * 35);
                    body.rotate("rightArm", Vector3.X, crouch * 35);
                    body.rotate("leftForearm", Vector3.X, crouch * 55);
                    body.rotate("rightForearm", Vector3.X, crouch * 55);
                    body.rotate("torso", Vector3.Z, -twist);
                    fallenPose(body, sampledPosture != null ? sampledPosture.rising() : fallen < previousFall,
                          sampledPosture == null ? 1 - fallen : sampledPosture.progress());
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
            if (bodyMotion.jets() != null) {
                tiltForJump(body, model, unit, bodyMotion);
            }
        }
        dying = unit.model().state().pose().dead();
        if (dying) { deathPose(1); }
        settleContacts();
    }

    private static void tiltForJump(Body body, GpuUnitModel model, BoardScene.Unit unit, UnitMotion.Sample motion) {
        Joint root = body.joints.get("root");
        if (root == null) { return; }
        float scale = model.horizontalScale(unit);
        // Compensate for display height scaling so the world-space lean stays within the trajectory's tilt limit.
        double angle = Math.atan(Math.tan(Math.toRadians(motion.jets().tilt())) * model.verticalScale(scale, unit) / scale);
        float forward = body.rig.trooper() ? 1 : motion.forward();
        float lateral = body.rig.trooper() ? 0 : motion.lateral();
        // Lookup-table sine/cosine are not an exactly unit-length axis, which nonuniform height scaling amplifies.
        float sine = (float) (Math.sin(angle / 2) / Math.hypot(forward, lateral)), cosine = (float) Math.cos(angle / 2);
        // Troop containers already face their own travel; other bodies keep their game's facing during the jump.
        root.node().rotation.mulLeft(-forward * sine, lateral * sine, 0, cosine);
    }

    private void settleContacts() {
        instance.calculateTransforms();
        // Preserve each body's authored contact level (including naval waterlines); only articulated land bodies settle.
        for (Body body : bodies) {
            if (body.rig.trooper() || "proto-v1".equals(body.rig.type())
                  || body.joints.containsKey("leftLeg") || body.joints.containsKey("FLL")) {
                Node root = body.joints.get("root").node();
                var bounds = UnitBounds.subtree(root);
                if (bounds.isValid()) {
                    float scale = root.getParent() == null ? 1 : root.getParent().globalTransform.getScaleZ();
                    root.translation.z += (body.restFloor + formation.verticalOffset(body.rig.container()) - bounds.min.z) / scale;
                    if (body.rig.mek() && fallen > 0 && !dying) {
                        // Rotate about the occupied hex, not about a hip pivot that sends the hull into another hex.
                        var center = bounds.getCenter(new Vector3());
                        root.translation.x += (body.restCenter.x - center.x) / scale;
                        root.translation.y += (body.restCenter.y - center.y) / scale;
                    }
                }
            }
        }
        instance.calculateTransforms();
    }

    private void fallenPose(Body body, boolean rising, float progress) {
        if (fallen <= 0) { return; }
        var side = fallSide == null ? megamek.common.units.FallSide.FRONT : fallSide;
        Vector3 axis = side == megamek.common.units.FallSide.LEFT || side == megamek.common.units.FallSide.RIGHT ? Vector3.Y : Vector3.X;
        float sign = side == megamek.common.units.FallSide.FRONT || side == megamek.common.units.FallSide.LEFT ? -1 : 1;
        if (rising) {
            // Roll onto the front, brace on the arms, draw the legs beneath the body, then stand.
            float roll = MathUtils.clamp(progress / .25f, 0, 1);
            float rise = MathUtils.clamp((progress - .2f) / .8f, 0, 1);
            rise = rise * rise * (3 - 2 * rise);
            var rotation = new Quaternion(axis, sign * 90).slerp(new Quaternion(Vector3.X, -90), roll);
            rotation.slerp(new Quaternion(), rise);
            body.joints.get("root").node().rotation.mul(rotation);
            float brace = MathUtils.clamp(progress / .25f, 0, 1);
            brace = brace * brace * (3 - 2 * brace);
            float release = MathUtils.clamp((progress - .5f) / .5f, 0, 1);
            float kneel = brace * (1 - release * release * (3 - 2 * release));
            if ("quad-v1".equals(body.rig.type())) { legs(body, 0, 0, kneel * .85f, 0, 0); }
            else {
                // One foot comes underneath the hips while the other knee and both hands carry the weight.
                body.rotate("leftLeg", Vector3.X, 85 * kneel);
                body.rotate("leftShin", Vector3.X, -125 * kneel);
                body.rotate("leftFoot", Vector3.X, 40 * kneel);
                body.rotate("rightLeg", Vector3.X, 55 * kneel);
                body.rotate("rightShin", Vector3.X, -120 * kneel);
                body.rotate("rightFoot", Vector3.X, 65 * kneel);
            }
            body.rotate("torso", Vector3.X, -12 * kneel);
            body.rotate("leftArm", Vector3.X, (12 + 68 * brace) * (1 - rise));
            body.rotate("rightArm", Vector3.X, (18 + 62 * brace) * (1 - rise));
            body.rotate("leftForearm", Vector3.X, 65 * kneel);
            body.rotate("rightForearm", Vector3.X, 65 * kneel);
        } else {
            body.rotate("root", axis, sign * 90 * fallen * fallen);
            float brace = MathUtils.sin(fallen * MathUtils.PI);
            legs(body, 0, 0, brace * .28f, 0, 0);
            body.rotate("leftArm", Vector3.X, 55 * brace + 12 * fallen);
            body.rotate("rightArm", Vector3.X, 65 * brace + 18 * fallen);
            body.rotate("leftForearm", Vector3.X, 30 * brace);
            body.rotate("rightForearm", Vector3.X, 35 * brace);
        }
    }

    /** Called after footprint fitting and world placement, before final bounds, picking, shadows and drawing. */
    boolean groundSupports(BoardScene scene, BoardScene.Unit unit, UnitMotion.Sample motion) {
        if (dying) { return false; }
        boolean contact = groundContact != null && groundContact.apply(scene, unit, motion);
        return (landingSupports != null && landingSupports.apply(scene, unit, motion)) || contact;
    }

    void conversion(UnitConversion conversion, BoardScene.Unit unit) {
        if (UnitConversion.quadVee(unit)) {
            float pose = UnitConversion.vehiclePose(unit);
            if (conversion != null && conversion.event.entityId() == unit.id()) {
                pose = MathUtils.lerp(UnitConversion.vehiclePose(conversion.event.before()),
                      UnitConversion.vehiclePose(conversion.event.after()), conversion.progress());
            }
            for (var body : bodies) {
                if (!body.rig.mek()) { continue; }
                for (String[] leg : LEGS) {
                    if (!body.joints.containsKey(leg[0])) { continue; }
                    // Opposed upper legs fold alongside the hull; feet stay level as drive pads.
                    float sign = leg[0].startsWith("F") ? 1 : -1;
                    body.rotate(leg[0], Vector3.X, sign * 85 * pose);
                    body.rotate(leg[1], Vector3.X, -sign * 170 * pose);
                    body.rotate(leg[2], Vector3.X, sign * 85 * pose);
                }
            }
            settleContacts();
            return;
        }
        if (conversion == null || conversion.event.entityId() != unit.id()) { return; }
        float fold = conversion.fold();
        for (var body : bodies) {
            if (body.rig.mek()) {
                legs(body, 0, 0, fold * .65f, 0, 0);
                body.rotate("leftArm", Vector3.X, 65 * fold);
                body.rotate("rightArm", Vector3.X, 65 * fold);
                body.rotate("torso", Vector3.X, -25 * fold);
                body.joints.forEach((role, joint) -> {
                    if (role.startsWith("wing")) { joint.rotate(Vector3.Y, -Math.signum(joint.translation.x) * 70 * fold); }
                });
            } else {
                body.joints.forEach((role, joint) -> {
                    if (role.startsWith("wing")) { joint.rotate(Vector3.Y, -Math.signum(joint.translation.x) * 65 * fold); }
                });
                body.rotate("turret", Vector3.X, 20 * fold);
                body.joints.keySet().stream().filter(role -> role.startsWith("gear"))
                      .forEach(role -> body.rotate(role, Vector3.X, 85 * fold));
            }
        }
        settleContacts();
    }

    /** Add a resolved clip to this frame's rest-based pose, before placement, emitters, bounds and shadows. */
    void attack(GpuUnitModel model, BoardScene.Unit unit, UnitAttack attack) {
        attack(model, unit, attack, true, true);
    }

    void attacks(GpuUnitModel model, BoardScene.Unit unit, List<UnitAttack> attacks) {
        if (attacks.isEmpty()) { return; }
        var strongest = attacks.stream().filter(attack -> attack.event.entityId() == unit.id())
              .max(java.util.Comparator.comparingDouble(UnitAttack::recoil)).orElse(null);
        var impact = attacks.stream().filter(attack -> attack.event.target() != null && attack.event.target().id() == unit.id())
              .max(java.util.Comparator.comparingDouble(UnitAttack::impact)).orElse(null);
        for (var attack : attacks) {
            if (attack == impact || attack.event.entityId() == unit.id()) {
                attack(model, unit, attack, attack == strongest || !attack.shot(), attack == impact);
            }
        }
    }

    private void attack(GpuUnitModel model, BoardScene.Unit unit, UnitAttack attack, boolean bodyPose, boolean impactPose) {
        if (attack == null || instance == null) {
            return;
        }
        var event = attack.event;
        if (attack.death()) {
            if (unit.id() == event.entityId()) {
                dying = true;
                deathPose(attack.deathProgress());
                settleContacts();
            }
            return;
        }
        if (dying) { return; }
        if (unit.id() == event.entityId()) {
            int location = event.result().limb();
            boolean left = location == megamek.common.units.Mek.LOC_LEFT_ARM || location == megamek.common.units.Mek.LOC_LEFT_LEG;
            String arm = left ? "leftArm" : "rightArm";
            for (Body body : bodies) {
                if (attack.shot() && bodyPose) {
                    if (location == megamek.common.units.Mek.LOC_LEFT_ARM || location == megamek.common.units.Mek.LOC_RIGHT_ARM) {
                        body.rotate(arm, Vector3.X, -5 * attack.recoil());
                    } else {
                        body.rotate("torso", Vector3.X, -2 * attack.recoil());
                    }
                    body.rotate("hull", Vector3.X, -2 * attack.recoil());
                }
            }
            if (attack.shot()) {
                for (var binding : model.equipment()) {
                    if (attack.fires(binding)) {
                        mountRecoil.merge(binding.node(), attack.recoil(binding), Math::max);
                    }
                }
            }
        }
        if (impactPose && event.target() != null && unit.id() == event.target().id()) {
            for (Body body : bodies) {
                body.rotate("torso", Vector3.X, -2 * attack.impact());
                body.rotate("hull", Vector3.X, -1 * attack.impact());
            }
        }
        instance.calculateTransforms();
        applyRecoil(model);
    }

    /** Add constrained target tracking after both participants have their final world placement. */
    void aim(GpuUnitModel model, BoardScene.Unit unit, UnitAttack attack, Vector3 target, ModelInstance victim) {
        if (instance == null || attack == null || attack.event.entityId() != unit.id() || attack.aimWeight() <= 0
              || dying) { return; }
        if (attack.shot()) {
            aimShots(model, unit, List.of(attack), ignored -> victim);
        } else if (fallen < .01f && crouch < .01f && unit.model().state().structure().anatomy() != null) {
            physicalContact(model, attack, target);
        }
    }

    private record Aim(UnitAttack attack, UnitEquipmentAssembly.Binding binding, UnitModelDescriptor.Emitter emitter,
          Node joint, float limit, ModelInstance victim) {
        float start() { return attack.group == null ? attack.delay : attack.group.start; }
        float clock() { return attack.group == null ? attack.seconds + attack.delay : attack.group.clock; }
        Object group() { return attack.group == null ? attack : attack.group; }
        float turn() { return attack.group == null ? UnitAttack.smooth(attack.seconds / UnitAttack.ANTICIPATION_SECONDS) : attack.group.turn(); }
        float recovery(float time) {
            return attack.group == null ? 1 - UnitAttack.smooth((time - attack.delay - attack.firingEndSeconds()) / UnitAttack.RECOVERY_SECONDS)
                  : attack.group.recovery(time);
        }
    }

    /** One target pass poses each shared joint once, then each gun corrects for its own hit/miss point. */
    void aimShots(GpuUnitModel model, BoardScene.Unit unit, List<UnitAttack> attacks,
          java.util.function.Function<UnitAttack, ModelInstance> victims) {
        if (instance == null || dying) { return; }
        Map<Node, List<Aim>> requests = new java.util.LinkedHashMap<>();
        for (var attack : attacks) {
            if (!attack.shot() || attack.event.entityId() != unit.id()) { continue; }
            for (var binding : model.equipment()) {
                var attachment = instance.getNode(binding.node());
                if (!attack.fires(binding) || attachment == null || !UnitBounds.subtree(attachment).isValid()) { continue; }
                var emitter = binding.emitters().stream().filter(item -> "muzzle".equals(item.role())
                      || "beam".equals(item.role()) || "launcher".equals(item.role())).findFirst().orElse(null);
                if (emitter == null) { continue; }
                Node joint = attachment;
                float limit = 25;
                for (Node ancestor = attachment.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
                    String role = role(ancestor);
                    if ("turret".equals(role) || "turret2".equals(role) || "leftForearm".equals(role) || "rightForearm".equals(role)
                          || "leftArm".equals(role) || "rightArm".equals(role)) {
                        joint = ancestor;
                        // Resting arm barrels point down: reaching level fire alone can require 90 degrees.
                        limit = "turret".equals(role) || "turret2".equals(role) ? 180 : ARM_AIM_LIMIT_DEGREES;
                        break;
                    }
                }
                requests.computeIfAbsent(joint, ignored -> new ArrayList<>())
                      .add(new Aim(attack, binding, emitter, joint, limit, victims.apply(attack)));
            }
        }
        for (var entry : requests.entrySet()) {
            Aim current = null, previous = null;
            for (var request : entry.getValue()) {
                if (request.clock() >= request.start() && (current == null || request.start() > current.start())) { current = request; }
            }
            if (current == null) { continue; }
            for (var request : entry.getValue()) {
                if (request.start() < current.start() && (previous == null || request.start() > previous.start())) { previous = request; }
            }
            var from = previous == null ? new Quaternion()
                  : new Quaternion().slerp(aimRotation(previous, previous.joint(), previous.limit()), previous.recovery(current.start()));
            var rotation = from.slerp(aimRotation(current, current.joint(), current.limit()), current.turn());
            float weight = current.recovery(current.clock());
            entry.getKey().rotation.mulLeft(new Quaternion().slerp(rotation, weight));
            instance.calculateTransforms();
            for (var request : entry.getValue()) {
                var attachment = instance.getNode(request.binding().node());
                if (request.group() == current.group() && attachment != request.joint()) {
                    attachment.rotation.mulLeft(new Quaternion().slerp(aimRotation(request, attachment, 25), current.turn() * weight));
                    instance.calculateTransforms();
                }
            }
        }
        applyRecoil(model);
    }

    /** Re-evaluate after aiming as well: a gun's own correction changes its barrel direction inside the arm. */
    private void applyRecoil(GpuUnitModel model) {
        for (var binding : model.equipment()) {
            Float recoil = mountRecoil.get(binding.node());
            Node node = instance.getNode(binding.node()), rest = model.instance.getNode(binding.node());
            if (recoil == null || node == null || rest == null) { continue; }
            var emitter = binding.emitters().isEmpty() ? null : binding.emitters().getFirst();
            UnitModelAttachment.barrelDirection(instance, node, emitter, recoilDirection);
            node.translation.set(rest.translation).mulAdd(recoilDirection, -UnitAttack.RECOIL_DISTANCE * recoil);
        }
        instance.calculateTransforms();
    }

    private Quaternion aimRotation(Aim request, Node joint, float limit) {
        var attack = request.attack();
        var origin = new Vector3();
        var forward = new Vector3();
        UnitModelAttachment.emitter(instance, request.emitter(), origin, forward);
        var aim = attack.endpoint(request.victim(), origin, new Vector3());
        int ordinal = (request.binding().node() + ":1").hashCode();
        if (attack.event.result().hit() && !attack.defensive() && !attack.arcing(request.binding())) {
            attack.hitEndpoint(request.victim(), origin, ordinal, 0, 1, aim);
        } else if (!attack.event.result().hit() && !attack.defensive()) {
            if ("laser".equals(request.emitter().effect()) || "ppc".equals(request.emitter().effect())) {
                attack.beamAim(request.victim(), origin, ordinal, aim);
            } else if (!attack.arcing(request.binding())) { attack.endpoint(request.victim(), origin, true, ordinal, aim); }
        }
        float loft = attack.arcing(request.binding()) ? MathUtils.PI * UnitAttack.arcHeight(origin, aim) : 0;
        return trackingRotation(joint, forward, aim.sub(origin).add(0, 0, loft).nor(), limit);
    }

    /** Deterministic rigid collapse; restored wrecks use the terminal pose without replaying an event. */
    private void deathPose(float progress) {
        for (Body body : bodies) {
            Joint root = body.joints.get("root");
            if (root == null) { continue; }
            body.reset();
            if (body.rig.mek() || body.rig.trooper() || "proto-v1".equals(body.rig.type())) {
                // Outward-looking troops fall forward, preserving space between the formation's bodies.
                root.rotate(Vector3.X, (body.rig.trooper() ? -90 : "quad-v1".equals(body.rig.type()) ? 65 : 90) * progress);
                body.rotate("leftArm", Vector3.X, -22 * progress);
                body.rotate("rightArm", Vector3.X, 18 * progress);
                body.rotate("head", Vector3.Y, 18 * progress);
            } else if ("naval-v1".equals(body.rig.type())) {
                root.rotate(Vector3.Y, 28 * progress);
                root.node().translation.z -= body.restHeight * .6f * progress;
            } else if ("static-v1".equals(body.rig.type())) {
                root.node().scale.z *= 1 - .6f * progress;
            } else {
                root.rotate(Vector3.Y, ("aircraft-v1".equals(body.rig.type()) ? 32 : 12) * progress);
                body.rotate("turret", Vector3.Z, 24 * progress);
            }
        }
    }

    private record StrikePart(Joint upper, Joint lower, Node tip, Vector3 point) {
        Vector3 world(ModelInstance instance) { return point.cpy().mul(tip.globalTransform).mul(instance.transform); }
    }

    /** Contact uses the authored physical-weapon tip, or the visible hand/foot of the game-selected limb. */
    private StrikePart strikePart(GpuUnitModel model, Body body, UnitAttack attack, boolean left) {
        if (!body.rig.mek()) { return null; }
        boolean kick = attack.event.result().kind() == megamek.common.ResolvedAttack.Kind.KICK;
        String role = kick ? body.rig.kickingLeg(attack.event.result().limb()) : left ? "leftArm" : "rightArm";
        String lowerRole = kick ? role.equals("leftLeg") ? "leftShin" : role.equals("rightLeg") ? "rightShin" : role + "Shin"
              : left ? "leftForearm" : "rightForearm";
        Joint upper = body.joints.get(role), lower = body.joints.get(lowerRole);
        if (upper == null || !UnitBounds.subtree(upper.node()).isValid()) { return null; }
        if (attack.event.result().kind() == megamek.common.ResolvedAttack.Kind.CLUB) {
            for (var binding : model.equipment()) {
                if (!attack.fires(binding)) { continue; }
                for (var emitter : binding.emitters()) {
                    Node tip = instance.getNode(emitter.node());
                    if ("contact".equals(emitter.role()) && tip != null && UnitBounds.subtree(tip).isValid()) {
                        var p = emitter.position();
                        return new StrikePart(upper, lower, tip, new Vector3(p.get(0), p.get(1), p.get(2)));
                    }
                }
            }
            return null; // A missing physical weapon cannot turn into a punch.
        }
        String tipRole = kick ? role.equals("leftLeg") ? "leftFoot" : role.equals("rightLeg") ? "rightFoot" : role + "Foot" : "";
        Node tip = kick && body.joints.containsKey(tipRole) ? body.joints.get(tipRole).node()
              : instance.getNode(upper.node().id + "@hand");
        if (tip == null) { tip = lower == null ? upper.node() : lower.node(); }
        Node rest = model.instance.getNode(tip.id);
        var bounds = rest == null ? null : UnitBounds.subtree(rest);
        if (bounds == null || !bounds.isValid() || !UnitBounds.subtree(tip).isValid()) { return null; }
        var point = bounds.getCenter(new Vector3());
        if (kick) { point.y = bounds.max.y; } else { point.z = bounds.min.z; }
        point.mul(rest.globalTransform.cpy().inv());
        return new StrikePart(upper, lower, tip, point);
    }

    private void physicalContact(GpuUnitModel model, UnitAttack attack, Vector3 target) {
        boolean left = attack.event.result().limb() == megamek.common.units.Mek.LOC_LEFT_ARM
              || attack.event.result().limb() == megamek.common.units.Mek.LOC_LEFT_LEG;
        for (Body body : bodies) {
            var part = strikePart(model, body, attack, left);
            if (part == null) { continue; }
            if (attack.approach == null) {
                // Measure once in the rest pose. Recomputing after each swing would move the contact stance.
                var inverse = instance.transform.cpy().inv();
                var pivot = part.upper.node().globalTransform.getTranslation(new Vector3());
                var tip = part.world(instance).mul(inverse);
                float reach = pivot.dst(tip);
                if (part.lower != null) {
                    var elbow = part.lower.node().globalTransform.getTranslation(new Vector3());
                    reach = pivot.dst(elbow) + elbow.dst(tip);
                }
                var approach = target.cpy().mul(inverse).sub(pivot);
                float vertical = approach.z;
                approach.z = 0;
                float horizontalReach = (float) Math.sqrt(Math.max(0, reach * reach - vertical * vertical)) * .88f;
                float distance = Math.max(0, approach.len() - horizontalReach);
                attack.approach = approach.nor().scl(distance).rot(instance.transform)
                      .limit(BoardGeometry.HEIGHT * PHYSICAL_APPROACH_HEXES);
            }
            float travel = attack.approachWeight();
            float t = attack.travelProgress();
            float gait = MathUtils.clamp(Math.min(t, 1 - t) * 10, 0, 1);
            var local = attack.approach.cpy().rot(instance.transform.cpy().inv());
            float distance = local.len() * travel / body.memberScale;
            float yaw = MathUtils.atan2(local.x, local.y) * MathUtils.radiansToDegrees;
            float phase = body.cycleDistance == 0 ? 0 : distance / body.cycleDistance * MathUtils.PI2;
            body.travelPitch.forEach((role, angle) -> body.rotate(role, Vector3.X, angle * gait));
            legs(body, phase, gait, 0, 0, yaw);
            body.rotate("leftArm", Vector3.X, -MathUtils.sin(phase) * 16 * gait);
            body.rotate("rightArm", Vector3.X, MathUtils.sin(phase) * 16 * gait);
            meleePose(body, attack, left);
            settleContacts();
            instance.transform.trn(attack.approach.x * travel, attack.approach.y * travel, 0);
            float weight = attack.contactWeight();
            if (weight > 0) { reach(part, target, weight); }
            if (attack.event.result().kind() == megamek.common.ResolvedAttack.Kind.PUSH) {
                var other = strikePart(model, body, attack, !left);
                if (other != null && weight > 0) { reach(other, target, weight); }
            }
        }
    }

    private static void meleePose(Body body, UnitAttack attack, boolean left) {
        float swing = attack.strike(), windup = attack.windup();
        String arm = left ? "leftArm" : "rightArm";
        String forearm = left ? "leftForearm" : "rightForearm";
        switch (attack.event.result().kind()) {
            case PUNCH -> {
                body.rotate(arm, Vector3.X, -25 * windup + 105 * swing);
                body.rotate(arm, Vector3.Y, (left ? -1 : 1) * 35 * windup * (1 - swing));
                body.rotate(forearm, Vector3.X, 65 * windup - 60 * swing);
                body.rotate("torso", Vector3.Z, (left ? 1 : -1) * 15 * windup * (1 - swing));
            }
            case KICK -> {
                String leg = body.rig.kickingLeg(attack.event.result().limb());
                body.rotate(leg, Vector3.X, 30 * windup + 35 * swing);
                body.rotate("leftLeg".equals(leg) ? "leftShin" : "rightLeg".equals(leg) ? "rightShin" : leg + "Shin",
                      Vector3.X, -65 * windup + 55 * swing);
            }
            case PUSH -> {
                body.rotate("leftArm", Vector3.X, 65 * windup + 15 * swing);
                body.rotate("rightArm", Vector3.X, 65 * windup + 15 * swing);
                body.rotate("leftForearm", Vector3.X, 35 * windup * (1 - swing));
                body.rotate("rightForearm", Vector3.X, 35 * windup * (1 - swing));
                body.rotate("torso", Vector3.X, 12 * swing);
            }
            case CLUB -> {
                body.rotate(arm, Vector3.X, attack.thrust() ? 65 * windup + 20 * swing : 155 * windup - 85 * swing);
                body.rotate(forearm, Vector3.X, (attack.thrust() ? -30 : 20) * windup * (1 - swing));
                body.rotate(arm, Vector3.Y, (left ? -1 : 1) * 15 * windup);
            }
            default -> { }
        }
    }

    private void reach(StrikePart part, Vector3 target, float weight) {
        var goal = part.world(instance).lerp(target, weight);
        for (int iteration = 0; iteration < 40; iteration++) {
            if (part.lower != null) { reachJoint(part, part.lower.node(), goal); }
            reachJoint(part, part.upper.node(), goal);
            if (part.world(instance).dst2(goal) < .01f) { break; }
        }
    }

    private void reachJoint(StrikePart part, Node joint, Vector3 goal) {
        var pivot = joint.globalTransform.getTranslation(new Vector3()).mul(instance.transform);
        track(joint, part.world(instance).sub(pivot).nor(), goal.cpy().sub(pivot).nor(), 100, 1);
    }

    Vector3 physicalTip(GpuUnitModel model, UnitAttack attack) {
        boolean left = attack.event.result().limb() == megamek.common.units.Mek.LOC_LEFT_ARM
              || attack.event.result().limb() == megamek.common.units.Mek.LOC_LEFT_LEG;
        for (Body body : bodies) {
            var part = strikePart(model, body, attack, left);
            if (part != null) { return part.world(instance); }
        }
        return null;
    }

    private String role(Node node) {
        for (var body : bodies) {
            for (var joint : body.joints.entrySet()) {
                if (joint.getValue().node() == node) { return joint.getKey(); }
            }
        }
        return "";
    }

    private Matrix4 parentWorld(Node node) {
        var parent = new Matrix4(instance.transform);
        return node.getParent() == null ? parent : parent.mul(node.getParent().globalTransform);
    }

    private void track(Node joint, Vector3 forward, Vector3 target, float limit, float weight) {
        joint.rotation.mulLeft(new Quaternion().slerp(trackingRotation(joint, forward, target, limit), weight));
        instance.calculateTransforms();
    }

    private Quaternion trackingRotation(Node joint, Vector3 forward, Vector3 target, float limit) {
        var inverse = parentWorld(joint).inv();
        forward.rot(inverse).nor();
        target.rot(inverse).nor();
        if (forward.isZero() || target.isZero()) { return new Quaternion(); }
        var rotation = new Quaternion().setFromCross(forward, target);
        float angle = rotation.getAngle();
        return new Quaternion().slerp(rotation, Math.min(1, limit / Math.max(.001f, angle)));
    }

    private static void legs(Body body, float phase, float gait, float crouch, float jump, float yaw) {
        for (int index = 0; index < LEGS.length; index++) {
            String[] pair = LEGS[index];
            // Tripods use three phases; quads alternate diagonal pairs. Other bipeds alternate their two legs.
            float offset = "tripod-v1".equals(body.rig.type()) && index < 3 ? index * MathUtils.PI2 / 3
                  : switch (index) { case 1, 4, 5, 8, 9 -> MathUtils.PI; default -> 0; };
            if (gait > 0 && crouch == 0 && jump == 0 && body.cycleDistance > 0
                  && plantedStep(body, pair, phase + offset, gait, yaw)) {
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
    private static boolean plantedStep(Body body, String[] roles, float phase, float weight, float yaw) {
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
            lift = (upper + lower) * body.stepLift * sine * sine;
        }
        // Asymmetric authored poses must still put both supporting feet on the same ground plane.
        float z = body.groundReach - lift;
        float reach = (float) Math.hypot(y, z);
        float kneeCos = MathUtils.clamp((upper * upper + lower * lower - reach * reach) / (2 * upper * lower), -1, 1);
        float hipCos = MathUtils.clamp((upper * upper + reach * reach - lower * lower) / (2 * upper * reach), -1, 1);
        float hip = (MathUtils.atan2(y, z) + (float) Math.acos(hipCos)) * MathUtils.radiansToDegrees;
        float knee = ((float) Math.acos(kneeCos) - MathUtils.PI) * MathUtils.radiansToDegrees;
        // Keep the torso facing its game direction while the leg's stepping plane follows lateral travel.
        body.joints.get(roles[0]).turn(-yaw * weight);
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
