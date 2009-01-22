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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.client.ui.Messages;

/**
 * A simple prompt.
 */
public class Slider extends JDialog implements ActionListener, ChangeListener {

    /**
     *
     */
    private static final long serialVersionUID = -7823206132140091543L;
    private JButton butOk = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
    private JSlider value;
    private JLabel lblText = new JLabel();
    private boolean ok;
    private JLabel minText = new JLabel();
    private JLabel maxText = new JLabel();
    private JLabel curText = new JLabel();

    public Slider(JFrame parent, String title, String question,
            int defaultValue, int min, int max) {
        super(parent, title, true);
        super.setResizable(false);

        value = new JSlider(SwingConstants.HORIZONTAL, min, max, defaultValue);
        value.addChangeListener(this);

        getContentPane().setLayout(new BorderLayout());
        JPanel qp = new JPanel();
        qp.setLayout(new BorderLayout());
        lblText.setText(question);
        qp.add(lblText, BorderLayout.NORTH);
        getContentPane().add(qp, BorderLayout.NORTH);

        JPanel sp1 = new JPanel();
        sp1.setLayout(new FlowLayout());
        minText.setText(String.valueOf(min));
        maxText.setText(String.valueOf(max));
        curText.setText(String.valueOf(defaultValue));
        sp1.add(minText);
        sp1.add(value);
        sp1.add(maxText);
        sp1.add(curText);
        getContentPane().add(sp1, BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout());
        butOk.addActionListener(this);
        p.add(butOk);
        butCancel.addActionListener(this);
        p.add(butCancel);
        getContentPane().add(p, BorderLayout.SOUTH);
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
        ok = evt.getSource().equals(butOk);
        setVisible(false);
        dispose();
    }

    public int getValue() {
        return value.getValue();
    }

    public void stateChanged(ChangeEvent event) {
        curText.setText(String.valueOf(value.getValue()));
        pack();
        repaint();
    }
}
