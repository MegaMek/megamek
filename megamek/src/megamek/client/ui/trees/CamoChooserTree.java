/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020-2021 - The MegaMek Team
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
package megamek.client.ui.trees;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class CamoChooserTree extends AbstractIconChooserTree {
    //region Constructors
    public CamoChooserTree() {
        super();
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected DefaultTreeModel createTreeModel() {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY);
        root.add(new DefaultMutableTreeNode(Camouflage.COLOUR_CAMOUFLAGE));
        return createTreeModel(root, MMStaticDirectoryManager.getCamouflage());
    }
    //endregion Initialization
}
