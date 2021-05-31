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
package megamek.client.ui.dialogs;

import megamek.client.ui.panels.PortraitChooser;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;

import javax.swing.*;

/**
 * This dialog allows players to select a portrait
 * It automatically fills itself with the portraits in the {@link Configuration#portraitImagesDir()}
 * directory tree.
 *
 * @see AbstractIconChooserDialog
 */
public class PortraitChooserDialog extends AbstractIconChooserDialog {
    //region Constructors
    public PortraitChooserDialog(final JFrame frame, final @Nullable AbstractIcon icon) {
        super(frame, "PortraitChooserDialog","PortraitChoiceDialog.select_portrait",
                new PortraitChooser(icon), true);
    }
    //endregion Constructors

    //region Getters
    @Override
    protected PortraitChooser getChooser() {
        return (PortraitChooser) super.getChooser();
    }
    //endregion Getters
}
