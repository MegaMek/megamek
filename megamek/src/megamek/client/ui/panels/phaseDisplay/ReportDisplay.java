/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.panels.phaseDisplay;

import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.phaseDisplay.AbandonUnitDialog;
import megamek.client.ui.dialogs.phaseDisplay.NovaNetworkDialog;
import megamek.client.ui.dialogs.phaseDisplay.VariableRangeTargetingDialog;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.widget.MegaMekButton;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.units.CombatVehicleEscapePod;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReportDisplay extends StatusBarPhaseDisplay {
    @Serial
    private static final long serialVersionUID = 6185643976857892270L;
    private static final Logger LOGGER = LogManager.getLogger();

    public enum ReportCommand implements PhaseCommand {
        REPORT_REPORT("reportReport"),
        REPORT_PLAYER_LIST("reportPlayerList"),
        REPORT_REROLL_INITIATIVE("reportRerollInitiative"),
        REPORT_NOVA_NETWORK("reportNovaNetwork"),
        REPORT_VAR_RANGE_TARGETING("reportVarRangeTargeting"),
        REPORT_ABANDON("reportAbandon");

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
    private Map<ReportCommand, MegaMekButton> buttons;
    private boolean rerolled; // have we rerolled an init?

    private static final String RD_REPORT_DISPLAY = "ReportDisplay.";
    private static final String RD_TOOLTIP = ".tooltip";

    private final ClientGUI clientgui;

    /**
     * Creates and lays out a new movement phase display for the specified clientGUI.getClient().
     */
    public ReportDisplay(ClientGUI clientGUI) {
        super(clientGUI);
        this.clientgui = clientGUI;

        if (clientGUI == null) {
            return;
        }

        setupStatusBar("");

        setButtons();
        setButtonsTooltips();

        butDone.setText(Messages.getString("ReportDisplay.Done"));
        butDone.setEnabled(false);

        setupButtonPanel();

        clientGUI.getClient().getGame().addGameListener(this);
        //        clientGUI.getBoardView().addBoardViewListener(this);
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (ReportCommand.values().length * 1.25 + 0.5));
        for (ReportCommand cmd : ReportCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), RD_REPORT_DISPLAY));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (ReportCommand cmd : ReportCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), RD_REPORT_DISPLAY, cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
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
        buttons.get(ReportCommand.REPORT_PLAYER_LIST).setEnabled(enabled);
    }

    public void setRerollInitiativeEnabled(boolean enabled) {
        buttons.get(ReportCommand.REPORT_REROLL_INITIATIVE).setEnabled(enabled);
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
        if (ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_REROLL_INITIATIVE.getCmd())) {
            rerollInitiative();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_REPORT.getCmd()))) {
            GUIP.toggleRoundReportEnabled();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_PLAYER_LIST.getCmd()))) {
            GUIP.togglePlayerListEnabled();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_NOVA_NETWORK.getCmd()))) {
            showNovaNetworkDialog();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_VAR_RANGE_TARGETING.getCmd()))) {
            showVariableRangeTargetingDialog();
        } else if ((ev.getActionCommand().equalsIgnoreCase(ReportCommand.REPORT_ABANDON.getCmd()))) {
            showAbandonDialog();
        }
    }

    private void resetButtons() {
        butDone.setEnabled(!clientgui.getClient().getLocalPlayer().isDone());
        setReportEnabled(true);
        setPlayerListEnabled(true);

        if ((clientgui.getClient().getGame().getPhase() == GamePhase.INITIATIVE_REPORT) && clientgui.getClient()
              .getGame()
              .hasTacticalGenius(clientgui.getClient().getLocalPlayer())) {
            setRerollInitiativeEnabled(true);
        }

        // Enable Nova Network button if player has Nova CEWS units (TT: declare networks in End Phase)
        // Check both END and END_REPORT phases to ensure button is available during end phase
        GamePhase currentPhase = clientgui.getClient().getGame().getPhase();
        if (currentPhase == GamePhase.END || currentPhase == GamePhase.END_REPORT) {
            setNovaNetworkEnabled(hasNovaUnits());
            // Enable Variable Range Targeting button (BMM pg. 86: player chooses mode during End Phase)
            setVariableRangeTargetingEnabled(hasVariableRangeUnits());
            // Enable Abandon button if player has abandonable units (Meks: prone+shutdown, Vehicles: any)
            setAbandonEnabled(hasAbandonableUnits());
        } else {
            setNovaNetworkEnabled(false);
            setVariableRangeTargetingEnabled(false);
            setAbandonEnabled(false);
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        rerolled = false;

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

        clientgui.bingMyTurn();
    }

    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.boardViews().forEach(bv -> bv.removeBoardViewListener(this));
    }

    /**
     * Shows the Nova CEWS network management dialog.
     */
    private void showNovaNetworkDialog() {
        NovaNetworkDialog dialog = new NovaNetworkDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
    }

    /**
     * Checks if the local player has any Nova CEWS units.
     */
    private boolean hasNovaUnits() {
        int localPlayerId = clientgui.getClient().getLocalPlayer().getId();
        return clientgui.getClient().getGame().getEntitiesVector().stream()
                .filter(e -> e.getOwnerId() == localPlayerId)
                .anyMatch(Entity::hasNovaCEWS);
    }

    /**
     * Enables or disables the Nova Network button.
     */
    private void setNovaNetworkEnabled(boolean enabled) {
        MegaMekButton button = buttons.get(ReportCommand.REPORT_NOVA_NETWORK);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    /**
     * Shows the Variable Range Targeting mode selection dialog (BMM pg. 86).
     */
    private void showVariableRangeTargetingDialog() {
        VariableRangeTargetingDialog dialog = new VariableRangeTargetingDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
        // Clear focus from the button after dialog closes to reset highlight state
        buttons.get(ReportCommand.REPORT_VAR_RANGE_TARGETING).transferFocus();
    }

    /**
     * Checks if the local player has any units with Variable Range Targeting quirk.
     */
    private boolean hasVariableRangeUnits() {
        int localPlayerId = clientgui.getClient().getLocalPlayer().getId();
        return clientgui.getClient().getGame().getEntitiesVector().stream()
              .filter(e -> e.getOwnerId() == localPlayerId)
              .anyMatch(Entity::hasVariableRangeTargeting);
    }

    /**
     * Enables or disables the Variable Range Targeting button.
     */
    private void setVariableRangeTargetingEnabled(boolean enabled) {
        MegaMekButton button = buttons.get(ReportCommand.REPORT_VAR_RANGE_TARGETING);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    /**
     * Shows the Unit Abandonment dialog (TW/TacOps: announce abandonment in End Phase).
     */
    private void showAbandonDialog() {
        AbandonUnitDialog dialog = new AbandonUnitDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
        // Clear focus from the button after dialog closes
        MegaMekButton button = buttons.get(ReportCommand.REPORT_ABANDON);
        if (button != null) {
            button.transferFocus();
        }
    }

    /**
     * Checks if the local player has any units that can be abandoned. Meks must be prone + shutdown; Vehicles can be
     * abandoned anytime.
     */
    private boolean hasAbandonableUnits() {
        int localPlayerId = clientgui.getClient().getLocalPlayer().getId();
        return clientgui.getClient().getGame().getEntitiesVector().stream()
              .filter(e -> e.getOwnerId() == localPlayerId)
              .anyMatch(e -> (e instanceof Mek mek && mek.canAbandon())
                    || (e instanceof CombatVehicleEscapePod pod && pod.canCrewExit())
                    || (e instanceof Tank tank && tank.canAbandon()));
    }

    /**
     * Enables or disables the Abandon button.
     */
    private void setAbandonEnabled(boolean enabled) {
        MegaMekButton button = buttons.get(ReportCommand.REPORT_ABANDON);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }
}
