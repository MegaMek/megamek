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

import megamek.common.loaders.MapSettings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for MapSettingsData and its serializer.
 */
class MapSettingsDataTest {

    @Test
    void testFromMapSettings() {
        MapSettings mockSettings = Mockito.mock(MapSettings.class);
        Mockito.when(mockSettings.getTheme()).thenReturn("Desert");
        Mockito.when(mockSettings.getCityType()).thenReturn("Industrial");
        Mockito.when(mockSettings.getBoardSize()).thenReturn(new megamek.common.board.BoardDimensions(1, 1));
        Mockito.when(mockSettings.getMapWidth()).thenReturn(16);
        Mockito.when(mockSettings.getMapHeight()).thenReturn(17);

        MapSettingsData data = MapSettingsData.fromMapSettings(mockSettings);

        assertEquals("Desert", data.get(MapSettingsData.Field.THEME));
        assertEquals("Industrial", data.get(MapSettingsData.Field.CITY_TYPE));
        assertEquals(new megamek.common.board.BoardDimensions(1, 1), data.get(MapSettingsData.Field.MAP_SIZE));
        assertEquals(16, data.get(MapSettingsData.Field.MAP_WIDTH));
        assertEquals(17, data.get(MapSettingsData.Field.MAP_HEIGHT));
    }

    @Test
    void testSerializer() {
        MapSettingsData data = new MapSettingsData();
        data.put(MapSettingsData.Field.THEME, "Snow")
              .put(MapSettingsData.Field.MAP_WIDTH, 15);

        MapSettingsDataSerializer serializer = new MapSettingsDataSerializer();
        String serialized = serializer.serialize(data);

        assertNotNull(serialized);
        assertTrue(serialized.contains("Snow"));
        assertTrue(serialized.contains("15"));
    }
}
