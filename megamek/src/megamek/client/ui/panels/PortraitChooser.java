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
package megamek.client.ui.panels;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.client.ui.trees.PortraitChooserTree;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import megamek.common.util.fileUtils.AbstractDirectory;

public class PortraitChooser extends AbstractIconChooser {
    //region Constructors
    public PortraitChooser(final AbstractIcon icon) {
        super(new PortraitChooserTree(), icon);
    }
    //endregion Constructors

    @Override
    protected @Nullable AbstractDirectory getDirectory() {
        return MMStaticDirectoryManager.getPortraits();
    }

    @Override
    protected Portrait createIcon(String category, final String filename) {
        return new Portrait(category, filename);
    }

    @Override
    public @Nullable Portrait getSelectedItem() {
        return (Portrait) getImageList().getSelectedValue();
    }

    @Override
    public void refreshDirectory() {
        MMStaticDirectoryManager.refreshPortraitDirectory();
        refreshDirectory(new PortraitChooserTree());
    }
}
