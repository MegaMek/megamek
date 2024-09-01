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
        if (gameManager instanceof GameManager gm) {
            if (gm.getGame().getBotSettings().containsKey(playerName)) {
                // apply this only if there is already a Princess with settings registered for the player name
                gm.getGame().getBotSettings().compute(playerName, (k, oldSettings) -> settingsBuilder.build(oldSettings));
//TODO                gm.send(player.getId(), new Packet(PacketCommand.PRINCESS_SETTINGS, settings));
            } else {
                LogManager.getLogger().warn("PrincessSettingsEvent found no bot player with the right name");
            }
        } else {
            LogManager.getLogger().error("PrincessSettingsEvent is only available in TW games");
        }
    }

    @Override
    public Trigger trigger() {
        return trigger;
    }
}
