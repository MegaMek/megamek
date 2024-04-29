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
package megamek.common.planetaryconditions;

import megamek.common.Messages;

public enum Weather {
    CLEAR("WEATHER_CLEAR", "PlanetaryConditions.DisplayableName.Weather.Clear", "\u239A"),
    LIGHT_RAIN("WEATHER_LIGHT_RAIN", "PlanetaryConditions.DisplayableName.Weather.LightRain", "\u2601 \u2022 \u2022 \u2022 \u2022"),
    MOD_RAIN("WEATHER_MOD_RAIN", "PlanetaryConditions.DisplayableName.Weather.ModerateRain", "\u2601 \u2601 \u2022 \u2022 \u2022"),
    HEAVY_RAIN("WEATHER_HEAVY_RAIN", "PlanetaryConditions.DisplayableName.Weather.HeavyRain", "\u2601 \u2601 \u2601 \u2022 \u2022"),
    GUSTING_RAIN("WEATHER_GUSTING_RAIN", "PlanetaryConditions.DisplayableName.Weather.GustingRain", "\u2601 \u2601 \u2601 \u2601 \u2022"),
    DOWNPOUR("WEATHER_DOWNPOUR", "PlanetaryConditions.DisplayableName.Weather.TorrentialDownpour", "\u2601 \u2601 \u2601 \u2601 \u2601"),
    LIGHT_SNOW("WEATHER_LIGHT_SNOW", "PlanetaryConditions.DisplayableName.Weather.LightSnowfall", "\u2744 \u2022 \u2022 \u2022"),
    MOD_SNOW("WEATHER_MOD_SNOW", "PlanetaryConditions.DisplayableName.Weather.ModerateSnowfall", "\u2744 \u2744 \u2022 \u2022"),
    SNOW_FLURRIES("WEATHER_SNOW_FLURRIES", "PlanetaryConditions.DisplayableName.Weather.SnowFlurries", "\u2744 \u2744 \u2744 \u2022"),
    HEAVY_SNOW("WEATHER_HEAVY_SNOW", "PlanetaryConditions.DisplayableName.Weather.HeavySnowfall", "\u2744 \u2744 \u2744 \u2744"),
    SLEET("WEATHER_SLEET", "PlanetaryConditions.DisplayableName.Weather.Sleet", "\u26C6 \u2022"),
    ICE_STORM("WEATHER_ICE_STORM", "PlanetaryConditions.DisplayableName.Weather.IceStorm", "\u26C6 \u26C6"),
    LIGHT_HAIL("WEATHER_LIGHT_HAIL", "PlanetaryConditions.DisplayableName.Weather.LightHail", "\u2591 \u2022"),
    HEAVY_HAIL("WEATHER_HEAVY_HAIL", "PlanetaryConditions.DisplayableName.Weather.HeavyHail", "\u2591 \u2591"),
    LIGHTNING_STORM("WEATHER_LIGHTNING_STORM", "PlanetaryConditions.DisplayableName.Weather.LightningStorm", "\u2608");

    private final String externalId;
    private final String name;
    private final String indicator;

    Weather(final String externalId, final String name, final String indicator) {
        this.externalId = externalId;
        this.name = name;
        this.indicator = indicator;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return Messages.getString(name);
    }

    public boolean isClear() {
        return this == CLEAR;
    }

    public boolean isLightRain() {
        return this == LIGHT_RAIN;
    }

    public boolean isModerateRain() {
        return this == MOD_RAIN;
    }

    public boolean isHeavyRain() {
        return this == HEAVY_RAIN;
    }

    public boolean isGustingRain() {
        return this == GUSTING_RAIN;
    }

    public boolean isDownpour() {
        return this == DOWNPOUR;
    }

    public boolean isLightSnow() {
        return this == LIGHT_SNOW;
    }

    public boolean isModerateSnow() {
        return this == MOD_SNOW;
    }

    public boolean isSnowFlurries() {
        return this == SNOW_FLURRIES;
    }

    public boolean isHeavySnow() {
        return this == HEAVY_SNOW;
    }

    public boolean isSleet() {
        return this == SLEET;
    }

    public boolean isIceStorm() {
        return this == ICE_STORM;
    }

    public boolean isLightHail() {
        return this == LIGHT_HAIL;
    }

    public boolean isHeavyHail() {
        return this == HEAVY_HAIL;
    }

    public boolean isLightningStorm() {
        return this == LIGHTNING_STORM;
    }

    public boolean isHeavyRainOrDownpour() {
        return isHeavyRain()
                || isDownpour();
    }

    public boolean isLightRainOrLightSnow() {
        return isLightRain()
                || isLightSnow();
    }

    public boolean isSnowFlurriesOrIceStorm() {
        return isSnowFlurries()
                || isIceStorm();
    }

    public boolean isHeavyRainOrGustingRain() {
        return isHeavyRain()
                || isGustingRain();
    }


    public boolean isLightRainOrModerateRainOrLightningStorm() {
        return isLightRain()
                || isModerateRain()
                || isLightningStorm();
    }

    public boolean isModerateSnowOrSnowFlurries() {
        return isModerateSnow()
                || isSnowFlurries();
    }

    public boolean isModerateRainOrLightningStorm() {
        return isModerateRain()
                || isLightningStorm();
    }

    public boolean isModerateRainOrModerateSnowOrLightningStorm() {
        return isModerateRain()
                || isModerateSnow()
                || isLightningStorm();
    }

    public boolean isDownpourOrHeavySnowOrIceStorm() {
        return isDownpour()
                || isHeavySnow()
                || isIceStorm();
    }

    public boolean isSnowFlurriesOrSleetOrIceStorm() {
        return isSnowFlurries()
                || isSleet()
                || isIceStorm();
    }

    public boolean isHeavySnowOrLightHailOrHeaveHail() {
        return isHeavySnow()
                || isLightHail()
                || isHeavyHail();
    }

    public boolean isLightRainOrLightSnowOrLightHail() {
        return isLightRain()
                || isLightSnow()
                || isLightHail();
    }

    public boolean isHeavyRainOrGustingRainOrDownpour() {
        return isHeavyRain()
                || isGustingRain()
                || isDownpour();
    }

    public boolean isHeavyRainOrGustingRainOrHeavySnow() {
        return isHeavyRain()
                || isGustingRain()
                || isHeavySnow();
    }

    public boolean isHeavyRainOrModerateSnowOrSnowFlurries() {
        return isHeavyRain()
                || isModerateSnow()
                || isSnowFlurries();
    }

    public boolean isModerateSnowOrHeavySnowOrSnowFlurries() {
        return isModerateSnow()
                || isHeavySnow()
                || isSnowFlurries();
    }

    public boolean isLightSnowOrSleetOrLightHailOrHeavyHail() {
        return isLightSnow()
                || isSleet()
                || isLightHail()
                || isHeavyHail();
    }

    public boolean isModerateSnowOrHeavySnowOrSnowFlurriesOrSleet() {
        return isModerateSnow()
                || isHeavySnow()
                || isSnowFlurries()
                || isSleet();
    }

    public boolean isModerateRainOrHeavyRainOrGustingRainOrDownpourOrLightningStorm() {
        return isModerateRain()
                || isHeavyRain()
                || isGustingRain()
                || isDownpour()
                || isLightningStorm();
    }

    public boolean isGustingRainOrSnowFlurriesOrIceStormOrLightningStorm() {
        return isGustingRain()
                || isSnowFlurries()
                || isIceStorm()
                || isLightningStorm();
    }

    public boolean isLightSnowOrModerateSnowOrSnowFlurriesOrHeavySnowOrSleet() {
        return isLightSnow()
                || isModerateSnow()
                || isSnowFlurries()
                || isHeavySnow()
                || isSleet();
    }

    public boolean isGustingRainOrDownpourOrHeavySnowOrIceStormOrSleetOrHeavyHail() {
        return isGustingRain()
                || isDownpour()
                || isHeavySnow()
                || isIceStorm()
                || isSleet()
                || isHeavyHail();
    }

    public boolean isHeavyRainOrGustingRainOrDownpourOrLightSnowOrModerateSnowOrSnowFlurries() {
        return isHeavyRain()
                || isGustingRain()
                || isDownpour()
                || isLightSnow()
                || isModerateSnow()
                || isSnowFlurries();
    }

    public boolean isModerateRainOrHeavyRainOrGustingRainOrModerateSnowOrSnowFlurriesOrHeavySnowOrSleetOrLightningStorm() {
        return isModerateRain()
                || isHeavyRain()
                || isGustingRain()
                || isModerateSnow()
                || isSnowFlurries()
                || isHeavySnow()
                || isSleet()
                || isLightningStorm();
    }

    public static Weather getWeather(int i) {
        return values()[i];
    }

    public static Weather getWeather(String s) {
        for (Weather condition : Weather.values()) {
            if (condition.getExternalId().equals(s)) {
                return condition;
            }
        }
        return Weather.CLEAR;
    }
}
