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

package megamek.common.event;

/**
 * Instances of this class are sent when Game phase changes   
 */
public class GamePhaseChangeEvent extends GameEvent {
    
    /**
     * Old phase
     */
    private int oldPhase;
    
    /**
     * new phase
     */
    private int newPhase;

    /**
     * Constructs new <code>GamePhaseChangeEvent</code>
     * @param source Event source
     * @param oldPhase
     * @param newPhase
     */
    public GamePhaseChangeEvent(Object source, int oldPhase, int newPhase) {
        super(source, GAME_PHASE_CHANGE);
        this.oldPhase= oldPhase;
        this.newPhase = newPhase;
    }

    /**
     * Returns the newPhase.
     * @return the newPhase.
     */
    public int getNewPhase() {
        return newPhase;
    }

    /**
     * Returns the oldPhase.
     * @return the oldPhase.
     */
    public int getOldPhase() {
        return oldPhase;
    }
    
}
