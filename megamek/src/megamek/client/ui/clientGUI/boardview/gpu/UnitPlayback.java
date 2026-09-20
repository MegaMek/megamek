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
    final Map<Integer, UnitMotion> motions = new HashMap<>();
    private final ArrayDeque<BoardScene.Animation> pending = new ArrayDeque<>();
    private final Map<Integer, BoardScene.Waypoint> observed = new HashMap<>();
    private BoardScene.Animation active;
    private UnitAttack attack;
    private double hold;
    private boolean completed;
    private boolean paused;
    private Predicate<BoardScene.Unit> transports = ignored -> false;
    private final Consumer<BoardScene.Movement> completeMovement;
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
        this.completeMovement = completeMovement;
    }

    void accept(List<BoardScene.Animation> events, BoardScene scene, Predicate<BoardScene.Unit> transports) {
        this.transports = transports;
        Set<Integer> visible = scene.units().stream().filter(unit -> !unit.sensorContact()).map(BoardScene.Unit::id)
              .collect(Collectors.toSet());
        observed.keySet().retainAll(visible);
        for (var event : events) {
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
                }
            }
            if (!completed) {
                UnitMotion motion = active instanceof BoardScene.Movement ? motions.get(active.entityId()) : null;
                double left = motion == null ? attack.duration - attack.seconds : motion.remainingSeconds();
                double step = Math.min(remaining, Math.max(0, left) / speed.rate);
                if (motion == null) {
                    attack.seconds = Math.min(attack.duration, attack.seconds + (float) (step * speed.rate));
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
        }
    }

    UnitAttack attack() { return attack; }

    boolean busy() { return active != null || !pending.isEmpty(); }

    double holdSeconds() { return hold; }

    boolean paused() { return paused; }

    void togglePaused() { paused = !paused; }

    private UnitMotion start(BoardScene.Movement movement) {
        var motion = motions.computeIfAbsent(movement.entityId(), ignored -> new UnitMotion(movement.path().getFirst()));
        var unit = movement.unit();
        int members = unit != null && unit.model() != null && unit.model().state() != null
              && unit.model().state().structure().activeTroopers() > 0 ? unit.model().figures() : 0;
        motion.append(movement.path(), movement.type(), movement.jumpMP(), transports.test(unit), movement.movementMP(), members);
        return motion;
    }

    void finish() {
        motions.values().forEach(UnitMotion::finish);
        if (active instanceof BoardScene.Movement movement && !completed) {
            completeMovement.accept(movement);
        }
        for (var event : pending) {
            if (event instanceof BoardScene.SceneUpdate update) {
                settledScene = update.scene();
            } else if (event instanceof BoardScene.Movement movement) {
                start(movement).finish();
                completeMovement.accept(movement);
            }
        }
        resetQueue();
    }

    private void resetQueue() {
        pending.clear();
        active = null;
        attack = null;
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
        if (active != null && !completed && (attack == null || attack.seconds < attack.contactSeconds)) {
            return;
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
        boolean awaitingAction = active != null && !completed && (attack == null || attack.seconds < attack.contactSeconds)
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
        if (active != null && !completed && (attack == null || attack.seconds < attack.contactSeconds)) {
            holdUnits(active, shown, true);
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

    private void holdUnits(BoardScene.Animation event, Map<Integer, BoardScene.Unit> shown, boolean playing) {
        if (!playing && checkpoints) {
            // The preceding checkpoint already contains the waiting unit's then-visible pose and appearance.
            return;
        }
        if (event instanceof BoardScene.Combat combat) {
            shown.putIfAbsent(combat.attacker().id(), combat.attacker());
            if (combat.target() != null) {
                shown.putIfAbsent(combat.target().id(), combat.target());
            }
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
