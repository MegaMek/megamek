/**
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

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;

/**
 * Shows a Report, with an Okay JButton
 */
public class MiniReportDisplay extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -703103629596703945L;
    private JButton butOkay;
    private JTextArea taData;

    public MiniReportDisplay(JFrame parent, String sReport) {
        super(parent, Messages.getString("MiniReportDisplay.title"), true); //$NON-NLS-1$

        butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        butOkay.addActionListener(this);
        taData = new JTextArea(sReport, 20, 48);
        taData.setEditable(false);
        taData.setOpaque(false);

        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(BorderLayout.SOUTH, butOkay);
        getContentPane().add(BorderLayout.CENTER, taData);
        setSize(GUIPreferences.getInstance().getMiniReportSizeWidth(),
                GUIPreferences.getInstance().getMiniReportSizeHeight());
        doLayout();
        setLocation(GUIPreferences.getInstance().getMiniReportPosX(),
                GUIPreferences.getInstance().getMiniReportPosY());

        // closing the window is the same as hitting butOkay
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                actionPerformed(new ActionEvent(butOkay,
                        ActionEvent.ACTION_PERFORMED, butOkay.getText()));
            }
        });

        butOkay.requestFocus();
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butOkay)) {
            GUIPreferences.getInstance()
                    .setMiniReportSizeWidth(getSize().width);
            GUIPreferences.getInstance().setMiniReportSizeHeight(
                    getSize().height);
            GUIPreferences.getInstance().setMiniReportPosX(getLocation().x);
            GUIPreferences.getInstance().setMiniReportPosY(getLocation().y);

            setVisible(false);
        }
    }
}
