/**
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

import megamek.common.*;
import megamek.common.actions.*;

/**
 * @author Ben Mazur
 */
public class Server
    implements Runnable
{
    // server setup
    private String              name;
    private ServerSocket        serverSocket;

    // game info
    private Vector              connections = new Vector(4);
    private Vector              connectionsPending = new Vector(4);
    private Hashtable           connectionIds = new Hashtable();

    private int                 connectionCounter = 0;
    private int                 entityCounter = 0;

    private Game                game = new Game();

    // list of turns and whose turn it is
    private int                 roundCounter = 0;
    private int[]               turns;
    private int                 ti;

    // stuff for the current turn
    private GameSettings        gameSettings = new GameSettings();
    private Vector              attacks = new Vector();
    private Vector              pendingCharges = new Vector();
    private Vector              pilotRolls = new Vector();

    private StringBuffer        roundReport = new StringBuffer();
    private StringBuffer        phaseReport = new StringBuffer();

    // listens for and connects players
    private Thread              connector;

    /**
     * Construct a new GameHost and begin listening for
     * incoming clients.
     */
    public Server(String name, int port) {
        this.name = name;
        // initialize server socket
        try {
            serverSocket = new ServerSocket(port);
        } catch(IOException ex) {
            System.err.println("could not create server socket on port " + port);
        }

        game.phase = Game.PHASE_LOUNGE;

        // display server start text
        System.out.println("s: starting a new server...");
        System.out.println("s: address = " + serverSocket.getInetAddress().getHostAddress() + " port = " + serverSocket.getLocalPort());

        connector = new Thread(this);
        connector.start();
    }

    /**
     * Sent when a clients attempts to connect.
     */
    private void greeting(int cn) {
        // send server info -- client should reply with client info.
        sendToPending(cn, new Packet(Packet.COMMAND_SERVER_NAME, name));
    }

    /**
     * Recieves a client name, sent from a pending connection as a signal to
     * connect.
     */
    private void receiveClientName(Packet packet, int connId) {
        final Connection conn = getPendingConnection(connId);

        // this had better be from a pending connection
        if (conn == null) {
            System.out.println("server: got a client name from a non-pending connection");
            return;
        }

        // right, switch the connection into the "active" bin
        connectionsPending.removeElement(conn);
        connections.addElement(conn);
        connectionIds.put(new Integer(conn.getId()), conn);

        // add and validate the player info
        game.addPlayer(connId, new Player(connId, (String)packet.getObject(0)));
        validatePlayerInfo(connId);

        // send info that the player has connected
        send(createPlayerConnectPacket(connId));

        // tell them their local playerId
        send(connId, new Packet(Packet.COMMAND_LOCAL_PN, new Integer(connId)));

        // send current game info
        transmitAllPlayerConnects(connId);
        send(connId, createSettingsPacket());
        send(connId, new Packet(Packet.COMMAND_PHASE_CHANGE, new Integer(game.phase)));
        send(connId, createEntitiesPacket());

        System.out.println("s: player " + connId
                           + " (" + getPlayer(connId).getName() + ") connected from "
                           + getClient(connId).socket.getInetAddress());
        sendChatToAll("***Server", getPlayer(connId).getName() + " connected from "
                           + getClient(connId).socket.getInetAddress());
    }

    /**
     * Validates the player info.
     */
    public void validatePlayerInfo(int playerId) {
        //TODO: remove unsavory characters from the name
        //TODO: check for duplicate or reserved names

        // make sure colorIndex is unique
        boolean[] colorUsed = new boolean[Player.colorNames.length];
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if (player.getId() != playerId) {
                colorUsed[player.getColorIndex()] = true;
            }
        }
        if (colorUsed[getPlayer(playerId).getColorIndex()]) {
            // find a replacement color;
            for (int i = 0; i < colorUsed.length; i++) {
                if (!colorUsed[i]) {
                    getPlayer(playerId).setColorIndex(i);
                }
            }
        }

    }

    /**
     * Called when it is sensed that a connection has terminated.
     */
    private void disconnected(int connId) {
        final Connection conn = getClient(connId);
        final Player player = getPlayer(connId);

        conn.die();

        connections.removeElement(conn);
        connectionIds.remove(new Integer(connId));

        // in the lounge, just remove all entities for that player
        if (game.phase == Game.PHASE_LOUNGE) {
            removeAllEntitesOwnedBy(player);
            send(createEntitiesPacket());
        }

        // if a player has active entities, he becomes a ghost
        if (game.getEntitiesOwnedBy(player) > 0) {
            player.setGhost(true);
            send(createPlayerUpdatePacket(player.getId()));
        } else {
            game.removePlayer(player.getId());
            send(new Packet(Packet.COMMAND_PLAYER_REMOVE, new Integer(player.getId())));
        }

        System.out.println("s: player " + connId + " disconnected");
        sendChatToAll("***Server", player.getName() + " disconnected.");
    }

    /**
     * Shortcut to game.getPlayer(id)
     */
    public Player getPlayer(int id) {
        return game.getPlayer(id);
    }

    /**
     * Counts up how many non-ghost, non-observer players are connected.
     */
        private int countActivePlayers() {
        int count = 0;

        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            if (!player.isGhost() && !player.isObserver()) {
                count++;
            }
        }

        return count;
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

            game.removeEntity(entity.getId());
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
    private Connection getConnection(int connId) {
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
     * Resets the server back to the lounge
     */
    private void resetServer() {
        //game.entities.removeAllElements();
        changePhase(Game.PHASE_LOUNGE);
    }

    /**
     * Are we out of turns (done with the phase?)
     */
    private boolean areMoreTurns() {
        return ti < turns.length;
    }

    /**
     * Returns the player number of who gets the next turn,
     * or -1 if we're done.
     */
    private int nextTurn() {
        if (ti < turns.length) {
            return turns[ti++];
        } else {
            return -1;
        }
    }

    /**
     * Called at the beginning of each game round to reset values on this entity
     * that are reset every round
     */
    private void resetEntityRound() {
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity entity = (Entity)e.nextElement();

            entity.delta_distance = 0;
            entity.moved = Entity.MOVE_NONE;
            
            entity.setCharging(false);
            entity.setMakingDfa(false);

            entity.crew.setKoThisRound(false);

            for (Enumeration i = entity.weapons.elements(); i.hasMoreElements();) {
                MountedWeapon w = (MountedWeapon)i.nextElement();
                w.setFiredThisRound(false);
            }
        }
    }

    /**
     * Called at the beginning of each phase.  Sets and resets
     * any entity parameters that need to be reset.
     */
    private void resetEntityPhase() {
    // first, mark doomed entities as destroyed and move them to the graveyard
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            if (entity.isDoomed()) {
                entity.setDestroyed(true);
            }
            if (entity.isDestroyed()) {
                game.moveToGraveyard(entity.getId());
            }
        }

        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();

            // if ammo has exploded, empty the bin
            for (Enumeration i = entity.ammo.elements(); i.hasMoreElements();) {
                final Ammo a = (Ammo)i.nextElement();
                if (a.exploded) {
                    a.shots = 0;
                    a.exploded = false;
                }
            }

            // weapons are readied, except destroyed ones.
            for (Enumeration i = entity.weapons.elements(); i.hasMoreElements();) {
               final MountedWeapon w = (MountedWeapon)i.nextElement();

                // first, if a weapon isn't destroyed, it's okay.
                boolean weaponOK = !w.isDestroyed();

                // does the weapon use ammo?
                if (weaponOK && w.getType().getAmmoType() != Ammo.TYPE_NA) {
                    // try to reload if needed
                    if (w.getAmmoFeed() == null || w.getAmmoFeed().shots <= 0) {
                        entity.loadWeapon(w);
                    }
                    // if still out of shots, weapon is useless
                    if (w.getAmmoFeed() == null || w.getAmmoFeed().shots <= 0) {
                        weaponOK = false;
                    }
                }

                // ready it if it's still okay
                w.setReady(weaponOK);
            }

            // destroy doomed criticals
            for (int i = 0; i < entity.locations(); i++) {
                for (int j = 0; j < entity.getNumberOfCriticals(i); j++) {
                    final CriticalSlot cs = entity.getCritical(i, j);
                    if (cs != null) {
                        cs.setDestroyed(cs.isDestroyed() | cs.isDoomed());
                    }
                }
            }
            // reset damage this phase
            entity.damageThisPhase = 0;

            // reset ready to true
            entity.ready = entity.isActive();
        }
    }

    /**
     * Resets an entity's secondary facing to face forwards
     */
    private void resetEntityFacing() {
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity entity = (Entity)e.nextElement();
            entity.setSecondaryFacing(entity.getFacing());
        }
    }

    /**
     * Called at the beginning of certain phases to make
     * every player not ready.
     */
    private void resetPlayerReady() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            player.setReady(false);
        }
    }

    /**
     * Called at the beginning of certain phases to make
     * every active player not ready.
     */
    private void resetActivePlayersReady() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            player.setReady(game.getEntitiesOwnedBy(player) <= 0);

        }
        transmitAllPlayerReadys();
    }

    /**
     * Called when a player is "ready".  Handles any moving
     * to the next turn or phase or that stuff.
     */
    private void checkReady() {
        // are there any active players?
        boolean allAboard = countActivePlayers() > 0;
        // check if all active players are ready
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if (!player.isReady()) {
                allAboard = false;
            }
        }
        // now, do something about it.
        switch(game.phase) {
        case Game.PHASE_LOUNGE :
        case Game.PHASE_EXCHANGE :
        case Game.PHASE_INITIATIVE :
        case Game.PHASE_MOVEMENT_REPORT :
        case Game.PHASE_FIRING_REPORT :
        case Game.PHASE_END :
            if (allAboard) {
                endCurrentPhase();
            }
            break;
        case Game.PHASE_MOVEMENT :
        case Game.PHASE_FIRING :
        case Game.PHASE_PHYSICAL :
            if (!areMoreTurns()) {
                endCurrentPhase();
            } else if (getPlayer(game.getTurn()).isReady()) {
                changeTurn(nextTurn());
            }
            break;
        }
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
        case Game.PHASE_EXCHANGE :
            gameSettings.friendlyFire = game.getNoOfPlayers() <= 1;
            resetPlayerReady();
            // apply board layout settings to produce a mega-board
            Board[] sheetBoards = new Board[gameSettings.sheetWidth * gameSettings.sheetHeight];
            for (int i = 0; i < gameSettings.sheetWidth * gameSettings.sheetHeight; i++) {
                sheetBoards[i] = new Board();
                sheetBoards[i].load(getRandomBoard());
            }
            game.board.combine(gameSettings.boardWidth, gameSettings.boardHeight,
                    gameSettings.sheetWidth, gameSettings.sheetHeight, sheetBoards);
            // deploy all entities
            Coords center = new Coords(game.board.width / 2, game.board.height / 2);
            for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
                Entity entity = (Entity)i.nextElement();
                deploy(entity, getStartingCoords(entity.getOwner().getStartingPos()), center, 10);
            }
            break;
        case Game.PHASE_INITIATIVE :
            // remove the last traces of last round
            attacks.removeAllElements();
            roundReport = new StringBuffer();
            resetEntityPhase();
            resetEntityRound();
            // roll 'em
            resetActivePlayersReady();
            rollInitiative();
            setIneligible(phase);
            determineTurnOrder();
            writeInitiativeReport();
            send(createReportPacket());
            break;
        case Game.PHASE_MOVEMENT :
        case Game.PHASE_FIRING :
            resetEntityFacing();
        case Game.PHASE_PHYSICAL :
            resetEntityPhase();
            setIneligible(phase);
            determineTurnOrder();
            resetActivePlayersReady();
            send(createEntitiesPacket());
            phaseReport = new StringBuffer();
            break;
        case Game.PHASE_END :
            phaseReport = new StringBuffer();
            resetEntityPhase();
            resolveHeat();
            checkForSuffocation();
            resolveCrewDamage();
            resolveCrewWakeUp();
            if (phaseReport.length() > 0) {
                roundReport.append(phaseReport.toString());
            }
        case Game.PHASE_MOVEMENT_REPORT :
        case Game.PHASE_FIRING_REPORT :
            resetActivePlayersReady();
            send(createReportPacket());
            break;
        }
    }

    /**
     * Should we play this phase or skip it?  The only phases we'll skip
     * are the firing or the physical phase if no entities are eligible.
     */
    private boolean isPhasePlayable(int phase) {
        switch (phase) {
        case Game.PHASE_MOVEMENT :
        case Game.PHASE_FIRING :
        case Game.PHASE_PHYSICAL :
            return areMoreTurns();
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
            // transmit the board to everybody
            send(createBoardPacket());
            break;
        case Game.PHASE_MOVEMENT :
        case Game.PHASE_FIRING :
        case Game.PHASE_PHYSICAL :
            // set turn
            ti = 0;
            changeTurn(nextTurn());
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
        case Game.PHASE_INITIATIVE :
            changePhase(Game.PHASE_MOVEMENT);
            break;
        case Game.PHASE_MOVEMENT :
            roundReport.append("\nMovement Phase\n-------------------\n");
            resolveCrewDamage();
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
            resolveWeaponAttacks();
            checkFor20Damage();
            resolveCrewDamage();
            resolvePilotingRolls();
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
            // check phase report
            if (phaseReport.length() > 0) {
                roundReport.append(phaseReport.toString());
            } else {
                roundReport.append("<nothing>\n");
            }
            changePhase(Game.PHASE_END);
            break;
        case Game.PHASE_END :
            changePhase(Game.PHASE_INITIATIVE);
            break;
        }
    }

    /**
     * Changes it to make it the specified player's turn.
     */
    private void changeTurn(int turn) {
        final Player player = getPlayer(game.getTurn());

        game.setTurn(turn);
        player.setReady(false);
        send(new Packet(Packet.COMMAND_TURN, new Integer(turn)));
    }

    /**
     * Deploys an entity near a selected point on the board.
     *
     * @param entity the entity to deploy
     * @param pos the point to deploy near
     * @param towards another point that the deployed mechs will face towards
     */
    private boolean deploy(Entity entity, Coords pos, Coords towards, int recurse) {
        if (game.board.contains(pos) && game.getEntity(pos) == null) {
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
     * Rolls initiative for all the players.
     */
    private void rollInitiative() {
        roundCounter++;

        // roll the dice for each player
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            player.clearInitiative();
            player.setInitiative(Compute.d6(2), 0);
        }
        
        // roll off initiative ties, up to 5 times
        // TODO: infinite rerolls
        for (int i = 0; i < 5; i++) {
            for (int j = 2; j <= 12; j++) {
                for (Enumeration k = game.getPlayers(); k.hasMoreElements();) {
                    final Player player = (Player)k.nextElement();
                    if (player.getInitiativeSize() > i && player.getInitiative(i) == j && isInitTie(j, i)) {
                        player.setInitiative(Compute.d6(2), i + 1);
                    }
                }
            }
        }
        
        transmitAllPlayerUpdates();
    }
    
    private boolean isInitTie(int init, int index) {
        int playersAt = 0;
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            if (player.getInitiativeSize() > index && player.getInitiative(index) == init) {
                playersAt++;
            }
        }
        return playersAt > 1;
    }
    
    
    /**
     * Determine turn order by number of entities that are selectable this phase
     */
    private void determineTurnOrder() {
        // determine turn order
        int[] order = new int[game.getNoOfPlayers()];
        int oi = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 2; j <= 12; j++) {
                for (Enumeration k = game.getPlayers(); k.hasMoreElements();) {
                    final Player player = (Player)k.nextElement();
                    if (player.getInitiativeSize() > i && player.getInitiative(i) == j && !isInitTie(j, i)) {
                        order[oi++] = player.getId();
                    }
                }
            }
        }
        
        // count how many entities each player controls, and how many turns we have to assign
        int MAX_PLAYERS = 255; //XXX HACK HACK HACK!
        int[] noe = new int[MAX_PLAYERS];
        int noOfTurns = 0;
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            final Entity entity = (Entity)e.nextElement();
            if (entity.isSelectable()) {
                noe[entity.getOwner().getId()]++;
                noOfTurns++;
            }
        }

        // generate turn list
        turns = new int[noOfTurns];
        ti = 0;
        while (ti < turns.length){
            // get lowest number of entities, minimum 1.
            int hnoe = 1;
            int lnoe = Integer.MAX_VALUE;
            for (int i = 0; i < MAX_PLAYERS; i++) {
                    if (noe[i] > 0 && noe[i] < lnoe) {
                            lnoe = noe[i];
                    }
                    if (noe[i] > hnoe) {
                            hnoe = noe[i];
                    }
            }
            // cycle through order list
            for (int i = 0; i < order.length; i++) {
                if (noe[order[i]] <= 0) {
                    continue;
                }
                // if you have less than twice the next lowest,
                // move 1, otherwise, move more.
                // if you have less than half the maximum,
                // move none
                int ntm = Math.max(1, (int)Math.floor(noe[order[i]] / lnoe));
                for (int j = 0; j < ntm; j++) {
                    turns[ti++] = order[i];
                    noe[order[i]]--;
                }
            }
        }
        // reset turn counter
        ti = 0;
    }

    /**
     * Write the initiative results to the report
     */
    private void writeInitiativeReport() {
        // write to report
        roundReport.append("\nInitiative Phase for Round #" + roundCounter
                           + "\n------------------------------\n");
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();
            roundReport.append(player.getName() + " rolls a ");
            boolean first = true;
            for (Enumeration j = player.getInitiatives(); j.hasMoreElements();) {
                Integer init = (Integer)j.nextElement();
                if (first) {
                    first = false;
                } else {
                    roundReport.append(" / ");
                }
                roundReport.append(init.toString());
            }
            roundReport.append(".\n");
        }
        roundReport.append("\nThe turn order is:\n  ");
        for (int i = 0; i < turns.length; i++) {
            roundReport.append((i == 0 ? "" : ", ") + getPlayer(turns[i]).getName());
        }
        roundReport.append("\n");

        // reset turn index
        ti = 0;
    }

    /**
     * Marks ineligible entities as not ready for this phase
     */
    private void setIneligible(int phase) {
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity entity = (Entity)e.nextElement();
            if (!isEligibleFor(entity, phase)) {
                entity.ready = false;
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
            return isEligibleForPhysical(entity, phase);
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
        if (entity.isCharging() || entity.isMakingDfa()) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if the entity has any valid targets for physical attacks.
     */
    private boolean isEligibleForPhysical(Entity entity, int phase) {
        boolean canHit = false;
        
        // if you're charging, it's already declared
        if (entity.isCharging() || entity.isMakingDfa()) {
            return false;
        }

        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity target = (Entity)e.nextElement();

            // don't hit yourself, please
            if (target.equals(entity)) {
                continue;
            }

            // don't hit your own guys with friendly fire
            if (!gameSettings.friendlyFire
                && target.getOwner().equals(entity.getOwner())) {
                continue;
            }
            
            canHit |= Compute.toHitPunch(game, entity.getId(), target.getId(),
                                         PunchAttackAction.LEFT).getValue()
                      != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitPunch(game, entity.getId(), target.getId(),
                                         PunchAttackAction.RIGHT).getValue()
                      != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitKick(game, entity.getId(), target.getId(),
                                        KickAttackAction.LEFT).getValue()
                      != ToHitData.IMPOSSIBLE;

            canHit |= Compute.toHitKick(game, entity.getId(), target.getId(),
                                        KickAttackAction.RIGHT).getValue()
                      != ToHitData.IMPOSSIBLE;
        }

        return canHit;
    }

    /**
     * Steps thru an entity movement packet, executing it.
     */
    private void doEntityMovement(Packet c, int cn) {
        final MovementData md = (MovementData)c.getObject(1);
        // walk thru data, stopping when the mech is out of movement
        final Entity entity = game.getEntity(c.getIntValue(0));
        Coords lastPos = entity.getPosition();
        Coords curPos = entity.getPosition();
        int curFacing = entity.getFacing();
        int distance = 0;
        int moveType = Entity.MOVE_NONE;
        int overallMoveType = Entity.MOVE_NONE;
        boolean firstStep;

        Compute.compile(game, entity.getId(), md);

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
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();

            // stop for illegal movement
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            }

            // check piloting skill for getting up
            if (step.getType() == MovementData.STEP_GET_UP) {
                entity.setProne(false);
                doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 0, "getting up"));
                entity.heatBuildup += 1;
            } else if (firstStep) {
                // running with destroyed hip or gyro needs a check
                if (overallMoveType == Entity.MOVE_RUN
                        && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0
                            || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, Mech.LOC_RLEG) > 0
                            || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, Mech.LOC_LLEG) > 0)) {
                    doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 0, "running with damaged hip actuator or gyro"));
                }
                firstStep = false;
            }

            // did the entity just fall?
            if (entity.isProne()) {
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                break;
            }
            
            // check for charge
            if (step.getType() == MovementData.STEP_CHARGE) {
                Entity target = game.getEntity(step.getPosition());

                distance = step.getDistance();
                entity.setCharging(true);
                
                pendingCharges.addElement(new ChargeAttackAction(entity.getId(), target.getId(), target.getPosition()));
                break;
            }

            // check for dfa
            if (step.getType() == MovementData.STEP_DFA) {
                Entity target = game.getEntity(step.getPosition());

                distance = step.getDistance();
                entity.setMakingDfa(true);
                
                pendingCharges.addElement(new DfaAttackAction(entity.getId(), target.getId(), target.getPosition()));
                break;
            }

            // step...
            moveType = step.getMovementType();
            curPos = step.getPosition();
            curFacing = step.getFacing();
            distance = step.getDistance();

            final Hex curHex = game.board.getHex(curPos);

            // check if we've moved into rubble
            if (!lastPos.equals(curPos)
                    && step.getMovementType() != Entity.MOVE_JUMP
                    && (curHex.getTerrainType() == Terrain.RUBBLE)) {
                doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), 0, "entering Rubble"));
            }

            // check if we've moved into water
            if (!lastPos.equals(curPos)
                    && step.getMovementType() != Entity.MOVE_JUMP
                    && curHex.getTerrainType() == Terrain.WATER
                    && curHex.getElevation() < 0) {
                if (curHex.getElevation() == -1) {
                    doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), -1, "entering Depth 1 Water"));
                } else if (curHex.getElevation() == -2) {
                    doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), 0, "entering Depth 2 Water"));
                } else {
                    doSkillCheckWhileMoving(entity, lastPos, curPos, new PilotingRollData(entity.getId(), 1, "entering Depth 3+ Water"));
                }
            }

            // did the entity just fall?
            if (entity.isProne()) {
                curFacing = entity.getFacing();
                curPos = entity.getPosition();
                break;
            }

            // update lastPos
            lastPos = new Coords(curPos);
        }

        // set entity parameters
        entity.setPosition(curPos);
        entity.setFacing(curFacing);
        entity.setSecondaryFacing(curFacing);
        entity.delta_distance = distance;
        entity.moved = moveType;

        // but the danger isn't over yet!  landing from a jump can be risky!
        if (overallMoveType == Entity.MOVE_JUMP
                && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, Mech.LOC_RLEG) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, Mech.LOC_RLEG) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, Mech.LOC_LLEG) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG) > 0
                    || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, Mech.LOC_LLEG) > 0)) {
            doSkillCheckInPlace(entity, new PilotingRollData(entity.getId(), 0, "landing with damaged leg actuator or gyro"));
        }

        // build up heat from movement
        if (moveType == Entity.MOVE_WALK) {
            entity.heatBuildup += 1;
        } else if (moveType == Entity.MOVE_RUN) {
            entity.heatBuildup += 2;
        } else if (moveType == Entity.MOVE_JUMP) {
            entity.heatBuildup += Math.max(3, distance);
        }

        entity.ready = false;

        // duhh.. send an outgoing packet to everybody
        send(createEntityPacket(entity.getId()));
    }

    /**
     * Do a piloting skill check while standing still (during the movement phase)
     */
    private void doSkillCheckInPlace(Entity entity, PilotingRollData reason) {
        final PilotingRollData roll = Compute.getBasePilotingRoll(game, entity.getId());

        // append the reason modifier
        roll.addModifier(reason.getValue(), reason.getDesc());

        // okay, print the info
        phaseReport.append("\n" + entity.getDisplayName()
                   + " must make a piloting skill check (" + reason.getDesc() + ")"
                   + ".\n");
        // roll
        final int diceRoll = Compute.d6(2);
        phaseReport.append("Needs " + roll.getValue()
                   + " (" + roll.getDesc() + ")"
                   + ", rolls " + diceRoll + " : ");
        if (diceRoll < roll.getValue()) {
            phaseReport.append("falls.\n");
            doEntityFall(entity, roll.getValue());
        } else {
            phaseReport.append("succeeds.\n");
        }
    }

    /**
     * Do a piloting skill check while moving
     */
    private void doSkillCheckWhileMoving(Entity entity, Coords src, Coords dest,
                                         PilotingRollData reason) {
        final PilotingRollData roll = Compute.getBasePilotingRoll(game, entity.getId());
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        boolean fallsInPlace;
        int fallElevation;

        // append the reason modifier
        roll.addModifier(reason.getValue(), reason.getDesc());

        // will the entity fall in the source or destination hex?
        if (src.equals(dest) || srcHex.getElevation() < destHex.getElevation()) {
            fallsInPlace = true;
        } else {
            fallsInPlace = false;
        }

        // how far down did it fall?
        fallElevation = Math.abs(destHex.getElevation() - srcHex.getElevation());
        
        // okay, print the info
        phaseReport.append("\n" + entity.getDisplayName()
                + " must make a piloting skill check"
                + " while moving from hex " + src.getBoardNum()
                + " to hex " + dest.getBoardNum()
                + " (" + reason.getDesc() + ")" + ".\n");
        // roll
        final int diceRoll = Compute.d6(2);
        phaseReport.append("Needs " + roll.getValue()
                + " (" + roll.getDesc() + ")"
                + ", rolls " + diceRoll + " : ");
        if (diceRoll < roll.getValue()) {
            phaseReport.append("falls.\n");
            doEntityFallsInto(entity, (fallsInPlace ? src : dest), (fallsInPlace ? dest : src), roll.getValue());
            //doEntityFall(entity, (fallsInPlace ? src : dest), fallElevation, roll.getValue());
        } else {
            phaseReport.append("succeeds.\n");
        }
    }

    /**
     * The entity falls into the hex specified.  Check for any conflicts and
     * resolve them.  Deal damage to faller.
     */
    private void doEntityFallsInto(Entity entity, Coords src, Coords dest, int roll) {
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final int fallElevation = Math.abs(destHex.getElevation() - srcHex.getElevation());
        int direction = src.direction(dest);
        // check entity in target hex
        Entity target = game.getEntity(dest);
        // check if we can fall in that hex
        if (target != null && !target.equals(entity)
            && !Compute.isValidDisplacement(game, target.getId(), src, dest)) {
            // if target can't be displaced, fall in source hex.
            // NOTE: source hex should never contain a non-displacable entity
            Coords temp = dest;
            dest = src;
            src = temp;
            target = game.getEntity(dest);
        }

        // falling mech falls
        phaseReport.append(entity.getDisplayName() + " falls "
                + fallElevation + " level(s) into hex "
                + dest.getBoardNum() + ".\n");

        // if hex was empty, deal damage and we're done
        if (target == null || target.equals(entity)) {
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
            doEntityDisplacement(target, dest, dest.translated(direction), new PilotingRollData(target.getId(), 0, "domino effect"));
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
        int fallElevation = srcHex.getElevation() - destHex.getElevation();
        Entity target = game.getEntity(dest);

        // can't fall upwards
        if (fallElevation < 0) {
            fallElevation = 0;
        }

        // if destination is empty, this could be easy...
        if (target == null || target.equals(entity)) {
            if (fallElevation < 2) {
                // no cliff: move and roll normally
                phaseReport.append(entity.getDisplayName()
                           + " is displaced into hex "
                           + dest.getBoardNum() + ".\n");
                entity.setPosition(dest);
                if (roll != null) {
                    pilotRolls.addElement(roll);
                }
                return;
            } else {
                // cliff: fall off it, deal damage, prone immediately
                phaseReport.append(entity.getDisplayName() + " falls "
                           + fallElevation + " levels into hex "
                           + dest.getBoardNum() + ".\n");
                doEntityFall(entity, dest, fallElevation, PilotingRollData.AUTOMATIC_FALL);
                return;
            }
        }

        // okay, destination occupied.  hmmm...
        System.err.println("server.doEntityDisplacement: destination occupied");
        if (fallElevation < 2) {
            // domino effect: move & displace target
            phaseReport.append(entity.getDisplayName()
                           + " is displaced into hex "
                           + dest.getBoardNum() + ", occupied by "
                           + target.getDisplayName() + ".\n");
            entity.setPosition(dest);
            if (roll != null) {
                pilotRolls.addElement(roll);
            }
            doEntityDisplacement(target, dest, dest.translated(direction), new PilotingRollData(target.getId(), 0, "domino effect"));
            return;
        } else {
            // accidental fall from above: havoc!
            phaseReport.append(entity.getDisplayName() + " falls "
                           + fallElevation + " levels into hex "
                           + dest.getBoardNum() + ", occupied by "
                           + target.getDisplayName() + ".\n");

            // determine to-hit number
            ToHitData toHit = new ToHitData(7, "7 (base)");
            toHit.append(Compute.getTargetMovementModifier(game, target.getId()));
            toHit.append(Compute.getTargetTerrainModifier(game, target.getId()));

            // roll dice
            final int diceRoll = Compute.d6(2);
            phaseReport.append("Collision occurs on a " + toHit.getValue()
                           + " or greater.  Rolls " + diceRoll);
            if (diceRoll >= toHit.getValue()) {
                phaseReport.append(", hits!\n");
                // deal damage to target
                int damage = (int)Math.ceil(entity.getWeight() / 10);
                phaseReport.append(target.getDisplayName() + " takes "
                                   + damage + " from the collision.");
                while (damage > 0) {
                    int cluster = Math.min(5, damage);
                    HitData hit = entity.rollHitLocation(ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT);
                    phaseReport.append(damageEntity(target, hit, cluster));
                    damage -= cluster;
                }
                phaseReport.append("\n");

                // attacker falls as normal, on his back
                doEntityFall(entity, dest, fallElevation, 3, PilotingRollData.AUTOMATIC_FALL);

                // defender pushed away, or destroyed
                Coords targetDest = Compute.getValidDisplacement(game, target.getId(), dest, direction);
                if (targetDest != null) {
                    doEntityDisplacement(target, dest, targetDest, new PilotingRollData(target.getId(), 2, "fallen on"));
                } else {
                    // ack!  automatic death!
                    phaseReport.append("*** " + target.getDisplayName()
                                       + " DESTROYED due to impossible displacement! ***");
                    target.setDoomed(true);
                }
            } else {
                phaseReport.append(", misses.\n");
                //TODO: this is not quite how the rules go
                Coords targetDest = Compute.getValidDisplacement(game, entity.getId(), dest, direction);
                if (targetDest != null) {
                    doEntityDisplacement(entity, src, targetDest, new PilotingRollData(entity.getId(), PilotingRollData.AUTOMATIC_FALL, "pushed off a cliff"));
                } else {
                    // ack!  automatic death!
                    phaseReport.append("*** " + entity.getDisplayName()
                                       + " DESTROYED due to impossible displacement! ***");
                    entity.setDoomed(true);
                }
            }
            return;
        }
    }

    /**
     * Gets a bunch of entity actions from the packet
     */
    private void receiveAttack(Packet pkt) {
        Vector vector = (Vector)pkt.getObject(1);
        Entity entity = game.getEntity(pkt.getIntValue(0));
        for (Enumeration i = vector.elements(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();

            // add to the list.
            attacks.addElement(ea);

            // if torso twist, twist so that everybody can see it later
            if (ea instanceof TorsoTwistAction) {
                TorsoTwistAction tta = (TorsoTwistAction)ea;
                game.getEntity(tta.getEntityId()).setSecondaryFacing(tta.getFacing());
            }

            // send an outgoing packet to everybody
            send(createAttackPacket(ea));
        }
        send(createEntityPacket(entity.getId()));
    }

    /**
     * Resolve all fire for the round
     */
    private void resolveWeaponAttacks() {
        roundReport.append("\nWeapon Attack Phase\n-------------------\n");

        int cen = Entity.NONE;

        // loop thru received attack actions
        for (Enumeration i = attacks.elements(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                resolveWeaponAttack(waa, cen);
                cen = waa.getEntityId();
            } else if (o instanceof TorsoTwistAction) {
                TorsoTwistAction tta = (TorsoTwistAction)o;
                game.getEntity(tta.getEntityId()).setSecondaryFacing(tta.getFacing());

                System.out.println("server.resolveFire: torso twisting "
                                   + game.getEntity(tta.getEntityId()).getDisplayName()
                                   + " in direction " + tta.getFacing());
            } else {
                // hmm, error
            }
        }

    }

    /**
     * Resolve a single Weapon Attack object
     */
    private void resolveWeaponAttack(WeaponAttackAction waa, int lastEntityId) {
        final Entity ae = game.getEntity(waa.getEntityId());
        final Entity te = game.getEntity(waa.getTargetId());
        final MountedWeapon w = ae.getWeapon(waa.getWeaponId());

        if (lastEntityId != waa.getEntityId()) {
            phaseReport.append("\nWeapons fire for " + ae.getDisplayName() + "\n");
        }

        phaseReport.append("    " + w.getType().getName() + " at "
                   + te.getDisplayName());

        // check ammo
        if (w.getAmmoFeed() != null) {
            if (w.getAmmoFeed().shots == 0) {
                // try reloading?
                ae.loadWeapon(w);
            }
            if (w.getAmmoFeed().shots == 0) {
                phaseReport.append(" but the weapon is out of ammo");
                return;
            }
            w.getAmmoFeed().shots--;
        }

        // build up some heat
        ae.heatBuildup += w.getType().getHeat();

        // set the weapon as having fired
        w.setFiredThisRound(true);

        // should we even bother?
        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
            phaseReport.append(" but the target is already destroyed!\n");
            return;
        }
        // compute to-hit
        ToHitData toHit = Compute.toHitWeapon(game, waa);
        phaseReport.append("; needs " + toHit.getValue() + ", ");

        // roll
        int roll = Compute.d6(2);
        phaseReport.append("rolls " + roll + " : ");

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            phaseReport.append("misses.\n");
            return;
        }
        // are we attacks normal weapons or missiles?
        if (w.getType().getDamage() != Weapon.DAMAGE_MISSILE) {
            // normal weapon; deal damage
            HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
            phaseReport.append("hits" + toHit.getTableDesc() + " " + te.getLocationAbbr(hit.loc));
            phaseReport.append(damageEntity(te, hit, w.getType().getDamage()));
        } else {
            // missiles; determine number of missiles hitting
            int hits = Compute.missilesHit(w.getType().getRackSize());
            phaseReport.append(hits + " missiles hit" + toHit.getTableDesc() + ".");
            // for SRMs, do each missile seperately
            if (w.getType().getAmmoType() == Ammo.TYPE_SRM) {
                for (int j = 0; j < hits; j++) {
                    HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                    phaseReport.append(damageEntity(te, hit, 2));
                }
            }
            // LRMs, do salvos of 5
                if (w.getType().getAmmoType() == Ammo.TYPE_LRM) {
                    while (hits > 0) {
                        int salvo = Math.min(5, hits);
                        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                        phaseReport.append(damageEntity(te, hit, salvo));
                        hits -= salvo;
                    }
                }
            }

        phaseReport.append("\n");
    }

    /**
     * Handle all physical attacks for the round
     */
    private void resolvePhysicalAttacks() {
        roundReport.append("\nPhysical Attack Phase\n-------------------\n");

        int cen = Entity.NONE;
        
        // add any pending charges
        for (Enumeration i = pendingCharges.elements(); i.hasMoreElements();) {
            attacks.addElement(i.nextElement());
        }
        pendingCharges.removeAllElements();

        // loop thru received attack actions
        for (Enumeration i = attacks.elements(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (o instanceof PunchAttackAction) {
                PunchAttackAction paa = (PunchAttackAction)o;
                if (paa.getArm() == PunchAttackAction.BOTH) {
                    paa.setArm(PunchAttackAction.LEFT);
                    resolvePunchAttack(paa, cen);
                    cen = paa.getEntityId();
                    paa.setArm(PunchAttackAction.RIGHT);
                    resolvePunchAttack(paa, cen);
                } else {
                    resolvePunchAttack(paa, cen);
                    cen = paa.getEntityId();
                }
            } else if (o instanceof KickAttackAction) {
                KickAttackAction kaa = (KickAttackAction)o;
                resolveKickAttack(kaa, cen);
                cen = kaa.getEntityId();
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
     * Handle a punch attack
     */
    private void resolvePunchAttack(PunchAttackAction paa, int lastEntityId) {
        final Entity ae = game.getEntity(paa.getEntityId());
        final Entity te = game.getEntity(paa.getTargetId());
        final String armName = paa.getArm() == PunchAttackAction.LEFT
                               ? "Left Arm" : "Right Arm";

        if (lastEntityId != paa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " + ae.getDisplayName() + "\n");
        }

        phaseReport.append("    Punch (" +armName + ") at " + te.getDisplayName());

        // should we even bother?
        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
            phaseReport.append(" but the target is already destroyed!\n");
            return;
        }
        // compute to-hit
        ToHitData toHit = Compute.toHitPunch(game, paa);
        phaseReport.append("; needs " + toHit.getValue() + ", ");

        // roll
        int roll = Compute.d6(2);
        phaseReport.append("rolls " + roll + " : ");

        // do we hit?
        if (roll < toHit.getValue()) {
            phaseReport.append("misses.\n");
            return;
        }
        int damage = Compute.getPunchDamageFor(ae, paa.getArm());

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        phaseReport.append("hits" + toHit.getTableDesc() + " " + te.getLocationAbbr(hit.loc));
        phaseReport.append(damageEntity(te, hit, damage));

        phaseReport.append("\n");
    }

    /**
     * Handle a kick attack
     */
    private void resolveKickAttack(KickAttackAction kaa, int lastEntityId) {
        final Entity ae = game.getEntity(kaa.getEntityId());
        final Entity te = game.getEntity(kaa.getTargetId());
        final String legName = kaa.getLeg() == KickAttackAction.LEFT 
                                ? "Left Leg" 
                                : "Right Leg";

        if (lastEntityId != ae.getId()) {
            phaseReport.append("\nPhysical attacks for " + ae.getDisplayName() + "\n");
        }

        phaseReport.append("    Kick (" + legName + ") at " + te.getDisplayName());

        // should we even bother?
        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
            phaseReport.append(" but the target is already destroyed!\n");
            return;
        }
        // compute to-hit
        ToHitData toHit = Compute.toHitKick(game, kaa);
        phaseReport.append("; needs " + toHit.getValue() + ", ");

        // roll
        int roll = Compute.d6(2);
        phaseReport.append("rolls " + roll + " : ");

        // do we hit?
        if (roll < toHit.getValue()) {
            // miss
            phaseReport.append("misses.\n");
            pilotRolls.addElement(new PilotingRollData(ae.getId(), 0, "missed a kick"));
            return;
        }
        
        int damage = Compute.getKickDamageFor(ae, kaa.getLeg());

        HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
        phaseReport.append("hits" + toHit.getTableDesc() + " " + te.getLocationAbbr(hit.loc));
        phaseReport.append(damageEntity(te, hit, damage));

        pilotRolls.addElement(new PilotingRollData(te.getId(), 0, "was kicked"));

        phaseReport.append("\n");
    }

    /**
     * Handle a push attack
     */
    private void resolvePushAttack(PushAttackAction paa, int lastEntityId) {
        final Entity ae = game.getEntity(paa.getEntityId());
        final Entity te = game.getEntity(paa.getTargetId());

        if (lastEntityId != paa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " + ae.getDisplayName() + "\n");
        }

        phaseReport.append("    Pushing " + te.getDisplayName());

        // should we even bother?
        if (te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
            phaseReport.append(" but the target is already destroyed!\n");
            return;
        }

        // compute to-hit
        ToHitData toHit = Compute.toHitPush(game, paa);
        phaseReport.append("; needs " + toHit.getValue() + ", ");

        // roll
        int roll = Compute.d6(2);
        phaseReport.append("rolls " + roll + " : ");

        // do we hit?
        if (roll < toHit.getValue()) {
            phaseReport.append("misses.\n");
            return;
        }

        // we hit...
        int direction = ae.getFacing();

        if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
            Coords src = te.getPosition();
            Coords dest = src.translated(direction);
            phaseReport.append("succeeds: target is pushed into hex "
                               + dest.getBoardNum()
                               + "\n");
            doEntityDisplacement(te, src, dest, new PilotingRollData(te.getId(), 0, "was pushed"));

            // if push actually moved the target, attacker follows thru
            if (game.getEntity(src) == null) {
                ae.setPosition(src);
            }
        } else {
            phaseReport.append("succeeds, but target can't be moved.\n");
            pilotRolls.addElement(new PilotingRollData(te.getId(), 0, "was pushed"));
        }


        phaseReport.append("\n");
    }

    /**
     * Handle a charge attack
     */
    private void resolveChargeAttack(ChargeAttackAction caa, int lastEntityId) {
        final Entity ae = game.getEntity(caa.getEntityId());
        final Entity te = game.getEntity(caa.getTargetId());
        
        // is the attacker dead?  because that sure messes up the calculations
        if (ae == null) {
            return;
        }
        
        final int direction = ae.getFacing();

        if (lastEntityId != caa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " + ae.getDisplayName() + "\n");
        }

        phaseReport.append("    Charging " + te.getDisplayName());
        
        // entity isn't charging any more
        ae.setCharging(false);

        // should we even bother?
        if (te == null || te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
            phaseReport.append(" but the target is already destroyed!\n");
            return;
        }

        // target still in the same position?
        if (!te.getPosition().equals(caa.getTargetPos())) {
            phaseReport.append(" but the target has moved.\n");
            return;
        }

        // compute to-hit
        ToHitData toHit = Compute.toHitCharge(game, caa);

        // hack: if the attacker's prone, fudge the roll
        int roll;
        if (ae.isProne()) {
            roll = -12;
            phaseReport.append("; but the attaker is prone : ");
        } else {
            // roll
            roll = Compute.d6(2);
            phaseReport.append("; needs " + toHit.getValue() + ", ");
            phaseReport.append("rolls " + roll + " : ");
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            Coords src = ae.getPosition();
            Coords dest = Compute.getMissedChargeDisplacement(game, ae.getId(), src, direction);
            phaseReport.append("misses.\n");
            // move attacker to side hex
            doEntityDisplacement(ae, src, dest, null);
            return;
        }

        // we hit...
        int damage = Compute.getChargeDamageFor(ae);
        int damageTaken = Compute.getChargeDamageTakenBy(ae, te);

        phaseReport.append("hits.");
        phaseReport.append("\n  Defender takes " + damage + " damage" + toHit.getTableDesc() + ".");
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
            phaseReport.append(damageEntity(te, hit, cluster));
            damage -= cluster;
        }
        phaseReport.append("\n  Attacker takes " + damageTaken + " damage.");
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = te.rollHitLocation(ToHitData.HIT_NORMAL, toHit.SIDE_FRONT);
            phaseReport.append(damageEntity(ae, hit, cluster));
            damageTaken -= cluster;
        }
        // move attacker and target, if possible
        if (Compute.isValidDisplacement(game, te.getId(), te.getPosition(), direction)) {
            Coords src = te.getPosition();
            Coords dest = src.translated(direction);
            
            phaseReport.append("\n");
            doEntityDisplacement(te, src, dest, new PilotingRollData(te.getId(), 2, "was charged"));
            doEntityDisplacement(ae, ae.getPosition(), src, new PilotingRollData(ae.getId(), 2, "charging"));
        } else {
            // they stil have to roll
            pilotRolls.addElement(new PilotingRollData(te.getId(), 2, "was charged"));
            pilotRolls.addElement(new PilotingRollData(ae.getId(), 2, "charging"));
        }

        phaseReport.append("\n");
    }

    /**
     * Handle a charge attack
     */
    private void resolveDfaAttack(DfaAttackAction daa, int lastEntityId) {
        final Entity ae = game.getEntity(daa.getEntityId());
        final Entity te = game.getEntity(daa.getTargetId());
        
        // is the attacker dead?  because that sure messes up the calculations
        if (ae == null) {
            return;
        }
        
        final int direction = ae.getFacing();

        if (lastEntityId != daa.getEntityId()) {
            phaseReport.append("\nPhysical attacks for " + ae.getDisplayName() + "\n");
        }

        phaseReport.append("    Attempting death from above on " + te.getDisplayName());

        // entity isn't charging any more
        ae.setMakingDfa(false);

        // should we even bother?
        if (te == null || te.isDestroyed() || te.isDoomed() || te.crew.isDead()) {
            phaseReport.append(" but the target is already destroyed!\n");
            return;
        }

        // target still in the same position?
        if (!te.getPosition().equals(daa.getTargetPos())) {
            phaseReport.append(" but the target has moved.\n");
            return;
        }
        
        // compute to-hit
        ToHitData toHit = Compute.toHitDfa(game, daa);

        // hack: if the attacker's prone, fudge the roll
        int roll;
        if (ae.isProne()) {
            roll = -12;
            phaseReport.append("; but the attacker is prone : ");
        } else {
            // roll
            roll = Compute.d6(2);
            phaseReport.append("; needs " + toHit.getValue() + ", ");
            phaseReport.append("rolls " + roll + " : ");
        }

        // do we hit?
        if (roll < toHit.getValue()) {
            Coords src = ae.getPosition();
            Coords dest = te.getPosition();
            Coords targetDest = Compute.getPreferredDisplacement(game, te.getId(), dest, direction);
            phaseReport.append("misses.\n");
            if (targetDest != null) {
                // move target to preferred hex
                doEntityDisplacement(te, src, targetDest, null);
                // attacker falls into destination hex
                phaseReport.append(ae.getDisplayName() + " falls into hex " + dest.getBoardNum() + ".\n");
                doEntityFall(ae, dest, 2, 3, PilotingRollData.AUTOMATIC_FALL);
            } else {
                // attacker destroyed
                phaseReport.append("*** " + ae.getDisplayName()
                                   + " DESTROYED due to impossible displacement! ***");
                ae.setDoomed(true);
            }
            return;
        }

        // we hit...
        int damage = Compute.getDfaDamageFor(ae);
        int damageTaken = Compute.getDfaDamageTakenBy(ae);

        phaseReport.append("hits.");

        phaseReport.append("\n  Defender takes " + damage + " damage" + toHit.getTableDesc() + ".");
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            HitData hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
            phaseReport.append(damageEntity(te, hit, cluster));
            damage -= cluster;
        }
        phaseReport.append("\n  Attacker takes " + damageTaken + " damage.");
        while (damageTaken > 0) {
            int cluster = Math.min(5, damageTaken);
            HitData hit = te.rollHitLocation(ToHitData.HIT_KICK, toHit.SIDE_FRONT);
            phaseReport.append(damageEntity(ae, hit, cluster));
            damageTaken -= cluster;
        }
        phaseReport.append("\n");
        
        // defender pushed away or destroyed
        Coords src = ae.getPosition();
        Coords dest = te.getPosition();
        Coords targetDest = Compute.getValidDisplacement(game, te.getId(), dest, direction);
        if (targetDest != null) {
            doEntityDisplacement(te, dest, targetDest, new PilotingRollData(te.getId(), 2, "hit by death from above"));
        } else {
            // ack!  automatic death!
            phaseReport.append("*** " + te.getDisplayName()
                               + " DESTROYED due to impossible displacement! ***");
            te.setDoomed(true);
        }
        doEntityDisplacement(ae, src, dest, new PilotingRollData(ae.getId(), 4, "executed death from above"));
    }

    /**
     * Each mech sinks the amount of heat appropriate to its current heat
     * capacity.
     */
    private void resolveHeat() {
        roundReport.append("\nHeat Phase\n----------\n");
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();

            // should we even bother?
            if (entity.isDestroyed() || entity.isDoomed() || entity.crew.isDead()) {
                continue;
            }
            // engine hits add a lot of heat, provided the engine is on
            if (!entity.isShutDown()) {
                entity.heatBuildup += 5 * entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_CT);
            }

            // add the heat we've built up so far.
            roundReport.append(entity.getDisplayName() + " gains " + entity.heatBuildup + " heat,");
            entity.heat += entity.heatBuildup;
            entity.heatBuildup = 0;

            // how much heat can we sink?
            int tosink = Math.min(entity.getHeatCapacityWithWater(game), entity.heat);

            entity.heat -= tosink;
            roundReport.append(" sinks " + tosink + " heat and is now at " + entity.heat + " heat.\n");

            // heat effects: start up
            if (entity.heat < 30 && entity.isShutDown()) {
                if (entity.heat < 14) {
                    entity.setShutDown(false);
                    roundReport.append(entity.getDisplayName() + " automatically starts up.\n");
                } else {
                    int startup = 4 + (((entity.heat - 14) / 4) * 2);
                    int suroll = Compute.d6(2);
                    roundReport.append(entity.getDisplayName() + " needs a " + startup + "+ to start up, rolls " + suroll + " : ");
                    if (suroll >= startup) {
                        entity.setShutDown(false);
                        roundReport.append("successful!\n");
                    } else {
                        roundReport.append("fails.\n");
                    }
                }
            }

            // heat effects: shutdown!
            if (entity.heat >= 14 && entity.isActive()) {
                if (entity.heat >= 30) {
                    roundReport.append(entity.getDisplayName() + " automatically shuts down.\n");
                    // add a piloting roll and resolve immediately
                    pilotRolls.addElement(new PilotingRollData(entity.getId(), 3, "reactor shutdown"));
                    resolvePilotingRolls();
                    // okay, now mark shut down
                    entity.setShutDown(true);
                } else if (entity.heat >= 14) {
                    int shutdown = 4 + (((entity.heat - 14) / 4) * 2);
                    int sdroll = Compute.d6(2);
                    roundReport.append(entity.getDisplayName() + " needs a " + shutdown + "+ to avoid shutdown, rolls " + sdroll + " : ");
                    if (sdroll >= shutdown) {
                        roundReport.append("avoids successfully!\n");
                    } else {
                        roundReport.append("shuts down.\n");
                        // add a piloting roll and resolve immediately
                        pilotRolls.addElement(new PilotingRollData(entity.getId(), 3, "reactor shutdown"));
                        resolvePilotingRolls();
                        // okay, now mark shut down
                        entity.setShutDown(true);
                    }
                }
            }

            // heat effects: ammo explosion!
            if (entity.heat >= 19) {
                int boom = 4 + (entity.heat >= 23 ? 2 : 0) + (entity.heat >= 28 ? 2 : 0);
                int boomroll = Compute.d6(2);
                roundReport.append(entity.getDisplayName() + " needs a " + boom + "+ to avoid ammo explosion, rolls " + boomroll + " : ");
                if (boomroll >= boom) {
                    roundReport.append("avoids successfully!\n");
                } else {
                    roundReport.append("fails to avoid explosion.\n");
                    roundReport.append(explodeAmmo(entity));
                }
            }

            // heat effects: mechwarrior damage
            if (entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, Mech.LOC_HEAD) > 0
                && entity.heat >= 15) {
                if (entity.heat >= 25) {
                    // mechwarrior takes 2 damage
                    roundReport.append(entity.getDisplayName() + " has 25 or higher heat and damaged life support.  Mechwarrior takes 2 damage.\n");
                    damageCrew(entity, 2);
                } else {
                    // mechwarrior takes 1 damage
                    roundReport.append(entity.getDisplayName() + " has 15 or higher heat and damaged life support.  Mechwarrior takes 1 damage.\n");
                    damageCrew(entity, 1);
                }
            }
        }
    }

    /**
     * Checks to see if any entity has takes 20 damage.  If so, they need a piloting
     * skill roll.
     */
    private void checkFor20Damage() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            // if this mech has 20+ damage, add another roll to the list.
            if (entity.damageThisPhase >= 20) {
                pilotRolls.addElement(new PilotingRollData(entity.getId(), 1, "20+ damage"));
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
            final Hex curHex = game.board.getHex(entity.getPosition());
            if (curHex.getTerrainType() == Terrain.WATER
                    && (curHex.getElevation() <= -2
                        || (curHex.getElevation() == -1 && entity.isProne()))
                    && entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, Mech.LOC_HEAD) > 0) {
                roundReport.append("\n" + entity.getDisplayName() + " is underwater with damaged life support.  Mechwarrior takes 1 damage.\n");
                damageCrew(entity, 1);
            }
        }
    }

    /**
     * Resolves all built up piloting skill rolls.
     * (used at end of weapons, physical phases)
     */
    private void resolvePilotingRolls() {
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (entity.isProne() || entity.isDoomed() || entity.isDestroyed()) {
                continue;
            }
            int rolls = 0;
            StringBuffer reasons = new StringBuffer();
            PilotingRollData roll = Compute.getBasePilotingRoll(game, entity.getId());
            for (Enumeration j = pilotRolls.elements(); j.hasMoreElements();) {
                final PilotingRollData modifier = (PilotingRollData)j.nextElement();
                if (modifier.getEntityId() == entity.getId()) {
                    rolls++;
                    if (reasons.length() > 0) {
                        reasons.append(", ");
                    }
                    reasons.append(modifier.getDesc());
                    roll.addModifier(modifier.getValue(), modifier.getDesc());
                }
            }
            // any rolls needed?
            if (rolls == 0) {
                continue;
            }
            if (roll.getValue() == PilotingRollData.AUTOMATIC_FALL) {
                phaseReport.append("\n" + entity.getDisplayName() + " must make " + rolls + " piloting skill roll(s) and automatically fails (" + roll.getDesc() + ").\n");
                doEntityFall(entity, roll.getValue());
            } else {
                phaseReport.append("\n" + entity.getDisplayName() + " must make " + rolls + " piloting skill roll(s) (" + reasons + ").\n");
                phaseReport.append("The target is " + roll.getValue() + " [" + roll.getDesc() + "].\n");
                for (int j = 0; j < rolls; j++) {
                    final int diceRoll = Compute.d6(2);
                    phaseReport.append("    " + entity.getDisplayName() + " needs " + roll.getValue() + ", rolls " + diceRoll + " : ");
                    phaseReport.append((diceRoll >= roll.getValue() ? "remains standing" : "falls") + ".\n");
                    if (diceRoll < roll.getValue()) {
                        doEntityFall(entity, roll.getValue());
                        // break rolling loop
                        break;
                    }
                }
            }
        }
        pilotRolls.removeAllElements();
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
                en.crew.setDead(true);
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
            final int rollsNeeded = e.getCrew().getRollsNeeded();
            e.crew.setRollsNeeded(0);

            if (!e.isTargetable() || !e.getCrew().isActive() || rollsNeeded == 0) {
                continue;
            }
            anyRolls = true;
            for (int j = 0; j < e.crew.getRollsNeeded(); j++) {
                int roll = Compute.d6(2);
                phaseReport.append("\nPilot of " + e.getDisplayName()
                   + " \"" + e.getCrew().getName() + "\""
                   + " needs a " + e.getCrew().getConciousnessNumber()
                   + " to stay concious.  Rolls " + roll + " : ");
                if (roll >= e.crew.getConciousnessNumber()) {
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

            if (!e.isTargetable() || !e.crew.isUnconcious() || e.crew.isKoThisRound()) {
                continue;
            }
            anyRolls = true;
            int roll = Compute.d6(2);
            roundReport.append("\nPilot of " + e.getDisplayName() + " \"" + e.crew.getName() + "\" needs a " + e.crew.getConciousnessNumber() + " to regain conciousness.  Rolls " + roll + " : ");
            if (roll >= e.crew.getConciousnessNumber()) {
                roundReport.append("successful!");
                e.crew.setUnconcious(false);
            } else {
                roundReport.append("fails.");
                break;
            }
        }
        if (anyRolls) {
            roundReport.append("\n");
        }
    }

    /**
     * Deals the listed damage to a mech.  Returns a description
     * string for the log.
     *
     * Currently mech only.
     */
    private String damageEntity(Entity te, HitData hit, int damage) {
        String desc = new String();

        int crits = hit.effect == HitData.EFFECT_CRITICAL ? 1 : 0;

        int loc = hit.loc, nextLoc = Entity.LOC_NONE;
        while (damage > 0 && !te.isDestroyed() && !te.isDoomed()) {
            // let's resolve some damage!
            desc += "\n        " + te.getDisplayName() + " takes " + damage + " damage to " + te.getLocationAbbr(loc) + (hit.effect == HitData.EFFECT_CRITICAL ? " (critical.)" : ".");
            te.damageThisPhase += damage;

            // is there armor in the location hit?
            if (te.getArmor(loc) > 0) {
                if (te.getArmor(loc) > damage) {
                    // armor absorbs all damage
                    te.setArmor(te.getArmor(loc) - damage, loc);
                    damage = 0;
                    desc += " " + te.getArmor(loc) + " Armor remaining";
                } else {
                    // damage goes on to internal
                    damage -= te.getArmor(loc);
                    te.setArmor(Entity.ARMOR_DESTROYED, loc);
                    desc += " Armor destroyed,";
                }
            }

            // is there damage remaining?
            if (damage > 0) {
                // is there internal structure in the location hit?
                if (te.getInternal(loc) > 0) {
                    // triggers a critical hit
                    crits++;
                    if (te.getInternal(loc) > damage) {
                        // internal structure absorbs all damage
                        te.setInternal(te.getInternal(loc) - damage, loc);
                        damage = 0;
                        desc += " " + te.getInternal(loc) + " Internal Structure remaining";
                    } else {
                        // damage transfers, maybe
                        damage -= te.getInternal(loc);
                        te.setInternal(Entity.ARMOR_DESTROYED, loc);
                        desc += " <<<SECTION DESTROYED>>>,";
                    }
                }

                // is the internal structure gone?
                if (te.isLocationDestroyed(loc)) {
                    destroyLocation(te, loc);
                    if (loc == Mech.LOC_RLEG || loc == Mech.LOC_LLEG) {
                        pilotRolls.addElement(new PilotingRollData(te.getId(),
                        PilotingRollData.AUTOMATIC_FALL, "leg destroyed"));
                    }
                    if (te.getTransferLocation(loc) == Entity.LOC_DESTROYED) {
                        // entity destroyed.
                        desc += " Entity destroyed!\n";
                        desc += "*** " + te.getDisplayName() + " DESTROYED! ***";
                        te.setDoomed(true);
                        // no need for further damage
                        damage = 0;
                        crits = 0;
                    } else {
                        // remaining damage transfers
                        nextLoc = te.getTransferLocation(loc);
                        desc += " " + damage + " damage transfers to "
                            + te.getLocationAbbr(nextLoc) + ";";
                    }
                }
            }
            // roll all critical hits against this location
            for (int i = 0; i < crits; i++) {
                desc += "\n" + criticalEntity(te, loc);
            }
            crits = 0;

            if (loc == Mech.LOC_HEAD) {
                desc += "\n" + damageCrew(te, 1);
            }

            // loop to next location
            loc = nextLoc;
        }


        return desc;
    }

    /**
     * Rolls and resolves critical hits
     *
     * Currently mech only
     */
    private String criticalEntity(Entity en, int loc) {
        if (en.isRearLocation(loc)) {
            loc = en.getFrontLocation(loc);
        }
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
            if (loc == Mech.LOC_HEAD ||
                    loc == Mech.LOC_RARM || loc == Mech.LOC_LARM ||
                    loc == Mech.LOC_RLEG || loc == Mech.LOC_LLEG) {
                desc += "<<<LIMB BLOWN OFF>>> " + en.getLocationName(loc) + " blown off.";
                destroyLocation(en, loc);
                if (loc == Mech.LOC_HEAD) {
                    en.crew.setDead(true);
                    desc += "\n*** " + en.getDisplayName() + " PILOT KILLED! ***";
                }
                return desc;
            }
            hits = 3;
            desc += " 3 locations.";
        }
        // transfer criticals, if needed
        if (hits > 0 && en.getHitableCriticals(loc) <= 0
                && en.getTransferLocation(loc) != Entity.LOC_DESTROYED) {
            loc = en.getTransferLocation(loc);
            desc += "\n            Location is empty, so criticals transfer to " + en.getLocationAbbr(loc) +".";
        }
        // roll criticals
        while (hits > 0) {
            if (en.getHitableCriticals(loc) <= 0) {
                desc += "\n            Location empty.";
                break;
            }
            int slot = (int)(Compute.random.nextDouble() * en.getNumberOfCriticals(loc));
            CriticalSlot cs = en.getCritical(loc, slot);
            if (cs != null && !cs.isHit()) {
                cs.setDoomed(true);
                en.setCritical(loc, slot, cs);
                switch(cs.getType()) {
                case CriticalSlot.TYPE_SYSTEM :
                    desc += "\n            <<<CRITICAL HIT>>> on " + Mech.systemNames[cs.getIndex()] + ".";
                    switch(cs.getIndex()) {
                    case Mech.SYSTEM_COCKPIT :
                        // boink!
                        en.crew.setDead(true);
                        desc += "\n*** " + en.getDisplayName() + " PILOT KILLED! ***";
                        break;
                    case Mech.SYSTEM_ENGINE :
                        if (en.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, loc) > 2) {
                            // third engine hit
                            en.setDoomed(true);
                            desc += "\n*** " + en.getDisplayName() + "ENGINE DESTROYED! ***";
                        }
                        break;
                    case Mech.SYSTEM_GYRO :
                        if (en.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, loc) > 1) {
                            // gyro destroyed
                            pilotRolls.addElement(new PilotingRollData(en.getId(), PilotingRollData.AUTOMATIC_FALL, "gyro destroyed"));
                        } else {
                            // first gyro hit
                            pilotRolls.addElement(new PilotingRollData(en.getId(), 3, "gyro hit"));
                        }
                        break;
                    case Mech.ACTUATOR_UPPER_LEG :
                    case Mech.ACTUATOR_LOWER_LEG :
                    case Mech.ACTUATOR_FOOT :
                        // leg/foot actuator piloting roll
                        pilotRolls.addElement(new PilotingRollData(en.getId(), 1, "leg/foot actuator hit"));
                        break;
                    case Mech.ACTUATOR_HIP :
                        // hip piloting roll
                        pilotRolls.addElement(new PilotingRollData(en.getId(), 2, "hip actuator hit"));
                        break;
                    }
                    break;
                case CriticalSlot.TYPE_WEAPON :
                    desc += "\n            <<<CRITICAL HIT>>> on " + en.getWeapon(cs.getIndex()).getType().getName() + ".";
                    en.getWeapon(cs.getIndex()).setDestroyed(true);
                    break;
                case CriticalSlot.TYPE_AMMO :
                    desc += "\n            <<<CRITICAL HIT>>> on " + en.getAmmo(cs.getIndex()).getName() + ".";
                    en.getWeapon(cs.getIndex()).setDestroyed(true);
                    desc += explodeAmmo(en, loc, slot);
                    break;
                }
                hits--;
                //System.err.println("s: critical loop, " + hits + " remaining");
            }
        }

        return desc;
    }

    /**
     * Marks all equipment in a location on an entity as destroyed.
     */
    private void destroyLocation(Entity en, int loc) {
        // mark armor, internal as destroyed
        en.setArmor(Entity.ARMOR_DESTROYED, loc);
        en.setInternal(Entity.ARMOR_DESTROYED, loc);
        if (en.getRearLocation(loc) != loc) {
            en.setArmor(Entity.ARMOR_DESTROYED, en.getRearLocation(loc));
        }
        // weapons destroyed
        for (int i = 0; i < en.weapons.size(); i++) {
            if (en.getWeapon(i).getLocation() == loc) {
                en.getWeapon(i).setDestroyed(true);
            }
        }
        // ammo destroyed
        for (int i = 0; i < en.ammo.size(); i++) {
            if (en.getAmmo(i).location == loc) {
                en.getAmmo(i).exploded = true;
            }
        }
        // all critical slots destroyed
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            if (cs != null) {
                cs.setDoomed(true);
            }
        }
        // dependent locations destroyed
        if (en.getDependentLocation(loc) != Entity.LOC_NONE) {
            destroyLocation(en, en.getDependentLocation(loc));
        }
    }

    /**
     * Explodes the ammo in the specified location and slot.
     */
    private String explodeAmmo(Entity en, int loc, int slot) {
        String desc = "";
        if (en.getCritical(loc, slot).getType() != CriticalSlot.TYPE_AMMO) {
            System.err.println("server: explodeAmmo called on non-ammo"
                               + " crititical slot (" + loc + " , " + slot + ")");
            return "";
        }
        // check amount of damage
        Ammo ammo = en.getAmmo(en.getCritical(loc, slot).getIndex());
        if (ammo.exploded) {
            System.err.println("server: explodeAmmo called already exploded ammo"
                               + " crititical slot (" + loc + " , " + slot + ")");
            return "";
        }
        int damage = ammo.damagePerShot * ammo.rackSize * ammo.shots;
        if (damage <= 0) {
            return "";
        }
        // if there is damage, it's probably a lot
        desc += "\n*** AMMO EXPLOSION!  " + damage + " DAMAGE! ***";
        ammo.exploded = true;
        desc += damageEntity(en, new HitData(loc), damage) + "\n\n";
        // if the mech survives, the pilot takes damage
        if (!en.isDoomed() && !en.isDestroyed()) {
            desc += damageCrew(en, 2) + "\n";
        }

        return desc;
    }


    /**
     * Makes one slot of ammo, determined by certain rules, explode on a mech.
     */
    private String explodeAmmo(Entity entity) {
        int damage = 0;
        int rack = 0;
        int boomloc = -1;
        int boomslot = -1;
        for (int j = 0; j < entity.locations(); j++) {
            for (int k = 0; k < entity.getNumberOfCriticals(j); k++) {
                CriticalSlot cs = entity.getCritical(j, k);
                    if (cs != null && !cs.isDestroyed()
                        && cs.getType() == CriticalSlot.TYPE_AMMO) {
                      Ammo a = entity.getAmmo(cs.getIndex());
                      if (!a.exploded && (rack < a.damagePerShot * a.rackSize
                            || damage < a.damagePerShot * a.rackSize * a.shots)) {
                            rack = a.damagePerShot * a.rackSize;
                            damage = a.damagePerShot * a.rackSize * a.shots;
                            boomloc = j;
                            boomslot = k;
                      }
                }
            }
        }
        if (boomloc != -1 && boomslot != -1) {
            return explodeAmmo(entity, boomloc, boomslot);
        } else {
            return "  Luckily, there is no ammo to explode.\n";
        }
    }

    /**
     * Makes a mech fall.
     */
    private void doEntityFall(Entity entity, Coords fallPos, int height,
        int facing, int roll) {
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

        // calculate damage
        int damage = (int)Math.round(entity.getWeight() / 10) * (height + 1);
        
        // TODO: only fall to surface of water
        if (game.board.getHex(fallPos).getTerrainType() == Terrain.WATER) {
            damage = (int)Math.ceil(damage / 2.0);
        }

        // report falling
        phaseReport.append("    " + entity.getDisplayName() + " falls on its " + side + ", suffering " + damage + " damage.");

        // standard damage loop
        while (damage > 0) {
            int cluster = Math.min(5, damage);
            HitData hit = entity.rollHitLocation(ToHitData.HIT_NORMAL, table);
            phaseReport.append(damageEntity(entity, hit, cluster));
            damage -= cluster;
        }

        // pilot damage?
        if (roll != PilotingRollData.AUTOMATIC_FALL) {
            roll += height;
        }
        if (roll > 12) {
            phaseReport.append("\nPilot of " + entity.getDisplayName() + " \"" + entity.crew.getName() + "\" cannot avoid damage.\n");
            damageCrew(entity, 1);
        } else {
            int diceRoll = Compute.d6(2);
            phaseReport.append("\nPilot of " + entity.getDisplayName() + " \"" + entity.crew.getName() + "\" must roll " + roll + " to avoid damage; rolls " + diceRoll + " : " + (diceRoll >= roll ? "succeeds" : "fails") + ".\n");
            if (diceRoll < roll) {
                damageCrew(entity, 1);
            }
        }

        entity.setProne(true);
        entity.setPosition(fallPos);
        entity.setFacing((entity.getFacing() + (facing - 1)) % 6);
        entity.setSecondaryFacing(entity.getFacing());
    }

    /**
     * The mech falls into an unoccupied hex from the given height above
     */
    private void doEntityFall(Entity entity, Coords fallPos, int height,
                          int roll) {
        doEntityFall(entity, fallPos, height, Compute.d6(1), roll);
    }

    /**
     * The mech falls down in place
     */
    private void doEntityFall(Entity entity, int roll) {
        doEntityFall(entity, entity.getPosition(), 0, roll);
    }

    /**
     * Returns a random board filename
     *
     * TODO: make this search the boards directory for boards of the correct size
     */
    private String getRandomBoard() {
        String[] boards = {"battletech.board", "citytech.board",
                           "deepcanyon1.board", "deepcanyon2.board",
                           "deserthills.board",
                           "desertmountain1.board", "desertmountain2.board",
                           "heavyforest1.board", "heavyforest2.board",
                           "lakearea.board",
                           "largelakes1.board", "largelakes2.board",
                           "openterrain1.board", "openterrain2.board",
                           "rivervalley.board",
                           "rollinghills1.board", "rollinghills2.board",
                           "scatteredwoods.board",
                           "woodland.board"};

        return boards[(int)(Compute.random.nextDouble() * boards.length)];
    }


    /**
     * Sets an entity ready status to false
     */
    private void receiveEntityReady(Packet pkt, int connIndex) {
        Entity entity = game.getEntity(pkt.getIntValue(0));
        if (entity != null && entity.getOwner() == getPlayer(connIndex)
            && game.getTurn() == connIndex) {
            entity.ready = false;
        } else {
            System.out.println("server.receiveEntityReady: got an invalid ready message");
        }
    }

    /**
      * Checks if an entity added by the client is valid and if so, adds it to the list
      */
     private void receiveEntityAdd(Packet c, int connIndex) {
        Entity entity = (Entity)c.getObject(0);

        entity.restore();
        entity.setOwner(getPlayer(connIndex));
        entity.setId(entityCounter++);
        game.addEntity(entity.getId(), entity);

        send(createEntitiesPacket());
     }

    /**
     * Deletes an entity owned by a certain player from the list
     */
    private void receiveEntityDelete(Packet c, int connIndex) {
        int enum = c.getIntValue(0);
        Entity entity = game.getEntity(enum);
        if (entity != null && entity.getOwner() == getPlayer(connIndex)) {
            game.removeEntity(enum);
            send(createEntitiesPacket());
        } else {
            // hey! that's not your entity
        }
    }

    /**
     * Sets a player's ready status
     */
    private void receivePlayerReady(Packet pkt, int connIndex) {
        boolean ready = pkt.getBooleanValue(0);
        // don't let them enter the game with no entities
        if (ready && game.phase == Game.PHASE_LOUNGE
            && game.getEntitiesOwnedBy(getPlayer(connIndex)) < 1) {
            ready = false;
        }
        getPlayer(connIndex).setReady(ready);
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
    private void transmitAllPlayerReadys() {
        for (Enumeration i = game.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player)i.nextElement();

            send(createPlayerReadyPacket(player.getId()));
        }
    }

    /**
     * Creates a packet containing the player ready status
     */
    private Packet createPlayerReadyPacket(int playerId) {
        Object[] data = new Object[2];
        data[0] = new Integer(playerId);
        data[1] = new Boolean(getPlayer(playerId).isReady());
        return new Packet(Packet.COMMAND_PLAYER_READY, data);
    }
    /**
     * Creates a packet containing the game settingss
     */
    private Packet createSettingsPacket() {
        return new Packet(Packet.COMMAND_SENDING_GAME_SETTINGS, gameSettings);
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
     * Transmits a chat message to all players
     */
    private void sendChatToAll(String origin, String message) {
        send(new Packet(Packet.COMMAND_CHAT, origin + ": " + message));
    }

    /**
     * Creates a packet for an attack
     */
    private Packet createAttackPacket(EntityAction ea) {
        return new Packet(Packet.COMMAND_ENTITY_ATTACK, ea);
    }

    /**
     * Send a packet to all connected clients.
     */
    private void send(Packet packet) {
    for (Enumeration i = connections.elements(); i.hasMoreElements();) {
        final Connection conn = (Connection)i.nextElement();
            conn.connSend(packet);
        }
    }

    /**
     * Send a packet to a specific connection.
     */
    private void send(int connId, Packet packet) {
        getClient(connId).connSend(packet);
    }

    /**
     * Send a packet to a pending connection
     */
    private void sendToPending(int connId, Packet packet) {
        getPendingConnection(connId).connSend(packet);
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

                int id = connectionCounter++;
                System.out.println("s: accepting player connection #" + id + " ...");

                connectionsPending.addElement(new Connection(s, id));

                greeting(id);
            } catch(IOException ex) {
                ;
            }
        }
    }

    /**
    * Listens for player messages and calls the appropriate
    * server functions.
    */
    class Connection implements Runnable {
        public Socket     socket;
        //public Player     player;

        public int                id;

        public Thread            receiver;


        public Connection(Socket socket, int id) {
            this.socket = socket;
            this.id = id;

            // start pump thread
            receiver = new Thread(this);
            receiver.start();
        }

        public int getId() {
            return id;
        }

        /**
         * Kill off the thread
         */
        public void die() {
            receiver = null;
        }


        /**
         * Allow the player to set whatever parameters he is able to
         */
        private void receivePlayerInfo(Packet c) {
            Player player = (Player)c.getObject(0);
            game.getPlayer(id).setColorIndex(player.getColorIndex());
            game.getPlayer(id).setStartingPos(player.getStartingPos());
        }

        /**
         * Reads a complete net command from the given socket
         */
        private Packet readCommand() {
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Packet packet = (Packet)ois.readObject();
                return packet;
            } catch (IOException ex) {
                System.err.println("s(" + id + "): IO error reading command");
                disconnected(id);
                return null;
            } catch (ClassNotFoundException ex) {
                System.err.println("s(" + id + "): curPosass not found error reading command");
                disconnected(id);
                return null;
            }
        }

        /**
         * Sends a packet!
         *
         * synchronized seems to keep this from getting munged.
         */
        public synchronized void connSend(Packet packet) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.flush();
                oos.writeObject(packet);
                oos.flush();
                //System.out.println("s(" + cn + "): command #" + c.getCommand() + " sent");
            } catch(IOException ex) {
                System.err.println("s(" + id + "): error sending command.  dropping player");
                System.err.println(ex);
                System.err.println(ex.getMessage());
                disconnected(id);
            }
        }

        /**
         * Process a packet
         */
        private void handle(Packet c) {
            //System.out.println("s(" + cn + "): received command");
            // act on it
            switch(c.getCommand()) {
            case Packet.COMMAND_CLIENT_NAME :
                receiveClientName(c, id);
                break;
            case Packet.COMMAND_PLAYER_UPDATE :
                receivePlayerInfo(c);
                validatePlayerInfo(id);
                send(createPlayerUpdatePacket(id));
                break;
            case Packet.COMMAND_ENTITY_READY :
                receiveEntityReady(c, id);
                send(createEntityPacket(c.getIntValue(0)));
                break;
            case Packet.COMMAND_PLAYER_READY :
                receivePlayerReady(c, id);
                send(createPlayerReadyPacket(id));
                checkReady();
                break;
            case Packet.COMMAND_CHAT :
                String chat = (String)c.getObject(0);
                sendChatToAll(getPlayer(id).getName(), chat);
                break;
            case Packet.COMMAND_ENTITY_MOVE :
                doEntityMovement(c, id);
                break;
            case Packet.COMMAND_ENTITY_ATTACK :
                receiveAttack(c);
                break;
            case Packet.COMMAND_ENTITY_ADD :
                receiveEntityAdd(c, id);
                resetPlayerReady();
                transmitAllPlayerReadys();
                break;
            case Packet.COMMAND_ENTITY_REMOVE :
                receiveEntityDelete(c, id);
                resetPlayerReady();
                transmitAllPlayerReadys();
                break;
            case Packet.COMMAND_SENDING_GAME_SETTINGS :
                gameSettings = (GameSettings)c.getObject(0);
                resetPlayerReady();
                transmitAllPlayerReadys();
                send(createSettingsPacket());
                break;
            }
        }


        /**
         * listen for packets & handle them
         */
        public void run() {
            while (receiver == Thread.currentThread()) {
                handle(readCommand());
            }
        }
    }
}
