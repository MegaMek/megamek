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
package megamek.common.net;

import megamek.common.net.enums.PacketMarshallerMethod;
import megamek.common.net.enums.PacketReadState;
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
    protected PacketMarshallerMethod marshallerMethod = PacketMarshallerMethod.NATIVE_SERIALIZATION_MARSHALLING;
    protected int len = 0;
    protected PacketReadState state = PacketReadState.HEADER;

    @Override
    protected INetworkPacket readNetworkPacket() throws Exception {
        if (in == null) {
            in = new DataInputStream(new BufferedInputStream(getInputStream(), getReceiveBufferSize()));
            state = PacketReadState.HEADER;
        }

        synchronized (in) {
            switch (state) {
                case HEADER:
                    marshallerMethod = PACKET_MARSHALLER_METHODS[in.readInt()];
                    zipped = in.readBoolean();
                    len = in.readInt();
                    state = PacketReadState.DATA;
                    // drop through on purpose
                case DATA:
                    byte[] data = new byte[len];
                    in.readFully(data);
                    NetworkPacket packet = new NetworkPacket(marshallerMethod, zipped, data);
                    state = PacketReadState.HEADER;
                    return packet;
                default:
                    LogManager.getLogger().error("Attempting to read network packet with unknown state " + state);
                    return null;
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
            out.writeInt(marshallingMethod.ordinal());
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

        if (out == null) {
            return;
        }

        try {
            // Flush the output stream, to ensure all packets are sent
            synchronized (out) {
                out.flush();
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
        //region Variable Declarations
        /** Data marshalling method */
        private final PacketMarshallerMethod marshallingMethod;

        /** Is the data compressed */
        private final boolean compressed;

        /** Packet data*/
        private final byte[] data;
        //endregion Variable Declarations

        //region Constructors
        public NetworkPacket(final PacketMarshallerMethod marshallingMethod,
                             final boolean compressed, final byte... data) {
            this.marshallingMethod = marshallingMethod;
            this.compressed = compressed;
            this.data = data;
        }
        //endregion Constructors

        //region Getters
        @Override
        public PacketMarshallerMethod getMarshallingMethod() {
            return marshallingMethod;
        }

        @Override
        public boolean isCompressed() {
            return compressed;
        }

        @Override
        public byte[] getData() {
            return data;
        }
        //endregion Getters
    }
}
