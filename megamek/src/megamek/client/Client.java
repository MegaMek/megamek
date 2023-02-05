/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
package megamek.client;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.Version;
import megamek.client.commands.ClientCommand;
import megamek.common.Entity;
import megamek.common.GameLog;
import megamek.common.Player;
import megamek.common.UnitNameTracker;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.events.DisconnectedEvent;
import megamek.common.net.events.PacketReceivedEvent;
import megamek.common.net.factories.ConnectionFactory;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.packets.Packet;
import megamek.common.preference.PreferenceManager;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.util.*;

/**
 * This class is instantiated for each client and for each bot running on that
 * client. non-local clients are not also instantiated on the local server.
 */
public abstract class Client implements GameClient {
    public static final String CLIENT_COMMAND = "#";

    // Server connection information
    protected String name;
    protected AbstractConnection connection;
	protected Thread connThread;
    protected boolean connected = false;
    protected String host;
    protected int port;
	private ConnectionHandler packetUpdate;

	/** The ID of the local player (the player connected through this client) */
    protected int localPlayerNumber = -1;

    protected Hashtable<String, ClientCommand> commandsHash = new Hashtable<>();

	protected GameLog log;
    public String phaseReport;
    public String roundReport;

    protected boolean disconnectFlag = false;
    protected final UnitNameTracker unitNameTracker = new UnitNameTracker();

    /** The bots controlled by the local player; maps a bot's name String to a bot's client. */
    protected Map<String, Client> bots = new TreeMap<>(String::compareTo);

    /**
     * Construct a client which will try to connect. If the connection fails, it
     * will alert the player, free resources and hide the frame.
     *
     * @param name the player name for this client
     * @param host the hostname
     * @param port the host port
     */
    public Client(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    @Override
    public int getLocalPlayerNumber() {
        return localPlayerNumber;
    }

    @Override
    public void setLocalPlayerNumber(int localPlayerNumber) {
        this.localPlayerNumber = localPlayerNumber;
    }

    protected void updateConnection() {
        if (connection != null && !connection.isClosed()) {
            connection.update();
        }
    }

    /** Attempt to connect to the specified host */
    @Override
    public boolean connect() {
        connection = ConnectionFactory.getInstance().createClientConnection(host, port, 1);
        boolean result = connection.open();
        if (result) {
            connection.addConnectionListener(connectionListener);
            packetUpdate = new ConnectionHandler();
            connThread = new Thread(packetUpdate, "Client Connection, Player " + name);
            connThread.start();
        }
        return result;
    }

    /** Shuts down threads and sockets */
    @Override
    public synchronized void die() {
        // If we're still connected, tell the server that we're going down.
        if (connected) {
            // Stop listening for in coming packets, this should be done before
            // sending the close connection command
            packetUpdate.signalStop();
            connThread.interrupt();
            send(new Packet(PacketCommand.CLOSE_CONNECTION));
            flushConn();
        }
        connected = false;

        if (connection != null) {
            connection.close();
        }

        if (log != null) {
            try {
                log.close();
            } catch (Exception ex) {
                LogManager.getLogger().error("Failed to close the client game log file.", ex);
            }
        }

        LogManager.getLogger().info(getName() + " client shutdown complete.");
    }

    /** The client has become disconnected from the server */
    protected void disconnected() {
        if (!disconnectFlag) {
            disconnectFlag = true;
            if (connected) {
                die();
            }

            if (!host.equals(MMConstants.LOCALHOST)) {
                getIGame().fireGameEvent(new GamePlayerDisconnectedEvent(this, getLocalPlayer()));
            }
        }
    }

    protected void initGameLog() {
        log = new GameLog(PreferenceManager.getClientPreferences().getGameLogFilename());
        log.append("<HTML><BODY>");
    }

    /**
     * Called to determine whether the game log should be kept.
     * Default implementation delegates to {@code PreferenceManager.getClientPreferences()}.
     */
    protected boolean keepGameLog() {
        return PreferenceManager.getClientPreferences().keepGameLog();
    }

    /**
     * give the initiative to the next player on the team.
     */
    @Override
    public void sendNextPlayer() {
        send(new Packet(PacketCommand.FORWARD_INITIATIVE));
    }

    /**
     * Sends the info associated with the local player.
     */
    public void sendPlayerInfo() {
        send(new Packet(PacketCommand.PLAYER_UPDATE, getIGame().getPlayer(localPlayerNumber)));
    }

    /**
     * Broadcast a general chat message from the local player
     */
    public void sendChat(String message) {
        send(new Packet(PacketCommand.CHAT, message));
        flushConn();
    }

    /**
     * Broadcast a general chat message from the local player
     */
    public void sendServerChat(int connId, String message) {
        send(new Packet(PacketCommand.CHAT, message, connId));
        flushConn();
    }

    /**
     * Sends a "player done" message to the server.
     */
    public synchronized void sendDone(boolean done) {
        send(new Packet(PacketCommand.PLAYER_READY, done));
        flushConn();
    }

    /**
     * Receives player information from the message packet.
     */
    protected void receivePlayerInfo(Packet c) {
        int pindex = c.getIntValue(0);
        Player newPlayer = (Player) c.getObject(1);
        if (!playerExists(newPlayer.getId())) {
            getIGame().addPlayer(pindex, newPlayer);
        } else {
            getIGame().setPlayer(pindex, newPlayer);
        }
    }

    /** Sends the packet to the server, if this client is connected. Otherwise, does nothing. */
    protected void send(Packet packet) {
        if (connection != null) {
            connection.send(packet);
        }
    }

    /**
     * send all buffered packets on their way this should be called after
     * everything which causes us to wait for a reply. For example "done" button
     * presses etc. to make stuff more efficient, this should only be called
     * after a batch of packets is sent, not separately for each packet
     */
    protected void flushConn() {
        if (connection != null) {
            connection.flush();
        }
    }

    /**
     * Perform a dump of the current memory usage. This method is useful in tracking performance issues
     * on various player's systems. You can activate it by changing the "memorydumpon" setting to
     * "true" in the clientsettings.xml file.
     *
     * @param where A String indicating which part of the game is making this call.
     */
    protected void memDump(String where) {
        if (PreferenceManager.getClientPreferences().memoryDumpOn()) {
            final long total = Runtime.getRuntime().totalMemory();
            final long free = Runtime.getRuntime().freeMemory();
            final long used = total - free;
            LogManager.getLogger().error("Memory dump " + where
                    + " ".repeat(Math.max(0, 25 - where.length())) + ": used (" + used
                    + ") + free (" + free + ") = " + total);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    protected boolean isBot() {
        return false;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHost() {
        return host;
    }

    protected void correctName(Packet inP) throws Exception {
        setName((String) (inP.getObject(0)));
    }

    protected void setName(String newName) {
        name = newName;
    }

    /**
     * Before we officially "add" this unit to the game, check and see if this
     * client (player) already has a unit in the game with the same name. If so,
     * add an identifier to the units name.
     */
    protected synchronized void checkDuplicateNamesDuringAdd(Entity entity) {
        if (entity != null) {
            unitNameTracker.add(entity);
        }
    }


    /**
     * @param cmd
     *            a client command with CLIENT_COMMAND prepended.
     */
    public String runCommand(String cmd) {
        cmd = cmd.substring(CLIENT_COMMAND.length());
        return runCommand(cmd.split("\\s+"));
    }

    /**
     * Runs the command
     *
     * @param args
     *            the command and it's arguments with the CLIENT_COMMAND already
     *            removed, and the string tokenized.
     */
    public String runCommand(String[] args) {
        if ((args != null) && (args.length > 0) && commandsHash.containsKey(args[0])) {
            return commandsHash.get(args[0]).run(args);
        }
        return "Unknown Client Command.";
    }

    /** Registers a new command in the client command table. */
    @Override
    public void registerCommand(ClientCommand command) {
        // Warning, the special direction commands are registered separately
        commandsHash.put(command.getName(), command);
    }

    /** Returns the command associated with the specified name. */
    @Override
    public ClientCommand getCommand(String commandName) {
        return commandsHash.get(commandName);
    }

    @Override
    public Enumeration<String> getAllCommandNames() {
        return commandsHash.keys();
    }

    protected void handlePacket(Packet packet) {
        if (packet == null) {
            LogManager.getLogger().error("Client: Received null packet!");
            return;
        }

        if (packet.getCommand() == PacketCommand.MULTI_PACKET) {
            int packetCount = packet.getIntValue(0);
            for (int i = 0; i < packetCount; i++) {
                if (packet.getObject(i + 1) instanceof Packet) {
                    handleSinglePacket((Packet) packet.getObject(i + 1));
                } else {
                    LogManager.getLogger().error("Wrong object in MULTI_PACKET command! ");
                }
            }
        } else {
            handleSinglePacket(packet);
        }
    }

    private void handleSinglePacket(Packet packet) {
        try {
            boolean isHandled = handleGameIndependentPacket(packet);
            isHandled |= handleGameSpecificPacket(packet);
            if (!isHandled) {
                LogManager.getLogger().error("Attempted to parse unknown PacketCommand of "
                        + packet.getCommand().name());
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to parse Packet command " + packet.getCommand(), ex);
        }
    }

    /**
     * Handles any Packets that are specific to the game type (TW, AS...). When implementing this,
     * make sure that this doesn't handle packets again that are already handled in
     * {@link #handleGameIndependentPacket(Packet)} except where necessary (e.g. Precognition).
     *
     * @param packet The packet to handle
     * @return True when the packet has been handled
     */
    protected abstract boolean handleGameSpecificPacket(Packet packet) throws Exception;

    /**
     * Handles any Packets that are independent of the game type (TW, AS...).
     *
     * @param packet The packet to handle
     * @return True when the packet has been handled
     */
    protected boolean handleGameIndependentPacket(Packet packet) {
        switch (packet.getCommand()) {
            case CLOSE_CONNECTION:
                disconnected();
                break;
            case SERVER_VERSION_CHECK:
                send(new Packet(PacketCommand.CLIENT_VERSIONS, MMConstants.VERSION, MegaMek.getMegaMekSHA256()));
                break;
            case ILLEGAL_CLIENT_VERSION:
                final Version serverVersion = (Version) packet.getObject(0);
                final String message = String.format(
                        "Failed to connect to the server at %s because of version differences. " +
                                "Cannot connect to a server running %s with a %s install.",
                        getHost(), serverVersion, MMConstants.VERSION);
                JOptionPane.showMessageDialog(null, message,
                        "Connection Failure: Version Difference", JOptionPane.ERROR_MESSAGE);
                LogManager.getLogger().error(message);
                disconnected();
                break;
            case LOCAL_PN:
                localPlayerNumber = packet.getIntValue(0);
                break;
            case PLAYER_UPDATE:
            case PLAYER_ADD:
                receivePlayerInfo(packet);
                break;
            case PLAYER_READY:
                Player player = getPlayer(packet.getIntValue(0));
                if (player != null) {
                    player.setDone(packet.getBooleanValue(1));
                }
                break;
            case PLAYER_REMOVE:
                bots.values().removeIf(bot -> bot.localPlayerNumber == packet.getIntValue(0));
                getIGame().removePlayer(packet.getIntValue(0));
                break;
            case CHAT:
                possiblyWriteToLog((String) packet.getObject(0));
                getIGame().fireGameEvent(new GamePlayerChatEvent(this, null, (String) packet.getObject(0)));
                break;
            default:
                return false;
        }
        return true;
    }

    protected void possiblyWriteToLog(String message) {
        if (keepGameLog()) {
            if (log == null) {
                initGameLog();
            }
            if (log != null) {
                log.append(message);
            }
        }
    }

    @Override
    public Map<String, Client> getBots() {
        // TODO : Make this an unmodifiable map return - check write accesses to it
        return bots;
    }

    protected ConnectionListener connectionListener = new ConnectionListener() {

        @Override
        public void disconnected(DisconnectedEvent event) {
            // Always handle events on the event dispatch thread to be effectively
            // single-threaded and  avoid threading issues.
            SwingUtilities.invokeLater(Client.this::disconnected);
        }

        @Override
        public void packetReceived(final PacketReceivedEvent event) {
            // Always handle packets on the event dispatch thread to be effectively
            // single-threaded and  avoid threading issues.
            SwingUtilities.invokeLater(() -> handlePacket(event.getPacket()));
        }
    };

    protected class ConnectionHandler implements Runnable {

        boolean shouldStop = false;

        public void signalStop() {
            shouldStop = true;
        }

        @Override
        public void run() {
            while (!shouldStop) {
                // Write any queued packets
                flushConn();
                // Wait for new input
                updateConnection();
                if ((connection == null) || connection.isClosed()) {
                    shouldStop = true;
                }
            }
        }
    }
}