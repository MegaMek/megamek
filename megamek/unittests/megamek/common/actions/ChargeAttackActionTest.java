/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ChargeAttackAction#getDamageTakenBy(Entity, Entity, boolean, int)}.
 * <p>
 * Per Total Warfare p.148, DropShips are "unusual targets" for charge attacks. When charging a DropShip, self-damage
 * should be calculated using the attacker's weight, not the DropShip's weight.
 *
 * @see <a href="https://github.com/MegaMek/megamek/issues/7182">GitHub Issue #7182</a>
 */
class ChargeAttackActionTest {

    /**
     * Creates a mock Entity with the specified weight and wet status.
     */
    private Entity createMockEntity(double weight, boolean isWet) {
        Entity entity = mock(Entity.class);
        when(entity.getWeight()).thenReturn(weight);
        when(entity.getLocationStatus(1)).thenReturn(
              isWet ? ILocationExposureStatus.WET : ILocationExposureStatus.NORMAL);
        return entity;
    }

    /**
     * Creates a mock Mek with the specified weight and wet status.
     */
    private Mek createMockMek(double weight, boolean isWet) {
        Mek mek = mock(Mek.class);
        when(mek.getWeight()).thenReturn(weight);
        when(mek.getLocationStatus(1)).thenReturn(
              isWet ? ILocationExposureStatus.WET : ILocationExposureStatus.NORMAL);
        return mek;
    }

    /**
     * Creates a mock Dropship with the specified weight.
     */
    private Dropship createMockDropship(double weight) {
        Dropship dropship = mock(Dropship.class);
        when(dropship.getWeight()).thenReturn(weight);
        return dropship;
    }

    @Nested
    @DisplayName("Standard Formula (tacOps=false)")
    class StandardFormulaTests {

        @Test
        @DisplayName("Mek vs Mek: uses target weight")
        void testMekVsMek_UsesTargetWeight() {
            // 50-ton Mek charges 75-ton Mek
            // Expected: ceil(75 / 10) = 8 damage
            Entity attacker = createMockMek(50, false);
            Entity target = createMockMek(75, false);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, false, 0);

            assertEquals(8, damage, "Mek vs Mek should use target's weight: ceil(75/10) = 8");
        }

        @Test
        @DisplayName("Mek vs DropShip: uses attacker weight (unusual target rule)")
        void testMekVsDropship_UsesAttackerWeight() {
            // 50-ton Mek charges 3500-ton DropShip
            // Per TW p.148, DropShips are "unusual targets" - use attacker's weight
            // Expected: ceil(50 / 10) = 5 damage (NOT ceil(3500/10) = 350!)
            Entity attacker = createMockMek(50, false);
            Dropship target = createMockDropship(3500);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, false, 0);

            assertEquals(5, damage,
                  "Mek vs DropShip should use attacker's weight per TW p.148: ceil(50/10) = 5");
        }

        @Test
        @DisplayName("100-ton Mek vs DropShip: uses attacker weight")
        void testHeavyMekVsDropship_UsesAttackerWeight() {
            // 100-ton Mek charges 8500-ton DropShip
            // Expected: ceil(100 / 10) = 10 damage
            Entity attacker = createMockMek(100, false);
            Dropship target = createMockDropship(8500);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, false, 0);

            assertEquals(10, damage,
                  "100-ton Mek vs DropShip should use attacker's weight: ceil(100/10) = 10");
        }

        @Test
        @DisplayName("Wet attacker: damage halved")
        void testWetAttacker_DamageHalved() {
            // 50-ton wet Mek charges 100-ton Mek
            // Expected: ceil(100 / 10 * 0.5) = ceil(5) = 5 damage
            Entity attacker = createMockMek(50, true);
            Entity target = createMockMek(100, false);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, false, 0);

            assertEquals(5, damage, "Wet attacker should take half damage: ceil(100/10 * 0.5) = 5");
        }

        @Test
        @DisplayName("Wet attacker vs DropShip: damage halved using attacker weight")
        void testWetAttackerVsDropship_DamageHalvedUsingAttackerWeight() {
            // 50-ton wet Mek charges 3500-ton DropShip
            // Expected: ceil(50 / 10 * 0.5) = ceil(2.5) = 3 damage
            Entity attacker = createMockMek(50, true);
            Dropship target = createMockDropship(3500);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, false, 0);

            assertEquals(3, damage,
                  "Wet Mek vs DropShip should use attacker's weight and halve: ceil(50/10 * 0.5) = 3");
        }
    }

    @Nested
    @DisplayName("TacOps Formula (tacOps=true)")
    class TacOpsFormulaTests {

        @Test
        @DisplayName("Mek vs Mek: TacOps formula with target weight")
        void testTacOpsMekVsMek() {
            // 50-ton Mek charges 75-ton Mek at distance 3
            // Formula: floor(((targetWeight * attackerWeight) * distance) / (targetWeight + attackerWeight)) / 10
            // = floor(((75 * 50) * 3) / (75 + 50)) / 10
            // = floor((3750 * 3) / 125) / 10
            // = floor(11250 / 125) / 10
            // = floor(90) / 10 = 9 damage
            Entity attacker = createMockMek(50, false);
            Entity target = createMockMek(75, false);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, true, 3);

            assertEquals(9, damage, "TacOps Mek vs Mek should calculate: floor(((75*50)*3)/(75+50))/10 = 9");
        }

        @Test
        @DisplayName("Mek vs DropShip: TacOps formula uses attacker weight")
        void testTacOpsMekVsDropship_UsesAttackerWeight() {
            // 50-ton Mek charges 3500-ton DropShip at distance 3
            // Per TW p.148, use attacker's weight as effectiveTargetWeight
            // = floor(((50 * 50) * 3) / (50 + 50)) / 10
            // = floor((2500 * 3) / 100) / 10
            // = floor(7500 / 100) / 10
            // = floor(75) / 10 = 7 damage
            Entity attacker = createMockMek(50, false);
            Dropship target = createMockDropship(3500);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, true, 3);

            assertEquals(7, damage,
                  "TacOps Mek vs DropShip should use attacker's weight: floor(((50*50)*3)/(50+50))/10 = 7");
        }

        @Test
        @DisplayName("Equal weight units: TacOps formula")
        void testTacOpsEqualWeight() {
            // 50-ton Mek charges 50-ton Mek at distance 4
            // = floor(((50 * 50) * 4) / (50 + 50)) / 10
            // = floor((2500 * 4) / 100) / 10
            // = floor(10000 / 100) / 10
            // = floor(100) / 10 = 10 damage
            Entity attacker = createMockMek(50, false);
            Entity target = createMockMek(50, false);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, true, 4);

            assertEquals(10, damage, "TacOps equal weight should calculate: floor(((50*50)*4)/(50+50))/10 = 10");
        }

        @Test
        @DisplayName("Distance 1: minimum TacOps damage")
        void testTacOpsDistanceOne() {
            // 100-ton Mek charges 100-ton Mek at distance 1
            // = floor(((100 * 100) * 1) / (100 + 100)) / 10
            // = floor(10000 / 200) / 10
            // = floor(50) / 10 = 5 damage
            Entity attacker = createMockMek(100, false);
            Entity target = createMockMek(100, false);

            int damage = ChargeAttackAction.getDamageTakenBy(attacker, target, true, 1);

            assertEquals(5, damage, "TacOps distance 1 should calculate: floor(((100*100)*1)/(100+100))/10 = 5");
        }
    }
}
