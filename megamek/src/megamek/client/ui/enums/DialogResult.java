/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.enums;

public enum DialogResult {
    //region Enum Declarations
    CONFIRMED,
    CANCELLED;
    //endregion Enum Declarations

    //region Boolean Comparison Methods
    public boolean isConfirmed() {
        return this == CONFIRMED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }
    //endregion Boolean Comparison Methods
}
