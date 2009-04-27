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

import java.io.Serializable;

/**
 * This class implements the <code>Distractable</code> interface. It is
 * intended to be the underlying implementation for any class that implements
 * the interface. Created on February 29, 2004
 * 
 * @author James Damour
 * @version 1
 */
public class DistractableAdapter implements Distractable, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -9093831078254025400L;
    /** Current state of distraction. */
    private boolean isDistracted;

    /**
     * Create a <code>Distractable</code> object.
     */
    public DistractableAdapter() {
        this.isDistracted = false;
    }

    /**
     * Determine if the listener is currently distracted.
     * 
     * @return <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return this.isDistracted;
    }

    /**
     * Specify if the listener should be distracted.
     * 
     * @param distract <code>true</code> if the listener should ignore events
     *            <code>false</code> if the listener should pay attention
     *            again. Events that occured while the listener was distracted
     *            NOT going to be processed.
     */
    public void setIgnoringEvents(boolean distracted) {
        this.isDistracted = distracted;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
    }

}
