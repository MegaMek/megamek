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
    
    static final String PTP_CONFIG = "CONFIG";
    static final String PTP_BOTREMOVE = "BOTREMOVE";
    static final String PTP_BOTSETTINGS = "BOTSETTINGS";
    static final String PTP_TEAM = "TEAM";
    static final String PTP_DEPLOY = "DEPLOY";
    static final String PTP_REPLACE = "REPLACE";

    static ScalingPopup playerTablePopup(ClientGUI clientGui, ActionListener listener, 
            Collection<Player> players) {
        
        ScalingPopup popup = new ScalingPopup();
        
        var cl = clientGui.getClient();
        var isOnePlayer = players.size() == 1;
        var singlePlayer = CollectionUtil.anyOneElement(players);
        var allOwnedBots = players.stream().allMatch(cl::isLocalBot);
        var isConfigurable = isOnePlayer 
                && (allOwnedBots || (cl.getLocalPlayer().equals(singlePlayer)));
        var allConfigurable = players.stream().allMatch(p -> cl.isLocalBot(p) || cl.getLocalPlayer().equals(p));
        var isSingleGhost = isOnePlayer && singlePlayer.isGhost();
        
        popup.add(menuItem("Player Settings...", PTP_CONFIG, isConfigurable, listener));
        popup.add(teamMenu(allConfigurable, listener));
        popup.add(startPosMenu(allConfigurable, listener));
        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("Remove Bot", PTP_BOTREMOVE, isOnePlayer && allOwnedBots, listener));
        popup.add(menuItem("Bot Settings...", PTP_BOTSETTINGS, isOnePlayer && allOwnedBots, listener));
        popup.add(menuItem("Replace Player...", PTP_REPLACE, isSingleGhost, listener));
        
        return popup;
        
    }

    /** Returns the "Team" submenu, allowing to assign a player to a team. */
    private static JMenu teamMenu(boolean enabled, ActionListener listener) {
        JMenu menu = new JMenu("Assign to Team");
        for (int i = 0; i < Player.TEAM_NAMES.length; i++) {
            JMenuItem item = menuItem(Player.TEAM_NAMES[i], PTP_TEAM + "|" + i, enabled, listener);
            menu.add(item);
        }
        menu.setEnabled(enabled);
        return menu;
    }
    
    /** Returns the "Starting Position" submenu, allowing to assign deployment positions. */
    private static JMenu startPosMenu(boolean enabled, ActionListener listener) {
        JMenu menu = new JMenu("Deployment Area");
        for (int i = 0; i < 11; i++) {
            JMenuItem item = menuItem(IStartingPositions.START_LOCATION_NAMES[i], PTP_DEPLOY + "|" + i, enabled, listener);
            menu.add(item);
        }
        menu.setEnabled(enabled);
        return menu;
    }
}

