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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Infantry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ToxinAttackAction}. Tests the toxin gas attack rules from IO pg 79.
 * <p>
 * Note: Full attack action testing requires integration tests due to static initialization dependencies. This test
 * covers construction, basic properties, and damage calculation.
 */
class ToxinAttackActionTest {

    private static final int ATTACKER_ID = 1;
    private static final int TARGET_ID = 2;
    private static final int TARGET_TYPE = 1; // TYPE_ENTITY

    @Nested
    @DisplayName("Construction tests")
    class ConstructionTests {

        @Test
        @DisplayName("Constructor with entity and target IDs creates valid action")
        void constructor_withEntityAndTargetIds_createsValidAction() {
            ToxinAttackAction action = new ToxinAttackAction(ATTACKER_ID, TARGET_ID);

            assertNotNull(action);
            assertEquals(ATTACKER_ID, action.getEntityId());
            assertEquals(TARGET_ID, action.getTargetId());
        }

        @Test
        @DisplayName("Constructor with entity, target type, and target ID creates valid action")
        void constructor_withEntityTargetTypeAndTargetId_createsValidAction() {
            ToxinAttackAction action = new ToxinAttackAction(ATTACKER_ID, TARGET_TYPE, TARGET_ID);

            assertNotNull(action);
            assertEquals(ATTACKER_ID, action.getEntityId());
            assertEquals(TARGET_TYPE, action.getTargetType());
            assertEquals(TARGET_ID, action.getTargetId());
        }
    }

    @Nested
    @DisplayName("Damage calculation tests")
    class DamageCalculationTests {

        @Test
        @DisplayName("getDamageFor with 4 troopers returns 1 damage (0.25 * 4 = 1)")
        void getDamageFor_with4Troopers_returns1Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(4);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(1, damage);
        }

        @Test
        @DisplayName("getDamageFor with 8 troopers returns 2 damage (0.25 * 8 = 2)")
        void getDamageFor_with8Troopers_returns2Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(8);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(2, damage);
        }

        @Test
        @DisplayName("getDamageFor with 10 troopers returns 3 damage (0.25 * 10 = 2.5 rounded to 3)")
        void getDamageFor_with10Troopers_returns3Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(10);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(3, damage);
        }

        @Test
        @DisplayName("getDamageFor with 21 troopers returns 5 damage (0.25 * 21 = 5.25 rounded to 5)")
        void getDamageFor_with21Troopers_returns5Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(21);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(5, damage);
        }

        @Test
        @DisplayName("getDamageFor with 28 troopers returns 7 damage (0.25 * 28 = 7)")
        void getDamageFor_with28Troopers_returns7Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(28);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(7, damage);
        }

        @Test
        @DisplayName("getDamageFor with 0 troopers returns 0 damage")
        void getDamageFor_with0Troopers_returns0Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(0);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(0, damage);
        }

        @Test
        @DisplayName("getDamageFor with 1 trooper returns 0 damage (0.25 rounded to 0)")
        void getDamageFor_with1Trooper_returns0Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(1);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(0, damage);
        }

        @Test
        @DisplayName("getDamageFor with 2 troopers returns 1 damage (0.25 * 2 = 0.5 rounded to 1)")
        void getDamageFor_with2Troopers_returns1Damage() {
            Infantry attacker = mock(Infantry.class);
            when(attacker.getShootingStrength()).thenReturn(2);

            int damage = ToxinAttackAction.getDamageFor(attacker);

            assertEquals(1, damage);
        }
    }
}
