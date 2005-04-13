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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Hashtable;

public class UnitFailureDialog extends Dialog
    implements ActionListener, ItemListener, KeyListener {

    private Hashtable hFailedFiles;

    private List failedList = new List(10);

    private TextArea reasonTextArea = 
        new TextArea("",4,40,TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$

    public UnitFailureDialog(Frame frame, Hashtable hff) {
        super(frame,Messages.getString("UnitFailureDialog.title")); //$NON-NLS-1$

        this.hFailedFiles = hff;
        Enumeration failedUnits = hFailedFiles.keys();

        reasonTextArea.setEditable(false);
        failedList.addItemListener(this);

        setLayout(new BorderLayout());
        add(failedList, BorderLayout.NORTH);
        add(reasonTextArea, BorderLayout.CENTER);

        setSize(400,300);
        setLocation(frame.getLocation().x + frame.getSize().width/2 - getSize().width/2,
                    frame.getLocation().y + frame.getSize().height/2 - getSize().height/2);

        Button okButton = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        okButton.addActionListener(this);

        add(okButton, BorderLayout.SOUTH);

        while (failedUnits.hasMoreElements()) {
            failedList.add(failedUnits.nextElement().toString());
        }

        failedList.select(0);
        
        reasonTextArea.setText(hFailedFiles.get(failedList.getSelectedItem()).toString());
        
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
        reasonTextArea.setText(hFailedFiles.get(failedList.getSelectedItem()).toString());
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
