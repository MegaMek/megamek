/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.server;

import com.thoughtworks.xstream.XStream;
import megamek.MMConstants;
import megamek.MegaMek;
import megamek.Version;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.commandline.AbstractCommandLineParser.ParseException;
import megamek.common.icons.Camouflage;
import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.factories.ConnectionFactory;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.packets.Packet;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.EmailService;
import megamek.common.util.SerializationHelper;
import megamek.server.commands.ServerCommand;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPInputStream;

/**
 * @author Ben Mazur
 */
public class Server implements Runnable {
    // server setup
    private String password;

    private final IGameManager gameManager;

    private final String metaServerUrl;

    private ServerSocket serverSocket;

    private String motd;

    private EmailService mailer;

    public static class ReceivedPacket {
        private int connectionId;
        private Packet packet;

        public ReceivedPacket(int cid, Packet p) {
            setPacket(p);
            setConnectionId(cid);
        }

        public int getConnectionId() {
            return connectionId;
        }

        public void setConnectionId(int connectionId) {
            this.connectionId = connectionId;
        }

        public Packet getPacket() {
            return packet;
        }

        public void setPacket(Packet packet) {
            this.packet = packet;
        }
    }

    private class PacketPump implements Runnable {
        boolean shouldStop;

        PacketPump() {
            shouldStop = false;
        }

        void signalEnd() {
            shouldStop = true;
        }

        @Override
        public void run() {
            while (!shouldStop) {
                while (!packetQueue.isEmpty()) {
                    ReceivedPacket rp = packetQueue.poll();
                    synchronized (serverLock) {
                        handle(rp.getConnectionId(), rp.getPacket());
                    }
                }

                try {
                    synchronized (packetQueue) {
                        packetQueue.wait();
                    }
                } catch (InterruptedException ignored) {
                    // If we are interrupted, just keep going, generally
                    // this happens after we are signalled to stop.
                }
            }
        }
    }

    // commands
    private Hashtable<String, ServerCommand> commandsHash = new Hashtable<>();

    // game info
    private final Vector<AbstractConnection> connections = new Vector<>(4);

    private Hashtable<Integer, ConnectionHandler> connectionHandlers = new Hashtable<>();

    private final ConcurrentLinkedQueue<ReceivedPacket> packetQueue = new ConcurrentLinkedQueue<>();

    private final boolean dedicated;

    private Vector<AbstractConnection> connectionsPending = new Vector<>(4);

    private Hashtable<Integer, AbstractConnection> connectionIds = new Hashtable<>();

    private int connectionCounter;

    // listens for and connects players
    private Thread connector;

    private PacketPump packetPump;
    private Thread packetPumpThread;

    private Timer watchdogTimer = new Timer("Watchdog Timer");

    private static Server serverInstance = null;

    private String serverAccessKey = null;

    private Timer serverBrowserUpdateTimer = null;

    /**
     * Used to ensure only one thread at a time is accessing this particular
     * instance of the server.
     */
    private final Object serverLock = new Object();

    public static final String ORIGIN = "***Server";

    // Easter eggs. Happy April Fool's Day!!
    private static final String DUNE_CALL = "They tried and failed?";

    private static final String DUNE_RESPONSE = "They tried and died!";

    private static final String STAR_WARS_CALL = "I'd just as soon kiss a Wookiee.";

    private static final String STAR_WARS_RESPONSE = "I can arrange that!";

    private static final String INVADER_ZIM_CALL = "What does the G stand for?";

    private static final String INVADER_ZIM_RESPONSE = "I don't know.";

    private static final String WARGAMES_CALL = "Shall we play a game?";

    private static final String WARGAMES_RESPONSE = "Let's play global thermonuclear war.";

    private ConnectionListener connectionListener = new ConnectionListener() {
        /**
         * Called when it is sensed that a connection has terminated.
         */
        @Override
        public void disconnected(DisconnectedEvent e) {
            synchronized (serverLock) {
                AbstractConnection conn = e.getConnection();

                // write something in the log
                LogManager.getLogger().info("s: connection " + conn.getId() + " disconnected");

                connections.removeElement(conn);
                connectionsPending.removeElement(conn);
                connectionIds.remove(conn.getId());
                ConnectionHandler ch = connectionHandlers.get(conn.getId());
                if (ch != null) {
                    ch.signalStop();
                    connectionHandlers.remove(conn.getId());
                }

                // if there's a player for this connection, remove it too
                Player player = getPlayer(conn.getId());
                if (null != player) {
                    Server.this.disconnected(player);
                }
            }
        }

        @Override
        public void packetReceived(PacketReceivedEvent e) {
            ReceivedPacket rp = new ReceivedPacket(e.getConnection().getId(), e.getPacket());
            switch (e.getPacket().getCommand()) {
                case CLIENT_FEEDBACK_REQUEST:
                    // Handled CFR packets specially
                    gameManager.handleCfrPacket(rp);
                    break;
                case CLOSE_CONNECTION:
                case CLIENT_NAME:
                case CLIENT_VERSIONS:
                case CHAT:
                    // Some packets should be handled immediately
                    handle(rp.getConnectionId(), rp.getPacket());
                    break;
                default:
                    synchronized (packetQueue) {
                        packetQueue.add(rp);
                        packetQueue.notifyAll();
                    }
                    break;
            }
        }
    };

    /**
     * @param serverAddress
     * @return valid hostName
     * @throws ParseException for null or empty serverAddress
     */
    public static String validateServerAddress(String serverAddress) throws ParseException {
        if ((serverAddress == null) || serverAddress.isBlank()) {
            String msg = "serverAddress must not be null or empty";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else {
            return serverAddress.trim();
        }
    }

    /**
     * @param playerName throw ParseException if null or empty
     * @return valid playerName
     */
    public static String validatePlayerName(String playerName) throws ParseException {
        if (playerName == null) {
            String msg = "playerName must not be null";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else if (playerName.isBlank()) {
            String msg = "playerName must not be empty string";
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else {
            return playerName.trim();
        }
    }

    /**
     * @param password
     * @return valid password or null if no password or password is blank string
     */
    public static @Nullable String validatePassword(@Nullable String password) {
        return ((password == null) || password.isBlank()) ? null : password.trim();
    }

    /**
     * Checks a String against the server password
     *
     * @param password The password provided by the user.
     * @return true if the user-supplied data matches the server password or no password is set.
     */
    public boolean passwordMatches(Object password) {
        return (this.password == null) || this.password.isBlank() || this.password.equals(password);
    }

    /**
     * @param port if 0 or less, will return default, if illegal number, throws ParseException
     * @return valid port number
     */
    public static int validatePort(int port) throws ParseException {
        if (port <= 0) {
            return MMConstants.DEFAULT_PORT;
        } else if ((port < MMConstants.MIN_PORT) || (port > MMConstants.MAX_PORT)) {
            String msg = String.format("Port number %d outside allowed range %d-%d", port, MMConstants.MIN_PORT, MMConstants.MAX_PORT);
            LogManager.getLogger().error(msg);
            throw new ParseException(msg);
        } else {
            return port;
        }
    }

    public Server(String password, int port, IGameManager gameManager) throws IOException {
        this(password, port, gameManager, false, "", null, false);
    }

    public Server(String password, int port, IGameManager gameManager,
                  boolean registerWithServerBrowser, String metaServerUrl) throws IOException {
        this(password, port, gameManager, registerWithServerBrowser, metaServerUrl, null, false);
    }

    public Server(String password, int port, IGameManager gameManager,
                  boolean registerWithServerBrowser, String metaServerUrl, EmailService mailer)
            throws IOException {
        this(password, port, gameManager, registerWithServerBrowser, metaServerUrl, mailer, false);
    }

    /**
     * Construct a new GameHost and begin listening for incoming clients.
     *
     * @param password                  the <code>String</code> that is set as a password
     * @param port                      the <code>int</code> value that specifies the port that is
     *                                  used
     * @param gameManager               the {@link IGameManager} instance for this server instance.
     * @param registerWithServerBrowser a <code>boolean</code> indicating whether we should register
     *                                  with the master server browser on MegaMek.info
     * @param mailer                    an email service instance to use for sending round reports.
     * @param dedicated                 set to true if this server is started from a GUI-less context
     */
    public Server(@Nullable String password, int port, IGameManager gameManager,
                  boolean registerWithServerBrowser, @Nullable String metaServerUrl,
                  @Nullable EmailService mailer, boolean dedicated) throws IOException {
        this.metaServerUrl = (metaServerUrl != null) && (!metaServerUrl.isBlank()) ? metaServerUrl : null;
        this.password = (password != null) && (!password.isBlank()) ? password : null;
        this.gameManager = gameManager;

        this.mailer = mailer;
        this.dedicated = dedicated;

        // initialize server socket
        serverSocket = new ServerSocket(port);

        motd = createMotd();

        // display server start text
        LogManager.getLogger().info("s: starting a new server...");

        try {
            StringBuilder sb = new StringBuilder();
            String host = InetAddress.getLocalHost().getHostName();
            sb.append("s: hostname = '");
            sb.append(host);
            sb.append("' port = ");
            sb.append(serverSocket.getLocalPort());
            sb.append('\n');
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                sb.append("s: hosting on address = ");
                sb.append(address.getHostAddress());
                sb.append('\n');
            }

            LogManager.getLogger().info(sb.toString());
        } catch (Exception ignored) {

        }

        LogManager.getLogger().info("s: password = " + this.password);

        for (ServerCommand command : gameManager.getCommandList(this)) {
            registerCommand(command);
        }

        packetPump = new PacketPump();
        packetPumpThread = new Thread(packetPump, "Packet Pump");
        packetPumpThread.start();

        if (registerWithServerBrowser) {
            if ((metaServerUrl != null) && (!metaServerUrl.isBlank())) {
                final TimerTask register = new TimerTask() {
                    @Override
                    public void run() {
                        registerWithServerBrowser(true, Server.getServerInstance().metaServerUrl);
                    }
                };
                serverBrowserUpdateTimer = new Timer("Server Browser Register Timer", true);
                serverBrowserUpdateTimer.schedule(register, 1, 40000);
            } else {
                LogManager.getLogger().error("Invalid URL for server browser " + this.metaServerUrl);
            }
        }

        // Fully initialised, now accept connections
        connector = new Thread(this, "Connection Listener");
        connector.start();

        serverInstance = this;
    }

    public IGameManager getGameManager() {
        return gameManager;
    }

    /**
     * Sets the game for this server. Restores any transient fields, and sets
     * all players as ghosts. This should only be called during server
     * initialization before any players have connected.
     */
    public void setGame(IGame g) {
        gameManager.setGame(g);
    }

    public IGame getGame() {
        return gameManager.getGame();
    }

    public EmailService getEmailService() {
        return mailer;
    }

    /**
     * Make a default message o' the day containing the version string, and if
     * it was found, the build timestamp
     */
    private String createMotd() {
        return "Welcome to MegaMek. Server is running version " + MMConstants.VERSION;
    }

    /**
     * @return true if the server has a password
     */
    public boolean isPassworded() {
        return password != null;
    }

    /**
     * @return true if the password matches
     */
    public boolean isPassword(Object guess) {
        return password.equals(guess);
    }

    /**
     * Registers a new command in the server command table
     */
    private void registerCommand(ServerCommand command) {
        commandsHash.put(command.getName(), command);
    }

    /**
     * Returns the command associated with the specified name
     */
    public ServerCommand getCommand(String name) {
        return commandsHash.get(name);
    }

    /**
     * @return true run from a GUI-less context
     */
    public boolean getDedicated() {
        return dedicated;
    }

    /**
     * Shuts down the server.
     */
    public void die() {
        watchdogTimer.cancel();

        // kill thread accepting new connections
        connector = null;
        packetPump.signalEnd();
        packetPumpThread.interrupt();
        packetPumpThread = null;

        // close socket
        try {
            serverSocket.close();
        } catch (Exception ignored) {

        }

        // kill pending connections
        for (Enumeration<AbstractConnection> connEnum = connectionsPending.elements(); connEnum.hasMoreElements(); ) {
            AbstractConnection conn = connEnum.nextElement();
            conn.close();
        }
        connectionsPending.removeAllElements();

        // Send "kill" commands to all connections
        // N.B. I may be starting a race here.
        send(new Packet(PacketCommand.CLOSE_CONNECTION));

        // kill active connections
        synchronized (connections) {
            connections.forEach(AbstractConnection::close);
            connections.clear();
        }

        connectionIds.clear();

        // Shutdown Email
        if (mailer != null) {
            mailer.shutdown();
        }

        // Unregister Server Browser Setup
        if (serverBrowserUpdateTimer != null) {
            serverBrowserUpdateTimer.cancel();
        }

        if ((metaServerUrl != null) && (!metaServerUrl.isBlank())) {
            registerWithServerBrowser(false, metaServerUrl);
        }
    }

    /**
     * Returns an enumeration of all the command names
     */
    public Enumeration<String> getAllCommandNames() {
        return commandsHash.keys();
    }

    /**
     * Sent when a client attempts to connect.
     */
    void clientVersionCheck(int cn) {
        sendToPending(cn, new Packet(PacketCommand.SERVER_VERSION_CHECK));
    }

    /**
     * Returns a free connection id.
     */
    public int getFreeConnectionId() {
        while ((getPendingConnection(connectionCounter) != null)
                || (getConnection(connectionCounter) != null)
                || (getPlayer(connectionCounter) != null)) {
            connectionCounter++;
        }
        return connectionCounter;
    }

    /**
     * Returns a free entity id. Perhaps this should be in Game instead.
     */
    public int getFreeEntityId() {
        return getGame().getNextEntityId();
    }

    /**
     * Allow the player to set whatever parameters he is able to
     */
    private void receivePlayerInfo(Packet packet, int connId) {
        Player player = (Player) packet.getObject(0);
        Player gamePlayer = getGame().getPlayer(connId);
        if (null != gamePlayer) {
            gamePlayer.setColour(player.getColour());
            gamePlayer.setStartingPos(player.getStartingPos());
            gamePlayer.setTeam(player.getTeam());
            gamePlayer.setCamouflage(player.getCamouflage().clone());
            gamePlayer.setNbrMFConventional(player.getNbrMFConventional());
            gamePlayer.setNbrMFCommand(player.getNbrMFCommand());
            gamePlayer.setNbrMFVibra(player.getNbrMFVibra());
            gamePlayer.setNbrMFActive(player.getNbrMFActive());
            gamePlayer.setNbrMFInferno(player.getNbrMFInferno());
            if (gamePlayer.getConstantInitBonus() != player.getConstantInitBonus()) {
                sendServerChat("Player " + gamePlayer.getName()
                        + " changed their initiative bonus from "
                        + gamePlayer.getConstantInitBonus() + " to "
                        + player.getConstantInitBonus() + '.');
            }
            gamePlayer.setConstantInitBonus(player.getConstantInitBonus());
            gamePlayer.setEmail(player.getEmail());
        }
    }

    /**
     * Correct a duplicate player name
     *
     * @param oldName the <code>String</code> old player name, that is a duplicate
     * @return the <code>String</code> new player name
     */
    private String correctDupeName(String oldName) {
        for (Enumeration<Player> i = getGame().getPlayers(); i.hasMoreElements(); ) {
            Player player = i.nextElement();
            if (player.getName().equals(oldName)) {
                // We need to correct it.
                String newName = oldName;
                int dupNum;
                try {
                    dupNum = Integer.parseInt(oldName.substring(oldName.lastIndexOf('.') + 1));
                    dupNum++;
                    newName = oldName.substring(0, oldName.lastIndexOf('.'));
                } catch (Exception e) {
                    // If this fails, we don't care much.
                    // Just assume it's the first time for this name.
                    dupNum = 2;
                }
                newName = newName.concat(".").concat(Integer.toString(dupNum));
                return correctDupeName(newName);
            }
        }
        return oldName;
    }

    private boolean receivePlayerVersion(Packet packet, int connId) {
        final Version version = (Version) packet.getObject(0);
        if (!MMConstants.VERSION.is(version)) {
            final String message = String.format("Client/Server Version Mismatch -- Client: %s, Server: %s",
                    version, MMConstants.VERSION);
            LogManager.getLogger().error(message);

            final Player player = getPlayer(connId);
            sendServerChat(String.format("For %s, Server reports:%s%s",
                    ((player == null) ? "unknown player" : player.getName()), System.lineSeparator(),
                    message));
            return false;
        }

        final String clientChecksum = (String) packet.getObject(1);
        final String serverChecksum = MegaMek.getMegaMekSHA256();
        final String message;

        // print a message indicating client doesn't have jar file
        if (clientChecksum == null) {
            message = "Client Checksum is null. Client may not have a jar file";
            LogManager.getLogger().info(message);
            // print message indicating server doesn't have jar file
        } else if (serverChecksum == null) {
            message = "Server Checksum is null. Server may not have a jar file";
            LogManager.getLogger().info(message);
            // print message indicating a client/server checksum mismatch
        } else if (!clientChecksum.equals(serverChecksum)) {
            message = String.format("Client/Server checksum mismatch. Server reports: %s, Client reports %s",
                    serverChecksum, clientChecksum);
            LogManager.getLogger().warn(message);
        } else {
            message = "";
        }

        // Now, if we need to, send message!
        if (message.isEmpty()) {
            LogManager.getLogger().info("SUCCESS: Client/Server Version (" + version + ") and Checksum ("
                    + clientChecksum + ") matched");
        } else {
            Player player = getPlayer(connId);
            sendServerChat(String.format("For %s, Server reports:%s%s",
                    ((player == null) ? "unknown player" : player.getName()), System.lineSeparator(),
                    message));
        }

        return true;
    }

    /**
     * Receives a player name, sent from a pending connection, and connects that
     * connection.
     */
    private void receivePlayerName(Packet packet, int connId) {
        final AbstractConnection conn = getPendingConnection(connId);
        String name = (String) packet.getObject(0);
        boolean isBot = (boolean) packet.getObject(1);
        boolean returning = false;

        // this had better be from a pending connection
        if (conn == null) {
            LogManager.getLogger().warn("Got a client name from a non-pending connection");
            return;
        }

        // check if they're connecting with the same name as a ghost player
        for (Enumeration<Player> i = getGame().getPlayers(); i.hasMoreElements(); ) {
            Player player = i.nextElement();
            if (player.getName().equals(name)) {
                if (player.isGhost()) {
                    returning = true;
                    player.setGhost(false);
                    // switch id
                    connId = player.getId();
                    conn.setId(connId);
                }
            }
        }

        if (!returning) {
            // Check to avoid duplicate names...
            sendToPending(connId, new Packet(PacketCommand.SERVER_CORRECT_NAME, correctDupeName(name)));
        }

        // right, switch the connection into the "active" bin
        connectionsPending.removeElement(conn);
        connections.addElement(conn);
        connectionIds.put(conn.getId(), conn);

        // add and validate the player info
        if (!returning) {
            addNewPlayer(connId, name, isBot);
        }

        // if it is not the lounge phase, this player becomes an observer
        Player player = getPlayer(connId);
        if (!getGame().getPhase().isLounge() && (null != player)
                && (getGame().getEntitiesOwnedBy(player) < 1)) {
            player.setObserver(true);
        }

        // send the player the motd
        sendServerChat(connId, motd);

        // send info that the player has connected
        transmitPlayerConnect(player);

        // tell them their local playerId
        send(connId, new Packet(PacketCommand.LOCAL_PN, connId));

        // send current game info
        sendCurrentInfo(connId);

        final boolean showIPAddressesInChat = PreferenceManager.getClientPreferences().getShowIPAddressesInChat();

        try {
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress address : addresses) {
                LogManager.getLogger().info("s: machine IP " + address.getHostAddress());
                if (showIPAddressesInChat) {
                    sendServerChat(connId, "Machine IP is " + address.getHostAddress());
                }
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().info("s: listening on port " + serverSocket.getLocalPort());
        if (showIPAddressesInChat) {
            // Send the port we're listening on. Only useful for the player
            // on the server machine to check.
            sendServerChat(connId, "Listening on port " + serverSocket.getLocalPort());
        }

        // Get the player *again*, because they may have disconnected.
        player = getPlayer(connId);
        if (null != player) {
            String who = player.getName() + " connected from " + getClient(connId).getInetAddress();
            LogManager.getLogger().info("s: player #" + connId + ", " + who);
            if (showIPAddressesInChat) {
                sendServerChat(who);
            }
        } // Found the player
    }

    /**
     * Sends a player the info they need to look at the current phase. This is
     * triggered when a player first connects to the server.
     */
    public void sendCurrentInfo(int connId) {
        transmitPlayerConnect(getClient(connId));
        gameManager.sendCurrentInfo(connId);
    }

    /**
     * Adds a new player to the game
     */
    private Player addNewPlayer(int connId, String name, boolean isBot) {
        int team = Player.TEAM_UNASSIGNED;
        if (getGame().getPhase().isLounge()) {
            team = Player.TEAM_NONE;
            for (Player p : getGame().getPlayersVector()) {
                if (p.getTeam() > team) {
                    team = p.getTeam();
                }
            }
            team++;
        }
        Player newPlayer = new Player(connId, name);
        newPlayer.setBot(isBot);
        PlayerColour colour = newPlayer.getColour();
        Enumeration<Player> players = getGame().getPlayers();
        final PlayerColour[] colours = PlayerColour.values();
        while (players.hasMoreElements()) {
            final Player p = players.nextElement();
            if (p.getId() == newPlayer.getId()) {
                continue;
            }

            if ((p.getColour() == colour) && (colours.length > (colour.ordinal() + 1))) {
                colour = colours[colour.ordinal() + 1];
            }
        }
        newPlayer.setColour(colour);
        newPlayer.setCamouflage(new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, colour.name()));
        newPlayer.setTeam(Math.min(team, 5));
        getGame().addPlayer(connId, newPlayer);
        validatePlayerInfo(connId);
        return newPlayer;
    }

    /**
     * Validates the player info.
     */
    public void validatePlayerInfo(int playerId) {
        final Player player = getPlayer(playerId);

        if (player != null) {
            // TODO : check for duplicate or reserved names

            // Colour Assignment
            final PlayerColour[] playerColours = PlayerColour.values();
            boolean allUsed = true;
            Set<PlayerColour> colourUtilization = new HashSet<>();
            for (Enumeration<Player> i = getGame().getPlayers(); i.hasMoreElements(); ) {
                final Player otherPlayer = i.nextElement();
                if (otherPlayer.getId() != playerId) {
                    colourUtilization.add(otherPlayer.getColour());
                } else {
                    allUsed = false;
                }
            }

            if (!allUsed && colourUtilization.contains(player.getColour())) {
                for (PlayerColour colour : playerColours) {
                    if (!colourUtilization.contains(colour)) {
                        player.setColour(colour);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Called when it's been determined that an actual player disconnected.
     * Notifies the other players and does the appropriate housekeeping.
     */
    void disconnected(Player player) {
        gameManager.disconnect(player);
    }

    public void resetGame() {
        gameManager.resetGame();
    }

    /**
     * send a packet to the connection tells it load a locally saved game
     *
     * @param connId The <code>int</code> connection id to send to
     * @param sFile  The <code>String</code> filename to use
     */
    public void sendLoadGame(int connId, String sFile) {
        String sFinalFile = sFile;
        if (!sFinalFile.endsWith(MMConstants.SAVE_FILE_EXT) && !sFinalFile.endsWith(MMConstants.SAVE_FILE_GZ_EXT)) {
            sFinalFile = sFile + MMConstants.SAVE_FILE_EXT;
        }
        if (!sFinalFile.endsWith(".gz")) {
            sFinalFile = sFinalFile + ".gz";
        }
        send(connId, new Packet(PacketCommand.LOAD_SAVEGAME, sFinalFile));
    }

    /**
     * load the game
     *
     * @param f The <code>File</code> to load
     * @return A <code>boolean</code> value whether or not the loading was successful
     */
    public boolean loadGame(File f) {
        return loadGame(f, true);
    }

    /**
     * load the game
     *
     * @param f        The <code>File</code> to load
     * @param sendInfo Determines whether the connections should be updated with
     *                 current info. This may be false if some reconnection remapping
     *                 needs to be done first.
     * @return A <code>boolean</code> value whether or not the loading was successful
     */
    public boolean loadGame(File f, boolean sendInfo) {
        LogManager.getLogger().info("s: loading saved game file '" + f + '\'');

        Game newGame;
        try (InputStream is = new FileInputStream(f); InputStream gzi = new GZIPInputStream(is)) {
            XStream xstream = SerializationHelper.getXStream();
            newGame = (Game) xstream.fromXML(gzi);
        } catch (Exception e) {
            LogManager.getLogger().error("Unable to load file: " + f, e);
            return false;
        }

        setGame(newGame);

        if (!sendInfo) {
            return true;
        }

        // update all the clients with the new game info
        for (AbstractConnection conn : connections) {
            sendCurrentInfo(conn.getId());
        }
        return true;
    }

    public void saveGame(String fileName) {
        gameManager.saveGame(fileName);
    }

    public void sendSaveGame(int connId, String fileName, String localPath) {
        gameManager.sendSaveGame(connId, fileName, localPath);
    }

    /**
     * When the load command is used, there is a list of already connected
     * players which have assigned names and player id numbers with the id
     * numbers matching the connection numbers. When a new game is loaded, this
     * mapping may need to be updated. This method takes a map of player names
     * to their current ids, and uses the list of players to figure out what the
     * current ids should change to.
     *
     * @param nameToIdMap This maps a player name to the current connection ID
     * @param idToNameMap This maps a current conn ID to a player name, and is just the
     *                    inverse mapping from nameToIdMap
     */
    public void remapConnIds(Map<String, Integer> nameToIdMap, Map<Integer, String> idToNameMap) {
        // Keeps track of connections without Ids
        List<AbstractConnection> unassignedConns = new ArrayList<>();
        // Keep track of which ids are used
        Set<Integer> usedPlayerIds = new HashSet<>();
        Set<String> currentPlayerNames = new HashSet<>();
        for (Player p : getGame().getPlayersVector()) {
            currentPlayerNames.add(p.getName());
        }
        // Map the old connection Id to new value
        Map<Integer, Integer> connIdRemapping = new HashMap<>();
        for (Player p : getGame().getPlayersVector()) {
            // Check to see if this player was already connected
            Integer oldId = nameToIdMap.get(p.getName());
            if ((oldId != null) && (oldId != p.getId())) {
                connIdRemapping.put(oldId, p.getId());
            }
            // If the old and new Ids match, make sure we remove ghost status
            if ((oldId != null) && (oldId == p.getId())) {
                p.setGhost(false);
            }
            // Check to see if this player's Id is taken
            String oldName = idToNameMap.get(p.getId());
            if ((oldName != null) && !oldName.equals(p.getName())) {
                // If this name doesn't belong to a current player, unassign it
                if (!currentPlayerNames.contains(oldName)) {
                    unassignedConns.add(connectionIds.get(p.getId()));
                    // Make sure we don't add this to unassigned connections twice
                    connectionIds.remove(p.getId());
                }
                // If it does belong to a current player, it'll get handled
                // when that player comes up
            }
            // Keep track of what Ids are used
            usedPlayerIds.add(p.getId());
        }

        // Remap old connection Ids to new ones
        for (Integer currConnId : connIdRemapping.keySet()) {
            Integer newId = connIdRemapping.get(currConnId);
            AbstractConnection conn = connectionIds.get(currConnId);
            conn.setId(newId);
            // If this Id is used, make sure we reassign that connection
            if (connectionIds.containsKey(newId)) {
                unassignedConns.add(connectionIds.get(newId));
            }
            // Map the new Id
            connectionIds.put(newId, conn);

            getGame().getPlayer(newId).setGhost(false);
            send(newId, new Packet(PacketCommand.LOCAL_PN, newId));
        }

        // It's possible we have players not in the saved game, add 'em
        for (AbstractConnection conn : unassignedConns) {
            int newId = 0;
            while (usedPlayerIds.contains(newId)) {
                newId++;
            }
            String name = idToNameMap.get(conn.getId());
            conn.setId(newId);
            Player newPlayer = addNewPlayer(newId, name, false);
            newPlayer.setObserver(true);
            connectionIds.put(newId, conn);
            send(newId, new Packet(PacketCommand.LOCAL_PN, newId));
        }

        // Ensure all clients are up-to-date on player info
        transmitAllPlayerUpdates();
    }

    /**
     * Shortcut to game.getPlayer(id)
     */
    public Player getPlayer(int id) {
        return getGame().getPlayer(id);
    }

    /**
     * Removes all entities owned by the given player. Should only be called when it
     * won't cause trouble (the lounge, for instance, or between phases.)
     */
    private void removeAllEntitiesOwnedBy(Player player) {
        gameManager.removeAllEntitiesOwnedBy(player);
    }

    /**
     * a shorter name for getConnection()
     */
    private AbstractConnection getClient(int connId) {
        return getConnection(connId);
    }

    /**
     * Returns a connection, indexed by id
     */
    public Enumeration<AbstractConnection> getConnections() {
        return connections.elements();
    }

    /**
     * Returns a connection, indexed by id
     */
    public AbstractConnection getConnection(int connId) {
        return connectionIds.get(connId);
    }

    /**
     * Returns a pending connection
     */
    public @Nullable AbstractConnection getPendingConnection(int connId) {
        for (AbstractConnection conn : connectionsPending) {
            if (conn.getId() == connId) {
                return conn;
            }
        }
        return null;
    }

    /**
     * Sends out all player info to the specified connection
     */
    private void transmitPlayerConnect(AbstractConnection connection) {
        for (var player : getGame().getPlayersVector()) {
            var connectionId = connection.getId();
            connection.send(createPlayerConnectPacket(player, player.getId() != connectionId));
        }
    }

    /**
     * Sends out player info to all connections
     */
    private void transmitPlayerConnect(Player player) {
        synchronized (connections) {
            for (var connection : connections) {
                var playerId = player.getId();
                connection.send(createPlayerConnectPacket(player, playerId != connection.getId()));
            }
        }
    }

    /**
     * Creates a packet informing that the player has connected
     */
    private Packet createPlayerConnectPacket(Player player, boolean isPrivate) {
        var playerId = player.getId();
        var destPlayer = player;
        if (isPrivate) {
            // Sending the player's data to another player's
            // connection, need to redact any private data
            destPlayer = player.copy();
            destPlayer.redactPrivateData();
        }
        return new Packet(PacketCommand.PLAYER_ADD, playerId, destPlayer);
    }

    /**
     * Sends out player info updates for a player to all connections
     */
    void transmitPlayerUpdate(Player player) {
        synchronized (connections) {
            for (var connection : connections) {
                var playerId = player.getId();
                var destPlayer = player;

                if (playerId != connection.getId()) {
                    // Sending the player's data to another player's
                    // connection, need to redact any private data
                    destPlayer = player.copy();
                    destPlayer.redactPrivateData();
                }
                connection.send(new Packet(PacketCommand.PLAYER_UPDATE, playerId, destPlayer));
            }
        }
    }

    /**
     * Sends out the player info updates for all players to all connections
     */
    private void transmitAllPlayerUpdates() {
        for (var player : getGame().getPlayersVector()) {
            transmitPlayerUpdate(player);
        }
    }

    public void requestTeamChange(int teamId, Player player) {
        gameManager.requestTeamChange(teamId, player);
    }

    public static String formatChatMessage(String origin, String message) {
        return origin + ": " + message;
    }

    /**
     * Transmits a chat message to all players
     */
    public void sendChat(int connId, String origin, String message) {
        send(connId, new Packet(PacketCommand.CHAT, formatChatMessage(origin, message)));
    }

    /**
     * Transmits a chat message to all players
     */
    public void sendChat(String origin, String message) {
        send(new Packet(PacketCommand.CHAT, formatChatMessage(origin, message)));
    }

    public void sendServerChat(int connId, String message) {
        sendChat(connId, ORIGIN, message);
    }

    public void sendServerChat(String message) {
        sendChat(ORIGIN, message);
    }

    void send(Packet packet) {
        synchronized (connections) {
            connections.stream()
                    .filter(Objects::nonNull)
                    .forEach(connection -> connection.send(packet));
        }
    }

    /**
     * Send a packet to a specific connection.
     */
    public void send(int connId, Packet packet) {
        if (getClient(connId) != null) {
            getClient(connId).send(packet);
        }
        // What should we do if we've lost this client?
        // For now, nothing.
    }

    /**
     * Send a packet to a pending connection
     */
    private void sendToPending(int connId, Packet packet) {
        AbstractConnection pendingConn = getPendingConnection(connId);
        if (pendingConn != null) {
            pendingConn.send(packet);
        }
        // What should we do if we've lost this client?
        // For now, nothing.
    }

    /**
     * Process an in-game command
     */
    private void processCommand(int connId, String commandString) {
        // all tokens are read as strings; if they're numbers, string-ize 'em.
        // replaced the tokenizer with the split function.
        String[] args = commandString.split("\\s+");

        // figure out which command this is
        String commandName = args[0].substring(1);

        // process it
        ServerCommand command = getCommand(commandName);
        if (command != null) {
            command.run(connId, args);
        } else {
            sendServerChat(connId, "Command not recognized. Type /help for a list of commands.");
        }
    }

    /**
     * Process a packet from a connection.
     *
     * @param connId - the <code>int</code> ID the connection that received the
     *               packet.
     * @param packet - the <code>Packet</code> to be processed.
     */
    protected void handle(int connId, Packet packet) {
        Player player = getGame().getPlayer(connId);
        // Check player. Please note, the connection may be pending.
        if ((null == player) && (null == getPendingConnection(connId))) {
            LogManager.getLogger().error("Server does not recognize player at connection " + connId);
            return;
        }

        if (packet == null) {
            LogManager.getLogger().error("Got null packet");
            return;
        }
        // act on it
        switch (packet.getCommand()) {
            case CLIENT_VERSIONS:
                final boolean valid = receivePlayerVersion(packet, connId);
                if (valid) {
                    sendToPending(connId, new Packet(PacketCommand.SERVER_GREETING));
                } else {
                    sendToPending(connId, new Packet(PacketCommand.ILLEGAL_CLIENT_VERSION, MMConstants.VERSION));
                    getPendingConnection(connId).close();
                }
                break;
            case CLOSE_CONNECTION:
                // We have a client going down!
                AbstractConnection c = getConnection(connId);
                if (c != null) {
                    c.close();
                }
                break;
            case CLIENT_NAME:
                receivePlayerName(packet, connId);
                break;
            case PLAYER_UPDATE:
                receivePlayerInfo(packet, connId);
                validatePlayerInfo(connId);
                transmitPlayerUpdate(getPlayer(connId));
                break;
            case CHAT:
                String chat = (String) packet.getObject(0);
                if (chat.startsWith("/")) {
                    processCommand(connId, chat);
                } else if (packet.getData().length > 1) {
                    connId = (int) packet.getObject(1);
                    if (connId == Player.PLAYER_NONE) {
                        sendServerChat(chat);
                    } else {
                        sendServerChat(connId, chat);
                    }
                } else {
                    sendChat(player.getName(), chat);
                }
                // Easter eggs. Happy April Fool's Day!!
                if (DUNE_CALL.equalsIgnoreCase(chat)) {
                    sendServerChat(DUNE_RESPONSE);
                } else if (STAR_WARS_CALL.equalsIgnoreCase(chat)) {
                    sendServerChat(STAR_WARS_RESPONSE);
                } else if (INVADER_ZIM_CALL.equalsIgnoreCase(chat)) {
                    sendServerChat(INVADER_ZIM_RESPONSE);
                } else if (WARGAMES_CALL.equalsIgnoreCase(chat)) {
                    sendServerChat(WARGAMES_RESPONSE);
                }
                break;
            case LOAD_GAME:
                try {
                    sendServerChat(getPlayer(connId).getName() + " loaded a new game.");
                    setGame((Game) packet.getObject(0));
                    for (AbstractConnection conn : connections) {
                        sendCurrentInfo(conn.getId());
                    }
                } catch (Exception e) {
                    LogManager.getLogger().error("Error loading save game sent from client", e);
                }
                break;
            default:
                gameManager.handlePacket(connId, packet);
        }
    }

    /**
     * Listen for incoming clients.
     */
    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        LogManager.getLogger().info("s: listening for clients...");
        while (connector == currentThread) {
            try {
                Socket s = serverSocket.accept();
                synchronized (serverLock) {
                    int id = getFreeConnectionId();
                    LogManager.getLogger().info("s: accepting player connection #" + id + "...");

                    AbstractConnection c = ConnectionFactory.getInstance().createServerConnection(s, id);
                    c.addConnectionListener(connectionListener);
                    c.open();
                    connectionsPending.addElement(c);
                    ConnectionHandler ch = new ConnectionHandler(c);
                    Thread newConnThread = new Thread(ch, "Connection " + id);
                    newConnThread.start();
                    connectionHandlers.put(id, ch);

                    clientVersionCheck(id);
                    ConnectionWatchdog w = new ConnectionWatchdog(this, id);
                    watchdogTimer.schedule(w, 1000, 500);
                }
            } catch (Exception ignored) {

            }
        }
    }

    /**
     * @return a <code>String</code> representing the hostname
     */
    public String getHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return "";
        }
    }

    /**
     * @return the <code>int</code> this server is listening on
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * @return the current server instance
     */
    public static Server getServerInstance() {
        return serverInstance;
    }

    private void registerWithServerBrowser(boolean register, String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream printout = new DataOutputStream(conn.getOutputStream());
            String content = "port=" + URLEncoder.encode(Integer.toString(serverSocket.getLocalPort()), StandardCharsets.UTF_8);
            if (register) {
                for (AbstractConnection iconn : connections) {
                    content += "&players[]=" + (getPlayer(iconn.getId()).getName());
                }
                if (!getGame().getPhase().isLounge() && !getGame().getPhase().isUnknown()) {
                    content += "&close=yes";
                }
                content += "&version=" + MMConstants.VERSION;
                if (isPassworded()) {
                    content += "&pw=yes";
                }
            } else {
                content += "&delete=yes";
            }
            if (serverAccessKey != null) {
                content += "&key=" + serverAccessKey;
            }
            printout.writeBytes(content);
            printout.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            if (conn.getResponseCode() == 200) {
                while ((line = rd.readLine()) != null) {
                    if (serverAccessKey == null) {
                        serverAccessKey = line;
                    }
                }
            }
            rd.close();
            printout.close();
        } catch (Exception ignored) {

        }
    }

    public void reportRoll(Roll roll) {
        Report r = new Report(1230);
        r.add(roll.getReport());
        gameManager.addReport(r);
    }
}
