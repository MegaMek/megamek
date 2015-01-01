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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import megamek.client.ui.Messages;

/**
 * Shows a Report, with an Okay Button
 */
public class MiniReportDisplay extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -8035930786467770860L;
    private Button butOkay;
    private TextArea taData;

    public MiniReportDisplay(Frame parent, String sReport) {
        super(parent, Messages.getString("MiniReportDisplay.title"), true); //$NON-NLS-1$

        butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        butOkay.addActionListener(this);
        taData = new TextArea(sReport, 20, 48);
        taData.setEditable(false);

        setLayout(new BorderLayout());

        add(BorderLayout.SOUTH, butOkay);
        add(BorderLayout.CENTER, taData);
        setSize(GUIPreferences.getInstance().getMiniReportSizeWidth(),
                GUIPreferences.getInstance().getMiniReportSizeHeight());
        doLayout();
        setLocation(GUIPreferences.getInstance().getMiniReportPosX(),
                GUIPreferences.getInstance().getMiniReportPosY());

        // closing the window is the same as hitting butOkay
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionPerformed(new ActionEvent(butOkay,
                        ActionEvent.ACTION_PERFORMED, butOkay.getLabel()));
            }
        });

        butOkay.requestFocus();
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == butOkay) {
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
