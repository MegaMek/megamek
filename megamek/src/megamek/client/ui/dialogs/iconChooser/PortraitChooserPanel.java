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
package megamek.client.ui.dialogs.iconChooser;

import javax.swing.JFrame;

import megamek.client.ui.panels.abstractPanels.abstractIconChooserPanel;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.trees.PortraitChooserTree;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import megamek.common.util.fileUtils.AbstractDirectory;

/**
 * PortraitChooser is an implementation of AbstractIconChooser that is used to select a Portrait from the Portrait
 * Directory.
 *
 * @see abstractIconChooserPanel
 */
public class PortraitChooserPanel extends abstractIconChooserPanel {
    //region Constructors
    public PortraitChooserPanel(final JFrame frame, final AbstractIcon icon) {
        super(frame, "PortraitChooser", new PortraitChooserTree(), icon);
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
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof Portrait) ? (Portrait) icon : null;
    }

    @Override
    public void refreshDirectory() {
        MMStaticDirectoryManager.refreshPortraitDirectory();
        refreshDirectory(new PortraitChooserTree());
    }
}
