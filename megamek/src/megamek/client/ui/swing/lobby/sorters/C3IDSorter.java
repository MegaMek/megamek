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
import megamek.common.IPlayer;

/** A Lobby Mek Table sorter that sorts by C3 network association (and by ID after that). */
public class C3IDSorter implements MekTableSorter {
    
    private ClientGUI clientGui;
    
    /** A Lobby Mek Table sorter that sorts mainly by association to C3 networks */
    public C3IDSorter(ClientGUI cg) {
        clientGui = cg;
    }
    
    @Override
    public String getDisplayName() {
        return "C3 Network";
    }
    
    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        final IPlayer p_a = clientGui.getClient().getGame().getPlayer(a.getOwnerId());
        final IPlayer p_b = clientGui.getClient().getGame().getPlayer(b.getOwnerId());
        final IPlayer localPlayer = clientGui.getClient().getLocalPlayer();
        final int t_a = p_a.getTeam();
        final int t_b = p_b.getTeam();
        String tr_a = a.getC3NetId();
        String tr_b = b.getC3NetId();
        tr_a = (tr_a == null ? "" : tr_a);
        tr_b = (tr_b == null ? "" : tr_b);
        boolean a_alone = a.getGame().getEntitiesVector().stream().filter(e -> e.onSameC3NetworkAs(a)).count() <= 1;
        boolean b_alone = b.getGame().getEntitiesVector().stream().filter(e -> e.onSameC3NetworkAs(b)).count() <= 1;
        int a_id = a.getId();
        int b_id = b.getId();
        
        boolean a_C3 = a.hasAnyC3System();
        boolean b_C3 = b.hasAnyC3System();
        
        if ((t_a == localPlayer.getTeam()) && (t_b != localPlayer.getTeam())) {
            return -1;
        } else if ((t_b == localPlayer.getTeam()) && (t_a != localPlayer.getTeam())) {
            return 1;
        } else if (t_a != t_b) {
            return t_a - t_b;
        } else if (!a_C3 && b_C3) {
            return 1;
        } else if (a_C3 && !b_C3) {
            return -1;
        } else if (!a_alone && b_alone) {
            return -1;
        } else if (a_alone && !b_alone) {
            return 1;
        } else if (a_alone && b_alone) {
            return a_id - b_id;
        } else {
            // The units are both on a network
            if (!tr_a.equals(tr_b)) {
                return tr_a.compareTo(tr_b);
            }
            // The units are on the same network; sort by hierarchy (for standard C3) and ID
            if (a.hasNhC3()) {
                return a_id - b_id;    
            } else {
                // The Company Commander on top
                if (a.isC3CompanyCommander()) {
                    return -1;
                } else if (b.isC3CompanyCommander()) {
                    return 1;
                }
                // All units below their masters
                if (b.C3MasterIs(a)) {
                    return -1;
                } else if (a.C3MasterIs(b)) {
                    return 1;
                }
                // Two slaves of the same master sort by ID
                if (a.hasC3S() && b.hasC3S() && a.getC3MasterId() == b.getC3MasterId()) {
                    return a_id - b_id;    
                }
                // Slaves of different masters sort by their master's IDs
                if (a.hasC3S()) {
                    a_id = a.getC3MasterId();
                }
                if (b.hasC3S()) {
                    b_id = b.getC3MasterId();
                }
                return a_id - b_id;
            }
        }
    }

}
