/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.awt.FlowLayout;
import java.util.Hashtable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.buttons.DialogButton;
import megamek.common.units.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

/**
 * This dialog allows players to select the camouflage pattern (or colour) used by their units. It automatically fills
 * itself with all the PlayerColour Enum colours and all the camouflage icons in the Camouflage directory.
 *
 * @see AbstractIconChooserDialog
 */
public class CamoChooserDialog extends AbstractIconChooserDialog {
    //region Variable Declarations
    private boolean useDefault = false;
    private JSlider rotationSlider;
    private JSlider scaleSlider;
    private final Camouflage originalCamo;
    private Entity entity;
    private EntityImagePanel entityImage;
    //endregion Variable Declarations

    //region Constructors
    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage) {
        this(frame, camouflage, false);
    }

    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage,
          final boolean canHaveIndividualCamouflage) {
        super(frame, "CamoChooserDialog", "CamoChoiceDialog.select_camo_pattern",
              new CamoChooserPanel(frame, camouflage, canHaveIndividualCamouflage), true);
        originalCamo = (Camouflage) camouflage;
    }
    //endregion Constructors

    //region Getters/Setters
    public boolean isUseDefault() {
        return useDefault;
    }

    public void setDisplayedEntity(Entity entity) {
        this.entity = entity;
    }

    public void setUseDefault(final boolean useDefault) {
        this.useDefault = useDefault;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected JPanel createButtonPanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));

        rotationSlider = new JSlider(-180, 180);
        rotationSlider.setMajorTickSpacing(90);
        rotationSlider.setMinorTickSpacing(10);
        rotationSlider.setPaintTicks(true);
        rotationSlider.setSnapToTicks(true);
        rotationSlider.addChangeListener(e -> updatePreview());
        rotationSlider.setPaintLabels(true);

        scaleSlider = new JSlider(3, 15);
        scaleSlider.setSnapToTicks(true);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setMajorTickSpacing(1);
        scaleSlider.setPaintLabels(true);
        Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
        labelTable.put(5, new JLabel("0.5"));
        labelTable.put(10, new JLabel("1"));
        labelTable.put(15, new JLabel("1.5"));
        scaleSlider.setLabelTable(labelTable);
        scaleSlider.addChangeListener(e -> updatePreview());

        entityImage = new EntityImagePanel(null, null);

        JPanel rotationPanel = new JPanel();
        rotationPanel.add(Box.createHorizontalStrut(20));
        rotationPanel.add(new JLabel(Messages.getString("CamoChoiceDialog.rotation") + ":"));
        rotationPanel.add(rotationSlider);

        JPanel scalePanel = new JPanel();
        scalePanel.add(Box.createHorizontalStrut(30));
        scalePanel.add(new JLabel(Messages.getString("CamoChoiceDialog.scale") + ":"));
        scalePanel.add(scaleSlider);

        JPanel modifierPanel = new JPanel();
        modifierPanel.add(entityImage);
        modifierPanel.add(rotationPanel);
        modifierPanel.add(scalePanel);

        JScrollPane modifierScrollPane = new JScrollPane(modifierPanel,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        var okButton = new DialogButton(Messages.getString("Ok.text"));
        okButton.addActionListener(this::okButtonActionPerformed);
        getRootPane().setDefaultButton(okButton);

        var cancelButton = new DialogButton(Messages.getString("Cancel.text"));
        cancelButton.addActionListener(this::cancelActionPerformed);

        var refreshButton = new DialogButton(Messages.getString("RefreshDirectory.text"));
        refreshButton.setToolTipText(Messages.getString("RefreshDirectory.toolTipText"));
        refreshButton.addActionListener(evt -> getChooser().refreshDirectory());

        var parentCamoButton = new DialogButton(Messages.getString("CamoChoiceDialog.btnParent.text"));
        parentCamoButton.setToolTipText(Messages.getString("CamoChoiceDialog.btnParent.toolTipText"));
        parentCamoButton.addActionListener(evt -> {
            setUseDefault(true);
            okButtonActionPerformed(evt);
        });
        parentCamoButton.setEnabled(getChooser().canHaveIndividualCamouflage());

        final JPanel buttonPanel = new JPanel(new WrapLayout(FlowLayout.RIGHT));
        buttonPanel.setName("buttonPanel");
        buttonPanel.add(parentCamoButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(Box.createHorizontalStrut(30));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        container.add(modifierScrollPane);
        container.add(buttonPanel);

        return container;
    }
    //endregion Initialization

    @Override
    protected CamoChooserPanel getChooser() {
        return (CamoChooserPanel) super.getChooser();
    }

    @Override
    public Camouflage getSelectedItem() {
        Camouflage result = new Camouflage();
        if (!isUseDefault() && (super.getSelectedItem() != null)) {
            result = ((Camouflage) super.getSelectedItem()).clone();
        }
        result.setScale(scaleSlider.getValue());
        result.setRotationAngle(rotationSlider.getValue());
        return result;
    }

    @Override
    public void setVisible(boolean b) {
        if ((originalCamo != null) && b) {
            rotationSlider.setValue(originalCamo.getRotationAngle());
            scaleSlider.setValue(originalCamo.getScale());
        }
        if (b) {
            getChooser().getImageList().addListSelectionListener(e -> updatePreview());
        }
        super.setVisible(b);
    }

    private void updatePreview() {
        if (getSelectedItem() != null) {
            entityImage.updateDisplayedEntity(entity, getSelectedItem());
        }
    }
}
