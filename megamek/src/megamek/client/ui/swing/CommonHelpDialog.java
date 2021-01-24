/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
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
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonHelpDialog extends ClientDialog {

    private static final long serialVersionUID = 5189627839475444823L;

    /**
     * Create a help dialog for the given <code>parentFrame</code> by reading
     * from the indicated <code>File</code>.
     */
    public CommonHelpDialog(JFrame parentFrame, File helpfile) {
        super(parentFrame, Messages.getString("CommonHelpDialog.helpFile") + helpfile.getName(), true, true);

        setLayout(new BorderLayout());
        JEditorPane helpPane = new JEditorPane();
        helpPane.setEditable(false);

        // Get the help content file if possible
        try {
            helpPane.setPage(helpfile.toURI().toURL());
            helpPane.setFont(new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1)));
            setTitle(Messages.getString("CommonHelpDialog.helpFile") + helpfile.getName());
        } catch (Exception exc) {
            helpPane.setText(Messages.getString("CommonHelpDialog.errorReading")
                    + exc.getMessage());
            setTitle(Messages.getString("CommonHelpDialog.noHelp.title"));
            exc.printStackTrace();
        }

        // Close Button
        JButton butClose = new ButtonEsc(new CloseAction(this));

        // Add all to the dialog
        getContentPane().add(new JScrollPane(helpPane), BorderLayout.CENTER);
        getContentPane().add(butClose, BorderLayout.SOUTH);
        
        pack();
        center();
        
        // Make the window half the screensize and center on screen
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Dimension windowSize = new Dimension(gd.getDisplayMode().getWidth() / 2,
                gd.getDisplayMode().getHeight() / 2);
        setPreferredSize(windowSize);
    }
}