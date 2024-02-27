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

import java.util.EnumSet;
import java.util.ResourceBundle;

public enum Wind {
    CALM("WIND_CALM", "PlanetaryConditions.DisplayableName.WindStrength.Calm", "PlanetaryConditions.Indicator.WindStrength.Calm"),
    LIGHT_GALE("WIND_LIGHT_GALE", "PlanetaryConditions.DisplayableName.WindStrength.Light Gale", "PlanetaryConditions.Indicator.WindStrength.LightGale"),
    MOD_GALE("WIND_MOD_GALE", "PlanetaryConditions.DisplayableName.WindStrength.Moderate Gale", "PlanetaryConditions.Indicator.WindStrength.ModGale"),
    STRONG_GALE("WIND_STRONG_GALE", "PlanetaryConditions.DisplayableName.WindStrength.Strong Gale", "PlanetaryConditions.Indicator.WindStrength.StrongGale"),
    STORM("WIND_STORM", "PlanetaryConditions.DisplayableName.WindStrength.Storm", "PlanetaryConditions.Indicator.WindStrength.Storm"),
    TORNADO_F1_TO_F3("WIND_TORNADO_F1_TO_F3", "PlanetaryConditions.DisplayableName.WindStrength.Tornado F1-F3", "PlanetaryConditions.Indicator.WindStrength.TornadoF13"),
    TORNADO_F4("WIND_TORNADO_F4", "PlanetaryConditions.DisplayableName.WindStrength.Tornado F4", "PlanetaryConditions.Indicator.WindStrength.TornadoF4");
    private final String externalId;
    private final String name;
    private final String indicator;

    Wind(final String externalId, final String name, final String indicator) {
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

    public Wind lowerWind() {
        switch (this) {
            case TORNADO_F4:
                return TORNADO_F1_TO_F3;
            case TORNADO_F1_TO_F3:
                return STORM;
            case STRONG_GALE:
                return MOD_GALE;
            case MOD_GALE:
                return LIGHT_GALE;
            default:
                return CALM;
        }
    }

    public Wind raiseWind() {
        switch (this) {
            case CALM:
                return LIGHT_GALE;
            case LIGHT_GALE:
                return MOD_GALE;
            case MOD_GALE:
                return STORM;
            case STRONG_GALE:
                return TORNADO_F1_TO_F3;
            default:
                return TORNADO_F4;
        }
    }

    public boolean isCalm() {
        return this == CALM;
    }

    public boolean isLightGale() {
        return this == LIGHT_GALE;
    }

    public boolean isModerateGale() {
        return this == MOD_GALE;
    }

    public boolean isStrongGale() {
        return this == STRONG_GALE;
    }

    public boolean isStorm() {
        return this == STORM;
    }

    public boolean isTornadoF1ToF3() {
        return this == TORNADO_F1_TO_F3;
    }

    public boolean isTornadoF4() {
        return this == TORNADO_F4;
    }

    public boolean isWeakerThan(final Wind wind) {
        return compareTo(wind) < 0;
    }

    public boolean isStrongerThan(final Wind wind) {
        return compareTo(wind) > 0;
    }

    public static Wind getWind(int i) {
        return Wind.values()[i];
    }

}
