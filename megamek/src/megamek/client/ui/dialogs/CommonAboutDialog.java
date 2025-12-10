/*
 * Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ButtonEsc;
import megamek.client.ui.buttons.DialogButton;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * This is MegaMek's Help -> About dialog
 */
public class CommonAboutDialog extends JDialog {

    private static final String FILENAME_MEGAMEK_SPLASH2 = "megamek-splash2.gif";
    private static final MegaMekFile titleImageFile = new MegaMekFile(Configuration.miscImagesDir(),
          FILENAME_MEGAMEK_SPLASH2);
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
        buttonPanel.setBorder(new EmptyBorder(25, 20, 10, 10));
        buttonPanel.add(closeButton);
        buttonPanel.add(copyButton);

        Box contentPanel = Box.createVerticalBox();
        contentPanel.setBorder(new EmptyBorder(35, 50, 0, 50));
        titleImageLabel.setAlignmentX(0.5f);
        contentPanel.add(titleImageLabel);
        contentPanel.add(Box.createVerticalStrut(35));
        contentPanel.add(lblVersion);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(lblCopyright);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(lblAbout);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.PAGE_END);

        // Size and center
        getRootPane().setDefaultButton(closeButton);
        pack();
        setLocationRelativeTo(parentFrame);
        setResizable(false);
    }

    private void copySystemData() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(MegaMek.getUnderlyingInformation(MMConstants.PROJECT_NAME)), null);
    }
}
