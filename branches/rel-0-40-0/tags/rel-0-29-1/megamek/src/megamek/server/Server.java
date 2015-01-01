/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.options.*;
import megamek.server.commands.*;

/**
 * @author Ben Mazur
 */
public class Server
implements Runnable {
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

    /**
     * Construct a new GameHost and begin listening for
     * incoming clients.
     */
    public Server(String password, int port) {
        this.password = password.length() > 0 ? password : null;
        // initialize server socket
        try {
            serverSocket = new ServerSocket(port);
        } catch(IOException ex) {
            System.err.println("could not create server socket on port " + port);
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
                System.out.println("s: hosting on address = " + addresses[i].getHostAddress());
            }
        } catch (UnknownHostException  e) {
            // oh well.
        }

        System.out.println("s: password = " + this.password);
        
        connector = new Thread(this);
        connector.start();
        
        // register commands
        registerCommand(new HelpCommand(this));
        registerCommand(new KickCommand(this));
        registerCommand(new ResetCommand(this));
        registerCommand(new RollCommand(this));
        registerCommand(new SaveGameCommand(this));
        registerCommand(new SkipCommand(this));
        registerCommand(new VictoryCommand(this));
        registerCommand(new WhoCommand(this));
        registerCommand(new SeeAllCommand(this));
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
            ent.setOwner(game.getPlayer(ent.getOwnerId()));
            ent.setGame(game);
            ent.restore();
        }
        
        for (Enumeration e = game.getPlayers(); e.hasMoreElements(); ) {
            Player p = (Player)e.nextElement();
            p.setGame(game);
            p.setGhost(true);
        }

        // Re-initialize the game's board.
        // TODO: have megamek.common.Board's de-serialization
        //       reconstruct the transient bldgByCoords instead.
        game.board.newData( game.board );

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
        for (Enumeration i = connectionsPending.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            conn.die();
        }
        connectionsPending = null;
        
        // kill active connnections
        for (Enumeration i = connections.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            conn.die();
        }
        connections = null;
        connectionIds = null;
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
        while (game.getEntity(entityCounter) != null) {
            entityCounter++;
        }
        return entityCounter;
    }
    
    /**
     * Allow the player to set whatever parameters he is able to
     */
    private void receivePlayerInfo(Packet packet, int connId) {
        Player player = (Player)packet.getObject(0);
        game.getPlayer(connId).setColorIndex(player.getColorIndex());
        game.getPlayer(connId).setStartingPos(player.getStartingPos());
        game.getPlayer(connId).setTeam(player.getTeam());
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
            System.out.println("server: got a client name from a non-pending connection");
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
        if (game.phase != Game.PHASE_LOUNGE
        && game.getEntitiesOwnedBy(getPlayer(connId)) < 1) {
            getPlayer(connId).setObserver(true);
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
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (int i = 0; i < addresses.length; i++) {
                sendServerChat(connId, "Machine IP is " + addresses[i].getHostAddress());
            }
        } catch (UnknownHostException  e) {
            // oh well.
        }
        
        System.out.println("s: player " + connId
        + " (" + getPlayer(connId).getName() + ") connected from "
        + getClient(connId).getSocket().getInetAddress());
        
        sendServerChat(getPlayer(connId).getName() + " connected from "
        + getClient(connId).getSocket().getInetAddress());
        
        // there is more than one player, uncheck the friendly fire option
        if (game.getNoOfPlayers() > 1 && game.getOptions().booleanOption("friendly_fire")) {
            game.getOptions().getOption("friendly_fire").setValue(false);
            send(createGameSettingsPacket());
        }
    }
    
    /**
     * Sends a player the info they need to look at the current phase
     */
    private void sendCurrentInfo(int connId) {
        transmitAllPlayerConnects(connId);
        send(connId, createGameSettingsPacket());
        if (doBlind()) {
            send(connId, createFilteredEntitiesPacket(getPlayer(connId)));
        }
        else {
            send(connId, createEntitiesPacket());
        }
        switch (game.phase) {
            case Game.PHASE_LOUNGE :
                send(connId, createMapSettingsPacket());
                break;
            case Game.PHASE_INITIATIVE :
            case Game.PHASE_MOVEMENT_REPORT :
            case Game.PHASE_FIRING_REPORT :
            case Game.PHASE_END :
            case Game.PHASE_VICTORY :
                send(connId, createReportPacket());
            default :
                getPlayer(connId).setDone(game.getEntitiesOwnedBy(getPlayer(connId)) <= 0);
                send(connId, createBoardPacket());
                break;
        }
        send(connId, new Packet(Packet.COMMAND_PHASE_CHANGE, new Integer(game.phase)));
        if (game.getPhase() == Game.PHASE_FIRING || game.getPhase() == Game.PHASE_PHYSICAL) {
            // can't go above, need board to have been sent
            send(createAttackPacket(game.getActionsVector(), false));
            send(createAttackPacket(game.getChargesVector(), true));
        }
        if (game.phaseHasTurns(game.getPhase())) {
            send(connId, createTurnVectorPacket());
            send(connId, createTurnIndexPacket());
        }
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
        if (colorUsed[player.getColorIndex()]) {
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
    public void disconnected(int connId) {
        final Connection conn = getClient(connId);
        final Player player = getPlayer(connId);
        
        // if the connection's even still there, remove it
        if (conn != null) {
            conn.die();
            connections.removeElement(conn);
            connectionIds.remove(new Integer(connId));
        }
        
        // in the lounge, just remove all entities for that player
        if (game.phase == Game.PHASE_LOUNGE) {
            removeAllEntitesOwnedBy(player);
        }
        
        // if a player has active entities, he becomes a ghost
        if (game.getEntitiesOwnedBy(player) > 0) {
            player.setGhost(true);
            player.setDone(true);
            send(createPlayerUpdatePacket(connId));
        } else {
            game.removePlayer(connId);
            send(new Packet(Packet.COMMAND_PLAYER_REMOVE, new Integer(connId)));
        }
        
        // make sure the game advances
        if (game.phaseHasTurns(game.getPhase())) {
            if (game.getTurn().getPlayerNum() == connId) {
                sendGhostSkipMessage();
            }
        } else {
            checkReady();
        }
        
        System.out.println("s: player " + connId + " disconnected");
        if (player != null) {
            sendServerChat(player.getName() + " disconnected.");
        } else {
            sendServerChat("Player #" + connId + " disconnected.");
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
            p.setObserver(game.getEntitiesOwnedBy(p) < 1 && game.phase != Game.PHASE_LOUNGE);
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
        
        //TODO: remove ghosts
        
        // reset all players
        resetPlayersDone();
        transmitAllPlayerDones();
        
        changePhase(Game.PHASE_LOUNGE);
    }
    
    public void autoSave()
    {
        saveGame("autosave");
    }
    
    public void saveGame(String sFile) {
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

        sendChat("MegaMek", "Game saved to " + sFinalFile);
    }
    
    public boolean loadGame(File f) {
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
        for (Enumeration i = connectionsPending.elements(); i.hasMoreElements();) {
            final Connection conn = (Connection)i.nextElement();
            
            if (conn.getId() == connId) {
                return conn;
            }
        }
        return null;
    }
    
    /**
     * Called at the beginning of each game round to reset values on this entity
     * that are reset every round
     */
    private void resetEntityRound() {
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity entity = (Entity)e.nextElement();
            
            entity.newRound();
        }
    }
    
    /**
     * Called at the beginning of each phase.  Sets and resets
     * any entity parameters that need to be reset.
     */
    private void resetEntityPhase() {
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
            
            // reset done to false
            entity.setDone(!entity.isActive());
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
            if ( game.getVictoryPlayerId() == Player.PLAYER_NONE ) {
                roundReport.append( "the Chicaco Cubs!!!\n\n" );
            } else {
                roundReport.append
                    ( getPlayer(game.getVictoryPlayerId()).getName() );
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
            roundReport.append("\nThe following utterly destroyed units are not available for salvage:\n");
            while ( devastated.hasMoreElements() ) {
                Entity entity = (Entity) devastated.nextElement();
                roundReport.append(entity.victoryReport());
                roundReport.append('\n');
            }
        }
        roundReport.append("\nDetailed unit status saved to entitystatus.txt\n");
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
                .append( " ++++++++++\n");

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
                sb.append("=============================================================\n");
                sb.append("The following utterly destroyed units are not available for salvage:\n");
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
                            .append( ")\n" );
                    }
                } // Handle the next unsalvageable unit for the player
                sb.append("=============================================================\n");
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
        // are there any active players?
        boolean allAboard = true;
        // check if all active players are done
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if (!player.isGhost() && !player.isObserver() && !player.isDone()) {
                allAboard = false;
            }
        }
        // need at least one entity in the game for the lounge phase to end
        if (allAboard && !game.phaseHasTurns(game.phase)
        && (game.phase != Game.PHASE_LOUNGE || game.getNoOfEntities() > 0)) {
            endCurrentPhase();
        }
    }
    
    /**
     * Called when the current player has done his current turn and the turn
     * counter needs to be advanced.
     * Also enforces the "inf_move_multi" option.  If the player has just moved
     * infantry with a "normal" turn, adds up to Game.INF_MOVE_MULTI - 1 more
     * infantry-specific turns after the current turn.
     */
    private void endCurrentTurn(Entity entityUsed) {
        // enforce "inf_move_multi" option
        boolean turnsChanged = false;
        if (game.getOptions().booleanOption("inf_move_multi")
        && entityUsed instanceof Infantry
        && !(game.getTurn() instanceof GameTurn.OnlyInfantryTurn)) {
            int playerId = game.getTurn().getPlayerNum();
            int remaining = game.infantryLeft(playerId);
            int moreInfTurns = Math.min(Game.INF_MOVE_MULTI - 1, remaining); 
            for (int i = 0; i < moreInfTurns; i++) {
		
                GameTurn newTurn = new GameTurn.OnlyInfantryTurn(playerId);
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
        game.phase = phase;

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
                resetEntityPhase();
                checkForObservers();
                // roll 'em
                resetActivePlayersDone();
                rollInitiative();
                //setIneligible(phase);
                determineTurnOrder();
                writeInitiativeReport();
                send(createReportPacket());
                autoSave();
                break;
            case Game.PHASE_DEPLOYMENT :
            case Game.PHASE_MOVEMENT :
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
                resetEntityPhase();
                checkForObservers();
                setIneligible(phase);
                determineTurnOrder();
                resetActivePlayersDone();
                //send(createEntitiesPacket());
                entityAllUpdate();
                game.resetPhaseReport();
                phaseReport = game.getPhaseReport(); //HACK
                break;
            case Game.PHASE_END :
                game.resetPhaseReport();
                phaseReport = game.getPhaseReport(); //HACK
                resolveHeat();
                checkForSuffocation();
                resolveFire();
                resolveCrewDamage();
                resolveCrewWakeUp();
                resetEntityPhase();
                checkForObservers();
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                }
                log.append( "\n" );
                log.append( roundReport.toString() );
            case Game.PHASE_MOVEMENT_REPORT :
            case Game.PHASE_FIRING_REPORT :
                resetActivePlayersDone();
                send(createReportPacket());
                break;
            case Game.PHASE_VICTORY :
                prepareVictoryReport();
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
            case Game.PHASE_DEPLOYMENT :
            case Game.PHASE_MOVEMENT :
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
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
                setupTeams();        
                applyBoardSettings();
                game.setHasDeployed(false);
                game.determineWindDirection();
                // If we add transporters for any Magnetic Clamp
                // equiped squads, then update the clients' entities.
                if ( game.checkForMagneticClamp() ) {
                    send(createEntitiesPacket());
                }
                // transmit the board to everybody
                send(createBoardPacket());
                break;
            case Game.PHASE_DEPLOYMENT :
            case Game.PHASE_MOVEMENT :
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
                changeToNextTurn();
                break;
        }
    }
    
    /**
     * Ends this phase and moves on to the next.
     */
    private void endCurrentPhase() {
        switch (game.phase) {
            case Game.PHASE_LOUNGE :
                changePhase(Game.PHASE_EXCHANGE);
                break;
            case Game.PHASE_EXCHANGE :
                changePhase(Game.PHASE_INITIATIVE);
                break;
            case Game.PHASE_DEPLOYMENT :
                game.setHasDeployed(true);
                changePhase(Game.PHASE_INITIATIVE);
                break;
            case Game.PHASE_INITIATIVE :
                if (game.hasDeployed()) {
                    changePhase(Game.PHASE_MOVEMENT);
                }
                else {
                    changePhase(Game.PHASE_DEPLOYMENT);
                }
                break;
            case Game.PHASE_MOVEMENT :
                roundReport.append("\nMovement Phase\n-------------------\n");
                addMovementHeat();
                resolveCrewDamage();
                resolvePilotingRolls(); // Skids cause damage in movement phase
                resolveCrewDamage(); // again, I guess
                checkForFlamingDeath();
                applyBuildingDamage();
                // check phase report
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                    changePhase(Game.PHASE_MOVEMENT_REPORT);
                } else {
                    roundReport.append("<nothing>\n");
                    changePhase(Game.PHASE_FIRING);
                }
                break;
            case Game.PHASE_MOVEMENT_REPORT :
                changePhase(Game.PHASE_FIRING);
                break;
            case Game.PHASE_FIRING :
                resolveAllButWeaponAttacks();
                assignAMS();
                resolveOnlyWeaponAttacks();
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                applyBuildingDamage();
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
                checkFor20Damage();
                resolveCrewDamage();
                resolvePilotingRolls();
                resolveCrewDamage(); // again, I guess
                applyBuildingDamage();
                // check phase report
                if (phaseReport.length() > 0) {
                    roundReport.append(phaseReport.toString());
                } else {
                    roundReport.append("<nothing>\n");
                }
                changePhase(Game.PHASE_END);
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
        
        if (game.getFirstEntity() == null) {
            sendTurnErrorSkipMessage();
        }

        if (getPlayer(nextTurn.getPlayerNum()).isGhost()) {
            sendGhostSkipMessage();
        }
    }

    /**
     * Sends out a notification message indicating that a ghost player may
     * be skipped.
     */
    private void sendGhostSkipMessage() {
        Player ghost = getPlayer(game.getTurn().getPlayerNum());
        sendServerChat("Player '" + ghost.getName() + "' is disconnected.  You may skip his/her current turn with the /skip command.");
    }
    
    /**
     * Sends out a notification message indicating that the current turn is an
     * error and should be skipped.
     */
    private void sendTurnErrorSkipMessage() {
        Player player = getPlayer(game.getTurn().getPlayerNum());
        sendServerChat("Player '" + player.getName() + "' has no units to move.  You should skip his/her/your current turn with the /skip command. "
        + "You may want to report this error.  See the MegaMek homepage (http://megamek.sf.net/) for details.");
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
                    processMovement(toSkip, new MovementData());
                }
                endCurrentTurn(toSkip);
                break;
            case Game.PHASE_FIRING :
            case Game.PHASE_PHYSICAL :
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
        return game.getFirstEntity() == null 
            || getPlayer(game.getTurn().getPlayerNum()).isGhost();
    }
    
    /**
     * Returns true if victory conditions have been met.  Victory conditions
     * are when there is only one player left with mechs or only one team.
     */
    public boolean victory() {
        if (game.isForceVictory()) {
            return true;
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
            if (game.getLiveEntitiesOwnedBy(player) <= 0) {
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
        Board[] sheetBoards = new Board[mapSettings.getMapWidth() * mapSettings.getMapHeight()];
        for (int i = 0; i < mapSettings.getMapWidth() * mapSettings.getMapHeight(); i++) {
            sheetBoards[i] = new Board();
            String name = (String)mapSettings.getBoardsSelectedVector().elementAt(i);
            boolean isRotated = false;
            if ( name.startsWith( Board.BOARD_REQUEST_ROTATION ) ) {
                isRotated = true;
                name = name.substring( Board.BOARD_REQUEST_ROTATION.length() );
            }
            sheetBoards[i].load( name + ".board");
            sheetBoards[i].flip( isRotated, isRotated );
        }
        game.board.combine(mapSettings.getBoardWidth(), mapSettings.getBoardHeight(),
        mapSettings.getMapWidth(), mapSettings.getMapHeight(), sheetBoards);
    }
    
    /**
     * Deploys an entity near a selected point on the board.
     *
     * @param entity the entity to deploy
     * @param pos the point to deploy near
     * @param towards another point that the deployed mechs will face towards
     */
    private boolean deploy(Entity entity, Coords pos, Coords towards, int recurse) {
        if (game.board.contains(pos) && game.getFirstEntity(pos) == null
        && !entity.isHexProhibited(game.board.getHex(pos))) {
            placeEntity(entity, pos, pos.direction(towards));
            return true;
        }
        
        // if pos is filled, try some different positions
        for (int j = 0; j < recurse; j++) {
            for (int i = 0; i < 6; i++) {
                Coords deployPos = pos.translated(i);
                if (deploy(entity, deployPos, towards, j)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Places a mech on the board
     */
    private void placeEntity(Entity entity, Coords pos, int facing) {
        entity.setPosition(pos);
        entity.setFacing(facing);
        entity.setSecondaryFacing(facing);
    }
    
    /**
     * Returns the starting point for the specified player
     */
    private Coords getStartingCoords(int startingPos) {
        switch (startingPos) {
            default :
            case 0 :
                return new Coords(1, 1);
            case 1 :
                return new Coords(game.board.width / 2, 1);
            case 2 :
                return new Coords(game.board.width - 2, 1);
            case 3 :
                return new Coords(game.board.width - 2, game.board.height / 2);
            case 4 :
                return new Coords(game.board.width - 2, game.board.height - 2);
            case 5 :
                return new Coords(game.board.width / 2, game.board.height - 2);
            case 6 :
                return new Coords(1, game.board.height - 2);
            case 7 :
                return new Coords(1, game.board.height / 2);
        }
    }

    /**
       Set up the teams vector.  Each player on a team (Team 1 .. Team X) is
       placed in the appropriate vector.  Any player on 'No Team', is placed
       in their own object
    */
    private void setupTeams()
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
        game.incrementRoundCount();

        // Roll for initative on the teams.
        TurnOrdered.rollInitiative(game.getTeamsVector());

        transmitAllPlayerUpdates();
    }

    private void determineTurnOrder() {

        // Reset all of the turn counts

        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            player.resetTankCount();
            player.resetInfantryCount();
            player.resetMechCount();
        }

        // Go through all entities, and update the player objects
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();
            if (entity.isSelectable()) {
                final Player player = entity.getOwner();
                if ( entity instanceof Infantry )
                    player.incrementInfantryCount();
                else if (entity instanceof Tank )
                    player.incrementTankCount();
                else       
                    player.incrementMechCount();
            }
        }

        // Go through each team, update the turn count,
        // and then generate the team order on each team.
        boolean infLast = game.getOptions().booleanOption("inf_move_last");
        TurnVectors team_order;
        for (Enumeration t = game.getTeams(); t.hasMoreElements(); ) {
            final Team team = (Team)t.nextElement();
            team.updateTurnCount();
            team.determineTeamOrder(infLast);
        }

        // Now, generate the order that the teams go in
        team_order = TurnOrdered.generateTurnOrder(game.getTeamsVector(),
                                                   infLast);

        // Now, we have the order that the teams
        // go in, and the order team goes in.
        Vector turns = new Vector(team_order.infantry.size() +
                                  team_order.non_infantry.size() );

        // First, the non_infantry.  We will do this by looking at
        // first element on the team_order vector, then looking at
        // the first element on THAT team's order list.  We will create
        // a new turn, then remove the entry from the team's order list and
        // the uber-turn-order list here.
        while ( !team_order.non_infantry.isEmpty() ) {
            GameTurn turn = null;
            Team t = (Team)team_order.non_infantry.firstElement();
            Player p = (Player)t.getTurnOrder().non_infantry.firstElement();

            if (infLast) {
                turn = new GameTurn.NotInfantryTurn(p.getId());
            } else {
                turn = new GameTurn(p.getId());
            }
            turns.addElement(turn);

            // Now, remove the entry
            t.getTurnOrder().non_infantry.removeElement(p);
            team_order.non_infantry.removeElement(t);
        }

        // Now, repeat for the infantry
        while ( !team_order.infantry.isEmpty() ) {
            GameTurn turn = null;
            Team t = (Team)team_order.infantry.firstElement();
            Player p = (Player)t.getTurnOrder().infantry.firstElement();

            turn = new GameTurn.OnlyInfantryTurn(p.getId());
            turns.addElement(turn);

            // Now, remove the entry
            t.getTurnOrder().infantry.removeElement(p);
            team_order.infantry.removeElement(t);

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
    private void writeInitiativeReport() {
        // write to report
        if (game.hasDeployed()) {
            roundReport.append("\nInitiative Phase for Round #").append(game.getRoundCount());
        }
        else {
            roundReport.append("\nInitiative Phase for Deployment");
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

        roundReport.append("\nThe turn order is:\n  ");
        boolean firstTurn = true;
        for (Enumeration i = game.getTurns(); i.hasMoreElements();) {
            GameTurn turn = (GameTurn)i.nextElement();
            roundReport.append((firstTurn ? "" : ", ") ).append( getPlayer(turn.getPlayerNum()).getName());
            firstTurn = false;
        }
        roundReport.append("\n\n");
        roundReport.append("  Wind direction is "+game.getStringWindDirection()+"\n");
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
        switch (phase) {
            case Game.PHASE_FIRING :
                return isEligibleForFiring(entity, phase);
            case Game.PHASE_PHYSICAL :
                if (entity instanceof Mech) {
                    return isEligibleForPhysical(entity, phase);
                }
                else {
                    return false;
                }
            default:
                return true;
        }
    }
    
    /**
     * An entity is eligible if its to-hit number is anything but impossible.
     * This is only really an issue if friendly fire is turned off.
     */
    private boolean isEligibleForFiring(Entity entity, int phase) {
        // if you're charging, no shooting
        if (entity.isUnjammingRAC()) return false;
        if (entity.isCharging() || entity.isMakingDfa()) {
            return false;
        }
        
        //        // check game options
        //        if (!game.getOptions().booleanOption("skip_ineligable_firing")) {
        //            return true;
        //        }
        
        // TODO: check for any weapon attacks
        
        return true;
    }
    
    /**
     * Check if the entity has any valid targets for physical attacks.
     */
    private boolean isEligibleForPhysical(Entity entity, int phase) {
        boolean canHit = false;
        boolean friendlyFire = game.getOptions().booleanOption("friendly_fire");
        
        // dead mek walking
        if (!entity.isActive()) return false;
        
        // if you're charging or finding a club, it's already declared
        if (entity.isUnjammingRAC()) return false;
        if (entity.isCharging() || entity.isMakingDfa() || entity.isFindingClub()) {
            return false;
        }
        
        // check game options
        if (!game.getOptions().booleanOption("skip_ineligable_physical")) {
            return true;
        }

        // Try to find a valid entity target.
        Enumeration e = game.getEntities();
        while ( !canHit && e.hasMoreElements() ) {
            Entity target = (Entity)e.nextElement();
            
            // don't shoot at friendlies unless you are into that sort of thing
            // and do not shoot yourself even then
            if (!(entity.isEnemyOf(target) || (friendlyFire && entity.getId() != target.getId() ))) {
                continue;
            }

            // No physical attack works at distances > 1.
            if ( target.getPosition() != null &&
                 entity.getPosition().distance(target.getPosition()) > 1 ) {
                continue;
            }

            canHit |= Compute.toHitPunch
                ( game, entity.getId(), target,
                 PunchAttackAction.LEFT ).getValue()
                != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitPunch
                ( game, entity.getId(), target,
                 PunchAttackAction.RIGHT ).getValue()
                != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitKick
                ( game, entity.getId(), target,
                 KickAttackAction.LEFT ).getValue()
                != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitKick
                ( game, entity.getId(), target,
                 KickAttackAction.RIGHT ).getValue()
                != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitBrushOff
                ( game, entity.getId(), target,
                  BrushOffAttackAction.LEFT ).getValue()
                != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitBrushOff
                ( game, entity.getId(), target,
                  BrushOffAttackAction.RIGHT ).getValue()
                != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitThrash
                ( game, entity.getId(), target ).getValue()
                != ToHitData.IMPOSSIBLE;

        }

        // If there are no valid Entity targets, check for add valid buildings.
        Enumeration bldgs = game.board.getBuildings();
        while ( !canHit && bldgs.hasMoreElements() ) {
            final Building bldg = (Building) bldgs.nextElement();

            // Walk through the hexes of the building.
            Enumeration hexes = bldg.getCoords();
            while ( !canHit && hexes.hasMoreElements() ) {
                final Coords coords = (Coords) hexes.nextElement();

                // No physical attack works at distances > 1.
                if ( entity.getPosition().distance(coords) > 1 ) {
                    continue;
                }

                // Can the entity target *this* hex of the building?
                final BuildingTarget target = new BuildingTarget( coords,
                                                                  game.board,
                                                                  false );

                canHit |= Compute.toHitPunch
                    ( game, entity.getId(), target,
                      PunchAttackAction.LEFT ).getValue()
                    != ToHitData.IMPOSSIBLE;

                canHit |= Compute.toHitPunch
                    ( game, entity.getId(), target,
                      PunchAttackAction.RIGHT ).getValue()
                    != ToHitData.IMPOSSIBLE;

                canHit |= Compute.toHitKick
                    ( game, entity.getId(), target,
                      KickAttackAction.LEFT ).getValue()
                    != ToHitData.IMPOSSIBLE;

                canHit |= Compute.toHitKick
                    ( game, entity.getId(), target,
                      KickAttackAction.RIGHT ).getValue()
                    != ToHitData.IMPOSSIBLE;

                canHit |= Compute.toHitBrushOff
                    ( game, entity.getId(), target,
                      BrushOffAttackAction.LEFT ).getValue()
                    != ToHitData.IMPOSSIBLE;

                canHit |= Compute.toHitBrushOff
                    ( game, entity.getId(), target,
                      BrushOffAttackAction.RIGHT ).getValue()
                    != ToHitData.IMPOSSIBLE;

                canHit |= Compute.toHitThrash
                    ( game, entity.getId(), target ).getValue()
                    != ToHitData.IMPOSSIBLE;

            } // Check the next hex of the building

        } // Check the next building

        return canHit;
    }

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
            System.err.println( unloaded.getDisplayName() + " is not an entity." );//killme
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
     */
    private void receiveMovement(Packet packet, int connId) {
        Entity entity = game.getEntity(packet.getIntValue(0));
        MovementData md = (MovementData)packet.getObject(1);
        
        // is this the right phase?
        if (game.getPhase() != Game.PHASE_MOVEMENT) {
            System.err.println("error: server got movement packet in wrong phase");
            return;
        }
        
        // can this player/entity act right now?
        if (!game.getTurn().isValid(connId, entity)) {
            System.err.println("error: server got invalid movement packet");
            return;
        }
        
        // looks like mostly everything's okay
        processMovement(entity, md);

        // Notify the clients about any building updates.
        applyAffectedBldgs();

        // This entity's turn is over.
        // N.B. if the entity fell, a *new* turn has already been added.
        endCurrentTurn(entity);
    }

    /**
     * Steps thru an entity movement packet, executing it.
     */
    private void processMovement(Entity entity, MovementData md) {
        // check for fleeing
        if (md.contains(MovementData.STEP_FLEE)) {
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
                game.removeEntity( swarmerId, Entity.REMOVE_IN_RETREAT );
                send( createRemoveEntityPacket(swarmerId,
                                               Entity.REMOVE_IN_RETREAT) );
            }
            game.removeEntity( entity.getId(), Entity.REMOVE_IN_RETREAT );
            send( createRemoveEntityPacket(entity.getId(),
                                           Entity.REMOVE_IN_RETREAT) );
            return;
        }
        
        if (md.contains(MovementData.STEP_EJECT)) {
            phaseReport.append("\n" ).append( entity.getDisplayName()).append( " ejects.\n");
            phaseReport.append(destroyEntity(entity, "ejection"));

            // Is the unit being swarmed?
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );
                swarmer.setSwarmTargetId( Entity.NONE );
                entity.setSwarmAttackerId( Entity.NONE );
                phaseReport.append( swarmer.getDisplayName() );
                phaseReport.append( " ends its swarm attack.\n" );
                this.entityUpdate( swarmerId );
            }

            // Now remove the unit that ejected.
            game.removeEntity( entity.getId(), Entity.REMOVE_SALVAGEABLE );
            send(createRemoveEntityPacket(entity.getId(), Entity.REMOVE_SALVAGEABLE));
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
        boolean firstStep;
        boolean wasProne;
        boolean fellDuringMovement;
        int prevFacing = curFacing;
        Hex prevHex = null;
        final boolean isInfantry = (entity instanceof Infantry);
        AttackAction charge = null;
        
        // Compile the move
        Compute.compile(game, entity.getId(), md);

        // check for MASC failure
        if (entity instanceof Mech) {
            if (((Mech)entity).checkForMASCFailure(phaseReport,md)) {
                // no movement after that
                return;
            }
        }
        
        // get last step's movement type
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                overallMoveType = step.getMovementType();
            }
        }
        
        // iterate through steps
        firstStep = true;
        fellDuringMovement = false;
        /* Bug 754610: Revert fix for bug 702735. */
        MovementData.Step prevStep = null;
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
            wasProne = entity.isProne();
            boolean isPavementStep = step.isOnPavement();
            
            // stop for illegal movement
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            }
            
            // check piloting skill for getting up
            if (step.getType() == MovementData.STEP_GET_UP) {
                entity.heatBuildup += 1;
                entity.setProne(false);
                wasProne = false;
                doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 0, "getting up"), true);
            } else if (firstStep) {
                // running with destroyed hip or gyro needs a check
                if (overallMoveType == Entity.MOVE_RUN && !entity.isProne()
                && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0
                || entity.hasHipCrit())) {
                    doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 0, "running with damaged hip actuator or gyro"), false);
                }
                firstStep = false;
            }
            
            // did the entity just fall?
            if (!wasProne && entity.isProne()) {
                moveType = step.getMovementType();
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                mpUsed = step.getMpUsed();
                fellDuringMovement = true;
                break;
            }
             
            if (step.getType() == MovementData.STEP_UNJAM_RAC) {
                entity.setUnjammingRAC(true);
                game.addAction(new UnjamAction(entity.getId()));

                break;
            }

            // set most step parameters
            moveType = step.getMovementType();
            distance = step.getDistance();
            mpUsed = step.getMpUsed();

            // check for charge
            if (step.getType() == MovementData.STEP_CHARGE) {
                Targetable target = step.getTarget( game );
                ChargeAttackAction caa = new ChargeAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                entity.setDisplacementAttack(caa);
                game.addCharge(caa);
                charge = caa;
                break;
            }
            
            // check for dfa
            if (step.getType() == MovementData.STEP_DFA) {
                Targetable target = step.getTarget( game );
                DfaAttackAction daa = new DfaAttackAction(entity.getId(), target.getTargetType(), target.getTargetId(), target.getPosition());
                entity.setDisplacementAttack(daa);
                game.addCharge(daa);
                charge = daa;
                break;
            }

            // set last step parameters
            curPos = step.getPosition();
            curFacing = step.getFacing();

            final Hex curHex = game.board.getHex(curPos);

            // Check for skid.
            // TODO: add check for elevation of pavement, road,
            //       or bridge matches entity elevation.
            /* Bug 754610: Revert fix for bug 702735.
               && ( prevHex.contains(Terrain.PAVEMENT) ||
                    prevHex.contains(Terrain.ROAD) ||
                    prevHex.contains(Terrain.BRIDGE) )
            */
            if ( moveType != Entity.MOVE_JUMP
                 && prevHex != null
                 && prevStep.isOnPavement()
                 && overallMoveType == Entity.MOVE_RUN
                 && prevFacing != curFacing
                 && !lastPos.equals(curPos)
                 && !isInfantry ) {

                // Have an entity-meaningful PSR message.
                boolean psrPassed = true;
                PilotingRollData psr = null;
                if ( entity instanceof Mech ) {
                    psr = new PilotingRollData
                        (entity.getId(), getMovementPSRModifier(distance),
                         "running & turning on pavement");
                    psrPassed = doSkillCheckWhileMoving( entity, lastPos,
                                                         lastPos, psr );
                } else {
                    psr = new PilotingRollData
                        (entity.getId(), getMovementPSRModifier(distance),
                         "reckless driving on pavement");
                    psrPassed = doSkillCheckWhileMoving( entity, lastPos,
                                                         lastPos, psr, false );
                }
                // Does the entity skid?
                if ( !psrPassed ){

                    curPos = lastPos;
                    Coords nextPos = curPos;
                    Hex    nextHex = null;
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
                            if ( game.getOptions().booleanOption("push_off_board") ) {
                                // Yup.  One dead entity.
                                game.removeEntity(entity.getId(),
                                                  Entity.REMOVE_IN_RETREAT);
                                send(createRemoveEntityPacket(entity.getId(),
                                                              Entity.REMOVE_IN_RETREAT));
                                phaseReport.append("*** " )
                                    .append( entity.getDisplayName() )
                                    .append( " has skidded off the field. ***\n");

                                // TODO: remove passengers and swarmers.

                                // The entity's movement is completed.
                                return;

                            } else {
                                // Nope.  Update the report.
                                phaseReport.append( "   Can't skid off the field.\n" );
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
                            Terrain land = curHex.
                                getTerrain(Terrain.WATER);
                            if ( land != null ) {
                                curElevation += land.getLevel();
                            }
                            land = nextHex.getTerrain(Terrain.WATER);
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
                                               Compute.getBasePilotingRoll
                                               (game, entity.getId()) );

                            // Stay in the current hex and stop skidding.
                            break;
                        }

                        // Get any building in the hex.
                        Building bldg = game.board.getBuildingAt(nextPos);
                        boolean bldgSuffered = false;

                        // Does the next hex contain an entities?
                        // ASSUMPTION: hurt EVERYONE in the hex.
                        // TODO: allow entities to occupy different levels of
                        //       buildings, and only skid into a single level.
                        boolean stopTheSkid = false;
                        targets = game.getEntities( nextPos );
                        while ( targets.hasMoreElements() ) {
                            target = (Entity) targets.nextElement();

                            // TODO : allow ready targets to move out of way

                            // Mechs and vehicles get charged.
                            if ( !(target instanceof Infantry) ) {

                                // Update report.
                                phaseReport.append( "   Skids into " +
                                                    target.getShortName() +
                                                    " in hex " +
                                                    nextPos.getBoardNum() +
                                                    "... " );

                                // Resolve a charge against the target.
                                // ASSUMPTION: buildings block damage for
                                //             *EACH* entity charged.
                                ToHitData toHit = new ToHitData();

                                // Calculate hit location.
                                if ( entity instanceof Tank &&
                                     entity.getMovementType() ==
                                     Entity.MovementType.HOVER &&
                                     0 < nextHex.levelOf(Terrain.WATER) ) {
                                    if ( 2 <= nextHex.levelOf(Terrain.WATER) ||
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

                                // Resolve the hit.
                                toHit.setSideTable
                                    (Compute.targetSideTable(entity, target));
                                resolveChargeDamage
                                    (entity, target, toHit, prevFacing);
                                bldgSuffered = true;

                                // The skid ends here if the target lives.
                                // TODO : we should keep skiding if the target
                                //        is pushed off the board as it has
                                //        been "destroyed" according to the
                                //        MegaMek interpretation of the rules.
                                if ( !target.isDoomed() &&
                                     !target.isDestroyed() ) {
                                    stopTheSkid = true;
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
                            int chargeDamage = Compute.getChargeDamageFor
                                ( entity );
                            if ( !bldgSuffered ) {
                                phaseReport.append( "      " )
                                    .append( damageBuilding( bldg, 
                                                             chargeDamage ) );

                                // Apply damage to the attacker.
                                int toAttacker = Compute.getChargeDamageTakenBy
                                    ( entity, bldg );
                                HitData hit = entity.rollHitLocation( ToHitData.HIT_NORMAL,
                                                                      Compute.targetSideTable(curPos, nextPos, entity.getFacing(), false)
                                                                      );
                                phaseReport.append( this.damageEntity( entity, hit, toAttacker ) )
                                    .append( "\n" );

                                curPos = nextPos;
                                entity.setPosition( curPos );
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

                        // Update the position and keep skidding.
                        curPos = nextPos;
                        entity.setPosition( curPos );
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
            if (!lastPos.equals(curPos)
            && step.getMovementType() != Entity.MOVE_JUMP
            && curHex.levelOf(Terrain.RUBBLE) > 0) {
                doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), 0, "entering Rubble"));
            }
            
            // check to see if we've moved OUT of fire and we are a mech
            if (!lastPos.equals(curPos)
            && step.getMovementType() != Entity.MOVE_JUMP
            && game.board.getHex(lastPos).contains(Terrain.FIRE)) {
                if (entity instanceof Mech) {
                    entity.heatBuildup+=2;
                    phaseReport.append("\n" ).append( entity.getDisplayName()
                    ).append( " passes through a fire.  It will generate 2 more heat this round.\n");
                }
            }   
            
            // check to see if we've moved INTO fire and we are not a mech
            if (!lastPos.equals(curPos)
            && step.getMovementType() != Entity.MOVE_JUMP
            && game.board.getHex(curPos).contains(Terrain.FIRE)) {
                if (!(entity instanceof Mech)) {
                    doFlamingDeath(entity);
                }
            }   

            // check if we've moved into water
            if (!lastPos.equals(curPos)
                && step.getMovementType() != Entity.MOVE_JUMP
                && curHex.levelOf(Terrain.WATER) > 0
                && entity.getMovementType() != Entity.MovementType.HOVER
                && !isPavementStep) {
                if (curHex.levelOf(Terrain.WATER) == 1) {
                    doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), -1, "entering Depth 1 Water"));
                } else if (curHex.levelOf(Terrain.WATER) == 2) {
                    doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), 0, "entering Depth 2 Water"));
                    // Any swarming infantry will be destroyed.
                    final int swarmerId = entity.getSwarmAttackerId();
                    if ( Entity.NONE != swarmerId ) {
                        final Entity swarmer = game.getEntity( swarmerId );
                        swarmer.setSwarmTargetId( Entity.NONE );
                        entity.setSwarmAttackerId( Entity.NONE );
                        swarmer.setPosition( curPos );
                        phaseReport.append( "   The swarming unit, " )
                            .append( swarmer.getShortName() )
                            .append( ", drowns!\n" )
                            .append( destroyEntity(swarmer,
                                                   "a watery grave", false) );
                        entityUpdate( swarmerId );
                    }
                } else {
                    doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), 1, "entering Depth 3+ Water"));
                    // Any swarming infantry will be destroyed.
                    final int swarmerId = entity.getSwarmAttackerId();
                    if ( Entity.NONE != swarmerId ) {
                        final Entity swarmer = game.getEntity( swarmerId );
                        swarmer.setSwarmTargetId( Entity.NONE );
                        entity.setSwarmAttackerId( Entity.NONE );
                        swarmer.setPosition( curPos );
                        phaseReport.append( "   The swarming unit, " )
                            .append( swarmer.getShortName() )
                            .append( ", drowns!\n" )
                            .append( destroyEntity(swarmer,
                                                   "a watery grave", false) );
                        entityUpdate( swarmerId );
                    }
                }
                
                // check for inferno wash-off
                checkForWashedInfernos(entity, curPos);
            }

            // Handle loading units.
            if ( step.getType() == MovementData.STEP_LOAD ) {

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
                             !loaded.isSelectable() ) {
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
            if ( step.getType() == MovementData.STEP_UNLOAD ) {
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
            if ( !lastPos.equals(curPos) &&
                 step.getMovementType() != Entity.MOVE_JUMP &&
                 ( curHex.contains(Terrain.BUILDING) ||
                   (prevHex != null && prevHex.contains(Terrain.BUILDING)) ) &&
                 !(entity instanceof Infantry) ) {

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
            if (step.getType() == MovementData.STEP_GO_PRONE) {
                mpUsed = step.getMpUsed();
                entity.setProne(true);
                // check to see if we washed off infernos
                checkForWashedInfernos(entity, curPos);
                break;
            }
            
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
        }
        
        // set entity parameters
        entity.setPosition(curPos);
        entity.setFacing(curFacing);
        entity.setSecondaryFacing(curFacing);
        entity.delta_distance = distance;
        entity.moved = moveType;
        entity.mpUsed = mpUsed;

        // but the danger isn't over yet!  landing from a jump can be risky!
        if (overallMoveType == Entity.MOVE_JUMP && !entity.isMakingDfa()) {
            // check for damaged criticals
            if (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0 || entity.hasLegActuatorCrit()) {
                doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 0, "landing with damaged leg actuator or gyro"), false);
            }
            // jumped into water?
            int waterLevel = game.board.getHex(curPos).levelOf(Terrain.WATER);
            if (waterLevel == 1) {
                doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), -1, "entering Depth 1 Water"), false);
            } else if (waterLevel == 2) {
                doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 0, "entering Depth 2 Water"), false);
                // Any swarming infantry will be destroyed.
                final int swarmerId = entity.getSwarmAttackerId();
                if ( Entity.NONE != swarmerId ) {
                    final Entity swarmer = game.getEntity( swarmerId );
                    swarmer.setSwarmTargetId( Entity.NONE );
                    entity.setSwarmAttackerId( Entity.NONE );
                    swarmer.setPosition( curPos );
                    phaseReport.append( "   The swarming unit, " )
                        .append( swarmer.getShortName() )
                        .append( ", drowns!\n" )
                        .append( destroyEntity(swarmer,
                                               "a watery grave", false) );
                    entityUpdate( swarmerId );
                }
            } else if (waterLevel >= 3) {
                doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 1, "entering Depth 3+ Water"), false);
                // Any swarming infantry will be destroyed.
                final int swarmerId = entity.getSwarmAttackerId();
                if ( Entity.NONE != swarmerId ) {
                    final Entity swarmer = game.getEntity( swarmerId );
                    swarmer.setSwarmTargetId( Entity.NONE );
                    entity.setSwarmAttackerId( Entity.NONE );
                    swarmer.setPosition( curPos );
                    phaseReport.append( "   The swarming unit, " )
                        .append( swarmer.getShortName() )
                        .append( ", drowns!\n" )
                        .append( destroyEntity(swarmer,
                                               "a watery grave", false) );
                    entityUpdate( swarmerId );
                }
            }

            // If the entity is being swarmed, jumping may dislodge the fleas.
            final int swarmerId = entity.getSwarmAttackerId();
            if ( Entity.NONE != swarmerId ) {
                final Entity swarmer = game.getEntity( swarmerId );
                final PilotingRollData roll =
                    Compute.getBasePilotingRoll(game, entity.getId());

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
                    phaseReport.append("succeeds.\n");
                    entity.setSwarmAttackerId( Entity.NONE );
                    swarmer.setSwarmTargetId( Entity.NONE );
                    // Did the infantry fall into water?
                    final Hex curHex = game.board.getHex(curPos);
                    if ( curHex.levelOf(Terrain.WATER) > 0 ) {
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

        // should we give another turn to the entity to keep moving?
        if (fellDuringMovement && entity.mpUsed < entity.getRunMP() 
        && entity.isSelectable() && !entity.isDoomed()) {
            entity.applyDamage();
            entity.setDone(false);
            GameTurn newTurn = new GameTurn.SpecificEntityTurn(entity.getOwner().getId(), entity.getId());
            game.insertNextTurn(newTurn);
            // brief everybody on the turn update
            send(createTurnVectorPacket());
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
                 game.board.getHex(curPos).contains(Terrain.FIRE) ) {
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
            entityUpdate( entity.getId() );
        }
        
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
     * Checks to see if we may have just washed off infernos.  Call after
     * a step which may have done this.
     */
    void checkForWashedInfernos(Entity entity, Coords coords) {
        Hex hex = game.board.getHex(coords);
        int waterLevel = hex.levelOf(Terrain.WATER);
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
     */
    void washInferno(Entity entity, Coords coords) {
        game.board.addInfernoTo( coords, InfernoTracker.STANDARD_ROUND, 1 );
        entity.infernos.clear();

        // Start a fire in the hex?
        Hex hex = game.board.getHex(coords);
        phaseReport.append( " Inferno removed from " )
            .append( entity.getDisplayName() );
        if ( hex.contains(Terrain.FIRE) ) {
            phaseReport.append( ".\n" );
        } else {
            phaseReport.append( " and fire started in hex!\n" );
            hex.addTerrain(new Terrain(Terrain.FIRE, 1));
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
            } else if (entity.moved == Entity.MOVE_RUN) {
                entity.heatBuildup += 2;
            } else if (entity.moved == Entity.MOVE_JUMP) {
                entity.heatBuildup += Math.max(3, entity.delta_distance);
            }
        }
    }

    /**
     * Do a piloting skill check while standing still (during the movement phase).
     * We have a special case for getting up because quads need not roll to stand
     * if they have no damaged legs.  If a quad is short a gyro, however....
     */
    private void doSkillCheckInPlace(Entity entity, PilotingRollData reason, boolean gettingUp) {
        // non-mechs should never get here
        if (! (entity instanceof Mech) || entity.isProne()) {
            return;
        }
        
        if (gettingUp && !entity.needsRollToStand() && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) < 2)) {
            phaseReport.append("\n" ).append( entity.getDisplayName() ).append( " does not need to make "
            ).append( "a piloting skill check to stand up because it has all four of its legs.");
            return;
        }
        final PilotingRollData roll = Compute.getBasePilotingRoll(game, entity.getId());
        
        // append the reason modifier
        roll.append(reason);
        
        // okay, print the info
        phaseReport.append("\n" ).append( entity.getDisplayName()
        ).append( " must make a piloting skill check (" ).append( reason.getPlainDesc() ).append( ")"
        ).append( ".\n");
        // roll
        final int diceRoll = Compute.d6(2);
        phaseReport.append("Needs " ).append( roll.getValueAsString()
        ).append( " [" ).append( roll.getDesc() ).append( "]"
        ).append( ", rolls " ).append( diceRoll ).append( " : ");
        if (diceRoll < roll.getValue()) {
            phaseReport.append("falls.\n");
            doEntityFall(entity, roll);
        } else {
            phaseReport.append("succeeds.\n");
        }
        
    }
    
    /**
     * Do a piloting skill check for a Mech while it is moving.
     * Failing this roll will cause the Mech to fall.
     *
     * @param   entity - the <code>Entity</code> object for the Mech.
     * @param   src - the <code>Coords</code> the Mech is moving from.
     * @param   dest - the <code>Coords</code> the Mech is moving to.
     *          This value can be the same as src for in-place checks.
     * @param   reason - the <code>PilotingRollData</code> that is causing
     *          this check.
     * @return <code>true</code> if the pilot passes the skill check.
     */
    private boolean doSkillCheckWhileMoving( Entity entity,
                                             Coords src,
                                             Coords dest,
                                             PilotingRollData reason ) {

        // Non mechs should never get here.
        if ( !(entity instanceof Mech) ) {
            return true;
        }

        return doSkillCheckWhileMoving( entity, src, dest, reason, true );
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
                                             PilotingRollData reason,
                                             boolean isFallRoll ) {
        boolean result = true;
        
        final PilotingRollData roll =
            Compute.getBasePilotingRoll(game, entity.getId());
        boolean fallsInPlace;
        
        // append the reason modifier
        roll.append(reason);
        
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
            .append( reason.getPlainDesc() )
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
     */
    private void doEntityFallsInto(Entity entity, Coords src, Coords dest, PilotingRollData roll) {
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final int fallElevation = Math.abs(destHex.floor() - srcHex.floor());
        int direction = src.direction(dest);
        // check entity in target hex
        Entity violation = Compute.stackingViolation(game, entity.getId(), dest);
        // check if we can fall in that hex
        if (violation != null
        && !Compute.isValidDisplacement(game, violation.getId(), src, dest)) {
            // if target can't be displaced, fall in source hex.
            // NOTE: source hex should never contain a non-displacable entity
            Coords temp = dest;
            dest = src;
            src = temp;
            violation = Compute.stackingViolation(game, entity.getId(), dest);
        }
        
        // falling mech falls
        phaseReport.append(entity.getDisplayName() ).append( " falls "
        ).append( fallElevation ).append( " level(s) into hex "
        ).append( dest.getBoardNum() ).append( ".\n");
        
        // if hex was empty, deal damage and we're done
        if (violation == null) {
            doEntityFall(entity, dest, fallElevation, roll);
            return;
        }
        
        // hmmm... somebody there... problems.
        if (fallElevation >= 2) {
            // accidental death from above
        } else {
            // damage as normal
            doEntityFall(entity, dest, fallElevation, roll);
            // target gets displaced
            doEntityDisplacement(violation, dest, dest.translated(direction), new PilotingRollData(violation.getId(), 0, "domino effect"));
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
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final int direction = src.direction(dest);
        // Handle null hexes.
        if ( srcHex == null || destHex == null ) {
            System.err.println( "Can not displace " + entity.getShortName() +
                                " from " + src + 
                                " to " + dest + "." );
        }
        int fallElevation = entity.elevationOccupied(srcHex) - entity.elevationOccupied(destHex);
        Entity violation =Compute.stackingViolation(game, entity.getId(), dest);
        
        // can't fall upwards
        if (fallElevation < 0) {
            fallElevation = 0;
        }
        
        // if destination is empty, this could be easy...
        if (violation == null) {
            if (fallElevation < 2) {
                // no cliff: move and roll normally
                phaseReport.append(entity.getDisplayName()
                ).append( " is displaced into hex "
                ).append( dest.getBoardNum() ).append( ".\n");
                entity.setPosition(dest);
                if (roll != null) {
                    game.addPSR(roll);
                }

                // Update the entity's postion on the client.
                entityUpdate( entity.getId() );
                return;
            } else {
                // cliff: fall off it, deal damage, prone immediately
                phaseReport.append(entity.getDisplayName() ).append( " falls "
                ).append( fallElevation ).append( " levels into hex "
                ).append( dest.getBoardNum() ).append( ".\n");
                // only given a modifier, so flesh out into a full piloting roll
                PilotingRollData pilotRoll = Compute.getBasePilotingRoll(game, entity.getId());
                if (roll != null) {
                    pilotRoll.append(roll);
                }
                doEntityFall(entity, dest, fallElevation, pilotRoll);
                return;
            }
        }
        
        // okay, destination occupied.  hmmm...
        System.err.println("server.doEntityDisplacement: destination occupied");
        if (fallElevation < 2) {
            // domino effect: move & displace target
            phaseReport.append(entity.getDisplayName()
            ).append( " is displaced into hex "
            ).append( dest.getBoardNum() ).append( ", violating stacking with "
            ).append( violation.getDisplayName() ).append( ".\n");
            entity.setPosition(dest);
            if (roll != null) {
                game.addPSR(roll);
            }
            doEntityDisplacement(violation, dest, dest.translated(direction), new PilotingRollData(violation.getId(), 0, "domino effect"));
            // Update the violating entity's postion on the client.
            entityUpdate( violation.getId() );
            return;
        } else {
            // accidental fall from above: havoc!
            phaseReport.append(entity.getDisplayName() ).append( " falls "
            ).append( fallElevation ).append( " levels into hex "
            ).append( dest.getBoardNum() ).append( ", violating stacking with "
            ).append( violation.getDisplayName() ).append( ".\n");
            
            // determine to-hit number
            ToHitData toHit = new ToHitData(7, "base");
            toHit.append(Compute.getTargetMovementModifier(game, violation.getId()));
            toHit.append(Compute.getTargetTerrainModifier(game, violation));
            
            // roll dice
            final int diceRoll = Compute.d6(2);
            phaseReport.append("Collision occurs on a " ).append( toHit.getValue()
            ).append( " or greater.  Rolls " ).append( diceRoll);
            if (diceRoll >= toHit.getValue()) {
                phaseReport.append(", hits!\n");
                // deal damage to target
                int damage = (int)Math.ceil(entity.getWeight() / 10.0);
                phaseReport.append(violation.getDisplayName() ).append( " takes "
                ).append( damage ).append( " from the collision.");
                while (damage > 0) {
                    int cluster = Math.min(5, damage);
                    HitData hit = violation.rollHitLocation(ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT);
                    phaseReport.append(damageEntity(violation, hit, cluster));
                    damage -= cluster;
                }
                phaseReport.append("\n");
                
                // attacker falls as normal, on his back
                // only given a modifier, so flesh out into a full piloting roll
                PilotingRollData pilotRoll = Compute.getBasePilotingRoll(game, entity.getId());
                pilotRoll.append(roll);
                doEntityFall(entity, dest, fallElevation, 3, pilotRoll);
                
                // defender pushed away, or destroyed
                Coords targetDest = Compute.getValidDisplacement(game, violation.getId(), dest, direction);
                if (targetDest != null) {
                    doEntityDisplacement(violation, dest, targetDest, new PilotingRollData(violation.getId(), 2, "fallen on"));
                    // Update the violating entity's postion on the client.
                    entityUpdate( violation.getId() );
                } else {
                    // ack!  automatic death!  Tanks
                    // suffer an ammo/power plant hit.
                    // TODO : a Mech suffers a Head Blown Off crit.
                    phaseReport.append(destroyEntity(violation, "impossible displacement", (violation instanceof Mech), (violation instanceof Mech)));
                }
            } else {
                phaseReport.append(", misses.\n");
                //TODO: this is not quite how the rules go
                Coords targetDest = Compute.getValidDisplacement(game, entity.getId(), dest, direction);
                if (targetDest != null) {
                    doEntityDisplacement(entity, src, targetDest, new PilotingRollData(entity.getId(), PilotingRollData.IMPOSSIBLE, "pushed off a cliff"));
                    // Update the entity's postion on the client.
                    entityUpdate( entity.getId() );
                } else {
                    // ack!  automatic death!  Tanks
                    // suffer an ammo/power plant hit.
                    // TODO : a Mech suffers a Head Blown Off crit.
                    phaseReport.append(destroyEntity(entity, "impossible displacement", (entity instanceof Mech), (entity instanceof Mech)));
                }
            }
            return;
        }
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
        if (!game.getTurn().isValid(connId, entity)
        || !game.board.isLegalDeployment(coords, entity.getOwner())) {
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
        entityUpdate(entity.getId());
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
        && game.getPhase() != Game.PHASE_PHYSICAL) {
            System.err.println("error: server got attack packet in wrong phase");
            return;
        }
        
        // can this player/entity act right now?
        if (!game.getTurn().isValid(connId, entity)) {
            System.err.println("error: server got invalid attack packet");
            return;
        }
        
        // looks like mostly everything's okay
        processAttack(entity, vector);
        endCurrentTurn(entity);
    }
    
    /**
     * Process a batch of entity attack (or twist) actions by adding them to
     * the proper list to be processed later.
     */
    private void processAttack(Entity entity, Vector vector) {
        for (Enumeration i = vector.elements(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            
            // is this the right entity?
            if (ea.getEntityId() != entity.getId()) {
                System.err.println("error: attack packet has wrong attacker");
                continue;
            }
            
            if (ea instanceof PushAttackAction) {
                // push attacks go the end of the displacement attacks
                PushAttackAction paa = (PushAttackAction)ea;
                entity.setDisplacementAttack(paa);
                game.addCharge(paa);
            } else {
                // add to the normal attack list.
                game.addAction(ea);
            }
        }
        // this entity is done for the round
        entity.setDone(true);
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
        roundReport.append("\nWeapon Attack Phase\n-------------------\n");
        
        // loop thru actions and handle everything we expect except attacks
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            Entity entity = game.getEntity(ea.getEntityId());
            if (ea instanceof TorsoTwistAction) {
                TorsoTwistAction tta = (TorsoTwistAction)ea;
                entity.setSecondaryFacing(tta.getFacing());
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
        }
    }
    
    /** Called during the fire phase to resolve all (and only) weapon attacks 
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
        Hex curHex = game.board.getHex( coords );

        // Is there the rubble of a medium, heavy,
        // or hardened building in the hex?
        if ( Building.LIGHT < curHex.levelOf( Terrain.RUBBLE ) ) {

            // Finding a club is not guaranteed.  The chances are
            // based on the type of building that produced the
            // rubble.
            boolean found = false;
            int roll = Compute.d6(2);
            switch ( curHex.levelOf( Terrain.RUBBLE ) ) {
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
                clubType = EquipmentType.getByInternalName("Girder Club");
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
        else if ( 1 <= curHex.levelOf( Terrain.WOODS ) ) {
            clubType = EquipmentType.getByInternalName("Tree Club");
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
     * Generated by a first pass through the weapon attack list.
     */
    private class WeaponResult {
        public WeaponAttackAction waa = null;
        public ToHitData toHit = null; // stored before ammo depletion, jams
        public int roll = -1;
        public boolean revertsToSingleShot = false;
        public int amsShotDown = 0;
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
        if (weapon.isUsedThisRound()) {
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
        wr.toHit = Compute.toHitWeapon(game, waa);
        
        // roll dice
        wr.roll = Compute.d6(2);
        
        // if the shot is possible and not a streak miss, add heat and use ammo
        streakMiss = (wtype.getAmmoType() == AmmoType.T_SRM_STREAK && wr.roll < wr.toHit.getValue());
        if (wr.toHit.getValue() != TargetRoll.IMPOSSIBLE && !streakMiss) {
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
        if (vCounters == null) {
            return wr;
        } 
        
        // resolve AMS counter-fire
        for (int x = 0; x < vCounters.size(); x++) {
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

            wr.amsShotDown += amsHits;
        }
        
        return wr;
    }
    
    private boolean tryIgniteHex(Coords c, boolean bInferno, int nTargetRoll) {

        Hex hex = game.board.getHex(c);                                 
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
        if ( hex.contains( Terrain.FIRE ) ) {
            return true;
        }
        else if (ignite(hex, nTargetRoll, bAnyTerrain)) {
            phaseReport.append("           The hex ignites!\n");
            sendChangedHex(c);
            return true;
        }
        return false;
    }
    
    private boolean tryClearHex(Coords c, int nTarget) {
        
        Hex h = game.board.getHex(c);
        int woodsRoll = Compute.d6(2);
        phaseReport.append("    Checking to clear woods; needs " ).append( nTarget )
                .append( ", rolls " ).append( woodsRoll ).append( ": ");

        if(woodsRoll >= nTarget) {
            int woods = h.levelOf(Terrain.WOODS);
            if(woods > 1) {
                 h.removeTerrain(Terrain.WOODS);
                 h.addTerrain(new Terrain(Terrain.WOODS, woods - 1));
                 phaseReport.append(" Heavy Woods converted to Light Woods!\n");
            }
            else if(woods == 1) {
                 h.removeTerrain(Terrain.WOODS);
                 h.addTerrain(new Terrain(Terrain.ROUGH, 1));
                 phaseReport.append(" Light Woods converted to Rough!\n");
            }
            sendChangedHex(c);
            return true;
        } else {
            phaseReport.append(" fails!\n");
            return false;
        }
    }


    /**
     * Resolve a single Weapon Attack object
     */
    private void resolveWeaponAttack(WeaponResult wr, int lastEntityId) {
        final Entity ae = game.getEntity(wr.waa.getEntityId());
        final Targetable target = game.getTarget(wr.waa.getTargetType(), wr.waa.getTargetId());
        Entity entityTarget = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            entityTarget = (Entity)target;
        }
        final Mounted weapon = ae.getEquipment(wr.waa.getWeaponId());
        final WeaponType wtype = (WeaponType)weapon.getType();
        final boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        // 2002-09-16 Infantry weapons have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !isWeaponInfantry;
        final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        final AmmoType atype = ammo == null ? null : (AmmoType)ammo.getType();
        Infantry platoon = null;
        final boolean isBattleArmorAttack = wtype.hasFlag(WeaponType.F_BATTLEARMOR);
        ToHitData toHit = wr.toHit;
        boolean bInferno = (usesAmmo && atype.getMunitionType() == AmmoType.M_INFERNO);
        if (!bInferno) {
            // also check for inferno infantry
            bInferno = (isWeaponInfantry && wtype.hasFlag(WeaponType.F_INFERNO));
        }
        final boolean targetInBuilding = 
            Compute.isInBuilding( game, entityTarget );

        // Which building takes the damage?
        Building bldg = game.board.getBuildingAt( target.getPosition() );
        
        if (lastEntityId != ae.getId()) {
            phaseReport.append("\nWeapons fire for " ).append( ae.getDisplayName() ).append( "\n");
        }

        // Swarming infantry can stop during any weapons phase after start.
        if ( Infantry.STOP_SWARM.equals( wtype.getInternalName() ) ) {
            // ... but only as their *only* attack action.
            if ( toHit.getValue() == ToHitData.IMPOSSIBLE ) {
                phaseReport.append( "Swarm attack can not be ended (" +
                                    toHit.getDesc() ).append( ")\n" );
                return;
            } else {
                phaseReport.append( "Swarm attack ended.\n" );
                // Only apply the "stop swarm 'attack'" to the swarmed Mek.
                if ( ae.getSwarmTargetId() != target.getTargetId() ) {
                    Entity other = game.getEntity( ae.getSwarmTargetId() );
                    other.setSwarmAttackerId( Entity.NONE );
                } else {
                    entityTarget.setSwarmAttackerId( Entity.NONE );
                }
                ae.setSwarmTargetId( Entity.NONE );
                return;
            }
        }

        // Report weapon attack and its to-hit value.
        phaseReport.append("    " ).append( wtype.getName() ).append( " at " ).append( target.getDisplayName());
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the shot is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            phaseReport.append(", the shot is an automatic miss (" ).append( toHit.getDesc() ).append( "), ");
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the shot is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
        } else {
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
        }
        
        // if firing an HGR unbraced, schedule a PSR
        if (wtype.getAmmoType() == AmmoType.T_GAUSS_HEAVY && ae.mpUsed > 0) {
            // the mod is weight-based
            int nMod;
            if (ae.getWeight() <= Mech.WEIGHT_LIGHT) {
                nMod = 2;
            } else if (ae.getWeight() <= Mech.WEIGHT_MEDIUM) {
                nMod = 1;
            } else if (ae.getWeight() <= Mech.WEIGHT_HEAVY) {
                nMod = 0;
            } else {
                nMod = -1;
            }
            PilotingRollData psr = new PilotingRollData(ae.getId(), nMod, "fired HeavyGauss unbraced");
            psr.setCumulative(false); // about the only time we set this flag
            game.addPSR(psr);
        }
        
        // dice have been rolled, thanks
        phaseReport.append("rolls " ).append( wr.roll ).append( " : ");
        
        // check for AC jams
        int nShots = weapon.howManyShots();
        if (nShots > 1) {
            int jamCheck = 0;
            if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA && weapon.curMode().equals("Ultra")) {
                jamCheck = 2;
            } else if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                if (nShots == 2) {
                    jamCheck = 2;
                }
                else if (nShots == 4) {
                    jamCheck = 3;
                }
                else if (nShots == 6) {
                    jamCheck = 4;
                }
            }
        
            if (jamCheck > 0 && wr.roll <= jamCheck) {
                phaseReport.append("misses AND THE AUTOCANNON JAMS.\n");
                weapon.setJammed(true);
                // ultras are destroyed by jamming
                if (wtype.getAmmoType() == AmmoType.T_AC_ULTRA) {
                    weapon.setHit(true);
                }
                return;
            }
        }
        
        // do we hit?
        boolean bMissed = false;
        if (wr.roll < toHit.getValue()) {
            // miss
            bMissed = true;
            phaseReport.append("misses.\n"); 
            if (wr.amsShotDown > 0) {
                phaseReport.append( "\tAMS activates, firing " )
                    .append( wr.amsShotDown )
                    .append( " shot(s).\n" );
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
            if ( (usesAmmo && wr.amsShotDown >= maxMissiles) ||
                 toHit.getValue() == TargetRoll.AUTOMATIC_FAIL ||
                 wtype.getAmmoType() == AmmoType.T_SRM_STREAK ) {
                return;
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
                return;
            }
        }
        
        // special case NARC hits.  No damage, but a beacon is appended
        if (!bMissed && wtype.getAmmoType() == AmmoType.T_NARC) {

            // TODO: AMS can shoot down NARC pods too.
            if (entityTarget == null) {
                phaseReport.append("hits, but doesn't do anything.\n");
            } else {
                entityTarget.setNarcedBy(ae.getOwner().getTeam());
                phaseReport.append("hits.  Pod attached.\n");
            }
            return;
        }

        // yeech.  handle damage. . different weapons do this in very different ways
        int hits = 1, nCluster = 1, nSalvoBonus = 0;
        int nDamPerHit = wtype.getDamage();
        boolean bSalvo = false;
        // ecm check is heavy, so only do it once
        boolean bCheckedECM = false;
        boolean bECMAffected = false;
        boolean bMekStealthActive = false;
        String sSalvoType = " shot(s) ";
        boolean bAllShotsHit = false;

        // All shots fired by a Streak SRM weapon, during
        // a Mech Swarm hit, or at an adjacent building.
        if ( wtype.getAmmoType() == AmmoType.T_SRM_STREAK ||
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
            return;
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
        // the "racksize".
        else if ( isBattleArmorAttack ) {
            bSalvo = true;
            platoon = (Infantry) ae;
            nCluster = 1;
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
                nDamPerHit = atype.getDamagePerShot();
            }
            
            if ( wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_MRM || 
                 wtype.getAmmoType() == AmmoType.T_ATM ) {
                nCluster = 5;
            }

            // calculate # of missiles hitting
            if ( wtype.getAmmoType() == AmmoType.T_LRM ||
                 wtype.getAmmoType() == AmmoType.T_SRM || 
                 wtype.getAmmoType() == AmmoType.T_ATM ) {
                
                // check for artemis, else check for narc
                Mounted mLinker = weapon.getLinkedBy();
                if ( wtype.getAmmoType() == AmmoType.T_ATM ||
                     ( mLinker != null &&
                       mLinker.getType() instanceof MiscType && 
                       !mLinker.isDestroyed() && !mLinker.isMissing() &&
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
                    if (!bECMAffected && !bMekStealthActive) {
                        nSalvoBonus += 2;
                    }
                } else if (entityTarget != null && entityTarget.isNarcedBy(ae.getOwner().getTeam())) {
                    // check ECM interference
                    if (!bCheckedECM) {
                        // Attacking Meks using stealth suffer ECM effects.
                        if ( ae instanceof Mech ) {
                            bMekStealthActive = ae.isStealthActive();
                        }
                        bECMAffected = Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition());
                        bCheckedECM = true;
                    }
                    if (!bECMAffected && !bMekStealthActive) {
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

            // Large MRM missile racks roll twice.
            // MRM missiles never recieve hit bonuses.
            if ( wtype.getRackSize() == 30 || wtype.getRackSize() == 40 ) {
                hits = Compute.missilesHit(wtype.getRackSize() / 2) +
                    Compute.missilesHit(wtype.getRackSize() / 2);
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
                        hits += Compute.missilesHit( 20 );
                        temp -= 20;
                    }
                    hits += Compute.missilesHit( temp );
                } // End not-all-shots-hit
            }

            // If all shots hit, use the full racksize.
            else if ( bAllShotsHit ) {
                hits = wtype.getRackSize();
            }

            // In all other circumstances, roll for hits.
            else {
                hits = Compute.missilesHit(wtype.getRackSize(), nSalvoBonus);
            }

            // Advanced SRM's only hit with even numbers of missles.
            if ( null != atype &&
                 atype.getAmmoType() == AmmoType.T_SRM_ADVANCED ) {
                hits = 2 * (int) Math.floor( (1.0 + (float) hits) / 2.0);
            }

        } else if (atype != null && atype.getMunitionType() == AmmoType.M_CLUSTER) {
            // Cluster shots break into single point clusters.
            bSalvo = true;
            hits = wtype.getRackSize();
            if ( !bAllShotsHit ) {
                hits = Compute.missilesHit( hits );
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
            int nRange = ae.getPosition().distance(target.getPosition());
            if (nRange <= wtype.getShortRange()) {
                nDamPerHit = 25;
            } else if (nRange <= wtype.getMediumRange()) {
                nDamPerHit = 20;
            } else {
                nDamPerHit = 10;
            }
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
                if (wr.amsShotDown > 0) {
                    int shotDown = Math.min(wr.amsShotDown, hits);
                    phaseReport.append("\tAMS shoots down ")
                        .append(shotDown).append(" missile(s).\n");
                    hits -= wr.amsShotDown;
                }

                // Is the building hit by Inferno rounds?
                if ( bInferno && hits > 0 ) {
                
                    // start a fire in the targets hex
                    Coords c = target.getPosition();
                    Hex h = game.getBoard().getHex(c);
                
                    // Is there a fire in the hex already?
                    if ( h.contains( Terrain.FIRE ) ) {
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
                        h.addTerrain(new Terrain(Terrain.FIRE, 1));
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
            return;

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
                return;
            }
            nDamPerHit = Compute.d6(hits);
            phaseReport.append( "riddles the target with " ).append( 
                nDamPerHit ).append( sSalvoType ).append( "and " );
            hits = 1;
        }

        // Mech and Vehicle MGs do *DICE* of damage to PBI.
        else if (atype != null && atype.hasFlag(AmmoType.F_MG) &&
                  !isWeaponInfantry && (target instanceof Infantry) &&
                  !(target instanceof BattleArmor) ) {

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
                return;
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
            
            if (wr.amsShotDown > 0) {
                int shotDown = Math.min(wr.amsShotDown, hits);
                phaseReport.append("\n\tAMS engages, firing ")
                    .append(wr.amsShotDown).append(" shots, shooting down ")
                    .append(shotDown).append(" missile(s).");
                hits -= wr.amsShotDown;
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
                if (wr.amsShotDown > 0) {
                    int shotDown = Math.min(wr.amsShotDown, hits);
                    phaseReport.append("\n\tAMS engages, firing ")
                        .append(wr.amsShotDown).append(" shots, shooting down ")
                        .append(shotDown).append(" missile(s).");
                    hits -= wr.amsShotDown;
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
                    Hex h = game.getBoard().getHex(c);
                    if ( !h.contains( Terrain.FIRE ) ) {
                        phaseReport.append( " Fire started in hex " )
                            .append( c.getBoardNum() )
                            .append( ".\n" );
                        h.addTerrain(new Terrain(Terrain.FIRE, 1));
                    }
                    game.board.addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, hits );
                    sendChangedHex(c);

                    return;
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
                    Hex h = game.getBoard().getHex(c);

                    // Unless there a fire in the hex already, start one.
                    if ( !h.contains( Terrain.FIRE ) ) {
                        phaseReport.append( " Fire started in hex " )
                            .append( c.getBoardNum() )
                            .append( ".\n" );
                        h.addTerrain(new Terrain(Terrain.FIRE, 1));
                    }
                    game.board.addInfernoTo
                        ( c, InfernoTracker.STANDARD_ROUND, hits );
                    sendChangedHex(c);

                    return;
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
                    tryIgniteHex( target.getPosition(), bInferno, tn );
                }
                return;
            }
            
            // targeting a hex for clearing
            if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {

                nDamage = nDamPerHit * hits;
                if ( !bSalvo ) {
                    phaseReport.append("hits!");
                }
                if (ae instanceof Infantry) {
                    phaseReport.append("\n    But infantry cannot try to clear hexes!\n");
                    return;
                }


                phaseReport.append("    Terrain takes " ).append( nDamage ).append( " damage.\n");
                
                // Any clear attempt can result in accidental ignition, even
                // weapons that can't normally start fires.  that's weird.
                // Buildings can't be accidentally ignited.
                if ( bldg != null &&
                     tryIgniteHex(target.getPosition(), bInferno, 9) ) {
                    return;
                }
                
                int tn = 14 - nDamage;
                tryClearHex(target.getPosition(), tn);
                
                return;
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
                return;
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

                     return;
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
                HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());

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
                    } else {
                        phaseReport.append
                            ( damageEntity(entityTarget, hit, nDamage) );
                    }
                }
                hits -= nCluster;
            }
        } // Handle the next cluster.

        phaseReport.append("\n");
    }
    
    /**
     * Handle all physical attacks for the round
     */
    private void resolvePhysicalAttacks() {
        roundReport.append("\nPhysical Attack Phase\n-------------------\n");
        
        int cen = Entity.NONE;
        
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
            
            if (o instanceof PunchAttackAction) {
                PunchAttackAction paa = (PunchAttackAction)o;
                if (paa.getArm() == PunchAttackAction.BOTH) {
                    // If we're punching while prone (at a Tank,
                    // duh), then we can only use one arm.
                    Entity ae = game.getEntity( aa.getEntityId() );
                    if ( ae.isProne() ) {
                        // As a sanity check, make certain
                        // that no arm has been destroyed.
                        if ( ae.isLocationDestroyed(Mech.LOC_RARM) ) {
                            phaseReport.append( ae.getDisplayName() ).append
                                ( " can't punch: right arm destroyed.\n" );
                            continue;
                        }
                        else if ( ae.isLocationDestroyed(Mech.LOC_LARM) ) {
                            phaseReport.append( ae.getDisplayName() ).append
                                ( " can't punch: left arm destroyed.\n" );
                            continue;
                        }

                        // Find out which arm has the best attack.
                        paa.setArm(PunchAttackAction.LEFT);
                        ToHitData left = Compute.toHitPunch( game, paa );
                        double oddsLeft = Compute.oddsAbove(left.getValue());
                        int damageLeft = Compute.getPunchDamageFor
                            ( ae, PunchAttackAction.LEFT );
                        paa.setArm(PunchAttackAction.RIGHT);
                        ToHitData right = Compute.toHitPunch( game, paa );
                        double oddsRight = Compute.oddsAbove(right.getValue());
                        int damageRight = Compute.getPunchDamageFor
                            ( ae, PunchAttackAction.RIGHT );

                        // Use the best attack.
                        if (  oddsLeft*damageLeft > oddsRight*damageRight ) {
                            // Be sure to set the left arm first.
                            paa.setArm(PunchAttackAction.LEFT);
                            resolvePunchAttack(paa, cen);
                            cen = paa.getEntityId();
                        } else {
                            // We've already set the right arm.
                            resolvePunchAttack(paa, cen);
                            cen = paa.getEntityId();
                        }
                    } // End Entity-is-prone
                    else {
                        paa.setArm(PunchAttackAction.LEFT);
                        resolvePunchAttack(paa, cen);
                        cen = paa.getEntityId();
                        paa.setArm(PunchAttackAction.RIGHT);
                        resolvePunchAttack(paa, cen);
                    }
                } else {
                    resolvePunchAttack(paa, cen);
                    cen = paa.getEntityId();
                }
            } else if (o instanceof KickAttackAction) {
                KickAttackAction kaa = (KickAttackAction)o;
                resolveKickAttack(kaa, cen);
                cen = kaa.getEntityId();
            } else if (o instanceof BrushOffAttackAction) {
                BrushOffAttackAction baa = (BrushOffAttackAction)o;
                if (baa.getArm() == BrushOffAttackAction.BOTH) {
                    baa.setArm(BrushOffAttackAction.LEFT);
                    resolveBrushOffAttack(baa, cen);
                    cen = baa.getEntityId();
                    baa.setArm(BrushOffAttackAction.RIGHT);
                    resolveBrushOffAttack(baa, cen);
                } else {
                    resolveBrushOffAttack(baa, cen);
                    cen = baa.getEntityId();
                }
            } else if (o instanceof ThrashAttackAction) {
                ThrashAttackAction taa = (ThrashAttackAction)o;
                resolveThrashAttack(taa, cen);
                cen = taa.getEntityId();
            } else if (o instanceof ClubAttackAction) {
                ClubAttackAction caa = (ClubAttackAction)o;
                resolveClubAttack(caa, cen);
                cen = caa.getEntityId();
            } else if (o instanceof PushAttackAction) {
                PushAttackAction paa = (PushAttackAction)o;
                resolvePushAttack(paa, cen);
                cen = paa.getEntityId();
            }  else if (o instanceof ChargeAttackAction) {
                ChargeAttackAction caa = (ChargeAttackAction)o;
                resolveChargeAttack(caa, cen);
                cen = caa.getEntityId();
            }  else if (o instanceof DfaAttackAction) {
                DfaAttackAction daa = (DfaAttackAction)o;
                resolveDfaAttack(daa, cen);
                cen = daa.getEntityId();
            } else {
                // hmm, error.
            }
        }
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
     * kept even if the pilot is unconcious, so that he can fail.
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
    private void resolvePunchAttack(PunchAttackAction paa, int lastEntityId) {
        final Entity ae = game.getEntity(paa.getEntityId());
        final Targetable target = game.getTarget(paa.getTargetType(), paa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final String armName = paa.getArm() == PunchAttackAction.LEFT
        ? "Left Arm" : "Right Arm";
        final boolean targetInBuilding = Compute.isInBuilding( game, te );

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
        // compute to-hit
        ToHitData toHit = Compute.toHitPunch(game, paa);
        int roll = 0;
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the punch is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the punch is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // roll
            roll = Compute.d6(2);
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
        }

        int damage = Compute.getPunchDamageFor(ae, paa.getArm());
        
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
            phaseReport.append( damageEntity(te, hit, damage) );
        }

        
        phaseReport.append("\n");
    }
    
    /**
     * Handle a kick attack
     */
    private void resolveKickAttack(KickAttackAction kaa, int lastEntityId) {
        final Entity ae = game.getEntity(kaa.getEntityId());
        final Targetable target = game.getTarget(kaa.getTargetType(), kaa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final String legName = kaa.getLeg() == KickAttackAction.LEFT
        ? "Left Leg"
        : "Right Leg";
        final boolean targetInBuilding = Compute.isInBuilding( game, te );

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
        // compute to-hit
        ToHitData toHit = Compute.toHitKick(game, kaa);
        int roll = 0;
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the kick is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            game.addPSR(new PilotingRollData(ae.getId(), 0, "missed a kick"));
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the kick is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // roll
            roll = Compute.d6(2);
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
        }
        
        int damage = Compute.getKickDamageFor(ae, kaa.getLeg());
        
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
            phaseReport.append( damageEntity(te, hit, damage) );
        }

        if (te.getMovementType() == Entity.MovementType.BIPED || te.getMovementType() == Entity.MovementType.QUAD) {
            game.addPSR(new PilotingRollData(te.getId(), 0, "was kicked"));
        }
        
        phaseReport.append("\n");
    }

    /**
     * Handle a brush off attack
     */
    private void resolveBrushOffAttack( BrushOffAttackAction baa,
                                        int lastEntityId ) {
        final Entity ae = game.getEntity(baa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "brush off".
        final Entity te = game.getEntity(baa.getTargetId());
        final String armName = baa.getArm() == BrushOffAttackAction.LEFT
            ? "Left Arm" : "Right Arm";

        if (lastEntityId != baa.getEntityId()) {
            phaseReport.append( "\nPhysical attacks for " )
                .append( ae.getDisplayName() )
                .append( "\n" );
        }

        phaseReport.append("    Brush Off " )
            .append( te.getDisplayName() )
            .append( " with " )
            .append( armName );

        // compute to-hit
        ToHitData toHit = Compute.toHitBrushOff(game, baa);
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append( ", but the brush off is impossible (" )
                .append( toHit.getDesc() )
                .append( ")\n" );
            return;
        }
        phaseReport.append("; needs ").append(toHit.getValue()).append(", ");

        // roll
        int roll = Compute.d6(2);
        phaseReport.append("rolls ").append(roll).append(" : ");

        // ASSUMPTION: buildings can't absorb *this* damage.
        int damage = Compute.getBrushOffDamageFor(ae, baa.getArm());

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
    }

    /**
     * Handle a thrash attack
     */
    private void resolveThrashAttack( ThrashAttackAction baa,
                                        int lastEntityId ) {
        final Entity ae = game.getEntity(baa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "thrash".
        final Entity te = game.getEntity(baa.getTargetId());

        if (lastEntityId != baa.getEntityId()) {
            phaseReport.append( "\nPhysical attacks for " )
                .append( ae.getDisplayName() )
                .append( "\n" );
        }

        phaseReport.append("    Thrash at " )
            .append( te.getDisplayName() );

        // compute to-hit
        ToHitData toHit = Compute.toHitThrash(game, baa);
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

            // roll
            int roll = Compute.d6(2);
            phaseReport.append("rolls ").append(roll).append(" : ");

            // do we hit?
            if (roll < toHit.getValue()) {
                phaseReport.append("misses.\n");
                return;
            }
            phaseReport.append( ", hits" );
        }

        // Standard damage loop in 5 point clusters.
        int hits = Compute.getThrashDamageFor(ae);
        phaseReport.append( " and deals " )
            .append( hits )
            .append( " points of damage in 5 point clusters.");
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
        PilotingRollData roll = Compute.getBasePilotingRoll(game, ae.getId());
        roll.addModifier( 0, "thrashing at infantry" );
        phaseReport.append( ae.getDisplayName() )
            .append( " must make a piloting skill check (" )
            .append( "thrashing at infantry).\n");
        final int diceRoll = Compute.d6(2);
        phaseReport.append("Needs " )
            .append( roll.getValueAsString() )
            .append( " [" )
            .append( roll.getDesc() )
            .append( "]" )
            .append( ", rolls " )
            .append( diceRoll )
            .append( " : " );
        if (diceRoll < roll.getValue()) {
            phaseReport.append("fails.\n");
            doEntityFall( ae, roll );
        } else {
            phaseReport.append("succeeds.\n");
        }

    }

    /**
     * Handle a club attack
     */
    private void resolveClubAttack(ClubAttackAction caa, int lastEntityId) {
        final Entity ae = game.getEntity(caa.getEntityId());
        final Targetable target = game.getTarget(caa.getTargetType(), caa.getTargetId());
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }
        final boolean targetInBuilding = Compute.isInBuilding( game, te );

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
        // compute to-hit
        ToHitData toHit = Compute.toHitClub(game, caa);
        int roll = 0;
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the attack is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the club attack is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // roll
            roll = Compute.d6(2);
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
        }

        int damage = Compute.getClubDamageFor(ae, caa.getClub());
        
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
    private void resolvePushAttack(PushAttackAction paa, int lastEntityId) {
        final Entity ae = game.getEntity(paa.getEntityId());
        // PLEASE NOTE: buildings are *never* the target of a "push".
        final Entity te = game.getEntity(paa.getTargetId());
        
        if (lastEntityId != paa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " ).append( ae.getDisplayName() ).append( "\n");
        }
        
        phaseReport.append("    Pushing " ).append( te.getDisplayName());
        
//        // should we even bother?
//        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
//            phaseReport.append(" but the target is already destroyed!\n");
//            return;
//        }
        
        // compute to-hit
        ToHitData toHit = Compute.toHitPush(game, paa);
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            phaseReport.append(", but the push is impossible (" ).append( toHit.getDesc() ).append( ")\n");
            return;
        }
        phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
        
        // roll
        int roll = Compute.d6(2);
        phaseReport.append("rolls " ).append( roll ).append( " : ");
        
        // do we hit?
        if (roll < toHit.getValue()) {
            phaseReport.append("misses.\n");
            return;
        }
        
        // we hit...
        int direction = ae.getFacing();
        
        Coords src = te.getPosition();
        Coords dest = src.translated(direction);
        
        if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
            phaseReport.append("succeeds: target is pushed into hex "
            ).append( dest.getBoardNum()
            ).append( "\n");
            doEntityDisplacement(te, src, dest, new PilotingRollData(te.getId(), 0, "was pushed"));
            
            // if push actually moved the target, attacker follows thru
            if (!te.getPosition().equals(src)) {
                ae.setPosition(src);
            }
        } else {
            if (game.getOptions().booleanOption("push_off_board") && !game.board.contains(dest)) {
                game.removeEntity(te.getId(),
                                  Entity.REMOVE_IN_RETREAT);
                send(createRemoveEntityPacket(te.getId(),
                                              Entity.REMOVE_IN_RETREAT));
                phaseReport.append("\n*** " ).append( te.getDisplayName() ).append( " has been forced from the field. ***\n");
                // TODO: remove passengers and swarmers.
                ae.setPosition(src);
            } else {
                phaseReport.append("succeeds, but target can't be moved.\n");
                game.addPSR(new PilotingRollData(te.getId(), 0, "was pushed"));
            }
        }
        
        
        phaseReport.append("\n");
    }
    
    /**
     * Handle a charge attack
     */
    private void resolveChargeAttack(ChargeAttackAction caa, int lastEntityId) {
        final Entity ae = game.getEntity(caa.getEntityId());
        final Targetable target = game.getTarget(caa.getTargetType(), caa.getTargetId());
        Entity te = null;
        if (target != null && target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }

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
            doEntityDisplacement(ae, ae.getPosition(), caa.getTargetPos(), null);
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
        
        phaseReport.append("    Charging " ).append( target.getDisplayName());
        
        // target still in the same position?
        if (!target.getPosition().equals(caa.getTargetPos())) {
            phaseReport.append(" but the target has moved.\n");
            doEntityDisplacement(ae, ae.getPosition(), caa.getTargetPos(), null);
            return;
        }
        
        // compute to-hit
        ToHitData toHit = Compute.toHitCharge(game, caa);
        
        // if the attacker's prone, fudge the roll
        int roll;
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            roll = -12;
            phaseReport.append(", but the charge is impossible (" ).append( toHit.getDesc() ).append( ") : ");
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            phaseReport.append(", the charge is an automatic hit (" ).append( toHit.getDesc() ).append( "), ");
            roll = Integer.MAX_VALUE;
        } else {
            // roll
            roll = Compute.d6(2);
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
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
            int damage = Compute.getChargeDamageFor(ae);
            phaseReport.append( "hits.\n  " )
                .append( damageBuilding( bldg, damage ) );
        
            // Damage any infantry in the hex.
            this.damageInfantryIn( bldg, damage );

            // Apply damage to the attacker.
            int toAttacker = Compute.getChargeDamageTakenBy( ae, bldg );
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
            resolveChargeDamage( ae, te, toHit, direction );
        }
        return;
    }

    /**
     * Handle a charge's damage
     */
    private void resolveChargeDamage(Entity ae, Entity te, ToHitData toHit, int direction) {

        // we hit...
        int damage = Compute.getChargeDamageFor(ae);
        int damageTaken = Compute.getChargeDamageTakenBy(ae, te);
        PilotingRollData chargePSR = null;

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
        phaseReport.append("\n  Defender takes " ).append( damage ).append( " damage" ).append( toHit.getTableDesc() ).append( ".");
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
        phaseReport.append("\n  Attacker takes " ).append( damageTaken ).append( " damage.");
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = ae.rollHitLocation(ToHitData.HIT_NORMAL, toHit.SIDE_FRONT);
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
        } else {
            if (game.getOptions().booleanOption("push_off_board") && !game.board.contains(dest)) {
                game.removeEntity(te.getId(),
                                  Entity.REMOVE_IN_RETREAT);
                send(createRemoveEntityPacket(te.getId(),
                                              Entity.REMOVE_IN_RETREAT));
                phaseReport.append("\n*** " ).append( te.getDisplayName() ).append( " target has been forced from the field. ***\n");
                // TODO: remove passengers and swarmers.

                doEntityDisplacement(ae, ae.getPosition(), src, chargePSR);
            } else {
                // they stil have to roll
                game.addPSR(new PilotingRollData(te.getId(), 2, "was charged"));
                game.addPSR(chargePSR);
            }
        }
        
        phaseReport.append("\n");

    } // End private void resolveChargeDamage( Entity, Entity, ToHitData )
    
    /**
     * Handle a death from above attack
     */
    private void resolveDfaAttack(DfaAttackAction daa, int lastEntityId) {
        final Entity ae = game.getEntity(daa.getEntityId());
        final Targetable target = game.getTarget(daa.getTargetType(), daa.getTargetId());
        Entity te = null;
        if (target != null && target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity)target;
        }

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
                doEntityFall(ae, daa.getTargetPos(), 2, 3, Compute.getBasePilotingRoll(game, ae.getId()));
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
        
        // compute to-hit
        ToHitData toHit = Compute.toHitDfa(game, daa);
        
        // hack: if the attacker's prone, or incapacitated, fudge the roll
        int roll;
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
            // roll
            roll = Compute.d6(2);
            phaseReport.append("; needs " ).append( toHit.getValue() ).append( ", ");
            phaseReport.append("rolls " ).append( roll ).append( " : ");
        }
        
        // do we hit?
        if (roll < toHit.getValue()) {
            Coords dest = te.getPosition();
            Coords targetDest = Compute.getPreferredDisplacement(game, te.getId(), dest, direction);
            phaseReport.append("misses.\n");
            if (targetDest != null) {
                // attacker falls into destination hex
                phaseReport.append(ae.getDisplayName() ).append( " falls into hex " ).append( dest.getBoardNum() ).append( ".\n");
                doEntityFall(ae, dest, 2, 3, Compute.getBasePilotingRoll(game, ae.getId()));

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
        int damage = Compute.getDfaDamageFor(ae);
        int damageTaken = Compute.getDfaDamageTakenBy(ae);
        
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
            phaseReport.append("\n  Defender takes " ).append( damage ).append( " damage" ).append( toHit.getTableDesc() ).append( ".");
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                phaseReport.append(damageEntity(te, hit, cluster));
                damage -= cluster;
            }
        }

        phaseReport.append("\n  Attacker takes " ).append( damageTaken ).append( " damage.");
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = ae.rollHitLocation(ToHitData.HIT_KICK, toHit.SIDE_FRONT);
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
        if (game.getOptions().booleanOption("push_off_board") && !game.board.contains(dest.translated(direction))) {
            game.removeEntity(te.getId(),
                              Entity.REMOVE_IN_RETREAT);
            send(createRemoveEntityPacket(te.getId(),
                                          Entity.REMOVE_IN_RETREAT));
            phaseReport.append("\n*** " ).append( te.getDisplayName() ).append( " target has been forced from the field. ***\n");
            // TODO: remove passengers and swarmers.
        } else {
            if (targetDest != null) {
                doEntityDisplacement(te, dest, targetDest, new PilotingRollData(te.getId(), 2, "hit by death from above"));
            } else {
                // ack!  automatic death!  Tanks
                // suffer an ammo/power plant hit.
                // TODO : a Mech suffers a Head Blown Off crit.
                phaseReport.append(destroyEntity(te, "impossible displacement", (te instanceof Mech), (te instanceof Mech)));
            }
        }
        // HACK: to avoid automatic falls, displace from dest to dest
        doEntityDisplacement(ae, dest, dest, new PilotingRollData(ae.getId(), 4, "executed death from above"));
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
            Hex entityHex = game.getBoard().getHex(entity.getPosition());
            
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
                 entity.crew.isDead() ) {
                continue;
            }

            // engine hits add a lot of heat, provided the engine is on
            entity.heatBuildup += entity.getEngineCritHeat();

            // If a Mek had an active Stealth suite, add 10 heat.
            if ( entity instanceof Mech && entity.isStealthActive() ) {
                entity.heatBuildup += 10;
                roundReport.append("Added 10 heat from Stealth Armor...\n");
            }

            // Add +5 Heat if the hex you're in is on fire
            // and was on fire for the full round.
            if (entityHex.levelOf(Terrain.FIRE) == 2) {
                entity.heatBuildup += 5;
                roundReport.append("Added 5 heat from a fire...\n");
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

            // heat effects: start up
            if (entity.heat < 30 && entity.isShutDown()) {
                if (entity.heat < 14) {
                    entity.setShutDown(false);
                    roundReport.append( entity.getDisplayName() )
                        .append( " automatically starts up.\n" );
                } else {
                    int startup = 4 + (((entity.heat - 14) / 4) * 2);
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
                if (entity.heat >= 30) {
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
            if (entity.getHitCriticals( CriticalSlot.TYPE_SYSTEM,
                                        Mech.SYSTEM_LIFE_SUPPORT,
                                        Mech.LOC_HEAD ) > 0
                && entity.heat >= 15) {
                if (entity.heat >= 25) {
                    // mechwarrior takes 2 damage
                    roundReport.append(entity.getDisplayName() ).append( " has 25 or higher heat and damaged life support.  Mechwarrior takes 2 damage.\n");
                    damageCrew(entity, 2);
                } else {
                    // mechwarrior takes 1 damage
                    roundReport.append(entity.getDisplayName() ).append( " has 15 or higher heat and damaged life support.  Mechwarrior takes 1 damage.\n");
                    damageCrew(entity, 1);
                }
                // The pilot may have just expired.
                if ( entity.crew.isDead() ) {
                    roundReport.append( "*** " )
                        .append( entity.getDisplayName() )
                        .append( " PILOT BAKES TO DEATH! ***" );
                }
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
            if ( BattleArmor.ASSAULT_CLAW.equals(equip.getInternalName()) ) {
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
            if (entity.getMovementType() == Entity.MovementType.BIPED ||
            entity.getMovementType() == Entity.MovementType.QUAD) {
                // if this mech has 20+ damage, add another roll to the list.
                if (entity.damageThisPhase >= 20) {
                    game.addPSR(new PilotingRollData(entity.getId(), 1, "20+ damage"));
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
                 entity.isDestroyed()) {
                continue;
            }
            final Hex curHex = game.board.getHex(entity.getPosition());
            if (curHex.contains(Terrain.FIRE)) {
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
            if ( null == entity.getPosition() ) {
                continue;
            }
            final Hex curHex = game.board.getHex(entity.getPosition());
            if ((curHex.levelOf(Terrain.WATER) > 1
            || (curHex.levelOf(Terrain.WATER) == 1 && entity.isProne()))
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
        // non-mechs don't need to
        if (!(entity instanceof Mech) || entity.isProne() || entity.isDoomed() 
        || entity.isDestroyed()) {
            return;
        }
        // add all cumulative rolls, count all rolls
        Vector rolls = new Vector();
        StringBuffer reasons = new StringBuffer();
        PilotingRollData base = Compute.getBasePilotingRoll(game, entity.getId());
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
            phaseReport.append("\n").append(entity.getDisplayName()).append(" must make ").append(rolls.size()).append(" piloting skill roll(s) and automatically fails (").append(base.getDesc()).append(").\n");
            doEntityFall(entity, base);
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
                phaseReport.append(", extra modifier ").append(modifier.getValueAsString());
                target = new PilotingRollData(entity.getId());
                target.append(base);
                target.append(modifier);
            }
            int diceRoll = Compute.d6(2);
            phaseReport.append("); needs ").append(target.getValueAsString());
            phaseReport.append(", rolls ").append(diceRoll).append(" : ");
            if (diceRoll < target.getValue()) {
                phaseReport.append(" falls.\n");
                doEntityFall(entity, base);
                return;
            } else {
                phaseReport.append(" succeeds.\n");
            }
        }
    }
    
    /**
     * Inflict damage on a pilot
     */
    private String damageCrew(Entity en, int damage) {
        String s = new String();
        
        if (!en.crew.isDead()) {
            en.crew.setHits(en.crew.getHits() + damage);
            s += "        Pilot of " + en.getDisplayName() + " \"" + en.crew.getName() + "\" takes " + damage + " damage.";
            if (en.crew.getHits() < 6) {
                en.crew.setRollsNeeded(en.crew.getRollsNeeded() + damage);
            } else {
                en.crew.setDoomed(true);
                en.crew.setRollsNeeded(0);
                s += "\n*** " + en.getDisplayName() + " PILOT KILLED! ***";
            }
        }
        
        return s;
    }
    
    /**
     * This checks if the mech pilot goes unconcious from the damage he has
     * taken this phase.
     */
    private void resolveCrewDamage() {
        boolean anyRolls = false;
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();
            final int totalHits = e.getCrew().getHits();
            final int rollsNeeded = e.getCrew().getRollsNeeded();
            e.crew.setRollsNeeded(0);
            
            if (!e.isTargetable() || !e.getCrew().isActive() || rollsNeeded == 0) {
                continue;
            }
            anyRolls = true;
            for (int hit = totalHits - rollsNeeded + 1; hit <= totalHits; hit++) {
                int roll = Compute.d6(2);
                int rollTarget = Compute.getConciousnessNumber( hit );
                phaseReport.append("\nPilot of " ).append( e.getDisplayName()
                                   ).append( " \"" ).append( e.getCrew().getName()
                                   ).append( "\" needs a " ).append( rollTarget
                                   ).append( " to stay concious.  Rolls " ).append( roll
                                   ).append( " : ");
                if (roll >= rollTarget) {
                    phaseReport.append("successful!");
                } else {
                    e.crew.setUnconcious(true);
                    e.crew.setKoThisRound(true);
                    phaseReport.append("blacks out.");
                    break;
                }
            }
        }
        if (anyRolls) {
            phaseReport.append("\n");
        }
    }
    
    /**
     * Make the rolls indicating whether any unconcious crews wake up
     */
    private void resolveCrewWakeUp() {
        boolean anyRolls = false;
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity e = (Entity)i.nextElement();
            
            // only unconscious pilots of mechs can roll to wake up
            if (!e.isTargetable() || !e.crew.isUnconcious() ||
            e.crew.isKoThisRound() || !(e instanceof Mech)) {
                continue;
            }
            anyRolls = true;
            int roll = Compute.d6(2);
            int rollTarget = Compute.getConciousnessNumber( e.crew.getHits() );
            roundReport.append("\nPilot of " ).append( e.getDisplayName()
                               ).append( " \"" ).append( e.crew.getName()
                               ).append( "\" needs a " ).append( rollTarget
                               ).append( " to regain conciousness.  Rolls " ).append( roll
                               ).append( " : ");
            if (roll >= rollTarget) {
                roundReport.append("successful!");
                e.crew.setUnconcious(false);
            } else {
                roundReport.append("fails.");
            }
        }
        if (anyRolls) {
            roundReport.append("\n");
        }
    }
    
    public String damageEntity(Entity te, HitData hit, int damage) {
        return damageEntity(te, hit, damage, false);
    }
    
    /**
     * Deals the listed damage to a mech.  Returns a description
     * string for the log.
     *
     * Currently mech only.
     *
     * @param te the target entity
     * @param hit the hit data for the location hit
     * @param damage the damage to apply
     * @param ammoExplosion ammo explosion type damage is handled slightly differently
     */
    private String damageEntity(Entity te, HitData hit, int damage, boolean ammoExplosion) {
        String desc = new String();
        boolean isBattleArmor = (te instanceof BattleArmor);
        boolean isPlatoon = !isBattleArmor && (te instanceof Infantry);
        Hex te_hex = null;
        
        int crits = hit.getEffect() == HitData.EFFECT_CRITICAL ? 1 : 0;
        //int loc = hit.getLocation();
        HitData nextHit = null;

        // Is the infantry in the open?
        // TODO : do infantry take double damage in Smoke?
        if ( isPlatoon && !te.isDestroyed() && !te.isDoomed() ) {
            te_hex = game.board.getHex( te.getPosition() );
            if ( te_hex != null &&
                 !te_hex.contains( Terrain.WOODS ) &&
                 !te_hex.contains( Terrain.ROUGH ) &&
                 !te_hex.contains( Terrain.RUBBLE ) &&
                 !te_hex.contains( Terrain.BUILDING ) ) {
                // PBI.  Damage is doubled.
                damage = damage * 2;
                desc += "\n        Infantry platoon caught in the open!!!  Damage doubled." ;
            }
        }

        // Allocate the damage
        while (damage > 0) {
            // let's resolve some damage!
            desc += "\n        " + te.getDisplayName() + " takes " + damage + " damage to " + te.getLocationAbbr(hit) + ".";
            
            // was the section destroyed earlier this phase?
            if (te.getInternal(hit) == Entity.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                crits = 0;
            }

            // Does an exterior passenger absorb some of the damage?
            int nLoc = hit.getLocation();
            Entity passenger = te.getExteriorUnitAt( nLoc, hit.isRear() );
            if ( !ammoExplosion &&
                 null != passenger && !passenger.isDoomed() ) {

                // Yup.  Roll up some hit data for that passenger.
                desc += "\n            The passenger, " +
                    passenger.getDisplayName() + ", gets in the way.";
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
                desc += damageEntity( passenger, passHit, damage );

                // Did some damage pass on?
                if ( damage > absorb ) {
                    // Yup.  Remove the absorbed damage.
                    damage -= absorb;
                    desc += "\n    " + damage +
                        " damage point(s) passes on to " +
                        te.getDisplayName() + ".";
                } else {
                    // Nope.  Return our description.
                    return desc;
                }

            } // End nLoc-has-exterior-passenger

            // is this a mech dumping ammo being hit in the rear torso?
            boolean bTorso = (nLoc == Mech.LOC_CT || nLoc == Mech.LOC_RT || nLoc == Mech.LOC_LT);
            if (te instanceof Mech && hit.isRear() && bTorso) {
                for (Enumeration e = te.getAmmo(); e.hasMoreElements(); ) {
                    Mounted mAmmo = (Mounted)e.nextElement();
                    if (mAmmo.isDumping() && !mAmmo.isDestroyed() && !mAmmo.isHit()) {
                        // doh.  explode it
                        desc += explodeEquipment(te, mAmmo.getLocation(), mAmmo);
                        mAmmo.setHit(true);
                    }
                }
            }
            
            // is there armor in the location hit?
            if (!ammoExplosion && te.getArmor(hit) > 0) {
                if (te.getArmor(hit) > damage) {
                    // armor absorbs all damage
                    te.setArmor(te.getArmor(hit) - damage, hit);
                    te.damageThisPhase += damage;
                    damage = 0;
                    desc += " " + te.getArmor(hit) + " Armor remaining";
                } else {
                    // damage goes on to internal
                    int absorbed = Math.max(te.getArmor(hit), 0);
                    te.setArmor(Entity.ARMOR_DESTROYED, hit);
                    te.damageThisPhase += absorbed;
                    damage -= absorbed;
                    desc += " Armor destroyed,";
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
                            desc += " " + te.getInternal(hit) + " men alive.";
                        } else {
                        desc += " " + te.getInternal(hit) + " Internal Structure remaining";
                        }
                    } else {
                        // damage transfers, maybe
                        int absorbed = Math.max(te.getInternal(hit), 0);
                        destroyLocation(te, hit.getLocation());
                        te.damageThisPhase += absorbed;
                        damage -= absorbed;
                        // Infantry have only one section.
                        if ( isPlatoon ) {
                            desc += " <<<PLATOON KILLED>>>,";
                        } else if ( isBattleArmor ) {
                            desc += " <<<TROOPER KILLED>>>,";
                        } else {
                        desc += " <<<SECTION DESTROYED>>>,";
                        }
                        if (hit.getLocation() == Mech.LOC_RT || hit.getLocation() == Mech.LOC_LT) {
                            int numEngineHits = 0;
                            numEngineHits += te.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_CT);
                            numEngineHits += te.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_RT);
                            numEngineHits += te.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_LT);
                            if (numEngineHits > 2) {
                                // third engine hit
                                phaseReport.append(destroyEntity(te, "engine destruction"));
                            }
                        }
                    }
                }

                // is the internal structure gone?  what are the transfer potentials?
                if (te.getInternal(hit) <= 0) {
                    nextHit = te.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        // Entity destroyed.  Ammo explosions are
                        // neither survivable nor salvagable.
                        desc += destroyEntity(te, "damage", !ammoExplosion,
                                              !ammoExplosion);
                        // nowhere for further damage to go
                        damage = 0;
                    } else if ( nextHit.getLocation() == Entity.LOC_NONE ) {
                        // Rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && te.locationHasCase(hit.getLocation())) {
                        // remaining damage prevented
                        desc += " remaining " + damage + " damage prevented by CASE.";
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        desc += " " + damage + " damage transfers to "
                        + te.getLocationAbbr(nextHit) + ".";
                    }
                }
            }

            // resolve special results
            if (hit.getEffect() == HitData.EFFECT_VEHICLE_MOVE_DAMAGED) {
                desc += "\n            Movement system damaged!";
                int nMP = te.getOriginalWalkMP();
                if (nMP <= 1) {
                    ((Tank)te).immobilize();
                    // Does the hovercraft sink?
                    te_hex = game.board.getHex( te.getPosition() );
                    if ( te.getMovementType() == Entity.MovementType.HOVER &&
                         te_hex.levelOf(Terrain.WATER) > 0 ) {
                        desc += destroyEntity(te, "a watery grave", false);
                    }
                }
                else {
                    te.setOriginalWalkMP(nMP - 1);
                }
            }
            else if (hit.getEffect() == HitData.EFFECT_VEHICLE_MOVE_DESTROYED) {
                desc += "\n            Movement system destroyed!";
                ((Tank)te).immobilize();
                // Does the hovercraft sink?
                te_hex = game.board.getHex( te.getPosition() );
                if ( te.getMovementType() == Entity.MovementType.HOVER &&
                     te_hex.levelOf(Terrain.WATER) > 0 ) {
                    desc += destroyEntity(te, "a watery grave", false);
                }
            }
            else if (hit.getEffect() == HitData.EFFECT_VEHICLE_TURRETLOCK) {
                desc += "\n            Turret locked!";
                ((Tank)te).lockTurret();
            }
            
            // roll all critical hits against this location
            for (int i = 0; i < crits; i++) {
                desc += "\n" + criticalEntity(te, hit.getLocation());
            }
            crits = 0;
            
            if (te instanceof Mech && hit.getLocation() == Mech.LOC_HEAD) {
                desc += "\n" + damageCrew(te, 1);
            }
            
            // loop to next location
            hit = nextHit;
        }
        
        return desc;
    }
    
    /**
     * Rolls and resolves critical hits on mechs or vehicles.
     */
    private String criticalEntity(Entity en, int loc) {
        String desc = "        Critical hit on " + en.getLocationAbbr(loc) + ". ";
        int hits = 0;
        int roll = Compute.d6(2);
        desc += "Roll = " + roll + ";";
        if (roll <= 7) {
            desc += " no effect.";
            return desc;
        } else if (roll >= 8 && roll <= 9) {
            hits = 1;
            desc += " 1 location.";
        } else if (roll >= 10 && roll <= 11) {
            hits = 2;
            desc += " 2 locations.";
        } else if (roll == 12) {
            if (en instanceof Tank) {
                hits = 3;
                desc += " 3 locations.";
            } else if (en.locationIsLeg(loc)) {
                desc += "<<<LIMB BLOWN OFF>>> " + en.getLocationName(loc) + " blown off.";
                if (en.getInternal(loc) > 0) {
                    destroyLocation(en, loc);
                }
                return desc;
            } else if (loc == Mech.LOC_RARM || loc == Mech.LOC_LARM) {
                desc += "<<<LIMB BLOWN OFF>>> " + en.getLocationName(loc) + " blown off.";
                destroyLocation(en, loc);
                return desc;
            } else if (loc == Mech.LOC_HEAD) {
                desc += "<<<HEAD BLOWN OFF>>> " + en.getLocationName(loc) + " blown off.";
                destroyLocation(en, loc);
                en.crew.setDoomed(true);
                desc += "\n*** " + en.getDisplayName() + " PILOT KILLED! ***";
                return desc;
            } else {
                // torso hit
                hits = 3;
                desc += " 3 locations.";
            }
        }
        
        // vehicle handle crits in their own 'special' way
        if (en instanceof Tank) {
            Tank tank = (Tank)en;
            for (int x = 0; x < hits && !tank.isDoomed(); x++) {
                switch (Compute.d6(1)) {
                    case 1 :
                        desc += "\n            <<<CRITICAL HIT>>> Crew stunned for 3 turns";
                        // Carried units can't unload from a stunned transport.
                        // Units that escape a transport don't need to un-stun.
                        tank.stunCrew();
                        break;
                    case 2 :
                        // this one's ridiculous.  the 'main weapon' jams.
                        Mounted mWeap = tank.getMainWeapon();
                        if (mWeap == null) {
                            desc += "\n            No main weapon crit, because no main weapon!";
                        }
                        else {
                            desc += "\n            <<<CRITICAL HIT>>> " + mWeap.getName() +
                            " jams for 1 turn.";
                            tank.setJammedWeapon(mWeap);
                        }
                        break;
                    case 3 :
                        desc += "\n            <<<CRITICAL HIT>>> Engine destroyed.  Immobile.";
                        tank.immobilize();
                        // Does the hovercraft sink?
                        Hex te_hex = game.board.getHex( en.getPosition() );
                        if ( en.getMovementType() == Entity.MovementType.HOVER &&
                             te_hex.levelOf(Terrain.WATER) > 0 ) {
                            desc += destroyEntity(en, "a watery grave", false);
                        }
                        break;
                    case 4 :
                        desc += "\n            <<<CRITICAL HIT>>> Crew killed";
                        desc += destroyEntity(en, "crew death", true);
                        en.getCrew().setDoomed(true);
                        break;
                    case 5 :
                        desc += "\n            <<<CRITICAL HIT>>> Fuel tank hit.  BOOM!";
                        desc += destroyEntity(en, "fuel tank explosion", false, false);
                        en.getCrew().setDoomed(true);
                       break;
                    case 6 :
                        desc += "\n            <<<CRITICAL HIT>>> Power plant hit.  BOOM!";
                        desc += destroyEntity(en, "power plant destruction", false, false);
                        en.getCrew().setDoomed(true);
                        break;
                }
            }
        }
        else {
            // transfer criticals, if needed
            while (hits > 0 && en.canTransferCriticals(loc)
            && en.getTransferLocation(loc) != Entity.LOC_DESTROYED) {
                loc = en.getTransferLocation(loc);
                desc += "\n            Location is empty, so criticals transfer to " + en.getLocationAbbr(loc) +".";
            }
            // roll criticals
            while (hits > 0) {
                if (en.getHittableCriticals(loc) <= 0) {
                    desc += "\n            Location has no more hittable critical slots.";
                    break;
                }
                int slot = Compute.randomInt(en.getNumberOfCriticals(loc));
                CriticalSlot cs = en.getCritical(loc, slot);
                if (cs == null || !cs.isHittable()) {
                    continue;
                }
                cs.setHit(true);
                switch(cs.getType()) {
                    case CriticalSlot.TYPE_SYSTEM :
                        desc += "\n            <<<CRITICAL HIT>>> on " + Mech.systemNames[cs.getIndex()] + ".";
                        switch(cs.getIndex()) {
                            case Mech.SYSTEM_COCKPIT :
                                // boink!
                                en.crew.setDoomed(true);
                                desc += "\n*** " + en.getDisplayName() + " PILOT KILLED! ***";
                                break;
                            case Mech.SYSTEM_ENGINE :
                                int numEngineHits = 0;
                                numEngineHits += en.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_CT);
                                numEngineHits += en.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_RT);
                                numEngineHits += en.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_LT);
                                if (numEngineHits > 2) {
                                    // third engine hit
                                    desc += destroyEntity(en, "engine destruction");
                                }
                                break;
                            case Mech.SYSTEM_GYRO :
                                if (en.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, loc) > 1) {
                                    // gyro destroyed
                                    game.addPSR(new PilotingRollData(en.getId(), PilotingRollData.AUTOMATIC_FAIL, 3, "gyro destroyed"));
                                } else {
                                    // first gyro hit
                                    game.addPSR(new PilotingRollData(en.getId(), 3, "gyro hit"));
                                }
                                break;
                            case Mech.ACTUATOR_UPPER_LEG :
                            case Mech.ACTUATOR_LOWER_LEG :
                            case Mech.ACTUATOR_FOOT :
                                // leg/foot actuator piloting roll
                                game.addPSR(new PilotingRollData(en.getId(), 1, "leg/foot actuator hit"));
                                break;
                            case Mech.ACTUATOR_HIP :
                                // hip piloting roll
                                game.addPSR(new PilotingRollData(en.getId(), 2, "hip actuator hit"));
                                break;
                        }
                        break;
                    case CriticalSlot.TYPE_EQUIPMENT :
                        Mounted mounted = en.getEquipment(cs.getIndex());
                        EquipmentType eqType = mounted.getType();
                        boolean hitBefore = mounted.isHit();
                        desc += "\n            <<<CRITICAL HIT>>> on " + mounted.getDesc() + ".";
                        mounted.setHit(true);

                        // If the item is the ECM suite of a Mek Stealth system
                        // then it's destruction turns off the stealth.
                        if ( !hitBefore && eqType instanceof MiscType &&
                             eqType.hasFlag(MiscType.F_ECM) &&
                             mounted.getLinkedBy() != null ) {
                            Mounted stealth = mounted.getLinkedBy();
                            desc += "\n       " + stealth.getType().getName() +
                               " will stop functioning at end of turn.";
                            stealth.setMode( "Off" );
                        }

                        // Handle equipment explosions.
                        if (eqType.isExplosive() && !hitBefore) {
                            desc += explodeEquipment(en, loc, slot);
                        }
                        break;
                }
                hits--;

            }
        }
        
        return desc;
    }
    
    /**
     * Marks all equipment in a location on an entity as destroyed.
     */
    private void destroyLocation(Entity en, int loc) {
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
                cs.setMissing(true);
            }
        }
        // if it's a leg, the entity falls
        if (loc == Mech.LOC_RLEG || loc == Mech.LOC_LLEG) {
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

            // Handle escape of transported units.
            Enumeration iter = entity.getLoadedUnits().elements();
            if ( iter.hasMoreElements() ) {
                Entity other = null;
                Coords curPos = entity.getPosition();
                Hex entityHex = game.getBoard().getHex( curPos );
                int curFacing = entity.getFacing();
                while ( iter.hasMoreElements() ) {
                    other = (Entity) iter.nextElement();

                    // Can the other unit survive?
                    if ( !survivable ) {

                        // Nope.
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
        if (!en.isDoomed() && !en.isDestroyed()) {
            desc.append(damageCrew(en, 2));
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
        
        int waterDepth = game.board.getHex(fallPos).levelOf(Terrain.WATER);
        int damageHeight = height;
        
        // HACK: if the dest hex is water, assume that the fall height given is
        // to the floor of the hex, and modifiy it so that it's to the surface
        if (waterDepth > 0) {
            damageHeight = Math.max(height - waterDepth, 0);
        }

        // calculate damage
        int damage = (int)Math.round(entity.getWeight() / 10.0) * (damageHeight + 1);
        
        // half damage for water falls
        if (waterDepth > 0) {
            damage = (int)Math.ceil(damage / 2.0);
        }
        
        // report falling
        phaseReport.append("    " ).append( entity.getDisplayName() ).append( " falls on its " ).append( side ).append( ", suffering " ).append( damage ).append( " damage.");

        // Any swarming infantry will be dislodged, but we don't want to
        // interrupt the fall's report.  We have to get the ID now because
        // the fall may kill the entity which will reset the attacker ID.
        final int swarmerId = entity.getSwarmAttackerId();

        // standard damage loop
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, table);
            phaseReport.append(damageEntity(entity, hit, cluster));
            damage -= cluster;
        }
        
        // pilot damage?
        roll.removeAutos();

        if (height > 0) {
            roll.addModifier(height, "height of fall");
        }
        
        if (roll.getValue() == PilotingRollData.IMPOSSIBLE) {
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
         
        // Only Mechs can fall prone.
        if ( entity instanceof Mech ) {
            entity.setProne(true);
        }
        entity.setPosition(fallPos);
        entity.setFacing((entity.getFacing() + (facing - 1)) % 6);
        entity.setSecondaryFacing(entity.getFacing());

        // Now dislodge any swarming infantry.
        if ( Entity.NONE != swarmerId ) {
            final Entity swarmer = game.getEntity( swarmerId );
            entity.setSwarmAttackerId( Entity.NONE );
            swarmer.setSwarmTargetId( Entity.NONE );
            // Did the infantry fall into water?
            Hex fallHex = game.board.getHex( fallPos );
            if ( fallHex.levelOf(Terrain.WATER) > 0 ) {
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

    /** Make fires spread, smoke spread, and make sure that all fires
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
        Board board = game.getBoard();
        int width = board.width;
        int height = board.height;
        int windDirection = game.getWindDirection();
        
        phaseReport.append("\n\nResolving fire movement \n ------------------------\n");

        // Get the position map of all entities in the game.
        Hashtable positionMap = game.getPositionMap();

        // Build vector to send for updated buildings at once.
        Vector burningBldgs = new Vector();

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

        // Cycle through all hexes, checking for fire.
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {
            
            for (int currentYCoord = 0; currentYCoord < height;
                 currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord,
                                                  currentYCoord);
                Hex currentHex = board.getHex(currentXCoord, currentYCoord);
                boolean infernoBurning = board.burnInferno( currentCoords );

                // If the woods has been cleared, or the building
                // has collapsed put non-inferno fires out.
                if ( currentHex.contains(Terrain.FIRE) && !infernoBurning &&
                     !(currentHex.contains(Terrain.WOODS)) &&
                     !(currentHex.contains(Terrain.BUILDING)) ) {
                    removeFire(currentXCoord, currentYCoord, currentHex);
                }

                // Was the fire was started on a previous turn?
                else if (currentHex.levelOf(Terrain.FIRE) == 2)
                {
                    if ( infernoBurning ) {
                        phaseReport.append( "Inferno fire at " );
                    } else {
                        phaseReport.append( "Fire at " );
                    }
                    phaseReport.append( currentCoords.getBoardNum() )
                        .append( " is burning brightly.\n" );
                    spreadFire(currentXCoord, currentYCoord, windDirection);
                }  // End the Else If Hex was on fire previously
            }  // end the loop through Y coordinates
        }  // end the loop through X coordinates
        //  Loop a second time, to set all fires to level 2 before next turn, and add smoke.
        for (int currentXCoord = 0; currentXCoord < width; currentXCoord++ ) {
            
            for (int currentYCoord = 0; currentYCoord < height; currentYCoord++) {
                Coords currentCoords = new Coords(currentXCoord, currentYCoord);
                Hex currentHex = board.getHex(currentXCoord,currentYCoord);
                // if the fire in the hex was started this turn
                if (currentHex.levelOf(Terrain.FIRE) == 1) {
                    currentHex.removeTerrain(Terrain.FIRE);
                    currentHex.addTerrain(new Terrain(Terrain.FIRE, 2));
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
                if (currentHex.contains(Terrain.FIRE)) {
                    addSmoke(currentXCoord, currentYCoord, windDirection);
                    addSmoke(currentXCoord, currentYCoord, (windDirection+1)%6);
                    addSmoke(currentXCoord, currentYCoord, (windDirection+5)%6);
                    board.initializeAround(currentXCoord,currentYCoord);
                }
            }
        }

        // If any buildings are burning, update the clients.
        if ( !burningBldgs.isEmpty() ) {
            send( createUpdateBuildingCFPacket(burningBldgs) );
        }
        
    }  // End the ResolveFire() method
    
    /**
     * Spreads the fire around the specified coordinates.
     */
    public void spreadFire(int x, int y, int windDir) {
        Coords src = new Coords(x, y);
        Coords nextCoords = src.translated(windDir);
        
        spreadFire(nextCoords, 9);
        
        // Spread to the next hex downwind on a 12 if the first hex wasn't burning...
        Hex nextHex = game.getBoard().getHex(nextCoords);
        if (nextHex != null && !(nextHex.contains(Terrain.FIRE))) {
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
     * possible and the fire roll is made.
     */
    public void spreadFire(Coords coords, int roll) {
        Hex hex = game.getBoard().getHex(coords);
        if (ignite(hex, roll)) {
            sendChangedHex(coords);
            phaseReport.append("Fire spreads to " ).append( coords.getBoardNum() ).append( "!\n");
        }
    }
    
    /**
     * Returns true if the hex is set on fire with the specified roll.  Of
     * course, also checks to see that fire is possible in the specified hex.
     */
    public boolean ignite(Hex hex, int roll, boolean bAnyTerrain) {

        // The hex might be null due to spreadFire translation
        // goes outside of the board limit.
        if ( !game.getOptions().booleanOption("fire") || null == hex ) {
            return false;
        }

        // The hex may already be on fire.
        if ( hex.contains( Terrain.FIRE ) ) {
            return true;
        }

        if ( !bAnyTerrain &&
             !(hex.contains(Terrain.WOODS)) &&
             !(hex.contains(Terrain.BUILDING)) ) {
            return false;
        }
        
        int fireRoll = Compute.d6(2);
        if (fireRoll >= roll) {
            hex.addTerrain(new Terrain(Terrain.FIRE, 1));
            return true;
        } else {
            return false;
        }
    }
    
    // default signature, assuming only woods can burn
    public boolean ignite(Hex hex, int roll) {
        return ignite(hex, roll, false);
    }
    
    
    public void removeFire(int x, int y, Hex hex) {
        Coords fireCoords = new Coords(x, y);
        int windDir = game.getWindDirection();
        hex.removeTerrain(Terrain.FIRE);
        sendChangedHex(fireCoords);
        removeSmoke(x, y, windDir);
        removeSmoke(x, y, (windDir + 1) % 6);
        removeSmoke(x, y, (windDir + 5) % 6);
        phaseReport.append("Fire at " ).append( fireCoords.getBoardNum() ).append( " goes out due to lack of fuel!\n");
    }
    
    // called when a fire is burning.  Adds smoke to hex in the direction specified.  Called 3 times per fire hex,
    public void addSmoke(int x, int y, int windDir) {
        Coords smokeCoords = new Coords(Coords.xInDir(x, y, windDir), Coords.yInDir(x, y, windDir));
        Hex nextHex = game.getBoard().getHex(smokeCoords);
        if (nextHex != null && !(nextHex.contains(Terrain.SMOKE))) {
            nextHex.addTerrain(new Terrain(Terrain.SMOKE, 1));
            sendChangedHex(smokeCoords);
            phaseReport.append("Smoke fills " ).append( smokeCoords.getBoardNum() ).append( "!\n");
        }
    }
    
    public void removeSmoke(int x, int y, int windDir) {
        Coords smokeCoords = new Coords(Coords.xInDir(x, y, windDir), Coords.yInDir(x, y, windDir));
        Hex nextHex = game.getBoard().getHex(smokeCoords);
        if (nextHex != null && nextHex.contains(Terrain.SMOKE)) {
            nextHex.removeTerrain(Terrain.SMOKE);
            sendChangedHex(smokeCoords);
            phaseReport.append("Smoke clears from " ).append( smokeCoords.getBoardNum() ).append( "!\n");
        }
    }
    
    /**
     * Scans the boards directory for map boards of the appropriate size
     * and returns them.
     */
    private Vector scanForBoards(int boardWidth, int boardHeight) {
        Vector boards = new Vector();
        
        File boardDir = new File("data/boards");
        
        // just a check...
        if (!boardDir.isDirectory()) {
            return boards;
        }
        
        // scan files
        String[] fileList = boardDir.list();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].indexOf(".board") == -1) {
                continue;
            }
            if (Board.boardIsSize(fileList[i], boardWidth, boardHeight)) {
                boards.addElement(fileList[i].substring(0, fileList[i].lastIndexOf(".board")));
            }
        }
        
        // if there are any boards, add these:
        if (boards.size() > 0) {
            boards.insertElementAt(MapSettings.BOARD_RANDOM, 0);
            boards.insertElementAt(MapSettings.BOARD_SURPRISE, 1);
        }
        
        //TODO: alphabetize files?
        
        return boards;
    }
    
    private boolean doBlind() {
        return (game.getOptions().booleanOption("double_blind") &&
        game.phase >= Game.PHASE_INITIATIVE);
    }
    
    /**
     * In a double-blind game, update only visible entities.  Otherwise,
     * update everyone
     */
    private void entityUpdate(int nEntityID) {
        if (doBlind()) {
            Entity eTarget = game.getEntity(nEntityID);
            Vector vPlayers = game.getPlayersVector();
            Vector vCanSee = whoCanSee(eTarget);
            // send an entity update to everyone who can see
            Packet pack = createEntityPacket(nEntityID);
            for (int x = 0; x < vCanSee.size(); x++) {
                Player p = (Player)vCanSee.elementAt(x);
                send(p.getId(), pack);
            }
            // send an entity delete to everyone else
            pack = createRemoveEntityPacket(nEntityID);
            for (int x = 0; x < vPlayers.size(); x++) {
                if (!vCanSee.contains(vPlayers.elementAt(x))) {
                    Player p = (Player)vPlayers.elementAt(x);
                    send(p.getId(), pack);
                }
            }
        }
        else {
            // everyone can see
            send(createEntityPacket(nEntityID));
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
     * Checks if an entity added by the client is valid and if so, adds it to the list
     */
    private void receiveEntityAdd(Packet c, int connIndex) {
        Entity entity = (Entity)c.getObject(0);
        
        entity.restore();
        entity.setOwner(getPlayer(connIndex));

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
            entity.restore();
            entity.setOwner(getPlayer(connIndex));
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
        if (e.getOwner() != getPlayer(connIndex)) {
            System.err.print
                ( "Server.receiveEntityAmmoChange: player " );
            System.err.print( getPlayer(connIndex).getName() );
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

        // Load the weapon.
        e.loadWeapon( mWeap, mAmmo );
    }

    /**
     * Deletes an entity owned by a certain player from the list
     */
    private void receiveEntityDelete(Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        Entity entity = game.getEntity(entityId);
        if (entity != null && entity.getOwner() == getPlayer(connIndex)) {
            game.removeEntity(entityId, Entity.REMOVE_NEVER_JOINED);
            send(createRemoveEntityPacket(entityId, Entity.REMOVE_NEVER_JOINED));
        } else {
            // hey! that's not your entity
        }
    }
    
    /**
     * Sets a player's ready status
     */
    private void receivePlayerDone(Packet pkt, int connIndex) {
        boolean ready = pkt.getBooleanValue(0);
        getPlayer(connIndex).setDone(ready);
    }
    
    /**
     * Sets game options, providing that the player has specified the password
     * correctly.
     *
     * @returns true if any options have been successfully changed.
     */
    private boolean receiveGameOptions(Packet packet, int connId) {
        // check password
        if (password != null && password.length() > 0 && !password.equals(packet.getObject(0))) {
            sendServerChat(connId, "The password you specified to change game options is incorrect.");
            return false;
        }
        
        int changed = 0;
        boolean infLastValue =
            game.getOptions().getOption("inf_move_last").booleanValue();
        boolean infMultiValue =
            game.getOptions().getOption("inf_move_multi").booleanValue();
        
        for (Enumeration i = ((Vector)packet.getObject(1)).elements(); i.hasMoreElements();) {
            GameOption option = (GameOption)i.nextElement();
            GameOption originalOption = game.getOptions().getOption(option.getShortName());
            
            if (originalOption == null) {
                continue;
            }
            
            sendServerChat("Player " + getPlayer(connId).getName() + " changed option \"" + originalOption.getFullName() + "\" to " + option.stringValue() + ".");

            // Record mutually-exclusive infantry move options.
            if ( option.getShortName().equals("inf_move_last") ) {
                infLastValue = option.booleanValue();
            }
            else if ( option.getShortName().equals("inf_move_multi") ) {
                infMultiValue = option.booleanValue();
            }

            originalOption.setValue(option.getValue());
            changed++;
        }

        // Infantry move options can't BOTH be on!!!
        if ( infLastValue && (infLastValue == infMultiValue) ) {
            sendServerChat("Player " + getPlayer(connId).getName() + " tried to set BOTH \"" + game.getOptions().getOption("inf_move_last").getFullName() + "\" and \""  + game.getOptions().getOption("inf_move_multi").getFullName() + "\" to true.");
            sendServerChat("Clearing *BOTH* options.");
            game.getOptions().getOption("inf_move_last").setValue(false);
            game.getOptions().getOption("inf_move_multi").setValue(false);
            changed += 2;
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
            
            send(createPlayerUpdatePacket(player.getId()));
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
        final Entity entity = game.getEntity(entityId);
        final Object[] data = new Object[2];
        data[0] = new Integer(entityId);
        data[1] = entity;
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
     *          <code>Game.UNIT_SALVAGEABLE</code>, or
     *          <code>Game.UNIT_DEVASTATED</code> or an
     *          <code>IllegalArgumentException</code> will be thrown.
     * @return  A <code>Packet</code> to be sent to clients.
     */
    private Packet createRemoveEntityPacket(int entityId, int condition) {
        if ( condition != Entity.REMOVE_UNKNOWN &&
             condition != Entity.REMOVE_IN_RETREAT &&
             condition != Entity.REMOVE_SALVAGEABLE &&
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
    private Packet createHexChangePacket(Coords coords, Hex hex) {
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
        getClient(connId).send(packet);
    }
    
    /**
     * Send a packet to a pending connection
     */
    private void sendToPending(int connId, Packet packet) {
        getPendingConnection(connId).send(packet);
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
 
    /**
     * Calculate the piloting skill roll modifier, based upon the number
     * of hexes moved this phase.
     */
    private int getMovementPSRModifier( int distance ) {
        if ( distance > 10 ) // 11+ hexes
            return 4;
        else if ( distance > 7 ) // 8-10 hexes
            return 2;
        else if ( distance > 4 ) // 5-7 hexes
            return 1;
        else if ( distance > 2 ) // 3-4 hexes
            return 0;
        return -1; // 0-2 hexes
    }

    /**
     * Process a packet
     */
    synchronized void handle(int connId, Packet packet) {
        //System.out.println("s(" + cn + "): received command");
        if (packet == null) {
            System.out.println("server.connection.handle: got null packet");
            return;
        }
        // act on it
        switch(packet.getCommand()) {
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
            case Packet.COMMAND_CHAT :
                String chat = (String)packet.getObject(0);
                if (chat.startsWith("/")) {
                    processCommand(connId, chat);
                } else {
                    sendChat(getPlayer(connId).getName(), chat);
                }
                break;
            case Packet.COMMAND_ENTITY_MOVE :
                receiveMovement(packet, connId);
                break;
            case Packet.COMMAND_ENTITY_DEPLOY :
                receiveDeployment(packet, connId);
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
                mapSettings = (MapSettings)packet.getObject(0);
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
        PilotingRollData psr = null;
        switch ( bldg.getType() ) {
        case Building.LIGHT:
            psr = new PilotingRollData
                ( entity.getId(), 0, why + " Light " + bldg.getName() );
            break;
        case Building.MEDIUM:
            psr = new PilotingRollData
                ( entity.getId(), 1, why + " Medium " + bldg.getName() );
            break;
        case Building.HEAVY:
            psr = new PilotingRollData
                ( entity.getId(), 2, why + " Heavy " + bldg.getName() );
            break;
        case Building.HARDENED:
            psr = new PilotingRollData
                ( entity.getId(), 5, why + " Hardened " + bldg.getName() );
            break;
        }

        // Modify the roll by the distance moved so far.      
        if (distance >= 3 && distance <= 4) {
            psr.addModifier(1, "moved 3-4 hexes");
        } else if (distance >= 5 && distance <= 6) {
            psr.addModifier(2, "moved 5-6 hexes");
        } else if (distance >= 7 && distance <= 9) {
            psr.addModifier(3, "moved 7-9 hexes");
        } else if (distance >= 10) {
            psr.addModifier(4, "moved 10+ hexes");
        }

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
                final Hex curHex = game.board.getHex( coords );
                final int hexElev = curHex.surface();
                final int numFloors = curHex.levelOf( Terrain.BLDG_ELEV );

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
                final Hex curHex = game.board.getHex( coords );
                final int hexElev = curHex.surface();
                final int numFloors = curHex.levelOf( Terrain.BLDG_ELEV );

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
                        int next = Math.max( cluster, remaining );
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
                        PilotingRollData psr = Compute.getBasePilotingRoll
                            ( this.game, entity.getId() );
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
     * Tell the clients to update the CFs of the given building.
     *
     * @param   bldg - the <code>Building</code> that has collapsed.
     * @return  a <code>Packet</code> for the command.
     */
    private Packet createUpdateBuildingCFPacket( Building bldg ) {
        Vector buildings = new Vector();
        buildings.addElement( bldg );
        return this.createUpdateBuildingCFPacket( buildings );
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

}
