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

/**
 * Displays the info for a mech.  This is also a sort
 * of interface for special movement and firing actions.
 */
public class MiniReportDisplay extends Dialog 
    implements ActionListener
{
    private Button butOkay;
    private TextArea taData;

    public MiniReportDisplay(Frame f, String sReport) {
        super(f, "Turn Report", true);
        
        taData = new TextArea(sReport, 20, 48);
        taData.setEditable(false);
        butOkay = new Button("Okay");
        butOkay.addActionListener(this);
        setLayout(new BorderLayout());
        
        add(BorderLayout.CENTER, taData);
        add(BorderLayout.SOUTH, butOkay);
        setSize(200, 300);
        setLocation(100, 100);
        doLayout();
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == butOkay) {
            hide();
        }
    }
}
