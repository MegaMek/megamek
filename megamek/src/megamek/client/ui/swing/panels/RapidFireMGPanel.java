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
 */
package megamek.client.ui.swing.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serial;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.Mounted;

public class RapidFireMGPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 5261919826318225201L;

    private final Mounted<?> mounted;

    JCheckBox chRapid = new JCheckBox();

    public RapidFireMGPanel(Mounted<?> mounted, Entity entity) {
        this.mounted = mounted;
        int mountedLocation = mounted.getLocation();
        String stringDescription = Messages.getString("CustomMekDialog.gridBagLayout",
              entity.getLocationAbbr(mountedLocation));
        JLabel labelLocation = new JLabel(stringDescription);
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);
        add(labelLocation, GBC.std().anchor(GridBagConstraints.EAST));
        chRapid.setSelected(mounted.isRapidfire());
        add(chRapid, GBC.eol());
    }

    public void applyChoice() {
        mounted.setRapidfire(chRapid.isSelected());
    }

    @Override
    public void setEnabled(boolean enabled) {
        chRapid.setEnabled(enabled);
    }
}
