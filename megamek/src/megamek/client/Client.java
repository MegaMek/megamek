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

package megamek.client;

import java.awt.*;
import java.net.*;
import java.util.*;
import java.io.*;

import megamek.common.*;
import megamek.common.actions.*;

public class Client extends Panel
	implements Runnable
{
    // a frame, to show stuff in
    public Frame				frame;
    
    // another frame for the report
    public Frame                reportFrame;
    	
    // we need these to communicate with the server
    private String              name;
    Socket                      socket;

    // some info about us and the server
    public String				serverName;
    public int					max_players;
    public int					local_pn;
    	
    // the actual game (imagine that)
    public Game                 game;
    	
    // here's some game phase stuff
    public GameSettings gameSettings;
    public String				eotr;
    	
    // keep me
    public ChatterBox			cb;
    public BoardView1			bv;
    public Window               mechW;
    public MechDisplay          mechD;
    	
    private Panel				curPanel;

    	
    // message pump listening to the server
    private Thread				pump;
    	
    // I send out game events!
    private Vector				gameListeners;
	
	/**
	 * Constructor
	 */
	public Client(Frame frame, String playername) {
		this.frame = frame;
        this.name = playername;

        gameListeners = new Vector();
        		
        serverName = null;
        max_players = -1;
        local_pn = -1;
        		
        game = new Game();

        bv = new BoardView1(game, frame);
        cb = new ChatterBox(this);
        mechW = new Dialog(frame, "Mech Display", false);
        mechW.setSize(210, 320);
        mechD = new MechDisplay();
        mechW.add(mechD);
            
        gameSettings = new GameSettings();

        changePhase(Game.PHASE_UNKNOWN);
        		
        // layout
        setLayout(new BorderLayout());
        
        // report frame
        reportFrame = new Frame("MegaMek Reports");
        reportFrame.setSize(400, 600);
        //reportFrame.setVisible(true);
	}
	
	/**
	 * Attempt to connect to the specified host
	 */
	public boolean connect(String hostname, int port) {
		try {
			socket = new Socket(hostname, port);
		} catch(UnknownHostException ex) {
			socket = null;
			return false;
		} catch(IOException ex) {
			socket = null;
			return false;
		}
		
		pump = new Thread(this);
		pump.start();
    
		return true;
	}
    
    /**
     * The client has become disconnected from the server
     */
    private void disconnected() {
        AlertDialog alert = new AlertDialog(frame, "Disconnected!", "You have become disconnected from the server.");
        
        alert.show();
        
        System.exit(0);
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
	 * Return the maximum number of players in the client.
	 */
	public int getMaxPlayers() {
		return max_players;
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
	
	/**
	 * Changes the game phase, and the displays that go
	 * along with it.
	 */
	private void changePhase(int phase) {
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
	
	
	private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
		gridbag.setConstraints(comp, c);
		add(comp);
	}
	
	
	/**
	 * Adds the specified game listener to receive 
	 * board events from this board.
	 * 
	 * @param l			the game listener.
	 */
	public void addGameListener(GameListener l) {
		gameListeners.addElement(l);
	}
	
	/**
	 * Removes the specified game listener.
	 * 
	 * @param l			the game listener.
	 */
	public void removeGameListener(GameListener l) {
		gameListeners.removeElement(l);
	}
	
	/**
	 * Processes game events occurring on this 
	 * connection by dispatching them to any registered 
	 * GameListener objects. 
	 * 
	 * @param be		the board event.
	 */
	private void processGameEvent(GameEvent ge) {
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
		while(retry-- > 0 || serverName == null || local_pn == -1) {
			synchronized(this) {
				try {
					wait(10);
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
	private void changeTurn(int turn) {
		this.game.setTurn(turn);
		processGameEvent(new GameEvent(this, GameEvent.GAME_TURN_CHANGE, getPlayer(turn), ""));
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
	 * Send the game settings to the server
	 */
	public void sendGameSettings() {
    
        System.out.println("client: sending game settings");
    
		send(new Packet(Packet.COMMAND_SENDING_GAME_SETTINGS, gameSettings));
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
     * Sends a "delete entity" packet
     */
    public void sendDeleteEntity(int enum) {
        send(new Packet(Packet.COMMAND_ENTITY_REMOVE, new Integer(enum)));
    }

	
	/**
	 * Receives player information from the message packet.
	 */
	private void receivePlayerInfo(Packet c) {
        int pindex = c.getIntValue(0);
        Player newPlayer = (Player)c.getObject(1);
        if (getPlayer(newPlayer.getId()) != null) {
            game.setPlayer(pindex, newPlayer);
        } else {
            game.addPlayer(pindex, newPlayer);
        }
		processGameEvent(new GameEvent(this, GameEvent.GAME_PLAYER_STATUSCHANGE, newPlayer, ""));
	}

	/**
	 * Loads the board from the data in the net command.
	 */
	private void receiveBoard(Packet c) {
        Board newBoard = (Board)c.getObject(0);
        game.board.newData(newBoard.width, newBoard.height, newBoard.data);
        game.board.terrains = newBoard.terrains;
	}
	
	/**
	 * Loads the entities from the data in the net command.
	 */
	private void receiveEntities(Packet c) {
        Vector newEntities = (Vector)c.getObject(0);
        // re-link player in each entity
        for(Enumeration i = newEntities.elements(); i.hasMoreElements();) {
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
	private void receiveEntityUpdate(Packet c) {
        int eindex = c.getIntValue(0);
        Coords oc = game.getEntity(eindex).getPosition();
        Entity entity = (Entity)c.getObject(1);
        // re-link player
        entity.restore();
        entity.setOwner(getPlayer(entity.getOwnerId()));
            
        game.setEntity(eindex, entity);
        //XXX Hack alert!
        bv.boardChangedEntity(new BoardEvent(game.board, oc, entity, 0, 0)); //XXX
        //XXX
	}
	
	/**
	 * Loads entity firing data from the data in the net command
	 */
	private void receiveAttack(Packet c) {
        Object o = c.getObject(0);
        if (o instanceof AttackAction) {
            bv.addAttack((AttackAction)o);
        } else if (o instanceof TorsoTwistAction) {
            ;
        } else {
            System.out.println("client.receiveAttack: not attack or torso twist action");
            System.out.println(c.getObject(0));
        }
	}
	
	/**
	 * Reads a complete net command from the given input stream
	 */
	private Packet readPacket() {
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                  
            Packet packet = (Packet)ois.readObject();

            //System.out.println("c: received command #" + packet.getCommand());
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
	private void send(Packet c) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.flush();
			oos.writeObject(c);
			oos.flush();
			//System.out.println("c: command sent");
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
			// obey command
			switch(c.getCommand()) {
			case Packet.COMMAND_SERVER_NAME :
				serverName = (String)c.getObject(0);
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
			case Packet.COMMAND_ENTITY_UPDATE :
				receiveEntityUpdate(c);
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
				gameSettings = (GameSettings)c.getObject(0);
				processGameEvent(new GameEvent(this, GameEvent.GAME_NEW_SETTINGS, null, null));
        break;
			}
		}
	}
}
