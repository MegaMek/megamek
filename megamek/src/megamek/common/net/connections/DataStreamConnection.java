/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.net.connections;

import megamek.common.net.enums.PacketReadState;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Implementation of the AbstractConnection that uses the DataInputStream and
 * DataOutputStream to send/receive data.
 */
public class DataStreamConnection extends AbstractConnection {

    private DataInputStream in;
    private DataOutputStream out;

    /**
     * Creates new server connection.
     * 
     * @param socket The network socket to use
     * @param id The connection ID
     */
    public DataStreamConnection(Socket socket, int id) {
        super(socket, id);
    }

    /**
     * Creates new Client connection.
     * 
     * @param host The host address
     * @param port The network port
     * @param id The connection ID
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
    protected PacketReadState state = PacketReadState.HEADER;

    @Override
    protected INetworkPacket readNetworkPacket() throws Exception {
        NetworkPacket packet;
        if (in == null) {
            in = new DataInputStream(new BufferedInputStream(getInputStream(), getReceiveBufferSize()));
            state = PacketReadState.HEADER;
        }

        synchronized (in) {
            switch (state) {
                case HEADER:
                    zipped = in.readBoolean();
                    encoding = in.readInt();
                    len = in.readInt();
                    state = PacketReadState.DATA;
                    // Purposeful drop through
                case DATA:
                    byte[] data = new byte[len];
                    in.readFully(data);
                    packet = new NetworkPacket(zipped, encoding, data);
                    state = PacketReadState.HEADER;
                    return packet;
                default:
                    throw new Exception("Cannot Read Network Packet with unknown state " + state.name());
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
            // close this connection, because it's broken. This can happen if the connection is closed while
            // being written to, and it's not a big deal, since the connection is being broken anyway
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
}