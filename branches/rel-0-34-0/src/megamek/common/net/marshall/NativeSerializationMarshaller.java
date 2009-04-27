/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.net.marshall;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import megamek.common.net.Packet;

/**
 * Marshaller that Java native serialization for <code>Packet</code>
 * representation.
 */
class NativeSerializationMarshaller extends PacketMarshaller {

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.net.marshall.PacketMarshaller#marshall(megamek.common.net.Packet,
     *      java.io.OutputStream)
     */
    public void marshall(Packet packet, OutputStream stream) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(stream);
        out.writeInt(packet.getCommand());
        out.writeObject(packet.getData());
        out.flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.net.marshall.PacketMarshaller#unmarshall(java.io.InputStream)
     */
    public Packet unmarshall(InputStream stream) throws Exception {
        ObjectInputStream in = new ObjectInputStream(stream);
        int command = in.readInt();
        Object[] data = (Object[]) in.readObject();
        return new Packet(command, data);
    }

}
