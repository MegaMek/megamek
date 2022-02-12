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
package megamek.common.net.marshall;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import megamek.MMConstants;
import megamek.common.net.Packet;
import org.nibblesec.tools.SerialKiller;

/**
 * Marshaller that Java native serialization for <code>Packet</code> representation.
 */
public class NativeSerializationMarshaller extends PacketMarshaller {
    @Override
    public void marshall(Packet packet, OutputStream stream) throws Exception {
        ObjectOutputStream out = new ObjectOutputStream(stream);
        out.writeInt(packet.getCommand());
        out.writeObject(packet.getData());
        out.flush();
    }

    @Override
    public Packet unmarshall(InputStream stream) throws Exception {
        ObjectInputStream in = new SerialKiller(stream, MMConstants.SERIALKILLER_CONFIG_FILE);
        int command = in.readInt();
        Object[] data = (Object[]) in.readObject();
        return new Packet(command, data);
    }
}
