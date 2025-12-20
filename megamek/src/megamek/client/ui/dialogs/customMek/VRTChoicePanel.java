/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.FlowLayout;
import java.io.Serial;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import megamek.client.ui.Messages;
import megamek.common.enums.VariableRangeTargetingMode;
import megamek.common.units.Entity;

/**
 * Panel for selecting Variable Range Targeting mode during lobby configuration. Per BMM pg. 86: Player
 * selects Long or Short mode. - LONG mode: -1 TN at long range, +1 TN at short range - SHORT mode: -1 TN at short
 * range, +1 TN at long range
 */
public class VRTChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Entity entity;
    private final JRadioButton rbLong;
    private final JRadioButton rbShort;

    public VRTChoicePanel(Entity entity) {
        this.entity = entity;

        setLayout(new FlowLayout(FlowLayout.LEFT));

        rbLong = new JRadioButton(Messages.getString("CustomMekDialog.VRTModeLong"));
        rbShort = new JRadioButton(Messages.getString("CustomMekDialog.VRTModeShort"));

        rbLong.setToolTipText(Messages.getString("CustomMekDialog.VRTModeLong.tooltip"));
        rbShort.setToolTipText(Messages.getString("CustomMekDialog.VRTModeShort.tooltip"));

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(rbLong);
        modeGroup.add(rbShort);

        // Set initial selection from entity's current mode
        VariableRangeTargetingMode currentMode = entity.getVariableRangeTargetingMode();
        if (currentMode.isLong()) {
            rbLong.setSelected(true);
        } else {
            rbShort.setSelected(true);
        }

        add(rbLong);
        add(rbShort);
    }

    /**
     * Applies the selected VRT mode to the entity.
     */
    public void applyChoice() {
        VariableRangeTargetingMode mode = rbLong.isSelected()
              ? VariableRangeTargetingMode.LONG
              : VariableRangeTargetingMode.SHORT;
        entity.setVariableRangeTargetingMode(mode);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        rbLong.setEnabled(enabled);
        rbShort.setEnabled(enabled);
    }
}
