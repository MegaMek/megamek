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

package megamek.client.ui.AWT;

import gov.nist.gui.TabPanel;

import java.awt.Button;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.IGame;
import megamek.common.event.GamePhaseChangeEvent;

public class ReportDisplay extends StatusBarPhaseDisplay implements
        ActionListener, KeyListener, DoneButtoned {
    /**
     * 
     */
    private static final long serialVersionUID = 6160984033049923873L;

    // parent game
    public Client client;

    // // chatterbox keeps track of chatting and other messages
    // private ChatterBox cb;

    // displays
    private TabPanel tabs;
    private Vector<TextArea> vTextArea;

    // buttons
    private Button readyB;
    private Button rerollInitiativeB;

    private boolean rerolled; // have we rerolled an init?

    /**
     * Creates and lays out a new movement phase display for the specified
     * client.
     */
    public ReportDisplay(Client client) {
        this.client = client;

        client.game.addGameListener(this);

        // cb = client.cb;

        // Create a tabbed panel to hold our reports.
        tabs = new TabPanel();
        // debugReport: add new client setting
        Font tabPanelFont = new Font("Dialog", Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getInt(
                        "AdvancedChatLoungeTabFontSize"));
        tabs.setTabFont(tabPanelFont);

        resetTabs();

        setupStatusBar(""); //$NON-NLS-1$

        readyB = new Button(Messages.getString("ReportDisplay.Done")); //$NON-NLS-1$
        readyB.setActionCommand("ready"); //$NON-NLS-1$
        readyB.addActionListener(this);

        rerollInitiativeB = new Button(Messages
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

        // c.gridwidth = 1;
        // c.weightx = 1.0; c.weighty = 0.0;
        // addBag(cb.getComponent(), gridbag, c);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        Panel panButtons = new Panel();
        panButtons.setLayout(new GridLayout(1, 8));
        panButtons.add(rerollInitiativeB);
        for (int padding = 0; padding < 6; padding++) {
            panButtons.add(new Label("")); //$NON-NLS-1$
        }
        addBag(panButtons, gridbag, c);

        // c.weightx = 1.0; c.weighty = 0.0;
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // addBag(panStatus, gridbag, c);

        // c.gridwidth = GridBagConstraints.REMAINDER;
        // c.weightx = 0.0; c.weighty = 0.0;
        // addBag(readyB, gridbag, c);

        addKeyListener(this);

    }

    private void addBag(Component comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    /**
     * Show or hide the "reroll inititiative" button in this report display.
     * 
     * @param show a <code>boolean</code> that indicates that the button
     *            should be shown in this report display.
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
        if (client.game.getPhase() == IGame.Phase.PHASE_INITIATIVE_REPORT
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
        if (round >= vTextArea.size()) {
            // Need a new tab for the new round.

            // get rid of phase tab
            tabs.remove(vTextArea.elementAt(vTextArea.size() - 1));
            vTextArea.removeElementAt(vTextArea.size() - 1);

            // add as many round tabs as necessary to catch us up
            TextArea ta;
            while (round > vTextArea.size()) {
                // HACK: We shouldn't have to rely on our access to the client
                // object...
                ta = new TextArea(client.receiveReport(client.game
                        .getReports(vTextArea.size() + 1)), 40, 25,
                        TextArea.SCROLLBARS_VERTICAL_ONLY);
                ta.setEditable(false);
                tabs.add("Round " + (vTextArea.size() + 1), ta);
                vTextArea.addElement(ta);
            }

            // add the new current phase tab
            ta = new TextArea(phaseText, 40, 25,
                    TextArea.SCROLLBARS_VERTICAL_ONLY);
            ta.setEditable(false);
            tabs.add("Phase", ta);
            vTextArea.addElement(ta);
            tabs.last();
        } else {
            // Update the previous rounds tab and the phase tab.
            vTextArea.elementAt(round - 1).setText(roundText);
            vTextArea.elementAt(round).setText(phaseText);
        }
    }

    public void appendReportTab(String additionalText) {
        vTextArea.elementAt(vTextArea.size() - 1).append(additionalText);
        vTextArea.elementAt(vTextArea.size() - 2).append(additionalText);
    }

    public void resetTabs() {
        tabs.removeAll();
        vTextArea = new Vector<TextArea>();
        /*
         * HACK: Without this initial empty TextArea, the tabs will be blank (no
         * TextArea at all) during the first initiative phase. I think it has
         * something to do with the layout manager, but I'm not really sure.
         * Maybe a strategically placed validate() would be better?
         */
        TextArea ta = new TextArea("", 40, 25,
                TextArea.SCROLLBARS_VERTICAL_ONLY);
        ta.setEditable(false);
        vTextArea.addElement(ta);
        tabs.add("Phase", ta);
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
        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
        }
        if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isControlDown()) {
            ready();
        }
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (this.isIgnoringEvents()) {
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
     * @return the <code>java.awt.Button</code> that activates this object's
     *         "Done" action.
     */
    public Button getDoneButton() {
        return readyB;
    }

    /**
     * Get the secondary display section of this phase.
     * 
     * @return the <code>Component</code> which is displayed in the secondary
     *         section during this phase.
     */
    public Component getSecondaryDisplay() {
        return panStatus;
    }

}
