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

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the sensor families partition the {@link Sensor} types. The sensor preference in the client settings
 * ranks families rather than raw sensor types, so a sensor type that no family covers would be unreachable by the
 * preference and a sensor type covered twice would make the ranking ambiguous.
 */
class SensorFamilyTest {

    @Test
    void everySensorTypeBelongsToExactlyOneFamily() {
        // Built here rather than in the enum: the coverage view exists only to be checked, so it belongs in the
        // test rather than in the shipped class.
        List<Integer> covered = new ArrayList<>();
        List<Integer> expected = new ArrayList<>();
        for (int sensorType = 0; sensorType < Sensor.SIZE; sensorType++) {
            expected.add(sensorType);
            for (SensorFamily family : SensorFamily.values()) {
                if (family.covers(sensorType)) {
                    covered.add(sensorType);
                }
            }
        }

        assertEquals(expected, covered,
              "Every Sensor type must be covered by exactly one SensorFamily, in ascending order with no gaps "
                    + "and no duplicates. A newly added Sensor type needs a family.");
    }

    @Test
    void familyOfResolvesEverySensorType() {
        for (int sensorType = 0; sensorType < Sensor.SIZE; sensorType++) {
            SensorFamily family = SensorFamily.familyOf(sensorType);
            assertNotNull(family, "No family for sensor type " + sensorType);
            assertTrue(family.covers(sensorType),
                  "Family " + family + " was returned for sensor type " + sensorType + " but does not cover it");
        }
    }

    @Test
    void familyOfRejectsAnUnknownSensorType() {
        assertThrows(IllegalArgumentException.class, () -> SensorFamily.familyOf(-1));
        assertThrows(IllegalArgumentException.class, () -> SensorFamily.familyOf(Sensor.SIZE));
    }

    @Test
    void familyOfAcceptsASensor() {
        assertSame(SensorFamily.INFRARED, SensorFamily.familyOf(new Sensor(Sensor.TYPE_MEK_IR)));
        assertSame(SensorFamily.INFRARED, SensorFamily.familyOf(new Sensor(Sensor.TYPE_VEE_IR)));
    }

    @Test
    void everyProbeSensorIsInTheActiveProbeFamily() {
        for (int sensorType = 0; sensorType < Sensor.SIZE; sensorType++) {
            if (new Sensor(sensorType).isBAP()) {
                assertSame(SensorFamily.ACTIVE_PROBE, SensorFamily.familyOf(sensorType),
                      "Sensor type " + sensorType + " reports as an active probe but is not in the "
                            + "ACTIVE_PROBE family");
            }
        }
    }

    @Test
    void defaultOrderHoldsEveryFamilyOnce() {
        List<SensorFamily> defaultOrder = SensorFamily.defaultOrder();

        assertEquals(SensorFamily.values().length, defaultOrder.size(),
              "The default order must list every family");
        assertEquals(defaultOrder.size(), defaultOrder.stream().distinct().count(),
              "The default order must not repeat a family");
    }

    @Test
    void defaultOrderPrefersProbesAndRanksRadarBelowTheSpecialistSensors() {
        List<SensorFamily> defaultOrder = SensorFamily.defaultOrder();

        assertEquals(SensorFamily.ACTIVE_PROBE, defaultOrder.getFirst(),
              "Active probes were MegaMek's automatic choice before this preference existed and stay first");
        assertTrue(defaultOrder.indexOf(SensorFamily.RADAR) > defaultOrder.indexOf(SensorFamily.INFRARED),
              "Radar must rank below infrared; radar being the automatic fallback is the complaint this "
                    + "preference answers");
        assertTrue(defaultOrder.indexOf(SensorFamily.RADAR) > defaultOrder.indexOf(SensorFamily.MAGSCAN),
              "Radar must rank below magscan");
        assertTrue(defaultOrder.indexOf(SensorFamily.RADAR) > defaultOrder.indexOf(SensorFamily.SEISMIC),
              "Radar must rank below seismic");
    }

    @Test
    void aMekCarriesOneSensorFromEachOfFourDistinctFamilies() {
        // The four sensors MekFileParser gives every Mek
        List<SensorFamily> families = List.of(
              SensorFamily.familyOf(Sensor.TYPE_MEK_RADAR),
              SensorFamily.familyOf(Sensor.TYPE_MEK_IR),
              SensorFamily.familyOf(Sensor.TYPE_MEK_MAG_SCAN),
              SensorFamily.familyOf(Sensor.TYPE_MEK_SEISMIC));

        assertEquals(4, families.stream().distinct().count(),
              "A Mek's four sensors must fall into four different families, or the preference cannot tell them "
                    + "apart");
    }

    @Test
    void aMekAndAVehicleShareTheSameFamiliesForTheSameSensors() {
        assertSame(SensorFamily.familyOf(Sensor.TYPE_MEK_RADAR), SensorFamily.familyOf(Sensor.TYPE_VEE_RADAR));
        assertSame(SensorFamily.familyOf(Sensor.TYPE_MEK_IR), SensorFamily.familyOf(Sensor.TYPE_VEE_IR));
        assertSame(SensorFamily.familyOf(Sensor.TYPE_MEK_MAG_SCAN), SensorFamily.familyOf(Sensor.TYPE_VEE_MAG_SCAN));
        assertSame(SensorFamily.familyOf(Sensor.TYPE_MEK_SEISMIC), SensorFamily.familyOf(Sensor.TYPE_VEE_SEISMIC));
    }

    @Test
    void anAeroChoosesBetweenTwoDistinctFamilies() {
        // MekFileParser gives ASFs and small craft a thermal/optical suite and an active sensor suite
        assertSame(SensorFamily.THERMAL_OPTICAL, SensorFamily.familyOf(Sensor.TYPE_AERO_THERMAL));
        assertSame(SensorFamily.RADAR, SensorFamily.familyOf(Sensor.TYPE_AERO_SENSOR));
    }
}
