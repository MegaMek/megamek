/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.io.*;

import com.sun.java.util.collections.HashMap;

import megamek.common.*;
import megamek.common.actions.*;

public class Client extends Panel
    implements Runnable, MouseListener, WindowListener
{
    // a frame, to show stuff in
    public Frame                frame;
    
    private boolean             standalone;
    
//    // another frame for the report
//    public Frame                reportFrame;
        
    // we need these to communicate with the server
    private String              name;
    Socket                      socket;
    private ObjectInputStream   in = null;
    private ObjectOutputStream  out = null;

    // some info about us and the server
    private boolean             connected = false;
    public int                  local_pn;
        
    // the actual game (imagine that)
    public Game                 game;
        
    // here's some game phase stuff
    private MapSettings         mapSettings;
    public String               eotr;
        
    // keep me
    public ChatterBox           cb;
    public BoardView1           bv;
    public BoardComponent       bc;
    public Dialog               mechW;
    public MechDisplay          mechD;
    public Dialog		minimapW;
    public MiniMap		minimap;
    public PopupMenu            popup;
        
    protected Component         curPanel;
    
    // some dialogs...
    private BoardSelectionDialog    boardSelectionDialog;
    private GameOptionsDialog       gameOptionsDialog;
    private MechSelectorDialog      mechSelectorDialog;
    public Thread                   mechSelectorDialogThread;
    private StartingPositionDialog  startingPositionDialog;

    // message pump listening to the server
    private Thread              pump;
        
    // I send out game events!
    private Vector              gameListeners;

    /**
     * Save and Open dialogs for MegaMek Unit List (mul) files.
     */
    private FileDialog dlgLoadList = null;
    private FileDialog dlgSaveList = null;

    /**
     * Construct a non-standalone client.  This client will try to dispose of
     * itself as much as possible when it's done playing, but will not call
     * System.exit().
     *
     * This is mostly for use in MCWizards's game finder.
     */
    public Client(String playername) {
        this(new Frame("MegaMek Client"), playername);

        Settings.load();
        
        if(Settings.windowSizeHeight != 0) {
            frame.setLocation(Settings.windowPosX, Settings.windowPosY);
            frame.setSize(Settings.windowSizeWidth, Settings.windowSizeHeight);
        } else {
            frame.setSize(800, 600);
        }
        
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        
        frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { setVisible(false);
                // feed last window position to settings
                Settings.windowPosX = frame.getLocation().x;
                Settings.windowPosY = frame.getLocation().y;
                Settings.windowSizeWidth = frame.getSize().width;
                Settings.windowSizeHeight = frame.getSize().height;

                // save settings
                Settings.save();
                
                die();
            }
	});
        
        standalone = false;
        
        frame.setVisible(true);
    }
    
    /**
     * Construct a standalone client by giving it a frame to take over.  This
     * client will call System.exit(0) when it's done.
     */
    public Client(Frame frame, String playername) {
        this.frame = frame;
        this.name = playername;

        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        unitLoadingDialog.show();

        gameListeners = new Vector();
                
        local_pn = -1;
                
        game = new Game();
        
        popup = new PopupMenu("Board Popup...");

        bv = new BoardView1(game, frame);
//        bc = new BoardComponent(bv);
        bv.addMouseListener(this);
        bv.add(popup);
        
        cb = new ChatterBox(this);
        mechW = new Dialog(frame, "Mech Display", false);
        mechW.setLocation(Settings.displayPosX, Settings.displayPosY);
        mechW.setSize(Settings.displaySizeWidth, Settings.displaySizeHeight);
        mechW.setResizable(true);
        mechW.addWindowListener(this);
        mechD = new MechDisplay(this);
        mechW.add(mechD);
        // minimap
        minimapW = new Dialog(frame, "MiniMap", false);
        minimapW.setLocation(Settings.minimapPosX, Settings.minimapPosY);
        minimapW.setSize(Settings.minimapSizeWidth, Settings.minimapSizeHeight);
        minimap = new MiniMap(minimapW, this, bv);
        minimapW.addWindowListener(this);
        minimapW.add(minimap);
        
        mechSelectorDialog = new MechSelectorDialog(this,unitLoadingDialog);
        mechSelectorDialogThread = new Thread(mechSelectorDialog);
        mechSelectorDialogThread.start();

        changePhase(Game.PHASE_UNKNOWN);
                
        // layout
        setLayout(new BorderLayout());
        frame.setTitle(playername + " - MegaMek");
        
        frame.removeAll();
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.validate();
        
        standalone = true;
        
//        // report frame
//        reportFrame = new Frame("MegaMek Reports");
//        reportFrame.setSize(420, 600);
//        //reportFrame.setVisible(true);
    }
    
    /**
     * Attempt to connect to the specified host
     */
    public boolean connect(String hostname, int port) {
        try {
            socket = new Socket(hostname, port);
        } catch(UnknownHostException ex) {
            return false;
        } catch(IOException ex) {
            return false;
        }
        
        pump = new Thread(this);
        pump.start();
    
        return true;
    }
    
    /**
     * Shuts down threads and sockets
     */
    public void die() {
        connected = false;
        pump = null;
        
        // shut down threads & sockets
        try {
            socket.close();
            in.close();
            out.close();
        } catch (java.lang.Exception ex) { ; }
        
        if (standalone) {
            System.exit(0);
        } else {
            frame.setVisible(false);
            frame.dispose();
        }
    }
    
    /**
     * The client has become disconnected from the server
     */
    protected void disconnected() {
        AlertDialog alert = new AlertDialog(frame, "Disconnected!", "You have become disconnected from the server.");
        alert.show();
        
        die();
    }
    
    /**
     * Return an enumeration of the players in the game
     */
    public Enumeration getPlayers() {
        return game.getPlayers();
    }
    
    /**
     * Return the current number of players the client knows about
     */
    public int getNoOfPlayers() {
        int count = 0;
        for(Enumeration e = getPlayers(); e.hasMoreElements();) {
            if(e.nextElement() != null) {
                count++;
            }
        }
        return count;
    }
    
    public Entity getEntity(int enum) {
        return game.getEntity(enum);
    }

    /**
     * Returns the individual player assigned the index
     * parameter.
     */
    public Player getPlayer(int idx) {
        return (Player)game.getPlayer(idx);
    }
  
    /**
     * Return the local player
     */
    public Player getLocalPlayer() {
        return getPlayer(local_pn);
    }
    
    /**
     * Returns the number of first selectable entity
     */
    public int getFirstEntityNum() {
        return game.getFirstEntityNum();
    }
    
    /**
     * Returns the number of the next selectable entity after the one given
     */
    public int getNextEntityNum(int entityId) {
        return game.getNextEntityNum(entityId);
    }
  
    /**
     * Shortcut to game.board
     */
    public Board getBoard() {
        return game.board;
    }
    
    /**
     * Returns an emumeration of the entities in game.entities
     */
    public Enumeration getEntities() {
        return game.getEntities();
    }
    
    public MapSettings getMapSettings() {
        return mapSettings;
    }
    
    /**
     * Returns the board selection dialog, creating it on the first call
     */
    public BoardSelectionDialog getBoardSelectionDialog() {
        if (boardSelectionDialog == null) {
            boardSelectionDialog = new BoardSelectionDialog(this);
        }
        return boardSelectionDialog;
    }
    
    public GameOptionsDialog getGameOptionsDialog() {
        if (gameOptionsDialog == null) {
            gameOptionsDialog = new GameOptionsDialog(this);
        }
        return gameOptionsDialog;
    }
    
    public MechSelectorDialog getMechSelectorDialog() {
    	return mechSelectorDialog;
    }
    
    public StartingPositionDialog getStartingPositionDialog() {
        if (startingPositionDialog == null) {
            startingPositionDialog = new StartingPositionDialog(this);
        }
    	return startingPositionDialog;
    }
    
//    public ButtonMenuDialog getButtonMenuDialog() {
//        if (buttonMenuDialog == null) {
//            buttonMenuDialog = new ButtonMenuDialog(frame);
//        }
//        return buttonMenuDialog;
//    }
    
    /**
     * Changes the game phase, and the displays that go
     * along with it.
     */
    protected void changePhase(int phase) {
        this.game.phase = phase;
        
        bv.hideTooltip();    //so it does not cover up anything important during a report "phase"
        
        // remove the current panel
        curPanel = null;
        this.removeAll();
        doLayout();
        
        switch(phase) {
        case Game.PHASE_LOUNGE :
            switchPanel(new ChatLounge(this));
            break;
        case Game.PHASE_EXCHANGE :
            switchPanel(new Label("Transmitting game data..."));
            sendDone(true);
            break;
        case Game.PHASE_DEPLOYMENT :
            switchPanel(new DeploymentDisplay(this));
            if (Settings.minimapEnabled && !minimapW.isVisible()) {
                setMapVisible(true);
            }
            break;
        case Game.PHASE_MOVEMENT :
            switchPanel(new MovementDisplay(this));
            if (Settings.minimapEnabled && !minimapW.isVisible()) {
                setMapVisible(true);
            }
            break;
        case Game.PHASE_FIRING :
            switchPanel(new FiringDisplay(this));
            if (Settings.minimapEnabled && !minimapW.isVisible()) {
                setMapVisible(true);
            }
            break;
        case Game.PHASE_PHYSICAL :
            game.resetActions();
            bv.refreshAttacks();
            switchPanel(new PhysicalDisplay(this));
            if (Settings.minimapEnabled && !minimapW.isVisible()) {
                setMapVisible(true);
            }
            break;
        case Game.PHASE_INITIATIVE :
            game.resetActions();
            game.resetCharges();
            bv.clearAllAttacks();
        case Game.PHASE_MOVEMENT_REPORT :
        case Game.PHASE_FIRING_REPORT :
        case Game.PHASE_END :
        case Game.PHASE_VICTORY :
            switchPanel(new ReportDisplay(this));
            setMapVisible(false);
            break;
        }
        this.validate();
        this.doLayout();
        this.cb.moveToEnd();
        processGameEvent(new GameEvent(this, GameEvent.GAME_PHASE_CHANGE, null, ""));
    }
    
    private void switchPanel(Component panel) {
        // TODO: reuse existing panels.
        curPanel = panel;
        this.add(curPanel);
        curPanel.requestFocus();
    }
    
    protected void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }
    
    protected void showBoardPopup(Point point) {
        fillPopup(bv.getCoordsAt(point));
        
        if (popup.getItemCount() > 0) {
            popup.show(bv, point.x, point.y);
        }
    }
    
    private boolean canTargetEntities() {
        return isMyTurn() && (curPanel instanceof FiringDisplay 
                              || curPanel instanceof PhysicalDisplay);
    }
    
    private boolean canSelectEntities() {
        return isMyTurn() && (curPanel instanceof FiringDisplay 
                              || curPanel instanceof PhysicalDisplay
                              || curPanel instanceof MovementDisplay);
    }
    
    /** Toggles the entity display window
     */
    public void toggleDisplay() {
        mechW.setVisible(!mechW.isVisible());
    }
    
    /** Sets the visibility of the entity display window
     */
    public void setDisplayVisible(boolean visible) {
        mechW.setVisible(visible);
    }
    
    /** Toggles the minimap window
         Also, toggles the minimap enabled setting
     */
    public void toggleMap() {
        if (minimapW.isVisible()) {
            Settings.minimapEnabled = false;
        } else {
            Settings.minimapEnabled = true;
        }
        minimapW.setVisible(!minimapW.isVisible());
    }
    
    /** Sets the visibility of the minimap window
     */
    public void setMapVisible(boolean visible) {
        minimapW.setVisible(visible);
    }
    
    protected void fillPopup(Coords coords) {
        popup.removeAll();
        
        // add select options
        if (canSelectEntities()) {
            for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = (Entity)i.nextElement();
                if (game.getTurn().isValidEntity(entity)) {
                    popup.add(new SelectMenuItem(entity));
                }
            }
        }
        
        if (popup.getItemCount() > 0) {
            popup.addSeparator();
        }
        
        // add view options
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            popup.add(new ViewMenuItem(entity));
        }
        
        // add target options
        if (canTargetEntities()) {
            if (popup.getItemCount() > 0) {
                popup.addSeparator();
            }
            for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = (Entity)i.nextElement();
                popup.add(new TargetMenuItem(entity));
            }
            // Can target weapons at the hex if it contains woods or building.
            // Can target physical attacks at the hex if it contains building.
            if ( curPanel instanceof FiringDisplay ||
                 curPanel instanceof PhysicalDisplay ) {
                Hex h = game.board.getHex(coords);
                if (h != null && h.contains(Terrain.WOODS) &&
                    curPanel instanceof FiringDisplay ) {
                    popup.add(new TargetMenuItem(new HexTarget
                        (coords, game.board, false) ) );
                    if (game.getOptions().booleanOption("fire")) {
                        popup.add(new TargetMenuItem(new HexTarget
                            (coords, game.board, true) ) );
                    }
                }
                else if ( h != null && h.contains( Terrain.BUILDING ) ) {
                    popup.add( new TargetMenuItem( new BuildingTarget
                        ( coords, game.board, false ) ) );
                    if (game.getOptions().booleanOption("fire")) {
                        popup.add( new TargetMenuItem( new BuildingTarget
                            ( coords, game.board, true ) ) );
                    }
                }
            }
        }
    }
    
    
    /**
     * Adds the specified game listener to receive 
     * board events from this board.
     * 
     * @param l            the game listener.
     */
    public void addGameListener(GameListener l) {
        gameListeners.addElement(l);
    }
    
    /**
     * Removes the specified game listener.
     * 
     * @param l            the game listener.
     */
    public void removeGameListener(GameListener l) {
        gameListeners.removeElement(l);
    }
    
    /**
     * Processes game events occurring on this 
     * connection by dispatching them to any registered 
     * GameListener objects. 
     * 
     * @param be        the board event.
     */
    protected void processGameEvent(GameEvent ge) {
        for(Enumeration e = gameListeners.elements(); e.hasMoreElements();) {
            GameListener l = (GameListener)e.nextElement();
            switch(ge.type) {
            case GameEvent.GAME_PLAYER_CHAT :
                l.gamePlayerChat(ge);
                break;
            case GameEvent.GAME_PLAYER_STATUSCHANGE :
                l.gamePlayerStatusChange(ge);
                break;
            case GameEvent.GAME_PHASE_CHANGE :
                l.gamePhaseChange(ge);
                break;
            case GameEvent.GAME_TURN_CHANGE :
                l.gameTurnChange(ge);
                break;
            case GameEvent.GAME_NEW_ENTITIES :
                l.gameNewEntities(ge);
                break;
            case GameEvent.GAME_NEW_SETTINGS :
                l.gameNewSettings(ge);
                break;
            }
        }
    }
    
    /**
     * 
     */
    public void retrieveServerInfo() {
        int retry = 50;
        while(retry-- > 0 && !connected) {
            synchronized(this) {
                try {
                    wait(100);
                } catch(InterruptedException ex) {
                    ;
                }
            }
        }
    }
    
    /**
     * is it my turn?
     */
    public boolean isMyTurn() {
        return game.getTurn() != null && game.getTurn().getPlayerNum() == local_pn;
    }
    
    /** 
     * Change whose turn it is.
     */
    protected void changeTurnIndex(int index) {
        game.setTurnIndex(index);
        Player player = getPlayer(game.getTurn().getPlayerNum());
        processGameEvent(new GameEvent(this, GameEvent.GAME_TURN_CHANGE, player, ""));
    }
    
    /**
     * Pops up a dialog box showing an alert
     */
    public void doAlertDialog(String title, String message) {
        AlertDialog alert = new AlertDialog(frame, title, message);
        alert.show();
    }
    
    /**
     * Pops up a dialog box asking a yes/no question
     * @returns true if yes
     */
    public boolean doYesNoDialog(String title, String question) {
	ConfirmDialog confirm = new ConfirmDialog(frame,title,question);
        confirm.show();
        return confirm.getAnswer();
    };

    /**
     * Send mode-change data to the server
     */
    public void sendModeChange(int nEntity, int nEquip, int nMode)
    {
        Object[] data = { new Integer(nEntity), new Integer(nEquip), 
                          new Integer(nMode) };
        send(new Packet(Packet.COMMAND_ENTITY_MODECHANGE, data));
    }

    /**
     * Send mode-change data to the server
     */
    public void sendAmmoChange(int nEntity, int nWeapon, int nAmmo)
    {
        Object[] data = { new Integer(nEntity), new Integer(nWeapon), 
                          new Integer(nAmmo) };
        send(new Packet(Packet.COMMAND_ENTITY_AMMOCHANGE, data));
    }

    /**
     * Send movement data for the given entity to the server.
     */
    public void moveEntity(int enum, MovementData md) {
        Object[] data = new Object[2];
    
        data[0] = new Integer(enum);
        data[1] = md;
    
        send(new Packet(Packet.COMMAND_ENTITY_MOVE, data));
    }

    /**
     * Maintain backwards compatability.
     *
     * @param   enum - the <code>int</code> ID of the deployed entity
     * @param   c - the <code>Coords</code> where the entity should be deployed
     * @param   nFacing - the <code>int</code> direction the entity should face
     */
    public void deploy( int enum, Coords c, int nFacing ) {
        this.deploy( enum, c, nFacing, new Vector() );
    }

    /**
     * Deploy an entity at the given coordinates, with the given facing,
     * and starting with the given units already loaded.
     *
     * @param   enum - the <code>int</code> ID of the deployed entity
     * @param   c - the <code>Coords</code> where the entity should be deployed
     * @param   nFacing - the <code>int</code> direction the entity should face
     * @param   loadedUnits - a <code>List</code> of units that start the game
     *          being transported byt the deployed entity.
     */
    public void deploy( int enum, Coords c, int nFacing,
                        Vector loadedUnits ) {
        int packetCount = 4 + loadedUnits.size();
        int index = 0;
        Object[] data = new Object[packetCount];
        data[index++] = new Integer(enum);
        data[index++] = c;
        data[index++] = new Integer(nFacing);
        data[index++] = new Integer( loadedUnits.size() );

        Enumeration iter = loadedUnits.elements();
        while ( iter.hasMoreElements() ) {
            data[index++] = new Integer( ((Entity) iter.nextElement()).getId() );
        }
        
        send(new Packet(Packet.COMMAND_ENTITY_DEPLOY, data));
    }
    
    /**
     * Send a weapon fire command to the server.
     */
    public void sendAttackData(int aen, Vector attacks) {
        Object[] data = new Object[2];
                
        data[0] = new Integer(aen);
        data[1] = attacks;
                
        send(new Packet(Packet.COMMAND_ENTITY_ATTACK, data));
                
        /* DEBUG:
        System.out.println("client: sent fire:");
        for (Enumeration i = fire.elements(); i.hasMoreElements();) {
          FiringData fd = (FiringData)i.nextElement();
          System.out.println(fd);
        }
        */
    }
    
    /**
     * Send the game options to the server
     */
    public void sendGameOptions(String password, Vector options) {
        final Object[] data = new Object[2];
        data[0] = password;
        data[1] = options;
        send(new Packet(Packet.COMMAND_SENDING_GAME_SETTINGS, data));
    }
    
    /**
     * Send the game settings to the server
     */
    public void sendMapSettings(MapSettings mapSettings) {
        send(new Packet(Packet.COMMAND_SENDING_MAP_SETTINGS, mapSettings));
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
    }
    
    /**
     * Sends a "player done" message to the server.
     */
    public void sendDone(boolean done) {
        send(new Packet(Packet.COMMAND_PLAYER_READY, new Boolean(done)));
    }
    
    /**
     * Sends the info associated with the local player.
     */
    public void sendPlayerInfo() {
        send(new Packet(Packet.COMMAND_PLAYER_UPDATE, game.getPlayer(local_pn)));
    }
  
    /**
     * Sends an "add entity" packet
     */
    public void sendAddEntity(Entity entity) {
        send(new Packet(Packet.COMMAND_ENTITY_ADD, entity));
    }
      
    /**
     * Sends an "update entity" packet
     */
    public void sendUpdateEntity(Entity entity) {
        send(new Packet(Packet.COMMAND_ENTITY_UPDATE, entity));
    }
      
    /**
     * Sends a "delete entity" packet
     */
    public void sendDeleteEntity(int enum) {
        send(new Packet(Packet.COMMAND_ENTITY_REMOVE, new Integer(enum)));
    }

    
    /**
     * Receives player information from the message packet.
     */
    protected void receivePlayerInfo(Packet c) {
        int pindex = c.getIntValue(0);
        Player newPlayer = (Player)c.getObject(1);
        if (getPlayer(newPlayer.getId()) == null) {
            game.addPlayer(pindex, newPlayer);
        } else {
            game.setPlayer(pindex, newPlayer);
        }
        processGameEvent(new GameEvent(this, GameEvent.GAME_PLAYER_STATUSCHANGE, newPlayer, ""));
    }
    
    /**
     * Loads the turn list from the data in the packet
     */
    protected void receiveTurns(Packet packet) {
        game.setTurnVector((Vector)packet.getObject(0));
    }

    /**
     * Loads the board from the data in the net command.
     */
    protected void receiveBoard(Packet c) {
        Board newBoard = (Board)c.getObject(0);
        game.board.newData( newBoard );
    }
    
    /**
     * Loads the entities from the data in the net command.
     */
    protected void receiveEntities(Packet c) {
        Vector newEntities = (Vector)c.getObject(0);
        Vector newOutOfGame = (Vector)c.getObject(1);

        // Replace the entities in the game.
        game.setEntitiesVector(newEntities);
        if ( null != newOutOfGame ) {
            game.setOutOfGameEntitiesVector(newOutOfGame);
        }

        processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_ENTITIES, null, null));
        //XXX Hack alert!
        bv.boardNewEntities(new BoardEvent(game.board, null, null, 0, 0)); //XXX
        //XXX
    }
    
    /**
     * Loads entity update data from the data in the net command.
     */
    protected void receiveEntityUpdate(Packet c) {
        int eindex = c.getIntValue(0);
        Entity entity = (Entity)c.getObject(1);
        Coords oc = entity.getPosition();
        if (game.getEntity(eindex) != null) {
        	oc = game.getEntity(eindex).getPosition();
        }
        // Replace this entity in the game.
        game.setEntity(eindex, entity);
        //XXX Hack alert!
        bv.boardChangedEntity(new BoardEvent(game.board, oc, entity, 0, 0)); //XXX
        //XXX
    }
    
    protected void receiveEntityAdd(Packet packet) {
        int entityId = packet.getIntValue(0);
        Entity entity = (Entity)packet.getObject(1);

        // Add the entity to the game.
        game.addEntity(entityId, entity);
        
        processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_ENTITIES, null, null));
        //XXX Hack alert!
        bv.boardNewEntities(new BoardEvent(game.board, null, null, 0, 0)); //XXX
        //XXX
    }
    
    protected void receiveEntityRemove(Packet packet) {
        int entityId = packet.getIntValue(0);
        int condition = packet.getIntValue(1);

        // Move the unit to its final resting place.
        game.removeEntity(entityId, condition);

        processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_ENTITIES, null, null));
        //XXX Hack alert!
        bv.boardNewEntities(new BoardEvent(game.board, null, null, 0, 0)); //XXX
        //XXX
    }

    protected void receiveBuildingUpdateCF(Packet packet) {
        Vector bldgs = (Vector) packet.getObject(0);

        // Update the board.  The board will notify listeners.
        game.board.updateBuildingCF( bldgs );
    }

    protected void receiveBuildingCollapse(Packet packet) {
        Vector bldgs = (Vector) packet.getObject(0);

        // Update the board.  The board will notify listeners.
        game.board.collapseBuilding( bldgs );
    }

    /**
     * Loads entity firing data from the data in the net command
     */
    protected void receiveAttack(Packet c) {
        Vector vector = (Vector)c.getObject(0);
        boolean charge = c.getBooleanValue(1);
        for (Enumeration i = vector.elements(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            int entityId = ea.getEntityId();
            if (ea instanceof TorsoTwistAction && game.hasEntity(entityId)) {
                TorsoTwistAction tta = (TorsoTwistAction)ea;
                Entity entity = game.getEntity(entityId);
                entity.setSecondaryFacing(tta.getFacing());
                //XXX Hack alert!
                bv.boardChangedEntity(new BoardEvent(game.board, entity.getPosition(), entity, 0, 0)); //XXX
                //XXX
            } else if (ea instanceof FlipArmsAction && game.hasEntity(entityId)) {
                FlipArmsAction faa = (FlipArmsAction)ea;
                Entity entity = game.getEntity(entityId);
                entity.setArmsFlipped(faa.getIsFlipped());
                //XXX Hack alert!
                bv.boardChangedEntity(new BoardEvent(game.board, entity.getPosition(), entity, 0, 0)); //XXX
                //XXX
            } else if (ea instanceof AttackAction) {
                if ( ea instanceof ClubAttackAction ) {
                    ClubAttackAction clubAct = (ClubAttackAction) ea;
                    Entity entity = game.getEntity( clubAct.getEntityId() );
                    clubAct.setClub( Compute.clubMechHas(entity) );
                }
                bv.addAttack((AttackAction)ea);
            }
            // track in the appropriate list
            if (charge) {
                game.addCharge((AttackAction)ea);
            } else {
                game.addAction(ea);
            }
        }
    }
    
    /**
     * Saves server entity status data to a local file
     */
    private void saveEntityStatus(String sStatus)
    {
        try {
            FileWriter fw = new FileWriter("entitystatus.txt");
            fw.write(sStatus);
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Reads a complete net command from the given input stream
     */
    private Packet readPacket() {
        try {
            if (in == null) {
                in = new ObjectInputStream(socket.getInputStream());
            }
            Packet packet = (Packet)in.readObject();
//            System.out.println("c: received command #" + packet.getCommand() + " with " + packet.getData().length + " data");
            return packet;
        } catch (IOException ex) {
            System.err.println("client: IO error reading command:");
            System.err.println(ex);
            System.err.println(ex.getMessage());
            disconnected();
            return null;
        } catch (ClassNotFoundException ex) {
            System.err.println("client: class not found error reading command:");
            System.err.println(ex);
            System.err.println(ex.getMessage());
            disconnected();
            return null;
        }
    }
    
    /**
     * send the message to the server
     */
    protected void send(Packet packet) {
        packet.zipData();
        try {
            if (out == null) {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
            }
            out.reset(); // write each packet fresh; a lot changes
            out.writeObject(packet);
            out.flush();
//            System.out.println("c: packet #" + packet.getCommand() + " sent");
        } catch(IOException ex) {
            System.err.println("client: error sending command.");
        }
    }

    //
    // Runnable
    //
    public void run() {
        Thread currentThread = Thread.currentThread();
        Packet c;
        while(pump == currentThread) {
            c = readPacket();
            if (c == null) {
                System.out.println("client: got null packet");
                continue;
            }
            // obey command
            switch(c.getCommand()) {
            case Packet.COMMAND_SERVER_GREETING :
                connected = true;
                send(new Packet(Packet.COMMAND_CLIENT_NAME, name));
                break;
            case Packet.COMMAND_LOCAL_PN :
                this.local_pn = c.getIntValue(0);
                break;
            case Packet.COMMAND_PLAYER_UPDATE :
                receivePlayerInfo(c);
                break;
            case Packet.COMMAND_PLAYER_READY :
                getPlayer(c.getIntValue(0)).setDone(c.getBooleanValue(1));
                processGameEvent(new GameEvent(this, GameEvent.GAME_PLAYER_STATUSCHANGE, getPlayer(c.getIntValue(0)), ""));
                break;
            case Packet.COMMAND_PLAYER_ADD :
                receivePlayerInfo(c);
                break;
            case Packet.COMMAND_PLAYER_REMOVE :
                game.removePlayer(c.getIntValue(0));
                processGameEvent(new GameEvent(this, GameEvent.GAME_PLAYER_STATUSCHANGE, getPlayer(c.getIntValue(0)), ""));
                break;
            case Packet.COMMAND_CHAT :
                processGameEvent(new GameEvent(this, GameEvent.GAME_PLAYER_CHAT, null, (String)c.getObject(0)));
                break;
            case Packet.COMMAND_ENTITY_ADD :
                receiveEntityAdd(c);
                break;
            case Packet.COMMAND_ENTITY_UPDATE :
                receiveEntityUpdate(c);
                break;
            case Packet.COMMAND_ENTITY_REMOVE :
                receiveEntityRemove(c);
                break;
            case Packet.COMMAND_CHANGE_HEX :
                game.board.setHex((Coords)c.getObject(0), (Hex)c.getObject(1));
                break;
            case Packet.COMMAND_BLDG_UPDATE_CF :
                receiveBuildingUpdateCF( c );
                break;
            case Packet.COMMAND_BLDG_COLLAPSE :
                receiveBuildingCollapse( c );
                break;
            case Packet.COMMAND_PHASE_CHANGE :
                changePhase(c.getIntValue(0));
                break;
            case Packet.COMMAND_TURN :
                changeTurnIndex(c.getIntValue(0));
                break;
            case Packet.COMMAND_SENDING_TURNS :
                receiveTurns(c);
                break;
            case Packet.COMMAND_SENDING_BOARD :
                receiveBoard(c);
                break;
            case Packet.COMMAND_SENDING_ENTITIES :
                receiveEntities(c);
                break;
            case Packet.COMMAND_SENDING_REPORT :
                eotr = (String)c.getObject(0);
                if (curPanel instanceof ReportDisplay) {
                    ((ReportDisplay)curPanel).refresh();
                }
                break;
            case Packet.COMMAND_ENTITY_ATTACK :
                receiveAttack(c);
                break;
            case Packet.COMMAND_SENDING_GAME_SETTINGS :
                game.setOptions((GameOptions)c.getObject(0));
                if (gameOptionsDialog != null && gameOptionsDialog.isVisible()) {
                    gameOptionsDialog.update(game.getOptions());
                }
                processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_SETTINGS, null, null));
                break;
            case Packet.COMMAND_SENDING_MAP_SETTINGS :
                mapSettings = (MapSettings)c.getObject(0);
                if (boardSelectionDialog != null && boardSelectionDialog.isVisible()) {
                    boardSelectionDialog.update((MapSettings)c.getObject(0), true);
                }
                processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_SETTINGS, null, null));
                break;
            case Packet.COMMAND_QUERY_MAP_SETTINGS :
                if (boardSelectionDialog != null && boardSelectionDialog.isVisible()) {
                    boardSelectionDialog.update((MapSettings)c.getObject(0), false);
                }
                break;
            case Packet.COMMAND_END_OF_GAME :
                String sReport = (String)c.getObject(0);
                game.setVictoryPlayerId(c.getIntValue(1));
                game.setVictoryTeam(c.getIntValue(2));
                // save victory report
                saveEntityStatus(sReport);

                // Make a list of the player's living units.
                Vector living = game.getPlayerEntities( getLocalPlayer() );

                // Be sure to include all units that have retreated.
                for ( Enumeration iter = game.getRetreatedEntities();
                      iter.hasMoreElements(); ) {
                    living.addElement( iter.nextElement() );
                }

                // Allow players to save their living units to a file.
                // Don't bother asking if none survived.
                if ( !living.isEmpty() &&
                     doYesNoDialog( "Save Units?",
                                    "Do you want to save all surviving units\n"
                                    + "(including retreated units) to a file?")
                     ) {

                    // Allow the player to save the units to a file.
                    saveListFile( living );

                } // End user-wants-a-MUL
                break;
            }
        }
    }
    
    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            showBoardPopup(mouseEvent.getPoint());
        }
    }
    
    public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            showBoardPopup(mouseEvent.getPoint());
        }
    }
    

    /**
     * Allow the player to select a MegaMek Unit List file to load.  The
     * <code>Entity</code>s in the file will replace any that the player
     * has already selected.  As such, this method should only be called
     * in the chat lounge.  The file can record damage sustained, non-
     * standard munitions selected, and ammunition expended in a prior
     * engagement.
     */
    protected void loadListFile() {

        // Build the "load unit" dialog, if necessary.
        if ( null == dlgLoadList ) {
            dlgLoadList = new FileDialog( frame,
                                          "Open Unit List File",
                                          FileDialog.LOAD );

            // Add a filter for MUL files
            dlgLoadList.setFilenameFilter( new FilenameFilter() {
                    public boolean accept( File dir, String name ) {
                        return ( null != name && name.endsWith( ".mul" ) );
                    }
                } );

            // use base directory by default
            dlgLoadList.setDirectory(".");

            // Default to the player's name.
            dlgLoadList.setFile( getLocalPlayer().getName() + ".mul" );
        }

        // Display the "load unit" dialog.
        dlgLoadList.show();

        // Did the player select a file?
        String unitPath = dlgLoadList.getDirectory();
        String unitFile = dlgLoadList.getFile();
        if ( null != unitFile ) {
            try {
                // Read the units from the file.
                Vector loadedUnits = EntityListFile.loadFrom( unitPath, unitFile );

                // Clear the player's current units.
                Vector currentUnits =
                    game.getPlayerEntities( getLocalPlayer() );
                for ( Enumeration iter = currentUnits.elements();
                      iter.hasMoreElements(); ) {
                    final Entity entity = (Entity) iter.nextElement();
                    sendDeleteEntity( entity.getId() );
                }

                // Add the units from the file.
                for ( Enumeration iter = loadedUnits.elements();
                      iter.hasMoreElements(); ) {
                    final Entity entity = (Entity) iter.nextElement();
                    entity.setOwner( getLocalPlayer() );
                    sendAddEntity( entity );
                }
            } catch ( IOException excep ) {
                excep.printStackTrace( System.err );
                doAlertDialog( "Error Loading File", excep.getMessage() );
            }
        }
    }

    /**
     * Allow the player to save a list of entities to a MegaMek Unit List
     * file.  A "Save As" dialog will be displayed that allows the user to
     * select the file's name and directory. The player can later load this
     * file to quickly select the units for a new game.  The file will record
     * damage sustained, non-standard munitions selected, and ammunition
     * expended during the course of the current engagement.
     *
     * @param   unitList - the <code>Vector</code> of <code>Entity</code>s
     *          to be saved to a file.  If this value is <code>null</code>
     *          or empty, the "Save As" dialog will not be displayed.
     */
    protected void saveListFile( Vector unitList ) {

        // Handle empty lists.
        if ( null == unitList || unitList.isEmpty() ) {
            return;
        }

        // Build the "save unit" dialog, if necessary.
        if ( null == dlgSaveList ) {
            dlgSaveList = new FileDialog( frame,
                                          "Save Unit List As",
                                          FileDialog.SAVE );

            // Add a filter for MUL files
            dlgSaveList.setFilenameFilter( new FilenameFilter() {
                    public boolean accept( File dir, String name ) {
                        return ( null != name && name.endsWith( ".mul" ) );
                    }
                } );

            // use base directory by default
            dlgSaveList.setDirectory(".");
                
            // Default to the player's name.
            dlgSaveList.setFile( getLocalPlayer().getName() + ".mul" );
        }

        // Display the "save unit" dialog.
        dlgSaveList.show();

        // Did the player select a file?
        String unitPath = dlgSaveList.getDirectory();
        String unitFile = dlgSaveList.getFile();
        if ( null != unitFile ) {
            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo( unitPath, unitFile, unitList );
            } catch ( IOException excep ) {
                excep.printStackTrace( System.err );
                doAlertDialog( "Error Saving File", excep.getMessage() );
            }
        }
    }
    
    //
    // WindowListener
    //
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        if (windowEvent.getWindow() == minimapW) {
            setMapVisible(false);
        }
        else if (windowEvent.getWindow() == mechW) {
            setDisplayVisible(false);
        }
    }    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }
    
    /**
     * A menu item that lives to view an entity.
     */
    private class ViewMenuItem extends MenuItem implements ActionListener {
        Entity entity;
        
        public ViewMenuItem(Entity entity) {
            super("View " + entity.getDisplayName());
            this.entity = entity;
            addActionListener(this);
        }
        
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            setDisplayVisible(true);
            mechD.displayEntity(entity);
        }        
    }
    
    /**
     * A menu item that would really like to select an entity.  You can use
     * this during movement, firing & physical phases.  (Deployment would
     * just be silly.)
     */
    private class SelectMenuItem extends MenuItem implements ActionListener {
        Entity entity;
        
        public SelectMenuItem(Entity entity) {
            super("Select " + entity.getDisplayName());
            this.entity = entity;
            addActionListener(this);
        }
        
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            if (curPanel instanceof MovementDisplay) {
                ((MovementDisplay)curPanel).selectEntity(entity.getId());
            } else if (curPanel instanceof FiringDisplay) {
                ((FiringDisplay)curPanel).selectEntity(entity.getId());
            } else if (curPanel instanceof PhysicalDisplay) {
                ((PhysicalDisplay)curPanel).selectEntity(entity.getId());
            }
        }        
    }
    
    /**
     * A menu item that will target an entity, provided that it's sensible to
     * do so
     */
    private class TargetMenuItem extends MenuItem implements ActionListener {
        Targetable target;
        
        public TargetMenuItem(Targetable t) {
            super("Target " + t.getDisplayName());
            target = t;
            addActionListener(this);
        }
        
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            if (curPanel instanceof FiringDisplay) {
                ((FiringDisplay)curPanel).target(target);
            } else if (curPanel instanceof PhysicalDisplay) {
                ((PhysicalDisplay)curPanel).target(target);
            }
        }        
    }
    
}
