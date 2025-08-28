/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.scriptedEvent;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.jacksonAdapters.PrincessSettingsBuilder;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.IGameManager;
import megamek.server.totalWarfare.TWGameManager;
import megamek.server.trigger.Trigger;
import org.apache.logging.log4j.LogManager;

public record PrincessSettingsEvent(Trigger trigger, String playerName, PrincessSettingsBuilder settingsBuilder)
      implements TriggeredActiveEvent {

    /**
     * Creates a scripted even that changes the Princess settings for the bot player with the given name to the given
     * settings. This event only works in TW games and will do nothing if no Princess settings are previously registered
     * under the player name.
     *
     * @param trigger         The trigger that activates this event
     * @param playerName      The Princess player to change
     * @param settingsBuilder The new settings for Princess to use
     *
     * @see Game#getBotSettings()
     */
    public PrincessSettingsEvent {
    }

    @Override
    public void process(IGameManager gameManager) {
        if (!validateData(gameManager)) {
            return;
        }

        TWGameManager gm = (TWGameManager) gameManager;
        int id = findPlayerId(playerName, gm);
        BehaviorSettings newSettings = settingsBuilder.build(gm.getGame().getBotSettings().get(playerName));
        gm.getGame().getBotSettings().put(playerName, newSettings);
        gm.send(id, new Packet(PacketCommand.CHANGE_PRINCESS_SETTINGS, newSettings));
    }

    private boolean validateData(IGameManager gameManager) {
        if (!(gameManager instanceof TWGameManager gm)) {
            LogManager.getLogger().error("PrincessSettingsEvent is only available in TW games");
            return false;
        } else if (findPlayerId(playerName, gameManager) == Player.PLAYER_NONE) {
            LogManager.getLogger().warn("No player ID for the player name {}", playerName);
            return false;
        } else if (!gm.getGame().getBotSettings().containsKey(playerName)) {
            LogManager.getLogger().warn("No bot settings known for the player name {}", playerName);
            return false;
        }
        return true;
    }

    private int findPlayerId(String playerName, IGameManager gameManager) {
        for (Player player : gameManager.getGame().getPlayersList()) {
            if (playerName.equals(player.getName())) {
                return player.getId();
            }
        }
        return Player.PLAYER_NONE;
    }
}
