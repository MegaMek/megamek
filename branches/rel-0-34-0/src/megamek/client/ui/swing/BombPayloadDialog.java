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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.Messages;

/**
 * A dialog to determine bomb payload Right now it is just for space bombing
 * 
 * @author suvarov454@sourceforge.net
 * @version $version: $
 */
public class BombPayloadDialog extends JDialog implements ActionListener {
    /**
     *
     */
    private static final long serialVersionUID = -4629867982571421459L;

    private boolean confirm = false;

    private JPanel panButtons = new JPanel();
    private JButton butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private JComboBox b_choice_he;
    private JComboBox b_choice_cl;
    private JComboBox b_choice_lg;
    private JComboBox b_choice_inf;
    private JComboBox b_choice_mine;
    private JComboBox b_choice_tag;
    private JComboBox b_choice_arrow;
    private JComboBox b_choice_rl;
    private JComboBox b_choice_alamo;

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
    private void initialize(JFrame parent, String title, int[] bombs,
            boolean spaceBomb, boolean bombDump) {
        super.setResizable(false);

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;

        // add the bomb choices
        b_choice_he = new JComboBox();
        b_choice_cl = new JComboBox();
        b_choice_lg = new JComboBox();
        b_choice_inf = new JComboBox();
        b_choice_mine = new JComboBox();
        b_choice_tag = new JComboBox();
        b_choice_arrow = new JComboBox();
        b_choice_rl = new JComboBox();
        b_choice_alamo = new JComboBox();
        /*
         * for (int x = 0; x<=bombs[Aero.BOMB_HE]; x++) {
         * b_choice_he.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_CL]; x++) {
         * b_choice_cl.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_LG]; x++) {
         * b_choice_lg.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_INF]; x++) {
         * b_choice_inf.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_MINE]; x++) {
         * b_choice_mine.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_TAG]; x++) {
         * b_choice_tag.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_ARROW]; x++) {
         * b_choice_arrow.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_RL]; x++) {
         * b_choice_rl.add(Integer.toString(x)); }
         * 
         * for (int x = 0; x<=bombs[Aero.BOMB_ALAMO]; x++) {
         * b_choice_alamo.add(Integer.toString(x)); }
         */
        b_choice_he.setSelectedIndex(0);
        b_choice_cl.setSelectedIndex(0);
        b_choice_lg.setSelectedIndex(0);
        b_choice_inf.setSelectedIndex(0);
        b_choice_mine.setSelectedIndex(0);
        b_choice_tag.setSelectedIndex(0);
        b_choice_arrow.setSelectedIndex(0);
        b_choice_rl.setSelectedIndex(0);
        b_choice_alamo.setSelectedIndex(0);

        String heDesc = Messages.getString("CustomMechDialog.labBombHE");
        JLabel lhe = new JLabel(heDesc);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(lhe, c);
        add(lhe);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(b_choice_he, c);
        add(b_choice_he);

        String clDesc = Messages.getString("CustomMechDialog.labBombCL");
        JLabel lcl = new JLabel(clDesc);
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(lcl, c);
        add(lcl);
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(b_choice_cl, c);
        add(b_choice_cl);

        String lgDesc = Messages.getString("CustomMechDialog.labBombLG");
        JLabel llg = new JLabel(lgDesc);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(llg, c);
        add(llg);
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(b_choice_lg, c);
        add(b_choice_lg);

        if (!spaceBomb) {

            String infDesc = Messages.getString("CustomMechDialog.labBombInf");
            JLabel linf = new JLabel(infDesc);
            c.gridx = 0;
            c.gridy = 3;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(linf, c);
            add(linf);
            c.gridx = 1;
            c.gridy = 3;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(b_choice_inf, c);
            add(b_choice_inf);

            String mineDesc = Messages
                    .getString("CustomMechDialog.labBombMine");
            JLabel lmine = new JLabel(mineDesc);
            c.gridx = 0;
            c.gridy = 4;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(lmine, c);
            add(lmine);
            c.gridx = 1;
            c.gridy = 4;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(b_choice_mine, c);
            add(b_choice_mine);

            if (bombDump) {

                String tagDesc = Messages
                        .getString("CustomMechDialog.labBombTAG");
                JLabel ltag = new JLabel(tagDesc);
                c.gridx = 2;
                c.gridy = 0;
                c.anchor = GridBagConstraints.EAST;
                gridbag.setConstraints(ltag, c);
                add(ltag);
                c.gridx = 3;
                c.gridy = 0;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(b_choice_tag, c);
                add(b_choice_tag);

                String arrowDesc = Messages
                        .getString("CustomMechDialog.labBombArrow");
                JLabel larrow = new JLabel(arrowDesc);
                c.gridx = 2;
                c.gridy = 1;
                c.anchor = GridBagConstraints.EAST;
                gridbag.setConstraints(larrow, c);
                add(larrow);
                c.gridx = 3;
                c.gridy = 1;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(b_choice_arrow, c);
                add(b_choice_arrow);

                String rlDesc = Messages
                        .getString("CustomMechDialog.labBombRL");
                JLabel lrl = new JLabel(rlDesc);
                c.gridx = 2;
                c.gridy = 2;
                c.anchor = GridBagConstraints.EAST;
                gridbag.setConstraints(lrl, c);
                add(lrl);
                c.gridx = 3;
                c.gridy = 2;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(b_choice_rl, c);
                add(b_choice_rl);

                String alamoDesc = Messages
                        .getString("CustomMechDialog.labBombAlamo");
                JLabel lalamo = new JLabel(alamoDesc);
                c.gridx = 2;
                c.gridy = 3;
                c.anchor = GridBagConstraints.EAST;
                gridbag.setConstraints(lalamo, c);
                add(lalamo);
                c.gridx = 3;
                c.gridy = 3;
                c.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(b_choice_alamo, c);
                add(b_choice_alamo);
            }
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
            boolean spaceBomb, boolean bombDump) {
        super(parent, title, true);
        initialize(parent, title, bombs, spaceBomb, bombDump);
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
            /*
             * int[] temp = new int[ Aero.BOMB_NUM ]; temp[Aero.BOMB_HE] =
             * b_choice_he.getSelectedIndex(); temp[Aero.BOMB_CL] =
             * b_choice_cl.getSelectedIndex(); temp[Aero.BOMB_LG] =
             * b_choice_lg.getSelectedIndex(); temp[Aero.BOMB_INF] =
             * b_choice_inf.getSelectedIndex(); temp[Aero.BOMB_MINE] =
             * b_choice_mine.getSelectedIndex(); temp[Aero.BOMB_TAG] =
             * b_choice_tag.getSelectedIndex(); temp[Aero.BOMB_ARROW] =
             * b_choice_arrow.getSelectedIndex(); temp[Aero.BOMB_RL] =
             * b_choice_rl.getSelectedIndex(); temp[Aero.BOMB_ALAMO] =
             * b_choice_alamo.getSelectedIndex(); choices = temp;
             */
        }

        return choices;
    }

} // End public class ChoiceDialog
