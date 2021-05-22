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

import java.awt.Window;

import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;

/**
 * This dialog allows players to select a portrait
 * It automatically fills itself with the portraits
 * in the {@link Configuration#portraitImagesDir()} directory tree.
 * Should be shown by using showDialog(). This method
 * returns either JOptionPane.OK_OPTION or .CANCEL_OPTION.
 *
 * @see AbstractIconChooserDialog
 */
public class PortraitChooserDialog extends AbstractIconChooserDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 6487684461690549139L;
    //endregion Variable Declarations

    //region Constructors
    /** Creates a dialog that allows players to choose a portrait. */
    public PortraitChooserDialog(Window parent, @Nullable AbstractIcon icon) {
        super(parent, Messages.getString("PortraitChoiceDialog.select_portrait"),
                new PortraitChooser(icon));
    }
    //endregion Constructors

    //region Getters
    @Override
    protected PortraitChooser getChooser() {
        return (PortraitChooser) super.getChooser();
    }
    //endregion Getters
}
