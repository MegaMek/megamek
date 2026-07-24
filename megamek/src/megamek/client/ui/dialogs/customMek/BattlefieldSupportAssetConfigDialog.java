/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.awt.Container;
import java.awt.GridBagLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.enums.SkillLevel;

/**
 * A small configuration dialog for a Battlefield Support Asset's crew. Unlike a full unit, an asset has no
 * gunnery/piloting numbers and no equipment; its only crew choices are a name and a grade (Regular or Veteran). This
 * replaces {@link CustomMekDialog} for assets, whose many tabs (equipment, weapons, quirks) do not apply.
 */
public class BattlefieldSupportAssetConfigDialog extends AbstractButtonDialog {

    private final BattlefieldSupportAsset asset;
    private final boolean editable;

    private final JTextField fldName = new JTextField(20);
    private final JComboBox<SkillLevel> cbGrade = new JComboBox<>();

    /**
     * @param frame    the parent frame
     * @param asset    the asset whose crew is being configured
     * @param editable whether the local player may edit this asset (false shows the values read-only)
     */
    public BattlefieldSupportAssetConfigDialog(JFrame frame, BattlefieldSupportAsset asset, boolean editable) {
        super(frame, "BattlefieldSupportAssetConfigDialog", "BattlefieldSupportAssetConfigDialog.title");
        this.asset = asset;
        this.editable = editable;
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        panel.add(new JLabel(Messages.getString("BattlefieldSupportAssetConfigDialog.name"), SwingConstants.RIGHT),
              GBC.std());
        fldName.setText(asset.getCrew().getName(0));
        panel.add(fldName, GBC.eol());

        // Only offer Veteran when the asset actually defines a Veteran variant; otherwise it is always Regular.
        cbGrade.addItem(SkillLevel.REGULAR);
        if (asset.hasVeteranProfile()) {
            cbGrade.addItem(SkillLevel.VETERAN);
        }
        cbGrade.setSelectedItem(asset.getCrewSkillLevel());

        panel.add(new JLabel(Messages.getString("BattlefieldSupportAssetConfigDialog.grade"), SwingConstants.RIGHT),
              GBC.std());
        panel.add(cbGrade, GBC.eol());
        if (!asset.hasVeteranProfile()) {
            cbGrade.setToolTipText(Messages.getString("BattlefieldSupportAssetConfigDialog.gradeLockedTip"));
        }

        if (!editable) {
            fldName.setEnabled(false);
            cbGrade.setEnabled(false);
        }
        return panel;
    }

    /**
     * Copies the chosen name and grade onto the asset. Call only after the dialog was confirmed and when the asset is
     * editable.
     */
    public void applyChoices() {
        asset.getCrew().setName(fldName.getText(), 0);
        if (asset.hasVeteranProfile()) {
            asset.setVeteranCrew(cbGrade.getSelectedItem() == SkillLevel.VETERAN);
        }
    }
}
