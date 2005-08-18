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

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import megamek.common.net.marshall.PacketMarshaller;
import megamek.common.net.marshall.PacketMarshallerFactory;
import megamek.common.util.CircularIntegerBuffer;

/**
 * Generic bidirectional connection between client and server
 */
public abstract class Connection {

    /*
     * mev wrote:
     * This class provides common reusable code for both Client and Server.
     * I've constructed it from the Server & client implementations of the 
     * read/write functionality.
     * 
     * I'm not quite sure in the interface and the implementation of this
     * class and descendants, so comments/suggestions are welcome.  
     */
    
    private static PacketMarshallerFactory marshallerFactory = PacketMarshallerFactory.getInstance();

    private static final int DEFAULT_MARSHALLING = PacketMarshaller.NATIVE_SERIALIZATION_MARSHALING;

    /**
     * Peer Host
     * Non null in case if it's a client connection
     */
    private String host;
    
    /**
     * Peer port
     * != 0 in case if it's a client connection
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
     * Receiver thread
     */
    private Thread receiver;

    /**
     * Sender thread 
     */
    private Thread sender;

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
    private Vector connectionListeners = new Vector();

    /**
     * Buffer of the last commands sent; Used for debugging purposes.
     */
    private CircularIntegerBuffer debugLastFewCommandsSent =
        new CircularIntegerBuffer(5);

    /**
     * Buffer of the last commands received; Used for debugging purposes.
     */
    private CircularIntegerBuffer debugLastFewCommandsReceived =
        new CircularIntegerBuffer(5);

    /**
     * Type of marshalling used to represent sent packets
     */
    protected int marshallingType;

    /**
     * Marshaller used to send packets
     */
    private PacketMarshaller marshaller;
    
    /**
     * Indicates the need to compress sent data
     */
    private boolean zipData=true;
    
    /**
     * Creates new client (connection from client to server) connection
     * @param host target host
     * @param port target port
     * @param id connection ID
     */
    public Connection(String host, int port, int id) {
        this.host = host;
        this.port = port;
        this.id = id;
        setMarshallingType(DEFAULT_MARSHALLING);
    }
    
    /**
     * Creates new Server connection
     * @param socket accepted socket
     * @param id connection ID
     */
    public Connection(Socket socket, int id) {
        this.socket = socket;
        this.id = id;
        setMarshallingType(DEFAULT_MARSHALLING);
    }

    /**
     * Returns <code>true</code> if it's the Server connection
     * @return <code>true</code> if it's the Server connection
     */
    public boolean isServer() {
        return host == null;
    }
    
    /**
     * Returns the type of the marshalling used to send packets
     * @return the type of the marshalling used to send packets
     */
    public int getMarshallingType() {
        return marshallingType;
    }

    /**
     * Sets the type of the marshalling used to send packets 
     * @param marshallingType new marhalling type
     */
    public void setMarshallingType(int marshallingType) {
        PacketMarshaller pm = marshallerFactory.getMarshaller(marshallingType);
        megamek.debug.Assert.assertTrue(pm != null, "Unknown marshalling type");
        this.marshallingType = marshallingType;
        marshaller = pm;
    }
    
    /**
     * Opens the connection
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
            initThreads();
        }
        return true;
    }

    /**
     * Closes the socket and shuts down the receiver and sender threads
     */
    public void close() {
        synchronized (this) {
            sendQueue.finish();
            receiver = null;
            sender = null;
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.print( "Error closing connection #" ); //$NON-NLS-1$
                System.err.print( getId() );
                System.err.print( ": " ); //$NON-NLS-1$
                System.err.println( e.getMessage() );
                // We don't need a full stack trace... we're
                // just closing the connection anyway.
                //e.printStackTrace();
            } catch (NullPointerException ex) {
                //never initialized, poor thing
            }
            socket = null;
        }
        processConnectionEvent(new DisconnectedEvent(this));
    }

    /**
     * Returns the connection ID
     * @return the connection ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the connection ID
     * @param id new connection ID
     * Be careful with this...
     */
    public void setId(int id) {
        this.id = id;
    }
    
    public String getInetAddress() {
        if (socket != null) {
            return socket.getInetAddress().toString();
        }
        else {
            return "Unknown";
        }
    }

    /**
     * Returns <code>true</code> if this connection compress the sent data
     * @return <code>true</code> if this connection compress the sent data
     */
    public boolean isCompressed() {
        return zipData;
    }

    /**
     * Sets the compression
     * @param compress
     */
    public void setCompression(boolean compress) {
        zipData = compress;
    }

    /**
     * Adds a packet to the send queue to be send on a seperate thread.
     */
    public void send(Packet packet) {
        sendQueue.addPacket(packet);
    }

    /**
     * Send packet now; This is the blocking call.
     */
    public synchronized void sendNow(Packet packet) {
        boolean zipped = false;
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
            byte[] data = bos.toByteArray();
            sendNetworkPacket(data, zipped);
            bytesSent += data.length;
            debugLastFewCommandsSent.push(packet.getCommand());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Returns <code>true</code> if there are pending packets
     * @return <code>true</code> if there are pending packets
     */
    public boolean hasPending() {
        return sendQueue.hasPending();
    }

    /**
     * Returns a very approximate count of how many bytes were sent
     * @return a very approximate count of how many bytes were sent
     */
    public synchronized long bytesSent() {
        return bytesSent;
    }

    /**
     * Returns a very approximate count of how many bytes were received
     * @return a very approximate count of how many bytes were received
     */
    public synchronized long bytesReceived() {
        return bytesReceived;        
    }

    /**
     * Adds the specified connection listener to receive
     * connection events from connection.
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
     * @param ex <code>Exception</code>
     * @param packet <code>Packet</code>
     */
    protected void reportSendException(Exception ex, Packet packet) {
        StringBuffer message = new StringBuffer();
        reportSendException(ex, packet, message);
        System.err.println(message);
    }

    /**
     * Reports send exception
     * @param ex <code>Exception</code>
     * @param packet <code>Packet</code>
     */
    protected void reportSendException(Exception ex, Packet packet, StringBuffer buffer) {
        buffer.append(getConnectionTypeAbbrevation());
        buffer.append(" error sending command #").append(packet.getCommand()); //$NON-NLS-1$
        buffer.append(": ").append(ex.getMessage()); //$NON-NLS-1$
        buffer.append('\n'); //$NON-NLS-1$
        reportLastCommands(buffer);
    }

    /**
     * Reports receive exception to the <code>System.err</code> 
     * @param ex <code>Exception</code>
     */
    protected void reportReceiveException(Exception ex) {
        StringBuffer message = new StringBuffer();
        reportReceiveException(ex, message);
        System.err.println(message);
    }

    /**
     * Appends the receive exception report to the given <code>StringBuffer</code> 
     * @param ex <code>Exception</code>
     */
    protected void reportReceiveException(Exception ex, StringBuffer buffer) {
        buffer.append(getConnectionTypeAbbrevation());
        buffer.append(" error reading command: ").append(ex.getMessage()); //$NON-NLS-1$
        buffer.append('\n'); //$NON-NLS-1$
        reportLastCommands(buffer);
    }

    /**
     * Appends the last commands sent/received to the given <code>StringBuffer</code>
     * @param buffer <code>StringBuffer</code> to add the report to 
     */
    protected void reportLastCommands(StringBuffer buffer) {
        reportLastCommands(buffer, true);
        buffer.append('\n'); //$NON-NLS-1$
        reportLastCommands(buffer, false);
    }

    /**
     * Appends the last commands sent or received to the given <code>StringBuffer</code>
     * dependig on the <code>sent</code> parameter
     * @param buffer <code>StringBuffer</code> to add the report to
     * @param sent indicates which commands (sent/received) should be reported
     */
    protected void reportLastCommands(StringBuffer buffer, boolean sent) {
        CircularIntegerBuffer buf = sent?debugLastFewCommandsSent:debugLastFewCommandsReceived;
        buffer.append("    Last ").append(buf.length()).append(" commands that were "); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append(sent?"sent":"received").append(" (oldest first): ").append(buf); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
    }

    /**
     * Returns the the connection type abbrevation (client/server) that used
     * in the debug messages and so on. 
     * @return
     */
    protected String getConnectionTypeAbbrevation() {
        return isServer()?"s:":"c:"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns an input stream
     * @return an input stream
     * @throws IOException
     */
    protected InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    /**
     * Returns an output stream
     * @return an output stream
     * @throws IOException
     */
    protected OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    /**
     * Initializes the sender and receiver threads
     */
    private void initThreads() {

        Runnable receiverRunnable = new Runnable() {
            public void run() {
                while (receiver == Thread.currentThread()) {
                    INetworkPacket np=null;
                    try {
                        np = readNetworkPacket();
                        if (np != null) {
                            processPacket(np);
                        }
                    } catch (Exception e) {
                        reportReceiveException(e);
                        close();
                    }                   
                }
            }

            protected void processPacket(INetworkPacket np) throws Exception {
                PacketMarshaller pm = marshallerFactory.getMarshaller(np.getMarshallingType());
                megamek.debug.Assert.assertTrue(pm != null, "Unknown marshalling type");
                Packet packet = null;
                byte[] data = np.getData();
                bytesReceived += data.length;
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                InputStream in;
                if (np.isCompressed()) {
                    in = new GZIPInputStream(bis);                                
                } else {
                    in = bis;
                }
                packet = pm.unmarshall(in);
                if (packet != null) {
                    debugLastFewCommandsReceived.push(packet.getCommand());
                    processConnectionEvent(new PacketReceivedEvent(Connection.this, packet));
                }
                
            }
        };        
        receiver = new Thread(receiverRunnable, "Packet Receiver (" + getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

        Runnable senderRunable = new Runnable() {
            public void run() {
                while (sender == Thread.currentThread()) {
                    Packet packet = sendQueue.getPacket();
                    if (packet != null) {
                        try {
                            processPacket(packet);
                        }catch (Exception e) {
                            reportSendException(e, packet);
                            close();
                        }
                    }
                }
            }

            protected void processPacket(Packet packet) throws Exception {
                sendNow(packet);
            }
        };        
        sender = new Thread(senderRunable, "Packet Sender (" + getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        
        receiver.start();        
        sender.start();
    }
    
    /**
     * Reads a complete <code>NetworkPacket</code>
     * @return  the <code>NetworkPacket</code> that was sent.
     */
    protected abstract INetworkPacket readNetworkPacket() throws Exception;

    /**
     * Sends the data
     * @param data data to send
     * @param zipped should the data be compressed
     * @throws Exception
     */
    protected abstract void sendNetworkPacket(byte[] data, boolean zipped) throws Exception;
    
    private static class SendQueue {

        private Vector queue = new Vector();
        private boolean finished = false;

        public synchronized void addPacket(Packet packet) {
            queue.addElement(packet);
            notifyAll();
        }

        public synchronized void finish() {
            queue.removeAllElements();
            finished = true; 
            notifyAll();
        }

        /**
         * Waits for a packet to appear in the queue and then returns it.
         * @return the first available packet in the queue
         */
        public synchronized Packet getPacket() {
            Packet packet = null;
            while (!hasPending() && !finished) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
            }
            if (!finished) {
                packet = (Packet)queue.elementAt(0);
                queue.removeElementAt(0);
            }
            return packet;
        }
        /**
         * Returns true if this connection has pending data
         */
        public synchronized boolean hasPending() {
            return queue.size() > 0;
        }
    }

    /**
     * Processes game events occurring on this
     * connection by dispatching them to any registered
     * GameListener objects.
     *
     * @param event the game event.
     */
    protected void processConnectionEvent(ConnectionEvent event) {
        for (Enumeration e = connectionListeners.elements(); e.hasMoreElements();) {
            ConnectionListener l = (ConnectionListener) e.nextElement();
            switch (event.getType()) {
            case ConnectionEvent.CONNECTED:
                l.connected((ConnectedEvent)event);
                break;
            case ConnectionEvent.DISCONNECTED:
                l.disconnected((DisconnectedEvent)event);
                break;
            case ConnectionEvent.PACKET_RECEIVED:
                l.packetReceived((PacketReceivedEvent)event);
                break;                
            }
        }
    }

    /**
     * Connection layer data packet. 
     */
    protected interface INetworkPacket {

        /**
         * Returns data marshalling type
         * @return data marshalling type
         */
        public abstract int getMarshallingType();

        /**
         * Returns packet data
         * @return packet data
         */
        public abstract byte[] getData();

        /**
         * Returns <code>true</code> if data is compressed
         * @return <code>true</code> if data is compressed
         */
        public abstract boolean isCompressed();
    };
}
