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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;

public class RapidFireMGChoice {

    private final Mounted<?> mounted;

    private final JCheckBox chRapid = new JCheckBox(Messages.getString("CustomMekDialog.burstFireMachineGun"));

    public RapidFireMGChoice(Mounted<?> mounted, Entity entity, JPanel parentPanel, GBC2 gbc) {
        this.mounted = mounted;
        String weaponName = "(%s) %s:".formatted(entity.getLocationAbbr(mounted.getLocation()), mounted.getName());
        JLabel weaponLabel = new JLabel(weaponName);
        chRapid.setSelected(mounted.isRapidFire());
        parentPanel.add(weaponLabel, gbc.forLabel());
        parentPanel.add(chRapid, gbc.eol());
    }

    public void applyChoice() {
        mounted.setRapidFire(chRapid.isSelected());
    }

    public void setEnabled(boolean enabled) {
        chRapid.setEnabled(enabled);
    }
}
