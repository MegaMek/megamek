/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

public enum MPBoosters {
    //region Enum Declarations
    NONE,
    MASC_ONLY,
    SUPERCHARGER_ONLY,
    MASC_AND_SUPERCHARGER,
    VTOL_JET_BOOSTER;
    //endregion Enum Declarations

    //region Boolean Comparisons
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isMASCOnly() {
        return this == MASC_ONLY;
    }

    public boolean isSuperchargerOnly() {
        return this == SUPERCHARGER_ONLY;
    }

    public boolean isMASCAndSupercharger() {
        return this == MASC_AND_SUPERCHARGER;
    }

    public boolean hasMASC() {
        return isMASCOnly() || isMASCAndSupercharger();
    }

    public boolean hasSupercharger() {
        return isSuperchargerOnly() || isMASCAndSupercharger();
    }

    public boolean isMASCXorSupercharger() {
        return isMASCOnly() || isSuperchargerOnly();
    }

    public boolean isJetBooster() {
        return this == VTOL_JET_BOOSTER;
    }
    //endregion Boolean Comparisons

    public int calculateRunMP(int walkMP) {
        if (isMASCXorSupercharger() || isJetBooster()) {
            return (int) Math.ceil(walkMP * 2);
        } else if (isMASCAndSupercharger()) {
            return (int) Math.ceil(walkMP * 2.5);
        } else {
            return (int) Math.ceil(walkMP * 1.5);
        }
    }

    public int calculateSprintMP(int walkMP) {
        if (isMASCXorSupercharger() || isJetBooster()) {
            return (int) Math.ceil(walkMP * 2.5);
        } else if (isMASCAndSupercharger()) {
            return (int) Math.ceil(walkMP * 3);
        } else {
            return (int) Math.ceil(walkMP * 2);
        }
    }
}
