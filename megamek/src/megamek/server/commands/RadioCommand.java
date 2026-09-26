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
package megamek.server.commands;

import java.util.Arrays;

import megamek.codeUtilities.MathUtility;
import megamek.common.Player;
import megamek.common.event.GameToastEvent;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Relays a bot's radio call about one of its units to that unit's own side, as a toast with the unit's icon and a line
 * in the chat log. Enemies never receive it, since a call names the hex a unit is heading for - the same reason
 * artillery call-for-fire toasts go to the firing team only.
 *
 * <p>Only the unit's owner may send a call about it.</p>
 *
 * <p>Example: {@code /radio 12 Command Two: Set at 1504, holding.}</p>
 */
public class RadioCommand extends ServerCommand {

    private static final MMLogger LOGGER = MMLogger.create(RadioCommand.class);

    public static final String COMMAND_NAME = "radio";

    private final TWGameManager gameManager;

    public RadioCommand(Server server, TWGameManager gameManager) {
        super(server, COMMAND_NAME, "Relays a bot's radio call about its unit to its own side. "
              + "Usage: /radio <unit id> <message>");
        this.gameManager = gameManager;
    }

    /**
     * Builds the chat text a bot sends to make a radio call.
     *
     * @param unitId  the unit the call is about
     * @param message the call, already in the bot's voice
     *
     * @return the command, e.g. {@code /radio 12 Command Two: Set at 1504, holding.}
     */
    public static String commandText(int unitId, String message) {
        return "/" + COMMAND_NAME + ' ' + unitId + ' ' + message;
    }

    @Override
    public void run(int connId, String[] args) {
        if (args.length < 3) {
            server.sendServerChat(connId, getHelp());
            return;
        }
        Entity unit = gameManager.getGame().getEntity(MathUtility.parseInt(args[1], Entity.NONE));
        Player sender = gameManager.getGame().getPlayer(connId);
        if ((unit == null) || (sender == null) || (unit.getOwnerId() != sender.getId())) {
            LOGGER.debug("[BotOrders] radio call refused from connection {} about unit {}", connId, args[1]);
            return;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String origin = "Radio[" + sender.getName() + "]";
        int listeners = 0;
        for (Player player : gameManager.getGame().getPlayersList()) {
            if (player.isEnemyOf(sender)) {
                continue;
            }
            server.sendChat(player.getId(), origin, message);
            gameManager.send(player.getId(), new Packet(PacketCommand.SEND_TOAST, GameToastEvent.Level.INFO, message,
                  unit.getId()));
            listeners++;
        }
        LOGGER.info("[BotOrders] radio call about {} (ID {}) sent to {} player(s) on its side: {}",
              unit.getDisplayName(), unit.getId(), listeners, message);
    }
}
