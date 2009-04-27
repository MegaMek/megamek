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
 * IMechLoader.java
 *
 * Created on June 21, 2002, 11:03 AM
 */

package megamek.common.loaders;

import megamek.common.Entity;

/**
 * Classes implementing this interface are expected to be able to return a mech
 * with the getMech method or throw an EntityLoadingException. Implementing
 * classes will probably be constructed with a file, or file location as an
 * argument.
 * 
 * @author Ben
 * @version
 */
public interface IMechLoader {

    /**
     * Return a valid mech, matching the file to the best of MegaMek's current
     * capabilities, or throw an exception with a helpful message.
     */
    public Entity getEntity() throws EntityLoadingException;
}
