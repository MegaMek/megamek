/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.options.GameOptions;

/**
 * This interface is the root of all data about the game in progress.
 * Both the Client and the Server should have one of these objects and it
 * is their job to keep it synched.
 */
public interface IGame {
    
    public static final int PHASE_UNKNOWN = -1;
    
    public static final int PHASE_LOUNGE = 1;
    
    public static final int PHASE_SELECTION = 2;
    
    public static final int PHASE_EXCHANGE = 3;
    
    public static final int PHASE_DEPLOYMENT = 4;
    
    public static final int PHASE_INITIATIVE = 5;
    
    public static final int PHASE_TARGETING = 6; 
    
    public static final int PHASE_MOVEMENT = 7; 
    
    public static final int PHASE_MOVEMENT_REPORT = 8; 
    
    public static final int PHASE_OFFBOARD = 9; 
    
    public static final int PHASE_OFFBOARD_REPORT = 10;
    
    public static final int PHASE_FIRING = 11;
    
    public static final int PHASE_FIRING_REPORT = 12;
    
    public static final int PHASE_PHYSICAL = 13;
    
    public static final int PHASE_END = 14;
    
    public static final int PHASE_VICTORY = 15;
    
    public static final int PHASE_DEPLOY_MINEFIELDS = 16;
    
    public static final int PHASE_STARTING_SCENARIO = 17;
    
    public static final int PHASE_SET_ARTYAUTOHITHEXES = 18;

    /**
     * Adds the specified game listener to receive
     * board events from this Game.
     *
     * @param listener the game listener.
     */
    public abstract void addGameListener(GameListener listener);
    
    /**
     * Removes the specified game listener.
     *
     * @param listener the game listener.
     */
    public abstract void removeGameListener(GameListener listener);
    
    /**
     * Processes game events by dispatching them to any registered
     * GameListener objects.
     *
     * @param event the game event.
     */
    public abstract void processGameEvent(GameEvent event);    

    /**
     * @return If it's a mech in the first hex used by the LOS tool
     */
    public abstract boolean getMechInFirst();

    /**
     * 
     * @return If it's a mech in the second hex used by the LOS tool
     */
    public abstract boolean getMechInSecond();

    /**
     * 
     * @param mech If it's a mech in the first hex used by the LOS tool
     */
    public abstract void setMechInFirst(boolean mech);

    /**
     * 
     * @param mech If it's a mech in the second hex used by the LOS tool
     */
    public abstract void setMechInSecond(boolean mech);
    
    /**
     * Check if there is a minefield at given coords
     * @param coords coords to check
     * @return <code>true</code> if there is a minefield at given coords
     *         or <code>false</code> otherwise 
     */
    public abstract boolean containsMinefield(Coords coords);

    /**
     * Get the minefields at specified coords 
     * @param coords
     * @return the <code>Vector</code> of minefields at specified coord
     */
    public abstract Vector getMinefields(Coords coords);

    /**
     * Get the number of the minefields at specified coords 
     * @param coords
     * @return the number of the minefields at specified coord
     */
    public abstract int getNbrMinefields(Coords coords);
    
    /**
     * Get the coordinates of all mined hexes in the game.
     *
     * @return  an <code>Enumeration</code> of the <code>Coords</code>
     *          containing minefilds.  This will not be <code>null</code>.
     */
    public abstract Enumeration getMinedCoords();
    
    public abstract void addMinefield(Minefield mf);

    public abstract void addMinefields(Vector minefields);
    
    public abstract void setMinefields(Vector minefields);

    public abstract void removeMinefield(Minefield mf);
    
    public abstract void clearMinefields();
    
    public abstract Vector getVibrabombs();
    
    public abstract void addVibrabomb(Minefield mf);
    
    public abstract void removeVibrabomb(Minefield mf);
    
    public abstract boolean containsVibrabomb(Minefield mf);
    
    public abstract GameOptions getOptions();
    
    public abstract void setOptions(GameOptions options);
    
    public abstract IBoard getBoard();
    
    public abstract void setBoard(IBoard board);

    /**
     * Return an enumeration of teams in the game
     */
    public abstract Enumeration getTeams();
    
    /** Return the teams vector 
     * 
     */
    public abstract Vector getTeamsVector();
    
    /**
     * Return a players team
     *  Note: may return null if player has no team
     */
    public abstract Team getTeamForPlayer(Player p);
    
    /**
     * Return an enumeration of player in the game
     */
    public abstract Enumeration getPlayers();
    
    /**
     * Return the players vector
     */
    public abstract Vector getPlayersVector();
    
    /**
     * Return the current number of active players in the game.
     */
    public abstract int getNoOfPlayers();
    
    /**
     * Returns the individual player assigned the id parameter.
     */
    public abstract Player getPlayer(int id);
    
    public abstract void addPlayer(int id, Player player);
    
    public abstract void setPlayer(int id, Player player);
    
    public abstract void removePlayer(int id);
    
    /**
     * Returns the number of entities owned by the player, regardless of
     * their status, as long as they are in the game.
     */
    public abstract int getEntitiesOwnedBy(Player player);
    
    /**
     * Returns the number of non-destroyed entityes owned by the player
     */
    public abstract int getLiveEntitiesOwnedBy(Player player);
    
    /**
     * Returns the number of non-destroyed deployed entities owned
     * by the player.  Ignore offboard units and captured Mek pilots.
     */
    public abstract int getLiveDeployedEntitiesOwnedBy(Player player);
    
    /**
     * Returns true if the player has a valid unit with the Tactical Genius
     *  pilot special ability.
     */
    public abstract boolean hasTacticalGenius(Player player);
    
    /**
     * Returns how much higher than 50 or lower than -30
     * degrees, divided by ten, rounded up, the temperature is 
     */
    public abstract int getTemperatureDifference();
    
    /**
     * Get a vector of entity objects that are "acceptable" to attack with this entity
     */
    public abstract Vector getValidTargets(Entity entity);
    
    /**
     * Returns true if this phase has turns.  If false, the phase is simply
     * waiting for everybody to declare "done".
     */
    public abstract boolean phaseHasTurns(int phase);
    
    /**
     * Returns the current GameTurn object
     */
    public abstract GameTurn getTurn();
    
    /** 
     * Changes to the next turn, returning it.
     */
    public abstract GameTurn changeToNextTurn();
    
    /**
     * Resets the turn index to -1 (awaiting first turn)
     */
    public abstract void resetTurnIndex();
    
    /**
     * Returns true if there is a turn after the current one
     */
    public abstract boolean hasMoreTurns();
    
    /** 
     * Inserts a turn that will come directly after the current one
     */
    public abstract void insertNextTurn(GameTurn turn);
    
    /**
     * Returns an Enumeration of the current turn list
     */
    public abstract Enumeration getTurns();
    
    /**
     * Returns the current turn index
     */
    public abstract int getTurnIndex();
    
    /**
     * Sets the current turn index
     */
    public abstract void setTurnIndex(int turnIndex);
    
    /**
     * Returns the current turn vector
     */
    public abstract Vector getTurnVector();
    
    /**
     * Sets the current turn vector
     */
    public abstract void setTurnVector(Vector turnVector);
    
    public abstract int getPhase();
    
    public abstract void setPhase(int phase);
    
    public abstract int getLastPhase();
    
    public abstract void setLastPhase(int lastPhase);
    
    public abstract void setDeploymentComplete(boolean deploymentComplete);
    
    public abstract boolean isDeploymentComplete();
    
    /**
     * Sets up up the hashtable of who deploys when
     */
    public abstract void setupRoundDeployment();
    
    /**
     * Checks to see if we've past our deployment completion
     */
    public abstract void checkForCompleteDeployment();
    
    /**
     * Check to see if we should deploy this round
     */
    public abstract boolean shouldDeployThisRound();
    
    public abstract boolean shouldDeployForRound(int round);
    
    /**
     * Clear this round from this list of entities to deploy
     */
    public abstract void clearDeploymentThisRound();
    
    /**
     * Returns a vector of entities that have not yet deployed
     */
    public abstract Vector getUndeployedEntities();
    
    /**
     * Returns an enumeration of all the entites in the game.
     */
    public abstract Enumeration getEntities();
    
    /**
     * Returns the actual vector for the entities
     */
    public abstract Vector getEntitiesVector();
    
    public abstract void setEntitiesVector(Vector entities);

    /**
     * Returns the actual vector for the out-of-game entities
     */
    public abstract Vector getOutOfGameEntitiesVector();
    
    /**
     * Returns an out-of-game entity.
     *
     * @param   id the <code>int</code> ID of the out-of-game entity.
     * @return  the out-of-game <code>Entity</code> with that ID.  If no
     *          out-of-game entity has that ID, returns a <code>null</code>.
     */
    public abstract Entity getOutOfGameEntity (int id);
    
    /**
     * Swap out the current list of dead (or fled) units for a new one.
     *
     * @param   vOutOfGame - the new <code>Vector</code> of dead or fled units.
     *          This value should <em>not</em> be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if the new list is
     *          <code>null</code>.
     */
    public abstract void setOutOfGameEntitiesVector(Vector vOutOfGame);
    
    /**
     * Returns a <code>Vector</code> containing the <code>Entity</code>s
     * that are in the same C3 network as the passed-in unit.  The output
     * will contain the passed-in unit, if the unit has a C3 computer.  If
     * the unit has no C3 computer, the output will be empty (but it will
     * never be <code>null</code>).
     *
     * @param   entity - the <code>Entity</code> whose C3 network co-
     *          members is required.  This value may be <code>null</code>.
     * @return  a <code>Vector</code> that will contain all other
     *          <code>Entity</code>s that are in the same C3 network
     *          as the passed-in unit.  This <code>Vector</code> may
     *          be empty, but it will not be <code>null</code>.
     * @see     #getC3SubNetworkMembers(Entity)
     */
    public abstract Vector getC3NetworkMembers(Entity entity);
    
    /**
     * Returns a <code>Vector</code> containing the <code>Entity</code>s
     * that are in the C3 sub-network under the passed-in unit.  The output
     * will contain the passed-in unit, if the unit has a C3 computer.  If
     * the unit has no C3 computer, the output will be empty (but it will
     * never be <code>null</code>).  If the passed-in unit is a company
     * commander or a member of a C3i network, this call is the same as
     * <code>getC3NetworkMembers</code>.
     *
     * @param   entity - the <code>Entity</code> whose C3 network sub-
     *          members is required.  This value may be <code>null</code>.
     * @return  a <code>Vector</code> that will contain all other
     *          <code>Entity</code>s that are in the same C3 network
     *          under the passed-in unit.  This <code>Vector</code> may
     *          be empty, but it will not be <code>null</code>.
     * @see     #getC3NetworkMembers(Entity)
     */
    public abstract Vector getC3SubNetworkMembers(Entity entity);
    
    /**
     * Returns a <code>Hashtable</code> that maps the <code>Coords</code>
     * of each unit in this <code>Game</code> to a <code>Vector</code>
     * of <code>Entity</code>s at that positions.  Units that have no
     * position (e.g. loaded units) will not be in the map.
     *
     * @return  a <code>Hashtable</code> that maps the <code>Coords</code>
     *          positions or each unit in the game to a <code>Vector</code>
     *          of <code>Entity</code>s at that position.
     */
    public abstract Hashtable getPositionMap();
    
    /**
     * Returns an enumeration of salvagable entities.
     */
    public abstract Enumeration getGraveyardEntities();
    
    /**
     * Returns an enumeration of wrecked entities.
     */
    public abstract Enumeration getWreckedEntities();
    
    /**
     * Returns an enumeration of entities that have retreated
     */
    public abstract Enumeration getRetreatedEntities();
    
    /**
     * Returns an enumeration of entities that were utterly destroyed
     */
    public abstract Enumeration getDevastatedEntities();
    
    /**
     * Return the current number of entities in the game.
     */
    public abstract int getNoOfEntities();
    
    /**
     * Returns the appropriate target for this game given a type and id
     */
    public abstract Targetable getTarget(int nType, int nID);
    
    /**
     * Returns the entity with the given id number, if any.
     */
    public abstract Entity getEntity(int id);
    
    public abstract void addEntity(int id, Entity entity);
    
    public abstract void setEntity(int id, Entity entity);

    public void setEntity(int id, Entity entity, Vector movePath);
    
    /**
     * @return int containing an unused entity id
     */
    public abstract int getNextEntityId();
    
    /**
     * @return <code>true</code> if an entity with the specified id number exists 
     * in this game.
     */
    public abstract boolean hasEntity(int entityId);
    
    /**
     * Remove an entity from the master list.  If we can't find that entity,
     * (probably due to double-blind) ignore it.
     */
    public abstract void removeEntity(int id, int condition);
    
    /**
     * Resets this game by removing all entities.
     */
    public abstract void reset();
    
    /**
     * Returns the first entity at the given coordinate, if any.  Only returns
     * targetable (non-dead) entities.
     *
     * @param c the coordinates to search at
     */
    public abstract Entity getFirstEntity(Coords c);
    
    /**
     * Returns the first enemy entity at the given coordinate, if any.
     * Only returns targetable (non-dead) entities.
     *
     * @param c the coordinates to search at
     * @param currentEntity the entity that is firing
     */
    public abstract Entity getFirstEnemyEntity(Coords c, Entity currentEntity);
    
    /**
     * Returns an Enumeration of the active entities at the given coordinates.
     */
    public abstract Enumeration getEntities(Coords c);
    
    /**
     * Returns a Target for an Accidental Fall From above, or null if no
     * possible target is there
     * @param c The <code>Coords</code> of the hex in which the accidental fall
     *          from above happens
     * @return  The <code>Entity</code> that should be an AFFA target.
     */
    public abstract Entity getAffaTarget(Coords c);
    
    /**
     * Returns an <code>Enumeration</code> of the enemy's active
     * entities at the given coordinates.
     *
     * @param   c the <code>Coords</code> of the hex being examined.
     * @param   currentEntity the <code>Entity</code> whose enemies are needed.
     * @return  an <code>Enumeration</code> of <code>Entity</code>s at the
     *          given coordinates who are enemies of the given unit.
     */
    public abstract Enumeration getEnemyEntities(final Coords c, final Entity currentEntity);
    
    /**
     * Moves an entity into the graveyard so it stops getting sent
     * out every phase.
     */
    public abstract void moveToGraveyard(int id);
    
    /**
     * See if the <code>Entity</code> with the given ID is out of the game.
     *
     * @param id - the ID of the <code>Entity</code> to be checked.
     * @return  <code>true</code> if the <code>Entity</code> is in the
     *    graveyard, <code>false</code> otherwise.
     */
    public abstract boolean isOutOfGame(int id);
    
    /**
     * See if the <code>Entity</code> is out of the game.
     *
     * @param entity - the <code>Entity</code> to be checked.
     * @return  <code>true</code> if the <code>Entity</code> is in the
     *    graveyard, <code>false</code> otherwise.
     */
    public abstract boolean isOutOfGame(Entity entity);
    
    /**
     * Returns the first entity that can act in the present turn, or null if
     * none can.
     */
    public abstract Entity getFirstEntity();
    
    /**
     * Returns the first entity that can act in the specified turn, or null if
     * none can.33
     */
    public abstract Entity getFirstEntity(GameTurn turn);
    
    /**
     * Returns the id of the first entity that can act in the current turn,
     * or -1 if none can.
     */
    public abstract int getFirstEntityNum();
    
    /**
     * Returns the id of the first entity that can act in the specified turn,
     * or -1 if none can.
     */
    public abstract int getFirstEntityNum(GameTurn turn);
    
    /**
     * Returns the next selectable entity that can act this turn,
     * or null if none can.
     *
     * @param start the index number to start at
     */
    public abstract Entity getNextEntity(int start);
    
    public abstract int getNextEntityNum(int start);
    
    /**
     * Returns the entity id of the next entity that can move during the
     * specified
     *
     * @param turn the turn to use
     * @param start the entity id to start at
     */
    public abstract int getNextEntityNum(GameTurn turn, int start);
    
    /**
     * Returns the number of the first deployable entity
     */
    public abstract int getFirstDeployableEntityNum();
    
    public abstract int getFirstDeployableEntityNum(GameTurn turn);
    
    /**
     * Returns the number of the next deployable entity
     */
    public abstract int getNextDeployableEntityNum(int entityId);
    
    public abstract int getNextDeployableEntityNum(GameTurn turn, int start);
    
    public abstract void determineWind();
    
    public abstract int getWindDirection();
    
    public abstract String getStringWindDirection();
    
    public abstract int getWindStrength();
    
    public abstract String getStringWindStrength();
    
    /**
     * Get the entities for the player.
     *
     * @param   player - the <code>Player</code> whose entities are required.
     * @return  a <code>Vector</code> of <code>Entity</code>s.
     */
    public abstract Vector getPlayerEntities(Player player);
    
    /**
     * Determines if the indicated entity is stranded on a transport that
     * can't move.
     * <p/>
     * According to <a href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">
     * Randall Bills</a>, the "minimum move" rule allow stranded units to
     * dismount at the start of the turn.
     *
     * @param   entity the <code>Entity</code> that may be stranded
     * @return  <code>true</code> if the entity is stranded
     *          <code>false</code> otherwise.
     */
    public abstract boolean isEntityStranded(Entity entity);
    
    /**
     * Returns the number of remaining selectable infantry owned by a player.
     */
    public abstract int getInfantryLeft(int playerId);
    
    /**
     * Returns the number of remaining selectable Protomechs owned by a player.
     */
    public abstract int getProtomechsLeft(int playerId);
    
    /**
     * Removes the last, next turn found that the specified entity can move in.
     * Used when, say, an entity dies mid-phase.
     */
    public abstract void removeTurnFor(Entity entity);
    
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
    public abstract boolean checkForMagneticClamp();
    
    /** Adds the specified action to the actions list for this phase. */
    public abstract void addAction(EntityAction ea);
    
    public abstract void addArtilleryAttack(ArtilleryAttackAction aaa);
    
    public abstract void removeArtilleryAttack(ArtilleryAttackAction aaa);
    
    public abstract Vector getArtilleryVector();
    
    public abstract Enumeration getArtilleryAttacks();
    
    public abstract int getArtillerySize();
    
    /** 
     * Returns an Enumeration of actions scheduled for this phase. 
     */
    public abstract Enumeration getActions();
    
    /**
     * Resets the actions list.
     */
    public abstract void resetActions();
    
    /**
     * Removes all actions by the specified entity
     */
    public abstract void removeActionsFor(int entityId);

    /**
     * Remove a specified action
     * @param o The action to remove.
     */
    public abstract void removeAction(Object o);
    
    public abstract int actionsSize();
    
    /** 
     * Returns the actions vector.  Do not use to modify the actions;
     * I will be angry. >:[  Used for sending all actions to the client.
     */
    public abstract Vector getActionsVector();
    
    public abstract void addInitiativeRerollRequest(Team t);
    
    public abstract Vector getInitiativeRerollRequests();
    
    /**
     * Adds a pending displacement attack to the list for this phase.
     */
    public abstract void addCharge(AttackAction ea);
    
    /**
     * Returns an Enumeration of displacement attacks scheduled for the end
     * of the physical phase.
     */
    public abstract Enumeration getCharges();
    
    /**
     * Resets the pending charges list.
     */
    public abstract void resetCharges();
    
    /**
     * Returns the charges vector.  Do not modify. >:[ Used for sending all
     * charges to the client.
     */
    public abstract Vector getChargesVector();
    
    /**
     * Adds a pending PSR to the list for this phase.
     */
    public abstract void addPSR(PilotingRollData psr);
    
    /**
     * Returns an Enumeration of pending PSRs.
     */
    public abstract Enumeration getPSRs();
    
    /**
     * Adds a pending extreme Gravity PSR to the list for this phase.
     */
    public abstract void addExtremeGravityPSR(PilotingRollData psr);
    
    /**
     * Returns an Enumeration of pending extreme GravityPSRs.
     */
    public abstract Enumeration getExtremeGravityPSRs();
    
    /**
     * Resets the PSR list for a given entity.
     */
    public abstract void resetPSRs(Entity entity);
    
    /**
     * Resets the extreme Gravity PSR list.
     */
    public abstract void resetExtremeGravityPSRs();
    
    /**
     * Resets the extreme Gravity PSR list for a given entity.
     */
    public abstract void resetExtremeGravityPSRs(Entity entity);
    
    /**
     * Resets the PSR list.
     */
    public abstract void resetPSRs();
    
    /** 
     * Getter for property roundCount.
     * @return Value of property roundCount.
     */
    public abstract int getRoundCount();
    
    public abstract void setRoundCount(int roundCount);
    
    /**
     * Increments the round counter
     */
    public abstract void incrementRoundCount();
    
    /**
     * Getter for property forceVictory.
     * @return Value of property forceVictory.
     */
    public abstract boolean isForceVictory();
    
    /**
     * Setter for property forceVictory.
     * @param forceVictory New value of property forceVictory.
     */
    public abstract void setForceVictory(boolean forceVictory);
    
    /**
     * Getter for property roundReport.
     * @return Value of property roundReport.
     */
    public abstract java.lang.StringBuffer getRoundReport();
    
    /**
     * Resets the round report
     */
    public abstract void resetRoundReport();
    
    /**
     * Getter for property phaseReport.
     * @return Value of property phaseReport.
     */
    public abstract java.lang.StringBuffer getPhaseReport();
    
    /**
     * Resets the round report 
     */
    public abstract void resetPhaseReport();

    public abstract void end(int winner, int winnerTeam);

    /**
     * Getter for property victoryPlayerId.
     * @return Value of property victoryPlayerId.
     */
    public abstract int getVictoryPlayerId();
    
    /**
     * Setter for property victoryPlayerId.
     * @param victoryPlayerId New value of property victoryPlayerId.
     */
    public abstract void setVictoryPlayerId(int victoryPlayerId);
    
    /**
     * Getter for property victoryTeam.
     * @return Value of property victoryTeam.
     */
    public abstract int getVictoryTeam();
    
    /**
     * Setter for property victoryTeam.
     * @param victoryTeam New value of property victoryTeam.
     */
    public abstract void setVictoryTeam(int victoryTeam);
    
    /**
     * Returns true if the specified player is either the victor, or is on the
     * winning team.  Best to call during PHASE_VICTORY.
     */
    public abstract boolean isPlayerVictor(Player player);
    
    /**
     * Shortcut to isPlayerVictor(Player player)
     */
    public abstract boolean isPlayerVictor(int playerId);
    
    /**
     * Get all <code>Entity</code>s that pass the given selection criteria.
     *
     * @param   selector the <code>EntitySelector</code> that implements
     *          test that an entity must pass to be included.
     *          This value may be <code>null</code> (in which case all
     *          entities in the game will be returned).
     * @return  an <code>Enumeration</code> of all entities that the
     *          selector accepts.  This value will not be <code>null</code>
     *          but it may be empty.
     */
    public abstract Enumeration getSelectedEntities(EntitySelector selector);
    
    /**
     * Count all <code>Entity</code>s that pass the given selection criteria.
     *
     * @param   selector the <code>EntitySelector</code> that implements
     *          test that an entity must pass to be included.
     *          This value may be <code>null</code> (in which case the
     *          count of all entities in the game will be returned).
     * @return  the <code>int</code> count of all entities that the
     *          selector accepts.  This value will not be <code>null</code>
     *          but it may be empty.
     */
    public abstract int getSelectedEntityCount(EntitySelector selector);
    
    /**
     * Get all out-of-game <code>Entity</code>s that pass the given selection
     * criteria.
     *
     * @param   selector the <code>EntitySelector</code> that implements
     *          test that an entity must pass to be included.
     *          This value may be <code>null</code> (in which case all
     *          entities in the game will be returned).
     * @return  an <code>Enumeration</code> of all entities that the
     *          selector accepts.  This value will not be <code>null</code>
     *          but it may be empty.
     */
    public abstract Enumeration getSelectedOutOfGameEntities(EntitySelector selector);
    
    /**
     * Count all out-of-game<code>Entity</code>s that pass the given selection
     * criteria.
     *
     * @param   selector the <code>EntitySelector</code> that implements
     *          test that an entity must pass to be included.
     *          This value may be <code>null</code> (in which case the
     *          count of all out-of-game entities will be returned).
     * @return  the <code>int</code> count of all entities that the
     *          selector accepts.  This value will not be <code>null</code>
     *          but it may be empty.
     */
    public abstract int getSelectedOutOfGameEntityCount(EntitySelector selector);
    
    /**
     * Returns true if the player has any valid units this turn that
     * are not infantry, not protomechs, or not either of those.  This
     * method is utitilized by the "A players Infantry moves after
     * that players other units", and "A players Protomechs move after
     * that players other units" options.
     */
    public abstract boolean checkForValidNonInfantryAndOrProtomechs(int playerId);
    
    /**
     * Get Entities that have have a iNarc Nemesis pod attached and are situated
     * between two Coords
     *  
     * @param attacker The attacking <code>Entity</code>.
     * @param target The <code>Coords</code> of the original target.
     * @return a <code>Enumeration</code> of entities that have nemesis pods
     *         attached and are located between attacker and target and are
     *         friendly with the attacker.
     */
    public abstract Enumeration getNemesisTargets(Entity attacker, Coords target);
}