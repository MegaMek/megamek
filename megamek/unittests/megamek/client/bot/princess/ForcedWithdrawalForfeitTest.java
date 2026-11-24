/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #9595: a withdrawing bot unit that fires gives up its protection. Before this, a crippled Devastator that
 * was allowed to return fire shot at a player who could not shoot back without being marked dishonored.
 */
class ForcedWithdrawalForfeitTest {

    private static final int DEVASTATOR_ID = 10;
    private static final int HUNCHBACK_ID = 50;
    private static final int PLAYER_ID = 2;

    private Princess princess;
    private Game mockGame;
    private Tank devastator;
    private Entity hunchback;
    private final List<Entity> entitiesInGame = new ArrayList<>();
    private final Vector<Entity> entitiesOutOfGame = new Vector<>();

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        princess.getBehaviorSettings().setForcedWithdrawal(true);

        mockGame = mock(Game.class);
        doReturn(mockGame).when(princess).getGame();
        when(mockGame.getEntitiesVector()).thenReturn(entitiesInGame);
        when(mockGame.getOutOfGameEntitiesVector()).thenReturn(entitiesOutOfGame);
        List<Player> players = List.of(player(1), player(PLAYER_ID));
        when(mockGame.getPlayersList()).thenReturn(players);

        devastator = mock(Tank.class);
        when(devastator.getId()).thenReturn(DEVASTATOR_ID);
        when(devastator.getDisplayName()).thenReturn("Devastator Heavy Tank");
        when(devastator.getAttackedByThisTurn()).thenReturn(Set.of());
        when(mockGame.getEntity(DEVASTATOR_ID)).thenReturn(devastator);

        hunchback = mock(Entity.class);
        when(hunchback.getId()).thenReturn(HUNCHBACK_ID);
        when(hunchback.getOwnerId()).thenReturn(PLAYER_ID);
        when(hunchback.isMilitary()).thenReturn(true);
        when(hunchback.getDisplayName()).thenReturn("Hunchback HBK-4G");
        when(hunchback.getAttackedByThisTurn()).thenReturn(Set.of());
        when(mockGame.getEntity(HUNCHBACK_ID)).thenReturn(hunchback);

        entitiesInGame.add(devastator);
        entitiesInGame.add(hunchback);
        doReturn(List.of(devastator)).when(princess).getEntitiesOwned();

        // The Devastator was crippled and withdrawing when the turn began.
        princess.getMemory().setCrippledUnits(Set.of(DEVASTATOR_ID));
    }

    private static Player player(int id) {
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(id);
        return player;
    }

    @Test
    void aWithdrawingUnitThatFiresGivesUpItsProtection() {
        when(hunchback.getAttackedByThisTurn()).thenReturn(Set.of(DEVASTATOR_ID));

        princess.getForcedWithdrawalTracker().updateForfeitedProtection(Set.of(DEVASTATOR_ID));

        assertFalse(princess.getForcedWithdrawalTracker().isProtected(DEVASTATOR_ID));
        assertTrue(princess.buildHonorReport().withdrawingUnitIds().isEmpty(),
              "clients must stop tagging and warning about a unit that fired while withdrawing");
    }

    @Test
    void aWithdrawingUnitThatHoldsItsFireStaysProtected() {
        princess.getForcedWithdrawalTracker().updateForfeitedProtection(Set.of(DEVASTATOR_ID));

        assertTrue(princess.getForcedWithdrawalTracker().isProtected(DEVASTATOR_ID));
        assertEquals(Set.of(DEVASTATOR_ID), princess.buildHonorReport().withdrawingUnitIds());
    }

    @Test
    void aUnitCrippledThisTurnKeepsItsProtectionForShotsItFiredWhileHealthy() {
        // Attacks are declared at the same time, so the shots it fired this turn were fired by a healthy unit.
        when(hunchback.getAttackedByThisTurn()).thenReturn(Set.of(DEVASTATOR_ID));

        princess.getForcedWithdrawalTracker().updateForfeitedProtection(Set.of());

        assertTrue(princess.getForcedWithdrawalTracker().isProtected(DEVASTATOR_ID));
    }

    @Test
    void killingTheTargetStillCountsAsFiring() {
        entitiesInGame.remove(hunchback);
        entitiesOutOfGame.add(hunchback);
        when(hunchback.getAttackedByThisTurn()).thenReturn(Set.of(DEVASTATOR_ID));

        princess.getForcedWithdrawalTracker().updateForfeitedProtection(Set.of(DEVASTATOR_ID));

        assertFalse(princess.getForcedWithdrawalTracker().isProtected(DEVASTATOR_ID));
    }

    @Test
    void shootingAProtectedWithdrawingUnitDishonorsThePlayer() {
        when(devastator.getAttackedByThisTurn()).thenReturn(Set.of(HUNCHBACK_ID));

        princess.checkForDishonoredEnemies();

        assertEquals(List.of(PLAYER_ID), princess.resolveDishonoredPlayerIds());
    }

    @Test
    void shootingBackAtAUnitThatFiredWhileWithdrawingIsNoDishonor() {
        // Round 5: the Devastator fires on the Hunchback.
        when(hunchback.getAttackedByThisTurn()).thenReturn(Set.of(DEVASTATOR_ID));
        princess.getForcedWithdrawalTracker().updateForfeitedProtection(Set.of(DEVASTATOR_ID));

        // Round 6: the Hunchback fires back.
        when(hunchback.getAttackedByThisTurn()).thenReturn(Set.of());
        when(devastator.getAttackedByThisTurn()).thenReturn(Set.of(HUNCHBACK_ID));
        princess.checkForDishonoredEnemies();

        assertTrue(princess.resolveDishonoredPlayerIds().isEmpty());
    }

    @Test
    void returningToTheLobbyForgetsWhoGaveUpProtection() {
        // Unit IDs start again in the next game, so a kept list would strip a new unit of its protection.
        when(hunchback.getAttackedByThisTurn()).thenReturn(Set.of(DEVASTATOR_ID));
        princess.getForcedWithdrawalTracker().updateForfeitedProtection(Set.of(DEVASTATOR_ID));

        princess.getForcedWithdrawalTracker().forgetWithdrawingUnits();

        assertFalse(princess.getMemory().hasForfeitedProtection(DEVASTATOR_ID));
    }
}
