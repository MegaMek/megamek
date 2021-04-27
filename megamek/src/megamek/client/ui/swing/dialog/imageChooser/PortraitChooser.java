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

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import megamek.common.util.fileUtils.DirectoryItems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("serial") // Same-version serialization only (See Swing base classes)
public class PortraitChooser extends AbstractIconChooser {
    //region Constructors
    public PortraitChooser(AbstractIcon icon) {
        super(new PortraitChooserTree(), icon);
    }
    //endregion Constructors

    @Override
    protected DirectoryItems getDirectory() {
        return MMStaticDirectoryManager.getPortraits();
    }

    @Override
    protected AbstractIcon createIcon(String category, String filename) {
        return new Portrait(category, filename);
    }

    @Override
    protected List<AbstractIcon> getItems(String category) {
        List<AbstractIcon> result = new ArrayList<>();

        // The portraits of the selected category are presented.
        // When the includeSubDirs flag is true, all categories
        // below the selected one are also presented.
        if (includeSubDirs) {
            for (Iterator<String> catNames = getDirectory().getCategoryNames(); catNames.hasNext(); ) {
                String tcat = catNames.next();
                if (tcat.startsWith(category)) {
                    addCategoryItems(tcat, result);
                }
            }
        } else {
            addCategoryItems(category, result);
        }
        return result;
    }

    /** Reloads the portrait directory from disk. */
    @Override
    protected void refreshDirectory() {
        MMStaticDirectoryManager.refreshPortraitDirectory();
        refreshDirectory(new PortraitChooserTree());
    }
}
