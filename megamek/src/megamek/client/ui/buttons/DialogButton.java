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
package megamek.client.ui.buttons;

import java.awt.Dimension;
import javax.swing.AbstractAction;
import javax.swing.JButton;

import megamek.client.ui.util.UIUtil;

/**
 * A JButton that has a minimum width which scales with the GUI scale.
 */
public class DialogButton extends JButton {

    private static final long serialVersionUID = 952919304556828345L;

    /** The minimum width this button will have at GUI scale == 1 */
    private final static int BUTTON_MIN_WIDTH = 95;

    public DialogButton(String text) {
        super(text);
    }

    public DialogButton(AbstractAction action) {
        super(action);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension prefSize = super.getPreferredSize();
        prefSize.width = Math.max(prefSize.width, UIUtil.scaleForGUI(BUTTON_MIN_WIDTH));
        return prefSize;
    }
}
