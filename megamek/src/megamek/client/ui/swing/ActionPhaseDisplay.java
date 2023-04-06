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

import megamek.client.ui.swing.StatusBarPhaseDisplay;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.preference.PreferenceChangeEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;

public abstract class ActionPhaseDisplay extends StatusBarPhaseDisplay {
    protected MegamekButton butIgnoreNag;
    private boolean isDoingAction = false;
    private boolean ignoreNoActionNag = false;

    protected ActionPhaseDisplay(ClientGUI cg) {
        super(cg);
    }

    @Override
    protected UIUtil.FixedXPanel setupDonePanel() {
        var donePanel = super.setupDonePanel();
        butIgnoreNag = new MegamekButton("SKIP", SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp());
        butIgnoreNag.setPreferredSize(new Dimension(DONE_BUTTON_WIDTH, MIN_BUTTON_SIZE.height * 1));
        String f = guiScaledFontHTML(UIUtil.uiLightViolet()) +  KeyCommandBind.getDesc(KeyCommandBind.DONE_NO_ACTION)+ "</FONT>";
        butDone.setToolTipText("<html><body>" + f + "</body></html>");

        donePanel.add(butIgnoreNag);
        if (clientgui != null) {
            butIgnoreNag.addActionListener(new AbstractAction() {
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
                        ignoreNoActionNag = true;
                        ready();
                        // When the turn is ended, we could miss a key release
                        // event
                        // This will ensure no repeating keys are stuck down
                        clientgui.controller.stopAllRepeating();
                    }
                }
            });

            final AbstractPhaseDisplay display = this;
            // Register the action for DONE
            clientgui.controller.registerCommandAction(KeyCommandBind.DONE_NO_ACTION.cmd,
                    new CommandAction() {

                        @Override
                        public boolean shouldPerformAction() {
                            if (((!clientgui.getClient().isMyTurn()
                                    && (clientgui.getClient().getGame().getTurn() != null)
                                    && (!clientgui.getClient().getGame().getPhase().isReport())))
                                    || clientgui.getBoardView().getChatterBoxActive()
                                    || display.isIgnoringEvents()
                                    || !display.isVisible()
                                    || !(butDone.isEnabled() || butIgnoreNag.isEnabled())) {
                                return false;
                            } else {
                                return true;
                            }
                        }

                        @Override
                        public void performAction() {
                            ignoreNoActionNag = true;
                            ready();
                        }
                    });

        }

        updateVisibilityDonePanel();
        return donePanel;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        super.preferenceChange(e);
        updateVisibilityDonePanel();
    }

    protected void initDonePanelForNewTurn()
    {
        ignoreNoActionNag = false;
        updateVisibilityDonePanel();
    }

    private void updateVisibilityDonePanel()
    {
        if (GUIP.getNagForNoAction()) {
            butIgnoreNag.setVisible(true);
        } else {
            butIgnoreNag.setVisible(false);
        }
        updateDonePanelEnabled();
    }

    /**
     * @return true if a nag window should be shown when there is no action given to current unit.
     * This is true if user option wants a nag and they have not preemptively checked @butIgnoreNag
     */
    protected boolean needNagForNoAction() {
        return GUIP.getNagForNoAction() && !ignoreNoActionNag;
    }

    protected void updateDonePanel(String actionLabel, String noActionLabel, boolean doingAction) {
        isDoingAction = doingAction;
        if (GUIP.getNagForNoAction()) {
            butDone.setText("<html><b>" + actionLabel + "</b></html>");
        } else {
            // toggle the text on the done button
            if (doingAction) {
                butDone.setText("<html><b>" + actionLabel + "</b></html>");
            } else {
                butDone.setText("<html><b>" + noActionLabel + "</b></html>");
            }
        }
        butIgnoreNag.setText("<html><b>" + noActionLabel + "</b></html>");
        updateDonePanelEnabled();
    }

    protected void updateDonePanelEnabled() {
        if (isDoingAction || ignoreNoActionNag) {
            butDone.setEnabled(true);
            butIgnoreNag.setEnabled(false);
        } else {
            butDone.setEnabled(!GUIP.getNagForNoAction());
            butIgnoreNag.setEnabled(true);
        }
    }
}
