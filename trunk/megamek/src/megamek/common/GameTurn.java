/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * GameTurn.java
 *
 * Created on September 6, 2002, 11:52 AM
 */

package megamek.common;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * Represents a single turn within a phase of the game, where a specific player
 * has to declare his/her action.  The default game turn allows a player to
 * move any entity.
 *
 * @author  Ben
 */
public class GameTurn implements Serializable {

    private int playerId;
    
    /** Creates a new instance of GameTurn */
    public GameTurn(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerNum() {
        return playerId;
    }
    
    public void setPlayerNum(int playerId) {
        this.playerId = playerId;
    }

    /**
     * Determine if the specified entity is a valid one to use for this turn.
     *
     * @param   entity the <code>Entity</code> that may take this turn.
     * @param   game the <code>IGame</code> this turn belongs to.
     * @return  <code>true</code> if the specified entity can take this turn.
     *          <code>false</code> if the entity is not valid for this turn.
     */
    public boolean isValidEntity(Entity entity, IGame game) {
        return entity != null
            && entity.getOwnerId() == playerId
            && entity.isSelectableThisTurn()
            //This next bit enforces the "A players Infantry/Protos
            // move after that players other units" options.
            && !( game.getPhase() == Game.PHASE_MOVEMENT
                  && ( (entity instanceof Infantry &&
                        game.getOptions().booleanOption("inf_move_later")) ||
                       (entity instanceof Protomech &&
                        game.getOptions().booleanOption("protos_move_later")))
                  && game.checkForValidNonInfantryAndOrProtomechs(playerId));
    }

    /**
     * Returns true if the player and entity are both valid.
     */
    public boolean isValid(int playerId, Entity entity, IGame game) {
        return playerId == this.playerId && isValidEntity(entity, game);
    }
    
    /**
     * Returns true if the player is valid.
     */
    public boolean isValid(int playerId, IGame game) {
        return playerId == this.playerId;
    }
    
    public String toString() {
        return getClass().getName() + " [" + playerId + "]";
    }

    /**
     * A type of game turn that allows only one specific entity to move.
     */
    public static class SpecificEntityTurn extends GameTurn {
        private int entityId;
        
        public SpecificEntityTurn(int playerId, int entityId) {
            super(playerId);
            this.entityId = entityId;
        }
        
        public int getEntityNum() {
            return entityId;
        }
        
        public void setEntityNum(int entityId) {
            this.entityId = entityId;
        }
        
        /**
         * Returns true if the entity is normally valid and it is the specific
         * entity that can move this turn.
         */
        public boolean isValidEntity(Entity entity, Game game) {
            return super.isValidEntity(entity, game) && entity.getId() == entityId;
        }
    }

    /**
     * A type of game turn that allows only one specific entity to 
     * trigger their Anti-Personell pods against attacking infantry.
     */
    public static class TriggerAPPodTurn extends SpecificEntityTurn {
        
        public TriggerAPPodTurn(int playerId, int entityId) {
            super(playerId, entityId);
        }

        /**
         * Returns true if the entity matches this game turn, even if the
         * entity has declared an action.
         */
        public boolean isValidEntity(Entity entity, Game game) {
            final boolean oldDone = entity.done;
            entity.done = false;
            final boolean result = super.isValidEntity( entity, game );
            entity.done = oldDone;
            return result;
        }
    }

    /** The constant to represent Infantry (and Battle Armor) entities. */
    public static final int CLASS_INFANTRY      = 1;

    /** The constant to represent Protomech entities. */
    public static final int CLASS_PROTOMECH     = 2;

    /** The constant to represent Tank entities. */
    public static final int CLASS_TANK          = 4;

    /** The constant to represent Mech entities. */
    public static final int CLASS_MECH          = 8;

    /** The constant to represent Gun Emplacement entities. */
    public static final int CLASS_EMPLACEMENT   = 16;

    /**
     * Get the class code for the given entity.
     *
     * @param   entity the <code>Entity</code> whose class code is needed.
     * @return  the <code>int</code> code for the entity's class.
     */
    public static int getClassCode( Entity entity ) {
        int classCode = 0;
        if ( entity instanceof Infantry ) {
            classCode = GameTurn.CLASS_INFANTRY;
        }
        else if ( entity instanceof Protomech ) {
            classCode = GameTurn.CLASS_PROTOMECH;
        }
        else if ( entity instanceof Tank ) {
            classCode = GameTurn.CLASS_TANK;
        }
        else if ( entity instanceof Mech ) {
            classCode = GameTurn.CLASS_MECH;
        }
        return classCode;
    }

    /**
     * A type of game turn that allows only certain types of units to move.
     */
    public static class EntityClassTurn extends GameTurn {
        private final int mask;

        /**
         * Only allow entities for the given player which have types in
         * the class mask to move.
         *
         * @param   playerId the <code>int</code> ID of the player
         * @param   classMask the <code>int</code> bitmask containing
         *          all the valid class types for this move.
         */
        public EntityClassTurn( int playerId, int classMask ) {
            super( playerId );
            this.mask = classMask;
        }

        /**
         * Determine if the given entity is a valid one to use for this turn.
         *
         * @param   entity the <code>Entity</code> being tested for the move.
         * @param   game the <code>Game</code> the entity belongs to
         * @return  <code>true</code> if the entity can be moved.
         */
        public boolean isValidEntity( Entity entity, Game game ) {
            // The entity must be in the mask, and pass
            // the requirements of the parent class.
            return (GameTurn.getClassCode(entity) & this.mask) != 0 &&
                super.isValidEntity(entity, game);
        }

        /**
         * Determine if entities of the given class get to move.
         *
         * @param   classCode the <code>int</code> class code being tested
         * @return  <code>true</code> if entities of that class can move.
         */
        public boolean isValidClass( int classCode ) {
            return (classCode & this.mask) != 0;
        }
    }

    /**
     * A type of game turn that indicates that one or more players should
     * be given the opportunity to unload entities that are stranded on
     * immobile transports.  Each player declares which stranded units they
     * will unload at the beginning of the movement phase, without being
     * told what stranded units their opponent(s) are unloading.
     * <p/>
     * According to <a href="http://www.classicbattletech.com/w3t/showflat.php?Cat=&Board=ask&Number=555466&page=2&view=collapsed&sb=5&o=0&fpart=">
     * Randall Bills</a>, the "minimum move" rule allow stranded units to
     * dismount at the start of the turn.
     */
    public static class UnloadStrandedTurn extends GameTurn {
        private int[] entityIds = null;

        /**
         * Any player that owns an entity whose ID is in the passed array 
         * should be given a chance to unload it.
         *
         * @param   ids the array of <code>int</code> IDs of stranded entities.
         *          This value must not be <code>null</code> or empty.
         * @exception <code>IllegalArgumentException</code> if a
         *          <code>null</code> or empty value is passed for ids.
         */
        public UnloadStrandedTurn( int[] ids ) {
            super( Player.PLAYER_NONE );

            // Validate input.
            if ( null == ids ) {
                throw new IllegalArgumentException
                    ( "the passed array of ids is null" );
            }
            if ( 0 == ids.length ) {
                throw new IllegalArgumentException
                    ( "the passed array of ids is empty" );
            }

            // Create a copy of the array to prevent any post-call shenanigans.
            this.entityIds = new int[ids.length];
            System.arraycopy( ids, 0, this.entityIds, 0, ids.length );
        }

        /**
         * Any player that owns an entity in the passed enumeration
         * should be given a chance to unload it.
         *
         * @param   entities the <code>Enumeration</code> of stranded entities.
         *          This value must not be <code>null</code> or empty.
         * @exception <code>IllegalArgumentException</code> if a
         *          <code>null</code> or empty value is passed for entities.
         */
        public UnloadStrandedTurn( Enumeration entities ) {
            super( Player.PLAYER_NONE );

            // Validate input.
            if ( null == entities ) {
                throw new IllegalArgumentException
                    ( "the passed enumeration of entities is null" );
            }
            if ( !entities.hasMoreElements() ) {
                throw new IllegalArgumentException
                    ( "the passed enumeration of entities is empty" );
            }

            // Get the first entity.
            Entity entity = (Entity) entities.nextElement();

            // Do we need to get more entities?
            if ( entities.hasMoreElements() ) {

                // It's a bit of a hack, but get the Game from the first
                // entity, and create a temporary array that can hold the
                // IDs of every entity in the game.
                int[] ids = new int[ entity.game.getNoOfEntities() ];
                int length = 0;

                // Store the first entity's ID.
                ids[length++] = entity.getId();

                // Walk the list of remaining stranded entities.
                while ( entities.hasMoreElements() ) {
                    ids[length++] =
                        ( (Entity) entities.nextElement() ).getId();
                }

                // Create an array that just holds the stranded entity ids.
                this.entityIds = new int[length];
                System.arraycopy( ids, 0, this.entityIds, 0, length );

            } // End have-more-stranded-entities
            else {
                // There was only one stranded entity.
                this.entityIds = new int[1];
                this.entityIds[0] = entity.getId();
            }
        }

        /**
         * Determine if the given entity is a valid one to use for this turn.
         *
         * @param   entity the <code>Entity</code> being tested for the move.
         * @param   game the <code>Game</code> the entity belongs to
         * @return  <code>true</code> if the entity can be moved.
         */
        public boolean isValidEntity( Entity entity, Game game ) {
            boolean retVal = false;
            // Null entities don't need to be checked.
            if ( null != entity ) {

                // Any entity in the array is valid.
                // N.B. Stop looking after we've found the match.
                final int entityId = entity.getId();
                for ( int index = 0;
                      index < this.entityIds.length && !retVal;
                      index++ ) {
                    if ( entityId == this.entityIds[index] ) {
                        retVal = true;
                    }
                }

            } // End entity-isn't-null

            return retVal;
        }

        /**
         * Returns true if the player and entity are both valid.
         */
        public boolean isValid(int playerId, Entity entity, Game game) {
            return ( null != entity &&
                     entity.getOwnerId() == playerId &&
                     isValidEntity(entity, game) );
        }

        /**
         * Returns true if the player is valid.
         */
        public boolean isValid(int playerId, Game game) {
            boolean retVal = false;
            for ( int index = 0;
                  index < this.entityIds.length && !retVal;
                  index++ ) {
                if ( game.getEntity( this.entityIds[index] ) != null &&
                     playerId ==
                     game.getEntity( this.entityIds[index] ).getOwnerId() ) {
                    retVal = true;
                }
            }
            return retVal;
        }

        public String toString() {
            return getClass().getName() + ", entity IDs: [" + entityIds + "]";
        }

        public int[] getEntityIds() {
            return this.entityIds;
        }
    }

    /**
     * A type of game turn that allows only entities
     * belonging to certain units to move.
     */
    public static class UnitNumberTurn extends GameTurn {
        private final char unitNumber;

        /**
         * Only allow entities for the given player which have types in
         * the class mask to move.
         *
         * @param   playerId the <code>int</code> ID of the player
         * @param   unit the <code>int</code> unit number of the entities
         *          allowed to move.
         */
        public UnitNumberTurn( int playerId, char unit ) {
            super( playerId );
            this.unitNumber = unit;
        }

        /**
         * Returns true if the specified entity is a valid one to use for
         * this turn.
         */
        public boolean isValidEntity(Entity entity, Game game) {
            return ( super.isValidEntity( entity, game ) &&
                     this.unitNumber == entity.getUnitNumber() );
        }
    }

}
