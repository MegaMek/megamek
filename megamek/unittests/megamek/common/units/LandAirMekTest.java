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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LandAirMek} focusing on mode conversion behavior and type checking. These tests document the class
 * hierarchy and isAero() behavior that affects strafing eligibility (see GitHub issue #6708).
 */
class LandAirMekTest {

    private LandAirMek lam;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        lam = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
    }

    @Nested
    @DisplayName("Class Hierarchy Tests")
    class ClassHierarchyTests {

        @Test
        @DisplayName("LAM extends BipedMek, not Aero")
        void lamExtendsBipedMek() {
            // LAMs are Meks that can transform, not Aeros
            // This is why instanceof Aero checks fail for LAMs
            assertInstanceOf(BipedMek.class, lam);
            assertInstanceOf(Mek.class, lam);
            assertInstanceOf(Entity.class, lam);
        }

        @Test
        @DisplayName("LAM is not instanceof Aero - documents why instanceof check was wrong")
        void lamIsNotInstanceOfAero() {
            // This test documents the bug from issue #6708:
            // Code was using "instanceof Aero" which always returns false for LAMs,
            // even when in fighter mode. Use isAero() instead.
            // Note: Using Class.isAssignableFrom() because compiler rejects direct instanceof
            // check between unrelated types (which is exactly the point of this test)
            assertFalse(Aero.class.isAssignableFrom(lam.getClass()),
                  "LAM should not be instanceof Aero - use isAero() for mode-aware check");
        }

        @Test
        @DisplayName("LAM implements IAero interface")
        void lamImplementsIAero() {
            // LAMs implement IAero for aerospace behavior, but are not Aero subclasses
            assertInstanceOf(IAero.class, lam);
        }
    }

    @Nested
    @DisplayName("isAero() Mode Behavior Tests")
    class IsAeroModeTests {

        @Test
        @DisplayName("isAero() returns false in Mek mode")
        void isAeroFalseInMekMode() {
            lam.setConversionMode(LandAirMek.CONV_MODE_MEK);

            assertFalse(lam.isAero(), "LAM in Mek mode should not be considered aero");
            assertEquals(LandAirMek.CONV_MODE_MEK, lam.getConversionMode());
        }

        @Test
        @DisplayName("isAero() returns false in AirMek mode")
        void isAeroFalseInAirMekMode() {
            lam.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);

            assertFalse(lam.isAero(), "LAM in AirMek mode should not be considered aero");
            assertEquals(LandAirMek.CONV_MODE_AIR_MEK, lam.getConversionMode());
        }

        @Test
        @DisplayName("isAero() returns true in Fighter mode")
        void isAeroTrueInFighterMode() {
            lam.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);

            assertTrue(lam.isAero(), "LAM in Fighter mode should be considered aero");
            assertEquals(LandAirMek.CONV_MODE_FIGHTER, lam.getConversionMode());
        }

        @Test
        @DisplayName("isAero() changes when mode changes")
        void isAeroChangesWithMode() {
            // Start in Mek mode
            lam.setConversionMode(LandAirMek.CONV_MODE_MEK);
            assertFalse(lam.isAero());

            // Convert to Fighter mode
            lam.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
            assertTrue(lam.isAero());

            // Convert back to AirMek mode
            lam.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);
            assertFalse(lam.isAero());
        }
    }

    @Nested
    @DisplayName("Strafing Eligibility Tests")
    class StrafingEligibilityTests {

        @Test
        @DisplayName("LAM in Fighter mode satisfies isAero() check for strafing")
        void fighterModeSatisfiesStrafingAeroCheck() {
            // This test verifies the fix for issue #6708:
            // Strafing code should use isAero() not instanceof Aero
            lam.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);

            // The strafing eligibility check in FiringDisplay.updateStrafe() uses:
            // entity.isAero() && !entity.isSpheroid() && altitude conditions
            assertTrue(lam.isAero(), "Fighter mode LAM should pass isAero() check");
            assertFalse(lam.isSpheroid(), "LAM should not be spheroid");
        }

        @Test
        @DisplayName("LAM in Mek mode does not satisfy strafing aero check")
        void mekModeDoesNotSatisfyStrafingAeroCheck() {
            lam.setConversionMode(LandAirMek.CONV_MODE_MEK);

            assertFalse(lam.isAero(), "Mek mode LAM should not pass isAero() check for strafing");
        }

        @Test
        @DisplayName("LAM in AirMek mode does not satisfy strafing aero check")
        void airMekModeDoesNotSatisfyStrafingAeroCheck() {
            lam.setConversionMode(LandAirMek.CONV_MODE_AIR_MEK);

            assertFalse(lam.isAero(), "AirMek mode LAM should not pass isAero() check for strafing");
        }
    }
}
