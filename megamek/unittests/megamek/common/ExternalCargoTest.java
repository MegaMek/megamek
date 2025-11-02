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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.stream.Stream;

import megamek.common.equipment.Cargo;
import megamek.common.equipment.ExternalCargo;
import megamek.common.equipment.LiftHoist;
import megamek.common.equipment.MekArms;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.RoofRack;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ExternalCargoTest {
    static int MOCK_ENTITY_ID = 0;
    static int MOCK_EQUIPMENT_NUM = 0;

    static Game mockGame = mock(Game.class);
    static Entity mockEntity = mock(Entity.class);

    @BeforeAll
    static void beforeAll() {
        when(mockGame.getEntity(MOCK_ENTITY_ID)).thenReturn(mockEntity);

        when(mockEntity.getId()).thenReturn(MOCK_ENTITY_ID);
    }


    @Nested
    class GenericExternalCargoTests {
        static double TEST_WEIGHT = 55;

        static double MOCK_CARRYABLE_WEIGHT_1 = 20; // Less than half of TEST_ENTITY_WEIGHT, please
        static double MOCK_CARRYABLE_WEIGHT_2 = TEST_WEIGHT - MOCK_CARRYABLE_WEIGHT_1;
        static MiscMounted mockLiftHoist = mock(MiscMounted.class);

        static Stream<Arguments> externalCargoTypes() {
            return Stream.of(
                  Arguments.of(new RoofRack(55)),
                  Arguments.of(new MekArms(55)),
                  Arguments.of(new LiftHoist(mockLiftHoist, 55))
            );
        }

        @BeforeAll
        static void beforeAll() {
            when(mockEntity.isLocationBad(anyInt())).thenReturn(false);
            when(mockEntity.getEquipment(MOCK_EQUIPMENT_NUM)).thenReturn((Mounted) mockLiftHoist);

            when(mockLiftHoist.getEntity()).thenReturn(mockEntity);
            when(mockLiftHoist.getEquipmentNum()).thenReturn(MOCK_EQUIPMENT_NUM);
            when(mockLiftHoist.getLocation()).thenReturn(0);
            when(mockLiftHoist.isOperable()).thenReturn(true);

        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void getUnusedTest(ExternalCargo externalCargo) {
            externalCargo.setGame(mockGame);

            assertEquals(TEST_WEIGHT, externalCargo.getUnused());
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void getCarriedTonnageNothingCarriedTest(ExternalCargo externalCargo) {
            assertEquals(0, externalCargo.getCarriedTonnage());
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void getMPCargoReductionNothingCarriedTest(ExternalCargo externalCargo) {

            when(mockEntity.getDistinctCarriedObjects()).thenReturn(new ArrayList<>());

            assertEquals(0, externalCargo.getCargoMpReduction(mockEntity));
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void loadCarryableEnoughSpaceTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(MOCK_CARRYABLE_WEIGHT_1);

            // Act
            externalCargo.loadCarryable(cargo);

            // Assert
            assertEquals(MOCK_CARRYABLE_WEIGHT_1, externalCargo.getCarriedTonnage());
            assertEquals(TEST_WEIGHT - MOCK_CARRYABLE_WEIGHT_1, externalCargo.getUnused());
            assertEquals(1, externalCargo.getCarryables().size());
            assertEquals(cargo, externalCargo.getCarryables().get(0));
        }


        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void loadCarryablesJustEnoughSpaceTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(MOCK_CARRYABLE_WEIGHT_1);

            Cargo cargo2 = new Cargo();
            cargo2.setTonnage(MOCK_CARRYABLE_WEIGHT_2);

            // Act
            externalCargo.loadCarryable(cargo);
            externalCargo.loadCarryable(cargo2);

            // Assert
            assertEquals(MOCK_CARRYABLE_WEIGHT_1 + MOCK_CARRYABLE_WEIGHT_2, externalCargo.getCarriedTonnage());
            assertEquals(TEST_WEIGHT - MOCK_CARRYABLE_WEIGHT_1 - MOCK_CARRYABLE_WEIGHT_2, externalCargo.getUnused());
            assertEquals(2, externalCargo.getCarryables().size());
            assertTrue(externalCargo.getCarryables().contains(cargo));
            assertTrue(externalCargo.getCarryables().contains(cargo2));
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void loadCarryableBarelyTooMuchTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(TEST_WEIGHT + 1);

            // Act
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                  () -> externalCargo.loadCarryable(cargo));

            // Assert
            assertEquals("Not enough space to load " + cargo.specificName(), exception.getMessage());
            assertTrue(externalCargo.getCarryables().isEmpty());
            assertFalse(externalCargo.getCarryables().contains(cargo));
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void loadCarryablesBarelyTooMuchTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(MOCK_CARRYABLE_WEIGHT_1);

            Cargo cargo2 = new Cargo();
            cargo2.setTonnage(MOCK_CARRYABLE_WEIGHT_2 + 1);

            // Act
            externalCargo.loadCarryable(cargo);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                  () -> externalCargo.loadCarryable(cargo2));

            // Assert
            assertEquals(MOCK_CARRYABLE_WEIGHT_1, externalCargo.getCarriedTonnage());
            assertEquals(TEST_WEIGHT - MOCK_CARRYABLE_WEIGHT_1, externalCargo.getUnused());
            assertEquals(1, externalCargo.getCarryables().size());
            assertEquals("Not enough space to load " + cargo2.specificName(), exception.getMessage());
            assertTrue(externalCargo.getCarryables().contains(cargo));
            assertFalse(externalCargo.getCarryables().contains(cargo2));
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void unloadCarryableTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(TEST_WEIGHT);

            externalCargo.loadCarryable(cargo);

            // (Verify we loaded properly)
            assertEquals(1, externalCargo.getCarryables().size());
            assertEquals(cargo, externalCargo.getCarryables().get(0));

            // Act
            boolean unloadedCargo = externalCargo.unloadCarryable(cargo);

            // Assert
            assertTrue(unloadedCargo);
            assertTrue(externalCargo.getCarryables().isEmpty());
            assertEquals(TEST_WEIGHT, externalCargo.getUnused());
            assertEquals(0, externalCargo.getCarriedTonnage());
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void unloadCarryablesTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(MOCK_CARRYABLE_WEIGHT_1);

            Cargo cargo2 = new Cargo();
            cargo2.setTonnage(MOCK_CARRYABLE_WEIGHT_2);

            externalCargo.loadCarryable(cargo);
            externalCargo.loadCarryable(cargo2);

            assertTrue(externalCargo.getCarryables().contains(cargo));
            assertTrue(externalCargo.getCarryables().contains(cargo2));

            // Act
            boolean unloadedCargo = externalCargo.unloadCarryable(cargo);
            boolean unloadedCargo2 = externalCargo.unloadCarryable(cargo2);

            // Assert
            assertTrue(unloadedCargo);
            assertTrue(unloadedCargo2);
            assertEquals(0, externalCargo.getCarriedTonnage());
            assertEquals(TEST_WEIGHT, externalCargo.getUnused());
            assertEquals(0, externalCargo.getCarryables().size());
            assertFalse(externalCargo.getCarryables().contains(cargo));
            assertFalse(externalCargo.getCarryables().contains(cargo2));
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void unloadOneCarryableTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(MOCK_CARRYABLE_WEIGHT_1);

            Cargo cargo2 = new Cargo();
            cargo2.setTonnage(MOCK_CARRYABLE_WEIGHT_2);

            externalCargo.loadCarryable(cargo);
            externalCargo.loadCarryable(cargo2);

            assertTrue(externalCargo.getCarryables().contains(cargo));
            assertTrue(externalCargo.getCarryables().contains(cargo2));

            // Act
            boolean unloadedCargo = externalCargo.unloadCarryable(cargo);

            // Assert
            assertTrue(unloadedCargo);
            assertEquals(MOCK_CARRYABLE_WEIGHT_2, externalCargo.getCarriedTonnage());
            assertEquals(TEST_WEIGHT - MOCK_CARRYABLE_WEIGHT_2, externalCargo.getUnused());
            assertEquals(1, externalCargo.getCarryables().size());
            assertFalse(externalCargo.getCarryables().contains(cargo));
            assertTrue(externalCargo.getCarryables().contains(cargo2));
        }

        @ParameterizedTest
        @MethodSource(value = "externalCargoTypes")
        void resetTransporterTest(ExternalCargo externalCargo) {
            // Arrange
            externalCargo.setGame(mockGame);

            Cargo cargo = new Cargo();
            cargo.setTonnage(TEST_WEIGHT);

            externalCargo.loadCarryable(cargo);

            // (Verify we loaded properly)
            assertEquals(1, externalCargo.getCarryables().size());
            assertEquals(cargo, externalCargo.getCarryables().get(0));

            // Act
            externalCargo.resetTransporter();

            // Assert
            assertTrue(externalCargo.getCarryables().isEmpty());
            assertEquals(TEST_WEIGHT, externalCargo.getUnused());
            assertEquals(0, externalCargo.getCarriedTonnage());
        }

    }


    @Nested
    class RoofRackTests {
        static double TEST_WEIGHT = 60;

        static Entity mockCarrier = mock(Entity.class);

        @BeforeAll
        static void beforeAll() {
            when(mockCarrier.getWeight()).thenReturn(TEST_WEIGHT);
        }

        static Stream<Arguments> entityMovementData() {
            return Stream.of(
                  Arguments.of(1, 1, 1),
                  Arguments.of(2, 1, 1),
                  Arguments.of(3, 2, 2),
                  Arguments.of(6, 3, 3),
                  Arguments.of(9, 3, 5),
                  Arguments.of(12, 3, 6)
            );
        }

        @ParameterizedTest
        @MethodSource(value = "entityMovementData")
        void lightLoadTest(int baseEntityMP, int lessThanQuarterWeightMPReduction, int heavyLoadMPReduction) {
            // Assemble
            when(mockCarrier.getDistinctCarriedObjects()).thenReturn(new ArrayList<>());
            when(mockCarrier.getOriginalWalkMP()).thenReturn(baseEntityMP);

            RoofRack roofRack = new RoofRack(TEST_WEIGHT);

            Cargo cargo = new Cargo();
            cargo.setTonnage(TEST_WEIGHT / 4.0);

            // Act
            roofRack.loadCarryable(cargo);
            int mpReduction = roofRack.getCargoMpReduction(mockCarrier);

            // Assert
            assertEquals(lessThanQuarterWeightMPReduction, mpReduction);
            assertEquals(1, roofRack.getCarryables().size());
            assertEquals(cargo, roofRack.getCarryables().get(0));
        }

        @ParameterizedTest
        @MethodSource(value = "entityMovementData")
        void heavyLoadTest(int baseEntityMP, int lessThanQuarterWeightMPReduction, int heavyLoadMPReduction) {
            // Assemble
            when(mockCarrier.getDistinctCarriedObjects()).thenReturn(new ArrayList<>());
            when(mockCarrier.getOriginalWalkMP()).thenReturn(baseEntityMP);

            RoofRack roofRack = new RoofRack(TEST_WEIGHT);

            Cargo cargo = new Cargo();
            cargo.setTonnage((TEST_WEIGHT / 4.0) + 1);

            // Act
            roofRack.loadCarryable(cargo);
            int mpReduction = roofRack.getCargoMpReduction(mockCarrier);

            // Assert
            assertEquals(heavyLoadMPReduction, mpReduction);
            assertEquals(1, roofRack.getCarryables().size());
            assertEquals(cargo, roofRack.getCarryables().get(0));
        }
    }
}
