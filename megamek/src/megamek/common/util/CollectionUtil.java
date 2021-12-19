/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
        List<T> result = new ArrayList<>(c1);
        result.addAll(c2);
        return result;
    }
    
    /** 
     * Returns a random element of the collection or the element if it has only one.
     * Throws a NoSuchElement exception if it is empty.
     */
    public static final <T> T anyOneElement(Collection<T> collection) {
        return collection.stream().findFirst().get();
    }
    
    /** 
     * Returns the only element of the collection.
     * Throws an IllegalArgument exception if the collection size is greater than 1.
     * Throws a NoSuchElement exception if it is empty.
     */
    public static final <T> T theElement(Collection<T> collection) {
        if (collection.size() > 1) {
            throw new IllegalArgumentException("The given collection has more than one element.");
        }
        return collection.stream().findFirst().get();
    }
}
