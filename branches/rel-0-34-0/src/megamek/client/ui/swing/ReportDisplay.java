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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.IGame;
import megamek.common.event.GamePhaseChangeEvent;

public class ReportDisplay extends StatusBarPhaseDisplay implements
        KeyListener, DoneButtoned {
    /**
     *
     */
    private static final long serialVersionUID = 6185643976857892270L;

    // parent game
    public Client client;

    // displays
    private JTabbedPane tabs;

    // buttons
    private JButton readyB;
    private JButton rerollInitiativeB;

    private boolean rerolled; // have we rerolled an init?

    /**
     * Creates and lays out a new movement phase display for the specified
     * client.
     */
    public ReportDisplay(Client client) {
        this.client = client;

        client.game.addGameListener(this);

        // Create a tabbed panel to hold our reports.
        tabs = new JTabbedPane();

        Font tabPanelFont = new Font("Dialog", Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getInt(
                        "AdvancedChatLoungeTabFontSize"));
        tabs.setFont(tabPanelFont);

        resetTabs();

        setupStatusBar(""); //$NON-NLS-1$

        readyB = new JButton(Messages.getString("ReportDisplay.Done")); //$NON-NLS-1$
        readyB.setActionCommand("ready"); //$NON-NLS-1$
        readyB.addActionListener(this);

        rerollInitiativeB = new JButton(Messages
                .getString("ReportDisplay.Reroll")); //$NON-NLS-1$
        rerollInitiativeB.setActionCommand("reroll_initiative"); //$NON-NLS-1$
        rerollInitiativeB.addActionListener(this);

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(tabs, gridbag, c);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        JPanel panButtons = new JPanel();
        panButtons.setLayout(new GridLayout(1, 8));
        panButtons.add(rerollInitiativeB);
        for (int padding = 0; padding < 6; padding++) {
            panButtons.add(new JLabel("")); //$NON-NLS-1$
        }
        addBag(panButtons, gridbag, c);

        addKeyListener(this);

    }

    private void addBag(JComponent comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
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
    public void ready() {
        rerollInitiativeB.setEnabled(false);
        readyB.setEnabled(false);
        client.sendDone(true);
    }

    /**
     * Requests an initiative reroll and disables the ready button.
     */
    public void rerollInitiative() {
        rerolled = true;
        rerollInitiativeB.setEnabled(false);
        readyB.setEnabled(false);
        client.sendRerollInitiativeRequest();
    }

    /**
     * have we rerolled init this round?
     */
    public boolean hasRerolled() {
        return rerolled;
    }

    public void resetButtons() {
        resetReadyButton();
        if ((client.game.getPhase() == IGame.Phase.PHASE_INITIATIVE_REPORT)
                && client.game.hasTacticalGenius(client.getLocalPlayer())) {
            showRerollButton(true);
        } else {
            showRerollButton(false);
        }
        rerollInitiativeB.setEnabled(true);
    }

    public void resetReadyButton() {
        readyB.setEnabled(true);
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
            JTextArea ta;
            // TODO: we should remove the use of client
            for (int catchup = phaseTab + 1; catchup <= round; catchup++) {
                if (tabs.indexOfTab("Round " + catchup) != -1) {
                    ((JTextArea) ((JScrollPane) tabs.getComponentAt(tabs
                            .indexOfTab("Round " + catchup))).getViewport()
                            .getView()).setText(client
                            .receiveReport(client.game.getReports(catchup)));
                    continue;
                }
                String text = roundText;
                if (catchup != round) {
                    text = client
                            .receiveReport(client.game.getReports(catchup));
                }
                ta = new JTextArea(text, 40, 25);
                ta.setEditable(false);
                ta.setFont(new Font("Sans Serif", Font.PLAIN, 12));
                tabs.add("Round " + catchup, new JScrollPane(ta));
            }

            // add the new current phase tab
            ta = new JTextArea(phaseText, 40, 25);
            ta.setEditable(false);
            ta.setOpaque(false);
            ta.setFont(new Font("Sans Serif", Font.PLAIN, 12));
            JScrollPane sp = new JScrollPane(ta);
            tabs.add("Phase", sp);
            tabs.setSelectedComponent(sp);
        } else {
            // Update the existing round tab and the phase tab.
            ((JTextArea) ((JScrollPane) tabs.getComponentAt(tabs
                    .indexOfTab("Round " + round))).getViewport().getView())
                    .setText(roundText);
            ((JTextArea) ((JScrollPane) tabs.getComponentAt(tabs
                    .indexOfTab("Phase"))).getViewport().getView())
                    .setText(phaseText);
        }
    }

    public void appendReportTab(String additionalText) {
        int phaseTab = tabs.indexOfTab("Phase");
        if (phaseTab > 0) {
            ((JTextArea) ((JScrollPane) tabs.getComponentAt(phaseTab - 1))
                    .getViewport().getView()).append(additionalText);
        }
        ((JTextArea) ((JScrollPane) tabs.getComponentAt(phaseTab))
                .getViewport().getView()).append(additionalText);
    }

    public void resetTabs() {
        tabs.removeAll();
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equalsIgnoreCase("ready")) { //$NON-NLS-1$
            ready();
        }
        if (ev.getActionCommand().equalsIgnoreCase("reroll_initiative")) { //$NON-NLS-1$
            rerollInitiative();
        }
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if ((ev.getKeyCode() == KeyEvent.VK_ENTER) && ev.isControlDown()) {
            ready();
        }
    }

    public void keyReleased(KeyEvent ev) {
        // ignore
    }

    public void keyTyped(KeyEvent ev) {
        // ignore
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        setReportTab(client.game.getRoundCount(), client.roundReport,
                client.phaseReport);
        resetButtons();
        rerolled = false;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.game.removeGameListener(this);
    }

    /**
     * Retrieve the "Done" button of this object.
     * 
     * @return the <code>javax.swing.JButton</code> that activates this object's
     *         "Done" action.
     */
    public JButton getDoneButton() {
        return readyB;
    }

    /**
     * Get the secondary display section of this phase.
     * 
     * @return the <code>Component</code> which is displayed in the secondary
     *         section during this phase.
     */
    public JComponent getSecondaryDisplay() {
        return panStatus;
    }

}
