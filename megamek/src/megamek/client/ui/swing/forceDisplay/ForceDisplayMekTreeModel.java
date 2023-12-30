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
package megamek.client.ui.swing.forceDisplay;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.sorters.MekTreeTopLevelSorter;
import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.options.OptionsConstants;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

public class ForceDisplayMekTreeModel extends DefaultTreeModel {
    private ClientGUI clientGUI;
    private Game game;
    /** A sorted list of all top-level objects: top-level forces and force-less entities. */
    private ArrayList<Object> allToplevel;

    public ForceDisplayMekTreeModel(ClientGUI clientGUI) {
        super(new DefaultMutableTreeNode("Root"));
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
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

        } else if (parent instanceof Force) {
            Forces forces = game.getForces();
            Force pnt = (Force) parent;
            if (index < pnt.entityCount()) {
                return game.getEntity(pnt.getEntityId(index));
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

        } else if (parent instanceof Force) {
            Force pnt = (Force) parent;
            return pnt.getChildCount(); 

        } else { // Entity
            return 0;
        }
    }

    /** 
     * Creates and stores a sorted list of the top-level forces and entities.
     * Removes those that aren't visible in real blind drop. 
     */
    private void createTopLevel() {
        boolean realBD = game.getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        Forces forces = game.getForces();
        Player localPlayer = clientGUI.getClient().getLocalPlayer();
        ArrayList<Force> toplevel = new ArrayList<>(forces.getTopLevelForces());
        if (realBD) {
            toplevel.removeIf(f -> localPlayer.isEnemyOf(forces.getOwner(f)));
        }
        List<Entity> forceless = ForceAssignable.filterToEntityList(forces.forcelessEntities());
        if (realBD) {
            forceless.removeIf(e -> localPlayer.isEnemyOf(e.getOwner()));
        }
        allToplevel = new ArrayList<>(toplevel);
        allToplevel.addAll(forceless);
        allToplevel.sort(new MekTreeTopLevelSorter(clientGUI.getClient()));
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof Entity; 
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (child == root || !(parent instanceof Force)
                || !((child instanceof Force) || (child instanceof Entity))) {
            return -1;
        }
        Force pnt = (Force) parent;
        if (child instanceof Entity) {
            return pnt.entityIndex((Entity) child);
        } else {
            return pnt.subForceIndex((Force) child);
        }
    }

}