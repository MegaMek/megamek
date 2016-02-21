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
 * Instances of this class are sent when Connection closed
 */
public class DisconnectedEvent extends ConnectionEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -1427252999207396447L;

    /**
     * Constructs connection event
     * 
     * @param source The object on which the Event initially occurred.
     */
    public DisconnectedEvent(Object source) {
        super(source, DISCONNECTED);
    }

}
