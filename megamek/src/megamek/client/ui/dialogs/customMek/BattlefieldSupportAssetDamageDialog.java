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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;

/**
 * A small damage editor for a Battlefield Support Asset. An asset has no armor, internals or criticals; its only
 * persistent damage is a lowered Destroy Check. This lets the user set the current Destroy Check anywhere between 0 and
 * the as-constructed value, replacing the full {@code UnitEditorDialog} whose armor/structure/critical widgets do not
 * apply.
 */
public class BattlefieldSupportAssetDamageDialog extends AbstractButtonDialog {

    private final BattlefieldSupportAsset asset;
    private final JSpinner spnDestroyCheck;

    /**
     * @param frame the parent frame
     * @param asset the asset whose damage (lowered Destroy Check) is being edited
     */
    public BattlefieldSupportAssetDamageDialog(JFrame frame, BattlefieldSupportAsset asset) {
        super(frame, "BattlefieldSupportAssetDamageDialog", "BattlefieldSupportAssetDamageDialog.title");
        this.asset = asset;
        int original = asset.getODestroyCheck();
        int current = Math.max(0, Math.min(asset.getDestroyCheck(), original));
        spnDestroyCheck = new JSpinner(new SpinnerNumberModel(current, 0, original, 1));
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        panel.add(new JLabel(Messages.getString("BattlefieldSupportAssetDamageDialog.info")), GBC.eol());
        panel.add(new JLabel(Messages.getString("BattlefieldSupportAssetDamageDialog.original")
              + " " + asset.getODestroyCheck()), GBC.eol());
        panel.add(new JLabel(Messages.getString("BattlefieldSupportAssetDamageDialog.current"), SwingConstants.RIGHT),
              GBC.std());
        panel.add(spnDestroyCheck, GBC.eol());

        return panel;
    }

    /**
     * Copies the chosen current Destroy Check onto the asset. Call only after the dialog was confirmed.
     */
    public void applyChoices() {
        asset.setDestroyCheck((Integer) spnDestroyCheck.getValue());
    }
}
