/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.net.connections;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import megamek.common.net.enums.PacketReadState;
import megamek.logging.MMLogger;

/**
 * Implementation of the AbstractConnection that uses the DataInputStream and DataOutputStream to send/receive data.
 */
public class DataStreamConnection extends AbstractConnection {
    private static final MMLogger LOGGER = MMLogger.create(DataStreamConnection.class);

    private DataInputStream in;
    private DataOutputStream out;

    /**
     * Creates new server connection.
     *
     * @param socket The network socket to use
     * @param id     The connection ID
     */
    public DataStreamConnection(Socket socket, int id) {
        super(socket, id);
    }

    /**
     * Creates new Client connection.
     *
     * @param host The host address
     * @param port The network port
     * @param id   The connection ID
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
    protected void sendNetworkPacket(byte[] data, boolean isZipped) throws Exception {
        if (out == null) {
            out = new DataOutputStream(new BufferedOutputStream(getOutputStream(), getSendBufferSize()));
        }

        synchronized (out) {
            out.writeBoolean(isZipped);
            out.writeInt(marshallingType);
            out.writeInt((data != null) ? data.length : 0);
            out.write(data);
        }
    }

    /**
     * override flush to flush the data stream after flushing packet queue
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
            // close this connection, because it's broken. This can happen if the connection is closed while being
            // written to, and it's not a big deal, since the connection is being broken anyway
            close();
        } catch (IOException ex) {
            LOGGER.error("", ex);
            // close this connection, because it's broken
            close();
        }
    }

    @Override
    public String toString() {
        return "DataStreamConnection Id " + getId();
    }
}
