/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.MMConstants;
import megamek.client.ui.Messages;

/**
 * @author Ryan McConnell
 * @since June 15, 2003
 */
public class UnitFailureDialog extends JDialog implements ActionListener, ListSelectionListener, KeyListener {
    @Serial
    private static final long serialVersionUID = -7075012201265932299L;

    private final Map<String, String> hFailedFiles;

    private final JList<String> failedList;

    private final JTextArea reasonTextArea = new JTextArea("", 4, 20);

    public UnitFailureDialog(JFrame frame, Map<String, String> hff) {
        super(frame, Messages.getString("UnitFailureDialog.title"));

        hFailedFiles = hff;
        String[] failed = new String[hFailedFiles.size()];
        int i = 0;

        for (String string : hFailedFiles.keySet()) {
            failed[i++] = string;
        }
        failedList = new JList<>(failed);

        reasonTextArea.setEditable(false);
        reasonTextArea.setOpaque(false);
        reasonTextArea.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 12));
        failedList.addListSelectionListener(this);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(failedList), BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(reasonTextArea),
              BorderLayout.CENTER);

        setSize(400, 300);
        setLocation((frame.getLocation().x + (frame.getSize().width / 2))
              - (getSize().width / 2), (frame.getLocation().y
              + (frame.getSize().height / 2)) - (getSize().height / 2));

        JButton okButton = new JButton(Messages.getString("Okay"));
        okButton.addActionListener(this);

        getContentPane().add(okButton, BorderLayout.SOUTH);

        failedList.setSelectedIndex(0);

        reasonTextArea.setText(hFailedFiles.get(failedList.getSelectedValue()));

        setVisible(true);

        failedList.addKeyListener(this);
        reasonTextArea.addKeyListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        setVisible(false);
    }

    @Override
    public void valueChanged(ListSelectionEvent ie) {
        reasonTextArea.setText(hFailedFiles.get(failedList.getSelectedValue()));
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            setVisible(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent ke) {

    }

    @Override
    public void keyReleased(KeyEvent ke) {

    }
}
