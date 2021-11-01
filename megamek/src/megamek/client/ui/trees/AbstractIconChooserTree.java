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
    protected abstract DefaultTreeModel createTreeModel();

    protected DefaultTreeModel createTreeModel(final DefaultMutableTreeNode root,
                                               final @Nullable AbstractDirectory directory) {
        if (directory != null) {
            recursivelyAddToRoot(root, directory);
        }

        return new DefaultTreeModel(root);
    }

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
