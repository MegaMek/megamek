/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
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
package megamek.client.ui.swing.dialog.imageChooser;

import java.awt.Window;

import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

/**
 * This dialog allows players to select the camo pattern (or color) used by
 * their units during the game. It automatically fills itself with all the color
 * choices in IPlayer and all the camo patterns in the
 * {@link Configuration#camoDir()} directory tree.
 * Should be shown by using showDialog(IPlayer) or showDialog(Entity). These
 * methods return either JOptionPane.OK_OPTION or .CANCEL_OPTION.
 *
 * @see AbstractIconChooserDialog
 */
public class CamoChooserDialog extends AbstractIconChooserDialog {
    //region Variable Declarations
    private static final long serialVersionUID = -8060324139099113292L;
    //endregion Variable Declarations

    //region Constructors
    /** Creates a dialog that allows players to choose a camo pattern. */
    public CamoChooserDialog(Window parent, AbstractIcon individualCamouflage) {
        this(parent, null, individualCamouflage);
    }

    public CamoChooserDialog(Window parent, @Nullable AbstractIcon ownerCamouflage, AbstractIcon individualCamouflage) {
        super(parent, Messages.getString("CamoChoiceDialog.select_camo_pattern"), new CamoChooser(ownerCamouflage, individualCamouflage));
    }
    //endregion Constructors

    @Override
    protected CamoChooser getChooser() {
        return (CamoChooser) super.getChooser();
    }

    @Override
    public Camouflage getSelectedItem() {
        return ((Camouflage) super.getSelectedItem()).clone();
    }
}
