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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Set;

import megamek.common.board.Coords;
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
 * Tests for the PREEND_DECLARATIONS phase eligibility logic.
 * <p>
 * Units get a turn in the pre-end declarations phase when they have an end-phase declaration to make. Beyond initiating
 * infantry vs. infantry combat, this now covers the declarations moved off the end report: Nova CEWS network changes,
 * Variable Range Targeting mode changes, crew abandonment, and minesweeper activation. See
 * {@link Entity#isEligibleForPreEndDeclarations()} and {@link Entity#canAnnounceAbandon()}.
 */
class PreEndDeclarationsEligibilityTest {

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
    @DisplayName("canAnnounceAbandon delegation")
    class CanAnnounceAbandonTests {

        @Test
        @DisplayName("Base unit type cannot announce abandonment")
        void baseUnit_cannotAnnounceAbandon() {
            // ProtoMek does not override canAnnounceAbandon, so it uses the Entity base (false).
            assertFalse(new ProtoMek().canAnnounceAbandon(),
                  "A unit type without abandonment support uses the false base implementation");
        }

        @Test
        @DisplayName("Mek mirrors canAbandon when abandonment is possible")
        void mek_canAnnounceAbandon_tracksCanAbandonTrue() {
            BipedMek mek = abandonReadyMek();

            assertTrue(mek.canAbandon(), "precondition: the Mek can abandon");
            assertTrue(mek.canAnnounceAbandon(),
                  "Mek.canAnnounceAbandon should mirror canAbandon when abandonment is possible");
        }

        @Test
        @DisplayName("Mek mirrors canAbandon when not prone")
        void mek_canAnnounceAbandon_tracksCanAbandonFalse() {
            BipedMek mek = abandonReadyMek();
            mek.setProne(false); // a standing Mek cannot abandon

            assertFalse(mek.canAbandon(), "precondition: the Mek cannot abandon");
            assertFalse(mek.canAnnounceAbandon(),
                  "Mek.canAnnounceAbandon should mirror canAbandon when abandonment is not possible");
        }

        @Test
        @DisplayName("Tank can announce abandonment when the eject option is enabled")
        void tank_canAnnounceAbandon_tracksCanAbandonTrue() {
            Tank tank = new Tank();
            tank.setGame(mockGame);
            tank.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(true);

            assertTrue(tank.canAbandon(), "precondition: the Tank can abandon");
            assertTrue(tank.canAnnounceAbandon(),
                  "Tank.canAnnounceAbandon should mirror canAbandon when the eject option is enabled");
        }

        @Test
        @DisplayName("Tank cannot announce abandonment without the eject option")
        void tank_canAnnounceAbandon_tracksCanAbandonFalse() {
            Tank tank = new Tank();
            tank.setGame(mockGame);
            tank.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);
            when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
                  .thenReturn(false);

            assertFalse(tank.canAbandon(), "precondition: the Tank cannot abandon");
            assertFalse(tank.canAnnounceAbandon(),
                  "Tank.canAnnounceAbandon should mirror canAbandon when the eject option is disabled");
        }

        @Test
        @DisplayName("Escape pod delegates to canCrewExit (true)")
        void escapePod_canAnnounceAbandon_delegatesToCanCrewExitTrue() {
            CombatVehicleEscapePod pod = new CombatVehicleEscapePod() {
                @Override
                public boolean canCrewExit() {
                    return true;
                }
            };

            assertTrue(pod.canAnnounceAbandon(),
                  "Escape pod canAnnounceAbandon should follow canCrewExit");
        }

        @Test
        @DisplayName("Breached escape pod cannot announce abandonment")
        void escapePod_breached_cannotAnnounceAbandon() {
            CombatVehicleEscapePod pod = new CombatVehicleEscapePod();
            pod.setPosition(new Coords(5, 5));
            pod.applyDamage(3); // breach the pod (crew dead)

            assertFalse(pod.canCrewExit(), "precondition: crew cannot exit a breached pod");
            assertFalse(pod.canAnnounceAbandon(),
                  "A breached escape pod cannot announce abandonment");
        }
    }

    @Nested
    @DisplayName("isEligibleForPreEndDeclarations composition")
    class IsEligibleTests {

        @Test
        @DisplayName("No capability means not eligible")
        void noCapability_notEligible() {
            assertFalse(eligibilitySpyAllFalse().isEligibleForPreEndDeclarations(),
                  "A unit with no end-phase declaration capability gets no pre-end turn");
        }

        @Test
        @DisplayName("Infantry combat capability grants eligibility")
        void infantryCombat_grantsEligibility() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).canInitiateInfantryVsInfantryCombat();

            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "A unit that can initiate infantry combat is eligible");
        }

        @Test
        @DisplayName("Nova CEWS grants eligibility")
        void novaCews_grantsEligibility() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).hasNovaCEWS();

            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "A unit with Nova CEWS is eligible (network changes are declared in the End Phase)");
        }

        @Test
        @DisplayName("Variable Range Targeting grants eligibility")
        void variableRangeTargeting_grantsEligibility() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).hasVariableRangeTargeting();

            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "A unit with Variable Range Targeting is eligible");
        }

        @Test
        @DisplayName("Abandonment capability grants eligibility")
        void abandon_grantsEligibility() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).canAnnounceAbandon();

            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "A unit that can announce abandonment is eligible");
        }

        @Test
        @DisplayName("Minesweeper grants eligibility")
        void minesweeper_grantsEligibility() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).hasMinesweeper();

            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "A unit with a minesweeper is eligible");
        }

        @Test
        @DisplayName("Owning a demolition charge grants eligibility")
        void demolitionCharge_grantsEligibility() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).ownerHasDemolitionCharge();

            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "A unit whose owner set a demolition charge is eligible (to announce detonation)");
        }

        @Test
        @DisplayName("Owning a demolition charge is player-wide, not entity-scoped")
        void demolitionCharge_isNotEntityScoped() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).ownerHasDemolitionCharge();

            assertFalse(mek.hasEntityScopedPreEndDeclaration(),
                  "Detonating a charge is a player-wide declaration, so it collapses to one turn");
        }

        @Test
        @DisplayName("An abandon-ready Mek is eligible end to end")
        void abandonReadyMek_isEligible() {
            BipedMek mek = abandonReadyMek();

            assertTrue(mek.canAnnounceAbandon(), "precondition: the Mek can announce abandonment");
            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "Real abandon eligibility should flow through to pre-end declarations eligibility");
        }

        @Test
        @DisplayName("A player-wide declaration is eligible but not entity-scoped (gets collapsed to one turn)")
        void playerWideOnly_isNotEntityScoped() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).hasVariableRangeTargeting();

            assertTrue(mek.isEligibleForPreEndDeclarations(),
                  "A Variable Range Targeting unit is eligible for the phase");
            assertFalse(mek.hasEntityScopedPreEndDeclaration(),
                  "Variable Range Targeting is a player-wide declaration, so it is not entity-scoped");
        }

        @Test
        @DisplayName("Infantry combat is entity-scoped (keeps its own per-unit turn)")
        void infantryCombat_isEntityScoped() {
            BipedMek mek = eligibilitySpyAllFalse();
            doReturn(true).when(mek).canInitiateInfantryVsInfantryCombat();

            assertTrue(mek.hasEntityScopedPreEndDeclaration(),
                  "Infantry vs infantry combat is declared per unit, so it is entity-scoped");
        }

        @Test
        @DisplayName("A plain Mek is not eligible")
        void plainMek_notEligible() {
            BipedMek mek = new BipedMek();
            mek.setGame(mockGame);
            mek.setCrew(mockCrew);
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);
            // standing, not shutdown, no Nova/VRT/minesweeper equipment, not infantry

            assertFalse(mek.isEligibleForPreEndDeclarations(),
                  "A plain Mek with no end-phase declaration capability is not eligible");
        }
    }

    @Nested
    @DisplayName("ownerHasDemolitionCharge detection")
    class OwnerHasDemolitionChargeTests {

        @Test
        @DisplayName("Off-game unit owns no charge")
        void noGame_returnsFalse() {
            // A freshly constructed unit has no game set; the check must not NPE.
            assertFalse(new BipedMek().ownerHasDemolitionCharge(),
                  "A unit with no game cannot own a demolition charge");
        }

        @Test
        @DisplayName("True when this unit's owner set a charge")
        void ownerSetCharge_returnsTrue() {
            BipedMek mek = chargeOwnerMek(7, Set.of(7));

            assertTrue(mek.ownerHasDemolitionCharge(),
                  "Owner id is in the charge-owner set, so the owner has a charge");
        }

        @Test
        @DisplayName("False when only another player set a charge")
        void otherPlayerSetCharge_returnsFalse() {
            BipedMek mek = chargeOwnerMek(7, Set.of(8));

            assertFalse(mek.ownerHasDemolitionCharge(),
                  "The only charge belongs to a different player, so this owner has none");
        }

        @Test
        @DisplayName("False when no charges are set")
        void noCharges_returnsFalse() {
            BipedMek mek = chargeOwnerMek(7, Set.of());

            assertFalse(mek.ownerHasDemolitionCharge(),
                  "No demolition charges exist, so the owner has none");
        }

        /**
         * Builds a BipedMek owned by {@code ownerId}, on a game whose cached demolition-charge owner set is
         * {@code chargeOwnerIds}. {@code ownerHasDemolitionCharge()} does a constant-time lookup against that set.
         */
        private BipedMek chargeOwnerMek(int ownerId, Set<Integer> chargeOwnerIds) {
            when(mockGame.getPlayerIdsWithDemolitionCharges()).thenReturn(chargeOwnerIds);
            // Stub the id directly because setOwnerId() resolves a Player from the game, which is mocked here.
            BipedMek mek = spy(new BipedMek());
            mek.setGame(mockGame);
            doReturn(ownerId).when(mek).getOwnerId();
            return mek;
        }
    }

    /**
     * Returns a real BipedMek configured so {@link BipedMek#canAbandon()} is {@code true}: prone, shut down, with a
     * living crew and the vehicle eject/abandon option enabled.
     */
    private BipedMek abandonReadyMek() {
        BipedMek mek = new BipedMek();
        mek.setGame(mockGame);
        mek.setCrew(mockCrew);
        when(mockCrew.isEjected()).thenReturn(false);
        when(mockCrew.isDead()).thenReturn(false);
        mek.setProne(true);
        mek.setShutDown(true);
        when(mockOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT))
              .thenReturn(true);
        return mek;
    }

    /**
     * Returns a BipedMek spy with every pre-end declaration capability stubbed to {@code false}, so a single
     * capability can be flipped on to test that {@link Entity#isEligibleForPreEndDeclarations()} reacts to it.
     */
    private BipedMek eligibilitySpyAllFalse() {
        BipedMek mek = spy(new BipedMek());
        doReturn(false).when(mek).canInitiateInfantryVsInfantryCombat();
        doReturn(false).when(mek).hasNovaCEWS();
        doReturn(false).when(mek).hasVariableRangeTargeting();
        doReturn(false).when(mek).canAnnounceAbandon();
        doReturn(false).when(mek).hasMinesweeper();
        doReturn(false).when(mek).ownerHasDemolitionCharge();
        return mek;
    }
}
