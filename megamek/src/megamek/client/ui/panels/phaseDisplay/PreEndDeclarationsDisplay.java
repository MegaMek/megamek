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
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.dialogs.phaseDisplay.TargetChoiceDialog;
import megamek.client.ui.widget.MegaMekButton;
import megamek.common.actions.InitiateInfantryCombatAction;
import megamek.common.board.Coords;
import megamek.common.equipment.BridgeLayerLogic;
import megamek.common.equipment.BridgeLayerState;
import megamek.common.equipment.MiscMounted;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

public class PreEndDeclarationsDisplay extends AttackPhaseDisplay {

    /** General pre-end declarations phase diagnostics; tagged [PreEnd]. */
    private static final MMLogger LOGGER = MMLogger.create(PreEndDeclarationsDisplay.class);

    /** Bridge-Layer (AVLB) diagnostics; tagged [AVLB] (shared feature logger, see BridgeLayerState). */
    private static final MMLogger AVLB_LOGGER = MMLogger.create(BridgeLayerState.DIAGNOSTIC_LOGGER_NAME);

    public enum PreEndCommand implements PhaseCommand {
        PREEND_INITIATE_INFANTRY_COMBAT("initiateInfantryCombat"),
        PREEND_DEPLOY_BRIDGE("deployBridge"),
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

    /** True while waiting for the player to click the front hex to confirm a Bridge-Layer (AVLB) deployment. */
    private boolean selectingDeployBridgeHex = false;

    /** Index into the current unit's deployable bridgelayers - the one the multi-mode Deploy Bridge button will lay. */
    private int selectedDeployBridgeIndex = 0;

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
        buttons.put(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT,
              createButton(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT.getCmd(), "PreEndDeclarationsDisplay."));
        buttons.put(PreEndCommand.PREEND_DEPLOY_BRIDGE,
              createButton(PreEndCommand.PREEND_DEPLOY_BRIDGE.getCmd(), "PreEndDeclarationsDisplay."));
        buttons.put(PreEndCommand.PREEND_NEXT,
              createButton(PreEndCommand.PREEND_NEXT.getCmd(), "PreEndDeclarationsDisplay."));
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
        buttonList.add(buttons.get(PreEndCommand.PREEND_DEPLOY_BRIDGE));
        buttonList.add(buttons.get(PreEndCommand.PREEND_NEXT));
        return buttonList;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equals(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT.getCmd())) {
            initiateInfantryCombat();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_DEPLOY_BRIDGE.getCmd())) {
            deployBridge();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(currentEntity));
        }
    }

    /**
     * Enters confirm mode for a Bridge-Layer (AVLB) deployment: the hex directly in front of the unit, along its
     * facing, is the only valid target (TM p.242 / TW), so it is highlighted and the player clicks it to confirm. The
     * bridge is laid there at the end of the next turn if the unit stays stationary.
     */
    private void deployBridge() {
        Entity entity = game.getEntity(currentEntity);
        if (entity == null) {
            AVLB_LOGGER.debug("[AVLB] deploy bridge button pressed with no current entity");
            return;
        }
        if (!BridgeLayerLogic.canDeclareBridgeDeploy(entity, game)) {
            AVLB_LOGGER.debug("[AVLB] {}: deploy bridge pressed but unit is not eligible (see prior [AVLB] gate line)",
                  entity.getShortName());
            return;
        }
        List<MiscMounted> deployable = BridgeLayerLogic.getDeployableBridgeLayers(entity);
        if (deployable.isEmpty()) {
            AVLB_LOGGER.debug("[AVLB] {}: deploy bridge pressed but no deployable bridgelayer", entity.getShortName());
            return;
        }
        // Multi-mode button: the first press enters confirm mode for the first bridge; pressing it again while still
        // confirming cycles to the next bridge (e.g. Right -> Left on the Prometheus). The player commits by clicking
        // the highlighted front hex.
        if (selectingDeployBridgeHex && (deployable.size() > 1)) {
            selectedDeployBridgeIndex = (selectedDeployBridgeIndex + 1) % deployable.size();
        } else {
            selectingDeployBridgeHex = true;
            selectedDeployBridgeIndex = 0;
        }
        MiscMounted selected = deployable.get(selectedDeployBridgeIndex);
        updateDeployBridgeButtonLabel(entity, deployable);
        Coords frontHex = BridgeLayerLogic.getBridgeLayerTargetCoords(entity);
        if (frontHex != null) {
            clientgui.getBoardView().highlight(frontHex);
        }
        if (deployable.size() > 1) {
            MiscMounted next = deployable.get((selectedDeployBridgeIndex + 1) % deployable.size());
            setStatusBarText(Messages.getString("PreEndDeclarationsDisplay.SelectDeployBridgeHexMulti",
                  entity.getLocationName(selected.getLocation()), entity.getLocationName(next.getLocation())));
        } else {
            setStatusBarText(Messages.getString("PreEndDeclarationsDisplay.SelectDeployBridgeHex"));
        }
        AVLB_LOGGER.debug("[AVLB] {}: deploy-confirm; selected bridge at loc {} ({} of {}); front hex {} (facing {})",
              entity.getShortName(), selected.getLocation(), selectedDeployBridgeIndex + 1, deployable.size(), frontHex,
              entity.getFacing());
    }

    /**
     * Sets the Deploy Bridge button label: a plain "Deploy Bridge" for a single bridge, or "Deploy &lt;side&gt; Bridge"
     * (the currently selected bridge's mounted-location name) when the unit carries more than one.
     *
     * @param entity     the current unit
     * @param deployable the unit's deployable bridgelayers, in equipment order
     */
    private void updateDeployBridgeButtonLabel(Entity entity, List<MiscMounted> deployable) {
        MegaMekButton button = buttons.get(PreEndCommand.PREEND_DEPLOY_BRIDGE);
        if (deployable.size() <= 1) {
            button.setText(Messages.getString("PreEndDeclarationsDisplay.deployBridge"));
        } else {
            MiscMounted selected = deployable.get(Math.min(selectedDeployBridgeIndex, deployable.size() - 1));
            button.setText(Messages.getString("PreEndDeclarationsDisplay.deployBridgeSide",
                  entity.getLocationName(selected.getLocation())));
        }
    }

    /**
     * Confirms a Bridge-Layer (AVLB) deployment when the player clicks the hex in front of the unit; a click on any
     * other hex keeps waiting for the correct hex. TM p.242 / TW.
     *
     * @param targetCoords the hex the player clicked
     */
    private void completeDeployBridge(Coords targetCoords) {
        selectingDeployBridgeHex = false;
        Entity entity = game.getEntity(currentEntity);
        if ((entity == null) || !BridgeLayerLogic.canDeclareBridgeDeploy(entity, game)) {
            AVLB_LOGGER.debug("[AVLB] deploy confirm aborted: unit gone or no longer eligible");
            return;
        }
        Coords frontHex = BridgeLayerLogic.getBridgeLayerTargetCoords(entity);
        if (!targetCoords.equals(frontHex)) {
            // The only valid target is the hex in front; keep waiting for it.
            AVLB_LOGGER.debug("[AVLB] {}: clicked {} but the only valid target is the front hex {}; still waiting",
                  entity.getShortName(), targetCoords, frontHex);
            selectingDeployBridgeHex = true;
            setStatusBarText(Messages.getString("PreEndDeclarationsDisplay.SelectDeployBridgeHex"));
            return;
        }
        List<MiscMounted> deployable = BridgeLayerLogic.getDeployableBridgeLayers(entity);
        if (deployable.isEmpty()) {
            return;
        }
        MiscMounted selected = deployable.get(Math.min(selectedDeployBridgeIndex, deployable.size() - 1));
        AVLB_LOGGER.debug("[AVLB] {}: confirmed deploy of bridge at loc {} at front hex {}; sending declaration",
              entity.getShortName(), selected.getLocation(), frontHex);
        clientgui.getClient().sendDeployBridge(currentEntity, entity.getEquipmentNum(selected));
        ready();
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
            clientgui.addToast(ToastLevel.ERROR,
                  Messages.getString("InfantryVsInfantryCombatDisplay.alreadyEngaged"));
            return;
        }

        // Check if target is a building
        Entity targetEntity = game.getEntity(target.getId());
        if (!(targetEntity instanceof AbstractBuildingEntity)) {
            clientgui.addToast(ToastLevel.ERROR,
                  Messages.getString("InfantryVsInfantryCombatDisplay.targetMustBeBuilding"));
            return;
        }

        // Check if same hex
        if (!ce.getPosition().equals(targetEntity.getPosition())) {
            clientgui.addToast(ToastLevel.ERROR,
                  Messages.getString("InfantryVsInfantryCombatDisplay.mustBeSameHex"));
            return;
        }

        // Check if combat already exists in this building
        boolean combatExists = game.getEntitiesVector().stream()
              .filter(e -> e instanceof Infantry)
              .filter(e -> e.getPosition() != null && e.getPosition().equals(targetEntity.getPosition()))
              .map(e -> (Infantry) e)
              .anyMatch(e -> e.getInfantryCombatTargetId() != Entity.NONE);

        if (combatExists) {
            clientgui.addToast(ToastLevel.WARNING,
                  Messages.getString("InfantryVsInfantryCombatDisplay.combatAlreadyExists"));
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
     *
     * @return {@code true} if the turn should be cancelled
     */
    private boolean checkNags() {
        Entity entity = currentEntity();
        if (entity == null) {
            return false;
        }
        // Only nag about un-declared infantry combat for a unit that could actually initiate it. Other units in this
        // phase (e.g. a bridgelayer that chose not to deploy) end their turn silently.
        if (attacks.isEmpty() && entity.canInitiateInfantryVsInfantryCombat()) {
            LOGGER.debug("[PreEnd] {}: nag - infantry-combat-capable but none declared; confirming skip",
                  entity.getShortName());
            String title = Messages.getString("PreEndDeclarationsDisplay.skipTurn.title");
            String body = Messages.getString("PreEndDeclarationsDisplay.skipTurn.message");
            return !clientgui.doYesNoDialog(title, body);  // User canceled
        }
        return false;
    }

    @Override
    public void ready() {
        if (checkNags()) {
            return;
        }

        LOGGER.debug("[PreEnd] entity {} ready; {} queued action(s), advancing turn", currentEntity, attacks.size());
        // Always send attack data to advance turn, even if empty
        clientgui.getClient().sendAttackData(currentEntity, attacks.toVector());
        removeAllAttacks();

        endMyTurn();
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
            // Confirming a Bridge-Layer (AVLB) deploy target takes priority over normal target selection.
            if (selectingDeployBridgeHex) {
                completeDeployBridge(coords);
                return;
            }

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
     * Chooses a target from the given hex coordinates. If multiple entities exist at the hex, shows a dialog for
     * selection.
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
            return targets.getFirst();
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
        Entity entity = game.getEntity(currentEntity);

        // Bridge-Layer (AVLB) deployment is available to any eligible unit (vehicle or quad Mek), independent of the
        // infantry-combat declaration below.
        boolean canDeployBridge = (entity != null) && BridgeLayerLogic.canDeclareBridgeDeploy(entity, game);
        setDeployBridgeEnabled(canDeployBridge);
        if (entity != null) {
            // Show the button on its first selectable bridge from the start (e.g. "Deploy Right Bridge" on a unit
            // with two bridges) rather than a generic label.
            updateDeployBridgeButtonLabel(entity, BridgeLayerLogic.getDeployableBridgeLayers(entity));
        }
        LOGGER.debug("[PreEnd] updateButtons for {}: deployBridge={}",
              (entity == null) ? "<none>" : entity.getShortName(), canDeployBridge);

        if (!(entity instanceof Infantry infantry)) {
            setInitiateInfantryCombatEnabled(false);
            updateDonePanel();
            return;
        }

        boolean canInitiate = infantry.canInitiateInfantryVsInfantryCombat()
              && target != null
              && isValidBuildingTargetNoCombat(entity, target);

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
              .filter(e -> e.getPosition() != null && e.getPosition().equals(targetEntity.getPosition()))
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
     * Enables or disables the Deploy Bridge button.
     */
    protected void setDeployBridgeEnabled(boolean enabled) {
        buttons.get(PreEndCommand.PREEND_DEPLOY_BRIDGE).setEnabled(enabled);
    }

    /**
     * Selects an entity for this turn
     */
    private void selectEntity(int entityId) {
        Entity selected = game.getEntity(entityId);
        if (selected == null) {
            return;
        }
        LOGGER.debug("[PreEnd] selected {} (canDeployBridge={}, canInitiateInfantryCombat={})",
              selected.getShortName(), BridgeLayerLogic.canDeclareBridgeDeploy(selected, game),
              selected.canInitiateInfantryVsInfantryCombat());

        currentEntity = entityId;
        // Reset the multi-mode Deploy Bridge selection for the newly selected unit; updateButtons() sets the label.
        selectingDeployBridgeHex = false;
        selectedDeployBridgeIndex = 0;
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
        LOGGER.debug("[PreEnd] pre-end declarations turn begins for the local player");
        clientgui.maybeShowUnitDisplay();
        setTarget(null);
        selectingDeployBridgeHex = false;

        // Auto-select first entity
        if (GUIP.getAutoSelectNextUnit()) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
        }

        // Note: do not force butDone disabled here - updateButtons() -> updateDonePanel() already sets the correct
        // Done/Skip state. With "nag for no action" off, the Done button itself is the Skip-Turn button, so disabling
        // it would wrongly gray out Skip for a unit that just wants to pass (e.g. a bridgelayer not deploying).
        updateButtons();
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
        setDeployBridgeEnabled(false);
        selectingDeployBridgeHex = false;
        selectedDeployBridgeIndex = 0;
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
