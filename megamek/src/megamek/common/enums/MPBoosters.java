/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
            return walkMP * 2;
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
            return walkMP * 3;
        } else {
            return walkMP * 2;
        }
    }
}
