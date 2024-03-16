/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.loaders;

/**
 * This exception is thrown if a mech or other cannot be properly loaded from a
 * file due to IO errors, file format errors, or whatever.
 * 
 * @author Ben
 * @since June 21, 2002, 11:05 AM
 */
public class EntityLoadingException extends Exception {
    private static final long serialVersionUID = -4472736483205970852L;

    /**
     * Creates new <code>EntityLoadingException</code> without detail message.
     */
    public EntityLoadingException() {

    }

    /**
     * Constructs an <code>EntityLoadingException</code> with the specified
     * detail message.
     * 
     * @param msg the detail message.
     */
    public EntityLoadingException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public EntityLoadingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
