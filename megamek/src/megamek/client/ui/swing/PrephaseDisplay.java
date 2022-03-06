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
//                        if (!clientgui.getClient().isMyTurn()
//                                || clientgui.getBoardView().getChatterBoxActive()
//                                || !display.isVisible()
//                                || display.isIgnoringEvents()
//                                || !buttons.get(PrephaseCommand.PREPHASE_REVEAL).isEnabled()) {
//                            return false;
//                        } else {
                            return true;
//                        }
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
//            if (cmd == PrephaseCommand.FIRE_CANCEL) {
//                continue;
//            }
//            if ((cmd == PrephaseCommand.FIRE_DISENGAGE) && ((ce() == null) || !ce().isOffBoard())) {
//                continue;
//            }
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


            //            if (GUIPreferences.getInstance().getBoolean("FiringSolutions")
//                    && !ce().isOffBoard()) {
//                setFiringSolutions();
//            } else {
//                clientgui.getBoardView().clearFiringSolutionData();
//            }
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

        clientgui.getClient().sendPrephaseData(cen);
//        clientgui.getClient().sendDone(true);

        // send out attacks
//        clientgui.getClient().sendAttackData(cen, attacks);



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

        int fen = clientgui.getClient().getFirstEntityNum();
        selectEntity(fen);

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
        clientgui.getClient().sendActivateHidden(cen, phase == GamePhase.PREMOVEMENT ? GamePhase.MOVEMENT : GamePhase.FIRING );
    }
//    /**
//     * Fire Mode - Adds a Fire Mode Change to the current Attack Action
//     */
//    private void changeMode(boolean forward) {
//        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();
//
//        // Do nothing we have no unit selected.
//        if (null == ce()) {
//            return;
//        }
//
//        // If the weapon does not have modes, just exit.
//        Mounted m = ce().getEquipment(wn);
//        if ((m == null) || !m.getType().hasModes()) {
//            return;
//        }
//
//        // Dropship Artillery cannot be switched to "Direct" Fire
//        final WeaponType wtype = (WeaponType) m.getType();
//        if ((ce() instanceof Dropship) && (wtype instanceof ArtilleryWeapon)) {
//            return;
//        }
//
//        // send change to the server
//        int nMode = m.switchMode(forward);
//        clientgui.getClient().sendModeChange(cen, wn, nMode);
//
//        // notify the player
//        if (m.canInstantSwitch(nMode)) {
//            clientgui.systemMessage(Messages.getString(
//                    "FiringDisplay.switched", new Object[] { m.getName(),
//                            m.curMode().getDisplayableName() }));
//
//        } else {
//            clientgui.systemMessage(Messages.getString(
//                    "FiringDisplay.willSwitch", new Object[] { m.getName(),
//                            m.pendingMode().getDisplayableName() }));
//        }
//
//        updateTarget();
//        clientgui.mechD.wPan.displayMech(ce());
//        clientgui.mechD.wPan.selectWeapon(wn);
//    }

//    /**
//     * Called when the current entity is done firing. Send out our attack queue
//     * to the server.
//     */
//    @Override
//    public void ready() {
//        if (attacks.isEmpty()
//                && GUIPreferences.getInstance().getNagForNoAction()) {
//            // comfirm this action
//            String title = Messages
//                    .getString("PrephaseDisplay.DontFireDialog.title");
//            String body = Messages
//                    .getString("PrephaseDisplay.DontFireDialog.message");
//            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
//            if (!response.getShowAgain()) {
//                GUIPreferences.getInstance().setNagForNoAction(false);
//            }
//            if (!response.getAnswer()) {
//                return;
//            }
//        }
//
//        // stop further input (hopefully)
//        disableButtons();
//
//        // remove temporary attacks from game & board
//        removeTempAttacks();
//
//        // send out attacks
//        clientgui.getClient().sendAttackData(cen, attacks);
//
//        // clear queue
//        attacks.removeAllElements();
//
//        if ((ce() != null) && ce().isWeapOrderChanged()) {
//            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
//        }
//        endMyTurn();
//    }

//    private void doSearchlight() {
//        // validate
//        if ((ce() == null) || (target == null)) {
//            throw new IllegalArgumentException(
//                    "current searchlight parameters are invalid");
//        }
//
//        if (!SearchlightAttackAction.isPossible(clientgui.getClient().getGame(), cen, target, null)) {
//            return;
//        }
//
//        // create and queue a searchlight action
//        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target
//                .getTargetType(), target.getTargetId());
//        attacks.addElement(saa);
//
//        // and add it into the game, temporarily
//        clientgui.getClient().getGame().addAction(saa);
//        clientgui.getBoardView().addAttack(saa);
//
//        // refresh weapon panel, as bth will have changed
//        updateTarget();
//    }

//    /**
//     * Adds a weapon attack with the currently selected weapon to the attack
//     * queue.
//     */
//    private void fire() {
//        // get the selected weaponnum
//        int weaponNum = clientgui.mechD.wPan.getSelectedWeaponNum();
//        Mounted mounted = ce().getEquipment(weaponNum);
//
//        // validate
//        if ((ce() == null) || (target == null) || (mounted == null)
//                || !(mounted.getType() instanceof WeaponType)) {
//            throw new IllegalArgumentException(
//                    "current fire parameters are invalid");
//        }
//
//        // declare searchlight, if possible
//        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
//            doSearchlight();
//        }
//
//        WeaponAttackAction waa = new WeaponAttackAction(cen,
//                target.getTargetType(), target.getTargetId(), weaponNum);
//        Game game = clientgui.getClient().getGame();
//        int distance = Compute.effectiveDistance(game, waa.getEntity(game),
//                waa.getTarget(game));
//        if ((mounted.getType().hasFlag(WeaponType.F_ARTILLERY))
//                || (mounted.isInBearingsOnlyMode()
//                && distance >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM)
//                || (mounted.getType() instanceof CapitalMissileWeapon
//                && Compute.isGroundToGround(ce(), target))) {
//            waa = new ArtilleryAttackAction(cen, target.getTargetType(),
//                    target.getTargetId(), weaponNum, clientgui.getClient()
//                    .getGame());
//            // Get the launch velocity for bearings-only telemissiles
//            if (mounted.getType() instanceof TeleOperatedMissileBayWeapon) {
//                TeleMissileSettingDialog tsd = new TeleMissileSettingDialog(clientgui.frame, clientgui.getClient().getGame());
//                tsd.setVisible(true);
//                waa.setLaunchVelocity(tsd.getSetting());
//                waa.updateTurnsTilHit(clientgui.getClient().getGame());
//            }
//        }
//
//        updateDisplayForPendingAttack(mounted, waa);
//    }

//    /**
//     * Worker function that handles setting associated ammo and other bookkeeping/UI updates
//     * for a pending weapon attack action.
//     */
//    public void updateDisplayForPendingAttack(Mounted mounted, WeaponAttackAction waa) {
//        // put this and the rest of the method into a separate function for access externally.
//        if ((null != mounted.getLinked())
//                && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)) {
//            Mounted ammoMount = mounted.getLinked();
//            waa.setAmmoId(ammoMount.getEntity().getEquipmentNum(ammoMount));
//            waa.setAmmoCarrier(ammoMount.getEntity().getId());
//            if (((AmmoType) ammoMount.getType()).getMunitionType() == AmmoType.M_VIBRABOMB_IV) {
//                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
//                        clientgui.frame);
//                vsd.setVisible(true);
//                waa.setOtherAttackInfo(vsd.getSetting());
//            }
//        }
//
//        // add the attack to our temporary queue
//        attacks.addElement(waa);
//
//        // and add it into the game, temporarily
//        clientgui.getClient().getGame().addAction(waa);
//
//        // set the weapon as used
//        mounted.setUsedThisRound(true);
//
//        // find the next available weapon
//        int nextWeapon = clientgui.mechD.wPan.selectNextWeapon();
//
//        // check; if there are no ready weapons, you're done.
//        if ((nextWeapon == -1) && GUIPreferences.getInstance().getAutoEndFiring()) {
//            ready();
//            return;
//        }
//
//        // otherwise, display firing info for the next weapon
//        clientgui.mechD.wPan.displayMech(ce());
//        clientgui.mechD.wPan.selectWeapon(nextWeapon);
//        updateTarget();
//        setDisengageEnabled(false);
//    }

//    /**
//     * Skips to the next weapon
//     */
//    private void nextWeapon() {
//        if (ce() == null) {
//            return;
//        }
//        int weaponId = clientgui.mechD.wPan.selectNextWeapon();
//
//        if (ce().getId() != clientgui.mechD.wPan.getSelectedEntityId()) {
//            clientgui.mechD.wPan.displayMech(ce());
//        }
//
//        if (weaponId == -1) {
//            setFireModeEnabled(false);
//        } else {
//            Mounted m = ce().getEquipment(weaponId);
//            setFireModeEnabled(m.isModeSwitchable());
//        }
//        updateTarget();
//    }

//    /**
//     * Skips to the previous weapon
//     */
//    void prevWeapon() {
//        if (ce() == null) {
//            return;
//        }
//        int weaponId = clientgui.mechD.wPan.selectPrevWeapon();
//
//        if (ce().getId() != clientgui.mechD.wPan.getSelectedEntityId()) {
//            clientgui.mechD.wPan.displayMech(ce());
//        }
//
//        if (weaponId == -1) {
//            setFireModeEnabled(false);
//        } else {
//            Mounted m = ce().getEquipment(weaponId);
//            setFireModeEnabled(m.isModeSwitchable());
//        }
//        updateTarget();
//    }
//
//    /**
//     * Removes all current fire
//     */
//    private void clearAttacks() {
//        // We may not have an entity selected yet (race condition).
//        if (ce() == null) {
//            return;
//        }
//
//        // remove attacks, set weapons available again
//        Enumeration<EntityAction> i = attacks.elements();
//        while (i.hasMoreElements()) {
//            Object o = i.nextElement();
//            if (o instanceof WeaponAttackAction) {
//                WeaponAttackAction waa = (WeaponAttackAction) o;
//                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
//            }
//        }
//        attacks.removeAllElements();
//
//        // remove temporary attacks from game & board
//        removeTempAttacks();
//
//        // restore any other movement to default
//        ce().setSecondaryFacing(ce().getFacing());
//        ce().setArmsFlipped(false);
//        setDisengageEnabled(ce().isOffBoard() && ce().canFlee());
//    }
//
//    /**
//     * Removes temp attacks from the game and board
//     */
//    private void removeTempAttacks() {
//        // remove temporary attacks from game & board
//        clientgui.getClient().getGame().removeActionsFor(cen);
//        clientgui.getBoardView().removeAttacksFor(ce());
//
//    }

//    /**
//     * removes the last action
//     */
//    private void removeLastFiring() {
//        if (!attacks.isEmpty()) {
//            Object o = attacks.lastElement();
//            if (o instanceof WeaponAttackAction) {
//                WeaponAttackAction waa = (WeaponAttackAction) o;
//                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
//                attacks.removeElement(o);
//                setDisengageEnabled(attacks.isEmpty() && ce().isOffBoard() && ce().canFlee());
//                clientgui.mechD.wPan.displayMech(ce());
//                clientgui.getClient().getGame().removeAction(o);
//                clientgui.getBoardView().refreshAttacks();
//            }
//        }
//    }

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

//    /**
//     * Have the player select a target from the entities at the given coords.
//     *
//     * @param pos - the <code>Coords</code> containing targets.
//     */
//    private Targetable chooseTarget(Coords pos) {
//
//        boolean friendlyFire = clientgui.getClient().getGame().getOptions()
//                .booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
//        // Assume that we have *no* choice.
//        Targetable choice = null;
//        Iterator<Entity> choices;
//
//        // Get the available choices, depending on friendly fire
//        if (friendlyFire) {
//            choices = clientgui.getClient().getGame().getEntities(pos);
//        } else {
//            choices = clientgui.getClient().getGame()
//                    .getEnemyEntities(pos, ce());
//        }
//
//        // Convert the choices into a List of targets.
//        List<Targetable> targets = new ArrayList<>();
//        final Player localPlayer = clientgui.getClient().getLocalPlayer();
//        while (choices.hasNext()) {
//            Targetable t = choices.next();
//            boolean isSensorReturn = false;
//            boolean isVisible = true;
//            if (t instanceof Entity) {
//                isSensorReturn = ((Entity) t).isSensorReturn(localPlayer);
//                isVisible = ((Entity) t).hasSeenEntity(localPlayer);
//            }
//            if (!ce().equals(t) && !isSensorReturn && isVisible) {
//                targets.add(t);
//            }
//        }
//
//        // Is there a building in the hex?
//        Building bldg = clientgui.getClient().getGame().getBoard()
//                .getBuildingAt(pos);
//        if (bldg != null) {
//            targets.add(new BuildingTarget(pos, clientgui.getClient().getGame()
//                    .getBoard(), Targetable.TYPE_BLDG_TAG));
//        }
//
//        targets.add(new HexTarget(pos, Targetable.TYPE_HEX_TAG));
//
//        // Do we have a single choice?
//        if (targets.size() == 1) {
//            // Return that choice.
//            choice = targets.get(0);
//        }
//
//        // If we have multiple choices, display a selection dialog.
//        else if (targets.size() > 1) {
//            String input = (String) JOptionPane
//                    .showInputDialog(
//                            clientgui,
//                            Messages.getString(
//                                    "FiringDisplay.ChooseTargetDialog.message",
//                                    new Object[] { pos.getBoardNum() }),
//                            Messages.getString("FiringDisplay.ChooseTargetDialog.title"),
//                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
//                                    .getDisplayArray(targets), null);
//            choice = SharedUtility.getTargetPicked(targets, input);
//        } // End have-choices
//
//        // Return the chosen unit.
//        return choice;
//
//    } // End private Targetable chooseTarget( Coords )

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
        // if we're ending the firing phase, unregister stuff.
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
            ready();
        }

//        if (ev.getActionCommand().equals(PrephaseCommand.FIRE_FIRE.getCmd())) {
//            fire();
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_SKIP.getCmd())) {
//            nextWeapon();
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_NEXT.getCmd())) {
//            selectEntity(clientgui.getClient().getNextEntityNum(cen));
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_NEXT_TARG.getCmd())) {
//            jumpToNextTarget();
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_FLIP_ARMS.getCmd())) {
//            updateFlipArms(!ce().getArmsFlipped());
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_MODE.getCmd())) {
//            changeMode(true);
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_CANCEL.getCmd())) {
//            clear();
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_SEARCHLIGHT.getCmd())) {
//            doSearchlight();
//        } else if (ev.getActionCommand().equals(PrephaseCommand.FIRE_DISENGAGE.getCmd())
//                && clientgui.doYesNoDialog(Messages.getString("MovementDisplay.EscapeDialog.title"),
//                Messages.getString("MovementDisplay.EscapeDialog.message"))) {
//            clear();
//            attacks.add(new DisengageAction(cen));
//            ready();
//        }
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
