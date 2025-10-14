/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.planetaryConditions;

import megamek.common.Messages;

public enum Wind {
    CALM("WIND_CALM", "PlanetaryConditions.DisplayableName.WindStrength.Calm", "\u2690"),
    LIGHT_GALE("WIND_LIGHT_GALE",
          "PlanetaryConditions.DisplayableName.WindStrength.LightGale",
          "\u21F6 \u2022 \u2022 \u2022"),
    MOD_GALE("WIND_MOD_GALE",
          "PlanetaryConditions.DisplayableName.WindStrength.ModerateGale",
          "\u21F6 \u21F6 \u2022 \u2022"),
    STRONG_GALE("WIND_STRONG_GALE",
          "PlanetaryConditions.DisplayableName.WindStrength.StrongGale",
          "\u21F6 \u21F6 \u21F6 \u2022"),
    STORM("WIND_STORM", "PlanetaryConditions.DisplayableName.WindStrength.Storm", "\u21F6 \u21F6 \u21F6 \u21F6"),
    TORNADO_F1_TO_F3("WIND_TORNADO_F1_TO_F3",
          "PlanetaryConditions.DisplayableName.WindStrength.TornadoF1-F3",
          "\uD83C\uDF2A \uD83C\uDF2A \uD83C\uDF2A \u2022"),
    TORNADO_F4("WIND_TORNADO_F4",
          "PlanetaryConditions.DisplayableName.WindStrength.TornadoF4",
          "\uD83C\uDF2A \uD83C\uDF2A \uD83C\uDF2A \uD83C\uDF2A");
    private final String externalId;
    private final String name;
    private final String indicator;

    Wind(final String externalId, final String name, final String indicator) {
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

    public boolean isLightGaleOrModerateGale() {
        return isLightGale() || isModerateGale();
    }

    public boolean isStrongGaleOrStorm() {
        return isStrongGale() || isStorm();
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

    public static Wind getWind(String s) {
        for (Wind condition : Wind.values()) {
            if (condition.getExternalId().equals(s)) {
                return condition;
            }
        }
        return Wind.CALM;
    }
}
