/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;

import megamek.client.ui.clientGUI.MegaMekGUI;
import megamek.client.ui.clientGUI.SBFClientGUI;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.SkinSpecification;
import megamek.common.units.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.strategicBattleSystems.SBFFormation;

public abstract class SBFActionPhaseDisplay extends StatusBarPhaseDisplay {

    private boolean ignoreNoActionNag = false;

    /** The currently selected unit for taking action. Not necessarily equal to the unit shown in the unit viewer. */
    protected int currentFormation = Entity.NONE;

    protected MegaMekButton butSkipTurn;
    protected final SBFClientGUI clientGUI;
    protected final MegaMekController controller = MegaMekGUI.getKeyDispatcher();

    protected SBFActionPhaseDisplay(SBFClientGUI sbfClientGUI) {
        super(sbfClientGUI);
        clientGUI = sbfClientGUI;
    }

    @Override
    protected UIUtil.FixedXPanel setupDonePanel() {
        var donePanel = super.setupDonePanel();
        butSkipTurn = new MegaMekButton("SKIP", SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp());
        butSkipTurn.setPreferredSize(new Dimension(UIUtil.scaleForGUI(DONE_BUTTON_WIDTH), MIN_BUTTON_SIZE.height));
        String f = UIUtil.fontHTML(UIUtil.uiLightViolet())
              + KeyCommandBind.getDesc(KeyCommandBind.DONE_NO_ACTION)
              + "</FONT>";
        butSkipTurn.setToolTipText("<html><body>" + f + "</body></html>");
        addToDonePanel(donePanel, butSkipTurn);

        if (clientGUI != null) {
            butSkipTurn.addActionListener(new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isIgnoringEvents()) {
                        return;
                    }
                    if ((clientGUI.getClient().isMyTurn())
                          || (clientGUI.getClient().getGame().getTurn() == null)
                          || (clientGUI.getClient().getGame().getPhase().isReport())) {
                        // act like Done button
                        performDoneNoAction();
                        // When the turn is ended, we could miss a key release event
                        // This will ensure no repeating keys are stuck down
                        controller.stopAllRepeating();
                    }
                }
            });

            controller.registerCommandAction(KeyCommandBind.DONE_NO_ACTION, this::shouldReceiveDoneKeyCommand,
                  this::performDoneNoAction);
        }

        updateDonePanel();
        return donePanel;
    }

    private void performDoneNoAction() {
        ignoreNoActionNag = true;
        ready();
    }

    public boolean shouldReceiveDoneKeyCommand() {
        return ((clientGUI.getClient().isMyTurn()
              || (clientGUI.getClient().getGame().getTurn() == null)
              || (clientGUI.getClient().getGame().getPhase().isReport())))
              && !clientGUI.isChatBoxActive()
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

    /**
     * called to reset, show, hide and relabel the Done panel buttons. Override to change button labels and states,
     * being sure to call {@link #updateDonePanelButtons(String, String, boolean, java.util.List)} to set the button
     * labels and states
     */
    abstract protected void updateDonePanel();

    /**
     * set labels and enables on the done and skip buttons depending on the GUIP getNagForNoAction option
     *
     * @param isDoingAction true if user has entered actions for this turn, false if not.
     */
    protected void updateDonePanelButtons(final String doneButtonLabel, final String skipButtonLabel,
          final boolean isDoingAction, @Nullable List<String> turnDetails) {
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

        if ((currentFormation == SBFFormation.NONE)
              || getClientGUI().getClient().getGame().getInGameObject(currentFormation).isEmpty()) {
            butDone.setEnabled(false);
            butSkipTurn.setEnabled(false);
        } else if (isDoingAction || ignoreNoActionNag) {
            butDone.setEnabled(true);
            butSkipTurn.setEnabled(false);
        } else {
            butDone.setEnabled(!GUIP.getNagForNoAction());
            butSkipTurn.setEnabled(true);
        }
    }

    private void adaptToGUIScale() {
        butSkipTurn.setPreferredSize(new Dimension(UIUtil.scaleForGUI(DONE_BUTTON_WIDTH), MIN_BUTTON_SIZE.height));
    }

    /**
     * Returns the formation that is currently selected for action (movement/firing etc.), if any, as an optional. Note
     * that this can be empty in many cases. E.g., displays are active when it's not the player's turn. When the setting
     * to not auto-select a unit for the player is active, no unit may be selected even in a player's turn. Note that
     * this is not necessarily equal to the unit *viewed* in the unit display.
     *
     * @return The currently acting formation, if any
     */
    protected final Optional<SBFFormation> actingFormation() {
        return clientGUI.getClient().getGame().getFormation(currentFormation);
    }
}
