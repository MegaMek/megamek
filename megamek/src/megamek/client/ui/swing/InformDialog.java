/*
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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * A simple Inform dialog.
 * @author Luana Coppio
 */
public class InformDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = -8491332593940944311L;
    private final GridBagLayout gridBagLayout = new GridBagLayout();
    private final GridBagConstraints gridBagConstraints = new GridBagConstraints();

    private final boolean useCheckbox;
    private JCheckBox botherCheckbox;

    private final JPanel panButtons = new JPanel();
    JButton butOk;

    private static final String OK_ACTION = "OkAction";

    private JComponent firstFocusable;

    /**
     * Creates a new dialog window that informs the user of something and has an Ok button, with the
     * Ok button pre-focused, with an optional checkbox to specify future behaviour,
     * @param frame parent frame
     * @param title a title for the dialog window
     * @param message the text of the dialog
     * @param includeCheckbox whether the dialog includes a "bother me" checkbox for the user to tick
     */
    public InformDialog(JFrame frame, String title, String message,
                        boolean includeCheckbox) {
        super(frame, title, true);

        super.setResizable(false);
        useCheckbox = includeCheckbox;

        setLayout(gridBagLayout);
        addText(message);
        setupButtons();
        addInputs();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        finishSetup(frame);
    }

    private void setupButtons() {
        Action okAction = new AbstractAction() {
            @Serial
            private static final long serialVersionUID = -5442315938583454381L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        butOk = new JButton(okAction);
        butOk.setText(Messages.getString("Okay"));
        butOk.setMnemonic(KeyEvent.VK_Y);
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0);
        InputMap inputMap = butOk.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = butOk.getActionMap();
        inputMap.put(ks, OK_ACTION);
        actionMap.put(OK_ACTION, okAction);
    }

    private void addText(String message) {
        JTextArea label = new JTextArea(message);
        label.setEditable(false);
        label.setOpaque(false);
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagLayout.setConstraints(label, gridBagConstraints);
        add(label);
    }

    private void addInputs() {
        int y = 2;

        gridBagConstraints.gridheight = 1;

        if (useCheckbox) {
            botherCheckbox = new JCheckBox(Messages.getString("ConfirmDialog.dontBother"));

            gridBagConstraints.gridy = y++;
            gridBagLayout.setConstraints(botherCheckbox, gridBagConstraints);
            add(botherCheckbox);
        }

        GridBagLayout buttonGridBadLayout = new GridBagLayout();
        GridBagConstraints bc = new GridBagConstraints();
        panButtons.setLayout(buttonGridBadLayout);
        bc.insets = new Insets(5, 5, 5, 5);
        bc.ipadx = 20;
        bc.ipady = 5;
        buttonGridBadLayout.setConstraints(butOk, bc);
        panButtons.add(butOk);

        gridBagConstraints.gridy = y;

        this.gridBagLayout.setConstraints(panButtons, gridBagConstraints);
        add(panButtons);
    }

    private void finishSetup(JFrame frame) {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                dispose();
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
            firstFocusable = butOk;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            firstFocusable.requestFocus();
        }
        super.setVisible(visible);
    }

    public boolean getShowAgain() {
        if (botherCheckbox == null) {
            return true;
        }
        return !botherCheckbox.isSelected();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        InformDialog that = (InformDialog) o;

        return new EqualsBuilder().append(useCheckbox, that.useCheckbox)
                     .append(gridBagLayout, that.gridBagLayout)
                     .append(gridBagConstraints, that.gridBagConstraints)
                     .append(botherCheckbox, that.botherCheckbox)
                     .append(panButtons, that.panButtons)
                     .append(butOk, that.butOk)
                     .append(firstFocusable, that.firstFocusable)
                     .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(gridBagLayout)
                     .append(gridBagConstraints)
                     .append(useCheckbox)
                     .append(botherCheckbox)
                     .append(panButtons)
                     .append(butOk)
                     .append(firstFocusable)
                     .toHashCode();
    }
}
