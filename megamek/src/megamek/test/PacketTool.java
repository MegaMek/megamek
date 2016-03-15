/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.test;

import java.awt.Button;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import megamek.common.Board;
import megamek.common.net.ConnectionFactory;
import megamek.common.net.ConnectionListenerAdapter;
import megamek.common.net.DisconnectedEvent;
import megamek.common.net.IConnection;
import megamek.common.net.Packet;
import megamek.common.net.PacketReceivedEvent;

/**
 * This class provides an AWT GUI for testing the transmission and reception of
 * <code>Packet</code>s.
 *
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class PacketTool extends Frame implements Runnable {

    /**
     *
     */
    private static final long serialVersionUID = 3249150110009720658L;

    /**
     * The currently-loaded <code>Board</code>. May be <code>null</code>.
     */
    private Board board = null;

    /**
     * The panel containing the connection controls.
     */
    private Panel panConnect = null;

    /**
     * The panel containing the transimission controls.
     */
    private Panel panXmit = null;

    /**
     * The text control where the host name is entered.
     */
    private TextField hostName = null;

    /**
     * The text control where the host port is entered.
     */
    private TextField hostPort = null;

    /**
     * The label where the board name is displayed.
     */
    private Label boardName = null;

    /**
     * The button that sends the loaded board.
     */
    private Button butSend = null;

    /**
     * The connection to the other peer.
     */
    IConnection conn = null;

    /**
     * Display a window for testing the transmission of boards.
     */
    public static void main(String[] args) {
        Frame frame = new PacketTool();

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
        super("Board Transmition");
        Button button = null;
        Panel main = null;

        // Handle the frame stuff.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        // Create the panels.
        main = new Panel();
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
        panConnect.add(new Label(" Connect To:"));
        hostName = new TextField("localhost", 10);
        panConnect.add(hostName);
        panConnect.add(new Label("Port Number:"));
        hostPort = new TextField("2346", 10);
        panConnect.add(hostPort);
        button = new Button("Listen");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                (new Thread(PacketTool.this, "Packet Reader")).start();
            }
        });
        panConnect.add(button);
        button = new Button("Connect");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });
        panConnect.add(button);

        // Populate the transmission panel.
        button = new Button("Load Board");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boardLoad();
            }
        });
        panXmit.add(button);
        boardName = new Label();
        boardName.setAlignment(Label.CENTER);
        panXmit.add(boardName);
        butSend = new Button("Send");
        butSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });
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
        int port = 0;
        try {
            port = Integer.parseInt(hostPort.getText());
            conn = ConnectionFactory.getInstance().createServerConnection(
                    new Socket(host, port), 1);
            Timer t = new Timer(true);
            final Runnable packetUpdate = new Runnable() {
                public void run() {
                    IConnection connection = conn;
                    if (connection != null) {
                        connection.update();
                    }
                }
            };
            final TimerTask packetUpdate2 = new TimerTask() {
                @Override
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(packetUpdate);
                    } catch (Exception ie) {
                        //this should never fail
                    }
                }
            };
            t.schedule(packetUpdate2, 500, 150);

            // conn = new XmlConnection( this, new Socket(host, port), 1 );
            System.out.println("Connected to peer.");
            conn.addConnectionListener(connectionListener);

            board = new Board();
            panConnect.setEnabled(false);
            panXmit.setEnabled(true);
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }

    /**
     * Load a board from a file.
     */
    public void boardLoad() {
        FileDialog fd = new FileDialog(this, "Load Board...", FileDialog.LOAD);
        fd.setDirectory("data" + File.separator + "boards");
        if (boardName.getText().length() > 0) {
            fd.setFile(boardName.getText());
        }
        fd.setLocation(this.getLocation().x + 150, this.getLocation().y + 100);
        fd.setVisible(true);

        if (fd.getFile() == null) {
            // I want a file, y'know!
            return;
        }
        String curpath = fd.getDirectory();
        String curfile = fd.getFile();
        // load!
        try {
            InputStream is = new FileInputStream(new File(curpath, curfile));
            board.load(is);
            // okay, done!
            is.close();

            // Record the file's name.
            boardName.setText(curfile);

            // Enable the send button.
            butSend.setEnabled(true);
        } catch (IOException ex) {
            System.err.println("error opening file to save!");
            System.err.println(ex);
        }
    }

    /**
     * Send the loaded board to the peer.
     */
    public void send() {
        long start = conn.bytesSent();
        Packet packet = new Packet(Packet.COMMAND_SENDING_BOARD, board);
        /*
         * 2003-12-21 : prove connectivity first, then add refinements * like
         * data compression. packet.zipData();
         */
        conn.send(packet);
        System.out.print("Bytes sent: ");
        System.out.print(conn.bytesSent() - start);
        System.out.print(", total sent: ");
        System.out.println(conn.bytesSent());
    }

    /**
     * Listen for incoming clients.
     */
    public void run() {
        int port = 0;
        try {
            port = Integer.parseInt(hostPort.getText());
            ServerSocket serverSocket = new ServerSocket(port);
            Socket s = serverSocket.accept();
            serverSocket.close();

            System.out.println("Accepted peer connection.");

            conn = ConnectionFactory.getInstance().createServerConnection(s, 0);
            // conn = new XmlConnection(this, s, 0);
            conn.addConnectionListener(connectionListener);

            board = new Board();
            panConnect.setEnabled(false);
            panXmit.setEnabled(true);

        } catch (Throwable err) {
            err.printStackTrace();
        }
    }

    /**
     * Process a packet from a connection. <p/> Implements
     * <code>ConnectionHandler</code>.
     *
     * @param id - the <code>int</code> ID the connection that received the
     *            packet.
     * @param packet - the <code>Packet</code> to be processed.
     */
    public synchronized void handle(int id, Packet packet) {
        System.out.print("Connection #");
        System.out.print(id);
        System.out.print(", received a ");
        if (null == packet) {
            System.out.print("null");
        } else {
            switch (packet.getCommand()) {
                case Packet.COMMAND_SERVER_GREETING:
                    System.out.print("COMMAND_SERVER_GREETING");
                    break;
                case Packet.COMMAND_CLIENT_NAME:
                    System.out.print("COMMAND_CLIENT_NAME");
                    break;
                case Packet.COMMAND_LOCAL_PN:
                    System.out.print("COMMAND_LOCAL_PN");
                    break;
                case Packet.COMMAND_PLAYER_ADD:
                    System.out.print("COMMAND_PLAYER_ADD");
                    break;
                case Packet.COMMAND_PLAYER_REMOVE:
                    System.out.print("COMMAND_PLAYER_REMOVE");
                    break;
                case Packet.COMMAND_PLAYER_UPDATE:
                    System.out.print("COMMAND_PLAYER_UPDATE");
                    break;
                case Packet.COMMAND_PLAYER_READY:
                    System.out.print("COMMAND_PLAYER_READY");
                    break;
                case Packet.COMMAND_CHAT:
                    System.out.print("COMMAND_CHAT");
                    break;
                case Packet.COMMAND_ENTITY_ADD:
                    System.out.print("COMMAND_ENTITY_ADD");
                    break;
                case Packet.COMMAND_ENTITY_REMOVE:
                    System.out.print("COMMAND_ENTITY_REMOVE");
                    break;
                case Packet.COMMAND_ENTITY_MOVE:
                    System.out.print("COMMAND_ENTITY_MOVE");
                    break;
                case Packet.COMMAND_ENTITY_DEPLOY:
                    System.out.print("COMMAND_ENTITY_DEPLOY");
                    break;
                case Packet.COMMAND_ENTITY_ATTACK:
                    System.out.print("COMMAND_ENTITY_ATTACK");
                    break;
                case Packet.COMMAND_ENTITY_UPDATE:
                    System.out.print("COMMAND_ENTITY_UPDATE");
                    break;
                case Packet.COMMAND_ENTITY_MODECHANGE:
                    System.out.print("COMMAND_ENTITY_MODECHANGE");
                    break;
                case Packet.COMMAND_ENTITY_MOUNTED_FACINGCHANGE:
                    System.out.print("COMMAND_ENTITY_MOUNTED_FACINGCHANGE");
                    break;
                case Packet.COMMAND_ENTITY_AMMOCHANGE:
                    System.out.print("COMMAND_ENTITY_AMMOCHANGE");
                    break;
                case Packet.COMMAND_ENTITY_VISIBILITY_INDICATOR:
                    System.out.print("COMMAND_ENTITY_VISIBILITY_INDICATOR");
                    break;
                case Packet.COMMAND_CHANGE_HEX:
                    System.out.print("COMMAND_CHANGE_HEX");
                    break;
                case Packet.COMMAND_BLDG_ADD:
                    System.out.print("COMMAND_BLDG_ADD");
                    break;
                case Packet.COMMAND_BLDG_REMOVE:
                    System.out.print("COMMAND_BLDG_REMOVE");
                    break;
                case Packet.COMMAND_BLDG_UPDATE:
                    System.out.print("COMMAND_BLDG_UPDATE_CF");
                    break;
                case Packet.COMMAND_BLDG_COLLAPSE:
                    System.out.print("COMMAND_BLDG_COLLAPSE");
                    break;
                case Packet.COMMAND_PHASE_CHANGE:
                    System.out.print("COMMAND_PHASE_CHANGE");
                    break;
                case Packet.COMMAND_TURN:
                    System.out.print("COMMAND_TURN");
                    break;
                case Packet.COMMAND_ROUND_UPDATE:
                    System.out.print("COMMAND_ROUND_UPDATE");
                    break;
                case Packet.COMMAND_SENDING_BOARD:
                    System.out.print("COMMAND_SENDING_BOARD");
                    /*
                     * * Save the board here.
                     */
                    Board recvBoard = (Board) packet.getObject(0);
                    try(OutputStream os = new FileOutputStream("xmit.board")) { //$NON-NLS-1$
                        recvBoard.save(os);
                    } catch (IOException ioErr) {
                        ioErr.printStackTrace();
                    }
                    break;
                case Packet.COMMAND_SENDING_ENTITIES:
                    System.out.print("COMMAND_SENDING_ENTITIES");
                    break;
                case Packet.COMMAND_SENDING_PLAYERS:
                    System.out.print("COMMAND_SENDING_PLAYERS");
                    break;
                case Packet.COMMAND_SENDING_TURNS:
                    System.out.print("COMMAND_SENDING_TURNS");
                    break;
                case Packet.COMMAND_SENDING_REPORTS:
                    System.out.print("COMMAND_SENDING_REPORTS");
                    break;
                case Packet.COMMAND_SENDING_GAME_SETTINGS:
                    System.out.print("COMMAND_SENDING_GAME_SETTINGS");
                    break;
                case Packet.COMMAND_SENDING_MAP_SETTINGS:
                    System.out.print("COMMAND_SENDING_MAP_SETTINGS");
                    break;
                case Packet.COMMAND_SENDING_MAP_DIMENSIONS:
                    System.out.print("COMMAND_SENDING_MAP_SETTINGS");
                    break;
                case Packet.COMMAND_END_OF_GAME:
                    System.out.print("COMMAND_END_OF_GAME");
                    break;
                case Packet.COMMAND_DEPLOY_MINEFIELDS:
                    System.out.print("COMMAND_DEPLOY_MINEFIELDS");
                    break;
                case Packet.COMMAND_REVEAL_MINEFIELD:
                    System.out.print("COMMAND_REVEAL_MINEFIELD");
                    break;
                case Packet.COMMAND_REMOVE_MINEFIELD:
                    System.out.print("COMMAND_REMOVE_MINEFIELD");
                    break;
                case Packet.COMMAND_SENDING_MINEFIELDS:
                    System.out.print("COMMAND_SENDING_MINEFIELDS");
                    break;
                case Packet.COMMAND_REROLL_INITIATIVE:
                    System.out.print("COMMAND_REROLL_INITIATIVE");
                    break;
                case Packet.COMMAND_SET_ARTYAUTOHITHEXES:
                    System.out.print("COMMAND_SET_ARTYAUTOHITHEXES");
                    break;
                default:
                    System.out.print("unknown");
                    break;
            }
        }
        System.out.println(" packet.");
    }

    /**
     * Called when it is sensed that a connection has terminated. <p/>
     * Implements <code>ConnectionHandler</code>.
     *
     * @param deadConn - the <code>Connection</code> that has terminated.
     */
    public synchronized void disconnected(IConnection deadConn) {
        // write something in the log
        System.out.println("s: connection " + deadConn.getId() + " disconnected");

        // kill the connection and remove it from any lists it might be on
        panXmit.setEnabled(false);
        butSend.setEnabled(false);
        boardName.setText("");
        board = null;
        deadConn = null;
        panConnect.setEnabled(true);
    }

    private ConnectionListenerAdapter connectionListener = new ConnectionListenerAdapter() {

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
