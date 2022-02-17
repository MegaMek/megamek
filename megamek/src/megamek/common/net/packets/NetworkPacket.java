/*
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
package megamek.common.net.packets;

import megamek.common.net.enums.PacketMarshallerMethod;

public class NetworkPacket implements INetworkPacket {
    //region Variable Declarations
    /** Data marshalling method */
    private final PacketMarshallerMethod marshallingMethod;

    /** Is the data compressed */
    private final boolean compressed;

    /** Packet data*/
    private final byte[] data;
    //endregion Variable Declarations

    //region Constructors
    public NetworkPacket(final PacketMarshallerMethod marshallingMethod,
                         final boolean compressed, final byte... data) {
        this.marshallingMethod = marshallingMethod;
        this.compressed = compressed;
        this.data = data;
    }
    //endregion Constructors

    //region Getters
    @Override
    public PacketMarshallerMethod getMarshallingMethod() {
        return marshallingMethod;
    }

    @Override
    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public byte[] getData() {
        return data;
    }
    //endregion Getters
}
