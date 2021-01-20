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

    private final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.enums", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    SkillLevel(final String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    @Override
    public String toString() {
        return name;
    }
}
