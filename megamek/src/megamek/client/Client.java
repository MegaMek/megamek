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
    implements Runnable
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
        
    protected Panel             curPanel;
    
    // some dialogs...
    private BoardSelectionDialog    boardSelectionDialog;
    private GameOptionsDialog       gameOptionsDialog;
    private MechSelectorDialog 		mechSelectorDialog;

    // message pump listening to the server
    private Thread              pump;
        
    // I send out game events!
    private Vector              gameListeners;
    
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

        gameListeners = new Vector();
                
        local_pn = -1;
                
        game = new Game();

        bv = new BoardView1(game, frame);
//        bc = new BoardComponent(bv);
        cb = new ChatterBox(this);
        mechW = new Dialog(frame, "Mech Display", false);
        mechW.setSize(210, 340);
        mechW.setResizable(true);
        mechD = new MechDisplay();
        mechW.add(mechD);
        if (Settings.minimapEnabled) {
            minimapW = new Dialog(frame, "MiniMap", false);
            minimapW.setLocation(Settings.minimapPosX, Settings.minimapPosY);
            minimapW.setSize(Settings.minimapSizeWidth, Settings.minimapSizeHeight);
            minimap = new MiniMap(minimapW, this, bv);
            minimapW.add(minimap);
        }
        
        // load at init time because it's heavy
        mechSelectorDialog = new MechSelectorDialog(this, 
        		new File(Settings.mechDirectory));
        mechSelectorDialog.loadMechs();
            
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
        } catch (IOException ex) { ; }
        
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
        
        // remove the current panel
        curPanel = null;
        this.removeAll();
        doLayout();
        
        switch(phase) {
        case Game.PHASE_LOUNGE :
            curPanel = new ChatLounge(this);
            this.add(curPanel);
            curPanel.requestFocus();
            break;
        case Game.PHASE_EXCHANGE :
            sendReady(true);
            break;
        case Game.PHASE_MOVEMENT :
            curPanel = new MovementDisplay(this);
            this.add(curPanel);
            curPanel.requestFocus();
            break;
        case Game.PHASE_FIRING :
            curPanel = new FiringDisplay(this);
            this.add(curPanel);
            curPanel.requestFocus();
            break;
        case Game.PHASE_PHYSICAL :
            curPanel = new PhysicalDisplay(this);
            this.add(curPanel);
            curPanel.requestFocus();
            break;
        case Game.PHASE_INITIATIVE :
        case Game.PHASE_MOVEMENT_REPORT :
        case Game.PHASE_FIRING_REPORT :
        case Game.PHASE_END :
            bv.hideTooltip();    //so it does not cover up anything important during a report "phase"
            curPanel = new ReportDisplay(this);
            this.add(curPanel);
            curPanel.requestFocus();
            break;
        }
        this.validate();
        this.doLayout();
        this.cb.moveToEnd();
        processGameEvent(new GameEvent(this, GameEvent.GAME_PHASE_CHANGE, null, ""));
    }
    
    
    protected void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
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
        return game.getTurn() == local_pn;
    }
    
    /** 
     * Change whose turn it is.
     */
    protected void changeTurn(int turn) {
        this.game.setTurn(turn);
        processGameEvent(new GameEvent(this, GameEvent.GAME_TURN_CHANGE, getPlayer(turn), ""));
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
     * Send movement data for the given entity to the server.
     */
    public void moveEntity(int enum, MovementData md) {
        Object[] data = new Object[2];
    
        data[0] = new Integer(enum);
        data[1] = md;
    
        send(new Packet(Packet.COMMAND_ENTITY_MOVE, data));
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
     * Sends a "player ready" message to the server.
     */
    public void sendReady(boolean ready) {
        send(new Packet(Packet.COMMAND_PLAYER_READY, new Boolean(ready)));
    }
    
    /**
     * Sends an "entity ready" message to the server.
     */
    public void sendEntityReady(int enum) {
        game.getEntity(enum).ready = false;
        send(new Packet(Packet.COMMAND_ENTITY_READY, new Integer(enum)));
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
     * Loads the board from the data in the net command.
     */
    protected void receiveBoard(Packet c) {
        Board newBoard = (Board)c.getObject(0);
        game.board.newData(newBoard.width, newBoard.height, newBoard.data);
    }
    
    /**
     * Loads the entities from the data in the net command.
     */
    protected void receiveEntities(Packet c) {
        Vector newEntities = (Vector)c.getObject(0);
        // re-link player in each entity
        for (Enumeration i = newEntities.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            entity.restore();
            entity.setOwner(getPlayer(entity.getOwnerId()));
        }
    
        game.setEntitiesVector(newEntities);
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
        // re-link player
        entity.restore();
        entity.setOwner(getPlayer(entity.getOwnerId()));
        
        game.setEntity(eindex, entity);
        //XXX Hack alert!
        bv.boardChangedEntity(new BoardEvent(game.board, oc, entity, 0, 0)); //XXX
        //XXX
    }
    
    protected void receiveEntityAdd(Packet packet) {
        int entityId = packet.getIntValue(0);
        Entity entity = (Entity)packet.getObject(1);
        // re-link player
        entity.restore();
        entity.setOwner(getPlayer(entity.getOwnerId()));
        
        game.addEntity(entityId, entity);
        
        processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_ENTITIES, null, null));
        //XXX Hack alert!
        bv.boardNewEntities(new BoardEvent(game.board, null, null, 0, 0)); //XXX
        //XXX
    }
    
    protected void receiveEntityRemove(Packet packet) {
        int entityId = packet.getIntValue(0);
        game.removeEntity(entityId);
        processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_ENTITIES, null, null));
        //XXX Hack alert!
        bv.boardNewEntities(new BoardEvent(game.board, null, null, 0, 0)); //XXX
        //XXX
    }
    
    /**
     * Loads entity firing data from the data in the net command
     */
    protected void receiveAttack(Packet c) {
        Object o = c.getObject(0);
        if (o instanceof TorsoTwistAction) {
            TorsoTwistAction tta = (TorsoTwistAction)o;
            if (game.getEntity(tta.getEntityId()) != null) {
            game.getEntity(tta.getEntityId()).setSecondaryFacing(tta.getFacing());
            }
        } else if (o instanceof FlipArmsAction) {
            FlipArmsAction faa = (FlipArmsAction)o;
            if (game.getEntity(faa.getEntityId()) != null) {
            game.getEntity(faa.getEntityId()).setArmsFlipped(faa.getIsFlipped());
            }
        } else if (o instanceof AttackAction) {
            bv.addAttack((AttackAction)o);
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
                getPlayer(c.getIntValue(0)).setReady(c.getBooleanValue(1));
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
            case Packet.COMMAND_PHASE_CHANGE :
                changePhase(c.getIntValue(0));
                break;
            case Packet.COMMAND_TURN :
                changeTurn(c.getIntValue(0));
                break;
            case Packet.COMMAND_SENDING_BOARD :
                receiveBoard(c);
                break;
            case Packet.COMMAND_SENDING_ENTITIES :
                receiveEntities(c);
                break;
            case Packet.COMMAND_SENDING_REPORT :
                eotr = (String)c.getObject(0);
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
            }
        }
    }
}
