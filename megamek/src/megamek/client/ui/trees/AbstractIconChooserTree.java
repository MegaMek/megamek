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
package megamek.client.ui.trees;

import java.util.Map;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.AbstractDirectory;

/**
 * AbstractIconChooserTree is an extension of JTree that provides additional AbstractIcon specific functionality to
 * simplify initialization.
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
     * @param root      the root tree node for this Tree
     * @param directory the specified directory, which may be null if there are no children to add
     *
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
     * This recursively creates a tree originating from the node of the first call, and recursively adds all child
     * categories until there are no categories left to add
     *
     * @param root      the root tree node for the specified directory
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
