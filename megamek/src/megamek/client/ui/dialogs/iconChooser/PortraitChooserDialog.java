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

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;

/**
 * PortraitChooserDialog is an implementation of AbstractIconChooserDialog that is used to select a Portrait from the
 * Portrait Directory.
 *
 * @see AbstractIconChooserDialog
 */
public class PortraitChooserDialog extends AbstractIconChooserDialog {
    //region Constructors
    public PortraitChooserDialog(final JFrame frame, final @Nullable AbstractIcon icon) {
        super(frame, "PortraitChooserDialog", "PortraitChoiceDialog.select_portrait",
              new PortraitChooserPanel(frame, icon), true);
    }
    //endregion Constructors

    //region Getters
    @Override
    protected PortraitChooserPanel getChooser() {
        return (PortraitChooserPanel) super.getChooser();
    }

    @Override
    public @Nullable Portrait getSelectedItem() {
        return getChooser().getSelectedItem();
    }
    //endregion Getters
}
