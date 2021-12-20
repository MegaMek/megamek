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

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.Player;

/** A Lobby Mek Table sorter that sorts by 1) player 2) transported units 3) ID. */
public class PlayerTransportIDSorter implements MekTableSorter {
    
    private ClientGUI clientGui;
    
    /** A Lobby Mek Table sorter that sorts by 1) player 2) transported units 3) ID. */
    public PlayerTransportIDSorter(ClientGUI cg) {
        clientGui = cg;
    }
    
    @Override
    public String getDisplayName() {
        return "Player, Transported Units, ID";
    }
    
    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        // entity.getOwner() does not work properly because teams are
        // not updated for entities when the user switches teams
        final Player p_a = clientGui.getClient().getGame().getPlayer(a.getOwnerId());
        final Player p_b = clientGui.getClient().getGame().getPlayer(b.getOwnerId());
        final Player localPlayer = clientGui.getClient().getLocalPlayer();
        final int t_a = p_a.getTeam();
        final int t_b = p_b.getTeam();
        final int tr_a = a.getTransportId();
        final int tr_b = b.getTransportId();
        if (p_a.equals(localPlayer) && !p_b.equals(localPlayer)) {
            return -1;
        } else if (!p_a.equals(localPlayer) && p_b.equals(localPlayer)) {
            return 1;
        } else if ((t_a == localPlayer.getTeam()) && (t_b != localPlayer.getTeam())) {
            return -1;
        } else if ((t_b == localPlayer.getTeam()) && (t_a != localPlayer.getTeam())) {
            return 1;
        } else if (t_a != t_b) {
            return t_a - t_b;
        } else if (!p_a.equals(p_b)) {
            return p_a.getName().compareTo(p_b.getName());
        } else {
            int a_id = a.getId();
            int b_id = b.getId();
            // loaded units should be put immediately below their parent unit
            // if a unit's transport ID is not none, then it should
            // replace their actual id
            if (tr_a == tr_b) {
                // either they are both not being transported, or they
                // are being transported by the same unit
                return a_id - b_id;
            }

            if (tr_b != Entity.NONE) {
                if (tr_b == a_id) {
                    // b is loaded on a
                    return -1;
                }
                b_id = tr_b;
            }
            
            if (tr_a != Entity.NONE) {
                if (tr_a == b_id) {
                    // a is loaded on b
                    return 1;
                }
                a_id = tr_a;
            }
            return a_id - b_id;
        }
    }

}
