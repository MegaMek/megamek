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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.Mounted;

/**
 * A dialog to determine bomb payload
 * Right now it is just for space bombing
 *
 * @author  suvarov454@sourceforge.net
 * @version $version: $
 */
public class BombPayloadDialog
    extends Dialog implements ActionListener
{
    /**
     *
     */
    private static final long serialVersionUID = -9040626038371951886L;

    private boolean confirm = false;

    private Panel panButtons = new Panel();
    private Button butOK = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$

    private Checkbox[] b_choices;
    private Label[] b_labels;
    private int maxRows;

    /**
     * Create and initialize the dialog.
     *
     * @param   parent - the <code>Frame</code> that is locked by this dialog.
     * @param   title - the title <code>String</code> for this dialog.
     * @param   question - <code>String</code> displayed above the choices.
     *          The question string is tokenised on "\n".
     * @param   choices - an array of <code>String</code>s to be displayed.
     * @param   isSingle - a <code>boolean</code> that identifies whether the
     *          dialog is supposed to be a single choice dialog or support
     *          multiple choices.
     */
    private void initialize(Frame parent, String title, int[] bombs, boolean spaceBomb, boolean bombDump)
    {
        super.setResizable(false);

        //b_choices = new Checkbox[bombs.size()];
        //b_labels = new Label[bombs.size()];

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;

        int column = 0;
        int row = 0;
        int i = 0;
        /*
        for(Mounted bomb : bombs) {

            b_labels[i] = new Label();
            b_choices[i] = new Checkbox();

            b_choices[i].setState(false);
            b_labels[type].setText(BombType.getBombName(type));

            if(row >= maxRows) {
                row = 0;
                column += 2;
            }

            c.gridx = column;
            c.gridy = row;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(b_labels[type], c);
            add(b_labels[type]);

            c.gridx = column + 1;
            c.gridy = row;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(b_choices[type], c);
            add(b_choices[type]);
            row++;
            i++;
        }
        */
        // Allow the player to confirm or abort the choice.
        setupButtons();
        c.gridy++;
        c.insets = new Insets(5, 5, 5, 5);
        add(panButtons,c);
        butOK.requestFocus();

        addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) { setVisible(false); }
            });

        pack();
        Dimension size = getSize();
        boolean updateSize = false;
        if ( size.width < GUIPreferences.getInstance().getMinimumSizeWidth() ) {
            size.width = GUIPreferences.getInstance().getMinimumSizeWidth();
        }
        if ( size.height < GUIPreferences.getInstance().getMinimumSizeHeight() ) {
            size.height = GUIPreferences.getInstance().getMinimumSizeHeight();
        }
        if ( updateSize ) {
            setSize( size );
            size = getSize();
        }
        setLocation(parent.getLocation().x + parent.getSize().width/2 - size.width/2,
                    parent.getLocation().y + parent.getSize().height/2 - size.height/2);
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
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;

        c.gridwidth = 1;
        gridbag.setConstraints(butOK, c);
        panButtons.add(butOK);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }

    /**
     * Create a choice dialog.  The player can choose any or all of the
     * choices.  If no choices are passed in, this will be
     * a very boring dialog, but it will not suffer an exception.
     *
     * @param   parent - the <code>Frame</code> that is locked by this dialog.
     * @param   title - the title <code>String</code> for this dialog.
     * @param   question - <code>String</code> displayed above the choices.
     *          The question string is tokenised on "\n".
     * @param   bombs - an array of <code>String</code>s the number of bombs of each type
     * @param   isSingle - a <code>boolean</code> that identifies that
     */
    /* package */ BombPayloadDialog(Frame parent, String title, int[] bombs, boolean spaceBomb, boolean bombDump)
    {
        super(parent, title, true);
        initialize( parent, title, bombs, spaceBomb, bombDump);
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
     * @return  <code>true</code> if the player has confirmed a choice.
     *          <code>false</code> if the player canceled, if the player
     *          did not select a choice, or if no choices were available.
     */
    public boolean getAnswer() {
        return (null != getChoices());
    }

    /**
     * Which choices did the player select?
     *
     * @return  If no choices were available, if the player canceled, if the
     *          player did not select a choice, or if the player canceled the
     *          choice, a <code>null</code> value is returned, otherwise an
     *          array of the <code>int</code> indexes from the input array
     *          that match the selected choices is returned.
     */
    public Vector<Mounted> getChoices() {

        Vector<Mounted> choices = new Vector<Mounted>();
        /*
        if(this.confirm) {
            int[] temp = new int[ BombType.B_NUM ];
            for(int type = 0; type < BombType.B_NUM; type++) {
                temp[type] = b_choices[type].getSelectedIndex();
                int chosen = 0;
                for(Mounted bombs : getBombs()) {

                }
            }
            temp = choices;
        }
        */
        return choices;
    }

} // End public class ChoiceDialog
