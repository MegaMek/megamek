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

package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for PlanetaryConditionsData and its serializer.
 */
class PlanetaryConditionsDataTest {

    @Test
    void testFromPlanetaryConditions() {
        PlanetaryConditions mockConditions = Mockito.mock(PlanetaryConditions.class);
        Mockito.when(mockConditions.getTemperature()).thenReturn(25);
        Mockito.when(mockConditions.getWeather()).thenReturn(Weather.CLEAR);
        Mockito.when(mockConditions.getGravity()).thenReturn(1.0f);
        Mockito.when(mockConditions.getWind()).thenReturn(Wind.CALM);
        Mockito.when(mockConditions.getAtmosphere()).thenReturn(Atmosphere.STANDARD);
        Mockito.when(mockConditions.getFog()).thenReturn(Fog.FOG_NONE);
        Mockito.when(mockConditions.getLight()).thenReturn(Light.DAY);

        PlanetaryConditionsData data = PlanetaryConditionsData.fromPlanetaryConditions(mockConditions);

        assertEquals(25, data.get(PlanetaryConditionsData.Field.TEMPERATURE));
        assertEquals("CLEAR", data.get(PlanetaryConditionsData.Field.WEATHER));
        assertEquals(1.0f, data.get(PlanetaryConditionsData.Field.GRAVITY));
        assertEquals("CALM", data.get(PlanetaryConditionsData.Field.WIND));
        assertEquals("STANDARD", data.get(PlanetaryConditionsData.Field.ATMOSPHERE));
        assertEquals("FOG_NONE", data.get(PlanetaryConditionsData.Field.FOG));
        assertEquals("DAY", data.get(PlanetaryConditionsData.Field.LIGHT));
    }

    @Test
    void testSerializer() {
        PlanetaryConditionsData data = new PlanetaryConditionsData();
        data.put(PlanetaryConditionsData.Field.TEMPERATURE, 20)
              .put(PlanetaryConditionsData.Field.GRAVITY, 0.9f);

        PlanetaryConditionsDataSerializer serializer = new PlanetaryConditionsDataSerializer();
        String serialized = serializer.serialize(data);

        assertNotNull(serialized);
        assertTrue(serialized.contains("20"));
        assertTrue(serialized.contains("0.90")); // format handler for float
    }
}
