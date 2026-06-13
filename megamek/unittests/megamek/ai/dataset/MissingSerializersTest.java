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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for standard serializers that don't have their own test classes.
 */
class MissingSerializersTest {

    @Test
    void testMapSettingsDataSerializer() {
        MapSettingsData data = new MapSettingsData();
        data.put(MapSettingsData.Field.THEME, "Desert");

        MapSettingsDataSerializer serializer = new MapSettingsDataSerializer();
        String serialized = serializer.serialize(data);

        assertNotNull(serialized);
        assertTrue(serialized.contains("Desert"));
        assertTrue(serialized.contains("MapSettingsData.31052025"));
        assertTrue(serializer.getHeaderLine().contains("THEME"));
    }

    @Test
    void testPlanetaryConditionsDataSerializer() {
        PlanetaryConditionsData data = new PlanetaryConditionsData();
        data.put(PlanetaryConditionsData.Field.TEMPERATURE, 25);

        PlanetaryConditionsDataSerializer serializer = new PlanetaryConditionsDataSerializer();
        String serialized = serializer.serialize(data);

        assertNotNull(serialized);
        assertTrue(serialized.contains("25"));
        assertTrue(serialized.contains("PlanetaryConditionsData.31052025"));
        assertTrue(serializer.getHeaderLine().contains("TEMPERATURE"));
    }

    @Test
    void testUnitActionSerializer() {
        UnitAction data = new UnitAction();
        data.put(UnitAction.Field.CHASSIS, "Atlas");

        UnitActionSerializer serializer = new UnitActionSerializer();
        String serialized = serializer.serialize(data);

        assertNotNull(serialized);
        assertTrue(serialized.contains("Atlas"));
        assertTrue(serialized.contains("UnitAction.31052025"));
        assertTrue(serializer.getHeaderLine().contains("CHASSIS"));
    }

    @Test
    void testUnitAttackSerializer() {
        UnitAttack data = new UnitAttack();
        data.put(UnitAttack.Field.TYPE, "Marauder");

        UnitAttackSerializer serializer = new UnitAttackSerializer();
        String serialized = serializer.serialize(data);

        assertNotNull(serialized);
        assertTrue(serialized.contains("Marauder"));
        assertTrue(serialized.contains("UnitAttack.31052025"));
        assertTrue(serializer.getHeaderLine().contains("TYPE"));
    }
}
