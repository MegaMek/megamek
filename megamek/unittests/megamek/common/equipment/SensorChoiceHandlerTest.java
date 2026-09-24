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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the saved per-design sensor choices that let a preference follow a chassis into a MekHQ campaign.
 *
 * <p>These tests exercise the in-memory map only. Writing the file is not exercised here because it would touch the
 * user's real {@code mmconf} directory.</p>
 */
class SensorChoiceHandlerTest {

    private static final String CHASSIS = "TestSensorChassis";
    private static final String MODEL = "TSC-1";

    @AfterEach
    void forgetTestDesigns() {
        SensorChoiceHandler.setSensorChoice(CHASSIS, MODEL, null);
        SensorChoiceHandler.setSensorChoice(CHASSIS, "TSC-2", null);
    }

    @Test
    @DisplayName("A design with nothing saved returns nothing")
    void aDesignWithNothingSavedReturnsNothing() {
        assertNull(SensorChoiceHandler.getSensorChoice(CHASSIS, MODEL),
              "A design nobody has configured must keep its normal default");
    }

    @Test
    @DisplayName("A saved choice comes back")
    void aSavedChoiceComesBack() {
        SensorChoiceHandler.setSensorChoice(CHASSIS, MODEL, SensorFamily.INFRARED);

        assertSame(SensorFamily.INFRARED, SensorChoiceHandler.getSensorChoice(CHASSIS, MODEL));
    }

    @Test
    @DisplayName("Saving again replaces the previous choice")
    void savingAgainReplacesThePreviousChoice() {
        SensorChoiceHandler.setSensorChoice(CHASSIS, MODEL, SensorFamily.INFRARED);
        SensorChoiceHandler.setSensorChoice(CHASSIS, MODEL, SensorFamily.SEISMIC);

        assertSame(SensorFamily.SEISMIC, SensorChoiceHandler.getSensorChoice(CHASSIS, MODEL));
    }

    @Test
    @DisplayName("Saving null forgets the design")
    void savingNullForgetsTheDesign() {
        SensorChoiceHandler.setSensorChoice(CHASSIS, MODEL, SensorFamily.MAGSCAN);
        SensorChoiceHandler.setSensorChoice(CHASSIS, MODEL, null);

        assertNull(SensorChoiceHandler.getSensorChoice(CHASSIS, MODEL),
              "Unticking the lobby checkbox must put the design back to its normal default");
    }

    @Test
    @DisplayName("Models of one chassis are saved separately")
    void modelsOfOneChassisAreSavedSeparately() {
        SensorChoiceHandler.setSensorChoice(CHASSIS, MODEL, SensorFamily.INFRARED);
        SensorChoiceHandler.setSensorChoice(CHASSIS, "TSC-2", SensorFamily.SEISMIC);

        assertSame(SensorFamily.INFRARED, SensorChoiceHandler.getSensorChoice(CHASSIS, MODEL));
        assertSame(SensorFamily.SEISMIC, SensorChoiceHandler.getSensorChoice(CHASSIS, "TSC-2"),
              "A variant must be able to differ from its sibling");
    }

    @Test
    @DisplayName("A design with no chassis or model is not saved")
    void aDesignWithNoChassisOrModelIsNotSaved() {
        // The key is built from chassis and model, so a unit missing either has nothing to save against
        SensorChoiceHandler.setSensorChoice("", MODEL, SensorFamily.RADAR);
        SensorChoiceHandler.setSensorChoice(CHASSIS, "", SensorFamily.RADAR);

        assertNull(SensorChoiceHandler.getSensorChoice("", MODEL));
        assertNull(SensorChoiceHandler.getSensorChoice(CHASSIS, ""));
    }

    @Test
    @DisplayName("The saved family resolves to a sensor the unit carries")
    void theSavedFamilyResolvesToASensorTheUnitCarries() {
        // This is what MekFileParser does with the saved value: walk the unit's sensors for one the family covers.
        // A Mek carries Mek IR, so a saved Infrared resolves; it carries no ESM, so a saved ESM does not.
        SensorFamily saved = SensorFamily.INFRARED;

        assertEquals(true, saved.covers(Sensor.TYPE_MEK_IR),
              "Infrared must resolve to the Mek's own infrared sensor");
        assertEquals(false, SensorFamily.ESM.covers(Sensor.TYPE_MEK_IR),
              "A family the design cannot supply must not match, so the unit keeps its default");
    }
}
