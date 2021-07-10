/*
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

import megamek.common.annotations.Nullable;
import megamek.common.util.StringUtil;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * AbstractDirectory is a class that is used to
 */
public abstract class AbstractDirectory {
    //region Variable Declarations
    private String rootName; // The root category name

    /**
     * A map of the category names to the sub-categories. Please note that this
     * map includes the root category, if the root category contains any items.
     */
    protected TreeMap<String, AbstractDirectory> categories = new TreeMap<>(StringUtil.stringComparator());

    /**
     * A map of item names to the <code>ItemFile</code>s in the root
     * category
     */
    protected TreeMap<String, Object> items = new TreeMap<>(StringUtil.stringComparator());
    //endregion Variable Declarations

    //region Constructors
    /**
     * @param file the directory file, included to ensure that is it not null
     * @param rootName the root directory name
     * @param itemFileFactory this is included to ensure that it is not null, as that is required
     *                        for the files to be processed
     */
    protected AbstractDirectory(File file, String rootName, ItemFileFactory itemFileFactory) {
        // Validate input.
        assert file != null : "A null root directory was passed.";
        assert itemFileFactory != null : "A null item factory was passed.";

        this.rootName = (rootName == null) ? "" : rootName;
    }
    //endregion Constructors

    //region Getters/Setters
    public String getRootName() {
        return rootName;
    }

    public void setRootName(String rootName) {
        this.rootName = rootName;
    }

    public TreeMap<String, AbstractDirectory> getCategories() {
        return categories;
    }

    public @Nullable AbstractDirectory getCategory(final String categoryName) {
        return getCategories().get(categoryName);
    }

    /**
     * Get the names of all the categories.
     *
     * @return an <code>Enumeration</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    public Iterator<String> getCategoryNames() {
        return categories.keySet().iterator();
    }

    /**
     * Helper function to file away new categories. It adds one entry in the map
     * for each sub-category in the passed category.
     *
     * @param category - the <code>AbstractDirectory</code> files.
     */
    protected void addCategory(AbstractDirectory category) {
        Iterator<String> names = category.getCategoryNames();
        while (names.hasNext()) {
            categories.put(names.next(), category);
        }
    }

    /**
     * @return a TreeMap of all the Objects within the Directory
     */
    public TreeMap<String, Object> getItems(){
        return items;
    }

    /**
     * Get the names of all the items in one of the categories.
     *
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required.
     * @return an <code>Iterator</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    public Iterator<String> getItemNames(String categoryName) {
        // Get the category with the given name.
        AbstractDirectory category = categories.get(categoryName);

        if (category == null) { // ensure the category exists first
            // Return an empty Iterator if we couldn't find the category
            return Collections.emptyIterator();
        } else if (!this.equals(category)) { // then check if it is a subcategory
            // Yup. Pass the request on.
            return category.getItemNames(categoryName);
        }

        // Return the names of this directory's items.
        return items.keySet().iterator();
    }

    /**
     * Get the indicated item from the correct category.
     *
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required. This value may be
     *            <code>null</code>.
     * @param itemName - the <code>String</code> name of the indicated item.
     * @return the <code>Object<code> in the given category with the given
     *          name. This value may be <code>null</code>.
     * @throws Exception if there's any error getting the item.
     */
    public Object getItem(String categoryName, String itemName) throws Exception {
        // Get the category with the given name.
        AbstractDirectory category = categories.get(categoryName);

        if (category == null) { // ensure the category exists first
            return null; // return null if that is the case
        } else if (!this.equals(category)) { // then check if it is a subcategory
            // Yup. Pass the request on.
            return category.getItem(categoryName, itemName);
        }

        // Find the named entry.
        ItemFile entry = (ItemFile) items.get(itemName);

        // Return the item.
        return (entry == null) ? null : entry.getItem();
    }
    //endregion Getters/Setters

    //region Utility Methods
    /**
     * Identify when a name belongs to a ZIP or JAR file (both are processed as being equal)
     *
     * @param name - the <code>String</code> which may be a ZIP file name. This value must not
     *             be <code>null</code>.
     * @return <code>true</code> if the name is for a ZIP file.
     *         <code>false</code> if the name is not for a ZIP file.
     */
    public boolean isZipName(String name) {
        // Convert the file name to upper case, and compare it to image
        // file extensions. Yeah, it's a bit of a hack, but whatever.
        String ucName = name.toUpperCase();
        return (ucName.endsWith("ZIP") || ucName.endsWith("JAR"));
    }
    //endregion Utility Methods
}