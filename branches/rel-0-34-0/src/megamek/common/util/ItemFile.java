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
 * This interface represents a categorizable file. Created on January 18, 2004
 * 
 * @author James Damour
 * @version 1
 */
public interface ItemFile {

    /**
     * Get the item for this file.
     * 
     * @throws <code>Exception</code> if there's any error getting the item.
     */
    public Object getItem() throws Exception;
}
