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
import megamek.common.*;
import megamek.common.equipment.WeaponMounted;
import org.apache.logging.log4j.LogManager;

import java.util.Comparator;
import java.util.ResourceBundle;

public enum WeaponSortOrder {
    //region Enum Declarations
    DEFAULT("WeaponSortOrder.DEFAULT.text"),
    RANGE_LOW_HIGH("WeaponSortOrder.RANGE_LOW_HIGH.text"),
    RANGE_HIGH_LOW("WeaponSortOrder.RANGE_HIGH_LOW.text"),
    DAMAGE_LOW_HIGH("WeaponSortOrder.DAMAGE_LOW_HIGH.text"),
    DAMAGE_HIGH_LOW("WeaponSortOrder.DAMAGE_HIGH_LOW.text"),
    WEAPON_ARC("WeaponSortOrder.WEAPON_ARC.text"),
    CUSTOM("WeaponSortOrder.CUSTOM.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    //endregion Variable Declarations

    //region Constructors
    WeaponSortOrder(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
                MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparisons
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
    //endregion Boolean Comparisons

    /**
     * @param entity the entity to compare weapons for
     * @return the comparator for weapon sorting, or the default weapon sort comparator if the sort
     * order isn't handled yet.
     */
    public Comparator<WeaponMounted> getWeaponSortComparator(final Entity entity) {
        switch (this) {
            case DEFAULT:
                return new WeaponComparatorNum(entity);
            case RANGE_LOW_HIGH:
                return new WeaponComparatorRange(true);
            case RANGE_HIGH_LOW:
                return new WeaponComparatorRange(false);
            case DAMAGE_LOW_HIGH:
                return new WeaponComparatorDamage(true);
            case DAMAGE_HIGH_LOW:
                return new WeaponComparatorDamage(false);
            case WEAPON_ARC:
                return new WeaponComparatorArc(entity);
            case CUSTOM:
                return new WeaponComparatorCustom(entity);
            default:
                LogManager.getLogger().error(String.format(
                        "Attempted to get weapon sort comparator for unknown WeaponSortOrder %s, returning the DEFAULT weapon sort comparator.",
                        name()));
                return DEFAULT.getWeaponSortComparator(entity);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
