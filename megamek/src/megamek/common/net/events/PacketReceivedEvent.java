/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.net.events;

import megamek.common.net.Packet;
import megamek.common.net.enums.ConnectionEventType;

/**
 * Instances of this class are sent when packet received
 */
public class PacketReceivedEvent extends AbstractConnectionEvent {
    //region Variable Declarations
    private static final long serialVersionUID = -3542045596045067466L;
    private final Packet packet;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructs connection event
     *
     * @param source The object on which the Event initially occurred.
     * @param packet The received packet
     */
    public PacketReceivedEvent(final Object source, final Packet packet) {
        super(ConnectionEventType.PACKET_RECEIVED, source);
        this.packet = packet;
    }
    //endregion Constructors

    //region Getters
    /**
     * @return the received packet
     */
    public Packet getPacket() {
        return packet;
    }
    //endregion Getters
}
