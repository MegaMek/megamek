/*
 * MegaMek - Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020-2023 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 * This is MegaMek's Help -> About dialog
 */
public class CommonAboutDialog extends JDialog {

    private static final String FILENAME_MEGAMEK_SPLASH2 = "megamek-splash2.gif";
    private static final MegaMekFile titleImageFile = new MegaMekFile(Configuration.miscImagesDir(), FILENAME_MEGAMEK_SPLASH2);
    private static Image imgTitleImage;

    /** @return loads and returns the MegaMek title image */
    private static synchronized Image getTitleImage() {
        if (imgTitleImage == null) {
            imgTitleImage = ImageUtil.loadImageFromFile(titleImageFile.toString());
            // ImageIcon makes sure it is fully loaded
            new ImageIcon(imgTitleImage);
        }
        return imgTitleImage;
    }

    /**
     * Creates the Help -> About dialog for MegaMek.
     *
     * @param parentFrame the parent JFrame for this dialog.
     */
    public CommonAboutDialog(JFrame parentFrame) {
        super(parentFrame, Messages.getString("CommonAboutDialog.title"), true);

        Image titleImage = getTitleImage();
        int scaledWidth = UIUtil.scaleForGUI(titleImage.getWidth(null));
        int scaledHeight = UIUtil.scaleForGUI(titleImage.getHeight(null));
        titleImage = ImageUtil.getScaledImage(titleImage, scaledWidth, scaledHeight);
        var titleImageLabel = new JLabel(new ImageIcon(titleImage));

        JTextArea lblVersion = new JTextArea(MegaMek.getUnderlyingInformation(MMConstants.PROJECT_NAME));
        lblVersion.setEditable(false);
        JTextArea lblCopyright = new JTextArea(Messages.getString("CommonAboutDialog.copyright"));
        lblCopyright.setEditable(false);
        JTextArea lblAbout = new JTextArea(Messages.getString("CommonAboutDialog.about"));
        lblAbout.setEditable(false);

        JButton closeButton = new ButtonEsc(new CloseAction(this));
        JButton copyButton = new DialogButton(Messages.getString("CommonAboutDialog.copy"));
        copyButton.addActionListener(e -> copySystemData());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        buttonPanel.setBorder(new UIUtil.ScaledEmptyBorder(25, 20, 10, 10));
        buttonPanel.add(closeButton);
        buttonPanel.add(copyButton);

        Box contentPanel = Box.createVerticalBox();
        contentPanel.setBorder(new UIUtil.ScaledEmptyBorder(35, 50, 0, 50));
        titleImageLabel.setAlignmentX(0.5f);
        contentPanel.add(titleImageLabel);
        contentPanel.add(UIUtil.scaledVerticalSpacer(35), BorderLayout.PAGE_START);
        contentPanel.add(lblVersion);
        contentPanel.add(UIUtil.scaledVerticalSpacer(15), BorderLayout.PAGE_START);
        contentPanel.add(lblCopyright);
        contentPanel.add(UIUtil.scaledVerticalSpacer(15), BorderLayout.PAGE_START);
        contentPanel.add(lblAbout);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.PAGE_END);

        // Size and center
        getRootPane().setDefaultButton(closeButton);
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
        pack();
        setLocationRelativeTo(parentFrame);
        setResizable(false);
    }

    private void copySystemData() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(MegaMek.getUnderlyingInformation(MMConstants.PROJECT_NAME)), null);
    }
}