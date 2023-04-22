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

import megamek.client.TwClient;
import megamek.common.*;
import megamek.common.force.*;

/** A Comparator for the top level entries of the Mek Tree (forces and force-less entities). */
public class MekTreeTopLevelSorter implements Comparator<Object> {

    private TwClient client;

    public MekTreeTopLevelSorter(TwClient cl) {
        client = cl;
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
        if (a instanceof Force) {
            ownerA = forces.getOwner((Force) a);
            idA = ((Force) a).getId();
        } else {
            ownerA = ((Entity) a).getOwner();
            idA = ((Entity) a).getId();
        }
        if (b instanceof Force) {
            ownerB = forces.getOwner((Force) b);
            idB = ((Force) b).getId();
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
        } else {
            return idA - idB;
        }
    }
}
