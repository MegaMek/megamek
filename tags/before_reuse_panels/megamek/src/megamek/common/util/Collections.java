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

import com.sun.java.util.collections.*;
import java.util.Enumeration;

/**
 * This class consists exclusively of methods that operate on or return
 * members of the <code>com.sun.java.util.collections</code> package.
 *
 * @author      James Damour
 */
public final class Collections {

    /**
     * Get a <code>java.util.Enumeration</code> of the elements of a
     * given <code>Collection</code>.
     *
     * @param   c - the <code>Collection</code> whose elements are required.
     *          This value may be <code>null</code>.
     * @return  the <code>Enumeration</code> of the collections's elements.
     *          This value will not be <code>null</code>.
     */
    public static Enumeration elements( Collection c ) {
        // Were we passed a null?
        if ( null == c ) {
            return (new Vector()).elements();
        }
        return (new Vector(c)).elements();
    }

}
