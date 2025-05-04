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

import java.util.List;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.internationalization.I18n;

/** A Lobby Mek Table sorter that sorts by C3 network association (and by ID after that). */
public class C3IDSorter extends MekTableSorter {
    
    private final Client client;

    /** A Lobby Mek Table sorter that sorts mainly by association to C3 networks */
    public C3IDSorter(ClientGUI clientGUI) {
        this(clientGUI.getClient());
    }

    /** A Lobby Mek Table sorter that sorts mainly by association to C3 networks */
    public C3IDSorter(Client client) {
        super(I18n.getTextAt(MekTableSorter.RESOURCE_BUNDLE, "C3IDSorter.DisplayName"), MekTableModel.COL_UNIT);
        this.client = client;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        return this.getPlayerTeamIndexPosition(client, a, b).orElse(compareC3(a, b));
    }

    private static int compareC3(Entity a, Entity b) {
        String c3NetIdUnitA = a.getC3NetId();
        String c3NetIdUnitB = b.getC3NetId();
        c3NetIdUnitA = (c3NetIdUnitA == null ? "" : c3NetIdUnitA);
        c3NetIdUnitB = (c3NetIdUnitB == null ? "" : c3NetIdUnitB);

        List<Entity> entities = a.getGame().inGameTWEntities();

        boolean isUnitAAlone = isIsUnitAlone(a, entities);
        boolean isUnitBAlone = isIsUnitAlone(b, entities);
        int unitAID = a.getId();
        int unitBID = b.getId();

        boolean unitAHasC3System = a.hasAnyC3System();
        boolean unitBHasC3System = b.hasAnyC3System();

        if (!unitAHasC3System && unitBHasC3System) {
            return 1;
        } else if (unitAHasC3System && !unitBHasC3System) {
            return -1;
        } else if (!isUnitAAlone && isUnitBAlone) {
            return -1;
        } else if (isUnitAAlone && !isUnitBAlone) {
            return 1;
        } else if (isUnitAAlone) {
            return unitAID - unitBID;
        }
        // The units are both on a network
        if (!c3NetIdUnitA.equals(c3NetIdUnitB)) {
            return c3NetIdUnitA.compareTo(c3NetIdUnitB);
        }

        // The units are on the same network; sort by hierarchy (for standard C3) and ID
        if (a.hasHierarchicalC3()) {
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
                return unitAID - unitBID;
            }
            // Slaves of different masters sort by their master's IDs
            if (a.hasC3S()) {
                unitAID = a.getC3MasterId();
            }
            if (b.hasC3S()) {
                unitBID = b.getC3MasterId();
            }
        }
        return unitAID - unitBID;
    }

    private static boolean isIsUnitAlone(Entity a, List<Entity> entities) {
        boolean isUnitAAlone = true;
        for (Entity e : entities) {
            if (e != a && e.onSameC3NetworkAs(a)) {
                isUnitAAlone = false;
                break;
            }
        }
        return isUnitAAlone;
    }

}
