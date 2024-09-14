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

import megamek.client.ui.Base64Image;
import megamek.common.annotations.Nullable;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.IGameManager;
import megamek.server.trigger.Trigger;

import java.awt.*;

public class MessageTriggeredActiveEvent implements TriggeredActiveEvent {

    private final Trigger trigger;
    private final String message;
    private final String header;
    private final Base64Image image;

    public MessageTriggeredActiveEvent(Trigger trigger, String header, String message, @Nullable Image image) {
        this.trigger = trigger;
        this.message = message;
        this.header = header;
        this.image = new Base64Image(image);
    }

    public MessageTriggeredActiveEvent(Trigger trigger, String header, String message) {
        this(trigger, header, message, null);
    }

    @Override
    public Trigger trigger() {
        return trigger;
    }

    @Override
    public void process(IGameManager gameManager) {
        gameManager.send(new Packet(PacketCommand.SCRIPTED_MESSAGE, header, message, image));
    }
}
