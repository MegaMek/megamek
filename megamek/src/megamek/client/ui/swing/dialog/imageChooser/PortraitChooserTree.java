/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.dialog.imageChooser;

import java.util.Iterator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.icons.AbstractIcon;

public class PortraitChooserTree extends AbstractIconChooserTree {
    private static final long serialVersionUID = 1274949831997174959L;

    public PortraitChooserTree() {
        super();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(AbstractIcon.ROOT_CATEGORY);
        if (MMStaticDirectoryManager.getPortraits() != null) {
            Iterator<String> catNames = MMStaticDirectoryManager.getPortraits().getCategoryNames();
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
