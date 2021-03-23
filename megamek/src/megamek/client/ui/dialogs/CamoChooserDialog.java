/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020-2021 - The MegaMek Team
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
package megamek.client.ui.dialogs;

import megamek.client.ui.swing.dialog.imageChooser.CamoChooser;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * This dialog allows players to select the camouflage pattern (or color) used by their units
 * during the game. It automatically fills itself with all the PlayerColour Enum colours and all
 * the camouflage patterns in the {@link Configuration#camoDir()} directory tree.
 *
 * @see AbstractIconChooserDialog
 */
public class CamoChooserDialog extends AbstractIconChooserDialog {
    //region Variable Declarations
    private boolean useDefault = false;
    //endregion Variable Declarations

    //region Constructors
    /** Creates a dialog that allows players to choose a camo pattern. */
    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage) {
        this(frame, camouflage, false);
    }

    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage,
                             final boolean canHaveIndividualCamouflage) {
        super(frame, "CamoChooserDialog", "CamoChoiceDialog.select_camo_pattern",
                new CamoChooser(camouflage, canHaveIndividualCamouflage));
    }
    //endregion Constructors

    //region Getters/Setters
    public boolean isUseDefault() {
        return useDefault;
    }

    public void setUseDefault(final boolean useDefault) {
        this.useDefault = useDefault;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.add(createButton("Ok.text", "Ok.toolTipText", "okButton", this::okButtonActionPerformed));
        if (getChooser().canHaveIndividualCamouflage()) {
            panel.add(createButton("CamoChoiceDialog.parentButton.text",
                    "CamoChoiceDialog.parentButton.toolTipText", "parentButton", evt -> {
                setUseDefault(true);
                okButtonActionPerformed(evt);
            }));
        }
        panel.add(createButton("Cancel.text", "Cancel.toolTipText", "cancelButton", this::cancelActionPerformed));
        panel.add(createButton("refreshDirectory.text", "refreshDirectory.toolTipText",
                "refreshButton", evt -> getChooser().refreshDirectory()));

        return panel;
    }
    //endregion Initialization

    @Override
    protected CamoChooser getChooser() {
        return (CamoChooser) super.getChooser();
    }

    @Override
    public Camouflage getSelectedItem() {
        return useDefault ? new Camouflage() : ((Camouflage) super.getSelectedItem()).clone();
    }
}
