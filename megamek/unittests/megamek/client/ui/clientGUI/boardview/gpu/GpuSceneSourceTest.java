/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.Vector;
import javax.swing.SwingUtilities;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.ResolvedAttack;
import megamek.common.board.Coords;
import megamek.common.event.GameAttackResolvedEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Targetable;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;

class GpuSceneSourceTest {
    @Test
    void hullDownPacketsRetainTheStartingPoseUntilTheQueuedTransition() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var initial = fixture.source.takeFrame();
            SwingUtilities.invokeAndWait(() -> {
                var at = fixture.entity.getPosition();
                fixture.entity.setHullDown(true);
                fixture.entity.moved = EntityMovementType.MOVE_WALK;
                var path = new Vector<>(List.of(
                      new UnitLocation(1, at, 0, 0, 0, megamek.common.units.ProneCause.NONE, null, null, false),
                      new UnitLocation(1, at, 0, 0, 0, megamek.common.units.ProneCause.NONE, null, null, true)));
                fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fixture.entity, path));
            });
            var frame = fixture.source.takeFrame();
            var movement = (BoardScene.Movement) frame.animations().getFirst();
            assertEquals(false, movement.path().getFirst().hullDown());
            assertEquals(true, movement.path().getLast().hullDown());
            var playback = new UnitPlayback();
            playback.accept(initial.timeline(), initial.scene(), ignored -> false);
            playback.accept(frame.timeline(), frame.scene(), ignored -> false);
            playback.togglePaused();
            playback.advance(100, UnitMotion.Speed.NORMAL);
            assertEquals(false, playback.present(frame.scene()).units().getFirst().location().hullDown());
            playback.togglePaused();
            playback.advance(UnitMotion.POSTURE_SECONDS / (2 * UnitMotion.Speed.NORMAL.rate), UnitMotion.Speed.NORMAL);
            assertEquals(.5, playback.motions.get(1).sample().posture().kneel(), .001);
            playback.advance(0, UnitMotion.Speed.INSTANT);
            assertEquals(true, playback.present(frame.scene()).units().getFirst().location().hullDown());
        }
    }

    @Test
    void sensorChangesPublishCheckpointsWithoutInventingVisibleMovement() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            fixture.source.takeFrame();
            SwingUtilities.invokeAndWait(() -> {
                var enemy = new Player(2, "Enemy");
                enemy.setTeam(2);
                fixture.game.addPlayer(enemy.getId(), enemy);
                fixture.entity.setOwner(enemy);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
                fixture.game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS).setValue(true);
                fixture.entity.addBeenDetectedBy(fixture.player);
                move(fixture, fixture.entity.getPosition(), new Coords(6, 5));
            });
            var frame = fixture.source.takeFrame();
            assertTrue(frame.movements().isEmpty());
            assertTrue(frame.scene().units().getFirst().sensorContact());
            assertEquals(new Coords(6, 5), frame.scene().units().getFirst().location().coords());
            assertTrue(frame.timeline().getLast() instanceof BoardScene.SceneUpdate);
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setHidden(true);
                move(fixture, fixture.entity.getPosition(), new Coords(7, 5));
            });
            frame = fixture.source.takeFrame();
            assertTrue(frame.movements().isEmpty());
            assertTrue(frame.scene().units().isEmpty());
            assertTrue(frame.timeline().getLast() instanceof BoardScene.SceneUpdate);
        }
    }

    @Test
    void batchedMovementPacketsKeepTheirOwnArrivalSnapshots() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var initial = fixture.source.takeFrame();
            var start = fixture.entity.getPosition();
            var middle = new Coords(6, 5);
            var end = new Coords(7, 5);
            var changed = new Coords(2, 2);
            SwingUtilities.invokeAndWait(() -> {
                // MULTI_PACKET dispatches consecutive entity changes in the same Swing callback.
                fixture.game.getBoard().setHex(changed, new Hex(1));
                move(fixture, start, middle);
                fixture.game.getBoard().setHex(changed, new Hex(2));
                move(fixture, middle, end);
            });
            var frame = fixture.source.takeFrame();
            var updates = frame.timeline().stream().filter(BoardScene.SceneUpdate.class::isInstance)
                  .map(BoardScene.SceneUpdate.class::cast).toList();
            assertEquals(2, frame.movements().size());
            assertEquals(middle, frame.movements().getFirst().unit().location().coords());
            assertEquals(end, frame.movements().getLast().unit().location().coords());
            assertEquals(1, updates.getFirst().scene().tile(changed).elevation());
            assertEquals(2, updates.getLast().scene().tile(changed).elevation());
            var playback = new UnitPlayback();
            playback.accept(initial.timeline(), initial.scene(), ignored -> false);
            playback.accept(frame.timeline(), frame.scene(), ignored -> false);
            assertEquals(initial.scene().tile(changed), playback.present(frame.scene()).tile(changed));
            playback.advance(0, UnitMotion.Speed.NORMAL);
            playback.advance(playback.motions.get(1).remainingSeconds() / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
            assertEquals(1, playback.present(frame.scene()).tile(changed).elevation());
            playback.advance(100, UnitMotion.Speed.NORMAL);
            assertEquals(2, playback.present(frame.scene()).tile(changed).elevation());
            assertTrue(fixture.source.takeFrame().animations().isEmpty(), "Received movements are consumed once");
        }
    }

    @Test
    void batchedAttacksCaptureBoardChangesBetweenResultsBeforeAnotherSwingRefresh() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var initial = fixture.source.takeFrame();
            var changed = new Coords(2, 2);
            SwingUtilities.invokeAndWait(() -> {
                attack(fixture);
                fixture.game.getBoard().setHex(changed, new Hex(1));
                // Damage to the board arrives before the next resolved attack packet.
                attack(fixture);
                fixture.game.getBoard().setHex(changed, new Hex(2));
                fixture.source.refresh();
            });
            var frame = fixture.source.takeFrame();
            assertEquals(2, frame.animations().size());
            var playback = new UnitPlayback();
            playback.accept(initial.timeline(), initial.scene(), ignored -> false);
            playback.accept(frame.timeline(), frame.scene(), ignored -> false);
            playback.advance(0, UnitMotion.Speed.NORMAL);
            assertEquals(initial.scene().tile(changed), playback.present(frame.scene()).tile(changed));
            var first = playback.attack();
            playback.advance((first.contactSeconds + .001) / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
            assertEquals(1, playback.present(frame.scene()).tile(changed).elevation());
            playback.advance((first.duration - first.seconds) / UnitMotion.Speed.NORMAL.rate + UnitPlayback.COMPLETION_HOLD_SECONDS,
                  UnitMotion.Speed.NORMAL);
            assertEquals(1, playback.present(frame.scene()).tile(changed).elevation());
            playback.advance((playback.attack().contactSeconds + .001) / UnitMotion.Speed.NORMAL.rate, UnitMotion.Speed.NORMAL);
            assertEquals(2, playback.present(frame.scene()).tile(changed).elevation());
        }
    }

    static void move(GpuBoardFixture fixture, Coords from, Coords to) {
        fixture.entity.moved = EntityMovementType.MOVE_WALK;
        fixture.entity.setPosition(to);
        var path = new Vector<>(List.of(new UnitLocation(1, from, 0, 0, 0), new UnitLocation(1, to, 0, 0, 0)));
        fixture.game.fireGameEvent(new GameEntityChangeEvent(fixture.game, fixture.entity, path));
    }

    private static void attack(GpuBoardFixture fixture) {
        var entity = fixture.entity;
        var location = new UnitLocation(entity.getId(), entity.getPosition(), entity.getFacing(), entity.getElevation(), 0);
        var result = new ResolvedAttack(UUID.randomUUID(), ResolvedAttack.Kind.SHOT, location, location,
              Targetable.TYPE_ENTITY, 0, "ISMediumLaser", 0, true);
        fixture.game.processGameEvent(new GameAttackResolvedEvent(fixture.game, result, entity, entity));
    }
}
