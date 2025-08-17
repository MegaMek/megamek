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

import static megamek.client.ui.util.UIUtil.menuItem;

import java.awt.event.ActionListener;
import java.util.Collection;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.ScalingPopup;
import megamek.common.Board;
import megamek.common.IStartingPositions;
import megamek.common.Player;
import megamek.common.util.CollectionUtil;

/**
 * A popup menu for the lobby's player table. Offers configuration, bot settings and team assignment.
 *
 * @author Simon (Juliez)
 */
class PlayerTablePopup {

    static final String PTP_CONFIG = "CONFIG";
    static final String PTP_BOT_REMOVE = "BOTREMOVE";
    static final String PTP_BOT_SETTINGS = "BOTSETTINGS";
    static final String PTP_TEAM = "TEAM";
    static final String PTP_DEPLOY = "DEPLOY";
    static final String PTP_REPLACE = "REPLACE";

    static JPopupMenu playerTablePopup(ClientGUI clientGui, ActionListener listener,
          Collection<Player> players, Board currentBoard) {

        JPopupMenu popup = new ScalingPopup();

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
        popup.add(startPosMenu(allConfigurable, listener, currentBoard));
        popup.add(ScalingPopup.spacer());
        popup.add(menuItem("Remove Bot", PTP_BOT_REMOVE, isOnePlayer && allOwnedBots, listener));
        popup.add(menuItem("Bot Settings...", PTP_BOT_SETTINGS, isOnePlayer && allOwnedBots, listener));
        popup.add(menuItem("Edit Bots...", PTP_REPLACE, isSingleGhost, listener));

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
    private static JMenu startPosMenu(boolean enabled, ActionListener listener, Board currentBoard) {
        JMenu menu = new JMenu("Deployment Area");
        for (int i = 0; i < Board.NUM_ZONES; i++) {
            JMenuItem item = menuItem(IStartingPositions.START_LOCATION_NAMES[i],
                  PTP_DEPLOY + "|" + i,
                  enabled,
                  listener);
            menu.add(item);
        }

        for (int i : currentBoard.getCustomDeploymentZones()) {
            JMenuItem item = menuItem("Zone " + i,
                  PTP_DEPLOY + "|" + Board.encodeCustomDeploymentZoneID(i),
                  enabled,
                  listener);
            menu.add(item);
        }

        menu.setEnabled(enabled);
        return menu;
    }
}

