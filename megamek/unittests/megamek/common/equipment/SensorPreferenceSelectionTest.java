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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.exceptions.LocationFullException;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies which sensor a unit is given under a player's sensor preference.
 *
 * <p>The unit under test is built the way {@code MekFileParser} builds one: a Mek gets radar, infrared, magscan and
 * seismic, in that order, and starts on radar. That starting radar is the behaviour RFE #5868 complains about, so
 * every test below states what the preference changes relative to it.</p>
 */
class SensorPreferenceSelectionTest {

    private static final int MEK_RADAR_INDEX = 0;
    private static final int MEK_IR_INDEX = 1;
    private static final int MEK_MAG_SCAN_INDEX = 2;
    private static final int MEK_SEISMIC_INDEX = 3;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Builds a Mek with the four sensors every Mek gets, starting on radar, exactly as the unit file parser does.
     */
    private static Mek mekWithStandardSensors() {
        Mek mek = new BipedMek();
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_RADAR));
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_IR));
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_MAG_SCAN));
        mek.getSensors().add(new Sensor(Sensor.TYPE_MEK_SEISMIC));
        mek.setNextSensor(mek.getSensors().firstElement());
        return mek;
    }

    private static List<SensorFamily> orderStartingWith(SensorFamily... preferred) {
        List<SensorFamily> order = new ArrayList<>(List.of(preferred));
        for (SensorFamily family : SensorFamily.defaultOrder()) {
            if (!order.contains(family)) {
                order.add(family);
            }
        }
        return order;
    }

    @Test
    @DisplayName("A Mek with no probe takes the highest ranked sensor it carries")
    void aMekTakesTheHighestRankedSensorItCarries() {
        Mek mek = mekWithStandardSensors();

        assertEquals(MEK_IR_INDEX,
              SensorFamily.preferredSensorIndex(mek, orderStartingWith(SensorFamily.INFRARED)),
              "Ranking Infrared first must move the Mek off its starting radar and onto infrared");
        assertEquals(MEK_MAG_SCAN_INDEX,
              SensorFamily.preferredSensorIndex(mek, orderStartingWith(SensorFamily.MAGSCAN)),
              "Ranking Magscan first must pick magscan");
        assertEquals(MEK_SEISMIC_INDEX,
              SensorFamily.preferredSensorIndex(mek, orderStartingWith(SensorFamily.SEISMIC)),
              "Ranking Seismic first must pick seismic");
    }

    @Test
    @DisplayName("A family the unit does not carry is skipped for the next one it does")
    void aFamilyTheUnitDoesNotCarryIsSkipped() {
        Mek mek = mekWithStandardSensors();

        // A Mek carries no ESM and no thermal/optical sensors, so both are passed over
        List<SensorFamily> order = orderStartingWith(SensorFamily.ESM,
              SensorFamily.THERMAL_OPTICAL,
              SensorFamily.SEISMIC);

        assertEquals(MEK_SEISMIC_INDEX, SensorFamily.preferredSensorIndex(mek, order),
              "Families the unit cannot supply must be skipped rather than blocking the choice");
    }

    @Test
    @DisplayName("Nothing is changed when the unit already uses the preferred sensor")
    void nothingIsChangedWhenTheSensorIsAlreadyCorrect() {
        Mek mek = mekWithStandardSensors();

        // The Mek already starts on radar
        assertEquals(-1, SensorFamily.preferredSensorIndex(mek, orderStartingWith(SensorFamily.RADAR)),
              "A unit already using the preferred sensor needs no change and no packet sent");
    }

    @Test
    @DisplayName("The default order moves a Mek off radar and onto infrared")
    void theDefaultOrderMovesAMekOffRadar() {
        Mek mek = mekWithStandardSensors();

        assertEquals(MEK_IR_INDEX, SensorFamily.preferredSensorIndex(mek, SensorFamily.defaultOrder()),
              "Out of the box the preference must fix the complaint in RFE #5868: a Mek stops defaulting to radar");
    }

    @Test
    @DisplayName("A unit with nothing to choose from is left alone")
    void aUnitWithNothingToChooseFromIsLeftAlone() {
        Mek noSensors = new BipedMek();
        assertEquals(-1, SensorFamily.preferredSensorIndex(noSensors, SensorFamily.defaultOrder()),
              "A unit with no sensors has no choice to make");

        Mek oneSensor = new BipedMek();
        oneSensor.getSensors().add(new Sensor(Sensor.TYPE_MEK_RADAR));
        oneSensor.setNextSensor(oneSensor.getSensors().firstElement());
        assertEquals(-1, SensorFamily.preferredSensorIndex(oneSensor, SensorFamily.defaultOrder()),
              "A unit with a single sensor has no choice to make");
    }

    @Test
    @DisplayName("An installed active probe is chosen when it can be used")
    void anInstalledProbeIsChosenWhenUsable() throws LocationFullException {
        Mek mek = mekWithStandardSensors();
        EquipmentType beagle = EquipmentType.get(Sensor.BAP);
        assertNotNull(beagle, "Beagle Active Probe should exist");
        mek.addEquipment(beagle, Mek.LOC_LEFT_TORSO);
        // The unit file parser appends the probe to the sensor list and selects it
        mek.getSensors().add(new Sensor(Sensor.TYPE_BAP));

        assertTrue(mek.hasBAP(false), "The mounted probe should be usable");
        assertEquals(4, SensorFamily.preferredSensorIndex(mek, SensorFamily.defaultOrder()),
              "Active Probe ranks first by default, so the mounted probe must be chosen");
    }

    @Test
    @DisplayName("A probe that cannot be used is passed over for the next family")
    void anUnusableProbeIsPassedOver() {
        Mek mek = mekWithStandardSensors();
        // A probe listed among the sensors with no usable probe equipment behind it, which is what a destroyed or
        // switched off probe looks like to the selection
        mek.getSensors().add(new Sensor(Sensor.TYPE_BAP));

        assertFalse(mek.hasBAP(false), "No usable probe is mounted");
        assertEquals(MEK_IR_INDEX, SensorFamily.preferredSensorIndex(mek, SensorFamily.defaultOrder()),
              "An unusable probe must not be chosen; the next family the unit can supply wins instead");
    }

    @Test
    @DisplayName("A hand-picked sensor is marked so the preference leaves the unit alone")
    void aHandPickedSensorIsMarked() {
        Entity mek = mekWithStandardSensors();

        assertFalse(mek.hasCustomSensorChoice(),
              "A freshly loaded unit has no hand-picked sensor, so the preference applies to it");

        mek.setNextSensor(mek.getSensors().elementAt(MEK_SEISMIC_INDEX));
        mek.setCustomSensorChoice(true);

        assertTrue(mek.hasCustomSensorChoice(),
              "Picking a sensor by hand must mark the unit so the preference stops overriding it");
        assertEquals(MEK_RADAR_INDEX,
              SensorFamily.preferredSensorIndex(mek, orderStartingWith(SensorFamily.RADAR)),
              "The selection itself still reports what the preference would pick; honouring the mark is the "
                    + "caller's job");
    }
}
