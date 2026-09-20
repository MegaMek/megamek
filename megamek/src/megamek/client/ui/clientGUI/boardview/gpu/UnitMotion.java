/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;

/** A render-only timeline; advancing it never writes to Entity or MovePath. */
final class UnitMotion {
    // Animation-clock seconds, independent of hex/model scale. Normal playback advances this clock at half real time.
    static final double WALK_SECONDS_PER_HEX = .2;
    static final double RUN_SECONDS_PER_HEX = WALK_SECONDS_PER_HEX / 1.5;
    static final double SPRINT_SECONDS_PER_HEX = WALK_SECONDS_PER_HEX / 2;
    static final double JUMP_SECONDS_PER_HEX = .175;
    private static final double JUMP_DURATION_SCALE = 1.1;
    static final double MIN_UNIT_SPEED = .65;
    static final double MAX_UNIT_SPEED = 2.5;
    static final double RAMP_HEXES = 3;
    static final double DEFAULT_SPEED_GAIN_PER_HEX = .03;
    private static final double TURN_SPEED_DROP_PER_FACING = .3;
    private static final double MIN_CORNER_SPEED = .5;
    private static final double CORNER_RADIUS_HEXES = .35;
    private static final double REFERENCE_MOVE_MP = 4;
    private static final double REFERENCE_JUMP_MP = 3;
    static final double TURN_SECONDS_PER_FACING = .25;
    static final double POSTURE_SECONDS = .4;
    static final float FORMATION_SETTLE_SECONDS = .3f;
    static final float JUMP_DESTINATION_CLEARANCE = 3;
    private static final float JUMP_ARC_LIFT = 1;
    // Reference visual acceleration at 1g, before jump pacing; horizontal range never scales it.
    private static final double JUMP_GRAVITY = 20;
    private static final double MIN_JUMP_GRAVITY = .25;
    private static final double POWERED_JUMP_FRACTION = .2;
    static final float MAX_JUMP_TILT = 28;
    private static final float DESCENT_FLAME = .45f;
    static final float BOARD_SECONDS = .45f;
    static final float UNLOAD_SECONDS = .6f;
    static final double LANDING_GEAR_SECONDS = .45;
    // Maximum start-time jitter on the animation clock. Zero restores synchronized group movement.
    static final double GROUP_START_JITTER_SECONDS = .16;

    enum Speed {
        HALF("0.5x", .5), NORMAL("1x", 1), DOUBLE("2x", 2), QUADRUPLE("4x", 4), INSTANT("", 0);

        final String label;
        final double rate;

        Speed(String label, double multiplier) {
            this.label = label;
            rate = multiplier * .5;
        }

        Speed next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
    /** Powered ascent opposes gravity; descent follows it. Horizontal travel may add supported airtime at the apex. */
    private record JumpArc(float start, float end, float apex, double ascent, double descent, double duration, Easing horizontal) {
        float progress(double seconds) {
            return horizontal.progress(seconds * horizontal.duration() / duration);
        }

        float elevation(double seconds) {
            if (seconds < ascent) {
                return start + (apex - start) * lift(seconds / ascent);
            }
            if (seconds > duration - descent) {
                return end + (apex - end) * lift((duration - seconds) / descent);
            }
            return apex;
        }

        float descentProgress(double seconds) {
            return (float) Math.clamp((seconds - duration + descent) / descent, 0, 1);
        }

        float tilt(double seconds, double horizontalDistance) {
            // Sample the trajectory, independent of rendering frame rate; exhaust inherits the resulting body pose.
            double delta = .001;
            double horizontalSpeed = horizontalDistance * BoardGeometry.HEIGHT
                  * (progress(seconds + delta) - progress(seconds - delta));
            double verticalSpeed = BoardGeometry.LEVEL * (elevation(seconds + delta) - elevation(seconds - delta));
            float angle = (float) Math.toDegrees(Math.atan2(horizontalSpeed, Math.max(0, verticalSpeed)));
            double launch = Math.clamp(seconds / (ascent * POWERED_JUMP_FRACTION), 0, 1);
            // Finish recovery at the apex, before a fast low-gravity climb flattens its trajectory.
            double recover = Math.clamp((seconds - ascent * .75) / (ascent * .25), 0, 1);
            return (float) (Math.min(MAX_JUMP_TILT, angle) * launch * launch * (3 - 2 * launch)
                  * (1 - recover * recover * (3 - 2 * recover)));
        }

        /** Ease into launch or out of landing, with a parabolic approach to the apex and zero endpoint velocity. */
        private static float lift(double fraction) {
            double t = Math.clamp(fraction, 0, 1);
            if (t < POWERED_JUMP_FRACTION) {
                double u = t / POWERED_JUMP_FRACTION;
                return (float) (2 * POWERED_JUMP_FRACTION * u * u * u * (1 - u / 2));
            }
            return (float) (1 - (1 - t) * (1 - t) / (1 - POWERED_JUMP_FRACTION));
        }

        private static double timeAtHeight(double fraction) {
            double low = 0, high = 1;
            for (int i = 0; i < 24; i++) {
                double mid = (low + high) / 2;
                if (lift(mid) < fraction) { low = mid; } else { high = mid; }
            }
            return (low + high) / 2;
        }
    }
    private record Playback(List<BoardScene.Waypoint> path, double duration, JumpArc jump, EntityMovementType type,
          boolean transport, long sequence, List<Travel> travel, List<GearPause> gear, List<PosturePause> postures,
          double stagger, double settle) {
        double end() { return duration + stagger + settle; }
    }

    /** Delayed children sample the same route and clock; they never own another movement timeline. */
    record Group(Playback playback, double seconds) {
        private double memberTime(int unit, String member) {
            int slot = Integer.parseInt(member.substring("trooper-".length()));
            return seconds - Math.floorMod(slot * 5 + unit * 3 + (int) playback.sequence(), 7) / 6.0 * playback.stagger();
        }

        Sample member(int unit, String member) {
            double time = memberTime(unit, member);
            if (time < 0 || time + 1e-9 >= playback.duration()) {
                return new Sample(false, EntityMovementType.MOVE_NONE, time < 0 ? 0 : 1, 0, 0, 1,
                      arrivalHeading(playback.path()), time < 0 ? Float.POSITIVE_INFINITY : (float) (time - playback.duration()), null);
            }
            return sample(playback, time);
        }

        Vector3 offset(int unit, String member) {
            return position(playback, memberTime(unit, member)).sub(position(playback, seconds));
        }
    }
    /** Cubic velocity ramps integrated to position; short routes lower peak speed at the same acceleration scale. */
    private record Easing(double baseDuration, double rampUp, double rampDown, double from, double peak, double to,
          double travelSeconds, double[] clock) {
        static Easing of(double seconds, double distance) {
            return of(seconds, distance, 0, 0);
        }

        static Easing of(double seconds, double distance, double from, double to) {
            if (distance == 0) {
                return new Easing(seconds * 2, seconds, seconds, 0, 1, 0, seconds, null);
            }
            double peak = Math.max(Math.max(from, to), Math.min(1,
                  Math.sqrt((distance / RAMP_HEXES + from * from + to * to) / 2)));
            double rampUp = 2 * RAMP_HEXES * seconds / distance * (peak - from);
            double rampDown = 2 * RAMP_HEXES * seconds / distance * (peak - to);
            double cruise = Math.max(0, seconds - (from + peak) * rampUp / 2 - (to + peak) * rampDown / 2) / peak;
            return new Easing(rampUp + cruise + rampDown, rampUp, rampDown, from, peak, to, seconds, null);
        }

        double duration() { return clock == null ? baseDuration : clock[clock.length - 1]; }

        double timeAtProgress(double progress) {
            double low = 0, high = duration();
            for (int i = 0; i < 24; i++) {
                double mid = (low + high) / 2;
                if (progress(mid) < progress) { low = mid; } else { high = mid; }
            }
            return (low + high) / 2;
        }

        /** Integrate the distance-dependent gain once; all consumers then sample this immutable clock. */
        Easing withSpeedGain(double offset, double distance, double totalDistance, double gain) {
            if (gain <= 0 || totalDistance <= 2 * RAMP_HEXES || distance == 0) {
                return this;
            }
            int segments = Math.max(128, (int) Math.ceil(distance * 64));
            double[] times = new double[segments + 1];
            double step = baseDuration / segments;
            for (int i = 1; i <= segments; i++) {
                double traveled = offset + distance * baseProgress((i - .5) * step);
                times[i] = times[i - 1] + step / speedGain(traveled, totalDistance, gain);
            }
            return new Easing(baseDuration, rampUp, rampDown, from, peak, to, travelSeconds, times);
        }

        private static double speedGain(double distance, double totalDistance, double gain) {
            double middle = totalDistance - 2 * RAMP_HEXES;
            double traveled = Math.clamp(distance - RAMP_HEXES, 0, middle);
            double blend = Math.min(.5, middle / 2);
            // Ease growth into and out of the middle, without moving either three-hex ramp boundary.
            if (traveled < blend) {
                traveled = blendGrowth(traveled, blend);
            } else if (traveled > middle - blend) {
                traveled = middle - blendGrowth(middle - traveled, blend);
            }
            return 1 + gain * traveled;
        }

        private static double blendGrowth(double distance, double blend) {
            double fraction = distance / blend;
            return distance * fraction * (2 - fraction);
        }

        float progress(double seconds) {
            double time = clock == null ? seconds : baseDuration * distanceProgress(clock, seconds / duration());
            return (float) baseProgress(time);
        }

        private double baseProgress(double seconds) {
            if (baseDuration == 0) {
                return seconds < 0 ? 0 : 1;
            }
            double time = Math.clamp(seconds, 0, baseDuration);
            if (time < rampUp) {
                return (from * time + (peak - from) * rampDistance(time, rampUp)) / travelSeconds;
            }
            if (time > baseDuration - rampDown) {
                double remaining = baseDuration - time;
                return 1 - (to * remaining + (peak - to) * rampDistance(remaining, rampDown)) / travelSeconds;
            }
            return ((from + peak) * rampUp / 2 + peak * (time - rampUp)) / travelSeconds;
        }

        private static double rampDistance(double time, double ramp) {
            double t = time / ramp;
            return ramp * t * t * t * (1 - t / 2);
        }
    }
    private record Travel(double start, double end, Easing easing, double[] distances) {
        double distance() { return distances[distances.length - 1]; }
    }
    private record GearPause(int waypoint, boolean retract, double start, double end) { }
    private record PosturePause(ProneCause from, ProneCause to, megamek.common.units.FallSide side, double start, double end) { }
    record Posture(float crouch, float fallen, megamek.common.units.FallSide side, boolean rising, float progress) {
        static Posture of(ProneCause cause) {
            return of(cause, null);
        }
        static Posture of(ProneCause cause, megamek.common.units.FallSide side) {
            return new Posture(cause == ProneCause.VOLUNTARY ? 1 : 0,
                  cause == ProneCause.FORCED || cause == ProneCause.UNKNOWN ? 1 : 0, side, false, 1);
        }
    }
    record LandingGear(float deployment, BoardScene.Waypoint ground) { }
    enum Stage { BOARD, DRIVE, UNLOAD }
    record JumpJets(long sequence, float flame, float smoke, float seconds, Playback playback) {
        float smoke(double seconds) { return 1 - progress(playback, seconds); }
        float tilt() {
            return playback.jump().tilt(seconds, horizontalDistance(playback.path().getFirst(), playback.path().getLast()));
        }
    }
    record Boarding(long sequence, Stage stage, float progress, int origin, int destination, float arrivalHeading,
          List<BoardScene.Waypoint> path) {
        Vector3 position(float progress) {
            float step = MathUtils.clamp(progress, 0, 1) * (path.size() - 1);
            int index = Math.min((int) step, path.size() - 2);
            return curve(path, index, step - index, false);
        }

        float heading(float progress) {
            float step = MathUtils.clamp(progress, 0, 1) * (path.size() - 1);
            int index = Math.min((int) step, path.size() - 2);
            var tangent = curve(path, index, step - index, true);
            return MathUtils.atan2(tangent.x, tangent.y) * MathUtils.radiansToDegrees;
        }
    }
    /** Derived from this timeline; animation never guesses movement type or posture from the final Entity. */
    record Sample(boolean moving, EntityMovementType type, float progress, float steps, float turn,
          float forward, float heading, float settledSeconds, ProneCause proneCause, Boarding boarding, JumpJets jets,
          BoardScene.AeroState aeroState, LandingGear gear, long sequence, Group group, Posture posture, float lateral) {
        Sample(boolean moving, EntityMovementType type, float progress, float steps, float turn,
              float forward, float heading, float settledSeconds, ProneCause proneCause, Boarding boarding, JumpJets jets,
              BoardScene.AeroState aeroState, LandingGear gear, long sequence, Group group) {
            this(moving, type, progress, steps, turn, forward, heading, settledSeconds, proneCause, boarding, jets, aeroState, gear, sequence, group, null, 0);
        }
        Sample(boolean moving, EntityMovementType type, float progress, float steps, float turn,
              float forward, float heading, float settledSeconds, ProneCause proneCause) {
            this(moving, type, progress, steps, turn, forward, heading, settledSeconds, proneCause, null, null, null, null, 0, null);
        }

        /** Keep the grounded footprint while gear moves, even when the final game snapshot already has one flight hex. */
        BoardScene.Unit placement(BoardScene.Unit unit) {
            if (gear == null || gear.ground().footprint().isEmpty()) {
                return unit;
            }
            return new BoardScene.Unit(unit.id(), unit.part(), unit.name(), gear.ground(), unit.image(), unit.sensorContact(),
                  unit.annotations(), unit.height(), false, unit.model(), unit.outlineRgb(), gear.ground().footprint());
        }

        BoardScene.AeroState aeroState(BoardScene.Unit unit) {
            return moving ? aeroState : unit.location().aeroState();
        }

        boolean airborne(BoardScene.Unit unit) {
            var state = aeroState(unit);
            return state == null ? unit.airborne() : state != BoardScene.AeroState.LANDED;
        }

        Sample member(int unit, String container) {
            return group == null || container == null || !container.startsWith("trooper-") ? this : group.member(unit, container);
        }
        static final Sample STILL = new Sample(false, EntityMovementType.MOVE_NONE, 1, 0, 0, 1, 0,
              Float.POSITIVE_INFINITY, null);
    }
    private final ArrayDeque<Playback> remaining = new ArrayDeque<>();
    private final Vector3 position = new Vector3();
    private float facing;
    private double elapsed;
    private float arrivalHeading;
    private float settledSeconds = Float.POSITIVE_INFINITY;
    private long sequence;
    private Boarding arrival;
    private Playback arrivedGroup;
    private ProneCause arrivedProne;
    private BoardScene.Waypoint observed;

    public UnitMotion(BoardScene.Waypoint initial) {
        snap(initial);
    }

    public void snap(BoardScene.Waypoint location) {
        observed = location;
        remaining.clear();
        position.set(BoardGeometry.center(location.coords(), location.elevation()));
        facing = location.facing() * 60;
        arrivalHeading = facing;
        settledSeconds = Float.POSITIVE_INFINITY;
        arrival = null;
        arrivedGroup = null;
        arrivedProne = null;
        elapsed = 0;
    }

    /** Some takeoff/landing updates have no travel path. Visible transitions still use this same playback clock. */
    void observe(BoardScene.Waypoint location) {
        var from = isMoving() ? remaining.getLast().path().getLast() : observed;
        if (changesGear(from, location)) {
            append(List.of(from, location), EntityMovementType.MOVE_SAFE_THRUST, 0);
        }
        observed = location;
    }

    public void append(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP) {
        append(path, type, jumpMP, false);
    }

    public void append(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP, boolean transport) {
        append(path, type, jumpMP, transport, 0);
    }

    void append(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP, boolean transport, int movementMP) {
        append(path, type, jumpMP, transport, movementMP, 0);
    }

    void append(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP, boolean transport, int movementMP, int members) {
        append(path, type, jumpMP, transport, movementMP, members, DEFAULT_SPEED_GAIN_PER_HEX, 1);
    }

    void append(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP, boolean transport, int movementMP, int members,
          double speedGainPerHex, double gravity) {
        if (path.isEmpty()) {
            return;
        }
        List<BoardScene.Waypoint> points = new ArrayList<>();
        if (isMoving()) {
            points.add(remaining.getLast().path().getLast());
        }
        for (BoardScene.Waypoint point : path) {
            if (points.isEmpty() || !points.getLast().samePose(point)) {
                points.add(point);
            } else if (!point.footprint().isEmpty()) {
                points.set(points.size() - 1, point);
            }
        }
        if (!isMoving()) {
            snap(points.getFirst());
        }
        if (points.size() < 2) {
            return;
        }
        if (type != EntityMovementType.MOVE_JUMP) {
            blendTravelTurns(points);
        }
        transport &= type != EntityMovementType.MOVE_JUMP;
        remaining.add(playback(List.copyOf(points), type, jumpMP, transport, ++sequence, movementMP,
              members > 1 && !transport ? GROUP_START_JITTER_SECONDS : 0,
              members > 0 && !transport ? FORMATION_SETTLE_SECONDS : 0, Math.max(0, speedGainPerHex), gravity));
    }

    /** Only the render copy folds intermediate facing steps into travel; explicit state changes still stop. */
    private static void blendTravelTurns(List<BoardScene.Waypoint> path) {
        for (int first = 1; first < path.size() - 1; first++) {
            int last = first;
            while (last + 1 < path.size() && path.get(first).samePoseExceptFacing(path.get(last + 1))) {
                last++;
            }
            if (last > first && last < path.size() - 1
                  && travelDistance(path.get(first - 1), path.get(first)) > 0
                  && travelDistance(path.get(last), path.get(last + 1)) > 0
                  && Math.abs(UpperBodyTurn.shortestTurn(heading(path.get(last), path.get(last + 1))
                        - heading(path.get(first - 1), path.get(first)))) < 179.99
                  && !changesGear(path.get(first - 1), path.get(first))
                  && !changesPosture(path.get(first - 1), path.get(first))
                  && !changesGear(path.get(last), path.get(last + 1))
                  && !changesPosture(path.get(last), path.get(last + 1))) {
                path.subList(first, last).clear();
            }
        }
    }

    static boolean changesGear(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        return from != null && from.aeroState() != null && to.aeroState() != null
              && (from.aeroState() == BoardScene.AeroState.LANDED) != (to.aeroState() == BoardScene.AeroState.LANDED);
    }

    /** Each leg earns its own travel time. Boarding and gear dwell never make the intervening travel faster. */
    private static Playback playback(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP,
          boolean transport, long sequence, int movementMP, double stagger, double settle, double speedGainPerHex, double gravity) {
        List<PosturePause> postures = new ArrayList<>();
        if (type == EntityMovementType.MOVE_JUMP) {
            JumpArc arc = jumpArc(path, jumpMP, gravity);
            double end = addPosture(postures, path.getFirst(), path.getLast(), arc.duration());
            return new Playback(path, end, arc, type, false, sequence,
                  List.of(), List.of(), List.copyOf(postures), stagger, settle);
        }
        double time = transport ? BOARD_SECONDS : 0;
        List<GearPause> gear = new ArrayList<>();
        List<Travel> travel = new ArrayList<>();
        int blockStart = 0;
        for (int i = 1; i < path.size(); i++) {
            boolean changes = changesGear(path.get(i - 1), path.get(i));
            boolean takeoff = path.get(i - 1).aeroState() == BoardScene.AeroState.LANDED;
            if (changes && takeoff) {
                time += rampTravel(travel, blockStart, path, speedGainPerHex);
                gear.add(new GearPause(i - 1, true, time, time + LANDING_GEAR_SECONDS));
                time += LANDING_GEAR_SECONDS;
                blockStart = travel.size();
            }
            double[] distances = travelDistances(path, i - 1);
            double seconds = travelSeconds(path.get(i - 1), path.get(i), distances[distances.length - 1], type, movementMP);
            boolean turningInPlace = seconds > 0 && travelDistance(path.get(i - 1), path.get(i)) == 0;
            if (turningInPlace) {
                time += rampTravel(travel, blockStart, path, speedGainPerHex);
                blockStart = travel.size();
            }
            double end = time + seconds;
            travel.add(new Travel(time, end, null, distances));
            time = end;
            if (turningInPlace) {
                time += rampTravel(travel, blockStart, path, speedGainPerHex);
                blockStart = travel.size();
            }
            if (changes && !takeoff) {
                time += rampTravel(travel, blockStart, path, speedGainPerHex);
                gear.add(new GearPause(i, false, time, time + LANDING_GEAR_SECONDS));
                time += LANDING_GEAR_SECONDS;
                blockStart = travel.size();
            }
            if (changesPosture(path.get(i - 1), path.get(i))) {
                time += rampTravel(travel, blockStart, path, speedGainPerHex);
                time = addPosture(postures, path.get(i - 1), path.get(i), time);
                blockStart = travel.size();
            }
        }
        time += rampTravel(travel, blockStart, path, speedGainPerHex);
        return new Playback(path, time + (transport ? UNLOAD_SECONDS : 0), null, type, transport, sequence,
              List.copyOf(travel), List.copyOf(gear), List.copyOf(postures), stagger, settle);
    }

    static boolean changesPosture(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        return from != null && from.proneCause() != null && to.proneCause() != null && from.proneCause() != to.proneCause();
    }

    private static double addPosture(List<PosturePause> pauses, BoardScene.Waypoint from, BoardScene.Waypoint to, double time) {
        if (!changesPosture(from, to)) {
            return time;
        }
        pauses.add(new PosturePause(from.proneCause(), to.proneCause(),
              to.fallSide() == null ? from.fallSide() : to.fallSide(), time, time + POSTURE_SECONDS));
        return time + POSTURE_SECONDS;
    }

    /** Anticipate corners, preserve their exit momentum, and build speed through the middle of the continuous route. */
    private static double rampTravel(List<Travel> travel, int first, List<BoardScene.Waypoint> path, double speedGainPerHex) {
        if (first == travel.size()) {
            return 0;
        }
        double start = travel.get(first).start(), duration = travel.getLast().end() - start;
        if (duration == 0) {
            return 0;
        }
        List<Integer> corners = new ArrayList<>();
        corners.add(first);
        for (int i = first + 1; i < travel.size(); i++) {
            if (cornerSpeed(path, i) < 1) {
                corners.add(i);
            }
        }
        corners.add(travel.size());
        double[] distances = new double[corners.size() - 1];
        double[] speeds = new double[corners.size()];
        for (int block = 0; block < distances.length; block++) {
            for (int i = corners.get(block); i < corners.get(block + 1); i++) {
                distances[block] += travel.get(i).distance();
            }
            speeds[block + 1] = block == distances.length - 1 ? 0 : cornerSpeed(path, corners.get(block + 1));
            speeds[block + 1] = Math.min(speeds[block + 1], Math.sqrt(speeds[block] * speeds[block] + distances[block] / RAMP_HEXES));
        }
        // Short stretches may never reach the corner cap; propagate braking requirements back from the destination.
        for (int block = distances.length - 1; block >= 0; block--) {
            speeds[block] = Math.min(speeds[block], Math.sqrt(speeds[block + 1] * speeds[block + 1] + distances[block] / RAMP_HEXES));
        }
        double time = start, offset = 0, totalDistance = Arrays.stream(distances).sum();
        for (int block = 0; block < distances.length; block++) {
            int begin = corners.get(block), end = corners.get(block + 1);
            double oldStart = travel.get(begin).start(), seconds = travel.get(end - 1).end() - oldStart;
            Easing easing = Easing.of(seconds, distances[block], speeds[block], speeds[block + 1])
                  .withSpeedGain(offset, distances[block], totalDistance, speedGainPerHex);
            offset += distances[block];
            double scale = seconds == 0 ? 1 : easing.duration() / seconds;
            for (int i = begin; i < end; i++) {
                Travel leg = travel.get(i);
                travel.set(i, new Travel(time + (leg.start() - oldStart) * scale,
                      time + (leg.end() - oldStart) * scale, easing, leg.distances()));
            }
            time = travel.get(end - 1).end();
        }
        return time - start - duration;
    }

    private static double horizontalDistance(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        return Math.hypot(BoardGeometry.centerX(to.coords()) - BoardGeometry.centerX(from.coords()),
              BoardGeometry.centerY(to.coords()) - BoardGeometry.centerY(from.coords())) / BoardGeometry.HEIGHT;
    }

    private static double travelDistance(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        double vertical = (to.elevation() - from.elevation()) * BoardGeometry.LEVEL / BoardGeometry.HEIGHT;
        return Math.hypot(horizontalDistance(from, to), vertical);
    }

    private static double unitSpeed(int mp, double reference) {
        return mp <= 0 ? 1 : Math.clamp(mp / reference, MIN_UNIT_SPEED, MAX_UNIT_SPEED);
    }

    private static double travelSeconds(BoardScene.Waypoint from, BoardScene.Waypoint to, double distance,
          EntityMovementType type, int movementMP) {
        double pace = switch (type) {
            case MOVE_SPRINT, MOVE_VTOL_SPRINT, MOVE_SKID -> SPRINT_SECONDS_PER_HEX;
            case MOVE_RUN, MOVE_VTOL_RUN, MOVE_SUBMARINE_RUN, MOVE_OVER_THRUST -> RUN_SECONDS_PER_HEX;
            default -> WALK_SECONDS_PER_HEX;
        };
        double unitSpeed = unitSpeed(movementMP, REFERENCE_MOVE_MP);
        if (movementMP > 0) {
            // The game already includes walking/running/sprinting and damage/heat modifiers in this MP snapshot.
            pace = WALK_SECONDS_PER_HEX / unitSpeed;
        }
        double turn = Math.abs(UpperBodyTurn.shortestTurn((to.facing() - from.facing()) * 60))
              / 60 * TURN_SECONDS_PER_FACING / unitSpeed;
        // Moving turns are timed by their speed cap. Only a standalone pivot needs a stationary interval.
        return distance > 0 ? distance * pace : turn;
    }

    private static JumpArc jumpArc(List<BoardScene.Waypoint> points, int jumpMP, double gravity) {
        BoardScene.Waypoint start = points.getFirst();
        BoardScene.Waypoint end = points.getLast();
        float ceiling = start.elevation() + Math.max(0, jumpMP);
        float apex = Math.max(Math.max(start.elevation(), end.elevation()),
              Math.min(ceiling, end.elevation() + JUMP_DESTINATION_CLEARANCE));
        for (BoardScene.Waypoint point : points) {
            apex = Math.max(apex, Math.min(ceiling, point.elevation()));
        }
        // At zero/very low gravity, the packs provide a bounded assisted arc and a positive landing acceleration.
        double effectiveGravity = Math.max(MIN_JUMP_GRAVITY, Double.isFinite(gravity) ? gravity : 1);
        float clearance = points.stream().map(BoardScene.Waypoint::elevation).max(Float::compare).orElse(0f) + JUMP_ARC_LIFT;
        apex = (float) Math.max(clearance, start.elevation() + (apex + JUMP_ARC_LIFT - start.elevation()) / effectiveGravity);
        // Gravity resists the powered climb. Squared resistance lets low-g jets reach even the taller arc sooner.
        double climbAcceleration = JUMP_GRAVITY / (effectiveGravity * effectiveGravity);
        double ascent = Math.sqrt(2 * (apex - start.elevation()) / (climbAcceleration * (1 - POWERED_JUMP_FRACTION)));
        double descent = Math.sqrt(2 * (apex - end.elevation()) / (JUMP_GRAVITY * effectiveGravity * (1 - POWERED_JUMP_FRACTION)));
        double distance = horizontalDistance(start, end);
        Easing horizontal = Easing.of(Math.max(.001, distance * JUMP_SECONDS_PER_HEX / unitSpeed(jumpMP, REFERENCE_JUMP_MP)), distance);
        double duration = Math.max(horizontal.duration(), ascent + descent);
        double dx = BoardGeometry.centerX(end.coords()) - BoardGeometry.centerX(start.coords());
        double dy = BoardGeometry.centerY(end.coords()) - BoardGeometry.centerY(start.coords());
        // Delay crossing raised intermediate waypoints until they are cleared, without slowing vertical flight.
        for (int index = 1; index < points.size() - 1; index++) {
            double progress = distance == 0 ? 0
                  : ((BoardGeometry.centerX(points.get(index).coords()) - BoardGeometry.centerX(start.coords())) * dx
                        + (BoardGeometry.centerY(points.get(index).coords()) - BoardGeometry.centerY(start.coords())) * dy) / (dx * dx + dy * dy);
            if (progress <= 0 || progress >= 1) { continue; }
            double time = horizontal.timeAtProgress(progress) / horizontal.duration();
            float elevation = points.get(index).elevation();
            if (elevation > start.elevation()) {
                duration = Math.max(duration, ascent * JumpArc.timeAtHeight((elevation - start.elevation()) / (apex - start.elevation())) / time);
            }
            if (elevation > end.elevation()) {
                duration = Math.max(duration, descent * JumpArc.timeAtHeight((elevation - end.elevation()) / (apex - end.elevation())) / (1 - time));
            }
        }
        return new JumpArc(start.elevation(), end.elevation(), apex,
              ascent * JUMP_DURATION_SCALE, descent * JUMP_DURATION_SCALE, duration * JUMP_DURATION_SCALE, horizontal);
    }

    public void advance(double seconds, double speed) {
        if (!isMoving()) {
            settledSeconds = speed <= 0 ? Float.POSITIVE_INFINITY : settledSeconds + (float) (Math.max(0, seconds) * speed);
            return;
        }
        if (speed <= 0) {
            finish();
            return;
        }
        elapsed += Math.max(0, seconds) * speed;
        while (isMoving() && elapsed + 1e-9 >= remaining.getFirst().end()) {
            Playback completed = remaining.removeFirst();
            BoardScene.Waypoint end = completed.path().getLast();
            position.set(BoardGeometry.center(end.coords(), end.elevation()));
            facing = end.facing() * 60;
            arrivalHeading = arrivalHeading(completed.path());
            arrival = boarding(completed, completed.duration());
            arrivedGroup = completed.stagger() > 0 ? completed : null;
            arrivedProne = end.proneCause();
            settledSeconds = (float) completed.settle();
            elapsed = Math.max(0, elapsed - completed.end());
        }
        if (!isMoving()) {
            settledSeconds += (float) elapsed;
            elapsed = 0;
            return;
        }
        Playback playback = remaining.getFirst();
        position.set(position(playback, elapsed));
        facing = facing(playback, elapsed);
    }

    private static float facing(Playback playback, double seconds) {
        float progress = progress(playback, seconds);
        if (playback.jump() != null) {
            return MathUtils.lerpAngleDeg(playback.path().getFirst().facing() * 60,
                  playback.path().getLast().facing() * 60, progress);
        }
        float step = progress * (playback.path().size() - 1);
        int index = Math.min((int) step, playback.path().size() - 2);
        var from = playback.path().get(index);
        var to = playback.path().get(index + 1);
        var tangent = curve(playback.path(), index, step - index, true);
        if (tangent.x == 0 && tangent.y == 0) {
            return MathUtils.lerpAngleDeg(from.facing() * 60, to.facing() * 60, step - index);
        }
        float direction = MathUtils.atan2(tangent.x, tangent.y) * MathUtils.radiansToDegrees;
        float entry = heading(from, to);
        float exit = cornerRadius(playback.path(), index + 1) > 0
              ? heading(to, playback.path().get(index + 2)) : entry;
        // Heading offsets preserve reverse/lateral movement while the body follows the shared curve tangent.
        return (direction + MathUtils.lerpAngleDeg(from.facing() * 60 - entry,
              to.facing() * 60 - exit, step - index) + 360) % 360;
    }

    private static Vector3 position(Playback playback, double seconds) {
        float progress = progress(playback, seconds);
        if (playback.jump() != null) {
            var start = playback.path().getFirst();
            var end = playback.path().getLast();
            var position = BoardGeometry.center(start.coords(), start.elevation())
                  .lerp(BoardGeometry.center(end.coords(), end.elevation()), progress);
            position.z = playback.jump().elevation(seconds) * BoardGeometry.LEVEL;
            return position;
        }
        float step = progress * (playback.path().size() - 1);
        int index = Math.min((int) step, playback.path().size() - 2);
        return curve(playback.path(), index, step - index, false);
    }

    public void finish() {
        if (isMoving()) {
            var last = remaining.getLast();
            float heading = arrivalHeading(last.path());
            snap(last.path().getLast());
            arrivalHeading = heading;
            arrival = boarding(last, last.duration());
        }
        settledSeconds = Float.POSITIVE_INFINITY;
        arrivedGroup = null;
    }

    private static float arrivalHeading(List<BoardScene.Waypoint> path) {
        var end = path.getLast();
        for (int index = path.size() - 2; index >= 0; index--) {
            var from = path.get(index);
            if (!from.coords().equals(end.coords())) {
                return heading(from, end);
            }
        }
        return end.facing() * 60;
    }

    public boolean isMoving() {
        return !remaining.isEmpty();
    }

    double remainingSeconds() {
        return remaining.stream().mapToDouble(Playback::end).sum() - elapsed;
    }

    public Vector3 position() {
        return position;
    }

    /** Grounded road travel follows the rendered cut/ramp without changing the game path. */
    public Vector3 surfacePosition(BoardScene scene) {
        if (!isMoving() || remaining.getFirst().jump() != null) {
            return position;
        }
        Playback playback = remaining.getFirst();
        int index = Math.min((int) (progress(playback, elapsed) * (playback.path().size() - 1)), playback.path().size() - 2);
        BoardScene.Waypoint from = playback.path().get(index), to = playback.path().get(index + 1);
        BoardScene.Tile start = scene.tile(from.coords()), end = scene.tile(to.coords());
        if (start == null || end == null || start.water() || end.water()
              || from.elevation() != start.elevation() || to.elevation() != end.elevation()) {
            return position;
        }
        for (int direction = 0; direction < 6; direction++) {
            if (from.coords().translated(direction).equals(to.coords())
                  && BoardSurface.hasRoadApproach(start, end, direction)) {
                BoardScene.Tile tile = BoardGeometry.contains(from.coords(), position.x, position.y) ? start : end;
                return new Vector3(position.x, position.y, new BoardSurface(scene, tile).height(position.x, position.y));
            }
        }
        return position;
    }

    public Vector3 destination() {
        BoardScene.Waypoint last = isMoving() ? remaining.getLast().path().getLast() : null;
        return last == null ? new Vector3(position) : BoardGeometry.center(last.coords(), last.elevation());
    }

    public float facing() {
        return facing;
    }

    Sample sample() {
        if (!isMoving()) {
            return new Sample(false, EntityMovementType.MOVE_NONE, 1, 0, 0, 1, arrivalHeading, settledSeconds, arrivedProne, arrival, null, null, null, sequence,
                  arrivedGroup == null ? null : new Group(arrivedGroup, arrivedGroup.duration() + arrivedGroup.stagger() + settledSeconds));
        }
        Playback playback = remaining.getFirst();
        Sample pose = sample(playback, elapsed);
        return new Sample(pose.moving(), pose.type(), pose.progress(), pose.steps(), pose.turn(), pose.forward(), pose.heading(),
              pose.settledSeconds(), pose.proneCause(), pose.boarding(), pose.jets(), pose.aeroState(), pose.gear(), pose.sequence(),
              playback.stagger() == 0 ? null : new Group(playback, elapsed), pose.posture(), pose.lateral());
    }

    private static Sample sample(Playback playback, double seconds) {
        float progress = progress(playback, seconds);
        float facing = facing(playback, seconds);
        float step = progress * (playback.path().size() - 1);
        int index = Math.min((int) step, playback.path().size() - 2);
        var from = playback.path().get(index);
        var to = playback.path().get(index + 1);
        float distance = playback.jump() == null ? 0
              : (float) (horizontalDistance(playback.path().getFirst(), playback.path().getLast()) * progress);
        float dx, dy;
        if (playback.jump() == null) {
            for (int i = 1; i <= index + 1; i++) {
                distance += distanceAt(playback.travel().get(i - 1).distances(), i == index + 1 ? step - index : 1);
            }
            var tangent = curve(playback.path(), index, step - index, true);
            dx = tangent.x;
            dy = tangent.y;
        } else {
            dx = BoardGeometry.centerX(playback.path().getLast().coords()) - BoardGeometry.centerX(playback.path().getFirst().coords());
            dy = BoardGeometry.centerY(playback.path().getLast().coords()) - BoardGeometry.centerY(playback.path().getFirst().coords());
        }
        float direction = dx == 0 && dy == 0 ? facing : MathUtils.atan2(dx, dy) * MathUtils.radiansToDegrees;
        ProneCause posture = playback.path().getFirst().proneCause();
        for (PosturePause pause : playback.postures()) {
            if (seconds < pause.end()) {
                break;
            }
            posture = pause.to();
        }
        JumpJets jets = null;
        if (playback.jump() != null && seconds < playback.jump().duration()) {
            float descent = playback.jump().descentProgress(seconds);
            float flame = MathUtils.lerp(DESCENT_FLAME, 1, 1 - descent * descent * (3 - 2 * descent));
            jets = new JumpJets(playback.sequence(), flame, 1 - progress, (float) seconds, playback);
        }
        LandingGear gear = landingGear(playback, seconds);
        BoardScene.AeroState state = aeroState(from, to);
        float turn = UpperBodyTurn.shortestTurn((to.facing() - from.facing()) * 60);
        if (playback.jump() == null && (state == null || state == BoardScene.AeroState.LANDED) && travelDistance(from, to) > 0) {
            // The curved route already drives the walking gait; adding pivot steps would jump its phase at corners.
            turn = 0;
        }
        return new Sample(seconds < playback.duration(), playback.type(), progress, distance,
              turn,
              MathUtils.cosDeg(direction - facing), direction, (float) Math.max(0, seconds - playback.duration()), posture, boarding(playback, seconds), jets,
              gear == null ? state : BoardScene.AeroState.LANDED, gear, playback.sequence(), null, posture(playback, seconds),
              MathUtils.sinDeg(direction - facing));
    }

    private static Posture posture(Playback playback, double seconds) {
        ProneCause cause = playback.path().getFirst().proneCause();
        var side = playback.path().getFirst().fallSide();
        for (PosturePause pause : playback.postures()) {
            if (seconds < pause.start()) {
                break;
            }
            if (seconds < pause.end()) {
                float t = (float) ((seconds - pause.start()) / POSTURE_SECONDS);
                t = t * t * (3 - 2 * t);
                var from = Posture.of(pause.from());
                var to = Posture.of(pause.to());
                return new Posture(MathUtils.lerp(from.crouch(), to.crouch(), t), MathUtils.lerp(from.fallen(), to.fallen(), t),
                      pause.side(), from.fallen() > to.fallen(), t);
            }
            cause = pause.to();
            side = pause.side();
        }
        return cause == null ? null : Posture.of(cause, side);
    }

    private static LandingGear landingGear(Playback playback, double seconds) {
        for (GearPause pause : playback.gear()) {
            if (seconds >= pause.start() && seconds < pause.end()) {
                float progress = (float) ((seconds - pause.start()) / (pause.end() - pause.start()));
                float smooth = progress * progress * (3 - 2 * progress);
                return new LandingGear(pause.retract() ? 1 - smooth : smooth, playback.path().get(pause.waypoint()));
            }
        }
        return null;
    }

    /** Keep supports hidden throughout a flight/landing segment, including its last approach to the ground. */
    private static BoardScene.AeroState aeroState(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        if (from.aeroState() == null || to.aeroState() == null) {
            return null;
        }
        if (from.aeroState() == BoardScene.AeroState.AIRBORNE || to.aeroState() == BoardScene.AeroState.AIRBORNE) {
            return BoardScene.AeroState.AIRBORNE;
        }
        return from.aeroState() == BoardScene.AeroState.LANDED && to.aeroState() == BoardScene.AeroState.LANDED
              ? BoardScene.AeroState.LANDED : BoardScene.AeroState.ELEVATED;
    }

    private static float progress(Playback playback, double seconds) {
        if (playback.jump() != null) {
            return playback.jump().progress(seconds);
        }
        // Ease continuous travel once; boarding, gear, posture and stationary turns delimit blocks.
        for (int first = 0; first < playback.travel().size();) {
            int last = first;
            while (last + 1 < playback.travel().size()
                  && playback.travel().get(last + 1).start() == playback.travel().get(last).end()
                  && playback.travel().get(last + 1).easing() == playback.travel().get(first).easing()) {
                last++;
            }
            double start = playback.travel().get(first).start(), end = playback.travel().get(last).end();
            if (seconds <= end) {
                Easing easing = playback.travel().get(first).easing();
                seconds = start + (easing == null ? 1 : easing.progress(seconds - start)) * (end - start);
                break;
            }
            first = last + 1;
        }
        for (int i = 0; i < playback.travel().size(); i++) {
            Travel leg = playback.travel().get(i);
            if (seconds <= leg.end()) {
                double fraction = leg.end() == leg.start() ? 1 : Math.clamp((seconds - leg.start()) / (leg.end() - leg.start()), 0, 1);
                return (i + distanceProgress(leg.distances(), fraction)) / playback.travel().size();
            }
        }
        return 1;
    }

    private static Boarding boarding(Playback playback, double seconds) {
        if (!playback.transport()) {
            return null;
        }
        Stage stage = seconds < BOARD_SECONDS ? Stage.BOARD
              : seconds < playback.duration() - UNLOAD_SECONDS ? Stage.DRIVE : Stage.UNLOAD;
        float fraction = switch (stage) {
            case BOARD -> (float) (seconds / BOARD_SECONDS);
            case DRIVE -> progress(playback, seconds);
            case UNLOAD -> (float) ((seconds - playback.duration() + UNLOAD_SECONDS) / UNLOAD_SECONDS);
        };
        return new Boarding(playback.sequence(), stage, MathUtils.clamp(fraction, 0, 1),
              playback.path().getFirst().coords().hashCode(), playback.path().getLast().coords().hashCode(),
              arrivalHeading(playback.path()), playback.path());
    }

    private static float heading(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        return MathUtils.atan2(BoardGeometry.centerX(to.coords()) - BoardGeometry.centerX(from.coords()),
              BoardGeometry.centerY(to.coords()) - BoardGeometry.centerY(from.coords())) * MathUtils.radiansToDegrees;
    }

    private static double turnAngle(List<BoardScene.Waypoint> path, int index) {
        if (index <= 0 || index >= path.size() - 1) {
            return 0;
        }
        var from = path.get(index - 1);
        var at = path.get(index);
        var to = path.get(index + 1);
        if (horizontalDistance(from, at) == 0 || horizontalDistance(at, to) == 0
              || changesGear(from, at) || changesGear(at, to) || changesPosture(from, at) || changesPosture(at, to)) {
            return 0;
        }
        return Math.abs(UpperBodyTurn.shortestTurn(heading(at, to) - heading(from, at)));
    }

    private static double cornerSpeed(List<BoardScene.Waypoint> path, int index) {
        double angle = turnAngle(path, index);
        if (angle < .01) {
            return 1;
        }
        // Reversing along the same line needs a stop; ordinary corners retain forward travel.
        return angle > 179.99 ? 0 : Math.max(MIN_CORNER_SPEED, 1 - angle / 60 * TURN_SPEED_DROP_PER_FACING);
    }

    private static float cornerRadius(List<BoardScene.Waypoint> path, int index) {
        double angle = turnAngle(path, index);
        if (angle < .01 || angle > 179.99) {
            return 0;
        }
        return (float) (BoardGeometry.HEIGHT * Math.min(CORNER_RADIUS_HEXES,
              Math.min(horizontalDistance(path.get(index - 1), path.get(index)),
                    horizontalDistance(path.get(index), path.get(index + 1))) / 2));
    }

    /** Tangent quadratic corners stay inside the turning hex; every ground view and transport uses this route. */
    private static Vector3 curve(List<BoardScene.Waypoint> path, int index, float t, boolean tangent) {
        var b = point(path, index);
        var c = point(path, index + 1);
        float length = (float) Math.hypot(c.x - b.x, c.y - b.y);
        float entry = cornerRadius(path, index), exit = cornerRadius(path, index + 1);
        Vector3 result;
        if (entry > 0 && t * length < entry) {
            result = corner(path, index, .5f + t * length / (2 * entry), tangent);
            if (tangent) { result.scl(length / (2 * entry)); }
        } else if (exit > 0 && (1 - t) * length < exit) {
            result = corner(path, index + 1, .5f - (1 - t) * length / (2 * exit), tangent);
            if (tangent) { result.scl(length / (2 * exit)); }
        } else {
            result = tangent ? c.cpy().sub(b) : b.cpy().lerp(c, t);
        }
        result.z = tangent ? c.z - b.z : MathUtils.lerp(b.z, c.z, t);
        return result;
    }

    private static Vector3 corner(List<BoardScene.Waypoint> path, int index, float t, boolean tangent) {
        var center = point(path, index);
        var incoming = center.cpy().sub(point(path, index - 1));
        var outgoing = point(path, index + 1).sub(center);
        incoming.z = 0;
        outgoing.z = 0;
        float radius = cornerRadius(path, index);
        incoming.nor().scl(radius);
        outgoing.nor().scl(radius);
        if (tangent) {
            return incoming.scl(2 * (1 - t)).mulAdd(outgoing, 2 * t);
        }
        return center.mulAdd(incoming, -(1 - t) * (1 - t)).mulAdd(outgoing, t * t);
    }

    /** Arc length tables are immutable after construction, and drive both placement and gait distance. */
    private static double[] travelDistances(List<BoardScene.Waypoint> path, int index) {
        double distance = travelDistance(path.get(index), path.get(index + 1));
        if (cornerRadius(path, index) == 0 && cornerRadius(path, index + 1) == 0) {
            return new double[] { 0, distance };
        }
        int segments = Math.max(16, (int) Math.ceil(distance * 64));
        double[] distances = new double[segments + 1];
        var previous = curve(path, index, 0, false);
        for (int i = 1; i <= segments; i++) {
            var position = curve(path, index, i / (float) segments, false);
            distances[i] = distances[i - 1] + previous.dst(position) / BoardGeometry.HEIGHT;
            previous = position;
        }
        return distances;
    }

    private static float distanceProgress(double[] distances, double fraction) {
        double length = distances[distances.length - 1];
        if (length == 0) {
            return (float) Math.clamp(fraction, 0, 1);
        }
        double target = Math.clamp(fraction, 0, 1) * length;
        int index = Arrays.binarySearch(distances, target);
        if (index >= 0) {
            return index / (float) (distances.length - 1);
        }
        int next = -index - 1;
        double progress = (target - distances[next - 1]) / (distances[next] - distances[next - 1]);
        return (float) ((next - 1 + progress) / (distances.length - 1));
    }

    private static float distanceAt(double[] distances, float progress) {
        float step = MathUtils.clamp(progress, 0, 1) * (distances.length - 1);
        int index = Math.min((int) step, distances.length - 2);
        return (float) (distances[index] + (distances[index + 1] - distances[index]) * (step - index));
    }

    private static Vector3 point(List<BoardScene.Waypoint> path, int index) {
        var point = path.get(index);
        return BoardGeometry.center(point.coords(), point.elevation());
    }
}
