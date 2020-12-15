/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing.dialog.imageChooser;

import java.awt.Window;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.IPlayer;
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
    public CamoChooserDialog(Window parent) {
        super(parent, Messages.getString("CamoChoiceDialog.select_camo_pattern"), new CamoChooser());
    }
    //endregion Constructors

    @Override
    protected CamoChooser getChooser() {
        return (CamoChooser) super.getChooser();
    }

    /**
     * Show the camo choice dialog and pre-select the camo or color
     * of the given player. The dialog will allow choosing camos
     * and colors. Also refreshes the camos from disk.
     */
    public int showDialog(IPlayer player) {
        getChooser().refreshDirectory();
        getChooser().setIndividualCamo(false);
        getChooser().setSelection(player.getCamouflage());
        return showDialog();
    }

    /**
     * Show the camo choice dialog and pre-select the camo or color
     * of the given entity. The dialog will allow choosing camos
     * and the single color of the player owning the entity.
     * Also refreshes the camos from disk.
     */
    public int showDialog(Entity entity) {
        getChooser().refreshDirectory();
        getChooser().setIndividualCamo(true);
        setEntity(entity);
        return showDialog();
    }

    /**
     * Preselects the Tree and the Images with the given entity's camo
     * or the owner's, if the entity has no individual camo. Also stores
     * the owner's camo to present a "revert to no individual" camo option.
     */
    private void setEntity(Entity entity) {
        // Store the owner's camo to display as the only "No Camo" option
        // This may be a color
        String item = entity.getOwner().getCamouflage().getFilename();
        if (entity.getOwner().getCamouflage().getCategory().equals(Camouflage.NO_CAMOUFLAGE)) {
            item = PlayerColors.COLOR_NAMES[entity.getOwner().getColorIndex()];
        }
        getChooser().setEntityOwnerCamo(getChooser()
                .createIcon(entity.getOwner().getCamouflage().getCategory(), item));

        // Set the camo category and filename to the entity's if it has one,
        // otherwise to the corresponding player's camo category
        getChooser().setSelection((entity.getCamouflage()).isDefault()
                ? entity.getOwner().getCamouflage(): entity.getCamouflage());
    }
}
