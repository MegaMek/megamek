
/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.advancedsearch;

/**
 * Represents a filter token for a weapon class.
 */
class WeaponClassFT extends EquipmentFilterToken {

    WeaponClass weaponClass;

    WeaponClassFT(WeaponClass in_class, int in_qty) {
        this(in_class, in_qty, true);
    }

    WeaponClassFT(WeaponClass in_class, int in_qty, boolean atleast) {
        weaponClass = in_class;
        qty = in_qty;
        this.atleast = atleast;
    }

    @Override
    public String toString() {
        return (atleast ? "" : "less than ") + qty + " " + weaponClass.toString() + ((qty != 1) ? "s" : "");
    }
}
