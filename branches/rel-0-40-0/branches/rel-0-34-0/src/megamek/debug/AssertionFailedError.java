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

package megamek.debug;

/**
 * Thrown when an assertion failed.
 */
public class AssertionFailedError extends Error {

    /**
     * 
     */
    private static final long serialVersionUID = -6889087511944790249L;

    public AssertionFailedError() {
    }

    public AssertionFailedError(String message) {
        super(message);
    }
}