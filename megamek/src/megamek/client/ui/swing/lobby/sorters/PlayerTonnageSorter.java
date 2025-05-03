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

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.internationalization.I18n;

/** A Lobby Mek Table sorter that sorts by 1) player 2) tonnage. */
public class PlayerTonnageSorter implements MekTableSorter {

    private final Client client;
    private final Sorting sorting;

    /** A Lobby Mek Table sorter that sorts by 1) player 2) tonnage. */
    public PlayerTonnageSorter(ClientGUI clientGUI, Sorting sorting) {
        this(clientGUI.getClient(), sorting);
    }

    public PlayerTonnageSorter(Client client, Sorting sorting) {
        this.client = client;
        this.sorting = sorting;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        final Player playerA = client.getGame().getPlayer(a.getOwnerId());
        final Player playerB = client.getGame().getPlayer(b.getOwnerId());
        final Player localPlayer = client.getLocalPlayer();
        if (playerA != null && playerB != null) {
            final int teamA = playerA.getTeam();
            final int teamB = playerB.getTeam();
            if (playerA.equals(localPlayer) && !playerB.equals(localPlayer)) {
                return -1;
            } else if (!playerA.equals(localPlayer) && playerB.equals(localPlayer)) {
                return 1;
            } else if ((teamA == localPlayer.getTeam()) && (teamB != localPlayer.getTeam())) {
                return -1;
            } else if ((teamB == localPlayer.getTeam()) && (teamA != localPlayer.getTeam())) {
                return 1;
            } else if (teamA != teamB) {
                return teamA - teamB;
            } else if (!playerA.equals(playerB)) {
                return playerA.getName().compareTo(playerB.getName());
            }
        }
        double aWeight = a.getWeight();
        double bWeight = b.getWeight();
        return (int) Math.signum((aWeight - bWeight) * sorting.getDirection());
    }

    @Override
    public String getDisplayName() {
        return I18n.getTextAt(MekTableSorter.RESOURCE_BUNDLE, "PlayerTonnageSorter.DisplayName");
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
