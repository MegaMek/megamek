/*
 * MegaMek - Copyright (C) 2002, 2003, 2004 Ben Mazur (bmazur@sev.org)
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
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import megamek.client.ui.Messages;

/**
 * A (somewhat primitive) dialog that asks a question and lets the player select
 * from the available choices. The question string is tokenised on "\n". <p/>
 * Refactored from SingleChoiceDialog (which was based on Confirm)
 * 
 * @author suvarov454@sourceforge.net
 * @version $version: $
 */
public class ChoiceDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -5672829093914553736L;

    private boolean confirm = false;

    private Panel panButtons = new Panel();
    private Button butSelectAll = new Button(Messages
            .getString("ChoiceDialog.SelectAll")); //$NON-NLS-1$
    private Button butClearAll = new Button(Messages
            .getString("ChoiceDialog.ClearAll")); //$NON-NLS-1$
    private Button butOK = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$

    /**
     * The checkboxes for available choices.
     */
    private Checkbox[] checkboxes = null;

    /**
     * Create and initialize the dialog.
     * 
     * @param parent - the <code>Frame</code> that is locked by this dialog.
     * @param title - the title <code>String</code> for this dialog.
     * @param question - <code>String</code> displayed above the choices. The
     *            question string is tokenised on "\n".
     * @param choices - an array of <code>String</code>s to be displayed.
     * @param isSingle - a <code>boolean</code> that identifies whether the
     *            dialog is supposed to be a single choice dialog or support
     *            multiple choices.
     */
    private void initialize(Frame parent, String title, String question,
            String[] choices, boolean isSingle) {
        super.setResizable(false);

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;

        // Display the question in a scrollable, uneditable area.
        TextArea message = new TextArea(question, 5, 50,
                TextArea.SCROLLBARS_VERTICAL_ONLY);
        message.setEditable(false);

        c.gridy = 0;
        c.insets = new Insets(0, 5, 0, 5);
        add(message, c);

        // Do we have any choices?
        if (choices != null && choices.length > 0) {

            // Display the choices as a single column of radio buttons.
            // The first checkbox is selected by default.
            Panel choiceArea = new Panel(new GridLayout(0, 1));
            c.gridy++;
            c.insets = new Insets(0, 5, 0, 5);

            // If there are many choices, display them in a scroll pane.
            GridBagConstraints center = new GridBagConstraints();
            center.anchor = GridBagConstraints.CENTER;
            if (choices.length > 5) {

                // Save the current value of c.fill; change it to HORIZONTAL.
                int saveFill = c.fill;
                c.fill = GridBagConstraints.HORIZONTAL;

                // Place the choice area in the center
                // of another panel that is scrolled.
                ScrollPane scroller = new ScrollPane();
                Panel scrollee = new Panel(new GridBagLayout());
                scrollee.add(choiceArea, center);
                scroller.add(scrollee);
                add(scroller, c);

                // Restore the saved value of c.fill.
                c.fill = saveFill;

            } else {
                add(choiceArea, c);
            }

            // Single choice dialogs use radio buttons.
            this.checkboxes = new Checkbox[choices.length];
            if (isSingle) {
                CheckboxGroup radioGroup = new CheckboxGroup();
                for (int loop = 0; loop < choices.length; loop++) {
                    this.checkboxes[loop] = new Checkbox(choices[loop],
                            (loop == 0), radioGroup);
                    choiceArea.add(this.checkboxes[loop]);
                }
            }

            // All others use check boxes.
            else {
                for (int loop = 0; loop < choices.length; loop++) {
                    this.checkboxes[loop] = new Checkbox(choices[loop],
                            (loop == 0));
                    choiceArea.add(this.checkboxes[loop]);
                }

                // If this is not a single-choice dialog, place the
                // "select all" and "clear all" buttons in a row
                // under the scrollable area.
                GridLayout grid = new GridLayout(1, 0);
                grid.setHgap(20);
                Panel panAllButtons = new Panel(grid);
                panAllButtons.add(butSelectAll);
                butSelectAll.addActionListener(this);
                panAllButtons.add(butClearAll);
                butClearAll.addActionListener(this);
                add(panAllButtons, center);
            }

        } // End have-choices

        // Allow the player to confirm or abort the choice.
        setupButtons();
        c.gridy++;
        c.insets = new Insets(5, 5, 5, 5);
        add(panButtons, c);
        butOK.requestFocus();

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
        }
        if (size.height < GUIPreferences.getInstance().getMinimumSizeHeight()) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
        }
        if (updateSize) {
            setSize(size);
            size = getSize();
        }
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - size.width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - size.height / 2);
    }

    private void setupButtons() {
        butOK.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 5, 5, 5);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;

        c.gridwidth = 1;
        gridbag.setConstraints(butOK, c);
        panButtons.add(butOK);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }

    /**
     * Create a choice dialog. The player can choose any or all of the choices.
     * If no choices are passed in, this will be a very boring dialog, but it
     * will not suffer an exception.
     * 
     * @param parent - the <code>Frame</code> that is locked by this dialog.
     * @param title - the title <code>String</code> for this dialog.
     * @param question - <code>String</code> displayed above the choices. The
     *            question string is tokenised on "\n".
     * @param choices - an array of <code>String</code>s to be displayed.
     * @param isSingle - a <code>boolean</code> that identifies that
     */
    /* package */ChoiceDialog(Frame parent, String title, String question,
            String[] choices, boolean isSingle) {
        super(parent, title, true);
        this.initialize(parent, title, question, choices, isSingle);
    }

    /**
     * Create a choice dialog. The player can choose any or all of the choices.
     * If no choices are passed in, this will be a very boring dialog, but it
     * will not suffer an exception.
     * 
     * @param parent - the <code>Frame</code> that is locked by this dialog.
     * @param title - the title <code>String</code> for this dialog.
     * @param question - <code>String</code> displayed above the choices. The
     *            question string is tokenised on "\n".
     * @param choices - an array of <code>String</code>s to be displayed.
     * @param isSingle - a <code>boolean</code> that identifies that
     */
    public ChoiceDialog(Frame parent, String title, String question,
            String[] choices) {
        super(parent, title, true);
        this.initialize(parent, title, question, choices, false);
    }

    public void actionPerformed(ActionEvent e) {
        // No choices, no selection.
        if (this.checkboxes == null) {
            confirm = false;
            this.setVisible(false);
        } else if (e.getSource() == butSelectAll) {
            for (int index = 0; index < this.checkboxes.length; index++) {
                this.checkboxes[index].setState(true);
            }
        } else if (e.getSource() == butClearAll) {
            for (int index = 0; index < this.checkboxes.length; index++) {
                this.checkboxes[index].setState(false);
            }
        } else if (e.getSource() == butOK) {
            confirm = true;
            this.setVisible(false);
        } else {
            confirm = false;
            this.setVisible(false);
        }
    }

    /**
     * See if the player confirmed a choice.
     * 
     * @return <code>true</code> if the player has confirmed a choice.
     *         <code>false</code> if the player canceled, if the player did
     *         not select a choice, or if no choices were available.
     */
    public boolean getAnswer() {
        return (null != this.getChoices());
    }

    /**
     * Which choices did the player select?
     * 
     * @return If no choices were available, if the player canceled, if the
     *         player did not select a choice, or if the player canceled the
     *         choice, a <code>null</code> value is returned, otherwise an
     *         array of the <code>int</code> indexes from the input array that
     *         match the selected choices is returned.
     */
    public int[] getChoices() {
        int[] retval = null;

        // Did the player make a choice?
        if (null != this.checkboxes && this.confirm) {

            // Make a temporary array that can hold all answers.
            int[] temp = new int[this.checkboxes.length];

            // Fill the temporary array.
            int index = 0;
            for (int loop = 0; loop < this.checkboxes.length; loop++) {
                if (this.checkboxes[loop].getState() == true) {
                    temp[index] = loop;
                    index++;
                }
            }

            // Do we need to shrink the array?
            if (this.checkboxes.length == index) {
                // No, the player selected all choices.
                retval = temp;
            } else if (index > 0) {
                // Yup. Create an array and copy the values from temp.
                retval = new int[index];
                System.arraycopy(temp, 0, retval, 0, index);
            }
            // If 0 == index, then we want to return a null array.
        }

        return retval;
    }

} // End public class ChoiceDialog
