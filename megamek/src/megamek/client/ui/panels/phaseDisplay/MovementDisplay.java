/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
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

import static megamek.common.LandingDirection.HORIZONTAL;
import static megamek.common.LandingDirection.VERTICAL;
import static megamek.common.bays.Bay.UNSET_BAY;
import static megamek.common.equipment.MiscType.F_CHAFF_POD;
import static megamek.common.options.OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_ZIPLINES;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.CollapseWarning;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.overlay.AbstractBoardViewOverlay;
import megamek.client.ui.clientGUI.boardview.sprite.FlyOverSprite;
import megamek.client.ui.dialogs.ChoiceDialog;
import megamek.client.ui.dialogs.ConfirmDialog;
import megamek.client.ui.dialogs.phaseDisplay.BombPayloadDialog;
import megamek.client.ui.dialogs.phaseDisplay.FlightPathNotice;
import megamek.client.ui.dialogs.phaseDisplay.LandingConfirmation;
import megamek.client.ui.dialogs.phaseDisplay.LandingHexNotice;
import megamek.client.ui.dialogs.phaseDisplay.ManeuverChoiceDialog;
import megamek.client.ui.dialogs.phaseDisplay.MineLayingDialog;
import megamek.client.ui.dialogs.phaseDisplay.TargetChoiceDialog;
import megamek.client.ui.dialogs.phaseDisplay.VibrabombSettingDialog;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import megamek.client.ui.util.CommandAction;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.MekPanelTabStrip;
import megamek.codeUtilities.MathUtility;
import megamek.common.AtmosphericLandingMovePath;
import megamek.common.Hex;
import megamek.common.LandingDirection;
import megamek.common.ManeuverType;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.AirMekRamAttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.RamAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.battleArmor.ProtoMekClampMount;
import megamek.common.bays.BattleArmorBay;
import megamek.common.bays.Bay;
import megamek.common.bays.CargoBay;
import megamek.common.bays.InfantryBay;
import megamek.common.board.Board;
import megamek.common.board.BoardHelper;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.ExternalCargo;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.TankTrailerHitch;
import megamek.common.equipment.Transporter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.GameTurn;
import megamek.common.game.IGame;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.GameOptions;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.LongestPathFinder;
import megamek.common.pathfinder.ShortestPathFinder;
import megamek.common.pathfinder.StopConditionTimeout;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.turns.UnloadStrandedTurn;
import megamek.common.units.*;
import megamek.common.weapons.TeleMissile;
import megamek.logging.MMLogger;

public class MovementDisplay extends ActionPhaseDisplay {
    private static final MMLogger LOGGER = MMLogger.create(MovementDisplay.class);

    @Serial
    private static final long serialVersionUID = -7246715124042905688L;

    // Defines for the different flags
    public static final int CMD_NONE = 0;
    public static final int CMD_MEK = 1;
    public static final int CMD_TANK = 1 << 1;
    public static final int CMD_VTOL = 1 << 2;
    public static final int CMD_INF = 1 << 3;
    public static final int CMD_AERO = 1 << 4;
    public static final int CMD_AERO_VECTORED = 1 << 5;
    public static final int CMD_CONVERTER = 1 << 6;
    public static final int CMD_AIR_MEK = 1 << 7;

    // Command used only in menus and has no associated button
    public static final int CMD_NO_BUTTON = 1 << 8;
    public static final int CMD_PROTOMEK = 1 << 9;

    // Convenience defines for common combinations
    public static final int CMD_AERO_BOTH = CMD_AERO | CMD_AERO_VECTORED;
    public static final int CMD_GROUND = CMD_MEK | CMD_TANK | CMD_VTOL | CMD_INF | CMD_PROTOMEK;
    public static final int CMD_NON_VECTORED = CMD_MEK | CMD_TANK | CMD_VTOL | CMD_INF | CMD_AERO | CMD_PROTOMEK;
    public static final int CMD_ALL = CMD_MEK |
          CMD_TANK |
          CMD_VTOL |
          CMD_INF |
          CMD_AERO |
          CMD_AERO_VECTORED |
          CMD_PROTOMEK;
    public static final int CMD_NON_INF = CMD_MEK | CMD_TANK | CMD_VTOL | CMD_AERO | CMD_AERO_VECTORED | CMD_PROTOMEK;

    public static int NO_UNIT_SELECTED = -1;

    private boolean isUnJammingRAC;
    private boolean isUsingChaff;

    // buttons
    private Map<MoveCommand, MegaMekButton> buttons;

    // let's keep track of what we're moving, too
    // considering movement data
    private MovePath cmd;

    // what "gear" is our mek in?
    private int gear;
    private int jumpSubGear;

    /**
     * Used to position a ground map flight path of an aero on an atmospheric map
     */
    private Coords flightPathPosition;

    /**
     * The ground map flight path of an aero on an atmospheric map
     */
    private FlyOverSprite flightPath;

    // is the shift key held?
    private boolean shiftHeld;

    private List<Entity> unloadableUnits = null;

    /**
     * A local copy of the current entity's towed trailers.
     */
    private List<Entity> towedUnits = null;

    /**
     * A dialog asking if a planned landing should be performed, ending the movement phase for this unit immediately.
     */
    LandingConfirmation landingConfirmation = new LandingConfirmation(clientgui);

    public static final int GEAR_LAND = 0;
    public static final int GEAR_BACKUP = 1;
    public static final int GEAR_JUMP = 2;
    public static final int GEAR_CHARGE = 3;
    public static final int GEAR_DFA = 4;
    public static final int GEAR_TURN = 5;
    public static final int GEAR_SWIM = 6;
    public static final int GEAR_RAM = 7;
    public static final int GEAR_IM_MEL = 8;
    public static final int GEAR_SPLIT_S = 9;
    public static final int GEAR_LONGEST_RUN = 10;
    public static final int GEAR_LONGEST_WALK = 11;
    public static final int GEAR_STRAFE = 12;
    /** Used to designate a ground map flight path for an aero on an atmospheric map. */
    public static final int GEAR_FLIGHTPATH = 13;
    /** Used to choose a ground map hex for landing an aero from an atmospheric map (aero-on-ground move off). */
    public static final int GEAR_LANDING_AERO = 14;
    public static final int GEAR_SUB_STANDARD = 0;
    public static final int GEAR_SUB_MEK_BOOSTERS = 2;

    public static final String turnDetailsFormat = "%s%-3s %-14s %1s %2dMP%s";

    /**
     * Creates and lays out a new movement phase display for the specified clientGUI.getClient().
     */
    public MovementDisplay(final ClientGUI clientgui) {
        super(clientgui);

        if (clientgui != null) {
            game.addGameListener(this);
            game.setupTeams();
        }

        setupStatusBar(Messages.getString("MovementDisplay.waitingForMovementPhase"));
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();
        gear = MovementDisplay.GEAR_LAND;
        registerKeyCommands();
    }

    @Override
    protected void setButtons() {
        // Create all the buttons
        buttons = new HashMap<>((int) (MoveCommand.values().length * 1.25 + 0.5));
        for (MoveCommand cmd : MoveCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "MovementDisplay."));
        }
    }

    @Override
    protected void setButtonsTooltips() {
        for (MoveCommand cmd : MoveCommand.values()) {
            String toolTip = createToolTip(cmd.getCmd(), "MovementDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(toolTip);
        }
    }

    protected boolean shouldPerformClearKeyCommand() {
        return !clientgui.isChatBoxActive() && !isIgnoringEvents() && isVisible();
    }

    private void turnLeft() {
        if (buttons.get(MoveCommand.MOVE_TURN).isEnabled()) {
            int finalFacing = cmd.getFinalFacing();
            finalFacing = (finalFacing + 5) % 6;
            Coords curPos = cmd.getFinalCoords();
            Coords target = curPos.translated(finalFacing);
            currentMoveHoldingShift(target, cmd.getFinalBoardId());
            updateMove();
        }
    }

    private void turnRight() {
        if (buttons.get(MoveCommand.MOVE_TURN).isEnabled()) {
            int finalFacing = cmd.getFinalFacing();
            finalFacing = (finalFacing + 7) % 6;
            Coords curPos = cmd.getFinalCoords();
            Coords target = curPos.translated(finalFacing);
            currentMoveHoldingShift(target, cmd.getFinalBoardId());
            updateMove();
        }
    }

    /**
     * Some types of movement require that shift is considered enabled/held
     *
     * @param target       target coordinate to which this movement is being executed
     * @param finalBoardId id of the final board where the movement ends
     */
    private void currentMoveHoldingShift(Coords target, int finalBoardId) {
        shiftHeld = true;
        currentMove(target, finalBoardId);
        shiftHeld = false;
    }

    private void undoIllegalStep() {
        // Remove all illegal steps, if none, then do a normal backspace function.
        if (!removeIllegalSteps()) {
            removeLastStep();
        }
        computeEnvelope();
    }

    private void undoLastStep() {
        removeLastStep();
        computeEnvelope();
    }

    private void computeEnvelope() {
        Entity currentEntity = currentEntity();
        if (currentEntity != null) {
            computeMovementEnvelope(currentEntity);
        }
    }

    private void cancel() {
        clear();
        Entity currentEntity = currentEntity();

        if (currentEntity != null) {
            computeMovementEnvelope(currentEntity);
            clientgui.updateFiringArc(currentEntity);
        }

        updateMove();
    }

    private void performToggleMovementMode() {
        if (gear != MovementDisplay.GEAR_JUMP) {
            if (buttons.get(MoveCommand.MOVE_JUMP).isEnabled()) {
                buttons.get(MoveCommand.MOVE_JUMP).doClick();
            }
        } else {
            if (buttons.get(MoveCommand.MOVE_WALK).isEnabled()) {
                buttons.get(MoveCommand.MOVE_WALK).doClick();
            }
        }
    }

    private void moveBackUp() {
        buttons.get(MoveCommand.MOVE_BACK_UP).doClick();
    }

    private void moveGoProne() {
        buttons.get(MoveCommand.MOVE_GO_PRONE).doClick();
    }

    private void moveGetUp() {
        buttons.get(MoveCommand.MOVE_GET_UP).doClick();
    }

    private void moveStepForward() {
        cmd.addStep(MoveStepType.FORWARDS);
        updateMove();
    }

    private void moveStepBackward() {
        cmd.addStep(MoveStepType.BACKWARDS);
        updateMove();
    }

    private void performToggleConversionMode() {
        final Entity currentlySelectedEntity = currentEntity();

        if (currentlySelectedEntity == null) {
            LOGGER.error("Cannot execute a conversion mode command for a null entity.");
            return;
        }

        EntityMovementMode nextMode = currentlySelectedEntity.nextConversionMode(cmd.getFinalConversionMode());

        // LAMs may have to skip the next mode due to damage
        if (currentlySelectedEntity instanceof LandAirMek landAirMek) {
            if (!landAirMek.canConvertTo(nextMode)) {
                nextMode = currentlySelectedEntity.nextConversionMode(nextMode);
            }

            if (!landAirMek.canConvertTo(nextMode)) {
                nextMode = currentlySelectedEntity.getMovementMode();
            }
        }
        adjustConvertSteps(nextMode);
    }

    /**
     * Register all of the {@link CommandAction}s for this panel display.
     */
    private void registerKeyCommands() {
        if ((clientgui == null) || (clientgui.controller == null)) {
            return;
        }

        MegaMekController controller = clientgui.controller;
        controller.registerCommandAction(KeyCommandBind.TURN_LEFT, this, this::turnLeft);
        controller.registerCommandAction(KeyCommandBind.TURN_RIGHT, this, this::turnRight);
        controller.registerCommandAction(KeyCommandBind.UNDO_LAST_STEP, this, this::undoIllegalStep);
        controller.registerCommandAction(KeyCommandBind.UNDO_SINGLE_STEP, this, this::undoLastStep);

        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT,
              this,
              () -> selectEntity(clientgui.getClient().getNextEntityNum(currentEntity)));
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT,
              this,
              () -> selectEntity(clientgui.getClient().getPrevEntityNum(currentEntity)));

        controller.registerCommandAction(KeyCommandBind.CANCEL, this::shouldPerformClearKeyCommand, this::cancel);
        controller.registerCommandAction(KeyCommandBind.TOGGLE_MOVE_MODE, this, this::performToggleMovementMode);
        controller.registerCommandAction(KeyCommandBind.MOVE_BACKUP, this, this::moveBackUp);
        controller.registerCommandAction(KeyCommandBind.MOVE_GO_PRONE, this, this::moveGoProne);
        controller.registerCommandAction(KeyCommandBind.MOVE_GETUP, this, this::moveGetUp);
        controller.registerCommandAction(KeyCommandBind.MOVE_STEP_FORWARD, this, this::moveStepForward);
        controller.registerCommandAction(KeyCommandBind.MOVE_STEP_BACKWARD, this, this::moveStepBackward);
        controller.registerCommandAction(KeyCommandBind.TOGGLE_CONVERSION_MODE,
              this,
              this::performToggleConversionMode);
    }

    /**
     * @return the button list: we need to determine what unit type is selected and then get a button list appropriate
     *       for that unit.
     */
    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        final Entity currentlySelectedEntity = currentEntity();
        int flag = CMD_MEK;
        // Chain of Instance Of Tests that should be refactored using either polymorphism or something else.
        if (currentlySelectedEntity != null) {
            if (currentlySelectedEntity instanceof Infantry) {
                flag = CMD_INF;
            } else if (currentlySelectedEntity instanceof VTOL) {
                flag = CMD_VTOL;
            } else if (currentlySelectedEntity instanceof Tank) {
                flag = CMD_TANK;
            } else if (currentlySelectedEntity.isAero()) {
                if (currentlySelectedEntity.isAirborne()) {
                    flag = game.useVectorMove() ? CMD_AERO_VECTORED : CMD_AERO;
                } else {
                    flag = CMD_TANK;
                }

                if (currentlySelectedEntity instanceof LandAirMek) {
                    flag |= CMD_CONVERTER;
                }
            } else if (currentlySelectedEntity instanceof QuadVee) {
                if (currentlySelectedEntity.getConversionMode() == QuadVee.CONV_MODE_MEK) {
                    flag = CMD_MEK | CMD_CONVERTER;
                } else {
                    flag = CMD_TANK | CMD_CONVERTER;
                }
            } else if (currentlySelectedEntity instanceof LandAirMek) {
                if (currentlySelectedEntity.getConversionMode() == LandAirMek.CONV_MODE_AIR_MEK) {
                    flag = CMD_TANK | CMD_CONVERTER | CMD_AIR_MEK;
                } else {
                    flag = CMD_MEK | CMD_CONVERTER;
                }
            } else if ((currentlySelectedEntity instanceof Mek) && ((Mek) currentlySelectedEntity).hasTracks()) {
                flag = CMD_MEK | CMD_CONVERTER;
            } else if ((currentlySelectedEntity instanceof ProtoMek) &&
                  currentlySelectedEntity.getMovementMode().isWiGE()) {
                flag = CMD_PROTOMEK | CMD_MEK | CMD_AIR_MEK;
            } else if (currentlySelectedEntity instanceof ProtoMek) {
                flag = CMD_PROTOMEK;
            }
        }

        return getButtonList(flag);
    }

    private ArrayList<MegaMekButton> getButtonList(int flag) {
        boolean forwardIni = false;
        GameOptions gameOptions = null;

        if (clientgui != null) {
            Player localPlayer = clientgui.getClient().getLocalPlayer();
            forwardIni = (game.getTeamForPlayer(localPlayer) != null) &&
                  (game.getTeamForPlayer(localPlayer).size() > 1);
            gameOptions = game.getOptions();
        }

        ArrayList<MegaMekButton> buttonList = new ArrayList<>();

        int i = 0;
        MoveCommand[] commands = MoveCommand.values(flag, gameOptions, forwardIni);
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (MoveCommand moveCommand : commands) {
            if (i % buttonsPerGroup == 0) {
                buttonList.add(getBtn(MoveCommand.MOVE_NEXT));
                i++;
            }

            buttonList.add(buttons.get(moveCommand));
            i++;

            if ((i + 1) % buttonsPerGroup == 0) {
                buttonList.add(getBtn(MoveCommand.MOVE_MORE));
                i++;
            }
        }
        if (!buttonList.get(i - 1).getActionCommand().equals(MoveCommand.MOVE_MORE.getCmd())) {
            while ((i + 1) % buttonsPerGroup != 0) {
                buttonList.add(null);
                i++;
            }
            buttonList.add(getBtn(MoveCommand.MOVE_MORE));
        }
        numButtonGroups = (int) Math.ceil((buttonList.size() + 0.0) / buttonsPerGroup);
        return buttonList;
    }

    /**
     * Hands over the current turn to the next valid player on the same team as the supplied player. If no player on the
     * team apart from this player has any turns left, it activates this player again.
     */
    public synchronized void selectNextPlayer() {
        clientgui.getClient().sendNextPlayer();
    }

    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int entityID) {
        final Entity selectedEntity = game.getEntity(entityID);
        if (selectedEntity == null) {
            LOGGER.error("Tried to select non-existent entity with id {}", entityID);
            return;
        }

        if (selectedEntity.isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(selectedEntity);
        }

        currentEntity = entityID;
        clientgui.setSelectedEntityNum(entityID);
        updateUnitDisplay(selectedEntity);

        gear = MovementDisplay.GEAR_LAND;
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).setHighlightColor(GUIP.getMoveDefaultColor()));

        clear();
        updateButtonsLater();

        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        clientgui.getBoardView(selectedEntity).highlight(selectedEntity.getPosition());
        if (!clientgui.isCurrentBoardViewShowingAnimation()) {
            clientgui.centerOnUnit(selectedEntity);
        }

        initializeStatusBarText(selectedEntity);

        clientgui.clearFieldOfFire();
        computeMovementEnvelope(selectedEntity);
        updateMove();
        computeCFWarningHexes(selectedEntity);
    }

    private void initializeStatusBarText(Entity selectedEntity) {
        String remainingPlayerWithTurns = getRemainingPlayerWithTurns();

        String yourTurnMsg = Messages.getString("MovementDisplay.its_your_turn") + remainingPlayerWithTurns;
        if (selectedEntity.hasQuirk(OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE)) {
            String poorPerfMsg;

            if (selectedEntity.getMpUsedLastRound() < selectedEntity.getWalkMP()) {
                poorPerfMsg = Messages.getString("MovementDisplay.NotUpToSpeed");
            } else {
                poorPerfMsg = Messages.getString("MovementDisplay.UpToSpeed");
            }

            setStatusBarText("<html><center>" + yourTurnMsg + "<br>" + poorPerfMsg + "</center></html>");
        } else {
            setStatusBarText(yourTurnMsg);
        }
    }

    private MegaMekButton getBtn(MoveCommand command) {
        return buttons.get(command);
    }

    private boolean isEnabled(MoveCommand command) {
        return getBtn(command).isEnabled();
    }

    /**
     * Updates the Unit Display Panels by selecting the current entity in the unit display and shows the
     * {@link MekPanelTabStrip} if configured.
     *
     * @param entity Currently Selected Entity
     */
    private void updateUnitDisplay(final Entity entity) {
        clientgui.getUnitDisplay().displayEntity(entity);
        if (GUIP.getMoveDisplayTabDuringMovePhases()) {
            clientgui.getUnitDisplay().showPanel(MekPanelTabStrip.SUMMARY);
        }
    }

    /**
     * Sets buttons to their proper state, but let's Swing do this later after all the current BoardView repaints and
     * updates are complete. This is done to prevent some buttons from painting correctly when the maps are zoomed way
     * out. See Issue: #4444
     */
    private void updateButtonsLater() {
        SwingUtilities.invokeLater(this::updateButtons);
    }

    /**
     * @return True when the active unit has jump MP available, either standard jump jet MP or, in the case of Meks, MP
     *       of Mechanical Jump Boosters.
     */
    private boolean hasJumpMP() {
        return (currentEntity() != null) && (currentEntity().getAnyTypeMaxJumpMP() > 0);
    }

    /**
     * Sets the buttons to their proper states
     */
    private void updateButtons() {
        final GameOptions gameOptions = game.getOptions();
        final Entity selectedUnit = currentEntity();
        if (selectedUnit == null) {
            LOGGER.error("Cannot update buttons based on a null entity");
            return;
        }
        boolean isMEK = (selectedUnit instanceof Mek);
        boolean isInfantry = (selectedUnit instanceof Infantry);
        boolean isTank = (selectedUnit instanceof Tank);
        boolean isAero = selectedUnit.isAero();

        setWalkEnabled(!selectedUnit.isImmobile() &&
              ((selectedUnit.getWalkMP() > 0) || (selectedUnit.getRunMP() > 0)) &&
              !selectedUnit.isStuck());

        // Conventional infantry also uses jump MP for VTOL and UMU MP
        setJumpEnabled(!isAero &&
              !selectedUnit.isImmobileForJump() &&
              !selectedUnit.isProne() &&
              (hasJumpMP() &&
                    (!selectedUnit.isConventionalInfantry() ||
                          selectedUnit.getMovementMode().isJumpInfantry())) &&
              !(selectedUnit.isStuck() && !selectedUnit.canUnstickByJumping()));

        setSwimEnabled(!isAero &&
              !selectedUnit.isImmobile() &&
              (selectedUnit.getActiveUMUCount() > 0) &&
              selectedUnit.isUnderwater());

        setBackUpEnabled(!isAero && isEnabled(MoveCommand.MOVE_WALK));
        setChargeEnabled(selectedUnit.canCharge());
        setDFAEnabled(selectedUnit.canDFA());
        setRamEnabled(selectedUnit.canRam());

        if (isInfantry) {
            setClearEnabled(game.containsMinefield(selectedUnit.getPosition()));
        } else {
            setClearEnabled(false);
        }

        boolean entityNotAbleToClimb = Stream.of(EntityMovementMode.HYDROFOIL,
                    EntityMovementMode.NAVAL,
                    EntityMovementMode.SUBMARINE,
                    EntityMovementMode.INF_UMU,
                    EntityMovementMode.VTOL,
                    EntityMovementMode.BIPED_SWIM,
                    EntityMovementMode.QUAD_SWIM)
              .noneMatch(entityMovementMode -> (selectedUnit.getMovementMode() ==
                    entityMovementMode));

        getBtn(MoveCommand.MOVE_CLIMB_MODE).setEnabled(entityNotAbleToClimb);
        updateTurnButton();

        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateLoadButtons();
        updateElevationButtons();
        updateTakeOffButtons();
        updateLandButtons();
        updateJoinButton();
        updateRecoveryButton();
        updateEvadeButton();
        updateBootleggerButton();
        updateLayMineButton();
        updateChaffButton();
        updateStartupButton();
        updateShutdownButton();

        updateAeroButtons();

        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();
        updateFlyOffButton();
        updateLaunchButton();
        updateDropButton();
        updateConvertModeButton();
        updateRecklessButton();
        updateBraceButton();
        updateHoverButton();
        updateManeuverButton();
        updateStrafeButton();
        updateBombButton();

        // Infantry and Tank - Fortify
        if (isInfantry && selectedUnit.hasWorkingMisc(MiscType.F_TRENCH_CAPABLE)) {
            // Crews adrift in space or atmosphere can't do this
            if (selectedUnit instanceof EjectedCrew &&
                  (selectedUnit.isSpaceborne() || selectedUnit.isAirborne())) {
                getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(false);
            } else {
                getBtn(MoveCommand.MOVE_FORTIFY).setEnabled(true);
            }
        } else {
            getBtn(MoveCommand.MOVE_FORTIFY).setEnabled(isTank &&
                  selectedUnit.hasWorkingMisc(MiscType.F_TRENCH_CAPABLE));
        }

        // Infantry - Digging in, TO:AR p.106; could add terrain checking and restrict to first action here
        boolean canDigIn = (selectedUnit instanceof Infantry infantry)
              && gameOptions.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_DIG_IN)
              && game.isOnGroundMap(selectedUnit)
              && (selectedUnit.getAltitude() == 0)
              && (selectedUnit.getElevation() == 0)
              && !infantry.isMechanized()
              && (infantry.getDugIn() == Infantry.DUG_IN_NONE);
        getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(canDigIn);

        // Infantry - Take Cover
        // Crews adrift in space or atmosphere can't do this
        if (selectedUnit instanceof EjectedCrew &&
              (selectedUnit.isSpaceborne() || selectedUnit.isAirborne())) {
            getBtn(MoveCommand.MOVE_TAKE_COVER).setEnabled(false);
        } else {
            updateTakeCoverButton();
        }

        // Infantry - Urban Guerrilla calling for support
        getBtn(MoveCommand.MOVE_CALL_SUPPORT).setEnabled(isInfantry &&
              selectedUnit.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA) &&
              ((Infantry) selectedUnit).getCanCallSupport());

        getBtn(MoveCommand.MOVE_SHAKE_OFF).setEnabled((selectedUnit instanceof Tank) &&
              (selectedUnit.getSwarmAttackerId() !=
                    Entity.NONE));

        updateFleeButton();

        if (gameOptions.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLES_CAN_EJECT) &&
              (selectedUnit instanceof Tank)) {
            // Vehicles don't have ejection systems, so crews abandon, and must enter a valid hex. If they cannot,
            // they can't abandon as per TO pg 197.
            Coords position = currentEntity().getPosition();
            Infantry infantry = new Infantry();
            infantry.setGame(game);
            boolean hasLegalHex = !infantry.isLocationProhibited(position);
            for (int i = 0; i < 6; i++) {
                hasLegalHex |= !infantry.isLocationProhibited(position.translated(i));
            }

            setEjectEnabled(hasLegalHex);
        } else {
            setEjectEnabled(((isMEK &&
                  (((Mek) selectedUnit).getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED)) ||
                  isAero) &&
                  selectedUnit.isActive() &&
                  !selectedUnit.hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT));
        }

        // Mek abandonment - only available for prone+shutdown Meks per TacOps:AR p.165
        if (isMEK && (selectedUnit instanceof Mek mek)) {
            setAbandonEnabled(mek.canAbandon());
        } else {
            setAbandonEnabled(false);
        }

        // if dropping unit only allows turning
        if (!selectedUnit.isAero() && cmd.getFinalAltitude() > 0) {
            disableButtons();
            if (selectedUnit instanceof LandAirMek) {
                updateConvertModeButton();
                if (selectedUnit.getMovementMode().isWiGE() &&
                      (selectedUnit.getAltitude() <= 3)) {
                    updateHoverButton();
                }
                getBtn(MoveCommand.MOVE_MORE).setEnabled(numButtonGroups > 1);
            }
            butDone.setEnabled(true);
        }

        // If a Small Craft / DropShip that has unloaded units, then only allowed to unload more
        if (selectedUnit.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
        }

        setupButtonPanel();
        updateDonePanel();
        updateMoreButton(); // Update more needs to go last!
    }

    private void addStepToMovePath(MoveStepType moveStep) {
        cmd.addStep(moveStep);
        updateMove();
    }

    private void addStepsToMovePath(MoveStepType... moveSteps) {
        for (MoveStepType moveStep : moveSteps) {
            cmd.addStep(moveStep);
        }

        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, Entity entity) {
        cmd.addStep(moveStep, entity);
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, TreeMap<Integer, Vector<Integer>> targets) {
        cmd.addStep(moveStep, targets);
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, boolean noCost) {
        cmd.addStep(moveStep, noCost);
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, boolean noCost, boolean isManeuver, int maneuverType) {
        cmd.addStep(moveStep, noCost, isManeuver, maneuverType);
        updateMove();
    }

    private void addStepsToMovePath(boolean noCost, boolean isManeuver, int maneuverType, MoveStepType... moveSteps) {
        for (MoveStepType moveStep : moveSteps) {
            cmd.addStep(moveStep, noCost, isManeuver, maneuverType);
        }
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, Entity entity, Coords coords) {
        cmd.addStep(moveStep, entity, coords);
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, Minefield minefield) {
        cmd.addStep(moveStep, minefield);
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, int additionalIntData) {
        cmd.addStep(moveStep, additionalIntData);
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, int recover, int mineToLay) {
        cmd.addStep(moveStep, recover, mineToLay);
        updateMove();
    }

    private void addStepToMovePath(MoveStepType moveStep, Map<Integer, Integer> additionalIntData) {
        cmd.addStep(moveStep, additionalIntData);
        updateMove();
    }

    private void updateMove() {
        updateMove(true);
    }

    private void updateMove(boolean redrawMovement) {
        Entity currentEntity = currentEntity();
        if (redrawMovement && (currentEntity != null)) {
            clientgui.getBoardView(currentEntity).drawMovementData(currentEntity, cmd);
        }

        updateFleeButton();
        updateDonePanel();
    }

    private void updateFleeButton() {
        Entity movingEntity = currentEntity();
        if (movingEntity == null) {
            return;
        }

        boolean hasLastStep = (cmd != null) && (cmd.getLastStep() != null);
        boolean fleeStart = !hasLastStep && movingEntity.canFlee();
        boolean fleeEnd = hasLastStep
              && (cmd.getMpUsed() < cmd.getMaxMP())
              && (cmd.getLastStepMovementType() != EntityMovementType.MOVE_ILLEGAL)
              && game.canFleeFrom(movingEntity, cmd.getLastStep().getPosition());

        setFleeEnabled(fleeStart || fleeEnd);
    }

    private void updateAeroButtons() {
        Entity movingEntity = currentEntity();
        if (movingEntity instanceof IAero aero) {
            getBtn(MoveCommand.MOVE_THRUST).setEnabled(true);
            getBtn(MoveCommand.MOVE_YAW).setEnabled(true);
            getBtn(MoveCommand.MOVE_END_OVER).setEnabled(true);
            getBtn(MoveCommand.MOVE_TURN_LEFT).setEnabled(true);
            getBtn(MoveCommand.MOVE_TURN_RIGHT).setEnabled(true);
            setEvadeAeroEnabled(cmd != null && !cmd.contains(MoveStepType.EVADE));
            setEjectEnabled(true);
            // no turning for spheroids in atmosphere
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            boolean spheroidOrLessThanThin = aero.isSpheroid()
                  || conditions.getAtmosphere().isLighterThan(Atmosphere.THIN);
            if (spheroidOrLessThanThin && !movingEntity.isSpaceborne()) {
                setTurnEnabled(false);
            }
        }
    }

    /**
     * toggles the status of the Done and No Nag buttons based on if the current move order is valid
     */
    @Override
    protected void updateDonePanel() {
        // we don't need to be doing all this stuff if we're not showing this
        if (!game.getPhase().isMovement()) {
            return;
        }

        if (cmd == null || cmd.length() == 0) {
            updateDonePanelButtons(Messages.getString("MovementDisplay.Move"),
                  Messages.getString("MovementDisplay.Skip"),
                  false,
                  null);
            return;
        }

        MovePath possible = cmd.clone();
        possible.clipToPossible();
        Entity currentEntity = currentEntity();
        if ((possible.length() == 0) || (currentEntity == null)) {
            updateDonePanelButtons(Messages.getString("MovementDisplay.Move"),
                  Messages.getString("MovementDisplay.Skip"),
                  false,
                  null);
        } else if (!possible.isMoveLegal()) {
            updateDonePanelButtons(Messages.getString("MovementDisplay.IllegalMove"),
                  Messages.getString("MovementDisplay.Skip"),
                  false,
                  null);
        } else {
            int mp = possible.countMp(possible.isJumping());
            boolean psrCheck = (!SharedUtility.doPSRCheck(cmd.clone()).isBlank())
                  || (!SharedUtility.doThrustCheck(cmd.clone(), clientgui.getClient()).isBlank());
            boolean damageCheck = cmd.shouldMechanicalJumpCauseFallDamage() || cmd.hasActiveMASC()
                  || (!(currentEntity instanceof VTOL) && cmd.hasActiveSupercharger()) || cmd.willCrushBuildings();
            String moveMsg = Messages.getString("MovementDisplay.Move") + " (" + mp + "MP)" + (psrCheck ? "*" : "")
                  + (damageCheck ? "!" : "");
            updateDonePanelButtons(moveMsg, Messages.getString("MovementDisplay.Skip"), true, computeTurnDetails());
        }
    }

    private List<String> computeTurnDetails() {
        String validTextColor = AbstractBoardViewOverlay.colorToHex(AbstractBoardViewOverlay.getTextColor());
        String invalidTextColor = AbstractBoardViewOverlay.colorToHex(AbstractBoardViewOverlay.getTextColor(), 0.7f);

        MoveStepType accumType = null;
        int accumTypeCount = 0;
        int accumMP = 0;
        int accumDanger = 0;
        boolean accumLegal = true;
        String unicodeIcon = "";

        ArrayList<String> turnDetails = new ArrayList<>();
        for (final ListIterator<MoveStep> steps = cmd.getSteps(); steps.hasNext(); ) {
            MoveStep currentStep = steps.next();
            MoveStepType currentType = currentStep.getType();
            int currentDanger = currentStep.isDanger() ? 1 : 0;
            boolean currentLegal = currentStep.isLegal(cmd);

            if (accumTypeCount != 0 && accumType == currentType && accumLegal == currentLegal) {
                accumTypeCount++;
                accumMP += currentStep.getMp();
                accumDanger += currentDanger;
                continue;
            }

            // switching to a new move type, so write a line
            if (accumTypeCount != 0) {
                turnDetails.add(String.format(turnDetailsFormat,
                      accumLegal ? validTextColor : invalidTextColor,
                      accumTypeCount == 1 ? "" : "x" + accumTypeCount,
                      accumType,
                      unicodeIcon,
                      accumMP,
                      "*".repeat(accumDanger)));
            }

            // switching to a new move type, reset
            accumType = currentType;
            accumTypeCount = 1;
            accumMP = currentStep.getMp();
            accumDanger = currentDanger;
            accumLegal = currentLegal;
            unicodeIcon = switch (accumType) {
                case TURN_LEFT -> "\u21B0";
                case TURN_RIGHT -> "\u21B1";
                case FORWARDS -> "\u2191";
                case BACKWARDS -> "\u2193";
                case START_JUMP -> "\u21EF";
                default -> "";
            };
        }

        // add line for last moves
        turnDetails.add(String.format(turnDetailsFormat,
              accumLegal ? validTextColor : invalidTextColor,
              accumTypeCount == 1 ? "" : "x" + accumTypeCount,
              accumType, unicodeIcon, accumMP, "*".repeat(accumDanger)));
        return turnDetails;
    }

    private boolean shouldDesignateFlightPath(Entity entity) {
        return entity != null
              && game.hasBoardLocation(finalPosition(), finalBoardId())
              && entity.isAirborne()
              && game.getBoard(entity).isLowAltitude()
              && game.getBoard(entity).getEmbeddedBoardHexes().contains(finalPosition())
              && cmd != null
              // Only designate a flight path when movement ends in that hex legally (all velocity used)
              && cmd.isMoveLegal()
              // Interpreting TW p.242 to include that the unit must have entered that hex in this movement and cannot
              // just hover at velocity 0, doing flight paths at will; with aero-on-ground move, there is naturally
              // no flight path without actually moving
              && cmd.getDistanceTravelled() > 0;
    }

    private int flightPathTarget(Entity entity) {
        if (shouldDesignateFlightPath(entity)) {
            return groundMapAtAtmosphericHex();
        } else {
            return 0;
        }
    }

    private int groundMapAtAtmosphericHex() {
        if (game.hasBoardLocation(finalPosition(), finalBoardId())
              && game.getBoard(finalBoardId()).isLowAltitude()
              && game.getBoard(finalBoardId()).getEmbeddedBoardHexes().contains(finalPosition())) {
            return game.getBoard(finalBoardId()).getEmbeddedBoardAt(finalPosition());
        } else {
            // fall back to the standard map to be safe
            return 0;
        }
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        initDonePanelForNewTurn();
        setNextEnabled(true);
        setForwardIniEnabled(true);
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();
        updateMoreButton();

        if (!clientgui.isCurrentBoardViewShowingAnimation()) {
            clientgui.maybeShowUnitDisplay();
        }

        if (GUIP.getAutoSelectNextUnit()) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
        }

        startTimer();
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private synchronized void endMyTurn() {
        final Entity currentlySelectedEntity = currentEntity();

        stopTimer();

        // end my turn, then.
        disableButtons();
        Entity next = game
              .getNextEntity(game.getTurnIndex());
        if (game.getPhase().isMovement()
              && (null != next)
              && (null != currentlySelectedEntity)
              && (next.getOwnerId() != currentlySelectedEntity.getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }
        currentEntity = Entity.NONE;
        clearFlightPath();
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        // Return the highlight sprite back to its original color
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).setHighlightColor(Color.WHITE));
        clientgui.setSelectedEntityNum(Entity.NONE);
        clearMovementSprites();
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();
        cmd = null;
    }

    private void clearFlightPath() {
        if (flightPath != null) {
            clientgui.onAllBoardViews(bv -> bv.removeSprite(flightPath));
        }
        flightPathPosition = null;
        flightPath = null;
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setWalkEnabled(false);
        setJumpEnabled(false);
        setBackUpEnabled(false);
        setChaffEnabled(false);
        setTurnEnabled(false);
        setFleeEnabled(false);
        setFlyOffEnabled(false);
        setEjectEnabled(false);
        setAbandonEnabled(false);
        setUnjamEnabled(false);
        setSearchlightEnabled(false, false);
        setGetUpEnabled(false);
        setGoProneEnabled(false);
        setChargeEnabled(false);
        setDFAEnabled(false);
        setNextEnabled(false);
        setForwardIniEnabled(false);
        getBtn(MoveCommand.MOVE_MORE).setEnabled(false);
        butDone.setEnabled(false);
        butSkipTurn.setEnabled(false);
        setLoadEnabled(false);
        setMountEnabled(false);
        setTowEnabled(false);
        setUnloadEnabled(false);
        setDisconnectEnabled(false);
        setClearEnabled(false);
        setHullDownEnabled(false);
        setBraceEnabled(false);
        setSwimEnabled(false);
        setModeConvertEnabled(false);
        setAccEnabled(false);
        setDecEnabled(false);
        setEvadeEnabled(false);
        setBootleggerEnabled(false);
        setShutdownEnabled(false);
        setStartupEnabled(false);
        setSelfDestructEnabled(false);
        setTraitorEnabled(false);
        setEvadeAeroEnabled(false);
        setAccNEnabled(false);
        setDecNEnabled(false);
        setRollEnabled(false);
        setLaunchEnabled(false);
        setDockEnabled(false);
        setDropEnabled(false);
        setThrustEnabled(false);
        setYawEnabled(false);
        setEndOverEnabled(false);
        setTurnLeftEnabled(false);
        setTurnRightEnabled(false);
        setDumpEnabled(false);
        setRamEnabled(false);
        setHoverEnabled(false);
        setJoinEnabled(false);
        setTakeOffEnabled(false);
        setVTakeOffEnabled(false);
        setLandEnabled(false);
        setVLandEnabled(false);
        setLowerEnabled(false);
        setRaiseEnabled(false);
        setRecklessEnabled(false);
        setGoProneEnabled(false);
        setManeuverEnabled(false);
        setStrafeEnabled(false);
        setBombEnabled(false);
        setPickupCargoEnabled(false);
        setDropCargoEnabled(false);

        getBtn(MoveCommand.MOVE_CLIMB_MODE).setEnabled(false);
        getBtn(MoveCommand.MOVE_DIG_IN).setEnabled(false);
        getBtn(MoveCommand.MOVE_CALL_SUPPORT).setEnabled(false);
    }

    /**
     * Clears out the currently selected movement data and resets it.
     */
    @Override
    public void clear() {
        final Entity currentlySelectedEntity = currentEntity();

        // clear board cursors
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        // Needed to clear best move modifiers
        clientgui.clearTemporarySprites();

        if (currentlySelectedEntity == null) {
            return;
        }

        // Remove Careful stand, in case it was set
        currentlySelectedEntity.setCarefulStand(false);
        currentlySelectedEntity.setIsJumpingNow(false);
        currentlySelectedEntity.setConvertingNow(false);
        currentlySelectedEntity.setClimbMode(GUIP.getMoveDefaultClimbMode());

        // switch back from swimming to normal mode.
        if (currentlySelectedEntity.getMovementMode() == EntityMovementMode.BIPED_SWIM) {
            currentlySelectedEntity.setMovementMode(EntityMovementMode.BIPED);
        } else if (currentlySelectedEntity.getMovementMode() == EntityMovementMode.QUAD_SWIM) {
            currentlySelectedEntity.setMovementMode(EntityMovementMode.QUAD);
        }

        // create new current and considered paths
        cmd = new MovePath(game, currentlySelectedEntity);
        clientgui.updateFiringArc(currentlySelectedEntity);
        clientgui.showSensorRanges(currentlySelectedEntity, cmd.getFinalCoords());
        computeCFWarningHexes(currentlySelectedEntity);

        // set to "walk," or the equivalent
        gear = MovementDisplay.GEAR_LAND;
        jumpSubGear = GEAR_SUB_STANDARD;
        clearFlightPath();
        Color walkColor = GUIP.getMoveDefaultColor();
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).setHighlightColor(walkColor));
        initializeStatusBarText(currentlySelectedEntity);

        // update some GUI elements
        clearMovementSprites();
        updateDonePanel();
        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateElevationButtons();
        updateTakeOffButtons();
        updateLandButtons();
        updateFlyOffButton();
        updateLaunchButton();
        updateDropButton();
        updateConvertModeButton();
        updateRecklessButton();
        updateBraceButton();
        updateHoverButton();
        updateManeuverButton();
        updateAeroButtons();
        updateLayMineButton();

        unloadableUnits = currentlySelectedEntity.getUnloadableUnits();
        towedUnits = currentlySelectedEntity.getLoadedTrailers();

        updateLoadButtons();
        updateJoinButton();
        updateRecoveryButton();
        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();

        // if dropping unit only allows turning
        if (!currentlySelectedEntity.isAero() && cmd.getFinalAltitude() > 0) {
            disableButtons();
            if (currentlySelectedEntity instanceof LandAirMek) {
                updateConvertModeButton();
                if (currentlySelectedEntity.getMovementMode().isWiGE() &&
                      (currentlySelectedEntity.getAltitude() <= 3)) {
                    updateHoverButton();
                }
                getBtn(MoveCommand.MOVE_MORE).setEnabled(numButtonGroups > 1);
            } else {
                gear = MovementDisplay.GEAR_TURN;
            }
            butDone.setEnabled(true);
        }

        // If a Small Craft / DropShip that has unloaded units, then only allowed to unload more
        if (currentlySelectedEntity.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
            butDone.setEnabled(true);
        }

        clientgui.showBoardView(currentlySelectedEntity.getBoardId());
    }

    private void removeLastStep() {
        if (cmd == null) {
            LOGGER.warn("Cannot process removeLastStep() request, cmd is null!");
            return;
        }

        cmd.removeLastStep();

        final Entity currentlySelectedEntity = currentEntity();
        if (currentlySelectedEntity == null) {
            LOGGER.warn("Cannot process removeLastStep for a null currentlySelectedEntity.");
            return;
        } else if (cmd.length() == 0) {
            clear();
            if ((gear == MovementDisplay.GEAR_JUMP) && !cmd.isJumping()) {
                initializeJumpMovePath();
            } else if (currentlySelectedEntity.isConvertingNow()) {
                addStepToMovePath(MoveStepType.CONVERT_MODE);
            }
        } else {
            // clear board cursors
            clientgui.getBoardView(currentEntity()).select(cmd.getFinalCoords());
            clientgui.getBoardView(currentEntity()).cursor(cmd.getFinalCoords());
            clientgui.getBoardView(currentEntity()).drawMovementData(currentlySelectedEntity, cmd);
            clientgui.updateFiringArc(currentlySelectedEntity);
            clientgui.showSensorRanges(currentlySelectedEntity, cmd.getFinalCoords());

            // FIXME what is this
            // Set the button's label to "Done" if the entire move is impossible.
            MovePath possible = cmd.clone();
            possible.clipToPossible();
            if (possible.length() == 0) {
                updateDonePanel();
            }
        }
        updateButtons();
    }

    private void initializeJumpMovePath() {
        addStepToMovePath(MoveStepType.START_JUMP);
        if (jumpSubGear == GEAR_SUB_MEK_BOOSTERS) {
            addStepToMovePath(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER);
        }
    }

    /**
     * Removes all the trailing illegal movement steps and the end of the current entities movement path. (This is
     * helpful for Aero movement, overshooting MP and wanting to evade etc.)
     *
     * @return - Returns true if the call removed any illegal steps otherwise returns false.
     */
    private boolean removeIllegalSteps() {
        if (cmd == null) {
            return false;
        }

        boolean removed = false;

        // Keep removing the last step until it's a valid movement step.
        while (cmd.getLastStepMovementType() == EntityMovementType.MOVE_ILLEGAL) {
            removeLastStep();
            removed = true;
        }

        return removed;
    }

    private boolean checkNags() {
        String check = SharedUtility.doPSRCheck(cmd);
        String thrustCheck = SharedUtility.doThrustCheck(cmd, clientgui.getClient());

        Entity currentlySelectedEntity = currentEntity();

        if (needNagForNoAction()) {
            if ((currentlySelectedEntity != null) && (cmd.length() == 0) && !currentlySelectedEntity.isAirborne()) {
                // Hmm... no movement steps confirm this action
                String title = Messages.getString("MovementDisplay.ConfirmNoMoveDlg.title");
                String body = Messages.getString("MovementDisplay.ConfirmNoMoveDlg.message");
                if (checkNagForNoAction(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForNoUnJamRAC()) {
            if ((currentlySelectedEntity != null) && currentlySelectedEntity.canUnjamRAC() && !isUnJammingRAC) {
                // confirm this action
                String title = Messages.getString("MovementDisplay.ConfirmUnJamRACDlg.title");
                String body = Messages.getString("MovementDisplay.ConfirmUnJamRACDlg.message");
                if (checkNagForNoUnJamRAC(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForMASC()) {
            if ((currentlySelectedEntity != null) &&
                  cmd.hasActiveMASC() &&
                  !(currentlySelectedEntity instanceof VTOL)) {
                // pop up are you sure dialog
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmMASCRoll",
                      currentlySelectedEntity.getMASCTarget());
                if (checkNagForMASC(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForMASC()) {
            if ((currentlySelectedEntity != null) &&
                  !(currentlySelectedEntity instanceof VTOL) &&
                  cmd.hasActiveSupercharger()) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmSuperchargerRoll",
                      currentlySelectedEntity.getSuperchargerTarget());
                if (checkNagForMASC(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForSprint()) {
            boolean sprintOrVtolSprint = cmd.getLastStepMovementType() == EntityMovementType.MOVE_SPRINT ||
                  cmd.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_SPRINT;
            boolean quadVeeVehicle = cmd.getEntity() instanceof QuadVee &&
                  cmd.getEntity().getConversionMode() == QuadVee.CONV_MODE_VEHICLE;
            boolean tankOrQuadVee = cmd.getEntity() instanceof Tank || quadVeeVehicle;
            // no need to nag for vehicles using overdrive if they already get a PSR nag
            boolean psrNag = tankOrQuadVee && needNagForPSR();
            if (sprintOrVtolSprint && !psrNag) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmSprint");
                if (checkNagForSprint(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForPSR()) {
            if (!check.isBlank()) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmPilotingRoll") + check;
                if (checkNagForPSR(title, body)) {
                    return true;
                }
            }
        }

        // check for unsafe takeoffs
        if (needNagForPSR()) {
            boolean verticalTakeoffOrTakeoff = cmd.contains(MoveStepType.VERTICAL_TAKE_OFF) ||
                  cmd.contains(MoveStepType.TAKEOFF);
            if ((currentlySelectedEntity != null) && verticalTakeoffOrTakeoff) {
                boolean unsecure = false;
                for (Entity loaded : currentlySelectedEntity.getLoadedUnits()) {
                    if (loaded.wasLoadedThisTurn() && !(loaded instanceof Infantry)) {
                        unsecure = true;
                        break;
                    }
                }
                if (unsecure) {
                    String title = Messages.getString("MovementDisplay.areYouSure");
                    String body = Messages.getString("MovementDisplay.UnsecuredTakeoff");
                    if (checkNagForPSR(title, body)) {
                        return true;
                    }
                }
            }
        }

        // check for G-forces
        if (needNagForPSR()) {
            if (!thrustCheck.isBlank()) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmPilotingRoll") + thrustCheck;
                if (checkNagForPSR(title, body)) {
                    return true;
                }
            }
        }

        // Check to see if spheroids will drop elevation. They will do so if they're not hovering, landing, or
        // changing altitude voluntarily.
        if (needNagForPSR()) {
            if (Compute.useSpheroidAtmosphere(game, currentlySelectedEntity) &&
                  !cmd.contains(MoveStepType.HOVER) &&
                  !cmd.contains(MoveStepType.VERTICAL_LAND) &&
                  !cmd.contains(MoveStepType.UP) &&
                  !cmd.contains(MoveStepType.DOWN)) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.SpheroidAltitudeLoss") + thrustCheck;
                if (checkNagForPSR(title, body)) {
                    return true;
                }
            }
        }

        // Should we nag about taking fall damage with mechanical jump boosters?
        if (needNagForMechanicalJumpFallDamage()) {
            if ((currentlySelectedEntity != null) && cmd.shouldMechanicalJumpCauseFallDamage()) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmMechanicalJumpFallDamage",
                      cmd.getJumpMaxElevationChange(),
                      currentlySelectedEntity.getMechanicalJumpBoosterMP(),
                      cmd.getJumpMaxElevationChange() - currentlySelectedEntity.getMechanicalJumpBoosterMP());
                if (checkNagForMechanicalJumpFallDamage(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForCrushingBuildings()) {
            if (cmd.willCrushBuildings()) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmCrushingBuildings");
                if (checkNagForCrushingBuildings(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForWiGELanding()) {
            if (cmd.automaticWiGELanding(true)) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.ConfirmWiGELanding");
                if (checkNagForWiGELanding(title, body)) {
                    return true;
                }
            }
        }

        // Check for unused velocity for airborne and spacecraft.
        if (needNagForOther()) {
            if ((currentlySelectedEntity != null) && (null != cmd) && currentlySelectedEntity.isAero()) {
                boolean airborneOrSpaceborne = currentlySelectedEntity.isAirborne() ||
                      currentlySelectedEntity.isSpaceborne();
                boolean unusedVelocity;

                if (null != cmd.getLastStep()) {
                    unusedVelocity = cmd.getLastStep().getVelocityLeft() > 0;
                } else {
                    unusedVelocity = (((IAero) currentlySelectedEntity).getCurrentVelocity() > 0) &&
                          (currentlySelectedEntity.delta_distance == 0);
                }

                boolean offOrReturn = cmd.contains(MoveStepType.OFF) || cmd.contains(MoveStepType.RETURN);
                if (airborneOrSpaceborne &&
                      !game.useVectorMove() &&
                      !((IAero) currentlySelectedEntity).isOutControlTotal() &&
                      unusedVelocity &&
                      !offOrReturn &&
                      !cmd.contains(MoveStepType.LAND) &&
                      !cmd.contains(MoveStepType.VERTICAL_LAND) &&
                      !cmd.contains(MoveStepType.EJECT) &&
                      !cmd.contains(MoveStepType.FLEE)) {
                    String title = Messages.getString("MovementDisplay.VelocityLeft.title");
                    String body = Messages.getString("MovementDisplay.VelocityLeft.message");
                    clientgui.doAlertDialog(title, body);
                    // always return true here, or airborne units will make illegal moves.
                    return true;
                }
            }
        }

        // check to see if spheroids will drop an elevation
        if (needNagForOther()) {
            if (currentlySelectedEntity instanceof LandAirMek &&
                  currentlySelectedEntity.isAssaultDropInProgress() &&
                  cmd.getFinalConversionMode() == EntityMovementMode.AERODYNE) {
                String title = Messages.getString("MovementDisplay.areYouSure");
                String body = Messages.getString("MovementDisplay.insufficientAltitudeForConversion") + thrustCheck;
                if (!clientgui.doYesNoDialog(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForOther()) {
            if (currentEntity() != null
                  && (currentEntity() instanceof Infantry)
                  && ((Infantry) currentEntity()).hasMicrolite()) {
                boolean finalElevation = (currentEntity().getElevation() != cmd.getFinalElevation());
                boolean airborneVTOLOrWIGEOrFinalElevation = currentEntity().isAirborneVTOLorWIGE()
                      || finalElevation;
                int terrainLevelBuilding = game.getBoard(currentEntity()).getHex(cmd.getFinalCoords())
                      .terrainLevel(Terrains.BLDG_ELEV);
                int terrainLevelBridge = game.getBoard(currentEntity()).getHex(cmd.getFinalCoords())
                      .terrainLevel(Terrains.BRIDGE_ELEV);
                if (airborneVTOLOrWIGEOrFinalElevation
                      && !cmd.contains(MoveStepType.FORWARDS)
                      && !cmd.contains(MoveStepType.FLEE)
                      && cmd.getFinalElevation() > 0
                      && terrainLevelBuilding < cmd.getFinalElevation()
                      && terrainLevelBridge < cmd.getFinalElevation()) {
                    String title = Messages.getString("MovementDisplay.MicroliteMove.title");
                    String body = Messages.getString("MovementDisplay.MicroliteMove.message");
                    if (!clientgui.doYesNoDialog(title, body)) {
                        return true;
                    }
                }
            }
        }

        if (needNagForOther()) {
            boolean landOrVerticalLand = cmd.contains(MoveStepType.LAND) || cmd.contains(MoveStepType.VERTICAL_LAND);
            if ((currentlySelectedEntity != null) && landOrVerticalLand) {
                Set<Coords> landingPath = ((IAero) currentlySelectedEntity).getLandingCoords(cmd.contains(MoveStepType.VERTICAL_LAND),
                      cmd.getFinalCoords(),
                      cmd.getFinalFacing());
                if (landingPath.stream()
                      .map(c -> game.getBoard(currentEntity()).getHex(c))
                      .filter(Objects::nonNull)
                      .anyMatch(h -> h.containsTerrain(Terrains.ROUGH) || h.containsTerrain(Terrains.RUBBLE))) {
                    String title = Messages.getString("MovementDisplay.areYouSure");
                    String body = Messages.getString("MovementDisplay.ConfirmLandingGearDamage");
                    if (!clientgui.doYesNoDialog(title, body)) {
                        return true;
                    }
                }
            }
        }

        if (needNagForOther()) {
            if (currentlySelectedEntity instanceof Infantry) {
                InfantryMount mount = ((Infantry) currentlySelectedEntity).getMount();
                if ((mount != null) &&
                      currentlySelectedEntity.getMovementMode().isSubmarine() &&
                      (currentlySelectedEntity.underwaterRounds >= mount.getUWEndurance()) &&
                      cmd.isAllUnderwater(game)) {
                    String title = Messages.getString("MovementDisplay.areYouSure");
                    String body = Messages.getString("MovementDisplay.ConfirmMountSuffocation");
                    if (!clientgui.doYesNoDialog(title, body)) {
                        return true;
                    }
                }
            }
        }

        return currentEntity() == null;
    }

    @Override
    public synchronized void ready() {
        Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            return;
        }

        cmd.clipToPossible();

        if (checkNags()) {
            return;
        }

        disableButtons();

        if (currentEntity.isAirborne() || currentEntity.isSpaceborne()) {
            // Depending on the rules and location (i.e., space v. atmosphere), Aerospace might need to have
            // additional move steps tacked on. This must be done after all prompts; otherwise a user who cancels
            // will still have steps added to the MovePath.
            cmd = SharedUtility.moveAero(cmd, clientgui.getClient());
        }

        if (flightPathPosition != null) {
            cmd.setFlightPathHex(BoardLocation.of(flightPathPosition, flightPathTarget(currentEntity)));
        }

        if (isUsingChaff) {
            addStepToMovePath(MoveStepType.CHAFF);
            isUsingChaff = false;
        }

        clientgui.clearTemporarySprites();
        clearMovementSprites();
        if (currentEntity.hasUMU()) {
            clientgui.getClient().sendUpdateEntity(currentEntity);
        }
        clientgui.getClient().moveEntity(this.currentEntity, cmd);
        if (currentEntity.isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(currentEntity);
        }
        endMyTurn();
    }

    /**
     * Returns new {@link MovePath} for the currently selected movement type
     */
    private void currentMove(Coords dest, int boardId) {
        if (shiftHeld || (gear == GEAR_TURN)) {
            if (buttons.get(MoveCommand.MOVE_TURN).isEnabled()) {
                cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), false, ManeuverType.MAN_NONE);
            }
        } else if ((gear == GEAR_JUMP) && (jumpSubGear == GEAR_SUB_MEK_BOOSTERS)) {
            if (cmd.getFinalBoardId() != boardId) {
                // only extend the path when staying on the same board
                return;
            }
            // Jumps with mechanical jump boosters are special
            Coords src = (cmd.getLastStep() != null) ? cmd.getLastStep().getPosition() : currentEntity().getPosition();
            int direction = src.direction(dest);
            MoveStepType moveStepType = MoveStepType.stepTypeForRelativeDirection(direction,
                  currentEntity().getFacing());
            cmd.findSimplePathTo(dest, moveStepType, src.direction(dest), currentEntity().getFacing());

        } else if (gear == GEAR_STRAFE) {
            // Only set the steps that enter new hexes.
            int start = cmd.length();
            extendPathTo(dest, boardId, MoveStepType.FORWARDS);
            // Skip turns at the beginning of the new part of the path unless we're extending an existing strafing
            // pattern.
            if (start > 0 && !cmd.getStep(start - 1).isStrafingStep()) {
                while (start < cmd.length() && cmd.getStep(start).getType() != MoveStepType.FORWARDS) {
                    start++;
                }
            }

            for (int i = cmd.length() - 1; i >= start; i--) {
                cmd.setStrafingStep(cmd.getStep(i).getPosition());
            }

            cmd.compile(game, currentEntity(), false);
            gear = GEAR_LAND;
        } else if ((gear == GEAR_LAND) || (gear == GEAR_JUMP)) {
            extendPathTo(dest, boardId, MoveStepType.FORWARDS);
            if (shouldDesignateFlightPath(currentEntity())) {
                // Interpreting TW p.242 to mean that designating a flight path is optional, as making A2G attacks is
                // certainly optional
                gear = GEAR_FLIGHTPATH;
                setStatusBarText(Messages.getString("MovementDisplay.status.designateFlightPath"));
                new FlightPathNotice(clientgui).show();
            }
        } else if (gear == GEAR_BACKUP) {
            extendPathTo(dest, boardId, MoveStepType.BACKWARDS);
        } else if (gear == GEAR_CHARGE) {
            extendPathTo(dest, boardId, MoveStepType.CHARGE);
            // The path planner shouldn't add the charge step
            if (cmd.getFinalCoords().equals(dest) && (cmd.getLastStep().getType() != MoveStepType.CHARGE)) {
                cmd.removeLastStep();
                addStepToMovePath(MoveStepType.CHARGE);
            }
        } else if (gear == GEAR_DFA) {
            extendPathTo(dest, boardId, MoveStepType.DFA);
            // The path planner shouldn't add the DFA step
            if (cmd.getFinalCoords().equals(dest) && (cmd.getLastStep().getType() != MoveStepType.DFA)) {
                cmd.removeLastStep();
                addStepToMovePath(MoveStepType.DFA);
            }
        } else if (gear == GEAR_SWIM) {
            extendPathTo(dest, boardId, MoveStepType.SWIM);
        } else if (gear == GEAR_RAM) {
            extendPathTo(dest, boardId, MoveStepType.FORWARDS);
        } else if (gear == GEAR_IM_MEL) {
            addStepsToMovePath(true,
                  true,
                  ManeuverType.MAN_IMMELMAN,
                  MoveStepType.UP,
                  MoveStepType.UP,
                  MoveStepType.DEC,
                  MoveStepType.DEC);
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), true, ManeuverType.MAN_IMMELMAN);
            gear = GEAR_LAND;
        } else if (gear == GEAR_SPLIT_S) {
            addStepsToMovePath(true,
                  true,
                  ManeuverType.MAN_SPLIT_S,
                  MoveStepType.DOWN,
                  MoveStepType.DOWN,
                  MoveStepType.ACC);
            cmd.rotatePathfinder(cmd.getFinalCoords().direction(dest), true, ManeuverType.MAN_SPLIT_S);
            gear = GEAR_LAND;
        }
        if (gear == GEAR_LONGEST_WALK || gear == GEAR_LONGEST_RUN) {
            int maxMp;
            MoveStepType stepType;
            if (gear == GEAR_LONGEST_WALK) {
                maxMp = currentEntity().getWalkMP();
                stepType = MoveStepType.BACKWARDS;
                gear = GEAR_BACKUP;
            } else {
                maxMp = currentEntity().getRunMPWithoutMASC();
                stepType = MoveStepType.FORWARDS;
                gear = GEAR_LAND;
            }

            LongestPathFinder lpf;
            if (currentEntity().isAero()) {
                lpf = LongestPathFinder.newInstanceOfAeroPath(maxMp, currentEntity().getGame());
            } else {
                lpf = LongestPathFinder.newInstanceOfLongestPath(maxMp, stepType, currentEntity().getGame());
            }
            final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();
            lpf.addStopCondition(new StopConditionTimeout<>(timeLimit * 4));

            lpf.run(cmd);
            MovePath lPath = lpf.getComputedPath(dest);
            if (lPath != null) {
                cmd = lPath;
            }
        }

        clientgui.showSensorRanges(currentEntity(), cmd.getFinalCoords());
        clientgui.updateFiringArc(currentEntity());
    }

    /**
     * Tries to extend the existing path (cmd) to the given location, but only, if the current path ends on the same
     * board.
     *
     * @param dest    The destination
     * @param boardId The destination
     * @param type    The step type to use
     */
    private void extendPathTo(Coords dest, int boardId, MoveStepType type) {
        if (cmd.getFinalBoardId() == boardId) {
            cmd.findPathTo(dest, type);
        }
    }

    //
    // BoardListener
    //
    @Override
    public synchronized void hexMoused(BoardViewEvent boardViewEvent) {
        if (clientgui == null) {
            return;
        }

        final Entity currentlySelectedEntity = currentEntity();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        // don't make a movement path for aeros if advanced movement is on
        boolean noPath = (currentlySelectedEntity != null && currentlySelectedEntity.isAero())
              && game.useVectorMove();

        // ignore buttons other than 1
        if (!clientgui.getClient().isMyTurn() || ((boardViewEvent.getButton() != MouseEvent.BUTTON1))) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((boardViewEvent.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0) ||
              ((boardViewEvent.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0)) {
            return;
        }

        if ((currentlySelectedEntity != null) && (gear == GEAR_FLIGHTPATH)
              && (boardViewEvent.getBoardId() == flightPathTarget(currentlySelectedEntity))
              && (boardViewEvent.getType() == BoardViewEvent.BOARD_HEX_CLICKED)) {
            if (flightPath != null) {
                boardViewEvent.getBoardView().removeSprite(flightPath);
            }
            clearFlightPath();
            flightPathPosition = boardViewEvent.getCoords();
            List<Coords> line = BoardHelper.coordsLine(game.getBoard(boardViewEvent.getBoardId()),
                  flightPathPosition,
                  finalFacing());
            currentlySelectedEntity.setPassedThrough(new Vector<>(line));
            flightPath = new FlyOverSprite(boardViewEvent.getBoardView(), currentlySelectedEntity);
            boardViewEvent.getBoardView().addSprite(flightPath);
            updateDonePanel();
            return;
        }

        if ((currentlySelectedEntity != null)
              && (gear == GEAR_LANDING_AERO)
              && (boardViewEvent.getBoardId()
              == game.getBoard(currentlySelectedEntity).getEmbeddedBoardAt(currentlySelectedEntity.getPosition()))
              && (boardViewEvent.getType() == BoardViewEvent.BOARD_HEX_CLICKED)
              && (currentlySelectedEntity instanceof IAero aero)
              && hasLandingMoveStep()) {
            finalizeAeroLandFromAtmosphereMap(aero, boardViewEvent);
            return;
        }

        // check for shifty goodness
        if (shiftHeld == ((boardViewEvent.getModifiers() & InputEvent.SHIFT_DOWN_MASK) == 0)) {
            shiftHeld = (boardViewEvent.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
        }

        Coords currPosition = cmd != null ?
              cmd.getFinalCoords() :
              currentlySelectedEntity != null ? currentlySelectedEntity.getPosition() : null;

        if ((boardViewEvent.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) && !noPath) {
            if (!boardViewEvent.getCoords().equals(currPosition) || shiftHeld || (gear == MovementDisplay.GEAR_TURN)) {
                boardViewEvent.getBoardView().cursor(boardViewEvent.getCoords());
                // either turn or move
                if (currentlySelectedEntity != null) {
                    currentMove(boardViewEvent.getCoords(), boardViewEvent.getBoardId());
                    updateMove();
                }
            }
        } else if (boardViewEvent.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            Coords moveto = boardViewEvent.getCoords();
            updateMove();
            if (shiftHeld || (gear == MovementDisplay.GEAR_TURN)) {
                updateDonePanel();

                if (cmd != null) {
                    // FIXME what is this
                    // Set the button's label to "Done" if the entire move is impossible.
                    MovePath possible = cmd.clone();
                    possible.clipToPossible();
                    if (possible.length() == 0) {
                        updateDonePanel();
                    }
                }
            } else {
                boardViewEvent.getBoardView().select(boardViewEvent.getCoords());
            }

            if (gear == MovementDisplay.GEAR_RAM) {
                // check if the target is valid
                final Targetable target = chooseTarget(boardViewEvent.getCoords());
                if ((target == null) || target.equals(currentlySelectedEntity) || !target.isAero()) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantRam"),
                          Messages.getString("MovementDisplay.NoTarget"));
                    clear();
                    return;
                }

                // check if it's a valid ram
                // First I need to add moves to the path if advanced
                if (currentlySelectedEntity != null &&
                      currentlySelectedEntity.isAero() &&
                      game.useVectorMove()) {
                    cmd.clipToPossible();
                }

                addStepToMovePath(MoveStepType.RAM);

                ToHitData toHit = new RamAttackAction(currentEntity,
                      target.getTargetType(),
                      target.getId(),
                      target.getPosition()).toHit(game, cmd);
                if (toHit.getValue() != TargetRoll.IMPOSSIBLE &&
                      target instanceof IAero targetAero &&
                      currentlySelectedEntity instanceof IAero attackingEntity) {
                    // Determine how much damage the charger will take.
                    int toAttacker = RamAttackAction.getDamageTakenBy(attackingEntity,
                          (Entity) targetAero,
                          cmd.getSecondFinalPosition(currentlySelectedEntity.getPosition()),
                          cmd.getHexesMoved(),
                          targetAero.getCurrentVelocity());
                    int toDefender = RamAttackAction.getDamageFor(attackingEntity,
                          (Entity) targetAero,
                          cmd.getSecondFinalPosition(currentlySelectedEntity.getPosition()),
                          cmd.getHexesMoved(),
                          targetAero.getCurrentVelocity());

                    // Ask the player if they want to charge.
                    if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.RamDialog.title",
                                target.getDisplayName()),
                          Messages.getString("MovementDisplay.RamDialog.message",
                                toHit.getValueAsString(),
                                Compute.oddsAbove(toHit.getValue(),
                                      currentlySelectedEntity.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                                toHit.getDesc(),
                                toDefender,
                                toHit.getTableDesc(),
                                toAttacker))) {
                        // if they answer yes, charge the target.
                        cmd.getLastStep().setTarget(target);
                        ready();
                    } else {
                        // else clear movement
                        clear();
                    }

                    return;
                }
                // if not valid, tell why
                clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantRam"), toHit.getDesc());
                clear();
                return;
            } else if (gear == MovementDisplay.GEAR_CHARGE) {
                // check if the target is valid
                final Targetable target = chooseTarget(boardViewEvent.getCoords());
                if (currentlySelectedEntity != null && ((target == null) || target.equals(currentlySelectedEntity))) {
                    clientgui.doAlertDialog(Messages.getString(currentlySelectedEntity.isAirborneVTOLorWIGE() ?
                                "MovementDisplay.CantRam" :
                                "MovementDisplay.CantCharge"),
                          Messages.getString("MovementDisplay.NoTarget"));
                    clear();
                    computeMovementEnvelope(currentlySelectedEntity);
                    return;
                }

                // check if it's a valid charge
                ToHitData toHit = null;
                if (target != null) {
                    if (currentlySelectedEntity != null && currentlySelectedEntity.isAirborneVTOLorWIGE()) {
                        toHit = new AirMekRamAttackAction(currentEntity,
                              target.getTargetType(),
                              target.getId(),
                              target.getPosition()).toHit(game, cmd);
                    } else {
                        toHit = new ChargeAttackAction(currentEntity,
                              target.getTargetType(),
                              target.getId(),
                              target.getPosition()).toHit(game, cmd);
                    }
                }

                if (currentlySelectedEntity != null && toHit != null && toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // Determine how much damage the charger will take.
                    int toDefender;
                    int toAttacker = 0;
                    if (currentlySelectedEntity.isAirborneVTOLorWIGE()) {
                        toAttacker = AirMekRamAttackAction.getDamageTakenBy(currentlySelectedEntity,
                              target,
                              cmd.getHexesMoved());
                        toDefender = AirMekRamAttackAction.getDamageFor(currentlySelectedEntity, cmd.getHexesMoved());
                    } else {
                        toDefender = ChargeAttackAction.getDamageFor(
                              currentlySelectedEntity, game.getOptions()
                                    .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE),
                              cmd.getHexesMoved());
                        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
                            Entity te = (Entity) target;
                            toAttacker = ChargeAttackAction.getDamageTakenBy(currentlySelectedEntity,
                                  te,
                                  game
                                        .getOptions()
                                        .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE),
                                  cmd.getHexesMoved());
                        } else if ((target.getTargetType() == Targetable.TYPE_FUEL_TANK) ||
                              (target.getTargetType() == Targetable.TYPE_BUILDING)) {
                            IBuilding bldg = game.getBoard(currentlySelectedEntity).getBuildingAt(moveto);
                            toAttacker = ChargeAttackAction.getDamageTakenBy(currentlySelectedEntity, bldg, moveto);
                        }
                    }

                    String title = "MovementDisplay.ChargeDialog.title";
                    String msg = "MovementDisplay.ChargeDialog.message";
                    if (currentlySelectedEntity.isAirborneVTOLorWIGE()) {
                        title = "MovementDisplay.AirMekRamDialog.title";
                        msg = "MovementDisplay.AirMekRamDialog.message";
                    }
                    // Ask the player if they want to charge.
                    if (clientgui.doYesNoDialog(Messages.getString(title, target.getDisplayName()),
                          Messages.getString(msg,
                                toHit.getValueAsString(),
                                Compute.oddsAbove(toHit.getValue()),
                                toHit.getDesc(),
                                toDefender,
                                toHit.getTableDesc(),
                                toAttacker))) {
                        // if they answer yes, charge the target.
                        cmd.getLastStep().setTarget(target);
                        ready();
                    } else {
                        // else clear movement
                        clear();
                    }
                    return;
                }
                // if not valid, tell why
                if (toHit != null) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantCharge"), toHit.getDesc());
                } else {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantCharge"), "toHit Value Is Null");
                }

                clear();

                if (currentlySelectedEntity != null) {
                    computeMovementEnvelope(currentlySelectedEntity);
                }

                return;
            } else if (gear == MovementDisplay.GEAR_DFA) {
                // check if the target is valid
                final Targetable target = chooseTarget(boardViewEvent.getCoords());
                if ((target == null) || target.equals(currentlySelectedEntity)) {
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantDFA"),
                          Messages.getString("MovementDisplay.NoTarget"));
                    clear();

                    if (currentlySelectedEntity != null) {
                        computeMovementEnvelope(currentlySelectedEntity);
                    }

                    return;
                }

                // check if it's a valid DFA
                ToHitData toHit = DfaAttackAction.toHit(game, currentEntity, target, cmd);
                if (toHit != null && toHit.getValue() != TargetRoll.IMPOSSIBLE) {
                    // if yes, ask them if they want to DFA
                    if (currentlySelectedEntity != null) {
                        // Calculate piloting roll to stay standing after DFA
                        PilotingRollData pilotRoll = currentlySelectedEntity.getBasePilotingRoll(
                              EntityMovementType.MOVE_JUMP);
                        pilotRoll.addModifier(4, Messages.getString("MovementDisplay.DFADialog.dfaModifier"));

                        if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.DFADialog.title",
                                      target.getDisplayName()),
                                Messages.getString("MovementDisplay.DFADialog.message",
                                      toHit.getValueAsString(),
                                      Compute.oddsAbove(toHit.getValue()),
                                      toHit.getDesc(),
                                      DfaAttackAction.getDamageFor(currentlySelectedEntity,
                                            target.isConventionalInfantry()),
                                      toHit.getTableDesc(),
                                      DfaAttackAction.getDamageTakenBy(currentlySelectedEntity),
                                      pilotRoll.getValueAsString(),
                                      Compute.oddsAbove(pilotRoll.getValue()),
                                      pilotRoll.getDesc()))) {
                            // if they answer yes, DFA the target
                            cmd.getLastStep().setTarget(target);
                            ready();
                        } else {
                            // else clear movement
                            clear();
                        }
                    }
                    return;
                }

                if (toHit != null) {
                    // if not valid, tell why
                    clientgui.doAlertDialog(Messages.getString("MovementDisplay.CantDFA"), toHit.getDesc());
                }

                clear();
                return;
            }
            updateDonePanel();
            updateProneButtons();
            updateChaffButton();
            updateRACButton();
            updateSearchlightButton();
            updateLoadButtons();
            updateElevationButtons();
            updateTakeOffButtons();
            updateLandButtons();
            updateEvadeButton();
            updateBootleggerButton();
            updateShutdownButton();
            updateStartupButton();
            updateSelfDestructButton();
            updateTraitorButton();
            updateFlyOffButton();
            updateLaunchButton();
            updateDropButton();
            updateConvertModeButton();
            updateRecklessButton();
            updateBraceButton();
            updateHoverButton();
            updateManeuverButton();
            updateSpeedButtons();
            updateThrustButton();
            updateRollButton();
            updateTurnButton();
            updateTakeCoverButton();
            updateLayMineButton();
            checkFuel();
            checkOOC();
            checkAtmosphere();
        }
    }

    private void updateTakeCoverButton() {
        final GameOptions gOpts = game.getOptions();
        boolean isInfantry = (currentEntity() instanceof Infantry);

        // Infantry - Taking Cover
        if (isInfantry && gOpts.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_TAKE_COVER)) {
            // Determine the current position of the infantry
            Coords pos;
            int elevation;
            if (cmd == null) {
                pos = currentEntity().getPosition();
                elevation = currentEntity().getElevation();
            } else {
                pos = cmd.getFinalCoords();
                elevation = cmd.getFinalElevation();
            }
            getBtn(MoveCommand.MOVE_TAKE_COVER).setEnabled(Infantry.hasValidCover(game, pos, elevation));
        } else {
            getBtn(MoveCommand.MOVE_TAKE_COVER).setEnabled(false);
        }
    }

    private synchronized void updateChaffButton() {
        Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            return;
        }

        setChaffEnabled(currentEntity.hasWorkingMisc(F_CHAFF_POD));
    }

    private synchronized void updateProneButtons() {
        final Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            setGetUpEnabled(false);
            setGoProneEnabled(false);
            setHullDownEnabled(false);
            return;
        }

        boolean isMek = currentEntity instanceof Mek;

        if (cmd.getFinalProne()) {
            setGetUpEnabled(!currentEntity.isImmobile() && !currentEntity.isStuck());
            setGoProneEnabled(false);
            setHullDownEnabled(true);
        } else if (cmd.getFinalHullDown()) {
            if (isMek) {
                setGetUpEnabled(!currentEntity.isImmobile() && !currentEntity.isStuck() && !((Mek) currentEntity).cannotStandUpFromHullDown());
            } else {
                setGetUpEnabled(!currentEntity.isImmobile() && !currentEntity.isStuck());
            }
            setGoProneEnabled(!currentEntity.isImmobile() && isMek && !currentEntity.isStuck());
            setHullDownEnabled(false);
        } else {
            setGetUpEnabled(false);
            setGoProneEnabled(!currentEntity.isImmobile() &&
                  isMek &&
                  !currentEntity.isStuck() &&
                  !(getBtn(MoveCommand.MOVE_GET_UP).isEnabled()));
            if (!(currentEntity instanceof Tank) &&
                  !(currentEntity instanceof QuadVee && currentEntity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
                setHullDownEnabled(currentEntity.canGoHullDown());
            } else {
                // So that the vehicle can move and go hull-down, we have to check if it's moved into a fortified
                // position
                if (cmd.getLastStep() != null) {
                    boolean hullDownEnabled = game
                          .getOptions()
                          .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN);
                    Hex occupiedHex = game.getBoard(currentEntity)
                          .getHex(cmd.getLastStep().getPosition());
                    boolean fortifiedHex = occupiedHex.containsTerrain(Terrains.FORTIFIED);
                    setHullDownEnabled(hullDownEnabled && fortifiedHex);
                } else {
                    // If there's queued up movement, we can call the canGoHullDown() method in the Tank class.
                    setHullDownEnabled(currentEntity.canGoHullDown());
                }

            }
        }
    }

    private void updateRACButton() {
        final Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            return;
        }
        isUnJammingRAC = false;
        GameOptions opts = game.getOptions();
        setUnjamEnabled(currentEntity.canUnjamRAC() &&
              ((gear == MovementDisplay.GEAR_LAND) ||
                    (gear == MovementDisplay.GEAR_TURN) ||
                    (gear == MovementDisplay.GEAR_BACKUP)) &&
              ((cmd.getMpUsed() <= currentEntity.getWalkMP()) ||
                    (cmd.getLastStep().isOnlyPavementOrRoad() &&
                          (cmd.getMpUsed() <= (currentEntity.getWalkMP() + 1)))) &&
              !(opts.booleanOption(OptionsConstants.ADVANCED_TAC_OPS_TANK_CREWS) &&
                    (cmd.getMpUsed() > 0) &&
                    (currentEntity instanceof Tank) &&
                    (currentEntity.getCrew().getSize() < 2)));
    }

    private void updateSearchlightButton() {
        final Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            return;
        }
        boolean isNight = game
              .getPlanetaryConditions()
              .getLight()
              .isDuskOrFullMoonOrMoonlessOrPitchBack();
        setSearchlightEnabled(isNight && currentEntity.hasSearchlight() && !cmd.contains(MoveStepType.SEARCHLIGHT),
              currentEntity.isUsingSearchlight());
    }

    private synchronized void updateElevationButtons() {
        final Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            return;
        }

        if (currentEntity.isAirborne()) {
            // then use altitude not elevation
            setRaiseEnabled(currentEntity.canGoUp(cmd.getFinalAltitude(), cmd.getFinalCoords(), cmd.getFinalBoardId()));
            setLowerEnabled(currentEntity.canGoDown(cmd.getFinalAltitude(), cmd.getFinalCoords(), cmd.getFinalBoardId()));
            return;
        }
        // WiGEs (and LAMs and glider ProtoMeks) cannot go up if they've used ground movement.
        if (currentEntity.getMovementMode().isWiGE() &&
              !currentEntity.isAirborneVTOLorWIGE() &&
              (cmd.getMpUsed() > 0) &&
              !cmd.contains(MoveStepType.UP)) {
            setRaiseEnabled(false);
        } else {
            setRaiseEnabled(currentEntity.canGoUp(cmd.getFinalElevation(), cmd.getFinalCoords(), cmd.getFinalBoardId()));
        }
        setLowerEnabled(currentEntity.canGoDown(cmd.getFinalElevation(), cmd.getFinalCoords(), cmd.getFinalBoardId()));
    }

    private synchronized void updateTakeOffButtons() {
        if ((null != cmd) && (cmd.length() > 0)) {
            // you can't take off if you have already moved
            setTakeOffEnabled(false);
            setVTakeOffEnabled(false);
            return;
        }

        final Entity currentEntity = currentEntity();
        if ((currentEntity instanceof IAero aero) && currentEntity.isAero() && !currentEntity.isAirborne() && !currentEntity.isShutDown()
              && (usingAeroOnGroundMovement() || hasAtmosphericMapForLiftOff(game, currentEntity))) {
            setTakeOffEnabled(aero.canTakeOffHorizontally());
            setVTakeOffEnabled(aero.canTakeOffVertically());
        } else {
            setTakeOffEnabled(false);
            setVTakeOffEnabled(false);
        }
    }

    private boolean usingAeroOnGroundMovement() {
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_GROUND_MOVE);
    }

    /**
     * @return True when there is a position on an existing atmospheric map that corresponds to the entity's current
     *       ground map. TW p.88
     */
    public static boolean hasAtmosphericMapForLiftOff(IGame game, Entity entity) {
        Board groundBoard = game.getBoard(entity);
        return (groundBoard != null) && (game.getEnclosingBoard(groundBoard) != null)
              && game.getEnclosingBoard(groundBoard).isLowAltitude();
    }

    private synchronized void updateLandButtons() {
        setLandEnabled(false);
        setVLandEnabled(false);

        if ((cmd == null) || (cmd.length() > 0)) {
            return;
        }

        Entity selectedEntity = currentEntity();
        if ((selectedEntity == null) || !selectedEntity.isAero() || !(selectedEntity instanceof IAero aero)) {
            return;
        }

        // With aero on ground movement, only allow landing on the ground map
        if (usingAeroOnGroundMovement() && !game.isOnGroundMap(selectedEntity)) {
            return;
        }

        // Without aero on ground movement, allow landing when over a ground map hex on an atmospheric map
        if (!usingAeroOnGroundMovement()
              && (!game.hasBoardLocationOf(selectedEntity)
              || !selectedEntity.isAirborne()
              || !game.isOnAtmosphericMap(selectedEntity)
              || !game.getBoard(selectedEntity).getEmbeddedBoardHexes().contains(finalPosition()))) {
            return;
        }

        if (selectedEntity.isAirborne() && (altitudeAboveTerrain(selectedEntity) == 1)) {
            setLandEnabled(aero.canLandHorizontally());
            setVLandEnabled(aero.canLandVertically());
        }
    }

    private int altitudeAboveTerrain(Entity aero) {
        int terrainCeiling = 0;
        Hex hex = game.getHexOf(aero);
        if (hex != null) {
            terrainCeiling = hex.ceilingAltitude(game.isOnAtmosphericMap(aero));
        }
        return aero.getAltitude() - terrainCeiling;
    }

    private void updateRollButton() {
        final Entity currentEntity = currentEntity();
        if ((currentEntity == null) || !currentEntity.isAero()) {
            return;
        }

        setRollEnabled(true);

        if (!game.getBoard(currentEntity).isSpace()) {
            setRollEnabled(false);
        }

        if (cmd.contains(MoveStepType.ROLL)) {
            setRollEnabled(false);
        }
    }

    private void updateHoverButton() {
        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        if (currentEntity.isAero()) {
            if (!((IAero) currentEntity).isVSTOL()) {
                return;
            }
            if (game.getBoard(currentEntity).isSpace()) {
                return;
            }
        } else if (!(currentEntity instanceof ProtoMek) &&
              !(currentEntity instanceof LandAirMek && (currentEntity.getConversionMode() == LandAirMek.CONV_MODE_AIR_MEK)) &&
              (currentEntity.getAltitude() <= 3)) {
            return;
        }

        setHoverEnabled(!cmd.contains(MoveStepType.HOVER));
    }

    private void updateThrustButton() {
        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        if (!currentEntity.isAero()) {
            return;
        }

        // only allow thrust if there is thrust left to spend
        int mpUsed = 0;
        MoveStep last = cmd.getLastStep();
        if (null != last) {
            mpUsed = last.getMpUsed();
        }

        setThrustEnabled(mpUsed < currentEntity.getRunMP());
    }

    private synchronized void updateSpeedButtons() {
        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        if (!currentEntity.isAero()) {
            return;
        }

        IAero a = (IAero) currentEntity;

        // only allow acceleration and deceleration if the cmd is empty or the
        // last step was
        // acc/dec

        setAccEnabled(false);
        setDecEnabled(false);
        MoveStep last = cmd.getLastStep();
        // figure out implied velocity, so you can't decelerate below zero
        int currentVelocity = a.getCurrentVelocity();
        int nextVelocity = a.getNextVelocity();
        if (null != last) {
            currentVelocity = last.getVelocity();
            nextVelocity = last.getVelocityN();
        }

        if (null == last) {
            setAccEnabled(true);
            if (currentVelocity > 0) {
                setDecEnabled(true);
            }
        } else if (last.getType() == MoveStepType.ACC) {
            setAccEnabled(true);
        } else if ((last.getType() == MoveStepType.DEC) && (currentVelocity > 0)) {
            setDecEnabled(true);
        }

        // if the aero has failed a maneuver this turn, then don't allow
        if (a.didFailManeuver()) {
            setAccEnabled(false);
            setDecEnabled(false);
        }

        // if accelerate/decelerate at the end of last turn then disable
        if (a.didAccLast()) {
            setAccEnabled(false);
            setDecEnabled(false);
        }

        // allow to accelerate/decelerate next if acceleration/deceleration hasn't been used
        setAccNEnabled(false);
        setDecNEnabled(false);
        if (Stream.of(MoveStepType.ACC, MoveStepType.DEC, MoveStepType.DECELERATION)
              .noneMatch(moveStepType -> cmd.contains(moveStepType))) {
            setAccNEnabled(true);
        }

        if (!cmd.contains(MoveStepType.ACC) &&
              !cmd.contains(MoveStepType.DEC) &&
              !cmd.contains(MoveStepType.ACCELERATION) &&
              (nextVelocity > 0)) {
            setDecNEnabled(true);
        }

        // acc/dec next needs to be disabled if acc/dec used before a failed
        // maneuver
        if (a.didFailManeuver() && a.didAccDecNow()) {
            setDecNEnabled(false);
            setAccNEnabled(false);
        }

        // Disable accelerate/decelerate if a jumpship has changed facing
        if ((a instanceof Jumpship) &&
              ((Jumpship) a).hasStationKeepingDrive() &&
              (cmd.contains(MoveStepType.TURN_LEFT) || cmd.contains(MoveStepType.TURN_RIGHT))) {
            setDecNEnabled(false);
            setAccNEnabled(false);
        }

        // if in the atmosphere, limit acceleration to 2x safe thrust
        if (!game.getBoard(currentEntity).isSpace() && (currentVelocity == (2 * currentEntity.getWalkMP()))) {
            setAccEnabled(false);
        }
        // velocity next will get halved before the next turn so allow up to 4 times
        if (!game.getBoard(currentEntity).isSpace() && (nextVelocity == (4 * currentEntity.getWalkMP()))) {
            setAccNEnabled(false);
        }

        // if this is a teleoperated missile, then it can't decelerate no matter what
        if (a instanceof TeleMissile) {
            setDecEnabled(false);
            setDecNEnabled(false);
        }
    }

    private void updateFlyOffButton() {
        final Entity currentEntity = currentEntity();

        // Aerospace should be able to fly off if they reach a border hex with velocity remaining and facing the
        // right direction, OR if at altitude 10 they can climb out
        if ((currentEntity == null) || !currentEntity.isAero() || !currentEntity.isAirborne()) {
            setFlyOffEnabled(false);
            return;
        }

        IAero a = (IAero) currentEntity;
        MoveStep step = cmd.getLastStep();
        Coords position = currentEntity.getPosition();
        int facing = currentEntity.getFacing();
        int altitude = currentEntity.getAltitude();

        int velocityLeft = a.getCurrentVelocity();
        if (step != null) {
            position = step.getPosition();
            facing = step.getFacing();
            velocityLeft = step.getVelocityLeft();
            altitude = step.getAltitude();
        }

        final Board board = game.getBoard(currentEntity);

        // Check if at altitude 10 - can climb out of atmosphere (requires both return flyover and climb out options)
        boolean canClimbOut = altitude == 10 && board.isGround()
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_CLIMB_OUT);

        // Check if can fly off edge (calculate before using)
        boolean canFlyOffEdge = false;

        // for spheroids in atmosphere we just need to check being on the edge
        if (a.isSpheroid() && !board.isSpace()) {
            canFlyOffEdge = (position != null) &&
                  (currentEntity.getWalkMP() > 0) &&
                  ((position.getX() == 0) ||
                        (position.getX() == (board.getWidth() - 1)) ||
                        (position.getY() == 0) ||
                        (position.getY() == (board.getHeight() - 1)));
        } else if (position != null) {
            // for all aerodynes and spheroids in space, it is more complicated - the nose of the aircraft must be facing
            // in the right direction, and there must be velocity remaining
            boolean evenX = (position.getX() % 2) == 0;
            canFlyOffEdge = (velocityLeft > 0) &&
                  (((position.getX() == 0) && ((facing == 5) || (facing == 4))) ||
                        ((position.getX() == (board.getWidth() - 1)) &&
                              ((facing == 1) || (facing == 2))) ||
                        ((position.getY() == 0) &&
                              ((facing == 1) || (facing == 5) || (facing == 0)) &&
                              evenX) ||
                        ((position.getY() == 0) && (facing == 0)) ||
                        ((position.getY() == (board.getHeight() - 1)) &&
                              ((facing == 2) || (facing == 3) || (facing == 4)) &&
                              !evenX) ||
                        ((position.getY() == (board.getHeight() - 1)) && (facing == 3)));
        }

        // Determine button state and label
        if (canClimbOut && !canFlyOffEdge) {
            // Only climb out available - show "Climb Out" button
            setFlyOffEnabled(true, true);
        } else if (canClimbOut && canFlyOffEdge) {
            // Both options available - show "Fly Off" button, dialog will offer choice
            setFlyOffEnabled(true, false);
        } else if (canFlyOffEdge) {
            // Only fly off edge available
            setFlyOffEnabled(true, false);
        } else {
            setFlyOffEnabled(false);
        }
    }

    private void updateLaunchButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        setLaunchEnabled(!currentEntity.getLaunchableFighters().isEmpty() ||
              !currentEntity.getLaunchableSmallCraft().isEmpty() ||
              !currentEntity.getLaunchableDropships().isEmpty());
    }

    /**
     * @param bay          Instance
     * @param droppedUnits Set of unit ids of entities already dropped this turn.
     *
     * @return true if there are available droppable units in this bay
     */
    private boolean checkBayDropEnable(Bay bay, Set<Integer> droppedUnits) {
        // If this bay has unloaded more units this turn than
        // it has doors* for, we should move on
        // *(excluding Infantry, see StratOps pg. 20)
        int doorsEligibleForDrop = bay.getCurrentDoors();
        List<Entity> droppableUnits = bay.getDroppableUnits();
        boolean infantryTransporter = (bay instanceof InfantryTransporter);

        // No units == no drops
        if (droppableUnits.isEmpty()) {
            return false;
        }

        // If we have droppable units, and we haven't dropped any, let's enable the button. doorsEligibleForDrop can
        // be 0 even before dropping units if it's damaged.
        if (doorsEligibleForDrop > 0 && droppedUnits.isEmpty()) {
            return true;
        }

        // If the current entity has any viable bays with droppable units that aren't already dropping, activate the
        // button.
        boolean hasDroppableUnit = false;
        for (Entity droppableUnit : droppableUnits) {
            if (infantryTransporter || doorsEligibleForDrop > 0) {
                if (droppedUnits.contains(droppableUnit.getId())) {
                    // Infantry don't count against door usage
                    if (!droppableUnit.isInfantry()) {
                        doorsEligibleForDrop--;
                    }
                } else {
                    // Cannot set the button enabled yet,
                    // need to make sure we consider every
                    // unit in the bay that's already dropped
                    hasDroppableUnit = true;
                }
            }
        }

        return hasDroppableUnit;
    }

    /**
     * @param compartment  Instance
     * @param droppedUnits Set of unit ids of entities already dropped this turn.
     *
     * @return true if there are available droppable units in this compartment
     */
    private boolean checkCompartmentDropEnable(InfantryCompartment compartment, Set<Integer> droppedUnits) {
        List<Entity> droppableUnits = compartment.getDroppableUnits();
        return droppableUnits.stream().map(Entity::getId).anyMatch(u -> !droppedUnits.contains(u));
    }

    private void updateDropButton() {
        final Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            return;
        }

        if (currentEntity.isAirborne() && !currentEntity.getDroppableUnits().isEmpty()) {
            Set<Integer> droppedUnits = cmd.getDroppedUnits();
            boolean setEnabled = false;

            // check bays and compartments
            for (Transporter t : currentEntity.getTransports()) {
                if (t instanceof Bay tBay) {
                    setEnabled = checkBayDropEnable(tBay, droppedUnits);
                } else if (t instanceof InfantryCompartment tCompartment) {
                    setEnabled = checkCompartmentDropEnable(tCompartment, droppedUnits);
                }
                setDropEnabled(setEnabled);
                // Only need one viable drop bay/compartment to enable the button
                if (setEnabled) {
                    return;
                }
            }
        }
        setDropEnabled(false);
    }

    private void updateEvadeButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_EVADE)) {
            return;
        }

        if (!((currentEntity instanceof Mek) || (currentEntity instanceof Tank))) {
            return;
        }

        setEvadeEnabled((cmd.getLastStepMovementType() != EntityMovementType.MOVE_JUMP) &&
              (cmd.getLastStepMovementType() != EntityMovementType.MOVE_SPRINT));
    }

    private void updateBootleggerButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        if (!game
              .getOptions()
              .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS)) {
            return;
        }

        if (!(currentEntity instanceof Tank || currentEntity instanceof QuadVee)) {
            return;
        }

        if (currentEntity.getMovementMode() != EntityMovementMode.WHEELED &&
              currentEntity.getMovementMode() != EntityMovementMode.HOVER &&
              currentEntity.getMovementMode() != EntityMovementMode.VTOL) {
            return;
        }

        setBootleggerEnabled(cmd.getLastStep() != null && cmd.getLastStep().getNStraight() >= 3);
    }

    private void updateShutdownButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        if (currentEntity instanceof Infantry) {
            return;
        }

        setShutdownEnabled(!currentEntity.isManualShutdown() && !currentEntity.isStartupThisPhase());
    }

    private void updateStartupButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        if (currentEntity instanceof Infantry) {
            return;
        }

        setStartupEnabled(currentEntity.isManualShutdown() && !currentEntity.isShutDownThisPhase());
    }

    private void updateSelfDestructButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        if (!game
              .getOptions()
              .booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SELF_DESTRUCT)) {
            return;
        }

        if (currentEntity instanceof Infantry) {
            return;
        }

        setSelfDestructEnabled(currentEntity.hasEngine() &&
              currentEntity.getEngine().isFusion() &&
              !currentEntity.getSelfDestructing() &&
              !currentEntity.getSelfDestructInitiated());
    }

    private void updateTraitorButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        setTraitorEnabled(true);
    }

    private void updateConvertModeButton() {
        // Issue #5280 NPE - make sure the move path is valid, and the last step isn't null. MovePath::getLastStep()
        // can return null.
        if ((cmd != null) && (cmd.getLastStep() != null)) {
            if (cmd.length() > 0 && cmd.getLastStep().getType() != MoveStepType.CONVERT_MODE) {
                setModeConvertEnabled(false);
                return;
            }
        }

        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            setModeConvertEnabled(false);
            return;
        }

        if (currentEntity instanceof LandAirMek) {
            boolean canConvert = false;
            for (int i = 0; i < 3; i++) {
                if (i != currentEntity.getConversionMode() && ((LandAirMek) currentEntity).canConvertTo(currentEntity.getConversionMode(), i)) {
                    canConvert = true;
                }
            }
            if (!canConvert) {
                setModeConvertEnabled(false);
                return;
            }
        } else if (!((currentEntity instanceof QuadVee) || ((currentEntity instanceof Mek) && ((Mek) currentEntity).hasTracks()))) {
            setModeConvertEnabled(false);
            return;
        }

        Hex currHex = game.getBoard(currentEntity).getHex(currentEntity.getPosition());
        if (currHex.containsTerrain(Terrains.WATER) && (currentEntity.getElevation() < 0)) {
            setModeConvertEnabled(false);
            return;
        }

        if ((currentEntity instanceof LandAirMek) && currentEntity.isGyroDestroyed()) {
            setModeConvertEnabled(false);
            return;
        }

        if ((currentEntity instanceof QuadVee) && (((QuadVee) currentEntity).conversionCost() > currentEntity.getRunMP())) {
            setModeConvertEnabled(false);
            return;
        }

        setModeConvertEnabled(true);
    }

    private void updateRecklessButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        if (currentEntity.isAirborne()) {
            setRecklessEnabled(false);
        }

        if (currentEntity instanceof ProtoMek) {
            setRecklessEnabled(false);
        } else {
            setRecklessEnabled((null == cmd) || (cmd.length() == 0));
        }
    }

    private void updateBraceButton() {
        if (null == currentEntity()) {
            return;
        }

        MovePath movePath = cmd;
        if (null == movePath) {
            movePath = new MovePath(game, currentEntity());
        }

        setBraceEnabled(!movePath.contains(MoveStepType.BRACE) &&
              movePath.isValidPositionForBrace(movePath.getFinalCoords(), finalBoardId(),
                    movePath.getFinalFacing()));
    }

    private void updateManeuverButton() {
        final Entity currentEntity = currentEntity();

        if (null == currentEntity) {
            return;
        }

        if (game.getBoard(currentEntity).isSpace()) {
            return;
        }

        if (!currentEntity.isAirborne()) {
            return;
        }

        if (!currentEntity.isAero()) {
            return;
        }

        IAero a = (IAero) currentEntity;

        if (a.isSpheroid()) {
            return;
        }

        if (a.didFailManeuver()) {
            setManeuverEnabled(false);
        }

        setManeuverEnabled((null == cmd) || !cmd.contains(MoveStepType.MANEUVER));
    }

    private void updateStrafeButton() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_VTOL_STRAFING)) {
            return;
        }

        setStrafeEnabled(currentEntity() instanceof VTOL);
    }

    private void updateBombButton() {
        MoveStep lastStep = cmd.getLastStep();
        if ((lastStep == null) && !currentEntity().isAirborneVTOLorWIGE()) {
            setBombEnabled(false);
            return;
        }

        if (lastStep != null && lastStep.getClearance() <= 0) {
            setBombEnabled(false);
            return;
        }

        if (currentEntity().isBomber()
              && ((currentEntity() instanceof LandAirMek)
              || game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_VTOL_ATTACKS))
              && ((IBomber) currentEntity()).getBombPoints() > 0) {
            setBombEnabled(true);
        }
    }

    /**
     * Updates the status of the Load, Unload, Mount, Tow and Disconnect buttons.
     */
    private synchronized void updateLoadButtons() {
        updateLoadButton();
        updateUnloadButton();
        updateTowingButtons();
        updateMountButton();
        updatePickupCargoButton();
        updateDropCargoButton();
    }

    /** Updates the status of the "pickup cargo" button */
    private void updatePickupCargoButton() {
        final Entity currentEntity = currentEntity();
        // there has to be an entity, objects are on the ground,
        // the entity can pick them up
        if ((currentEntity == null) ||
              ((game.getGroundObjects(finalPosition(), currentEntity).isEmpty())
                    && (game.getEntitiesVector(finalPosition())
                    .stream()
                    .filter(currentEntity::canPickupCarryableObject)
                    .toList()
                    .isEmpty())) ||
              ((cmd.getLastStep() != null) && (cmd.getLastStep().getType() == MoveStepType.PICKUP_CARGO))) {
            setPickupCargoEnabled(false);
            return;
        }

        setPickupCargoEnabled(true);
    }

    /** Updates the status of the "drop cargo" button */
    private void updateDropCargoButton() {
        final Entity currentlySelectedEntity = currentEntity();
        // there has to be an entity, objects are on the ground, the entity can pick them up
        if ((currentlySelectedEntity == null)
              || (currentlySelectedEntity.getCarriedObjects().isEmpty()
              && currentlySelectedEntity.getTransports()
              .stream()
              .filter(t -> t instanceof ExternalCargo)
              .flatMap(t -> ((ExternalCargo) t).getCarryables().stream())
              .toList().isEmpty())) {
            setDropCargoEnabled(false);
            return;
        }

        setDropCargoEnabled(true);
    }

    /** Updates the status of the Load button. */
    private void updateLoadButton() {
        final Entity currentEntity = currentEntity();
        if ((currentEntity == null) || (currentEntity.getWalkMP() <= 0 && !currentEntity.isAerospace()) || (currentEntity.isAerospace() && currentEntity.isAirborne())) {
            setLoadEnabled(false);
            return;
        }

        // Different vehicles can load from different areas
        Vector<Entity> candidates = new Vector<Entity>();
        for (Coords coords: Compute.getLoadableCoords(currentEntity, finalPosition(), finalBoardId())) {
            candidates.addAll(game.getEntitiesVector(coords, finalBoardId()));
        }

        final boolean canLoad = candidates
              .stream()
              .filter(other -> !currentEntity.canTow(other.getId()))
              .filter(Entity::isLoadableThisTurn)
              .anyMatch(other -> currentEntity.canLoad(other, true, cmd.getFinalElevation()) &&
                    other.getTargetBay() == UNSET_BAY);
        setLoadEnabled(canLoad);
    }

    /** Updates the status of the Unload button. */
    private void updateUnloadButton() {
        final Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            setUnloadEnabled(false);
            return;
        }

        if ((currentEntity instanceof SmallCraft) || currentEntity.isSupportVehicle()) {
            setUnloadEnabled(!unloadableUnits.isEmpty() && !currentEntity.isAirborne());
            return;
        }

        final boolean legalGear = ((gear == GEAR_LAND) ||
              (gear == GEAR_TURN) ||
              (gear == GEAR_BACKUP) ||
              (gear == GEAR_JUMP));
        final int unloadEl = cmd.getFinalElevation();
        final Hex hex = game.getBoard(currentEntity).getHex(cmd.getFinalCoords());
        boolean canUnloadHere = false;

        // A unit that has somehow exited the map is assumed to be unable to unload
        if (isFinalPositionOnBoard()) {
            canUnloadHere = unloadableUnits.stream()
                  .anyMatch(en -> en.isElevationValid(unloadEl, hex) || (en.getJumpMP() > 0));
            // Zip lines, TO pg 219
            if (game.getOptions().booleanOption(ADVANCED_GROUND_MOVEMENT_TAC_OPS_ZIPLINES) && (currentEntity instanceof VTOL)) {
                canUnloadHere |= unloadableUnits.stream()
                      .filter(Entity::isInfantry)
                      .anyMatch(en -> !((Infantry) en).isMechanized());
            }
            // Glider wings allow infantry to exit VTOLs as if jump infantry (IO p.85)
            if (currentEntity instanceof VTOL) {
                canUnloadHere |= unloadableUnits.stream()
                      .filter(Entity::isInfantry)
                      .anyMatch(en -> ((Infantry) en).canExitVTOLWithGliderWings());
            }
        }
        setUnloadEnabled(legalGear && canUnloadHere && !unloadableUnits.isEmpty());
    }

    private void updateMountButton() {
        final Entity movingEntity = currentEntity();
        if ((movingEntity == null) || (movingEntity instanceof SmallCraft)) {
            setMountEnabled(false);
            return;
        }

        final Coords pos = finalPosition();
        int elev = movingEntity.getElevation();
        int mpUsed = movingEntity.mpUsed;
        if (null != cmd) {
            elev = cmd.getFinalElevation();
            mpUsed = cmd.getMpUsed();
        }
        final boolean canMount = isFinalPositionOnBoard() &&
              !movingEntity.isAirborne() &&
              (mpUsed <= Math.ceil(movingEntity.getWalkMP() / 2.0)) &&
              !Compute.getMountableUnits(movingEntity, pos, finalBoardId(),
                    elev + game.getBoard(movingEntity).getHex(pos).getLevel(),
                    game).isEmpty();
        setMountEnabled(canMount);
    }

    /** Updates the status of the Tow and Disconnect buttons. */
    private void updateTowingButtons() {
        final Entity currentEntity = currentEntity();
        if ((currentEntity == null) || (currentEntity instanceof SmallCraft)) {
            setTowEnabled(false);
            setDisconnectEnabled(false);
            return;
        }

        final boolean legalGear = ((gear == GEAR_LAND) ||
              (gear == GEAR_TURN) ||
              (gear == GEAR_BACKUP) ||
              (gear == GEAR_JUMP));
        final Hex hex = game.getBoard(currentEntity).getHex(cmd.getFinalCoords());
        final int unloadEl = cmd.getFinalElevation();
        final boolean canDropTrailerHere = towedUnits.stream().anyMatch(en -> en.isElevationValid(unloadEl, hex));
        setDisconnectEnabled(legalGear && isFinalPositionOnBoard() && canDropTrailerHere);

        final boolean canTow = currentEntity.getHitchLocations()
              .stream()
              .flatMap(c -> game.getEntitiesVector(c).stream())
              .anyMatch(e -> currentEntity.canTow(e.getId()));
        setTowEnabled(canTow);
    }

    /**
     * @return The end position of the current movement path if there is one, the current position otherwise.
     */
    private Coords finalPosition() {
        return cmd == null ? currentEntity().getPosition() : cmd.getFinalCoords();
    }

    /**
     * @return True when the endpoint of the current movement path is on the board.
     */
    private boolean isFinalPositionOnBoard() {
        return game.getBoard(currentEntity().getBoardId())
              .contains(cmd == null ? currentEntity().getPosition() : cmd.getFinalCoords());
    }

    private int finalBoardId() {
        return cmd == null ? currentEntity().getBoardId() : cmd.getFinalBoardId();
    }

    private int finalFacing() {
        return cmd == null ? currentEntity().getFacing() : cmd.getFinalFacing();
    }

    private void updateLayMineButton() {
        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        if (!currentEntity.canLayMine() || cmd.contains(MoveStepType.LAY_MINE)) {
            setLayMineEnabled(false);
        } else if (currentEntity instanceof BattleArmor) {
            setLayMineEnabled(cmd.getLastStep() == null ||
                  cmd.isJumping() ||
                  cmd.getLastStepMovementType().equals(EntityMovementType.MOVE_VTOL_WALK));
        } else {
            setLayMineEnabled(true);
        }
    }

    private Entity getMountedUnit() {
        Entity currentEntity = currentEntity();
        Entity choice = null;
        Coords pos = currentEntity.getPosition();
        int elev = currentEntity.getElevation();
        if (null != cmd) {
            pos = cmd.getFinalCoords();
            elev = cmd.getFinalElevation();
        }
        Hex hex = game.getBoard(finalBoardId()).getHex(pos);
        if (null != hex) {
            elev += hex.getLevel();
        }

        List<Entity> mountableUnits = Compute.getMountableUnits(currentEntity, pos, finalBoardId(), elev, game);

        // Handle error condition.
        if (mountableUnits.isEmpty()) {
            LOGGER.error("Called getMountedUnits without any mountable units.");
        } else if (mountableUnits.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  Messages.getString("MovementDisplay.MountUnitDialog.message", currentEntity.getShortName()),
                  Messages.getString("MovementDisplay.MountUnitDialog.title"),
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  SharedUtility.getDisplayArray(mountableUnits),
                  null);
            choice = (Entity) SharedUtility.getTargetPicked(mountableUnits, input);
        } else {
            // Only one choice.
            choice = mountableUnits.get(0);
        }

        if (choice != null && !(currentEntity instanceof Infantry)) {
            Vector<Integer> bayChoices = new Vector<>();
            for (Transporter t : choice.getTransports()) {
                if (t.canLoad(currentEntity) && (t instanceof Bay)) {
                    bayChoices.add(((Bay) t).getBayNumber());
                }
            }
            String[] retVal = new String[bayChoices.size()];
            int i = 0;
            for (Integer bn : bayChoices) {
                retVal[i++] = bn.toString() + " (Free Slots: " + (int) choice.getBayById(bn).getUnused() + ")";
            }
            if (bayChoices.size() > 1) {
                String bayString = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                      Messages.getString("MovementDisplay.MountUnitBayNumberDialog.message", choice.getShortName()),
                      Messages.getString("MovementDisplay.MountUnitBayNumberDialog.title"),
                      JOptionPane.QUESTION_MESSAGE,
                      null,
                      retVal,
                      null);
                currentEntity.setTargetBay(MathUtility.parseInt(bayString.substring(0, bayString.indexOf(" "))));
                // We need to update the entity here so that the server knows
                // about our target bay
                clientgui.getClient().sendUpdateEntity(currentEntity);
            } else {
                currentEntity.setTargetBay(UNSET_BAY); // Safety set!
            }
        } else {
            currentEntity.setTargetBay(UNSET_BAY); // Safety set!
        }

        // Return the chosen unit.
        return choice;
    }

    private @Nullable Entity getLoadedUnit() {
        Entity choice;

        Vector<Entity> choices = new Vector<>();
        for (Coords coords: Compute.getLoadableCoords(currentEntity(), finalPosition(), finalBoardId())) {
            for (Entity other : game.getEntitiesVector(coords)) {
                // Only allow selecting units that aren't already getting loaded
                if (other.isLoadableThisTurn() && (currentEntity() != null) && currentEntity().canLoad(other, true,
                      cmd.getFinalElevation()) && (other.getTargetBay() == UNSET_BAY))
                {
                    choices.addElement(other);
                }
            }
        }

        // Handle error condition.
        if (choices.isEmpty()) {
            LOGGER.error("getLoadedUnit called without loadable units.");
            return null;
        }

        // If we have multiple choices, display a selection dialog.
        if (choices.size() > 1) {
            String input = (String) JOptionPane
                  .showInputDialog(clientgui.getFrame(),
                        Messages.getString(
                              "DeploymentDisplay.loadUnitDialog.message",
                              currentEntity().getShortName(),
                              currentEntity().getUnusedString()),
                        Messages.getString("DeploymentDisplay.loadUnitDialog.title"),
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        SharedUtility.getDisplayArray(choices),
                        null);
            choice = (Entity) SharedUtility.getTargetPicked(choices, input); // Add matching notification below
        } else {
            // Only one choice.
            choice = choices.get(0);
            // Notify user
            JOptionPane.showMessageDialog(clientgui.getFrame(),
                  Messages.getString(
                        "DeploymentDisplay.loadUnitToDefault.message",
                        currentEntity().getShortName(),
                        choice.getShortName()),
                  Messages.getString("DeploymentDisplay.loadUnitDialog.title"), JOptionPane.INFORMATION_MESSAGE
            );
        }

        // Handle canceled dialog
        if (choice == null) {
            return null;
        }

        // Safety set, apparently
        choice.setTargetBay(UNSET_BAY);

        List<Integer> bayChoices = new ArrayList<>();
        for (Transporter transporter : currentEntity().getTransports()) {
            if (transporter.canLoad(choice) && (transporter instanceof Bay)) {
                bayChoices.add(((Bay) transporter).getBayNumber());
            }
        }

        if (bayChoices.size() == 1) {
            JOptionPane.showMessageDialog(clientgui.getFrame(),
                  Messages.getString(
                        "MovementDisplay.loadUnitBayNumberDefault.message",
                        currentEntity().getShortName(),
                        choice.getShortName(),
                        String.format("Bay %s", bayChoices.get(0))
                  ),
                  Messages.getString("MovementDisplay.loadUnitBayNumberDialog.title"), JOptionPane.INFORMATION_MESSAGE
            );
            choice.setTargetBay(bayChoices.get(0));
        } else if (bayChoices.size() > 1) {
            String[] bayChoicesArray = new String[bayChoices.size()];
            int i = 0;
            for (Integer bayNumber : bayChoices) {
                bayChoicesArray[i++] = bayNumber.toString()
                      + " (Free Slots: "
                      + (int) currentEntity().getBayById(bayNumber).getUnused()
                      + ")";
            }
            String bayString = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  Messages.getString("MovementDisplay.loadUnitBayNumberDialog.message",
                        currentEntity().getShortName()),
                  Messages.getString("MovementDisplay.loadUnitBayNumberDialog.title"),
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  bayChoicesArray,
                  null);
            // Handle canceled dialog
            if (bayString == null) {
                return null;
            }

            choice.setTargetBay(MathUtility.parseInt(bayString.substring(0, bayString.indexOf(" "))));
            // We need to update the entity here so that the server knows
            // about our target bay
            clientgui.getClient().sendUpdateEntity(choice);
        } else if (choice.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
            bayChoices = new ArrayList<>();
            for (Transporter transporter : currentEntity().getTransports()) {
                if ((transporter instanceof ProtoMekClampMount) && transporter.canLoad(choice)) {
                    bayChoices.add(((ProtoMekClampMount) transporter).isRear() ? 1 : 0);
                }
            }
            if (bayChoices.size() > 1) {
                String[] clampChoicesArray = new String[bayChoices.size()];
                int i = 0;
                for (Integer bayNumber : bayChoices) {
                    clampChoicesArray[i++] = bayNumber > 0 ?
                          Messages.getString("MovementDisplay.loadProtoClampMountDialog.rear") :
                          Messages.getString("MovementDisplay.loadProtoClampMountDialog.front");
                }
                String bayString = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                      Messages.getString("MovementDisplay.loadProtoClampMountDialog.message",
                            currentEntity().getShortName()),
                      Messages.getString("MovementDisplay.loadProtoClampMountDialog.title"),
                      JOptionPane.QUESTION_MESSAGE,
                      null,
                      clampChoicesArray,
                      null);

                if (bayString == null) {
                    // Cancelled out, best to cancel the loading.
                    return null;
                }

                choice.setTargetBay(bayString.equals(Messages.getString(
                      "MovementDisplay.loadProtoClampMountDialog.front")) ? 0 : 1);
                // We need to update the entity here so that the server knows
                // about our target bay
                clientgui.getClient().sendUpdateEntity(choice);
            }
        }

        // Return the chosen unit.
        return choice;
    }

    /**
     * Get the unit (trailer) that the player wants to connect. This method will add the trailer to our local copy of
     * loaded units.
     *
     * @return The <code>Entity</code> that the player wants to tow. This value may be null if there are no eligible
     *       targets
     */
    private Entity getTowedUnit() {
        Entity choice;

        List<Entity> choices = new ArrayList<>();

        // We have to account for the positions of the whole train when looking to add
        // new trailers
        for (Coords pos : currentEntity().getHitchLocations()) {
            for (Entity other : game.getEntitiesVector(pos)) {
                if (currentEntity() != null && currentEntity().canTow(other.getId())) {
                    choices.add(other);
                }
            }
        }

        // Handle error condition.
        if (choices.isEmpty()) {
            LOGGER.debug("Method called without towable units.");
            return null;
        }

        // If we have multiple choices, display a selection dialog.
        if (choices.size() > 1) {
            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  Messages.getString("DeploymentDisplay.towUnitDialog.message", currentEntity().getShortName()),
                  Messages.getString("DeploymentDisplay.towUnitDialog.title"),
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  SharedUtility.getDisplayArray(choices),
                  null);
            choice = (Entity) SharedUtility.getTargetPicked(choices, input);
        } else {
            // Only one choice.
            choice = choices.get(0);
        }

        // Set up the correct hitch/transporter to use
        // We need lots of data about the hitch to store in different places. Save that
        // here
        final class HitchChoice {
            private final int id;
            private final int number;
            private final TankTrailerHitch hitch;

            private HitchChoice(int id, int number, TankTrailerHitch t) {
                this.id = id;
                this.number = number;
                this.hitch = t;
            }

            private int getId() {
                return id;
            }

            private int getNumber() {
                return number;
            }

            private TankTrailerHitch getHitch() {
                return hitch;
            }

            @Override
            public String toString() {
                Entity entity = game.getEntity(id);

                if (entity == null) {
                    return "Unknown Entity";
                }

                // the string should tell the user if the hitch is mounted front or rear
                if (getHitch().getRearMounted()) {
                    return String.format("%s Trailer Hitch #[%d] (rear)", entity.getShortName(), getNumber());
                }
                return String.format("%s Trailer Hitch #[%d] (front)", entity.getShortName(), getNumber());
            }
        }

        // Create a collection to keep my choices in
        List<HitchChoice> hitchChoices = new ArrayList<>();

        // next, set up a list of all the entities in this train
        ArrayList<Entity> thisTrain = new ArrayList<>();
        thisTrain.add(currentEntity());
        for (int id : currentEntity().getAllTowedUnits()) {
            Entity tr = game.getEntity(id);
            thisTrain.add(tr);
        }
        // And store all the valid Hitch transporters that each one has really, there shouldn't be but one per entity.
        // "Towing" front and rear with the tractor in the center isn't going to work well...
        for (Entity entity : thisTrain) {
            for (Transporter transporter : entity.getTransports()) {
                if (transporter.canTow(choice)) {
                    TankTrailerHitch tankTrailerHitch = (TankTrailerHitch) transporter;
                    HitchChoice hitch = new HitchChoice(entity.getId(),
                          entity.getTransports().indexOf(transporter),
                          tankTrailerHitch);
                    hitchChoices.add(hitch);
                }
            }
        }

        // Gah, multiple choice test!
        if (hitchChoices.size() > 1) {
            // Set up a dialog box for the hitch options
            String[] retVal = new String[hitchChoices.size()];
            int i = 0;
            for (HitchChoice hc : hitchChoices) {
                retVal[i++] = hc.toString();
            }
            String selection = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  Messages.getString("MovementDisplay.loadUnitHitchDialog.message", currentEntity().getShortName()),
                  Messages.getString("MovementDisplay.loadUnitHitchDialog.title"),
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  retVal,
                  null);
            HitchChoice hc = null;
            if (selection != null) {
                for (HitchChoice hitchChoice : hitchChoices) {
                    if (selection.equals(hitchChoice.toString())) {
                        hc = hitchChoice;
                        break;
                    }
                }
            }

            if (hc != null) {
                // Set the transporter number in the towed entity from the selection
                choice.setTargetBay(hc.getNumber());
                // and then the Entity id the transporter is attached to...
                choice.setTowedBy(hc.getId());
            }
        } else {
            // and in case there's just one choice...
            choice.setTargetBay(hitchChoices.get(0).getNumber());
            choice.setTowedBy(hitchChoices.get(0).getId());
        }

        // We need to update the entities here so that the server knows
        // about our changes
        currentEntity().setTowing(choice.getId());
        clientgui.getClient().sendUpdateEntity(currentEntity());
        clientgui.getClient().sendUpdateEntity(choice);

        // Return the chosen unit.
        return choice;
    }

    /**
     * Get the unit that the player wants to unload. This method will remove the unit from our local copy of loaded
     * units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This value will not be <code>null</code>.
     */
    private Entity getDisconnectedUnit() {
        Entity currentEntity = currentEntity();
        Entity choice;

        // Handle error condition.
        if (currentEntity.getAllTowedUnits().isEmpty()) {
            LOGGER.debug("Method called without any towed units.");
            return null;
        } else if (currentEntity.getAllTowedUnits().size() > 1) {
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  Messages.getString("MovementDisplay.DisconnectUnitDialog.message",
                        currentEntity.getShortName(),
                        currentEntity.getUnusedString()),
                  Messages.getString("MovementDisplay.DisconnectUnitDialog.title"),
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  SharedUtility.getDisplayArray(towedUnits),
                  null);
            choice = (Entity) SharedUtility.getTargetPicked(towedUnits, input);
        } else {
            // Only one choice.
            choice = towedUnits.get(0);
            towedUnits.remove(0);
        }

        // Return the chosen unit.
        return choice;
    }

    /**
     * Returns the unit the player wants to unload, unless there is only one, in which case this is returned directly.
     * Returns null when the player cancels the dialog or there is no unit to unload.
     * <p>
     * This method will remove the unit from our local copy of loaded units.
     *
     * @return The Entity to unload.
     */
    private @Nullable Entity getUnloadedUnit() {
        Entity currentEntity = currentEntity();
        Entity choice = null;
        if (unloadableUnits.isEmpty()) {
            LOGGER.error("No loaded units");
        } else if (unloadableUnits.size() > 1) {
            // Only show the units we are not already planning to unload
            List<Entity> filteredUnits = unloadableUnits
                  .stream()
                  .filter(entity -> entity.getTargetBay() == UNSET_BAY).collect(Collectors.toList());
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  Messages.getString("MovementDisplay.UnloadUnitDialog.message",
                        currentEntity.getShortName(),
                        currentEntity.getUnusedString()),
                  Messages.getString("MovementDisplay.UnloadUnitDialog.title"),
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  SharedUtility.getDisplayArray(filteredUnits),
                  null);
            choice = (Entity) SharedUtility.getTargetPicked(filteredUnits, input);
        } else {
            // Only one choice.
            choice = unloadableUnits.get(0);
            unloadableUnits.remove(0);
        }

        // Return the chosen unit.
        return choice;
    }

    /**
     * Returns a position to unload a unit into or null if the player cancels the dialog.
     *
     * @param unloaded The unit to unload
     *
     * @return The position to unload to
     */
    private @Nullable Coords getUnloadPosition(Entity unloaded) {
        Entity currentEntity = currentEntity();
        // we need to allow the user to select a hex for offloading
        Coords pos = currentEntity.getPosition();
        int elev = game.getBoard(currentEntity).getHex(pos).getLevel() + currentEntity.getElevation();
        int altitude = 0;

        // Special handling for unloading on the move
        if (null != cmd) {
            pos = cmd.getFinalCoords();
            elev = (currentEntity.isAirborne()) ? 999 : cmd.getFinalElevation();
            altitude = (currentEntity.isAirborne()) ? cmd.getFinalAltitude() : 0;
        }
        // Flying units can unload under themselves or in their hex; note that dropships ignore this.
        List<Coords> ring = new ArrayList<>();
        if (elev > 0 || altitude > 0) {
            ring.add(pos);
        }
        ring.addAll(pos.allAdjacent());

        if (currentEntity instanceof Dropship) {
            ring = pos.allAtDistance(2);
        }

        // ok, now we need to go through the ring and identify available Positions
        ring = Compute.getAcceptableUnloadPositions(ring, finalBoardId(), unloaded, game, elev);

        // If we're a train, eliminate positions held by any unit in the train. You get stacking violation weirdness
        // if this isn't done.
        Set<Coords> toRemove = new HashSet<>();
        if (currentEntity.getTowing() != Entity.NONE) {
            for (int i : currentEntity.getAllTowedUnits()) {
                Entity e = currentEntity.getGame().getEntity(i);
                if (e != null && e.getPosition() != null) {
                    toRemove.add(e.getPosition());
                }
            }
        } else if (currentEntity.getTractor() != Entity.NONE) {
            Entity tractor = currentEntity.getGame().getEntity(currentEntity.getTractor());
            if (tractor != null && tractor.getPosition() != null) {
                toRemove.add(tractor.getPosition());
                for (int i : tractor.getAllTowedUnits()) {
                    Entity e = currentEntity.getGame().getEntity(i);
                    if (e != null && e.getPosition() != null) {
                        toRemove.add(e.getPosition());
                    }
                }
            }
        }
        ring.removeAll(toRemove);

        if (ring.isEmpty()) {
            String title = Messages.getString("MovementDisplay.NoPlaceToUnload.title");
            String body = Messages.getString("MovementDisplay.NoPlaceToUnload.message");
            clientgui.doAlertDialog(title, body);
            return null;
        }
        String[] choices = new String[ring.size()];
        int i = 0;
        for (Coords c : ring) {
            choices[i++] = c.toString();
        }
        String selected = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
              Messages.getString("MovementDisplay.ChooseHex" + ".message", currentEntity.getShortName(), currentEntity.getUnusedString()),
              Messages.getString("MovementDisplay.ChooseHex.title"),

              JOptionPane.QUESTION_MESSAGE,
              null,
              choices,
              null);
        if (selected == null) {
            return null;
        }

        Coords choice = null;
        for (Coords c : ring) {
            if (selected.equals(c.toString())) {
                choice = c;
                break;
            }
        }
        return choice;
    }

    /**
     * @param abandoned - The vessel we're escaping from
     *
     * @return Uses player input to find a legal hex where an EjectedCrew unit can be placed
     */
    private Coords getEjectPosition(Entity abandoned) {
        // we need to allow the user to select a hex for offloading the unit's crew
        Coords pos = abandoned.getPosition();

        // Create a bogus crew entity to use for legal hex calculation
        Entity crew = new EjectedCrew();
        crew.setId(game.getNextEntityId());
        crew.setGame(game);
        int elev = game.getBoard(abandoned).getHex(pos).getLevel() + abandoned.getElevation();
        List<Coords> ring = pos.allAdjacent();
        if (abandoned instanceof Dropship) {
            ring = pos.allAtDistance(2);
        }
        // ok, now we need to go through the ring and identify available Positions
        ring = Compute.getAcceptableUnloadPositions(ring, finalBoardId(), crew, game, elev);
        if (ring.isEmpty()) {
            String title = Messages.getString("MovementDisplay.NoPlaceToEject.title");
            String body = Messages.getString("MovementDisplay.NoPlaceToEject.message");
            clientgui.doAlertDialog(title, body);
            return null;
        }
        String[] choices = new String[ring.size()];
        int i = 0;
        for (Coords c : ring) {
            choices[i++] = c.toString();
        }
        String selected = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
              Messages.getString("MovementDisplay.ChooseEjectHex.message",
                    abandoned.getShortName(), abandoned.getUnusedString()),
              Messages.getString("MovementDisplay.ChooseHex.title"),
              JOptionPane.QUESTION_MESSAGE, null, choices, null);
        if (selected == null) {
            return null;
        }
        Coords choice = null;
        for (Coords c : ring) {
            if (selected.equals(c.toString())) {
                choice = c;
                break;
            }
        }
        return choice;
    }

    /**
     * FIGHTER RECOVERY will be handled differently than loading other units. Namely, it will be an action of the
     * fighter, not the carrier. So the fighter just flies right up to a carrier whose movement is ended and hops on.
     * Need a new function
     */
    private synchronized void updateRecoveryButton() {
        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        // I also want to handle fighter recovery here. If using advanced movement, it is not a function of where
        //  the carrier is but where the carrier will be at the end of its move
        if (currentEntity.isAero()) {
            Coords loadedPos = cmd.getFinalCoords();
            if (game.useVectorMove()) {
                // not where you are, but where you will be
                loadedPos = Compute.getFinalPosition(currentEntity.getPosition(), cmd.getFinalVectors());
            }
            boolean isGood = false;
            for (Entity other : game.getEntitiesVector(loadedPos)) {
                // Is the other unit friendly and not the current entity? must be done with its movement it also must
                // be the same heading and velocity
                if ((other instanceof Aero oa) &&
                      other.isDone() &&
                      other.canLoad(currentEntity) &&
                      (cmd.getFinalFacing() == other.getFacing()) &&
                      !other.isCapitalFighter()) {
                    // now let's check velocity
                    // depends on movement rules
                    if (game.useVectorMove()) {
                        if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                            if (currentEntity instanceof Dropship) {
                                setDockEnabled(true);
                            } else {
                                setRecoverEnabled(true);
                            }
                            isGood = true;

                            // We can stop looking now.
                            break;
                        }
                    } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                        if (currentEntity instanceof Dropship) {
                            setDockEnabled(true);
                        } else {
                            setRecoverEnabled(true);
                        }
                        isGood = true;

                        // We can stop looking now.
                        break;
                    }
                }
                // Nope. Discard it.
            } // Check the next entity in this position.
            if (!isGood) {
                setRecoverEnabled(false);
                setDockEnabled(false);
            }
        }

    }

    /**
     * Joining a squadron - Similar to fighter recovery. You can fly up and join a squadron or another solo fighter
     */
    private synchronized void updateJoinButton() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER)) {
            return;
        }

        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        if (!currentEntity.isCapitalFighter()) {
            return;
        }

        Coords loadedPos = cmd.getFinalCoords();
        if (game.useVectorMove()) {
            // not where you are, but where you will be
            loadedPos = Compute.getFinalPosition(currentEntity.getPosition(), cmd.getFinalVectors());
        }
        boolean isGood = false;
        for (Entity other : game.getEntitiesVector(loadedPos)) {
            // Is the other unit friendly and not the current entity? Must be done with its movement it also must be
            // the same heading and velocity
            if (currentEntity.getOwner().equals(other.getOwner()) &&
                  other.isCapitalFighter() &&
                  other.isDone() &&
                  other.canLoad(currentEntity) &&
                  (cmd.getFinalFacing() == other.getFacing())) {
                // now let's check velocity
                // depends on movement rules
                Aero oa = (Aero) other;
                if (game.useVectorMove()) {
                    // can you do equality with vectors?
                    if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                        setJoinEnabled(true);
                        isGood = true;

                        // We're done looping now...
                        break;
                    }
                } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                    setJoinEnabled(true);
                    isGood = true;

                    // We're done looping now...
                    break;
                }
            }
            // Nope. Discard it.
        } // Check the next entity in this position.
        if (!isGood) {
            setJoinEnabled(false);
        }
    }

    /**
     * Get the unit that the player wants to unload. This method will remove the unit from our local copy of loaded
     * units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getLaunchedUnits() {
        Entity currentEntity = currentEntity();
        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<>();

        Vector<Entity> launchableFighters = currentEntity.getLaunchableFighters();
        Vector<Entity> launchableSmallCraft = currentEntity.getLaunchableSmallCraft();
        List<Entity> launchableDropships = currentEntity.getLaunchableDropships();

        // Handle error condition.
        if ((launchableFighters.isEmpty()) && (launchableSmallCraft.isEmpty()) && (launchableDropships.isEmpty())) {
            LOGGER.error("MovementDisplay#getLaunchedUnits() called without loaded units.");
            return choices;
        }

        // cycle through the fighter bays and then the small craft bays
        int bayNum = 1;
        int i;
        Bay currentBay;
        int doors;
        Vector<Bay> FighterBays = currentEntity.getFighterBays();
        for (i = 0; i < FighterBays.size(); i++) {
            currentBay = FighterBays.elementAt(i);
            Vector<Integer> bayChoices = new Vector<>();
            List<Entity> currentFighters = currentBay.getLaunchableUnits();
            /*
             * We will assume that if more fighters are launched than is safe,
             * that these excess fighters will be distributed equally among
             * available doors
             */
            doors = currentBay.getCurrentDoors();
            if (currentFighters.isEmpty()) {
                bayNum++;
                continue;
            }
            String[] names = new String[currentFighters.size()];
            String question = Messages.getString("MovementDisplay.LaunchFighterDialog.message",
                  currentEntity.getShortName(),
                  doors * 2,
                  bayNum);
            for (int loop = 0; loop < names.length; loop++) {
                names[loop] = currentFighters.get(loop).getShortName();
            }

            boolean doIt = false;
            ChoiceDialog choiceDialog = null;
            while (!doIt) {
                choiceDialog = new ChoiceDialog(clientgui.getFrame(),
                      Messages.getString("MovementDisplay.LaunchFighterDialog.title",
                            currentBay.getTransporterType(),
                            bayNum),
                      question,
                      names);
                choiceDialog.setVisible(true);
                if (choiceDialog.getChoices() == null) {
                    doIt = true;
                    continue;
                }
                int numChoices = choiceDialog.getChoices().length;
                if (needNagForLaunchDoors() && (numChoices > currentBay.getSafeLaunchRate())) {
                    int aerospacePerDoor = numChoices / doors;
                    int remainder = numChoices % doors;
                    // Determine PSRs
                    StringBuilder pilotSkillRolls = new StringBuilder();
                    for (int choice = 0; choice < numChoices; choice++) {
                        int modifier = aerospacePerDoor - 2;
                        if ((choice / aerospacePerDoor) >= (doors - 1)) {
                            modifier += remainder;
                        }
                        modifier += currentFighters.get(choice).getCrew().getPiloting();
                        String damageMsg = Messages.getString("MovementDisplay.LaunchFighterDialog.controlroll",
                              names[choice],
                              modifier);
                        pilotSkillRolls.append("\t").append(damageMsg).append("\n");
                    }
                    String title = Messages.getString("MovementDisplay.areYouSure");
                    String body = Messages.getString("MovementDisplay.ConfirmLaunch") + pilotSkillRolls;
                    doIt = checkNagLaunchDoors(title, body);
                } else {
                    doIt = true;
                }
            }

            if (choiceDialog.getAnswer()) {
                // load up the choices
                int[] unitsLaunched = choiceDialog.getChoices();
                for (int element : unitsLaunched) {
                    bayChoices.add(currentFighters.get(element).getId());
                    // Prompt the player to load passengers aboard small craft
                    Entity en = game.getEntity(currentFighters.get(element).getId());
                    if (en instanceof SmallCraft) {
                        loadPassengerAtLaunch((SmallCraft) en);
                    }
                }
                choices.put(i, bayChoices);
                // now remove them (must be a better way?)
                for (int l = unitsLaunched.length; l > 0; l--) {
                    currentFighters.remove(unitsLaunched[l - 1]);
                }
            }

            bayNum++;
        }
        return choices;
    }

    /**
     * Get the unit that the player wants to unload. This method will remove the unit from our local copy of loaded
     * units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getUndockedUnits() {
        Entity currentlySelectedEntity = currentEntity();
        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<>();

        Vector<Entity> launchableFighters = currentlySelectedEntity.getLaunchableFighters();
        Vector<Entity> launchableSmallCraft = currentlySelectedEntity.getLaunchableSmallCraft();
        List<Entity> launchableDropships = currentlySelectedEntity.getLaunchableDropships();

        // Handle error condition.
        if ((launchableFighters.isEmpty()) && (launchableSmallCraft.isEmpty()) && (launchableDropships.isEmpty())) {
            LOGGER.error("Method called without loaded units.");
        } else {
            // cycle through the docking collars
            int i = 0;
            int collarNum = 1;
            for (DockingCollar collar : currentlySelectedEntity.getDockingCollars()) {
                List<Entity> currentDropships = collar.getLaunchableUnits();
                Vector<Integer> collarChoices = new Vector<>();
                if (!currentDropships.isEmpty()) {
                    String[] names = new String[currentDropships.size()];
                    String question = Messages.getString("MovementDisplay.LaunchDropshipDialog.message",
                          currentlySelectedEntity.getShortName(),
                          1,
                          collarNum);
                    for (int loop = 0; loop < names.length; loop++) {
                        names[loop] = currentDropships.get(loop).getShortName();
                    }

                    boolean doIt = false;
                    ChoiceDialog choiceDialog = new ChoiceDialog(clientgui.getFrame(),
                          Messages.getString("MovementDisplay.LaunchDropshipDialog.title",
                                collar.getTransporterType(),
                                collarNum),
                          question,
                          names);
                    while (!doIt) {
                        choiceDialog = new ChoiceDialog(clientgui.getFrame(),
                              Messages.getString("MovementDisplay.LaunchDropshipDialog.title",
                                    collar.getTransporterType(),
                                    collarNum),
                              question,
                              names);
                        choiceDialog.setVisible(true);
                        if ((choiceDialog.getChoices() != null) && (choiceDialog.getChoices().length > 1)) {
                            ConfirmDialog nag = new ConfirmDialog(clientgui.getFrame(),
                                  Messages.getString("MovementDisplay.areYouSure"),
                                  Messages.getString("MovementDisplay.ConfirmLaunch"),
                                  true);
                            nag.setVisible(true);
                            doIt = nag.getAnswer();
                        } else {
                            doIt = true;
                        }
                    }

                    if (choiceDialog.getAnswer()) {
                        // load up the choices
                        int[] unitsLaunched = choiceDialog.getChoices();
                        for (int element : unitsLaunched) {
                            collarChoices.add(currentDropships.get(element).getId());
                            // Prompt the player to load passengers aboard the launching ship(s)
                            Entity en = game
                                  .getEntity(currentDropships.get(element).getId());
                            if (en instanceof SmallCraft) {
                                loadPassengerAtLaunch((SmallCraft) en);
                            }
                        }
                        choices.put(i, collarChoices);
                        // now remove them (must be a better way?)
                        for (int l = unitsLaunched.length; l > 0; l--) {
                            currentDropships.remove(unitsLaunched[l - 1]);
                        }
                    }
                }
                collarNum++;
                i++;
            }
        }

        return choices;
    }

    /**
     * Worker function that consolidates code for loading DropShips / Small Craft with passengers
     *
     * @param craft The launching entity, which has already been tested to see if it's a small craft
     */
    private void loadPassengerAtLaunch(SmallCraft craft) {
        final Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            LOGGER.error("Cannot load passenger at launch for a null current entity.");
            return;
        }

        int space = getSpace(craft, currentEntity);
        ConfirmDialog takePassenger = new ConfirmDialog(clientgui.getFrame(),
              Messages.getString("MovementDisplay.FillSmallCraftPassengerDialog.Title"),
              Messages.getString("MovementDisplay.FillSmallCraftPassengerDialog.message",
                    craft.getShortName(),
                    space,
                    currentEntity.getShortName() + "'",
                    currentEntity.getNPassenger()),
              false);
        takePassenger.setVisible(true);
        if (takePassenger.getAnswer()) {
            // Move the passengers
            currentEntity.setNPassenger(currentEntity.getNPassenger() - space);
            if (currentEntity instanceof Aero) {
                ((Aero) currentEntity).addEscapeCraft(craft.getExternalIdAsString());
            }
            clientgui.getClient().sendUpdateEntity(currentEntity);
            craft.addPassengers(currentEntity.getExternalIdAsString(), space);
            clientgui.getClient().sendUpdateEntity(craft);
        }
    }

    private static int getSpace(SmallCraft craft, Entity currentEntity) {
        int space = 0;
        for (Bay b : craft.getTransportBays()) {
            if ((b instanceof CargoBay) || (b instanceof InfantryBay) || (b instanceof BattleArmorBay)) {
                // Assume a passenger takes up 0.1 tons per single infantryman weight calculations
                space += (int) Math.round(b.getUnused() / 0.1);
            }
        }

        // Passengers don't 'load' into bays to consume space, so update what's available for anyone already aboard
        space -= (int) Math.round((craft.getTotalOtherCrew() + craft.getTotalPassengers()) * 0.1);
        // Make sure the text displays either the carrying capacity or the number of passengers left aboard
        space = Math.min(space, currentEntity.getNPassenger());
        return space;
    }

    /**
     * Get the unit that the player wants to drop. This method will remove the unit from our local copy of loaded
     * units.
     *
     * @return The <code>Entity</code> that the player wants to unload. This value will not be <code>null</code>.
     */
    private TreeMap<Integer, Vector<Integer>> getDroppedUnits() {
        Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            LOGGER.error("Cannot get dropped units for a null current entity");
            return new TreeMap<>();
        }

        Vector<Entity> droppableUnits = currentEntity.getDroppableUnits();

        if (droppableUnits.isEmpty()) {
            LOGGER.error("Cannot get dropped units when no units are droppable.");
            return new TreeMap<>();
        }

        TreeMap<Integer, Vector<Integer>> choices = new TreeMap<>();
        Set<Integer> alreadyDropped = cmd.getDroppedUnits();
        // cycle through the bays
        int bayNum = 1;
        Vector<Transporter> transporters = currentEntity.getTransports();
        List<Entity> currentUnits;

        for (int i = 0; i < transporters.size(); i++) {
            Transporter currentTransporter = transporters.elementAt(i);
            boolean isInfantryTransporter = false;
            currentUnits = new ArrayList<>();
            Vector<Integer> bayChoices = new Vector<>();
            int doorsEligibleForDrop = 0;

            // Handle infantry first
            if (currentTransporter instanceof InfantryTransporter infantryTransporter) {
                isInfantryTransporter = true;

                // Can't drop infantry from above 8 Altitude
                if (cmd.getFinalAltitude() > 8) {
                    continue;
                }

                for (Entity entity : infantryTransporter.getDroppableUnits()) {
                    if (!alreadyDropped.contains(entity.getId())) {
                        currentUnits.add(entity);
                    }
                }
                // Handle other stuff
            } else if (currentTransporter instanceof Bay currentBay) {
                doorsEligibleForDrop = currentBay.getCurrentDoors();

                for (Entity entity : currentBay.getDroppableUnits()) {
                    // Do account for already-dropped Infantry
                    if (alreadyDropped.contains(entity.getId())) {
                        // But exclude infantry from the count of doors used/usable
                        if (!entity.isInfantry()) {
                            //If a unit is set to drop from a bay, we should reduce our capacity
                            doorsEligibleForDrop--;
                        }
                    } else {
                        currentUnits.add(entity);
                    }
                }
            }

            if (!currentUnits.isEmpty() && (isInfantryTransporter || (doorsEligibleForDrop > 0))) {
                String[] names = new String[currentUnits.size()];
                String question = Messages.getString("MovementDisplay.DropUnitDialog.message",
                      doorsEligibleForDrop,
                      bayNum);
                for (int loop = 0; loop < names.length; loop++) {
                    names[loop] = currentUnits.get(loop).getShortName();
                }
                // If this is an infantry-transporting bay (cargo, Infantry Bay, etc.), no limit on drops
                int max = (isInfantryTransporter) ? -1 : doorsEligibleForDrop;
                ChoiceDialog choiceDialog = new ChoiceDialog(clientgui.getFrame(),
                      Messages.getString("MovementDisplay.DropUnitDialog.title",
                            currentTransporter.getTransporterType(),
                            bayNum),
                      question,
                      names,
                      false,
                      max);
                choiceDialog.setVisible(true);
                if (choiceDialog.getAnswer()) {
                    // load up the choices
                    int[] unitsLaunched = choiceDialog.getChoices();
                    for (int element : unitsLaunched) {
                        bayChoices.add(currentUnits.get(element).getId());
                    }
                    choices.put(i, bayChoices);
                    // now remove them (must be a better way?)
                    for (int l = unitsLaunched.length; l > 0; l--) {
                        currentUnits.remove(unitsLaunched[l - 1]);
                    }
                }
            }
            bayNum++;
        }

        return choices;
    }

    /**
     * get the unit id that the player wants to be recovered by
     */
    private int getRecoveryUnit() {
        final Entity currentEntity = currentEntity();
        List<Entity> choices = new ArrayList<>();

        // collect all possible choices
        Coords loadedPos = cmd.getFinalCoords();
        if (game.useVectorMove()) {
            // not where you are, but where you will be
            loadedPos = Compute.getFinalPosition(currentEntity.getPosition(), cmd.getFinalVectors());
        }
        for (Entity other : game.getEntitiesVector(loadedPos)) {
            // Is the other unit friendly and not the current entity? Must be done with its movement it also must be
            // the same heading and velocity
            if ((other instanceof Aero oa) &&
                  !oa.isOutControlTotal() &&
                  other.isDone() &&
                  other.canLoad(currentEntity) &&
                  currentEntity.isLoadableThisTurn() &&
                  (cmd.getFinalFacing() == other.getFacing())) {
                // now let's check velocity
                // depends on movement rules
                if (game.useVectorMove()) {
                    if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                        choices.add(other);
                    }
                } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                    choices.add(other);
                }
            }
            // Nope. Discard it.
        }

        if (choices.isEmpty()) {
            return NO_UNIT_SELECTED;
        }

        if (choices.size() == 1) {
            if (choices.get(0).mpUsed > 0) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.RecoverSureDialog.title"),
                      Messages.getString("MovementDisplay.RecoverSureDialog.message"))) {
                    return choices.get(0).getId();
                }
            } else {
                return choices.get(0).getId();
            }
            return NO_UNIT_SELECTED;
        }

        String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
              Messages.getString("MovementDisplay.RecoverFighterDialog.message"),
              Messages.getString("MovementDisplay.RecoverFighterDialog.title"),
              JOptionPane.QUESTION_MESSAGE,
              null,
              SharedUtility.getDisplayArray(choices),
              null);
        Entity picked = (Entity) SharedUtility.getTargetPicked(choices, input);

        if (picked != null) {
            // if this unit is thrusting, make sure they are aware
            if (picked.mpUsed > 0) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.RecoverSureDialog.title"),
                      Messages.getString("MovementDisplay.RecoverSureDialog.message"))) {
                    return picked.getId();
                }
            } else {
                return picked.getId();
            }
        }
        return NO_UNIT_SELECTED;
    }

    /**
     * @return the unit id that the player wants to join
     */
    private int getUnitJoined() {
        final Entity currentEntity = currentEntity();
        List<Entity> choices = new ArrayList<>();

        // collect all possible choices
        Coords loadedPos = cmd.getFinalCoords();
        if (game.useVectorMove()) {
            // not where you are, but where you will be
            loadedPos = Compute.getFinalPosition(currentEntity.getPosition(), cmd.getFinalVectors());
        }
        for (Entity other : game.getEntitiesVector(loadedPos)) {
            // Is the other unit friendly and not the current entity? Must be done with its movement it also must be
            // the same heading and velocity
            if ((other instanceof Aero oa) &&
                  !oa.isOutControlTotal() &&
                  other.isDone() &&
                  other.canLoad(currentEntity) &&
                  currentEntity.isLoadableThisTurn() &&
                  (cmd.getFinalFacing() == other.getFacing())) {
                // now let's check velocity
                // depends on movement rules
                if (game.useVectorMove()) {
                    if (Compute.sameVectors(cmd.getFinalVectors(), oa.getVectors())) {
                        choices.add(other);
                    }
                } else if (cmd.getFinalVelocity() == oa.getCurrentVelocity()) {
                    choices.add(other);
                }
            }
            // Nope. Discard it.
        }

        if (choices.isEmpty()) {
            return NO_UNIT_SELECTED;
        }

        if (choices.size() == 1) {
            return choices.get(0).getId();
        }

        String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
              Messages.getString("MovementDisplay.JoinSquadronDialog.message"),
              Messages.getString("MovementDisplay.JoinSquadronDialog.title"),
              JOptionPane.QUESTION_MESSAGE,
              null,
              SharedUtility.getDisplayArray(choices),
              null);
        Entity picked = (Entity) SharedUtility.getTargetPicked(choices, input);
        if (picked != null) {
            return picked.getId();
        }
        return NO_UNIT_SELECTED;
    }

    /**
     * check for out-of-control and adjust buttons
     */
    private void checkOOC() {
        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        if (!currentEntity.isAero()) {
            return;
        }

        IAero a = (IAero) currentEntity;

        if (a.isOutControlTotal() && a.isAirborne()) {
            disableButtons();
            butDone.setEnabled(true);
            updateMoreButton();
            getBtn(MoveCommand.MOVE_NEXT).setEnabled(true);
            setForwardIniEnabled(true);
            if (currentEntity instanceof Aero) {
                setLaunchEnabled(!currentEntity.getLaunchableFighters().isEmpty() ||
                      !currentEntity.getLaunchableSmallCraft().isEmpty() ||
                      !currentEntity.getLaunchableDropships().isEmpty());
            }
        }
    }

    /**
     * Displays the More button if there is a second page of buttons
     */
    private void updateMoreButton() {
        if (numButtonGroups > 1) {
            getBtn(MoveCommand.MOVE_MORE).setEnabled(true);
        }
    }

    /** Checks Aerospace for remaining fuel and adjusts buttons when necessary. */
    private void checkFuel() {
        final Entity currentEntity = currentEntity();
        if ((currentEntity == null) || !currentEntity.isAero()) {
            return;
        }

        IAero aero = (IAero) currentEntity;
        if ((aero.getCurrentFuel() < 1) && aero.requiresFuel()) {
            disableButtons();
            butDone.setEnabled(true);
            getBtn(MoveCommand.MOVE_NEXT).setEnabled(true);
            setForwardIniEnabled(true);
            if (currentEntity instanceof Aero) {
                setLaunchEnabled(!currentEntity.getLaunchableFighters().isEmpty() ||
                      !currentEntity.getLaunchableSmallCraft().isEmpty() ||
                      !currentEntity.getLaunchableDropships().isEmpty());
            }
            updateRACButton();
            updateJoinButton();
            updateRecoveryButton();
        }
    }

    /**
     * check for atmosphere and adjust buttons
     */
    private void checkAtmosphere() {
        final Entity currentEntity = currentEntity();
        if ((currentEntity instanceof IAero aero) && !aero.isSpaceborne()) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            if (aero.isSpheroid() || conditions.getAtmosphere().isLighterThan(Atmosphere.THIN)) {
                getBtn(MoveCommand.MOVE_ACC).setEnabled(false);
                getBtn(MoveCommand.MOVE_DEC).setEnabled(false);
                getBtn(MoveCommand.MOVE_ACCELERATION).setEnabled(false);
                getBtn(MoveCommand.MOVE_DECELERATION).setEnabled(false);
            }
        }
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {
        final Entity currentEntity = currentEntity();

        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.

        // Convert the choices into a List of targets.
        ArrayList<Targetable> targets = new ArrayList<>();
        for (Entity ent : game.getEntitiesVector(pos)) {
            if ((currentEntity == null) || !currentEntity.equals(ent)) {
                targets.add(ent);
            }
        }

        // Is there a building in the hex?
        if (currentEntity != null) {
            IBuilding bldg = game.getBoard(currentEntity).getBuildingAt(pos);
            if (bldg != null) {
                targets.add(new BuildingTarget(pos, game.getBoard(currentEntity), false));
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        } else if (targets.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            choice = TargetChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                  "MovementDisplay.ChooseTargetDialog.title",
                  Messages.getString("MovementDisplay.ChooseTargetDialog.message", pos.getBoardNum()),
                  targets,
                  clientgui,
                  currentEntity());

        }

        return choice;
    }

    private int chooseMineToLay() {
        MineLayingDialog mld = new MineLayingDialog(clientgui.getFrame(), currentEntity());
        mld.setVisible(true);
        return mld.getAnswer() ? mld.getMine() : -1;
    }

    private void dumpBombs() {
        if (!currentEntity().isAero()) {
            return;
        }

        EntityMovementType overallMoveType = EntityMovementType.MOVE_NONE;
        if (null != cmd) {
            overallMoveType = cmd.getLastStepMovementType();
        }
        // bring up a dialog to dump bombs, then make a control roll and report success or failure should update mp
        // available
        int numFighters = currentEntity().getActiveSubEntities().size();
        BombPayloadDialog dumpBombsDialog = new BombPayloadDialog(clientgui.getFrame(),
              Messages.getString("MovementDisplay.BombDumpDialog.title"),
              currentEntity().getBombLoadout(),
              false,
              true,
              -1,
              numFighters);
        dumpBombsDialog.setVisible(true);
        if (dumpBombsDialog.getAnswer()) {
            dumpBombsDialog.getChoices();
            // first make a control roll
            PilotingRollData psr = currentEntity().getBasePilotingRoll(overallMoveType);
            Roll diceRoll = Compute.rollD6(2);
            Report report = new Report(9500);
            report.subject = currentEntity().getId();
            report.add(currentEntity().getDisplayName());
            report.add(psr);
            report.add(diceRoll);
            report.newlines = 0;
            report.indent(1);
            if (diceRoll.getIntValue() < psr.getValue()) {
                report.choose(false);
                String title = Messages.getString("MovementDisplay.DumpingBombs.title");
                String body = Messages.getString("MovementDisplay.DumpFailure.message");
                clientgui.doAlertDialog(title, body);
                // failed the roll, so dump all bombs
                currentEntity().getBombLoadout();
            } else {
                // avoided damage
                report.choose(true);
                String title = Messages.getString("MovementDisplay.DumpingBombs.title");
                String body = Messages.getString("MovementDisplay.DumpSuccessful.message");
                clientgui.doAlertDialog(title, body);
            }
        }

    }

    /**
     * based on maneuver type add the appropriate steps return true if we should redraw the movement data
     */
    private boolean addManeuver(int type) {
        cmd.addManeuver(type);
        switch (type) {
            case ManeuverType.MAN_HAMMERHEAD:
                addStepToMovePath(MoveStepType.YAW, true, true, type);
                return true;
            case ManeuverType.MAN_HALF_ROLL:
                addStepToMovePath(MoveStepType.ROLL, true, true, type);
                return true;
            case ManeuverType.MAN_BARREL_ROLL:
                addStepToMovePath(MoveStepType.DEC, true, true, type);
                return true;
            case ManeuverType.MAN_IMMELMAN:
                gear = MovementDisplay.GEAR_IM_MEL;
                return false;
            case ManeuverType.MAN_SPLIT_S:
                gear = MovementDisplay.GEAR_SPLIT_S;
                return false;
            case ManeuverType.MAN_VIFF:
                if (!currentEntity().isAero()) {
                    return false;
                }
                IAero a = (IAero) currentEntity();
                MoveStep last = cmd.getLastStep();
                int vel = a.getCurrentVelocity();
                if (null != last) {
                    vel = last.getVelocityLeft();
                }
                while (vel > 0) {
                    addStepToMovePath(MoveStepType.DEC, true, true, type);
                    vel--;
                }
                addStepToMovePath(MoveStepType.UP);
                return true;
            case ManeuverType.MAN_SIDE_SLIP_LEFT:
                // If we are on a ground map, slide slip works slightly differently
                // See Total Warfare pg 85
                if (game.getBoard(currentEntity()).isGround()) {
                    for (int i = 0; i < 8; i++) {
                        addStepToMovePath(MoveStepType.LATERAL_LEFT, true, true, type);
                    }
                    for (int i = 0; i < 8; i++) {
                        addStepToMovePath(MoveStepType.FORWARDS, true, true, type);
                    }
                } else {
                    addStepToMovePath(MoveStepType.LATERAL_LEFT, true, true, type);
                }
                return true;
            case ManeuverType.MAN_SIDE_SLIP_RIGHT:
                // If we are on a ground map, slide slip works slightly differently
                // See Total Warfare pg 85
                if (game.getBoard(currentEntity()).isGround()) {
                    for (int i = 0; i < 8; i++) {
                        addStepToMovePath(MoveStepType.LATERAL_RIGHT, true, true, type);
                    }
                    for (int i = 0; i < 8; i++) {
                        addStepToMovePath(MoveStepType.FORWARDS, true, true, type);
                    }
                } else {
                    addStepToMovePath(MoveStepType.LATERAL_RIGHT, true, true, type);
                }
                return true;
            case ManeuverType.MAN_LOOP:
                addStepToMovePath(MoveStepType.LOOP, true, true, type);
                return true;
            default:
                return false;
        }
    }

    private void setStatusBarTextOthersTurn(@Nullable Player player, String s) {
        String playerName = (player != null) ? player.getName() : "Unknown";
        setStatusBarText(Messages.getString("MovementDisplay.its_others_turn", playerName) + s);
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        // On simultaneous phases, each player ending their turn will generate a turn
        // change
        // We want to ignore turns from other players and only listen to events we
        // generated
        // Except on the first turn
        if (game.getPhase().isSimultaneous(game) &&
              (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber()) &&
              (game.getTurnIndex() != 0)) {
            return;
        }

        String s = getRemainingPlayerWithTurns();

        // if all our entities are actually done, don't start up the turn.
        if (game
              .getPlayerEntities(clientgui.getClient().getLocalPlayer(), false)
              .stream()
              .allMatch(Entity::isDone)) {
            setStatusBarTextOthersTurn(e.getPlayer(), s);
            clientgui.bingOthersTurn();
            return;
        }

        if (!game.getPhase().isMovement()) {
            // ignore
            return;
        }

        if (clientgui.getClient().isMyTurn()) {
            // Can the player unload entities stranded on immobile transports?
            if (clientgui.getClient().canUnloadStranded()) {
                unloadStranded();
            } else if (currentEntity == Entity.NONE) {
                setStatusBarText(Messages.getString("MovementDisplay.its_your_turn") + s);
                beginMyTurn();
            }
            clientgui.bingMyTurn();
        } else {
            endMyTurn();
            if ((e.getPlayer() == null) && (game.getTurn() instanceof UnloadStrandedTurn)) {
                setStatusBarText(Messages.getString("MovementDisplay.waitForAnother") + s);
            } else {
                setStatusBarTextOthersTurn(e.getPlayer(), s);
            }
            clientgui.bingOthersTurn();
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (game.getPhase().isLounge()) {
            endMyTurn();
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && !game.getPhase().isMovement()) {
            endMyTurn();
        }

        if (game.getPhase().isMovement()) {
            setStatusBarText(Messages.getString("MovementDisplay.waitingForMovementPhase"));
        }
    }

    private int maxMP(Entity en, int mvMode) {
        int maxMP;
        if (mvMode == GEAR_DFA) {
            maxMP = en.getJumpMP();
        } else if (mvMode == GEAR_JUMP) {
            maxMP = en.getJumpMP();
            if ((en instanceof Mek mek) && (jumpSubGear == GEAR_SUB_MEK_BOOSTERS)) {
                maxMP = mek.getMechanicalJumpBoosterMP();
            }
        } else if (mvMode == GEAR_BACKUP) {
            maxMP = en.getWalkMP();
        } else if ((currentEntity() instanceof Mek) &&
              !(currentEntity() instanceof QuadVee) &&
              (currentEntity().getMovementMode() == EntityMovementMode.TRACKED)) {
            // A non-QuadVee `Mek that is using tracked movement is limited to walking
            maxMP = en.getWalkMP();
        } else {
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_SPRINT)) {
                maxMP = en.getSprintMP();
            } else {
                maxMP = en.getRunMP();
            }
        }

        return maxMP;
    }


    /**
     * Computes all the possible moves for an {@link Entity}. The {@link Entity} can either be a suggested
     * {@link Entity} or the currently selected one.
     *
     * @param suggestion The suggested Entity to use to compute the movement envelope.
     */
    public void computeMovementEnvelope(Entity suggestion) {
        if (suggestion.isAero()) {
            computeAeroMovementEnvelope(suggestion);
        } else {
            computeSimpleMovementEnvelope(suggestion);
        }
    }

    /**
     * Computes all the possible moves for an {@link Entity} in a particular gear. The {@link Entity} can either be a
     * suggested {@link Entity} or the currently selected one. If there is a selected entity (which implies it's the
     * current players turn), then the current gear is used (which is set by the user). If there is no selected entity,
     * then the current gear is invalid, and it defaults to {@link #GEAR_LAND} (standard "walk forward").
     *
     * @param suggestion The suggested Entity to use to compute the movement envelope. If used, the gear will be set to
     *                   {@link #GEAR_LAND}. This takes precedence over the currently selected unit.
     */
    private void computeSimpleMovementEnvelope(Entity suggestion) {
        // do nothing if deactivated in the settings
        if (!GUIP.getMoveEnvelope()) {
            // Issue #5700: Move envelope doesn't clear when turning off move envelopes from the menu or shortcut.
            // this here makes sure to clear it next time this function is called
            clientgui.clearMovementEnvelope();
            return;
        }

        Entity entity = currentEntity();
        int movementGear = gear;

        if ((entity == null) && (suggestion == null)) {
            return;
        } else if (entity == null) {
            entity = suggestion;
            movementGear = GEAR_LAND;
        } else {
            entity = suggestion;
        }

        if (entity.isDone()) {
            return;
        }

        MovePath movePath = new MovePath(game, entity);

        MoveStepType stepType = (movementGear == GEAR_BACKUP) ? MoveStepType.BACKWARDS : MoveStepType.FORWARDS;
        if (movementGear == GEAR_JUMP || movementGear == GEAR_DFA) {
            movePath.addStep(MoveStepType.START_JUMP);
            if (jumpSubGear == GEAR_SUB_MEK_BOOSTERS) {
                movePath.addStep(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER);
            }
        }

        int maxMP = maxMP(entity, movementGear);

        // Create a pathfinder to find possible moves;
        // if aerodyne, use a custom Aero pathfinder.
        ShortestPathFinder shortestPathFinder = getShortestPathFinder(entity, maxMP, stepType);
        shortestPathFinder.run(movePath);

        Map<Coords, MovePath> movePathForEachCoordsMap = shortestPathFinder.getAllComputedPaths();
        Map<Coords, Integer> movementEnvelopeMP = new HashMap<>((int) ((movePathForEachCoordsMap.size() * 1.25) + 1));
        for (Coords coords : movePathForEachCoordsMap.keySet()) {
            var candidateMovePath = movePathForEachCoordsMap.get(coords);
            if (candidateMovePath.isMoveLegal()) {
                movementEnvelopeMP.put(coords, candidateMovePath.countMp(movementGear == GEAR_JUMP));
            }
        }
        clientgui.showMovementEnvelope(entity, movementEnvelopeMP, movementGear);
    }

    private ShortestPathFinder getShortestPathFinder(Entity en, int maxMP, MoveStepType stepType) {
        ShortestPathFinder shortestPathFinder;
        if (en.isAerodyne() && !en.isAeroLandedOnGroundMap()) {
            shortestPathFinder = ShortestPathFinder.newInstanceOfOneToAllAero(maxMP, stepType, game);
        } else {
            shortestPathFinder = ShortestPathFinder.newInstanceOfOneToAll(maxMP, stepType, game);
        }
        return shortestPathFinder;
    }

    /**
     * <b>Important: You should call {@link MovementDisplay#computeMovementEnvelope} instead!</b>
     * <p>
     * Computes possible moves for entities of Aero units. This is similar to the
     * {@link MovementDisplay#computeMovementEnvelope(Entity)} method; however, it uses the {@link MovePath} final
     * velocity to temporarily set unit velocity to draw the move envelope. This method always sets the original entity
     * velocity back to its original.
     * </p>
     *
     * @param entity - Suggested {@link Entity} to use to compute {@link Aero} move envelope.
     */
    public void computeAeroMovementEnvelope(Entity entity) {
        // do nothing if deactivated in the settings
        if (!GUIP.getMoveEnvelope()) {
            // Issue #6880: Move envelope doesn't clear when turning off move envelopes from the menu or shortcut.
            clientgui.clearMovementEnvelope();
            return;
        }

        if ((entity == null) || !(entity.isAero()) || (cmd == null)) {
            return;
        }

        // Increment the entity's delta-v then compute the movement envelope. LAM and Aerospace both implement this
        // interface
        IAero ae = (IAero) entity;
        int currentVelocity = ae.getCurrentVelocity();
        ae.setCurrentVelocity(cmd.getFinalVelocity());

        // Refresh the new velocity envelope on the map.
        try {
            computeSimpleMovementEnvelope(entity);
            updateMove();
        } catch (Exception e) {
            LOGGER.error(e, "An error occurred trying to compute the move envelope for an Aero.");
        } finally {
            // Reset the bird's velocity back to original velocity no-matter-what. It will be updated when the 'move'
            // button is clicked and the move is processed.
            ae.setCurrentVelocity(currentVelocity);
        }
    }

    public void computeModifierEnvelope() {
        Entity currentEntity = currentEntity();
        if (currentEntity == null) {
            return;
        }
        int maxMP = getMaxMP(currentEntity);
        MoveStepType stepType = (gear == GEAR_BACKUP) ? MoveStepType.BACKWARDS : MoveStepType.FORWARDS;
        MovePath movePath = new MovePath(game, currentEntity);
        if (gear == GEAR_JUMP) {
            movePath.addStep(MoveStepType.START_JUMP);
            if (jumpSubGear == GEAR_SUB_MEK_BOOSTERS) {
                movePath.addStep(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER);
            }
        }
        LongestPathFinder lpf = LongestPathFinder.newInstanceOfLongestPath(maxMP, stepType, currentEntity.getGame());
        final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();
        StopConditionTimeout<MovePath> timeoutCondition = new StopConditionTimeout<>(
              timeLimit * 10);
        lpf.addStopCondition(timeoutCondition);
        lpf.run(movePath);
        clientgui.showMovementModifiers(lpf.getLongestComputedPaths());
    }

    private int getMaxMP(Entity currentEntity) {
        int maxMP;
        if (gear == GEAR_JUMP) {
            maxMP = currentEntity.getJumpMP();
        } else if (gear == GEAR_BACKUP) {
            maxMP = currentEntity.getWalkMP();
        } else if (
              (currentEntity instanceof Mek mek) &&
                    !(mek instanceof QuadVee) &&
                    (mek.getMovementMode() == EntityMovementMode.TRACKED)) {
            // A non-QuadVee `Mek that is using tracked movement (or converting to it) is limited to walking
            maxMP = mek.getWalkMP();
        } else {
            maxMP = currentEntity.getRunMP();
        }
        return maxMP;
    }

    private void computeCFWarningHexes(Entity ce) {
        List<BoardLocation> warnList = CollapseWarning.findCFWarningsMovement(game, ce);
        clientgui.showCollapseWarning(warnList);
    }

    //
    // ActionListener
    //
    @Override
    public synchronized void actionPerformed(ActionEvent ev) {
        final Entity entity = currentEntity();
        final String actionCmd = ev.getActionCommand();
        if (actionCmd.equals(MoveCommand.MOVE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(currentEntity));
        }

        if (entity == null) {
            return;
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (!clientgui.getClient().isMyTurn()) {
            // odd...
            return;
        }
        final IGameOptions opts = game.getOptions();
        if (actionCmd.equals(MoveCommand.MOVE_FORWARD_INI.getCmd())) {
            selectNextPlayer();
        } else if (actionCmd.equals(MoveCommand.MOVE_CANCEL.getCmd())) {
            clear();
            computeMovementEnvelope(entity);
            updateMove();
        } else if (ev.getSource().equals(getBtn(MoveCommand.MOVE_MORE))) {
            currentButtonGroup++;
            currentButtonGroup %= numButtonGroups;
            setupButtonPanel();
        } else if (actionCmd.equals(MoveCommand.MOVE_UNJAM.getCmd())) {
            String title = Messages.getString("MovementDisplay.UnjamRAC.title");
            String msg = Messages.getString("MovementDisplay.UnjamRAC.message");
            if ((gear == MovementDisplay.GEAR_JUMP) ||
                  (gear == MovementDisplay.GEAR_CHARGE) ||
                  (gear == MovementDisplay.GEAR_DFA) ||
                  ((cmd.getMpUsed() > entity.getWalkMP()) &&
                        !(cmd.getLastStep().isOnlyPavementOrRoad() &&
                              (cmd.getMpUsed() <= (entity.getWalkMP() + 1)))) ||
                  (opts.booleanOption("tacops_tank_crews") &&
                        (cmd.getMpUsed() > 0) &&
                        (entity instanceof Tank) &&
                        (entity.getCrew().getSize() < 2)) ||
                  (gear == MovementDisplay.GEAR_SWIM) ||
                  (gear == MovementDisplay.GEAR_RAM)) {

                setUnjamEnabled(false);
            } else if (clientgui.doYesNoDialog(title, msg)) {
                addStepToMovePath(MoveStepType.UNJAM_RAC);
                isUnJammingRAC = true;
                ready();
                // If ready() fires, it will call endMyTurn, which sets cen to Entity.NONE. If this doesn't happen,
                // it means that the ready() was cancelled (ie, not all Velocity is spent), if it is canceled, we
                // have to ensure the UNJAM_RAC step is removed; otherwise it can fire multiple times.
                if (currentEntity != Entity.NONE) {
                    cmd.removeLastStep();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_SEARCHLIGHT.getCmd())) {
            addStepToMovePath(MoveStepType.SEARCHLIGHT);
        } else if (actionCmd.equals(MoveCommand.MOVE_WALK.getCmd())) {
            if ((gear == MovementDisplay.GEAR_JUMP) || (gear == MovementDisplay.GEAR_SWIM)) {
                gear = MovementDisplay.GEAR_LAND;
                clear();
            }
            Color walkColor = GUIP.getMoveDefaultColor();
            clientgui.boardViews().forEach(bv -> ((BoardView) bv).setHighlightColor(walkColor));
            gear = MovementDisplay.GEAR_LAND;
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_JUMP.getCmd())) {
            if ((gear != MovementDisplay.GEAR_JUMP) &&
                  !((cmd.getLastStep() != null) &&
                        cmd.getLastStep().isFirstStep() &&
                        (cmd.getLastStep().getType() == MoveStepType.LAY_MINE))) {
                clear();
            }
            gear = MovementDisplay.GEAR_JUMP;
            jumpSubGear = GEAR_SUB_STANDARD;
            if (mustChooseJumpType(entity)) {
                String[] choices = { "Mechanical Jump Boosters", "Jump Jets" };

                int jumpChoice = JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(this),
                      "Choose jump type:", "Choose Jump Type",
                      JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                      null, choices, choices[1]);

                if (jumpChoice == 0) {
                    jumpSubGear = GEAR_SUB_MEK_BOOSTERS;
                }

            } else {
                if ((entity instanceof Mek mek) && (mek.getMechanicalJumpBoosterMP() > 0) && (mek.getJumpMP() == 0)) {
                    jumpSubGear = GEAR_SUB_MEK_BOOSTERS;
                }
            }
            if (!cmd.isJumping()) {
                initializeJumpMovePath();
            }
            Color jumpColor = GUIP.getMoveJumpColor();
            clientgui.boardViews().forEach(bv -> ((BoardView) bv).setHighlightColor(jumpColor));
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_SWIM.getCmd())) {
            if (gear != MovementDisplay.GEAR_SWIM) {
                clear();
            }
            gear = MovementDisplay.GEAR_SWIM;
            entity.setMovementMode((entity instanceof BipedMek) ?
                  EntityMovementMode.BIPED_SWIM :
                  EntityMovementMode.QUAD_SWIM);
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_CONVERT.getCmd())) {
            EntityMovementMode nextMode = entity.nextConversionMode(cmd.getFinalConversionMode());
            // LAMs may have to skip the next mode due to damage
            if (entity instanceof LandAirMek landAirMek) {
                if (!landAirMek.canConvertTo(nextMode)) {
                    nextMode = landAirMek.nextConversionMode(nextMode);
                }
                if (!landAirMek.canConvertTo(nextMode)) {
                    nextMode = landAirMek.getMovementMode();
                }
            }
            adjustConvertSteps(nextMode);
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_LEG.getCmd())) {
            if (entity instanceof QuadVee) {
                adjustConvertSteps(EntityMovementMode.QUAD);
            } else if (entity instanceof TripodMek) {
                adjustConvertSteps(EntityMovementMode.TRIPOD);
            } else if (entity instanceof BipedMek) {
                adjustConvertSteps(EntityMovementMode.BIPED);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_VEE.getCmd())) {
            if (entity instanceof QuadVee quadVee && quadVee.getMotiveType() == QuadVee.MOTIVE_WHEEL) {
                adjustConvertSteps(EntityMovementMode.WHEELED);
            } else if ((entity instanceof Mek mek && mek.hasTracks()) || (entity instanceof QuadVee)) {
                adjustConvertSteps(EntityMovementMode.TRACKED);
            } else if (entity instanceof LandAirMek landAirMek && landAirMek.getLAMType() == LandAirMek.LAM_STANDARD) {
                adjustConvertSteps(EntityMovementMode.WIGE);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_MODE_AIR.getCmd())) {
            if (entity instanceof LandAirMek) {
                adjustConvertSteps(EntityMovementMode.AERODYNE);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_TURN.getCmd())) {
            gear = MovementDisplay.GEAR_TURN;
        } else if (actionCmd.equals(MoveCommand.MOVE_BACK_UP.getCmd())) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                gear = MovementDisplay.GEAR_BACKUP; // on purpose...
                clear();
            }
            gear = MovementDisplay.GEAR_BACKUP; // on purpose...
            Color backColor = GUIP.getMoveBackColor();
            clientgui.boardViews().forEach(bv -> ((BoardView) bv).setHighlightColor(backColor));
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_LONGEST_RUN.getCmd())) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                clear();
            }
            gear = MovementDisplay.GEAR_LONGEST_RUN;
        } else if (actionCmd.equals(MoveCommand.MOVE_LONGEST_WALK.getCmd())) {
            if (gear == MovementDisplay.GEAR_JUMP) {
                clear();
            }
            gear = MovementDisplay.GEAR_LONGEST_WALK;
        } else if (actionCmd.equals(MoveCommand.MOVE_CLEAR.getCmd())) {
            clear();
            if (!game.containsMinefield(entity.getPosition())) {
                String title = Messages.getString("MovementDisplay.CantClearMinefield");
                String body = Messages.getString("MovementDisplay.NoMinefield");
                clientgui.doAlertDialog(title, body);
                return;
            }

            // Does the entity have a minesweeper?
            int clear = Minefield.CLEAR_NUMBER_INFANTRY;
            int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;
            // Check for Minesweeping Engineers
            if ((currentEntity() instanceof Infantry inf)) {
                if (inf.hasSpecialization(Infantry.MINE_ENGINEERS)) {
                    clear = Minefield.CLEAR_NUMBER_INF_ENG;
                    boom = Minefield.CLEAR_NUMBER_INF_ENG_ACCIDENT;
                }
            }
            // Check for Mine clearance manipulators on BA
            if ((currentEntity() instanceof BattleArmor ba)) {
                String mcmName = BattleArmor.MANIPULATOR_TYPE_STRINGS[BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE];
                if (ba.getLeftManipulatorName().equals(mcmName)) {
                    clear = Minefield.CLEAR_NUMBER_BA_SWEEPER;
                    boom = Minefield.CLEAR_NUMBER_BA_SWEEPER_ACCIDENT;
                }
            }

            // need to choose a mine
            List<Minefield> mfs = game.getMinefields(entity.getPosition());
            String[] choices = new String[mfs.size()];
            for (int loop = 0; loop < choices.length; loop++) {
                choices[loop] = Minefield.getDisplayableName(mfs.get(loop).getType());
            }

            String title = Messages.getString("MovementDisplay.ChooseMinefieldDialog.title");
            String body = Messages.getString("MovementDisplay.ChooseMinefieldDialog.message");
            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  body,
                  title,
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  choices,
                  null);
            Minefield mf = null;
            if (input != null) {
                for (int loop = 0; loop < choices.length; loop++) {
                    if (input.equals(choices[loop])) {
                        mf = mfs.get(loop);
                        break;
                    }
                }
            }
            title = Messages.getString("MovementDisplay.ClearMinefieldDialog.title");
            body = Messages.getString("MovementDisplay.ClearMinefieldDialog.message", clear, boom);
            if ((null != mf) && clientgui.doYesNoDialog(title, body)) {
                addStepToMovePath(MoveStepType.CLEAR_MINEFIELD, mf);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_CHARGE.getCmd())) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clear();
            }
            gear = MovementDisplay.GEAR_CHARGE;
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_DFA.getCmd())) {
            if (gear != MovementDisplay.GEAR_JUMP) {
                clear();
            }
            gear = MovementDisplay.GEAR_DFA;
            computeMovementEnvelope(entity);
            if (!cmd.isJumping()) {
                initializeJumpMovePath();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_RAM.getCmd())) {
            if (gear != MovementDisplay.GEAR_LAND) {
                clear();
            }
            if (entity.isAirborneVTOLorWIGE()) {
                gear = MovementDisplay.GEAR_CHARGE;
            } else {
                gear = MovementDisplay.GEAR_RAM;
            }
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_GET_UP.getCmd())) {
            // if the unit has a hull down step, then don't clear the moves
            if (!cmd.contains(MoveStepType.HULL_DOWN)) {
                clear();
            }

            if (opts.booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_CAREFUL_STAND)
                  && (entity.getWalkMP() > 2)) {
                String title = Messages.getString("MovementDisplay.CarefulStand.title");
                String body = Messages.getString("MovementDisplay.CarefulStand.message");
                boolean response = clientgui.doYesNoDialog(title, body);
                if (response) {
                    entity.setCarefulStand(true);
                    if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                        addStepToMovePath(MoveStepType.CAREFUL_STAND);
                    }
                } else {
                    if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                        addStepToMovePath(MoveStepType.GET_UP);
                    }
                }
            } else {
                updateDonePanel();
                if (cmd.getFinalProne() || cmd.getFinalHullDown()) {
                    addStepToMovePath(MoveStepType.GET_UP);
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_GO_PRONE.getCmd())) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalProne()) {
                addStepToMovePath(MoveStepType.GO_PRONE);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_HULL_DOWN.getCmd())) {
            gear = MovementDisplay.GEAR_LAND;
            if (!cmd.getFinalHullDown()) {
                addStepToMovePath(MoveStepType.HULL_DOWN);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_BRACE.getCmd())) {
            var options = currentEntity().getValidBraceLocations();
            if (options.size() == 1) {
                addStepToMovePath(MoveStepType.BRACE, options.get(0));
            } else if (options.size() > 1) {
                String[] locationNames = new String[options.size()];

                for (int x = 0; x < options.size(); x++) {
                    locationNames[x] = currentEntity().getLocationName(options.get(x));
                }

                // Dialog for choosing which location to brace
                String title = "Choose Brace Location";
                String body = "Choose the location to brace:";
                String option = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                      body,
                      title,
                      JOptionPane.QUESTION_MESSAGE,
                      null,
                      locationNames,
                      locationNames[0]);

                // Verify that we have a valid option...
                if (option != null) {
                    int id = options.get(Arrays.asList(locationNames).indexOf(option));
                    addStepToMovePath(MoveStepType.BRACE, id);
                    updateDonePanel();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_FLEE.getCmd()) &&
              clientgui.doYesNoDialog(Messages.getString("MovementDisplay.EscapeDialog.title"),
                    Messages.getString("MovementDisplay.EscapeDialog.message"))) {
            addStepToMovePath(MoveStepType.FLEE);
            ready();
            clear();
        } else if (actionCmd.equals(MoveCommand.MOVE_FLY_OFF.getCmd())) {
            handleFlyOffOrClimbOut();
        } else if (actionCmd.equals(MoveCommand.MOVE_EJECT.getCmd())) {
            if (entity instanceof Tank) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.AbandonDialog.title"),
                      Messages.getString("MovementDisplay.AbandonDialog.message"))) {
                    clear();
                    addStepToMovePath(MoveStepType.EJECT);
                    ready();
                }
            } else if (entity.isLargeCraft()) {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.AbandonDialog.title"),
                      Messages.getString("MovementDisplay.AbandonDialog.message"))) {
                    clear();
                    // If we're abandoning while grounded, find a legal position to put an
                    // EjectedCrew unit
                    if (!entity.isSpaceborne() && entity.getAltitude() == 0) {
                        Coords pos = getEjectPosition(entity);
                        addStepToMovePath(MoveStepType.EJECT, entity, pos);
                    } else {
                        addStepToMovePath(MoveStepType.EJECT);
                    }
                    ready();
                }
            } else if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.AbandonDialog1.title"),
                  Messages.getString("MovementDisplay.AbandonDialog1.message"))) {
                clear();
                addStepToMovePath(MoveStepType.EJECT);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_ABANDON.getCmd())) {
            // Mek abandonment (two-phase per TacOps:AR p.165)
            if ((entity instanceof Mek mek) && mek.canAbandon()) {
                if (clientgui.doYesNoDialog(
                      Messages.getString("MovementDisplay.MekAbandonDialog.title"),
                      Messages.getString("MovementDisplay.MekAbandonDialog.message"))) {
                    clear();
                    addStepToMovePath(MoveStepType.ABANDON);
                    ready();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_LOAD.getCmd())) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = getLoadedUnit();
            if (other != null) {
                int length = cmd.length();
                addStepToMovePath(MoveStepType.LOAD, other, other.getPosition());
                if (cmd.length() == length || cmd.getLastStepMovementType() == EntityMovementType.MOVE_ILLEGAL) {
                    // Load step failed to add or was illegal; unset other's loading flag
                    other.setTargetBay(UNSET_BAY);
                }
                gear = MovementDisplay.GEAR_LAND;
            } // else - didn't find a unit to load
        } else if (actionCmd.equals(MoveCommand.MOVE_TOW.getCmd())) {
            // Find the other friendly unit in our hex, add it
            // to our local list of loaded units, and then stop.
            Entity other = getTowedUnit();
            if (other != null) {
                addStepToMovePath(MoveStepType.TOW);
            } // else - didn't find a unit to tow
        } else if (actionCmd.equals(MoveCommand.MOVE_DISCONNECT.getCmd())) {
            // Ask the user if we're carrying multiple units.
            Entity other = getDisconnectedUnit();
            if (other != null) {
                addStepToMovePath(MoveStepType.DISCONNECT, other);
            } // else - didn't find a unit to tow
        } else if (actionCmd.equals(MoveCommand.MOVE_MOUNT.getCmd())) {
            Entity other = getMountedUnit();
            if (other != null) {
                addStepToMovePath(MoveStepType.MOUNT, other);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_UNLOAD.getCmd())) {
            Entity other = getUnloadedUnit();
            if (other != null) {
                if (!other.isInfantry() ||
                      currentEntity() instanceof SmallCraft ||
                      (currentEntity().isSupportVehicle() && (currentEntity().getWeightClass()
                            == EntityWeightClass.WEIGHT_LARGE_SUPPORT))
                      // FIXME: unclear why towed/towing is checked here:
                      ||
                      !currentEntity().getAllTowedUnits().isEmpty() ||
                      currentEntity().getTowedBy() != Entity.NONE) {
                    // unload into adjacent hexes
                    Coords pos = getUnloadPosition(other);
                    if (null != pos) {
                        // set other's position and end this turn - the unloading unit will get
                        // another turn for further unloading later
                        // Also mark the chosen unit as planning to unload this turn.
                        int length = cmd.length();
                        addStepToMovePath(MoveStepType.UNLOAD, other, pos);
                        if (!(length == cmd.length() || cmd.getLastStepMovementType() == EntityMovementType.MOVE_ILLEGAL)) {
                            // Record the hashcode of the target hex temporarily, for filtering.
                            other.setTargetBay(pos.hashCode());
                        }
                        // We give SmallCraft and DropShips extra unloading turns, so ready them now.
                        // Also ready other craft that have unloaded but don't have any more units to unload.
                        if (entity.isSmallCraft() || entity.isDropShip()) {
                            ready();
                        } else {
                            List<Entity> filteredUnits = unloadableUnits
                                  .stream()
                                  .filter(unloadable -> unloadable.getTargetBay() == UNSET_BAY)
                                  .collect(Collectors.toList());
                            if (filteredUnits.isEmpty()) {
                                ready();
                            }
                        }
                    }
                } else {
                    // unload into the same hex
                    addStepToMovePath(MoveStepType.UNLOAD, other);
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_PICKUP_CARGO.getCmd())) {
            processPickupCargoCommand();
        } else if (actionCmd.equals(MoveCommand.MOVE_DROP_CARGO.getCmd())) {
            var options = currentEntity().getDistinctCarriedObjects();
            List<ICarryable> moreOptions = entity.getTransports()
                  .stream()
                  .filter(t -> t instanceof ExternalCargo)
                  .map(t -> ((ExternalCargo) t).getCarryables().toArray(ICarryable[]::new))
                  .flatMap(Arrays::stream)
                  .toList().stream().filter(carryable -> !options.contains(carryable)).collect(Collectors.toList());

            List<ICarryable> fullOptions = new ArrayList<>(options);
            fullOptions.addAll(moreOptions);

            if (fullOptions.size() == 1) {
                addStepToMovePath(MoveStepType.DROP_CARGO);
                updateDonePanel();
            } else if (fullOptions.size() > 1) {
                // reverse lookup: location name to location ID - we're going to wind up with a
                // name chosen
                // but need to send the ID in the move path.
                Map<String, Integer> locationMap = currentEntity().getDropCargoLocationMap();

                // Dialog for choosing which object to pick up
                String title = "Choose Cargo to Drop";
                String body = "Choose the cargo to drop:";
                String option = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                      body,
                      title,
                      JOptionPane.QUESTION_MESSAGE,
                      null,
                      locationMap.keySet().toArray(),
                      locationMap.keySet().toArray()[0]);

                // Verify that we have a valid option...
                if (option != null) {
                    int location = locationMap.get(option);
                    addStepToMovePath(MoveStepType.DROP_CARGO, location);
                    updateDonePanel();
                }
            }
        }
        if (actionCmd.equals(MoveCommand.MOVE_RAISE_ELEVATION.getCmd())) {
            addStepToMovePath(MoveStepType.UP);
        } else if (actionCmd.equals(MoveCommand.MOVE_LOWER_ELEVATION.getCmd())) {
            if (entity.isAero()) {
                PlanetaryConditions conditions = game.getPlanetaryConditions();
                boolean spheroidOrLessThanThin = ((IAero) entity).isSpheroid() ||
                      conditions.getAtmosphere().isLighterThan(Atmosphere.THIN);
                if ((null != cmd.getLastStep()) &&
                      (cmd.getLastStep().getNDown() == 1) &&
                      (cmd.getLastStep().getVelocity() < 12) &&
                      !spheroidOrLessThanThin) {
                    addStepToMovePath(MoveStepType.ACC, true);
                    computeMovementEnvelope(entity);
                }
            }
            addStepToMovePath(MoveStepType.DOWN);
        } else if (actionCmd.equals(MoveCommand.MOVE_CLIMB_MODE.getCmd())) {
            MoveStep ms = cmd.getLastStep();
            if ((ms != null) &&
                  ((ms.getType() == MoveStepType.CLIMB_MODE_ON) || (ms.getType() == MoveStepType.CLIMB_MODE_OFF))) {
                MoveStep lastStep = cmd.getLastStep();
                cmd.removeLastStep();
                // Add another climb mode step. Without this, we end up with 3 effect modes: no climb step, climb step
                // on, climb step off. This affects how the StepSprite gets rendered, so it's clearer to keep a climb
                // step once one has been added
                if (lastStep.getType() == MoveStepType.CLIMB_MODE_ON) {
                    addStepToMovePath(MoveStepType.CLIMB_MODE_OFF);
                } else {
                    addStepToMovePath(MoveStepType.CLIMB_MODE_ON);
                }
            } else if (cmd.getFinalClimbMode()) {
                addStepToMovePath(MoveStepType.CLIMB_MODE_OFF);
            } else {
                addStepToMovePath(MoveStepType.CLIMB_MODE_ON);
            }
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_LAY_MINE.getCmd())) {
            int i = chooseMineToLay();
            if (i != -1) {
                MiscMounted m = (MiscMounted) entity.getEquipment(i);
                if (m.getMineType() == MiscMounted.MINE_VIBRABOMB) {
                    VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.getFrame());
                    vsd.setVisible(true);
                    m.setVibraSetting(vsd.getSetting());
                }
                if (cmd.getLastStep() == null &&
                      entity instanceof BattleArmor &&
                      entity.getMovementMode().equals(EntityMovementMode.INF_JUMP)) {
                    initializeJumpMovePath();
                    gear = GEAR_JUMP;
                    Color jumpColor = GUIP.getMoveJumpColor();
                    clientgui.boardViews().forEach(bv -> ((BoardView) bv).setHighlightColor(jumpColor));
                    computeMovementEnvelope(entity);
                }
                addStepToMovePath(MoveStepType.LAY_MINE, i);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_CALL_SUPPORT.getCmd())) {
            ((Infantry) entity).createLocalSupport();
            clientgui.getClient().sendUpdateEntity(currentEntity());
        } else if (actionCmd.equals(MoveCommand.MOVE_DIG_IN.getCmd())) {
            addStepToMovePath(MoveStepType.DIG_IN);
        } else if (actionCmd.equals(MoveCommand.MOVE_FORTIFY.getCmd())) {
            addStepToMovePath(MoveStepType.FORTIFY);
        } else if (actionCmd.equals(MoveCommand.MOVE_TAKE_COVER.getCmd())) {
            addStepToMovePath(MoveStepType.TAKE_COVER);
        } else if (actionCmd.equals(MoveCommand.MOVE_SHAKE_OFF.getCmd())) {
            addStepToMovePath(MoveStepType.SHAKE_OFF_SWARMERS);
        } else if (actionCmd.equals(MoveCommand.MOVE_RECKLESS.getCmd())) {
            cmd.setCareful(false);
        } else if (actionCmd.equals(MoveCommand.MOVE_STRAFE.getCmd())) {
            gear = GEAR_STRAFE;
        } else if (actionCmd.equals(MoveCommand.MOVE_BOMB.getCmd())) {
            if (cmd.getLastStep() == null) {
                addStepToMovePath(MoveStepType.NONE);
            }
            cmd.setVTOLBombStep(cmd.getFinalCoords());
            cmd.compile(game, entity, false);
            updateMove();
        } else if (actionCmd.equals(MoveCommand.MOVE_ACCELERATION.getCmd())) {
            removeIllegalSteps();
            addStepToMovePath(MoveStepType.ACCELERATION);
        } else if (actionCmd.equals(MoveCommand.MOVE_DECELERATION.getCmd())) {
            removeIllegalSteps();
            addStepToMovePath(MoveStepType.DECELERATION);
        } else if (actionCmd.equals(MoveCommand.MOVE_ACC.getCmd())) {
            addStepToMovePath(MoveStepType.ACC);
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_DEC.getCmd())) {
            addStepToMovePath(MoveStepType.DEC);
            computeMovementEnvelope(entity);
        } else if (actionCmd.equals(MoveCommand.MOVE_EVADE.getCmd())) {
            addStepToMovePath(MoveStepType.EVADE);
        } else if (actionCmd.equals(MoveCommand.MOVE_BOOTLEGGER.getCmd())) {
            addStepToMovePath(MoveStepType.BOOTLEGGER);
        } else if (actionCmd.equals(MoveCommand.MOVE_SHUTDOWN.getCmd())) {
            if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.ShutdownDialog.title"),
                  Messages.getString("MovementDisplay.ShutdownDialog.message"))) {
                addStepToMovePath(MoveStepType.SHUTDOWN);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_STARTUP.getCmd())) {
            // Check if unit has pending abandonment - warn that startup will cancel it
            boolean proceedWithStartup = true;
            if (entity.isPendingAbandon()) {
                proceedWithStartup = clientgui.doYesNoDialog(
                      Messages.getString("MovementDisplay.StartupCancelAbandonDialog.title"),
                      Messages.getString("MovementDisplay.StartupCancelAbandonDialog.message"));
            } else {
                proceedWithStartup = clientgui.doYesNoDialog(
                      Messages.getString("MovementDisplay.StartupDialog.title"),
                      Messages.getString("MovementDisplay.StartupDialog.message"));
            }
            if (proceedWithStartup) {
                clear();
                addStepToMovePath(MoveStepType.STARTUP);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_SELF_DESTRUCT.getCmd())) {
            if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.SelfDestructDialog.title"),
                  Messages.getString("MovementDisplay.SelfDestructDialog.message"))) {
                addStepToMovePath(MoveStepType.SELF_DESTRUCT);
                ready();
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_EVADE_AERO.getCmd())) {
            removeIllegalSteps();
            addStepToMovePath(MoveStepType.EVADE);
            setEvadeAeroEnabled(false);
        } else if (actionCmd.equals(MoveCommand.MOVE_ROLL.getCmd())) {
            addStepToMovePath(MoveStepType.ROLL);
        } else if (actionCmd.equals(MoveCommand.MOVE_HOVER.getCmd())) {
            addStepToMovePath(MoveStepType.HOVER);
            if (entity instanceof LandAirMek
                  && entity.getMovementMode() == EntityMovementMode.WIGE
                  && entity.isAirborne()) {
                gear = GEAR_LAND;
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_MANEUVER.getCmd())) {
            ManeuverChoiceDialog choiceDialog = new ManeuverChoiceDialog(clientgui.getFrame(),
                  Messages.getString("MovementDisplay.ManeuverDialog.title"));

            IAero a = (IAero) entity;
            MoveStep last = cmd.getLastStep();
            int vel = a.getCurrentVelocity();
            int altitude = entity.getAltitude();
            Coords pos = entity.getPosition();
            int distance = 0;
            if (null != last) {
                vel = last.getVelocityLeft();
                altitude = last.getAltitude();
                distance = last.getDistance();
            }
            Board board = game.getBoard(finalBoardId());
            // On Atmospheric maps, elevations are treated as altitudes, so hex ceiling is the ground
            int ceil = board.getHex(pos).ceiling(board.isLowAltitude());
            // On the ground map, Aerospace ignores hex elevations
            if (board.isGround()) {
                ceil = 0;
            }
            choiceDialog.checkPerformability(vel, altitude, ceil, a.isVSTOL(), distance, board,
                  cmd);
            choiceDialog.setVisible(true);
            int manType = choiceDialog.getChoice();
            updateMove((manType > ManeuverType.MAN_NONE) && addManeuver(manType));
        } else if (actionCmd.equals(MoveCommand.MOVE_LAUNCH.getCmd())) {
            TreeMap<Integer, Vector<Integer>> undocked = getUndockedUnits();
            if (!undocked.isEmpty()) {
                addStepToMovePath(MoveStepType.UNDOCK, undocked);
            }
            TreeMap<Integer, Vector<Integer>> launched = getLaunchedUnits();
            if (!launched.isEmpty()) {
                addStepToMovePath(MoveStepType.LAUNCH, launched);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_RECOVER.getCmd()) ||
              actionCmd.equals(MoveCommand.MOVE_DOCK.getCmd())) {
            // if more than one unit is available as a carrier, then bring up an option dialog
            int recoverer = getRecoveryUnit();
            if (recoverer != NO_UNIT_SELECTED) {
                addStepToMovePath(MoveStepType.RECOVER, recoverer, -1);
            }
            if (actionCmd.equals(MoveCommand.MOVE_DOCK.getCmd())) {
                cmd.getLastStep().setDocking(true);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_DROP.getCmd())) {
            TreeMap<Integer, Vector<Integer>> dropped = getDroppedUnits();
            if (!dropped.isEmpty()) {
                addStepToMovePath(MoveStepType.DROP, dropped);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_JOIN.getCmd())) {
            // if more than one unit is available as a carrier, then bring up an option dialog
            int joined = getUnitJoined();
            if (joined != -1) {
                addStepToMovePath(MoveStepType.JOIN, joined, -1);
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_TURN_LEFT.getCmd())) {
            addStepToMovePath(MoveStepType.TURN_LEFT);
        } else if (actionCmd.equals(MoveCommand.MOVE_TURN_RIGHT.getCmd())) {
            addStepToMovePath(MoveStepType.TURN_RIGHT);
        } else if (actionCmd.equals(MoveCommand.MOVE_THRUST.getCmd())) {
            addStepToMovePath(MoveStepType.THRUST);
        } else if (actionCmd.equals(MoveCommand.MOVE_YAW.getCmd())) {
            addStepToMovePath(MoveStepType.YAW);
        } else if (actionCmd.equals(MoveCommand.MOVE_END_OVER.getCmd())) {
            addStepsToMovePath(MoveStepType.YAW, MoveStepType.ROLL);
        } else if (actionCmd.equals(MoveCommand.MOVE_DUMP.getCmd())) {
            dumpBombs();
        } else if (actionCmd.equals(MoveCommand.MOVE_TAKE_OFF.getCmd())) {
            if (currentEntity().isAero() && (null != ((IAero) currentEntity()).hasRoomForHorizontalTakeOff())) {
                String title = Messages.getString("MovementDisplay.NoTakeOffDialog.title");
                String body = Messages.getString("MovementDisplay.NoTakeOffDialog.message",
                      ((IAero) currentEntity()).hasRoomForHorizontalTakeOff());
                clientgui.doAlertDialog(title, body);
            } else {
                if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.TakeOffDialog.title"),
                      Messages.getString("MovementDisplay.TakeOffDialog.message"))) {
                    clear();
                    addStepToMovePath(MoveStepType.TAKEOFF);
                    ready();
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_VERT_TAKE_OFF.getCmd())) {
            if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.TakeOffDialog.title"),
                  Messages.getString("MovementDisplay.TakeOffDialog.message"))) {
                clear();
                addStepToMovePath(MoveStepType.VERTICAL_TAKE_OFF);
                ready();
            }

        } else if (actionCmd.equals(MoveCommand.MOVE_LAND.getCmd())) {
            performAeroLand(HORIZONTAL);

        } else if (actionCmd.equals(MoveCommand.MOVE_VERT_LAND.getCmd())) {
            performAeroLand(VERTICAL);

        } else if (actionCmd.equals(MoveCommand.MOVE_ENVELOPE.getCmd())) {
            computeMovementEnvelope(clientgui.getUnitDisplay().getCurrentEntity());
        } else if (actionCmd.equals(MoveCommand.MOVE_TRAITOR.getCmd())) {
            var players = game.getPlayersList();
            Integer[] playerIds = new Integer[players.size() - 1];
            String[] playerNames = new String[players.size() - 1];
            String[] options = new String[players.size() - 1];
            Entity e = currentEntity();
            if (e == null) {
                return;
            }

            Player currentOwner = e.getOwner();
            // Loop through the `players` vector and fill in the arrays
            int idx = 0;
            for (var player : players) {
                if (player.getName().equals(currentOwner.getName()) || (player.getTeam() == Player.TEAM_UNASSIGNED)) {
                    continue;
                }
                playerIds[idx] = player.getId();
                playerNames[idx] = player.getName();
                options[idx] = player.getName() + " (ID: " + player.getId() + ")";
                idx++;
            }

            // No players available?
            if (idx == 0) {
                JOptionPane.showMessageDialog(clientgui.getFrame(),
                      "No players available. Units cannot turn traitor to players " +
                            "that aren't assigned to a team.");
                return;
            }

            // Dialog for choosing which player to transfer to
            String option = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  "Choose the player to gain ownership of this unit (" + e.getDisplayName() + ") when it turns traitor",
                  "Traitor",
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  options,
                  options[0]);

            // Verify that we have a valid option...
            if (option != null) {
                // Now that we've selected a player, correctly associate the ID and name
                int id = playerIds[Arrays.asList(options).indexOf(option)];
                String name = playerNames[Arrays.asList(options).indexOf(option)];

                // And now we perform the actual transfer
                int confirm = JOptionPane.showConfirmDialog(clientgui.getFrame(),
                      e.getDisplayName() + " will switch to " + name + "'s side at the end of this turn. Are you sure?",
                      "Confirm",
                      JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    e.setTraitorId(id);
                    clientgui.getClient().sendUpdateEntity(e);
                }
            }
        } else if (actionCmd.equals(MoveCommand.MOVE_CHAFF.getCmd())) {
            if (clientgui.doYesNoDialog(Messages.getString("MovementDisplay.ConfirmChaff.title"),
                  Messages.getString("MovementDisplay.ConfirmChaff.message"))) {
                isUsingChaff = true;
            }
        }
        updateProneButtons();
        updateRACButton();
        updateSearchlightButton();
        updateElevationButtons();
        updateTakeOffButtons();
        updateLandButtons();
        updateFlyOffButton();
        updateLaunchButton();
        updateLoadButtons();
        updateDropButton();
        updateConvertModeButton();
        updateRecklessButton();
        updateHoverButton();
        updateManeuverButton();
        updateEvadeButton();
        updateBootleggerButton();
        updateShutdownButton();
        updateStartupButton();
        updateSelfDestructButton();
        updateTraitorButton();
        updateSpeedButtons();
        updateThrustButton();
        updateRollButton();
        updateTakeCoverButton();
        updateLayMineButton();
        updateBraceButton();
        checkFuel();
        checkOOC();
        checkAtmosphere();

        // If small craft / DropShip that has unloaded units, then only allowed to
        // unload more
        if (entity.hasUnloadedUnitsFromBays()) {
            disableButtons();
            updateLoadButtons();
            butDone.setEnabled(true);
        }
    }

    /**
     * @param ce The entity to test
     *
     * @return True when the given unit is a Mek with both jump jets and mechanical jump boosters.
     */
    private boolean mustChooseJumpType(Entity ce) {
        return (ce instanceof Mek mek) && (ce.getJumpMP() > 0) && (mek.getMechanicalJumpBoosterMP() > 0);
    }

    /**
     * Worker function containing the "pickup cargo" command.
     */
    private void processPickupCargoCommand() {
        var options = game.getGroundObjects(finalPosition());
        options.addAll(game.getEntitiesVector(finalPosition()).stream().filter(Entity::isCarryableObject).toList());
        var displayedOptions =
              options.stream().filter(o -> currentEntity().canPickupCarryableObject(o)).toList();

        // if there's only one thing to pick up, pick it up. regardless of how many objects we are picking up, we may
        // have to choose the location with which to pick it up
        if (displayedOptions.size() == 1) {
            Integer pickupLocation = getPickupLocation(displayedOptions.get(0));

            if (pickupLocation != null) {
                Map<Integer, Integer> data = new HashMap<>();
                // we pick the only eligible object out of all the objects on the ground
                data.put(MoveStep.CARGO_PICKUP_KEY, options.indexOf(displayedOptions.get(0)));
                data.put(MoveStep.CARGO_LOCATION_KEY, pickupLocation);

                addStepToMovePath(MoveStepType.PICKUP_CARGO, data);
                updateDonePanel();
            }
        } else if (displayedOptions.size() > 1) {
            // Dialog for choosing which object to pick up
            String title = "Choose Cargo to Pick Up";
            String body = "Choose the cargo to pick up:";
            ICarryable option = (ICarryable) JOptionPane.showInputDialog(clientgui.getFrame(),
                  body,
                  title,
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  displayedOptions.toArray(),
                  displayedOptions.get(0));

            if (option != null) {
                Integer pickupLocation = getPickupLocation(option);

                if (pickupLocation != null) {
                    int cargoIndex = options.indexOf(option);
                    Map<Integer, Integer> data = new HashMap<>();
                    data.put(MoveStep.CARGO_PICKUP_KEY, cargoIndex);
                    data.put(MoveStep.CARGO_LOCATION_KEY, pickupLocation);

                    addStepToMovePath(MoveStepType.PICKUP_CARGO, data);
                    updateDonePanel();
                }
            }
        }
    }

    /**
     * Worker function to choose a limb (or whatever) with which to pick up cargo
     */
    private Integer getPickupLocation(ICarryable cargo) {
        Map<String, Integer> locationMap = currentEntity().getPickupLocationMap(cargo);
        int pickupLocation = Entity.LOC_NONE;

        // if we need to choose a pickup location, then do so
        if (locationMap.size() > 1) {


            // Dialog for choosing which object to pick up
            String title = "Choose Pickup Location";
            String body = "Choose the location with which to pick up cargo:";
            String locationChoice = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  body,
                  title,
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  locationMap.keySet().toArray(),
                  locationMap.keySet().toArray()[0]);

            if (locationChoice != null) {
                pickupLocation = locationMap.get(locationChoice);
            } else {
                return null;
            }
        } else if (locationMap.size() == 1) {
            pickupLocation = locationMap.get(locationMap.keySet().iterator().next());
        }

        return pickupLocation;
    }

    /**
     * Add enough {@link MoveStepType#CONVERT_MODE} steps to get to the requested mode, or clear the path if the unit is
     * in the requested mode at the beginning of the turn.
     *
     * @param endMode The mode to convert to
     */
    private void adjustConvertSteps(EntityMovementMode endMode) {
        // Since conversion is not allowed in water, we shouldn't have to deal with the possibility of `swim` modes.
        // Account for grounded LAMs in fighter mode with movement type wheeled
        if (currentEntity().getMovementMode() == endMode || (currentEntity().isAero()
              && endMode == EntityMovementMode.AERODYNE)) {
            cmd.clear();
            return;
        }
        if (cmd.getFinalConversionMode() == endMode) {
            return;
        }
        clear();
        currentEntity().setConvertingNow(true);
        addStepToMovePath(MoveStepType.CONVERT_MODE);
        if (cmd.getFinalConversionMode() != endMode) {
            addStepToMovePath(MoveStepType.CONVERT_MODE);
        }
        if (currentEntity() instanceof Mek && ((Mek) currentEntity()).hasTracks()) {
            currentEntity().setMovementMode(endMode);
        }
        updateMove();
    }

    /**
     * Give the player the opportunity to unload all entities that are stranded on immobile transports.
     * <p>
     * According to Randall Bills, the "minimum move" rule allows stranded units to dismount at the start of the turn.
     */
    private void unloadStranded() {
        Vector<Entity> stranded = new Vector<>();
        String[] names;
        Entity entity;
        Entity transport;

        // Let the player know what's going on.
        setStatusBarText(Messages.getString("MovementDisplay.AllPlayersUnload"));

        // Collect the stranded entities into the vector.
        Iterator<Entity> entities = clientgui.getClient().getSelectedEntities(new EntitySelector() {
            private final GameTurn turn = game.getTurn();
            private final int ownerId = clientgui.getClient().getLocalPlayer().getId();

            @Override
            public boolean accept(Entity acc) {
                return turn.isValid(ownerId, acc, game);
            }
        });
        while (entities.hasNext()) {
            stranded.addElement(entities.next());
        }

        // Construct an array of stranded entity names
        names = new String[stranded.size()];
        for (int index = 0; index < names.length; index++) {
            entity = stranded.elementAt(index);
            transport = clientgui.getClient().getEntity(entity.getTransportId());
            String buffer;
            if (null == transport) {
                buffer = entity.getDisplayName();
            } else {
                buffer = Messages.getString("MovementDisplay.EntityAt",
                      entity.getDisplayName(),
                      transport.getPosition().getBoardNum());
            }
            names[index] = buffer;
        }

        // Show the choices to the player
        int[] indexes = clientgui.doChoiceDialog(Messages.getString("MovementDisplay.UnloadStrandedUnitsDialog.title"),
              Messages.getString("MovementDisplay.UnloadStrandedUnitsDialog.message"),
              names);

        // Convert the indexes into selected entity IDs and tell the server.
        int[] ids = null;
        if (null != indexes) {
            ids = new int[indexes.length];
            for (int index = 0; index < indexes.length; index++) {
                entity = stranded.elementAt(index);
                ids[index] = entity.getId();
            }
        }
        clientgui.getClient().sendUnloadStranded(ids);
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        final Entity currentEntity = currentEntity();

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (clientgui.getClient().isMyTurn() && (currentEntity != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.centerOnUnit(currentEntity);
        }
    }

    @Override
    public void unitSelected(BoardViewEvent boardViewEvent) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity entity = game.getEntity(boardViewEvent.getEntityId());

        if (null == entity) {
            return;
        }

        if (clientgui.getClient().isMyTurn()) {
            GameTurn currentTurn = game.getTurn();

            if (currentTurn != null && currentTurn.isValidEntity(entity, game)) {
                selectEntity(entity.getId());
            }
        } else {
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(entity);
            if (entity.isDeployed()) {
                clientgui.centerOnUnit(entity);
            }
        }
    }

    private void setWalkEnabled(boolean enabled) {
        buttons.get(MoveCommand.MOVE_WALK).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_WALK.getCmd(), enabled);
    }

    private void setTurnEnabled(boolean enabled) {
        buttons.get(MoveCommand.MOVE_TURN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_TURN.getCmd(), enabled);
    }

    private void setNextEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_NEXT.getCmd(), enabled);
    }

    private void setForwardIniEnabled(boolean enabled) {
        // `forward initiative` can only be done if Teams have an initiative!
        if (game.getOptions().booleanOption(OptionsConstants.BASE_TEAM_INITIATIVE)) {
            getBtn(MoveCommand.MOVE_FORWARD_INI).setEnabled(enabled);
            clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_FORWARD_INI.getCmd(), enabled);
        } else {
            // turn them off regardless of what is said!
            getBtn(MoveCommand.MOVE_FORWARD_INI).setEnabled(false);
            clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_FORWARD_INI.getCmd(), false);
        }
    }

    private void setLayMineEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_LAY_MINE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_LAY_MINE.getCmd(), enabled);
    }

    private void setLoadEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_LOAD).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_LOAD.getCmd(), enabled);
    }

    private void setMountEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_MOUNT).setEnabled(enabled);
    }

    private void setTowEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_TOW).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_TOW.getCmd(), enabled);
    }

    private void setUnloadEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_UNLOAD).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_UNLOAD.getCmd(), enabled);
    }

    private void setDisconnectEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DISCONNECT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_DISCONNECT.getCmd(), enabled);
    }

    private void setJumpEnabled(boolean enabled) {
        buttons.get(MoveCommand.MOVE_JUMP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_JUMP.getCmd(), enabled);
    }

    private void setModeConvertEnabled(boolean enabled) {
        buttons.get(MoveCommand.MOVE_MODE_CONVERT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_MODE_CONVERT.getCmd(), enabled);
    }

    private void setSwimEnabled(boolean enabled) {
        buttons.get(MoveCommand.MOVE_SWIM).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_SWIM.getCmd(), enabled);
    }

    private void setBackUpEnabled(boolean enabled) {
        buttons.get(MoveCommand.MOVE_BACK_UP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_BACK_UP.getCmd(), enabled);
    }

    private void setChargeEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_CHARGE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_CHARGE.getCmd(), enabled);
    }

    private void setDFAEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DFA).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_DFA.getCmd(), enabled);
    }

    private void setGoProneEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_GO_PRONE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_GO_PRONE.getCmd(), enabled);
    }

    private void setFleeEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_FLEE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_FLEE.getCmd(), enabled);
    }

    private void setFlyOffEnabled(boolean enabled) {
        setFlyOffEnabled(enabled, false);
    }

    private void setFlyOffEnabled(boolean enabled, boolean isClimbOut) {
        String label = isClimbOut
              ? Messages.getString("MovementDisplay.butClimbOut")
              : Messages.getString("MovementDisplay.MoveOff");
        getBtn(MoveCommand.MOVE_FLY_OFF).setText(label);
        getBtn(MoveCommand.MOVE_FLY_OFF).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_FLY_OFF.getCmd(), enabled);
    }

    /**
     * Handles the fly off or climb out action when player clicks the Fly Off/Climb Out button. At altitude 10, units
     * can "climb out" of the atmosphere vertically. At map edges, units can fly off normally. When both options are
     * available, player chooses.
     */
    private void handleFlyOffOrClimbOut() {
        Entity entity = currentEntity();
        if (entity == null) {
            return;
        }

        // Get current position and altitude (from move path if steps exist, otherwise entity)
        MoveStep step = cmd.getLastStep();
        int altitude = entity.getAltitude();
        Coords position = entity.getPosition();
        int facing = entity.getFacing();
        int velocityLeft = ((IAero) entity).getCurrentVelocity();

        if (step != null) {
            altitude = step.getAltitude();
            position = step.getPosition();
            facing = step.getFacing();
            velocityLeft = step.getVelocityLeft();
        }

        Board board = game.getBoard(entity);
        boolean isGroundBoard = board.isGround();

        // Check if can climb out (altitude 10 on ground map, requires both return flyover and climb out options)
        boolean canClimbOut = (altitude == 10) && isGroundBoard
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_CLIMB_OUT);

        // Check if can fly off edge (same logic as updateFlyOffButton)
        boolean canFlyOffEdge = false;
        if (position != null) {
            IAero aero = (IAero) entity;
            if (aero.isSpheroid() && !board.isSpace()) {
                // Spheroids in atmosphere just need to be on edge
                canFlyOffEdge = (entity.getWalkMP() > 0) &&
                      ((position.getX() == 0) ||
                            (position.getX() == (board.getWidth() - 1)) ||
                            (position.getY() == 0) ||
                            (position.getY() == (board.getHeight() - 1)));
            } else {
                // Aerodynes and space - need correct facing and velocity
                boolean evenX = (position.getX() % 2) == 0;
                canFlyOffEdge = (velocityLeft > 0) &&
                      (((position.getX() == 0) && ((facing == 5) || (facing == 4))) ||
                            ((position.getX() == (board.getWidth() - 1)) &&
                                  ((facing == 1) || (facing == 2))) ||
                            ((position.getY() == 0) &&
                                  ((facing == 1) || (facing == 5) || (facing == 0)) &&
                                  evenX) ||
                            ((position.getY() == 0) && (facing == 0)) ||
                            ((position.getY() == (board.getHeight() - 1)) &&
                                  ((facing == 2) || (facing == 3) || (facing == 4)) &&
                                  !evenX) ||
                            ((position.getY() == (board.getHeight() - 1)) && (facing == 3)));
            }
        }

        // Determine which action to take
        boolean doClimbOut = false;
        if (canClimbOut && canFlyOffEdge) {
            // Both options available - ask player to choose
            // Yes = climb out, No = fly off edge
            doClimbOut = clientgui.doYesNoDialog(
                  Messages.getString("MovementDisplay.ClimbOrFlyOff.title"),
                  Messages.getString("MovementDisplay.ClimbOrFlyOff.message"));
        } else if (canClimbOut) {
            doClimbOut = true;
        }

        if (doClimbOut) {
            // Climb out path
            if (clientgui.doYesNoDialog(
                  Messages.getString("MovementDisplay.ClimbOutDialog.title"),
                  Messages.getString("MovementDisplay.ClimbOutDialog.message"))) {

                IGameOptions opts = game.getOptions();
                if (opts.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER)) {
                    // Allow deployment anywhere on map when returning from climb out
                    entity.setStartingPos(Board.START_ANY);
                    ((IAero) entity).setExitAltitude(altitude);
                    addStepToMovePath(MoveStepType.RETURN);
                    ready();
                } else {
                    // No return option - just leave
                    addStepToMovePath(MoveStepType.OFF);
                    ready();
                }
            }
        } else {
            // Normal fly off edge path (existing behavior)
            if (clientgui.doYesNoDialog(
                  Messages.getString("MovementDisplay.FlyOffDialog.title"),
                  Messages.getString("MovementDisplay.FlyOffDialog.message"))) {

                IGameOptions opts = game.getOptions();
                if (opts.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER) &&
                      clientgui.doYesNoDialog(
                            Messages.getString("MovementDisplay.ReturnFly.title"),
                            Messages.getString("MovementDisplay.ReturnFly.message"))) {
                    addStepToMovePath(MoveStepType.RETURN);
                } else {
                    addStepToMovePath(MoveStepType.OFF);
                }
                ready();
            }
        }
    }

    /**
     * Shows a dialog for the player to select which edge to return from when climbing out.
     *
     * @return The selected edge direction, or NONE if cancelled
     */
    private OffBoardDirection showEdgeSelectionDialog() {
        String[] options = {
              Messages.getString("MovementDisplay.Edge.North"),
              Messages.getString("MovementDisplay.Edge.South"),
              Messages.getString("MovementDisplay.Edge.East"),
              Messages.getString("MovementDisplay.Edge.West")
        };

        int result = JOptionPane.showOptionDialog(
              clientgui.getFrame(),
              Messages.getString("MovementDisplay.ClimbOutEdge.message"),
              Messages.getString("MovementDisplay.ClimbOutEdge.title"),
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              options,
              options[0]);

        return switch (result) {
            case 0 -> OffBoardDirection.NORTH;
            case 1 -> OffBoardDirection.SOUTH;
            case 2 -> OffBoardDirection.EAST;
            case 3 -> OffBoardDirection.WEST;
            default -> OffBoardDirection.NONE;
        };
    }

    private void setEjectEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_EJECT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_EJECT.getCmd(), enabled);
    }

    private void setAbandonEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_ABANDON).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_ABANDON.getCmd(), enabled);
    }

    private void setUnjamEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_UNJAM).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_UNJAM.getCmd(), enabled);
    }

    private void setChaffEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_CHAFF).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_CHAFF.getCmd(), enabled);
    }

    private void setSearchlightEnabled(boolean enabled, boolean state) {
        if (state) {
            getBtn(MoveCommand.MOVE_SEARCHLIGHT).setText(Messages.getString("MovementDisplay.butSearchlightOff"));
        } else {
            getBtn(MoveCommand.MOVE_SEARCHLIGHT).setText(Messages.getString("MovementDisplay.butSearchlightOn"));
        }
        getBtn(MoveCommand.MOVE_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_SEARCHLIGHT.getCmd(), enabled);
    }

    private void setHullDownEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_HULL_DOWN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_HULL_DOWN.getCmd(), enabled);
    }

    private void setBraceEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_BRACE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_BRACE.getCmd(), enabled);
    }

    private void setClearEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_CLEAR).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_CLEAR.getCmd(), enabled);
    }

    private void setGetUpEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_GET_UP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_GET_UP.getCmd(), enabled);
    }

    private void setRaiseEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_RAISE_ELEVATION).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_RAISE_ELEVATION.getCmd(), enabled);
    }

    private void setLowerEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_LOWER_ELEVATION).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_LOWER_ELEVATION.getCmd(), enabled);
    }

    private void setRecklessEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_RECKLESS).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_RECKLESS.getCmd(), enabled);
    }

    private void setAccEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_ACC).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_ACC.getCmd(), enabled);
    }

    private void setDecEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DEC).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_DEC.getCmd(), enabled);
    }

    private void setAccNEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_ACCELERATION).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_ACCELERATION.getCmd(), enabled);
    }

    private void setDecNEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DECELERATION).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_DECELERATION.getCmd(), enabled);
    }

    private void setEvadeEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_EVADE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_EVADE.getCmd(), enabled);
    }

    private void setBootleggerEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_BOOTLEGGER).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_BOOTLEGGER.getCmd(), enabled);
    }

    private void setShutdownEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_SHUTDOWN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_SHUTDOWN.getCmd(), enabled);
    }

    private void setStartupEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_STARTUP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_STARTUP.getCmd(), enabled);
    }

    private void setSelfDestructEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_SELF_DESTRUCT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_SELF_DESTRUCT.getCmd(), enabled);
    }

    private void setTraitorEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_TRAITOR).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_TRAITOR.getCmd(), enabled);
    }

    private void setEvadeAeroEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_EVADE_AERO).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_EVADE_AERO.getCmd(), enabled);
    }

    private void setRollEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_ROLL).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_ROLL.getCmd(), enabled);
    }

    private void setLaunchEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_LAUNCH).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_LAUNCH.getCmd(), enabled);
    }

    private void setDockEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DOCK).setEnabled(enabled);
    }

    private void setRecoverEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_RECOVER).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_RECOVER.getCmd(), enabled);
    }

    private void setDropEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DROP).setEnabled(enabled);
    }

    private void setJoinEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_JOIN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_JOIN.getCmd(), enabled);
    }

    private void setDumpEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DUMP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_DUMP.getCmd(), enabled);
    }

    private void setRamEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_RAM).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_RAM.getCmd(), enabled);
    }

    private void setHoverEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_HOVER).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_HOVER.getCmd(), enabled);
    }

    private void setTakeOffEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_TAKE_OFF).setEnabled(enabled);
    }

    private void setVTakeOffEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_VERT_TAKE_OFF).setEnabled(enabled);
    }

    private void setLandEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_LAND).setEnabled(enabled);
    }

    private void setVLandEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_VERT_LAND).setEnabled(enabled);
    }

    private void setManeuverEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_MANEUVER).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_MANEUVER.getCmd(), enabled);
    }

    private void setTurnLeftEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_TURN_LEFT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_TURN_LEFT.getCmd(), enabled);
    }

    private void setTurnRightEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_TURN_RIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_TURN_RIGHT.getCmd(), enabled);
    }

    private void setThrustEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_THRUST).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_THRUST.getCmd(), enabled);
    }

    private void setYawEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_YAW).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_YAW.getCmd(), enabled);
    }

    private void setEndOverEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_END_OVER).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_END_OVER.getCmd(), enabled);
    }

    private void setStrafeEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_STRAFE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_STRAFE.getCmd(), enabled);
    }

    private void setBombEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_BOMB).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_BOMB.getCmd(), enabled);
    }

    private void setPickupCargoEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_PICKUP_CARGO).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_PICKUP_CARGO.getCmd(), enabled);
    }

    private void setDropCargoEnabled(boolean enabled) {
        getBtn(MoveCommand.MOVE_DROP_CARGO).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(MoveCommand.MOVE_DROP_CARGO.getCmd(), enabled);
    }

    private void updateTurnButton() {
        final Entity currentEntity = currentEntity();
        if (null == currentEntity) {
            return;
        }

        setTurnEnabled(!currentEntity.isImmobile() &&
              !currentEntity.isStuck() &&
              ((currentEntity.getWalkMP() > 0) || (currentEntity.getJumpMP() > 0)) &&
              !(cmd.isJumping() && (currentEntity instanceof Mek) && (jumpSubGear == GEAR_SUB_MEK_BOOSTERS)));
    }

    @Nullable
    public MovePath getPlannedMovement() {
        return cmd;
    }

    private void performAeroLand(LandingDirection landingDirection) {
        Entity entity = currentEntity();
        if (!(entity instanceof IAero aero) || !entity.isAero()) {
            LOGGER.warn("Selected aero landing for a non-aero unit!");
            return;
        }
        if (usingAeroOnGroundMovement()) {
            finalizeAeroLandFromGroundMap(aero, landingDirection);
        } else {
            startAeroLandNoGroundMove(entity, landingDirection);
        }
    }

    private void finalizeAeroLandFromGroundMap(IAero aero, LandingDirection landingDirection) {
        if (!game.isOnGroundMap((Entity) aero)) {
            LOGGER.warn("Selected aero landing from ground map for unit that isn't on a ground map!");
            return;
        }
        String failMessage = aero.hasRoomForLanding(landingDirection);
        if (failMessage != null) {
            String title = Messages.getString("MovementDisplay.NoLandingDialog.title");
            String body = Messages.getString("MovementDisplay.NoLandingDialog.message", failMessage);
            clientgui.doAlertDialog(title, body);
        } else {
            landingConfirmation.ask();
            if (landingConfirmation.isOkSelected()) {
                clear();
                addStepToMovePath(landingDirection.moveStepType());
                ready();
            }
        }
    }

    private void startAeroLandNoGroundMove(Entity entity, LandingDirection landingDirection) {
        if (!game.hasBoardLocationOf(entity)
              || !entity.isAirborne()
              || !game.isOnAtmosphericMap(entity)
              || !game.getBoard(entity).getEmbeddedBoardHexes().contains(finalPosition())) {
            LOGGER.warn("No ground map to land on in the present hex!");
            return;
        }

        gear = GEAR_LANDING_AERO;
        setStatusBarText(Messages.getString("MovementDisplay.status.selectTouchDownHex"));
        cmd = new MovePath(game, entity);
        addStepToMovePath(landingDirection.moveStepType());
        Board landingBoard = game.getBoard(groundMapAtAtmosphericHex());
        clientgui.showBoardView(landingBoard.getBoardId());
        new LandingHexNotice(clientgui).show();
    }

    private void finalizeAeroLandFromAtmosphereMap(IAero aero, BoardViewEvent event) {
        if (!hasLandingMoveStep()) {
            LOGGER.error("No landing move path; can't determine if vertical or horizontal landing!");
            return;
        }
        LandingDirection landingDirection = cmd.contains(MoveStepType.LAND) ? HORIZONTAL : VERTICAL;
        String failMessage = aero.hasRoomForLanding(event.getBoardId(), event.getCoords(), landingDirection);
        if (failMessage != null) {
            String title = Messages.getString("MovementDisplay.NoLandingDialog.title");
            String body = Messages.getString("MovementDisplay.NoLandingDialog.message", failMessage);
            clientgui.doAlertDialog(title, body);
        } else {
            landingConfirmation.ask();
            if (landingConfirmation.isOkSelected()) {
                clear();
                cmd = new AtmosphericLandingMovePath(game, (Entity) aero, event.getBoardLocation(), landingDirection);
                ready();
            }
        }
    }

    private boolean hasLandingMoveStep() {
        return (cmd != null) && (cmd.contains(MoveStepType.LAND) || cmd.contains(MoveStepType.VERTICAL_LAND));
    }
}
