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

package megamek.common.planetaryconditions;

import megamek.common.Messages;

public enum BlowingSand {
    BLOWING_SAND_NONE("BLOWING_SAND_NONE", "PlanetaryConditions.DisplayableName.SandBlowing.false", "\uD83D\uDC41"),
    BLOWING_SAND("BLOWING_SAND", "PlanetaryConditions.DisplayableName.SandBlowing.true", "\uD83C\uDF2C");
    private final String externalId;
    private final String name;
    private final String indicator;

    BlowingSand(final String externalId, final String name, final String indicator) {
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

    public boolean isBlowingSandNone() {
        return this == BLOWING_SAND_NONE;
    }

    public boolean isBlowingSand() {
        return this == BLOWING_SAND;
    }

    public static BlowingSand getBlowingSand(int i) {
        return BlowingSand.values()[i];
    }

    public static BlowingSand getBlowingSand(String s) {
        for (BlowingSand condition : BlowingSand.values()) {
            if (condition.getExternalId().equals(s)) {
                return condition;
            }
        }
        return BlowingSand.BLOWING_SAND_NONE;
    }
}
