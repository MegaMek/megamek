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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A simple yes/no confirmation dialog.
 */
public class ConfirmDialog
        extends JDialog implements ActionListener {

    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();

    private boolean useCheckbox;
    private JCheckBox botherCheckbox;

    private JPanel panButtons = new JPanel();
    private JButton butYes = new JButton(Messages.getString("Yes")); //$NON-NLS-1$
    private JButton butNo = new JButton(Messages.getString("No")); //$NON-NLS-1$
    private JButton defaultButton = butYes;

    private boolean confirmation;

    private JComponent firstFocusable;

    /**
     * Creates a new dialog window that lets the user answer Yes or No,
     * with the Yes button pre-focused
     *
     * @param title    a title for the dialog window
     * @param question the text of the dialog
     */
    public ConfirmDialog(JFrame p, String title, String question) {
        this(p, title, question, false);
    }

    /**
     * Creates a new dialog window that lets the user answer Yes or No,
     * with an optional checkbox to specify future behaviour,
     * and the Yes button pre-focused
     *
     * @param title           a title for the dialog window
     * @param question        the text of the dialog
     * @param includeCheckbox whether the dialog includes a "bother me" checkbox for the user to tick
     */
    public ConfirmDialog(JFrame p, String title, String question, boolean includeCheckbox) {
        this(p, title, question, includeCheckbox, 'y');
    }

    /**
     * Creates a new dialog window that lets the user answer Yes or No,
     * with an optional checkbox to specify future behaviour,
     * and either the Yes or No button pre-focused
     *
     * @param title           a title for the dialog window
     * @param question        the text of the dialog
     * @param includeCheckbox whether the dialog includes a "bother me" checkbox for the user to tick
     * @param defButton       set it to 'n' to make the No button pre-focused (Yes button is focused by default)
     */
    private ConfirmDialog(JFrame p, String title, String question, boolean includeCheckbox, char defButton) {
        super(p, title, true);

        if (defButton == 'n') {
            defaultButton = butNo;
        }

        super.setResizable(false);
        useCheckbox = includeCheckbox;

        setLayout(gridbag);
        addQuestion(question);
        addInputs();
        finishSetup(p);
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
            botherCheckbox = new JCheckBox(Messages.getString("ConfirmDialog.dontBother")); //$NON-NLS-1$

            c.gridy = y++;
            gridbag.setConstraints(botherCheckbox, c);
            getContentPane().add(botherCheckbox);
        }

        butYes.addActionListener(this);
        butNo.addActionListener(this);

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
        setLocation(p.getLocation().x + p.getSize().width / 2 - size.width / 2,
                p.getLocation().y + p.getSize().height / 2 - size.height / 2);


        // work out which component will get the focus in the window
        if (useCheckbox) {
            firstFocusable = botherCheckbox;
        } else {
            firstFocusable = butYes;
        }
        // we'd like the default button to have focus, but that can only be done on displayed
        // dialogs in Windows. So, this rather elaborate setup: as soon as the first focusable
        // component receives the focus, it shunts the focus to the OK button, and then
        // removes the FocusListener to prevent this happening again

        if (!firstFocusable.equals(defaultButton)) {
            firstFocusable.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    defaultButton.requestFocus();
                }

                public void focusLost(FocusEvent e) {
                    firstFocusable.removeFocusListener(this); // refers to listener
                }
            });
        }
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

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(butYes)) {
            confirmation = true;
        } else if (actionEvent.getSource().equals(butNo)) {
            confirmation = false;
        }
        setVisible(false);
    }
}
