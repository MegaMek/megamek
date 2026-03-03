/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.clientDialogs;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A MegaMek Dialog box.
 */
public class ClientDialog extends JDialog {

    protected Window owner;

    /**
     * Creates a ClientDialog with modality as given by modal.
     *
     * @see JDialog#JDialog(Window, String, ModalityType) 
     */
    public ClientDialog(Window owner, String title, boolean modal) {
        super(owner, title, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        this.owner = owner;
    }

    /** Center the dialog within the owner frame. */
    public void center() {
        if (owner == null) {
            return;
        }

        setLocation(owner.getLocation().x + (owner.getSize().width / 2)
                    - (getSize().width / 2),
              owner.getLocation().y + (owner.getSize().height / 2)
                    - (getSize().height / 2));
    }

    /**
     * Adds a row (line) with the two JComponents <code>label, secondC</code> to the given <code>panel</code>, using
     * constraints c. The label will be right-aligned, the secondC left-aligned to bring them close together. Only
     * useful for simple panels with GridBagLayout.
     */
    public void addOptionRow(JPanel targetP, GridBagConstraints c, JLabel label, Component secondC) {
        int oldGridW = c.gridwidth;
        int oldAnchor = c.anchor;

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        targetP.add(label, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        targetP.add(secondC, c);

        c.gridwidth = oldGridW;
        c.anchor = oldAnchor;
    }
}
