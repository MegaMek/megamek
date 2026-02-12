/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.dialogs.phaseDisplay.TargetChoiceDialog;
import megamek.client.ui.widget.MegaMekButton;
import megamek.common.board.Coords;
import megamek.common.units.Targetable;
import megamek.common.actions.InitiateInfantryCombatAction;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

public class PreEndDeclarationsDisplay extends AttackPhaseDisplay {

    public enum PreEndCommand implements PhaseCommand {
        PREEND_INITIATE_INFANTRY_COMBAT("initiateInfantryCombat"),
        PREEND_NEXT("next");

        private final String cmd;
        private int priority;

        PreEndCommand(String c) {
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
            return Messages.getString("PreEndDeclarationsDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            return "";
        }
    }

    protected Map<PreEndCommand, MegaMekButton> buttons;
    protected Targetable target;

    /**
     * Sets the current target and updates button states
     */
    public void setTarget(Targetable newTarget) {
        this.target = newTarget;
        updateButtons();
    }

    public PreEndDeclarationsDisplay(ClientGUI clientgui) {
        super(clientgui);
        clientgui.getClient().getGame().addGameListener(this);

        // TODO: Keyboard shortcuts need parent class support for keybind registration
        // This would require updates to AbstractPhaseDisplay or similar parent classes

        setupStatusBar(Messages.getString("PreEndDeclarationsDisplay.waitingForPreEndDeclarationsPhase"));
        buttons = new HashMap<>();
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();

        // Initialize buttons to disabled state
        setInitiateInfantryCombatEnabled(false);
        updateDonePanel();
    }

    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
    }

    @Override
    protected void setButtons() {
        buttons.put(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT, createButton(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT.getCmd(), "PreEndDeclarationsDisplay."));
        buttons.put(PreEndCommand.PREEND_NEXT, createButton(PreEndCommand.PREEND_NEXT.getCmd(), "PreEndDeclarationsDisplay."));
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        // Add tooltips if needed
    }

    @Override
    protected List<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
        buttonList.add(buttons.get(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT));
        buttonList.add(buttons.get(PreEndCommand.PREEND_NEXT));
        return buttonList;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equals(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT.getCmd())) {
            initiateInfantryCombat();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(currentEntity));
        }
    }

    // TODO: Add propertyChange handler for keyboard shortcuts once parent class infrastructure exists

    private void initiateInfantryCombat() {
        if (target == null) {
            return;
        }

        Entity ce = game.getEntity(currentEntity);
        if (!(ce instanceof Infantry inf)) {
            return;
        }

        // Check if already in combat
        if (inf.getInfantryCombatTargetId() != Entity.NONE) {
            clientgui.doAlertDialog("Impossible",
                  "Already engaged in combat");
            return;
        }

        // Check if target is a building
        Entity targetEntity = game.getEntity(target.getId());
        if (!(targetEntity instanceof AbstractBuildingEntity)) {
            clientgui.doAlertDialog("Impossible",
                  "Target must be a building");
            return;
        }

        // Check if same hex
        if (!ce.getPosition().equals(targetEntity.getPosition())) {
            clientgui.doAlertDialog("Impossible",
                  "Must be in same hex as target");
            return;
        }

        // Check if combat already exists in this building
        boolean combatExists = game.getEntitiesVector().stream()
              .filter(e -> e instanceof Infantry)
              .filter(e -> e.getPosition().equals(targetEntity.getPosition()))
              .map(e -> (Infantry) e)
              .anyMatch(e -> e.getInfantryCombatTargetId() != Entity.NONE);

        if (combatExists) {
            clientgui.doAlertDialog("Impossible",
                  "Combat already exists - use reinforce action instead");
            return;
        }

        String title = Messages.getString("PreEndDeclarationsDisplay.InitiateInfantryCombatDialog.title");
        String message = Messages.getString("PreEndDeclarationsDisplay.InitiateInfantryCombatDialog.message",
              ce.getDisplayName(),
              target.getDisplayName());

        if (clientgui.doYesNoDialog(title, message)) {
            addAttack(new InitiateInfantryCombatAction(currentEntity, target.getId()));
            ready();
        }
    }

    @Override
    protected String getDoneButtonLabel() {
        return Messages.getString("PreEndDeclarationsDisplay.Done");
    }

    @Override
    protected String getSkipTurnButtonLabel() {
        return Messages.getString("PreEndDeclarationsDisplay.Skip");
    }

    /**
     * Checks for nag conditions before ending turn.
     * @return true if the turn should be cancelled
     */
    private boolean checkNags() {
        if (attacks.isEmpty() && currentEntity() != null) {
            String title = "Skip Turn?";
            String body = "You haven't initiated any infantry combat. Skip turn anyway?";
            if (!clientgui.doYesNoDialog(title, body)) {
                return true;  // User cancelled
            }
        }
        return false;
    }

    @Override
    public void ready() {
        if (checkNags()) {
            return;
        }

        // Always send attack data to advance turn, even if empty
        clientgui.getClient().sendAttackData(currentEntity, attacks.toVector());
        removeAllAttacks();
        sendDone();
    }

    @Override
    public void clear() {
        setTarget(null);
        removeAllAttacks();
    }

    //
    // BoardViewListener
    //
    @Override
    public void hexSelected(BoardViewEvent event) {
        if (isIgnoringEvents()) {
            return;
        }

        Coords coords = event.getCoords();
        if (isMyTurn() && (coords != null) && (currentEntity() != null)) {
            Targetable chosenTarget = chooseTarget(coords);

            if (chosenTarget != null) {
                target(chosenTarget);
            }
        }
    }

    @Override
    public void hexMoused(BoardViewEvent event) {
        if (isIgnoringEvents() || !isMyTurn()) {
            return;
        }

        // Update cursor position for hex info display
        //if (event.getCoords() != null) {
        //    clientgui.getBoardView().cursor(event.getCoords());
        //}
        if (clientgui.getClient().isMyTurn()
              && (event.getButton() == MouseEvent.BUTTON1)) {
            if (event.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
                if (!event.getCoords().equals(
                      event.getBoardView().getLastCursor())) {
                    event.getBoardView().cursor(event.getCoords());
                }
            } else if (event.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                event.getBoardView().select(event.getCoords());
            }
        }
    }

    @Override
    public void unitSelected(BoardViewEvent event) {
        if (isIgnoringEvents()) {
            return;
        }

        Entity clickedEntity = game.getEntity(event.getEntityId());
        if (clickedEntity != null && isMyTurn()) {
            if (clientgui.getClient().getMyTurn().isValidEntity(clickedEntity, game)) {
                selectEntity(clickedEntity.getId());
            }
        }
    }

    /**
     * Sets the current target and updates display
     */
    public void target(Targetable t) {
        setTarget(t);
        //clientgui.getBoardView().select(t.getPosition());
    }

    /**
     * Chooses a target from the given hex coordinates.
     * If multiple entities exist at the hex, shows a dialog for selection.
     */
    private Targetable chooseTarget(Coords coords) {
        List<Targetable> targets = new ArrayList<>();

        // Gather all entities at hex
        for (Entity e : game.getEntitiesVector()) {
            if (e.getPosition() != null && e.getPosition().equals(coords) && e.isBoardable()) {
                targets.add(e);
            }
        }

        if (targets.isEmpty()) {
            return null;
        } else if (targets.size() == 1) {
            return targets.get(0);
        } else {
            // Multiple targets - show choice dialog
            return TargetChoiceDialog.showSingleChoiceDialog(
                clientgui.getFrame(),
                "PreEndDeclarationsDisplay.ChooseTargetDialog.title",
                Messages.getString("PreEndDeclarationsDisplay.ChooseTargetDialog.message"),
                targets,
                clientgui,
                game.getEntity(currentEntity)
            );
        }
    }

    /**
     * Updates button states based on current game state
     */
    protected void updateButtons() {
        Entity ce = game.getEntity(currentEntity);

        if (!(ce instanceof Infantry inf)) {
            setInitiateInfantryCombatEnabled(false);
            return;
        }

        boolean canInitiate = inf.canInitiateInfantryVsInfantryCombat()
              && target != null
              && isValidBuildingTargetNoCombat(ce, target);

        setInitiateInfantryCombatEnabled(canInitiate);
        updateDonePanel();
    }

    /**
     * Checks if the target is a valid building with no existing combat
     */
    private boolean isValidBuildingTargetNoCombat(Entity entity, Targetable target) {
        Entity targetEntity = game.getEntity(target.getId());
        if (!(targetEntity instanceof AbstractBuildingEntity)) {
            return false;
        }

        if (!entity.getPosition().equals(targetEntity.getPosition())) {
            return false;
        }

        // Check if combat already exists in this building
        boolean combatExists = game.getEntitiesVector().stream()
              .filter(e -> e instanceof Infantry)
              .filter(e -> e.getPosition().equals(targetEntity.getPosition()))
              .map(e -> (Infantry) e)
              .anyMatch(e -> e.getInfantryCombatTargetId() != Entity.NONE);

        return !combatExists;
    }

    /**
     * Enables or disables the initiate infantry combat button
     */
    protected void setInitiateInfantryCombatEnabled(boolean enabled) {
        buttons.get(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT.getCmd(), enabled);
    }

    /**
     * Selects an entity for this turn
     */
    private void selectEntity(int entityId) {
        if (game.getEntity(entityId) == null) {
            return;
        }

        currentEntity = entityId;
        clientgui.setSelectedEntityNum(entityId);
        clientgui.getUnitDisplay().displayEntity(game.getEntity(entityId));
        clientgui.getBoardView().highlight(game.getEntity(entityId).getPosition());
        clientgui.getBoardView().centerOnHex(game.getEntity(entityId).getPosition());

        updateButtons();
    }

    /**
     * Called when the player's turn begins
     */
    private void beginMyTurn() {
        clientgui.maybeShowUnitDisplay();
        setTarget(null);

        // Auto-select first entity
        if (GUIP.getAutoSelectNextUnit()) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
        }

        updateButtons();
        butDone.setEnabled(false);
        startTimer();
    }

    /**
     * Called when the player's turn ends
     */
    private void endMyTurn() {
        currentEntity = Entity.NONE;
        stopTimer();
        disableButtons();
        setTarget(null);
        clientgui.onAllBoardViews(IBoardView::clearMarkedHexes);
    }

    /**
     * Disables all action buttons
     */
    private void disableButtons() {
        setInitiateInfantryCombatEnabled(false);
        butDone.setEnabled(false);
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(megamek.common.event.GameTurnChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        if (!game.getPhase().isPreEndDeclarations()) {
            return;
        }

        if (!game.getPhase().isSimultaneous(game)) {
            if (clientgui.getClient().isMyTurn()) {
                beginMyTurn();
                String s = getRemainingPlayerWithTurns();
                setStatusBarText(Messages.getString("PreEndDeclarationsDisplay.its_your_turn") + s);
                clientgui.bingMyTurn();
            } else {
                endMyTurn();
                String playerName;

                if (e.getPlayer() != null) {
                    playerName = e.getPlayer().getName();
                } else {
                    playerName = "Unknown";
                }

                setStatusBarText(Messages.getString("PreEndDeclarationsDisplay.its_others_turn", playerName));
                clientgui.bingOthersTurn();
            }
        }
    }

    @Override
    public void gamePhaseChange(megamek.common.event.GamePhaseChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (game.getPhase().isLounge()) {
            endMyTurn();
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn() && !game.getPhase().isPreEndDeclarations()) {
            endMyTurn();
        }

        if (game.getPhase().isPreEndDeclarations()) {
            setStatusBarText(Messages.getString("PreEndDeclarationsDisplay.waitingForPreEndDeclarationsPhase"));
        }
    }
}
