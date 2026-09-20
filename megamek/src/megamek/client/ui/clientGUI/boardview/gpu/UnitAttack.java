/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import megamek.common.ResolvedAttack;

/** A resolved attack's presentation clock. Outcomes and damage are never calculated here. */
final class UnitAttack {
    static final float ANTICIPATION_SECONDS = .2f;
    static final float RECOVERY_SECONDS = .35f;
    static final float DEATH_SECONDS = 1.2f;
    static final float MELEE_WALK_SECONDS = 1.05f;
    static final float MELEE_RUN_SECONDS = .65f;
    static final float MELEE_SWING_SECONDS = .4f;
    final BoardScene.Combat event;
    final float contactSeconds;
    final float duration;
    private final Map<Integer, Set<Integer>> firingMounts = new HashMap<>();
    private final Map<Long, ResolvedAttack.Shot> profiles = new HashMap<>();
    private Vector3 physicalContact;
    private Vector3 contactCenter;
    Vector3 approach;
    final float approachSeconds;
    java.util.function.Function<Ray, BoardGeometry.Hit> landscape;
    private final Map<Integer, Vector3> misses = new HashMap<>();
    private final Map<Integer, Vector3> beamMisses = new HashMap<>();
    private UnitPicking surfaces;
    private record HitPoint(ModelInstance target, String location, int seed) { }
    private final Map<HitPoint, UnitPicking.SurfacePoint> hitPoints = new HashMap<>();
    float seconds;
    float delay;

    UnitAttack(BoardScene.Combat event) {
        this.event = event;
        event.result().mounts().forEach(mount -> firingMounts
              .computeIfAbsent(mount.entityId(), ignored -> new HashSet<>()).add(mount.equipmentIndex()));
        event.result().mounts().forEach(mount -> {
            if (mount.shot() != null) { profiles.put(mountKey(mount.entityId(), mount.equipmentIndex()), mount.shot()); }
        });
        float distance = BoardGeometry.center(event.attacker().location().coords(), event.attacker().location().elevation())
              .dst(BoardGeometry.center(event.destination().coords(), event.destination().elevation())) / BoardGeometry.HEIGHT;
        approachSeconds = event.result().kind() == ResolvedAttack.Kind.PUSH ? MELEE_RUN_SECONDS : MELEE_WALK_SECONDS;
        contactSeconds = death() ? DEATH_SECONDS : shot() ? ANTICIPATION_SECONDS + MathUtils.clamp(distance * .05f, .18f, .7f)
              : approachSeconds + MELEE_SWING_SECONDS;
        duration = death() ? DEATH_SECONDS : contactSeconds + RECOVERY_SECONDS + (shot() ? 0 : MELEE_RUN_SECONDS);
    }

    boolean shot() {
        return event.result().kind() == ResolvedAttack.Kind.SHOT;
    }

    boolean death() { return event.result().kind() == ResolvedAttack.Kind.DEATH; }

    boolean defensive() { return event.result().shot() != null && event.result().shot().defensive(); }

    float deathProgress() { return death() ? smooth(seconds / DEATH_SECONDS) : 0; }

    boolean fires(UnitEquipmentAssembly.Binding binding) {
        int owner = binding.memberId() < 0 ? event.attacker().id() : binding.memberId();
        var state = event.attacker().model() == null ? null : event.attacker().model().state();
        var appearance = state == null ? null : state.appearance();
        if (appearance != null && binding.memberId() >= 0) { appearance = appearance.fighters().get(binding.memberId()); }
        return firingMounts.getOrDefault(owner, Set.of()).contains(binding.index())
              && (appearance == null || !appearance.inoperableEquipment().contains(binding.index()));
    }

    /** The same visible posed endpoint is used for aiming and effects in either camera. */
    static Vector3 center(ModelInstance instance, BoardScene.Waypoint location, Vector3 result) {
        return instance == null ? result.set(BoardGeometry.center(location.coords(), location.elevation()))
              .add(0, 0, BoardGeometry.LEVEL * .5f) : UnitBounds.world(instance).getCenter(result);
    }

    /** A cosmetic miss must clear the posed target, including large hulls; it never changes the resolved outcome. */
    Vector3 endpoint(ModelInstance targetInstance, Vector3 origin, Vector3 result) {
        if (!shot() && physicalContact != null) { return result.set(physicalContact); }
        return endpoint(targetInstance, origin, !event.result().hit(), 0, result);
    }

    /** Use the same posed mesh picker as the board. This is a contact location, never another hit decision. */
    Vector3 contact(ModelInstance target, Vector3 origin, UnitPicking picking, Vector3 result) {
        if (physicalContact != null) {
            result.set(physicalContact);
            if (event.result().kind() == ResolvedAttack.Kind.PUSH && contactCenter != null && target != null) {
                result.add(UnitBounds.world(target).getCenter(new Vector3()).sub(contactCenter));
            }
            return result;
        }
        endpoint(target, origin, result);
        if (event.result().hit() && target != null && !event.result().impacts().isEmpty()) {
            hitEndpoint(target, origin, event.result().limb(), 0, 1, result);
        } else if (event.result().hit() && target != null) {
            var bounds = UnitBounds.world(target);
            var center = bounds.getCenter(new Vector3());
            float height = event.result().kind() == ResolvedAttack.Kind.KICK ? .18f : .65f;
            var side = new Vector3(center.y - origin.y, origin.x - center.x, 0).nor();
            for (float offset : new float[] { 0, -.22f, .22f }) {
                var point = center.cpy().mulAdd(side, offset * Math.max(bounds.getWidth(), bounds.getHeight()));
                point.z = bounds.min.z + bounds.getDepth() * height;
                var ray = new Ray(origin, point.sub(origin).nor());
                float distance = picking.distance(target, ray);
                if (Float.isFinite(distance)) {
                    result.set(origin).mulAdd(ray.direction, (float) Math.sqrt(distance));
                    break;
                }
            }
        }
        physicalContact = result.cpy();
        if (target != null) { contactCenter = UnitBounds.world(target).getCenter(new Vector3()); }
        return result;
    }

    Vector3 endpoint(ModelInstance targetInstance, Vector3 origin, boolean missed, int missile, Vector3 result) {
        var profile = event.result().shot();
        if (profile != null && profile.impact() != null) {
            return result.set(BoardGeometry.center(profile.impact().coords(), profile.impact().elevation()));
        }
        if (defensive()) {
            // Point defense aims into the incoming approach, never at the launcher's hull.
            center(targetInstance, event.destination(), result);
            return result.set(origin.cpy().lerp(result, .25f)).add(0, 0, BoardGeometry.LEVEL * .35f);
        }
        center(targetInstance, event.destination(), result);
        if (!missed) { return shot() ? hitEndpoint(targetInstance, origin, missile, 0, 1, result) : result; }
        if (misses.containsKey(missile)) { return result.set(misses.get(missile)); }
        scatter(targetInstance, origin, missile, false, result);
        if (landscape != null) { misses.put(missile, result.cpy()); }
        return result;
    }

    /** Cosmetic placement on engine-reported locations. Unknown/unsplit locations use the actual hull surface. */
    Vector3 hitEndpoint(ModelInstance target, Vector3 origin, int seed, int ordinal, int count, Vector3 result) {
        center(target, event.destination(), result);
        if (target == null) { return result; }
        String location = "*";
        int weight = event.result().impacts().stream().mapToInt(impact -> Math.max(0, impact.weight())).sum();
        float at = (ordinal + .5f) / Math.max(1, count) * weight;
        for (var impact : event.result().impacts()) {
            at -= Math.max(0, impact.weight());
            if (at < 0) { location = impact.location(); break; }
        }
        if (surfaces == null) { surfaces = new UnitPicking(); }
        var key = new HitPoint(target, location, seed);
        var point = hitPoints.computeIfAbsent(key, ignored -> surfaces.surface(target, key.location(), origin,
              seed ^ event.result().id().hashCode()));
        if (point != null) { point.world(target, result); }
        return result;
    }

    private void scatter(ModelInstance targetInstance, Vector3 origin, int ordinal, boolean beam, Vector3 result) {
        var center = center(targetInstance, event.destination(), new Vector3());
        var bounds = targetInstance == null ? null : UnitBounds.world(targetInstance);
        float radius = bounds == null ? BoardGeometry.HEIGHT * .35f : Math.max(bounds.getWidth(), bounds.getHeight()) * .55f;
        var hit = new Vector3();
        int seed = event.result().id().hashCode() ^ ordinal * 7919;
        for (int attempt = 0; attempt < 16; attempt++) {
            float angle = noise(seed + attempt * 31) * MathUtils.PI2;
            float spread = radius + BoardGeometry.HEIGHT * (.15f + noise(seed + 137 + attempt) * 1.6f);
            result.set(center).add(MathUtils.cos(angle) * spread, MathUtils.sin(angle) * spread,
                  beam ? (noise(seed + 991) * 2 - 1) * (bounds == null ? BoardGeometry.LEVEL : bounds.getDepth()) : 0);
            if (shot() && !beam) { ground(result); }
            var ray = new Ray(origin, new Vector3(result).sub(origin).nor());
            if (bounds == null || !Intersector.intersectRayBounds(ray, bounds, hit)
                  || (!beam && hit.dst2(origin) > result.dst2(origin))) { break; }
            radius *= 1.15f;
        }
    }

    /** Straight light does not stop at an arbitrary point in the air. False means no visible impact. */
    boolean beamEndpoint(ModelInstance target, Vector3 origin, int ordinal, Vector3 result) {
        if (defensive()) { endpoint(target, origin, result); return true; }
        if (event.result().hit()) { hitEndpoint(target, origin, ordinal, 0, 1, result); return true; }
        beamAim(target, origin, ordinal, result);
        var ray = new Ray(origin, result.cpy().sub(origin).nor());
        var hit = landscape == null ? null : landscape.apply(ray);
        result.set(origin).mulAdd(ray.direction, hit == null ? BoardGeometry.HEIGHT * 200 : (float) Math.sqrt(hit.distance()));
        return hit != null;
    }

    Vector3 beamAim(ModelInstance target, Vector3 origin, int ordinal, Vector3 result) {
        if (beamMisses.containsKey(ordinal)) { return result.set(beamMisses.get(ordinal)); }
        scatter(target, origin, ordinal, true, result);
        if (landscape != null) { beamMisses.put(ordinal, result.cpy()); }
        return result;
    }

    private void ground(Vector3 point) {
        float fallback = event.destination().elevation() * BoardGeometry.LEVEL;
        var ray = new Ray(new Vector3(point.x, point.y, Math.max(point.z, fallback) + BoardGeometry.LEVEL * 100), new Vector3(0, 0, -1));
        var hit = landscape == null ? null : landscape.apply(ray);
        point.z = hit == null ? fallback : ray.origin.z - (float) Math.sqrt(hit.distance());
    }

    static float noise(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return (value & 0xFFFFFF) / (float) 0x1000000;
    }

    float aimWeight() {
        if (death()) { return 0; }
        if (!shot()) { return seconds < duration ? 1 : 0; }
        return seconds < ANTICIPATION_SECONDS ? smooth(seconds / ANTICIPATION_SECONDS)
              : 1 - smooth((seconds - contactSeconds) / RECOVERY_SECONDS);
    }

    ResolvedAttack.Shot profile(UnitEquipmentAssembly.Binding binding) {
        return profile(binding.memberId() < 0 ? event.entityId() : binding.memberId(), binding.index());
    }

    ResolvedAttack.Shot profile(int owner, int index) {
        if (owner == event.entityId() && index == event.result().equipmentIndex() && event.result().shot() != null) {
            return event.result().shot();
        }
        return profiles.getOrDefault(mountKey(owner, index), event.result().shot());
    }

    /** Preserve the server's total. Division across logical group members is cosmetic, never another cluster roll. */
    int missileHits(int owner, int index, int missiles) {
        if (!event.result().hit()) { return 0; }
        var result = event.result();
        var individual = profiles.get(mountKey(owner, index));
        if (individual != null && individual.missileHits() != null) {
            return MathUtils.clamp(individual.missileHits(), 0, missiles);
        }
        if (result.shot() == null || result.shot().missileHits() == null) { return missiles; }
        int total = 0, before = 0;
        boolean found = false;
        for (var mount : result.mounts()) {
            var shot = mount.shot() == null ? result.shot() : mount.shot();
            int count = shot == null ? 0 : shot.missiles();
            if (mount.entityId() == owner && mount.equipmentIndex() == index) { before = total; found = true; }
            total += count;
        }
        if (!found || total == 0) { return MathUtils.clamp(result.shot().missileHits(), 0, missiles); }
        int hits = MathUtils.clamp(result.shot().missileHits(), 0, total);
        return (int) ((long) hits * (before + missiles) / total - (long) hits * before / total);
    }

    float contactFlash() {
        return seconds >= contactSeconds ? Math.max(0, 1 - (seconds - contactSeconds) / RECOVERY_SECONDS) : 0;
    }

    private static long mountKey(int owner, int index) { return (long) owner << 32 | index & 0xFFFFFFFFL; }

    boolean arcing(UnitEquipmentAssembly.Binding binding) {
        var shot = profile(binding);
        return shot != null && (shot.artillery() || shot.indirect());
    }

    float flight() {
        return MathUtils.clamp((seconds - ANTICIPATION_SECONDS) / (contactSeconds - ANTICIPATION_SECONDS), 0, 1);
    }

    static Vector3 projectile(Vector3 origin, Vector3 target, float progress, boolean arcing, Vector3 result) {
        float t = MathUtils.clamp(progress, 0, 1);
        float arc = arcing ? Math.max(BoardGeometry.HEIGHT * .4f, origin.dst(target) * .25f) : 0;
        return result.set(origin).lerp(target, t).add(0, 0, MathUtils.sin(t * MathUtils.PI) * arc);
    }

    float impact() {
        return !death() && !defensive() && event.result().hit() ? contactFlash() : 0;
    }

    float recoil() {
        float t = (seconds - ANTICIPATION_SECONDS) / .24f;
        return t <= 0 || t >= 1 ? 0 : MathUtils.sin(t * MathUtils.PI);
    }

    /** Approach, stationary strike, recovery, then a faster return share this event's one clock. */
    float approachWeight() {
        return seconds <= contactSeconds + RECOVERY_SECONDS ? smooth(seconds / approachSeconds)
              : 1 - smooth((seconds - contactSeconds - RECOVERY_SECONDS) / MELEE_RUN_SECONDS);
    }

    float travelProgress() {
        return seconds < approachSeconds ? seconds / approachSeconds
              : seconds > contactSeconds + RECOVERY_SECONDS ? (seconds - contactSeconds - RECOVERY_SECONDS) / MELEE_RUN_SECONDS : 0;
    }

    boolean returning() { return seconds > contactSeconds + RECOVERY_SECONDS; }

    float contactWeight() {
        return seconds <= contactSeconds ? smooth((seconds - contactSeconds + .16f) / .16f)
              : 1 - smooth((seconds - contactSeconds) / RECOVERY_SECONDS);
    }

    boolean thrust() {
        String weapon = event.result().equipmentName().toLowerCase(java.util.Locale.ROOT);
        return weapon.contains("lance") || weapon.contains("spear");
    }

    float strike() {
        if (shot() || death()) { return 0; }
        if (seconds < contactSeconds) {
            return smooth((seconds - approachSeconds) / MELEE_SWING_SECONDS);
        }
        return 1 - smooth((seconds - contactSeconds) / RECOVERY_SECONDS);
    }

    float windup() {
        float start = event.result().kind() == ResolvedAttack.Kind.PUNCH || event.result().kind() == ResolvedAttack.Kind.KICK
              ? approachSeconds : approachSeconds * .45f;
        return smooth((seconds - start) / .25f) * (1 - smooth((seconds - contactSeconds) / RECOVERY_SECONDS));
    }

    private static float smooth(float value) {
        float t = MathUtils.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
