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

/*
 * GameTurn.java
 *
 * Created on September 6, 2002, 11:52 AM
 */

package megamek.common;

import java.io.*;

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
     * Returns true if the specified entity is a valid one to use for this turn.
     */
    public boolean isValidEntity(Entity entity, Game game) {
        return entity != null && entity.getOwnerId() == playerId
        && entity.isSelectableThisTurn(game);
    }
    
    /**
     * Returns true if the player and entity are both valid.
     */
    public boolean isValid(int playerId, Entity entity, Game game) {
        return playerId == this.playerId && isValidEntity(entity, game);
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
    
    /**
     * A type of game turn that allows only infantry and protomechs to move
     */
    public static class OnlyInfantryAndProtomechTurn extends GameTurn {
        public OnlyInfantryAndProtomechTurn(int playerId) {
            super(playerId);
        }
        
        /**
         * Returns true if the entity is normally valid and it is infantry or protomech.
         */
        public boolean isValidEntity(Entity entity, Game game) {
            return super.isValidEntity(entity, game) && (entity instanceof Infantry || entity instanceof Protomech);
        }
    }
    
    /**
     * A type of game turn that allows anything except infantry to move
     */
    public static class NotInfantryOrProtomechTurn extends GameTurn {
        public NotInfantryOrProtomechTurn(int playerId) {
            super(playerId);
        }
        
        /**
         * Returns true if the entity is normally valid and it is not infantry or protomech.
         */
        public boolean isValidEntity(Entity entity, Game game) {
            return super.isValidEntity(entity, game) && !(entity instanceof Infantry || entity instanceof Protomech);
        }
    }
}
