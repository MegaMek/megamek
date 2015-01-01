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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.AdvancedLabel;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonHelpDialog extends Dialog {
    /**
     * 
     */
    private static final long serialVersionUID = 6500342607269603829L;
    private AdvancedLabel lblHelp;

    /**
     * Create a help dialog for the given parent <code>Frame</code> by reading
     * from the indicated <code>File</code>.
     * 
     * @param frame - the parent <code>Frame</code> for this dialog. This
     *            value should <b>not</b> be <code>null</code>.
     * @param helpfile - the <code>File</code> containing the help text. This
     *            value should <b>not</b> be <code>null</code>.
     */
    public CommonHelpDialog(Frame frame, File helpfile) {
        // Construct the superclass.
        super(frame);

        // Make sure we close at the appropriate times.
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        // Create the help dialog.
        this.setLayout(new BorderLayout());
        lblHelp = new AdvancedLabel(Messages
                .getString("CommonHelpDialog.noHelp.Message")); //$NON-NLS-1$
        ScrollPane scroll = new ScrollPane(
                java.awt.ScrollPane.SCROLLBARS_ALWAYS);
        scroll.add(lblHelp);
        this.add(scroll, BorderLayout.CENTER);

        // Add a "Close" button.
        Button butClose = new Button(Messages
                .getString("CommonHelpDialog.Close")); //$NON-NLS-1$
        butClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                quit();
            }
        });
        this.add(butClose, BorderLayout.SOUTH);

        // Make the window half the screensize by default.
        Dimension screenSize = frame.getToolkit().getScreenSize();
        Dimension windowSize = new Dimension(screenSize.width / 2,
                screenSize.height / 2);
        this.pack();
        this.setSize(windowSize);

        // Place this dialog on middle of screen.
        this.setLocation(screenSize.width / 2 - windowSize.width / 2,
                screenSize.height / 2 - windowSize.height / 2);

        setFile(helpfile);
    }

    public void setFile(File helpfile) {
        // Create a buffer to contain our help text.
        StringBuffer buff = new StringBuffer();

        // Were we passed a null helpfile?
        if (helpfile == null) {
            // Big error.
            this.setTitle(Messages.getString("CommonHelpDialog.noHelp.title")); //$NON-NLS-1$
            buff.append(Messages.getString("CommonHelpDialog.noHelp.Message")); //$NON-NLS-1$
        } else {
            // Set our title.
            this
                    .setTitle(Messages.getString("CommonHelpDialog.helpFile") + helpfile.getName()); //$NON-NLS-1$

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
                                        .getString("CommonHelpDialog.errorReading")) //$NON-NLS-1$
                        .append(exp.getMessage());
                exp.printStackTrace();
            }
        } // End non-null-helpfile
        lblHelp.setText(buff.toString());
    }

    /**
     * Close this dialog.
     */
    /* package */void quit() {
        this.setVisible(false);
    }

}
