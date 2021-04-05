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
import java.util.Collection;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.common.*;
import megamek.common.util.CollectionUtil;

import static megamek.client.ui.swing.util.UIUtil.*;

/** 
 * A popup menu for the lobby's player table.
 * Offers configuration, bot settings and team assignment.
 * 
 * @author Simon (Juliez)
 */
class PlayerTablePopup {

    static ScalingPopup playerTablePopup(ClientGUI clientGui, ActionListener listener, 
            Collection<IPlayer> players) {
        
        ScalingPopup popup = new ScalingPopup();
        
        var cl = clientGui.getClient();
        var isOnePlayer = players.size() == 1;
        var SinglePlayer = CollectionUtil.randomElement(players);
        var allOwnedBots = players.stream().allMatch(cl::isLocalBot);
        var isConfigurable = isOnePlayer 
                && (allOwnedBots || (cl.getLocalPlayer().equals(SinglePlayer)));
        var allConfigurable = players.stream().allMatch(p -> cl.isLocalBot(p) || cl.getLocalPlayer().equals(p));
        
        popup.add(menuItem("Player Settings...", "CONFIG", isConfigurable, listener));
        popup.add(teamMenu(allConfigurable, listener));
        popup.add(startPosMenu(allConfigurable, listener));
        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("Remove Bot", "BOTREMOVE", isOnePlayer && allOwnedBots, listener));
        popup.add(menuItem("Bot Settings...", "BOTSETTINGS", isOnePlayer && allOwnedBots, listener));
        
        return popup;
        
    }

    /** Returns the "Team" submenu, allowing to assign a player to a team. */
    private static JMenu teamMenu(boolean enabled, ActionListener listener) {
        JMenu menu = new JMenu("Assign to Team");
        for (int i = 0; i < IPlayer.MAX_TEAMS; i++) {
            JMenuItem item = menuItem(IPlayer.teamNames[i], "TEAM|" + i, enabled, listener);
            menu.add(item);
        }
        menu.setEnabled(enabled);
        return menu;
    }
    
    /** Returns the "Starting Position" submenu, allowing to assign deployment positions. */
    private static JMenu startPosMenu(boolean enabled, ActionListener listener) {
        JMenu menu = new JMenu("Deployment Area");
        for (int i = 0; i < 11; i++) {
            JMenuItem item = menuItem(IStartingPositions.START_LOCATION_NAMES[i], "DEPLOY|" + i, enabled, listener);
            menu.add(item);
        }
        menu.setEnabled(enabled);
        return menu;
    }
}

