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
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import megamek.client.ui.Messages;

/**
 * A simple prompt.
 */
public class Slider extends Dialog implements ActionListener,
        AdjustmentListener {

    /**
     *
     */
    private static final long serialVersionUID = 6489454301676953500L;
    private Button butOk = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
    private Scrollbar value;
    private Label lblText = new Label();
    private boolean ok = false;
    private Label minText = new Label(), maxText = new Label(),
            curText = new Label();

    public Slider(Frame parent, String title, String question,
            int defaultValue, int min, int max) {
        super(parent, title, true);
        super.setResizable(false);

        value = new Scrollbar(Scrollbar.HORIZONTAL, defaultValue, 1, min,
                max + 1);
        value.addAdjustmentListener(this);

        setLayout(new BorderLayout());
        Panel qp = new Panel();
        qp.setLayout(new BorderLayout());
        lblText.setText(question);
        qp.add(lblText, BorderLayout.NORTH);
        add(qp, BorderLayout.NORTH);

        Panel sp1 = new Panel();
        sp1.setLayout(new FlowLayout());
        minText.setText(String.valueOf(min));
        maxText.setText(String.valueOf(max));
        curText.setText(String.valueOf(defaultValue));
        sp1.add(minText);
        sp1.add(value);
        sp1.add(maxText);
        sp1.add(curText);
        add(sp1, BorderLayout.CENTER);

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

    public int getValue() {
        return value.getValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.AdjustmentListener#adjustmentValueChanged(java.awt.event.AdjustmentEvent)
     */
    public void adjustmentValueChanged(AdjustmentEvent arg0) {
        curText.setText(String.valueOf(value.getValue()));
        pack();
    }

}
