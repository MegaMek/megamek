/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.server.commands.*;
import megamek.common.net.*;
import megamek.common.options.*;
import megamek.common.util.BoardUtilities;
import megamek.common.util.StringUtil;

/**
 * @author Ben Mazur
 */
public class Server
implements Runnable, ConnectionHandler {
    //    public final static String  LEGAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.-";
    public final static String  DEFAULT_BOARD = MapSettings.BOARD_SURPRISE;

    // server setup
    private String              password;
    private ServerSocket        serverSocket;
    private ServerLog           log = new ServerLog();
    private String              motd;

    // game info
    private Vector              connections = new Vector(4);
    private Vector              connectionsPending = new Vector(4);
    private Hashtable           connectionIds = new Hashtable();

    private int                 connectionCounter = 0;
    private int                 entityCounter = 0;

    private Game                game = new Game();

    private StringBuffer        roundReport = game.getRoundReport(); //HACK
    private StringBuffer        phaseReport = game.getPhaseReport(); //HACK

    private MapSettings         mapSettings = new MapSettings();

    // commands
    private Hashtable           commandsHash = new Hashtable();

    // listens for and connects players
    private Thread              connector;

    // Track buildings that are affected by an entity's movement.
    private Hashtable           affectedBldgs = new Hashtable();

    // Track Physical Action results, HACK to deal with opposing pushes 
    // canceling each other
    private Vector              physicalResults = new Vector();
    
    /* Tracks entities which have been destroyed recently.  Allows refactoring of the
     * damage and kill logic from Server, where it is now, to the Entity subclasses eventually.  
     * This has not been implemented yet -- I am just starting to build the groundwork into Server.
     * It isn't in the execution path and shouldn't cause any bugs */
    //Note from another coder - I have commented out your groundwork
    //for now because it is using HashSet, which isn't available in
    //Java 1.1 unless you import the collections classes.  Since the
    //Server class isn't using any other collecitons classes, there
    //might be a reason we're avoiding them here...if not, feel free
    //to add the import.
    //private HashSet             knownDeadEntities = new HashSet();

    /**
     * Construct a new GameHost and begin listening for
     * incoming clients.
     * @param   password the <code>String</code> that is set as a password
     * @param   port the <code>int</code> value that specifies the port that
     *          is used
     */
    public Server(String password, int port) {
        this.password = password.length() > 0 ? password : null;
        // initialize server socket
        try {
            serverSocket = new ServerSocket(port);
        } catch(IOException ex) {
            System.err.println("could not create server socket on port "+port);
        }

        motd = createMotd();

        game.getOptions().initialize();

        changePhase(Game.PHASE_LOUNGE);

        // display server start text
        System.out.println("s: starting a new server...");

        try {
            String host = InetAddress.getLocalHost().getHostName();
            System.out.print("s: hostname = '" );
            System.out.print( host );
            System.out.print( "' port = " );
            System.out.println( serverSocket.getLocalPort() );
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (int i = 0; i < addresses.length; i++) {
                System.out.println("s: hosting on address = "
                                   + addresses[i].getHostAddress());
            }
        } catch (UnknownHostException  e) {
            // oh well.
        }

        System.out.println("s: password = " + this.password);

        connector = new Thread(this, "Connection Listener");
        connector.start();

        // register commands
        registerCommand(new DefeatCommand(this));
        registerCommand(new HelpCommand(this));
        registerCommand(new KickCommand(this));
        registerCommand(new ResetCommand(this));
        registerCommand(new RollCommand(this));
        registerCommand(new SaveGameCommand(this));
        registerCommand(new SkipCommand(this));
        registerCommand(new VictoryCommand(this));
        registerCommand(new WhoCommand(this));
        registerCommand(new SeeAllCommand(this));
        registerCommand(new HeatSinkCommand(this));
    }

    /**
     * Sets the game for this server.  Restores any transient fields, and sets
     * all players as ghosts.
     * This should only be called during server initialization before any
     * players have connected.
     */
    public void setGame(Game g) {
        this.game = g;

        // reattach the transient fields and ghost the players
        for (Enumeration e = game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = (Entity)e.nextElement();
            ent.setGame(game);
        }
        game.setOutOfGameEntitiesVector(game.getOutOfGameEntitiesVector());
        for (Enumeration e = game.getPlayers(); e.hasMoreElements(); ) {
            Player p = (Player)e.nextElement();
            p.setGame(game);
            p.setGhost(true);
        }

        //HACK
        roundReport = game.getRoundReport();
        phaseReport = game.getPhaseReport();
    }

    /** Returns the current game object */
    public Game getGame() {
        return game;
    }

    /**
     * Make a default message o' the day containing the version string, and
     * if it was found, the build timestamp
     */
    private String createMotd() {
        StringBuffer buf = new StringBuffer();
        buf.append("Welcome to MegaMek.  Server is running version ");
        buf.append(MegaMek.VERSION);
        buf.append(", build date ");
        if (MegaMek.TIMESTAMP > 0L) {
            buf.append(new Date(MegaMek.TIMESTAMP).toString());
        } else {
            buf.append("unknown");
        }
        buf.append(".");

        return buf.toString();
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
        return (ServerCommand)commandsHash.get(name);
    }

    /**
     * Shuts down the server.
     */
    public void die() {
        // kill thread accepting new connections
        connector = null;

        // close socket
        try {
            serverSocket.close();
        } catch(IOException ex) { ; }

        // kill pending connnections
        for (Enumeration i=connectionsPending.elements();i.hasMoreElements();){
            final Connection conn = (Connection)i.nextElement();
            conn.die();
        }
        connectionsPending.removeAllElements();

        // Send "kill" commands to all connections
        // N.B. I may be starting a race here.
        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            send(conn.getId(), new Packet(Packet.COMMAND_CLOSE_CONNECTION));
        }

        // kill active connnections
        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            conn.die();
        }
        connections.removeAllElements();
        connectionIds.clear();
    }

    /**
     * Returns an enumeration of all the command names
     */
    public Enumeration getAllCommandNames() {
        return commandsHash.keys();
    }

    /**
     * Sent when a clients attempts to connect.
     */
    private void greeting(int cn) {
        // send server greeting -- client should reply with client info.
        sendToPending(cn, new Packet(Packet.COMMAND_SERVER_GREETING));
    }

    /**
     * Returns a free connection id.
     */
    public int getFreeConnectionId() {
        while (getPendingConnection(connectionCounter) != null
        || getConnection(connectionCounter) != null
        || getPlayer(connectionCounter) != null) {
            connectionCounter++;
        }
        return connectionCounter;
    }

    /**
     * Returns a free entity id.  Perhaps this should be in Game instead.
     */
    public int getFreeEntityId() {
        return game.getNextEntityId();
    }

    /**
     * Allow the player to set whatever parameters he is able to
     */
    private void receivePlayerInfo(Packet packet, int connId) {
        Player player = (Player)packet.getObject(0);
        Player connPlayer = game.getPlayer( connId );
        if ( null != connPlayer ) {
            connPlayer.setColorIndex(player.getColorIndex());
            connPlayer.setStartingPos(player.getStartingPos());
            connPlayer.setTeam(player.getTeam());
            connPlayer.setCamoCategory(player.getCamoCategory());
            connPlayer.setCamoFileName(player.getCamoFileName());
            connPlayer.setNbrMFConventional(player.getNbrMFConventional());
            connPlayer.setNbrMFCommand(player.getNbrMFCommand());
            connPlayer.setNbrMFVibra(player.getNbrMFVibra());
        }
    }

    /**
     * Recieves a player name, sent from a pending connection, and connects
     * that connection.
     */
    private void receivePlayerName(Packet packet, int connId) {
        final Connection conn = getPendingConnection(connId);
        String name = (String)packet.getObject(0);
        boolean returning = false;

        // this had better be from a pending connection
        if (conn == null) {
            System.out.println("server: got a client name from a non-pending" +
                               " connection");
            return;
        }

        // check if they're connecting with the same name as a ghost player
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            if (player.isGhost() && player.getName().equals(name)) {
                returning = true;
                player.setGhost(false);
                // switch id
                connId = player.getId();
                conn.setId(connId);
            }
        }

        // right, switch the connection into the "active" bin
        connectionsPending.removeElement(conn);
        connections.addElement(conn);
        connectionIds.put(new Integer(conn.getId()), conn);

        // add and validate the player info
        if (!returning) {
            game.addPlayer(connId, new Player(connId, name));
            validatePlayerInfo(connId);
        }

        // if it is not the lounge phase, this player becomes an observer
        Player player = getPlayer( connId );
        if ( game.getPhase() != Game.PHASE_LOUNGE
             && null != player
             && game.getEntitiesOwnedBy(player) < 1) {
            player.setObserver(true);
        }

        // send the player the motd
        sendServerChat(connId, motd);

        // send info that the player has connected
        send(createPlayerConnectPacket(connId));

        // tell them their local playerId
        send(connId, new Packet(Packet.COMMAND_LOCAL_PN, new Integer(connId)));

        // send current game info
        sendCurrentInfo(connId);

        try {
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress
                                                               .getLocalHost()
                                                               .getHostName());
            for (int i = 0; i < addresses.length; i++) {
                sendServerChat(connId, "Machine IP is " +
                               addresses[i].getHostAddress());
            }
        } catch (UnknownHostException  e) {
            // oh well.
        }

        // Send the port we're listening on. Only useful for the player
        // on the server machine to check.
        sendServerChat(connId, "Listening on port " + serverSocket
                                                      .getLocalPort());

        // Get the player *again*, because they may have disconnected.
        player = getPlayer( connId );
        if ( null != player ) {
            StringBuffer buff = new StringBuffer();
            buff.append( player.getName() )
                .append( " connected from " )
                .append( getClient(connId).getSocket().getInetAddress() );
            String who = buff.toString();
            System.out.print( "s: player #" );
            System.out.print( connId );
            System.out.print( ", " );
            System.out.println( who );

            sendServerChat( who );

            // there is more than one player, uncheck the friendly fire option
            if ( game.getNoOfPlayers() > 1
                 && game.getOptions().booleanOption("friendly_fire") ) {
                game.getOptions().getOption("friendly_fire").setValue(false);
                send(createGameSettingsPacket());
            }
        } // Found the player
    }

    /**
     * Sends a player the info they need to look at the current phase
     */
    private void sendCurrentInfo(int connId) {
        //why are these two outside the player != null check below?
        transmitAllPlayerConnects(connId);
        send(connId, createGameSettingsPacket());

        Player player = game.getPlayer(connId);
        if ( null != player ) {
            send(connId, new Packet(Packet.COMMAND_SENDING_MINEFIELDS,
                                    player.getMinefields()));

            switch (game.getPhase()) {
            case Game.PHASE_LOUNGE :
                send(connId, createMapSettingsPacket());
                // Send Entities *after* the Lounge Phase Change
                send(connId, new Packet(Packet.COMMAND_PHASE_CHANGE,
                                        new Integer(game.getPhase())));
                if (doBlind()) {
                    send(connId, createFilteredFullEntitiesPacket(player));
                }
                else {
                    send(connId, createFullEntitiesPacket());
                }
                break;
            default :
                send(connId, createReportPacket());

                // Send Entites *before* other phase changes.
                if (doBlind()) {
                    send(connId, createFilteredFullEntitiesPacket(player));
                }
                else {
                    send(connId, createFullEntitiesPacket());
                }
                player.setDone( game.getEntitiesOwnedBy(player) <= 0 );
                send(connId, createBoardPacket());
                send(connId, new Packet(Packet.COMMAND_PHASE_CHANGE,
                                        new Integer(game.getPhase())));
                break;
            }
            if (game.getPhase() == Game.PHASE_FIRING ||
                game.getPhase() == Game.PHASE_TARGETING ||
                game.getPhase() == Game.PHASE_OFFBOARD ||
                game.getPhase() == Game.PHASE_PHYSICAL) {
                // can't go above, need board to have been sent
                send(connId,createAttackPacket(game.getActionsVector(),false));
                send(connId,createAttackPacket(game.getChargesVector(),true));
            }
            if (game.phaseHasTurns(game.getPhase())) {
                send(connId, createTurnVectorPacket());
                send(connId, createTurnIndexPacket());
            }

        } // Found the player.

    }

    /**
     * Validates the player info.
     */
    public void validatePlayerInfo(int playerId) {
        final Player player = getPlayer(playerId);

        //        maybe this isn't actually useful
        //        // replace characters we don't like with "X"
        //        StringBuffer nameBuff = new StringBuffer(player.getName());
        //        for (int i = 0; i < nameBuff.length(); i++) {
        //            int chr = nameBuff.charAt(i);
        //            if (LEGAL_CHARS.indexOf(chr) == -1) {
        //                nameBuff.setCharAt(i, 'X');
        //            }
        //        }
        //        player.setName(nameBuff.toString());

        //TODO: check for duplicate or reserved names

        // make sure colorIndex is unique
        boolean[] colorUsed = new boolean[Player.colorNames.length];
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player otherPlayer = (Player)i.nextElement();
            if (otherPlayer.getId() != playerId) {
                colorUsed[otherPlayer.getColorIndex()] = true;
            }
        }
        if (null != player && colorUsed[player.getColorIndex()]) {
            // find a replacement color;
            for (int i = 0; i < colorUsed.length; i++) {
                if (!colorUsed[i]) {
                    player.setColorIndex(i);
                    break;
                }
            }
        }

    }

    /**
     * Called when it is sensed that a connection has terminated.
     */
    public void disconnected(Connection conn) {
        // write something in the log
        System.out.println("s: connection " + conn.getId() + " disconnected");

        // kill the connection and remove it from any lists it might be on
        conn.die();
        connections.removeElement(conn);
        connectionsPending.removeElement(conn);
        connectionIds.remove(new Integer(conn.getId()));

        // if there's a player for this connection, remove it too
        Player player = getPlayer(conn.getId());
        if (null != player) {
            disconnected( player );
        }
    }

    /**
     * Called when it's been determined that an actual player
     * disconnected.  Notifies the other players and does the appropriate
     * housekeeping.
     */
    void disconnected(Player player) {
        // in the lounge, just remove all entities for that player
        if (game.getPhase() == Game.PHASE_LOUNGE) {
            removeAllEntitesOwnedBy(player);
        }

        // if a player has active entities, he becomes a ghost
        if (game.getEntitiesOwnedBy(player) > 0) {
            player.setGhost(true);
            player.setDone(true);
            send(createPlayerUpdatePacket(player.getId()));
        } else {
            game.removePlayer(player.getId());
            send(new Packet(Packet.COMMAND_PLAYER_REMOVE,
                 new Integer(player.getId())));
        }

        // make sure the game advances
        if ( game.phaseHasTurns(game.getPhase()) && null != game.getTurn() ) {
            if ( game.getTurn().isValid( player.getId(), game ) ) {
                sendGhostSkipMessage( player );
            }
        } else {
            checkReady();
        }

        // notify other players
        sendServerChat(player.getName() + " disconnected.");

        // log it
        System.out.println("s: removed player " + player.getName());

        // Reset the game after Elvis has left the building.
        if ( 0 == game.getNoOfPlayers() ) {
            resetGame();
        }
    }

    /**
     * Checks each player to see if he has no entities, and if true, sets the
     * observer flag for that player.  An exception is that there are no
     * observers during the lounge phase.
     */
    public void checkForObservers() {
        for (Enumeration e = game.getPlayers(); e.hasMoreElements(); ) {
            Player p = (Player)e.nextElement();
            p.setObserver(game.getEntitiesOwnedBy(p) < 1 &&
                          game.getPhase() != Game.PHASE_LOUNGE);
        }
    }

    /**
     * Reset the game back to the lounge.
     *
     * TODO: couldn't this be a hazard if there are other things executing at
     *  the same time?
     */
    public void resetGame() {
        // remove all entities
        game.reset();
        send(createEntitiesPacket());
        send(new Packet(Packet.COMMAND_SENDING_MINEFIELDS, new Vector()));

        //TODO: remove ghosts

        // reset all players
        resetPlayersDone();
        transmitAllPlayerDones();

        // Write end of game to stdout so controlling scripts can rotate logs.
        SimpleDateFormat format = new SimpleDateFormat
            ( "yyyy-MM-dd HH:mm:ss z" );
        System.out.print( format.format(new Date()) );
        System.out.println( " END OF GAME" );

        changePhase(Game.PHASE_LOUNGE);
    }

    public void autoSave()
    {
        saveGame("autosave",game.getOptions().booleanOption("autosave_msg"));
    }

    public void saveGame(String sFile, boolean sendChat) {
        String sFinalFile = sFile;
        if (!sFinalFile.endsWith(".sav")) {
            sFinalFile = sFile + ".sav";
        }
        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(sFinalFile));
            oos.writeObject(game);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            System.err.println("Unable to save file: " + sFinalFile);
            e.printStackTrace();
        }

        if (sendChat) sendChat("MegaMek", "Game saved to " + sFinalFile);
    }

    public void saveGame(String sFile) {
        saveGame(sFile,true);
    }

    public boolean loadGame(File f) {
        System.out.println("s: loading saved game file '"+f+"'");
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(f));
            game = (Game)ois.readObject();
            ois.close();
        } catch (Exception e) {
            System.err.println("Unable to load file: " + f);
            e.printStackTrace();
            return false;
        }

        // a bit redundant, but there's some initialization code there
        setGame(game);

        return true;
    }

    /**
     * Shortcut to game.getPlayer(id)
     */
    public Player getPlayer(int id) {
        return game.getPlayer(id);
    }

    /**
     * Removes all entities owned by a player.  Should only be called when it
     * won't cause trouble (the lounge, for instance, or between phases.)
     */
    private void removeAllEntitesOwnedBy(Player player) {
        Vector toRemove = new Vector();

        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            if (entity.getOwner().equals(player)) {
                toRemove.addElement(entity);
            }
        }

        for (Enumeration e = toRemove.elements(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();
            int id = entity.getId();
            game.removeEntity(id, Entity.REMOVE_NEVER_JOINED);
            send(createRemoveEntityPacket(id, Entity.REMOVE_NEVER_JOINED));
        }
    }

    /**
     * a shorter name for getConnection()
     */
    private Connection getClient(int connId) {
        return getConnection(connId);
    }

    /**
     * Returns a connection, indexed by id
     */
    public Enumeration getConnections() {
        return connections.elements();
    }

    /**
     * Returns a connection, indexed by id
     */
    public Connection getConnection(int connId) {
        return (Connection)connectionIds.get(new Integer(connId));
    }

    /**
     * Returns a pending connection
     */
    private Connection getPendingConnection(int connId) {
        for (Enumeration i=connectionsPending.elements();i.hasMoreElements();){
            final Connection conn = (Connection)i.nextElement();

            if (conn.getId() == connId) {
                return conn;
            }
        }
        return null;
    }

    /**
     * Called at the beginning of each game round to reset values on this
     * entity that are reset every round
     */
    private void resetEntityRound() {
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity entity = (Entity)e.nextElement();

            entity.newRound(game.getRoundCount());
        }
    }

    /**
     * Called at the beginning of each phase.  Sets and resets
     * any entity parameters that need to be reset.
     */
    private void resetEntityPhase(int phase) {
        // first, mark doomed entities as destroyed and flag them
        Vector toRemove = new Vector(0, 10);
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            if (entity.crew.isDoomed()) {
                entity.crew.setDead(true);
                entity.setDestroyed(true);
            }

            if (entity.isDoomed()) {
                entity.setDestroyed(true);

                // Is this unit swarming somebody?
                final int swarmedId = entity.getSwarmTargetId();
                if ( Entity.NONE != swarmedId ) {
                    final Entity swarmed = game.getEntity( swarmedId );
                    swarmed.setSwarmAttackerId( Entity.NONE );
                    entity.setSwarmTargetId( Entity.NONE );
                    phaseReport.append( swarmed.getDisplayName() );
                    phaseReport.append( " is freed from its swarm attack.\n" );
                    this.entityUpdate( swarmedId );
                }
            }

            if (entity.isDestroyed() || entity.getCrew().isDead()) {
                toRemove.addElement(entity);
            }
        }

        // actually remove all flagged entities
        for (Enumeration e = toRemove.elements(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();
            int condition = Entity.REMOVE_SALVAGEABLE;
            if ( !entity.isSalvage() ) {
                condition = Entity.REMOVE_DEVASTATED;
            }

            game.removeEntity(entity.getId(), condition);
            send( createRemoveEntityPacket(entity.getId(), condition) );
        }

        // do some housekeeping on all the remaining
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            entity.applyDamage();

            entity.reloadEmptyWeapons();

            // reset damage this phase
            entity.damageThisPhase = 0;
            entity.engineHitsThisRound = 0;
            entity.rolledForEngineExplosion = false;
            entity.dodging = false;

            // reset done to false

            if ( phase == Game.PHASE_DEPLOYMENT ) {
              entity.setDone(!entity.shouldDeploy(game.getRoundCount()));
            } else {
              entity.setDone(false);
            }
        }
    }

    /**
     * Called at the beginning of certain phases to make
     * every player not ready.
     */
    private void resetPlayersDone() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            player.setDone(false);
        }
    }

    /**
     * Called at the beginning of certain phases to make
     * every active player not ready.
     */
    private void resetActivePlayersDone() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            player.setDone(game.getEntitiesOwnedBy(player) <= 0);

        }
        transmitAllPlayerDones();
    }

    /**
     * Writes the victory report
     */
    private void prepareVictoryReport() {
        game.resetRoundReport();
        roundReport = game.getRoundReport(); //HACK

        roundReport.append("\nVictory!\n-------------------\n\n");

        roundReport.append("Winner is: ");
        if (game.getVictoryTeam() == Player.TEAM_NONE) {
            Player player = getPlayer( game.getVictoryPlayerId() );
            if ( null == player ) {
                roundReport.append( "the Chicago Cubs!!!\n\n" );
            } else {
                roundReport.append( player.getName() );
                roundReport.append("\n\n");
            }
        } else {
            roundReport.append("TEAM #").append(game.getVictoryTeam());
            roundReport.append("\n\n");
        }

        Enumeration survivors = game.getEntities();
        if ( survivors.hasMoreElements() ) {
            roundReport.append("Survivors are:\n");
            while ( survivors.hasMoreElements() ) {
                Entity entity = (Entity) survivors.nextElement();

                if ( !entity.isDeployed() )
                  continue;

                roundReport.append(entity.victoryReport());
                roundReport.append('\n');
            }
        }
        Enumeration undeployed = game.getEntities();
        if ( undeployed.hasMoreElements() ) {
            boolean wroteHeader = false;

            while ( undeployed.hasMoreElements() ) {
                Entity entity = (Entity) undeployed.nextElement();

                if ( entity.isDeployed() )
                  continue;

                if ( !wroteHeader ) {
                  roundReport.append("The following units never entered the "
                                     + "field of battle:\n");
                  wroteHeader = true;
                }

                roundReport.append(entity.victoryReport());
                roundReport.append('\n');
            }
        }
        Enumeration retreat = game.getRetreatedEntities();
        if ( retreat.hasMoreElements() ) {
            roundReport.append("\nThe following units are in retreat:\n");
            while ( retreat.hasMoreElements() ) {
                Entity entity = (Entity) retreat.nextElement();
                roundReport.append(entity.victoryReport());
                roundReport.append('\n');
            }
        }
        Enumeration graveyard = game.getGraveyardEntities();
        if ( graveyard.hasMoreElements() ) {
            roundReport.append("\nGraveyard contains:\n");
            while ( graveyard.hasMoreElements() ) {
                Entity entity = (Entity) graveyard.nextElement();
                roundReport.append(entity.victoryReport());
                roundReport.append('\n');
            }
        }
        Enumeration devastated = game.getDevastatedEntities();
        if ( devastated.hasMoreElements() ) {
            roundReport.append("\nThe following utterly destroyed units are "
                               + "not available for salvage:\n");
            while ( devastated.hasMoreElements() ) {
                Entity entity = (Entity) devastated.nextElement();
                roundReport.append(entity.victoryReport());
                roundReport.append('\n');
            }
        }
        roundReport.append("\nDetailed unit status saved to "
                           + "entitystatus.txt\n");
    }

    /**
     * Generates a detailed report for campaign use
     */
    private String getDetailedVictoryReport() {
        StringBuffer sb = new StringBuffer();

        Vector vAllUnits = new Vector();
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            vAllUnits.addElement(i.nextElement());
        }

        for ( Enumeration i = game.getRetreatedEntities();
              i.hasMoreElements(); ) {
            vAllUnits.addElement(i.nextElement());
        }

        for ( Enumeration i = game.getGraveyardEntities();
              i.hasMoreElements(); ) {
            vAllUnits.addElement(i.nextElement());
        }

        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {

            // Record the player.
            Player p = (Player)i.nextElement();
            sb.append("++++++++++ " )
                .append( p.getName() )
                .append( " ++++++++++");
            sb.append( CommonConstants.NL );

            // Record the player's alive, retreated, or salvageable units.
            for (int x = 0; x < vAllUnits.size(); x++) {
                Entity e = (Entity)vAllUnits.elementAt(x);
                if (e.getOwner() == p) {
                    sb.append(UnitStatusFormatter.format(e));
                }
            }

            // Record the player's devastated units.
            Enumeration devastated = game.getDevastatedEntities();
            if ( devastated.hasMoreElements() ) {
                sb.append("=============================================================");
                sb.append( CommonConstants.NL );
                sb.append("The following utterly destroyed units are not available for salvage:");
                sb.append( CommonConstants.NL );
                while ( devastated.hasMoreElements() ) {
                    Entity e = (Entity) devastated.nextElement();
                    if (e.getOwner() == p) {
                        sb.append( e.getShortName() )
                            .append( ", Pilot: " )
                            .append( e.getCrew().getName() )
                            .append( " (" )
                            .append( e.getCrew().getGunnery() )
                            .append( "/" )
                            .append( e.getCrew().getPiloting() )
                            .append( ")" );
                        sb.append( CommonConstants.NL );
                    }
                } // Handle the next unsalvageable unit for the player
                sb.append("=============================================================");
                sb.append( CommonConstants.NL );
            }

        } // Handle the next player

        return sb.toString();
    }

    /**
     * Forces victory for the specified player, or his/her team at the end of
     * the round.
     */
    public void forceVictory(Player victor) {
        game.setForceVictory(true);
        if (victor.getTeam() == Player.TEAM_NONE) {
            game.setVictoryPlayerId(victor.getId());
            game.setVictoryTeam(Player.TEAM_NONE);
        } else {
            game.setVictoryPlayerId(Player.PLAYER_NONE);
            game.setVictoryTeam(victor.getTeam());
        }

        Vector players = game.getPlayersVector();
        for (int i = 0; i < players.size(); i++) {
            Player player = (Player) players.elementAt(i);
            player.setAdmitsDefeat(false);
        }
    }

    /** Cancels the force victory */
    public void cancelVictory() {
        game.setForceVictory(false);
        game.setVictoryPlayerId(Player.PLAYER_NONE);
        game.setVictoryTeam(Player.TEAM_NONE);
    }

    /**
     * Called when a player declares that he is "done."  Checks to see if all
     * players are done, and if so, moves on to the next phase.
     */
    private void checkReady() {
        // check if all active players are done
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if (!player.isGhost() && !player.isObserver() && !player.isDone()) {
                return;
            }
        }

        // Tactical Genius pilot special ability (lvl 3)
        if (game.getInitiativeRerollRequests().size() > 0) {
            resetActivePlayersDone();
            TurnOrdered.rollInitAndResolveTies(game.getTeamsVector(), game.getInitiativeRerollRequests());

            determineTurnOrder(Game.PHASE_INITIATIVE);
            writeInitiativeReport(true);
            send(createReportPacket());
            game.getInitiativeRerollRequests().removeAllElements();
            return;  // don't end the phase yet, players need to see new report
        }

        // need at least one entity in the game for the lounge phase to end
        if (!game.phaseHasTurns(game.getPhase())
        && (game.getPhase() != Game.PHASE_LOUNGE || game.getNoOfEntities() > 0)) {
            endCurrentPhase();
        }
    }

    /**
     * Called when the current player has done his current turn and the turn
     * counter needs to be advanced.
     * Also enforces the "protos_move_multi" and the "protos_move_multi"
     * option.  If the player has just moved infantry/protos with a "normal"
     * turn, adds up to Game.INF_AND_PROTOS_MOVE_MULTI - 1 more
     * infantry/proto-specific turns after the current turn.
     */
    private void endCurrentTurn(Entity entityUsed) {

        // Enforce "inf_move_multi" and "protos_move_multi" options.
        // The "isNormalTurn" flag is checking to see if any non-Infantry
        // or non-Protomech units can move during the current turn.
        boolean turnsChanged = false;
        GameTurn turn = game.getTurn();
        final int playerId = (null == entityUsed) ?
            Player.PLAYER_NONE : entityUsed.getOwnerId();
        boolean infMoved = entityUsed instanceof Infantry;
        boolean infMoveMulti =
            game.getOptions().booleanOption("inf_move_multi");
        boolean protosMoved = entityUsed instanceof Protomech;
        boolean protosMoveMulti =
            game.getOptions().booleanOption("protos_move_multi");

        // If infantry or protos move multi see if any
        // other unit types can move in the current turn.
        int multiMask = 0;
        if ( infMoveMulti ) {
            multiMask += GameTurn.CLASS_INFANTRY;
        }
        if ( protosMoveMulti ) {
            multiMask += GameTurn.CLASS_PROTOMECH;
        }

        // If a proto declared fire and protos don't move
        // multi, ignore whether infantry move or not.
        else if ( protosMoved && game.getPhase() == Game.PHASE_FIRING ) {
            multiMask = 0;
        }

        // Is this a general move turn?
        boolean isGeneralMoveTurn =
            ( !(turn instanceof GameTurn.SpecificEntityTurn) &&
              !(turn instanceof GameTurn.UnitNumberTurn) &&
              !(turn instanceof GameTurn.UnloadStrandedTurn) &&
              ( !(turn instanceof GameTurn.EntityClassTurn) ||
                ( (turn instanceof GameTurn.EntityClassTurn) &&
                  ( (GameTurn.EntityClassTurn) turn ).isValidClass(~multiMask)
                  )
                )
              );

        // Unless overridden by the "protos_move_multi" option, all Protomechs
        // in a unit declare fire, and they don't mix with infantry.
        if ( protosMoved && !protosMoveMulti && isGeneralMoveTurn &&
             game.getPhase() == Game.PHASE_FIRING ) {

            // What's the unit number and ID of the entity used?
            final char movingUnit = entityUsed.getUnitNumber();
            final int movingId = entityUsed.getId();

            // How many other Protomechs are in the unit that can fire?
            int protoTurns = game.getSelectedEntityCount
                ( new EntitySelector() {
                        private final int ownerId = playerId;
                        private final int entityId = movingId;
                        private final char unitNum = movingUnit;
                        public boolean accept( Entity entity ) {
                            if ( entity instanceof Protomech &&
                                 entity.isSelectableThisTurn() &&
                                 ownerId == entity.getOwnerId() &&
                                 entityId != entity.getId() &&
                                 unitNum == entity.getUnitNumber() )
                                return true;
                            return false;
                        }
                    } );

            // Add the correct number of turns for the Protomech unit number.
            for (int i = 0; i < protoTurns; i++) {
                GameTurn newTurn = new GameTurn.UnitNumberTurn
                    ( playerId, movingUnit );
                game.insertNextTurn(newTurn);
                turnsChanged = true;
            }
        }

        // Otherwise, we may need to add turns for the "*_move_multi" options.
        else if ( ( (infMoved && infMoveMulti) ||
                    (protosMoved && protosMoveMulti) ) &&
                  isGeneralMoveTurn ) {
            int remaining = 0;

            // Calculate the number of EntityClassTurns need to be added.
            if ( infMoveMulti ) {
                remaining += game.getInfantryLeft(playerId);
            }
            if ( protosMoveMulti ) {
                remaining += game.getProtomechsLeft(playerId);
            }
            int moreInfAndProtoTurns =
                Math.min(game.getOptions().intOption("inf_proto_move_multi") - 1, remaining);

            // Add the correct number of turns for the right unit classes.
            for (int i = 0; i < moreInfAndProtoTurns; i++) {
                GameTurn newTurn =
                    new GameTurn.EntityClassTurn( playerId, multiMask );
                game.insertNextTurn(newTurn);
                turnsChanged = true;
            }
        }
        // brief everybody on the turn update, if they changed
        if (turnsChanged) {
            send(createTurnVectorPacket());
        }

        // move along
        changeToNextTurn();
    }

    /**
     * Changes the current phase, does some bookkeeping and
     * then tells the players.
     */
    private void changePhase(int phase) {
        game.setLastPhase(game.getPhase());
        game.setPhase(phase);

        // prepare for the phase
        prepareForPhase(phase);

        if (isPhasePlayable(phase)) {
            // tell the players about the new phase
            send(new Packet(Packet.COMMAND_PHASE_CHANGE, new Integer(phase)));

            // post phase change stuff
            executePhase(phase);
        } else {
            endCurrentPhase();
        }
    }

    /**
     * Prepares for, presumably, the next phase.  This typically involves
     * resetting the states of entities in the game and making sure the client
     * has the information it needs for the new phase.
     */
    private void prepareForPhase(int phase) {
        switch (phase) {
            case Game.PHASE_LOUNGE :
                mapSettings.setBoardsAvailableVector(scanForBoards(mapSettings.getBoardWidth(), mapSettings.getBoardHeight()));
                mapSettings.setNullBoards(DEFAULT_BOARD);
                send(createMapSettingsPacket());
                break;
            case Game.PHASE_INITIATIVE :
                // remove the last traces of last round
                game.resetActions();
                game.resetRoundReport();
                roundReport = game.getRoundReport(); //HACK
                resetEntityRound();
                resetEntityPhase(phase);
                checkForObservers();
                // roll 'em
                resetActivePlayersDone();
                rollInitiative();

                if ( !game.shouldDeployThisRound() )
                  incrementAndSendGameRound();

                //setIneligible(phase);
                determineTurnOrder(phase);
                writeInitiativeReport(false);
                doTryUnstuck();
                send(createReportPacket());
                autoSave();
                System.out.println("Round " + game.getRoundCount() + " memory usage: " + MegaMek.getMemoryUsed());
                break;
            case Game.PHASE_DEPLOY_MINEFIELDS :
                checkForObservers();
                resetActivePlayersDone();
                setIneligible(phase);

                Enumeration e = game.getPlayers();
                Vector turns = new Vector();
                while (e.hasMoreElements()) {
                    Player p = (Player) e.nextElement();
                    if (p.hasMinefields()) {
                        GameTurn gt = new GameTurn(p.getId());
                        turns.addElement(gt);
                    }
                }
                game.setTurnVector(turns);
                game.resetTurnIndex();

                // send turns to all players
                send(createTurnVectorPacket());
                break;
            case Game.PHASE_SET_ARTYAUTOHITHEXES :
                // place off board entities actually off-board
                Enumeration i = game.getEntities();
                while (i.hasMoreElements()) {
                    Entity en = (Entity) i.nextElement();
                    en.deployOffBoard();
                }
                checkForObservers();
                resetActivePlayersDone();
                setIneligible(phase);

                i = game.getPlayers();
                Vector turn = new Vector();

                // Walk through the players of the game, and add
                // a turn for all players with artillery weapons.
                while (i.hasMoreElements()) {

                    // Get the next player.
                    final Player p = (Player) i.nextElement();

                    // Does the player have any artillery-equipped units?
                    EntitySelector playerArtySelector = new EntitySelector() {
                            private Player owner = p;
                            public boolean accept (Entity entity) {
                                if ( owner.equals( entity.getOwner() ) &&
                                     isEligibleForTargetingPhase( entity ) )
                                    return true;
                                return false;
                            }
                        };
                    if ( game.getSelectedEntities( playerArtySelector )
                         .hasMoreElements() ) {

                        // Yes, the player has arty-equipped units.
                        GameTurn gt = new GameTurn(p.getId());
                        turn.addElement(gt);
                    }
                }
                game.setTurnVector(turn);
                game.resetTurnIndex();

                // send turns to all players
                send(createTurnVectorPacket());
                break;
            case Game.PHASE_MOVEMENT :
                roundReport.append("\nMovement Phase\n-------------------\n");
            case Game.PHASE_DEPLOYMENT :
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
            case Game.PHASE_TARGETING:
            case Game.PHASE_OFFBOARD:
                resetEntityPhase(phase);
                checkForObservers();
                setIneligible(phase);
                determineTurnOrder(phase);
                resetActivePlayersDone();
                //send(createEntitiesPacket());
                entityAllUpdate();
                game.resetPhaseReport();
                phaseReport = game.getPhaseReport(); //HACK
                break;
            case Game.PHASE_END :
                resetEntityPhase(phase);
                game.resetPhaseReport();
                phaseReport = game.getPhaseReport(); //HACK
                resolveHeat();
                phaseReport.append("\n\nEnd Phase\n------------------------\n");
                checkForSuffocation();
                resolveFire();
                resolveExtremeTempInfantryDeath();
                resolveAmmoDumps();
                resolveCrewDamage();
                resolveCrewWakeUp();
                resolveMechWarriorPickUp();
                resolveVeeINarcPodRemoval();
                checkForObservers();
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                }
                log.append( "\n" );
                log.append( roundReport.toString() );
            case Game.PHASE_MOVEMENT_REPORT :
            case Game.PHASE_FIRING_REPORT :
            case Game.PHASE_OFFBOARD_REPORT :
                resetActivePlayersDone();
                send(createReportPacket());
                if (game.getOptions().booleanOption("paranoid_autosave")) autoSave();
                break;
            case Game.PHASE_VICTORY :
                prepareVictoryReport();
                log.append( "\n" );
                log.append( roundReport.toString() );
                send(createFullEntitiesPacket());
                send(createReportPacket());
                send(createEndOfGamePacket());
                break;
        }
    }

    /**
     * Should we play this phase or skip it?  The only phases we'll skip
     * are the firing or the physical phase if no entities are eligible.
     */
    private boolean isPhasePlayable(int phase) {
        switch (phase) {
            case Game.PHASE_SET_ARTYAUTOHITHEXES :
            case Game.PHASE_DEPLOY_MINEFIELDS :
            case Game.PHASE_DEPLOYMENT :
            case Game.PHASE_MOVEMENT :
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
            case Game.PHASE_TARGETING:
            case Game.PHASE_OFFBOARD :
                return game.hasMoreTurns();
            default :
                return true;
        }
    }

    /**
     * Do anything we seed to start the new phase, such as give a turn to
     * the first player to play.
     */
    private void executePhase(int phase) {
        switch (phase) {
            case Game.PHASE_EXCHANGE :
                resetPlayersDone();
                // Build teams vector
                setupTeams(game);
                applyBoardSettings();
                game.setupRoundDeployment();
                game.determineWind();
                // If we add transporters for any Magnetic Clamp
                // equiped squads, then update the clients' entities.
                if ( game.checkForMagneticClamp() ) {
                    send(createEntitiesPacket());
                }
                // transmit the board to everybody
                send(createBoardPacket());
                break;
            case Game.PHASE_SET_ARTYAUTOHITHEXES :
            case Game.PHASE_DEPLOY_MINEFIELDS :
            case Game.PHASE_DEPLOYMENT :
            case Game.PHASE_MOVEMENT :
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
            case Game.PHASE_TARGETING :
                changeToNextTurn();
                if (game.getOptions().booleanOption("paranoid_autosave")) autoSave();
                break;
        }
    }

    /**
     * Ends this phase and moves on to the next.
     */
    private void endCurrentPhase() {
        switch (game.getPhase()) {
            case Game.PHASE_LOUNGE :
                changePhase(Game.PHASE_EXCHANGE);
                break;
            case Game.PHASE_EXCHANGE :
                changePhase(Game.PHASE_SET_ARTYAUTOHITHEXES);
                break;
            case Game.PHASE_STARTING_SCENARIO :
                changePhase(Game.PHASE_SET_ARTYAUTOHITHEXES);
                break;
            case Game.PHASE_SET_ARTYAUTOHITHEXES :
                Enumeration e = game.getPlayers();
                boolean mines = false;
                while (e.hasMoreElements()) {
                    Player p = (Player) e.nextElement();
                    if (p.hasMinefields()) {
                         mines = true;
                    }
                }
                if (mines) {
                    changePhase(Game.PHASE_DEPLOY_MINEFIELDS);
                } else {
                    changePhase(Game.PHASE_INITIATIVE);
                }
                break;
            case Game.PHASE_DEPLOY_MINEFIELDS :
                changePhase(Game.PHASE_INITIATIVE);
                break;
            case Game.PHASE_DEPLOYMENT :
                game.clearDeploymentThisRound();
                game.checkForCompleteDeployment();
                changePhase(Game.PHASE_INITIATIVE);
                break;
            case Game.PHASE_INITIATIVE :
                boolean doDeploy = game.shouldDeployThisRound() && (game.getLastPhase() != Game.PHASE_DEPLOYMENT);

                if ( doDeploy ) {
                  changePhase(Game.PHASE_DEPLOYMENT);
                } else {
                  changePhase(Game.PHASE_TARGETING);
                }
                break;
            case Game.PHASE_MOVEMENT :
                addMovementHeat();
                applyBuildingDamage();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls(); // Skids cause damage in movement phase
                resolveCrewDamage(); // again, I guess
                checkForFlamingDeath();
                // check phase report
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                    changePhase(Game.PHASE_MOVEMENT_REPORT);
                } else {
                    roundReport.append("<nothing>\n");
                    changePhase(Game.PHASE_OFFBOARD);
                }
                break;
            case Game.PHASE_MOVEMENT_REPORT :
                changePhase(Game.PHASE_OFFBOARD);
                break;
            case Game.PHASE_FIRING :
                resolveAllButWeaponAttacks();
                assignAMS();
                resolveOnlyWeaponAttacks();
                applyBuildingDamage();                
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                // check phase report
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                    changePhase(Game.PHASE_FIRING_REPORT);
                } else {
                    roundReport.append("<nothing>\n");
                    changePhase(Game.PHASE_PHYSICAL);
                }
                break;
            case Game.PHASE_FIRING_REPORT :
                changePhase(Game.PHASE_PHYSICAL);
                break;
            case Game.PHASE_PHYSICAL :
                resolvePhysicalAttacks();
                applyBuildingDamage();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                resolveSinkVees();
                // check phase report
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                } else {
                    roundReport.append("<nothing>\n");
                }
                changePhase(Game.PHASE_END);
                break;
            case Game.PHASE_TARGETING :
                enqueueIndirectArtilleryAttacks();
                changePhase(Game.PHASE_MOVEMENT);
                break;
            case Game.PHASE_OFFBOARD :
                roundReport.append("\nOffboard Attack Phase\n-----------------\n");
                resolveIndirectArtilleryAttacks();
                applyBuildingDamage();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                    changePhase(Game.PHASE_OFFBOARD_REPORT);
                } else {
                    roundReport.append("<nothing>\n");
                    changePhase(Game.PHASE_FIRING);
                }
                break;
            case Game.PHASE_OFFBOARD_REPORT:
                changePhase(Game.PHASE_FIRING);
                break;
            case Game.PHASE_END :
                if (victory()) {
                    changePhase(Game.PHASE_VICTORY);
                } else {
                  changePhase(Game.PHASE_INITIATIVE);
                }
                break;
            case Game.PHASE_VICTORY :
                resetGame();
                break;
        }
    }

    /**
     * Increment's the server's game round and send it to all the clients
     */
    private void incrementAndSendGameRound() {
      game.incrementRoundCount();
      send(new Packet(Packet.COMMAND_ROUND_UPDATE, new Integer(game.getRoundCount())));
    }

    /**
     * Tries to change to the next turn.  If there are no more turns, ends the
     * current phase.  If the player whose turn it is next is not connected,
     * we allow the other players to skip that player.
     */
    private void changeToNextTurn() {
        // if there aren't any more turns, end the phase
        if (!game.hasMoreTurns()) {
            endCurrentPhase();
            return;
        }

        // okay, well next turn then!
        GameTurn nextTurn = game.changeToNextTurn();
        send(createTurnIndexPacket());

        Player player = getPlayer( nextTurn.getPlayerNum() );
        if ( null != player && player.isGhost() ) {
            sendGhostSkipMessage( player );
        }
        else if ( null == game.getFirstEntity()
                  && null != player
                  && ((game.getPhase() != Game.PHASE_DEPLOY_MINEFIELDS) && (game.getPhase() != Game.PHASE_SET_ARTYAUTOHITHEXES))) {
            sendTurnErrorSkipMessage( player );
        }
    }

    /**
     * Sends out a notification message indicating that a ghost player may
     * be skipped.
     *
     * @param   ghost - the <code>Player</code> who is ghosted.
     *          This value must not be <code>null</code>.
     */
    private void sendGhostSkipMessage( Player ghost ) {
        StringBuffer message = new StringBuffer();
        message.append( "Player '" )
            .append( ghost.getName() )
            .append( "' is disconnected.  You may skip his/her current turn with the /skip command." );
        sendServerChat( message.toString() );
    }

    /**
     * Sends out a notification message indicating that the current turn is an
     * error and should be skipped.
     *
     * @param   skip - the <code>Player</code> who is to be skipped.
     *          This value must not be <code>null</code>.
     */
    private void sendTurnErrorSkipMessage( Player skip ) {
        StringBuffer message = new StringBuffer();
        message.append( "Player '" )
            .append( skip.getName() )
            .append( "' has no units to move.  You should skip his/her/your current turn with the /skip command. You may want to report this error.  See the MegaMek homepage (http://megamek.sf.net/) for details." );
        sendServerChat( message.toString() );
    }

    /**
     * Skips the current turn.  This only makes sense in phases that have turns.
     * Operates by finding an entity to move and then doing nothing with it.
     */
    public void skipCurrentTurn() {
        // find an entity to skip...
        Entity toSkip = game.getFirstEntity();

        switch (game.getPhase()) {
            case Game.PHASE_DEPLOYMENT :
                sendServerChat("Turns cannot be skipped in the deployment phase.");
                break;
            case Game.PHASE_MOVEMENT :
                if ( toSkip != null ) {
                    processMovement(toSkip, new MovePath(game, toSkip));
                }
                endCurrentTurn(toSkip);
                break;
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
            case Game.PHASE_TARGETING :
            case Game.PHASE_OFFBOARD :
                if ( toSkip != null ) {
                    processAttack(toSkip, new Vector(0));
                }
                endCurrentTurn(toSkip);
                break;
            default :

        }
    }

    /**
     * Returns true if the current turn may be skipped.  Ghost players' turns
     * are skippable, and a turn should be skipped if there's nothing to move.
     */
    public boolean isTurnSkippable() {
        GameTurn turn = game.getTurn();
        if (null == turn) return false;
        Player player = getPlayer( turn.getPlayerNum() );
        return ( null == player || player.isGhost()
                 || game.getFirstEntity() == null );
    }

    /**
     * Returns true if victory conditions have been met.  Victory conditions
     * are when there is only one player left with mechs or only one team.
     */
    public boolean victory() {
        if (game.isForceVictory()) {
            int victoryPlayerId = game.getVictoryPlayerId();
            int victoryTeam = game.getVictoryTeam();
            Vector players = game.getPlayersVector();
            boolean forceVictory = true;

            // Individual victory.
            if (victoryPlayerId != Player.PLAYER_NONE) {
                for (int i = 0; i < players.size(); i++) {
                    Player player = (Player) players.elementAt(i);

                    if (player.getId() != victoryPlayerId && !player.isObserver()) {
                        if (!player.admitsDefeat()) {
                            forceVictory = false;
                            break;
                        }
                    }
                }
            }
            // Team victory.
            if (victoryTeam != Player.TEAM_NONE) {
                for (int i = 0; i < players.size(); i++) {
                    Player player = (Player) players.elementAt(i);

                    if (player.getTeam() != victoryTeam && !player.isObserver()) {
                        if (!player.admitsDefeat()) {
                            forceVictory = false;
                            break;
                        }
                    }
                }
            }

            for (int i = 0; i < players.size(); i++) {
                Player player = (Player) players.elementAt(i);
                player.setAdmitsDefeat(false);
            }

            if (forceVictory) {
            return true;
            }
            cancelVictory();
        }

        if (!game.getOptions().booleanOption("check_victory")) {
            return false;
        }

        // check all players/teams for aliveness
        int playersAlive = 0;
        Player lastPlayer = null;
        boolean oneTeamAlive = false;
        int lastTeam = Player.TEAM_NONE;
        boolean unteamedAlive = false;
        for (Enumeration e = game.getPlayers(); e.hasMoreElements();) {
            Player player = (Player)e.nextElement();
            int team = player.getTeam();
            if (game.getLiveDeployedEntitiesOwnedBy(player) <= 0) {
                continue;
            }
            // we found a live one!
            playersAlive++;
            lastPlayer = player;
            // check team
            if (team == Player.TEAM_NONE) {
                unteamedAlive = true;
            } else if (lastTeam == Player.TEAM_NONE) {
                // possibly only one team alive
                oneTeamAlive = true;
                lastTeam = team;
            } else if (team != lastTeam) {
                // more than one team alive
                oneTeamAlive = false;
                lastTeam = team;
            }
        }

        // check if there's one player alive
        if (playersAlive < 1) {
            game.setVictoryPlayerId( Player.PLAYER_NONE );
            game.setVictoryTeam( Player.TEAM_NONE );
            return true;
        }
        else if ( playersAlive == 1 ) {
            if (lastPlayer.getTeam() == Player.TEAM_NONE) {
                // individual victory
                game.setVictoryPlayerId(lastPlayer.getId());
                game.setVictoryTeam(Player.TEAM_NONE);
                return true;
            }
        }

        // did we only find one live team?
        if (oneTeamAlive && !unteamedAlive) {
            // team victory
            game.setVictoryPlayerId(Player.PLAYER_NONE);
            game.setVictoryTeam(lastTeam);
            return true;
        }

        return false;
    }

    /**
     * Applies board settings.  This loads and combines all the boards that
     * were specified into one mega-board and sets that board as current.
     */
    public void applyBoardSettings() {
        mapSettings.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
        mapSettings.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        IBoard[] sheetBoards = new Board[mapSettings.getMapWidth() * mapSettings.getMapHeight()];
        for (int i = 0; i < mapSettings.getMapWidth() * mapSettings.getMapHeight(); i++) {
            sheetBoards[i] = new Board();
            String name = (String)mapSettings.getBoardsSelectedVector().elementAt(i);
            boolean isRotated = false;
            if ( name.startsWith( Board.BOARD_REQUEST_ROTATION ) ) {
                isRotated = true;
                name = name.substring( Board.BOARD_REQUEST_ROTATION.length() );
            }
            if (name.startsWith(MapSettings.BOARD_GENERATED)) {
                sheetBoards[i] = BoardUtilities.generateRandom(mapSettings);
            } else {
                sheetBoards[i].load( name + ".board");
                BoardUtilities.flip(sheetBoards[i], isRotated, isRotated );
            }
        }
        IBoard newBoard = BoardUtilities.combine(mapSettings.getBoardWidth(), mapSettings.getBoardHeight(),
                mapSettings.getMapWidth(), mapSettings.getMapHeight(), sheetBoards);
        game.setBoard(newBoard);
    }

    /**
       Set up the teams vector.  Each player on a team (Team 1 .. Team X) is
       placed in the appropriate vector.  Any player on 'No Team', is placed
       in their own object
    */
    static void setupTeams(Game game)
    {
        Vector teams = game.getTeamsVector();
        boolean useTeamInit =
            game.getOptions().getOption("team_initiative").booleanValue();

        // This is a reference to THE team vector,
        // so we need to clear it before use.
        teams.removeAllElements();

        // Get all NO_TEAM players.  If team_initiative is false, all
        // players are on their own teams for initiative purposes.
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if ( !useTeamInit || player.getTeam() == Player.TEAM_NONE ) {
                Team new_team = new Team(Player.TEAM_NONE);
                new_team.addPlayer(player);
                teams.addElement(new_team);
            }
        }

        // If useTeamInit is false, all players have been placed
        if (!useTeamInit) {
            return;
        }

        // Now, go through all the teams, and add the apropriate player
        for (int t = Player.TEAM_NONE + 1; t < Player.MAX_TEAMS; t++) {
            Team new_team = null;
            for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
                final Player player = (Player)i.nextElement();
                if (player.getTeam() == t) {
                    if (new_team == null) {
                        new_team = new Team(t);
                    }
                    new_team.addPlayer(player);
                }
            }

            if (new_team != null) {
                teams.addElement(new_team);
            }
        }
    }

    /**
     * Rolls initiative for all the players.
     */
    private void rollInitiative() {
        // Roll for initative on the teams.
        TurnOrdered.rollInitiative(game.getTeamsVector());

        transmitAllPlayerUpdates();
    }

    private void determineTurnOrder(int phase) {

        // Determine whether infantry and/or Protomechs move
        // and/or deploy even according to game options.
        boolean infMoveEven =
            ( game.getOptions().booleanOption("inf_move_even") &&
              (game.getPhase() == Game.PHASE_INITIATIVE ||
               game.getPhase() == Game.PHASE_MOVEMENT) ) ||
            ( game.getOptions().booleanOption("inf_deploy_even") &&
              game.getPhase() == Game.PHASE_DEPLOYMENT );
        boolean infMoveMulti =
            game.getOptions().booleanOption("inf_move_multi");
        boolean protosMoveEven =
            ( game.getOptions().booleanOption("protos_move_even") &&
              (game.getPhase() == Game.PHASE_INITIATIVE ||
               game.getPhase() == Game.PHASE_MOVEMENT) ) ||
            ( game.getOptions().booleanOption("protos_deploy_even") &&
              game.getPhase() == Game.PHASE_DEPLOYMENT );
        boolean protosMoveMulti =
            game.getOptions().booleanOption("protos_move_multi");
        boolean protosFireMulti = !protosMoveMulti &&
            game.getPhase() == Game.PHASE_FIRING;
        int evenMask = 0;
        if ( infMoveEven ) evenMask += GameTurn.CLASS_INFANTRY;
        if ( protosMoveEven ) evenMask += GameTurn.CLASS_PROTOMECH;

        // Reset all of the Players' turn category counts
        for (Enumeration loop = game.getPlayers(); loop.hasMoreElements();) {
            final Player player = (Player) loop.nextElement();
            player.resetEvenTurns();
            player.resetMultiTurns();
            player.resetOtherTurns();

            // Add turns for protomechs weapons declaration.
            if ( protosFireMulti ) {

                // How many Protomechs does the player have?
                int numPlayerProtos = game.getSelectedEntityCount
                    ( new EntitySelector() {
                            private final int ownerId = player.getId();
                            public boolean accept( Entity entity ) {
                                if ( entity instanceof Protomech &&
                                     ownerId == entity.getOwnerId() )
                                    return true;
                                return false;
                            }
                        } );
                int numProtoUnits =
                    (int) Math.ceil( ((double) numPlayerProtos) / 5.0 );
                for ( int unit = 0; unit < numProtoUnits; unit++ ) {
                    if ( protosMoveEven ) player.incrementEvenTurns();
                    else player.incrementOtherTurns();
                }

            } // End handle-proto-firing-turns

        } // Handle the next player

        // Go through all entities, and update the turn categories of the
        // entity's player.  The teams get their totals from their players.
        // N.B. protomechs declare weapons fire based on their point.
        for (Enumeration loop = game.getEntities(); loop.hasMoreElements();) {
            final Entity entity = (Entity)loop.nextElement();
            if (entity.isSelectableThisTurn()) {
                final Player player = entity.getOwner();
                final Team team = game.getTeamForPlayer( player );
                if ( entity instanceof Infantry ) {
                    if ( infMoveEven ) player.incrementEvenTurns();
                    else if ( infMoveMulti ) player.incrementMultiTurns();
                    else player.incrementOtherTurns();
                }
                else if ( entity instanceof Protomech ) {
                    if ( !protosFireMulti ) {
                        if ( protosMoveEven ) player.incrementEvenTurns();
                        else if ( protosMoveMulti ) player.incrementMultiTurns();
                        else player.incrementOtherTurns();
                    }
                }
                else
                    player.incrementOtherTurns();
            }
        }

        // Generate the turn order for the Players *within*
        // each Team.  Map the teams to their turn orders.
        // Count the number of teams moving this turn.
        Hashtable allTeamTurns = new Hashtable( game.getTeamsVector().size() );
        Hashtable evenTrackers = new Hashtable( game.getTeamsVector().size() );
        int numTeamsMoving = 0;
        for (Enumeration loop = game.getTeams(); loop.hasMoreElements(); ) {
            final Team team = (Team) loop.nextElement();
            allTeamTurns.put( team, team.determineTeamOrder(game) );

            // Track both the number of times we've checked the team for
            // "leftover" turns, and the number of "leftover" turns placed.
            int[] evenTracker = new int[2];
            evenTracker[0] = 0;
            evenTracker[1] = 0;
            evenTrackers.put (team, evenTracker);

            // Count this team if it has any "normal" moves.
            if (team.getNormalTurns(game) > 0)
                numTeamsMoving++;
        }

        // Now, generate the global order of all teams' turns.
        TurnVectors team_order = TurnOrdered.generateTurnOrder
            ( game.getTeamsVector(), game );

        // See if there are any loaded units stranded on immobile transports.
        Enumeration strandedUnits = game.getSelectedEntities
            ( new EntitySelector() {
                    public boolean accept( Entity entity ) {
                        if ( Server.this.game.isEntityStranded(entity) )
                            return true;
                        return false;
                    }
                } );

        // Now, we collect everything into a single vector.
        Vector turns;

        if ( strandedUnits.hasMoreElements() &&
             game.getPhase() == Game.PHASE_MOVEMENT ) {
            // Add a game turn to unload stranded units, if this
            //  is the movement phase.
            turns = new Vector( team_order.getNormalTurns() +
                                team_order.getEvenTurns() + 1);
            turns.addElement( new GameTurn.UnloadStrandedTurn(strandedUnits) );
        } else {
            // No stranded units.
            turns = new Vector( team_order.getNormalTurns() +
                                team_order.getEvenTurns() );
        }

        // Walk through the global order, assigning turns
        // for individual players to the single vector.
        // Keep track of how many turns we've added to the vector.
        Team prevTeam = null;
        int min = team_order.getMin();
        for ( int numTurn = 0; team_order.hasMoreElements(); numTurn++ ) {
            Team team = (Team) team_order.nextElement();
            TurnVectors withinTeamTurns = (TurnVectors) allTeamTurns.get(team);

            int[] evenTracker = (int[]) evenTrackers.get (team);
            float teamEvenTurns = (float) team.getEvenTurns();

            // Calculate the number of "even" turns to add for this team.
            int numEven = 0;
            if (1 == numTeamsMoving) {
                // The only team moving should move all "even" units.
                numEven += teamEvenTurns;
            }
            else if (prevTeam == null) {
                // Increment the number of times we've checked for "leftovers".
                evenTracker[0]++;

                // The first team to move just adds the "baseline" turns.
                numEven += teamEvenTurns / min;
            }
            else if (!team.equals(prevTeam)) {
                // Increment the number of times we've checked for "leftovers".
                evenTracker[0]++;

                // This wierd equation attempts to spread the "leftover"
                // turns accross the turn's moves in a "fair" manner.
                // It's based on the number of times we've checked for
                // "leftovers" the number of "leftovers" we started with,
                // the number of times we've added a turn for a "leftover",
                // and the total number of times we're going to check.
                numEven += Math.ceil (evenTracker[0] * (teamEvenTurns % min)
                                       / min - 0.5) - evenTracker[1];

                // Update the number of turns actually added for "leftovers".
                evenTracker[1] += numEven;

                // Add the "baseline" number of turns.
                numEven += teamEvenTurns / min;
            }

            // Record this team for the next move.
            prevTeam = team;

            // This may be a "placeholder" for a team without "normal" turns.
            if (withinTeamTurns.hasMoreElements()) {

                // Not a placeholder... get the player who moves next.
                Player player = (Player) withinTeamTurns.nextElement();

                // If we've added all "normal" turns, allocate turns
                // for the infantry and/or protomechs moving even.
                GameTurn turn = null;
                if ( numTurn >= team_order.getNormalTurns() ) {
                    turn = new GameTurn.EntityClassTurn
                        (player.getId(), evenMask);
                }

                // If either Infantry or Protomechs move even, only allow
                // the other classes to move during the "normal" turn.
                else if ( infMoveEven || protosMoveEven ) {
                    turn = new GameTurn.EntityClassTurn
                        (player.getId(), ~evenMask);
                }

                // Otherwise, let *anybody* move.
                else {
                    turn = new GameTurn( player.getId() );
                }
                turns.addElement(turn);

            } // End team-has-"normal"-turns

            // Add the calculated number of "even" turns.
            // Allow the player at least one "normal" turn before the
            // "even" turns to help with loading infantry in deployment.
            while (numEven > 0 && withinTeamTurns.hasMoreEvenElements()) {
                Player evenPlayer = (Player) withinTeamTurns.nextEvenElement();
                turns.addElement
                    (new GameTurn.EntityClassTurn (evenPlayer.getId(),
                                                   evenMask));
                numEven--;
            }
        }

        // set fields in game
        game.setTurnVector(turns);
        game.resetTurnIndex();

        // send turns to all players
        send(createTurnVectorPacket());

    }

    /**
     * Write the initiative results to the report
     */
    private void writeInitiativeReport(boolean abbreviatedReport) {
        // write to report
        if (!abbreviatedReport) {
            if ((game.getLastPhase() == Game.PHASE_DEPLOYMENT) || game.isDeploymentComplete() || !game.shouldDeployThisRound()) {
                roundReport.append("\nInitiative Phase for Round #").append(game.getRoundCount());
            } else {
                if ( game.getRoundCount() == 0 ) {
                    roundReport.append("\nInitiative Phase for Deployment");
                } else {
                    roundReport.append("\nInitiative Phase for Deployment for Round #").append(game.getRoundCount());
                }
            }
        }
        roundReport.append("\n------------------------------\n");

        for (Enumeration i = game.getTeams(); i.hasMoreElements();) {
            final Team team = (Team)i.nextElement();

            // If there is only one player, list them as the 'team', and
            // use the team iniative
            if (team.getSize() == 1) {
                final Player player = (Player)team.getPlayers().nextElement();

                roundReport.append(player.getName()).append(" rolls a ").
                    append(team.getInitiative().toString()).append(".\n");
            } else {
                // Multiple players.  List the team, then break it down.
                roundReport.append(Player.teamNames[team.getId()]).
                    append(" rolls a ").
                    append(team.getInitiative().toString()).
                    append(".\n");

                for( Enumeration j = team.getPlayers(); j.hasMoreElements();) {
                    final Player player = (Player)j.nextElement();
                    roundReport.append("\t").append(player.getName()).
                        append(" rolls a ").
                        append(player.getInitiative().toString()).
                        append(".\n");
                }

            }
        }

        // The turn order is different in movement phase
        // if a player has any "even" moving units.
        roundReport.append("\nThe turn order for movement is:\n  ");
        boolean firstTurn = true;
        boolean hasEven = false;
        for (Enumeration i = game.getTurns(); i.hasMoreElements();) {
            GameTurn turn = (GameTurn)i.nextElement();
            Player player = getPlayer( turn.getPlayerNum() );
            if ( null != player ) {
                roundReport.append( (firstTurn ? "" : ", ") )
                    .append( player.getName() );
                firstTurn = false;
                if (player.getEvenTurns() > 0)
                    hasEven = true;
            }
        }
        if (hasEven) {
            roundReport.append( "\n    The turn order for " );
            if (game.getOptions().booleanOption("inf_deploy_even")
                || game.getOptions().booleanOption("protos_deploy_even"))
                roundReport.append( "deployment phase and " );
            roundReport.append( "firing phase will be different." );
        }

        roundReport.append("\n\n");
        if (!abbreviatedReport) {
            roundReport.append("  Wind direction is ")
                .append(game.getStringWindDirection());
            if (game.getWindStrength() != -1) {
                roundReport.append(".  Wind strength is ")
                    .append(game.getStringWindStrength());
            }
            roundReport.append(".\n");
        }
    }

    /**
     * Marks ineligible entities as not ready for this phase
     */
    private void setIneligible(int phase) {
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity entity = (Entity)e.nextElement();
            if (!isEligibleFor(entity, phase)) {
                entity.setDone(true);
            }
        }
    }

    /**
     * Determines if an entity is eligible for a phase.
     */
    private boolean isEligibleFor(Entity entity, int phase) {
        // only deploy in deployment phase
        if ((phase == Game.PHASE_DEPLOYMENT) == entity.isDeployed()) {
            return false;
        }

        switch (phase) {
            case Game.PHASE_MOVEMENT :
                return isEligibleForMovement(entity);
            case Game.PHASE_FIRING :
                return isEligibleForFiring(entity);
            case Game.PHASE_PHYSICAL :
                return entity.isEligibleForPhysical();
            case Game.PHASE_TARGETING :
                return isEligibleForTargetingPhase(entity);
            case Game.PHASE_OFFBOARD :
                return isEligibleForOffboard(entity);
            default:
                return true;
        }
    }

    /**
     * Pretty much anybody's eligible for movement. If the game option
     * is toggled on, inactive and immobile entities are not eligible.
     * OffBoard units are always ineligible
     * @param entity
     * @return
     */
    private boolean isEligibleForMovement(Entity entity) {
        // check if entity is offboard
        if (entity.isOffBoard()) {
            return false;
        }
        // check game options
        if (!game.getOptions().booleanOption("skip_ineligable_movement")) {
            return true;
        }

        // must be active
        if (!entity.isActive() || entity.isImmobile()) {
            return false;
        }

        return true;
    }

    /**
     * An entity is eligible if its to-hit number is anything but impossible.
     * This is only really an issue if friendly fire is turned off.
     */
    private boolean isEligibleForFiring(Entity entity) {
        // if you're charging, no shooting
        if (entity.isUnjammingRAC()
            || entity.isCharging()
            || entity.isMakingDfa()) {
            return false;
        }

        // if you're offboard, no shooting
        if (entity.isOffBoard()) {
            return false;
        }

        // check game options
        if (!game.getOptions().booleanOption("skip_ineligable_firing")) {
            return true;
        }

        // must be active
        if (!entity.isActive()) {
            return false;
        }

        // TODO: check for any weapon attacks

        return true;
    }

    /*
     * public boolean isEligibleForPhysical()
     *
     * This method moved to Entity class.  Perhaps the other
     * isEligible methods should also be moved?
     */

    /**
     * Have the loader load the indicated unit.
     * The unit being loaded loses its turn.
     *
     * @param   loader - the <code>Entity</code> that is loading the unit.
     * @param   unit - the <code>Entity</code> being loaded.
     */
    private void loadUnit( Entity loader, Entity unit ) {

        // Remove the loaded unit from the screen.
        unit.setPosition( null );


        // Remove the *last* friendly turn (removing the *first* penalizes
        // the opponent too much, and re-calculating moves is too hard).
        game.removeTurnFor(unit);
        send(createTurnVectorPacket());

        // Load the unit.
        loader.load( unit );

        // The loaded unit is being carried by the loader.
        unit.setTransportId( loader.getId() );

        // Update the loaded unit.
        this.entityUpdate( unit.getId() );
    }

    /**
     * Have the unloader unload the indicated unit.
     * The unit being unloaded does *not* gain a turn.
     *
     * @param   unloader - the <code>Entity</code> that is unloading the unit.
     * @param   unloaded - the <code>Targetable</code> unit being unloaded.
     * @param   pos - the <code>Coords</code> for the unloaded unit.
     * @param   facing - the <code>int</code> facing for the unloaded unit.
     * @return  <code>true</code> if the unit was successfully unloaded,
     *          <code>false</code> if the unit isn't carried in unloader.
     */
    private boolean unloadUnit( Entity unloader, Targetable unloaded,
                             Coords pos, int facing ) {

        // We can only unload Entities.
        Entity unit = null;
        if ( unloaded instanceof Entity ) {
            unit = (Entity) unloaded;
        } else {
            return false;
        }

        // Unload the unit.
        if ( !unloader.unload( unit ) ) {
            return false;
        }

        // The unloaded unit is no longer being carried.
        unit.setTransportId( Entity.NONE );

        // Place the unloaded unit onto the screen.
        unit.setPosition( pos );

        // Units unloaded onto the screen are deployed.
        if ( pos != null ) {
            unit.setDeployed( true );
        }

        // Point the unloaded unit in the given direction.
        unit.setFacing( facing );
        unit.setSecondaryFacing( facing );

        // Update the unloaded unit.
        this.entityUpdate( unit.getId() );

        // Unloaded successfully.
        return true;
    }

    /**
     * Record that the given building has been affected by the current
     * entity's movement.  At the end of the entity's movement, notify
     * the clients about the updates.
     *
     * @param   bldg - the <code>Building</code> that has been affected.
     * @param   collapse - a <code>boolean</code> value that specifies that
     *          the building collapsed (when <code>true</code>).
     */
    private void addAffectedBldg( Building bldg, boolean collapse ) {

        // If the building collapsed, then the clients have already
        // been notified, so remove it from the notification list.
        if ( collapse ) {
            System.err.print( "Removing building from a list of " + affectedBldgs.size() + "\n" );//killme
            this.affectedBldgs.remove( bldg );
            System.err.print( "... now list of " + affectedBldgs.size() + "\n" );//killme
        }

        // Otherwise, make sure that this building is tracked.
        else {
            this.affectedBldgs.put( bldg, Boolean.FALSE );
        }
    }

    /**
     * Walk through the building hexes that were affected by the recent
     * entity's movement.  Notify the clients about the updates to all
     * affected entities and uncollapsed buildings.  The affected hexes
     * is then cleared for the next entity's movement.
     */
    private void applyAffectedBldgs() {

        // Build a list of Building updates.
        Vector bldgUpdates = new Vector();

        // Only send a single turn update.
        boolean bTurnsChanged = false;

        // Walk the set of buildings.
        Enumeration bldgs = this.affectedBldgs.keys();
        while ( bldgs.hasMoreElements() ) {
            final Building bldg = (Building) bldgs.nextElement();

            // Walk through the building's coordinates.
            Enumeration bldgCoords = bldg.getCoords();
            while ( bldgCoords.hasMoreElements() ) {
                final Coords coords = (Coords) bldgCoords.nextElement();

                // Walk through the entities at these coordinates.
                Enumeration entities = game.getEntities( coords );
                while( entities.hasMoreElements() ) {
                    final Entity entity = (Entity) entities.nextElement();

                    // Is the entity infantry?
                    if ( entity instanceof Infantry ) {

                        // Is the infantry dead?
                        if ( entity.isDoomed() || entity.isDestroyed() ) {

                            // Has the entity taken a turn?
                            if ( !entity.isDone() ) {

                                // Dead entities don't take turns.
                                game.removeTurnFor(entity);
                                bTurnsChanged = true;

                            } // End entity-still-to-move

                            // Clean out the dead entity.
                            entity.setDestroyed(true);
                            game.moveToGraveyard(entity.getId());
                            send(createRemoveEntityPacket(entity.getId()));
                        }

                        // Infantry that aren't dead are damaged.
                        else {
                            this.entityUpdate( entity.getId() );
                        }

                    } // End entity-is-infantry

                } // Check the next entity.

            } // Handle the next hex in this building.

            // Add this building to the report.
            bldgUpdates.addElement( bldg );

        } // Handle the next affected building.

        // Did we update the turns?
        if ( bTurnsChanged ) {
            send(createTurnVectorPacket());
        }

        // Are there any building updates?
        if ( !bldgUpdates.isEmpty() ) {

            // Send the building updates to the clients.
            send( createUpdateBuildingCFPacket( bldgUpdates ) );

            // Clear the list of affected buildings.
            this.affectedBldgs.clear();
        }

        // And we're done.
        return;

    } // End private void applyAffectedBldgs()

    /**
     * Receives an entity movement packet, and if valid, executes it and ends
     * the current turn.
     *
     */
    private void receiveMovement(Packet packet, int connId) {
        Entity entity = game.getEntity(packet.getIntValue(0));
        MovePath md = (MovePath)packet.getObject(1);

        // is this the right phase?
        if (game.getPhase() != Game.PHASE_MOVEMENT) {
            System.err.println("error: server got movement packet in wrong phase");
            return;
        }

        // can this player/entity act right now?
        if (!game.getTurn().isValid(connId, entity, game)) {
            System.err.println("error: server got invalid movement packet");
            return;
        }

        // looks like mostly everything's okay
        processMovement(entity, md);

        // Notify the clients about any building updates.
        applyAffectedBldgs();

        // Update visibility indications if using double blind.
        if (doBlind()) {
            updateVisibilityIndicator();
        }

        // This entity's turn is over.
        // N.B. if the entity fell, a *new* turn has already been added.
        endCurrentTurn(entity);
    }

    /**
     * Steps thru an entity movement packet, executing it.
     */
    private void processMovement(Entity entity, MovePath md) {
        // check for fleeing
        if (md.contains(MovePath.STEP_FLEE)) {
            // Unit has fled the battlefield.
            phaseReport.append("\n" ).append( entity.getDisplayName()
            ).append( " flees the battlefield.\n");

            // Is the unit carrying passengers?
            final Vector passengers = entity.getLoadedUnits();
            if ( !passengers.isEmpty() ) {
                final Enumeration iter = passengers.elements();
                while ( iter.hasMoreElements() ) {
                    final Entity passenger = (Entity) iter.nextElement();
                    // Unit has fled the battlefield.
                    phaseReport.append( "   It carries " )
                        .append( passenger.getDisplayName() )
                        .append( " with it.\n" );
                    game.removeEntity( passenger.getId(),
                                       Entity.REMOVE_IN_RETREAT );
                    send( createRemoveEntityPacket(passenger.getId(),
                                                   Entity.REMOVE_IN_RETREAT) );
                }
            }

            // Handle any picked up MechWarriors
            Enumeration iter = entity.getPickedUpMechWarriors().elements();
            while (iter.hasMoreElements() ) {
                Integer mechWarriorId = (Integer)iter.nextElement();
                Entity mw = game.getEntity(mechWarriorId.intValue());

                // Is the MechWarrior an enemy?
                int condition = Entity.REMOVE_IN_RETREAT;
                String leavingText = "carries ";
                if (mw.isCaptured()) {
                    condition = Entity.REMOVE_CAPTURED;
                    leavingText = "takes ";
                }
                game.removeEntity( mw.getId(), condition );
                send( createRemoveEntityPacket(mw.getId(), condition) );
                    phaseReport.append( "   It " )
                        .append( leavingText )
                        .append( mw.getDisplayName() )
                        .append( " with it.\n" );
            }

            // Is the unit being swarmed?
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );

                // Has the swarmer taken a turn?
                if ( !swarmer.isDone() ) {

                    // Dead entities don't take turns.
                    game.removeTurnFor(swarmer);
                    send(createTurnVectorPacket());

                } // End swarmer-still-to-move

                // Unit has fled the battlefield.
                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                phaseReport.append( "   It takes " )
                    .append( swarmer.getDisplayName() )
                    .append( " with it.\n" );
                game.removeEntity( swarmerId, Entity.REMOVE_CAPTURED );
                send( createRemoveEntityPacket(swarmerId,
                                               Entity.REMOVE_CAPTURED) );
            }
            game.removeEntity( entity.getId(), Entity.REMOVE_IN_RETREAT );
            send( createRemoveEntityPacket(entity.getId(),
                                           Entity.REMOVE_IN_RETREAT) );
            return;
        }

        if (md.contains(MovePath.STEP_EJECT)) {
            phaseReport.append("\n" );
            if (entity instanceof Mech) {
                phaseReport.append( entity.getDisplayName()).append( " ejects.\n");
            } else if (entity instanceof Tank) {
                phaseReport.append( entity.getDisplayName()).append( " is abandoned by its crew.\n");
            }
            phaseReport.append(ejectEntity(entity, false));

            return;
        }

        // okay, proceed with movement calculations
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
        int curFacing = entity.getFacing();
        int distance = 0;
        int mpUsed = 0;
        int moveType = Entity.MOVE_NONE;
        int overallMoveType = Entity.MOVE_NONE;
        // if the entity already used some MPs,
        // it previously tried to get up and fell,
        // and then got another turn. set moveType
        // and overallMoveType accordingly
        if (entity.mpUsed > 0) {
            moveType = Entity.MOVE_WALK;
            overallMoveType = Entity.MOVE_WALK;
            if (entity.mpUsed > entity.getWalkMP()) {
                moveType = Entity.MOVE_RUN;
                overallMoveType = Entity.MOVE_RUN;
            }
        }
        boolean firstStep;
        boolean wasProne;
        boolean fellDuringMovement;
        int prevFacing = curFacing;
        IHex prevHex = null;
        final boolean isInfantry = (entity instanceof Infantry);
        AttackAction charge = null;
        PilotingRollData rollTarget;
        // cache this here, otherwise changing MP in the turn causes 
        // errorneous gravity PSRs
        int cachedGravityLimit = (Entity.MOVE_JUMP == moveType)?
            entity.getOriginalJumpMP() : entity.getRunMP(false); 

        // Compile the move
        md.compile(game, entity);

        if (md.contains(MovePath.STEP_CLEAR_MINEFIELD)) {
            ClearMinefieldAction cma = new ClearMinefieldAction(entity.getId());
            entity.setClearingMinefield(true);
            game.addAction(cma);
        }

        // check for MASC failure
        if (entity instanceof Mech) {
            if (((Mech)entity).checkForMASCFailure(phaseReport,md)) {
                // no movement after that
                md.clear();
            }
        }

        overallMoveType = md.getLastStepMovementType();

        // iterate through steps
        firstStep = true;
        fellDuringMovement = false;
        /* Bug 754610: Revert fix for bug 702735. */
        MoveStep prevStep = null;

        Vector movePath = new Vector();

        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            wasProne = entity.isProne();
            boolean isPavementStep = step.isPavementStep();
            boolean entityFellWhileAttemptingToStand = false;

            // stop for illegal movement
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            }
            
            // check piloting skill for getting up
            rollTarget = entity.checkGetUp(step);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                entity.heatBuildup += 1;
                entity.setProne(false);
                wasProne = false;
                game.resetPSRs(entity);
                entityFellWhileAttemptingToStand = !doSkillCheckInPlace(entity, rollTarget);
            }
            // did the entity just fall?
            if (entityFellWhileAttemptingToStand) {
                moveType = step.getMovementType();
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                break;
            }

            if (step.getType() == MovePath.STEP_UNJAM_RAC) {
                entity.setUnjammingRAC(true);
                game.addAction(new UnjamAction(entity.getId()));

                break;
            }

            // set most step parameters
            moveType = step.getMovementType();
            distance = step.getDistance();
            mpUsed = step.getMpUsed();

            // check for charge
            if (step.getType() == MovePath.STEP_CHARGE) {
                if (entity.canCharge()) {
                    checkExtremeGravityMovement(entity, step, curPos, cachedGravityLimit);
                    Targetable target = step.getTarget( game );
                    ChargeAttackAction caa = new ChargeAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                    entity.setDisplacementAttack(caa);
                    game.addCharge(caa);
                    charge = caa;
                } else {
                    sendServerChat("Illegal charge!! I don't think "+entity.getDisplayName() +" should be allowed to charge,"+
                                   " but the client of "+entity.getOwner().getName()+" disagrees.");
                    sendServerChat("Please make sure "+entity.getOwner().getName()+" is running MegaMek "+MegaMek.VERSION+
                                   ", or if that is already the case, submit a bug report at http://megamek.sf.net/");
                    return;
                };
                break;
            }

            // check for dfa
            if (step.getType() == MovePath.STEP_DFA) {
                if (entity.canDFA()) {
                    checkExtremeGravityMovement(entity, step, curPos, cachedGravityLimit);
                    Targetable target = step.getTarget( game );
                    DfaAttackAction daa = new DfaAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                    entity.setDisplacementAttack(daa);
                    game.addCharge(daa);
                    charge = daa;
                } else {
                    sendServerChat("Illegal DFA!! I don't think "+entity.getDisplayName() +" should be allowed to DFA,"+
                                   " but the client of "+entity.getOwner().getName()+" disagrees.");
                    sendServerChat("Please make sure "+entity.getOwner().getName()+" is running MegaMek "+MegaMek.VERSION+
                                   ", or if that is already the case, submit a bug report at http://megamek.sf.net/");
                    return;
                };
                break;
            }

            // set last step parameters
            curPos = step.getPosition();
            curFacing = step.getFacing();

            final IHex curHex = game.board.getHex(curPos);

            // Check for skid.
            rollTarget = entity.checkSkid(moveType, prevHex, overallMoveType,
                                          prevStep, prevFacing, curFacing,
                                          lastPos, curPos, isInfantry,
                                          distance);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Have an entity-meaningful PSR message.
                boolean psrPassed = true;
                if ( entity instanceof Mech ) {
                    psrPassed = doSkillCheckWhileMoving( entity, lastPos,
                                                          lastPos, rollTarget,
                                                          true );
                } else {
                    psrPassed = doSkillCheckWhileMoving( entity, lastPos,
                                                          lastPos, rollTarget,
                                                          false );
                }
                // Does the entity skid?
                if ( !psrPassed ){

                    curPos = lastPos;
                    Coords nextPos = curPos;
                    IHex    nextHex = null;
                    int    skidDistance = 0;
                    Enumeration targets = null;
                    Entity target = null;
                    int    curElevation;
                    int    nextElevation;

                    // All charge damage is based upon
                    // the pre-skid move distance.
                    entity.delta_distance = distance-1;

                    // Attacks against a skidding target have additional +2.
                    moveType = Entity.MOVE_SKID;

                    // What is the first hex in the skid?
                    nextPos = curPos.translated( prevFacing );
                    nextHex = game.board.getHex( nextPos );

                    // Move the entity a number hexes from curPos in the
                    // prevFacing direction equal to half the distance moved
                    // this turn (rounded up), unless something intervenes.
                    for ( skidDistance = 0;
                          skidDistance < (int) Math.ceil(entity.delta_distance / 2.0);
                          skidDistance++ ) {

                        // Is the next hex off the board?
                        if ( !game.board.contains(nextPos) ) {

                            // Can the entity skid off the map?
                            if (game.getOptions().booleanOption("push_off_board")) {
                                // Yup.  One dead entity.
                                game.removeEntity(entity.getId(),
                                                  Entity.REMOVE_PUSHED);
                                send(createRemoveEntityPacket(entity.getId(),
                                                              Entity.REMOVE_PUSHED));
                                phaseReport.append("*** " )
                                    .append( entity.getDisplayName() )
                                    .append( " has skidded off the field. ***\n");

                                // TODO: remove passengers and swarmers.

                                // The entity's movement is completed.
                                return;

                            } else {
                                // Nope.  Update the report.
                                phaseReport.append("   Can't skid off the field.\n");
                            }
                            // Stay in the current hex and stop skidding.
                            break;
                        }
                        
                        // Can the skiding entity enter the next hex from this?
                        // N.B. can skid along roads.
                        if ( ( entity.isHexProhibited(curHex) ||
                               entity.isHexProhibited(nextHex) ) &&
                             !Compute.canMoveOnPavement(game, curPos, nextPos)
                             ) {
                            // Update report.
                            phaseReport.append( "   Can't skid into hex " )
                                .append( nextPos.getBoardNum() )
                                .append( ".\n" );

                            // N.B. the BMRr pg. 22 says that the unit
                            // "crashes" into the terrain but it doesn't
                            // mention any damage.

                            // Stay in the current hex and stop skidding.
                            break;
                        }

                        // Hovercraft can "skid" over water.
                        // TODO: allow entities to occupy different levels of
                        //       buildings.
                        curElevation = curHex.floor();
                        nextElevation = nextHex.floor();
                        if ( entity instanceof Tank &&
                             entity.getMovementType() ==
                             Entity.MovementType.HOVER ) {
                            ITerrain land = curHex.
                                getTerrain(Terrains.WATER);
                            if ( land != null ) {
                                curElevation += land.getLevel();
                            }
                            land = nextHex.getTerrain(Terrains.WATER);
                            if ( land != null ) {
                                nextElevation += land.getLevel();
                            }
                        }

                        // BMRr pg. 22 - Can't skid uphill,
                        //      but can skid downhill.
                        if ( curElevation < nextElevation ) {
                            phaseReport.append
                                ( "   Can not skid uphill into hex " +
                                  nextPos.getBoardNum() ).append( ".\n" );
                            // Stay in the current hex and stop skidding.
                            break;
                        }

                        // Have skidding units suffer falls.
                        else if ( curElevation > nextElevation + 1 ) {
                            doEntityFallsInto( entity, curPos, nextPos,
                                               entity.getBasePilotingRoll() );
                            doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                            // Stay in the current hex and stop skidding.
                            break;
                        }

                        // Get any building in the hex.
                        Building bldg = game.board.getBuildingAt(nextPos);
                        boolean bldgSuffered = false;
                        boolean stopTheSkid = false;
                        // Does the next hex contain an entities?
                        // ASSUMPTION: hurt EVERYONE in the hex.
                        // TODO: allow entities to occupy different levels of
                        //       buildings, and only skid into a single level.
                        targets = game.getEntities( nextPos );
                        if ( targets.hasMoreElements()) {
                            boolean skidChargeHit = false;
                            while ( targets.hasMoreElements() ) {
                                target = (Entity) targets.nextElement();

                                // TODO : allow ready targets to move out of way

                                // Mechs and vehicles get charged,
                                // but need to make a to-hit roll
                                if ( !(target instanceof Infantry) ) {
                                    ChargeAttackAction caa = new ChargeAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                                    ToHitData toHit = caa.toHit(game, true);

                                    // Calculate hit location.
                                    if ( entity instanceof Tank &&
                                         entity.getMovementType() ==
                                         Entity.MovementType.HOVER &&
                                         0 < nextHex.terrainLevel(Terrains.WATER) ) {
                                        if ( 2 <= nextHex.terrainLevel(Terrains.WATER) ||
                                             target.isProne() ) {
                                            // Hovercraft can't hit the Mek.
                                            continue;
                                        }
                                        else {
                                            toHit.setHitTable(ToHitData.HIT_PUNCH);
                                        }
                                    }
                                    else if ( entity.getHeight() <
                                              target.getHeight() ) {
                                        toHit.setHitTable(ToHitData.HIT_KICK);
                                    } else {
                                        toHit.setHitTable(ToHitData.HIT_NORMAL);
                                    }
                                    toHit.setSideTable
                                        (Compute.targetSideTable(entity, target));

                                    // roll
                                    int roll = Compute.d6(2);
                                    // Update report.
                                    phaseReport.append( "   Skids into " +
                                                        target.getShortName() +
                                                        " in hex " +
                                                        nextPos.getBoardNum() );
                                    if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
                                        roll = -12;
                                        phaseReport.append(", but the charge is impossible (" ).append( toHit.getDesc() ).append( ") : ");
                                    } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
                                        phaseReport.append(", the charge is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
                                        roll = Integer.MAX_VALUE;
                                    } else {
                                        // report the roll
                                        phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
                                        phaseReport.append("rolls " ).append( roll ).append( " : ");
                                    }

                                    // Resolve a charge against the target.
                                    // ASSUMPTION: buildings block damage for
                                    //             *EACH* entity charged.
                                    if (roll < toHit.getValue()) {
                                        phaseReport.append("misses.\n");
                                    } else {
                                        // Resolve the charge.
                                        resolveChargeDamage
                                            (entity, target, toHit, prevFacing);
                                        // HACK: set the entity's location
                                        // to the original hex again, for the other targets
                                        if (targets.hasMoreElements()) {
                                            entity.setPosition(curPos);
                                        }
                                        bldgSuffered = true;
                                        skidChargeHit = true;
                                    }
                                    // The skid ends here if the target lives.
                                    if ( !target.isDoomed() &&
                                         !target.isDestroyed() &&
                                         !game.isOutOfGame(target) ) {
                                        stopTheSkid = true;
                                    }

                                    // if we don't do this here,
                                    // we can have a mech without a leg
                                    // standing on the field and moving
                                    // as if it still had his leg after
                                    // getting skid-charged.
                                    if (!target.isDone()) {
                                        resolvePilotingRolls(target);
                                        game.resetPSRs(target);
                                        target.applyDamage();
                                        phaseReport.append("\n");
                                    }

                                }

                                // Resolve "move-through" damage on infantry.
                                // Infantry inside of a building don't get a
                                // move-through, but suffer "bleed through"
                                // from the building.
                                else if ( bldg != null ) {

                                    // Update report.
                                    phaseReport.append( "   Skids through " +
                                                        target.getShortName() +
                                                        " in hex " +
                                                        nextPos.getBoardNum() +
                                                        "... " );

                                    // Infantry don't have different
                                    // tables for punches and kicks
                                    HitData hit = target.rollHitLocation( ToHitData.HIT_NORMAL,
                                                                          Compute.targetSideTable(entity, target)
                                                                          );

                                    // Damage equals tonnage, divided by 5.
                                    // ASSUMPTION: damage is applied in one hit.
                                    phaseReport.append( damageEntity(target, hit, (int)Math.round(entity.getWeight()/5)) );
                                    phaseReport.append( "\n" );

                                } // End handle-infantry

                                // Has the target been destroyed?
                                if ( target.isDoomed() ) {

                                    // Has the target taken a turn?
                                    if ( !target.isDone() ) {

                                        // Dead entities don't take turns.
                                        game.removeTurnFor(target);
                                        send(createTurnVectorPacket());

                                    } // End target-still-to-move

                                    // Clean out the entity.
                                    target.setDestroyed(true);
                                    game.moveToGraveyard(target.getId());
                                    send(createRemoveEntityPacket(target.getId()));

                                }

                                // Update the target's position,
                                // unless it is off the game map.
                                if ( !game.isOutOfGame(target) ) {
                                    entityUpdate( target.getId() );
                                }

                            } // Check the next entity in the hex.

                            // if we missed all the entities in the hex,
                            // move attacker to side hex
                            if (!skidChargeHit) {
                                Coords src = entity.getPosition();
                                Coords dest = Compute.getMissedChargeDisplacement
                                    (game, entity.getId(), src, prevFacing);
                                doEntityDisplacement(entity, src, dest, null);
                            } else {
                                // HACK: otherwise, set the entities position to that
                                // hex's coords, because we had to move the entity
                                // back earlier for the other targets
                                entity.setPosition(nextPos);
                            }
                        }

                        // Handle the building in the hex.
                        // TODO : BMRr pg. 22, only count buildings that are
                        //      higher than our starting terrain height.
                        // TODO: allow units to skid on top of buildings.
                        if ( bldg != null ) {

                            // Report that the entity has entered the bldg.
                            phaseReport.append( "   Skids into " )
                                .append( bldg.getName() )
                                .append( " in hex " )
                                .append( nextPos.getBoardNum() )
                                .append( ".\n" );

                            // If the building hasn't already suffered
                            // damage, then apply charge damage to the
                            // building and displace the entity inside.
                            // ASSUMPTION: you don't charge the building
                            //             if Tanks or Mechs were charged.
                            int chargeDamage = ChargeAttackAction.getDamageFor
                                ( entity );
                            if ( !bldgSuffered ) {
                                phaseReport.append( "      " )
                                    .append( damageBuilding( bldg,
                                                             chargeDamage ) );

                                // Apply damage to the attacker.
                                int toAttacker = ChargeAttackAction.getDamageTakenBy
                                    ( entity, bldg );
                                HitData hit = entity.rollHitLocation( ToHitData.HIT_NORMAL,
                                                                      Compute.targetSideTable(curPos, nextPos, entity.getFacing(), false)
                                                                      );
                                phaseReport.append( this.damageEntity( entity, hit, toAttacker ) )
                                    .append( "\n" );

                                entity.setPosition( nextPos );
                                doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                                curPos = nextPos;
                            } // End buildings-suffer-too

                            // Any infantry in the building take damage
                            // equal to the building being charged.
                            // ASSUMPTION: infantry take no damage from the
                            //             building absorbing damage from
                            //             Tanks and Mechs being charged.
                            damageInfantryIn( bldg, chargeDamage );

                            // If a building still stands, then end the skid,
                            // and add it to the list of affected buildings.
                            if ( bldg.getCurrentCF() > 0 ) {
                                stopTheSkid = true;
                                this.addAffectedBldg( bldg, false );
                            }

                        } // End handle-building.

                        // Do we stay in the current hex and stop skidding?
                        if ( stopTheSkid ) {
                            break;
                        }
                        // is the next hex a rubble hex?
                        rollTarget = entity.checkRubbleMove(step, nextHex,
                                                curPos, nextPos);
                        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                            doSkillCheckWhileMoving(entity, curPos, nextPos,
                                                        rollTarget, true);
                            if (entity.isProne()) {
                                // if we fell, stop the skid (see bug 1115608)
                                break;
                            }
                        }
                        
                        // is the next hex a swamp?
                        rollTarget = entity.checkSwampMove(step, nextHex, 
                                                              curPos, nextPos);
                        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                            if (!doSkillCheckWhileMoving(entity, curPos,
                                                   nextPos, rollTarget, false)){
                                entity.setStuck(true);
                                phaseReport.append("\n").append(
                                    entity.getDisplayName()).append(
                                    " gets stuck in the swamp.\n");
                                // stay here and stop skidding, see bug 1115608
                                break;
                            }
                        }

                        // Update the position and keep skidding.
                        entity.setPosition( nextPos );
                        doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                        curPos = nextPos;
                        phaseReport.append( "   Skids into hex " ).append(
                                            curPos.getBoardNum() ).append( ".\n" );

                        // Get the next hex in the skid?
                        nextPos = nextPos.translated( prevFacing );
                        nextHex = game.board.getHex( nextPos );

                    } // Handle the next skid hex.

                    // If the skidding entity violates stacking,
                    // displace targets until it doesn't.
                    curPos = entity.getPosition();
                    target = Compute.stackingViolation
                        (game, entity.getId(), curPos);
                    while (target != null) {
                        nextPos = Compute.getValidDisplacement
                            (game, target.getId(),
                             target.getPosition(), prevFacing);
                        // ASSUMPTION
                        // There should always be *somewhere* that
                        // the target can go... last skid hex if
                        // nothing else is available.
                        if ( null == nextPos ) {
                            // But I don't trust the assumption fully.
                            // Report the error and try to continue.
                            System.err.println( "The skid of " +
                                                entity.getShortName() +
                                                " should displace " +
                                                target.getShortName() +
                                                " in hex " +
                                                curPos.getBoardNum() +
                                                " but there is nowhere to go."
                                                );
                            break;
                        }
                        phaseReport.append( "    " ); // indent displacement
                        doEntityDisplacement(target, curPos, nextPos, null);
                        doEntityDisplacementMinefieldCheck(entity, curPos, nextPos);
                        target = Compute.stackingViolation( game,
                                                            entity.getId(),
                                                            curPos );
                    }

                    // Mechs suffer damage for every hex skidded.
                    if ( entity instanceof Mech ) {
                        // Calculate one half falling damage times skid length.
                        int damage = skidDistance * (int) Math.ceil(Math.round(entity.getWeight() / 10.0) / 2.0);

                        // report skid damage
                        phaseReport.append("    " ).append( entity.getDisplayName() ).append( " suffers " ).append( damage ).append( " damage from the skid.");

                        // standard damage loop
                        // All skid damage is to the front.
                        while (damage > 0) {
                            int cluster = Math.min(5, damage);
                            HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                            phaseReport.append(damageEntity(entity, hit, cluster));
                            damage -= cluster;
                        }
                        phaseReport.append( "\n" );
                    }

                    // Clean up the entity if it has been destroyed.
                    if ( entity.isDoomed() ) {
                        entity.setDestroyed(true);
                        game.moveToGraveyard(entity.getId());
                        send(createRemoveEntityPacket(entity.getId()));

                        // The entity's movement is completed.
                        return;
                    }

                    // Let the player know the ordeal is over.
                    phaseReport.append( "      Skid ends.\n" );

                    // set entity parameters
                    curFacing = entity.getFacing();
                    curPos = entity.getPosition();
                    entity.setSecondaryFacing( curFacing );

                    // skid consumes all movement
                    if (md.hasActiveMASC()) {
                        mpUsed = entity.getRunMP();
                    } else {
                        mpUsed = entity.getRunMPwithoutMASC();
                    }

                    entity.moved = moveType;
                    fellDuringMovement = true;
                    distance = entity.delta_distance;
                    break;

                } // End failed-skid-psr

            } // End need-skid-psr

            // check if we've moved into rubble
            rollTarget = entity.checkRubbleMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                doSkillCheckWhileMoving(entity, lastPos, curPos, rollTarget,
                                         true);
            }

            // check if we've moved into a swamp
            rollTarget = entity.checkSwampMove(step, curHex, lastPos, curPos);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                if (!doSkillCheckWhileMoving(entity, lastPos, curPos, rollTarget,
                                         false)){
                    entity.setStuck(true);
                    phaseReport.append("\n" ).append( entity.getDisplayName()
                    ).append( " gets stuck in the swamp.\n");
                    break;
                }
            }

            // check to see if we are a mech and we've moved OUT of fire
            if (entity instanceof Mech) {
                if ( !lastPos.equals(curPos)
                    && game.board.getHex(lastPos).containsTerrain(Terrains.FIRE)
                    && ( step.getMovementType() != Entity.MOVE_JUMP
                         // Bug #828741 -- jumping bypasses fire, but not on the first step
                         //   getMpUsed -- total MP used to this step
                         //   getMp -- MP used in this step
                         //   the difference will always be 0 on the "first step" of a jump,
                         //   and >0 on a step in the midst of a jump
                         || ( 0 == step.getMpUsed() - step.getMp() ) ) )
                {
                    entity.heatBuildup+=2;
                    phaseReport.append("\n" ).append( entity.getDisplayName()
                    ).append( " passes through a fire.  It will generate 2 more heat this round.\n");
                }
            }

            // check to see if we are not a mech and we've moved INTO fire
            if (!(entity instanceof Mech)) {
                if ( game.board.getHex(curPos).containsTerrain(Terrains.FIRE)
                    && !lastPos.equals(curPos)
                    && step.getMovementType() != Entity.MOVE_JUMP ) {
                        doFlamingDeath(entity);
                }
            }
            // check for extreme gravity movement
            if (!i.hasMoreElements() && !firstStep) {
                checkExtremeGravityMovement(entity, step, curPos, cachedGravityLimit);
            }
            // check for minefields.
            if ((!lastPos.equals(curPos) && (step.getMovementType() != Entity.MOVE_JUMP))
                || ((overallMoveType == Entity.MOVE_JUMP) && (!i.hasMoreElements()))) {
                checkVibrabombs(entity, curPos, false, lastPos, curPos);
                if (game.containsMinefield(curPos)) {
                    Enumeration minefields = game.getMinefields(curPos).elements();
                    while (minefields.hasMoreElements()) {
                        Minefield mf = (Minefield) minefields.nextElement();

                        switch (mf.getType()) {
                            case (Minefield.TYPE_CONVENTIONAL) :
                            case (Minefield.TYPE_THUNDER) :
                            case (Minefield.TYPE_THUNDER_INFERNO) :
                            case (Minefield.TYPE_COMMAND_DETONATED) :
                                if ((step.getMovementType() != Entity.MOVE_JUMP) || (!i.hasMoreElements()))
                                    enterMinefield(entity, mf, curPos, curPos, true);
                                break;
                            case (Minefield.TYPE_THUNDER_ACTIVE) :
                                if ((step.getMovementType() != Entity.MOVE_JUMP) || (!i.hasMoreElements()))
                                    enterMinefield(entity, mf, curPos, curPos, true);
                                else
                                    enterMinefield(entity, mf, curPos, curPos, true, 2);
                                break;
                        }
                    }
                }
            }

            // infantry discovers minefields if they end their move
            // in a minefield.

            if (!lastPos.equals(curPos) &&
                !i.hasMoreElements() &&
                isInfantry) {
                if (game.containsMinefield(curPos)) {
                    Player owner = entity.getOwner();
                    Enumeration minefields = game.getMinefields(curPos).elements();
                    while (minefields.hasMoreElements()) {
                        Minefield mf = (Minefield) minefields.nextElement();
                        if (!owner.containsMinefield(mf)) {
                            phaseReport.append(entity.getShortName() + " discovers a minefield.\n");
                            revealMinefield(owner, mf);
                        }
                    }
                }
            }

            // check if we've moved into water
            rollTarget = entity.checkWaterMove(step, curHex, lastPos, curPos,
                                               isPavementStep);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                // Swarmers need special handling.
                final int swarmerId = entity.getSwarmAttackerId();
                boolean swarmerDone = true;
                Entity swarmer = null;
                if (Entity.NONE != swarmerId) {
                    swarmer = game.getEntity( swarmerId );
                    swarmerDone = swarmer.isDone();
                }

                // Now do the skill check.
                doSkillCheckWhileMoving(entity, lastPos, curPos, rollTarget,
                                         true);

                // Swarming infantry platoons may drown.
                if (curHex.terrainLevel(Terrains.WATER) > 1) {
                    drownSwarmer(entity, curPos);
                }

                // Do we need to remove a game turn for the swarmer
                if (!swarmerDone &&
                    ( swarmer.isDoomed() || swarmer.isDestroyed() )) {
                    // We have to diddle with the swarmer's
                    // status to get its turn removed.
                    swarmer.setDone( false );
                    swarmer.setUnloaded( false );

                    // Dead entities don't take turns.
                    game.removeTurnFor( swarmer );
                    send( createTurnVectorPacket() );

                    // Return the original status.
                    swarmer.setDone( true );
                    swarmer.setUnloaded( true );
                }

                // check for inferno wash-off
                checkForWashedInfernos(entity, curPos);
            }
            // In water, may or may not be a new hex, neccessary to
            // check during movement, for breach damage, and always
            // set dry if appropriate
            //TODO: possibly make the locations local and set later
            doSetLocationsExposure(entity, curHex, isPavementStep,
                                   step.getMovementType() == Entity.MOVE_JUMP);

            // Handle loading units.
            if ( step.getType() == MovePath.STEP_LOAD ) {

                // Find the unit being loaded.
                Entity loaded = null;
                Enumeration entities = game.getEntities( curPos );
                while ( entities.hasMoreElements() ) {

                    // Is the other unit friendly and not the current entity?
                    loaded = (Entity)entities.nextElement();
                    if ( entity.getOwner() == loaded.getOwner() &&
                         !entity.equals(loaded) ) {

                        // The moving unit should be able to load the other
                        // unit and the other should be able to have a turn.
                        if ( !entity.canLoad(loaded) ||
                             !loaded.isSelectableThisTurn() ) {
                            // Something is fishy in Denmark.
                            System.err.println( entity.getShortName() +
                                                " can not load " +
                                                loaded.getShortName() );
                            loaded = null;
                        }
                        else {
                            // Have the deployed unit load the indicated unit.
                            this.loadUnit( entity, loaded );

                            // Stop looking.
                            break;
                        }

                    } else {
                        // Nope. Discard it.
                        loaded = null;
                    }

                } // Handle the next entity in this hex.

                // We were supposed to find someone to load.
                if ( loaded == null ) {
                    System.err.println( "Could not find unit for " +
                                        entity.getShortName() +
                                        " to load in " + curPos );
                }

            } // End STEP_LOAD

            // Handle unloading units.
            if ( step.getType() == MovePath.STEP_UNLOAD ) {
                Targetable unloaded = step.getTarget( game );
                if ( !this.unloadUnit( entity, unloaded,
                                       curPos, curFacing ) ) {
                    System.err.println( "Error! Server was told to unload " +
                                        unloaded.getDisplayName() +
                                        " from " + entity.getDisplayName() +
                                        " into " + curPos.getBoardNum() );
                }
            }

            // Handle non-infantry moving into a building.
            if (entity.checkMovementInBuilding(lastPos, curPos, step,
                                               curHex, prevHex)) {

                // Get the building being exited.
                // TODO: allow units to climb on top of buildings.
                Building bldgExited = game.board.getBuildingAt( lastPos );

                // Get the building being entered.
                // TODO: allow units to climb on top of buildings.
                Building bldgEntered = game.board.getBuildingAt( curPos );

                // If we're not leaving a building, just handle the "entered".
                boolean collapsed = false;
                if ( bldgExited == null ) {
                    collapsed = passBuildingWall( entity, bldgEntered,
                                                  lastPos, curPos,
                                                  distance, "entering" );
                    this.addAffectedBldg( bldgEntered, collapsed );
                }

                // If we're moving withing the same building, just handle
                // the "within".
                else if ( bldgExited.equals( bldgEntered ) ) {
                    collapsed = passBuildingWall( entity, bldgEntered,
                                                  lastPos, curPos,
                                                  distance, "moving in" );
                    this.addAffectedBldg( bldgEntered, collapsed );
                }

                // If we have different buildings, roll for each.
                else if ( bldgExited != null && bldgEntered != null ) {
                    collapsed = passBuildingWall( entity, bldgExited,
                                                  lastPos, curPos,
                                                  distance, "exiting" );
                    this.addAffectedBldg( bldgExited, collapsed );
                    collapsed = passBuildingWall( entity, bldgEntered,
                                                  lastPos, curPos,
                                                  distance, "entering" );
                    this.addAffectedBldg( bldgEntered, collapsed );
                }

                // Otherwise, just handle the "exited".
                else {
                    collapsed = passBuildingWall( entity, bldgExited,
                                                  lastPos, curPos,
                                                  distance, "exiting" );
                    this.addAffectedBldg( bldgExited, collapsed );
                }

                // Clean up the entity if it has been destroyed.
                if ( entity.isDoomed() ) {
                    entity.setDestroyed(true);
                    game.moveToGraveyard(entity.getId());
                    send(createRemoveEntityPacket(entity.getId()));

                    // The entity's movement is completed.
                    return;
                }

                // TODO: what if a building collapses into rubble?
            }

            // did the entity just fall?
            if (!wasProne && entity.isProne()) {
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                break;
            }

            // dropping prone intentionally?
            if (step.getType() == MovePath.STEP_GO_PRONE) {
                mpUsed = step.getMpUsed();
                rollTarget = entity.checkDislodgeSwarmers();
                if (rollTarget.getValue() == TargetRoll.CHECK_FALSE) {
                    // Not being swarmed
                    entity.setProne(true);
                    // check to see if we washed off infernos
                    checkForWashedInfernos(entity, curPos);
                    break;
                } else {
                    // Being swarmed
                    entity.setPosition(curPos);
                    if (doDislodgeSwarmerSkillCheck(entity,
                                                    rollTarget,
                                                    curPos)) {
                        // Entity falls
                        curFacing = entity.getFacing();
                        curPos = entity.getPosition();
                        fellDuringMovement = true;
                        break;
                    }
                }
            }

            // What the *heck* is this???
            movePath.addElement
                (new Integer(curPos.hashCode() ^ (curFacing << 16)));

            // update lastPos, prevStep, prevFacing & prevHex
            lastPos = new Coords(curPos);
            prevStep = step;
            /* Bug 754610: Revert fix for bug 702735.
            if (prevHex != null && !curHex.equals(prevHex)) {
            */
            if (!curHex.equals(prevHex)) {
                prevFacing = curFacing;
            }
            prevHex = curHex;

            firstStep = false;
        }

        // set entity parameters
        entity.setPosition(curPos);
        entity.setFacing(curFacing);
        entity.setSecondaryFacing(curFacing);
        entity.delta_distance = distance;
        entity.moved = moveType;
        entity.mpUsed = mpUsed;

        // if we ran with destroyed hip or gyro, we need a psr
        rollTarget = entity.checkRunningWithDamage(overallMoveType);
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            doSkillCheckInPlace(entity, rollTarget);
        }

        // but the danger isn't over yet!  landing from a jump can be risky!
        if (overallMoveType == Entity.MOVE_JUMP && !entity.isMakingDfa()) {
            // check for damaged criticals
            rollTarget = entity.checkLandingWithDamage();
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                doSkillCheckInPlace(entity, rollTarget);
            }
            // jumped into water?
            int waterLevel = game.board.getHex(curPos).terrainLevel(Terrains.WATER);
            rollTarget = entity.checkWaterMove(waterLevel);
            if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
                doSkillCheckInPlace(entity, rollTarget);
            }
            if (waterLevel > 1) {
                // Any swarming infantry will be destroyed.
                drownSwarmer(entity, curPos);
            }

            // jumped into swamp? maybe stuck!
            if (game.board.getHex(curPos).containsTerrain(Terrains.SWAMP)) {
                if (entity instanceof Mech) {
                    entity.setStuck(true);
                    phaseReport.append("\n" ).append( entity.getDisplayName()
                    ).append( " jumps into the swamp and gets stuck.\n");
                } else if (entity instanceof Infantry) {
                    PilotingRollData roll = entity.getBasePilotingRoll();
                    roll.addModifier(5, "infantry jumping into swamp");
                    if (!doSkillCheckWhileMoving(entity, curPos, curPos, roll, false)) {
                        entity.setStuck(true);
                        phaseReport.append("\n" ).append( entity.getDisplayName()
                        ).append( " gets stuck in the swamp.\n");
                    }
                }
            }

            // If the entity is being swarmed, jumping may dislodge the fleas.
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );
                final PilotingRollData roll =
                    entity.getBasePilotingRoll();

                // Add a +4 modifier.
                roll.addModifier( 4, "dislodge swarming infantry" );

                // If the swarmer has Assault claws, give a 1 modifier.
                // We can stop looking when we find our first match.
                for ( Enumeration iter = swarmer.getMisc();
                      iter.hasMoreElements(); ) {
                    Mounted mount = (Mounted) iter.nextElement();
                    EquipmentType equip = mount.getType();
                    if ( BattleArmor.ASSAULT_CLAW.equals
                         (equip.getInternalName()) ) {
                        roll.addModifier( 1, "swarmer has assault claws" );
                        break;
                    }
                }

                // okay, print the info
                phaseReport.append("\n")
                    .append( entity.getDisplayName() )
                    .append( " tries to dislodge swarming infantry.\n" );

                // roll
                final int diceRoll = Compute.d6(2);
                phaseReport.append("Needs " ).append( roll.getValueAsString()
                                   ).append( " [" ).append( roll.getDesc() ).append( "]"
                                   ).append( ", rolls " ).append( diceRoll ).append( " : ");
                if (diceRoll < roll.getValue()) {
                    phaseReport.append("fails.\n");
                } else {
                    // Dislodged swarmers don't get turns.
                    game.removeTurnFor( swarmer );
                    send( createTurnVectorPacket() );

                    // Update the report and the swarmer's status.
                    phaseReport.append("succeeds.\n");
                    entity.setSwarmAttackerId( Entity.NONE );
                    swarmer.setSwarmTargetId( Entity.NONE );

                    // Did the infantry fall into water?
                    final IHex curHex = game.board.getHex(curPos);
                    if ( curHex.terrainLevel(Terrains.WATER) > 0 ) {
                        // Swarming infantry die.
                        swarmer.setPosition( curPos );
                        phaseReport.append("    ")
                            .append(swarmer.getDisplayName())
                            .append(" is dislodged and drowns!")
                            .append(destroyEntity(swarmer, "a watery grave", false));
                    } else {
                        // Swarming infantry take an 11 point hit.
                        // ASSUMPTION : damage should not be doubled.
                        phaseReport.append("    ")
                            .append(swarmer.getDisplayName())
                            .append(" is dislodged and suffers 11 damage.")
                            .append( damageEntity(swarmer,
                                                  swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT),
                                                  11) )
                            .append( "\n" );
                        swarmer.setPosition( curPos );
                    }
                    entityUpdate( swarmerId );
                } // End successful-PSR

            } // End try-to-dislodge-swarmers

            // one more check for inferno wash-off
            checkForWashedInfernos(entity, curPos);

        } // End entity-is-jumping
        // update entity's locations' exposure
        doSetLocationsExposure(entity, game.board.getHex(curPos), game.board.getHex(curPos).surface() <= entity.getElevation(), false);

        // should we give another turn to the entity to keep moving?
        if (fellDuringMovement && entity.mpUsed < entity.getRunMP()
        && entity.isSelectableThisTurn() && !entity.isDoomed()) {
            entity.applyDamage();
            entity.setDone(false);
            GameTurn newTurn = new GameTurn.SpecificEntityTurn(entity.getOwner().getId(), entity.getId());
            game.insertNextTurn(newTurn);
            // brief everybody on the turn update
            send(createTurnVectorPacket());
            // let everyone know about what just happened
            roundReport.append(phaseReport.toString());
            game.resetPhaseReport();
            phaseReport = game.getPhaseReport(); //HACK
            send(createReportPacket());
        } else {
            entity.setDone(true);
        }

        // If the entity is being swarmed, update the attacker's position.
        final int swarmerId = entity.getSwarmAttackerId();
        if ( Entity.NONE != swarmerId ) {
            final Entity swarmer = game.getEntity( swarmerId );
            swarmer.setPosition( curPos );
            // If the hex is on fire, and the swarming infantry is
            // *not* Battle Armor, it drops off.
            if ( !(swarmer instanceof BattleArmor) &&
                 game.board.getHex(curPos).containsTerrain(Terrains.FIRE) ) {
                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                phaseReport.append( "\n   " )
                    .append( swarmer.getShortName() )
                    .append( " can't stand the fire's heat and drops off.\n" );
            }
            entityUpdate( swarmerId );
        }

        // Update the entitiy's position,
        // unless it is off the game map.
        if (!game.isOutOfGame(entity)) {
            if (entity.isDoomed()) {
                send(createRemoveEntityPacket(entity.getId(), entity.getRemovalCondition()));
            } else {
                entityUpdate( entity.getId(), movePath  );
            };
        };

        // if using double blind, update the player on new units he might see
        if (doBlind()) {
            send(entity.getOwner().getId(), createFilteredEntitiesPacket(entity.getOwner()));
        }

        // if we generated a charge attack, report it now
        if (charge != null) {
            send(createAttackPacket(charge, true));
        }
    }


    /**
     * Delivers a thunder-aug shot to the targetted hex area.
     * Thunder-Augs are 7 hexes, though, so...
     */
    private void deliverThunderAugMinefield( Coords coords,
                                             int playerId, int damage ) {
        Coords mfCoord = null;
        for (int dir=0; dir < 7; dir++) {
            switch (dir) {
            case 6:
                // The targeted hex.
                mfCoord = new Coords(coords);
                break;
            default:
                // The hex in the dir direction from the targeted hex.
                mfCoord = coords.translated(dir);
                break;
            }

            // Only if this is on the board...
            if ( game.board.contains(mfCoord) ) {
                Minefield minefield = null;
                Enumeration minefields = game.getMinefields(mfCoord).elements();
                // Check if there already are Thunder minefields in the hex.
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();
                    if (mf.getType() == Minefield.TYPE_THUNDER) {
                        minefield = mf;
                        break;
                    }
                }

                // Did we find a Thunder minefield in the hex?
                // N.B. damage Thunder minefields equals the number of
                //      missiles, divided by two, rounded up.
                if (minefield == null) {
                    // Nope.  Create a new Thunder minefield
                    minefield = Minefield.createThunderMF
                        ( mfCoord, playerId, (int)(damage/2 + damage%2) );
                } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
                    // Yup.  Replace the old one.
                    removeMinefield(minefield);
                    int newDamage = (int)(damage/2 + damage%2);
                    newDamage += minefield.getDamage();

                    // Damage from Thunder minefields are capped.
                    if ( newDamage > Minefield.MAX_DAMAGE ) {
                        newDamage = Minefield.MAX_DAMAGE;
                    }
                    minefield.setDamage(newDamage);
                }
                game.addMinefield(minefield);
                revealMinefield(minefield);

            } // End coords-on-board

        } // Handle the next coords

    }

    /**
     * Adds a Thunder minefield to the hex.
     * @param coords
     * @param playerId
     * @param damage
     */
    private void deliverThunderMinefield( Coords coords, int playerId,
                                          int damage ) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder minefield
        if (minefield == null) {
            minefield = Minefield.createThunderMF(coords, playerId, damage);
            // Add to the old one
        } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
        }
        game.addMinefield(minefield);
        revealMinefield(minefield);
    }

    /**
     * Adds a Thunder Inferno minefield to the hex.
     * @param coords
     * @param playerId
     * @param damage
     */
    private void deliverThunderInfernoMinefield(Coords coords, int playerId, int damage) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER_INFERNO) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder Inferno minefield
        if (minefield == null) {
            minefield = Minefield.createThunderInfernoMF(coords, playerId, damage);
            // Add to the old one
        } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
        }
        game.addMinefield(minefield);
        revealMinefield(minefield);
    }

    /**
     *Delivers a Arrow IV FASCAM shot to the targetted hex area.
     */
    private void deliverFASCAMMinefield( Coords coords, int playerId) {
        // Only if this is on the board...
        if ( game.board.contains(coords) ) {
            Minefield minefield = null;
            Enumeration minefields = game.getMinefields(coords).elements();
            // Check if there already are Thunder minefields in the hex.
            while (minefields.hasMoreElements()) {
                Minefield mf = (Minefield) minefields.nextElement();
                if (mf.getType() == Minefield.TYPE_THUNDER) {
                    minefield = mf;
                    break;
                }
            }
            // Did we find a Thunder minefield in the hex?
            // N.B. damage of FASCAM minefields is 30
            if (minefield == null) minefield = Minefield.createThunderMF( coords, playerId, 30 );
            removeMinefield(minefield);
            minefield.setDamage(30);
            game.addMinefield(minefield);
            revealMinefield(minefield);
        } // End coords-on-board
    }

    /**
     * Adds a Thunder-Active minefield to the hex.
     */
    private void deliverThunderActiveMinefield(Coords coords, int playerId, int damage) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER_ACTIVE) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder-Active minefield
        if (minefield == null) {
            minefield = Minefield.createThunderActiveMF(coords, playerId, damage);
            // Add to the old one
        } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
        }
        game.addMinefield(minefield);
        revealMinefield(minefield);
    }

    /**
     * Adds a Thunder-Vibrabomb minefield to the hex.
     */
    private void deliverThunderVibraMinefield(Coords coords, int playerId, int damage, int sensitivity) {
        Minefield minefield = null;
        Enumeration minefields = game.getMinefields(coords).elements();
        // Check if there already are Thunder minefields in the hex.
        while (minefields.hasMoreElements()) {
            Minefield mf = (Minefield) minefields.nextElement();
            if (mf.getType() == Minefield.TYPE_THUNDER_VIBRABOMB) {
                minefield = mf;
                break;
            }
        }

        // Create a new Thunder-Vibra minefield
        if (minefield == null) {
            minefield = Minefield.createThunderVibrabombMF(coords, playerId, damage, sensitivity);
            // Add to the old one
        } else if (minefield.getDamage() < Minefield.MAX_DAMAGE) {
            removeMinefield(minefield);
            int oldDamage = minefield.getDamage();
            damage += oldDamage;
            damage = (damage > Minefield.MAX_DAMAGE) ? Minefield.MAX_DAMAGE : damage;
            minefield.setDamage(damage);
        }
        game.addVibrabomb(minefield);
        revealMinefield(minefield);
    }

    /**
     * When an entity enters a conventional or Thunder minefield.
     */
    private void enterMinefield(Entity entity, Minefield mf, Coords src, Coords dest, boolean resolvePSRNow) {
        enterMinefield(entity, mf, src, dest, resolvePSRNow, 0);
    }

    /**
     * When an entity enters a conventional or Thunder minefield.
     * @param entity
     *
     * @param mf
     * @param src
     * @param dest
     * @param resolvePSRNow
     * @param hitMod
     */
    private void enterMinefield(Entity entity, Minefield mf, Coords src, Coords dest, boolean resolvePSRNow, int hitMod) {
        // Bug 954272: Mines shouldn't work underwater
        if (!game.board.getHex(mf.getCoords()).containsTerrain(Terrains.WATER) || game.board.getHex(mf.getCoords()).containsTerrain(Terrains.PAVEMENT)) {
        switch (mf.getType()) {
            case (Minefield.TYPE_CONVENTIONAL) :
            case (Minefield.TYPE_THUNDER) :
            case (Minefield.TYPE_THUNDER_ACTIVE) :
                if (mf.getTrigger() != Minefield.TRIGGER_NONE &&
                    Compute.d6(2) < (mf.getTrigger()+hitMod)) {
                    return;
                }

                phaseReport.append("\n" + entity.getShortName() + " hits a mine in hex " + mf.getCoords().getBoardNum() + ".");
                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                phaseReport.append(damageEntity(entity, hit, mf.getDamage())).append("\n");

                if (resolvePSRNow) {
                    resolvePilotingRolls(entity, true, src, dest);
                }

                if (!mf.isOneUse()) {
                    revealMinefield(mf);
                } else {
                    removeMinefield(mf);
                }
                break;

            case (Minefield.TYPE_THUNDER_INFERNO) :
                if (mf.getTrigger() != Minefield.TRIGGER_NONE &&
                    Compute.d6(2) < (mf.getTrigger()+hitMod)) {
                    return;
                }
                entity.infernos.add( InfernoTracker.STANDARD_ROUND, mf.getDamage() );
                phaseReport.append("\n" + entity.getShortName())
                           .append(" hits an inferno mine in hex ")
                           .append(mf.getCoords().getBoardNum() + ".")
                           .append("\n        " )
                           .append( entity.getDisplayName() )
                           .append( " now on fire for ")
                           .append( entity.infernos.getTurnsLeftToBurn() )
                           .append(" turns.\n");

                // start a fire in the targets hex
                IHex h = game.getBoard().getHex(dest);

                // Unless there a fire in the hex already, start one.
                if ( !h.containsTerrain( Terrains.FIRE ) ) {
                    phaseReport.append( " Fire started in hex " )
                               .append( dest.getBoardNum() )
                               .append( ".\n" );
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                }
                game.board.addInfernoTo(dest, InfernoTracker.STANDARD_ROUND, 1);
                sendChangedHex(dest);
                break;
            }
        }
    }

    /**
     * Checks to see if an entity sets off any vibrabombs.
     */
    private void checkVibrabombs(Entity entity, Coords coords, boolean displaced) {
        checkVibrabombs(entity, coords, displaced, null, null);
    }

    private void checkVibrabombs(Entity entity, Coords coords, boolean displaced, Coords lastPos, Coords curPos) {
        // Only mechs can set off vibrabombs.
        if (!(entity instanceof Mech)) {
            return;
        }

        int mass = (int) entity.getWeight();

        Enumeration e = game.getVibrabombs().elements();

        while (e.hasMoreElements()) {
            Minefield mf = (Minefield) e.nextElement();

            // Bug 954272: Mines shouldn't work underwater, and BMRr says Vibrabombs are mines
            if (game.board.getHex(mf.getCoords()).containsTerrain(Terrains.WATER) && !game.board.getHex(mf.getCoords()).containsTerrain(Terrains.PAVEMENT)) {
                continue;
            }

            // Mech weighing 10 tons or less can't set off the bomb
            if (mass <= mf.getSetting() - 10) {
                continue;
            }

            int effectiveDistance = (mass - mf.getSetting()) / 10;
            int actualDistance = coords.distance(mf.getCoords());

            if (actualDistance <= effectiveDistance) {
                phaseReport.append("\n" + entity.getShortName() + " sets off a vibrabomb in hex " + mf.getCoords().getBoardNum() + ".\n");
                explodeVibrabomb(mf);
            }

            // Hack; when moving, the Mech isn't in the hex during
            // the movement.
            if (!displaced && actualDistance == 0) {
                phaseReport.append(entity.getShortName() + " is hit by a vibrabomb attack.");
                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                phaseReport.append(damageEntity(entity, hit, mf.getDamage())).append("\n");
                resolvePilotingRolls(entity, true, lastPos, curPos);
                // we need to apply Damage now, in case the entity lost a leg,
                // otherwise it won't get a leg missing mod if it hasn't yet
                // moved and lost a leg, see bug 1071434 for an example
                entity.applyDamage();
            }
        }
    }

    /**
     * Removes the minefield from the game.
     * @param mf The <code>Minefield</code> to remove
     */
    private void removeMinefield(Minefield mf) {
        if (game.containsVibrabomb(mf)) {
            game.removeVibrabomb(mf);
        }
        game.removeMinefield(mf);

        Enumeration players = game.getPlayers();
        while (players.hasMoreElements()) {
            Player player = (Player) players.nextElement();
            removeMinefield(player, mf);
        }
    }

    /**
     * Removes the minfield from a player.
     * @param player The <code>Player</code> who's minefield should be removed
     * @param mf The <code>Minefield</code> to be removed
     */
    private void removeMinefield(Player player, Minefield mf) {
        if (player.containsMinefield(mf)) {
            player.removeMinefield(mf);
            send(player.getId(), new Packet(Packet.COMMAND_REMOVE_MINEFIELD, mf));
        }
    }

    /**
     * Reveals a minefield for all players.
     * @param mf The <code>Minefield</code> to be revealed
     */
    private void revealMinefield(Minefield mf) {
        Enumeration players = game.getPlayers();
        while (players.hasMoreElements()) {
            Player player = (Player) players.nextElement();
            revealMinefield(player, mf);
        }
    }

    /**
     * Reveals a minefield for a player.
     * @param player The <code>Player</code> who's minefiled should be revealed
     * @param mf The <code>Minefield</code> to be revealed
     */
    private void revealMinefield(Player player, Minefield mf) {
        if (!player.containsMinefield(mf)) {
            player.addMinefield(mf);
            send(player.getId(), new Packet(Packet.COMMAND_REVEAL_MINEFIELD, mf));
        }
    }

    /**
     * Explodes a vibrabomb.
     * @param mf The <code>Minefield</code> to explode
     */
    private void explodeVibrabomb(Minefield mf) {
        Enumeration targets = game.getEntities(mf.getCoords());

        while (targets.hasMoreElements()) {
            Entity entity = (Entity) targets.nextElement();

            // check for the "no_premove_vibra" option
            // If it's set, and the target has not yet moved,
            // it doesn't get damaged.
            if (!(entity.isDone() && game.getOptions().booleanOption("no_premove_vibra"))) {
                phaseReport.append(entity.getShortName() + " evades a vibrabomb attack.\n");
                continue;
            }
            phaseReport.append(entity.getShortName() + " is hit by a vibrabomb attack.");
            if (mf.getType() == Minefield.TYPE_VIBRABOMB) {
                // normal vibrabombs do all damage in one pack
                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                phaseReport.append(damageEntity(entity, hit, mf.getDamage())).append("\n");
            } else if (mf.getType() == Minefield.TYPE_THUNDER_VIBRABOMB) {
                // thunder vibrabombs do damage in 5 point packs
                int damage = mf.getDamage();
                while (damage > 0) {
                    int cluster = Math.min(5, damage);
                    HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                    phaseReport.append(damageEntity(entity, hit, cluster));
                    damage -= cluster;
                }
            }

            resolvePilotingRolls(entity, true, entity.getPosition(), entity.getPosition());
            // we need to apply Damage now, in case the entity lost a leg,
            // otherwise it won't get a leg missing mod if it hasn't yet
            // moved and lost a leg, see bug 1071434 for an example
            game.resetPSRs(entity);
            entity.applyDamage();
            phaseReport.append("\n");
            entityUpdate(entity.getId());
        }

        if (!mf.isOneUse()) {
            revealMinefield(mf);
        } else {
            removeMinefield(mf);
        }
    }

    /**
     * drowns any units swarming the entity
     * @param entity The <code>Entity</code> that is being swarmed
     * @param pos The <code>Coords</code> the entity is at
     */
    private void drownSwarmer(Entity entity, Coords pos) {
        // Any swarming infantry will be destroyed.
        final int swarmerId = entity.getSwarmAttackerId();
        if ( Entity.NONE != swarmerId ) {
            final Entity swarmer = game.getEntity( swarmerId );
            // Only *platoons* drown while swarming.
            if (!(swarmer instanceof BattleArmor)) {
                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                swarmer.setPosition( pos );
                phaseReport.append( "   The swarming unit, " )
                    .append( swarmer.getShortName() )
                    .append( ", drowns!\n" )
                    .append( destroyEntity(swarmer,
                                           "a watery grave", false) );
                entityUpdate( swarmerId );
            }
        }
    }

    /**
     * Checks to see if we may have just washed off infernos.  Call after
     * a step which may have done this.
     *
     * @param entity The <code>Entity</code> that is being checked
     * @param coords The <code>Coords</code> the entity is at
     */
    void checkForWashedInfernos(Entity entity, Coords coords) {
        IHex hex = game.board.getHex(coords);
        int waterLevel = hex.terrainLevel(Terrains.WATER);
        // Mech on fire with infernos can wash them off.
        if (!(entity instanceof Mech) || !entity.infernos.isStillBurning()) {
            return;
        }
        // Check if entering depth 2 water or prone in depth 1.
        if (waterLevel > entity.getHeight() ) {
            washInferno(entity, coords);
        }
    }

    /**
     * Washes off an inferno from a mech and adds it to the (water) hex.
     *
     * @param entity The <code>Entity</code> that is taking a bath
     * @param coords The <code>Coords</code> the entity is at
     */
    void washInferno(Entity entity, Coords coords) {
        game.board.addInfernoTo( coords, InfernoTracker.STANDARD_ROUND, 1 );
        entity.infernos.clear();

        // Start a fire in the hex?
        IHex hex = game.board.getHex(coords);
        phaseReport.append( " Inferno removed from " )
            .append( entity.getDisplayName() );
        if ( hex.containsTerrain(Terrains.FIRE) ) {
            phaseReport.append( ".\n" );
        } else {
            phaseReport.append( " and fire started in hex!\n" );
            hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
        }
        sendChangedHex(coords);
    }

    /**
     * Add heat from the movement phase
     */
    public void addMovementHeat() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            // build up heat from movement
            if (entity.moved == Entity.MOVE_WALK) {
                entity.heatBuildup += 1;
            } else if (entity.moved == Entity.MOVE_RUN || entity.moved == Entity.MOVE_SKID) {
                entity.heatBuildup += 2;
            } else if (entity.moved == Entity.MOVE_JUMP) {
                entity.heatBuildup += Math.max(3, entity.delta_distance);
            }
        }
    }

    /**
     * Set the locationsexposure of an entity
     *
     * @param entity The <code>Entity</code> who's exposure is being set
     * @param hex The <code>IHex</code> the entity is in
     * @param isPavementStep a <code>boolean</code> value wether
     *                       the entity is moving on a road
     * @param isJump a <code>boolean</code> value wether the entity is jumping
     */

    public void doSetLocationsExposure(Entity entity, IHex hex, boolean isPavementStep, boolean isJump) {
        if ( hex.terrainLevel(Terrains.WATER) > 0
            && entity.getMovementType() != Entity.MovementType.HOVER
            && !isPavementStep &&!isJump) {
            if (entity instanceof Mech && !entity.isProne()
                && hex.terrainLevel(Terrains.WATER) == 1) {
                for (int loop = 0; loop < entity.locations(); loop++) {
                    if (game.getOptions().booleanOption("vacuum"))
                        entity.setLocationStatus(loop, Entity.LOC_VACUUM);
                    else entity.setLocationStatus(loop, Entity.LOC_NORMAL);
                }
                entity.setLocationStatus(Mech.LOC_RLEG, Entity.LOC_WET);
                entity.setLocationStatus(Mech.LOC_LLEG, Entity.LOC_WET);
                phaseReport.append
                    (breachCheck(entity, Mech.LOC_RLEG, hex));
                phaseReport.append
                    (breachCheck(entity, Mech.LOC_LLEG, hex));
                if (entity instanceof QuadMech) {
                    entity.setLocationStatus(Mech.LOC_RARM, Entity.LOC_WET);
                    entity.setLocationStatus(Mech.LOC_LARM, Entity.LOC_WET);
                    phaseReport.append
                        (breachCheck(entity, Mech.LOC_RARM, hex));
                    phaseReport.append
                        (breachCheck(entity, Mech.LOC_LARM, hex));
                }
            } else {
                for (int loop = 0; loop < entity.locations(); loop++) {
                    entity.setLocationStatus(loop, Entity.LOC_WET);
                    phaseReport.append (breachCheck(entity, loop, hex));
                }
            }
        }else {
            for (int loop = 0; loop < entity.locations(); loop++) {
                if (game.getOptions().booleanOption("vacuum"))
                    entity.setLocationStatus(loop, Entity.LOC_VACUUM);
                else entity.setLocationStatus(loop, Entity.LOC_NORMAL);
            }
        }

    }

    /**
     * Do a piloting skill check while standing still (during the
     *  movement phase).
     *
     * @param entity The <code>Entity</code> that should make the PSR
     * @param roll   The <code>PilotingRollData</code> to be used for this PSR.
     *
     *@param Returns true if check succeeds, false otherwise.
     *
     */
    private boolean doSkillCheckInPlace(Entity entity, PilotingRollData roll) {
        if (roll.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            return true;
        }

        // non-mechs should never get here
        if (! (entity instanceof Mech) || entity.isProne()) {
            return true;
        }

        // okay, print the info
        phaseReport.append("\n" ).append( entity.getDisplayName() )
            .append( " must make a piloting skill check (" )
            .append( roll.getLastPlainDesc() ).append( ")" ).append( ".\n");
        // roll
        final int diceRoll = Compute.d6(2);
        phaseReport.append("Needs " ).append( roll.getValueAsString()
        ).append( " [" ).append( roll.getDesc() ).append( "]"
        ).append( ", rolls " ).append( diceRoll ).append( " : ");
        if (diceRoll < roll.getValue()) {
            phaseReport.append("falls.\n");
            doEntityFall(entity, roll);
            return false;
        } else {
            phaseReport.append("succeeds.\n");
            return true;
        }
    }

    /**
     * Do a Piloting Skill check to dislogde swarming infantry.
     *
     * @param   entity The <code>Entity</code> that is doing the dislodging.
     * @param   roll The <code>PilotingRollData</code> for this PSR.
     * @param   curPos The <code>Coords</code> the entity is at.
     * @return  <code>true</code> if the dislodging is successful.
     */
    private boolean doDislodgeSwarmerSkillCheck
        (Entity entity, PilotingRollData roll, Coords curPos)
    {
        // okay, print the info
        phaseReport.append("\n" ).append( entity.getDisplayName() )
            .append( " must make a piloting skill check (" )
            .append( roll.getLastPlainDesc() ).append( ")" ).append( ".\n");
        // roll
        final int diceRoll = Compute.d6(2);
        phaseReport.append("Needs " ).append( roll.getValueAsString()
        ).append( " [" ).append( roll.getDesc() ).append( "]"
        ).append( ", rolls " ).append( diceRoll ).append( " : ");
        if (diceRoll < roll.getValue()) {
            phaseReport.append("fails.\n");
            return false;
        } else {
            // Dislodged swarmers don't get turns.
            int swarmerId = entity.getSwarmAttackerId();
            final Entity swarmer = game.getEntity( swarmerId );
            game.removeTurnFor( swarmer );
            send( createTurnVectorPacket() );

            // Update the report and cause a fall.
            phaseReport.append("succeeds.\n");
            entity.setPosition( curPos );
            doEntityFallsInto(entity, curPos, curPos, roll, false);
            return true;
        }
    }

    /**
     * Do a piloting skill check while moving.
     *
     * @param   entity - the <code>Entity</code> that must roll.
     * @param   src - the <code>Coords</code> the entity is moving from.
     * @param   dest - the <code>Coords</code> the entity is moving to.
     *          This value can be the same as src for in-place checks.
     * @param   reason - the <code>PilotingRollData</code> that is causing
     *          this check.
     * @param   isFallRoll - a <code>boolean</code> flag that indicates that
     *          failure will result in a fall or not.  Falls will be processed.
     * @return  <code>true</code> if the pilot passes the skill check.
     */
    private boolean doSkillCheckWhileMoving( Entity entity,
                                             Coords src,
                                             Coords dest,
                                             PilotingRollData roll,
                                             boolean isFallRoll ) {
        boolean result = true;
        boolean fallsInPlace;

        // Start the info for this roll.
        phaseReport.append("\n" )
            .append( entity.getDisplayName() )
            .append( " must make a piloting skill check" );

        // Will the entity fall in the source or destination hex?
        if ( src.equals(dest) ) {
            fallsInPlace = true;
            phaseReport.append( " while moving in hex " )
                .append( src.getBoardNum() );
        } else {
            fallsInPlace = false;
            phaseReport.append( " while moving from hex " )
                .append( src.getBoardNum() )
                .append( " to hex " )
                .append( dest.getBoardNum() );
        }

        // Finish the info.
        phaseReport.append( " (" )
            .append( roll.getLastPlainDesc() )
            .append( ")" )
            .append( ".\n" );

        // roll
        final int diceRoll = Compute.d6(2);
        phaseReport.append( "Needs " )
            .append( roll.getValueAsString() )
            .append( " [" )
            .append( roll.getDesc() )
            .append( "]" )
            .append( ", rolls " )
            .append( diceRoll )
            .append( " : " );
        if (diceRoll < roll.getValue()) {
            // Does failing the PSR result in a fall.
            if ( isFallRoll ) {
                phaseReport.append("falls.\n");
                doEntityFallsInto( entity,
                                   (fallsInPlace ? dest : src),
                                   (fallsInPlace ? src : dest),
                                   roll );
            } else {
                phaseReport.append("fails.\n");
                entity.setPosition( fallsInPlace ? src : dest );
            }
            result = false;
        } else {
            phaseReport.append("succeeds.\n");
        }
        return result;
    }


    /**
     * The entity falls into the hex specified.  Check for any conflicts and
     * resolve them.  Deal damage to faller.
     *
     * @param entity The <code>Entity</code> that is falling.
     * @param src The <code>Coords</code> of the source hex.
     * @param dest The <code>Coords</code> of the destination hex.
     * @param roll The <code>PilotingRollData</code> to be used for PSRs induced
     * by the falling.
     */
    private void doEntityFallsInto(Entity entity, Coords src, Coords dest, PilotingRollData roll) {
        doEntityFallsInto(entity, src, dest, roll, true);
    }

    /**
    * The entity falls into the hex specified.  Check for any conflicts and
    * resolve them.  Deal damage to faller.
    *
    * @param entity The <code>Entity</code> that is falling.
    * @param src The <code>Coords</code> of the source hex.
    * @param dest The <code>Coords</code> of the destination hex.
    * @param roll The <code>PilotingRollData</code> to be used for PSRs induced
    * by the falling.
    * @param causeAffa The <code>boolean</code> value wether this fall should
    * be able to cause an accidental fall from above
    */
    private void doEntityFallsInto(Entity entity, Coords src, Coords dest, PilotingRollData roll, boolean causeAffa) {
        final IHex srcHex = game.board.getHex(src);
        final IHex destHex = game.board.getHex(dest);
        final int fallElevation = Math.abs(destHex.floor() - srcHex.floor());
        int direction = src.direction(dest);
        // check entity in target hex
        Entity violation = Compute.stackingViolation(game, entity.getId(), dest);

        Entity affaTarget = game.getAffaTarget(dest);
        // falling mech falls
        phaseReport.append(entity.getDisplayName() ).append( " falls "
        ).append( fallElevation ).append( " level(s) into hex "
        ).append( dest.getBoardNum() );

        // if hex was empty, deal damage and we're done
        if (violation == null || affaTarget == null) {
            doEntityFall(entity, dest, fallElevation, roll);
            return;
        }

        // hmmm... somebody there... problems.
        if (fallElevation >= 2 && causeAffa && affaTarget != null) {
            // accidental fall from above: havoc!
            phaseReport.append(", causing an accidental fall from above onto "
            ).append( affaTarget.getDisplayName() );

            // determine to-hit number
            ToHitData toHit = new ToHitData(7, "base");
            if (affaTarget instanceof Tank ) {
                toHit = new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target is a Tank");
            } else {
                toHit.append(Compute.getTargetMovementModifier(game, affaTarget.getId()));
                toHit.append(Compute.getTargetTerrainModifier(game, affaTarget));
            }

            if (toHit.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                // roll dice
                final int diceRoll = Compute.d6(2);
                phaseReport.append( ".\n");
                phaseReport.append("Collision occurs on a " ).append( toHit.getValue()
                ).append( " or greater.  Rolls " ).append( diceRoll);
                if (diceRoll >= toHit.getValue()) {
                    phaseReport.append(", hits!\n");
                    // deal damage to target
                    int damage = Compute.getAffaDamageFor(entity);
                    phaseReport.append(affaTarget.getDisplayName() ).append( " takes "
                    ).append( damage ).append( " from the collision.");
                    while (damage > 0) {
                        int cluster = Math.min(5, damage);
                        HitData hit = affaTarget.rollHitLocation(ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT);
                        phaseReport.append(damageEntity(affaTarget, hit, cluster));
                        damage -= cluster;
                    }
                    phaseReport.append("\n");

                    // attacker falls as normal, on his back
                    // only given a modifier, so flesh out into a full piloting roll
                    PilotingRollData pilotRoll = entity.getBasePilotingRoll();
                    pilotRoll.append(roll);
                    doEntityFall(entity, dest, fallElevation, 3, pilotRoll);
                    doEntityDisplacementMinefieldCheck(entity, src, dest);

                    // defender pushed away, or destroyed, if there is a stacking violation
                    if (Compute.stackingViolation(game, entity.getId(), dest) != null) {
                        Coords targetDest = Compute.getValidDisplacement(game, violation.getId(), dest, direction);
                        if (targetDest != null) {
                            doEntityDisplacement(affaTarget, dest, targetDest, new PilotingRollData(violation.getId(), 2, "fallen on"));
                            // Update the violating entity's postion on the client.
                            entityUpdate( affaTarget.getId() );
                        } else {
                            // ack!  automatic death!  Tanks
                            // suffer an ammo/power plant hit.
                            // TODO : a Mech suffers a Head Blown Off crit.
                            phaseReport.append(destroyEntity(affaTarget, "impossible displacement", (violation instanceof Mech), (violation instanceof Mech)));
                        }
                    }
                } else {
                    phaseReport.append(", misses.\n");
                }
            } else {
                phaseReport.append(", but the accidental fall is an automatic miss (" ).append( toHit.getDesc() ).append( ") :\n");
            }
            // ok, we missed, let's fall into a valid other hex and not cause an AFFA while doing so
            Coords targetDest = Compute.getValidDisplacement(game, entity.getId(), dest, direction);
            if (targetDest != null) {
                doEntityFallsInto(entity, src, targetDest, new PilotingRollData(entity.getId(), PilotingRollData.IMPOSSIBLE, "pushed off a cliff"), false);
                // Update the entity's postion on the client.
                entityUpdate( entity.getId() );
            } else {
                // ack!  automatic death!  Tanks
                // suffer an ammo/power plant hit.
                // TODO : a Mech suffers a Head Blown Off crit.
                phaseReport.append(destroyEntity(entity, "impossible displacement", (entity instanceof Mech), (entity instanceof Mech)));
            }
        } else {
            // damage as normal
            doEntityFall(entity, dest, fallElevation, roll);
            // target gets displaced, because of low elevation
            Coords targetDest = Compute.getValidDisplacement(game, entity.getId(), dest, direction);
            doEntityDisplacement(violation, dest, targetDest, new PilotingRollData(violation.getId(), 0, "domino effect"));
            // Update the violating entity's postion on the client.
            entityUpdate( violation.getId() );
        }
    }

    /**
     * Displace a unit in the direction specified.  The unit moves in that
     * direction, and the piloting skill roll is used to determine if it
     * falls.  The roll may be unnecessary as certain situations indicate an
     * automatic fall.  Rolls are added to the piloting roll list.
     */
    private void doEntityDisplacement(Entity entity, Coords src, Coords dest,
    PilotingRollData roll) {
        if (!game.board.contains(dest)) {
            // set position anyway, for pushes moving through and stuff like
            // that
            entity.setPosition(dest);
            if (!entity.isDoomed()) {
                game.removeEntity(entity.getId(),
                        Entity.REMOVE_PUSHED);
                send(createRemoveEntityPacket(entity.getId(),
                                    Entity.REMOVE_PUSHED));
                phaseReport.append("\n*** " ).append( entity.getDisplayName() ).append( " has been forced from the field. ***\n");
                // TODO: remove passengers and swarmers.
            }
            return;
        }
        final IHex srcHex = game.board.getHex(src);
        final IHex destHex = game.board.getHex(dest);
        final int direction = src.direction(dest);
        // Handle null hexes.
        if ( srcHex == null || destHex == null ) {
            System.err.println( "Can not displace " + entity.getShortName() +
                                " from " + src +
                                " to " + dest + "." );
            return;
        }
        int fallElevation = entity.elevationOccupied(srcHex) - entity.elevationOccupied(destHex);
        if (fallElevation > 1) {
            doEntityFallsInto(entity, src, dest, roll);
            return;
        } else {
            Entity violation = Compute.stackingViolation(game, entity.getId(), dest);
            if (violation == null) {
                // move and roll normally
                phaseReport.append("    "
                ).append( entity.getDisplayName()
                ).append( " is displaced into hex "
                ).append( dest.getBoardNum() ).append( ".\n");
                entity.setPosition(dest);
                doEntityDisplacementMinefieldCheck(entity, src, dest);
                doSetLocationsExposure(entity, destHex, destHex.hasPavement(), false);
                if (roll != null) {
                    game.addPSR(roll);
                }
                // Update the entity's postion on the client.
                entityUpdate( entity.getId() );
                return;
            } else {
                // domino effect: move & displace target
                phaseReport.append("    "
                ).append( entity.getDisplayName()
                ).append( " is displaced into hex "
                ).append( dest.getBoardNum() ).append( ", violating stacking with "
                ).append( violation.getDisplayName() ).append( ".\n");
                entity.setPosition(dest);
                doEntityDisplacementMinefieldCheck(entity, src, dest);
                if (roll != null) {
                    game.addPSR(roll);
                }
                doEntityDisplacement(violation, dest, dest.translated(direction), new PilotingRollData(violation.getId(), 0, "domino effect"));
                // Update the violating entity's postion on the client,
                // if it didn't get displaced off the board.
                if ( !game.isOutOfGame(violation) ) {
                    entityUpdate( violation.getId() );
                }
                return;
            }
        }
    }

    private void doEntityDisplacementMinefieldCheck(Entity entity, Coords src, Coords dest) {
        if (game.containsMinefield(dest)) {
            Enumeration minefields = game.getMinefields(dest).elements();
            while (minefields.hasMoreElements()) {
                Minefield mf = (Minefield) minefields.nextElement();
                enterMinefield(entity, mf, src, dest, false);
            }
        }
        checkVibrabombs(entity, dest, true);
    }

    /**
     * Receive a deployment packet.  If valid, execute it and end the current
     * turn.
     */
    private void receiveDeployment(Packet packet, int connId) {
        Entity entity = game.getEntity(packet.getIntValue(0));
        Coords coords = (Coords)packet.getObject(1);
        int nFacing = packet.getIntValue(2);

        // Handle units that deploy loaded with other units.
        int loadedCount = packet.getIntValue(3);
        Vector loadVector = new Vector();
        for ( int i = 0; i < loadedCount; i++ ){
            int loadedId = packet.getIntValue( 4 + i );
            loadVector.addElement(game.getEntity( loadedId ));
        }

        // is this the right phase?
        if (game.getPhase() != Game.PHASE_DEPLOYMENT) {
            System.err.println("error: server got deployment packet in wrong phase");
            return;
        }

        // can this player/entity act right now?
        if ( !game.getTurn().isValid(connId, entity, game)
             || !game.board.isLegalDeployment(coords, entity.getOwner()) ) {
            System.err.println("error: server got invalid deployment packet");
            return;
        }

        // looks like mostly everything's okay
        processDeployment(entity, coords, nFacing, loadVector);
        endCurrentTurn(entity);
    }

    /**
     * Process a deployment packet by... deploying the entity!  We load any
     * other specified entities inside of it too.  Also, check that the
     * deployment is valid.
     */
    private void processDeployment(Entity entity, Coords coords, int nFacing, Vector loadVector) {
        for (Enumeration i = loadVector.elements(); i.hasMoreElements();) {
            Entity loaded = (Entity)i.nextElement();
            if ( loaded == null || loaded.getPosition() != null ||
                 loaded.getTransportId() != Entity.NONE ) {
                // Something is fishy in Denmark.
                System.err.println("error: " + entity + " can not load entity #" + loaded );
                break;
            }
            else {
                // Have the deployed unit load the indicated unit.
                this.loadUnit( entity, loaded );
            }
        }

        entity.setPosition(coords);
        entity.setFacing(nFacing);
        entity.setSecondaryFacing(nFacing);
        entity.setDone(true);
        entity.setDeployed(true);
        entityUpdate(entity.getId());
    }

    private void receiveArtyAutoHitHexes(Packet packet, int connId) {
        Vector artyAutoHitHexes = (Vector) packet.getObject(0);

        Integer playerId = (Integer)artyAutoHitHexes.firstElement();
        artyAutoHitHexes.removeElementAt(0);

        // is this the right phase?
        if (game.getPhase() != Game.PHASE_SET_ARTYAUTOHITHEXES) {
            System.err.println("error: server got set artyautohithexespacket in wrong phase");
            return;
        }
        game.getPlayer(playerId.intValue()).setArtyAutoHitHexes(artyAutoHitHexes);
        endCurrentTurn(null);
    }

    private void receiveDeployMinefields(Packet packet, int connId) {
        Vector minefields = (Vector) packet.getObject(0);

        // is this the right phase?
        if (game.getPhase() != Game.PHASE_DEPLOY_MINEFIELDS) {
            System.err.println("error: server got deploy minefields packet in wrong phase");
            return;
        }

        // looks like mostly everything's okay
        processDeployMinefields(minefields);
        endCurrentTurn(null);
    }

    private void processDeployMinefields(Vector minefields) {
        int playerId = Player.PLAYER_NONE;
        for (int i = 0; i < minefields.size(); i++) {
            Minefield mf = (Minefield) minefields.elementAt(i);
            playerId = mf.getPlayerId();

            game.addMinefield(mf);
            if (mf.getType() == Minefield.TYPE_VIBRABOMB) {
                game.addVibrabomb(mf);
            }
        }

        Player player = game.getPlayer( playerId );
        if ( null != player ) {
            int teamId = player.getTeam();

            if (teamId != Player.TEAM_NONE) {
                Enumeration teams = game.getTeams();
                while (teams.hasMoreElements()) {
                    Team team = (Team) teams.nextElement();
                    if (team.getId() == teamId) {
                        Enumeration players = team.getPlayers();
                        while (players.hasMoreElements()) {
                            Player teamPlayer = (Player) players.nextElement();
                            if (teamPlayer.getId() != player.getId()) {
                                send(teamPlayer.getId(), new Packet(Packet.COMMAND_DEPLOY_MINEFIELDS, minefields));
                            }
                            teamPlayer.addMinefields(minefields);
                        }
                        break;
                    }
                }
            } else {
                player.addMinefields(minefields);
            }
        }
    }
    /**
     * Gets a bunch of entity attacks from the packet.  If valid, processess
     * them and ends the current turn.
     */
    private void receiveAttack(Packet packet, int connId) {
        Entity entity = game.getEntity(packet.getIntValue(0));
        Vector vector = (Vector)packet.getObject(1);

        // is this the right phase?
        if (game.getPhase() != Game.PHASE_FIRING
        && game.getPhase() != Game.PHASE_PHYSICAL
        && game.getPhase() != Game.PHASE_TARGETING) {
            System.err.println("error: server got attack packet in wrong phase");
            return;
        }

        // can this player/entity act right now?
        if (!game.getTurn().isValid(connId, entity, game)) {
            System.err.println("error: server got invalid attack packet");
            return;
        }

        // looks like mostly everything's okay
        processAttack(entity, vector);

        // Update visibility indications if using double blind.
        if (doBlind()) {
            updateVisibilityIndicator();
        }

        endCurrentTurn(entity);
    }

    /**
     * Process a batch of entity attack (or twist) actions by adding them to
     * the proper list to be processed later.
     */
    private void processAttack(Entity entity, Vector vector) {

        // Not **all** actions take up the entity's turn.
        boolean setDone =
            !(game.getTurn() instanceof GameTurn.TriggerAPPodTurn);
        for (Enumeration i = vector.elements(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();

            // is this the right entity?
            if (ea.getEntityId() != entity.getId()) {
                System.err.println("error: attack packet has wrong attacker");
                continue;
            }

            // Anti-mech and pointblank attacks from
            // hiding may allow the target to respond.
            if ( ea instanceof WeaponAttackAction ) {
                final WeaponAttackAction waa = (WeaponAttackAction) ea;
                final String weaponName = entity.getEquipment
                    ( waa.getWeaponId() ).getType().getInternalName();

                if ( Infantry.SWARM_MEK.equals(weaponName) ||
                     Infantry.LEG_ATTACK.equals(weaponName) ) {

                    // Does the target have any AP Pods available?
                    final Entity target = game.getEntity( waa.getTargetId() );
                    Enumeration misc = target.getMisc();
                    while ( misc.hasMoreElements() ) {
                        final Mounted equip = (Mounted) misc.nextElement();
                        if ( equip.getType().hasFlag(MiscType.F_AP_POD) &&
                             equip.canFire()) {

                            // Yup.  Insert a game turn to handle AP pods.
                            // ASSUMPTION : AP pod declarations come
                            // immediately after the attack declaration.
                            game.insertNextTurn( new GameTurn.TriggerAPPodTurn
                                  ( target.getOwnerId(), target.getId() ) );
                            send(createTurnVectorPacket());

                            // We can stop looking.
                            break;

                        } // end found-available-ap-pod

                    } // Check the next piece of equipment on the target.

                } // End check-for-available-ap-pod
            }

            // The equipment type of a club needs to be restored.
            if (ea instanceof ClubAttackAction) {
                ClubAttackAction caa = (ClubAttackAction) ea;
                Mounted club = caa.getClub();
                club.restore();
            }

            if (ea instanceof PushAttackAction) {
                // push attacks go the end of the displacement attacks
                PushAttackAction paa = (PushAttackAction)ea;
                entity.setDisplacementAttack(paa);
                game.addCharge(paa);
            } else if (ea instanceof DodgeAction) {
                entity.dodging = true;
            } else if (ea instanceof SpotAction) {
                entity.setSpotting(true);
            } else {
                // add to the normal attack list.
                game.addAction(ea);
            }

            // Mark any AP Pod as used in this turn.
            if ( ea instanceof TriggerAPPodAction ) {
                TriggerAPPodAction tapa = (TriggerAPPodAction) ea;
                Mounted pod = entity.getEquipment( tapa.getPodId() );
                pod.setUsedThisRound( true );
            }
        }

        // Unless otherwise stated,
        // this entity is done for the round.
        if ( setDone ) {
            entity.setDone(true);
        }
        entityUpdate(entity.getId());

        // update all players on the attacks.  Don't worry about pushes being a
        // "charge" attack.  It doesn't matter to the client.
        send(createAttackPacket(vector, false));
    }

    /**
     * Auto-target active AMS systems
     */
    private void assignAMS() {

        // sort all missile-based attacks by the target
        Hashtable htAttacks = new Hashtable();
        for (Enumeration i = game.getActions(); i.hasMoreElements(); ) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                Mounted weapon = game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId());

                // Only entities can have AMS.
                if ( Targetable.TYPE_ENTITY != waa.getTargetType() ) {
                    continue;
                }

                // Can only use AMS versus missles.
                if (((WeaponType)weapon.getType()).getDamage() == WeaponType.DAMAGE_MISSILE) {
                    Entity target = game.getEntity(waa.getTargetId());
                    Vector v = (Vector)htAttacks.get(target);
                    if (v == null) {
                        v = new Vector();
                        htAttacks.put(target, v);
                    }
                    v.addElement(waa);
                }
            }
        }

        // let each target assign its AMS
        for (Enumeration i = htAttacks.keys(); i.hasMoreElements(); ) {
            Entity e = (Entity)i.nextElement();
            Vector vAttacks = (Vector)htAttacks.get(e);
            e.assignAMS(vAttacks);
        }
    }

    /** Called during the weapons fire phase.  Resolves anything other than
     * weapons fire that happens.  Torso twists, for example.
     */
    private void resolveAllButWeaponAttacks() {
        if(game.getPhase()==Game.PHASE_FIRING) {
            roundReport.append("\nWeapon Attack Phase\n-------------------\n");
        }

        Vector clearAttempts = new Vector();
        Vector triggerPodActions = new Vector();
        // loop thru actions and handle everything we expect except attacks
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            Entity entity = game.getEntity(ea.getEntityId());
            if (ea instanceof TorsoTwistAction) {
                TorsoTwistAction tta = (TorsoTwistAction)ea;
                if ( entity.canChangeSecondaryFacing() ) {
                    entity.setSecondaryFacing(tta.getFacing());
                }
            }
            else if (ea instanceof FlipArmsAction) {
                FlipArmsAction faa = (FlipArmsAction)ea;
                entity.setArmsFlipped(faa.getIsFlipped());
            }
            else if (ea instanceof FindClubAction) {
                resolveFindClub(entity);
            }
            else if (ea instanceof UnjamAction) {
                resolveUnjam(entity);
            }
            else if (ea instanceof ClearMinefieldAction) {
                clearAttempts.addElement(entity);
            }
            else if (ea instanceof TriggerAPPodAction) {
                TriggerAPPodAction tapa = (TriggerAPPodAction) ea;

                // Don't trigger the same pod twice.
                if ( !triggerPodActions.contains( tapa ) ) {
                    triggerAPPod(entity, tapa.getPodId());
                    triggerPodActions.addElement( tapa );
                } else {
                    System.err.print( "AP Pod #" );
                    System.err.print( tapa.getPodId() );
                    System.err.print( " on " );
                    System.err.print( entity.getDisplayName() );
                    System.err.println(" was already triggered this round!!");
                }
            }
        }

        resolveClearMinefieldAttempts(clearAttempts);
    }

    private void resolveClearMinefieldAttempts(Vector clearAttempts) {
        boolean[] doneWith = new boolean[clearAttempts.size()];

        for (int i = 0; i < clearAttempts.size(); i++) {
            Vector temp = new Vector();
            Entity e = (Entity) clearAttempts.elementAt(i);
            Coords pos = e.getPosition();
            temp.addElement(e);

            for (int j = i + 1; j < clearAttempts.size(); j++) {
                Entity ent = (Entity) clearAttempts.elementAt(j);
                if (ent.getPosition().equals(pos)) {
                    temp.addElement(ent);
                    clearAttempts.removeElement(ent);
                }
            }

            boolean accident = false;
            boolean cleared = false;
            for (int j = 0; j < temp.size(); j++) {
                Entity ent = (Entity) temp.elementAt(j);
                int roll = Compute.d6(2);
                int clear = Minefield.CLEAR_NUMBER_INFANTRY;
                int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;

                // Does the entity has a minesweeper?
                Enumeration equip = ent.getMisc();
                while ( equip.hasMoreElements() ) {
                    Mounted mounted = (Mounted) equip.nextElement();
                    if ( mounted.getType().hasFlag(MiscType.F_MINESWEEPER) ) {
                        clear = Minefield.CLEAR_NUMBER_SWEEPER;
                        boom = Minefield.CLEAR_NUMBER_SWEEPER_ACCIDENT;
                        break;
                    }
                }
                phaseReport.append( ent.getShortName() )
                           .append( " attempts to clear mines in hex " )
                           .append( pos.getBoardNum() )
                           .append( "; needs a " )
                           .append( clear )
                           .append( ", rolls " )
                           .append( roll );
                if (roll >= clear) {
                    phaseReport.append(" and is successful!\n");
                    cleared = true;
                } else if (roll <= boom) {
                    phaseReport.append(" and accidently sets it off!\n");
                    accident = true;
                } else {
                    phaseReport.append(" and fails!\n");
                }
            }
            if (accident) {
                Enumeration minefields = game.getMinefields(pos).elements();
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();
                    switch (mf.getType()) {
                        case (Minefield.TYPE_CONVENTIONAL) :
                        case (Minefield.TYPE_THUNDER) :
                            for (int j = 0; j < temp.size(); j++) {
                                Entity entity = (Entity) temp.elementAt(j);
                                phaseReport.append(entity.getShortName() + " is damaged in minefield accident.");
                                HitData hit = entity.rollHitLocation(Minefield.TO_HIT_TABLE, Minefield.TO_HIT_SIDE);
                                phaseReport.append(damageEntity(entity, hit, mf.getDamage())).append("\n");
                            }
                            break;
                        case (Minefield.TYPE_VIBRABOMB) :
                            explodeVibrabomb(mf);
                            break;
                    }
                }
            }
            if (cleared) {
                Enumeration minefields = game.getMinefields(pos).elements();
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();
                    removeMinefield(mf);
                }
            }
        }
    }

    /**
     * Called during the fire phase to resolve all (and only) weapon attacks
     */
    private void resolveOnlyWeaponAttacks() {
        Vector results = new Vector(game.actionsSize());

        // loop thru received attack actions, getting weapon results
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                results.addElement(preTreatWeaponAttack(waa));
            }
        }

        // loop through weapon results and resolve
        int cen = Entity.NONE;
        for (Enumeration i = results.elements(); i.hasMoreElements();) {
            WeaponResult wr = (WeaponResult)i.nextElement();
            resolveWeaponAttack(wr, cen);
            cen = wr.waa.getEntityId();
        }

        // and clear the attacks Vector
        game.resetActions();
    }

    /**
     * Trigger the indicated AP Pod of the entity.
     *
     * @param   entity the <code>Entity</code> triggering the AP Pod.
     * @param   podId the <code>int</code> ID of the AP Pod.
     */
    private void triggerAPPod( Entity entity, int podId ) {

        // Get the mount for this pod.
        Mounted mount = entity.getEquipment( podId );

        // Confirm that this is, indeed, an AP Pod.
        if ( null == mount ) {
            System.err.print( "Expecting to find an AP Pod at " );
            System.err.print( podId );
            System.err.print( " on the unit, " );
            System.err.print( entity.getDisplayName() );
            System.err.println( " but found NO equipment at all!!!" );
            return;
        }
        EquipmentType equip = mount.getType();
        if ( !(equip instanceof MiscType) ||
             !equip.hasFlag(MiscType.F_AP_POD) ) {
            System.err.print( "Expecting to find an AP Pod at " );
            System.err.print( podId );
            System.err.print( " on the unit, " );
            System.err.print( entity.getDisplayName() );
            System.err.print( " but found " );
            System.err.print( equip.getName() );
            System.err.println( " instead!!!" );
            return;
        }

        // Now confirm that the entity can trigger the pod.
        // Ignore the "used this round" flag.
        boolean oldFired = mount.isUsedThisRound();
        mount.setUsedThisRound( false );
        boolean canFire = mount.canFire();
        mount.setUsedThisRound( oldFired );
        if ( !canFire ) {
            System.err.print( "Can not trigger the AP Pod at " );
            System.err.print( podId );
            System.err.print( " on the unit, " );
            System.err.print( entity.getDisplayName() );
            System.err.println( "!!!" );
            return;
        }

        // Mark the pod as fired and log the action.
        mount.setFired( true );
        phaseReport.append("\n")
            .append( entity.getDisplayName() )
            .append( " triggers an Anti-Personell Pod:" );

        // Walk through ALL entities in the triggering entity's hex.
        Enumeration targets = game.getEntities( entity.getPosition() );
        while ( targets.hasMoreElements() ) {
            final Entity target = (Entity) targets.nextElement();

            // Is this an unarmored infantry platoon?
            if ( target instanceof Infantry &&
                 !(target instanceof BattleArmor) ) {

                // Roll d6-1 for damage.
                final int damage = Compute.d6() - 1;

                // If the platoon took no damage, log it and go no further
                if ( 0 == damage ) {
                    phaseReport.append( "\n        " )
                        .append( target.getDisplayName() )
                        .append( " gets lucky and takes no damage." );
                }
                else {
                    // Damage the platoon.
                    phaseReport.append
                        ( damageEntity( target,
                                        new HitData(Infantry.LOC_INFANTRY),
                                        damage ) );

                    // Damage from AP Pods is applied immediately.
                    target.applyDamage();
                }

            } // End target-is-unarmored

            // Nope, the target is immune.
            // Don't make a log entry for the triggering entity.
            else if ( !entity.equals( target ) ) {
                phaseReport.append( "\n        " )
                    .append( target.getDisplayName() )
                    .append( " is immune and takes no damage." );
            }

        } // Check the next entity in the triggering entity's hex.
    }

    /**
     * Resolve an Unjam Action object
     */
    private void resolveUnjam(Entity entity) {
        final int TN = entity.getCrew().getGunnery() + 3;
        phaseReport.append("\nRAC unjam attempts for " ).append( entity.getDisplayName() ).append( "\n");
        for (Enumeration i = entity.getWeapons(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if(mounted.isJammed()) {
                WeaponType wtype = (WeaponType)mounted.getType();
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    phaseReport.append("  Unjamming " ).append( wtype.getName() ).append( "; needs " ).append( TN ).append( ", ");
                    int roll = Compute.d6(2);
                    phaseReport.append("rolls " ).append( roll ).append( " : ");
                    if(roll >= TN) {
                        phaseReport.append(" Successfully unjammed!\n");
                        mounted.setJammed(false);
                    }
                    else {
                        phaseReport.append(" Still jammed!\n");
                    }
                }
            }
        }
    }

    private void resolveFindClub(Entity entity) {
        EquipmentType clubType = null;

        entity.setFindingClub(true);

        // Get the entity's current hex.
        Coords coords = entity.getPosition();
        IHex curHex = game.board.getHex( coords );

        // Is there a blown off arm in the hex?
        if (curHex.terrainLevel(Terrains.ARMS) > 0) {
            clubType = EquipmentType.get("Limb Club");
            curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, curHex.terrainLevel(Terrains.ARMS)-1));
            sendChangedHex(entity.getPosition());
            phaseReport.append("\n" ).append( entity.getDisplayName() ).append( " picks up a blown-off arm for use as a club.\n");
        }

        // Is there a blown off leg in the hex?
        else if (curHex.terrainLevel(Terrains.LEGS) > 0) {
            clubType = EquipmentType.get("Limb Club");
            curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, curHex.terrainLevel(Terrains.LEGS)-1));
            sendChangedHex(entity.getPosition());
            phaseReport.append("\n" ).append( entity.getDisplayName() ).append( " picks up a blown-off leg for use as a club.\n");
        }

        // Is there the rubble of a medium, heavy,
        // or hardened building in the hex?
        else if ( Building.LIGHT < curHex.terrainLevel( Terrains.RUBBLE ) ) {

            // Finding a club is not guaranteed.  The chances are
            // based on the type of building that produced the
            // rubble.
            boolean found = false;
            int roll = Compute.d6(2);
            switch ( curHex.terrainLevel( Terrains.RUBBLE ) ) {
            case Building.MEDIUM:
                if ( roll >= 7 ) {
                    found = true;
                }
                break;
            case Building.HEAVY:
                if ( roll >= 6 ) {
                    found = true;
                }
                break;
            case Building.HARDENED:
                if ( roll >= 5 ) {
                    found = true;
                }
                break;
            }

            // Let the player know if they found a club.
            if ( found ) {
                clubType = EquipmentType.get("Girder Club");
                phaseReport.append( "\n" )
                    .append( entity.getDisplayName() )
                    .append( " found a girder to use as a club.\n" );
            } else {
                clubType = null;
                phaseReport.append( "\n" )
                    .append( entity.getDisplayName() )
                    .append( " did not find a club, but may have better luck next turn.\n" );
            }
        }

        // Are there woods in the hex?
        else if ( 1 <= curHex.terrainLevel( Terrains.WOODS ) ) {
            clubType = EquipmentType.get("Tree Club");
            phaseReport.append("\n" ).append( entity.getDisplayName() ).append( " uproots a tree for use as a club.\n");
        }

        // add the club
        try {
            if (clubType != null) {
                entity.addEquipment(clubType, Mech.LOC_NONE);
            }
        } catch (LocationFullException ex) {
            // unlikely...
            phaseReport.append("\n" )
                .append( entity.getDisplayName() )
                .append( " did not have room for a club.\n");
        }
    }

    /**
     * Generates a WeaponResult object for a WeaponAttackAction.  Adds heat,
     * depletes ammo, sets weapons used.
     */
    private WeaponResult preTreatWeaponAttack(WeaponAttackAction waa) {
        final Entity ae = game.getEntity(waa.getEntityId());
        final Mounted weapon = ae.getEquipment(waa.getWeaponId());
        final WeaponType wtype = (WeaponType)weapon.getType();
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !wtype.hasFlag(WeaponType.F_INFANTRY);

        Mounted ammo = null;
        if (usesAmmo) {
            if (waa.getAmmoId() > -1) {
                ammo = ae.getEquipment(waa.getAmmoId());
                weapon.setLinked(ammo);
            } else {
                ammo = weapon.getLinked();
            }
        }
        boolean streakMiss;

        WeaponResult wr = new WeaponResult();
        wr.waa = waa;

        // has this weapon fired already?
        if (weapon.isUsedThisRound()) {
            wr.toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon has already been used this round");
            return wr;
        }
        // is the weapon functional?
        if (weapon.isDestroyed()) {
            wr.toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon was destroyed in a previous round");
            return wr;
        }
        // is it jammed?
        if (weapon.isJammed()) {
            wr.toHit = new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon is jammed");
            return wr;
        }
        // make sure ammo is loaded
        if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0 || ammo.isDumping())) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
        }

        // compute to-hit
        wr.toHit = waa.toHit(game);
        
        if (waa.isNemesisConfused()) {
            wr.toHit.addModifier(1, "iNarc Nemesis pod");
        }
        // roll dice
        wr.roll = Compute.d6(2);

        // if the shot is possible and not a streak miss
        // and not a nemesis-confused shot, add heat and use ammo
        streakMiss = (wtype.getAmmoType() == AmmoType.T_SRM_STREAK && wr.roll < wr.toHit.getValue());
        if (wr.toHit.getValue() != TargetRoll.IMPOSSIBLE && !streakMiss &&
            !waa.isNemesisConfused()) {
            wr = addHeatUseAmmoFor(waa, wr);
        }

        // set the weapon as having fired
        weapon.setUsedThisRound(true);

        // if not streak miss, resolve any AMS attacks on this attack
        if (!streakMiss) {
            wr = resolveAmsFor(waa, wr);
        }

        return wr;
    }

    /**
     * Adds heat and uses ammo appropriate for a single attack of this weapon.
     * Call only on a valid attack (and with a streak weapon, only on hits.)
     *
     * @returns modified WeaponResult
     */
    private WeaponResult addHeatUseAmmoFor(WeaponAttackAction waa, WeaponResult wr) {
        final Entity ae = game.getEntity(waa.getEntityId());
        final Mounted weapon = ae.getEquipment(waa.getWeaponId());
        final WeaponType wtype = (WeaponType)weapon.getType();
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !wtype.hasFlag(WeaponType.F_INFANTRY);

        Mounted ammo = weapon.getLinked();

        // how many shots are we firing?
        int nShots = weapon.howManyShots();

        // do we need to revert to single shot?
        if (usesAmmo && nShots > 1) {
            int nAvail = ae.getTotalAmmoOfType((AmmoType)ammo.getType());
            if (nAvail < nShots) {
                wr.revertsToSingleShot = true;
                nShots = 1;
            }
        }

        // use up ammo
        if (usesAmmo) {
            for (int i = 0; i < nShots; i++) {
                if (ammo.getShotsLeft() <= 0) {
                    ae.loadWeapon(weapon);
                    ammo = weapon.getLinked();
                }
                ammo.setShotsLeft(ammo.getShotsLeft() - 1);
            }
        }

        // build up some heat
        ae.heatBuildup += (wtype.getHeat() * nShots);

        return wr;
    }

    /**
     * Resolves any AMS fire for this weapon attack, adding AMS heat, depleting
     * AMS ammo.
     * @returns the appropriately modified WeaponResult
     */
    private WeaponResult resolveAmsFor(WeaponAttackAction waa, WeaponResult wr) {
        final Entity te = game.getEntity(waa.getTargetId());

        // any AMS attacks by the target?
        Vector vCounters = waa.getCounterEquipment();
        if (null != vCounters) {
            // resolve AMS counter-fire
            wr.amsShotDown = new int[vCounters.size()];
            for (int x = 0; x < vCounters.size(); x++) {
                wr.amsShotDown[x] = 0;

                Mounted counter = (Mounted)vCounters.elementAt(x);
                Mounted mAmmo = counter.getLinked();
                if (!(counter.getType() instanceof WeaponType)
                || ((WeaponType)counter.getType()).getAmmoType() != AmmoType.T_AMS
                || !counter.isReady() || counter.isMissing()) {
                    continue;
                }
                // roll hits
                int amsHits = Compute.d6(((WeaponType)counter.getType()).getDamage());

                // build up some heat (assume target is ams owner)
                te.heatBuildup += ((WeaponType)counter.getType()).getHeat();

                // decrement the ammo
                mAmmo.setShotsLeft(Math.max(0, mAmmo.getShotsLeft() - amsHits));

                // set the ams as having fired
                counter.setUsedThisRound(true);

                wr.amsShotDown[x]    = amsHits;
                wr.amsShotDownTotal += amsHits;
            }
        }

        return wr;
    }

    /**
     * Try to ignite the hex, taking into account exisiting fires and the
     * effects of Inferno rounds.
     *
     * @param   c - the <code>Coords</code> of the hex being lit.
     * @param   bInferno - <code>true</code> if the weapon igniting the
     *          hex is an Inferno round.  If some other weapon or ammo
     *          is causing the roll, this should be <code>false</code>.
     * @param   nTargetRoll - the <code>int</code> target number for the
     *          ignition roll.
     * @param   nTargetRoll - the <code>int</code> roll target for the attempt.
     * @param   bReportAttempt - <code>true</code> if the attempt roll should
     *          be added to the report.
     */
    private boolean tryIgniteHex( Coords c, boolean bInferno, int nTargetRoll,
                                  boolean bReportAttempt ) {

        IHex hex = game.board.getHex(c);
        boolean bAnyTerrain = false;

        // Ignore bad coordinates.
        if ( hex == null ) {
            return false;
        }

        // inferno always ignites
        if (bInferno) {
            game.board.addInfernoTo(c, InfernoTracker.STANDARD_ROUND, 1);
            nTargetRoll = 0;
            bAnyTerrain = true;
        }

        // The hex may already be on fire.
        if ( hex.containsTerrain( Terrains.FIRE ) ) {
            if ( bReportAttempt ) {
                phaseReport.append("           The hex is already on fire.\n");
            }
            return true;
        }
        else if ( ignite(hex, nTargetRoll, bAnyTerrain, bReportAttempt) ) {
            phaseReport.append("           The hex ignites!\n");
            sendChangedHex(c);
            return true;
        }
        return false;
    }

    /**
     * Try to ignite the hex, taking into account exisiting fires and the
     * effects of Inferno rounds.  This version of the method will not report
     * the attempt roll.
     *
     * @param   c - the <code>Coords</code> of the hex being lit.
     * @param   bInferno - <code>true</code> if the weapon igniting the
     *          hex is an Inferno round.  If some other weapon or ammo
     *          is causing the roll, this should be <code>false</code>.
     * @param   nTargetRoll - the <code>int</code> roll target for the attempt.
     */
   private boolean tryIgniteHex(Coords c, boolean bInferno, int nTargetRoll) {
       return tryIgniteHex(c, bInferno, nTargetRoll, false);
   }

    private void tryClearHex(Coords c, int nTarget) {
        IHex h = game.board.getHex(c);
        int woods = h.terrainLevel(Terrains.WOODS);
        if (woods == ITerrain.LEVEL_NONE) {
            phaseReport.append("      Woods already cleared.\n" );
        } else {
            int woodsRoll = Compute.d6(2);
            phaseReport.append("      Checking to clear woods; needs " )
                .append( nTarget ).append( ", rolls " )
                .append( woodsRoll ).append( ": ");
            if(woodsRoll >= nTarget) {
                if(woods > 1) {
                    h.removeTerrain(Terrains.WOODS);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.WOODS, woods - 1));
                    phaseReport.append(" Heavy Woods converted to Light Woods!\n");
                }
                else if(woods == 1) {
                    h.removeTerrain(Terrains.WOODS);
                    h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ROUGH, 1));
                    phaseReport.append(" Light Woods converted to Rough!\n");
                }
                sendChangedHex(c);
            } else {
                phaseReport.append(" fails!\n");
            }
        }
    }

    private void resolveWeaponAttack(WeaponResult wr, int lastEntityId) {
        resolveWeaponAttack(wr, lastEntityId, false);
    }
    
    /**
     * Resolve a single Weapon Attack object
     * @param wr The <code>WeaponResult</code> to resolve
     * @param lastEntityId The <code>int</code> ID of the last
     *        resolved weaponattack's attacking entity
     * @param isNemesisConfused The <code>boolean</code> value of wether
     *        this attack is one caused by homing in on a iNarc Nemesis pod
     *        and so should not be further diverted
     * @return wether we hit or not, only needed for nnemesis pod stuff
     */
    private boolean resolveWeaponAttack(WeaponResult wr, int lastEntityId, boolean isNemesisConfused) {
      // If it's an artillery shot, the shooting entity
      // might have died in the meantime
      Entity ae = game.getEntity( wr.waa.getEntityId() );
      if (ae == null) {
          ae = game.getOutOfGameEntity( wr.waa.getEntityId() );
      }
      final Targetable target = game.getTarget(wr.waa.getTargetType(),
                                               wr.waa.getTargetId());
      Entity entityTarget = null;
      if (target.getTargetType() == Targetable.TYPE_ENTITY) {
        entityTarget = (Entity) target;
      }
      final Mounted weapon = ae.getEquipment(wr.waa.getWeaponId());
      final WeaponType wtype = (WeaponType) weapon.getType();
      final boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
      // 2002-09-16 Infantry weapons have unlimited ammo.
      final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
          wtype.getAmmoType() != AmmoType.T_BA_MG &&
          wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
          !isWeaponInfantry;
      Mounted ammo = usesAmmo ? weapon.getLinked() : null;
      final AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();
      Infantry platoon = null;
      final boolean isBattleArmorAttack = wtype.hasFlag(WeaponType.F_BATTLEARMOR);
      ToHitData toHit = wr.toHit;
      boolean bInferno = (usesAmmo &&
                          atype.getMunitionType() == AmmoType.M_INFERNO);
      boolean bFragmentation = (usesAmmo &&
                                atype.getMunitionType() == AmmoType.M_FRAGMENTATION);
      boolean bFlechette = (usesAmmo &&
                            atype.getMunitionType() == AmmoType.M_FLECHETTE);
      boolean bArtillery = target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY;
      boolean glancing = false; // For Glancing Hits Rule
      int glancingMissileMod = 0;
      int glancingCritMod = 0;

      if (!bInferno) {
        // also check for inferno infantry
        bInferno = (isWeaponInfantry && wtype.hasFlag(WeaponType.F_INFERNO));
      }
      final boolean targetInBuilding =
          Compute.isInBuilding(game, entityTarget);
      if(bArtillery && game.getPhase()==Game.PHASE_FIRING) { //if direct artillery
          wr.artyAttackerCoords=ae.getPosition();
      }

      // Which building takes the damage?
      Building bldg = game.board.getBuildingAt(target.getPosition());
      
      // Are we iNarc Nemesis Confusable?
      boolean isNemesisConfusable = false;
      Mounted mLinker = weapon.getLinkedBy();
      if ( wtype.getAmmoType() == AmmoType.T_ATM ||
           ( mLinker != null &&
             mLinker.getType() instanceof MiscType &&
             !mLinker.isDestroyed() && !mLinker.isMissing() && !mLinker.isBreached() &&
             mLinker.getType().hasFlag(MiscType.F_ARTEMIS) ) ) {
          if ((!weapon.getType().hasModes()
               || !weapon.curMode().equals("Indirect")) &&
              (atype.getMunitionType() == AmmoType.M_STANDARD ||
               atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE ||
               atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) ) {
              isNemesisConfusable = true;
          }
      } else if (wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_SRM) {
          if (usesAmmo && atype.getMunitionType() == AmmoType.M_NARC_CAPABLE) {
              isNemesisConfusable = true;
          }
      }

      if (lastEntityId != ae.getId()) {
          phaseReport.append("\nWeapons fire for ").append(ae.getDisplayName()).
              append("\n");
      }
      
      // Swarming infantry can stop during any weapons phase after start.
      if (Infantry.STOP_SWARM.equals(wtype.getInternalName())) {
          // ... but only as their *only* attack action.
          if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
              phaseReport.append("Swarm attack can not be ended (" +
                   toHit.getDesc()).append(")\n");
              return true;
          } else {
              phaseReport.append("Swarm attack ended.\n");
              // Only apply the "stop swarm 'attack'" to the swarmed Mek.
              if (ae.getSwarmTargetId() != target.getTargetId()) {
                  Entity other = game.getEntity(ae.getSwarmTargetId());
                  other.setSwarmAttackerId(Entity.NONE);
              } else {
                  entityTarget.setSwarmAttackerId(Entity.NONE);
              }
              ae.setSwarmTargetId(Entity.NONE);
              return true;
          }
      }

      // Report weapon attack and its to-hit value.
      phaseReport.append("    ").append(wtype.getName()).append(" at ").append(
          target.getDisplayName());
      
      boolean shotAtNemesisTarget = false;
      // check for nemesis
      if (isNemesisConfusable && !isNemesisConfused) {
          // loop through nemesis targets
          for (Enumeration e = game.getNemesisTargets(ae, target.getPosition());e.hasMoreElements();) {
              Entity entity = (Entity)e.nextElement();
              phaseReport.append(", but a friendly unit with an attached iNarc Nemesis Pod is standing in the way!\n");
              weapon.setUsedThisRound(false);
              WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                  entity.getTargetId(), wr.waa.getWeaponId());
              newWaa.setNemesisConfused(true);
              WeaponResult newWr = preTreatWeaponAttack(newWaa);
              // attack the new target, and if we hit it, return;
              if (resolveWeaponAttack(newWr, ae.getId(), true)) return true;
              shotAtNemesisTarget = true;
          }
      }
      if (shotAtNemesisTarget) {
          phaseReport.append("\n    Now targeting original Target again");
      }
      if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
          phaseReport.append(", but the shot is impossible (").append(toHit.getDesc()).
              append(")\n");
          return false;
      } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
          phaseReport.append(", the shot is an automatic miss (").append(toHit.
              getDesc()).append("), ");
      } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
          phaseReport.append(", the shot is an automatic hit (").append(toHit.
              getDesc()).append("), ");
      } else {
          phaseReport.append("; needs ").append(toHit.getValue()).append(", ");
      }

      // if firing an HGR unbraced, schedule a PSR
      if (wtype.getAmmoType() == AmmoType.T_GAUSS_HEAVY && ae.mpUsed > 0) {
          // the mod is weight-based
          int nMod;
          if (ae.getWeight() <= Entity.WEIGHT_LIGHT) {
              nMod = 2;
          } else if (ae.getWeight() <= Entity.WEIGHT_MEDIUM) {
              nMod = 1;
          } else if (ae.getWeight() <= Entity.WEIGHT_HEAVY) {
              nMod = 0;
          } else {
              nMod = -1;
          }
          PilotingRollData psr = new PilotingRollData(ae.getId(), nMod,
                                          "fired HeavyGauss unbraced");
          psr.setCumulative(false);
          game.addPSR(psr);
      }

      // dice have been rolled, thanks
      phaseReport.append("rolls ").append(wr.roll).append(" : ");

      // check for AC jams
      int nShots = weapon.howManyShots();
      if (nShots > 1) {
          int jamCheck = 0;
          if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA &&
              weapon.curMode().equals("Ultra")) {
              jamCheck = 2;
          } else if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
              if (nShots == 2) {
                  jamCheck = 2;
              } else if (nShots == 4) {
                  jamCheck = 3;
              } else if (nShots == 6) {
                  jamCheck = 4;
              }
          }

          if (jamCheck > 0 && wr.roll <= jamCheck) {
              // ultras are destroyed by jamming
              if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                  phaseReport.append("misses AND THE AUTOCANNON JAMS.\n");
                  weapon.setJammed(true);
                  weapon.setHit(true);
              } else {
                  phaseReport.append("misses BECAUSE THE RAC JAMS.\n");
                  weapon.setJammed(true);
              }
              return true;
          }
      }

      // Resolve roll for disengaged field inhibitors on PPCs, if needed
      if (game.getOptions().booleanOption("maxtech_ppc_inhibitors")
          && wtype.hasModes()
          && weapon.curMode().equals("Field Inhibitor OFF") ) {
          int rollTarget = 0;
          int dieRoll = Compute.d6(2);
          int distance = Compute.effectiveDistance(game, ae, target);

          if (distance>=3) {
              rollTarget = 3;
          } else if (distance == 2) {
              rollTarget = 6;
          } else if (distance == 1) {
              rollTarget = 10;
          }
          phaseReport.append("\n    Fired PPC without field inhibitor, checking for damage:\n");
          phaseReport.append("    Needs ");
          phaseReport.append(rollTarget);
          phaseReport.append(" to avoid damage, rolls ");
          phaseReport.append(dieRoll);
          phaseReport.append(": ");
          if (dieRoll<rollTarget) {
              // Oops, we ruined our day...
              int wlocation = weapon.getLocation();
              int wid = ae.getEquipmentNum(weapon);
              int slot = 0;
              weapon.setDestroyed (true);
              for (int i=0; i<ae.getNumberOfCriticals(wlocation); i++) {
                  CriticalSlot slot1 = ae.getCritical (wlocation, i);
                  if (slot1 == null || slot1.getType() != CriticalSlot.TYPE_SYSTEM) {
                      continue;
                  }
                  Mounted mounted = ae.getEquipment(slot1.getIndex());
                  if (mounted.equals(weapon)) {
                      ae.hitAllCriticals(wlocation,i);
                  }
              }
              // Bug 1066147 : damage is *not* like an ammo explosion,
              //        but it *does* get applied directly to the IS.
              phaseReport.append( "fails." )
                  .append( damageEntity(ae, new HitData(wlocation),
                                        10, false, 0, true) )
                  .append( "\n    (continuing hit report):" );
          } else {
              phaseReport.append("Succeeds.\n    ");
          }
      }

      // do we hit?
      boolean bMissed = wr.roll < toHit.getValue();
      if (game.getOptions().booleanOption("maxtech_glancing_blows")) {
        if (wr.roll == toHit.getValue()) {
            glancing = true;
            glancingMissileMod = -4;
            glancingCritMod = -2;
            phaseReport.append(" - Glancing Blow - ");
        } else {
            glancing = false;
            glancingMissileMod = 0;
            glancingCritMod = 0;
        }
      } else {
        glancing = false;
        glancingMissileMod = 0;
        glancingCritMod = 0;
      }
      
      // special case minefield delivery, no damage and scatters if misses.
      if (target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER) {
        Coords coords = target.getPosition();
        if (!bMissed) {
          phaseReport.append("hits the intended hex ")
              .append(coords.getBoardNum())
              .append("\n");
        }
        else {
          coords = Compute.scatter(coords, game.getOptions().booleanOption("margin_scatter_distance")?toHit.getValue()-wr.roll:-1);
          if (game.board.contains(coords)) {
            phaseReport.append("misses and scatters to hex ")
                .append(coords.getBoardNum())
                .append("\n");
          }
          else {
            phaseReport.append("misses and scatters off the board\n");
            return !bMissed;
          }
        }

        // Handle the thunder munitions.
        if (atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED) {
          deliverThunderAugMinefield(coords, ae.getOwner().getId(),
                                     atype.getRackSize());
        }
        else if (atype.getMunitionType() == AmmoType.M_THUNDER) {
          deliverThunderMinefield(coords, ae.getOwner().getId(),
                                  atype.getRackSize());
        }
        else if (atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO)
          deliverThunderInfernoMinefield(coords, ae.getOwner().getId(),
                                         atype.getRackSize());
        else if (atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)
          deliverThunderVibraMinefield(coords, ae.getOwner().getId(),
                                       atype.getRackSize(),
                                       wr.waa.getOtherAttackInfo());
        else if (atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE)
          deliverThunderActiveMinefield(coords, ae.getOwner().getId(),
                                        atype.getRackSize());
          //else
          //{
          //...This is an error, but I'll just ignore it for now.
          //}
        return !bMissed;
      }
      // FASCAM Artillery
      if (target.getTargetType() == Targetable.TYPE_HEX_FASCAM) {
          Coords coords = target.getPosition();
          if (!bMissed) {
              phaseReport.append("hits the intended hex ")
                  .append(coords.getBoardNum())
                  .append("\n");
          }
          else {
              coords = Compute.scatter(coords, (game.getOptions().booleanOption("margin_scatter_distance"))?(toHit.getValue()-wr.roll):-1);
              if (game.board.contains(coords)) {
                  phaseReport.append("misses and scatters to hex ")
                      .append(coords.getBoardNum())
                      .append("\n");
              }
              else {
                  phaseReport.append("misses and scatters off the board\n");
                  return !bMissed;
              }
          }
          if (game.board.contains(coords)) {
              deliverFASCAMMinefield(coords, ae.getOwner().getId());
          }
          return !bMissed;
      }
      // Vibrabomb-IV Artillery
      if (target.getTargetType() == Targetable.TYPE_HEX_VIBRABOMB_IV) {
          Coords coords = target.getPosition();
          if (!bMissed) {
              phaseReport.append("hits the intended hex ")
                  .append(coords.getBoardNum())
                  .append("\n");
          }
          else {
              coords = Compute.scatter(coords, (game.getOptions().booleanOption("margin_scatter_distance"))?(toHit.getValue()-wr.roll):-1);
              if (game.board.contains(coords)) {
                  phaseReport.append("misses and scatters to hex ")
                      .append(coords.getBoardNum())
                      .append("\n");
              }
              else {
                  phaseReport.append("misses and scatters off the board\n");
              }
          }
          if (game.board.contains(coords)) {
              deliverThunderVibraMinefield(coords, ae.getOwner().getId(), 20,
                                           wr.waa.getOtherAttackInfo());
          }
          return !bMissed;
      }
      // Inferno IV artillery
      if (target.getTargetType() == Targetable.TYPE_HEX_INFERNO_IV) {
          Coords coords = target.getPosition();
          if (!bMissed) {
              phaseReport.append("hits the intended hex ")
                  .append(coords.getBoardNum())
                  .append("\n");
          }
          else {
              coords = Compute.scatter(coords, (game.getOptions().booleanOption("margin_scatter_distance"))?(toHit.getValue()-wr.roll):-1);
              if (game.board.contains(coords)) {
                  phaseReport.append("misses and scatters to hex ")
                      .append(coords.getBoardNum())
                      .append("\n");
              }
              else {
                  phaseReport.append("misses and scatters off the board\n");
                  return !bMissed;
              }
          }
          IHex h = game.getBoard().getHex(coords);
          //Unless there is a fire in the hex already, start one.
          if ( !h.containsTerrain( Terrains.FIRE ) ) {
              phaseReport.append( "       Fire started in hex " )
                  .append( coords.getBoardNum() )
                  .append( ".\n" );
              h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
          }
          game.board.addInfernoTo( coords, InfernoTracker.INFERNO_IV_ROUND, 1 );
          sendChangedHex(coords);
          for(Enumeration impactHexHits = game.getEntities(coords);impactHexHits.hasMoreElements();) {
              Entity entity = (Entity)impactHexHits.nextElement();
              entity.infernos.add( InfernoTracker.INFERNO_IV_ROUND, 1 );
              phaseReport.append( entity.getDisplayName() )
                  .append( " now on fire for ")
                  .append( entity.infernos.getTurnsLeftToBurn() )
                  .append(" turns.\n");
          }
          for(int dir=0;dir<=5;dir++) {
              Coords tempcoords=coords.translated(dir);
              if(!game.board.contains(tempcoords)) {
                  continue;
              }
              if(coords.equals(tempcoords)) {
                  continue;
              }
              h = game.getBoard().getHex(tempcoords);
              // Unless there is a fire in the hex already, start one.
              if ( !h.containsTerrain( Terrains.FIRE ) ) {
                  phaseReport.append( "       Fire started in hex " )
                      .append( tempcoords.getBoardNum() )
                      .append( ".\n" );
                  h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
              }
              game.board.addInfernoTo( tempcoords, InfernoTracker.INFERNO_IV_ROUND, 1 );
              sendChangedHex(tempcoords);
              for(Enumeration splashHexHits = game.getEntities(tempcoords);splashHexHits.hasMoreElements();) {
                  Entity entity = (Entity)splashHexHits.nextElement();
                  entity.infernos.add( InfernoTracker.INFERNO_IV_ROUND, 1 );
                  phaseReport.append( entity.getDisplayName() )
                      .append( " now on fire for ")
                      .append( entity.infernos.getTurnsLeftToBurn() )
                      .append(" turns.\n");
              }
          }
          return !bMissed;
      }
      //special case artillery
      if (target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY) {
        Coords coords = target.getPosition();
        if (!bMissed) {
          phaseReport.append("hits the intended hex ")
              .append(coords.getBoardNum())
              .append("\n");
        }
        else {
          coords = Compute.scatter(coords, (game.getOptions().booleanOption("margin_scatter_distance"))?(toHit.getValue()-wr.roll):-1);
          if (game.board.contains(coords)) {
            phaseReport.append("misses and scatters to hex ")
                .append(coords.getBoardNum())
                .append("\n");
          }
          else {
            phaseReport.append("misses and scatters off the board\n");
            return !bMissed;
          }
        }

        int nCluster = 5;
        
        int ratedDamage = wtype.getRackSize();
        bldg = null;
        bldg = game.board.getBuildingAt(coords);
        int bldgAbsorbs = (bldg != null)? bldg.getPhaseCF() / 10 : 0;
        bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
        ratedDamage -= bldgAbsorbs;
        if ((bldg != null) && (bldgAbsorbs > 0)) {
            phaseReport.append("The building in the hex absorbs " + bldgAbsorbs + "damage from the artillery strike!\n");
            phaseReport.append(damageBuilding(bldg, ratedDamage));   
        }

        for(Enumeration impactHexHits = game.getEntities(coords);impactHexHits.hasMoreElements();) {
            Entity entity = (Entity)impactHexHits.nextElement();
            int hits = ratedDamage;
            
            while(hits>0) {
                if(wr.artyAttackerCoords!=null) {
                    toHit.setSideTable(Compute.targetSideTable(wr.artyAttackerCoords,entity.getPosition(),entity.getFacing(),entity instanceof Tank));
                }
                HitData hit = entity.rollHitLocation
                    ( toHit.getHitTable(),
                      toHit.getSideTable(),
                      wr.waa.getAimedLocation(),
                      wr.waa.getAimingMode() );

                phaseReport.append(damageEntity(entity, hit, Math.min(nCluster, hits), false, 0, false, true) + "\n");
                hits -= Math.min(nCluster,hits);
            }

        }
        
        for(int dir=0;dir<=5;dir++) {
            Coords tempcoords=coords.translated(dir);
            if(!game.board.contains(tempcoords)) {
                continue;
            }
            if(coords.equals(tempcoords)) {
                continue;

            }
            
            ratedDamage = wtype.getRackSize()/2;
            bldg = null;
            bldg = game.board.getBuildingAt(tempcoords);
            bldgAbsorbs = (bldg != null)? bldg.getPhaseCF() / 10 : 0;
            bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
            ratedDamage -= bldgAbsorbs;
            if ((bldg != null) && (bldgAbsorbs > 0)) {
                phaseReport.append("The building in the hex absorbs " + bldgAbsorbs + "damage from the artillery strike!\n");
                phaseReport.append(damageBuilding(bldg, ratedDamage));   
            }
            
            Enumeration splashHexHits = game.getEntities(tempcoords);
            if(splashHexHits.hasMoreElements()) {
                phaseReport.append("in hex " + tempcoords.getBoardNum());
            }
            for(;splashHexHits.hasMoreElements();) {
                Entity entity = (Entity)splashHexHits.nextElement();
                int hits = ratedDamage;
                while(hits>0) {
                    HitData hit = entity.rollHitLocation
                        ( toHit.getHitTable(),
                          toHit.getSideTable(),
                          wr.waa.getAimedLocation(),
                          wr.waa.getAimingMode() );
                    phaseReport.append(damageEntity(entity, hit, Math.min(nCluster, hits)) + "\n");
                    hits -= Math.min(nCluster,hits);
                }
            }
        }
        return !bMissed;
      } // End artillery
      int ammoUsage=0;
      int nDamPerHit = wtype.getDamage();
      if (bMissed) {
          // Report the miss.
          // MGs in rapidfire do heat even when they miss.
          if (weapon.isRapidfire() &&
              !(target instanceof Infantry &&
              !(target instanceof BattleArmor)) ){
              // Check for rapid fire Option. Only MGs can be rapidfire.
              nDamPerHit = Compute.d6();
              ammoUsage = 3*nDamPerHit;
              for (int i=0; i<ammoUsage; i++) {
                  if (ammo.getShotsLeft() <= 0) {
                      ae.loadWeapon(weapon);
                      ammo = weapon.getLinked();
                  }
                  ammo.setShotsLeft(ammo.getShotsLeft()-1);
              }
              if (ae instanceof Mech) {
                  // Apply heat
                  ae.heatBuildup += nDamPerHit;
              }
          }
                 if ( wtype.getAmmoType() == AmmoType.T_SRM_STREAK ) {
                phaseReport.append( "fails to achieve lock.\n" );
            } else {
                phaseReport.append("misses");
                if (weapon.isRapidfire() &&
                    !(target instanceof Infantry &&
                    !(target instanceof BattleArmor)) ){
                    phaseReport.append (" using ").append(ammoUsage).append(" shots.");
                }
                phaseReport.append(".\n");
            }

            // Report any AMS action.
            for (int i=0; i < wr.amsShotDown.length; i++) {
                if (wr.amsShotDown[i] > 0) {
                    phaseReport.append( "\tAMS activates, firing " )
                        .append( wr.amsShotDown[i] )
                        .append( " shot(s).\n" );
                }
            }

            // Figure out the maximum number of missile hits.
            // TODO: handle this in a different place.
            int maxMissiles = 0;
            if ( usesAmmo ) {
                maxMissiles = wtype.getRackSize();
                if ( wtype.hasFlag(WeaponType.F_DOUBLE_HITS) ) {
                    maxMissiles *= 2;
                }
                if ( ae instanceof BattleArmor ) {
                    platoon = (Infantry) ae;
                    maxMissiles *= platoon.getShootingStrength();
                }
            }

            // If the AMS shot down *all* incoming missiles, if
            // the shot is an automatic failure, or if it's from
            // a Streak rack, then Infernos can't ignite the hex
            // and any building is safe from damage.
            if ( (usesAmmo && wr.amsShotDownTotal >= maxMissiles) ||
                 toHit.getValue() == TargetRoll.AUTOMATIC_FAIL ||
                 wtype.getAmmoType() == AmmoType.T_SRM_STREAK ) {
                return !bMissed;
            }

            // Shots that miss an entity can set fires.
            // Infernos always set fires.  Otherwise
            // Buildings can't be accidentally ignited,
            // and some weapons can't ignite fires.
            if ( entityTarget != null &&
                 ( bInferno ||
                   ( bldg == null &&
                     wtype.getFireTN() != TargetRoll.IMPOSSIBLE ) ) ) {
                tryIgniteHex(target.getPosition(), bInferno, 11);
            }

            // BMRr, pg. 51: "All shots that were aimed at a target inside
            // a building and miss do full damage to the building instead."
            if ( !targetInBuilding ) {
                return !bMissed;
            }
        }

        // special case NARC hits.  No damage, but a beacon is appended
        if (!bMissed &&
            wtype.getAmmoType() == AmmoType.T_NARC &&
            atype.getMunitionType() != AmmoType.M_NARC_EX) {

            if (wr.amsShotDownTotal > 0) {
                phaseReport.append("would hit, but...");
                for (int i=0; i < wr.amsShotDown.length; i++) {
                    phaseReport.append("\n\tAMS engages, firing ")
                        .append(wr.amsShotDown[i]).append(" shots");
                }
                phaseReport.append("\nThe pod is destroyed by AMS fire.\n");
            } else if (entityTarget == null) {
                phaseReport.append("hits, but doesn't do anything.\n");
            } else {
                entityTarget.setNarcedBy(ae.getOwner().getTeam());
                phaseReport.append("hits.  Pod attached.\n");
            }
            return !bMissed;
        }
        
        // special case iNARC hits.  No damage, but a beacon is appended
        if (!bMissed &&
            wtype.getAmmoType() == AmmoType.T_INARC &&
            atype.getMunitionType() != AmmoType.M_EXPLOSIVE) {

            if (wr.amsShotDownTotal > 0) {
                phaseReport.append("would hit, but...");
                for (int i=0; i < wr.amsShotDown.length; i++) {
                    phaseReport.append("\n\tAMS engages, firing ")
                        .append(wr.amsShotDown[i]).append(" shots");
                }
                phaseReport.append("\nThe pod is destroyed by AMS fire.\n");
            } else if (entityTarget == null) {
                phaseReport.append("hits, but doesn't do anything.\n");
            } else {
                INarcPod pod = null;
                switch (atype.getMunitionType()) {
                case AmmoType.M_ECM:
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.ECM );
                    phaseReport.append("hits.  ECM Pod attached.\n");
                    break;
                case AmmoType.M_HAYWIRE:
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.HAYWIRE );
                    phaseReport.append("hits.  Haywire Pod attached.\n");
                    break;
                case AmmoType.M_NEMESIS:
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.NEMESIS );
                    phaseReport.append("hits.  Nemesis Pod attached.\n");
                    break;
                default:
                    pod = new INarcPod( ae.getOwner().getTeam(),
                                        INarcPod.HOMING );
                    phaseReport.append("hits.  Homing Pod attached.\n");
                }
                entityTarget.attachINarcPod(pod);
            }
            return !bMissed;
        }

        // attempt to clear minefield by LRM/MRM fire.
        if (!bMissed && target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR) {
            int clearAttempt = Compute.d6(2);

            if (clearAttempt >= Minefield.CLEAR_NUMBER_WEAPON) {
                phaseReport.append("\n\thits and clears it.\n");
                Coords coords = target.getPosition();

                Enumeration minefields = game.getMinefields(coords).elements();
                while (minefields.hasMoreElements()) {
                    Minefield mf = (Minefield) minefields.nextElement();

                    removeMinefield(mf);
                }
            } else {
                phaseReport.append("\n\thits, but fails to clear it.\n");
            }
            return !bMissed;
        }

        // yeech.  handle damage. . different weapons do this in very different ways
        int hits = 1, nCluster = 1, nSalvoBonus = 0;
        boolean bSalvo = false;
        // ecm check is heavy, so only do it once
        boolean bCheckedECM = false;
        boolean bECMAffected = false;
        boolean bMekStealthActive = false;
        String sSalvoType = " shot(s) ";
        boolean bAllShotsHit = false;
        int nRange = ae.getPosition().distance(target.getPosition());
        int nMissilesModifier = 0;
        boolean maxtechmissiles = game.getOptions().booleanOption("maxtech_mslhitpen");
        if (maxtechmissiles) {
            if (nRange<=1) {
                nMissilesModifier = +1;
            } else if (nRange <= wtype.getShortRange()) {
                nMissilesModifier = 0;
            } else if (nRange <= wtype.getMediumRange()) {
                nMissilesModifier = -1;
            } else {
                nMissilesModifier = -2;
            }
       }
        // All shots fired by a Streak SRM weapon, during
        // a Mech Swarm hit, or at an adjacent building.
        if ( wtype.getAmmoType() == AmmoType.T_SRM_STREAK ||
             wtype.getAmmoType() == AmmoType.T_NARC ||
             ae.getSwarmTargetId() == wr.waa.getTargetId() ||
             ( ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
                 target.getTargetType() == Targetable.TYPE_BUILDING ) &&
               ae.getPosition().distance( target.getPosition() ) <= 1 ) ) {
            bAllShotsHit = true;
        }

        // Mek swarms attach the attacker to the target.
        if ( !bMissed && Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
            // Is the target already swarmed?
            if ( Entity.NONE != entityTarget.getSwarmAttackerId() ) {
                phaseReport.append( "succeds, but the defender is " );
                phaseReport.append( "already swarmed by another unit.\n" );
            }
            // Did the target get destroyed by weapons fire?
            else if ( entityTarget.isDoomed() || entityTarget.isDestroyed() ||
                      entityTarget.getCrew().isDead() ) {
                phaseReport.append( "succeds, but the defender was " );
                phaseReport.append( "destroyed by weapons fire.\n" );
            } else {
                phaseReport.append( "succeeds!  Defender swarmed.\n" );
                ae.setSwarmTargetId( wr.waa.getTargetId() );
                entityTarget.setSwarmAttackerId( wr.waa.getEntityId() );
            }
            return !bMissed;
        }

        // Magnetic Mine Launchers roll number of hits on battle armor
        // hits table but use # mines firing instead of men shooting.
        else if ( wtype.getInternalName().equals(BattleArmor.MINE_LAUNCHER) ) {
            hits = nShots;
            if ( !bAllShotsHit ) {
                hits = Compute.getBattleArmorHits( hits );
            }
            bSalvo = true;
            sSalvoType = " mine(s) ";
        }

        // Other battle armor attacks use # of men firing to determine hits.
        // Each hit can be in a new location. The damage per shot comes from
        // the "racksize", or from the ammo, for ammo weapons
        else if ( isBattleArmorAttack ) {
            bSalvo = true;
            platoon = (Infantry) ae;
            nCluster = 1;
            if (usesAmmo) {
                nDamPerHit = atype.getDamagePerShot();
            }
            nDamPerHit = wtype.getRackSize();
            hits = platoon.getShootingStrength();
            // All attacks during Mek Swarms hit; all
            // others use the Battle Armor hits table.
            if ( !bAllShotsHit ) {
                hits = Compute.getBattleArmorHits( hits );
            }

            // Handle Inferno SRM squads.
            if (bInferno) {
                nCluster = hits;
                nDamPerHit = 0;
                sSalvoType = " Inferno missle(s) ";
                bSalvo = false;
            }

        }

        // Infantry damage depends on # men left in platoon.
        else if (isWeaponInfantry) {
            bSalvo = true;
            platoon = (Infantry)ae;
            nCluster = 5;
            nDamPerHit = 1;
            hits = platoon.getDamage(platoon.getShootingStrength());

            // Handle Inferno SRM infantry.
            if (bInferno) {
                nCluster = hits;
                nDamPerHit = 0;
                sSalvoType = " Inferno missle(s) ";
                bSalvo = false;
            }
        } else if (wtype.getDamage() == WeaponType.DAMAGE_MISSILE ||
                   wtype.hasFlag(WeaponType.F_MISSILE_HITS) ) {
            bSalvo = true;

            // Weapons with ammo type T_BA_MG or T_BA_SMALL_LASER
            // don't have an atype object.
            if ( wtype.getAmmoType() == AmmoType.T_BA_MG ||
                 wtype.getAmmoType() == AmmoType.T_BA_SMALL_LASER ) {
                nDamPerHit = Math.abs( wtype.getAmmoType() );
            } else {
                sSalvoType = " missile(s) ";
                // Get the damage from the linked ammo.
                nDamPerHit = atype.getDamagePerShot();
            }

            if ( wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_MRM ||
                 wtype.getAmmoType() == AmmoType.T_ATM ||
                 wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER ) {
                nCluster = 5;
            }

            // calculate # of missiles hitting
            if ( wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_SRM ||
                 wtype.getAmmoType() == AmmoType.T_ATM ) {

                // check for artemis, else check for narc
                mLinker = weapon.getLinkedBy();
                if ( wtype.getAmmoType() == AmmoType.T_ATM ||
                     ( mLinker != null &&
                       mLinker.getType() instanceof MiscType &&
                       !mLinker.isDestroyed() && !mLinker.isMissing() && !mLinker.isBreached() &&
                       mLinker.getType().hasFlag(MiscType.F_ARTEMIS) ) ) {

                    // check ECM interference
                    if (!bCheckedECM) {
                        // Attacking Meks using stealth suffer ECM effects.
                        if ( ae instanceof Mech ) {
                            bMekStealthActive = ae.isStealthActive();
                        }
                        bECMAffected = Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition());
                        bCheckedECM = true;
                    }
                    // also no artemis for IDF, and only use standard ammo (excepot for ATMs)
                    if (!bECMAffected&& !bMekStealthActive
                        && (!weapon.getType().hasModes()
                            || !weapon.curMode().equals("Indirect"))
                        && (atype.getMunitionType() == AmmoType.M_STANDARD
                            || atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE
                            || atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) ) {
                        nSalvoBonus += 2;
                    }
                } else if (entityTarget != null && 
                        (entityTarget.isNarcedBy(ae.getOwner().getTeam()) || 
                         entityTarget.isINarcedBy(ae.getOwner().getTeam()))) {
                    // check ECM interference
                    if (!bCheckedECM) {
                        // Attacking Meks using stealth suffer ECM effects.
                        if ( ae instanceof Mech ) {
                            bMekStealthActive = ae.isStealthActive();
                        }
                        bECMAffected = Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition());
                        bCheckedECM = true;
                    }
                    // only apply Narc bonus if we're not suffering ECM effect
                    // and we are using standard ammo.
                    if (!bECMAffected && !bMekStealthActive && atype.getMunitionType() == AmmoType.M_NARC_CAPABLE) {
                        nSalvoBonus += 2;
                    }
                }
            }

            // If dealing with Inferno rounds set damage to zero and reset
            // all salvo bonuses (cannot mix with other special munitions).
            if (bInferno) {
                    nDamPerHit = 0;
                    nSalvoBonus = 0;
                    sSalvoType = " inferno missile(s) ";
                    bSalvo = false;
            }

            // If dealing with fragmentation missiles,
            // it does double damage to infantry...
            if (bFragmentation) {
                sSalvoType = " fragmentation missile(s) ";
            }

            // Large MRM missile racks roll twice.
            // MRM missiles never recieve hit bonuses.
            if ( wtype.getRackSize() == 30 || wtype.getRackSize() == 40 ) {
                hits = Compute.missilesHit(wtype.getRackSize() / 2, nMissilesModifier+glancingMissileMod, maxtechmissiles | glancing) +
                    Compute.missilesHit(wtype.getRackSize() / 2, nMissilesModifier+glancingMissileMod, maxtechmissiles | glancing);
            }

            // Battle Armor units multiply their racksize by the number
            // of men shooting and they can't get missile hit bonuses.
            else if ( ae instanceof BattleArmor ) {
                platoon = (Infantry) ae;
                int temp = wtype.getRackSize() * platoon.getShootingStrength();

                // Do all shots hit?
                if ( bAllShotsHit ) {
                    hits = temp;
                } else {
                    // Account for more than 20 missles hitting.
                    hits = 0;
                    while ( temp > 20 ) {
                        hits += Compute.missilesHit( 20, nMissilesModifier+glancingMissileMod, maxtechmissiles | glancing );
                        temp -= 20;
                    }
                    hits += Compute.missilesHit( temp, nMissilesModifier+glancingMissileMod, maxtechmissiles | glancing );
                } // End not-all-shots-hit
            }

            // If all shots hit, use the full racksize.
            else if ( bAllShotsHit ) {
                hits = wtype.getRackSize();
            }

            // In all other circumstances, roll for hits.
            else {
                hits = Compute.missilesHit(wtype.getRackSize(), nSalvoBonus + nMissilesModifier + glancingMissileMod, maxtechmissiles | glancing);
            }

            // Advanced SRMs may get additional missiles
            if ( usesAmmo &&
                 atype.getAmmoType() == AmmoType.T_SRM_ADVANCED) {
                int tmp = wtype.getRackSize() * platoon.getShootingStrength();
                if (hits%2 == 1 && hits < tmp) {
                    hits++;
                }
            }

        } else if (usesAmmo && atype.getMunitionType() == AmmoType.M_CLUSTER) {
            // Cluster shots break into single point clusters.
            bSalvo = true;
            hits = wtype.getRackSize();
            if ( !bAllShotsHit ) {
                if (!glancing) {
                    hits = Compute.missilesHit( hits );
                } else {
                    // if glancing blow, half the number of missiles that hit,
                    // that halves damage. do this, and not adjust number of 
                    // pellets, because maxtech only talks about missile weapons
                    hits = Compute.missilesHit(hits)/2;
                }
            }
            nDamPerHit = 1;
        } else if (nShots > 1) {
            // this should handle multiple attacks from ultra and rotary ACs
            bSalvo = true;
            hits = nShots;
            if ( !bAllShotsHit ) {
                hits = Compute.missilesHit( hits );
            }
        }
        else if (wtype.getAmmoType() == AmmoType.T_GAUSS_HEAVY) {
            // HGR does range-dependent damage
            if (nRange <= wtype.getShortRange()) {
                nDamPerHit = 25;
            } else if (nRange <= wtype.getMediumRange()) {
                nDamPerHit = 20;
            } else {
                nDamPerHit = 10;
            }
        } else if (wtype.hasFlag(WeaponType.F_ENERGY)) {
            // Check for Altered Damage from Energy Weapons (MTR, pg.22)
            nDamPerHit = wtype.getDamage();
            if (game.getOptions().booleanOption("maxtech_altdmg")) {
                if (nRange<=1) {
                    nDamPerHit++;
                } else if (nRange <= wtype.getMediumRange()) {
                    // Do Nothing for Short and Medium Range
                } else if (nRange <= wtype.getLongRange()) {
                    nDamPerHit--;
                } else if (nRange <= wtype.getExtremeRange()) {
                    nDamPerHit = (int)Math.floor((double)nDamPerHit/2.0);
                }
            }
        } else if (weapon.isRapidfire() &&
                   !(target instanceof Infantry &&
                     !(target instanceof BattleArmor)) ){
            // Check for rapid fire Option. Only MGs can be rapidfire.
            nDamPerHit = Compute.d6();
            ammoUsage = 3*nDamPerHit;
            for (int i=0; i<ammoUsage; i++) {
                if (ammo.getShotsLeft() <= 0) {
                    ae.loadWeapon(weapon);
                    ammo = weapon.getLinked();
                }
                ammo.setShotsLeft(ammo.getShotsLeft()-1);
            }
            if (ae instanceof Mech) {
                // Apply heat
                ae.heatBuildup += nDamPerHit;
            }
        }
        // only halve damage for non-missiles and non-cluster,
        // because cluster lbx gets handled above.
        if (glancing && !wtype.hasFlag(WeaponType.F_MISSILE) && !wtype.hasFlag(WeaponType.F_MISSILE_HITS) &&
                !(usesAmmo && atype.getMunitionType() == AmmoType.M_CLUSTER)) {
            nDamPerHit = (int)Math.floor((double)nDamPerHit/2.0);
        }

        // Some weapons double the number of hits scored.
        if ( wtype.hasFlag(WeaponType.F_DOUBLE_HITS) ) {
            hits *= 2;
        }

        // We've calculated how many hits.  At this point, any missed
        // shots damage the building instead of the target.
        if ( bMissed ) {
            if ( targetInBuilding && bldg != null ) {

                // Reduce the number of hits by AMS hits.
                if (wr.amsShotDownTotal > 0) {
                    for (int i=0; i < wr.amsShotDown.length; i++) {
                        int shotDown = Math.min(wr.amsShotDown[i], hits);
                        phaseReport.append("\tAMS shoots down ")
                                .append(shotDown).append(" missile(s).\n");
                    }
                    hits -= wr.amsShotDownTotal;
                }

                // Is the building hit by Inferno rounds?
                if ( bInferno && hits > 0 ) {

                    // start a fire in the targets hex
                    Coords c = target.getPosition();
                    IHex h = game.getBoard().getHex(c);

                    // Is there a fire in the hex already?
                    if ( h.containsTerrain( Terrains.FIRE ) ) {
                        phaseReport.append( "        " )
                            .append( hits )
                            .append( " Inferno rounds added to hex " )
                            .append( c.getBoardNum() )
                            .append( ".\n" );
                    } else {
                        phaseReport.append( "        " )
                            .append( hits )
                            .append( " Inferno rounds start fire in hex " )
                            .append( c.getBoardNum() )
                            .append( ".\n" );
                        h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                    }
                    game.board.addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, hits );
                    sendChangedHex(c);

                }

                // Damage the building in one big lump.
                else {

                    // Only report if damage was done to the building.
                    int toBldg = hits * nDamPerHit;
                    if ( toBldg > 0 ) {
                        phaseReport.append( "        " )
                            .append( damageBuilding( bldg, toBldg ) )
                            .append( "\n" );
                    }

                } // End rounds-hit

            } // End missed-target-in-building
            return !bMissed;

        } // End missed-target

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if ( targetInBuilding && bldg != null ) {
            bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
        }

        // All attacks (except from infantry weapons)
        // during Mek Swarms hit the same location.
        if ( !isWeaponInfantry &&
             ae.getSwarmTargetId() == wr.waa.getTargetId() ) {
            nCluster = hits;
        }

        // Battle Armor MGs do one die of damage per hit to PBI.
        if ( wtype.getAmmoType() == AmmoType.T_BA_MG &&
             (target instanceof Infantry) &&
             !(target instanceof BattleArmor) ) {

            // ASSUMPTION: Building walls protect infantry from BA MGs.
            if ( bldgAbsorbs > 0 ) {
                int toBldg = nDamPerHit * hits;
                phaseReport.append( hits )
                    .append( sSalvoType )
                    .append( "hit,\n        but " )
                    .append( damageBuilding( bldg,
                               Math.min( toBldg, bldgAbsorbs ),
                               " absorbs the shots, taking " ) )
                    .append( "\n" );
                return !bMissed;
            }
            nDamPerHit = Compute.d6(hits);
            phaseReport.append( "riddles the target with " ).append(
                nDamPerHit ).append( sSalvoType ).append( "and " );
            hits = 1;
        }

        // Mech and Vehicle MGs do *DICE* of damage to PBI.
        else if (usesAmmo && atype.hasFlag(AmmoType.F_MG) &&
                  !isWeaponInfantry && (target instanceof Infantry) &&
                  !(target instanceof BattleArmor) && !weapon.isRapidfire()) {

            int dice = wtype.getDamage();

            // A building may absorb the entire shot.
            if ( nDamPerHit <= bldgAbsorbs ) {
                int toBldg = nDamPerHit * hits;
                int curCF = bldg.getCurrentCF();
                curCF = Math.min( curCF, toBldg );
                bldg.setCurrentCF( curCF );
                if ( bSalvo ) {
                    phaseReport.append( hits )
                        .append( sSalvoType )
                        .append( "hit,\n" );
                } else{
                    phaseReport.append( "hits,\n" );
                }
                phaseReport.append( "        but " )
                    .append( damageBuilding( bldg,
                               Math.min( toBldg, bldgAbsorbs ),
                               " absorbs the shots, taking " ) )
                    .append( "\n" );
                return !bMissed;
            }

            // If a building absorbs partial damage, reduce the dice of damage.
            else if ( bldgAbsorbs > 0 ) {
                dice -= bldgAbsorbs;
            }

            nDamPerHit = Compute.d6( dice );
            phaseReport.append( "riddles the target with " ).append(
                nDamPerHit ).append( sSalvoType ).append( ".\n" );
            bSalvo = true;

            // If a building absorbed partial damage, report it now
            // instead of later and then clear the variable.
            if ( bldgAbsorbs > 0 ) {
                phaseReport.append( "        " )
                    .append( damageBuilding( bldg, bldgAbsorbs ) );
                bldgAbsorbs = 0;
            }

        }

        // Report the number of hits.  Infernos have their own reporting
        else if (bSalvo && !bInferno) {
            phaseReport.append( hits ).append( sSalvoType ).append( "hit" )
                .append( toHit.getTableDesc() );
            if (bECMAffected) {
                phaseReport.append(" (ECM prevents bonus)");
            }
            else if (bMekStealthActive) {
                phaseReport.append(" (active Stealth prevents bonus)");
            }
            if (nSalvoBonus > 0) {
                phaseReport.append(" (w/ +")
                    .append(nSalvoBonus)
                    .append(" bonus)");
            }
            phaseReport.append(".");

            if (wr.amsShotDownTotal > 0) {
                for (int i=0; i < wr.amsShotDown.length; i++) {
                    int shotDown = Math.min(wr.amsShotDown[i], hits);
                    phaseReport.append("\n\tAMS engages, firing ")
                        .append(wr.amsShotDown[i]).append(" shots, shooting down ")
                        .append(shotDown).append(" missile(s).");
                }
                hits -= wr.amsShotDownTotal;

                phaseReport.append("\n    ");
                if (hits < 1) {
                    phaseReport.append("AMS shoots down all incoming missiles!");
                } else {
                    phaseReport.append(hits);
                    if (1 == hits) {
                        phaseReport.append(" missile gets through.");
                    } else {
                        phaseReport.append(" missiles get through.");
                    };
                };
            }
        }

        // convert the ATM missile damages to LRM type 5 point cluster damage
        // done here after AMS has been performed
        if (wtype.getAmmoType() == AmmoType.T_ATM)
        {
            hits = nDamPerHit * hits;
            nDamPerHit = 1;
        }

        // Make sure the player knows when his attack causes no damage.
        if ( hits == 0 ) {
            phaseReport.append( "attack deals zero damage.\n" );
        }

        // for each cluster of hits, do a chunk of damage
        while (hits > 0) {
            int nDamage;

            // If the attack was with inferno rounds then
            // do heat and fire instead of damage.
            if ( bInferno ) {
                // AMS can shoot down infernos, too.
                if (wr.amsShotDownTotal > 0) {
                    for (int i=0; i < wr.amsShotDown.length; i++) {
                        int shotDown = Math.min(wr.amsShotDown[i], hits);
                        phaseReport.append("\n\tAMS engages, firing ")
                                .append(wr.amsShotDown[i]).append(" shots, shooting down ")
                                .append(shotDown).append(" missile(s).");
                    }
                    hits -= wr.amsShotDownTotal;

                    phaseReport.append("\n    ");
                    if (hits < 1) {
                        phaseReport.append("AMS shoots down all incoming missiles!");
                    } else {
                        phaseReport.append(hits);
                        if (1 == hits) {
                            phaseReport.append(" missile gets through.");
                        } else {
                            phaseReport.append(" missiles get through.");
                        };
                    };
                    if ( hits <= 0 ) {
                        continue;
                    }
                }

                // targeting a hex for ignition
                if( target.getTargetType() == Targetable.TYPE_HEX_IGNITE ||
                    target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {

                    phaseReport.append( "hits with " )
                        .append( hits )
                        .append( " inferno missles.\n" );

                    // Unless there a fire in the hex already, start one.
                    Coords c = target.getPosition();
                    IHex h = game.getBoard().getHex(c);
                    if ( !h.containsTerrain( Terrains.FIRE ) ) {
                        phaseReport.append( " Fire started in hex " )
                            .append( c.getBoardNum() )
                            .append( ".\n" );
                        h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                    }
                    game.board.addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, hits );
                    sendChangedHex(c);

                    return !bMissed;
                }

                // Targeting an entity
                if (entityTarget != null ) {
                    entityTarget.infernos.add( InfernoTracker.STANDARD_ROUND,
                                               hits );
                    phaseReport.append( "hits with " )
                        .append( hits )
                        .append( " inferno missles." );
                    phaseReport.append("\n        " )
                        .append( target.getDisplayName() )
                        .append( " now on fire for ")
                        .append( entityTarget.infernos.getTurnsLeftToBurn() )
                        .append(" turns.\n");

                    // start a fire in the targets hex
                    Coords c = target.getPosition();
                    IHex h = game.getBoard().getHex(c);

                    // Unless there a fire in the hex already, start one.
                    if ( !h.containsTerrain( Terrains.FIRE ) ) {
                        phaseReport.append( " Fire started in hex " )
                            .append( c.getBoardNum() )
                            .append( ".\n" );
                        h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
                    }
                    game.board.addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, 1 );
                    sendChangedHex(c);

                    return !bMissed;
                }

            } // End is-inferno

            // targeting a hex for igniting
            if( target.getTargetType() == Targetable.TYPE_HEX_IGNITE ||
                target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {
                if ( !bSalvo ) {
                    phaseReport.append("hits!");
                }
                // We handle Inferno rounds above.
                int tn = wtype.getFireTN();
                if (tn != TargetRoll.IMPOSSIBLE) {
                    if ( bldg != null ) {
                        tn += bldg.getType() - 1;
                    }
                    phaseReport.append( "\n" );
                    tryIgniteHex( target.getPosition(), bInferno, tn, true );
                }
                return !bMissed;
            }

            // targeting a hex for clearing
            if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {

                nDamage = nDamPerHit * hits;
                if ( !bSalvo ) {
                    phaseReport.append("hits!");
                }
                if (ae instanceof Infantry) {
                    phaseReport.append("\n    But infantry cannot try to clear hexes!\n");
                    return !bMissed;
                }


                phaseReport.append("    Terrain takes " ).append( nDamage ).append( " damage.\n");

                // Any clear attempt can result in accidental ignition, even
                // weapons that can't normally start fires.  that's weird.
                // Buildings can't be accidentally ignited.
                if ( bldg == null) {
                    boolean alreadyIgnited = game.board.getHex(target.getPosition()).containsTerrain(Terrains.FIRE);
                    boolean ignited = tryIgniteHex(target.getPosition(), bInferno, 9);
                    if (!alreadyIgnited && ignited) return !bMissed;
                }

                int tn = 14 - nDamage;
                tryClearHex(target.getPosition(), tn);

                return !bMissed;
            }

            // Targeting a building.
            if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

                // The building takes the full brunt of the attack.
                nDamage = nDamPerHit * hits;
                if ( !bSalvo ) {
                    phaseReport.append( "hits." );
                }
                phaseReport.append( "\n        " )
                    .append( damageBuilding( bldg, nDamage ) )
                    .append( "\n" );

                // Damage any infantry in the hex.
                this.damageInfantryIn( bldg, nDamage );

                // And we're done!
                return !bMissed;
            }

            // Battle Armor squads equipped with fire protection
            // gear automatically avoid flaming death.
            if ( wtype.hasFlag(WeaponType.F_FLAMER) &&
                 target instanceof BattleArmor ) {

                for ( Enumeration iter = entityTarget.getMisc();
                      iter.hasMoreElements(); ) {
                    Mounted mount = (Mounted) iter.nextElement();
                    EquipmentType equip = mount.getType();
                    if ( BattleArmor.FIRE_PROTECTION.equals
                         (equip.getInternalName()) ) {
                        if ( !bSalvo ) {
                            phaseReport.append( "hits." );
                        }
                        phaseReport.append( "\n        However, " )
                            .append(target.getDisplayName() )
                            .append( " has fire protection gear so no damage is done.\n" );

                        // A building may be damaged, even if the squad is not.
                        if ( bldgAbsorbs > 0 ) {
                            int toBldg = nDamPerHit * Math.min( bldgAbsorbs,
                                                                hits );
                            phaseReport.append( "        " )
                                .append( damageBuilding( bldg, toBldg ) )
                                .append( "\n" );
                        }

                     return !bMissed;
                    }
                }
            } // End target-may-be-immune

            // Flamers do heat to mechs instead damage if the option is
            // available and the mode is set.
            if ( entityTarget != null &&
                 (entityTarget instanceof Mech) &&
                 wtype.hasFlag(WeaponType.F_FLAMER) &&
                 game.getOptions().booleanOption("flamer_heat") &&
                 wtype.hasModes() &&
                 weapon.curMode().equals("Heat") ) {
                nDamage = nDamPerHit * hits;
                if ( !bSalvo ) {
                    phaseReport.append( "hits." );
                }
                phaseReport.append("\n        Target gains ").append(nDamage).append(" more heat during heat phase.");
                entityTarget.heatBuildup += nDamage;
                hits = 0;
            }
            else if (entityTarget != null) {
                 HitData hit = entityTarget.rollHitLocation
                     ( toHit.getHitTable(),
                       toHit.getSideTable(),
                       wr.waa.getAimedLocation(),
                       wr.waa.getAimingMode() );

                // If a leg attacks hit a leg that isn't
                // there, then hit the other leg.
                if ( wtype.getInternalName().equals("LegAttack") &&
                     entityTarget.getInternal(hit) <= 0 ) {
                    if ( hit.getLocation() == Mech.LOC_RLEG ) {
                        hit = new HitData( Mech.LOC_LLEG );
                    }
                    else {
                        hit = new HitData( Mech.LOC_RLEG );
                    }
                }

                // Mine Launchers automatically hit the
                // CT of a Mech or the front of a Tank.
                if ( wtype.getInternalName()
                     .equals(BattleArmor.MINE_LAUNCHER) ) {
                    if ( target instanceof Mech ) {
                        hit = new HitData( Mech.LOC_CT );
                    }
                    else { // te instanceof Tank
                        hit = new HitData( Tank.LOC_FRONT );
                    }
                }

                // Each hit in the salvo get's its own hit location.
                if (!bSalvo) {
                    phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).
                            append( entityTarget.getLocationAbbr(hit));
                    if (hit.hitAimedLocation()) {
                        phaseReport.append("(hit aimed location)");
                    }
                }

                // Special weapons do criticals instead of damage.
                if ( nDamPerHit == WeaponType.DAMAGE_SPECIAL ) {
                    // Do criticals.
                    String specialDamage = criticalEntity( entityTarget, hit.getLocation() );

                    // Replace "no effect" results with 4 points of damage.
                    if ( specialDamage.endsWith(" no effect.") ) {
                        // ASSUMPTION: buildings CAN'T absorb *this* damage.
                        specialDamage = damageEntity(entityTarget, hit, 4);
                    }
                    else {
                        specialDamage = "\n" + specialDamage;
                    }

                    // Report the result
                    phaseReport.append( specialDamage );
                }
                else {
                    // Resolve damage normally.
                    nDamage = nDamPerHit * Math.min(nCluster, hits);

                    // A building may be damaged, even if the squad is not.
                    if ( bldgAbsorbs > 0 ) {
                        int toBldg = Math.min( bldgAbsorbs, nDamage );
                        nDamage -= toBldg;
                        phaseReport.append( "\n        " )
                            .append( damageBuilding( bldg, toBldg ) );
                    }

                    // A building may absorb the entire shot.
                    if ( nDamage == 0 ) {
                        phaseReport.append( "\n        " )
                            .append( entityTarget.getDisplayName() )
                            .append( " suffers no damage." );
                    } else if (bFragmentation) {
                        // If it's a frag missile...
                        if (glancing) {
                            hit.makeGlancingBlow();
                        }
                        phaseReport.append
                            ( damageEntity(entityTarget, hit, nDamage, false, 1) );
                    } else if (bFlechette) {
                        // If it's a frag missile...
                        if (glancing) {
                            hit.makeGlancingBlow();
                        }
                        phaseReport.append
                            ( damageEntity(entityTarget, hit, nDamage, false, 2) );
                    } else {
                        if (usesAmmo && (atype.getMunitionType() == AmmoType.M_ARMOR_PIERCING))
                            hit.makeArmorPiercing(atype);
                        if (glancing) {
                            hit.makeGlancingBlow();
                        }
                        phaseReport.append
                            ( damageEntity(entityTarget, hit, nDamage) );
                    }
                }
                hits -= nCluster;
                creditKill(entityTarget, ae);
            }
        } // Handle the next cluster.

        phaseReport.append("\n");
        return !bMissed;
    }

    /**
     * Handle all physical attacks for the round
     */
    private void resolvePhysicalAttacks() {
        roundReport.append("\nPhysical Attack Phase\n-------------------\n");

        // add any pending charges
        for (Enumeration i = game.getCharges(); i.hasMoreElements();) {
            game.addAction((EntityAction)i.nextElement());
        }
        game.resetCharges();

        // remove any duplicate attack declarations
        cleanupPhysicalAttacks();

        // loop thru received attack actions
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            Object o = i.nextElement();

            // verify that the attacker is still active
            AttackAction aa = (AttackAction)o;
            if (!(game.getEntity(aa.getEntityId()).isActive())
            && !(o instanceof DfaAttackAction)) {
                continue;
            }
            AbstractAttackAction aaa = (AbstractAttackAction)o;
            physicalResults.addElement(preTreatPhysicalAttack(aaa));
        }
        int cen = Entity.NONE;
        for (Enumeration i = physicalResults.elements(); i.hasMoreElements();) {
            PhysicalResult pr = (PhysicalResult)i.nextElement();
            resolvePhysicalAttack(pr, cen);
            cen = pr.aaa.getEntityId();
        }
        physicalResults.removeAllElements();
    }

    /**
     * Cleans up the attack declarations for the physical phase by removing
     * all attacks past the first for any one mech.  Also clears out attacks
     * by dead or disabled mechs.
     */
    private void cleanupPhysicalAttacks() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            removeDuplicateAttacks(entity.getId());
        }
        removeDeadAttacks();
    }

    /**
     * Removes any actions in the attack queue beyond the first by the
     * specified entity.
     */
    private void removeDuplicateAttacks(int entityId) {
        boolean attacked = false;
        Vector toKeep = new Vector(game.actionsSize());

        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction action = (EntityAction)i.nextElement();
            if (action.getEntityId() != entityId) {
                toKeep.addElement(action);
            } else if (!attacked) {
                toKeep.addElement(action);
                attacked = true;
            } else {
                System.err.println("server: removing duplicate phys attack for id#" + entityId);
            }
        }

        // reset actions and re-add valid elements
        game.resetActions();
        for (Enumeration i = toKeep.elements(); i.hasMoreElements();) {
            game.addAction((EntityAction)i.nextElement());
        }
    }

    /**
     * Removes all attacks by any dead entities.  It does this by going through
     * all the attacks and only keeping ones from active entities.  DFAs are
     * kept even if the pilot is unconscious, so that he can fail.
     */
    private void removeDeadAttacks() {
        Vector toKeep = new Vector(game.actionsSize());

        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction action = (EntityAction)i.nextElement();
            Entity entity = game.getEntity(action.getEntityId());
            if (entity != null && !entity.isDestroyed()
            && (entity.isActive() || action instanceof DfaAttackAction)) {
                toKeep.addElement(action);
            }
        }

        // reset actions and re-add valid elements
        game.resetActions();
        for (Enumeration i = toKeep.elements(); i.hasMoreElements();) {
            game.addAction((EntityAction)i.nextElement());
        }
    }

    /**
     * Handle a punch attack
     */
    private void resolvePunchAttack(PhysicalResult pr, int lastEntityId) {
        final PunchAttackAction paa = (PunchAttackAction)pr.aaa;
        final Entity ae = game.getEntity(paa.getEntityId());
        final Targetable target = game.getTarget(paa.getTargetType(), paa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final String armName = paa.getArm() == PunchAttackAction.LEFT
        ? "Left Arm" : "Right Arm";
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = paa.getArm() == PunchAttackAction.LEFT
        ? pr.damage : pr.damageRight;
        final ToHitData toHit = paa.getArm() == PunchAttackAction.LEFT
        ? pr.toHit : pr.toHitRight;
        int roll = paa.getArm() == PunchAttackAction.LEFT
        ? pr.roll : pr.rollRight;
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( target.getPosition() );

        if (lastEntityId != paa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }

        phaseReport.append("    Punch (" +armName ).append( ") at " ).append( target.getDisplayName());

//        // should we even bother?
//        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
//            phaseReport.append(" but the target is already destroyed!\n");
//            return;
//        }
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the punch is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the punch is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
            if (glancing) phaseReport.append(" - Glancing Blow - ");
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            phaseReport.append("misses.\n");

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    phaseReport.append( "  " )
                        .append( damageBuilding( bldg, damage ) )
                        .append( "\n" );
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            phaseReport.append( "hits.\n  " )
                .append( damageBuilding( bldg, damage ) )
                .append( "\n" );

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).append( te.getLocationAbbr(hit));

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            phaseReport.append( "\n  " )
                .append( damageBuilding( bldg, toBldg ) );
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            phaseReport.append( "\n  " )
                .append( te.getDisplayName() )
                .append( " suffers no damage." );
        } else {
            if (glancing) {
                damage = (int)Math.floor((double)damage/2.0);
            }
            phaseReport.append( damageEntity(te, hit, damage) );
        }


        phaseReport.append("\n");
    }

    /**
     * Handle a kick attack
     */
    private void resolveKickAttack(PhysicalResult pr, int lastEntityId) {
        KickAttackAction kaa = (KickAttackAction)pr.aaa;
        final Entity ae = game.getEntity(kaa.getEntityId());
        final Targetable target = game.getTarget(kaa.getTargetType(), kaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final String legName = kaa.getLeg() == KickAttackAction.LEFT
        ? "Left Leg"
        : "Right Leg";
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( target.getPosition() );

        if (lastEntityId != ae.getId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }

        phaseReport.append("    Kick (" ).append( legName ).append( ") at " ).append( target.getDisplayName());

//        // should we even bother?
//        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
//            phaseReport.append(" but the target is already destroyed!\n");
//            return;
//        }
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the kick is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a kick"));
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the kick is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
            if (glancing) phaseReport.append(" - Glancing Blow - ");
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            phaseReport.append("misses.\n");
            game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a kick"));

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    phaseReport.append( "  " )
                        .append( damageBuilding( bldg, damage ) )
                        .append( "\n" );
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            phaseReport.append( "hits.\n  " )
                .append( damageBuilding( bldg, damage ) )
                .append( "\n" );

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).append( te.getLocationAbbr(hit));

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            phaseReport.append( "\n  " )
                .append( damageBuilding( bldg, toBldg ) );
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            phaseReport.append( "\n  " )
                .append( te.getDisplayName() )
                .append( " suffers no damage." );
        } else {
            if (glancing) {
                damage = (int)Math.floor((double)damage/2.0);
            }
            phaseReport.append( damageEntity(te, hit, damage) );
        }

        if (te.getMovementType() == Entity.MovementType.BIPED || te.getMovementType() == Entity.MovementType.QUAD) {
            PilotingRollData kickPRD = new PilotingRollData(te.getId(), getKickPushPSRMod(ae, te, 0), "was kicked");
            kickPRD.setCumulative(false); // see Bug# 811987 for more info
            game.addPSR(kickPRD);
        }

        phaseReport.append("\n");
    }

    /**
     * Handle a Protomech physicalattack
     */

    private void resolveProtoAttack(PhysicalResult pr, int lastEntityId) {
        final ProtomechPhysicalAttackAction ppaa = (ProtomechPhysicalAttackAction)pr.aaa;
        final Entity ae = game.getEntity(ppaa.getEntityId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final Targetable target = game.getTarget(ppaa.getTargetType(), ppaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( target.getPosition() );

        if (lastEntityId != ae.getId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }

        phaseReport.append("    Protomech physical attack" ).append( " at " ).append( target.getDisplayName());

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the attack is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the attack is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
            if (glancing) phaseReport.append(" - Glancing Blow - ");
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            phaseReport.append("misses.\n");

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    phaseReport.append( "  " )
                        .append( damageBuilding( bldg, damage ) )
                        .append( "\n" );
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            phaseReport.append( "hits.\n  " )
                .append( damageBuilding( bldg, damage ) )
                .append( "\n" );

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).append( te.getLocationAbbr(hit));

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            phaseReport.append( "\n  " )
                .append( damageBuilding( bldg, toBldg ) );
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            phaseReport.append( "\n  " )
                .append( te.getDisplayName() )
                .append( " suffers no damage." );
        } else {
            if (glancing) {
                damage = (int)Math.floor((double)damage/2.0);
            }
            phaseReport.append( damageEntity(te, hit, damage) );
        }

        phaseReport.append("\n");
    }


    /**
     * Handle a brush off attack
     */
    private void resolveBrushOffAttack( PhysicalResult pr,
                                        int lastEntityId ) {
        final BrushOffAttackAction baa = (BrushOffAttackAction)pr.aaa;
        final Entity ae = game.getEntity(baa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target
        // of a "brush off", but iNarc pods **are**.
        Targetable target = game.getTarget( baa.getTargetType(),
                                            baa.getTargetId() );
        Entity te = null;
        final String armName = baa.getArm() == BrushOffAttackAction.LEFT
            ? "Left Arm" : "Right Arm";

        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = game.getEntity(baa.getTargetId());
        }

        // get damage, ToHitData and roll from the PhysicalResult
        // ASSUMPTION: buildings can't absorb *this* damage.
        int damage = baa.getArm() == BrushOffAttackAction.LEFT
        ? pr.damage : pr.damageRight;
        final ToHitData toHit = baa.getArm() == BrushOffAttackAction.LEFT
        ? pr.toHit : pr.toHitRight;
        int roll = baa.getArm() == BrushOffAttackAction.LEFT
        ? pr.roll : pr.rollRight;

        if (lastEntityId != baa.getEntityId()) {
            phaseReport.append( "\nPhysical attacks for " )
                .append( ae.getDisplayName() )
                .append( "\n" );
        }

        phaseReport.append("    Brush Off " )
            .append( target.getDisplayName() )
            .append( " with " )
            .append( armName );

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append( ", but the brush off is impossible (" )
                .append( toHit.getDesc() )
                .append( ")\n" );
            return;
        }
        phaseReport.append("; needs ").append(toHit.getValue()).append(", ");

        // report the roll
        phaseReport.append("rolls ").append(roll).append(" : ");

        // do we hit?
        if (roll < toHit.getValue()) {
            phaseReport.append("misses.\n");

            // Missed Brush Off attacks cause punch damage to the attacker.
            toHit.setHitTable( ToHitData.HIT_PUNCH );
            toHit.setSideTable( ToHitData.SIDE_FRONT );
            HitData hit = ae.rollHitLocation( toHit.getHitTable(),
                                              toHit.getSideTable() );
            phaseReport.append( ae.getDisplayName() )
                .append( " punches itself in the " )
                .append( ae.getLocationAbbr(hit) )
                .append( damageEntity(ae, hit, damage) )
                .append("\n");
            return;
        }

        // Different target types get different handling.
        switch ( target.getTargetType() ) {
        case Targetable.TYPE_ENTITY:
            // Handle Entity targets.
            HitData hit = te.rollHitLocation( toHit.getHitTable(),
                                              toHit.getSideTable() );
            phaseReport.append("hits")
                .append( toHit.getTableDesc() )
                .append( " " )
                .append( te.getLocationAbbr(hit) );
            phaseReport.append(damageEntity(te, hit, damage));

            phaseReport.append("\n");

            // Dislodge the swarming infantry.
            ae.setSwarmAttackerId( Entity.NONE );
            te.setSwarmTargetId( Entity.NONE );
            phaseReport.append( te.getDisplayName() )
                .append( " is dislodged.\n" );
            break;
        case Targetable.TYPE_INARC_POD:
            // Handle iNarc pod targets.
            // TODO : check the return code and handle false appropriately.
            ae.removeINarcPod( (INarcPod) target );
//             // TODO : confirm that we don't need to update the attacker. //killme
//             entityUpdate( ae.getId() ); // killme
            phaseReport.append( target.getDisplayName() )
                .append( " is destroyed.\n" );
            break;
            // TODO : add a default: case and handle it appropriately.
        }
    }

    /**
     * Handle a thrash attack
     */
    private void resolveThrashAttack( PhysicalResult pr,
                                        int lastEntityId ) {
        final ThrashAttackAction taa = (ThrashAttackAction)pr.aaa;
        final Entity ae = game.getEntity(taa.getEntityId());

        // get damage, ToHitData and roll from the PhysicalResult
        int hits = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // PLEASE NOTE: buildings are *never* the target of a "thrash".
        final Entity te = game.getEntity(taa.getTargetId());

        if (lastEntityId != taa.getEntityId()) {
            phaseReport.append( "\nPhysical attacks for " )
                .append( ae.getDisplayName() )
                .append( "\n" );
        }

        phaseReport.append("    Thrash at " )
            .append( te.getDisplayName() );

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append( ", but the thrash is impossible (" )
                .append( toHit.getDesc() )
                .append( ")\n" );
            return;
        }

        // Thrash attack may hit automatically
        if ( toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS ) {
            phaseReport.append( "; hits automatically," );
        } else {
            phaseReport.append( "; needs " )
                .append( toHit.getValue() )
                .append( ", " );

            // report the roll
            phaseReport.append("rolls ").append(roll).append(" : ");

            // do we hit?
            if (roll < toHit.getValue()) {
                phaseReport.append("misses.\n");
                return;
            }
            phaseReport.append( ", hits" );
        }

        // Standard damage loop in 5 point clusters.
        if (glancing) {
            hits = (int)Math.floor((double)hits/2.0);
        }
        phaseReport.append( " and deals " )
            .append( hits)
            .append( " points of damage in 5 point clusters.");
        if (glancing) phaseReport.append (" (Glancing Blow) ");
        while ( hits > 0 ) {
            int damage = Math.min(5, hits);
            hits -= damage;
            HitData hit = te.rollHitLocation( toHit.getHitTable(),
                                              toHit.getSideTable() );
            phaseReport.append("\nHits ").append( te.getLocationAbbr(hit) );
            phaseReport.append(damageEntity(te, hit, damage));
        }
        phaseReport.append("\n");

        // Thrash attacks cause PSRs.  Failed PSRs cause falling damage.
        // This fall damage applies even though the Thrashing Mek is prone.
        PilotingRollData rollData = ae.getBasePilotingRoll();
        rollData.addModifier( 0, "thrashing at infantry" );
        phaseReport.append( ae.getDisplayName() )
            .append( " must make a piloting skill check (" )
            .append( "thrashing at infantry).\n");
        final int diceRoll = Compute.d6(2);
        phaseReport.append("Needs " )
            .append( rollData.getValueAsString() )
            .append( " [" )
            .append( rollData.getDesc() )
            .append( "]" )
            .append( ", rolls " )
            .append( diceRoll )
            .append( " : " );
        if (diceRoll < rollData.getValue()) {
            phaseReport.append("fails.\n");
            doEntityFall( ae, rollData );
        } else {
            phaseReport.append("succeeds.\n");
        }

    }

    /**
     * Handle a club attack
     */
    private void resolveClubAttack(PhysicalResult pr, int lastEntityId) {
        final ClubAttackAction caa = (ClubAttackAction)pr.aaa;
        final Entity ae = game.getEntity(caa.getEntityId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        final Targetable target = game.getTarget(caa.getTargetType(), caa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( target.getPosition() );

        // restore club attack
        caa.getClub().restore();

        if (lastEntityId != caa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }

        phaseReport.append("    " ).append( caa.getClub().getName() ).append( " attack on " ).append( target.getDisplayName());

//        // should we even bother?
//        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
//            phaseReport.append(" but the target is already destroyed!\n");
//            return;
//        }
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the attack is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the club attack is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
            if (glancing) phaseReport.append(" - Glancing Blow - ");
       }

        // do we hit?
        if (roll < toHit.getValue()) {
            phaseReport.append("misses.\n");

            // If the target is in a building, the building absorbs the damage.
            if ( targetInBuilding && bldg != null ) {

                // Only report if damage was done to the building.
                if ( damage > 0 ) {
                    phaseReport.append( "  " )
                        .append( damageBuilding( bldg, damage ) )
                        .append( "\n" );
                }

            }
            return;
        }

        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            phaseReport.append( "hits.\n  " )
                .append( damageBuilding( bldg, damage ) )
                .append( "\n" );

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // And we're done!
            return;
        }

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).append( te.getLocationAbbr(hit));

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        if ( targetInBuilding && bldg != null ) {
            int bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
            int toBldg = Math.min( bldgAbsorbs, damage );
            damage -= toBldg;
            phaseReport.append( "\n  " )
                .append( damageBuilding( bldg, toBldg ) );
        }

        // A building may absorb the entire shot.
        if ( damage == 0 ) {
            phaseReport.append( "\n  " )
                .append( te.getDisplayName() )
                .append( " suffers no damage." );
        } else {
            if (glancing) {
                damage = (int)Math.floor((double)damage/2.0);
            }
            phaseReport.append( damageEntity(te, hit, damage) );
        }

        phaseReport.append("\n");

        if (caa.getClub().getType().hasFlag(MiscType.F_TREE_CLUB)) {
            phaseReport.append("The " ).append( caa.getClub().getName() ).append( " breaks.\n");
            ae.removeMisc(caa.getClub().getName());
        }
    }

    /**
     * Handle a push attack
     */
    private void resolvePushAttack(PhysicalResult pr, int lastEntityId) {
        final PushAttackAction paa = (PushAttackAction)pr.aaa;
        final Entity ae = game.getEntity(paa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "push".
        final Entity te = game.getEntity(paa.getTargetId());
        // get roll and ToHitData from the PhysicalResult
        int roll = pr.roll;
        final ToHitData toHit = pr.toHit;

        // was this push resolved earlier?
        if (pr.pushBackResolved) {
            return;
        }

        if (lastEntityId != paa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }

        phaseReport.append("    Pushing " ).append( te.getDisplayName());

//        // should we even bother?
//        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
//            phaseReport.append(" but the target is already destroyed!\n");
//            return;
//        }

        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the push is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        }
        phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");

        // report the roll
        phaseReport.append("rolls " ).append( roll ).append( " : ");

        // check if our target has a push against us, too, and get it
        PhysicalResult targetPushResult = null;
        for (Enumeration i = physicalResults.elements(); i.hasMoreElements();) {
            PhysicalResult tpr = (PhysicalResult)i.nextElement();
            if (tpr.aaa.getEntityId() == te.getId() &&
                tpr.aaa instanceof PushAttackAction &&
                tpr.aaa.getTargetId() == ae.getId() ) {
                targetPushResult = tpr;
            }
        }
        // if our target has a push against us, we need to resolve both now
        if (targetPushResult != null) {
            // do both hit?
            if (targetPushResult.roll >= targetPushResult.toHit.getValue() &&
                roll >= toHit.getValue()) {
                phaseReport.append("succeeds: but ")
                           .append(te.getDisplayName())
                           .append("  pushed back!.\n")
                           .append("\nPhysical attacks for " )
                           .append( te.getDisplayName() ).append( "\n")
                           .append("    Pushing " ).append( ae.getDisplayName())
                           .append("; needs " ).append( toHit.getValue() )
                           .append( ", ")
                           .append("rolls " ).append( roll ).append( " : ")
                           .append("succeeds: but ")
                           .append(ae.getDisplayName())
                           .append("  pushed back!.\n");
                PilotingRollData targetPushPRD = new PilotingRollData(te.getId(), getKickPushPSRMod(ae, te, 0), "was pushed");
                targetPushPRD.setCumulative(false); // see Bug# 811987 for more info
                PilotingRollData pushPRD = new PilotingRollData(ae.getId(), getKickPushPSRMod(ae, te, 0), "was pushed");
                pushPRD.setCumulative(false); // see Bug# 811987 for more info
                game.addPSR(pushPRD);
                game.addPSR(targetPushPRD);
                targetPushResult.pushBackResolved = true;
                return;
            }
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            phaseReport.append("misses.\n");
            return;
        }

        // we hit...
        int direction = ae.getFacing();

        Coords src = te.getPosition();
        Coords dest = src.translated(direction);

        PilotingRollData pushPRD = new PilotingRollData(te.getId(), getKickPushPSRMod(ae, te, 0), "was pushed");
        pushPRD.setCumulative(false); // see Bug# 811987 for more info

        if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
            phaseReport.append("succeeds: target is pushed ");
            if (game.board.contains(dest)) {
                phaseReport.append("into hex "
                ).append( dest.getBoardNum()
                ).append( "\n");
            } else {
                phaseReport.append("off the board.\n");
            }

            doEntityDisplacement(te, src, dest, pushPRD);

            // if push actually moved the target, attacker follows thru
            if (!te.getPosition().equals(src)) {
                ae.setPosition(src);
            }
        } else {
            phaseReport.append("succeeds, but target can't be moved.\n");
            game.addPSR(pushPRD);
        }

        phaseReport.append("\n");
    }

    /**
     * Handle a charge attack
     */
    private void resolveChargeAttack(PhysicalResult pr, int lastEntityId) {
        final ChargeAttackAction caa = (ChargeAttackAction)pr.aaa;
        final Entity ae = game.getEntity(caa.getEntityId());
        final Targetable target = game.getTarget(caa.getTargetType(), caa.getTargetId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        Entity te = null;
        if (target != null && target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( caa.getTargetPos() );

        // is the attacker dead?  because that sure messes up the calculations
        if (ae == null) {
            return;
        }

        final int direction = ae.getFacing();

        // entity isn't charging any more
        ae.setDisplacementAttack(null);

        if (lastEntityId != caa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }

        // should we even bother?
        if (target == null || (target.getTargetType() == Targetable.TYPE_ENTITY
             && (te.isDestroyed() || te.isDoomed() || te.crew.isDead()))) {
            phaseReport.append("    Charge cancelled as the target has been destroyed.\n");
            // doEntityDisplacement(ae, ae.getPosition(), caa.getTargetPos(), null);
            // Randall said that if a charge fails because of target destruction,
            // the attacker stays in the hex he was in at the end of the movement phase
            // See Bug 912094
            return;
        }

        // attacker fell down?
        if (ae.isProne()) {
            phaseReport.append("    Charge cancelled as the attacker has fallen.\n");
            return;
        }

        // attacker immobile?
        if (ae.isImmobile()) {
            phaseReport.append("    Charge cancelled as the attacker has been immobilized.\n");
            return;
        }

        if (te.isProne()) {
            phaseReport.append("    Charge cancelled as the target has fallen.\n");
            return;
        }

        phaseReport.append("    Charging " ).append( target.getDisplayName());

        // target still in the same position?
        if (!target.getPosition().equals(caa.getTargetPos())) {
            phaseReport.append(" but the target has moved.\n");
            doEntityDisplacement(ae, ae.getPosition(), caa.getTargetPos(), null);
            return;
        }

        // if the attacker's prone, fudge the roll
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            roll = -12;
            phaseReport.append(", but the charge is impossible (" ).append( toHit.getDesc() ).append( ") : ");
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the charge is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
            if (glancing) phaseReport.append(" - Glancing Blow - ");
       }

        // do we hit?
        if (roll < toHit.getValue()) {
            Coords src = ae.getPosition();
            Coords dest = Compute.getMissedChargeDisplacement(game, ae.getId(), src, direction);

            // TODO: handle movement into/out of/through a building.  Do it here?

            phaseReport.append("misses.\n");
            // move attacker to side hex
            doEntityDisplacement(ae, src, dest, null);
        }

        // Targeting a building.
        else if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            phaseReport.append( "hits.\n  " )
                .append( damageBuilding( bldg, damage ) );

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // Apply damage to the attacker.
            int toAttacker = ChargeAttackAction.getDamageTakenBy( ae, bldg );
            HitData hit = ae.rollHitLocation( ToHitData.HIT_NORMAL,
                                              Compute.targetSideTable(target.getPosition(), ae.getPosition(), ae.getFacing(), false)
                                                  );
            phaseReport.append( this.damageEntity( ae, hit, toAttacker ) )
                .append( "\n" );
            entityUpdate( ae.getId() );

            // TODO: Does the attacker enter the building?
            // TODO: What if the building collapses?
        }
        else {
            // Resolve the damage.
            resolveChargeDamage( ae, te, toHit, direction, glancing );
        }
        return;
    }

    /**
     * Handle a charge's damage
     */
    private void resolveChargeDamage(Entity ae, Entity te, ToHitData toHit, int direction) {
        resolveChargeDamage (ae, te, toHit, direction, false);
    }

    private void resolveChargeDamage(Entity ae, Entity te, ToHitData toHit, int direction, boolean glancing) {

        // we hit...
        int damage = ChargeAttackAction.getDamageFor(ae);
        int damageTaken = ChargeAttackAction.getDamageTakenBy(ae, te, game.getOptions().booleanOption("maxtech_charge_damage"));
        PilotingRollData chargePSR = null;
        if (glancing) {
            // Glancing Blow rule doesn't state whether damage to attacker on charge
            // or DFA is halved as well, assume yes. TODO: Check with PM
            damage = (int)Math.floor((double)damage/2.0);
            damageTaken = (int)Math.floor((double)damageTaken/2.0);
        }
        // Is the target inside a building?
        final boolean targetInBuilding = Compute.isInBuilding( game, te );

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( te.getPosition() );

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if ( targetInBuilding && bldg != null ) {
            bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
        }

        // If we're upright, we may fall down.
        if ( !ae.isProne() ) {
            chargePSR = new PilotingRollData(ae.getId(), 2, "charging");
        }

        phaseReport.append("hits.");
        if (glancing) phaseReport.append(" - Glancing Blow - ");
        phaseReport.append("\n    Defender takes " ).append( damage ).append( " damage" ).append( toHit.getTableDesc() ).append( ".");
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            damage -= cluster;
            if ( bldgAbsorbs > 0 ) {
                int toBldg = Math.min( bldgAbsorbs, cluster );
                cluster -= toBldg;
                phaseReport.append( "\n  " )
                    .append( damageBuilding( bldg, toBldg ) );
            }

            // A building may absorb the entire shot.
            if ( cluster == 0 ) {
                phaseReport.append( "\n  " )
                    .append( te.getDisplayName() )
                .append( " suffers no damage." );
            } else {
                HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                phaseReport.append(damageEntity(te, hit, cluster));
            }
        }
        phaseReport.append("\n    Attacker takes " ).append( damageTaken ).append( " damage.");
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = ae.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            phaseReport.append(damageEntity(ae, hit, cluster));
            damageTaken -= cluster;
        }
        // move attacker and target, if possible
        Coords src = te.getPosition();
        Coords dest = src.translated(direction);

        if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
            phaseReport.append("\n");
            doEntityDisplacement(te, src, dest, new PilotingRollData(te.getId(), 2, "was charged"));
            doEntityDisplacement(ae, ae.getPosition(), src, chargePSR);
        }

        phaseReport.append("\n");

    } // End private void resolveChargeDamage( Entity, Entity, ToHitData )

    /**
     * Handle a death from above attack
     */
    private void resolveDfaAttack(PhysicalResult pr, int lastEntityId) {
        final DfaAttackAction daa = (DfaAttackAction)pr.aaa;
        final Entity ae = game.getEntity(daa.getEntityId());
        final Targetable target = game.getTarget(daa.getTargetType(), daa.getTargetId());
        // get damage, ToHitData and roll from the PhysicalResult
        int damage = pr.damage;
        final ToHitData toHit = pr.toHit;
        int roll = pr.roll;
        Entity te = null;
        if (target != null && target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
            // If target Entity underwater, damage is halved, round up
            // using getLocationStatus(1), because only Mechs and Protos
            // can be underwater, and 1 is CT for mechs and torso for Protos
            if (te.getLocationStatus(1) == Entity.LOC_WET) {
                damage = (int)Math.ceil(damage * 0.5f);
            }
        }
        final boolean glancing = (game.getOptions().booleanOption("maxtech_glancing_blows") && (roll == toHit.getValue()));

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( daa.getTargetPos() );

        // is the attacker dead?  because that sure messes up the calculations
        if (ae == null) {
            return;
        }

        final int direction = ae.getFacing();

        if (lastEntityId != daa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }

        // entity isn't charging any more
        ae.setDisplacementAttack(null);

        // should we even bother?
        if (target == null || (target.getTargetType() == Targetable.TYPE_ENTITY
             && (te.isDestroyed() || te.isDoomed() || te.crew.isDead()))) {
            phaseReport.append("    Death from above deals no damage as the target has been destroyed.\n");
            if (ae.isProne()) {
                // attacker prone during weapons phase
                doEntityFall(ae, daa.getTargetPos(), 2, 3, ae.getBasePilotingRoll());

            } else {
                // same effect as successful DFA
                doEntityDisplacement(ae, ae.getPosition(), daa.getTargetPos(), new PilotingRollData(ae.getId(), 4, "executed death from above"));
            }
            return;
        }

        phaseReport.append("    Attempting death from above on " ).append( target.getDisplayName());

        // target still in the same position?
        if ( !target.getPosition().equals(daa.getTargetPos()) ) {
            phaseReport.append(" but the target has moved.\n");
            return;
        }

        // hack: if the attacker's prone, or incapacitated, fudge the roll
        if (ae.isProne() || !ae.isActive()) {
            roll = -12;
            phaseReport.append(" but the attacker is prone or incapacitated : ");
        } else if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            roll = -12;
            phaseReport.append(" but the attack is impossible (" ).append( toHit.getDesc() ).append( ") : ");
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the DFA is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // report the roll
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
            if (glancing) phaseReport.append(" - Glancing Blow - ");
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            Coords dest = te.getPosition();
            Coords targetDest = Compute.getPreferredDisplacement(game, te.getId(), dest, direction);
            phaseReport.append("misses.\n");
            if (targetDest != null) {
                // attacker falls into destination hex
                phaseReport.append(ae.getDisplayName() ).append( " falls into hex " ).append( dest.getBoardNum() ).append( ".\n");
                doEntityFall(ae, dest, 2, 3, ae.getBasePilotingRoll());

                // move target to preferred hex
                doEntityDisplacement(te, dest, targetDest, null);
            } else {
                // attacker destroyed  Tanks
                // suffer an ammo/power plant hit.
                // TODO : a Mech suffers a Head Blown Off crit.
                phaseReport.append(destroyEntity(ae, "impossible displacement", (ae instanceof Mech), (ae instanceof Mech)));
            }
            return;
        }

        // we hit...
        // Can't DFA a target inside of a building.
        int damageTaken = DfaAttackAction.getDamageTakenBy(ae);

        phaseReport.append("hits.");


        // Targeting a building.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

            // The building takes the full brunt of the attack.
            phaseReport.append( "\n  " )
                .append( damageBuilding( bldg, damage ) )
                .append( "\n" );

            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

        }

        // Target isn't building.
        else {
            if (glancing) {
                damage = (int)Math.floor((double)damage/2.0);
            }
            phaseReport.append("\n    Defender takes " ).append( damage ).append( " damage" ).append( toHit.getTableDesc() ).append( ".");
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                phaseReport.append(damageEntity(te, hit, cluster));
                damage -= cluster;
            }
        }

        if (glancing) {
            // Glancing Blow rule doesn't state whether damage to attacker on charge
            // or DFA is halved as well, assume yes. TODO: Check with PM
            damageTaken = (int)Math.floor((double)damageTaken/2.0);
        }
        phaseReport.append("\n    Attacker takes " ).append( damageTaken ).append( " damage.");
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = ae.rollHitLocation(ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
            phaseReport.append(damageEntity(ae, hit, cluster));
            damageTaken -= cluster;
        }
        phaseReport.append("\n");

        // That's it for target buildings.
        // TODO: where do I put the attacker?!?
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return;
        }

        // Target entities are pushed away or destroyed.
        Coords dest = te.getPosition();
        Coords targetDest = Compute.getValidDisplacement(game, te.getId(), dest, direction);
        if (targetDest != null) {
            doEntityDisplacement(te, dest, targetDest, new PilotingRollData(te.getId(), 2, "hit by death from above"));
        } else {
            // ack!  automatic death!  Tanks
            // suffer an ammo/power plant hit.
            // TODO : a Mech suffers a Head Blown Off crit.
            phaseReport.append(destroyEntity(te, "impossible displacement", (te instanceof Mech), (te instanceof Mech)));
        }
        // HACK: to avoid automatic falls, displace from dest to dest
        doEntityDisplacement(ae, dest, dest, new PilotingRollData(ae.getId(), 4, "executed death from above"));
    }

    private int getKickPushPSRMod(Entity attacker, Entity target, int def) {
      int mod = def;

      if ( game.getOptions().booleanOption("maxtech_physical_psr") ) {
        int attackerMod = 0;
        int targetMod = 0;

        switch ( attacker.getWeightClass() ) {
          case Entity.WEIGHT_LIGHT:
            attackerMod = 1;
            break;

          case Entity.WEIGHT_MEDIUM:
            attackerMod = 2;
            break;

          case Entity.WEIGHT_HEAVY:
            attackerMod = 3;
            break;

          case Entity.WEIGHT_ASSAULT:
            attackerMod = 4;
            break;
        }

        switch ( target.getWeightClass() ) {
          case Entity.WEIGHT_LIGHT:
            targetMod = 1;
            break;

          case Entity.WEIGHT_MEDIUM:
            targetMod = 2;
            break;

          case Entity.WEIGHT_HEAVY:
            targetMod = 3;
            break;

          case Entity.WEIGHT_ASSAULT:
            targetMod = 4;
            break;
        }

        mod = attackerMod - targetMod;
      }

      return mod;
    }

    /**
     * Each mech sinks the amount of heat appropriate to its current heat
     * capacity.
     */
    private void resolveHeat() {
        roundReport.append("\nHeat Phase\n----------\n");
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ) {
                continue;
            }
            IHex entityHex = game.getBoard().getHex(entity.getPosition());

            // heat doesn't matter for non-mechs
            if (!(entity instanceof Mech)) {
                entity.heatBuildup = 0;

                // If the unit is hit with an Inferno, do flaming death test.
                if ( entity.infernos.isStillBurning() ) {
                    doFlamingDeath(entity);
                }
                continue;
            }
            else {
                // Meks gain heat from inferno hits.
                if ( entity.infernos.isStillBurning() ) {
                    int infernoHeat = entity.infernos.getHeat();
                    entity.heatBuildup += infernoHeat;
                    roundReport.append( "Added " )
                        .append( infernoHeat )
                        .append( " from a burning inferno round...\n" );
                }
            }

            // should we even bother?
            if ( entity.isDestroyed() || entity.isDoomed() ||
                 entity.crew.isDoomed() || entity.crew.isDead() ) {
                continue;
            }

            // engine hits add a lot of heat, provided the engine is on
            entity.heatBuildup += entity.getEngineCritHeat();

            // If a Mek had an active Stealth suite, add 10 heat.
            if ( entity instanceof Mech && entity.isStealthActive() ) {
                entity.heatBuildup += 10;
                roundReport.append("Added 10 heat from Stealth Armor...\n");
            }

            // If a Mek is in extreme Temperatures, add or subtract one
            // heat per 10 degrees (or fraction of 10 degrees) above or
            // below 50 or -30 degrees Celsius
            if ( entity instanceof Mech && game.getTemperatureDifference() != 0) {
                if (game.getOptions().intOption("temperature") > 50) {
                    entity.heatBuildup += game.getTemperatureDifference();
                    roundReport.append( "Added " )
                        .append( game.getTemperatureDifference() )
                        .append( " heat due to extreme temperatures...\n" );
                }
                else {
                    entity.heatBuildup -= game.getTemperatureDifference();
                    roundReport.append( "Subtracted " )
                        .append( game.getTemperatureDifference() )
                        .append( " heat due to extreme temperatures...\n" );
                }
            }

            // Add +5 Heat if the hex you're in is on fire
            // and was on fire for the full round.
            if (entityHex != null) {
                if (entityHex.terrainLevel(Terrains.FIRE) == 2) {
                    entity.heatBuildup += 5;
                    roundReport.append("Added 5 heat from a fire...\n");
                }
            }
            
            // if heatbuildup is negative due to temperature, set it to 0
            // for prettier turnreports
            if (entity.heatBuildup < 0) {
                entity.heatBuildup = 0;
            }

            // add the heat we've built up so far.
            roundReport.append( entity.getDisplayName() )
                .append( " gains " )
                .append( entity.heatBuildup )
                .append( " heat,");
            entity.heat += entity.heatBuildup;
            entity.heatBuildup = 0;

            // how much heat can we sink?
            int tosink = Math.min( entity.getHeatCapacityWithWater(),
                                   entity.heat );

            entity.heat -= tosink;
            roundReport.append( " sinks " )
                .append( tosink )
                .append( " heat and is now at " )
                .append( entity.heat )
                .append( " heat.\n");

            // Does the unit have inferno ammo?
            if( entity.hasInfernoAmmo() ) {

                // Apply the inferno ammo explosion.
                if (entity.heat >= 10) {
                    int boom = 4 + (entity.heat >= 14 ? 2 : 0) +
                        (entity.heat >= 19 ? 2 : 0) +
                        (entity.heat >= 23 ? 2 : 0) +
                        (entity.heat >= 28 ? 2 : 0);
                    int boomroll = Compute.d6(2);
                    roundReport.append(entity.getDisplayName() )
                        .append( " needs a " )
                        .append( boom )
                        .append( "+ to avoid inferno ammo explosion, rolls " )
                        .append( boomroll )
                        .append( " : " );
                    if (boomroll >= boom) {
                        roundReport.append("avoids successfully!\n");
                    } else {
                        roundReport.append("fails to avoid explosion.\n");
                        roundReport.append(explodeInfernoAmmoFromHeat(entity));
                    }
                }
            } // End avoid-inferno-explosion
            int autoShutDownHeat;
            boolean mtHeat;
            
            if (game.getOptions().booleanOption("maxtech_heat")) {
                autoShutDownHeat = 50;
                mtHeat = true;
            } else {
                autoShutDownHeat = 30;
                mtHeat = false;
            }
            // heat effects: start up
            if (entity.heat < autoShutDownHeat && entity.isShutDown()) {
                if (entity.heat < 14) {
                    entity.setShutDown(false);
                    roundReport.append( entity.getDisplayName() )
                        .append( " automatically starts up.\n" );
                } else {
                    int startup = 4 + (((entity.heat - 14) / 4) * 2);
                    if (mtHeat) {
                        startup = entity.crew.getPiloting() + startup - 8;
                    }
                    int suroll = Compute.d6(2);
                    roundReport.append( entity.getDisplayName() )
                        .append( " needs a " )
                        .append( startup )
                        .append( "+ to start up, rolls " )
                        .append( suroll )
                        .append( " : " );
                    if (suroll >= startup) {
                        entity.setShutDown(false);
                        roundReport.append("successful!\n");
                    } else {
                        roundReport.append("fails.\n");
                    }
                }
            }

            // heat effects: shutdown!
            // 2003-01-26 JAD - Don't shut down if you just restarted.
            else if (entity.heat >= 14 && !entity.isShutDown()) {
                if (entity.heat >= autoShutDownHeat) {
                    roundReport.append( entity.getDisplayName() )
                        .append( " automatically shuts down.\n" );
                    // add a piloting roll and resolve immediately
                    game.addPSR(new PilotingRollData
                        ( entity.getId(), 3, "reactor shutdown" ));
                    resolvePilotingRolls();
                    // okay, now mark shut down
                    entity.setShutDown(true);
                } else if (entity.heat >= 14) {
                    int shutdown = 4 + (((entity.heat - 14) / 4) * 2);
                    if (mtHeat) {
                        shutdown = entity.crew.getPiloting() + shutdown - 8;
                    }
                    int sdroll = Compute.d6(2);
                    roundReport.append(entity.getDisplayName() )
                        .append( " needs a " )
                        .append( shutdown )
                        .append( "+ to avoid shutdown, rolls " )
                        .append( sdroll )
                        .append( " : ");
                    if (sdroll >= shutdown) {
                        roundReport.append("avoids successfully!\n");
                    } else {
                        roundReport.append("shuts down.\n");
                        // add a piloting roll and resolve immediately
                        game.addPSR(new PilotingRollData
                            ( entity.getId(), 3, "reactor shutdown" ));
                        resolvePilotingRolls();
                        // okay, now mark shut down
                        entity.setShutDown(true);
                    }
                }
            }

            // heat effects: ammo explosion!
            if (entity.heat >= 19) {
                int boom = 4 + (entity.heat >= 23 ? 2 : 0) +
                    (entity.heat >= 28 ? 2 : 0);
                if (mtHeat) {
                    boom += 
                        (entity.heat >= 35 ? 2 : 0) +
                        (entity.heat >= 40 ? 2 : 0) +
                        (entity.heat >= 45 ? 2 : 0);
                    // Last line is a crutch; 45 heat should be no roll
                    // but automatic explosion.
                }
                int boomroll = Compute.d6(2);
                roundReport.append( entity.getDisplayName() )
                        .append( " needs a " )
                        .append( boom )
                        .append( "+ to avoid ammo explosion, rolls " )
                        .append( boomroll )
                        .append( " : ");
                if (boomroll >= boom) {
                    roundReport.append("avoids successfully!\n");
                } else {
                    roundReport.append("fails to avoid explosion.\n");
                    roundReport.append(explodeAmmoFromHeat(entity));
                }
            }

            // heat effects: mechwarrior damage
            // N.B. The pilot may already be dead.
            if ( entity.getHitCriticals( CriticalSlot.TYPE_SYSTEM,
                                         Mech.SYSTEM_LIFE_SUPPORT,
                                         Mech.LOC_HEAD ) > 0
                 && entity.heat >= 15
                 && !entity.crew.isDead() && !entity.crew.isDoomed() ) {
                if (entity.heat >= 47) {
                    if (mtHeat) {
                        roundReport.append(entity.getDisplayName() ).append( " has 47 or higher heat and damaged life support.  Mechwarrior takes 5 damage.\n");
                        damageCrew(entity, 5);
                    }
                } else if (entity.heat >= 39) {
                    if (mtHeat) {
                        roundReport.append(entity.getDisplayName() ).append( " has 39 or higher heat and damaged life support.  Mechwarrior takes 4 damage.\n");
                        damageCrew(entity, 4);
                    }
                } else if (entity.heat >= 32) {
                    if (mtHeat) {
                        roundReport.append(entity.getDisplayName() ).append( " has 32 or higher heat and damaged life support.  Mechwarrior takes 3 damage.\n");
                        damageCrew(entity, 3);
                    }
                } else if (entity.heat >= 25) {
                    // mechwarrior takes 2 damage
                    roundReport.append(entity.getDisplayName() ).append( " has 25 or higher heat and damaged life support.  Mechwarrior takes 2 damage.\n");
                    damageCrew(entity, 2);
                } else {
                    // mechwarrior takes 1 damage
                    roundReport.append(entity.getDisplayName() ).append( " has 15 or higher heat and damaged life support.  Mechwarrior takes 1 damage.\n");
                    damageCrew(entity, 1);
                }
            } else if (mtHeat) {
                // Pilot may take damage from heat if MaxTech option is set
                int heatroll;
                if (entity.heat >= 32) {
                    heatroll = Compute.d6(2);
                    roundReport.append (entity.getDisplayName())
                        .append (" needs a 8 to avoid pilot hit, rolls a ")
                        .append (heatroll);
                    if (heatroll<=7) {
                        roundReport.append (", fails and takes a hit.\n");
                        damageCrew (entity, 1);
                    } else {
                        roundReport.append (", succeeds.\n");
                    }
                }
                if (entity.heat >= 39) {
                    heatroll = Compute.d6(2);
                    roundReport.append (entity.getDisplayName())
                        .append (" needs a 10 to avoid pilot hit, rolls a ")
                        .append (heatroll);
                    if (heatroll<=9) {
                        roundReport.append (", fails and takes a hit.\n");
                        damageCrew (entity, 1);
                    } else {
                        roundReport.append (", succeeds.\n");
                    }
                }
                if (entity.heat >= 47) {
                    heatroll = Compute.d6(2);
                    roundReport.append (entity.getDisplayName())
                        .append (" needs a 12 to avoid pilot hit, rolls a ")
                        .append (heatroll);
                    if (heatroll<=11) {
                        roundReport.append (", fails and takes a hit.\n");
                        damageCrew (entity, 1);
                    } else {
                        roundReport.append (", succeeds.\n");
                    }
                }
            }
            // The pilot may have just expired.
            if ( entity.crew.isDead() || entity.crew.isDoomed() ) {
                roundReport.append( "*** " )
                    .append( entity.getDisplayName() )
                    .append( " PILOT BAKES TO DEATH! ***" )
                    .append( destroyEntity(entity, "crew death", true) );
            }
            
            // With MaxTech Heat Scale, there may occur critical damage
            if (mtHeat) {
                int damageroll = Compute.d6(2);
                if (entity.heat >= 44) {
                    roundReport.append (entity.getDisplayName())
                        .append (" needs a 10 to avoid system failure, rolls a ")
                        .append (damageroll);
                    if (damageroll>=10) {
                        roundReport.append (", succeeds.\n");
                    } else {
                        roundReport.append (", fails and takes a critical hit.\n");
                        roundReport.append(oneCriticalEntity (entity, Compute.d6(2)));
                    }
                } else if (entity.heat >= 36) {
                    roundReport.append (entity.getDisplayName())
                    .append (" needs an 8 to avoid system failure, rolls a ")
                    .append (damageroll);
                if (damageroll>=8) {
                    roundReport.append (", succeeds.\n");
                } else {
                    roundReport.append (", fails and takes a critical hit.\n");
                    roundReport.append(oneCriticalEntity (entity, Compute.d6(2)));
                }
                }
            }
        }
    }

    /**
     * check to see if unarmored infantry is outside in extreme temperatures
     * (crude fix because infantry shouldn't be able to be deployed
     * outside of vehicles or buildings, but we can't do that because
     * we don't know wether the map has buildings or not or wether the
     * player has an apc
     */
    private void resolveExtremeTempInfantryDeath() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ) {
                continue;
            }
            IHex entityHex = game.getBoard().getHex(entity.getPosition());
            if (entity instanceof Infantry &&
                    !(entity instanceof BattleArmor) &&
                    game.getTemperatureDifference() > 0 &&
                    !(entityHex.containsTerrain(Terrains.BUILDING))) {
                phaseReport.append(entity.getDisplayName() )
                           .append( " is in extreme temperatures and dies.\n" );
                phaseReport.append(destroyEntity(entity, "heat/cold", false, false));
            }
        }
    }

    private void doFlamingDeath(Entity entity) {
        int boomroll = Compute.d6(2);
        // Infantry are unaffected by fire while they're still swarming.
        if ( Entity.NONE != entity.getSwarmTargetId() ) {
            return;
        }
        // Battle Armor squads equipped with fire protection
        // gear automatically avoid flaming death.
        for ( Enumeration iter = entity.getMisc(); iter.hasMoreElements(); ) {
            Mounted mount = (Mounted) iter.nextElement();
            EquipmentType equip = mount.getType();
            if ( BattleArmor.FIRE_PROTECTION.equals(equip.getInternalName()) ) {
                phaseReport.append(entity.getDisplayName() )
                    .append( " is on fire, but is protected by its gear.\n" );
                return;
            }
        }

        phaseReport.append( entity.getDisplayName() )
            .append( " is on fire.  Needs an 8+ to avoid destruction, rolls " )
            .append( boomroll )
            .append( " : ");
        if (boomroll >= 8) {
            phaseReport.append("avoids successfully!\n");
        } else {
            phaseReport.append("fails to avoid horrible instant flaming death.\n");
            phaseReport.append(destroyEntity(entity, "fire", false, false));
        }
    }

    /**
     * Checks to see if any entity has takes 20 damage.  If so, they need a piloting
     * skill roll.
     */
    private void checkFor20Damage() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (entity instanceof Mech) {
                // if this mech has 20+ damage, add another roll to the list.
                if (entity.damageThisPhase >= 20) {
                    if ( game.getOptions().booleanOption("maxtech_round_damage") ) {
                      int damMod = (entity.damageThisPhase / 20);
                      int weightMod = 0;
                      StringBuffer reportStr = new StringBuffer();
                      reportStr.append(entity.damageThisPhase)
                          .append(" damage +").append(damMod);

                      switch ( entity.getWeightClass() ) {
                        case Entity.WEIGHT_LIGHT:
                          weightMod = 1;
                          break;

                        case Entity.WEIGHT_MEDIUM:
                          weightMod = 0;
                          break;

                        case Entity.WEIGHT_HEAVY:
                          weightMod = -1;
                          break;

                        case Entity.WEIGHT_ASSAULT:
                          weightMod = -2;
                          break;
                      }

                      PilotingRollData damPRD = new PilotingRollData(entity.getId(), damMod + weightMod, reportStr.toString());
                      damPRD.setCumulative(false);  // see Bug# 811987 for more info
                      game.addPSR(damPRD);
                    } else {
                      game.addPSR(new PilotingRollData(entity.getId(), 1, "20+ damage"));
                    }
                }
            }
        }
    }

    /**
     * Checks to see if any non-mech units are standing in fire.  Called at the
     * end of the movement phase
     */
    public void checkForFlamingDeath() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ||
                 entity instanceof Mech ||
                 entity.isDoomed() ||
                 entity.isDestroyed() ||
                 entity.isOffBoard()) {
                continue;
            }
            final IHex curHex = game.board.getHex(entity.getPosition());
            if (curHex.containsTerrain(Terrains.FIRE)) {
                doFlamingDeath(entity);
            }
        }
    }

    /**
     * Checks to see if any entities are underwater with damaged life support.
     * Called during the end phase.
     */
    private void checkForSuffocation() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( null == entity.getPosition() ||
                 entity.isOffBoard()) {
                continue;
            }
            final IHex curHex = game.board.getHex(entity.getPosition());
            if ((curHex.terrainLevel(Terrains.WATER) > 1
            || (curHex.terrainLevel(Terrains.WATER) == 1 && entity.isProne()))
            && entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, Mech.LOC_HEAD) > 0) {
                roundReport.append("\n" ).append( entity.getDisplayName() ).append( " is underwater with damaged life support.  Mechwarrior takes 1 damage.\n");
                roundReport.append( damageCrew(entity, 1) );

            }
        }
    }

    /**
     * Resolves all built up piloting skill rolls.
     * Used at end of weapons, physical phases.
     */
    private void resolvePilotingRolls() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            resolvePilotingRolls((Entity)i.nextElement());
        }
        game.resetPSRs();
    }

    /**
     * Resolves and reports all piloting skill rolls for a single mech.
     */
    void resolvePilotingRolls(Entity entity) {
        resolvePilotingRolls(entity, false, null, null);
    }
    void resolvePilotingRolls( Entity entity, boolean moving,
                               Coords src, Coords dest ) {
        // dead units don't need to.
        if ( entity.isDoomed() || entity.isDestroyed() ) {
            return;
        }
        // first, do extreme gravity PSR, because non-mechs do these, too
        PilotingRollData rollTarget = null;
        for (Enumeration i = game.getExtremeGravityPSRs();i.hasMoreElements();) {
            final PilotingRollData roll = (PilotingRollData)i.nextElement();
            if (roll.getEntityId() != entity.getId()) {
                continue;
            }
            // found a roll, use it (there can be only 1 per entity)
            rollTarget = roll;
            game.resetExtremeGravityPSRs(entity);
        }
        if (rollTarget != null &&
            rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            // okay, print the info
            phaseReport.append("\n" ).append( entity.getDisplayName() )
                .append( " must make a piloting skill check (" )
                .append( rollTarget.getLastPlainDesc() ).append( ")" ).append( ".\n");
            // roll
            final int diceRoll = Compute.d6(2);
            phaseReport.append("Needs " ).append( rollTarget.getValueAsString()
            ).append( " [" ).append( rollTarget.getDesc() ).append( "]"
            ).append( ", rolls " ).append( diceRoll ).append( " : ");
            if (diceRoll < rollTarget.getValue()) {
                phaseReport.append("fails.\n");
                // walking and running, 1 damage per MP used more than we would
                // have normally
                if (entity.moved == Entity.MOVE_WALK || entity.moved == Entity.MOVE_RUN) {
                    if (entity instanceof Mech) {
                        int j = entity.mpUsed;
                        int damage = 0;
                        while (j > entity.getRunMP(false)) {
                            j--;
                            damage++;
                        }
                        // Wee, direct internal damage
                        doExtremeGravityDamage(entity, damage);
                    } else if (entity instanceof Tank) {
                        // if we got a pavement bonus, take care of it
                        int k = entity.gotPavementBonus ? 1 : 0;  
                        if (!entity.gotPavementBonus) {
                            int j = entity.mpUsed;
                            int damage = 0;
                            while (j > entity.getRunMP(false) + k) {
                                j--;
                                damage++;
                            }
                            doExtremeGravityDamage(entity, damage);
                        }                      
                    }
                }
                // jumping
                if (entity.moved == Entity.MOVE_JUMP && entity instanceof Mech) {
                    // low g, 1 damage for each hex jumped further than
                    // possible normally
                    if (game.getOptions().floatOption("gravity") < 1) {
                        int j = entity.mpUsed;
                        int damage = 0;
                        while (j > entity.getOriginalJumpMP()) {
                            j--;
                            damage++;
                        }
                        // Wee, direct internal damage
                        doExtremeGravityDamage(entity, damage);
                    }
                    // high g, 1 damage for each MP we have less than normally
                    else if (game.getOptions().floatOption("gravity") > 1) {
                            int damage = entity.getWalkMP(false) - entity.getWalkMP();
                            // Wee, direct internal damage
                            doExtremeGravityDamage(entity, damage);                    
                    }
                }                
            } else {
                phaseReport.append("succeeds.\n");
            }
        }
        // non mechs and prone mechs can now return
        if ( !(entity instanceof Mech) || entity.isProne()) {
            return;
        }
        // add all cumulative rolls, count all rolls
        Vector rolls = new Vector();
        StringBuffer reasons = new StringBuffer();
        PilotingRollData base = entity.getBasePilotingRoll();
        for (Enumeration i = game.getPSRs(); i.hasMoreElements();) {
            final PilotingRollData modifier = (PilotingRollData)i.nextElement();
            if (modifier.getEntityId() != entity.getId()) {
                continue;
            }
            // found a roll, add it
            rolls.addElement(modifier);
            if (reasons.length() > 0) {
                reasons.append(", ");
            }
            reasons.append(modifier.getPlainDesc());
            // only cumulative rolls get added to the base roll
            if (modifier.isCumulative()) {
                base.append(modifier);
            }
        }
        // any rolls needed?
        if (rolls.size() == 0) {
            return;
        }
        // is our base roll impossible?
        if (base.getValue() == PilotingRollData.AUTOMATIC_FAIL || base.getValue() == PilotingRollData.IMPOSSIBLE) {
            phaseReport.append("\n").append(entity.getDisplayName()).append(" must make ").append(rolls.size()).append(" piloting skill roll(s) (" ).append( reasons.toString() ).append(") and automatically fails (").append(base.getDesc()).append(").\n");
            if (moving) {
                doEntityFallsInto( entity, src, dest, base );
            } else {
                doEntityFall(entity, base);
            }
            return;
        }
        // loop thru rolls we do have to make...
        phaseReport.append("\n" ).append( entity.getDisplayName() ).append( " must make " ).append( rolls.size() ).append( " piloting skill roll(s) (" ).append( reasons.toString() ).append( ").\n");
        phaseReport.append("The base target is " ).append( base.getValueAsString() ).append( " [" ).append( base.getDesc() ).append( "].\n");
        for (int i = 0; i < rolls.size(); i++) {
            PilotingRollData modifier = (PilotingRollData)rolls.elementAt(i);
            PilotingRollData target = base;
            phaseReport.append("    Roll #").append(i + 1).append(", (");
            phaseReport.append(modifier.getPlainDesc());
            if (!modifier.isCumulative()) {
                // non-cumulative rolls only happen due to weight class adj.
                phaseReport.append(", after weight adjustment the modifier is ").append(modifier.getValueAsString());
                target = new PilotingRollData(entity.getId());
                target.append(base);
                target.append(modifier);
            }
            int diceRoll = Compute.d6(2);
            phaseReport.append("); needs ").append(target.getValueAsString());
            phaseReport.append(", rolls ").append(diceRoll).append(" : ");
            if (diceRoll < target.getValue()) {
                phaseReport.append(" falls.\n");
                if (moving) {
                    doEntityFallsInto( entity, src, dest, base );
                } else {
                    doEntityFall(entity, base);
                }
                return;
            } else {
                phaseReport.append(" succeeds.\n");
            }
        }
    }

    /**
     * Inflict damage on a pilot
     *
     * @param en     The <code>Entity</code> who's pilot gets damaged.
     * @param damage The <code>int</code> amount of damage.
     */
    private String damageCrew(Entity en, int damage) {
        StringBuffer desc = new StringBuffer();
        Pilot crew = en.getCrew();

        if (!crew.isDead() && !crew.isEjected() && !crew.isDoomed()) {
            crew.setHits( crew.getHits() + damage );
            desc.append( "        Pilot of " )
                .append( en.getDisplayName() )
                .append( " \"" )
                .append( crew.getName() )
                .append( "\" takes " )
                .append( damage )
                .append( " damage." );
            if ( Pilot.DEATH > crew.getHits() ) {
                crew.setRollsNeeded( crew.getRollsNeeded() + damage );
            } else if ( !crew.isDoomed() ) {
                crew.setDoomed(true);
                desc.append( "\n" )
                    .append( destroyEntity(en, "pilot death", true) );
            }
        }

        return desc.toString();
    }

    /**
     * This checks if the mech pilot goes unconscious from the damage he has
     * taken this phase.
     */
    private void resolveCrewDamage() {
        boolean anyRolls = false;
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();
            if (resolveCrewDamage(e, anyRolls)) {
                anyRolls = true;
            }
        }
        if (anyRolls) {
            phaseReport.append("\n");
        }
    }

    /**
     * resolves consciousness rolls for one entity
     */

    private boolean resolveCrewDamage(Entity e, boolean anyRolls) {
        final int totalHits = e.getCrew().getHits();
        final int rollsNeeded = e.getCrew().getRollsNeeded();
        e.crew.setRollsNeeded(0);
        if (!e.isTargetable() || !e.getCrew().isActive() || rollsNeeded == 0) {
            return false;
        }
        for (int hit = totalHits - rollsNeeded + 1; hit <= totalHits; hit++) {
            int roll = Compute.d6(2);

            if ( e.getCrew().getOptions().booleanOption("pain_resistance") )
              roll = Math.min(12, roll + 1);

            int rollTarget = Compute.getConsciousnessNumber( hit );
            phaseReport.append("\nPilot of " ).append( e.getDisplayName()
                               ).append( " \"" ).append( e.getCrew().getName()
                               ).append( "\" needs a " ).append( rollTarget
                               ).append( " to stay conscious.  Rolls " ).append( roll
                               ).append( " : ");
            if (roll >= rollTarget) {
                phaseReport.append("successful!");
            } else {
                e.crew.setUnconscious(true);
                e.crew.setKoThisRound(true);
                phaseReport.append("blacks out.");
                return true;
            }
        }
        return true;
    }

    /**
     * Make the rolls indicating whether any unconscious crews wake up
     */
    private void resolveCrewWakeUp() {
        boolean anyRolls = false;
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();

            // only unconscious pilots of mechs and protos and MechWarrirs
            // can roll to wake up
            if ( !e.isTargetable() || !e.crew.isUnconscious() ||
                 e.crew.isKoThisRound() ||
                !(e instanceof Mech || e instanceof Protomech || e instanceof MechWarrior)) {
                continue;
            }
            anyRolls = true;
            int roll = Compute.d6(2);

            if ( e.getCrew().getOptions().booleanOption("pain_resistance") )
              roll = Math.min(12, roll + 1);

            int rollTarget = Compute.getConsciousnessNumber( e.crew.getHits() );
            roundReport.append("\nPilot of " ).append( e.getDisplayName()
                               ).append( " \"" ).append( e.crew.getName()
                               ).append( "\" needs a " ).append( rollTarget
                               ).append( " to regain consciousness.  Rolls " ).append( roll
                               ).append( " : ");
            if (roll >= rollTarget) {
                roundReport.append("successful!");
                e.crew.setUnconscious(false);
            } else {
                roundReport.append("fails.");
            }
        }
        if (anyRolls) {
            roundReport.append("\n");
        }
    }

    public String damageEntity(Entity te, HitData hit, int damage,
                               boolean ammoExplosion) {
        return damageEntity(te, hit, damage, ammoExplosion, 0,
                            false, false);
    }

    public String damageEntity(Entity te, HitData hit, int damage) {
        return damageEntity(te, hit, damage, false, 0,
                            false, false);
    }

    public String damageEntity(Entity te, HitData hit, int damage,
                               boolean ammoExplosion, int bFrag) {
        return damageEntity(te, hit, damage, ammoExplosion, bFrag,
                            false, false);
    }

    public String damageEntity(Entity te, HitData hit, int damage,
                               boolean ammoExplosion, int bFrag,
                               boolean damageIS) {
        return damageEntity(te, hit, damage, ammoExplosion, bFrag,
                            damageIS, false);
    }

    /**
     * Deals the listed damage to an entity.  Returns a description
     * string for the log.
     *
     * @param te the target entity
     * @param hit the hit data for the location hit
     * @param damage the damage to apply
     * @param ammoExplosion ammo explosion type damage is applied
     *          directly to the IS, hurts the pilot, causes auto-ejects,
     *          and can blow the unit to smithereens
     * @param bFrag If 0, nothing; if 1, Fragmentation; if 2, Flechette.
     * @param damageIS Should the target location's internal structure be
     *          damaged directly?
     * @param areaSatArty Is the damage from an area saturating artillery
     *          attack?
     */
    private String damageEntity(Entity te, HitData hit, int damage,
                                boolean ammoExplosion, int bFrag,
                                boolean damageIS, boolean areaSatArty) {
        StringBuffer desc = new StringBuffer();
        boolean autoEject = false;
        if (ammoExplosion) {
            if (te instanceof Mech) {
                Mech mech = (Mech)te;
                if (mech.isAutoEject()) {
                    autoEject = true;
                    desc.append(ejectEntity(te, true));
                }
            }
        }
        boolean isBattleArmor = (te instanceof BattleArmor);
        boolean isPlatoon = !isBattleArmor && (te instanceof Infantry);
        boolean wasDamageIS = false;
        IHex te_hex = null;

        int crits = hit.getEffect() == HitData.EFFECT_CRITICAL ? 1 : 0;
        int specCrits = 0;
        HitData nextHit = null;

        // Some "hits" on a Protomech are actually misses.
        if( te instanceof Protomech &&
            hit.getLocation() == Protomech.LOC_NMISS ) {
            desc.append
                ( "\n        The Protomech takes no damage from a near miss." );
            return desc.toString();
        }

        // Is the infantry in the open?
        if ( isPlatoon && !te.isDestroyed() && !te.isDoomed() ) {
            te_hex = game.board.getHex( te.getPosition() );
            if ( te_hex != null &&
                 !te_hex.containsTerrain( Terrains.WOODS ) &&
                 !te_hex.containsTerrain( Terrains.ROUGH ) &&
                 !te_hex.containsTerrain( Terrains.RUBBLE ) &&
                 !te_hex.containsTerrain( Terrains.SWAMP ) &&
                 !te_hex.containsTerrain( Terrains.BUILDING ) ) {
                // PBI.  Damage is doubled.
                damage = damage * 2;
                desc.append( "\n        Infantry platoon caught in the open!!!  Damage doubled." );
            }
        }
        // Is the infantry in vacuum?
        if ( (isPlatoon || isBattleArmor ) && !te.isDestroyed()
                && !te.isDoomed() && game.getOptions().booleanOption("vacuum")) {
            // PBI. Double damage.
            damage = damage * 2;
            desc.append( "\n        Infantry is in Vacuum!!! Space suits are breached!!! Damage doubled." );
        }
        // If dealing with fragmentation missiles,
        // it does double damage to infantry...
        switch (bFrag)
        {
        case 1:
            if (isPlatoon) {
                damage *= 2;
                desc.append( "\n        Infantry platoon hit by fragmentation missiles!!!  Damage doubled." );
            }
            else if (te != null) {
                damage = 0;
                desc.append( "\n        Hardened unit hit by fragmentation missiles!!!  No damage." );
            }
            break;
        case 2:
            if (isPlatoon) {
                damage *= 2;
                desc.append( "\n        Infantry platoon hit by flechette ammunition!!!  Damage doubled." );
            }
            else if ((te != null) && (!isBattleArmor)) {
                damage /= 2;
                desc.append( "\n        Hardened unit hit by flechette ammunition!!!  Damage halved." );
            }
            break;
        default:
            // We can ignore this.
            break;
        }

        // Allocate the damage
        while (damage > 0) {

            // let's resolve some damage!
            desc.append( "\n        " )
                .append( te.getDisplayName() )
                .append( " takes " )
                .append( damage )
                .append( " damage to " );

            if (damageIS) desc.append ( "Internal Structure of ");

            desc.append(te.getLocationAbbr(hit) )
                .append( "." );

            // was the section destroyed earlier this phase?
            if (te.getInternal(hit) == Entity.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                crits = 0;
            }

            // Does an exterior passenger absorb some of the damage?
            if (!damageIS) {
                int nLoc = hit.getLocation();
                Entity passenger = te.getExteriorUnitAt( nLoc, hit.isRear() );
                if ( !ammoExplosion &&
                     null != passenger && !passenger.isDoomed() ) {

                    // Yup.  Roll up some hit data for that passenger.
                    desc.append( "\n            The passenger, " )
                        .append( passenger.getDisplayName() )
                        .append( ", gets in the way." );
                    HitData passHit = passenger.rollHitLocation
                        ( ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT );

                    // How much damage will the passenger absorb?
                    int absorb = 0;
                    HitData nextPassHit = passHit;
                    do {
                        if ( 0 < passenger.getArmor( nextPassHit ) ) {
                            absorb += passenger.getArmor( nextPassHit );
                        }
                        if ( 0 < passenger.getInternal( nextPassHit ) ) {
                            absorb += passenger.getInternal( nextPassHit );
                        }
                        nextPassHit = passenger.getTransferLocation( nextPassHit );
                    } while ( damage > absorb && nextPassHit.getLocation() >= 0 );

                    // Damage the passenger.
                    desc.append( damageEntity( passenger, passHit, damage ) );

                    // Did some damage pass on?
                    if ( damage > absorb ) {
                        // Yup.  Remove the absorbed damage.
                        damage -= absorb;
                        desc.append( "\n    " )
                            .append( damage )
                            .append( " damage point(s) passes on to " )
                            .append( te.getDisplayName() )
                            .append( "." );
                    } else {
                        // Nope.  Return our description.
                        return desc.toString();
                    }

                } // End nLoc-has-exterior-passenger

                // is this a mech dumping ammo being hit in the rear torso?
                boolean bTorso = (nLoc == Mech.LOC_CT || nLoc == Mech.LOC_RT ||
                                  nLoc == Mech.LOC_LT);
                if (te instanceof Mech && hit.isRear() && bTorso) {
                    for (Enumeration e = te.getAmmo(); e.hasMoreElements(); ) {
                        Mounted mAmmo = (Mounted)e.nextElement();
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed() &&
                            !mAmmo.isHit()) {
                            // doh.  explode it
                            desc.append( explodeEquipment(te, mAmmo.getLocation(),
                                                          mAmmo) );
                            mAmmo.setHit(true);
                        }
                    }
                }
            }

            // is there armor in the location hit?
            if (!ammoExplosion && te.getArmor(hit) > 0 && !damageIS) {
                if (te.getArmor(hit) > damage) {
                    // armor absorbs all damage
                    te.setArmor(te.getArmor(hit) - damage, hit);
                    te.damageThisPhase += damage;
                    damage = 0;
                    desc.append( " " )
                        .append( te.getArmor(hit) )
                        .append( " Armor remaining" )
                        .append( breachCheck(te, hit.getLocation(), null) );
                } else {
                    // damage goes on to internal
                    int absorbed = Math.max(te.getArmor(hit), 0);
                    te.setArmor(Entity.ARMOR_DESTROYED, hit);
                    te.damageThisPhase += absorbed;
                    damage -= absorbed;
                    desc.append( " Armor destroyed," )
                        .append( breachCheck(te, hit.getLocation(), null) );
                }
            }

            // is there damage remaining?
            if (damage > 0) {
                // is there internal structure in the location hit?
                if (te.getInternal(hit) > 0) {
                    // Triggers a critical hit on Vehicles and Mechs.
                    if ( !isPlatoon && !isBattleArmor ) {
                        crits++;
                    }
                    if (te.getInternal(hit) > damage) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(hit) - damage, hit);
                        te.damageThisPhase += damage;
                        damage = 0;
                        // Infantry platoons have men not "Internals".
                        if ( isPlatoon ) {
                            desc.append( " " )
                                .append( te.getInternal(hit))
                                .append(" men alive" );
                        } else {
                            desc.append( " " )
                                .append( te.getInternal(hit))
                                .append(" Internal Structure remaining" );
                        }
                    } else {
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);

                        // Handle Protomech pilot damage
                        // due to location destruction
                        if ( te instanceof Protomech ) {
                            int hits = Protomech.POSSIBLE_PILOT_DAMAGE[hit.getLocation()] -
                                ((Protomech)te).getPilotDamageTaken(hit.getLocation());
                            if ( hits > 0 ) {
                                desc.append( "\n" )
                                     .append( damageCrew( te, hits ) );
                                ((Protomech)te).setPilotDamageTaken
                                     (hit.getLocation(),
                                      Protomech.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]);
                            }
                        }

                        // Infantry have only one section.
                        if ( isPlatoon ) {
                            desc.append( " <<<PLATOON KILLED>>>," );
                        } else if ( isBattleArmor ) {
                            desc.append( " <<<TROOPER KILLED>>>," );
                        } else {
                            desc.append( " <<<SECTION DESTROYED>>>," );
                        }

                        // If a sidetorso got destroyed, and the corresponding arm
                        // is not yet destroyed, add it as a club to that hex (p.35 BMRr)

                        if (te instanceof Mech &&
                                (hit.getLocation() == Mech.LOC_RT ||
                                 hit.getLocation() == Mech.LOC_LT)) {
                                if (hit.getLocation() == Mech.LOC_RT &&
                                    te.getInternal(Mech.LOC_RARM) > 0) {
                                    desc.append( " <<<LIMB BLOWN OFF>>>\n        " )
                                    .append( te.getLocationName(Mech.LOC_RARM) )
                                    .append( " blown off." );
                                    IHex h = game.board.getHex(te.getPosition());
                                    if (te instanceof BipedMech) {
                                        if (!h.containsTerrain( Terrains.ARMS)) {
                                            h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, 1));
                                        }
                                        else h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, h.terrainLevel(Terrains.ARMS)+1));
                                    } else if (!h.containsTerrain( Terrains.LEGS)) {
                                               h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, 1));
                                           } else h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, h.terrainLevel(Terrains.LEGS)+1));
                                    sendChangedHex(te.getPosition());
                                }
                                if (hit.getLocation() == Mech.LOC_LT &&
                                    te.getInternal(Mech.LOC_LARM) > 0) {
                                    desc.append( " <<<LIMB BLOWN OFF>>>\n        " )
                                    .append( te.getLocationName(Mech.LOC_LARM) )
                                    .append( " blown off." );
                                    IHex h = game.board.getHex(te.getPosition());
                                    if (te instanceof BipedMech) {
                                        if (!h.containsTerrain( Terrains.ARMS)) {
                                            h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, 1));
                                        }
                                        else h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, h.terrainLevel(Terrains.ARMS)+1));
                                    } else if (!h.containsTerrain( Terrains.LEGS)) {
                                               h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, 1));
                                           } else h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, h.terrainLevel(Terrains.LEGS)+1));
                                    sendChangedHex(te.getPosition());
                                }
                        }

                        // Destroy the location.
                        destroyLocation(te, hit.getLocation());
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;

                        if (te instanceof Mech &&
                            (hit.getLocation() == Mech.LOC_RT ||
                             hit.getLocation() == Mech.LOC_LT)) {

                            boolean engineExploded = false;

                            if ( te.engineHitsThisRound >= 2 ) {
                              engineExploded = checkEngineExplosion(te, desc);
                            }

                            if ( !engineExploded ) {
                              int numEngineHits = 0;
                              numEngineHits +=
                                  te.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                                     Mech.SYSTEM_ENGINE,
                                                     Mech.LOC_CT);
                              numEngineHits +=
                                  te.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                                     Mech.SYSTEM_ENGINE,
                                                     Mech.LOC_RT);
                              numEngineHits +=
                                  te.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                                     Mech.SYSTEM_ENGINE,
                                                      Mech.LOC_LT);

                              if ( numEngineHits > 2  ) {
                                  // third engine hit
                                  desc.append(destroyEntity(te, "engine destruction"));
                              }
                           }
                        }
                    }
                } if (te.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer potentials?
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        if (te instanceof Mech) {
                            // add all non-destroyed engine crits
                            te.engineHitsThisRound += te.getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, hit.getLocation());
                            // and substract those that where hit previously this round
                            // hackish, but works.
                            te.engineHitsThisRound -= te.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, hit.getLocation());
                        }

                        boolean engineExploded = false;

                        if ( te.engineHitsThisRound >= 2 ) {
                          engineExploded = checkEngineExplosion(te, desc);
                        }

                        if ( !engineExploded ) {
                          // Entity destroyed.  Ammo explosions are
                          // neither survivable nor salvagable.
                          // Only ammo explosions in the CT are devastating.
                          desc.append( destroyEntity( te, "damage",
                                                      !ammoExplosion,
                                                      !( (ammoExplosion || areaSatArty) &&
                                                         hit.getLocation() ==
                                                         Mech.LOC_CT ) ) );
                          // If the head is destroyed, kill the crew.
                          if (hit.getLocation() == Mech.LOC_HEAD ||
                              (hit.getLocation() == Mech.LOC_CT && ((ammoExplosion && !autoEject) || areaSatArty))) {
                            te.getCrew().setDoomed(true);
                          }
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if ( nextHit.getLocation() == Entity.LOC_NONE ) {
                        // Rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && te.locationHasCase(hit.getLocation())) {
                        // Remaining damage prevented...
                        desc.append( " remaining " )
                            .append( damage )
                            .append( " damage prevented by CASE." );

                        // ... but page 21 of the Ask The Precentor Martial FAQ
                        // www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf
                        // says that the damage counts for making PSRs.
                        te.damageThisPhase += damage;

                        // The target takes no more damage from the explosion.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        desc.append( "\n        " )
                            .append( damage )
                            .append( " damage transfers to " )
                            .append( te.getLocationAbbr(nextHit) )
                            .append( "." );
                    }
                }
            }
            else if (hit.getSpecCritMod() < 0)
            { // If there ISN'T any armor left but we did damage, then there's a chance of a crit, using Armor Piercing.
                specCrits++;
            }

            // resolve special results
            if (hit.getEffect() == HitData.EFFECT_VEHICLE_MOVE_DAMAGED) {
                desc.append( "\n            Movement system damaged!" );

                int nMP = te.getOriginalWalkMP();
                if (nMP > 0) {
                    te.setOriginalWalkMP(nMP - 1);

                    if (te.getOriginalWalkMP()==0) {
                        // From http://www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf
                        // page 19, tanks are only immobile if they take that critical hit.
                        // ((Tank)te).immobilize();

                        // Hovercraft reduced to 0MP over water sink
                        if ( te.getMovementType() == Entity.MovementType.HOVER &&
                                game.board.getHex( te.getPosition() ).terrainLevel(Terrains.WATER) > 0 ) {
                            desc.append( destroyEntity(te, "a watery grave", false) );
                        }
                    };
                };
            }
            else if (hit.getEffect() == HitData.EFFECT_VEHICLE_MOVE_DESTROYED) {
                desc.append( "\n            Movement system destroyed!" );
                ((Tank)te).immobilize();
                // Does the hovercraft sink?
                te_hex = game.board.getHex( te.getPosition() );
                if ( te.getMovementType() == Entity.MovementType.HOVER &&
                     te_hex.terrainLevel(Terrains.WATER) > 0 ) {
                    desc.append( destroyEntity(te, "a watery grave", false) );
                }
            }
            else if (hit.getEffect() == HitData.EFFECT_VEHICLE_TURRETLOCK) {
                desc.append( "\n            Turret locked!" );
                ((Tank)te).lockTurret();
            }

            // roll all critical hits against this location
            for (int i = 0; i < crits; i++) {
                desc.append( "\n" )
                    .append( criticalEntity(te, hit.getLocation(), hit.glancingMod()) );
            }
            crits = 0;

            for (int i = 0; i < specCrits; i++) {
                desc.append( "\n" )
                    .append( criticalEntity(te, hit.getLocation(),
                                            hit.getSpecCritMod()+hit.glancingMod()) );
            }
            specCrits = 0;

            if (te instanceof Mech && hit.getLocation() == Mech.LOC_HEAD) {
                desc.append( "\n" ).append( damageCrew(te, 1) );
            }

            // loop to next location
            hit = nextHit;
            if (damageIS) {
                wasDamageIS = true;
                damageIS = false;
            }
        }
        if (wasDamageIS) desc.append( "\n" );
        return desc.toString();
    }

    /**
     * Check to see if the entity's engine explodes
     */

    private boolean checkEngineExplosion(Entity en, StringBuffer sbDesc) {
      if ( !game.getOptions().booleanOption("engine_explosions") || en.rolledForEngineExplosion )
        return false;

      int explosionBTH = 12;
      int explosionRoll = Compute.d6(2);

      boolean didExplode = explosionRoll >= explosionBTH;

      sbDesc.append("\n        " + en.getDisplayName() + " has taken " + en.engineHitsThisRound + " engine hits this round.\n");
      sbDesc.append("        Checking for engine explosion on BTH = " + explosionBTH + ", Roll = " + explosionRoll + "\n");
      en.rolledForEngineExplosion = true;

      if ( !didExplode ) {
        sbDesc.append("        Engine safety systems remain in place.\n");
      } else {
        sbDesc.append("        ***The safety systems on the engine fail catastrophically resulting in a cascading engine failure!\n");
        sbDesc.append( destroyEntity(en, "engine explosion", false, false) );
        //kill the crew
        en.getCrew().setDoomed(true);

        //This is a hack so MM.NET marks the mech as not salvageable
          if ( en instanceof Mech )
            destroyLocation(en, Mech.LOC_CT);

        //Light our hex on fire
          final IHex curHex = game.board.getHex(en.getPosition());

          if ( (null != curHex) && !curHex.containsTerrain(Terrains.FIRE) && curHex.containsTerrain(Terrains.WOODS) ) {
            curHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            sbDesc.append("        The hex at " + en.getPosition().x + "," + en.getPosition().y + " ignites!\n");
            sendChangedHex(en.getPosition());
          }

        //Nuke anyone that is in our hex
          Enumeration entitesWithMe = game.getEntities(en.getPosition());
          Hashtable entitesHit = new Hashtable();

          entitesHit.put(en, en);

          while ( entitesWithMe.hasMoreElements() ) {
            Entity entity = (Entity)entitesWithMe.nextElement();

            if ( entity.equals(en) )
              continue;

            sbDesc.append(destroyEntity(entity, "engine explosion proximity", false, false));
            // Kill the crew
            entity.getCrew().setDoomed(true);

            entitesHit.put(entity, entity);
          }

        //Now we damage people near us
          int engineRating = ((Mech)en).engineRating();
          int[] damages = { 999, (engineRating / 10), (engineRating / 20), (engineRating / 40) };

          Vector entites = game.getEntitiesVector();

          for ( int i = 0; i < entites.size(); i++ ) {
            Entity entity = (Entity)entites.elementAt(i);

            if ( entitesHit.containsKey(entity) )
              continue;

            if ( entity.isDoomed() || entity.isDestroyed() || !entity.isDeployed() )
              continue;

            int range = en.getPosition().distance(entity.getPosition());

            if ( range > 3 )
              continue;

            int damage = damages[range];

            sbDesc.append("        \n" + entity.getDisplayName() + " is hit for " + damage + " damage!");

            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, Compute.targetSideTable(en, entity));
                sbDesc.append(damageEntity(entity, hit, cluster));
                damage -= cluster;
            }

            sbDesc.append("\n");
          }
      }

      return didExplode;
    }

    /**
     * Apply a single critical hit.
     *
     * The following private member of Server are accessed from this function,
     * preventing it from being factored out of the Server class:
     * destroyEntity()
     * destroyLocation()
     * checkEngineExplosion()
     * damageCrew()
     * explodeEquipment()
     * game
     *
     * @param   en the <code>Entity</code> that is being damaged.
     *          This value may not be <code>null</code>.
     * @param   loc the <code>int</code> location of critical hit.
     *          This value may be <code>Entity.NONE</code> for hits
     *          to <code>Tank</code>s and for hits to a <code>Protomech</code>
     *          torso weapon.
     * @param   cs the <code>CriticalSlot</code> being damaged.
     *          This value may not be <code>null</code>.
     *          For critical hits on a <code>Tank</code>, the index of
     *          the slot should be the index of the critical hit table.
     * @param   secondaryEffects the <code>boolean</code> flag that indicates
     *          whether to allow critical hits to cause secondary effects (such
     *          as triggering an ammo explosion, sending hovercraft to watery
     *          graves, or damaging Protomech torso weapons). This value is
     *          normally <code>true</code>, but it will be <code>false</code>
     *          when the hit is being applied from a saved game or scenario.
     */

    public String applyCriticalHit( Entity en, int loc, CriticalSlot cs,
                                    boolean secondaryEffects ) {
        StringBuffer desc = new StringBuffer();

        // Handle hits on "critical slots" of tanks.
        if ( en instanceof Tank ) {
            Tank tank = (Tank)en;
            switch ( cs.getIndex() ) {
                case 1 :
                    desc.append( "\n            <<<CRITICAL HIT>>> " )
                        .append( "Crew stunned for 3 turns" );
                    // Carried units can't unload from a stunned transport.
                    // Units that escape a transport don't need to un-stun.
                    tank.stunCrew();
                    break;
                case 2 :
                    // this one's ridiculous.  the 'main weapon' jams.
                    Mounted mWeap = tank.getMainWeapon();
                    if (mWeap == null) {
                        desc.append( "\n            No main weapon crit, " )
                            .append( "because no main weapon!" );
                    }
                    else {
                        desc.append( "\n            <<<CRITICAL HIT>>> " )
                            .append( mWeap.getName() );
                        int jamTurns = tank.getJammedTurns() + 1;
                        if ( jamTurns > 1 ) {
                            desc.append( " jams for 1 more turn (" )
                                .append( jamTurns )
                                 .append( " turns total)." );
                        } else {
                            desc.append( " jams for 1 turn." );
                        }
                        tank.setJammedTurns( jamTurns );
                    }
                    break;
                case 3 :
                    desc.append( "\n            <<<CRITICAL HIT>>> " )
                        .append( "Engine destroyed.  Immobile." );
                    tank.immobilize();
                    // Does the hovercraft sink?
                    // Sinking immobile hovercraft is a secondary effect
                    // and does not occur when loading from a scenario.
                    if ( secondaryEffects ) {
                        IHex te_hex = game.board.getHex( en.getPosition() );
                        if ( en.getMovementType() == Entity.MovementType.HOVER
                             && te_hex.terrainLevel(Terrains.WATER) > 0 ) {
                            desc.append( destroyEntity
                                         (en, "a watery grave", false) );
                        }
                    }
                    break;
                case 4 :
                    desc.append( "\n            <<<CRITICAL HIT>>> " )
                        .append( "Crew killed" );
                    desc.append( destroyEntity(en, "crew death", true) );
                    en.getCrew().setDoomed(true);
                    break;
                case 5 :
                    desc.append( "\n            <<<CRITICAL HIT>>> " )
                        .append( "Fuel Tank / Engine Shielding Hit " )
                        .append( "(Vehicle Explodes)" );
                    desc.append( destroyEntity
                                 (en, "fuel tank explosion", false, false) );
                    en.getCrew().setDoomed(true);
                    break;
                case 6 :
                    desc.append( "\n            <<<CRITICAL HIT>>> " )
                        .append( "Power plant hit.  BOOM!" );
                    desc.append( destroyEntity
                                 (en, "power plant destruction",
                                  false, false) );
                    en.getCrew().setDoomed(true);
                    break;
                }

        } // End entity-is-tank

        // Handle critical hits on system slots.
        else if ( CriticalSlot.TYPE_SYSTEM == cs.getType() ) {
            cs.setHit(true);
            if (en instanceof Protomech) {
                int numHit=((Protomech)en).getCritsHit(loc);
                if ( cs.getIndex() != Protomech.SYSTEM_TORSO_WEAPON_A &&
                     cs.getIndex() != Protomech.SYSTEM_TORSO_WEAPON_B ) {
                    desc.append( "\n            <<<CRITICAL HIT>>> on " )
                        .append( Protomech.systemNames[cs.getIndex()] )
                        .append( "." );
                }
                switch (cs.getIndex()) {
                case Protomech.SYSTEM_HEADCRIT:
                    if (2==numHit) {
                        desc.append( "\n <<<HEAD DESTROYED>>>" );
                        destroyLocation(en, loc);
                    }
                    break;
                case Protomech.SYSTEM_ARMCRIT:
                    if (2==numHit) {
                        desc.append( "\n <<<ARM DESTROYED>>>" );
                        destroyLocation(en,loc);
                    }
                    break;
                case Protomech.SYSTEM_LEGCRIT:
                    if (3==numHit) {
                        desc.append( "\n <<<LEGS DESTROYED>>>" );
                        destroyLocation(en,loc);
                    }
                    break;
                case Protomech.SYSTEM_TORSOCRIT:
                    if (3==numHit) {
                        desc.append( destroyEntity(en, "torso destruction") );
                    }
                    // Torso weapon hits are secondary effects and
                    // do not occur when loading from a scenario.
                    else if ( secondaryEffects ) {
                        int tweapRoll=Compute.d6(1);
                        CriticalSlot newSlot = null;
                        switch (tweapRoll) {
                        case 1:
                        case 2:
                            newSlot = new CriticalSlot
                                ( CriticalSlot.TYPE_SYSTEM,
                                  Protomech.SYSTEM_TORSO_WEAPON_A );
                            desc.append( applyCriticalHit(en, Entity.NONE,
                                                          newSlot,
                                                          secondaryEffects) );
                            break;
                        case 3:
                        case 4:
                            newSlot = new CriticalSlot
                                ( CriticalSlot.TYPE_SYSTEM,
                                  Protomech.SYSTEM_TORSO_WEAPON_B );
                            desc.append( applyCriticalHit(en, Entity.NONE,
                                                          newSlot,
                                                          secondaryEffects) );
                            break;
                        }
                    }
                    break;
                case Protomech.SYSTEM_TORSO_WEAPON_A:
                    Mounted weaponA =( (Protomech) en ).getTorsoWeapon(true);
                    if ( null != weaponA ) {
                        weaponA.setHit(true);
                        desc.append("\n Torso A weapon destroyed");
                    }
                    break;
                case Protomech.SYSTEM_TORSO_WEAPON_B:
                    Mounted weaponB = ( (Protomech) en ).getTorsoWeapon(false);
                    if ( null != weaponB ) {
                        weaponB.setHit(true);
                        desc.append("\n Torso B weapon destroyed");
                    }
                    break;


                } // End switch( cs.getType() )

                // Shaded hits cause pilot damage.
                if ( ((Protomech)en).shaded(loc, numHit) ) {
                    // Destroyed Protomech sections have
                    // already damaged the pilot.
                    int pHits =
                        Protomech.POSSIBLE_PILOT_DAMAGE[ loc ] -
                        ((Protomech)en).getPilotDamageTaken( loc );
                    if (  Math.min(1, pHits) > 0 ) {
                        desc.append( "\n" )
                            .append( damageCrew(en, 1) );
                        pHits = 1 + ((Protomech)en)
                            .getPilotDamageTaken( loc );
                        ((Protomech)en).setPilotDamageTaken
                            ( loc, pHits );
                    }
                } // End have-shaded-hit

            } // End entity-is-protomech
            else {
                desc.append( "\n            <<<CRITICAL HIT>>> on " )
                    .append( Mech.systemNames[cs.getIndex()] )
                    .append( "." );
                switch(cs.getIndex()) {
                case Mech.SYSTEM_COCKPIT :
                    // Don't kill a pilot multiple times.
                    if ( Pilot.DEATH > en.getCrew().getHits() ) {
                        // boink!
                        en.getCrew().setDoomed(true);
                        desc.append( "\n" )
                            .append( destroyEntity(en, "pilot death", true) );
                    }
                    break;
                case Mech.SYSTEM_ENGINE :
                    en.engineHitsThisRound++;

                    boolean engineExploded = false;
                    StringBuffer descBuffer = new StringBuffer();

                    if ( en.engineHitsThisRound >= 2 ) {
                        engineExploded = checkEngineExplosion
                            (en, descBuffer);
                    }

                    desc.append( descBuffer.toString() );

                    if ( !engineExploded ) {
                        int numEngineHits = 0;
                        numEngineHits += en.getHitCriticals
                            (CriticalSlot.TYPE_SYSTEM,
                             Mech.SYSTEM_ENGINE, Mech.LOC_CT);
                        numEngineHits += en.getHitCriticals
                            (CriticalSlot.TYPE_SYSTEM,
                             Mech.SYSTEM_ENGINE, Mech.LOC_RT);
                        numEngineHits += en.getHitCriticals
                            (CriticalSlot.TYPE_SYSTEM,
                             Mech.SYSTEM_ENGINE, Mech.LOC_LT);

                        if ( numEngineHits > 2 ) {
                            // third engine hit
                            desc.append( destroyEntity
                                         (en, "engine destruction") );
                        }
                    }
                    break;
                case Mech.SYSTEM_GYRO :
                    if (en.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                                           Mech.SYSTEM_GYRO, loc) > 1) {
                                // gyro destroyed
                        game.addPSR( new PilotingRollData
                            (en.getId(), PilotingRollData.AUTOMATIC_FAIL,
                             3, "gyro destroyed") );
                    } else {
                                // first gyro hit
                        game.addPSR( new PilotingRollData
                            (en.getId(), 3, "gyro hit") );
                    }
                    break;
                case Mech.ACTUATOR_UPPER_LEG :
                case Mech.ACTUATOR_LOWER_LEG :
                case Mech.ACTUATOR_FOOT :
                    // leg/foot actuator piloting roll
                    game.addPSR( new PilotingRollData
                        (en.getId(), 1, "leg/foot actuator hit") );
                    break;
                case Mech.ACTUATOR_HIP :
                    // hip piloting roll
                    game.addPSR( new PilotingRollData
                        (en.getId(), 2, "hip actuator hit") );
                    break;
                }

            } // End entity-is-mek

        } // End crit-on-system-slot

        // Handle critical hits on equipment slots.
        else if ( CriticalSlot.TYPE_EQUIPMENT == cs.getType() ) {
            cs.setHit(true);
            Mounted mounted = en.getEquipment(cs.getIndex());
            EquipmentType eqType = mounted.getType();
            boolean hitBefore = mounted.isHit();
            desc.append( "\n            <<<CRITICAL HIT>>> on " )
                .append( mounted.getDesc() )
                .append( "." );
            mounted.setHit(true);

            // If the item is the ECM suite of a Mek Stealth system
            // then it's destruction turns off the stealth.
            if ( !hitBefore && eqType instanceof MiscType &&
                 eqType.hasFlag(MiscType.F_ECM) &&
                 mounted.getLinkedBy() != null ) {
                Mounted stealth = mounted.getLinkedBy();
                desc.append( "\n       " )
                    .append( stealth.getType().getName() )
                    .append( " will stop functioning at end of turn." );
                stealth.setMode( "Off" );
            }

            // Handle equipment explosions.
            // Equipment explosions are secondary effects and
            // do not occur when loading from a scenario.
            if ( secondaryEffects && eqType.isExplosive() && !hitBefore ) {
                desc.append( explodeEquipment(en, loc, mounted) );
            }

            // Make sure that ammo in this slot is exhaused.
            if ( mounted.getShotsLeft() > 0 ) {
                mounted.setShotsLeft(0);
            }

        } // End crit-on-equipment-slot

        // Return the results of the damage.
        return desc.toString();

    }

    /**
     * Rolls and resolves critical hits on mechs or vehicles.
     * if rollNumber is false, a single hit is applied - needed for
     * MaxTech Heat Scale rule.
     */
    private String criticalEntity(Entity en, int loc, int critMod, boolean rollNumber) {
        CriticalSlot slot = null;
        StringBuffer desc = new StringBuffer();
        Coords coords = en.getPosition();
        IHex hex = null;
        int hits;
        if (rollNumber) {
            if (null != coords) hex = game.board.getHex (coords);
            desc.append( "        Critical hit on " )
                .append( en.getLocationAbbr(loc) )
                .append( ". " );
            hits = 0;
            int roll = Compute.d6(2);
            desc.append( "Roll = " );
            if ( critMod != 0 ) {
                desc.append( "(" ).append( roll );
                if ( critMod > 0 ) {
                    desc.append( "+" );
                }
                desc.append( critMod ).append( ") = " );
                roll += critMod;
            }
            desc.append( roll ).append( ";" );
            if (roll <= 7) {
                desc.append( " no effect." );
                return desc.toString();
            } else if (roll >= 8 && roll <= 9) {
                hits = 1;
                desc.append( " 1 location." );
            } else if (roll >= 10 && roll <= 11) {
                hits = 2;
                desc.append( " 2 locations." );
            } else if (roll == 12) {
                if (en instanceof Tank) {
                    hits = 3;
                    desc.append( " 3 locations." );
                } else if (en instanceof Protomech) {
                    hits=3;
                    desc.append( " 3 locations" );

                } else if (en.locationIsLeg(loc)) {
                    desc.append( "<<<LIMB BLOWN OFF>>> " )
                        .append( en.getLocationName(loc) )
                        .append( " blown off." );
                    if (en.getInternal(loc) > 0) {
                        destroyLocation(en, loc);
                    }
                    if (null != hex) {
                        if (!hex.containsTerrain (Terrains.LEGS)) {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain(Terrains.LEGS, 1));
                        }
                        else {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain
                                            (Terrains.LEGS,
                                             hex.terrainLevel(Terrains.LEGS)+1));
                        }
                    }
                    sendChangedHex(en.getPosition());
                    return desc.toString();
                } else if (loc == Mech.LOC_RARM || loc == Mech.LOC_LARM) {
                    desc.append( "<<<LIMB BLOWN OFF>>> " )
                        .append( en.getLocationName(loc) )
                        .append( " blown off." );
                    destroyLocation(en, loc);
                    if (null != hex) {
                        if (!hex.containsTerrain( Terrains.ARMS)) {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain(Terrains.ARMS, 1));
                        }
                        else {
                            hex.addTerrain (Terrains.getTerrainFactory().createTerrain
                                            (Terrains.ARMS,
                                             hex.terrainLevel(Terrains.ARMS)+1));
                        }
                    }
                    sendChangedHex(en.getPosition());
                    return desc.toString();
                } else if (loc == Mech.LOC_HEAD) {
                    desc.append( "<<<HEAD BLOWN OFF>>> " )
                        .append( en.getLocationName(loc) )
                        .append( " blown off." );
                    destroyLocation(en, loc);
                    // Don't kill a pilot multiple times.
                    if ( Pilot.DEATH > en.getCrew().getHits() ) {
                        en.crew.setDoomed(true);
                        desc.append( "\n" )
                            .append( destroyEntity(en, "pilot death", true) );
                    }
                    return desc.toString();
                } else {
                    // torso hit
                    hits = 3;
                    desc.append( " 3 locations." );
                }
            }
        } else {
            hits = 1;
        }
        
            // vehicle handle crits in their own 'special' way
        if (en instanceof Tank) {
            Tank tank = (Tank)en;
            for (int x = 0; x < hits; x++) {
                slot = new CriticalSlot( CriticalSlot.TYPE_SYSTEM,
                                         Compute.d6(1) );
                desc.append( applyCriticalHit(en, Entity.NONE, slot, true) );
            }
        }
        else {
            // transfer criticals, if needed
            while (hits > 0 && en.canTransferCriticals(loc)
                   && en.getTransferLocation(loc) != Entity.LOC_DESTROYED) {
                loc = en.getTransferLocation(loc);
                desc.append( "\n            Location is empty, " )
                    .append( "so criticals transfer to " )
                    .append( en.getLocationAbbr(loc) )
                    .append( "." );
            }

            // Roll critical hits in this location.
            while (hits > 0) {

                // Have we hit all available slots in this location?
                if (en.getHittableCriticals(loc) <= 0) {
                    desc.append( "\n            Location has no more " )
                        .append( "hittable critical slots." );
                    break;
                }

                // Randomly pick a slot to be hit.
                int slotIndex = Compute.randomInt
                    ( en.getNumberOfCriticals(loc) );
                slot = en.getCritical(loc, slotIndex);

                // Ignore empty or unhitable slots (this
                // includes all previously hit slots).
                if (slot != null && slot.isHittable()) {
                    desc.append( applyCriticalHit(en, loc, slot, true) );
                    hits--;
                }

            } // Hit another slot in this location.
        }

        return desc.toString();
    }

    /**
     * Rolls and resolves critical hits with no die roll modifiers.
     */
    private String criticalEntity(Entity en, int loc) {
        return criticalEntity(en, loc, 0, true);
    }

    private String criticalEntity (Entity en, int loc, int critMod) {
        return criticalEntity (en, loc, critMod, true);
    }
    
    /**
     * Rolls one critical hit
     */
    private String oneCriticalEntity (Entity en, int loc) {
        return criticalEntity(en, loc, 0, false);
    }
    /**
     * Checks for location breach and returns phase logging.
     * <p/>
     * Please note that dependent locations ARE NOT considered breached!
     *
     * @param   entity the <code>Entity</code> that needs to be checked.
     * @param   loc the <code>int</code> location on the entity that needs
     *          to be checked for a breach.
     * @param   hex the <code>IHex</code> the enitity occupies when checking
     *          This value will be <code>null</code> if the check is the
     *          result of an attack, and non-null if it occurs during movement.
     */
    private String breachCheck(Entity entity, int loc, IHex hex) {
        StringBuffer desc = new StringBuffer();
        // BattleArmor does not breach
        if (entity instanceof Infantry) {
            return "";
        }
        // This handles both water and vacuum breaches.
        if (entity.getLocationStatus(loc) > Entity.LOC_NORMAL) {
            // Does the location have armor (check rear armor on Mek)
            // and is the check due to damage?
            int breachroll = 0;
            if (entity.getArmor(loc) > 0 &&
                (entity instanceof Mech ? (entity.getArmor(loc,true)>0) : true)
                && null == hex) {
                breachroll = Compute.d6(2);
                desc.append( "\n            Possible breach on " )
                    .append( entity.getLocationAbbr(loc) )
                    .append( ". Roll = " )
                    .append( breachroll )
                    .append( "." );
            }
            // Breach by damage or lack of armor.
            if ( breachroll >= 10
                 || !(entity.getArmor(loc) > 0)
                 || !(entity instanceof Mech ? (entity.getArmor(loc,true)>0) :
                      true) ) {
                desc.append( breachLocation(entity, loc, hex) );
            }
        }
        return desc.toString();
    }

    /**
     * Marks all equipment in a location on an entity as useless.
     *
     * @param   entity the <code>Entity</code> that needs to be checked.
     * @param   loc the <code>int</code> location on the entity that needs
     *          to be checked for a breach.
     * @param   hex the <code>IHex</code> the enitity occupies when checking
     *          This value will be <code>null</code> if the check is the
     *          result of an attack, and non-null if it occurs during movement.
     */
    private String breachLocation(Entity entity, int loc, IHex hex) {
        StringBuffer desc = new StringBuffer();
        if (entity.getInternal(loc) < 0 ||
            entity.getLocationStatus(loc) < Entity.LOC_NORMAL) {
            //already destroyed or breached? don't bother
            return desc.toString();
        }
        desc.append( " <<<" )
            .append( entity.getShortName() )
            .append( " ")
            .append( entity.getLocationAbbr(loc) )
            .append( " BREACHED>>>" );
        if (entity instanceof Tank) {
            desc.append( destroyEntity(entity, "hull breach in vacuum", true, true) );
            return desc.toString();
        }
        // equipment and crits will be marked in applyDamage?
        // equipment marked missing
        for (Enumeration i = entity.getEquipment(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getLocation() == loc) {
                mounted.setBreached(true);
            }
        }
        // all critical slots set as useless
        for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = entity.getCritical(loc, i);
            if (cs != null) {
                // for every undamaged actuator destroyed by breaching,
                // we make a PSR (see bug 1040858)
                if (entity.locationIsLeg(loc)) {
                    if (cs.isHittable()) {
                        switch(cs.getIndex()) {
                            case Mech.ACTUATOR_UPPER_LEG :
                            case Mech.ACTUATOR_LOWER_LEG :
                            case Mech.ACTUATOR_FOOT :
                                // leg/foot actuator piloting roll
                                game.addPSR( new PilotingRollData
                                    (entity.getId(), 1, "leg/foot actuator hit") );
                                break;
                            case Mech.ACTUATOR_HIP :
                                // hip piloting roll (at +0, because we get the +2
                                // anyway because the location is breached
                                // phase report will look a bit weird, but the roll
                                // is correct
                                game.addPSR( new PilotingRollData
                                    (entity.getId(), 0, "hip actuator hit") );
                                break;
                        }
                    }
                }
                cs.setBreached(true);
            }
        }

        //Check location for engine/cockpit breach and report accordingly
        if (loc == Mech.LOC_CT) {
            desc.append( destroyEntity(entity, "hull breach") );
        }
        if (loc == Mech.LOC_HEAD) {
            entity.crew.setDoomed(true);
            desc.append( destroyEntity(entity, "hull breach") );
            desc.append( "\n*** " )
                .append( entity.getDisplayName() );
            if (entity.getLocationStatus(loc) == Entity.LOC_WET)
                desc.append( " Pilot Drowned! ***" );
            else desc.append( " Pilot died to explosive decompression! ***");
        }

        // Set the status of the location.
        // N.B. if we set the status before rolling water PSRs, we get a
        // "LEG DESTROYED" modifier; setting the status after gives a hip
        // actuator modifier.
        entity.setLocationStatus(loc, Entity.LOC_BREACHED);

        // Did the hull breach destroy the engine?
        if (entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_LT) +
            entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_CT) +
            entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_RT) >= 3) {
            desc.append( destroyEntity(entity, "engine destruction") );
        }

        return desc.toString();
    }

    /**
     * Marks all equipment in a location on an entity as destroyed.
     */
    void destroyLocation(Entity en, int loc) {
        // if it's already marked as destroyed, don't bother
        if (en.getInternal(loc) < 0) {
            return;
        }
        // mark armor, internal as doomed
        en.setArmor(Entity.ARMOR_DOOMED, loc, false);
        en.setInternal(Entity.ARMOR_DOOMED, loc);
        if (en.hasRearArmor(loc)) {
            en.setArmor(Entity.ARMOR_DOOMED, loc, true);
        }
        // equipment marked missing
        for (Enumeration i = en.getEquipment(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getLocation() == loc) {
                mounted.setMissing(true);
            }
        }
        // all critical slots set as missing
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            if (cs != null) {
                // count engine hits for maxtech engine explosions
                if (cs.getType() == CriticalSlot.TYPE_SYSTEM &&
                    cs.getIndex() == Mech.SYSTEM_ENGINE &&
                    !cs.isDamaged()) {
                        en.engineHitsThisRound++;
                }
                cs.setMissing(true);
            }
        }
        // if it's a leg, the entity falls
        if (en instanceof Mech && en.locationIsLeg(loc)) {
            game.addPSR(new PilotingRollData(en.getId(), PilotingRollData.AUTOMATIC_FAIL, 5, "leg destroyed"));
        }
        // dependent locations destroyed
        if (en.getDependentLocation(loc) != Mech.LOC_NONE) {
            destroyLocation(en, en.getDependentLocation(loc));
        }
    }

    /**
     * Mark the unit as destroyed!  Units transported in the destroyed unit
     * will get a chance to escape.
     *
     * @param   entity - the <code>Entity</code> that has been destroyed.
     * @param   reason - a <code>String</code> detailing why the entity
     *          was destroyed.
     * @return  a <code>String</code> that can be sent to the output log.
     */
    private String destroyEntity(Entity entity, String reason) {
        return destroyEntity( entity, reason, true );
    }

    /**
     * Marks a unit as destroyed!  Units transported inside the destroyed
     * unit will get a chance to escape unless the destruction was not
     * survivable.
     *
     * @param   entity - the <code>Entity</code> that has been destroyed.
     * @param   reason - a <code>String</code> detailing why the entity
     *          was destroyed.
     * @param   survivable - a <code>boolean</code> that identifies the
     *          desctruction as unsurvivable for transported units.
     * @return  a <code>String</code> that can be sent to the output log.
     */
    private String destroyEntity(Entity entity, String reason,
                                 boolean survivable) {
        // Generally, the entity can still be salvaged.
        return this.destroyEntity( entity, reason, survivable, true );
    }

    /**
     * Marks a unit as destroyed!  Units transported inside the destroyed
     * unit will get a chance to escape unless the destruction was not
     * survivable.
     *
     * @param   entity - the <code>Entity</code> that has been destroyed.
     * @param   reason - a <code>String</code> detailing why the entity
     *          was destroyed.
     * @param   survivable - a <code>boolean</code> that identifies the
     *          desctruction as unsurvivable for transported units.
     * @param   canSalvage - a <code>boolean</code> that indicates if
     *          the unit can be salvaged (or cannibalized for spare parts).
     *          If <code>true</code>, salvage operations are possible, if
     *          <code>false</code>, the unit is too badly damaged.
     * @return  a <code>String</code> that can be sent to the output log.
     */
    private String destroyEntity(Entity entity, String reason,
                                 boolean survivable, boolean canSalvage) {
        StringBuffer sb = new StringBuffer();

        // The unit can suffer an ammo explosion after it has been destroyed.
        int condition = Entity.REMOVE_SALVAGEABLE;
        if ( !canSalvage ) {
            entity.setSalvage( canSalvage );
            condition = Entity.REMOVE_DEVASTATED;
        }

        // Ignore entities that are already destroyed.
        if (!entity.isDoomed() && !entity.isDestroyed()) {
            sb.append("\n*** ");
            sb.append(entity.getDisplayName());
            sb.append(" DESTROYED by ");
            sb.append(reason);
            sb.append("! ***\n");

            entity.setDoomed(true);

            // Kill any picked up MechWarriors
            Enumeration iter = entity.getPickedUpMechWarriors().elements();
            while (iter.hasMoreElements() ) {
                Integer mechWarriorId = (Integer)iter.nextElement();
                Entity mw = game.getEntity(mechWarriorId.intValue());
                mw.setDestroyed(true);
                game.removeEntity( mw.getId(), condition );
                send( createRemoveEntityPacket(mw.getId(), condition) );
                sb.append("\n*** " ).append( mw.getDisplayName() +
                          " died in the wreckage. ***\n");
            }

            // Handle escape of transported units.
            iter = entity.getLoadedUnits().elements();
            if ( iter.hasMoreElements() ) {
                Entity other = null;
                Coords curPos = entity.getPosition();
                IHex entityHex = game.getBoard().getHex( curPos );
                int curFacing = entity.getFacing();
                while ( iter.hasMoreElements() ) {
                    other = (Entity) iter.nextElement();

                    // Can the other unit survive?
                    if ( !survivable ) {

                        // Nope.
                        other.setDestroyed(true);
                        game.moveToGraveyard( other.getId() );
                        send( createRemoveEntityPacket(other.getId(),
                                                       condition) );
                        sb.append("\n*** " ).append( other.getDisplayName() +
                                  " was trapped in the wreckage. ***\n");

                    }
                    // Can we unload the unit to the current hex?
                    // TODO : unloading into stacking violation is not
                    //        explicitly prohibited in the BMRr.
                    else if (null != Compute.stackingViolation(game, other.getId(), curPos)
                             || other.isHexProhibited(entityHex) ) {
                        // Nope.
                        other.setDestroyed(true);
                        game.moveToGraveyard( other.getId() );
                        send( createRemoveEntityPacket(other.getId(),
                                                       condition) );
                        sb.append("\n*** " ).append( other.getDisplayName() +
                                  " tried to escape the wreckage, but couldn't. ***\n");
                    } // End can-not-unload
                    else {
                        // The other unit survives.
                        this.unloadUnit( entity, other, curPos, curFacing );
                    }

                } // Handle the next transported unit.

            } // End has-transported-unit

            // Handle transporting unit.
            if ( Entity.NONE != entity.getTransportId() ) {
                final Entity transport = game.getEntity
                    ( entity.getTransportId() );
                Coords curPos = transport.getPosition();
                int curFacing = transport.getFacing();
                this.unloadUnit( transport, entity, curPos, curFacing );
                this.entityUpdate( transport.getId() );
            } // End unit-is-transported

            // Is this unit being swarmed?
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );

                // remove the swarmer from the move queue
                game.removeTurnFor(swarmer);
                send(createTurnVectorPacket());

                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                sb.append( swarmer.getDisplayName() );
                sb.append( " ends its swarm attack.\n" );
                this.entityUpdate( swarmerId );
            }

            // Is this unit swarming somebody?
            final int swarmedId = entity.getSwarmTargetId();
            if ( Entity.NONE != swarmedId ) {
                final Entity swarmed = game.getEntity( swarmedId );
                swarmed.setSwarmAttackerId( Entity.NONE );
                entity.setSwarmTargetId( Entity.NONE );
                sb.append( swarmed.getDisplayName() );
                sb.append( " is freed from its swarm attack.\n" );
                this.entityUpdate( swarmedId );
            }

        } // End entity-not-already-destroyed.

        return sb.toString();

    }
    

    /**
     * Makes a piece of equipment on a mech explode!  POW!  This expects either
     * ammo, or an explosive weapon.
     */
    private String explodeEquipment(Entity en, int loc, int slot) {
        return explodeEquipment(en, loc, en.getEquipment(en.getCritical(loc, slot).getIndex()));
    }

    private String explodeEquipment(Entity en, int loc, Mounted mounted) {
        StringBuffer desc = new StringBuffer();
        // is this already destroyed?
        if (mounted.isDestroyed()) {
            System.err.println("server: explodeEquipment called on destroyed"
            + " equipment (" + mounted.getName() + ")");
            return "";
        }

        // special-case.  RACs only explode when jammed
        if (mounted.getType() instanceof WeaponType &&
                ((WeaponType)mounted.getType()).getAmmoType() == AmmoType.T_AC_ROTARY) {
            if (!mounted.isJammed()) {
                return "";
            }
        }

        // Inferno ammo causes heat buildup as well as the damage
        if ( mounted.getType() instanceof AmmoType &&
             ((AmmoType)mounted.getType()).getMunitionType() == AmmoType.M_INFERNO) {
            en.heatBuildup += 30;
        }

        // determine and deal damage
        int damage = mounted.getExplosionDamage();

        if (damage <= 0) {
            return "";
        }

        desc.append("\n*** ");
        desc.append(mounted.getName());
        desc.append(" EXPLODES!  ");
        desc.append(damage);
        desc.append(" DAMAGE! ***");

        desc.append(damageEntity(en, new HitData(loc), damage, true));
        desc.append("\n");
        int pilotDamage = 2;
        if (en.getCrew().getOptions().booleanOption("pain_resistance")) pilotDamage = 1;
        if (en.getCrew().getOptions().booleanOption("iron_man")) pilotDamage = 1;
        desc.append(damageCrew(en, pilotDamage));
        if ( en.crew.isDoomed() || en.crew.isDead() ) {
            desc.append( destroyEntity(en, "crew death", true) );
        } else {
            desc.append("\n");
        }

        return desc.toString();
    }

    /**
     * Makes one slot of ammo, determined by certain rules, explode on a mech.
     */
    private String explodeAmmoFromHeat(Entity entity) {
        int damage = 0;
        int rack = 0;
        int boomloc = -1;
        int boomslot = -1;
        for (int j = 0; j < entity.locations(); j++) {
            for (int k = 0; k < entity.getNumberOfCriticals(j); k++) {
                CriticalSlot cs = entity.getCritical(j, k);
                if (cs == null || cs.isDestroyed() || cs.isHit() || cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                    continue;
                }
                Mounted mounted = entity.getEquipment(entity.getCritical(j, k).getIndex());
                if (!(mounted.getType() instanceof AmmoType)) {
                    continue;
                }
                AmmoType atype = (AmmoType)mounted.getType();
                if (!atype.isExplosive()) {
                    continue;
                }
                // BMRr, pg. 48, compare one rack's
                // damage.  Ties go to most rounds.
                int newRack = atype.getDamagePerShot() * atype.getRackSize();
                int newDamage = mounted.getExplosionDamage();
                if ( !mounted.isHit() && ( rack < newRack ||
                       (rack == newRack && damage < newDamage) ) ) {
                    rack = newRack;
                    damage = newDamage;
                    boomloc = j;
                    boomslot = k;
                }
            }
        }
        if (boomloc != -1 && boomslot != -1) {
            CriticalSlot slot = entity.getCritical(boomloc, boomslot);
            slot.setHit(true);
            entity.getEquipment(slot.getIndex()).setHit(true);
            return explodeEquipment(entity, boomloc, boomslot);
        } else {
            return "  Luckily, there is no ammo to explode.\n";
        }
    }

    /**
     * Makes a mech fall.
     */
    private void doEntityFall(Entity entity, Coords fallPos, int height, int facing, PilotingRollData roll) {
        IHex fallHex = game.board.getHex(fallPos);
        // we don't need to deal damage yet, if the entity is doing DFA
        if (entity.isMakingDfa()) {
            phaseReport.append("But, since the 'mech is making a death from above attack, damage will be dealt during the physical phase.\n");
            entity.setProne(true);
            return;
        }
        // facing after fall
        String side;
        int table;
        switch(facing) {
            case 1:
            case 2:
                side = "right side";
                table = ToHitData.SIDE_RIGHT;
                break;
            case 3:
                side = "rear";
                table = ToHitData.SIDE_REAR;
                break;
            case 4:
            case 5:
                side = "left side";
                table = ToHitData.SIDE_LEFT;
                break;
            case 0:
            default:
                side = "front";
                table = ToHitData.SIDE_FRONT;
        }

        int waterDepth = fallHex.terrainLevel(Terrains.WATER);
        int damageHeight = height;


        // HACK: if the dest hex is water, assume that the fall height given is
        // to the floor of the hex, and modifiy it so that it's to the surface
        if (waterDepth > 0) {
            damageHeight = Math.max(height - waterDepth, 0);
        }

        // calculate damage for hitting the surface
        int damage = (int)Math.round(entity.getWeight() / 10.0) * (damageHeight + 1);
        // calculate damage for hitting the ground, but only if we actually fell
        // into water
        // if we fell onto the water surface, that damage is halved.
        int waterDamage = 0;
        if (waterDepth > 0) {
            damage /= 2;
            waterDamage = (int)Math.round(entity.getWeight() / 10.0) * (waterDepth + 1) /2;
        }

        // If the waterDepth is larger than the fall height,
        // we fell underwater
        if (waterDepth > height) {
            damage = 0;
            waterDamage = (int)Math.round(entity.getWeight() / 10.0) * (height + 1) /2;
        }
        // adjust damage for gravity
        damage = (int)Math.round(damage * game.getOptions().floatOption("gravity"));
        waterDamage = (int)Math.round(waterDamage * game.getOptions().floatOption("gravity"));

        // report falling
        if (waterDamage == 0) {
            phaseReport.append("    " ).append( entity.getDisplayName() ).append( " falls on its " ).append( side ).append( ", suffering " ).append( damage ).append( " damage.");
        } else if (damage > 0) {
            phaseReport.append("    " ).append( entity.getDisplayName() ).append( " falls on its " ).append( side ).append( ", suffering " ).append( damage ).append( " damage when hitting the water surface and ")
                       .append(waterDamage).append(" when hitting the ground");
        } else {
            phaseReport.append("    " ).append( entity.getDisplayName() ).append( " falls on its " ).append( side ).append( ", suffering " ).append( waterDamage ).append( " damage.");
        }

        damage += waterDamage;

        // Any swarming infantry will be dislodged, but we don't want to
        // interrupt the fall's report.  We have to get the ID now because
        // the fall may kill the entity which will reset the attacker ID.
        final int swarmerId = entity.getSwarmAttackerId();

        // Positioning must be prior to damage for proper handling of breaches
        // Only Mechs can fall prone.
        if ( entity instanceof Mech ) {
            entity.setProne(true);
        }
        entity.setPosition(fallPos);
        entity.setFacing((entity.getFacing() + (facing - 1)) % 6);
        entity.setSecondaryFacing(entity.getFacing());
        if (fallHex.terrainLevel(Terrains.WATER) > 0) {
            for (int loop=0; loop< entity.locations();loop++){
                entity.setLocationStatus(loop, Entity.LOC_WET);
            }
        }

        // standard damage loop
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, table);
            phaseReport.append(damageEntity(entity, hit, cluster));
            damage -= cluster;
        }

        //check for location exposure
        doSetLocationsExposure(entity, fallHex, fallHex.hasPavement(), false);

        // pilot damage?
        roll.removeAutos();

        if (height > 0) {
            roll.addModifier(height, "height of fall");
        }

        if (roll.getValue() == PilotingRollData.AUTOMATIC_FAIL) {
            phaseReport.append("\nPilot of " ).append( entity.getDisplayName()
            ).append( " \"" ).append( entity.crew.getName() ).append( "\" cannot avoid damage.\n");
            phaseReport.append(damageCrew(entity, 1) ).append( "\n");
        } else {
            int diceRoll = Compute.d6(2);
            phaseReport.append("\nPilot of " ).append( entity.getDisplayName()
            ).append( " \"" ).append( entity.crew.getName() ).append( "\" must roll " ).append( roll.getValueAsString()
            ).append( " to avoid damage; rolls " ).append( diceRoll ).append( " : ");
            if (diceRoll >= roll.getValue()) {
                phaseReport.append("succeeds.\n");
            } else {
                phaseReport.append("fails.\n");
                phaseReport.append(damageCrew(entity, 1) ).append( "\n");
            }
        }

        // Now dislodge any swarming infantry.
        if ( Entity.NONE != swarmerId ) {
            final Entity swarmer = game.getEntity( swarmerId );
            entity.setSwarmAttackerId( Entity.NONE );
            swarmer.setSwarmTargetId( Entity.NONE );
            // Did the infantry fall into water?
            if ( fallHex.terrainLevel(Terrains.WATER) > 0 ) {
                // Swarming infantry die.
                swarmer.setPosition( fallPos );
                phaseReport.append("    ")
                    .append(swarmer.getDisplayName())
                    .append(" is dislodged and drowns!")
                    .append(destroyEntity(swarmer, "a watery grave", false));
            } else {
                // Swarming infantry take an 11 point hit.
                // ASSUMPTION : damage should not be doubled.
                phaseReport.append("    ")
                    .append(swarmer.getDisplayName())
                    .append(" is dislodged and suffers 11 damage.")
                    .append( damageEntity(swarmer,
                                          swarmer.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT),
                                          11) )
                    .append( "\n" );
            }
            swarmer.setPosition( fallPos );
            entityUpdate( swarmerId );
        } // End dislodge-infantry

        // clear all PSRs after a fall -- the Mek has already failed ONE and fallen, it'd be cruel to make it fail some more!
        game.resetPSRs(entity);
    }

    /**
     * The mech falls into an unoccupied hex from the given height above
     */
    private void doEntityFall(Entity entity, Coords fallPos, int height, PilotingRollData roll) {
        doEntityFall(entity, fallPos, height, Compute.d6(1), roll);
    }

    /**
     * The mech falls down in place
     */
    private void doEntityFall(Entity entity, PilotingRollData roll) {
        doEntityFall(entity, entity.getPosition(), 0, roll);
    }

    /**
     * Report:
     * - Any ammo dumps beginning the following round.
     * - Any ammo dumps that have ended with the end of this round.
     */
    private void resolveAmmoDumps() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            for (Enumeration j = entity.getAmmo(); j.hasMoreElements(); ) {
                Mounted m = (Mounted)j.nextElement();
                if (m.isPendingDump()) {
                    phaseReport.append(entity.getDisplayName()).append(" will begin dumping ")
                    .append(m.getName()).append(" in the following round.\n");
                }
                else if (m.isDumping()) {
                    phaseReport.append(entity.getDisplayName()).append(" has finished dumping ")
                    .append(m.getName()).append(".\n");
                }
            }
        }
    }

    /**
     * This debug/profiling function will print the current time
     * (in milliseconds) to the log.  If the boolean is true, the
     * garbage collector will be called in an attempt to minimize
     * timing errors.  You should try and minimize applications
     * being run in the background when using this function.
     * Note that MS Windows only has 10 milisecond resolution.
     *
     * The function should be optimized completely out of the code
     * when the first if-statement below reads "if (false)...", so
     * performance shouldn't be impacted if you leave calls to this
     * function in the code (I think).
     */
    private void debugTime(String s, boolean collectGarbage) {
        //Change the "false" below to "true" to enable this function
        if (false) {
            if (collectGarbage)
                System.gc();
            System.out.println(s + ": " + System.currentTimeMillis());
        }
    }

    /**
     * Make fires spread, smoke spread, and make sure that all fires
     * started this turn are marked as "burning" for next turn.
     *
     * A "FIRE" terrain has one of two levels:
     *  1 (Created this turn, and so can't spread of generate heat)
     *  2 (Created as a result of spreading fire or on a previous turn)
     *
     * Since fires created at end of turn act normally in the following turn,
     * spread fires have level 2.
     *
     * At NO TIME should any fire created outside this function have a level of
     * 2, nor should anything except this function SET fires to level 2.
     *
     * Newly created "spread" fires have a level of 1, so that they do not
     * spread in the turn they are created.  After all spreading has been
     * completed, all burning hexes are set to level 2.
     */
    private void resolveFire() {
        IBoard board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        int windDirection = game.getWindDirection();

        // Get the position map of all entities in the game.
        Hashtable positionMap = game.getPositionMap();

        // Build vector to send for updated buildings at once.
        Vector burningBldgs = new Vector();

        // If we're in L3 rules, process smoke FIRST, before any fires spread or smoke is produced.
        if (game.getOptions().booleanOption("maxtech_fire")) {
            resolveSmoke();
        }

        // Cycle through all buildings, checking for fire.
        // ASSUMPTION: buildings don't lose 2 CF on the turn a fire starts.
        // ASSUMPTION: multi-hex buildings lose 2 CF max, regardless of # fires
        Enumeration buildings = game.board.getBuildings();
        while ( buildings.hasMoreElements() ) {
            Building bldg = (Building) buildings.nextElement();
            if ( bldg.isBurning() ) {
                int cf = bldg.getCurrentCF() - 2;
                bldg.setCurrentCF( cf );

                // Does the building burned down?
                if ( cf == 0 ) {
                    phaseReport.append( bldg.getName() )
                        .append( " has burned to the ground!\n" );
                    this.collapseBuilding( bldg, positionMap );
                }

                // If it doesn't collapse under its load, mark it for update.
                else if ( !checkForCollapse(bldg, positionMap) ) {
                    bldg.setPhaseCF( cf );
                    burningBldgs.addElement( bldg );
                }
            }
        }

        debugTime("resolve fire 1", true);

        // Cycle through all hexes, checking for fire.
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {

            for (int currentYCoord = 0; currentYCoord < height;
                 currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord,
                                                  currentYCoord);
                IHex currentHex = board.getHex(currentXCoord, currentYCoord);
                boolean infernoBurning = board.burnInferno( currentCoords );

                // optional rule, woods burn down
                if (currentHex.containsTerrain(Terrains.WOODS) && currentHex.terrainLevel(Terrains.FIRE) == 2 && game.getOptions().booleanOption("woods_burn_down")) {
                    burnDownWoods(currentCoords);
                }

                // If the woods has been cleared, or the building
                // has collapsed put non-inferno fires out.
                if ( currentHex.containsTerrain(Terrains.FIRE) && !infernoBurning &&
                     !(currentHex.containsTerrain(Terrains.WOODS)) &&
                     !(currentHex.containsTerrain(Terrains.BUILDING)) ) {
                    removeFire(currentXCoord, currentYCoord, currentHex);
                }

                // Was the fire started on a previous turn?
                else if (currentHex.terrainLevel(Terrains.FIRE) == 2)
                {
                    if ( infernoBurning ) {
                        phaseReport.append( "Inferno fire at " );
                    } else {
                        phaseReport.append( "Fire at " );
                    }
                    phaseReport.append( currentCoords.getBoardNum() )
                        .append( " is burning brightly.\n" );
                    spreadFire(currentXCoord, currentYCoord, windDirection);
                }
            }
        }

        debugTime("resolve fire 1 end, begin resolve fire 2", true);

        //  Loop a second time, to set all fires to level 2 before next turn, and add smoke.
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {

            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                IHex currentHex = board.getHex(currentXCoord,currentYCoord);
                // if the fire in the hex was started this turn
                if (currentHex.terrainLevel(Terrains.FIRE) == 1) {
                    currentHex.removeTerrain(Terrains.FIRE);
                    currentHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 2));
                    sendChangedHex(currentCoords);
                    phaseReport.append( "Fire at " )
                        .append( currentCoords.getBoardNum() )
                        .append( " was started this round.\n" );

                    // If the hex contains a building, set it on fire.
                    Building bldg = game.board.getBuildingAt( currentCoords );
                    if ( bldg != null ) {
                        bldg.setBurning( true );
                        burningBldgs.addElement( bldg );
                    }
                }
                // If the L3 smoke rule is off, add smoke normally, otherwise call the L3 method
                if (currentHex.containsTerrain(Terrains.FIRE) && !game.getOptions().booleanOption("maxtech_fire")) {
                    addSmoke(currentXCoord, currentYCoord, windDirection);
                    addSmoke(currentXCoord, currentYCoord, (windDirection+1)%6);
                    addSmoke(currentXCoord, currentYCoord, (windDirection+5)%6);
                    board.initializeAround(currentXCoord,currentYCoord);
                }
                else if (currentHex.containsTerrain(Terrains.FIRE) && game.getOptions().booleanOption("maxtech_fire")) {
                    addL3Smoke(currentXCoord, currentYCoord);
                    board.initializeAround(currentXCoord, currentYCoord);
                }
            }
        }

        debugTime("resolve fire 2 end", false);

        // If any buildings are burning, update the clients.
        if ( !burningBldgs.isEmpty() ) {
            send( createUpdateBuildingCFPacket(burningBldgs) );
        }

        // If we're in L3 rules, shift the wind.
        if (game.getOptions().booleanOption("maxtech_fire")) {
            game.determineWind();
        }

    }  // End the ResolveFire() method

    public void burnDownWoods(Coords coords) {
        IHex hex = game.board.getHex(coords);
        int roll = Compute.d6(2);
        if(roll >= 11) {
            if(hex.terrainLevel(Terrains.WOODS) > 1) {
                hex.removeTerrain(Terrains.WOODS);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.WOODS, 1));
                phaseReport.append( "Heavy woods at ")
                           .append( coords.getBoardNum() )
                           .append( " burns down to Light Woods!\n" );
            }
            else if(hex.terrainLevel(Terrains.WOODS) == 1) {
                hex.removeTerrain(Terrains.WOODS);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.ROUGH, 1));
                phaseReport.append( "Light woods at ")
                           .append( coords.getBoardNum() )
                           .append( " burns down to Rough and goes out!!\n" );
            }
            sendChangedHex(coords);
        }
    }

    /**
     * Spreads the fire around the specified coordinates.
     */
    public void spreadFire(int x, int y, int windDir) {
        Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir);

        spreadFire(nextCoords, 9);

        // Spread to the next hex downwind on a 12 if the first hex wasn't burning...
        IHex nextHex = game.getBoard().getHex(nextCoords);
        if (nextHex != null && !(nextHex.containsTerrain(Terrains.FIRE))) {
            // we've already gone one step in the wind direction, now go another
            spreadFire(nextCoords.translated(windDir), 12);
        }

        // spread fire 60 degrees clockwise....
        spreadFire(src.translated((windDir + 1) % 6), 11);

        // spread fire 60 degrees counterclockwise
        spreadFire(src.translated((windDir + 5) % 6), 11);
    }

    /**
     * Spreads the fire, and reports the spread, to the specified hex, if
     * possible, if the hex isn't already on fire, and the fire roll is made.
     */
    public void spreadFire(Coords coords, int roll) {
        IHex hex = game.getBoard().getHex(coords);
        if (hex == null) {
            // Don't attempt to spread fire off the board.
            return;
        }
        if (!(hex.containsTerrain(Terrains.FIRE)) && ignite(hex, roll)) {
            sendChangedHex(coords);
            phaseReport.append("Fire spreads to " ).append( coords.getBoardNum() ).append( "!\n");
        }
    }

    /**
     * Returns true if the hex is set on fire with the specified roll.  Of
     * course, also checks to see that fire is possible in the specified hex.
     *
     * @param   hex - the <code>IHex</code> to be lit.
     * @param   roll - the <code>int</code> target number for the ignition roll
     * @param   bAnyTerrain - <code>true</code> if the fire can be lit in any
     *          terrain.  If this value is <code>false</code> the hex will be
     *          lit only if it contains Woods or a Building.
     * @param   bReportAttempt - <code>true</code> if the attempt roll should
     *          be added to the report.
     */
   public boolean ignite( IHex hex, int roll, boolean bAnyTerrain,
                          boolean bReportAttempt ) {

        // The hex might be null due to spreadFire translation
        // goes outside of the board limit.
        if ( !game.getOptions().booleanOption("fire") || null == hex ) {
            return false;
        }

        // The hex may already be on fire.
        if ( hex.containsTerrain( Terrains.FIRE ) ) {
            return true;
        }

        if ( !bAnyTerrain &&
             !(hex.containsTerrain(Terrains.WOODS)) &&
             !(hex.containsTerrain(Terrains.BUILDING)) ) {
            return false;
        }

        int fireRoll = Compute.d6(2);
        if (bReportAttempt) {
            phaseReport.append( "           Needs " )
                .append( roll )
                .append( " to ignite, rolls " )
                .append( fireRoll )
                .append( "\n" );
        }
        if (fireRoll >= roll) {
            hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the hex is set on fire with the specified roll.  Of
     * course, also checks to see that fire is possible in the specified hex.
     * This version of the method will not report the attempt roll.
     *
     * @param   hex - the <code>IHex</code> to be lit.
     * @param   roll - the <code>int</code> target number for the ignition roll
     * @param   bAnyTerrain - <code>true</code> if the fire can be lit in any
     *          terrain.  If this value is <code>false</code> the hex will be
     *          lit only if it contains Woods or a Building.
     */
    public boolean ignite(IHex hex, int roll, boolean bAnyTerrain) {
       return ignite(hex, roll, bAnyTerrain, false);
    }

    public boolean ignite(IHex hex, int roll) {
        // default signature, assuming only woods can burn
        return ignite(hex, roll, false);
    }

    public void removeFire(int x, int y, IHex hex) {
        Coords fireCoords = new Coords(x, y);
        hex.removeTerrain(Terrains.FIRE);
        sendChangedHex(fireCoords);
        if (!game.getOptions().booleanOption("maxtech_fire")) {
            // only remove the 3 smoke hexes if under L2 rules!
            int windDir = game.getWindDirection();
            removeSmoke(x, y, windDir);
            removeSmoke(x, y, (windDir + 1) % 6);
            removeSmoke(x, y, (windDir + 5) % 6);
        }
        phaseReport.append("Fire at " ).append( fireCoords.getBoardNum() ).append( " goes out due to lack of fuel!\n");
    }

    /**
     * called when a fire is burning.  Adds smoke to hex in the direction specified.  Called 3 times per fire hex
     */
    public void addSmoke(int x, int y, int windDir) {
        Coords smokeCoords = new Coords(Coords.xInDir(x, y, windDir), Coords.yInDir(x, y, windDir));
        IHex nextHex = game.getBoard().getHex(smokeCoords);
        if (nextHex != null && !(nextHex.containsTerrain(Terrains.SMOKE))) {
            nextHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 1));
            sendChangedHex(smokeCoords);
            phaseReport.append("Smoke fills " ).append( smokeCoords.getBoardNum() ).append( "!\n");
        }
    }

    /**
     * Called under L3 fire rules. Called once.
     */
    public void addL3Smoke(int x, int y) {
        IBoard board = game.getBoard();
        Coords smokeCoords = new Coords(x, y);
        IHex smokeHex = game.getBoard().getHex(smokeCoords);
        boolean infernoBurning = board.isInfernoBurning( smokeCoords );
        if (smokeHex == null) {
            return;
        }
        // Have to check if it's inferno smoke or from a heavy/hardened building - heavy smoke from those
        if(infernoBurning || Building.MEDIUM < smokeHex.terrainLevel(Terrains.BUILDING)) {
            if (smokeHex.terrainLevel(Terrains.SMOKE) == 2){
                phaseReport.append("Heavy smoke continues to fill ").append( smokeCoords.getBoardNum() ).append( ".\n");
            } else {
                if (smokeHex.terrainLevel(Terrains.SMOKE) == 1){
                    //heavy smoke overrides light
                    smokeHex.removeTerrain(Terrains.SMOKE);
                }
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 2));
                sendChangedHex(smokeCoords);
                phaseReport.append("Heavy smoke fills ").append( smokeCoords.getBoardNum() ).append( "!\n");
            }
        }
        else {
            if (smokeHex.terrainLevel(Terrains.SMOKE) == 2){
                phaseReport.append("Heavy smoke overpowers light smoke in ").append( smokeCoords.getBoardNum() ).append( ".\n");
            } else if (smokeHex.terrainLevel(Terrains.SMOKE) == 1){
                phaseReport.append("Light smoke continues to fill ").append( smokeCoords.getBoardNum() ).append( ".\n");
            } else {
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 1));
                sendChangedHex(smokeCoords);
                phaseReport.append("Light smoke fills ").append( smokeCoords.getBoardNum() ).append( "!\n");
            }
        }
    }

    public void removeSmoke(int x, int y, int windDir) { // L2 smoke removal
        Coords smokeCoords = new Coords(Coords.xInDir(x, y, windDir), Coords.yInDir(x, y, windDir));
        IHex nextHex = game.getBoard().getHex(smokeCoords);
        if (nextHex != null && nextHex.containsTerrain(Terrains.SMOKE)) {
            nextHex.removeTerrain(Terrains.SMOKE);
            sendChangedHex(smokeCoords);
            phaseReport.append("Smoke clears from " ).append( smokeCoords.getBoardNum() ).append( "!\n");
        }
    }

    /**
     * Under L3 rules, smoke drifts in the direction of the wind and has a chance to dissipate.
     * This function will keep track of hexes to have smoke removed and added, since there's no other way
     * to tell if a certain smoke cloud has drifted that turn.
     * This method creates the class SmokeDrift to store hex and size data for the smoke clouds.
     * This method calls functions driftAddSmoke, driftSmokeDissipate, driftSmokeReport
     */
    private void resolveSmoke() {
        IBoard board = game.getBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        int windDir = game.getWindDirection();
        int windStr = game.getWindStrength();
        Vector SmokeToAdd = new Vector();

        class SmokeDrift { // hold the hex and level of the smoke cloud
            public Coords coords;
            public int size;

            public SmokeDrift(Coords c, int s) {
                coords = c;
                size = s;
            }

            public SmokeDrift(SmokeDrift sd) {
                sd.coords = coords;
                sd.size = size;
            }
        }

        // Cycle through all hexes, checking for smoke, IF the wind is higher than calm! Calm means no drift!
        if(windStr > 0) {

            debugTime("resolve smoke 1", true);

            for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {

                for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                    Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                    IHex currentHex = board.getHex(currentXCoord, currentYCoord);

                    // check for existence of smoke, then add it to the vector...if the wind is not Calm!
                    if (currentHex.containsTerrain(Terrains.SMOKE)){
                        int smokeLevel = currentHex.terrainLevel(Terrains.SMOKE);
                        Coords smokeCoords = driftAddSmoke(currentXCoord, currentYCoord, windDir, windStr);
                        //                        System.out.println(currentCoords.toString() + " to " + smokeCoords.toString());
                        IHex smokeHex = game.getBoard().getHex(smokeCoords);
                        if( board.contains(smokeCoords)) { // don't add it to the vector if it's not on board!
                            SmokeToAdd.addElement(new SmokeDrift(new Coords(smokeCoords), smokeLevel));
                        }
                        else {
                            // report that the smoke has blown off the map
                            phaseReport.append("Smoke at ").append( currentCoords.getBoardNum() ).append(" has blown off the map!\n");
                        }
                        currentHex.removeTerrain(Terrains.SMOKE);
                        sendChangedHex(currentCoords);

                    }

                }  // end the loop through Y coordinates
            }  // end the loop through X coordinates

            debugTime("resolve smoke 1 end, resolve smoke 2 begin", true);

            // Cycle through the vector and add the drifted smoke
            for (int sta = 0; sta < SmokeToAdd.size(); sta++ ) {
                SmokeDrift drift = (SmokeDrift)SmokeToAdd.elementAt(sta);
                Coords smokeCoords = drift.coords;
                int smokeSize = drift.size;
                IHex smokeHex = game.getBoard().getHex(smokeCoords);
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, smokeSize));
                sendChangedHex(smokeCoords);
            }

            debugTime("resolve smoke 2 end, resolve smoke 3 begin", true);

            // Cycle through the vector again and dissipate the smoke, then reporting it
            for (int dis = 0; dis < SmokeToAdd.size(); dis++ ) {
                SmokeDrift drift = (SmokeDrift)SmokeToAdd.elementAt(dis);
                Coords smokeCoords = drift.coords;
                int smokeSize = drift.size;
                IHex smokeHex = game.getBoard().getHex(smokeCoords);
                int roll = Compute.d6(2);

                boolean smokeDis = driftSmokeDissipate(smokeHex, roll, smokeSize, windStr);
                driftSmokeReport(smokeCoords, smokeSize, smokeDis);
                sendChangedHex(smokeCoords);
            }

            debugTime("resolve smoke 3 end", false);

        } // end smoke resolution
    }

    public Coords driftAddSmoke(int x, int y, int windDir, int windStr){
        Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir);

        // if the wind is High, it blows 2 hexes! If it's Calm, there's no drift!
        if (windStr == 3) {
            nextCoords = nextCoords.translated(windDir);
        }

        return nextCoords;
    }

    /**
     * This method does not currently support "smoke clouds" as specified
     * in MaxTech (revised ed.) under "Dissipation" on page 51.  The
     * added complexity was not worth it given that smoke-delivering
     * weapons were not even implemented yet (and might never be).
     */
    public boolean driftSmokeDissipate(IHex smokeHex, int roll, int smokeSize, int windStr) {
        // Dissipate in various winds
        if (roll > 10 || (roll > 9 && windStr == 2) || (roll > 7 && windStr == 3)) {
            smokeHex.removeTerrain(Terrains.SMOKE);

            if (smokeSize == 2) {
                smokeHex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.SMOKE, 1));
                return true;
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }

    public void driftSmokeReport(Coords smokeCoords, int size, boolean dis) {
        if (size == 2 && dis == true) {
            phaseReport.append("Heavy smoke drifts to ").append( smokeCoords.getBoardNum() ).append(" and dissipates to light smoke!\n");
        }
        else if (size == 2 && dis == false) {
            phaseReport.append("Heavy smoke drifts to ").append( smokeCoords.getBoardNum() ).append("!\n");
        }
        else if (size == 1 && dis == true) {
            phaseReport.append("Light smoke drifts to ").append( smokeCoords.getBoardNum() ).append(" and dissipates completely!\n");
        }
        else if (size == 1 && dis == false) {
            phaseReport.append("Light smoke drifts to ").append( smokeCoords.getBoardNum() ).append("!\n");
        }
    }

    /**
     * Scans the boards directory for map boards of the appropriate size
     * and returns them.
     */
    private Vector scanForBoards(int boardWidth, int boardHeight) {
        Vector boards = new Vector();

        File boardDir = new File("data/boards");
        boards.addElement(MapSettings.BOARD_GENERATED);
        // just a check...
        if (!boardDir.isDirectory()) {
            return boards;
        }

        // scan files
        String[] fileList = boardDir.list();
        com.sun.java.util.collections.Vector tempList = new com.sun.java.util.collections.Vector();
        com.sun.java.util.collections.Comparator sortComp = StringUtil.stringComparator();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].indexOf(".board") == -1) {
                continue;
            }
            if (Board.boardIsSize(fileList[i], boardWidth, boardHeight)) {
                tempList.addElement(fileList[i].substring(0, fileList[i].lastIndexOf(".board")));
            }
        }

        // if there are any boards, add these:
        if (tempList.size() > 0) {
            boards.addElement( MapSettings.BOARD_RANDOM );
            boards.addElement( MapSettings.BOARD_SURPRISE );
            com.sun.java.util.collections.Collections.sort(tempList, sortComp);
            for ( int loop = 0; loop < tempList.size(); loop++ ) {
                boards.addElement( tempList.elementAt(loop) );
            }
        }

        //TODO: alphabetize files?

        return boards;
    }

    private boolean doBlind() {
        return (game.getOptions().booleanOption("double_blind") &&
        game.getPhase() >= Game.PHASE_DEPLOYMENT);
    }

    /**
     * In a double-blind game, update only visible entities.  Otherwise,
     * update everyone
     */
    private void entityUpdate(int nEntityID) {
        entityUpdate(nEntityID, new Vector());
    }

    private void entityUpdate(int nEntityID, Vector movePath) {
        if (doBlind()) {
            Entity eTarget = game.getEntity(nEntityID);
            Vector vPlayers = game.getPlayersVector();
            Vector vCanSee = whoCanSee(eTarget);
            // send an entity update to everyone who can see
            Packet pack = createEntityPacket(nEntityID, movePath);
            for (int x = 0; x < vCanSee.size(); x++) {
                Player p = (Player)vCanSee.elementAt(x);
                send(p.getId(), pack);
            }
            // send an entity delete to everyone else
            pack = createRemoveEntityPacket( nEntityID,
                                             eTarget.getRemovalCondition() );
            for (int x = 0; x < vPlayers.size(); x++) {
                if (!vCanSee.contains(vPlayers.elementAt(x))) {
                    Player p = (Player)vPlayers.elementAt(x);
                    send(p.getId(), pack);
                }
            }
        }
        else {
            // everyone can see
            send(createEntityPacket(nEntityID, movePath));
        }
    }

    /**
     * Returns a vector of which players can see this entity.
     */
    private Vector whoCanSee(Entity entity) {
        boolean bTeamVision = game.getOptions().booleanOption("team_vision");
        Vector vEntities = game.getEntitiesVector();

        Vector vCanSee = new Vector();
        vCanSee.addElement(entity.getOwner());
        if (bTeamVision) {
            addTeammates(vCanSee, entity.getOwner());
        }

        for (Enumeration p = game.getPlayers(); p.hasMoreElements();) {
            Player player = (Player)p.nextElement();

            if (player.canSeeAll() && !vCanSee.contains(p))
                vCanSee.addElement(player);
        }

        for (int i = 0; i < vEntities.size(); i++) {
            Entity e = (Entity)vEntities.elementAt(i);
            if (vCanSee.contains(e.getOwner()) || !e.isActive()) {
                continue;
            }
            if (Compute.canSee(game, e, entity)) {
                vCanSee.addElement(e.getOwner());
                if (bTeamVision) {
                    addTeammates(vCanSee, e.getOwner());
                }
            }
        }
        return vCanSee;
    }

    /**
     * Adds teammates of a player to the Vector.
     * Utility function for whoCanSee.
     */
    private void addTeammates(Vector vector, Player player) {
        Vector vPlayers = game.getPlayersVector();
        for (int j = 0; j < vPlayers.size(); j++) {
            Player p = (Player)vPlayers.elementAt(j);
            if (!player.isEnemyOf(p) && !vector.contains(p)) {
                vector.addElement(p);
            }
        }
    }

    /**
     * Send the complete list of entities to the players.
     * If double_blind is in effect, enforce it by filtering the entities
     */
    private void entityAllUpdate() {
        if (doBlind()) {
            Vector vPlayers = game.getPlayersVector();
            for (int x = 0; x < vPlayers.size(); x++) {
                Player p = (Player)vPlayers.elementAt(x);
                send(p.getId(), createFilteredEntitiesPacket(p));
            }
        }
        else {
            send(createEntitiesPacket());
        }
    }


    /**
     * Filters an entity vector according to LOS
     */
    private Vector filterEntities(Player pViewer, Vector vEntities) {
        Vector vCanSee = new Vector();
        Vector vAllEntities = game.getEntitiesVector();
        Vector vMyEntities = new Vector();
        boolean bTeamVision = game.getOptions().booleanOption("team_vision");

        // If they can see all, return the input list
        if (pViewer.canSeeAll()) {
            return vEntities;
        }

        for (int x = 0; x < vAllEntities.size(); x++) {
            Entity e = (Entity)vAllEntities.elementAt(x);
            if (e.getOwner() == pViewer || (bTeamVision && !e.getOwner().isEnemyOf(pViewer))) {
                vMyEntities.addElement(e);
            }
        }

        for (int x = 0; x < vEntities.size(); x++) {
            Entity e = (Entity)vEntities.elementAt(x);
            if (vMyEntities.contains(e)) {
                vCanSee.addElement(e);
                continue;
            }
            for (int y = 0; y < vMyEntities.size(); y++) {
                Entity e2 = (Entity)vMyEntities.elementAt(y);
                if (Compute.canSee(game, e2, e)) {
                    vCanSee.addElement(e);
                    break;
                }
            }
        }
        return vCanSee;
    }

    /**
     * Updates entities graphical "visibility indications" which are used
     *  in double-blind games.
     */
    private void updateVisibilityIndicator() {
        Vector vAllEntities = game.getEntitiesVector();
        for (int x = 0; x < vAllEntities.size(); x++) {
            Entity e = (Entity)vAllEntities.elementAt(x);
            boolean previousVisibleValue = e.isVisibleToEnemy();
            boolean previousSeenValue = e.isSeenByEnemy();
            e.setVisibleToEnemy(false);
            Vector vCanSee = whoCanSee(e);
            for (int y = 0; y < vCanSee.size(); y++) {
                Player p = (Player)vCanSee.elementAt(y);
                if (e.getOwner().isEnemyOf(p)) {
                    e.setVisibleToEnemy(true);
                    e.setSeenByEnemy(true);
                }
            }
            if (previousVisibleValue != e.isVisibleToEnemy()
                || previousSeenValue != e.isSeenByEnemy()) {
                sendVisibilityIndicator(e);
            }
        }
    }

    /**
     * Checks if an entity added by the client is valid and if so, adds it to the list
     */
    private void receiveEntityAdd(Packet c, int connIndex) {
        final Entity entity = (Entity)c.getObject(0);

        // If we're adding a Protomech, calculate it's unit number.
        if ( entity instanceof Protomech ) {

            // How many Protomechs does the player already have?
            int numPlayerProtos = game.getSelectedEntityCount
                ( new EntitySelector() {
                        private final int ownerId = entity.getOwnerId();
                        public boolean accept( Entity entity ) {
                            if ( entity instanceof Protomech &&
                                 ownerId == entity.getOwnerId() )
                                return true;
                            return false;
                        }
                    } );

            // According to page XXX of the BMRr, Protomechs must be
            // deployed in full Points of five, unless "losses" have
            // reduced the number to less that that.
            entity.setUnitNumber( (char) (numPlayerProtos / 5) );

        } // End added-Protomech

        // Only assign an entity ID when the client hasn't.
        if ( Entity.NONE == entity.getId() ) {
            entity.setId(getFreeEntityId());
        }
        game.addEntity(entity.getId(), entity);

        send(createAddEntityPacket(entity.getId()));
    }

    /**
     * Updates an entity with the info from the client.  Only valid to do this
     * durring the lounge phase.
     */
    private void receiveEntityUpdate(Packet c, int connIndex) {
        Entity entity = (Entity)c.getObject(0);
        Entity oldEntity = game.getEntity(entity.getId());
        if (oldEntity != null && oldEntity.getOwner() == getPlayer(connIndex)) {
            game.setEntity(entity.getId(), entity);
            send(createEntitiesPacket());
        } else {
            // hey!
        }
    }

    private void receiveEntityModeChange(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int equipId = c.getIntValue(1);
        int mode = c.getIntValue(2);
        Entity e = game.getEntity(entityId);
        if (e.getOwner() != getPlayer(connIndex)) {
            return;
        }
        Mounted m = e.getEquipment(equipId);

        // a mode change for ammo means dumping
        if (m.getType() instanceof AmmoType) {
            m.setPendingDump(mode == 1);
        }
        else {
            m.setMode(mode);
        }
    }

    private void receiveEntityAmmoChange(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int weaponId = c.getIntValue(1);
        int ammoId = c.getIntValue(2);
        Entity e = game.getEntity(entityId);

        // Did we receive a request for a valid Entity?
        if ( null == e ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: could not find entity #" );
            System.err.println( entityId );
            return;
        }
        Player player = getPlayer( connIndex );
        if ( null != player && e.getOwner() != player ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: player " );
            System.err.print( player.getName() );
            System.err.print( " does not own the entity " );
            System.err.println( e.getDisplayName() );
            return;
        }

        // Make sure that the entity has the given equipment.
        Mounted mWeap = e.getEquipment(weaponId);
        Mounted mAmmo = e.getEquipment(ammoId);
        if ( null == mAmmo ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " does not have ammo #" );
            System.err.println( ammoId );
            return;
        }
        if ( !(mAmmo.getType() instanceof AmmoType) ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( ammoId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mAmmo.getName() );
            System.err.println( " and not ammo." );
            return;
        }
        if ( null == mWeap ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " does not have weapon #" );
            System.err.println( weaponId );
            return;
        }
        if ( !(mWeap.getType() instanceof WeaponType) ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( weaponId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mWeap.getName() );
            System.err.println( " and not a weapon." );
            return;
        }
        if ( ((WeaponType) mWeap.getType()).getAmmoType() == AmmoType.T_NA ) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( weaponId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mWeap.getName() );
            System.err.println( " and does not use ammo." );
            return;
        }
        if ( ((WeaponType) mWeap.getType()).hasFlag(WeaponType.F_ONESHOT)) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: item # " );
            System.err.print( weaponId );
            System.err.print( " of entity " );
            System.err.print( e.getDisplayName() );
            System.err.print( " is a " );
            System.err.print( mWeap.getName() );
            System.err.println( " and cannot use external ammo." );
            return;
        }

        // Load the weapon.
        e.loadWeapon( mWeap, mAmmo );
    }

    /**
     * Deletes an entity owned by a certain player from the list
     */
    private void receiveEntityDelete(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        final Entity entity = game.getEntity(entityId);

        // Only allow players to delete their *own* entities.
        if ( entity != null && entity.getOwner() == getPlayer(connIndex) ) {

            // If we're deleting a Protomech, recalculate unit numbers.
            if ( entity instanceof Protomech ) {

                // How many Protomechs does the player have (include this one)?
                int numPlayerProtos = game.getSelectedEntityCount
                    ( new EntitySelector() {
                            private final int ownerId = entity.getOwnerId();
                            public boolean accept( Entity entity ) {
                                if ( entity instanceof Protomech &&
                                     ownerId == entity.getOwnerId() )
                                    return true;
                                return false;
                            }
                        } );

                // According to page 54 of the BMRr, Protomechs must be
                // deployed in full Points of five, unless "losses" have
                // reduced the number to less that that.
                final char oldMax =
                    (char)(Math.ceil( ((double)numPlayerProtos) / 5.0 )-1);
                char newMax =
                    (char)(Math.ceil( ((double) (numPlayerProtos-1))/ 5.0 )-1);
                char deletedUnitNum = entity.getUnitNumber();

                // Do we have to update a Protomech from the last unit?
                if ( oldMax != deletedUnitNum && oldMax != newMax ) {

                    // Yup.  Find a Protomech from the last unit, and
                    // set it's unit number to the deleted entity.
                    Enumeration lastUnit = game.getSelectedEntities
                        ( new EntitySelector() {
                            private final int ownerId = entity.getOwnerId();
                            private final char lastUnitNum = oldMax;
                            public boolean accept( Entity entity ) {
                                if ( entity instanceof Protomech &&
                                     ownerId == entity.getOwnerId() &&
                                     lastUnitNum == entity.getUnitNumber() )
                                    return true;
                                return false;
                            }
                        } );
                    Entity lastUnitMember = (Entity) lastUnit.nextElement();
                    lastUnitMember.setUnitNumber( deletedUnitNum );
                    this.entityUpdate( lastUnitMember.getId() );

                } // End update-unit-numbetr

            } // End added-Protomech

            game.removeEntity(entityId, Entity.REMOVE_NEVER_JOINED);
            send(createRemoveEntityPacket(entityId, Entity.REMOVE_NEVER_JOINED));
        }
    }

    /**
     * Sets a player's ready status
     */
    private void receivePlayerDone(Packet pkt, int connIndex) {
        boolean ready = pkt.getBooleanValue(0);
        Player player = getPlayer(connIndex);
        if ( null != player ) {
            player.setDone(ready);
        }
    }

    private void receiveInitiativeRerollRequest(Packet pkt, int connIndex) {
        Player player = getPlayer(connIndex);
        if ( Game.PHASE_INITIATIVE != game.getPhase() ) {
            StringBuffer message = new StringBuffer();
            if ( null == player ) {
                message.append( "Player #" )
                    .append( connIndex );
            } else {
                message.append( player.getName() );
            }
            message.append( " is not allowed to ask for a reroll at this time." );
            System.err.println( message.toString() );
            sendServerChat( message.toString() );
            return;
        }
        if (game.hasTacticalGenius(player)) {
            game.addInitiativeRerollRequest(game.getTeamForPlayer(player));
        }
        if ( null != player ) {
            player.setDone(true);
        }
        checkReady();
    }

    /**
     * Sets game options, providing that the player has specified the password
     * correctly.
     *
     * @return true if any options have been successfully changed.
     */
    private boolean receiveGameOptions(Packet packet, int connId) {
        Player player = game.getPlayer( connId );
        // Check player
        if ( null == player ) {
            System.err.print
                ( "Server does not recognize player at connection " );
            System.err.println( connId );
            return false;
        }

        // check password
        if (password != null && password.length() > 0 && !password.equals(packet.getObject(0))) {
            sendServerChat(connId, "The password you specified to change game options is incorrect.");
            return false;
        }

        int changed = 0;

        for (Enumeration i = ((Vector)packet.getObject(1)).elements(); i.hasMoreElements();) {
            IBasicOption option = (IBasicOption)i.nextElement();
            IOption originalOption = game.getOptions().getOption(option.getName());

            if (originalOption == null) {
                continue;
            }

            StringBuffer message = new StringBuffer();
            message.append( "Player " )
                .append( player.getName() )
                .append( " changed option \"" )
                .append( originalOption.getDisplayableName() )
                .append( "\" to " )
                .append( option.getValue().toString() )
                .append( "." );
            sendServerChat( message.toString() );
            originalOption.setValue(option.getValue());
            changed++;
        }

        // Set proper RNG
        Compute.setRNG(game.getOptions().intOption("rng_type"));

        return changed > 0;
    }

    /**
     * Sends out all player info to the specified connection
     */
    private void transmitAllPlayerConnects(int connId) {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            send(connId, createPlayerConnectPacket(player.getId()));
        }
    }



    /**
     * Creates a packet informing that the player has connected
     */
    private Packet createPlayerConnectPacket(int playerId) {
        final Object[] data = new Object[2];
        data[0] = new Integer(playerId);
        data[1] = getPlayer(playerId);
        return new Packet(Packet.COMMAND_PLAYER_ADD, data);
    }

    /**
     * Creates a packet containing the player info, for update
     */
    private Packet createPlayerUpdatePacket(int playerId) {
        final Object[] data = new Object[2];
        data[0] = new Integer(playerId);
        data[1] = getPlayer(playerId);
        return new Packet(Packet.COMMAND_PLAYER_UPDATE, data);
    }

    /**
     * Sends out the player info updates for all players to all connections
     */
    private void transmitAllPlayerUpdates() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if ( null != player ) {
                send(createPlayerUpdatePacket(player.getId()));
            }
        }
    }

    /**
     * Sends out the player ready stats for all players to all connections
     */
    private void transmitAllPlayerDones() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            send(createPlayerDonePacket(player.getId()));
        }
    }

    /**
     * Creates a packet containing the player ready status
     */
    private Packet createPlayerDonePacket(int playerId) {
        Object[] data = new Object[2];
        data[0] = new Integer(playerId);
        data[1] = new Boolean(getPlayer(playerId).isDone());
        return new Packet(Packet.COMMAND_PLAYER_READY, data);
    }

    /** Creates a packet containing the current turn vector */
    private Packet createTurnVectorPacket() {
        return new Packet(Packet.COMMAND_SENDING_TURNS, game.getTurnVector());
    }

    /** Creates a packet containing the current turn index */
    private Packet createTurnIndexPacket() {
        return new Packet(Packet.COMMAND_TURN, new Integer(game.getTurnIndex()));
    }

    /**
     * Creates a packet containing the map settings
     */
    private Packet createMapSettingsPacket() {
        return new Packet(Packet.COMMAND_SENDING_MAP_SETTINGS, mapSettings);
    }

    /**
     * Creates a packet containing temporary map settings as a response to a
     * client query
     */
    private Packet createMapQueryPacket(MapSettings temp) {
        return new Packet(Packet.COMMAND_QUERY_MAP_SETTINGS, temp);
    }

    /**
     * Creates a packet containing the game settingss
     */
    private Packet createGameSettingsPacket() {
        return new Packet(Packet.COMMAND_SENDING_GAME_SETTINGS, game.getOptions());
    }

    /**
     * Creates a packet containing the game board
     */
    private Packet createBoardPacket() {
        return new Packet(Packet.COMMAND_SENDING_BOARD, game.board);
    }

    /**
     * Creates a packet containing a single entity, for update
     */
    private Packet createEntityPacket(int entityId) {
        return createEntityPacket(entityId, new Vector());
    }
    private Packet createEntityPacket(int entityId, Vector movePath) {
        final Entity entity = game.getEntity(entityId);
        final Object[] data = new Object[3];
        data[0] = new Integer(entityId);
        data[1] = entity;
        data[2] = movePath;
        return new Packet(Packet.COMMAND_ENTITY_UPDATE, data);
    }


    /**
     * Creates a packet containing the round report
     */
    private Packet createReportPacket() {
        return new Packet(Packet.COMMAND_SENDING_REPORT, roundReport.toString());
    }

    /**
     * Creates a packet containing all current entities
     */
    private Packet createEntitiesPacket() {
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, game.getEntitiesVector());
    }

    /**
     * Creates a packet containing all current and out-of-game entities
     */
    private Packet createFullEntitiesPacket() {
        final Object[] data = new Object[2];
        data[0] = game.getEntitiesVector();
        data[1] = game.getOutOfGameEntitiesVector();
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, data);
    }

    /**
     * Creates a packet containing all entities visible to the player in a blind game
     */
    private Packet createFilteredEntitiesPacket(Player p) {
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, filterEntities(p, game.getEntitiesVector()));
    }

    /**
     * Creates a packet containing all entities, including wrecks, visible to the player in a blind game
     */
    private Packet createFilteredFullEntitiesPacket(Player p) {
        final Object[] data = new Object[2];
        data[0] = filterEntities(p, game.getEntitiesVector());
        data[1] = game.getOutOfGameEntitiesVector();
        return new Packet(Packet.COMMAND_SENDING_ENTITIES, data);
    }

    /**
     * Creates a packet detailing the addition of an entity
     */
    private Packet createAddEntityPacket(int entityId) {
        final Entity entity = game.getEntity(entityId);
        final Object[] data = new Object[2];
        data[0] = new Integer(entityId);
        data[1] = entity;
        return new Packet(Packet.COMMAND_ENTITY_ADD, data);
    }

    /**
     * Creates a packet detailing the removal of an entity.
     * Maintained for backwards compatability.
     *
     * @param   entityId - the <code>int</code> ID of the entity being removed.
     * @return  A <code>Packet</code> to be sent to clients.
     */
    private Packet createRemoveEntityPacket(int entityId) {
        return this.createRemoveEntityPacket(entityId, Entity.REMOVE_SALVAGEABLE);
    }

    /**
     * Creates a packet detailing the removal of an entity.
     *
     * @param   entityId - the <code>int</code> ID of the entity being removed.
     * @param   condition - the <code>int</code> condition the unit was in.
     *          This value must be one of <code>Game.UNIT_IN_RETREAT</code>,
     *          <code>Game.UNIT_PUSHED</code>, or
     *          <code>Game.UNIT_SALVAGEABLE</code>, or
     *          <code>Game.UNIT_EJECTED</code>, or
     *          <code>Game.UNIT_DEVASTATED</code> or an
     *          <code>IllegalArgumentException</code> will be thrown.
     * @return  A <code>Packet</code> to be sent to clients.
     */
    private Packet createRemoveEntityPacket(int entityId, int condition) {
        if ( condition != Entity.REMOVE_UNKNOWN &&
             condition != Entity.REMOVE_IN_RETREAT &&
             condition != Entity.REMOVE_PUSHED &&
             condition != Entity.REMOVE_SALVAGEABLE &&
             condition != Entity.REMOVE_EJECTED &&
             condition != Entity.REMOVE_CAPTURED &&
             condition != Entity.REMOVE_DEVASTATED &&
             condition != Entity.REMOVE_NEVER_JOINED ) {
            throw new IllegalArgumentException( "Unknown unit condition: " +
                                                condition );
        }
        Object[] array = new Object[2];
        array[0] = new Integer(entityId);
        array[1] = new Integer(condition);
        return new Packet(Packet.COMMAND_ENTITY_REMOVE, array);
    }

    /**
     * Creates a packet indicating end of game, including detailed unit status
     */
    private Packet createEndOfGamePacket() {
        Object[] array = new Object[3];
        array[0] = getDetailedVictoryReport();
        array[1] = new Integer(game.getVictoryPlayerId());
        array[2] = new Integer(game.getVictoryTeam());
        return new Packet(Packet.COMMAND_END_OF_GAME, array);
    }

    /**
     * Transmits a chat message to all players
     */
    private void sendChat(int connId, String origin, String message) {
        send(connId, new Packet(Packet.COMMAND_CHAT, origin + ": " + message));
    }

    /**
     * Transmits a chat message to all players
     */
    private void sendChat(String origin, String message) {
        String chat = origin + ": " + message;
        send(new Packet(Packet.COMMAND_CHAT, chat));
        log.append(chat);
    }

    public void sendServerChat(int connId, String message) {
        sendChat(connId, "***Server", message);
    }

    public void sendServerChat(String message) {
        sendChat("***Server", message);
    }

    /**
     * Creates a packet containing a hex, and the coordinates it goes at.
     */
    private Packet createHexChangePacket(Coords coords, IHex hex) {
        final Object[] data = new Object[2];
        data[0] = coords;
        data[1] = hex;
        return new Packet(Packet.COMMAND_CHANGE_HEX, data);
    }

    /**
     * Sends notification to clients that the specified hex has changed.
     */
    public void sendChangedHex(Coords coords) {
        send(createHexChangePacket(coords, game.board.getHex(coords)));
    }

    public void sendVisibilityIndicator(Entity e) {
        final Object[] data = new Object[3];
        data[0] = new Integer(e.getId());
        data[1] = new Boolean(e.isSeenByEnemy());
        data[2] = new Boolean(e.isVisibleToEnemy());
        send(new Packet(Packet.COMMAND_ENTITY_VISIBILITY_INDICATOR, data));
    }

    /**
     * Creates a packet for an attack
     */
    private Packet createAttackPacket(Vector vector, boolean charges) {
        final Object[] data = new Object[2];
        data[0] = vector;
        data[1] = new Boolean(charges);
        return new Packet(Packet.COMMAND_ENTITY_ATTACK, data);
    }

    /**
     * Creates a packet for an attack
     */
    private Packet createAttackPacket(EntityAction ea, boolean charge) {
        Vector vector = new Vector(1);
        vector.addElement(ea);
        Object[] data = new Object[2];
        data[0] = vector;
        data[1] = new Boolean(charge);
        return new Packet(Packet.COMMAND_ENTITY_ATTACK, data);
    }

    /**
     * Send a packet to all connected clients.
     */
    private void send(Packet packet) {
        if (connections == null) {
            return;
        }
        packet.zipData();
        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            conn.send(packet);
        }
    }

    /**
     * Send a packet to a specific connection.
     */
    private void send(int connId, Packet packet) {
        packet.zipData();
        if (getClient(connId) != null) {
            getClient(connId).send(packet);
        } else {
            //What should we do if we've lost this client?
            // For now, nothing.
        }
    }

    /**
     * Send a packet to a pending connection
     */
    private void sendToPending(int connId, Packet packet) {
        if (getPendingConnection(connId) != null) {
            getPendingConnection(connId).send(packet);
        } else {
            //What should we do if we've lost this client?
            // For now, nothing.
        }
    }

    /**
     * Process an in-game command
     */
    private void processCommand(int connId, String commandString) {
        String[] args;
        String commandName;
        // all tokens are read as strings; if they're numbers, string-ize 'em.
        StringTokenizer st = new StringTokenizer(commandString);
        args = new String[st.countTokens()];
        for (int i = 0; i < args.length; i++) {
            args[i] = st.nextToken();
        }

        // figure out which command this is
        commandName = args[0].substring(1);

        // process it
        ServerCommand command = getCommand(commandName);
        if (command != null) {
            command.run(connId, args);
        } else {
            sendServerChat(connId, "Command not recognized.  Type /help for a list of commands.");
        }
    }

    // Easter eggs.  Happy April Fool's Day!!
    private static final String DUNE_CALL = "They tried and failed?";
    private static final String DUNE_RESPONSE = "They tried and died!";
    private static final String STAR_WARS_CALL = "I'd just as soon kiss a Wookiee.";
    private static final String STAR_WARS_RESPONSE = "I can arrange that!";

    /**
     * Process a packet from a connection.
     *
     * @param   id - the <code>int</code> ID the connection that
     *          received the packet.
     * @param   packet - the <code>Packet</code> to be processed.
     */
    public synchronized void handle(int connId, Packet packet) {
        Player player = game.getPlayer( connId );
        // Check player.  Please note, the connection may be pending.
        if ( null == player && null == getPendingConnection(connId) ) {
            System.err.print
                ( "Server does not recognize player at connection " );
            System.err.println( connId );
            return;
        }

        //System.out.println("s(" + cn + "): received command");
        if (packet == null) {
            System.out.println("server.connection.handle: got null packet");
            return;
        }
        // act on it
        switch(packet.getCommand()) {
            case Packet.COMMAND_CLOSE_CONNECTION :
                // We have a client going down!
                this.disconnected( this.getConnection(connId) );
                break;
            case Packet.COMMAND_CLIENT_NAME :
                receivePlayerName(packet, connId);
                break;
            case Packet.COMMAND_PLAYER_UPDATE :
                receivePlayerInfo(packet, connId);
                validatePlayerInfo(connId);
                send(createPlayerUpdatePacket(connId));
                break;
            case Packet.COMMAND_PLAYER_READY :
                receivePlayerDone(packet, connId);
                send(createPlayerDonePacket(connId));
                checkReady();
                break;
            case Packet.COMMAND_REROLL_INITIATIVE :
                receiveInitiativeRerollRequest(packet, connId);
                send(createPlayerDonePacket(connId));
                break;
            case Packet.COMMAND_CHAT :
                String chat = (String)packet.getObject(0);
                if (chat.startsWith("/")) {
                    processCommand(connId, chat);
                } else {
                    sendChat(player.getName(), chat);
                }
                // Easter eggs.  Happy April Fool's Day!!
                if ( DUNE_CALL.equals(chat) ) {
                    sendServerChat( DUNE_RESPONSE );
                }
                else if ( STAR_WARS_CALL.equals(chat) ) {
                    sendServerChat( STAR_WARS_RESPONSE );
                }
                break;
            case Packet.COMMAND_ENTITY_MOVE :
                receiveMovement(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_DEPLOY :
                receiveDeployment(packet, connId);
                break;
            case Packet.COMMAND_DEPLOY_MINEFIELDS :
                receiveDeployMinefields(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_ATTACK :
                receiveAttack(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_ADD :
                receiveEntityAdd(packet, connId);
                resetPlayersDone();
                transmitAllPlayerDones();
                break;
            case Packet.COMMAND_ENTITY_UPDATE :
                receiveEntityUpdate(packet, connId);
                resetPlayersDone();
                transmitAllPlayerDones();
                break;
            case Packet.COMMAND_ENTITY_MODECHANGE :
                receiveEntityModeChange(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_AMMOCHANGE :
                receiveEntityAmmoChange(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_REMOVE :
                receiveEntityDelete(packet, connId);
                resetPlayersDone();
                transmitAllPlayerDones();
                break;
            case Packet.COMMAND_SENDING_GAME_SETTINGS :
                if (receiveGameOptions(packet, connId)) {
                    resetPlayersDone();
                    transmitAllPlayerDones();
                    send(createGameSettingsPacket());
                }
                break;
            case Packet.COMMAND_SENDING_MAP_SETTINGS :
                MapSettings newSettings=(MapSettings)packet.getObject(0);
                if (!mapSettings.equalMapGenParameters(newSettings)) {
                    sendServerChat("Player " + player.getName() +
                                   " changed mapsettings");
                }
                mapSettings = newSettings;
                newSettings = null;
                mapSettings.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
                resetPlayersDone();
                transmitAllPlayerDones();
                send(createMapSettingsPacket());
                break;
            case Packet.COMMAND_QUERY_MAP_SETTINGS :
                MapSettings temp = (MapSettings)packet.getObject(0);
                temp.setBoardsAvailableVector(scanForBoards(temp.getBoardWidth(), temp.getBoardHeight()));
                temp.removeUnavailable();
                temp.setNullBoards(DEFAULT_BOARD);
                temp.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
                temp.removeUnavailable();
                send(connId, createMapQueryPacket(temp));
                break;
            case Packet.COMMAND_UNLOAD_STRANDED :
                receiveUnloadStranded(packet, connId);
                break;
            case Packet.COMMAND_SET_ARTYAUTOHITHEXES :
                receiveArtyAutoHitHexes(packet, connId);
        }
    }


    /**
     * Listen for incoming clients.
     */
    public void run() {
        Thread currentThread = Thread.currentThread();
        System.out.println("s: listening for clients...");
        while (connector == currentThread) {
            try {
                Socket s = serverSocket.accept();

                int id = getFreeConnectionId();
                System.out.println("s: accepting player connection #" + id + " ...");

                connectionsPending.addElement(new Connection(this, s, id));

                greeting(id);
            } catch(IOException ex) {
                ;
            }
        }
    }

    /**
     * Makes one slot of inferno ammo, determined
     * by certain rules, explode on a mech.
     *
     * @param entity  The <code>Entity</code> that should suffer an
     *                inferno ammo explosion.
     */
    private String explodeInfernoAmmoFromHeat(Entity entity) {
        int damage = 0;
        int rack = 0;
        int boomloc = -1;
        int boomslot = -1;
        StringBuffer result = new StringBuffer();

        // Find the most destructive Inferno ammo.
        for (int j = 0; j < entity.locations(); j++) {
            for (int k = 0; k < entity.getNumberOfCriticals(j); k++) {
                CriticalSlot cs = entity.getCritical(j, k);
                // Ignore empty, destroyed, hit, and structure slots.
                if ( cs == null || cs.isDestroyed() || cs.isHit() ||
                     cs.getType() != CriticalSlot.TYPE_EQUIPMENT ) {
                    continue;
                }
                // Ignore everything but weapons slots.
                Mounted mounted = entity.getEquipment
                    ( entity.getCritical(j, k).getIndex() );
                if (!(mounted.getType() instanceof AmmoType)) {
                    continue;
                }
                // Ignore everything but Inferno ammo.
                AmmoType atype = (AmmoType)mounted.getType();
                if ( !atype.isExplosive() ||
                     atype.getMunitionType() != AmmoType.M_INFERNO) {
                    continue;
                }
                // Find the most destructive undamaged ammo.
                // BMRr, pg. 48, compare one rack's
                // damage.  Ties go to most rounds.
                int newRack = atype.getDamagePerShot() * atype.getRackSize();
                int newDamage = mounted.getExplosionDamage();
                if ( !mounted.isHit() && ( rack < newRack ||
                       (rack == newRack && damage < newDamage) ) ) {
                    rack = newRack;
                    damage = newDamage;
                    boomloc = j;
                    boomslot = k;
                }
            }
        }
        // Did we find anything to explode?
        if (boomloc != -1 && boomslot != -1) {
            CriticalSlot slot = entity.getCritical(boomloc, boomslot);
            slot.setHit(true);
            entity.getEquipment(slot.getIndex()).setHit(true);
            // We've allocated heatBuildup to heat in resolveHeat(),
            // so need to add to the entity's heat instead.
            result.append( explodeEquipment(entity, boomloc, boomslot) );
            entity.heat += 30;
            result.append( "   Gains 30 heat and is now at " )
                .append( entity.heat )
                .append( " heat.\n" );
            entity.heatBuildup = 0;
        } else {
            result.append("  Luckily, there is no inferno ammo to explode.\n");
        }
        return result.toString();
    }

    /**
     * Determine the results of an entity moving through a wall of a building
     * after having moved a certain distance.  This gets called when a Mech
     * or a Tank enters a building, leaves a building, or travels from one
     * hex to another inside a multi-hex building.
     *
     * @param   entity - the <code>Entity</code> that passed through a wall.
     *          Don't pass <code>Infantry</code> units to this method.
     * @param   bldg - the <code>Building</code> the entity is passing through.
     * @param   lastPos - the <code>Coords</code> of the hex the entity is
     *          exiting.
     * @param   curPos - the <code>Coords</code> of the hex the entity is
     *          entering
     * @param   distance - the <code>int</code> number of hexes the entity
     *          has moved already this phase.
     * @param   why - the <code>String</code> explanatin for this action.
     * @return  <code>true</code> if the building collapses due to overloading.
     */
    private boolean passBuildingWall( Entity entity,
                                      Building bldg,
                                      Coords lastPos,
                                      Coords curPos,
                                      int distance,
                                      String why ) {

        // Need to roll based on building type.
        PilotingRollData psr = entity.rollMovementInBuilding(bldg, distance, why);

        // Did the entity make the roll?
        if ( !doSkillCheckWhileMoving( entity, lastPos,
                                       curPos, psr, false ) ) {

            // Divide the building's current CF by 10, round up.
            int damage = (int) Math.ceil( bldg.getCurrentCF() / 10.0 );

            // It is possible that the unit takes no damage.
            if ( damage == 0 ) {
                phaseReport.append( "        " )
                    .append( entity.getDisplayName() )
                    .append( " takes no damage.\n" );
            } else {
                // BMRr, pg. 50: The attack direction for this damage is the front.
                HitData hit = entity.rollHitLocation( ToHitData.HIT_NORMAL,
                                                      ToHitData.SIDE_FRONT );
                phaseReport.append( damageEntity(entity, hit, damage) )
                    .append( "\n" );
            }
        }

        // Damage the building.  The CF can never drop below 0.
        int toBldg = (int) Math.ceil( entity.getWeight() / 10.0 );
        int curCF = bldg.getCurrentCF();
        curCF -= Math.min( curCF, toBldg );
        bldg.setCurrentCF( curCF );

        // Apply the correct amount of damage to infantry in the building.
        // ASSUMPTION: We inflict toBldg damage to infantry and
        //             not the amount to bring building to 0 CF.
        this.damageInfantryIn( bldg, toBldg );

        // Get the position map of all entities in the game.
        Hashtable positionMap = game.getPositionMap();

        // Count the moving entity in its current position, not
        // its pre-move postition.  Be sure to handle nulls.
        Vector entities = null;
        if ( entity.getPosition() != null ) {
            entities = (Vector) positionMap.get( entity.getPosition() );
            entities.removeElement( entity );
        }
        entities = (Vector) positionMap.get( curPos );
        if ( entities == null ) {
            entities = new Vector();
            positionMap.put( curPos, entities );
        }
        entities.addElement( entity );

        // Check for collapse of this building due to overloading, and return.
        return this.checkForCollapse( bldg, positionMap );
    }

    /**
     * Apply the correct amount of damage that passes on to any infantry unit
     * in the given building, based upon the amount of damage the building
     * just sustained.  This amount is a percentage dictated by pg. 52 of BMRr.
     *
     * @param   bldg - the <code>Building</code> that sustained the damage.
     * @param   damage - the <code>int</code> amount of damage.
     */
    private void damageInfantryIn( Building bldg, int damage ) {

        // Calculate the amount of damage the infantry will sustain.
        float percent = 0.0f;
        switch( bldg.getType() ) {
        case Building.LIGHT: percent = 0.75f; break;
        case Building.MEDIUM: percent = 0.5f; break;
        case Building.HEAVY: percent = 0.25f; break;
        }

        // Round up at .5 points of damage.
        int toInf = Math.round( damage * percent );

        // Exit if the infantry receive no points of damage.
        if ( toInf == 0 ) {
            phaseReport.append( "    Infantry receive no damage.\n" );
            return;
        }

        // Record if we find any infantry.
        boolean foundInfantry = false;

        // Walk through the entities in the game.
        Enumeration entities = game.getEntities();
        while ( entities.hasMoreElements() ) {
            Entity entity = (Entity) entities.nextElement();
            final Coords coords = entity.getPosition();

            // If the entity is infantry in one of the building's hexes?
            if ( entity instanceof Infantry &&
                 bldg.isIn( coords ) ) {

                // Is the entity is inside of the building
                // (instead of just on top of it)?
                if ( Compute.isInBuilding( game, entity, coords ) ) {

                    // Yup.  Damage the entity.
                    // Battle Armor units use 5 point clusters.
                    phaseReport.append( "      " )
                        .append( entity.getDisplayName() )
                        .append( " takes " )
                        .append( toInf )
                        .append( " damage." );
                    int remaining = toInf;
                    int cluster = toInf;
                    if ( entity instanceof BattleArmor ) {
                        cluster = 5;
                    }
                    while ( remaining > 0 ) {
                        int next = Math.min( cluster, remaining );
                        HitData hit = entity.rollHitLocation
                            ( ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT );
                        phaseReport.append( damageEntity(entity, hit, next) );
                        remaining -= next;
                    }
                    phaseReport.append( "\n" );

                } // End infantry-inside-building

            } // End entity-is-infantry-in-building-hex

        } // Handle the next entity

        // If we found any infantry, add a line to the phase report.
        if ( foundInfantry ) {
            phaseReport.append( "\n" );
        }

    } // End private void damageInfantryIn( Building, int )

    /**
     * Determine if the given building should collapse.  If so,
     * inflict the appropriate amount of damage on each entity in
     * the building and update the clients.  If the building does
     * not collapse, determine if any entities crash through its
     * floor into its basement.  Again, apply appropriate damage.
     *
     * @param   bldg - the <code>Building</code> being checked.
     *          This value should not be <code>null</code>.
     * @param   positionMap - a <code>Hashtable</code> that maps
     *          the <code>Coords</code> positions or each unit
     *          in the game to a <code>Vector</code> of
     *          <code>Entity</code>s at that position.
     *          This value should not be <code>null</code>.
     * @return  <code>true</code> if the building collapsed.
     */
    private boolean checkForCollapse( Building bldg, Hashtable positionMap ) {

        // If the input is meaningless, do nothing and throw no exception.
        if ( bldg == null ||
             positionMap == null || positionMap.isEmpty() ) {
            return false;
        }

        // Get the building's current CF.
        final int currentCF = bldg.getCurrentCF();

        // Track all units that fall into the building's basement by Coords.
        Hashtable basementMap = new Hashtable();

        // Walk through the hexes in the building, looking for a collapse.
        Enumeration bldgCoords = bldg.getCoords();
        boolean collapse = false;
        while ( !collapse && bldgCoords.hasMoreElements() ) {
            final Coords coords = (Coords) bldgCoords.nextElement();

            // Get the Vector of Entities at these coordinates.
            final Vector vector = (Vector) positionMap.get( coords );

            // Are there any Entities at these coords?
            if ( vector != null ) {

                // How many levels does this building have in this hex?
                final IHex curHex = game.board.getHex( coords );
                final int hexElev = curHex.surface();
                final int numFloors = curHex.terrainLevel( Terrains.BLDG_ELEV );

                // Track the load of each floor (and of the roof) separately.
                // Track all units that fall into the basement in this hex.
                // N.B. don't track the ground floor, the first floor is at
                // index 0, the second is at index 1, etc., and the roof is
                // at index (numFloors-1).
                int[] loads = new int[numFloors];
                Vector basement = new Vector();
                for ( int loop = 0; loop < numFloors; loop++ ) {
                    loads[loop] = 0;
                }

                // Walk through the entities in this position.
                Enumeration entities = vector.elements();
                while ( !collapse && entities.hasMoreElements() ) {
                    final Entity entity = (Entity) entities.nextElement();
                    final int entityElev = entity.elevationOccupied( curHex );

                    // Ignore entities not *inside* the building
                    if ( !Compute.isInBuilding( game, entity, coords ) ) {
                        continue;
                    }

                    // Add the weight of a Mek or tank to the correct floor.
                    if ( entity instanceof Mech || entity instanceof Tank ) {
                        int load = (int) entity.getWeight();
                        int floor = entityElev - hexElev;

                        // Entities on the ground floor may fall into the
                        // basement, but they won't collapse the building.
                        if ( floor == 0 && load > currentCF ) {
                            basement.addElement( entity );
                        } else if ( floor > 0 ) {

                            // If the load on any floor but the ground floor
                            // exceeds the building's current CF it collapses.
                            floor--;
                            loads[ floor ] += load;
                            if ( loads[ floor ] > currentCF ) {
                                collapse = true;
                            }

                        } // End not-ground-floor

                    } // End increase-load

                } // Handle the next entity.

                // Track all entities that fell into the basement.
                if ( !basement.isEmpty() ) {
                    basementMap.put( coords, basement );
                }

            } // End have-entities-here

        } // Check the next hex of the building.

        // Collapse the building if the flag is set.
        if ( collapse ) {
            phaseReport.append( bldg.getName() )
                .append( " collapses due to heavy loads!\n" );
            this.collapseBuilding( bldg, positionMap );
        }

        // Otherwise, did any entities fall into the basement?
        else if ( !basementMap.isEmpty() ) {
            // TODO: implement basements
        }

        // Return true if the building collapsed.
        return collapse;

    } // End private boolean checkForCollapse( Building, Hashtable )

    /**
     * Collapse the building.  Inflict the appropriate amount of damage
     * on all entities in the building.  Update all clients.
     *
     * @param   bldg - the <code>Building</code> that has collapsed.
     * @param   positionMap - a <code>Hashtable</code> that maps
     *          the <code>Coords</code> positions or each unit
     *          in the game to a <code>Vector</code> of
     *          <code>Entity</code>s at that position.
     *          This value should not be <code>null</code>.
     */
    private void collapseBuilding( Building bldg, Hashtable positionMap ) {

        // Loop through the hexes in the building, and apply
        // damage to all entities inside or on top of the building.
        final int phaseCF = bldg.getPhaseCF();
        Enumeration bldgCoords = bldg.getCoords();
        while ( bldgCoords.hasMoreElements() ) {
            final Coords coords = (Coords) bldgCoords.nextElement();

            // Get the Vector of Entities at these coordinates.
            final Vector vector = (Vector) positionMap.get( coords );

            // Are there any Entities at these coords?
            if ( vector != null ) {

                // How many levels does this building have in this hex?
                final IHex curHex = game.board.getHex( coords );
                final int hexElev = curHex.surface();
                final int numFloors = curHex.terrainLevel( Terrains.BLDG_ELEV );

                // Walk through the entities in this position.
                Enumeration entities = vector.elements();
                while ( entities.hasMoreElements() ) {
                    final Entity entity = (Entity) entities.nextElement();
                    final int entityElev = entity.elevationOccupied( curHex );
                    int floor = entityElev - hexElev;

                    // Ignore units not *inside* the building.
                    if ( !Compute.isInBuilding( game, entity, coords ) ) {
                        continue;
                    }

                    // Treat units on the roof like
                    // they were in the top floor.
                    if ( floor == numFloors ) {
                        floor--;
                    }

                    // Calculate collapse damage for this entity.
                    int damage = (int) Math.ceil
                        ( phaseCF * (numFloors-floor) / 10.0 );

                    // Infantry suffer triple damage.
                    if ( entity instanceof Infantry ) {
                        damage *= 3;
                    }

                    // Apply collapse damage the entity.
                    // ASSUMPTION: use 5 point clusters.
                    phaseReport.append( "   " )
                        .append( entity.getDisplayName() )
                        .append( " is hit by falling debris for " )
                        .append( damage )
                        .append( " damage." );
                    int remaining = damage;
                    int cluster = damage;
                    if ( entity instanceof BattleArmor ||
                         entity instanceof Mech ||
                         entity instanceof Tank ) {
                        cluster = 5;
                    }
                    while ( remaining > 0 ) {
                        int next = Math.min( cluster, remaining );
                        // In www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf,
                        // pg. 18, Randall Bills says that all damage from a
                        // collapsing building is applied to the front.

                        HitData hit = entity.rollHitLocation
                            (ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT );
                        phaseReport.append
                            ( damageEntity(entity, hit, next) );
                        remaining -= next;
                    }
                    phaseReport.append( "\n" );
                    // TODO: Why are dead entities showing up on firing phase?

                    // Do we need to handle falling Meks?
                    // BMRr, pg. 53 only mentions falling BattleMechs;
                    // Tanks can't be above the floor and I guess that
                    // infantry don't suffer falling damage.
                    // TODO: implement basements, then fall into it.
                    // ASSUMPTION: we'll let the Mech fall twice: once
                    // during damageEntity() above and once here.
                    floor = entityElev - hexElev;
                    if ( floor > 0 && entity instanceof Mech ) {
                        // ASSUMPTION: PSR to avoid pilot damage
                        // should use mods for entity damage and
                        // 20+ points of collapse damage (if any).
                        PilotingRollData psr = entity.getBasePilotingRoll();
                        if ( damage >= 20 ) {
                            psr.addModifier( 1, "20+ damage" );
                        }
                        this.doEntityFall( entity, coords, floor, psr );
                    }

                    // Update this entity.
                    // ASSUMPTION: this is the correct thing to do.
                    this.entityUpdate( entity.getId() );

                } // Handle the next entity.

            } // End have-entities-here.

        } // Handle the next hex of the building.

        // Update the building.
        bldg.setCurrentCF( 0 );
        bldg.setPhaseCF( 0 );
        send( createCollapseBuildingPacket(bldg) );
        game.board.collapseBuilding( bldg );

    } // End private void collapseBuilding( Building )

    /**
     * Tell the clients to replace the given building with rubble hexes.
     *
     * @param   bldg - the <code>Building</code> that has collapsed.
     * @return  a <code>Packet</code> for the command.
     */
    private Packet createCollapseBuildingPacket( Building bldg ) {
        Vector buildings = new Vector();
        buildings.addElement( bldg );
        return this.createCollapseBuildingPacket( buildings );
    }

    /**
     * Tell the clients to replace the given buildings with rubble hexes.
     *
     * @param   buildings - a <code>Vector</code> of <code>Building</code>s
     *          that has collapsed.
     * @return  a <code>Packet</code> for the command.
     */
    private Packet createCollapseBuildingPacket( Vector buildings ) {
        return new Packet( Packet.COMMAND_BLDG_COLLAPSE, buildings );
    }

    /**
     * Tell the clients to update the CFs of the given buildings.
     *
     * @param   buildings - a <code>Vector</code> of <code>Building</code>s
     *          that need to be updated.
     * @return  a <code>Packet</code> for the command.
     */
    private Packet createUpdateBuildingCFPacket( Vector buildings ) {
        return new Packet( Packet.COMMAND_BLDG_UPDATE_CF, buildings );
    }

    /**
     * Apply this phase's damage to all buildings.  Buildings may
     * collapse due to damage.
     */
    private void applyBuildingDamage() {

        // Walk through the buildings in the game.
        // Build the collapse and update vectors as you go.
        // N.B. never, NEVER, collapse buildings while you are walking through
        //      the Enumeration from megamek.common.Board#getBuildings.
        Vector collapse = new Vector();
        Vector update = new Vector();
        Enumeration buildings = game.board.getBuildings();
        while ( buildings.hasMoreElements() ) {
            Building bldg = (Building) buildings.nextElement();

            // If the CF is zero, the building should fall.
            if ( bldg.getCurrentCF() == 0 ) {
                collapse.addElement( bldg );
            }

            // If the building took damage this round, update it.
            else if ( bldg.getPhaseCF() != bldg.getCurrentCF() ) {
                bldg.setPhaseCF( bldg.getCurrentCF() );
                update.addElement( bldg );
            }

        } // Handle the next building

        // If we have any buildings to collapse, collapse them now.
        if ( !collapse.isEmpty() ) {

            // Get the position map of all entities in the game.
            Hashtable positionMap = game.getPositionMap();

            // Walk through the buildings that have collapsed.
            buildings = collapse.elements();
            while ( buildings.hasMoreElements() ) {
                Building bldg = (Building) buildings.nextElement();
                phaseReport.append( "\n" )
                    .append( bldg.getName() )
                    .append( " collapses due to damage!\n" );
                this.collapseBuilding( bldg, positionMap );
            }

        }

        // If we have any buildings to update, send the message.
        if ( !update.isEmpty() ) {
            send( createUpdateBuildingCFPacket( update ) );
        }
    }

    /**
     * Apply the given amount of damage to the building.  Please note,
     * this method does <b>not</b> apply any damage to units inside the
     * building, update the clients, or check for the building's collapse.
     * <p/>
     * A default message will be used to describe why the building
     * took the damage.
     *
     * @param   bldg - the <code>Building</code> that has been damaged.
     *          This value should not be <code>null</code>, but no
     *          exception will occur.
     * @param   damage - the <code>int</code> amount of damage.
     * @return  a <code>String</code> message to be shown to the players.
     */
    private String damageBuilding( Building bldg, int damage ) {
        final String defaultWhy = " absorbs ";
        return damageBuilding( bldg, damage, defaultWhy );
    }

    /**
     * Apply the given amount of damage to the building.  Please note,
     * this method does <b>not</b> apply any damage to units inside the
     * building, update the clients, or check for the building's collapse.
     *
     * @param   bldg - the <code>Building</code> that has been damaged.
     *          This value should not be <code>null</code>, but no
     *          exception will occur.
     * @param   damage - the <code>int</code> amount of damage.
     * @param   why - the <code>String</code> message that describes
     *          why the building took the damage.
     * @return  a <code>String</code> message to be shown to the players.
     */
    private String damageBuilding( Building bldg, int damage, String why ) {
        StringBuffer buffer = new StringBuffer();

        // Do nothing if no building or no damage was passed.
        if ( bldg != null && damage > 0 ) {
            int curCF = bldg.getCurrentCF();
            final int startingCF = curCF;
            curCF -= Math.min( curCF, damage );
            bldg.setCurrentCF( curCF );
            buffer.append( bldg.getName() )
                .append( why )
                .append( damage )
                .append( " points of damage." );

            // If the CF is zero, the building should fall.
            if ( curCF == 0 && startingCF != 0 ) {
                buffer.append( " <<<BUILDING DESTROYED>>>" );
            }

        }
        return buffer.toString();
    }

    /**
     * Receives an packet to unload entityis stranded on immobile transports,
     * and queue all valid requests for execution.  If all players that have
     * stranded entities have answered, executes the pending requests and end
     * the current turn.
     */
    private void receiveUnloadStranded( Packet packet, int connId ) {
        GameTurn.UnloadStrandedTurn turn = null;
        final Player player = game.getPlayer( connId );
        int[] entityIds = (int[]) packet.getObject(0);
        Vector declared = null;
        Player other = null;
        Enumeration pending = null;
        UnloadStrandedAction action = null;
        Entity entity = null;

        // Is this the right phase?
        if (game.getPhase() != Game.PHASE_MOVEMENT) {
            System.err.println
                ("error: server got unload stranded packet in wrong phase");
            return;
        }

        // Are we in an "unload stranded entities" turn?
        if ( game.getTurn() instanceof GameTurn.UnloadStrandedTurn ) {
            turn = (GameTurn.UnloadStrandedTurn) game.getTurn();
        } else {
            System.err.println
                ("error: server got unload stranded packet out of sequence");
            StringBuffer message = new StringBuffer();
            message.append( player.getName() )
                .append( " should not be sending 'unload stranded entity' packets at this time." );
            sendServerChat( message.toString() );
            return;
        }

        // Can this player act right now?
        if (!turn.isValid(connId, game)) {
            System.err.println
                ("error: server got unload stranded packet from invalid player");
            StringBuffer message = new StringBuffer();
            message.append( player.getName() )
                .append( " should not be sending 'unload stranded entity' packets." );
            sendServerChat( message.toString() );
            return;
        }

        // Did the player already send an 'unload' request?
        // N.B. we're also building the list of players who
        //      have declared their "unload stranded" actions.
        declared = new Vector();
        pending = game.getActions();
        while ( pending.hasMoreElements() ) {
            action = (UnloadStrandedAction) pending.nextElement();
            if ( action.getPlayerId() == connId ) {
                System.err.println("error: server got multiple unload stranded packets from player");
                StringBuffer message = new StringBuffer();
                message.append( player.getName() )
                    .append( " should not send multiple 'unload stranded entity' packets." );
                sendServerChat( message.toString() );
                return;
            } else {
                // This player is not from the current connection.
                // Record this player to determine if this turn is done.
                other = game.getPlayer( action.getPlayerId() );
                if ( !declared.contains( other ) ) {
                    declared.addElement( other );
                }
            }
        } // Handle the next "unload stranded" action.

        // Make sure the player selected at least *one* valid entity ID.
        boolean foundValid = false;
        for ( int index = 0; null != entityIds && index < entityIds.length;
              index++ ) {
            entity = game.getEntity( entityIds[index] );
            if (!game.getTurn().isValid(connId, entity, game)) {
                System.err.println("error: server got unload stranded packet for invalid entity");
                StringBuffer message = new StringBuffer();
                message.append( player.getName() )
                    .append( " can not unload stranded entity " );
                if ( null == entity ) {
                    message.append( "#" )
                        .append( entityIds[index] );
                } else {
                    message.append( entity.getDisplayName() );
                }
                message.append( " at this time." );
                sendServerChat( message.toString() );
            } else {
                foundValid = true;
                game.addAction( new UnloadStrandedAction( connId,
                                                          entityIds[index] ) );
            }
        }

        // Did the player choose not to unload any valid stranded entity?
        if ( !foundValid ) {
            game.addAction( new UnloadStrandedAction( connId, Entity.NONE ) );
        }

        // Either way, the connection's player has now declared.
        declared.addElement( player );

        // Are all players who are unloading entities done? Walk
        // through the turn's stranded entities, and look to see
        // if their player has finished their turn.
        entityIds = turn.getEntityIds();
        for ( int index = 0; index < entityIds.length; index++ ) {
            entity = game.getEntity( entityIds[index] );
            other = entity.getOwner();
            if ( !declared.contains( other ) ) {
                // At least one player still needs to declare.
                return;
            }
        }

        // All players have declared whether they're unloading stranded units.
        // Walk the list of pending actions and unload the entities.
        pending = game.getActions();
        while ( pending.hasMoreElements() ) {
            action = (UnloadStrandedAction) pending.nextElement();

            // Some players don't want to unload any stranded units.
            if ( Entity.NONE != action.getEntityId() ) {
                entity = game.getEntity( action.getEntityId() );
                if ( null == entity ) {
                    // After all this, we couldn't find the entity!!!
                    System.err.print
                        ("error: server could not find stranded entity #");
                    System.err.print( action.getEntityId() );
                    System.err.println( " to unload!!!");
                } else {
                    // Unload the entity.  Get the unit's transporter.
                    Entity transporter =
                        game.getEntity( entity.getTransportId() );
                    this.unloadUnit( transporter, entity,
                                     transporter.getPosition(),
                                     transporter.getFacing() );
                }
            }

        } // Handle the next pending unload action

        // Clear the list of pending units and move to the next turn.
        game.resetActions();
        changeToNextTurn();
    }
    /**
     * For all current artillery attacks in the air from this entity
     * with this weapon, clear the list of spotters.  Needed because
     * firing another round before first lands voids spotting.
     *
     * @param entityID int
     */
    private void clearArtillerySpotters(int entityID,int weaponID)  {
        for (Enumeration i = game.getArtilleryAttacks(); i.hasMoreElements();) {
            ArtilleryAttackAction aaa = (ArtilleryAttackAction) i.nextElement();
            if ( aaa.getWR().waa.getEntityId()==entityID &&
                 aaa.getWR().waa.getWeaponId()==weaponID ) {
                aaa.setSpotterIds(null);
            }

        }
    }
    private boolean isEligibleForTargetingPhase(Entity entity) {
        for (Enumeration i = entity.getWeapons(); i.hasMoreElements();) {
              Mounted mounted = (Mounted)i.nextElement();
              WeaponType wtype = (WeaponType)mounted.getType();
              if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                  return true;
              }
          }
          return false;

    }
    private boolean isEligibleForOffboard(Entity entity) {
        return false;//only things w/ tag are, and we don't yet have TAG.
    }

    /**
     * resolve Indirect Artillery Attacks for this turn
     */
    private void resolveIndirectArtilleryAttacks()  {
        Vector results = new Vector(game.getArtillerySize());
        Vector attacks = new Vector(game.getArtillerySize());

        // loop thru received attack actions, getting weapon results
        for (Enumeration i = game.getArtilleryAttacks(); i.hasMoreElements();) {
            ArtilleryAttackAction aaa =
                (ArtilleryAttackAction) i.nextElement();

            // Does the attack land this turn?
            if (aaa.turnsTilHit <= 0) {
                final WeaponResult wr = aaa.getWR();
                //HACK, for correct hit table resolution.
                wr.artyAttackerCoords=aaa.getCoords();
                final Vector spottersBefore=aaa.getSpotterIds();
                final Targetable target = wr.waa.getTarget(game);
                final Coords targetPos = target.getPosition();
                final int playerId = aaa.getPlayerId();
                Entity bestSpotter=null;

                // Are there any valid spotters?
                if ( null != spottersBefore ) {

                    //fetch possible spotters now
                    Enumeration spottersAfter=
                        game.getSelectedEntities( new EntitySelector() {
                                public int player = playerId;
                                public Targetable targ = target;
                                public boolean accept(Entity entity) {
                                    Integer id = new Integer( entity.getId() );
                                    if ( player == entity.getOwnerId() &&
                                         spottersBefore.contains(id) &&
                                         !( LosEffects.calculateLos
                                            (game, entity.getId(), targ)
                                            ).isBlocked() &&
                                         entity.isActive() &&
                                         !entity.isINarcedWith(INarcPod.HAYWIRE)) {
                                        return true;
                                    }
                                    return false;
                                }
                            } );

                    // Out of any valid spotters, pick the best.
                    while ( spottersAfter.hasMoreElements() ) {
                        Entity ent = (Entity) spottersAfter.nextElement();
                        if ( bestSpotter == null || ent.crew.getGunnery() <
                             bestSpotter.crew.getGunnery() ){
                            bestSpotter = ent;
                        }
                    }

                } // End have-valid-spotters

                //If at least one valid spotter, then get the benefits thereof.
                if (null != bestSpotter) {
                    int mod = (bestSpotter.crew.getGunnery() - 4) / 2;
                    wr.toHit.addModifier(mod, "Spotting modifier");
                }

                // Is the attacker still alive?
                Entity artyAttacker = wr.waa.getEntity( game );
                if (null != artyAttacker) {

                    // Get the arty weapon.
                    Mounted weapon = artyAttacker.getEquipment
                        ( wr.waa.getWeaponId() );

                    // If the shot hit the target hex, then all subsequent
                    // fire will hit the hex automatically.
                    if(wr.roll >= wr.toHit.getValue()) {
                        artyAttacker.aTracker.setModifier
                            ( weapon,
                              ToHitData.AUTOMATIC_SUCCESS,
                              targetPos );
                    }
                    // If the shot missed, but was adjusted by a
                    // spotter, future shots are more likely to hit.
                    else if (null != bestSpotter) {
                        artyAttacker.aTracker.setModifier
                            ( weapon,
                              artyAttacker.aTracker.getModifier
                              ( weapon, targetPos ) - 1,
                              targetPos );
                    }

                } // End artyAttacker-alive

                // Schedule this attack to be resolved.
                results.addElement(wr);
                attacks.addElement( aaa );

            } // End attack-hits-this-turn

            // This attack is one round closer to hitting.
            aaa.turnsTilHit--;

        } // Handle the next attack

        // loop through weapon results and resolve
        int lastEntityId = Entity.NONE;
        for (Enumeration i = results.elements();i.hasMoreElements();) {
            WeaponResult wr = (WeaponResult) i.nextElement();
            resolveWeaponAttack(wr, lastEntityId);
            lastEntityId = wr.waa.getEntityId();
        }

        // Clear out all resolved attacks.
        for (Enumeration i = attacks.elements(); i.hasMoreElements();) {
            game.removeArtilleryAttack
                ( (ArtilleryAttackAction) i.nextElement() );
        }
    }

    /**
     * enqueues any indirect artillery attacks made this turn
     */
    private void enqueueIndirectArtilleryAttacks() {
        resolveAllButWeaponAttacks();
        ArtilleryAttackAction aaa;
        for (Enumeration i = game.getActions();i.hasMoreElements();) {
            EntityAction ea = (EntityAction) i.nextElement();
            final Entity firingEntity = game.getEntity(ea.getEntityId());
            if (ea instanceof WeaponAttackAction) {
                final WeaponAttackAction waa = (WeaponAttackAction) ea;
                WeaponResult wr = preTreatWeaponAttack(waa);
                boolean firingAtNewHex = false;
                for (Enumeration j = game.getArtilleryAttacks();
                     !firingAtNewHex && j.hasMoreElements();) {
                    ArtilleryAttackAction oaaa = (ArtilleryAttackAction) j.nextElement();
                    if ( oaaa.getWR().waa.getEntityId() == wr.waa.getEntityId() &&
                         !oaaa.getWR().waa.getTarget(game).getPosition().equals(wr.waa.getTarget(game).getPosition())) {
                        firingAtNewHex = true;
                    }
                }
                if (firingAtNewHex) {
                    clearArtillerySpotters( firingEntity.getId(),
                                            waa.getWeaponId() );
                }
                Enumeration spotters = game.getSelectedEntities(new EntitySelector() {
                    public int player = firingEntity.getOwnerId();
                    public Targetable target = waa.getTarget(game);
                    public boolean accept(Entity entity) {
                        if ( (player == entity.getOwnerId()) &&
                            !((LosEffects.calculateLos(game, entity.getId(), target)).isBlocked()) && entity.isActive()) {
                            return true;
                        } else {
                            return false;
                        }

                    }
                } );

                Vector spotterIds = new Vector();
                while ( spotters.hasMoreElements() ) {
                    Integer id = new Integer
                        ( ((Entity) spotters.nextElement() ).getId() );
                    spotterIds.addElement( id );
                }
                aaa = new ArtilleryAttackAction( wr, game,
                                                 firingEntity.getOwnerId(),
                                                 spotterIds, firingEntity.getPosition());
                game.addArtilleryAttack(aaa);
            }
        }
        game.resetActions();
    }

    /**
     * Credits a Kill for an entity, if the target got killed.
     *
     * @param target   The <code>Entity</code> that got killed.
     * @param attacker The <code>Entity</code> that did the killing.
     */
    private void creditKill(Entity target, Entity attacker) {
        if (target.isDoomed() && !target.getGaveKillCredit()) {
            attacker.addKill(target);
        }
    }

    /**
     * pre-treats a physical attack
     *
     * @param aaa The <code>AbstractAttackAction</code> of the physical attack
     *            to pre-treat
     *
     * @return    The <code>PhysicalResult</code> of that action, including
     *            possible damage.
     */
    private PhysicalResult preTreatPhysicalAttack(AbstractAttackAction aaa) {
        final Entity ae = game.getEntity(aaa.getEntityId());
        int damage = 0;
        PhysicalResult pr = new PhysicalResult();
        ToHitData toHit = new ToHitData();
        pr.roll = Compute.d6(2);
        pr.aaa = aaa;
        if (aaa instanceof BrushOffAttackAction) {
            BrushOffAttackAction baa = (BrushOffAttackAction)aaa;
            int arm = baa.getArm();
            baa.setArm(BrushOffAttackAction.LEFT);
            toHit = BrushOffAttackAction.toHit(game, aaa.getEntityId(), aaa.getTarget(game), BrushOffAttackAction.LEFT);
            baa.setArm(BrushOffAttackAction.RIGHT);
            pr.toHitRight = BrushOffAttackAction.toHit(game, aaa.getEntityId(), aaa.getTarget(game), BrushOffAttackAction.RIGHT);
            damage = BrushOffAttackAction.getDamageFor(ae, BrushOffAttackAction.LEFT);
            pr.damageRight = BrushOffAttackAction.getDamageFor(ae, BrushOffAttackAction.RIGHT);
            baa.setArm(arm);
            pr.rollRight = Compute.d6(2);
        } else if (aaa instanceof ChargeAttackAction) {
            ChargeAttackAction caa = (ChargeAttackAction)aaa;
            toHit = caa.toHit(game);
            damage = ChargeAttackAction.getDamageFor(ae);
        } else if (aaa instanceof ClubAttackAction) {
            ClubAttackAction caa = (ClubAttackAction)aaa;
            toHit = caa.toHit(game);
            damage = ClubAttackAction.getDamageFor(ae, caa.getClub());
        } else if (aaa instanceof DfaAttackAction) {
            DfaAttackAction daa = (DfaAttackAction)aaa;
            toHit = daa.toHit(game);
            damage = DfaAttackAction.getDamageFor(ae);
        } else if (aaa instanceof KickAttackAction) {
            KickAttackAction kaa = (KickAttackAction)aaa;
            toHit = kaa.toHit(game);
            damage = KickAttackAction.getDamageFor(ae, kaa.getLeg());
        } else if (aaa instanceof ProtomechPhysicalAttackAction) {
            ProtomechPhysicalAttackAction paa = (ProtomechPhysicalAttackAction)aaa;
            toHit = paa.toHit(game);
            damage = ProtomechPhysicalAttackAction.getDamageFor(ae);
        } else if (aaa instanceof PunchAttackAction) {
            PunchAttackAction paa = (PunchAttackAction)aaa;
            int arm = paa.getArm();
            int damageRight = 0;
            paa.setArm(PunchAttackAction.LEFT);
            toHit = paa.toHit(game);
            paa.setArm(PunchAttackAction.RIGHT);
            ToHitData toHitRight = paa.toHit(game);
            damage = PunchAttackAction.getDamageFor(ae, PunchAttackAction.LEFT);
            damageRight = PunchAttackAction.getDamageFor(ae, PunchAttackAction.RIGHT);
            paa.setArm(arm);
            // If we're punching while prone (at a Tank,
            // duh), then we can only use one arm.
            if ( ae.isProne() ) {
                double oddsLeft = Compute.oddsAbove(toHit.getValue());
                double oddsRight = Compute.oddsAbove(toHitRight.getValue());
                // Use the best attack.
                if (  oddsLeft*damage > oddsRight*damageRight ) {
                    paa.setArm(PunchAttackAction.LEFT);
                } else paa.setArm(PunchAttackAction.RIGHT);
            }
            pr.damageRight = damageRight;
            pr.toHitRight = toHitRight;
            pr.rollRight = Compute.d6(2);
        } else if (aaa instanceof PushAttackAction) {
            PushAttackAction paa = (PushAttackAction)aaa;
            toHit = paa.toHit(game);
        } else if (aaa instanceof ThrashAttackAction) {
            ThrashAttackAction taa = (ThrashAttackAction)aaa;
            toHit = taa.toHit(game);
            damage = ThrashAttackAction.getDamageFor(ae);
        }
        pr.toHit = toHit;
        pr.damage = damage;
        return pr;
    }

    /**
     * Resolve a Physical Attack
     *
     * @param pr  The <code>PhysicalResult</code> of the physical attack
     * @param cen The <code>int</code> Entity Id of the entit's whose
     *            physical attack was last resolved
     */
    private void resolvePhysicalAttack(PhysicalResult pr, int cen) {
        AbstractAttackAction aaa = pr.aaa;
        int roll = pr.roll;
        if (aaa instanceof PunchAttackAction) {
            PunchAttackAction paa = (PunchAttackAction)aaa;
            if (paa.getArm() == PunchAttackAction.BOTH) {
                paa.setArm(PunchAttackAction.LEFT);
                pr.aaa = (AbstractAttackAction)paa;
                resolvePunchAttack(pr, cen);
                cen = paa.getEntityId();
                paa.setArm(PunchAttackAction.RIGHT);
                pr.aaa = (AbstractAttackAction)paa;
                resolvePunchAttack(pr, cen);
            } else {
                resolvePunchAttack(pr, cen);
                cen = paa.getEntityId();
            }
        } else if (aaa instanceof KickAttackAction) {
            resolveKickAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof BrushOffAttackAction) {
            BrushOffAttackAction baa = (BrushOffAttackAction)aaa;
            if (baa.getArm() == BrushOffAttackAction.BOTH) {
                baa.setArm(BrushOffAttackAction.LEFT);
                pr.aaa = (AbstractAttackAction)baa;
                resolveBrushOffAttack(pr, cen);
                cen = baa.getEntityId();
                baa.setArm(BrushOffAttackAction.RIGHT);
                pr.aaa = (AbstractAttackAction)baa;
                resolveBrushOffAttack(pr, cen);
            } else {
                resolveBrushOffAttack(pr, cen);
                cen = baa.getEntityId();
            }
        } else if (aaa instanceof ThrashAttackAction) {
            resolveThrashAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof ProtomechPhysicalAttackAction) {
            resolveProtoAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof ClubAttackAction) {
            resolveClubAttack(pr, cen);
            cen = aaa.getEntityId();
        } else if (aaa instanceof PushAttackAction) {
            resolvePushAttack(pr, cen);
            cen = aaa.getEntityId();
        }  else if (aaa instanceof ChargeAttackAction) {
            resolveChargeAttack(pr, cen);
            cen = aaa.getEntityId();
        }  else if (aaa instanceof DfaAttackAction) {
            resolveDfaAttack(pr, cen);
            cen = aaa.getEntityId();
        } else {
            // hmm, error.
        }
        // Not all targets are Entities.
        Targetable target = game.getTarget( aaa.getTargetType(),
                                        aaa.getTargetId() );
        if ( target instanceof Entity ) {
            creditKill( (Entity) target, game.getEntity(cen) );
        }
    }

    /**
     * Add any extreme gravity PSRs the entity gets due to its movement
     *
     * @param entity The <code>Entity</code> to check.
     * @param step   The last <code>MoveStep</code> of this entity
     * @param curPos The current <code>Coords</code> of this entity
     * @param cachedMaxMPExpenditure Server checks run/jump MP at start of move, as appropriate, caches to avoid mid-move change in MP causing erroneous grav check
     */
    private void checkExtremeGravityMovement(Entity entity, MoveStep step, Coords curPos, int cachedMaxMPExpenditure) {
        PilotingRollData rollTarget;
        if (game.getOptions().floatOption("gravity") != 1) {
            if (entity instanceof Mech) {
                if ((step.getMovementType() == Entity.MOVE_WALK) || (step.getMovementType() == Entity.MOVE_RUN)) {
                    if (step.getMpUsed() > cachedMaxMPExpenditure) {
                        // We moved too fast, let's make PSR to see if we get damage
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    }
                } else if (step.getMovementType() == Entity.MOVE_JUMP) {
                    if (step.getMpUsed() > cachedMaxMPExpenditure) {
                        // We jumped too far, let's make PSR to see if we get damage
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    } else if (game.getOptions().floatOption("gravity") > 1) {
                        // jumping in high g is bad for your legs
                        rollTarget = entity.getBasePilotingRoll();
                        rollTarget.append(new PilotingRollData(entity.getId(), 0, "jumped in high gravity"));
                        game.addExtremeGravityPSR(rollTarget);
                    }
                }
            } else if (entity instanceof Tank) {
                if (step.getMovementType() == Entity.MOVE_WALK
                    || step.getMovementType() == Entity.MOVE_RUN) {
                    // For Tanks, we need to check if the tank had
                    // more MPs because it was moving along a road.
                    if ((step.getMpUsed() > cachedMaxMPExpenditure) && !step.isOnlyPavement()) {
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    }
                    else if (step.getMpUsed() > cachedMaxMPExpenditure + 1) {
                        // If the tank was moving on a road, he got a +1 bonus.
                        // N.B. The Ask Precentor Martial forum said that a 4/6
                        //      tank on a road can move 5/7, **not** 5/8.
                        game.addExtremeGravityPSR(entity.checkMovedTooFast(step));
                    } // End tank-has-road-bonus
                }
            }
        }
    }

    /**
     * Damage the inner structure of a mech's leg / a tank's front.
     * This only happens when the Entity fails an extreme Gravity PSR.
     * @param entity The <code>Entity</code> to damage.
     * @param damage The <code>int</code> amount of damage.
     */
    private void doExtremeGravityDamage(Entity entity, int damage) {
        HitData hit;
        if (entity instanceof BipedMech) {
            for (int i = 6; i<=7; i++) {
                hit = new HitData (i);
                phaseReport.append(damageEntity(entity, hit, damage, false, 0, true));
            }
        } if (entity instanceof QuadMech) {
            for (int i = 4; i<=7; i++) {
                hit = new HitData (i);
                phaseReport.append(damageEntity(entity, hit, damage, false, 0, true));
            }
        } else if (entity instanceof Tank) {
            hit = new HitData (Tank.LOC_FRONT);
            phaseReport.append(damageEntity(entity, hit, damage, false, 0, true));
        }
    }

    /**
     * Eject an Entity.
     * @param entity    The <code>Entity</code> to eject.
     * @param autoEject The <code>boolean</code> state of the entity's auto-
     *                  ejection system
     * @return a <code>String</code> description for the serverlog.
     */
    public String ejectEntity(Entity entity, boolean autoEject) {

        StringBuffer desc = new StringBuffer();
        // An entity can only eject it's crew once.
        if (entity.getCrew().isEjected())
            return "";

        // Mek pilots may get hurt during ejection,
        // and run around the board afterwards.
        if (entity instanceof Mech) {
            PilotingRollData rollTarget = new PilotingRollData(entity.getId(), entity.getCrew().getPiloting(), "ejecting");
            if (entity.isProne()) {
                rollTarget.addModifier(5, "Mech is prone");
            }
            if (entity.getCrew().isUnconscious()) {
                rollTarget.addModifier(3, "pilot unconscious");
            }
            if (autoEject) {
                rollTarget.addModifier(1, "automatic ejection");
            }
            if (entity.getInternal(Mech.LOC_HEAD) < 3) {
                rollTarget.addModifier(Math.min(3 - entity.getInternal(Mech.LOC_HEAD),2), "Head Internal Structure Damage");
            }
            int facing = entity.getFacing();
            Coords targetCoords = entity.getPosition().translated((facing + 3)%6);
            IHex targetHex = game.board.getHex(targetCoords);
            if (targetHex != null) {
                if (targetHex.terrainLevel(Terrains.WATER) > 0) {
                    rollTarget.addModifier(-1, "landing in water");
                } else if (targetHex.terrainLevel(Terrains.ROUGH) > 0) {
                    rollTarget.addModifier(0, "landing in rough");
                } else if (targetHex.terrainLevel(Terrains.RUBBLE) > 0) {
                    rollTarget.addModifier(0, "landing in rubble");
                } else if (targetHex.terrainLevel(Terrains.WOODS) == 1) {
                    rollTarget.addModifier(2, "landing in light woods");
                } else if (targetHex.terrainLevel(Terrains.WOODS) == 2) {
                    rollTarget.addModifier(3, "landing in heavy woods");
                } else if (targetHex.terrainLevel(Terrains.BLDG_ELEV) > 0) {
                    rollTarget.addModifier(targetHex.terrainLevel(Terrains.BLDG_ELEV), "landing in a building");
                } else rollTarget.addModifier(-2, "landing in clear terrain");
            } else {
                    rollTarget.addModifier(-2, "landing off the board");
            }
            if (autoEject) {
                desc.append("\n").append(entity.getDisplayName())
                    .append(" suffers an ammunition explosion, but the autoeject system was engaged.");
            }
            // okay, print the info
            desc.append("\n" ).append( entity.getDisplayName() )
                .append( " must make a piloting skill check (" )
                .append( rollTarget.getLastPlainDesc() ).append( ")" ).append( ".\n");
            // roll
            final int diceRoll = Compute.d6(2);
            desc.append("Needs " ).append( rollTarget.getValueAsString()
            ).append( " [" ).append( rollTarget.getDesc() ).append( "]"
            ).append( ", rolls " ).append( diceRoll ).append( " : ");
            // create the MechWarrior in any case, for campaign tracking
            MechWarrior pilot = new MechWarrior(entity);
            pilot.setDeployed(true);
            pilot.setId(getFreeEntityId());
            game.addEntity(pilot.getId(), pilot);
            send(createAddEntityPacket(pilot.getId()));
            // make him not get a move this turn
            pilot.setDone(true);
            if (diceRoll < rollTarget.getValue()) {
                desc.append("fails.\n");
                desc.append(damageCrew(pilot, 1));
            } else {
                desc.append("succeeds.\n");
            }
            if (entity.getCrew().isDoomed()) {
                desc.append("but the pilot does not survive!\n");
                desc.append(destroyEntity(pilot, "deadly ejection", false, false));
            }
            else {
                // Add the pilot as an infantry unit on the battlefield.
                if (game.board.contains(targetCoords)) {
                    pilot.setPosition(targetCoords);
/* Can pilots eject into water???
   ASSUMPTION : They can (because they get a -1 mod to the PSR.
                    // Did the pilot land in water?
                    if ( game.board.getHex( targetCoords).levelOf
                         ( Terrain.WATER ) > 0 ) {
                        desc.append("and the pilot ejects, but lands in water!!!\n");
                        desc.append(destroyEntity( pilot, "a watery grave", false ));
                    } else {
                        desc.append("and the pilot ejects safely!\n");
                    }
*/
                    desc.append("\n The pilot ejects safely!\n");
                    if (game.getOptions().booleanOption("vacuum")) {
                        desc.append("Unfortunately, the pilot is not wearing a pressure suit.");
                        desc.append(destroyEntity(pilot, "explosive decompression", false, false));
                    }
                    // Update the entity, unless the pilot died.
                    if (!pilot.isDoomed()) {
                        this.entityUpdate(pilot.getId());
                    }
                    // check if the pilot lands in a minefield
                    doEntityDisplacementMinefieldCheck( pilot,
                            entity.getPosition(),
                            targetCoords );
                } else {
                    desc.append("\n The pilot ejects safely and lands far from the battle!");
                    if (game.getOptions().booleanOption("vacuum")) {
                        desc.append("Unfortunately, the pilot is not wearing a pressure suit.");
                        desc.append(destroyEntity(pilot, "explosive decompression", false, false));
                    } else {
                        game.removeEntity( pilot.getId(), Entity.REMOVE_IN_RETREAT );
                        send(createRemoveEntityPacket(pilot.getId(), Entity.REMOVE_IN_RETREAT) );
                    }
                }
            } // Pilot safely ejects.

        } // End entity-is-Mek

        // Mark the entity's crew as "ejected".
        entity.getCrew().setEjected( true );
        desc.append(destroyEntity(entity, "ejection", true, true));

        // only remove the unit that ejected in the movement phase
        if (game.getPhase() == Game.PHASE_MOVEMENT) {
            game.removeEntity( entity.getId(), Entity.REMOVE_EJECTED );
            send(createRemoveEntityPacket(entity.getId(), Entity.REMOVE_EJECTED));
        }
        return desc.toString();
    }

    /**
     * Checks if ejected Mechwarriors are eligible to be picked up,
     * and if so, captures them or picks them up
     */
    private void resolveMechWarriorPickUp() {
        // fetch all mechWarriors that are not picked up
        Enumeration mechWarriors =
            game.getSelectedEntities( new EntitySelector() {
                public boolean accept(Entity entity) {
                    if (entity instanceof MechWarrior) {
                        MechWarrior mw = (MechWarrior)entity;
                        if (mw.getPickedUpById() == Entity.NONE && !mw.isDoomed()) {
                            return true;
                        }
                    }
                    return false;
                }
            } );
        // loop through them, check if they are in a hex occupied by another
        // unit
        while ( mechWarriors.hasMoreElements() ) {
            boolean pickedUp = false;
            MechWarrior e = (MechWarrior) mechWarriors.nextElement();
            Enumeration pickupEntities = game.getEntities(e.getPosition());
            while (pickupEntities.hasMoreElements() ) {
                Entity pe = (Entity) pickupEntities.nextElement();
                if (pe.isDoomed() || pe.isShutDown() || pe.getCrew().isUnconscious()) {
                    continue;
                }
                if (!pickedUp && pe.getOwnerId() == e.getOwnerId() && pe.getId() != e.getId()) {
                    if (pe instanceof MechWarrior && game.getOptions().booleanOption("no_pilot_pickup")) {
                        phaseReport.append(pe.getDisplayName())
                             .append(" sits down with his colleagues and cracks open a beer!\n");
                        continue;
                    }
                    // Pick up the unit.
                    pe.pickUp(e);
                    // The picked unit is being carried by the loader.
                    e.setPickedUpById(pe.getId());
                    e.setPickedUpByExternalId(pe.getExternalId());
                    pickedUp = true;
                    phaseReport.append(e.getDisplayName()).append(" has been picked up by ")
                         .append(pe.getDisplayName()).append(".\n");
                }
            }
            if (!pickedUp) {
                Enumeration pickupEnemyEntities = game.getEnemyEntities(e.getPosition(), e);
                while (pickupEnemyEntities.hasMoreElements() ) {
                    Entity pe = (Entity) pickupEnemyEntities.nextElement();
                    if (pe.isDoomed() || pe.isShutDown() ||
                        pe.getCrew().isUnconscious()) {
                        continue;
                    }
                    if (pe instanceof MechWarrior &&
                          game.getOptions().booleanOption("no_pilot_pickup")) {
                        phaseReport.append(pe.getDisplayName())
                             .append(" sits down with his colleagues and cracks open a beer\n");
                        continue;
                    }
                    // Capture the unit.
                    pe.pickUp(e);
                    // The captured unit is being carried by the loader.
                    e.setCaptured( true );
                    e.setPickedUpById(pe.getId());
                    e.setPickedUpByExternalId(pe.getExternalId());
                    pickedUp = true;
                    phaseReport.append(e.getDisplayName()).append(" has been picked up by ")
                         .append(pe.getDisplayName()).append(".\n");
                }
            }
            if (pickedUp) {
                // Remove the picked-up unit from the screen.
                e.setPosition( null );
                // Update the loaded unit.
                this.entityUpdate( e.getId() );
            }
        }
    }
    /**
     * destroy all wheeled and tracked Tanks that got displaced into water
     */
    private void resolveSinkVees() {
        Enumeration sinkableTanks =
            game.getSelectedEntities( new EntitySelector() {
            public boolean accept(Entity entity) {
                if (entity.isOffBoard()) {
                    return false;
                }
                // TODO : correctly handle bridges.
                if (entity instanceof Tank &&
                    (entity.getMovementType() == Entity.MovementType.TRACKED ||
                     entity.getMovementType() == Entity.MovementType.WHEELED ) &&
                    game.board.getHex(entity.getPosition()).terrainLevel(Terrains.WATER) > 0) {
                        return true;
                }
                return false;
            }
        });
        while (sinkableTanks.hasMoreElements()) {
            Entity e = (Entity)sinkableTanks.nextElement();
            phaseReport.append(destroyEntity(e, "a watery grave", false));
        }
    }

    /**
     * let all Entities make their "break-free-of-swamp-stickyness" PSR
     */
    private void doTryUnstuck() {
        Enumeration stuckEntities =
            game.getSelectedEntities( new EntitySelector() {
            public boolean accept(Entity entity) {
                if (entity.isStuck()) {
                    return true;
                }
                return false;
            }
        });
        PilotingRollData rollTarget;
        while (stuckEntities.hasMoreElements()) {
            Entity entity = (Entity)stuckEntities.nextElement();
            rollTarget = entity.getBasePilotingRoll();
            // okay, print the info
            roundReport.append("\n")
                .append( entity.getDisplayName() )
                .append( " tries to break free of the swamp.\n" );

            // roll
            final int diceRoll = Compute.d6(2);
            roundReport.append("Needs " ).append( rollTarget.getValueAsString()
                               ).append( " [" ).append( rollTarget.getDesc() ).append( "]"
                               ).append( ", rolls " ).append( diceRoll ).append( " : ");
            if (diceRoll < rollTarget.getValue()) {
                roundReport.append("fails.\n");
            } else {
                roundReport.append("succeeds.\n");
                entity.setStuck(false);
            }
        }
    }
    
    /**
     * Remove all iNarc pods from all vehicles that did not
     * move and shoot this round
     * NOTE: this is not quite what the rules say, the player
     * should be able to choose wether or not to remove all iNarc Pods
     * that are attached.
     */
    private void resolveVeeINarcPodRemoval() {
        Enumeration vees =
            game.getSelectedEntities( new EntitySelector() {
            public boolean accept(Entity entity) {
                if (entity instanceof Tank &&
                    entity.mpUsed == 0) {
                    return true;
                }
                return false;
            }
        });
        boolean canSwipePods;
        while (vees.hasMoreElements()) {
            canSwipePods = true;
            Entity entity = (Entity)vees.nextElement();
            for (int i=0;i<=5;i++) {
                if ( entity.weaponFiredFrom(i) ) {
                    canSwipePods = false;
                }
            }
            if (canSwipePods && entity.hasINarcPodsAttached() &&
                entity.getCrew().isActive()) {
                entity.removeAllINarcPods();
                phaseReport.append("The crew of ")
                     .append(entity.getDisplayName())
                     .append(" has removed all attached iNarc Pods.\n");
            }
        }
    }
    
    private void deadEntitiesCleanup() {
        //See note above where knownDeadEntities variable is declared
        /*
        Entity en = null;
        for(Enumeration k = game.getGraveyardEntities(); k.hasMoreElements(); en = (Entity) k.nextElement()) {
            if (en != null) {
                if (!knownDeadEntities.contains(en)) {
                    knownDeadEntities.add(en);
                }
            }           
        }
        */
    }
}
