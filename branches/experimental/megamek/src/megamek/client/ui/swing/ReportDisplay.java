/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.IGame;
import megamek.common.event.GamePhaseChangeEvent;

public class ReportDisplay extends StatusBarPhaseDisplay {
    /**
     *
     */
    private static final long serialVersionUID = 6185643976857892270L;

    // displays
    private JTabbedPane tabs;

    // buttons
    private JButton rerollInitiativeB;

    private boolean rerolled; // have we rerolled an init?

    /**
     * Creates and lays out a new movement phase display for the specified
     * clientgui.getClient().
     */
    public ReportDisplay(ClientGUI clientgui) {
        butDone = new MegamekButton();
        butDone.setActionCommand("doneButton");
        butDone.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -5034474968902280850L;

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("doneButton")) {
                    ready();
                }
            }
        });
        this.clientgui = clientgui;
        clientgui.getClient().game.addGameListener(this);

        // Create a tabbed panel to hold our reports.
        tabs = new JTabbedPane();

        Font tabPanelFont = new Font("Dialog", Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getInt("AdvancedChatLoungeTabFontSize"));
        tabs.setFont(tabPanelFont);

        resetTabs();

        setupStatusBar(""); //$NON-NLS-1$

        butDone.setText(Messages.getString("ReportDisplay.Done")); //$NON-NLS-1$

        rerollInitiativeB = new JButton(Messages.getString("ReportDisplay.Reroll")); //$NON-NLS-1$
        rerollInitiativeB.setActionCommand("reroll_initiative"); //$NON-NLS-1$
        rerollInitiativeB.addActionListener(this);

        // layout screen
        setLayout(new GridBagLayout());
        add(tabs, GBC.eol().fill(GridBagConstraints.BOTH));
        JPanel panButtons = new JPanel();
        panButtons.setLayout(new GridLayout(1, 8));
        panButtons.add(rerollInitiativeB);
        for (int padding = 0; padding < 6; padding++) {
            panButtons.add(new JLabel("")); //$NON-NLS-1$
        }
        add(panButtons, GBC.eol().fill(GridBagConstraints.HORIZONTAL));

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
        //butDone.setEnabled(false);
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
        if ((clientgui.getClient().game.getPhase() == IGame.Phase.PHASE_INITIATIVE_REPORT) && clientgui.getClient().game.hasTacticalGenius(clientgui.getClient().getLocalPlayer())) {
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
        if (tabs.indexOfTab("Round " + round) == -1) {
            // Need a new tab for the new round.

            // get rid of phase tab
            int phaseTab = tabs.indexOfTab("Phase");
            if (phaseTab >= 0) {
                tabs.removeTabAt(phaseTab);
            }
            if (phaseTab == -1) {
                phaseTab += 1; // special handling for round 0
            }

            // add as many round tabs as necessary to catch us up
            JTextPane ta;
            // TODO: we should remove the use of client
            for (int catchup = phaseTab + 1; catchup <= round; catchup++) {
                if (tabs.indexOfTab("Round " + catchup) != -1) {
                    ((JTextPane) ((JScrollPane) tabs.getComponentAt(tabs.indexOfTab("Round " + catchup))).getViewport().getView()).setText(
                            "<pre>" + clientgui.getClient().receiveReport(clientgui.getClient().game.getReports(catchup)) + "</pre>");
                    continue;
                }
                String text = roundText;
                if (catchup != round) {
                    text = clientgui.getClient().receiveReport(clientgui.getClient().game.getReports(catchup));
                }
                ta = new JTextPane();
                setupStylesheet(ta);
                ta.setText("<pre>" + text + "</pre>");
                ta.setEditable(false);
                ta.setOpaque(false);
                tabs.add("Round " + catchup, new JScrollPane(ta));
            }

            // add the new current phase tab
            ta = new JTextPane();
            setupStylesheet(ta);
            ta.setText("<pre>" + phaseText + "</pre>");
            ta.setEditable(false);
            ta.setOpaque(false);


            JScrollPane sp = new JScrollPane(ta);
            tabs.add("Phase", sp);
            tabs.setSelectedComponent(sp);
        } else {
            // Update the existing round tab and the phase tab.
            ((JTextPane) ((JScrollPane) tabs.getComponentAt(tabs.indexOfTab("Round " + round))).getViewport().getView()).setText("<pre>" + roundText + "</pre>");
            ((JTextPane) ((JScrollPane) tabs.getComponentAt(tabs.indexOfTab("Phase"))).getViewport().getView()).setText("<pre>" + phaseText + "</pre>");
        }
    }

    public static void setupStylesheet(JTextPane pane) {
        pane.setContentType("text/html");
        Font font = UIManager.getFont("Label.font");
        ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet().addRule(
               "pre { font-family: " + font.getFamily() + "; font-size: 12pt; font-style:normal;}");
    }

    public void appendReportTab(String additionalText) {
        int phaseTab = tabs.indexOfTab("Phase");
        if (phaseTab > 0) {
            JTextPane pane = ((JTextPane) ((JScrollPane) tabs.getComponentAt(phaseTab - 1)).getViewport().getView());
            pane.setText(pane.getText() + "<pre>"+additionalText+"</pre>");
        }
        JTextPane pane = ((JTextPane) ((JScrollPane) tabs.getComponentAt(phaseTab)).getViewport().getView());
        pane.setText(pane.getText() + "<pre>"+additionalText+"</pre>");
    }

    public void resetTabs() {
        tabs.removeAll();
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equalsIgnoreCase("reroll_initiative")) { //$NON-NLS-1$
            rerollInitiative();
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        setReportTab(clientgui.getClient().game.getRoundCount(), clientgui.getClient().roundReport, clientgui.getClient().phaseReport);
        resetButtons();
        rerolled = false;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int phaseTab = tabs.indexOfTab("Phase");
                if (phaseTab > 0) {
                    JViewport vp = ((JScrollPane) tabs.getComponentAt(phaseTab - 1)).getViewport();
                    vp.setViewPosition(new Point());
                }
                JViewport vp = ((JScrollPane) tabs.getComponentAt(phaseTab)).getViewport();
                vp.setViewPosition(new Point());
            }
        });
    }

    @Override
    public void clear() {
        // move along, move along, nothing to see here
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        clientgui.getClient().game.removeGameListener(this);
    }

}
