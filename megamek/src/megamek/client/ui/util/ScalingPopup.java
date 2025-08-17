/*
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

package megamek.client.ui.util;

import java.awt.Dimension;
import java.io.Serial;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A JPopupMenu that automatically scales with MegaMek's GUI Scaling value obtained from
 * {@link megamek.client.ui.clientGUI.GUIPreferences#getGUIScale()}
 *
 * @author Juliez
 */
public class ScalingPopup extends JPopupMenu {

    /** Returns a spacer (empty, small menu item) for a scaling popup menu. */
    public static JMenuItem spacer() {
        JMenuItem result = new JMenuItem() {
            @Serial
            private static final long serialVersionUID = 1249257644704746075L;

            @Override
            public Dimension getPreferredSize() {
                Dimension s = super.getPreferredSize();
                return new Dimension(s.width, 8);
            }
        };
        result.setEnabled(false);
        return result;
    }
}
