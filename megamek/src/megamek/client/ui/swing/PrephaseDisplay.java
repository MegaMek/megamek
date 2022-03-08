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
import megamek.client.ui.swing.FiringDisplay.FiringCommand;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import org.apache.commons.lang.NotImplementedException;

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
        FIRE_NEXT("fireNext"),
        FIRE_FIRE("fireFire"),
        FIRE_SKIP("fireSkip"),
        //GHOSTTARGET
        FIRE_SEARCHLIGHT("fireSearchlight"),
        FIRE_CANCEL("fireCancel"),
        FIRE_DISENGAGE("fireDisengage");

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
    }

    // buttons
    protected Map<PrephaseCommand, MegamekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number

    // is the shift key held?
    private boolean shiftheld;

    private final GamePhase phase;



    /**
     * Creates and lays out a new targeting phase display for the specified
     * clientgui.getClient().
     */
    public PrephaseDisplay(final ClientGUI clientgui, GamePhase phase) {
        super(clientgui);
        this.phase = phase;
        shiftheld = false;

        setupStatusBar(Messages
                .getString("PrephaseDisplay.waitingForTargetingPhase"));

        buttons = new HashMap<>(
                (int) (PrephaseCommand.values().length * 1.25 + 0.5));
        for (PrephaseCommand cmd : PrephaseCommand.values()) {
            String title = Messages.getString("PrephaseDisplay."
                    + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title,
                    SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            String ttKey = "PrephaseDisplay." + cmd.getCmd() + ".tooltip";
            if (Messages.keyExists(ttKey)) {
                newButton.setToolTipText(Messages.getString(ttKey));
            }
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);

            buttons.put(cmd, newButton);
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0)
                / buttonsPerGroup);

        butDone.setText(Messages.getString("PrephaseDisplay.Done"));
        butDone.setEnabled(false);

        setupButtonPanel();

        MegaMekController controller = clientgui.controller;
        final StatusBarPhaseDisplay display = this;
        // Register the action for UNDO
        controller.registerCommandAction(KeyCommandBind.UNDO_LAST_STEP.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || display.isIgnoringEvents()
                                || !display.isVisible()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        throw new NotImplementedException("UNDO_LAST_STEP.performAction");
                    }
                });

        // Register the action for FIRE
        controller.registerCommandAction(KeyCommandBind.FIRE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()
                                || !buttons.get(PrephaseCommand.FIRE_FIRE).isEnabled()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        reveal();
                    }
                });

        // Register the action for NEXT_UNIT
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
                        selectEntity(clientgui.getClient()
                                .getNextEntityNum(cen));
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

        // Register the action for CLEAR
        controller.registerCommandAction(KeyCommandBind.CANCEL.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        clear();
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
        clientgui.mechD.wPan.weaponList.addListSelectionListener(this);
        clientgui.mechD.wPan.weaponList.addKeyListener(this);
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
                    System.err.println("PrephaseDisplay: could not find "
                            + "an on-board entity: " +
                            en);
                    return;
                }

            } // End ce()-not-on-board

            clientgui.getBoardView().highlight(ce().getPosition());
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();

            if (!clientgui.getBoardView().isMovingUnits() && !ce().isOffBoard()) {
                clientgui.getBoardView().centerOnHex(ce().getPosition());
            }

            setRevealEnabled(true);
            butDone.setEnabled(true);

        } else {
            System.err.println("PrephaseDisplay: "
                    + "tried to select non-existant entity: " + en);
        }
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

        // not sure if should finish turn ?
//        clientgui.getClient().sendDone(true);

        endMyTurn();
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        setStatusBarText(Messages.getString("MovementDisplay.its_your_turn"));
        butDone.setText("<html><b>" + Messages.getString("MovementDisplay.Done") + "</b></html>");
        butDone.setEnabled(true);
        setNextEnabled(true);
        clientgui.getBoardView().clearFieldofF();

        if (!clientgui.getBoardView().isMovingUnits()) {
            clientgui.maybeShowUnitDisplay();
        }

        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());

        if (next != null) {
            selectEntity(next.getId());
        } else {
            selectEntity(Entity.NONE);
        }

        clientgui.getBoardView().select(null);

        setupButtonPanel();
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        final Entity ce = ce();

        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());
        if ((phase == clientgui.getClient().getGame().getPhase())
                && (null != next) && (null != ce())
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.setUnitDisplayVisible(false);
        }

        cen = Entity.NONE;
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().clearFiringSolutionData();
        clientgui.getBoardView().clearMovementData();
        clientgui.getBoardView().clearFieldofF();
        clientgui.setSelectedEntityNum(Entity.NONE);
        disableButtons();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setRevealEnabled(false);
        setSkipEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
    }


    private void reveal() {
//        clientgui.getClient().sendActivateHidden(cen, phase == GamePhase.PREMOVEMENT ? GamePhase.MOVEMENT : GamePhase.FIRING );
        ce().setHiddenActivationPhase(phase == GamePhase.PREMOVEMENT ? GamePhase.MOVEMENT : GamePhase.FIRING);
    }


    /**
     * Refeshes all displays.
     */
    private void refreshAll() {
        if (ce() == null) {
            return;
        }
        clientgui.getBoardView().redrawEntity(ce());
        clientgui.mechD.displayEntity(ce());
        clientgui.mechD.showPanel("weapons");
        clientgui.mechD.wPan.selectFirstWeapon();
    }

    /**
     * Returns the current entity.
     */
    Entity ce() {
        return clientgui.getClient().getGame().getEntity(cen);
    }

    //
    // BoardListener
    //
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
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0)) {
            shiftheld = (b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (shiftheld) {
//                updateFlipArms(false);
            }
            clientgui.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            clientgui.getBoardView().select(b.getCoords());
        }
    }

    @Override
    public void hexSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        final Client client = clientgui.getClient();

//        if (client.isMyTurn() && (b.getCoords() != null)
//                && (ce() != null) && !b.getCoords().equals(ce().getPosition())) {
//            if (shiftheld) {
//                updateFlipArms(false);
//            } else if (phase == GamePhase.TARGETING) {
//                target(new HexTarget(b.getCoords(), Targetable.TYPE_HEX_ARTILLERY));
//            } else {
//                target(chooseTarget(b.getCoords()));
//            }
//        }
    }



    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {

        // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase() == GamePhase.LOUNGE) {
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
                setStatusBarText(Messages
                        .getString("PrephaseDisplay.its_your_turn"));
            } else {
                endMyTurn();
                if (e.getPlayer() != null) {
                    setStatusBarText(Messages.getString(
                            "PrephaseDisplay.its_others_turn",
                            e.getPlayer().getName()));
                }

            }
        }
    }

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
                    .getString("PrephaseDisplay.waitingForFiringPhase"));
        }
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (!clientgui.getClient().isMyTurn()) {
            return;
        }

        if (ev.getActionCommand().equals(PrephaseCommand.FIRE_FIRE.getCmd())) {
            reveal();
        }

        if (ev.getActionCommand().equals(PrephaseCommand.FIRE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient()
                    .getNextEntityNum(cen));
        }

    }


    private void setRevealEnabled(boolean enabled) {
        buttons.get(PrephaseCommand.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PrephaseCommand.FIRE_FIRE.getCmd(), enabled);
    }

    private void setSkipEnabled(boolean enabled) {
        buttons.get(PrephaseCommand.FIRE_SKIP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SKIP.getCmd(), enabled);
    }

    private void setNextEnabled(boolean enabled) {
        buttons.get(PrephaseCommand.FIRE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_NEXT.getCmd(), enabled);
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
            clientgui.mechD.displayEntity(e);
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
        clientgui.mechD.wPan.weaponList.removeListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
//        if (event.getSource().equals(clientgui.mechD.wPan.weaponList)) {
//            // update target data in weapon display
//            updateTarget();
//        }
    }


}
