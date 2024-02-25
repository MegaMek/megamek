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

public enum Atmosphere {
    VACUUM("VACUUM", "PlanetaryConditions.DisplayableName.Atmosphere.Vacuum", "PlanetaryConditions.Indicator.Atmosphere.Vacuum"),
    TRACE("TRACE", "PlanetaryConditions.DisplayableName.Atmosphere.Trace", "PlanetaryConditions.Indicator.Atmosphere.Trace"),
    THIN("THIN", "PlanetaryConditions.DisplayableName.Atmosphere.Thin", "PlanetaryConditions.Indicator.Atmosphere.Thin"),
    STANDARD("STANDARD", "PlanetaryConditions.DisplayableName.Atmosphere.Standard", "PlanetaryConditions.Indicator.Atmosphere.Standard"),
    HIGH("HIGH", "PlanetaryConditions.DisplayableName.Atmosphere.High", "PlanetaryConditions.Indicator.Atmosphere.High"),
    VERY_HIGH("VERY_HIGH", "PlanetaryConditions.DisplayableName.Atmosphere.Very High", "PlanetaryConditions.Indicator.Atmosphere.VHigh");
    private final String externalId;
    private final String name;
    private final String indicator;

    Atmosphere(final String externalId, final String name, final String indicator) {
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

    public static Atmosphere getAtmosphere(int i) {
        return Atmosphere.values()[i];
    }

    public static boolean isVacuum(Atmosphere atmosphere) {
        return atmosphere == VACUUM;
    }

    public static boolean isTrace(Atmosphere atmosphere) {
        return atmosphere == TRACE;
    }

    public static boolean isThin(Atmosphere atmosphere) {
        return atmosphere == THIN;
    }

    public static boolean isStandard(Atmosphere atmosphere) {
        return atmosphere == STANDARD;
    }

    public static boolean isHigh(Atmosphere atmosphere) {
        return atmosphere == HIGH;
    }

    public static boolean isVeryHigh(Atmosphere atmosphere) {
        return atmosphere == VERY_HIGH;
    }

    public static boolean isLessThanThin(Atmosphere atmosphere) {
        EnumSet<Atmosphere> lessThanThin = EnumSet.of(VACUUM, TRACE);
        return lessThanThin.contains(atmosphere);
    }

    public static boolean isGreaterThanTrace(Atmosphere atmosphere) {
        EnumSet<Atmosphere> greaterThanTrace = EnumSet.of(THIN, STANDARD, HIGH, VERY_HIGH);
        return greaterThanTrace.contains(atmosphere);
    }
}
