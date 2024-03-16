/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.MMConstants;
import megamek.client.ui.Messages;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Ryan McConnell
 * @since June 15, 2003
 */
public class UnitFailureDialog extends JDialog implements ActionListener, ListSelectionListener, KeyListener {
    private static final long serialVersionUID = -7075012201265932299L;

    private Map<String, String> hFailedFiles;

    private JList<String> failedList;

    private JTextArea reasonTextArea = new JTextArea("", 4, 20);

    public UnitFailureDialog(JFrame frame, Map<String, String> hff) {
        super(frame, Messages.getString("UnitFailureDialog.title"));

        hFailedFiles = hff;
        String[] failed = new String[hFailedFiles.size()];
        int i = 0;
        Iterator<String> failedUnits = hFailedFiles.keySet().iterator();
        while (failedUnits.hasNext()) {
            failed[i++] = failedUnits.next();
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
