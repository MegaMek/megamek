/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import megamek.common.units.EntityMovementType;

/** One GL-owned event queue for both cameras. The game remains free to receive updates while it plays. */
final class UnitPlayback {
    static final double COMPLETION_HOLD_SECONDS = 1;
    static final int MAX_PENDING_EVENTS = 512;
    /** Maximum spread between a unit's weapon launches, in shared animation seconds. */
    static final float VOLLEY_JITTER_SECONDS = .12f;
    final Map<Integer, UnitMotion> motions = new HashMap<>();
    /** GL-owned visual preview; NaN keeps each event's captured game gravity. Read only when a jump starts. */
    float gravityOverride = Float.NaN;
    private final ArrayDeque<BoardScene.Animation> pending = new ArrayDeque<>();
    private final Map<Integer, BoardScene.Waypoint> observed = new HashMap<>();
    private BoardScene.Animation active;
    private UnitAttack attack;
    private final List<UnitAttack> attacks = new ArrayList<>();
    private final UnitVolley volley = new UnitVolley();
    private final List<UnitAttack> visibleAttacks = java.util.Collections.unmodifiableList(attacks);
    private double combatSeconds, combatDuration, combatContact;
    private BoardScene volleyScene;
    private UnitConversion conversion;
    private double hold;
    private boolean completed;
    private boolean paused;
    private Predicate<BoardScene.Unit> transports = ignored -> false;
    private final Consumer<BoardScene.Movement> completeMovement;
    private final java.util.function.BiConsumer<UnitAttack, Boolean> soundCue;
    // GL-owned presentation history only. The game and current selection continue to update on Swing.
    private BoardScene settledScene;
    private BoardScene overlayInput;
    private BoardScene overlayState;
    private BoardScene presentationScene;
    private boolean overlayHideMovement;
    private boolean checkpoints;

    UnitPlayback() {
        this(ignored -> { });
    }

    UnitPlayback(Consumer<BoardScene.Movement> completeMovement) {
        this(completeMovement, (attack, contact) -> { });
    }

    /** Optional audio consumes launch/contact edges from this clock, never from drawing or attack declarations. */
    UnitPlayback(Consumer<BoardScene.Movement> completeMovement, java.util.function.BiConsumer<UnitAttack, Boolean> soundCue) {
        this.completeMovement = completeMovement;
        this.soundCue = soundCue;
    }

    void accept(List<BoardScene.Animation> events, BoardScene scene, Predicate<BoardScene.Unit> transports) {
        this.transports = transports;
        int start = 0;
        for (int index = 0; index < events.size(); index++) {
            if (events.get(index) instanceof BoardScene.Concealed hidden && hidden.boardId() == scene.boardId()) {
                // Catch up to the authorized scene. Historical snapshots must not restore a hidden identity.
                boolean wasPaused = paused;
                clear();
                paused = wasPaused;
                settledScene = scene;
                start = index + 1;
            }
        }
        Set<Integer> visible = scene.units().stream().filter(unit -> !unit.sensorContact()).map(BoardScene.Unit::id)
              .collect(Collectors.toSet());
        observed.keySet().retainAll(visible);
        for (var event : events.subList(start, events.size())) {
            if (event.boardId() == scene.boardId()
                  && (!(event instanceof BoardScene.Movement move)
                        || (move.unit() != null || visible.contains(move.entityId())) && move.path().size() > 1)) {
                checkpoints |= event instanceof BoardScene.SceneUpdate;
                enqueue(event);
            }
        }
        for (var unit : scene.units()) {
            if (unit.sensorContact() || unit.location().aeroState() == null) {
                continue;
            }
            var previous = observed.put(unit.id(), unit.location());
            if (!checkpoints && UnitMotion.changesGear(previous, unit.location()) && events.stream().noneMatch(event ->
                  event instanceof BoardScene.Movement movement && movement.entityId() == unit.id())) {
                enqueue(new BoardScene.Movement(unit.id(), scene.boardId(), List.of(previous, unit.location()),
                      EntityMovementType.MOVE_SAFE_THRUST, 0, 0, unit));
            }
        }
        collectVolley();
        applySceneUpdates();
    }

    private void enqueue(BoardScene.Animation event) {
        if (event instanceof BoardScene.SceneUpdate && pending.peekLast() instanceof BoardScene.SceneUpdate) {
            pending.removeLast();
        }
        if (pending.size() >= MAX_PENDING_EVENTS) {
            // A client catching up on a large backlog snaps to the game, instead of retaining unbounded snapshots.
            finish();
        }
        pending.addLast(event);
    }

    void advance(double seconds, UnitMotion.Speed speed) {
        advance(seconds, speed, ignored -> true);
    }

    /** The view may briefly hold an action while its camera frames the participants or complete movement route. */
    void advance(double seconds, UnitMotion.Speed speed, Predicate<UnitPlayback> cameraReady) {
        if (speed == UnitMotion.Speed.INSTANT) {
            paused = false;
            finish();
            return;
        }
        if (paused) {
            return;
        }
        double remaining = Math.max(0, seconds);
        motions.values().stream().filter(motion -> !motion.isMoving()).forEach(motion -> motion.advance(seconds, speed.rate));
        while (true) {
            applySceneUpdates();
            if (active == null) {
                active = pending.pollFirst();
                if (active == null) {
                    return;
                }
                completed = false;
                if (active instanceof BoardScene.Movement movement) {
                    start(movement);
                } else if (active instanceof BoardScene.Combat combat) {
                    attack = new UnitAttack(combat);
                    attacks.add(attack);
                    combatSeconds = 0;
                    combatDuration = attack.duration;
                    combatContact = attack.contactSeconds;
                    if (attack.shot()) { volley.add(attack, 0); }
                    collectVolley();
                } else if (active instanceof BoardScene.Conversion change) {
                    conversion = new UnitConversion(change);
                }
            }
            if (!completed) {
                if (!cameraReady.test(this)) { return; }
                UnitMotion motion = active instanceof BoardScene.Movement ? motions.get(active.entityId()) : null;
                double left = conversion != null ? UnitConversion.DURATION_SECONDS - conversion.seconds
                      : motion == null ? combatDuration - combatSeconds : motion.remainingSeconds();
                double step = Math.min(remaining, Math.max(0, left) / speed.rate);
                if (conversion != null) {
                    conversion.seconds = Math.min(UnitConversion.DURATION_SECONDS, conversion.seconds + (float) (step * speed.rate));
                } else if (motion == null) {
                    combatSeconds = Math.min(combatDuration, combatSeconds + step * speed.rate);
                    volley.advance((float) combatSeconds);
                    attacks.forEach(shot -> {
                        float previous = shot.seconds;
                        shot.seconds = Math.min(shot.duration, (float) combatSeconds - shot.delay);
                        if (previous < UnitAttack.ANTICIPATION_SECONDS && shot.seconds >= UnitAttack.ANTICIPATION_SECONDS) {
                            soundCue.accept(shot, false);
                        }
                        if (previous < shot.contactSeconds && shot.seconds >= shot.contactSeconds) { soundCue.accept(shot, true); }
                    });
                    applySceneUpdates();
                } else {
                    motion.advance(step, speed.rate);
                }
                remaining = Math.max(0, remaining - step);
                if (step * speed.rate + 1e-7 < left) {
                    return;
                }
                completed = true;
                if (active instanceof BoardScene.Movement movement) {
                    completeMovement.accept(movement);
                } else if (active instanceof BoardScene.Conversion change) {
                    settleConversion(change);
                }
                applySceneUpdates();
                hold = COMPLETION_HOLD_SECONDS;
            }
            double pause = Math.min(remaining, hold);
            hold -= pause;
            remaining -= pause;
            if (hold > 1e-9) {
                return;
            }
            active = null;
            attack = null;
            attacks.clear();
            volley.clear();
            volleyScene = null;
            conversion = null;
        }
    }

    UnitAttack attack() { return attack; }

    List<UnitAttack> attacks() { return visibleAttacks; }

    BoardScene.Movement movement() { return active instanceof BoardScene.Movement move ? move : null; }

    /** Interpolate only a displacement actually present in the post-resolution checkpoint. */
    void placeDisplacement(BoardScene.Unit unit, com.badlogic.gdx.math.Vector3 position) {
        if (attack == null || attack.event.result().kind() != megamek.common.ResolvedAttack.Kind.PUSH
              || !attack.event.result().hit() || beforeImpact()) { return; }
        var before = unit.id() == attack.event.entityId() ? attack.event.attacker()
              : attack.event.target() != null && unit.id() == attack.event.target().id() ? attack.event.target() : null;
        if (before == null || before.location().coords().equals(unit.location().coords())) { return; }
        float progress = com.badlogic.gdx.math.MathUtils.clamp((attack.seconds - attack.contactSeconds)
              / UnitAttack.RECOVERY_SECONDS, 0, 1);
        progress *= progress * (3 - 2 * progress);
        var origin = BoardGeometry.center(before.location().coords(), before.location().elevation());
        position.set(origin.lerp(position, progress));
    }

    /** Adjacent confirmed shots from the same pose form one volley; movement and physical actions are barriers. */
    private void collectVolley() {
        if (attack == null || !attack.shot()) { return; }
        while (attacks.size() < MAX_PENDING_EVENTS) {
            BoardScene.Combat next = null;
            for (var event : pending) {
                if (event instanceof BoardScene.SceneUpdate) { continue; }
                if (event instanceof BoardScene.Combat casualty && casualty.result().kind() == megamek.common.ResolvedAttack.Kind.DEATH) {
                    continue; // Finish the firing unit's volley before playing its resulting casualties.
                }
                if (event instanceof BoardScene.Combat combat && combat.result().kind() == megamek.common.ResolvedAttack.Kind.SHOT
                      && sameVolley(combat)) { next = combat; }
                break;
            }
            if (next == null) { return; }
            for (var iterator = pending.iterator(); iterator.hasNext();) {
                var event = iterator.next();
                if (event == next) { iterator.remove(); break; }
                if (event instanceof BoardScene.SceneUpdate update) {
                    volleyScene = update.scene();
                    iterator.remove();
                }
            }
            var shot = new UnitAttack(next);
            attacks.add(shot);
            volley.add(shot, (float) combatSeconds);
            combatDuration = attacks.stream().mapToDouble(item -> item.delay + item.duration).max().orElseThrow();
            combatContact = attacks.stream().mapToDouble(item -> item.delay + item.contactSeconds).max().orElseThrow();
            completed = false;
            hold = 0;
        }
    }

    private boolean beforeImpact() { return attack == null || combatSeconds + 1e-7 < combatContact; }

    private boolean sameVolley(BoardScene.Combat next) {
        var primary = attacks.stream().filter(shot -> !shot.defensive()).findFirst().orElse(attack);
        boolean defensive = next.result().shot() != null && next.result().shot().defensive();
        if (defensive) {
            int incoming = primary.defensive() ? primary.event.result().target().entityId() : primary.event.entityId();
            return next.result().target().entityId() == incoming;
        }
        if (primary.defensive()) { return next.entityId() == primary.event.result().target().entityId(); }
        return next.entityId() == primary.event.entityId()
              && next.attacker().location().samePose(primary.event.attacker().location());
    }

    UnitConversion conversion() { return conversion; }

    boolean busy() { return active != null || !pending.isEmpty(); }

    /** The final queued action lets Instant playback settle its camera once, after the whole queue is applied. */
    BoardScene.Animation lastAction() {
        var events = pending.descendingIterator();
        while (events.hasNext()) {
            var event = events.next();
            if (!(event instanceof BoardScene.SceneUpdate)) { return event; }
        }
        return active;
    }

    /** The action being presented retains camera focus through its completion hold. */
    int activeEntityId() { return active == null ? -1 : active.entityId(); }

    double holdSeconds() { return hold; }

    boolean paused() { return paused; }

    void togglePaused() { paused = !paused; }

    private UnitMotion start(BoardScene.Movement movement) {
        var motion = motions.computeIfAbsent(movement.entityId(), ignored -> new UnitMotion(movement.path().getFirst()));
        var unit = movement.unit();
        int members = unit != null && unit.model() != null && unit.model().state() != null
              && unit.model().state().structure().activeTroopers() > 0 ? unit.model().figures() : 0;
        motion.append(movement.path(), movement.type(), movement.jumpMP(), transports.test(unit), movement.movementMP(), members,
              UnitMotion.DEFAULT_SPEED_GAIN_PER_HEX, Float.isFinite(gravityOverride) ? gravityOverride : movement.gravity());
        return motion;
    }

    void finish() {
        motions.values().forEach(UnitMotion::finish);
        if (active instanceof BoardScene.Movement movement && !completed) {
            completeMovement.accept(movement);
        } else if (active instanceof BoardScene.Conversion change) {
            settleConversion(change);
        }
        if (volleyScene != null) { settledScene = volleyScene; }
        for (var event : pending) {
            if (event instanceof BoardScene.SceneUpdate update) {
                settledScene = update.scene();
            } else if (event instanceof BoardScene.Movement movement) {
                start(movement).finish();
                completeMovement.accept(movement);
            } else if (event instanceof BoardScene.Conversion change) {
                settleConversion(change);
            }
        }
        resetQueue();
    }

    private void resetQueue() {
        pending.clear();
        active = null;
        attack = null;
        attacks.clear();
        volley.clear();
        volleyScene = null;
        conversion = null;
        hold = 0;
        completed = false;
    }

    void clear() {
        resetQueue();
        paused = false;
        motions.clear();
        observed.clear();
        settledScene = null;
        overlayInput = null;
        overlayState = null;
        presentationScene = null;
        checkpoints = false;
    }

    /** Checkpoints after an action become visible at arrival/impact, before its recovery or completion hold. */
    private void applySceneUpdates() {
        if (active != null && !completed && beforeImpact()) {
            return;
        }
        if (volleyScene != null) {
            settledScene = volleyScene;
            volleyScene = null;
        }
        while (pending.peekFirst() instanceof BoardScene.SceneUpdate update) {
            pending.removeFirst();
            settledScene = update.scene();
        }
    }

    /** Retain only captured, authorized appearance until its event plays; no later loadout or damage leaks forward. */
    BoardScene present(BoardScene scene) {
        applySceneUpdates();
        // UnitMotion completion includes landing, unloading and formation settling. Pending moves also hide
        // overlays between events; the final completion hold does not delay their return.
        boolean movementPending = active instanceof BoardScene.Movement && !completed
              || pending.stream().anyMatch(BoardScene.Movement.class::isInstance);
        boolean awaitingAction = active != null && !completed && beforeImpact()
              || pending.stream().anyMatch(event -> !(event instanceof BoardScene.SceneUpdate));
        if (awaitingAction) {
            // Review fixtures without checkpoints cannot supply history for their first combat frame.
            if (settledScene == null && !checkpoints && !movementPending) {
                settledScene = scene;
            }
            if (overlayInput != scene || overlayState != settledScene || overlayHideMovement != movementPending) {
                overlayInput = scene;
                overlayState = settledScene;
                overlayHideMovement = movementPending;
                presentationScene = scene.duringPlayback(settledScene, movementPending);
            }
            scene = presentationScene;
        } else {
            settledScene = scene;
            overlayInput = null;
            overlayState = null;
            presentationScene = null;
        }
        if (!busy()) {
            retainMotions(scene.units());
            return scene;
        }
        Map<Integer, BoardScene.Unit> shown = new LinkedHashMap<>();
        if (active != null && !completed && beforeImpact()) {
            if (attack == null) { holdUnits(active, shown, true); }
            else { attacks.forEach(shot -> holdUnits(shot.event, shown, true)); }
        }
        pending.forEach(event -> holdUnits(event, shown, false));
        List<BoardScene.Unit> units = new ArrayList<>();
        for (var unit : scene.units()) {
            var replacement = shown.remove(unit.id());
            units.add(replacement == null ? unit : replacement);
        }
        // An already-visible victim may be removed by the game before its final received attack has played.
        units.addAll(shown.values());
        retainMotions(units);
        return scene.withUnits(units);
    }

    private void retainMotions(List<BoardScene.Unit> units) {
        motions.keySet().retainAll(units.stream().filter(unit -> !unit.sensorContact())
              .map(BoardScene.Unit::id).collect(Collectors.toSet()));
    }

    private void settleConversion(BoardScene.Conversion change) {
        if (settledScene != null) {
            settledScene = settledScene.withUnits(settledScene.units().stream()
                  .map(unit -> unit.id() == change.entityId() ? change.after() : unit).toList());
        }
    }

    private void holdUnits(BoardScene.Animation event, Map<Integer, BoardScene.Unit> shown, boolean playing) {
        if (!playing && checkpoints
              && !(event instanceof BoardScene.Combat combat && combat.result().kind() == megamek.common.ResolvedAttack.Kind.DEATH)) {
            // The preceding checkpoint already contains the waiting unit's then-visible pose and appearance.
            // A queued collapse still needs its authorized victim after the volley checkpoint removes it.
            return;
        }
        if (event instanceof BoardScene.Combat combat) {
            shown.putIfAbsent(combat.attacker().id(), combat.attacker());
            if (combat.target() != null) {
                shown.putIfAbsent(combat.target().id(), combat.target());
            }
        } else if (event instanceof BoardScene.Conversion change) {
            shown.putIfAbsent(change.entityId(), playing && conversion != null ? conversion.displayed() : change.before());
        } else if (event instanceof BoardScene.Movement movement && movement.unit() != null) {
            var unit = movement.unit();
            var appearance = checkpoints && settledScene != null ? settledScene.units().stream()
                  .filter(previous -> previous.id() == unit.id() && !previous.sensorContact()).findFirst().orElse(unit) : unit;
            var location = playing ? movement.path().getLast() : movement.path().getFirst();
            var footprint = location.footprint().isEmpty()
                  ? playing ? unit.footprint() : List.of(location.coords()) : location.footprint();
            shown.putIfAbsent(unit.id(), new BoardScene.Unit(unit.id(), unit.part(), appearance.name(), location, appearance.image(),
                  false, appearance.annotations(), unit.height(), unit.airborne(), appearance.model(), unit.outlineRgb(), footprint));
        }
    }
}
