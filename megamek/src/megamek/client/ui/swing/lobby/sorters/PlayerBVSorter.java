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

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.internationalization.I18n;

/** A Lobby Mek Table sorter that sorts for 1) Player 2) BV. */
public class PlayerBVSorter implements MekTableSorter {
    
    private final Client client;
    private final Sorting sorting;

    /** A Lobby Mek Table sorter that sorts for 1) Player 2) BV. */
    public PlayerBVSorter(ClientGUI clientGUI, Sorting sorting) {
        this(clientGUI.getClient(), sorting);
    }

    public PlayerBVSorter(Client client, Sorting sorting) {
        this.client = client;
        this.sorting = sorting;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        return this.getPlayerTeamIndexPosition(client, a, b).orElse(compareBV(a, b));
    }

    private int compareBV(Entity a, Entity b) {
        int aBV = a.calculateBattleValue();
        int bBV = b.calculateBattleValue();
        return (bBV - aBV) * sorting.getDirection();
    }

    @Override
    public String getDisplayName() {
        return I18n.getTextAt(MekTableSorter.RESOURCE_BUNDLE, "PlayerBVSorter.DisplayName");
    }
    
    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_BV;
    }
    
    @Override
    public Sorting getSortingDirection() {
        return sorting;
    }

}
