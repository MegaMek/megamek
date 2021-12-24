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

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * StandardForceIconChooserTree is an implementation of AbstractIconChooserTree that initializes the
 * tree using the Camouflage Directory, with the additional and external category of Colour
 * Camouflage added before any of the the Camouflage Directory categories.
 * @see AbstractIconChooserTree
 */
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
