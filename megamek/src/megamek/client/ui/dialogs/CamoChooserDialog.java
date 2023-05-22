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

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.panels.CamoChooser;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

import javax.swing.*;
import java.awt.*;

/**
 * This dialog allows players to select the camouflage pattern (or colour) used by their units. It
 * automatically fills itself with all the PlayerColour Enum colours and all the camouflage icons in
 * the Camouflage directory.
 * @see AbstractIconChooserDialog
 */
public class CamoChooserDialog extends AbstractIconChooserDialog {
    //region Variable Declarations
    private boolean useDefault = false;
    //endregion Variable Declarations

    //region Constructors
    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage) {
        this(frame, camouflage, false);
    }

    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage,
                             final boolean canHaveIndividualCamouflage) {
        super(frame, "CamoChooserDialog", "CamoChoiceDialog.select_camo_pattern",
                new CamoChooser(frame, camouflage, canHaveIndividualCamouflage), true);
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
        final JPanel panel = new JPanel(new GridLayout(1, 3));
        panel.setName("buttonPanel");

        panel.add(new MMButton("btnOk", resources, "Ok.text", "Ok.toolTipText",
                this::okButtonActionPerformed));
        if (getChooser().canHaveIndividualCamouflage()) {
            panel.add(new MMButton("btnParent", resources, "btnParent.text",
                    "btnParent.toolTipText", evt -> {
                setUseDefault(true);
                okButtonActionPerformed(evt);
            }));
        }

        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText",
                this::cancelActionPerformed));
        panel.add(new MMButton("btnRefresh", resources, "RefreshDirectory.text",
                "RefreshDirectory.toolTipText", evt -> getChooser().refreshDirectory()));

        return panel;
    }
    //endregion Initialization

    @Override
    protected CamoChooser getChooser() {
        return (CamoChooser) super.getChooser();
    }

    @Override
    public Camouflage getSelectedItem() {
        return isUseDefault() ? new Camouflage() : ((Camouflage) super.getSelectedItem()).clone();
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        adaptToGUIScale();
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}
