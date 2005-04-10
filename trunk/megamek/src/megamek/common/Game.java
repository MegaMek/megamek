/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

/* Do not use the Sun collections (com.sun.java.util.collections.*) framework
 * in this class until Java 1.1 compatibility is abandoned or a
 * non-serialization based save feature is implemented.
 */
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

import java.io.*;

import megamek.common.actions.*;
import megamek.common.options.GameOptions;

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
    public static final int PHASE_DEPLOYMENT        = 4;
    public static final int PHASE_INITIATIVE        = 5;
    public static final int PHASE_TARGETING         = 6; 
    public static final int PHASE_MOVEMENT          = 7; 
    public static final int PHASE_MOVEMENT_REPORT   = 8; 
    public static final int PHASE_OFFBOARD          = 9; 
    public static final int PHASE_OFFBOARD_REPORT   = 10;
    public static final int PHASE_FIRING            = 11;
    public static final int PHASE_FIRING_REPORT     = 12;
    public static final int PHASE_PHYSICAL          = 13;
    public static final int PHASE_END               = 14;
    public static final int PHASE_VICTORY           = 15;
    public static final int PHASE_DEPLOY_MINEFIELDS = 16;
    public static final int PHASE_STARTING_SCENARIO = 17;
    public static final int PHASE_SET_ARTYAUTOHITHEXES = 18;

    /**
     * Define constants to describe the condition a
     * unit was in when it wass removed from the game.
     */

    private GameOptions options = new GameOptions();

    public Board board = new Board();

    private Vector entities = new Vector();
    private Hashtable entityIds = new Hashtable();

    /** Track entities removed from the game (probably by death) */
    private Vector vOutOfGame = new Vector();

    private Vector players = new Vector();
    private Vector teams   = new Vector(); // DES

    private Hashtable playerIds = new Hashtable();

    /** have the entities been deployed? */
    private boolean deploymentComplete = false;

    /** how's the weather? */
    private int windDirection = -1;
    private int windStrength = -1;
    private String stringWindDirection;
    private String stringWindStrength;

    /** what round is it? */
    private int roundCount = 0;

    /** The current turn list */
    private Vector turnVector = new Vector();
    private int turnIndex = 0;

    /** The present phase */
    private int phase = PHASE_UNKNOWN;

    /** The past phase */
    private int lastPhase = PHASE_UNKNOWN;

    // phase state
    private Vector actions = new Vector();
    private Vector pendingCharges = new Vector();
    private Vector pilotRolls = new Vector();
    private Vector extremeGravityRolls = new Vector();
    private Vector initiativeRerollRequests = new Vector();


    // reports
    private StringBuffer roundReport = new StringBuffer();
    private StringBuffer phaseReport = new StringBuffer();

    private boolean forceVictory = false;
    private int victoryPlayerId = Player.PLAYER_NONE;
    private int victoryTeam = Player.TEAM_NONE;

    private Hashtable deploymentTable = new Hashtable();
    private int lastDeploymentRound = 0;

    // Settings for the LOS tool.
    private boolean mechInFirstHex = true;
    private boolean mechInSecondHex = true;

    private Hashtable minefields = new Hashtable();
    private Vector vibrabombs = new Vector();
    private Vector offboardArtilleryAttacks = new Vector();
    
    
    private int lastEntityId;
    
    
    /**
     * Constructor
     */
    public Game() {
        ;
    }

    // If it's a mech in the first hex used by the LOS tool
    public boolean getMechInFirst() {
      return mechInFirstHex;
    }

    // If it's a mech in the second hex used by the LOS tool
    public boolean getMechInSecond() {
      return mechInSecondHex;
    }

    // If it's a mech in the first hex used by the LOS tool
    public void setMechInFirst(boolean mech) {
      mechInFirstHex = mech;
    }

    // If it's a mech in the second hex used by the LOS tool
    public void setMechInSecond(boolean mech) {
      mechInSecondHex = mech;
    }

  public boolean containsMinefield(Coords coords) {
    return minefields.containsKey(coords);
  }

  public Vector getMinefields(Coords coords) {
      Vector mfs = (Vector) minefields.get(coords);
      if (mfs == null) {
        return new Vector();
      }
    return mfs;
  }

  public int getNbrMinefields(Coords coords) {
      Vector mfs = (Vector) minefields.get(coords);
      if (mfs == null) {
        return 0;
      }

    return mfs.size();
  }

    /**
     * Get the coordinates of all mined hexes in the game.
     *
     * @return  an <code>Enumeration</code> of the <code>Coords</code>
     *          containing minefilds.  This will not be <code>null</code>.
     */
    public Enumeration getMinedCoords() {
        return minefields.keys();
    }
    public void addMinefield(Minefield mf) {
      Vector mfs = (Vector) minefields.get(mf.getCoords());
      if (mfs == null) {
        mfs = new Vector();
        mfs.addElement(mf);
        minefields.put(mf.getCoords(), mfs);
        return;
      }
      mfs.addElement(mf);
    }

    public void removeMinefield(Minefield mf) {
      Vector mfs = (Vector) minefields.get(mf.getCoords());
      if (mfs == null) {
        return;
      }

      Enumeration e = mfs.elements();
      while (e.hasMoreElements()) {
        Minefield mftemp = (Minefield) e.nextElement();
        if (mftemp.equals(mf)) {
          mfs.removeElement(mftemp);
          break;
        }
      }
      if (mfs.isEmpty()) {
        minefields.remove(mf.getCoords());
      }
  }

  public void clearMinefields() {
    minefields.clear();
  }

    public Vector getVibrabombs() {
      return vibrabombs;
    }

    public void addVibrabomb(Minefield mf) {
      vibrabombs.addElement(mf);
    }

    public void removeVibrabomb(Minefield mf) {
      vibrabombs.removeElement(mf);
  }

    public boolean containsVibrabomb(Minefield mf) {
      return vibrabombs.contains(mf);
    }

    public GameOptions getOptions() {
        return options;
    }

    public void setOptions(GameOptions options) {
        if ( null == options ) {
            System.err.println( "Can't set the game options to null!" );
        } else {
            this.options = options;
        }
    }


    public Board getBoard() {
        return board;
    }


    /**
     * Return an enumeration of teams in the game
     */
    public Enumeration getTeams() {
        return teams.elements();
    }

    /** Return the teams vector */
    public Vector getTeamsVector() {
        return teams;
    }

    /**
     * Return a players team
     *  Note: may return null if player has no team
     */
    public Team getTeamForPlayer(Player p) {
        for (Enumeration i = teams.elements(); i.hasMoreElements();) {
            final Team team = (Team)i.nextElement();
            for (Enumeration j = team.getPlayers(); j.hasMoreElements();) {
                final Player player = (Player)j.nextElement();
                if (p == player) {
                    return team;
                }
            }
        }
        return null;
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
        if ( Player.PLAYER_NONE == id ) {
            return null;
        }
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
     * Returns the number of non-destroyed deployed entities owned
     * by the player.  Ignore offboard units and captured Mek pilots.
     */
    public int getLiveDeployedEntitiesOwnedBy(Player player) {
        int count = 0;
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( entity.getOwner().equals(player)
                 && !entity.isDestroyed()
                 && entity.isDeployed()
                 && !entity.isOffBoard()
                 && !entity.isCaptured() ) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns true if the player has a valid unit with the Tactical Genius
     *  pilot special ability.
     */
    public boolean hasTacticalGenius(Player player) {
        int count = 0;
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if (entity.getCrew().getOptions().booleanOption("tactical_genius")
                && entity.getOwner().equals(player) && !entity.isDestroyed()
                && entity.isDeployed() && !entity.getCrew().isUnconscious()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns how much higher than 50 or lower than -30
     * degrees, divided by ten, rounded up, the temperature is 
     */
    
    public int getTemperatureDifference() {
        int i = 0;
        if (getOptions().intOption("temperature") >= -30 && getOptions().intOption("temperature") <= 50 )
            return i;
        else if (getOptions().intOption("temperature") < -30) {
            do {
                i++;
            } while (getOptions().intOption("temperature") + i * 10 < -30);
            return i;
        }
        else do {
            i++;
        } while (getOptions().intOption("temperature") - i * 10 > 50);
        return i;
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
            if ( (null != otherEntity.getPosition()) &&
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
            case PHASE_SET_ARTYAUTOHITHEXES :
            case PHASE_DEPLOY_MINEFIELDS :
            case PHASE_DEPLOYMENT :
            case PHASE_MOVEMENT :
            case PHASE_FIRING :
            case PHASE_PHYSICAL :
            case PHASE_TARGETING :
            case PHASE_OFFBOARD :
                return true;
            default :
                return false;
        }
    }

    /**
     * Returns the current GameTurn object
     */
    public GameTurn getTurn() {
        if (turnIndex < 0 || turnIndex >= turnVector.size()) {
            return null;
        }
        return (GameTurn)turnVector.elementAt(turnIndex);
    }

    /** Changes to the next turn, returning it. */
    public GameTurn changeToNextTurn() {
        turnIndex++;
        return getTurn();
    }

    /** Resets the turn index to -1 (awaiting first turn) */
    public void resetTurnIndex() {
        turnIndex = -1;
    }

    /** Returns true if there is a turn after the current one */
    public boolean hasMoreTurns() {
        return turnVector.size() > (turnIndex + 1);
    }

    /** Inserts a turn that will come directly after the current one */
    public void insertNextTurn(GameTurn turn) {
        turnVector.insertElementAt(turn, turnIndex + 1);
    }

    /** Returns an Enumeration of the current turn list */
    public Enumeration getTurns() {
        return turnVector.elements();
    }

    /** Returns the current turn index */
    public int getTurnIndex() {
        return turnIndex;
    }

    /** Sets the current turn index */
    public void setTurnIndex(int turnIndex) {
        this.turnIndex = turnIndex;
    }

    /** Returns the current turn vector */
    public Vector getTurnVector() {
        return turnVector;
    }

    /** Sets the current turn vector */
    public void setTurnVector(Vector turnVector) {
        this.turnVector = turnVector;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public int getLastPhase() {
        return lastPhase;
    }

    public void setLastPhase(int lastPhase) {
        this.lastPhase = lastPhase;
    }

    public void setDeploymentComplete(boolean deploymentComplete) {
      this.deploymentComplete = deploymentComplete;
    }

    public boolean isDeploymentComplete() {
      return deploymentComplete;
    }

  /**
   * Sets up up the hashtable of who deploys when
   */
    public void setupRoundDeployment() {
      deploymentTable = new Hashtable();

      for ( int i = 0; i < entities.size(); i++ ) {
        Entity ent = (Entity)entities.elementAt(i);

        Vector roundVec = (Vector)deploymentTable.get(new Integer(ent.getDeployRound()));

        if ( null == roundVec ) {
          roundVec = new Vector();
          deploymentTable.put(new Integer(ent.getDeployRound()), roundVec);
        }

        roundVec.addElement(ent);
        lastDeploymentRound = Math.max(lastDeploymentRound, ent.getDeployRound());
      }
    }

  /**
   * Checks to see if we've past our deployment completion
   */
    public void checkForCompleteDeployment() {
      setDeploymentComplete(lastDeploymentRound < getRoundCount());
    }

   /**
    * Check to see if we should deploy this round
    */
    public boolean shouldDeployThisRound() {
      return shouldDeployForRound(getRoundCount());
    }

    public boolean shouldDeployForRound(int round) {
      Vector vec = getEntitiesToDeployForRound(round);

      return ( ((null == vec) || (vec.size() == 0)) ? false : true);
    }

    private Vector getEntitiesToDeployForRound(int round) {
      return (Vector)deploymentTable.get(new Integer(round));
    }

   /**
    * Clear this round from this list of entities to deploy
    */
    public void clearDeploymentThisRound() {
      deploymentTable.remove(new Integer(getRoundCount()));
    }

   /**
    * Returns a vector of entities that have not yet deployed
    */
    public Vector getUndeployedEntities() {
      Vector entList = new Vector();
      Enumeration iter = deploymentTable.elements();

      while ( iter.hasMoreElements() ) {
        Vector vecTemp = (Vector)iter.nextElement();

        for ( int i = 0; i < vecTemp.size(); i++ ) {
          entList.addElement(vecTemp.elementAt(i));
        }
      }

      return entList;
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
     * Returns the actual vector for the out-of-game entities
     */
    public Vector getOutOfGameEntitiesVector() {
        return vOutOfGame;
    }

    /**
     * Returns an out-of-game entity.
     *
     * @param   id the <code>int</code> ID of the out-of-game entity.
     * @return  the out-of-game <code>Entity</code> with that ID.  If no
     *          out-of-game entity has that ID, returns a <code>null</code>.
     */
    public Entity getOutOfGameEntity (int id) {
        Entity match = null;
        Enumeration iter = vOutOfGame.elements();
        while (null == match && iter.hasMoreElements()) {
            Entity entity = (Entity) iter.nextElement();
            if (id == entity.getId()) {
                match = entity;
            }
        }
        return match;
    }

    /**
     * Swap out the current list of dead (or fled) units for a new one.
     *
     * @param   vOutOfGame - the new <code>Vector</code> of dead or fled units.
     *          This value should <em>not</em> be <code>null</code>.
     * @throws  <code>IllegalArgumentException</code> if the new list is
     *          <code>null</code>.
     */
    public void setOutOfGameEntitiesVector(Vector vOutOfGame) {
        if ( null == vOutOfGame ) {
            throw new IllegalArgumentException
                ( "New out-of-game list is null." );
        }
        this.vOutOfGame = vOutOfGame;
        // Add these entities to the game.
        for (Enumeration i = vOutOfGame.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            entity.setGame(this);
        }
    }

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
     * @see     #getC3SubNetworkMembers( Entity )
     */
    public Vector getC3NetworkMembers( Entity entity ){
        Vector members = new Vector();

        // Does the unit have a C3 computer?
        if ( entity != null && (entity.hasC3() || entity.hasC3i()) ) {

            // Walk throught the entities in the game, and add all
            // members of the C3 network to the output Vector.
            Enumeration units = entities.elements();
            while ( units.hasMoreElements() ) {
                Entity unit = (Entity) units.nextElement();
                if ( entity.equals(unit) || entity.onSameC3NetworkAs(unit) ) {
                    members.addElement( unit );
                }
            }

        } // End entity-has-C3

        return members;
    }

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
     * @see     #getC3NetworkMembers( Entity )
     */
    public Vector getC3SubNetworkMembers( Entity entity ){

        // Handle null, C3i, and company commander units.
        if ( entity == null || entity.hasC3i() || entity.C3MasterIs(entity) ) {
            return getC3NetworkMembers( entity );
        }

        Vector members = new Vector();

        // Does the unit have a C3 computer?
        if ( entity.hasC3() ) {

            // Walk throught the entities in the game, and add all
            // sub-members of the C3 network to the output Vector.
            Enumeration units = entities.elements();
            while ( units.hasMoreElements() ) {
                Entity unit = (Entity) units.nextElement();
                if ( entity.equals(unit) || unit.C3MasterIs(entity) ) {
                    members.addElement( unit );
                }
            }

        } // End entity-has-C3

        return members;
    }

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
    public Hashtable getPositionMap() {
        Hashtable positionMap = new Hashtable();
        Vector atPos = null;

        // Walk through the entities in this game.
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();

            // Get the vector for this entity's position.
            final Coords coords = entity.getPosition();
            if ( coords != null ) {
                atPos = (Vector) positionMap.get( coords );

                // If this is the first entity at this position,
                // create the vector and add it to the map.
                if ( atPos == null ) {
                    atPos = new Vector();
                    positionMap.put( coords, atPos );
                }

                // Add the entity to the vector for this position.
                atPos.addElement( entity );

            }
        } // Handle the next entity.

        // Return the map.
        return positionMap;
    }

    /**
     * Returns an enumeration of salvagable entities.
     */
    public Enumeration getGraveyardEntities() {
        Vector graveyard = new Vector();

        for (Enumeration i = vOutOfGame.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( entity.getRemovalCondition() == Entity.REMOVE_SALVAGEABLE ||
                 entity.getRemovalCondition() == Entity.REMOVE_EJECTED ) {
                graveyard.addElement(entity);
            }
        }

        return graveyard.elements();
    }

    /**
     * Returns an enumeration of wrecked entities.
     */
    public Enumeration getWreckedEntities() {
        Vector wrecks = new Vector();
        for (Enumeration i = vOutOfGame.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( entity.getRemovalCondition() == Entity.REMOVE_SALVAGEABLE ||
                 entity.getRemovalCondition() == Entity.REMOVE_EJECTED ||
                 entity.getRemovalCondition() == Entity.REMOVE_DEVASTATED ) {
                wrecks.addElement(entity);
            }
        }

        return wrecks.elements();
    }

    /**
     * Returns an enumeration of entities that have retreated
     */
    public Enumeration getRetreatedEntities() {
        Vector sanctuary = new Vector();

        for (Enumeration i = vOutOfGame.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if ( entity.getRemovalCondition() == Entity.REMOVE_IN_RETREAT ||
                 entity.getRemovalCondition() == Entity.REMOVE_CAPTURED ||
                 entity.getRemovalCondition() == Entity.REMOVE_PUSHED ) {
                sanctuary.addElement(entity);
            }
        }

        return sanctuary.elements();
    }

    /**
     * Returns an enumeration of entities that were utterly destroyed
     */
    public Enumeration getDevastatedEntities() {
        Vector smithereens = new Vector();

        for (Enumeration i = vOutOfGame.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            if (entity.getRemovalCondition() == Entity.REMOVE_DEVASTATED) {
                smithereens.addElement(entity);
            }
        }

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
        case Targetable.TYPE_MINEFIELD_DELIVER :
            return new HexTarget(HexTarget.idToCoords(nID), board, nType);
        case Targetable.TYPE_BUILDING :
        case Targetable.TYPE_BLDG_IGNITE :
            return new BuildingTarget
                ( BuildingTarget.idToCoords(nID), board, nType );
        case Targetable.TYPE_MINEFIELD_CLEAR :
            return new MinefieldTarget(MinefieldTarget.idToCoords(nID), board);
        case Targetable.TYPE_HEX_ARTILLERY:
            return new HexTarget(HexTarget.idToCoords(nID), board, nType);
        case Targetable.TYPE_HEX_FASCAM:
            return new HexTarget(HexTarget.idToCoords(nID), board, nType);
        case Targetable.TYPE_HEX_INFERNO_IV:
            return new HexTarget(HexTarget.idToCoords(nID), board, nType);
        case Targetable.TYPE_HEX_VIBRABOMB_IV:
            return new HexTarget(HexTarget.idToCoords(nID), board, nType);
        case Targetable.TYPE_INARC_POD:
            return INarcPod.idToInstance( nID );
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
        
        
        if(id > lastEntityId) lastEntityId = id;
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
        
        
        if(id > lastEntityId) lastEntityId = id;
    }
    
    
    /**
     * @return int containing an unused entity id
     */
    public int getNextEntityId()
    {
      return lastEntityId + 1;
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
     */
    public void removeEntity( int id, int condition ) {
        Entity toRemove = getEntity(id);
        if (toRemove == null) {
            //This next statement has been cluttering up double-blind
            // logs for quite a while now.  I'm assuming it's no longer
            // useful.
            //System.err.println("Game#removeEntity: could not find entity to remove");
            return;
        }

        entities.removeElement(toRemove);
        entityIds.remove(new Integer(id));

        toRemove.setRemovalCondition(condition);

        // do not keep never-joined entities
        if (vOutOfGame != null && condition != Entity.REMOVE_NEVER_JOINED) {
            vOutOfGame.addElement(toRemove);
        }

        //We also need to remove it from the list of things to be deployed...
        //we might still be in this list if we never joined the game
          if ( deploymentTable.size() > 0 ) {
            Enumeration iter = deploymentTable.elements();

            while ( iter.hasMoreElements() ) {
              Vector vec = (Vector)iter.nextElement();

              for ( int i = vec.size() - 1; i >= 0; i-- ) {
                Entity en = (Entity)vec.elementAt(i);

                if ( en.getId() == id )
                  vec.removeElementAt(i);
              }
            }
          }
    }

    /**
     * Resets this game by removing all entities.
     */
    public void reset() {
        roundCount = 0;

        entities.removeAllElements();
        entityIds.clear();

        vOutOfGame.removeAllElements();

        resetActions();
        resetCharges();
        resetPSRs();
        removeMinefields();
        removeArtyAutoHitHexes();

        forceVictory = false;
        victoryPlayerId = Player.PLAYER_NONE;
        victoryTeam = Player.TEAM_NONE;
    }

    private void removeArtyAutoHitHexes() {
        Enumeration iter = getPlayers();
        while (iter.hasMoreElements()) {
            Player player = (Player) iter.nextElement();
            player.removeArtyAutoHitHexes();
        }
    }
    
    
    private void removeMinefields() {
        minefields.clear();
        vibrabombs.removeAllElements();

        Enumeration iter = getPlayers();
        while (iter.hasMoreElements()) {
            Player player = (Player) iter.nextElement();
            player.removeMinefields();
        }
    }

    /**
     * Regenerates the entities by id hashtable by going thru all entities
     * in the Vector
     */
    private void reindexEntities() {
        entityIds.clear();
        lastEntityId = 0;
        if ( entities != null ) {
            // Add these entities to the game.
            for (Enumeration i = entities.elements(); i.hasMoreElements();) {
                final Entity entity = (Entity)i.nextElement();
                final int id = entity.getId();
                entityIds.put(new Integer(id), entity);
                entity.setGame(this);
                
                
                if(id > lastEntityId) lastEntityId = id;
            }
        }
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
     * Returns the first enemy entity at the given coordinate, if any.
     * Only returns targetable (non-dead) entities.
     *
     * @param c the coordinates to search at
     * @param currentEntity the entity that is firing
     */
    public Entity getFirstEnemyEntity(Coords c, Entity currentEntity) {
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (c.equals(entity.getPosition()) && entity.isTargetable() && entity.isEnemyOf(currentEntity)) {
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

        // Only build the list if the coords are on the board.
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
     * Returns a Target for an Accidental Fall From above, or null if no
     * possible target is there
     * @param c The <code>Coords</code> of the hex in which the accidental fall
     *          from above happens
     * @return  The <code>Entity</code> that should be an AFFA target.
     */
    public Entity getAffaTarget(Coords c) {
        Vector vector = new Vector();
        if ( this.board.contains(c) ) {
            for (Enumeration i = entities.elements(); i.hasMoreElements();) {
                final Entity entity = (Entity)i.nextElement();
                if (c.equals(entity.getPosition()) && entity.isTargetable() &&
                    !(entity instanceof Infantry)) {
                    vector.addElement(entity);
                }
            }
        }
        if (!vector.isEmpty()) {
            int count = vector.size();
            int random = Compute.randomInt(count);
            return (Entity)vector.get(random);
        } else {
            return null;
        }
    }
    
    /**
     * Returns an <code>Enumeration</code> of the enemy's active
     * entities at the given coordinates.
     *
     * @param   c the <code>Coords</code> of the hex being examined.
     * @param   currentEntity the <code>Entity</code> whose enemies are needed.
     * @return  an <code>Enumeration</code> of <code>Entity</code>s at the
     *          given coordinates who are enemies of the given unit.
     */
    public Enumeration getEnemyEntities( final Coords c,
                                         final Entity currentEntity ) {
        // Use an EntitySelector to avoid walking the entities vector twice.
        return this.getSelectedEntities
            (new EntitySelector() {
                    private Coords coords = c;
                    private Entity friendly = currentEntity;
                    public boolean accept (Entity entity) {
                        if ( coords.equals(entity.getPosition())
                             && entity.isTargetable()
                             && entity.isEnemyOf(friendly) )
                            return true;
                        return false;
                    }
                });
    }

    /**
     * Moves an entity into the graveyard so it stops getting sent
     * out every phase.
     */
    public void moveToGraveyard(int id) {
        this.removeEntity( id, Entity.REMOVE_SALVAGEABLE );
    }

    /**
     * See if the <code>Entity</code> with the given ID is out of the game.
     *
     * @param id - the ID of the <code>Entity</code> to be checked.
     * @return  <code>true</code> if the <code>Entity</code> is in the
     *    graveyard, <code>false</code> otherwise.
     */
    public boolean isOutOfGame( int id ) {
        for (Enumeration i = vOutOfGame.elements(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();

            if (entity.getId() == id) {
                return true;
            }
        }

        return false;
    }

    /**
     * See if the <code>Entity</code> is out of the game.
     *
     * @param entity - the <code>Entity</code> to be checked.
     * @return  <code>true</code> if the <code>Entity</code> is in the
     *    graveyard, <code>false</code> otherwise.
     */
    public boolean isOutOfGame( Entity entity ) {
        return isOutOfGame(entity.getId());
    }

    /**
     * Returns the first entity that can act in the present turn, or null if
     * none can.
     */
    public Entity getFirstEntity() {
        return getFirstEntity(getTurn());
    }

    /**
     * Returns the first entity that can act in the specified turn, or null if
     * none can.33
     */
    public Entity getFirstEntity(GameTurn turn) {
        return getEntity(getFirstEntityNum(getTurn()));
    }

    /**
     * Returns the id of the first entity that can act in the current turn,
     * or -1 if none can.
     */
    public int getFirstEntityNum() {
        return getFirstEntityNum(getTurn());
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

            if (turn.isValidEntity(entity, this)) {
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
        return getEntity(getNextEntityNum(getTurn(), start));
    }

    public int getNextEntityNum(int start) {
        return getNextEntityNum(getTurn(), start);
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
            } else if (startPassed && turn.isValidEntity(entity, this)) {
                return entity.getId();
            }
        }
        return getFirstEntityNum(turn);
    }

    /**
     * Returns the number of the first deployable entity
     */
    public int getFirstDeployableEntityNum() {
      return getFirstDeployableEntityNum(getTurn());
    }

    public int getFirstDeployableEntityNum(GameTurn turn) {
        // Repeat the logic from getFirstEntityNum.
        if (turn == null) {
            return -1;
        }
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();

            if ( turn.isValidEntity(entity, this) &&
                 entity.shouldDeploy(getRoundCount()) ) {
                return entity.getId();
            }
        }
        return -1;
    }

    /**
     * Returns the number of the next deployable entity
     */
    public int getNextDeployableEntityNum(int entityId) {
      return getNextDeployableEntityNum(getTurn(), entityId);
    }

    public int getNextDeployableEntityNum(GameTurn turn, int start) {
        // Repeat the logic from getNextEntityNum.
        boolean startPassed = false;
        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if (entity.getId() == start) {
                startPassed = true;
            } else if ( startPassed && turn.isValidEntity(entity, this) &&
                        entity.shouldDeploy(getRoundCount()) ) {
                return entity.getId();
            }
        }
        return getFirstDeployableEntityNum(turn);
    }

    public void determineWind() {
        String[] dirNames = {"North", "Northeast", "Southeast", "South", "Southwest", "Northwest"};
        String[] strNames = {"Calm", "Light", "Moderate", "High"};

        if (windDirection == -1) {
            // Initial wind direction.  If using level 2 rules, this
            //  will be the wind direction for the whole battle.
            windDirection = Compute.d6(1)-1;
        } else if (getOptions().booleanOption("maxtech_fire")) {
            // Wind direction changes on a roll of 1 or 6
            switch (Compute.d6()) {
            case 1: //rotate clockwise
                windDirection = (windDirection + 1) % 6;
                break;
            case 6: //rotate counter-clockwise
                windDirection = (windDirection + 5) % 6;
            }
        }
        if (getOptions().booleanOption("maxtech_fire")) {
            if (windStrength == -1) {
                // Initial wind strength
                switch (Compute.d6()) {
                case 1:
                    windStrength = 0;
                    break;
                case 2:
                case 3:
                    windStrength = 1;
                    break;
                case 4:
                case 5:
                    windStrength = 2;
                    break;
                case 6:
                    windStrength = 3;
                }
            } else {
                // Wind strength changes on a roll of 1 or 6
                switch (Compute.d6()) {
                case 1: //weaker
                    if (windStrength > 0)
                        windStrength--;
                    break;
                case 6: //stronger
                    if (windStrength < 3)
                        windStrength++;
                }
            }
            stringWindStrength = strNames[windStrength];
        }

        stringWindDirection = dirNames[windDirection];
    }

    public int getWindDirection() {
        return windDirection;
    }

    public String getStringWindDirection() {
        return stringWindDirection;
    }

    public int getWindStrength() {
        return windStrength;
    }

    public String getStringWindStrength() {
        return stringWindStrength;
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
    public boolean isEntityStranded( Entity entity ) {

        // Is the entity being transported?
        final int transportId = entity.getTransportId();
        Entity transport = getEntity( transportId );
        if ( Entity.NONE != transportId && null != transport ) {

            // Can that transport unload the unit?
            if ( transport.isImmobile() || 0 == transport.getWalkMP() ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of remaining selectable infantry owned by a player.
     */
    public int getInfantryLeft(int playerId) {
        Player player = this.getPlayer( playerId );
        int remaining = 0;

        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( player.equals(entity.getOwner()) &&
                 entity.isSelectableThisTurn() &&
                 entity instanceof Infantry ) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * Returns the number of remaining selectable Protomechs owned by a player.
     */
    public int getProtomechsLeft(int playerId) {
        Player player = this.getPlayer( playerId );
        int remaining = 0;

        for (Enumeration i = entities.elements(); i.hasMoreElements();) {
            final Entity entity = (Entity)i.nextElement();
            if ( player.equals(entity.getOwner()) &&
                 entity.isSelectableThisTurn() &&
                 entity instanceof Protomech ) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * Removes the last, next turn found that the specified entity can move in.
     * Used when, say, an entity dies mid-phase.
     */
    public void removeTurnFor(Entity entity) {
        for (int i = turnVector.size() - 1; i >= turnIndex; i--) {
            GameTurn turn = (GameTurn)turnVector.elementAt(i);
            if (turn.isValidEntity(entity, this)) {
                turnVector.removeElementAt(i);
                break;
            }
        }
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
        Enumeration     misc = null;
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
                misc = unit.getMisc();
                while ( misc.hasMoreElements() ) {
                    equip = (Mounted) misc.nextElement();
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

    /** Adds the specified action to the actions list for this phase. */
    public void addAction(EntityAction ea) {
        actions.addElement(ea);
    }
    public void addArtilleryAttack(ArtilleryAttackAction aaa) {
        offboardArtilleryAttacks.addElement(aaa);
    }
    public void removeArtilleryAttack(ArtilleryAttackAction aaa) {
        offboardArtilleryAttacks.removeElement(aaa);
    }
    public Vector getArtilleryVector() {
        return offboardArtilleryAttacks;
    }
    public Enumeration getArtilleryAttacks() {
        return offboardArtilleryAttacks.elements(); //Fix?
    }
    public int getArtillerySize() {
        return offboardArtilleryAttacks.size();
    }


    /** Returns an Enumeration of actions scheduled for this phase. */
    public Enumeration getActions() {
        return actions.elements();
    }

    /** Resets the actions list. */
    public void resetActions() {
        actions.removeAllElements();
    }

    /** Removes all actions by the specified entity */
    public void removeActionsFor(int entityId) {
        // or rather, only keeps actions NOT by that entity
        Vector toKeep = new Vector(actions.size());
        for (Enumeration i = actions.elements(); i.hasMoreElements();) {
            EntityAction ea = (EntityAction)i.nextElement();
            if (ea.getEntityId() != entityId) {
                toKeep.addElement(ea);
            }
        }
        this.actions = toKeep;
    }
    
    /**
     * Remove a specified action
     * @param o The action to remove.
     */
    public void removeAction(Object o) {
        actions.removeElement(o);
    }

    public int actionsSize() {
        return actions.size();
    }

    /** Returns the actions vector.  Do not use to modify the actions;
     * I will be angry. >:[  Used for sending all actions to the client.
     */
    public Vector getActionsVector() {
        return actions;
    }

    public void addInitiativeRerollRequest(Team t) {
        initiativeRerollRequests.addElement(t);
    }

    public Vector getInitiativeRerollRequests() {
        return initiativeRerollRequests;
    }

    /** Adds a pending displacement attack to the list for this phase. */
    public void addCharge(AttackAction ea) {
        pendingCharges.addElement(ea);
    }

    /**
     * Returns an Enumeration of displacement attacks scheduled for the end
     * of the physical phase.
     */
    public Enumeration getCharges() {
        return pendingCharges.elements();
    }

    /** Resets the pending charges list. */
    public void resetCharges() {
        pendingCharges.removeAllElements();
    }

    /** Returns the charges vector.  Do not modify. >:[ Used for sending all
     * charges to the client.
     */
    public Vector getChargesVector() {
        return pendingCharges;
    }

    /** Adds a pending PSR to the list for this phase. */
    public void addPSR(PilotingRollData psr) {
        pilotRolls.addElement(psr);
    }

    /** Returns an Enumeration of pending PSRs. */
    public Enumeration getPSRs() {
        return pilotRolls.elements();
    }
    
    /** Adds a pending extreme Gravity PSR to the list for this phase. */
    public void addExtremeGravityPSR(PilotingRollData psr) {
        extremeGravityRolls.addElement(psr);
    }
    
    /** Returns an Enumeration of pending extreme GravityPSRs. */
    public Enumeration getExtremeGravityPSRs() {
        return extremeGravityRolls.elements();
    }

    /** Resets the PSR list for a given entity. */
    public void resetPSRs(Entity entity) {
        PilotingRollData roll;
        Vector rollsToRemove = new Vector();
        int i=0;

        // first, find all the rolls belonging to the target entity
        for (i=0; i < pilotRolls.size(); i++) {
            roll = (PilotingRollData)pilotRolls.elementAt(i);
            if ( roll.getEntityId()==entity.getId() ) {
               rollsToRemove.addElement(new Integer(i));
            };
        };

        // now, clear them out
        for (i=rollsToRemove.size()-1; i > -1; i--) {
            pilotRolls.removeElementAt( ((Integer)rollsToRemove.elementAt(i)).intValue() );
        };
    }

    /** Resets the extreme Gravity PSR list. */
    public void resetExtremeGravityPSRs() {
        extremeGravityRolls.removeAllElements();
    }
    
    /** Resets the extreme Gravity PSR list for a given entity. */
    public void resetExtremeGravityPSRs(Entity entity) {
        PilotingRollData roll;
        Vector rollsToRemove = new Vector();
        int i=0;

        // first, find all the rolls belonging to the target entity
        for (i=0; i < extremeGravityRolls.size(); i++) {
            roll = (PilotingRollData)extremeGravityRolls.elementAt(i);
            if ( roll.getEntityId()==entity.getId() ) {
               rollsToRemove.addElement(new Integer(i));
            };
        };

        // now, clear them out
        for (i=rollsToRemove.size()-1; i > -1; i--) {
            extremeGravityRolls.removeElementAt( ((Integer)rollsToRemove.elementAt(i)).intValue() );
        };
    }

    /** Resets the PSR list. */
    public void resetPSRs() {
        pilotRolls.removeAllElements();
    }

    /** Getter for property roundCount.
     * @return Value of property roundCount.
     */
    public int getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    /** Increments the round counter */
    public void incrementRoundCount() {
        roundCount++;
    }

    /** Getter for property forceVictory.
     * @return Value of property forceVictory.
     */
    public boolean isForceVictory() {
        return forceVictory;
    }

    /** Setter for property forceVictory.
     * @param forceVictory New value of property forceVictory.
     */
    public void setForceVictory(boolean forceVictory) {
        this.forceVictory = forceVictory;
    }

    /** Getter for property roundReport.
     * @return Value of property roundReport.
     */
    public java.lang.StringBuffer getRoundReport() {
        return roundReport;
    }

    /** Resets the round report */
    public void resetRoundReport() {
        this.roundReport = new StringBuffer();
    }

    /** Getter for property phaseReport.
     * @return Value of property phaseReport.
     */
    public java.lang.StringBuffer getPhaseReport() {
        return phaseReport;
    }

    /** Resets the round report */
    public void resetPhaseReport() {
        this.phaseReport = new StringBuffer();
    }

    /** Getter for property victoryPlayerId.
     * @return Value of property victoryPlayerId.
     */
    public int getVictoryPlayerId() {
        return victoryPlayerId;
    }

    /** Setter for property victoryPlayerId.
     * @param victoryPlayerId New value of property victoryPlayerId.
     */
    public void setVictoryPlayerId(int victoryPlayerId) {
        this.victoryPlayerId = victoryPlayerId;
    }

    /** Getter for property victoryTeam.
     * @return Value of property victoryTeam.
     */
    public int getVictoryTeam() {
        return victoryTeam;
    }

    /** Setter for property victoryTeam.
     * @param victoryTeam New value of property victoryTeam.
     */
    public void setVictoryTeam(int victoryTeam) {
        this.victoryTeam = victoryTeam;
    }

    /**
     * Returns true if the specified player is either the victor, or is on the
     * winning team.  Best to call during PHASE_VICTORY.
     */
    public boolean isPlayerVictor(Player player) {
        if (player.getTeam() == Player.TEAM_NONE) {
            return player.getId() == victoryPlayerId;
        } else {
            return player.getTeam() == victoryTeam;
        }
    }
    /** Shortcut to isPlayerVictor(Player player) */
    public boolean isPlayerVictor(int playerId) {
        return isPlayerVictor(getPlayer(playerId));
    }

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
    public Enumeration getSelectedEntities( EntitySelector selector ) {
        Enumeration retVal;

        // If no selector was supplied, return all entities.
        if ( null == selector ) {
            retVal = this.getEntities();
        }

        // Otherwise, return an anonymous Enumeration
        // that selects entities in this game.
        else {
            final EntitySelector entry = selector;
            retVal = new Enumeration() {
                    private EntitySelector selector = entry;
                    private Entity current = null;
                    private Enumeration iter = Game.this.getEntities();

                    // Do any more entities meet the selection criteria?
                    public boolean hasMoreElements() {
                        // See if we have a pre-approved entity.
                        if ( null == current ) {

                            // Find the first acceptable entity
                            while ( null == current &&
                                    iter.hasMoreElements() ) {
                                current = (Entity) iter.nextElement();
                                if ( !selector.accept( current ) ) {
                                    current = null;
                                }
                            }
                        }
                        return ( null != current );
                    }

                    // Get the next entity that meets the selection criteria.
                    public Object nextElement() {
                        // Pre-approve an entity.
                        if ( !this.hasMoreElements() ) {
                            return null;
                        }

                        // Use the pre-approved entity, and null out our reference.
                        Entity next = this.current;
                        this.current = null;
                        return next;
                    }
                };

        } // End use-selector

        // Return the selected entities.
        return retVal;

    }

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
    public int getSelectedEntityCount( EntitySelector selector ) {
        int retVal = 0;

        // If no selector was supplied, return the count of all game entities.
        if ( null == selector ) {
            retVal = this.getNoOfEntities();
        }

        // Otherwise, count the entities that meet the selection criteria.
        else {
            Enumeration iter = this.getEntities();
            while ( iter.hasMoreElements() ) {
                if ( selector.accept((Entity) iter.nextElement()) ) {
                    retVal++;
                }
            }

        } // End use-selector

        // Return the number of selected entities.
        return retVal;
    }

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
    public Enumeration getSelectedOutOfGameEntities( EntitySelector selector ) {
        Enumeration retVal;

        // If no selector was supplied, return all entities.
        if ( null == selector ) {
            retVal = Game.this.vOutOfGame.elements();
        }

        // Otherwise, return an anonymous Enumeration
        // that selects entities in this game.
        else {
            final EntitySelector entry = selector;
            retVal = new Enumeration() {
                    private EntitySelector selector = entry;
                    private Entity current = null;
                    private Enumeration iter = Game.this.vOutOfGame.elements();

                    // Do any more entities meet the selection criteria?
                    public boolean hasMoreElements() {
                        // See if we have a pre-approved entity.
                        if ( null == current ) {

                            // Find the first acceptable entity
                            while ( null == current &&
                                    iter.hasMoreElements() ) {
                                current = (Entity) iter.nextElement();
                                if ( !selector.accept( current ) ) {
                                    current = null;
                                }
                            }
                        }
                        return ( null != current );
                    }

                    // Get the next entity that meets the selection criteria.
                    public Object nextElement() {
                        // Pre-approve an entity.
                        if ( !this.hasMoreElements() ) {
                            return null;
                        }

                        // Use the pre-approved entity, and null out our reference.
                        Entity next = this.current;
                        this.current = null;
                        return next;
                    }
                };

        } // End use-selector

        // Return the selected entities.
        return retVal;

    }

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
    public int getSelectedOutOfGameEntityCount( EntitySelector selector ) {
        int retVal = 0;

        // If no selector was supplied, return the count of all game entities.
        if ( null == selector ) {
            retVal = Game.this.vOutOfGame.size();
        }

        // Otherwise, count the entities that meet the selection criteria.
        else {
            Enumeration iter = Game.this.vOutOfGame.elements();
            while ( iter.hasMoreElements() ) {
                if ( selector.accept((Entity) iter.nextElement()) ) {
                    retVal++;
                }
            }

        } // End use-selector

        // Return the number of selected entities.
        return retVal;
    }

    /** Returns true if the player has any valid units this turn that
     * are not infantry, not protomechs, or not either of those.  This
     * method is utitilized by the "A players Infantry moves after
     * that players other units", and "A players Protomechs move after
     * that players other units" options.
     */
    public boolean checkForValidNonInfantryAndOrProtomechs(int playerId) {
        Vector playerEnts = getPlayerEntities(getPlayer(playerId));
        Enumeration iter = playerEnts.elements();
        while (iter.hasMoreElements()) {
            Entity entity = (Entity) iter.nextElement();
            boolean excluded = false;
            if (entity instanceof Infantry &&
                getOptions().booleanOption("inf_move_later")) {
                excluded = true;
            } else if (entity instanceof Protomech &&
                       getOptions().booleanOption("protos_move_later")) {
                excluded = true;
            }

            if (!excluded && getTurn().isValidEntity(entity, this)) {
                return true;
            }
        }
        return false;
    }
    
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
    public Enumeration getNemesisTargets(Entity attacker, Coords target) {
        final Coords attackerPos = attacker.getPosition();
        final Coords[] in = Coords.intervening(attackerPos, target);
        Vector nemesisTargets = new Vector();
        for (int i = 0; i < in.length; i++) {
            for (Enumeration e = getEntities(in[i]);e.hasMoreElements();) {
                Entity entity = (Entity)e.nextElement();
                if (entity.isINarcedWith(INarcPod.NEMESIS) &&
                     !entity.isEnemyOf(attacker)) {
                    nemesisTargets.addElement(entity);
                }
            }
        }
        return nemesisTargets.elements();
    }
}
