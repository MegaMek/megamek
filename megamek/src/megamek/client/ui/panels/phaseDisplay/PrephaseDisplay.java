/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.MekPanelTabStrip;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * PrephaseDisplay for revealing hidden units. This occurs before Move and Firing
 */
public class PrephaseDisplay extends StatusBarPhaseDisplay implements ListSelectionListener {
    private static final MMLogger logger = MMLogger.create(PrephaseDisplay.class);

    @Serial
    private static final long serialVersionUID = 3441669419807288865L;

    /**
     * This enumeration lists all the possible ActionCommands that can be carried out during the Prephase. Each command
     * has a string for the command plus a flag that determines what unit type it is appropriate for.
     */
    public enum PrephaseCommand implements PhaseCommand {
        PREPHASE_NEXT("prephaseNext"),
        PREPHASE_REVEAL("prephaseReveal"),
        PREPHASE_CANCEL_REVEAL("prephaseCancelReveal");

        final String cmd;

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

            String msg_next = Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");

            if (this == PREPHASE_NEXT) {
                result = "<BR>";
                result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
            }

            return result;
        }
    }

    // buttons
    protected Map<PrephaseCommand, MegaMekButton> buttons;

    private int cen = Entity.NONE; // current entity number

    private final GamePhase phase;

    protected final ClientGUI clientgui;

    /**
     * Creates and lays out a new PreFiring or PreMovement phase display for the specified clientGUi.getClient().
     */
    public PrephaseDisplay(final ClientGUI clientGUI, GamePhase phase) {
        super(clientGUI);
        this.clientgui = clientGUI;
        this.phase = phase;

        setupStatusBar(Messages.getFormattedString("PrephaseDisplay.waitingForPrephasePhase", phase.toString()));
        setButtons();
        setButtonsTooltips();
        butDone.setText(Messages.getString("PrephaseDisplay.Done"));
        butDone.setEnabled(false);
        setupButtonPanel();
        registerKeyCommands();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (PrephaseCommand.values().length * 1.25 + 0.5));
        for (PrephaseCommand cmd : PrephaseCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "PrephaseDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (PrephaseCommand cmd : PrephaseCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), "PrephaseDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    protected void registerKeyCommands() {
        MegaMekController controller = clientgui.controller;
        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT, this,
              () -> selectEntity(clientgui.getClient().getNextEntityNum(cen)));
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT, this,
              () -> selectEntity(clientgui.getClient().getPrevEntityNum(cen)));
    }

    /**
     * Have the panel register itself as a listener wherever it's needed.
     * <p>
     * According to http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a major bad no-no to perform
     * these registrations before the constructor finishes, so this function has to be called after the panel is
     * created. Please note, this restriction only applies to listeners for objects that aren't on the panel itself.
     */
    public void initializeListeners() {
        game().addGameListener(this);
        clientgui.getUnitDisplay().wPan.weaponList.addListSelectionListener(this);
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
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
        // clear any previously considered actions
        if (en != cen) {
            refreshAll();
        }
        Client client = clientgui.getClient();

        if (client.getGame().getEntity(en) != null) {

            cen = en;
            clientgui.setSelectedEntityNum(en);

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (null == currentEntity().getPosition()) {

                // Walk through the list of entities for this player.
                for (int nextId = client.getNextEntityNum(en); nextId != en; nextId = client.getNextEntityNum(nextId)) {

                    Entity nextEntity = client.getEntity(nextId);

                    if (nextEntity != null && null != nextEntity.getPosition()) {
                        cen = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (null == currentEntity().getPosition()) {
                    logger.error("Could not find an on-board entity: {}", en);
                    return;
                }
            }

            clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
            clientgui.getBoardView(currentEntity()).highlight(currentEntity().getPosition());

            refreshAll();

            if (!clientgui.isCurrentBoardViewShowingAnimation() && !currentEntity().isOffBoard()) {
                clientgui.centerOnUnit(currentEntity());
            }
        } else {
            logger.error("Tried to select non-existent entity: {}", en);
        }
    }

    private void refreshButtons() {
        final Entity ce = currentEntity();

        if (ce == null || ce.isDone()) {
            // how get here?
            disableButtons();
            return;
        }

        setStatusBarText(
              Messages.getFormattedString("PrephaseDisplay.its_your_turn", phase.toString(), ce.getDisplayName()));

        boolean isRevealing = !ce.getHiddenActivationPhase().isUnknown();
        setRevealEnabled(!isRevealing);
        setCancelRevealEnabled(isRevealing);
        setNextEnabled(true);
        butDone.setEnabled(true);
    }

    @Override
    public void ready() {
        disableButtons();
        clientgui.getClient().sendPrePhaseData(cen);
        endMyTurn();
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        setStatusBarText(Messages.getFormattedString("PrephaseDisplay.its_your_turn", phase.toString(), ""));
        butDone.setText("<html><b>" + Messages.getString("PrephaseDisplay.Done") + "</b></html>");

        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();

        if (GUIP.getAutoSelectNextUnit()) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
        }

        if (!clientgui.isCurrentBoardViewShowingAnimation()) {
            clientgui.maybeShowUnitDisplay();
        }

        setupButtonPanel();
        refreshButtons();
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        cen = Entity.NONE;
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).clearMovementData());
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();
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

        if (currentEntity() == null) {
            return;
        }
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).redrawEntity(currentEntity()));
        clientgui.getUnitDisplay().displayEntity(currentEntity());
        if (GUIP.getFireDisplayTabDuringFiringPhases()) {
            clientgui.getUnitDisplay().showPanel(MekPanelTabStrip.WEAPONS);
        }
        clientgui.getUnitDisplay().wPan.selectFirstWeapon();
    }

    /**
     * Returns the current entity.
     */
    Entity currentEntity() {
        return game().getEntity(cen);
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
            b.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            b.getBoardView().select(b.getCoords());
        }
    }

    // GameListener
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (game().getPhase().isLounge()) {
            endMyTurn();
        }
        // On simultaneous phases, each player ending their turn will generate a turn
        // change
        // We want to ignore turns from other players and only listen to events we
        // generated
        // Except on the first turn
        if (game().getPhase().isSimultaneous(game())
              && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
              && (game().getTurnIndex() != 0)) {
            return;
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (game().getPhase() == phase) {
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

    // GameListener
    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn() && (game().getPhase() != phase)) {
            endMyTurn();
        }

        if (game().getPhase() == phase) {
            setStatusBarText(Messages
                  .getFormattedString("PrephaseDisplay.waitingForPrephasePhase", phase.toString()));
        }
    }

    // GameListener
    @Override
    public void gameEntityChange(GameEntityChangeEvent event) {
        if (!event.getEntity().equals(currentEntity())) {
            return;
        }

        // Reviewers: Is this the right place to catch the change applied by a server packet
        // that changes an entity from done to not-done?
        if (currentEntity().isDone()) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
        }

        refreshButtons();
    }

    // ActionListener
    @Override
    public void actionPerformed(ActionEvent ev) {
        if (isIgnoringEvents() || !isMyTurn()) {
            return;
        }

        if (ev.getActionCommand().equals(PrephaseCommand.PREPHASE_REVEAL.getCmd())) {
            reveal();
        } else if (ev.getActionCommand().equals(PrephaseCommand.PREPHASE_CANCEL_REVEAL.getCmd())) {
            cancelReveal();
        } else if (ev.getActionCommand().equals(PrephaseCommand.PREPHASE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
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
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        refreshAll();
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && (currentEntity() != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.centerOnUnit(currentEntity());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity selectedUnit = game().getEntity(b.getEntityId());
        if (selectedUnit == null) {
            return;
        }
        if (isMyTurn()) {
            if (clientgui.getClient().getMyTurn().isValidEntity(selectedUnit, game())) {
                selectEntity(selectedUnit.getId());
            }
        } else {
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(selectedUnit);
            if (selectedUnit.isDeployed()) {
                clientgui.centerOnUnit(selectedUnit);
            }
        }
    }

    @Override
    public void removeAllListeners() {
        game().removeGameListener(this);
        clientgui.boardViews().forEach(bv -> bv.removeBoardViewListener(this));
        clientgui.getUnitDisplay().wPan.weaponList.removeListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {}

    private Game game() {
        return clientgui.getClient().getGame();
    }
}
