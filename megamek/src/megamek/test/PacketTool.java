/*
 * MegaMek
 * Copyright (c) 2003-2004 - Ben Mazur (bmazur@sev.org)
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
package megamek.test;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.common.Board;
import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.factories.ConnectionFactory;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.packets.Packet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class provides an AWT GUI for testing the transmission and reception of
 * <code>Packet</code>s.
 *
 * @author James Damour (suvarov454@users.sourceforge.net)
 */
public class PacketTool extends JFrame implements Runnable {
    /** The currently-loaded <code>Board</code>. May be <code>null</code>. */
    private Board board = null;

    /** The panel containing the connection controls. */
    private Panel panConnect;

    /** The panel containing the transmission controls. */
    private Panel panXmit;

    /** The text control where the host name is entered. */
    private TextField hostName;

    /** The text control where the host port is entered. */
    private TextField hostPort;

    /** The label where the board name is displayed. */
    private Label boardName;

    /** The button that sends the loaded board. */
    private Button butSend;

    /** The connection to the other peer. */
    private AbstractConnection conn = null;

    /**
     * Display a window for testing the transmission of boards.
     */
    public static void main(String... args) {
        MegaMek.initializeLogging("Packet Tool");
        JFrame frame = new PacketTool();

        // set visible on middle of screen
        frame.pack();
        frame.setLocationRelativeTo(null);

        // Show the window.
        frame.setVisible(true);
    }

    /**
     * Create a window for testing the transmission of boards. The window will
     * <em>not</em> be displayed by this constructor; calling functions should
     * call <code>setVisible(true)</code>.
     */
    public PacketTool() {
        super("Board Transmission");

        // Handle the frame stuff.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                quit();
            }
        });

        // Create the panels.
        Panel main = new Panel();
        main.setLayout(new GridLayout(0, 1));
        panConnect = new Panel();
        panConnect.setLayout(new GridLayout(0, 2));
        main.add(panConnect);
        panXmit = new Panel();
        panXmit.setLayout(new GridLayout(0, 1));
        panXmit.setEnabled(false);
        main.add(panXmit);
        this.add(main);

        // Populate the connection panel.
        panConnect.add(new Label("Connect To:"));
        hostName = new TextField(MMConstants.LOCALHOST, 10);
        panConnect.add(hostName);
        panConnect.add(new Label("Port Number:"));
        hostPort = new TextField( String.valueOf(MMConstants.DEFAULT_PORT), 10);
        panConnect.add(hostPort);
        Button button = new Button("Listen");
        button.addActionListener(evt -> (new Thread(this, "Packet Reader")).start());
        panConnect.add(button);
        button = new Button("Connect");
        button.addActionListener(evt -> connect());
        panConnect.add(button);

        // Populate the transmission panel.
        button = new Button("Load Board");
        button.addActionListener(evt -> boardLoad());
        panXmit.add(button);
        boardName = new Label();
        boardName.setAlignment(Label.CENTER);
        panXmit.add(boardName);
        butSend = new Button("Send");
        butSend.addActionListener(evt -> send());
        butSend.setEnabled(false);
        panXmit.add(butSend);
    }

    /**
     * Close this window.
     */
    public synchronized void quit() {
        if (null != conn) {
            conn.close();
        }
        System.exit(0);
    }

    /**
     * Connect to the specified host.
     */
    public void connect() {
        String host = hostName.getText();
        int port;
        try {
            port = Integer.parseInt(hostPort.getText());
            conn = ConnectionFactory.getInstance().createServerConnection(new Socket(host, port), 1);
            Timer t = new Timer(true);
            final Runnable packetUpdate = () -> {
                AbstractConnection connection = conn;
                if (connection != null) {
                    connection.update();
                }
            };

            final TimerTask packetUpdate2 = new TimerTask() {
                @Override
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(packetUpdate);
                    } catch (Exception ignored) {
                        // this should never fail
                    }
                }
            };
            t.schedule(packetUpdate2, 500, 150);

            System.out.println("Connected to peer.");
            conn.addConnectionListener(connectionListener);

            board = new Board();
            panConnect.setEnabled(false);
            panXmit.setEnabled(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Load a board from a file.
     */
    public void boardLoad() {
        FileDialog fd = new FileDialog(this, "Load Board...", FileDialog.LOAD);
        fd.setDirectory("data" + File.separator + "boards");
        if (!boardName.getText().isBlank()) {
            fd.setFile(boardName.getText());
        }
        fd.setLocation(this.getLocation().x + 150, this.getLocation().y + 100);
        fd.setVisible(true);

        if (fd.getFile() == null) {
            return;
        }

        final String currentFile = fd.getFile();

        try (InputStream is = new FileInputStream(new File(fd.getDirectory(), currentFile))) {
            board.load(is);
        } catch (Exception ex) {
            System.err.println("Error opening file to save!");
            ex.printStackTrace();
            return;
        }

        // Record the file's name and enable the send button
        boardName.setText(currentFile);
        butSend.setEnabled(true);
    }

    /**
     * Send the loaded board to the peer.
     */
    public void send() {
        long start = conn.getBytesSent();
        conn.send(new Packet(PacketCommand.SENDING_BOARD, board));
        System.out.print("Bytes sent: ");
        System.out.print(conn.getBytesSent() - start);
        System.out.print(", total sent: ");
        System.out.println(conn.getBytesSent());
    }

    /**
     * Listen for incoming clients.
     */
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(hostPort.getText()));
            Socket s = serverSocket.accept();
            serverSocket.close();

            System.out.println("Accepted peer connection.");

            conn = ConnectionFactory.getInstance().createServerConnection(s, 0);
            conn.addConnectionListener(connectionListener);

            board = new Board();
            panConnect.setEnabled(false);
            panXmit.setEnabled(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Process a packet from a connection. <p> Implements
     * <code>ConnectionHandler</code>.
     *
     * @param id - the <code>int</code> ID the connection that received the
     *            packet.
     * @param packet - the <code>Packet</code> to be processed.
     */
    public synchronized void handle(int id, Packet packet) {
        if (packet == null) {
            System.err.printf("Connection #%s received a null packet from id %n", id);
            return;
        }

        System.out.printf("Connection #%s received a %s packet from id %n", id, packet.getCommand().name());

        if (packet.getCommand().isSendingBoard()) {
            // Save the board here
            Board recvBoard = (Board) packet.getObject(0);
            try (OutputStream os = new FileOutputStream("xmit.board")) {
                recvBoard.save(os);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Called when it is sensed that a connection has terminated. <p>
     * Implements <code>ConnectionHandler</code>.
     *
     * @param deadConn - the <code>Connection</code> that has terminated.
     */
    public synchronized void disconnected(AbstractConnection deadConn) {
        // write something in the log
        System.out.println("s: connection " + deadConn.getId() + " disconnected");

        // kill the connection and remove it from any lists it might be on
        panXmit.setEnabled(false);
        butSend.setEnabled(false);
        boardName.setText("");
        board = null;
        panConnect.setEnabled(true);
    }

    private ConnectionListener connectionListener = new ConnectionListener() {
        /**
         * Called when it is sensed that a connection has terminated.
         */
        @Override
        public void disconnected(DisconnectedEvent e) {
            PacketTool.this.disconnected(e.getConnection());
        }

        @Override
        public void packetReceived(PacketReceivedEvent e) {
            PacketTool.this.handle(e.getConnection().getId(), e.getPacket());
        }
    };
}
