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

package megamek.client.ui.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.Messages;
import megamek.common.BombType;

/**
 * A dialog to determine bomb payload Right now it is just for space bombing
 *
 * @author suvarov454@sourceforge.net
 * @version $version: $
 */
public class BombPayloadDialog extends JDialog implements ActionListener, ItemListener {
    /**
     *
     */
    private static final long serialVersionUID = -4629867982571421459L;

    private boolean confirm = false;
    private int limit;
    private int[] bombs;

    private JPanel panButtons = new JPanel();
    private JButton butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private JComboBox[] b_choices;
    private JLabel[] b_labels;

    /**
     * Create and initialize the dialog.
     *
     * @param parent
     *            - the <code>Frame</code> that is locked by this dialog.
     * @param title
     *            - the title <code>String</code> for this dialog.
     * @param question
     *            - <code>String</code> displayed above the choices. The
     *            question string is tokenised on "\n".
     * @param choices
     *            - an array of <code>String</code>s to be displayed.
     * @param isSingle
     *            - a <code>boolean</code> that identifies whether the dialog is
     *            supposed to be a single choice dialog or support multiple
     *            choices.
     */
    private void initialize(JFrame parent, String title, int[] b,
            boolean spaceBomb, boolean bombDump, int lim) {
        super.setResizable(false);

        bombs = b;
        limit = lim;

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;

        b_choices = new JComboBox[bombs.length];
        b_labels = new JLabel[bombs.length];
        //initialize the bomb choices
        for(int i = 0; i< bombs.length; i++) {
            b_choices[i] = new JComboBox();
            b_labels[i] = new JLabel(BombType.getBombName(i));
            int max = bombs[i];
            if((limit > -1) && (max > limit)) {
                max = limit;
            }
            for (int x = 0; x <= max; x++) {
                b_choices[i].addItem(Integer.toString(x));
            }
            b_choices[i].setSelectedIndex(0);
            b_choices[i].addItemListener(this);
            //only display eligible bomb drops
            if(spaceBomb && !BombType.canSpaceBomb(i)) {
                continue;
            }
            if(!spaceBomb && !bombDump && !BombType.canGroundBomb(i)) {
                continue;
            }
            if(bombs[i] == 0) {
                continue;
            }
            c.gridx = 0;
            c.gridy = i;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(b_labels[i], c);
            add(b_labels[i]);
            c.gridx = 1;
            c.gridy = i;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(b_choices[i], c);
            add(b_choices[i]);
        }

        // Allow the player to confirm or abort the choice.
        setupButtons();
        c.gridy++;
        c.insets = new Insets(5, 5, 5, 5);
        add(panButtons, c);
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

    /**
     * Create a choice dialog. The player can choose any or all of the choices.
     * If no choices are passed in, this will be a very boring dialog, but it
     * will not suffer an exception.
     *
     * @param parent
     *            - the <code>Frame</code> that is locked by this dialog.
     * @param title
     *            - the title <code>String</code> for this dialog.
     * @param question
     *            - <code>String</code> displayed above the choices. The
     *            question string is tokenised on "\n".
     * @param bombs
     *            - an array of <code>String</code>s the number of bombs of each
     *            type
     * @param isSingle
     *            - a <code>boolean</code> that identifies that
     */
    /* package */BombPayloadDialog(JFrame parent, String title, int[] bombs,
            boolean spaceBomb, boolean bombDump, int limit) {
        super(parent, title, true);
        initialize(parent, title, bombs, spaceBomb, bombDump, limit);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOK) {
            confirm = true;
            setVisible(false);
        } else {
            confirm = false;
            setVisible(false);
        }
    }

    public void itemStateChanged(ItemEvent ie) {

        if(limit < 0) {
            return;
        }

        int[] current = new int[b_choices.length];
        for(int i = 0; i < b_choices.length; i++) {
            current[i] = b_choices[i].getSelectedIndex();
        }

        //don't factor in your own choice when determining how much is left
        int[] left = new int[b_choices.length];
        for(int i = 0; i < left.length; i++) {
            left[i] = limit;
            for(int j = 0; j < current.length; j++) {
                if(i != j) {
                    left[i] -= current[j];
                }
            }
        }

        for(int i = 0; i < b_choices.length; i++) {
            b_choices[i].removeItemListener(this);
            b_choices[i].removeAllItems();
            int max = bombs[i];
            if(max > left[i]) {
                max = left[i];
            }
            for (int x = 0; x <= max; x++) {
                b_choices[i].addItem(Integer.toString(x));
            }
            b_choices[i].setSelectedIndex(current[i]);
            b_choices[i].addItemListener(this);
        }
    }

    /**
     * See if the player confirmed a choice.
     *
     * @return <code>true</code> if the player has confirmed a choice.
     *         <code>false</code> if the player canceled, if the player did not
     *         select a choice, or if no choices were available.
     */
    public boolean getAnswer() {
        return (null != getChoices());
    }

    /**
     * Which choices did the player select?
     *
     * @return If no choices were available, if the player canceled, if the
     *         player did not select a choice, or if the player canceled the
     *         choice, a <code>null</code> value is returned, otherwise an array
     *         of the <code>int</code> indexes from the input array that match
     *         the selected choices is returned.
     */
    public int[] getChoices() {

        int[] choices = null;
        if (confirm) {
            choices = new int[b_choices.length];
            for(int i = 0; i < b_choices.length; i++) {
                choices[i] = b_choices[i].getSelectedIndex();
            }
        }

        return choices;
    }

} // End public class ChoiceDialog
