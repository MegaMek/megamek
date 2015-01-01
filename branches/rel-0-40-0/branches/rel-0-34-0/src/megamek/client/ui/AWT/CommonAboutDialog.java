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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
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

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.AdvancedLabel;
import megamek.client.ui.AWT.widget.BackGroundDrawer;
import megamek.client.ui.AWT.widget.BufferedPanel;

/**
 * Every about dialog in MegaMek should have an identical look-and-feel.
 */
public class CommonAboutDialog extends Dialog {
    /**
     * 
     */
    private static final long serialVersionUID = 4295988339023189039L;
    /**
     * We only need a single copy of the "about" title image that we share.
     */
    private static Image imgTitleImage = null;

    /**
     * Get the single title image in a threadsafe way.
     * 
     * @param frame - a <code>Frame</code> object to instantiate the image.
     * @return the title <code>Image</code> common to all "about" dialogs.
     *         This value should <b>not</b> be <code>null</code>.
     */
    private static synchronized Image getTitleImage(Frame frame) {

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
     * @param frame - the parent <code>Frame</code> for this dialog.
     */
    public CommonAboutDialog(Frame frame) {
        // Construct the superclass.
        super(frame, Messages.getString("CommonAboutDialog.title")); //$NON-NLS-1$

        // Make sure we close at the appropriate times.
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        // Make a splash image panel.
        BufferedPanel panTitle = new BufferedPanel();
        Image imgSplash = CommonAboutDialog.getTitleImage(frame);
        BackGroundDrawer bgdTitle = new BackGroundDrawer(imgSplash);
        panTitle.addBgDrawer(bgdTitle);
        panTitle.setPreferredSize(imgSplash.getWidth(null), imgSplash
                .getHeight(null));

        // Make a label containing the version of this app.
        StringBuffer buff = new StringBuffer();
        buff.append(Messages.getString("CommonAboutDialog.version")) //$NON-NLS-1$
                .append(megamek.MegaMek.VERSION).append(
                        Messages.getString("CommonAboutDialog.timestamp")) //$NON-NLS-1$
                .append(new Date(megamek.MegaMek.TIMESTAMP).toString()).append(
                        Messages.getString("CommonAboutDialog.javaVendor")) //$NON-NLS-1$
                .append(System.getProperty("java.vendor")) //$NON-NLS-1$
                .append(Messages.getString("CommonAboutDialog.javaVersion")) //$NON-NLS-1$
                .append(System.getProperty("java.version")); //$NON-NLS-1$
        AdvancedLabel lblVersion = new AdvancedLabel(buff.toString());

        AdvancedLabel lblCopyright = new AdvancedLabel(Messages
                .getString("CommonAboutDialog.copyright")); //$NON-NLS-1$
        AdvancedLabel lblAbout = new AdvancedLabel(Messages
                .getString("CommonAboutDialog.about")); //$NON-NLS-1$

        // Add a "Close" button.
        Button butClose = new Button(Messages
                .getString("CommonAboutDialog.Close")); //$NON-NLS-1$
        butClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                quit();
            }
        });

        // Layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(gridbag);

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
        this.add(panTitle, c);

        c.weighty = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 1;
        this.add(lblVersion, c);

        c.gridy = 2;
        this.add(lblCopyright, c);

        c.gridy = 3;
        this.add(lblAbout, c);

        c.gridy = 4;
        this.add(butClose, c);

        // Place this dialog on middle of screen.
        Dimension screenSize = frame.getToolkit().getScreenSize();
        this.pack();
        this.setLocation(screenSize.width / 2 - this.getSize().width / 2,
                screenSize.height / 2 - this.getSize().height / 2);

        // Stop allowing resizing.
        this.setResizable(false);
    }

    /**
     * Close this dialog.
     */
    /* package */void quit() {
        this.setVisible(false);
    }

}
