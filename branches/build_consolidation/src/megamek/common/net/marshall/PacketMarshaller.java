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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import megamek.common.net.Packet;

/**
 * Generic marshaller that [un]marshalls the <code>Packet</code>
 */
public abstract class PacketMarshaller {

    /**
     * Java native serialization marshalling
     */
    public static final int NATIVE_SERIALIZATION_MARSHALING = 0;

    /**
     * XML serialization marshalling
     */
    public static final int XML_MARSHALING = 1;

    /**
     * Marshalls the packet data into the <code>byte[]</code>
     * 
     * @param packet packet to marshall
     * @return marshalled representation of the given <code>Packet</code>
     */
    public byte[] marshall(Packet packet) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            marshall(packet, bo);
            bo.flush();
            return bo.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Marshalls the packet data into the given <code>OutputStream</code>
     * 
     * @param packet packet to marshall
     * @param stream <code>OutputStream</code> to marshall the
     *            <code>Packet</code> to
     * @throws Exception
     */
    public abstract void marshall(Packet packet, OutputStream stream)
            throws Exception;

    /**
     * Unmarshalls the packet data from the given <code>byte[]</code> array
     * 
     * @param data <code>byte[]</code> array to unmarshall the packet from
     * @return the new <code>Packet</code>unmarshalled from the given
     *         <code>byte[]</code> array
     */
    public Packet unmarshall(byte[] data) {
        try {
            return unmarshall(new ByteArrayInputStream(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Unmarshalls the packet data from the given <code>InputStream</code>
     * 
     * @param stream <code>InputStream</code> to unmarshall the packet from
     * @return the new <code>Packet</code>unmarshalled from the given
     *         <code>InputStream</code>
     * @throws Exception
     */
    public abstract Packet unmarshall(InputStream stream) throws Exception;

}
