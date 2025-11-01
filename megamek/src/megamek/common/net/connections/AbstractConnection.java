/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import megamek.common.net.events.AbstractConnectionEvent;
import megamek.common.net.events.ConnectedEvent;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.marshalling.PacketMarshaller;
import megamek.common.net.marshalling.PacketMarshallerFactory;
import megamek.common.net.packets.Packet;
import megamek.logging.MMLogger;

/**
 * Generic bidirectional connection between client and server
 */
public abstract class AbstractConnection {
    private static final MMLogger LOGGER = MMLogger.create(AbstractConnection.class);

    private static final PacketMarshallerFactory marshallerFactory = PacketMarshallerFactory.getInstance();
    private static final int DEFAULT_MARSHALLING = PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING;

    private Socket socket;
    private int connectionId;

    /** Peer Host Non-null in case if it's a client connection */
    private String host;

    /** Peer port != 0 in case if it's a client connection */
    private int port;

    /** Connection state */
    private boolean open;

    /** Bytes send during the connection lifecycle */
    private long bytesSent;

    /** Bytes received during the connection lifecycle */
    private long bytesReceived;

    /** Queue of Packets to send */
    private final SendQueue sendQueue = new SendQueue();

    /** Connection listeners list */
    private final Vector<ConnectionListener> connectionListeners = new Vector<>();

    /** Type of marshalling used to represent sent packets */
    protected int marshallingType;

    /** Marshaller used to send packets */
    protected PacketMarshaller marshaller;

    /** Indicates the need to compress sent data */
    private boolean zipData = true;

    /**
     * Creates new client (connection from client to server) connection
     *
     * @param host target host
     * @param port target port
     * @param id   connection ID
     */
    public AbstractConnection(String host, int port, int id) {
        this.host = host;
        this.port = port;
        connectionId = id;
        setMarshallingType(DEFAULT_MARSHALLING);
    }

    /**
     * Creates new Server connection
     *
     * @param socket accepted socket
     * @param id     connection ID
     */
    public AbstractConnection(Socket socket, int id) {
        this.socket = socket;
        connectionId = id;
        setMarshallingType(DEFAULT_MARSHALLING);
    }

    /** @return True if this is the Server connection. */
    public boolean isServer() {
        return host == null;
    }

    /** @return The type of the marshalling used to send packets. */
    protected int getMarshallingType() {
        return marshallingType;
    }

    /**
     * Sets the type of the marshalling used to send packets
     *
     * @param marshallingType new marshalling type
     */
    protected void setMarshallingType(int marshallingType) {
        PacketMarshaller pm = marshallerFactory.getMarshaller(marshallingType);
        Objects.requireNonNull(pm);
        this.marshallingType = marshallingType;
        marshaller = pm;
    }

    /**
     * Opens the connection
     *
     * @return True on success, false otherwise
     */
    public synchronized boolean open() {
        if (!open) {
            if (socket == null) {
                try {
                    socket = new Socket(host, port);
                } catch (Exception e) {
                    return false;
                }
            }
            open = true;
        }
        return true;
    }

    /** Closes the socket and shuts down the receiver and sender threads. */
    public void close() {
        synchronized (this) {
            LOGGER.info("Starting to close {}", getConnectionTypeText());
            sendQueue.reportContents();
            sendQueue.finish();
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                LOGGER.error("Failed closing connection {}", getId(), e);
            }
            socket = null;
        }
        processConnectionEvent(new DisconnectedEvent(this));
    }

    /**
     * @return True if the socket for this connection has been closed (or is null).
     */
    public boolean isClosed() {
        return (socket == null) || socket.isClosed();
    }

    /** @return the connection ID. */
    public int getId() {
        return connectionId;
    }

    /**
     * Sets the connection ID Note: Be careful with using this method
     *
     * @param id new connection ID
     */
    public void setId(int id) {
        connectionId = id;
    }

    /** @return The address this socket is or was connected to. */
    public String getInetAddress() {
        if (socket != null) {
            return socket.getInetAddress().toString();
        }
        return "Unknown";
    }

    /** @return True if this connection compresses the sent data. */
    public boolean isCompressed() {
        return zipData;
    }

    /**
     * Sets the compression
     *
     * @param compress True when compression is to be used
     */
    public void setCompression(boolean compress) {
        zipData = compress;
    }

    /** Adds a packet to the send queue to be sent on a separate thread. */
    public synchronized void send(Packet packet) {
        try {
            sendQueue.addPacket(new SendPacket(packet, this));
            // Send right now
            flush();
        } catch (Exception e) {
            LOGGER.error("Failed to send packet {}", packet, e);
        }
    }

    /** Send the packet now, on a separate thread; This is the blocking call. */
    public void sendNow(SendPacket packet) {
        try {
            sendNetworkPacket(packet.data(), packet.isCompressed());
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }
    }

    /** @return True if there are pending packets. */
    public synchronized boolean hasPending() {
        return sendQueue.hasPending();
    }

    /** @return a very approximate count of how many bytes were sent. */
    public synchronized long getBytesSent() {
        return bytesSent;
    }

    /** @return a very approximate count of how many bytes were received. */
    public synchronized long getBytesReceived() {
        return bytesReceived;
    }

    /**
     * Adds the specified connection listener to receive connection events from connection.
     *
     * @param listener the connection listener.
     */
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.addElement(listener);
    }

    /**
     * Removes the specified connection listener.
     *
     * @param listener the connection listener.
     */
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.removeElement(listener);
    }

    /**
     * @return The connection type ("Client" / "Server") that is used in the debug messages and so on.
     */
    protected String getConnectionTypeText() {
        return isServer() ? "Server" : "Client";
    }

    /** @return an input stream */
    protected InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    /** @return an output stream */
    protected OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    protected int getSendBufferSize() throws SocketException {
        return socket.getSendBufferSize();
    }

    protected int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }

    /**
     * Process all incoming data, blocking on the input stream until new input is available. This method should not be
     * synchronized as it should only deal with the input side of things. Without creating separate read/write locks,
     * making this method synchronized would not allow synchronous reads and writes.
     */
    public void update() {
        try {
            INetworkPacket networkPacket;
            while ((networkPacket = readNetworkPacket()) != null) {
                processPacket(networkPacket);
            }
        } catch (SocketException | EOFException ignored) {
            // Do nothing, happens when the socket closes
            close();
        } catch (IOException ex) {
            LOGGER.error(getConnectionTypeText(), ex);
            close();
        } catch (Exception ex) {
            LOGGER.error("{} had an error receiving a packet", getConnectionTypeText(), ex);
            close();
        }
    }

    /**
     * Send all queued packets. This method is synchronized since it deals with the non-thread-safe send queue.
     */
    public synchronized void flush() {
        SendPacket packet = null;
        try {
            while ((packet = sendQueue.getPacket()) != null) {
                processPacket(packet);
            }
        } catch (Exception ex) {
            if (packet == null) {
                LOGGER.error(ex, "{} had an error sending a null packet", getConnectionTypeText());
            } else {
                LOGGER.error(ex,
                      "{} had an error sending command {}",
                      getConnectionTypeText(),
                      packet.getCommand().name());
            }
            close();
        }
    }

    /** Processes a received packet. */
    protected void processPacket(INetworkPacket np) throws Exception {
        PacketMarshaller pm = marshallerFactory.getMarshaller(np.marshallingType());
        Objects.requireNonNull(pm);
        byte[] data = np.data();
        bytesReceived += data.length;
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        InputStream in = np.isCompressed() ? new GZIPInputStream(bis) : bis;
        Packet packet = pm.unmarshall(in);
        if (packet != null) {
            processConnectionEvent(new PacketReceivedEvent(this, packet));
        }
    }

    /** Processes a packet to be sent. */
    protected void processPacket(SendPacket packet) {
        sendNow(packet);
    }

    /**
     * Reads a complete NetworkPacket. This method must not block, must return null instead.
     *
     * @return the NetworkPacket that was sent.
     */
    protected abstract INetworkPacket readNetworkPacket() throws Exception;

    /**
     * Sends the data. This must not be blocked for too long
     *
     * @param data   data to send
     * @param zipped should the data be compressed
     *
     * @throws Exception if there's an issue with sending the packet
     */
    protected abstract void sendNetworkPacket(byte[] data, boolean zipped) throws Exception;

    /**
     * Processes game events occurring on this connection by dispatching them to any registered GameListener objects.
     *
     * @param event the game event.
     */
    protected void processConnectionEvent(AbstractConnectionEvent event) {
        for (ConnectionListener listener : connectionListeners) {
            switch (event.getType()) {
                case CONNECTED:
                    listener.connected((ConnectedEvent) event);
                    break;
                case DISCONNECTED:
                    listener.disconnected((DisconnectedEvent) event);
                    break;
                case PACKET_RECEIVED:
                    listener.packetReceived((PacketReceivedEvent) event);
                    break;
            }
        }
    }

    void addBytesSent(int additionalBytesSent) {
        bytesSent += additionalBytesSent;
    }
}
