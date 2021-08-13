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
import megamek.common.util.StringUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Iterator;

public class PortraitChooserTree extends AbstractIconChooserTree {
    //region Constructors
    public PortraitChooserTree() {
        super();
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected DefaultTreeModel createTreeModel() {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY);
        if (MMStaticDirectoryManager.getPortraits() != null) {
            final Iterator<String> categoryNames = MMStaticDirectoryManager.getPortraits().getCategoryNames();
            while (categoryNames.hasNext()) {
                final String categoryName = categoryNames.next();
                if (!StringUtil.isNullOrEmpty(categoryName)) {
                    addCategoryToTree(root, categoryName.split("/"));
                }
            }
        }
        return new DefaultTreeModel(root);
    }
    //endregion Initialization
}
