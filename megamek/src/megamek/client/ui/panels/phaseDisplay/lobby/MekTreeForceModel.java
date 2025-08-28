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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import megamek.client.ui.panels.phaseDisplay.lobby.sorters.MekTreeTopLevelSorter;
import megamek.common.units.Entity;
import megamek.common.interfaces.ForceAssignable;
import megamek.common.game.Game;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.options.OptionsConstants;

public class MekTreeForceModel extends DefaultTreeModel {

    @Serial
    private static final long serialVersionUID = -6458173460367645667L;

    private final ChatLounge lobby;
    /** A sorted list of all top-level objects: top-level forces and force-less entities. */
    private ArrayList<Object> allToplevel;

    public MekTreeForceModel(ChatLounge cl) {
        super(new DefaultMutableTreeNode("Root"));
        lobby = cl;
    }

    public void refreshData() {
        allToplevel = null;
        nodeStructureChanged(root);
    }

    public void refreshDisplay() {
        nodeChanged(root);
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (index < 0) {
            return null;
        }

        if (parent == root) {
            if (allToplevel == null) {
                createTopLevel();
            }
            return allToplevel.get(index);

        } else if (parent instanceof Force pnt) {
            Forces forces = lobby.game().getForces();
            if (index < pnt.entityCount()) {
                return lobby.game().getEntity(pnt.getEntityId(index));
            } else if (index < pnt.getChildCount()) {
                return forces.getForce(pnt.getSubForceId(index - pnt.entityCount()));
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == root) {
            if (allToplevel == null) {
                createTopLevel();
            }
            return allToplevel.size();

        } else if (parent instanceof Force pnt) {
            return pnt.getChildCount();

        } else { // Entity
            return 0;
        }
    }

    /**
     * Creates and stores a sorted list of the top-level forces and entities. Removes those that aren't visible in real
     * blind drop.
     */
    private void createTopLevel() {
        Game game = lobby.getClientGUI().getClient().getGame();
        boolean realBD = game.getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        Forces forces = lobby.game().getForces();
        Player localPlayer = lobby.getClientGUI().getClient().getLocalPlayer();
        boolean localGM = localPlayer.isGameMaster();
        ArrayList<Force> toplevel = new ArrayList<>(forces.getTopLevelForces());
        if (!localGM && realBD) {
            toplevel.removeIf(f -> localPlayer.isEnemyOf(forces.getOwner(f)));
        }
        List<Entity> forceless = ForceAssignable.filterToEntityList(forces.forcelessEntities());
        if (!localGM && realBD) {
            forceless.removeIf(e -> localPlayer.isEnemyOf(e.getOwner()));
        }
        allToplevel = new ArrayList<>(toplevel);
        allToplevel.addAll(forceless);
        allToplevel.sort(new MekTreeTopLevelSorter(lobby.getClientGUI().getClient()));
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof Entity;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (child == root || !(parent instanceof Force pnt)
              || !((child instanceof Force) || (child instanceof Entity))) {
            return -1;
        }
        if (child instanceof Entity) {
            return pnt.entityIndex((Entity) child);
        } else {
            return pnt.subForceIndex((Force) child);
        }
    }

}
