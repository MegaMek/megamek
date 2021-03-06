/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util.fileUtils;

import java.util.Iterator;
import java.util.TreeMap;

/**
 * This interface represents a collection of items organized according to
 * categories. These categories are not necessarily hierarchical. Created on
 * January 17, 2004
 * 
 * @author James Damour
 */
public interface Categorized {

    /**
     * Get the names of all the categories.
     * 
     * @return an <code>Enumeration</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    Iterator<String> getCategoryNames();

    /**
     * @return a TreeMap of all the Objects within the Directory
     */
    TreeMap<String, Object> getItems();

    /**
     * Get the names of all the items in one of the categories.
     * 
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required.
     * @return an <code>Enumeration</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    Iterator<String> getItemNames(String categoryName);

    /**
     * Get the indicated item from the correct category.
     * 
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required.
     * @param itemName - the <code>String</code> name of the indicated item.
     * @return the <code>Object<code> in the correct category with the given
     *          name.  This value may be <code>null</code>.
     * @throws Exception if there's any error getting the item.
     */
    Object getItem(String categoryName, String itemName) throws Exception;
}
