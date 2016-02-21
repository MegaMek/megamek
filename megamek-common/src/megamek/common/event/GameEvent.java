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
 * Instances of descendant classes are sent as a result of Game change
 * 
 * @see GameListener
 */
public abstract class GameEvent extends java.util.EventObject {

    /**
     * 
     */
    private static final long serialVersionUID = -6199335692173395907L;

    /**
     * Construct game event
     */
    public GameEvent(Object source) {
        super(source);
    }
    
    /**
     * Sub-classed events implement this method to call their specific method on 
     * a GameListener instance that their event has been fired.
     * @param gl GameListener recipient.
     */
    abstract public void fireEvent(GameListener gl);
    
    abstract public String getEventName(); 

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(getEventName());
        buff.append(" game event ");
        return buff.toString();
    }
}