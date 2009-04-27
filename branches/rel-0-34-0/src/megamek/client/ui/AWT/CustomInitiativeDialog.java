/*
 * MegaMek -
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import megamek.client.Client;
import megamek.client.ui.Messages;

public class CustomInitiativeDialog extends Dialog implements ActionListener,
        WindowListener {

    /**
     * 
     */
    private static final long serialVersionUID = -599945723220511118L;
    //private ClientGUI m_clientgui;
    private Client m_client;

    private Panel m_pParameters = new Panel();
    private Panel m_pButtons = new Panel();

    private Button m_bOK = new Button(Messages.getString("Okay"));
    private Button m_bCancel = new Button(Messages.getString("Cancel"));

    /*private Label m_labRound = new Label("");
    private Label m_labRoundTitle = new Label(Messages
            .getString("CustomInitiativeDialog.RoundTitle"));*/
    private Label m_labConstant = new Label(Messages
            .getString("CustomInitiativeDialog.ConstantBonus"));
    private TextField m_constant = new TextField(3);

    //private Vector<TextField> v_rounds = new Vector<TextField>();

    public CustomInitiativeDialog(ClientGUI cl) {
        super(cl.frame,
                Messages.getString("CustomInitiativeDialog.title"), true); //$NON-NLS-1$
        //m_clientgui = cl;
        m_client = cl.getClient();

        updateValues();

        // construct the parameters panel
        GridBagLayout layout = new GridBagLayout();
        m_pParameters.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.LINE_START;
        layout.setConstraints(m_labConstant, constraints);
        m_pParameters.add(m_labConstant);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(m_constant, constraints);
        m_pParameters.add(m_constant);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        /*
         * layout.setConstraints(m_labRoundTitle, constraints);
         * m_pParameters.add(m_labRoundTitle); for(int i = 0; i <
         * v_rounds.size(); i++) { constraints.gridwidth = 1; m_labRound = new
         * Label("Round " + Integer.toString(i+1));
         * layout.setConstraints(m_labRound, constraints);
         * m_pParameters.add(m_labRound); constraints.gridwidth =
         * GridBagConstraints.REMAINDER;
         * layout.setConstraints(v_rounds.elementAt(i), constraints);
         * m_pParameters.add(v_rounds.elementAt(i)); }
         */

        // construct the buttons panel
        m_pButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        m_pButtons.add(m_bOK);
        m_bOK.addActionListener(this);
        m_pButtons.add(m_bCancel);
        m_bCancel.addActionListener(this);

        // contruct the main dialog
        setLayout(new BorderLayout());
        add(m_pButtons, BorderLayout.SOUTH);
        add(m_pParameters, BorderLayout.WEST);
        validate();
        pack();
    }

    public void updateValues() {
        // set defaults
        // int[] turns = m_client.getLocalPlayer().getTurnInitBonus();
        m_constant.setText(Integer.toString(m_client.getLocalPlayer()
                .getConstantInitBonus()));
        // m_constant.setEditable(e);
        /*
         * v_rounds.clear(); for(int i = 0; i < turns.length; i++) { TextField
         * round = new TextField(3); round.setText(Integer.toString(turns[i]));
         * //round.setEditable(editable); v_rounds.add(round); }
         */
    }

    public void send() {
        if (m_client != null) {
            m_client.sendCustomInit(m_client.getLocalPlayer());
        }
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(m_bOK)) {
            int bonus = 0;
            if (m_constant.getText().trim().equals("")) { //$NON-NLS-1$
                bonus = 0; //$NON-NLS-1$
            } else {
                bonus = Integer.valueOf(m_constant.getText());
            }
            m_client.getLocalPlayer().setConstantInitBonus(bonus);
            /*
             * int[] turnBonus = new int[ v_rounds.size() ]; for(int i = 0; i <
             * v_rounds.size(); i++) { bonus = 0;
             * if(v_rounds.elementAt(i).getText().trim().equals("")) {
             * //$NON-NLS-1$ bonus = 0; //$NON-NLS-1$ } else { bonus =
             * Integer.valueOf(v_rounds.elementAt(i).getText()); } turnBonus[i] =
             * bonus; //m_client.getLocalPlayer().setTurnInitBonus(bonus,i); }
             * m_client.getLocalPlayer().setTurnInitBonus(turnBonus);
             */
            // send the change to the server
            send();
            // update player
            m_client.sendPlayerInfo();
            updateValues();
            setVisible(false);
        } else if (ev.getSource().equals(m_bCancel)) {
            updateValues();
            setVisible(false);
        }
    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowClosed(WindowEvent arg0) {
    }

    public void windowClosing(WindowEvent arg0) {
        this.setVisible(false);
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
    }

    public void windowIconified(WindowEvent arg0) {
    }

    public void windowOpened(WindowEvent arg0) {
    }

    public void setVisible(boolean show) {
        super.setVisible(show);
    }

    public void setClient(Client client) {
        this.m_client = client;
    }

}
