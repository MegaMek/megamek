/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.Arrays;
import java.util.ResourceBundle;

public enum BasementType {
    //region Enum Declarations
    UNKNOWN("BasementType.UNKNOWN.text", 0),
    NONE("BasementType.NONE.text", 0),
    TWO_DEEP_FEET("BasementType.TWO_DEEP_FEET.text", 2),
    ONE_DEEP_FEET("BasementType.ONE_DEEP_FEET.text", 1),
    ONE_DEEP_NORMAL("BasementType.ONE_DEEP_NORMAL.text", 1),
    ONE_DEEP_NORMAL_INFANTRY_ONLY("BasementType.ONE_DEEP_NORMAL_INFANTRY_ONLY.text", 1),
    ONE_DEEP_HEAD("BasementType.ONE_DEEP_HEAD.text", 1),
    TWO_DEEP_HEAD("BasementType.TWO_DEEP_HEAD.text", 2);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final int depth;
    //endregion Variable Declarations

    //region Constructors
    BasementType(final String name, final int depth) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
                MegaMek.getMMOptions().getLocale(), new EncodeControl());
        this.name = resources.getString(name);
        this.depth = depth;
    }
    //endregion Constructors

    //region Getters
    public int getDepth() {
        return depth;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isTwoDeepFeet() {
        return this == TWO_DEEP_FEET;
    }

    public boolean isOneDeepFeet() {
        return this == ONE_DEEP_FEET;
    }

    public boolean isOneDeepNormal() {
        return this == ONE_DEEP_NORMAL;
    }

    public boolean isOneDeepNormalInfantryOnly() {
        return this == ONE_DEEP_NORMAL_INFANTRY_ONLY;
    }

    public boolean isOneDeepHead() {
        return this == ONE_DEEP_HEAD;
    }

    public boolean isTwoDeepHead() {
        return this == TWO_DEEP_HEAD;
    }

    public boolean isUnknownOrNone() {
        return isUnknown() || isNone();
    }
    //endregion Boolean Comparison Methods

    public static BasementType getType(final int ordinal) {
        return Arrays.stream(BasementType.values()).filter(type -> type.ordinal() == ordinal).findFirst().orElse(UNKNOWN);
    }

    @Override
    public String toString() {
        return name;
    }
}
