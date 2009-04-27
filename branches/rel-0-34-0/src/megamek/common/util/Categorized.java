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

import java.util.Iterator;

/**
 * This interface represents a collection of items organized according to
 * categories. These categories are not necessarily hierarchical. Created on
 * January 17, 2004
 * 
 * @author James Damour
 * @version 1
 */
public interface Categorized {

    /**
     * Get the names of all the categories.
     * 
     * @return an <code>Enumeration</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    public Iterator<String> getCategoryNames();

    /**
     * Get the names of all the items in one of the categories.
     * 
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required.
     * @return an <code>Enumeration</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    public Iterator<String> getItemNames(String categoryName);

    /**
     * Get the indicated item from the correct catagory.
     * 
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required.
     * @param itemName - the <code>String</code> name of the indicated item.
     * @return the <code>Object<code> in the correct category with the given
     *          name.  This value may be <code>null</code>.
     * @throws  <code>Exception</code> if there's any error getting the item.
     */
    public Object getItem(String categoryName, String itemName)
            throws Exception;

}
