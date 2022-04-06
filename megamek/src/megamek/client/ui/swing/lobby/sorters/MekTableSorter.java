/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.lobby.sorters;

import java.util.Comparator;

import megamek.common.Entity;
import megamek.common.options.GameOptions;

/** An interface for the Comparators used for the lobby Mek table. */
public interface MekTableSorter extends Comparator<Entity> {
    enum Sorting { ASCENDING, DESCENDING }

    /** 
     * Returns the info that is displayed in the column header to show
     * the sorting that is used, such as "Team / BV".
     */
    String getDisplayName();
    
    /** 
     * Returns the column index of the Mek Table that this sorter is to be used with. 
     */
    int getColumnIndex();
    
    /**
     * Returns true if this Sorter is currently allowed. Sorters might not be allowed
     * e.g. when they would give away info in blind drops.
     */
    default boolean isAllowed(GameOptions opts) {
        return true;
    }
    
    /** Returns the sorting direction. */
    default Sorting getSortingDirection() {
        return null;
    }
    
    /** Returns 1 if dir is ASCENDING, -1 otherwise. */
    default int bigger(Sorting dir) {
        return dir == Sorting.ASCENDING ? 1 : -1;
    }
    
    /** Returns -1 if dir is ASCENDING, 1 otherwise. */
    default int smaller(Sorting dir) {
        return dir == Sorting.ASCENDING ? -1 : 1;
    }
}
