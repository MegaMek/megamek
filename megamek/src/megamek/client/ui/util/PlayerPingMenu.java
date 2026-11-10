/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.util;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jakarta.annotation.Nullable;
import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.logging.MMLogger;
import megamek.server.commands.PingCommand;

/**
 * Adds a ping entry to a player list's right-click menu. Right-clicking another player offers "Ping this player";
 * right-clicking yourself, or no player, offers "Ping everyone". Both send the {@code /ping} chat command, so the server
 * applies the same checks and cooldown as when the command is typed.
 */
public final class PlayerPingMenu {

    private static final MMLogger LOGGER = MMLogger.create(PlayerPingMenu.class);

    private PlayerPingMenu() {}

    /**
     * Adds the ping entry that fits the right-clicked row to the end of the given menu, after a separator. Adds
     * nothing for a disconnected player, who could not hear the ping.
     *
     * @param popup  the menu to add to
     * @param client the local client that sends the ping
     * @param target the player that was right-clicked, or {@code null} when no player was
     */
    public static void addTo(JPopupMenu popup, Client client, @Nullable Player target) {
        if ((target != null) && target.isGhost()) {
            return;
        }
        boolean isOtherPlayer = (target != null) && (target.getId() != client.getLocalPlayerNumber());
        JMenuItem pingItem;
        if (isOtherPlayer) {
            pingItem = new JMenuItem(Messages.getString("PlayerPing.menu.ping"));
            pingItem.addActionListener(event -> sendPing(client, target));
        } else {
            pingItem = new JMenuItem(Messages.getString("PlayerPing.menu.pingEveryone"));
            pingItem.addActionListener(event -> sendPing(client, null));
        }
        popup.addSeparator();
        popup.add(pingItem);
    }

    private static void sendPing(Client client, @Nullable Player target) {
        String command = PingCommand.commandText(target);
        LOGGER.info("[PlayerPing] menu sent {}", command);
        client.sendChat(command);
    }
}
