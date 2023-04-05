package megamek.client.ui.swing;

import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.preference.PreferenceChangeEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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
//                        // act like checkbox
//                        ignoreNag = !ignoreNag;
//                        updateDonePanel();

                        // act like Done button
                        ignoreNoActionNag = true;
                        ready();
                    }
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
            // toggle buttons enables on the two buttons, not the text
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

    private void updateDonePanelEnabled() {
        if (isDoingAction || ignoreNoActionNag) {
            butDone.setEnabled(true);
            butIgnoreNag.setEnabled(false);
        } else {
            butDone.setEnabled(!GUIP.getNagForNoAction());
            butIgnoreNag.setEnabled(true);
        }
    }

    protected void whatisthis() {}
}
