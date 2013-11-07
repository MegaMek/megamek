/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import megamek.client.Client;

public abstract class StatusBarPhaseDisplay extends AbstractPhaseDisplay
        implements ActionListener {

    private static final long serialVersionUID = 639696875125581395L;
    
    protected static final int TRANSPARENT = 0xFFFF00FF;
    
    // displays
    private JLabel labStatus;
    protected JPanel panStatus;

    protected StatusBarPhaseDisplay() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
        "clearButton");

        getActionMap().put("clearButton", new AbstractAction() {
            private static final long serialVersionUID = -7781405756822535409L;

            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.getClient().isMyTurn()) {
                    clear();
                }
            }
        });
    }

    /**
     * clears the actions of this phase
     */
    public abstract void clear();

    /**
     * Sets up the status bar with toggle buttons for the mek display and map.
     */
    protected void setupStatusBar(String defStatus) {
        panStatus = new JPanel();
        panStatus.setOpaque(false);
        labStatus = new JLabel(defStatus, SwingConstants.CENTER);
        labStatus.setForeground(Color.yellow);
        labStatus.setOpaque(false);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panStatus.setLayout(gridbag);

        c.insets = new Insets(0, 1, 0, 1);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labStatus, c);
        panStatus.add(labStatus);
    }

    protected void setStatusBarText(String text) {
        labStatus.setText(text);
    }

    protected boolean statusBarActionPerformed(ActionEvent ev, Client client) {
        return false;
    }
}
