/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum SkillLevel {
    //region Enum Declarations
    NONE("SkillLevel.NONE.text"),
    ULTRA_GREEN("SkillLevel.ULTRA_GREEN.text"),
    GREEN("SkillLevel.GREEN.text"),
    REGULAR("SkillLevel.REGULAR.text"),
    VETERAN("SkillLevel.VETERAN.text"),
    ELITE("SkillLevel.ELITE.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;

    //endregion Variable Declarations

    //region Constructors
    SkillLevel(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages", new EncodeControl());
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparisons
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isUltraGreen() {
        return this == ULTRA_GREEN;
    }

    public boolean isGreen() {
        return this == GREEN;
    }

    public boolean isRegular() {
        return this == REGULAR;
    }

    public boolean isVeteran() {
        return this == VETERAN;
    }

    public boolean isElite() {
        return this == ELITE;
    }
    //endregion Boolean Comparisons

    /**
     * This returns the default skill values by level. This should never return the value for NONE,
     * as NONE means one does not have the skill.
     * @return the default skill array pairing
     */
    public int[] getDefaultSkillValues() {
        switch (this) {
            case NONE:
                MegaMek.getLogger().error("Attempting to get illegal default skill values for NONE Skill Level. Returning { 10, 10 }");
                return new int[]{ 10, 10 };
            case ULTRA_GREEN:
                return new int[]{ 6, 7 };
            case GREEN:
                return new int[]{ 5, 6 };
            case VETERAN:
                return new int[]{ 3, 4 };
            case ELITE:
                return new int[]{ 2, 3 };
            case REGULAR:
            default:
                return new int[]{ 4, 5 };
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
