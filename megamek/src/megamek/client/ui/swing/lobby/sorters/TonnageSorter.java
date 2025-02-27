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

import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.options.GameOptions;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;

/** A Lobby Mek Table sorter that sorts by tonnage. */
public class TonnageSorter implements MekTableSorter {

    private Sorting direction;

    /** A Lobby Mek Table sorter that sorts by tonnage. */
    public TonnageSorter(MekTableSorter.Sorting dir) {
        direction = dir;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        double aWeight = a.getWeight();
        double bWeight = b.getWeight();
        if (bWeight > aWeight) {
            return smaller(direction);
        } else if (bWeight < aWeight) {
            return bigger(direction);
        } else {
            return 0;
        }
    }

    @Override
    public String getDisplayName() {
        return "Tonnage";
    }

    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }

    @Override
    public boolean isAllowed(IGameOptions opts) {
        return !opts.booleanOption(OptionsConstants.BASE_BLIND_DROP);
    }

    @Override
    public Sorting getSortingDirection() {
        return direction;
    }

}
