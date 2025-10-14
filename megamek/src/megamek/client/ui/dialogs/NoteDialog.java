/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;
import megamek.common.SpecialHexDisplay;

/**
 * A dialog for creating/editing a note that is attached to a hex via the
 * <code>SpecialHexDisplay</code> framework.
 *
 * @author arlith
 */
public class NoteDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -3126840102187553386L;

    private final JComboBox<String> visibility = new JComboBox<>();

    private final JTextArea noteText = new JTextArea("");

    private final JButton butDone = new JButton(Messages.getString("NoteDialog.Done"));

    boolean accepted = false;

    SpecialHexDisplay note;

    public NoteDialog(JFrame frame, SpecialHexDisplay note) {
        super(frame, Messages.getString("NoteDialog.title"), true);
        this.note = note;
        setResizable(false);
        butDone.addActionListener(this);
        JButton butCancel = new JButton(Messages.getString("NoteDialog.Cancel"));
        butCancel.addActionListener(this);

        JPanel layout;

        noteText.setLineWrap(true);
        noteText.setMinimumSize(new Dimension(getWidth(), 200));
        noteText.setPreferredSize(new Dimension(getWidth(), 200));

        JLabel noteLbl = new JLabel(Messages.getString("NoteDialog.note"));
        JLabel visibilityLbl = new JLabel(Messages.getString("NoteDialog.visibility"));

        visibility.addItem(Messages.getString("NoteDialog.owner"));
        visibility.addItem(Messages.getString("NoteDialog.team"));
        visibility.addItem(Messages.getString("NoteDialog.all"));
        visibility.setSelectedIndex(0);

        if (note != null) {
            noteText.setText(note.getInfo());
            visibility.setSelectedIndex(note.getObscuredLevel());
        }

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        getContentPane().add(noteLbl);
        getContentPane().add(noteText);

        layout = new JPanel();
        layout.add(visibilityLbl);
        layout.add(visibility);
        getContentPane().add(layout);

        layout = new JPanel();
        layout.add(butDone);
        layout.add(butCancel);
        getContentPane().add(layout);

        pack();
        setLocation(frame.getLocation().x + frame.getSize().width / 2
              - getSize().width / 2, frame.getLocation().y
              + frame.getSize().height / 2 - getSize().height / 2);
    }

    public boolean isAccepted() {
        return accepted;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(butDone)) {
            note.setInfo(noteText.getText());
            note.setObscuredLevel(visibility.getSelectedIndex());
            accepted = true;
        }
        setVisible(false);
    }
}
