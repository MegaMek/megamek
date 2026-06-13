/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
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
public class SliderDialog extends JDialog implements ActionListener, ChangeListener {
    @Serial
    private static final long serialVersionUID = -7823206132140091543L;
    private final JButton butOk = new JButton(Messages.getString("Okay"));
    private final JSlider value;
    private boolean ok;
    private final JLabel curText = new JLabel();

    public SliderDialog(JFrame parent, String title, String question,
          int defaultValue, int min, int max) {
        super(parent, title, true);
        super.setResizable(false);

        value = new JSlider(SwingConstants.HORIZONTAL, min, max, defaultValue);
        value.addChangeListener(this);

        getContentPane().setLayout(new BorderLayout());
        JPanel qp = new JPanel();
        qp.setLayout(new BorderLayout());
        JLabel lblText = new JLabel();
        lblText.setText(question);
        qp.add(lblText, BorderLayout.NORTH);
        getContentPane().add(qp, BorderLayout.NORTH);

        JPanel sp1 = new JPanel();
        sp1.setLayout(new FlowLayout());
        JLabel minText = new JLabel();
        minText.setText(String.valueOf(min));
        JLabel maxText = new JLabel();
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
        JButton butCancel = new JButton(Messages.getString("Cancel"));
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

    @Override
    public void actionPerformed(ActionEvent evt) {
        ok = evt.getSource().equals(butOk);
        setVisible(false);
        dispose();
    }

    public int getValue() {
        return value.getValue();
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        curText.setText(String.valueOf(value.getValue()));
        pack();
        repaint();
    }
}
