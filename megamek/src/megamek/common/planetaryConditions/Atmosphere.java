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


public enum Atmosphere {
    VACUUM("VACUUM",
          "PlanetaryConditions.DisplayableName.Atmosphere.Vacuum",
          "\u2726 \u2727 \u2727 \u25AF \u2727 \u2727"),
    TRACE("TRACE", "PlanetaryConditions.DisplayableName.Atmosphere.Trace", "\u2726 \u2726 \u2727 \u25AF \u2727 \u2727"),
    THIN("THIN", "PlanetaryConditions.DisplayableName.Atmosphere.Thin", "\u2726 \u2726 \u2726 \u25AF \u2727 \u2727"),
    STANDARD("STANDARD",
          "PlanetaryConditions.DisplayableName.Atmosphere.Standard",
          "\u2726 \u2726 \u2726 \u25AE \u2727 \u2727"),
    HIGH("HIGH", "PlanetaryConditions.DisplayableName.Atmosphere.High", "\u2726 \u2726 \u2726 \u25AE \u2726 \u2727"),
    VERY_HIGH("VERY_HIGH",
          "PlanetaryConditions.DisplayableName.Atmosphere.VeryHigh",
          "\u2726 \u2726 \u2726 \u25AE \u2726 \u2726");
    private final String externalId;
    private final String name;
    private final String indicator;

    Atmosphere(final String externalId, final String name, final String indicator) {
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

    public boolean isVacuum() {
        return this == VACUUM;
    }

    public boolean isTrace() {
        return this == TRACE;
    }

    public boolean isThin() {
        return this == THIN;
    }

    public boolean isStandard() {
        return this == STANDARD;
    }

    public boolean isHigh() {
        return this == HIGH;
    }

    public boolean isVeryHigh() {
        return this == VERY_HIGH;
    }

    public boolean isTraceOrThin() {
        return isTrace()
              || isThin();
    }

    public boolean isLighterThan(final Atmosphere atmosphere) {
        return compareTo(atmosphere) < 0;
    }

    public boolean isDenserThan(final Atmosphere atmosphere) {
        return compareTo(atmosphere) > 0;
    }

    public static Atmosphere getAtmosphere(int i) {
        return Atmosphere.values()[i];
    }

    public static Atmosphere getAtmosphere(String s) {
        for (Atmosphere condition : Atmosphere.values()) {
            if (condition.getExternalId().equals(s)) {
                return condition;
            }
        }
        return Atmosphere.STANDARD;
    }
}
