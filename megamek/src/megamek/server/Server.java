/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import com.thoughtworks.xstream.XStream;
import megamek.MMConstants;
import megamek.SuiteConstants;
import megamek.Version;
import megamek.client.ui.util.PlayerColour;
import megamek.codeUtilities.StringUtility;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.commandLine.AbstractCommandLineParser.ParseException;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.icons.Camouflage;
import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.factories.ConnectionFactory;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.Roll;
import megamek.common.util.EmailService;
import megamek.common.util.SerializationHelper;
import megamek.logging.MMLogger;
import megamek.server.commands.ServerCommand;

/**
 * @author Ben Mazur
 */
public class Server implements Runnable {
    // server setup
    private final String password;

    private final IGameManager gameManager;

    private final String metaServerUrl;

    private final ServerSocket serverSocket;

    private final String messageOfTheDay;

    private final EmailService mailer;

    private static final MMLogger LOGGER = MMLogger.create(Server.class);

    private final ReentrantLock GAME_LOCK = new ReentrantLock();

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
                    // If we are interrupted, just keep going, generally this happens after we are signalled to stop.
                }
            }
        }
    }

    // commands
    private final Map<String, ServerCommand> commandsHash = new ConcurrentHashMap<>();

    // game info
    private final List<AbstractConnection> connections = new CopyOnWriteArrayList<>();

    private final Map<Integer, ConnectionHandler> connectionHandlers = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<ReceivedPacket> packetQueue = new ConcurrentLinkedQueue<>();

    private final boolean dedicated;

    private final List<AbstractConnection> connectionsPending = new CopyOnWriteArrayList<>();

    private final Map<Integer, AbstractConnection> connectionIds = new ConcurrentHashMap<>();

    private int connectionCounter;

    // listens for and connects players
    private Thread connector;

    private final PacketPump packetPump;
    private Thread packetPumpThread;

    private final Timer watchdogTimer = new Timer("Watchdog Timer");

    private static Server serverInstance = null;

    private String serverAccessKey = null;

    private Timer serverBrowserUpdateTimer = null;

    /**
     * Used to ensure only one thread at a time is accessing this particular instance of the server.
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

    public static final int SERVER_CONN = Integer.MIN_VALUE;

    private final ConnectionListener connectionListener = new ConnectionListener() {

        private boolean isPaused = false;
        private final List<ReceivedPacket> pausedWaitingList = new ArrayList<>();

        /**
         * Called when it is sensed that a connection has terminated.
         */
        @Override
        public void disconnected(DisconnectedEvent e) {
            AbstractConnection conn = e.getConnection();

            // write something in the log
            LOGGER.info("s: connect {} disconnected.", conn.getId());

            connections.remove(conn);
            synchronized (serverLock) {
                connectionsPending.remove(conn);
                connectionIds.remove(conn.getId());
                ConnectionHandler ch = connectionHandlers.get(conn.getId());
                if (ch != null) {
                    ch.signalStop();
                    connectionHandlers.remove(conn.getId());
                }
            }

            // if there's a player for this connection, remove it too
            Player player = getPlayer(conn.getId());
            if (null != player) {
                Server.this.disconnected(player);
            }
        }

        @Override
        public void packetReceived(PacketReceivedEvent e) {
            ReceivedPacket rp = new ReceivedPacket(e.getConnection().getId(), e.getPacket());
            switch (e.getPacket().command()) {
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
                case PAUSE:
                    if (!isPaused) {
                        LOGGER.info("Pause packet received - pausing packet handling");
                        sendServerChat("Game is paused.");
                    }
                    isPaused = true;
                    break;
                case UNPAUSE:
                    if (isPaused) {
                        LOGGER.info("Unpause packet received - resuming packet handling");
                        sendServerChat("Game is resumed.");
                    }
                    isPaused = false;
                    synchronized (packetQueue) {
                        packetQueue.addAll(pausedWaitingList);
                        packetQueue.notifyAll();
                    }
                    pausedWaitingList.clear();
                    break;
                default:
                    if (isPaused) {
                        pausedWaitingList.add(rp);
                    } else {
                        synchronized (packetQueue) {
                            packetQueue.add(rp);
                            packetQueue.notifyAll();
                        }
                    }
                    break;
            }
        }
    };

    /**
     * @param serverAddress IP or Domain Name Of Server
     *
     * @return valid hostName
     *
     * @throws ParseException for null or empty serverAddress
     */
    public static String validateServerAddress(String serverAddress) throws ParseException {
        if ((serverAddress == null) || serverAddress.isBlank()) {
            String message = "serverAddress must not be null or empty";
            LOGGER.error(message);
            throw new ParseException(message);
        } else {
            return serverAddress.trim();
        }
    }

    /**
     * @param playerName throw ParseException if null or empty
     *
     * @return valid playerName
     */
    public static String validatePlayerName(String playerName) throws ParseException {
        if (playerName == null) {
            String message = "playerName must not be null";
            LOGGER.error(message);
            throw new ParseException(message);
        } else if (playerName.isBlank()) {
            String message = "playerName must not be empty string";
            LOGGER.error(message);
            throw new ParseException(message);
        } else {
            return playerName.trim();
        }
    }

    /**
     * @param password Password to set for server.
     *
     * @return valid password or null if no password or password is blank string
     */
    public static @Nullable String validatePassword(@Nullable String password) {
        return StringUtility.isNullOrBlank(password) ? null : password.trim();
    }

    /**
     * Checks a String against the server password
     *
     * @param password The password provided by the user.
     *
     * @return true if the user-supplied data matches the server password or no password is set.
     */
    public boolean passwordMatches(Object password) {
        return StringUtility.isNullOrBlank(this.password) || this.password.equals(password);
    }

    /**
     * @param port if outside the established range of #{@see MMConstants#MIN_PORT} and #{@see MMConstants#MAX_PORT}
     *             return #{@see MMConstants#DEFAULT_PORT} or the passed in port.
     *
     * @return valid port number
     */
    public static int validatePort(int port) {
        if (port < MMConstants.MIN_PORT || port > MMConstants.MAX_PORT) {
            return MMConstants.DEFAULT_PORT;
        }

        return port;
    }

    public Server(@Nullable String password, int port, IGameManager gameManager) throws IOException {
        this(password, port, gameManager, false, "", null, false);
    }

    public Server(@Nullable String password, int port, IGameManager gameManager, boolean registerWithServerBrowser,
          @Nullable String metaServerUrl) throws IOException {
        this(password, port, gameManager, registerWithServerBrowser, metaServerUrl, null, false);
    }

    /**
     * Construct a new GameHost and begin listening for incoming clients.
     *
     * @param password                  the <code>String</code> that is set as a password
     * @param port                      the <code>int</code> value that specifies the port that is used
     * @param gameManager               the {@link IGameManager} instance for this server instance.
     * @param registerWithServerBrowser a <code>boolean</code> indicating whether we should register with the master
     *                                  server browser on <a href="https://api.megamek.org">...</a>
     * @param mailer                    an email service instance to use for sending round reports.
     * @param dedicated                 set to true if this server is started from a GUI-less context
     */
    public Server(@Nullable String password, int port, IGameManager gameManager, boolean registerWithServerBrowser,
          @Nullable String metaServerUrl, @Nullable EmailService mailer, boolean dedicated) throws IOException {
        this.metaServerUrl = StringUtility.isNullOrBlank(metaServerUrl) ? null : metaServerUrl;
        this.password = StringUtility.isNullOrBlank(password) ? null : password;
        this.gameManager = gameManager;
        this.mailer = mailer;
        this.dedicated = dedicated;

        // initialize server socket
        serverSocket = new ServerSocket(port);

        messageOfTheDay = createMOTD();

        // display server start text
        LOGGER.info("s: starting a new server...");

        try {
            String host = InetAddress.getLocalHost().getHostName();
            LOGGER.info("s: hostname = '{}' port = {}", host, serverSocket.getLocalPort());

            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                LOGGER.info("s: hosting on address = {}", address.getHostAddress());
            }
        } catch (Exception ignored) {
        }

        if (password != null) {
            LOGGER.info("s: password is set (redacted)");
        }

        for (ServerCommand command : gameManager.getCommandList(this)) {
            registerCommand(command);
        }

        packetPump = new PacketPump();
        packetPumpThread = new Thread(packetPump, "Packet Pump");
        packetPumpThread.start();

        if (registerWithServerBrowser) {
            if (!StringUtility.isNullOrBlank(metaServerUrl)) {
                final TimerTask register = new TimerTask() {
                    @Override
                    public void run() {
                        registerWithServerBrowser(true, Server.getServerInstance().metaServerUrl);
                    }
                };
                serverBrowserUpdateTimer = new Timer("Server Browser Register Timer", true);
                serverBrowserUpdateTimer.schedule(register, 1, 40000);
            } else {
                LOGGER.error("Invalid URL for server browser {}", this.metaServerUrl);
            }
        }

        // Fully initialized, now accept connections
        connector = new Thread(this, "Connection Listener");
        connector.start();

        serverInstance = this;
    }

    public IGameManager getGameManager() {
        return gameManager;
    }

    /**
     * Sets the game for this server. Restores any transient fields, and sets all players as ghosts. This should only be
     * called during server initialization before any players have connected.
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
     * Make a default message o' the day containing the version string, and if it was found, the build timestamp
     */
    private String createMOTD() {
        return "Welcome to MegaMek. Server is running version " + SuiteConstants.VERSION;
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
        connectionsPending.forEach(AbstractConnection::close);
        connectionsPending.clear();

        // Send "kill" commands to all connections
        // This WILL handle the connection end on both sides
        send(new Packet(PacketCommand.CLOSE_CONNECTION));
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
    public Collection<String> getAllCommandNames() {
        return commandsHash.keySet();
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
        while ((getPendingConnection(connectionCounter) != null) ||
              (getConnection(connectionCounter) != null) ||
              (getPlayer(connectionCounter) != null)) {
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
    private void receivePlayerInfo(Packet packet, int connId) throws InvalidPacketDataException {
        Player player = packet.getPlayer(0);

        if (player == null) {
            return;
        }

        Player gamePlayer = getGame().getPlayer(connId);
        if (null != gamePlayer) {
            gamePlayer.setColour(player.getColour());
            gamePlayer.setStartingPos(player.getStartingPos());
            gamePlayer.setStartWidth(player.getStartWidth());
            gamePlayer.setStartOffset(player.getStartOffset());
            gamePlayer.setStartingAnyNWx(player.getStartingAnyNWx());
            gamePlayer.setStartingAnyNWy(player.getStartingAnyNWy());
            gamePlayer.setStartingAnySEx(player.getStartingAnySEx());
            gamePlayer.setStartingAnySEy(player.getStartingAnySEy());
            gamePlayer.setTeam(player.getTeam());
            gamePlayer.setCamouflage(player.getCamouflage().clone());
            gamePlayer.setNbrMFConventional(player.getNbrMFConventional());
            gamePlayer.setNbrMFCommand(player.getNbrMFCommand());
            gamePlayer.setNbrMFVibra(player.getNbrMFVibra());
            gamePlayer.setNbrMFActive(player.getNbrMFActive());
            gamePlayer.setNbrMFInferno(player.getNbrMFInferno());
            gamePlayer.setNbrMFEMP(player.getNbrMFEMP());
            if (gamePlayer.getConstantInitBonus() != player.getConstantInitBonus()) {
                sendServerChat("Player " +
                      gamePlayer.getName() +
                      " changed their initiative bonus from " +
                      gamePlayer.getConstantInitBonus() +
                      " to " +
                      player.getConstantInitBonus() +
                      '.');
            }
            gamePlayer.setConstantInitBonus(player.getConstantInitBonus());
            gamePlayer.setEmail(player.getEmail());
            gamePlayer.setGroundObjectsToPlace(new ArrayList<>(player.getGroundObjectsToPlace()));
        }
    }

    /**
     * Correct a duplicate player name
     *
     * @param oldName the <code>String</code> old player name, that is a duplicate
     *
     * @return the <code>String</code> new player name
     */
    private String correctDupeName(String oldName) {
        for (Player player : getGame().getPlayersList()) {
            if (player.getName().equals(oldName)) {
                // We need to correct it.
                String newName = oldName;
                int dupNum;
                try {
                    dupNum = Integer.parseInt(oldName.substring(oldName.lastIndexOf('.') + 1));
                    dupNum++;
                    newName = oldName.substring(0, oldName.lastIndexOf('.'));
                } catch (Exception e) {
                    // If this fails, we don't care much. Just assume it's the first time for this name.
                    dupNum = 2;
                }
                newName = newName.concat(".").concat(Integer.toString(dupNum));
                return correctDupeName(newName);
            }
        }
        return oldName;
    }

    private boolean receivePlayerVersion(Packet packet, int connId) throws InvalidPacketDataException {
        final Version version = (Version) packet.getObject(0);

        if (!SuiteConstants.VERSION.is(version)) {
            final String message = String.format("Client/Server Version Mismatch -- Client: %s, Server: %s",
                  version,
                  SuiteConstants.VERSION);
            LOGGER.error(message);

            final Player player = getPlayer(connId);
            sendServerChat(String.format("For %s, Server reports:%s%s",
                  ((player == null) ? "unknown player" : player.getName()),
                  System.lineSeparator(),
                  message));
            return false;
        }

        return true;
    }

    /**
     * Receives a player name, sent from a pending connection, and connects that connection.
     */
    private void receivePlayerName(Packet packet, int connId) throws InvalidPacketDataException {
        final AbstractConnection conn = getPendingConnection(connId);
        String name = packet.getStringValue(0);
        boolean isBot = packet.getBooleanValue(1);
        boolean returning = false;
        String message;

        // this had better be from a pending connection
        if (conn == null) {
            LOGGER.warn("Got a client name from a non-pending connection");
            return;
        }

        // check if they're connecting with the same name as a ghost player
        for (Player player : getGame().getPlayersList()) {
            if (player.getName().equals(name) && player.isGhost()) {
                returning = true;
                player.setGhost(false);
                player.setBot(isBot);
                // switch id
                connId = player.getId();
                conn.setId(connId);
            }
        }

        if (!returning) {
            // Check to avoid duplicate names...
            name = correctDupeName(name);
            sendToPending(connId, new Packet(PacketCommand.SERVER_CORRECT_NAME, name));
        }

        // right, switch the connection into the "active" bin
        connectionsPending.remove(conn);
        connections.add(conn);
        connectionIds.put(conn.getId(), conn);

        // add and validate the player info
        if (!returning) {
            addNewPlayer(connId, name, isBot);
        }

        // if it is not the lounge phase, this player becomes an observer
        Player player = getPlayer(connId);
        if (!getGame().getPhase().isLounge() && (null != player) && (getGame().getEntitiesOwnedBy(player) < 1)) {
            player.setObserver(true);
        }

        // send the player the messageOfTheDay
        sendServerChat(connId, messageOfTheDay);

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
                message = String.format("s: Machine IP %s", address.getHostAddress());
                LOGGER.info(message);

                if (showIPAddressesInChat) {
                    sendServerChat(connId, message);
                }
            }
        } catch (Exception ignored) {
        }

        message = String.format("s: Listening on port %d", serverSocket.getLocalPort());
        LOGGER.info(message);

        if (showIPAddressesInChat) {
            // Send the port we're listening on. Only useful for the player on the server machine to check.
            sendServerChat(connId, message);
        }

        // Get the player *again*, because they may have disconnected.
        player = getPlayer(connId);
        if (null != player) {
            String who = String.format("%s connected from %s", player.getName(), getClient(connId).getInetAddress());
            message = String.format("s: player #%d, %s", connId, who);
            LOGGER.info(message);

            if (showIPAddressesInChat) {
                sendServerChat(who);
            }
        } // Found the player
    }

    /**
     * Sends a player the info they need to look at the current phase. This is triggered when a player first connects to
     * the server.
     */
    public void sendCurrentInfo(int connId) {
        transmitPlayerConnect(getClient(connId));
        gameManager.sendCurrentInfo(connId);
    }

    /**
     * Adds a new player to the game
     */
    private Player addNewPlayer(int connId, String name, boolean isBot) {
        int team = getTeam(isBot);
        Player newPlayer = new Player(connId, name);
        newPlayer.setBot(isBot);
        PlayerColour colour = newPlayer.getColour();
        final PlayerColour[] colours = PlayerColour.values();

        for (Player player : getGame().getPlayersList()) {
            if (player.getId() == newPlayer.getId()) {
                continue;
            }

            if ((player.getColour() == colour) && (colours.length > (colour.ordinal() + 1))) {
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

    private int getTeam(boolean isBot) {
        int team = Player.TEAM_UNASSIGNED;

        if (getGame().getPhase().isLounge()) {
            team = Player.TEAM_NONE;
            final var gOpts = getGame().getOptions();
            if (isBot || !gOpts.booleanOption(OptionsConstants.BASE_SET_DEFAULT_TEAM_1)) {
                for (Player p : getGame().getPlayersList()) {
                    if (p.getTeam() > team) {
                        team = p.getTeam();
                    }
                }
                team++;
            } else {
                team = 1;
            }

        }

        return team;
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
            for (Player otherPlayer : getGame().getPlayersList()) {
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
     * Called when it's been determined that an actual player disconnected. Notifies the other players and does the
     * appropriate housekeeping.
     */
    void disconnected(Player player) {
        // Only do this if nobody is trying to save a game currently
        GAME_LOCK.lock();
        try {
            gameManager.disconnect(player);
        } finally {
            GAME_LOCK.unlock();
        }
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
     *
     * @return A <code>boolean</code> value whether the loading was successful
     */
    public boolean loadGame(File f) {
        return loadGame(f, true);
    }

    /**
     * load the game
     *
     * @param f        The <code>File</code> to load
     * @param sendInfo Determines whether the connections should be updated with current info. This may be false if some
     *                 reconnection remapping needs to be done first.
     *
     * @return A <code>boolean</code> value whether the loading was successful
     */
    public boolean loadGame(File f, boolean sendInfo) {
        String message = String.format("s: Loading saved game file '%s'", f.getAbsolutePath());
        LOGGER.info(message);

        Game newGame;
        try (InputStream is = new FileInputStream(f)) {
            InputStream gzi;

            if (f.getName().toLowerCase().endsWith(".gz")) {
                gzi = new GZIPInputStream(is);
            } else {
                gzi = is;
            }

            XStream xStream = SerializationHelper.getLoadSaveGameXStream();
            newGame = (Game) xStream.fromXML(gzi);
        } catch (Exception e) {
            message = String.format("Unable to load file: %s", f);
            LOGGER.error(e, message);
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
        GAME_LOCK.lock();
        try {
            gameManager.saveGame(fileName);
        } finally {
            GAME_LOCK.unlock();
        }
    }

    public void sendSaveGame(int connId, String fileName, String localPath) {
        GAME_LOCK.lock();
        try {
            gameManager.sendSaveGame(connId, fileName, localPath);
        } finally {
            GAME_LOCK.unlock();
        }
    }

    /**
     * When the load command is used, there is a list of already connected players which have assigned names and player
     * id numbers with the id numbers matching the connection numbers. When a new game is loaded, this mapping may need
     * to be updated. This method takes a map of player names to their current ids, and uses the list of players to
     * figure out what the current ids should change to.
     *
     * @param nameToIdMap This maps a player name to the current connection ID
     * @param idToNameMap This maps a current conn ID to a player name, and is just the inverse mapping from
     *                    nameToIdMap
     */
    public void remapConnIds(Map<String, Integer> nameToIdMap, Map<Integer, String> idToNameMap) {
        // Keeps track of connections without Ids
        List<AbstractConnection> unassignedConnections = new ArrayList<>();

        // Keep track of which ids are used
        Set<Integer> usedPlayerIds = new HashSet<>();
        Set<String> currentPlayerNames = new HashSet<>();

        for (Player p : getGame().getPlayersList()) {
            currentPlayerNames.add(p.getName());
        }

        // Map the old connection ID to new value
        Map<Integer, Integer> connIdRemapping = new HashMap<>();
        for (Player p : getGame().getPlayersList()) {
            // Check to see if this player was already connected
            Integer oldId = nameToIdMap.get(p.getName());
            if ((oldId != null) && (oldId != p.getId())) {
                connIdRemapping.put(oldId, p.getId());
            }

            // If the old and new Ids match, make sure we remove ghost status
            if ((oldId != null) && (oldId == p.getId())) {
                p.setGhost(false);
            }

            // Check to see if this player's ID is taken
            String oldName = idToNameMap.get(p.getId());
            if ((oldName != null) && !oldName.equals(p.getName())) {
                // If this name doesn't belong to a current player, unassign it
                if (!currentPlayerNames.contains(oldName)) {
                    unassignedConnections.add(connectionIds.get(p.getId()));
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
        for (Entry<Integer, Integer> currentConnect : connIdRemapping.entrySet()) {
            Integer newId = currentConnect.getValue();
            AbstractConnection conn = connectionIds.get(currentConnect.getKey());
            conn.setId(newId);

            // If this ID is used, make sure we reassign that connection
            if (connectionIds.containsKey(newId)) {
                unassignedConnections.add(connectionIds.get(newId));
            }
            // Map the new ID
            connectionIds.put(newId, conn);

            getGame().getPlayer(newId).setGhost(false);
            send(newId, new Packet(PacketCommand.LOCAL_PN, newId));
        }

        // It's possible we have players not in the saved game, add 'em
        for (AbstractConnection conn : unassignedConnections) {
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
     * a shorter name for getConnection()
     */
    private AbstractConnection getClient(int connId) {
        return getConnection(connId);
    }

    /**
     * Executes the process on each active connection.
     *
     * @param process The process to execute.
     */
    public void forEachConnection(Consumer<AbstractConnection> process) {
        connections.forEach(process);
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
        for (Player player : getGame().getPlayersList()) {
            var connectionId = connection.getId();
            connection.send(createPlayerConnectPacket(player, player.getId() != connectionId));
        }
    }

    /**
     * Sends out player info to all connections
     */
    private void transmitPlayerConnect(Player player) {
        for (var connection : connections) {
            var playerId = player.getId();
            connection.send(createPlayerConnectPacket(player, playerId != connection.getId()));
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

    /**
     * Sends out the player info updates for all players to all connections
     */
    private void transmitAllPlayerUpdates() {
        for (Player player : getGame().getPlayersList()) {
            transmitPlayerUpdate(player);
        }
    }

    /**
     * Player can request its own change of team
     *
     * @param teamID target team id
     * @param player player requesting the change
     */
    public void requestTeamChangeForPlayer(int teamID, Player player) {
        gameManager.requestTeamChangeForPlayer(teamID, player);
    }

    public void requestGameMaster(Player player) {
        gameManager.requestGameMaster(player);
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

    /**
     * Sends the given packet to all connections (all connected Clients = players).
     */
    void send(Packet packet) {
        connections.stream().filter(Objects::nonNull).forEach(connection -> connection.send(packet));
    }

    /**
     * Sends the given packet to the given connection (= player ID) if it is not null. Does nothing otherwise.
     */
    public void send(int connId, Packet packet) {
        AbstractConnection connection = getClient(connId);

        if (connection != null) {
            connection.send(packet);
        }
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
     * @param connId - the <code>int</code> ID the connection that received the packet.
     * @param packet - the <code>Packet</code> to be processed.
     */
    protected void handle(int connId, Packet packet) {
        Player player = getGame().getPlayer(connId);

        // Check player. Please note, the connection may be pending.
        if ((null == player) && (null == getPendingConnection(connId))) {
            String message = String.format("Server does not recognize player at connection %d", connId);
            LOGGER.error(message);
            return;
        }

        if (packet == null) {
            LOGGER.error("Got null packet");
            return;
        }

        // act on it
        try {
            switch (packet.command()) {
                case CLIENT_VERSIONS:
                    final boolean valid = receivePlayerVersion(packet, connId);
                    if (valid) {
                        sendToPending(connId, new Packet(PacketCommand.SERVER_GREETING));
                    } else {
                        sendToPending(connId, new Packet(PacketCommand.ILLEGAL_CLIENT_VERSION, SuiteConstants.VERSION));
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
                    String chat = packet.getStringValue(0);
                    if (chat.startsWith("/")) {
                        processCommand(connId, chat);
                    } else if (packet.data().length > 1) {
                        connId = packet.getIntValue(1);
                        if (connId == Player.PLAYER_NONE) {
                            sendServerChat(chat);
                        } else {
                            sendServerChat(connId, chat);
                        }
                    } else {
                        if (player != null) {
                            sendChat(player.getName(), chat);
                        }
                    }

                    // Easter eggs. Happy April Fool's Day!!
                    if (DUNE_CALL.equalsIgnoreCase(chat)) {
                        sendServerChat(DUNE_RESPONSE);
                    } else if (STAR_WARS_CALL.equalsIgnoreCase(chat)) {
                        sendServerChat(STAR_WARS_RESPONSE);
                    } else if (INVADER_ZIM_CALL.equalsIgnoreCase(chat)) {
                        sendServerChat(INVADER_ZIM_RESPONSE);
                    } else if (WARGAMES_CALL.equalsIgnoreCase(chat)) {
                        wargamesResponse();
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
                        LOGGER.error(e, "Error loading save game sent from client");
                    }
                    break;
                default:
                    // We don't want to change the game state while other threads may be in the game
                    GAME_LOCK.lock();
                    try {
                        gameManager.handlePacket(connId, packet);
                    } finally {
                        GAME_LOCK.unlock();
                    }
            }
        } catch (InvalidPacketDataException e) {
            LOGGER.error("Invalid packet data:", e);
        }
    }

    private void wargamesResponse() {
        sendServerChat(WARGAMES_RESPONSE);
    }

    /**
     * Listen for incoming clients.
     */
    @Override
    public void run() {
        String message;
        Thread currentThread = Thread.currentThread();
        LOGGER.info("s: listening for clients...");
        while (connector == currentThread) {
            try {
                Socket s = serverSocket.accept();
                synchronized (serverLock) {
                    int id = getFreeConnectionId();
                    message = String.format("s: accepting player connection #%d...", id);
                    LOGGER.info(message);

                    AbstractConnection c = ConnectionFactory.getInstance().createServerConnection(s, id);
                    c.addConnectionListener(connectionListener);
                    c.open();
                    connectionsPending.add(c);
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
            LOGGER.error(ex, "Get Host exception");
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
     * @return the current server instance. This may be null if a server has not been started
     */
    public static @Nullable Server getServerInstance() {
        return serverInstance;
    }

    private void registerWithServerBrowser(boolean register, String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStream os = conn.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            StringBuilder content = new StringBuilder("port=" +
                  URLEncoder.encode(Integer.toString(serverSocket.getLocalPort()),
                        StandardCharsets.UTF_8));
            if (register) {
                for (AbstractConnection abstractConnection : connections) {
                    content.append("&players[]=").append(getPlayer(abstractConnection.getId()).getName());
                }

                if (!getGame().getPhase().isLounge() && !getGame().getPhase().isUnknown()) {
                    content.append("&close=yes");
                }

                content.append("&version=").append(SuiteConstants.VERSION);

                if (isPassworded()) {
                    content.append("&pw=yes");
                }
            } else {
                content.append("&delete=yes");
            }

            if (serverAccessKey != null) {
                content.append("&key=").append(serverAccessKey);
            }

            dos.writeBytes(content.toString());
            dos.flush();

            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            if (conn.getResponseCode() == 200) {
                while ((line = br.readLine()) != null) {
                    if (serverAccessKey == null) {
                        serverAccessKey = line;
                    }
                }
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Adds a roll report to the GameManager's current pending report list.
     *
     * @param roll The roll to add
     */
    public void reportRoll(Roll roll) {
        gameManager.addReport(getGame().getNewReport(1230).addRoll(roll));
    }
}
