/*
 * MegaMek
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.net;

import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Implementation of the <code>Connection</code> that uses the
 * <code>DataInputStream</code> and <code>DataOutputStream</code> to
 * send/receive data.
 */
class DataStreamConnection extends AbstractConnection {
    private DataInputStream in;
    private DataOutputStream out;

    /**
     * Creates new server connection
     */
    public DataStreamConnection(Socket socket, int id) {
        super(socket, id);
    }

    /**
     * Creates new Client connection
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

    @Override
    protected INetworkPacket readNetworkPacket() throws Exception {
        if (in == null) {
            in = new DataInputStream(new BufferedInputStream(getInputStream(), getReceiveBufferSize()));
            state = PacketReadState.Header;
        }

        synchronized (in) {
            switch (state) {
                case Header:
                    zipped = in.readBoolean();
                    encoding = in.readInt();
                    len = in.readInt();
                    state = PacketReadState.Data;
                    // drop through on purpose
                case Data:
                    byte[] data = new byte[len];
                    in.readFully(data);
                    NetworkPacket packet = new NetworkPacket(zipped, encoding, data);
                    state = PacketReadState.Header;
                    return packet;
                default:
                    throw new Exception("Attempting to read network packet with unknown state");
            }
        }
    }

    @Override
    protected void sendNetworkPacket(byte[] data, boolean iszipped) throws Exception {
        if (out == null) {
            out = new DataOutputStream(new BufferedOutputStream(getOutputStream(), getSendBufferSize()));
        }

        synchronized (out) {
            out.writeBoolean(iszipped);
            out.writeInt(marshallingType);
            out.writeInt(data.length);
            out.write(data);
        }
    }

    /**
     * override flush to flush the datastream after flushing packetqueue
     */
    @Override
    public synchronized void flush() {
        // Sends all queued packets
        super.flush();
        try {
            // Flush the output stream, to ensure all packets are sent
            if (out != null) {
                synchronized (out) {
                    out.flush();
                }
            }
        } catch (SocketException ignored) {
            // close this connection, because it's broken
            // This can happen if the connection is closed while being written to, and it's not a
            // big deal, since the connection is being broken anyways
            close();
        } catch (IOException ex) {
            LogManager.getLogger().error("", ex);
            // close this connection, because it's broken
            close();
        }
    }

    @Override
    public String toString() {
        return "DataStreamConnection Id " + getId();
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
         */
        NetworkPacket(boolean compressed, int marshallingType, byte... data) {
            this.compressed = compressed;
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
            return compressed;
        }
    }
}

enum PacketReadState {
    Header, // next will be header data
    Data
}
