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
import javax.swing.JMenuItem;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.common.*;
import static megamek.client.ui.swing.util.UIUtil.*;

/** 
 * A popup menu for the lobby's player table.
 * Offers configuration, bot settings and team assignment.
 * 
 * @author Simon (Juliez)
 */
class PlayerTablePopup {

    static ScalingPopup playerTablePopup(ClientGUI clientGui, ActionListener listener, int playerID) {
        
        ScalingPopup popup = new ScalingPopup();
        
        Client cl = clientGui.getClient();
        boolean isOwnedBot = cl.bots.containsKey(cl.getGame().getPlayer(playerID).getName());
        boolean isConfigurable = isOwnedBot || (clientGui.getClient().getLocalPlayerNumber() == playerID);
        int currentTeam = cl.getGame().getPlayer(playerID).getTeam();
        
        popup.add(menuItem("Player Settings...", "CONFIG", isConfigurable, listener));
        popup.add(teamMenu(isConfigurable, listener, currentTeam));
        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("Remove Bot", "BOTREMOVE", isOwnedBot, listener));
        popup.add(menuItem("Bot Settings...", "BOTSETTINGS", isOwnedBot, listener));

        return popup;
        
    }

    /** Returns the "Team" submenu, allowing assigning a player to a team. */
    private static JMenu teamMenu(boolean enabled, ActionListener listener, int currentTeam) {

        JMenu menu = new JMenu("Assign to Team");
        for (int i = 0; i < IPlayer.MAX_TEAMS; i++) {
            JMenuItem item = menuItem(IPlayer.teamNames[i], "TEAM|" + i, enabled, listener);
            item.setEnabled(i != currentTeam);
            menu.add(item);
        }
        menu.setEnabled(enabled);
        return menu;
    }
}

