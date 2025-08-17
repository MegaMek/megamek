/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common;

import java.util.HashMap;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;

/**
 * This class contains data and logic for temperature restrictions.
 * TODO: Move the "data" out to user-configurable files?
 */
public class WeatherRestriction {
    private static HashMap<Integer, WeatherRestriction> fogRestrictions;
    private static HashMap<Integer, WeatherRestriction> weatherRestrictions;
    private static HashMap<Integer, WeatherRestriction> windRestrictions;

    private Integer minAtmoLevel;
    private Integer maxTemp;

    static {
        // init fog restrictions
        fogRestrictions = new HashMap<>();

        WeatherRestriction lightFogRestriction = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        fogRestrictions.put(Fog.FOG_LIGHT.ordinal(), lightFogRestriction);

        WeatherRestriction heavyFogRestriction = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        fogRestrictions.put(Fog.FOG_HEAVY.ordinal(), heavyFogRestriction);

        // init weather restrictions
        weatherRestrictions = new HashMap<>();

        WeatherRestriction weatherRestrictionLtHail = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.LIGHT_HAIL.ordinal(), weatherRestrictionLtHail);

        WeatherRestriction weatherRestrictionHvyHail = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.HEAVY_HAIL.ordinal(), weatherRestrictionHvyHail);

        WeatherRestriction weatherRestrictionLtRain = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        weatherRestrictions.put(Weather.LIGHT_RAIN.ordinal(), weatherRestrictionLtRain);

        WeatherRestriction weatherRestrictionMdRain = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        weatherRestrictions.put(Weather.MOD_RAIN.ordinal(), weatherRestrictionMdRain);

        WeatherRestriction weatherRestrictionLtStorm = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        weatherRestrictions.put(Weather.LIGHTNING_STORM.ordinal(), weatherRestrictionLtStorm);

        WeatherRestriction weatherRestrictionHvyRain = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        weatherRestrictions.put(Weather.HEAVY_RAIN.ordinal(), weatherRestrictionHvyRain);

        WeatherRestriction weatherRestrictionGustRain = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        weatherRestrictions.put(Weather.GUSTING_RAIN.ordinal(), weatherRestrictionGustRain);

        WeatherRestriction weatherRestrictionDownpour = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), null);
        weatherRestrictions.put(Weather.DOWNPOUR.ordinal(), weatherRestrictionDownpour);

        WeatherRestriction weatherRestrictionLtSnow = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.LIGHT_SNOW.ordinal(), weatherRestrictionLtSnow);

        WeatherRestriction weatherRestrictionMdSnow = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.MOD_SNOW.ordinal(), weatherRestrictionMdSnow);

        WeatherRestriction weatherRestrictionSleet = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.SLEET.ordinal(), weatherRestrictionSleet);

        WeatherRestriction weatherRestrictionFlurry = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.SNOW_FLURRIES.ordinal(), weatherRestrictionFlurry);

        WeatherRestriction weatherRestrictionHvySnow = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.HEAVY_SNOW.ordinal(), weatherRestrictionHvySnow);

        WeatherRestriction weatherRestrictionIceStorm = new WeatherRestriction(Atmosphere.STANDARD.ordinal(), 30);
        weatherRestrictions.put(Weather.ICE_STORM.ordinal(), weatherRestrictionIceStorm);

        // init wind restrictions
        windRestrictions = new HashMap<>();

        WeatherRestriction weatherRestrictionLtGale = new WeatherRestriction(Atmosphere.THIN.ordinal(), null);
        windRestrictions.put(Wind.LIGHT_GALE.ordinal(), weatherRestrictionLtGale);

        WeatherRestriction weatherRestrictionMdGale = new WeatherRestriction(Atmosphere.TRACE.ordinal(), null);
        windRestrictions.put(Wind.MOD_GALE.ordinal(), weatherRestrictionMdGale);

        WeatherRestriction weatherRestrictionStGale = new WeatherRestriction(Atmosphere.TRACE.ordinal(), null);
        windRestrictions.put(Wind.STRONG_GALE.ordinal(), weatherRestrictionStGale);

        WeatherRestriction weatherRestrictionStorm = new WeatherRestriction(Atmosphere.TRACE.ordinal(), null);
        windRestrictions.put(Wind.STORM.ordinal(), weatherRestrictionStorm);

        WeatherRestriction weatherRestrictionF13 = new WeatherRestriction(Atmosphere.TRACE.ordinal(), null);
        windRestrictions.put(Wind.TORNADO_F1_TO_F3.ordinal(), weatherRestrictionF13);

        WeatherRestriction weatherRestrictionF4 = new WeatherRestriction(Atmosphere.TRACE.ordinal(), null);
        windRestrictions.put(Wind.TORNADO_F4.ordinal(), weatherRestrictionF4);
    }

    public WeatherRestriction(Integer minAtmoLevel, Integer maxTemp) {
        this.minAtmoLevel = minAtmoLevel;
        this.maxTemp = maxTemp;
    }

    /**
     * Given a set of planetary conditions, determine if they are valid for their current atmosphere/temperature
     * Currently validates fog, weather (precipitation) and wind strength
     */
    public static boolean IsRestricted(PlanetaryConditions conditions) {
        return IsFogRestricted(conditions.getFog().ordinal(),
              conditions.getAtmosphere().ordinal(),
              conditions.getTemperature()) ||
              IsWeatherRestricted(conditions.getWeather().ordinal(),
                    conditions.getAtmosphere().ordinal(),
                    conditions.getTemperature()) ||
              IsWindRestricted(conditions.getWind().ordinal(),
                    conditions.getAtmosphere().ordinal(),
                    conditions.getTemperature());
    }

    /**
     * Given a fog level and set of relevant planetary conditions, determine if the fog level is allowed
     */
    public static boolean IsFogRestricted(int fogLevel, int atmoLevel, int temperature) {
        return IsRestricted(fogLevel, atmoLevel, temperature, fogRestrictions);
    }

    /**
     * Given a weather (precipitation) type and set of relevant planetary conditions, determine if it's allowed
     */
    public static boolean IsWeatherRestricted(int weatherType, int atmoLevel, int temperature) {
        return IsRestricted(weatherType, atmoLevel, temperature, weatherRestrictions);
    }

    /**
     * Given a wind level and set of relevant planetary conditions, determine if the wind level is allowed
     */
    public static boolean IsWindRestricted(int windLevel, int atmoLevel, int temperature) {
        return IsRestricted(windLevel, atmoLevel, temperature, windRestrictions);
    }

    /**
     * Given a condition type, current atmospheric level and temperature, determine if the condition type is allowed
     * from the given restriction mapping.
     */
    private static boolean IsRestricted(int conditionType, int atmoLevel, int currentTemp,
          HashMap<Integer, WeatherRestriction> restrictionMap) {
        if (restrictionMap.containsKey(conditionType)) {
            WeatherRestriction restriction = restrictionMap.get(conditionType);

            // condition is restricted:
            // if there's a specified minimum atmo level and we're below it OR
            // if there's a specified max temp and we're at or above it
            return ((restriction.minAtmoLevel != null) && (atmoLevel < restriction.minAtmoLevel)) ||
                  ((restriction.maxTemp != null) && (currentTemp >= restriction.maxTemp));
        }

        return true;
    }
}
