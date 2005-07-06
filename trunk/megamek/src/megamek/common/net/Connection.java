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

import megamek.common.*;
import megamek.common.util.CircularIntegerBuffer;

/**
 * Generic bi-directional connection between client and server
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
    
    /**
     * Peer Host 
     */
    private String host;
    
    /**
     * Peer port
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
        
    private long bytesSent;
    
    private SendQueue sendQueue = new SendQueue(); 

    private Vector connectionListeners = new Vector();

    private CircularIntegerBuffer debugLastFewCommandsSent =
        new CircularIntegerBuffer(5);
    
    /**
     * Creates new connection
     */
    public Connection(String host, int port, int id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }
    
    /**
     * Creates new connection
     * @param socket
     * @param id
     */
    public Connection(Socket socket, int id) {
        this.socket = socket;
        this.id = id;
    }

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
     * Closes the socket and shuts down the receiver snd sender threads
     */
    public void close() {
        synchronized (this) {
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
     * Initializes the sender and receiver threads
     */
    private void initThreads() {

        Runnable receiverRunnable = new Runnable() {
            public void run() {
                while (receiver == Thread.currentThread()) {
                    Packet packet;
                    try {
                        packet = readPacket();
                        processConnectionEvent(new PacketReceivedEvent(Connection.this, packet));
                    } catch (Exception e) {
                        reportReceiveException(e);
                        close();
                    }                   
                }
            }
        };        
        receiver = new Thread(receiverRunnable, "Packet Receiver (" + getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

        Runnable senderRunable = new Runnable() {
            public void run() {
                while (sender == Thread.currentThread()) {
                    Packet packet = sendQueue.getPacket(); 
                    debugLastFewCommandsSent.push(packet.getCommand());
                    try {
                        bytesSent += sendPacket(packet);
                    } catch (Exception e) {
                        reportSendException(e, packet);
                        close();
                    }
                }
            }
        };        
        sender = new Thread(senderRunable, "Packet Sender (" + getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        
        receiver.start();        
        sender.start();
    }

    protected void reportSendException(Exception ex, Packet packet) {
        System.err.println("error sending command #" + packet.getCommand() + ": " + ex.getMessage()); //$NON-NLS-1$
        System.err.println("    Last five commands that were sent (oldest first): " + debugLastFewCommandsSent.print());      
    }

    protected void reportReceiveException(Exception ex) {
        System.err.println("error reading command: " + ex.getMessage()); //$NON-NLS-1$
    }

    /**
     * Returns the connection id
     * @return the connection id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the connection id
     * Be careful with this...
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the socket used to read/write
     * @return the socket used to read/write
     */
    public Socket getSocket() {
        return socket;
    }
    
    /**
     * Adds a packet to the send queue to be send on a seperate thread.
     */
    public void send(Packet packet) {
        sendQueue.addPacket(packet);
    }

    public boolean hasPending() {
        return sendQueue.hasPending();
    }
    
    /**
     * Reads a complete net command from the given socket
     * @return  the <code>Packet</code> that was sent.
     */
    protected abstract Packet readPacket() throws Exception;

    /**
     * Sends a packet.
     *
     * @param   packet - the <code>Packet</code> to be sent.
     * @return  the <code>int</code> number of bytes sent.
     */
    protected abstract int sendPacket(Packet packet) throws Exception;
    
    /**
     * Returns a very approximate count of how many bytes were sent to this
     * player.
     */
    public synchronized long bytesSent() {
        return bytesSent;
    }

    private static class SendQueue {

        private Vector queue = new Vector();

        public synchronized void addPacket(Packet packet) {
            queue.addElement(packet);
            notifyAll();
        }

        /**
         * Waits for a packet to appear in the queue and then returns it.
         * @return the first available packet in the queue
         */
        public synchronized Packet getPacket() {
            while (!hasPending()) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                }
            }
            Packet packet = (Packet)queue.elementAt(0);
            queue.removeElementAt(0);
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

}
