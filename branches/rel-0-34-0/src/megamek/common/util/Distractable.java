/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.util;

/**
 * This interface represents an event listener that can be "distracted" so as to
 * ignore any event notifications. Created on February 29, 2004
 * 
 * @author James Damour
 * @version 1
 */
public interface Distractable extends java.util.EventListener {

    /**
     * Determine if the listener is currently distracted.
     * 
     * @return <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents();

    /**
     * Specify if the listener should be distracted.
     * 
     * @param distract <code>true</code> if the listener should ignore events
     *            <code>false</code> if the listener should pay attention
     *            again. Events that occured while the listener was distracted
     *            NOT going to be processed.
     */
    public void setIgnoringEvents(boolean distracted);

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners();

}
