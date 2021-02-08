/*  
 * MegaMek - Copyright (C) 2021 - The MegaMek Team  
 *  
 * listener program is free software; you can redistribute it and/or modify it under  
 * the terms of the GNU General Public License as published by the Free Software  
 * Foundation; either version 2 of the License, or (at your option) any later  
 * version.  
 *  
 * listener program is distributed in the hope that it will be useful, but WITHOUT  
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
 * details.  
 */ 
package megamek.client.ui.swing.lobby;

import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.tree.TreePath;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.common.*;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import static megamek.client.ui.swing.util.UIUtil.*;

public class MekForceTreePopup {

    static ScalingPopup getPopup(ClientGUI clientGui, TreePath selection, 
            ActionListener listener, ChatLounge lobby) {

        Entity entity = null;
        Force force = null;
        boolean isForce = false;
        
        Object selected = selection.getLastPathComponent();
        if (selected instanceof Entity && lobby.isEditable((Entity)selected)) {
            entity = (Entity) selected;
        } else if (selected instanceof Force) {
            force = (Force) selected;
            isForce = true;
        } else {
            return new ScalingPopup();
        }
        
        ScalingPopup popup = new ScalingPopup();

        if (isForce) {
            popup.add(menuItem("Create new Force...", "CREATETOP", true, listener));
            popup.add(menuItem("Create new Subforce...", "CREATESUB|" + force.getId(), true, listener));
            popup.add(menuItem("Promote to Top-Level Force", "PROMOTE|" + force.getId(), true, listener));
            popup.add(menuItem("Rename", "RENAME|" + force.getId(), true, listener));
            popup.add(forceMenu(clientGui, force, true, listener));
//            Forces forces = clientGui.getClient().getGame().getForces();
//            IPlayer localPlayer = clientGui.getClient().getLocalPlayer();
//            for (Force availForce: forces.getAvailableForces(localPlayer)) {
//                String command = "ATTACHTO|" + force.getId() + "|" + availForce.getId();
//                popup.add(menuItem("Attach to " + availForce.getName(), command, true, listener));
//            }
        } else {
            popup.add(entityMenu(clientGui, entity, true, listener));
        }

        return popup;
    }

    /**
     * Returns the "Force" submenu, allowing assignment to forces
     */
    private static JMenu forceMenu(ClientGUI clientGui, Force force, boolean enabled, ActionListener listener) {
        JMenu menu = new JMenu("Attach to");
        menu.setEnabled(enabled);

//        menu.add(menuItem("Create new Force...", "CREATETOP", enabled, listener));
//        menu.add(menuItem("Create new Subforce...", "CREATESUB|" + force.getId(), enabled, listener));
//        menu.add(menuItem("Promote to Top-Level Force", "PROMOTE|" + force.getId(), enabled, listener));
        //            for (String f: Forces.getAvailableForces(clientGui.getClient().getGame(), clientGui.getClient().getLocalPlayer())) {
        //                menu.add(menuItem("Add to " + f, "LANCE|ADD|" + f, enabled, listener));
        //            }
//        menu.add(menuItem("Attach to Force", "LANCE|REMOVE", enabled, listener));
        Forces forces = clientGui.getClient().getGame().getForces();
        IPlayer localPlayer = clientGui.getClient().getLocalPlayer();
        for (Force availForce: forces.getAvailableForces(localPlayer)) {
            String command = "ATTACHTO|" + force.getId() + "|" + availForce.getId();
            menu.add(menuItem("Attach to " + availForce.getName(), command, enabled, listener));
        }
        return menu;
    }
    
    /**
     * Returns the "Force" submenu, allowing assignment to forces
     */
    private static JMenu entityMenu(ClientGUI clientGui, Entity entity, boolean enabled, ActionListener listener) {
        JMenu menu = new JMenu("Force");
        menu.setEnabled(enabled);

        menu.add(menuItem("Create new Top-Level Force...", "CREATETOP", enabled, listener));
//        menu.add(menuItem("Assign to Force...", "CREATETOP", enabled, listener));
//        menu.add(menuItem("Create new Subforce...", "CREATESUB|" + force.getId(), enabled, listener));
//        menu.add(menuItem("Promote to Top-Level Force", "PROMOTE|" + force.getId(), enabled, listener));
        
        Forces forces = clientGui.getClient().getGame().getForces();
        IPlayer localPlayer = clientGui.getClient().getLocalPlayer();
        for (Force force: forces.getAvailableForces(localPlayer)) {
            String command = "ADDTO|" + entity.getId() + "|" + force.getId();
            menu.add(menuItem("Add to " + force.getName(), command, enabled, listener));
        }
        menu.add(menuItem("Remove from Force", "LANCE|REMOVE", enabled, listener));
        return menu;
    }




}


