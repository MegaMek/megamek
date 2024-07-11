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
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;

import java.awt.event.ActionEvent;
import java.util.*;

public class SBFReportDisplay extends StatusBarPhaseDisplay {

    public enum ReportCommand implements PhaseCommand {
        REPORT_REPORT("reportReport"),
        REPORT_PLAYERLIST("reportPlayerList");

        final String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        ReportCommand(String c) {
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

        public String getHotKeyDesc() {
            String result = "";

            if (this == REPORT_REPORT) {
                result = "<BR>";
                result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.ROUND_REPORT);
            }

            return result;
        }
    }

    // buttons
    private Map<ReportCommand, MegamekButton> buttons;

    private static final String RD_REPORTDISPLAY = "ReportDisplay.";
    private static final String RD_TOOLTIP = ".tooltip";


    /**
     * Creates and lays out a new movement phase display for the specified
     * clientgui.getClient().
     */
    public SBFReportDisplay(SBFClientGUI clientgui) {
        super(Objects.requireNonNull(clientgui));

        setupStatusBar("");
        setButtons();
        setButtonsTooltips();

        butDone.setText(Messages.getString("ReportDisplay.Done"));
        butDone.setEnabled(false);
        setupButtonPanel();
        clientgui.getClient().getGame().addGameListener(this);
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (ReportCommand.values().length * 1.25 + 0.5));
        for (ReportCommand cmd : ReportCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), RD_REPORTDISPLAY));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (ReportCommand cmd : ReportCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), RD_REPORTDISPLAY, cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
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

    @Override
    public void clear() { }

    @Override
    public void ready() {
        butDone.setEnabled(false);
        setReportEnabled(false);
        setPlayerListEnabled(false);
        sendDone();
    }

    public void setReportEnabled(boolean enabled) {
        buttons.get(ReportCommand.REPORT_REPORT).setEnabled(enabled);
    }

    public void setPlayerListEnabled(boolean enabled) {
        buttons.get(ReportCommand.REPORT_PLAYERLIST).setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_REPORT.getCmd()))) {
            GUIP.toggleRoundReportEnabled();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_PLAYERLIST.getCmd()))) {
            GUIP.togglePlayerListEnabled();
        }
    }

    private void resetButtons() {
        butDone.setEnabled(!clientgui.getClient().getLocalPlayer().isDone());
        setReportEnabled(true);
        setPlayerListEnabled(true);
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        GamePhase phase = clientgui.getClient().getGame().getPhase();

        switch (phase) {
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
                resetButtons();
                setStatusBarWithNotDonePlayers();
                break;
            default:
                setStatusBarText(phase.toString());
                break;
        }

//        clientgui.bingMyTurn();
    }

    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
    }
}
