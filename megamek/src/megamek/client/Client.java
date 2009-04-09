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

package megamek.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.SwingUtilities;

import megamek.client.bot.BotClient;
import megamek.client.commands.AddBotCommand;
import megamek.client.commands.ClientCommand;
import megamek.client.commands.DeployCommand;
import megamek.client.commands.FireCommand;
import megamek.client.commands.HelpCommand;
import megamek.client.commands.MoveCommand;
import megamek.client.commands.RulerCommand;
import megamek.client.commands.ShowEntityCommand;
import megamek.client.commands.ShowTileCommand;
import megamek.client.ui.IClientCommandHandler;
import megamek.common.Board;
import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.Flare;
import megamek.common.Game;
import megamek.common.GameLog;
import megamek.common.GameTurn;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.MapSettings;
import megamek.common.MechFileParser;
import megamek.common.MechSummaryCache;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.PlanetaryConditions;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.UnitLocation;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DodgeAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.net.ConnectionFactory;
import megamek.common.net.ConnectionListenerAdapter;
import megamek.common.net.DisconnectedEvent;
import megamek.common.net.IConnection;
import megamek.common.net.Packet;
import megamek.common.net.PacketReceivedEvent;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;

/**
 * This class is instanciated for each client and for each bot running on that
 * client. non-local clients are not also instantiated on the
 * local server.
 */
public class Client implements IClientCommandHandler {
    public static final String CLIENT_COMMAND = "#";

    // we need these to communicate with the server
    private String name;

    private IConnection connection;

    // the hash table of client commands
    private Hashtable<String, ClientCommand> commandsHash = new Hashtable<String, ClientCommand>();

    // some info about us and the server
    private boolean connected = false;
    public int local_pn = -1;
    private String host;
    private int port;

    // the game state object
    public IGame game = new Game();

    // here's some game phase stuff
    private MapSettings mapSettings;
    public String phaseReport;
    public String roundReport;

    // And close client events!
    private Vector<CloseClientListener> closeClientListeners = new Vector<CloseClientListener>();

    // we might want to keep a game log...
    private GameLog log;

    private Vector<Coords> artilleryAutoHitHexes = null;

    private boolean disconnectFlag = false;

    private Hashtable<String, Integer> duplicateNameHash = new Hashtable<String, Integer>();

    private ConnectionListenerAdapter connectionListener = new ConnectionListenerAdapter() {

        /**
         * Called when it is sensed that a connection has terminated.
         */
        @Override
        public void disconnected(DisconnectedEvent e) {
            Client.this.disconnected();
        }

        @Override
        public void packetReceived(PacketReceivedEvent e) {
            handlePacket(e.getPacket());
        }

    };

    /**
     * Construct a client which will try to connect. If the connection fails, it
     * will alert the player, free resources and hide the frame.
     *
     * @param name the player name for this client
     * @param host the hostname
     * @param port the host port
     */
    public Client(String name, String host, int port) {
        // construct new client
        this.name = name;
        this.host = host;
        this.port = port;

        registerCommand(new HelpCommand(this));
        registerCommand(new MoveCommand(this));
        registerCommand(new RulerCommand(this));
        registerCommand(new ShowEntityCommand(this));
        registerCommand(new FireCommand(this));
        registerCommand(new DeployCommand(this));
        registerCommand(new ShowTileCommand(this));
        registerCommand(new AddBotCommand(this));

        TimerSingleton ts = TimerSingleton.getInstance();
        /*
         * this should be moved to UI implementations so that they are
         * responsible for figuring out who should call update for connection..
         * so if somebody does a text-only implementation which doesnt support
         * AWT event queue, we dont depend on it
         */
        final Runnable packetUpdate = new Runnable() {
            public void run() {
                updateConnection();
            }
        };
        final TimerTask packetUpdate2 = new TimerTask() {
            @Override
            public void run() {
                try {
                    SwingUtilities.invokeAndWait(packetUpdate);
                } catch (Exception ie) {
                }
            }
        };
        ts.schedule(packetUpdate2, 500, 100);
    }

    /**
     * call this once to update the connection
     */
    protected void updateConnection() {
        if (connection != null) {
            connection.update();
        }
    }

    /**
     * Attempt to connect to the specified host
     */
    public boolean connect() {
        connection = ConnectionFactory.getInstance().createClientConnection(
                host, port, 1);
        boolean result = connection.open();
        if (result) {
            connection.addConnectionListener(connectionListener);
        }
        return result;
    }

    /**
     * Shuts down threads and sockets
     */
    public void die() {
        // If we're still connected, tell the server that we're going down.
        if (connected) {
            send(new Packet(Packet.COMMAND_CLOSE_CONNECTION));
            flushConn();
        }
        connected = false;

        if (connection != null) {
            connection.close();
            connection = null;
        }

        for (int i = 0; i < closeClientListeners.size(); i++) {
            closeClientListeners.elementAt(i).clientClosed();
        }

        if (log != null) {
            try {
                log.close();
            } catch (IOException e) {
                System.err.print("Exception closing logfile: "); //$NON-NLS-1$
                e.printStackTrace();
            }
        }
        System.out.println("client: died"); //$NON-NLS-1$
        System.out.flush();
    }

    /**
     * The client has become disconnected from the server
     */
    protected void disconnected() {
        if (!disconnectFlag) {
            disconnectFlag = true;
            if (connected) {
                connected = false;
                die();
            }
            if (!host.equals("localhost")) { //$NON-NLS-1$
                game.processGameEvent(new GamePlayerDisconnectedEvent(this,
                        getLocalPlayer()));
            }
        }
    }

    /**
     * Get hexes designated for automatic artillery hits.
     */
    public Vector<Coords> getArtilleryAutoHit() {
        return artilleryAutoHitHexes;
    }

    private void initGameLog() {
        // log = new GameLog(
        // PreferenceManager.getClientPreferences().getGameLogFilename(),
        // false,
        // (new
        // Integer(PreferenceManager.getClientPreferences().getGameLogMaxSize()).longValue()
        // * 1024 * 1024) );
        log = new GameLog(PreferenceManager.getClientPreferences()
                .getGameLogFilename());
    }

    private boolean keepGameLog() {
        return PreferenceManager.getClientPreferences().keepGameLog()
                && !(this instanceof BotClient);
    }

    /**
     * Return an enumeration of the players in the game
     */
    public Enumeration<Player> getPlayers() {
        return game.getPlayers();
    }

    public Entity getEntity(int id) {
        return game.getEntity(id);
    }

    /**
     * Returns the individual player assigned the index parameter.
     */
    public Player getPlayer(int idx) {
        return game.getPlayer(idx);
    }

    /**
     * Return the local player
     */
    public Player getLocalPlayer() {
        return getPlayer(local_pn);
    }

    /**
     * Returns an <code>Enumeration</code> of the entities that match the
     * selection criteria.
     */
    public Enumeration<Entity> getSelectedEntities(EntitySelector selector) {
        return game.getSelectedEntities(selector);
    }

    /**
     * Returns the number of first selectable entity
     */
    public int getFirstEntityNum() {
        return game.getFirstEntityNum(getMyTurn());
    }

    /**
     * Returns the number of the next selectable entity after the one given
     */
    public int getNextEntityNum(int entityId) {
        return game.getNextEntityNum(getMyTurn(), entityId);
    }

    /**
     * Returns the number of the first deployable entity
     */
    public int getFirstDeployableEntityNum() {
        return game.getFirstDeployableEntityNum();
    }

    /**
     * Returns the number of the next deployable entity
     */
    public int getNextDeployableEntityNum(int entityId) {
        return game.getNextDeployableEntityNum(entityId);
    }

    /**
     * Shortcut to game.board
     */
    public IBoard getBoard() {
        return game.getBoard();
    }

    /**
     * Returns an enumeration of the entities in game.entities
     */
    public Enumeration<Entity> getEntities() {
        return game.getEntities();
    }

    public MapSettings getMapSettings() {
        return mapSettings;
    }

    /**
     * Changes the game phase, and the displays that go along with it.
     */
    public void changePhase(IGame.Phase phase) {
        game.setPhase(phase);
        // Handle phase-specific items.
        switch (phase) {
            case PHASE_STARTING_SCENARIO:
                sendDone(true);
                break;
            case PHASE_EXCHANGE:
                sendDone(true);
                break;
            case PHASE_DEPLOYMENT:
                // free some memory thats only needed in lounge
                MechSummaryCache.dispose();
                MechFileParser.dispose();
                memDump("entering deployment phase"); //$NON-NLS-1$
                break;
            case PHASE_TARGETING:
                memDump("entering targeting phase"); //$NON-NLS-1$
                break;
            case PHASE_MOVEMENT:
                memDump("entering movement phase"); //$NON-NLS-1$
                break;
            case PHASE_OFFBOARD:
                memDump("entering offboard phase"); //$NON-NLS-1$
                break;
            case PHASE_FIRING:
                memDump("entering firing phase"); //$NON-NLS-1$
                break;
            case PHASE_PHYSICAL:
                memDump("entering physical phase"); //$NON-NLS-1$
                break;
            case PHASE_LOUNGE:
                MechSummaryCache.getInstance();
                duplicateNameHash.clear(); // reset this
                break;
        }
    }

    /**
     * Adds the specified close client listener to receive close client events.
     * This is used by external programs running megamek
     *
     * @param l the game listener.
     */
    public void addCloseClientListener(CloseClientListener l) {
        closeClientListeners.addElement(l);
    }

    /**
     * wtf is this? waits for 5 seconds just for nothing?? - itmo fixed this to
     * be a bit more sensible..
     */
    public void retrieveServerInfo() {
        updateConnection();
        int retry = 50;
        while ((retry-- > 0) && !connected) {
            synchronized (this) {
                flushConn();
                updateConnection();
                try {
                    wait(100);
                } catch (InterruptedException ex) {

                }
            }
        }
    }

    /**
     * is it my turn?
     */
    public boolean isMyTurn() {
        if(game.isPhaseSimultaneous()) {
            return game.getTurnForPlayer(local_pn) != null;
        }
        return (game.getTurn() != null) && game.getTurn().isValid(local_pn, game);
    }

    public GameTurn getMyTurn() {
        if(game.isPhaseSimultaneous()) {
            return game.getTurnForPlayer(local_pn);
        }
        return game.getTurn();
    }

    /**
     * Can I unload entities stranded on immobile transports?
     */
    public boolean canUnloadStranded() {
        return (game.getTurn() instanceof GameTurn.UnloadStrandedTurn)
                && game.getTurn().isValid(local_pn, game);
    }

    /**
     * Send command to unload stranded entities to the server
     */
    public void sendUnloadStranded(int[] entityIds) {
        Object[] data = new Object[1];
        data[0] = entityIds;
        send(new Packet(Packet.COMMAND_UNLOAD_STRANDED, data));
    }

    /**
     * Change whose turn it is.
     */
    protected void changeTurnIndex(int index) {
        game.setTurnIndex(index);
    }

    /**
     * Send mode-change data to the server
     */
    public void sendModeChange(int nEntity, int nEquip, int nMode) {
        Object[] data = { new Integer(nEntity), new Integer(nEquip),
                new Integer(nMode) };
        send(new Packet(Packet.COMMAND_ENTITY_MODECHANGE, data));
    }

    /**
     * Send system mode-change data to the server
     */
    public void sendSystemModeChange(int nEntity, int nSystem, int nMode) {
        Object[] data = { new Integer(nEntity), new Integer(nSystem),
                new Integer(nMode) };
        send(new Packet(Packet.COMMAND_ENTITY_SYSTEMMODECHANGE, data));
    }

    /**
     * Send mode-change data to the server
     */
    public void sendAmmoChange(int nEntity, int nWeapon, int nAmmo) {
        Object[] data = { new Integer(nEntity), new Integer(nWeapon),
                new Integer(nAmmo) };
        send(new Packet(Packet.COMMAND_ENTITY_AMMOCHANGE, data));
    }

    /**
     * Send movement data for the given entity to the server.
     */
    public void moveEntity(int id, MovePath md) {
        Object[] data = new Object[2];

        data[0] = new Integer(id);
        data[1] = md;

        send(new Packet(Packet.COMMAND_ENTITY_MOVE, data));
    }

    /**
     * Maintain backwards compatability.
     *
     * @param id - the <code>int</code> ID of the deployed entity
     * @param c - the <code>Coords</code> where the entity should be deployed
     * @param nFacing - the <code>int</code> direction the entity should face
     */
    public void deploy(int id, Coords c, int nFacing) {
        this.deploy(id, c, nFacing, new Vector<Entity>(), false);
    }

    /**
     * Deploy an entity at the given coordinates, with the given facing, and
     * starting with the given units already loaded.
     *
     * @param id - the <code>int</code> ID of the deployed entity
     * @param c - the <code>Coords</code> where the entity should be deployed
     * @param nFacing - the <code>int</code> direction the entity should face
     * @param loadedUnits - a <code>List</code> of units that start the game
     *            being transported byt the deployed entity.
     * @param assaultDrop - true if deployment is an assault drop
     */
    public void deploy(int id, Coords c, int nFacing,
            Vector<Entity> loadedUnits, boolean assaultDrop) {
        int packetCount = 5 + loadedUnits.size();
        int index = 0;
        Object[] data = new Object[packetCount];
        data[index++] = new Integer(id);
        data[index++] = c;
        data[index++] = new Integer(nFacing);
        data[index++] = new Integer(loadedUnits.size());
        data[index++] = new Boolean(assaultDrop);

        Enumeration<Entity> iter = loadedUnits.elements();
        while (iter.hasMoreElements()) {
            data[index++] = new Integer((iter.nextElement()).getId());
        }

        send(new Packet(Packet.COMMAND_ENTITY_DEPLOY, data));
        flushConn();
    }

    /**
     * Send a weapon fire command to the server.
     */
    public void sendAttackData(int aen, Vector<EntityAction> attacks) {
        Object[] data = new Object[2];

        data[0] = new Integer(aen);
        data[1] = attacks;

        send(new Packet(Packet.COMMAND_ENTITY_ATTACK, data));
        flushConn();
    }

    /**
     * Send the game options to the server
     */
    public void sendGameOptions(String password, Vector<IBasicOption> options) {
        final Object[] data = new Object[2];
        data[0] = password;
        data[1] = options;
        send(new Packet(Packet.COMMAND_SENDING_GAME_SETTINGS, data));
    }

    /**
     * Send the game settings to the server
     */
    public void sendMapSettings(MapSettings settings) {
        send(new Packet(Packet.COMMAND_SENDING_MAP_SETTINGS, settings));
    }

    /**
     * Send the planetary Conditions to the server
     */
    public void sendPlanetaryConditions(PlanetaryConditions conditions) {
        send(new Packet(Packet.COMMAND_SENDING_PLANETARY_CONDITIONS, conditions));
    }

    /**
     * Send the game settings to the server
     */
    public void sendMapQuery(MapSettings query) {
        send(new Packet(Packet.COMMAND_QUERY_MAP_SETTINGS, query));
    }

    /**
     * Broadcast a general chat message from the local player
     */
    public void sendChat(String message) {
        send(new Packet(Packet.COMMAND_CHAT, message));
        flushConn();
    }

    /**
     * Sends a "player done" message to the server.
     */
    public synchronized void sendDone(boolean done) {
        send(new Packet(Packet.COMMAND_PLAYER_READY, new Boolean(done)));
        flushConn();
    }

    /**
     * Sends a "reroll initiative" message to the server.
     */
    public void sendRerollInitiativeRequest() {
        send(new Packet(Packet.COMMAND_REROLL_INITIATIVE));
    }

    /**
     * Sends the info associated with the local player.
     */
    public void sendPlayerInfo() {
        Player player = game.getPlayer(local_pn);
        PreferenceManager.getClientPreferences().setLastPlayerColor(
                player.getColorIndex());
        PreferenceManager.getClientPreferences().setLastPlayerCategory(
                player.getCamoCategory());
        PreferenceManager.getClientPreferences().setLastPlayerCamoName(
                player.getCamoFileName());
        send(new Packet(Packet.COMMAND_PLAYER_UPDATE, player));
    }

    /**
     * Sends an "add entity" packet
     */
    public void sendAddEntity(Entity entity) {
        checkDuplicateNamesDuringAdd(entity);
        send(new Packet(Packet.COMMAND_ENTITY_ADD, entity));
    }

    /**
     * Sends an "add squadron" packet
     * This is not working, don't use it
     */
    public void sendAddSquadron(Vector<Entity> fighters) {
        //checkDuplicateNamesDuringAdd(fs);
        send(new Packet(Packet.COMMAND_SQUADRON_ADD, fighters));
    }

    /**
     * Sends an "deploy minefields" packet
     */
    public void sendDeployMinefields(Vector<Minefield> minefields) {
        send(new Packet(Packet.COMMAND_DEPLOY_MINEFIELDS, minefields));
    }

    /**
     * Sends a "set Artillery Autohit Hexes" packet
     */
    public void sendArtyAutoHitHexes(Vector<Coords> hexes) {
        artilleryAutoHitHexes = hexes; // save for minimap use
        send(new Packet(Packet.COMMAND_SET_ARTYAUTOHITHEXES, hexes));
    }

    /**
     * Sends an "update entity" packet
     */
    public void sendUpdateEntity(Entity entity) {
        send(new Packet(Packet.COMMAND_ENTITY_UPDATE, entity));
    }

    /**
     * Sends an "update custom initiative" packet
     */
    public void sendCustomInit(Player player) {
        send(new Packet(Packet.COMMAND_CUSTOM_INITIATIVE, player));
    }

    /**
     * Sends a "delete entity" packet
     */
    public void sendDeleteEntity(int id) {
        checkDuplicateNamesDuringDelete(id);
        send(new Packet(Packet.COMMAND_ENTITY_REMOVE, new Integer(id)));
    }

    /***
     * sends a load game file to the server
     */
    public void sendLoadGame(File f) {
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new FileInputStream(f));
            send(new Packet(Packet.COMMAND_LOAD_GAME, new Object[] { f, ois.readObject() }));
        } catch (Exception e) {
            System.out.println("Can't find local savegame "+f);
        }
    }

    /**
     * Receives player information from the message packet.
     */
    protected void receivePlayerInfo(Packet c) {
        int pindex = c.getIntValue(0);
        Player newPlayer = (Player) c.getObject(1);
        if (getPlayer(newPlayer.getId()) == null) {
            game.addPlayer(pindex, newPlayer);
        } else {
            game.setPlayer(pindex, newPlayer);
        }

        PreferenceManager.getClientPreferences().setLastPlayerColor(
                newPlayer.getColorIndex());
        PreferenceManager.getClientPreferences().setLastPlayerCategory(
                newPlayer.getCamoCategory());
        PreferenceManager.getClientPreferences().setLastPlayerCamoName(
                newPlayer.getCamoFileName());
    }

    /**
     * Loads the turn list from the data in the packet
     */
    @SuppressWarnings("unchecked")
    protected void receiveTurns(Packet packet) {
        game.setTurnVector((Vector<GameTurn>) packet.getObject(0));
    }

    /**
     * Loads the board from the data in the net command.
     */
    protected void receiveBoard(Packet c) {
        Board newBoard = (Board) c.getObject(0);
        game.setBoard(newBoard);
    }

    /**
     * Loads the entities from the data in the net command.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntities(Packet c) {
        Vector<Entity> newEntities = (Vector<Entity>) c.getObject(0);
        Vector<Entity> newOutOfGame = (Vector<Entity>) c.getObject(1);

        // Replace the entities in the game.
        game.setEntitiesVector(newEntities);
        if (newOutOfGame != null) {
            game.setOutOfGameEntitiesVector(newOutOfGame);
        }
    }

    /**
     * Loads entity update data from the data in the net command.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntityUpdate(Packet c) {
        int eindex = c.getIntValue(0);
        Entity entity = (Entity) c.getObject(1);
        Vector<UnitLocation> movePath = (Vector<UnitLocation>) c.getObject(2);
        // Replace this entity in the game.
        game.setEntity(eindex, entity, movePath);
    }

    protected void receiveEntityAdd(Packet packet) {
        int entityId = packet.getIntValue(0);
        Entity entity = (Entity) packet.getObject(1);

        // Add the entity to the game.
        game.addEntity(entityId, entity);
    }

    protected void receiveEntityRemove(Packet packet) {
        int entityId = packet.getIntValue(0);
        int condition = packet.getIntValue(1);
        // Move the unit to its final resting place.
        game.removeEntity(entityId, condition);
    }

    protected void receiveEntityVisibilityIndicator(Packet packet) {
        Entity e = game.getEntity(packet.getIntValue(0));
        if (e != null) { // we may not have this entity due to double blind
            e.setSeenByEnemy(packet.getBooleanValue(1));
            e.setVisibleToEnemy(packet.getBooleanValue(2));
            // this next call is only needed sometimes, but we'll just
            // call it everytime
            game.processGameEvent(new GameEntityChangeEvent(this, e));
        }
    }

    @SuppressWarnings("unchecked")
    protected void receiveDeployMinefields(Packet packet) {
        game.addMinefields((Vector<Minefield>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveSendingMinefields(Packet packet) {
        game.setMinefields((Vector<Minefield>) packet.getObject(0));
    }

    protected void receiveRevealMinefield(Packet packet) {
        game.addMinefield((Minefield) packet.getObject(0));
    }

    protected void receiveRemoveMinefield(Packet packet) {
        game.removeMinefield((Minefield) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveUpdateMinefields(Packet packet) {
        //only update information if you know about the minefield
        Vector<Minefield> newMines = new Vector<Minefield>();
        for(Minefield mf : (Vector<Minefield>)packet.getObject(0)) {
            if(getLocalPlayer().containsMinefield(mf)) {
                newMines.add(mf);
            }
        }
        if(newMines.size() > 0) {
            game.resetMinefieldDensity(newMines);
        }
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingUpdateCF(Packet packet) {
        game.getBoard()
                .updateBuildingCF((Vector<Building>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingCollapse(Packet packet) {
        game.getBoard()
                .collapseBuilding((Vector<Coords>) packet.getObject(0));
    }

    /**
     * Loads entity firing data from the data in the net command
     */
    @SuppressWarnings("unchecked")
    protected void receiveAttack(Packet c) {
        Vector<EntityAction> vector = (Vector<EntityAction>) c.getObject(0);
        int charge = c.getIntValue(1);
        boolean addAction = true;
        for (EntityAction ea : vector) {
            int entityId = ea.getEntityId();
            if ((ea instanceof TorsoTwistAction) && game.hasEntity(entityId)) {
                TorsoTwistAction tta = (TorsoTwistAction) ea;
                Entity entity = game.getEntity(entityId);
                entity.setSecondaryFacing(tta.getFacing());
            } else if ((ea instanceof FlipArmsAction) && game.hasEntity(entityId)) {
                FlipArmsAction faa = (FlipArmsAction) ea;
                Entity entity = game.getEntity(entityId);
                entity.setArmsFlipped(faa.getIsFlipped());
            } else if ((ea instanceof DodgeAction) && game.hasEntity(entityId)) {
                Entity entity = game.getEntity(entityId);
                entity.dodging = true;
                addAction = false;
            } else if (ea instanceof AttackAction) {
                // The equipment type of a club needs to be restored.
                if (ea instanceof ClubAttackAction) {
                    ClubAttackAction caa = (ClubAttackAction) ea;
                    Mounted club = caa.getClub();
                    club.restore();
                }
            }

            if (addAction) {
                // track in the appropriate list
                if (charge == 0) {
                    game.addAction(ea);
                } else if (charge == 1) {
                    game.addCharge((AttackAction) ea);
                }
            }
        }
    }

    // Should be private?
    public String receiveReport(Vector<Report> v) {
        boolean doubleBlind = false;

        if (v == null) {
            return "[null report vector]";
        }

        StringBuffer report = new StringBuffer();
        for (int i = 0; i < v.size(); i++) {
            report.append((v.elementAt(i)).getText());
        }

        /*
         * This make Double blind fully blind. Its best to do this here as at
         * the server level you can have two lines merged, i.e. no new line, and
         * the second line doesn't have any obsuring data. This way once the
         * line gets to the client we filter it. --Torren
         */
        while (game.getOptions().booleanOption(
                "supress_all_double_blind_messages")
                && (report.indexOf(Report.OBSCURED_STRING) != -1)) {
            doubleBlind = true;
            int startPos = report.indexOf(Report.OBSCURED_STRING);
            int endPos = report.indexOf("\n", startPos);
            if (report.lastIndexOf("\n", startPos) != -1) {
                startPos = report.lastIndexOf("\n", startPos);
            }

            // In case we get obscured reports but not final \n
            if (endPos <= 0) {
                endPos = report.length();
            }

            if (startPos < 0) {
                startPos = 0;
            }

            report.delete(startPos, endPos);
        }

        String endReport = report.toString();

        // Get rid of some extra double spaces that the pasing can sometimes
        // cause.
        while ((endReport.indexOf("\n\n") != -1) && doubleBlind) {
            // Looks silly but it slows the proccess down enough to keep an
            // Inf Loop from happening. -- Torren
            endReport = endReport.replaceAll("\n\n", "\n");
        }

        return endReport;
    }

    /**
     * Saves server entity status data to a local file
     */
    private void saveEntityStatus(String sStatus) {
        try {
            String sLogDir = PreferenceManager.getClientPreferences()
                    .getLogDirectory();
            File logDir = new File(sLogDir);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            String fileName = "entitystatus.txt";
            if (PreferenceManager.getClientPreferences().stampFilenames()) {
                fileName = StringUtil.addDateTimeStamp(fileName);
            }
            FileWriter fw = new FileWriter(sLogDir + File.separator + fileName);
            fw.write(sStatus);
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * send the message to the server
     */
    protected void send(Packet packet) {
        connection.send(packet);
    }

    /**
     * send all buffered packets on their way this should be called after
     * everything which causes us to wait for a reply. For example "done" button
     * presses etc. to make stuff more efficient, this should only be called
     * after a batch of packets is sent,not separately for each packet
     */
    protected void flushConn() {
        connection.flush();
    }

    @SuppressWarnings("unchecked")
    protected void handlePacket(Packet c) {
        if (c == null) {
            System.out.println("client: got null packet"); //$NON-NLS-1$
            return;
        }
        switch (c.getCommand()) {
            case Packet.COMMAND_CLOSE_CONNECTION:
                disconnected();
                break;
            case Packet.COMMAND_RESET_CONNECTION:
                disconnected();
                connect();
                break;
            case Packet.COMMAND_SERVER_GREETING:
                connected = true;
                send(new Packet(Packet.COMMAND_CLIENT_NAME, name));
                break;
            case Packet.COMMAND_SERVER_CORRECT_NAME:
                correctName(c);
                break;
            case Packet.COMMAND_LOCAL_PN:
                local_pn = c.getIntValue(0);
                break;
            case Packet.COMMAND_PLAYER_UPDATE:
                receivePlayerInfo(c);
                break;
            case Packet.COMMAND_PLAYER_READY:
                getPlayer(c.getIntValue(0)).setDone(c.getBooleanValue(1));
                break;
            case Packet.COMMAND_PLAYER_ADD:
                receivePlayerInfo(c);
                break;
            case Packet.COMMAND_PLAYER_REMOVE:
                game.removePlayer(c.getIntValue(0));
                break;
            case Packet.COMMAND_CHAT:
                if (log == null) {
                    initGameLog();
                }
                if ((log != null) && keepGameLog()) {
                    log.append((String) c.getObject(0));
                }
                game.processGameEvent(new GamePlayerChatEvent(this, null,
                        (String) c.getObject(0)));
                break;
            case Packet.COMMAND_ENTITY_ADD:
                receiveEntityAdd(c);
                break;
            case Packet.COMMAND_ENTITY_UPDATE:
                receiveEntityUpdate(c);
                break;
            case Packet.COMMAND_ENTITY_REMOVE:
                receiveEntityRemove(c);
                break;
            case Packet.COMMAND_ENTITY_VISIBILITY_INDICATOR:
                receiveEntityVisibilityIndicator(c);
                break;
            case Packet.COMMAND_SENDING_MINEFIELDS:
                receiveSendingMinefields(c);
                break;
            case Packet.COMMAND_UPDATE_MINEFIELDS:
                receiveUpdateMinefields(c);
                break;
            case Packet.COMMAND_DEPLOY_MINEFIELDS:
                receiveDeployMinefields(c);
                break;
            case Packet.COMMAND_REVEAL_MINEFIELD:
                receiveRevealMinefield(c);
                break;
            case Packet.COMMAND_REMOVE_MINEFIELD:
                receiveRemoveMinefield(c);
                break;
            case Packet.COMMAND_CHANGE_HEX:
                game.getBoard().setHex((Coords) c.getObject(0),
                        (IHex) c.getObject(1));
                break;
            case Packet.COMMAND_BLDG_UPDATE_CF:
                receiveBuildingUpdateCF(c);
                break;
            case Packet.COMMAND_BLDG_COLLAPSE:
                receiveBuildingCollapse(c);
                break;
            case Packet.COMMAND_PHASE_CHANGE:
                changePhase((IGame.Phase) c.getObject(0));
                break;
            case Packet.COMMAND_TURN:
                changeTurnIndex(c.getIntValue(0));
                break;
            case Packet.COMMAND_ROUND_UPDATE:
                game.setRoundCount(c.getIntValue(0));
                break;
            case Packet.COMMAND_SENDING_TURNS:
                receiveTurns(c);
                break;
            case Packet.COMMAND_SENDING_BOARD:
                receiveBoard(c);
                break;
            case Packet.COMMAND_SENDING_ENTITIES:
                receiveEntities(c);
                break;
            case Packet.COMMAND_SENDING_REPORTS:
            case Packet.COMMAND_SENDING_REPORTS_TACTICAL_GENIUS:
                phaseReport = receiveReport((Vector) c.getObject(0));
                if (keepGameLog()) {
                    if ((log == null) && (game.getRoundCount() == 1)) {
                        initGameLog();
                    }
                    if (log != null) {
                        log.append(phaseReport);
                    }
                }
                game.addReports((Vector<Report>) c.getObject(0));
                roundReport = receiveReport(game.getReports(game
                        .getRoundCount()));
                if (c.getCommand() == Packet.COMMAND_SENDING_REPORTS_TACTICAL_GENIUS) {
                    game.processGameEvent(new GameReportEvent(this, null));
                }
                break;
            case Packet.COMMAND_SENDING_REPORTS_SPECIAL:
                game.processGameEvent(new GameReportEvent(this,
                        receiveReport((Vector) c.getObject(0))));
                break;
            case Packet.COMMAND_SENDING_REPORTS_ALL:
                Vector<Vector<Report>> allReports = (Vector<Vector<Report>>) c
                        .getObject(0);
                game.setAllReports(allReports);
                if (keepGameLog()) {
                    // Re-write gamelog.txt from scratch
                    initGameLog();
                    if (log != null) {
                        for (int i = 0; i < allReports.size(); i++) {
                            log.append(receiveReport(allReports.elementAt(i)));
                        }
                    }
                }
                roundReport = receiveReport(game.getReports(game
                        .getRoundCount()));
                // We don't really have a copy of the phase report at
                // this point, so I guess we'll just use the round report
                // until the next phase actually completes.
                phaseReport = roundReport;
                break;
            case Packet.COMMAND_ENTITY_ATTACK:
                receiveAttack(c);
                break;
            case Packet.COMMAND_SENDING_GAME_SETTINGS:
                game.setOptions((GameOptions) c.getObject(0));
                break;
            case Packet.COMMAND_SENDING_MAP_SETTINGS:
                mapSettings = (MapSettings) c.getObject(0);
                game.processGameEvent(new GameSettingsChangeEvent(this));
                break;
            case Packet.COMMAND_SENDING_PLANETARY_CONDITIONS:
                game.setPlanetaryConditions((PlanetaryConditions) c.getObject(0));
                game.processGameEvent(new GameSettingsChangeEvent(this));
                break;
            case Packet.COMMAND_QUERY_MAP_SETTINGS:
                game.processGameEvent(new GameMapQueryEvent(this,
                        (MapSettings) c.getObject(0)));
                break;
            case Packet.COMMAND_END_OF_GAME:
                String sEntityStatus = (String) c.getObject(0);
                game.end(c.getIntValue(1), c.getIntValue(2));
                // save victory report
                saveEntityStatus(sEntityStatus);
                break;
            case Packet.COMMAND_SENDING_ARTILLERYATTACKS:
                Vector<ArtilleryAttackAction> v = (Vector<ArtilleryAttackAction>) c
                        .getObject(0);
                game.setArtilleryVector(v);
                break;
            case Packet.COMMAND_SENDING_FLARES:
                Vector<Flare> v2 = (Vector<Flare>) c.getObject(0);
                game.setFlares(v2);
                break;
            case Packet.COMMAND_SEND_SAVEGAME:
                String sFinalFile = (String) c.getObject(0);
                String localFile = "savegames" + File.separator + sFinalFile;
                try {
                    File sDir = new File("savegames");
                    if (!sDir.exists()) {
                        sDir.mkdir();
                    }
                } catch (Exception e) {
                    System.err.println("Unable to create savegames directory");
                }
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(
                            new FileOutputStream(localFile));
                    oos.writeObject(c.getObject(1));
                    oos.flush();
                    oos.close();
                } catch (Exception e) {
                    System.err.println("Unable to save file: " + sFinalFile);
                    e.printStackTrace();
                }
                break;
            case Packet.COMMAND_LOAD_SAVEGAME:
                String loadFile = (String) c.getObject(0);
                try {
                    File f = new File("savegames", loadFile);
                    sendLoadGame(f);
                } catch (Exception e) {
                    System.err.println("Unable to find the file: " + loadFile);
                }
                break;
            case Packet.COMMAND_SENDING_SPECIAL_HEX_DISPLAY:
                game.getBoard().setSpecialHexDisplayTable(
                        (Hashtable<Coords, Collection<SpecialHexDisplay>>) c
                                .getObject(0));
                //System.err.println("Specials updated");
                break;
        }
    }

    /**
     * Perform a dump of the current memory usage. <p/> This method is useful in
     * tracking performance issues on various player's systems. You can activate
     * it by changing the "memorydumpon" setting to "true" in the MegaMek.cfg
     * file.
     *
     * @param where - a <code>String</code> indicating which part of the game
     *            is making this call.
     * @see megamek.common.Settings#memoryDumpOn
     * @see megamek.client.Client#changePhase(int)
     */
    private void memDump(String where) {
        if (PreferenceManager.getClientPreferences().memoryDumpOn()) {
            StringBuffer buf = new StringBuffer();
            final long total = Runtime.getRuntime().totalMemory();
            final long free = Runtime.getRuntime().freeMemory();
            final long used = total - free;
            buf.append("Memory dump ").append(where); //$NON-NLS-1$
            for (int loop = where.length(); loop < 25; loop++) {
                buf.append(' ');
            }
            buf
                    .append(": used (").append(used).append(") + free (").append(free).append(") = ").append(total); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            System.out.println(buf.toString());
        }
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    private void correctName(Packet inP) {
        setName((String) (inP.getObject(0)));
    }

    public void setName(String newN) {
        name = newN;
    }

    /**
     * Before we officially "add" this unit to the game, check and see if this
     * client (player) already has a unit in the game with the same name. If so,
     * add an identifier to the units name.
     */
    private void checkDuplicateNamesDuringAdd(Entity entity) {
        if (duplicateNameHash.get(entity.getShortName()) == null) {
            duplicateNameHash.put(entity.getShortName(), new Integer(1));
        } else {
            int count = duplicateNameHash.get(entity.getShortName()).intValue();
            count++;
            duplicateNameHash.put(entity.getShortName(), new Integer(count));
            entity.duplicateMarker = count;
            entity.generateShortName();
            entity.generateDisplayName();

        }
    }

    /**
     *  If we remove an entity, we may need to update the duplicate identifier.
     *  TODO: This function is super slow :(
     * @param id
     */
    private void checkDuplicateNamesDuringDelete(int id) {
        Entity entity = game.getEntity(id);
        Object o = duplicateNameHash.get(entity.getShortNameRaw());
        if (o != null) {
            int count = ((Integer) o).intValue();
            if (count > 1) {
                ArrayList<Entity> myEntities = game.getPlayerEntities(game
                        .getPlayer(local_pn), false);
                for (int i = 0; i < myEntities.size(); i++) {
                    Entity e = myEntities.get(i);
                    if (e.getShortNameRaw().equals(entity.getShortNameRaw())
                            && (e.duplicateMarker > entity.duplicateMarker)) {
                        e.duplicateMarker--;
                        e.generateShortName();
                        e.generateDisplayName();
                        sendUpdateEntity(e);
                    }
                }
                duplicateNameHash.put(entity.getShortNameRaw(), new Integer(
                        count - 1));
            } else {
                duplicateNameHash.remove(entity.getShortNameRaw());
            }
        }
    }

    /**
     * @param text a client command with CLIENT_COMMAND prepended.
     */
    public String runCommand(String cmd) {
        cmd = cmd.substring(CLIENT_COMMAND.length());

        return runCommand(cmd.split("\\s+"));
    }

    /**
     * Runs the command
     *
     * @param args the command and it's arguments with the CLIENT_COMMAND
     *            already removed, and the string tokenized.
     */
    public String runCommand(String[] args) {
        if ((args != null) && (args.length > 0)
                && commandsHash.containsKey(args[0])) {
            return commandsHash.get(args[0]).run(args);
        }
        return "Unknown Client Command.";
    }

    /**
     * Registers a new command in the client command table
     */
    public void registerCommand(ClientCommand command) {
        commandsHash.put(command.getName(), command);
    }

    /**
     * Returns the command associated with the specified name
     */
    public ClientCommand getCommand(String commandName) {
        return commandsHash.get(commandName);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.client.ui.IClientCommandHandler#getAllCommandNames()
     */
    public Enumeration<String> getAllCommandNames() {
        return commandsHash.keys();
    }
}
