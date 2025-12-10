/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.util.fileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;

/**
 * AbstractDirectory is a class that is used to define a directory.
 */
public abstract class AbstractDirectory {
    //region Variable Declarations
    private String rootName; // The root category name
    private String rootPath; // The root path

    /**
     * A map of the category names to the direct sub-categories
     */
    private final Map<String, AbstractDirectory> categories = new TreeMap<>(new NaturalOrderComparator());

    /**
     * A map of item names to the <code>ItemFile</code>s in the root category
     */
    private final Map<String, ItemFile> items = new TreeMap<>(new NaturalOrderComparator());
    //endregion Variable Declarations

    //region Constructors

    /**
     * @param file            the directory file, included to ensure that is it not null
     * @param rootName        the root directory name
     * @param itemFileFactory this is included to ensure that it is not null, as that is required for the files to be
     *                        processed
     */
    protected AbstractDirectory(final File file, final @Nullable String rootName,
          final @Nullable String rootPath,
          final ItemFileFactory itemFileFactory) throws NullPointerException {
        Objects.requireNonNull(file, "A null root directory was passed.");
        Objects.requireNonNull(itemFileFactory, "A null item factory was passed.");

        setRootName((rootName == null) ? "" : rootName);
        setRootPath((rootPath == null) ? "" : rootPath);
    }
    //endregion Constructors

    //region Getters/Setters
    public String getRootName() {
        return rootName;
    }

    public void setRootName(final String rootName) {
        this.rootName = rootName;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    public Map<String, AbstractDirectory> getCategories() {
        return categories;
    }

    public @Nullable AbstractDirectory getCategory(final String categoryPath) {
        return categoryPath.isBlank() ? this : getCategory(categoryPath.split("/"), 0);
    }

    private @Nullable AbstractDirectory getCategory(final String[] categories, final int index) {
        if (categories.length == 0) {
            return null;
        } else if (index >= categories.length) {
            return getRootName().equals(categories[index - 1]) ? this : null;
        } else if (getRootName().equals(categories[index])) {
            return this;
        }

        final AbstractDirectory category = getCategories().get(categories[index]);
        return (category == null) ? null : category.getCategory(categories, index + 1);
    }

    public List<String> getNonEmptyCategoryPaths() {
        // This needs to be a list because the same name can be found twice, in different paths
        final List<String> categoryNames = new ArrayList<>();
        for (final AbstractDirectory directory : getCategories().values()) {
            categoryNames.addAll(directory.getNonEmptyCategoryPaths());
        }

        if (!getItems().isEmpty()) {
            categoryNames.add(getRootPath());
        }

        return categoryNames;
    }

    public Map<String, ItemFile> getItems() {
        return items;
    }

    /**
     * Get the names of all the items in one of the categories.
     *
     * @param categoryName the <code>String</code> name of the category whose item names are required.
     *
     * @return an <code>Iterator</code> of <code>String</code> names. This value will not be <code>null</code>, but it
     *       may be empty.
     */
    public Iterator<String> getItemNames(final @Nullable String categoryName) {
        final AbstractDirectory category = getCategory(categoryName);
        return (category == null) ? Collections.emptyIterator() : category.getItems().keySet().iterator();
    }

    /**
     * Get the indicated item from the correct category.
     *
     * @param categoryName the <code>String</code> name of the category whose item names are required
     * @param itemName     the <code>String</code> name of the indicated item
     *
     * @return the <code>Object</code> in the given category with the given name
     *
     * @throws Exception if there's any error getting the item
     */
    public @Nullable Object getItem(final @Nullable String categoryName,
          final String itemName) throws Exception {
        final AbstractDirectory category = getCategory(categoryName);
        if (category == null) {
            return null;
        }

        final ItemFile entry = category.getItems().get(itemName);
        return (entry == null) ? null : entry.getItem();
    }
    //endregion Getters/Setters

    /**
     * Adds the given AbstractDirectory's contents to this one's. Note: equally named categories are integrated into one
     * another, but for items in the exact same category (file path) and with the same name the item of other will
     * replace the item in this AbstractDirectory.
     *
     * @param other The AbstractDirectory to merge into this one
     */
    public void merge(AbstractDirectory other) {
        getItems().putAll(other.getItems());
        for (Map.Entry<String, AbstractDirectory> categoryEntry : other.getCategories().entrySet()) {
            if (categories.containsKey(categoryEntry.getKey())) {
                categories.get(categoryEntry.getKey()).merge(categoryEntry.getValue());
            } else {
                categories.put(categoryEntry.getKey(), categoryEntry.getValue());
            }
        }
    }
}
