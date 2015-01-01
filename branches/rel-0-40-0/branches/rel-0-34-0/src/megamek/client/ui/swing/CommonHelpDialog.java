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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonHelpDialog extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = 5189627839475444823L;
    private JTextArea lblHelp;

    /**
     * Create a help dialog for the given parent <code>Frame</code> by reading
     * from the indicated <code>File</code>.
     * 
     * @param frame - the parent <code>Frame</code> for this dialog. This
     *            value should <b>not</b> be <code>null</code>.
     * @param helpfile - the <code>File</code> containing the help text. This
     *            value should <b>not</b> be <code>null</code>.
     */
    public CommonHelpDialog(JFrame frame, File helpfile) {
        // Construct the superclass.
        super(frame);

        // Make sure we close at the appropriate times.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        // Create the help dialog.
        setLayout(new BorderLayout());
        lblHelp = new JTextArea(Messages
                .getString("CommonHelpDialog.noHelp.Message")); //$NON-NLS-1$
        lblHelp.setEditable(false);
        lblHelp.setOpaque(false);
        JScrollPane scroll = new JScrollPane(lblHelp,
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        getContentPane().add(scroll, BorderLayout.CENTER);

        // Add a "Close" button.
        JButton butClose = new JButton(Messages
                .getString("CommonHelpDialog.Close")); //$NON-NLS-1$
        butClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                quit();
            }
        });
        getContentPane().add(butClose, BorderLayout.SOUTH);

        // Make the window half the screensize by default.
        Dimension screenSize = frame.getToolkit().getScreenSize();
        Dimension windowSize = new Dimension(screenSize.width / 2,
                screenSize.height / 2);
        pack();
        setSize(windowSize);

        // Place this dialog on middle of screen.
        setLocation(screenSize.width / 2 - windowSize.width / 2,
                screenSize.height / 2 - windowSize.height / 2);

        setFile(helpfile);
    }

    public void setFile(File helpfile) {
        // Create a buffer to contain our help text.
        StringBuffer buff = new StringBuffer();

        // Were we passed a null helpfile?
        if (helpfile == null) {
            // Big error.
            setTitle(Messages.getString("CommonHelpDialog.noHelp.title")); //$NON-NLS-1$
            buff.append(Messages.getString("CommonHelpDialog.noHelp.Message")); //$NON-NLS-1$
        } else {
            // Set our title.
            setTitle(Messages.getString("CommonHelpDialog.helpFile") + helpfile.getName()); //$NON-NLS-1$

            // Try to read in the help file.
            boolean firstLine = true;
            try {
                BufferedReader input = new BufferedReader(new FileReader(
                        helpfile));
                String line = input.readLine();
                // while ( line != null && line.length() > 0 ) {
                while (line != null) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        buff.append(" \n"); // the space is to force a line-feed
                                            // on empty lines //$NON-NLS-1$
                    }
                    buff.append(line);
                    line = input.readLine();
                }
            } catch (IOException exp) {
                if (!firstLine) {
                    buff.append("\n \n"); //$NON-NLS-1$
                }
                buff
                        .append(
                                Messages
                                        .getString("CommonHelpDialog.errorReading"))//$NON-NLS-1$
                        .append(exp.getMessage());
                exp.printStackTrace();
            }
        } // End non-null-helpfile
        lblHelp.setText(buff.toString());
    }

    /**
     * Close this dialog.
     */
    /* package */
    void quit() {
        setVisible(false);
    }

}
