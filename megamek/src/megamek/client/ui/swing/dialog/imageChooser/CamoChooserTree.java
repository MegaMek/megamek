/* MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
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
package megamek.client.ui.swing.dialog.imageChooser;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.IPlayer;

public class CamoChooserTree extends JTree {

    private static final long serialVersionUID = -452869897803327464L;

    public CamoChooserTree() {
        super(); 

        // set up the directory tree (left panel) 
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(IPlayer.ROOT_CAMO);
        root.add(new DefaultMutableTreeNode(IPlayer.NO_CAMO));
        if (MMStaticDirectoryManager.getCamouflage() != null) {
            if (MMStaticDirectoryManager.getCamouflage().getItemNames("").hasNext()) {
                root.add(new DefaultMutableTreeNode(IPlayer.ROOT_CAMO));
            }
            Iterator<String> catNames = MMStaticDirectoryManager.getCamouflage().getCategoryNames();
            while (catNames.hasNext()) {
                String catName = catNames.next();
                if ((catName != null) && !catName.equals("")) {
                    String[] names = catName.split("/");
                    addCategoryToTree(root, names);
                }
            }
        }
        setModel(new DefaultTreeModel(root));
    }
    
    /**
     * This recursive method is a hack: DirectoryItems flattens the directory
     * structure, but it provides useful functionality, so this method will
     * reconstruct the directory structure for the JTree.
     *
     * @param node
     * @param names
     */
    private void addCategoryToTree(DefaultMutableTreeNode node, String[] names) {

        // Shouldn't happen
        if (names.length == 0) {
            return;
        }

        boolean matched = false;
        for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            String nodeName = (String) childNode.getUserObject();
            if (nodeName.equals(names[0])) {
                if (names.length > 1) {
                    addCategoryToTree(childNode,
                            Arrays.copyOfRange(names, 1, names.length));
                    matched = true;
                } else {
                    // I guess we're done? This shouldn't happen, as there
                    // shouldn't be duplicates
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

}
