/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import megamek.client.ui.Messages;

/**
 * A simple yes/no confirmation dialog.
 */
public class ConfirmDialog extends JDialog{

    private static final long serialVersionUID = -8491332593940944224L;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();

    private boolean useCheckbox;
    private JCheckBox botherCheckbox;

    private JPanel panButtons = new JPanel();
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
        finishSetup(p);
    }

    private void setupButtons() {
        Action yesAction = new AbstractAction() {
            private static final long serialVersionUID = -5442315938595454381L;

            public void actionPerformed(ActionEvent e) {
                confirmation = true;
                setVisible(false);
            }
        };
        butYes = new JButton(yesAction);
        butYes.setText(Messages.getString("Yes")); //$NON-NLS-1$

        KeyStroke ks = null;
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, 0);

        InputMap imap = butYes
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap amap = butYes.getActionMap();
        imap.put(ks, YESACTION);
        amap.put(YESACTION, yesAction);

        Action noAction = new AbstractAction() {
            private static final long serialVersionUID = -952830599469731009L;

            public void actionPerformed(ActionEvent e) {
                confirmation = false;
                setVisible(false);
            }
        };
        butNo = new JButton(noAction);
        butNo.setText(Messages.getString("No")); //$NON-NLS-1$

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
        c.gridheight = 2;
        c.insets = new Insets(5, 5, 5, 5);
        gridbag.setConstraints(questionLabel, c);
        getContentPane().add(questionLabel);
    }

    private void addInputs() {
        int y = 2;

        c.gridheight = 1;

        if (useCheckbox) {
            botherCheckbox = new JCheckBox(Messages
                    .getString("ConfirmDialog.dontBother")); //$NON-NLS-1$

            c.gridy = y++;
            gridbag.setConstraints(botherCheckbox, c);
            getContentPane().add(botherCheckbox);
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

        c.gridy = y;

        gridbag.setConstraints(panButtons, c);
        getContentPane().add(panButtons);
    }

    private void finishSetup(JFrame p) {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

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
            size = getSize();
        }
        setLocationRelativeTo(p);

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
