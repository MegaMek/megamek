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

public enum Fog {
    FOG_NONE("FOG_NONE", "PlanetaryConditions.DisplayableName.Fog.None", "PlanetaryConditions.Indicator.Fog.None"),
    FOG_LIGHT("FOG_LIGHT", "PlanetaryConditions.DisplayableName.Fog.Light Fog", "PlanetaryConditions.Indicator.Fog.Light"),
    FOG_HEAVY("FOG_HEAVY", "PlanetaryConditions.DisplayableName.Fog.Heavy Fog", "PlanetaryConditions.Indicator.Fog.Heavy");
    private final String externalId;
    private final String name;
    private final String indicator;

    Fog(final String externalId, final String name, final String indicator) {
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

    public boolean isFogNone() {
        return this == FOG_NONE;
    }

    public boolean isFogLight() {
        return this == FOG_LIGHT;
    }

    public boolean isFogHeavy() {
        return this == FOG_HEAVY;
    }

    public static Fog getFog(int i) {
        return Fog.values()[i];
    }
}
