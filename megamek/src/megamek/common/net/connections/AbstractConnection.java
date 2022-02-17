/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import megamek.common.net.enums.PacketMarshallerMethod;
import megamek.common.net.events.AbstractConnectionEvent;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.factories.PacketMarshallerFactory;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.marshalling.AbstractPacketMarshaller;
import megamek.common.net.packets.INetworkPacket;
import megamek.common.net.packets.Packet;
import megamek.common.net.packets.SendPacket;
import megamek.common.util.CircularIntegerBuffer;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

/**
 * Generic bidirectional connection between client and server
 */
public abstract class AbstractConnection {
    //region Variable Declarations
    protected static final PacketMarshallerMethod[] PACKET_MARSHALLER_METHODS = PacketMarshallerMethod.values();

    /** The connection id */
    private int id;

    /** Peer Host. This is null if this is a server connection, and non-null otherwise */
    private String host;

    /** Peer port != 0 in case if it's a client connection */
    private int port;

    /** The socket for this connection. */
    private Socket socket;

    /** Connection state */
    private boolean open;

    /** Bytes send during the connection lifecycle */
    private long bytesSent = 0L;

    /** Bytes received during the connection lifecycle */
    private long bytesReceived;

    /** Queue of {@link Packet}s to send */
    private SendQueue sendQueue = new SendQueue();

    /** Connection listeners list */
    private List<ConnectionListener> connectionListeners = new Vector<>();

    /** Buffer of the last commands sent; Used for debugging purposes. */
    private CircularIntegerBuffer debugLastFewCommandsSent = new CircularIntegerBuffer(50);

    /** Buffer of the last commands received; Used for debugging purposes. */
    private CircularIntegerBuffer debugLastFewCommandsReceived = new CircularIntegerBuffer(50);

    /** Type of marshalling used to represent sent packets */
    protected PacketMarshallerMethod marshallingMethod;

    /** Marshaller used to send packets */
    protected AbstractPacketMarshaller marshaller;

    /** Indicates the need to compress sent data */
    private boolean compressed = true;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates new client (connection from client to server) connection
     *
     * @param id connection id
     * @param host target host
     * @param port target port
     */
    public AbstractConnection(final int id, final String host, final int port) {
        this(id, host, port, null);
    }

    /**
     * Creates new Server connection
     *
     * @param id connection id
     * @param socket accepted socket
     */
    public AbstractConnection(final int id, final Socket socket) {
        this(id, null, 0, socket);
    }

    public AbstractConnection(final int id, final @Nullable String host, final int port,
                              final @Nullable Socket socket) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.socket = socket;
        setMarshallingMethod(PacketMarshallerMethod.NATIVE_SERIALIZATION_MARSHALLING);
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * @return the connection id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the connection id
     * Note: Be careful with using this method.
     *
     * @param id new connection id
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     * @return a very approximate count of how many bytes were sent
     */
    public synchronized long getBytesSent() {
        return bytesSent;
    }

    public synchronized void increaseBytesSent(final long bytesSent) {
        setBytesSent(this.bytesSent + bytesSent);
    }

    public synchronized void setBytesSent(final long bytesSent) {
        this.bytesSent = bytesSent;
    }

    /**
     * @return a very approximate count of how many bytes were received
     */
    public synchronized long bytesReceived() {
        return bytesReceived;
    }

    public synchronized void increaseBytesReceived(final long bytesReceived) {
        setBytesSent(this.bytesReceived + bytesReceived);
    }

    public synchronized void setBytesReceived(final long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    /**
     * @return the method of the marshalling used to send packets
     */
    protected PacketMarshallerMethod getMarshallingMethod() {
        return marshallingMethod;
    }

    /**
     * Sets the type of the marshalling used to send packets
     * @param marshallingMethod new marshalling method
     */
    protected void setMarshallingMethod(final PacketMarshallerMethod marshallingMethod) {
        this.marshallingMethod = marshallingMethod;
        marshaller = Objects.requireNonNull(PacketMarshallerFactory.getInstance()
                .getMarshaller(marshallingMethod), "Unimplemented marshalling type");
    }

    /**
     * @return <code>true</code> if this connection compresses the sent data
     */
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * @param compressed if this connection is to be compressed
     */
    public void setCompressed(final boolean compressed) {
        this.compressed = compressed;
    }
    //endregion Getters/Setters

    /**
     * @return <code>true</code> if it's the Server connection
     */
    public boolean isServer() {
        return host == null;
    }

    /**
     * Opens the connection.
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    public synchronized boolean open() {
        if (!open) {
            if (socket == null) {
                try {
                    socket = new Socket(host, port);
                } catch (Exception ignored) {
                    return false;
                }
            }
            open = true;
        }
        return true;
    }

    /**
     * Closes the socket and shuts down the receiver and sender threads
     */
    public void close() {
        synchronized (this) {
            LogManager.getLogger().info("Starting to shut down " + (isServer() ? "server" : "client"));
            sendQueue.reportContents();
            sendQueue.finish();
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("Failed closing connection " + getId(), ex);
            }
            socket = null;
        }

        processConnectionEvent(new DisconnectedEvent(this));
    }

    /**
     * @return if the socket for this connection has been closed.
     */
    public boolean isClosed() {
        return (socket == null) || socket.isClosed();
    }

    /**
     * @return the address this socket is or was connected to
     */
    public String getInetAddress() {
        if (socket != null) {
            return socket.getInetAddress().toString();
        }
        return "Unknown";
    }

    /**
     * Adds a packet to the send queue to be sent on a separate thread.
     */
    public synchronized void send(Packet packet) {
        sendQueue.addPacket(new SendPacket(packet));
        // Send right now
        flush();
    }

    /**
     * Send the packet now, on a separate thread; This is the blocking call.
     */
    public void sendNow(final SendPacket packet) {
        try {
            sendNetworkPacket(packet.getData(), packet.isCompressed());
            debugLastFewCommandsSent.push(packet.getCommand().ordinal());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * @return <code>true</code> if there are pending packets
     */
    public synchronized boolean hasPending() {
        return sendQueue.hasPending();
    }

    /**
     * Adds the specified connection listener to receive connection events from connection.
     *
     * @param listener the connection listener.
     */
    public void addConnectionListener(final ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    /**
     * Removes the specified connection listener.
     *
     * @param listener the connection listener.
     */
    public void removeConnectionListener(final ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    /**
     * Reports receive exception to the <code>System.err</code>
     *
     * @param ex <code>Exception</code>
     * @param packet <code>Packet</code>
     */
    protected void reportSendException(final Exception ex, final SendPacket packet) {
        System.err.print(getConnectionTypeAbbreviation());
        System.err.print(" error sending command #");
        System.err.print(packet.getCommand());
        System.err.print(": ");
        System.err.println(ex.getMessage());
        reportLastCommands();
    }

    /**
     * Reports receive exception to the <code>System.err</code>
     *
     * @param ex <code>Exception</code>
     */
    protected void reportReceiveException(final Exception ex) {
        StringBuffer message = new StringBuffer();
        reportReceiveException(ex, message);
        System.err.println(message);
    }

    /**
     * Appends the receive exception report to the given
     * <code>StringBuffer</code>
     *
     * @param ex <code>Exception</code>
     */
    protected void reportReceiveException(final Exception ex, final StringBuffer buffer) {
        System.err.print(getConnectionTypeAbbreviation());
        System.err.print(" error reading command: ");
        System.err.println(ex.getMessage());
        reportLastCommands();
    }

    /**
     * Appends the last commands sent/received to the given
     * <code>StringBuffer</code>
     */
    protected synchronized void reportLastCommands() {
        reportLastCommands(true);
        System.err.println();
        reportLastCommands(false);
        System.err.println();
        sendQueue.reportContents();
    }

    /**
     * Appends the last commands sent or received to the given
     * <code>StringBuffer</code> depending on the <code>sent</code> parameter
     *
     * @param sent indicates which commands (sent/received) should be reported
     */
    protected void reportLastCommands(final boolean sent) {
        CircularIntegerBuffer buf = sent ? debugLastFewCommandsSent
                : debugLastFewCommandsReceived;
        System.err.print("    Last ");
        System.err.print(buf.length());
        System.err.print(" commands that were ");
        System.err.print(sent ? "sent" : "received");
        System.err.print(" (oldest first): ");
        System.err.println(buf);
    }

    /**
     * @return the connection type abbreviation (client/server) that used in the debug messages and
     * so on.
     */
    protected String getConnectionTypeAbbreviation() {
        return isServer() ? "s:" : "c:";
    }

    /**
     * @return an input stream
     * @throws IOException
     */
    protected InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    /**
     * @return an output stream
     * @throws IOException
     */
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
     * Process all incoming data, blocking on the input stream until new input is available. This
     * method should not be synchronized as it should only deal with the input side of things.
     * Without creating separate read/write locks, making this method synchronized would not allow
     * synchronous reads and writes.
     */
    public void update() {
        try {
            INetworkPacket np;
            while ((np = readNetworkPacket()) != null) {
                processPacket(np);
            }
        } catch (SocketException | EOFException ignored) {
            // Do nothing, happens when the socket closes
            close();
        } catch (IOException ex) {
            LogManager.getLogger().error("", ex);
            close();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            reportReceiveException(ex);
            close();
        }
    }

    /**
     * Send all queued packets. This method is synchronized since it deals with the non-thread-safe
     * send queue.
     */
    public synchronized void flush() {
        SendPacket packet = null;
        try {
            while ((packet = sendQueue.getPacket()) != null) {
                processPacket(packet);
            }
        } catch (Exception ex) {
            if (packet == null) {
                LogManager.getLogger().error("Attempted to process null packet", ex);
            } else {
                reportSendException(ex, packet);
            }
            close();
        }
    }

    /**
     * process a received packet
     */
    protected void processPacket(final INetworkPacket np) throws Exception {
        AbstractPacketMarshaller pm = Objects.requireNonNull(PacketMarshallerFactory.getInstance()
                        .getMarshaller(np.getMarshallingMethod()), "Unknown marshalling type");
        byte[] data = np.getData();
        bytesReceived += data.length;
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        InputStream in;
        if (np.isCompressed()) {
            in = new GZIPInputStream(bis);
        } else {
            in = bis;
        }
        Packet packet = pm.unmarshall(in);
        if (packet != null) {
            debugLastFewCommandsReceived.push(packet.getCommand().ordinal());
            processConnectionEvent(new PacketReceivedEvent(this, packet));
        }
    }

    /**
     * process a packet to be sent
     */
    protected void processPacket(final SendPacket packet) {
        sendNow(packet);
    }

    /**
     * Reads a complete <code>NetworkPacket</code>. Must not block, must return null instead
     *
     * @return the <code>NetworkPacket</code> that was sent, or null if it can't be read.
     */
    protected abstract @Nullable INetworkPacket readNetworkPacket() throws Exception;

    /**
     * Sends the data. This must not be blocked for too long
     *
     * @param data data to send
     * @param zipped should the data be compressed
     * @throws Exception if there's an issue with sending the packet
     */
    protected abstract void sendNetworkPacket(final byte[] data, final boolean zipped) throws Exception;

    /**
     * Processes game events occurring on this connection by dispatching them to
     * any registered GameListener objects.
     *
     * @param event the game event.
     */
    protected void processConnectionEvent(final AbstractConnectionEvent event) {
        connectionListeners.forEach(listener -> event.getType().processListener(event, listener));
    }


    protected static class SendQueue {

    }
}
