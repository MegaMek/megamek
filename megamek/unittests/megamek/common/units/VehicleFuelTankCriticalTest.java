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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import megamek.common.equipment.Engine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** The Fuel Tank row of the ground vehicle and VTOL critical hit tables (TW pp. 195, 197–198). */
class VehicleFuelTankCriticalTest {

    static Stream<Arguments> powerplants() {
        return Stream.of(
              Arguments.of(Engine.COMBUSTION_ENGINE, Tank.CRIT_FUEL_TANK),
              Arguments.of(Engine.FUEL_CELL, Tank.CRIT_FUEL_TANK),
              Arguments.of(Engine.FISSION, Tank.CRIT_ENGINE),
              Arguments.of(Engine.NORMAL_ENGINE, Tank.CRIT_ENGINE));
    }

    @ParameterizedTest
    @MethodSource("powerplants")
    void sideAndRearFuelTankResultsDependOnPowerplant(int engineType, int expected) {
        for (Tank vehicle : List.of(new Tank(), new VTOL())) {
            vehicle.setEngine(new Engine(100, engineType, Engine.TANK_ENGINE));
            assertRollTwelve(vehicle, Tank.LOC_LEFT, expected);
            assertRollTwelve(vehicle, Tank.LOC_REAR, expected);
        }
    }

    @Test
    void aVehicleWithoutAnEngineHasNoFuelTankCritical() {
        SupportTank trailer = new SupportTank();
        trailer.setEngine(new Engine(0, Engine.NONE, Engine.SUPPORT_VEE_ENGINE | Engine.TANK_ENGINE));
        for (Tank vehicle : List.of(new Tank(), new VTOL(), trailer)) {
            assertRollTwelve(vehicle, Tank.LOC_LEFT, Tank.CRIT_NONE);
            assertRollTwelve(vehicle, Tank.LOC_REAR, Tank.CRIT_NONE);
        }
    }

    @Test
    void batteryPoweredSupportVehiclesUseTheirOwnCriticalTable() {
        int flags = Engine.SUPPORT_VEE_ENGINE | Engine.TANK_ENGINE;
        SupportTank tank = new SupportTank();
        tank.setEngine(new Engine(0, Engine.BATTERY, flags));
        // Ground Fuel Tank skips this powerplant. The bare side wraps to Crew Stunned, the rear to Engine Hit.
        assertRollTwelve(tank, Tank.LOC_LEFT, Tank.CRIT_CREW_STUNNED);
        assertRollTwelve(tank, Tank.LOC_REAR, Tank.CRIT_ENGINE);

        SupportVTOL vtol = new SupportVTOL();
        vtol.setEngine(new Engine(0, Engine.BATTERY, flags));
        assertRollTwelve(vtol, Tank.LOC_LEFT, Tank.CRIT_FUEL_TANK);
        assertRollTwelve(vtol, Tank.LOC_REAR, Tank.CRIT_FUEL_TANK);
    }

    @ParameterizedTest
    @ValueSource(ints = { Engine.NORMAL_ENGINE, Engine.FISSION })
    void aDestroyedNuclearEngineWrapsToTheFirstApplicableCriticalInTheSameColumn(int engineType) {
        Tank tank = new Tank();
        tank.setEngine(new Engine(100, engineType, Engine.TANK_ENGINE));
        tank.engineHit();
        // With a live crew, no cargo, weapons or ammo, the first applicable side result is Crew Stunned.
        assertRollTwelve(tank, Tank.LOC_LEFT, Tank.CRIT_CREW_STUNNED);
        assertRollTwelve(tank, Tank.LOC_REAR, Tank.CRIT_NONE);

        VTOL vtol = new VTOL();
        vtol.setEngine(new Engine(100, engineType, Engine.TANK_ENGINE));
        vtol.engineHit();
        assertRollTwelve(vtol, Tank.LOC_LEFT, Tank.CRIT_NONE);
        assertRollTwelve(vtol, Tank.LOC_REAR, Tank.CRIT_SENSOR);
    }

    static Stream<Arguments> sixFacingChassis() {
        return Stream.of(
              Arguments.of(new LargeSupportTank(), LargeSupportTank.LOC_REAR_RIGHT, LargeSupportTank.LOC_REAR),
              Arguments.of(new SuperHeavyTank(), SuperHeavyTank.LOC_REAR_RIGHT, SuperHeavyTank.LOC_REAR));
    }

    @ParameterizedTest
    @MethodSource("sixFacingChassis")
    void aRepeatedFissionHitUsesTheRearOrSideColumnOfTheSixFacingChassis(Tank tank, int rearRight, int rear) {
        int flags = tank.isSupportVehicle() ? Engine.TANK_ENGINE | Engine.SUPPORT_VEE_ENGINE : Engine.TANK_ENGINE;
        tank.setEngine(new Engine(100, Engine.FISSION, flags));
        tank.engineHit();
        assertRollTwelve(tank, rearRight, Tank.CRIT_CREW_STUNNED);
        assertRollTwelve(tank, rear, Tank.CRIT_NONE);
    }

    private static void assertRollTwelve(Tank vehicle, int location, int expected) {
        assertEquals(expected, vehicle.getCriticalEffect(12, location, false),
              () -> vehicle.getClass().getSimpleName() + " " + vehicle.getLocationName(location));
    }
}
