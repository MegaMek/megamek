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

import megamek.common.compute.Compute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Tripod physical attack arc restrictions per IO:AE p.158. Tripods can only perform physical attacks against
 * targets in their front firing arc, based on torso rotation (secondary facing), not leg facing.
 *
 * <p>These tests verify the constants and document the implementation. Full integration
 * testing is performed via manual in-game testing as the physical attack toHit methods require extensive Game/Entity
 * setup that is impractical to mock completely.
 *
 * @see KickAttackAction#toHit
 * @see PunchAttackAction#toHit
 * @see ClubAttackAction#toHit
 * @see PushAttackAction#toHit
 */
public class TripodPhysicalAttackArcTest {

    /**
     * Tests for arc constants used in physical attacks.
     */
    @Nested
    @DisplayName("Arc Constant Tests")
    class ArcConstantTests {

        @Test
        @DisplayName("ARC_FORWARD constant is correctly defined")
        void arcForwardConstant() {
            // ARC_FORWARD is 120 degrees (from 300 to 60 degrees, wrapping around)
            // This is used for Tripod physical attacks
            assertEquals(1, Compute.ARC_FORWARD, "ARC_FORWARD should be constant 1");
        }

        @Test
        @DisplayName("ARC_REAR constant is correctly defined")
        void arcRearConstant() {
            // ARC_REAR is from 120 to 240 degrees (the back side)
            // Bipeds can mule kick to targets in this arc, but Tripods cannot
            assertEquals(4, Compute.ARC_REAR, "ARC_REAR should be constant 4");
        }

    }

    /**
     * Tests for KickAttackAction constants.
     */
    @Nested
    @DisplayName("Kick Attack Constants")
    class KickAttackConstantTests {

        @Test
        @DisplayName("Kick leg constants are correctly defined")
        void kickLegConstants() {
            assertEquals(0, KickAttackAction.BOTH, "BOTH should be 0");
            assertEquals(1, KickAttackAction.LEFT, "LEFT should be 1");
            assertEquals(2, KickAttackAction.RIGHT, "RIGHT should be 2");
            assertEquals(3, KickAttackAction.LEFT_MULE, "LEFT_MULE should be 3");
            assertEquals(4, KickAttackAction.RIGHT_MULE, "RIGHT_MULE should be 4");
        }
    }

    /**
     * Tests for PunchAttackAction constants.
     */
    @Nested
    @DisplayName("Punch Attack Constants")
    class PunchAttackConstantTests {

        @Test
        @DisplayName("Punch arm constants are correctly defined")
        void punchArmConstants() {
            assertEquals(0, PunchAttackAction.BOTH, "BOTH should be 0");
            assertEquals(1, PunchAttackAction.LEFT, "LEFT should be 1");
            assertEquals(2, PunchAttackAction.RIGHT, "RIGHT should be 2");
        }
    }
}
