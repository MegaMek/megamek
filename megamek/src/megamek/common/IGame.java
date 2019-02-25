/*
* MegaMek -
* Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AttackHandler;
import megamek.server.SmokeCloud;
import megamek.server.victory.Victory;

/**
 * This interface is the root of all data about the game in progress. Both the
 * Client and the Server should have one of these objects and it is their job to
 * keep it synched. note from itmo: this needs WAY MORE javadoc. also preferably
 * remove the abstract-modifiers and divide this interface into two
 * subinterfaces for reading and modifying. -stuff should be documented as
 * contracts. bad example of javadoccing found in getVictory* ... also phases
 * should be documented. wtf is an exchange-phase?
 */
public interface IGame {

    public static int ILLUMINATED_NONE = 0;
    public static int ILLUMINATED_FIRE = 1;
    public static int ILLUMINATED_FLARE = 2;
    public static int ILLUMINATED_LIGHT = 3;

    public enum Phase {
        PHASE_UNKNOWN,
        PHASE_LOUNGE,
        PHASE_SELECTION,
        PHASE_EXCHANGE,
        PHASE_DEPLOYMENT,
        PHASE_INITIATIVE,
        PHASE_INITIATIVE_REPORT,
        PHASE_TARGETING,
        PHASE_TARGETING_REPORT,
        PHASE_MOVEMENT,
        PHASE_MOVEMENT_REPORT,
        PHASE_OFFBOARD,
        PHASE_OFFBOARD_REPORT,
        PHASE_POINTBLANK_SHOT, // Fake phase only reached through hidden units
        PHASE_FIRING,
        PHASE_FIRING_REPORT,
        PHASE_PHYSICAL,
        PHASE_PHYSICAL_REPORT,
        PHASE_END,
        PHASE_END_REPORT,
        PHASE_VICTORY,
        PHASE_DEPLOY_MINEFIELDS,
        PHASE_STARTING_SCENARIO,
        PHASE_SET_ARTYAUTOHITHEXES;

        /**
         * @param otherPhase
         * @return
         */
        public boolean isDuringOrAfter(Phase otherPhase) {
            return compareTo(otherPhase) >= 0;
        }

        /**
         * @param otherPhase
         * @return
         */
        public boolean isBefore(Phase otherPhase) {
            return compareTo(otherPhase) < 0;
        }

        /**
         * Get the displayable name for the given Phase.
         *
         * @param phase
         * @return
         */
        static public String getDisplayableName(Phase phase) {
            return Messages.getString("GAME_" + phase.name());
        }

        /**
         * Given a displayable name for a phase, return the Phase instance for
         * that name.  Null will be returned if no match is found or a null
         * string is passed.
         *
         * @param name
         * @return
         */
        @Nullable
        static public Phase getPhaseFromName(@Nullable String name) {
            if (name == null) {
                return null;
            }

            for (Phase p : values()) {
                if (name.equals(getDisplayableName(p))) {
                    return p;
                }
            }
            return null;
        }

        /**
         * Returns true if this phase is simultaneous.
         *
         * @param game  Game instance used to get game options
         * @return
         */
        public boolean isPhaseSimultaneous(IGame game) {
            switch (this) {
                case PHASE_DEPLOYMENT:
                    return game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_DEPLOYMENT);
                case PHASE_MOVEMENT:
                    return game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_MOVEMENT);
                case PHASE_FIRING:
                    return game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_FIRING);
                case PHASE_PHYSICAL:
                    return game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_PHYSICAL);
                case PHASE_TARGETING:
                case PHASE_OFFBOARD:
                    return game.getOptions().booleanOption(OptionsConstants.INIT_SIMULTANEOUS_TARGETING);
                default:
                    return false;
            }
        }

    }

    // New accessors for external game id
    abstract int getExternalGameId();

    abstract void setExternalGameId(int value);

    /**
     * @return the currently active context-object for victorycondition
     *         checking. This should be a mutable object and it will be modified
     *         by the victory condition checkers. whoever saves the game state
     *         when doing saves, is also responsible of saving this state. at
     *         the start of the game this should be initialized to an empty
     *         hashmap
     */
    abstract HashMap<String, Object> getVictoryContext();

    /**
     * set the game victory state.
     */
    abstract void setVictoryContext(HashMap<String, Object> ctx);

    /**
     * Adds the specified game listener to receive board events from this Game.
     *
     * @param listener the game listener.
     */
    abstract void addGameListener(GameListener listener);

    /**
     * Removes the specified game listener.
     *
     * @param listener the game listener.
     */
    abstract void removeGameListener(GameListener listener);

    /**
     * Returns all the GameListeners.
     *
     * @return
     */
    abstract List<GameListener> getGameListeners();

    /**
     * purges all Game Listener objects.
     */
    abstract void purgeGameListeners();

    /**
     * Processes game events by dispatching them to any registered GameListener
     * objects.
     *
     * @param event the game event.
     */
    abstract void processGameEvent(GameEvent event);

    /**
     * Check if there is a minefield at given coords
     *
     * @param coords coords to check
     * @return <code>true</code> if there is a minefield at given coords or
     *         <code>false</code> otherwise
     */
    abstract boolean containsMinefield(Coords coords);

    /**
     * Get the minefields at specified coords
     *
     * @param coords
     * @return the <code>Vector</code> of minefields at specified coord
     */
    abstract Vector<Minefield> getMinefields(Coords coords);

    /**
     * Get the number of the minefields at specified coords
     *
     * @param coords
     * @return the number of the minefields at specified coord
     */
    abstract int getNbrMinefields(Coords coords);

    /**
     * Get the coordinates of all mined hexes in the game.
     *
     * @return an <code>Enumeration</code> of the <code>Coords</code>
     *         containing minefilds. This will not be <code>null</code>.
     */
    abstract Enumeration<Coords> getMinedCoords();

    /**
     * Addds the specified minefield
     *
     * @param mf minefield to add
     */
    abstract void addMinefield(Minefield mf);

    /**
     * Adds a number of minefields
     *
     * @param minefields the <code>Vector</code> of the minefields to add
     */
    abstract void addMinefields(Vector<Minefield> minefields);

    /**
     * Sets the minefields to the given <code>Vector</code> of the minefields
     *
     * @param minefields
     */
    abstract void setMinefields(Vector<Minefield> minefields);

    /**
     * Resets the minefield density for a given <code>Vector</code> of minefields
     * @param newMinefields
     */
    abstract void resetMinefieldDensity(Vector<Minefield> newMinefields);

    /**
     * Removes the specified minefield
     *
     * @param mf minefield to remove
     */
    abstract void removeMinefield(Minefield mf);

    /**
     * Removes all minefields
     */
    abstract void clearMinefields();

    /**
     * @return the <code>Vector</code> of the vibrabombs
     */
    abstract Vector<Minefield> getVibrabombs();

    /**
     * Addds the specified vibrabomb
     *
     * @param mf Vibrabomb to add
     */
    abstract void addVibrabomb(Minefield mf);

    /**
     * Removes the specified Vibrabomb
     *
     * @param mf Vibrabomb to remove
     */
    abstract void removeVibrabomb(Minefield mf);

    /**
     * Checks if the game contains the specified Vibrabomb
     *
     * @param mf the Vibrabomb to ceck
     * @return true iff the minefield contains a vibrabomb.
     */
    abstract boolean containsVibrabomb(Minefield mf);

    /**
     * @return game options
     */
    abstract GameOptions getOptions();

    /**
     * sets the game options
     *
     * @param options
     */
    abstract void setOptions(GameOptions options);

    /**
     * @return the game board
     */
    abstract IBoard getBoard();

    /**
     * Sets the new game board
     *
     * @param board
     */
    abstract void setBoard(IBoard board);

    /**
     * Return an enumeration of teams in the game
     */
    abstract Enumeration<Team> getTeams();

    /**
     * Return the current number of teams in the game.
     */
    abstract int getNoOfTeams();

    /**
     * Return the immutable vector of teams
     */
    abstract List<Team> getTeamsVector();

    /**
     * Return a players team Note: may return null if player has no team
     */
    abstract Team getTeamForPlayer(IPlayer p);

    /**
     * Set up the teams vector. Each player on a team (Team 1 .. Team X) is
     * placed in the appropriate vector. Any player on 'No Team', is placed in
     * their own object
     */
    abstract void setupTeams();

    /**
     * Return an enumeration of player in the game
     */
    abstract Enumeration<IPlayer> getPlayers();

    /**
     * Return the players vector
     */
    abstract Vector<IPlayer> getPlayersVector();

    /**
     * Return the current number of active players in the game.
     */
    abstract int getNoOfPlayers();

    /**
     * Returns the individual player assigned the id parameter.
     */
    abstract IPlayer getPlayer(int id);

    abstract void addPlayer(int id, IPlayer player);

    abstract void setPlayer(int id, IPlayer player);

    abstract void removePlayer(int id);

    /**
     * Returns the number of entities owned by the player, regardless of their
     * status, as long as they are in the game.
     * @param player
     */
    abstract int getEntitiesOwnedBy(IPlayer player);

    /**
     * Returns the number of entities owned by the player, regardless of their
     * status.
     * @param player
     */
    abstract int getAllEntitiesOwnedBy(IPlayer player);

    /**
     * Returns the number of non-destroyed entityes owned by the player
     * @param player
     */
    abstract int getLiveEntitiesOwnedBy(IPlayer player);

    /**
     * Returns the number of non-destroyed deployed entities owned by the
     * player. Ignore offboard units and captured Mek pilots.
     * @param player
     */
    abstract int getLiveDeployedEntitiesOwnedBy(IPlayer player);

    /**
     * Returns the number of non-destroyed deployed entities owned by the
     * player. Ignore offboard units and captured Mek pilots.
     * @param player
     */
    abstract int getLiveCommandersOwnedBy(IPlayer player);

    /**
     * Returns true if the player has a valid unit with the Tactical Genius
     * pilot special ability.
     * @param player
     */
    abstract boolean hasTacticalGenius(IPlayer player);

    /**
     * Get a vector of entity objects that are "acceptable" to attack with this
     * entity
     */
    abstract List<Entity> getValidTargets(Entity entity);

    /**
     * Returns true if this phase has turns. If false, the phase is simply
     * waiting for everybody to declare "done".
     */
    abstract boolean phaseHasTurns(IGame.Phase phase);

    /**
     * @return true if the current phase can be played simultaneously
     */
    abstract boolean isPhaseSimultaneous();

    /**
     * Returns the current GameTurn object
     */
    abstract GameTurn getTurn();

    /**
     * @return the first GameTurn object for the specified player, or null
     * if the player has no turns to play
     */
    abstract GameTurn getTurnForPlayer(int pn);

    /**
     * Changes to the next turn, returning it.
     */
    abstract GameTurn changeToNextTurn();

    /**
     * Resets the turn index to -1 (awaiting first turn)
     */
    abstract void resetTurnIndex();

    /**
     * Returns true if there is a turn after the current one
     */
    abstract boolean hasMoreTurns();

    /**
     * Inserts a turn that will come directly after the current one
     */
    abstract void insertNextTurn(GameTurn turn);

    /**
     * Inserts a turn after the specific index
     */
    abstract void insertTurnAfter(GameTurn turn, int index);

    /**
     * Swaps the turn at index 1 with the turn at index 2.
     *
     * @param index1
     * @param index2
     */
    abstract void swapTurnOrder(int index1, int index2);
    /**
     * Returns an Enumeration of the current turn list
     */
    abstract Enumeration<GameTurn> getTurns();

    /**
     * Returns the current turn index
     */
    abstract int getTurnIndex();

    /**
     * Sets the current turn index
     *
     * @param turnIndex
     *            The new turn index.
     * @param prevPlayerId
     *            The ID of the player who triggered the turn index change.
     */
    abstract void setTurnIndex(int turnIndex, int prevPlayerId);

    /**
     * Returns the current turn vector
     */
    abstract List<GameTurn> getTurnVector();

    /**
     * Sets the current turn vector
     */
    abstract void setTurnVector(List<GameTurn> turnVector);

    abstract IGame.Phase getPhase();

    abstract void setPhase(IGame.Phase phase);

    abstract IGame.Phase getLastPhase();

    abstract void setLastPhase(IGame.Phase lastPhase);

    abstract void setDeploymentComplete(boolean deploymentComplete);

    abstract boolean isDeploymentComplete();

    /**
     * Sets up up the hashtable of who deploys when
     */
    abstract void setupRoundDeployment();

    /**
     * Checks to see if we've past our deployment completion
     */
    abstract void checkForCompleteDeployment();

    /**
     * Check to see if we should deploy this round
     */
    abstract boolean shouldDeployThisRound();

    abstract boolean shouldDeployForRound(int round);

    /**
     * Clear this round from this list of entities to deploy
     */
    abstract void clearDeploymentThisRound();

    /**
     * Returns a vector of entities that have not yet deployed
     */
    abstract List<Entity> getUndeployedEntities();

    /**
     * Returns an enumeration of all the entites in the game.
     */
    abstract Iterator<Entity> getEntities();

    /**
     * Returns the actual vector for the entities
     */
    abstract List<Entity> getEntitiesVector();

    abstract void setEntitiesVector(List<Entity> entities);

    /**
     * Returns the actual vector for the out-of-game entities
     */
    abstract Vector<Entity> getOutOfGameEntitiesVector();

    /**
     * Returns an out-of-game entity.
     *
     * @param id the <code>int</code> ID of the out-of-game entity.
     * @return the out-of-game <code>Entity</code> with that ID. If no
     *         out-of-game entity has that ID, returns a <code>null</code>.
     */
    abstract Entity getOutOfGameEntity(int id);

    /**
     * Swap out the current list of dead (or fled) units for a new one.
     *
     * @param vOutOfGame - the new <code>Vector</code> of dead or fled units.
     *            This value should <em>not</em> be <code>null</code>.
     * @throw <code>IllegalArgumentException</code> if the new list is
     *        <code>null</code>.
     */
    abstract void setOutOfGameEntitiesVector(List<Entity> vOutOfGame);

    /**
     * Returns a <code>Vector</code> containing the <code>Entity</code>s
     * that are in the same C3 network as the passed-in unit. The output will
     * contain the passed-in unit, if the unit has a C3 computer. If the unit
     * has no C3 computer, the output will be empty (but it will never be
     * <code>null</code>).
     *
     * @param entity - the <code>Entity</code> whose C3 network co- members is
     *            required. This value may be <code>null</code>.
     * @return a <code>Vector</code> that will contain all other
     *         <code>Entity</code>s that are in the same C3 network as the
     *         passed-in unit. This <code>Vector</code> may be empty, but it
     *         will not be <code>null</code>.
     * @see #getC3SubNetworkMembers(Entity)
     */
    abstract Vector<Entity> getC3NetworkMembers(Entity entity);

    /**
     * Returns a <code>Vector</code> containing the <code>Entity</code>s
     * that are in the C3 sub-network under the passed-in unit. The output will
     * contain the passed-in unit, if the unit has a C3 computer. If the unit
     * has no C3 computer, the output will be empty (but it will never be
     * <code>null</code>). If the passed-in unit is a company commander or a
     * member of a C3i network, this call is the same as
     * <code>getC3NetworkMembers</code>.
     *
     * @param entity - the <code>Entity</code> whose C3 network sub- members
     *            is required. This value may be <code>null</code>.
     * @return a <code>Vector</code> that will contain all other
     *         <code>Entity</code>s that are in the same C3 network under the
     *         passed-in unit. This <code>Vector</code> may be empty, but it
     *         will not be <code>null</code>.
     * @see #getC3NetworkMembers(Entity)
     */
    abstract Vector<Entity> getC3SubNetworkMembers(Entity entity);

    /**
     * Returns a <code>Hashtable</code> that maps the <code>Coords</code> of
     * each unit in this <code>Game</code> to a <code>Vector</code> of
     * <code>Entity</code>s at that positions. Units that have no position
     * (e.g. loaded units) will not be in the map.
     *
     * @return a <code>Hashtable</code> that maps the <code>Coords</code>
     *         positions or each unit in the game to a <code>Vector</code> of
     *         <code>Entity</code>s at that position.
     */
    abstract Hashtable<Coords, Vector<Entity>> getPositionMap();

    /**
     * Returns an enumeration of salvagable entities.
     */
    abstract Enumeration<Entity> getGraveyardEntities();

    /**
     * Returns an enumeration of wrecked entities.
     */
    abstract Enumeration<Entity> getWreckedEntities();

    /**
     * Returns an enumeration of entities that have retreated
     */
    abstract Enumeration<Entity> getRetreatedEntities();

    /**
     * Returns an enumeration of entities that were utterly destroyed
     */
    abstract Enumeration<Entity> getDevastatedEntities();

    /**
     * Returns an enumeration of "carcass" entities, i.e., vehicles with dead
     * crews that are still on the map.
     */
    abstract Enumeration<Entity> getCarcassEntities();
    /**
     * Return the current number of entities in the game.
     */
    abstract int getNoOfEntities();

    /**
     * Returns the appropriate target for this game given a type and id
     */
    abstract Targetable getTarget(int nType, int nID);

    /**
     * Returns the entity with the given id number, if any.
     */
    abstract Entity getEntity(int id);

    /**
     * looks for an entity by id number even if out of the game
     */
    abstract Entity getEntityFromAllSources(int id);

    /**
     * Adds a collection of new Entities.  Only one GameEntityNewEvent is
     * created for the whole list.
     *
     * @param entities  The Entity objects to be added.
     */
    abstract void addEntities(List<Entity> entities);

    /**
     * Adds a new Entity to this Game object.
     *
     * @param entity    The Entity to add.
     * @param genEvent  A flag that determiens wheher a GameEntityNewEvent is
     *                  generated.
     */
    abstract void addEntity(Entity entity, boolean genEvent);

    /**
     * Adds a new Entity to this Game object and generates a GameEntityNewEvent.
     *
     * @param entity The Entity to add.
     **/
    void addEntity(Entity entity);

    /**
     * Adds a new Entity.  The id parameter is ignored and addEntity(Entity)
     * is called instead.  This is just to maintain compatibility with the old
     * API.
     *
     * @param id    Value that is ignored: the id is pulled from the passed
     *               Entity
     * @param entity The Entity to add to the game.
     */
    void addEntity(int id, Entity entity);

    abstract void setEntity(int id, Entity entity);

    void setEntity(int id, Entity entity, Vector<UnitLocation> movePath);

    /**
     * @return int containing an unused entity id
     */
    abstract int getNextEntityId();

    /**
     * @return <code>true</code> if an entity with the specified id number
     *         exists in this game.
     */
    abstract boolean hasEntity(int entityId);

    /**
     * Remove an entity from the master list. If we can't find that entity,
     * (probably due to double-blind) ignore it.
     */
    abstract void removeEntity(int id, int condition);

    abstract void removeEntities(List<Integer> ids, int condition);

    /**
     * Resets this game by removing all entities.
     */
    abstract void reset();

    /**
     * add a smoke cloud to the list of smoke clouds
     */
    abstract void addSmokeCloud(SmokeCloud cloud);

    /**
     * get the list of smokeclouds
     */
    abstract List<SmokeCloud> getSmokeCloudList();

    /**
     * Remove a list of smoke clouds
     * @param cloudsToRemove
     */
    abstract void removeSmokeClouds(List<SmokeCloud> cloudsToRemove);

    /**
     * Returns the first entity at the given coordinate, if any. Only returns
     * targetable (non-dead) entities.
     *
     * @param c the coordinates to search at
     */
    abstract Entity getFirstEntity(Coords c);

    /**
     * Returns the first enemy entity at the given coordinate, if any. Only
     * returns targetable (non-dead) entities.
     *
     * @param c the coordinates to search at
     * @param currentEntity the entity that is firing
     */
    abstract Entity getFirstEnemyEntity(Coords c, Entity currentEntity);

    /**
     * Returns an Enumeration of the active entities at the given coordinates.
     */
    abstract Iterator<Entity> getEntities(Coords c);

    /**
     * Returns an Enumeration of the active entities at the given coordinates.
     */
    abstract Iterator<Entity> getEntities(Coords c, boolean ignore);

    /**
     * Returns a List of the active entities at the given coordinates.
     */
    abstract List<Entity> getEntitiesVector(Coords c);

    /**
     * Returns a List of the active entities at the given coordinates.
     */
    abstract List<Entity> getEntitiesVector(Coords c, boolean ignore);

    /**
     * Returns a Vector of the gun emplacements at the given coordinates.
     */
    abstract Vector<GunEmplacement> getGunEmplacements(Coords c);

    /**
     * Determine if the given set of coordinates has a gun emplacement on the roof of a building.
     */
    abstract boolean hasRooftopGunEmplacement(Coords c);

    /**
     * Returns a Target for an Accidental Fall From above, or null if no
     * possible target is there
     *
     * @param c The <code>Coords</code> of the hex in which the accidental
     *            fall from above happens
     * @param ignore The entity who is falling, so shouldn't be returned
     * @return The <code>Entity</code> that should be an AFFA target.
     */
    abstract Entity getAffaTarget(Coords c, Entity ignore);

    /**
     * Returns an <code>Enumeration</code> of the enemy's active entities at
     * the given coordinates.
     *
     * @param c the <code>Coords</code> of the hex being examined.
     * @param currentEntity the <code>Entity</code> whose enemies are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the
     *         given coordinates who are enemies of the given unit.
     */
    abstract Iterator<Entity> getEnemyEntities(final Coords c,
            final Entity currentEntity);

    /**
     * Returns an <code>Enumeration</code> of active enemy entities
     *
     * @param currentEntity the <code>Entity</code> whose enemies are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the
     *         given coordinates who are enemies of the given unit.
     */
    abstract Iterator<Entity> getAllEnemyEntities(final Entity currentEntity);

    /**
     * Returns an <code>Enumeration</code> of friendly active entities at the
     * given coordinates.
     *
     * @param c the <code>Coords</code> of the hex being examined.
     * @param currentEntity the <code>Entity</code> whose friends are needed.
     * @return an <code>Enumeration</code> of <code>Entity</code>s at the
     *         given coordinates who are friends of the given unit.
     */
    abstract Iterator<Entity> getFriendlyEntities(final Coords c,
            final Entity currentEntity);

    /**
     * Moves an entity into the graveyard so it stops getting sent out every
     * phase.
     */
    abstract void moveToGraveyard(int id);

    /**
     * See if the <code>Entity</code> with the given ID is out of the game.
     *
     * @param id - the ID of the <code>Entity</code> to be checked.
     * @return <code>true</code> if the <code>Entity</code> is in the
     *         graveyard, <code>false</code> otherwise.
     */
    abstract boolean isOutOfGame(int id);

    /**
     * See if the <code>Entity</code> is out of the game.
     *
     * @param entity - the <code>Entity</code> to be checked.
     * @return <code>true</code> if the <code>Entity</code> is in the
     *         graveyard, <code>false</code> otherwise.
     */
    abstract boolean isOutOfGame(Entity entity);

    /**
     * Returns the first entity that can act in the present turn, or null if
     * none can.
     */
    abstract Entity getFirstEntity();

    /**
     * Returns the first entity that can act in the specified turn, or null if
     * none can.33
     */
    abstract Entity getFirstEntity(GameTurn turn);

    /**
     * Returns the id of the first entity that can act in the current turn, or
     * -1 if none can.
     */
    abstract int getFirstEntityNum();

    /**
     * Returns the id of the first entity that can act in the specified turn, or
     * -1 if none can.
     */
    abstract int getFirstEntityNum(GameTurn turn);

    /**
     * Returns the next selectable entity that can act this turn, or null if
     * none can.
     *
     * @param start
     *            the index number to start at (not an Entity Id)
     */
    abstract Entity getNextEntity(int start);

    /**
     * Returns the entity id of the next entity that can move during the
     * specified
     *
     * @param turn the turn to use
     * @param start the entity id to start at
     */
    abstract int getNextEntityNum(GameTurn turn, int start);

    /**
     * Returns the entity id of the previous entity that can move during the
     * specified
     *
     * @param turn the turn to use
     * @param start the entity id to start at
     */
    abstract int getPrevEntityNum(GameTurn turn, int start);

    /**
     * Returns the number of the first deployable entity that is valid for the specified turn
     */
    abstract int getFirstDeployableEntityNum(GameTurn turn);

    /**
     * Returns the number of the next deployable entity that is valid for the specified turn
     */
    abstract int getNextDeployableEntityNum(GameTurn turn, int start);

    /**
     * Get the entities for the player.
     *
     *
     * @param player - the <code>Player</code> whose entities are required.
     * @param hide - should fighters loaded into squadrons be excluded from this list?
     * @return a <code>Vector</code> of <code>Entity</code>s.
     */
    abstract ArrayList<Entity> getPlayerEntities(IPlayer player, boolean hide);

    /**
     * Get the entities for the player.
     *
     *
     * @param player - the <code>Player</code> whose entities are required.
     * @param hide - should fighters loaded into squadrons be excluded from this list?
     * @return a <code>Vector</code> of <code>Entity</code>s.
     */
    abstract ArrayList<Integer> getPlayerEntityIds(IPlayer player, boolean hide);

    /**
     * Determines if the indicated entity is stranded on a transport that can't
     * move. <p/> According to <a
     * href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">
     * Randall Bills</a>, the "minimum move" rule allow stranded units to
     * dismount at the start of the turn.
     *
     * @param entity the <code>Entity</code> that may be stranded
     * @return <code>true</code> if the entity is stranded <code>false</code>
     *         otherwise.
     */
    abstract boolean isEntityStranded(Entity entity);

    /**
     * Returns the number of remaining selectable infantry owned by a player.
     */
    abstract int getInfantryLeft(int playerId);

    /**
     * Returns the number of remaining selectable Protomechs owned by a player.
     */
    abstract int getProtomechsLeft(int playerId);

    /**
     * Returns the number of remaining selectable Vehicles owned by a player.
     */
    abstract int getVehiclesLeft(int playerId);

    /**
     * Returns the number of remaining selectable Mechs owned by a player.
     */
    abstract int getMechsLeft(int playerId);

    /**
     * Removes the last, next turn found that the specified entity can move in.
     * Used when, say, an entity dies mid-phase.
     */
    abstract void removeTurnFor(Entity entity);

    /**
     * Removes the first turn found that the specified entity can move in.
     * Used when a turn is played out of order
     */
    abstract GameTurn removeFirstTurnFor(Entity entity);

    /**
     * Removes any turns that can only be taken by the specified entity.  Useful
     * if the specified Entity is being removed from the game to ensure any
     * turns that only it can take are gone.
     * @param entity
     * @return The number of turns returned
     */
    abstract int removeSpecificEntityTurnsFor(Entity entity);

    /**
     * Check each player for the presence of a Battle Armor squad equipped with
     * a Magnetic Clamp. If one unit is found, update that player's units to
     * allow the squad to be transported. <p/> This method should be called
     * </b>*ONCE*</b> per game, after all units for all players have been
     * loaded.
     *
     * @return <code>true</code> if a unit was updated, <code>false</code>
     *         if no player has a Battle Armor squad equipped with a Magnetic
     *         Clamp.
     */
    /* Taharqa: I am removing this function and instead I am simply adding clamp mounts to all
     * non omni/ none BA handled mechs in the game.addEntity routine - It should not be too much memory to
     * do this and it allows us to load these units in the lobby
    abstract boolean checkForMagneticClamp();
    */

    /** Adds the specified action to the actions list for this phase. */
    abstract void addAction(EntityAction ea);

    abstract void addAttack(AttackHandler ah);

    abstract void removeAttack(AttackHandler ah);

    abstract Enumeration<AttackHandler> getAttacks();

    abstract Vector<AttackHandler> getAttacksVector();

    abstract void resetAttacks();

    int getArtillerySize();

    void setArtilleryVector(Vector<ArtilleryAttackAction> v);

    Enumeration<ArtilleryAttackAction> getArtilleryAttacks();

    // HACK.
    abstract void setAttacksVector(Vector<AttackHandler> v);

    /**
     * Returns an Enumeration of actions scheduled for this phase.
     */
    abstract Enumeration<EntityAction> getActions();

    /**
     * Resets the actions list.
     */
    abstract void resetActions();

    /**
     * Removes all actions by the specified entity
     */
    abstract void removeActionsFor(int entityId);

    /**
     * Remove a specified action
     *
     * @param o The action to remove.
     */
    abstract void removeAction(Object o);

    abstract int actionsSize();

    /**
     * Returns the actions vector. Do not use to modify the actions; I will be
     * angry. >:[ Used for sending all actions to the client.
     */
    abstract List<EntityAction> getActionsVector();

    abstract void addInitiativeRerollRequest(Team t);

    abstract void rollInitAndResolveTies();

    abstract void handleInitiativeCompensation();

    abstract int getNoOfInitiativeRerollRequests();

    /**
     * Adds a pending displacement attack to the list for this phase.
     */
    abstract void addCharge(AttackAction ea);

    /**
     * Returns an Enumeration of displacement attacks scheduled for the end of
     * the physical phase.
     */
    abstract Enumeration<AttackAction> getCharges();

    /**
     * Resets the pending charges list.
     */
    abstract void resetCharges();

    /**
     * Returns the charges vector. Do not modify. >:[ Used for sending all
     * charges to the client.
     */
    abstract List<AttackAction> getChargesVector();

    /**
     * Adds a pending ram attack to the list for this phase.
     */
    abstract void addRam(AttackAction ea);

    /**
     * Returns an Enumeration of ram attacks scheduled for the end of
     * the physical phase.
     */
    abstract Enumeration<AttackAction> getRams();

    /**
     * Resets the pending ram list.
     */
    abstract void resetRams();

    /**
     * Returns the ram vector. Do not modify. >:[ Used for sending all
     * charges to the client.
     */
    abstract List<AttackAction> getRamsVector();

    /**
     * Adds a pending tele-missile attack to the list for this phase.
     */
    abstract void addTeleMissileAttack(AttackAction ea);

    /**
     * Returns an Enumeration of telemissile attacks scheduled for the end of
     * the physical phase.
     */
    abstract Enumeration<AttackAction> getTeleMissileAttacks();

    /**
     * Resets the pending telemissile attack list.
     */
    abstract void resetTeleMissileAttacks();

    /**
     * Returns the telemissile attack vector. Do not modify. >:[ Used for sending all
     * charges to the client.
     */
    abstract List<AttackAction> getTeleMissileAttacksVector();

    /**
     * Adds a pending PSR to the list for this phase.
     */
    abstract void addPSR(PilotingRollData psr);

    /**
     * Returns an Enumeration of pending PSRs.
     */
    abstract Enumeration<PilotingRollData> getPSRs();

    /**
     * Adds a pending extreme Gravity PSR to the list for this phase.
     */
    abstract void addExtremeGravityPSR(PilotingRollData psr);

    /**
     * Returns an Enumeration of pending extreme GravityPSRs.
     */
    abstract Enumeration<PilotingRollData> getExtremeGravityPSRs();

    /**
     * Resets the PSR list for a given entity.
     */
    abstract void resetPSRs(Entity entity);

    /**
     * Resets the extreme Gravity PSR list.
     */
    abstract void resetExtremeGravityPSRs();

    /**
     * Resets the extreme Gravity PSR list for a given entity.
     */
    abstract void resetExtremeGravityPSRs(Entity entity);

    /**
     * Resets the PSR list.
     */
    abstract void resetPSRs();

    /**
     * Getter for property roundCount.
     *
     * @return Value of property roundCount.
     */
    abstract int getRoundCount();

    abstract void setRoundCount(int roundCount);

    /**
     * Increments the round counter
     */
    abstract void incrementRoundCount();

    /**
     * Getter for property forceVictory. this tells us that a claim for victory
     * is active.
     *
     * @return Value of property forceVictory.
     */
    abstract boolean isForceVictory();

    /**
     * Setter for property forceVictory.
     *
     * @param forceVictory New value of property forceVictory.
     */
    abstract void setForceVictory(boolean forceVictory);

    /**
     * Adds the given reports vector to the GameReport collection.
     *
     * @param v Vector of reports
     */
    abstract void addReports(Vector<Report> v);

    /**
     * Returns a vector of reports for the given round.
     *
     * @param r Round number
     */
    abstract Vector<Report> getReports(int r);

    /**
     * Returns a vector of all the reports.
     */
    abstract Vector<Vector<Report>> getAllReports();

    /**
     * Used to populate previous game reports, e.g. after a client connects to
     * an existing game.
     */
    void setAllReports(Vector<Vector<Report>> v);

    /**
     * Clears out all the current reports, paving the way for a new game.
     */
    void clearAllReports();

    abstract void end(int winner, int winnerTeam);

    /**
     * Getter for property victoryPlayerId. itmo: apparently this is the guy who
     * claims to have won the game also used to tell who won when the game is
     * won
     *
     * @return Value of property victoryPlayerId.
     */
    abstract int getVictoryPlayerId();

    /**
     * Setter for property victoryPlayerId.
     *
     * @param victoryPlayerId New value of property victoryPlayerId.
     */
    abstract void setVictoryPlayerId(int victoryPlayerId);

    /**
     * Getter for property victoryTeam. corresponding claiming/winning team if
     * the player is in a team
     *
     * @return Value of property victoryTeam.
     */
    abstract int getVictoryTeam();

    /**
     * Setter for property victoryTeam.
     *
     * @param victoryTeam New value of property victoryTeam.
     */
    abstract void setVictoryTeam(int victoryTeam);

    /**
     * Returns true if the specified player is either the victor, or is on the
     * winning team. Best to call during PHASE_VICTORY.
     * @param player
     */
    abstract boolean isPlayerVictor(IPlayer player);

    /**
     * Shortcut to isPlayerVictor(Player player)
     */
    abstract boolean isPlayerVictor(int playerId);

    /**
     * Get all <code>Entity</code>s that pass the given selection criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test
     *            that an entity must pass to be included. This value may be
     *            <code>null</code> (in which case all entities in the game
     *            will be returned).
     * @return an <code>Enumeration</code> of all entities that the selector
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
     */
    abstract Iterator<Entity> getSelectedEntities(
            EntitySelector selector);

    /**
     * Count all <code>Entity</code>s that pass the given selection criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test
     *            that an entity must pass to be included. This value may be
     *            <code>null</code> (in which case the count of all entities
     *            in the game will be returned).
     * @return the <code>int</code> count of all entities that the selector
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
     */
    abstract int getSelectedEntityCount(EntitySelector selector);

    /**
     * Get all out-of-game <code>Entity</code>s that pass the given selection
     * criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test
     *            that an entity must pass to be included. This value may be
     *            <code>null</code> (in which case all entities in the game
     *            will be returned).
     * @return an <code>Enumeration</code> of all entities that the selector
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
     */
    abstract Enumeration<Entity> getSelectedOutOfGameEntities(
            EntitySelector selector);

    /**
     * Count all out-of-game<code>Entity</code>s that pass the given
     * selection criteria.
     *
     * @param selector the <code>EntitySelector</code> that implements test
     *            that an entity must pass to be included. This value may be
     *            <code>null</code> (in which case the count of all
     *            out-of-game entities will be returned).
     * @return the <code>int</code> count of all entities that the selector
     *         accepts. This value will not be <code>null</code> but it may be
     *         empty.
     */
    abstract int getSelectedOutOfGameEntityCount(EntitySelector selector);

    /**
     * Returns true if the player has any valid units this turn that are not
     * infantry, not protomechs, or not either of those. This method is
     * utitilized by the "A players Infantry moves after that players other
     * units", and "A players Protomechs move after that players other units"
     * options.
     */
    abstract boolean checkForValidNonInfantryAndOrProtomechs(int playerId);

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
    abstract Enumeration<Entity> getNemesisTargets(Entity attacker,
            Coords target);

    /**
     * Returns the previous entity from the master list of entities. Will wrap
     * around to the end of the list if necessary.
     *
     * @param current The <code>Entity</code> whose list position you wish to
     *            start from.
     * @return The previous <code>Entity</code> in the list.
     */
    abstract Entity getPreviousEntityFromList(Entity current);

    /**
     * Returns the next entity from the master list of entities. Will wrap
     * around to the begining of the list if necessary.
     *
     * @param current The <code>Entity</code> whose list position you wish to
     *            start from.
     * @return The next <code>Entity</code> in the list.
     */
    abstract Entity getNextEntityFromList(Entity current);

    /**
     * Returns this turn's tag information
     */
    abstract Vector<TagInfo> getTagInfo();

    /**
     * add the results of one tag attack
     */
    abstract void addTagInfo(TagInfo info);

    /**
     * clears the "shots" attribute of all TagInfos where attacker is on same
     * team as ae and target is on same mapsheet as tc
     */
    abstract void clearTagInfoShots(Entity ae, Coords tc);


    /**
     * Computes whether two coordinates are within 8 hexes of each other
     *
     * @param c1 The first coordinate
     * @param c2 The second coordinate
     * @return True if both coordinates are within 8 hexes of each other
     */
    boolean isIn8HexRadius(Coords c1, Coords c2);


    /**
     * Reset tag information
     */
    abstract void resetTagInfo();

    /**
     * Get a list of flares
     */
    abstract Vector<Flare> getFlares();

    /**
     * Set the list of flares
     */
    abstract void setFlares(Vector<Flare> flares);

    /**
     * Add a new flare
     */
    abstract void addFlare(Flare flare);

    /**
     * Returns the level of illumination for a given coords.  Different light
     * sources affect how much the night-time penalties are reduced. Note: this
     * method should be used for determining is a Coords/Hex is illuminated, not
     * IGame. getIlluminatedPositions(), as that just returns the hexes that
     * are effected by spotlights, whereas this one considers searchlights as
     * well as other light sources.
     */
    abstract int isPositionIlluminated(Coords c);

    /**
     * Age the flare list and remove any which have burnt out Artillery flares
     * drift with wind. (called at end of turn)
     */
    abstract Vector<Report> ageFlares();

    abstract boolean gameTimerIsExpired();

    /**
     * use victoryfactory to generate a new victorycondition checker provided
     * that the victorycontext is saved properly, calling this method at any
     * time is ok and should not affect anything unless the
     * victorycondition-configoptions have changed.
     */
    abstract void createVictoryConditions();

    abstract Victory getVictory();

    abstract boolean useVectorMove();

    /**
     * Adds a pending control roll to the list for this phase.
     */
    abstract void addControlRoll(PilotingRollData control);

    /**
     * Returns an Enumeration of pending Control roll.
     */
    abstract Enumeration<PilotingRollData> getControlRolls();

    /**
     * Resets the Control Roll list for a given entity.
     */
    abstract void resetControlRolls(Entity entity);

    /**
     * Resets the Control Roll list.
     */
    abstract void resetControlRolls();

    abstract boolean checkForValidSpaceStations(int playerId);

    abstract boolean checkForValidJumpships(int playerId);

    abstract boolean checkForValidWarships(int playerId);

    abstract boolean checkForValidDropships(int playerId);

    abstract boolean checkForValidSmallCraft(int playerId);

    abstract PlanetaryConditions getPlanetaryConditions();

    abstract void setPlanetaryConditions(PlanetaryConditions conditions);

    /**
     * Get a set of Coords illuminated by searchlights.
     *
     * Note: coords could be illuminated by other sources as well, it's likely
     * that IGame.isPositionIlluminated is desired unless the searchlighted hex
     * set is being sent to the client or server.
     */
    abstract HashSet<Coords> getIlluminatedPositions();

    /**
     * Clear the set of searchlight illuminated hexes.
     */
    abstract void clearIlluminatedPositions();

    /**
     * Setter for the list of Coords illuminated by search lights.
     */
    abstract void setIlluminatedPositions(HashSet<Coords> ip);

    /**
     * Add a new hex to the collection of Coords illuminated by searchlights.
     *
     * @return True if a new hex was added, else false if the set already
     *      contained the input hex.
     */
    abstract boolean addIlluminatedPosition(Coords c);

    /**
     * Updates the map that maps a position to the list of Entity's in that
     * position.
     *
     * @param e
     */
    abstract void updateEntityPositionLookup(Entity e,
            HashSet<Coords> oldPositions);

}
