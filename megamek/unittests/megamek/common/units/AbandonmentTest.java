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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for unit abandonment behavior per TacOps:AR p.165 rules.
 * <p>
 * Abandonment differs from ejection: - Abandonment: Two-phase (announce -> execute next turn), unit remains intact -
 * Ejection: Immediate, destroys unit cockpit/crew compartment
 * <p>
 * Note: Full canAbandon() testing requires integration tests with properly initialized Game and Entity objects. These
 * unit tests focus on state management and the isAbandoned() detection logic.
 */
class AbandonmentTest {

    private Game mockGame;
    private GameOptions mockOptions;
    private Crew mockCrew;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        mockGame = mock(Game.class);
        mockOptions = mock(GameOptions.class);
        mockCrew = mock(Crew.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);
    }

    @Nested
    @DisplayName("Mek Abandonment - Early Exit Conditions")
    class MekAbandonmentEarlyExitTests {

        /**
         * Tests that canAbandon() returns false when Mek is not prone. Uses a real BipedMek with game set to test the
         * actual logic.
         */
        @Test
        @DisplayName("Mek cannot abandon when not prone")
        void canAbandon_NotProne_ReturnsFalse() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            mek.setProne(false);  // Not prone - should fail early
            mek.setShutDown(true);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertFalse(mek.canAbandon(),
                  "Mek cannot abandon when not prone");
        }

        @Test
        @DisplayName("Mek cannot abandon when not shutdown")
        void canAbandon_NotShutdown_ReturnsFalse() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            mek.setProne(true);
            mek.setShutDown(false);  // Not shutdown - should fail
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertFalse(mek.canAbandon(),
                  "Mek cannot abandon when not shutdown");
        }

        @Test
        @DisplayName("Mek cannot abandon when crew is dead")
        void canAbandon_DeadCrew_ReturnsFalse() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(true);  // Dead crew

            mek.setProne(true);
            mek.setShutDown(true);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertFalse(mek.canAbandon(),
                  "Mek cannot abandon when crew is dead");
        }

        @Test
        @DisplayName("Mek cannot abandon when crew already ejected")
        void canAbandon_CrewAlreadyEjected_ReturnsFalse() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(true);  // Already ejected
            when(mockCrew.isDead()).thenReturn(false);

            mek.setProne(true);
            mek.setShutDown(true);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertFalse(mek.canAbandon(),
                  "Mek cannot abandon when crew already ejected");
        }

        @Test
        @DisplayName("Mek cannot abandon when abandonment already pending")
        void canAbandon_AlreadyPending_ReturnsFalse() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            mek.setProne(true);
            mek.setShutDown(true);
            mek.setPendingAbandon(true);  // Already announced
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertFalse(mek.canAbandon(),
                  "Mek cannot abandon when abandonment is already pending");
        }

        @Test
        @DisplayName("Mek cannot abandon when game option disabled")
        void canAbandon_OptionDisabled_ReturnsFalse() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            mek.setProne(true);
            mek.setShutDown(true);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(false);  // Option disabled

            assertFalse(mek.canAbandon(),
                  "Mek cannot abandon when game option is disabled");
        }

        @Test
        @DisplayName("Mek can abandon when all conditions met")
        void canAbandon_AllConditionsMet_ReturnsTrue() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            mek.setProne(true);
            mek.setShutDown(true);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertTrue(mek.canAbandon(),
                  "Mek should be able to abandon when all conditions met");
        }
    }

    @Nested
    @DisplayName("Tank Abandonment - Option Requirements")
    class TankAbandonmentTests {

        @Test
        @DisplayName("Tank can abandon when eject option enabled")
        void canAbandon_EjectOptionEnabled_ReturnsTrue() {
            Tank tank = new Tank();
            tank.setGame(mockGame);
            tank.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            // Only requires the vehicle eject/abandon option
            // Crew size is defined in TM p.103, not dependent on TacOps Vehicle Crews option
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertTrue(tank.canAbandon(),
                  "Tank should be able to abandon when eject option enabled");
        }

        @Test
        @DisplayName("Tank cannot abandon without eject option")
        void canAbandon_MissingEjectOption_ReturnsFalse() {
            Tank tank = new Tank();
            tank.setGame(mockGame);
            tank.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(false);  // Missing eject option

            assertFalse(tank.canAbandon(),
                  "Tank cannot abandon without eject option");
        }

        @Test
        @DisplayName("Tank cannot abandon when crew is dead")
        void canAbandon_DeadCrew_ReturnsFalse() {
            Tank tank = new Tank();
            tank.setGame(mockGame);
            tank.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(true);  // Dead crew

            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertFalse(tank.canAbandon(),
                  "Tank cannot abandon when crew is dead");
        }
    }

    @Nested
    @DisplayName("Abandoned State Detection")
    class IsAbandonedTests {

        @Test
        @DisplayName("Mek is abandoned when crew ejected and unit not destroyed")
        void isAbandoned_MekCrewEjectedNotDestroyed_ReturnsTrue() {
            BipedMek mek = new BipedMek();
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(true);
            // BipedMek is not destroyed by default

            assertTrue(mek.isAbandoned(),
                  "Mek should be considered abandoned when crew ejected but unit intact");
        }

        @Test
        @DisplayName("Mek is not abandoned when crew still present")
        void isAbandoned_MekCrewPresent_ReturnsFalse() {
            BipedMek mek = new BipedMek();
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);

            assertFalse(mek.isAbandoned(),
                  "Mek should not be considered abandoned when crew is still present");
        }

        @Test
        @DisplayName("Tank is abandoned when crew ejected and unit not destroyed")
        void isAbandoned_TankCrewEjected_ReturnsTrue() {
            Tank tank = new Tank();
            tank.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(true);

            assertTrue(tank.isAbandoned(),
                  "Tank should be considered abandoned when crew ejected but unit intact");
        }

        @Test
        @DisplayName("Tank is not abandoned when crew still present")
        void isAbandoned_TankCrewPresent_ReturnsFalse() {
            Tank tank = new Tank();
            tank.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);

            assertFalse(tank.isAbandoned(),
                  "Tank should not be considered abandoned when crew is still present");
        }
    }

    @Nested
    @DisplayName("Pending Abandonment State Management")
    class PendingAbandonmentTests {

        @Test
        @DisplayName("setPendingAbandon clears announced round when set to false")
        void setPendingAbandon_SetToFalse_ClearsAnnouncedRound() {
            BipedMek mek = new BipedMek();

            // Simulate announcement
            mek.setPendingAbandon(true);
            mek.setAbandonmentAnnouncedRound(5);

            // Clear pending status
            mek.setPendingAbandon(false);

            assertFalse(mek.isPendingAbandon(),
                  "Pending abandon should be false after clearing");
            assertEquals(-1, mek.getAbandonmentAnnouncedRound(),
                  "Announced round should be reset to -1 when pending is cleared");
        }

        @Test
        @DisplayName("Abandonment announcement tracks the round correctly")
        void abandonmentAnnouncement_TracksRound() {
            BipedMek mek = new BipedMek();

            mek.setPendingAbandon(true);
            mek.setAbandonmentAnnouncedRound(3);

            assertTrue(mek.isPendingAbandon());
            assertEquals(3, mek.getAbandonmentAnnouncedRound(),
                  "Announced round should be tracked correctly");
        }

        @Test
        @DisplayName("Tank pending abandonment state works the same as Mek")
        void tankPendingAbandon_WorksSameAsMek() {
            Tank tank = new Tank();

            tank.setPendingAbandon(true);
            tank.setAbandonmentAnnouncedRound(7);

            assertTrue(tank.isPendingAbandon());
            assertEquals(7, tank.getAbandonmentAnnouncedRound());

            tank.setPendingAbandon(false);

            assertFalse(tank.isPendingAbandon());
            assertEquals(-1, tank.getAbandonmentAnnouncedRound());
        }
    }
}
