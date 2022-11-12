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
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;

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
    private Client currentClient;
    private JTabbedPane tabs;

    private static final String MRD_TITLE = Messages.getString("MiniReportDisplay.title");
    private static final String MRD_ROUND = Messages.getString("MiniReportDisplay.Round");
    private static final String MRD_PHASE = Messages.getString("MiniReportDisplay.Phase");
    private static final String MRD_OKAY= Messages.getString("Okay");

    public MiniReportDisplay(JFrame parent, Client client) {
        super(parent, MRD_TITLE, false);

        currentClient = client;
        currentClient.getGame().addGameListener(gameListener);

        butOkay = new JButton(MRD_OKAY);
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
            savePrefHide();
        }
    }

    private void setupReportTabs(Client c) {
        tabs = new JTabbedPane();

        addReportPages();
        
        getContentPane().add(BorderLayout.CENTER, tabs);
    }

    public static void setupStylesheet(JTextPane pane) {
        pane.setContentType("text/html");
        Font font = UIManager.getFont("Label.font");
        ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet().addRule(
                "pre { font-family: " + font.getFamily()
                        + "; font-size: 12pt; font-style:normal;}");
    }

    private void savePrefHide() {
        GUIPreferences.getInstance().setMiniReportSizeWidth(getSize().width);
        GUIPreferences.getInstance().setMiniReportSizeHeight(getSize().height);
        GUIPreferences.getInstance().setMiniReportPosX(getLocation().x);
        GUIPreferences.getInstance().setMiniReportPosY(getLocation().y);
        setVisible(false);
    }

    public void addReportPages() {
        int numRounds = currentClient.getGame().getRoundCount();
        tabs.removeAll();

        for (int round = 1; round <= numRounds; round++) {
            String text = currentClient.receiveReport(currentClient.getGame().getReports(round));
            JTextPane ta = new JTextPane();
            setupStylesheet(ta);
            BASE64ToolKit toolKit = new BASE64ToolKit();
            ta.setEditorKit(toolKit);
            ta.setText("<pre>" + text + "</pre>");
            ta.setEditable(false);
            ta.setOpaque(false);
            tabs.add(MRD_ROUND + " " + round, new JScrollPane(ta));
        }

        // add the new current phase tab
        JTextPane ta = new JTextPane();
        setupStylesheet(ta);
        BASE64ToolKit toolKit = new BASE64ToolKit();
        ta.setEditorKit(toolKit);
        ta.setText("<pre>" + currentClient.roundReport + "</pre>");
        ta.setEditable(false);
        ta.setOpaque(false);

        JScrollPane sp = new JScrollPane(ta);
        tabs.add(MRD_PHASE, sp);

        tabs.setSelectedIndex(tabs.getTabCount() - 1);
    }

    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            switch (e.getOldPhase()) {
                case VICTORY:
                    savePrefHide();
                    break;
                default:
                    addReportPages();
            }
        }
    };
}
