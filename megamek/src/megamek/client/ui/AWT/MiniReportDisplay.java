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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import megamek.common.Settings;

/**
 * Shows a Report, with an Okay Button
 */
public class MiniReportDisplay extends Dialog 
    implements ActionListener
{
    private Button butOkay;
    private TextArea taData;

    public MiniReportDisplay(Frame parent, String sReport) {
        super(parent, "Turn Report", true);
        
        butOkay = new Button("Okay");
        butOkay.addActionListener(this);
        taData = new TextArea(sReport, 20, 48);
        taData.setEditable(false);

        setLayout(new BorderLayout());
        
        add(BorderLayout.SOUTH, butOkay);
        add(BorderLayout.CENTER, taData);
        setSize(Settings.miniReportSizeWidth, Settings.miniReportSizeHeight);
        doLayout();
        setLocation(Settings.miniReportPosX, Settings.miniReportPosY);

        // closing the window is the same as hitting butOkay
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    actionPerformed(new ActionEvent(butOkay,ActionEvent.ACTION_PERFORMED,butOkay.getLabel()));
                };
        });

        butOkay.requestFocus();
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == butOkay) {
            Settings.miniReportSizeWidth=getSize().width;
            Settings.miniReportSizeHeight=getSize().height;
            Settings.miniReportPosX=getLocation().x;
            Settings.miniReportPosY=getLocation().y;

            hide();
        }
    }
}
