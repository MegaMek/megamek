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

package megamek.common.enums;

import java.util.Arrays;
import java.util.ResourceBundle;

import megamek.MegaMek;

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
              MegaMek.getMMOptions().getLocale());
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
        return Arrays.stream(BasementType.values())
              .filter(type -> type.ordinal() == ordinal)
              .findFirst()
              .orElse(UNKNOWN);
    }

    @Override
    public String toString() {
        return name;
    }
}
