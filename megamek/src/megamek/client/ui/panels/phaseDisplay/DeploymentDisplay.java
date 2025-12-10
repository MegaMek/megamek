/*
 * Copyright (C) 2000-2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.CollapseWarning;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.dialogs.ConfirmDialog;
import megamek.client.ui.dialogs.phaseDisplay.DeployElevationChoiceDialog;
import megamek.client.ui.dialogs.phaseDisplay.EntityChoiceDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.util.CommandAction;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.MekPanelTabStrip;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.ProtoMekClampMount;
import megamek.common.bays.Bay;
import megamek.common.board.AllowedDeploymentHelper;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.DeploymentElevationType;
import megamek.common.board.ElevationOption;
import megamek.common.equipment.Transporter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;

import static megamek.common.bays.Bay.UNSET_BAY;

public class DeploymentDisplay extends StatusBarPhaseDisplay {
    private final static MMLogger logger = MMLogger.create(DeploymentDisplay.class);

    /**
     * This enumeration lists all the possible ActionCommands that can be carried out during the deployment phase. Each
     * command has a string for the command plus a flag that determines what unit type it is appropriate for.
     *
     * @author arlith
     */
    public enum DeployCommand implements PhaseCommand {
        DEPLOY_NEXT("deployNext"),
        DEPLOY_TURN("deployTurn"),
        DEPLOY_LOAD("deployLoad"),
        DEPLOY_UNLOAD("deployUnload"),
        DEPLOY_REMOVE("deployRemove"),
        DEPLOY_ASSAULT_DROP("assaultDrop"),
        DEPLOY_DOCK("deployDock");

        public final String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        DeployCommand(String c) {
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
            return Messages.getString("DeploymentDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            String result = "";

            String msg_next = Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");

            if (this == DeployCommand.DEPLOY_NEXT) {
                result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
            }

            if (this == DeployCommand.DEPLOY_TURN) {
                result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.DEPLOY_TURN_UNIT);
            }

            return result;
        }
    }

    protected Map<DeployCommand, MegaMekButton> buttons;

    private int cen = Entity.NONE; // current entity number
    // is the shift key held?
    private boolean turnMode = false;
    private boolean assaultDropPreference = false;
    private final Set<ElevationOption> lastHexDeploymentOptions = new HashSet<>();
    private ElevationOption lastDeploymentOption = null;

    private final ClientGUI clientgui;
    private final Game game;

    /**
     * Creates and lays out a new deployment phase display for the specified client.
     */
    public DeploymentDisplay(ClientGUI clientgui) {
        super(clientgui);
        this.clientgui = clientgui;
        game = clientgui.getClient().getGame();
        game.addGameListener(this);
        setupStatusBar(Messages.getString("DeploymentDisplay.waitingForDeploymentPhase"));

        setButtons();
        setButtonsTooltips();

        butDone.setText("<html><body>" + Messages.getString("DeploymentDisplay.Deploy") + "</body></html>");
        butDone.setEnabled(false);

        setupButtonPanel();
        registerKeyCommands();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (DeployCommand.values().length * 1.25 + 0.5));
        for (DeployCommand cmd : DeployCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "DeploymentDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (DeployCommand cmd : DeployCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), "DeploymentDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
        DeployCommand[] commands = DeployCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (DeployCommand cmd : commands) {
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /**
     * Register all of the {@link CommandAction}s for this panel display.
     */
    private void registerKeyCommands() {
        if ((clientgui == null) || (clientgui.controller == null)) {
            return;
        }

        MegaMekController controller = clientgui.controller;
        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT, this, this::nextUnit);
        controller.registerCommandAction(KeyCommandBind.DEPLOY_TURN_UNIT, this, this::turnUnit);
    }

    private void nextUnit() {
        buttons.get(DeployCommand.DEPLOY_NEXT).doClick();
    }

    private void turnUnit() {
        buttons.get(DeployCommand.DEPLOY_TURN).doClick();
    }

    /** Selects an entity for deployment. */
    public void selectEntity(int en) {
        lastHexDeploymentOptions.clear();
        lastDeploymentOption = null;

        // hmm, sometimes this gets called when there's no ready entities?
        Entity entity = game.getEntity(en);
        if (entity == null) {
            disableButtons();
            setNextEnabled(true);
            clientgui.clearFieldOfFire();
            clientgui.clearTemporarySprites();
            logger.error("DeploymentDisplay: Tried to select non-existent entity: {}", en);
            return;
        }

        if (entity.isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(entity);
        }

        // FIXME: Hack alert: remove C3 sprites from earlier here, or we might crash
        // when
        // trying to draw a c3 sprite belonging to the previously selected,
        // but not deployed entity. BoardView1 should take care of that itself.
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).clearC3Networks());
        cen = en;
        clientgui.setSelectedEntityNum(en);
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        setTurnEnabled(true);
        butDone.setEnabled(false);
        markDeploymentHexes(entity);
        // set facing according to starting position
        switch (entity.getStartingPos()) {
            case Board.START_W:
            case Board.START_SW:
                entity.setFacing(1);
                entity.setSecondaryFacing(1);
                break;
            case Board.START_SE:
            case Board.START_E:
                entity.setFacing(5);
                entity.setSecondaryFacing(5);
                break;
            case Board.START_NE:
                entity.setFacing(4);
                entity.setSecondaryFacing(4);
                break;
            case Board.START_N:
                entity.setFacing(3);
                entity.setSecondaryFacing(3);
                break;
            case Board.START_NW:
                entity.setFacing(2);
                entity.setSecondaryFacing(2);
                break;
            default:
                entity.setFacing(0);
                entity.setSecondaryFacing(0);
                break;
        }
        boolean assaultDropOption = game.getOptions().booleanOption(OptionsConstants.ADVANCED_ASSAULT_DROP);
        setAssaultDropEnabled(entity.canAssaultDrop() && assaultDropOption);
        if (!entity.canAssaultDrop() && assaultDropOption) {
            buttons.get(DeployCommand.DEPLOY_ASSAULT_DROP)
                  .setText(Messages.getString("DeploymentDisplay.AssaultDrop"));
            assaultDropPreference = false;
        }

        setLoadEnabled(!getLoadableEntities().isEmpty());
        setUnloadEnabled(!entity.getLoadedUnits().isEmpty());

        setNextEnabled(true);
        setRemoveEnabled(true);

        clientgui.getUnitDisplay().displayEntity(entity);
        clientgui.getUnitDisplay().showPanel(MekPanelTabStrip.SUMMARY);
        clientgui.updateFiringArc(entity);
        clientgui.showSensorRanges(entity);
        computeWarningHexes(entity);
    }

    private void computeWarningHexes(Entity ce) {
        Set<BoardLocation> warnList = new HashSet<>(CollapseWarning.findCFWarningsDeployment(game, ce));
        warnList.addAll(TowLinkWarning.findTowLinkIssues(game, ce));
        clientgui.showCollapseWarning(warnList);
    }

    /** Enables relevant buttons and sets up for the local player's turn. */
    private void beginMyTurn() {
        clientgui.maybeShowUnitDisplay();
        selectEntity(clientgui.getClient().getFirstDeployableEntityNum());
        setNextEnabled(true);
        setRemoveEnabled(true);
    }

    /** Clears out old deployment data and disables relevant buttons. */
    private void endMyTurn() {
        Entity next = game.getNextEntity(game.getTurnIndex());
        if (game.getPhase().isDeployment() &&
              (null != next) &&
              (null != currentEntity()) &&
              (next.getOwnerId() != currentEntity().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }
        cen = Entity.NONE;
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        hideDeploymentHexes();
        clientgui.setSelectedEntityNum(Entity.NONE);
        clientgui.clearTemporarySprites();
        disableButtons();
    }

    /** Disables all buttons in the interface. */
    private void disableButtons() {
        for (DeployCommand cmd : DeployCommand.values()) {
            setButtonEnabled(cmd, false);
        }
        butDone.setEnabled(false);
        setLoadEnabled(false);
        setUnloadEnabled(false);
        setAssaultDropEnabled(false);
    }

    private void setButtonEnabled(DeployCommand cmd, boolean enabled) {
        MegaMekButton button = buttons.get(cmd);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    /**
     * Notify the player that the planned deployment is not possible if veteran circumstances are met. Returns true in
     * that case (cancel deployment), false if deployment can proceed.
     *
     * @return True to cancel deployment, false to proceed.
     */
    private boolean checkNags() {
        Entity entity = currentEntity();
        if (entity == null || entity.getPosition() == null) {
            return true;
        }
        if ((entity instanceof Dropship) && !entity.isAirborne()) {
            List<Coords> allPositions = new ArrayList<>();
            allPositions.add(entity.getPosition());
            allPositions.addAll(entity.getPosition().allAdjacent());
            boolean crushesBuildingHex = allPositions.stream()
                  .anyMatch(c -> game.getBoard(entity.getBoardId()).getBuildingAt(c) != null);
            if (crushesBuildingHex) {
                String title = Messages.getString("DeploymentDisplay.alertDialog.title");
                String body = Messages.getString("DeploymentDisplay.dropshipBuildingDeploy");
                clientgui.doAlertDialog(title, body);
                return true;
            }
        }

        // Check nag for doomed planetary conditions
        if (GUIP.getNagForDoomed()) {
            String reason = game.getPlanetaryConditions().whyDoomed(entity, game);
            if (reason != null) {
                String title = Messages.getString("DeploymentDisplay.ConfirmDoomed.title");
                String body = Messages.getString("DeploymentDisplay.ConfirmDoomed.message", reason);
                ConfirmDialog nag = clientgui.doYesNoBotherDialog(title, body);
                if (nag.getAnswer()) {
                    // do they want to be bothered again?
                    if (!nag.getShowAgain()) {
                        GUIP.setNagForDoomed(false);
                    }
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void ready() {
        final Entity entity = currentEntity();
        if (checkNags()) {
            return;
        }
        disableButtons();

        int elevationOrAltitude = entity.isAero() ? entity.getAltitude() : entity.getElevation();
        clientgui.getClient().deploy(entity.getId(), entity.getPosition(), entity.getBoardId(), entity.getFacing(),
              elevationOrAltitude, entity.getLoadedUnits(), assaultDropPreference);
        entity.setDeployed(true);

        if (entity.isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(entity);
        }
        endMyTurn();
    }

    /** Sends an entity removal to the server. */
    private void remove() {
        disableButtons();
        clientgui.getClient().sendDeleteEntity(cen);
        // Also remove units that are carried by the present unit

        Entity entity = game.getNextEntity(cen);

        if (entity != null) {
            for (Entity carried : entity.getLoadedUnits()) {
                clientgui.getClient().sendDeleteEntity(carried.getId());
            }
        }

        cen = Entity.NONE;
    }

    /** Returns the current entity. */
    private Entity currentEntity() {
        return game.getEntity(cen);
    }

    public void die() {
        if (clientgui.getClient().isMyTurn()) {
            endMyTurn();
        }
        hideDeploymentHexes();
        game.removeGameListener(this);
        clientgui.boardViews().forEach(bv -> bv.removeBoardViewListener(this));
        removeAll();
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (isIgnoringEvents() || !game.getPhase().isDeployment()) {
            return;
        }

        // On simultaneous phases, each player ending their turn will generate a turn change
        // We want to ignore turns from other players and only listen to events we generated
        // Except on the first turn
        if (game.getPhase().isSimultaneous(game) &&
              (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber()) &&
              (game.getTurnIndex() != 0)) {
            return;
        }

        String s = getRemainingPlayerWithTurns();

        if (clientgui.getClient().isMyTurn()) {
            if (cen == Entity.NONE) {
                beginMyTurn();
                clientgui.bingMyTurn();
            }
            setStatusBarText(Messages.getString("DeploymentDisplay.its_your_turn") + s);
        } else {
            endMyTurn();
            String playerName;

            if (e.getPlayer() != null) {
                playerName = e.getPlayer().getName();
            } else {
                playerName = "Unknown";
            }

            setStatusBarText(Messages.getString("DeploymentDisplay.its_others_turn", playerName) + s);
            clientgui.bingOthersTurn();
        }

    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        hideDeploymentHexes();

        // In case of a /reset command, ensure the state gets reset
        if (game.getPhase().isLounge()) {
            endMyTurn();
        }
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (game.getPhase().isDeployment()) {
            setStatusBarText(Messages.getString("DeploymentDisplay.waitingForDeploymentPhase"));
        }
    }

    private void hideDeploymentHexes() {
        markDeploymentHexes(null);
    }

    private void markDeploymentHexes(@Nullable Entity entity) {
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).markDeploymentHexesFor(entity));
    }

    //
    // BoardViewListener
    //
    @Override
    public void hexMoused(BoardViewEvent b) {
        Coords coords = b.getCoords();
        Entity entity = currentEntity();
        if (isIgnoringEvents() ||
              !game.hasBoardLocation(coords, b.getBoardId()) ||
              (entity == null) ||
              !clientgui.getClient().isMyTurn() ||
              (b.getType() != BoardViewEvent.BOARD_HEX_DRAGGED) ||
              (b.getButton() != MouseEvent.BUTTON1) ||
              ((b.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0) ||
              ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0)) {
            return;
        }

        try {
            // Tooltips go above even modal dialogs; therefore hide them; try block makes sure they get re-enabled
            ToolTipManager.sharedInstance().setEnabled(false);

            boolean shiftHeld = (b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
            Board board = game.getBoard(b.getBoardId());
            int previousBoardId = entity.getBoardId();
            // use turn mode only when the unit is already on that same board
            if ((entity.getPosition() != null) && (b.getBoardId() == previousBoardId) && (shiftHeld || turnMode)) {
                processTurn(entity, coords);
                return;
            } else if (entity.isBoardProhibited(board)) {
                showWrongBoardTypeMessage(board);
                return;
            } else if (!(board.isLegalDeployment(coords, entity) || assaultDropPreference)) {
                showOutsideDeployAreaMessage();
                return;
            }

            if (!board.isSpace()) {
                int finalElevation;
                var deploymentHelper = new AllowedDeploymentHelper(entity, coords, board,
                      board.getHex(coords), game);
                List<ElevationOption> elevationOptions = deploymentHelper.findAllowedElevations();

                if (elevationOptions.isEmpty()) {
                    showCannotDeployHereMessage(coords);
                    return;
                } else if (elevationOptions.size() == 1) {
                    finalElevation = elevationOptions.get(0).elevation();
                    lastHexDeploymentOptions.clear();
                    lastHexDeploymentOptions.addAll(elevationOptions);
                    lastDeploymentOption = elevationOptions.get(0);
                } else if (useLastDeployElevation(elevationOptions) && !coords.equals(entity.getPosition())) {
                    // When the player clicks the same hex again, always ask for the elevation
                    finalElevation = entity.isAero() ? entity.getAltitude() : entity.getElevation();
                } else {
                    ElevationOption elevationOption = showElevationChoiceDialog(elevationOptions);
                    if (elevationOption != null) {
                        lastHexDeploymentOptions.clear();
                        lastHexDeploymentOptions.addAll(elevationOptions);
                        lastDeploymentOption = elevationOption;
                        finalElevation = elevationOption.elevation();
                    } else {
                        return;
                    }
                }

                // entity.isAero will check if a unit is a LAM in Fighter mode
                if ((entity instanceof IAero aero) && (entity.isAero())) {
                    entity.setAltitude(finalElevation);
                    if (finalElevation == 0) {
                        aero.land();
                    } else {
                        aero.liftOff(finalElevation);
                    }
                } else {
                    entity.setElevation(finalElevation);
                }
            }
            entity.setPosition(coords);
            entity.setBoardId(b.getBoardId());
            clientgui.boardViews().forEach(bv -> ((BoardView) bv).redrawAllEntities());
            clientgui.updateFiringArc(entity);
            clientgui.showSensorRanges(entity);
            clientgui.boardViews().forEach(IBoardView::repaint);
            butDone.setEnabled(true);
            if (!shiftHeld) {
                clientgui.boardViews().forEach(bv -> bv.select(null));
                clientgui.getBoardView(entity).select(coords);
            }
        } finally {
            ToolTipManager.sharedInstance().setEnabled(true);
        }
    }

    private @Nullable ElevationOption showElevationChoiceDialog(List<ElevationOption> elevationOptions) {
        var dlg = new DeployElevationChoiceDialog(clientgui.getFrame(), elevationOptions);
        DialogResult result = dlg.showDialog();
        if ((result == DialogResult.CONFIRMED) && (dlg.getFirstChoice() != null)) {
            if (dlg.getFirstChoice().type() == DeploymentElevationType.ELEVATIONS_ABOVE) {
                int elevation = showHighElevationChoiceDialog();
                return (elevation == -1) ?
                      null :
                      new ElevationOption(elevation, DeploymentElevationType.ELEVATIONS_ABOVE);
            } else {
                return dlg.getFirstChoice();
            }
        } else {
            return null;
        }
    }

    private int showHighElevationChoiceDialog() {
        String msg = Messages.getString("DeploymentDisplay.elevationChoice");
        String input = JOptionPane.showInputDialog(clientgui.getFrame(), msg);
        try {
            return Integer.parseInt(input);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * @return True when the last chosen elevation can be re-used without asking again. This is true when the options
     *       for the current hex have no option that the previous hex didn't and the previous deployment option is
     *       available in the new hex.
     */
    private boolean useLastDeployElevation(List<ElevationOption> currentOptions) {
        return ((lastDeploymentOption != null) &&
              (lastDeploymentOption.type() == DeploymentElevationType.ELEVATIONS_ABOVE) &&
              isHighElevationAvailable(currentOptions, lastDeploymentOption.elevation())) ||
              ((currentOptions.size() <= lastHexDeploymentOptions.size()) &&
                    lastHexDeploymentOptions.containsAll(currentOptions) &&
                    currentOptions.contains(lastDeploymentOption));
    }

    private boolean isHighElevationAvailable(List<ElevationOption> currentOptions, int elevation) {
        return currentOptions.stream()
              .filter(o -> o.type() == DeploymentElevationType.ELEVATIONS_ABOVE)
              .anyMatch(o -> o.elevation() <= elevation);
    }

    private void showWrongBoardTypeMessage(Board board) {
        String title = Messages.getString("DeploymentDisplay.alertDialog.title");
        String boardType = switch (board.getBoardType()) {
            case CAPITAL_RADAR, RADAR -> "Radar";
            case SKY, SKY_WITH_TERRAIN -> "Atmospheric";
            case FAR_SPACE, NEAR_SPACE -> "Space";
            case GROUND -> "Ground";
        };
        String msg = Messages.getString("DeploymentDisplay.wrongMapType", currentEntity().getShortName(), boardType);
        JOptionPane.showMessageDialog(clientgui.getFrame(), msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showOutsideDeployAreaMessage() {
        String msg = Messages.getString("DeploymentDisplay.outsideDeployArea");
        String title = Messages.getString("DeploymentDisplay.alertDialog.title");
        JOptionPane.showMessageDialog(clientgui.getFrame(), msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showCannotDeployHereMessage(Coords coords) {
        String msg = Messages.getString("DeploymentDisplay.cantDeployInto", currentEntity().getShortName(), coords.getBoardNum());
        String title = Messages.getString("DeploymentDisplay.alertDialog.title");
        JOptionPane.showMessageDialog(clientgui.getFrame(), msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void processTurn(Entity entity, Coords coords) {
        entity.setFacing(entity.getPosition().direction(coords));
        entity.setSecondaryFacing(entity.getFacing());
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).redrawEntity(entity));
        clientgui.updateFiringArc(entity);
        clientgui.showSensorRanges(entity);
        turnMode = false;
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent evt) {
        final Client client = clientgui.getClient();
        final String actionCmd = evt.getActionCommand();
        if (isIgnoringEvents() || !client.isMyTurn()) {
            return;
        }

        if (actionCmd.equals(DeployCommand.DEPLOY_NEXT.getCmd())) {
            if (currentEntity() != null) {
                currentEntity().setPosition(null);
                clientgui.boardViews().forEach(bv -> ((BoardView) bv).redrawEntity(currentEntity()));
                // Unload any loaded units during this turn
                List<Integer> lobbyLoadedUnits = currentEntity().getLoadedKeepers();
                for (Entity other : currentEntity().getLoadedUnits()) {
                    // Ignore units loaded before this turn
                    if (!lobbyLoadedUnits.contains(other.getId())) {
                        currentEntity().unload(other);
                        other.setTransportId(Entity.NONE);
                        other.newRound(client.getGame().getRoundCount());
                    }
                }
            }
            selectEntity(client.getNextDeployableEntityNum(cen));
        } else if (actionCmd.equals(DeployCommand.DEPLOY_TURN.getCmd())) {
            turnMode = true;
        } else if (actionCmd.equals(DeployCommand.DEPLOY_LOAD.getCmd())) {
            // What un-deployed units can we load?
            List<Entity> choices = getLoadableEntities();

            // Do we have anyone to load?
            if (!choices.isEmpty()) {
                // If we have multiple choices, display a selection dialog.
                Entity other = EntityChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                      "DeploymentDisplay.loadUnitDialog.title",
                      Messages.getString("DeploymentDisplay.loadUnitDialog.message",
                            currentEntity().getShortName(),
                            currentEntity().getUnusedString()),
                      choices);

                // Abort here if no Entity was generated
                if (other == null) {
                    return;
                }

                // Otherwise continue
                if (!(other instanceof Infantry)) {
                    List<Integer> bayChoices = new ArrayList<>();
                    for (Transporter t : currentEntity().getTransports()) {
                        if (t.canLoad(other) && (t instanceof Bay)) {
                            bayChoices.add(((Bay) t).getBayNumber());
                        }
                    }

                    if (bayChoices.size() > 1) {
                        String[] retVal = new String[bayChoices.size()];
                        int i = 0;
                        for (Integer bn : bayChoices) {
                            retVal[i++] = bn.toString() +
                                  " (Free Slots: " +
                                  (int) currentEntity().getBayById(bn).getUnused() +
                                  ")";
                        }
                        String title = Messages.getString("DeploymentDisplay.loadUnitBayNumberDialog.title");
                        String msg = Messages.getString("DeploymentDisplay.loadUnitBayNumberDialog.message",
                              currentEntity().getShortName());
                        String bayString = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                              msg,
                              title,
                              JOptionPane.QUESTION_MESSAGE,
                              null,
                              retVal,
                              null);

                        // No choice made? Bug out.
                        if (bayString == null) {
                            return;
                        }

                        int bayNum = Integer.parseInt(bayString.substring(0, bayString.indexOf(" ")));
                        other.setTargetBay(bayNum);
                        // We need to update the entity here so that the server knows about our target
                        // bay
                        client.sendUpdateEntity(other);
                    } else if (other.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
                        bayChoices = new ArrayList<>();
                        for (Transporter t : currentEntity().getTransports()) {
                            if ((t instanceof ProtoMekClampMount) && t.canLoad(other)) {
                                bayChoices.add(((ProtoMekClampMount) t).isRear() ? 1 : 0);
                            }
                        }
                        if (bayChoices.size() > 1) {
                            String[] retVal = new String[bayChoices.size()];
                            int i = 0;
                            for (Integer bn : bayChoices) {
                                retVal[i++] = bn > 0 ?
                                      Messages.getString("MovementDisplay.loadProtoClampMountDialog.rear") :
                                      Messages.getString("MovementDisplay.loadProtoClampMountDialog.front");
                            }
                            String bayString = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                                  Messages.getString("MovementDisplay.loadProtoClampMountDialog.message",
                                        currentEntity().getShortName()),
                                  Messages.getString("MovementDisplay.loadProtoClampMountDialog.title"),
                                  JOptionPane.QUESTION_MESSAGE,
                                  null,
                                  retVal,
                                  null);

                            // No choice made? Bug out.
                            if (bayString == null) {
                                return;
                            }

                            other.setTargetBay(bayString.equals(Messages.getString(
                                  "MovementDisplay.loadProtoClampMountDialog.front")) ? 0 : 1);
                            // We need to update the entity here so that the server knows about our target
                            // bay
                            clientgui.getClient().sendUpdateEntity(other);
                        } else {
                            other.setTargetBay(UNSET_BAY); // Safety set!
                        }
                    } else {
                        other.setTargetBay(UNSET_BAY); // Safety set!
                    }
                } else {
                    other.setTargetBay(UNSET_BAY); // Safety set!
                }

                // Please note, the Server may never get this load order.
                currentEntity().load(other, false, other.getTargetBay());
                other.setTransportId(cen);
                clientgui.getUnitDisplay().displayEntity(currentEntity());
                setUnloadEnabled(true);
            } else {
                JOptionPane.showMessageDialog(clientgui.getFrame(),
                      Messages.getString("DeploymentDisplay.alertDialog1.message", currentEntity().getShortName()),
                      Messages.getString("DeploymentDisplay.alertDialog1.title"),
                      JOptionPane.ERROR_MESSAGE);
            }
        } else if (actionCmd.equals(DeployCommand.DEPLOY_UNLOAD.getCmd())) {
            // Do we have anyone to unload?
            Entity loader = currentEntity();
            List<Entity> choices = loader.getLoadedUnits();
            if (!choices.isEmpty()) {

                Entity loaded = EntityChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                      "DeploymentDisplay.unloadUnitDialog.title",
                      Messages.getString("DeploymentDisplay.unloadUnitDialog.message",
                            currentEntity().getShortName(),
                            currentEntity().getUnusedString()),
                      choices);

                if (loaded != null) {
                    if (loader.unload(loaded)) {
                        loaded.setTransportId(Entity.NONE);
                        loaded.newRound(game.getRoundCount());
                        clientgui.getUnitDisplay().displayEntity(currentEntity());
                        // Unit loaded in the lobby? Server needs updating
                        if (loader.getLoadedKeepers().contains(loaded.getId())) {
                            Vector<Integer> lobbyLoaded = loader.getLoadedKeepers();
                            lobbyLoaded.removeElement(loaded.getId());
                            loader.setLoadedKeepers(lobbyLoaded);
                            client.sendDeploymentUnload(loader, loaded);
                            // Need to take turn for unloaded unit, so select it
                            selectEntity(loaded.getId());
                        }
                        setLoadEnabled(!getLoadableEntities().isEmpty());
                    } else {
                        logger.error("Could not unload {} from {}", loaded.getShortName(), currentEntity().getShortName());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(clientgui.getFrame(),
                      Messages.getString("DeploymentDisplay.alertDialog2.message", currentEntity().getShortName()),
                      Messages.getString("DeploymentDisplay.alertDialog2.title"),
                      JOptionPane.ERROR_MESSAGE);
            }
        } else if (actionCmd.equals(DeployCommand.DEPLOY_REMOVE.getCmd())) {
            if (JOptionPane.showConfirmDialog(clientgui.getFrame(),
                  Messages.getString("DeploymentDisplay.removeUnit", currentEntity().getShortName()),
                  Messages.getString("DeploymentDisplay.removeTitle"),
                  JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                remove();
            }
        } else if (actionCmd.equals(DeployCommand.DEPLOY_ASSAULT_DROP.getCmd())) {
            assaultDropPreference = !assaultDropPreference;
            if (assaultDropPreference) {
                buttons.get(DeployCommand.DEPLOY_ASSAULT_DROP)
                      .setText(Messages.getString("DeploymentDisplay.assaultDropOff"));
            } else {
                buttons.get(DeployCommand.DEPLOY_ASSAULT_DROP)
                      .setText(Messages.getString("DeploymentDisplay.assaultDrop"));
            }
        }
    }

    @Override
    public void clear() {
        beginMyTurn();
    }

    //
    // BoardViewListener
    //
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()) {
            clientgui.maybeShowUnitDisplay();
        }
    }

    // Selected a unit in the unit overview.
    @Override
    public void unitSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        final Client client = clientgui.getClient();
        final Entity e = client.getGame().getEntity(b.getEntityId());
        if (null == e) {
            return;
        }
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();
        if (client.isMyTurn()) {
            GameTurn currentTurn = client.getGame().getTurn();
            if (currentTurn != null) {
                if (currentTurn.isValidEntity(e, client.getGame())) {
                    if (currentEntity() != null) {
                        currentEntity().setPosition(null);
                        clientgui.boardViews().forEach(bv -> ((BoardView) bv).redrawEntity(currentEntity()));
                        // Unload any loaded units during this turn
                        List<Integer> lobbyLoadedUnits = currentEntity().getLoadedKeepers();
                        for (Entity other : currentEntity().getLoadedUnits()) {
                            // Ignore units loaded before this turn
                            if (!lobbyLoadedUnits.contains(other.getId())) {
                                currentEntity().unload(other);
                                other.setTransportId(Entity.NONE);
                                other.newRound(client.getGame().getRoundCount());
                            }
                        }
                    }
                    selectEntity(e.getId());
                    if (game.hasBoardLocation(e.getPosition(), e.getBoardId())) {
                        clientgui.getBoardView(e).centerOnHex(e.getPosition());
                    }
                }
            }
        } else {
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(e);
            if (game.hasBoardLocation(e.getPosition(), e.getBoardId())) {
                clientgui.getBoardView(e).centerOnHex(e.getPosition());
            }
        }
    }

    private void setNextEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_NEXT.getCmd(), enabled);
    }

    private void setTurnEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_TURN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_TURN.getCmd(), enabled);
    }

    private void setLoadEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_LOAD).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_LOAD.getCmd(), enabled);
    }

    private void setUnloadEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_UNLOAD).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_UNLOAD.getCmd(), enabled);
    }

    private void setRemoveEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_REMOVE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_REMOVE.getCmd(), enabled);
    }

    private void setAssaultDropEnabled(boolean enabled) {
        buttons.get(DeployCommand.DEPLOY_ASSAULT_DROP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(DeployCommand.DEPLOY_ASSAULT_DROP.getCmd(), enabled);
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        die();
    }

    /**
     * Returns a list of the entities that can be loaded into the currently selected entity.
     */
    private List<Entity> getLoadableEntities() {
        ArrayList<Entity> choices = new ArrayList<>();
        // If current entity is null, nothing to do
        if (currentEntity() == null) {
            return choices;
        }
        List<Entity> entities = game.getEntitiesVector();
        for (Entity other : entities) {
            if (other.isSelectableThisTurn() && currentEntity().canLoad(other, false)
                  // We can't depend on the transport id to be set because we sent a server update
                  // before loading on the client side, and the loaded unit may have been reset
                  // by the resulting update from the server.
                  && !currentEntity().getLoadedUnits().contains(other)
                  // If you want to load a trailer into a DropShip or large support vehicle, do it
                  // in the lobby
                  // The 'load' button should not allow trailers - that's what 'tow' is for.
                  && !other.isTrailer()) {
                choices.add(other);
            }
        }
        return choices;
    }
}
