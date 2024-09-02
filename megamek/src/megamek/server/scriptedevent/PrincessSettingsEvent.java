/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.server.scriptedevent;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.Player;
import megamek.common.jacksonadapters.PrincessSettingsBuilder;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.GameManager;
import megamek.server.IGameManager;
import megamek.server.trigger.Trigger;
import org.apache.logging.log4j.LogManager;
import megamek.common.Game;

public class PrincessSettingsEvent implements TriggeredActiveEvent {

    private final Trigger trigger;
    private final String playerName;
    private final PrincessSettingsBuilder settingsBuilder;

    /**
     * Creates a scripted even that changes the Princess settings for the bot player with the given name to
     * the given settings. This event only works in TW games and will do nothing if no Princess settings are
     * previously registered under the player name.
     *
     * @param trigger The trigger that activates this event
     * @param playerName The Princess player to change
     * @param settingsBuilder The new settings for Princess to use
     * @see Game#getBotSettings()
     */
    public PrincessSettingsEvent(Trigger trigger, String playerName, PrincessSettingsBuilder settingsBuilder) {
        this.trigger = trigger;
        this.playerName = playerName;
        this.settingsBuilder = settingsBuilder;
    }

    @Override
    public void process(IGameManager gameManager) {
        if (!validateData(gameManager)) {
            return;
        }

        GameManager gm = (GameManager) gameManager;
        int id = findPlayerId(playerName, gm);
        BehaviorSettings newSettings = settingsBuilder.build(gm.getGame().getBotSettings().get(playerName));
        gm.getGame().getBotSettings().put(playerName, newSettings);
        gm.send(id, new Packet(PacketCommand.CHANGE_PRINCESS_SETTINGS, newSettings));
    }

    private boolean validateData(IGameManager gameManager) {
        if (!(gameManager instanceof GameManager gm)) {
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

    @Override
    public Trigger trigger() {
        return trigger;
    }
}
