/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * UnitFailureDialog.java
 *  Created by Ryan McConnell on June 15, 2003
 */

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.List;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;

import megamek.client.ui.Messages;

public class UnitFailureDialog extends Dialog implements ActionListener,
        ItemListener, KeyListener {

    /**
     * 
     */
    private static final long serialVersionUID = 7570434698437571985L;

    private Map<String, String> hFailedFiles;

    private List failedList = new List(10);

    private TextArea reasonTextArea = new TextArea(
            "", 4, 40, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$

    public UnitFailureDialog(Frame frame, Map<String, String> hff) {
        super(frame, Messages.getString("UnitFailureDialog.title")); //$NON-NLS-1$

        this.hFailedFiles = hff;
        Iterator<String> failedUnits = hFailedFiles.keySet().iterator();

        reasonTextArea.setEditable(false);
        failedList.addItemListener(this);

        setLayout(new BorderLayout());
        add(failedList, BorderLayout.NORTH);
        add(reasonTextArea, BorderLayout.CENTER);

        setSize(400, 300);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);

        Button okButton = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        okButton.addActionListener(this);

        add(okButton, BorderLayout.SOUTH);

        while (failedUnits.hasNext()) {
            failedList.add(failedUnits.next());
        }

        failedList.select(0);

        reasonTextArea.setText(hFailedFiles.get(failedList.getSelectedItem())
                .toString());

        setVisible(true);
        failedList.makeVisible(0); // why are you fighting me java?

        failedList.addKeyListener(this);
        reasonTextArea.addKeyListener(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }

    public void actionPerformed(ActionEvent actionEvent) {
        setVisible(false);
    }

    public void itemStateChanged(ItemEvent ie) {
        reasonTextArea.setText(hFailedFiles.get(failedList.getSelectedItem())
                .toString());
    }

    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            setVisible(false);
        }
    }

    public void keyTyped(KeyEvent ke) {

    }

    public void keyReleased(KeyEvent ke) {

    }
}
