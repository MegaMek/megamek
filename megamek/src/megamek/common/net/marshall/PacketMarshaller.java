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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import megamek.common.annotations.Nullable;
import megamek.common.net.Packet;
import org.apache.logging.log4j.LogManager;

/**
 * Generic marshaller that [un]marshalls the <code>Packet</code>
 */
public abstract class PacketMarshaller {

    /**
     * Java native serialization marshalling
     */
    public static final int NATIVE_SERIALIZATION_MARSHALING = 0;

    /**
     * Marshalls the packet data into the <code>byte[]</code>
     *
     * @param packet packet to marshall
     * @return marshalled representation of the given <code>Packet</code>
     */
    public @Nullable byte[] marshall(Packet packet) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            marshall(packet, bo);
            bo.flush();
            return bo.toByteArray();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return null;
        }
    }

    /**
     * Marshalls the packet data into the given <code>OutputStream</code>
     *
     * @param packet packet to marshall
     * @param stream <code>OutputStream</code> to marshall the <code>Packet</code> to
     * @throws Exception
     */
    public abstract void marshall(Packet packet, OutputStream stream) throws Exception;

    /**
     * Unmarshalls the packet data from the given <code>byte[]</code> array
     *
     * @param data <code>byte[]</code> array to unmarshall the packet from
     * @return the new <code>Packet</code>unmarshalled from the given <code>byte[]</code> array,
     * or null if there's an exception
     */
    public @Nullable Packet unmarshall(byte... data) {
        try {
            return unmarshall(new ByteArrayInputStream(data));
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return null;
        }
    }

    /**
     * Unmarshalls the packet data from the given <code>InputStream</code>
     *
     * @param stream <code>InputStream</code> to unmarshall the packet from
     * @return the new <code>Packet</code>unmarshalled from the given <code>InputStream</code>
     * @throws Exception
     */
    public abstract Packet unmarshall(InputStream stream) throws Exception;
}
