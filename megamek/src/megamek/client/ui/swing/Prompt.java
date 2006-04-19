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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A simple prompt.
 */
public class Prompt extends JDialog implements ActionListener {

    private JButton butOk = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
    private JTextField txtValue = new JTextField();
    private JLabel lblText = new JLabel();
    private boolean ok = false;

    public Prompt(JFrame parent, String title, String question, String default_text, int size) {
        super(parent, title, true);
        super.setResizable(false);

        txtValue.setColumns(size);
        txtValue.setText(default_text);
        setLayout(new BorderLayout());
        JPanel qp = new JPanel();
        qp.setLayout(new FlowLayout());
        lblText.setText(question);
        qp.add(lblText);
        qp.add(txtValue);
        add(qp, BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout());
        butOk.addActionListener(this);
        p.add(butOk);
        butCancel.addActionListener(this);
        p.add(butCancel);
        add(p, BorderLayout.SOUTH);
        pack();
        setLocation(parent.getLocation().x + parent.getSize().width / 2 - getSize().width / 2,
                parent.getLocation().y + parent.getSize().height / 2 - getSize().height / 2);
    }

    public boolean showDialog() {
        setVisible(true);
        return ok;
    }

    public void actionPerformed(ActionEvent evt) {
        ok = evt.getSource().equals(butOk);
        setVisible(false);
        dispose();
    }

    public String getText() {
        return txtValue.getText();
    }

}
