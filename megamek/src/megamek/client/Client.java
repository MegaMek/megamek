/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.SwingUtilities;

import megamek.MegaMek;
import megamek.client.bot.BotClient;
import megamek.client.commands.AddBotCommand;
import megamek.client.commands.AssignNovaNetworkCommand;
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
import megamek.common.BoardDimensions;
import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.QuirksHandler;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.FighterSquadron;
import megamek.common.Flare;
import megamek.common.Game;
import megamek.common.GameLog;
import megamek.common.GameTurn;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.MapSettings;
import megamek.common.MechFileParser;
import megamek.common.MechSummaryCache;
import megamek.common.Minefield;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.PlanetaryConditions;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.TagInfo;
import megamek.common.UnitLocation;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DodgeAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameVictoryEvent;
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
import megamek.server.SmokeCloud;

import com.thoughtworks.xstream.XStream;

/**
 * This class is instanciated for each client and for each bot running on that
 * client. non-local clients are not also instantiated on the local server.
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
    protected int localPlayerNumber = -1;
    private String host;
    private int port;

    // the game state object
    protected IGame game = new Game();

    // here's some game phase stuff
    private MapSettings mapSettings;
    public String phaseReport;
    public String roundReport;

    // random generatorsI
    private RandomSkillsGenerator rsg;
    // And close client events!
    private Vector<CloseClientListener> closeClientListeners = new Vector<CloseClientListener>();

    // we might want to keep a game log...
    private GameLog log;

    private Set<BoardDimensions> availableSizes = new TreeSet<BoardDimensions>();

    private Vector<Coords> artilleryAutoHitHexes = null;

    private boolean disconnectFlag = false;

    private Hashtable<String, Integer> duplicateNameHash = new Hashtable<String, Integer>();

    public Map<String, Client> bots = new TreeMap<String, Client>(
            StringUtil.stringComparator());

    ConnectionHandler packetUpdate;

    private class ConnectionHandler implements Runnable {

        boolean shouldStop = false;

        public void signalStop() {
            shouldStop = true;
        }

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

    ;

    private Thread connThread;

    private ConnectionListenerAdapter connectionListener = new ConnectionListenerAdapter() {

        /**
         * Called when it is sensed that a connection has terminated.
         */
        @Override
        public void disconnected(DisconnectedEvent e) {
            // We can't just run this directly, otherwise we open up all sorts
            //  of concurrency issues with the AWT event dispatch thread.
            // Instead, if we will have the event dispatch thread handle it,
            // by using SwingUtilities.invokeLater
            // Not running this on the AWT EDT can lead to dead-lock
            Runnable handlePacketEvent = new Runnable() {
                public void run() {
                    Client.this.disconnected();
                }
            };
            SwingUtilities.invokeLater(handlePacketEvent);            
        }

        @Override
        public void packetReceived(final PacketReceivedEvent e) {
            // We can't just run this directly, otherwise we open up all sorts
            //  of concurrency issues with the AWT event dispatch thread.
            // Instead, if we will have the event dispatch thread handle it,
            // by using SwingUtilities.invokeLater
            // TODO: I don't think this is really what we should do: ideally
            //  Client.handlePacket should play well with the AWT event queue,
            //  but nothing appears to really be designed to be thread safe, so
            //  this is a reasonable hack for now
            Runnable handlePacketEvent = new Runnable() {
                public void run() {
                    handlePacket(e.getPacket());
                }
            };
            SwingUtilities.invokeLater(handlePacketEvent);
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
        registerCommand(new AssignNovaNetworkCommand(this));

        rsg = new RandomSkillsGenerator();
    }

    public int getLocalPlayerNumber() {
        return localPlayerNumber;
    }

    public void setLocalPlayerNumber(int localPlayerNumber) {
        this.localPlayerNumber = localPlayerNumber;
    }

    /**
     * call this once to update the connection
     */
    protected void updateConnection() {
        if (connection != null && !connection.isClosed()) {
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
            packetUpdate = new ConnectionHandler();
            connThread = new Thread(packetUpdate, "Client Connection, Player "
                    + name);
            connThread.start();
        }
        return result;
    }

    /**
     * Shuts down threads and sockets
     */
    public synchronized void die() {
        // If we're still connected, tell the server that we're going down.
        if (connected) {
            // Stop listening for in coming packets, this should be done before
            //  sending the close connection command
            packetUpdate.signalStop();
            connThread.interrupt();
            send(new Packet(Packet.COMMAND_CLOSE_CONNECTION));
            flushConn();
        }
        connected = false;

        if (connection != null) {
            connection.close();
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
        log.append("<html><body>");
    }

    private boolean keepGameLog() {
        return PreferenceManager.getClientPreferences().keepGameLog()
               && !(this instanceof BotClient);
    }

    /**
     * Return an enumeration of the players in the game
     */
    public Enumeration<IPlayer> getPlayers() {
        return game.getPlayers();
    }

    public Entity getEntity(int id) {
        return game.getEntity(id);
    }

    /**
     * Returns the individual player assigned the index parameter.
     */
    public IPlayer getPlayer(int idx) {
        return game.getPlayer(idx);
    }

    /**
     * Return the local player
     */
    public IPlayer getLocalPlayer() {
        return getPlayer(localPlayerNumber);
    }

    /**
     * Returns an <code>Enumeration</code> of the entities that match the
     * selection criteria.
     */
    public Iterator<Entity> getSelectedEntities(EntitySelector selector) {
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
     * Returns the number of the previous selectable entity after the one given
     */
    public int getPrevEntityNum(int entityId) {
        return game.getPrevEntityNum(getMyTurn(), entityId);
    }

    /**
     * Returns the number of the first deployable entity
     */
    public int getFirstDeployableEntityNum() {
        return game.getFirstDeployableEntityNum(getMyTurn());
    }

    /**
     * Returns the number of the next deployable entity
     */
    public int getNextDeployableEntityNum(int entityId) {
        return game.getNextDeployableEntityNum(getMyTurn(), entityId);
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
    public List<Entity> getEntitiesVector() {
        return game.getEntitiesVector();
    }

    public MapSettings getMapSettings() {
        return mapSettings;
    }

    /**
     * give the initiative to the next player on the team.
     */
    public void sendNextPlayer() {
        connection.send(new Packet(Packet.COMMAND_FORWARD_INITIATIVE));
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
                MechFileParser.dispose();
                getRandomNameGenerator().dispose();
                // We must do this last, as the name and unit generators can
                // create
                // a new instance if they are running
                MechSummaryCache.dispose();
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
                try {
                    QuirksHandler.initQuirksList();
                } catch (IOException e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
                RandomNameGenerator.initialize();
                MechSummaryCache.getInstance().addListener(
                        new MechSummaryCache.Listener() {
                            public void doneLoading() {
                                RandomUnitGenerator.getInstance();
                            }
                        });
                if (MechSummaryCache.getInstance().isInitialized()) {
                    RandomUnitGenerator.getInstance();
                }
                duplicateNameHash.clear(); // reset this
                break;
            default:
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
     * is it my turn?
     */
    public boolean isMyTurn() {
        if (game.isPhaseSimultaneous()) {
            return game.getTurnForPlayer(localPlayerNumber) != null;
        }
        return (game.getTurn() != null)
               && game.getTurn().isValid(localPlayerNumber, game);
    }

    public GameTurn getMyTurn() {
        if (game.isPhaseSimultaneous()) {
            return game.getTurnForPlayer(localPlayerNumber);
        }
        return game.getTurn();
    }

    /**
     * Can I unload entities stranded on immobile transports?
     */
    public boolean canUnloadStranded() {
        return (game.getTurn() instanceof GameTurn.UnloadStrandedTurn)
               && game.getTurn().isValid(localPlayerNumber, game);
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
        Object[] data = {new Integer(nEntity), new Integer(nEquip),
                         new Integer(nMode)};
        send(new Packet(Packet.COMMAND_ENTITY_MODECHANGE, data));
    }

    /**
     * Send mount-facing-change data to the server
     */
    public void sendMountFacingChange(int nEntity, int nEquip, int nFacing) {
        Object[] data = {new Integer(nEntity), new Integer(nEquip),
                         new Integer(nFacing)};
        send(new Packet(Packet.COMMAND_ENTITY_MOUNTED_FACINGCHANGE, data));
    }

    /**
     * Send called shot change data to the server
     */
    public void sendCalledShotChange(int nEntity, int nEquip) {
        Object[] data = {new Integer(nEntity), new Integer(nEquip)};
        send(new Packet(Packet.COMMAND_ENTITY_CALLEDSHOTCHANGE, data));
    }

    /**
     * Send system mode-change data to the server
     */
    public void sendSystemModeChange(int nEntity, int nSystem, int nMode) {
        Object[] data = {new Integer(nEntity), new Integer(nSystem),
                         new Integer(nMode)};
        send(new Packet(Packet.COMMAND_ENTITY_SYSTEMMODECHANGE, data));
    }

    /**
     * Send mode-change data to the server
     */
    public void sendAmmoChange(int nEntity, int nWeapon, int nAmmo) {
        Object[] data = {new Integer(nEntity), new Integer(nWeapon),
                         new Integer(nAmmo)};
        send(new Packet(Packet.COMMAND_ENTITY_AMMOCHANGE, data));
    }
    
    /**
     * Send sensor-change data to the server
     */
    public void sendSensorChange(int nEntity, int nSensor) {
        Object[] data = {new Integer(nEntity), new Integer(nSensor)};
        send(new Packet(Packet.COMMAND_ENTITY_SENSORCHANGE, data));
    }
    
    /**
     * Send sinks-change data to the server
     */
    public void sendSinksChange(int nEntity, int activeSinks) {
        Object[] data = {new Integer(nEntity), new Integer(activeSinks)};
        send(new Packet(Packet.COMMAND_ENTITY_SINKSCHANGE, data));
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
     * @param id      - the <code>int</code> ID of the deployed entity
     * @param c       - the <code>Coords</code> where the entity should be deployed
     * @param nFacing - the <code>int</code> direction the entity should face
     */
    public void deploy(int id, Coords c, int nFacing, int elevation) {
        this.deploy(id, c, nFacing, elevation, new Vector<Entity>(), false);
    }

    /**
     * Deploy an entity at the given coordinates, with the given facing, and
     * starting with the given units already loaded.
     *
     * @param id          - the <code>int</code> ID of the deployed entity
     * @param c           - the <code>Coords</code> where the entity should be deployed
     * @param nFacing     - the <code>int</code> direction the entity should face
     * @param loadedUnits - a <code>List</code> of units that start the game being
     *                    transported byt the deployed entity.
     * @param assaultDrop - true if deployment is an assault drop
     */
    public void deploy(int id, Coords c, int nFacing, int elevation,
                       List<Entity> loadedUnits, boolean assaultDrop) {
        int packetCount = 6 + loadedUnits.size();
        int index = 0;
        Object[] data = new Object[packetCount];
        data[index++] = new Integer(id);
        data[index++] = c;
        data[index++] = new Integer(nFacing);
        data[index++] = new Integer(elevation);
        data[index++] = new Integer(loadedUnits.size());
        data[index++] = new Boolean(assaultDrop);

        for (Entity ent : loadedUnits) {
            data[index++] = new Integer(ent.getId());
        }

        send(new Packet(Packet.COMMAND_ENTITY_DEPLOY, data));
        flushConn();
    }
    
    /**
     * For ground to air attacks, the ground unit targets the closest hex in
     * the air units flight path.  In the case of several equidistant hexes,
     * the attacker gets to choose.  This method updates the server with the
     * users choice.
     * 
     * @param targetId
     * @param attackerId
     * @param pos
     */
    public void sendPlayerPickedPassThrough(Integer targetId,
            Integer attackerId, Coords pos) {
        Object[] data = new Object[3];
        data[0] = targetId;
        data[1] = attackerId;
        data[2] = pos;
        
        send(new Packet(Packet.COMMAND_ENTITY_GTA_HEX_SELECT, data));
    }

    /**
     * Send a weapon fire command to the server.
     */
    public void sendAttackData(int aen, Vector<EntityAction> attacks) {
        Object[] data = new Object[2];

        data[0] = aen;
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
     * Send the new map selection to the server
     */
    public void sendMapSettings(MapSettings settings) {
        send(new Packet(Packet.COMMAND_SENDING_MAP_SETTINGS, settings));
    }

    /**
     * Send the new map dimensions to the server
     */
    public void sendMapDimensions(MapSettings settings) {
        send(new Packet(Packet.COMMAND_SENDING_MAP_DIMENSIONS, settings));
    }

    /**
     * Send the planetary Conditions to the server
     */
    public void sendPlanetaryConditions(PlanetaryConditions conditions) {
        send(new Packet(Packet.COMMAND_SENDING_PLANETARY_CONDITIONS, conditions));
    }

    /**
     * Broadcast a general chat message from the local player
     */
    public void sendChat(String message) {
        send(new Packet(Packet.COMMAND_CHAT, message));
        flushConn();
    }

    /**
     * Broadcast a general chat message from the local player
     */
    public void sendServerChat(int connId, String message) {
        Object[] data = {message, connId};
        send(new Packet(Packet.COMMAND_CHAT, data));
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
        IPlayer player = game.getPlayer(localPlayerNumber);
        PreferenceManager.getClientPreferences().setLastPlayerColor(
                player.getColorIndex());
        PreferenceManager.getClientPreferences().setLastPlayerCategory(
                player.getCamoCategory());
        PreferenceManager.getClientPreferences().setLastPlayerCamoName(
                player.getCamoFileName());
        send(new Packet(Packet.COMMAND_PLAYER_UPDATE, player));
    }

    /**
     * Reset round deployment packet
     */
    public void sendResetRoundDeployment() {
        send(new Packet(Packet.COMMAND_RESET_ROUND_DEPLOYMENT));
    }

    public void sendEntityWeaponOrderUpdate(Entity entity) {
        Object data[];
        if (entity.getWeaponSortOrder() == Entity.WeaponSortOrder.CUSTOM) {
            data = new Object[3];
            data[2] = entity.getCustomWeaponOrder();
        } else {
            data = new Object[2];
        }
        data[0] = entity.getId();
        data[1] = entity.getWeaponSortOrder();
        send(new Packet(Packet.COMMAND_ENTITY_WORDER_UPDATE, data));
        entity.setWeapOrderChanged(false);
    }

    /**
     * Sends an "add entity" packet with only one Entity.
     *
     * @param entity The Entity to add.
     */
    public void sendAddEntity(Entity entity) {
        ArrayList<Entity> entities = new ArrayList<Entity>(1);
        entities.add(entity);
        sendAddEntity(entities);
    }

    /**
     * Sends an "add entity" packet that contains a collection of Entity
     * objections.
     *
     * @param entities The collection of Entity objects to add.
     */
    public void sendAddEntity(List<Entity> entities) {
        for (Entity entity : entities) {
            checkDuplicateNamesDuringAdd(entity);
        }
        send(new Packet(Packet.COMMAND_ENTITY_ADD, entities));
    }

    /**
     * Sends an "add squadron" packet
     */
    public void sendAddSquadron(FighterSquadron fs, Vector<Integer> fighterIds) {
        checkDuplicateNamesDuringAdd(fs);
        send(new Packet(Packet.COMMAND_SQUADRON_ADD, new Object[]{fs,
                                                                  fighterIds}));
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
     * Sends an "update entity" packet
     */
    public void sendDeploymentUnload(Entity loader, Entity loaded) {
        Object data[] = {loader.getId(), loaded.getId()};
        send(new Packet(Packet.COMMAND_ENTITY_DEPLOY_UNLOAD, data));
    }

    /**
     * Sends an "update custom initiative" packet
     */
    public void sendCustomInit(IPlayer player) {
        send(new Packet(Packet.COMMAND_CUSTOM_INITIATIVE, player));
    }

    /**
     * Sends a "delete entity" packet
     */
    public void sendDeleteEntity(int id) {
        ArrayList<Integer> ids = new ArrayList<Integer>(1);
        ids.add(id);
        sendDeleteEntities(ids);
    }

    public void sendDeleteEntities(List<Integer> ids) {
        checkDuplicateNamesDuringDelete(ids);
        send(new Packet(Packet.COMMAND_ENTITY_REMOVE, ids));
    }

    /**
     * Sends a "load entity" packet
     */
    public void sendLoadEntity(int id, int loaderId, int bayNumber) {
        send(new Packet(Packet.COMMAND_ENTITY_LOAD, new Object[]{id,
                                                                 loaderId, bayNumber}));
    }

    /**
     * sends a load game file to the server
     */
    public void sendLoadGame(File f) {
        try {
            XStream xstream = new XStream();

            game.reset();
            IGame newGame = (IGame) xstream.fromXML(new GZIPInputStream(
                    new FileInputStream(f)));

            send(new Packet(Packet.COMMAND_LOAD_GAME, new Object[]{newGame}));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Can't find local savegame " + f);
        }
    }

    /**
     * Receives player information from the message packet.
     */
    protected void receivePlayerInfo(Packet c) {
        int pindex = c.getIntValue(0);
        IPlayer newPlayer = (IPlayer) c.getObject(1);
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
        game.setTurnVector((List<GameTurn>) packet.getObject(0));
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
        List<Entity> newEntities = (List<Entity>) c.getObject(0);
        List<Entity> newOutOfGame = (List<Entity>) c.getObject(1);

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
        @SuppressWarnings("unchecked")
        List<Integer> entityIds = (List<Integer>) packet.getObject(0);
        @SuppressWarnings("unchecked")
        List<Entity> entities = (List<Entity>) packet.getObject(1);

        assert (entityIds.size() == entities.size());
        for (int i = 0; i < entityIds.size(); i++) {
            assert (entityIds.get(i) == entities.get(i).getId());
        }
        game.addEntities(entities);
    }

    protected void receiveEntityRemove(Packet packet) {
        @SuppressWarnings("unchecked")
        List<Integer> entityIds = (List<Integer>) packet.getObject(0);
        int condition = packet.getIntValue(1);
        // Move the unit to its final resting place.
        game.removeEntities(entityIds, condition);
    }

    @SuppressWarnings("unchecked")
    protected void receiveEntityVisibilityIndicator(Packet packet) {
        Entity e = game.getEntity(packet.getIntValue(0));
        if (e != null) { // we may not have this entity due to double blind
            e.setEverSeenByEnemy(packet.getBooleanValue(1));
            e.setVisibleToEnemy(packet.getBooleanValue(2));
            e.setDetectedByEnemy(packet.getBooleanValue(3));
            e.setWhoCanSee((Vector<IPlayer>)packet.getObject(4));
            e.setWhoCanDetect((Vector<IPlayer>)packet.getObject(5));
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

    @SuppressWarnings("unchecked")
    protected void receiveIlluminatedHexes(Packet p) {
        game.setIlluminatedPositions((HashSet<Coords>) p.getObject(0));
    }

    protected void receiveRevealMinefield(Packet packet) {
        game.addMinefield((Minefield) packet.getObject(0));
    }

    protected void receiveRemoveMinefield(Packet packet) {
        game.removeMinefield((Minefield) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveUpdateMinefields(Packet packet) {
        // only update information if you know about the minefield
        Vector<Minefield> newMines = new Vector<Minefield>();
        for (Minefield mf : (Vector<Minefield>) packet.getObject(0)) {
            if (getLocalPlayer().containsMinefield(mf)) {
                newMines.add(mf);
            }
        }
        if (newMines.size() > 0) {
            game.resetMinefieldDensity(newMines);
        }
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingUpdate(Packet packet) {
        game.getBoard().updateBuildings((Vector<Building>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingCollapse(Packet packet) {
        game.getBoard().collapseBuilding((Vector<Coords>) packet.getObject(0));
    }

    /**
     * Loads entity firing data from the data in the net command
     */
    @SuppressWarnings("unchecked")
    protected void receiveAttack(Packet c) {
        List<EntityAction> vector = (List<EntityAction>) c.getObject(0);
        int charge = c.getIntValue(1);
        boolean addAction = true;
        for (EntityAction ea : vector) {
            int entityId = ea.getEntityId();
            if ((ea instanceof TorsoTwistAction) && game.hasEntity(entityId)) {
                TorsoTwistAction tta = (TorsoTwistAction) ea;
                Entity entity = game.getEntity(entityId);
                entity.setSecondaryFacing(tta.getFacing());
            } else if ((ea instanceof FlipArmsAction)
                       && game.hasEntity(entityId)) {
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
        if (v == null) {
            return "[null report vector]";
        }

        StringBuffer report = new StringBuffer();
        for (Report r : v) {
            report.append(r.getText());
        }
        return report.toString();
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
        if (connection != null) {
            connection.send(packet);
        }
    }

    /**
     * Send a Nova CEWS update packet
     *
     * @param ID
     * @param net
     */
    public void sendNovaChange(int ID, String net) {
        Object[] data = {new Integer(ID), new String(net)};
        Packet packet = new Packet(Packet.COMMAND_ENTITY_NOVA_NETWORK_CHANGE,
                                   data);
        send(packet);
    }

    public void sendSpecialHexDisplayAppend(Coords c, SpecialHexDisplay shd) {
        Object[] data = {c, shd};
        Packet packet = new Packet(Packet.COMMAND_SPECIAL_HEX_DISPLAY_APPEND,
                                   data);
        send(packet);
    }

    public void sendSpecialHexDisplayDelete(Coords c, SpecialHexDisplay shd) {
        Object[] data = {c, shd};
        Packet packet = new Packet(Packet.COMMAND_SPECIAL_HEX_DISPLAY_DELETE,
                                   data);
        send(packet);
    }

    /**
     * send all buffered packets on their way this should be called after
     * everything which causes us to wait for a reply. For example "done" button
     * presses etc. to make stuff more efficient, this should only be called
     * after a batch of packets is sent,not separately for each packet
     */
    protected void flushConn() {
        if (connection != null) {
            connection.flush();
        }
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
            case Packet.COMMAND_SERVER_GREETING:
                connected = true;
                send(new Packet(Packet.COMMAND_CLIENT_NAME, name));
                Object[] versionData = new Object[2];
                versionData[0] = MegaMek.VERSION;
                versionData[1] = MegaMek.getMegaMekSHA256();
                send(new Packet(Packet.COMMAND_CLIENT_VERSIONS, versionData));
                break;
            case Packet.COMMAND_SERVER_CORRECT_NAME:
                correctName(c);
                break;
            case Packet.COMMAND_LOCAL_PN:
                localPlayerNumber = c.getIntValue(0);
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
                for (Iterator<Client> botIterator = bots.values().iterator(); botIterator
                        .hasNext(); ) {
                    Client bot = botIterator.next();
                    if (bot.localPlayerNumber == c.getIntValue(0)) {
                        botIterator.remove();
                    }
                }
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
            case Packet.COMMAND_SENDING_ILLUM_HEXES:
                receiveIlluminatedHexes(c);
                break;
            case Packet.COMMAND_CLEAR_ILLUM_HEXES:
                game.clearIlluminatedPositions();
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
            case Packet.COMMAND_ADD_SMOKE_CLOUD:
                SmokeCloud cloud = (SmokeCloud) c.getObject(0);
                game.addSmokeCloud(cloud);
                break;
            case Packet.COMMAND_CHANGE_HEX:
                game.getBoard().setHex((Coords) c.getObject(0),
                                       (IHex) c.getObject(1));
                break;
            case Packet.COMMAND_CHANGE_HEXES:
                List<Coords> coords = new ArrayList<Coords>(
                        (Set<Coords>) c.getObject(0));
                List<IHex> hexes = new ArrayList<IHex>(
                        (Set<IHex>) c.getObject(1));
                game.getBoard().setHexes(coords, hexes);
                break;
            case Packet.COMMAND_BLDG_UPDATE:
                receiveBuildingUpdate(c);
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
                phaseReport = receiveReport((Vector<Report>) c.getObject(0));
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
                    game.processGameEvent(new GameReportEvent(this, roundReport));
                }
                break;
            case Packet.COMMAND_SENDING_REPORTS_SPECIAL:
                game.processGameEvent(new GameReportEvent(this,
                                                          receiveReport((Vector<Report>) c.getObject(0))));
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
                mapSettings.adjustPathSeparator();
                GameSettingsChangeEvent evt = new GameSettingsChangeEvent(this);
                evt.setMapSettingsOnlyChange(true);
                game.processGameEvent(evt);
                break;
            case Packet.COMMAND_SENDING_PLANETARY_CONDITIONS:
                game.setPlanetaryConditions((PlanetaryConditions) c
                        .getObject(0));
                game.processGameEvent(new GameSettingsChangeEvent(this));
                break;
            case Packet.COMMAND_SENDING_TAGINFO:
                Vector<TagInfo> vti = (Vector<TagInfo>) c.getObject(0);
                for (TagInfo ti : vti) {
                    game.addTagInfo(ti);
                }
                break;
            case Packet.COMMAND_RESET_TAGINFO:
                game.resetTagInfo();
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
                String sLocalPath = (String) c.getObject(2);
                String localFile = sLocalPath + File.separator + sFinalFile;
                try {
                    File sDir = new File(sLocalPath);
                    if (!sDir.exists()) {
                        sDir.mkdir();
                    }
                } catch (Exception e) {
                    System.err.println("Unable to create savegames directory");
                }
                try {

                    BufferedOutputStream fout = new BufferedOutputStream(
                            new FileOutputStream(localFile));
                    ArrayList<Integer> data = (ArrayList<Integer>) c
                            .getObject(1);
                    for (Integer d : data) {
                        fout.write(d);
                    }
                    fout.flush();
                    fout.close();
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
                game.processGameEvent(new GameBoardChangeEvent(this));
                break;
            case Packet.COMMAND_SENDING_AVAILABLE_MAP_SIZES:
                availableSizes = (Set<BoardDimensions>) c.getObject(0);
                game.processGameEvent(new GameSettingsChangeEvent(this));
                break;
            case Packet.COMMAND_ENTITY_NOVA_NETWORK_CHANGE:
                receiveEntityNovaNetworkModeChange(c);
                break;
            case Packet.COMMAND_CLIENT_FEEDBACK_REQUEST:
                int cfrType = (int) c.getData()[0];
                GameCFREvent cfrEvt = new GameCFREvent(this, cfrType);
                switch (cfrType) {
                    case (Packet.COMMAND_CFR_DOMINO_EFFECT):
                        cfrEvt.setEntityId((int) c.getData()[1]);
                        break;
                    case Packet.COMMAND_CFR_AMS_ASSIGN:
                        cfrEvt.setEntityId((int) c.getData()[1]);
                        cfrEvt.setAmsEquipNum((int) c.getData()[2]);
                        cfrEvt.setWAAs((List<WeaponAttackAction>) c.getData()[3]);
                        break;
                    case Packet.COMMAND_CFR_APDS_ASSIGN:
                        cfrEvt.setEntityId((int) c.getData()[1]);
                        cfrEvt.setApdsDists((List<Integer>) c.getData()[2]);
                        cfrEvt.setWAAs((List<WeaponAttackAction>) c.getData()[3]);
                        break;
                }
                game.processGameEvent(cfrEvt);
                break;
            case Packet.COMMAND_GAME_VICTORY_EVENT:
                GameVictoryEvent gve = new GameVictoryEvent(this, game);
                game.processGameEvent(gve);
                break;
        }
    }

    /**
     * receive and process an entity nova network mode change packet
     *
     * @param c
     */
    private void receiveEntityNovaNetworkModeChange(Packet c) {
        try {
            int entityId = c.getIntValue(0);
            String networkID = c.getObject(1).toString();
            Entity e = game.getEntity(entityId);
            if (e != null) {
                e.setNewRoundNovaNetworkString(networkID);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void sendDominoCFRResponse(MovePath mp) {
        Object data[] = { Packet.COMMAND_CFR_DOMINO_EFFECT, mp };
        Packet packet = new Packet(Packet.COMMAND_CLIENT_FEEDBACK_REQUEST, data);
        send(packet);
    }

    public void sendAMSAssignCFRResponse(Integer waaIndex) {
        Object data[] = { Packet.COMMAND_CFR_AMS_ASSIGN, waaIndex };
        Packet packet = new Packet(Packet.COMMAND_CLIENT_FEEDBACK_REQUEST, data);
        send(packet);
    }

    public void sendAPDSAssignCFRResponse(Integer waaIndex) {
        Object data[] = { Packet.COMMAND_CFR_APDS_ASSIGN, waaIndex };
        Packet packet = new Packet(Packet.COMMAND_CLIENT_FEEDBACK_REQUEST, data);
        send(packet);
    }

    /**
     * Perform a dump of the current memory usage.
     * <p/>
     * This method is useful in tracking performance issues on various player's
     * systems. You can activate it by changing the "memorydumpon" setting to
     * "true" in the clientsettings.xml file.
     *
     * @param where
     *            - a <code>String</code> indicating which part of the game is
     *            making this call.
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
            buf.append(": used (").append(used).append(") + free (").append(free).append(") = ").append(total); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

    protected void correctName(Packet inP) {
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
     * If we remove an entity, we may need to update the duplicate identifier.
     *
     * @param ids
     */
    private void checkDuplicateNamesDuringDelete(List<Integer> ids) {
        ArrayList<Entity> myEntities = game.getPlayerEntities(
                game.getPlayer(localPlayerNumber), false);
        Hashtable<String, ArrayList<Integer>> rawNameToId =
                new Hashtable<String, ArrayList<Integer>>(
                        (int) (myEntities.size() * 1.26));

        for (Entity e : myEntities) {
            String rawName = e.getShortNameRaw();
            ArrayList<Integer> namedIds = rawNameToId.get(rawName);
            if (namedIds == null) {
                namedIds = new ArrayList<Integer>();
            }
            namedIds.add(e.getId());
            rawNameToId.put(rawName, namedIds);
        }

        for (int id : ids) {
            Entity removedEntity = game.getEntity(id);
            if (removedEntity == null) {
                continue;
            }

            String removedRawName = removedEntity.getShortNameRaw();
            Integer count = duplicateNameHash.get(removedEntity
                                                          .getShortNameRaw());
            if ((count != null) && (count > 1)) {
                ArrayList<Integer> namedIds = rawNameToId.get(removedRawName);
                for (Integer i : namedIds) {
                    Entity e = game.getEntity(i);
                    String eRawName = e.getShortNameRaw();
                    if (eRawName.equals(removedRawName)
                        && (e.duplicateMarker > removedEntity.duplicateMarker)) {
                        e.duplicateMarker--;
                        e.generateShortName();
                        e.generateDisplayName();
                        // Update the Entity, unless it's going to be deleted
                        if (!ids.contains(e.getId())) {
                            sendUpdateEntity(e);
                        }
                    }
                }
                duplicateNameHash.put(removedEntity.getShortNameRaw(),
                                      new Integer(count - 1));

            } else if (count != null) {
                duplicateNameHash.remove(removedEntity.getShortNameRaw());
            }
        }
    }

    /**
     * @param cmd a client command with CLIENT_COMMAND prepended.
     */
    public String runCommand(String cmd) {
        cmd = cmd.substring(CLIENT_COMMAND.length());

        return runCommand(cmd.split("\\s+"));
    }

    /**
     * Runs the command
     *
     * @param args the command and it's arguments with the CLIENT_COMMAND already
     *             removed, and the string tokenized.
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

    public RandomSkillsGenerator getRandomSkillsGenerator() {
        return rsg;
    }

    public RandomNameGenerator getRandomNameGenerator() {
        return RandomNameGenerator.getInstance();
    }

    public Set<BoardDimensions> getAvailableMapSizes() {
        return availableSizes;
    }

    public IGame getGame() {
        return game;
    }
}
