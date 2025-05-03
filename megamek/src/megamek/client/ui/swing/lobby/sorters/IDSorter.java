/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.lobby.sorters;

import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.internationalization.I18n;

/** A Lobby Mek Table sorter that sorts by unit ID. */
public class IDSorter implements MekTableSorter {
    
    private final Sorting sorting;
    
    /** A Lobby Mek Table sorter that sorts by unit ID. */
    public IDSorter(MekTableSorter.Sorting sorting) {
        this.sorting = sorting;
    }
    
    @Override
    public int compare(final Entity a, final Entity b) {
        int aVal = a.getId();
        int bVal = b.getId();
        return (aVal - bVal) * sorting.getDirection();
    }

    @Override
    public String getDisplayName() {
        return I18n.getTextAt(MekTableSorter.RESOURCE_BUNDLE, "IDSorter.DisplayName");
    }

    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }
    
    @Override
    public Sorting getSortingDirection() {
        return sorting;
    }

}
