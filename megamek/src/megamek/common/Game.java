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

package megamek.common;

import java.util.*;
import java.io.*;

/**
 * The game class is the root of all data about the game in progress.
 * Both the Client and the Server should have one of these objects and it
 * is their job to keep it synched.
 */
public class Game implements Serializable
{
    public static final int PHASE_UNKNOWN           = -1;
    public static final int PHASE_LOUNGE            = 1;
    public static final int PHASE_SELECTION         = 2;
    public static final int PHASE_EXCHANGE          = 3;
    public static final int PHASE_DEPLOYMENT        = 12;  // reorder someday
    public static final int PHASE_INITIATIVE        = 4;
    public static final int PHASE_MOVEMENT          = 5;
    public static final int PHASE_MOVEMENT_REPORT   = 6;
    public static final int PHASE_FIRING            = 7;
    public static final int PHASE_FIRING_REPORT     = 8;
    public static final int PHASE_PHYSICAL          = 9;
    public static final int PHASE_END               = 10;
    public static final int PHASE_VICTORY           = 11;
    /**
     * The number of Infantry platoons that have to move for every Mek
     * or Vehicle, if the "inf_move_multi" option is selected.
     */
    public static final int INF_MOVE_MULTI     	    = 3;
    
    public int phase = PHASE_UNKNOWN;
    private GameTurn turn;
    
    private GameOptions options = new GameOptions();

    public Board board = new Board();
    
    private Vector entities = new Vector();
    private Hashtable entityIds = new Hashtable();
    
    private Vector graveyard = new Vector(); // for dead entities
    
    private Vector players = new Vector();
    private Hashtable playerIds = new Hashtable();
    
    // have the entities been deployed?
    private boolean m_bHasDeployed = false;
    
	private int windDirection;
	private String stringWindDirection;
	
    /**
     * Constructor
     */
    public Game() {
        ;
    }
    
    public GameOptions getOptions() {
        return options;
    }
    
    public void setOptions(GameOptions options) {
        this.options = options;
    }
    
    
    public Board getBoard() {
        return board;
    }
    
    /**
     * Return an enumeration of player in the game
     */
    public Enumeration getPlayers() {
        return players.elements();
    }
    
    /**
     * Return the players vector
     */
    public Vector getPlayersVector() {
        return players;
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
        player.setGame(this);
        players.addElement(player);
        playerIds.put(new Integer(id), player);
    }
  
    public void setPlayer(int id, Player player) {
        final Player oldPlayer = getPlayer(id);
        player.setGame(this);
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
     * Returns the number of non-destroyed entityes owned by the player
     */
    public int getLiveEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if (entity.getOwner().equals(player) && !entity.isDestroyed()) {
                count++;
            }
        }
        return count;
    }    
  
    /**
     * Get a vector of entity objects that are "acceptable" to attack with this entity
     */
    public Vector getValidTargets(Entity entity) {
        Vector ents = new Vector();

        boolean friendlyFire = getOptions().booleanOption("friendly_fire");
        
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            Entity otherEntity = (Entity)i.nextElement();

            //if friendly fire is acceptable, do not shoot yourself            
            if (entity.isEnemyOf(otherEntity) || (friendlyFire && entity.getId() != otherEntity.getId() )) {
                ents.addElement( otherEntity );
            }
        }
        
        return ents;
    }
    
    /**
     * Returns the current GameTurn object
     */
    public GameTurn getTurn() {
        return turn;
    }
  
    /**
     * Sets the turn
     */
    public void setTurn(GameTurn turn) {
        this.turn = turn;
    }
    
    public boolean hasDeployed() {
        return m_bHasDeployed;
    }
    
    public void setHasDeployed(boolean in) {
        m_bHasDeployed = in;
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
     * Returns an enumeration of entities in the graveyard
     */
    public Enumeration getGraveyardEntities() {
        return graveyard.elements();
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

    /* The Entity ID space has been polluted; negative ID's below -1000 are targeted coordinates */

    public static Coords IdToCoords(int id) {
        System.out.print(id);
        if (id > -1000) {
            return null;
        }
        Coords c = new Coords((-id - 1000) & 16383, (-id - 1000) / 16384);
//        System.out.println(" -> (" + c.x + ", " + c.y + ")");
        return c;
    }

    public static int CoordsToId(Coords c) {
        int id = -1000 - c.x - c.y * 16384;
//        System.out.println("(" + c.x + ", " + c.y + ") -> " + id);
        return id;
    }


    public Entity getEntity(int id) {
        if(id <= -1000) {
           final Entity te = new HexEntity(IdToCoords(id));
           te.setGame(this);
           return te;
        }
        return (Entity)entityIds.get(new Integer(id));
    }
 
    public void addEntity(int id, Entity entity) {
        entity.setGame(this);
        entities.addElement(entity);
        entityIds.put(new Integer(id), entity);
    }
    
    public void setEntity(int id, Entity entity) {
        final Entity oldEntity = getEntity(id);
        entity.setGame(this);
        if (oldEntity == null) {
            entities.addElement(entity);
        } else {
            entities.setElementAt(entity, entities.indexOf(oldEntity));
        }
        entityIds.put(new Integer(id), entity);
    }
  
    /**
     * Remove an entity from the master list.  If we can't find that entity,
     * (probably due to double-blind) ignore it.
     */
    public void removeEntity(int id) {
        Entity toRemove = getEntity(id);
        if (toRemove != null) {
            entities.removeElement(toRemove);
            entityIds.remove(new Integer(id));
        }
    }
    
    /**
     * Checks to see if we have an entity in the master list, and if so,
     * returns its id number.  Otherwise returns -1.
     */
    public int getEntityID(Entity entity) {
        for (Enumeration i = entityIds.keys();i.hasMoreElements();) {
            Integer key = (Integer)i.nextElement();
            if (entityIds.get(key) == entity) return key.intValue();
        }
        return -1;
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
            // may as well set this here as well
            entity.setGame(this);
        }
    }
    
    /**
     * @deprecated use getFirstEntity instead
     */
    public Entity getEntity(Coords c) {
        return getFirstEntity(c);
    }
    
    /**
     * Returns the first entity at the given coordinate, if any.  Only returns
     * targetable (non-dead) entities.
     * 
     * @param c the coordinates to search at
     */
    public Entity getFirstEntity(Coords c) {
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (c.equals(entity.getPosition()) && entity.isTargetable()) {
                return entity;
            }
        }
        return null;
    }
    
    /**
     * Returns an Enumeration of the active entities at the given coordinates.
     */
    public Enumeration getEntities(Coords c) {
        Vector vector = new Vector();
        
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (c.equals(entity.getPosition()) && entity.isTargetable()) {
                vector.addElement(entity);
            }
        }

        return vector.elements();
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
     * See if the <code>Entity</code> with the given ID is in the graveyard.
     *
     * @param	id - the ID of the <code>Entity</code> to be checked.
     * @return  <code>true</code> if the <code>Entity</code> is in the
     *		graveyard, <code>false</code> otherwise.
     */
    public boolean isInGraveyard( int id ) {
        final Entity entity = getEntity(id);
        if (entity == null) {
	    // Unknown entity ID
	    return false;
	}
	return isInGraveyard( entity );
    }

    /**
     * See if the <code>Entity</code> is in the graveyard.
     *
     * @param	entity - the <code>Entity</code> to be checked.
     * @return  <code>true</code> if the <code>Entity</code> is in the
     *		graveyard, <code>false</code> otherwise.
     */
    public boolean isInGraveyard( Entity entity ) {
        if (entity == null) {
	    // No nulls in the graveyard
	    return false;
	}
	return graveyard.contains( entity );
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


    public void determineWindDirection() {
        windDirection = Compute.d6(1)-1;
        String[] dirNames = {"North", "Northeast", "Southeast", "South", "Southwest", "Northwest"};
        stringWindDirection = dirNames[windDirection];
    }
    
    public int getWindDirection() {
        return windDirection;
    }
    
    public String getStringWindDirection() {
        return stringWindDirection;
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

    /**
     * Determines if the indicated player has any remaining selectable infanty.
     *
     * @param	playerId - the <code>int</code> ID of the player
     * @return	<code>true</code> if the player has any remaining
     *		active infantry, <code>false</code> otherwise.
     */
    public boolean hasInfantry( int playerId ) {
	Player player = this.getPlayer( playerId );

        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( player.equals(entity.getOwner()) &&
		 entity.isSelectable() &&
		 entity instanceof Infantry ) {
                return true;
            }          
        }
        return false;
    }
}
