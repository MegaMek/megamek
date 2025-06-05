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
package megamek.ai.dataset;

import megamek.common.MapSettings;

/**
 * Flexible container for map settings data using a map-based approach with enum keys.
 * @author Luana Coppio
 */
public class MapSettingsData extends EntityDataMap<MapSettingsData.Field> {

    /**
     * Enum defining all available map settings fields.
     */
    public enum Field {
        THEME,
        CITY_TYPE,
        MAP_SIZE,
        MAP_WIDTH,
        MAP_HEIGHT
    }

    /**
     * Creates an empty MapSettingsData.
     */
    public MapSettingsData() {
        super(Field.class);
    }

    /**
     * Creates a MapSettingsData from MapSettings.
     * @param settings The map settings to extract data from
     * @return A populated MapSettingsData
     */
    public static MapSettingsData fromMapSettings(MapSettings settings) {
        MapSettingsData data = new MapSettingsData();

        data.put(Field.THEME, settings.getTheme())
              .put(Field.CITY_TYPE, settings.getCityType())
              .put(Field.MAP_SIZE, settings.getBoardSize())
              .put(Field.MAP_WIDTH, settings.getMapWidth())
              .put(Field.MAP_HEIGHT, settings.getMapHeight());

        return data;
    }
}
