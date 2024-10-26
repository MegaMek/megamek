/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A MegaMek Dialog box.
 */
public class ClientDialog extends JDialog {

    private static final long serialVersionUID = 6154951760485853883L;

    protected JFrame owner = null;

    /**
     * Creates a ClientDialog with modality as given by modal.
     * @see JDialog#JDialog(java.awt.Frame, String, boolean)
     */
    public ClientDialog(JFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.owner = owner;
    }

    /** Center the dialog within the owner frame.  */
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
     * Adds a row (line) with the two JComponents <code>label, secondC</code>
     * to the given <code>panel</code>, using constraints c. The label will be
     * right-aligned, the secondC left-aligned to bring them close together.
     * Only useful for simple panels with GridBagLayout.
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
