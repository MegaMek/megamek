/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import javax.swing.SwingUtilities;

import megamek.common.Player;
import megamek.common.ResolvedAttack;
import megamek.common.event.GameAttackResolvedEvent;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Targetable;
import megamek.common.units.UnitLocation;
import org.junit.jupiter.api.Test;

class GpuCombatSourceTest {
    @Test
    void removedWrecksUseTheExistingBoardListAndRespectTheWreckPreference() throws Exception {
        var preferences = megamek.client.ui.clientGUI.GUIPreferences.getInstance();
        boolean show = preferences.getShowWrecks();
        try (var fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                preferences.setShowWrecks(true);
                fixture.entity.setDestroyed(true);
                fixture.game.removeEntity(fixture.entity.getId(), megamek.common.interfaces.IEntityRemovalConditions.REMOVE_SALVAGEABLE);
                fixture.view.redrawAllEntities();
                fixture.source.refresh();
            });
            var wrecks = fixture.source.takeFrame().scene().units();
            assertEquals(1, wrecks.size());
            assertTrue(wrecks.getFirst().model().state().pose().dead());
            assertEquals(null, wrecks.getFirst().annotations(), "Wrecks cannot retain live health bars");
            SwingUtilities.invokeAndWait(() -> { preferences.setShowWrecks(false); fixture.source.refresh(); });
            assertTrue(fixture.source.takeFrame().scene().units().isEmpty());
        } finally { preferences.setShowWrecks(show); }
    }

    @Test
    void capturedResultsAreImmutableAndDuplicatesDoNotReplay() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            var entity = fixture.entity;
            var location = new UnitLocation(entity.getId(), entity.getPosition(), entity.getFacing(), entity.getElevation(), 0);
            var result = new ResolvedAttack(UUID.randomUUID(), ResolvedAttack.Kind.SHOT, location, location,
                  Targetable.TYPE_ENTITY, 0, entity.getEquipment(0).getType().getInternalName(), 0, true);
            var event = new GameAttackResolvedEvent(this, result, entity, entity);
            SwingUtilities.invokeAndWait(() -> {
                fixture.game.processGameEvent(event);
                fixture.game.processGameEvent(event);
            });
            SwingUtilities.invokeAndWait(() -> { });
            var events = fixture.source.takeFrame().animations();
            assertEquals(1, events.size());
            var combat = (BoardScene.Combat) events.getFirst();
            assertEquals(location.coords(), combat.attacker().location().coords());
            assertFalse(combat.attacker().sensorContact());
            var oldModel = combat.attacker().model();
            SwingUtilities.invokeAndWait(() -> {
                entity.setFacing(3);
                entity.setPosition(entity.getPosition().translated(2));
                fixture.source.refresh();
            });
            assertEquals(location.coords(), combat.attacker().location().coords());
            assertEquals(oldModel, combat.attacker().model());
            assertTrue(fixture.source.takeFrame().animations().isEmpty());
        }
    }

    @Test
    void anEnemyHiddenWithoutDoubleBlindNeverBecomesACombatModel() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                var enemy = new Player(2, "Hidden");
                enemy.setTeam(2);
                fixture.game.addPlayer(2, enemy);
                fixture.entity.setOwner(enemy);
                fixture.entity.setHidden(true);
                var location = new UnitLocation(fixture.entity.getId(), fixture.entity.getPosition(), 0, 0, 0);
                var result = new ResolvedAttack(UUID.randomUUID(), ResolvedAttack.Kind.SHOT, location, location,
                      Targetable.TYPE_ENTITY, 0, "ISMediumLaser", 0, true);
                fixture.game.processGameEvent(new GameAttackResolvedEvent(this, result, fixture.entity, fixture.entity));
            });
            SwingUtilities.invokeAndWait(() -> { });
            assertTrue(fixture.source.takeFrame().animations().isEmpty());
        }
    }

    @Test
    void movementPaceCopiesTheGamesCapabilityAndAeroVelocity() {
        Entity unit = mock(Entity.class);
        when(unit.getWalkMP()).thenReturn(3);
        when(unit.getRunMP()).thenReturn(5);
        when(unit.getSprintMP()).thenReturn(6);
        when(unit.getJumpMP()).thenReturn(4);
        assertEquals(3, GpuBoardSource.movementMP(unit, EntityMovementType.MOVE_WALK));
        assertEquals(5, GpuBoardSource.movementMP(unit, EntityMovementType.MOVE_RUN));
        assertEquals(6, GpuBoardSource.movementMP(unit, EntityMovementType.MOVE_SPRINT));
        assertEquals(4, GpuBoardSource.movementMP(unit, EntityMovementType.MOVE_JUMP));
        var aero = mock(Aero.class);
        when(aero.isAirborne()).thenReturn(true);
        when(aero.getCurrentVelocity()).thenReturn(40);
        when(aero.getWalkMP()).thenReturn(3);
        assertEquals(40, GpuBoardSource.movementMP(aero, EntityMovementType.MOVE_SAFE_THRUST), "Velocity, not thrust");
    }
}
