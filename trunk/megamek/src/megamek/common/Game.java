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

package megamek.common;

import java.util.*;
import java.io.*;

/**
 * The game class is the root of all data about the game in progress.
 * Both the Client and the Server should have one of these objects and it
 * is their job to keep it synched.
 */
public class Game
{
	public static final int PHASE_UNKNOWN           = -1;
	public static final int PHASE_LOUNGE            = 1;
	public static final int PHASE_SELECTION         = 2;
	public static final int PHASE_EXCHANGE          = 3;
	public static final int PHASE_INITIATIVE        = 4;
	public static final int PHASE_MOVEMENT		    = 5;
	public static final int PHASE_MOVEMENT_REPORT   = 6;
	public static final int PHASE_FIRING            = 7;
	public static final int PHASE_FIRING_REPORT     = 8;
	public static final int PHASE_PHYSICAL		    = 9;
	public static final int PHASE_END               = 10;
	
	public int phase = PHASE_UNKNOWN;
	private int turn; // whose turn it is

    public Board board = new Board();
    
    private Vector entities = new Vector();
    private Hashtable entityIds = new Hashtable();
    
    private Vector graveyard = new Vector(); // for dead entities
    
    private Vector players = new Vector();
    private Hashtable playerIds = new Hashtable();
	
	/**
	 * Constructor
	 */
	public Game() {
		;
	}
	
	/**
	 * Return an enumeration of player in the game
	 */
	public Enumeration getPlayers() {
		return players.elements();
	}
	
	/**
	 * Return the current number of active players in the game.
	 */
	public int getNoOfPlayers() {
		return players.size();
	}
	
	/**
	 * Returns the individual player assigned the id parameter.
	 */
	public Player getPlayer(int id) {
        return (Player)playerIds.get(new Integer(id));
	}
	
    public void addPlayer(int id, Player player) {
        players.addElement(player);
        playerIds.put(new Integer(id), player);
    }
  
    public void setPlayer(int id, Player player) {
        final Player oldPlayer = getPlayer(id);
        players.setElementAt(player, players.indexOf(oldPlayer));
        playerIds.put(new Integer(id), player);
    }
  
    public void removePlayer(int id) {
        players.removeElement(getPlayer(id));
        playerIds.remove(new Integer(id));
    }
  
    /**
     * Returns the number of entities owned by the player, regardless of
     * their status.
     */
    public int getEntitiesOwnedBy(Player player) {
        int count = 0;
	    for (Enumeration i = entities.elements(); i.hasMoreElements();) {
	    	Entity entity = (Entity)i.nextElement();
	    	if (entity.getOwner().equals(player)) {
	    		count++;
	    	}
	    }
        return count;
    }
  
    /**
     * Returns the player number whose turn it is
     */
    public int getTurn() {
        return turn;
    }
  
    /**
     * Sets the turn
     */
    public void setTurn(int turn) {
        this.turn = turn;
    }
	
	/**
	 * Returns an enumeration of all the entites in the game.
	 */
	public Enumeration getEntities() {
        return entities.elements();
	}
	
	/**
	 * Returns the actual vector for the entities
	 */
	public Vector getEntitiesVector() {
        return entities;
	}
	
	public void setEntitiesVector(Vector entities) {
        this.entities = entities;
        reindexEntities();
        
	}
	
	/**
	 * Return the current number of entities in the game.
	 */
	public int getNoOfEntities() {
		return entities.size();
	}
	
	/**
	 * Returns the entity with the given id number, if any.
	 */
	public Entity getEntity(int id) {
        return (Entity)entityIds.get(new Integer(id));
	}
  
    public void addEntity(int id, Entity entity) {
        entities.addElement(entity);
        entityIds.put(new Integer(id), entity);
    }
	
    public void setEntity(int id, Entity entity) {
        final Entity oldEntity = getEntity(id);
        entities.setElementAt(entity, entities.indexOf(oldEntity));
        entityIds.put(new Integer(id), entity);
    }
  
    public void removeEntity(int id) {
        entities.removeElement(getEntity(id));
        entityIds.remove(new Integer(id));
    }
	
    /**
     * Regenerates the entities by id hashtable by going thru all entities
     * in the Vector
     */
    private void reindexEntities() {
        entityIds.clear();
		for (Enumeration i = entities.elements(); i.hasMoreElements();) {
			final Entity entity = (Entity)i.nextElement();
            entityIds.put(new Integer(entity.getId()), entity);
        }
    }
    
	/**
	 * Returns the entity at the given coordinate, if any.
	 * 
	 * @param c the coordinates to search at
	 */
	public Entity getEntity(Coords c) {
		for (Enumeration i = entities.elements(); i.hasMoreElements();) {
			final Entity entity = (Entity)i.nextElement();
			if (c.equals(entity.getPosition()) && entity.isTargetable()) {
				return entity;
			}
		}
		return null;
	}
  
    /**
     * Moves an entity into the graveyard so it stops getting sent
     * out every phase.
     */
    public void moveToGraveyard(int id) {
        final Entity entity = getEntity(id);
        if (entity != null) {
            removeEntity(id);
            graveyard.addElement(entity);
        }
    }
	
	/**
	 * Returns the first selectable entity that belongs to the player,
	 * or null if none do.
	 * 
	 * @param player the player.
	 */
	public Entity getFirstEntity(Player player) {
	    return getEntity(getFirstEntityNum(player));
	}
	
	/**
	 * Returns the entity number corresponding to the first selectable 
	 * entity that belongs to the player, or -1 if none do.
	 * 
	 * @param player the player.
	 */
	public int getFirstEntityNum(Player player) {
		for (Enumeration i = entities.elements(); i.hasMoreElements();) {
			final Entity entity = (Entity)i.nextElement();
			if (player.equals(entity.getOwner()) && entity.isSelectable()) {
				return entity.getId();
			}
		}
		return -1;
    }
	
	/**
	 * Returns the next selectable entity that belongs to the player,
	 * or null if none do.
	 * 
	 * @param player the player.
	 * @param start the index number to start at.
	 */
	public Entity getNextEntity(Player player, int start) {
		return getEntity(getNextEntityNum(player, start));
	}
	
	/**
	 * Returns the entity number corresponding to the next selectable 
	 * entity that belongs to the player, or -1 if none do.
	 * 
	 * @param player the player.
	 * @param start the index number to start at.
	 */
	public int getNextEntityNum(Player player, int start) {
        boolean startPassed = false;
		for (Enumeration i = entities.elements(); i.hasMoreElements();) {
			final Entity entity = (Entity)i.nextElement();
            if (entity.getId() == start) {
                startPassed = true;
            } else if (startPassed && player.equals(entity.getOwner()) 
                       && entity.isSelectable() ) {
                return entity.getId();
            }          
		}
        return getFirstEntityNum(player);
	}
	
}
