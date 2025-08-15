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

import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * A {@link javax.swing.JButton JButton} that will react to ESC key presses when in a focused window.
 *
 * @author SJuliez
 */
public class ButtonEsc extends DialogButton {

    private static final long serialVersionUID = -1259826896841611521L;

    private static final String MYACTION = "ButtonEscAction";

    /**
     * Constructs a {@link javax.swing.JButton JButton} that will react to ESC key presses by calling the
     * {@link javax.swing.AbstractAction Action}
     * <code>escAction</code> when the button is in a focused window
     */
    public ButtonEsc(AbstractAction escAction) {
        super(escAction);

        // Link ESC to the escAction
        getActionMap().put(MYACTION, escAction);
        InputMap imap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), MYACTION);
    }
}
