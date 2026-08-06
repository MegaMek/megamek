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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PheromoneAttackAction}. Tests the pheromone gas attack rules from IO pg 79.
 * <p>
 * Note: Full attack action testing requires integration tests due to static initialization dependencies. This test
 * covers construction and basic properties.
 */
class PheromoneAttackActionTest {

    private static final int ATTACKER_ID = 1;
    private static final int TARGET_ID = 2;
    private static final int TARGET_TYPE = 1; // TYPE_ENTITY

    @Nested
    @DisplayName("Construction tests")
    class ConstructionTests {

        @Test
        @DisplayName("Constructor with entity and target IDs creates valid action")
        void constructor_withEntityAndTargetIds_createsValidAction() {
            PheromoneAttackAction action = new PheromoneAttackAction(ATTACKER_ID, TARGET_ID);

            assertNotNull(action);
            assertEquals(ATTACKER_ID, action.getEntityId());
            assertEquals(TARGET_ID, action.getTargetId());
        }

        @Test
        @DisplayName("Constructor with entity, target type, and target ID creates valid action")
        void constructor_withEntityTargetTypeAndTargetId_createsValidAction() {
            PheromoneAttackAction action = new PheromoneAttackAction(ATTACKER_ID, TARGET_TYPE, TARGET_ID);

            assertNotNull(action);
            assertEquals(ATTACKER_ID, action.getEntityId());
            assertEquals(TARGET_TYPE, action.getTargetType());
            assertEquals(TARGET_ID, action.getTargetId());
        }
    }
}
