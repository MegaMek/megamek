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
 * LocationFullExecption.java
 *
 * Created on June 19, 2002, 7:16 PM
 */

package megamek.common;

/**
 * Thrown by Mech, if a location is too full for new equipment.
 * 
 * @author Ben
 * @version
 */
public class LocationFullException extends java.lang.Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -8220621982313473559L;

    /**
     * Creates new <code>LocationFullExecption</code> without detail message.
     */
    public LocationFullException() {
    }

    /**
     * Constructs an <code>LocationFullExecption</code> with the specified
     * detail message.
     * 
     * @param msg the detail message.
     */
    public LocationFullException(String msg) {
        super(msg);
    }
}
