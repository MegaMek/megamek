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
import java.net.SocketException;

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

    @Override
    protected INetworkPacket readNetworkPacket() throws Exception {
        
            NetworkPacket packet = null;
            if (in == null) {
                in = new DataInputStream(new BufferedInputStream(
                        getInputStream(), getReceiveBufferSize()));
                state = PacketReadState.Header;
            }
            synchronized (in){
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
                        packet = new NetworkPacket(zipped, encoding, data);
                        state = PacketReadState.Header;
                        return packet;
                    default:
                        assert (false);
                }
            }
        assert (false);
        return null;
    }

    @Override
    protected void sendNetworkPacket(byte[] data, boolean iszipped)
            throws Exception {
        
        if (out == null) {
            out = new DataOutputStream(new BufferedOutputStream(
                    getOutputStream(),getSendBufferSize()));
        }
        synchronized (out){
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
    public void flush() {
        // Sends all queued packets
        super.flush();
        try {
            // Flush the output stream, to ensure all packets are sent
            if (out != null)
                synchronized (out) {
                    out.flush();
                }
        } catch (SocketException se) {
            // close this connection, because it's broken
            // This can happen if the connection is closed while being written
            // to, and it's not a big deal, since the connection is being broken
            // anyways
            close();
        } catch (IOException ioe) {
            // Log non-SocketException IOExceptions
            ioe.printStackTrace();
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
