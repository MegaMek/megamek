/*
 * MegaMek - Copyright (C) 2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import static megamek.MegaMek.TIMESTAMP;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.MegaMekConstants;
import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonAboutDialog extends JDialog {
    private static final long serialVersionUID = -9019180090528719535L;
    
    private static final String FILENAME_MEGAMEK_SPLASH2 = "megamek-splash2.gif";
    /** We only need a single copy of the "about" title image. */
    private static Image imgTitleImage;

    /**
     * Get the single title image in a threadsafe way.
     * 
     * @param frame - a <code>JFrame</code> object to instantiate the image.
     * @return the title <code>Image</code> common to all "about" dialogs.
     *         This value should <b>not</b> be <code>null</code>.
     */
    private static synchronized Image getTitleImage(JFrame frame) {
        // Have we loaded our image yet?
        if (imgTitleImage == null) {
            // Nope. Load it.
            Image image = frame.getToolkit().getImage(
                    new MegaMekFile(Configuration.miscImagesDir(), FILENAME_MEGAMEK_SPLASH2).toString()
            );
            MediaTracker tracker = new MediaTracker(frame);
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0);
                imgTitleImage = image;
            } catch (InterruptedException exp) {
                exp.printStackTrace();
            }
        } // End load-imgTitleImage

        return imgTitleImage;
    }

    /**
     * Create an "About" dialog for MegaMek.
     * 
     * @param parentFrame - the parent <code>JFrame</code> for this dialog.
     */
    public CommonAboutDialog(JFrame parentFrame) {
        super(parentFrame, Messages.getString("CommonAboutDialog.title"), true);

        // Splash image
        Image imgSplash = getTitleImage(parentFrame);
        JLabel panTitle = new JLabel(new ImageIcon(imgSplash));

        // Version text
        StringBuffer buff = new StringBuffer();
        buff.append(Messages.getString("CommonAboutDialog.version"))
                .append(MegaMekConstants.VERSION).append(
                        Messages.getString("CommonAboutDialog.timestamp"))
                .append(new Date(TIMESTAMP)).append(
                        Messages.getString("CommonAboutDialog.javaVendor"))
                .append(System.getProperty("java.vendor"))
                .append(Messages.getString("CommonAboutDialog.javaVersion"))
                .append(System.getProperty("java.version"));
        JTextArea lblVersion = new JTextArea(buff.toString());
        lblVersion.setEditable(false);
        lblVersion.setOpaque(false);
        
        // Copyright notice
        JTextArea lblCopyright = new JTextArea(Messages.getString("CommonAboutDialog.copyright"));
        lblCopyright.setEditable(false);
        lblCopyright.setOpaque(false);
        
        // MegaMek About message
        JTextArea lblAbout = new JTextArea(Messages.getString("CommonAboutDialog.about"));
        lblAbout.setEditable(false);
        lblAbout.setOpaque(false);

        // Close Button
        JButton butClose = new ButtonEsc(new CloseAction(this));

        // Assemble all
        JPanel middlePanel = new JPanel();
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));
        add(panTitle, BorderLayout.PAGE_START);
        middlePanel.add(lblVersion);
        middlePanel.add(lblCopyright);
        middlePanel.add(lblAbout);
        add(middlePanel, BorderLayout.CENTER);
        add(butClose, BorderLayout.PAGE_END);

        // Place in the middle of the screen
        pack();
        setLocationRelativeTo(parentFrame);
        setResizable(false);
    }
}
