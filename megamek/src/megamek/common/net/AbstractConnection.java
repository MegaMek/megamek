/*
* MegaMek -
* Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common.net;

import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.AbstractConnectionEvent;
import megamek.common.net.events.ConnectedEvent;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.marshall.PacketMarshaller;
import megamek.common.net.marshall.PacketMarshallerFactory;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Generic bidirectional connection between client and server
 */
public abstract class AbstractConnection {

    private static PacketMarshallerFactory marshallerFactory = PacketMarshallerFactory.getInstance();

    private static final int DEFAULT_MARSHALLING = PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING;

    /**
     * Peer Host Non null in case if it's a client connection
     */
    private String host;

    /**
     * Peer port != 0 in case if it's a client connection
     */
    private int port;

    /**
     * Connection state
     */
    private boolean open;

    /**
     * The socket for this connection.
     */
    private Socket socket;

    /**
     * The connection ID
     */
    private int id;

    /**
     * Bytes send during the connection lifecycle
     */
    private long bytesSent;

    /**
     * Bytes received during the connection lifecycle
     */
    private long bytesReceived;

    /**
     * Queue of <code>Packets</code> to send
     */
    private SendQueue sendQueue = new SendQueue();

    /**
     * Connection listeners list
     */
    private Vector<ConnectionListener> connectionListeners = new Vector<>();

    /**
     * Type of marshalling used to represent sent packets
     */
    protected int marshallingType;

    /**
     * Marshaller used to send packets
     */
    protected PacketMarshaller marshaller;

    /**
     * Indicates the need to compress sent data
     */
    private boolean zipData = true;

    /**
     * Creates new client (connection from client to server) connection
     *
     * @param host target host
     * @param port target port
     * @param id connection ID
     */
    public AbstractConnection(String host, int port, int id) {
        this.host = host;
        this.port = port;
        this.id = id;
        setMarshallingType(DEFAULT_MARSHALLING);
    }

    /**
     * Creates new Server connection
     *
     * @param socket accepted socket
     * @param id connection ID
     */
    public AbstractConnection(Socket socket, int id) {
        this.socket = socket;
        this.id = id;
        setMarshallingType(DEFAULT_MARSHALLING);
    }

    /**
     * @return <code>true</code> if it's the Server connection
     */
    public boolean isServer() {
        return host == null;
    }

    /**
     * @return the type of the marshalling used to send packets
     */
    protected int getMarshallingType() {
        return marshallingType;
    }

    /**
     * Sets the type of the marshalling used to send packets
     *
     * @param marshallingType new marhalling type
     */
    protected void setMarshallingType(int marshallingType) {
        PacketMarshaller pm = marshallerFactory.getMarshaller(marshallingType);
        assert (pm != null) : "Unknown marshalling type";
        this.marshallingType = marshallingType;
        marshaller = pm;
    }

    /**
     * Opens the connection
     *
     * @return <code>true</code> on success, <code>false</code> otherwise
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

    /**
     * Closes the socket and shuts down the receiver and sender threads
     */
    public void close() {
        synchronized (this) {
            System.err.print(getConnectionTypeAbbrevation());
            sendQueue.reportContents();
            sendQueue.finish();
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                LogManager.getLogger().error("Failed closing connection " + getId(), e);
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
     * @return the connection ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the connection ID
     * Note: Be careful with using this method
     * @param id new connection ID
     */
    public void setId(int id) {
        this.id = id;
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
     * Returns <code>true</code> if this connection compress the sent data
     *
     * @return <code>true</code> if this connection compress the sent data
     */
    public boolean isCompressed() {
        return zipData;
    }

    /**
     * Sets the compression
     *
     * @param compress
     */
    public void setCompression(boolean compress) {
        zipData = compress;
    }

    /**
     * Adds a packet to the send queue to be send on a seperate thread.
     */
    public synchronized void send(Packet packet) {
        sendQueue.addPacket(new SendPacket(packet));
        // Send right now
        flush();
    }

    /**
     * Send the packet now, on a separate thread; This is the blocking call.
     */
    public void sendNow(SendPacket packet) {
        try {
            sendNetworkPacket(packet.getData(), packet.isCompressed());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return <code>true</code> if there are pending packets
     */
    public synchronized boolean hasPending() {
        return sendQueue.hasPending();
    }

    /**
     * @return a very approximate count of how many bytes were sent
     */
    public synchronized long bytesSent() {
        return bytesSent;
    }

    /**
     * @return a very approximate count of how many bytes were received
     */
    public synchronized long bytesReceived() {
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
     * Reports receive exception to the <code>System.err</code>
     *
     * @param ex <code>Exception</code>
     * @param packet <code>Packet</code>
     */
    protected void reportSendException(Exception ex, SendPacket packet) {
        System.err.print(getConnectionTypeAbbrevation());
        System.err.print(" error sending command #");
        System.err.print(packet.getCommand());
        System.err.print(": ");
        System.err.println(ex.getMessage());
    }

    /**
     * Reports receive exception to the <code>System.err</code>
     *
     * @param ex <code>Exception</code>
     */
    protected void reportReceiveException(Exception ex) {
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
    protected void reportReceiveException(Exception ex, StringBuffer buffer) {
        System.err.print(getConnectionTypeAbbrevation());
        System.err.print(" error reading command: ");
        System.err.println(ex.getMessage());
    }

    /**
     * Appends the last commands sent/received to the given
     * <code>StringBuffer</code>
     */
    protected synchronized void reportLastCommands() {
        System.err.println();
        System.err.println();
        sendQueue.reportContents();
    }

    /**
     * Returns the the connection type abbrevation (client/server) that used in
     * the debug messages and so on.
     *
     * @return
     */
    protected String getConnectionTypeAbbrevation() {
        return isServer() ? "s:" : "c:";
    }

    /**
     * Returns an input stream
     *
     * @return an input stream
     * @throws IOException
     */
    protected InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    /**
     * Returns an output stream
     *
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
        } catch (SocketException e) {
            // Do nothing, happens when the socket closes
            close();
        } catch (EOFException e) {
            // Do nothing, happens when the socket closes
            close();
        } catch (IOException e) {
            System.out
                    .println("IOException during AbstractConnection#update()");
            close();
        } catch (Exception e) {
            e.printStackTrace();
            reportReceiveException(e);
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
        } catch (Exception e) {
            reportSendException(e, packet);
            close();
        }
    }

    /**
     * process a received packet
     */
    protected void processPacket(INetworkPacket np) throws Exception {
        PacketMarshaller pm = marshallerFactory.getMarshaller(np
                .getMarshallingType());
        assert (pm != null) : "Unknown marshalling type";
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
            processConnectionEvent(new PacketReceivedEvent(this, packet));
        }
    }

    /**
     * process a packet to be sent
     */
    protected void processPacket(SendPacket packet) throws Exception {
        sendNow(packet);
    }

    /**
     * Reads a complete <code>NetworkPacket</code> must not block, must return
     * null instead
     *
     * @return the <code>NetworkPacket</code> that was sent.
     */
    protected abstract INetworkPacket readNetworkPacket() throws Exception;

    /**
     * Sends the data. This must not be blocked for too long
     *
     * @param data data to send
     * @param zipped should the data be compressed
     * @throws Exception if there's an issue with sending the packet
     */
    protected abstract void sendNetworkPacket(byte[] data, boolean zipped) throws Exception;

    /**
     * Wrapper around a <code>LinkedList</code> for keeping a queue of packets
     * to send. Note that this implementation is not synchronized.
     */
    static class SendQueue {

        private LinkedList<SendPacket> queue = new LinkedList<>();
        private boolean finished = false;

        public void addPacket(SendPacket packet) {
            queue.add(packet);
        }

        public void finish() {
            queue.clear();
            finished = true;
        }

        /**
         * Waits for a packet to appear in the queue and then returns it.
         *
         * @return the first available packet in the queue or null if none
         */
        public SendPacket getPacket() {
            if (!finished) {
                return queue.poll();
            }
            return null;
        }

        /**
         * Returns true if this connection has pending data
         */
        public boolean hasPending() {
            return !queue.isEmpty();
        }

        public void reportContents() {
            System.err.print("Contents of Send Queue: ");
            for (SendPacket p : queue) {
                System.err.print(p.command);
            }
            System.err.println();
        }
    }

    /**
     * Processes game events occurring on this connection by dispatching them to
     * any registered GameListener objects.
     *
     * @param event the game event.
     */
    protected void processConnectionEvent(AbstractConnectionEvent event) {
        for (Enumeration<ConnectionListener> e = connectionListeners.elements(); e
                .hasMoreElements();) {
            ConnectionListener l = e.nextElement();
            switch (event.getType()) {
                case CONNECTED:
                    l.connected((ConnectedEvent) event);
                    break;
                case DISCONNECTED:
                    l.disconnected((DisconnectedEvent) event);
                    break;
                case PACKET_RECEIVED:
                    l.packetReceived((PacketReceivedEvent) event);
                    break;
            }
        }
    }

    private class SendPacket implements INetworkPacket {
        byte[] data;
        boolean zipped = false;
        PacketCommand command;

        public SendPacket(Packet packet) {
            command = packet.getCommand();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream out;
            try {
                if (zipData && packet.getData() != null) {
                    out = new GZIPOutputStream(bos);
                    zipped = true;
                } else {
                    out = bos;
                }
                marshaller.marshall(packet, out);
                out.close();
                data = bos.toByteArray();
                bytesSent += data.length;
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            return zipped;
        }

        public PacketCommand getCommand() {
            return command;
        }
    }

    /**
     * Connection layer data packet.
     */
    protected interface INetworkPacket {

        /**
         * Returns data marshalling type
         *
         * @return data marshalling type
         */
        public abstract int getMarshallingType();

        /**
         * Returns packet data
         *
         * @return packet data
         */
        public abstract byte[] getData();

        /**
         * Returns <code>true</code> if data is compressed
         *
         * @return <code>true</code> if data is compressed
         */
        public abstract boolean isCompressed();
    }
}
