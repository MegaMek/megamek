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

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.IGameManager;
import megamek.server.trigger.Trigger;

public class MessageScriptedEvent implements ScriptedEvent {

    private final Trigger trigger;
    private final String message;
    private final String header;

    public MessageScriptedEvent(Trigger trigger, String header, String message) {
        this.trigger = trigger;
        this.message = message;
        this.header = header;
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    @Override
    public void process(IGameManager gameManager) {
        gameManager.send(new Packet(PacketCommand.SCRIPTED_MESSAGE, header, message));
    }
}
