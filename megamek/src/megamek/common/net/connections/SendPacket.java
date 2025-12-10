/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.net.connections;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.logging.MMLogger;

public class SendPacket implements INetworkPacket {
    private static final MMLogger logger = MMLogger.create(SendPacket.class);

    private byte[] data;
    private boolean zipped = false;
    private final PacketCommand command;
    private final AbstractConnection connection;

    public SendPacket(Packet packet, AbstractConnection connection) throws java.io.NotSerializableException {
        command = packet.command();
        this.connection = connection;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream out;
        try {
            if (connection.isCompressed() && (packet.data() != null)) {
                out = new GZIPOutputStream(bos);
                zipped = true;
            } else {
                out = bos;
            }
            connection.marshaller.marshall(packet, out);
            out.close();
            data = bos.toByteArray();
            connection.addBytesSent(data.length);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    @Override
    public int marshallingType() {
        return connection.getMarshallingType();
    }

    @Override
    public byte[] data() {
        return data;
    }

    @Override
    public boolean isCompressed() {
        return zipped;
    }

    public PacketCommand getCommand() {
        return command;
    }
}
