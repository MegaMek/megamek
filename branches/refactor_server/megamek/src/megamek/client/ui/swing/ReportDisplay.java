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

import megamek.client.Client;
import megamek.common.IGame;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class ReportDisplay
        extends StatusBarPhaseDisplay
        implements ActionListener, KeyListener, DoneButtoned, Distractable {
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // parent game
    public Client client;
    
//     // chatterbox keeps track of chatting and other messages
//     private ChatterBox        cb;
    
    // displays
    private JTabbedPane tabs;
    private ArrayList<JScrollPane> vTextArea;

    // buttons
    private JButton readyB;
    private JButton rerollInitiativeB;

    private boolean rerolled; //have we rerolled an init?

    /**
     * Creates and lays out a new movement phase display
     * for the specified client.
     */
    public ReportDisplay(Client client) {
        this.client = client;

        client.game.addGameListener(this);

//         cb = client.cb;

        // Create a tabbed panel to hold our reports.
        tabs = new JTabbedPane();
        //debugReport: add new client setting
        Font tabPanelFont = new Font("Dialog", Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getInt("AdvancedChatLoungeTabFontSize"));
        tabs.setFont(tabPanelFont);

        resetTabs();

        setupStatusBar(""); //$NON-NLS-1$

        readyB = new JButton(Messages.getString("ReportDisplay.Done")); //$NON-NLS-1$
        readyB.setActionCommand("ready"); //$NON-NLS-1$
        readyB.addActionListener(this);

        rerollInitiativeB = new JButton(Messages.getString("ReportDisplay.Reroll")); //$NON-NLS-1$
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

//         c.gridwidth = 1;
//         c.weightx = 1.0;    c.weighty = 0.0;
//         addBag(cb.getComponent(), gridbag, c);

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

//         c.weightx = 1.0;    c.weighty = 0.0;
//         c.gridwidth = GridBagConstraints.REMAINDER;
//         addBag(panStatus, gridbag, c);

//         c.gridwidth = GridBagConstraints.REMAINDER;
//         c.weightx = 0.0;    c.weighty = 0.0;
//         addBag(readyB, gridbag, c);

        addKeyListener(this);

    }

    private void addBag(JComponent comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    /**
     * Show or hide the "reroll inititiative" button in this report display.
     *
     * @param show a <code>boolean</code> that indicates that the button
     *             should be shown in this report display.
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
        if (client.game.getPhase() == IGame.PHASE_INITIATIVE_REPORT
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
            //The deployment reports (round 0) are combined with round one's
            // report.
            round = 1;
        }
        if (round >= vTextArea.size()) {
            //Need a new tab for the new round.

            //get rid of phase tab
            if (round > 1) {
                tabs.remove(vTextArea.get(vTextArea.size() - 1));
                vTextArea.remove(vTextArea.size() - 1);
            }

            //add as many round tabs as necessary to catch us up
            JTextArea ta;
            while (round > vTextArea.size()) {
                //HACK: We shouldn't have to rely on our access to the client object...
                ta = new JTextArea(client.receiveReport(client.game.getReports(vTextArea.size() + 1)), 40, 25);
                ta.setEditable(false);
                tabs.add("Round " + (vTextArea.size() + 1), ta);
                vTextArea.add(new JScrollPane(ta));
            }

            //add the new current phase tab
            ta = new JTextArea(phaseText, 40, 25);
            ta.setEditable(false);
            ta.setOpaque(false);
            JScrollPane sp = new JScrollPane(ta);
            vTextArea.add(sp);
            tabs.add("Phase", sp);
            tabs.setSelectedComponent(sp);
        } else {
            //Update the previous rounds tab and the phase tab.
            ((JTextArea) ((JViewport) vTextArea.get(round - 1).getComponent(0)).getView()).setText(roundText);
            ((JTextArea) ((JViewport) vTextArea.get(round).getComponent(0)).getView()).setText(phaseText);
        }
    }

    public void appendReportTab(String additionalText) {
        ((JTextArea) vTextArea.get(vTextArea.size() - 1).getComponent(0)).append(additionalText);
        ((JTextArea) vTextArea.get(vTextArea.size() - 2).getComponent(0)).append(additionalText);
    }

    public void resetTabs() {
        tabs.removeAll();
        vTextArea = new ArrayList<JScrollPane>();
        /* HACK: Without this initial empty TextArea, the tabs will be
           blank (no TextArea at all) during the first initiative
           phase.  I think it has something to do with the layout
           manager, but I'm not really sure.  Maybe a strategically
           placed validate() would be better?
        JTextArea ta = new JTextArea("", 40, 25);
        ta.setEditable(false);
        vTextArea.add(new JScrollPane(ta));
        tabs.add("Phase", ta);*/
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
        if (isIgnoringEvents()) {
            return;
        }

        setReportTab(client.game.getRoundCount(), client.roundReport, client.phaseReport);
        resetButtons();
        rerolled = false;
    }

    /**
     * Determine if the listener is currently distracted.
     *
     * @return <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     *
     * @param distracted <code>true</code> if the listener should ignore events
     *                   <code>false</code> if the listener should pay attention again.
     *                   Events that occured while the listener was distracted NOT
     *                   going to be processed.
     */
    public void setIgnoringEvents(boolean distracted) {
        this.distracted.setIgnoringEvents(distracted);
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
     * @return the <code>javax.swing.JButton</code> that activates this
     *         object's "Done" action.
     */
    public JButton getDoneButton() {
        return readyB;
    }

    /**
     * Get the secondary display section of this phase.
     *
     * @return the <code>Component</code> which is displayed in the
     *         secondary section during this phase.
     */
    public JComponent getSecondaryDisplay() {
        return panStatus;
    }

}
