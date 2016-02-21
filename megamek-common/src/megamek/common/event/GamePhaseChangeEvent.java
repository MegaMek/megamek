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

import megamek.common.IGame;

/**
 * Instances of this class are sent when Game phase changes
 */
public class GamePhaseChangeEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 5589252062756476819L;

    /**
     * Old phase
     */
    private IGame.Phase oldPhase;

    /**
     * new phase
     */
    private IGame.Phase newPhase;

    /**
     * Constructs new <code>GamePhaseChangeEvent</code>
     * 
     * @param source Event source
     * @param oldPhase
     * @param newPhase
     */
    public GamePhaseChangeEvent(Object source, IGame.Phase oldPhase, IGame.Phase newPhase) {
        super(source);
        this.oldPhase = oldPhase;
        this.newPhase = newPhase;
    }

    /**
     * Returns the newPhase.
     * 
     * @return the newPhase.
     */
    public IGame.Phase getNewPhase() {
        return newPhase;
    }

    /**
     * Returns the oldPhase.
     * 
     * @return the oldPhase.
     */
    public IGame.Phase getOldPhase() {
        return oldPhase;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gamePhaseChange(this);    
    }

    @Override
    public String getEventName() {
        return "Phase Change";
    }
}
