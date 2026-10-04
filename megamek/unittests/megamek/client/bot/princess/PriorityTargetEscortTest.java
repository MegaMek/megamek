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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A priority target is scored as at least as big a threat as the most dangerous enemy on the board, and pays no
 * overkill cost until it is already expected to be destroyed, so an unarmed convoy truck is not passed over for the
 * armed escort beside it (#9174).
 */
class PriorityTargetEscortTest {
    private static final double TOLERANCE = 0.0001;

    @Test
    void anUnarmedPriorityTargetCountsAsTheMostDangerousEnemy() {
        Entity truck = unit(3);
        Entity escort = unit(10);
        FireControl fireControl = fireControlAgainst(Set.of(3), truck, escort);
        doReturn(1.0).when(fireControl).calcTargetPotentialDamageMultiplier(truck);
        doReturn(1.85).when(fireControl).calcTargetPotentialDamageMultiplier(escort);

        assertEquals(1.85, fireControl.calcThreatMultiplier(truck), TOLERANCE);
        assertEquals(1.85, fireControl.calcThreatMultiplier(escort), TOLERANCE);
    }

    @Test
    void aTargetThatIsNoPriorityKeepsItsOwnThreat() {
        Entity truck = unit(3);
        Entity escort = unit(10);
        FireControl fireControl = fireControlAgainst(Set.of(), truck, escort);
        doReturn(1.0).when(fireControl).calcTargetPotentialDamageMultiplier(truck);
        doReturn(1.85).when(fireControl).calcTargetPotentialDamageMultiplier(escort);

        assertEquals(1.0, fireControl.calcThreatMultiplier(truck), TOLERANCE);
    }

    @Test
    void aPriorityTargetPaysNoOverkillCostUntilItIsAlreadyDoomed() {
        Entity truck = unit(3);
        Entity otherTruck = unit(4);
        FireControl fireControl = fireControlAgainst(Set.of(3), truck, otherTruck);
        doReturn(0.7).when(fireControl).calcDamageAllocationUtility(truck, 36.0);
        doReturn(0.7).when(fireControl).calcDamageAllocationUtility(otherTruck, 36.0);
        doReturn(100.0).when(fireControl).calcDamageAllocationUtility(truck, 5.0);

        assertEquals(0, fireControl.calcOverkillFraction(truck, 36.0), TOLERANCE);
        assertEquals(0.7, fireControl.calcOverkillFraction(otherTruck, 36.0), TOLERANCE, "not a priority target");
        assertEquals(100, fireControl.calcOverkillFraction(truck, 5.0), TOLERANCE, "already expected destroyed");
    }

    @Test
    void aPriorityOneTruckOutscoresTheArmedEscortNextToIt() {
        // 2026-10-04 playtest, round 7: a Masakari with a truck 11 hexes off and a Phoenix Hawk 5 hexes off shot the
        // Phoenix Hawk every time, because the escort's threat outweighed the truck's priority bonus.
        // Truck: (18 damage + 0.15 kill x 50) x threat 1.85 x (1 + 0.55 priority 1 bonus) = 73.1.
        // Escort: (26 damage + 0.4 criticals x 10) x threat 1.85 = 55.5.
        Entity truck = unit(3);
        Entity escort = unit(10);
        when(escort.isMilitary()).thenReturn(true);
        FireControl fireControl = fireControlAgainst(Set.of(3), truck, escort);
        when(fireControl.owner.getTargetPriority(3)).thenReturn(1);
        doReturn(1.0).when(fireControl).calcTargetPotentialDamageMultiplier(truck);
        doReturn(1.85).when(fireControl).calcTargetPotentialDamageMultiplier(escort);
        doReturn(0.0).when(fireControl).calcCommandUtility(any());
        doReturn(0.0).when(fireControl).calcDamageAllocationUtility(any(), anyDouble());
        FiringPlan truckPlan = plan(truck, 18.0, 0.0, 0.15);
        FiringPlan escortPlan = plan(escort, 26.0, 0.4, 0.0);

        fireControl.calculateUtility(truckPlan, 99, false);
        fireControl.calculateUtility(escortPlan, 99, false);

        double truckScore = capturedUtility(truckPlan);
        double escortScore = capturedUtility(escortPlan);
        assertEquals(73.1, truckScore, 0.1);
        assertEquals(55.5, escortScore, 0.1);
        assertTrue(truckScore > escortScore, "the priority 1 truck must outscore its escort");
    }

    private static Entity unit(int id) {
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(id);
        return unit;
    }

    private static FireControl fireControlAgainst(Set<Integer> priorityTargets, Entity... enemies) {
        Princess princess = mock(Princess.class);
        Game game = mock(Game.class);
        when(game.getRoundCount()).thenReturn(7);
        when(game.getPhase()).thenReturn(GamePhase.FIRING);
        when(princess.getGame()).thenReturn(game);
        when(princess.getPriorityUnitTargets()).thenReturn(priorityTargets);
        when(princess.getEnemyEntities()).thenReturn(List.of(enemies));
        return spy(new FireControl(princess));
    }

    private static FiringPlan plan(Entity target, double expectedDamage, double expectedCriticals,
          double killProbability) {
        FiringPlan plan = mock(FiringPlan.class);
        when(plan.getTarget()).thenReturn(target);
        when(plan.getExpectedDamage()).thenReturn(expectedDamage);
        when(plan.getExpectedCriticals()).thenReturn(expectedCriticals);
        when(plan.getKillProbability()).thenReturn(killProbability);
        return plan;
    }

    private static double capturedUtility(FiringPlan plan) {
        ArgumentCaptor<Double> utility = ArgumentCaptor.forClass(Double.class);
        verify(plan).setUtility(utility.capture());
        return utility.getValue();
    }
}
