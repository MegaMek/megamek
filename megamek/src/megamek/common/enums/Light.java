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

public enum Light {
    DAY("LIGHT_DAY", "PlanetaryConditions.DisplayableName.Light.Daylight", "PlanetaryConditions.Indicator.Light.Day"),
    DUSK("LIGHT_DUSK", "PlanetaryConditions.DisplayableName.Light.Dusk", "PlanetaryConditions.Indicator.Light.Dusk"),
    FULL_MOON("LIGHT_FULL_MOON", "PlanetaryConditions.DisplayableName.Light.Full Moon Night", "PlanetaryConditions.Indicator.Light.FullMoon"),
    MOONLESS("LIGHT_MOONLESS", "PlanetaryConditions.DisplayableName.Light.Moonless Night", "PlanetaryConditions.Indicator.Light.Moonless"),
    PITCH_BLACK("LIGHT_PITCH_BLACK", "PlanetaryConditions.DisplayableName.Light.Pitch Black", "PlanetaryConditions.Indicator.Light.PitchBlack");

    private final String externalId;
    private final String name;
    private final String indicator;

    Light(final String externalId, final String name, final String indicator) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
                MegaMek.getMMOptions().getLocale());
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

    public static Light getLight(int i) {
        return Light.values()[i];
    }

    /**
     * Returns true when visual range is increased by a illumination
     * in the light condition, i.e. in dusk/dawn, full moon,
     * moonless and pitch black night.
     */
    public static boolean isIlluminationEffective(Light light) {
        EnumSet<Light> illuminationEffective = EnumSet.of(DUSK,  FULL_MOON, MOONLESS, PITCH_BLACK);
        return illuminationEffective.contains(light);
    }

    /**
     * Returns true when visual range is dark
     * in the light condition, i.e. in full moon,
     * moonless and pitch black night.
     */
    public static boolean isDark(Light light) {
        EnumSet<Light> dark = EnumSet.of(FULL_MOON, MOONLESS, PITCH_BLACK);
        return dark.contains(light);
    }

    /**
     * Returns true when visual range is very dark
     * in the light condition, i.e. in moonless and pitch black night.
     */
    public static boolean isVeryDark(Light light) {
        EnumSet<Light> veryDark = EnumSet.of(MOONLESS, PITCH_BLACK);
        return veryDark.contains(light);
    }

    /**
     * Returns true when visual range is light
     * in the light condition, i.e. in day, dusk
     */
    public static boolean isLighted(Light light) {
        EnumSet<Light> lighted = EnumSet.of(Light.DAY, Light.DUSK);
        return lighted.contains(light);
    }

    public static boolean isDay(Light light) {
        return light == DAY;
    }

    public static boolean isDusk(Light light) {
        return light == DUSK;
    }

    public static boolean isFullMoon(Light light) {
        return light == FULL_MOON;
    }

    public static boolean isMoonless(Light light) {
        return light == MOONLESS;
    }

    public static boolean isPitchBack(Light light) {
        return light == PITCH_BLACK;
    }
}
