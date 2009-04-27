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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Implementation of the <code>Connection</code> that uses the
 * <code>DataInputStream</code> and <code>DataOutputStream</code> to
 * send/receive data.
 */
class DataStreamConnection extends AbstractConnection {

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
     * 
     * @param socket
     * @param id
     */
    public DataStreamConnection(Socket socket, int id) {
        super(socket, id);
    }

    /**
     * Creates new Client connection
     * 
     * @param host
     * @param port
     * @param id
     */
    public DataStreamConnection(String host, int port, int id) {
        super(host, port, id);
    }

    /**
     * store data for packet reception statemachine
     */
    protected boolean zipped = false;
    protected int encoding = -1;
    protected int len = 0;
    protected PacketReadState state = PacketReadState.Header;

    protected INetworkPacket readNetworkPacket() throws Exception {
        NetworkPacket packet = null;
        if (in == null) {
            in = new DataInputStream(new BufferedInputStream(getInputStream(),
                    1024));
            state = PacketReadState.Header;
        }

        switch (state) {
            case Header:
                if (in.available() < 9)
                    return null;
                zipped = in.readBoolean();
                encoding = in.readInt();
                len = in.readInt();
                state = PacketReadState.Data;
                // drop through on purpose
            case Data:
                // we want to let huge packets block a bit..
                if (len < 1000 || in.available() < 500) {
                    if (in.available() < len)
                        return null;
                }
                byte[] data = new byte[len];
                in.readFully(data);
                packet = new NetworkPacket(zipped, encoding, data);
                state = PacketReadState.Header;
                return packet;
            default:
                assert (false);
        }
        assert (false);
        return null;
    }

    protected void sendNetworkPacket(byte[] data, boolean zipped)
            throws Exception {
        if (out == null) {
            out = new DataOutputStream(new BufferedOutputStream(
                    getOutputStream()));
            // out.flush(); thsi should be unnecessary?
        }

        out.writeBoolean(zipped);
        out.writeInt(marshallingType);
        out.writeInt(data.length);
        out.write(data);
        // out.flush(); avoid flushing before all packets are sent
    }

    /**
     * override flush to flush the datastream after flushing packetqueue
     */
    public synchronized void flush() {
        super.flush();
        try {
            if (out != null)
                out.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // close this connection, because it's broken
            close();

        }
    }

    private static class NetworkPacket implements INetworkPacket {

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
         * 
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
    }
}

enum PacketReadState {
    Header, // next will be header data
    Data;
}
