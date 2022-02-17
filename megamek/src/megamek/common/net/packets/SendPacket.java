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

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.enums.PacketMarshallerMethod;
import org.apache.logging.log4j.LogManager;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class SendPacket implements INetworkPacket {
    private final PacketCommand command;
    private boolean zipped = false;
    private byte[] data;

    public SendPacket(Packet packet) {
        command = packet.getCommand();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream out;
        try {
            if (compressed && (packet.getData() != null)) {
                out = new GZIPOutputStream(bos);
                zipped = true;
            } else {
                out = bos;
            }
            marshaller.marshall(packet, out);
            out.close();
            data = bos.toByteArray();
            bytesSent += data.length;
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    public PacketCommand getCommand() {
        return command;
    }

    @Override
    public PacketMarshallerMethod getMarshallingMethod() {
        return marshallingMethod;
    }

    @Override
    public boolean isCompressed() {
        return zipped;
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
