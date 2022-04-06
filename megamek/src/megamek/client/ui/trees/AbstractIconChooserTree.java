/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.trees;

import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.AbstractDirectory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.util.Map;

/**
 * AbstractIconChooserTree is an extension of JTree that provides additional AbstractIcon specific
 * functionality to simplify initialization.
 */
public abstract class AbstractIconChooserTree extends JTree {
    //region Constructors
    protected AbstractIconChooserTree() {
        this(true);
    }

    protected AbstractIconChooserTree(final boolean initialize) {
        super();
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        if (initialize) {
            setModel(createTreeModel());
        }
    }
    //endregion Constructors

    //region Initialization
    /**
     * @return the created Tree Model for this Tree
     */
    protected abstract DefaultTreeModel createTreeModel();

    /**
     * @param root the root tree node for this Tree
     * @param directory the specified directory, which may be null if there are no children to add
     * @return the created Tree Model for this Tree
     */
    protected DefaultTreeModel createTreeModel(final DefaultMutableTreeNode root,
                                               final @Nullable AbstractDirectory directory) {
        if (directory != null) {
            recursivelyAddToRoot(root, directory);
        }

        return new DefaultTreeModel(root);
    }

    /**
     * This recursively creates a tree originating from the node of the first call, and recursively
     * adds all child categories until there are no categories left to add
     * @param root the root tree node for the specified directory
     * @param directory the specified directory
     */
    private void recursivelyAddToRoot(final DefaultMutableTreeNode root,
                                      final AbstractDirectory directory) {
        for (final Map.Entry<String, AbstractDirectory> category : directory.getCategories().entrySet()) {
            final DefaultMutableTreeNode node = new DefaultMutableTreeNode(category.getKey());
            recursivelyAddToRoot(node, category.getValue());
            root.add(node);
        }
    }
    //endregion Initialization
}
