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
    public boolean isValidEntity(Entity entity) {
        return entity != null && entity.getOwnerId() == playerId
        && entity.isSelectable();
    }
    
    /**
     * Returns true if the player and entity are both valid.
     */
    public boolean isValid(int playerId, Entity entity) {
        return playerId == this.playerId && isValidEntity(entity);
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
        public boolean isValidEntity(Entity entity) {
            return super.isValidEntity(entity) && entity.getId() == entityId;
        }
    }
    
    /**
     * A type of game turn that allows only infantry to move
     */
    public static class OnlyInfantryTurn extends GameTurn {
        public OnlyInfantryTurn(int playerId) {
            super(playerId);
        }
        
        /**
         * Returns true if the entity is normally valid and it is infantry.
         */
        public boolean isValidEntity(Entity entity) {
            return super.isValidEntity(entity) && entity instanceof Infantry;
        }
    }
    
    /**
     * A type of game turn that allows anything except infantry to move
     */
    public static class NotInfantryTurn extends GameTurn {
        public NotInfantryTurn(int playerId) {
            super(playerId);
        }
        
        /**
         * Returns true if the entity is normally valid and it is not infantry.
         */
        public boolean isValidEntity(Entity entity) {
            return super.isValidEntity(entity) && !(entity instanceof Infantry);
        }
    }
}
