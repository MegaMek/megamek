/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static megamek.client.ui.clientGUI.boardview.gpu.UnitPlaybackTest.attack;
import static megamek.client.ui.clientGUI.boardview.gpu.UnitPlaybackTest.scene;
import static megamek.client.ui.clientGUI.boardview.gpu.UnitPlaybackTest.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import megamek.common.ResolvedAttack;
import org.junit.jupiter.api.Test;

class UnitVolleyTest {
    @Test
    void observedArtilleryLandingReplacesCosmeticMissOffsets() {
        var base = attack(unit(1, 0), unit(2, 2), ResolvedAttack.Kind.SHOT, false);
        var result = base.result();
        var landing = new megamek.common.units.UnitLocation(-1, new megamek.common.board.Coords(4, 5), 0, 3, 0);
        var shot = new ResolvedAttack.Shot("Indirect", Set.of("M_SMOKE"), true, false, 1, 0, true)
              .withTrajectory(result.attacker(), landing);
        var resolved = new ResolvedAttack(result.id(), result.kind(), result.attacker(), result.target(), result.targetType(),
              result.equipmentIndex(), result.equipmentName(), result.limb(), false, result.mounts(), shot);
        var animation = new UnitAttack(new BoardScene.Combat(resolved, base.attacker(), base.target(), base.destination()));
        var origin = BoardGeometry.center(base.attacker().location().coords(), 0);
        assertEquals(BoardGeometry.center(landing.coords(), landing.elevation()),
              animation.endpoint(null, origin, new com.badlogic.gdx.math.Vector3()));
        var halfway = UnitAttack.projectile(origin, animation.endpoint(null, origin, new com.badlogic.gdx.math.Vector3()),
              .5f, true, new com.badlogic.gdx.math.Vector3());
        assertTrue(halfway.z > BoardGeometry.LEVEL * 1.5f);
    }

    @Test
    void counterfireJoinsIncomingWeaponsAndSoundEdgesPauseAndSkipWithTheSameClock() {
        var shooter = unit(1, 0);
        var defender = unit(2, 2);
        var base = attack(defender, shooter, ResolvedAttack.Kind.SHOT, true);
        var result = base.result();
        var defense = new ResolvedAttack.Shot("On", Set.of(), false, true, 1, 0, false);
        var counter = new BoardScene.Combat(new ResolvedAttack(result.id(), result.kind(), result.attacker(), result.target(),
              result.targetType(), result.equipmentIndex(), result.equipmentName(), result.limb(), true, result.mounts(), defense),
              defender, shooter, shooter.location());
        var cues = new java.util.ArrayList<Boolean>();
        var playback = new UnitPlayback(ignored -> { }, (shot, contact) -> cues.add(contact));
        var scene = scene(shooter, defender);
        playback.accept(List.of(counter, attack(shooter, defender, ResolvedAttack.Kind.SHOT, true)), scene, ignored -> false);
        playback.advance(.6, UnitMotion.Speed.NORMAL);
        assertEquals(2, playback.attacks().size());
        playback.togglePaused();
        int count = cues.size();
        playback.advance(10, UnitMotion.Speed.NORMAL);
        assertEquals(count, cues.size());
        playback.togglePaused();
        playback.advance(10, UnitMotion.Speed.NORMAL);
        assertEquals(2, cues.stream().filter(Boolean::booleanValue).count());
        assertEquals(2, cues.stream().filter(value -> !value).count());
        var defensive = new UnitAttack(counter);
        defensive.seconds = defensive.contactSeconds;
        assertEquals(0, defensive.impact(), "Counterfire cannot rock or damage the attacking unit");
        var start = BoardGeometry.center(defender.location().coords(), 0);
        var endpoint = defensive.endpoint(null, start, new com.badlogic.gdx.math.Vector3());
        assertTrue(start.dst(endpoint) < start.dst(BoardGeometry.center(shooter.location().coords(), 0)) * .5f);
        playback.accept(List.of(counter), scene, ignored -> false);
        playback.advance(0, UnitMotion.Speed.INSTANT);
        assertEquals(4, cues.size(), "Skipping never plays sounds for unseen historical shots");
    }

    @Test
    void aCasualtyBetweenWeaponResultsDoesNotSerializeTheVolley() {
        var attacker = unit(1, 0);
        var victim = unit(2, 2);
        var other = unit(3, 3);
        var playback = new UnitPlayback();
        var finalScene = scene(attacker, other);
        playback.accept(List.of(new BoardScene.SceneUpdate(scene(attacker, victim, other)),
              attack(attacker, victim, ResolvedAttack.Kind.SHOT, true),
              attack(victim, victim, ResolvedAttack.Kind.DEATH, true),
              new BoardScene.SceneUpdate(finalScene), attack(attacker, other, ResolvedAttack.Kind.SHOT, true),
              new BoardScene.SceneUpdate(finalScene)), finalScene, ignored -> false);
        playback.advance(0, UnitMotion.Speed.NORMAL);
        assertEquals(2, playback.attacks().size());
        double duration = playback.attacks().stream().mapToDouble(shot -> shot.duration + shot.delay).max().orElseThrow();
        playback.advance(duration / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
        assertTrue(playback.present(finalScene).units().contains(victim), "Keep the victim through the volley's recovery and hold");
        playback.advance(UnitPlayback.COMPLETION_HOLD_SECONDS, UnitMotion.Speed.NORMAL);
        assertTrue(playback.attack().death());
        assertTrue(playback.present(finalScene).units().contains(victim));
        playback.advance(UnitAttack.DEATH_SECONDS / UnitMotion.Speed.NORMAL.rate + 1.001, UnitMotion.Speed.NORMAL);
        assertFalse(playback.busy());
        assertEquals(finalScene.units(), playback.present(finalScene).units());
    }

    @Test
    void targetPassesShareOneHoldAndDamageWaitsForTheLastImpactAtEverySpeed() {
        var attacker = unit(1, 0);
        var victim = unit(2, 2);
        var distant = unit(3, 12);
        var before = scene(attacker, victim, distant);
        var after = scene(attacker);
        for (var speed : List.of(UnitMotion.Speed.HALF, UnitMotion.Speed.NORMAL, UnitMotion.Speed.DOUBLE, UnitMotion.Speed.QUADRUPLE)) {
            var playback = new UnitPlayback();
            playback.accept(List.of(new BoardScene.SceneUpdate(before), attack(attacker, victim, ResolvedAttack.Kind.SHOT, true),
                  new BoardScene.SceneUpdate(scene(attacker, distant)), attack(attacker, distant, ResolvedAttack.Kind.SHOT, true),
                  new BoardScene.SceneUpdate(after)), after, ignored -> false);
            playback.advance(0, speed);
            assertEquals(2, playback.attacks().size());
            var shots = List.copyOf(playback.attacks());
            assertEquals(shots.getFirst().contactSeconds + UnitVolley.TARGET_SWITCH_SECONDS,
                  shots.get(1).delay + UnitAttack.ANTICIPATION_SECONDS, 1e-5,
                  "Different targets require a turn before the next shot");
            double impact = shots.stream().mapToDouble(shot -> shot.delay + shot.contactSeconds).max().orElseThrow();
            double end = shots.stream().mapToDouble(shot -> shot.delay + shot.duration).max().orElseThrow();
            playback.advance((impact - .001) / speed.rate, speed);
            assertEquals(3, playback.present(after).units().size(), "Later damage must not appear during the volley");
            playback.advance(.002 / speed.rate, speed);
            assertEquals(1, playback.present(after).units().size());
            playback.advance((end - impact - .001) / speed.rate, speed);
            assertEquals(1, playback.holdSeconds(), 1e-5);
            playback.advance(.999, speed);
            assertTrue(playback.busy());
            playback.advance(.002, speed);
            assertFalse(playback.busy(), "There is one hold for the volley, not one per gun");
        }
    }

    @Test
    void interleavedTargetsGroupTogetherAndLatePacketsNeverRetimeAnAlreadyStartedPass() {
        var source = unit(1, 0);
        var a = unit(2, 2);
        var b = unit(3, 3);
        var c = unit(4, 4);
        var scene = scene(source, a, b, c);
        var playback = new UnitPlayback();
        playback.accept(List.of(attack(source, a, ResolvedAttack.Kind.SHOT, true),
              attack(source, b, ResolvedAttack.Kind.SHOT, true), attack(source, a, ResolvedAttack.Kind.SHOT, false),
              attack(source, c, ResolvedAttack.Kind.SHOT, true), attack(source, b, ResolvedAttack.Kind.SHOT, false)), scene, ignored -> false);
        playback.advance(0, UnitMotion.Speed.DOUBLE);
        var shots = List.copyOf(playback.attacks());
        assertSame(shots.get(0).group, shots.get(2).group);
        assertSame(shots.get(1).group, shots.get(4).group);
        assertTrue(Math.abs(shots.get(0).delay - shots.get(2).delay) <= UnitPlayback.VOLLEY_JITTER_SECONDS);
        assertEquals(shots.get(0).group.fireEnd(), shots.get(1).group.start, 1e-5);
        assertEquals(shots.get(1).group.fireEnd(), shots.get(3).group.start, 1e-5);
        float started = shots.get(1).delay;
        playback.advance(started + UnitAttack.ANTICIPATION_SECONDS + .01, UnitMotion.Speed.DOUBLE);
        playback.accept(List.of(attack(source, a, ResolvedAttack.Kind.SHOT, true)), scene, ignored -> false);
        var late = playback.attacks().getLast();
        assertEquals(started, shots.get(1).delay, "A launched projectile keeps its clock");
        assertTrue(late.group.start >= shots.get(3).group.fireEnd(), "Late fire at an old target gets a new pass");
        playback.togglePaused();
        float clock = late.group.clock;
        playback.advance(10, UnitMotion.Speed.DOUBLE);
        assertEquals(clock, late.group.clock);
        playback.advance(0, UnitMotion.Speed.INSTANT);
        assertTrue(playback.attacks().isEmpty());
    }

    @Test
    void secondTargetsCounterfireWaitsForItsIncomingPass() {
        var source = unit(1, 0);
        var a = unit(2, 2);
        var b = unit(3, 3);
        var counter = attack(b, source, ResolvedAttack.Kind.SHOT, true);
        var raw = counter.result();
        var defense = new ResolvedAttack.Shot("On", Set.of(), false, true, 1, 0, false);
        counter = new BoardScene.Combat(new ResolvedAttack(raw.id(), raw.kind(), raw.attacker(), raw.target(), raw.targetType(),
              raw.equipmentIndex(), raw.equipmentName(), raw.limb(), true, raw.mounts(), defense), b, source, source.location());
        var playback = new UnitPlayback();
        playback.accept(List.of(attack(source, a, ResolvedAttack.Kind.SHOT, true), counter,
              attack(source, b, ResolvedAttack.Kind.SHOT, true)), scene(source, a, b), ignored -> false);
        playback.advance(0, UnitMotion.Speed.DOUBLE);
        var shots = playback.attacks();
        assertEquals(shots.get(2).group.start, shots.get(1).group.start, 1e-5);
        assertTrue(shots.get(1).delay >= shots.getFirst().contactSeconds);
    }

    @Test
    void lateShotsJoinTheVolleyAndPauseInstantAndPhysicalBarriersStayConsistent() {
        var attacker = unit(1, 0);
        var victim = unit(2, 2);
        var scene = scene(attacker, victim);
        var playback = new UnitPlayback();
        playback.accept(List.of(attack(attacker, victim, ResolvedAttack.Kind.SHOT, true)), scene, ignored -> false);
        playback.advance(.7, UnitMotion.Speed.NORMAL);
        playback.accept(List.of(attack(attacker, victim, ResolvedAttack.Kind.SHOT, false)), scene, ignored -> false);
        assertEquals(2, playback.attacks().size());
        var late = playback.attacks().getLast();
        assertTrue(late.seconds <= 0, "A packet received after launch still gets its complete animation");
        playback.togglePaused();
        float frozen = late.seconds;
        playback.advance(20, UnitMotion.Speed.DOUBLE);
        assertEquals(frozen, late.seconds);
        playback.togglePaused();
        var kick = attack(attacker, victim, ResolvedAttack.Kind.KICK, true);
        playback.accept(List.of(kick, attack(attacker, victim, ResolvedAttack.Kind.SHOT, true)), scene, ignored -> false);
        assertEquals(2, playback.attacks().size());
        double end = late.delay + late.duration;
        playback.advance((end - .35) / UnitMotion.Speed.NORMAL.rate + 1, UnitMotion.Speed.NORMAL);
        assertSame(kick, playback.attack().event);
        playback.advance(0, UnitMotion.Speed.INSTANT);
        assertTrue(playback.attacks().isEmpty());
        assertFalse(playback.busy());
        playback.clear();
        assertTrue(playback.attacks().isEmpty());
    }

    @Test
    void partialResultsKeepExactCountsIncludingLogicalGroupsAndUnknownAttackValues() {
        var base = attack(unit(1, 0), unit(2, 2), ResolvedAttack.Kind.SHOT, true);
        var result = base.result();
        var configuration = new ResolvedAttack.Shot("Indirect", Set.of("M_STANDARD"), false, false, 1, 20, true);
        for (int hits : List.of(0, 1, 12, 20, 27, 40)) {
            var resolved = new ResolvedAttack(result.id(), result.kind(), result.attacker(), result.target(), result.targetType(),
                  0, "LRM20", 0, true, List.of(new ResolvedAttack.Mount(11, 2, configuration),
                        new ResolvedAttack.Mount(12, 5, configuration)), configuration.withResolution(null, hits));
            var shot = new UnitAttack(new BoardScene.Combat(resolved, base.attacker(), base.target(), base.destination()));
            assertEquals(hits, shot.missileHits(11, 2, 20) + shot.missileHits(12, 5, 20));
        }
        var unknown = new UnitAttack(base);
        assertEquals(20, unknown.missileHits(1, 0, 20), "Do not invent a cluster roll when the server has no missile count");
        var miss = new UnitAttack(attack(unit(1, 0), unit(2, 2), ResolvedAttack.Kind.SHOT, false));
        assertEquals(0, miss.missileHits(1, 0, 20));
    }
}
