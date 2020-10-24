/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.dialog.imageChooser;


import java.util.Iterator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

public class CamoChooserTree extends AbstractIconChooserTree {
    private static final long serialVersionUID = -452869897803327464L;

    public CamoChooserTree() {
        super();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY);
        root.add(new DefaultMutableTreeNode(Camouflage.NO_CAMOUFLAGE));
        if (MMStaticDirectoryManager.getCamouflage() != null) {
            if (MMStaticDirectoryManager.getCamouflage().getItemNames("").hasNext()) {
                root.add(new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY));
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
}
