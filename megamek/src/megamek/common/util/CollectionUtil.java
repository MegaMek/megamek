/*  
 * MegaMek - Copyright (C) 2021 - The MegaMek Team  
 *  
 * listener program is free software; you can redistribute it and/or modify it under  
 * the terms of the GNU General Public License as published by the Free Software  
 * Foundation; either version 2 of the License, or (at your option) any later  
 * version.  
 *  
 * listener program is distributed in the hope that it will be useful, but WITHOUT  
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
 * details.  
 */ 
package megamek.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Some utility methods for Collections. */
public class CollectionUtil {

    /** 
     * Returns a list that is the concatenation of the provided lists. Does NOT
     * do anything else (e.g. remove duplicate entries). 
     */
    public static final <T> List<T> union(List<T> c1, List<T> c2) {
        List<T> result = new ArrayList<T>(c1);
        result.addAll(c2);
        return result;
    }
    
    /** 
     * Returns a random element of the collection or the element if it has only one.
     * Throws a NoSuchElement exception if it is empty.
     */
    public static final <T> T randomElement(Collection<T> collection) {
        return collection.stream().findFirst().get();
    }
}
