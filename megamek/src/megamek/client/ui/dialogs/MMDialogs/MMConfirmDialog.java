/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.MMDialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.DialogButton;
import megamek.client.ui.dialogs.clientDialogs.ClientDialog;
import megamek.client.ui.util.UIUtil;

/**
 * A simple modal confirmation dialog showing a single question and YES and NO buttons. This dialog will scale with the
 * gui scaling (and I found no way to do this for JOptionDialog)
 *
 * @author Juliez
 */
public class MMConfirmDialog {

    private final static Dimension MINIMUM_SIZE = new Dimension(400, 180);
    private final static int BASE_WIDTH = 400;

    /**
     * Shows a modal confirmation dialog with a YES and a NO button. The String title is shown as the window title. The
     * given message is shown as the question to be confirmed in the center of the dialog.
     *
     * <BR><BR>Returns Response.YES when the user pressed ENTER or selected YES,
     * Response.NO otherwise.
     *
     * <BR><BR>The dialog scales itself with the current GUI scale. It closes when ESC is pressed.
     */
    public static boolean confirm(JFrame owner, String title, String message) {
        ConfirmDialog dialog = new ConfirmDialog(owner, title, message);
        dialog.center();
        dialog.setVisible(true);
        return dialog.userResponse;
    }

    // PRIVATE

    private static class ConfirmDialog extends ClientDialog {

        private static final long serialVersionUID = -2877691301521648979L;

        private boolean userResponse = false;

        private JPanel panButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        private JButton butYes = new DialogButton(Messages.getString("Yes"));
        private JButton butNo = new DialogButton(Messages.getString("No"));

        public ConfirmDialog(JFrame owner, String title, String message) {
            super(owner, title, true);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    respondNo();
                    super.windowClosed(e);
                }
            });
            add(panButtons, BorderLayout.PAGE_END);
            panButtons.add(butYes);
            panButtons.add(butNo);
            butYes.addActionListener(e -> respondYes());
            butNo.addActionListener(e -> respondNo());
            JLabel lblMain = new JLabel("<HTML>" + message.replace("\n", "<BR>"));
            lblMain.setVerticalAlignment(JLabel.CENTER);
            lblMain.setHorizontalAlignment(JLabel.CENTER);
            lblMain.setBorder(new EmptyBorder(0, 20, 0, 20));
            add(lblMain, BorderLayout.CENTER);
            setMinimumSize(MINIMUM_SIZE);
            center();
            // Make the dialog take ENTER as Yes and ESC as No
            getRootPane().setDefaultButton(butYes);
            addKeyListener(k);
            butYes.addKeyListener(k);
            butNo.addKeyListener(k);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension sSize = super.getPreferredSize();
            return new Dimension(UIUtil.scaleForGUI(BASE_WIDTH), sSize.height);
        }

        private void respondNo() {
            setVisible(false);
        }

        private void respondYes() {
            userResponse = true;
            setVisible(false);
        }

        KeyListener k = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    respondNo();
                }
            }
        };
    }
}
