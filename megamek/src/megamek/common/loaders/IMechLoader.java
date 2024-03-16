/*
 * Copyright (c) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.Entity;

/**
 * Classes implementing this interface are expected to be able to return a mech with the getMech
 * method or throw an EntityLoadingException. Implementing classes will probably be constructed with
 * a file, or file location as an argument.
 * 
 * @author Ben
 * @since June 21, 2002, 11:03 AM
 */
public interface IMechLoader {
    /**
     * @return A valid mech, matching the file to the best of MegaMek's current capabilities
     * @throws Exception when the file type isn't understood or the file can't be parsed.
     */
    Entity getEntity() throws Exception;
}
