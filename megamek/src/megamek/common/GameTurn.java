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
 * has to declare his/her action.  May also include data on a specific entity
 * that that player has to move.
 *
 * @author  Ben
 */
public class GameTurn implements Serializable {
    public final static int ENTITY_ANY = -1;
    
    private int playerNum;
    private int entityNum;
    
    /** Creates a new instance of GameTurn */
    public GameTurn(int playerNum, int entityNum) {
        this.playerNum = playerNum;
        this.entityNum = entityNum;
    }
    
    public GameTurn(int playerNum) {
        this(playerNum, ENTITY_ANY);
    }
    
    public int getPlayerNum() {
        return playerNum;
    }
    
    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }
    
    public int getEntityNum() {
        return entityNum;
    }
    
    public void setEntityNum(int entityNum) {
        this.entityNum = entityNum;
    }
}
