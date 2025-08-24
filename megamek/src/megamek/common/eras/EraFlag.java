/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.eras;

/**
 * @author Justin "Windchild" Bowen
 */
public enum EraFlag {
    PRE_SPACEFLIGHT,
    EARLY_SPACEFLIGHT,
    AGE_OF_WAR,
    STAR_LEAGUE,
    EARLY_SUCCESSION_WARS,
    LATE_SUCCESSION_WARS_LOS_TECH,
    LATE_SUCCESSION_WARS_RENAISSANCE,
    CLAN_INVASION,
    CIVIL_WAR,
    JIHAD,
    EARLY_REPUBLIC,
    LATE_REPUBLIC,
    DARK_AGES,
    ILCLAN;

    //region Boolean Comparison Methods
    public boolean isPreSpaceflight() {
        return this == PRE_SPACEFLIGHT;
    }

    public boolean isEarlySpaceflight() {
        return this == EARLY_SPACEFLIGHT;
    }

    public boolean isAgeOfWar() {
        return this == AGE_OF_WAR;
    }

    public boolean isStarLeague() {
        return this == STAR_LEAGUE;
    }

    public boolean isEarlySuccessionWars() {
        return this == EARLY_SUCCESSION_WARS;
    }

    public boolean isLateSuccessionWarsLosTech() {
        return this == LATE_SUCCESSION_WARS_LOS_TECH;
    }

    public boolean isLateSuccessionWarsRenaissance() {
        return this == LATE_SUCCESSION_WARS_RENAISSANCE;
    }

    public boolean isClanInvasion() {
        return this == CLAN_INVASION;
    }

    public boolean isCivilWar() {
        return this == CIVIL_WAR;
    }

    public boolean isJihad() {
        return this == JIHAD;
    }

    public boolean isEarlyRepublic() {
        return this == EARLY_REPUBLIC;
    }

    public boolean isLateRepublic() {
        return this == LATE_REPUBLIC;
    }

    public boolean isDarkAges() {
        return this == DARK_AGES;
    }

    public boolean isIlClan() {
        return this == ILCLAN;
    }

    //endregion Boolean Comparison Methods
}
