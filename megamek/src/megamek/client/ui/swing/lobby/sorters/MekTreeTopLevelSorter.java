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

import java.util.Comparator;

import megamek.client.Client;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;

/** A Comparator for the top level entries of the Mek Tree (forces and force-less entities). */
public class MekTreeTopLevelSorter implements Comparator<Object> {

    private final Client client;

    public MekTreeTopLevelSorter(Client client) {
        this.client = client;
    }

    @Override
    public int compare(final Object a, final Object b) {
        if (!((a instanceof Entity) || (a instanceof Force))
                || !((b instanceof Entity) || (b instanceof Force))) {
            throw new IllegalArgumentException("Can only compare Entities/Forces");
        }

        Game game = client.getGame();
        Forces forces = game.getForces();

        Player localPlayer = client.getLocalPlayer();
        Player ownerA;
        Player ownerB;
        int idA;
        int idB;
        if (a instanceof Force aForce) {
            ownerA = forces.getOwner(aForce);
            idA = aForce.getId();
        } else {
            ownerA = ((Entity) a).getOwner();
            idA = ((Entity) a).getId();
        }
        if (b instanceof Force bForce) {
            ownerB = forces.getOwner(bForce);
            idB = bForce.getId();
        } else {
            ownerB = ((Entity) b).getOwner();
            idB = ((Entity) b).getId();
        }

        boolean isLocalBotA = (ownerA != null) && client.getBots().containsKey(ownerA.getName());
        boolean isLocalBotB = (ownerB != null) && client.getBots().containsKey(ownerB.getName());


        boolean isLocalAllyA = (ownerA) != null && !ownerA.equals(localPlayer) && !ownerA.isEnemyOf(localPlayer);
        boolean isLocalAllyB = (ownerB) != null && !ownerB.equals(localPlayer) && !ownerB.isEnemyOf(localPlayer);


        if ((ownerA == localPlayer) && (ownerB != localPlayer)) {
            return -1;
        } else if ((ownerA != localPlayer) && (ownerB == localPlayer)) {
            return 1;
        } else if (isLocalAllyA && !isLocalAllyB) {
            return -1;
        } else if (!isLocalAllyA && isLocalAllyB) {
            return 1;
        } else if (isLocalBotA && !isLocalBotB) {
            return -1;
        } else if (!isLocalBotA && isLocalBotB) {
            return 1;
        } else if (a instanceof Force && b instanceof Entity) {
            return -1;
        } else if (a instanceof Entity && b instanceof Force) {
            return 1;
        }

        return idA - idB;
    }
}
