/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.net.marshalling;

import megamek.MMConstants;
import megamek.common.net.packets.Packet;
import megamek.common.net.enums.PacketCommand;
import org.nibblesec.tools.SerialKiller;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Marshaller that Java native serialization for <code>Packet</code> representation.
 */
class NativeSerializationMarshaller extends PacketMarshaller {
    protected static final PacketCommand[] PACKET_COMMANDS = PacketCommand.values();

    @Override
    public void marshall(final Packet packet, final OutputStream stream) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(stream);
        out.writeInt(packet.getCommand().ordinal());
        out.writeObject(packet.getData());
        out.flush();
    }

    @Override
    public Packet unmarshall(final InputStream stream) throws Exception {
        final ObjectInputStream in = new SerialKiller(stream, MMConstants.SERIALKILLER_CONFIG_FILE);
        return new Packet(PACKET_COMMANDS[in.readInt()], (Object[]) in.readObject());
    }
}
