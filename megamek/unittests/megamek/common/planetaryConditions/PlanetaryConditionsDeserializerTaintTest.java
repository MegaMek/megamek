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

package megamek.common.planetaryConditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a scenario file can set the atmospheric taint through its {@code planetaryconditions} block.
 */
class PlanetaryConditionsDeserializerTaintTest {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private PlanetaryConditions readConditions(String yaml) throws IOException {
        return yamlMapper.readValue(yaml, PlanetaryConditions.class);
    }

    @Test
    @DisplayName("A scenario that names a taint gets that taint")
    void taintIsReadFromTheScenario() throws IOException {
        assertEquals(AtmosphericTaint.CAUSTIC_TAINTED, readConditions("taint: caustic tainted").getAtmosphericTaint());
        assertEquals(AtmosphericTaint.CAUSTIC_TOXIC, readConditions("taint: caustic toxic").getAtmosphericTaint());
        assertEquals(AtmosphericTaint.RADIOLOGICAL_TAINTED,
              readConditions("taint: radiological tainted").getAtmosphericTaint());
        assertEquals(AtmosphericTaint.RADIOLOGICAL_TOXIC,
              readConditions("taint: radiological toxic").getAtmosphericTaint());
        assertEquals(AtmosphericTaint.FLAMMABLE_TAINTED,
              readConditions("taint: flammable tainted").getAtmosphericTaint());
        assertEquals(AtmosphericTaint.FLAMMABLE_TOXIC,
              readConditions("taint: flammable toxic").getAtmosphericTaint());
        assertEquals(AtmosphericTaint.BREATHABLE, readConditions("taint: breathable").getAtmosphericTaint());
    }

    @Test
    @DisplayName("A scenario that says nothing about the air gets breathable air")
    void missingTaintIsBreathable() throws IOException {
        assertEquals(AtmosphericTaint.BREATHABLE, readConditions("temperature: 25").getAtmosphericTaint());
    }

    @Test
    @DisplayName("The taint is independent of the atmospheric pressure")
    void taintAndPressureAreSetSeparately() throws IOException {
        PlanetaryConditions conditions = readConditions("pressure: thin\ntaint: flammable toxic");

        assertEquals(Atmosphere.THIN, conditions.getAtmosphere());
        assertEquals(AtmosphericTaint.FLAMMABLE_TOXIC, conditions.getAtmosphericTaint());
    }
    @Test
    @DisplayName("A scenario that misspells the taint gets breathable air rather than a crash")
    void anUnrecognisedTaintLeavesTheAirBreathable() throws IOException {
        // Storing the unmatched lookup would leave the taint null, and every later getAtmosphericTaint() call
        // dereferences it, so a typo in a scenario file would take the game down rather than be ignored.
        PlanetaryConditions conditions = readConditions("taint: flamable toxic");

        assertNotNull(conditions.getAtmosphericTaint(), "a misspelled taint must not leave the field null");
        assertEquals(AtmosphericTaint.BREATHABLE, conditions.getAtmosphericTaint());
    }
}
