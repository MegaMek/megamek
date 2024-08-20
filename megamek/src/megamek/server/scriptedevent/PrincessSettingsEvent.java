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
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.GameManager;
import megamek.server.IGameManager;
import megamek.server.trigger.Trigger;
import org.apache.logging.log4j.LogManager;

public class PrincessSettingsEvent implements TriggeredActiveEvent {

    private final Trigger trigger;
    private final String playerName;
    private final BehaviorSettings settings;

    /**
     * Creates a scripted even that changes the Princess settings for the bot player with the given name to
     * the given settings.
     *
     * @param trigger The trigger that activates this event
     * @param playerName The Princess player to change
     * @param settings The new settings for Princess to use
     */
    public PrincessSettingsEvent(Trigger trigger, String playerName, BehaviorSettings settings) {
        this.trigger = trigger;
        this.playerName = playerName;
        this.settings = settings;
    }

    @Override
    public void process(IGameManager gameManager) {
        if (gameManager instanceof GameManager gm) {
            for (Player player : gm.getGame().getPlayersList()) {
                if (player.getName().equals(playerName) && player.isBot()) {
                    gm.send(player.getId(), new Packet(PacketCommand.PRINCESS_SETTINGS, settings));
                    return;
                }
            }
            // only reached when no bot player of the name was found
            LogManager.getLogger().warn("PrincessSettingsEvent found no bot player with the right name");
        } else {
            LogManager.getLogger().error("PrincessSettingsEvent is only available in TW games");
        }
    }

    @Override
    public Trigger trigger() {
        return trigger;
    }
}
