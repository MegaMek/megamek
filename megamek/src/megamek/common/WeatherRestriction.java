/*
 * MegaMek -
 * Copyright (C) 2023 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package megamek.common;

import java.util.HashMap;

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

        WeatherRestriction lightFogRestriction = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        fogRestrictions.put(PlanetaryConditions.FOG_LIGHT, lightFogRestriction);

        WeatherRestriction heavyFogRestriction = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        fogRestrictions.put(PlanetaryConditions.FOG_HEAVY, heavyFogRestriction);

        // init weather restrictions
        weatherRestrictions = new HashMap<>();

        WeatherRestriction weatherRestrictionLtHail = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_LIGHT_HAIL, weatherRestrictionLtHail);

        WeatherRestriction weatherRestrictionHvyHail = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_HEAVY_HAIL, weatherRestrictionHvyHail);

        WeatherRestriction weatherRestrictionLtRain = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        weatherRestrictions.put(PlanetaryConditions.WE_LIGHT_RAIN, weatherRestrictionLtRain);

        WeatherRestriction weatherRestrictionMdRain = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        weatherRestrictions.put(PlanetaryConditions.WE_MOD_RAIN, weatherRestrictionMdRain);

        WeatherRestriction weatherRestrictionLtStorm = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        weatherRestrictions.put(PlanetaryConditions.WE_LIGHTNING_STORM, weatherRestrictionLtStorm);

        WeatherRestriction weatherRestrictionHvyRain = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        weatherRestrictions.put(PlanetaryConditions.WE_HEAVY_RAIN, weatherRestrictionHvyRain);

        WeatherRestriction weatherRestrictionGustRain = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        weatherRestrictions.put(PlanetaryConditions.WE_GUSTING_RAIN, weatherRestrictionGustRain);

        WeatherRestriction weatherRestrictionDownpour = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, null);
        weatherRestrictions.put(PlanetaryConditions.WE_DOWNPOUR, weatherRestrictionDownpour);

        WeatherRestriction weatherRestrictionLtSnow = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_LIGHT_SNOW, weatherRestrictionLtSnow);

        WeatherRestriction weatherRestrictionMdSnow = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_MOD_SNOW, weatherRestrictionMdSnow);

        WeatherRestriction weatherRestrictionSleet = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_SLEET, weatherRestrictionSleet);

        WeatherRestriction weatherRestrictionFlurry = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_SNOW_FLURRIES, weatherRestrictionFlurry);

        WeatherRestriction weatherRestrictionHvySnow = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_HEAVY_SNOW, weatherRestrictionHvySnow);

        WeatherRestriction weatherRestrictionIceStorm = new WeatherRestriction(PlanetaryConditions.ATMO_STANDARD, 30);
        weatherRestrictions.put(PlanetaryConditions.WE_ICE_STORM, weatherRestrictionIceStorm);

        // init wind restrictions
        windRestrictions = new HashMap<>();

        WeatherRestriction weatherRestrictionLtGale = new WeatherRestriction(PlanetaryConditions.ATMO_THIN, null);
        weatherRestrictions.put(PlanetaryConditions.WI_LIGHT_GALE, weatherRestrictionLtGale);

        WeatherRestriction weatherRestrictionMdGale = new WeatherRestriction(PlanetaryConditions.ATMO_TRACE, null);
        weatherRestrictions.put(PlanetaryConditions.WI_MOD_GALE, weatherRestrictionMdGale);

        WeatherRestriction weatherRestrictionStGale = new WeatherRestriction(PlanetaryConditions.ATMO_TRACE, null);
        weatherRestrictions.put(PlanetaryConditions.WI_STRONG_GALE, weatherRestrictionStGale);

        WeatherRestriction weatherRestrictionStorm = new WeatherRestriction(PlanetaryConditions.ATMO_TRACE, null);
        weatherRestrictions.put(PlanetaryConditions.WI_STORM, weatherRestrictionStorm);

        WeatherRestriction weatherRestrictionF13 = new WeatherRestriction(PlanetaryConditions.ATMO_TRACE, null);
        weatherRestrictions.put(PlanetaryConditions.WI_TORNADO_F13, weatherRestrictionF13);

        WeatherRestriction weatherRestrictionF4 = new WeatherRestriction(PlanetaryConditions.ATMO_TRACE, null);
        weatherRestrictions.put(PlanetaryConditions.WI_TORNADO_F4, weatherRestrictionF4);
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
        return IsFogRestricted(conditions.getFog(), conditions.getAtmosphere(), conditions.getTemperature()) ||
                IsWeatherRestricted(conditions.getWeather(), conditions.getAtmosphere(), conditions.getTemperature()) ||
                IsWindRestricted(conditions.getWindStrength(), conditions.getAtmosphere(), conditions.getTemperature());
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
     * Given a condition type, current atmospheric level and temperature,
     * determine if the condition type is allowed from the given restriction mapping.
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
