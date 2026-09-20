/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import megamek.common.ResolvedAttack;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Targetable;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;

class UnitPlaybackTest {
    @Test
    void cameraHoldStopsEveryShotAndSoundAtEachVolleyBoundaryEvenWithALargeFastFrame() {
        var attacker = unit(1, 0);
        var firstTarget = unit(2, 8);
        var secondTarget = unit(3, 12);
        var first = attack(attacker, firstTarget, ResolvedAttack.Kind.SHOT, true);
        var second = attack(attacker, secondTarget, ResolvedAttack.Kind.SHOT, true);
        var reply = attack(firstTarget, attacker, ResolvedAttack.Kind.SHOT, true);
        List<UnitAttack> sounds = new ArrayList<>();
        var playback = new UnitPlayback(ignored -> { }, (shot, contact) -> sounds.add(shot));
        playback.accept(List.of(first, second, reply), scene(attacker, firstTarget, secondTarget), ignored -> false);
        playback.advance(10, UnitMotion.Speed.QUADRUPLE, state -> {
            assertEquals(2, state.attacks().size(), "The camera receives every target before any shot advances");
            return false;
        });
        assertEquals(2, playback.attacks().size());
        playback.attacks().forEach(shot -> assertTrue(shot.seconds <= 0, "Later target passes may have a launch delay"));
        assertTrue(sounds.isEmpty());
        assertFalse(playback.paused(), "Camera framing must not toggle the user's pause setting");
        playback.advance(10, UnitMotion.Speed.QUADRUPLE, state -> state.attack().event == first);
        assertSame(reply, playback.attack().event, "The next attacker also gets a camera hold within the same frame");
        assertEquals(0, playback.attack().seconds);
        assertTrue(sounds.stream().noneMatch(shot -> shot.event == reply));
        playback.advance(0, UnitMotion.Speed.INSTANT, ignored -> false);
        assertFalse(playback.busy(), "Skipping playback must also skip its camera hold");
    }

    @Test
    void movementCapturesTuningAtStartAndJumpUsesCapturedGravity() {
        var start = unit(1, 0).location();
        var end = unit(1, 40);
        var walk = new BoardScene.Movement(1, 0, List.of(start, end.location()), EntityMovementType.MOVE_WALK, 0, 4, end);
        var playback = new UnitPlayback();
        playback.speedGainPerHex = 0;
        playback.accept(List.of(walk, walk), scene(end), ignored -> false);
        playback.advance(0, UnitMotion.Speed.NORMAL);
        double duration = playback.motions.get(1).remainingSeconds();
        playback.speedGainPerHex = .06;
        playback.advance(0, UnitMotion.Speed.NORMAL);
        assertEquals(duration, playback.motions.get(1).remainingSeconds(), "Changing tuning cannot warp active movement");
        playback.advance(duration / UnitMotion.Speed.NORMAL.rate + UnitPlayback.COMPLETION_HOLD_SECONDS, UnitMotion.Speed.NORMAL);
        assertTrue(playback.motions.get(1).remainingSeconds() < duration, "The next move uses the new gain");
        for (float gravity : new float[] { 0, .5f, 1, 2 }) {
            var landing = unit(2, 1);
            var jump = new BoardScene.Movement(2, 0, List.of(start, landing.location()), EntityMovementType.MOVE_JUMP, 6, 6, landing, gravity);
            var flight = new UnitPlayback();
            flight.accept(List.of(jump), scene(landing), ignored -> false);
            flight.advance(0, UnitMotion.Speed.NORMAL);
            var motion = flight.motions.get(2);
            double step = motion.remainingSeconds() / 1000 / UnitMotion.Speed.NORMAL.rate;
            float highest = 0;
            for (int i = 0; i < 1000; i++) {
                flight.advance(step, UnitMotion.Speed.NORMAL);
                highest = Math.max(highest, motion.position().z);
            }
            assertEquals(4 / Math.max(.25, gravity), highest / BoardGeometry.LEVEL, .001);
            flight.finish();
            assertFalse(flight.busy());
            assertNull(motion.sample().jets());
        }
    }

    @Test
    void pushUsesObservedDisplacementForBothParticipantsAndNeverInventsAMove() {
        var attacker = unit(1, 0);
        var victim = unit(2, 1);
        var advanced = unit(1, 1);
        var pushed = unit(2, 2);
        for (boolean moved : List.of(true, false)) {
            var after = moved ? scene(advanced, pushed) : scene(attacker, victim);
            for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.QUADRUPLE)) {
                var playback = new UnitPlayback();
                playback.accept(List.of(new BoardScene.SceneUpdate(scene(attacker, victim)),
                      attack(attacker, victim, ResolvedAttack.Kind.PUSH, true), new BoardScene.SceneUpdate(after)), after, ignored -> false);
                playback.advance((.65 + UnitAttack.RECOVERY_SECONDS * .5) / speed.rate, speed);
                var visible = playback.present(after);
                for (var unit : visible.units()) {
                    var end = BoardGeometry.center(unit.location().coords(), 0);
                    var position = end.cpy();
                    playback.placeDisplacement(unit, position);
                    var original = BoardGeometry.center((unit.id() == 1 ? attacker : victim).location().coords(), 0);
                    assertEquals(original.lerp(end, .5f).y, position.y, .0001);
                }
                playback.advance(UnitAttack.RECOVERY_SECONDS / speed.rate + 1, speed);
                assertEquals(after.units(), playback.present(after).units());
            }
        }
    }

    @Test
    void destructionFinishesBeforeRemovalAtEverySpeedAndCanBePausedOrSkipped() {
        var victim = unit(2, 5);
        var removed = scene();
        for (var speed : UnitMotion.Speed.values()) {
            var playback = new UnitPlayback();
            playback.accept(List.of(new BoardScene.SceneUpdate(scene(victim)),
                  attack(victim, victim, ResolvedAttack.Kind.DEATH, true), new BoardScene.SceneUpdate(removed)),
                  removed, ignored -> false);
            playback.advance(0, speed);
            if (speed != UnitMotion.Speed.INSTANT) {
                playback.advance((UnitAttack.DEATH_SECONDS - .001) / speed.rate, speed);
                assertTrue(playback.present(removed).units().contains(victim));
                float pose = playback.attack().deathProgress();
                playback.togglePaused();
                playback.advance(20, speed);
                assertEquals(pose, playback.attack().deathProgress());
                playback.togglePaused();
                playback.advance(.001 / speed.rate, speed);
                assertTrue(playback.present(removed).units().isEmpty());
                assertEquals(1, playback.holdSeconds(), 1e-5);
                playback.advance(1.001, speed);
            }
            assertFalse(playback.busy());
            assertTrue(playback.present(removed).units().isEmpty());
        }
    }

    @Test
    void concealmentClearsCapturedPassengersAttacksAndHistoryWithoutReplayingOnReveal() {
        var original = unit(1, 4);
        var victim = unit(2, 5);
        var playback = new UnitPlayback();
        playback.accept(List.of(new BoardScene.SceneUpdate(scene(original, victim)), move(1),
              attack(original, victim, ResolvedAttack.Kind.SHOT, true)), scene(original, victim), ignored -> true);
        playback.advance(.2, UnitMotion.Speed.NORMAL);
        assertNotNull(playback.motions.get(1).sample().boarding());
        playback.togglePaused();
        playback.accept(List.of(new BoardScene.Concealed(1, 0), new BoardScene.SceneUpdate(scene(victim))),
              scene(victim), ignored -> true);
        assertTrue(playback.paused());
        assertTrue(playback.motions.isEmpty());
        assertTrue(playback.attacks().isEmpty());
        assertEquals(List.of(victim), playback.present(scene(victim)).units());
        playback.accept(List.of(new BoardScene.SceneUpdate(scene(original, victim))), scene(original, victim), ignored -> true);
        playback.togglePaused();
        playback.advance(10, UnitMotion.Speed.NORMAL);
        assertFalse(playback.busy());
        assertTrue(playback.motions.isEmpty());
    }

    @Test
    void pauseFreezesTravelAttacksAndTheRealTimeCompletionHoldWhileInstantStillSkips() {
        var playback = new UnitPlayback();
        playback.accept(List.of(move(1), attack(unit(1, 4), unit(2, 5), ResolvedAttack.Kind.SHOT, true)),
              scene(unit(1, 4), unit(2, 5)), ignored -> false);
        playback.advance(.5, UnitMotion.Speed.NORMAL);
        var motion = playback.motions.get(1);
        var position = motion.position().cpy();
        double remaining = motion.remainingSeconds();
        playback.togglePaused();
        playback.advance(100, UnitMotion.Speed.QUADRUPLE);
        assertEquals(position, motion.position());
        assertEquals(remaining, motion.remainingSeconds());
        playback.togglePaused();
        playback.advance(remaining / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        playback.togglePaused();
        playback.advance(100, UnitMotion.Speed.HALF);
        assertEquals(1, playback.holdSeconds(), 1e-6);
        playback.togglePaused();
        playback.advance(1.1, UnitMotion.Speed.NORMAL);
        float attackSeconds = playback.attack().seconds;
        playback.togglePaused();
        playback.advance(100, UnitMotion.Speed.NORMAL);
        assertEquals(attackSeconds, playback.attack().seconds);
        playback.advance(0, UnitMotion.Speed.INSTANT);
        assertFalse(playback.busy());
        assertFalse(playback.paused());
    }

    @Test
    void skippingQueuedMovesAppliesCapturedArrivalStatesInOrderAndClearingDoesNotReplayThem() {
        var completions = new ArrayList<BoardScene.Movement>();
        var playback = new UnitPlayback(completions::add);
        var first = move(1);
        var second = new BoardScene.Movement(1, 0, List.of(unit(1, 4).location(), unit(1, 0).location()),
              EntityMovementType.MOVE_WALK, 0, 4, unit(1, 0));
        playback.accept(List.of(first, second), scene(unit(1, 0)), ignored -> true);
        playback.advance(.1, UnitMotion.Speed.NORMAL);
        playback.advance(0, UnitMotion.Speed.INSTANT);
        assertEquals(List.of(first, second), completions);
        assertEquals(BoardGeometry.center(second.path().getLast().coords(), 0), playback.motions.get(1).position());
        assertEquals(UnitMotion.Stage.UNLOAD, playback.motions.get(1).sample().boarding().stage());
        assertEquals(1, playback.motions.get(1).sample().boarding().progress());
        playback.accept(List.of(first), scene(unit(1, 4)), ignored -> true);
        playback.clear();
        assertEquals(2, completions.size(), "A board switch discards its old playback instead of re-creating old models");
        assertTrue(playback.motions.isEmpty());
    }

    static BoardScene.Unit unit(int id, int row) {
        return new BoardScene.Unit(id, -1, "Unit " + id, new BoardScene.Waypoint(new Coords(0, row), 0, 0),
              null, false, null, 2, false);
    }

    static BoardScene scene(BoardScene.Unit... units) {
        var tile = new BoardScene.Tile(new Coords(0, 0), 0, -1, false, 0, null, null, null, null, List.of(), List.of());
        return new BoardScene(0, 1, 1, List.of(tile), List.of(units), List.of(), -1, "MOVEMENT", List.of());
    }

    static BoardScene.Combat attack(BoardScene.Unit attacker, BoardScene.Unit target, ResolvedAttack.Kind kind, boolean hit) {
        var result = new ResolvedAttack(UUID.randomUUID(), kind,
              new UnitLocation(attacker.id(), attacker.location().coords(), 0, 0, 0),
              new UnitLocation(target.id(), target.location().coords(), 0, 0, 0), Targetable.TYPE_ENTITY, 0, "ISMediumLaser", 4, hit);
        return new BoardScene.Combat(result, attacker, target, target.location());
    }

    private static BoardScene.Movement move(int id) {
        return new BoardScene.Movement(id, 0, List.of(unit(id, 0).location(), unit(id, 4).location()),
              EntityMovementType.MOVE_WALK, 0, 4, unit(id, 4));
    }

    private static BoardScene withRanges(BoardScene scene, String label) {
        Coords coords = new Coords(0, 0);
        var line = new BoardScene.FiringLine(unit(1, 0).location(), unit(2, 4).location(), 0x44FF88, false, 1, 2);
        return new BoardScene(scene.boardId(), scene.width(), scene.height(), scene.tiles(), scene.units(),
              scene.plannedPath(), scene.selectedId(), scene.phase(), scene.commands(), scene.light(), List.of(line),
              List.of(new BoardScene.RangeBorder(coords, 63, 0x44FF88, label)), scene.markers(), scene.tactical(),
              List.of(new BoardScene.RangeLabel(coords, 0x44FF88, label)), scene.fieldOfView());
    }

    @Test
    void rangesWaitForMovementAndThenUseTheLatestSnapshotAtEverySpeed() {
        for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE, UnitMotion.Speed.QUADRUPLE)) {
            var playback = new UnitPlayback();
            var initial = withRanges(scene(unit(1, 4)), "S");
            playback.accept(List.of(move(1)), initial, ignored -> false);
            assertTrue(playback.present(initial).rangeBorders().isEmpty(), "Queued movement must already suppress ranges");
            assertTrue(playback.present(initial).rangeLabels().isEmpty());
            playback.advance(0, speed);
            var motion = playback.motions.get(1);
            playback.advance(motion.remainingSeconds() / (2 * speed.rate), speed);
            assertTrue(motion.isMoving());
            var latest = withRanges(initial, "M");
            playback.accept(List.of(), latest, ignored -> false);
            var moving = playback.present(latest);
            assertTrue(moving.rangeBorders().isEmpty());
            assertTrue(moving.rangeLabels().isEmpty());
            assertTrue(moving.firingLines().isEmpty(), "Attack endpoints must not jump to the destination mid-move");
            assertEquals("S", initial.rangeBorders().getFirst().label(), "Presentation must not mutate published scenes");
            playback.advance(motion.remainingSeconds() / speed.rate, speed);
            assertFalse(motion.isMoving());
            assertTrue(playback.busy(), "The final completion hold is still active");
            assertEquals(latest.rangeBorders(), playback.present(latest).rangeBorders());
            assertEquals(latest.rangeLabels(), playback.present(latest).rangeLabels());
        }
    }

    @Test
    void rangesStayHiddenAcrossQueuedMovesAndInterveningHoldsAndAttacks() {
        var playback = new UnitPlayback();
        var scene = withRanges(scene(unit(1, 4), unit(2, 4)), "S");
        playback.accept(List.of(move(1), attack(unit(1, 4), unit(2, 4), ResolvedAttack.Kind.SHOT, true), move(2)),
              scene, ignored -> false);
        playback.advance(0, UnitMotion.Speed.NORMAL);
        playback.advance(playback.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        assertFalse(playback.motions.get(1).isMoving());
        assertNull(playback.motions.get(2));
        assertTrue(playback.present(scene).rangeBorders().isEmpty(), "Ranges must not flash during the gap between moves");
        playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS, UnitMotion.Speed.NORMAL);
        assertNotNull(playback.attack());
        assertTrue(playback.present(scene).rangeLabels().isEmpty(), "The second move is still waiting behind combat");
        playback.advance(playback.attack().duration / UnitMotion.Speed.NORMAL.rate + UnitPlayback.COMPLETION_HOLD_SECONDS,
              UnitMotion.Speed.NORMAL);
        assertTrue(playback.motions.get(2).isMoving());
        assertTrue(playback.present(scene).rangeBorders().isEmpty());
        playback.advance(playback.motions.get(2).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        assertEquals(scene.rangeBorders(), playback.present(scene).rangeBorders());
        assertEquals(scene.rangeLabels(), playback.present(scene).rangeLabels());
    }

    @Test
    void combatAloneKeepsRangesAndSkippingOrClearingMovementRestoresThem() {
        var scene = withRanges(scene(unit(1, 4), unit(2, 5)), "S");
        var playback = new UnitPlayback();
        playback.accept(List.of(attack(unit(1, 4), unit(2, 5), ResolvedAttack.Kind.SHOT, true)), scene, ignored -> false);
        playback.advance(.1, UnitMotion.Speed.NORMAL);
        assertTrue(playback.busy());
        assertEquals(scene.rangeBorders(), playback.present(scene).rangeBorders());
        assertEquals(scene.rangeLabels(), playback.present(scene).rangeLabels());
        for (Consumer<UnitPlayback> finish : List.<Consumer<UnitPlayback>>of(
              value -> value.advance(0, UnitMotion.Speed.INSTANT), UnitPlayback::finish, UnitPlayback::clear)) {
            playback.clear();
            playback.accept(List.of(move(1)), scene, ignored -> false);
            playback.advance(.1, UnitMotion.Speed.NORMAL);
            assertTrue(playback.present(scene).rangeBorders().isEmpty());
            finish.accept(playback);
            assertSame(scene, playback.present(scene));
        }
    }

    @Test
    void movementHoldsOneRealSecondBeforeTheNextUnitsMoveAtEverySpeed() {
        for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE, UnitMotion.Speed.QUADRUPLE)) {
            var playback = new UnitPlayback();
            var scene = scene(unit(1, 4), unit(2, 4));
            playback.accept(List.of(move(1), move(2)), scene, id -> false);
            playback.advance(0, speed);
            assertEquals(0, playback.present(scene).units().get(1).location().coords().getY(), "Waiting unit stays at departure");
            playback.advance(playback.motions.get(1).remainingSeconds() / speed.rate, speed);
            assertFalse(playback.motions.get(1).isMoving());
            assertEquals(1, playback.holdSeconds(), 1e-6);
            playback.advance(.999, speed);
            assertNull(playback.motions.get(2));
            playback.advance(.002, speed);
            assertTrue(playback.motions.get(2).isMoving());
        }
    }

    @Test
    void attacksShareTheQueueAndHoldAfterRecovery() {
        var playback = new UnitPlayback();
        var scene = scene(unit(1, 4), unit(2, 5));
        var attack = attack(unit(1, 4), unit(2, 5), ResolvedAttack.Kind.SHOT, true);
        playback.accept(List.of(move(1), attack, move(2)), scene, id -> false);
        playback.advance(0, UnitMotion.Speed.NORMAL);
        playback.advance(playback.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        assertNull(playback.attack());
        playback.advance(1, UnitMotion.Speed.NORMAL);
        assertNotNull(playback.attack());
        float duration = playback.attack().duration;
        playback.advance(duration * 2, UnitMotion.Speed.NORMAL);
        assertEquals(duration, playback.attack().seconds);
        assertEquals(1, playback.holdSeconds(), 1e-6);
        assertNull(playback.motions.get(2));
        playback.advance(1, UnitMotion.Speed.NORMAL);
        assertNull(playback.attack());
        assertTrue(playback.motions.get(2).isMoving());
    }

    @Test
    void instantAndBoardChangesCancelEffectsAndHolds() {
        var playback = new UnitPlayback();
        var scene = scene(unit(1, 4), unit(2, 5));
        for (var kind : ResolvedAttack.Kind.values()) {
            playback.accept(List.of(attack(unit(1, 4), unit(2, 5), kind, true), move(1)), scene, id -> false);
            playback.advance(.5, UnitMotion.Speed.NORMAL);
            assertNotNull(playback.attack());
            playback.advance(0, UnitMotion.Speed.INSTANT);
            assertFalse(playback.busy());
            assertNull(playback.attack());
            assertSame(scene, playback.present(scene));
        }
        playback.accept(List.of(move(1)), scene, id -> false);
        playback.advance(.1, UnitMotion.Speed.NORMAL);
        playback.clear();
        assertFalse(playback.busy());
        assertTrue(playback.motions.isEmpty());
    }

    @Test
    void frameRateAndLargeFrameDeltasDoNotChangeQueueTiming() {
        var low = new UnitPlayback();
        var high = new UnitPlayback();
        var once = new UnitPlayback();
        var scene = scene(unit(1, 4), unit(2, 5));
        List<BoardScene.Animation> events = List.of(move(1), attack(unit(1, 4), unit(2, 5), ResolvedAttack.Kind.KICK, false), move(2));
        for (var playback : List.of(low, high, once)) {
            playback.accept(events, scene, id -> false);
            playback.advance(0, UnitMotion.Speed.NORMAL);
        }
        double seconds = once.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate
              + UnitPlayback.COMPLETION_HOLD_SECONDS + .4;
        for (int rate : new int[] { 30, 144 }) {
            var playback = rate == 30 ? low : high;
            int frames = (int) (seconds * rate);
            for (int i = 0; i < frames; i++) { playback.advance(1.0 / rate, UnitMotion.Speed.NORMAL); }
            playback.advance(seconds - frames / (double) rate, UnitMotion.Speed.NORMAL);
        }
        once.advance(seconds, UnitMotion.Speed.NORMAL);
        assertNotNull(once.attack());
        assertEquals(once.attack().seconds, low.attack().seconds, 1e-5);
        assertEquals(once.attack().seconds, high.attack().seconds, 1e-5);
        assertEquals(0, high.attack().impact(), "A miss must not generate a hit reaction");
    }

    @Test
    void queuedAttacksKeepOnlyTheAlreadyKnownVictimUntilPlaybackFinishes() {
        var attacker = unit(1, 4);
        var victim = unit(2, 5);
        var playback = new UnitPlayback();
        var removed = scene(attacker);
        playback.accept(List.of(attack(attacker, victim, ResolvedAttack.Kind.SHOT, true)), removed, id -> false);
        playback.advance(.5, UnitMotion.Speed.NORMAL);
        assertTrue(playback.present(removed).units().contains(victim));
        playback.advance(10, UnitMotion.Speed.NORMAL);
        assertSame(removed, playback.present(removed));
    }

    @Test
    void groupCompletionHoldWaitsForTheLastSuitToLand() {
        var armor = new BattleArmor();
        armor.setSquadSize(6);
        for (int i = 1; i <= 6; i++) { armor.initializeInternal(1, i); }
        var appearance = new BoardScene.UnitModel("", "", "", 6, 0, BoardScene.LocationDamage.NONE, UnitModelState.capture(armor));
        var unit = new BoardScene.Unit(10, -1, "Battle Armor", unit(10, 4).location(), null, false, null, 1, false, appearance, 0);
        var playback = new UnitPlayback();
        var scene = withRanges(scene(unit), "S");
        playback.accept(List.of(new BoardScene.Movement(10, 0, List.of(unit(10, 0).location(), unit.location()),
              EntityMovementType.MOVE_JUMP, 3, 3, unit)), scene, id -> false);
        playback.advance(0, UnitMotion.Speed.NORMAL);
        var motion = playback.motions.get(10);
        assertNotNull(motion.sample().group());
        playback.advance((motion.remainingSeconds() - UnitMotion.GROUP_START_JITTER_SECONDS
              - UnitMotion.FORMATION_SETTLE_SECONDS) * 2, UnitMotion.Speed.NORMAL);
        assertTrue(motion.isMoving());
        assertEquals(0, playback.holdSeconds());
        playback.advance(UnitMotion.GROUP_START_JITTER_SECONDS * 2, UnitMotion.Speed.NORMAL);
        assertTrue(motion.isMoving());
        assertEquals(0, playback.holdSeconds());
        assertTrue(playback.present(scene).rangeBorders().isEmpty(), "The final formation settling must also finish");
        assertTrue(playback.present(scene).rangeLabels().isEmpty());
        playback.advance(UnitMotion.FORMATION_SETTLE_SECONDS * 2, UnitMotion.Speed.NORMAL);
        assertFalse(motion.isMoving());
        assertEquals(1, playback.holdSeconds(), 1e-6);
        assertEquals(scene.rangeBorders(), playback.present(scene).rangeBorders());
        assertEquals(scene.rangeLabels(), playback.present(scene).rangeLabels());
    }

    @Test
    void queuedTransportDecisionUsesItsCapturedUnitInsteadOfTheLatestFormation() {
        var original = unit(1, 4);
        var changed = unit(1, 5);
        var target = unit(2, 6);
        var movement = new BoardScene.Movement(1, 0, List.of(unit(1, 0).location(), original.location()),
              EntityMovementType.MOVE_WALK, 0, 4, original);
        var playback = new UnitPlayback();
        AtomicReference<BoardScene.Unit> used = new AtomicReference<>();
        java.util.function.Predicate<BoardScene.Unit> transports = captured -> {
            used.set(captured);
            return captured == original;
        };
        playback.accept(List.of(attack(original, target, ResolvedAttack.Kind.SHOT, true), movement), scene(original, target), transports);
        playback.advance(0, UnitMotion.Speed.NORMAL);
        float duration = playback.attack().duration;
        playback.accept(List.of(), scene(changed, target), transports);
        playback.advance(duration / UnitMotion.Speed.NORMAL.rate + 1, UnitMotion.Speed.NORMAL);
        assertSame(original, used.get());
        assertNotNull(playback.motions.get(1).sample().boarding());
        assertEquals(UnitMotion.Stage.BOARD, playback.motions.get(1).sample().boarding().stage());
    }
}
