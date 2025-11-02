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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.stream.Stream;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ExternalCargoTest {

    @Nested
    class GenericExternalCargoTests {
        static int TEST_ENTITY_WEIGHT = 55;
        static int MOCK_ENTITY_ID = 0;
        static int MOCK_EQUIPMENT_NUM = 0;
        static Game mockGame = mock(Game.class);
        static Entity mockEntity = mock(Entity.class);
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
            when(mockGame.getEntity(MOCK_ENTITY_ID)).thenReturn(mockEntity);

            when(mockEntity.getId()).thenReturn(MOCK_ENTITY_ID);
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

            assertEquals(TEST_ENTITY_WEIGHT, externalCargo.getUnused());
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
    }


    @Nested
    class RoofRackTests {
        int MOCK_ENTITY_WEIGHT = 55;

        @Test
        void simpleTest() {

            RoofRack roofRack = new RoofRack(MOCK_ENTITY_WEIGHT);

        }
    }
}
