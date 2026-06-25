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
import megamek.client.ui.dialogs.phaseDisplay.AbandonUnitDialog;
import megamek.client.ui.dialogs.phaseDisplay.DetonateChargesDialog;
import megamek.client.ui.dialogs.phaseDisplay.MinesweeperActivationDialog;
import megamek.client.ui.dialogs.phaseDisplay.NovaNetworkDialog;
import megamek.client.ui.dialogs.phaseDisplay.TargetChoiceDialog;
import megamek.client.ui.dialogs.phaseDisplay.VariableRangeTargetingDialog;
import megamek.client.ui.widget.MegaMekButton;
import megamek.common.Player;
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
        PREEND_NOVA_NETWORK("novaNetwork"),
        PREEND_VAR_RANGE_TARGETING("varRangeTargeting"),
        PREEND_ABANDON("abandon"),
        PREEND_DETONATE_CHARGES("detonateCharges"),
        PREEND_MINESWEEPER("minesweeper"),
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
     * {@code true} once the local player applied a player-wide declaration (Nova, Variable Targeting, abandon, charges,
     * minesweeper) this turn. These send their data directly rather than queuing an attack, so this flag drives the
     * end-turn button to read "Done" instead of "Skip Turn" - otherwise the player gets no sign the declaration took.
     */
    private boolean declarationMade;

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
        for (PreEndCommand command : PreEndCommand.values()) {
            buttons.put(command, createButton(command.getCmd(), "PreEndDeclarationsDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        // The end-phase declaration buttons carry rulebook references; the infantry-combat and next buttons do not.
        setTooltip(PreEndCommand.PREEND_NOVA_NETWORK);
        setTooltip(PreEndCommand.PREEND_VAR_RANGE_TARGETING);
        setTooltip(PreEndCommand.PREEND_ABANDON);
        setTooltip(PreEndCommand.PREEND_DETONATE_CHARGES);
        setTooltip(PreEndCommand.PREEND_MINESWEEPER);
    }

    private void setTooltip(PreEndCommand command) {
        buttons.get(command)
              .setToolTipText(Messages.getString("PreEndDeclarationsDisplay." + command.getCmd() + ".tooltip"));
    }

    @Override
    protected List<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
        buttonList.add(buttons.get(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT));
        buttonList.add(buttons.get(PreEndCommand.PREEND_NOVA_NETWORK));
        buttonList.add(buttons.get(PreEndCommand.PREEND_VAR_RANGE_TARGETING));
        buttonList.add(buttons.get(PreEndCommand.PREEND_ABANDON));
        buttonList.add(buttons.get(PreEndCommand.PREEND_DETONATE_CHARGES));
        buttonList.add(buttons.get(PreEndCommand.PREEND_MINESWEEPER));
        buttonList.add(buttons.get(PreEndCommand.PREEND_DEPLOY_BRIDGE));
        buttonList.add(buttons.get(PreEndCommand.PREEND_NEXT));
        return buttonList;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equals(PreEndCommand.PREEND_INITIATE_INFANTRY_COMBAT.getCmd())) {
            initiateInfantryCombat();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_NOVA_NETWORK.getCmd())) {
            showNovaNetworkDialog();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_VAR_RANGE_TARGETING.getCmd())) {
            showVariableRangeTargetingDialog();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_ABANDON.getCmd())) {
            showAbandonDialog();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_DETONATE_CHARGES.getCmd())) {
            showDetonateChargesDialog();
        } else if (ev.getActionCommand().equals(PreEndCommand.PREEND_MINESWEEPER.getCmd())) {
            showMinesweeperDialog();
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
        // phase (a unit making an end-phase declaration, or a bridgelayer that chose not to deploy) end silently.
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
        LOGGER.debug("[PreEnd] ready: entity {} advancing turn with {} action(s)", currentEntity, attacks.size());
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

        // The end-phase declarations are player-wide: their dialogs act on every eligible unit (or charge) the local
        // player owns, so they are enabled on the player's turn regardless of which unit is currently selected.
        boolean nova = hasNovaUnits();
        boolean variableRange = hasVariableRangeUnits();
        boolean abandon = hasAbandonableUnits();
        boolean charges = hasDemolitionCharges();
        boolean minesweeper = hasMinesweeperUnits();
        setNovaNetworkEnabled(nova);
        setVariableRangeTargetingEnabled(variableRange);
        setAbandonEnabled(abandon);
        setDetonateChargesEnabled(charges);
        setMinesweeperEnabled(minesweeper);

        // Bridge-Layer (AVLB) deployment is entity-scoped: it depends on the selected unit (vehicle or quad Mek).
        boolean canDeployBridge = (entity != null) && BridgeLayerLogic.canDeclareBridgeDeploy(entity, game);
        setDeployBridgeEnabled(canDeployBridge);
        if (entity != null) {
            // Show the button on its first selectable bridge from the start (e.g. "Deploy Right Bridge" on a unit
            // with two bridges) rather than a generic label.
            updateDeployBridgeButtonLabel(entity, BridgeLayerLogic.getDeployableBridgeLayers(entity));
        }

        // Initiate Infantry Combat is entity-scoped: it depends on the selected unit and target building.
        boolean canInitiate = (entity instanceof Infantry infantry)
              && infantry.canInitiateInfantryVsInfantryCombat()
              && target != null
              && isValidBuildingTargetNoCombat(entity, target);
        setInitiateInfantryCombatEnabled(canInitiate);
        updateDonePanel();

        LOGGER.debug("[PreEnd] updateButtons: currentEntity={}, infantryCombat={}, deployBridge={}, nova={}, vrt={}, "
                    + "abandon={}, charges={}, minesweeper={}, declarationMade={}, butDoneEnabled={}, butSkipEnabled={}",
              currentEntity, canInitiate, canDeployBridge, nova, variableRange, abandon, charges, minesweeper,
              declarationMade, butDone.isEnabled(), butSkipTurn.isEnabled());
    }

    @Override
    protected void updateDonePanel() {
        // A player-wide declaration sends its data directly and never queues an attack, so also treat a made
        // declaration as "acted". Without this the button stays on "Skip Turn" and reads as if nothing happened.
        boolean acted = !attacks.isEmpty() || declarationMade;
        updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), acted,
              acted ? attacks.getDescriptions() : null);
    }

    /**
     * Records that the local player applied a player-wide declaration this turn and shows a confirmation toast. Flips
     * the end-turn button to "Done" so the player can see the declaration registered.
     *
     * @param confirmationKey i18n key for the confirmation toast text
     */
    private void registerDeclaration(String confirmationKey) {
        clientgui.addToast(ToastLevel.SUCCESS, Messages.getString(confirmationKey));
        registerDeclaration();
    }

    /**
     * Records that the local player applied a player-wide declaration this turn, without its own toast (used where the
     * dialog already shows one, e.g. Detonate Charges). Flips the end-turn button to "Done".
     */
    private void registerDeclaration() {
        declarationMade = true;
        LOGGER.debug("[PreEnd] declaration registered for {}", currentEntity);
        updateButtons();
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
     * Shows the Nova CEWS network management dialog (IO:AE p.60).
     */
    private void showNovaNetworkDialog() {
        NovaNetworkDialog dialog = new NovaNetworkDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
        if (dialog.wasApplied()) {
            registerDeclaration("PreEndDeclarationsDisplay.declared.novaNetwork");
        }
    }

    /**
     * The local player's id, or {@link Player#PLAYER_NONE} when there is no local player (e.g. an observer or during
     * early initialization). PLAYER_NONE matches no owned unit, so the player-wide checks below return false.
     */
    private int localPlayerId() {
        Player localPlayer = clientgui.getClient().getLocalPlayer();
        return (localPlayer == null) ? Player.PLAYER_NONE : localPlayer.getId();
    }

    /**
     * Checks if the local player has any Nova CEWS units.
     */
    private boolean hasNovaUnits() {
        int localPlayerId = localPlayerId();
        return game.getEntitiesVector().stream()
              .filter(entity -> entity.getOwnerId() == localPlayerId)
              .anyMatch(Entity::hasNovaCEWS);
    }

    private void setNovaNetworkEnabled(boolean enabled) {
        buttons.get(PreEndCommand.PREEND_NOVA_NETWORK).setEnabled(enabled);
    }

    /**
     * Shows the Variable Range Targeting mode selection dialog (BMM pg. 86).
     */
    private void showVariableRangeTargetingDialog() {
        VariableRangeTargetingDialog dialog = new VariableRangeTargetingDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
        if (dialog.wasApplied()) {
            registerDeclaration("PreEndDeclarationsDisplay.declared.varRangeTargeting");
        }
        buttons.get(PreEndCommand.PREEND_VAR_RANGE_TARGETING).transferFocus();
    }

    /**
     * Checks if the local player has any units with the Variable Range Targeting quirk.
     */
    private boolean hasVariableRangeUnits() {
        int localPlayerId = localPlayerId();
        return game.getEntitiesVector().stream()
              .filter(entity -> entity.getOwnerId() == localPlayerId)
              .anyMatch(Entity::hasVariableRangeTargeting);
    }

    private void setVariableRangeTargetingEnabled(boolean enabled) {
        buttons.get(PreEndCommand.PREEND_VAR_RANGE_TARGETING).setEnabled(enabled);
    }

    /**
     * Shows the Unit Abandonment dialog (TO:AR p.165: announce abandonment in the End Phase).
     */
    private void showAbandonDialog() {
        AbandonUnitDialog dialog = new AbandonUnitDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
        if (dialog.wasApplied()) {
            registerDeclaration("PreEndDeclarationsDisplay.declared.abandon");
        }
        buttons.get(PreEndCommand.PREEND_ABANDON).transferFocus();
    }

    /**
     * Checks if the local player has any units that can announce crew abandonment (Meks: prone+shutdown, vehicles: any,
     * escape pods: crew can exit).
     */
    private boolean hasAbandonableUnits() {
        int localPlayerId = localPlayerId();
        return game.getEntitiesVector().stream()
              .filter(entity -> entity.getOwnerId() == localPlayerId)
              .anyMatch(Entity::canAnnounceAbandon);
    }

    private void setAbandonEnabled(boolean enabled) {
        buttons.get(PreEndCommand.PREEND_ABANDON).setEnabled(enabled);
    }

    /**
     * Shows the Detonate Charges dialog (TO:AUE p.152: detonation of finished demolition charges is announced in any
     * End Phase after the charges were set).
     */
    private void showDetonateChargesDialog() {
        DetonateChargesDialog dialog = new DetonateChargesDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
        if (dialog.wasApplied()) {
            // The dialog already shows its own "charges announced" toast, so flip the button without a second toast.
            registerDeclaration();
        }
        buttons.get(PreEndCommand.PREEND_DETONATE_CHARGES).transferFocus();
    }

    /**
     * Checks if the local player has any demolition charges set on any building.
     */
    private boolean hasDemolitionCharges() {
        int localPlayerId = localPlayerId();
        return game.getBoards().values().stream()
              .flatMap(board -> board.getBuildingsVector().stream())
              .flatMap(building -> building.getDemolitionCharges().stream())
              .anyMatch(charge -> charge.playerId == localPlayerId);
    }

    private void setDetonateChargesEnabled(boolean enabled) {
        buttons.get(PreEndCommand.PREEND_DETONATE_CHARGES).setEnabled(enabled);
    }

    /**
     * Shows the Minesweeper activation dialog (TO:AUE p.138: the sweeper is activated or deactivated in the End Phase,
     * taking effect next turn).
     */
    private void showMinesweeperDialog() {
        MinesweeperActivationDialog dialog = new MinesweeperActivationDialog(clientgui.getFrame(), clientgui);
        dialog.setVisible(true);
        if (dialog.wasApplied()) {
            registerDeclaration("PreEndDeclarationsDisplay.declared.minesweeper");
        }
        buttons.get(PreEndCommand.PREEND_MINESWEEPER).transferFocus();
    }

    /**
     * Checks if the local player has any units mounting a minesweeper.
     */
    private boolean hasMinesweeperUnits() {
        int localPlayerId = localPlayerId();
        return game.getEntitiesVector().stream()
              .filter(entity -> entity.getOwnerId() == localPlayerId)
              .anyMatch(Entity::hasMinesweeper);
    }

    private void setMinesweeperEnabled(boolean enabled) {
        buttons.get(PreEndCommand.PREEND_MINESWEEPER).setEnabled(enabled);
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
        clientgui.getUnitDisplay().displayEntity(selected);
        clientgui.getBoardView().highlight(selected.getPosition());
        clientgui.getBoardView().centerOnHex(selected.getPosition());

        updateButtons();
    }

    /**
     * Called when the player's turn begins
     */
    private void beginMyTurn() {
        declarationMade = false;
        LOGGER.debug("[PreEnd] pre-end declarations turn begins for the local player");
        clientgui.maybeShowUnitDisplay();
        setTarget(null);
        selectingDeployBridgeHex = false;

        // Auto-select first entity
        if (GUIP.getAutoSelectNextUnit()) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
        }

        // Let updateButtons() -> updateDonePanel() decide the Done/Skip state. Do NOT force butDone disabled here:
        // with "nag for no action" off the Done button doubles as Skip Turn, so disabling it would strand a player who
        // has nothing to declare (e.g. a unit eligible only for a player-wide declaration) with no way to end the turn.
        updateButtons();
        startTimer();
        LOGGER.debug("[PreEnd] beginMyTurn complete: currentEntity={}, butDoneEnabled={}, butSkipEnabled={}",
              currentEntity, butDone.isEnabled(), butSkipTurn.isEnabled());
    }

    /**
     * Called when the player's turn ends
     */
    private void endMyTurn() {
        declarationMade = false;
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
        setNovaNetworkEnabled(false);
        setVariableRangeTargetingEnabled(false);
        setAbandonEnabled(false);
        setDetonateChargesEnabled(false);
        setMinesweeperEnabled(false);
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

        boolean myTurn = clientgui.getClient().isMyTurn();
        boolean simultaneous = game.getPhase().isSimultaneous(game);

        if (!simultaneous) {
            if (myTurn) {
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
