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
import megamek.common.internationalization.I18n;

/** A Lobby Mek Table sorter that sorts by 1) player 2) transported units 3) ID. */
public class PlayerTransportIDSorter extends MekTableSorter {

    private final Client client;

    /** A Lobby Mek Table sorter that sorts by 1) player 2) transported units 3) ID. */
    public PlayerTransportIDSorter(ClientGUI clientGUI) {
        this(clientGUI.getClient());
    }

    /** A Lobby Mek Table sorter that sorts by 1) player 2) transported units 3) ID. */
    public PlayerTransportIDSorter(Client client) {
        super(I18n.getTextAt(MekTableSorter.RESOURCE_BUNDLE, "PlayerTransportIDSorter.DisplayName"),
              MekTableModel.COL_UNIT);
        this.client = client;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        return getPlayerTeamIndexPosition(client, a, b).orElse(compareByUnitAndTransportId(a, b));
    }

    private static int compareByUnitAndTransportId(Entity a, Entity b) {
        int unitIdA = a.getId();
        int unitIdB = b.getId();
        final int transportIdA = a.getTransportId();
        final int transportIdB = b.getTransportId();

        // loaded units should be put immediately below their parent unit
        // if a unit's transport ID is not none, then it should
        // replace their actual id
        if (transportIdA == transportIdB) {
            // either they are both not being transported, or they
            // are being transported by the same unit
            return unitIdA - unitIdB;
        }

        if (transportIdB != Entity.NONE) {
            if (transportIdB == unitIdA) {
                // unit B is loaded on unit A
                return -1;
            }
            unitIdB = transportIdB;
        }

        if (transportIdA != Entity.NONE) {
            if (transportIdA == unitIdB) {
                // unit A is loaded on unit B
                return 1;
            }
            unitIdA = transportIdA;
        }

        return unitIdA - unitIdB;
    }
}
