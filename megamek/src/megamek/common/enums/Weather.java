/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.enums;

import megamek.MegaMek;

import java.util.ResourceBundle;

public enum Weather {
    WEATHER_NONE("WEATHER_NONE", "PlanetaryConditions.DisplayableName.Weather.Clear", "PlanetaryConditions.Indicator.Weather.None"),
    LIGHT_RAIN("WEATHER_LIGHT_RAIN", "PlanetaryConditions.DisplayableName.Weather.Light Rain", "PlanetaryConditions.Indicator.Weather.LightRain"),
    MOD_RAIN("WEATHER_MOD_RAIN", "PlanetaryConditions.DisplayableName.Weather.Moderate Rain", "PlanetaryConditions.Indicator.Weather.ModRain"),
    HEAVY_RAIN("WEATHER_HEAVY_RAIN", "PlanetaryConditions.DisplayableName.Weather.Heavy Rain", "PlanetaryConditions.Indicator.Weather.HeavyRain"),
    GUSTING_RAIN("WEATHER_GUSTING_RAIN", "PlanetaryConditions.DisplayableName.Weather.Gusting Rain", "PlanetaryConditions.Indicator.Weather.GustingRain"),
    DOWNPOUR("WEATHER_DOWNPOUR", "PlanetaryConditions.DisplayableName.Weather.Torrential Downpour", "PlanetaryConditions.Indicator.Weather.Downpour"),
    LIGHT_SNOW("WEATHER_LIGHT_SNOW", "PlanetaryConditions.DisplayableName.Weather.Light Snowfall", "PlanetaryConditions.Indicator.Weather.LightSnow"),
    MOD_SNOW("WEATHER_MOD_SNOW", "PlanetaryConditions.DisplayableName.Weather.Moderate Snowfall", "PlanetaryConditions.Indicator.Weather.ModSnow"),
    SNOW_FLURRIES("WEATHER_SNOW_FLURRIES", "PlanetaryConditions.DisplayableName.Weather.Snow Flurries", "PlanetaryConditions.Indicator.Weather.SnowFlurries"),
    HEAVY_SNOW("WEATHER_HEAVY_SNOW", "PlanetaryConditions.DisplayableName.Weather.Heavy Snowfall", "PlanetaryConditions.Indicator.Weather.HeavySnow"),
    SLEET("WEATHER_SLEET", "PlanetaryConditions.DisplayableName.Weather.Sleet", "PlanetaryConditions.Indicator.Weather.Sleet"),
    ICE_STORM("WEATHER_ICE_STORM", "PlanetaryConditions.DisplayableName.Weather.Ice Storm", "PlanetaryConditions.Indicator.Weather.IceStorm"),
    LIGHT_HAIL("WEATHER_LIGHT_HAIL", "PlanetaryConditions.DisplayableName.Weather.Light Hail", "PlanetaryConditions.Indicator.Weather.LightHail"),
    HEAVY_HAIL("WEATHER_HEAVY_HAIL", "PlanetaryConditions.DisplayableName.Weather.Heavy Hail", "PlanetaryConditions.Indicator.Weather.HeavyHail"),
    LIGHTNING_STORM("WEATHER_LIGHTNING_STORM", "PlanetaryConditions.DisplayableName.Weather.Lightning Storm", "PlanetaryConditions.Indicator.Weather.LightningStorm");

    private final String externalId;
    private final String name;
    private final String indicator;

    Weather(final String externalId, final String name, final String indicator) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages", MegaMek.getMMOptions().getLocale());
        this.externalId = externalId;
        this.name = resources.getString(name);
        this.indicator = resources.getString(indicator);
    }

    public String getIndicator() {
        return indicator;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Weather getWeather(int i) {
        return Weather.values()[i];
    }

    public static boolean isWeatherNone(Weather weather) {
        return weather == WEATHER_NONE;
    }

    public static boolean isLightRain(Weather weather) {
        return weather == LIGHT_RAIN;
    }

    public static boolean isModerateRain(Weather weather) {
        return weather == MOD_RAIN;
    }

    public static boolean isHeavyRain(Weather weather) {
        return weather == HEAVY_RAIN;
    }

    public static boolean isGustingRain(Weather weather) {
        return weather == GUSTING_RAIN;
    }

    public static boolean isDownpour(Weather weather) {
        return weather == DOWNPOUR;
    }

    public static boolean isLightSnow(Weather weather) {
        return weather == LIGHT_SNOW;
    }

    public static boolean isModerateSnow(Weather weather) {
        return weather == MOD_SNOW;
    }

    public static boolean isSnowFlurries(Weather weather) {
        return weather == SNOW_FLURRIES;
    }

    public static boolean isHeavySnow(Weather weather) {
        return weather == HEAVY_SNOW;
    }

    public static boolean isSleet(Weather weather) {
        return weather == SLEET;
    }

    public static boolean isIceStorm(Weather weather) {
        return weather == ICE_STORM;
    }

    public static boolean isLightHail(Weather weather) {
        return weather == LIGHT_HAIL;
    }

    public static boolean isHeaveHail(Weather weather) {
        return weather == HEAVY_HAIL;
    }

    public static boolean isLightningStorm(Weather weather) {
        return weather == LIGHTNING_STORM;
    }
}
