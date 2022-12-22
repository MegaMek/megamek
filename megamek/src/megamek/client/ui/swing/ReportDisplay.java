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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReportDisplay extends StatusBarPhaseDisplay  {
    private static final long serialVersionUID = 6185643976857892270L;

    public static enum ReportCommand implements PhaseCommand {
        REPORT_REPORT("reportReport"),
        REPORT_PLAYERLIST("reportPlayerList"),
        REPORT_REROLLINITIATIVE("reportRerollInitiative");

        String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        private ReportCommand(String c) {
            cmd = c;
        }

        @Override
        public String getCmd() {
            return cmd;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public void setPriority(int p) {
            priority = p;
        }

        @Override
        public String toString() {
            return Messages.getString("ReportDisplay." + getCmd());
        }
    }

    // buttons
    private Map<ReportCommand, MegamekButton> buttons;
    private boolean rerolled; // have we rerolled an init?

    /**
     * Creates and lays out a new movement phase display for the specified
     * clientgui.getClient().
     */
    public ReportDisplay(ClientGUI clientgui) {
        super(clientgui);

        if (clientgui == null) {
            return;
        }

        setupStatusBar("");

        buttons = new HashMap<>((int) (ReportCommand.values().length * 1.25 + 0.5));
        for (ReportCommand cmd : ReportCommand.values()) {
            String title = Messages.getString("ReportDisplay." + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title,
                    SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            String ttKey = "ReportDisplay." + cmd.getCmd() + ".tooltip";
            if (Messages.keyExists(ttKey)) {
                newButton.setToolTipText(Messages.getString(ttKey));
            }
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);
            buttons.put(cmd, newButton);
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);

        butDone.setText("<html><b>" + Messages.getString("ReportDisplay.Done") + "</b></html>");
        butDone.setEnabled(false);

        setupButtonPanel();

        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);
        clientgui.getBoardView().addKeyListener(this);
    }

    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<>();
        ReportCommand[] commands = ReportCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (ReportCommand cmd : commands) {
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /**
     * Clears all current actions
     */
    @Override
    public void clear() {

    }

    /**
     * Sets you as ready and disables the ready button.
     */
    @Override
    public void ready() {
        butDone.setEnabled(false);
        setReportEnabled(false);
        setPlayerListEnabled(false);
        setRerollInitiativeEnabled(false);
        clientgui.getClient().sendDone(true);
    }

    public void setReportEnabled(boolean enabled) {
        buttons.get(ReportCommand.REPORT_REPORT).setEnabled(enabled);
    }

    public void setPlayerListEnabled(boolean enabled) {
        buttons.get(ReportCommand.REPORT_PLAYERLIST).setEnabled(enabled);
    }

    public void setRerollInitiativeEnabled(boolean enabled) {
        buttons.get(ReportCommand.REPORT_REROLLINITIATIVE).setEnabled(enabled);
    }

    public void resetRerollInitiativeEnabled() {
        if (!rerolled) {
            setRerollInitiativeEnabled(true);
        }
    }

    public void setDoneEnabled(boolean enabled) {
        butDone.setEnabled(enabled);
    }

    /**
     * Requests an initiative reroll and disables the ready button.
     */
    public void rerollInitiative() {
        rerolled = true;
        setRerollInitiativeEnabled(false);
        clientgui.getClient().sendRerollInitiativeRequest();
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_REROLLINITIATIVE.getCmd())) {
            rerollInitiative();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_REPORT.getCmd()))) {
            GUIP.toggleRoundReportEnabled();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_PLAYERLIST.getCmd()))) {
            GUIP.togglePlayerListEnabled();
        }
    }

    private void resetButtons() {
        butDone.setEnabled(true);
        setReportEnabled(true);
        setPlayerListEnabled(true);

        if ((clientgui.getClient().getGame().getPhase() == GamePhase.INITIATIVE_REPORT) && clientgui.getClient().getGame().hasTacticalGenius(clientgui.getClient().getLocalPlayer())) {
            setRerollInitiativeEnabled(true);
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        rerolled = false;

        switch (clientgui.getClient().getGame().getPhase()) {
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
                resetButtons();
                setStatusBarText(clientgui.getClient().getGame().getPhase().toString());
                break;
            default:
                setStatusBarText(clientgui.getClient().getGame().getPhase().toString());
                break;
        }
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.getBoardView().removeKeyListener(this);
    }
}
