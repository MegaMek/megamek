/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

/** Cosmetic formation placement. UnitMotion alone owns route/time; these are children, never transported Entities. */
final class InfantryMotion {
    record Step(float gait, float distance, float standing) { }
    private static final float DEPART_END = .22f;
    private static final float PARK_START = .72f;
    private final Map<String, Member> members = new LinkedHashMap<>();
    private float layoutScale = Float.NaN;

    private static final class Member {
        final UnitRig rig;
        final Node node;
        final Vector3 rest, scale, door;
        final BoundingBox footprint;
        final List<NodePart> visibleParts = new ArrayList<>();
        final Vector3 start = new Vector3();
        final Vector3 goal = new Vector3();
        final Vector3 lastDrivePosition = new Vector3();
        float drivenDistance;
        float heading, startHeading, goalHeading;
        long sequence = -1;
        boolean reversing;
        Step step;
        float verticalOffset;

        Member(UnitRig rig, Node node, Node rest, Member previous) {
            this.rig = rig;
            this.node = node;
            this.rest = new Vector3(rest.translation);
            scale = new Vector3(rest.scale);
            var shape = rest.copy();
            shape.translation.setZero();
            shape.rotation.idt();
            shape.calculateTransforms(true);
            footprint = UnitBounds.subtree(shape);
            heading = -rest.rotation.getAngleAround(Vector3.Z);
            Node marker = rig.joints().containsKey("boarding")
                  ? UnitAnimator.find(rest.getChildren(), rig.joints().get("boarding")) : null;
            door = marker == null ? new Vector3(0, -15, 0)
                  : marker.globalTransform.getTranslation(new Vector3()).mul(rest.globalTransform.cpy().inv());
            start.set(this.rest);
            goal.set(this.rest);
            visibleParts(node, visibleParts);
            if (previous != null) {
                start.set(previous.start);
                goal.set(previous.goal);
                heading = previous.heading;
                startHeading = previous.startHeading;
                goalHeading = previous.goalHeading;
                sequence = previous.sequence;
                reversing = previous.reversing;
                lastDrivePosition.set(previous.lastDrivePosition);
                drivenDistance = previous.drivenDistance;
                node.translation.set(previous.node.translation);
            }
        }

        private static void visibleParts(Node node, List<NodePart> result) {
            node.parts.forEach(part -> { if (part.enabled) { result.add(part); } });
            node.getChildren().forEach(child -> visibleParts(child, result));
        }

        void visible(boolean visible) {
            visibleParts.forEach(part -> part.enabled = visible);
        }

        void orient(float value) {
            heading = value;
            node.rotation.set(Vector3.Z, -value);
        }

        Vector3 doorway() {
            return new Vector3(door).scl(scale).rotate(Vector3.Z, -heading).add(node.translation);
        }

        void fit(Vector3 position, float facing, float scale) {
            InfantryFootprint.fit(position, footprint, facing, scale);
        }
    }

    void bind(GpuUnitModel model, ModelInstance placed) {
        Map<String, Member> previous = new LinkedHashMap<>(members);
        members.clear();
        for (var rig : model.rigs()) {
            if (rig.container() != null && (rig.trooper() || rig.transport())) {
                var rest = model.instance.getNode(rig.container());
                members.put(rig.container(), new Member(rig, placed.getNode(rig.container()), rest, previous.get(rig.container())));
            }
        }
    }

    Step step(String member) {
        Member pose = members.get(member);
        return pose == null ? null : pose.step;
    }

    float forward(String member) {
        var pose = members.get(member);
        return pose != null && pose.reversing ? -1 : 1;
    }

    float drivenDistance(String member) {
        var pose = members.get(member);
        return pose == null ? 0 : pose.drivenDistance;
    }

    float verticalOffset(String member) {
        var pose = members.get(member);
        return pose == null ? 0 : pose.verticalOffset;
    }

    void apply(GpuUnitModel model, BoardScene.Unit unit, UnitMotion.Sample motion) {
        var travel = motion.boarding();
        float scale = model.horizontalScale(unit);
        var vehicles = members.values().stream().filter(member -> member.rig.transport()).toList();
        boolean fitGoals = travel != null && (scale != layoutScale
              || members.values().stream().anyMatch(member -> member.sequence != travel.sequence()));
        layoutScale = scale;
        if (travel != null && members.values().stream().allMatch(member -> member.sequence < 0)) {
            fitFormation(vehicles, false, scale);
        }
        for (Member member : members.values()) {
            if (travel != null && member.sequence != travel.sequence()) {
                member.sequence = travel.sequence();
                member.start.set(member.node.translation);
                member.lastDrivePosition.set(travel.position(0)).scl(1 / scale)
                      .add(member.start);
                member.drivenDistance = 0;
                member.startHeading = member.heading;
                member.reversing = member.rig.transport()
                      && Math.abs(UpperBodyTurn.shortestTurn(travel.heading(0) - member.heading)) > 110;
                member.goal.set(member.rest).rotate(Vector3.Z, -travel.arrivalHeading());
                member.goalHeading = member.rig.transport()
                      ? travel.arrivalHeading() + Math.signum(member.rest.x) * 40 + (member.reversing ? 180 : 0)
                      : watchHeading(unit.id(), member.rig.container(), member.goal, travel.destination());
            }
            if (fitGoals) {
                member.goal.set(member.rest).rotate(Vector3.Z, -travel.arrivalHeading());
            }
        }
        if (fitGoals) {
            // Keep the captured parking positions through unloading and casualty/material rebinds.
            fitFormation(vehicles, true, scale);
        }
        for (Member member : members.values()) {
            member.node.scale.set(member.scale);
            member.visible(true);
            member.step = null;
            member.verticalOffset = 0;
        }
        if (travel == null || vehicles.isEmpty()) {
            for (Member member : members.values()) {
                if (member.rig.trooper()) {
                    var individual = motion.member(unit.id(), member.rig.container());
                    float target = watchHeading(unit.id(), member.rig.container(), member.rest,
                          unit.location().coords().hashCode());
                    if (individual.moving() || individual.progress() != 0 || motion.group() == null) {
                        member.orient(individual.moving() ? individual.heading() : MathUtils.lerpAngleDeg(individual.heading(), target,
                              Math.min(1, individual.settledSeconds() / UnitMotion.FORMATION_SETTLE_SECONDS)));
                    }
                    member.node.translation.set(member.rest);
                    member.fit(member.node.translation, member.heading, scale);
                    if (motion.group() != null) {
                        var offset = motion.group().offset(unit.id(), member.rig.container());
                        member.verticalOffset = offset.z / model.verticalScale(scale, unit);
                        member.node.translation.add(offset.x / scale, offset.y / scale, member.verticalOffset);
                    }
                } else if (motion.moving()) {
                    member.orient(motion.heading());
                } else {
                    member.node.translation.set(member.rest);
                }
            }
            fitVehicles(vehicles, false, scale);
            avoidVehicles(vehicles, false, scale);
            return;
        }
        for (Member vehicle : vehicles) {
            vehicle(vehicle, travel, scale);
        }
        int index = 0;
        for (Member member : members.values()) {
            if (member.rig.trooper()) {
                // Stable slot IDs keep an existing passenger on its vehicle when another trooper disappears.
                int slot = Integer.parseInt(member.rig.container().substring("trooper-".length()));
                Member vehicle = vehicles.get(Math.floorMod(slot, vehicles.size()));
                passenger(member, vehicle, travel, index++);
            }
        }
    }

    private void fitFormation(List<Member> vehicles, boolean goal, float scale) {
        for (var member : members.values()) {
            if (member.rig.trooper()) {
                member.fit(goal ? member.goal : member.node.translation, goal ? member.goalHeading : member.heading, scale);
            }
        }
        fitVehicles(vehicles, goal, scale);
        avoidVehicles(vehicles, goal, scale);
    }

    private static void fitVehicles(List<Member> vehicles, boolean goal, float scale) {
        if (vehicles.size() == 2) {
            var a = vehicles.get(0);
            var b = vehicles.get(1);
            InfantryFootprint.fitPair(goal ? a.goal : a.node.translation, a.footprint, goal ? a.goalHeading : a.heading,
                  goal ? b.goal : b.node.translation, b.footprint, goal ? b.goalHeading : b.heading, scale);
        } else if (!vehicles.isEmpty()) {
            var vehicle = vehicles.getFirst();
            InfantryFootprint.fit(goal ? vehicle.goal : vehicle.node.translation, vehicle.footprint,
                  goal ? vehicle.goalHeading : vehicle.heading, scale);
        }
    }

    private void avoidVehicles(List<Member> vehicles, boolean goal, float scale) {
        if (vehicles.isEmpty()) { return; }
        var obstacles = vehicles.stream().map(vehicle -> {
            var shape = InfantryFootprint.polygon(vehicle.footprint, goal ? vehicle.goalHeading : vehicle.heading);
            var position = goal ? vehicle.goal : vehicle.node.translation;
            shape.setPosition(position.x, position.y);
            return shape;
        }).toList();
        for (var member : members.values()) {
            if (member.rig.trooper()) {
                InfantryFootprint.avoid(goal ? member.goal : member.node.translation, member.footprint,
                      goal ? member.goalHeading : member.heading, obstacles, scale);
            }
        }
    }

    private static void vehicle(Member member, UnitMotion.Boarding travel, float scale) {
        if (travel.stage() == UnitMotion.Stage.BOARD) {
            member.node.translation.set(member.start);
            member.orient(member.startHeading);
            return;
        }
        if (travel.stage() == UnitMotion.Stage.UNLOAD) {
            var point = travel.position(1).scl(1 / scale).add(member.goal);
            member.drivenDistance += point.dst(member.lastDrivePosition);
            member.lastDrivePosition.set(point);
            member.node.translation.set(member.goal);
            member.orient(member.goalHeading);
            return;
        }
        float t = travel.progress();
        Vector3 root = travel.position(t).scl(1 / scale);
        Vector3 point;
        if (t < DEPART_END) {
            Vector3 start = travel.position(0).scl(1 / scale).add(member.start);
            Vector3 end = travel.position(DEPART_END).scl(1 / scale).add(member.rest);
            point = steer(member, start, end, member.startHeading + (member.reversing ? 180 : 0),
                  travel.heading(DEPART_END), t / DEPART_END);
        } else if (t > PARK_START) {
            Vector3 start = travel.position(PARK_START).scl(1 / scale).add(member.rest);
            Vector3 end = travel.position(1).scl(1 / scale).add(member.goal);
            point = steer(member, start, end, travel.heading(PARK_START), member.goalHeading - (member.reversing ? 180 : 0),
                  (t - PARK_START) / (1 - PARK_START));
        } else {
            point = new Vector3(root).add(member.rest);
            member.orient(travel.heading(t) + (member.reversing ? 180 : 0));
        }
        member.drivenDistance += point.dst(member.lastDrivePosition);
        member.lastDrivePosition.set(point);
        member.node.translation.set(point.sub(root));
        member.node.translation.z = member.rest.z;
    }

    /** Turn by driving through a short cubic curve. Heading follows its tangent, including the parking approach. */
    private static Vector3 steer(Member member, Vector3 start, Vector3 end, float from, float to, float progress) {
        float distance = start.dst(end) * .36f;
        var a = new Vector3(start).mulAdd(direction(from), distance);
        var b = new Vector3(end).mulAdd(direction(to), -distance);
        // The shared route already accelerates/brakes. Easing each parking sub-curve would cause extra stops.
        float t = MathUtils.clamp(progress, 0, 1), u = 1 - t;
        var tangent = new Vector3(a).sub(start).scl(u * u).mulAdd(new Vector3(b).sub(a), 2 * u * t)
              .mulAdd(new Vector3(end).sub(b), t * t);
        member.orient((tangent.isZero(.0001f) ? to : MathUtils.atan2(tangent.x, tangent.y) * MathUtils.radiansToDegrees)
              + (member.reversing ? 180 : 0));
        return new Vector3(start).scl(u * u * u).mulAdd(a, 3 * u * u * t).mulAdd(b, 3 * u * t * t)
              .mulAdd(end, t * t * t);
    }

    private static void passenger(Member member, Member vehicle, UnitMotion.Boarding travel, int order) {
        float offset = order * .035f;
        Vector3 door = vehicle.doorway();
        if (travel.stage() == UnitMotion.Stage.DRIVE) {
            member.node.translation.set(door);
            member.visible(false);
            member.step = new Step(0, 0, 1);
            return;
        }
        boolean boarding = travel.stage() == UnitMotion.Stage.BOARD;
        float progress = MathUtils.clamp((travel.progress() - offset) / (1 - offset), 0, 1);
        float walk = boarding ? Math.min(1, progress / .9f) : Math.min(1, progress / .8f);
        var from = boarding ? member.start : door;
        var to = boarding ? door : member.goal;
        member.node.translation.set(from).lerp(to, smooth(walk));
        float direction = MathUtils.atan2(to.x - from.x, to.y - from.y) * MathUtils.radiansToDegrees;
        float turn = boarding ? Math.min(1, progress * 7) : Math.max(0, (progress - .8f) / .2f);
        member.orient(boarding ? MathUtils.lerpAngleDeg(member.startHeading, direction, turn)
              : MathUtils.lerpAngleDeg(direction, member.goalHeading, turn));
        if (boarding && progress >= .97f || !boarding && progress <= .02f) {
            member.visible(false);
        }
        float gait = Math.min(1, Math.min(walk, 1 - walk) * 10);
        float standing = boarding ? Math.min(1, progress * 6) : 1 - smooth(Math.max(0, (progress - .7f) / .3f));
        member.step = new Step(gait, from.dst(to) * smooth(walk), standing);
    }

    private static Vector3 direction(float heading) {
        return new Vector3(MathUtils.sinDeg(heading), MathUtils.cosDeg(heading), 0);
    }

    private static float smooth(float value) {
        float t = MathUtils.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static float watchHeading(int unit, String member, Vector3 position, int destination) {
        int seed = 31 * (31 * unit + member.hashCode()) + destination;
        seed ^= seed >>> 16;
        seed *= 0x45d9f3b;
        seed ^= seed >>> 16;
        float outward = MathUtils.atan2(position.x, position.y) * MathUtils.radiansToDegrees;
        int formation = 31 * unit + destination;
        int slot = Integer.parseInt(member.substring("trooper-".length()));
        // At most one reserved slot occasionally watches inward. Casualties never reroll surviving headings.
        boolean inward = Math.floorMod(formation, 4) == 0 && slot == Math.floorMod(formation / 4, 6);
        return outward + Math.floorMod(seed, 701) * .1f - 35 + (inward ? 180 : 0);
    }
}
