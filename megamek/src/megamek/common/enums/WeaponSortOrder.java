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

import megamek.MegaMek;
import megamek.common.util.EncodeControl;
import org.apache.logging.log4j.LogManager;

import java.util.ResourceBundle;

public enum WeaponSortOrder {
    //region Enum Declarations
    DEFAULT("WeaponSortOrder.DEFAULT.text", "WeaponSortOrder.DEFAULT.toolTipText"),
    RANGE_LH("WeaponSortOrder.RANGE_LH.text", "WeaponSortOrder.RANGE_LH.toolTipText"),
    RANGE_HL("WeaponSortOrder.RANGE_HL.text", "WeaponSortOrder.RANGE_HL.toolTipText"),
    DAMAGE_LH("WeaponSortOrder.DAMAGE_LH.text", "WeaponSortOrder.DAMAGE_LH.toolTipText"),
    DAMAGE_HL("WeaponSortOrder.DAMAGE_HL.text", "WeaponSortOrder.DAMAGE_HL.toolTipText"),
    ARC("WeaponSortOrder.ARC.text", "WeaponSortOrder.ARC.toolTipText"),
    CUSTOM("WeaponSortOrder.CUSTOM.text", "WeaponSortOrder.CUSTOM.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    WeaponSortOrder(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.common.messages",
                MegaMek.getMMOptions().getLocale(), new EncodeControl());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isDamageLH() {
        return this == RANGE_LH;
    }

    public boolean isDamageHL() {
        return this == RANGE_HL;
    }

    public boolean isDamageLH() {
        return this == DAMAGE_LH;
    }

    public boolean isDamageHL() {
        return this == DAMAGE_HL;
    }

    public boolean isArc() {
        return this == ARC;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }
    //endregion Boolean Comparisons

    //region File I/O
    public static WeaponSortOrder parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return GREEN;
                case 1:
                    return REGULAR;
                case 2:
                    return VETERAN;
                case 3:
                    return ELITE;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse " + text + " into a WeaponSortOrder. Returning REGULAR.");

        return REGULAR;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
