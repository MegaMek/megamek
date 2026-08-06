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
package megamek.ai.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.GunEmplacement;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.SpaceStation;
import megamek.common.units.SuperHeavyTank;
import megamek.common.units.Tank;
import megamek.common.units.Warship;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link EntityFeatureUtils}.
 */
class EntityFeatureUtilsTest {

    private static final float DELTA = 1e-6f;

    @Test
    void testFrontHealthStatsMek() {
        Mek mek = Mockito.mock(Mek.class);
        Mockito.when(mek.getArmor(Mek.LOC_CENTER_TORSO)).thenReturn(10);
        Mockito.when(mek.getArmor(Mek.LOC_LEFT_TORSO)).thenReturn(5);
        Mockito.when(mek.getArmor(Mek.LOC_RIGHT_TORSO)).thenReturn(5);
        Mockito.when(mek.getOArmor(Mek.LOC_CENTER_TORSO)).thenReturn(20);
        Mockito.when(mek.getOArmor(Mek.LOC_LEFT_TORSO)).thenReturn(10);
        Mockito.when(mek.getOArmor(Mek.LOC_RIGHT_TORSO)).thenReturn(10);

        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(mek), DELTA);
        assertEquals(20, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(mek));
    }

    @Test
    void testBackHealthStatsMek() {
        Mek mek = Mockito.mock(Mek.class);
        Mockito.when(mek.getArmor(Mek.LOC_CENTER_TORSO, true)).thenReturn(2);
        Mockito.when(mek.getArmor(Mek.LOC_LEFT_TORSO, true)).thenReturn(1);
        Mockito.when(mek.getArmor(Mek.LOC_RIGHT_TORSO, true)).thenReturn(1);
        Mockito.when(mek.getOArmor(Mek.LOC_CENTER_TORSO, true)).thenReturn(4);
        Mockito.when(mek.getOArmor(Mek.LOC_LEFT_TORSO, true)).thenReturn(2);
        Mockito.when(mek.getOArmor(Mek.LOC_RIGHT_TORSO, true)).thenReturn(2);

        assertEquals(0.5f, EntityFeatureUtils.getTargetBackHealthStats(mek), DELTA);
        assertEquals(4, EntityFeatureUtils.getDiscreteTargetBackHealthStats(mek));
    }

    @Test
    void testLeftSideHealthStatsMek() {
        Mek mek = Mockito.mock(Mek.class);
        Mockito.when(mek.getArmor(Mek.LOC_LEFT_TORSO)).thenReturn(5);
        Mockito.when(mek.getArmor(Mek.LOC_LEFT_ARM)).thenReturn(5);
        Mockito.when(mek.getArmor(Mek.LOC_LEFT_LEG)).thenReturn(5);
        Mockito.when(mek.getOArmor(Mek.LOC_LEFT_TORSO)).thenReturn(10);
        Mockito.when(mek.getOArmor(Mek.LOC_LEFT_ARM)).thenReturn(10);
        Mockito.when(mek.getOArmor(Mek.LOC_LEFT_LEG)).thenReturn(10);

        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(mek), DELTA);
        assertEquals(15, EntityFeatureUtils.getDiscreteTargetLeftSideHealthStats(mek));
    }

    @Test
    void testRightSideHealthStatsMek() {
        Mek mek = Mockito.mock(Mek.class);
        Mockito.when(mek.getArmor(Mek.LOC_RIGHT_TORSO)).thenReturn(5);
        Mockito.when(mek.getArmor(Mek.LOC_RIGHT_ARM)).thenReturn(5);
        Mockito.when(mek.getArmor(Mek.LOC_RIGHT_LEG)).thenReturn(5);
        Mockito.when(mek.getOArmor(Mek.LOC_RIGHT_TORSO)).thenReturn(10);
        Mockito.when(mek.getOArmor(Mek.LOC_RIGHT_ARM)).thenReturn(10);
        Mockito.when(mek.getOArmor(Mek.LOC_RIGHT_LEG)).thenReturn(10);

        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(mek), DELTA);
        assertEquals(15, EntityFeatureUtils.getDiscreteTargetRightSideHealthStats(mek));
    }

    @Test
    void testOverallHealthStats() {
        Entity entity = Mockito.mock(Entity.class);
        Mockito.when(entity.getArmorRemainingPercent()).thenReturn(0.75);
        Mockito.when(entity.getTotalArmor()).thenReturn(75);

        assertEquals(0.75f, EntityFeatureUtils.getTargetOverallHealthStats(entity), DELTA);
        assertEquals(75, EntityFeatureUtils.getDiscreteTargetOverallHealthStats(entity));
    }

    @Test
    void testAeroFront() {
        Aero aero = Mockito.mock(Aero.class);
        Mockito.when(aero.getArmor(Aero.LOC_NOSE)).thenReturn(10);
        Mockito.when(aero.getOArmor(Aero.LOC_NOSE)).thenReturn(20);
        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(aero), DELTA);
        assertEquals(10, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(aero));
    }

    @Test
    void testTankFront() {
        Tank tank = Mockito.mock(Tank.class);
        Mockito.when(tank.getArmor(Tank.LOC_FRONT)).thenReturn(10);
        Mockito.when(tank.getOArmor(Tank.LOC_FRONT)).thenReturn(20);
        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(tank), DELTA);
        assertEquals(10, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(tank));
    }

    @Test
    void testSuperHeavyTankFront() {
        SuperHeavyTank tank = Mockito.mock(SuperHeavyTank.class);
        Mockito.when(tank.getArmor(SuperHeavyTank.LOC_FRONT)).thenReturn(10);
        Mockito.when(tank.getOArmor(SuperHeavyTank.LOC_FRONT)).thenReturn(20);
        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(tank), DELTA);
        assertEquals(10, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(tank));
    }

    @Test
    void testJumpshipFront() {
        Jumpship unit = Mockito.mock(Jumpship.class);
        Mockito.when(unit.getArmor(Jumpship.LOC_NOSE)).thenReturn(10);
        Mockito.when(unit.getOArmor(Jumpship.LOC_NOSE)).thenReturn(20);
        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(unit), DELTA);
        assertEquals(10, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(unit));
    }

    @Test
    void testSpaceStationFront() {
        SpaceStation unit = Mockito.mock(SpaceStation.class);
        Mockito.when(unit.getArmor(SpaceStation.LOC_NOSE)).thenReturn(10);
        Mockito.when(unit.getOArmor(SpaceStation.LOC_NOSE)).thenReturn(20);
        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(unit), DELTA);
        assertEquals(10, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(unit));
    }

    @Test
    void testWarshipFront() {
        Warship unit = Mockito.mock(Warship.class);
        Mockito.when(unit.getArmor(Warship.LOC_NOSE)).thenReturn(10);
        Mockito.when(unit.getOArmor(Warship.LOC_NOSE)).thenReturn(20);
        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(unit), DELTA);
        assertEquals(10, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(unit));
    }

    @Test
    void testGunEmplacementFront() {
        GunEmplacement unit = Mockito.mock(GunEmplacement.class);
        Mockito.when(unit.getArmorRemainingPercent()).thenReturn(0.8);
        Mockito.when(unit.getTotalArmor()).thenReturn(80);
        assertEquals(0.8f, EntityFeatureUtils.getTargetFrontHealthStats(unit), DELTA);
        assertEquals(80, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(unit));
    }

    @Test
    void testInfantryFront() {
        Infantry unit = Mockito.mock(Infantry.class);
        Mockito.when(unit.getInternalRemainingPercent()).thenReturn(0.9);
        Mockito.when(unit.getTotalInternal()).thenReturn(9);
        assertEquals(0.9f, EntityFeatureUtils.getTargetFrontHealthStats(unit), DELTA);
        assertEquals(9, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(unit));
    }

    @Test
    void testAeroBack() {
        Aero unit = Mockito.mock(Aero.class);
        Mockito.when(unit.getArmor(Aero.LOC_AFT)).thenReturn(5);
        Mockito.when(unit.getOArmor(Aero.LOC_AFT)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetBackHealthStats(unit), DELTA);
    }

    @Test
    void testTankBack() {
        Tank unit = Mockito.mock(Tank.class);
        Mockito.when(unit.getArmor(Tank.LOC_REAR)).thenReturn(5);
        Mockito.when(unit.getOArmor(Tank.LOC_REAR)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetBackHealthStats(unit), DELTA);
    }

    @Test
    void testJumpshipBack() {
        Jumpship unit = Mockito.mock(Jumpship.class);
        Mockito.when(unit.getArmor(Jumpship.LOC_AFT)).thenReturn(5);
        Mockito.when(unit.getArmor(Jumpship.LOC_ARS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Jumpship.LOC_FRS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Jumpship.LOC_ARS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetBackHealthStats(unit), DELTA);
    }

    @Test
    void testAeroSide() {
        Aero unit = Mockito.mock(Aero.class);
        Mockito.when(unit.getArmor(Aero.LOC_LEFT_WING)).thenReturn(5);
        Mockito.when(unit.getOArmor(Aero.LOC_LEFT_WING)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(unit), DELTA);

        Mockito.when(unit.getArmor(Aero.LOC_RIGHT_WING)).thenReturn(5);
        Mockito.when(unit.getOArmor(Aero.LOC_RIGHT_WING)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(unit), DELTA);
    }

    @Test
    void testWarshipSide() {
        Warship unit = Mockito.mock(Warship.class);
        Mockito.when(unit.getArmor(Warship.LOC_LBS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Warship.LOC_LBS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(unit), DELTA);

        Mockito.when(unit.getArmor(Warship.LOC_RBS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Warship.LOC_RBS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(unit), DELTA);
    }

    @Test
    void testTankSide() {
        Tank unit = Mockito.mock(Tank.class);
        Mockito.when(unit.getArmor(Tank.LOC_LEFT)).thenReturn(5);
        Mockito.when(unit.getOArmor(Tank.LOC_LEFT)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(unit), DELTA);

        Mockito.when(unit.getArmor(Tank.LOC_RIGHT)).thenReturn(5);
        Mockito.when(unit.getOArmor(Tank.LOC_RIGHT)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(unit), DELTA);
    }

    @Test
    void testSuperHeavyTankBack() {
        SuperHeavyTank unit = Mockito.mock(SuperHeavyTank.class);
        Mockito.when(unit.getArmor(SuperHeavyTank.LOC_REAR)).thenReturn(5);
        Mockito.when(unit.getOArmor(SuperHeavyTank.LOC_REAR)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetBackHealthStats(unit), DELTA);
    }

    @Test
    void testSpaceStationBack() {
        SpaceStation unit = Mockito.mock(SpaceStation.class);
        Mockito.when(unit.getArmor(SpaceStation.LOC_FRS)).thenReturn(5);
        Mockito.when(unit.getArmor(SpaceStation.LOC_ARS)).thenReturn(5);
        Mockito.when(unit.getOArmor(SpaceStation.LOC_FRS)).thenReturn(10);
        Mockito.when(unit.getOArmor(SpaceStation.LOC_ARS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetBackHealthStats(unit), DELTA);
    }

    @Test
    void testWarshipBack() {
        Warship unit = Mockito.mock(Warship.class);
        Mockito.when(unit.getArmor(Warship.LOC_FRS)).thenReturn(5);
        Mockito.when(unit.getArmor(Warship.LOC_ARS)).thenReturn(5);
        Mockito.when(unit.getArmor(Warship.LOC_RBS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Warship.LOC_FRS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Warship.LOC_ARS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Warship.LOC_RBS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetBackHealthStats(unit), DELTA);
    }

    @Test
    void testSuperHeavyTankSide() {
        SuperHeavyTank unit = Mockito.mock(SuperHeavyTank.class);
        Mockito.when(unit.getArmor(SuperHeavyTank.LOC_FRONT_LEFT)).thenReturn(5);
        Mockito.when(unit.getArmor(SuperHeavyTank.LOC_REAR_LEFT)).thenReturn(5);
        Mockito.when(unit.getOArmor(SuperHeavyTank.LOC_FRONT_LEFT)).thenReturn(10);
        Mockito.when(unit.getOArmor(SuperHeavyTank.LOC_REAR_LEFT)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(unit), DELTA);

        Mockito.when(unit.getArmor(SuperHeavyTank.LOC_FRONT_RIGHT)).thenReturn(5);
        Mockito.when(unit.getArmor(SuperHeavyTank.LOC_REAR_RIGHT)).thenReturn(5);
        Mockito.when(unit.getOArmor(SuperHeavyTank.LOC_FRONT_RIGHT)).thenReturn(10);
        Mockito.when(unit.getOArmor(SuperHeavyTank.LOC_REAR_RIGHT)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(unit), DELTA);
    }

    @Test
    void testJumpshipSide() {
        Jumpship unit = Mockito.mock(Jumpship.class);
        Mockito.when(unit.getArmor(Jumpship.LOC_FLS)).thenReturn(5);
        Mockito.when(unit.getArmor(Jumpship.LOC_ALS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Jumpship.LOC_FLS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Jumpship.LOC_ALS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(unit), DELTA);

        Mockito.when(unit.getArmor(Jumpship.LOC_FRS)).thenReturn(5);
        Mockito.when(unit.getArmor(Jumpship.LOC_ARS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Jumpship.LOC_FRS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Jumpship.LOC_ARS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(unit), DELTA);
    }

    @Test
    void testSpaceStationSide() {
        SpaceStation unit = Mockito.mock(SpaceStation.class);
        Mockito.when(unit.getArmor(SpaceStation.LOC_FLS)).thenReturn(5);
        Mockito.when(unit.getArmor(SpaceStation.LOC_ALS)).thenReturn(5);
        Mockito.when(unit.getOArmor(SpaceStation.LOC_FLS)).thenReturn(10);
        Mockito.when(unit.getOArmor(SpaceStation.LOC_ALS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(unit), DELTA);

        Mockito.when(unit.getArmor(SpaceStation.LOC_FRS)).thenReturn(5);
        Mockito.when(unit.getArmor(SpaceStation.LOC_ARS)).thenReturn(5);
        Mockito.when(unit.getOArmor(SpaceStation.LOC_FRS)).thenReturn(10);
        Mockito.when(unit.getOArmor(SpaceStation.LOC_ARS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(unit), DELTA);
    }

    @Test
    void testWarshipLeftSide() {
        Warship unit = Mockito.mock(Warship.class);
        Mockito.when(unit.getArmor(Warship.LOC_FLS)).thenReturn(5);
        Mockito.when(unit.getArmor(Warship.LOC_ALS)).thenReturn(5);
        Mockito.when(unit.getArmor(Warship.LOC_LBS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Warship.LOC_FLS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Warship.LOC_ALS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Warship.LOC_LBS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetLeftSideHealthStats(unit), DELTA);
    }

    @Test
    void testWarshipRightSide() {
        Warship unit = Mockito.mock(Warship.class);
        Mockito.when(unit.getArmor(Warship.LOC_FRS)).thenReturn(5);
        Mockito.when(unit.getArmor(Warship.LOC_ARS)).thenReturn(5);
        Mockito.when(unit.getArmor(Warship.LOC_RBS)).thenReturn(5);
        Mockito.when(unit.getOArmor(Warship.LOC_FRS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Warship.LOC_ARS)).thenReturn(10);
        Mockito.when(unit.getOArmor(Warship.LOC_RBS)).thenReturn(10);
        assertEquals(0.5f, EntityFeatureUtils.getTargetRightSideHealthStats(unit), DELTA);
    }

    @Test
    void testHierarchyDispatch() {
        // Use a generic mock of Mek to verify it uses Mek's calculator
        Mek mockMek = Mockito.mock(Mek.class);
        Mockito.when(mockMek.getArmor(Mek.LOC_CENTER_TORSO)).thenReturn(10);
        Mockito.when(mockMek.getOArmor(Mek.LOC_CENTER_TORSO)).thenReturn(20);

        assertEquals(0.5f, EntityFeatureUtils.getTargetFrontHealthStats(mockMek), DELTA);
    }
}
