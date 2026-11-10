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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a retreating enemy convoy unit still counts as a target when the bot looks for the closest enemy to move
 * against. Regression for #9269: the search read only the priority targets in the behavior settings, which leave out
 * convoy units, so a withdrawing convoy truck was skipped while fire control still treated it as a priority target.
 */
class ConvoyClosestEnemyTest {
    private static final int CONVOY_TRUCK_ID = 12;
    private static final int ESCORT_ID = 20;
    private static final Coords ORIGIN = new Coords(0, 0);

    private Princess princess;
    private Game game;
    private Entity convoyTruck;
    private Entity escort;
    private Entity me;

    @BeforeEach
    void setUp() {
        BehaviorSettings behaviorSettings = mock(BehaviorSettings.class);
        when(behaviorSettings.getPriorityUnitTargets()).thenReturn(new HashSet<>());
        when(behaviorSettings.getIgnoredUnitTargets()).thenReturn(new HashSet<>());
        when(behaviorSettings.getNumberOfEnemiesToConsiderFacing()).thenReturn(1);

        UnitOrdersFollower ordersFollower = mock(UnitOrdersFollower.class);
        when(ordersFollower.enemyConvoyUnitIds()).thenReturn(Set.of(CONVOY_TRUCK_ID));

        // the convoy truck is withdrawing; the escort is not
        IHonorUtil honorUtil = mock(IHonorUtil.class);
        when(honorUtil.isEnemyBroken(eq(CONVOY_TRUCK_ID), anyInt(), anyBoolean())).thenReturn(true);

        convoyTruck = enemy(CONVOY_TRUCK_ID, new Coords(0, 3));
        escort = enemy(ESCORT_ID, new Coords(0, 10));
        me = mock(BipedMek.class);

        princess = mock(Princess.class);
        when(princess.getBehaviorSettings()).thenReturn(behaviorSettings);
        when(princess.getUnitOrdersFollower()).thenReturn(ordersFollower);
        when(princess.getHonorUtil()).thenReturn(honorUtil);
        when(princess.getEnemyEntities()).thenReturn(List.of(convoyTruck, escort));
        when(princess.getPriorityUnitTargets()).thenCallRealMethod();

        game = mock(Game.class);
        when(game.onTheSameBoard(any(Targetable.class), any(Targetable.class))).thenReturn(true);
    }

    private static Entity enemy(int id, Coords position) {
        Entity enemy = mock(Tank.class);
        when(enemy.getId()).thenReturn(id);
        when(enemy.getPosition()).thenReturn(position);
        when(enemy.isSelectableThisTurn()).thenReturn(false);
        return enemy;
    }

    @Test
    void groundRankerMovesAgainstARetreatingConvoyUnit() {
        BasicPathRanker ranker = new BasicPathRanker(princess);

        assertEquals(convoyTruck, ranker.findClosestEnemy(me, ORIGIN, game, false));
    }

    @Test
    void spaceRankerMovesAgainstARetreatingConvoyUnit() {
        NewtonianAerospacePathRanker ranker = new NewtonianAerospacePathRanker(princess);

        assertEquals(convoyTruck, ranker.findClosestEnemy(me, ORIGIN, game));
    }
}
