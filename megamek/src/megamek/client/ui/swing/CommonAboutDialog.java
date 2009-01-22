/*
 * MegaMek - Copyright (C) 2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
import static megamek.MegaMek.VERSION;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonAboutDialog extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = -9019180090528719535L;
    /**
     * We only need a single copy of the "about" title image that we share.
     */
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
                    "data/images/misc/megamek-splash2.gif"); //$NON-NLS-1$
            MediaTracker tracker = new MediaTracker(frame);
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0);
                imgTitleImage = image;
            } catch (InterruptedException exp) {
                exp.printStackTrace();
            }
        } // End load-imgTitleImage

        // Return our image.
        return imgTitleImage;
    }

    /**
     * Create an "about" dialog for MegaMek.
     * 
     * @param frame - the parent <code>JFrame</code> for this dialog.
     */
    public CommonAboutDialog(JFrame frame) {
        // Construct the superclass.
        super(frame, Messages.getString("CommonAboutDialog.title")); //$NON-NLS-1$

        // Make sure we close at the appropriate times.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        // Make a splash image panel.
        Image imgSplash = getTitleImage(frame);
        JLabel panTitle = new JLabel(new ImageIcon(imgSplash));
        panTitle.setPreferredSize(new Dimension(imgSplash.getWidth(null),
                imgSplash.getHeight(null)));

        // Make a label containing the version of this app.
        StringBuffer buff = new StringBuffer();
        buff.append(Messages.getString("CommonAboutDialog.version"))//$NON-NLS-1$
                .append(VERSION).append(
                        Messages.getString("CommonAboutDialog.timestamp"))//$NON-NLS-1$
                .append(new Date(TIMESTAMP).toString()).append(
                        Messages.getString("CommonAboutDialog.javaVendor"))//$NON-NLS-1$
                .append(System.getProperty("java.vendor"))//$NON-NLS-1$
                .append(Messages.getString("CommonAboutDialog.javaVersion"))//$NON-NLS-1$
                .append(System.getProperty("java.version")); //$NON-NLS-1$
        JTextArea lblVersion = new JTextArea(buff.toString());
        lblVersion.setEditable(false);
        lblVersion.setOpaque(false);
        JTextArea lblCopyright = new JTextArea(Messages
                .getString("CommonAboutDialog.copyright")); //$NON-NLS-1$
        lblCopyright.setEditable(false);
        lblCopyright.setOpaque(false);
        JTextArea lblAbout = new JTextArea(Messages
                .getString("CommonAboutDialog.about")); //$NON-NLS-1$
        lblAbout.setEditable(false);
        lblAbout.setOpaque(false);

        // Add a "Close" button.
        JButton butClose = new JButton(Messages
                .getString("CommonAboutDialog.Close")); //$NON-NLS-1$
        butClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                quit();
            }
        });

        // Layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(4, 4, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.ipadx = 10;
        c.ipady = 5;
        c.gridx = 0;
        c.gridy = 0;
        getContentPane().add(panTitle, c);
        c.weighty = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 1;
        getContentPane().add(lblVersion, c);
        c.gridy = 2;
        getContentPane().add(lblCopyright, c);
        c.gridy = 3;
        getContentPane().add(lblAbout, c);
        c.gridy = 4;
        getContentPane().add(butClose, c);

        // Place this dialog on middle of screen.
        Dimension screenSize = frame.getToolkit().getScreenSize();
        pack();
        setLocation(screenSize.width / 2 - getSize().width / 2,
                screenSize.height / 2 - getSize().height / 2);

        // Stop allowing resizing.
        setResizable(false);
    }

    /**
     * Close this dialog.
     */
    void quit() {
        setVisible(false);
    }
}
