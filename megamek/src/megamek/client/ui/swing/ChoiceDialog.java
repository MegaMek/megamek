/*
 * MegaMek - Copyright (C) 2002-2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;

/**
 * A (somewhat primitive) dialog that asks a question and lets the player select
 * from the available choices. The question string is tokenised on "\n". <p>
 * Refactored from SingleChoiceDialog (which was based on Confirm)
 * 
 * @author suvarov454@sourceforge.net
 */
public class ChoiceDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 3093043054221558221L;

    private boolean confirm;

    private JPanel panButtons = new JPanel();
    private JButton butSelectAll = new JButton(Messages.getString("ChoiceDialog.SelectAll"));
    private JButton butClearAll = new JButton(Messages.getString("ChoiceDialog.ClearAll"));
    private JButton butOK = new JButton(Messages.getString("Okay"));
    private JButton butCancel = new JButton(Messages.getString("Cancel"));

    /**
     * The checkboxes for available choices.
     */
    private AbstractButton[] checkboxes;

    //the maximum number of choices that can be made; -1 if no maximum
    private int maxChoices;
    
    /**
     * Create and initialize the dialog.
     * 
     * @param parent - the <code>Frame</code> that is locked by this dialog.
     * @param question - <code>String</code> displayed above the choices. The
     *            question string is tokenised on "\n".
     * @param choices - an array of <code>String</code>s to be displayed.
     * @param isSingle - a <code>boolean</code> that identifies whether the
     *            dialog is supposed to be a single choice dialog or support
     */
    private void initialize(JFrame parent, String question, String[] choices, boolean isSingle, int max) {
        super.setResizable(false);

        this.maxChoices = max;
        
        GridBagLayout gridbag = new GridBagLayout();
        getContentPane().setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;

        // Display the question in a scrollable, uneditable area.
        JTextArea message = new JTextArea(question, 5, 50);
        message.setEditable(false);
        message.setOpaque(false);

        c.gridy = 0;
        c.insets = new Insets(0, 5, 0, 5);
        getContentPane().add(new JScrollPane(message), c);

        // Do we have any choices?
        if (choices != null && choices.length > 0) {

            // Display the choices as a single column of radio buttons.
            // The first checkbox is selected by default.
            JPanel choiceArea = new JPanel(new GridLayout(0, 1));
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
                JPanel scrollee = new JPanel(new GridBagLayout());
                scrollee.add(choiceArea, center);
                JScrollPane scroller = new JScrollPane(scrollee);
                getContentPane().add(scroller, c);

                // Restore the saved value of c.fill.
                c.fill = saveFill;

            } else {
                getContentPane().add(choiceArea, c);
            }

            // Single choice dialogs use radio buttons.
            if (isSingle) {
                checkboxes = new JRadioButton[choices.length];
                ButtonGroup radioGroup = new ButtonGroup();
                for (int loop = 0; loop < choices.length; loop++) {
                    checkboxes[loop] = new JRadioButton(choices[loop],
                            loop == 0);
                    radioGroup.add(checkboxes[loop]);
                    choiceArea.add(checkboxes[loop]);
                }
            } else {
                // All others use check boxes.
                checkboxes = new JCheckBox[choices.length];
                for (int loop = 0; loop < choices.length; loop++) {
                    checkboxes[loop] = new JCheckBox(choices[loop], false);
                    checkboxes[loop].addActionListener(evt -> checkDisableChoices());
                    choiceArea.add(checkboxes[loop]);
                }
                
                // If this is not a single-choice dialog, place the
                // "select all" and "clear all" buttons in a row
                // under the scrollable area.
                GridLayout grid = new GridLayout(1, 0);
                grid.setHgap(20);
                JPanel panAllButtons = new JPanel(grid);
                panAllButtons.add(butSelectAll);
                butSelectAll.addActionListener(this);
                if (maxChoices != -1 && maxChoices < choices.length) {
                    butSelectAll.setEnabled(false);
                }
                panAllButtons.add(butClearAll);
                butClearAll.addActionListener(this);
                getContentPane().add(panAllButtons, center);
            }
        }
        
        // Allow the player to confirm or abort the choice.
        setupButtons();
        c.gridy++;
        c.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(panButtons, c);
        butOK.requestFocus();

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

    public void checkDisableChoices() {
        if (maxChoices == -1) {
            return;
        }
        if (countChoices() >= maxChoices) {
            for (int loop = 0; loop < checkboxes.length; loop++) {
                if (!checkboxes[loop].isSelected()) {
                    checkboxes[loop].setEnabled(false);
                }              
            }
        } else {
            for (int loop = 0; loop < checkboxes.length; loop++) {
                checkboxes[loop].setEnabled(true);           
            }
        }
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
     * @param max - the maximum number of choices that can be made
     */
    public ChoiceDialog(JFrame parent, String title, String question,
            String[] choices, boolean isSingle, int max) {
        super(parent, title, true);
        initialize(parent, question, choices, isSingle, max);
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
    public ChoiceDialog(JFrame parent, String title, String question,
            String[] choices, boolean isSingle) {
        super(parent, title, true);
        initialize(parent, question, choices, isSingle, -1);
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
     */
    public ChoiceDialog(JFrame parent, String title, String question,
            String[] choices) {
        super(parent, title, true);
        initialize(parent, question, choices, false, -1);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // No choices, no selection.
        if (checkboxes == null) {
            confirm = false;
            setVisible(false);
        } else if (e.getSource().equals(butSelectAll)) {
            for (final AbstractButton newVar : checkboxes) {
                newVar.setSelected(true);
            }
        } else if (e.getSource().equals(butClearAll)) {
            for (final AbstractButton newVar : checkboxes) {
                newVar.setSelected(false);
            }
        } else if (e.getSource().equals(butOK)) {
            confirm = true;
            setVisible(false);
        } else if (e.getSource().equals(checkboxes)) {
            
        } else {
            confirm = false;
            setVisible(false);
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
        return getChoices() != null;
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
        if (checkboxes != null && confirm) {

            // Make a temporary array that can hold all answers.
            int[] temp = new int[checkboxes.length];

            // Fill the temporary array.
            int index = 0;
            for (int loop = 0; loop < checkboxes.length; loop++) {
                if (checkboxes[loop].isSelected()) {
                    temp[index] = loop;
                    index++;
                }
            }

            // Do we need to shrink the array?
            if (checkboxes.length == index) {
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
    
    public int countChoices() {
        int index = 0;
        if (checkboxes != null) {
            for (int loop = 0; loop < checkboxes.length; loop++) {
                if (checkboxes[loop].isSelected()) {
                    index++;
                }
            }   
        }
        return index;
    }

} // End public class ChoiceDialog
