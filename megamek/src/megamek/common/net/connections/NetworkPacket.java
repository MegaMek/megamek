/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.net.connections;

class NetworkPacket implements INetworkPacket {

    private final boolean isCompressed;
    private final int marshallingType;
    private final byte[] data;

    /**
     * Creates new packet
     *
     * @param compressed True when the data is compressed
     * @param marshallingType The marhsalling type used
     * @param data The packet data
     */
    NetworkPacket(boolean compressed, int marshallingType, byte[] data) {
        isCompressed = compressed;
        this.marshallingType = marshallingType;
        this.data = data;
    }

    @Override
    public int getMarshallingType() {
        return marshallingType;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public boolean isCompressed() {
        return isCompressed;
    }
}
