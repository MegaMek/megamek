/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.ui.swing.util;

import java.awt.Dimension;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A JPopupMenu that automatically scales with MegaMek's GUI Scaling value
 * obtained from {@link megamek.client.ui.swing.GUIPreferences#getGUIScale()}
 * @author Juliez
 */
public class ScalingPopup extends JPopupMenu {

    /** Returns a spacer (empty, small menu item) for a scaling popup menu. */
    public static JMenuItem spacer() {
        JMenuItem result = new JMenuItem() {
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
