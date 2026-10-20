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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Covers how target priority changes a bot's keenness to fire (#9174): priority 3 is the bonus every priority target
 * got before, and each step either side adds or takes off ten points. Enemy convoy units are always priority 1.
 */
class PriorityTargetUtilityTest {

    private static final double TOLERANCE = 0.0001;
    private static final int PHOENIX_HAWK = 9;
    private static final int SHERPA = 3;

    @Test
    void eachPriorityGivesItsBonus() {
        assertEquals(0.55, FireControl.priorityTargetUtility(1), TOLERANCE);
        assertEquals(0.35, FireControl.priorityTargetUtility(2), TOLERANCE);
        assertEquals(0.25, FireControl.priorityTargetUtility(3), TOLERANCE);
        assertEquals(0.15, FireControl.priorityTargetUtility(4), TOLERANCE);
        assertEquals(0.05, FireControl.priorityTargetUtility(5), TOLERANCE);
    }

    @Test
    void theDefaultPriorityPlaysExactlyAsPriorityTargetsAlwaysHave() {
        assertEquals(FireControl.PRIORITY_TARGET_UTILITY,
              FireControl.priorityTargetUtility(BehaviorSettings.DEFAULT_TARGET_PRIORITY), TOLERANCE);
    }

    @Test
    void fireControlUsesTheTargetsPriority() {
        Princess princess = mock(Princess.class);
        when(princess.getPriorityUnitTargets()).thenReturn(new HashSet<>(Set.of(PHOENIX_HAWK)));
        when(princess.getTargetPriority(PHOENIX_HAWK)).thenReturn(2);
        Entity phoenixHawk = mock(Entity.class);
        when(phoenixHawk.getId()).thenReturn(PHOENIX_HAWK);
        Entity locust = mock(Entity.class);
        when(locust.getId()).thenReturn(11);

        FireControl fireControl = new FireControl(princess);

        assertEquals(0.35, fireControl.calcPriorityUnitTargetUtility(phoenixHawk), TOLERANCE);
        assertEquals(0, fireControl.calcPriorityUnitTargetUtility(locust), TOLERANCE);
    }

    @Test
    void enemyConvoyUnitsArePriorityOneWhateverTheSettingsSay() {
        Princess princess = mock(Princess.class);
        doCallRealMethod().when(princess).getTargetPriority(anyInt());
        UnitOrdersFollower follower = mock(UnitOrdersFollower.class);
        when(follower.enemyConvoyUnitIds()).thenReturn(Set.of(SHERPA));
        when(princess.getUnitOrdersFollower()).thenReturn(follower);
        BehaviorSettings settings = new BehaviorSettings();
        settings.setPriorityUnit(SHERPA, 5);
        settings.setPriorityUnit(PHOENIX_HAWK, 4);
        when(princess.getBehaviorSettings()).thenReturn(settings);

        assertEquals(1, princess.getTargetPriority(SHERPA));
        assertEquals(4, princess.getTargetPriority(PHOENIX_HAWK));
    }
    /**
     * Whether the bot would shoot the priority target over an identical unit that is not one. The utility of a shot is
     * its expected damage, which already carries the odds of hitting, times the priority bonus, so for two identical
     * targets only the odds and the bonus differ.
     */
    private static boolean shootsPriorityTarget(int priority, int priorityTargetNumber, int otherTargetNumber) {
        double priorityShot = Compute.oddsAbove(priorityTargetNumber) * (1 + FireControl.priorityTargetUtility(priority));
        double otherShot = Compute.oddsAbove(otherTargetNumber);
        return priorityShot > otherShot;
    }

    @Test
    void priorityOneTipsAShotOneStepHarderButNeverTwo() {
        // The design rule: a priority target is the better choice when the odds are close, never a hunt.
        // 8+ against 7+ is one step; 9+ against 7+ is two.
        assertTrue(shootsPriorityTarget(1, 7, 6));
        assertTrue(shootsPriorityTarget(1, 8, 7));
        assertTrue(shootsPriorityTarget(1, 9, 8));
        assertFalse(shootsPriorityTarget(1, 9, 7));
        assertFalse(shootsPriorityTarget(1, 10, 8));
    }

    @Test
    void priorityOneOutranksAnEnemyCommanderAtTheSameOdds() {
        // The bot adds a bonus of its own for any enemy commander, C3 master or initiative leader; a unit the player
        // marked as priority 1 must still come first.
        double priorityOneShot = 1 + FireControl.priorityTargetUtility(1);
        double commanderShot = 1 + FireControl.COMMANDER_UTILITY;

        assertTrue(priorityOneShot > commanderShot);
        assertTrue((1 + FireControl.priorityTargetUtility(2)) < commanderShot);
    }

    @Test
    void theDefaultPriorityOnlyTipsTheEasiestStep() {
        assertTrue(shootsPriorityTarget(3, 7, 6));
        assertFalse(shootsPriorityTarget(3, 8, 7));
    }

    @Test
    void priorityFiveOnlyBreaksTies() {
        assertTrue(shootsPriorityTarget(5, 7, 7));
        assertFalse(shootsPriorityTarget(5, 7, 6));
    }
}
