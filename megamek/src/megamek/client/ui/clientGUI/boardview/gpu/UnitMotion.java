/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import megamek.common.units.EntityMovementType;
import megamek.common.units.ProneCause;

/** A render-only timeline; advancing it never writes to Entity or MovePath. */
final class UnitMotion {
    // Animation-clock seconds, independent of hex/model scale. Normal playback advances this clock at half real time.
    static final double WALK_SECONDS_PER_HEX = .3;
    static final double RUN_SECONDS_PER_HEX = WALK_SECONDS_PER_HEX / 1.5;
    static final double SPRINT_SECONDS_PER_HEX = WALK_SECONDS_PER_HEX / 2;
    static final double JUMP_SECONDS_PER_HEX = .4;
    static final double MIN_UNIT_SPEED = .65;
    static final double MAX_UNIT_SPEED = 2.5;
    static final double RAMP_SECONDS = .2;
    private static final double REFERENCE_MOVE_MP = 4;
    private static final double REFERENCE_JUMP_MP = 3;
    static final double TURN_SECONDS_PER_FACING = .25;
    static final double POSTURE_SECONDS = .4;
    static final float FORMATION_SETTLE_SECONDS = .3f;
    static final float JUMP_DESTINATION_CLEARANCE = 3;
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
    private record JumpArc(float start, float end, float apex, float ascentEnd, float descentStart) {
        float elevation(float progress) {
            if (progress < ascentEnd) {
                float remaining = 1 - progress / ascentEnd;
                return apex - (apex - start) * remaining * remaining;
            }
            if (progress > descentStart) {
                float remaining = (progress - descentStart) / (1 - descentStart);
                return apex - (apex - end) * remaining * remaining;
            }
            return apex;
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
    private record Travel(double start, double end) { }
    private record GearPause(int waypoint, boolean retract, double start, double end) { }
    private record PosturePause(ProneCause from, ProneCause to, megamek.common.units.FallSide side, double start, double end) { }
    record Posture(float crouch, float fallen, megamek.common.units.FallSide side, boolean rising, float progress) {
        Posture(float crouch, float fallen) { this(crouch, fallen, null, false, fallen); }
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
    record JumpJets(long sequence, float flame, float smoke, float seconds, float duration) { }
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
              BoardScene.AeroState aeroState, LandingGear gear, long sequence, Group group, Posture posture) {
            this(moving, type, progress, steps, turn, forward, heading, settledSeconds, proneCause, boarding, jets,
                  aeroState, gear, sequence, group, posture, 0);
        }
        Sample(boolean moving, EntityMovementType type, float progress, float steps, float turn,
              float forward, float heading, float settledSeconds, ProneCause proneCause, Boarding boarding, JumpJets jets,
              BoardScene.AeroState aeroState, LandingGear gear, long sequence, Group group) {
            this(moving, type, progress, steps, turn, forward, heading, settledSeconds, proneCause, boarding, jets, aeroState, gear, sequence, group, null);
        }
        Sample(boolean moving, EntityMovementType type, float progress, float steps, float turn,
              float forward, float heading, float settledSeconds, ProneCause proneCause, Boarding boarding, JumpJets jets,
              BoardScene.AeroState aeroState, LandingGear gear, long sequence) {
            this(moving, type, progress, steps, turn, forward, heading, settledSeconds, proneCause, boarding, jets, aeroState, gear, sequence, null);
        }
        Sample(boolean moving, EntityMovementType type, float progress, float steps, float turn,
              float forward, float heading, float settledSeconds, ProneCause proneCause, Boarding boarding, JumpJets jets,
              BoardScene.AeroState aeroState, LandingGear gear) {
            this(moving, type, progress, steps, turn, forward, heading, settledSeconds, proneCause, boarding, jets, aeroState, gear, 0);
        }
        Sample(boolean moving, EntityMovementType type, float progress, float steps, float turn,
              float forward, float heading, float settledSeconds, ProneCause proneCause) {
            this(moving, type, progress, steps, turn, forward, heading, settledSeconds, proneCause, null, null, null, null);
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
        transport &= type != EntityMovementType.MOVE_JUMP;
        remaining.add(playback(List.copyOf(points), type, jumpMP, transport, ++sequence, movementMP,
              members > 1 && !transport ? GROUP_START_JITTER_SECONDS : 0,
              members > 0 && !transport ? FORMATION_SETTLE_SECONDS : 0));
    }

    static boolean changesGear(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        return from != null && from.aeroState() != null && to.aeroState() != null
              && (from.aeroState() == BoardScene.AeroState.LANDED) != (to.aeroState() == BoardScene.AeroState.LANDED);
    }

    /** Each leg earns its own travel time. Boarding and gear dwell never make the intervening travel faster. */
    private static Playback playback(List<BoardScene.Waypoint> path, EntityMovementType type, int jumpMP,
          boolean transport, long sequence, int movementMP, double stagger, double settle) {
        List<PosturePause> postures = new ArrayList<>();
        if (type == EntityMovementType.MOVE_JUMP) {
            JumpArc arc = jumpArc(path, jumpMP);
            double vertical = (Math.sqrt(arc.apex() - arc.start()) + Math.sqrt(arc.apex() - arc.end())) * .35;
            double duration = Math.max(.6, Math.max(vertical,
                  horizontalDistance(path.getFirst(), path.getLast()) * JUMP_SECONDS_PER_HEX
                        / unitSpeed(jumpMP, REFERENCE_JUMP_MP)));
            duration += Math.min(RAMP_SECONDS, duration);
            double end = addPosture(postures, path.getFirst(), path.getLast(), duration);
            return new Playback(path, end, arc, type, false, sequence,
                  List.of(new Travel(0, duration)), List.of(), List.copyOf(postures), stagger, settle);
        }
        double time = transport ? BOARD_SECONDS : 0;
        List<GearPause> gear = new ArrayList<>();
        List<Travel> travel = new ArrayList<>();
        int blockStart = 0;
        for (int i = 1; i < path.size(); i++) {
            boolean changes = changesGear(path.get(i - 1), path.get(i));
            boolean takeoff = path.get(i - 1).aeroState() == BoardScene.AeroState.LANDED;
            if (changes && takeoff) {
                time += rampTravel(travel, blockStart);
                gear.add(new GearPause(i - 1, true, time, time + LANDING_GEAR_SECONDS));
                time += LANDING_GEAR_SECONDS;
                blockStart = travel.size();
            }
            double end = time + travelSeconds(path.get(i - 1), path.get(i), type, movementMP);
            travel.add(new Travel(time, end));
            time = end;
            if (changes && !takeoff) {
                time += rampTravel(travel, blockStart);
                gear.add(new GearPause(i, false, time, time + LANDING_GEAR_SECONDS));
                time += LANDING_GEAR_SECONDS;
                blockStart = travel.size();
            }
            if (changesPosture(path.get(i - 1), path.get(i))) {
                time += rampTravel(travel, blockStart);
                time = addPosture(postures, path.get(i - 1), path.get(i), time);
                blockStart = travel.size();
            }
        }
        time += rampTravel(travel, blockStart);
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

    /** Reserve acceleration/braking time without raising the cruise speed or stopping at every waypoint. */
    private static double rampTravel(List<Travel> travel, int first) {
        if (first == travel.size()) {
            return 0;
        }
        double start = travel.get(first).start(), duration = travel.getLast().end() - start;
        if (duration == 0) {
            return 0;
        }
        double ramp = Math.min(RAMP_SECONDS, duration);
        double scale = (duration + ramp) / duration;
        for (int i = first; i < travel.size(); i++) {
            Travel leg = travel.get(i);
            travel.set(i, new Travel(start + (leg.start() - start) * scale, start + (leg.end() - start) * scale));
        }
        return ramp;
    }

    private static double horizontalDistance(BoardScene.Waypoint from, BoardScene.Waypoint to) {
        return Math.hypot(BoardGeometry.centerX(to.coords()) - BoardGeometry.centerX(from.coords()),
              BoardGeometry.centerY(to.coords()) - BoardGeometry.centerY(from.coords())) / BoardGeometry.HEIGHT;
    }

    private static double unitSpeed(int mp, double reference) {
        return mp <= 0 ? 1 : Math.clamp(mp / reference, MIN_UNIT_SPEED, MAX_UNIT_SPEED);
    }

    private static double travelSeconds(BoardScene.Waypoint from, BoardScene.Waypoint to, EntityMovementType type, int movementMP) {
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
        double vertical = (to.elevation() - from.elevation()) * BoardGeometry.LEVEL / BoardGeometry.HEIGHT;
        double translation = Math.hypot(horizontalDistance(from, to), vertical) * pace;
        double turn = Math.abs(UpperBodyTurn.shortestTurn((to.facing() - from.facing()) * 60))
              / 60 * TURN_SECONDS_PER_FACING / unitSpeed;
        // Posture changes have their own interval after arrival; they cannot finish while the unit is still travelling.
        return Math.max(translation, turn);
    }

    private static JumpArc jumpArc(List<BoardScene.Waypoint> points, int jumpMP) {
        BoardScene.Waypoint start = points.getFirst();
        BoardScene.Waypoint end = points.getLast();
        float ceiling = start.elevation() + Math.max(0, jumpMP);
        float apex = Math.max(Math.max(start.elevation(), end.elevation()),
              Math.min(ceiling, end.elevation() + JUMP_DESTINATION_CLEARANCE));
        for (BoardScene.Waypoint point : points) {
            apex = Math.max(apex, Math.min(ceiling, point.elevation()));
        }
        float rise = (float) Math.sqrt(apex - start.elevation());
        float fall = (float) Math.sqrt(apex - end.elevation());
        float ascentEnd = rise + fall == 0 ? 0 : rise / (rise + fall);
        float descentStart = ascentEnd;
        for (int index = 1; index < points.size() - 1; index++) {
            float progress = index / (float) (points.size() - 1);
            float elevation = Math.min(apex, points.get(index).elevation());
            if (elevation > start.elevation()) {
                float fraction = 1 - (float) Math.sqrt((apex - elevation) / (apex - start.elevation()));
                ascentEnd = Math.min(ascentEnd, progress / fraction);
            }
            if (elevation > end.elevation()) {
                float fraction = (float) Math.sqrt((apex - elevation) / (apex - end.elevation()));
                descentStart = Math.max(descentStart, (progress - fraction) / (1 - fraction));
            }
        }
        return new JumpArc(start.elevation(), end.elevation(), apex, ascentEnd, descentStart);
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
        float step = progress(playback, seconds) * (playback.path().size() - 1);
        int index = Math.min((int) step, playback.path().size() - 2);
        if (playback.jump() != null) {
            return MathUtils.lerpAngleDeg(playback.path().getFirst().facing() * 60,
                  playback.path().getLast().facing() * 60, progress(playback, seconds));
        }
        return MathUtils.lerpAngleDeg(playback.path().get(index).facing() * 60,
              playback.path().get(index + 1).facing() * 60, step - index);
    }

    private static Vector3 position(Playback playback, double seconds) {
        Vector3 position = new Vector3();
        float progress = progress(playback, seconds);
        float step = progress * (playback.path().size() - 1);
        int index = Math.min((int) step, playback.path().size() - 2);
        BoardScene.Waypoint start = playback.jump() != null ? playback.path().getFirst() : playback.path().get(index);
        BoardScene.Waypoint end = playback.jump() != null ? playback.path().getLast() : playback.path().get(index + 1);
        float fraction = playback.jump() != null ? progress : step - index;
        position.set(BoardGeometry.center(start.coords(), start.elevation()))
              .lerp(BoardGeometry.center(end.coords(), end.elevation()), fraction);
        if (playback.transport()) {
            position.set(curve(playback.path(), index, fraction, false));
        }
        if (playback.jump() != null) {
            position.z = playback.jump().elevation(progress) * BoardGeometry.LEVEL;
        }
        return position;
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
                return MathUtils.atan2(BoardGeometry.centerX(end.coords()) - BoardGeometry.centerX(from.coords()),
                      BoardGeometry.centerY(end.coords()) - BoardGeometry.centerY(from.coords())) * MathUtils.radiansToDegrees;
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
        int index = Math.min((int) (progress(playback) * (playback.path().size() - 1)), playback.path().size() - 2);
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
        float distance = 0;
        for (int i = 1; i <= index + 1; i++) {
            var a = playback.path().get(i - 1).coords();
            var b = playback.path().get(i).coords();
            float segment = (float) Math.hypot(BoardGeometry.centerX(b) - BoardGeometry.centerX(a),
                  BoardGeometry.centerY(b) - BoardGeometry.centerY(a)) / BoardGeometry.HEIGHT;
            distance += segment * (i == index + 1 ? step - index : 1);
        }
        float dx = BoardGeometry.centerX(to.coords()) - BoardGeometry.centerX(from.coords());
        float dy = BoardGeometry.centerY(to.coords()) - BoardGeometry.centerY(from.coords());
        if (playback.transport()) {
            var tangent = curve(playback.path(), index, step - index, true);
            dx = tangent.x;
            dy = tangent.y;
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
        if (playback.jump() != null && seconds < playback.travel().getFirst().end()) {
            // Terrain clearance and unequal landing heights can move the apex away from the middle of the timeline.
            float descent = MathUtils.clamp((progress - playback.jump().descentStart())
                  / Math.max(.0001f, 1 - playback.jump().descentStart()), 0, 1);
            jets = new JumpJets(playback.sequence(), 1 - descent * descent * (3 - 2 * descent), 1 - progress,
                  (float) seconds, (float) playback.travel().getFirst().end());
        }
        LandingGear gear = landingGear(playback, seconds);
        return new Sample(seconds < playback.duration(), playback.type(), progress, distance,
              UpperBodyTurn.shortestTurn((to.facing() - from.facing()) * 60),
              MathUtils.cosDeg(direction - facing), direction, (float) Math.max(0, seconds - playback.duration()), posture, boarding(playback, seconds), jets,
              gear == null ? aeroState(from, to) : BoardScene.AeroState.LANDED, gear, playback.sequence(), null, posture(playback, seconds),
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

    private float progress(Playback playback) {
        return progress(playback, elapsed);
    }

    private static float progress(Playback playback, double seconds) {
        if (playback.jump() != null) {
            return travelProgress(seconds, playback.travel().getFirst().end());
        }
        // Ease a continuous travel block once, not every hex. Gear/boarding stops delimit independent blocks.
        for (int first = 0; first < playback.travel().size();) {
            int last = first;
            while (last + 1 < playback.travel().size()
                  && playback.travel().get(last + 1).start() == playback.travel().get(last).end()) {
                last++;
            }
            double start = playback.travel().get(first).start(), end = playback.travel().get(last).end();
            if (seconds <= end) {
                seconds = start + travelProgress(seconds - start, end - start) * (end - start);
                break;
            }
            first = last + 1;
        }
        for (int i = 0; i < playback.travel().size(); i++) {
            Travel leg = playback.travel().get(i);
            if (seconds <= leg.end()) {
                double fraction = leg.end() == leg.start() ? 1 : Math.clamp((seconds - leg.start()) / (leg.end() - leg.start()), 0, 1);
                return (float) ((i + fraction) / playback.travel().size());
            }
        }
        return 1;
    }

    static float travelProgress(double seconds, double duration) {
        if (duration == 0) {
            return seconds < 0 ? 0 : 1;
        }
        double time = Math.clamp(seconds, 0, duration);
        double ramp = Math.min(RAMP_SECONDS, duration / 2);
        double distance = duration - ramp;
        if (time < ramp) {
            return (float) (time * time / (2 * ramp * distance));
        }
        if (time > duration - ramp) {
            double remaining = duration - time;
            return (float) (1 - remaining * remaining / (2 * ramp * distance));
        }
        return (float) ((time - ramp / 2) / distance);
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

    /** A short corner arc follows the same waypoints; this changes presentation only, never movement legality. */
    private static Vector3 curve(List<BoardScene.Waypoint> path, int index, float t, boolean tangent) {
        var a = point(path, index - 1);
        var b = point(path, index);
        var c = point(path, index + 1);
        var d = point(path, index + 2);
        float square = t * t;
        float cube = square * t;
        if (tangent) {
            return a.scl(-1 + 4 * t - 3 * square).mulAdd(b, -10 * t + 9 * square)
                  .mulAdd(c, 1 + 8 * t - 9 * square).mulAdd(d, -2 * t + 3 * square).scl(.5f);
        }
        // Keep elevation on the original segment; road cuts/ramps remain owned by surfacePosition().
        float elevation = MathUtils.lerp(b.z, c.z, t);
        var result = a.scl(-t + 2 * square - cube).mulAdd(b, 2 - 5 * square + 3 * cube)
              .mulAdd(c, t + 4 * square - 3 * cube).mulAdd(d, -square + cube).scl(.5f);
        result.z = elevation;
        return result;
    }

    private static Vector3 point(List<BoardScene.Waypoint> path, int index) {
        var point = path.get(Math.clamp(index, 0, path.size() - 1));
        return BoardGeometry.center(point.coords(), point.elevation());
    }
}
