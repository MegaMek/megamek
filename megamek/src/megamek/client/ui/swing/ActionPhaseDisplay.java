/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import megamek.client.ui.swing.boardview.TurnDetailsOverlay;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.MegaMekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceChangeEvent;

public abstract class ActionPhaseDisplay extends StatusBarPhaseDisplay {

    protected MegaMekButton butSkipTurn;

    /** The currently selected unit for taking action. Not necessarily equal to the unit shown in the unit viewer. */
    protected int currentEntity = Entity.NONE;

    private boolean ignoreNoActionNag = false;

    protected final ClientGUI clientgui;

    protected ActionPhaseDisplay(ClientGUI cg) {
        super(cg);
        clientgui = cg;
    }

    @Override
    protected UIUtil.FixedXPanel setupDonePanel() {
        var donePanel = super.setupDonePanel();
        butSkipTurn = new MegaMekButton("SKIP", SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp());
        butSkipTurn.setPreferredSize(new Dimension(UIUtil.scaleForGUI(DONE_BUTTON_WIDTH), MIN_BUTTON_SIZE.height));
        String f = UIUtil.fontHTML(UIUtil.uiLightViolet()) +  KeyCommandBind.getDesc(KeyCommandBind.DONE_NO_ACTION)+ "</FONT>";
        butSkipTurn.setToolTipText("<html><body>" + f + "</body></html>");
        addToDonePanel(donePanel, butSkipTurn);

        if (clientgui != null) {
            butSkipTurn.addActionListener(new AbstractAction() {
                private static final long serialVersionUID = -5034474968902280850L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isIgnoringEvents()) {
                        return;
                    }
                    if ((clientgui.getClient().isMyTurn())
                            || (clientgui.getClient().getGame().getTurn() == null)
                            || (clientgui.getClient().getGame().getPhase().isReport())) {
                        // act like Done button
                        performDoneNoAction();
                        // When the turn is ended, we could miss a key release event
                        // This will ensure no repeating keys are stuck down
                        clientgui.controller.stopAllRepeating();
                    }
                }
            });

            clientgui.controller.registerCommandAction(KeyCommandBind.DONE_NO_ACTION, this::shouldReceiveDoneKeyCommand,
                    this::performDoneNoAction);
        }

        updateDonePanel();
        return donePanel;
    }

    protected void performDoneNoAction() {
        ignoreNoActionNag = true;
        ready();
    }

    public boolean shouldReceiveDoneKeyCommand() {
        return ((clientgui.getClient().isMyTurn()
                || (clientgui.getClient().getGame().getTurn() == null)
                || (clientgui.getClient().getGame().getPhase().isReport())))
                && !clientgui.getBoardView().getChatterBoxActive()
                && !isIgnoringEvents()
                && isVisible()
                && (butDone.isEnabled() || butSkipTurn.isEnabled());
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        super.preferenceChange(e);
        updateDonePanel();
        adaptToGUIScale();
    }

    protected void initDonePanelForNewTurn() {
        ignoreNoActionNag = false;
        updateDonePanel();
    }

    /** called to reset, show, hide and relabel the Done panel buttons. Override to change button labels and states,
     * being sure to call {@link #updateDonePanelButtons(String, String, boolean, List)}
     * to set the button labels and states
     */
    abstract protected void updateDonePanel();

    /**
     * @return true if a nag dialog should be shown when there is no action given to current unit.
     * This is true if user option wants a nag
     * they have not preemptively checked @butIgnoreNag
     * the turn timer is not expired
     */
    protected boolean needNagForNoAction() {
        return GUIP.getNagForNoAction() && !ignoreNoActionNag && !isTimerExpired();
    }

    protected boolean needNagForOverheat() {
        return GUIP.getNagForOverheat() && !isTimerExpired();
    }

    protected boolean needNagForNoUnJamRAC() {
        return GUIP.getNagForNoUnJamRAC() && !isTimerExpired();
    }

    protected boolean needNagForMASC() {
        return GUIP.getNagForMASC() && !isTimerExpired();
    }

    protected boolean needNagForPSR() {
        return GUIP.getNagForPSR() && !isTimerExpired();
    }

    protected boolean needNagForMechanicalJumpFallDamage() {
        return GUIP.getNagForMechanicalJumpFallDamage() && !isTimerExpired();
    }

    protected boolean needNagForCrushingBuildings() {
        return GUIP.getNagForCrushingBuildings() && !isTimerExpired();
    }

    protected boolean needNagForWiGELanding() {
        return GUIP.getNagForWiGELanding() && !isTimerExpired();
    }

    protected boolean needNagForLaunchDoors() {
        return GUIP.getNagForLaunchDoors() && !isTimerExpired();
    }

    protected boolean needNagForSprint() {
        return GUIP.getNagForSprint() && !isTimerExpired();
    }

    protected boolean needNagForOther() {
        return !isTimerExpired();
    }

    private boolean doYesNoBotherDialog(String title, String body, Runnable setNag) {
        ConfirmDialog nag = clientgui.doYesNoBotherDialog(title, body);
        if (nag.getAnswer()) {
            // do they want to be bothered again?
            if (!nag.getShowAgain()) {
                setNag.run();
            }
        } else {
            return true;
        }
        return false;
    }

    protected boolean checkNagForNoAction(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForNoAction(false));
    }

    protected boolean checkNagForNoUnJamRAC(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForNoUnJamRAC(false));
    }

    protected boolean checkNagForMASC(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForMASC(false));
    }

    protected boolean checkNagForSprint(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForSprint(false));
    }

    protected boolean checkNagForPSR(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForPSR(false));
    }

    protected boolean checkNagForMechanicalJumpFallDamage(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForMechanicalJumpFallDamage(false));
    }

    protected boolean checkNagForCrushingBuildings(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForCrushingBuildings(false));
    }

    protected boolean checkNagForWiGELanding(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForWiGELanding(false));
    }

    protected boolean checkNagForOverheat(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForOverheat(false));
    }

    protected boolean checkNagLaunchDoors(String title, String body) {
        return doYesNoBotherDialog(title, body, () -> GUIP.setNagForLaunchDoors(false));
    }

    /** set labels and enables on the done and skip buttons depending on the GUIP getNagForNoAction option
     *
     * @param doneButtonLabel
     * @param skipButtonLabel
     * @param isDoingAction true if user has entered actions for this turn, false if not.
     */
    protected void updateDonePanelButtons(final String doneButtonLabel, final String skipButtonLabel, final boolean isDoingAction,
                                          @Nullable List<String> turnDetails) {
        if (isIgnoringEvents()) {
            return;
        }

        if (GUIP.getNagForNoAction()) {
            butDone.setText("<html><b>" + doneButtonLabel + "</b></html>");
            butSkipTurn.setText("<html><b>" + skipButtonLabel + "</b></html>");
        } else {
            // toggle the text on the done button, butIgnoreNag is not used
            butSkipTurn.setVisible(false);
            if (isDoingAction) {
                butDone.setText("<html><b>" + doneButtonLabel + "</b></html>");
            } else {
                butDone.setText("<html><b>" + skipButtonLabel + "</b></html>");
            }
        }
        butSkipTurn.setText("<html><b>" + skipButtonLabel + "</b></html>");

        // point blank shots don't have the "isMyTurn()" characteristic
        if ((currentEntity == Entity.NONE)
                || getClientgui().getClient().getGame().getInGameObject(currentEntity).isEmpty()
                || (!clientgui.getClient().isMyTurn() && !clientgui.isProcessingPointblankShot())) {
            butDone.setEnabled(false);
            butSkipTurn.setEnabled(false);
        } else if (isDoingAction || ignoreNoActionNag) {
            butDone.setEnabled(true);
            butSkipTurn.setEnabled(false);
        } else {
            butDone.setEnabled(!GUIP.getNagForNoAction());
            butSkipTurn.setEnabled(true);
        }

        TurnDetailsOverlay turnDetailsOverlay = clientgui.getBoardView().getTurnDetailsOverlay();
        if (turnDetailsOverlay != null) {
            turnDetailsOverlay.setLines(turnDetails);
        }
    }

    private void adaptToGUIScale() {
        butSkipTurn.setPreferredSize(new Dimension(UIUtil.scaleForGUI(DONE_BUTTON_WIDTH), MIN_BUTTON_SIZE.height));
    }

    /**
     * @return The currently selected entity, if any, or null. A null check is **always** required, as displays
     * are active when it's not the player's turn and when the setting to not auto-select a unit for the
     * player is active, no unit may be selected even in a player's turn.
     * Note that this is not necessarily equal to the currently *viewed* unit in the unit display.
     *
     * @see ClientGUI#getDisplayedUnit()
     */
    protected final Entity ce() {
        return clientgui.getClient().getGame().getEntity(currentEntity);
    }
}
