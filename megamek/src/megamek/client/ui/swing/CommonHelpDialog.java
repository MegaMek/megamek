/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

import megamek.client.ui.Messages;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonHelpDialog extends JDialog {

    private static final long serialVersionUID = 5189627839475444823L;
    private static final String CLOSEACTION = "CloseAction"; //$NON-NLS-1$

    /**
     * Create a help dialog for the given <code>parentFrame</code> by reading
     * from the indicated <code>File</code>.
     */
    public CommonHelpDialog(JFrame parentFrame, File helpfile) {
        // Construct the superclass.
        super(parentFrame);

        // Load the help file
        setLayout(new BorderLayout());
        JTextPane helpPane = new JTextPane();
        helpPane.setEditable(false);

        try {
            URL url = helpfile.toURI().toURL();
            helpPane.setPage(url);
            setTitle(Messages.getString("CommonHelpDialog.helpFile") + helpfile.getName()); //$NON-NLS-1$
        } catch (Exception e1) {
            helpPane.setText(Messages.getString("CommonHelpDialog.errorReading") //$NON-NLS-1$
                    + e1.getMessage());
            setTitle(Messages.getString("CommonHelpDialog.noHelp.title")); //$NON-NLS-1$
            e1.printStackTrace();
        }

        // Add a "Close" button.
        Action closeAction = new AbstractAction() {
            private static final long serialVersionUID = 1680850851585381148L;

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        JButton butClose = new JButton(closeAction);
        butClose.setText(Messages.getString("Close")); //$NON-NLS-1$

        InputMap imap = butClose.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), CLOSEACTION);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSEACTION);
        butClose.getActionMap().put(CLOSEACTION, closeAction);

        // Add to the frame
        JScrollPane scroll = new JScrollPane(helpPane);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(butClose, BorderLayout.SOUTH);

        // Make the window half the screensize by default.
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Dimension windowSize = new Dimension(gd.getDisplayMode().getWidth() / 2,
                gd.getDisplayMode().getHeight() / 2);
        pack();
        setSize(windowSize);

        // Place this dialog on middle of screen.
        setLocationRelativeTo(null);
    }

}
