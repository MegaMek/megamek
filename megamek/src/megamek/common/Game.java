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

    /**
     * Define constants to describe the condition a
     * unit was in when it wass removed from the game.
     */
    public static final int UNIT_NEVER_JOINED   = 0x0000;
    public static final int UNIT_IN_RETREAT     = 0x0100;
    public static final int UNIT_SALVAGEABLE    = 0x0200;
    public static final int UNIT_DEVASTATED     = 0x0400;
    
    public int phase = PHASE_UNKNOWN;
    private GameTurn turn;
    
    private GameOptions options = new GameOptions();

    public Board board = new Board();
    
    private Vector entities = new Vector();
    private Hashtable entityIds = new Hashtable();
    
    private Vector graveyard = new Vector(); // for dead entities

    /**
     * Track units that have been retreated.
     */
    private Vector sanctuary = new Vector();

    /**
     * Track units that have been utterly devastated.
     */
    private Vector smithereens = new Vector();

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
     * their status, as long as they are in the game.
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

            // Even if friendly fire is acceptable, do not shoot yourself
            // Enemy units not on the board can not be shot.
            if ( null != otherEntity.getPosition() &&
                 ( entity.isEnemyOf(otherEntity) || 
                   (friendlyFire && entity.getId() != otherEntity.getId()) ) ) {
                ents.addElement( otherEntity );
            }
        }
        
        return ents;
    }
    
    /**
     * Returns true if this phase has turns.  If false, the phase is simply
     * waiting for everybody to declare "done".
     */
    public boolean phaseHasTurns(int phase) {
        switch (phase) {
            case PHASE_DEPLOYMENT :
            case PHASE_MOVEMENT :
            case PHASE_FIRING :
            case PHASE_PHYSICAL :
                return true;
            default :
                return false;
        }
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
    
    public int getPhase() {
        return phase;
    }
    
    public void setPhase(int phase) {
        this.phase = phase;
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
     * Returns an enumeration of entities that have retreated
     */
    public Enumeration getRetreatedEntities() {
        return sanctuary.elements();
    }

    /**
     * Returns an enumeration of entities that were utterly destroyed
     */
    public Enumeration getDevastatedEntities() {
        return smithereens.elements();
    }
    
    /**
     * Return the current number of entities in the game.
     */
    public int getNoOfEntities() {
        return entities.size();
    }
    
    /**
     * Returns the appropriate target for this game given a type and id
     */
    public Targetable getTarget(int nType, int nID) {
        switch (nType) {
            case Targetable.TYPE_ENTITY :
                return getEntity(nID);
            case Targetable.TYPE_HEX_CLEAR :
            case Targetable.TYPE_HEX_IGNITE :
                return new HexTarget(HexTarget.idToCoords(nID), nType);
            default :
                return null;
        }
    }
    
    /**
     * Returns the entity with the given id number, if any.
     */

    public Entity getEntity(int id) {
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
     * Returns true if an entity with the specified id number exists in this
     * game.
     */
    public boolean hasEntity(int entityId) {
        return entityIds.containsKey(new Integer(entityId));
    }
  
    /**
     * Remove an entity from the master list.  If we can't find that entity,
     * (probably due to double-blind) ignore it.
     * Method kept for backwards compatability.
     */
    public void removeEntity(int id) {
        this.removeEntity( id, UNIT_NEVER_JOINED );
    }

    /**
     * Remove an entity from the master list.  If we can't find that entity,
     * (probably due to double-blind) ignore it.
     */
    public void removeEntity( int id, int condition ) {
        Entity toRemove = getEntity(id);
        if (toRemove != null) {
            entities.removeElement(toRemove);
            entityIds.remove(new Integer(id));

            // Where does it go from here?
            switch ( condition ) {
            case UNIT_NEVER_JOINED:
                // Nothing further required.
                break;
            case UNIT_IN_RETREAT  :
                // Move it to the rolls of those with dubious honor.
                sanctuary.addElement( toRemove );
                break;
            case UNIT_SALVAGEABLE :
                // Move it into the graveyard where it may be resurrected.
                graveyard.addElement( toRemove );
                break;
            case UNIT_DEVASTATED  :
                // Mark the unit as way gone.
                smithereens.addElement( toRemove );
                break;
            }
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

        // Only build the list if the coords are off the board.
        if ( this.board.contains(c) ) {
            for (Enumeration i = entities.elements(); i.hasMoreElements();) {
                final Entity entity = (Entity)i.nextElement();
                if (c.equals(entity.getPosition()) && entity.isTargetable()) {
                    vector.addElement(entity);
                }
            }
        }

        return vector.elements();
    }

    /**
     * Moves an entity into the graveyard so it stops getting sent
     * out every phase.
     */
    public void moveToGraveyard(int id) {
        this.removeEntity( id, UNIT_SALVAGEABLE );
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
     * Returns the first entity that can act in the present turn, or null if
     * none can.
     */
    public Entity getFirstEntity() {
        return getFirstEntity(turn);
    }
    
    /**
     * Returns the first entity that can act in the specified turn, or null if
     * none can.
     */
    public Entity getFirstEntity(GameTurn turn) {
        return getEntity(getFirstEntityNum(turn));
    }
    
    /**
     * Returns the id of the first entity that can act in the current turn,
     * or -1 if none can.
     */
    public int getFirstEntityNum() {
        return getFirstEntityNum(turn);
    }
    
    /**
     * Returns the id of the first entity that can act in the specified turn,
     * or -1 if none can.
     */
    public int getFirstEntityNum(GameTurn turn) {
        if (turn == null) {
            return -1;
        }
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (turn.isValidEntity(entity)) {
                return entity.getId();
            }
        }
        return -1;
    }
    
    /**
     * Returns the next selectable entity that can act this turn,
     * or null if none can.
     * 
     * @param start the index number to start at
     */
    public Entity getNextEntity(int start) {
        return getEntity(getNextEntityNum(turn, start));
    }

    public int getNextEntityNum(int start) {
        return getNextEntityNum(turn, start);
    }

    /**
     * Returns the entity id of the next entity that can move during the 
     * specified
     * 
     * @param turn the turn to use
     * @param start the entity id to start at
     */
    public int getNextEntityNum(GameTurn turn, int start) {
        boolean startPassed = false;
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (entity.getId() == start) {
                startPassed = true;
            } else if (startPassed && turn.isValidEntity(entity)) {
                return entity.getId();
            }
        }
        return getFirstEntityNum(turn);
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
     * Get the entities for the player.
     *
     * @param   player - the <code>Player</code> whose entities are required.
     * @return  a <code>Vector</code> of <code>Entity</code>s.
     */
    public Vector getPlayerEntities( Player player ) {
        Vector output = new Vector();
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( player.equals(entity.getOwner()) ) {
                output.addElement( entity );
            }
        }
        return output;
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

    /**
     * Check each player for the presence of a Battle Armor squad equipped
     * with a Magnetic Clamp.  If one unit is found, update that player's
     * units to allow the squad to be transported.
     * <p/>
     * This method should be called </b>*ONCE*</b> per game, after all units
     * for all players have been loaded.
     *
     * @return  <code>true</code> if a unit was updated, <code>false</code>
     *          if no player has a Battle Armor squad equipped with a
     *          Magnetic Clamp.
     */
    public boolean checkForMagneticClamp() {

        // Declare local variables.
        Player          player = null;
        Entity          unit = null;
        boolean         result;
        Hashtable       playerFlags = null;
        Enumeration     enum = null;
        Mounted         equip = null;
        String          name = null;

        // Assume that we don't need new transporters.
        result = false;

        // Create a map of flags for the players.
        playerFlags = new Hashtable( this.getNoOfPlayers() );

        // Walk through the game's entities.
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {

            // Is the next unit a Battle Armor squad?
            unit = (Entity)i.nextElement();
            if ( unit instanceof BattleArmor ) {

                // Does the unit have a Magnetic Clamp?
                enum = unit.getMisc();
                while ( enum.hasMoreElements() ) {
                    equip = (Mounted) enum.nextElement();
                    name = equip.getType().getInternalName();
                    if ( BattleArmor.MAGNETIC_CLAMP.equals( name ) ){
                        // The unit's player needs new transporters.
                        result = true;
                        playerFlags.put( unit.getOwner(), Boolean.TRUE );

                        // Stop looking.
                        break;
                    }
                }

            } // End unit-is-BattleArmor

        } // Handle the next entity.

        // Do we need to add any Magnetic Clamp transporters?
        if ( result ) {

            // Walk through the game's entities again.
            for (Enumeration i = entities.elements(); i.hasMoreElements();) {

                // Get this unit's player.
                unit = (Entity)i.nextElement();
                player = unit.getOwner();

                // Does this player need updated transporters?
                if ( Boolean.TRUE.equals( playerFlags.get(player) ) ) {

                    // Add the appropriate transporter to the unit.
                    if ( !unit.isOmni() && unit instanceof Mech ) {
                        unit.addTransporter( new ClampMountMech() );
                    }
                    else if ( unit instanceof Tank ) {
                        unit.addTransporter( new ClampMountTank() );
                    }

                }
            } // End player-needs-transports

        } // Handle the next unit.

        // Return the result.
        return result;

    } // End private boolean checkForMagneticClamp()

}
