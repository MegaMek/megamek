/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
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
 */
package megamek.ai.dataset;

import megamek.common.planetaryconditions.PlanetaryConditions;

/**
 * Flexible container for planetary conditions data using a map-based approach with enum keys.
 * @author Luana Coppio
 */
public class PlanetaryConditionsData extends EntityDataMap<PlanetaryConditionsData.Field> {

    /**
     * Enum defining all available planetary conditions fields.
     */
    public enum Field {
        TEMPERATURE,
        WEATHER,
        GRAVITY,
        WIND,
        ATMOSPHERE,
        FOG,
        LIGHT
    }

    /**
     * Creates an empty PlanetaryConditionsData.
     */
    public PlanetaryConditionsData() {
        super(Field.class);
    }

    /**
     * Creates a PlanetaryConditionsData from PlanetaryConditions.
     * @param conditions The planetary conditions to extract data from
     * @return A populated PlanetaryConditionsData
     */
    public static PlanetaryConditionsData fromPlanetaryConditions(PlanetaryConditions conditions) {
        PlanetaryConditionsData data = new PlanetaryConditionsData();

        data.put(Field.TEMPERATURE, conditions.getTemperature())
              .put(Field.WEATHER, conditions.getWeather().name())
              .put(Field.GRAVITY, conditions.getGravity())
              .put(Field.WIND, conditions.getWind().name())
              .put(Field.ATMOSPHERE, conditions.getAtmosphere().name())
              .put(Field.FOG, conditions.getFog().name())
              .put(Field.LIGHT, conditions.getLight().name());

        return data;
    }
}
