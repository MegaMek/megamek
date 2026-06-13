/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.scenario;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import megamek.common.scenario.Scenario;

/**
 * This is a JList renderer for {@link ScenarioInfoPanel}.
 */
public class ScenarioInfoRenderer extends ScenarioInfoPanel implements ListCellRenderer<Scenario> {

    @Override
    public Component getListCellRendererComponent(final JList<? extends Scenario> list,
          final Scenario value, final int index,
          final boolean isSelected,
          final boolean cellHasFocus) {
        final Color foreground = new Color((isSelected
              ? list.getSelectionForeground() : list.getForeground()).getRGB());
        final Color background = new Color((isSelected
              ? list.getSelectionBackground() : list.getBackground()).getRGB());
        setForeground(foreground);
        setBackground(background);
        updateFromPreset(value);
        return this;
    }
}
