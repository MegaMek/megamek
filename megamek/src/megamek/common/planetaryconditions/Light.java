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

public enum Light {
    DAY("LIGHT_DAY", "PlanetaryConditions.DisplayableName.Light.Daylight", "\u2600"),
    DUSK("LIGHT_DUSK", "PlanetaryConditions.DisplayableName.Light.Dusk", "\u263D \u263C"),
    FULL_MOON("LIGHT_FULL_MOON", "PlanetaryConditions.DisplayableName.Light.FullMoonNight", "\u26AB"),
    GLARE("LIGHT_GLARE", "PlanetaryConditions.DisplayableName.Light.Glare", "\u27E1"),
    MOONLESS("LIGHT_MOONLESS", "PlanetaryConditions.DisplayableName.Light.MoonlessNight", "\u26AA"),
    SOLAR_FLARE("LIGHT_SOLAR_FLARE", "PlanetaryConditions.DisplayableName.Light.SolarFlare", "\u2604"),
    PITCH_BLACK("LIGHT_PITCH_BLACK", "PlanetaryConditions.DisplayableName.Light.PitchBlack", "\u2588");

    private final String externalId;
    private final String name;
    private final String indicator;

    Light(final String externalId, final String name, final String indicator) {
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

    public boolean isDay() {
        return this == DAY;
    }

    public boolean isDusk() {
        return this == DUSK;
    }

    public boolean isFullMoon() {
        return this == FULL_MOON;
    }

    public boolean isGlare() {
        return this == GLARE;
    }

    public boolean isMoonless() {
        return this == MOONLESS;
    }

    public boolean isSolarFlare() {
        return this == SOLAR_FLARE;
    }

    public boolean isPitchBack() {
        return this == PITCH_BLACK;
    }

    public boolean isFullMoonOrGlare() {
        return this == FULL_MOON || this == GLARE;
    }

    public boolean isMoonlessOrSolarFlare() {
        return this == MOONLESS || this == SOLAR_FLARE;
    }

    public boolean isFullMoonOrMoonlessOrPitchBack() {
        return this == FULL_MOON || this == MOONLESS|| this == PITCH_BLACK;
    }

    public boolean isDuskOrFullMoonOrMoonlessOrPitchBack() {
        return this == DUSK || this == FULL_MOON || this == MOONLESS|| this == PITCH_BLACK;
    }

    public boolean isLighterThan(final Light light) {
        return compareTo(light) < 0;
    }

    public boolean isDarkerThan(final Light light) {
        return compareTo(light) > 0;
    }

    public static Light getLight(int i) {
        return Light.values()[i];
    }

    public static Light getLight(String s) {
        for (Light condition : Light.values()) {
            if (condition.getExternalId().equals(s)) {
                return condition;
            }
        }
        return Light.DAY;
    }
}
