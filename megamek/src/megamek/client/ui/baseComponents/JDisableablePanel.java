/*
 * Copyright (c) 2021-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.baseComponents;

import javax.swing.*;
import java.awt.*;

/**
 * This panel is used when you want a JPanel that enables/disables all of its children when it is
 * enabled/disabled. This is purposefully not recursive, as we only want child components to be
 * disabled based on the settings here.
 */
public class JDisableablePanel extends JPanel {
    //region Constructors
    public JDisableablePanel(final String name) {
        super();
        setName(name);
    }
    //endregion Constructors

    /**
     * This override forces all child components to be the same value for enabled as this component,
     * thus allowing one to easily enable/disable child components and panels.
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
