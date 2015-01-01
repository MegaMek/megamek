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

package megamek.common.net;

/**
 * Instances of this class are sent when Connection established
 */
public class ConnectedEvent extends ConnectionEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -5880245401940306338L;

    /**
     * Constructs connection event
     * 
     * @param source The object on which the Event initially occurred.
     */
    public ConnectedEvent(Object source) {
        super(source, CONNECTED);
    }

}
