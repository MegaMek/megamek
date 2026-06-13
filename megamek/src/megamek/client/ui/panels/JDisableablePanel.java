/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels;

import java.awt.Component;
import javax.swing.JPanel;

/**
 * This panel is used when you want a JPanel that enables/disables all of its children when it is enabled/disabled. This
 * is purposefully not recursive, as we only want child components to be disabled based on the settings here.
 */
public class JDisableablePanel extends JPanel {
    //region Constructors
    public JDisableablePanel(final String name) {
        super();
        setName(name);
    }
    //endregion Constructors

    /**
     * This override forces all child components to be the same value for enabled as this component, thus allowing one
     * to easily enable/disable child components and panels.
     *
     * @param enabled whether to enable the child components or not
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        for (final Component component : getComponents()) {
            component.setEnabled(enabled);
        }
    }
}
