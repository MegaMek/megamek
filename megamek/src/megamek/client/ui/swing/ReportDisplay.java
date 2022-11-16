/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.BASE64ToolKit;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.Entity;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ReportDisplay extends AbstractPhaseDisplay implements
        ActionListener, HyperlinkListener, IPreferenceChangeListener {
    private static final long serialVersionUID = 6185643976857892270L;

    // displays
    private JTabbedPane tabs;

    // buttons
    private JButton rerollInitiativeB;
    private JSplitPane sp;

    private static final String RD_ACTIONCOMMAND_DONEBUTTON = "doneButton";
    private static final String RD_ACTIONCOMMAND_REROLLINITIATIVE = "reroll_initiative";

    private static final String MSG_DONE = Messages.getString("ReportDisplay.Done");
    private static final String MSG_REROLL = Messages.getString("ReportDisplay.Reroll");
    private static final String MSG_ROUND = Messages.getString("ReportDisplay.Round");
    private static final String MSG_PHASE = Messages.getString("ReportDisplay.Phase");
    private static final String MSG_DETAILS =Messages.getString("ReportDisplay.Details");

    private boolean rerolled; // have we rerolled an init?

    /**
     * Creates and lays out a new movement phase display for the specified
     * clientgui.getClient().
     */
    public ReportDisplay(ClientGUI clientgui) {
        super(clientgui);
        butDone = new MegamekButton("",
                SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp());
        UIUtil.scaleComp(butDone, UIUtil.FONT_SCALE1);
        butDone.setActionCommand(RD_ACTIONCOMMAND_DONEBUTTON);
        butDone.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -5034474968902280850L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(RD_ACTIONCOMMAND_DONEBUTTON)) {
                    ready();
                }
            }
        });
        clientgui.getClient().getGame().addGameListener(this);

        // Create a tabbed panel to hold our reports.
        tabs = new JTabbedPane();

        resetTabs();

        butDone.setText(MSG_DONE);

        rerollInitiativeB = new JButton(MSG_REROLL);
        rerollInitiativeB.setActionCommand(RD_ACTIONCOMMAND_REROLLINITIATIVE);
        rerollInitiativeB.addActionListener(this);

        // layout screen
        setLayout(new BorderLayout());
        sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sp.setDividerSize(15);
        sp.setResizeWeight(0.95);
        JPanel p = new JPanel(new GridBagLayout());
        p.add(tabs, GBC.eol().fill(GridBagConstraints.BOTH));

        JPanel panButtons = new JPanel();
        panButtons.setLayout(new GridLayout(1, 8));
        panButtons.add(rerollInitiativeB);
        for (int padding = 0; padding < 6; padding++) {
            panButtons.add(new JLabel(""));
        }
        p.add(panButtons, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        sp.setTopComponent(p);
        add(sp);

        adaptToGUIScale();
        GUIPreferences.getInstance().addPreferenceChangeListener(this);
    }

    public void setBottom(JComponent comp) {
        sp.setBottomComponent(comp);
    }

    /**
     * Show or hide the "reroll inititiative" button in this report display.
     *
     * @param show
     *            a <code>boolean</code> that indicates that the button should
     *            be shown in this report display.
     */
    public void showRerollButton(boolean show) {
        rerollInitiativeB.setVisible(show);
    }

    /**
     * Sets you as ready and disables the ready button.
     */
    @Override
    public void ready() {
        rerollInitiativeB.setEnabled(false);
        butDone.setEnabled(false);
        clientgui.getClient().sendDone(true);
    }

    /**
     * Requests an initiative reroll and disables the ready button.
     */
    public void rerollInitiative() {
        rerolled = true;
        rerollInitiativeB.setEnabled(false);
        // butDone.setEnabled(false);
        clientgui.getClient().sendRerollInitiativeRequest();
    }

    /**
     * have we rerolled init this round?
     */
    public boolean hasRerolled() {
        return rerolled;
    }

    public void resetButtons() {
        resetReadyButton();
        if ((clientgui.getClient().getGame().getPhase() == GamePhase.INITIATIVE_REPORT) && clientgui.getClient().getGame().hasTacticalGenius(clientgui.getClient().getLocalPlayer())) {
            showRerollButton(true);
        } else {
            showRerollButton(false);
        }
        rerollInitiativeB.setEnabled(true);
    }

    public void resetReadyButton() {
        butDone.setEnabled(true);
    }

    public void resetRerollButton() {
        rerollInitiativeB.setEnabled(true);
    }

    public void setReportTab(int round, String roundText, String phaseText) {
        if (round == 0) {
            // The deployment reports (round 0) are combined with round one's
            // report.
            round = 1;
        }
        if (tabs.indexOfTab(MSG_ROUND + " " + round) == -1) {
            // Need a new tab for the new round.

            // get rid of phase tab
            int phaseTab = tabs.indexOfTab(MSG_PHASE);
            if (phaseTab >= 0) {
                tabs.removeTabAt(phaseTab);
            }
            if (phaseTab == -1) {
                phaseTab += 1; // special handling for round 0
            }

            // add as many round tabs as necessary to catch us up
            JTextPane ta;
            // TODO: we should remove the use of client
            final Client client = clientgui.getClient();
            for (int catchup = phaseTab + 1; catchup <= round; catchup++) {
                if (tabs.indexOfTab(MSG_ROUND + " " + catchup) != -1) {
                    ((JTextPane) ((JScrollPane) tabs.getComponentAt(tabs
                            .indexOfTab(MSG_ROUND + " " + catchup))).getViewport()
                            .getView()).setText("<pre>"
                            + client.receiveReport(client.getGame().getReports(
                                    catchup)) + "</pre>");
                    continue;
                }
                String text = roundText;
                if (catchup != round) {
                    text = client.receiveReport(client.getGame().getReports(catchup));
                }
                ta = new JTextPane();
                ta.addHyperlinkListener(this);
                setupStylesheet(ta);
                BASE64ToolKit toolKit = new BASE64ToolKit();
                ta.setEditorKit(toolKit);
                ta.setText("<pre>" + text + "</pre>");
                ta.setEditable(false);
                ta.setOpaque(false);
                tabs.add(MSG_ROUND + " " + catchup, new JScrollPane(ta));
            }

            // add the new current phase tab
            ta = new JTextPane();
            ta.addHyperlinkListener(this);
            setupStylesheet(ta);
            BASE64ToolKit toolKit = new BASE64ToolKit();
            ta.setEditorKit(toolKit);
            ta.setText("<pre>" + phaseText + "</pre>");
            ta.setEditable(false);
            ta.setOpaque(false);


            JScrollPane sp = new JScrollPane(ta);
            tabs.add(MSG_PHASE, sp);
            tabs.setSelectedComponent(sp);
        } else {
            // Update the existing round tab and the phase tab.
            ((JTextPane) ((JScrollPane) tabs.getComponentAt(tabs.indexOfTab(MSG_ROUND + " " + round))).getViewport().getView()).setText("<pre>" + roundText + "</pre>");
            ((JTextPane) ((JScrollPane) tabs.getComponentAt(tabs.indexOfTab(MSG_PHASE))).getViewport().getView()).setText("<pre>" + phaseText + "</pre>");
        }
    }

    public static void setupStylesheet(JTextPane pane) {
        pane.setContentType("text/html");
        StyleSheet styleSheet = ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet();
        Report.setupStylesheet(styleSheet);
    }
    
    public void appendReportTab(String additionalText) {
        int phaseTab = tabs.indexOfTab(MSG_PHASE);
        if (phaseTab > 0) {
            JTextPane pane = ((JTextPane) ((JScrollPane) tabs.getComponentAt(phaseTab - 1)).getViewport().getView());
            BASE64ToolKit toolKit = new BASE64ToolKit();
            pane.setEditorKit(toolKit);
            pane.setText(pane.getText() + "<pre>"+additionalText+"</pre>");
        }
        JTextPane pane = ((JTextPane) ((JScrollPane) tabs.getComponentAt(phaseTab)).getViewport().getView());
        BASE64ToolKit toolKit = new BASE64ToolKit();
        pane.setEditorKit(toolKit);
        pane.setText(pane.getText() + "<pre>"+additionalText+"</pre>");
    }

    public void resetTabs() {
        tabs.removeAll();
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equalsIgnoreCase(RD_ACTIONCOMMAND_REROLLINITIATIVE)) {
            rerollInitiative();
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        setReportTab(clientgui.getClient().getGame().getRoundCount(), clientgui.getClient().roundReport, clientgui.getClient().phaseReport);
        resetButtons();
        rerolled = false;

        SwingUtilities.invokeLater(() -> {
            int phaseTab = tabs.indexOfTab(MSG_PHASE);
            if (phaseTab > 0) {
                JViewport vp = ((JScrollPane) tabs.getComponentAt(phaseTab - 1)).getViewport();
                vp.setViewPosition(new Point());
            }
            JViewport vp = ((JScrollPane) tabs.getComponentAt(phaseTab)).getViewport();
            vp.setViewPosition(new Point());
        });
    }
    
    public void clear() {
        // move along, move along, nothing to see here
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        GUIPreferences.getInstance().removePreferenceChangeListener(this);
    }

    private JComponent activePane() {
        return (JComponent) ((JScrollPane) tabs.getSelectedComponent()).getViewport().getView();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent evt) {
        String evtDesc = evt.getDescription();
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (evtDesc.startsWith(Report.ENTITY_LINK)) {
                String idString = evtDesc.substring(Report.ENTITY_LINK.length());
                int id;
                try {
                    id = Integer.parseInt(idString);
                } catch (Exception ex) {
                    id = -1;
                }
                Entity ent = clientgui.getClient().getGame().getEntity(id);
                if (ent != null) {
                    clientgui.getUnitDisplay().displayEntity(ent);
                    clientgui.setUnitDisplayVisible(true);
                }
            } else if (evtDesc.startsWith(Report.TOOLTIP_LINK)) {
                String desc = evtDesc.substring(Report.TOOLTIP_LINK.length());
                JOptionPane.showMessageDialog(clientgui, desc, Messages.getString(MSG_DETAILS),
                        JOptionPane.PLAIN_MESSAGE);
            }
        } else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            if (evtDesc.startsWith(Report.TOOLTIP_LINK)) {
                String desc = evtDesc.substring(Report.TOOLTIP_LINK.length());
                activePane().setToolTipText(desc);
            }
        } else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
            activePane().setToolTipText(null);
        }
    }
    private void adaptToGUIScale() {
        UIUtil.scaleComp(sp, UIUtil.FONT_SCALE1);

        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component cp = tabs.getComponentAt(i);
            if (cp instanceof JScrollPane) {
                Component pane = ((JScrollPane) cp).getViewport().getView();
                if (pane instanceof JTextPane) {
                    JTextPane tp = (JTextPane) pane;
                    setupStylesheet(tp);
                    tp.setText(tp.getText());
                }
            }
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        } 
    }

}
