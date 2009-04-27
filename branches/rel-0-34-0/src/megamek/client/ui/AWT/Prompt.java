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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import megamek.client.ui.Messages;

/**
 * A simple prompt.
 */
public class Prompt extends Dialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -6997067046923377859L;
    private Button butOk = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
    private TextField txtValue = new TextField();
    private Label lblText = new Label();
    private boolean ok = false;

    public Prompt(Frame parent, String title, String question,
            String default_text, int size) {
        super(parent, title, true);
        super.setResizable(false);

        txtValue.setColumns(size);
        txtValue.setText(default_text);
        setLayout(new BorderLayout());
        Panel qp = new Panel();
        qp.setLayout(new FlowLayout());
        lblText.setText(question);
        qp.add(lblText);
        qp.add(txtValue);
        add(qp, BorderLayout.CENTER);

        Panel p = new Panel();
        p.setLayout(new FlowLayout());
        butOk.addActionListener(this);
        p.add(butOk);
        butCancel.addActionListener(this);
        p.add(butCancel);
        add(p, BorderLayout.SOUTH);
        pack();
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - getSize().width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - getSize().height / 2);
    }

    public boolean showDialog() {
        setVisible(true);
        return ok;
    }

    public void actionPerformed(ActionEvent evt) {
        ok = evt.getSource() == butOk;
        setVisible(false);
        dispose();
    }

    public String getText() {
        return txtValue.getText();
    }

}
