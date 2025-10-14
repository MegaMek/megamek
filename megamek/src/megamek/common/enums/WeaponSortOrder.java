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

import java.util.Comparator;
import java.util.ResourceBundle;

import megamek.MegaMek;
import megamek.common.comparators.WeaponComparatorArc;
import megamek.common.comparators.WeaponComparatorCustom;
import megamek.common.comparators.WeaponComparatorDamage;
import megamek.common.comparators.WeaponComparatorNum;
import megamek.common.comparators.WeaponComparatorRange;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;

public enum WeaponSortOrder {
    // region Enum Declarations
    DEFAULT("WeaponSortOrder.DEFAULT.text"),
    RANGE_LOW_HIGH("WeaponSortOrder.RANGE_LOW_HIGH.text"),
    RANGE_HIGH_LOW("WeaponSortOrder.RANGE_HIGH_LOW.text"),
    DAMAGE_LOW_HIGH("WeaponSortOrder.DAMAGE_LOW_HIGH.text"),
    DAMAGE_HIGH_LOW("WeaponSortOrder.DAMAGE_HIGH_LOW.text"),
    WEAPON_ARC("WeaponSortOrder.WEAPON_ARC.text"),
    CUSTOM("WeaponSortOrder.CUSTOM.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    WeaponSortOrder(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
              MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparisons
    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isRangeLowHigh() {
        return this == RANGE_LOW_HIGH;
    }

    public boolean isRangeHighLow() {
        return this == RANGE_HIGH_LOW;
    }

    public boolean isDamageLowHigh() {
        return this == DAMAGE_LOW_HIGH;
    }

    public boolean isDamageHighLow() {
        return this == DAMAGE_HIGH_LOW;
    }

    public boolean isWeaponArc() {
        return this == WEAPON_ARC;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }
    // endregion Boolean Comparisons

    /**
     * @param entity the entity to compare weapons for
     *
     * @return the comparator for weapon sorting, or the default weapon sort comparator if the sort order isn't handled
     *       yet.
     */
    public Comparator<WeaponMounted> getWeaponSortComparator(final Entity entity) {
        return switch (this) {
            case DEFAULT -> new WeaponComparatorNum(entity);
            case RANGE_LOW_HIGH -> new WeaponComparatorRange(true);
            case RANGE_HIGH_LOW -> new WeaponComparatorRange(false);
            case DAMAGE_LOW_HIGH -> new WeaponComparatorDamage(true);
            case DAMAGE_HIGH_LOW -> new WeaponComparatorDamage(false);
            case WEAPON_ARC -> new WeaponComparatorArc(entity);
            case CUSTOM -> new WeaponComparatorCustom(entity);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
