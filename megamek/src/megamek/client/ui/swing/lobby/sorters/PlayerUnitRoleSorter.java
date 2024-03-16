/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.Player;

/** A Lobby Mek Table sorter that sorts by 1) player 2) unit role. */
public class PlayerUnitRoleSorter implements MekTableSorter {

    private ClientGUI clientGui;
    private Sorting direction;
    private int d;

    /** A Lobby Mek Table sorter that sorts by 1) player 2) unit role. */
    public PlayerUnitRoleSorter(ClientGUI cg, Sorting dir) {
        clientGui = cg;
        direction = dir;
        d = direction == Sorting.ASCENDING ? 1 : -1;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        final Player p_a = clientGui.getClient().getGame().getPlayer(a.getOwnerId());
        final Player p_b = clientGui.getClient().getGame().getPlayer(b.getOwnerId());
        final Player localPlayer = clientGui.getClient().getLocalPlayer();
        final int t_a = p_a.getTeam();
        final int t_b = p_b.getTeam();
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
            String aRole = a.getRole().toString();
            String bRole = b.getRole().toString();
            return aRole.compareTo(bRole) * d;
        }
    }

    @Override
    public String getDisplayName() {
        String msg_player = Messages.getString("ChatLounge.Player");
        String msg_unitrole = Messages.getString("ChatLounge.UnitRole");
        return msg_player + ", " + msg_unitrole;
    }

    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }

    @Override
    public Sorting getSortingDirection() {
        return direction;
    }
}
