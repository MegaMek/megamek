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

import megamek.MegaMek;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.util.Arrays;
import java.util.Enumeration;

public abstract class AbstractIconChooserTree extends JTree {
    //region Constructors
    protected AbstractIconChooserTree() {
        super();
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setModel(createTreeModel());
    }
    //endregion Constructors

    //region Initialization
    protected abstract DefaultTreeModel createTreeModel();

    /**
     * This recursive method is a hack: DirectoryItems flattens the directory
     * structure, but it provides useful functionality, so this method will
     * reconstruct the directory structure for the JTree.
     *
     * @param node the node to add the category beneath
     * @param names the categories to add to the tree
     */
    protected void addCategoryToTree(final DefaultMutableTreeNode node, final String... names) {
        // Shouldn't happen
        if (names.length == 0) {
            return;
        }

        boolean matched = false;
        for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
            final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            final String nodeName = (String) childNode.getUserObject();
            if (nodeName.equals(names[0])) {
                if (names.length > 1) {
                    addCategoryToTree(childNode, Arrays.copyOfRange(names, 1, names.length));
                    matched = true;
                } else {
                    // I guess we're done? This shouldn't happen, as there shouldn't be duplicates
                    MegaMek.getLogger().error("Duplicate categories found in tree");
                }
            }
        }

        // If we didn't match, lets create nodes for each name
        if (!matched) {
            DefaultMutableTreeNode root = node;
            for (String name : names) {
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(name);
                root.add(newNode);
                root = newNode;
            }
        }
    }
    //endregion Initialization
}
