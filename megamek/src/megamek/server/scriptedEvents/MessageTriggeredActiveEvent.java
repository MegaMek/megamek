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

package megamek.server.scriptedEvents;

import java.awt.Image;

import megamek.client.ui.Base64Image;
import megamek.common.annotations.Nullable;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.server.IGameManager;
import megamek.server.trigger.Trigger;

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

    @Override
    public String toString() {
        return "Message: " + trigger + ", \"" + message.substring(0, Math.min(message.length(), 20)) + "...\"";
    }

}
