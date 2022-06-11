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

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.BASE64ToolKit;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Shows reports, with an Okay JButton
 */
public class MiniReportDisplay extends JDialog implements ActionListener {
    private JButton butOkay;

    public MiniReportDisplay(JFrame parent, Client client) {
        super(parent, Messages.getString("MiniReportDisplay.title"), true);

        butOkay = new JButton(Messages.getString("Okay"));
        butOkay.addActionListener(this);
        
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(BorderLayout.SOUTH, butOkay);
        
        setupReportTabs(client);
                
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

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butOkay)) {
            GUIPreferences.getInstance().setMiniReportSizeWidth(getSize().width);
            GUIPreferences.getInstance().setMiniReportSizeHeight(getSize().height);
            GUIPreferences.getInstance().setMiniReportPosX(getLocation().x);
            GUIPreferences.getInstance().setMiniReportPosY(getLocation().y);
            setVisible(false);
        }
    }

    private void setupReportTabs(Client c) {
        JTabbedPane tabs = new JTabbedPane();

        int numRounds = c.getGame().getRoundCount();
        for (int round = 1; round < numRounds; round++) {
            String text = c.receiveReport(c.getGame().getReports(round));
            JTextPane ta = new JTextPane();
            setupStylesheet(ta);
            BASE64ToolKit toolKit = new BASE64ToolKit();
            ta.setEditorKit(toolKit);
            ta.setText("<pre>" + text + "</pre>");
            ta.setEditable(false);
            ta.setOpaque(false);
            tabs.add("Round " + round, new JScrollPane(ta));
        }

        // add the new current phase tab
        JTextPane ta = new JTextPane();
        setupStylesheet(ta);
        BASE64ToolKit toolKit = new BASE64ToolKit();
        ta.setEditorKit(toolKit);
        ta.setText("<pre>" + c.roundReport + "</pre>");
        ta.setEditable(false);
        ta.setOpaque(false);

        JScrollPane sp = new JScrollPane(ta);
        tabs.add("Phase", sp);
        tabs.setSelectedComponent(sp);
        
        getContentPane().add(BorderLayout.CENTER, tabs);
    }

    public static void setupStylesheet(JTextPane pane) {
        pane.setContentType("text/html");
        Font font = UIManager.getFont("Label.font");
        ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet().addRule(
                "pre { font-family: " + font.getFamily()
                        + "; font-size: 12pt; font-style:normal;}");
    }    
}
