/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import javax.swing.*;

import megamek.client.ui.Messages;

/**
 * A simple yes/no confirmation dialog.
 */
public class ConfirmDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = -8491332593940944224L;
    private final GridBagLayout gridbag = new GridBagLayout();
    private final GridBagConstraints gridBagConstraints = new GridBagConstraints();

    private final boolean useCheckbox;
    private JCheckBox botherCheckbox;

    private final JPanel panButtons = new JPanel();
    JButton butYes;
    JButton butNo;
    JButton defaultButton;

    private static final String YESACTION = "YesAction";
    private static final String NOACTION = "NoAction";

    boolean confirmation;

    JComponent firstFocusable;

    /**
     * Creates a new dialog window that lets the user answer Yes or No, with the
     * Yes button pre-focused
     *
     * @param title
     *            a title for the dialog window
     * @param question
     *            the text of the dialog
     */
    public ConfirmDialog(JFrame p, String title, String question) {
        this(p, title, question, false);
    }

    /**
     * Creates a new dialog window that lets the user answer Yes or No, with an
     * optional checkbox to specify future behaviour, and the Yes button
     * pre-focused
     *
     * @param title
     *            a title for the dialog window
     * @param question
     *            the text of the dialog
     * @param includeCheckbox
     *            whether the dialog includes a "bother me" checkbox for the
     *            user to tick
     */
    public ConfirmDialog(JFrame p, String title, String question,
            boolean includeCheckbox) {
        this(p, title, question, includeCheckbox, 'y');
    }

    /**
     * Creates a new dialog window that lets the user answer Yes or No, with an
     * optional checkbox to specify future behaviour, and either the Yes or No
     * button pre-focused
     *
     * @param title
     *            a title for the dialog window
     * @param question
     *            the text of the dialog
     * @param includeCheckbox
     *            whether the dialog includes a "bother me" checkbox for the
     *            user to tick
     * @param defButton
     *            set it to 'n' to make the No button pre-focused (Yes button is
     *            focused by default)
     */
    private ConfirmDialog(JFrame p, String title, String question,
            boolean includeCheckbox, char defButton) {
        super(p, title, true);

        super.setResizable(false);
        useCheckbox = includeCheckbox;

        setLayout(gridbag);
        addQuestion(question);
        setupButtons();
        addInputs();
        if (defButton == 'n') {
            defaultButton = butNo;
        } else {
            defaultButton = butYes;
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        finishSetup(p);
    }

    private void setupButtons() {
        Action yesAction = new AbstractAction() {
            @Serial
            private static final long serialVersionUID = -5442315938595454381L;

            @Override
            public void actionPerformed(ActionEvent e) {
                confirmation = true;
                setVisible(false);
            }
        };
        butYes = new JButton(yesAction);
        butYes.setText(Messages.getString("Yes"));
        butYes.setMnemonic(KeyEvent.VK_Y);
        KeyStroke ks = null;
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0);

        InputMap imap = butYes.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap amap = butYes.getActionMap();
        imap.put(ks, YESACTION);
        amap.put(YESACTION, yesAction);

        Action noAction = new AbstractAction() {
            @Serial
            private static final long serialVersionUID = -952830599469731009L;

            @Override
            public void actionPerformed(ActionEvent e) {
                confirmation = false;
                setVisible(false);
            }
        };
        butNo = new JButton(noAction);
        butNo.setText(Messages.getString("No"));
        butNo.setMnemonic(KeyEvent.VK_N);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_N, 0);
        imap = butNo.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        amap = butNo.getActionMap();
        imap.put(ks, NOACTION);
        amap.put(NOACTION, noAction);
    }

    private void addQuestion(String question) {
        JTextArea questionLabel = new JTextArea(question);
        questionLabel.setEditable(false);
        questionLabel.setOpaque(false);
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridbag.setConstraints(questionLabel, gridBagConstraints);
        add(questionLabel);
    }

    private void addInputs() {
        int y = 2;

        gridBagConstraints.gridheight = 1;

        if (useCheckbox) {
            botherCheckbox = new JCheckBox(Messages.getString("ConfirmDialog.dontBother"));

            gridBagConstraints.gridy = y++;
            gridbag.setConstraints(botherCheckbox, gridBagConstraints);
            add(botherCheckbox);
        }

        GridBagLayout buttonGridbag = new GridBagLayout();
        GridBagConstraints bc = new GridBagConstraints();
        panButtons.setLayout(buttonGridbag);
        bc.insets = new Insets(5, 5, 5, 5);
        bc.ipadx = 20;
        bc.ipady = 5;
        buttonGridbag.setConstraints(butYes, bc);
        panButtons.add(butYes);
        buttonGridbag.setConstraints(butNo, bc);
        panButtons.add(butNo);

        gridBagConstraints.gridy = y;

        gridbag.setConstraints(panButtons, gridBagConstraints);
        add(panButtons);
    }

    private void finishSetup(JFrame frame) {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        String closeAction = "closeAction";
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
        getRootPane().getActionMap().put(closeAction, new CloseAction(this));

        pack();

        Dimension size = getSize();
        boolean updateSize = false;
        if (size.width < GUIPreferences.getInstance().getMinimumSizeWidth()) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
            updateSize = true;
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
            updateSize = true;
        }
        if (updateSize) {
            setSize(size);
        }
        setLocationRelativeTo(frame);

        // work out which component will get the focus in the window
        if (useCheckbox) {
            firstFocusable = botherCheckbox;
        } else {
            firstFocusable = defaultButton;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            firstFocusable.requestFocus();
        }
        super.setVisible(visible);
    }

    public boolean getAnswer() {
        return confirmation;
    }

    public boolean getShowAgain() {
        if (botherCheckbox == null) {
            return true;
        }
        return !botherCheckbox.isSelected();
    }
}
