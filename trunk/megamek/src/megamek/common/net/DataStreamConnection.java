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

package megamek.common.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Implementation of the <code>Connection</code> that uses the
 * <code>DataInputStream</code> and <code>DataOutputStream</code>
 * to send/receive data.    
 *
 */
class DataStreamConnection extends Connection {

    /**
     * Input stream
     */
    private DataInputStream in;

    /**
     * Output stream 
     */
    private DataOutputStream out;

    /**
     * Creates new server connection
     * @param socket
     * @param id
     */
    public DataStreamConnection(Socket socket, int id) {
        super(socket, id);
    }

    /**
     * Creates new Client connection
     * @param host
     * @param port
     * @param id
     */
    public DataStreamConnection(String host, int port, int id) {
        super(host, port, id);
    }

    protected INetworkPacket readNetworkPacket() throws Exception {
        NetworkPacket packet = null;
        if (in == null) {
            in = new DataInputStream(getInputStream());
        }

        boolean zipped = in.readBoolean();
        int encoding = in.readInt();
        int len = in.readInt();
        byte[] data = new byte[len];
        in.readFully(data);
        packet = new NetworkPacket(zipped, encoding, data);
        return packet;
    }

    protected void sendNetworkPacket(byte[] data, boolean zipped) throws Exception {
        if (out == null) {
            out = new DataOutputStream(getOutputStream());
            out.flush();
        }

        out.writeBoolean(zipped);
        out.writeInt(marshallingType);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    private static class NetworkPacket  implements INetworkPacket {
        
        /**
         * Is data compressed
         */
        private boolean compressed;
        
        /**
         * Data marshalling type
         */
        private int marshallingType;
        
        /**
         * Packet data
         */
        private byte[] data;
        
        /**
         * Creates new packet
         * @param compressed
         * @param marshallingType
         * @param data
         */
        NetworkPacket(boolean compressed, int marshallingType, byte[] data) {
            this.compressed = compressed;
            this.marshallingType = marshallingType;
            this.data = data;
        }
        
        public int getMarshallingType() {
            return marshallingType;
        }
        
        public byte[] getData() {
            return data;
        }
        
        public boolean isCompressed() {
            return compressed;
        }
    };    
}
