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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.actions.EntityAction;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Princess used to throw when nothing was eligible to fire, and the handler meant to recover from that threw on
 * the same null, so the bot sent no attack data at all and stood mute for the rest of the phase (issue #8936).
 * {@code getEntityToFire} never throws: it falls back to the game's first eligible entity, which is itself
 * {@code null} when the turn list and the fire control state disagree about which units can still act.
 */
class PrincessNoEligibleShooterTest {

    private static final int FALLBACK_ENTITY_ID = 7;

    @Test
    void firingTurnWithNoEligibleShooterSendsAnEmptyAttack() {
        Princess princess = mock(Princess.class);
        Game game = mock(Game.class);
        GameTurn turn = mock(GameTurn.class);

        doCallRealMethod().when(princess).calculateFiringTurn();
        when(princess.getEntityToFire(nullable(FireControlState.class))).thenReturn(null);
        when(princess.getGame()).thenReturn(game);
        when(princess.getMyTurn()).thenReturn(turn);
        when(game.getFirstEntityNum(turn)).thenReturn(FALLBACK_ENTITY_ID);

        assertDoesNotThrow(princess::calculateFiringTurn);

        ArgumentCaptor<Vector<EntityAction>> attacks = ArgumentCaptor.forClass(Vector.class);
        verify(princess).sendAttackData(eq(FALLBACK_ENTITY_ID), attacks.capture());
        assertTrue(attacks.getValue().isEmpty(), "the turn must be cleared with an empty attack");
    }

    /**
     * When the game has no entity to fall back on either, there is nothing to clear the turn with, so the bot
     * must simply skip the turn rather than send an attack for {@link Entity#NONE}.
     */
    @Test
    void firingTurnWithNoFallbackEntitySendsNothing() {
        Princess princess = mock(Princess.class);
        Game game = mock(Game.class);
        GameTurn turn = mock(GameTurn.class);

        doCallRealMethod().when(princess).calculateFiringTurn();
        when(princess.getEntityToFire(nullable(FireControlState.class))).thenReturn(null);
        when(princess.getGame()).thenReturn(game);
        when(princess.getMyTurn()).thenReturn(turn);
        when(game.getFirstEntityNum(turn)).thenReturn(Entity.NONE);

        assertDoesNotThrow(princess::calculateFiringTurn);

        verify(princess, never()).sendAttackData(anyInt(), nullable(Vector.class));
    }

    /**
     * The targeting turn had the same shape with no guard at all, dereferencing the game's first entity straight
     * away.
     */
    @Test
    void targetingTurnWithNoEligibleEntityIsSkipped() {
        Princess princess = mock(Princess.class);
        Game game = mock(Game.class);
        GameTurn turn = mock(GameTurn.class);

        doCallRealMethod().when(princess).calculateTargetingOffBoardTurn();
        when(princess.getGame()).thenReturn(game);
        when(princess.getMyTurn()).thenReturn(turn);
        when(game.getFirstEntity(turn)).thenReturn(null);

        assertDoesNotThrow(princess::calculateTargetingOffBoardTurn);

        verify(princess, never()).sendAttackData(anyInt(), nullable(Vector.class));
        verify(princess).sendDone(true);
    }

    /**
     * {@code getEntityToFire} can also throw. A plain return there is treated as success by
     * {@code BotClient.calculateMyTurnWorker}, so the turn has to be cleared the same way.
     */
    @Test
    void firingTurnWhereFindingAShooterThrowsStillClearsTheTurn() {
        Princess princess = mock(Princess.class);
        Game game = mock(Game.class);
        GameTurn turn = mock(GameTurn.class);

        doCallRealMethod().when(princess).calculateFiringTurn();
        when(princess.getEntityToFire(nullable(FireControlState.class)))
              .thenThrow(new IllegalStateException("no fire control state"));
        when(princess.getGame()).thenReturn(game);
        when(princess.getMyTurn()).thenReturn(turn);
        when(game.getFirstEntityNum(turn)).thenReturn(FALLBACK_ENTITY_ID);

        assertDoesNotThrow(princess::calculateFiringTurn);

        ArgumentCaptor<Vector<EntityAction>> attacks = ArgumentCaptor.forClass(Vector.class);
        verify(princess).sendAttackData(eq(FALLBACK_ENTITY_ID), attacks.capture());
        assertTrue(attacks.getValue().isEmpty(), "the turn must be cleared with an empty attack");
    }
}
