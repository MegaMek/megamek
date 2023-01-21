/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import org.apache.logging.log4j.LogManager;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiLightViolet;

/**
 * Targeting Phase Display. Breaks naming convention because TargetingDisplay is too easy to confuse
 * with something else
 */
public class PrephaseDisplay extends StatusBarPhaseDisplay implements
        KeyListener, ItemListener, ListSelectionListener {
    private static final long serialVersionUID = 3441669419807288865L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the deploy minefield phase.  Each command has a string
     * for the command plus a flag that determines what unit type it is
     * appropriate for.
     *
     * @author arlith
     */
    public enum PrephaseCommand implements PhaseCommand {
        PREPHASE_NEXT("prephaseNext"),
        PREPHASE_REVEAL("prephaseReveal"),
        PREPHASE_CANCEL_REVEAL("prephaseCancelReveal");

        String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        PrephaseCommand(String c) {
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
            return Messages.getString("PrephaseDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            String result = "";

            String msg_next= Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");

            switch (this) {
                case PREPHASE_NEXT:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
                    break;
                default:
                    break;
            }

            return result;
        }
    }

    // buttons
    protected Map<PrephaseCommand, MegamekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number

    // is the shift key held?
    private boolean shiftheld;

    private final GamePhase phase;

    /**
     * Creates and lays out a new Prefiring or PreMovement phase display for the specified
     * clientgui.getClient().
     */
    public PrephaseDisplay(final ClientGUI clientgui, GamePhase phase) {
        super(clientgui);
        this.phase = phase;

        setupStatusBar(Messages.getFormattedString("PrephaseDisplay.waitingForPrephasePhase", phase.toString()));

        setButtons();
        setButtonsTooltips();

        butDone.setText(Messages.getString("PrephaseDisplay.Done"));
        String f = guiScaledFontHTML(uiLightViolet()) +  KeyCommandBind.getDesc(KeyCommandBind.DONE)+ "</FONT>";
        butDone.setToolTipText("<html><body>" + f + "</body></html>");
        butDone.setEnabled(false);

        setupButtonPanel();

        registerKeyCommands();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (PrephaseCommand.values().length * 1.25 + 0.5));
        for (PrephaseCommand cmd : PrephaseCommand.values()) {
            String title = Messages.getString("PrephaseDisplay." + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title, SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            String ttKey = "PrephaseDisplay." + cmd.getCmd() + ".tooltip";
            if (Messages.keyExists(ttKey)) {
                newButton.setToolTipText(Messages.getString(ttKey));
            }
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);

            buttons.put(cmd, newButton);
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (PrephaseCommand cmd : PrephaseCommand.values()) {
            String ttKey = "PrephaseDisplay." + cmd.getCmd() + ".tooltip";
            String tt = cmd.getHotKeyDesc();
            if (!tt.isEmpty()) {
                String title = Messages.getString("PrephaseDisplay." + cmd.getCmd());
                tt = guiScaledFontHTML(uiLightViolet()) + title + ": " + tt + "</FONT>";
                tt += "<BR>";
            }
            if (Messages.keyExists(ttKey)) {
                String msg_key = Messages.getString(ttKey);
                tt += guiScaledFontHTML() + msg_key + "</FONT>";
            }
            String b = "<BODY>" + tt + "</BODY>";
            String h = "<HTML>" + b + "</HTML>";
            if (!tt.isEmpty()) {
                buttons.get(cmd).setToolTipText(h);
            }
        }
    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    protected void registerKeyCommands() {
        MegaMekController controller = clientgui.controller;
        final StatusBarPhaseDisplay display = this;

        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        selectEntity(clientgui.getClient().getNextEntityNum(cen));
                    }
                });

        // Register the action for PREV_UNIT
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        selectEntity(clientgui.getClient().getPrevEntityNum(cen));
                    }
                });
    }

    /**
     * Have the panel register itself as a listener wherever it's needed.
     * <p>
     * According to
     * http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a
     * major bad no-no to perform these registrations before the constructor
     * finishes, so this function has to be called after the panel is created.
     * Please note, this restriction only applies to listeners for objects that
     * aren't on the panel itself.
     */
    public void initializeListeners() {
        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        clientgui.getBoardView().addKeyListener(this);

        // mech display.
        clientgui.getUnitDisplay().wPan.weaponList.addListSelectionListener(this);
        clientgui.getUnitDisplay().wPan.weaponList.addKeyListener(this);
    }

    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<>();
        PrephaseCommand[] commands = PrephaseCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (PrephaseCommand cmd : commands) {
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /**
     * Selects an entity, by number, for targeting.
     */
    private void selectEntity(int en) {
        // clear any previously considered attacks
        if (en != cen) {
            refreshAll();
        }
        Client client = clientgui.getClient();

        if (client.getGame().getEntity(en) != null) {

            cen = en;
            clientgui.setSelectedEntityNum(en);

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (null == ce().getPosition()) {

                // Walk through the list of entities for this player.
                for (int nextId = client.getNextEntityNum(en); nextId != en;
                     nextId = client.getNextEntityNum(nextId)) {

                    if (null != clientgui.getClient().getGame()
                            .getEntity(nextId).getPosition()) {
                        cen = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (null == ce().getPosition()) {
                    LogManager.getLogger().error("Could not find an on-board entity: " + en);
                    return;
                }
            }

            clientgui.getBoardView().highlight(ce().getPosition());
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();

            if (!clientgui.getBoardView().isMovingUnits() && !ce().isOffBoard()) {
                clientgui.getBoardView().centerOnHex(ce().getPosition());
            }
        } else {
            LogManager.getLogger().error("Tried to select non-existent entity: " + en);
        }
    }

    private void refreshButtons() {
        final Entity ce = ce();

        if ( ce == null || ce.isDone() ) {
            // how get here?
            disableButtons();
            return;
        }

        setStatusBarText(Messages.getFormattedString("PrephaseDisplay.its_your_turn", phase.toString(), ce.getDisplayName()));

        boolean isRevealing = !ce.getHiddenActivationPhase().isUnknown();
        setRevealEnabled(!isRevealing);
        setCancelRevealEnabled(isRevealing);
        butDone.setEnabled(true);
    }

    /**
     * Called when the current entity is done firing. Send out our attack queue
     * to the server.
     */
    @Override
    public void ready() {
        // stop further input (hopefully)
        disableButtons();

        clientgui.getClient().sendUpdateEntity(ce());
        clientgui.getClient().sendPrephaseData(cen);

        endMyTurn();
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        setStatusBarText(Messages.getFormattedString("PrephaseDisplay.its_your_turn", phase.toString(), ""));
        butDone.setText("<html><b>" + Messages.getString("PrephaseDisplay.Done") + "</b></html>");

        clientgui.getBoardView().clearFieldofF();

        if (!clientgui.getBoardView().isMovingUnits()) {
            clientgui.maybeShowUnitDisplay();
        }

        // make best guess at next unit to select
        int nextId = Entity.NONE;
        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());

        if (next == null) {
            nextId = clientgui.getClient().getFirstEntityNum();
        } else {
            nextId = next.getId();
        }
        selectEntity(nextId);

        clientgui.getBoardView().select(null);
        setupButtonPanel();
        refreshButtons();
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());
        if ((phase == clientgui.getClient().getGame().getPhase())
                && (null != next) && (null != ce())
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }

        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().clearFiringSolutionData();
        clientgui.getBoardView().clearMovementData();
        clientgui.getBoardView().clearFieldofF();
        clientgui.setSelectedEntityNum(Entity.NONE);
        refreshButtons();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setRevealEnabled(false);
        setCancelRevealEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
    }

    private GamePhase revealInPhase() {
        return phase.isPremovement() ? GamePhase.MOVEMENT : GamePhase.FIRING;
    }

    private void reveal() {
        clientgui.getClient().sendActivateHidden(cen, revealInPhase());
    }

    private void cancelReveal() {
        // or could send change of state
        clientgui.getClient().sendActivateHidden(cen, GamePhase.UNKNOWN);
    }
    /**
     * Refreshes all displays.
     */
    private void refreshAll() {
        refreshButtons();

        if (ce() == null) {
            return;
        }
        clientgui.getBoardView().redrawEntity(ce());
        clientgui.getUnitDisplay().displayEntity(ce());
        clientgui.getUnitDisplay().showPanel("weapons");
        clientgui.getUnitDisplay().wPan.selectFirstWeapon();
    }

    /**
     * Returns the current entity.
     */
    Entity ce() {
        return clientgui.getClient().getGame().getEntity(cen);
    }

    // BoardListener
    @Override
    public void hexMoused(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        // ignore buttons other than 1
        if (!clientgui.getClient().isMyTurn()
                || ((b.getButton() != MouseEvent.BUTTON1))) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0)
                || ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0)) {
            return;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            clientgui.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            clientgui.getBoardView().select(b.getCoords());
        }
    }

    // GameListener
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase().isLounge()) {
            endMyTurn();
        }
        // On simultaneous phases, each player ending their turn will generate a turn change
        // We want to ignore turns from other players and only listen to events we generated
        // Except on the first turn
        if (clientgui.getClient().getGame().getPhase().isSimultaneous(clientgui.getClient().getGame())
                && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
                && (clientgui.getClient().getGame().getTurnIndex() != 0)) {
            return;
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().getGame().getPhase() == phase) {
            if (clientgui.getClient().isMyTurn()) {
                if (cen == Entity.NONE) {
                    beginMyTurn();
                }
            } else {
                endMyTurn();
                if (e.getPlayer() != null) {
                    setStatusBarText(Messages.getFormattedString(
                            "PrephaseDisplay.its_others_turn",
                            phase.toString(), e.getPlayer().getName()));
                }

            }
        }
    }

    //GameListener
    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()
                && (clientgui.getClient().getGame().getPhase() != phase)) {
            endMyTurn();
        }

        if (clientgui.getClient().getGame().getPhase() == phase) {
            setStatusBarText(Messages
                    .getFormattedString("PrephaseDisplay.waitingForPrephasePhase", phase.toString()));
        }
    }

    //GameListener
    @Override
    public void gameEntityChange(GameEntityChangeEvent event) {
        if (!event.getEntity().equals(ce())) {
            return;
        }

        refreshButtons();
    }

    // ActionListener
    @Override
    public void actionPerformed(ActionEvent ev) {
        if (isIgnoringEvents()) {
            return;
        }

        if (!clientgui.getClient().isMyTurn()) {
            return;
        }

        if (ev.getActionCommand().equals(PrephaseCommand.PREPHASE_REVEAL.getCmd())) {
            reveal();
        }

        if (ev.getActionCommand().equals(PrephaseCommand.PREPHASE_CANCEL_REVEAL.getCmd())) {
            cancelReveal();
        }

        if (ev.getActionCommand().equals(PrephaseCommand.PREPHASE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient()
                    .getNextEntityNum(cen));
        }
    }

    private void setRevealEnabled(boolean enabled) {
        buttons.get(PrephaseCommand.PREPHASE_REVEAL).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PrephaseCommand.PREPHASE_REVEAL.getCmd(), enabled);
    }

    private void setCancelRevealEnabled(boolean enabled) {
        buttons.get(PrephaseCommand.PREPHASE_CANCEL_REVEAL).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PrephaseCommand.PREPHASE_CANCEL_REVEAL.getCmd(), enabled);
    }

    private void setNextEnabled(boolean enabled) {
        buttons.get(PrephaseCommand.PREPHASE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PrephaseCommand.PREPHASE_NEXT.getCmd(), enabled);
    }


    @Override
    public void clear() {
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        refreshAll();
    }

    //
    // ItemListener
    //
    @Override
    public void itemStateChanged(ItemEvent ev) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && (ce() != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.getBoardView().centerOnHex(ce().getPosition());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity e = clientgui.getClient().getGame().getEntity(b.getEntityId());
        if (clientgui.getClient().isMyTurn()) {
            if (clientgui.getClient().getMyTurn()
                    .isValidEntity(e, clientgui.getClient().getGame())) {
                selectEntity(e.getId());
            }
        } else {
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(e);
            if (e.isDeployed()) {
                clientgui.getBoardView().centerOnHex(e.getPosition());
            }
        }
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.getUnitDisplay().wPan.weaponList.removeListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
    }


}
